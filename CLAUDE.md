# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Myophoria is a gym lift tracker: a pure Java core, a Javalin REST API, and a mobile-first Svelte web UI, backed by SQLite. All tasks in `PLAN.md` are done and `v1.0.0` is tagged; what remains is the developer studying `docs/DEFENSE.md` before the defense. See the board in `PLAN.md` §1. Dependency versions are logged in `PLAN.md` §9 — don't invent versions.

## Environment and commands

- `nix develop` (or direnv via `.envrc`: `use flake`) provides JDK 25, Maven, Node, SQLite CLI, gh, httpie, jq. `JAVA_HOME` is set by the shell. Only `x86_64-linux` is supported.
- Use `./mvnw` (e.g. `./mvnw verify`; single test: `./mvnw test -Dtest=ClassName#method`); system `maven` only bootstrapped the wrapper.
- Front end: `cd web && npm run dev` (Svelte + Vite + TypeScript + Tailwind; proxies `/api` to :7070). `npm run build` writes into `src/main/resources/public/`.
- Format Nix with `nix fmt` (alejandra). Commit `flake.lock`.

## Architecture (intended layout)

Ports-and-adapters split under base package `myophoria`:

- `myophoria.core` — pure Java domain, no framework/DB dependencies:
  - `model` (domain types), `service` (use cases), `port` (interfaces the core needs, e.g. repositories), `csv` (CSV import/export), `error` (domain errors).
- `myophoria.persistence` — SQLite implementations of the core ports, plus backup and corruption recovery; schema migrations in `src/main/resources/db/migration/`.
- `myophoria.api` — Javalin REST endpoints under `/api/v1`, exception handlers, bootstrap; adapts HTTP to core services.
- `web/` — Svelte UI. Its build output goes to `src/main/resources/public/` (gitignored), so the backend serves the built front end.
- Tests for the core live in `src/test/java/myophoria/core`.

Dependencies point inward: `api` and `persistence` depend on `core`, never the reverse.

## Start here: PLAN.md

Every model (Opus plans, Sonnet executes, Haiku explains) starts by reading `PLAN.md` §0 (protocol and your role), §1 (task board) and the task card for your task (the next `TODO` whose dependencies are `DONE`, or the one the developer named). Read only the files that card lists; do not re-read the whole repo. Before starting a task, tell the developer which model the board's Model column assigns to it, and ask them to switch (`/model opus|sonnet|haiku`) if the current model differs. Record progress, questions and contract changes in `PLAN.md` as §0 describes.

## Source of truth

`PLAN.md` holds the architecture, contracts (classes, schema, API, CSV), task board and log. `handoff.md` is the source of truth for domain rules, scope and open questions, referenced from `PLAN.md` by section. Items tagged PROPOSED or OPEN there must be confirmed with the developer, and resolved questions (section 15) must not be re-asked. The project runs in **autonomous mode** (PLAN.md §0.2): models write code and work through the task board without asking the developer to approve code or designs; the developer reviews the finished program before the defense.

## Docs to keep in sync

- `docs/DESIGN.md` — architecture and design rules.
- `docs/DECISIONS.md` — decision log; one entry per decision: date, decision, reason.
- `docs/API.md` — endpoint reference (`/api/v1`).
- `docs/DATA_FORMAT.md` — CSV export/import format.
- `docs/uml/classes.md` — Mermaid class diagram; **must match the code**.

## Conventions

- `.editorconfig`: UTF-8, LF, 4-space indent; 2 spaces for nix/json/yaml/ts/js/svelte/css/html/md.
- Never commit runtime/personal data: `data/`, `backups/`, `exports/`, `*.db*` are gitignored.
- Commit messages use Conventional Commits style (e.g. `chore: initial project skeleton`).

<!-- rtk-instructions v2 -->
# Command output

Command output here is condensed to save tokens, keeping every signal and
dropping costly noise. Treat it as the complete result: run commands
normally, and batch related commands into one call to avoid extra turns.
Truncated results state their recovery path in their own output. Re-run a
command as `rtk bypass <cmd>` only when its result is unusable: empty when
output was clearly expected, contradicting its exit code, or garbled.

Delegation commands (they hand work to a cheaper model; the files never enter your context):
- `rtk bulk-read --question "..." <paths...>`
- `rtk code-write --spec "..." --reference <file> --target <file>`
<!-- /rtk-instructions -->