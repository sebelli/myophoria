import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [svelte(), tailwindcss()],
  // MYOPHORIA_API lets a dev UI talk to a second backend (e.g. one on a data copy).
  server: { proxy: { '/api': process.env.MYOPHORIA_API ?? 'http://127.0.0.1:7070' } },
  build: { outDir: '../src/main/resources/public', emptyOutDir: true },
})
