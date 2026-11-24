<script lang="ts">
  let { message }: { message: string } = $props()
</script>

{#if message}
  <p class="rounded bg-red-50 p-2 text-sm text-red-800" role="alert">{message}</p>
{/if}
