<script lang="ts">
  import type { Component } from 'svelte'
  import ActiveWorkoutBar from './components/ActiveWorkoutBar.svelte'
  import ConfirmDialog from './components/ConfirmDialog.svelte'
  import NavBar from './components/NavBar.svelte'
  import StatusBanner from './components/StatusBanner.svelte'
  import { route } from './lib/router.svelte'
  import { loadOpen, workout } from './lib/workout.svelte'
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
    route.parts[0] === 'exercises' && route.parts[1]
      ? ExerciseDetail
      : route.parts[0] === 'history' && route.parts[1]
        ? SessionDetail
        : (screens[route.parts[0] ?? ''] ?? Workout), // an old #/workout/<id> link lands on Workout
  )

  // reopening the app mid-workout fills the store, so the workout bar offers Resume and End
  loadOpen().catch(() => {})
</script>

<StatusBanner />
<main class="mx-auto max-w-xl px-4 pt-4 {workout.session ? 'pb-40' : 'pb-24'}">
  {#key route.parts.join('/')}
    <Screen />
  {/key}
</main>
<div class="fixed inset-x-0 bottom-0">
  <ActiveWorkoutBar />
  <NavBar />
</div>
<ConfirmDialog />
