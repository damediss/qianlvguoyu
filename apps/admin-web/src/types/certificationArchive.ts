export interface ArchivePageQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  archiveStatus?: string;
  archiveId?: string;
}

export interface CertificationArchiveTask {
  taskId: string;
  taskNo: string;
  taskName: string;
  batchName?: string;
  workTypeName?: string;
  levelName?: string;
  completedAt?: string;
  candidateCount: number;
  taskStatus: string;
  archiveId?: string;
  basicArchived: boolean;
  materialArchived: boolean;
  archiveStatus: string;
  archivedAt?: string;
  lastOperatorName?: string;
}

export interface CertificationArchiveRecord {
  id: string;
  taskId: string;
  taskNo: string;
  taskName: string;
  batchName?: string;
  workTypeName?: string;
  levelName?: string;
  completedAt?: string;
  candidateCount: number;
  archiveStatus: string;
  basicArchived: boolean;
  materialArchived: boolean;
  archivedBy?: string;
  archivedAt?: string;
  lastOperatorName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CertificationArchiveBasicSnapshot {
  id: string;
  archiveId: string;
  completedAt?: string;
  candidateCount: number;
  taskJson?: string;
  scopeJson?: string;
  examStaffJson?: string;
  deviceJson?: string;
  roomJson?: string;
  evaluatorJson?: string;
  createdAt?: string;
}

export interface CertificationArchiveMaterial {
  id: string;
  archiveId: string;
  materialType: string;
  materialGroup: string;
  materialName: string;
  fileName: string;
  fileSize: number;
  fileHash?: string;
  contentType?: string;
  fileStatus: string;
  uploadedBy?: string;
  uploadedAt?: string;
}

export interface CertificationArchiveCandidateMaterial {
  id: string;
  archiveCandidateId: string;
  materialType: string;
  materialGroup: string;
  fileName: string;
  fileSize: number;
  fileHash?: string;
  contentType?: string;
}

export interface CertificationArchiveCandidate {
  id: string;
  archiveId: string;
  candidateNo: string;
  name: string;
  idCard: string;
  maskedIdCard: string;
  folderName: string;
  materials: CertificationArchiveCandidateMaterial[];
}

export interface CertificationArchiveImportBatch {
  id: string;
  archiveId: string;
  batchNo: string;
  importType: string;
  zipFileName: string;
  status: string;
  totalCandidates: number;
  successCount: number;
  errorCount: number;
  statisticsJson?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CertificationArchiveImportError {
  id: string;
  batchId: string;
  candidateNo?: string;
  name?: string;
  idCard?: string;
  maskedIdCard?: string;
  errorType: string;
  errorMessage: string;
  createdAt?: string;
}

export interface CertificationArchiveCheckItem {
  code: string;
  name: string;
  passed: boolean;
  message: string;
}

export interface CertificationArchiveCompleteCheck {
  passed: boolean;
  items: CertificationArchiveCheckItem[];
}

export interface CertificationArchiveDetail {
  record: CertificationArchiveRecord;
  basicSnapshot?: CertificationArchiveBasicSnapshot;
  materials: CertificationArchiveMaterial[];
  importBatches: CertificationArchiveImportBatch[];
}
