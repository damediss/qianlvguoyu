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
  /** Jev 通道分类行数（§8.5 统计） */
  jevClassified: number | null;
  /** 走 LLM 全流程兜底行数 */
  llmFallback: number | null;
  /** Jev 行平均置信度（信息项，不卡门禁） */
  jevAvgConfidence: number | null;
  status: string;
  failReason: string | null;
  /** 未分类行数（未进过模型；total - done - failed） */
  unclassified: number;
  /** 累计 LLM 用量（FIX-10/v1.7；进度接口返回，供"费用超预期就暂停"判断） */
  usage: FixBatchUsage | null;
}

/** 批次累计 LLM 用量：cachedTokens 为命中前缀缓存的输入 token */
export interface FixBatchUsage {
  calls: number;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  cachedTokens: number;
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

/** 范围提交（§13 步骤 8 小批次）：courseId 限定课程、limit 限行数（1~1000），均缺省 = 全量；暂停续跑须带相同参数 */
export const submitClassify = (scope?: { courseId?: number; limit?: number }) =>
  unwrap<FixBatch>(apiClient.post('/exam/knowledgefix/classify/submit', null, {
    params: scope,
  }));

export const resetClassify = () =>
  unwrap<number>(apiClient.post('/exam/knowledgefix/classify/reset'));

/** 人工暂停（FIX-10/v1.7）：停止派发新行，在飞行的行跑完后批次收尾为 paused；继续=再点提交 */
export const pauseClassify = () =>
  unwrap<number>(apiClient.post('/exam/knowledgefix/classify/pause'));

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
