package myophoria.core.model;

import java.time.Instant;

/** A dumbbell lift; the weight is per dumbbell. */
public class DumbbellExercise extends Exercise {
    /** Creates the exercise. */
    public DumbbellExercise(String id, String name, Laterality laterality, Instant deletedAt) {
        super(id, name, laterality, deletedAt);
    }

    @Override
    public EquipmentType type() {
        return EquipmentType.DUMBBELL;
    }

    @Override
    public String weightLabel() {
        return "per dumbbell";
    }

    @Override
    public String formatSet(double weight, Unit unit, Double bodyweight) {
        return super.formatSet(weight, unit, bodyweight) + " each";
    }
}
