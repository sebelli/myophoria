package myophoria.core.model;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import myophoria.core.csv.CsvSerializable;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.error.InvalidWorkoutInputException;

/** A lift in the library; subclasses override only what differs per equipment type. */
public abstract class Exercise implements CsvSerializable {
    private final String id;
    private String name;
    private String nameKey;
    private Laterality laterality;
    private Instant deletedAt; // null while active

    /** Creates an exercise; values go through the validated setters. */
    protected Exercise(String id, String name, Laterality laterality, Instant deletedAt) {
        if (id == null || id.isBlank()) {
            throw new InvalidWorkoutInputException("Exercise id is required");
        }
        this.id = id;
        setName(name);
        setLaterality(laterality);
        this.deletedAt = deletedAt;
    }

    /** Builds the right subclass for a stored equipment type (the only type-to-subclass switch). */
    public static Exercise of(EquipmentType type, String id, String name, Laterality laterality, Instant deletedAt) {
        if (type == null) {
            throw new InvalidWorkoutInputException("Equipment type is required");
        }
        return switch (type) {
            case BARBELL -> new BarbellExercise(id, name, laterality, deletedAt);
            case DUMBBELL -> new DumbbellExercise(id, name, laterality, deletedAt);
            case STACK -> new StackExercise(id, name, laterality, deletedAt);
            case PLATE_LOADED -> new PlateLoadedExercise(id, name, laterality, deletedAt);
            case CALISTHENICS -> new CalisthenicsExercise(id, name, laterality, deletedAt);
        };
    }

    /** Returns the comparison key: NFC, trimmed, single spaces, lower-case. */
    public static String nameKey(String name) {
        String nfc = Normalizer.normalize(name, Normalizer.Form.NFC);
        return nfc.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Returns the equipment type of this exercise. */
    public abstract EquipmentType type();

    /** Returns the text shown beside the weight field ("total", "per dumbbell", ...). */
    public abstract String weightLabel();

    /** Throws if the weight is not allowed for this exercise (default: must be above 0). */
    public void validateWeight(double weight) {
        if (!(weight > 0) || Double.isInfinite(weight)) {
            throw new InvalidWorkoutInputException("Weight must be greater than 0");
        }
    }

    /** Returns true if every set needs a bodyweight snapshot (default: false). */
    public boolean requiresBodyweight() {
        return false;
    }

    /** Returns the set as display text (default "100 kg"); bodyweight is null unless required. */
    public String formatSet(double weight, Unit unit, Double bodyweight) {
        return formatNumber(weight) + " " + unit.label();
    }

    /** Returns the suggested warm-up weight: 80% of the last top working weight. */
    public double warmupWeight(double lastTopWeight) {
        return 0.8 * lastTopWeight;
    }

    /** Returns the badge letters: all initials for 2+ words, first two letters for one word. */
    public String initials() {
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        if (words.length >= 2) {
            for (String word : words) {
                sb.append(word.charAt(0));
            }
        } else {
            sb.append(name, 0, Math.min(2, name.length()));
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    /** Formats a number without a trailing ".0" and with at most two decimals. */
    protected static String formatNumber(double value) {
        String s = String.format(Locale.ROOT, "%.2f", value);
        s = s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
        return s;
    }

    /** Returns the unique ID. */
    public String getId() {
        return id;
    }

    /** Returns the display name (trimmed, single spaces). */
    public String getName() {
        return name;
    }

    /** Returns the normalized name used for uniqueness checks. */
    public String getNameKey() {
        return nameKey;
    }

    /** Returns whether the exercise is bilateral or unilateral. */
    public Laterality getLaterality() {
        return laterality;
    }

    /** Returns when the exercise was soft-deleted, or null if active. */
    public Instant getDeletedAt() {
        return deletedAt;
    }

    /** Sets the name; it must not be blank. */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidWorkoutInputException("Exercise name is required");
        }
        this.nameKey = nameKey(name);
        this.name = Normalizer.normalize(name, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ");
    }

    /** Sets the laterality; it must not be null. */
    public void setLaterality(Laterality laterality) {
        if (laterality == null) {
            throw new InvalidWorkoutInputException("Laterality is required");
        }
        this.laterality = laterality;
    }

    /** Marks the exercise as deleted at the given time. */
    public void softDelete(Instant at) {
        if (at == null) {
            throw new InvalidWorkoutInputException("Deletion time is required");
        }
        this.deletedAt = at;
    }

    /** Clears the soft-delete marker. */
    public void restore() {
        this.deletedAt = null;
    }

    /** Returns true if the exercise is soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public String[] toCsvRow() {
        throw new UnsupportedOperationException("CSV support arrives in T4.2");
    }

    /** Parses one CSV row into the right subclass. */
    public static Exercise fromCsvRow(String[] row) throws CorruptedCsvException {
        throw new UnsupportedOperationException("CSV support arrives in T4.2");
    }
}
