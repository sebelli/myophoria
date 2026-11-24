package myophoria.api;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DataApiTest extends ApiTestSupport {
    private HttpResponse<String> upload(byte[] zip) throws Exception {
        String boundary = "----myophoria-test";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"export.zip\"\r\n"
                + "Content-Type: application/zip\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        body.write(zip);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/import"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void exportThenImportReportsAlreadyPresent() throws Exception {
        createExercise("Squat", "BARBELL");
        HttpResponse<byte[]> export = http.send(HttpRequest.newBuilder(URI.create(base + "/export")).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, export.statusCode());
        assertEquals("application/zip", export.headers().firstValue("Content-Type").orElse(""));
        assertTrue(export.headers().firstValue("Content-Disposition").orElse("").contains("myophoria-"));

        HttpResponse<String> imported = upload(export.body());
        assertEquals(200, imported.statusCode(), imported.body());
        JsonNode files = body(imported).get("files");
        assertEquals(1, files.get("exercises.csv").get("alreadyPresent").asInt());
        assertEquals(0, files.get("exercises.csv").get("imported").asInt());
    }

    @Test
    void notAnExportIs422AndMissingFileIs400() throws Exception {
        HttpResponse<String> r = upload("hello".getBytes(StandardCharsets.UTF_8));
        assertEquals(422, r.statusCode());
        assertEquals("CORRUPTED_CSV", body(r).get("error").asText());
        assertEquals(400, call("POST", "/import", "{}").statusCode());
    }
}
