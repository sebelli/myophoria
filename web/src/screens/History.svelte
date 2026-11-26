<script lang="ts">
  import Icon from '../components/Icon.svelte'
  import ErrorText from '../components/ErrorText.svelte'
  import { api, errorMessage } from '../lib/api'
  import { fromLocalInput, longDate, time, today } from '../lib/format'
  import { go } from '../lib/router.svelte'
  import type { Exercise, Gym, SessionHeader } from '../lib/types'

  let from = $state('')
  let to = $state('')
  let gymId = $state('')
  let exerciseId = $state('')
  let sessions = $state<SessionHeader[]>([])
  let gyms = $state<Gym[]>([])
  let exercises = $state<Exercise[]>([])
  let loaded = $state(false)
  let error = $state('')
  let backfilling = $state(false)
  let bfGym = $state('')
  let bfDate = $state(today())
  let bfStart = $state('')
  let bfEnd = $state('')
  let latest = 0

  Promise.all([api.listGyms(), api.listExercises('', [])])
    .then(([g, e]) => {
      gyms = g
      exercises = e
      bfGym = g.find((x) => x.isDefault)?.id ?? g[0]?.id ?? ''
    })
    .catch((e) => (error = errorMessage(e)))

  $effect(() => {
    const filter = { from, to, gymId, exerciseId }
    const seq = ++latest
    api
      .history(filter)
      .then((s) => {
        if (seq !== latest) return
        sessions = s
        loaded = true
        error = ''
      })
      .catch((e) => (error = errorMessage(e)))
  })

  async function backfill(event: SubmitEvent) {
    event.preventDefault()
    try {
      const s = await api.backfill({ gymId: bfGym, localDate: bfDate, startedAt: fromLocalInput(bfStart), endedAt: fromLocalInput(bfEnd) })
      go('history/' + s.id)
    } catch (e) {
      error = errorMessage(e)
    }
  }
</script>

<div class="mb-4 flex items-center justify-between">
  <h1 class="text-3xl leading-none">History</h1>
  <button type="button" class="btn" onclick={() => (backfilling = !backfilling)}>{#if backfilling}Close{:else}<Icon name="plus" size={18} />Past workout{/if}</button>
</div>
<ErrorText message={error} />

{#if backfilling}
  <form class="card mb-4 space-y-2 p-3" onsubmit={backfill}>
    <h2 class="font-medium">Log a past workout</h2>
    <select class="input" aria-label="Gym" bind:value={bfGym}>
      {#each gyms as g}<option value={g.id}>{g.name}</option>{/each}
    </select>
    <label class="block"><span class="label">Date</span><input class="input" type="date" bind:value={bfDate} required /></label>
    <label class="block"><span class="label">Start (optional)</span><input class="input" type="datetime-local" bind:value={bfStart} /></label>
    <label class="block"><span class="label">End (optional)</span><input class="input" type="datetime-local" bind:value={bfEnd} /></label>
    <button class="btn btn-primary w-full" type="submit">Create and add sets</button>
  </form>
{/if}

<details class="card mb-4 p-3">
  <summary class="min-h-11 cursor-pointer content-center font-medium">Filters{from || to || gymId || exerciseId ? ' (active)' : ''}</summary>
  <div class="mt-2 grid grid-cols-2 gap-2">
    <label><span class="label">From</span><input class="input" type="date" bind:value={from} /></label>
    <label><span class="label">To</span><input class="input" type="date" bind:value={to} /></label>
    <label class="col-span-2"
      ><span class="label">Gym</span>
      <select class="input" bind:value={gymId}>
        <option value="">All gyms</option>
        {#each gyms as g}<option value={g.id}>{g.name}</option>{/each}
      </select>
    </label>
    <label class="col-span-2"
      ><span class="label">Exercise</span>
      <select class="input" bind:value={exerciseId}>
        <option value="">All exercises</option>
        {#each exercises as e}<option value={e.id}>{e.name}</option>{/each}
      </select>
    </label>
    <button type="button" class="btn col-span-2" onclick={() => ((from = ''), (to = ''), (gymId = ''), (exerciseId = ''))}>Clear filters</button>
  </div>
</details>

<ul class="card divide-y">
  {#each sessions as s (s.id)}
    <li>
      <a href={'#/history/' + s.id} class="block min-h-14 p-3">
        <span class="flex justify-between">
          <span class="font-medium">{longDate(s.localDate)}</span>
          {#if s.open}<span class="border border-live px-2 font-display text-xs tracking-[0.1em] text-live uppercase">running</span>{/if}
        </span>
        <span class="block text-sm text-dim">
          {s.gymName ?? 'Unknown gym'} · {s.setCount} sets{s.startedAt ? ' · ' + time(s.startedAt) : ''}{s.endedAt ? '–' + time(s.endedAt) : ''}
        </span>
      </a>
    </li>
  {:else}
    {#if loaded}<li class="p-3 text-dim">No workouts found.</li>{/if}
  {/each}
</ul>
