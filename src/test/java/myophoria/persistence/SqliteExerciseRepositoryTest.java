package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteExerciseRepositoryTest {
    @TempDir
    Path dir;
    Database db;
    SqliteExerciseRepository repo;

    @BeforeEach
    void setUp() {
        db = new Database(dir);
        db.open();
        repo = new SqliteExerciseRepository(db);
    }

    @Test
    void migrationIsIdempotentAndSeedsDefaultGym() throws Exception {
        db.open(); // second run must not fail or re-insert
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA user_version")) {
                assertEquals(1, rs.getInt(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM gyms WHERE is_default = 1")) {
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    void insertFindUpdateRoundTrip() {
        Exercise e = Exercise.of(EquipmentType.DUMBBELL, "e1", "Incline Press", Laterality.UNILATERAL, null);
        repo.insert(e);

        Exercise loaded = repo.findById("e1");
        assertEquals("Incline Press", loaded.getName());
        assertEquals("incline press", loaded.getNameKey());
        assertEquals(EquipmentType.DUMBBELL, loaded.type());
        assertEquals(Laterality.UNILATERAL, loaded.getLaterality());
        assertNull(loaded.getDeletedAt());

        Instant at = Instant.parse("2026-11-25T07:30:00.123Z");
        loaded.setName("Flat Press");
        loaded.softDelete(at);
        repo.update(loaded);

        Exercise again = repo.findById("e1");
        assertEquals("Flat Press", again.getName());
        assertEquals(at, again.getDeletedAt());
        assertEquals(List.of("e1"), repo.findAll().stream().map(Exercise::getId).toList());
        assertEquals(0, repo.countSets("e1"));
        assertEquals(0, repo.countSessions("e1"));
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        assertNull(repo.findById("nope"));
    }

    @Test
    void activeNameMustBeUnique() {
        repo.insert(Exercise.of(EquipmentType.BARBELL, "a", "Squat", Laterality.BILATERAL, null));
        Exercise dup = Exercise.of(EquipmentType.BARBELL, "b", " squat ", Laterality.BILATERAL, null);
        assertThrows(IllegalStateException.class, () -> repo.insert(dup));
    }
}
