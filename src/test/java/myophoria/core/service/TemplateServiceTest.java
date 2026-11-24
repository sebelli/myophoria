package myophoria.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.fake.FakeExerciseRepository;
import myophoria.core.fake.FakeTemplateRepository;
import myophoria.core.fake.FakeWorkoutStore;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.model.WorkoutTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateServiceTest {
    static final Instant T0 = Instant.parse("2026-11-25T07:00:00Z");
    FakeExerciseRepository exercises;
    FakeWorkoutStore store;
    TemplateService service;

    @BeforeEach
    void setUp() {
        exercises = new FakeExerciseRepository();
        store = new FakeWorkoutStore();
        for (String id : List.of("squat", "bench", "row")) {
            exercises.insert(Exercise.of(EquipmentType.BARBELL, id, id, Laterality.BILATERAL, null));
        }
        service = new TemplateService(new FakeTemplateRepository(), exercises, store.sessions());
    }

    private void softDelete(String id) {
        Exercise e = exercises.findById(id);
        e.softDelete(T0);
        exercises.update(e);
    }

    @Test
    void createListUpdateDelete() {
        WorkoutTemplate legs = service.create(" Legs ", List.of("squat", "row"));
        service.create("arms", List.of("bench"));
        assertEquals(List.of("arms", "Legs"), service.list().stream().map(WorkoutTemplate::getName).toList());
        service.update(legs.getId(), "Legs", List.of("row", "squat", "bench"));
        assertEquals(List.of("row", "squat", "bench"), service.list().get(1).getExerciseIds());
        service.delete(legs.getId());
        assertEquals(1, service.list().size());
        assertThrows(NotFoundException.class, () -> service.delete(legs.getId()));
    }

    @Test
    void validation() {
        assertThrows(InvalidWorkoutInputException.class, () -> service.create(" ", List.of("squat")));
        assertThrows(InvalidWorkoutInputException.class, () -> service.create("x", List.of()));
        assertThrows(InvalidWorkoutInputException.class, () -> service.create("x", null));
        assertThrows(InvalidWorkoutInputException.class, () -> service.create("x", List.of("squat", "squat")));
        assertThrows(NotFoundException.class, () -> service.create("x", List.of("nope")));
        softDelete("row");
        assertThrows(InvalidWorkoutInputException.class, () -> service.create("x", List.of("row")));
    }

    @Test
    void softDeletedExercisesAreHiddenAndComeBack() {
        WorkoutTemplate t = service.create("Full", List.of("squat", "bench", "row"));
        softDelete("bench");
        assertEquals(List.of("squat", "row"), service.list().get(0).getExerciseIds());
        Exercise bench = exercises.findById("bench");
        bench.restore();
        exercises.update(bench);
        assertEquals(List.of("squat", "bench", "row"), service.list().get(0).getExerciseIds());
        assertEquals(t.getId(), service.list().get(0).getId());
    }

    @Test
    void saveFromSessionUsesFirstSetOrder() {
        store.sessions().insert(new WorkoutSession("s", "g", LocalDate.of(2026, 11, 25), T0, null, false));
        String[] order = {"bench", "squat", "bench", "row", "squat"};
        for (int i = 0; i < order.length; i++) {
            store.sets().insert(new WorkoutSet("x" + i, "s", order[i], 1, SetType.WORKING, 50, Unit.KG, 5, null,
                    T0.plusSeconds(60L * i)));
        }
        softDelete("row");
        WorkoutTemplate t = service.saveFromSession("s", "Push");
        assertEquals(List.of("bench", "squat"), t.getExerciseIds());
        assertThrows(NotFoundException.class, () -> service.saveFromSession("nope", "x"));
    }
}
