package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteSessionRepositoryTest {
    static final String GYM = "00000000-0000-0000-0000-000000000001"; // seeded by V1
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);
    static final Instant T0 = Instant.parse("2026-11-25T07:00:00.500Z");

    @TempDir
    Path dir;
    SqliteSessionRepository sessions;
    SqliteSetRepository sets;

    @BeforeEach
    void setUp() {
        Database db = new Database(dir);
        db.open();
        sessions = new SqliteSessionRepository(db);
        sets = new SqliteSetRepository(db);
        new SqliteExerciseRepository(db).insert(Exercise.of(EquipmentType.CALISTHENICS, "e1", "Pull Up", Laterality.BILATERAL, null));
    }

    @Test
    void sessionAndSetsRoundTrip() {
        sessions.insert(new WorkoutSession("s1", GYM, DAY, T0, null, true));
        sets.insert(new WorkoutSet("x2", "s1", "e1", 2, SetType.FAILURE, 10, Unit.KG, 6, 70.0, T0.plusSeconds(120)));
        sets.insert(new WorkoutSet("x1", "s1", "e1", 1, SetType.WORKING, 0, Unit.LB, 8, 154.0, T0.plusSeconds(60)));

        WorkoutSession open = sessions.findOpen();
        assertEquals("s1", open.getId());
        assertEquals(T0, open.getStartedAt());
        assertEquals(2, open.getSets().size());
        WorkoutSet first = open.getSets().get(0); // ordered by completed_at
        assertEquals("x1", first.getId());
        assertEquals(Unit.LB, first.getUnit());
        assertEquals(154.0, first.getBodyweight());
        assertEquals(SetType.FAILURE, sets.findById("x2").getSetType());

        WorkoutSet edit = sets.findById("x1");
        edit.setReps(9);
        edit.setBodyweight(null);
        sets.update(edit);
        assertEquals(9, sets.findById("x1").getReps());
        assertNull(sets.findById("x1").getBodyweight());

        open.finish(T0.plusSeconds(3600));
        sessions.update(open);
        assertNull(sessions.findOpen());
        assertEquals(T0.plusSeconds(3600), sessions.findById("s1").getEndedAt());

        sets.delete("x2");
        assertEquals(1, sessions.findById("s1").getSets().size());
        sessions.delete("s1");
        assertNull(sessions.findById("s1"));
        assertNull(sets.findById("x1")); // cascade
    }

    @Test
    void findAllGroupsSetsPerSession() {
        sessions.insert(new WorkoutSession("a", GYM, DAY, null, null, false));
        sessions.insert(new WorkoutSession("b", GYM, DAY.minusDays(1), null, null, false));
        sets.insert(new WorkoutSet("x", "a", "e1", 1, SetType.WORKING, 5, Unit.KG, 5, 70.0, T0));
        int total = 0;
        for (WorkoutSession s : sessions.findAll()) {
            total += s.getSets().size();
            assertEquals(s.getId().equals("a") ? 1 : 0, s.getSets().size());
        }
        assertEquals(1, total);
        assertEquals(2, sessions.findAll().size());
    }

    @Test
    void secondOpenSessionIsRejectedByIndex() {
        sessions.insert(new WorkoutSession("a", GYM, DAY, T0, null, true));
        assertThrows(IllegalStateException.class,
                () -> sessions.insert(new WorkoutSession("b", GYM, DAY, T0, null, true)));
    }
}
