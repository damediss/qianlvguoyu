/**
 * 操作技能试题生成管理接口封装 —— 方案 v1.1 §T5
 * 权限码 exam:skill:blueprint:* / exam:skill:gen:* / exam:skill:draft:*
 */
import { apiClient, unwrap } from '../../api/client';
import type { PageResponse } from '../../types/api';

// ------------------------------------------------------------------
// 类型
// ------------------------------------------------------------------

export interface SkillBlueprint {
  id: string;
  workTypeId: number;
  workTypeName?: string | null;
  level: number;
  levelName?: string;
  title: string;
  sourceFileName?: string | null;
  sourceFileId?: string | null;
  pointCount: number;
  xyzRatio?: string | null;
  weightCheck: number;
  checkReport?: string | null;
  status: string;
  createdAt?: string;
}

export interface SkillBlueprintNode {
  id: string;
  parentId: string;
  nodeLevel: number;
  code: string;
  fullCode: string;
  name: string;
  weight?: number | null;
  selectMode?: string | null;
  skillRequirement?: string | null;
  importance?: string | null;
  questionQuota?: number | null;
  xyzStat?: string | null;
  sortOrder: number;
  children: SkillBlueprintNode[];
}

export interface SkillGenBatch {
  id: string;
  blueprintId: string;
  workTypeId?: number | null;
  level?: number | null;
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
  createdAt?: string;
  startedAt?: string | null;
  finishedAt?: string | null;
  approvedCount?: number;
  pendingReviewCount?: number;
  rejectedCount?: number;
  publishedCount?: number;
}

export interface SkillScoringItem {
  item: string;
  score: string;
  scoreDesc: string;
  maxDeduct: string;
  step: string;
}

export interface SkillDraft {
  id: string;
  batchId: string;
  blueprintId: string;
  nodeId: string;
  questionName: string;
  score?: number | null;
  weight?: number | null;
  durationMinutes?: number | null;
  examForm?: string | null;
  sitePreparation?: string | null;
  examineePrepare?: string | null;
  requirements?: string | null;
  negatives?: string | null;
  scoringItems?: string | null;
  difficulty?: number | null;
  consistency?: number | null;
  aiModel?: string | null;
  reviewStatus: 'draft' | 'approved' | 'rejected';
  publishStatus: number;
  publishedTaskId?: string | null;
  publishedDetailId?: string | null;
}

export interface PublishPreview {
  approvedCount: number;
  cumulativeTotal: number;
  cumulativeAiTotal: number;
  floor: number;
  afterTotal: number;
  gap: number;
  canPublish: boolean;
}

// ------------------------------------------------------------------
// 细目表批次
// ------------------------------------------------------------------

export const importSkillBlueprint = (file: File, workTypeId: number, level: number) => {
  const form = new FormData();
  form.append('file', file);
  form.append('workTypeId', String(workTypeId));
  form.append('level', String(level));
  return unwrap<SkillBlueprint>(
    apiClient.post('/exam/skill/blueprint/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    }));
};

export const listSkillBlueprints = (params: { workTypeId?: number; level?: number;
  page?: number; pageSize?: number }) =>
  unwrap<PageResponse<SkillBlueprint>>(
    apiClient.get('/exam/skill/blueprint/page', { params }));

export const getSkillBlueprintTree = (id: string) =>
  unwrap<{ blueprint: SkillBlueprint; tree: SkillBlueprintNode[] }>(
    apiClient.get(`/exam/skill/blueprint/${id}/tree`));

export const deleteSkillBlueprint = (id: string) =>
  unwrap<void>(apiClient.delete(`/exam/skill/blueprint/${id}`));

export const downloadSkillBlueprintSource = async (id: string): Promise<Blob> => {
  const resp = await apiClient.get(`/exam/skill/blueprint/${id}/source-file`, {
    responseType: 'blob',
    timeout: 120000,
  });
  return resp.data as Blob;
};

// ------------------------------------------------------------------
// 生成批次
// ------------------------------------------------------------------

export const submitSkillGenBatch = (blueprintId: string, modelConfigId: string) =>
  unwrap<SkillGenBatch>(
    apiClient.post('/exam/skill/gen/submit', { blueprintId, modelConfigId }));

export const listSkillGenBatches = (params: { blueprintId?: string; status?: string;
  page?: number; pageSize?: number }) =>
  unwrap<PageResponse<SkillGenBatch>>(
    apiClient.get('/exam/skill/gen/page', { params }));

export const getSkillGenProgress = (batchId: string) =>
  unwrap<SkillGenBatch>(apiClient.get(`/exam/skill/gen/${batchId}/progress`));

export const pauseSkillGenBatch = (batchId: string) =>
  unwrap<SkillGenBatch>(apiClient.post(`/exam/skill/gen/${batchId}/pause`));

export const resumeSkillGenBatch = (batchId: string) =>
  unwrap<SkillGenBatch>(apiClient.post(`/exam/skill/gen/${batchId}/resume`));

export const retrySkillGenBatch = (batchId: string) =>
  unwrap<SkillGenBatch>(apiClient.post(`/exam/skill/gen/${batchId}/retry-failed`));

export interface SkillModelConfig {
  id: string;
  modelCode: string;
  modelName: string;
  modelId: string;
  thinking?: string | null;
  reasoningEffort?: string | null;
}

export const listSkillModelConfigs = () =>
  unwrap<SkillModelConfig[]>(apiClient.get('/exam/skill/gen/model-configs'));

// ------------------------------------------------------------------
// 草稿
// ------------------------------------------------------------------

export interface SkillDraftQuery {
  /** 不传为"全部批次"模式（工作台跨批次审核，按创建时间倒序） */
  batchId?: string;
  reviewStatus?: string;
  publishStatus?: number;
  nodeId?: string;
  page?: number;
  pageSize?: number;
}

export const listSkillDrafts = (params: SkillDraftQuery) =>
  unwrap<PageResponse<SkillDraft>>(apiClient.get('/exam/skill/draft/page', { params }));

export const editSkillDraft = (id: string, patch: Partial<SkillDraft>) =>
  unwrap<SkillDraft>(apiClient.put(`/exam/skill/draft/${id}`, patch));

export const reviewSkillDrafts = (draftIds: string[], approve: boolean) =>
  unwrap<number>(apiClient.post('/exam/skill/draft/review', { draftIds, approve }));

export const publishSkillDrafts = (batchId: string, taskMode: 'create' | 'append',
    taskId?: string) =>
  unwrap<{ published: number; skipped: number; taskId?: string | null;
    cumulativeTotal: number; cumulativeAiTotal: number }>(
    apiClient.post('/exam/skill/draft/publish', { batchId, taskMode, taskId }));

/** 入库预览：本次可入库数、累计进度 vs 等级下限、缺口（闸门 UI） */
export const getSkillPublishPreview = (batchId: string) =>
  unwrap<PublishPreview>(apiClient.get('/exam/skill/draft/publish-preview', { params: { batchId } }));

/**
 * 导出批次试题表：返回文件字节 + 服务端文件名（Content-Disposition filename*）。
 * 文件名规则由后端统一维护（工种(等级)_操作技能试题_批次{短号}.xlsx），前端不再自行拼装。
 */
export const exportSkillDrafts = async (
  batchId: string,
): Promise<{ blob: Blob; filename: string }> => {
  const resp = await apiClient.get('/exam/skill/draft/export', {
    params: { batchId },
    responseType: 'blob',
    timeout: 120000,
  });
  let filename = '';
  const disposition = String(resp.headers?.['content-disposition'] ?? '');
  const m = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  if (m) {
    try {
      filename = decodeURIComponent(m[1].trim());
    } catch {
      filename = m[1].trim();
    }
  }
  return { blob: resp.data as Blob, filename };
};
