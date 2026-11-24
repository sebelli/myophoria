package myophoria.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import myophoria.core.service.ExerciseService;
import myophoria.persistence.Database;
import myophoria.persistence.SqliteExerciseRepository;
import org.slf4j.LoggerFactory;

/** Application entry point: reads the environment, wires the layers by hand and starts the server on localhost. */
public class Main {
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1");

    /** Starts the app; env: MYOPHORIA_PORT (7070), MYOPHORIA_DATA_DIR (./data), MYOPHORIA_ALLOWED_HOSTS (comma list). */
    public static void main(String[] args) {
        int port = Integer.parseInt(env("MYOPHORIA_PORT", "7070"));
        Path dataDir = Path.of(env("MYOPHORIA_DATA_DIR", "data"));
        Set<String> allowed = new HashSet<>();
        for (String host : env("MYOPHORIA_ALLOWED_HOSTS", "").split(",")) {
            if (!host.isBlank()) {
                allowed.add(host.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
        Database db = new Database(dataDir);
        List<String> warnings = db.open();
        createApp(db, warnings, allowed).start("127.0.0.1", port);
    }

    /** Wires repositories, services and controllers and returns the (not yet started) server. */
    static Javalin createApp(Database db, List<String> warnings, Set<String> allowedHosts) {
        ExerciseService exercises = new ExerciseService(new SqliteExerciseRepository(db), Clock.systemUTC());
        ExerciseController exerciseController = new ExerciseController(exercises);
        DataController dataController = new DataController(warnings);
        return Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(new ObjectMapper(), false));
            if (Main.class.getResource("/public") != null) { // absent on a fresh clone before the web build
                config.staticFiles.add("/public", Location.CLASSPATH);
            } else {
                LoggerFactory.getLogger(Main.class).warn("No web build on the classpath; serving the API only");
            }
            ErrorHandlers.register(config);
            config.routes.before("/api/*", ctx -> {
                if (!hostAllowed(ctx.header("Host"), true, allowedHosts)
                        || !hostAllowed(ctx.header("Origin"), false, allowedHosts)) {
                    throw new ForbiddenResponse("Host or origin not allowed");
                }
            });
            dataController.register(config.routes);
            exerciseController.register(config.routes);
        });
    }

    /** Checks a Host header (host[:port]) or an Origin header (scheme://host[:port]); an absent Origin is fine. */
    static boolean hostAllowed(String header, boolean required, Set<String> allowedHosts) {
        if (header == null) {
            return !required;
        }
        if (header.contains("@")) {
            return false;
        }
        String host;
        try {
            host = required ? URI.create("//" + header).getHost() : URI.create(header).getHost();
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (host == null) {
            return false;
        }
        host = host.toLowerCase(java.util.Locale.ROOT);
        return LOCAL_HOSTS.contains(host) || allowedHosts.contains(host);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
