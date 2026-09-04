/**
 * 理论知识试题生成管理接口封装 —— 方案 v1.2 §T5
 * 权限码 exam:theory:blueprint:* / exam:theory:gen:* / exam:theory:draft:*
 */
import { apiClient, unwrap } from '../../api/client';
import type { PageResponse } from '../../types/api';

// ------------------------------------------------------------------
// 类型
// ------------------------------------------------------------------

export interface TheoryBlueprint {
  id: string;
  workTypeId: number;
  workTypeName?: string | null;
  level: number;
  levelName?: string;
  courseId?: number | null;
  title: string;
  sourceFileName?: string | null;
  pointCount: number;
  xyzRatio?: string | null;
  weightCheck: number;
  checkReport?: string | null;
  status: string;
}

export interface BlueprintNode {
  id: string;
  parentId: string;
  nodeLevel: number;
  code: string;
  fullCode: string;
  name: string;
  weight?: number | null;
  importance?: string | null;
  relatedKnowledge?: string | null;
  xyzStat?: string | null;
  knowledgeId?: string | null;
  sortOrder: number;
  children: BlueprintNode[];
}

export interface TheoryGenBatch {
  id: string;
  blueprintId: string;
  modelId: string;
  thinking?: string | null;
  reasoningEffort?: string | null;
  quotaConfig?: string | null;
  status: 'pending' | 'running' | 'paused' | 'done' | 'failed';
  totalPoints: number;
  donePoints: number;
  failedPoints: number;
  totalQuestions: number;
  tokenUsage: number;
  failReason?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
}

export interface TheoryDraft {
  id: string;
  batchId: string;
  blueprintId: string;
  nodeId: string;
  knowledgeId?: string | null;
  questionType: string;
  typeCode: string;
  difficulty: number;
  consistency: number;
  content: string;
  options?: string | null;
  answer: string;
  analysis?: string | null;
  referenceAnswer?: string | null;
  scorePoints?: string | null;
  scoringStandard?: string | null;
  score?: number | null;
  aiModel?: string | null;
  genDurationMs?: number | null;
  reviewStatus: 'draft' | 'approved' | 'rejected';
  publishStatus: number;
  publishedTaskType?: string | null;
  publishedTaskId?: string | null;
  publishedDetailId?: string | null;
}

// ------------------------------------------------------------------
// 细目表批次
// ------------------------------------------------------------------

export const importBlueprint = (file: File, workTypeId: number, level: number) => {
  const form = new FormData();
  form.append('file', file);
  form.append('workTypeId', String(workTypeId));
  form.append('level', String(level));
  return unwrap<TheoryBlueprint>(
    apiClient.post('/exam/theory/blueprint/import', form));
};

export const listBlueprints = (params: { workTypeId?: number; level?: number; page?: number; pageSize?: number }) =>
  unwrap<PageResponse<TheoryBlueprint>>(
    apiClient.get('/exam/theory/blueprint/page', { params }));

export const getBlueprintTree = (id: string) =>
  unwrap<{ blueprint: TheoryBlueprint; tree: BlueprintNode[] }>(
    apiClient.get(`/exam/theory/blueprint/${id}/tree`));

export const deleteBlueprint = (id: string) =>
  unwrap<void>(apiClient.delete(`/exam/theory/blueprint/${id}`));

// ------------------------------------------------------------------
// 生成批次
// ------------------------------------------------------------------

export const submitGenBatch = (blueprintId: string, modelConfigId: string, quota?: Record<string, number>) =>
  unwrap<TheoryGenBatch>(
    apiClient.post('/exam/theory/gen/submit', { blueprintId, modelConfigId, quota }));

export const listGenBatches = (params: { blueprintId?: string; page?: number; pageSize?: number }) =>
  unwrap<PageResponse<TheoryGenBatch>>(
    apiClient.get('/exam/theory/gen/page', { params }));

export const getGenProgress = (batchId: string) =>
  unwrap<TheoryGenBatch>(
    apiClient.get(`/exam/theory/gen/${batchId}/progress`));

export const pauseGenBatch = (batchId: string) =>
  unwrap<TheoryGenBatch>(apiClient.post(`/exam/theory/gen/${batchId}/pause`));

export const resumeGenBatch = (batchId: string) =>
  unwrap<TheoryGenBatch>(apiClient.post(`/exam/theory/gen/${batchId}/resume`));

export const retryGenBatch = (batchId: string) =>
  unwrap<TheoryGenBatch>(apiClient.post(`/exam/theory/gen/${batchId}/retry-failed`));

export interface TheoryModelConfig {
  id: string;
  modelCode: string;
  modelName: string;
  modelId: string;
  thinking?: string | null;
  reasoningEffort?: string | null;
}

/** 启用中的模型配置（生成任务下拉，默认选中 theory-gen-glm） */
export const listTheoryModelConfigs = () =>
  unwrap<TheoryModelConfig[]>(apiClient.get('/exam/theory/gen/model-configs'));

// ------------------------------------------------------------------
// 草稿
// ------------------------------------------------------------------

export interface DraftQuery {
  batchId: string;
  questionType?: string;
  reviewStatus?: string;
  publishStatus?: number;
  nodeId?: string;
  page?: number;
  pageSize?: number;
}

export const listDrafts = (params: DraftQuery) =>
  unwrap<PageResponse<TheoryDraft>>(
    apiClient.get('/exam/theory/draft/page', { params }));

export const editDraft = (id: string, patch: Partial<TheoryDraft>) =>
  unwrap<TheoryDraft>(apiClient.put(`/exam/theory/draft/${id}`, patch));

export const reviewDrafts = (draftIds: string[], approve: boolean) =>
  unwrap<number>(apiClient.post('/exam/theory/draft/review', { draftIds, approve }));

export const publishDrafts = (batchId: string, taskMode: 'create' | 'append',
    taskId?: string, subTaskId?: string, confirmedBelowQuota?: boolean) =>
  unwrap<{ published: number; objectiveCount: number; subjectiveCount: number; skipped: number;
    examTaskId?: string | null; examsubTaskId?: string | null }>(
    apiClient.post('/exam/theory/draft/publish', {
      batchId, taskMode, taskId, subTaskId, confirmedBelowQuota }));

export const exportDrafts = async (batchId: string): Promise<Blob> => {
  const resp = await apiClient.get(`/exam/theory/draft/export`, {
    params: { batchId },
    responseType: 'blob',
  });
  return resp.data as Blob;
};
