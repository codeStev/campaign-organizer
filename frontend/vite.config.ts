import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// During `npm run dev`, /api is proxied to the backend so the frontend can use
// same-origin relative URLs identical to production (where nginx proxies /api).
// /webauthn and /login/webauthn are Spring Security's own WebAuthn ceremony endpoints
// (ADR-0111 follow-up), deliberately at the root rather than under /api to match their
// fixed paths — proxied the same way, mirroring nginx.conf in production.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
      '/webauthn': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
      '/login/webauthn': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
