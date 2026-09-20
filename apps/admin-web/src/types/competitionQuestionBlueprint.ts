import type { CompetitionId } from './competition';
import type { CompetitionQuestionContentType } from './competitionQuestion';

export interface CompetitionQuestionBlueprint {
  id: string;
  eventId: string;
  eventWorkTypeId: string;
  eventWorkTypeLevelId: string;
  contentType: CompetitionQuestionContentType;
  versionNo: number;
  title: string;
  sourceFileName: string;
  sourceFileId?: string | null;
  pointCount: number;
  topicCount: number;
  xyzRatio?: string | null;
  weightCheck: number;
  checkReport?: string | null;
  status: 'draft' | 'active' | 'archived' | string;
  createdAt?: string | null;
}

export interface CompetitionQuestionBlueprintNode {
  id: string;
  parentId: string;
  nodeLevel: number;
  code: string;
  fullCode: string;
  name: string;
  weight?: number | string | null;
  selectMode?: string | null;
  skillRequirement?: string | null;
  relatedKnowledge?: string | null;
  importance?: string | null;
  xyzStat?: string | null;
  questionQuota?: number | null;
  sortOrder: number;
  children?: CompetitionQuestionBlueprintNode[];
}

export interface CompetitionQuestionBlueprintTree {
  blueprint: CompetitionQuestionBlueprint;
  tree: CompetitionQuestionBlueprintNode[];
}

export interface CompetitionQuestionBlueprintCoverage {
  blueprintId: string;
  totalPoints: number;
  coveredPoints: number;
  totalQuestions: number;
  boundQuestions: number;
  points: Array<{
    nodeId: string;
    fullCode: string;
    path: string;
    name: string;
    questionQuota?: number | null;
    boundQuestionCount: number;
    shortage: number;
  }>;
}

export interface CompetitionQuestionBlueprintQuery {
  page: number;
  pageSize: number;
  eventWorkTypeId?: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId;
  contentType: CompetitionQuestionContentType;
  status?: string;
}
