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

export interface DefenseAssessorReportListItem {
  reportId: string;
  taskId: string;
  taskName?: string;
  candidateId: string;
  candidateName?: string;
  maskedIdCardNo?: string;
  workTypeName?: string;
  levelName?: string;
  fileId: string;
  fileName?: string;
}

export interface DefenseAssessorReportDetail extends DefenseAssessorReportListItem {
  orgName?: string;
}
