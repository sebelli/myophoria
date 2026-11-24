package myophoria.core.fake;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.port.SessionRepository;
import myophoria.core.port.SetRepository;

/**
 * In-memory sessions and sets behind two port views (sessions(), sets()). Reads return fresh copies like the
 * database, so a service that forgets to save shows up in tests.
 */
public class FakeWorkoutStore {
    private static final Comparator<WorkoutSet> SET_ORDER =
            Comparator.comparing(WorkoutSet::getCompletedAt).thenComparing(WorkoutSet::getPosition);
    private final Map<String, WorkoutSession> sessionRows = new LinkedHashMap<>();
    private final Map<String, WorkoutSet> setRows = new LinkedHashMap<>();
    private final SessionRepository sessions = new Sessions();
    private final SetRepository sets = new Sets();

    /** Returns the session port. */
    public SessionRepository sessions() {
        return sessions;
    }

    /** Returns the set port. */
    public SetRepository sets() {
        return sets;
    }

    private class Sessions implements SessionRepository {
        @Override
        public List<WorkoutSession> findAll() {
            List<WorkoutSession> result = new ArrayList<>();
            for (WorkoutSession s : sessionRows.values()) {
                result.add(withSets(s));
            }
            return result;
        }

        @Override
        public WorkoutSession findById(String id) {
            WorkoutSession s = sessionRows.get(id);
            return s == null ? null : withSets(s);
        }

        @Override
        public WorkoutSession findOpen() {
            for (WorkoutSession s : sessionRows.values()) {
                if (s.isOpen()) {
                    return withSets(s);
                }
            }
            return null;
        }

        @Override
        public void insert(WorkoutSession s) {
            if (s.isOpen() && findOpen() != null) {
                throw new IllegalStateException("sessions_one_open violated"); // mirrors the unique index
            }
            sessionRows.put(s.getId(), copy(s));
        }

        @Override
        public void update(WorkoutSession s) {
            sessionRows.put(s.getId(), copy(s));
        }

        @Override
        public void delete(String id) {
            sessionRows.remove(id);
            setRows.values().removeIf(x -> x.getSessionId().equals(id)); // ON DELETE CASCADE
        }
    }

    private class Sets implements SetRepository {
        @Override
        public WorkoutSet findById(String id) {
            WorkoutSet x = setRows.get(id);
            return x == null ? null : copy(x);
        }

        @Override
        public void insert(WorkoutSet x) {
            if (!sessionRows.containsKey(x.getSessionId())) {
                throw new IllegalStateException("FOREIGN KEY failed"); // mirrors the database
            }
            setRows.put(x.getId(), copy(x));
        }

        @Override
        public void update(WorkoutSet x) {
            setRows.put(x.getId(), copy(x));
        }

        @Override
        public void delete(String id) {
            setRows.remove(id);
        }
    }

    private WorkoutSession withSets(WorkoutSession row) {
        WorkoutSession s = copy(row);
        List<WorkoutSet> own = new ArrayList<>();
        for (WorkoutSet x : setRows.values()) {
            if (x.getSessionId().equals(s.getId())) {
                own.add(copy(x));
            }
        }
        own.sort(SET_ORDER);
        for (WorkoutSet x : own) {
            s.addSet(x);
        }
        return s;
    }

    private static WorkoutSession copy(WorkoutSession s) {
        return new WorkoutSession(s.getId(), s.getGymId(), s.getLocalDate(), s.getStartedAt(), s.getEndedAt(), s.isOpen());
    }

    private static WorkoutSet copy(WorkoutSet x) {
        return new WorkoutSet(x.getId(), x.getSessionId(), x.getExerciseId(), x.getPosition(), x.getSetType(),
                x.getWeight(), x.getUnit(), x.getReps(), x.getBodyweight(), x.getCompletedAt());
    }
}
