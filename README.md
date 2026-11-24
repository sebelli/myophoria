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

See docs/ for the design, decisions, API and data format.
