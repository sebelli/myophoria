package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import myophoria.core.model.Gym;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteGymRepositoryTest {
    @TempDir
    Path dir;

    @Test
    void roundTripAndMakeDefault() {
        Database db = new Database(dir);
        db.open();
        SqliteGymRepository repo = new SqliteGymRepository(db);
        assertTrue(repo.findById(Gym.SEEDED_DEFAULT_ID).isDefault());

        repo.insert(new Gym("g2", "Home Gym", false, null));
        Gym home = repo.findById("g2");
        assertEquals("home gym", home.getNameKey());
        repo.makeDefault("g2");
        assertTrue(repo.findById("g2").isDefault());
        assertFalse(repo.findById(Gym.SEEDED_DEFAULT_ID).isDefault());

        Instant at = Instant.parse("2026-11-25T07:00:00.001Z");
        Gym seeded = repo.findById(Gym.SEEDED_DEFAULT_ID);
        seeded.softDelete(at);
        repo.update(seeded);
        assertEquals(at, repo.findById(Gym.SEEDED_DEFAULT_ID).getDeletedAt());
        assertEquals(2, repo.findAll().size());
        assertThrows(IllegalStateException.class, () -> repo.insert(new Gym("g3", "home gym", false, null)));
    }
}
