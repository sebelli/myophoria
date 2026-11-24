package myophoria.core.port;

import java.util.List;
import myophoria.core.model.Exercise;

/** Storage for exercises; findById returns null when missing (the service throws NotFoundException). */
public interface ExerciseRepository {
    /** Returns every exercise, active and soft-deleted. */
    List<Exercise> findAll();

    /** Returns the exercise with this ID, or null. */
    Exercise findById(String id);

    /** Stores a new exercise. */
    void insert(Exercise exercise);

    /** Saves changes to an existing exercise. */
    void update(Exercise exercise);

    /** Returns how many logged sets use the exercise. */
    int countSets(String exerciseId);

    /** Returns how many sessions contain the exercise. */
    int countSessions(String exerciseId);

    /** Removes the exercise and everything that depends on it, in one transaction. */
    void deletePermanently(String id);
}
