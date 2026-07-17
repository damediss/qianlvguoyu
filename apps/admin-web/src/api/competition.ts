import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  CompetitionEvent,
  CompetitionEventCreateRequest,
  CompetitionEventOrg,
  CompetitionEventOrgCreateRequest,
  CompetitionEventOrgPageQuery,
  CompetitionEventOrgStatusUpdateRequest,
  CompetitionRosterImportDetail,
  CompetitionRosterImport,
  CompetitionRosterImportListQuery,
  CompetitionRosterImportPageQuery,
  CompetitionNotice,
  CompetitionNoticeCreateRequest,
  CompetitionNoticePageQuery,
  CompetitionNoticePublishResult,
  CompetitionNoticeUpdateRequest,
  CompetitionEventPageQuery,
  CompetitionEventPublishResult,
  CompetitionEventUpdateRequest,
  CompetitionId,
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
  payload: { fileId: CompetitionId; orgId: CompetitionId },
) {
  return unwrap<CompetitionRosterImportDetail>(
    apiClient.post(`/competition/events/${eventId}/roster-imports/precheck`, payload),
  );
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
