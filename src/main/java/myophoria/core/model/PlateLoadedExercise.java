package myophoria.core.model;

import java.time.Instant;

/** A plate-loaded machine lift; the weight is the loaded plates. */
public class PlateLoadedExercise extends Exercise {
    /** Creates the exercise. */
    public PlateLoadedExercise(String id, String name, Laterality laterality, Instant deletedAt) {
        super(id, name, laterality, deletedAt);
    }

    @Override
    public EquipmentType type() {
        return EquipmentType.PLATE_LOADED;
    }

    @Override
    public String weightLabel() {
        return "plates";
    }
}
