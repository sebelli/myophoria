package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Gym;
import myophoria.core.model.Laterality;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.model.WorkoutTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PermanentDeleteTest {
    static final String GYM = Gym.SEEDED_DEFAULT_ID;
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);
    static final Instant T = Instant.parse("2026-11-25T07:00:00Z");

    @TempDir
    Path dir;

    @Test
    void removesSetsEmptiedClosedSessionsAndTemplateRows() {
        Database db = new Database(dir);
        db.open();
        SqliteExerciseRepository exercises = new SqliteExerciseRepository(db);
        SqliteSessionRepository sessions = new SqliteSessionRepository(db);
        SqliteSetRepository sets = new SqliteSetRepository(db);
        SqliteTemplateRepository templates = new SqliteTemplateRepository(db);
        exercises.insert(Exercise.of(EquipmentType.BARBELL, "gone", "Gone", Laterality.BILATERAL, T));
        exercises.insert(Exercise.of(EquipmentType.BARBELL, "kept", "Kept", Laterality.BILATERAL, null));

        sessions.insert(new WorkoutSession("only-gone", GYM, DAY.minusDays(2), null, null, false));
        sessions.insert(new WorkoutSession("mixed", GYM, DAY.minusDays(1), null, null, false));
        sessions.insert(new WorkoutSession("open", GYM, DAY, T, null, true));
        sets.insert(new WorkoutSet("a", "only-gone", "gone", 1, SetType.WORKING, 50, Unit.KG, 5, null, T));
        sets.insert(new WorkoutSet("b", "mixed", "gone", 1, SetType.WORKING, 50, Unit.KG, 5, null, T));
        sets.insert(new WorkoutSet("c", "mixed", "kept", 1, SetType.WORKING, 50, Unit.KG, 5, null, T));
        sets.insert(new WorkoutSet("d", "open", "gone", 1, SetType.WORKING, 50, Unit.KG, 5, null, T));
        templates.insert(new WorkoutTemplate("t", "Both", List.of("gone", "kept")));

        assertEquals(3, exercises.countSets("gone"));
        assertEquals(3, exercises.countSessions("gone"));
        exercises.deletePermanently("gone");

        assertNull(exercises.findById("gone"));
        assertNull(sessions.findById("only-gone"));                 // closed and emptied
        assertEquals(1, sessions.findById("mixed").getSets().size()); // other exercise's set stays
        assertNotNull(sessions.findById("open"));                     // running workout stays even if empty
        assertEquals(0, sessions.findById("open").getSets().size());
        assertEquals(List.of("kept"), templates.findById("t").getExerciseIds());
        assertNotNull(exercises.findById("kept"));
    }
}
