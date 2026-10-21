import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TextbookProjectPage } from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/TextbookProjectPage';

const api = vi.hoisted(() => ({
  parsePreview: vi.fn(),
  createProject: vi.fn(),
  pageProjects: vi.fn(),
  getProject: vi.fn(),
  updateProject: vi.fn(),
  deleteProject: vi.fn(),
  rebuildOutline: vi.fn(),
  getOutline: vi.fn(),
  updateOutlineNode: vi.fn(),
  createOutlineNode: vi.fn(),
  deleteOutlineNode: vi.fn(),
  exportProject: vi.fn(),
}));

vi.mock('../src/api/textbook', () => api);

let permittedCodes: string[] = [];

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (selector: (s: { hasPermission: (code: string) => boolean }) => unknown) =>
    selector({ hasPermission: (code: string) => permittedCodes.includes(code) }),
}));

const RECORD = {
  id: '8001',
  workTypeTitle: '铝电解工',
  seriesNo: '6-17-02-02',
  standardVersion: '2020',
  fileId: '6001',
  direction: '槽操作工',
  levelCode: 5,
  functionNos: [1, 2, 12],
  trainingHours: 40,
  wordsPerHour: 2500,
  totalWords: 100000,
  status: 'outlined',
  parseWarnings: null,
  updatedAt: '2026-08-21 10:00:00',
};

const PREVIEW = {
  workTypeTitle: '铝电解工',
  seriesNo: '6-17-02-02',
  standardVersion: '2020',
  directions: ['槽操作工', '吸出'],
  directionLevels: { 槽操作工: [5], 吸出: [4, 5] },
  trainingHoursByLevel: { 5: 40, 4: 36 },
  guessedFunctionNos: [1, 2, 12],
  warnings: ['未找到技能要求权重表，已按条数均分'],
};

const PREVIEW_NO_GUESS = {
  ...PREVIEW,
  directions: ['默认'],
  directionLevels: { 默认: [5] },
  guessedFunctionNos: [],
  warnings: ['方向映射未识别，请人工登记'],
};

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={client}>
        <TextbookProjectPage />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

async function selectOption(title: string) {
  const option = await screen.findByTitle(title);
  await userEvent.click(option);
}

async function uploadDocx() {
  const input = document.querySelector('input[type="file"]') as HTMLInputElement;
  await userEvent.upload(
    input,
    new File(['docx-bytes'], '铝电解工.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    }),
  );
}

describe('TextbookProjectPage', () => {
  beforeEach(() => {
    permittedCodes = ['textbook:project:view', 'textbook:project:manage', 'textbook:project:export'];
    window.history.replaceState({}, '', '/');
    api.pageProjects.mockReset().mockResolvedValue({ items: [RECORD], page: 1, pageSize: 20, total: 1 });
    api.parsePreview.mockReset().mockResolvedValue(PREVIEW);
    api.createProject.mockReset().mockResolvedValue(RECORD);
    api.updateProject.mockReset().mockResolvedValue(RECORD);
    api.deleteProject.mockReset().mockResolvedValue(undefined);
    api.exportProject.mockReset();
  });

  it('列表渲染：表格列齐全，查询参数进入请求', async () => {
    renderPage();
    expect(await screen.findByText('铝电解工')).toBeInTheDocument();
    for (const header of ['工种', '方向', '等级', '学时', '总字数', '状态', '更新时间', '操作']) {
      // antd Table 渲染隐藏 measure 行，表头文本会出现多次
      expect(screen.getAllByText(header).length).toBeGreaterThan(0);
    }
    expect(screen.getByText('槽操作工')).toBeInTheDocument();
    expect(screen.getByText('五级/初级工')).toBeInTheDocument();
    expect(screen.getByText('大纲已生成')).toBeInTheDocument();
    expect(api.pageProjects).toHaveBeenCalledWith({ page: 1, pageSize: 20 });

    await userEvent.type(screen.getByPlaceholderText('工种名称'), '铝电解');
    await waitFor(() =>
      expect(api.pageProjects).toHaveBeenLastCalledWith({
        page: 1,
        pageSize: 20,
        workTypeTitle: '铝电解',
      }),
    );
  });

  it('权限显隐：仅 view 时新建/编辑/删除/导出不出现，大纲可进', async () => {
    permittedCodes = ['textbook:project:view'];
    renderPage();
    await screen.findByText('铝电解工');

    expect(screen.queryByRole('button', { name: /新建教材工程/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /^编辑$/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /^删除$/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /导出/ })).toBeNull();
    expect(screen.getByRole('button', { name: '大纲' })).toBeInTheDocument();
    expect(screen.getByText(/当前账号仅有查看权限/)).toBeInTheDocument();
  });

  it('新建向导：上传 → parse-preview 回填 → 提交调 createProject 并跳大纲页', async () => {
    renderPage();
    await screen.findByText('铝电解工');

    await userEvent.click(screen.getByRole('button', { name: /新建教材工程/ }));
    const dialog = await screen.findByRole('dialog');

    await uploadDocx();
    // 回填预览：职业名称/编码/版本 + 解析警告
    expect(await within(dialog).findByText('铝电解工')).toBeInTheDocument();
    expect(within(dialog).getByText('6-17-02-02')).toBeInTheDocument();
    expect(within(dialog).getByText('未找到技能要求权重表，已按条数均分')).toBeInTheDocument();
    expect(api.parsePreview).toHaveBeenCalled();

    await userEvent.click(within(dialog).getByRole('button', { name: '下一步' }));
    // 选方向：等级受 directionLevels 约束并回填学时，职业功能预选，系数默认 2500
    await userEvent.click(within(dialog).getAllByRole('combobox')[0]);
    await selectOption('槽操作工');
    expect(within(dialog).getByDisplayValue('40')).toBeInTheDocument();
    expect(within(dialog).getByDisplayValue('2500')).toBeInTheDocument();
    expect(within(dialog).getByText('职业功能 1')).toBeInTheDocument();
    expect(within(dialog).getByText('职业功能 12')).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: '下一步' }));
    expect(within(dialog).getByText('预计总字数')).toBeInTheDocument();
    expect(within(dialog).getByText('100000')).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /创建并生成大纲/ }));
    await waitFor(() => expect(api.createProject).toHaveBeenCalledTimes(1));
    const [createReq] = api.createProject.mock.calls[0];
    expect(createReq).toEqual(
      expect.objectContaining({
        direction: '槽操作工',
        levelCode: 5,
        functionNos: [1, 2, 12],
        trainingHours: 40,
        wordsPerHour: 2500,
      }),
    );
    expect((createReq.file as File).name).toBe('铝电解工.docx');
    await waitFor(() => expect(window.location.pathname).toBe('/textbook/outline/8001'));
  }, 60000);

  it('新建向导：解析失败展示 Alert 且不能进入下一步', async () => {
    api.parsePreview.mockRejectedValue({ response: { data: { message: '未识别权重表，无法配比' } } });
    renderPage();
    await screen.findByText('铝电解工');

    await userEvent.click(screen.getByRole('button', { name: /新建教材工程/ }));
    const dialog = await screen.findByRole('dialog');
    await uploadDocx();

    expect(await within(dialog).findByText('未识别权重表，无法配比')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: '下一步' })).toBeDisabled();
  }, 30000);

  it('新建向导：方向映射未识别时提示人工勾选，可手工选择职业功能提交', async () => {
    api.parsePreview.mockResolvedValue(PREVIEW_NO_GUESS);
    renderPage();
    await screen.findByText('铝电解工');

    await userEvent.click(screen.getByRole('button', { name: /新建教材工程/ }));
    const dialog = await screen.findByRole('dialog');
    await uploadDocx();
    await userEvent.click(await within(dialog).findByRole('button', { name: '下一步' }));

    // 单方向自动选中「默认」并回填学时；出现人工勾选提示
    expect(await within(dialog).findByText('未识别方向映射，请人工勾选职业功能')).toBeInTheDocument();
    expect(within(dialog).getByDisplayValue('40')).toBeInTheDocument();

    // 手工选择职业功能（兜底选项范围）
    await userEvent.click(within(dialog).getAllByRole('combobox')[2]);
    await selectOption('职业功能 1');
    await selectOption('职业功能 2');
    await userEvent.keyboard('{Escape}');

    await userEvent.click(within(dialog).getByRole('button', { name: '下一步' }));
    expect(await within(dialog).findByText('职业功能 1、职业功能 2')).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: /创建并生成大纲/ }));
    await waitFor(() => expect(api.createProject).toHaveBeenCalledTimes(1));
    const [manualReq] = api.createProject.mock.calls[0];
    expect(manualReq).toEqual(
      expect.objectContaining({ direction: '默认', levelCode: 5, functionNos: [1, 2] }),
    );
  }, 60000);
});
