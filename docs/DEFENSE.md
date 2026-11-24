# Defense study guide

The code was built from the plan in autonomous mode, so this guide is for you
(the developer) to own every line before the defense. Read it top to bottom
once, then use it to look things up. Each class gets: what it does, why it
exists, the rubric item it proves, and the questions an examiner is likely to
ask, with answers. Line numbers are from the `v1.0.0` code.

Related: [DESIGN.md](DESIGN.md) (architecture), [uml/classes.md](uml/classes.md)
(diagrams), [DEMO.md](DEMO.md) (the 10-minute script), [DECISIONS.md](DECISIONS.md) (why).

## 1. The whole program in six sentences

1. A Java 25 program started by `Main` serves a REST API (Javalin) and the
   built Svelte UI on 127.0.0.1:7070.
2. The **core** (`myophoria.core`) holds the domain classes and use cases and
   has no framework imports.
3. The core talks to storage only through **ports** (interfaces such as
   `ExerciseRepository`), which `myophoria.persistence` implements with SQLite (JDBC).
4. Controllers in `myophoria.api` turn HTTP into service calls and service
   results into JSON. `ErrorHandlers` turns exceptions into status codes.
5. Files: the SQLite database, `VACUUM INTO` backups, startup recovery from a
   corrupted file, and a CSV ZIP export/import that reports bad rows instead of failing.
6. The UI is mobile-first: start a workout, tick sets that are prefilled from
   last time, see a summary, browse history and a progress chart.

## 2. One request end to end: ticking a set

| Step | Where | What happens |
|---|---|---|
| 1 | `web/src/screens/ExerciseCard.svelte:57` | The tick checks the row in the browser (`input()`), then calls `api.addSet` |
| 2 | `web/src/lib/api.ts:82` → `:18` | `request('POST', '/sessions/{id}/sets', body)` does a `fetch` of `/api/v1/...` with JSON |
| 3 | `src/main/java/myophoria/api/Main.java:94` | `before("/api/*")` filter: `hostAllowed` checks the Host and Origin headers, else 403 |
| 4 | `api/WorkoutController.java:75` | Route handler: `ctx.bodyAsClass(SetInput.class)` (Jackson builds the record), then `workouts.addSet(...)` |
| 5 | `core/service/WorkoutService.java:78` | `getSession` (404 if unknown), then `checkInput` |
| 6 | `WorkoutService.java:186` | Loads the exercise (404), `exercise.validateWeight(...)` (**polymorphic** call), and the bodyweight rule via `requiresBodyweight()` |
| 7 | `WorkoutService.java:84` | `new WorkoutSet(...)`: the constructor runs the validating setters (`WorkoutSet.java:120` weight, `:136` reps, `:144` bodyweight) |
| 8 | `persistence/SqliteSetRepository.java:38` → `:60` | `INSERT INTO workout_sets ... VALUES (?, ...)` with a `PreparedStatement` in try-with-resources |
| 9 | `persistence/Database.java:120` | `connect()` opens a connection with `foreign_keys = ON` and a busy timeout |
| 10 | `WorkoutController.java:77` | 201 with `SetDto.of(set, exercise)`: `display` comes from `exercise.formatSet(...)` (polymorphic again) |
| 11 | on any error | the exception reaches `api/ErrorHandlers.java:31` (and the lines after it) → `{error, message, details}` with 400/404/409/422/500; the row shows the message and keeps its values |

All paths in the table are under `src/main/java/myophoria/` unless they start with `web/`.

## 3. Rubric map with evidence

| Rubric item | Show this | Say this |
|---|---|---|
| Class design | `core/model/Exercise.java`, `WorkoutSet.java` | Private fields, validated setters, plain getters; services split by use case; records only for data carriers |
| Inheritance | `Exercise` + 5 subclasses | Shared state and rules in the abstract parent; subclasses override only what differs |
| Polymorphism | `WorkoutService.java:194` `exercise.validateWeight`, `WorkoutController` `SetDto.of` → `formatSet` | Called through an `Exercise` reference; the JVM picks the override at run time |
| Abstraction / interfaces | `VolumeCalculable` (2 impls), `CsvSerializable` (6 entity types), the 7 ports | Code depends on what something can do, not on what it is |
| GUI | `web/` (12 screens) | Svelte, phone-first, every value computed on the server |
| Collections | `ExerciseService` (HashMap by name key, sorting), `ReportService:35` (LinkedHashMap), `CsvService:105` (HashSet), `SqliteSessionRepository` (HashMap grouping), `EnumSet` in `ExerciseController` | Each choice is explained in §5 below |
| Functionality | CRUD for exercises, gyms, sessions, sets, bodyweight, templates; search and filter; summary and progression reports | `docs/API.md` lists all 42 routes |
| Exceptions | 4 custom exceptions; `ErrorHandlers`; checked `CorruptedCsvException` caught per row (`CsvService.java:134`); `finally` in `SqliteBackups.java:51` | Unchecked for programming/domain errors, checked for the expected file problem |
| File handling | SQLite, backups, recovery (`Database.java:60`), CSV ZIP (`CsvService`, `CsvCodec`) | Atomic rename, temp-file cleanup, never-silent recovery |
| Documentation/UML | `docs/`, Javadoc on every public type and member, generated UML | `docs/uml/classes.md` is generated from `javap`, so it matches |
| Testing | 116 JUnit tests: core with fakes, adapters on temp files, API end to end | `./mvnw verify` |

## 4. Package `myophoria.core.model`

### `Exercise` (abstract)
- **What:** a lift in the library: id, name, normalised `nameKey`, laterality,
  `deletedAt` (soft delete). It holds the factory `of(type, ...)`, the rules
  shared by every type, and the CSV row mapping.
- **Why:** one parent type lets the services and lists treat all exercises the
  same way (`List<Exercise>`), while each equipment type changes only its own behaviour.
- **Rubric:** class design, inheritance, abstraction, polymorphism, CSV file handling.
- **Q: Why is it abstract?** There is no "generic" exercise: every exercise has
  equipment, and `type()` and `weightLabel()` have no sensible default. Abstract
  stops anyone creating one and forces each subclass to answer them.
- **Q: Why a static factory `of` instead of `new`?** The caller (service, CSV
  import, repository) only knows the `EquipmentType` value. `of` maps it to the
  right subclass in one `switch`, so this mapping exists in exactly one place.
- **Q: How is name uniqueness case-insensitive for "Bänkdrücken"?**
  `nameKey` (`Exercise.java:45`) normalises Unicode (NFC), trims, collapses
  spaces and lower-cases with `Locale.ROOT`. SQLite's `NOCASE` only folds
  ASCII, so the key is computed in Java and has a unique partial index on active rows.

### `BarbellExercise`, `DumbbellExercise`, `StackExercise`, `PlateLoadedExercise`, `CalisthenicsExercise`
- **What:** one subclass per `EquipmentType`. Every subclass overrides `type()`
  and `weightLabel()`. Dumbbell also overrides `formatSet` ("30 kg each").
  Calisthenics overrides `validateWeight` (0 allowed: added weight),
  `requiresBodyweight` (true), and `formatSet` ("70 | 20 kg": bodyweight | added).
- **Why:** these are real differences the app uses on screen and in validation (decision G2).
- **Q: Barbell, Stack and PlateLoaded look almost the same. Why separate classes?**
  They differ in `weightLabel` (total / stack / plates), which the logging
  screen shows. The planned plate calculator (v2) would use the type too. Keeping
  them as types costs one small class each.
- **Q: Can an exercise change type?** Only while it has no sets
  (`ExerciseService.update`): the service rebuilds it with `Exercise.of`, because
  an object cannot change its class.

### `WorkoutSession`
- **What:** a workout at one gym on one day: `startedAt`/`endedAt` (both
  nullable for a backfilled workout), `open` flag, and its list of `WorkoutSet`s.
  Methods: `finish(now)`, `effectiveEnd()` (end time, else the last set's time),
  `duration()`, `volume(unit)`, `addSet`.
- **Why:** owns the session rules: you can't finish twice, the end can't be
  before the start, and an open workout has no end.
- **Rubric:** class design, `VolumeCalculable`, `CsvSerializable`, collections (`List<WorkoutSet>`).
- **Q: Why an `open` flag instead of `endedAt IS NULL`?** A backfilled past
  workout may have no times at all; it would look "open" and block a new
  workout. A partial unique index on `is_open = 1` allows at most one open workout (G1).
- **Q: Why does `finish` validate before changing anything?** If it set
  `endedAt` first and then threw, the object would be left half-changed.

### `WorkoutSet`
- **What:** one completed set: exercise id, position, `SetType`, weight, unit,
  reps, optional bodyweight snapshot, `completedAt`. `volume(unit)` = weight ×
  reps in the reporting unit, and 0 for warm-ups.
- **Q: Where is validation?** In the setters (`WorkoutSet.java:120`, `:136`,
  `:144`), and the constructor calls them, so an invalid set can never exist.
  Rules that need the exercise (calisthenics bodyweight, weight > 0) are in
  `WorkoutService.checkInput`, because a set only knows the exercise id.
- **Q: Why store a bodyweight snapshot?** Bodyweight changes over time. A
  pull-up from March must keep March's bodyweight.

### `Gym`, `BodyweightEntry`, `WorkoutTemplate`
- **Gym:** a name with a normalised key, `isDefault`, and soft delete. Exactly one active
  gym is the default (partial unique index). A database starts with a seeded
  "Default" gym (`SEEDED_DEFAULT_ID`).
- **BodyweightEntry:** weight + unit + `recordedAt` + `localDate`, independent of workouts.
- **WorkoutTemplate:** a name plus an ordered `List<String>` of exercise ids,
  with no set counts. It is stored in `template_exercises` by position.
- **Q: Why `localDate` *and* an `Instant`?** The instant is the exact moment
  (UTC). The local date is the day *on the phone*, which the client sends,
  because a 23:30 workout in Berlin is on a different UTC day.

### Enums: `EquipmentType`, `Laterality`, `SetType`, `Unit`
- `Unit` stores its size in kilograms; `convert(value, to)` = `value × kg / to.kg`.
  An enum with a field and methods shows that enums are full classes.
- `SetType.isWorking()`: WORKING and FAILURE count towards volume and records; WARMUP doesn't.
- **Q: Why enums and not strings?** The compiler checks them, `switch` on
  them is exhaustive, and the database stores `name()` with a CHECK constraint.

### Interface `VolumeCalculable`
- `volume(Unit)`, implemented by `WorkoutSet` and `WorkoutSession` (the session
  adds up its sets). The summary uses it.
- **Q: Why an interface for two classes?** Both are "something with a
  training volume". The summary code can ask either one without caring which it is.

## 5. Packages `myophoria.core.csv` and `myophoria.core.error`

### `CsvSerializable` and `CsvCodec`
- **CsvSerializable:** `toCsvRow()`. Each entity also has a static `HEADER`
  and `fromCsvRow(String[])` (static methods can't be in the interface contract).
- **CsvCodec:** RFC 4180 writer and reader (quotes, commas and line breaks in
  fields, BOM, CRLF), plus field parsers (`number`, `instant`, `enumValue`, ...).
  Each parser throws `CorruptedCsvException` with the column name. Also holds
  `TIMESTAMP`, the one timestamp format used in both the database and CSV.
- **Q: Why not a CSV library?** The format is small and fixed, and writing it
  shows file I/O. `CsvCodecTest` covers quoting, line breaks, BOM and CRLF.

### The four exceptions

| Class | Type | HTTP | Thrown when |
|---|---|---|---|
| `InvalidWorkoutInputException` | unchecked | 400 | a domain rule is broken (setters, services) |
| `NotFoundException` | unchecked | 404 | an id doesn't exist |
| `DuplicateResourceException` | unchecked | 409 | an active name is taken; carries `existingId` and `softDeleted`, so the UI can offer a restore |
| `CorruptedCsvException` | **checked** | 422 (whole bundle) | a CSV row/header/manifest is bad; carries file and line |

- **Q: Why is only one checked?** Checked = an expected, recoverable condition
  the caller must handle. A broken CSV row is expected (people edit exports in
  spreadsheets), and the importer must catch it to report and continue
  (`CsvService.java:134`). Domain errors are caught in exactly one place
  (`ErrorHandlers`), so making them checked would only add `throws` everywhere.
- **Q: What happens to an unexpected exception?** `ErrorHandlers` logs it
  and returns 500 `INTERNAL` with a fixed message. Internal details never reach the client.

## 6. Package `myophoria.core.port`

`ExerciseRepository`, `GymRepository`, `SessionRepository`, `SetRepository`,
`BodyweightRepository`, `TemplateRepository`, `Backups`.

- **What:** interfaces the core needs (`findAll`, `findById`, `insert`,
  `update`, `delete`, plus a few specific ones such as `countSets`,
  `deletePermanently`, `makeDefault`). `findById` returns null when missing; the
  service decides that this is a 404.
- **Why:** dependency inversion. The core says *what* it needs; SQLite is one
  implementation, and the tests use in-memory fakes (`src/test/java/myophoria/core/fake`).
- **Q: Why not let services use JDBC directly?** The services couldn't be
  tested without a database, and the domain would depend on SQL. With ports,
  `WorkoutServiceTest` runs in milliseconds against `FakeWorkoutStore`.
- **Q: Where are transactions?** Inside the adapter, for the three
  multi-statement writes (set default gym, permanent delete, template save)
  (G7). Import is row by row by design, with a backup first.

## 7. Package `myophoria.core.service`

All services get their ports and a `Clock` through the constructor (manual
dependency injection in `Main`). They create UUIDs and truncate times to milliseconds.

| Service | Key methods | Notable rules |
|---|---|---|
| `ExerciseService` | `search`, `create`, `update`, `softDelete`, `restore`, `usage`, `deletePermanently` | name check through a `HashMap` by name key (`:119`); type change only without sets; permanent delete only after soft delete, backup first |
| `GymService` | `list`, `create`, `rename`, `makeDefault`, `delete`, `activeGym` | can't delete the default or the last gym; `activeGym` = the open workout's gym, else the default |
| `WorkoutService` | `open`, `start`, `finish`, `addSet`, `updateSet`, `deleteSet`, `prefill` | one open workout; finish needs ≥ 1 set; deleting the last set deletes a *closed* session only (G6) |
| `HistoryService` | `list(HistoryFilter)`, `get`, `backfill`, `edit`, `delete` | filtering and newest-first sort in Java; the gym of a running workout is locked |
| `ReportService` | `summary`, `progression` | `LinkedHashMap` keeps exercises in first-set order (`:35`); top/avg over working sets only |
| `BodyweightService` | `list`, `latest`, CRUD | newest first |
| `TemplateService` | `list`, `create`, `update`, `delete`, `saveFromSession` | soft-deleted exercises are hidden on read (G11); order = first-set order |
| `CsvService` | `export`, `importZip` | see `DATA_FORMAT.md`; `HashSet`s of ids for orphan detection (`:105`); 64 MB unzip cap |

Records here: `SetInput`, `Prefill`, `PrefillRow`, `HistoryFilter`,
`SessionHeader`, `SessionSummary`, `ExerciseSummary`, `ProgressPoint`,
`Usage`, `ImportReport`, `FileReport`.

- **Q: Explain prefill.** For an exercise in the current workout:
  1. Take the unit of the latest set of it at this gym.
  2. Find the last *other* session at this gym that contains it.
  3. Copy its rows converted to that unit, plus a warm-up suggestion
     (80% of the top working weight) and, for calisthenics, the latest bodyweight.
- **Q: Why records for these?** They are pure data with no rules or
  identity: immutable, with `equals`/`hashCode` and less code. Entities have
  rules and change, so they are classes with validating setters.
- **Q: Why filter in Java instead of SQL `WHERE`?** The data is small (one
  person's history), the course grades collections, and the logic stays testable
  with fakes. The cost is loading every session for a history list, which is fine at
  this size. The upgrade path is a query method on the port.
- **Q: Why does `ReportService` use `LinkedHashMap` and not `HashMap`?**
  It groups sets by exercise but must keep the order in which exercises were
  done; `HashMap` order is undefined.
- **Q: Why a `Clock` parameter?** Tests pass `Clock.fixed(...)`, so the
  timestamps are predictable.

## 8. Package `myophoria.persistence`

### `Database`
- **What:** owns the file. `open()` creates the folder, checks the file
  (`isHealthy`, `:92`: `PRAGMA quick_check`), and recovers if it is damaged
  (`recoverIfCorrupt`, `:60`). Then it enables WAL and runs `Migrator`. `connect()` (`:120`) hands out
  connections with foreign keys and a busy timeout.
- **Q: Walk through recovery.** If the check fails with SQLite code 11 or 26:
  1. Rename the file (and its `-wal`/`-shm`) to `.corrupt-<time>`. It is never deleted.
  2. Try the backups newest first, copying each and checking it.
  3. Each outcome becomes a warning, which `/status` shows as a UI banner.

  Any other error (a lock, permissions) stops startup instead, so a healthy
  file is never replaced by mistake.
- **Q: One connection per call, isn't that slow?** SQLite connections to a
  local file are cheap, and it avoids sharing a connection across Jetty
  threads. WAL lets readers and the writer work at the same time.

### `Migrator`
- Runs the numbered scripts (`db/migration/V1__init.sql`) newer than
  `PRAGMA user_version`, each in a transaction, then bumps the version.
- **Q: Why not Flyway?** One script and one number: a 50-line class is easier
  to explain and adds no dependency or history table (D1).

### `SqliteBackups`
- `createBackup()`:
  1. `VACUUM INTO` a `.tmp` file, which is a consistent copy even while the app runs.
  2. Atomic rename to `myophoria-<time>.db`.
  3. A `finally` block (`:51`) always removes the temp file and closes the connection.
- **Q: Why `finally` here?** On failure a half-written `.tmp` file must not
  stay behind, and the connection must close on every path. Without the atomic
  rename, a crash could leave a partial file that looks like a real backup.

### `Sqlite*Repository` (6 classes)
- Each class implements one port with `PreparedStatement`s in
  try-with-resources. `SqliteSessionRepository` loads all sets in one query and
  groups them by session id in a `HashMap` (no N+1 queries).
  `SqliteExerciseRepository.deletePermanently` (`:98`) runs sets → empty
  closed sessions → template rows → exercise in one transaction, with rollback on error.
- **Q: How do you prevent SQL injection?** Every value is a `?` parameter.
  The only built SQL string is the `VACUUM INTO` path, which is generated by the
  app, not user input, and has its quotes escaped.

## 9. Package `myophoria.api`

### `Main`
- Reads the 3 environment variables and opens the `Database`. Then it wires
  adapters → services → controllers **by hand** (`createApp`, `:56`) and starts
  Javalin on 127.0.0.1 with the static UI, the Host/Origin guard and the routes.
- **Q: Why no Spring?** About 20 objects: manual wiring is 30 readable lines,
  starts in under a second, and every dependency is visible.
- **Q: What does the Host/Origin guard stop?** DNS rebinding and other
  websites calling the API through your browser. There is no login, so only
  localhost or your Tailscale host name may talk to it. `@` in a header is
  rejected (`evil.com@localhost`).

### `ErrorHandlers`
- Maps each exception type to a status and code. `DuplicateResourceException`
  adds `details`. JSON and date parse errors become 400; anything else is a logged 500.

### Controllers
`ExerciseController`, `GymController`, `WorkoutController`, `HistoryController`,
`ReportController`, `BodyweightController`, `TemplateController`,
`DataController`. Each has a `register(routes)` method and nested request/response records.

- **Q: Why DTO records instead of returning entities?** The JSON shape is a
  contract with the UI; entities can change without breaking it. DTOs also add
  computed fields (`initials`, `weightLabel`, `display`) so the UI never repeats domain logic.
- **Q: Why is `WorkoutController` registered before `HistoryController`?**
  `/sessions/open` must match before `/sessions/{id}`.

## 10. The web UI (`web/`)

- Svelte 5 + TypeScript + Tailwind, built by Vite into
  `src/main/resources/public`, so the jar serves it.
- `lib/api.ts` has one function per endpoint and `ApiError` with the server's code.
  `lib/workout.svelte.ts` is a module-level store, so unticked rows survive navigation.
  `lib/router.svelte.ts` handles hash routes.
- **Q: Is there logic in the UI?** Only input checks for a friendly message
  (empty weight, reps < 1). The server re-checks everything, and display values
  come from the server.

## 11. Tests (116)

| Layer | Classes | Technique |
|---|---|---|
| core model/csv | `ExerciseTest`, `UnitTest`, `WorkoutSessionTest`, `CsvCodecTest`, `CsvSerializationTest` | pure unit tests |
| core services | `*ServiceTest` (6) | fakes of the ports, fixed `Clock` |
| persistence | `Sqlite*RepositoryTest`, `CsvRoundTripTest`, `DatabaseRecoveryTest`, `PermanentDeleteTest`, `SqliteBackupsTest` | real SQLite in a JUnit `@TempDir` |
| api | `*ApiTest` (8) | real server on a random port, HTTP requests, JSON asserts |

- **Q: How do you test corruption recovery?** `DatabaseRecoveryTest` writes
  garbage over a database file, opens it, and asserts that the backup was
  restored, the corrupt file was kept, and a warning was returned.

## 12. General questions

- **Why SQLite?** A single user, a single file, nothing to install, and
  backups by file copy (`VACUUM INTO`). STRICT tables give real type checks.
- **Why soft delete?** Old workouts reference the exercise. Deleting it would
  break history, so it is hidden instead and can be restored. A permanent delete
  exists, but only after a soft delete, and with a backup.
- **What would you change with more time?** The v2 list in `handoff.md` §9:
  a rest timer, 1RM and plate calculators, PRs, a settings screen (unit
  preference), and scheduled backups.
- **What was hardest?** Import semantics (never overwrite, orphans, the
  one-default and one-open rules) and recovering from a corrupted database
  without ever losing the damaged file.
