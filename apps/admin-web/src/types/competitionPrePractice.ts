export interface CompetitionPrePracticeSourceScope {
  sourceWorkTypeIds: string[];
  /** Course-bank levels use national grade codes: 5=初级工 through 1=高级技师. */
  sourceLevelCodes: number[];
}

export interface CompetitionPrePracticeQuestionTypeCount {
  questionType: string;
  questionTypeName: string;
  questionCount: number;
  availableCount: number;
}

export interface CompetitionPrePracticeTask {
  id: string;
  eventId: string;
  eventName: string;
  eventWorkTypeId: string;
  eventWorkTypeName: string;
  eventWorkTypeLevelCode: string;
  eventWorkTypeLevelName: string;
  stageId: string;
  stageName: string;
  title: string;
  effectiveTimeEnabled: boolean;
  effectiveStartAt?: string | null;
  effectiveEndAt?: string | null;
  sourceScope: Array<{
    workTypeId: string;
    workTypeName: string;
    levelCode: number;
    levelName: string;
  }>;
  questionTypeCounts: CompetitionPrePracticeQuestionTypeCount[];
  questionCount: number;
  status: 'configured' | string;
  deliveryStatus: 'not_pushed' | string;
  createdAt?: string | null;
}

export interface CompetitionPrePracticeTaskCreateRequest {
  eventId: string;
  eventWorkTypeId: string;
  eventWorkTypeLevelCode: string;
  stageId: string;
  title: string;
  effectiveTimeEnabled: boolean;
  effectiveStartAt?: string;
  effectiveEndAt?: string;
  sourceScope: CompetitionPrePracticeSourceScope;
  questionTypeQuotas: Array<{ questionType: string; questionCount: number }>;
}

export interface CompetitionPrePracticeTaskQuery {
  page: number;
  pageSize: number;
  eventId?: string;
}

export interface CompetitionPrePracticePushScopeQuery {
  eventId: string;
  eventWorkTypeId: string;
  levelCode: string;
  orgId?: string;
  page: number;
  pageSize: number;
}

export interface CompetitionPrePracticePushContestant {
  profileId: string;
  registrationId: string;
  playerId: string;
  contestantNo?: string | null;
  contestantName: string;
  orgId?: string | null;
  orgName?: string | null;
}

export interface CompetitionPrePracticePushOrganization {
  id?: string | null;
  name: string;
}

export interface CompetitionPrePracticeDispatch {
  id: string;
  taskId: string;
  taskTitle: string;
  eventId: string;
  eventName: string;
  eventWorkTypeId: string;
  eventWorkTypeName: string;
  eventWorkTypeLevelCode: string;
  eventWorkTypeLevelName: string;
  stageId: string;
  stageName: string;
  recipientCount: number;
  pushedAt: string;
}

export interface CompetitionPrePracticeDispatchCreateRequest {
  taskId: string;
  eventId: string;
  eventWorkTypeId: string;
  levelCode: string;
  orgId?: string;
  selectAllMatching: boolean;
  contestantProfileIds: string[];
}

export interface CompetitionPrePracticeDispatchQuery {
  page: number;
  pageSize: number;
  eventId?: string;
  eventWorkTypeId?: string;
  levelCode?: string;
}

export interface CompetitionPlayerPrePracticeTask {
  deliveryId: string;
  batchId: string;
  taskId: string;
  title: string;
  eventId: string;
  eventName: string;
  eventWorkTypeId: string;
  eventWorkTypeName: string;
  eventWorkTypeLevelCode: string;
  eventWorkTypeLevelName: string;
  stageId: string;
  stageName: string;
  questionCount: number;
  status: string;
  deliveredAt: string;
  pushedAt: string;
  effectiveTimeEnabled: boolean;
  effectiveStartAt?: string | null;
  effectiveEndAt?: string | null;
}
