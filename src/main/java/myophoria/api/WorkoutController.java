package myophoria.api;

import io.javalin.config.RoutesConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import myophoria.core.model.Exercise;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.service.ExerciseService;
import myophoria.core.service.SetInput;
import myophoria.core.service.WorkoutService;

/** HTTP adapter for the live workout: open session, start, finish, sets and prefill. */
public class WorkoutController {
    /** Request body for starting a workout. */
    public record StartRequest(String gymId, LocalDate localDate) {
    }

    /** Set as sent to the browser; display comes from Exercise.formatSet. */
    public record SetDto(String id, String sessionId, String exerciseId, String exerciseName, int position,
            SetType setType, double weight, Unit unit, int reps, Double bodyweight, Instant completedAt,
            String display) {
        static SetDto of(WorkoutSet s, Exercise e) {
            return new SetDto(s.getId(), s.getSessionId(), s.getExerciseId(), e == null ? null : e.getName(),
                    s.getPosition(), s.getSetType(), s.getWeight(), s.getUnit(), s.getReps(), s.getBodyweight(),
                    s.getCompletedAt(), e == null ? null : e.formatSet(s.getWeight(), s.getUnit(), s.getBodyweight()));
        }
    }

    /** Session with its sets as sent to the browser. */
    public record SessionDto(String id, String gymId, LocalDate localDate, Instant startedAt, Instant endedAt,
            boolean open, List<SetDto> sets) {
        static SessionDto of(WorkoutSession s, Map<String, Exercise> exercises) {
            List<SetDto> sets = new ArrayList<>();
            for (WorkoutSet set : s.getSets()) {
                sets.add(SetDto.of(set, exercises.get(set.getExerciseId())));
            }
            return new SessionDto(s.getId(), s.getGymId(), s.getLocalDate(), s.getStartedAt(), s.getEndedAt(),
                    s.isOpen(), sets);
        }
    }

    private final WorkoutService workouts;
    private final ExerciseService exercises;

    /** Creates the controller on its services. */
    public WorkoutController(WorkoutService workouts, ExerciseService exercises) {
        this.workouts = workouts;
        this.exercises = exercises;
    }

    /** Registers the routes; must run before HistoryController so /sessions/open wins over /sessions/{id}. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/sessions/open", ctx -> {
            WorkoutSession open = workouts.open();
            if (open == null) {
                ctx.status(204);
            } else {
                ctx.json(SessionDto.of(open, exercises.allById()));
            }
        });
        routes.post("/api/v1/sessions/start", ctx -> {
            StartRequest r = ctx.bodyAsClass(StartRequest.class);
            ctx.status(201).json(SessionDto.of(workouts.start(r.gymId(), r.localDate()), Map.of()));
        });
        routes.post("/api/v1/sessions/{id}/finish",
                ctx -> ctx.json(SessionDto.of(workouts.finish(ctx.pathParam("id")), exercises.allById())));
        routes.get("/api/v1/sessions/{id}/prefill",
                ctx -> ctx.json(workouts.prefill(ctx.pathParam("id"), ctx.queryParam("exerciseId"))));
        routes.post("/api/v1/sessions/{id}/sets", ctx -> {
            WorkoutSet set = workouts.addSet(ctx.pathParam("id"), ctx.bodyAsClass(SetInput.class));
            ctx.status(201).json(SetDto.of(set, exercises.get(set.getExerciseId())));
        });
        routes.put("/api/v1/sets/{id}", ctx -> {
            WorkoutSet set = workouts.updateSet(ctx.pathParam("id"), ctx.bodyAsClass(SetInput.class));
            ctx.json(SetDto.of(set, exercises.get(set.getExerciseId())));
        });
        routes.delete("/api/v1/sets/{id}", ctx -> {
            workouts.deleteSet(ctx.pathParam("id"));
            ctx.status(204);
        });
    }
}
