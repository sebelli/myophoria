<script lang="ts">
  import Badge from '../components/Badge.svelte'
  import ErrorText from '../components/ErrorText.svelte'
  import { api, errorMessage } from '../lib/api'
  import { nextSetType, round2, setLabels, unitLabel } from '../lib/format'
  import { go, route } from '../lib/router.svelte'
  import type { SetInput, Unit } from '../lib/types'
  import { blankRow, loadOpen, newKey, openCard, workout, type Card, type Row } from '../lib/workout.svelte'

  const exerciseId = route.parts[1]
  let card = $state<Card | null>(null)
  let error = $state('')

  async function init() {
    try {
      if (!workout.session && !(await loadOpen())) return go('workout')
      await openCard(exerciseId)
      card = workout.cards[exerciseId] // the proxied object, so edits stay in the shared store
    } catch (e) {
      error = errorMessage(e)
    }
  }
  init()

  const labels = $derived(card ? setLabels(card.rows.map((r) => r.setType)) : [])
  const calisthenics = $derived(card?.exercise.equipmentType === 'CALISTHENICS')

  function input(row: Row, index: number): SetInput | string {
    const weight = Number(row.weight)
    const reps = Number(row.reps)
    if (row.weight === '' || !Number.isFinite(weight)) return 'Enter a weight'
    if (!Number.isInteger(reps) || reps < 1) return 'Enter the reps'
    let bodyweight: number | null = null
    if (calisthenics) {
      bodyweight = Number(card!.bodyweight)
      if (!card!.bodyweight || !(bodyweight > 0)) return 'Enter your bodyweight first'
      // the chip is in the card unit; a ticked row in another unit gets it converted
      if (row.unit !== card!.unit) bodyweight = round2(convert(bodyweight, card!.unit, row.unit))
    }
    return { exerciseId, setType: row.setType, weight, unit: row.unit, reps, bodyweight, position: index + 1 }
  }

  async function toggle(row: Row, index: number) {
    row.error = ''
    row.busy = true
    try {
      if (row.setId) {
        await api.deleteSet(row.setId)
        workout.session!.sets = workout.session!.sets.filter((s) => s.id !== row.setId)
        row.setId = null
      } else {
        const body = input(row, index)
        if (typeof body === 'string') {
          row.error = body
          return
        }
        const saved = await api.addSet(workout.session!.id, body)
        workout.session!.sets.push(saved)
        row.setId = saved.id
      }
    } catch (e) {
      row.error = errorMessage(e) // the row stays as it was so it can be retried (handoff §8.6)
    } finally {
      row.busy = false
    }
  }

  // editing a ticked row updates the saved set
  async function changed(row: Row, index: number) {
    if (!row.setId) return
    const body = input(row, index)
    if (typeof body === 'string') {
      row.error = body
      return
    }
    row.busy = true
    try {
      const saved = await api.updateSet(row.setId, body)
      workout.session!.sets = workout.session!.sets.map((s) => (s.id === saved.id ? saved : s))
      row.error = ''
    } catch (e) {
      row.error = errorMessage(e)
    } finally {
      row.busy = false
    }
  }

  function cycle(row: Row, index: number) {
    row.setType = nextSetType(row.setType)
    if (row.setType === 'WARMUP' && !row.setId && card!.warmupWeight != null) {
      row.weight = String(round2(convert(card!.warmupWeight, card!.unit, row.unit)))
    }
    changed(row, index)
  }

  function addRow() {
    const above = card!.rows.at(-1)
    card!.rows.push(
      above
        ? { ...blankRow(card!.unit), setType: above.setType, weight: above.unit === card!.unit ? above.weight : '', reps: above.reps }
        : blankRow(card!.unit),
    )
  }

  function removeDraft(row: Row) {
    card!.rows = card!.rows.filter((r) => r.key !== row.key)
    if (card!.rows.length === 0) card!.rows.push({ ...blankRow(card!.unit), key: newKey() })
  }

  const KG_PER_LB = 0.45359237
  function convert(value: number, from: Unit, to: Unit) {
    if (from === to) return value
    return from === 'KG' ? value / KG_PER_LB : value * KG_PER_LB
  }

  // unit dropdown: drafts and the bodyweight chip are converted, ticked sets keep their unit
  function switchUnit(to: Unit) {
    const from = card!.unit
    if (from === to) return
    for (const r of card!.rows) {
      if (!r.setId) {
        if (r.weight !== '' && Number.isFinite(Number(r.weight))) r.weight = String(round2(convert(Number(r.weight), from, to)))
        r.unit = to
      }
    }
    if (card!.bodyweight) card!.bodyweight = String(round2(convert(Number(card!.bodyweight), from, to)))
    card!.unit = to
  }
</script>

<a href="#/workout" class="mb-3 inline-flex min-h-11 items-center text-sm text-slate-600">← Workout</a>
<ErrorText message={error} />

{#if card}
  <div class="mb-3 flex items-center gap-3">
    <Badge initials={card.exercise.initials} />
    <h1 class="text-xl font-semibold">{card.exercise.name}</h1>
  </div>

  {#if calisthenics}
    <label class="mb-3 flex items-center gap-2 text-sm">
      <span class="text-slate-600">Bodyweight</span>
      <input
        class="input w-24 {card.bodyweight ? '' : 'border-amber-500'}"
        inputmode="decimal"
        aria-label="Bodyweight"
        placeholder="needed"
        bind:value={card.bodyweight}
      />
      <span>{unitLabel(card.unit)}</span>
    </label>
  {/if}

  <table class="w-full text-center">
    <thead class="text-sm text-slate-600">
      <tr>
        <th class="w-14 py-1 font-medium">Set</th>
        <th class="py-1 font-medium">
          <select
            class="min-h-11 rounded bg-transparent font-medium"
            aria-label="Weight unit"
            value={card.unit}
            onchange={(e) => switchUnit(e.currentTarget.value as Unit)}
          >
            <option value="KG">KG</option>
            <option value="LB">LB</option>
          </select>
          <span class="block text-xs font-normal">{card.exercise.weightLabel}</span>
        </th>
        <th class="py-1 font-medium">Reps</th>
        <th class="w-14 py-1 font-medium">Done</th>
        <th class="w-10"><span class="sr-only">Remove</span></th>
      </tr>
    </thead>
    <tbody>
      {#each card.rows as row, i (row.key)}
        <tr class={row.setId ? 'bg-green-50' : 'text-slate-500'}>
          <td class="py-1">
            <button
              type="button"
              class="min-h-11 w-12 rounded font-semibold {row.setType === 'WARMUP' ? 'text-amber-600' : row.setType === 'FAILURE' ? 'text-red-600' : ''}"
              aria-label="Set type {labels[i]}, tap to change"
              onclick={() => cycle(row, i)}>{labels[i]}</button
            >
          </td>
          <td class="px-1 py-1">
            <input
              class="input text-center"
              inputmode="decimal"
              aria-label="Weight set {labels[i]}"
              bind:value={row.weight}
              onchange={() => changed(row, i)}
            />
            {#if row.unit !== card.unit}<span class="text-xs">{unitLabel(row.unit)}</span>{/if}
          </td>
          <td class="px-1 py-1">
            <input
              class="input text-center"
              inputmode="numeric"
              aria-label="Reps set {labels[i]}"
              bind:value={row.reps}
              onchange={() => changed(row, i)}
            />
          </td>
          <td class="py-1">
            <input
              type="checkbox"
              class="h-7 w-7 accent-green-700"
              aria-label="Set {labels[i]} done"
              checked={row.setId != null}
              disabled={row.busy}
              onclick={(e) => {
                e.preventDefault() // the box follows the saved state, not the click
                toggle(row, i)
              }}
            />
          </td>
          <td>
            {#if !row.setId}
              <button type="button" class="min-h-11 w-10 text-slate-400" aria-label="Remove draft row" onclick={() => removeDraft(row)}>×</button>
            {/if}
          </td>
        </tr>
        {#if row.error}
          <tr><td colspan="5" class="pb-2 text-left text-sm text-red-700"><span role="alert">{row.error}</span></td></tr>
        {/if}
      {/each}
    </tbody>
  </table>

  <button type="button" class="btn mt-3 w-full" onclick={addRow}>+ Add set</button>
  <a href="#/workout" class="btn btn-primary mt-3 w-full">Done with this exercise</a>
{/if}
