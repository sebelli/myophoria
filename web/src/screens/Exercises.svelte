<script lang="ts">
  import { api, ApiError } from '../lib/api'
  import { EQUIPMENT_TYPES, type EquipmentType, type Exercise, type Laterality } from '../lib/types'

  let exercises = $state<Exercise[]>([])
  let query = $state('')
  let types = $state<EquipmentType[]>([])
  let error = $state('')

  let name = $state('')
  let equipmentType = $state<EquipmentType>('BARBELL')
  let laterality = $state<Laterality>('BILATERAL')

  const label = (t: string) => t.replace('_', ' ').toLowerCase()

  async function load() {
    try {
      exercises = await api.listExercises(query.trim(), types)
      error = ''
    } catch (e) {
      error = message(e)
    }
  }

  function message(e: unknown) {
    return e instanceof ApiError ? e.message : String(e)
  }

  function toggle(t: EquipmentType) {
    types = types.includes(t) ? types.filter((x) => x !== t) : [...types, t]
  }

  async function add(event: SubmitEvent) {
    event.preventDefault()
    try {
      await api.createExercise({ name, equipmentType, laterality })
      name = ''
      await load()
    } catch (e) {
      error = message(e)
    }
  }

  async function remove(ex: Exercise) {
    try {
      await api.deleteExercise(ex.id)
      await load()
    } catch (e) {
      error = message(e)
    }
  }

  // reload whenever the search text or type chips change
  $effect(() => {
    query
    types
    load()
  })
</script>

<section class="space-y-4">
  <h1 class="text-xl font-semibold">Exercises</h1>

  {#if error}
    <p class="rounded bg-red-100 p-2 text-red-800" role="alert">{error}</p>
  {/if}

  <input
    class="w-full rounded border p-2"
    type="search"
    placeholder="Search exercises"
    aria-label="Search exercises"
    bind:value={query}
  />

  <div class="flex flex-wrap gap-2">
    {#each EQUIPMENT_TYPES as t}
      <button
        type="button"
        class="rounded-full border px-3 py-1 text-sm {types.includes(t) ? 'bg-slate-800 text-white' : ''}"
        aria-pressed={types.includes(t)}
        onclick={() => toggle(t)}>{label(t)}</button
      >
    {/each}
  </div>

  <ul class="divide-y rounded border">
    {#each exercises as ex (ex.id)}
      <li class="flex items-center gap-3 p-2">
        <span class="flex h-10 w-10 items-center justify-center rounded-full bg-slate-200 text-sm font-semibold">{ex.initials}</span>
        <span class="flex-1">
          <span class="block font-medium">{ex.name}</span>
          <span class="block text-sm text-slate-600">{label(ex.equipmentType)} · {ex.laterality.toLowerCase()} · {ex.weightLabel}</span>
        </span>
        <button type="button" class="rounded border px-3 py-2 text-sm" onclick={() => remove(ex)}>Delete</button>
      </li>
    {:else}
      <li class="p-3 text-slate-600">No exercises yet. Add your first one below.</li>
    {/each}
  </ul>

  <form class="space-y-2 rounded border p-3" onsubmit={add}>
    <h2 class="font-medium">Add exercise</h2>
    <input class="w-full rounded border p-2" placeholder="Name" aria-label="Name" bind:value={name} required />
    <select class="w-full rounded border p-2" aria-label="Equipment type" bind:value={equipmentType}>
      {#each EQUIPMENT_TYPES as t}
        <option value={t}>{label(t)}</option>
      {/each}
    </select>
    <select class="w-full rounded border p-2" aria-label="Laterality" bind:value={laterality}>
      <option value="BILATERAL">bilateral</option>
      <option value="UNILATERAL">unilateral</option>
    </select>
    <button class="w-full rounded bg-slate-800 p-2 text-white" type="submit">Add</button>
  </form>
</section>
