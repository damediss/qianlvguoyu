/** 知识归属修正 API（方案 §7.3）：去重软删除 + LLM 分类 + 置信分层写回 */
import { apiClient, unwrap } from '../../api/client';

export interface DedupStats {
  mayTotalRows: number;
  /** 重复题组数（同课程口径：一组 = 同一课程内同一道题的 N 份副本） */
  dupGroups: number;
  /** 当前清单待执行删除行数（pending/approved） */
  deleteRows: number;
  /** 已执行软删行数（done，可回滚） */
  alreadyDeletedRows: number;
  /** 人工复核行（有答题日志引用，不自动删除） */
  manualReviewRows: number;
}

export interface FixBatch {
  id: string;
  batchNo: string;
  total: number;
  done: number;
  failed: number;
  applyCnt: number;
  keepCnt: number;
  flagCnt: number;
  selfConsistencyRate: number | null;
  status: string;
  failReason: string | null;
  /** 未分类行数（未进过模型；total - done - failed） */
  unclassified: number;
}

export interface FixItem {
  rowId: string;
  detailId: number;
  oldKnowledgeId: number;
  courseId: number;
  newKnowledgeId: number | null;
  confidence: number | null;
  basis: string | null;
  llmReason: string | null;
  status: string;
  stemPreview: string | null;
}

export interface ApplyResult {
  backupTable: string | null;
  approvedRows: number;
  affectedRows: number;
}

export const generateDedupList = () =>
  unwrap<DedupStats>(apiClient.post('/exam/knowledgefix/dedup/generate'));

export const dedupStats = () => unwrap<DedupStats>(apiClient.get('/exam/knowledgefix/dedup/stats'));

export const applyDedup = () =>
  unwrap<ApplyResult>(apiClient.post('/exam/knowledgefix/dedup/apply'));

export const rollbackDedup = (backupTable: string) =>
  unwrap<number>(apiClient.post('/exam/knowledgefix/dedup/rollback', null, {
    params: { backupTable },
  }));

export const submitClassify = () =>
  unwrap<FixBatch>(apiClient.post('/exam/knowledgefix/classify/submit'));

export const resetClassify = () =>
  unwrap<number>(apiClient.post('/exam/knowledgefix/classify/reset'));

export const classifyProgress = () =>
  unwrap<FixBatch | null>(apiClient.get('/exam/knowledgefix/classify/progress'));

export const fixList = (params: { status?: string; page: number; pageSize: number }) =>
  unwrap<{ records: FixItem[]; total: number }>(
    apiClient.get('/exam/knowledgefix/fix/list', { params }),
  );

export const exportFixList = async (): Promise<Blob> => {
  const resp = await apiClient.get('/exam/knowledgefix/fix/export', { responseType: 'blob' });
  return resp.data as Blob;
};

export const applyFix = () => unwrap<ApplyResult>(apiClient.post('/exam/knowledgefix/fix/apply'));

export const rollbackFix = (backupTable: string) =>
  unwrap<number>(apiClient.post('/exam/knowledgefix/fix/rollback', null, {
    params: { backupTable },
  }));

export const qualityReport = () =>
  unwrap<FixBatch | null>(apiClient.get('/exam/knowledgefix/report'));
