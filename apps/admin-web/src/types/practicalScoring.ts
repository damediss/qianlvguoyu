export interface PracticalTemplateItem {
  itemNo: number;
  itemName: string;
  maxScore: number;
  criterion: string;
  maxDeduction: number;
  deductionStep: number;
}

export interface PracticalScopeOption {
  id: string;
  workTypeId?: string;
  workTypeName?: string;
  levelCode?: string;
  levelName?: string;
}

export interface PracticalGroupOption {
  id: string;
  groupName: string;
  leaderAssessorId?: string;
  memberCount: number;
}

export interface PracticalPaperQuestion {
  id: string;
  title: string;
  score?: number;
  templateReady: boolean;
  templateItems: PracticalTemplateItem[];
}

export interface PracticalPaperOption {
  id: string;
  workTypeId?: string;
  level?: string;
  title: string;
  totalPoint?: number;
  questions: PracticalPaperQuestion[];
}

export interface PracticalScoringConfig {
  id: string;
  taskId: string;
  scopeId: string;
  candidateId?: string;
  workTypeId: string;
  workTypeName: string;
  levelCode: string;
  levelName?: string;
  examDate: string;
  paperId: string;
  groupId: string;
  status: string;
}

export interface PracticalScoringSetup {
  taskId: string;
  examDate?: string;
  scopes: PracticalScopeOption[];
  groups: PracticalGroupOption[];
  papers: PracticalPaperOption[];
  configs: PracticalScoringConfig[];
}

export interface PracticalGenerateResult {
  createdTasks: number;
  existingTasks: number;
  candidates: number;
  questions: number;
}

export interface PracticalImportPaperResult {
  paperId: string;
  title: string;
  questions: number;
  scoreItems: number;
}

export interface PracticalScoreItem {
  id: string;
  itemNo: number;
  itemName: string;
  criterion: string;
  maxScore: number;
  maxDeduction: number;
  deductionStep: number;
  deductedScore: number;
  awardedScore: number;
  remark?: string;
}

export interface AssessorScoringTask {
  id: string;
  taskId: string;
  candidateId: string;
  candidateNo?: string;
  candidateName: string;
  workTypeName?: string;
  levelName?: string;
  paperQuestionId: string;
  questionTitle: string;
  status: 'assigned' | 'draft' | 'submitted' | 'returned';
  totalScore?: number;
  version: number;
  revision: number;
  updatedAt?: string;
  submittedAt?: string;
  leader: boolean;
}

export interface AssessorScoringTaskDetail {
  task: AssessorScoringTask;
  questionContent?: string;
  studentPrepare?: string;
  equipmentPrepare?: string;
  scoreItems: PracticalScoreItem[];
  progressNote?: string;
  returnReason?: string;
  startSession?: PracticalStartSession;
}

export interface AssessorExamListItem {
  taskId: string;
  candidateId: string;
  paperId: string;
  candidateNo?: string;
  candidateName: string;
  workTypeName?: string;
  levelName?: string;
  submittedQuestions: number;
  totalQuestions: number;
  totalPoint?: number;
  currentScore?: number;
  status: 'assigned' | 'draft' | 'submitted' | 'returned';
  updatedAt?: string;
  startSession?: PracticalStartSession;
  resultStatus?: string;
  reviewRequired?: boolean;
}

export interface AssessorExamDetail {
  exam: AssessorExamListItem;
  questions: AssessorScoringTaskDetail[];
}

export interface PracticalStartSession {
  id: string;
  taskId: string;
  candidateId: string;
  paperId: string;
  status: 'waiting' | 'ready' | 'started' | 'completed';
  candidateConfirmed: boolean;
  confirmedAssessors: number;
  requiredAssessors: number;
  currentAssessorConfirmed: boolean;
  version: number;
  startedAt?: string;
  completedAt?: string;
  summaryFileName?: string;
}

export interface PracticalCandidateProgress {
  candidateId: string;
  candidateNo?: string;
  candidateName: string;
  submittedTasks: number;
  totalTasks: number;
  completedQuestions: number;
  totalQuestions: number;
  totalScore?: number;
  status: string;
  startSession?: PracticalStartSession;
}

export interface PracticalTaskProgressItem {
  assessorTaskId: string;
  candidateId: string;
  candidateName: string;
  paperQuestionId: string;
  questionTitle: string;
  assessorId: string;
  assessorName: string;
  status: string;
  totalScore?: number;
  submittedAt?: string;
  questionResultStatus?: string;
  scoreDeviation?: number;
  reviewRequired?: boolean;
}

export interface PracticalScoringProgress {
  taskId: string;
  submittedTasks: number;
  totalTasks: number;
  completedQuestions: number;
  totalQuestions: number;
  candidates: PracticalCandidateProgress[];
  scoringTasks: PracticalTaskProgressItem[];
}

export interface PracticalSaveScorePayload {
  version: number;
  items: Array<{ itemNo: number; deductedScore: number; remark?: string }>;
  note?: string;
}

export interface PracticalAdjustScorePayload extends PracticalSaveScorePayload {
  reason: string;
}
