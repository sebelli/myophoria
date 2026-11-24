package myophoria.core.model;

import java.time.Instant;

/** A cable or machine lift; the weight is the stack number. */
public class StackExercise extends Exercise {
    /** Creates the exercise. */
    public StackExercise(String id, String name, Laterality laterality, Instant deletedAt) {
        super(id, name, laterality, deletedAt);
    }

    @Override
    public EquipmentType type() {
        return EquipmentType.STACK;
    }

    @Override
    public String weightLabel() {
        return "stack";
    }
}
