<script lang="ts">
  import { route } from '../lib/router.svelte'

  const tabs = [
    { label: 'Workout', href: '#/workout', match: ['', 'workout', 'summary'] },
    { label: 'Exercises', href: '#/exercises', match: ['exercises'] },
    { label: 'History', href: '#/history', match: ['history'] },
    { label: 'More', href: '#/more', match: ['more', 'bodyweight', 'gyms', 'templates', 'data'] },
  ]
  const current = $derived(route.parts[0] ?? '')
</script>

<nav class="fixed inset-x-0 bottom-0 border-t bg-white pb-[env(safe-area-inset-bottom)]" aria-label="Main">
  <ul class="mx-auto flex max-w-xl">
    {#each tabs as t}
      <li class="flex-1">
        <a
          href={t.href}
          class="flex min-h-14 items-center justify-center text-sm {t.match.includes(current)
            ? 'font-semibold text-slate-900'
            : 'text-slate-500'}"
          aria-current={t.match.includes(current) ? 'page' : undefined}>{t.label}</a
        >
      </li>
    {/each}
  </ul>
</nav>
