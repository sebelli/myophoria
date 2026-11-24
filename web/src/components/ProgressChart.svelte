<script lang="ts">
  import { longDate, num, unitLabel } from '../lib/format'
  import type { ProgressPoint, Unit } from '../lib/types'

  // Hand-written SVG line chart (G14): one series, x = sessions in date order, y = top working weight.
  let { points, unit }: { points: ProgressPoint[]; unit: Unit } = $props()

  const W = 320
  const H = 160
  const PAD = { left: 40, right: 12, top: 12, bottom: 28 }

  const allZero = $derived(points.length > 0 && points.every((p) => p.topWeight === 0))
  const min = $derived(Math.min(...points.map((p) => p.topWeight)))
  const max = $derived(Math.max(...points.map((p) => p.topWeight)))
  const span = $derived(max - min || 1) // one value or a flat line still gets a usable scale

  function x(i: number) {
    return points.length === 1 ? (PAD.left + W - PAD.right) / 2 : PAD.left + (i * (W - PAD.left - PAD.right)) / (points.length - 1)
  }
  function y(v: number) {
    if (max === min) return (PAD.top + H - PAD.bottom) / 2 // a flat series sits in the middle, not on the axis
    return PAD.top + (1 - (v - min) / span) * (H - PAD.top - PAD.bottom)
  }
  const line = $derived(points.map((p, i) => `${x(i)},${y(p.topWeight)}`).join(' '))
  const description = $derived(
    points.length
      ? `Top working weight over ${points.length} sessions, from ${num(points[0].topWeight)} to ${num(points.at(-1)!.topWeight)} ${unitLabel(unit)}, best ${num(max)}.`
      : '',
  )
</script>

{#if points.length === 0}
  <p class="text-sm text-slate-600">No working sets logged yet.</p>
{:else if allZero}
  <p class="text-sm text-slate-600">No added weight logged.</p>
{:else}
  <svg viewBox="0 0 {W} {H}" class="w-full" role="img" aria-label={description}>
    <title>{description}</title>
    <line x1={PAD.left} x2={W - PAD.right} y1={H - PAD.bottom} y2={H - PAD.bottom} stroke="#cbd5e1" />
    <text x={PAD.left - 6} y={y(max) + 4} text-anchor="end" font-size="10" fill="#475569">{num(max)}</text>
    {#if max !== min}
      <text x={PAD.left - 6} y={y(min) + 4} text-anchor="end" font-size="10" fill="#475569">{num(min)}</text>
    {/if}
    <text x={x(0)} y={H - 10} text-anchor={points.length === 1 ? 'middle' : 'start'} font-size="10" fill="#475569">{longDate(points[0].localDate)}</text>
    {#if points.length > 1}
      <text x={x(points.length - 1)} y={H - 10} text-anchor="end" font-size="10" fill="#475569">{longDate(points.at(-1)!.localDate)}</text>
    {/if}
    <polyline points={line} fill="none" stroke="#0f172a" stroke-width="2" />
    {#each points as p, i}
      <circle cx={x(i)} cy={y(p.topWeight)} r="3" fill="#0f172a"><title>{longDate(p.localDate)}: {num(p.topWeight)} {unitLabel(unit)}</title></circle>
    {/each}
  </svg>
{/if}
