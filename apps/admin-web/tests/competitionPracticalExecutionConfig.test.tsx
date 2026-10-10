import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getCompetitionPracticalExecutionConfig,
  listCompetitionPracticalScoringSchemes,
  saveCompetitionPracticalExecutionConfig,
  validateCompetitionPracticalExecutionConfig,
} from '../src/api/competitionPracticalExecutionConfig';
import {
  buildPracticalRulePreview,
  CompetitionPracticalExecutionConfigPanel,
} from '../src/pages/competition/CompetitionPracticalExecutionConfigPanel';
import type {
  CompetitionPracticalExecutionConfig,
  CompetitionPracticalScoringScheme,
} from '../src/types/competitionPracticalExecutionConfig';

vi.mock('../src/api/competitionPracticalExecutionConfig', () => ({
  getCompetitionPracticalExecutionConfig: vi.fn(),
  listCompetitionPracticalScoringSchemes: vi.fn(),
  saveCompetitionPracticalExecutionConfig: vi.fn(),
  validateCompetitionPracticalExecutionConfig: vi.fn(),
}));

const scheme: CompetitionPracticalScoringScheme = {
  id: '301',
  sourceEventId: '201',
  sourceEventWorkTypeId: '31',
  sourceEventWorkTypeLevelId: '41',
  title: '电工实操评分方案',
  paperCode: 'P1',
  versionNo: 1,
  durationMinutes: 90,
  questionCount: 4,
  totalScore: 100,
};

const config: CompetitionPracticalExecutionConfig = {
  id: '501',
  eventId: '20',
  stageId: '10',
  stageContentId: '11',
  eventWorkTypeId: '30',
  eventWorkTypeLevelId: '40',
  scoringSchemeId: '301',
  examName: '初赛电工实操',
  durationSeconds: 5400,
  timeUnit: 'SECONDS',
  startMode: 'UNIFIED_START',
  allowEarlySubmit: false,
  timeScoringEnabled: true,
  overtimeRuleType: 'STEP_DEDUCTION',
  overtimeStepSeconds: 60,
  overtimeScorePerStep: 1,
  overtimeMaxDeduction: 10,
  earlyFinishRuleType: 'STEP_BONUS',
  earlyFinishStepSeconds: 300,
  earlyFinishScorePerStep: 1,
  earlyFinishMaxBonus: 5,
  overtimeAction: 'AUTO_FINISH',
  maxDurationSeconds: 6000,
  finalScoreUpperLimit: 100,
  standardTotalScore: 100,
  version: 1,
  scoringScheme: scheme,
};

function renderPanel() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <CompetitionPracticalExecutionConfigPanel
        eventId="400"
        stageId="10"
        stageName="初赛"
        eventWorkTypeId="30"
        eventWorkTypeLevelId="40"
        editable
      />
    </QueryClientProvider>,
  );
}

describe('赛段实操运行规则配置', () => {
  beforeEach(() => {
    vi.mocked(getCompetitionPracticalExecutionConfig).mockResolvedValue(null);
    vi.mocked(listCompetitionPracticalScoringSchemes).mockResolvedValue([scheme]);
    vi.mocked(saveCompetitionPracticalExecutionConfig).mockResolvedValue(config);
    vi.mocked(validateCompetitionPracticalExecutionConfig).mockResolvedValue({
      valid: true,
      errors: [],
      message: '实操配置校验通过',
    });
  });

  it('loads saved settings and shows their human-readable rule preview', async () => {
    vi.mocked(getCompetitionPracticalExecutionConfig).mockResolvedValue(config);
    renderPanel();

    expect(await screen.findByDisplayValue('初赛电工实操')).toBeInTheDocument();
    expect(screen.getByTestId('practical-standard-total-score')).toHaveTextContent('100 分');
    expect(screen.getByText('超过标准时间后，每超1分钟扣1分，最大扣10分；不足一步按整步向上取整。')).toBeInTheDocument();
    expect(screen.getByText('达到100分钟后由服务端自动结束考试。')).toBeInTheDocument();
  });

  it('hides time score controls when time scoring is disabled but keeps auto-finish controls', async () => {
    const user = userEvent.setup();
    renderPanel();

    const timeSwitch = await screen.findByRole('switch', { name: '时间参与成绩' });
    await user.click(timeSwitch);
    expect(screen.getByText('超时规则')).toBeInTheDocument();
    await user.click(timeSwitch);

    expect(screen.queryByText('超时规则')).not.toBeInTheDocument();
    expect(screen.getByText('超时后允许继续考试')).toBeInTheDocument();
    expect(screen.getByText('实际用时只记录，不参与最终成绩计算。')).toBeInTheDocument();
    await user.click(screen.getByLabelText('达到最大允许时间后自动结束'));
    expect(screen.getByRole('spinbutton', { name: '最大允许作答时间（分钟）' })).toBeInTheDocument();
  });

  it('saves the form with seconds as the API unit and restores values after refresh', async () => {
    const user = userEvent.setup();
    vi.mocked(getCompetitionPracticalExecutionConfig).mockResolvedValue(config);
    renderPanel();

    await waitFor(() => expect(screen.getByTestId('practical-runtime-save')).not.toBeDisabled());
    await user.clear(screen.getByRole('spinbutton', { name: '标准考核时长（分钟）' }));
    await user.type(screen.getByRole('spinbutton', { name: '标准考核时长（分钟）' }), '91');
    await user.click(screen.getByTestId('practical-runtime-save'));

    await waitFor(() => expect(saveCompetitionPracticalExecutionConfig).toHaveBeenCalledWith('10',
      expect.objectContaining({ scoringSchemeId: '301', durationSeconds: 5460 })));
    expect(await screen.findByText('达到100分钟后由服务端自动结束考试。')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('runtime-config-refresh'));
    await waitFor(() => expect(getCompetitionPracticalExecutionConfig).toHaveBeenCalledTimes(3));
    expect(await screen.findByDisplayValue('初赛电工实操')).toBeInTheDocument();
    expect(screen.getByRole('spinbutton', { name: '标准考核时长（分钟）' })).toHaveValue('90');
  });

  it('runs server validation from the current form', async () => {
    const user = userEvent.setup();
    vi.mocked(getCompetitionPracticalExecutionConfig).mockResolvedValue(config);
    renderPanel();

    await user.click(await screen.findByTestId('practical-runtime-validate'));
    await waitFor(() => expect(validateCompetitionPracticalExecutionConfig).toHaveBeenCalledWith('10',
      expect.objectContaining({ durationSeconds: 5400, scoringSchemeId: '301' })));
  });

  it('blocks saving when the scoring scheme is missing', async () => {
    const user = userEvent.setup();
    renderPanel();

    await waitFor(() => expect(screen.getByTestId('practical-runtime-save')).not.toBeDisabled());
    await user.click(screen.getByTestId('practical-runtime-save'));

    expect(await screen.findByText('请选择已发布的实操评分方案')).toBeInTheDocument();
    expect(saveCompetitionPracticalExecutionConfig).not.toHaveBeenCalled();
  });

  it('defaults the final score ceiling to the selected scheme total', async () => {
    const user = userEvent.setup();
    renderPanel();

    await user.click(screen.getByRole('combobox', { name: '绑定实操评分方案' }));
    await user.click(await screen.findByText(/电工实操评分方案 P1卷 v1/));
    await user.click(screen.getByTestId('practical-runtime-save'));

    await waitFor(() => expect(saveCompetitionPracticalExecutionConfig).toHaveBeenCalledWith('10',
      expect.objectContaining({ scoringSchemeId: '301', finalScoreUpperLimit: 100 })));
  });

  it('shows a recoverable error when APIs fail', async () => {
    vi.mocked(getCompetitionPracticalExecutionConfig).mockRejectedValue(new Error('network down'));
    renderPanel();

    expect(await screen.findByText('实操运行规则加载失败')).toBeInTheDocument();
    expect(screen.getByText('network down')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '重新加载' })).toBeInTheDocument();
  });

  it('previews disabled time scoring without applying bonus or deduction language', () => {
    const preview = buildPracticalRulePreview({
      durationMinutes: 90,
      timeScoringEnabled: false,
      overtimeAction: 'AUTO_FINISH',
      maxDurationMinutes: 100,
    }, scheme);

    expect(preview).toContain('实际用时只记录，不参与最终成绩计算。');
    expect(preview).toContain('达到100分钟后由服务端自动结束考试。');
    expect(preview.join('')).not.toContain('超时扣分');
  });
});
