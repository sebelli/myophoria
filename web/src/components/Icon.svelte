<script lang="ts">
  // Authored 24px line icons, one 2px stroke, square caps; decorative (the control carries the label).
  let { name, size = 20 }: { name: 'close' | 'back' | 'chevron' | 'check' | 'plus'; size?: number } = $props()
  const paths = {
    close: 'M6 6l12 12M18 6L6 18',
    back: 'M15 5l-7 7 7 7',
    chevron: 'M6 9l6 6 6-6',
    check: 'M5 12.5l4.5 4.5L19 7',
    plus: 'M12 5v14M5 12h14',
  }
</script>

<svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="square" aria-hidden="true"
  ><path d={paths[name]} /></svg
>
