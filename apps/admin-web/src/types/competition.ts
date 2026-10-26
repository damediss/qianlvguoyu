export type CompetitionId = number | string;

export type CompetitionEventScope = 'platform' | 'enterprise_internal';
export type CompetitionEventStatus =
  'draft' | 'published' | 'closed' | 'archived';
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
  stagePlanStatus?: CompetitionStagePlanStatus | null;
  activeWorkTypeCount?: number;
  selectedLevelCount?: number;
  confirmedScopeCount?: number;
  stageCount?: number;
}

export interface CompetitionEventPublishReadinessCheck {
  code: string;
  passed: boolean;
  message: string;
  targetType?: string | null;
  targetId?: CompetitionId | null;
}

export interface CompetitionEventPublishScopeReadiness {
  eventWorkTypeId: CompetitionId;
  workTypeId: CompetitionId;
  workTypeName?: string | null;
  levelId: CompetitionId;
  levelCode: string;
  levelName: string;
  stagePlanStatus?: CompetitionStagePlanStatus | null;
  stageCount: number;
  ready: boolean;
  checks: CompetitionEventPublishReadinessCheck[];
}

export interface CompetitionEventPublishReadiness {
  eventId: CompetitionId;
  eventCode?: string | null;
  eventName?: string | null;
  eventStatus: CompetitionEventStatus;
  eventScope?: CompetitionEventScope | null;
  startAt?: string | null;
  endAt?: string | null;
  ready: boolean;
  activeWorkTypeCount: number;
  selectedLevelCount: number;
  confirmedScopeCount: number;
  stageCount: number;
  eventChecks: CompetitionEventPublishReadinessCheck[];
  scopes: CompetitionEventPublishScopeReadiness[];
}

export type CompetitionEventOrgStatus =
  'invited' | 'active' | 'suspended' | 'closed';

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

export type CompetitionRosterImportStatus =
  'validating' | 'validation_failed' | 'pending_confirm' | 'confirmed';
export type CompetitionRosterValidationStatus = 'valid' | 'invalid';

export interface CompetitionRosterItem {
  id: CompetitionId;
  batchId: CompetitionId;
  orgId?: CompetitionId | null;
  orgName?: string | null;
  eventWorkTypeId?: CompetitionId | null;
  eventWorkTypeLevelId?: CompetitionId | null;
  sourceRowNo?: number | null;
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
  orgId?: CompetitionId | null;
  orgName?: string | null;
  eventWorkTypeId?: CompetitionId | null;
  eventWorkTypeName?: string | null;
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
  orgId?: CompetitionId | null;
  orgName?: string | null;
  eventWorkTypeId?: CompetitionId | null;
  eventWorkTypeName?: string | null;
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
  eventWorkTypeLevelId?: CompetitionId | null;
  eventWorkTypeLevelCode?: string | null;
  eventWorkTypeLevelName?: string | null;
  name?: string | null;
  idCardNo?: string | null;
  mobile?: string | null;
  level?: string | null;
  status?: string | null;
  playerStatus?: string | null;
  createdAt?: string | null;
  workTypes?: CompetitionPlayerWorkType[];
}

export interface CompetitionPlayerWorkType {
  registrationId?: CompetitionId | null;
  profileId?: CompetitionId | null;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeName?: string | null;
  eventWorkTypeLevelId?: CompetitionId | null;
  levelCode?: string | null;
  levelName?: string | null;
  level?: string | null;
  status?: string | null;
}

export interface CompetitionPlayerQuery {
  page: number;
  pageSize: number;
  eventId?: CompetitionId;
  orgId?: CompetitionId;
  eventWorkTypeId?: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId;
  keyword?: string;
  name?: string;
  status?: string;
}

export interface CompetitionPlayerCreateRequest {
  eventId: CompetitionId;
  orgId: CompetitionId;
  eventWorkTypeId?: CompetitionId;
  name: string;
  idCardNo: string;
  mobile: string;
  level?: string;
  workTypes?: CompetitionPlayerWorkTypeRequest[];
}

export interface CompetitionPlayerUpdateRequest {
  orgId: CompetitionId;
  eventWorkTypeId?: CompetitionId;
  name?: string;
  idCardNo?: string;
  mobile?: string;
  level?: string;
  workTypes?: CompetitionPlayerWorkTypeRequest[];
}

export interface CompetitionPlayerWorkTypeRequest {
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  levelCode?: string | null;
  level?: string | null;
}

export interface CompetitionRefereeQualification {
  workTypeId: CompetitionId;
  workTypeName?: string | null;
  levelFromCode: string;
  levelToCode: string;
  levelFromName?: string | null;
  levelToName?: string | null;
  levelRange?: string | null;
  /** Legacy single-level aliases returned during migration compatibility. */
  levelCode?: string | null;
  levelName?: string | null;
}

export interface CompetitionReferee {
  id: CompetitionId;
  eventId?: CompetitionId | null;
  memberId?: CompetitionId | null;
  personId: CompetitionId;
  userId?: CompetitionId | null;
  accountUsername?: string | null;
  eventName?: string | null;
  eventWorkTypeId?: CompetitionId | null;
  eventWorkTypeName?: string | null;
  eventWorkTypeLevelId?: CompetitionId | null;
  eventWorkTypeLevelCode?: string | null;
  eventWorkTypeLevelName?: string | null;
  name: string;
  idCardNo: string;
  mobile: string;
  status?: string | null;
  createdAt?: string | null;
  qualifications?: CompetitionRefereeQualification[];
}

export interface CompetitionRefereeQuery {
  page: number;
  pageSize: number;
  eventId?: CompetitionId;
  workTypeId?: CompetitionId;
  keyword?: string;
  status?: string;
}

export interface CompetitionRefereeCreateRequest {
  qualifications: CompetitionRefereeQualificationRequest[];
  name: string;
  idCardNo: string;
  mobile: string;
}

export interface CompetitionRefereeQualificationRequest {
  workTypeId: CompetitionId;
  levelFromCode: string;
  levelToCode: string;
}

export type CompetitionRefereeUpdateRequest = Omit<
  CompetitionRefereeCreateRequest,
  'eventId'
>;

export interface CompetitionRefereeAssignment {
  id: CompetitionId;
  refereeMemberId: CompetitionId;
  refereeGroupId?: CompetitionId | null;
  refereeProfileId?: CompetitionId | null;
  refereeName?: string | null;
  assignmentRole: string;
  status: string;
}

export interface CompetitionRefereeCandidate {
  contestantProfileId: CompetitionId;
  contestantMemberId: CompetitionId;
  contestantNo?: string | null;
  name?: string | null;
  orgName?: string | null;
  workTypeName?: string | null;
  levelCode?: string | null;
  levelName?: string | null;
  paperSnapshotId?: CompetitionId | null;
  paperCode?: string | null;
  paperTitle?: string | null;
  assignmentStatus: 'assigned' | 'unassigned' | string;
  refereeGroupId?: CompetitionId | null;
  refereeGroupName?: string | null;
  refereeGroupStatus?: string | null;
  assignments: CompetitionRefereeAssignment[];
}

export interface CompetitionRefereeGroupMember {
  id: CompetitionId;
  refereeMemberId: CompetitionId;
  refereeProfileId?: CompetitionId | null;
  refereeName?: string | null;
  memberRole: 'leader' | 'member' | string;
  status: string;
}

export interface CompetitionRefereeGroupBoundTask {
  eventId?: CompetitionId | null;
  eventName?: string | null;
  stageId?: CompetitionId | null;
  stageName?: string | null;
  eventWorkTypeId?: CompetitionId | null;
  workTypeName?: string | null;
  eventWorkTypeLevelId?: CompetitionId | null;
  levelName?: string | null;
  contestantCount?: number | null;
  completedCount?: number | null;
  status: 'pending' | 'in_progress' | 'completed' | string;
}

export interface CompetitionRefereeGroup {
  id: CompetitionId;
  stageId?: CompetitionId | null;
  groupNo: number;
  groupName: string;
  status: 'draft' | 'confirmed' | 'closed' | string;
  leaderRefereeProfileId?: CompetitionId | null;
  bindable?: boolean | null;
  releasable?: boolean | null;
  unavailableReason?: string | null;
  conflictStageName?: string | null;
  occupiedEventName?: string | null;
  members: CompetitionRefereeGroupMember[];
  boundTasks?: CompetitionRefereeGroupBoundTask[];
}

export interface CompetitionRefereeGroupUpsertRequest {
  groupName: string;
  refereeProfileIds: CompetitionId[];
  leaderRefereeProfileId: CompetitionId;
}

export interface CompetitionRefereeAssignmentWorkspace {
  stageId: CompetitionId;
  eventId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  stageName?: string | null;
  workTypeName?: string | null;
  levelName?: string | null;
  referees: CompetitionReferee[];
  groups: CompetitionRefereeGroup[];
  candidates: CompetitionRefereeCandidate[];
}

export interface CompetitionRefereeBatchAssignmentResult {
  stageId: CompetitionId;
  contestantMemberIds: CompetitionId[];
  updatedCount: number;
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
  eventWorkTypeId?: CompetitionId | null;
  eventWorkTypeLevelCode?: string | null;
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
  promotionRuleType?:
    'FIXED_COUNT' | 'PERCENTAGE' | 'FINAL_PODIUM' | 'LEGACY' | string | null;
  promotionPercentage?: number | null;
  legacyRule?: number | null;
  targetStageId?: CompetitionId | null;
  rosterSnapshotId?: CompetitionId | null;
  resources: CompetitionStagePlanResource[];
}

export interface CompetitionStageExecutionPlanCreateRequest {
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  resourceVersion?: string;
  scoreRuleId?: CompetitionId | null;
  promotionQuota?: number | null;
  promotionRuleType?: string | null;
  promotionPercentage?: number | null;
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
  eventId?: CompetitionId;
  stageId?: CompetitionId;
  eventWorkTypeId?: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  sessionCode: string;
  sessionName: string;
  startAt: string;
  endAt: string;
  capacity?: number | null;
  resourceId?: CompetitionId | null;
  paperSnapshotId?: CompetitionId | null;
  status: string;
}

export interface CompetitionPracticalSessionCreateRequest {
  planId: CompetitionId;
  sessionCode: string;
  sessionName: string;
  startAt: string;
  endAt: string;
  capacity?: number | null;
  resourceId?: CompetitionId | null;
}

export interface CompetitionPracticalPaperBinding {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  paperSnapshotId: CompetitionId;
}

export interface CompetitionPracticalPaperBindingRequest {
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  paperSnapshotId: CompetitionId;
}

export interface CompetitionPracticalAssessmentSheet {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  paperSnapshotId: CompetitionId;
  paperCode?: string | null;
  versionNo: number;
  sheetCode: string;
  title: string;
  questionCount: number;
  totalScore: CompetitionDecimal;
  status: string;
  sheet: {
    summary?: Array<{
      paperQuestionId: CompetitionId;
      sortOrder: number;
      title: string;
      score: CompetitionDecimal;
      weight: number;
    }>;
    questions?: Array<{
      paperQuestionId: CompetitionId;
      sortOrder: number;
      title: string;
      score: CompetitionDecimal;
      scoringMethod?: string | null;
      scoreItems?: unknown;
      detail?: unknown;
    }>;
  };
}

export interface CompetitionPracticalConsoleQuestion {
  paperQuestionId?: CompetitionId | null;
  sortOrder: number;
  title: string;
  maxScore?: CompetitionDecimal | null;
  score?: CompetitionDecimal | null;
  status?: string | null;
}

export interface CompetitionPracticalConsoleTask {
  assignmentId: CompetitionId;
  sessionId: CompetitionId;
  contestantMemberId: CompetitionId;
  refereeMemberId: CompetitionId;
  assessmentSheetId: CompetitionId;
  workstationCode?: string | null;
  taskStatus: string;
  scoreSheetId?: CompetitionId | null;
  scoreSheetStatus?: string | null;
  totalScore?: CompetitionDecimal | null;
  scoreDifferenceLimit?: CompetitionDecimal | null;
  actualScoreDifference?: CompetitionDecimal | null;
  scoreDifferenceStatus?: 'waiting' | 'normal' | 'exceeded' | 'unrestricted' | string;
  practicalScoreCompleted?: boolean;
  contestantNo?: string | null;
  contestantName?: string | null;
  refereeName?: string | null;
  assessmentSheetTitle?: string | null;
  questions?: CompetitionPracticalConsoleQuestion[];
}

export interface CompetitionPracticalConsole {
  planId?: CompetitionId | null;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  sessionCount: number;
  contestantCount: number;
  scoringTaskCount: number;
  openedSheetCount: number;
  submittedSheetCount: number;
  status: 'not_initialized' | 'initialized' | 'completed' | string;
  tasks: CompetitionPracticalConsoleTask[];
}

export interface CompetitionPracticalJudgeTask {
  assignmentId: CompetitionId;
  sessionId: CompetitionId;
  eventId?: CompetitionId | null;
  eventName?: string | null;
  stageId?: CompetitionId | null;
  stageName?: string | null;
  workTypeName?: string | null;
  levelName?: string | null;
  contestantMemberId: CompetitionId;
  contestantNo?: string | null;
  contestantName?: string | null;
  refereeMemberId: CompetitionId;
  workstationCode?: string | null;
  status: string;
  assessmentSheetId: CompetitionId;
  assessmentSheet: CompetitionPracticalAssessmentSheet;
  scoreSheetId?: CompetitionId | null;
  scoreSheetStatus?: string | null;
  totalScore?: CompetitionDecimal | null;
  scoreDifferenceLimit?: CompetitionDecimal | null;
  actualScoreDifference?: CompetitionDecimal | null;
  scoreDifferenceStatus?: 'waiting' | 'normal' | 'exceeded' | 'unrestricted' | string;
  judgeRole: 'leader' | 'member';
  startStatus: 'waiting' | 'ready' | 'started' | 'awaiting_leader' | 'completed';
  confirmedJudges: number;
  requiredJudges: number;
  currentJudgeConfirmed: boolean;
  questionProgress?: CompetitionPracticalQuestionProgress[];
  activeStageId?: CompetitionId | null;
    startedAt?: string | null;
    standardDeadlineAt?: string | null;
    deadlineAt?: string | null;
    examEndedAt?: string | null;
    pausedDurationSeconds?: number;
}

export interface CompetitionPracticalQuestionProgress {
  paperQuestionId: CompetitionId;
  status: 'unopened' | 'scoring' | 'submitted' | 'returned' | string;
  score?: CompetitionDecimal | null;
  entries?: CompetitionPracticalJudgeScoreEntry[];
}

export interface CompetitionPracticalJudgeScoreEntry {
  paperItemNo: number;
  score: CompetitionDecimal;
  deduction: CompetitionDecimal;
  remark?: string | null;
}

export interface CompetitionPracticalScoreEntryRequest {
  ruleItemId?: CompetitionId | null;
  paperQuestionId: CompetitionId;
  paperItemNo: number;
  score: CompetitionDecimal;
  deduction: CompetitionDecimal;
  remark?: string;
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
  eventWorkTypeLevelCode?: string | null;
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

export interface CompetitionEventWorkTypeLevel {
  id: CompetitionId;
  eventId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  levelCode: string;
  levelName: string;
  sourceLevelCode?: string | null;
  sourceLevelText?: string | null;
  reconciliationStatus?: string | null;
  stagePlanStatus?: 'draft' | 'confirmed' | string;
  stagePlanConfirmedBy?: CompetitionId | null;
  stagePlanConfirmedAt?: string | null;
}

export interface CompetitionEventWorkTypeLevelReplaceItem {
  id?: CompetitionId | null;
  levelCode: string;
  levelName: string;
  sourceLevelCode?: string | null;
  sourceLevelText?: string | null;
  reconciliationStatus?: string | null;
}

export type CompetitionScoreRuleStatus = 'draft' | 'published';

export interface CompetitionScoreRule {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
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
  eventWorkTypeLevelId?: CompetitionId | null;
  ready: boolean;
  publishedScoreRuleVersionId?: CompetitionId | null;
  checks: CompetitionScoringReadinessCheck[];
}

export type CompetitionDecimal = number | string;
export type CompetitionScoreSourceType = 'theory' | 'simulation' | 'practical';
export type CompetitionScoreConfirmationStatus =
  'pending' | 'confirmed' | 'voided';

export interface CompetitionScoreScopeQuery {
  page: number;
  pageSize: number;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId;
  contestantMemberId?: CompetitionId;
  sourceType?: CompetitionScoreSourceType;
  scoreRuleId?: CompetitionId;
}

export interface CompetitionScoreSource {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  contestantMemberId: CompetitionId;
  sourceType: CompetitionScoreSourceType;
  rawScore: CompetitionDecimal;
  confirmationStatus: CompetitionScoreConfirmationStatus;
  sourceSystem: string;
  sourceReference?: string | null;
  scoreRuleId?: CompetitionId | null;
  revisionNo: number;
  currentFlag: number;
  achievedAt?: string | null;
  confirmedAt?: string | null;
  confirmedBy?: CompetitionId | null;
  createdAt?: string | null;
  createdBy?: CompetitionId | null;
}

export interface CompetitionScoreSourceRevisionRequest {
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
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
  eventWorkTypeLevelId?: CompetitionId | null;
  contestantMemberId: CompetitionId;
  scoreRuleId?: CompetitionId | null;
  theorySourceId?: CompetitionId | null;
  simulationSourceId?: CompetitionId | null;
  practicalSourceId?: CompetitionId | null;
  theoryScore: CompetitionDecimal;
  simulationScore: CompetitionDecimal;
  practicalScore: CompetitionDecimal;
  simulationPracticalScore: CompetitionDecimal;
  theoryWeight: CompetitionDecimal;
  simulationWeight?: CompetitionDecimal | null;
  practicalWeight?: CompetitionDecimal | null;
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
  eventWorkTypeLevelId?: CompetitionId | null;
  contestantMemberId: CompetitionId;
  scoreRuleId?: CompetitionId | null;
}

export type CompetitionRankingSnapshotStatus =
  'draft' | 'confirmed' | 'published' | 'superseded';
export type CompetitionContestantResultStatus = 'completed' | 'absent';

export interface CompetitionRankingSnapshot {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  scoreRuleId?: CompetitionId | null;
  snapshotVersion: number;
  status: CompetitionRankingSnapshotStatus;
  generationReason?: string | null;
  generatedAt?: string | null;
  createdAt?: string | null;
  inputFingerprint?: string | null;
  confirmedAt?: string | null;
  confirmedBy?: CompetitionId | null;
  publishedAt?: string | null;
  publishedBy?: CompetitionId | null;
  supersededAt?: string | null;
  supersededBy?: CompetitionId | null;
  supersedesSnapshotId?: CompetitionId | null;
  correctionReason?: string | null;
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
  eventWorkTypeLevelId?: CompetitionId | null;
  scoreRuleId: CompetitionId;
  generationReason?: string;
  candidates: CompetitionRankingCandidate[];
}

export interface CompetitionCalculatedRankingPublishRequest {
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  correctionReason?: string;
}

export interface CompetitionRankingGenerateResult {
  snapshot: CompetitionRankingSnapshot;
  entries: CompetitionRankingEntry[];
}

export interface CompetitionFormalScoringTask {
  id: CompetitionId;
  examSessionId: CompetitionId;
  candidateExamId: CompetitionId;
  contestantMemberId?: CompetitionId | null;
  resultRevisionNo: number;
  candidateNo?: string | null;
  candidateName?: string | null;
  taskStatus: 'pending' | 'processing' | 'retry' | 'completed' | 'failed';
  attemptCount: number;
  nextAttemptAt?: string | null;
  leaseUntil?: string | null;
  lastError?: string | null;
  requestedReason?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  candidateStatus?: string | null;
}

export interface CompetitionSubjectScoreImportBatch {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  sourceType: 'practical' | 'simulation';
  sourceFileId: CompetitionId;
  fileName: string;
  batchStatus:
    'parsing' | 'ready' | 'invalid' | 'confirming' | 'completed' | 'failed';
  totalRows: number;
  validRows: number;
  errorRows: number;
  confirmedRows: number;
  errorMessage?: string | null;
  correctionReason?: string | null;
  confirmedAt?: string | null;
  confirmedBy?: CompetitionId | null;
}

export interface CompetitionSimulationImportBatch {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  scoreRuleId: CompetitionId;
  sourceFileId: CompetitionId;
  fileName: string;
  batchStatus:
    'parsing' | 'ready' | 'invalid' | 'confirming' | 'completed' | 'failed';
  totalRows: number;
  validRows: number;
  errorRows: number;
  confirmedRows: number;
  errorMessage?: string | null;
  correctionReason?: string | null;
  confirmedAt?: string | null;
  confirmedBy?: CompetitionId | null;
}

export interface CompetitionSimulationProviderBinding {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  providerCode: 'plantvr';
  externalHostTenantId: string;
  externalSessionId: string;
  moduleFilter?: string | null;
  status: 'active' | 'disabled';
  lastSyncAt?: string | null;
}

export interface CompetitionSimulationProviderBindingRequest {
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  externalHostTenantId: string;
  externalSessionId: string;
  moduleFilter?: string;
}

export interface PlantVrCompetitionSession {
  id: string;
  name: string;
  hostTenantId: string;
  hostTenantName?: string | null;
  status: string;
  startsAt?: string | null;
  endsAt?: string | null;
}

export interface CompetitionSimulationSyncBatch {
  id: CompetitionId;
  bindingId: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId | null;
  batchStatus:
    | 'syncing'
    | 'ready'
    | 'ready_with_errors'
    | 'confirming'
    | 'completed'
    | 'failed';
  totalRows: number;
  validRows: number;
  errorRows: number;
  confirmedRows: number;
  correctionReason?: string | null;
  errorMessage?: string | null;
  confirmedAt?: string | null;
  confirmedBy?: CompetitionId | null;
}

export interface CompetitionSimulationScoreDetailDeduction {
  title?: string | null;
  reason?: string | null;
  score?: CompetitionDecimal | null;
  points?: CompetitionDecimal | null;
}

export interface CompetitionSimulationScoreDetailItem {
  key?: string | null;
  sequence?: string | number | null;
  title?: string | null;
  name?: string | null;
  phaseTitle?: string | null;
  groupTitle?: string | null;
  label?: string | null;
  assessment?: string | string[] | null;
  assessmentItem?: string | string[] | null;
  project?: string | string[] | null;
  criteria?: string | string[] | null;
  standard?: string | string[] | null;
  scoringStandard?: string | string[] | null;
  score?: CompetitionDecimal | null;
  earnedScore?: CompetitionDecimal | null;
  value?: CompetitionDecimal | null;
  maxScore?: CompetitionDecimal | null;
  allocatedScore?: CompetitionDecimal | null;
  hasMaxScore?: boolean;
  hasScore?: boolean;
  deductions?:
    | string
    | Array<string | CompetitionSimulationScoreDetailDeduction>
    | null;
  deductionItems?:
    | string
    | Array<string | CompetitionSimulationScoreDetailDeduction>
    | null;
  deduction?:
    | string
    | CompetitionSimulationScoreDetailDeduction
    | null;
  lines?: CompetitionSimulationScoreDetailItem[] | null;
  items?: CompetitionSimulationScoreDetailItem[] | null;
  criteriaItems?: CompetitionSimulationScoreDetailItem[] | null;
  [key: string]: unknown;
}

export interface CompetitionSimulationScoreDetail {
  totalScore?: CompetitionDecimal;
  lines?: CompetitionSimulationScoreDetailItem[];
  groups?: Array<CompetitionSimulationScoreDetailItem & { title?: string | null }>;
  sheet?: {
    moduleTitle?: string | null;
    phases?: CompetitionSimulationScoreDetailItem[] | null;
  } | null;
  [key: string]: unknown;
}

export interface CompetitionSimulationSyncRow {
  id: CompetitionId;
  batchId: CompetitionId;
  rowNo: number;
  externalParticipantId?: string | null;
  externalSessionId: string;
  playerName?: string | null;
  idCardMasked?: string | null;
  contestantMemberId?: CompetitionId | null;
  rawScore?: CompetitionDecimal | null;
  moduleTitle?: string | null;
  appDisplayName?: string | null;
  durationSeconds?: number | null;
  achievedAt?: string | null;
  validationStatus: 'pending' | 'valid' | 'invalid' | 'conflict' | 'confirmed';
  errorMessage?: string | null;
  scoreSourceId?: CompetitionId | null;
  detail?: CompetitionSimulationScoreDetail | null;
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
  | 'id'
  | 'eventId'
  | 'stageId'
  | 'eventWorkTypeId'
  | 'scoreRuleId'
  | 'createdAt'
  | 'updatedAt'
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
  eventWorkTypeLevelId?: CompetitionId | null;
  scoreRuleId?: CompetitionId | null;
  requirementId?: CompetitionId | null;
  paperCode: string;
  versionNo: number;
  sourceCourseId?: CompetitionId | null;
  sourcePlanId?: CompetitionId | null;
  sourcePaperId?: CompetitionId | null;
  sourceCompetitionPaperSnapshotId?: CompetitionId | null;
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

export interface CompetitionStagePaperAssignmentPaper {
  id: CompetitionId;
  paperCode: string;
  title: string;
  versionNo: number;
  assignedCount: number;
  percentage: CompetitionDecimal;
}

export interface CompetitionStagePaperAssignmentSummary {
  eventId: CompetitionId;
  eventName: string;
  stageId: CompetitionId;
  stageName: string;
  eventWorkTypeId: CompetitionId;
  workTypeName: string;
  eventWorkTypeLevelId: CompetitionId;
  levelCode: string;
  levelName: string;
  eligibleCount: number;
  paperCount: number;
  assignedCount: number;
  unassignedCount: number;
  confirmed: boolean;
  papers: CompetitionStagePaperAssignmentPaper[];
  formalExamLocked?: boolean;
  formalExamSessionId?: CompetitionId | null;
  formalExamStatus?: string | null;
}

export type CompetitionStagePaperAssignmentContentType = 'theory' | 'practical';

export interface CompetitionStagePaperAssignmentPlayer {
  contestantProfileId: CompetitionId;
  contestantMemberId: CompetitionId;
  registrationId: CompetitionId;
  playerId: CompetitionId;
  contestantNo?: string | null;
  name?: string | null;
  orgId?: CompetitionId | null;
  orgName?: string | null;
  workTypeName?: string | null;
  levelCode?: string | null;
  levelName?: string | null;
  rosterLevel?: string | null;
  paperSnapshotId?: CompetitionId | null;
  paperCode?: string | null;
  paperTitle?: string | null;
  assessmentSheetId?: CompetitionId | null;
  assessmentSheetCode?: string | null;
  allocationSource?: 'random' | 'manual' | string | null;
  assignmentStatus: 'assigned' | 'unassigned' | string;
  configurationStatus: 'draft' | 'confirmed' | string;
}

export interface CompetitionStagePaperAssignmentQuery {
  page: number;
  pageSize: number;
  keyword?: string;
  orgId?: CompetitionId;
  paperSnapshotId?: CompetitionId;
  assignmentStatus?: 'assigned' | 'unassigned';
}

export interface CompetitionStagePaperAssignmentItem {
  contestantProfileId: CompetitionId;
  paperSnapshotId?: CompetitionId | null;
}

export interface CompetitionTheoryPaperPreviewQuestion {
  sortOrder: number;
  content: string;
  questionType: string;
  score: CompetitionDecimal;
}

export interface CompetitionTheoryPaperPreview {
  id: CompetitionId;
  paperCode: string;
  versionNo: number;
  sourceTitle: string;
  durationMinutes: number;
  questionCount: number;
  totalScore: CompetitionDecimal;
  questions: CompetitionTheoryPaperPreviewQuestion[];
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
  sessionCode: string;
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

export interface CompetitionFormalExamPaper {
  id: CompetitionId;
  sourcePaperId: CompetitionId;
  paperCode: string;
  paperName?: string | null;
  snapshotVersion: number;
  questionCount: number;
  totalScore?: CompetitionDecimal | null;
  durationMinutes?: number | null;
  candidateCount: number;
}

export interface CompetitionFormalExamSession {
  id: CompetitionId;
  eventId: CompetitionId;
  eventName?: string | null;
  stageId: CompetitionId;
  stageName?: string | null;
  eventWorkTypeId: CompetitionId;
  workTypeName?: string | null;
  eventWorkTypeLevelId: CompetitionId;
  levelCode?: string | null;
  levelName?: string | null;
  examType: string;
  status: string;
  durationSeconds: number;
  plannedStartTime?: string | null;
  actualStartTime?: string | null;
  actualEndTime?: string | null;
  candidateTotal: number;
  submittedTotal: number;
  waitingEnterCount?: number;
  readyCount?: number;
  runningCount?: number;
  submittedCount?: number;
  autoSubmittedCount?: number;
  forcedSubmittedCount?: number;
  absentCount?: number;
  paperCount: number;
  papers: CompetitionFormalExamPaper[];
  serverTime?: string | null;
  deadlineTime?: string | null;
  finishEligible?: boolean;
  finishBlockers?: CompetitionFormalExamFinishBlocker[];
  scoringStatus?: string | null;
  scoringStartedAt?: string | null;
  scoredTime?: string | null;
  scoringError?: string | null;
  scoredCount?: number | null;
  scoringAbsentCount?: number | null;
  scoringFailedCount?: number | null;
}

export interface CompetitionFormalExamFinishBlocker {
  code: string;
  count?: number;
  message: string;
}

export interface CompetitionFormalExamPreflightCheck {
  code: string;
  passed: boolean;
  message: string;
}

export interface CompetitionFormalExamPreflight {
  ready: boolean;
  checks: CompetitionFormalExamPreflightCheck[];
}

export interface CompetitionFormalCandidateExam {
  id: CompetitionId;
  examSessionId: CompetitionId;
  candidateId: CompetitionId;
  contestantProfileId: CompetitionId;
  candidateNo?: string | null;
  candidateName?: string | null;
  paperSnapshotId: CompetitionId;
  sourcePaperId: CompetitionId;
  paperCode?: string | null;
  paperName?: string | null;
  status: string;
  answeredCount: number;
  totalQuestionCount: number;
  enterTime?: string | null;
  startTime?: string | null;
  submitTime?: string | null;
  submitType?: string | null;
  lastActiveTime?: string | null;
  forceSubmitReason?: string | null;
  forceSubmittedBy?: string | number | null;
  absentTime?: string | null;
  absentReason?: string | null;
  resultStatus?: string | null;
  totalScore?: CompetitionDecimal | null;
  fullScore?: CompetitionDecimal | null;
  accessSessionStatus?: string | null;
  connectionState?: string | null;
  lastSeenAt?: string | null;
}

export interface CompetitionFormalExamForceOfflineBatchResult {
  requestedCount: number;
  kickedCount: number;
  alreadyOfflineCount: number;
}

export interface CompetitionFormalExamConsoleRealtimeTicket {
  ticket: string;
  expiresIn: number;
  wsPath: string;
  heartbeatSeconds: number;
}

export interface CompetitionFormalCandidateQuestionScore {
  questionSnapshotId: CompetitionId;
  questionOrder: number;
  questionType: string;
  answerVersion: number;
  answered: boolean;
  correct: boolean;
  maxScore: CompetitionDecimal;
  awardedScore: CompetitionDecimal;
}

export interface CompetitionFormalCandidateAnswerSheetQuestion {
  questionSnapshotId: CompetitionId;
  questionOrder: number;
  questionType: string;
  stem: string;
  answered: boolean;
  correct: boolean;
  candidateAnswer: {
    selectedOptionIds?: string[];
    blanks?: string[];
  };
  options: Array<{
    optionId: string;
    label: string;
    content: string;
  }>;
}

export interface CompetitionFormalCandidateAnswerSheet {
  candidateExamId: CompetitionId;
  candidateName: string;
  candidateNo: string;
  questions: CompetitionFormalCandidateAnswerSheetQuestion[];
}

export interface CompetitionFormalCandidateResult {
  id: CompetitionId;
  examSessionId: CompetitionId;
  candidateExamId: CompetitionId;
  resultStatus: string;
  fullScore?: CompetitionDecimal | null;
  totalScore?: CompetitionDecimal | null;
  questionCount?: number | null;
  answeredCount?: number | null;
  correctCount?: number | null;
  wrongCount?: number | null;
  unansweredCount?: number | null;
  scoreVersion?: number | null;
  scoredTime?: string | null;
  questionScores?: CompetitionFormalCandidateQuestionScore[];
}
