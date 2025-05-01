import { defineConfig } from 'vite';
import solidPlugin from 'vite-plugin-solid';
import tailwindcss from '@tailwindcss/vite';
import devtools from 'solid-devtools/vite';
export default defineConfig({
  plugins: [
    solidPlugin(),
    tailwindcss(),
    devtools({
      autoname: true
    })
  ],
  server: {
    port: 3000,
  },
  build: {
    target: 'esnext',
  },
});
