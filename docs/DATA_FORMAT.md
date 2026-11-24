# Data format

`GET /api/v1/export` downloads every record as a ZIP of CSV files, and
`POST /api/v1/import` reads the same ZIP back. Use it to move data between
machines, or to edit history in a spreadsheet.

## Encoding

- UTF-8. Every file has a header row first.
- RFC 4180 quoting: a field containing a comma, a quote or a line break is
  wrapped in `"`, and an inner `"` is doubled.
- Written with LF line endings. On read, CRLF and a leading BOM are also
  accepted, and blank lines are skipped.
- An empty field means null. Booleans are `true`/`false`. Enums are written by
  name (`BARBELL`, `KG`, ...). The decimal point is `.`.
- Instants are UTC with milliseconds, fixed width:
  `2026-11-25T18:04:00.000Z` (`CsvCodec.TIMESTAMP`). Dates are `2026-11-25`.
- Ids are UUID strings, the same ones the database uses.

## Files

| File | Columns |
|---|---|
| `manifest.csv` | `format_version,exported_at`: exactly one row, version `1` |
| `gyms.csv` | `id,name,is_default,deleted_at` |
| `exercises.csv` | `id,name,equipment_type,laterality,deleted_at` |
| `sessions.csv` | `id,gym_id,local_date,started_at,ended_at,is_open` |
| `sets.csv` | `id,session_id,exercise_id,position,set_type,weight,unit,reps,bodyweight,completed_at` |
| `bodyweight.csv` | `id,weight,unit,recorded_at,local_date` |
| `templates.csv` | `id,name,exercise_ids` (ids joined by `;`, in template order) |

Example `sets.csv`:

```
id,session_id,exercise_id,position,set_type,weight,unit,reps,bodyweight,completed_at
5b0c…,9e1f…,a3d2…,1,WORKING,100,KG,5,,2026-11-25T18:04:00.000Z
```

## Import rules

1. **Size:** the unpacked ZIP is capped at 64 MB.
2. **Manifest:** missing, malformed or unsupported → the whole import fails
   with 422 `CORRUPTED_CSV`, and nothing is written.
3. **Backup:** a backup of the database is taken before the first write.
4. **Order:** gyms → exercises → sessions → sets → bodyweight → templates, so
   parents exist before their children. Each row is imported on its own (no
   all-or-nothing transaction).
5. **Rows are never overwritten.** For every row, the outcome is one of:

| Outcome | When |
|---|---|
| `imported` | new id, valid row |
| `alreadyPresent` | the id already exists (counted, left untouched) |
| `skipped` | the row is broken: wrong column count, bad number, date or enum, or failed validation. Reported as `file:line reason` |
| `orphans` | it points to a gym, session or exercise that is neither in the database nor imported |
| `nameConflicts` | a different id with an active name that is already taken |
| `adjusted` | imported, but `is_default` or `is_open` was cleared because there is already a default gym or an open workout |

A CSV file that is missing or cannot be read as a whole (for example a wrong
header) is listed under `skipped`, and none of its rows are imported. The
response is an `ImportReport` with one entry per file ([API.md](API.md)).
