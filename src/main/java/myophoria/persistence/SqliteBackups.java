package myophoria.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import myophoria.core.port.Backups;

/** Backups with VACUUM INTO: written to a .tmp file first and renamed only when complete. */
public class SqliteBackups implements Backups {
    /** File-name-safe UTC timestamp, sortable as text. */
    static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final Database db;
    private final Path backupDir;
    private final Clock clock;

    /** Creates the backup adapter writing into backupDir. */
    public SqliteBackups(Database db, Path backupDir, Clock clock) {
        this.db = db;
        this.backupDir = backupDir;
        this.clock = clock;
    }

    @Override
    public String createBackup() throws IOException {
        Files.createDirectories(backupDir);
        String base = "myophoria-" + FILE_TIME.format(clock.instant());
        Path target = backupDir.resolve(base + ".db");
        for (int n = 2; Files.exists(target); n++) { // two backups in the same millisecond
            target = backupDir.resolve(base + "-" + n + ".db");
        }
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            try (Connection c = db.connect(); Statement st = c.createStatement()) {
                st.execute("VACUUM INTO '" + tmp.toAbsolutePath().toString().replace("'", "''") + "'");
            } catch (SQLException e) {
                throw new IOException("Backup failed: " + e.getMessage(), e);
            }
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            return target.getFileName().toString();
        } finally {
            Files.deleteIfExists(tmp); // only left over when something failed
        }
    }
}
