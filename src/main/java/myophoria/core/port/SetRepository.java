package myophoria.core.port;

import myophoria.core.model.WorkoutSet;

/** Storage for single sets; findById returns null when missing. */
public interface SetRepository {
    /** Returns the set with this ID, or null. */
    WorkoutSet findById(String id);

    /** Stores a new set. */
    void insert(WorkoutSet set);

    /** Saves changes to an existing set. */
    void update(WorkoutSet set);

    /** Removes the set. */
    void delete(String id);
}
