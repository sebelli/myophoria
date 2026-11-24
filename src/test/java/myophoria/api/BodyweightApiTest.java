package myophoria.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class BodyweightApiTest extends ApiTestSupport {
    private static String entry(String weight, String unit, String at, String date) {
        return "{\"weight\":" + weight + ",\"unit\":\"" + unit + "\",\"recordedAt\":\"" + at + "\",\"localDate\":\"" + date + "\"}";
    }

    @Test
    void crudAndLatest() throws Exception {
        assertEquals(204, call("GET", "/bodyweight/latest", null).statusCode());
        JsonNode first = post("/bodyweight", entry("70.5", "KG", "2026-11-20T07:00:00Z", "2026-11-20"), 201);
        JsonNode second = post("/bodyweight", entry("155", "LB", "2026-11-24T07:00:00.250Z", "2026-11-24"), 201);
        assertEquals("2026-11-24T07:00:00.250Z", second.get("recordedAt").asText());
        assertEquals("2026-11-24", second.get("localDate").asText());

        JsonNode list = get("/bodyweight");
        assertEquals(2, list.size());
        assertEquals(second.get("id"), list.get(0).get("id")); // newest first
        assertEquals(second.get("id"), get("/bodyweight/latest").get("id"));

        String id = first.get("id").asText();
        assertEquals(200, call("PUT", "/bodyweight/" + id, entry("71", "KG", "2026-11-25T07:00:00Z", "2026-11-25")).statusCode());
        assertEquals(71.0, get("/bodyweight/latest").get("weight").asDouble());

        assertEquals(204, call("DELETE", "/bodyweight/" + id, null).statusCode());
        assertEquals(404, call("DELETE", "/bodyweight/" + id, null).statusCode());
        assertEquals(1, get("/bodyweight").size());
    }

    @Test
    void invalidEntriesAre400() throws Exception {
        String[] bad = {
            entry("0", "KG", "2026-11-20T07:00:00Z", "2026-11-20"),
            entry("-1", "KG", "2026-11-20T07:00:00Z", "2026-11-20"),
            entry("null", "KG", "2026-11-20T07:00:00Z", "2026-11-20"),
            entry("70", "STONE", "2026-11-20T07:00:00Z", "2026-11-20"),
            entry("70", "KG", "yesterday", "2026-11-20"),
            entry("70", "KG", "2026-11-20T07:00:00Z", "2026-13-40"),
            "{\"weight\":70,\"unit\":\"KG\",\"localDate\":\"2026-11-20\"}"};
        for (String b : bad) {
            assertEquals(400, call("POST", "/bodyweight", b).statusCode(), b);
        }
    }
}
