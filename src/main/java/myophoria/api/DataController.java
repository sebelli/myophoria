package myophoria.api;

import io.javalin.config.RoutesConfig;
import java.util.List;
import myophoria.core.port.Backups;

/** Status, backups, export and import. */
public class DataController {
    /** Response of GET /status. */
    public record StatusResponse(List<String> warnings) {
    }

    /** Response of POST /backups. */
    public record BackupResponse(String file) {
    }

    private final List<String> warnings;
    private final Backups backups;

    /** Creates the controller with the warnings recorded at startup and the backup port. */
    public DataController(List<String> warnings, Backups backups) {
        this.warnings = warnings;
        this.backups = backups;
    }

    /** Registers the routes. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/status", ctx -> ctx.json(new StatusResponse(warnings)));
        routes.post("/api/v1/backups", ctx -> ctx.json(new BackupResponse(backups.createBackup())));
    }
}
