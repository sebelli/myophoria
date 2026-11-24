import type { ErrorBody, Exercise, ExerciseInput, EquipmentType } from './types'

export class ApiError extends Error {
  constructor(
    public status: number,
    public error: string,
    message: string,
    public details?: Record<string, unknown>,
  ) {
    super(message)
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let res: Response
  try {
    res = await fetch('/api/v1' + path, {
      method,
      headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server')
  }
  if (res.status === 204) return undefined as T
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

export const api = {
  status: () => request<{ warnings: string[] }>('GET', '/status'),
  listExercises: (q: string, types: EquipmentType[], deleted = false) => {
    const params = new URLSearchParams()
    if (q) params.set('q', q)
    if (types.length) params.set('type', types.join(','))
    if (deleted) params.set('deleted', 'true')
    const qs = params.toString()
    return request<Exercise[]>('GET', '/exercises' + (qs ? '?' + qs : ''))
  },
  createExercise: (input: ExerciseInput) => request<Exercise>('POST', '/exercises', input),
  updateExercise: (id: string, input: ExerciseInput) => request<Exercise>('PUT', `/exercises/${id}`, input),
  deleteExercise: (id: string) => request<void>('DELETE', `/exercises/${id}`),
  restoreExercise: (id: string) => request<Exercise>('POST', `/exercises/${id}/restore`),
}
