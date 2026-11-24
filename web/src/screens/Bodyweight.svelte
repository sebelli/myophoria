<script lang="ts">
  import ErrorText from '../components/ErrorText.svelte'
  import { api, errorMessage } from '../lib/api'
  import { confirmAction } from '../lib/dialog.svelte'
  import { fromLocalInput, longDate, num, time, toLocalInput, unitLabel } from '../lib/format'
  import type { BodyweightEntry, Unit } from '../lib/types'

  let entries = $state<BodyweightEntry[]>([])
  let error = $state('')
  let weight = $state('')
  let unit = $state<Unit>('KG')
  let at = $state(toLocalInput(new Date().toISOString()))
  let editing = $state<string | null>(null)
  let editWeight = $state('')
  let editUnit = $state<Unit>('KG')
  let editAt = $state('')

  async function load() {
    try {
      entries = await api.listBodyweight()
      if (entries[0]) unit = entries[0].unit
    } catch (e) {
      error = errorMessage(e)
    }
  }
  load()

  // localDate comes from the client: the calendar day of the chosen local time
  function body(w: string, u: Unit, local: string) {
    return { weight: Number(w), unit: u, recordedAt: fromLocalInput(local)!, localDate: local.slice(0, 10) }
  }

  async function add(event: SubmitEvent) {
    event.preventDefault()
    try {
      await api.createBodyweight(body(weight, unit, at))
      weight = ''
      error = ''
      await load()
    } catch (e) {
      error = errorMessage(e)
    }
  }

  function edit(e: BodyweightEntry) {
    editing = e.id
    editWeight = String(e.weight)
    editUnit = e.unit
    editAt = toLocalInput(e.recordedAt)
  }

  async function save() {
    try {
      await api.updateBodyweight(editing!, body(editWeight, editUnit, editAt))
      editing = null
      error = ''
      await load()
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function remove(e: BodyweightEntry) {
    if (!(await confirmAction(`Delete ${num(e.weight)} ${unitLabel(e.unit)} from ${longDate(e.localDate)}?`, 'Delete', true))) return
    try {
      await api.deleteBodyweight(e.id)
      await load()
    } catch (err) {
      error = errorMessage(err)
    }
  }
</script>

<a href="#/more" class="mb-3 inline-flex min-h-11 items-center text-sm text-slate-600">← More</a>
<h1 class="mb-4 text-xl font-semibold">Bodyweight</h1>
<ErrorText message={error} />

<form class="card mb-4 space-y-2 p-3" onsubmit={add}>
  <div class="flex gap-2">
    <input class="input" inputmode="decimal" placeholder="Weight" aria-label="Weight" bind:value={weight} required />
    <select class="input w-24" aria-label="Unit" bind:value={unit}>
      <option value="KG">kg</option>
      <option value="LB">lb</option>
    </select>
  </div>
  <input class="input" type="datetime-local" aria-label="When" bind:value={at} required />
  <button class="btn btn-primary w-full" type="submit">Add</button>
</form>

<ul class="card divide-y">
  {#each entries as e (e.id)}
    <li class="p-3">
      {#if editing === e.id}
        <div class="space-y-2">
          <div class="flex gap-2">
            <input class="input" inputmode="decimal" aria-label="Weight" bind:value={editWeight} />
            <select class="input w-24" aria-label="Unit" bind:value={editUnit}>
              <option value="KG">kg</option>
              <option value="LB">lb</option>
            </select>
          </div>
          <input class="input" type="datetime-local" aria-label="When" bind:value={editAt} />
          <div class="flex gap-2">
            <button type="button" class="btn btn-primary flex-1" onclick={save}>Save</button>
            <button type="button" class="btn" onclick={() => (editing = null)}>Cancel</button>
          </div>
        </div>
      {:else}
        <div class="flex items-center gap-2">
          <span class="flex-1">
            <span class="block font-medium">{num(e.weight)} {unitLabel(e.unit)}</span>
            <span class="block text-sm text-slate-600">{longDate(e.localDate)} · {time(e.recordedAt)}</span>
          </span>
          <button type="button" class="btn" onclick={() => edit(e)}>Edit</button>
          <button type="button" class="btn text-red-700" aria-label="Delete entry" onclick={() => remove(e)}>×</button>
        </div>
      {/if}
    </li>
  {:else}
    <li class="p-3 text-slate-600">No entries yet.</li>
  {/each}
</ul>
