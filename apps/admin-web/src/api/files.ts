import axios from 'axios';
import JSONBig from 'json-bigint';
import { useAuthStore } from '../stores/authStore';
import { apiClient } from './client';
import type { ApiResponse } from '../types/api';
import type { FileVO } from '../types/knowledge';

const jsonBig = JSONBig({ storeAsString: true });

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
  try {
    const response = await apiClient.get(path, { responseType: 'blob' });
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
  return new File([buffer], name, { type });
}

function xhrUploadBinary(
  url: string,
  body: Blob,
  originalName: string,
  contentType: string,
  accessPolicy?: string,
): Promise<ApiResponse<FileVO>> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', url);
    const token = useAuthStore.getState().token;
    const tenantId = useAuthStore.getState().tenantId;
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
    if (tenantId) {
      xhr.setRequestHeader('X-Tenant-Id', String(tenantId));
    }
    try {
      xhr.setRequestHeader('X-Request-Id', crypto.randomUUID());
    } catch {
      xhr.setRequestHeader('X-Request-Id', String(Date.now()));
    }
    xhr.setRequestHeader('Content-Type', 'application/octet-stream');
    if (contentType && contentType !== 'application/octet-stream') {
      xhr.setRequestHeader('X-Content-Type', contentType);
    }
    xhr.setRequestHeader('X-Original-Filename', encodeURIComponent(originalName));
    if (accessPolicy) {
      xhr.setRequestHeader('X-Access-Policy', accessPolicy);
    }
    xhr.onload = () => {
      try {
        const payload = jsonBig.parse(xhr.responseText) as ApiResponse<FileVO>;
        if (xhr.status >= 400 || payload.code !== '0') {
          reject(new Error(payload.message || '文件上传失败'));
          return;
        }
        resolve(payload);
      } catch {
        reject(new Error('文件上传失败'));
      }
    };
    xhr.onerror = () => reject(new Error('文件上传失败'));
    xhr.send(body);
  });
}

export async function uploadPlatformFile(file: File | Blob, accessPolicy?: string): Promise<FileVO> {
  const resolved = resolveUploadFile(file);
  if (!(resolved instanceof Blob)) {
    throw new Error('无法读取文件，请重新选择');
  }
  const uploadBody = await materializeUploadFile(resolved);
  const payload = await xhrUploadBinary(
    '/api/files',
    uploadBody,
    uploadBody.name,
    uploadBody.type || 'application/octet-stream',
    accessPolicy,
  );
  if (!payload.data) {
    throw new Error(payload.message || '文件上传失败');
  }
  return payload.data;
}

export function fetchCandidateDocumentBlob(taskId: string, documentId: string) {
  return fetchFileBlob(
    `/certification/preparation/tasks/${taskId}/candidate-documents/${documentId}/download`,
  );
}

export function fetchPlatformFileBlob(fileId: string) {
  return fetchFileBlob(`/files/${encodeURIComponent(fileId)}/download?inline=true`);
}

/** 将 CDN 绝对地址转为 apiClient 相对路径（不含 /api 前缀，baseURL 已含） */
export function toProxiedFileFetchPath(url: string): string {
  if (!url) return url;
  const marker = '/exam/files/';
  let path = url;
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      path = new URL(url).pathname;
    } catch {
      return url;
    }
  }
  const idx = path.indexOf(marker);
  if (idx >= 0) {
    return path.substring(idx);
  }
  if (path.startsWith('/api/')) {
    return path.substring(4);
  }
  return path.startsWith('/') ? path : `/${path}`;
}

export function fetchUrlFileBlob(url: string) {
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
