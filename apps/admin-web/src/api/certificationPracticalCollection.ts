import { apiClient, unwrap } from './client';
import { parseContentDispositionFilename } from '../utils/download';
import type { PageResponse } from '../types/api';
import type {
  CandidateCollectionDetailVO,
  CandidateMatchVO,
  CollectionActionResultVO,
  CollectionExceptionQueryRequest,
  CollectionExceptionVO,
  CollectionTaskQueryRequest,
  CollectionTaskVO,
  CollectionWorkbenchVO,
  CompleteCollectionRequest,
  ExceptionReleaseRequest,
  FingerprintTraceVO,
  JoinQueueRequest,
  MarkInfoErrorRequest,
  MarkPendingRetryRequest,
  MatchByTokenRequest,
  QueueItemVO,
  RemoveQueueRequest,
  ScanMatchResultVO,
  SearchCandidateRequest,
  SignatureRecordVO,
  SnowflakeId,
  StartNextRequest,
  SubmitFingerprintRequest,
  SubmitSignatureRequest,
} from '../types/certificationPracticalCollection';

const base = '/certification/practical-collection';
const collectionRequestConfig = { timeout: 120000 };

export function getCollectionTasks(
  params: CollectionTaskQueryRequest,
): Promise<PageResponse<CollectionTaskVO>> {
  return unwrap<PageResponse<CollectionTaskVO>>(
    apiClient.get(`${base}/tasks`, { params, ...collectionRequestConfig }),
  );
}

export function getCollectionWorkbench(taskId: SnowflakeId): Promise<CollectionWorkbenchVO> {
  return unwrap<CollectionWorkbenchVO>(
    apiClient.get(`${base}/workbench/${taskId}`, collectionRequestConfig),
  );
}

export function initializeCollection(taskId: SnowflakeId): Promise<CollectionActionResultVO> {
  return unwrap<CollectionActionResultVO>(
    apiClient.post(`${base}/tasks/${taskId}/initialize`, undefined, collectionRequestConfig),
  );
}

export function generateQrCodes(taskId: SnowflakeId): Promise<CollectionActionResultVO> {
  return unwrap<CollectionActionResultVO>(
    apiClient.post(`${base}/tasks/${taskId}/qrcodes/generate`, undefined, collectionRequestConfig),
  );
}

export async function exportQrCodes(taskId: SnowflakeId): Promise<{ filename: string }> {
  const response = await apiClient.get(`${base}/tasks/${taskId}/qrcodes/export`, {
    ...collectionRequestConfig,
    responseType: 'blob',
  });
  const blob = new Blob([response.data as BlobPart], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const filename = parseContentDispositionFilename(response)
    || `practical-collection-qrcodes-${taskId}.xlsx`;
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
  return { filename };
}

export function matchByToken(params: MatchByTokenRequest): Promise<CandidateMatchVO> {
  return unwrap<CandidateMatchVO>(
    apiClient.post(`${base}/match-by-token`, params, collectionRequestConfig),
  );
}

export function scanMatchCurrent(
  taskId: SnowflakeId,
  token: string,
): Promise<ScanMatchResultVO> {
  return unwrap<ScanMatchResultVO>(
    apiClient.post(`${base}/workbench/${taskId}/scan-match-current`, { token }, collectionRequestConfig),
  );
}

export function openCandidateCollection(
  taskId: SnowflakeId,
  candidateId: SnowflakeId,
): Promise<CandidateCollectionDetailVO> {
  return unwrap<CandidateCollectionDetailVO>(
    apiClient.post(`${base}/workbench/${taskId}/candidates/${candidateId}/open`, undefined, collectionRequestConfig),
  );
}

export function searchCandidate(params: SearchCandidateRequest): Promise<CandidateMatchVO[]> {
  return unwrap<CandidateMatchVO[]>(
    apiClient.post(`${base}/search-candidate`, params, collectionRequestConfig),
  );
}

export function joinQueue(params: JoinQueueRequest): Promise<QueueItemVO> {
  return unwrap<QueueItemVO>(
    apiClient.post(`${base}/queue/join`, params, collectionRequestConfig),
  );
}

export function removeQueue(params: RemoveQueueRequest): Promise<QueueItemVO> {
  return unwrap<QueueItemVO>(
    apiClient.post(`${base}/queue/remove`, params, collectionRequestConfig),
  );
}

export function startNext(params: StartNextRequest): Promise<CandidateCollectionDetailVO | null> {
  return unwrap<CandidateCollectionDetailVO | null>(
    apiClient.post(`${base}/queue/start-next`, params, collectionRequestConfig),
  );
}

export function submitFingerprint(params: SubmitFingerprintRequest): Promise<FingerprintTraceVO> {
  return unwrap<FingerprintTraceVO>(
    apiClient.post(`${base}/fingerprint/submit`, params, collectionRequestConfig),
  );
}

export function submitSignature(params: SubmitSignatureRequest): Promise<SignatureRecordVO> {
  return unwrap<SignatureRecordVO>(
    apiClient.post(`${base}/signature/submit`, params, collectionRequestConfig),
  );
}

export function completeCollection(params: CompleteCollectionRequest): Promise<CollectionActionResultVO> {
  return unwrap<CollectionActionResultVO>(
    apiClient.post(`${base}/complete`, params, collectionRequestConfig),
  );
}

export function markPendingRetry(params: MarkPendingRetryRequest): Promise<CollectionActionResultVO> {
  return unwrap<CollectionActionResultVO>(
    apiClient.post(`${base}/mark-pending-retry`, params, collectionRequestConfig),
  );
}

export function markInfoError(params: MarkInfoErrorRequest): Promise<CollectionActionResultVO> {
  return unwrap<CollectionActionResultVO>(
    apiClient.post(`${base}/mark-info-error`, params, collectionRequestConfig),
  );
}

export function exceptionRelease(params: ExceptionReleaseRequest): Promise<CollectionActionResultVO> {
  return unwrap<CollectionActionResultVO>(
    apiClient.post(`${base}/exception-release`, params, collectionRequestConfig),
  );
}

export function getCollectionDetail(
  taskId: SnowflakeId,
  candidateId: SnowflakeId,
): Promise<CandidateCollectionDetailVO> {
  return unwrap<CandidateCollectionDetailVO>(
    apiClient.get(`${base}/detail/${taskId}/${candidateId}`, collectionRequestConfig),
  );
}

export function getCollectionExceptions(
  params: CollectionExceptionQueryRequest,
): Promise<PageResponse<CollectionExceptionVO>> {
  return unwrap<PageResponse<CollectionExceptionVO>>(
    apiClient.get(`${base}/exceptions`, { params, ...collectionRequestConfig }),
  );
}
