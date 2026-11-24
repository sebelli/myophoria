package myophoria.core.model;

import java.time.Instant;
import java.time.LocalDate;
import myophoria.core.csv.CsvSerializable;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.error.InvalidWorkoutInputException;

/** One bodyweight measurement, independent of workouts. */
public class BodyweightEntry implements CsvSerializable {
    private final String id;
    private double weight;
    private Unit unit;
    private Instant recordedAt;
    private LocalDate localDate;

    /** Creates an entry; values go through the validated setters. */
    public BodyweightEntry(String id, double weight, Unit unit, Instant recordedAt, LocalDate localDate) {
        if (id == null || id.isBlank()) {
            throw new InvalidWorkoutInputException("Bodyweight id is required");
        }
        this.id = id;
        setWeight(weight);
        setUnit(unit);
        setRecordedAt(recordedAt);
        setLocalDate(localDate);
    }

    /** Returns the unique ID. */
    public String getId() {
        return id;
    }

    /** Returns the weight in the entry's unit. */
    public double getWeight() {
        return weight;
    }

    /** Returns the unit the weight was entered in. */
    public Unit getUnit() {
        return unit;
    }

    /** Returns when the weight was measured. */
    public Instant getRecordedAt() {
        return recordedAt;
    }

    /** Returns the calendar day of the measurement as the client saw it. */
    public LocalDate getLocalDate() {
        return localDate;
    }

    /** Sets the weight; it must be above 0. */
    public void setWeight(double weight) {
        if (!(weight > 0) || Double.isInfinite(weight)) {
            throw new InvalidWorkoutInputException("Bodyweight must be greater than 0");
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

    /** Sets the measurement time; it must not be null. */
    public void setRecordedAt(Instant recordedAt) {
        if (recordedAt == null) {
            throw new InvalidWorkoutInputException("Recorded time is required");
        }
        this.recordedAt = recordedAt;
    }

    /** Sets the local date; it must not be null. */
    public void setLocalDate(LocalDate localDate) {
        if (localDate == null) {
            throw new InvalidWorkoutInputException("Local date is required");
        }
        this.localDate = localDate;
    }

    @Override
    public String[] toCsvRow() {
        throw new UnsupportedOperationException("CSV support arrives in T4.2");
    }

    /** Parses one CSV row. */
    public static BodyweightEntry fromCsvRow(String[] row) throws CorruptedCsvException {
        throw new UnsupportedOperationException("CSV support arrives in T4.2");
    }
}
