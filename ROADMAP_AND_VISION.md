# Myophoria: Vision, Roadmap and Scalability

What the app is meant to become, how it gets there (v1, v1.x, v2, future builds), and how the architecture is built to grow. It collects the vision and planning discussions in one place, including ideas that were parked and the reasoning behind them.

**Source and status tags**

- **DECIDED**: the developer explicitly confirmed it.
- **PROPOSED**: an assistant recommendation the developer has not confirmed.
- **PARKED**: from the developer's original handoff or an earlier idea, deliberately not in the current plan and not re-confirmed.
- **OPEN**: unanswered.

As of 2026-11-22. Nothing is coded yet. For the exact current design (domain rules, class design, schema preview), see `handoff.md`; this document is the forward-looking companion.

---

## 1. Vision

### 1.1 What Myophoria is

A personal gym lift tracker for logging workouts (exercises, sets, weight, reps), tracking bodyweight, and seeing progress. It is meant to be used on a phone, one-handed, between sets, and it should feel fast and low-friction.

It is two things at once:

1. The **CS108 final project** (Java OOP, BSCS 2B), where the **Java core is the graded centerpiece**.
2. A **personal GitHub project** that is meant to live on after the course.

### 1.2 What the developer wants it to become

- Usable on iPhone, Android and laptop, with the data under the developer's own control and kept on the developer's own machine, "similar to how local hosting works".
- The laptop runs the server and the database, and the phone reaches it from the gym without needing the same Wi-Fi.
- A modern, scalable stack, not a throwaway: clean boundaries so it can grow (more features, always-on hosting, more users) without a rewrite.
- A Java core that stays genuine and explainable, with the web UI as the front end.
- A polished everyday training log: quick set entry, last-time values prefilled, per-gym memory, templates, and meaningful progress insights.
- Built minimally (suckless): no unnecessary code, classes or abstractions.

### 1.3 The core experience (developer-described)

- Before picking an exercise, a start screen offers **a blank workout or a template** (for example an "Upper Day" with its related exercises).
- Pick an exercise from one searchable list; the gym defaults automatically and can be changed at the start.
- A **logging screen modeled on a reference screenshot of a workout app**: exercise image and name at the top, rows of sets, a weight column in kg or lbs, a reps column, a completion checkbox and an "Add set" button. The set label is a number, **W** for warm-up or **F** for a failure set.
- Weight and reps **auto-fill from the last recorded set**, so repeating last time takes a single tap.
- Removed from the reference: the rest timer, the "previous" column and the description text. The image becomes an **initials badge** for now (Bench Press = BP, Incline Dumbbell Press = IDP, Squat = SQ).
- A **finish flow** ending on a summary screen (number of sets, workout time and more).
- Calisthenics (pull-ups, dips) logged with bodyweight and added weight kept separate (`70 | 20`).
- Units (kg or lbs) remembered per gym and per exercise.
- Reaching the app from the gym over Tailscale, with no public exposure.

### 1.4 Principles that shape every decision

- **The Java core is the centerpiece.** Domain and services stay framework-free (ports and adapters). If the core wants to import Javalin, JDBC or Jackson, the design is wrong.
- **Suckless.** Do things as minimally as possible. If an addition does not earn its place, leave it out. Examples already applied: no free-weight parent class, no extra category field on the equipment enum, gyms are name-only, no factory class (one static method instead), a derived unit memory (no preferences table).
- **Mobile-first and fast to log.** The most frequent action, logging a set, must take very few taps.
- **Private by default.** Tailscale access, localhost binding, never a public exposure.
- **Boring, mainstream infrastructure.** Dependencies serve infrastructure (HTTP, JSON, database, tests) and never replace domain logic.
- **Clean boundaries instead of heavy infrastructure.** Scalability here means swappable adapters, stable IDs and migrations, not microservices, Kubernetes, queues or caches.
- **Design now what is expensive to change later** (IDs, units, time, API versioning, schema migrations); defer what is cheap to add later (images, muscle group, settings).

---

## 2. Roadmap at a glance

| Version | Theme | Status |
|---|---|---|
| v0.1.0 | Thin end-to-end slice: one exercise created and listed from the phone over Tailscale | DECIDED (tag) |
| v1.0.0 | The submission: all must-ship v1 features | DECIDED (tag) |
| v1.x | Post-submission: bring back anything cut for the deadline and harden | PROPOSED |
| v2 | Insights and tools: PRs, 1RM and plate calculators, rest timer, streaks, extra charts, settings (muscle group enables the analytics) | Scope DECIDED as "v2"; ordering PROPOSED |
| v3+ / Future | Always-on hosting, scheduled backups, images, login and multi-user, offline-first, native wrapper | Direction DECIDED, details PROPOSED |

Version numbers beyond v1.0.0 are a proposed naming scheme; the order inside each is guidance, not a commitment.

---

## 3. v1 (the submission)

### 3.1 v1 scope (DECIDED)

- Active workout logging: warm-up (W) and failure (F) sets, per-gym and per-exercise unit memory, the `bodyweight | added` display for Calisthenics, prefilled rows.
- A start screen (blank workout or minimal template) and an explicit Start and Finish with a summary.
- Independent bodyweight tracking.
- Lightweight gym profiles (name, default flag, soft delete).
- Exercise management with soft delete, restore, rename or permanent delete on name conflicts, and search and filter by name, equipment type and laterality. The exercise library **starts empty**; the user adds their own.
- Workout Summary report: total sets, working sets, total volume, top weight and average per exercise, start and end time, duration.
- Filterable History screen with editing (this also covers the rubric's search and filter requirement).
- One core progression chart: top working weight over time per exercise.
- Minimal templates (name plus an ordered list of exercises).
- CSV import and export, database backup (manual) and recovery from a corrupted file.
- Full CRUD, including edits of logged sets, exercises, gyms and bodyweight entries.

### 3.2 Course constraints that shape v1

The rubric (100 points: class design 15, inheritance and polymorphism 15, GUI 14, collections 10, functionality 10, documentation and UML 10, presentation 10, exceptions 8, file handling 8) and a live Q&A against the UML mean v1 intentionally shows off core Java: a flat `Exercise` hierarchy with five subclasses, two interfaces (`VolumeCalculable`, `CsvSerializable`), four custom exceptions, collections-based service logic, and a CSV and backup design that survives missing or corrupted files.

### 3.3 Cut line for the deadline (PROPOSED)

The deadline is Friday 2026-11-27. If time runs short, drop in this order:

1. Tailscale and phone access (demo on the laptop browser at phone width; try Tailscale only if ahead).
2. Templates and "Save as template".
3. Permanent delete and the Deleted exercises list (soft delete, restore and rename stay).
4. The progression chart (the summary report already covers "report").
5. The ZIP bundle and manifest (export and import become a folder of CSV files).
6. kg and lbs switching (the unit column stays in the schema, so no migration later).

**Never cut:** CSV import and backup (they carry file-handling points), the `Exercise` hierarchy, exceptions and handlers, collections-based services, CRUD, at least three working web screens, the UML, the README and the demo.

Because Flyway is used, Schema V1 can hold only the must-ship tables, and anything cut can return later as a new migration.

### 3.4 How v1 gets built

**Approach:** thin end-to-end slice first (de-risks Javalin, SQLite, Svelte and Tailscale together), then deepen the core, then widen. Keep the demo working at every stage.

**Steps, each requiring the developer's explicit "go":**

1. Design the SQLite schema, then stop for approval.
2. Design the class hierarchy and UML (classes, interfaces, DTO records, exceptions, repository ports, the exact collection each service uses), then stop for approval.
3. Maven skeleton with verified dependency versions, then the thin slice, then core, persistence, API, front end, CSV and backup, hardening and docs.

**5-day plan (PROPOSED):**

| Day | Focus |
|---|---|
| Sun Nov 22 | Schema and UML (approval gates), Maven skeleton, thin slice for exercises |
| Mon Nov 23 | Core domain, services, exceptions, JUnit tests, repositories, all CRUD endpoints |
| Tue Nov 24 | Front end: workout screens (start, log, finish, summary) and exercises |
| Wed Nov 25 | History, bodyweight and gyms, CSV import and export, backup and recovery |
| Thu Nov 26 | Freeze, hardening, final UML, README, Javadoc, demo script, two rehearsals |
| Fri Nov 27 | Deadline; morning is buffer |

An earlier 12-day plan was dropped when the deadline was corrected to five days. It is still open whether Friday is a submission date or a live demo and Q&A.

**Branch plan (PROPOSED):** `main` always builds; short-lived branches per issue, squash-merged. In order: `chore/maven-skeleton`, `feat/schema`, `docs/uml-v1`, `feat/exercise-slice` (tag v0.1.0), `feat/core-domain`, `feat/core-services`, `feat/persistence`, `feat/api-crud`, `feat/web-shell`, `feat/web-workout`, `feat/web-exercises`, `feat/web-history`, `feat/web-bodyweight-gyms`, `feat/csv-export-import`, `feat/backup-recovery`, `chore/hardening`, `docs/final-uml-readme`, `docs/demo-script` (tag v1.0.0). Stretch: `feat/progression-chart`, `feat/templates`, `feat/permanent-delete`, `feat/unit-switch`, `chore/tailscale-access`.

**Issues (PROPOSED):** one per branch; milestones per day plus "Stretch"; labels by type (`feature`, `bug`, `docs`, `chore`), area (`core`, `persistence`, `api`, `web`, `csv`) and priority (`must`, `should`, `stretch`); a pinned rubric-tracking issue (a checklist of the nine rubric items linking to the covering issues). A good issue has a one-line goal, acceptance criteria (behavior, validation and error cases, tests or a manual check, docs) and notes.

**Definition of done for a feature:** it works, validates input, handles failure cases, has a test (core) or a manual check (web), and is documented.

**Testing plan (PROPOSED):** JUnit on the core (the graded layer); a few persistence tests against a temporary SQLite database; curl or httpie for the API; manual front-end checks; a failure-case checklist (missing file, corrupt line, huge number, negative number, empty name, duplicate name). CI on GitHub comes after the deadline.

**Documentation and demo plan:** UML kept as text (Mermaid, rendered natively on GitHub) so it updates with the code and matches it; a decision log; design, API and data-format docs; Javadoc on public classes and methods; a README with a screenshot and run instructions. The demo script tells a story (a day at the gym), is rehearsed twice, and includes a deliberate corrupted-file recovery to show the file-handling item.

---

## 4. v1.x: after the submission (PROPOSED)

Return anything that was cut, in the same order of value:

- Templates, "Save as template", and the permanent delete flow if they were dropped.
- The progression chart, kg and lbs switching, and the ZIP bundle with a format-version manifest if dropped.
- Tailscale polish and a systemd user service on NixOS so the server starts on its own.
- A runnable jar attached to a GitHub release, plus a LICENSE (MIT was suggested, not chosen), a changelog and a README screenshot.
- CI on GitHub: build and test on every push.
- Bug fixes found in real gym use. Real use is the best source of what v2 should prioritize.
- Open items to resolve: navigation structure, where the chart lives, the set-delete gesture, the template builder UI.

---

## 5. v2 backlog

Everything the developer explicitly deferred to v2, plus the original handoff's ideas that were not in v1. Items marked PARKED come from the first handoff and were not re-confirmed under the new design.

### 5.1 Tools

- **Rest timer** (DECIDED as v2). The original idea was a timer button at the top right of every screen, with a configurable default rest time.
- **1RM calculator** (DECIDED as v2). Original design: a `OneRepMax` interface with Epley and Brzycki implementations. **Best estimated 1RM** also feeds PRs and trends.
- **Plate calculator** (DECIDED as v2), working in both directions (weight to plates, and plates to weight). The domain is already prepared: `usesPlates()` is true for Barbell and PlateLoaded exercises. Needs configurable bar weight and available plates.
- **Warm-up button** (PARKED): an optional button that adds a first set at 80% of past weight. In v1, warm-ups already prefill at 80% of the last top working weight, so this is largely covered.
- **Settings screen** (DECIDED as v2): the place for kg and lbs defaults, bar weight, plates and rest time.

### 5.2 Insights and analytics

- **Personal records** (DECIDED as v2): heaviest weight, best estimated 1RM, most reps at a weight. Warm-ups never count. A "new PR" message when one is hit.
- **Streaks and frequency** (DECIDED as v2), by week, month or year.
- **Extra analytics charts** (DECIDED as v2). Original ideas: volume or sets per muscle group; total weight per day; weight per session for one exercise (an average by default, with a heaviest-set toggle); 1RM trend; weekly volume per muscle group; bodyweight trend.
- **Muscle group** (the developer wants it in future development): a field on `Exercise` (each exercise has one primary muscle group), not a parent that owns exercises. It is an additive migration (a new nullable column). It unlocks the per-muscle-group charts, weekly volume per muscle group, and optionally a "Chest, Legs" picker in front of the same exercise list. PROPOSED placement: early in v2, since several analytics depend on it.
- **RPE** (PARKED): the original handoff had optional RPE on a set; it is not in the current design.

### 5.3 Planning and organization

- Richer templates (PROPOSED): reordering, optional target set counts, updating a template from a finished workout. v1 templates are just a name and an ordered exercise list.
- **Starter exercise pack** (PROPOSED idea): the original handoff had starter lifts; v1 starts with an empty library by the developer's choice. An optional seeded pack of about 15 to 20 common lifts could be offered later via a migration.

### 5.4 Suggested v2 order (PROPOSED)

1. Quick wins that reuse existing data: PRs, the 1RM calculator, the plate calculator, the settings screen, the rest timer.
2. Muscle group, then the analytics that depend on it.
3. Streaks and frequency, and the extra charts.
4. Richer templates and the starter pack.

---

## 6. Future builds and scalability

The direction below was discussed as a staged path. Each stage keeps the same core and swaps adapters or adds layers.

### 6.1 Stages

| Stage | What changes | What it needs |
|---|---|---|
| 1. Now | Laptop-hosted, SQLite, Tailscale | Laptop on and awake during workouts |
| 2. Always-on host | A Raspberry Pi, mini PC or cheap cloud server runs the same code, packaged in Docker, with scheduled backups and a systemd service | No code redesign; backups copied off the host |
| 3. Multi-user | Accounts with proper login (hashed passwords such as bcrypt or Argon2), `user_id` on every row, PostgreSQL, hosted deployment | A new persistence adapter, a Flyway migration for `user_id`, authentication |
| 4. Offline-first | Installable PWA, service worker caching, a write queue, then sync using the existing UUIDs; a native wrapper such as Capacitor is an option beyond that | HTTPS (Tailscale provides it); conflict handling |

### 6.2 Other future items

- **Scheduled backups.** Backups are manual in v1 but built so a scheduler can call the same backup operation later (DECIDED). A retention rule such as keeping the last 7 was suggested.
- **Real exercise images** (DECIDED as later; v1 uses initials badges). Uploaded photos mean storing files, backing them up (the export becomes a ZIP with an image folder), seeding images for starter lifts, and not reusing another app's pictures. Adding an `image_path` column later is a small migration.
- **Login.** Not needed in v1 behind Tailscale. It becomes required if the app is ever exposed beyond the private network or becomes multi-user.
- **Other remote access options** if Tailscale is ever not enough: Cloudflare Tunnel (a public URL with login in front) or self-run WireGuard. Raw port forwarding was rejected (many ISPs use CGNAT, home IPs change, and it exposes an unauthenticated server). Tailscale Funnel must never be used for v1 since it exposes the app publicly.
- **A true on-device phone app.** A phone cannot run a Java server (iOS cannot run a JVM at all). Fully on-device use would mean a JavaScript or PWA rewrite of the logic, or experimental Java-to-browser tooling. Treat this as a separate post-course project, not part of this roadmap.

### 6.3 Known limits to revisit as it grows

- **Aggregation lives in Java collections** (deliberate, for the rubric, and fine at personal scale). At larger data sizes, heavy reports should move into SQL queries with proper indexes.
- **SQLite is single-writer.** Fine for one user; multi-user is the trigger to move to PostgreSQL.
- **The laptop must stay on and awake.** The always-on host stage removes this.
- **A bad gym signal.** Sets save over the network when ticked. v1 shows a clear error and leaves the row unticked for a retry. Offline queueing is the later fix.
- **No login in v1.** Safe only because access is private; it must change before any wider exposure.

### 6.4 What not to build

Microservices, Kubernetes, message queues, caching layers and heavy front-end state management. Nothing about a personal gym log needs them, and they would consume the time the real features need.

---

## 7. Architecture choices that protect the future

| Decision (now) | What it makes cheap later |
|---|---|
| Ports and adapters (pure core, adapters around it) | Swapping SQLite for PostgreSQL, or Javalin for another HTTP layer, without touching the core |
| UUID primary keys | Sync, offline entry, merging data from another device |
| Weight and unit stored exactly as entered | No rounding drift; converting only when comparing keeps history faithful |
| UTC instant plus local calendar date | Correct day grouping for any time zone, and any future multi-user use |
| Versioned API (`/api/v1`), DTOs separate from domain objects | Changing the UI or API without breaking the other, and a stable contract for a future native client |
| Flyway migrations with a lean Schema V1 | Adding `user_id`, muscle group, image path, templates or settings as small additive migrations |
| Soft delete for exercises and gyms | Preserving history while the library changes |
| Polymorphic `Exercise` (one static method maps type to subclass) | Adding an equipment type with one new subclass and one line |
| Server returns formatted set displays | UI changes (including a future native app) never need to know the equipment rules |
| CSV export plus `.db` backups | Data portability and disaster recovery from day one |
| Config outside the code, secrets out of git | A clean move to Docker and an always-on host |
| Localhost binding with `tailscale serve` | Private HTTPS now, and a secure context for PWA features later |

---

## 8. Standards and workflow

- **Git:** small commits, Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`), short-lived branches, squash merges, semantic version tags, `main` always buildable.
- **Open-source posture** (a personal GitHub project): a README with a screenshot and run instructions, a LICENSE, a changelog, optionally a short CONTRIBUTING file, and a runnable jar on releases.
- **Code:** standard Java naming; Javadoc on public classes and methods; one style guide used consistently; small focused classes. The original handoff's size limits (classes about 200 lines, methods under 30, no global mutable state) are PARKED and were not re-confirmed under the new design.
- **Web:** mobile-first, large tap targets, numeric keyboards for weight and reps, semantic HTML, escaping user text. Web norms are followed, but not strictly where they create friction.
- **Dev environment:** NixOS flake (JDK 25, Maven, Node, SQLite, git, gh, curl, httpie, jq) as the primary setup; Windows works with native tools and the Maven Wrapper, with versions aligned through the Maven `release` setting, `.nvmrc` or `engines`, and committed lockfiles; `.gitattributes` keeps LF line endings.

---

## 9. Deliberately not building

- From the original handoff: cloud sync, social features, nutrition tracking, themes, workout notes (notes were also removed from the current design), and exercise archiving (replaced by soft delete).
- Relaxed norms for a solo project: a full accessibility audit, internationalization, perfect Lighthouse scores, server-side rendering, high front-end test coverage, a formal design system, complex state management, end-to-end browser tests.
- Superseded designs: Swing UI, TSV storage, a `FreeWeightExercise` class, a separate pure-bodyweight exercise type, a rest timer and "previous" column on the logging screen, per-set notes.

---

## 10. Open questions for the roadmap

- Is Friday 2026-11-27 a submission date or a live demo and Q&A?
- Confirm the cut list and the 5-day plan (both PROPOSED).
- Navigation structure (a bottom tab bar was proposed), where the chart lives, the set-delete gesture, and the template builder UI.
- Whether any of the PARKED items (RPE, the size limits, the warm-up button) should come back.
- When to start v2, and whether muscle group should open it.
- The timing and approach for a LICENSE, a changelog and CI.
- Peer evaluation handling for a solo developer.
