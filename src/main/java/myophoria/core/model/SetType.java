package myophoria.core.model;

/** The kind of a logged set. */
public enum SetType {
    /** Warm-up set; excluded from volume and records. */
    WARMUP,
    /** Normal working set. */
    WORKING,
    /** Working set taken to failure. */
    FAILURE;

    /** Returns true for sets that count towards volume and records (WORKING or FAILURE). */
    public boolean isWorking() {
        return this != WARMUP;
    }
}
