<script lang="ts">
  import ErrorText from './ErrorText.svelte'
  import Icon from './Icon.svelte'
  import { api, errorMessage } from '../lib/api'
  import { nextSetType, round2, setLabels, unitLabel } from '../lib/format'
  import type { SetInput, Unit } from '../lib/types'
  import { blankRow, newKey, openCard, workout, type Card, type Row } from '../lib/workout.svelte'

  // one exercise of the running workout, shown inline under its row on the Workout screen
  let { exerciseId }: { exerciseId: string } = $props()
  let card = $state<Card | null>(null)
  let error = $state('')

  async function init() {
    try {
      await openCard(exerciseId)
      card = workout.cards[exerciseId] // the proxied object, so edits stay in the shared store
    } catch (e) {
      error = errorMessage(e)
    }
  }
  init()

  const labels = $derived(card ? setLabels(card.rows.map((r) => r.setType)) : [])
  const calisthenics = $derived(card?.exercise.equipmentType === 'CALISTHENICS')
  const nextKey = $derived(card?.rows.find((r) => !r.setId)?.key) // the next set to tick is marked red

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

  // ticked cells read as plain numbers (still editable); the next set's cells are outlined red
  function cellClass(row: Row) {
    if (row.setId) return 'border-transparent bg-transparent focus:border-ink'
    return row.key === nextKey ? 'border-live text-ink' : ''
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

<ErrorText message={error} />

{#if card}
  {#if calisthenics}
    <label class="mb-2 flex items-center gap-2 text-sm">
      <span class="font-display text-xs tracking-[0.1em] text-dim uppercase">Bodyweight</span>
      <input
        class="input w-24 {card.bodyweight ? '' : 'border-live'}"
        inputmode="decimal"
        aria-label="Bodyweight"
        placeholder="needed"
        bind:value={card.bodyweight}
      />
      <span>{unitLabel(card.unit)}</span>
    </label>
  {/if}

  <table class="w-full text-center">
    <thead class="font-display text-xs tracking-[0.1em] text-dim uppercase">
      <tr>
        <th class="w-12 py-1 font-normal">Set</th>
        <th class="py-1 font-normal">
          <select
            class="min-h-11 bg-transparent tracking-[0.1em] uppercase"
            aria-label="Weight unit"
            value={card.unit}
            onchange={(e) => switchUnit(e.currentTarget.value as Unit)}
          >
            <option value="KG">KG</option>
            <option value="LB">LB</option>
          </select>
          <span class="block text-[11px] tracking-normal text-faint normal-case">{card.exercise.weightLabel}</span>
        </th>
        <th class="py-1 font-normal">Reps</th>
        <th class="w-12 py-1 font-normal">Done</th>
        <th class="w-10"><span class="sr-only">Remove</span></th>
      </tr>
    </thead>
    <tbody>
      {#each card.rows as row, i (row.key)}
        <tr class="border-t {row.setId ? '' : 'text-dim'}">
          <td class="py-1">
            <button
              type="button"
              class="min-h-11 w-12 font-display text-lg font-semibold {row.setType === 'FAILURE' ? 'text-live' : row.setType === 'WARMUP' ? 'text-dim' : row.key === nextKey ? 'text-ink' : ''}"
              aria-label="Set type {labels[i]}, tap to change"
              onclick={() => cycle(row, i)}>{labels[i]}</button
            >
          </td>
          <td class="px-1 py-1">
            <input
              class="input text-center font-display text-xl {cellClass(row)}"
              inputmode="decimal"
              aria-label="Weight set {labels[i]}"
              bind:value={row.weight}
              onchange={() => changed(row, i)}
            />
            {#if row.unit !== card.unit}<span class="text-xs">{unitLabel(row.unit)}</span>{/if}
          </td>
          <td class="px-1 py-1">
            <input
              class="input text-center font-display text-xl {cellClass(row)}"
              inputmode="numeric"
              aria-label="Reps set {labels[i]}"
              bind:value={row.reps}
              onchange={() => changed(row, i)}
            />
          </td>
          <td class="py-1">
            <input
              type="checkbox"
              class="tick mx-auto"
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
              <button type="button" class="min-h-11 w-10 text-faint" aria-label="Remove draft row" onclick={() => removeDraft(row)}><Icon name="close" /></button>
            {/if}
          </td>
        </tr>
        {#if row.error}
          <tr><td colspan="5" class="pb-2 text-left text-sm text-live"><span role="alert">{row.error}</span></td></tr>
        {/if}
      {/each}
    </tbody>
  </table>

  <button type="button" class="btn mt-2 w-full text-dim" onclick={addRow}><Icon name="plus" size={18} />Add set</button>
{/if}
