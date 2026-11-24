package myophoria.api;

import static org.junit.jupiter.api.Assertions.*;

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

class ApiTest {
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

    private HttpResponse<String> call(String method, String path, String body, String... headers) throws Exception {
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

    private JsonNode body(HttpResponse<String> r) throws Exception {
        return json.readTree(r.body());
    }

    private String create(String name) throws Exception {
        HttpResponse<String> r = call("POST", "/exercises",
                "{\"name\":\"" + name + "\",\"equipmentType\":\"DUMBBELL\",\"laterality\":\"BILATERAL\"}");
        assertEquals(201, r.statusCode());
        return body(r).get("id").asText();
    }

    @Test
    void statusHasNoWarnings() throws Exception {
        HttpResponse<String> r = call("GET", "/status", null);
        assertEquals(200, r.statusCode());
        assertEquals(0, body(r).get("warnings").size());
    }

    @Test
    void createListUpdateDeleteRestore() throws Exception {
        String id = create("Incline Dumbbell Press");
        JsonNode got = body(call("GET", "/exercises/" + id, null));
        assertEquals("IDP", got.get("initials").asText());
        assertEquals("per dumbbell", got.get("weightLabel").asText());
        assertFalse(got.get("deleted").asBoolean());

        assertEquals(1, body(call("GET", "/exercises?q=incline&type=DUMBBELL,STACK", null)).size());
        assertEquals(0, body(call("GET", "/exercises?type=BARBELL", null)).size());

        HttpResponse<String> put = call("PUT", "/exercises/" + id,
                "{\"name\":\"Flat Press\",\"equipmentType\":\"BARBELL\",\"laterality\":\"UNILATERAL\"}");
        assertEquals(200, put.statusCode());
        assertEquals("total", body(put).get("weightLabel").asText());

        assertEquals(204, call("DELETE", "/exercises/" + id, null).statusCode());
        assertEquals(0, body(call("GET", "/exercises", null)).size());
        assertEquals(1, body(call("GET", "/exercises?deleted=true", null)).size());

        HttpResponse<String> restored = call("POST", "/exercises/" + id + "/restore", null);
        assertEquals(200, restored.statusCode());
        assertFalse(body(restored).get("deleted").asBoolean());
    }

    @Test
    void duplicateNameIs409WithDetails() throws Exception {
        String id = create("Curl");
        HttpResponse<String> r = call("POST", "/exercises",
                "{\"name\":\" curl \",\"equipmentType\":\"STACK\",\"laterality\":\"BILATERAL\"}");
        assertEquals(409, r.statusCode());
        JsonNode b = body(r);
        assertEquals("DUPLICATE_RESOURCE", b.get("error").asText());
        assertEquals(id, b.get("details").get("existingId").asText());
        assertEquals("exercise", b.get("details").get("resourceType").asText());
        assertFalse(b.get("details").get("softDeleted").asBoolean());
    }

    @Test
    void badInputIs400Json() throws Exception {
        String[] bad = {
            "{not json",
            "{\"name\":\"\",\"equipmentType\":\"STACK\",\"laterality\":\"BILATERAL\"}",
            "{\"name\":\"X\",\"equipmentType\":\"NOPE\",\"laterality\":\"BILATERAL\"}",
            "{\"name\":\"X\",\"laterality\":\"BILATERAL\"}",
            "{\"name\":\"X\",\"equipmentType\":\"STACK\",\"laterality\":\"BILATERAL\",\"extra\":1}"};
        for (String b : bad) {
            HttpResponse<String> r = call("POST", "/exercises", b);
            assertEquals(400, r.statusCode(), b);
            assertEquals("INVALID_INPUT", body(r).get("error").asText(), b);
        }
        assertEquals(400, call("GET", "/exercises?type=NOPE", null).statusCode());
        assertEquals(400, call("GET", "/exercises?deleted=maybe", null).statusCode());
    }

    @Test
    void unknownIdAndRouteAre404Json() throws Exception {
        HttpResponse<String> r = call("GET", "/exercises/nope", null);
        assertEquals(404, r.statusCode());
        assertEquals("NOT_FOUND", body(r).get("error").asText());
        HttpResponse<String> route = call("GET", "/nothing-here", null);
        assertEquals(404, route.statusCode());
        assertEquals("NOT_FOUND", body(route).get("error").asText());
    }

    @Test
    void foreignOriginIs403() throws Exception {
        HttpResponse<String> r = call("GET", "/status", null, "Origin", "http://evil.com");
        assertEquals(403, r.statusCode());
        assertEquals("FORBIDDEN", body(r).get("error").asText());
        assertEquals(200, call("GET", "/status", null, "Origin", "http://localhost:5173").statusCode());
        assertEquals(200, call("GET", "/status", null, "Origin", "https://lift.example.ts.net").statusCode());
        assertEquals(403, call("GET", "/status", null, "Origin", "http://evil.com@localhost").statusCode());
    }

    @Test
    void foreignHostIs403() throws Exception {
        assertEquals(403, rawStatus("evil.com"));
        assertEquals(200, rawStatus("localhost:7070"));
        assertEquals(200, rawStatus("lift.example.ts.net"));
        assertEquals(403, rawStatus("localhost.evil.com"));
        assertNotEquals(200, rawStatus("evil.com@localhost")); // Jetty already answers 400 here
    }

    // HttpClient forbids setting Host, so speak HTTP over a plain socket
    private int rawStatus(String host) throws Exception {
        try (Socket s = new Socket("127.0.0.1", app.port())) {
            s.getOutputStream().write(("GET /api/v1/status HTTP/1.1\r\nHost: " + host
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            InputStream in = s.getInputStream();
            String first = new String(in.readNBytes(12), StandardCharsets.US_ASCII); // "HTTP/1.1 200"
            return Integer.parseInt(first.substring(9, 12));
        }
    }
}
