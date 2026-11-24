package myophoria.core.model;

import java.time.Instant;
import myophoria.core.error.InvalidWorkoutInputException;

/** A bodyweight lift; added weight may be 0 and a bodyweight snapshot is required. */
public class CalisthenicsExercise extends Exercise {
    /** Creates the exercise. */
    public CalisthenicsExercise(String id, String name, Laterality laterality, Instant deletedAt) {
        super(id, name, laterality, deletedAt);
    }

    @Override
    public EquipmentType type() {
        return EquipmentType.CALISTHENICS;
    }

    @Override
    public String weightLabel() {
        return "added";
    }

    @Override
    public void validateWeight(double weight) {
        if (!(weight >= 0) || Double.isInfinite(weight)) {
            throw new InvalidWorkoutInputException("Added weight must be 0 or more");
        }
    }

    @Override
    public boolean requiresBodyweight() {
        return true;
    }

    @Override
    public String formatSet(double weight, Unit unit, Double bodyweight) {
        String bw = bodyweight == null ? "?" : formatNumber(bodyweight);
        return bw + " | " + formatNumber(weight) + " " + unit.label();
    }
}
