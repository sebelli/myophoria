// State of the running workout, shared by the Workout screen and the exercise cards.
// Unticked rows are drafts that live only here (handoff §7.2); ticked rows are saved sets.
import { api } from './api'
import { confirmAction } from './dialog.svelte'
import { round2 } from './format'
import { go } from './router.svelte'
import type { Exercise, Session, SetType, Unit } from './types'

export interface Row {
  key: number
  setId: string | null // null while the row is an unticked draft
  setType: SetType
  weight: string
  reps: string
  unit: Unit // a ticked set keeps the unit it was saved in
  busy: boolean
  error: string
}

export interface Card {
  exercise: Exercise
  unit: Unit
  rows: Row[]
  warmupWeight: number | null
  bodyweight: string // Calisthenics only, in the card's unit
}

export const workout = $state<{
  session: Session | null
  cards: Record<string, Card>
  queue: string[] // template exercises not opened yet
  open: string | null // the exercise expanded on the Workout screen (one at a time)
}>({ session: null, cards: {}, queue: [], open: null })

let nextKey = 1
export const newKey = () => nextKey++

const QUEUE_KEY = 'myophoria.queue'

function saveQueue() {
  try {
    if (workout.session) localStorage.setItem(QUEUE_KEY, JSON.stringify({ id: workout.session.id, queue: workout.queue }))
    else localStorage.removeItem(QUEUE_KEY)
  } catch {
    // private mode: the queue just does not survive a reload
  }
}

function loadQueue(sessionId: string): string[] {
  try {
    const saved = JSON.parse(localStorage.getItem(QUEUE_KEY) ?? 'null')
    return saved?.id === sessionId ? saved.queue : []
  } catch {
    return []
  }
}

/** Loads the open session (if any); keeps drafts when it is the same session. */
export async function loadOpen(): Promise<Session | null> {
  const s = await api.openSession()
  if (!s || s.id !== workout.session?.id) {
    workout.cards = {}
    workout.queue = s ? loadQueue(s.id) : []
    workout.open = null
  }
  workout.session = s
  return s
}

export async function startWorkout(gymId: string, localDate: string, queue: string[]) {
  workout.session = await api.startSession(gymId, localDate)
  workout.cards = {}
  workout.queue = queue
  workout.open = null
  saveQueue()
}

/** Exercise ids in the order they were first logged, then cards opened but not logged yet. */
export function exerciseOrder(): string[] {
  const ids: string[] = []
  for (const s of workout.session?.sets ?? []) if (!ids.includes(s.exerciseId)) ids.push(s.exerciseId)
  for (const id of Object.keys(workout.cards)) if (!ids.includes(id)) ids.push(id)
  return ids
}

/** Builds a card the first time an exercise is opened: saved sets, else last session's rows, else one blank row. */
export async function openCard(exerciseId: string): Promise<Card> {
  const existing = workout.cards[exerciseId]
  if (existing) return existing
  const session = workout.session!
  const [exercise, prefill] = await Promise.all([api.getExercise(exerciseId), api.prefill(session.id, exerciseId)])
  const saved = session.sets.filter((s) => s.exerciseId === exerciseId).sort((a, b) => a.position - b.position)
  const rows: Row[] = saved.length
    ? saved.map((s) => ({ key: newKey(), setId: s.id, setType: s.setType, weight: String(s.weight), reps: String(s.reps), unit: s.unit, busy: false, error: '' }))
    : prefill.rows.map((r) => ({ key: newKey(), setId: null, setType: r.setType, weight: String(round2(r.weight)), reps: String(r.reps), unit: prefill.unit, busy: false, error: '' }))
  if (rows.length === 0) rows.push(blankRow(prefill.unit))
  const lastBody = saved.findLast((s) => s.bodyweight != null)?.bodyweight ?? prefill.bodyweight
  workout.cards[exerciseId] = {
    exercise,
    unit: prefill.unit,
    rows,
    warmupWeight: prefill.warmupWeight,
    bodyweight: lastBody == null ? '' : String(round2(lastBody)),
  }
  workout.queue = workout.queue.filter((id) => id !== exerciseId)
  saveQueue()
  return workout.cards[exerciseId]
}

export function blankRow(unit: Unit): Row {
  return { key: newKey(), setId: null, setType: 'WORKING', weight: '', reps: '', unit, busy: false, error: '' }
}

function draftCount(): number {
  let n = 0
  for (const card of Object.values(workout.cards)) for (const r of card.rows) if (!r.setId && (r.weight || r.reps)) n++
  return n
}

/** Finish flow (handoff §8.6): warn about drafts, discard an empty workout, else finish and show the summary. */
export async function finishWorkout() {
  const open = await loadOpen()
  if (!open) return go('workout')
  const drafts = draftCount()
  if (drafts && !(await confirmAction(`${drafts} unticked set${drafts > 1 ? 's' : ''} will be discarded.`, 'Finish anyway'))) return
  if (open.sets.length === 0) {
    if (!(await confirmAction('No sets were ticked. Discard this workout?', 'Discard workout', true))) return
    await api.deleteSession(open.id)
    reset()
    return go('workout')
  }
  await api.finishSession(open.id)
  reset()
  go('summary/' + open.id)
}

/** Throws the running workout away, sets included, after a confirmation that names the set count. */
export async function discardWorkout() {
  const open = await loadOpen()
  if (!open) return go('workout')
  const n = open.sets.length
  const what = n ? `this workout and its ${n} logged set${n === 1 ? '' : 's'}` : 'this workout'
  if (!(await confirmAction(`Discard ${what}? This cannot be undone.`, 'Discard workout', true))) return
  await api.deleteSession(open.id)
  reset()
  go('workout')
}

function reset() {
  workout.session = null
  workout.cards = {}
  workout.queue = []
  workout.open = null
  saveQueue()
}
