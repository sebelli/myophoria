import type {
  BodyweightEntry, ErrorBody, Exercise, ExerciseInput, EquipmentType, Gym, ImportReport, Prefill, ProgressPoint,
  Session, SessionHeader, SessionSummary, SetInput, Template, Unit, Usage, WorkoutSet,
} from './types'

/** Error thrown for every failed call; message is safe to show to the user. */
export class ApiError extends Error {
  constructor(
    public status: number,
    public error: string,
    message: string,
    public details?: ErrorBody['details'],
  ) {
    super(message)
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let res: Response
  try {
    const isForm = body instanceof FormData
    res = await fetch('/api/v1' + path, {
      method,
      headers: body === undefined || isForm ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Is it running?')
  }
  if (res.status === 204) return null as T
  const text = await res.text()
  let data: unknown
  try {
    data = JSON.parse(text)
  } catch {
    throw new ApiError(res.status, 'INTERNAL', 'Unexpected response from the server')
  }
  if (!res.ok) {
    const e = data as ErrorBody
    throw new ApiError(res.status, e.error, e.message, e.details)
  }
  return data as T
}

const unitParam = (unit: Unit) => `unit=${unit.toLowerCase()}`

function query(params: Record<string, string | undefined | null>) {
  const p = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) if (v) p.set(k, v)
  const s = p.toString()
  return s ? '?' + s : ''
}

export const api = {
  status: () => request<{ warnings: string[] }>('GET', '/status'),

  listExercises: (q: string, types: EquipmentType[], deleted = false) =>
    request<Exercise[]>('GET', '/exercises' + query({ q, type: types.join(','), deleted: deleted ? 'true' : null })),
  getExercise: (id: string) => request<Exercise>('GET', `/exercises/${id}`),
  createExercise: (input: ExerciseInput) => request<Exercise>('POST', '/exercises', input),
  updateExercise: (id: string, input: ExerciseInput) => request<Exercise>('PUT', `/exercises/${id}`, input),
  deleteExercise: (id: string) => request<null>('DELETE', `/exercises/${id}`),
  restoreExercise: (id: string) => request<Exercise>('POST', `/exercises/${id}/restore`),
  exerciseUsage: (id: string) => request<Usage>('GET', `/exercises/${id}/usage`),
  deleteExercisePermanently: (id: string) => request<{ backupFile: string }>('DELETE', `/exercises/${id}/permanent`),
  progression: (id: string, unit: Unit) => request<ProgressPoint[]>('GET', `/exercises/${id}/progression?${unitParam(unit)}`),

  listGyms: () => request<Gym[]>('GET', '/gyms'),
  activeGym: () => request<Gym>('GET', '/gyms/active'),
  createGym: (name: string) => request<Gym>('POST', '/gyms', { name }),
  renameGym: (id: string, name: string) => request<Gym>('PUT', `/gyms/${id}`, { name }),
  makeDefaultGym: (id: string) => request<Gym>('POST', `/gyms/${id}/default`),
  deleteGym: (id: string) => request<null>('DELETE', `/gyms/${id}`),

  openSession: () => request<Session | null>('GET', '/sessions/open'),
  startSession: (gymId: string, localDate: string) => request<Session>('POST', '/sessions/start', { gymId, localDate }),
  finishSession: (id: string) => request<Session>('POST', `/sessions/${id}/finish`),
  prefill: (sessionId: string, exerciseId: string) =>
    request<Prefill>('GET', `/sessions/${sessionId}/prefill?exerciseId=${encodeURIComponent(exerciseId)}`),
  summary: (sessionId: string, unit: Unit) => request<SessionSummary>('GET', `/sessions/${sessionId}/summary?${unitParam(unit)}`),
  saveAsTemplate: (sessionId: string, name: string) => request<Template>('POST', `/sessions/${sessionId}/template`, { name }),
  addSet: (sessionId: string, input: SetInput) => request<WorkoutSet>('POST', `/sessions/${sessionId}/sets`, input),
  updateSet: (id: string, input: SetInput) => request<WorkoutSet>('PUT', `/sets/${id}`, input),
  deleteSet: (id: string) => request<null>('DELETE', `/sets/${id}`),

  history: (f: { from?: string; to?: string; gymId?: string; exerciseId?: string }) =>
    request<SessionHeader[]>('GET', '/sessions' + query(f)),
  getSession: (id: string) => request<Session>('GET', `/sessions/${id}`),
  backfill: (b: { gymId: string; localDate: string; startedAt: string | null; endedAt: string | null }) =>
    request<Session>('POST', '/sessions', b),
  editSession: (id: string, b: { gymId: string; localDate: string; startedAt: string | null; endedAt: string | null }) =>
    request<Session>('PUT', `/sessions/${id}`, b),
  deleteSession: (id: string) => request<null>('DELETE', `/sessions/${id}`),

  listBodyweight: () => request<BodyweightEntry[]>('GET', '/bodyweight'),
  latestBodyweight: () => request<BodyweightEntry | null>('GET', '/bodyweight/latest'),
  createBodyweight: (b: Omit<BodyweightEntry, 'id'>) => request<BodyweightEntry>('POST', '/bodyweight', b),
  updateBodyweight: (id: string, b: Omit<BodyweightEntry, 'id'>) => request<BodyweightEntry>('PUT', `/bodyweight/${id}`, b),
  deleteBodyweight: (id: string) => request<null>('DELETE', `/bodyweight/${id}`),

  listTemplates: () => request<Template[]>('GET', '/templates'),
  createTemplate: (name: string, exerciseIds: string[]) => request<Template>('POST', '/templates', { name, exerciseIds }),
  updateTemplate: (id: string, name: string, exerciseIds: string[]) =>
    request<Template>('PUT', `/templates/${id}`, { name, exerciseIds }),
  deleteTemplate: (id: string) => request<null>('DELETE', `/templates/${id}`),

  backup: () => request<{ file: string }>('POST', '/backups'),
  importZip: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<ImportReport>('POST', '/import', form)
  },
}

/** Turns any thrown value into a message for the user. */
export function errorMessage(e: unknown): string {
  return e instanceof ApiError ? e.message : e instanceof Error ? e.message : String(e)
}
