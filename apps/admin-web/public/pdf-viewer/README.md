# 历年真题在线预览（pdf.js）

- 组件：`pdfjs-dist@3.11.174` 的 **legacy** 预构建产物（`pdf_viewer.js` / `pdf_viewer.css` / `build/pdf.js` / `build/pdf.worker.js` / `cmaps` / `standard_fonts`），Apache-2.0。
- 来源：`apps/admin-web/node_modules/pdfjs-dist`（与项目依赖同版本，避免二次 CDN 依赖）。
- 定制：`viewer.html` / `viewer.js` / `viewer.css` 为本项目自建，**刻意不提供下载、打印、打开原文件、分享入口**。
- 加载方式：`viewer.html?file=<urlencode(F1 带签名地址)>`，签名由后端 `ZhentiViewerUrlBuilder` 生成，前端不感知 secret。
- 升级：替换 `pdf_viewer.*` 与 `build/*` 时需保持 `pdfjsLib.version === pdfjsViewer` 版本一致（PDFViewer 构造函数会校验），并回归 `e2e/zhenti-past-papers.spec.ts` 路径 1。
