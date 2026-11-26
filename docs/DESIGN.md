# Design

Myophoria is a single-user gym lift tracker. One Java process serves a REST
API and a mobile-first web UI from the developer's laptop; the phone reaches it
over Tailscale. Data lives in one SQLite file.

```
phone/laptop browser ──HTTPS (tailscale serve)──▶ Javalin on 127.0.0.1:7070
                                                   ├─ /          built Svelte app (classpath /public)
                                                   └─ /api/v1/*  controllers ─▶ services ─▶ ports ─▶ SQLite adapters ─▶ data/myophoria.db
```

Class diagrams: [uml/classes.md](uml/classes.md). Endpoints: [API.md](API.md).
CSV bundle: [DATA_FORMAT.md](DATA_FORMAT.md). Why things are the way they are:
[DECISIONS.md](DECISIONS.md).

## 1. Layers (ports and adapters)

One Maven module. Dependencies point inward: `api` and `persistence` depend on
`core`; `core` imports nothing from Javalin, JDBC or Jackson.

| Package | Holds | May import |
|---|---|---|
| `myophoria.core.model` | entities, enums, `VolumeCalculable` | `core.error`, `core.csv` |
| `myophoria.core.error` | the four custom exceptions | – |
| `myophoria.core.csv` | `CsvSerializable`, `CsvCodec` (RFC 4180 reader/writer, field parsers) | `core.error` |
| `myophoria.core.port` | repository and backup interfaces | `core.model` |
| `myophoria.core.service` | use cases and their input/result records | all of `core` |
| `myophoria.persistence` | SQLite adapters, `Database`, `Migrator`, `SqliteBackups` | `core`, `java.sql` |
| `myophoria.api` | `Main`, controllers, `ErrorHandlers` | everything |
| `web/` | Svelte 5 + TypeScript + Tailwind UI | the HTTP API only |

Why: the core can be unit-tested with in-memory fakes of the ports
(`src/test/java/myophoria/core/fake`), and the storage or HTTP layer could be
swapped without touching the domain.

## 2. Request flow

1. The browser sends JSON to `/api/v1/...`.
2. A `before` filter in `Main` checks the `Host` and `Origin` headers
   (localhost, 127.0.0.1, or a host in `MYOPHORIA_ALLOWED_HOSTS`); anything else is 403.
3. The controller parses the body into a nested request record and calls one service method.
4. The service validates (entity setters plus business rules), loads and stores
   through ports, and throws a custom exception on bad input.
5. The SQLite adapter runs the SQL, one connection per call; multi-statement
   writes run in one transaction inside the adapter.
6. The controller maps the result to a response record (DTO); Jackson writes JSON.
7. `ErrorHandlers` turns exceptions into `{error, message, details}` with the
   matching status code.

Example, ticking a set: `WorkoutController` (`POST /sessions/{id}/sets`) →
`WorkoutService.addSet` (checks the session is open and the exercise exists,
builds a `WorkoutSet`, whose setters validate weight, reps and bodyweight) →
`SetRepository.insert` → `SqliteSetRepository` → `INSERT INTO workout_set`.

## 3. Startup (`Main`)

1. Read the environment: `MYOPHORIA_PORT` (7070), `MYOPHORIA_DATA_DIR`
   (`./data`), `MYOPHORIA_ALLOWED_HOSTS`.
2. `Database.open()`: if the file exists, run `PRAGMA quick_check`. Only a
   corrupt file or "not a database" (SQLite codes 11 and 26) triggers recovery:
   the file is renamed to `myophoria.db.corrupt-<time>` and the newest usable
   backup is copied into place. Any other SQL error stops startup. Then
   `Migrator` brings the schema up to date (`PRAGMA user_version`). Recovery
   messages are returned as warnings.
3. Wire adapters, then services, then controllers by hand (no DI framework).
4. Start Javalin on 127.0.0.1 with the static UI, the Host/Origin guard, the
   routes and the exception handlers. `GET /status` hands the warnings to the
   UI, which shows a banner.

## 4. Domain model

- `Exercise` is abstract, with one subclass per equipment type: `Barbell`,
  `Dumbbell`, `Stack`, `PlateLoaded` and `Calisthenics`. `Exercise.of` picks the
  subclass. The subclasses differ in behaviour that the app uses:
  - `weightLabel` is what the weight means: total, per dumbbell, stack, plates, or added.
  - `formatSet` is the display text, such as "30 kg each" or "70 | 20 kg".
  - `validateWeight`: calisthenics allows 0.
  - `requiresBodyweight`: calisthenics only.
- `WorkoutSession` owns its `WorkoutSet`s. An open session is the running
  workout, and at most one is open. A backfilled session is created closed and may have no times.
- `VolumeCalculable` (weight × reps, converted to the reporting unit) is
  implemented by `WorkoutSet` and `WorkoutSession`. `CsvSerializable` is
  implemented by all six stored entity types.
- Entities validate in their setters and throw `InvalidWorkoutInputException`;
  getters are plain. Records are used only for DTOs and service inputs/results.
- Names are unique among active rows by a normalised key (NFC, trimmed, single
  spaces, lower case), computed in Java because SQLite `NOCASE` folds ASCII only.
- Deletes of exercises and gyms are soft (`deleted_at`). Re-adding a deleted
  name offers a restore. A permanent exercise delete takes a backup first and
  removes the exercise's sets, any closed sessions left empty, and its template rows, all in one transaction.

## 5. Where logic lives

SQL only loads and stores. Services filter, search, sort and aggregate in
Java collections:

| Service | Collections |
|---|---|
| `ExerciseService` | `Set<EquipmentType>` type filter (an `EnumSet` built by the controller), `HashMap` by id and by name key, sort by name |
| `ReportService` | `LinkedHashMap` grouping of sets per exercise, chronological sort for progression |
| `HistoryService` | filter by date/gym/exercise, `Comparator` sort newest first, `HashMap` of gym names |
| `CsvService` | `HashSet`s of imported ids to detect orphans, `LinkedHashMap` report per file |

The front end never recomputes domain values: initials, weight labels, set
display text, volume and summaries all come from the server.

## 6. Errors

| Exception | Kind | HTTP |
|---|---|---|
| `InvalidWorkoutInputException` | unchecked | 400 `INVALID_INPUT` (also malformed JSON and bad dates) |
| `NotFoundException` | unchecked | 404 `NOT_FOUND` |
| `DuplicateResourceException` | unchecked | 409 `DUPLICATE_RESOURCE`, `details` carry the existing id and whether it is soft-deleted |
| `CorruptedCsvException` | **checked** | caught per row during import; 422 `CORRUPTED_CSV` only when a whole bundle is unusable |
| anything else | – | 500 `INTERNAL`, logged, message hidden |

`CorruptedCsvException` is checked because a broken file is an expected,
recoverable condition: the compiler forces the importer to handle it.

## 7. Files

- **Database:** `data/myophoria.db`. The tables are STRICT and foreign keys are on.
  WAL mode is used, with a busy timeout. Timestamps are fixed-width UTC text
  (`yyyy-MM-ddTHH:mm:ss.SSSZ`) so they sort as text; times are truncated to milliseconds.
- **Backups:** `VACUUM INTO` a temp file, then an atomic rename into
  `data/backups/`. The temp file is always removed in a `finally` block.
  Backups are taken on demand, before every import, and before every permanent delete.
- **CSV ZIP:** export/import of everything ([DATA_FORMAT.md](DATA_FORMAT.md)).
  Import is row by row: bad rows, orphans and name clashes are skipped and
  reported, and nothing is overwritten.

## 8. Web UI

Svelte 5 (runes) with hash routing and no router library. A bottom tab bar
holds Workout, Exercises, History and More. A module-level workout store keeps
unsaved set rows while you move between screens. The progression chart is
hand-written SVG. The layout is designed for a 390 px wide phone with 44 px tap
targets.

The look is black with red accents: white ink on matte black, one grey ramp,
and red only for time and live state (the workout clock, the open exercise's
count, the next set, the active tab). Headings, labels and numbers use Oswald,
body text Barlow; both are self-hosted in `web/public/fonts`. Corners are square
and groups are divided by hairlines, not cards. Tokens live in `web/src/app.css`.

During a workout the exercises open in place on the Workout screen, one at a
time, instead of on a page of their own. On every other tab a bar above the tabs
shows the running clock with **Resume** and **End**; End finishes and saves the
workout or discards it after a confirmation that names the set count.

## 9. Security

The server binds to 127.0.0.1 only. `tailscale serve` exposes it on the
private tailnet with HTTPS; Funnel (public) is never used. There is no login
(single user). The Host/Origin check blocks DNS rebinding and cross-site
requests from other pages.
