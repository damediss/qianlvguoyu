import { defineConfig, type Plugin } from 'vite';
import { build } from 'esbuild';
import react from '@vitejs/plugin-react';

/**
 * 接管压缩：config.build.minify 保持 false。两个历史原因：
 * 1. vite 内置压缩走 esbuild transform API，Windows 下对大 chunk（>数 MB）有协议
 *    缺陷（输入被截断/错位，报 Unexpected "\x88"），antd 级 4MB chunk 必炸——
 *    历史上为此全局关了压缩（minify:false）；
 * 2. 本插件改用 esbuild build API（stdin 文件通道，实测 4MB+ 正常），并强制
 *    charset:'ascii' 输出纯 ASCII（非 ASCII 一律 \uXXXX 转义），杜绝下游编码链路问题。
 */
function asciiMinify(): Plugin {
  return {
    name: 'ascii-minify',
    renderChunk: {
      order: 'post',
      async handler(code) {
        const result = await build({
          stdin: { contents: code, loader: 'js' },
          write: false,
          minify: true,
          charset: 'ascii',
          target: 'esnext',
          logLevel: 'silent',
        });
        const output = result.outputFiles?.[0];
        if (!output) {
          throw new Error('ascii-minify: esbuild build produced no output');
        }
        return { code: output.text, map: null };
      },
    },
  };
}

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://127.0.0.1:8080';
const rencaiProxyTarget = process.env.VITE_RENCAI_PROXY_TARGET ?? 'http://127.0.0.1:8001';

export default defineConfig({
  plugins: [react(), asciiMinify()],
  build: {
    target: 'esnext',
    // 压缩由 asciiMinify 插件接管（vite 内置 esbuild transform 对大 chunk 有 Windows 协议缺陷）。
    // 不做 manualChunks：rollup 默认按"入口可达性"切 chunk——懒加载页面独占的 antd 组件
    // 留在异步 chunk，只有入口真用到的进首屏。
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
