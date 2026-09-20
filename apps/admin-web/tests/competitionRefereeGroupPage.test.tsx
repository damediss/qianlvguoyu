import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { listCompetitionRefereeGroups, pageCompetitionReferees } from '../src/api/competition';
import { CompetitionRefereeGroupPage } from '../src/pages/competition/CompetitionRefereeGroupPage';

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (select: (state: { permissionCodes: string[] }) => unknown) => select({
    permissionCodes: [
      'competition:referee-group:view',
      'competition:referee-group:manage',
    ],
  }),
}));

vi.mock('../src/api/competition', () => ({
  confirmCompetitionRefereeGroup: vi.fn(),
  createCompetitionRefereeGroup: vi.fn(),
  deleteCompetitionRefereeGroup: vi.fn(),
  listCompetitionRefereeGroups: vi.fn(),
  pageCompetitionReferees: vi.fn(),
  updateCompetitionRefereeGroup: vi.fn(),
}));

describe('裁判小组页面', () => {
  it('在裁判管理下维护高于赛段的裁判小组', async () => {
    vi.mocked(listCompetitionRefereeGroups).mockResolvedValue([{
      id: '100',
      groupNo: 1,
      groupName: '第一裁判小组',
      status: 'draft',
      leaderRefereeProfileId: '1',
      members: [
        { id: '11', refereeMemberId: '21', refereeProfileId: '1', refereeName: '张三', memberRole: 'leader', status: 'active' },
        { id: '12', refereeMemberId: '22', refereeProfileId: '2', refereeName: '李四', memberRole: 'member', status: 'active' },
        { id: '13', refereeMemberId: '23', refereeProfileId: '3', refereeName: '王五', memberRole: 'member', status: 'active' },
      ],
    }]);
    vi.mocked(pageCompetitionReferees).mockResolvedValue({
      items: [], page: 1, pageSize: 200, total: 0,
    } as never);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionRefereeGroupPage />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('第一裁判小组')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /新建裁判小组/ })).toBeInTheDocument();
    expect(screen.getByText(/层级高于赛段/)).toBeInTheDocument();
  });
});
