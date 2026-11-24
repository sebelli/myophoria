<script lang="ts">
  import { api } from '../lib/api'

  // startup recovery warnings (G8): shown until dismissed, never silent
  let warnings = $state<string[]>([])
  api.status().then((s) => (warnings = s.warnings)).catch(() => {})
</script>

{#if warnings.length}
  <div class="bg-amber-100 p-3 text-sm text-amber-900" role="alert">
    {#each warnings as w}<p>{w}</p>{/each}
    <button type="button" class="mt-1 underline" onclick={() => (warnings = [])}>Dismiss</button>
  </div>
{/if}
