import axios from 'axios';
import { apiClient } from './client';
import type { ApiResponse } from '../types/api';
import type { FileVO } from '../types/knowledge';

async function parseBlobError(blob: Blob, fallback: string): Promise<string> {
  if (!blob.type.includes('json') && blob.size > 512) {
    return fallback;
  }
  try {
    const text = await blob.text();
    const json = JSON.parse(text) as { message?: string };
    return json.message || fallback;
  } catch {
    return fallback;
  }
}

export async function fetchFileBlob(path: string, options?: { silentError?: boolean }): Promise<Blob> {
  const isAbsolute = path.startsWith('http://') || path.startsWith('https://');
  const silentError = options?.silentError === true;
  try {
    const response = isAbsolute
      ? await axios.get(path, { responseType: 'blob', timeout: 120000 })
      : await apiClient.get(path, { responseType: 'blob', timeout: 120000, silentError });
    const blob = response.data as Blob;
    if (blob.type.includes('json') && blob.size < 4096) {
      throw new Error(await parseBlobError(blob, '文件下载失败'));
    }
    return blob;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
      throw new Error(await parseBlobError(error.response.data, '文件下载失败'));
    }
    throw error;
  }
}

export function resolveUploadFile(file: File | Blob | { originFileObj?: File }): File | Blob {
  if (file && typeof file === 'object' && 'originFileObj' in file) {
    const origin = (file as { originFileObj?: File }).originFileObj;
    if (origin instanceof Blob) {
      return origin;
    }
  }
  if (file instanceof Blob) {
    return file;
  }
  return file as File;
}

async function materializeUploadFile(file: File | Blob): Promise<File> {
  const buffer = await file.arrayBuffer();
  if (buffer.byteLength <= 0) {
    throw new Error('文件为空，请重新选择');
  }
  const name = file instanceof File ? file.name : 'upload.bin';
  const type = file.type || 'application/octet-stream';
  const bytes = new Uint8Array(buffer);

  // 非图片直接放行（由业务侧约束）
  if (!looksLikeImageUpload(name, type)) {
    return new File([buffer], name, { type });
  }
  // 标准图片头：直接上传
  if (hasImageMagic(bytes)) {
    return new File([buffer], name, { type: type.startsWith('image/') ? type : guessImageMime(bytes) });
  }

  // HEIC/HEIF（含改后缀为 .jpg 的情况）
  const heicConverted = await convertHeicToJpeg(file, name, bytes);
  if (heicConverted) {
    return heicConverted;
  }

  // 浏览器可解码的其他格式 → 转成标准 JPG
  const reencoded = await reencodeAsJpeg(file, name);
  if (reencoded) {
    return reencoded;
  }

  // 与旧 PHP 对齐：非标准文件头不再前端拦截，交由后端软校验后原样存储
  return new File([buffer], name, { type: type.startsWith('image/') ? type : 'image/jpeg' });
}

function looksLikeImageUpload(name: string, type: string): boolean {
  return type.startsWith('image/')
    || /\.(png|jpe?g|jfif|gif|bmp|webp|heic|heif)$/i.test(name);
}

function guessImageMime(bytes: Uint8Array): string {
  if (hasJpegMagic(bytes)) return 'image/jpeg';
  if (bytes.length >= 8 && bytes[0] === 0x89 && bytes[1] === 0x50) return 'image/png';
  if (bytes.length >= 12 && bytes[0] === 0x52 && bytes[8] === 0x57) return 'image/webp';
  return 'application/octet-stream';
}

function hasJpegMagic(bytes: Uint8Array): boolean {
  return bytes.length >= 2 && bytes[0] === 0xff && bytes[1] === 0xd8;
}

function hasImageMagic(bytes: Uint8Array): boolean {
  if (bytes.length < 2) {
    return false;
  }
  const isPng = bytes.length >= 8
    && bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47;
  const isJpeg = hasJpegMagic(bytes);
  const isGif = bytes.length >= 3
    && bytes[0] === 0x47 && bytes[1] === 0x49 && bytes[2] === 0x46;
  const isBmp = bytes[0] === 0x42 && bytes[1] === 0x4d;
  const isWebp = bytes.length >= 12
    && bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46
    && bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50;
  return isPng || isJpeg || isGif || isBmp || isWebp;
}

function isHeicLike(bytes: Uint8Array, name: string): boolean {
  if (/\.(heic|heif)$/i.test(name)) {
    return true;
  }
  if (bytes.length < 12) {
    return false;
  }
  // ISO BMFF: size(4) + 'ftyp'(4) + major_brand(4)
  const brand = String.fromCharCode(bytes[4], bytes[5], bytes[6], bytes[7]);
  if (brand !== 'ftyp') {
    return false;
  }
  const major = String.fromCharCode(bytes[8], bytes[9], bytes[10], bytes[11]).toLowerCase();
  if (/heic|heif|mif1|msf1|heim|heis|hevm|hevs/.test(major)) {
    return true;
  }
  // 兼容 brand 列表也可能在后续字节
  const head = Array.from(bytes.slice(8, Math.min(bytes.length, 32)))
    .map((b) => (b >= 32 && b < 127 ? String.fromCharCode(b) : ''))
    .join('')
    .toLowerCase();
  return /heic|heif|mif1|msf1/.test(head);
}

async function convertHeicToJpeg(
  file: Blob,
  originalName: string,
  bytes: Uint8Array,
): Promise<File | null> {
  if (!isHeicLike(bytes, originalName)) {
    return null;
  }
  try {
    const heic2any = (await import('heic2any')).default;
    const result = await heic2any({
      blob: file,
      toType: 'image/jpeg',
      quality: 0.92,
    });
    const blob = Array.isArray(result) ? result[0] : result;
    if (!(blob instanceof Blob) || blob.size <= 0) {
      return null;
    }
    return toJpegFile(blob, originalName);
  } catch {
    return null;
  }
}

async function reencodeAsJpeg(file: Blob, originalName: string): Promise<File | null> {
  const fromBitmap = await drawToJpegBlob(async () => createImageBitmap(file));
  if (fromBitmap) {
    return toJpegFile(fromBitmap, originalName);
  }
  // createImageBitmap 失败时，再用 <img> 解码兜底
  const objectUrl = URL.createObjectURL(file);
  try {
    const fromImg = await drawToJpegBlob(async () => {
      const img = await loadHtmlImage(objectUrl);
      return img;
    }, true);
    if (fromImg) {
      return toJpegFile(fromImg, originalName);
    }
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
  return null;
}

function loadHtmlImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('image load failed'));
    img.src = src;
  });
}

async function drawToJpegBlob(
  sourceFactory: () => Promise<ImageBitmap | HTMLImageElement>,
  closeBitmap = false,
): Promise<Blob | null> {
  try {
    const source = await sourceFactory();
    const width = 'width' in source ? source.width : 0;
    const height = 'height' in source ? source.height : 0;
    if (!width || !height) {
      return null;
    }
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return null;
    }
    ctx.drawImage(source, 0, 0);
    if (closeBitmap && source instanceof ImageBitmap) {
      source.close();
    } else if (source instanceof ImageBitmap) {
      source.close();
    }
    return await new Promise<Blob | null>((resolve) => {
      canvas.toBlob((b) => resolve(b), 'image/jpeg', 0.92);
    });
  } catch {
    return null;
  }
}

function toJpegFile(blob: Blob, originalName: string): File | null {
  if (!blob || blob.size <= 0) {
    return null;
  }
  const base = originalName.replace(/\.[^.]+$/, '') || 'upload';
  return new File([blob], `${base}.jpg`, { type: 'image/jpeg' });
}

export async function uploadPlatformFile(
  file: File | Blob,
  accessPolicy?: string,
  timeout = 120000,
): Promise<FileVO> {
  const resolved = resolveUploadFile(file);
  if (!(resolved instanceof Blob)) {
    throw new Error('无法读取文件，请重新选择');
  }
  // 非图片（特别是大尺寸 xlsx）无需读取到浏览器内存，直接流式交给浏览器上传。
  const name = resolved instanceof File ? resolved.name : 'upload.bin';
  const uploadBody = looksLikeImageUpload(name, resolved.type)
    ? await materializeUploadFile(resolved)
    : resolved instanceof File
      ? resolved
      : new File([resolved], name, { type: resolved.type || 'application/octet-stream' });
  if (uploadBody.size <= 0) {
    throw new Error('文件为空，请重新选择');
  }
  const form = new FormData();
  form.append('file', uploadBody, uploadBody.name);
  if (accessPolicy) {
    form.append('accessPolicy', accessPolicy);
  }
  const response = await apiClient.post<ApiResponse<FileVO>>('/files', form, {
    timeout,
  });
  if (!response.data.data) {
    throw new Error(response.data.message || '文件上传失败');
  }
  return response.data.data;
}

export async function getPlatformFile(fileId: string): Promise<FileVO> {
  const response = await apiClient.get<ApiResponse<FileVO>>(`/files/${encodeURIComponent(fileId)}`);
  if (!response.data.data) {
    throw new Error(response.data.message || '文件不存在');
  }
  return response.data.data;
}

export function fetchCandidateDocumentBlob(taskId: string, documentId: string) {
  return fetchFileBlob(
    `/certification/preparation/tasks/${taskId}/candidate-documents/${documentId}/download`,
  );
}

/** 私有 COS 桶直链浏览器无法访问，须走后端鉴权下载 */
export function isPrivateCosUrl(url?: string | null): boolean {
  if (!url?.trim()) return false;
  return /\.cos\.[\w-]+\.myqcloud\.com/i.test(url.trim());
}

function shouldFetchFileViaApi(file: FileVO): boolean {
  if (file.accessPolicy === 'private') return true;
  return isPrivateCosUrl(file.url);
}

export async function fetchPlatformFileBlob(fileId: string) {
  const file = await getPlatformFile(fileId);
  if (file.url && isUsableFileUrl(file.url) && !shouldFetchFileViaApi(file)) {
    return fetchUrlFileBlob(file.url);
  }
  return fetchFileBlob(`/files/${encodeURIComponent(fileId)}/download?inline=true`);
}

/** 是否为可拉取的文件地址（排除 OSS 处理参数片段等脏数据） */
export function isUsableFileUrl(url?: string | null): boolean {
  if (!url?.trim()) return false;
  const t = url.trim();
  if (t.startsWith('blob:') || t.startsWith('data:')) return true;
  if (/^(process=image|x-oss-process=|\?x-oss-process=)/i.test(t)) return false;
  if (/^image\/(resize|quality|crop)/i.test(t)) return false;
  if (t.startsWith('http://') || t.startsWith('https://')) return true;
  if (t.includes('/exam/files/') || t.includes('/files/')) return true;
  if (/\.(jpe?g|png|gif|webp|bmp|pdf|docx?)$/i.test((t.split('?')[0] ?? ''))) return true;
  if (/^(idCard|contract|upload|down)\//i.test(t.replace(/^\//, ''))) return true;
  return t.includes('/');
}

/** 将 CDN 绝对地址转为 apiClient 相对路径（不含 /api 前缀，baseURL 已含） */
export function toProxiedFileFetchPath(url: string): string {
  if (!url) return url;
  const marker = '/exam/files/';
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      const parsed = new URL(url);
      const idx = parsed.pathname.indexOf(marker);
      if (idx >= 0) {
        return parsed.pathname.substring(idx) + parsed.search;
      }
      const filesIdx = parsed.pathname.indexOf('/files/');
      if (filesIdx >= 0) {
        return parsed.pathname.substring(filesIdx) + parsed.search;
      }
      return url;
    } catch {
      return url;
    }
  }
  const idx = url.indexOf(marker);
  if (idx >= 0) {
    return url.substring(idx);
  }
  if (url.startsWith('/api/')) {
    return url.substring(4);
  }
  return url.startsWith('/') ? url : `/${url}`;
}

/**
 * 后端 /exam/files/** 为 permitAll，本地缺失时还会 302 到生产 CDN。
 * 这类地址应用 img 直链，不能走 axios：XHR 跟随跨域 302 会变成 Network Error 并刷 toast。
 */
export function isPublicExamFileUrl(url?: string | null): boolean {
  if (!url?.trim()) return false;
  return url.includes('/exam/files/');
}

/**
 * 将 exam/files 地址规范为浏览器可访问路径。
 * 一律改为同源 `/api/exam/files/...`（Vite 代理 → platform-api）：
 * - 本地有文件：直接读盘
 * - App 扫码上传（sign-up/**）本地没有：后端 302 到 platform.cdn.public-url（Nginx）
 * 不要把远端绝对地址直接塞给 <img>：开发机若改写错误或 302 到 cdn.app 会造成整列裂图。
 */
export function toBrowserPublicFileSrc(url: string): string {
  if (!url) return url;
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      const parsed = new URL(url);
      const marker = '/exam/files/';
      const idx = parsed.pathname.indexOf(marker);
      if (idx < 0) {
        return url;
      }
      return `/api${parsed.pathname.substring(idx)}${parsed.search}`;
    } catch {
      return url;
    }
  }
  if (url.startsWith('/api/')) return url;
  if (url.startsWith('/exam/files/')) return `/api${url}`;
  const idx = url.indexOf('/exam/files/');
  if (idx >= 0) return `/api${url.substring(idx)}`;
  return url.startsWith('/') ? url : `/${url}`;
}

export function fetchUrlFileBlob(url: string, options?: { silentError?: boolean }) {
  if (!isUsableFileUrl(url)) {
    return Promise.reject(new Error('无效的文件地址'));
  }
  return fetchFileBlob(toProxiedFileFetchPath(url), options);
}

export async function downloadUrlFile(url: string, fileName?: string): Promise<void> {
  const blob = await fetchUrlFileBlob(url);
  const name = fileName || fileNameFromUrl(url) || 'download';
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = name;
  anchor.click();
  URL.revokeObjectURL(objectUrl);
}

/**
 * 导出文件下载：/exam/files/ 路径是 permitAll，用 window.open 原生流式下载（大文件不占内存）；
 * 其他路径（如 /api/files/{id}/download）需要 X-Tenant-Id 认证头，用 downloadUrlFile 走 apiClient。
 */
export function downloadExportFile(url: string): void {
  if (isPublicExamFileUrl(url)) {
    window.open(url, '_blank');
  } else {
    void downloadUrlFile(url).catch(() => {});
  }
}

export function fileNameFromUrl(url?: string): string {
  if (!url) return '';
  const path = url.split('?')[0].split('#')[0];
  const name = path.substring(path.lastIndexOf('/') + 1);
  return decodeURIComponent(name);
}

export type FilePreviewKind = 'image' | 'docx' | 'pdf' | 'spreadsheet' | 'office' | 'unsupported';

function isImageType(type: string, name: string): boolean {
  return type.startsWith('image/')
    || ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(type)
    || /\.(png|jpe?g|gif|webp|bmp|svg)$/.test(name);
}

function isDocxType(type: string, name: string): boolean {
  return type.includes('wordprocessingml')
    || type.includes('msword')
    || type === 'doc'
    || type === 'docx'
    || type.includes('docx')
    || name.endsWith('.docx')
    || name.endsWith('.doc');
}

function isPdfType(type: string, name: string): boolean {
  return type === 'pdf'
    || type.includes('pdf')
    || name.endsWith('.pdf');
}

/** Excel 表格（本地 SheetJS 预览，不依赖 Office Online） */
export function isSpreadsheetType(type: string, name: string): boolean {
  return type.includes('spreadsheetml')
    || type.includes('ms-excel')
    || type === 'xls'
    || type === 'xlsx'
    || /\.xlsx?$/.test(name);
}

/** Office Online 可预览的 Word / PowerPoint（对齐 PHP，须公网 URL） */
function isOfficeOnlineType(type: string, name: string): boolean {
  return type.includes('wordprocessingml')
    || type.includes('msword')
    || type === 'doc'
    || type === 'docx'
    || type.includes('presentationml')
    || type.includes('ms-powerpoint')
    || type === 'ppt'
    || type === 'pptx'
    || /\.(docx?|pptx?)$/.test(name);
}

export function buildAbsoluteFileUrl(url: string): string {
  if (!url) return url;
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  const origin = typeof window !== 'undefined' ? window.location.origin : '';
  return url.startsWith('/') ? `${origin}${url}` : `${origin}/${url}`;
}

function isLocalhostHost(hostname: string): boolean {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '[::1]';
}

/**
 * 解析 Office Online 所需的文件直链（须公网可访问）。
 * 本地开发可将 VITE_PUBLIC_FILE_BASE_URL 设为线上 CDN，例如 https://cdn.example.com/api/exam/files
 */
export function resolveOfficeOnlineFileSrc(fileUrl: string): string {
  const absolute = buildAbsoluteFileUrl(fileUrl);
  try {
    const parsed = new URL(absolute);
    const publicBase = (import.meta.env.VITE_PUBLIC_FILE_BASE_URL as string | undefined)?.trim();
    if (publicBase && isLocalhostHost(parsed.hostname)) {
      const marker = '/exam/files/';
      const idx = parsed.pathname.indexOf(marker);
      if (idx >= 0) {
        const relative = parsed.pathname.substring(idx + marker.length).replace(/^\//, '');
        return `${publicBase.replace(/\/$/, '')}/${relative}${parsed.search}`;
      }
    }
    return absolute;
  } catch {
    return absolute;
  }
}

/** 对齐 PHP: https://view.officeapps.live.com/op/view.aspx?src= + file_url */
export function buildOfficeOnlinePreviewUrl(fileUrl: string): string {
  const src = resolveOfficeOnlineFileSrc(fileUrl);
  return `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(src)}`;
}

export function isOfficeOnlinePreviewLocalhost(fileUrl: string): boolean {
  try {
    const host = new URL(resolveOfficeOnlineFileSrc(fileUrl)).hostname;
    return isLocalhostHost(host);
  } catch {
    return false;
  }
}

/** CDN 直链 + 公网地址时走 Office Online（PHP 方式）；Excel 一律本地预览 */
export function shouldUseOfficeOnlinePreview(
  fileUrl?: string,
  fileName?: string,
  fileType?: string,
  mimeType?: string,
): boolean {
  if (!fileUrl || isOfficeOnlinePreviewLocalhost(fileUrl)) return false;
  const name = (fileName || fileNameFromUrl(fileUrl) || '').toLowerCase();
  const hints = [(mimeType || '').toLowerCase(), (fileType || '').toLowerCase()].filter(Boolean);
  for (const type of hints) {
    if (isSpreadsheetType(type, name)) return false;
    if (isOfficeOnlineType(type, name)) return true;
  }
  if (isSpreadsheetType('', name)) return false;
  return isOfficeOnlineType('', name);
}

export function shouldUseSpreadsheetPreview(
  fileName?: string,
  fileType?: string,
  mimeType?: string,
  fileUrl?: string,
): boolean {
  const name = (fileName || fileNameFromUrl(fileUrl) || '').toLowerCase();
  const hints = [(mimeType || '').toLowerCase(), (fileType || '').toLowerCase()].filter(Boolean);
  for (const type of hints) {
    if (isSpreadsheetType(type, name)) return true;
  }
  return isSpreadsheetType('', name);
}

/** 从文件名、扩展名（如 pdf/docx）或 MIME 推断预览类型 */
export function resolveFilePreviewKind(
  fileName?: string,
  fileType?: string,
  mimeType?: string,
): FilePreviewKind {
  const name = (fileName || '').toLowerCase();
  const hints = [(mimeType || '').toLowerCase(), (fileType || '').toLowerCase()].filter(Boolean);

  for (const type of hints) {
    if (isImageType(type, name)) return 'image';
    if (isSpreadsheetType(type, name)) return 'spreadsheet';
    if (isDocxType(type, name)) return 'docx';
    if (isPdfType(type, name)) return 'pdf';
    if (isOfficeOnlineType(type, name)) return 'office';
  }

  if (isImageType('', name)) return 'image';
  if (isSpreadsheetType('', name)) return 'spreadsheet';
  if (isDocxType('', name)) return 'docx';
  if (isPdfType('', name)) return 'pdf';
  if (isOfficeOnlineType('', name)) return 'office';
  return 'unsupported';
}
