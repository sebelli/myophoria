package myophoria.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import myophoria.core.model.WorkoutTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteTemplateRepositoryTest {
    @TempDir
    Path dir;

    @Test
    void orderIsKeptAndUpdateReplacesRows() {
        Database db = new Database(dir);
        db.open();
        SqliteExerciseRepository exercises = new SqliteExerciseRepository(db);
        for (String id : List.of("a", "b", "c")) {
            exercises.insert(Exercise.of(EquipmentType.BARBELL, id, id, Laterality.BILATERAL, null));
        }
        SqliteTemplateRepository repo = new SqliteTemplateRepository(db);
        repo.insert(new WorkoutTemplate("t1", "Legs", List.of("c", "a")));
        repo.insert(new WorkoutTemplate("t2", "Empty", List.of()));
        assertEquals(List.of("c", "a"), repo.findById("t1").getExerciseIds());
        assertEquals(2, repo.findAll().size());

        repo.update(new WorkoutTemplate("t1", "Legs 2", List.of("b", "c", "a")));
        WorkoutTemplate loaded = repo.findById("t1");
        assertEquals("Legs 2", loaded.getName());
        assertEquals(List.of("b", "c", "a"), loaded.getExerciseIds());

        // an unknown exercise fails the foreign key and rolls the whole update back
        assertThrows(IllegalStateException.class, () -> repo.update(new WorkoutTemplate("t1", "Broken", List.of("zzz"))));
        assertEquals("Legs 2", repo.findById("t1").getName());
        assertEquals(3, repo.findById("t1").getExerciseIds().size());

        repo.delete("t1");
        assertNull(repo.findById("t1"));
    }
}
