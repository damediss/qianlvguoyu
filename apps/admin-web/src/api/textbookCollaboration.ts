/**
 * 教材编写协同（二期）接口封装 —— 《职业标准驱动教材生成·二期编写协同实施设计》v1.1 §6
 * C1~C6 候选人/分配/完成、T1~T4 我的任务、W1~W9 工作台/正文、A1~A3 图片、E1~E4 docx 导出。
 * 写接口统一 silentError:true —— 409 冲突等由页面按场景展示，避免全局 toast 与弹窗重复（§6 通用约定）。
 */
import type { AxiosResponse } from 'axios';
import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type { TextbookProjectVO } from '../types/textbook';
import type {
  ChapterAssignmentItem,
  ChapterWorkspaceVO,
  CollaboratorQuery,
  CollaboratorVO,
  MyTaskQuery,
  ReviewSectionContentReq,
  ReviewTaskSummaryVO,
  RestoreSectionVersionReq,
  SaveSectionContentReq,
  SectionAssetUploadVO,
  SectionContentVO,
  SectionVersionDetailVO,
  SectionVersionVO,
  TextbookAssignmentVO,
  TextbookBookExportMode,
  TextbookCollaborationVO,
  TextbookReviewRecordVO,
  TextbookTaskVO,
  WritingTaskSummaryVO,
} from '../types/textbookCollaboration';

const BASE = '/textbook';

// ------------------------------------------------------------------
// C1~C6 候选人 / 工程编写管理
// ------------------------------------------------------------------

/** C1 候选人分页（服务端按权限过滤 enabled 用户，不返回手机号） */
export function pageCollaborators(params: CollaboratorQuery): Promise<PageResponse<CollaboratorVO>> {
  return unwrap<PageResponse<CollaboratorVO>>(apiClient.get(`${BASE}/collaborators`, { params }));
}

/** C2 工程、合计校验、全部章分配与节进度一次返回 */
export function getCollaboration(projectId: string): Promise<TextbookCollaborationVO> {
  return unwrap<TextbookCollaborationVO>(apiClient.get(`${BASE}/projects/${projectId}/collaboration`));
}

/** C3 批量创建/改派（单事务，全部成功才提交），返回全部受影响 AssignmentVO */
export function saveAssignments(
  projectId: string,
  items: ChapterAssignmentItem[],
): Promise<TextbookAssignmentVO[]> {
  return unwrap<TextbookAssignmentVO[]>(
    apiClient.put(`${BASE}/projects/${projectId}/assignments`, { items }, { silentError: true }),
  );
}

/** C4 取消当前分配（active=0），返回取消后的最新 AssignmentVO */
export function cancelAssignment(chapterId: string, assignmentVersion: number): Promise<TextbookAssignmentVO> {
  return unwrap<TextbookAssignmentVO>(
    apiClient.post(
      `${BASE}/chapters/${chapterId}/assignment/cancel`,
      { assignmentVersion },
      { silentError: true },
    ),
  );
}

/** C5 校验完成条件 → done；校验失败 409 data.completion 与 C2 同构 */
export function completeProject(projectId: string): Promise<TextbookProjectVO> {
  return unwrap<TextbookProjectVO>(
    apiClient.post(`${BASE}/projects/${projectId}/complete`, undefined, { silentError: true }),
  );
}

/** C6 done → writing，不清正文状态 */
export function reopenProject(projectId: string): Promise<TextbookProjectVO> {
  return unwrap<TextbookProjectVO>(
    apiClient.post(`${BASE}/projects/${projectId}/reopen`, undefined, { silentError: true }),
  );
}

// ------------------------------------------------------------------
// T1~T4 我的任务
// ------------------------------------------------------------------

export function pageMyWritingTasks(params: MyTaskQuery): Promise<PageResponse<TextbookTaskVO>> {
  return unwrap<PageResponse<TextbookTaskVO>>(apiClient.get(`${BASE}/my-writing-tasks`, { params }));
}

export function pageMyReviewTasks(params: MyTaskQuery): Promise<PageResponse<TextbookTaskVO>> {
  return unwrap<PageResponse<TextbookTaskVO>>(apiClient.get(`${BASE}/my-review-tasks`, { params }));
}

/** T3 编写统计卡（只按当前用户 + active 统计，不受筛选/分页影响） */
export function getWritingTaskSummary(): Promise<WritingTaskSummaryVO> {
  return unwrap<WritingTaskSummaryVO>(apiClient.get(`${BASE}/my-writing-tasks/summary`));
}

/** T4 审定统计卡 */
export function getReviewTaskSummary(): Promise<ReviewTaskSummaryVO> {
  return unwrap<ReviewTaskSummaryVO>(apiClient.get(`${BASE}/my-review-tasks/summary`));
}

// ------------------------------------------------------------------
// W1~W9 章工作台与正文
// ------------------------------------------------------------------

/** W1 章信息、分配、节列表、进度、当前用户 readOnly 原因 */
export function getChapterWorkspace(chapterId: string): Promise<ChapterWorkspaceVO> {
  return unwrap<ChapterWorkspaceVO>(apiClient.get(`${BASE}/chapters/${chapterId}/workspace`));
}

/** W2 当前正文（空正文契约：授权通过但无记录时返回 contentExists=false，不是 404） */
export function getSectionContent(sectionId: string): Promise<SectionContentVO> {
  return unwrap<SectionContentVO>(apiClient.get(`${BASE}/sections/${sectionId}/content`));
}

/** W3 自动/手动保存，返回最新 W2 */
export function saveSectionContent(
  sectionId: string,
  req: SaveSectionContentReq,
): Promise<SectionContentVO> {
  return unwrap<SectionContentVO>(
    apiClient.put(`${BASE}/sections/${sectionId}/content`, req, { silentError: true }),
  );
}

/** W4 提交并生成 submit 快照，返回最新 W2 */
export function submitSectionContent(sectionId: string, baseVersion: number): Promise<SectionContentVO> {
  return unwrap<SectionContentVO>(
    apiClient.post(`${BASE}/sections/${sectionId}/submit`, { baseVersion }, { silentError: true }),
  );
}

/** W5 通过/打回/撤销通过，返回最新 W2 */
export function reviewSectionContent(
  sectionId: string,
  req: ReviewSectionContentReq,
): Promise<SectionContentVO> {
  return unwrap<SectionContentVO>(
    apiClient.post(`${BASE}/sections/${sectionId}/review`, req, { silentError: true }),
  );
}

/** W6 提交与审定时间线（createdAt/id 稳定倒序） */
export function pageSectionReviews(
  sectionId: string,
  params: { page: number; pageSize: number },
): Promise<PageResponse<TextbookReviewRecordVO>> {
  return unwrap<PageResponse<TextbookReviewRecordVO>>(
    apiClient.get(`${BASE}/sections/${sectionId}/reviews`, { params }),
  );
}


/** W7 快照列表（snapshotNo 倒序，不含大正文），分页加载 */
export function pageSectionVersions(
  sectionId: string,
  params: { page: number; pageSize: number },
): Promise<PageResponse<SectionVersionVO>> {
  return unwrap<PageResponse<SectionVersionVO>>(
    apiClient.get(`${BASE}/sections/${sectionId}/versions`, { params }),
  );
}

/** W8 单个快照完整内容 */
export function getSectionVersion(versionId: string): Promise<SectionVersionDetailVO> {
  return unwrap<SectionVersionDetailVO>(apiClient.get(`${BASE}/section-versions/${versionId}`));
}

/** W9 恢复历史版本（保持 draft/returned 原状态），返回最新 W2 */
export function restoreSectionVersion(
  sectionId: string,
  req: RestoreSectionVersionReq,
): Promise<SectionContentVO> {
  return unwrap<SectionContentVO>(
    apiClient.post(`${BASE}/sections/${sectionId}/restore`, req, { silentError: true }),
  );
}

// ------------------------------------------------------------------
// A1~A3 图片（仅教材受保护文件区，Markdown 中用 textbook-asset://{assetId}）
// ------------------------------------------------------------------

/** A1 上传节图片（jpeg/png/webp、≤10MB），返回 assetId/sourceUri；不要求先创建正文记录 */
export function uploadSectionAsset(sectionId: string, file: File): Promise<SectionAssetUploadVO> {
  const form = new FormData();
  form.append('file', file);
  return unwrap<SectionAssetUploadVO>(
    apiClient.post(`${BASE}/sections/${sectionId}/assets`, form, { silentError: true }),
  );
}

/** A2 节读取授权后 inline 下载（Cache-Control: private, no-store）；blob 在 AxiosResponse.data（坑 #10） */
export async function getSectionAssetContent(sectionId: string, assetId: string): Promise<Blob> {
  const response = await apiClient.get(`${BASE}/sections/${sectionId}/assets/${assetId}/content`, {
    responseType: 'blob',
    silentError: true,
  });
  return response.data as Blob;
}

/** A3 删除未进入任何快照的误传图片（快照引用禁止删除） */
export function deleteSectionAsset(
  sectionId: string,
  assetId: string,
  baseVersion: number,
): Promise<void> {
  return unwrap<void>(
    apiClient.delete(`${BASE}/sections/${sectionId}/assets/${assetId}`, {
      params: { baseVersion },
      silentError: true,
    }),
  );
}

// ------------------------------------------------------------------
// E1~E4 docx 导出（blob 下载，坑 #10：blob 数据在 .data，文件名取 Content-Disposition）
// ------------------------------------------------------------------

export function exportSectionDocx(sectionId: string): Promise<AxiosResponse> {
  return apiClient.get(`${BASE}/sections/${sectionId}/export-docx`, {
    responseType: 'blob',
    timeout: 120000,
  });
}

export function exportChapterDocx(chapterId: string): Promise<AxiosResponse> {
  return apiClient.get(`${BASE}/chapters/${chapterId}/export-docx`, {
    responseType: 'blob',
    timeout: 120000,
  });
}

export function exportBookDocx(projectId: string, mode: TextbookBookExportMode): Promise<AxiosResponse> {
  return apiClient.get(`${BASE}/projects/${projectId}/export-book`, {
    params: { mode },
    responseType: 'blob',
    timeout: 120000,
  });
}
