package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Gym;
import myophoria.core.model.Laterality;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.model.WorkoutTemplate;
import myophoria.core.service.CsvService;
import myophoria.core.service.FileReport;
import myophoria.core.service.ImportReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Export → import through real SQLite databases (CsvService with the SQLite adapters). */
class CsvRoundTripTest {
    static final Instant T = Instant.parse("2026-11-25T07:00:00.000Z");
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);
    static final Clock CLOCK = Clock.fixed(T, ZoneOffset.UTC);

    @TempDir
    Path dir;

    /** One database with all adapters and its CsvService. */
    final class Store {
        final Path root;
        final SqliteGymRepository gyms;
        final SqliteExerciseRepository exercises;
        final SqliteSessionRepository sessions;
        final SqliteSetRepository sets;
        final SqliteBodyweightRepository bodyweight;
        final SqliteTemplateRepository templates;
        final CsvService csv;

        Store(String name) {
            root = dir.resolve(name);
            Database db = new Database(root);
            db.open();
            gyms = new SqliteGymRepository(db);
            exercises = new SqliteExerciseRepository(db);
            sessions = new SqliteSessionRepository(db);
            sets = new SqliteSetRepository(db);
            bodyweight = new SqliteBodyweightRepository(db);
            templates = new SqliteTemplateRepository(db);
            csv = new CsvService(gyms, exercises, sessions, sets, bodyweight, templates,
                    new SqliteBackups(db, root.resolve("backups"), CLOCK), CLOCK);
        }

        byte[] export() throws Exception {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            csv.export(out);
            return out.toByteArray();
        }

        ImportReport importZip(byte[] zip) throws Exception {
            return csv.importZip(new ByteArrayInputStream(zip));
        }

        long backupCount() throws Exception {
            if (!Files.exists(root.resolve("backups"))) {
                return 0;
            }
            try (var files = Files.list(root.resolve("backups"))) {
                return files.count();
            }
        }
    }

    private Store seeded() {
        Store a = new Store("a");
        a.gyms.insert(new Gym("g-home", "Home, \"garage\"", false, null));
        a.exercises.insert(Exercise.of(EquipmentType.BARBELL, "e-squat", "Squat", Laterality.BILATERAL, null));
        a.exercises.insert(Exercise.of(EquipmentType.CALISTHENICS, "e-pull", "Pull Up", Laterality.BILATERAL, null));
        a.exercises.insert(Exercise.of(EquipmentType.DUMBBELL, "e-old", "Old Curl", Laterality.UNILATERAL, T));
        a.sessions.insert(new WorkoutSession("s-1", "g-home", DAY.minusDays(1), T.minusSeconds(86400), T.minusSeconds(82800), false));
        a.sessions.insert(new WorkoutSession("s-2", Gym.SEEDED_DEFAULT_ID, DAY, T, null, true));
        a.sets.insert(new WorkoutSet("x-1", "s-1", "e-squat", 1, SetType.WORKING, 102.5, Unit.KG, 5, null, T.minusSeconds(86000)));
        a.sets.insert(new WorkoutSet("x-2", "s-1", "e-pull", 1, SetType.FAILURE, 10, Unit.LB, 6, 154.0, T.minusSeconds(85000)));
        a.sets.insert(new WorkoutSet("x-3", "s-2", "e-old", 1, SetType.WARMUP, 8, Unit.KG, 12, null, T.plusSeconds(60)));
        a.bodyweight.insert(new BodyweightEntry("b-1", 70.4, Unit.KG, T, DAY));
        a.templates.insert(new WorkoutTemplate("t-1", "Legs; heavy", List.of("e-pull", "e-squat")));
        return a;
    }

    private static Map<String, String> unzip(byte[] zip) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                files.put(e.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return files;
    }

    private static byte[] zip(Map<String, String> files) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> f : files.entrySet()) {
                z.putNextEntry(new ZipEntry(f.getKey()));
                z.write(f.getValue().getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    void exportImportIntoEmptyDatabaseRecreatesEverything() throws Exception {
        Store a = seeded();
        byte[] zip = a.export();
        Map<String, String> files = unzip(zip);
        assertEquals(List.of("manifest.csv", "gyms.csv", "exercises.csv", "sessions.csv", "sets.csv",
                "bodyweight.csv", "templates.csv"), List.copyOf(files.keySet()));
        assertEquals("format_version,exported_at\n1,2026-11-25T07:00:00.000Z\n", files.get("manifest.csv"));

        Store b = new Store("b");
        ImportReport r = b.importZip(zip);
        assertEquals(1, b.backupCount()); // backup before the first write
        assertEquals(new FileReport(1, 1, List.of(), List.of(), List.of(), List.of()), r.files().get("gyms.csv")); // seeded gym shared
        assertEquals(3, r.files().get("exercises.csv").imported());
        assertEquals(2, r.files().get("sessions.csv").imported());
        assertEquals(3, r.files().get("sets.csv").imported());
        assertEquals(1, r.files().get("bodyweight.csv").imported());
        assertEquals(1, r.files().get("templates.csv").imported());

        // every data file is identical after the trip (only the manifest time could differ)
        Map<String, String> again = unzip(b.export());
        for (String name : files.keySet()) {
            assertEquals(files.get(name), again.get(name), name);
        }
        assertEquals(EquipmentType.CALISTHENICS, b.exercises.findById("e-pull").type());
        assertTrue(b.sessions.findOpen() != null);
    }

    @Test
    void reimportFindsEverythingAlreadyPresent() throws Exception {
        Store a = seeded();
        byte[] zip = a.export();
        ImportReport r = a.importZip(zip);
        for (Map.Entry<String, FileReport> f : r.files().entrySet()) {
            assertEquals(0, f.getValue().imported(), f.getKey());
            assertTrue(f.getValue().skipped().isEmpty(), f.getKey());
        }
        assertEquals(2, r.files().get("gyms.csv").alreadyPresent());
        assertEquals(3, r.files().get("sets.csv").alreadyPresent());
    }

    @Test
    void corruptedRowIsSkippedAndTheRestImported() throws Exception {
        Map<String, String> files = unzip(seeded().export());
        files.put("sets.csv", files.get("sets.csv").replace(",102.5,", ",heavy,"));
        ImportReport r = new Store("b").importZip(zip(files));
        FileReport sets = r.files().get("sets.csv");
        assertEquals(2, sets.imported());
        assertEquals(1, sets.skipped().size());
        assertTrue(sets.skipped().get(0).startsWith("sets.csv:2 weight is not a number"), sets.skipped().get(0));
    }

    @Test
    void missingFilesAreReportedAndChildrenOrphaned() throws Exception {
        Map<String, String> files = unzip(seeded().export());
        files.remove("sessions.csv");
        files.remove("bodyweight.csv");
        ImportReport r = new Store("b").importZip(zip(files));
        assertEquals(List.of("sessions.csv is missing from the archive"), r.files().get("sessions.csv").skipped());
        assertEquals(List.of("bodyweight.csv is missing from the archive"), r.files().get("bodyweight.csv").skipped());
        FileReport sets = r.files().get("sets.csv");
        assertEquals(0, sets.imported());
        assertEquals(3, sets.orphans().size());
        assertTrue(sets.orphans().get(0).contains("session s-1 is missing"));
        assertEquals(1, r.files().get("templates.csv").imported()); // exercises still arrived
    }

    @Test
    void badManifestRejectsEverythingBeforeAnyWrite() throws Exception {
        Map<String, String> files = unzip(seeded().export());
        files.put("manifest.csv", "format_version,exported_at\n2,2026-11-25T07:00:00.000Z\n");
        Store b = new Store("b");
        CorruptedCsvException e = assertThrows(CorruptedCsvException.class, () -> b.importZip(zip(files)));
        assertTrue(e.getMessage().contains("unsupported format version 2"));
        assertEquals(0, b.backupCount());
        assertEquals(0, b.exercises.findAll().size());

        files.remove("manifest.csv");
        assertThrows(CorruptedCsvException.class, () -> b.importZip(zip(files)));
        assertThrows(CorruptedCsvException.class, () -> b.importZip("not a zip at all".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void nameConflictsAndOpenSessionAreHandled() throws Exception {
        byte[] zip = seeded().export();
        Store b = new Store("b");
        b.exercises.insert(Exercise.of(EquipmentType.STACK, "other-squat", "squat", Laterality.BILATERAL, null));
        b.sessions.insert(new WorkoutSession("b-open", Gym.SEEDED_DEFAULT_ID, DAY, T, null, true));
        ImportReport r = b.importZip(zip);

        FileReport ex = r.files().get("exercises.csv");
        assertEquals(1, ex.nameConflicts().size());
        assertTrue(ex.nameConflicts().get(0).contains("other-squat"));
        assertEquals(1, r.files().get("sets.csv").orphans().size()); // the squat set lost its exercise
        assertEquals(1, r.files().get("templates.csv").orphans().size());

        FileReport sessions = r.files().get("sessions.csv");
        assertEquals(2, sessions.imported());
        assertEquals(1, sessions.adjusted().size());
        assertFalse(b.sessions.findById("s-2").isOpen());
        assertEquals("b-open", b.sessions.findOpen().getId());
    }

    @Test
    void wrongHeaderSkipsTheWholeFile() throws Exception {
        Map<String, String> files = unzip(seeded().export());
        files.put("bodyweight.csv", "id,kilos\nb-1,70\n");
        FileReport bw = new Store("b").importZip(zip(files)).files().get("bodyweight.csv");
        assertEquals(0, bw.imported());
        assertTrue(bw.skipped().get(0).contains("unexpected header"));
    }
}
