# API

REST API under `/api/v1`, served by Javalin on 127.0.0.1 (port
`MYOPHORIA_PORT`, default 7070). Bodies are JSON with camelCase field names.
Dates are ISO `yyyy-MM-dd`; instants are ISO UTC (`2026-11-25T18:04:00Z`).
`localDate` always comes from the client, because the day of a workout is the
phone's local day. Optional fields may be left out or sent as `null`.

## Access

Every `/api/*` request must have a `Host` of `localhost`, `127.0.0.1` or a host
listed in `MYOPHORIA_ALLOWED_HOSTS`. If it sends an `Origin`, that must match
the same list. Otherwise the response is 403 `FORBIDDEN`. There is no login.

## Errors

```json
{"error": "DUPLICATE_RESOURCE", "message": "…",
 "details": {"resourceType": "exercise", "existingId": "…", "softDeleted": true}}
```

| Code | HTTP | When |
|---|---|---|
| `INVALID_INPUT` | 400 | validation failed, malformed JSON, bad date, missing parameter |
| `FORBIDDEN` | 403 | Host/Origin not allowed |
| `NOT_FOUND` | 404 | unknown id |
| `DUPLICATE_RESOURCE` | 409 | active name already taken; `details` only here |
| `CORRUPTED_CSV` | 422 | import bundle unusable (missing/bad manifest, unsupported version, too large) |
| `INTERNAL` | 500 | anything else; logged, message hidden |

## Shapes

| Name | Fields |
|---|---|
| `Exercise` | `id, name, equipmentType, laterality, initials, weightLabel, deleted` |
| `Gym` | `id, name, isDefault` |
| `Set` | `id, sessionId, exerciseId, exerciseName, position, setType, weight, unit, reps, bodyweight?, completedAt, display` |
| `Session` | `id, gymId, localDate, startedAt?, endedAt?, open, sets: Set[]` |
| `SessionHeader` | `id, gymName, localDate, startedAt?, endedAt?, setCount, open` |
| `SessionSummary` | `sessionId, localDate, startedAt?, endedAt?, durationSeconds?, unit, totalSets, workingSets, volume, exercises: {exerciseId, name, workingSets, topWeight?, averageWeight?, volume}[]` |
| `Prefill` | `unit, rows: {setType, weight, reps}[], warmupWeight?, bodyweight?` |
| `Entry` (bodyweight) | `id, weight, unit, recordedAt, localDate` |
| `Template` | `id, name, exercises: Exercise[]` (soft-deleted exercises hidden) |
| `ImportReport` | `files: {"<name>.csv": {imported, alreadyPresent, skipped[], orphans[], nameConflicts[], adjusted[]}}` |

Enums: `equipmentType` `BARBELL | DUMBBELL | STACK | PLATE_LOADED | CALISTHENICS`;
`laterality` `BILATERAL | UNILATERAL`; `setType` `WARMUP | WORKING | FAILURE`;
`unit` `KG | LB` in bodies, and `kg | lb` in the `?unit=` query parameter (default `kg`).

## Endpoints

### Status, backups, export/import

| Method + path | Request | Response |
|---|---|---|
| `GET /status` | – | `{warnings: string[]}` (startup recovery messages) |
| `POST /backups` | – | `{file}` |
| `GET /export` | – | `application/zip`, download `myophoria-<time>.zip` |
| `POST /import` | multipart field `file` (the ZIP) | `ImportReport` |

### Exercises

| Method + path | Request | Response |
|---|---|---|
| `GET /exercises` | `?q=` name contains, `?type=BARBELL,DUMBBELL`, `?deleted=true` for the bin | `Exercise[]` sorted by name |
| `GET /exercises/{id}` | – | `Exercise` |
| `POST /exercises` | `{name, equipmentType, laterality}` | 201 `Exercise`; 409 if the name is taken (`softDeleted: true` → offer restore) |
| `PUT /exercises/{id}` | same | `Exercise` |
| `DELETE /exercises/{id}` | – | 204 (soft delete) |
| `POST /exercises/{id}/restore` | – | `Exercise`; 409 if an active exercise has the name |
| `GET /exercises/{id}/usage` | – | `{sets, sessions}` |
| `DELETE /exercises/{id}/permanent` | – | `{backupFile}`; only for soft-deleted exercises |
| `GET /exercises/{id}/progression` | `?unit=` | `{localDate, topWeight}[]` (top working-set weight per day) |

### Gyms

| Method + path | Request | Response |
|---|---|---|
| `GET /gyms` | – | `Gym[]` (active only) |
| `GET /gyms/active` | – | `Gym`: the open workout's gym, else the default |
| `POST /gyms` | `{name}` | 201 `Gym` |
| `PUT /gyms/{id}` | `{name}` | `Gym` |
| `POST /gyms/{id}/default` | – | `Gym` |
| `DELETE /gyms/{id}` | – | 204 (soft delete) |

### Workout (the running session)

| Method + path | Request | Response |
|---|---|---|
| `GET /sessions/open` | – | `Session`, or 204 if none is open |
| `POST /sessions/start` | `{gymId, localDate}` | 201 `Session`; 400 if one is already open |
| `POST /sessions/{id}/finish` | – | `Session`; 400 if it has no sets (delete it instead) |
| `GET /sessions/{id}/prefill` | `?exerciseId=` | `Prefill` from the last session at the same gym |
| `POST /sessions/{id}/sets` | `{exerciseId, setType, weight, unit, reps, bodyweight?, position}` | 201 `Set` |
| `PUT /sets/{id}` | same | `Set` |
| `DELETE /sets/{id}` | – | 204; a closed session left empty is deleted |
| `GET /sessions/{id}/summary` | `?unit=` | `SessionSummary` |
| `POST /sessions/{id}/template` | `{name}` | 201 `Template` from the session's exercises |

### History

| Method + path | Request | Response |
|---|---|---|
| `GET /sessions` | `?from=&to=` (dates), `?gymId=`, `?exerciseId=` | `SessionHeader[]`, newest first |
| `GET /sessions/{id}` | – | `Session` |
| `POST /sessions` | `{gymId, localDate, startedAt?, endedAt?}` | 201 `Session` (backfilled, closed) |
| `PUT /sessions/{id}` | same | `Session` |
| `DELETE /sessions/{id}` | – | 204 (with its sets) |

### Bodyweight

| Method + path | Request | Response |
|---|---|---|
| `GET /bodyweight` | – | `Entry[]` |
| `GET /bodyweight/latest` | – | `Entry`, or 204 if none |
| `POST /bodyweight` | `{weight, unit, recordedAt, localDate}` | 201 `Entry` |
| `PUT /bodyweight/{id}` | same | `Entry` |
| `DELETE /bodyweight/{id}` | – | 204 |

### Templates

| Method + path | Request | Response |
|---|---|---|
| `GET /templates` | – | `Template[]` |
| `POST /templates` | `{name, exerciseIds: string[]}` (ordered) | 201 `Template` |
| `PUT /templates/{id}` | same | `Template` |
| `DELETE /templates/{id}` | – | 204 |
