package myophoria.api;

import io.javalin.config.RoutesConfig;
import java.time.Instant;
import java.time.LocalDate;
import myophoria.api.WorkoutController.SessionDto;
import myophoria.core.service.ExerciseService;
import myophoria.core.service.HistoryFilter;
import myophoria.core.service.HistoryService;

/** HTTP adapter for history: list, view, backfill, edit, delete. */
public class HistoryController {
    /** Request body for backfill and edit. */
    public record SessionRequest(String gymId, LocalDate localDate, Instant startedAt, Instant endedAt) {
    }

    private final HistoryService history;
    private final ExerciseService exercises;

    /** Creates the controller on its services. */
    public HistoryController(HistoryService history, ExerciseService exercises) {
        this.history = history;
        this.exercises = exercises;
    }

    /** Registers the routes (after WorkoutController, see there). */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/sessions", ctx -> ctx.json(history.list(new HistoryFilter(
                date(ctx.queryParam("from")), date(ctx.queryParam("to")),
                blankToNull(ctx.queryParam("gymId")), blankToNull(ctx.queryParam("exerciseId"))))));
        routes.get("/api/v1/sessions/{id}",
                ctx -> ctx.json(SessionDto.of(history.get(ctx.pathParam("id")), exercises.allById())));
        routes.post("/api/v1/sessions", ctx -> {
            SessionRequest r = ctx.bodyAsClass(SessionRequest.class);
            ctx.status(201).json(SessionDto.of(
                    history.backfill(r.gymId(), r.localDate(), r.startedAt(), r.endedAt()), exercises.allById()));
        });
        routes.put("/api/v1/sessions/{id}", ctx -> {
            SessionRequest r = ctx.bodyAsClass(SessionRequest.class);
            ctx.json(SessionDto.of(history.edit(ctx.pathParam("id"), r.gymId(), r.localDate(), r.startedAt(),
                    r.endedAt()), exercises.allById()));
        });
        routes.delete("/api/v1/sessions/{id}", ctx -> {
            history.delete(ctx.pathParam("id"));
            ctx.status(204);
        });
    }

    private static LocalDate date(String text) {
        return text == null || text.isBlank() ? null : LocalDate.parse(text); // bad format → 400 via ErrorHandlers
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
