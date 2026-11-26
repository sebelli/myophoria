# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

One person: the developer, a solo lifter. The main use is at the gym, on a phone held
in one hand, in short glances between sets: start a workout, log each set, check what
was lifted last time. The same UI also runs in a laptop browser for history, edits and
data tasks. There are no other users, accounts or roles.

The CS108 defense audience (professor, graders) is **not** a design audience; UI
decisions serve the lifter at the gym.

## Product Purpose

Myophoria is a personal gym lift tracker. It logs workouts set by set, tracks
bodyweight, and shows progress over time. It started as a CS108 final project (Java
OOP) and lives on as a personal GitHub project.

Success means a set is logged in very few taps, usually one, because weight and reps
are prefilled from last time, and the data stays complete, correct and under the
owner's control.

## Positioning

- **Private and self-hosted.** One Java process on the owner's laptop and one SQLite
  file. The phone reaches it over Tailscale. There is no cloud, no account and no
  public exposure: the server binds to 127.0.0.1 and `/api` has a Host/Origin guard.
- **Suckless.** It does the job with the fewest features, screens, classes and
  dependencies it can. Anything new has to earn its place.

## Operating Context

- **Hosting:** the laptop runs the server (Javalin REST API under `/api/v1` plus the
  built Svelte SPA). The phone opens it through `tailscale serve` over HTTPS inside
  the tailnet.
- **Workout flow:** start (blank or from a template; the gym is chosen and then locked)
  → pick an exercise → log sets (ticking a row saves it) → finish → summary, with an
  optional "Save as template".
- **Several gyms**, each with its own unit (kg or lb) and its own last-time prefill.
- **Data handling:** CSV export/import as a ZIP bundle, automatic `VACUUM INTO`
  backups before risky operations, and recovery from a corrupted database at startup.

## Capabilities and Constraints

**Built (v1.0.0):** workout logging with prefill; warm-up, working and failure sets;
five equipment types; exercise management (search, filter, soft delete, restore);
gyms; minimal templates; bodyweight log; filterable and editable history with
backfill; session summary; a per-exercise progression chart; CSV import/export;
backups.

**Terminology** (use these words in UI copy):
- *Workout* is the user-facing word for a session.
- Set labels: **W** is a warm-up, a number is a working set (numbers count working
  sets only), **F** is a failure set.
- A ticked set is saved. An unticked row is a draft, and drafts are discarded on
  Finish.
- Equipment types: Barbell (total weight), Dumbbell (per dumbbell, "30 kg each"),
  Stack, Plate-loaded, Calisthenics (bodyweight plus added weight, shown `70 | 20`).
- Exercises show an initials badge (Incline Dumbbell Press → IDP) instead of an
  image.

**Constraints:**
- No new front-end dependencies: no chart or UI libraries (decision G14). Charts stay
  hand-written SVG. The stack is Svelte 5, TypeScript, Vite and Tailwind 4.
- The front end never recomputes business logic (volume, top weight, unit
  conversion). It shows what the API returns.
- Single user: no authentication, no offline support.
- `main` must stay demo-able. UI work happens on `feat/ui-improvement` and is merged
  only when the developer says it is stable.

**Not built yet** (do not design as if they exist): PRs, 1RM and plate calculators,
rest timer, settings screen, streaks, extra charts, muscle groups. These are the v2
backlog in `ROADMAP_AND_VISION.md`. Planned UI work is in `UI_IMPROVEMENT_PLAN.md`.

## Brand Commitments

- The name is **Myophoria**. The favicon is `web/public/favicon.svg`.
- Exercises are represented by initials badges, not images.

## Evidence on Hand

- A synthetic demo dataset in `docs/demo/data/` (CSV bundle: exercises, gyms,
  sessions, sets, bodyweight, templates). The README and `docs/DEMO.md` use it.
- Real training data lives in `data/` and is personal. Never commit it, show it in
  screenshots or ship it.
- There are no testimonials, user counts, metrics, press or exercise images. Do not
  invent any.

## Product Principles

1. **Logging a set is the hot path.** It must work one-handed, in very few taps,
   prefilled from last time. Nothing should slow it down.
2. **Private by default.** Data stays on the owner's machine. Nothing is public and
   there are no accounts.
3. **Suckless.** Every feature, screen and dependency has to earn its place. Prefer
   cutting to adding.
4. **The server owns the numbers.** The UI displays what the core computes and never
   recomputes it.
5. **Never lose data silently.** Destructive actions are confirmed, risky operations
   are backed up first, and drafts are visibly drafts.

## Accessibility & Inclusion

- Tap targets of at least 44px, for one-handed use at the gym.
- Numeric keyboards on weight and reps fields.
- No formal WCAG level has been set.
