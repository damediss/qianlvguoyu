import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CompetitionMakeExamPlanPage } from '../src/pages/competition/CompetitionMakeExamPlanPage';

const api = vi.hoisted(() => ({
  pageEvents: vi.fn(),
  listWorkTypes: vi.fn(),
  listLevels: vi.fn(),
  pageBlueprints: vi.fn(),
  getBlueprintTree: vi.fn(),
  pageQuestions: vi.fn(),
  pagePlans: vi.fn(),
  getPlan: vi.fn(),
  precheck: vi.fn(),
  createPlan: vi.fn(),
  updatePlan: vi.fn(),
  deletePlan: vi.fn(),
  publishPlan: vi.fn(),
  listSnapshots: vi.fn(),
  previewSnapshot: vi.fn(),
}));

vi.mock('../src/api/competitionQuestion', () => ({
  createCompetitionMakeExamPlan: api.createPlan,
  deleteCompetitionMakeExamPlan: api.deletePlan,
  getCompetitionMakeExamPlan: api.getPlan,
  listCompetitionMakeExamSnapshots: api.listSnapshots,
  previewCompetitionMakeExamSnapshot: api.previewSnapshot,
  pageCompetitionMakeExamPlans: api.pagePlans,
  publishCompetitionMakeExamPlan: api.publishPlan,
  updateCompetitionMakeExamPlan: api.updatePlan,
  precheckCompetitionMakeExamPlan: api.precheck,
  pageCompetitionQuestions: api.pageQuestions,
}));

vi.mock('../src/api/competition', () => ({
  listCompetitionEventWorkTypeLevels: api.listLevels,
  listCompetitionEventWorkTypes: api.listWorkTypes,
  pageCompetitionEvents: api.pageEvents,
}));

vi.mock('../src/api/competitionQuestionBlueprint', () => ({
  getCompetitionQuestionBlueprintTree: api.getBlueprintTree,
  pageCompetitionQuestionBlueprints: api.pageBlueprints,
}));

const theoryBlueprint = {
  id: 'bp-theory-active', eventId: '100', eventWorkTypeId: '200', eventWorkTypeLevelId: '300',
  contentType: 'theory', versionNo: 2, title: '理论细目表 v2', sourceFileName: 'theory.xlsx',
  pointCount: 1, topicCount: 1, weightCheck: 1, status: 'active',
};
const practicalBlueprint = {
  ...theoryBlueprint, id: 'bp-practical-active', contentType: 'practical', title: '实操细目表 v2',
};

function treeFor(blueprint: typeof theoryBlueprint) {
  const point = {
    id: `${blueprint.id}-point`, parentId: `${blueprint.id}-topic`, nodeLevel: 4,
    code: 'A-1-1-1', fullCode: 'A-1-1-1', name: '末级考评点', weight: null,
    selectMode: null, sortOrder: 1, children: [],
  };
  const topic = {
    id: `${blueprint.id}-topic`, parentId: `${blueprint.id}-range2`, nodeLevel: 3,
    code: 'A-1-1', fullCode: 'A-1-1', name: '设备知识', weight: 1,
    selectMode: '必考', sortOrder: 1, children: [point],
  };
  const range2 = {
    id: `${blueprint.id}-range2`, parentId: `${blueprint.id}-range1`, nodeLevel: 2,
    code: 'A-1', fullCode: 'A-1', name: '一级考核范围', weight: null,
    selectMode: null, sortOrder: 1, children: [topic],
  };
  return {
    blueprint,
    tree: [{
      id: `${blueprint.id}-range1`, parentId: '0', nodeLevel: 1,
      code: 'A', fullCode: 'A', name: '考评范围', weight: null,
      selectMode: null, sortOrder: 1, children: [range2],
    }],
  };
}

const readyPrecheck = {
  ready: true,
  candidateCount: 10,
  topics: [{ blueprintNodeId: 'bp-theory-active-topic', path: '考评范围 / 一级考核范围 / 设备知识', weight: 1,
    sortOrder: 1, required: 10, available: 10, shortage: 0, allocations: [] }],
  details: [{ ruleIndex: 0, sortOrder: 1, questionType: 'single', required: 10, available: 10, shortage: 0 }],
  errors: [],
  theoryAllocations: [{ blueprintNodeId: 'bp-theory-active-topic', path: '考评范围 / 一级考核范围 / 设备知识', weight: 1,
    sortOrder: 1, questionCount: 10, available: 10, assigned: 10, shortage: 0, details: [] }],
  unboundQuestionCount: 0,
  warnings: [],
};

function mount(contentType: 'theory' | 'practical' = 'theory') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}><CompetitionMakeExamPlanPage contentType={contentType} /></QueryClientProvider>);
}

async function openCreate(user: ReturnType<typeof userEvent.setup>) {
  await waitFor(() => expect(screen.getByRole('button', { name: /新建计划/ })).toBeEnabled(), { timeout: 5000 });
  await user.click(screen.getByRole('button', { name: /新建计划/ }));
  await screen.findByRole('dialog');
}

async function submitModal(user: ReturnType<typeof userEvent.setup>) {
  const modal = document.querySelector('.ant-modal') as HTMLElement;
  await user.click(within(modal).getByRole('button', { name: /OK|确定/ }));
}

beforeEach(() => {
  window.history.pushState({}, '', '/competition/make-exam-plans?eventId=100');
  vi.clearAllMocks();
  api.pageEvents.mockResolvedValue({ items: [{ id: '100', name: '测试赛事', code: 'T2026' }], total: 1, page: 1, size: 200 });
  api.listWorkTypes.mockResolvedValue([{ id: '200', displayName: '铝加工', workTypeName: '铝加工' }]);
  api.listLevels.mockResolvedValue([{ id: '300', levelName: '中级', levelCode: '2' }]);
  api.pageBlueprints.mockImplementation((_eventId: string, params: { contentType: string }) => Promise.resolve({
    items: [params.contentType === 'practical' ? practicalBlueprint : theoryBlueprint], total: 1, page: 1, size: 20,
  }));
  api.getBlueprintTree.mockImplementation((_eventId: string, blueprintId: string) => Promise.resolve(
    treeFor(blueprintId === practicalBlueprint.id ? practicalBlueprint : theoryBlueprint),
  ));
  api.pageQuestions.mockResolvedValue({ items: [{
    id: 'question-practical', sortOrder: 1, stem: '实操题', contentType: 'practical', questionType: 'practical',
    practicalDetail: { durationMinutes: 60, totalScore: 100 },
  }], total: 1, page: 1, size: 500 });
  api.pagePlans.mockResolvedValue({ items: [], total: 0, page: 1, size: 100 });
  api.precheck.mockResolvedValue(readyPrecheck);
  api.createPlan.mockResolvedValue({ id: 'plan-1', status: 'draft' });
  api.updatePlan.mockResolvedValue({ id: 'plan-1', status: 'draft' });
  api.listSnapshots.mockResolvedValue([]);
  api.previewSnapshot.mockResolvedValue(null);
});

afterEach(() => cleanup());

describe('竞赛理论组卷自动分配页面', () => {
  it('does not render editable theory topic quota controls', async () => {
    const user = userEvent.setup();
    mount();
    await openCreate(user);

    expect(screen.getByText('理论抽题分布（根据细目表三级权重自动计算）')).toBeInTheDocument();
    expect(screen.queryByText('实操主题规则（必考主题默认选中，选考主题可按赛事要求选择）')).not.toBeInTheDocument();
    expect(screen.queryByText('主题分值')).not.toBeInTheDocument();
  });

  it('omits topicRules from theory precheck and create payloads', async () => {
    const user = userEvent.setup();
    mount();
    await openCreate(user);
    await user.type(screen.getByRole('textbox', { name: '计划标题' }), '理论自动组卷');
    await submitModal(user);

    await waitFor(() => expect(api.precheck).toHaveBeenCalled());
    await waitFor(() => expect(api.createPlan).toHaveBeenCalled());
    expect(api.precheck.mock.calls.at(-1)?.[1]).not.toHaveProperty('topicRules');
    expect(api.createPlan.mock.calls.at(-1)?.[1]).not.toHaveProperty('topicRules');
  });

  it('renders the read-only theory allocation returned by precheck', async () => {
    const user = userEvent.setup();
    mount();
    await openCreate(user);
    await user.type(screen.getByRole('textbox', { name: '计划标题' }), '理论预检');
    await user.click(screen.getByRole('button', { name: '预检并查看自动分配' }));

    await waitFor(() => expect(api.precheck).toHaveBeenCalled());
    expect(screen.getByText('考评范围 / 一级考核范围 / 设备知识')).toBeInTheDocument();
    expect(screen.getByText('理论抽题分布（根据细目表三级权重自动计算）')).toBeInTheDocument();
  });

  it('blocks saving when theory precheck reports an exact shortage', async () => {
    api.precheck.mockResolvedValue({
      ...readyPrecheck,
      ready: false,
      errors: ['考评范围 / 一级考核范围 / 设备知识 / 判断题 / 难度2-3：需要3题，可用1题，缺少2题'],
    });
    const user = userEvent.setup();
    mount();
    await openCreate(user);
    await user.type(screen.getByRole('textbox', { name: '计划标题' }), '缺口计划');
    await submitModal(user);

    await waitFor(() => expect(api.precheck).toHaveBeenCalled());
    expect((await screen.findAllByText(/设备知识 \/ 判断题 \/ 难度2-3：需要3题，可用1题，缺少2题/)).length).toBeGreaterThan(0);
    expect(api.createPlan).not.toHaveBeenCalled();
  });

  it('renders practical structure preview and omits legacy topicRules from payloads', async () => {
    api.precheck.mockResolvedValue({
      ...readyPrecheck,
      practicalQuestionCount: 2,
      practicalReferenceDurationMinutes: 80,
      practicalAllocations: [
        {
          selectionGroup: 'REQUIRED:bp-practical-active-range2', selectionMode: 'REQUIRED',
          blueprintNodeId: 'bp-practical-active-topic', path: '考评范围 / 一级考核范围 / 设备知识',
          scoreWeight: 0.6, targetScore: 60, candidateQuestionCount: 4,
          candidateNodes: [{ blueprintNodeId: 'bp-practical-active-topic', path: '设备知识', scoreWeight: 1,
            assessmentPointIds: ['bp-practical-active-point'], candidateQuestionCount: 4,
            candidateQuestionIds: ['question-practical'], selected: true }],
          selectedQuestionId: 'question-practical', selectedAssessmentPointId: 'bp-practical-active-point',
          sourceTotalScore: 100, scoreScale: 0.6, referenceDurationMinutes: 40,
        },
        {
          selectionGroup: 'CHOOSE_ONE:bp-practical-active-range2', selectionMode: 'CHOOSE_ONE',
          blueprintNodeId: 'bp-practical-active-topic-2', path: '考评范围 / 一级考核范围 / 安全操作',
          scoreWeight: 0.4, targetScore: 40, candidateQuestionCount: 3,
          candidateNodes: [{ blueprintNodeId: 'bp-practical-active-topic-2', path: '安全操作', scoreWeight: 1,
            assessmentPointIds: ['bp-practical-active-point-2'], candidateQuestionCount: 3,
            candidateQuestionIds: ['question-practical-2'], selected: true }],
          selectedQuestionId: 'question-practical-2', selectedAssessmentPointId: 'bp-practical-active-point-2',
          sourceTotalScore: 100, scoreScale: 0.4, referenceDurationMinutes: 40,
        },
      ],
    });
    const user = userEvent.setup();
    mount('practical');
    await openCreate(user);

    expect(screen.getByText('实操自动组卷结构预览')).toBeInTheDocument();
    expect(screen.getAllByText(/一级必考、二级任选一项/)).toHaveLength(2);
    expect(screen.queryByText('实操主题规则（必考主题默认选中，选考主题可按赛事要求选择）')).not.toBeInTheDocument();
    expect(screen.queryByText('主题分值')).not.toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: '计划标题' }), '实操自动组卷');
    await submitModal(user);

    await waitFor(() => expect(api.precheck).toHaveBeenCalled());
    await waitFor(() => expect(api.createPlan).toHaveBeenCalled());
    expect(api.precheck.mock.calls.at(-1)?.[1]).toEqual(expect.objectContaining({
      paperCount: 1, details: [],
    }));
    expect(api.precheck.mock.calls.at(-1)?.[1]).not.toHaveProperty('topicRules');
    expect(api.createPlan.mock.calls.at(-1)?.[1]).toEqual(expect.objectContaining({
      paperCount: 1, details: [],
    }));
    expect(api.createPlan.mock.calls.at(-1)?.[1]).not.toHaveProperty('topicRules');
  });

  it('loads the original blueprint when editing a historical draft', async () => {
    api.pagePlans.mockResolvedValue({ items: [{
      id: 'plan-old', eventId: '100', eventWorkTypeId: '200', eventWorkTypeLevelId: '300',
      blueprintId: 'bp-theory-old', contentType: 'theory', title: '历史草稿', durationMinutes: 90,
      totalScore: 100, passScore: 60, paperCount: 1, planVersion: 1, status: 'draft', details: [], topicRules: [],
    }], total: 1, page: 1, size: 100 });
    api.getPlan.mockResolvedValue({
      id: 'plan-old', eventId: '100', eventWorkTypeId: '200', eventWorkTypeLevelId: '300',
      blueprintId: 'bp-theory-old', contentType: 'theory', title: '历史草稿', durationMinutes: 90,
      totalScore: 100, passScore: 60, paperCount: 1, planVersion: 1, status: 'draft', details: [
        { questionType: 'single', questionCount: 1, point: 100, sortOrder: 1 },
      ], topicRules: [],
    });
    mount();
    await userEvent.setup().click(await screen.findByRole('button', { name: /编辑/ }));

    await waitFor(() => expect(api.getBlueprintTree).toHaveBeenCalledWith('100', 'bp-theory-old'));
  });

  it('renders a practical snapshot with the supplied practical-paper structure', async () => {
    const practicalSnapshot = {
      id: 'snapshot-practical-1', planId: 'plan-practical', eventId: '100', eventWorkTypeId: '200', eventWorkTypeLevelId: '300',
      contentType: 'practical', paperCode: 'P', versionNo: 1,
      title: '铝电解槽操作工二级技能现场试卷考核试卷', durationMinutes: 60, questionCount: 1,
      totalScore: 40, passScore: 24, status: 'draft',
      questions: [{
        id: 'snapshot-question-1', sourceQuestionId: 'question-practical', sortOrder: 1,
        questionType: 'practical', stem: '加铜盘存铝操作', score: 40,
        operationContent: null, studentPrepare: null, equipmentPrepare: null,
        practicalDetailJson: JSON.stringify({
          durationMinutes: 20,
          assessmentRequirementsHtml: '<p>出铝前对原铝液中铜含量取样并完成化验。</p>',
          negativeItemsHtml: '<ol><li>考生没有按规定穿戴劳保用品。</li><li>操作过程中出现严重违章操作。</li><li>造成人身伤害或设备损坏。</li></ol>',
        }),
      }],
    };
    api.pagePlans.mockResolvedValue({ items: [{
      id: 'plan-practical', eventId: '100', eventWorkTypeId: '200', eventWorkTypeLevelId: '300',
      blueprintId: 'bp-practical-active', contentType: 'practical', title: practicalSnapshot.title,
      durationMinutes: 60, totalScore: 40, passScore: 24, paperCount: 1, planVersion: 1, status: 'draft', details: [], topicRules: [],
    }], total: 1, page: 1, size: 100 });
    api.listSnapshots.mockResolvedValue([practicalSnapshot]);
    api.previewSnapshot.mockResolvedValue(practicalSnapshot);

    const user = userEvent.setup();
    mount('practical');
    await user.click(await screen.findByRole('button', { name: /快照预览/ }));

    expect(await screen.findByTestId('competition-practical-snapshot-paper')).toBeInTheDocument();
    expect(screen.getByText('职业技能等级认定')).toBeInTheDocument();
    expect(screen.getByText('注 意 事 项')).toBeInTheDocument();
    expect(screen.getByText('试题1、加铜盘存铝操作')).toBeInTheDocument();
    expect(screen.getByText('（1）本题分值：')).toBeInTheDocument();
    expect(screen.getByText('40分')).toBeInTheDocument();
    expect(screen.getByText(/考核时间：20min/)).toBeInTheDocument();
    expect(screen.getByText('出铝前对原铝液中铜含量取样并完成化验。')).toBeInTheDocument();
    expect(screen.getByText('（5）否定项说明：')).toBeInTheDocument();
    expect(screen.queryByText('题干/操作内容')).not.toBeInTheDocument();
  });
});
