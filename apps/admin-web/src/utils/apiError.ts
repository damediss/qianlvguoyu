/**
 * API 错误信息与 409 冲突 data 提取（教材二期共用）。
 * 后端错误体为 { code, message, data, traceId }；409 冲突时 data 携带结构化冲突信息（设计 §6）。
 */
import type { AssignmentConflictData, VersionConflictData } from '../types/textbookCollaboration';

interface ApiLikeError {
  response?: {
    status?: number;
    data?: { message?: string; data?: unknown } | string;
  };
  message?: string;
}

/** 提取用户可读的错误信息（业务 message 优先），供 message.error / Modal 展示 */
export function apiErrorMessage(e: unknown, fallback: string): string {
  const err = e as ApiLikeError;
  const body = typeof err?.response?.data === 'object' ? err.response.data : undefined;
  if (body?.message) return body.message;
  return err?.message || fallback;
}

/** 是否携带 409 HTTP 状态 */
function isHttp409(e: unknown): boolean {
  return (e as ApiLikeError)?.response?.status === 409;
}

/** 409 响应体中的结构化 data（body 为 JSON 时才有） */
function conflictPayload(e: unknown): Record<string, unknown> | null {
  const err = e as ApiLikeError;
  const body = typeof err?.response?.data === 'object' ? err.response.data : undefined;
  const data = body?.data;
  return data && typeof data === 'object' ? (data as Record<string, unknown>) : null;
}

/** W3/W4/W5/W9/A3 的 409 内容版本冲突 data（reason='VERSION_CONFLICT'）；非该结构返回 null */
export function extractVersionConflict(e: unknown): VersionConflictData | null {
  if (!isHttp409(e)) return null;
  const data = conflictPayload(e) as unknown as VersionConflictData | null;
  return data && data.reason === 'VERSION_CONFLICT' ? data : null;
}

/** C3/C4 的 409 分配版本冲突 data（reason='ASSIGNMENT_VERSION_CONFLICT'） */
export function extractAssignmentConflict(e: unknown): AssignmentConflictData | null {
  if (!isHttp409(e)) return null;
  const data = conflictPayload(e) as unknown as AssignmentConflictData | null;
  return data && data.reason === 'ASSIGNMENT_VERSION_CONFLICT' ? data : null;
}

/** C5 完成校验失败 409 data.completion（与 C2 同构） */
export function extractCompletionBlockers(e: unknown): { code: string; message: string }[] | null {
  if (!isHttp409(e)) return null;
  const data = conflictPayload(e) as { completion?: { blockers?: { code: string; message: string }[] } } | null;
  const blockers = data?.completion?.blockers;
  return Array.isArray(blockers) && blockers.length > 0 ? blockers : null;
}
