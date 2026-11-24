# UML class diagrams

Status: **design draft from PLAN.md §4 (Gate C, 2026-11-25).** Task T7.1 regenerates these diagrams from the finished code; the final version must match the code exactly.

Notation: `-` private, `#` protected, `+` public, `$` static, `*` abstract. `?` in a note marks a nullable value.

## 1. Domain model (`myophoria.core.model`, `myophoria.core.csv`, `myophoria.core.error`)

```mermaid
classDiagram
  direction TB

  class CsvSerializable {
    <<interface>>
    +toCsvRow() String[]
  }
  class VolumeCalculable {
    <<interface>>
    +volume(Unit reportingUnit) double
  }

  class Exercise {
    <<abstract>>
    -String id
    -String name
    -String nameKey
    -Laterality laterality
    -Instant deletedAt
    #Exercise(String id, String name, Laterality laterality, Instant deletedAt)
    +of(EquipmentType type, String id, String name, Laterality laterality, Instant deletedAt) Exercise$
    +nameKey(String name) String$
    +fromCsvRow(String[] row) Exercise$
    +type() EquipmentType*
    +weightLabel() String*
    +validateWeight(double weight) void
    +requiresBodyweight() boolean
    +formatSet(double weight, Unit unit, Double bodyweight) String
    +warmupWeight(double lastTopWeight) double
    +initials() String
    +setName(String name) void
    +setLaterality(Laterality laterality) void
    +softDelete(Instant at) void
    +restore() void
    +isDeleted() boolean
    +toCsvRow() String[]
  }
  class BarbellExercise {
    +type() EquipmentType
    +weightLabel() String
  }
  class DumbbellExercise {
    +type() EquipmentType
    +weightLabel() String
    +formatSet(double weight, Unit unit, Double bodyweight) String
  }
  class StackExercise {
    +type() EquipmentType
    +weightLabel() String
  }
  class PlateLoadedExercise {
    +type() EquipmentType
    +weightLabel() String
  }
  class CalisthenicsExercise {
    +type() EquipmentType
    +weightLabel() String
    +validateWeight(double weight) void
    +requiresBodyweight() boolean
    +formatSet(double weight, Unit unit, Double bodyweight) String
  }

  class Gym {
    -String id
    -String name
    -String nameKey
    -boolean isDefault
    -Instant deletedAt
    +setName(String name) void
    +setDefault(boolean isDefault) void
    +softDelete(Instant at) void
    +isDeleted() boolean
    +fromCsvRow(String[] row) Gym$
    +toCsvRow() String[]
  }
  class BodyweightEntry {
    -String id
    -double weight
    -Unit unit
    -Instant recordedAt
    -LocalDate localDate
    +setWeight(double weight) void
    +setUnit(Unit unit) void
    +fromCsvRow(String[] row) BodyweightEntry$
    +toCsvRow() String[]
  }
  class WorkoutSet {
    -String id
    -String sessionId
    -String exerciseId
    -int position
    -SetType setType
    -double weight
    -Unit unit
    -int reps
    -Double bodyweight
    -Instant completedAt
    +volume(Unit reportingUnit) double
    +fromCsvRow(String[] row) WorkoutSet$
    +toCsvRow() String[]
  }
  class WorkoutSession {
    -String id
    -String gymId
    -LocalDate localDate
    -Instant startedAt
    -Instant endedAt
    -boolean open
    -List~WorkoutSet~ sets
    +volume(Unit reportingUnit) double
    +finish(Instant now) void
    +effectiveEnd() Instant
    +duration() Duration
    +fromCsvRow(String[] row) WorkoutSession$
    +toCsvRow() String[]
  }
  class WorkoutTemplate {
    -String id
    -String name
    -List~String~ exerciseIds
    +setName(String name) void
    +setExerciseIds(List~String~ ids) void
    +fromCsvRow(String[] row) WorkoutTemplate$
    +toCsvRow() String[]
  }

  class EquipmentType {
    <<enumeration>>
    BARBELL
    DUMBBELL
    STACK
    PLATE_LOADED
    CALISTHENICS
  }
  class Laterality {
    <<enumeration>>
    BILATERAL
    UNILATERAL
  }
  class SetType {
    <<enumeration>>
    WARMUP
    WORKING
    FAILURE
    +isWorking() boolean
  }
  class Unit {
    <<enumeration>>
    KG
    LB
    -double kgPerUnit
    +convert(double value, Unit to) double
  }

  class CsvCodec {
    <<utility>>
    +TIMESTAMP DateTimeFormatter$
    +formatRow(String[] fields) String$
    +parse(Reader in, String fileName) List~String[]~$
  }

  Exercise <|-- BarbellExercise
  Exercise <|-- DumbbellExercise
  Exercise <|-- StackExercise
  Exercise <|-- PlateLoadedExercise
  Exercise <|-- CalisthenicsExercise

  CsvSerializable <|.. Exercise
  CsvSerializable <|.. Gym
  CsvSerializable <|.. BodyweightEntry
  CsvSerializable <|.. WorkoutSet
  CsvSerializable <|.. WorkoutSession
  CsvSerializable <|.. WorkoutTemplate
  VolumeCalculable <|.. WorkoutSet
  VolumeCalculable <|.. WorkoutSession

  WorkoutSession "1" *-- "0..*" WorkoutSet : sets
  WorkoutSet ..> Exercise : exerciseId
  WorkoutSession ..> Gym : gymId
  WorkoutTemplate ..> Exercise : exerciseIds

  Exercise --> Laterality
  Exercise ..> EquipmentType
  WorkoutSet --> SetType
  WorkoutSet --> Unit
  BodyweightEntry --> Unit
  CsvSerializable ..> CsvCodec : rows formatted by

  note for Exercise "of() is the only EquipmentType-to-subclass switch.\nSetters throw InvalidWorkoutInputException."
  note for WorkoutSet "WARMUP sets have volume 0.\nbodyweight is nullable, in the set's unit."
```

### Exceptions

```mermaid
classDiagram
  class RuntimeException
  class Exception
  class InvalidWorkoutInputException {
    +InvalidWorkoutInputException(String message)
  }
  class NotFoundException {
    -String resourceType
    -String id
  }
  class DuplicateResourceException {
    -String resourceType
    -String existingId
    -boolean softDeleted
  }
  class CorruptedCsvException {
    -String file
    -int line
    -String reason
  }
  RuntimeException <|-- InvalidWorkoutInputException
  RuntimeException <|-- NotFoundException
  RuntimeException <|-- DuplicateResourceException
  Exception <|-- CorruptedCsvException

  note for CorruptedCsvException "Checked: CsvService catches it per row and reports it.\nThe others are unchecked and reach ErrorHandlers (400, 404, 409)."
```

## 2. Ports and services (`myophoria.core.port`, `myophoria.core.service`)

```mermaid
classDiagram
  direction LR

  class ExerciseRepository {
    <<interface>>
    +findAll() List~Exercise~
    +findById(String id) Exercise
    +insert(Exercise e) void
    +update(Exercise e) void
    +countSets(String exerciseId) int
    +countSessions(String exerciseId) int
    +deletePermanently(String id) void
  }
  class GymRepository {
    <<interface>>
    +findAll() List~Gym~
    +findById(String id) Gym
    +insert(Gym g) void
    +update(Gym g) void
    +makeDefault(String id) void
  }
  class SessionRepository {
    <<interface>>
    +findAll() List~WorkoutSession~
    +findById(String id) WorkoutSession
    +findOpen() WorkoutSession
    +insert(WorkoutSession s) void
    +update(WorkoutSession s) void
    +delete(String id) void
  }
  class SetRepository {
    <<interface>>
    +findById(String id) WorkoutSet
    +insert(WorkoutSet s) void
    +update(WorkoutSet s) void
    +delete(String id) void
  }
  class BodyweightRepository {
    <<interface>>
    +findAll() List~BodyweightEntry~
    +findById(String id) BodyweightEntry
    +insert(BodyweightEntry b) void
    +update(BodyweightEntry b) void
    +delete(String id) void
  }
  class TemplateRepository {
    <<interface>>
    +findAll() List~WorkoutTemplate~
    +findById(String id) WorkoutTemplate
    +insert(WorkoutTemplate t) void
    +update(WorkoutTemplate t) void
    +delete(String id) void
  }
  class Backups {
    <<interface>>
    +createBackup() String
  }

  class ExerciseService {
    -ExerciseRepository exercises
    -Backups backups
    -Clock clock
    +search(String query, Set~EquipmentType~ types, boolean deleted) List~Exercise~
    +get(String id) Exercise
    +create(String name, EquipmentType type, Laterality laterality) Exercise
    +update(String id, String name, EquipmentType type, Laterality laterality) Exercise
    +softDelete(String id) void
    +restore(String id) Exercise
    +usage(String id) Usage
    +deletePermanently(String id) String
  }
  class GymService {
    -GymRepository gyms
    -SessionRepository sessions
    -Clock clock
    +list() List~Gym~
    +create(String name) Gym
    +rename(String id, String name) Gym
    +makeDefault(String id) Gym
    +delete(String id) void
    +activeGym() Gym
  }
  class WorkoutService {
    -SessionRepository sessions
    -SetRepository sets
    -ExerciseRepository exercises
    -GymRepository gyms
    -BodyweightRepository bodyweights
    -Clock clock
    +open() WorkoutSession
    +start(String gymId, LocalDate localDate) WorkoutSession
    +finish(String id) WorkoutSession
    +addSet(String sessionId, SetInput input) WorkoutSet
    +updateSet(String id, SetInput input) WorkoutSet
    +deleteSet(String id) void
    +prefill(String sessionId, String exerciseId) Prefill
  }
  class HistoryService {
    -SessionRepository sessions
    -GymRepository gyms
    +list(HistoryFilter filter) List~WorkoutSession~
    +get(String id) WorkoutSession
    +backfill(String gymId, LocalDate localDate, Instant startedAt, Instant endedAt) WorkoutSession
    +edit(String id, String gymId, LocalDate localDate, Instant startedAt, Instant endedAt) WorkoutSession
    +delete(String id) void
  }
  class ReportService {
    -SessionRepository sessions
    -ExerciseRepository exercises
    +summary(String sessionId, Unit unit) SessionSummary
    +progression(String exerciseId, Unit unit) List~ProgressPoint~
  }
  class BodyweightService {
    -BodyweightRepository entries
    +list() List~BodyweightEntry~
    +latest() BodyweightEntry
    +create(double weight, Unit unit, Instant recordedAt, LocalDate localDate) BodyweightEntry
    +update(String id, double weight, Unit unit, Instant recordedAt, LocalDate localDate) BodyweightEntry
    +delete(String id) void
  }
  class TemplateService {
    -TemplateRepository templates
    -ExerciseRepository exercises
    -SessionRepository sessions
    +list() List~WorkoutTemplate~
    +create(String name, List~String~ exerciseIds) WorkoutTemplate
    +update(String id, String name, List~String~ exerciseIds) WorkoutTemplate
    +delete(String id) void
    +saveFromSession(String sessionId, String name) WorkoutTemplate
  }
  class CsvService {
    -ExerciseRepository exercises
    -GymRepository gyms
    -SessionRepository sessions
    -SetRepository sets
    -BodyweightRepository bodyweights
    -TemplateRepository templates
    -Backups backups
    -Clock clock
    +export(OutputStream out) void
    +importZip(InputStream in) ImportReport
  }

  ExerciseService --> ExerciseRepository
  ExerciseService --> Backups
  GymService --> GymRepository
  GymService --> SessionRepository
  WorkoutService --> SessionRepository
  WorkoutService --> SetRepository
  WorkoutService --> ExerciseRepository
  WorkoutService --> GymRepository
  WorkoutService --> BodyweightRepository
  HistoryService --> SessionRepository
  HistoryService --> GymRepository
  ReportService --> SessionRepository
  ReportService --> ExerciseRepository
  BodyweightService --> BodyweightRepository
  TemplateService --> TemplateRepository
  TemplateService --> ExerciseRepository
  TemplateService --> SessionRepository
  CsvService --> Backups

  note for ExerciseService "HashMap~String, Exercise~ keyed by nameKey for duplicate checks.\nEnumSet filter; ArrayList sorted by name."
  note for ReportService "LinkedHashMap~String, List~WorkoutSet~~ groups sets per exercise in first-set order."
  note for CsvService "HashSet~String~ of imported IDs per file to detect orphans.\nCatches CorruptedCsvException per row; finally deletes the temp file."
  note for WorkoutService "ArrayList filtering over sessions for prefill, unit memory and warm-up."
```

`CsvService` also uses every other repository; those arrows are left out for readability.

### Service result records

| Record | Fields |
|---|---|
| `SetInput` | `exerciseId, setType, weight, unit, reps, bodyweight?, position` |
| `Prefill` | `unit, List<PrefillRow> rows, warmupWeight?, bodyweight?` |
| `PrefillRow` | `setType, weight, reps` |
| `HistoryFilter` | `from?, to?, gymId?, exerciseId?` |
| `SessionSummary` | `totalSets, workingSets, totalVolume, List<ExerciseSummary> exercises, startedAt?, endedAt?, duration?` |
| `ExerciseSummary` | `exerciseId, name, workingSets, topWeight, averageWeight, volume` |
| `ProgressPoint` | `localDate, topWeight` |
| `Usage` | `sets, sessions` |
| `ImportReport` | `Map<String, FileReport> files` |
| `FileReport` | `imported, alreadyPresent, List<String> skipped, List<String> orphans, List<String> nameConflicts` |

## 3. Adapters (`myophoria.persistence`, `myophoria.api`)

```mermaid
classDiagram
  direction LR

  class Database {
    -Path dataDir
    +open() List~String~
    +connect() Connection
  }
  class Migrator {
    +migrate(Connection connection) void$
  }
  class SqliteExerciseRepository
  class SqliteGymRepository
  class SqliteSessionRepository
  class SqliteSetRepository
  class SqliteBodyweightRepository
  class SqliteTemplateRepository
  class SqliteBackups {
    -Database database
    -Path backupDir
    +createBackup() String
  }

  ExerciseRepository <|.. SqliteExerciseRepository
  GymRepository <|.. SqliteGymRepository
  SessionRepository <|.. SqliteSessionRepository
  SetRepository <|.. SqliteSetRepository
  BodyweightRepository <|.. SqliteBodyweightRepository
  TemplateRepository <|.. SqliteTemplateRepository
  Backups <|.. SqliteBackups
  Database ..> Migrator
  SqliteExerciseRepository --> Database
  SqliteBackups --> Database

  class Main {
    +main(String[] args) void$
  }
  class ErrorHandlers
  class ExerciseController
  class GymController
  class WorkoutController
  class HistoryController
  class ReportController
  class BodyweightController
  class TemplateController
  class DataController

  Main ..> Database : opens, migrates, recovers
  Main ..> ErrorHandlers
  ExerciseController --> ExerciseService
  GymController --> GymService
  WorkoutController --> WorkoutService
  HistoryController --> HistoryService
  ReportController --> ReportService
  BodyweightController --> BodyweightService
  TemplateController --> TemplateService
  DataController --> CsvService
  DataController --> Backups

  note for Main "Reads MYOPHORIA_PORT, MYOPHORIA_DATA_DIR, MYOPHORIA_ALLOWED_HOSTS.\nWires adapters into services into controllers; Host/Origin guard."
```

Every SQLite repository holds a `Database` and opens one connection per call; only two of those arrows are drawn. Controllers declare their request and response records as nested types.
