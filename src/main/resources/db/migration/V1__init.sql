CREATE TABLE gyms (
  id          TEXT PRIMARY KEY,
  name        TEXT NOT NULL,
  name_key    TEXT NOT NULL,
  is_default  INTEGER NOT NULL DEFAULT 0 CHECK (is_default IN (0, 1)),
  deleted_at  TEXT
) STRICT;
CREATE UNIQUE INDEX gyms_one_default  ON gyms (is_default) WHERE is_default = 1;
CREATE UNIQUE INDEX gyms_active_name  ON gyms (name_key)   WHERE deleted_at IS NULL;

CREATE TABLE exercises (
  id              TEXT PRIMARY KEY,
  name            TEXT NOT NULL,
  name_key        TEXT NOT NULL,
  equipment_type  TEXT NOT NULL CHECK (equipment_type IN ('BARBELL','DUMBBELL','STACK','PLATE_LOADED','CALISTHENICS')),
  laterality      TEXT NOT NULL DEFAULT 'BILATERAL' CHECK (laterality IN ('BILATERAL','UNILATERAL')),
  deleted_at      TEXT
) STRICT;
CREATE UNIQUE INDEX exercises_active_name ON exercises (name_key) WHERE deleted_at IS NULL;

CREATE TABLE workout_sessions (
  id          TEXT PRIMARY KEY,
  gym_id      TEXT NOT NULL REFERENCES gyms (id),
  local_date  TEXT NOT NULL,
  started_at  TEXT,
  ended_at    TEXT,
  is_open     INTEGER NOT NULL DEFAULT 0 CHECK (is_open IN (0, 1))
) STRICT;
CREATE UNIQUE INDEX sessions_one_open ON workout_sessions (is_open) WHERE is_open = 1;
CREATE INDEX sessions_gym_date ON workout_sessions (gym_id, local_date);

CREATE TABLE workout_sets (
  id            TEXT PRIMARY KEY,
  session_id    TEXT NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
  exercise_id   TEXT NOT NULL REFERENCES exercises (id),
  position      INTEGER NOT NULL CHECK (position >= 1),
  set_type      TEXT NOT NULL CHECK (set_type IN ('WARMUP','WORKING','FAILURE')),
  weight        REAL NOT NULL CHECK (weight >= 0),
  unit          TEXT NOT NULL CHECK (unit IN ('KG','LB')),
  reps          INTEGER NOT NULL CHECK (reps > 0),
  bodyweight    REAL CHECK (bodyweight IS NULL OR bodyweight > 0),
  completed_at  TEXT NOT NULL
) STRICT;
CREATE INDEX sets_session  ON workout_sets (session_id);
CREATE INDEX sets_exercise ON workout_sets (exercise_id);

CREATE TABLE bodyweight_entries (
  id           TEXT PRIMARY KEY,
  weight       REAL NOT NULL CHECK (weight > 0),
  unit         TEXT NOT NULL CHECK (unit IN ('KG','LB')),
  recorded_at  TEXT NOT NULL,
  local_date   TEXT NOT NULL
) STRICT;

CREATE TABLE workout_templates (
  id    TEXT PRIMARY KEY,
  name  TEXT NOT NULL
) STRICT;
CREATE TABLE template_exercises (
  template_id  TEXT NOT NULL REFERENCES workout_templates (id) ON DELETE CASCADE,
  exercise_id  TEXT NOT NULL REFERENCES exercises (id),
  position     INTEGER NOT NULL,
  PRIMARY KEY (template_id, position)
) STRICT;

-- Fixed ID so every database and every export share the same seeded gym.
INSERT INTO gyms (id, name, name_key, is_default) VALUES ('00000000-0000-0000-0000-000000000001', 'Default', 'default', 1);
