package myophoria.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.fake.FakeBodyweightRepository;
import myophoria.core.fake.FakeExerciseRepository;
import myophoria.core.fake.FakeGymRepository;
import myophoria.core.fake.FakeWorkoutStore;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Gym;
import myophoria.core.model.Laterality;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkoutServiceTest {
    static final String GYM = Gym.SEEDED_DEFAULT_ID;
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);

    /** A clock the test can move forward, so sets get increasing completion times. */
    static final class StepClock extends Clock {
        Instant now = Instant.parse("2026-11-25T07:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            now = now.plusSeconds(60);
            return now;
        }
    }

    FakeWorkoutStore store;
    FakeExerciseRepository exercises;
    FakeGymRepository gyms;
    FakeBodyweightRepository bodyweights;
    WorkoutService service;
    Exercise squat;
    Exercise pullUp;

    @BeforeEach
    void setUp() {
        store = new FakeWorkoutStore();
        exercises = new FakeExerciseRepository();
        gyms = new FakeGymRepository();
        bodyweights = new FakeBodyweightRepository();
        service = new WorkoutService(store.sessions(), store.sets(), exercises, gyms, bodyweights, new StepClock());
        squat = Exercise.of(EquipmentType.BARBELL, "squat", "Squat", Laterality.BILATERAL, null);
        pullUp = Exercise.of(EquipmentType.CALISTHENICS, "pull", "Pull Up", Laterality.BILATERAL, null);
        exercises.insert(squat);
        exercises.insert(pullUp);
    }

    private static SetInput in(String ex, SetType type, double weight, Unit unit, int reps, Double bw, int pos) {
        return new SetInput(ex, type, weight, unit, reps, bw, pos);
    }

    /** Runs one finished workout at a gym with the given squat sets. */
    private WorkoutSession pastWorkout(String gym, LocalDate day, SetInput... sets) {
        WorkoutSession s = service.start(gym, day);
        for (SetInput i : sets) {
            service.addSet(s.getId(), i);
        }
        return service.finish(s.getId());
    }

    @Test
    void startRules() {
        WorkoutSession s = service.start(GYM, DAY);
        assertTrue(s.isOpen());
        assertNotNull(s.getStartedAt());
        assertEquals(s.getId(), service.open().getId());
        InvalidWorkoutInputException ex = assertThrows(InvalidWorkoutInputException.class, () -> service.start(GYM, DAY));
        assertEquals("A workout is already open", ex.getMessage());
    }

    @Test
    void startNeedsAnActiveGym() {
        assertThrows(NotFoundException.class, () -> service.start("nope", DAY));
        Gym gone = new Gym("gone", "Gone", false, Instant.EPOCH);
        gyms.insert(gone);
        assertThrows(NotFoundException.class, () -> service.start("gone", DAY));
        assertThrows(InvalidWorkoutInputException.class, () -> service.start(GYM, null));
    }

    @Test
    void finishNeedsSets() {
        WorkoutSession s = service.start(GYM, DAY);
        assertThrows(InvalidWorkoutInputException.class, () -> service.finish(s.getId()));
        service.addSet(s.getId(), in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 1));
        WorkoutSession done = service.finish(s.getId());
        assertFalse(done.isOpen());
        assertNull(service.open());
        assertThrows(InvalidWorkoutInputException.class, () -> service.finish(s.getId()));
    }

    @Test
    void setRulesDependOnExercise() {
        String id = service.start(GYM, DAY).getId();
        assertThrows(InvalidWorkoutInputException.class,
                () -> service.addSet(id, in("squat", SetType.WORKING, 0, Unit.KG, 5, null, 1)));
        assertThrows(InvalidWorkoutInputException.class,
                () -> service.addSet(id, in("squat", SetType.WORKING, 100, Unit.KG, 5, 70.0, 1)));
        assertThrows(InvalidWorkoutInputException.class,
                () -> service.addSet(id, in("pull", SetType.WORKING, 0, Unit.KG, 8, null, 1)));
        assertThrows(InvalidWorkoutInputException.class,
                () -> service.addSet(id, in("squat", SetType.WORKING, 100, Unit.KG, 0, null, 1)));
        assertThrows(NotFoundException.class,
                () -> service.addSet(id, in("nope", SetType.WORKING, 100, Unit.KG, 5, null, 1)));
        assertThrows(NotFoundException.class,
                () -> service.addSet("nope", in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 1)));
        WorkoutSet ok = service.addSet(id, in("pull", SetType.WORKING, 0, Unit.KG, 8, 70.0, 1));
        assertEquals(70.0, ok.getBodyweight());

        squat.softDelete(Instant.EPOCH);
        exercises.update(squat);
        assertThrows(InvalidWorkoutInputException.class,
                () -> service.addSet(id, in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 1)));
    }

    @Test
    void updateKeepsCompletionTime() {
        String id = service.start(GYM, DAY).getId();
        WorkoutSet set = service.addSet(id, in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 1));
        WorkoutSet edited = service.updateSet(set.getId(), in("squat", SetType.FAILURE, 105, Unit.KG, 3, null, 1));
        assertEquals(set.getCompletedAt(), edited.getCompletedAt());
        WorkoutSet stored = store.sets().findById(set.getId());
        assertEquals(105, stored.getWeight());
        assertEquals(SetType.FAILURE, stored.getSetType());
        assertThrows(NotFoundException.class,
                () -> service.updateSet("nope", in("squat", SetType.WORKING, 1, Unit.KG, 1, null, 1)));
    }

    @Test
    void deletingLastSetKeepsOpenSessionButRemovesClosedOne() {
        String open = service.start(GYM, DAY).getId();
        WorkoutSet a = service.addSet(open, in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 1));
        service.deleteSet(a.getId());
        assertNotNull(store.sessions().findById(open)); // G6: running workout stays

        WorkoutSet b = service.addSet(open, in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 1));
        WorkoutSet c = service.addSet(open, in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 2));
        service.finish(open);
        service.deleteSet(b.getId());
        assertNotNull(store.sessions().findById(open));
        service.deleteSet(c.getId());
        assertNull(store.sessions().findById(open)); // closed and empty → cleaned up
        assertThrows(NotFoundException.class, () -> service.deleteSet(c.getId()));
    }

    @Test
    void prefillWithoutHistory() {
        String id = service.start(GYM, DAY).getId();
        Prefill p = service.prefill(id, "squat");
        assertEquals(Unit.KG, p.unit());
        assertTrue(p.rows().isEmpty());
        assertNull(p.warmupWeight());
        assertNull(p.bodyweight());
        assertThrows(NotFoundException.class, () -> service.prefill(id, "nope"));
    }

    @Test
    void prefillCopiesLastSessionAtThisGym() {
        pastWorkout(GYM, DAY.minusDays(7),
                in("squat", SetType.WORKING, 90, Unit.KG, 5, null, 1));
        pastWorkout(GYM, DAY.minusDays(2),
                in("squat", SetType.WARMUP, 60, Unit.KG, 10, null, 1),
                in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 2),
                in("squat", SetType.FAILURE, 110, Unit.KG, 3, null, 3));
        Gym away = new Gym("away", "Away", false, null);
        gyms.insert(away);
        pastWorkout("away", DAY.minusDays(1), in("squat", SetType.WORKING, 200, Unit.KG, 1, null, 1));

        String current = service.start(GYM, DAY).getId();
        Prefill p = service.prefill(current, "squat");
        assertEquals(Unit.KG, p.unit());
        assertEquals(List.of(
                new PrefillRow(SetType.WARMUP, 60, 10),
                new PrefillRow(SetType.WORKING, 100, 5),
                new PrefillRow(SetType.FAILURE, 110, 3)), p.rows());
        assertEquals(88, p.warmupWeight(), 1e-9); // 80% of the heaviest working set (failure counts as working)
    }

    @Test
    void prefillUsesTheRememberedUnitAndConverts() {
        pastWorkout(GYM, DAY.minusDays(3), in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 1));
        String current = service.start(GYM, DAY).getId();
        service.addSet(current, in("squat", SetType.WORKING, 225, Unit.LB, 5, null, 1)); // switched to lb today
        Prefill p = service.prefill(current, "squat");
        assertEquals(Unit.LB, p.unit());
        assertEquals(100 / 0.45359237, p.rows().get(0).weight(), 1e-9);
        assertEquals(0.8 * 100 / 0.45359237, p.warmupWeight(), 1e-9);
    }

    @Test
    void prefillAddsBodyweightOnlyForCalisthenics() {
        bodyweights.insert(new BodyweightEntry("b1", 150, Unit.LB, Instant.parse("2026-11-20T07:00:00Z"), DAY));
        bodyweights.insert(new BodyweightEntry("b2", 70, Unit.KG, Instant.parse("2026-11-24T07:00:00Z"), DAY));
        String current = service.start(GYM, DAY).getId();
        assertEquals(70.0, service.prefill(current, "pull").bodyweight(), 1e-9);
        assertNull(service.prefill(current, "squat").bodyweight());

        service.addSet(current, in("pull", SetType.WORKING, 10, Unit.LB, 8, 154.0, 1));
        assertEquals(70 / 0.45359237, service.prefill(current, "pull").bodyweight(), 1e-9); // in the set unit
    }

    @Test
    void durationOfAFinishedWorkout() {
        WorkoutSession s = pastWorkout(GYM, DAY, in("squat", SetType.WORKING, 100, Unit.KG, 5, null, 1));
        assertEquals(Duration.ofMinutes(2), s.duration()); // StepClock: start, set, finish one minute apart
    }
}
