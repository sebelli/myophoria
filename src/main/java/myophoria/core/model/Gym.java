package myophoria.core.model;

import java.text.Normalizer;
import java.time.Instant;
import myophoria.core.csv.CsvCodec;
import myophoria.core.csv.CsvSerializable;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.error.InvalidWorkoutInputException;

/** A place where workouts happen; exactly one active gym is the default. */
public class Gym implements CsvSerializable {
    /** ID of the gym seeded by the first migration. */
    public static final String SEEDED_DEFAULT_ID = "00000000-0000-0000-0000-000000000001";

    private final String id;
    private String name;
    private String nameKey;
    private boolean isDefault;
    private Instant deletedAt; // null while active

    /** Creates a gym; values go through the validated setters. */
    public Gym(String id, String name, boolean isDefault, Instant deletedAt) {
        if (id == null || id.isBlank()) {
            throw new InvalidWorkoutInputException("Gym id is required");
        }
        this.id = id;
        setName(name);
        setDefault(isDefault);
        this.deletedAt = deletedAt;
    }

    /** Returns the unique ID. */
    public String getId() {
        return id;
    }

    /** Returns the display name (trimmed, single spaces). */
    public String getName() {
        return name;
    }

    /** Returns the normalized name used for uniqueness checks (same rule as exercises, G3). */
    public String getNameKey() {
        return nameKey;
    }

    /** Returns true for the default gym. */
    public boolean isDefault() {
        return isDefault;
    }

    /** Returns when the gym was soft-deleted, or null if active. */
    public Instant getDeletedAt() {
        return deletedAt;
    }

    /** Returns true if the gym is soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Sets the name; it must not be blank. */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidWorkoutInputException("Gym name is required");
        }
        this.nameKey = Exercise.nameKey(name);
        this.name = Normalizer.normalize(name, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ");
    }

    /** Sets the default flag (GymService keeps exactly one default). */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /** Marks the gym as deleted at the given time. */
    public void softDelete(Instant at) {
        if (at == null) {
            throw new InvalidWorkoutInputException("Deletion time is required");
        }
        this.deletedAt = at;
    }

    /** Column names of this entity's CSV file. */
    public static final String[] HEADER = {"id", "name", "is_default", "deleted_at"};

    @Override
    public String[] toCsvRow() {
        return new String[] {id, name, String.valueOf(isDefault), CsvCodec.format(deletedAt)};
    }

    /** Parses one CSV row in HEADER order; a bad value throws CorruptedCsvException. */
    public static Gym fromCsvRow(String[] row) throws CorruptedCsvException {
        CsvCodec.requireColumns(row, HEADER.length);
        try {
            return new Gym(row[0], row[1], CsvCodec.bool(row[2], "is_default"), CsvCodec.instant(row[3], "deleted_at"));
        } catch (InvalidWorkoutInputException e) {
            throw CsvCodec.bad(e.getMessage());
        }
    }
}
