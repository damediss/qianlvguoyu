import type { Edge, Node, XYPosition } from '@xyflow/react';
import type {
  CompetitionId,
  CompetitionStage,
  CompetitionStageContentType,
  CompetitionStageStatus,
} from './competition';

export const STAGE_DEFINITIONS = [
  { stageType: 'qualification', name: '资格赛', sortOrder: 10 },
  { stageType: 'preliminary', name: '初赛', sortOrder: 20 },
  { stageType: 'quarterfinal', name: '复赛', sortOrder: 30 },
  { stageType: 'semifinal', name: '半决赛', sortOrder: 40 },
  { stageType: 'final', name: '决赛', sortOrder: 50 },
] as const;

export type StageType = typeof STAGE_DEFINITIONS[number]['stageType'];
export type StageNodeKind = 'stage' | 'marker';
export type StageMarkerKind = 'start' | 'end';

export interface StageContentConfig {
  contentType: CompetitionStageContentType;
  weight: number;
}

export interface StageBusinessNodeData {
  [key: string]: unknown;
  kind: 'stage';
  stageType: StageType;
  stageId?: CompetitionId;
  name: string;
  sortOrder: number;
  status: CompetitionStageStatus;
  startAt?: string | null;
  endAt?: string | null;
  description?: string | null;
  contents?: StageContentConfig[];
}

export interface StageMarkerNodeData {
  [key: string]: unknown;
  kind: 'marker';
  marker: StageMarkerKind;
  label: string;
}

export type StageNodeData = StageBusinessNodeData | StageMarkerNodeData;
export type StageNode = Node<StageNodeData, StageNodeKind>;

export interface StageEdgeData {
  [key: string]: unknown;
  business: boolean;
  note?: string;
}

export type StageEdge = Edge<StageEdgeData>;

export interface StageDraftSaveItem {
  id?: string;
  stageType: StageType;
  startAt?: string;
  endAt?: string;
  description?: string;
  contents?: StageContentConfig[];
}

export interface StageDraftSavePayload {
  stages: StageDraftSaveItem[];
}

export type StageValidationLevel = 'error' | 'warning';
export type StageValidationTargetType = 'node' | 'edge' | 'canvas';

export interface StageValidationIssue {
  code: string;
  level: StageValidationLevel;
  targetType: StageValidationTargetType;
  targetId?: string;
  message: string;
}

export type StageGraph = {
  nodes: StageNode[];
  edges: StageEdge[];
};

export type StageGraphSelection = {
  nodeId?: string;
  edgeId?: string;
};

export type StageCanvasNotes = {
  schemaVersion: 1;
  tenantId: string;
  eventId: string;
  notes: Record<string, string>;
};

export function isBusinessStageNode(node: StageNode): node is StageNode & { data: StageBusinessNodeData } {
  return node.data.kind === 'stage';
}

export function stageTypeDefinition(stageType: string) {
  return STAGE_DEFINITIONS.find((item) => item.stageType === stageType);
}

export function stageTypeSortOrder(stageType: string) {
  return stageTypeDefinition(stageType)?.sortOrder ?? Number.MAX_SAFE_INTEGER;
}

export function stageTypeDisplayOrder(stageType: string) {
  const index = STAGE_DEFINITIONS.findIndex((item) => item.stageType === stageType);
  return index >= 0 ? index + 1 : undefined;
}

export function stageStatusLabel(status: CompetitionStageStatus, stagePlanConfirmed = false) {
  if (stagePlanConfirmed && status === 'draft') return '已锁定';
  return {
    draft: '草稿',
    configuring: '配置中',
    ready_locked: '已锁定',
    in_progress: '进行中',
    closed: '已关闭',
    settled: '已结算',
    result_published: '结果已发布',
  }[status];
}

export function stageNodeId(stageType: StageType) {
  return `stage:${stageType}`;
}

export function stageEdgeId(sourceType: StageType, targetType: StageType) {
  return `edge:${sourceType}:${targetType}`;
}

export function stageNoteKey(sourceType: StageType, targetType: StageType) {
  return `${sourceType}->${targetType}`;
}

export function stageNodePosition(index: number): XYPosition {
  return { x: 120 + index * 260, y: 180 };
}

export function toStageBusinessNode(stage: CompetitionStage, index: number): StageNode {
  const definition = stageTypeDefinition(stage.stageType);
  if (!definition) {
    throw new Error(`未知赛段类型：${stage.stageType}`);
  }
  return {
    id: stageNodeId(definition.stageType),
    type: 'stage',
    position: stageNodePosition(index),
    data: {
      kind: 'stage',
      stageType: definition.stageType,
      stageId: stage.id,
      name: definition.name,
      sortOrder: definition.sortOrder,
      status: stage.status,
      startAt: stage.startAt,
      endAt: stage.endAt,
      description: stage.description,
    },
  };
}
