import { apiClient, unwrap } from '../../api/client';
import type { PageResponse } from '../../types/api';
import type {
  ObjectiveAnswerBatch,
  ObjectiveAnswerFilterOptions,
  ObjectiveAnswerItem,
  ObjectiveAnswerItemDetail,
  ObjectiveAnswerItemFilter,
  ObjectiveAnswerPreview,
  ObjectiveAnswerQuestionPage,
  ObjectiveAnswerReviewStatus,
  ObjectiveAnswerScope,
  ObjectiveAnswerSelectionReq,
} from './objectiveAnswerCheckTypes';

const BASE = '/exam/objective-answer-check';

export function getFilterOptions(workTypeId?: number | null) {
  return unwrap<ObjectiveAnswerFilterOptions>(
    apiClient.get(`${BASE}/filter-options`, { params: { workTypeId: workTypeId ?? undefined } }),
  );
}

export function pageObjectiveAnswerQuestions(payload: {
  scope?: ObjectiveAnswerScope;
  page: number;
  pageSize: number;
}) {
  return unwrap<ObjectiveAnswerQuestionPage>(
    apiClient.post(`${BASE}/questions/page`, payload),
  );
}

export function previewObjectiveAnswerSelection(payload: ObjectiveAnswerSelectionReq) {
  return unwrap<ObjectiveAnswerPreview>(apiClient.post(`${BASE}/selection/preview`, payload));
}

export function createObjectiveAnswerBatch(payload: ObjectiveAnswerSelectionReq) {
  return unwrap<{ batchId: string }>(apiClient.post(`${BASE}/batches`, payload));
}

export function getCurrentObjectiveAnswerBatch() {
  return unwrap<ObjectiveAnswerBatch | null>(apiClient.get(`${BASE}/batches/current`));
}

export function pageObjectiveAnswerBatches(page: number, pageSize: number) {
  return unwrap<PageResponse<ObjectiveAnswerBatch>>(
    apiClient.get(`${BASE}/batches`, { params: { page, pageSize } }),
  );
}

export function getObjectiveAnswerBatch(batchId: string) {
  return unwrap<ObjectiveAnswerBatch>(apiClient.get(`${BASE}/batches/${batchId}`));
}

export function pauseObjectiveAnswerBatch(batchId: string) {
  return unwrap<void>(apiClient.post(`${BASE}/batches/${batchId}/pause`));
}

export function resumeObjectiveAnswerBatch(batchId: string) {
  return unwrap<void>(apiClient.post(`${BASE}/batches/${batchId}/resume`));
}

export function retryFailedObjectiveAnswerItems(batchId: string) {
  return unwrap<{ resetCount: number }>(
    apiClient.post(`${BASE}/batches/${batchId}/retry-failed`),
  );
}

function filterParams(filter?: ObjectiveAnswerItemFilter) {
  if (!filter) return {};
  return {
    resultCode: filter.resultCode || undefined,
    contentQuality: filter.contentQuality || undefined,
    needsReview: filter.needsReview === undefined ? undefined : filter.needsReview,
    reviewStatus: filter.reviewStatus || undefined,
    questionType: filter.questionType || undefined,
    keyword: filter.keyword || undefined,
  };
}

export function pageObjectiveAnswerItems(
  batchId: string,
  filter: ObjectiveAnswerItemFilter | undefined,
  page: number,
  pageSize: number,
) {
  return unwrap<PageResponse<ObjectiveAnswerItem>>(
    apiClient.get(`${BASE}/batches/${batchId}/items`, {
      params: { ...filterParams(filter), page, pageSize },
    }),
  );
}

export function getObjectiveAnswerItem(batchId: string, itemId: string) {
  return unwrap<ObjectiveAnswerItemDetail>(
    apiClient.get(`${BASE}/batches/${batchId}/items/${itemId}`),
  );
}

export function reviewObjectiveAnswerItem(
  batchId: string,
  itemId: string,
  payload: { reviewStatus: ObjectiveAnswerReviewStatus; note?: string; expectedVersion: number },
) {
  return unwrap<void>(
    apiClient.post(`${BASE}/batches/${batchId}/items/${itemId}/review`, payload),
  );
}

export function exportObjectiveAnswerBatch(
  batchId: string,
  filter?: ObjectiveAnswerItemFilter,
) {
  return apiClient.get(`${BASE}/batches/${batchId}/export`, {
    params: filterParams(filter),
    responseType: 'blob',
  });
}
