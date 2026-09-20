import type { CompetitionDecimal, CompetitionId } from './competition';

export type CompetitionQuestionContentType = 'theory' | 'practical';
export type CompetitionQuestionType = 'single' | 'multi' | 'judge' | 'fill' | 'practical';

export type CompetitionPracticalScoringMethod = 'additive' | 'deductive';

export interface CompetitionPracticalAssessmentElementRequest {
  elementOrder: number;
  elementHtml: string;
  allocationEnabled: boolean;
  maxScore?: CompetitionDecimal;
  standardHtml: string;
  maxDeduction?: CompetitionDecimal;
  deductionStep?: CompetitionDecimal;
}

export interface CompetitionPracticalAssessmentContentRequest {
  sortOrder: number;
  contentHtml: string;
  elements: CompetitionPracticalAssessmentElementRequest[];
}

export interface CompetitionPracticalQuestionDetailRequestV2 {
  durationMinutes: number;
  scoreDifference: CompetitionDecimal;
  totalScore: CompetitionDecimal;
  scoringMethod: CompetitionPracticalScoringMethod;
  assessmentRequirementsHtml?: string;
  examRoomPrepareHtml?: string;
  candidatePrepareHtml?: string;
  negativeItemsHtml?: string;
  questionMaterialsHtml?: string;
  assessmentContents: CompetitionPracticalAssessmentContentRequest[];
}

export interface CompetitionPracticalAssessmentElement extends CompetitionPracticalAssessmentElementRequest {
  id: string;
  itemNo: number;
  status?: string;
}

export interface CompetitionPracticalAssessmentContent extends CompetitionPracticalAssessmentContentRequest {
  id: string;
  totalScore: CompetitionDecimal;
  elements: CompetitionPracticalAssessmentElement[];
}

export interface CompetitionPracticalQuestionDetail {
  durationMinutes?: number | null;
  scoreDifference?: CompetitionDecimal | null;
  totalScore?: CompetitionDecimal | null;
  scoringMethod?: CompetitionPracticalScoringMethod | string | null;
  assessmentRequirementsHtml?: string | null;
  examRoomPrepareHtml?: string | null;
  candidatePrepareHtml?: string | null;
  negativeItemsHtml?: string | null;
  questionMaterialsHtml?: string | null;
  assessmentContents: CompetitionPracticalAssessmentContent[];
}

export interface CompetitionPracticalScoreItemRequest {
  itemNo: number;
  itemName: string;
  criterion?: string;
  maxScore?: CompetitionDecimal | null;
  maxDeduction?: CompetitionDecimal | null;
  deductionStep?: CompetitionDecimal | null;
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
  blueprintId?: string | null;
  blueprintNodeId?: string | null;
  blueprintFullCode?: string | null;
  blueprintPath?: string | null;
  bindingStatus?: 'bound' | 'unbound' | string | null;
  operationContent?: string | null;
  studentPrepare?: string | null;
  equipmentPrepare?: string | null;
  scoreItems: CompetitionPracticalScoreItem[];
  practicalDetail?: CompetitionPracticalQuestionDetail | null;
}

export interface CompetitionPracticalScoreItem extends CompetitionPracticalScoreItemRequest {
  id: string;
  status?: string;
  contentId?: string | null;
  elementOrder?: number | null;
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
  practicalDetail?: CompetitionPracticalQuestionDetailRequestV2 | CompetitionPracticalQuestionDetailRequest;
  scoreItems?: CompetitionPracticalScoreItemRequest[];
  blueprintId?: CompetitionId;
  blueprintNodeId?: CompetitionId;
}

export interface CompetitionQuestionQuery {
  page: number;
  pageSize: number;
  eventWorkTypeId?: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId;
  contentType: CompetitionQuestionContentType;
  questionType?: CompetitionQuestionType;
  keyword?: string;
  blueprintId?: CompetitionId;
  blueprintNodeId?: CompetitionId;
  bindingStatus?: string;
}

export interface CompetitionQuestionImportBatch {
  id: string;
  eventId: string;
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  contentType: CompetitionQuestionContentType;
  blueprintId?: string | null;
  originalFilename: string;
  selectedSheet?: string | null;
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
  blueprintId?: string | null;
  practicalQuestionId?: string | null;
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
  randomSeed?: string | null;
  details: CompetitionMakeExamPlanDetail[];
  topicRules?: CompetitionMakeExamPlanTopicRule[];
}

export interface CompetitionMakeExamPlanRequest {
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  practicalQuestionId?: CompetitionId;
  blueprintId?: CompetitionId;
  contentType: CompetitionQuestionContentType;
  title: string;
  examType?: string;
  durationMinutes: number;
  totalScore: CompetitionDecimal;
  passScore: CompetitionDecimal;
  comments?: string;
  details: CompetitionMakeExamPlanDetail[];
  topicRules?: CompetitionMakeExamPlanTopicRule[];
  paperCount?: number;
  randomSeed?: CompetitionId;
}

export interface CompetitionMakeExamPlanPrecheck {
  ready: boolean;
  candidateCount: number;
  topics: Array<{
    blueprintNodeId: string;
    path: string;
    weight?: CompetitionDecimal | null;
    sortOrder?: number | null;
    required: number;
    available: number;
    shortage: number;
    allocations?: CompetitionMakeExamPlanTheoryCell[];
  }>;
  details: Array<{
    ruleIndex?: number | null;
    sortOrder?: number | null;
    questionType: string;
    required: number;
    available: number;
    shortage: number;
  }>;
  errors: string[];
  theoryAllocations: CompetitionMakeExamPlanTheoryAllocation[];
  unboundQuestionCount: number;
  warnings: string[];
  practicalAllocations?: CompetitionMakeExamPlanPracticalAllocation[];
  practicalQuestionCount?: number;
  practicalReferenceDurationMinutes?: number;
}

export interface CompetitionMakeExamPlanPracticalAllocation {
  selectionGroup: string;
  selectionMode: 'REQUIRED' | 'CHOOSE_ONE' | string;
  blueprintNodeId: string;
  path: string;
  scoreWeight: CompetitionDecimal;
  targetScore?: CompetitionDecimal | null;
  candidateQuestionCount: number;
  candidateNodes: CompetitionMakeExamPlanPracticalCandidateNode[];
  selectedQuestionId?: string | null;
  selectedAssessmentPointId?: string | null;
  sourceTotalScore?: CompetitionDecimal | null;
  scoreScale?: CompetitionDecimal | null;
  referenceDurationMinutes?: number | null;
}

export interface CompetitionMakeExamPlanPracticalCandidateNode {
  blueprintNodeId: string;
  path: string;
  scoreWeight: CompetitionDecimal;
  assessmentPointIds: string[];
  candidateQuestionCount: number;
  candidateQuestionIds: string[];
  selected: boolean;
}

export interface CompetitionMakeExamPlanTheoryCell {
  ruleIndex: number;
  sortOrder: number;
  questionType: string;
  point: CompetitionDecimal;
  difficultyStart?: number | null;
  difficultyEnd?: number | null;
  required: number;
  available: number;
  assigned: number;
  shortage: number;
}

export interface CompetitionMakeExamPlanTheoryAllocation {
  blueprintNodeId: string;
  path: string;
  weight: CompetitionDecimal;
  sortOrder: number;
  questionCount: number;
  available: number;
  assigned: number;
  shortage: number;
  details: CompetitionMakeExamPlanTheoryCell[];
}

export interface CompetitionMakeExamPlanTopicRule {
  id?: string;
  blueprintNodeId: string;
  questionCount: number;
  score: CompetitionDecimal;
  selected: boolean;
  sortOrder: number;
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
  practicalDetailJson?: string | null;
  scoreDifference?: CompetitionDecimal | null;
  scoringMethod?: string | null;
  blueprintId?: string | null;
  blueprintNodeId?: string | null;
  blueprintFullCode?: string | null;
  blueprintPath?: string | null;
  sourceTotalScore?: CompetitionDecimal | null;
  scoreScale?: CompetitionDecimal | null;
}

export interface CompetitionMakeExamSnapshot {
  id: string;
  planId: string;
  blueprintId?: string | null;
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
