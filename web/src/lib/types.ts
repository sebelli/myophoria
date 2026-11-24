// Mirrors the API DTOs (PLAN §6.2). Instants are ISO strings, dates are YYYY-MM-DD.
export type EquipmentType = 'BARBELL' | 'DUMBBELL' | 'STACK' | 'PLATE_LOADED' | 'CALISTHENICS'
export type Laterality = 'BILATERAL' | 'UNILATERAL'
export type SetType = 'WARMUP' | 'WORKING' | 'FAILURE'
export type Unit = 'KG' | 'LB'

export const EQUIPMENT_TYPES: EquipmentType[] = ['BARBELL', 'DUMBBELL', 'STACK', 'PLATE_LOADED', 'CALISTHENICS']

export interface Exercise {
  id: string
  name: string
  equipmentType: EquipmentType
  laterality: Laterality
  initials: string
  weightLabel: string
  deleted: boolean
}

export interface ExerciseInput {
  name: string
  equipmentType: EquipmentType
  laterality: Laterality
}

export interface Gym {
  id: string
  name: string
  isDefault: boolean
}

export interface WorkoutSet {
  id: string
  sessionId: string
  exerciseId: string
  exerciseName: string | null
  position: number
  setType: SetType
  weight: number
  unit: Unit
  reps: number
  bodyweight: number | null
  completedAt: string
  display: string | null
}

export interface SetInput {
  exerciseId: string
  setType: SetType
  weight: number
  unit: Unit
  reps: number
  bodyweight: number | null
  position: number
}

export interface Session {
  id: string
  gymId: string
  localDate: string
  startedAt: string | null
  endedAt: string | null
  open: boolean
  sets: WorkoutSet[]
}

export interface SessionHeader {
  id: string
  gymName: string | null
  localDate: string
  startedAt: string | null
  endedAt: string | null
  setCount: number
  open: boolean
}

export interface PrefillRow {
  setType: SetType
  weight: number
  reps: number
}

export interface Prefill {
  unit: Unit
  rows: PrefillRow[]
  warmupWeight: number | null
  bodyweight: number | null
}

export interface ExerciseSummary {
  exerciseId: string
  name: string | null
  workingSets: number
  topWeight: number | null
  averageWeight: number | null
  volume: number
}

export interface SessionSummary {
  sessionId: string
  localDate: string
  startedAt: string | null
  endedAt: string | null
  durationSeconds: number | null
  unit: Unit
  totalSets: number
  workingSets: number
  volume: number
  exercises: ExerciseSummary[]
}

export interface ProgressPoint {
  localDate: string
  topWeight: number
}

export interface BodyweightEntry {
  id: string
  weight: number
  unit: Unit
  recordedAt: string
  localDate: string
}

export interface Template {
  id: string
  name: string
  exercises: Exercise[]
}

export interface FileReport {
  imported: number
  alreadyPresent: number
  skipped: string[]
  orphans: string[]
  nameConflicts: string[]
  adjusted: string[]
}

export interface ImportReport {
  files: Record<string, FileReport>
}

export interface Usage {
  sets: number
  sessions: number
}

export interface ErrorBody {
  error: string
  message: string
  details?: { resourceType?: string; existingId?: string; softDeleted?: boolean }
}
