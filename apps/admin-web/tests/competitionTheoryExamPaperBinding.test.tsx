import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';
import { CompetitionTheoryExamPage } from '../src/pages/competition/CompetitionTheoryExamPage';
import {
  getCompetitionEvent,
  importCompetitionTheoryPaperSnapshots,
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  listCompetitionTheoryPaperSnapshots,
  pageCompetitionStages,
} from '../src/api/competition';
import { listCompetitionMakeExamSnapshotsForScope } from '../src/api/competitionQuestion';

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (select: (state: { permissionCodes: string[] }) => unknown) => select({
    permissionCodes: [
      'competition:event:view',
      'competition:theory-paper:view',
      'competition:theory-paper:manage',
      'competition:stage:view',
      'competition:event-work-type:view',
    ],
  }),
}));

vi.mock('../src/api/competition', () => ({
  createCompetitionTheorySchedule: vi.fn(),
  deleteCompetitionTheoryRoom: vi.fn(),
  deleteCompetitionTheorySession: vi.fn(),
  downloadCompetitionTheoryRoster: vi.fn(),
  getCompetitionEvent: vi.fn(),
  getCompetitionTheoryPaperPreview: vi.fn(),
  getCompetitionTheorySchedule: vi.fn(),
  getCompetitionTheorySchedulePreflight: vi.fn(),
  importCompetitionTheoryPaperSnapshots: vi.fn(),
  listCompetitionEventWorkTypes: vi.fn(),
  listCompetitionEventWorkTypeLevels: vi.fn(),
  listCompetitionScoreRules: vi.fn(),
  listCompetitionTheoryMonitor: vi.fn(),
  listCompetitionTheoryPaperSnapshots: vi.fn(),
  listCompetitionTheoryResults: vi.fn(),
  listCompetitionTheoryRooms: vi.fn(),
  listCompetitionTheorySchedules: vi.fn(),
  pageCompetitionStages: vi.fn(),
  performCompetitionTheoryMonitorAction: vi.fn(),
  publishCompetitionTheorySchedule: vi.fn(),
  randomAllocateCompetitionTheorySchedule: vi.fn(),
  saveCompetitionTheoryRoom: vi.fn(),
  saveCompetitionTheorySession: vi.fn(),
  withdrawCompetitionTheorySchedule: vi.fn(),
}));

vi.mock('../src/api/competitionQuestion', () => ({
  listCompetitionMakeExamSnapshotsForScope: vi.fn(),
}));

describe('理论试卷工作台', () => {
  it('binds a competition paper by clicking its snapshot row without opening a selector modal', async () => {
    const user = userEvent.setup();
    vi.mocked(getCompetitionEvent).mockResolvedValue({ id: '20', name: '技能大赛' } as never);
    vi.mocked(pageCompetitionStages).mockResolvedValue({
      items: [{
        id: '10', eventId: '20', name: '初赛', stageType: 'preliminary', sortOrder: 1,
      }],
      page: 1, pageSize: 500, total: 1,
    } as never);
    vi.mocked(listCompetitionEventWorkTypes).mockResolvedValue([
      { id: '30', workTypeName: '电工', displayName: '电工' },
    ] as never);
    vi.mocked(listCompetitionEventWorkTypeLevels).mockResolvedValue([
      { id: '40', levelCode: '3', levelName: '高级工' },
    ] as never);
    vi.mocked(listCompetitionTheoryPaperSnapshots).mockResolvedValue([]);
    vi.mocked(listCompetitionMakeExamSnapshotsForScope).mockResolvedValue([{
      id: '500', planId: '600', eventId: '20', eventWorkTypeId: '30', eventWorkTypeLevelId: '40',
      contentType: 'theory', paperCode: 'A', versionNo: 1, title: 'A卷计划', durationMinutes: 60,
      questionCount: 10, totalScore: '100', passScore: '60', status: 'published', questions: [],
    }] as never);
    vi.mocked(importCompetitionTheoryPaperSnapshots).mockResolvedValue([] as never);

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <CompetitionTheoryExamPage
          eventId="20"
          embedded
          onNavigate={() => undefined}
          initialStageId="10"
          initialEventWorkTypeId="30"
          initialEventWorkTypeLevelCode="3"
        />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('A 卷')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '选择试卷' })).not.toBeInTheDocument();
    await user.click(screen.getByText('A 卷').closest('tr')!);
    await waitFor(() => expect(importCompetitionTheoryPaperSnapshots).toHaveBeenCalledWith('20', {
      stageId: '10',
      eventWorkTypeId: '30',
      eventWorkTypeLevelId: '40',
      paperSnapshotIds: ['500'],
    }));
    client.clear();
  });
});
