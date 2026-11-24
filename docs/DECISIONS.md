# Decision log

One entry per decision: date, decision, reason.

## 2026-11-25 — Full v1 scope, no cuts
**Decision:** Build the full v1 scope from handoff §9; the cut list in handoff §10 is retired and the deadline does not drive the plan.
**Reason:** The developer considers v1 already minimal.

## 2026-11-25 — Git workflow: branch per phase
**Decision:** One short-lived branch per build phase (see PLAN.md §1), one Conventional Commit per task, merged into main after a phase review. No GitHub issues, milestones or PRs.
**Reason:** Solo developer; keeps history readable without per-feature ceremony.

## 2026-11-25 — PLAN.md as the shared plan for all models
**Decision:** PLAN.md holds the protocol, task board, contracts and log. Opus plans and reviews, Sonnet executes named tasks, Haiku answers questions; each starts from PLAN.md after /clear.
**Reason:** Models switch often; a single file replaces re-reading the repo or the transcript.

## 2026-11-25 — Own Migrator instead of Flyway (D1)
**Decision:** A small `Migrator` in `myophoria.persistence` runs `db/migration/V<n>__*.sql` resources in order and tracks the schema version with `PRAGMA user_version`.
**Reason:** One less dependency, plain explainable JDBC, no Flyway history table in the database or backups; the Flyway file naming keeps a later switch cheap.

## 2026-11-25 — Explicit is_open flag on sessions (G1)
**Decision:** `workout_sessions.is_open` (0/1) with a partial unique index allowing at most one open session; `started_at` and `ended_at` are both nullable; backfilled sessions are inserted closed.
**Reason:** Using `ended_at IS NULL` as "open" would make backfilled sessions without an end time look like running workouts and block new ones.

## 2026-11-25 — Exercise subclass overrides with v1 callers; usesPlates deferred (G2)
**Decision:** `Exercise` declares abstract `weightLabel()` and overridable `validateWeight()`, `requiresBodyweight()` and `formatSet()`; every subclass implements `weightLabel()` ("total", "per dumbbell", "stack", "plates", "added"), Dumbbell and Calisthenics override `formatSet()`, Calisthenics overrides `validateWeight()` and `requiresBodyweight()`. `usesPlates()` is dropped until the v2 plate calculator defines what it needs.
**Reason:** Every override has a caller visible in the app, so each of the five subclasses can be defended in the Q&A; adding plate behaviour later is additive and gets the shape the calculator actually needs.

## 2026-11-25 — Normalised name keys for exercises and gyms (G3)
**Decision:** Java stores `name_key` (NFC, trimmed, single spaces, `toLowerCase(Locale.ROOT)`) on exercises and gyms; a partial unique index on active rows enforces uniqueness as a safety net. Gym names are unique among active gyms.
**Reason:** SQLite `NOCASE` folds ASCII only; duplicate gym names would make pickers and History ambiguous.

## 2026-11-25 — Prefill converts into the set's unit (G4, G5)
**Decision:** The Calisthenics bodyweight snapshot is stored in the set's unit, converted from the latest bodyweight entry; prefilled rows from the last session are converted into the remembered unit.
**Reason:** Every number on a set row shares the unit shown in the KG/LB dropdown, so display and volume math never mix units.

## 2026-11-25 — Only closed sessions are cleaned up when emptied (G6)
**Decision:** Removing the last set deletes the session only if it is closed; an open workout with zero sets stays until Finish, where the discard prompt applies.
**Reason:** Unticking the only set mid-workout must not end the workout.

## 2026-11-25 — Transactions stay inside the SQLite adapter (G7)
**Decision:** No transaction abstraction in core. Multi-row writes (switch default gym, permanent exercise delete, template save) are single repository methods running one transaction in the adapter. CSV import is row by row and takes an automatic backup first.
**Reason:** All-or-nothing where it matters without an extra interface; core stays JDBC-free.

## 2026-11-25 — Startup warnings via a status endpoint (G8)
**Decision:** `GET /api/v1/status` returns warnings from startup recovery; the UI shows them as a banner.
**Reason:** Handoff §8.6 requires corrupted-database recovery to never be silent, including on the phone.

## 2026-11-25 — No router or chart library (G9, G14)
**Decision:** Hash-based navigation in `App.svelte`; the progression chart is hand-written inline SVG.
**Reason:** Two fewer dependencies and no server-side SPA fallback; one chart with one series doesn't need a library.

## 2026-11-25 — Import never overwrites; templates follow exercise deletes (G10, G11)
**Decision:** Import skips rows whose ID already exists ("already present") and clears imported default-gym/open-session flags that would conflict (reported). Soft-deleted exercises are hidden from templates and return on restore; permanent delete removes them from templates.
**Reason:** Re-importing a backup is harmless and soft delete stays fully reversible.

## 2026-11-25 — Server-computed display values, env config, Maven coordinates (G12, G13, G15)
**Decision:** Initials and weight labels are computed in Java and sent by the API. Configuration is read in `Main` from `MYOPHORIA_PORT` (7070), `MYOPHORIA_DATA_DIR` (`./data`) and `MYOPHORIA_ALLOWED_HOSTS`. Maven coordinates `myophoria:myophoria:0.1.0-SNAPSHOT`.
**Reason:** The front end never recomputes domain rules; no config class or file is needed; the project is not published to Maven Central.

## 2026-11-25 — Schema V1 approved (Step 1)
**Decision:** The seven-table schema in PLAN.md §5 is approved as the contract for `V1__init.sql`: UUID text keys, enums by name with CHECKs, `deleted_at` soft delete, partial unique indexes (one default gym, one open session, unique active names), a seeded Default gym with a fixed ID, no stored totals or unit-memory table.
**Reason:** Matches handoff §7 and §14 plus the Gate A rules; Java stays the authority and the indexes are safety nets.

## 2026-11-25 — No cascade from exercises; explicit permanent delete
**Decision:** `workout_sets.exercise_id` and `template_exercises.exercise_id` reference `exercises` without `ON DELETE CASCADE`; permanent delete removes sets, emptied closed sessions, template rows and the exercise explicitly in one transaction.
**Reason:** An accidental hard delete fails loudly instead of silently erasing history, and each step is visible in code.

## 2026-11-25 — STRICT tables
**Decision:** Every table is declared `STRICT`.
**Reason:** SQLite otherwise stores values of any type in any column; STRICT surfaces mapping bugs at the first insert.

## 2026-11-25 — Fixed-width timestamp text
**Decision:** All instants are stored and exported as `uuuu-MM-dd'T'HH:mm:ss.SSS'Z'` (UTC) via one shared `CsvCodec.TIMESTAMP` formatter.
**Reason:** `Instant.toString()` varies in length, which breaks text ordering (`…00.500Z` sorts before `…00Z`).

## 2026-11-25 — Class design approved (Step 2)
**Decision:** PLAN.md §4 and docs/uml/classes.md are the class contract: `Exercise` hierarchy, six `CsvSerializable` entity types, `VolumeCalculable` on sets and sessions, four exceptions, seven ports, eight services with result records, SQLite adapters and Javalin controllers. Services generate UUIDs; services that stamp times take a `java.time.Clock`.
**Reason:** Covers the rubric's class design, inheritance, abstraction, collections and exceptions items; the Clock keeps time-dependent tests deterministic.

## 2026-11-25 — Autonomous build mode
**Decision:** Models build the project through the PLAN.md board without asking the developer to approve code or designs; Opus answers design questions and reviews each phase. The developer reviews and studies the finished program before the defense, supported by docs/DEFENSE.md (T7.4) and a Q&A drill (T7.5).
**Reason:** Developer's choice; replaces the handoff's "wait for go before every step" agreement.
