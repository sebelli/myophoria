package myophoria.api;

import io.javalin.config.RoutesConfig;
import io.javalin.http.UploadedFile;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.service.CsvService;
import myophoria.persistence.SqliteBackups;
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
    private final CsvService csv;

    /** Creates the controller with the warnings recorded at startup, the backup port and the CSV service. */
    public DataController(List<String> warnings, Backups backups, CsvService csv) {
        this.warnings = warnings;
        this.backups = backups;
        this.csv = csv;
    }

    /** Registers the routes. */
    public void register(RoutesConfig routes) {
        routes.get("/api/v1/status", ctx -> ctx.json(new StatusResponse(warnings)));
        routes.post("/api/v1/backups", ctx -> ctx.json(new BackupResponse(backups.createBackup())));
        routes.get("/api/v1/export", ctx -> {
            ByteArrayOutputStream zip = new ByteArrayOutputStream(); // built first, so a failure is still a JSON error
            csv.export(zip);
            String name = "myophoria-" + SqliteBackups.FILE_TIME.format(Instant.now()) + ".zip";
            ctx.contentType("application/zip")
                    .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                    .result(zip.toByteArray());
        });
        routes.post("/api/v1/import", ctx -> {
            UploadedFile file = ctx.isMultipartFormData() ? ctx.uploadedFile("file") : null;
            if (file == null) {
                throw new InvalidWorkoutInputException("Send the ZIP as multipart form field \"file\"");
            }
            try (InputStream in = file.content()) {
                ctx.json(csv.importZip(in));
            }
        });
    }
}
