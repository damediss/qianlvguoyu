import type { CompetitionId } from './competition';

export type PracticalStartMode = 'UNIFIED_START' | 'MANUAL_START';
export type PracticalOvertimeRuleType = 'NONE' | 'STEP_DEDUCTION' | 'FIXED_DEDUCTION';
export type PracticalEarlyFinishRuleType = 'NONE' | 'STEP_BONUS' | 'FIXED_BONUS';
export type PracticalOvertimeAction = 'ALLOW_CONTINUE' | 'AUTO_FINISH';

export interface CompetitionPracticalScoringScheme {
  id: CompetitionId;
  sourceEventId: CompetitionId;
  sourceEventWorkTypeId: CompetitionId;
  sourceEventWorkTypeLevelId: CompetitionId;
  title: string;
  paperCode: string;
  versionNo: number;
  durationMinutes: number;
  questionCount: number;
  totalScore: number;
  sourceHash?: string | null;
}

export interface CompetitionPracticalExecutionConfig {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  stageContentId: CompetitionId;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  scoringSchemeId: CompetitionId;
  examName: string;
  durationSeconds: number;
  timeUnit: 'SECONDS' | string;
  startMode: PracticalStartMode;
  allowEarlySubmit: boolean;
  timeScoringEnabled: boolean;
  overtimeRuleType: PracticalOvertimeRuleType;
  overtimeStepSeconds?: number | null;
  overtimeScorePerStep?: number | null;
  overtimeFixedDeduction?: number | null;
  overtimeMaxDeduction?: number | null;
  earlyFinishRuleType: PracticalEarlyFinishRuleType;
  earlyFinishStepSeconds?: number | null;
  earlyFinishScorePerStep?: number | null;
  earlyFinishFixedBonus?: number | null;
  earlyFinishMaxBonus?: number | null;
  overtimeAction: PracticalOvertimeAction;
  maxDurationSeconds?: number | null;
  finalScoreUpperLimit: number;
  standardTotalScore: number;
  version: number;
  scoringScheme: CompetitionPracticalScoringScheme;
}

export interface CompetitionPracticalExecutionConfigRequest {
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  scoringSchemeId: CompetitionId;
  examName: string;
  durationSeconds: number;
  startMode: PracticalStartMode;
  allowEarlySubmit: boolean;
  timeScoringEnabled: boolean;
  overtimeRuleType: PracticalOvertimeRuleType;
  overtimeStepSeconds?: number | null;
  overtimeScorePerStep?: number | null;
  overtimeFixedDeduction?: number | null;
  overtimeMaxDeduction?: number | null;
  earlyFinishRuleType: PracticalEarlyFinishRuleType;
  earlyFinishStepSeconds?: number | null;
  earlyFinishScorePerStep?: number | null;
  earlyFinishFixedBonus?: number | null;
  earlyFinishMaxBonus?: number | null;
  overtimeAction: PracticalOvertimeAction;
  maxDurationSeconds?: number | null;
  finalScoreUpperLimit: number;
}

export interface CompetitionPracticalExecutionConfigValidation {
  valid: boolean;
  errors: string[];
  message: string;
}
