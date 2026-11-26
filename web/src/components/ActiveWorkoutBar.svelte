<script lang="ts">
  import { errorMessage } from '../lib/api'
  import { choose } from '../lib/dialog.svelte'
  import { go, route } from '../lib/router.svelte'
  import { discardWorkout, finishWorkout, workout } from '../lib/workout.svelte'
  import Clock from './Clock.svelte'

  // A running workout stays one tap away on every other tab (replaces the old startup popup).
  const onWorkoutTab = $derived(['', 'workout', 'summary'].includes(route.parts[0] ?? ''))
  let error = $state('')

  async function end() {
    const answer = await choose('End this workout?', [
      { label: 'Finish & save', value: 'finish' },
      { label: 'Discard workout', value: 'discard', danger: true },
    ])
    try {
      if (answer === 'finish') await finishWorkout()
      if (answer === 'discard') await discardWorkout()
      error = ''
    } catch (e) {
      error = errorMessage(e)
    }
  }
</script>

{#if workout.session && !onWorkoutTab}
  <div class="border-t bg-field" role="region" aria-label="Workout in progress">
    <div class="mx-auto flex max-w-xl items-center gap-3 px-4 py-2">
      <Clock since={workout.session.startedAt} class="text-3xl" />
      <span class="flex-1 font-display text-xs leading-tight tracking-[0.1em] text-dim uppercase">Workout<br />running</span>
      <button type="button" class="btn min-h-11 px-3" onclick={end}>End</button>
      <button type="button" class="btn btn-primary min-h-11 px-3" onclick={() => go('workout')}>Resume</button>
    </div>
    {#if error}<p class="mx-auto max-w-xl px-4 pb-2 text-sm text-live" role="alert">{error}</p>{/if}
  </div>
{/if}
