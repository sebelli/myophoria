package myophoria.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import myophoria.persistence.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Starts the real app on a random port with a temp database; subclasses call the API with call(). */
abstract class ApiTestSupport {
    @TempDir
    Path dir;
    Javalin app;
    String base;
    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void start() {
        Database db = new Database(dir);
        List<String> warnings = db.open();
        app = Main.createApp(db, warnings, Set.of("lift.example.ts.net")).start("127.0.0.1", 0);
        base = "http://127.0.0.1:" + app.port() + "/api/v1";
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    protected HttpResponse<String> call(String method, String path, String body, String... headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).method(method,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            b.header("Content-Type", "application/json");
        }
        for (int i = 0; i < headers.length; i += 2) {
            b.header(headers[i], headers[i + 1]);
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected JsonNode body(HttpResponse<String> r) throws Exception {
        return json.readTree(r.body());
    }

    protected JsonNode post(String path, String json, int expectedStatus) throws Exception {
        HttpResponse<String> r = call("POST", path, json);
        org.junit.jupiter.api.Assertions.assertEquals(expectedStatus, r.statusCode(), r.body());
        return r.body().isEmpty() ? null : body(r);
    }

    protected JsonNode get(String path) throws Exception {
        HttpResponse<String> r = call("GET", path, null);
        org.junit.jupiter.api.Assertions.assertEquals(200, r.statusCode(), r.body());
        return body(r);
    }

    protected String createExercise(String name, String type) throws Exception {
        return post("/exercises", "{\"name\":\"" + name + "\",\"equipmentType\":\"" + type
                + "\",\"laterality\":\"BILATERAL\"}", 201).get("id").asText();
    }
}
