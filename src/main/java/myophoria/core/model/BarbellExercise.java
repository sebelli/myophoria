package myophoria.core.model;

import java.time.Instant;

/** A barbell lift; the weight is the total including the bar. */
public class BarbellExercise extends Exercise {
    /** Creates the exercise. */
    public BarbellExercise(String id, String name, Laterality laterality, Instant deletedAt) {
        super(id, name, laterality, deletedAt);
    }

    @Override
    public EquipmentType type() {
        return EquipmentType.BARBELL;
    }

    @Override
    public String weightLabel() {
        return "total";
    }
}
