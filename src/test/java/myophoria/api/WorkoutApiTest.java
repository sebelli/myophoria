package myophoria.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class WorkoutApiTest extends ApiTestSupport {
    private String gym() throws Exception {
        return get("/gyms/active").get("id").asText();
    }

    private static String set(String exerciseId, String type, String weight, int reps, String bw, int pos) {
        return "{\"exerciseId\":\"" + exerciseId + "\",\"setType\":\"" + type + "\",\"weight\":" + weight
                + ",\"unit\":\"KG\",\"reps\":" + reps + ",\"bodyweight\":" + bw + ",\"position\":" + pos + "}";
    }

    @Test
    void liveWorkoutWalkthrough() throws Exception {
        String squat = createExercise("Squat", "BARBELL");
        String pullUp = createExercise("Pull Up", "CALISTHENICS");
        assertEquals(204, call("GET", "/sessions/open", null).statusCode());

        String start = "{\"gymId\":\"" + gym() + "\",\"localDate\":\"2026-11-25\"}";
        JsonNode session = post("/sessions/start", start, 201);
        String id = session.get("id").asText();
        assertTrue(session.get("open").asBoolean());
        JsonNode again = body(call("POST", "/sessions/start", start));
        assertEquals("A workout is already open", again.get("message").asText());

        JsonNode s1 = post("/sessions/" + id + "/sets", set(squat, "WORKING", "100", 5, "null", 1), 201);
        assertEquals("100 kg", s1.get("display").asText());
        assertEquals("Squat", s1.get("exerciseName").asText());
        JsonNode p1 = post("/sessions/" + id + "/sets", set(pullUp, "WORKING", "20", 8, "70", 1), 201);
        assertEquals("70 | 20 kg", p1.get("display").asText());
        assertEquals(400, call("POST", "/sessions/" + id + "/sets", set(pullUp, "WORKING", "20", 8, "null", 1)).statusCode());
        assertEquals(400, call("POST", "/sessions/" + id + "/sets", set(squat, "WORKING", "null", 5, "null", 1)).statusCode());

        JsonNode edited = body(call("PUT", "/sets/" + s1.get("id").asText(), set(squat, "FAILURE", "102.5", 3, "null", 1)));
        assertEquals("102.5 kg", edited.get("display").asText());

        JsonNode open = get("/sessions/open");
        assertEquals(2, open.get("sets").size());

        JsonNode prefill = get("/sessions/" + id + "/prefill?exerciseId=" + squat);
        assertEquals("KG", prefill.get("unit").asText());
        assertEquals(0, prefill.get("rows").size()); // no earlier session

        JsonNode finished = post("/sessions/" + id + "/finish", null, 200);
        assertFalse(finished.get("open").asBoolean());
        assertNotNull(finished.get("endedAt").asText());

        // a second workout gets the first one's sets as prefill
        String second = post("/sessions/start", "{\"gymId\":\"" + gym() + "\",\"localDate\":\"2026-11-26\"}", 201)
                .get("id").asText();
        JsonNode p2 = get("/sessions/" + second + "/prefill?exerciseId=" + squat);
        assertEquals(1, p2.get("rows").size());
        assertEquals(102.5, p2.get("rows").get(0).get("weight").asDouble());
        assertEquals(82.0, p2.get("warmupWeight").asDouble(), 1e-9);

        assertEquals(204, call("DELETE", "/sets/" + p1.get("id").asText(), null).statusCode());
        assertEquals(404, call("DELETE", "/sets/" + p1.get("id").asText(), null).statusCode());
        assertEquals(400, call("POST", "/sessions/" + second + "/finish", null).statusCode()); // no sets
    }
}
