import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ObjectiveAnswerCheckResults from '../src/pages/question_bank/ObjectiveAnswerCheckResults';

const api = vi.hoisted(() => ({
  getFilterOptions: vi.fn(),
  pageObjectiveAnswerQuestions: vi.fn(),
  previewObjectiveAnswerSelection: vi.fn(),
  createObjectiveAnswerBatch: vi.fn(),
  getCurrentObjectiveAnswerBatch: vi.fn(),
  pageObjectiveAnswerBatches: vi.fn(),
  getObjectiveAnswerBatch: vi.fn(),
  pauseObjectiveAnswerBatch: vi.fn(),
  resumeObjectiveAnswerBatch: vi.fn(),
  retryFailedObjectiveAnswerItems: vi.fn(),
  pageObjectiveAnswerItems: vi.fn(),
  getObjectiveAnswerItem: vi.fn(),
  reviewObjectiveAnswerItem: vi.fn(),
  exportObjectiveAnswerBatch: vi.fn(),
}));

vi.mock('../src/pages/question_bank/objectiveAnswerCheckApi', () => api);

const BATCH_ID = '2100000000000000009';
const BATCH = {
  id: BATCH_ID,
  status: 'done',
  selectionMode: 'filter',
  filterSummary: '铝电解工 / 五级',
  total: 1,
  done: 1,
  pending: 0,
  processing: 0,
  consistentHigh: 0,
  mismatchHigh: 1,
  lowConfidence: 0,
  invalid: 0,
  failed: 0,
  contentSuspect: 0,
  contentInvalid: 0,
  expectedCalls: 1,
  callCount: 1,
  inputTokensExact: 10,
  inputTokensEstimated: 0,
  usageEstimatedCalls: 0,
  outputTokens: 0,
  modelId: 'ateve-jev-v1',
  promptVersion: 'objective-answer-v1',
  confidenceThreshold: 0.8,
  marginThreshold: 0.2,
  createdBy: '1',
  createdByName: '管理员',
  createdAt: '2026-10-28T10:00:00',
  startedAt: null,
  finishedAt: '2026-10-28T10:05:00',
  failReason: null,
  canPause: false,
  canResume: false,
  canRetryFailed: false,
};

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={client}>
        <ObjectiveAnswerCheckResults batchId={BATCH_ID} onBatchChange={() => undefined} />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

describe('ObjectiveAnswerCheckExport 导出', () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset());
    api.getObjectiveAnswerBatch.mockResolvedValue(BATCH);
    api.pageObjectiveAnswerItems.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0,
    });
  });

  it('按当前过滤导出，取 Blob.data 与中文文件名', async () => {
    const blob = new Blob(['xlsx-bytes']);
    api.exportObjectiveAnswerBatch.mockResolvedValue({
      data: blob,
      headers: {
        'content-disposition':
          "attachment; filename=\"check.xlsx\"; filename*=UTF-8''%E6%A3%80%E6%9F%A5.xlsx",
      },
    });
    const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock');
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    renderPage();
    expect(await screen.findByText(/铝电解工 \/ 五级/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /导\s*出/ }));

    await waitFor(() =>
      expect(api.exportObjectiveAnswerBatch).toHaveBeenCalledWith(
        BATCH_ID,
        expect.objectContaining({ needsReview: true }),
      ),
    );
    expect(await screen.findByText('导出成功')).toBeInTheDocument();
    expect(createObjectURL).toHaveBeenCalledWith(blob);
    expect(click).toHaveBeenCalled();
    expect(revoke).toHaveBeenCalled();
  });

  it('导出失败时显示后端白话错误，不静默', async () => {
    api.exportObjectiveAnswerBatch.mockRejectedValue(new Error('批次不存在'));
    renderPage();
    expect(await screen.findByText(/铝电解工 \/ 五级/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /导\s*出/ }));
    expect(await screen.findByText('批次不存在')).toBeInTheDocument();
  });
});
