package myophoria.core.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.Exercise;
import myophoria.core.model.Gym;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.port.BodyweightRepository;
import myophoria.core.port.ExerciseRepository;
import myophoria.core.port.GymRepository;
import myophoria.core.port.SessionRepository;
import myophoria.core.port.SetRepository;

/** The live workout: start, log sets, prefill, finish (handoff §7.1, §7.2, §7.5). */
public class WorkoutService {
    private static final Comparator<WorkoutSet> BY_POSITION = Comparator.comparingInt(WorkoutSet::getPosition);

    private final SessionRepository sessions;
    private final SetRepository sets;
    private final ExerciseRepository exercises;
    private final GymRepository gyms;
    private final BodyweightRepository bodyweights;
    private final Clock clock;

    /** Creates the service on its repositories and a clock. */
    public WorkoutService(SessionRepository sessions, SetRepository sets, ExerciseRepository exercises,
            GymRepository gyms, BodyweightRepository bodyweights, Clock clock) {
        this.sessions = sessions;
        this.sets = sets;
        this.exercises = exercises;
        this.gyms = gyms;
        this.bodyweights = bodyweights;
        this.clock = clock;
    }

    /** Returns the running workout, or null. */
    public WorkoutSession open() {
        return sessions.findOpen();
    }

    /** Starts a workout at an active gym; only one workout can be open. */
    public WorkoutSession start(String gymId, LocalDate localDate) {
        if (sessions.findOpen() != null) {
            throw new InvalidWorkoutInputException("A workout is already open");
        }
        Gym gym = gymId == null ? null : gyms.findById(gymId);
        if (gym == null || gym.isDeleted()) {
            throw new NotFoundException("gym", gymId);
        }
        WorkoutSession s = new WorkoutSession(UUID.randomUUID().toString(), gymId, localDate, now(), null, true);
        sessions.insert(s);
        return s;
    }

    /** Finishes a workout; one without sets must be discarded (deleted) instead. */
    public WorkoutSession finish(String sessionId) {
        WorkoutSession s = getSession(sessionId);
        if (s.getSets().isEmpty()) {
            throw new InvalidWorkoutInputException("A workout without sets cannot be finished; discard it instead");
        }
        s.finish(now());
        sessions.update(s);
        return s;
    }

    /** Saves a ticked set into a session (live or backfilled). */
    public WorkoutSet addSet(String sessionId, SetInput in) {
        getSession(sessionId);
        Exercise exercise = checkInput(in);
        if (exercise.isDeleted()) {
            throw new InvalidWorkoutInputException("Cannot log sets for a deleted exercise");
        }
        WorkoutSet set = new WorkoutSet(UUID.randomUUID().toString(), sessionId, in.exerciseId(), in.position(),
                in.setType(), in.weight(), in.unit(), in.reps(), in.bodyweight(), now());
        sets.insert(set);
        return set;
    }

    /** Edits a ticked set; its completion time stays. */
    public WorkoutSet updateSet(String setId, SetInput in) {
        WorkoutSet set = getSet(setId);
        Exercise exercise = checkInput(in);
        if (exercise.isDeleted() && !exercise.getId().equals(set.getExerciseId())) {
            throw new InvalidWorkoutInputException("Cannot move a set to a deleted exercise");
        }
        set.setExerciseId(in.exerciseId());
        set.setPosition(in.position());
        set.setSetType(in.setType());
        set.setWeight(in.weight());
        set.setUnit(in.unit());
        set.setReps(in.reps());
        set.setBodyweight(in.bodyweight());
        sets.update(set);
        return set;
    }

    /** Removes a set (unticking); a closed session left without sets is deleted too (G6). */
    public void deleteSet(String setId) {
        WorkoutSet set = getSet(setId);
        sets.delete(setId);
        WorkoutSession s = sessions.findById(set.getSessionId());
        if (s != null && !s.isOpen() && s.getSets().isEmpty()) {
            sessions.delete(s.getId());
        }
    }

    /** Suggests rows for an exercise card from the last session at the same gym (PLAN §4.5 prefill algorithm). */
    public Prefill prefill(String sessionId, String exerciseId) {
        WorkoutSession current = getSession(sessionId);
        Exercise exercise = exercises.findById(exerciseId);
        if (exercise == null) {
            throw new NotFoundException("exercise", exerciseId);
        }
        // 1. unit of the newest set of this exercise at this gym; 2. last other session containing it
        WorkoutSet newest = null;
        WorkoutSession last = null;
        for (WorkoutSession s : sessions.findAll()) {
            if (!s.getGymId().equals(current.getGymId())) {
                continue;
            }
            boolean contains = false;
            for (WorkoutSet set : s.getSets()) {
                if (set.getExerciseId().equals(exerciseId)) {
                    contains = true;
                    if (newest == null || set.getCompletedAt().isAfter(newest.getCompletedAt())) {
                        newest = set;
                    }
                }
            }
            boolean later = last == null || GymService.CHRONOLOGICAL.compare(s, last) > 0;
            if (contains && !s.getId().equals(sessionId) && later) {
                last = s;
            }
        }
        Unit unit = newest == null ? Unit.KG : newest.getUnit();

        // 3. rows from the last session in position order; 4. warm-up from its heaviest working set
        List<PrefillRow> rows = new ArrayList<>();
        Double topWorking = null;
        if (last != null) {
            List<WorkoutSet> own = new ArrayList<>();
            for (WorkoutSet set : last.getSets()) {
                if (set.getExerciseId().equals(exerciseId)) {
                    own.add(set);
                }
            }
            own.sort(BY_POSITION);
            for (WorkoutSet set : own) {
                double weight = set.weightIn(unit);
                rows.add(new PrefillRow(set.getSetType(), weight, set.getReps()));
                if (set.getSetType().isWorking() && (topWorking == null || weight > topWorking)) {
                    topWorking = weight;
                }
            }
        }
        Double warmup = topWorking == null ? null : exercise.warmupWeight(topWorking);

        // 5. latest bodyweight converted into the unit, only where the exercise needs it
        Double bodyweight = null;
        if (exercise.requiresBodyweight()) {
            BodyweightEntry latest = null;
            for (BodyweightEntry e : bodyweights.findAll()) {
                if (latest == null || e.getRecordedAt().isAfter(latest.getRecordedAt())) {
                    latest = e;
                }
            }
            if (latest != null) {
                bodyweight = latest.getUnit().convert(latest.getWeight(), unit);
            }
        }
        return new Prefill(unit, rows, warmup, bodyweight);
    }

    // exercise-dependent rules; the set's own field rules run in the WorkoutSet setters
    private Exercise checkInput(SetInput in) {
        if (in == null || in.exerciseId() == null) {
            throw new InvalidWorkoutInputException("Exercise is required");
        }
        Exercise exercise = exercises.findById(in.exerciseId());
        if (exercise == null) {
            throw new NotFoundException("exercise", in.exerciseId());
        }
        exercise.validateWeight(in.weight());
        if (exercise.requiresBodyweight() && in.bodyweight() == null) {
            throw new InvalidWorkoutInputException("Bodyweight is required for this exercise");
        }
        if (!exercise.requiresBodyweight() && in.bodyweight() != null) {
            throw new InvalidWorkoutInputException("Bodyweight is only recorded for calisthenics");
        }
        return exercise;
    }

    private WorkoutSession getSession(String id) {
        WorkoutSession s = sessions.findById(id);
        if (s == null) {
            throw new NotFoundException("session", id);
        }
        return s;
    }

    private WorkoutSet getSet(String id) {
        WorkoutSet set = sets.findById(id);
        if (set == null) {
            throw new NotFoundException("set", id);
        }
        return set;
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
