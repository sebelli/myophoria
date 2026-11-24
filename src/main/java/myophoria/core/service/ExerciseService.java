package myophoria.core.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import myophoria.core.error.DuplicateResourceException;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import myophoria.core.port.ExerciseRepository;

/** Use cases for the exercise library (usage and permanent delete arrive in T4.6). */
public class ExerciseService {
    private static final String RESOURCE = "exercise";
    private static final Comparator<Exercise> BY_NAME = Comparator.comparing(Exercise::getNameKey);

    private final ExerciseRepository exercises;
    private final Clock clock;

    /** Creates the service on a repository and a clock (the clock makes deletion times testable). */
    public ExerciseService(ExerciseRepository exercises, Clock clock) {
        this.exercises = exercises;
        this.clock = clock;
    }

    /** Lists active (or, if deleted is true, soft-deleted) exercises whose name contains the query, sorted by name. */
    public List<Exercise> search(String query, Set<EquipmentType> types, boolean deleted) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Exercise> result = new ArrayList<>();
        for (Exercise e : exercises.findAll()) {
            boolean typeOk = types == null || types.isEmpty() || types.contains(e.type());
            if (e.isDeleted() == deleted && typeOk && e.getNameKey().contains(needle)) {
                result.add(e);
            }
        }
        result.sort(BY_NAME);
        return result;
    }

    /** Returns one exercise or throws NotFoundException. */
    public Exercise get(String id) {
        Exercise e = exercises.findById(id);
        if (e == null) {
            throw new NotFoundException(RESOURCE, id);
        }
        return e;
    }

    /** Creates an exercise; throws if the name is invalid or already used. */
    public Exercise create(String name, EquipmentType type, Laterality laterality) {
        Exercise e = Exercise.of(type, UUID.randomUUID().toString(), name, laterality, null);
        checkNameFree(e.getNameKey(), e.getId());
        exercises.insert(e);
        return e;
    }

    /** Changes name, type and laterality; the type can only change while no set uses the exercise. */
    public Exercise update(String id, String name, EquipmentType type, Laterality laterality) {
        Exercise old = get(id);
        if (type != old.type() && exercises.countSets(id) > 0) {
            throw new InvalidWorkoutInputException("Equipment type cannot change once the exercise has sets");
        }
        Exercise updated = Exercise.of(type, id, name, laterality, old.getDeletedAt());
        if (!updated.getNameKey().equals(old.getNameKey())) { // an unchanged name never blocks editing other fields
            checkNameFree(updated.getNameKey(), id);
        }
        exercises.update(updated);
        return updated;
    }

    /** Soft-deletes an exercise (already deleted is left as it is). */
    public Exercise softDelete(String id) {
        Exercise e = get(id);
        if (!e.isDeleted()) {
            e.softDelete(Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
            exercises.update(e);
        }
        return e;
    }

    /** Restores a soft-deleted exercise; throws if an active exercise now uses its name. */
    public Exercise restore(String id) {
        Exercise e = get(id);
        if (e.isDeleted()) {
            Exercise active = activeByKey().get(e.getNameKey());
            if (active != null) {
                throw new DuplicateResourceException(RESOURCE, active.getId(), false);
            }
            e.restore();
            exercises.update(e);
        }
        return e;
    }

    // The map holds every exercise by name key; an active one wins over a deleted one with the same key.
    private void checkNameFree(String nameKey, String selfId) {
        Map<String, Exercise> byKey = new HashMap<>();
        for (Exercise e : exercises.findAll()) {
            if (e.getId().equals(selfId)) {
                continue;
            }
            Exercise seen = byKey.get(e.getNameKey());
            if (seen == null || (seen.isDeleted() && !e.isDeleted())) {
                byKey.put(e.getNameKey(), e);
            }
        }
        Exercise clash = byKey.get(nameKey);
        if (clash != null) {
            throw new DuplicateResourceException(RESOURCE, clash.getId(), clash.isDeleted());
        }
    }

    private Map<String, Exercise> activeByKey() {
        Map<String, Exercise> active = new HashMap<>();
        for (Exercise e : exercises.findAll()) {
            if (!e.isDeleted()) {
                active.put(e.getNameKey(), e);
            }
        }
        return active;
    }
}
