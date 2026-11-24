# Myophoria

A gym lift tracker for one person. It runs on a laptop and a phone uses it
over Tailscale.

- **Backend:** pure Java 25 core, Javalin REST API (`/api/v1`), SQLite
- **Frontend:** mobile-first Svelte 5 + TypeScript + Tailwind, served by the backend
- **Architecture:** ports and adapters; the core has no framework or database code

## Features

- Log a workout set by set, prefilled with last time's weights at the same gym
- Warm-up, working and failure sets; kg or lb per set
- 5 equipment types (barbell, dumbbell, stack, plate-loaded, calisthenics), each with its own weight rules
- Multiple gyms, workout templates, bodyweight tracking
- History with filters, backfill of past workouts, editing
- Session summary (volume, top and average weight) and a per-exercise progression chart
- CSV export/import (ZIP), automatic backups before risky operations, recovery from a corrupted database

## Quick start

Requirements: JDK 25 and Node 24+. Maven comes with the wrapper (`./mvnw`).
With Nix, `nix develop` (or direnv) provides everything, plus SQLite, jq and httpie.

```sh
cd web && npm install && npm run build && cd ..   # UI → src/main/resources/public/
./mvnw -q package                                  # runs tests, builds the jar
java -jar target/myophoria-*.jar                   # http://127.0.0.1:7070
```

### Settings (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `MYOPHORIA_PORT` | `7070` | HTTP port (always bound to 127.0.0.1) |
| `MYOPHORIA_DATA_DIR` | `./data` | holds `myophoria.db` and `backups/` |
| `MYOPHORIA_ALLOWED_HOSTS` | empty | comma-separated extra host names the API accepts |

## Development

```sh
./mvnw verify                          # backend build and all tests
./mvnw test -Dtest=ClassName#method    # one test
./mvnw -q exec:java                    # run the backend from source
cd web && npm run dev                  # UI with hot reload; proxies /api to :7070
cd web && npm run check                # Svelte + TypeScript checks
```

## Project layout

```
src/main/java/myophoria/
  core/          domain: model, service (use cases), port (interfaces), csv, error
  persistence/   SQLite implementations of the ports, backups, recovery, migrations
  api/           Javalin controllers, error mapping, Main (wiring)
src/main/resources/db/migration/   SQL schema
src/test/java/myophoria/           tests per layer (core runs on in-memory fakes)
web/src/                           Svelte UI: screens, components, lib (api client, state)
docs/                              documentation (see below)
```

[docs/DOCUMENTATION.md](docs/DOCUMENTATION.md) explains every class and how a
request travels through the layers.

## Demo data

`docs/demo/data/` is a CSV bundle with six weeks of workouts (6 exercises,
2 gyms, bodyweight, 2 templates). `docs/demo/broken/` is the same bundle with
one bad row and one orphan set, to show the import report. Zip a folder and
import it on the Data screen (More → Data → Import):

```sh
jar --create --no-manifest --file demo.zip -C docs/demo/data .
```

Importing never overwrites: import the broken bundle first, then the good one,
and only the two missing sets are added. [docs/DEMO.md](docs/DEMO.md) is the
full demo script.

## Phone access over Tailscale

The server only listens on 127.0.0.1. `tailscale serve` puts it on your
tailnet with HTTPS. **Never use `tailscale funnel`**: it makes the app public,
and there is no login.

```sh
tailscale serve --bg 7070                 # https://<machine>.<tailnet>.ts.net
tailscale serve status                    # shows the URL
MYOPHORIA_ALLOWED_HOSTS=<machine>.<tailnet>.ts.net java -jar target/myophoria-*.jar
tailscale serve reset                     # stop serving
```

Open the URL on a phone signed in to the same tailnet.

## Documentation

| File | Contents |
|---|---|
| [docs/DOCUMENTATION.md](docs/DOCUMENTATION.md) | every class and interface, their relations, program paths |
| [docs/DESIGN.md](docs/DESIGN.md) | architecture and design rules |
| [docs/uml/classes.md](docs/uml/classes.md) | Mermaid class diagrams |
| [docs/API.md](docs/API.md) | REST endpoints |
| [docs/DATA_FORMAT.md](docs/DATA_FORMAT.md) | CSV export/import format |
| [docs/DECISIONS.md](docs/DECISIONS.md) | decision log |
| [docs/DEMO.md](docs/DEMO.md), [docs/DEFENSE.md](docs/DEFENSE.md) | demo script, study guide |
