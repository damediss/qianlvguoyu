import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import {
  bindCompetitionPracticalPaper,
  listCompetitionEventWorkTypeLevels,
  listCompetitionPracticalPaperBindings,
} from '../src/api/competition';
import { listCompetitionMakeExamSnapshotsForScope } from '../src/api/competitionQuestion';
import { CompetitionPracticalConfigPage } from '../src/pages/competition/CompetitionPracticalConfigPage';

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (select: (state: { permissionCodes: string[] }) => unknown) => select({
    permissionCodes: [
      'competition:make-plan:view',
      'competition:stage-plan:view',
      'competition:stage-plan:manage',
    ],
  }),
}));

vi.mock('../src/api/competition', () => ({
  bindCompetitionPracticalPaper: vi.fn(),
  listCompetitionEventWorkTypeLevels: vi.fn(),
  listCompetitionPracticalPaperBindings: vi.fn(),
}));

vi.mock('../src/api/competitionQuestion', () => ({
  listCompetitionMakeExamSnapshotsForScope: vi.fn(),
}));

describe('实操配置试卷绑定工作台', () => {
  it('直接点击已发布实操试卷并建立赛段绑定关系', async () => {
    const user = userEvent.setup();
    vi.mocked(listCompetitionEventWorkTypeLevels).mockResolvedValue([
      { id: '40', levelCode: '3', levelName: '高级工' },
    ] as never);
    vi.mocked(listCompetitionMakeExamSnapshotsForScope).mockResolvedValue([{
      id: '500', planId: '600', eventId: '20', eventWorkTypeId: '30', eventWorkTypeLevelId: '40',
      contentType: 'practical', paperCode: 'P1', versionNo: 1, title: '实操蓝图 P1', durationMinutes: 90,
      questionCount: 14, totalScore: '100', passScore: '60', status: 'published', questions: [],
    }] as never);
    vi.mocked(listCompetitionPracticalPaperBindings).mockResolvedValue([]);
    vi.mocked(bindCompetitionPracticalPaper).mockResolvedValue({
      id: '900', eventId: '20', stageId: '10', eventWorkTypeId: '30',
      eventWorkTypeLevelId: '40', paperSnapshotId: '500',
    } as never);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionPracticalConfigPage
          eventId="20"
          stageId="10"
          stageName="初赛"
          eventWorkTypeId="30"
          eventWorkTypeLevelCode="3"
        />
      </QueryClientProvider>,
    );

    expect(screen.queryByText('执行方案与绑定场次')).not.toBeInTheDocument();
    expect(screen.queryByText('初赛实操场次')).not.toBeInTheDocument();
    expect(await screen.findByText('P1 卷')).toBeInTheDocument();
    await waitFor(() => expect(document.querySelector('.ant-spin-container.ant-spin-blur')).not.toBeInTheDocument());
    await user.click(screen.getByText('P1 卷').closest('tr')!);
    await waitFor(() => expect(bindCompetitionPracticalPaper).toHaveBeenCalledWith('10', {
      eventWorkTypeId: '30',
      eventWorkTypeLevelId: '40',
      paperSnapshotId: '500',
    }));
  });
});
