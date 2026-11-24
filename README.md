# Myophoria

A gym lift tracker: a pure Java core, a Javalin REST API, and a mobile-first
Svelte web UI, backed by SQLite. It runs on a laptop; a phone uses it over
Tailscale.

What it does: log a workout set by set with prefilled weights from last time,
manage exercises (5 equipment types) and gyms, browse and edit history,
see a session summary and a per-exercise progression chart, track bodyweight,
build templates, and export/import everything as CSV with automatic backups
and recovery from a corrupted database.

## Requirements

- JDK 25 and Node 22+ (Maven comes with the wrapper, `./mvnw`).
- On NixOS or with Nix: `nix develop` (or direnv) provides all of it, plus
  SQLite, jq and httpie.

## Build and run

One runnable jar serves the UI and the API on http://127.0.0.1:7070:

    cd web && npm install && npm run build && cd ..   # writes src/main/resources/public/
    ./mvnw -q package                                  # tests + jar
    java -jar target/myophoria-*.jar

Settings (environment variables):

| Variable | Default | Meaning |
|---|---|---|
| `MYOPHORIA_PORT` | `7070` | HTTP port (always bound to 127.0.0.1) |
| `MYOPHORIA_DATA_DIR` | `./data` | holds `myophoria.db` and `backups/` |
| `MYOPHORIA_ALLOWED_HOSTS` | empty | comma-separated extra host names the API accepts |

## Development

    ./mvnw verify                          # backend build and all tests
    ./mvnw test -Dtest=ClassName#method    # one test
    ./mvnw -q exec:java                    # run the backend from source
    cd web && npm run dev                  # UI with hot reload; proxies /api to :7070
    cd web && npm run check                # Svelte + TypeScript checks

## Demo data

`docs/demo/data/` is a CSV bundle with six weeks of workouts (6 exercises,
2 gyms, bodyweight, 2 templates). `docs/demo/broken/` is the same bundle with
one bad row and one orphan set, to show the import report. Zip a folder and
import it on the Data screen (More → Data → Import):

    jar --create --no-manifest --file demo.zip -C docs/demo/data .

Importing never overwrites: import the broken bundle first, then the good one,
and only the two missing sets are added. [docs/DEMO.md](docs/DEMO.md) is the
full demo script.

## Phone access over Tailscale

The server only listens on 127.0.0.1. `tailscale serve` puts it on your
tailnet with HTTPS. Never use `tailscale funnel`: it makes the app public, and
there is no login.

    tailscale serve --bg 7070                 # https://<machine>.<tailnet>.ts.net
    tailscale serve status                    # shows the URL
    MYOPHORIA_ALLOWED_HOSTS=<machine>.<tailnet>.ts.net java -jar target/myophoria-*.jar
    tailscale serve reset                     # stop serving

Open the URL on a phone signed in to the same tailnet.

## Documentation

- [docs/DESIGN.md](docs/DESIGN.md): architecture and design rules
- [docs/uml/classes.md](docs/uml/classes.md): class diagrams, generated from the code
- [docs/API.md](docs/API.md): REST endpoints
- [docs/DATA_FORMAT.md](docs/DATA_FORMAT.md): CSV export/import format
- [docs/DECISIONS.md](docs/DECISIONS.md): decision log
- [docs/DEMO.md](docs/DEMO.md) and [docs/DEFENSE.md](docs/DEFENSE.md): demo script and study guide
