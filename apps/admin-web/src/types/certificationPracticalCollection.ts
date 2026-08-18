export type QrIssueStatus = 'NOT_ISSUED' | 'ISSUED';

export type CollectionOverallStatus =
  | 'NOT_ISSUED'
  | 'ISSUED'
  | 'NOT_ARRIVED'
  | 'ARRIVED'
  | 'QUEUED'
  | 'COLLECTING'
  | 'PENDING_RETRY'
  | 'INFO_ERROR'
  | 'EXCEPTION_RELEASED'
  | 'COMPLETED'
  | 'CANCELLED';

export type CollectionQueueStatus =
  | 'NOT_QUEUED'
  | 'QUEUED'
  | 'COLLECTING'
  | 'COMPLETED'
  | 'REMOVED'
  | 'CANCELLED';

export type FingerprintStatus =
  | 'NOT_STARTED'
  | 'CAPTURING'
  | 'DONE'
  | 'FAILED'
  | 'EXCEPTION_RELEASED';

export type SignatureStatus =
  | 'NOT_STARTED'
  | 'SIGNING'
  | 'DONE'
  | 'FAILED'
  | 'EXCEPTION_RELEASED';

export type CollectionExceptionStatus =
  | 'NONE'
  | 'PENDING_RETRY'
  | 'INFO_ERROR'
  | 'RELEASED'
  | 'RESOLVED';

export type CollectionReleaseScope =
  | 'FINGERPRINT'
  | 'SIGNATURE'
  | 'ALL'
  | 'INFO'
  | 'OTHER';

export type CollectionMatchMethod =
  | 'QR_CODE'
  | 'ID_CARD_LAST4'
  | 'NAME'
  | 'ADMISSION_NO'
  | 'MANUAL';

export type FingerprintCollectStatus = 'SUCCESS' | 'FAILED' | 'EXCEPTION';

export type SignerType = 'CANDIDATE' | 'EVALUATOR' | 'LEADER';

export type CollectionExceptionType =
  | 'DEVICE_ERROR'
  | 'FINGERPRINT_UNAVAILABLE'
  | 'SIGNATURE_UNAVAILABLE'
  | 'INFO_ERROR'
  | 'OTHER';

export type SignatureRecordStatus = 'ACTIVE' | 'REVOKED';

export type SnowflakeId = string | number;

export type CollectionEntrySource = 'QR_SCAN' | 'CANDIDATE_ROW' | 'SEARCH';

export interface CandidateCollectionContext {
  taskId: SnowflakeId;
  candidateId?: SnowflakeId;
  staffId?: SnowflakeId;
  candidateName: string;
  idCardNo?: string;
  collectionStatus?: CollectionOverallStatus | string;
  entrySource: CollectionEntrySource;
}

export const QrIssueStatusText: Record<QrIssueStatus, string> = {
  NOT_ISSUED: '未发放',
  ISSUED: '已发放',
};

export const CollectionOverallStatusText: Record<CollectionOverallStatus, string> = {
  NOT_ISSUED: '未发放',
  ISSUED: '已发放',
  NOT_ARRIVED: '未到场',
  ARRIVED: '已到场',
  QUEUED: '已入队',
  COLLECTING: '采集中',
  PENDING_RETRY: '待补采',
  INFO_ERROR: '异常',
  EXCEPTION_RELEASED: '已放行',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
};

export const CollectionQueueStatusText: Record<CollectionQueueStatus, string> = {
  NOT_QUEUED: '未入队',
  QUEUED: '排队中',
  COLLECTING: '采集中',
  COMPLETED: '已完成',
  REMOVED: '已移出',
  CANCELLED: '已取消',
};

export const FingerprintStatusText: Record<FingerprintStatus, string> = {
  NOT_STARTED: '未开始',
  CAPTURING: '采集中',
  DONE: '已采集',
  FAILED: '异常',
  EXCEPTION_RELEASED: '已放行',
};

export const SignatureStatusText: Record<SignatureStatus, string> = {
  NOT_STARTED: '未开始',
  SIGNING: '签名中',
  DONE: '已签名',
  FAILED: '异常',
  EXCEPTION_RELEASED: '已放行',
};

export const CollectionExceptionStatusText: Record<CollectionExceptionStatus, string> = {
  NONE: '无异常',
  PENDING_RETRY: '待补采',
  INFO_ERROR: '异常',
  RELEASED: '已放行',
  RESOLVED: '已处理',
};

export const CollectionReleaseScopeText: Record<CollectionReleaseScope, string> = {
  FINGERPRINT: '指纹',
  SIGNATURE: '签名',
  ALL: '全部',
  INFO: '信息',
  OTHER: '其他',
};

export const CollectionMatchMethodText: Record<CollectionMatchMethod, string> = {
  QR_CODE: '二维码',
  ID_CARD_LAST4: '身份证后四位',
  NAME: '姓名',
  ADMISSION_NO: '准考证号',
  MANUAL: '人工处理',
};

export const FingerprintCollectStatusText: Record<FingerprintCollectStatus, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  EXCEPTION: '异常',
};

export const SignerTypeText: Record<SignerType, string> = {
  CANDIDATE: '考生',
  EVALUATOR: '考评员',
  LEADER: '组长',
};

export const CollectionExceptionTypeText: Record<CollectionExceptionType, string> = {
  DEVICE_ERROR: '设备异常',
  FINGERPRINT_UNAVAILABLE: '无法采集指纹',
  SIGNATURE_UNAVAILABLE: '无法签名',
  INFO_ERROR: '信息异常',
  OTHER: '其他',
};

export interface CollectionTaskQueryRequest {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  workTypeName?: string;
  levelCode?: string;
}

export interface CollectionTaskVO {
  taskId: SnowflakeId;
  examSessionId?: SnowflakeId;
  taskNo?: string;
  taskName: string;
  sessionName?: string;
  examType?: 'THEORY' | 'PRACTICE';
  projectName?: string;
  workType?: string;
  level?: string;
  examDate?: string;
  candidateTotal: number;
  qrIssuedCount: number;
  arrivedCount: number;
  queuedCount: number;
  collectingCount: number;
  completedCount: number;
  pendingRetryCount: number;
  exceptionCount: number;
  /** 任务管理中配置的工种/等级范围，保留每一行的对应关系。 */
  workTypeLevel?: string;
}

export interface CollectionStatsVO {
  candidateTotal: number;
  qrIssuedCount: number;
  arrivedCount: number;
  queuedCount: number;
  collectingCount: number;
  completedCount: number;
  pendingRetryCount: number;
  infoErrorCount: number;
  exceptionReleasedCount: number;
  exceptionCount: number;
}

export interface CollectionWorkbenchVO {
  task: CollectionTaskVO;
  stats: CollectionStatsVO;
  candidates: CandidateCollectionItemVO[];
  queue: QueueItemVO[];
  currentCandidate?: CandidateCollectionDetailVO | null;
}

export interface MatchByTokenRequest {
  taskId: SnowflakeId;
  collectToken: string;
}

export interface SearchCandidateRequest {
  taskId: SnowflakeId;
  keyword: string;
}

export interface ScanMatchCurrentRequest {
  token: string;
}

export interface ScanMatchResultVO {
  alreadyCompleted: boolean;
  currentCandidate: CandidateCollectionDetailVO;
  message?: string;
}

export interface CandidateMatchVO {
  candidateId: SnowflakeId;
  taskId: SnowflakeId;
  name: string;
  idCardLast4?: string;
  maskedIdCardNo?: string;
  unitName?: string;
  workType?: string;
  level?: string;
  admissionNo?: string;
  avatarFileId?: SnowflakeId;
  currentStatus: CollectionOverallStatus;
  currentStatusLabel?: string;
  alreadyQueued: boolean;
  queueNo?: number;
  qrIssueStatus?: QrIssueStatus;
  qrIssueStatusLabel?: string;
  matchMethod?: CollectionMatchMethod;
  matchMethodLabel?: string;
  documentStatus?: 'COMPLETED' | 'INCOMPLETE' | 'INVALID' | string;
  missingDocumentCodes?: string[];
  invalidDocumentCodes?: string[];
  collectionEligible?: boolean;
}

export interface CandidateCollectionItemVO {
  candidateId: SnowflakeId;
  taskId: SnowflakeId;
  name: string;
  idCardLast4?: string;
  maskedIdCardNo?: string;
  unitName?: string;
  workType?: string;
  level?: string;
  admissionNo?: string;
  overallStatus: CollectionOverallStatus;
  overallStatusLabel?: string;
  queueStatus: CollectionQueueStatus;
  queueStatusLabel?: string;
  fingerprintStatus: FingerprintStatus;
  fingerprintStatusLabel?: string;
  signatureStatus: SignatureStatus;
  signatureStatusLabel?: string;
  exceptionStatus: CollectionExceptionStatus;
  exceptionStatusLabel?: string;
  queueNo?: number;
  documentStatus?: 'COMPLETED' | 'INCOMPLETE' | 'INVALID' | string;
  missingDocumentCodes?: string[];
  invalidDocumentCodes?: string[];
  collectionEligible?: boolean;
}

export interface JoinQueueRequest {
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  matchMethod: CollectionMatchMethod;
}

export interface RemoveQueueRequest {
  taskId: SnowflakeId;
  queueId: SnowflakeId;
  removeReason?: string;
}

export interface StartNextRequest {
  taskId: SnowflakeId;
}

export interface QueueItemVO {
  queueId: SnowflakeId;
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  queueNo: number;
  name?: string;
  idCardLast4?: string;
  unitName?: string;
  currentStatus?: CollectionOverallStatus;
  currentStatusLabel?: string;
  queueStatus: CollectionQueueStatus;
  queueStatusLabel?: string;
  matchMethod?: CollectionMatchMethod;
  matchMethodLabel?: string;
  joinedAt: string;
  startedAt?: string;
}

export interface SubmitFingerprintRequest {
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  sessionId?: SnowflakeId;
  deviceSn?: string;
  deviceModel?: string;
  deviceVendorId?: string;
  deviceProductId?: string;
  sdkDeviceType?: string;
  adapterType?: 'MOCK' | 'WEB' | 'ANDROID_BRIDGE';
  collectStatus: FingerprintCollectStatus;
  qualityScore?: number | null;
  durationMs?: number | null;
  errorCode?: string;
  exceptionReason?: string;
  collectedAt: string;
  fingerprintImageFileId?: SnowflakeId;
  fingerprintImageHash?: string;
  fingerprintImageFormat?: string;
  fingerprintImageCapturedAt?: string;
  requestId?: string;
}

export interface FingerprintTraceVO {
  id: SnowflakeId;
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  sessionId?: SnowflakeId;
  deviceSn?: string;
  deviceModel?: string;
  deviceVendorId?: string;
  deviceProductId?: string;
  sdkDeviceType?: string;
  adapterType?: 'MOCK' | 'WEB' | 'ANDROID_BRIDGE';
  collectStatus: FingerprintCollectStatus;
  collectStatusLabel?: string;
  qualityScore?: number | null;
  durationMs?: number | null;
  errorCode?: string;
  exceptionReason?: string;
  collectedAt: string;
  fingerprintImageFileId?: SnowflakeId;
  fingerprintImageHash?: string;
  fingerprintImageFormat?: string;
  fingerprintImageCapturedAt?: string;
}

export interface SubmitSignatureRequest {
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  sessionId?: SnowflakeId;
  signatureFileId: SnowflakeId;
  signatureHash: string;
  signerType: SignerType;
  signerName: string;
  signedAt: string;
  deviceInfo?: string;
}

export interface SignatureRecordVO {
  id: SnowflakeId;
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  sessionId?: SnowflakeId;
  signatureFileId: SnowflakeId;
  signatureHash: string;
  signerType: SignerType;
  signerTypeLabel?: string;
  signerName: string;
  signedAt: string;
  recordStatus: SignatureRecordStatus;
  recordStatusLabel?: string;
}

export interface CompleteCollectionRequest {
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  sessionId?: SnowflakeId;
  remark?: string;
}

export interface MarkPendingRetryRequest {
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  reason: string;
}

export interface MarkInfoErrorRequest {
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  reason: string;
}

export interface ExceptionReleaseRequest {
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  exceptionType: CollectionExceptionType;
  releaseScope: CollectionReleaseScope;
  reason: string;
  deviceSn?: string;
  deviceStatus?: string;
}

export interface CollectionActionResultVO {
  taskId: SnowflakeId;
  candidateId?: SnowflakeId;
  overallStatus?: CollectionOverallStatus;
  overallStatusLabel?: string;
  queueStatus?: CollectionQueueStatus;
  queueStatusLabel?: string;
  fingerprintStatus?: FingerprintStatus;
  fingerprintStatusLabel?: string;
  signatureStatus?: SignatureStatus;
  signatureStatusLabel?: string;
  exceptionStatus?: CollectionExceptionStatus;
  exceptionStatusLabel?: string;
  queueItem?: QueueItemVO;
  message?: string;
}

export interface CollectionPdfExportVO {
  fileId: SnowflakeId;
  fileName: string;
  downloadUrl: string;
}

export interface CollectionExceptionQueryRequest {
  page?: number;
  pageSize?: number;
  taskId?: SnowflakeId;
  candidateId?: SnowflakeId;
  exceptionType?: CollectionExceptionType;
  exceptionStatus?: CollectionExceptionStatus;
  keyword?: string;
}

export interface CollectionExceptionVO {
  id: SnowflakeId;
  taskId: SnowflakeId;
  candidateId: SnowflakeId;
  candidateName?: string;
  idCardLast4?: string;
  exceptionType: CollectionExceptionType;
  exceptionTypeLabel?: string;
  exceptionStatus: CollectionExceptionStatus;
  exceptionStatusLabel?: string;
  releaseScope?: CollectionReleaseScope;
  releaseScopeLabel?: string;
  reason: string;
  deviceSn?: string;
  deviceStatus?: string;
  handledBy?: SnowflakeId;
  handledAt?: string;
  createdAt?: string;
}

export interface CandidateCollectionDetailVO {
  statusId: SnowflakeId;
  candidateId: SnowflakeId;
  taskId: SnowflakeId;
  sessionId?: SnowflakeId;
  name: string;
  idCardLast4?: string;
  maskedIdCardNo?: string;
  unitName?: string;
  workType?: string;
  level?: string;
  admissionNo?: string;
  avatarFileId?: SnowflakeId;
  hasCollectToken: boolean;
  qrIssueStatus?: QrIssueStatus;
  qrIssueStatusLabel?: string;
  overallStatus: CollectionOverallStatus;
  overallStatusLabel?: string;
  queueStatus: CollectionQueueStatus;
  queueStatusLabel?: string;
  fingerprintStatus: FingerprintStatus;
  fingerprintStatusLabel?: string;
  signatureStatus: SignatureStatus;
  signatureStatusLabel?: string;
  exceptionStatus: CollectionExceptionStatus;
  exceptionStatusLabel?: string;
  queueNo?: number;
  arrivedAt?: string;
  collectingStartedAt?: string;
  completedAt?: string;
  currentQueue?: QueueItemVO | null;
  latestFingerprint?: FingerprintTraceVO | null;
  latestSignature?: SignatureRecordVO | null;
  exceptions: CollectionExceptionVO[];
  documentStatus?: 'COMPLETED' | 'INCOMPLETE' | 'INVALID' | string;
  missingDocumentCodes?: string[];
  invalidDocumentCodes?: string[];
  collectionEligible?: boolean;
}

export interface BatchJoinQueueResultVO {
  successCount: number;
  failureCount: number;
  failures: Array<{
    candidateId: SnowflakeId;
    candidateName?: string;
    admissionTicketNo?: string;
    missingDocumentCodes?: string[];
    invalidDocumentCodes?: string[];
    message: string;
  }>;
}
