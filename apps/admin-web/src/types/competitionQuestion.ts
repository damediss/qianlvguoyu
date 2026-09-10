import type { CompetitionDecimal, CompetitionId } from './competition';

export type CompetitionQuestionContentType = 'theory' | 'practical';
export type CompetitionQuestionType = 'single' | 'multi' | 'judge' | 'fill' | 'practical';

export interface CompetitionPracticalScoreItemRequest {
  itemNo: number;
  itemName: string;
  criterion?: string;
  maxScore: CompetitionDecimal;
  maxDeduction: CompetitionDecimal;
  deductionStep: CompetitionDecimal;
}

export interface CompetitionPracticalQuestionDetailRequest {
  operationContent?: string;
  studentPrepare?: string;
  equipmentPrepare?: string;
}

export interface CompetitionQuestion {
  id: string;
  eventId: string;
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  contentType: CompetitionQuestionContentType;
  questionType: CompetitionQuestionType;
  sortOrder: number;
  stem: string;
  referenceAnswer?: string | null;
  fillAnswer?: string | null;
  optionsJson?: string | null;
  difficulty?: number | null;
  status: 'enabled' | 'disabled' | string;
  sourceBatchId?: string | null;
  operationContent?: string | null;
  studentPrepare?: string | null;
  equipmentPrepare?: string | null;
  scoreItems: CompetitionPracticalScoreItem[];
}

export interface CompetitionPracticalScoreItem extends CompetitionPracticalScoreItemRequest {
  id: string;
  status?: string;
}

export interface CompetitionQuestionRequest {
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  contentType: CompetitionQuestionContentType;
  questionType: CompetitionQuestionType;
  stem: string;
  referenceAnswer?: string;
  fillAnswer?: string;
  optionsJson?: string;
  difficulty?: number;
  status?: string;
  practicalDetail?: CompetitionPracticalQuestionDetailRequest;
  scoreItems?: CompetitionPracticalScoreItemRequest[];
}

export interface CompetitionQuestionQuery {
  page: number;
  pageSize: number;
  eventWorkTypeId?: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId;
  contentType: CompetitionQuestionContentType;
  questionType?: CompetitionQuestionType;
  keyword?: string;
}

export interface CompetitionQuestionImportBatch {
  id: string;
  eventId: string;
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  contentType: CompetitionQuestionContentType;
  originalFilename: string;
  status: 'pending' | 'processing' | 'succeeded' | 'failed' | string;
  totalCount: number;
  successCount: number;
  failureCount: number;
  errorDetail?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
}

export interface CompetitionMakeExamPlanDetail {
  id?: string;
  questionType: CompetitionQuestionType;
  questionCount: number;
  point: CompetitionDecimal;
  difficultyStart?: number | null;
  difficultyEnd?: number | null;
  sortOrder: number;
}

export interface CompetitionMakeExamPlan {
  id: string;
  eventId: string;
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  contentType: CompetitionQuestionContentType;
  title: string;
  examType?: string | null;
  durationMinutes: number;
  totalScore: CompetitionDecimal;
  passScore: CompetitionDecimal;
  paperCount: number;
  planVersion: number;
  status: 'draft' | 'published' | 'withdrawn' | string;
  comments?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  details: CompetitionMakeExamPlanDetail[];
}

export interface CompetitionMakeExamPlanRequest {
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  contentType: CompetitionQuestionContentType;
  title: string;
  examType?: string;
  durationMinutes: number;
  totalScore: CompetitionDecimal;
  passScore: CompetitionDecimal;
  comments?: string;
  details: CompetitionMakeExamPlanDetail[];
}

export interface CompetitionMakeExamSnapshotQuestion {
  id: string;
  sourceQuestionId: string;
  sortOrder: number;
  questionType: CompetitionQuestionType;
  stem: string;
  optionsJson?: string | null;
  referenceAnswer?: string | null;
  fillAnswer?: string | null;
  score: CompetitionDecimal;
  difficulty?: number | null;
  operationContent?: string | null;
  studentPrepare?: string | null;
  equipmentPrepare?: string | null;
  scoringItemsJson?: string | null;
}

export interface CompetitionMakeExamSnapshot {
  id: string;
  planId: string;
  eventId: string;
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  contentType: CompetitionQuestionContentType;
  paperCode: 'A' | 'B' | 'P' | string;
  versionNo: number;
  title: string;
  durationMinutes: number;
  questionCount: number;
  totalScore: CompetitionDecimal;
  passScore: CompetitionDecimal;
  status: string;
  questions: CompetitionMakeExamSnapshotQuestion[];
}
