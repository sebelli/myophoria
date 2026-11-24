package myophoria.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.fake.FakeGymRepository;
import myophoria.core.fake.FakeWorkoutStore;
import myophoria.core.model.Gym;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistoryServiceTest {
    static final String GYM = Gym.SEEDED_DEFAULT_ID;
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);
    static final Instant T0 = Instant.parse("2026-11-25T07:00:00Z");
    FakeWorkoutStore store;
    FakeGymRepository gyms;
    HistoryService service;

    @BeforeEach
    void setUp() {
        store = new FakeWorkoutStore();
        gyms = new FakeGymRepository();
        gyms.insert(new Gym("away", "Away", false, null));
        service = new HistoryService(store.sessions(), gyms);
    }

    private void session(String id, String gym, LocalDate day, Instant start, String exercise) {
        store.sessions().insert(new WorkoutSession(id, gym, day, start, null, false));
        if (exercise != null) {
            store.sets().insert(new WorkoutSet(id + "-set", id, exercise, 1, SetType.WORKING, 50, Unit.KG, 5, null, T0));
        }
    }

    private static List<String> ids(List<SessionHeader> headers) {
        return headers.stream().map(SessionHeader::id).toList();
    }

    @Test
    void listIsNewestFirstWithNamesAndCounts() {
        session("old", GYM, DAY.minusDays(3), null, "squat");
        session("morning", GYM, DAY, T0, null);
        session("evening", "away", DAY, T0.plusSeconds(36000), "row");
        List<SessionHeader> all = service.list(new HistoryFilter(null, null, null, null));
        assertEquals(List.of("evening", "morning", "old"), ids(all));
        assertEquals("Away", all.get(0).gymName());
        assertEquals(1, all.get(0).setCount());
        assertEquals(0, all.get(1).setCount());
    }

    @Test
    void filtersCombine() {
        session("a", GYM, DAY.minusDays(10), null, "squat");
        session("b", GYM, DAY.minusDays(5), null, "row");
        session("c", "away", DAY, null, "squat");
        assertEquals(List.of("c", "a"), ids(service.list(new HistoryFilter(null, null, null, "squat"))));
        assertEquals(List.of("b", "a"), ids(service.list(new HistoryFilter(null, null, GYM, null))));
        assertEquals(List.of("c", "b"), ids(service.list(new HistoryFilter(DAY.minusDays(5), DAY, null, null))));
        assertEquals(List.of("b"), ids(service.list(new HistoryFilter(DAY.minusDays(5), DAY.minusDays(5), null, null))));
        assertTrue(service.list(new HistoryFilter(null, null, "away", "row")).isEmpty());
    }

    @Test
    void backfillIsClosedWithOptionalTimes() {
        WorkoutSession s = service.backfill(GYM, DAY, null, null);
        assertFalse(s.isOpen());
        assertNull(s.duration());
        WorkoutSession timed = service.backfill(GYM, DAY, T0, T0.plusSeconds(3600).plusNanos(5000));
        assertEquals(T0.plusSeconds(3600), store.sessions().findById(timed.getId()).getEndedAt());
        assertThrows(InvalidWorkoutInputException.class, () -> service.backfill(GYM, DAY, T0, T0.minusSeconds(1)));
        assertThrows(NotFoundException.class, () -> service.backfill("nope", DAY, null, null));
        assertThrows(InvalidWorkoutInputException.class, () -> service.backfill(GYM, null, null, null));
    }

    @Test
    void editAndDelete() {
        WorkoutSession s = service.backfill(GYM, DAY, null, null);
        service.edit(s.getId(), "away", DAY.minusDays(1), T0, T0.plusSeconds(60));
        WorkoutSession stored = service.get(s.getId());
        assertEquals("away", stored.getGymId());
        assertEquals(DAY.minusDays(1), stored.getLocalDate());
        service.delete(s.getId());
        assertThrows(NotFoundException.class, () -> service.get(s.getId()));
        assertThrows(NotFoundException.class, () -> service.delete(s.getId()));
    }

    @Test
    void openWorkoutCannotGetAnEndTimeFromHistory() {
        store.sessions().insert(new WorkoutSession("live", GYM, DAY, T0, null, true));
        assertThrows(InvalidWorkoutInputException.class, () -> service.edit("live", GYM, DAY, T0, T0.plusSeconds(60)));
        assertDoesNotThrow(() -> service.edit("live", GYM, DAY, T0.minusSeconds(60), null));
    }
}
