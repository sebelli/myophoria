# UML class diagrams

Generated from the compiled code on 2026-11-25 (T7.1) with `javap -p`, so every class, field and non-private method below is in the code. Regenerate after any change to a class's members.

Notation: `-` private, `#` protected, `+` public, `~` package-private, `$` static, `*` abstract. Parameters show types only. Left out: private helper methods, lambdas, the generated members of records and enums (`equals`, `hashCode`, `toString`, accessors, `values`, `valueOf`), and nested types (the controllers' request/response records and `CsvService`'s private `Row`/`Tally` helpers). A solid arrow `-->` is a field; a dotted arrow `..>` is a reference by id or a use without a field.

## 1. Domain model (`myophoria.core.model`, `myophoria.core.csv`)

```mermaid
classDiagram
  direction TB

  class CsvCodec {
    +DateTimeFormatter TIMESTAMP$
    +formatRow(String[]) String$
    +parse(Reader, String) List~String[]~$
    +requireColumns(String[], int) void$
    +optional(String) String$
    +number(String, String) double$
    +optionalNumber(String, String) Double$
    +integer(String, String) int$
    +bool(String, String) boolean$
    +enumValue(Class~E~, String, String) E$
    +instant(String, String) Instant$
    +date(String, String) LocalDate$
    +format(double) String$
    +format(Instant) String$
    +bad(String) CorruptedCsvException$
  }
  class CsvSerializable {
    <<interface>>
    +toCsvRow() String[]
  }
  class BarbellExercise {
    +BarbellExercise(String, String, Laterality, Instant)
    +type() EquipmentType
    +weightLabel() String
  }
  class BodyweightEntry {
    -String id
    -double weight
    -Unit unit
    -Instant recordedAt
    -LocalDate localDate
    +String[] HEADER$
    +BodyweightEntry(String, double, Unit, Instant, LocalDate)
    +getId() String
    +getWeight() double
    +getUnit() Unit
    +getRecordedAt() Instant
    +getLocalDate() LocalDate
    +setWeight(double) void
    +setUnit(Unit) void
    +setRecordedAt(Instant) void
    +setLocalDate(LocalDate) void
    +toCsvRow() String[]
    +fromCsvRow(String[]) BodyweightEntry$
  }
  class CalisthenicsExercise {
    +CalisthenicsExercise(String, String, Laterality, Instant)
    +type() EquipmentType
    +weightLabel() String
    +validateWeight(double) void
    +requiresBodyweight() boolean
    +formatSet(double, Unit, Double) String
  }
  class DumbbellExercise {
    +DumbbellExercise(String, String, Laterality, Instant)
    +type() EquipmentType
    +weightLabel() String
    +formatSet(double, Unit, Double) String
  }
  class EquipmentType {
    <<enum>>
    BARBELL
    DUMBBELL
    STACK
    PLATE_LOADED
    CALISTHENICS
  }
  class Exercise {
    <<abstract>>
    -String id
    -String name
    -String nameKey
    -Laterality laterality
    -Instant deletedAt
    +String[] HEADER$
    #Exercise(String, String, Laterality, Instant)
    +of(EquipmentType, String, String, Laterality, Instant) Exercise$
    +nameKey(String) String$
    +type() EquipmentType*
    +weightLabel() String*
    +validateWeight(double) void
    +requiresBodyweight() boolean
    +formatSet(double, Unit, Double) String
    +warmupWeight(double) double
    +initials() String
    #formatNumber(double) String$
    +getId() String
    +getName() String
    +getNameKey() String
    +getLaterality() Laterality
    +getDeletedAt() Instant
    +setName(String) void
    +setLaterality(Laterality) void
    +softDelete(Instant) void
    +restore() void
    +isDeleted() boolean
    +toCsvRow() String[]
    +fromCsvRow(String[]) Exercise$
  }
  class Gym {
    +String SEEDED_DEFAULT_ID$
    -String id
    -String name
    -String nameKey
    -boolean isDefault
    -Instant deletedAt
    +String[] HEADER$
    +Gym(String, String, boolean, Instant)
    +getId() String
    +getName() String
    +getNameKey() String
    +isDefault() boolean
    +getDeletedAt() Instant
    +isDeleted() boolean
    +setName(String) void
    +setDefault(boolean) void
    +softDelete(Instant) void
    +toCsvRow() String[]
    +fromCsvRow(String[]) Gym$
  }
  class Laterality {
    <<enum>>
    BILATERAL
    UNILATERAL
  }
  class PlateLoadedExercise {
    +PlateLoadedExercise(String, String, Laterality, Instant)
    +type() EquipmentType
    +weightLabel() String
  }
  class SetType {
    <<enum>>
    WARMUP
    WORKING
    FAILURE
    +isWorking() boolean
  }
  class StackExercise {
    +StackExercise(String, String, Laterality, Instant)
    +type() EquipmentType
    +weightLabel() String
  }
  class Unit {
    <<enum>>
    KG
    LB
    -double kilograms
    +convert(double, Unit) double
    +label() String
  }
  class VolumeCalculable {
    <<interface>>
    +volume(Unit) double
  }
  class WorkoutSession {
    -String id
    -String gymId
    -LocalDate localDate
    -Instant startedAt
    -Instant endedAt
    -boolean open
    -List~WorkoutSet~ sets
    +String[] HEADER$
    +WorkoutSession(String, String, LocalDate, Instant, Instant, boolean)
    +volume(Unit) double
    +finish(Instant) void
    +effectiveEnd() Instant
    +duration() Duration
    +addSet(WorkoutSet) void
    +getSets() List~WorkoutSet~
    +getId() String
    +getGymId() String
    +getLocalDate() LocalDate
    +getStartedAt() Instant
    +getEndedAt() Instant
    +isOpen() boolean
    +setGymId(String) void
    +setLocalDate(LocalDate) void
    +setTimes(Instant, Instant) void
    +toCsvRow() String[]
    +fromCsvRow(String[]) WorkoutSession$
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
    +String[] HEADER$
    +WorkoutSet(String, String, String, int, SetType, double, Unit, int, Double, Instant)
    +volume(Unit) double
    +weightIn(Unit) double
    +getId() String
    +getSessionId() String
    +getExerciseId() String
    +getPosition() int
    +getSetType() SetType
    +getWeight() double
    +getUnit() Unit
    +getReps() int
    +getBodyweight() Double
    +getCompletedAt() Instant
    +setExerciseId(String) void
    +setPosition(int) void
    +setSetType(SetType) void
    +setWeight(double) void
    +setUnit(Unit) void
    +setReps(int) void
    +setBodyweight(Double) void
    +setCompletedAt(Instant) void
    +toCsvRow() String[]
    +fromCsvRow(String[]) WorkoutSet$
  }
  class WorkoutTemplate {
    -String id
    -String name
    -List~String~ exerciseIds
    +String[] HEADER$
    +WorkoutTemplate(String, String, List~String~)
    +getId() String
    +getName() String
    +getExerciseIds() List~String~
    +setName(String) void
    +setExerciseIds(List~String~) void
    +toCsvRow() String[]
    +fromCsvRow(String[]) WorkoutTemplate$
  }

  Exercise <|-- BarbellExercise
  CsvSerializable <|.. BodyweightEntry
  BodyweightEntry --> Unit
  Exercise <|-- CalisthenicsExercise
  Exercise <|-- DumbbellExercise
  CsvSerializable <|.. Exercise
  Exercise --> Laterality
  CsvSerializable <|.. Gym
  Exercise <|-- PlateLoadedExercise
  Exercise <|-- StackExercise
  VolumeCalculable <|.. WorkoutSession
  CsvSerializable <|.. WorkoutSession
  WorkoutSession --> "*" WorkoutSet
  VolumeCalculable <|.. WorkoutSet
  CsvSerializable <|.. WorkoutSet
  WorkoutSet --> SetType
  WorkoutSet --> Unit
  CsvSerializable <|.. WorkoutTemplate
  WorkoutSet ..> Exercise : exerciseId
  WorkoutSet ..> WorkoutSession : sessionId
  WorkoutSession ..> Gym : gymId
  WorkoutTemplate ..> Exercise : exerciseIds
  Exercise ..> EquipmentType : type()
  CsvSerializable ..> CsvCodec : rows formatted by
```

## 2. Exceptions (`myophoria.core.error`)

```mermaid
classDiagram
  direction TB

  class CorruptedCsvException {
    -String file
    -int line
    -String reason
    +CorruptedCsvException(String, int, String)
    +getFile() String
    +getLine() int
    +getReason() String
  }
  class DuplicateResourceException {
    -String resourceType
    -String existingId
    -boolean softDeleted
    +DuplicateResourceException(String, String, boolean)
    +getResourceType() String
    +getExistingId() String
    +isSoftDeleted() boolean
  }
  class InvalidWorkoutInputException {
    +InvalidWorkoutInputException(String)
  }
  class NotFoundException {
    -String resourceType
    -String id
    +NotFoundException(String, String)
    +getResourceType() String
    +getId() String
  }

  Exception <|-- CorruptedCsvException
  RuntimeException <|-- DuplicateResourceException
  RuntimeException <|-- InvalidWorkoutInputException
  RuntimeException <|-- NotFoundException
```

`CorruptedCsvException` is checked: a broken CSV file is an expected, recoverable condition the importer must handle. The other three are unchecked and are mapped to HTTP status codes by `ErrorHandlers`.

## 3. Ports (`myophoria.core.port`)

The interfaces the core needs from the outside. `myophoria.persistence` implements them (section 6).

```mermaid
classDiagram
  direction TB

  class Backups {
    <<interface>>
    +createBackup() String
  }
  class BodyweightRepository {
    <<interface>>
    +findAll() List~BodyweightEntry~
    +findById(String) BodyweightEntry
    +insert(BodyweightEntry) void
    +update(BodyweightEntry) void
    +delete(String) void
  }
  class ExerciseRepository {
    <<interface>>
    +findAll() List~Exercise~
    +findById(String) Exercise
    +insert(Exercise) void
    +update(Exercise) void
    +countSets(String) int
    +countSessions(String) int
    +deletePermanently(String) void
  }
  class GymRepository {
    <<interface>>
    +findAll() List~Gym~
    +findById(String) Gym
    +insert(Gym) void
    +update(Gym) void
    +makeDefault(String) void
  }
  class SessionRepository {
    <<interface>>
    +findAll() List~WorkoutSession~
    +findById(String) WorkoutSession
    +findOpen() WorkoutSession
    +insert(WorkoutSession) void
    +update(WorkoutSession) void
    +delete(String) void
  }
  class SetRepository {
    <<interface>>
    +findById(String) WorkoutSet
    +insert(WorkoutSet) void
    +update(WorkoutSet) void
    +delete(String) void
  }
  class TemplateRepository {
    <<interface>>
    +findAll() List~WorkoutTemplate~
    +findById(String) WorkoutTemplate
    +insert(WorkoutTemplate) void
    +update(WorkoutTemplate) void
    +delete(String) void
  }


```

## 4. Services (`myophoria.core.service`)

```mermaid
classDiagram
  direction TB

  class BodyweightService {
    -String RESOURCE$
    +Comparator~BodyweightEntry~ NEWEST_FIRST$
    -BodyweightRepository entries
    +BodyweightService(BodyweightRepository)
    +list() List~BodyweightEntry~
    +latest() BodyweightEntry
    +create(double, Unit, Instant, LocalDate) BodyweightEntry
    +update(String, double, Unit, Instant, LocalDate) BodyweightEntry
    +delete(String) void
  }
  class CsvService {
    +String FORMAT_VERSION$
    ~String[] MANIFEST_HEADER$
    ~long MAX_UNZIPPED_BYTES$
    -GymRepository gyms
    -ExerciseRepository exercises
    -SessionRepository sessions
    -SetRepository sets
    -BodyweightRepository bodyweights
    -TemplateRepository templates
    -Backups backups
    -Clock clock
    +CsvService(GymRepository, ExerciseRepository, SessionRepository, SetRepository, BodyweightRepository, TemplateRepository, Backups, Clock)
    +export(OutputStream) void
    +importZip(InputStream) ImportReport
  }
  class ExerciseService {
    -String RESOURCE$
    -Comparator~Exercise~ BY_NAME$
    -ExerciseRepository exercises
    -Backups backups
    -Clock clock
    +ExerciseService(ExerciseRepository, Backups, Clock)
    +search(String, Set~EquipmentType~, boolean) List~Exercise~
    +get(String) Exercise
    +allById() Map~String, Exercise~
    +create(String, EquipmentType, Laterality) Exercise
    +update(String, String, EquipmentType, Laterality) Exercise
    +softDelete(String) Exercise
    +restore(String) Exercise
    +usage(String) Usage
    +deletePermanently(String) String
  }
  class GymService {
    -String RESOURCE$
    -Comparator~Gym~ BY_NAME$
    +Comparator~WorkoutSession~ CHRONOLOGICAL$
    -GymRepository gyms
    -SessionRepository sessions
    -Clock clock
    +GymService(GymRepository, SessionRepository, Clock)
    +list() List~Gym~
    +get(String) Gym
    +create(String) Gym
    +rename(String, String) Gym
    +makeDefault(String) Gym
    +delete(String) void
    +activeGym() Gym
  }
  class HistoryService {
    +Comparator~WorkoutSession~ NEWEST_FIRST$
    -SessionRepository sessions
    -GymRepository gyms
    +HistoryService(SessionRepository, GymRepository)
    +list(HistoryFilter) List~SessionHeader~
    +get(String) WorkoutSession
    +backfill(String, LocalDate, Instant, Instant) WorkoutSession
    +edit(String, String, LocalDate, Instant, Instant) WorkoutSession
    +delete(String) void
  }
  class ReportService {
    -SessionRepository sessions
    -ExerciseRepository exercises
    +ReportService(SessionRepository, ExerciseRepository)
    +summary(String, Unit) SessionSummary
    +progression(String, Unit) List~ProgressPoint~
  }
  class TemplateService {
    -String RESOURCE$
    -Comparator~WorkoutTemplate~ BY_NAME$
    -TemplateRepository templates
    -ExerciseRepository exercises
    -SessionRepository sessions
    +TemplateService(TemplateRepository, ExerciseRepository, SessionRepository)
    +list() List~WorkoutTemplate~
    +create(String, List~String~) WorkoutTemplate
    +update(String, String, List~String~) WorkoutTemplate
    +delete(String) void
    +saveFromSession(String, String) WorkoutTemplate
  }
  class WorkoutService {
    -Comparator~WorkoutSet~ BY_POSITION$
    -SessionRepository sessions
    -SetRepository sets
    -ExerciseRepository exercises
    -GymRepository gyms
    -BodyweightRepository bodyweights
    -Clock clock
    +WorkoutService(SessionRepository, SetRepository, ExerciseRepository, GymRepository, BodyweightRepository, Clock)
    +open() WorkoutSession
    +start(String, LocalDate) WorkoutSession
    +finish(String) WorkoutSession
    +addSet(String, SetInput) WorkoutSet
    +updateSet(String, SetInput) WorkoutSet
    +deleteSet(String) void
    +prefill(String, String) Prefill
  }

  BodyweightService --> BodyweightRepository
  CsvService --> GymRepository
  CsvService --> ExerciseRepository
  CsvService --> SessionRepository
  CsvService --> SetRepository
  CsvService --> BodyweightRepository
  CsvService --> TemplateRepository
  CsvService --> Backups
  ExerciseService --> ExerciseRepository
  ExerciseService --> Backups
  GymService --> GymRepository
  GymService --> SessionRepository
  HistoryService --> SessionRepository
  HistoryService --> GymRepository
  ReportService --> SessionRepository
  ReportService --> ExerciseRepository
  TemplateService --> TemplateRepository
  TemplateService --> ExerciseRepository
  TemplateService --> SessionRepository
  WorkoutService --> SessionRepository
  WorkoutService --> SetRepository
  WorkoutService --> ExerciseRepository
  WorkoutService --> GymRepository
  WorkoutService --> BodyweightRepository
```

## 5. Service inputs and results (records in `myophoria.core.service`)

```mermaid
classDiagram
  direction TB

  class ExerciseSummary {
    <<record>>
    +String exerciseId
    +String name
    +int workingSets
    +Double topWeight
    +Double averageWeight
    +double volume
  }
  class FileReport {
    <<record>>
    +int imported
    +int alreadyPresent
    +List~String~ skipped
    +List~String~ orphans
    +List~String~ nameConflicts
    +List~String~ adjusted
  }
  class HistoryFilter {
    <<record>>
    +LocalDate from
    +LocalDate to
    +String gymId
    +String exerciseId
  }
  class ImportReport {
    <<record>>
    +Map~String, FileReport~ files
  }
  class Prefill {
    <<record>>
    +Unit unit
    +List~PrefillRow~ rows
    +Double warmupWeight
    +Double bodyweight
  }
  class PrefillRow {
    <<record>>
    +SetType setType
    +double weight
    +int reps
  }
  class ProgressPoint {
    <<record>>
    +LocalDate localDate
    +double topWeight
  }
  class SessionHeader {
    <<record>>
    +String id
    +String gymName
    +LocalDate localDate
    +Instant startedAt
    +Instant endedAt
    +int setCount
    +boolean open
  }
  class SessionSummary {
    <<record>>
    +String sessionId
    +LocalDate localDate
    +Instant startedAt
    +Instant endedAt
    +Long durationSeconds
    +Unit unit
    +int totalSets
    +int workingSets
    +double volume
    +List~ExerciseSummary~ exercises
  }
  class SetInput {
    <<record>>
    +String exerciseId
    +SetType setType
    +double weight
    +Unit unit
    +int reps
    +Double bodyweight
    +int position
  }
  class Usage {
    <<record>>
    +int sets
    +int sessions
  }

  Prefill --> Unit
  Prefill --> "*" PrefillRow
  PrefillRow --> SetType
  SessionSummary --> Unit
  SessionSummary --> "*" ExerciseSummary
  SetInput --> SetType
  SetInput --> Unit
  ImportReport --> "*" FileReport : files
```

## 6. Persistence adapters (`myophoria.persistence`)

```mermaid
classDiagram
  direction TB

  class Database {
    +String FILE_NAME$
    -int SQLITE_CORRUPT$
    -int SQLITE_NOTADB$
    -String url
    -Path dataDir
    +Database(Path)
    +backupDir() Path
    +open() List~String~
    +connect() Connection
    ~format(Instant) String$
    ~instant(String) Instant$
  }
  class Migrator {
    -List~String~ SCRIPTS$
    +migrate(Connection) void$
  }
  class SqliteBackups {
    +DateTimeFormatter FILE_TIME$
    -Database db
    -Path backupDir
    -Clock clock
    +SqliteBackups(Database, Path, Clock)
    +createBackup() String
  }
  class SqliteBodyweightRepository {
    -String SELECT$
    -Database db
    +SqliteBodyweightRepository(Database)
    +findAll() List~BodyweightEntry~
    +findById(String) BodyweightEntry
    +insert(BodyweightEntry) void
    +update(BodyweightEntry) void
    +delete(String) void
  }
  class SqliteExerciseRepository {
    -String COLUMNS$
    -Database db
    +SqliteExerciseRepository(Database)
    +findAll() List~Exercise~
    +findById(String) Exercise
    +insert(Exercise) void
    +update(Exercise) void
    +countSets(String) int
    +countSessions(String) int
    +deletePermanently(String) void
  }
  class SqliteGymRepository {
    -String SELECT$
    -Database db
    +SqliteGymRepository(Database)
    +findAll() List~Gym~
    +findById(String) Gym
    +insert(Gym) void
    +update(Gym) void
    +makeDefault(String) void
  }
  class SqliteSessionRepository {
    -String SELECT$
    -String SET_ORDER$
    -Database db
    +SqliteSessionRepository(Database)
    +findAll() List~WorkoutSession~
    +findById(String) WorkoutSession
    +findOpen() WorkoutSession
    +insert(WorkoutSession) void
    +update(WorkoutSession) void
    +delete(String) void
  }
  class SqliteSetRepository {
    ~String SELECT$
    -Database db
    +SqliteSetRepository(Database)
    +findById(String) WorkoutSet
    +insert(WorkoutSet) void
    +update(WorkoutSet) void
    +delete(String) void
    ~map(ResultSet) WorkoutSet$
  }
  class SqliteTemplateRepository {
    -Database db
    +SqliteTemplateRepository(Database)
    +findAll() List~WorkoutTemplate~
    +findById(String) WorkoutTemplate
    +insert(WorkoutTemplate) void
    +update(WorkoutTemplate) void
    +delete(String) void
  }

  Backups <|.. SqliteBackups
  SqliteBackups --> Database
  BodyweightRepository <|.. SqliteBodyweightRepository
  SqliteBodyweightRepository --> Database
  ExerciseRepository <|.. SqliteExerciseRepository
  SqliteExerciseRepository --> Database
  GymRepository <|.. SqliteGymRepository
  SqliteGymRepository --> Database
  SessionRepository <|.. SqliteSessionRepository
  SqliteSessionRepository --> Database
  SetRepository <|.. SqliteSetRepository
  SqliteSetRepository --> Database
  TemplateRepository <|.. SqliteTemplateRepository
  SqliteTemplateRepository --> Database
  Database ..> Migrator : open() runs
```

## 7. HTTP adapters and bootstrap (`myophoria.api`)

```mermaid
classDiagram
  direction TB

  class BodyweightController {
    -BodyweightService service
    +BodyweightController(BodyweightService)
    +register(RoutesConfig) void
  }
  class DataController {
    -List~String~ warnings
    -Backups backups
    -CsvService csv
    +DataController(List~String~, Backups, CsvService)
    +register(RoutesConfig) void
  }
  class ErrorHandlers {
    -Logger LOG$
    +register(JavalinConfig) void$
  }
  class ExerciseController {
    -ExerciseService service
    +ExerciseController(ExerciseService)
    +register(RoutesConfig) void
  }
  class GymController {
    -GymService service
    +GymController(GymService)
    +register(RoutesConfig) void
  }
  class HistoryController {
    -HistoryService history
    -ExerciseService exercises
    +HistoryController(HistoryService, ExerciseService)
    +register(RoutesConfig) void
  }
  class Main {
    -Set~String~ LOCAL_HOSTS$
    +Main()
    +main(String[]) void$
    ~createApp(Database, Path, List~String~, Set~String~) Javalin$
    ~jsonMapper() ObjectMapper$
    ~hostAllowed(String, boolean, Set~String~) boolean$
  }
  class ReportController {
    -ReportService reports
    +ReportController(ReportService)
    +register(RoutesConfig) void
    ~unit(String) Unit$
  }
  class TemplateController {
    -TemplateService templates
    -ExerciseService exercises
    +TemplateController(TemplateService, ExerciseService)
    +register(RoutesConfig) void
  }
  class WorkoutController {
    -WorkoutService workouts
    -ExerciseService exercises
    +WorkoutController(WorkoutService, ExerciseService)
    +register(RoutesConfig) void
  }

  BodyweightController --> BodyweightService
  DataController --> Backups
  DataController --> CsvService
  ExerciseController --> ExerciseService
  GymController --> GymService
  HistoryController --> HistoryService
  HistoryController --> ExerciseService
  ReportController --> ReportService
  TemplateController --> TemplateService
  TemplateController --> ExerciseService
  WorkoutController --> WorkoutService
  WorkoutController --> ExerciseService
  Main ..> Database : opens
  Main ..> ErrorHandlers : registers
  Main ..> DataController : wires every controller
```
