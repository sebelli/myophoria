# UI improvement plan

Status: **planned, not started.** Branch: `feat/ui-improvement`. Do not merge into `main`
until the developer says the UI is stable (`git switch main && git merge --no-ff feat/ui-improvement`).
Written 2026-11-26.

## Goal (developer's request)

1. Minimalist UI, red and black colour scheme.
2. No nested pages for exercises in a workout: the exercise opens as a collapsible
   drop-down in place, not on a separate `#/workout/<id>` page.
3. While a workout is running, show a bar at the bottom (on the other tabs) saying a
   workout is in session, with **Resume** and **End**. Ending must be able to discard
   the workout, so the user never has to go to History to delete a running workout.
4. Show the changes live with the Playwright MCP while building.

## Decisions (answered by the developer 2026-11-26)

| # | Question | Answer |
|---|---|---|
| 1 | Theme base: black background + red accents, or white background + black/red? | black background, red accents |
| 2 | Collapse: one exercise open at a time, or several? | one at a time |
| 3 | Also flatten History → session, Exercises → detail, More → sub-screens? | no, leave them |
| 4 | Test on a throwaway copy of `data/`? | yes |

Testing note: the live backend on the real `data/` keeps running untouched. The UI pass runs a
second backend on a snapshot (`sqlite3 data/myophoria.db ".backup <copy>"`) with
`MYOPHORIA_PORT=7071`, and Vite with `MYOPHORIA_API=http://127.0.0.1:7071 npm run dev`.

## Steps (one Conventional Commit each, scope `web`)

### 1. Theme: red and black (`feat(web): red and black theme`)

- `web/src/app.css`: black body background, near-black `.card`, thin dark-grey borders
  (the base-layer `border-color` rule), white text, red accent. `.btn-primary` solid red;
  `.btn-danger` red outline so danger stays distinct from primary; focus ring red.
- Swap the hard-coded `slate-*` / `green-*` / `amber-*` / `red-*` classes in the 19 files
  under `web/src` (`grep -rlE "slate|green-|amber-|red-" web/src`). Ticked set rows: red tint
  instead of `bg-green-50`; checkbox `accent-red-600`. Warm-up label stays amber.
- Check `ProgressChart.svelte` (SVG colours), `Badge.svelte`, `NavBar.svelte` (active tab red),
  `StatusBanner.svelte`, `ConfirmDialog.svelte` (dark dialog + backdrop).
- No new dependency.

### 2. Exercises collapse in place (`feat(web): collapsible exercise cards in the workout`)

- Move `web/src/screens/ExerciseCard.svelte` → `web/src/components/ExerciseCard.svelte`;
  take `exerciseId` as a prop instead of reading `route.parts[1]`. Keep all logic
  (drafts, tick, unit switch, warm-up prefill). Remove "← Workout" and
  "Done with this exercise" (collapsing replaces them).
- `web/src/screens/Workout.svelte`: each exercise is a card with a header (badge, name,
  "N sets", chevron) that toggles the inline set table. One open at a time
  (`let openId = $state<string | null>(null)`). Picking an exercise in `ExercisePicker`
  or tapping a template "up next" item opens it in place instead of `go('workout/' + id)`.
- `web/src/App.svelte`: drop the `route.parts[0] === 'workout' && route.parts[1]` →
  `ExerciseCard` branch; an old `#/workout/<id>` link just shows the Workout screen.

### 3. Active-workout bar (`feat(web): active workout bar with resume and end`)

- New `web/src/components/ActiveWorkoutBar.svelte`, fixed above `NavBar`, shown when
  `workout.session` is set and the current tab is not Workout:
  "● Workout in progress · started 08:57 · <gym>", buttons **Resume** (`go('workout')`) and **End**.
- **End** → `choose(...)`: **Finish & save** (`finishWorkout()`) or **Discard workout**
  (danger; second `confirmAction` naming the set count, then `api.deleteSession(id)` + reset).
  Add a `discardWorkout()` next to `finishWorkout()` in `web/src/lib/workout.svelte.ts`
  (reuse its private `reset()`).
- Workout screen: add a small "Discard workout" action next to "Finish workout".
- `App.svelte`: replace the startup "A workout is still running" `choose` popup with
  `loadOpen()` so the shared store is filled and the bar shows instead.
- Make sure `<main>` bottom padding (`pb-24`) clears nav + bar.

### 4. Docs (`docs: document the UI pass`)

- `docs/DESIGN.md` line ~139 ("Reloading during a workout asks whether to resume or finish it")
  → describe the bar; describe inline exercise cards and the theme.
- `docs/DECISIONS.md`: one entry per decision (theme, inline cards, active-workout bar).
- `docs/DOCUMENTATION.md`: file tree (~line 188, `ExerciseCard` now in `components/`) and ~line 220.
- **`docs/DEFENSE.md` line 31 cites `web/src/screens/ExerciseCard.svelte:57`** (the tick →
  `api.addSet`). Update path and line number; the developer studies this doc for the defense.

## Live preview (Playwright MCP)

- Backend: stop any running jar (`pkill -f 'myophoria.*\.jar'`; one was started this session
  on the real `data/`, log in `/tmp/myophoria.log`). Restart on a copy:
  `cp -r data /tmp/myophoria-ui && MYOPHORIA_DATA_DIR=/tmp/myophoria-ui java -jar target/myophoria-*.jar`.
- Front end: `cd web && npm run dev` (Vite, proxies `/api` to :7070).
- Playwright: open the Vite URL at phone size (~390×844); screenshot after each step.

## Checks before each commit (PLAN.md §0.5)

```bash
(cd web && npm run check && npm run build)
```

No front-end tests exist; verify each step by clicking through in Playwright:
start workout → add exercise (opens inline) → tick sets → switch to History (bar shows) →
Resume → End → Discard (session gone) and End → Finish (summary shows).

## Out of scope

v2 features (PRs, 1RM, plates, rest timer, settings, …) come after this pass.
No backend changes are needed.
