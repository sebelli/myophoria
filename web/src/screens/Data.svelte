<script lang="ts">
  import Icon from '../components/Icon.svelte'
  import ErrorText from '../components/ErrorText.svelte'
  import { api, errorMessage } from '../lib/api'
  import type { ImportReport } from '../lib/types'

  let error = $state('')
  let backupFile = $state('')
  let file = $state<File | null>(null)
  let report = $state<ImportReport | null>(null)
  let busy = $state(false)

  async function backup() {
    try {
      backupFile = (await api.backup()).file
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }

  async function importFile(event: SubmitEvent) {
    event.preventDefault()
    if (!file) return
    busy = true
    try {
      report = await api.importZip(file)
      error = ''
    } catch (e) {
      error = errorMessage(e)
      report = null
    } finally {
      busy = false
    }
  }

  function notes(r: ImportReport['files'][string]) {
    return [
      ...r.skipped.map((m) => ['Skipped', m]),
      ...r.orphans.map((m) => ['Orphan', m]),
      ...r.nameConflicts.map((m) => ['Name conflict', m]),
      ...r.adjusted.map((m) => ['Adjusted', m]),
    ]
  }
</script>

<a href="#/more" class="mb-3 inline-flex min-h-11 items-center gap-1 font-display text-sm tracking-[0.08em] text-dim uppercase"><Icon name="back" size={18} />More</a>
<h1 class="mb-4 text-3xl leading-none">Data</h1>
<ErrorText message={error} />

<section class="card mb-4 space-y-2 p-3">
  <h2 class="font-medium">Export</h2>
  <p class="text-sm text-dim">Everything as a ZIP of CSV files (opens in any spreadsheet).</p>
  <a class="btn w-full" href="/api/v1/export" download>Download export</a>
</section>

<section class="card mb-4 space-y-2 p-3">
  <h2 class="font-medium">Backup</h2>
  <p class="text-sm text-dim">A copy of the database in the data folder's backups.</p>
  <button type="button" class="btn w-full" onclick={backup}>Back up now</button>
  {#if backupFile}<p class="text-sm text-ink">Saved {backupFile}</p>{/if}
</section>

<form class="card mb-4 space-y-2 p-3" onsubmit={importFile}>
  <h2 class="font-medium">Import</h2>
  <p class="text-sm text-dim">Adds rows from a Myophoria export. Existing rows are never overwritten; a backup is taken first.</p>
  <input class="input py-2" type="file" accept=".zip,application/zip" aria-label="Export ZIP" onchange={(e) => (file = e.currentTarget.files?.[0] ?? null)} />
  <button class="btn btn-primary w-full" type="submit" disabled={!file || busy}>{busy ? 'Importing…' : 'Import'}</button>
</form>

{#if report}
  <section class="card p-3">
    <h2 class="mb-2 font-medium">Import report</h2>
    <table class="mb-2 w-full text-sm">
      <thead class="text-dim"><tr><th class="text-left font-medium">File</th><th class="font-medium">New</th><th class="font-medium">Already there</th></tr></thead>
      <tbody>
        {#each Object.entries(report.files) as [name, r]}
          <tr><td>{name}</td><td class="text-center">{r.imported}</td><td class="text-center">{r.alreadyPresent}</td></tr>
        {/each}
      </tbody>
    </table>
    <ul class="space-y-1 text-sm">
      {#each Object.values(report.files).flatMap(notes) as [kind, text]}
        <li><span class="font-medium">{kind}:</span> {text}</li>
      {/each}
    </ul>
  </section>
{/if}
