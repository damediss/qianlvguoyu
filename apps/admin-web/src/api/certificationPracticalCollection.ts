import { apiClient, unwrap } from './client';
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

export function exportQrCodes(taskId: SnowflakeId): Promise<unknown> {
  return unwrap<unknown>(
    apiClient.get(`${base}/tasks/${taskId}/qrcodes/export`, collectionRequestConfig),
  );
}

export function matchByToken(params: MatchByTokenRequest): Promise<CandidateMatchVO> {
  return unwrap<CandidateMatchVO>(
    apiClient.post(`${base}/match-by-token`, params, collectionRequestConfig),
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
