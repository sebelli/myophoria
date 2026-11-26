import type { EquipmentType, SetType, Unit } from './types'

// Display rounding only; values are stored exactly as entered (handoff §7.1).

/** Rounds to at most 2 decimals for inputs and labels. */
export function round2(n: number): number {
  return Math.round(n * 100) / 100
}

/** "102.5", "100", "45.36" */
export function num(n: number | null | undefined): string {
  return n == null ? '–' : String(round2(n))
}

export const unitLabel = (u: Unit) => u.toLowerCase()

export const typeLabel = (t: EquipmentType) =>
  ({ BARBELL: 'Barbell', DUMBBELL: 'Dumbbell', STACK: 'Stack', PLATE_LOADED: 'Plate loaded', CALISTHENICS: 'Calisthenics' })[t]

/** Today as YYYY-MM-DD in the device's time zone (the client owns localDate). */
export function today(): string {
  return localDate(new Date())
}

export function localDate(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** "07:30" in local time, or "–". */
export function time(iso: string | null): string {
  if (!iso) return '–'
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

/** Time since an ISO instant: "5:07", "42:13", "1:02:13"; never negative. */
export function elapsed(since: string, now: number): string {
  const s = Math.max(0, Math.floor((now - Date.parse(since)) / 1000))
  const h = Math.floor(s / 3600)
  const mmss = `${String(Math.floor((s % 3600) / 60)).padStart(h ? 2 : 1, '0')}:${String(s % 60).padStart(2, '0')}`
  return h ? `${h}:${mmss}` : mmss
}

/** "Tue 25 Nov 2026" */
export function longDate(date: string): string {
  const [y, m, d] = date.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
}

/** "1 h 05 min", "42 min" */
export function duration(seconds: number | null): string {
  if (seconds == null) return '–'
  const m = Math.round(seconds / 60)
  return m >= 60 ? `${Math.floor(m / 60)} h ${String(m % 60).padStart(2, '0')} min` : `${m} min`
}

/** Value for <input type="datetime-local"> from an ISO instant (local time), or ''. */
export function toLocalInput(iso: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${localDate(d)}T${p(d.getHours())}:${p(d.getMinutes())}`
}

/** ISO instant from an <input type="datetime-local"> value, or null when empty. */
export function fromLocalInput(value: string): string | null {
  return value ? new Date(value).toISOString() : null
}

/** Set labels: W for warm-ups, F for failure, normal sets numbered 1, 2, … (W, 1, 2, F). */
export function setLabels(types: SetType[]): string[] {
  let n = 0
  return types.map((t) => (t === 'WARMUP' ? 'W' : t === 'FAILURE' ? 'F' : String(++n)))
}

/** Tapping a set label cycles number → W → F → number. */
export function nextSetType(t: SetType): SetType {
  return t === 'WORKING' ? 'WARMUP' : t === 'WARMUP' ? 'FAILURE' : 'WORKING'
}
