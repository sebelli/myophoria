package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteBodyweightRepositoryTest {
    @TempDir
    Path dir;

    @Test
    void roundTrip() {
        Database db = new Database(dir);
        db.open();
        SqliteBodyweightRepository repo = new SqliteBodyweightRepository(db);
        Instant at = Instant.parse("2026-11-25T07:30:00.123Z");
        repo.insert(new BodyweightEntry("b1", 70.5, Unit.KG, at, LocalDate.of(2026, 11, 25)));

        BodyweightEntry loaded = repo.findById("b1");
        assertEquals(70.5, loaded.getWeight());
        assertEquals(Unit.KG, loaded.getUnit());
        assertEquals(at, loaded.getRecordedAt());
        assertEquals(LocalDate.of(2026, 11, 25), loaded.getLocalDate());

        loaded.setWeight(155);
        loaded.setUnit(Unit.LB);
        repo.update(loaded);
        assertEquals(Unit.LB, repo.findById("b1").getUnit());
        assertEquals(1, repo.findAll().size());

        repo.delete("b1");
        assertNull(repo.findById("b1"));
    }
}
