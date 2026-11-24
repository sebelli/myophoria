package myophoria.core.model;

import java.time.Instant;
import myophoria.core.csv.CsvSerializable;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.error.InvalidWorkoutInputException;

/** One completed set; exercise-dependent rules (weight, bodyweight) are checked by WorkoutService. */
public class WorkoutSet implements VolumeCalculable, CsvSerializable {
    private final String id;
    private final String sessionId;
    private String exerciseId;
    private int position;
    private SetType setType;
    private double weight;
    private Unit unit;
    private int reps;
    private Double bodyweight; // snapshot in this set's unit; null unless the exercise requires it
    private Instant completedAt;

    /** Creates a set; values go through the validated setters. */
    public WorkoutSet(String id, String sessionId, String exerciseId, int position, SetType setType, double weight,
            Unit unit, int reps, Double bodyweight, Instant completedAt) {
        this.id = required(id, "Set id");
        this.sessionId = required(sessionId, "Session id");
        setExerciseId(exerciseId);
        setPosition(position);
        setSetType(setType);
        setWeight(weight);
        setUnit(unit);
        setReps(reps);
        setBodyweight(bodyweight);
        setCompletedAt(completedAt);
    }

    /** Returns the volume in the reporting unit: weight × reps, 0 for warm-ups (Calisthenics weight is added weight). */
    @Override
    public double volume(Unit reportingUnit) {
        return setType.isWorking() ? unit.convert(weight, reportingUnit) * reps : 0;
    }

    /** Returns the weight converted into another unit. */
    public double weightIn(Unit target) {
        return unit.convert(weight, target);
    }

    /** Returns the unique ID. */
    public String getId() {
        return id;
    }

    /** Returns the session this set belongs to. */
    public String getSessionId() {
        return sessionId;
    }

    /** Returns the exercise of this set. */
    public String getExerciseId() {
        return exerciseId;
    }

    /** Returns the row number within its exercise (1-based). */
    public int getPosition() {
        return position;
    }

    /** Returns warm-up, working or failure. */
    public SetType getSetType() {
        return setType;
    }

    /** Returns the weight as entered, in the set's unit. */
    public double getWeight() {
        return weight;
    }

    /** Returns the unit the weight was entered in. */
    public Unit getUnit() {
        return unit;
    }

    /** Returns the repetitions. */
    public int getReps() {
        return reps;
    }

    /** Returns the bodyweight snapshot in the set's unit, or null. */
    public Double getBodyweight() {
        return bodyweight;
    }

    /** Returns when the set was ticked. */
    public Instant getCompletedAt() {
        return completedAt;
    }

    /** Sets the exercise; it must not be blank. */
    public void setExerciseId(String exerciseId) {
        this.exerciseId = required(exerciseId, "Exercise id");
    }

    /** Sets the row number; it must be 1 or more. */
    public void setPosition(int position) {
        if (position < 1) {
            throw new InvalidWorkoutInputException("Set position must be 1 or more");
        }
        this.position = position;
    }

    /** Sets the set type; it must not be null. */
    public void setSetType(SetType setType) {
        if (setType == null) {
            throw new InvalidWorkoutInputException("Set type is required");
        }
        this.setType = setType;
    }

    /** Sets the weight; it must be 0 or more. */
    public void setWeight(double weight) {
        if (!(weight >= 0) || Double.isInfinite(weight)) {
            throw new InvalidWorkoutInputException("Weight must be 0 or more");
        }
        this.weight = weight;
    }

    /** Sets the unit; it must not be null. */
    public void setUnit(Unit unit) {
        if (unit == null) {
            throw new InvalidWorkoutInputException("Unit is required");
        }
        this.unit = unit;
    }

    /** Sets the repetitions; they must be above 0. */
    public void setReps(int reps) {
        if (reps < 1) {
            throw new InvalidWorkoutInputException("Reps must be greater than 0");
        }
        this.reps = reps;
    }

    /** Sets the bodyweight snapshot; null or above 0. */
    public void setBodyweight(Double bodyweight) {
        if (bodyweight != null && (!(bodyweight > 0) || bodyweight.isInfinite())) {
            throw new InvalidWorkoutInputException("Bodyweight must be greater than 0");
        }
        this.bodyweight = bodyweight;
    }

    /** Sets the completion time; it must not be null. */
    public void setCompletedAt(Instant completedAt) {
        if (completedAt == null) {
            throw new InvalidWorkoutInputException("Completion time is required");
        }
        this.completedAt = completedAt;
    }

    private static String required(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new InvalidWorkoutInputException(what + " is required");
        }
        return value;
    }

    @Override
    public String[] toCsvRow() {
        throw new UnsupportedOperationException("CSV support arrives in T4.2");
    }

    /** Parses one CSV row. */
    public static WorkoutSet fromCsvRow(String[] row) throws CorruptedCsvException {
        throw new UnsupportedOperationException("CSV support arrives in T4.2");
    }
}
