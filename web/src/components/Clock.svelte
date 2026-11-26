<script lang="ts">
  import { elapsed } from '../lib/format'

  // The workout clock: time since the start, ticking once a second with no animation.
  let { since, class: cls = '' }: { since: string | null; class?: string } = $props()
  let now = $state(Date.now())
  $effect(() => {
    const id = setInterval(() => (now = Date.now()), 1000)
    return () => clearInterval(id)
  })
  const text = $derived(since ? elapsed(since, now) : '–')
</script>

<span role="timer" aria-label="Workout time {text}" class="font-display leading-none font-medium text-live tabular-nums {cls}">{text}</span>
