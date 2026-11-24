package myophoria.core.port;

import java.util.List;
import myophoria.core.model.WorkoutSession;

/** Storage for sessions; loaded sessions carry their sets. findById and findOpen return null when missing. */
public interface SessionRepository {
    /** Returns every session with its sets. */
    List<WorkoutSession> findAll();

    /** Returns the session with this ID and its sets, or null. */
    WorkoutSession findById(String id);

    /** Returns the open session with its sets, or null. */
    WorkoutSession findOpen();

    /** Stores a new session row (sets are stored through SetRepository). */
    void insert(WorkoutSession session);

    /** Saves the session row. */
    void update(WorkoutSession session);

    /** Removes the session and its sets. */
    void delete(String id);
}
