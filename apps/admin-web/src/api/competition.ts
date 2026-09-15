import { apiClient, unwrap } from './client';
import type { FormalExamRealtimeCatchUpPage } from './formalExamPlayer';
import type { PageResponse } from '../types/api';
import type {
  CompetitionEvent,
  CompetitionEventCreateRequest,
  CompetitionEventOrg,
  CompetitionEventOrgCreateRequest,
  CompetitionEventOrgImportPermissionUpdateRequest,
  CompetitionEventOrgPageQuery,
  CompetitionEventOrgStatusUpdateRequest,
  CompetitionRosterImportDetail,
  CompetitionRosterImport,
  CompetitionRosterImportListQuery,
  CompetitionRosterImportPageQuery,
  CompetitionPlayer,
  CompetitionPlayerCreateRequest,
  CompetitionPlayerQuery,
  CompetitionPlayerUpdateRequest,
  CompetitionNotice,
  CompetitionNoticeAsset,
  CompetitionNoticeAssetType,
  CompetitionNoticeCreateRequest,
  CompetitionNoticePageQuery,
  CompetitionNoticePublishResult,
  CompetitionNoticeUpdateRequest,
  CompetitionEventPageQuery,
  CompetitionEventPublishResult,
  CompetitionEventPublishReadiness,
  CompetitionEventUpdateRequest,
  CompetitionId,
  CompetitionStage,
  CompetitionStageCreateRequest,
  CompetitionStageUpdateRequest,
  CompetitionEventWorkType,
  CompetitionEventWorkTypeLevel,
  CompetitionEventWorkTypeLevelReplaceItem,
  CompetitionScoreRule,
  CompetitionScoreRuleCreateRequest,
  CompetitionScoreRuleUpdateRequest,
  CompetitionScoringReadiness,
  CompetitionScoreScopeQuery,
  CompetitionScoreSource,
  CompetitionScoreSourceRevisionRequest,
  CompetitionTotalScoreSnapshot,
  CompetitionTotalScoreCalculateRequest,
  CompetitionRankingSnapshot,
  CompetitionRankingEntry,
  CompetitionRankingGenerateRequest,
  CompetitionRankingGenerateResult,
  CompetitionFormalScoringTask,
  CompetitionSimulationImportBatch,
  CompetitionTheoryRequirement,
  CompetitionTheoryRequirementRequest,
  CompetitionTheoryCourseSource,
  CompetitionTheoryPaperPreview,
  CompetitionTheoryPaperSource,
  CompetitionTheoryPaperSnapshot,
  CompetitionTheoryRoom,
  CompetitionTheoryScheduleVersion,
  CompetitionTheorySchedulePreflight,
  CompetitionTheoryMonitor,
  CompetitionTheoryResultSummary,
  CompetitionStageExecutionPlan,
  CompetitionStageContentItemRequest,
  CompetitionStageWorkspace,
  CompetitionStagePlanPreflight,
  CompetitionPracticalSession,
  CompetitionStageRosterSnapshot,
  CompetitionPracticalJudgeGroup,
  CompetitionPracticalAssignment,
  CompetitionStageSettlementPreflight,
  CompetitionStageSettlement,
  CompetitionWorkTypeStageConfig,
  CompetitionStageReferenceSnapshot,
  CompetitionStageFinalPodium,
  CompetitionStagePaperAssignmentSummary,
  CompetitionStagePaperAssignmentPlayer,
  CompetitionStagePaperAssignmentQuery,
  CompetitionStagePaperAssignmentItem,
  CompetitionFormalExamSession,
  CompetitionFormalExamPreflight,
  CompetitionFormalCandidateExam,
  CompetitionFormalCandidateResult,
  CompetitionFormalExamConsoleRealtimeTicket,
} from '../types/competition';
import { uploadPlatformFile } from './files';

export function pageCompetitionEvents(params: CompetitionEventPageQuery) {
  return unwrap<PageResponse<CompetitionEvent>>(
    apiClient.get('/competition/events', { params }),
  );
}

export function getCompetitionEvent(eventId: CompetitionId) {
  return unwrap<CompetitionEvent>(apiClient.get(`/competition/events/${eventId}`));
}

export function createCompetitionEvent(payload: CompetitionEventCreateRequest) {
  return unwrap<CompetitionEvent>(apiClient.post('/competition/events', payload));
}

export function updateCompetitionEvent(
  eventId: CompetitionId,
  payload: CompetitionEventUpdateRequest,
) {
  return unwrap<CompetitionEvent>(apiClient.put(`/competition/events/${eventId}`, payload));
}

export function publishCompetitionEvent(eventId: CompetitionId) {
  return unwrap<CompetitionEventPublishResult>(
    apiClient.post(`/competition/events/${eventId}/publish`),
  );
}

export function getCompetitionEventPublishReadiness(eventId: CompetitionId) {
  return unwrap<CompetitionEventPublishReadiness>(
    apiClient.get(`/competition/events/${eventId}/publish-readiness`),
  );
}

export function pageCompetitionEventOrganizations(
  eventId: CompetitionId,
  params: CompetitionEventOrgPageQuery,
) {
  return unwrap<PageResponse<CompetitionEventOrg>>(
    apiClient.get(`/competition/events/${eventId}/organizations`, { params }),
  );
}

export function addCompetitionEventOrganization(
  eventId: CompetitionId,
  payload: CompetitionEventOrgCreateRequest,
) {
  return unwrap<CompetitionEventOrg>(
    apiClient.post(`/competition/events/${eventId}/organizations`, payload),
  );
}

export function updateCompetitionEventOrganizationStatus(
  eventId: CompetitionId,
  orgId: CompetitionId,
  payload: CompetitionEventOrgStatusUpdateRequest,
) {
  return unwrap<CompetitionEventOrg>(
    apiClient.put(`/competition/events/${eventId}/organizations/${orgId}`, payload),
  );
}

export function updateCompetitionEventOrganizationImportPermission(
  eventId: CompetitionId,
  orgId: CompetitionId,
  payload: CompetitionEventOrgImportPermissionUpdateRequest,
) {
  return unwrap<CompetitionEventOrg>(
    apiClient.put(`/competition/events/${eventId}/organizations/${orgId}/import-permission`, payload),
  );
}

export function getCompetitionRosterImport(
  batchId: CompetitionId,
  params: CompetitionRosterImportPageQuery,
) {
  return unwrap<CompetitionRosterImportDetail>(
    apiClient.get(`/competition/roster-imports/${batchId}`, { params }),
  );
}

export function pageCompetitionRosterImports(
  eventId: CompetitionId,
  params: CompetitionRosterImportListQuery,
) {
  return unwrap<PageResponse<CompetitionRosterImport>>(
    apiClient.get(`/competition/events/${eventId}/roster-imports`, { params }),
  );
}

export function precheckCompetitionRoster(
  eventId: CompetitionId,
  payload: { fileId: CompetitionId; orgId?: CompetitionId; eventWorkTypeId?: CompetitionId },
) {
  return unwrap<CompetitionRosterImportDetail>(
    apiClient.post(`/competition/events/${eventId}/roster-imports/precheck`, payload),
  );
}

export function precheckSingleCompetitionRoster(
  eventId: CompetitionId,
  payload: {
    orgId: CompetitionId;
    eventWorkTypeId: CompetitionId;
    name: string;
    idCardNo: string;
    mobile: string;
    workType?: string;
    level?: string;
  },
) {
  return unwrap<CompetitionRosterImportDetail>(
    apiClient.post(`/competition/events/${eventId}/roster-imports/single`, payload),
  );
}

export function pageCompetitionPlayers(params: CompetitionPlayerQuery) {
  return unwrap<PageResponse<CompetitionPlayer>>(
    apiClient.get('/competition/players', { params }),
  );
}

export function getCompetitionPlayerForEdit(registrationId: CompetitionId) {
  return unwrap<CompetitionPlayer>(
    apiClient.get(`/competition/players/${registrationId}`, { silentError: true }),
  );
}

export function createCompetitionPlayer(payload: CompetitionPlayerCreateRequest) {
  return unwrap<CompetitionPlayer>(apiClient.post('/competition/players', payload));
}

export function updateCompetitionPlayer(
  registrationId: CompetitionId,
  payload: CompetitionPlayerUpdateRequest,
) {
  return unwrap<CompetitionPlayer>(
    apiClient.put(`/competition/players/${registrationId}`, payload),
  );
}

export function deleteCompetitionPlayer(registrationId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/players/${registrationId}`));
}

export async function downloadCompetitionRosterTemplate(): Promise<Blob> {
  const response = await apiClient.get('/competition/roster-import-template', { responseType: 'blob' });
  return response.data as Blob;
}

export { uploadPlatformFile };

export function confirmCompetitionRosterImport(batchId: CompetitionId) {
  return unwrap<CompetitionRosterImportDetail>(
    apiClient.post(`/competition/roster-imports/${batchId}/confirm`),
  );
}

export function pageCompetitionNotices(
  eventId: CompetitionId,
  params: CompetitionNoticePageQuery,
) {
  return unwrap<PageResponse<CompetitionNotice>>(
    apiClient.get(`/competition/events/${eventId}/notices`, { params }),
  );
}

export function getCompetitionNotice(noticeId: CompetitionId) {
  return unwrap<CompetitionNotice>(apiClient.get(`/competition/notices/${noticeId}`));
}

export function createCompetitionNotice(
  eventId: CompetitionId,
  payload: CompetitionNoticeCreateRequest,
) {
  return unwrap<CompetitionNotice>(apiClient.post(`/competition/events/${eventId}/notices`, payload));
}

export function updateCompetitionNotice(
  noticeId: CompetitionId,
  payload: CompetitionNoticeUpdateRequest,
) {
  return unwrap<CompetitionNotice>(apiClient.put(`/competition/notices/${noticeId}`, payload));
}

export function publishCompetitionNotice(noticeId: CompetitionId) {
  return unwrap<CompetitionNoticePublishResult>(
    apiClient.post(`/competition/notices/${noticeId}/publish`),
  );
}

export function uploadCompetitionNoticeAsset(
  noticeId: CompetitionId,
  file: File,
  assetType: CompetitionNoticeAssetType,
) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('assetType', assetType);
  return unwrap<CompetitionNoticeAsset>(
    apiClient.post(`/competition/notices/${noticeId}/assets`, formData),
  );
}

export async function getCompetitionNoticeAsset(
  noticeId: CompetitionId,
  assetId: CompetitionId,
): Promise<Blob> {
  const response = await apiClient.get(
    `/competition/notices/${noticeId}/assets/${assetId}`,
    { responseType: 'blob', timeout: 120000 },
  );
  return response.data as Blob;
}

export function deleteCompetitionNoticeAsset(
  noticeId: CompetitionId,
  assetId: CompetitionId,
) {
  return unwrap<void>(
    apiClient.delete(`/competition/notices/${noticeId}/assets/${assetId}`),
  );
}

export function pageCompetitionStages(
  eventId: CompetitionId,
  params: {
    page: number;
    pageSize: number;
    status?: string;
    eventWorkTypeId?: CompetitionId;
    eventWorkTypeLevelCode?: string;
  },
) {
  return unwrap<PageResponse<CompetitionStage>>(
    apiClient.get(`/competition/events/${eventId}/stages`, { params }),
  );
}

export function createCompetitionStage(
  eventId: CompetitionId,
  payload: CompetitionStageCreateRequest,
) {
  return unwrap<CompetitionStage>(
    apiClient.post(`/competition/events/${eventId}/stages`, payload),
  );
}

export function updateCompetitionStage(
  stageId: CompetitionId,
  payload: CompetitionStageUpdateRequest,
) {
  return unwrap<CompetitionStage>(apiClient.put(`/competition/stages/${stageId}`, payload));
}

export function deleteCompetitionStage(stageId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/stages/${stageId}`));
}

export function beginCompetitionStageConfiguration(stageId: CompetitionId, reason?: string) {
  return unwrap<CompetitionStage>(apiClient.post(`/competition/stages/${stageId}/configuration`, reason || null));
}

export function lockCompetitionStageReady(stageId: CompetitionId) {
  return unwrap<CompetitionStage>(apiClient.post(`/competition/stages/${stageId}/actions/lock-ready`));
}

export function startCompetitionStage(stageId: CompetitionId) {
  return unwrap<CompetitionStage>(apiClient.post(`/competition/stages/${stageId}/actions/start`));
}

export function closeCompetitionStage(stageId: CompetitionId) {
  return unwrap<CompetitionStage>(apiClient.post(`/competition/stages/${stageId}/actions/close`));
}

export function markCompetitionStageSettled(stageId: CompetitionId) {
  return unwrap<CompetitionStage>(apiClient.post(`/competition/stages/${stageId}/actions/mark-settled`));
}

export function publishCompetitionStageResult(stageId: CompetitionId) {
  return unwrap<CompetitionStage>(apiClient.post(`/competition/stages/${stageId}/actions/publish-result`));
}

export function getCompetitionStagePaperAssignmentSummary(stageId: CompetitionId) {
  return unwrap<CompetitionStagePaperAssignmentSummary>(
    apiClient.get(`/competition/stages/${stageId}/paper-assignments/summary`),
  );
}

export function pageCompetitionStagePaperAssignmentPlayers(
  stageId: CompetitionId,
  params: CompetitionStagePaperAssignmentQuery,
) {
  return unwrap<PageResponse<CompetitionStagePaperAssignmentPlayer>>(
    apiClient.get(`/competition/stages/${stageId}/paper-assignments/players`, { params }),
  );
}

export function listCompetitionStagePaperAssignmentPlayers(stageId: CompetitionId) {
  return unwrap<CompetitionStagePaperAssignmentPlayer[]>(
    apiClient.get(`/competition/stages/${stageId}/paper-assignments/visualization`),
  );
}

export function autoAllocateCompetitionStagePaperAssignments(stageId: CompetitionId) {
  return unwrap<CompetitionStagePaperAssignmentSummary>(
    apiClient.post(`/competition/stages/${stageId}/paper-assignments/auto-allocate`),
  );
}

export function assignCompetitionStagePaper(
  stageId: CompetitionId,
  contestantProfileId: CompetitionId,
  paperSnapshotId: CompetitionId | null,
) {
  return unwrap<CompetitionStagePaperAssignmentPlayer>(
    apiClient.put(
      `/competition/stages/${stageId}/paper-assignments/assignments/${contestantProfileId}`,
      { paperSnapshotId },
    ),
  );
}

export function batchAssignCompetitionStagePapers(
  stageId: CompetitionId,
  assignments: CompetitionStagePaperAssignmentItem[],
) {
  return unwrap<CompetitionStagePaperAssignmentSummary>(
    apiClient.put(`/competition/stages/${stageId}/paper-assignments/assignments/batch`, {
      assignments,
    }),
  );
}

export function saveCompetitionStagePaperAssignments(
  stageId: CompetitionId,
  assignments: CompetitionStagePaperAssignmentItem[],
) {
  return unwrap<CompetitionStagePaperAssignmentSummary>(
    apiClient.put(`/competition/stages/${stageId}/paper-assignments`, { assignments }),
  );
}

export function confirmCompetitionStagePaperAssignments(
  stageId: CompetitionId,
  assignments: CompetitionStagePaperAssignmentItem[],
) {
  return unwrap<CompetitionStagePaperAssignmentSummary>(
    apiClient.post(`/competition/stages/${stageId}/paper-assignments/confirm`, { assignments }),
  );
}

export function getCompetitionFormalExamSessionByStage(
  stageId: CompetitionId,
  examType = 'theory',
) {
  return unwrap<CompetitionFormalExamSession | null>(
    apiClient.get(`/competition/stages/${stageId}/formal-exam-sessions`, { params: { examType } }),
  );
}

export function prepareCompetitionFormalExamSession(
  stageId: CompetitionId,
  examType = 'theory',
) {
  return unwrap<CompetitionFormalExamSession>(
    apiClient.post(`/competition/stages/${stageId}/formal-exam-sessions/prepare`, null, {
      params: { examType },
    }),
  );
}

export function getCompetitionFormalExamSession(sessionId: CompetitionId) {
  return unwrap<CompetitionFormalExamSession>(
    apiClient.get(`/competition/formal-exam-sessions/${sessionId}`),
  );
}

export function pageCompetitionFormalCandidateExams(
  sessionId: CompetitionId,
  params: {
    page: number;
    pageSize: number;
    keyword?: string;
    paperSnapshotId?: CompetitionId;
    status?: string;
  },
) {
  return unwrap<PageResponse<CompetitionFormalCandidateExam>>(
    apiClient.get(`/competition/formal-exam-sessions/${sessionId}/candidates`, { params }),
  );
}

export function getCompetitionFormalExamSessionPreflight(sessionId: CompetitionId) {
  return unwrap<CompetitionFormalExamPreflight>(
    apiClient.get(`/competition/formal-exam-sessions/${sessionId}/preflight`),
  );
}

export function startCompetitionFormalExamSession(
  sessionId: CompetitionId,
  body: { confirmWaitingCandidates?: boolean } = {},
) {
  return unwrap<CompetitionFormalExamSession>(
    apiClient.post(`/competition/formal-exam-sessions/${sessionId}/start`, body),
  );
}

export function getCompetitionFormalCandidateExam(candidateExamId: CompetitionId) {
  return unwrap<CompetitionFormalCandidateExam>(
    apiClient.get(`/competition/formal-exam-candidates/${candidateExamId}`),
  );
}

export function forceSubmitCompetitionFormalCandidateExam(
  sessionId: CompetitionId,
  candidateExamId: CompetitionId,
  body: { reason: string },
) {
  return unwrap<CompetitionFormalCandidateExam>(
    apiClient.post(
      `/competition/formal-exam-sessions/${sessionId}/candidates/${candidateExamId}/force-submit`,
      body,
    ),
  );
}

export function forceOfflineCompetitionFormalCandidateExam(
  sessionId: CompetitionId,
  candidateExamId: CompetitionId,
) {
  return unwrap<void>(
    apiClient.post(
      `/competition/formal-exam-sessions/${sessionId}/candidates/${candidateExamId}/force-offline`,
    ),
  );
}

export function createFormalExamConsoleRealtimeTicket(sessionId: CompetitionId) {
  return unwrap<CompetitionFormalExamConsoleRealtimeTicket>(
    apiClient.post(`/competition/formal-exam-sessions/${sessionId}/realtime/ticket`),
  );
}

export function fetchFormalExamConsoleRealtimeEvents(
  sessionId: CompetitionId,
  query: { afterSeq?: string | number | null; limit?: number } = {},
) {
  const params = new URLSearchParams();
  if (query.afterSeq != null && query.afterSeq !== '') {
    params.set('afterSeq', String(query.afterSeq));
  }
  if (query.limit != null) {
    params.set('limit', String(query.limit));
  }
  const suffix = params.toString() ? `?${params.toString()}` : '';
  return unwrap<FormalExamRealtimeCatchUpPage>(
    apiClient.get(`/competition/formal-exam-sessions/${sessionId}/realtime/events${suffix}`),
  );
}

export function scoreCompetitionFormalExamSession(sessionId: CompetitionId) {
  return unwrap<CompetitionFormalExamSession>(
    apiClient.post(`/competition/formal-exam-sessions/${sessionId}/score`),
  );
}

export function getCompetitionFormalCandidateResult(candidateExamId: CompetitionId) {
  return unwrap<CompetitionFormalCandidateResult>(
    apiClient.get(`/competition/formal-exam-candidates/${candidateExamId}/result`),
  );
}

export function getCompetitionStageWorkspace(stageId: CompetitionId) {
  return unwrap<CompetitionStageWorkspace>(apiClient.get(`/competition/stages/${stageId}/workspace`));
}

export function replaceCompetitionStageContents(
  stageId: CompetitionId,
  contents: CompetitionStageContentItemRequest[],
) {
  return unwrap<CompetitionStageWorkspace['contents']>(
    apiClient.put(`/competition/stages/${stageId}/contents`, { contents }),
  );
}

export function listCompetitionExecutionPlans(stageId: CompetitionId, eventWorkTypeId: CompetitionId) {
  return unwrap<CompetitionStageExecutionPlan[]>(
    apiClient.get(`/competition/stages/${stageId}/execution-plans`, { params: { eventWorkTypeId } }),
  );
}

export function getCompetitionExecutionPlanPreflight(planId: CompetitionId) {
  return unwrap<CompetitionStagePlanPreflight>(apiClient.get(`/competition/execution-plans/${planId}/preflight`));
}

export function listCompetitionPracticalSessions(planId: CompetitionId) {
  return unwrap<CompetitionPracticalSession[]>(apiClient.get(`/competition/execution-plans/${planId}/practical-sessions`));
}

export function bindCompetitionPracticalPaperSnapshot(
  sessionId: CompetitionId,
  paperSnapshotId: CompetitionId,
) {
  return unwrap<CompetitionPracticalSession>(
    apiClient.post(`/competition/practical-sessions/${sessionId}/paper-snapshot`, {
      paperSnapshotId,
    }),
  );
}

export function listCompetitionStageRoster(planId: CompetitionId) {
  return unwrap<CompetitionStageRosterSnapshot[]>(apiClient.get(`/competition/execution-plans/${planId}/roster`));
}

export function generateCompetitionStageRoster(planId: CompetitionId, sourceType: 'eligibility' | 'promotion') {
  return unwrap<CompetitionStageRosterSnapshot>(
    apiClient.post(`/competition/execution-plans/${planId}/roster/generate`, { sourceType }),
  );
}

export function publishCompetitionStageRoster(planId: CompetitionId, snapshotId: CompetitionId) {
  return unwrap<CompetitionStageRosterSnapshot>(
    apiClient.post(`/competition/execution-plans/${planId}/roster/${snapshotId}/publish`),
  );
}

export function listCompetitionPracticalJudgeGroups(sessionId: CompetitionId) {
  return unwrap<CompetitionPracticalJudgeGroup[]>(apiClient.get(`/competition/practical-sessions/${sessionId}/judge-groups`));
}

export function listCompetitionPracticalAssignments(sessionId: CompetitionId) {
  return unwrap<CompetitionPracticalAssignment[]>(apiClient.get(`/competition/practical-sessions/${sessionId}/assignments`));
}

export function getCompetitionStageSettlementPreflight(planId: CompetitionId, scoreRuleId: CompetitionId) {
  return unwrap<CompetitionStageSettlementPreflight>(
    apiClient.get(`/competition/execution-plans/${planId}/settlement-preflight`, { params: { scoreRuleId } }),
  );
}

export function listCompetitionStageSettlements(planId: CompetitionId) {
  return unwrap<CompetitionStageSettlement[]>(apiClient.get(`/competition/execution-plans/${planId}/settlements`));
}

export function getCompetitionStageSettlement(settlementId: CompetitionId) {
  return unwrap<CompetitionStageSettlement>(apiClient.get(`/competition/stage-settlements/${settlementId}`));
}

export function createCompetitionStageSettlement(planId: CompetitionId, payload: {
  planId: CompetitionId;
  scoreRuleId: CompetitionId;
  rankingSnapshotId: CompetitionId;
  promotionQuota?: number | null;
  targetStageId?: CompetitionId | null;
}) {
  return unwrap<CompetitionStageSettlement>(apiClient.post(`/competition/execution-plans/${planId}/settlements`, payload));
}

export function publishCompetitionStageSettlement(settlementId: CompetitionId) {
  return unwrap<CompetitionStageSettlement>(apiClient.post(`/competition/stage-settlements/${settlementId}/publish`));
}

export function listCompetitionEventWorkTypes(eventId: CompetitionId) {
  return unwrap<CompetitionEventWorkType[]>(
    apiClient.get(`/competition/events/${eventId}/work-types`),
  );
}

export function listCompetitionEventWorkTypeLevels(
  eventId: CompetitionId,
  eventWorkTypeId: CompetitionId,
) {
  return unwrap<CompetitionEventWorkTypeLevel[]>(
    apiClient.get(`/competition/events/${eventId}/work-types/${eventWorkTypeId}/levels`),
  );
}

export function replaceCompetitionEventWorkTypeLevels(
  eventId: CompetitionId,
  eventWorkTypeId: CompetitionId,
  levels: CompetitionEventWorkTypeLevelReplaceItem[],
) {
  return unwrap<CompetitionEventWorkTypeLevel[]>(
    apiClient.put(
      `/competition/events/${eventId}/work-types/${eventWorkTypeId}/levels`,
      { levels },
    ),
  );
}

export function listCompetitionWorkTypeStages(
  eventId: CompetitionId,
  eventWorkTypeId: CompetitionId,
  eventWorkTypeLevelCode?: string,
) {
  return unwrap<CompetitionWorkTypeStageConfig[]>(
    apiClient.get(`/competition/events/${eventId}/work-types/${eventWorkTypeId}/stages`, {
      params: eventWorkTypeLevelCode == null ? undefined : { eventWorkTypeLevelCode },
    }),
  );
}

export function replaceCompetitionWorkTypeStages(
  eventId: CompetitionId,
  eventWorkTypeId: CompetitionId,
  stages: Array<{
    stageId: CompetitionId;
    enabled: boolean;
    sortOrder?: number | null;
    promotionRuleType?: string | null;
    promotionPercentage?: number | null;
    promotionQuota?: number | null;
  }>,
  eventWorkTypeLevelCode?: string,
) {
  return unwrap<CompetitionWorkTypeStageConfig[]>(
    apiClient.put(
      `/competition/events/${eventId}/work-types/${eventWorkTypeId}/stages`,
      { stages },
      { params: eventWorkTypeLevelCode == null ? undefined : { eventWorkTypeLevelCode } },
    ),
  );
}

export function confirmCompetitionWorkTypeStagePlan(
  eventId: CompetitionId,
  eventWorkTypeId: CompetitionId,
  eventWorkTypeLevelCode?: string,
) {
  return unwrap<CompetitionEventWorkType>(
    apiClient.post(
      `/competition/events/${eventId}/work-types/${eventWorkTypeId}/stages/confirm`,
      undefined,
      { params: eventWorkTypeLevelCode == null ? undefined : { eventWorkTypeLevelCode } },
    ),
  );
}

export function generateCompetitionPromotionReference(settlementId: CompetitionId) {
  return unwrap<CompetitionStageReferenceSnapshot>(apiClient.post(`/competition/stage-settlements/${settlementId}/promotion-reference/generate`));
}

export function getCompetitionPromotionReference(settlementId: CompetitionId) {
  return unwrap<CompetitionStageReferenceSnapshot | null>(apiClient.get(`/competition/stage-settlements/${settlementId}/promotion-reference`));
}

export function confirmCompetitionPromotionReference(settlementId: CompetitionId) {
  return unwrap<CompetitionStageReferenceSnapshot>(apiClient.post(`/competition/stage-settlements/${settlementId}/promotion-reference/confirm`));
}

export function getCompetitionFinalPodium(settlementId: CompetitionId) {
  return unwrap<CompetitionStageFinalPodium | null>(apiClient.get(`/competition/stage-settlements/${settlementId}/final-podium`));
}

export function saveCompetitionFinalPodium(
  settlementId: CompetitionId,
  payload: Pick<CompetitionStageFinalPodium, 'firstContestantMemberId' | 'secondContestantMemberId' | 'thirdContestantMemberId' | 'specialExplanation'>,
) {
  return unwrap<CompetitionStageFinalPodium>(apiClient.put(`/competition/stage-settlements/${settlementId}/final-podium`, payload));
}

export function confirmCompetitionFinalPodium(settlementId: CompetitionId) {
  return unwrap<CompetitionStageFinalPodium>(apiClient.post(`/competition/stage-settlements/${settlementId}/final-podium/confirm`));
}

export function createCompetitionEventWorkType(
  eventId: CompetitionId,
  payload: {
    workTypeId: CompetitionId;
    displayName?: string;
    status?: string;
    sortOrder?: number;
  },
) {
  return unwrap<CompetitionEventWorkType>(
    apiClient.post(`/competition/events/${eventId}/work-types`, payload),
  );
}

export function updateCompetitionEventWorkType(
  eventId: CompetitionId,
  eventWorkTypeId: CompetitionId,
  payload: {
    workTypeId: CompetitionId;
    displayName?: string;
    status?: string;
    sortOrder?: number;
  },
) {
  return unwrap<CompetitionEventWorkType>(
    apiClient.put(`/competition/events/${eventId}/work-types/${eventWorkTypeId}`, payload),
  );
}

export function listCompetitionScoreRules(
  eventId: CompetitionId,
  params: { stageId?: CompetitionId; eventWorkTypeId?: CompetitionId; eventWorkTypeLevelId?: CompetitionId },
) {
  return unwrap<CompetitionScoreRule[]>(
    apiClient.get(`/competition/events/${eventId}/score-rules`, { params }),
  );
}

export function createCompetitionScoreRule(
  eventId: CompetitionId,
  payload: CompetitionScoreRuleCreateRequest,
) {
  return unwrap<CompetitionScoreRule>(apiClient.post(`/competition/events/${eventId}/score-rules`, payload));
}

export function updateCompetitionScoreRule(
  ruleId: CompetitionId,
  payload: CompetitionScoreRuleUpdateRequest,
) {
  return unwrap<CompetitionScoreRule>(apiClient.put(`/competition/score-rules/${ruleId}`, payload));
}

export function getCompetitionScoringReadiness(
  eventId: CompetitionId,
  params: {
    stageId?: CompetitionId;
    eventWorkTypeId?: CompetitionId;
    eventWorkTypeLevelId?: CompetitionId;
  },
) {
  return unwrap<CompetitionScoringReadiness>(
    apiClient.get(`/competition/events/${eventId}/scoring-readiness`, { params }),
  );
}

export function pageCompetitionScoreSources(
  eventId: CompetitionId,
  params: CompetitionScoreScopeQuery,
) {
  return unwrap<PageResponse<CompetitionScoreSource>>(
    apiClient.get(`/competition/events/${eventId}/score-sources`, { params }),
  );
}

export function createCompetitionScoreSourceRevision(
  eventId: CompetitionId,
  payload: CompetitionScoreSourceRevisionRequest,
) {
  return unwrap<CompetitionScoreSource>(
    apiClient.post(`/competition/events/${eventId}/score-sources/revisions`, payload),
  );
}

export function pageCompetitionTotalScoreSnapshots(
  eventId: CompetitionId,
  params: CompetitionScoreScopeQuery,
) {
  return unwrap<PageResponse<CompetitionTotalScoreSnapshot>>(
    apiClient.get(`/competition/events/${eventId}/total-score-snapshots`, { params }),
  );
}

export function calculateCompetitionTotalScore(
  eventId: CompetitionId,
  payload: CompetitionTotalScoreCalculateRequest,
) {
  return unwrap<CompetitionTotalScoreSnapshot>(
    apiClient.post(`/competition/events/${eventId}/total-score-snapshots/calculate`, payload),
  );
}

export function pageCompetitionRankingSnapshots(
  eventId: CompetitionId,
  params: Pick<CompetitionScoreScopeQuery, 'page' | 'pageSize' | 'stageId' | 'eventWorkTypeId' | 'eventWorkTypeLevelId' | 'scoreRuleId'>,
) {
  return unwrap<PageResponse<CompetitionRankingSnapshot>>(
    apiClient.get(`/competition/events/${eventId}/ranking-snapshots`, { params }),
  );
}

export function pageCompetitionRankingEntries(
  snapshotId: CompetitionId,
  params: { page: number; pageSize: number },
) {
  return unwrap<PageResponse<CompetitionRankingEntry>>(
    apiClient.get(`/competition/ranking-snapshots/${snapshotId}/entries`, { params }),
  );
}

export function generateCompetitionRankingSnapshot(
  eventId: CompetitionId,
  payload: CompetitionRankingGenerateRequest,
) {
  return unwrap<CompetitionRankingGenerateResult>(
    apiClient.post(`/competition/events/${eventId}/ranking-snapshots/generate`, payload),
  );
}

export function confirmCompetitionRankingSnapshot(snapshotId: CompetitionId) {
  return unwrap<CompetitionRankingSnapshot>(
    apiClient.post(`/competition/ranking-snapshots/${snapshotId}/confirm`),
  );
}

export function publishCompetitionRankingSnapshot(snapshotId: CompetitionId, correctionReason?: string) {
  return unwrap<CompetitionRankingSnapshot>(
    apiClient.post(`/competition/ranking-snapshots/${snapshotId}/publish`, { correctionReason }),
  );
}

export function pageCompetitionFormalScoringTasks(params: {
  page: number; pageSize: number; sessionId?: CompetitionId; eventId?: CompetitionId;
  stageId?: CompetitionId; eventWorkTypeId?: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId; status?: string;
}) {
  return unwrap<PageResponse<CompetitionFormalScoringTask>>(
    apiClient.get('/competition/formal-scoring/tasks', { params }),
  );
}

export function retryCompetitionFormalScoringTask(taskId: CompetitionId, reason: string) {
  return unwrap<CompetitionFormalScoringTask>(
    apiClient.post(`/competition/formal-scoring/tasks/${taskId}/retry`, { reason }),
  );
}

export function uploadCompetitionSimulationScores(params: {
  eventId: CompetitionId;
  stageId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId;
  scoreRuleId: CompetitionId;
}, file: File) {
  const body = new FormData();
  body.append('file', file);
  return unwrap<CompetitionSimulationImportBatch>(apiClient.post(
    '/competition/simulation-score-imports', body, { params },
  ));
}

export function getCompetitionSimulationImport(batchId: CompetitionId) {
  return unwrap<CompetitionSimulationImportBatch>(
    apiClient.get(`/competition/simulation-score-imports/${batchId}`),
  );
}

export function confirmCompetitionSimulationImport(batchId: CompetitionId, correctionReason?: string) {
  return unwrap<CompetitionSimulationImportBatch>(
    apiClient.post(`/competition/simulation-score-imports/${batchId}/confirm`, { correctionReason }),
  );
}

export async function downloadCompetitionSimulationImportErrors(
  batchId: CompetitionId,
): Promise<Blob> {
  const response = await apiClient.get(
    `/competition/simulation-score-imports/${batchId}/errors`,
    { responseType: 'blob' },
  );
  return response.data;
}

export function getCompetitionTheoryRequirement(ruleId: CompetitionId) {
  return unwrap<CompetitionTheoryRequirement | null>(
    apiClient.get(`/competition/score-rules/${ruleId}/theory-requirement`),
  );
}

export function updateCompetitionTheoryRequirement(
  ruleId: CompetitionId,
  payload: CompetitionTheoryRequirementRequest,
) {
  return unwrap<CompetitionTheoryRequirement>(
    apiClient.put(`/competition/score-rules/${ruleId}/theory-requirement`, payload),
  );
}

export function listCompetitionTheoryCourses(eventId: CompetitionId, eventWorkTypeId: CompetitionId) {
  return unwrap<CompetitionTheoryCourseSource[]>(
    apiClient.get(`/competition/events/${eventId}/theory-exam/courses`, { params: { eventWorkTypeId } }),
  );
}

export function listCompetitionTheorySourcePapers(
  eventId: CompetitionId,
  eventWorkTypeId: CompetitionId,
  courseId: CompetitionId,
) {
  return unwrap<CompetitionTheoryPaperSource[]>(
    apiClient.get(`/competition/events/${eventId}/theory-exam/source-papers`, {
      params: { eventWorkTypeId, courseId },
    }),
  );
}

export function listCompetitionTheoryPaperSnapshots(
  eventId: CompetitionId,
  params: {
    stageId?: CompetitionId;
    eventWorkTypeId?: CompetitionId;
    eventWorkTypeLevelId?: CompetitionId;
    scoreRuleId?: CompetitionId;
  },
) {
  return unwrap<CompetitionTheoryPaperSnapshot[]>(
    apiClient.get(`/competition/events/${eventId}/theory-exam/paper-snapshots`, { params }),
  );
}

export function getCompetitionTheoryPaperPreview(
  eventId: CompetitionId,
  snapshotId: CompetitionId,
) {
  return unwrap<CompetitionTheoryPaperPreview>(
    apiClient.get(`/competition/events/${eventId}/theory-exam/paper-snapshots/${snapshotId}/preview`),
  );
}

export function importCompetitionTheoryPaperPair(
  eventId: CompetitionId,
  payload: {
    stageId: CompetitionId;
    eventWorkTypeId: CompetitionId;
    courseId: CompetitionId;
    paperAId: CompetitionId;
    paperBId: CompetitionId;
  },
) {
  return unwrap<CompetitionTheoryPaperSnapshot[]>(
    apiClient.post(`/competition/events/${eventId}/theory-exam/paper-snapshots/import`, payload),
  );
}

export function importCompetitionTheoryPaperSnapshots(
  eventId: CompetitionId,
  payload: {
    stageId: CompetitionId;
    eventWorkTypeId: CompetitionId;
    eventWorkTypeLevelId: CompetitionId;
    paperSnapshotIds: CompetitionId[];
  },
) {
  return unwrap<CompetitionTheoryPaperSnapshot[]>(
    apiClient.post(
      `/competition/events/${eventId}/theory-exam/paper-snapshots/import-competition`,
      payload,
    ),
  );
}

export function listCompetitionTheoryRooms(eventId: CompetitionId) {
  return unwrap<CompetitionTheoryRoom[]>(
    apiClient.get(`/competition/events/${eventId}/theory-exam-rooms`),
  );
}

export function saveCompetitionTheoryRoom(
  eventId: CompetitionId,
  roomId: CompetitionId | undefined,
  payload: Omit<CompetitionTheoryRoom, 'id' | 'eventId'>,
) {
  return unwrap<CompetitionTheoryRoom>(roomId
    ? apiClient.put(`/competition/theory-exam-rooms/${roomId}`, payload)
    : apiClient.post(`/competition/events/${eventId}/theory-exam-rooms`, payload));
}

export function deleteCompetitionTheoryRoom(roomId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/theory-exam-rooms/${roomId}`));
}

export function listCompetitionTheorySchedules(
  eventId: CompetitionId,
  params: { stageId?: CompetitionId; eventWorkTypeId?: CompetitionId },
) {
  return unwrap<CompetitionTheoryScheduleVersion[]>(
    apiClient.get(`/competition/events/${eventId}/theory-schedules`, { params }),
  );
}

export function createCompetitionTheorySchedule(
  eventId: CompetitionId,
  payload: { stageId: CompetitionId; eventWorkTypeId: CompetitionId; scoreRuleId: CompetitionId },
) {
  return unwrap<CompetitionTheoryScheduleVersion>(
    apiClient.post(`/competition/events/${eventId}/theory-schedules`, payload),
  );
}

export function getCompetitionTheorySchedule(versionId: CompetitionId) {
  return unwrap<CompetitionTheoryScheduleVersion>(
    apiClient.get(`/competition/theory-schedules/${versionId}`),
  );
}

export function getCompetitionTheorySchedulePreflight(versionId: CompetitionId) {
  return unwrap<CompetitionTheorySchedulePreflight>(
    apiClient.get(`/competition/theory-schedules/${versionId}/preflight`),
  );
}

export function saveCompetitionTheorySession(
  versionId: CompetitionId,
  sessionId: CompetitionId | undefined,
  payload: { sessionCode: string; sessionName: string; paperSnapshotId: CompetitionId; startAt: string; endAt: string; roomIds: CompetitionId[] },
) {
  return unwrap(sessionId
    ? apiClient.put(`/competition/theory-schedules/${versionId}/sessions/${sessionId}`, payload)
    : apiClient.post(`/competition/theory-schedules/${versionId}/sessions`, payload));
}

export function deleteCompetitionTheorySession(versionId: CompetitionId, sessionId: CompetitionId) {
  return unwrap<void>(apiClient.delete(`/competition/theory-schedules/${versionId}/sessions/${sessionId}`));
}

export function randomAllocateCompetitionTheorySchedule(versionId: CompetitionId) {
  return unwrap<CompetitionTheoryScheduleVersion>(
    apiClient.post(`/competition/theory-schedules/${versionId}/random-allocate`),
  );
}

export function publishCompetitionTheorySchedule(versionId: CompetitionId) {
  return unwrap<CompetitionTheoryScheduleVersion>(
    apiClient.post(`/competition/theory-schedules/${versionId}/publish`),
  );
}

export function withdrawCompetitionTheorySchedule(versionId: CompetitionId, reason: string) {
  return unwrap<CompetitionTheoryScheduleVersion>(
    apiClient.post(`/competition/theory-schedules/${versionId}/withdraw`, { reason }),
  );
}

export async function downloadCompetitionTheoryRoster(versionId: CompetitionId, format: 'xlsx' | 'pdf') {
  const response = await apiClient.get(`/competition/theory-schedules/${versionId}/roster.${format}`, {
    responseType: 'blob',
  });
  return response.data as Blob;
}

export function listCompetitionTheoryMonitor(versionId: CompetitionId) {
  return unwrap<CompetitionTheoryMonitor[]>(
    apiClient.get(`/competition/theory-schedules/${versionId}/monitor`),
  );
}

export function performCompetitionTheoryMonitorAction(
  attemptId: CompetitionId,
  action: 'force-offline' | 'force-submit' | 'restore-absence',
  reason: string,
) {
  return unwrap<void>(apiClient.post(`/competition/theory-attempts/${attemptId}/${action}`, { reason }));
}

export function listCompetitionTheoryResults(versionId: CompetitionId) {
  return unwrap<CompetitionTheoryResultSummary[]>(
    apiClient.get(`/competition/theory-schedules/${versionId}/results`),
  );
}
