export interface CertificationTask {
  id: string;
  taskNo: string;
  taskName: string;
  taskManagerName: string;
  currentPhase: string;
  currentPhaseName: string;
  status: string;
  statusName: string;
  sourceType: string;
  sourceName: string;
  companyName?: string;
  certificationType?: string;
  remark?: string;
  workTypeName?: string;
  levelCode?: string;
  levelName?: string;
  scopes?: CertificationTaskScope[];
  plannedPeopleCount?: number;
  planStartDate?: string;
  planEndDate?: string;
  examDate?: string;
  documentCompletedCount?: number;
  documentTotalCount?: number;
  phaseCompletedCount?: number;
  phaseTotalCount?: number;
  lastBusinessUpdatedAt?: string;
}

export interface CertificationTaskQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: CertificationTaskStatus | string;
  currentPhase?: CertificationTaskPhase | string;
  levelCode?: string;
  taskNo?: string;
}

export interface CertificationTaskPayload {
  taskNo?: string;
  taskName: string;
  taskManagerName: string;
  companyName?: string;
  certificationType?: string;
  scopes?: CertificationTaskScopePayload[];
  plannedPeopleCount?: number;
  planStartDate?: string;
  planEndDate?: string;
  examDate?: string;
  sourceType?: string;
  status?: CertificationTaskStatus | string;
  currentPhase?: CertificationTaskPhase | string;
  remark?: string;
}

// 任务阶段（current_phase）
export type CertificationTaskPhase =
  | 'task_creation'
  | 'preparation'
  | 'archive'
  | 'certification_completed';

// 任务状态（status）
export type CertificationTaskStatus =
  | 'in_progress'
  | 'pending_review'
  | 'rejected'
  | 'completed';

export const CERTIFICATION_TASK_PHASES: readonly CertificationTaskPhase[] = [
  'task_creation',
  'preparation',
  'archive',
  'certification_completed',
] as const;

export const CERTIFICATION_TASK_STATUSES: readonly CertificationTaskStatus[] = [
  'in_progress',
  'pending_review',
  'rejected',
  'completed',
] as const;

export const CERTIFICATION_TASK_PHASE_LABELS: Record<CertificationTaskPhase, string> = {
  task_creation: '任务创建阶段',
  preparation: '认定筹备阶段',
  archive: '认定归档阶段',
  certification_completed: '认定完成',
};

export const CERTIFICATION_TASK_STATUS_LABELS: Record<CertificationTaskStatus, string> = {
  in_progress: '进行中',
  pending_review: '待审核',
  rejected: '已驳回',
  completed: '已完成',
};

export interface CertificationTaskScope {
  id: string;
  certificationType?: string;
  workTypeName?: string;
  levelCode?: string;
  levelName?: string;
  plannedPeopleCount?: number;
}

export interface CertificationTaskScopePayload {
  certificationType?: string;
  workTypeName?: string;
  levelCode?: string;
  levelName?: string;
  plannedPeopleCount?: number;
}

export interface CertificationPhase {
  id: string;
  phaseCode: string;
  phaseName: string;
  phaseOrder: number;
  status: string;
  startedAt?: string;
  completedAt?: string;
}

export interface CertificationDocument {
  id: string;
  documentType: CertificationDocumentType;
  documentName: string;
  versionNo: number;
  status: string;
  completionPercent?: number;
  contentText?: string;
  contentJson?: string;
  submittedAt?: string;
  approvedAt?: string;
  rejectedAt?: string;
  rejectReason?: string;
  processInstanceId?: string;
  lastSavedAt?: string;
}

export type CertificationDocumentType = 'charter' | 'members' | 'wbs' | 'schedule' | 'risk';

export interface CertificationDocumentPayload {
  contentText?: string;
  contentJson?: string;
  remark?: string;
}

export interface CertificationDocumentReviewPayload {
  approved: boolean;
  comment?: string;
}

export interface CertificationDocumentWorkflowRecord {
  id: string;
  processInstanceId?: string;
  engineTaskId?: string;
  nodeKey?: string;
  nodeName?: string;
  action: string;
  actionResult: string;
  comment?: string;
  operatorUserId?: string;
  operatorName?: string;
  operatedAt?: string;
}

export interface CertificationDocumentWorkflow {
  documentId?: string;
  documentType: CertificationDocumentType;
  documentStatus: string;
  processInstanceId?: string;
  currentNodeKey?: string;
  currentNodeName?: string;
  records: CertificationDocumentWorkflowRecord[];
}

export interface CertificationMember {
  id?: string;
  userId?: string;
  memberName: string;
  roleCode?: string;
  roleName?: string;
  departmentName?: string;
  responsibility?: string;
  startDate?: string;
  endDate?: string;
  inputFrequency?: string;
  workloadPercent?: number;
  maskedPhone?: string;
  phone?: string;
  remark?: string;
}

export interface CertificationMemberUser {
  id: string;
  username: string;
  displayName: string;
  phone?: string;
}

export interface CertificationWbsItem {
  id?: string;
  wbsCode: string;
  categoryCode?: string;
  categoryName?: string;
  itemName: string;
  activityDescription?: string;
  estimateHours?: number;
  manpowerCount?: number;
  manpowerRoleCode?: string;
  responsibleMemberId?: string;
  resourceInput?: string;
  costAmount?: number;
  durationDays?: number;
  predecessorId?: string;
  progressPercent?: number;
  status?: string;
}

export interface CertificationScheduleItem {
  id?: string;
  dayNo?: number;
  planDate?: string;
  startDate?: string;
  endDate?: string;
  itemName: string;
  itemDescription?: string;
  required?: boolean;
  completed?: boolean;
  externalContactMemberId?: string;
  certificationOwnerMemberId?: string;
}

export interface CertificationRisk {
  id?: string;
  riskNo?: string;
  riskDescription: string;
  probabilityCode: string;
  impactCode: string;
  riskLevelCode?: string;
  riskLevelScore?: number;
  responsePlan?: string;
  ownerMemberId?: string;
  ownerName?: string;
  status?: string;
  expectedCloseDate?: string;
  remark?: string;
}

export interface CertificationRiskStatusPayload {
  status: string;
  closeReason?: string;
}

export interface CertificationWorkbench {
  task: CertificationTask;
  phases: CertificationPhase[];
  documents: CertificationDocument[];
  members: CertificationMember[];
  wbsItems: CertificationWbsItem[];
  scheduleItems: CertificationScheduleItem[];
  risks: CertificationRisk[];
}
