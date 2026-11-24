<script lang="ts">
  let { initials, deleted = false }: { initials: string; deleted?: boolean } = $props()
  const text = $derived(initials.slice(0, 4))
</script>

<span
  aria-hidden="true"
  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full font-semibold {deleted
    ? 'bg-slate-100 text-slate-400'
    : 'bg-slate-200 text-slate-700'} {text.length >= 3 ? 'text-xs' : 'text-sm'}">{text}</span
>
