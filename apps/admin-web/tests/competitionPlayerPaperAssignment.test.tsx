import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { CompetitionPlayerPaperAssignmentPage } from '../src/pages/competition/CompetitionPlayerPaperAssignmentPage';
import {
  getCompetitionStagePaperAssignmentSummary,
  listCompetitionStagePaperAssignmentPlayers,
  saveCompetitionStagePaperAssignments,
} from '../src/api/competition';

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (selector: (state: unknown) => unknown) => selector({ permissionCodes: [
    'competition:stage-paper-assignment:view', 'competition:stage-paper-assignment:manage',
    'competition:stage-paper-assignment:confirm',
    'competition:exam-session:view', 'competition:exam-session:prepare',
  ] }),
}));
vi.mock('../src/api/competition', () => ({
  getCompetitionStagePaperAssignmentSummary: vi.fn().mockResolvedValue({
    stageName: '初赛', workTypeName: '电工', levelName: '高级', levelCode: '3',
    eligibleCount: 1, paperCount: 5, assignedCount: 0, unassignedCount: 1, confirmed: false,
    papers: Array.from({ length: 5 }, (_, index) => ({
      id: String(500 + index), paperCode: `P${index + 1}`, title: `试卷${index + 1}`,
      versionNo: 1, assignedCount: 0, percentage: 0,
    })),
  }),
  listCompetitionStagePaperAssignmentPlayers: vi.fn().mockResolvedValue([{
    contestantProfileId: '100', name: '张三', orgName: '测试单位',
    orgId: '1', workTypeName: '电工', levelName: '高级', levelCode: '3',
    paperSnapshotId: null, assignmentStatus: 'unassigned', configurationStatus: 'draft',
  }]),
  autoAllocateCompetitionStagePaperAssignments: vi.fn(),
  saveCompetitionStagePaperAssignments: vi.fn().mockResolvedValue({}),
  confirmCompetitionStagePaperAssignments: vi.fn(),
}));

describe('选手试卷配置', () => {
  beforeEach(() => {
    vi.mocked(getCompetitionStagePaperAssignmentSummary).mockResolvedValue({
      stageName: '初赛', workTypeName: '电工', levelName: '高级', levelCode: '3',
      eligibleCount: 1, paperCount: 5, assignedCount: 0, unassignedCount: 1, confirmed: false,
      papers: Array.from({ length: 5 }, (_, index) => ({
        id: String(500 + index), paperCode: `P${index + 1}`, title: `试卷${index + 1}`,
        versionNo: 1, assignedCount: 0, percentage: 0,
      })),
    } as never);
    vi.mocked(listCompetitionStagePaperAssignmentPlayers).mockResolvedValue([{
      contestantProfileId: '100', name: '张三', orgName: '测试单位',
      orgId: '1', workTypeName: '电工', levelName: '高级', levelCode: '3',
      paperSnapshotId: null, assignmentStatus: 'unassigned', configurationStatus: 'draft',
    }] as never);
  });

  it('renders arbitrary paper counts and persists a visual draft selection', async () => {
    vi.mocked(saveCompetitionStagePaperAssignments).mockResolvedValue({} as never);
    const user = userEvent.setup();
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={client}><CompetitionPlayerPaperAssignmentPage stageId="10" /></QueryClientProvider>);
    expect(await screen.findByText('试卷5')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /确认配置/ })).toBeDisabled();
    await user.click(screen.getByRole('tab', { name: /可视化调整/ }));
    await user.click(screen.getByRole('button', { name: 'P5卷' }));
    const checkboxes = screen.getAllByRole('checkbox');
    await user.click(checkboxes[checkboxes.length - 1]);
    await user.click(screen.getByRole('button', { name: /移\s*入/ }));
    expect(screen.getByText('有未保存的人工调整')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => expect(saveCompetitionStagePaperAssignments).toHaveBeenCalledWith('10', [
      { contestantProfileId: '100', paperSnapshotId: '504' },
    ]));
    client.clear();
  }, 15000);

  it('locks assignment editing after a formal exam session exists without showing a console entry button', async () => {
    vi.mocked(getCompetitionStagePaperAssignmentSummary).mockResolvedValue({
      stageName: '初赛', workTypeName: '电工', levelName: '高级', levelCode: '3',
      eligibleCount: 1, paperCount: 1, assignedCount: 1, unassignedCount: 0, confirmed: true,
      formalExamLocked: true, formalExamSessionId: '9', formalExamStatus: 'ready',
      papers: [{ id: '500', paperCode: 'A', title: 'A卷', versionNo: 1, assignedCount: 1, percentage: 100 }],
    } as never);
    vi.mocked(listCompetitionStagePaperAssignmentPlayers).mockResolvedValue([{
      contestantProfileId: '100', name: '张三', orgName: '测试单位',
      orgId: '1', workTypeName: '电工', levelName: '高级', levelCode: '3',
      paperSnapshotId: '500', paperCode: 'A', assignmentStatus: 'assigned', configurationStatus: 'confirmed',
    }] as never);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={client}><CompetitionPlayerPaperAssignmentPage stageId="10" /></QueryClientProvider>);
    expect(await screen.findByText('正式比赛数据已生成，当前试卷分配已锁定。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /重新自动分配/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /确认配置/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /保\s*存/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /进入正式比赛/ })).not.toBeInTheDocument();
    expect(screen.getByText(/从竞赛菜单进入理论比赛控制台/)).toBeInTheDocument();
    client.clear();
  });
});
