/**
 * TextbookVersionDrawer 测试 —— 《二期编写协同实施设计》§12.3
 * 用例：快照列表、并排查看、恢复条件、恢复后刷新版本。
 */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TextbookVersionDrawer } from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/TextbookVersionDrawer';
import type { SectionContentVO } from '../src/types/textbookCollaboration';

vi.mock('md-editor-rt', () => ({
  MdPreview: (props: { value?: string }) => <div data-testid="md-preview">{props.value}</div>,
}));

const api = vi.hoisted(() => ({
  pageSectionVersions: vi.fn(),
  getSectionVersion: vi.fn(),
  restoreSectionVersion: vi.fn(),
  getSectionAssetContent: vi.fn(),
}));

vi.mock('../src/api/textbookCollaboration', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/api/textbookCollaboration')>();
  return {
    ...actual,
    pageSectionVersions: api.pageSectionVersions,
    getSectionVersion: api.getSectionVersion,
    restoreSectionVersion: api.restoreSectionVersion,
    getSectionAssetContent: api.getSectionAssetContent,
  };
});

function w2(overrides: Record<string, unknown> = {}): SectionContentVO {
  return {
    sectionId: '9101',
    chapterId: '9001',
    projectId: '8001',
    sectionTitle: '更换阳极',
    chapterTitle: '铝电解槽操作',
    targetWords: 5000,
    contentExists: true,
    contentMd: '# 当前内容',
    contentStatus: 'draft',
    wordCount: 100,
    version: 4,
    relations: ['writer'],
    canWrite: true,
    canReview: false,
    readOnly: false,
    readOnlyReason: null,
    checkReport: null,
    assets: [],
    submittedBy: null,
    submittedByName: null,
    submittedAt: null,
    submitRevision: null,
    currentSnapshotId: null,
    reviewedByName: null,
    reviewedAt: null,
    latestReviewRecord: null,
    ...overrides,
  };
}

const VERSIONS = [
  { id: '9602', snapshotNo: 2, snapshotType: 'submit', submitRevision: 2, wordCount: 4200, createdAt: '2026-10-24 11:00:00', createdByName: '张工' },
  { id: '9601', snapshotNo: 1, snapshotType: 'submit', submitRevision: 1, wordCount: 3800, createdAt: '2026-10-23 11:00:00', createdByName: '张工' },
];

function renderDrawer(section: SectionContentVO, dirty = false, onRestored: (vo: SectionContentVO) => void = vi.fn()) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={client}>
        <TextbookVersionDrawer open section={section} dirty={dirty} onClose={vi.fn()} onRestored={onRestored} />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

describe('TextbookVersionDrawer', () => {
  beforeEach(() => {
    api.pageSectionVersions.mockReset().mockResolvedValue({ items: VERSIONS, page: 1, pageSize: 20, total: 3 });
    api.getSectionVersion.mockReset().mockImplementation(async (id: string) => ({
      ...VERSIONS[0],
      id,
      contentMd: `# 快照内容-${id}`,
    }));
    api.restoreSectionVersion.mockReset().mockImplementation(async (_sid: string, req: { snapshotId: string }) =>
      w2({ contentMd: `# 快照内容-${req.snapshotId}`, version: 5 }),
    );
    api.getSectionAssetContent.mockReset().mockRejectedValue(new Error('no asset'));
  });

  it('快照列表：W7 分页加载，展示「提交第 N 版」，total>已加载时显示加载更多', async () => {
    renderDrawer(w2());
    expect(await screen.findByText('提交第 2 版')).toBeInTheDocument();
    expect(screen.getByText('提交第 1 版')).toBeInTheDocument();
    expect(api.pageSectionVersions).toHaveBeenCalledWith('9101', { page: 1, pageSize: 20 });
    expect(screen.getByRole('button', { name: /加载更多/ })).toBeInTheDocument();
    expect(screen.getByText(/2\/3/)).toBeInTheDocument();
  }, 30000);

  it('并排查看：点选快照后加载 W8，快照与当前内容并排渲染', async () => {
    renderDrawer(w2());
    await screen.findByText('提交第 2 版');
    await userEvent.click(screen.getByText('提交第 2 版'));

    await waitFor(() => expect(api.getSectionVersion).toHaveBeenCalledWith('9602'));
    expect(await screen.findByText('当前内容（v4）')).toBeInTheDocument();
    // 两个预览：快照内容 + 当前内容
    expect(screen.getByText('# 快照内容-9602')).toBeInTheDocument();
    expect(screen.getByText('# 当前内容')).toBeInTheDocument();
  }, 30000);

  it('恢复条件：非当前主笔/非 draft·returned 不出现恢复按钮；dirty 时禁用', async () => {
    // submitted 节：只允许查看
    const { rerender } = renderDrawer(w2({ contentStatus: 'submitted', canWrite: false, readOnly: true }));
    await screen.findByText('提交第 1 版');
    expect(screen.queryByRole('button', { name: /恢\s*复/ })).toBeNull();

    // draft + dirty：恢复按钮禁用
    rerender(
      <ConfigProvider locale={zhCN}>
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
          <TextbookVersionDrawer open section={w2()} dirty onClose={vi.fn()} onRestored={vi.fn()} />
        </QueryClientProvider>
      </ConfigProvider>,
    );
    await screen.findByText('提交第 1 版');
    const restoreButtons = screen.getAllByRole('button', { name: /恢\s*复/ });
    expect(restoreButtons.length).toBeGreaterThanOrEqual(1);
    restoreButtons.forEach((b) => expect(b).toBeDisabled());
  }, 30000);

  it('恢复后刷新版本：Popconfirm 确认 → W9 带 snapshotId+baseVersion → onRestored 回传最新 W2', async () => {
    const onRestored = vi.fn();
    renderDrawer(w2(), false, onRestored);
    await screen.findByText('提交第 1 版');

    const firstRow = screen.getByText('提交第 1 版').closest('.ant-list-item') as HTMLElement;
    const rowRestoreBtn = [...firstRow.querySelectorAll('button')].find(
      (b) => (b.textContent || '').replace(/\s/g, '') === '恢复',
    );
    expect(rowRestoreBtn).toBeTruthy();
    await userEvent.click(rowRestoreBtn!);
    expect(await screen.findByText(/恢复为「提交第 1 版」/)).toBeInTheDocument();
    // Popconfirm 确认按钮（位于弹出层内）
    const popover = document.querySelector('.ant-popover') as HTMLElement;
    await userEvent.click(within(popover).getByRole('button', { name: /恢\s*复/ }));

    await waitFor(() =>
      expect(api.restoreSectionVersion).toHaveBeenCalledWith('9101', { snapshotId: '9601', baseVersion: 4 }),
    );
    await waitFor(() => expect(onRestored).toHaveBeenCalled());
    expect(onRestored.mock.calls[0][0]).toMatchObject({ contentMd: '# 快照内容-9601', version: 5 });
  }, 30000);
});
