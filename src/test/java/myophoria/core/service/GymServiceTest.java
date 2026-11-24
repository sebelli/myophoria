package myophoria.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import myophoria.core.error.DuplicateResourceException;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.fake.FakeGymRepository;
import myophoria.core.fake.FakeWorkoutStore;
import myophoria.core.model.Gym;
import myophoria.core.model.WorkoutSession;
import org.junit.jupiter.api.Test;

class GymServiceTest {
    final FakeGymRepository gyms = new FakeGymRepository();
    final FakeWorkoutStore store = new FakeWorkoutStore();
    final GymService service =
            new GymService(gyms, store.sessions(), Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void createListAndDuplicate() {
        Gym home = service.create("  Home   Gym ");
        assertEquals("Home Gym", home.getName());
        assertEquals(2, service.list().size());
        assertEquals("Default", service.list().get(0).getName()); // sorted by name
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.create("home gym"));
        assertEquals(home.getId(), ex.getExistingId());
        assertThrows(InvalidWorkoutInputException.class, () -> service.create(" "));
    }

    @Test
    void deletedNamesCanBeReused() {
        Gym old = service.create("Garage");
        service.delete(old.getId());
        assertDoesNotThrow(() -> service.create("Garage"));
    }

    @Test
    void renameChecksOtherActiveNames() {
        Gym a = service.create("A");
        service.create("B");
        assertThrows(DuplicateResourceException.class, () -> service.rename(a.getId(), "b"));
        assertEquals("A2", service.rename(a.getId(), "A2").getName());
        assertEquals("a", service.rename(a.getId(), "a").getNameKey());
    }

    @Test
    void makeDefaultMovesTheFlag() {
        Gym home = service.create("Home");
        service.makeDefault(home.getId());
        assertTrue(service.get(home.getId()).isDefault());
        assertFalse(service.get(Gym.SEEDED_DEFAULT_ID).isDefault());
    }

    @Test
    void deleteRules() {
        assertThrows(InvalidWorkoutInputException.class, () -> service.delete(Gym.SEEDED_DEFAULT_ID)); // default
        Gym home = service.create("Home");
        service.makeDefault(home.getId());
        Gym other = service.create("Other");
        service.delete(Gym.SEEDED_DEFAULT_ID); // no longer default
        service.delete(other.getId());
        assertEquals(1, service.list().size());
        assertThrows(InvalidWorkoutInputException.class, () -> service.delete(home.getId()));
        assertThrows(NotFoundException.class, () -> service.delete(other.getId())); // already deleted
        assertThrows(NotFoundException.class, () -> service.delete("nope"));
    }

    @Test
    void activeGymFollowsLatestSessionElseDefault() {
        assertEquals(Gym.SEEDED_DEFAULT_ID, service.activeGym().getId());
        Gym home = service.create("Home");
        Gym away = service.create("Away");
        store.sessions().insert(new WorkoutSession("s1", home.getId(), LocalDate.of(2026, 11, 20), null, null, false));
        store.sessions().insert(new WorkoutSession("s2", away.getId(), LocalDate.of(2026, 11, 18), null, null, false));
        assertEquals(home.getId(), service.activeGym().getId());
        service.delete(home.getId());
        assertEquals(Gym.SEEDED_DEFAULT_ID, service.activeGym().getId()); // latest gym deleted → default
    }
}
