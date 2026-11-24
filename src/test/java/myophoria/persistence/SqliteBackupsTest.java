package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteBackupsTest {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T07:30:00.123Z"), ZoneOffset.UTC);
    @TempDir
    Path dir;
    Database db;
    SqliteBackups backups;

    @BeforeEach
    void setUp() {
        db = new Database(dir.resolve("data"));
        db.open();
        new SqliteExerciseRepository(db).insert(Exercise.of(EquipmentType.BARBELL, "e1", "Squat", Laterality.BILATERAL, null));
        backups = new SqliteBackups(db, dir.resolve("backups"), CLOCK);
    }

    @Test
    void backupIsAWorkingDatabaseWithTheData() throws Exception {
        String file = backups.createBackup();
        assertEquals("myophoria-20261125T073000.123Z.db", file);
        Path copy = dir.resolve("backups").resolve(file);
        // open the copy as if it were the live database
        Path restored = dir.resolve("restored");
        Files.createDirectories(restored);
        Files.copy(copy, restored.resolve("myophoria.db"));
        Database other = new Database(restored);
        other.open();
        assertEquals("Squat", new SqliteExerciseRepository(other).findById("e1").getName());

        assertEquals("myophoria-20261125T073000.123Z-2.db", backups.createBackup()); // same millisecond
        try (var files = Files.list(dir.resolve("backups"))) {
            assertEquals(2, files.count());
        }
    }

    @Test
    void failedBackupLeavesNoTmpFile() throws Exception {
        Path backupDir = dir.resolve("backups");
        Files.createDirectories(backupDir);
        Path tmp = backupDir.resolve("myophoria-20261125T073000.123Z.db.tmp");
        Files.writeString(tmp, "not empty"); // VACUUM INTO refuses a non-empty target
        assertThrows(IOException.class, () -> backups.createBackup());
        assertFalse(Files.exists(tmp));
        assertFalse(Files.exists(backupDir.resolve("myophoria-20261125T073000.123Z.db")));
    }
}
