<script lang="ts">
  import ErrorText from '../components/ErrorText.svelte'
  import { api, errorMessage } from '../lib/api'
  import { confirmAction } from '../lib/dialog.svelte'
  import type { Gym } from '../lib/types'

  let gyms = $state<Gym[]>([])
  let error = $state('')
  let name = $state('')
  let renaming = $state<string | null>(null)
  let newName = $state('')

  async function load() {
    try {
      gyms = await api.listGyms()
    } catch (e) {
      error = errorMessage(e)
    }
  }
  load()

  async function run(action: () => Promise<unknown>) {
    try {
      await action()
      error = ''
      await load()
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function add(event: SubmitEvent) {
    event.preventDefault()
    await run(() => api.createGym(name))
    if (!error) name = ''
  }

  async function remove(g: Gym) {
    if (await confirmAction(`Delete "${g.name}"? Its workouts stay in History.`, 'Delete', true)) await run(() => api.deleteGym(g.id))
  }
</script>

<a href="#/more" class="mb-3 inline-flex min-h-11 items-center text-sm text-slate-600">← More</a>
<h1 class="mb-4 text-xl font-semibold">Gyms</h1>
<ErrorText message={error} />

<form class="card mb-4 flex gap-2 p-3" onsubmit={add}>
  <input class="input" placeholder="New gym" aria-label="New gym name" bind:value={name} required />
  <button class="btn btn-primary shrink-0" type="submit">Add</button>
</form>

<ul class="card divide-y">
  {#each gyms as g (g.id)}
    <li class="p-3">
      {#if renaming === g.id}
        <form
          class="flex gap-2"
          onsubmit={async (e) => {
            e.preventDefault()
            await run(() => api.renameGym(g.id, newName))
            if (!error) renaming = null
          }}
        >
          <input class="input" aria-label="Gym name" bind:value={newName} required />
          <button class="btn btn-primary" type="submit">Save</button>
          <button class="btn" type="button" onclick={() => (renaming = null)}>Cancel</button>
        </form>
      {:else}
        <div class="flex flex-wrap items-center gap-2">
          <span class="flex-1 font-medium">{g.name}</span>
          {#if g.isDefault}
            <span class="rounded bg-slate-200 px-2 text-xs">default</span>
          {:else}
            <button type="button" class="btn" onclick={() => run(() => api.makeDefaultGym(g.id))}>Make default</button>
          {/if}
          <button type="button" class="btn" onclick={() => ((renaming = g.id), (newName = g.name))}>Rename</button>
          {#if !g.isDefault}
            <button type="button" class="btn text-red-700" aria-label="Delete {g.name}" onclick={() => remove(g)}>×</button>
          {/if}
        </div>
      {/if}
    </li>
  {/each}
</ul>
