<script lang="ts">
  import ErrorText from '../components/ErrorText.svelte'
  import { api, errorMessage } from '../lib/api'
  import { duration, longDate, num, time, unitLabel } from '../lib/format'
  import { route } from '../lib/router.svelte'
  import type { SessionSummary, Unit } from '../lib/types'

  const sessionId = route.parts[1]
  let unit = $state<Unit>('KG')
  let summary = $state<SessionSummary | null>(null)
  let error = $state('')
  let templateName = $state('')
  let saved = $state('')

  $effect(() => {
    api.summary(sessionId, unit).then((s) => (summary = s)).catch((e) => (error = errorMessage(e)))
  })

  async function saveTemplate(event: SubmitEvent) {
    event.preventDefault()
    try {
      const t = await api.saveAsTemplate(sessionId, templateName)
      saved = `Saved as template "${t.name}".`
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }
</script>

<div class="mb-4 flex items-center justify-between">
  <h1 class="text-xl font-semibold">Summary</h1>
  <select class="input w-24" aria-label="Unit" bind:value={unit}>
    <option value="KG">kg</option>
    <option value="LB">lb</option>
  </select>
</div>
<ErrorText message={error} />

{#if summary}
  <p class="mb-3 text-sm text-slate-600">
    {longDate(summary.localDate)} · {time(summary.startedAt)}–{time(summary.endedAt)} · {duration(summary.durationSeconds)}
  </p>
  <dl class="mb-4 grid grid-cols-3 gap-2 text-center">
    <div class="card p-2"><dt class="text-xs text-slate-600">Sets</dt><dd class="text-lg font-semibold">{summary.totalSets}</dd></div>
    <div class="card p-2"><dt class="text-xs text-slate-600">Working sets</dt><dd class="text-lg font-semibold">{summary.workingSets}</dd></div>
    <div class="card p-2">
      <dt class="text-xs text-slate-600">Volume</dt>
      <dd class="text-lg font-semibold">{Math.round(summary.volume)} {unitLabel(summary.unit)}</dd>
    </div>
  </dl>

  <table class="card mb-4 w-full text-sm">
    <thead class="text-slate-600">
      <tr><th class="p-2 text-left font-medium">Exercise</th><th class="p-2 font-medium">Sets</th><th class="p-2 font-medium">Top</th><th class="p-2 font-medium">Avg</th></tr>
    </thead>
    <tbody class="divide-y">
      {#each summary.exercises as ex}
        <tr>
          <td class="p-2">{ex.name ?? 'Deleted exercise'}</td>
          <td class="p-2 text-center">{ex.workingSets}</td>
          <td class="p-2 text-center">{num(ex.topWeight)}</td>
          <td class="p-2 text-center">{num(ex.averageWeight)}</td>
        </tr>
      {/each}
    </tbody>
  </table>
  <p class="mb-4 text-xs text-slate-500">Top and average use working sets only, in {unitLabel(summary.unit)}.</p>

  {#if saved}
    <p class="mb-3 rounded bg-green-50 p-2 text-sm text-green-800">{saved}</p>
  {:else}
    <form class="card mb-4 flex gap-2 p-3" onsubmit={saveTemplate}>
      <input class="input" placeholder="Template name" aria-label="Template name" bind:value={templateName} required />
      <button type="submit" class="btn shrink-0">Save as template</button>
    </form>
  {/if}
{/if}
<a href="#/workout" class="btn btn-primary w-full">Done</a>
