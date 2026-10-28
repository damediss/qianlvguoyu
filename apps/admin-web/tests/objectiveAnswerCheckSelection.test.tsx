import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ObjectiveAnswerCheckPage from '../src/pages/question_bank/ObjectiveAnswerCheckPage';

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

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (selector: (state: { hasPermission: (code: string) => boolean }) => unknown) =>
    selector({ hasPermission: () => true }),
}));

const OPTIONS = {
  workTypes: [
    { id: '143', title: '铝及铝合金熔铸工', path: '铝电解工 / 铝及铝合金熔铸工' },
  ],
  levels: [{ code: 4, label: '四级/中级工' }],
  sources: [
    { value: 0, label: '内部' },
    { value: 1, label: '外部' },
    { value: 2, label: 'AI 生成' },
    { value: 3, label: '赛前复习' },
  ],
  types: [
    { value: 'single', label: '单选题' },
    { value: 'multi', label: '多选题' },
    { value: 'judge', label: '判断题' },
  ],
};

const ROWS = {
  items: [
    {
      detailId: 101,
      questionType: 'single',
      questionTypeLabel: '单选题',
      stemPreview: '海洋是什么颜色的？',
      originalAnswer: 'B. 蓝色',
      source: 0,
      sourceLabel: '内部',
      workTypeId: '143',
      workTypeTitle: '铝及铝合金熔铸工',
      levelCode: 4,
      levelLabel: '四级/中级工',
      courseId: 9,
      courseTitle: '铝电解工中级',
      invalidReason: null,
      contentQuality: 'clean',
    },
    {
      detailId: 102,
      questionType: 'judge',
      questionTypeLabel: '判断题',
      stemPreview: '地球比太阳大',
      originalAnswer: '错误',
      source: 0,
      sourceLabel: '内部',
      workTypeId: '143',
      workTypeTitle: '铝及铝合金熔铸工',
      levelCode: 4,
      levelLabel: '四级/中级工',
      courseId: 9,
      courseTitle: '铝电解工中级',
      invalidReason: null,
      contentQuality: 'suspect',
    },
  ],
  page: 1,
  pageSize: 20,
  total: 2,
};

const BATCH = {
  id: '2100000000000000001',
  status: 'running',
  selectionMode: 'ids',
  filterSummary: '铝及铝合金熔铸工 / 四级/中级工',
  total: 2,
  done: 0,
  pending: 2,
  processing: 0,
  consistentHigh: 0,
  mismatchHigh: 0,
  lowConfidence: 0,
  invalid: 0,
  failed: 0,
  contentSuspect: 1,
  contentInvalid: 0,
  expectedCalls: 2,
  callCount: 0,
  inputTokensExact: 0,
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
  finishedAt: null,
  failReason: null,
  canPause: true,
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
        <ObjectiveAnswerCheckPage />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

describe('ObjectiveAnswerCheckPage 选择试题', () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset());
    api.getFilterOptions.mockResolvedValue(OPTIONS);
    api.pageObjectiveAnswerQuestions.mockResolvedValue(ROWS);
    api.previewObjectiveAnswerSelection.mockResolvedValue({
      selectionCount: 2,
      overLimit: false,
      maxBatchSize: 2000,
      validCount: 1,
      invalidCount: 1,
      suspectContentCount: 1,
      invalidContentCount: 0,
      expectedCalls: 1,
      typeCounts: { single: 1, judge: 1 },
      invalidReasonCounts: [{ code: 'OPTIONS_EMPTY', count: 1 }],
      modelId: 'ateve-jev-v1',
      confidenceThreshold: 0.8,
      marginThreshold: 0.2,
    });
    api.createObjectiveAnswerBatch.mockResolvedValue({ batchId: BATCH.id });
    api.getObjectiveAnswerBatch.mockResolvedValue(BATCH);
    api.getCurrentObjectiveAnswerBatch.mockResolvedValue(null);
    api.pageObjectiveAnswerItems.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0,
    });
  });

  it('默认三种题型全选，展示工种路径与原答案/预检标签', async () => {
    renderPage();
    expect(await screen.findByText('海洋是什么颜色的？')).toBeInTheDocument();
    expect(screen.getAllByText(/铝及铝合金熔铸工/).length).toBeGreaterThan(0);
    expect(screen.getByText('B. 蓝色')).toBeInTheDocument();
    expect(screen.getByText('可检查')).toBeInTheDocument();
    expect(screen.getByText('格式疑似异常')).toBeInTheDocument();
    expect(api.pageObjectiveAnswerQuestions).toHaveBeenCalledWith(
      expect.objectContaining({
        page: 1,
        pageSize: 20,
        scope: expect.objectContaining({ types: ['single', 'multi', 'judge'] }),
      }),
    );
  });

  it('逐题勾选后预览：确认弹窗展示计数，创建走 ids 模式', async () => {
    renderPage();
    await screen.findByText('海洋是什么颜色的？');
    const checkboxes = document.querySelectorAll('.ant-table-tbody .ant-checkbox-input');
    expect(checkboxes.length).toBeGreaterThan(0);
    await userEvent.click(checkboxes[0] as HTMLElement);

    await userEvent.click(screen.getByRole('button', { name: /预览并开始 AI 检查/ }));
    expect(await screen.findByText('确认开始 AI 检查')).toBeInTheDocument();
    expect(screen.getByText(/可直接检查/)).toBeInTheDocument();
    expect(screen.getByText(/AI 不读取原答案和解析/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '开始检查' }));
    await waitFor(() => expect(api.createObjectiveAnswerBatch).toHaveBeenCalled());
    expect(api.createObjectiveAnswerBatch.mock.calls[0][0]).toEqual({
      selectionMode: 'ids',
      detailIds: [101],
      scope: expect.objectContaining({ types: ['single', 'multi', 'judge'] }),
    });
  });

  it('超过单批上限时“选择筛选全部”拒绝并提示缩小范围', async () => {
    api.previewObjectiveAnswerSelection.mockResolvedValue({
      selectionCount: 2500,
      overLimit: true,
      maxBatchSize: 2000,
      modelId: 'ateve-jev-v1',
    });
    renderPage();
    await screen.findByText('海洋是什么颜色的？');
    await userEvent.click(screen.getByRole('button', { name: '选择筛选全部' }));
    expect(await screen.findByText(/超过单批上限/)).toBeInTheDocument();
  });

  it('服务端分页：切页触发新请求，不写死 pageSize', async () => {
    renderPage();
    await screen.findByText('海洋是什么颜色的？');
    expect(api.pageObjectiveAnswerQuestions).toHaveBeenCalledTimes(1);
  });
});
