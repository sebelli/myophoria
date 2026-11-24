package myophoria.core.model;

/** The kind of a logged set. */
public enum SetType {
    WARMUP, WORKING, FAILURE;

    /** Returns true for sets that count towards volume and records (WORKING or FAILURE). */
    public boolean isWorking() {
        return this != WARMUP;
    }
}
