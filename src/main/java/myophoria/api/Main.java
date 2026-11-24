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
import java.util.Locale;
import java.util.Set;
import myophoria.core.service.BodyweightService;
import myophoria.core.service.CsvService;
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
                allowed.add(host.trim().toLowerCase(Locale.ROOT));
            }
        }
        Database db = new Database(dataDir);
        List<String> warnings = db.open();
        createApp(db, db.backupDir(), warnings, allowed).start("127.0.0.1", port);
    }

    /** Wires repositories, services and controllers and returns the (not yet started) server. */
    static Javalin createApp(Database db, Path backupDir, List<String> warnings, Set<String> allowedHosts) {
        Clock clock = Clock.systemUTC();
        // adapters (persistence)
        SqliteGymRepository gymRepo = new SqliteGymRepository(db);
        SqliteExerciseRepository exerciseRepo = new SqliteExerciseRepository(db);
        SqliteSessionRepository sessionRepo = new SqliteSessionRepository(db);
        SqliteSetRepository setRepo = new SqliteSetRepository(db);
        SqliteBodyweightRepository bodyweightRepo = new SqliteBodyweightRepository(db);
        SqliteTemplateRepository templateRepo = new SqliteTemplateRepository(db);
        SqliteBackups backups = new SqliteBackups(db, backupDir, clock);
        // use cases (core)
        ExerciseService exercises = new ExerciseService(exerciseRepo, backups, clock);
        GymService gyms = new GymService(gymRepo, sessionRepo, clock);
        WorkoutService workouts = new WorkoutService(sessionRepo, setRepo, exerciseRepo, gymRepo, bodyweightRepo, clock);
        HistoryService history = new HistoryService(sessionRepo, gymRepo);
        ReportService reports = new ReportService(sessionRepo, exerciseRepo);
        BodyweightService bodyweight = new BodyweightService(bodyweightRepo);
        TemplateService templates = new TemplateService(templateRepo, exerciseRepo, sessionRepo);
        CsvService csv = new CsvService(gymRepo, exerciseRepo, sessionRepo, setRepo, bodyweightRepo, templateRepo,
                backups, clock);
        // HTTP adapters
        DataController dataController = new DataController(warnings, backups, csv);
        ExerciseController exerciseController = new ExerciseController(exercises);
        GymController gymController = new GymController(gyms);
        WorkoutController workoutController = new WorkoutController(workouts, exercises);
        HistoryController historyController = new HistoryController(history, exercises);
        ReportController reportController = new ReportController(reports);
        BodyweightController bodyweightController = new BodyweightController(bodyweight);
        TemplateController templateController = new TemplateController(templates, exercises);
        return Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(jsonMapper(), false));
            config.http.maxRequestSize = 64L * 1024 * 1024; // imports; CsvService caps the unpacked size too
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

    /**
     * Jackson setup: ISO dates and times; an explicit null number is an error (a missing one becomes 0, which the
     * entity setters reject wherever 0 is invalid).
     */
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
        host = host.toLowerCase(Locale.ROOT);
        return LOCAL_HOSTS.contains(host) || allowedHosts.contains(host);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
