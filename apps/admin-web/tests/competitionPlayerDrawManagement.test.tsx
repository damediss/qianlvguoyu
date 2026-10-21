import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { pageCompetitionPlayerDrawTasks } from '../src/api/competitionPlayerDraw';
import { TabPaneActiveProvider } from '../src/layouts/TabKeepAlive';
import { DrawTaskWorkspace } from '../src/pages/competition/CompetitionPlayerDrawPage';

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (select: (state: { permissionCodes: string[] }) => unknown) => select({
    permissionCodes: ['competition:player-draw:view'],
  }),
}));

vi.mock('../src/api/competitionPlayerDraw', () => ({
  createCompetitionPlayerDrawTask: vi.fn(),
  deleteCompetitionPlayerDrawTask: vi.fn(),
  downloadCompetitionPlayerDrawRoster: vi.fn(),
  getCompetitionPlayerDrawTask: vi.fn(),
  listCompetitionPlayerDrawCandidates: vi.fn(),
  pageCompetitionPlayerDrawTasks: vi.fn(),
  saveCompetitionPlayerDrawTaskSelection: vi.fn(),
}));

function renderWorkspace(active: boolean) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <TabPaneActiveProvider active={active}>
        <DrawTaskWorkspace mode="management" stageId="10" stageName="资格赛" />
      </TabPaneActiveProvider>
    </QueryClientProvider>,
  );
  return client;
}

describe('抽签管理首页刷新', () => {
  it('当前标签可见时每 3 秒刷新任务进度', async () => {
    vi.mocked(pageCompetitionPlayerDrawTasks).mockResolvedValue({
      items: [], page: 1, pageSize: 10, total: 0,
    });

    const client = renderWorkspace(true);
    await waitFor(() => expect(pageCompetitionPlayerDrawTasks).toHaveBeenCalled());

    const query = client.getQueryCache().find({
      queryKey: ['competition-player-draw-tasks', '10', 1],
    });
    expect(query?.options.refetchInterval).toBe(3000);
  });

  it('标签隐藏时暂停任务进度轮询', async () => {
    vi.mocked(pageCompetitionPlayerDrawTasks).mockResolvedValue({
      items: [], page: 1, pageSize: 10, total: 0,
    });

    const client = renderWorkspace(false);
    await waitFor(() => expect(pageCompetitionPlayerDrawTasks).toHaveBeenCalled());

    const query = client.getQueryCache().find({
      queryKey: ['competition-player-draw-tasks', '10', 1],
    });
    expect(query?.options.refetchInterval).toBe(false);
  });
});
