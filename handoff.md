# Myophoria: Session Handoff

Everything needed to continue the project in a new session: what is decided, why, what is still open, and what to do first. Resolved questions are logged in section 15 so they are not asked again.

**Status legend.** Anything not tagged is **DECIDED** (explicitly confirmed by the developer). **PROPOSED** means recommended but not confirmed. **OPEN** means unanswered. **ASSUMED** means a working assumption to verify.

**As of:** 2026-11-22. **Nothing is coded yet.** Step 1 (SQLite schema) and Step 2 (UML and class design) have not started.

---

## 1. Snapshot

| | |
|---|---|
| App | **Myophoria**, a gym lift tracker (renamed from the placeholder "lift"). Java base package: `myophoria` |
| Why | CS108 (Java OOP, BSCS 2B) final project, and also a personal GitHub project |
| Team | One person (solo) |
| Deadline | **Friday 2026-11-27** (5 days from 2026-11-22). **OPEN:** whether Friday is a submission date or a live demo and Q&A |
| Shape | Pure Java core, wrapped by a Javalin REST API, used through a mobile-first Svelte web UI, stored in SQLite |
| Hosting | The developer's laptop, reached from a phone over Tailscale |
| Philosophy | Suckless: do things as minimally as possible, no unnecessary code, classes or abstractions |

## 2. Working agreement

- The developer will ask the assistant for coding help per piece, on request. The assistant writes the code.
- ~~The assistant waits for an explicit "go" before executing anything.~~ Superseded 2026-11-25: autonomous mode, see `PLAN.md` §0.2. The developer reviews and studies the finished program before the defense.
- AI-written code is fine as long as the developer understands it. The core is graded in a live Q&A, so every class must be explainable.
- Files are traded by the developer pasting or uploading relevant files; the assistant returns files or patches.
- Web norms are followed, but not strictly when they create friction. Do not add code, fields or classes that are not needed.

## 3. Course context and rubric

Confirmed with the professor: only the core needs to be Java; a web UI counts for the GUI item; external libraries, Javalin and SQLite are allowed (the core should still be a genuine Java core, with no massive shortcuts); SQLite plus CSV import/export and backup satisfies file handling, and **missing or corrupted CSV or DB files must be handled without crashing**; the project idea is approved.

Grading: 90% group rubric (100 points) plus 10% peer evaluation (how this applies to a solo developer is unconfirmed).

| Rubric item | Points |
|---|---|
| Class design | 15 |
| Inheritance, polymorphism, abstraction | 15 |
| GUI | 14 |
| Collections | 10 |
| Functionality | 10 |
| Documentation and UML | 10 |
| Presentation | 10 |
| Exceptions | 8 |
| File handling | 8 |

The Java domain and service layers are checked against a **UML class diagram in a live Q&A**, so the diagram must match the code.

**What the core must visibly show:**

- 6+ well-defined classes with private fields, constructors and validated setters (plain getters). No god class. *Check the rubric's literal wording: it may say "validated getters/setters".*
- One inheritance hierarchy (1 parent + 2+ subclasses) using `super`/`this` and meaningful overriding, called polymorphically through parent references and collections.
- At least one abstract class and/or interface implemented by 2+ classes.
- Explicit `try-catch-finally`, `throw`/`throws`, and 2+ custom exceptions, caught cleanly in Javalin exception handlers.
- Meaningful use of `ArrayList`, `HashMap` and `Set` in the service layer for iteration, searching, filtering and sorting (not everything pushed into SQL).
- Full CRUD (including edits of logged sets, exercises, gyms and bodyweight entries), plus search/filter and summary reports.

## 4. Tech stack

| Area | Choice | Why |
|---|---|---|
| Backend language | Java 25 (current LTS). **ASSUMED:** fall back to Java 21 if the Nix package is missing | Course requirement; LTS |
| Backend framework | Javalin (REST, JSON) | Thin and transparent; keeps the Java core visible instead of hidden behind framework magic |
| Front end | Svelte (Vite + TypeScript), standalone SPA | Small, fast, little boilerplate |
| Styling | Tailwind CSS | Quick mobile-first styling |
| Build | Maven, with the Maven Wrapper | Mainstream, simple; wrapper means Windows needs no Maven install |
| Database | SQLite with Flyway migrations | Single file, no administration, easy backup; standard SQL keeps PostgreSQL open; Flyway lets the schema grow in small migrations |
| Libraries | Jackson (JSON), sqlite-jdbc, SLF4J, JUnit | Boring infrastructure that never replaces domain logic |
| Remote access | Tailscale | Phone reaches the laptop from the gym without exposing anything publicly |

**Why Tailscale and not port forwarding:** many ISPs use CGNAT (forwarding impossible), home IPs change, and forwarding exposes an unauthenticated server to the internet. Tailscale also provides HTTPS.

**Dependency rule:** dependencies serve infrastructure (HTTP, JSON, database, tests) and never replace domain logic.

**Not chosen:** Spring Boot (heavier, more magic), Spring Boot with Thymeleaf/HTMX, Quarkus/Micronaut, running Java in the browser, a full JavaScript rewrite.

## 5. Architecture

### 5.1 Pure core with adapters (ports and adapters)

The domain and services live in a framework-free package (`core`). Persistence and HTTP are adapters that depend on the core, never the reverse. If `core` ever wants to import Javalin, JDBC or Jackson, the design is wrong.

### 5.2 Layout

The scaffold script generates a **single Maven module** with packages (**PROPOSED, confirm; the earlier plan was a multi-module build**):

```
myophoria/
├── flake.nix  .envrc  .gitignore  .gitattributes  .editorconfig  README.md
├── docs/   (PROJECT_MASTER_PLAN.md, DESIGN.md, DECISIONS.md, API.md, DATA_FORMAT.md, uml/classes.md)
├── src/main/java/myophoria/
│   ├── core/  model/  service/  port/  csv/  error/
│   ├── persistence/        (SQLite adapter, backup and recovery)
│   └── api/                (Javalin controllers, exception handlers, bootstrap)
├── src/main/resources/db/migration/   (Flyway scripts)
├── src/test/java/myophoria/core/
└── web/                    (Svelte + Vite project)
```

Later, added by tools: `pom.xml`, Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`), `flake.lock`, and the built front end copied into `src/main/resources/public/` (gitignored).

### 5.3 Request flow

1. Phone sends a request over Tailscale to Javalin.
2. The controller validates the request shape and calls a core service.
3. The core service applies business rules and throws custom exceptions on bad input.
4. The service saves through a repository interface (port); the SQLite adapter writes.
5. The result is mapped to a response object and returned as JSON. Custom exceptions map to HTTP status codes in Javalin exception handlers with a consistent error format.

### 5.4 Where logic lives

- SQL does persistence and simple fetches.
- Java collections do searching, filtering, sorting and aggregation (deliberate, for the rubric; fine at personal-scale data).
- The front end never recomputes business logic (volume, top weight, conversions). It calls the API.

### 5.5 Conventions

- UUID primary keys (stored as text).
- Times: a UTC instant **plus** the local calendar date captured from the phone, so early-morning sessions group under the right local day.
- Versioned API (`/api/v1`), consistent JSON error format, API objects (records) kept separate from domain objects.
- Domain objects are classic classes with validated constructors and setters.
- Production: Javalin serves the built SPA from one origin. Development: Vite proxies API calls to Javalin.
- No offline support in v1. No `user_id` columns in v1 (no authentication; Flyway can add it later).
- Deliberately avoided: microservices, Kubernetes, message queues, caching layers, heavy front-end state management.

## 6. Class design

```
Exercise (abstract)            implements CsvSerializable
  defaults: volume = weight x reps
            warm-up = 80% of last top weight
            shared checks (name, laterality)
  |-- BarbellExercise          overrides: valid weights, uses plates, display
  |-- DumbbellExercise         overrides: valid weights, "per dumbbell" display
  |-- StackExercise            overrides: valid weights, display
  |-- PlateLoadedExercise      overrides: valid weights, uses plates, display
  `-- CalisthenicsExercise     overrides: zero added weight allowed,
                               bodyweight required, "70 | 20" display

<<interface>> VolumeCalculable <-- WorkoutSet, WorkoutSession
<<interface>> CsvSerializable  <-- Exercise (so all five subclasses), Gym,
                                   WorkoutSession, WorkoutSet,
                                   BodyweightEntry, WorkoutTemplate

Enums: SetType (WARMUP, WORKING, FAILURE), Unit, Laterality, EquipmentType
```

**Why this shape:**

1. **No `FreeWeightExercise`.** Its only unique rule (weight must be positive) was shared with Stack, and cable stacks are not free weights. A parent class exists because children share behavior or a contract, not because the real world groups them. A "free weights" filter, if ever wanted, would be data, not a class.
2. **Shared defaults live once in `Exercise`**, so subclasses override only real differences (valid weights, plates, display).
3. **`VolumeCalculable` sits on `WorkoutSet` and `WorkoutSession`**, so any mix of sets and sessions can be totalled uniformly. The call takes the reporting unit, and a warm-up returns 0 in a single place.
4. **`CsvSerializable` on the entities** (chosen over separate mapper classes): fewer classes, easy to explain, the export loop treats all records alike. Trade-off: entities know the CSV format. Parsing uses a static factory per entity (an interface cannot hold it).
5. **No separate pure-bodyweight class.** Pull-ups are Calisthenics at 0 kg added, with no distinction in the app or backend.
6. **One static method on `Exercise` maps the stored `EquipmentType` to the right subclass.** The mapping is needed in three places (database load, API create, CSV import), so one shared method avoids triplicating it. Everywhere else, code calls the same method on every `Exercise` and Java runs the right version. No separate factory class.

**Exceptions:**

| Exception | Kind | Thrown when | HTTP |
|---|---|---|---|
| `InvalidWorkoutInputException` | unchecked | bad values (negative weight, zero reps, blank name) | 400 |
| `DuplicateResourceException` | unchecked | active name already in use; carries resource type, existing ID and a soft-deleted flag so the UI can offer restore, rename or delete | 409 |
| `NotFoundException` | unchecked | an ID does not exist | 404 |
| `CorruptedCsvException` | checked | malformed row, header or format version during import | 422 |

Everything else falls to a generic handler returning 500 with a safe message (never a raw exception string). No shared base class. Rule: checked when the caller can recover at that spot; unchecked when it should bubble to one central handler. Use try-with-resources to close streams and connections. Use `finally` for non-resource cleanup (deleting a leftover temp file after a failed import or backup, which write to a temp file and rename on success).

## 7. Domain rules

### 7.1 Weights, units, bodyweight

- Dumbbell: entered weight is per dumbbell (two 30 kg dumbbells = `30 kg`); volume = per-dumbbell weight x reps, no doubling.
- Barbell: total loaded weight including the bar. Machine: the stack number shown.
- Calisthenics: bodyweight and added weight are separate, displayed `70 | 20` (no combined total in v1). The **bodyweight snapshot is required on every Calisthenics set**, prefilled from the latest bodyweight entry (if none exists, the set form asks once). Volume uses added weight only.
- Unilateral volume = bilateral volume (no doubling or halving). Labels: `unilateral` / `bilateral` only, defaulting to bilateral, informational and filterable.
- Weight and unit are stored exactly as entered. Units are converted in the Java service layer only when comparing or aggregating.
- **Reporting unit:** kg by default, optional `?unit=lb`. Factor: 1 lb = 0.45359237 kg exactly. Round only for display.
- Units are remembered per gym and per exercise, derived from the most recent set (no extra table). The user picks the unit from a dropdown on the KG label.
- Bodyweight is its own independent entry (weight, unit, time).

### 7.2 Sets

- `SetType`: WARMUP, WORKING, FAILURE. Warm-ups are excluded from PRs, volume, charts and averages. A failure set (F) counts as a working set. Numbers count normal sets only, so rows read W, 1, 2, F. Tapping the label cycles number, W, F.
- **Warm-up default weight:** 80% of the last top working weight at the current gym = the heaviest working set of the most recent session containing that exercise at that gym, converted into the remembered unit; blank if there is no history there.
- **The checkbox means "completed".** Only completed sets are stored. Ticking saves, unticking removes, editing a ticked row updates it. Unticked rows are on-screen drafts, discarded on Finish (with a prompt if any remain).
- **Prefill:** a new row copies the same-numbered set from the last session at that gym and unit; beyond that, it copies the row above. An exercise opens with as many rows as last session had, or one if there is no history.
- **Per-session average:** the mean working-set weight per exercise within a session, converted to the reporting unit.

### 7.3 Exercises

- Fields: name, equipment type, laterality, soft-delete marker. The library **starts empty**; the user adds their own (an "Add exercise" shortcut sits in the exercise list).
- Equipment type is **immutable once an exercise has sets** (it changes what weight means).
- Names are unique among active exercises, case-insensitive (Java is the authority; the DB index is a safety net).
- Re-adding a name that matches a soft-deleted exercise offers: restore it (only if the type matches), rename the new one, or permanently delete the old one. Permanent delete removes its logged sets after a confirmation showing the counts, and takes an automatic backup first. A "Deleted exercises" list is reachable without a name conflict.
- Soft delete preserves history and hides the exercise from lists, search and pickers.
- **Initials badge instead of an image in v1**, computed from the name: all initials for two or more words (Incline Dumbbell Press = IDP), first two letters for one word (Squat = SQ). Real images are decided later.
- Tapping an exercise already used in the current workout reopens its card.

### 7.4 Gyms

- A gym is a name plus a default flag and a soft-delete marker. A "Default" gym is seeded so no gym is ever null.
- Exactly one active gym is the default. The default or the last active gym cannot be deleted. Gyms are soft-deleted; no permanent purge in v1.
- The active gym is derived from the most recent session's gym, falling back to the default.
- The gym is chosen at workout start (default preselected), then **locked for that session**; it can be edited later from History.

### 7.5 Sessions and the workout flow

Start screen (empty workout or template) -> check gym -> tap Start -> pick an exercise -> log sets -> (another exercise) -> Finish workout -> summary.

- **The clock starts at the Start tap** (`started_at`); Finish sets `ended_at`. At most one session is open at a time.
- Reopening the app mid-workout offers "Resume or finish". If never finished, the end time falls back to the last set's timestamp.
- Finishing with zero ticked sets discards the workout after a confirmation.
- Backfilled sessions (forgot to log) have optional start and end times; duration shows only when both exist.
- Multiple sessions per day are allowed. Deleting a session removes its sets after a confirmation showing the count. A session left with zero sets is cleaned up. Sessions never auto-close.
- **Summary screen:** total sets, working sets (warm-ups excluded), total volume, top weight and average per exercise, start time, end time, duration.

### 7.6 Templates (minimal)

A template is a name plus an ordered list of exercises (no stored set counts), created from scratch or via "Save as template" on the summary screen. Templates are hard-deleted. A deleted exercise drops out of any template that lists it. Included in the CSV export. **Decided for v1 but the first cut candidate (see section 10).**

### 7.7 Delete behavior

| Record | Behavior |
|---|---|
| Exercise | Soft delete; permanent delete only from the name-conflict flow or the Deleted exercises list (cascades its sets) |
| Gym | Soft delete |
| WorkoutSet, BodyweightEntry | Hard delete (a correction) |
| WorkoutSession | Hard delete with its sets, after confirmation |
| WorkoutTemplate | Hard delete |

## 8. UI and screens

Full front-end layout and navigation are finalized after the backend exists. Everything below was discussed; items marked PROPOSED or OPEN were not confirmed. The logging screen was modeled on a reference screenshot of a workout app (exercise header, a table of set rows with a completion checkbox, an "Add set" button), adapted as described.

### 8.1 Screen inventory

The rubric needs 3+ working screens; v1 has these:

| Screen | Purpose |
|---|---|
| Start | Start an empty workout (or a template, a cut candidate); gym shown, defaulted and changeable |
| Exercise picker | One searchable list with optional equipment-type filter chips; initials badges; "Add exercise" shortcut |
| Logging | Log sets for one exercise (section 8.3) |
| Summary | Shown after Finish (section 7.5 lists its contents); "Save as template" is an optional action (cut candidate) |
| Exercises (management) | Add, edit, search, filter, soft delete, restore, Deleted exercises list |
| History | Filterable list of sessions; edit sessions (date, gym, times) and sets; delete sessions and sets |
| Bodyweight | List of entries with add, edit, delete |
| Gyms | List with add, rename, set default, soft delete |
| Progression chart | Top working weight over time per exercise (a cut candidate; placement OPEN) |
| Templates | Create, edit, delete (a cut candidate) |

**Navigation structure: OPEN.** PROPOSED: a bottom tab bar (Workout, Exercises, History, More) with Bodyweight and Gyms under More.

### 8.2 Workout flow

```
Start screen
  [Start empty workout]  or  [Pick a template]      (template = cut candidate)
  Gym: default or last used (changeable)
        |
        v
  Tap Start -> session created, clock starts, gym locked
        |
        v
  Pick an exercise  <-----------------------------+
  (or the next one in the template)               |
        |                                         |
        v                                         |
  Log sets (tick each row to save it) ------------+   another exercise
        |
        v
  Finish workout
        |-- no ticked sets --> Discard workout (after confirmation)
        `-- otherwise -------> Summary screen --> optional: Save as template
```

Reopening the app mid-workout offers "Resume or finish".

### 8.3 Logging screen

```
[PU]  Pull-up                        initials badge + exercise name only
 Bodyweight: 70 kg  (edit)           Calisthenics only; applies to the rows below
 SET    KG v    REPS    done
  W      0       8      [x]          completed: green tint, filled check
  1     10       8      [x]
  F     10       6      [x]
  2     10       8      [ ]          unticked: prefilled, muted
 [ + Add set ]
```

- **Header:** the initials badge and exercise name only. No equipment label, no description or notes, no image, no rest timer, no "previous" column (all removed from the reference).
- **Columns:** SET, KG, REPS, completion checkbox. The KG label is a dropdown to pick kg or lbs (remembered per gym and exercise); existing ticked sets keep the unit they were stored in.
- **Set label:** a number for normal sets, W for warm-up, F for failure. Tapping the label cycles number, W, F. Numbers count normal sets only (W, 1, 2, F).
- **Rows:** weight and reps are editable and prefilled (rules in section 7.2).
- **Ticking** saves the set, **unticking** removes it, **editing** a ticked row updates it. Unticked rows are on-screen drafts that are discarded on Finish.
- **Add set** adds a row prefilled from the row above.
- **Calisthenics:** the bodyweight chip sits under the exercise name; rows hold only the added weight. History and summaries show `70 | 20`.
- **Visual styling** (from the mockup, not formally decided): completed rows tinted green like the reference, W in an amber text color, F in a red text color, unticked rows in a muted color.
- **PROPOSED:** numeric keyboards on weight and reps fields, tap targets of at least 44px.
- **OPEN:** the gesture for deleting a set (swipe or long press).

### 8.4 Exercise picker

- One searchable list of exercises with optional equipment-type filter chips (Barbell, Dumbbell, Stack, PlateLoaded, Calisthenics). Variants are separate exercises ("Barbell Bench Press", "Dumbbell Bench Press"). No category tier and no muscle group in v1.
- Each row shows the initials badge (section 7.3). Soft-deleted exercises are hidden.
- An "Add exercise" shortcut lets a missing exercise be created mid-workout without leaving the flow.
- Tapping an exercise already used in the current workout reopens its card.
- With a template, its exercises appear as the queue, and the user can still add others.
- The library starts empty, so the first launch needs an empty state prompting the user to add an exercise.

### 8.5 Summary screen

Total sets, working sets (warm-ups excluded), total volume, top weight and average per exercise, start time, end time and duration. "Save as template" is available (cut candidate).

### 8.6 Prompts and dialogs

| Moment | Behavior |
|---|---|
| Reopen with an unfinished session | "Resume or finish" |
| Finish with zero ticked sets | Confirm discarding the workout |
| Finish with unticked draft rows | Prompt that they will be discarded |
| Delete a session | Confirm, showing the set count |
| Re-add a name matching a soft-deleted exercise | Offer restore (if the type matches), rename, or permanent delete |
| Permanent delete of an exercise | Confirm, showing sets and sessions that will be erased; an automatic backup is taken first |
| A set fails to save | Clear error; the row stays unticked so it can be retried |
| Corrupted database at startup | Warning that the file was renamed and a backup restored (never silent) |
| CSV import finished | Report of skipped rows, orphans and name conflicts |

### 8.7 Conventions and open UI questions

- Mobile-first, touch-friendly; semantic HTML and visible focus states; a full accessibility audit is skipped as too heavy.
- **Progression chart:** if every value is 0 (for example pull-ups at 0 kg), show "no added weight logged"; this is data-driven, not type-specific.
- **History filters:** fixed during the front-end build.
- **OPEN:** navigation structure, where the chart lives, the set-delete gesture, how the 3 to 4 letter badge fits, and the template builder UI.

## 9. Scope

- **v1:** active workout logging (warm-up and failure sets, per-gym unit memory, split bodyweight display), independent bodyweight tracking, lightweight gym profiles, exercise management with soft delete and search/filter, CSV import/export and backup, one progression chart, a workout summary report, filterable History, minimal templates.
- **v2:** rest timer, streaks, extra analytics charts, 1RM and plate calculators, PRs, a settings screen.
- **Later:** muscle group (as a field on `Exercise`, an additive migration; the picker could sit in front of the same list), real exercise images, scheduled backups, always-on host with Docker, multi-user with PostgreSQL, offline-first sync, native wrapper such as Capacitor.

## 10. Deadline plan and cuts (SUPERSEDED 2026-11-25: no cuts; the build order now lives in `PLAN.md`)

**Cut order if time runs short:**

1. Tailscale and phone access (demo on the laptop browser at phone width; try Tailscale only if ahead).
2. Templates and "Save as template".
3. Permanent delete and the Deleted exercises list (soft delete, restore and rename stay).
4. The progression chart (the summary report already covers "report").
5. ZIP bundle and manifest (export and import become a folder of CSV files).
6. kg and lbs switching if behind (the unit column stays in the schema).

**Never cut:** CSV import and backup (they carry file-handling points), the `Exercise` hierarchy, exceptions and handlers, collections-based services, CRUD, at least three working web screens, the UML, README and demo.

Because Flyway is kept, Schema V1 holds only the must-ship tables; cut features can arrive later as new migrations.

**Approach:** thin end-to-end slice first (one exercise saved and listed from the phone over Tailscale), then deepen the core, then widen.

| Day | Focus |
|---|---|
| Sun Nov 22 | Step 1 schema and Step 2 UML (approval gates), Maven skeleton, thin slice for exercises end to end |
| Mon Nov 23 | Core domain, services, exceptions, JUnit tests, repositories, all CRUD endpoints |
| Tue Nov 24 | Front end: workout screens (start, log, finish, summary) and exercises |
| Wed Nov 25 | History, bodyweight and gyms, CSV import/export, backup and recovery |
| Thu Nov 26 | Freeze, hardening, final UML, README, Javadoc, demo script, two rehearsals |
| Fri Nov 27 | Deadline; morning is buffer |

## 11. CSV, backup and recovery

- `CsvSerializable` on each entity (header and row), with a static "build from row" factory per entity for import.
- **Import:** skip bad rows and report them. Child rows whose parent was skipped are reported as orphans. A different UUID whose name collides with an active name is skipped and reported. Duplicates match on UUID.
- **Export** includes soft-deleted gyms and exercises so history stays valid. A small format-version manifest is PROPOSED.
- Hand-written CSV must handle quoting, embedded commas and newlines, and UTF-8.
- **Backups:** manual for now, built so scheduled backups can be added later. Use SQLite's `VACUUM INTO` (never a raw copy of a live database) plus the CSV export. One automatic backup before any permanent delete. Copy backups off the laptop occasionally.
- Corruption detection and `.db` backup logic live in the persistence layer behind a port defined in the core. CSV handling and orchestration stay in the core.
- **Corrupted database at startup:** rename the bad file, restore the latest valid backup if one exists, show a clear warning (never roll back silently).
- Demo should include a deliberate corrupted-file recovery to show the file-handling item.

## 12. Security and operations

- Javalin binds to **localhost** and is fronted by `tailscale serve`. **Never use Tailscale Funnel** (it exposes the app publicly).
- No login in v1. Mitigations: reject requests whose Origin or Host is not the app's own, server-side validation is the authority, parameterized SQL, no secrets in git.
- The laptop must stay on and awake during a workout. Run manually at first (build the front end, copy it where Javalin serves it, run a single jar); a NixOS systemd user service can come later.
- SQLite notes for the schema step: enable foreign keys on every connection; consider WAL mode with a busy timeout; `NOCASE` only folds ASCII, so Java normalizes names.

## 13. Dev environment and repository

- **NixOS (primary).** `flake.nix` dev shell: JDK 25 (`JAVA_HOME` from the JDK), Maven (to bootstrap the wrapper), Node, SQLite, git, gh, curl, httpie, jq; Linux systems only; formatter alejandra. nixpkgs tracks `nixos-unstable`. **The flake has not been run yet.** If `pkgs.jdk25` is missing, use `jdk21` and set the Maven `release` to 21.
  - **Pending two-line edit:** change `description` to `"Myophoria - development environment"` and the `shellHook` echo text from `lift dev shell:` to `myophoria dev shell:`.
- **Windows (sometimes), no Nix.** Native JDK, Maven (via the wrapper) and Node. Keep versions aligned through project files: Maven `release` setting, the wrapper, `.nvmrc` or `engines`, committed lockfiles. The flake files are harmless there.
- **Files:** `.gitattributes` has `* text=auto eol=lf`, `*.cmd text eol=crlf`, `*.bat text eol=crlf`. `.gitignore` covers build output, `web/node_modules`, `web/dist`, `src/main/resources/public/`, data and backups (`*.db`, `-wal`, `-shm`, `data/`, `backups/`, `exports/`), `.direnv/`, editors. Keep the executable bit on `mvnw`; avoid filenames differing only by case.
- **`scaffold.sh`** (Linux/bash): `bash scaffold.sh [folder]` (default `myophoria`) creates the directories above, config files and docs placeholders, copies `flake.nix` and `PROJECT_MASTER_PLAN.md` if they sit beside it, and makes the first commit. Tested. It deliberately creates **no Java stubs, no `pom.xml`, no Vite project, no LICENSE** (class names come from the UML step; dependency versions are verified when the pom is written).
- **Existing GitHub repo:** an older scaffold (Swing, `javac`, TSV layout) was already committed. Migrate by running the new script into a fresh folder, deleting the old directories in the old repo, copying the new tree in, and committing as `refactor: restructure for web stack`. The old `Makefile`, `build.ps1`, `store/` and `ui/` are obsolete.
- **Git workflow (PROPOSED):** `main` always builds; short-lived branches per issue (`feat/`, `fix/`, `docs/`, `chore/`); Conventional Commits; squash-merge PRs with `Closes #n`; tag `v0.1.0` (thin slice) and `v1.0.0` (submission). Branch order: `chore/maven-skeleton`, `feat/schema`, `docs/uml-v1`, `feat/exercise-slice`, `feat/core-domain`, `feat/core-services`, `feat/persistence`, `feat/api-crud`, `feat/web-shell`, `feat/web-workout`, `feat/web-exercises`, `feat/web-history`, `feat/web-bodyweight-gyms`, `feat/csv-export-import`, `feat/backup-recovery`, `chore/hardening`, `docs/final-uml-readme`, `docs/demo-script`. Stretch: `feat/progression-chart`, `feat/templates`, `feat/permanent-delete`, `feat/unit-switch`, `chore/tailscale-access`.
- **GitHub issues (PROPOSED):** one issue per branch; a milestone per day plus "Stretch"; labels by type (`feature`, `bug`, `docs`, `chore`), area (`core`, `persistence`, `api`, `web`, `csv`) and priority (`must`, `should`, `stretch`); one pinned rubric-tracking issue (checklist of the nine rubric items linking to the covering issues).

## 14. Schema preview (to be designed in Step 1; not final)

Expected tables: `gyms`, `exercises`, `workout_sessions`, `workout_sets`, `bodyweight_entries` (plus `workout_templates` and `template_exercises` if templates are kept).

| Table | Expected columns |
|---|---|
| `gyms` | id, name, is_default, deleted_at |
| `exercises` | id, name, equipment_type, laterality (default bilateral), deleted_at |
| `workout_sessions` | id, gym_id, started_at (UTC), ended_at (nullable), local_date |
| `workout_sets` | id, session_id, exercise_id, set_type, weight, unit, reps, bodyweight_snapshot (required for Calisthenics, null otherwise), set order, completed_at |
| `bodyweight_entries` | id, weight, unit, recorded_at (UTC), local_date |
| `workout_templates`, `template_exercises` | template name; template_id, exercise_id, position |

Constraints and indexes to settle: foreign keys enforced; `CHECK` weight >= 0 and reps > 0; a partial unique index for exactly one default gym; a partial unique index for at most one open session (`ended_at IS NULL`); a case-insensitive unique index on active exercise names; sets cascade with their session; permanent exercise delete cascades its sets. Unit memory is derived by query (no table). Flyway migration `V1__...sql` holds only the must-ship tables.

## 15. Questions already resolved (do not re-ask)

| Topic | Answer |
|---|---|
| Does a web UI count for the GUI rubric? | Yes (professor confirmed) |
| Are libraries, Javalin and SQLite allowed? | Yes; SQLite + CSV import/export + backup satisfies file handling |
| Project approved? | Yes |
| Backend, front end, styling, build, remote access | Javalin, Svelte + TypeScript, Tailwind, Maven, Tailscale |
| Dumbbell weight meaning | Per dumbbell |
| Weighted bodyweight | Replaced by Calisthenics; bodyweight and added weight separate; pure bodyweight = 0 kg added, no distinction; bodyweight snapshot required |
| Display of Calisthenics sets | `70 | 20` only; no combined total in v1 |
| Unilateral volume | Same as bilateral |
| Exercise labels | `unilateral` and `bilateral` only |
| Exercise deletion | Soft delete; on name conflict: restore, rename or permanently delete (cascades sets, with confirmation and auto backup); Deleted exercises list; no permanent purge for gyms |
| Warm-up handling | `SetType` enum; excluded from PRs, volume, graphs; default 80% of last top working weight at the gym |
| Failure sets | F counts as a working set; numbering counts normal sets only; label cycles number, W, F |
| Units | Stored as entered; remembered per gym and exercise (derived); dropdown on the KG label; reporting unit kg with `?unit=lb` |
| Per-session average | Mean working-set weight per exercise per session |
| Timestamps | UTC instant plus local calendar date |
| Equipment type edits | Immutable once the exercise has sets |
| Active gym | Derived from the most recent session; default flag fallback; locked per session |
| Session lifecycle | Explicit Start and Finish; clock starts at the Start tap; resume-or-finish; discard zero-set workouts; optional times for backfilled sessions |
| Exercise "type" in the picker | One searchable exercise list (like "Leg Press", "Bench Press"); optional equipment filter chips; no category tier |
| Muscle group | Future, as a field on `Exercise` |
| Templates | Minimal, in v1 (first cut candidate) |
| Checkbox meaning, prefill rules, same-exercise-twice | Completed sets only; prefill from last session's matching set; reopening an exercise reopens its card |
| Images | Initials badge in v1 (all initials; one word = first two letters); real images later |
| Exercise library at first launch | Empty; the user adds their own |
| Notes | None in v1 (exercise or session) |
| Rest timer and previous column | Removed |
| Gym fields | Name only (plus default flag and soft delete) |
| Hierarchy shape | Flat: abstract `Exercise` + five subclasses, no `FreeWeightExercise`; `VolumeCalculable` on `WorkoutSet` and `WorkoutSession` |
| CSV interface | `CsvSerializable` on the entities (not separate mapper classes) |
| Type-to-subclass mapping | One static method on `Exercise`; no factory class |
| Exceptions | Four custom exceptions as in section 6; `CorruptedCsvException` checked, the rest unchecked; no base class |
| Backups | Manual for now, scalable to scheduled |
| Network and security | Localhost bind plus `tailscale serve`; never Funnel; no login v1; Origin/Host check |
| Build order and ops | Thin slice first; manual packaging; clear error on failed save; copy backups off the laptop; UML kept as text (Mermaid) |
| Package and name | App name Myophoria; base package `myophoria`; Maven group ID deferred |
| AI-written code | Fine as long as the developer understands it |
| Scope cuts (2026-11-25) | None: full v1 scope; the section 10 cut list is retired and the deadline does not drive the plan |
| Git workflow (2026-11-25) | Branch per phase, one commit per task, no issues or PRs (see `PLAN.md`) |

## 16. Open items and next steps

**Open or unconfirmed:**

- Confirm the cut list, the 5-day plan, and the single-module layout (all PROPOSED).
- Is Friday a submission date or a live demo and Q&A?
- ~~Literal rubric wording~~ resolved 2026-11-25: validated setters, plain getters.
- Maven group ID (deferred); how the 3 to 4 letter badge fits the UI (front end).
- Apply the two-line `flake.nix` rename edit; run `nix develop` for the first time (flake and JDK 25 availability are untested).
- Migrate the old committed scaffold into the new layout.
- Peer-evaluation handling for a solo project (unconfirmed).
- Open UI questions are listed in section 8.7 (navigation, chart placement, set-delete gesture, badge fit, template builder); the full layout is finalized after the backend exists.

**Next steps, each only on the developer's explicit "go":**

1. **Step 1:** design the SQLite schema, explain each table and decision, then **stop for approval**.
2. **Step 2:** design the class hierarchy and UML (classes, interfaces, exceptions, which collection each service uses, Mermaid diagram), then **stop for approval**.
3. Maven skeleton (`pom.xml`, wrapper, verified dependency versions), then the thin slice, then the core, persistence, API, front end, CSV and backup, hardening and docs, following section 10.

## 17. Notes for the next assistant

- Autonomous mode since 2026-11-25 (`PLAN.md` §0.2): build without per-step approval; surface conflicts in `PLAN.md` §8.
- Treat untagged items as fixed, PROPOSED and OPEN items as questions to confirm, and do not re-ask anything in section 15.
- Keep the Java core clean and explainable: the grade depends on its OOP structure, exceptions, collections, file handling, and a UML that matches the code.
- Prefer minimal solutions. If an addition does not earn its place, say so.
- Surface conflicts and gaps honestly before building on them.
