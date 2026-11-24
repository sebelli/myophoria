<script lang="ts">
  import Exercises from './screens/Exercises.svelte'

  // hash routing without a router library (PLAN G9); more screens are added to this table
  const routes: Record<string, typeof Exercises> = { '/exercises': Exercises }

  let path = $state(location.hash.slice(1) || '/exercises')
  $effect(() => {
    const onHash = () => (path = location.hash.slice(1) || '/exercises')
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  })
  const Screen = $derived(routes[path] ?? Exercises)
</script>

<main class="mx-auto max-w-xl p-4">
  <Screen />
</main>
