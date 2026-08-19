export interface DefenseGroupOption {
  id: string;
  groupName?: string;
  leaderAssessorId?: string;
  leaderAssessorName?: string;
  memberCount: number;
}

export interface DefenseConfig {
  id: string;
  taskId: string;
  groupId: string;
  groupName?: string;
  status?: string;
  memberCount: number;
}

export interface DefenseCandidateRow {
  candidateId: string;
  candidateNo?: string;
  candidateName: string;
  maskedIdCardNo?: string;
  idCardNo?: string;
  workTypeName?: string;
  levelName?: string;
  orgName?: string;
  reportId?: string;
  fileId?: string;
  fileName?: string;
  uploadedAt?: string;
}

export interface DefenseSetup {
  taskId: string;
  config?: DefenseConfig | null;
  groups: DefenseGroupOption[];
  candidates: DefenseCandidateRow[];
}

export interface DefenseUnmatchedFile {
  fileId?: string;
  fileName?: string;
  reason?: string;
}

export interface DefenseMatchResult {
  matched: number;
  unmatched: number;
  replaced: number;
  unmatchedFiles: DefenseUnmatchedFile[];
}

export type DefenseScoreSheetType = 'THESIS' | 'PERFORMANCE';
export type DefenseScoreSheetStatus = 'empty' | 'draft' | 'submitted' | string;

export interface DefenseAssessorReportListItem {
  reportId: string;
  taskId: string;
  taskName?: string;
  candidateId: string;
  candidateName?: string;
  maskedIdCardNo?: string;
  admissionTicketNo?: string;
  workTypeName?: string;
  levelName?: string;
  fileId: string;
  fileName?: string;
  thesisStatus?: DefenseScoreSheetStatus;
  thesisFinalScore?: number | string | null;
  thesisRejected?: boolean;
  performanceStatus?: DefenseScoreSheetStatus;
  performanceFinalScore?: number | string | null;
  performanceRejected?: boolean;
}

export interface DefenseAssessorReportDetail extends DefenseAssessorReportListItem {
  orgName?: string;
}

export interface DefenseThesisPayload {
  paperTitle?: string;
  rejectFlags?: boolean[];
  rejectNote?: string;
  contentScores?: Array<number | null>;
  questions?: string[];
  defenseScores?: Array<number | null>;
}

export interface DefensePerformanceItemScore {
  key: string;
  score?: number | null;
}

export interface DefensePerformancePayload {
  items?: DefensePerformanceItemScore[];
  disqualify?: boolean;
  remark?: string;
}

export interface DefenseScoreSheetView {
  sheetType: DefenseScoreSheetType | string;
  status: DefenseScoreSheetStatus;
  thesis?: DefenseThesisPayload | null;
  performance?: DefensePerformancePayload | null;
  contentSubtotal?: number | string | null;
  defenseSubtotal?: number | string | null;
  rawTotal?: number | string | null;
  finalScore?: number | string | null;
  rejected?: boolean;
  signatureFileId?: string | null;
  signatureHash?: string | null;
  assessorName?: string;
  submittedAt?: string | null;
}

export interface DefenseScoreSheetsView {
  candidate: DefenseAssessorReportDetail;
  thesis: DefenseScoreSheetView;
  performance: DefenseScoreSheetView;
}

export interface DefenseSaveScoreSheetReq {
  thesis?: DefenseThesisPayload | null;
  performance?: DefensePerformancePayload | null;
  signatureFileId?: string | number | null;
  signatureHash?: string | null;
}

export interface DefenseSubmitScoreSheetReq {
  thesis?: DefenseThesisPayload | null;
  performance?: DefensePerformancePayload | null;
  signatureFileId: string | number;
  signatureHash: string;
}

export type DefenseOverallStatus = 'not_configured' | 'scoring' | 'ready_to_complete' | 'completed' | string;
export type DefenseCandidateProgressStatus = 'pending_report' | 'pending' | 'scoring' | 'completed' | string;

export interface DefenseExpertView {
  assessorId: string;
  assessorName?: string;
  leader?: boolean;
}

export interface DefenseAssessorScoreView {
  assessorId: string;
  assessorName?: string;
  leader?: boolean;
  thesisStatus?: DefenseScoreSheetStatus;
  thesisScore?: number | string | null;
  thesisRejected?: boolean;
  performanceStatus?: DefenseScoreSheetStatus;
  performanceScore?: number | string | null;
  performanceRejected?: boolean;
}

export interface DefenseCandidateProgress {
  candidateId: string;
  candidateNo?: string;
  admissionTicketNo?: string;
  candidateName: string;
  maskedIdCardNo?: string;
  idCardNo?: string;
  workTypeName?: string;
  levelName?: string;
  reportUploaded: boolean;
  status: DefenseCandidateProgressStatus;
  thesisScores: Array<number | string | null>;
  thesisAverage?: number | string | null;
  performanceScores: Array<number | string | null>;
  performanceAverage?: number | string | null;
  finalScore?: number | string | null;
  rejected?: boolean;
  assessors: DefenseAssessorScoreView[];
}

export interface DefenseProgress {
  taskId: string;
  taskNo?: string;
  taskName?: string;
  workTypeName?: string;
  levelName?: string;
  examDate?: string;
  examSite?: string;
  overallStatus: DefenseOverallStatus;
  configStatus?: string;
  groupName?: string;
  submittedCandidateCount: number;
  totalCandidateCount: number;
  experts: DefenseExpertView[];
  candidates: DefenseCandidateProgress[];
}

export interface DefenseCompleteResult {
  taskId: string;
  status: string;
  candidateCount: number;
}
