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
  class="m-auto w-[min(28rem,calc(100%-2rem))] rounded-lg p-4 shadow-xl backdrop:bg-black/40"
  oncancel={(e) => {
    e.preventDefault()
    closeDialog(null)
  }}
>
  <p class="whitespace-pre-line">{dialog.message}</p>
  <div class="mt-4 flex flex-wrap justify-end gap-2">
    <button type="button" class="btn" onclick={() => closeDialog(null)}>Cancel</button>
    {#each dialog.choices as c}
      <button type="button" class={c.danger ? 'btn btn-danger' : 'btn btn-primary'} onclick={() => closeDialog(c.value)}
        >{c.label}</button
      >
    {/each}
  </div>
</dialog>
