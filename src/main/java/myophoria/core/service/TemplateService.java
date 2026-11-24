package myophoria.core.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.model.Exercise;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.model.WorkoutTemplate;
import myophoria.core.port.ExerciseRepository;
import myophoria.core.port.SessionRepository;
import myophoria.core.port.TemplateRepository;

/** Use cases for workout templates (handoff §7.6, G11). */
public class TemplateService {
    private static final String RESOURCE = "template";
    private static final Comparator<WorkoutTemplate> BY_NAME =
            Comparator.comparing(t -> t.getName().toLowerCase(Locale.ROOT));

    private final TemplateRepository templates;
    private final ExerciseRepository exercises;
    private final SessionRepository sessions;

    /** Creates the service on its repositories. */
    public TemplateService(TemplateRepository templates, ExerciseRepository exercises, SessionRepository sessions) {
        this.templates = templates;
        this.exercises = exercises;
        this.sessions = sessions;
    }

    /** Lists templates by name; soft-deleted exercises are hidden (they come back when restored, G11). */
    public List<WorkoutTemplate> list() {
        Set<String> active = new HashSet<>(); // one load instead of one query per listed exercise
        for (Exercise e : exercises.findAll()) {
            if (!e.isDeleted()) {
                active.add(e.getId());
            }
        }
        List<WorkoutTemplate> result = new ArrayList<>();
        for (WorkoutTemplate t : templates.findAll()) {
            List<String> visible = new ArrayList<>();
            for (String exerciseId : t.getExerciseIds()) {
                if (active.contains(exerciseId)) {
                    visible.add(exerciseId);
                }
            }
            result.add(new WorkoutTemplate(t.getId(), t.getName(), visible));
        }
        result.sort(BY_NAME);
        return result;
    }

    /** Creates a template from active exercises. */
    public WorkoutTemplate create(String name, List<String> exerciseIds) {
        WorkoutTemplate t = new WorkoutTemplate(UUID.randomUUID().toString(), name, exerciseIds);
        requireUsable(t.getExerciseIds());
        templates.insert(t);
        return t;
    }

    /** Renames a template and replaces its exercise list. */
    public WorkoutTemplate update(String id, String name, List<String> exerciseIds) {
        WorkoutTemplate t = get(id);
        t.setName(name);
        t.setExerciseIds(exerciseIds);
        requireUsable(t.getExerciseIds());
        templates.update(t);
        return t;
    }

    /** Deletes a template (hard delete). */
    public void delete(String id) {
        get(id);
        templates.delete(id);
    }

    /** Saves a session's exercises as a template, in the order each was first logged. */
    public WorkoutTemplate saveFromSession(String sessionId, String name) {
        WorkoutSession s = sessions.findById(sessionId);
        if (s == null) {
            throw new NotFoundException("session", sessionId);
        }
        List<String> ids = new ArrayList<>();
        for (WorkoutSet set : s.getSets()) { // ordered by completion time
            Exercise e = exercises.findById(set.getExerciseId());
            if (!ids.contains(set.getExerciseId()) && e != null && !e.isDeleted()) {
                ids.add(set.getExerciseId());
            }
        }
        return create(name, ids);
    }

    private WorkoutTemplate get(String id) {
        WorkoutTemplate t = templates.findById(id);
        if (t == null) {
            throw new NotFoundException(RESOURCE, id);
        }
        return t;
    }

    private void requireUsable(List<String> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            throw new InvalidWorkoutInputException("A template needs at least one exercise");
        }
        for (String exerciseId : exerciseIds) {
            Exercise e = exercises.findById(exerciseId);
            if (e == null) {
                throw new NotFoundException("exercise", exerciseId);
            }
            if (e.isDeleted()) {
                throw new InvalidWorkoutInputException("A template cannot contain a deleted exercise");
            }
        }
    }
}
