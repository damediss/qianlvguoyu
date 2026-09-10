import { render, screen, waitFor, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CompetitionEventFormPage, CompetitionEventPublishButton } from '../src/pages/competition/CompetitionEventManagementPage';
import { createCompetitionEvent, publishCompetitionEvent } from '../src/api/competition';

const auth = vi.hoisted(() => ({ permissionCodes: ['competition:event:view', 'competition:event:create', 'competition:event:publish'] }));
vi.mock('../src/stores/authStore', () => ({ useAuthStore: (select: (state: unknown) => unknown) => select(auth) }));
vi.mock('../src/api/competition', () => ({ createCompetitionEvent: vi.fn(), publishCompetitionEvent: vi.fn() }));
vi.mock('../src/pages/competition/CompetitionEventModulePage', () => ({ CompetitionEventModulePage: () => null }));
vi.mock('../src/pages/competition/CompetitionNoticePage', () => ({ CompetitionNoticePage: () => null }));
vi.mock('../src/pages/competition/CompetitionTheoryExamPage', () => ({ CompetitionTheoryExamPage: () => null }));
vi.mock('../src/pages/competition/CompetitionPlayerManagementPage', () => ({ CompetitionPlayerManagementPage: () => null }));

function mount(element: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}>{element}</QueryClientProvider>);
}
beforeEach(() => {
  vi.clearAllMocks();
  auth.permissionCodes = ['competition:event:view', 'competition:event:create', 'competition:event:publish'];
  vi.mocked(createCompetitionEvent).mockResolvedValue({ id: '100', status: 'draft' } as never);
  vi.mocked(publishCompetitionEvent).mockResolvedValue({ eventId: '100', status: 'published', stagePlanStatus: 'draft' } as never);
});
afterEach(cleanup);

describe('赛事基础信息发布', () => {
  it('saves optional-date basic information then publishes without stage configuration', async () => {
    const user = userEvent.setup();
    const navigate = vi.fn();
    mount(<CompetitionEventFormPage onNavigate={navigate} />);
    await user.type(screen.getByPlaceholderText('请输入赛事名称'), '测试竞赛');
    await user.type(screen.getByPlaceholderText('请输入唯一赛事编码'), 'c007-test');
    await user.click(screen.getByRole('button', { name: '保存并发布赛事' }));
    await waitFor(() => expect(publishCompetitionEvent).toHaveBeenCalledWith('100'));
    expect(createCompetitionEvent).toHaveBeenCalledWith(expect.objectContaining({ name: '测试竞赛', code: 'c007-test', startAt: undefined, endAt: undefined }));
    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/competition/events/100'));
  });

  it('keeps a saved draft reachable when publishing fails', async () => {
    vi.mocked(publishCompetitionEvent).mockRejectedValue(new Error('发布失败'));
    const navigate = vi.fn();
    const user = userEvent.setup();
    mount(<CompetitionEventFormPage onNavigate={navigate} />);
    await user.type(screen.getByPlaceholderText('请输入赛事名称'), '测试竞赛');
    await user.type(screen.getByPlaceholderText('请输入唯一赛事编码'), 'c007-test');
    await user.click(screen.getByRole('button', { name: '保存并发布赛事' }));
    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/competition/events/100'));
    expect(createCompetitionEvent).toHaveBeenCalledTimes(1);
  });

  it('offers draft saving without publish permission', () => {
    auth.permissionCodes = ['competition:event:view', 'competition:event:create'];
    mount(<CompetitionEventFormPage onNavigate={vi.fn()} />);
    expect(screen.queryByRole('button', { name: '保存并发布赛事' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '保存赛事信息' })).toBeInTheDocument();
  });

  it('requires an explicit confirmation before publishing an existing draft', async () => {
    const user = userEvent.setup();
    mount(<CompetitionEventPublishButton eventId="100" />);
    await user.click(screen.getByRole('button', { name: '发布赛事' }));
    expect(publishCompetitionEvent).not.toHaveBeenCalled();
    const buttons = await screen.findAllByRole('button', { name: '发布赛事' });
    await user.click(buttons[buttons.length - 1]);
    await waitFor(() => expect(publishCompetitionEvent).toHaveBeenCalledWith('100'));
  });
});
