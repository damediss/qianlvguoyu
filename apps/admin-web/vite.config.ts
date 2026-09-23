import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://127.0.0.1:8080';
const rencaiProxyTarget = process.env.VITE_RENCAI_PROXY_TARGET ?? 'http://127.0.0.1:8001';

export default defineConfig({
  plugins: [react()],
  build: {
    target: 'esnext',
    minify: false,
  },
  optimizeDeps: {
    include: ['pdfjs-dist/legacy/build/pdf.min.js'],
  },
  worker: {
    format: 'es',
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    allowedHosts: true,
    cors: true,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
        ws: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            const origin = req.headers.origin;
            const host = req.headers.host;
            if (typeof origin !== 'string' || typeof host !== 'string') return;

            try {
              const originUrl = new URL(origin);
              const requestProtocol = req.socket.encrypted ? 'https:' : 'http:';
              if (originUrl.protocol === requestProtocol && originUrl.host.toLowerCase() === host.toLowerCase()) {
                // Browser-to-Vite is same-origin; do not make the API proxy look
                // like a cross-origin request to the backend CORS filter.
                proxyReq.removeHeader('origin');
              }
            } catch {
              // Keep malformed or non-matching origins so the backend can reject them.
            }
          });
        },
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
