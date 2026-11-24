package myophoria.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class HistoryApiTest extends ApiTestSupport {
    @Test
    void backfillFilterEditDelete() throws Exception {
        String gym = get("/gyms/active").get("id").asText();
        String away = post("/gyms", "{\"name\":\"Away\"}", 201).get("id").asText();
        String squat = createExercise("Squat", "BARBELL");

        String a = post("/sessions", "{\"gymId\":\"" + gym + "\",\"localDate\":\"2026-11-20\"}", 201).get("id").asText();
        JsonNode b = post("/sessions", "{\"gymId\":\"" + away + "\",\"localDate\":\"2026-11-22\","
                + "\"startedAt\":\"2026-11-22T07:00:00Z\",\"endedAt\":\"2026-11-22T08:00:00Z\"}", 201);
        assertFalse(b.get("open").asBoolean());
        post("/sessions/" + a + "/sets", "{\"exerciseId\":\"" + squat + "\",\"setType\":\"WORKING\",\"weight\":100,"
                + "\"unit\":\"KG\",\"reps\":5,\"position\":1}", 201);

        JsonNode all = get("/sessions");
        assertEquals(2, all.size());
        assertEquals(b.get("id"), all.get(0).get("id")); // newest first
        assertEquals("Away", all.get(0).get("gymName").asText());
        assertEquals(1, all.get(1).get("setCount").asInt());

        assertEquals(1, get("/sessions?gymId=" + away).size());
        assertEquals(1, get("/sessions?exerciseId=" + squat).size());
        assertEquals(1, get("/sessions?from=2026-11-21&to=2026-11-30").size());
        assertEquals(400, call("GET", "/sessions?from=21.11.2026", null).statusCode());

        JsonNode detail = get("/sessions/" + a);
        assertEquals("100 kg", detail.get("sets").get(0).get("display").asText());
        JsonNode edited = body(call("PUT", "/sessions/" + a, "{\"gymId\":\"" + away + "\",\"localDate\":\"2026-11-21\"}"));
        assertEquals(away, edited.get("gymId").asText());
        assertEquals(400, call("PUT", "/sessions/" + a, "{\"gymId\":\"" + away + "\",\"localDate\":\"2026-11-21\","
                + "\"startedAt\":\"2026-11-21T09:00:00Z\",\"endedAt\":\"2026-11-21T08:00:00Z\"}").statusCode());

        assertEquals(204, call("DELETE", "/sessions/" + a, null).statusCode());
        assertEquals(404, call("GET", "/sessions/" + a, null).statusCode());
        assertEquals(1, get("/sessions").size());
    }
}
