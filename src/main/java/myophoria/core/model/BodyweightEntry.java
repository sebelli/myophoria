package myophoria.core.model;

import java.time.Instant;
import java.time.LocalDate;
import myophoria.core.csv.CsvCodec;
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

    /** Column names of this entity's CSV file. */
    public static final String[] HEADER = {"id", "weight", "unit", "recorded_at", "local_date"};

    @Override
    public String[] toCsvRow() {
        return new String[] {id, CsvCodec.format(weight), unit.name(), CsvCodec.format(recordedAt), localDate.toString()};
    }

    /** Parses one CSV row in HEADER order; a bad value throws CorruptedCsvException. */
    public static BodyweightEntry fromCsvRow(String[] row) throws CorruptedCsvException {
        CsvCodec.requireColumns(row, HEADER.length);
        try {
            return new BodyweightEntry(row[0], CsvCodec.number(row[1], "weight"),
                    CsvCodec.enumValue(Unit.class, row[2], "unit"), CsvCodec.instant(row[3], "recorded_at"),
                    CsvCodec.date(row[4], "local_date"));
        } catch (InvalidWorkoutInputException e) {
            throw CsvCodec.bad(e.getMessage());
        }
    }
}
