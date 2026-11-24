package myophoria.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class TemplateApiTest extends ApiTestSupport {
    @Test
    void templateLifecycleAndSaveFromSession() throws Exception {
        String squat = createExercise("Squat", "BARBELL");
        String bench = createExercise("Bench Press", "BARBELL");
        JsonNode t = post("/templates", "{\"name\":\"Legs\",\"exerciseIds\":[\"" + squat + "\",\"" + bench + "\"]}", 201);
        assertEquals("BP", t.get("exercises").get(1).get("initials").asText());
        String id = t.get("id").asText();

        JsonNode updated = body(call("PUT", "/templates/" + id, "{\"name\":\"Legs\",\"exerciseIds\":[\"" + bench + "\"]}"));
        assertEquals(1, updated.get("exercises").size());
        assertEquals(400, call("POST", "/templates", "{\"name\":\"X\",\"exerciseIds\":[]}").statusCode());
        assertEquals(404, call("POST", "/templates", "{\"name\":\"X\",\"exerciseIds\":[\"nope\"]}").statusCode());

        call("DELETE", "/exercises/" + bench, null); // soft delete hides it from the template
        assertEquals(0, get("/templates").get(0).get("exercises").size());
        call("POST", "/exercises/" + bench + "/restore", null);
        assertEquals(1, get("/templates").get(0).get("exercises").size());

        String gym = get("/gyms/active").get("id").asText();
        String session = post("/sessions/start", "{\"gymId\":\"" + gym + "\",\"localDate\":\"2026-11-25\"}", 201)
                .get("id").asText();
        post("/sessions/" + session + "/sets", "{\"exerciseId\":\"" + squat + "\",\"setType\":\"WORKING\","
                + "\"weight\":100,\"unit\":\"KG\",\"reps\":5,\"position\":1}", 201);
        JsonNode saved = post("/sessions/" + session + "/template", "{\"name\":\"Today\"}", 201);
        assertEquals(squat, saved.get("exercises").get(0).get("id").asText());

        assertEquals(204, call("DELETE", "/templates/" + id, null).statusCode());
        assertEquals(1, get("/templates").size());
    }
}
