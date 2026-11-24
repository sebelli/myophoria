<script lang="ts">
  import type { Component } from 'svelte'
  import ConfirmDialog from './components/ConfirmDialog.svelte'
  import NavBar from './components/NavBar.svelte'
  import StatusBanner from './components/StatusBanner.svelte'
  import { route } from './lib/router.svelte'
  import Exercises from './screens/Exercises.svelte'
  import More from './screens/More.svelte'

  // first hash segment → screen; screens read further segments (ids) from route.parts themselves
  const screens: Record<string, Component> = {
    exercises: Exercises,
    more: More,
  }
  const Screen = $derived(screens[route.parts[0] ?? ''] ?? Exercises)
</script>

<StatusBanner />
<main class="mx-auto max-w-xl px-4 pt-4 pb-24">
  {#key route.parts.join('/')}
    <Screen />
  {/key}
</main>
<NavBar />
<ConfirmDialog />
