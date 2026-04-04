import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { createDevServerProxy } from './dev-proxy-config'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    exclude: ['**/node_modules/**', '**/e2e/**'],
  },
  server: {
    port: 3000,
    watch: {
      usePolling: true,
      interval: 150,
    },
    proxy: createDevServerProxy(),
  },
})
