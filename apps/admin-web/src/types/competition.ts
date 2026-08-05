export type CompetitionId = number | string;

export type CompetitionEventScope = 'platform' | 'enterprise_internal';
export type CompetitionEventStatus = 'draft' | 'published' | 'closed' | 'archived';
export type CompetitionStagePlanStatus = 'draft' | 'confirmed';

export interface CompetitionEvent {
  id: CompetitionId;
  name: string;
  code: string;
  playerEntryCode?: string | null;
  scope: CompetitionEventScope;
  ownerOrgId?: CompetitionId | null;
  status: CompetitionEventStatus;
  stagePlanStatus?: CompetitionStagePlanStatus | null;
  stagePlanConfirmedBy?: CompetitionId | null;
  stagePlanConfirmedAt?: string | null;
  registrationAuditRequired: number;
  startAt?: string | null;
  endAt?: string | null;
  description?: string | null;
  createdBy?: CompetitionId | null;
  createdAt?: string | null;
  updatedBy?: CompetitionId | null;
  updatedAt?: string | null;
}

export interface CompetitionEventPageQuery {
  page: number;
  pageSize: number;
  keyword?: string;
  status?: CompetitionEventStatus;
}

export interface CompetitionEventCreateRequest {
  name: string;
  code: string;
  scope: CompetitionEventScope;
  startAt?: string;
  endAt?: string;
  registrationAuditRequired: number;
  description?: string;
}

export interface CompetitionEventUpdateRequest {
  name: string;
  startAt?: string;
  endAt?: string;
  registrationAuditRequired: number;
  description?: string;
}

export interface CompetitionEventPublishResult {
  eventId: CompetitionId;
  eventCode: string;
  playerEntryCode?: string | null;
  eventScope: CompetitionEventScope;
  ownerOrgId?: CompetitionId | null;
  previousStatus: CompetitionEventStatus;
  status: CompetitionEventStatus;
}

export type CompetitionEventOrgStatus = 'invited' | 'active' | 'suspended' | 'closed';

export interface CompetitionEventOrg {
  id: CompetitionId;
  eventId: CompetitionId;
  orgId: CompetitionId;
  orgName?: string | null;
  orgCode?: string | null;
  quota?: number | null;
  status: CompetitionEventOrgStatus;
  importPermission: number;
  createdAt?: string | null;
}

export interface CompetitionEventOrgPageQuery {
  page: number;
  pageSize: number;
  status?: CompetitionEventOrgStatus;
}

export interface CompetitionEventOrgCreateRequest {
  orgId?: CompetitionId;
  orgName?: string;
  quota?: number;
  importPermission?: number;
}

export interface CompetitionEventOrgStatusUpdateRequest {
  status: Extract<CompetitionEventOrgStatus, 'active' | 'suspended'>;
}

export interface CompetitionEventOrgImportPermissionUpdateRequest {
  importPermission: 0 | 1;
}

export type CompetitionRosterImportStatus = 'validating' | 'validation_failed' | 'pending_confirm' | 'confirmed';
export type CompetitionRosterValidationStatus = 'valid' | 'invalid';

export interface CompetitionRosterItem {
  id: CompetitionId;
  batchId: CompetitionId;
  employeeNo?: string | null;
  name?: string | null;
  workType?: string | null;
  level?: string | null;
  validationStatus: CompetitionRosterValidationStatus;
  errorMessage?: string | null;
}

export interface CompetitionRosterImportDetail {
  id: CompetitionId;
  batchId?: CompetitionId;
  eventId: CompetitionId;
  orgId: CompetitionId;
  orgName?: string | null;
  eventWorkTypeId?: CompetitionId | null;
  batchNo: string;
  sourceFileId?: CompetitionId | null;
  status: CompetitionRosterImportStatus;
  totalCount: number;
  validCount: number;
  invalidCount: number;
  confirmedBy?: CompetitionId | null;
  confirmedAt?: string | null;
  createdAt?: string | null;
  createdBy?: CompetitionId | null;
  summary?: string | null;
  items: import('./api').PageResponse<CompetitionRosterItem>;
}

export interface CompetitionRosterImport {
  id: CompetitionId;
  batchId: CompetitionId;
  batchNo: string;
  eventId: CompetitionId;
  orgId: CompetitionId;
  orgName?: string | null;
  eventWorkTypeId?: CompetitionId | null;
  sourceFileId?: CompetitionId | null;
  status: CompetitionRosterImportStatus;
  totalCount: number;
  validCount: number;
  invalidCount: number;
  confirmedBy?: CompetitionId | null;
  confirmedAt?: string | null;
  createdBy?: CompetitionId | null;
  createdAt?: string | null;
}

export interface CompetitionRosterImportListQuery {
  page: number;
  pageSize: number;
  orgId?: CompetitionId;
  status?: CompetitionRosterImportStatus;
  batchNo?: string;
}

export interface CompetitionRosterImportPageQuery {
  page: number;
  pageSize: number;
}

export interface CompetitionPlayer {
  registrationId: CompetitionId;
  eventId: CompetitionId;
  eventName?: string | null;
  orgId: CompetitionId;
  orgName?: string | null;
  playerId?: CompetitionId | null;
  memberId?: CompetitionId | null;
  rosterItemId?: CompetitionId | null;
  eventWorkTypeId?: CompetitionId | null;
  eventWorkTypeName?: string | null;
  employeeNo?: string | null;
  name?: string | null;
  idCardNo?: string | null;
  mobile?: string | null;
  level?: string | null;
  status?: string | null;
  playerStatus?: string | null;
  createdAt?: string | null;
}

export interface CompetitionPlayerQuery {
  page: number;
  pageSize: number;
  eventId?: CompetitionId;
  orgId?: CompetitionId;
  eventWorkTypeId?: CompetitionId;
  keyword?: string;
  employeeNo?: string;
  name?: string;
  status?: string;
}

export interface CompetitionPlayerCreateRequest {
  eventId: CompetitionId;
  orgId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  employeeNo: string;
  name: string;
  idCardNo: string;
  mobile: string;
  level?: string;
}

export interface CompetitionPlayerUpdateRequest {
  orgId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  employeeNo?: string;
  name?: string;
  idCardNo?: string;
  mobile?: string;
  level?: string;
}

export type CompetitionNoticeStatus = 'draft' | 'published';
export type CompetitionNoticeContentType = 'markdown' | 'pdf';
export type CompetitionNoticeAssetType = 'inline_image' | 'pdf';

export interface CompetitionNoticeAsset {
  assetId: CompetitionId;
  assetType: CompetitionNoticeAssetType;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  sourceUri: string;
}

export interface CompetitionNotice {
  id: CompetitionId;
  eventId: CompetitionId;
  title: string;
  content: string;
  contentType: CompetitionNoticeContentType;
  assets: CompetitionNoticeAsset[];
  version: number;
  status: CompetitionNoticeStatus;
  publishedBy?: CompetitionId | null;
  publishedAt?: string | null;
  createdBy?: CompetitionId | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CompetitionNoticePageQuery {
  page: number;
  pageSize: number;
  status?: CompetitionNoticeStatus;
  contentType?: CompetitionNoticeContentType;
}

export interface CompetitionNoticeCreateRequest {
  title: string;
  content: string;
  contentType?: CompetitionNoticeContentType;
  version: number;
}

export type CompetitionNoticeUpdateRequest = CompetitionNoticeCreateRequest;

export interface CompetitionNoticePublishResult {
  noticeId: CompetitionId;
  eventId: CompetitionId;
  beforeStatus: CompetitionNoticeStatus;
  afterStatus: CompetitionNoticeStatus;
}

export type CompetitionStageStatus =
  | 'draft'
  | 'configuring'
  | 'ready_locked'
  | 'in_progress'
  | 'closed'
  | 'settled'
  | 'result_published';

export interface CompetitionStage {
  id: CompetitionId;
  eventId: CompetitionId;
  name: string;
  stageType: string;
  sortOrder: number;
  startAt?: string | null;
  endAt?: string | null;
  status: CompetitionStageStatus;
  description?: string | null;
}

export type CompetitionStageContentType = 'theory' | 'simulation' | 'practical';
export type CompetitionStageContentStatus = 'enabled' | 'disabled';

export interface CompetitionStageContent {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  contentType: CompetitionStageContentType;
  status: CompetitionStageContentStatus;
  weight?: CompetitionDecimal | null;
  sortOrder: number;
  providerCode?: string | null;
}

export interface CompetitionStageReadinessCheck {
  code: string;
  passed: boolean;
  message: string;
}

export interface CompetitionStageReadiness {
  stageId: CompetitionId;
  ready: boolean;
  checks: CompetitionStageReadinessCheck[];
}

export interface CompetitionStageWorkspace {
  stage: CompetitionStage;
  contents: CompetitionStageContent[];
  readiness: CompetitionStageReadiness;
}

export interface CompetitionStageContentItemRequest {
  contentType: CompetitionStageContentType;
  status?: CompetitionStageContentStatus;
  weight?: CompetitionDecimal | null;
  providerCode?: string;
}

export interface CompetitionStageCreateRequest {
  name?: string;
  stageType: string;
  sortOrder?: number;
  startAt?: string;
  endAt?: string;
  status?: CompetitionStageStatus;
  description?: string;
}

export type CompetitionStageUpdateRequest = CompetitionStageCreateRequest;

export interface CompetitionStageExecutionPlan {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  planVersion: number;
  status: string;
  resourceVersion?: string | null;
  preflightStatus: string;
  scoreRuleId?: CompetitionId | null;
  promotionQuota?: number | null;
  promotionRuleType?: 'FIXED_COUNT' | 'PERCENTAGE' | 'FINAL_PODIUM' | 'LEGACY' | string | null;
  promotionPercentage?: number | null;
  legacyRule?: number | null;
  targetStageId?: CompetitionId | null;
  rosterSnapshotId?: CompetitionId | null;
  resources: CompetitionStagePlanResource[];
}

export interface CompetitionStagePlanResource {
  id: CompetitionId;
  contentType: string;
  resourceType: string;
  resourceId?: CompetitionId | null;
  resourceCode?: string | null;
  status: string;
}

export interface CompetitionStagePlanPreflight {
  planId: CompetitionId;
  readyToPublish: boolean;
  checks: Array<{ code: string; passed: boolean; message: string }>;
}

export interface CompetitionPracticalSession {
  id: CompetitionId;
  planId: CompetitionId;
  sessionCode: string;
  sessionName: string;
  startAt: string;
  endAt: string;
  status: string;
}

export interface CompetitionStageRosterItem {
  id: CompetitionId;
  snapshotId: CompetitionId;
  contestantMemberId: CompetitionId;
  contestantProfileId?: CompetitionId | null;
  registrationId?: CompetitionId | null;
  inclusionStatus: string;
  eligibilityStatus?: string | null;
  reason?: string | null;
}

export interface CompetitionStageRosterSnapshot {
  id: CompetitionId;
  planId: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  snapshotVersion: number;
  sourceType: string;
  sourcePromotionSnapshotId?: CompetitionId | null;
  status: string;
  generatedAt?: string | null;
  publishedAt?: string | null;
  items: CompetitionStageRosterItem[];
}

export interface CompetitionPracticalJudgeMember {
  id: CompetitionId;
  judgeGroupId: CompetitionId;
  memberId: CompetitionId;
  memberRole: 'official' | 'standby' | string;
  status: string;
}

export interface CompetitionPracticalJudgeGroup {
  id: CompetitionId;
  sessionId: CompetitionId;
  planId: CompetitionId;
  groupNo: number;
  groupName: string;
  officialCount: number;
  status: string;
  members: CompetitionPracticalJudgeMember[];
}

export interface CompetitionPracticalAssignment {
  id: CompetitionId;
  planId: CompetitionId;
  sessionId: CompetitionId;
  judgeGroupId: CompetitionId;
  rosterItemId: CompetitionId;
  contestantMemberId: CompetitionId;
  refereeMemberId: CompetitionId;
  workstationCode?: string | null;
  status: string;
  assignedAt?: string | null;
}

export interface CompetitionStageSettlementPreflight {
  planId: CompetitionId;
  ready: boolean;
  checks: Array<{ code: string; passed: boolean; message: string }>;
}

export interface CompetitionStageSettlement {
  id: CompetitionId;
  planId: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  scoreRuleId: CompetitionId;
  settlementVersion: number;
  status: string;
  readinessStatus: string;
  rankingSnapshotId?: CompetitionId | null;
  promotionSnapshotId?: CompetitionId | null;
  publishedAt?: string | null;
  promotionRuleType?: string | null;
  promotionPercentage?: number | null;
  calculatedTargetCount?: number | null;
  referenceStatus?: string | null;
  promotionStatus?: string | null;
  finalPodiumStatus?: string | null;
  items: Array<{
    id: CompetitionId;
    contestantMemberId: CompetitionId;
    totalScore?: CompetitionDecimal | null;
    contestantStatus?: string | null;
    displayRank?: number | null;
    tiedFlag?: number | null;
    promotionStatus?: string | null;
    reason?: string | null;
  }>;
}

export interface CompetitionWorkTypeStageConfig {
  id?: CompetitionId | null;
  eventId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  stageId: CompetitionId;
  stageType: string;
  stageName: string;
  enabled: number;
  sortOrder?: number | null;
  promotionRuleType?: string | null;
  promotionPercentage?: number | null;
  promotionQuota?: number | null;
  legacyRule?: number | null;
}

export interface CompetitionStageReferenceSnapshot {
  id: CompetitionId;
  settlementId: CompetitionId;
  planId: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  targetStageId?: CompetitionId | null;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  promotionRuleType: string;
  promotionQuota?: number | null;
  promotionPercentage?: number | null;
  baseCount: number;
  calculatedTargetCount: number;
  status: 'draft' | 'confirmed' | 'withdrawn' | string;
  confirmedBy?: CompetitionId | null;
  confirmedAt?: string | null;
  items: Array<{
    id: CompetitionId;
    contestantMemberId: CompetitionId;
    sourceRank?: number | null;
    selectionStatus: string;
    reason?: string | null;
  }>;
}

export interface CompetitionStageFinalPodium {
  id: CompetitionId;
  settlementId: CompetitionId;
  planId: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  firstContestantMemberId?: CompetitionId | null;
  secondContestantMemberId?: CompetitionId | null;
  thirdContestantMemberId?: CompetitionId | null;
  specialExplanation?: string | null;
  status: 'draft' | 'confirmed' | 'published' | string;
  confirmedAt?: string | null;
  publishedAt?: string | null;
}

export interface CompetitionEventWorkType {
  id: CompetitionId;
  eventId: CompetitionId;
  workTypeId: CompetitionId;
  workTypeName?: string | null;
  displayName?: string | null;
  status: string;
  sortOrder: number;
  stagePlanStatus?: 'draft' | 'confirmed' | string;
  stagePlanConfirmedBy?: CompetitionId | null;
  stagePlanConfirmedAt?: string | null;
}

export type CompetitionScoreRuleStatus = 'draft' | 'published';

export interface CompetitionScoreRule {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  scoringSystem: string;
  theoryTemplate: string;
  theoryWeight?: CompetitionDecimal | null;
  simulationPracticalWeight?: CompetitionDecimal | null;
  simulationWeight?: CompetitionDecimal | null;
  practicalWeight?: CompetitionDecimal | null;
  status: CompetitionScoreRuleStatus;
  ruleVersion: number;
  publishedAt?: string | null;
  publishedBy?: CompetitionId | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CompetitionScoreRuleCreateRequest {
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  scoringSystem?: string;
  theoryTemplate?: string | null;
  theoryWeight?: CompetitionDecimal | null;
  simulationWeight?: CompetitionDecimal | null;
  practicalWeight?: CompetitionDecimal | null;
  simulationPracticalWeight?: CompetitionDecimal | null;
}

export interface CompetitionScoreRuleUpdateRequest {
  scoringSystem?: string;
  theoryTemplate?: string | null;
  theoryWeight?: CompetitionDecimal | null;
  simulationWeight?: CompetitionDecimal | null;
  practicalWeight?: CompetitionDecimal | null;
  simulationPracticalWeight?: CompetitionDecimal | null;
}

export interface CompetitionScoringReadinessCheck {
  code: string;
  passed: boolean;
  message: string;
}

export interface CompetitionScoringReadiness {
  eventId: CompetitionId;
  stageId?: CompetitionId | null;
  eventWorkTypeId?: CompetitionId | null;
  ready: boolean;
  publishedScoreRuleVersionId?: CompetitionId | null;
  checks: CompetitionScoringReadinessCheck[];
}

export type CompetitionDecimal = number | string;
export type CompetitionScoreSourceType = 'theory' | 'simulation' | 'practical';
export type CompetitionScoreConfirmationStatus = 'pending' | 'confirmed' | 'voided';

export interface CompetitionScoreScopeQuery {
  page: number;
  pageSize: number;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  contestantMemberId?: CompetitionId;
  sourceType?: CompetitionScoreSourceType;
  scoreRuleId?: CompetitionId;
}

export interface CompetitionScoreSource {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  contestantMemberId: CompetitionId;
  sourceType: CompetitionScoreSourceType;
  rawScore: CompetitionDecimal;
  confirmationStatus: CompetitionScoreConfirmationStatus;
  sourceSystem: string;
  sourceReference?: string | null;
  scoreRuleId: CompetitionId;
  revisionNo: number;
  currentFlag: number;
  achievedAt?: string | null;
  confirmedAt?: string | null;
  confirmedBy?: CompetitionId | null;
  createdAt?: string | null;
}

export interface CompetitionScoreSourceRevisionRequest {
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  contestantMemberId: CompetitionId;
  sourceType: CompetitionScoreSourceType;
  rawScore: CompetitionDecimal;
  confirmationStatus: CompetitionScoreConfirmationStatus;
  sourceSystem?: string;
  sourceReference?: string;
  scoreRuleId: CompetitionId;
  achievedAt: string;
}

export interface CompetitionTotalScoreSnapshot {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  contestantMemberId: CompetitionId;
  scoreRuleId: CompetitionId;
  theorySourceId: CompetitionId;
  simulationSourceId: CompetitionId;
  practicalSourceId: CompetitionId;
  theoryScore: CompetitionDecimal;
  simulationScore: CompetitionDecimal;
  practicalScore: CompetitionDecimal;
  simulationPracticalScore: CompetitionDecimal;
  theoryWeight: CompetitionDecimal;
  simulationPracticalWeight: CompetitionDecimal;
  totalScore: CompetitionDecimal;
  calculationVersion: number;
  confirmationStatus: CompetitionScoreConfirmationStatus;
  currentFlag: number;
  calculatedAt?: string | null;
  createdAt?: string | null;
}

export interface CompetitionTotalScoreCalculateRequest {
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  contestantMemberId: CompetitionId;
  scoreRuleId: CompetitionId;
}

export type CompetitionRankingSnapshotStatus = 'draft' | 'confirmed' | 'published';
export type CompetitionContestantResultStatus = 'completed' | 'absent';

export interface CompetitionRankingSnapshot {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  scoreRuleId: CompetitionId;
  snapshotVersion: number;
  status: CompetitionRankingSnapshotStatus;
  generationReason?: string | null;
  generatedAt?: string | null;
  createdAt?: string | null;
}

export interface CompetitionRankingEntry {
  id: CompetitionId;
  rankingSnapshotId: CompetitionId;
  contestantMemberId: CompetitionId;
  totalScoreSnapshotId?: CompetitionId | null;
  totalScore: CompetitionDecimal;
  displayRank?: number | null;
  tieKey?: string | null;
  tiedFlag: number;
  contestantStatus: CompetitionContestantResultStatus;
  sortOrder: number;
  createdAt?: string | null;
}

export interface CompetitionRankingCandidate {
  contestantMemberId: CompetitionId;
  totalScoreSnapshotId?: CompetitionId;
  tieKey?: string;
  contestantStatus: CompetitionContestantResultStatus;
}

export interface CompetitionRankingGenerateRequest {
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  scoreRuleId: CompetitionId;
  generationReason?: string;
  candidates: CompetitionRankingCandidate[];
}

export interface CompetitionRankingGenerateResult {
  snapshot: CompetitionRankingSnapshot;
  entries: CompetitionRankingEntry[];
}

export interface CompetitionTheoryRequirement {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  scoreRuleId: CompetitionId;
  templateCode: string;
  durationMinutes: number;
  singleCount: number;
  singleScore: CompetitionDecimal;
  multiCount: number;
  multiScore: CompetitionDecimal;
  judgeCount: number;
  judgeScore: CompetitionDecimal;
  totalScore: CompetitionDecimal;
  passScore: CompetitionDecimal;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export type CompetitionTheoryRequirementRequest = Omit<
  CompetitionTheoryRequirement,
  'id' | 'eventId' | 'stageId' | 'eventWorkTypeId' | 'scoreRuleId' | 'createdAt' | 'updatedAt'
>;

export interface CompetitionTheoryCourseSource {
  courseId: CompetitionId;
  workTypeId: CompetitionId;
  title: string;
  level?: number | null;
  version?: string | null;
}

export interface CompetitionTheoryPaperSource {
  paperId: CompetitionId;
  planId: CompetitionId;
  courseId: CompetitionId;
  planTitle: string;
  paperTitle: string;
  durationMinutes: number;
  totalScore: CompetitionDecimal;
  passScore: CompetitionDecimal;
  questionCount: number;
}

export interface CompetitionTheoryPaperSnapshot {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  scoreRuleId: CompetitionId;
  requirementId: CompetitionId;
  paperCode: 'A' | 'B';
  versionNo: number;
  sourceCourseId: CompetitionId;
  sourcePlanId: CompetitionId;
  sourcePaperId: CompetitionId;
  sourceTitle: string;
  sourceHash: string;
  durationMinutes: number;
  questionCount: number;
  totalScore: CompetitionDecimal;
  passScore: CompetitionDecimal;
  status: string;
  importedAt?: string | null;
  importedBy?: CompetitionId | null;
  publishedAt?: string | null;
}

export interface CompetitionTheoryRoom {
  id: CompetitionId;
  eventId: CompetitionId;
  name: string;
  address: string;
  capacity: number;
  status: 'enabled' | 'disabled';
  remark?: string | null;
}

export interface CompetitionTheorySessionRoom {
  id: CompetitionId;
  roomId: CompetitionId;
  roomName: string;
  address: string;
  capacity: number;
  sortOrder: number;
}

export interface CompetitionTheorySession {
  id: CompetitionId;
  paperSnapshotId: CompetitionId;
  sessionCode: 'A' | 'B';
  sessionName: string;
  startAt: string;
  endAt: string;
  totalCapacity: number;
  status: string;
  rooms: CompetitionTheorySessionRoom[];
}

export interface CompetitionTheoryAssignment {
  assignmentId: CompetitionId;
  playerId: CompetitionId;
  sessionId: CompetitionId;
  eventName: string;
  workTypeName: string;
  sessionName: string;
  startAt: string;
  endAt: string;
  roomName: string;
  address: string;
  seatNo: string;
  contestantNo: string;
  contestantName: string;
  orgName: string;
}

export interface CompetitionTheoryScheduleVersion {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  scoreRuleId: CompetitionId;
  versionNo: number;
  status: string;
  randomBatchId?: string | null;
  generatedAt?: string | null;
  publishedAt?: string | null;
  withdrawnAt?: string | null;
  withdrawReason?: string | null;
  eligibleCandidateCount: number;
  assignedCandidateCount: number;
  totalCapacity: number;
  capacityGap: number;
  sessions: CompetitionTheorySession[];
  assignments: CompetitionTheoryAssignment[];
}

export interface CompetitionTheorySchedulePreflight {
  scheduleVersionId: CompetitionId;
  readyToPublish: boolean;
  eligibleCandidateCount: number;
  assignedCandidateCount: number;
  totalCapacity: number;
  capacityGap: number;
  issues: string[];
}

export interface CompetitionTheoryMonitor {
  attemptId: CompetitionId;
  assignmentId: CompetitionId;
  playerId: CompetitionId;
  contestantNo: string;
  contestantName: string;
  orgName: string;
  sessionName: string;
  roomName: string;
  seatNo: string;
  status: string;
  startedAt?: string | null;
  lastSavedAt?: string | null;
  submittedAt?: string | null;
  examSessionOnline: boolean;
}

export interface CompetitionTheoryResultSummary {
  attemptId: CompetitionId;
  playerId: CompetitionId;
  contestantNo: string;
  contestantName: string;
  status: string;
  absent: boolean;
  totalScore?: CompetitionDecimal | null;
  confirmationStatus?: string | null;
  published: boolean;
  submittedAt?: string | null;
}
