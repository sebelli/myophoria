# Gym Lift Tracker

A gym lift tracker: a pure Java core, a Javalin REST API, and a mobile-first
Svelte web UI, backed by SQLite.

## Development

    nix develop        # JDK 25, Maven, Node, SQLite, tools (NixOS)
    ./mvnw verify      # backend build and tests (after the Maven Wrapper is added)
    cd web && npm run dev

See docs/ for the design, decisions, API and data format.
