package myophoria.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import myophoria.core.error.NotFoundException;
import myophoria.core.fake.FakeExerciseRepository;
import myophoria.core.fake.FakeWorkoutStore;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportServiceTest {
    static final double LB = 0.45359237;
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);
    static final Instant T0 = Instant.parse("2026-11-25T07:00:00Z");
    FakeWorkoutStore store;
    ReportService service;
    int n;

    @BeforeEach
    void setUp() {
        store = new FakeWorkoutStore();
        FakeExerciseRepository exercises = new FakeExerciseRepository();
        exercises.insert(Exercise.of(EquipmentType.BARBELL, "squat", "Squat", Laterality.BILATERAL, null));
        exercises.insert(Exercise.of(EquipmentType.DUMBBELL, "curl", "Curl", Laterality.BILATERAL, null));
        service = new ReportService(store.sessions(), exercises);
    }

    private void set(String session, String exercise, SetType type, double weight, Unit unit, int reps, int minute) {
        store.sets().insert(new WorkoutSet("x" + n++, session, exercise, n, type, weight, unit, reps, null,
                T0.plusSeconds(60L * minute)));
    }

    @Test
    void summaryInKilograms() {
        store.sessions().insert(new WorkoutSession("s", "g", DAY, T0, T0.plusSeconds(3600), false));
        set("s", "curl", SetType.WORKING, 10, Unit.KG, 10, 1);     // curl first → listed first
        set("s", "squat", SetType.WARMUP, 60, Unit.KG, 10, 2);
        set("s", "squat", SetType.WORKING, 100, Unit.KG, 5, 3);
        set("s", "squat", SetType.FAILURE, 220.462262, Unit.LB, 3, 4); // ≈ 100 kg
        set("s", "curl", SetType.WORKING, 14, Unit.KG, 8, 5);

        SessionSummary sum = service.summary("s", Unit.KG);
        assertEquals(5, sum.totalSets());
        assertEquals(4, sum.workingSets());
        assertEquals(3600L, sum.durationSeconds());
        assertEquals(100 + 112 + 500 + 220.462262 * LB * 3, sum.volume(), 1e-6);

        List<ExerciseSummary> ex = sum.exercises();
        assertEquals(List.of("curl", "squat"), ex.stream().map(ExerciseSummary::exerciseId).toList());
        ExerciseSummary curl = ex.get(0);
        assertEquals("Curl", curl.name());
        assertEquals(2, curl.workingSets());
        assertEquals(14, curl.topWeight(), 1e-9);
        assertEquals(12, curl.averageWeight(), 1e-9);
        ExerciseSummary squat = ex.get(1);
        assertEquals(2, squat.workingSets()); // warm-up excluded
        assertEquals(100, squat.topWeight(), 1e-4);
        assertEquals(100, squat.averageWeight(), 1e-4);
        assertEquals(500 + 220.462262 * LB * 3, squat.volume(), 1e-6);
    }

    @Test
    void summaryInPounds() {
        store.sessions().insert(new WorkoutSession("s", "g", DAY, T0, null, true));
        set("s", "squat", SetType.WORKING, 100, Unit.KG, 5, 10);
        SessionSummary sum = service.summary("s", Unit.LB);
        assertEquals(Unit.LB, sum.unit());
        assertEquals(500 / LB, sum.volume(), 1e-9);
        assertEquals(100 / LB, sum.exercises().get(0).topWeight(), 1e-9);
        assertEquals(T0.plusSeconds(600), sum.endedAt()); // open: effective end = last set
        assertEquals(600L, sum.durationSeconds());
    }

    @Test
    void summaryWithOnlyWarmupsHasNoTopWeight() {
        store.sessions().insert(new WorkoutSession("s", "g", DAY, null, null, false));
        set("s", "squat", SetType.WARMUP, 60, Unit.KG, 5, 1);
        ExerciseSummary squat = service.summary("s", null).exercises().get(0);
        assertNull(squat.topWeight());
        assertNull(squat.averageWeight());
        assertEquals(0, squat.volume());
        assertNull(service.summary("s", null).durationSeconds());
        assertThrows(NotFoundException.class, () -> service.summary("nope", Unit.KG));
    }

    @Test
    void progressionIsTopWorkingWeightPerSessionByDate() {
        store.sessions().insert(new WorkoutSession("late", "g", DAY, null, null, false));
        store.sessions().insert(new WorkoutSession("early", "g", DAY.minusDays(7), null, null, false));
        store.sessions().insert(new WorkoutSession("none", "g", DAY.minusDays(3), null, null, false));
        set("early", "squat", SetType.WORKING, 90, Unit.KG, 5, 1);
        set("early", "squat", SetType.WARMUP, 120, Unit.KG, 5, 2); // warm-ups never count
        set("late", "squat", SetType.WORKING, 220.462262, Unit.LB, 5, 3);
        set("late", "squat", SetType.FAILURE, 95, Unit.KG, 2, 4);
        set("none", "curl", SetType.WORKING, 10, Unit.KG, 10, 5);

        List<ProgressPoint> points = service.progression("squat", Unit.KG);
        assertEquals(2, points.size());
        assertEquals(new ProgressPoint(DAY.minusDays(7), 90), points.get(0));
        assertEquals(DAY, points.get(1).localDate());
        assertEquals(100, points.get(1).topWeight(), 1e-4);
        assertEquals(90 / LB, service.progression("squat", Unit.LB).get(0).topWeight(), 1e-9);
        assertThrows(NotFoundException.class, () -> service.progression("nope", Unit.KG));
    }
}
