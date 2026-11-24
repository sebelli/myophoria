package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Gym;
import myophoria.core.model.Laterality;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseRecoveryTest {
    @TempDir
    Path dir;

    private void corrupt() throws Exception {
        Files.writeString(dir.resolve(Database.FILE_NAME), "this is not a database, just garbage ".repeat(200));
    }

    private long corruptCopies() throws Exception {
        try (var files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith("myophoria.db.corrupt-")).count();
        }
    }

    @Test
    void healthyDatabaseOpensWithoutWarnings() {
        Database db = new Database(dir);
        assertEquals(List.of(), db.open());
        assertEquals(List.of(), db.open());
    }

    @Test
    void corruptFileIsKeptAndNewestBackupRestored() throws Exception {
        Database db = new Database(dir);
        db.open();
        SqliteExerciseRepository repo = new SqliteExerciseRepository(db);
        repo.insert(Exercise.of(EquipmentType.BARBELL, "old", "Old", Laterality.BILATERAL, null));
        new SqliteBackups(db, db.backupDir(), Clock.fixed(Instant.parse("2026-11-20T00:00:00Z"), ZoneOffset.UTC)).createBackup();
        repo.insert(Exercise.of(EquipmentType.BARBELL, "new", "New", Laterality.BILATERAL, null));
        new SqliteBackups(db, db.backupDir(), Clock.fixed(Instant.parse("2026-11-24T00:00:00Z"), ZoneOffset.UTC)).createBackup();
        Files.deleteIfExists(dir.resolve("myophoria.db-wal"));
        Files.deleteIfExists(dir.resolve("myophoria.db-shm"));
        corrupt();

        List<String> warnings = new Database(dir).open();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("myophoria-20261124T000000.000Z.db was restored"), warnings.get(0));
        assertEquals(1, corruptCopies());
        assertNotNull(new SqliteExerciseRepository(new Database(dir)).findById("new"));
    }

    @Test
    void damagedBackupsAreSkippedUntilOneWorks() throws Exception {
        Database db = new Database(dir);
        db.open();
        new SqliteBackups(db, db.backupDir(), Clock.fixed(Instant.parse("2026-11-20T00:00:00Z"), ZoneOffset.UTC)).createBackup();
        Files.writeString(db.backupDir().resolve("myophoria-20261124T000000.000Z.db"), "broken backup ".repeat(100));
        corrupt();

        List<String> warnings = new Database(dir).open();
        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("20261124") && warnings.get(0).contains("damaged too"));
        assertTrue(warnings.get(1).contains("myophoria-20261120T000000.000Z.db was restored"));
    }

    @Test
    void withoutBackupsAFreshDatabaseStarts() throws Exception {
        corrupt();
        Database db = new Database(dir);
        List<String> warnings = db.open();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("no usable backup"));
        assertEquals(1, corruptCopies());
        assertNotNull(new SqliteGymRepository(db).findById(Gym.SEEDED_DEFAULT_ID)); // migrated from scratch
    }
}
