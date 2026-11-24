package myophoria.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import myophoria.core.error.DuplicateResourceException;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.fake.FakeExerciseRepository;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExerciseServiceTest {
    static final Instant NOW = Instant.parse("2026-11-25T10:00:00.123456Z");
    FakeExerciseRepository repo;
    ExerciseService service;

    @BeforeEach
    void setUp() {
        repo = new FakeExerciseRepository();
        service = new ExerciseService(repo, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Exercise add(String name, EquipmentType t) {
        return service.create(name, t, Laterality.BILATERAL);
    }

    @Test
    void createStoresAndGetFinds() {
        Exercise e = add("Bench Press", EquipmentType.BARBELL);
        assertEquals("Bench Press", service.get(e.getId()).getName());
        assertEquals(EquipmentType.BARBELL, e.type());
        assertThrows(NotFoundException.class, () -> service.get("nope"));
    }

    @Test
    void createRejectsBlankNameAndNullType() {
        assertThrows(InvalidWorkoutInputException.class, () -> add(" ", EquipmentType.BARBELL));
        assertThrows(InvalidWorkoutInputException.class, () -> add("X", null));
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void duplicateActiveNameIsRejectedCaseInsensitively() {
        Exercise first = add("Squat", EquipmentType.BARBELL);
        DuplicateResourceException ex =
                assertThrows(DuplicateResourceException.class, () -> add("  SQUAT ", EquipmentType.STACK));
        assertEquals(first.getId(), ex.getExistingId());
        assertFalse(ex.isSoftDeleted());
        assertEquals("exercise", ex.getResourceType());
    }

    @Test
    void duplicateOfSoftDeletedNameReportsSoftDeleted() {
        Exercise old = add("Squat", EquipmentType.BARBELL);
        service.softDelete(old.getId());
        DuplicateResourceException ex =
                assertThrows(DuplicateResourceException.class, () -> add("squat", EquipmentType.BARBELL));
        assertEquals(old.getId(), ex.getExistingId());
        assertTrue(ex.isSoftDeleted());
    }

    @Test
    void updateRenamesAndKeepsOwnNameAllowed() {
        Exercise e = add("Row", EquipmentType.STACK);
        Exercise same = service.update(e.getId(), "Row", EquipmentType.STACK, Laterality.UNILATERAL);
        assertEquals(Laterality.UNILATERAL, same.getLaterality());
        Exercise renamed = service.update(e.getId(), "Cable Row", EquipmentType.STACK, Laterality.UNILATERAL);
        assertEquals("cable row", renamed.getNameKey());
    }

    @Test
    void updateCannotTakeAnotherActiveName() {
        add("Squat", EquipmentType.BARBELL);
        Exercise row = add("Row", EquipmentType.STACK);
        assertThrows(DuplicateResourceException.class,
                () -> service.update(row.getId(), "squat", EquipmentType.STACK, Laterality.BILATERAL));
    }

    @Test
    void typeChangeOnlyWithoutSets() {
        Exercise e = add("Press", EquipmentType.BARBELL);
        Exercise changed = service.update(e.getId(), "Press", EquipmentType.DUMBBELL, Laterality.BILATERAL);
        assertEquals(EquipmentType.DUMBBELL, changed.type());
        assertEquals(e.getId(), changed.getId());

        repo.setCount(e.getId(), 3);
        assertThrows(InvalidWorkoutInputException.class,
                () -> service.update(e.getId(), "Press", EquipmentType.STACK, Laterality.BILATERAL));
        // renaming with the same type is still fine with sets
        assertDoesNotThrow(() -> service.update(e.getId(), "Press 2", EquipmentType.DUMBBELL, Laterality.BILATERAL));
    }

    @Test
    void unchangedNameDoesNotClashWithDeletedTwin() {
        Exercise active = add("Squat", EquipmentType.BARBELL);
        repo.insert(Exercise.of(EquipmentType.BARBELL, "twin", "Squat", Laterality.BILATERAL, NOW)); // e.g. from import
        Exercise edited = service.update(active.getId(), "Squat", EquipmentType.BARBELL, Laterality.UNILATERAL);
        assertEquals(Laterality.UNILATERAL, edited.getLaterality());
    }

    @Test
    void updateUnknownIdIsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.update("nope", "X", EquipmentType.STACK, Laterality.BILATERAL));
    }

    @Test
    void softDeleteTruncatesToMillisAndHidesFromActiveList() {
        Exercise e = add("Squat", EquipmentType.BARBELL);
        service.softDelete(e.getId());
        assertEquals(Instant.parse("2026-11-25T10:00:00.123Z"), service.get(e.getId()).getDeletedAt());
        assertTrue(service.search(null, null, false).isEmpty());
        assertEquals(1, service.search(null, null, true).size());
        assertDoesNotThrow(() -> service.softDelete(e.getId())); // idempotent
    }

    @Test
    void restoreWorksUnlessNameTaken() {
        Exercise old = add("Squat", EquipmentType.BARBELL);
        service.softDelete(old.getId());
        service.restore(old.getId());
        assertFalse(service.get(old.getId()).isDeleted());

        // an active exercise with the same name can only appear by bypassing the service (e.g. import)
        service.softDelete(old.getId());
        Exercise intruder = Exercise.of(EquipmentType.STACK, "intruder", "squat", Laterality.BILATERAL, null);
        repo.insert(intruder);
        DuplicateResourceException ex =
                assertThrows(DuplicateResourceException.class, () -> service.restore(old.getId()));
        assertEquals("intruder", ex.getExistingId());
        assertFalse(ex.isSoftDeleted());
        assertTrue(service.get(old.getId()).isDeleted());
    }

    @Test
    void searchFiltersByTextTypeAndSortsByName() {
        add("Incline Press", EquipmentType.DUMBBELL);
        add("Bench Press", EquipmentType.BARBELL);
        add("Curl", EquipmentType.DUMBBELL);
        List<String> all = service.search(null, null, false).stream().map(Exercise::getName).toList();
        assertEquals(List.of("Bench Press", "Curl", "Incline Press"), all);
        List<String> press = service.search("PRESS", null, false).stream().map(Exercise::getName).toList();
        assertEquals(List.of("Bench Press", "Incline Press"), press);
        List<String> dumbbells = service.search("press", EnumSet.of(EquipmentType.DUMBBELL), false)
                .stream().map(Exercise::getName).toList();
        assertEquals(List.of("Incline Press"), dumbbells);
        assertTrue(service.search("zzz", null, false).isEmpty());
    }
}
