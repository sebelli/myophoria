# Demo script (10 minutes)

Each step names the rubric row it proves (PLAN §3.5). Rehearse it once the day
before. Every step was tried on 2026-11-25 against the real jar.

## Before the demo (5 minutes, not timed)

```bash
cd web && npm install && npm run build && cd ..
./mvnw -q package
export MYOPHORIA_DATA_DIR=/tmp/myophoria-demo      # fresh database, keeps real data out
rm -rf /tmp/myophoria-demo
jar --create --no-manifest --file /tmp/demo.zip   -C docs/demo/data .
jar --create --no-manifest --file /tmp/broken.zip -C docs/demo/broken .
java -jar target/myophoria-*.jar
```

Open http://127.0.0.1:7070 in Chrome with DevTools device mode at 390×844, or
on the phone over Tailscale (README). Keep a terminal and the IDE open beside it.

## 1. What it is (0:00–1:00) · Documentation/UML

- One sentence: "A gym lift tracker. Java core, REST API, Svelte UI, SQLite,
  running on my laptop and used from my phone."
- Show the layer diagram in `docs/DESIGN.md` §1. Say: dependencies point
  inward, and the core has no framework imports.
- Open `docs/uml/classes.md` section 1: it is generated from the compiled
  classes, so it matches the code.

## 2. Import with a broken row (1:00–2:30) · File handling, Exceptions

1. More → Data → Import → `/tmp/broken.zip`.
2. Read the report aloud:
   - `sets.csv:5 reps is not a whole number: five` was **skipped**: the
     checked `CorruptedCsvException` is caught per row (`CsvService.java:134`
     and the other per-file loops).
   - `sets.csv:10 session … is missing` is an **orphan**: the importer tracks
     imported ids in `HashSet`s (`CsvService.java:105`).
   - Everything else was imported.
3. Import `/tmp/demo.zip` (the fixed bundle): only **2 new** sets, and all the
   other rows are "Already there". Import never overwrites (decision G10).
4. Mention: a backup was taken before the first write, and a bad manifest
   would reject the whole file with 422 before anything is written.

## 3. Exercises (2:30–4:00) · Inheritance/polymorphism, Collections, Class design

1. Exercises tab: initials badges, and weight labels that differ per type
   (total / per dumbbell / stack / plates / added). These come from
   `Exercise.weightLabel()`, overridden in each of the 5 subclasses and called
   through an `Exercise` reference.
2. Type "pr" in the search box, then filter by type. The service filters
   in Java (`ExerciseService.search`, `ExerciseService.java:41`), not in SQL.
3. Add "  bench   PRESS " → 409 duplicate. The normalised name key matches
   "Bench Press" (`Exercise.nameKey`, `Exercise.java:45`).
4. Add "Cable Curl" as type Stack. A deleted exercise already has that name, so the
   app offers "Restore it" (offered only for the same type) or "Erase the old one". Restore it.
   Show deleted / Show active toggles the bin.

## 4. A workout (4:00–6:30) · GUI, Functionality, Exceptions

1. Workout → Start workout (gym "Default", empty workout) → + Add exercise →
   Bench Press.
2. The card is **prefilled** from the last session at this gym (72.5 kg × 5,
   with a warm-up row). Tick two sets; each tick is one `POST /sets` request.
3. Change a weight to `-5` and tick → the error shows on the row. The server
   rejected it: `Exercise.validateWeight` (`Exercise.java:57`) threw
   `InvalidWorkoutInputException`, and `ErrorHandlers` turned it into a 400. The
   `WorkoutSet` setter also rejects negative weights (`WorkoutSet.java:120`), so an entity can never hold one.
4. Add Pull-Up: a **bodyweight** field appears and is required (the
   `CalisthenicsExercise.requiresBodyweight` override). Tick one set at weight 0,
   which only calisthenics allows (`validateWeight` override).
5. Add One-Arm Dumbbell Row and tick a set: it shows "… kg each"
   (`DumbbellExercise.formatSet`).
6. Navigate to History: the workout bar shows the running clock. Tap Resume:
   the unticked rows are still there.
7. Finish workout → **Summary**: sets, working sets and volume, per-exercise
   top and average. `VolumeCalculable` is implemented by `WorkoutSet` and
   `WorkoutSession`. Switch kg → lb. Save as template "Push".

## 5. History and progress (6:30–7:30) · Functionality, Collections

1. History: newest first; filter by gym "City Gym", then by exercise.
   Open a session and show its sets.
2. Exercises → Bench Press → Progress chart: 60 → 72.5 kg over six weeks
   (`ReportService` groups sets with a `LinkedHashMap`).
3. More → Bodyweight: six entries; mention the bodyweight snapshot on Pull-Up sets.

## 6. Database corruption recovery (7:30–9:00) · File handling, Exceptions

1. More → Data → **Back up now** (a `VACUUM INTO` copy; the temp file is
   always removed in a `finally` block, `SqliteBackups.java:51`).
2. In the terminal, stop the server (Ctrl+C) and damage the file:
   ```bash
   printf 'this is not a database, just garbage bytes!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!' \
     | dd of=/tmp/myophoria-demo/myophoria.db conv=notrunc status=none
   java -jar target/myophoria-*.jar
   ```
3. Reload the page: a banner says the file was damaged, was kept as
   `myophoria.db.corrupt-<time>`, and the backup was restored. History is intact.
4. In code: `Database.java:92` `isHealthy` (`PRAGMA quick_check`, SQLite codes
   11/26 only), then `recoverIfCorrupt` at line 60, which tries backups newest first.

## 7. Code and tests (9:00–10:00) · Class design, Documentation

- `./mvnw verify` (116 tests: core with in-memory fakes, SQLite adapters on
  temp files, the HTTP API end to end). Start it at the beginning of step 6 so
  it has finished by now.
- Show `Exercise.java` (abstract, validated setters, plain getters) and one
  subclass. Then show `ErrorHandlers.java` (exception → status code).
- Close: the "if I had more time" list is the v2 scope in `handoff.md` §9 (rest
  timer, 1RM and plate calculator, more charts).

## If something goes wrong

- Port busy: `MYOPHORIA_PORT=7071 java -jar …`
- The page is 404 and the log says "No web build on the classpath": run
  `npm run build` in `web/`, then `./mvnw -q package` again.
- Start over: stop the server, `rm -rf /tmp/myophoria-demo`, start it again,
  and import `/tmp/demo.zip`.
