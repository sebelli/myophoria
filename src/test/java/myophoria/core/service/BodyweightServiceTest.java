package myophoria.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.fake.FakeBodyweightRepository;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.Unit;
import org.junit.jupiter.api.Test;

class BodyweightServiceTest {
    final BodyweightService service = new BodyweightService(new FakeBodyweightRepository());
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);

    @Test
    void latestIsNewestByRecordedAt() {
        assertNull(service.latest());
        service.create(70, Unit.KG, Instant.parse("2026-11-25T08:00:00Z"), DAY);
        BodyweightEntry newer = service.create(71, Unit.KG, Instant.parse("2026-11-25T09:00:00Z"), DAY);
        service.create(69, Unit.KG, Instant.parse("2026-11-24T09:00:00Z"), DAY.minusDays(1));
        assertEquals(newer.getId(), service.latest().getId());
        assertEquals(3, service.list().size());
        assertEquals(newer.getId(), service.list().get(0).getId());
    }

    @Test
    void createTruncatesToMillisAndValidates() {
        BodyweightEntry e = service.create(70, Unit.LB, Instant.parse("2026-11-25T08:00:00.123456Z"), DAY);
        assertEquals(Instant.parse("2026-11-25T08:00:00.123Z"), e.getRecordedAt());
        assertThrows(InvalidWorkoutInputException.class, () -> service.create(0, Unit.KG, Instant.EPOCH, DAY));
        assertThrows(InvalidWorkoutInputException.class, () -> service.create(70, null, Instant.EPOCH, DAY));
        assertThrows(InvalidWorkoutInputException.class, () -> service.create(70, Unit.KG, null, DAY));
        assertThrows(InvalidWorkoutInputException.class, () -> service.create(70, Unit.KG, Instant.EPOCH, null));
    }

    @Test
    void updateAndDelete() {
        BodyweightEntry e = service.create(70, Unit.KG, Instant.EPOCH, DAY);
        service.update(e.getId(), 72, Unit.KG, Instant.EPOCH, DAY);
        assertEquals(72, service.latest().getWeight());
        service.delete(e.getId());
        assertNull(service.latest());
        assertThrows(NotFoundException.class, () -> service.delete(e.getId()));
        assertThrows(NotFoundException.class, () -> service.update("x", 70, Unit.KG, Instant.EPOCH, DAY));
    }
}
