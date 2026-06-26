import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080';
const rencaiProxyTarget = process.env.VITE_RENCAI_PROXY_TARGET ?? 'http://localhost:8001';

export default defineConfig({
  plugins: [react()],
  build: {
    target: 'esnext',
    minify: false,
  },
  server: {
    port: 5174,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
        ws: true,
      },
      '/rencai-api': {
        target: rencaiProxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/rencai-api/, '/api'),
      },
      '/rencai': {
        target: rencaiProxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/rencai/, ''),
      },
    },
  },
});
