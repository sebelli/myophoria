package myophoria.core.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.model.Gym;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.port.GymRepository;
import myophoria.core.port.SessionRepository;

/** Past workouts: list with filters, view, backfill, edit and delete (handoff §7.5, §7.7). */
public class HistoryService {
    /** Newest session first. */
    public static final Comparator<WorkoutSession> NEWEST_FIRST = GymService.CHRONOLOGICAL.reversed();

    private final SessionRepository sessions;
    private final GymRepository gyms;

    /** Creates the service on its repositories. */
    public HistoryService(SessionRepository sessions, GymRepository gyms) {
        this.sessions = sessions;
        this.gyms = gyms;
    }

    /** Lists sessions matching the filter, newest first. */
    public List<SessionHeader> list(HistoryFilter filter) {
        List<WorkoutSession> matching = new ArrayList<>();
        for (WorkoutSession s : sessions.findAll()) {
            if (matches(s, filter)) {
                matching.add(s);
            }
        }
        matching.sort(NEWEST_FIRST);
        Map<String, String> gymNames = new HashMap<>();
        for (Gym g : gyms.findAll()) {
            gymNames.put(g.getId(), g.getName());
        }
        List<SessionHeader> result = new ArrayList<>();
        for (WorkoutSession s : matching) {
            result.add(new SessionHeader(s.getId(), gymNames.get(s.getGymId()), s.getLocalDate(), s.getStartedAt(),
                    s.getEndedAt(), s.getSets().size(), s.isOpen()));
        }
        return result;
    }

    /** Returns one session with its sets, or throws NotFoundException. */
    public WorkoutSession get(String id) {
        WorkoutSession s = sessions.findById(id);
        if (s == null) {
            throw new NotFoundException("session", id);
        }
        return s;
    }

    /** Records a forgotten workout as a closed session; start and end are optional. */
    public WorkoutSession backfill(String gymId, LocalDate localDate, Instant startedAt, Instant endedAt) {
        requireActiveGym(gymId);
        WorkoutSession s = new WorkoutSession(UUID.randomUUID().toString(), gymId, localDate, millis(startedAt),
                millis(endedAt), false);
        sessions.insert(s);
        return s;
    }

    /** Changes gym, date and times of a session; a running workout keeps its gym and gets no end time here. */
    public WorkoutSession edit(String id, String gymId, LocalDate localDate, Instant startedAt, Instant endedAt) {
        WorkoutSession s = get(id);
        if (!s.getGymId().equals(gymId)) {
            if (s.isOpen()) {
                throw new InvalidWorkoutInputException("The gym of a running workout is locked");
            }
            requireActiveGym(gymId);
        }
        s.setGymId(gymId);
        s.setLocalDate(localDate);
        s.setTimes(millis(startedAt), millis(endedAt));
        sessions.update(s);
        return s;
    }

    /** Deletes a session and its sets (the UI confirms with the set count first). */
    public void delete(String id) {
        get(id);
        sessions.delete(id);
    }

    private static boolean matches(WorkoutSession s, HistoryFilter f) {
        if (f == null) {
            return true;
        }
        if (f.from() != null && s.getLocalDate().isBefore(f.from())) {
            return false;
        }
        if (f.to() != null && s.getLocalDate().isAfter(f.to())) {
            return false;
        }
        if (f.gymId() != null && !f.gymId().equals(s.getGymId())) {
            return false;
        }
        if (f.exerciseId() != null) {
            for (WorkoutSet set : s.getSets()) {
                if (set.getExerciseId().equals(f.exerciseId())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void requireActiveGym(String gymId) {
        Gym g = gymId == null ? null : gyms.findById(gymId);
        if (g == null || g.isDeleted()) {
            throw new NotFoundException("gym", gymId);
        }
    }

    private static Instant millis(Instant at) {
        return at == null ? null : at.truncatedTo(ChronoUnit.MILLIS);
    }
}
