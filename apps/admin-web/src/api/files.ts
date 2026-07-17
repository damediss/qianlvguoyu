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

export async function fetchFileBlob(path: string): Promise<Blob> {
  const isAbsolute = path.startsWith('http://') || path.startsWith('https://');
  try {
    const response = isAbsolute
      ? await axios.get(path, { responseType: 'blob', timeout: 120000 })
      : await apiClient.get(path, { responseType: 'blob', timeout: 120000 });
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
  assertImageMagicIfNeeded(name, type, bytes);
  return new File([buffer], name, { type });
}

function assertImageMagicIfNeeded(name: string, type: string, bytes: Uint8Array) {
  const lower = name.toLowerCase();
  const looksImage = type.startsWith('image/')
    || /\.(png|jpe?g|gif|bmp|webp)$/.test(lower);
  if (!looksImage || bytes.length < 3) {
    return;
  }
  const isPng = bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47;
  const isJpeg = bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff;
  const isGif = bytes[0] === 0x47 && bytes[1] === 0x49 && bytes[2] === 0x46;
  const isBmp = bytes[0] === 0x42 && bytes[1] === 0x4d;
  const isWebp = bytes.length >= 12
    && bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46
    && bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50;
  if (!(isPng || isJpeg || isGif || isBmp || isWebp)) {
    throw new Error('上传的文件不是有效图片（内容已损坏或格式不支持），请用 JPG/PNG 重新导出后再上传');
  }
}

export async function uploadPlatformFile(file: File | Blob, accessPolicy?: string): Promise<FileVO> {
  const resolved = resolveUploadFile(file);
  if (!(resolved instanceof Blob)) {
    throw new Error('无法读取文件，请重新选择');
  }
  const uploadBody = await materializeUploadFile(resolved);
  const form = new FormData();
  form.append('file', uploadBody, uploadBody.name);
  if (accessPolicy) {
    form.append('accessPolicy', accessPolicy);
  }
  const response = await apiClient.post<ApiResponse<FileVO>>('/files', form, {
    timeout: 120000,
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

export function fetchUrlFileBlob(url: string) {
  if (!isUsableFileUrl(url)) {
    return Promise.reject(new Error('无效的文件地址'));
  }
  return fetchFileBlob(toProxiedFileFetchPath(url));
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
