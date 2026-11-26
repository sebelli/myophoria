<script lang="ts">
  import Icon from '../components/Icon.svelte'
  import Badge from '../components/Badge.svelte'
  import Clock from '../components/Clock.svelte'
  import ErrorText from '../components/ErrorText.svelte'
  import ExercisePicker from '../components/ExercisePicker.svelte'
  import { api, errorMessage } from '../lib/api'
  import { longDate, time, today } from '../lib/format'
  import { go } from '../lib/router.svelte'
  import type { Exercise, Gym, Template } from '../lib/types'
  import { exerciseOrder, finishWorkout, loadOpen, startWorkout, workout } from '../lib/workout.svelte'

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
    <ul class="card mb-4 divide-y">
      {#each exerciseOrder() as id (id)}
        <li>
          <a href={'#/workout/' + id} class="flex min-h-14 items-center gap-3 p-2">
            {#if names[id]}<Badge initials={names[id].initials} />{/if}
            <span class="flex-1 font-medium">{names[id]?.name ?? '…'}</span>
            <span class="text-sm text-dim">{setCount(id)} sets</span>
          </a>
        </li>
      {/each}
    </ul>
  {/if}

  {#if workout.queue.length}
    <h2 class="mb-2 text-sm font-medium text-dim">Up next (template)</h2>
    <ul class="card mb-4 divide-y">
      {#each workout.queue as id (id)}
        <li>
          <a href={'#/workout/' + id} class="flex min-h-14 items-center gap-3 p-2">
            {#if names[id]}<Badge initials={names[id].initials} />{/if}
            <span class="flex-1">{names[id]?.name ?? '…'}</span>
          </a>
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
      <ExercisePicker onPick={(e) => go('workout/' + e.id)} />
    </div>
  {:else}
    <button type="button" class="btn mb-3 w-full" onclick={() => (picking = true)}><Icon name="plus" size={18} />Add exercise</button>
  {/if}
  <button type="button" class="btn btn-primary w-full" onclick={finish}>Finish workout</button>
{/if}
