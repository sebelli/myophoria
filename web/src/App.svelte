<script lang="ts">
  import type { Component } from 'svelte'
  import ConfirmDialog from './components/ConfirmDialog.svelte'
  import NavBar from './components/NavBar.svelte'
  import StatusBanner from './components/StatusBanner.svelte'
  import { route } from './lib/router.svelte'
  import { api } from './lib/api'
  import { choose } from './lib/dialog.svelte'
  import { go } from './lib/router.svelte'
  import { finishWorkout } from './lib/workout.svelte'
  import ExerciseCard from './screens/ExerciseCard.svelte'
  import ExerciseDetail from './screens/ExerciseDetail.svelte'
  import Exercises from './screens/Exercises.svelte'
  import Bodyweight from './screens/Bodyweight.svelte'
  import Data from './screens/Data.svelte'
  import Gyms from './screens/Gyms.svelte'
  import History from './screens/History.svelte'
  import More from './screens/More.svelte'
  import SessionDetail from './screens/SessionDetail.svelte'
  import Templates from './screens/Templates.svelte'
  import Summary from './screens/Summary.svelte'
  import Workout from './screens/Workout.svelte'

  // first hash segment → screen; screens read further segments (ids) from route.parts themselves
  const screens: Record<string, Component> = {
    workout: Workout,
    summary: Summary,
    exercises: Exercises,
    history: History,
    more: More,
    bodyweight: Bodyweight,
    gyms: Gyms,
    templates: Templates,
    data: Data,
  }
  const Screen = $derived(
    route.parts[0] === 'workout' && route.parts[1]
      ? ExerciseCard
      : route.parts[0] === 'exercises' && route.parts[1]
        ? ExerciseDetail
        : route.parts[0] === 'history' && route.parts[1]
          ? SessionDetail
          : (screens[route.parts[0] ?? ''] ?? Workout),
  )

  // reopening the app mid-workout offers "Resume or finish" (handoff §8.6)
  api.openSession().then(async (open) => {
    if (!open || route.parts[0] === 'workout' || route.parts.length === 0) return
    const answer = await choose('A workout is still running.', [
      { label: 'Finish it', value: 'finish' },
      { label: 'Resume', value: 'resume' },
    ])
    if (answer === 'resume') go('workout')
    if (answer === 'finish') finishWorkout()
  }).catch(() => {})
</script>

<StatusBanner />
<main class="mx-auto max-w-xl px-4 pt-4 pb-24">
  {#key route.parts.join('/')}
    <Screen />
  {/key}
</main>
<NavBar />
<ConfirmDialog />
