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
  status?: string;
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
  status?: string;
  remark?: string;
}

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
