package myophoria.api;

import io.javalin.config.RoutesConfig;
import java.util.Locale;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.model.Unit;
import myophoria.core.service.ReportService;

/** HTTP adapter for the session summary and the progression chart. */
public class ReportController {
    private final ReportService reports;

    /** Creates the controller on the service. */
    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    /** Registers the routes. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/sessions/{id}/summary",
                ctx -> ctx.json(reports.summary(ctx.pathParam("id"), unit(ctx.queryParam("unit")))));
        routes.get("/api/v1/exercises/{id}/progression",
                ctx -> ctx.json(reports.progression(ctx.pathParam("id"), unit(ctx.queryParam("unit")))));
    }

    /** Parses the ?unit= query parameter: kg (default) or lb. */
    static Unit unit(String text) {
        if (text == null || text.isBlank()) {
            return Unit.KG;
        }
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "kg" -> Unit.KG;
            case "lb" -> Unit.LB;
            default -> throw new InvalidWorkoutInputException("unit must be kg or lb");
        };
    }
}
