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
  currentPhase?: string;
  status?: string;
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
  taskScopeId?: string;
  levelCode?: string;
  documentComplete?: boolean;
}

export interface PreparationCandidate {
  id: string;
  taskId: string;
  taskScopeId?: string;
  candidateName: string;
  admissionTicketNo?: string;
  idCardNo?: string;
  maskedIdCardNo?: string;
  phone?: string;
  maskedPhone?: string;
  levelCode?: string;
  levelName?: string;
  workTypeId?: string;
  workTypeName?: string;
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
  admissionTicketNo?: string;
  idCardNo: string;
  phone?: string;
  levelCode?: string;
  levelName?: string;
  importSource?: string;
  orgName?: string;
  remark?: string;
  taskScopeId?: string;
  gender?: string;
  education?: string;
  graduateDate?: string;
  companyAddress?: string;
  zipCode?: string;
  workStartDate?: string;
  originalWorkTypeName?: string;
  originalLevelName?: string;
  originalCertNo?: string;
  applyWorkYears?: string;
  companyCreditCode?: string;
  workTypeCode?: string;
  workDirection?: string;
  workResume?: string;
  trainingSummary?: string;
  homeAddress?: string;
  school?: string;
  major?: string;
  nation?: string;
}

export interface PreparationCandidateImportPayload {
  sourceType?: string;
  sourceFileId?: string;
  candidates?: PreparationCandidatePayload[];
  trainingPeopleIds?: number[];
  taskScopeId?: string;
  examSessionIds?: string[];
  defaultParticipation?: 'ASSIGNED' | 'EXEMPT' | 'CANCELLED';
  remark?: string;
}

export type CertificationExamType = 'THEORY' | 'PRACTICE';
export type CertificationExamSessionStatus = 'DRAFT' | 'CONFIGURED' | 'IN_PROGRESS' | 'ENDED';
export type CandidateParticipationStatus = 'ASSIGNED' | 'EXEMPT' | 'CANCELLED';

export interface CertificationExamSession {
  id: string;
  taskId: string;
  taskScopeId?: string;
  examType: CertificationExamType;
  sessionName: string;
  examDate?: string;
  startTime?: string;
  endTime?: string;
  examSiteId?: string;
  examVenue?: string;
  status: CertificationExamSessionStatus;
  identityCheckRequired: boolean;
  fingerprintRequired: boolean;
  signatureRequired: boolean;
  remark?: string;
}

export interface CertificationExamSessionCandidate {
  candidateId: string;
  candidateName: string;
  idCardNo?: string;
  admissionTicketNo?: string;
  participationStatus: CandidateParticipationStatus;
  collectionStatus: string;
}

export interface CertificationExamSessionCandidateAssignPayload {
  candidateIds: string[];
}

export interface CertificationExamSessionPayload {
  taskScopeId?: string;
  examType: CertificationExamType;
  sessionName: string;
  examDate?: string;
  startTime?: string;
  endTime?: string;
  examSiteId?: string;
  examVenue?: string;
  status?: CertificationExamSessionStatus;
  identityCheckRequired: boolean;
  fingerprintRequired: boolean;
  signatureRequired: boolean;
  remark?: string;
}

export interface CertificationCandidateExamSession {
  id: string;
  candidateId: string;
  examSessionId: string;
  examType: CertificationExamType;
  sessionName: string;
  participationStatus: CandidateParticipationStatus;
  collectionStatus: string;
  collectionSourceSessionId?: string;
  exemptReason?: string;
  remark?: string;
}

export interface CertificationCandidateExamSessionPayload {
  participationStatus: CandidateParticipationStatus;
  collectionStatus?: string;
  collectionSourceSessionId?: string;
  exemptReason?: string;
  remark?: string;
}

export interface PreparationCandidateStagingQuery extends PreparationPageQuery {
  keyword?: string;
  promoteStatus?: string;
  sourceType?: string;
}

export interface PreparationCandidateStaging {
  id: string;
  taskId?: string;
  candidateId?: string;
  sourceType?: string;
  promoteStatus?: string;
  promoteMessage?: string;
  candidateName: string;
  gender?: string;
  birthDate?: string;
  education?: string;
  graduateDate?: string;
  school?: string;
  major?: string;
  nation?: string;
  idCardNo?: string;
  phone?: string;
  homeAddress?: string;
  companyName?: string;
  companyAddress?: string;
  zipCode?: string;
  companyCreditCode?: string;
  industrialPark?: string;
  workStartDate?: string;
  originalWorkTypeName?: string;
  originalLevelName?: string;
  originalCertNo?: string;
  applyWorkTypeName?: string;
  certWorkTypeName?: string;
  workDirection?: string;
  workTypeCode?: string;
  applyLevelName?: string;
  certLevelName?: string;
  applyWorkYears?: string;
  workResume?: string;
  trainingSummary?: string;
  remark?: string;
  createdAt?: string;
}

export interface PreparationCandidateStagingPromotePayload {
  stagingIds: string[];
}

export interface PreparationStagingPromoteError {
  stagingId: string;
  candidateName?: string;
  idCardNo?: string;
  errorMessage?: string;
}

export interface PreparationCandidateStagingPromoteResult {
  total: number;
  successCount: number;
  failedCount: number;
  candidates: PreparationCandidate[];
  errors: PreparationStagingPromoteError[];
}

export interface PreparationDocumentGeneratePayload {
  docCode: string;
}

export interface PreparationImportBatch {
  id: string;
  taskId: string;
  taskScopeId?: string;
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

export interface PreparationChecklistItem {
  id: string;
  taskId: string;
  itemNo: number;
  itemName: string;
  quantityText?: string;
  checked: boolean;
  confirmerTaskStaffId?: string;
  confirmerName?: string;
  confirmedAt?: string;
  remark?: string;
}

export interface PreparationChecklist {
  taskId: string;
  taskName?: string;
  totalCount: number;
  checkedCount: number;
  allChecked: boolean;
  confirmerTaskStaffId?: string;
  confirmerName?: string;
  confirmedAt?: string;
  items: PreparationChecklistItem[];
}

export interface PreparationChecklistUpdatePayload {
  checkedItemIds: string[];
  confirmerTaskStaffId?: string;
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
  equipmentPhotoFileId?: string;
  equipmentPhotoFileName?: string;
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
  /** @deprecated 旧字段，多个工种以"、"拼接。优先使用 workTypeScopes。 */
  workTypeName?: string;
  workTypeScopes?: PreparationAssessorGroupWorktype[];
  status?: string;
  remark?: string;
  memberCount?: number;
}

export interface PreparationAssessorGroupWorktype {
  scopeId: string;
  workTypeName?: string;
  workTypeId?: string;
  levelCode?: string;
  levelName?: string;
  sortOrder?: number;
}

export interface PreparationAssessorGroupPayload {
  groupName?: string;
  leaderAssessorId?: string;
  assessorIds?: string[];
  /** @deprecated 旧字段。优先使用 workTypeScopeIds。 */
  workTypeName?: string;
  workTypeScopeIds?: string[];
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
