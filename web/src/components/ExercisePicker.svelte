<script lang="ts">
  import Icon from './Icon.svelte'
  import { api, errorMessage } from '../lib/api'
  import { typeLabel } from '../lib/format'
  import { EQUIPMENT_TYPES, type EquipmentType, type Exercise, type Laterality } from '../lib/types'
  import Badge from './Badge.svelte'
  import ErrorText from './ErrorText.svelte'

  let { onPick, exclude = [] }: { onPick: (e: Exercise) => void; exclude?: string[] } = $props()

  let query = $state('')
  let types = $state<EquipmentType[]>([])
  let list = $state<Exercise[]>([])
  let loaded = $state(false)
  let error = $state('')
  let adding = $state(false)
  let name = $state('')
  let equipmentType = $state<EquipmentType>('BARBELL')
  let laterality = $state<Laterality>('BILATERAL')
  let latest = 0

  async function load() {
    const seq = ++latest
    try {
      const result = await api.listExercises(query.trim(), types)
      if (seq !== latest) return
      list = result
      loaded = true
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }

  $effect(() => {
    query
    types
    load()
  })

  function toggle(t: EquipmentType) {
    types = types.includes(t) ? types.filter((x) => x !== t) : [...types, t]
  }

  async function add(event: SubmitEvent) {
    event.preventDefault()
    try {
      onPick(await api.createExercise({ name, equipmentType, laterality }))
    } catch (e) {
      error = errorMessage(e) // a soft-deleted twin is handled on the Exercises screen
    }
  }
</script>

<div class="space-y-3">
  <input class="input" type="search" placeholder="Search exercises" aria-label="Search exercises" bind:value={query} />
  <div class="flex flex-wrap gap-2">
    {#each EQUIPMENT_TYPES as t}
      <button
        type="button"
        class="min-h-11 border px-3 font-display text-[13px] tracking-[0.08em] uppercase {types.includes(t) ? 'border-ink bg-ink text-ground' : 'border-edge text-dim'}"
        aria-pressed={types.includes(t)}
        onclick={() => toggle(t)}>{typeLabel(t)}</button
      >
    {/each}
  </div>
  <ErrorText message={error} />

  <ul class="card divide-y">
    {#each list.filter((e) => !exclude.includes(e.id)) as ex (ex.id)}
      <li>
        <button type="button" class="flex min-h-14 w-full items-center gap-3 p-2 text-left" onclick={() => onPick(ex)}>
          <Badge initials={ex.initials} />
          <span class="flex-1">
            <span class="block font-medium">{ex.name}</span>
            <span class="block text-sm text-dim">{typeLabel(ex.equipmentType)}</span>
          </span>
        </button>
      </li>
    {:else}
      {#if loaded}
        <li class="p-3 text-dim">
          {query || types.length ? 'No exercise matches.' : 'Your library is empty. Add your first exercise below.'}
        </li>
      {/if}
    {/each}
  </ul>

  {#if adding}
    <form class="card space-y-2 p-3" onsubmit={add}>
      <h3 class="font-medium">Add exercise</h3>
      <input class="input" placeholder="Name" aria-label="Name" bind:value={name} required />
      <select class="input" aria-label="Equipment type" bind:value={equipmentType}>
        {#each EQUIPMENT_TYPES as t}<option value={t}>{typeLabel(t)}</option>{/each}
      </select>
      <select class="input" aria-label="Laterality" bind:value={laterality}>
        <option value="BILATERAL">Bilateral</option>
        <option value="UNILATERAL">Unilateral</option>
      </select>
      <div class="flex gap-2">
        <button class="btn btn-primary flex-1" type="submit">Add and use</button>
        <button class="btn" type="button" onclick={() => (adding = false)}>Cancel</button>
      </div>
    </form>
  {:else}
    <button type="button" class="btn w-full" onclick={() => ((adding = true), (name = query))}><Icon name="plus" size={18} />Add exercise</button>
  {/if}
</div>
