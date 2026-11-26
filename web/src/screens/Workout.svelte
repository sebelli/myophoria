<script lang="ts">
  import Icon from '../components/Icon.svelte'
  import Clock from '../components/Clock.svelte'
  import ErrorText from '../components/ErrorText.svelte'
  import ExerciseCard from '../components/ExerciseCard.svelte'
  import ExercisePicker from '../components/ExercisePicker.svelte'
  import { api, errorMessage } from '../lib/api'
  import { longDate, time, today } from '../lib/format'
  import type { Exercise, Gym, Template } from '../lib/types'
  import { exerciseOrder, finishWorkout, loadOpen, openCard, startWorkout, workout } from '../lib/workout.svelte'

  let loading = $state(true)
  let error = $state('')
  let gyms = $state<Gym[]>([])
  let gymId = $state('')
  let templates = $state<Template[]>([])
  let templateId = $state(readStartTemplate())

  // set by the Templates screen's "Start" button
  function readStartTemplate(): string {
    try {
      return sessionStorage.getItem('myophoria.startTemplate') ?? ''
    } catch {
      return ''
    }
  }
  let picking = $state(false)
  let names = $state<Record<string, Exercise>>({})

  async function init() {
    try {
      const open = await loadOpen()
      gyms = await api.listGyms()
      if (!open) {
        const [active, tpl] = await Promise.all([api.activeGym(), api.listTemplates()])
        gymId = active.id
        templates = tpl
      } else {
        await loadNames()
      }
    } catch (e) {
      error = errorMessage(e)
    }
    loading = false
  }
  init()

  async function loadNames() {
    const ids = [...exerciseOrder(), ...workout.queue]
    const missing = ids.filter((id) => !names[id])
    const loaded = await Promise.all(missing.map((id) => api.getExercise(id).catch(() => null)))
    for (const e of loaded) if (e) names[e.id] = e
  }

  async function start() {
    try {
      const queue = templates.find((t) => t.id === templateId)?.exercises.map((e) => e.id) ?? []
      try {
        sessionStorage.removeItem('myophoria.startTemplate')
      } catch {
        // storage blocked: nothing to clear
      }
      await startWorkout(gymId, today(), queue)
      await loadNames()
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }

  // one exercise open at a time; opening builds its card (prefill) first
  async function openExercise(id: string) {
    try {
      await openCard(id)
      await loadNames()
      workout.open = id
      picking = false
    } catch (e) {
      error = errorMessage(e)
    }
  }

  function toggle(id: string) {
    if (workout.open === id) workout.open = null
    else openExercise(id)
  }

  function ticked(id: string) {
    return workout.cards[id]?.rows.filter((r) => r.setId).length ?? 0
  }

  function setCount(exerciseId: string) {
    return workout.session?.sets.filter((s) => s.exerciseId === exerciseId).length ?? 0
  }

  async function finish() {
    try {
      await finishWorkout()
    } catch (e) {
      error = errorMessage(e)
    }
  }

  const gymName = $derived(gyms.find((g) => g.id === workout.session?.gymId)?.name)
</script>

{#if loading}
  <p class="text-dim">Loading…</p>
{:else if !workout.session}
  <h1 class="mb-4 text-3xl leading-none">Start workout</h1>
  <div class="card space-y-3 p-3">
    <label class="block">
      <span class="label">Gym</span>
      <select class="input" bind:value={gymId}>
        {#each gyms as g}<option value={g.id}>{g.name}{g.isDefault ? ' (default)' : ''}</option>{/each}
      </select>
    </label>
    {#if templates.length}
      <label class="block">
        <span class="label">Template (optional)</span>
        <select class="input" bind:value={templateId}>
          <option value="">Empty workout</option>
          {#each templates as t}<option value={t.id}>{t.name} · {t.exercises.length} exercises</option>{/each}
        </select>
      </label>
    {/if}
    <button type="button" class="btn btn-primary w-full" onclick={start}>Start</button>
  </div>
  <ErrorText message={error} />
{:else}
  <header class="-mx-4 -mt-4 mb-4 flex items-end justify-between gap-3 border-b px-4 pt-4 pb-3">
    <h1 class="sr-only">Workout</h1>
    <Clock since={workout.session.startedAt} class="text-6xl" />
    <p class="text-right text-[13px] leading-relaxed text-dim">
      {#if gymName}<span class="block text-[15px] font-semibold text-ink">{gymName}</span>{/if}
      {longDate(workout.session.localDate)} · from {time(workout.session.startedAt)}
    </p>
  </header>
  <ErrorText message={error} />

  {#if exerciseOrder().length}
    <ul class="mb-4 divide-y border-y">
      {#each exerciseOrder() as id (id)}
        {@const open = workout.open === id}
        {@const card = workout.cards[id]}
        <li>
          <button type="button" class="flex min-h-14 w-full items-center gap-3 text-left" aria-expanded={open} onclick={() => toggle(id)}>
            <span class="flex-1 font-display text-xl tracking-[0.04em] uppercase {open ? 'text-ink' : 'text-dim'}">{names[id]?.name ?? '…'}</span>
            {#if open && card}
              <span class="font-display text-base text-live" aria-label="{ticked(id)} of {card.rows.length} sets done">{ticked(id)}/{card.rows.length}</span>
            {:else}
              <span class="text-sm text-dim">{setCount(id)} {setCount(id) === 1 ? 'set' : 'sets'}</span>
            {/if}
            <span class="text-dim {open ? 'rotate-180' : ''}"><Icon name="chevron" /></span>
          </button>
          {#if open}<div class="pb-4"><ExerciseCard exerciseId={id} /></div>{/if}
        </li>
      {/each}
    </ul>
  {/if}

  {#if workout.queue.length}
    <h2 class="mb-2 text-xs tracking-[0.1em] text-dim">Up next</h2>
    <ul class="mb-4 divide-y border-y">
      {#each workout.queue as id (id)}
        <li>
          <button type="button" class="flex min-h-14 w-full items-center gap-3 text-left text-dim" onclick={() => openExercise(id)}>
            <span class="flex-1 font-display text-xl tracking-[0.04em] uppercase">{names[id]?.name ?? '…'}</span>
            <Icon name="plus" size={18} />
            <span class="sr-only">Start this exercise</span>
          </button>
        </li>
      {/each}
    </ul>
  {/if}

  {#if picking}
    <div class="mb-4">
      <div class="mb-2 flex items-center justify-between">
        <h2 class="font-medium">Pick an exercise</h2>
        <button type="button" class="btn" onclick={() => (picking = false)}>Close</button>
      </div>
      <ExercisePicker onPick={(e) => openExercise(e.id)} />
    </div>
  {:else}
    <button type="button" class="btn mb-3 w-full" onclick={() => (picking = true)}><Icon name="plus" size={18} />Add exercise</button>
  {/if}
  <button type="button" class="btn btn-primary w-full" onclick={finish}>Finish workout</button>
{/if}
