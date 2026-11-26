# Myophoria

A gym lift tracker for one person. One Java process runs on a laptop, serves a
REST API and a mobile-first web UI, and keeps everything in a single SQLite
file. A phone at the gym reaches it over Tailscale.

- [Features](#features) · [Roadmap](#roadmap-not-built-yet)
- [Tech stack](#tech-stack) · [Dependencies](#dependencies) · [Architecture](#architecture)
- Install, build and run on [Windows](#install-build-and-run-on-windows) or [NixOS / Linux](#install-build-and-run-on-nixos--linux)
- [Running the demo](#running-the-demo) · [Phone access](#phone-access-over-tailscale) · [Development](#development) · [Documentation](#documentation)

## Features

**Workout logging**
- Start a workout at a gym; the gym is locked for that session and the default gym is preselected
- Log set by set, prefilled with last time's weights and reps for that exercise at the same gym
- Set types: warm-up, working and failure; kg or lb per set, remembered per gym
- A running workout stays one tap away: a bar on the other tabs shows its clock, with Resume and End (finish or discard)
- Finish with a warning if unticked (draft) sets remain

**Exercises**
- 5 equipment types, each with its own weight rules and labels:
  barbell (total), dumbbell (per dumbbell, "30 kg each"), stack, plate-loaded (plates),
  calisthenics (added weight on top of bodyweight, 0 allowed, bodyweight required)
- Unilateral / bilateral label (informational)
- Search by name, filter by equipment type, initials badge (Incline Dumbbell Press → IDP)
- Soft delete and restore; permanent delete only after an automatic backup
- Equipment type can change only while the exercise has no sets

**Gyms, templates, bodyweight**
- Multiple gym profiles with a default gym and a unit per gym
- Minimal workout templates: save a finished workout as a template, start from one
- Independent bodyweight log, used for calisthenics volume

**History and reports**
- History of all sessions with filters; edit past sessions, backfill workouts you forgot to log
- Session summary: total volume, top and average weight per exercise
- Per-exercise progression chart

**Data safety**
- CSV export/import as a ZIP bundle; import never overwrites and reports skipped
  bad rows and orphan rows line by line
- Automatic `VACUUM INTO` backup before risky operations
- Corrupted database recovery at startup: the bad file is set aside and the newest backup is restored
- Server binds to 127.0.0.1 only, with a Host/Origin guard on `/api`

## Roadmap (not built yet)

**v2**
- Rest timer
- Streaks
- Personal records (PRs)
- 1RM calculator and plate calculator
- More analytics charts
- Settings screen

**Later**
- Muscle group per exercise (an additive migration)
- Real exercise images instead of initials badges
- Scheduled backups
- Always-on host with Docker, or a NixOS systemd service
- Multi-user with authentication and PostgreSQL
- Offline-first sync
- Native mobile wrapper (e.g. Capacitor)

## Tech stack

| Area | Choice |
|---|---|
| Language | Java 25 (LTS) |
| HTTP / REST | Javalin 7 |
| JSON | Jackson (databind + JSR-310 dates) |
| Database | SQLite via sqlite-jdbc, own numbered SQL migrations (`PRAGMA user_version`) |
| Logging | SLF4J simple |
| Tests | JUnit Jupiter 6 |
| Build | Maven via the wrapper (`mvnw` / `mvnw.cmd`), shade plugin → one runnable jar |
| Front end | Svelte 5 + TypeScript, Vite, Tailwind CSS 4 |
| Dev environment | Nix flake (JDK 25, Maven, Node, sqlite3, gh, httpie, jq), direnv |
| Remote access | Tailscale (`tailscale serve`, HTTPS inside your tailnet) |

## Dependencies

**You install these** (Windows steps [below](#install-build-and-run-on-windows)):

| Tool | Version | Needed for |
|---|---|---|
| JDK, e.g. Eclipse Temurin | 25 | building and running the backend (`java`, `jar`) |
| Node.js (includes npm) | 24 or newer | building the web UI |
| Git | any | getting the code (or download the ZIP from GitHub) |
| Tailscale | any, optional | phone access from the gym |

You do not install Maven or SQLite: the Maven Wrapper downloads Maven, and
sqlite-jdbc bundles the SQLite engine.

**Downloaded automatically by the first build** (needs internet once):

| Dependency | Version | Use |
|---|---|---|
| Maven (via wrapper 3.3.4) | 3.10.1 | build tool, fetched by `mvnw` / `mvnw.cmd` into `~/.m2/wrapper` |
| `io.javalin:javalin` | 7.3.0 | HTTP server, REST routes, static files |
| `com.fasterxml.jackson.core:jackson-databind` | 2.23.0 | JSON |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | 2.23.0 | JSON for `java.time` dates |
| `org.xerial:sqlite-jdbc` | 3.54.1.0 | SQLite driver with the native engine built in |
| `org.slf4j:slf4j-simple` | 2.0.20 | logging |
| `org.junit.jupiter:junit-jupiter` (BOM 6.1.3) | 6.1.3 | tests only |
| `maven-surefire-plugin`, `exec-maven-plugin`, `maven-shade-plugin` | 3.6.1, 3.7.0, 3.6.2 | run tests, run from source, build the one jar |

The web UI has only build-time npm packages; `npm ci` installs the exact
versions locked in `web/package-lock.json` into `web/node_modules/`. The built
UI is plain HTML, JS and CSS with self-hosted fonts, so nothing loads from the
internet at runtime.

| npm package | Version range | Use |
|---|---|---|
| `svelte` | ^5.57.3 | UI framework |
| `vite` | ^8.3.6 | dev server and bundler |
| `@sveltejs/vite-plugin-svelte` | ^7.3.1 | Svelte support for Vite |
| `tailwindcss`, `@tailwindcss/vite` | ^4.3.5 | CSS |
| `typescript` | ~6.0.3 | type checking |
| `svelte-check`, `@tsconfig/svelte`, `@types/node` | ^4.7.7, ^5.0.8, ^24.19.2 | `npm run check` |

The source of truth for versions is `pom.xml`, `.mvn/wrapper/maven-wrapper.properties`
and `web/package.json`; update this table when you change them.

## Architecture

Ports and adapters in one Maven module. Dependencies point inward: `api` and
`persistence` depend on `core`; `core` imports nothing from Javalin, JDBC or
Jackson, so the domain is plain Java and its tests run on in-memory fakes.

```
phone/laptop browser ──HTTPS (tailscale serve)──▶ Javalin on 127.0.0.1:7070
                                                   ├─ /          built Svelte app (classpath /public)
                                                   └─ /api/v1/*  controllers ─▶ services ─▶ ports ─▶ SQLite adapters ─▶ data/myophoria.db
```

| Package | Holds |
|---|---|
| `myophoria.core.model` | entities and enums: `Exercise` + 5 subclasses, `WorkoutSession`, `WorkoutSet`, `Gym`, `WorkoutTemplate`, `BodyweightEntry`, `VolumeCalculable` |
| `myophoria.core.service` | use cases: workout, history, exercises, gyms, templates, bodyweight, reports, CSV |
| `myophoria.core.port` | repository and backup interfaces the core needs |
| `myophoria.core.csv` | `CsvSerializable`, `CsvCodec` (RFC 4180 reader/writer) |
| `myophoria.core.error` | 4 custom exceptions (not found, duplicate, invalid input, corrupted CSV) |
| `myophoria.persistence` | SQLite implementations of the ports, `Database` (open, integrity check, recovery), `Migrator`, `SqliteBackups` |
| `myophoria.api` | `Main` (manual wiring, no DI framework), one controller per resource, `ErrorHandlers` |
| `web/` | Svelte screens (Workout, History, Exercises, Summary, Gyms, Templates, Bodyweight, Data) |

**A request:** controller parses JSON into a DTO → calls one service method →
the service validates and uses ports → controller maps the result to JSON →
`ErrorHandlers` turns domain exceptions into HTTP errors.

**Startup (`Main`):** read env → create the data dir → `Database.open()` runs
`PRAGMA quick_check`, recovers from backup if the file is corrupt, migrates →
build adapters, services, controllers → start Javalin on 127.0.0.1.

```
src/main/java/myophoria/
  core/          domain: model, service, port, csv, error
  persistence/   SQLite adapters, backups, recovery
  api/           Javalin controllers, error mapping, Main
src/main/resources/db/migration/   SQL schema
src/test/java/myophoria/           tests per layer
web/src/                           Svelte UI: screens, components, lib (api client, state)
docs/                              documentation
```

## Install, build and run on Windows

All commands are for PowerShell on Windows 10 or 11.

### 1. Install the dependencies

```powershell
winget install EclipseAdoptium.Temurin.25.JDK
winget install OpenJS.NodeJS.LTS
winget install Git.Git
```

Without winget, use the installers: JDK 25 (`.msi`) from
[adoptium.net](https://adoptium.net), Node.js LTS from [nodejs.org](https://nodejs.org)
and Git from [git-scm.com](https://git-scm.com).

Close PowerShell and open a new window so the tools are on `PATH`, then check:

```powershell
java -version     # openjdk version "25..."
node -v           # v24 or newer
npm -v
git --version
```

If `npm` fails with *running scripts is disabled on this system*, allow local
scripts for your user once: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

### 2. Get the code

```powershell
git clone https://github.com/sebelli/myophoria.git
cd myophoria
```

### 3. Build

Build the UI first: `npm run build` writes it into `src\main\resources\public\`,
and the jar packs it in.

```powershell
cd web
npm ci                    # exact versions from package-lock.json
npm run build
cd ..
.\mvnw.cmd -q package     # runs all tests, builds target\myophoria-0.1.0-SNAPSHOT.jar
```

The first build downloads Maven and the Java libraries, so it needs internet
and takes a few minutes. If Maven fails with an error about *release version 25*,
`JAVA_HOME` points to an older JDK (`.\mvnw.cmd -v` shows which Java it uses).
Point it at JDK 25 for this window and build again:

```powershell
$env:JAVA_HOME = (Get-Item "C:\Program Files\Eclipse Adoptium\jdk-25*").FullName
```

### 4. Start

```powershell
java -jar target\myophoria-0.1.0-SNAPSHOT.jar
```

Open <http://127.0.0.1:7070>. Stop the server with `Ctrl+C`.

- PowerShell does not expand `*` for `java -jar`, so use the full jar name.
- Your data goes to `data\myophoria.db` inside the folder you start from, so
  always start from the `myophoria` folder, or set `MYOPHORIA_DATA_DIR`
  (see [Settings](#settings-environment-variables)).
- Port 7070 taken? Run `$env:MYOPHORIA_PORT = "8080"` first.
- `$env:` variables last only for the current PowerShell window.

### 5. Optional: start with demo data

A separate data folder keeps your real data out of the demo:

```powershell
$env:MYOPHORIA_DATA_DIR = "$env:TEMP\myophoria-demo"
Remove-Item -Recurse -Force $env:MYOPHORIA_DATA_DIR -ErrorAction SilentlyContinue
jar --create --no-manifest --file "$env:TEMP\demo.zip"   -C docs\demo\data .
jar --create --no-manifest --file "$env:TEMP\broken.zip" -C docs\demo\broken .
java -jar target\myophoria-0.1.0-SNAPSHOT.jar
```

Then follow [Running the demo](#running-the-demo); the two ZIPs are in `%TEMP%`.

## Install, build and run on NixOS / Linux

With Nix (flakes enabled; on NixOS set
`nix.settings.experimental-features = [ "nix-command" "flakes" ];`), the dev
shell provides every dependency: JDK 25, Maven, Node, sqlite3, gh, httpie, jq.
Only `x86_64-linux` is supported by the flake. Without Nix, install JDK 25,
Node 24+ and Git with your package manager and skip `nix develop`.

```sh
git clone https://github.com/sebelli/myophoria.git && cd myophoria
nix develop                      # or `direnv allow` once

# build: UI first (it lands in src/main/resources/public/), then the jar
cd web && npm ci && npm run build && cd ..
./mvnw -q package                # runs all tests, builds target/myophoria-0.1.0-SNAPSHOT.jar

# start; data goes to ./data, stop with Ctrl+C
java -jar target/myophoria-*.jar # open http://127.0.0.1:7070
```

Optional demo data, in a separate data folder:

```sh
export MYOPHORIA_DATA_DIR=/tmp/myophoria-demo
rm -rf /tmp/myophoria-demo
jar --create --no-manifest --file /tmp/demo.zip   -C docs/demo/data .
jar --create --no-manifest --file /tmp/broken.zip -C docs/demo/broken .
java -jar target/myophoria-*.jar
```

## Running the demo

In the browser (Chrome DevTools device mode at 390×844 looks like a phone):

1. **More → Data → Import** `broken.zip`: the report lists one skipped bad row
   and one orphan set; everything else is imported.
2. Import `demo.zip`: only the 2 missing sets are added (import never overwrites).
3. Browse **History**, open a session summary, open an exercise's progression chart.
4. Start a workout: sets are prefilled from last time at that gym; tick sets, finish, see the summary.

The demo bundle holds six weeks of workouts (6 exercises, 2 gyms, bodyweight,
2 templates). [docs/DEMO.md](docs/DEMO.md) is the full 10-minute script.

## Settings (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `MYOPHORIA_PORT` | `7070` | HTTP port (always bound to 127.0.0.1) |
| `MYOPHORIA_DATA_DIR` | `./data` | holds `myophoria.db` and `backups/` |
| `MYOPHORIA_ALLOWED_HOSTS` | empty | comma-separated extra host names the API accepts |

## Phone access over Tailscale

The server only listens on 127.0.0.1. `tailscale serve` puts it on your
tailnet with HTTPS. **Never use `tailscale funnel`**: it makes the app public,
and there is no login. On NixOS enable the daemon with
`services.tailscale.enable = true;`; on Windows install the Tailscale app.

```sh
tailscale serve --bg 7070                 # https://<machine>.<tailnet>.ts.net
tailscale serve status                    # shows the URL
MYOPHORIA_ALLOWED_HOSTS=<machine>.<tailnet>.ts.net java -jar target/myophoria-*.jar
tailscale serve reset                     # stop serving
```

On Windows set the variable first: `$env:MYOPHORIA_ALLOWED_HOSTS = "<machine>.<tailnet>.ts.net"`.
Open the URL on a phone signed in to the same tailnet.

## Development

```sh
./mvnw verify                          # backend build and all tests
./mvnw test -Dtest=ClassName#method    # one test
./mvnw -q compile exec:java            # run the backend from source
cd web && npm run dev                  # UI with hot reload; proxies /api to :7070
cd web && npm run check                # Svelte + TypeScript checks
nix fmt                                # format Nix files
```

On Windows use `.\mvnw.cmd` instead of `./mvnw`. For UI work run the backend
and the dev server in two PowerShell windows, then open the URL Vite prints
(usually <http://localhost:5173>):

```powershell
.\mvnw.cmd -q compile exec:java        # window 1: backend on :7070
cd web; npm run dev                    # window 2: UI with hot reload
```

## Documentation

| File | Contents |
|---|---|
| [docs/DOCUMENTATION.md](docs/DOCUMENTATION.md) | every class and interface, their relations, how a request travels through the layers |
| [docs/DESIGN.md](docs/DESIGN.md) | architecture and design rules |
| [docs/uml/classes.md](docs/uml/classes.md) | Mermaid class diagrams |
| [docs/API.md](docs/API.md) | REST endpoints |
| [docs/DATA_FORMAT.md](docs/DATA_FORMAT.md) | CSV export/import format |
| [docs/DECISIONS.md](docs/DECISIONS.md) | decision log |
| [docs/DEMO.md](docs/DEMO.md), [docs/DEFENSE.md](docs/DEFENSE.md) | demo script, study guide |
