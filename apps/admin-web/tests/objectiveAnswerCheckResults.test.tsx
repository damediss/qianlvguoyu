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

const BATCH_ID = '2100000000000000001';
const ITEM_ID = '2100000000000000002';

const BATCH = {
  id: BATCH_ID,
  status: 'done',
  selectionMode: 'filter',
  filterSummary: '铝及铝合金熔铸工 / 四级/中级工',
  total: 12,
  done: 12,
  pending: 0,
  processing: 0,
  consistentHigh: 6,
  mismatchHigh: 2,
  lowConfidence: 2,
  invalid: 1,
  failed: 1,
  contentSuspect: 3,
  contentInvalid: 1,
  expectedCalls: 14,
  callCount: 14,
  inputTokensExact: 1200,
  inputTokensEstimated: 100,
  usageEstimatedCalls: 1,
  outputTokens: 0,
  modelId: 'ateve-jev-v1',
  promptVersion: 'objective-answer-v1',
  confidenceThreshold: 0.8,
  marginThreshold: 0.2,
  createdBy: '1',
  createdByName: '管理员',
  createdAt: '2026-10-28T10:00:00',
  startedAt: '2026-10-28T10:00:01',
  finishedAt: '2026-10-28T10:05:00',
  failReason: null,
  canPause: false,
  canResume: false,
  canRetryFailed: true,
};

const ITEM = {
  id: ITEM_ID,
  batchId: BATCH_ID,
  detailId: 12345,
  questionType: 'single',
  questionTypeLabel: '单选题',
  stemPreview: '海洋是什么颜色的？',
  originalAnswer: 'B. 蓝色',
  aiAnswer: 'C. 黑色',
  resultCode: 'mismatch_high',
  contentQuality: 'suspect',
  contentWarnings: [
    {
      ruleCode: 'EXCEL_SCIENTIFIC_NOTATION_SUSPECT',
      severity: 'suspect',
      location: 'option',
      optionId: 'opt_2',
      matchedText: '1.2E+3',
      suggestedText: '1200',
      message: '选项疑似被 Excel 转成了科学计数法，请核对原始数值',
    },
  ],
  needsReview: true,
  consistent: false,
  referenceConfidence: 0.06,
  aiConfidence: 0.91,
  minMargin: 0.78,
  decisionWarning: null,
  retryable: false,
  attempts: 1,
  version: 3,
  reviewStatus: 'unreviewed',
  reviewNote: null,
  checkedAt: '2026-10-28T10:01:00',
  updatedAt: '2026-10-28T10:01:00',
};

const DETAIL = {
  item: ITEM,
  sourceLabel: '内部',
  workTypeTitle: '铝及铝合金熔铸工',
  courseTitle: '铝电解工中级',
  levelCode: 4,
  knowledgePath: '基本要求/安全知识',
  contentSnapshot: '<p>海洋是什么颜色的？</p>',
  stemText: '海洋是什么颜色的？',
  resolveSnapshot: '人工解析不发送给 AI',
  options: [
    {
      optionId: 'opt_1',
      label: 'B',
      text: '蓝色',
      originalCorrect: true,
      aiCorrect: false,
      choice: null,
      confidence: 0.91,
      referenceProbability: 0.06,
      top1Probability: 0.88,
      top2Probability: 0.06,
      margin: 0.82,
    },
    {
      optionId: 'opt_2',
      label: 'C',
      text: '1.2E+3',
      originalCorrect: false,
      aiCorrect: true,
      choice: 'opt_2',
      confidence: 0.91,
      referenceProbability: 0.06,
      top1Probability: 0.88,
      top2Probability: 0.06,
      margin: 0.82,
    },
  ],
  calls: [
    {
      callId: 'call-1',
      chunkIndex: 1,
      chunkCount: 1,
      status: 'success',
      requestedModel: 'ateve-jev-v1',
      responseModel: 'ateve-jev-v1',
      confidenceDefinition: 'maximum candidate probability',
      inputTokens: 100,
      outputTokens: 0,
      usageEstimated: false,
      durationMs: 900,
      errorCode: null,
      errorMessage: null,
    },
  ],
  sourceChanged: false,
  modelId: 'ateve-jev-v1',
  promptVersion: 'objective-answer-v1',
  requestedModel: 'ateve-jev-v1',
  responseModel: 'ateve-jev-v1',
  confidenceDefinition: 'maximum candidate probability',
  failReason: null,
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

describe('ObjectiveAnswerCheckResults 检查结果', () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset());
    api.getObjectiveAnswerBatch.mockResolvedValue(BATCH);
    api.pageObjectiveAnswerItems.mockResolvedValue({
      items: [ITEM],
      page: 1,
      pageSize: 20,
      total: 1,
    });
    api.getObjectiveAnswerItem.mockResolvedValue(DETAIL);
    api.reviewObjectiveAnswerItem.mockResolvedValue(undefined);
    api.retryFailedObjectiveAnswerItems.mockResolvedValue({ resetCount: 1 });
    api.pauseObjectiveAnswerBatch.mockResolvedValue(undefined);
    api.resumeObjectiveAnswerBatch.mockResolvedValue(undefined);
  });

  it('默认按“需人工复核”查询，统计卡与内容格式标签同时可见', async () => {
    renderPage();
    expect(await screen.findByText('海洋是什么颜色的？')).toBeInTheDocument();
    expect(api.pageObjectiveAnswerItems).toHaveBeenCalledWith(
      BATCH_ID,
      expect.objectContaining({ needsReview: true }),
      1,
      20,
    );
    expect(screen.getAllByText('高置信不一致').length).toBeGreaterThan(0);
    expect(screen.getByText('内容格式待核')).toBeInTheDocument();
    expect(screen.getAllByText('格式疑似异常').length).toBeGreaterThan(0);
    expect(screen.getAllByText('高置信不一致').length).toBeGreaterThan(0);
    expect(screen.getByText('需人工复核（默认）')).toBeInTheDocument();
  });

  it('点击统计卡切换精确过滤，批次 ID 字符串原样传递', async () => {
    renderPage();
    await screen.findByText('海洋是什么颜色的？');
    const cards = screen.getAllByText('低置信复核');
    await userEvent.click(cards[cards.length - 1]);
    await waitFor(() =>
      expect(api.pageObjectiveAnswerItems).toHaveBeenLastCalledWith(
        BATCH_ID,
        expect.objectContaining({ resultCode: 'low_confidence' }),
        1,
        20,
      ),
    );
    expect(api.getObjectiveAnswerBatch).toHaveBeenCalledWith(BATCH_ID);
  });

  it('详情展示逐选项概率与格式警告，且没有“采纳 AI 答案”入口', async () => {
    renderPage();
    await screen.findByText('海洋是什么颜色的？');
    await userEvent.click(screen.getAllByRole('button', { name: /详\s*情/ })[0]);
    expect(await screen.findByText('原解析（仅供人工查看，未发送给 AI）')).toBeInTheDocument();
    expect(screen.getByText('人工解析不发送给 AI')).toBeInTheDocument();
    expect(screen.getByText(/命中文本/)).toBeInTheDocument();
    expect(screen.getByText('1200')).toBeInTheDocument();
    expect(screen.queryByText(/采纳 AI 答案/)).not.toBeInTheDocument();
    expect(screen.queryByText(/一键写回/)).not.toBeInTheDocument();
  });

  it('复核提交携带 expectedVersion；只写复核字段', async () => {
    renderPage();
    await screen.findByText('海洋是什么颜色的？');
    const reviewButtons = screen.getAllByRole('button', { name: /复\s*核/ });
    await userEvent.click(reviewButtons[0]);
    expect(await screen.findByText('这里只记录处理意见，不会修改题库；如需改题请在题库编辑页操作。')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('需要修改'));
    await userEvent.click(screen.getByRole('button', { name: '保存意见' }));
    await waitFor(() =>
      expect(api.reviewObjectiveAnswerItem).toHaveBeenCalledWith(BATCH_ID, ITEM_ID, {
        reviewStatus: 'needs_edit',
        note: '',
        expectedVersion: 3,
      }),
    );
  });

  it('重试失败项前二次确认，成功提示复位数量', async () => {
    renderPage();
    await screen.findByText('海洋是什么颜色的？');
    await userEvent.click(screen.getByRole('button', { name: /重试失败项/ }));
    expect(await screen.findByText('将重新检查失败试题，并清除这些行的旧复核标记（内容格式快照保留）。继续？')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '重 试' }));
    await waitFor(() => expect(api.retryFailedObjectiveAnswerItems).toHaveBeenCalledWith(BATCH_ID));
    expect(await screen.findByText(/已复位 1 道失败试题/)).toBeInTheDocument();
  });
});
