export interface PreparationPageQuery {
  page?: number;
  pageSize?: number;
}

export interface PreparationTaskQuery extends PreparationPageQuery {
  keyword?: string;
  status?: string;
  currentPhase?: string;
}

export interface PreparationTaskOverview {
  id: string;
  taskNo: string;
  taskName: string;
  batchName?: string;
  workTypeName?: string;
  levelName?: string;
  planStartDate?: string;
  planEndDate?: string;
  preparationStatus?: string;
  candidateCount: number;
  documentCompletionRate: number;
  materialStatus?: string;
  examSiteCount: number;
  examStaffCount: number;
  assessorCount: number;
  lastUpdatedAt?: string;
}

export interface PreparationCandidateQuery extends PreparationPageQuery {
  keyword?: string;
  status?: string;
  levelCode?: string;
}

export interface PreparationCandidate {
  id: string;
  taskId: string;
  candidateName: string;
  idCardNo?: string;
  maskedIdCardNo?: string;
  phone?: string;
  maskedPhone?: string;
  levelCode?: string;
  levelName?: string;
  importSource?: string;
  orgName?: string;
  status?: string;
  documentCompletedCount?: number;
  documentRequiredCount?: number;
  documentComplete?: boolean;
  documentUploadedCount?: number;
  documentMissingCount?: number;
  documentTotalCount?: number;
  createdAt?: string;
}

export interface PreparationCandidatePayload {
  candidateName: string;
  idCardNo: string;
  phone?: string;
  levelCode?: string;
  levelName?: string;
  importSource?: string;
  orgName?: string;
  remark?: string;
}

export interface PreparationCandidateImportPayload {
  sourceType?: string;
  sourceFileId?: string;
  candidates: PreparationCandidatePayload[];
  remark?: string;
}

export interface PreparationImportBatch {
  id: string;
  taskId: string;
  batchNo: string;
  sourceType?: string;
  totalCount?: number;
  successCount?: number;
  failedCount?: number;
  duplicateCount?: number;
  status?: string;
  completedAt?: string;
}

export interface PreparationImportError {
  id: string;
  batchId: string;
  taskId: string;
  rowNo?: number;
  candidateName?: string;
  idCardNo?: string;
  phone?: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface PreparationDocumentQuery extends PreparationPageQuery {
  candidateId?: string;
  keyword?: string;
  status?: string;
}

export interface PreparationCandidateDocument {
  id: string;
  taskId: string;
  candidateId: string;
  candidateName?: string;
  maskedIdCardNo?: string;
  docCode: string;
  docName: string;
  required?: boolean;
  fileId?: string;
  fileName?: string;
  fileType?: string;
  versionNo?: number;
  status?: string;
  uploadedAt?: string;
  reviewedAt?: string;
  reviewComment?: string;
}

export interface PreparationCandidateDocumentPayload {
  candidateId: string;
  docCode: string;
  docName: string;
  required?: boolean;
  fileId?: string;
  fileName?: string;
  fileType?: string;
  remark?: string;
}

export interface PreparationDocumentReviewPayload {
  status: string;
  reviewComment?: string;
}

export interface PreparationMaterialQuery extends PreparationPageQuery {
  status?: string;
}

export interface PreparationMaterialBatch {
  id: string;
  taskId: string;
  batchNo: string;
  batchType?: string;
  generateScope?: string;
  candidateCount?: number;
  successCount?: number;
  failedCount?: number;
  status?: string;
  packageFileId?: string;
  packageFileName?: string;
  items?: PreparationMaterialItem[];
}

export interface PreparationMaterialItem {
  id: string;
  taskId: string;
  batchId?: string;
  candidateId?: string;
  itemCode?: string;
  itemName?: string;
  required?: boolean;
  fileId?: string;
  fileName?: string;
  versionNo?: number;
  status?: string;
}

export interface PreparationMaterialItemPayload {
  candidateId?: string;
  itemCode?: string;
  itemName?: string;
  required?: boolean;
  fileId?: string;
  fileName?: string;
  remark?: string;
}

export interface PreparationMaterialGeneratePayload {
  generateScope?: string;
  candidateIds?: string[];
  remark?: string;
}

export interface PreparationExamSiteQuery extends PreparationPageQuery {
  keyword?: string;
  siteType?: string;
  status?: string;
}

export interface PreparationExamSite {
  id: string;
  siteName: string;
  siteType?: string;
  address: string;
  seatCount?: number;
  filingNo?: string;
  contactName?: string;
  contactPhone?: string;
  status?: string;
  remark?: string;
}

export interface PreparationExamSitePayload {
  siteName: string;
  siteType?: string;
  address: string;
  seatCount?: number;
  filingNo?: string;
  contactName?: string;
  contactPhone?: string;
  status?: string;
  equipmentCondition?: string;
  remark?: string;
}

export interface PreparationTaskExamSite {
  id: string;
  taskId: string;
  siteId: string;
  siteName: string;
  siteType?: string;
  address?: string;
  seatCount?: number;
  examDate?: string;
  startTime?: string;
  endTime?: string;
  examType?: string;
  arrangedCount?: number;
  contactName?: string;
  contactPhone?: string;
  status?: string;
}

export interface PreparationTaskExamSitePayload {
  siteId: string;
  examDate?: string;
  startTime?: string;
  endTime?: string;
  examType?: string;
  arrangedCount?: number;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
}

export interface PreparationTaskExamStaff {
  id: string;
  taskId: string;
  staffId: string;
  staffName: string;
  phone?: string;
  maskedPhone?: string;
  status?: string;
  taskRole?: string;
  roleName?: string;
  isLeader?: boolean;
  remark?: string;
}

export interface PreparationTaskExamStaffPayload {
  staffId: string;
  taskRole?: string;
  roleName?: string;
  remark?: string;
}

export interface PreparationTaskEquipment {
  id: string;
  taskId: string;
  equipmentId: string;
  equipmentName: string;
  equipmentNo: string;
  equipmentQuantity: number;
  status?: string;
  remark?: string;
}

export interface PreparationTaskEquipmentPayload {
  equipmentId: string;
  remark?: string;
}

export interface PreparationAssessorQuery extends PreparationPageQuery {
  keyword?: string;
  status?: string;
}

export interface PreparationAssessor {
  id: string;
  userId?: string;
  assessorName?: string;
  certificateNo?: string;
  workTypeScope?: string;
  levelScope?: string;
  validUntil?: string;
  phone?: string;
  status?: string;
  remark?: string;
}

export interface PreparationAssessorPayload {
  userId?: string;
  assessorName?: string;
  certificateNo?: string;
  workTypeScope?: string;
  levelScope?: string;
  validUntil?: string;
  phone?: string;
  status?: string;
  remark?: string;
}

export interface PreparationAssessorGroup {
  id: string;
  taskId: string;
  groupName?: string;
  leaderAssessorId?: string;
  leaderAssessorName?: string;
  workTypeName?: string;
  status?: string;
  remark?: string;
}

export interface PreparationAssessorGroupPayload {
  groupName?: string;
  leaderAssessorId?: string;
  workTypeName?: string;
  status?: string;
  remark?: string;
}

export interface PreparationTaskAssessor {
  id: string;
  taskId: string;
  groupId: string;
  assessorId: string;
  assessorName?: string;
  certificateNo?: string;
  taskRole?: string;
  roleName?: string;
  workTypeScope?: string;
  validUntil?: string;
  workStatus?: string;
}

export interface PreparationTaskAssessorPayload {
  assessorId: string;
  taskRole?: string;
  roleName?: string;
  remark?: string;
}

export interface PreparationAssessorDispatchPayload {
  subject?: string;
  title?: string;
  assessmentFileId?: string;
  assessmentFileName?: string;
  durationMin?: number;
  dueDate?: string;
  remark?: string;
}

export interface PreparationAssessorWorkbenchQuery extends PreparationPageQuery {
  assessorId?: string;
  /** 视角：assessor(我的任务)/leader(组长审批) */
  view?: 'assessor' | 'leader';
  status?: string;
}

export interface PreparationScore {
  id?: string;
  stepNo?: number;
  stepName?: string;
  maxScore?: number;
  score?: number;
  remark?: string;
}

export interface PreparationAssessorTask {
  id: string;
  taskId: string;
  taskName?: string;
  groupId: string;
  groupName?: string;
  assessorId: string;
  assessorName?: string;
  leaderAssessorId?: string;
  leaderAssessorName?: string;
  subject?: string;
  title?: string;
  assessmentFileId?: string;
  assessmentFileName?: string;
  durationMin?: number;
  dueDate?: string;
  status?: string;
  progressNote?: string;
  totalScore?: number;
  submittedAt?: string;
  reviewedAt?: string;
  reviewNote?: string;
  scores?: PreparationScore[];
}

export interface PreparationAssessorTaskDraftPayload {
  assessorId?: string;
  progressNote?: string;
  scores?: PreparationScore[];
}

export interface PreparationAssessorTaskReviewPayload {
  reviewerAssessorId?: string;
  reviewStatus?: string;
  reviewNote?: string;
}

export interface PreparationCheckItem {
  code: string;
  name: string;
  passed: boolean;
  message?: string;
}

export interface PreparationCompleteCheck {
  passed: boolean;
  items: PreparationCheckItem[];
}
