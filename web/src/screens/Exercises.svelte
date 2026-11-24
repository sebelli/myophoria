<script lang="ts">
  import Badge from '../components/Badge.svelte'
  import ErrorText from '../components/ErrorText.svelte'
  import { api, ApiError, errorMessage } from '../lib/api'
  import { choose, confirmAction } from '../lib/dialog.svelte'
  import { typeLabel } from '../lib/format'
  import { go } from '../lib/router.svelte'
  import { EQUIPMENT_TYPES, type EquipmentType, type Exercise, type Laterality } from '../lib/types'

  let exercises = $state<Exercise[]>([])
  let query = $state('')
  let types = $state<EquipmentType[]>([])
  let showDeleted = $state(false)
  let loaded = $state(false)
  let error = $state('')
  let adding = $state(false)
  let name = $state('')
  let equipmentType = $state<EquipmentType>('BARBELL')
  let laterality = $state<Laterality>('BILATERAL')
  let latest = 0 // only the newest search may update the list (responses can arrive out of order)

  async function load() {
    const seq = ++latest
    try {
      const result = await api.listExercises(query.trim(), types, showDeleted)
      if (seq !== latest) return
      exercises = result
      loaded = true
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }

  $effect(() => {
    query
    types
    showDeleted
    load()
  })

  function toggle(t: EquipmentType) {
    types = types.includes(t) ? types.filter((x) => x !== t) : [...types, t]
  }

  async function add(event: SubmitEvent) {
    event.preventDefault()
    try {
      await api.createExercise({ name, equipmentType, laterality })
      name = ''
      adding = false
      await load()
    } catch (e) {
      if (e instanceof ApiError && e.details?.softDeleted && e.details.existingId) await resolveDeletedTwin(e.details.existingId)
      else error = errorMessage(e)
    }
  }

  // handoff §7.3: a deleted exercise has this name → restore it (same type only), rename, or erase the old one
  async function resolveDeletedTwin(oldId: string) {
    const old = await api.getExercise(oldId)
    const options: { label: string; value: string; danger?: boolean }[] = []
    if (old.equipmentType === equipmentType) options.push({ label: 'Restore it', value: 'restore' })
    options.push({ label: 'Erase the old one', value: 'erase', danger: true })
    const answer = await choose(
      `A deleted exercise is already called "${old.name}" (${typeLabel(old.equipmentType)}).\nCancel to pick another name.`,
      options,
    )
    try {
      if (answer === 'restore') {
        await api.restoreExercise(oldId)
      } else if (answer === 'erase') {
        const usage = await api.exerciseUsage(oldId)
        const ok = await confirmAction(
          `This erases "${old.name}" with ${usage.sets} logged sets in ${usage.sessions} sessions. A backup is taken first.`,
          'Erase permanently',
          true,
        )
        if (!ok) return
        await api.deleteExercisePermanently(oldId)
        await api.createExercise({ name, equipmentType, laterality })
      } else {
        return
      }
      name = ''
      adding = false
      await load()
    } catch (e) {
      error = errorMessage(e)
    }
  }
</script>

<div class="mb-4 flex items-center justify-between">
  <h1 class="text-xl font-semibold">{showDeleted ? 'Deleted exercises' : 'Exercises'}</h1>
  <button type="button" class="btn" onclick={() => (showDeleted = !showDeleted)}>{showDeleted ? 'Show active' : 'Show deleted'}</button>
</div>

<div class="space-y-3">
  <ErrorText message={error} />
  <input class="input" type="search" placeholder="Search exercises" aria-label="Search exercises" bind:value={query} />
  <div class="flex flex-wrap gap-2">
    {#each EQUIPMENT_TYPES as t}
      <button
        type="button"
        class="min-h-11 rounded-full border px-3 text-sm {types.includes(t) ? 'border-slate-800 bg-slate-800 text-white' : 'border-slate-300 bg-white'}"
        aria-pressed={types.includes(t)}
        onclick={() => toggle(t)}>{typeLabel(t)}</button
      >
    {/each}
  </div>

  {#if !showDeleted}
    {#if adding}
      <form class="card space-y-2 p-3" onsubmit={add}>
        <h2 class="font-medium">Add exercise</h2>
        <input class="input" placeholder="Name" aria-label="Name" bind:value={name} required />
        <select class="input" aria-label="Equipment type" bind:value={equipmentType}>
          {#each EQUIPMENT_TYPES as t}<option value={t}>{typeLabel(t)}</option>{/each}
        </select>
        <select class="input" aria-label="Laterality" bind:value={laterality}>
          <option value="BILATERAL">Bilateral</option>
          <option value="UNILATERAL">Unilateral</option>
        </select>
        <div class="flex gap-2">
          <button class="btn btn-primary flex-1" type="submit">Add</button>
          <button class="btn" type="button" onclick={() => (adding = false)}>Cancel</button>
        </div>
      </form>
    {:else}
      <button type="button" class="btn w-full" onclick={() => (adding = true)}>+ Add exercise</button>
    {/if}
  {/if}

  <ul class="card divide-y">
    {#each exercises as ex (ex.id)}
      <li>
        <button type="button" class="flex min-h-14 w-full items-center gap-3 p-2 text-left" onclick={() => go('exercises/' + ex.id)}>
          <Badge initials={ex.initials} deleted={ex.deleted} />
          <span class="flex-1">
            <span class="block font-medium">{ex.name}</span>
            <span class="block text-sm text-slate-600">{typeLabel(ex.equipmentType)} · {ex.laterality.toLowerCase()}</span>
          </span>
        </button>
      </li>
    {:else}
      {#if loaded}
        <li class="p-3 text-slate-600">
          {showDeleted ? 'No deleted exercises.' : query || types.length ? 'No exercise matches.' : 'No exercises yet. Add your first one above.'}
        </li>
      {/if}
    {/each}
  </ul>
</div>
