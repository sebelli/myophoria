<script lang="ts">
  let { initials, deleted = false }: { initials: string; deleted?: boolean } = $props()
  const text = $derived(initials.slice(0, 4))
</script>

<span
  aria-hidden="true"
  class="flex h-10 w-10 shrink-0 items-center justify-center border font-display tracking-wide {deleted
    ? 'border-line text-faint'
    : 'border-edge text-dim'} {text.length >= 3 ? 'text-xs' : 'text-sm'}">{text}</span
>
