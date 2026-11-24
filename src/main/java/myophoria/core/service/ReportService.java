package myophoria.core.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import myophoria.core.error.NotFoundException;
import myophoria.core.model.Exercise;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.port.ExerciseRepository;
import myophoria.core.port.SessionRepository;

/** Read-only numbers: the finish summary and the progression chart (handoff §7.2, §7.5). */
public class ReportService {
    private final SessionRepository sessions;
    private final ExerciseRepository exercises;

    /** Creates the service on its repositories. */
    public ReportService(SessionRepository sessions, ExerciseRepository exercises) {
        this.sessions = sessions;
        this.exercises = exercises;
    }

    /** Summarizes a session; exercises appear in the order of their first set, warm-ups are excluded from numbers. */
    public SessionSummary summary(String sessionId, Unit unit) {
        WorkoutSession s = sessions.findById(sessionId);
        if (s == null) {
            throw new NotFoundException("session", sessionId);
        }
        Unit u = unit == null ? Unit.KG : unit;
        // sets arrive ordered by completion time, so insertion order = order of each exercise's first set
        Map<String, List<WorkoutSet>> byExercise = new LinkedHashMap<>();
        int workingSets = 0;
        for (WorkoutSet set : s.getSets()) {
            byExercise.computeIfAbsent(set.getExerciseId(), id -> new ArrayList<>()).add(set);
            if (set.getSetType().isWorking()) {
                workingSets++;
            }
        }
        List<ExerciseSummary> perExercise = new ArrayList<>();
        for (Map.Entry<String, List<WorkoutSet>> entry : byExercise.entrySet()) {
            perExercise.add(summarize(entry.getKey(), entry.getValue(), u));
        }
        Duration duration = s.duration();
        return new SessionSummary(s.getId(), s.getLocalDate(), s.getStartedAt(), s.effectiveEnd(),
                duration == null ? null : duration.toSeconds(), u, s.getSets().size(), workingSets, s.volume(u),
                perExercise);
    }

    /** Returns the top working weight per session for an exercise, oldest first, in the reporting unit. */
    public List<ProgressPoint> progression(String exerciseId, Unit unit) {
        if (exercises.findById(exerciseId) == null) {
            throw new NotFoundException("exercise", exerciseId);
        }
        Unit u = unit == null ? Unit.KG : unit;
        List<WorkoutSession> all = sessions.findAll();
        all.sort(GymService.CHRONOLOGICAL);
        List<ProgressPoint> points = new ArrayList<>();
        for (WorkoutSession s : all) {
            Double top = null;
            for (WorkoutSet set : s.getSets()) {
                if (set.getExerciseId().equals(exerciseId) && set.getSetType().isWorking()) {
                    double w = set.weightIn(u);
                    if (top == null || w > top) {
                        top = w;
                    }
                }
            }
            if (top != null) {
                points.add(new ProgressPoint(s.getLocalDate(), top));
            }
        }
        return points;
    }

    private ExerciseSummary summarize(String exerciseId, List<WorkoutSet> sets, Unit u) {
        int working = 0;
        double sum = 0;
        double volume = 0;
        Double top = null;
        for (WorkoutSet set : sets) {
            volume += set.volume(u);
            if (set.getSetType().isWorking()) {
                double w = set.weightIn(u);
                working++;
                sum += w;
                if (top == null || w > top) {
                    top = w;
                }
            }
        }
        Exercise e = exercises.findById(exerciseId);
        return new ExerciseSummary(exerciseId, e == null ? null : e.getName(), working, top,
                working == 0 ? null : sum / working, volume);
    }
}
