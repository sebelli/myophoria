<script lang="ts">
  import Icon from '../components/Icon.svelte'
  import Badge from '../components/Badge.svelte'
  import ErrorText from '../components/ErrorText.svelte'
  import ExercisePicker from '../components/ExercisePicker.svelte'
  import { api, errorMessage } from '../lib/api'
  import { confirmAction } from '../lib/dialog.svelte'
  import { go } from '../lib/router.svelte'
  import type { Exercise, Template } from '../lib/types'

  let templates = $state<Template[]>([])
  let error = $state('')
  // builder: editingId null = closed, '' = new template
  let editingId = $state<string | null>(null)
  let name = $state('')
  let chosen = $state<Exercise[]>([])
  let picking = $state(false)

  async function load() {
    try {
      templates = await api.listTemplates()
    } catch (e) {
      error = errorMessage(e)
    }
  }
  load()

  function open(t: Template | null) {
    editingId = t?.id ?? ''
    name = t?.name ?? ''
    chosen = t ? [...t.exercises] : []
    picking = false
  }

  function move(i: number, by: number) {
    const j = i + by
    if (j < 0 || j >= chosen.length) return
    const copy = [...chosen]
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
    chosen = copy
  }

  async function save(event: SubmitEvent) {
    event.preventDefault()
    const ids = chosen.map((e) => e.id)
    try {
      if (editingId) await api.updateTemplate(editingId, name, ids)
      else await api.createTemplate(name, ids)
      editingId = null
      error = ''
      await load()
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function remove(t: Template) {
    if (!(await confirmAction(`Delete template "${t.name}"?`, 'Delete', true))) return
    try {
      await api.deleteTemplate(t.id)
      await load()
    } catch (e) {
      error = errorMessage(e)
    }
  }

  function start(t: Template) {
    try {
      sessionStorage.setItem('myophoria.startTemplate', t.id)
    } catch {
      // storage blocked: the start screen just opens without the template preselected
    }
    go('workout')
  }
</script>

<a href="#/more" class="mb-3 inline-flex min-h-11 items-center gap-1 font-display text-sm tracking-[0.08em] text-dim uppercase"><Icon name="back" size={18} />More</a>
<div class="mb-4 flex items-center justify-between">
  <h1 class="text-3xl leading-none">Templates</h1>
  {#if editingId === null}<button type="button" class="btn" onclick={() => open(null)}><Icon name="plus" size={18} />New</button>{/if}
</div>
<ErrorText message={error} />

{#if editingId !== null}
  <form class="card mb-4 space-y-3 p-3" onsubmit={save}>
    <h2 class="font-medium">{editingId ? 'Edit template' : 'New template'}</h2>
    <input class="input" placeholder="Name" aria-label="Template name" bind:value={name} required />
    <ol class="divide-y rounded border">
      {#each chosen as ex, i (ex.id)}
        <li class="flex items-center gap-2 p-2">
          <Badge initials={ex.initials} />
          <span class="flex-1">{ex.name}</span>
          <button type="button" class="btn px-3" aria-label="Move {ex.name} up" disabled={i === 0} onclick={() => move(i, -1)}>↑</button>
          <button type="button" class="btn px-3" aria-label="Move {ex.name} down" disabled={i === chosen.length - 1} onclick={() => move(i, 1)}>↓</button>
          <button type="button" class="btn px-3 text-dim" aria-label="Remove {ex.name}" onclick={() => (chosen = chosen.filter((x) => x.id !== ex.id))}><Icon name="close" /></button>
        </li>
      {:else}
        <li class="p-2 text-sm text-dim">No exercises yet.</li>
      {/each}
    </ol>
    {#if picking}
      <ExercisePicker exclude={chosen.map((e) => e.id)} onPick={(e) => ((chosen = [...chosen, e]), (picking = false))} />
    {:else}
      <button type="button" class="btn w-full" onclick={() => (picking = true)}><Icon name="plus" size={18} />Add exercise</button>
    {/if}
    <div class="flex gap-2">
      <button class="btn btn-primary flex-1" type="submit">Save template</button>
      <button class="btn" type="button" onclick={() => (editingId = null)}>Cancel</button>
    </div>
  </form>
{/if}

<ul class="space-y-3">
  {#each templates as t (t.id)}
    <li class="card p-3">
      <div class="mb-2 flex items-center justify-between gap-2">
        <h2 class="font-medium">{t.name}</h2>
        <span class="text-sm text-dim">{t.exercises.length} exercises</span>
      </div>
      <p class="mb-3 text-sm text-dim">{t.exercises.map((e) => e.name).join(' · ') || 'All its exercises are deleted.'}</p>
      <div class="flex gap-2">
        <button type="button" class="btn btn-primary flex-1" onclick={() => start(t)}>Start</button>
        <button type="button" class="btn" onclick={() => open(t)}>Edit</button>
        <button type="button" class="btn text-dim" aria-label="Delete {t.name}" onclick={() => remove(t)}><Icon name="close" /></button>
      </div>
    </li>
  {:else}
    <li class="text-dim">No templates yet. Create one here or use "Save as template" after a workout.</li>
  {/each}
</ul>
