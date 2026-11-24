package myophoria.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class GymApiTest extends ApiTestSupport {
    @Test
    void gymLifecycle() throws Exception {
        JsonNode gyms = get("/gyms");
        assertEquals(1, gyms.size());
        String seeded = gyms.get(0).get("id").asText();
        assertTrue(gyms.get(0).get("isDefault").asBoolean());
        assertEquals(seeded, get("/gyms/active").get("id").asText());

        String home = post("/gyms", "{\"name\":\"Home\"}", 201).get("id").asText();
        assertEquals("Home 2", body(call("PUT", "/gyms/" + home, "{\"name\":\"Home 2\"}")).get("name").asText());
        assertEquals(409, call("POST", "/gyms", "{\"name\":\"home 2\"}").statusCode());

        assertTrue(post("/gyms/" + home + "/default", null, 200).get("isDefault").asBoolean());
        JsonNode after = get("/gyms");
        for (JsonNode g : after) {
            assertEquals(g.get("id").asText().equals(home), g.get("isDefault").asBoolean());
        }
        assertEquals(home, get("/gyms/active").get("id").asText()); // no sessions → default

        JsonNode err = body(call("DELETE", "/gyms/" + home, null));
        assertEquals("INVALID_INPUT", err.get("error").asText()); // default cannot be deleted
        assertEquals(204, call("DELETE", "/gyms/" + seeded, null).statusCode());
        assertEquals(400, call("DELETE", "/gyms/" + home, null).statusCode()); // default and last
        assertEquals(404, call("DELETE", "/gyms/nope", null).statusCode());
        assertEquals(1, get("/gyms").size());
    }
}
