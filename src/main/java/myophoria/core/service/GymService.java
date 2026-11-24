package myophoria.core.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import myophoria.core.error.DuplicateResourceException;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.model.Gym;
import myophoria.core.model.WorkoutSession;
import myophoria.core.port.GymRepository;
import myophoria.core.port.SessionRepository;

/** Use cases for gyms (handoff §7.4). */
public class GymService {
    private static final String RESOURCE = "gym";
    private static final Comparator<Gym> BY_NAME = Comparator.comparing(Gym::getNameKey);
    /** Oldest session first: by local date, then start time (sessions without a start sort first within a day). */
    public static final Comparator<WorkoutSession> CHRONOLOGICAL = Comparator
            .comparing(WorkoutSession::getLocalDate)
            .thenComparing(WorkoutSession::getStartedAt, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final GymRepository gyms;
    private final SessionRepository sessions;
    private final Clock clock;

    /** Creates the service on its repositories and a clock. */
    public GymService(GymRepository gyms, SessionRepository sessions, Clock clock) {
        this.gyms = gyms;
        this.sessions = sessions;
        this.clock = clock;
    }

    /** Lists active gyms sorted by name. */
    public List<Gym> list() {
        List<Gym> result = new ArrayList<>();
        for (Gym g : gyms.findAll()) {
            if (!g.isDeleted()) {
                result.add(g);
            }
        }
        result.sort(BY_NAME);
        return result;
    }

    /** Returns a gym (active or deleted) or throws NotFoundException. */
    public Gym get(String id) {
        Gym g = gyms.findById(id);
        if (g == null) {
            throw new NotFoundException(RESOURCE, id);
        }
        return g;
    }

    /** Creates a gym; the name must not be used by another active gym. */
    public Gym create(String name) {
        Gym g = new Gym(UUID.randomUUID().toString(), name, false, null);
        checkNameFree(g.getNameKey(), g.getId());
        gyms.insert(g);
        return g;
    }

    /** Renames an active gym. */
    public Gym rename(String id, String name) {
        Gym g = getActive(id);
        String oldKey = g.getNameKey();
        g.setName(name);
        if (!g.getNameKey().equals(oldKey)) {
            checkNameFree(g.getNameKey(), id);
        }
        gyms.update(g);
        return g;
    }

    /** Makes an active gym the default; the previous default is cleared. */
    public Gym makeDefault(String id) {
        getActive(id);
        gyms.makeDefault(id);
        return get(id);
    }

    /** Soft-deletes a gym; the default gym and the last active gym cannot be deleted. */
    public void delete(String id) {
        Gym g = getActive(id);
        if (g.isDefault()) {
            throw new InvalidWorkoutInputException("The default gym cannot be deleted");
        }
        if (list().size() <= 1) {
            throw new InvalidWorkoutInputException("The last gym cannot be deleted");
        }
        g.softDelete(Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
        gyms.update(g);
    }

    /** Returns the gym of the most recent session if it is still active, else the default gym. */
    public Gym activeGym() {
        WorkoutSession latest = null;
        for (WorkoutSession s : sessions.findAll()) {
            if (latest == null || CHRONOLOGICAL.compare(s, latest) > 0) {
                latest = s;
            }
        }
        if (latest != null) {
            Gym g = gyms.findById(latest.getGymId());
            if (g != null && !g.isDeleted()) {
                return g;
            }
        }
        for (Gym g : gyms.findAll()) {
            if (g.isDefault() && !g.isDeleted()) {
                return g;
            }
        }
        throw new IllegalStateException("No default gym");
    }

    private Gym getActive(String id) {
        Gym g = get(id);
        if (g.isDeleted()) {
            throw new NotFoundException(RESOURCE, id);
        }
        return g;
    }

    // deleted gyms have no restore flow, so only active names count (matches the unique index)
    private void checkNameFree(String nameKey, String selfId) {
        for (Gym g : gyms.findAll()) {
            if (!g.isDeleted() && !g.getId().equals(selfId) && g.getNameKey().equals(nameKey)) {
                throw new DuplicateResourceException(RESOURCE, g.getId(), false);
            }
        }
    }
}
