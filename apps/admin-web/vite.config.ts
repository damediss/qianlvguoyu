import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080';

// Chrome 80 / V8 8.0 (Android pad 现场版本) 不支持顶层 await、逻辑赋值、
// Array.at、Object.hasOwn、class 静态块等语法。Vite 6 默认 target='esnext'
// 不会降级，直接吐 ES2022+ 给浏览器，遇到旧 V8 会抛 SyntaxError 导致整页
// 白屏。这里把 dev / build / 预构建统一压到 Chrome 80 兼容基线 (es2020)。
const esTarget = 'es2020';

export default defineConfig({
  plugins: [react()],
  build: {
    target: esTarget,
  },
  esbuild: {
    target: esTarget,
  },
  optimizeDeps: {
    esbuildOptions: {
      target: esTarget,
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
