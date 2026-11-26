<script lang="ts">
  let { message }: { message: string } = $props()
</script>

{#if message}
  <p class="border border-live p-2 text-sm text-live" role="alert">{message}</p>
{/if}
