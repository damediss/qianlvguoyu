import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TextbookOutlinePage } from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/TextbookOutlinePage';

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

const BALANCED = { chapterSum: 100000, totalWords: 100000, balanced: true, unbalancedChapters: [] };

const PROJECT = {
  id: '7001',
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
  summary: BALANCED,
};

const FLAT = [
  { id: '100', parentId: '0', nodeLevel: 1, dimension: null, sourceNo: null, titleRaw: '基本要求', title: '基本要求', weight: null, targetWords: null, wordsManual: 0, sortNo: 1 },
  { id: '101', parentId: '0', nodeLevel: 1, dimension: null, sourceNo: null, titleRaw: '工作要求', title: '工作要求', weight: null, targetWords: null, wordsManual: 0, sortNo: 2 },
  { id: '200', parentId: '100', nodeLevel: 2, dimension: null, sourceNo: '2.1', titleRaw: '职业道德', title: '职业道德', weight: 5, targetWords: 5000, wordsManual: 0, sortNo: 1 },
  { id: '201', parentId: '100', nodeLevel: 2, dimension: null, sourceNo: '2.2', titleRaw: '基础知识', title: '基础知识', weight: 10, targetWords: 10000, wordsManual: 1, sortNo: 2 },
  { id: '300', parentId: '200', nodeLevel: 3, dimension: null, sourceNo: '2.1.1', titleRaw: '职业道德基本知识', title: '职业道德基本知识', weight: null, targetWords: 3000, wordsManual: 0, sortNo: 1 },
  { id: '400', parentId: '300', nodeLevel: 4, dimension: 'theory', sourceNo: '1.1.1.1', titleRaw: '职业道德基本知识', title: '职业道德基本知识', weight: null, targetWords: null, wordsManual: 0, sortNo: 1 },
  { id: '401', parentId: '300', nodeLevel: 4, dimension: 'skill', sourceNo: '1.1.1.2', titleRaw: '职业守则', title: '职业守则', weight: null, targetWords: null, wordsManual: 0, sortNo: 2 },
];

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={client}>
        <TextbookOutlinePage projectId="7001" />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

describe('TextbookOutlinePage', () => {
  beforeEach(() => {
    permittedCodes = ['textbook:project:view', 'textbook:project:manage', 'textbook:project:export'];
    api.getProject.mockReset().mockResolvedValue(PROJECT);
    api.getOutline.mockReset().mockResolvedValue(FLAT);
    api.updateOutlineNode.mockReset().mockResolvedValue(BALANCED);
    api.createOutlineNode.mockReset().mockResolvedValue(FLAT[6]);
    api.deleteOutlineNode.mockReset().mockResolvedValue(undefined);
    api.rebuildOutline.mockReset().mockResolvedValue(BALANCED);
    api.updateProject.mockReset().mockResolvedValue(PROJECT);
    api.exportProject.mockReset();
  });

  it('树渲染：篇/章/节/点缩进正确，理论/技能 Tag 与合计校验条展示', async () => {
    renderPage();
    expect(await screen.findByText('铝电解工（槽操作工 · 五级/初级工）')).toBeInTheDocument();
    expect(screen.getByText(/合计校验通过/)).toBeInTheDocument();

    expect(screen.getByText('职业道德')).toBeInTheDocument();
    expect(screen.getByText('职业守则')).toBeInTheDocument();
    expect(screen.getByText('理论')).toBeInTheDocument();
    expect(screen.getByText('技能')).toBeInTheDocument();
    expect(screen.getByText('已手调')).toBeInTheDocument();
    expect(screen.getByText('5%')).toBeInTheDocument();

    // 章挂篇下（level-1），点挂节下（level-3）
    expect(screen.getByText('职业道德').closest('tr')?.className).toContain('ant-table-row-level-1');
    expect(screen.getByText('职业守则').closest('tr')?.className).toContain('ant-table-row-level-3');
    await waitFor(() => expect(api.getOutline).toHaveBeenCalledWith('7001'));
  });

  it('行内改字数：失焦提交 updateOutlineNode，响应不平衡时校验条变红', async () => {
    api.updateOutlineNode.mockResolvedValue({
      chapterSum: 103000,
      totalWords: 100000,
      balanced: false,
      unbalancedChapters: ['职业道德基本知识'],
    });
    renderPage();
    await screen.findByText('职业道德');

    const input = await screen.findByDisplayValue('3000');
    fireEvent.change(input, { target: { value: '6000' } });
    fireEvent.blur(input);

    await waitFor(() => expect(api.updateOutlineNode).toHaveBeenCalledWith('300', { targetWords: 6000 }));
    expect(await screen.findByText(/合计不平衡/)).toBeInTheDocument();
    expect(screen.getByText(/不平衡章节：职业道德基本知识/)).toBeInTheDocument();
  });

  it('重新配比：Popconfirm 确认后调 rebuildOutline 并刷新大纲', async () => {
    renderPage();
    await screen.findByText('职业道德');

    await userEvent.click(screen.getByRole('button', { name: /重新配比/ }));
    await userEvent.click(await screen.findByRole('button', { name: /确\s*定/ }));

    await waitFor(() => expect(api.rebuildOutline).toHaveBeenCalledWith('7001'));
    await waitFor(() => expect(api.getOutline.mock.calls.length).toBeGreaterThan(1));
  });

  it('删除节点：Popconfirm 确认后调 deleteOutlineNode（含子孙提示）', async () => {
    renderPage();
    await screen.findByText('职业道德');

    const row = screen.getByText('职业道德').closest('tr') as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: '删除节点' }));
    await screen.findByText(/将同时删除其全部子孙节点/);
    await userEvent.click(await screen.findByRole('button', { name: /确\s*定/ }));

    // TanStack Query 的 mutationFn 还会收到 { client } 上下文参数，只校验业务入参
    await waitFor(() => expect(api.deleteOutlineNode.mock.calls[0]?.[0]).toBe('200'));
  });

  it('下移：同级交换 sortNo（两个节点各发一次请求）', async () => {
    renderPage();
    await screen.findByText('职业道德');

    const row = screen.getByText('职业道德').closest('tr') as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: '下移节点' }));

    await waitFor(() => expect(api.updateOutlineNode).toHaveBeenCalledWith('200', { sortNo: 2 }));
    await waitFor(() => expect(api.updateOutlineNode).toHaveBeenCalledWith('201', { sortNo: 1 }));
  });

  it('权限显隐：仅 view 时重新配比/评审通过/删除/导出不出现', async () => {
    permittedCodes = ['textbook:project:view'];
    renderPage();
    await screen.findByText('职业道德');

    expect(screen.queryByRole('button', { name: /重新配比/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /评审通过/ })).toBeNull();
    expect(screen.queryByRole('button', { name: '删除节点' })).toBeNull();
    expect(screen.queryByRole('button', { name: /导出/ })).toBeNull();
  });
});
