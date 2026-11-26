<script lang="ts">
  import { closeDialog, dialog } from '../lib/dialog.svelte'

  let el: HTMLDialogElement
  $effect(() => {
    if (dialog.resolve && !el.open) el.showModal()
    if (!dialog.resolve && el.open) el.close()
  })
</script>

<dialog
  bind:this={el}
  class="m-auto w-[min(28rem,calc(100%-2rem))] border border-edge bg-field p-4 text-ink backdrop:bg-ground/80"
  oncancel={(e) => {
    e.preventDefault()
    closeDialog(null)
  }}
>
  <p class="whitespace-pre-line">{dialog.message}</p>
  {#if dialog.choices.length > 1}
    <!-- several answers: one full-width column, in the order given, Cancel last -->
    <div class="mt-4 flex flex-col gap-2">
      {#each dialog.choices as c}
        <button type="button" class={c.danger ? 'btn btn-danger' : 'btn btn-primary'} onclick={() => closeDialog(c.value)}>{c.label}</button>
      {/each}
      <button type="button" class="btn" onclick={() => closeDialog(null)}>Cancel</button>
    </div>
  {:else}
    <div class="mt-4 flex flex-wrap justify-end gap-2">
      <button type="button" class="btn" onclick={() => closeDialog(null)}>Cancel</button>
      {#each dialog.choices as c}
        <button type="button" class={c.danger ? 'btn btn-danger' : 'btn btn-primary'} onclick={() => closeDialog(c.value)}>{c.label}</button>
      {/each}
    </div>
  {/if}
</dialog>
