package myophoria.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import myophoria.core.csv.CsvCodec;

/** Owns the SQLite file: creates and migrates it once, then hands out connections. */
public class Database {
    /** Name of the database file inside the data directory. */
    public static final String FILE_NAME = "myophoria.db";
    private static final int SQLITE_CORRUPT = 11;
    private static final int SQLITE_NOTADB = 26;
    private final String url;
    private final Path dataDir;

    /** Creates a handle for the database file myophoria.db inside the data directory. */
    public Database(Path dataDir) {
        this.dataDir = dataDir;
        this.url = "jdbc:sqlite:" + dataDir.resolve(FILE_NAME);
    }

    /** Returns the folder that holds backups (dataDir/backups). */
    public Path backupDir() {
        return dataDir.resolve("backups");
    }

    /**
     * Creates the data directory, recovers from a corrupted file (§3.3 step 2), enables WAL and migrates.
     * Returns warnings for the UI banner; recovery is never silent.
     */
    public List<String> open() {
        List<String> warnings = new ArrayList<>();
        try {
            Files.createDirectories(dataDir);
            recoverIfCorrupt(warnings);
            try (Connection c = connect()) {
                try (Statement st = c.createStatement()) { // closed before migrating: an open statement blocks commit
                    st.execute("PRAGMA journal_mode = WAL");
                }
                Migrator.migrate(c);
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Cannot open database", e);
        }
        return warnings;
    }

    // A corrupt file is renamed (never deleted); then backups are tried newest first until one passes the check.
    private void recoverIfCorrupt(List<String> warnings) throws IOException {
        Path file = dataDir.resolve(FILE_NAME);
        if (!Files.exists(file) || isHealthy()) {
            return;
        }
        Path kept = moveAside(file);
        List<Path> candidates = new ArrayList<>();
        if (Files.isDirectory(backupDir())) {
            try (var files = Files.list(backupDir())) {
                for (Path p : (Iterable<Path>) files::iterator) {
                    if (p.getFileName().toString().endsWith(".db")) {
                        candidates.add(p);
                    }
                }
            }
        }
        candidates.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed()); // names sort by time
        for (Path backup : candidates) {
            Files.copy(backup, file, StandardCopyOption.REPLACE_EXISTING);
            if (isHealthy()) {
                warnings.add("The database file was damaged. It was kept as " + kept.getFileName()
                        + " and the backup " + backup.getFileName() + " was restored.");
                return;
            }
            Files.delete(file);
            warnings.add("The backup " + backup.getFileName() + " is damaged too and was skipped.");
        }
        warnings.add("The database file was damaged. It was kept as " + kept.getFileName()
                + " and no usable backup exists, so the app started with an empty database.");
    }

    // Only real damage counts; a locked or unreadable file must stop startup instead of being replaced.
    private boolean isHealthy() throws IOException {
        try (Connection c = DriverManager.getConnection(url); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("PRAGMA quick_check")) {
            return rs.next() && "ok".equals(rs.getString(1));
        } catch (SQLException e) {
            int primary = e.getErrorCode() & 0xff; // SQLite result code without the extended bits
            if (primary == SQLITE_CORRUPT || primary == SQLITE_NOTADB) {
                return false;
            }
            throw new IOException("Cannot check the database file: " + e.getMessage(), e);
        }
    }

    // renames the file and its WAL/SHM companions to *.corrupt-<time>
    private Path moveAside(Path file) throws IOException {
        String suffix = ".corrupt-" + SqliteBackups.FILE_TIME.format(Instant.now());
        Path kept = file.resolveSibling(file.getFileName() + suffix);
        Files.move(file, kept);
        for (String companion : new String[] {"-wal", "-shm"}) {
            Path p = file.resolveSibling(file.getFileName() + companion);
            if (Files.exists(p)) {
                Files.move(p, p.resolveSibling(p.getFileName() + suffix));
            }
        }
        return kept;
    }

    /** Opens a new connection with foreign keys on and a 5 second busy timeout. */
    public Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            c.close();
            throw e;
        }
        return c;
    }

    /** Formats an Instant as fixed-width UTC text (null stays null). */
    static String format(Instant at) {
        return at == null ? null : CsvCodec.TIMESTAMP.format(at);
    }

    /** Parses stored timestamp text (null stays null). */
    static Instant instant(String text) {
        return text == null ? null : Instant.from(CsvCodec.TIMESTAMP.parse(text));
    }
}
