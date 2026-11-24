<script lang="ts">
  const links = [
    { href: '#/bodyweight', label: 'Bodyweight', text: 'Log and correct your bodyweight' },
    { href: '#/gyms', label: 'Gyms', text: 'Add, rename, set the default' },
    { href: '#/templates', label: 'Templates', text: 'Saved exercise lists to start from' },
    { href: '#/data', label: 'Data', text: 'Export, import and backups' },
  ]
</script>

<h1 class="mb-4 text-xl font-semibold">More</h1>
<ul class="card divide-y">
  {#each links as l}
    <li>
      <a href={l.href} class="block min-h-14 p-3">
        <span class="block font-medium">{l.label}</span>
        <span class="block text-sm text-slate-600">{l.text}</span>
      </a>
    </li>
  {/each}
</ul>
