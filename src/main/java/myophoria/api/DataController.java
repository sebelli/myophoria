package myophoria.api;

import io.javalin.config.RoutesConfig;
import java.util.List;

/** Status endpoint (export, import and backups arrive in Phase 4). */
public class DataController {
    /** Response of GET /status. */
    public record StatusResponse(List<String> warnings) {
    }

    private final List<String> warnings;

    /** Creates the controller with the warnings recorded at startup. */
    public DataController(List<String> warnings) {
        this.warnings = warnings;
    }

    /** Registers the routes. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/status", ctx -> ctx.json(new StatusResponse(warnings)));
    }
}
