package myophoria.api;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/** Application entry point: starts the HTTP server on localhost. */
public class Main {
    public static void main(String[] args) {
        Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.routes.get("/api/v1/status", ctx -> ctx.contentType("application/json").result("{\"warnings\":[]}"));
        }).start("127.0.0.1", 7070);
    }
}
