package myophoria.core.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import myophoria.core.error.InvalidWorkoutInputException;
import org.junit.jupiter.api.Test;

class ExerciseTest {
    private static Exercise make(EquipmentType t, String name) {
        return Exercise.of(t, "id1", name, Laterality.BILATERAL, null);
    }

    @Test
    void ofPicksSubclassAndType() {
        assertInstanceOf(BarbellExercise.class, make(EquipmentType.BARBELL, "Squat"));
        assertInstanceOf(DumbbellExercise.class, make(EquipmentType.DUMBBELL, "Curl"));
        assertInstanceOf(StackExercise.class, make(EquipmentType.STACK, "Row"));
        assertInstanceOf(PlateLoadedExercise.class, make(EquipmentType.PLATE_LOADED, "Press"));
        assertInstanceOf(CalisthenicsExercise.class, make(EquipmentType.CALISTHENICS, "Pull Up"));
        for (EquipmentType t : EquipmentType.values()) {
            assertEquals(t, make(t, "X Y").type());
        }
    }

    @Test
    void validationRejectsBadInput() {
        assertThrows(InvalidWorkoutInputException.class, () -> make(EquipmentType.BARBELL, "  "));
        assertThrows(InvalidWorkoutInputException.class, () -> make(EquipmentType.BARBELL, null));
        assertThrows(InvalidWorkoutInputException.class,
                () -> Exercise.of(EquipmentType.BARBELL, "id", "Squat", null, null));
        assertThrows(InvalidWorkoutInputException.class,
                () -> Exercise.of(EquipmentType.BARBELL, " ", "Squat", Laterality.BILATERAL, null));
        assertThrows(InvalidWorkoutInputException.class,
                () -> Exercise.of(null, "id", "Squat", Laterality.BILATERAL, null));
    }

    @Test
    void nameKeyAndName() {
        assertEquals("bench press", Exercise.nameKey("  Bench   PRESS "));
        assertEquals(Exercise.nameKey("Café"), Exercise.nameKey("Café"));
        Exercise e = make(EquipmentType.BARBELL, "  Bench   Press ");
        assertEquals("Bench Press", e.getName());
        assertEquals("bench press", e.getNameKey());
        e.setName("Row");
        assertEquals("row", e.getNameKey());
    }

    @Test
    void initials() {
        assertEquals("IDP", make(EquipmentType.DUMBBELL, "Incline Dumbbell Press").initials());
        assertEquals("SQ", make(EquipmentType.BARBELL, "Squat").initials());
        assertEquals("X", make(EquipmentType.BARBELL, "x").initials());
    }

    @Test
    void weightRulesAndLabels() {
        Exercise bar = make(EquipmentType.BARBELL, "Squat");
        assertThrows(InvalidWorkoutInputException.class, () -> bar.validateWeight(0));
        assertThrows(InvalidWorkoutInputException.class, () -> bar.validateWeight(-1));
        assertThrows(InvalidWorkoutInputException.class, () -> bar.validateWeight(Double.NaN));
        assertDoesNotThrow(() -> bar.validateWeight(100));
        assertFalse(bar.requiresBodyweight());
        assertEquals("total", bar.weightLabel());
        assertEquals("per dumbbell", make(EquipmentType.DUMBBELL, "Curl").weightLabel());
        assertEquals("stack", make(EquipmentType.STACK, "Row").weightLabel());
        assertEquals("plates", make(EquipmentType.PLATE_LOADED, "Press").weightLabel());

        Exercise cali = make(EquipmentType.CALISTHENICS, "Pull Up");
        assertDoesNotThrow(() -> cali.validateWeight(0));
        assertThrows(InvalidWorkoutInputException.class, () -> cali.validateWeight(-5));
        assertTrue(cali.requiresBodyweight());
        assertEquals("added", cali.weightLabel());
    }

    @Test
    void formatSetAndWarmup() {
        assertEquals("100 kg", make(EquipmentType.BARBELL, "Squat").formatSet(100, Unit.KG, null));
        assertEquals("22.5 lb", make(EquipmentType.STACK, "Row").formatSet(22.5, Unit.LB, null));
        assertEquals("30 kg each", make(EquipmentType.DUMBBELL, "Curl").formatSet(30, Unit.KG, null));
        assertEquals("70 | 20 kg",
                make(EquipmentType.CALISTHENICS, "Pull Up").formatSet(20, Unit.KG, 70.0));
        assertEquals("70 | 0 kg",
                make(EquipmentType.CALISTHENICS, "Pull Up").formatSet(0, Unit.KG, 70.0));
        assertEquals(80, make(EquipmentType.BARBELL, "Squat").warmupWeight(100), 1e-9);
    }

    @Test
    void softDeleteAndRestore() {
        Exercise e = make(EquipmentType.BARBELL, "Squat");
        assertFalse(e.isDeleted());
        Instant at = Instant.parse("2026-11-25T10:00:00Z");
        e.softDelete(at);
        assertTrue(e.isDeleted());
        assertEquals(at, e.getDeletedAt());
        assertThrows(InvalidWorkoutInputException.class, () -> e.softDelete(null));
        e.restore();
        assertFalse(e.isDeleted());
    }
}
