<script lang="ts">
  import ErrorText from '../components/ErrorText.svelte'
  import { api, errorMessage } from '../lib/api'
  import { confirmAction } from '../lib/dialog.svelte'
  import { fromLocalInput, longDate, setLabels, toLocalInput } from '../lib/format'
  import { go, route } from '../lib/router.svelte'
  import type { Exercise, Gym, Session, SetType, Unit, WorkoutSet } from '../lib/types'

  const id = route.parts[1]
  let session = $state<Session | null>(null)
  let gyms = $state<Gym[]>([])
  let exercises = $state<Record<string, Exercise>>({})
  let error = $state('')
  let message = $state('')
  // session form
  let gymId = $state('')
  let localDate = $state('')
  let startedAt = $state('')
  let endedAt = $state('')
  // add-set form
  let newExercise = $state('')
  let newType = $state<SetType>('WORKING')
  let newWeight = $state('')
  let newUnit = $state<Unit>('KG')
  let newReps = $state('')
  let newBody = $state('')

  async function load() {
    try {
      const [s, g, active, deleted] = await Promise.all([
        api.getSession(id),
        api.listGyms(),
        api.listExercises('', []),
        api.listExercises('', [], true),
      ])
      session = s
      gyms = g
      exercises = Object.fromEntries([...active, ...deleted].map((e) => [e.id, e]))
      gymId = s.gymId
      localDate = s.localDate
      startedAt = toLocalInput(s.startedAt)
      endedAt = toLocalInput(s.endedAt)
      newExercise ||= active[0]?.id ?? ''
    } catch (e) {
      error = errorMessage(e)
    }
  }
  load()

  // sets grouped by exercise, in the order each exercise was first logged
  const groups = $derived.by(() => {
    const map = new Map<string, WorkoutSet[]>()
    for (const s of session?.sets ?? []) {
      if (!map.has(s.exerciseId)) map.set(s.exerciseId, [])
      map.get(s.exerciseId)!.push(s)
    }
    for (const list of map.values()) list.sort((a, b) => a.position - b.position)
    return [...map.entries()]
  })

  async function saveSession(event: SubmitEvent) {
    event.preventDefault()
    try {
      session = await api.editSession(id, { gymId, localDate, startedAt: fromLocalInput(startedAt), endedAt: fromLocalInput(endedAt) })
      message = 'Saved.'
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function saveSet(s: WorkoutSet) {
    try {
      await api.updateSet(s.id, {
        exerciseId: s.exerciseId,
        setType: s.setType,
        weight: Number(s.weight),
        unit: s.unit,
        reps: Number(s.reps),
        bodyweight: s.bodyweight == null || (s.bodyweight as unknown) === '' ? null : Number(s.bodyweight),
        position: s.position,
      })
      await load()
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function deleteSet(s: WorkoutSet) {
    const last = session!.sets.length === 1 && !session!.open
    const text = last ? 'Delete this set? It is the last one, so the workout is removed too.' : 'Delete this set?'
    if (!(await confirmAction(text, 'Delete', true))) return
    try {
      await api.deleteSet(s.id)
      if (last) return go('history')
      await load()
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function addSet(event: SubmitEvent) {
    event.preventDefault()
    const calisthenics = exercises[newExercise]?.equipmentType === 'CALISTHENICS'
    try {
      await api.addSet(id, {
        exerciseId: newExercise,
        setType: newType,
        weight: Number(newWeight),
        unit: newUnit,
        reps: Number(newReps),
        bodyweight: calisthenics ? Number(newBody) : null,
        position: (session?.sets.filter((s) => s.exerciseId === newExercise).length ?? 0) + 1,
      })
      newWeight = ''
      newReps = ''
      await load()
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function deleteSession() {
    const n = session!.sets.length
    if (!(await confirmAction(`Delete this workout and its ${n} set${n === 1 ? '' : 's'}?`, 'Delete workout', true))) return
    try {
      await api.deleteSession(id)
      go('history')
    } catch (e) {
      error = errorMessage(e)
    }
  }
</script>

<a href="#/history" class="mb-3 inline-flex min-h-11 items-center text-sm text-slate-600">← History</a>
<ErrorText message={error} />

{#if session}
  <h1 class="mb-4 text-xl font-semibold">{longDate(session.localDate)}</h1>

  <form class="card mb-4 space-y-2 p-3" onsubmit={saveSession}>
    <label class="block"
      ><span class="label">Gym{session.open ? ' (locked while the workout runs)' : ''}</span>
      <select class="input" bind:value={gymId} disabled={session.open}>
        {#each gyms as g}<option value={g.id}>{g.name}</option>{/each}
        {#if !gyms.some((g) => g.id === gymId)}<option value={gymId}>Deleted gym</option>{/if}
      </select>
    </label>
    <label class="block"><span class="label">Date</span><input class="input" type="date" bind:value={localDate} required /></label>
    <label class="block"><span class="label">Start</span><input class="input" type="datetime-local" bind:value={startedAt} /></label>
    {#if !session.open}
      <label class="block"><span class="label">End</span><input class="input" type="datetime-local" bind:value={endedAt} /></label>
    {/if}
    {#if message}<p class="text-sm text-green-700">{message}</p>{/if}
    <button class="btn btn-primary w-full" type="submit">Save workout details</button>
  </form>

  {#each groups as [exerciseId, sets] (exerciseId)}
    {@const labels = setLabels(sets.map((s) => s.setType))}
    <section class="card mb-3 p-3">
      <h2 class="mb-2 font-medium">{exercises[exerciseId]?.name ?? sets[0].exerciseName ?? 'Exercise'}</h2>
      <ul class="space-y-2">
        {#each sets as s, i (s.id)}
          <li class="grid grid-cols-[2.5rem_1fr_1fr_auto] items-center gap-2">
            <select class="input px-1" aria-label="Set type" bind:value={s.setType}>
              <option value="WORKING">{s.setType === 'WORKING' ? labels[i] : 'N'}</option>
              <option value="WARMUP">W</option>
              <option value="FAILURE">F</option>
            </select>
            <span class="flex items-center gap-1">
              <input class="input text-center" inputmode="decimal" aria-label="Weight" bind:value={s.weight} />
              <span class="text-xs">{s.unit.toLowerCase()}</span>
            </span>
            <input class="input text-center" inputmode="numeric" aria-label="Reps" bind:value={s.reps} />
            <span class="flex gap-1">
              <button type="button" class="btn px-2" onclick={() => saveSet(s)}>Save</button>
              <button type="button" class="btn px-2 text-red-700" aria-label="Delete set" onclick={() => deleteSet(s)}>×</button>
            </span>
            {#if s.bodyweight != null}
              <label class="col-span-4 flex items-center gap-2 text-sm"
                ><span class="text-slate-600">Bodyweight</span>
                <input class="input w-24" inputmode="decimal" bind:value={s.bodyweight} />
              </label>
            {/if}
          </li>
        {/each}
      </ul>
    </section>
  {:else}
    <p class="mb-3 text-slate-600">No sets yet.</p>
  {/each}

  <form class="card mb-4 space-y-2 p-3" onsubmit={addSet}>
    <h2 class="font-medium">Add a set</h2>
    <select class="input" aria-label="Exercise" bind:value={newExercise} required>
      {#each Object.values(exercises).filter((e) => !e.deleted) as e}<option value={e.id}>{e.name}</option>{/each}
    </select>
    <div class="grid grid-cols-3 gap-2">
      <select class="input" aria-label="Set type" bind:value={newType}>
        <option value="WORKING">Normal</option>
        <option value="WARMUP">Warm-up</option>
        <option value="FAILURE">Failure</option>
      </select>
      <input class="input" inputmode="decimal" placeholder="Weight" aria-label="Weight" bind:value={newWeight} required />
      <select class="input" aria-label="Unit" bind:value={newUnit}>
        <option value="KG">kg</option>
        <option value="LB">lb</option>
      </select>
    </div>
    <input class="input" inputmode="numeric" placeholder="Reps" aria-label="Reps" bind:value={newReps} required />
    {#if exercises[newExercise]?.equipmentType === 'CALISTHENICS'}
      <input class="input" inputmode="decimal" placeholder="Bodyweight" aria-label="Bodyweight" bind:value={newBody} required />
    {/if}
    <button class="btn btn-primary w-full" type="submit">Add set</button>
  </form>

  <button type="button" class="btn w-full text-red-700" onclick={deleteSession}>Delete workout</button>
{/if}
