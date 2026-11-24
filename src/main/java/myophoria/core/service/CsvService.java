package myophoria.core.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import myophoria.core.csv.CsvCodec;
import myophoria.core.csv.CsvSerializable;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.Exercise;
import myophoria.core.model.Gym;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.model.WorkoutTemplate;
import myophoria.core.port.Backups;
import myophoria.core.port.BodyweightRepository;
import myophoria.core.port.ExerciseRepository;
import myophoria.core.port.GymRepository;
import myophoria.core.port.SessionRepository;
import myophoria.core.port.SetRepository;
import myophoria.core.port.TemplateRepository;

/** ZIP of CSV files for export and import (PLAN §6.3, G10). Import never overwrites existing rows. */
public class CsvService {
    /** Version written to and required in manifest.csv. */
    public static final String FORMAT_VERSION = "1";
    static final String[] MANIFEST_HEADER = {"format_version", "exported_at"};
    // zip-bomb guard: a personal log is a few MB at most
    static final long MAX_UNZIPPED_BYTES = 64L * 1024 * 1024;

    private final GymRepository gyms;
    private final ExerciseRepository exercises;
    private final SessionRepository sessions;
    private final SetRepository sets;
    private final BodyweightRepository bodyweights;
    private final TemplateRepository templates;
    private final Backups backups;
    private final Clock clock;

    /** Creates the service on every repository, the backup port and a clock. */
    public CsvService(GymRepository gyms, ExerciseRepository exercises, SessionRepository sessions,
            SetRepository sets, BodyweightRepository bodyweights, TemplateRepository templates, Backups backups,
            Clock clock) {
        this.gyms = gyms;
        this.exercises = exercises;
        this.sessions = sessions;
        this.sets = sets;
        this.bodyweights = bodyweights;
        this.templates = templates;
        this.backups = backups;
        this.clock = clock;
    }

    /** Writes every table (deleted rows included) as one ZIP of CSV files. */
    public void export(OutputStream out) throws IOException {
        List<WorkoutSession> allSessions = sessions.findAll();
        List<WorkoutSet> allSets = new ArrayList<>();
        for (WorkoutSession s : allSessions) {
            allSets.addAll(s.getSets());
        }
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            Writer w = new OutputStreamWriter(zip, StandardCharsets.UTF_8); // closed with the zip
            entry(zip, w, "manifest.csv", MANIFEST_HEADER,
                    List.<String[]>of(new String[] {FORMAT_VERSION, CsvCodec.format(clock.instant())}));
            entry(zip, w, "gyms.csv", Gym.HEADER, rowsOf(gyms.findAll()));
            entry(zip, w, "exercises.csv", Exercise.HEADER, rowsOf(exercises.findAll()));
            entry(zip, w, "sessions.csv", WorkoutSession.HEADER, rowsOf(allSessions));
            entry(zip, w, "sets.csv", WorkoutSet.HEADER, rowsOf(allSets));
            entry(zip, w, "bodyweight.csv", BodyweightEntry.HEADER, rowsOf(bodyweights.findAll()));
            entry(zip, w, "templates.csv", WorkoutTemplate.HEADER, rowsOf(templates.findAll()));
        }
    }

    /**
     * Imports a ZIP made by export. A bad manifest rejects the whole file (nothing written). Otherwise a backup is
     * taken first, then each file is imported row by row: bad rows are skipped, rows whose parent is missing are
     * orphans, known IDs are counted as already present, and active name clashes are reported.
     */
    public ImportReport importZip(InputStream in) throws IOException, CorruptedCsvException {
        Map<String, byte[]> files = unzip(in);
        checkManifest(files.get("manifest.csv"));
        backups.createBackup(); // throws before any write if the backup fails

        Map<String, FileReport> report = new LinkedHashMap<>();
        Set<String> gymIds = new HashSet<>();
        Set<String> exerciseIds = new HashSet<>();
        Set<String> sessionIds = new HashSet<>();
        report.put("gyms.csv", importGyms(files, gymIds));
        report.put("exercises.csv", importExercises(files, exerciseIds));
        report.put("sessions.csv", importSessions(files, gymIds, sessionIds));
        report.put("sets.csv", importSets(files, sessionIds, exerciseIds));
        report.put("bodyweight.csv", importBodyweight(files));
        report.put("templates.csv", importTemplates(files, exerciseIds));
        return new ImportReport(report);
    }

    // ---------- per file; each ID set starts with what the database already holds ----------

    private FileReport importGyms(Map<String, byte[]> files, Set<String> ids) {
        Tally t = new Tally("gyms.csv");
        Map<String, String> activeNames = new HashMap<>();
        boolean hasDefault = false;
        for (Gym g : gyms.findAll()) {
            ids.add(g.getId());
            if (!g.isDeleted()) {
                activeNames.put(g.getNameKey(), g.getId());
                hasDefault |= g.isDefault();
            }
        }
        for (Row row : t.rows(files, Gym.HEADER)) {
            Gym g;
            try {
                g = Gym.fromCsvRow(row.fields());
            } catch (CorruptedCsvException e) {
                t.skip(row, e.getReason());
                continue;
            }
            if (ids.contains(g.getId())) {
                t.alreadyPresent++;
                continue;
            }
            if (!g.isDeleted() && activeNames.containsKey(g.getNameKey())) {
                t.nameConflicts.add(t.where(row) + " name \"" + g.getName() + "\" is used by gym " + activeNames.get(g.getNameKey()));
                continue;
            }
            if (g.isDefault() && (hasDefault || g.isDeleted())) {
                g.setDefault(false);
                t.adjusted.add(t.where(row) + " imported without the default flag (there is already a default gym)");
            }
            if (t.insert(row, () -> gyms.insert(g))) {
                ids.add(g.getId());
                if (!g.isDeleted()) {
                    activeNames.put(g.getNameKey(), g.getId());
                    hasDefault |= g.isDefault();
                }
            }
        }
        return t.report();
    }

    private FileReport importExercises(Map<String, byte[]> files, Set<String> ids) {
        Tally t = new Tally("exercises.csv");
        Map<String, String> activeNames = new HashMap<>();
        for (Exercise e : exercises.findAll()) {
            ids.add(e.getId());
            if (!e.isDeleted()) {
                activeNames.put(e.getNameKey(), e.getId());
            }
        }
        for (Row row : t.rows(files, Exercise.HEADER)) {
            Exercise e;
            try {
                e = Exercise.fromCsvRow(row.fields());
            } catch (CorruptedCsvException ex) {
                t.skip(row, ex.getReason());
                continue;
            }
            if (ids.contains(e.getId())) {
                t.alreadyPresent++;
                continue;
            }
            if (!e.isDeleted() && activeNames.containsKey(e.getNameKey())) {
                t.nameConflicts.add(t.where(row) + " name \"" + e.getName() + "\" is used by exercise " + activeNames.get(e.getNameKey()));
                continue;
            }
            if (t.insert(row, () -> exercises.insert(e))) {
                ids.add(e.getId());
                if (!e.isDeleted()) {
                    activeNames.put(e.getNameKey(), e.getId());
                }
            }
        }
        return t.report();
    }

    private FileReport importSessions(Map<String, byte[]> files, Set<String> gymIds, Set<String> ids) {
        Tally t = new Tally("sessions.csv");
        boolean hasOpen = false;
        for (WorkoutSession s : sessions.findAll()) {
            ids.add(s.getId());
            hasOpen |= s.isOpen();
        }
        for (Row row : t.rows(files, WorkoutSession.HEADER)) {
            WorkoutSession s;
            try {
                s = WorkoutSession.fromCsvRow(row.fields());
            } catch (CorruptedCsvException e) {
                t.skip(row, e.getReason());
                continue;
            }
            if (ids.contains(s.getId())) {
                t.alreadyPresent++;
                continue;
            }
            if (!gymIds.contains(s.getGymId())) {
                t.orphans.add(t.where(row) + " gym " + s.getGymId() + " is missing");
                continue;
            }
            WorkoutSession toInsert = s;
            if (s.isOpen() && hasOpen) {
                toInsert = new WorkoutSession(s.getId(), s.getGymId(), s.getLocalDate(), s.getStartedAt(), null, false);
                t.adjusted.add(t.where(row) + " imported as a finished workout (another workout is open)");
            }
            WorkoutSession finalSession = toInsert;
            if (t.insert(row, () -> sessions.insert(finalSession))) {
                ids.add(s.getId());
                hasOpen |= finalSession.isOpen();
            }
        }
        return t.report();
    }

    private FileReport importSets(Map<String, byte[]> files, Set<String> sessionIds, Set<String> exerciseIds) {
        Tally t = new Tally("sets.csv");
        Set<String> ids = new HashSet<>();
        for (WorkoutSession s : sessions.findAll()) {
            for (WorkoutSet set : s.getSets()) {
                ids.add(set.getId());
            }
        }
        for (Row row : t.rows(files, WorkoutSet.HEADER)) {
            WorkoutSet set;
            try {
                set = WorkoutSet.fromCsvRow(row.fields());
            } catch (CorruptedCsvException e) {
                t.skip(row, e.getReason());
                continue;
            }
            if (ids.contains(set.getId())) {
                t.alreadyPresent++;
                continue;
            }
            if (!sessionIds.contains(set.getSessionId())) {
                t.orphans.add(t.where(row) + " session " + set.getSessionId() + " is missing");
                continue;
            }
            if (!exerciseIds.contains(set.getExerciseId())) {
                t.orphans.add(t.where(row) + " exercise " + set.getExerciseId() + " is missing");
                continue;
            }
            if (t.insert(row, () -> sets.insert(set))) {
                ids.add(set.getId());
            }
        }
        return t.report();
    }

    private FileReport importBodyweight(Map<String, byte[]> files) {
        Tally t = new Tally("bodyweight.csv");
        Set<String> ids = new HashSet<>();
        for (BodyweightEntry b : bodyweights.findAll()) {
            ids.add(b.getId());
        }
        for (Row row : t.rows(files, BodyweightEntry.HEADER)) {
            BodyweightEntry b;
            try {
                b = BodyweightEntry.fromCsvRow(row.fields());
            } catch (CorruptedCsvException e) {
                t.skip(row, e.getReason());
                continue;
            }
            if (ids.contains(b.getId())) {
                t.alreadyPresent++;
            } else if (t.insert(row, () -> bodyweights.insert(b))) {
                ids.add(b.getId());
            }
        }
        return t.report();
    }

    private FileReport importTemplates(Map<String, byte[]> files, Set<String> exerciseIds) {
        Tally t = new Tally("templates.csv");
        Set<String> ids = new HashSet<>();
        for (WorkoutTemplate tpl : templates.findAll()) {
            ids.add(tpl.getId());
        }
        for (Row row : t.rows(files, WorkoutTemplate.HEADER)) {
            WorkoutTemplate tpl;
            try {
                tpl = WorkoutTemplate.fromCsvRow(row.fields());
            } catch (CorruptedCsvException e) {
                t.skip(row, e.getReason());
                continue;
            }
            if (ids.contains(tpl.getId())) {
                t.alreadyPresent++;
                continue;
            }
            String missing = null;
            for (String exerciseId : tpl.getExerciseIds()) {
                if (!exerciseIds.contains(exerciseId)) {
                    missing = exerciseId;
                }
            }
            if (missing != null) {
                t.orphans.add(t.where(row) + " exercise " + missing + " is missing");
            } else if (t.insert(row, () -> templates.insert(tpl))) {
                ids.add(tpl.getId());
            }
        }
        return t.report();
    }

    // ---------- helpers ----------

    private record Row(int line, String[] fields) {
    }

    /** Mutable counters for one file while importing; report() freezes them into a FileReport. */
    private static final class Tally {
        final String file;
        int imported;
        int alreadyPresent;
        final List<String> skipped = new ArrayList<>();
        final List<String> orphans = new ArrayList<>();
        final List<String> nameConflicts = new ArrayList<>();
        final List<String> adjusted = new ArrayList<>();

        Tally(String file) {
            this.file = file;
        }

        /** Parses the file; a missing or unreadable file is reported and yields no rows. */
        List<Row> rows(Map<String, byte[]> files, String[] header) {
            List<Row> rows = new ArrayList<>();
            byte[] bytes = files.get(file);
            if (bytes == null) {
                skipped.add(file + " is missing from the archive");
                return rows;
            }
            List<String[]> parsed;
            try (Reader r = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                parsed = CsvCodec.parse(r, file);
            } catch (CorruptedCsvException e) {
                skipped.add(e.getMessage() + " (whole file skipped)");
                return rows;
            } catch (IOException e) {
                skipped.add(file + " cannot be read (whole file skipped)");
                return rows;
            }
            if (parsed.isEmpty() || !Arrays.equals(parsed.get(0), header)) {
                skipped.add(file + ":1 unexpected header, expected " + String.join(",", header) + " (whole file skipped)");
                return rows;
            }
            for (int i = 1; i < parsed.size(); i++) {
                rows.add(new Row(i + 1, parsed.get(i)));
            }
            return rows;
        }

        String where(Row row) {
            return file + ":" + row.line();
        }

        void skip(Row row, String reason) {
            skipped.add(where(row) + " " + reason);
        }

        /** Runs the insert; a row the database rejects is skipped instead of stopping the import. */
        boolean insert(Row row, Runnable insert) {
            try {
                insert.run();
                imported++;
                return true;
            } catch (IllegalStateException e) {
                skip(row, "rejected by the database");
                return false;
            }
        }

        FileReport report() {
            return new FileReport(imported, alreadyPresent, skipped, orphans, nameConflicts, adjusted);
        }
    }

    private static List<String[]> rowsOf(List<? extends CsvSerializable> items) {
        List<String[]> rows = new ArrayList<>();
        for (CsvSerializable item : items) {
            rows.add(item.toCsvRow());
        }
        return rows;
    }

    private static void entry(ZipOutputStream zip, Writer w, String name, String[] header, List<String[]> rows)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        w.write(CsvCodec.formatRow(header));
        w.write('\n');
        for (String[] row : rows) {
            w.write(CsvCodec.formatRow(row));
            w.write('\n');
        }
        w.flush();
        zip.closeEntry();
    }

    private static Map<String, byte[]> unzip(InputStream in) throws IOException, CorruptedCsvException {
        Map<String, byte[]> files = new HashMap<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry e;
            byte[] buffer = new byte[8192];
            while ((e = zip.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int n;
                while ((n = zip.read(buffer)) > 0) {
                    total += n;
                    if (total > MAX_UNZIPPED_BYTES) {
                        throw new CorruptedCsvException("import", 0, "archive is larger than 64 MB unpacked");
                    }
                    bytes.write(buffer, 0, n);
                }
                String name = e.getName().substring(e.getName().lastIndexOf('/') + 1); // tolerate a top folder
                files.put(name, bytes.toByteArray());
            }
        } catch (ZipException ex) {
            throw new CorruptedCsvException("import", 0, "not a valid ZIP file");
        }
        return files;
    }

    private static void checkManifest(byte[] manifest) throws CorruptedCsvException, IOException {
        if (manifest == null) {
            throw new CorruptedCsvException("manifest.csv", 0, "missing: not a Myophoria export");
        }
        List<String[]> rows = CsvCodec.parse(
                new InputStreamReader(new ByteArrayInputStream(manifest), StandardCharsets.UTF_8), "manifest.csv");
        if (rows.size() != 2 || !Arrays.equals(rows.get(0), MANIFEST_HEADER)) {
            throw new CorruptedCsvException("manifest.csv", 1, "expected header format_version,exported_at and one row");
        }
        if (!FORMAT_VERSION.equals(rows.get(1)[0])) {
            throw new CorruptedCsvException("manifest.csv", 2, "unsupported format version " + rows.get(1)[0]);
        }
    }
}
