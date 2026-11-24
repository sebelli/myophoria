package myophoria.core.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import myophoria.core.error.InvalidWorkoutInputException;
import org.junit.jupiter.api.Test;

class WorkoutSessionTest {
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);
    static final Instant T0 = Instant.parse("2026-11-25T07:00:00Z");

    static WorkoutSet set(String id, SetType type, double weight, Unit unit, int reps, Instant at) {
        return new WorkoutSet(id, "s1", "e1", 1, type, weight, unit, reps, null, at);
    }

    @Test
    void setVolumeSkipsWarmupsAndConverts() {
        assertEquals(0, set("a", SetType.WARMUP, 60, Unit.KG, 10, T0).volume(Unit.KG));
        assertEquals(1000, set("b", SetType.WORKING, 100, Unit.KG, 10, T0).volume(Unit.KG), 1e-9);
        assertEquals(500, set("c", SetType.FAILURE, 100, Unit.KG, 5, T0).volume(Unit.KG), 1e-9);
        assertEquals(10 * 100 * 0.45359237, set("d", SetType.WORKING, 100, Unit.LB, 10, T0).volume(Unit.KG), 1e-9);
        // calisthenics: weight is the added weight; the bodyweight snapshot never counts
        WorkoutSet pullUp = new WorkoutSet("e", "s1", "e1", 1, SetType.WORKING, 10, Unit.KG, 8, 70.0, T0);
        assertEquals(80, pullUp.volume(Unit.KG), 1e-9);
        WorkoutSet bodyOnly = new WorkoutSet("f", "s1", "e1", 1, SetType.WORKING, 0, Unit.KG, 8, 70.0, T0);
        assertEquals(0, bodyOnly.volume(Unit.KG));
    }

    @Test
    void setValidation() {
        assertThrows(InvalidWorkoutInputException.class, () -> set("a", SetType.WORKING, -1, Unit.KG, 5, T0));
        assertThrows(InvalidWorkoutInputException.class, () -> set("a", SetType.WORKING, 10, Unit.KG, 0, T0));
        assertThrows(InvalidWorkoutInputException.class, () -> set("a", null, 10, Unit.KG, 5, T0));
        assertThrows(InvalidWorkoutInputException.class, () -> set("a", SetType.WORKING, 10, null, 5, T0));
        assertThrows(InvalidWorkoutInputException.class, () -> set("a", SetType.WORKING, 10, Unit.KG, 5, null));
        assertThrows(InvalidWorkoutInputException.class,
                () -> new WorkoutSet("a", "s1", "e1", 0, SetType.WORKING, 10, Unit.KG, 5, null, T0));
        assertThrows(InvalidWorkoutInputException.class,
                () -> new WorkoutSet("a", "s1", "e1", 1, SetType.WORKING, 10, Unit.KG, 5, 0.0, T0));
    }

    @Test
    void sessionVolumeAndDuration() {
        WorkoutSession s = new WorkoutSession("s1", "g1", DAY, T0, null, true);
        assertNull(s.effectiveEnd());
        assertNull(s.duration());
        s.addSet(set("a", SetType.WARMUP, 60, Unit.KG, 10, T0.plusSeconds(60)));
        s.addSet(set("b", SetType.WORKING, 100, Unit.KG, 5, T0.plusSeconds(600)));
        assertEquals(500, s.volume(Unit.KG), 1e-9);
        assertEquals(T0.plusSeconds(600), s.effectiveEnd()); // never finished: last set's time
        assertEquals(Duration.ofMinutes(10), s.duration());

        assertThrows(InvalidWorkoutInputException.class, () -> s.finish(T0.minusSeconds(1)));
        assertTrue(s.isOpen()); // a rejected finish changes nothing
        s.finish(T0.plusSeconds(3600));
        assertFalse(s.isOpen());
        assertEquals(Duration.ofHours(1), s.duration());
        assertThrows(InvalidWorkoutInputException.class, () -> s.finish(T0.plusSeconds(4000)));
    }

    @Test
    void backfillTimesAreOptionalButOrdered() {
        WorkoutSession noTimes = new WorkoutSession("s1", "g1", DAY, null, null, false);
        assertNull(noTimes.duration());
        WorkoutSession endOnly = new WorkoutSession("s2", "g1", DAY, null, T0, false);
        assertNull(endOnly.duration());
        assertThrows(InvalidWorkoutInputException.class,
                () -> new WorkoutSession("s3", "g1", DAY, T0, T0.minusSeconds(1), false));
        assertThrows(InvalidWorkoutInputException.class,
                () -> new WorkoutSession("s4", "g1", DAY, T0, T0.plusSeconds(1), true));
        assertThrows(IllegalArgumentException.class,
                () -> noTimes.addSet(new WorkoutSet("x", "other", "e1", 1, SetType.WORKING, 1, Unit.KG, 1, null, T0)));
    }
}
