/** 知识归属修正 API（方案 §7.3）：去重软删除 + LLM 分类 + 置信分层写回 */
import { apiClient, unwrap } from '../../api/client';
export const generateDedupList = () => unwrap(apiClient.post('/exam/knowledgefix/dedup/generate'));
export const dedupStats = () => unwrap(apiClient.get('/exam/knowledgefix/dedup/stats'));
export const applyDedup = () => unwrap(apiClient.post('/exam/knowledgefix/dedup/apply'));
export const rollbackDedup = (backupTable) => unwrap(apiClient.post('/exam/knowledgefix/dedup/rollback', null, {
    params: { backupTable },
}));
/** 范围提交（§13 步骤 8 小批次）：courseId 限定课程、limit 限行数（1~1000），均缺省 = 全量；暂停续跑须带相同参数 */
export const submitClassify = (scope) => unwrap(apiClient.post('/exam/knowledgefix/classify/submit', null, {
    params: scope,
}));
export const resetClassify = () => unwrap(apiClient.post('/exam/knowledgefix/classify/reset'));
/** 人工暂停（FIX-10/v1.7）：停止派发新行，在飞行的行跑完后批次收尾为 paused；继续=再点提交 */
export const pauseClassify = () => unwrap(apiClient.post('/exam/knowledgefix/classify/pause'));
export const classifyProgress = () => unwrap(apiClient.get('/exam/knowledgefix/classify/progress'));
export const fixList = (params) => unwrap(apiClient.get('/exam/knowledgefix/fix/list', { params }));
export const exportFixList = async () => {
    const resp = await apiClient.get('/exam/knowledgefix/fix/export', { responseType: 'blob' });
    return resp.data;
};
export const applyFix = () => unwrap(apiClient.post('/exam/knowledgefix/fix/apply'));
/** 存疑行人工定夺（单行/批量）：apply=定夺为改挂（采纳新知识点，待写回落地）、keep=定夺为保留 */
export const adjudicateFix = (rowId, action) => unwrap(apiClient.post('/exam/knowledgefix/fix/adjudicate', { rowId, action }));
export const adjudicateFixBatch = (rowIds, action) => unwrap(apiClient.post('/exam/knowledgefix/fix/adjudicate/batch', { rowIds, action }));
export const rollbackFix = (backupTable) => unwrap(apiClient.post('/exam/knowledgefix/fix/rollback', null, {
    params: { backupTable },
}));
export const qualityReport = () => unwrap(apiClient.get('/exam/knowledgefix/report'));
