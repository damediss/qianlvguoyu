export type CompetitionId = number | string;

export type CompetitionEventScope = 'platform' | 'enterprise_internal';
export type CompetitionEventStatus = 'draft' | 'published' | 'closed' | 'archived';

export interface CompetitionEvent {
  id: CompetitionId;
  name: string;
  code: string;
  scope: CompetitionEventScope;
  ownerOrgId?: CompetitionId | null;
  status: CompetitionEventStatus;
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
  orgId: CompetitionId;
  quota?: number;
  importPermission?: number;
}

export interface CompetitionEventOrgStatusUpdateRequest {
  status: Exclude<CompetitionEventOrgStatus, 'invited'>;
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

export type CompetitionNoticeStatus = 'draft' | 'published';

export interface CompetitionNotice {
  id: CompetitionId;
  eventId: CompetitionId;
  title: string;
  content: string;
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
}

export interface CompetitionNoticeCreateRequest {
  title: string;
  content: string;
  version: number;
}

export type CompetitionNoticeUpdateRequest = CompetitionNoticeCreateRequest;

export interface CompetitionNoticePublishResult {
  noticeId: CompetitionId;
  eventId: CompetitionId;
  beforeStatus: CompetitionNoticeStatus;
  afterStatus: CompetitionNoticeStatus;
}
