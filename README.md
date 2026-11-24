# Myophoria

A gym lift tracker: a pure Java core, a Javalin REST API, and a mobile-first
Svelte web UI, backed by SQLite.

## Development

    nix develop        # JDK 25, Maven, Node, SQLite, tools (NixOS)
    ./mvnw verify      # backend build and tests
    cd web && npm run dev

One runnable jar (serves the UI and the API on http://127.0.0.1:7070):

    cd web && npm install && npm run build   # writes src/main/resources/public/
    ./mvnw -q package
    java -jar target/myophoria-*.jar

Settings (environment variables): `MYOPHORIA_PORT` (default 7070),
`MYOPHORIA_DATA_DIR` (default `./data`), `MYOPHORIA_ALLOWED_HOSTS`
(comma-separated extra host names the API accepts).

## Phone access over Tailscale

The server only listens on 127.0.0.1. `tailscale serve` puts it on your
tailnet with HTTPS. Never use `tailscale funnel`: it makes the app public, and
there is no login.

    tailscale serve --bg 7070                 # https://<machine>.<tailnet>.ts.net
    tailscale serve status                    # shows the URL
    MYOPHORIA_ALLOWED_HOSTS=<machine>.<tailnet>.ts.net java -jar target/myophoria-*.jar
    tailscale serve reset                     # stop serving

Open the URL on a phone signed in to the same tailnet.

See docs/ for the design, decisions, API and data format.
