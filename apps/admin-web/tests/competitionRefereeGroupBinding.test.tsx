import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { bindCompetitionRefereeGroup, getCompetitionRefereeAssignmentWorkspace } from '../src/api/competition';
import { CompetitionRefereeAssignmentPanel } from '../src/pages/competition/CompetitionRefereeAssignmentPanel';

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (select: (state: { permissionCodes: string[] }) => unknown) => select({
    permissionCodes: [
      'competition:referee-assignment:view',
      'competition:referee-assignment:manage',
    ],
  }),
}));

vi.mock('../src/api/competition', () => ({
  bindCompetitionRefereeGroup: vi.fn(),
  getCompetitionRefereeAssignmentWorkspace: vi.fn(),
}));

describe('赛段实操裁判配置', () => {
  it('只允许选择已确定的裁判小组绑定选手，不再提供创建入口', async () => {
    const user = userEvent.setup();
    vi.mocked(getCompetitionRefereeAssignmentWorkspace).mockResolvedValue({
      stageId: '10',
      eventId: '20',
      eventWorkTypeId: '30',
      eventWorkTypeLevelId: '40',
      stageName: '初赛',
      levelName: '高级工',
      referees: [],
      groups: [{
        id: '100',
        groupNo: 1,
        groupName: '第一裁判小组',
        status: 'confirmed',
        bindable: true,
        leaderRefereeProfileId: '1',
        members: [
          { id: '11', refereeMemberId: '21', refereeProfileId: '1', refereeName: '张三', memberRole: 'leader', status: 'active' },
          { id: '12', refereeMemberId: '22', refereeProfileId: '2', refereeName: '李四', memberRole: 'member', status: 'active' },
          { id: '13', refereeMemberId: '23', refereeProfileId: '3', refereeName: '王五', memberRole: 'member', status: 'active' },
        ],
      }],
      candidates: [{
        contestantProfileId: '200',
        contestantMemberId: '201',
        contestantNo: 'C001',
        name: '选手甲',
        orgName: '某单位',
        assignmentStatus: 'unassigned',
        paperSnapshotId: '500',
        paperCode: 'P1',
        assignments: [],
      }],
    });
    vi.mocked(bindCompetitionRefereeGroup).mockResolvedValue({} as never);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionRefereeAssignmentPanel stageId="10" />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('选手甲')).toBeInTheDocument();
    expect(screen.queryByText('新建裁判组')).not.toBeInTheDocument();
    expect(screen.queryByText('新建裁判小组')).not.toBeInTheDocument();
    expect(screen.getByText(/裁判管理 \/ 裁判小组/)).toBeInTheDocument();

    await user.click(screen.getByRole('combobox'));
    await user.click(await screen.findByText('第一裁判小组（3名裁判）'));
    await waitFor(() => expect(bindCompetitionRefereeGroup).toHaveBeenCalledWith('10', '201', '100'));
  });
});
