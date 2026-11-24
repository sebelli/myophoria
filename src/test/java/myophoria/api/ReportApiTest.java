package myophoria.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class ReportApiTest extends ApiTestSupport {
    @Test
    void summaryAndProgression() throws Exception {
        String gym = get("/gyms/active").get("id").asText();
        String squat = createExercise("Squat", "BARBELL");
        String id = post("/sessions/start", "{\"gymId\":\"" + gym + "\",\"localDate\":\"2026-11-25\"}", 201).get("id").asText();
        post("/sessions/" + id + "/sets", "{\"exerciseId\":\"" + squat + "\",\"setType\":\"WORKING\",\"weight\":100,"
                + "\"unit\":\"KG\",\"reps\":5,\"position\":1}", 201);
        post("/sessions/" + id + "/finish", null, 200);

        JsonNode kg = get("/sessions/" + id + "/summary");
        assertEquals("KG", kg.get("unit").asText());
        assertEquals(500.0, kg.get("volume").asDouble(), 1e-9);
        assertEquals("Squat", kg.get("exercises").get(0).get("name").asText());
        JsonNode lb = get("/sessions/" + id + "/summary?unit=lb");
        assertEquals(500 / 0.45359237, lb.get("volume").asDouble(), 1e-9);
        assertEquals(400, call("GET", "/sessions/" + id + "/summary?unit=stone", null).statusCode());

        JsonNode points = get("/exercises/" + squat + "/progression?unit=kg");
        assertEquals(1, points.size());
        assertEquals("2026-11-25", points.get(0).get("localDate").asText());
        assertEquals(100.0, points.get(0).get("topWeight").asDouble());
        assertEquals(404, call("GET", "/exercises/nope/progression", null).statusCode());
    }
}
