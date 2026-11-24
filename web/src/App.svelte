<script lang="ts">
  let status = $state('loading...')
  fetch('/api/v1/status')
    .then((r) => r.text())
    .then((t) => (status = t))
    .catch((e) => (status = String(e)))
</script>

<main class="p-4 font-mono">{status}</main>
