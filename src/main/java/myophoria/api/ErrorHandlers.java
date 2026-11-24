package myophoria.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import java.util.Map;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.error.DuplicateResourceException;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Maps exceptions to the JSON error format of PLAN §6.1; unknown exceptions never leak their message. */
public final class ErrorHandlers {
    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlers.class);

    /** JSON body of every error response; details only appears for duplicates. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String error, String message, Map<String, Object> details) {
    }

    private ErrorHandlers() {
    }

    /** Registers one handler per exception type on the Javalin config. */
    public static void register(JavalinConfig config) {
        config.routes.exception(InvalidWorkoutInputException.class,
                (e, ctx) -> send(ctx, 400, "INVALID_INPUT", e.getMessage(), null));
        config.routes.exception(NotFoundException.class,
                (e, ctx) -> send(ctx, 404, "NOT_FOUND", e.getMessage(), null));
        config.routes.exception(DuplicateResourceException.class, (e, ctx) -> send(ctx, 409, "DUPLICATE_RESOURCE",
                e.getMessage(), Map.of("resourceType", e.getResourceType(), "existingId", e.getExistingId(),
                        "softDeleted", e.isSoftDeleted())));
        config.routes.exception(CorruptedCsvException.class,
                (e, ctx) -> send(ctx, 422, "CORRUPTED_CSV", e.getMessage(), null));
        // Jackson errors (bad JSON, unknown enum value, unknown field) are the client's fault; parser text stays in the log
        config.routes.exception(JsonProcessingException.class, (e, ctx) -> {
            LOG.debug("Rejected request body on {} {}", ctx.method(), ctx.path(), e);
            send(ctx, 400, "INVALID_INPUT", "Request body is not valid JSON for this endpoint", null);
        });
        config.routes.exception(HttpResponseException.class, (e, ctx) -> send(ctx, e.getStatus(), codeFor(e.getStatus()),
                e.getStatus() < 500 ? e.getMessage() : "Unexpected error", null));
        config.routes.exception(Exception.class, (e, ctx) -> {
            LOG.error("Unhandled error on {} {}", ctx.method(), ctx.path(), e);
            send(ctx, 500, "INTERNAL", "Unexpected error", null);
        });
    }

    private static String codeFor(int status) {
        return switch (status) {
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            default -> status < 500 ? "INVALID_INPUT" : "INTERNAL";
        };
    }

    private static void send(Context ctx, int status, String code, String message, Map<String, Object> details) {
        ctx.status(status).json(new ErrorBody(code, message, details));
    }
}
