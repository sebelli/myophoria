package myophoria.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import myophoria.core.service.BodyweightService;
import myophoria.core.service.ExerciseService;
import myophoria.core.service.GymService;
import myophoria.core.service.HistoryService;
import myophoria.core.service.ReportService;
import myophoria.core.service.TemplateService;
import myophoria.core.service.WorkoutService;
import myophoria.persistence.Database;
import myophoria.persistence.SqliteBackups;
import myophoria.persistence.SqliteBodyweightRepository;
import myophoria.persistence.SqliteExerciseRepository;
import myophoria.persistence.SqliteGymRepository;
import myophoria.persistence.SqliteSessionRepository;
import myophoria.persistence.SqliteSetRepository;
import myophoria.persistence.SqliteTemplateRepository;
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
        createApp(db, dataDir.resolve("backups"), warnings, allowed).start("127.0.0.1", port);
    }

    /** Wires repositories, services and controllers and returns the (not yet started) server. */
    static Javalin createApp(Database db, Path backupDir, List<String> warnings, Set<String> allowedHosts) {
        Clock clock = Clock.systemUTC();
        SqliteSessionRepository sessionRepo = new SqliteSessionRepository(db);
        SqliteGymRepository gymRepo = new SqliteGymRepository(db);
        SqliteExerciseRepository exerciseRepo = new SqliteExerciseRepository(db);
        SqliteBodyweightRepository bodyweightRepo = new SqliteBodyweightRepository(db);
        ExerciseService exercises = new ExerciseService(exerciseRepo, clock);
        WorkoutService workouts = new WorkoutService(sessionRepo, new SqliteSetRepository(db), exerciseRepo, gymRepo,
                bodyweightRepo, clock);
        WorkoutController workoutController = new WorkoutController(workouts, exercises);
        TemplateController templateController = new TemplateController(
                new TemplateService(new SqliteTemplateRepository(db), exerciseRepo, sessionRepo), exercises);
        ReportController reportController = new ReportController(new ReportService(sessionRepo, exerciseRepo));
        HistoryController historyController = new HistoryController(new HistoryService(sessionRepo, gymRepo), exercises);
        GymController gymController = new GymController(new GymService(gymRepo, sessionRepo, clock));
        ExerciseController exerciseController = new ExerciseController(exercises);
        SqliteBackups backups = new SqliteBackups(db, backupDir, clock);
        DataController dataController = new DataController(warnings, backups);
        BodyweightController bodyweightController =
                new BodyweightController(new BodyweightService(bodyweightRepo));
        return Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(jsonMapper(), false));
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
            bodyweightController.register(config.routes);
            gymController.register(config.routes);
            workoutController.register(config.routes); // before history routes: /sessions/open vs /sessions/{id}
            historyController.register(config.routes);
            reportController.register(config.routes);
            templateController.register(config.routes);
        });
    }

    /** Jackson setup: ISO dates and times; an explicit null number is an error (a missing one is 0, which entity checks reject where 0 is invalid). */
    static ObjectMapper jsonMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
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
