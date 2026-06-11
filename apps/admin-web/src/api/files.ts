import axios from 'axios';
import { apiClient } from './client';
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

export async function uploadPlatformFile(file: File, accessPolicy?: string) {
  const formData = new FormData();
  formData.append('file', file);
  if (accessPolicy) {
    formData.append('accessPolicy', accessPolicy);
  }
  const response = await apiClient.post('/files', formData);
  // /files is not wrapped by unwrap() in this module to keep blob error parsing isolated.
  const data = response.data as { data?: FileVO; message?: string };
  if (!data?.data) {
    throw new Error(data?.message || '文件上传失败');
  }
  return data.data;
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

export function fileNameFromUrl(url?: string): string {
  if (!url) return '';
  const path = url.split('?')[0].split('#')[0];
  const name = path.substring(path.lastIndexOf('/') + 1);
  return decodeURIComponent(name);
}

export type FilePreviewKind = 'image' | 'docx' | 'pdf' | 'unsupported';

export function resolveFilePreviewKind(fileName?: string, fileType?: string): FilePreviewKind {
  const type = (fileType || '').toLowerCase();
  const name = (fileName || '').toLowerCase();

  if (type.startsWith('image/') || /\.(png|jpe?g|gif|webp|bmp|svg)$/.test(name)) {
    return 'image';
  }
  if (
    type.includes('wordprocessingml')
    || type.includes('msword')
    || type.includes('docx')
    || name.endsWith('.docx')
    || name.endsWith('.doc')
  ) {
    return 'docx';
  }
  if (type.includes('pdf') || name.endsWith('.pdf')) {
    return 'pdf';
  }
  return 'unsupported';
}
