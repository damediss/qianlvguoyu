import { parseCollectionToken } from './scanToken';

export interface ParsedScanResult {
  token?: string;
  taskId?: string;
  candidateId?: string;
  collectionId?: string;
  examNo?: string;
  idCardNo?: string;
  rawText: string;
}

function pickString(value: unknown): string | undefined {
  if (value === null || value === undefined) {
    return undefined;
  }
  const text = String(value).trim();
  return text || undefined;
}

/**
 * 解析扫码原始文本，兼容 URL、JSON、纯 token 与普通字符串。
 */
export function parseScanQRCodeResult(rawText?: string | null): ParsedScanResult {
  const raw = (rawText ?? '').trim();
  const result: ParsedScanResult = { rawText: raw };
  if (!raw) {
    return result;
  }

  if (raw.startsWith('{')) {
    try {
      const json = JSON.parse(raw) as Record<string, unknown>;
      result.taskId = pickString(json.taskId);
      result.candidateId = pickString(json.candidateId);
      result.collectionId = pickString(json.collectionId);
      result.examNo = pickString(json.examNo)
        || pickString(json.admissionNo)
        || pickString(json.candidateNo);
      result.idCardNo = pickString(json.idCardNo);
      result.token = parseCollectionToken(
        pickString(json.collectionToken)
        || pickString(json.collectToken)
        || pickString(json.token)
        || raw,
      );
      return result;
    } catch {
      // Fall through to generic parsing.
    }
  }

  if (raw.startsWith('http://') || raw.startsWith('https://')) {
    try {
      const url = new URL(raw);
      result.taskId = pickString(url.searchParams.get('taskId'));
      result.candidateId = pickString(url.searchParams.get('candidateId'));
      result.collectionId = pickString(url.searchParams.get('collectionId'));
      result.examNo = pickString(url.searchParams.get('examNo'))
        || pickString(url.searchParams.get('admissionNo'))
        || pickString(url.searchParams.get('candidateNo'));
      result.idCardNo = pickString(url.searchParams.get('idCardNo'));
      result.token = parseCollectionToken(raw);
      return result;
    } catch {
      // Fall through to generic parsing.
    }
  }

  result.token = parseCollectionToken(raw);
  return result;
}
