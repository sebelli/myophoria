<script lang="ts">
  import Icon from '../components/Icon.svelte'
  import Badge from '../components/Badge.svelte'
  import ErrorText from '../components/ErrorText.svelte'
  import ProgressChart from '../components/ProgressChart.svelte'
  import { api, errorMessage } from '../lib/api'
  import { confirmAction } from '../lib/dialog.svelte'
  import { typeLabel } from '../lib/format'
  import { go, route } from '../lib/router.svelte'
  import { EQUIPMENT_TYPES, type EquipmentType, type Exercise, type Laterality, type ProgressPoint, type Unit, type Usage } from '../lib/types'

  const id = route.parts[1]
  let exercise = $state<Exercise | null>(null)
  let usage = $state<Usage | null>(null)
  let name = $state('')
  let equipmentType = $state<EquipmentType>('BARBELL')
  let laterality = $state<Laterality>('BILATERAL')
  let unit = $state<Unit>('KG')
  let points = $state<ProgressPoint[]>([])
  let error = $state('')
  let message = $state('')

  async function load() {
    try {
      const [e, u] = await Promise.all([api.getExercise(id), api.exerciseUsage(id)])
      exercise = e
      usage = u
      name = e.name
      equipmentType = e.equipmentType
      laterality = e.laterality
    } catch (e) {
      error = errorMessage(e)
    }
  }
  load()

  $effect(() => {
    api.progression(id, unit).then((p) => (points = p)).catch((e) => (error = errorMessage(e)))
  })

  async function save(event: SubmitEvent) {
    event.preventDefault()
    try {
      exercise = await api.updateExercise(id, { name, equipmentType, laterality })
      message = 'Saved.'
      error = ''
    } catch (e) {
      error = errorMessage(e)
      message = ''
    }
  }

  async function softDelete() {
    if (!(await confirmAction(`Delete "${exercise!.name}"? Its history stays and it can be restored.`, 'Delete', true))) return
    try {
      await api.deleteExercise(id)
      go('exercises')
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function restore() {
    try {
      exercise = await api.restoreExercise(id)
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function erase() {
    const u = await api.exerciseUsage(id)
    const ok = await confirmAction(
      `Erase "${exercise!.name}" with ${u.sets} logged sets in ${u.sessions} sessions? A backup is taken first. This cannot be undone here.`,
      'Erase permanently',
      true,
    )
    if (!ok) return
    try {
      const r = await api.deleteExercisePermanently(id)
      alertAndLeave(r.backupFile)
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function alertAndLeave(backupFile: string) {
    await confirmAction(`Erased. Backup saved as ${backupFile}.`, 'OK')
    go('exercises')
  }
</script>

<a href="#/exercises" class="mb-3 inline-flex min-h-11 items-center gap-1 font-display text-sm tracking-[0.08em] text-dim uppercase"><Icon name="back" size={18} />Exercises</a>
<ErrorText message={error} />

{#if exercise}
  <div class="mb-4 flex items-center gap-3">
    <Badge initials={exercise.initials} deleted={exercise.deleted} />
    <h1 class="text-3xl leading-none">{exercise.name}</h1>
    {#if exercise.deleted}<span class="border border-edge px-2 font-display text-xs tracking-[0.1em] text-dim uppercase">deleted</span>{/if}
  </div>

  <section class="card mb-4 p-3">
    <div class="mb-2 flex items-center justify-between">
      <h2 class="font-medium">Progress</h2>
      <select class="input w-20" aria-label="Chart unit" bind:value={unit}>
        <option value="KG">kg</option>
        <option value="LB">lb</option>
      </select>
    </div>
    <ProgressChart {points} {unit} />
  </section>

  {#if exercise.deleted}
    <div class="flex gap-2">
      <button type="button" class="btn btn-primary flex-1" onclick={restore}>Restore</button>
      <button type="button" class="btn btn-danger flex-1" onclick={erase}>Erase permanently</button>
    </div>
  {:else}
    <form class="card space-y-2 p-3" onsubmit={save}>
      <h2 class="font-medium">Edit</h2>
      <input class="input" aria-label="Name" bind:value={name} required />
      <select class="input" aria-label="Equipment type" bind:value={equipmentType} disabled={(usage?.sets ?? 0) > 0}>
        {#each EQUIPMENT_TYPES as t}<option value={t}>{typeLabel(t)}</option>{/each}
      </select>
      {#if (usage?.sets ?? 0) > 0}
        <p class="text-xs text-dim">The type is fixed once sets are logged ({usage?.sets} {usage?.sets === 1 ? 'set' : 'sets'}).</p>
      {/if}
      <select class="input" aria-label="Laterality" bind:value={laterality}>
        <option value="BILATERAL">Bilateral</option>
        <option value="UNILATERAL">Unilateral</option>
      </select>
      {#if message}<p class="text-sm text-ink">{message}</p>{/if}
      <button class="btn btn-primary w-full" type="submit">Save</button>
    </form>
    <button type="button" class="btn btn-danger mt-3 w-full" onclick={softDelete}>Delete exercise</button>
  {/if}
{/if}
