import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FaqConfigDialog } from '../src/pages/exam/FaqConfigDialog';

const api = vi.hoisted(() => ({
  pageFaqModelConfigs: vi.fn(),
  createFaqModelConfig: vi.fn(),
  updateFaqModelConfig: vi.fn(),
  deleteFaqModelConfig: vi.fn(),
  getFaqWeknoraConfig: vi.fn(),
  saveFaqWeknoraConfig: vi.fn(),
}));

vi.mock('../src/api/exam', () => api);

const JEV_ROW = {
  id: '202610290101',
  modelCode: 'OBJECTIVE_ANSWER_CHECK_JEV_MODEL',
  modelName: '客观题答案检查 JEV',
  baseUrl: 'https://tokendance.space/gateway',
  apiKeyMasked: '配置文件提供',
  apiFormat: 'custom',
  featureScope: 'objective-answer-check',
  modelId: 'ateve-jev-v1',
  temperature: 0.7,
  maxTokens: 2000,
  extraConfig: null,
  status: 'disabled',
  jevConcurrency: 5,
  jevReadTimeoutSeconds: 30,
  createdAt: '2026-10-29 10:00:00',
  updatedAt: '2026-10-29 10:00:00',
};

describe('FaqConfigDialog v1.5 专用 JEV 配置行', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.pageFaqModelConfigs.mockResolvedValue({
      items: [JEV_ROW],
      page: 1,
      pageSize: 50,
      total: 1,
    });
    api.getFaqWeknoraConfig.mockResolvedValue({
      id: null,
      webhookSecretMasked: null,
      enabledEvents: null,
      knowledgeBaseId: null,
      faqSetId: null,
      webhookEnabled: 0,
      autoExport: 0,
      createdAt: null,
      updatedAt: null,
    });
    api.updateFaqModelConfig.mockResolvedValue({ ...JEV_ROW, status: 'enabled' });
  });

  it('列表显示适用范围与配置文件提供，编辑时禁用 API Key 并显示 JEV 参数', async () => {
    const user = userEvent.setup();
    render(<FaqConfigDialog open onClose={() => {}} />);

    await user.click(await screen.findByRole('tab', { name: '模型管理' }), { timeout: 30000 });
    await screen.findByText('OBJECTIVE_ANSWER_CHECK_JEV_MODEL');
    expect(screen.getByText('objective-answer-check')).toBeTruthy();
    expect(screen.getByText('配置文件提供')).toBeTruthy();
    expect(screen.queryByRole('button', { name: /delete\s*删除/ })).toBeNull();

    await user.click(screen.getAllByRole('button', { name: /edit\s*编辑/ })[0], { timeout: 30000 });

    expect(await screen.findByText('JEV 派发并发')).toBeTruthy();
    expect(screen.getByText('JEV 读超时（秒）')).toBeTruthy();
    const keyInput = screen.getByPlaceholderText('配置文件提供') as HTMLInputElement;
    expect(keyInput.disabled).toBe(true);
  }, 60000);

  it('保存专用行时不提交 apiKey，只提交非敏感运行参数', async () => {
    const user = userEvent.setup();
    render(<FaqConfigDialog open onClose={() => {}} />);

    await user.click(await screen.findByRole('tab', { name: '模型管理' }), { timeout: 30000 });
    await screen.findByText('OBJECTIVE_ANSWER_CHECK_JEV_MODEL');
    await user.click(screen.getAllByRole('button', { name: /edit\s*编辑/ })[0], { timeout: 30000 });
    await screen.findByText('JEV 派发并发');
    const ok = document.querySelector('.ant-modal-footer .ant-btn-primary') as HTMLButtonElement;
    expect(ok).toBeTruthy();
    await user.click(ok, { timeout: 30000 });

    await waitFor(() => expect(api.updateFaqModelConfig).toHaveBeenCalledTimes(1));
    const payload = api.updateFaqModelConfig.mock.calls[0][1] as Record<string, unknown>;
    expect(payload).not.toHaveProperty('apiKey');
    expect(payload.jevConcurrency).toBe(5);
    expect(payload.jevReadTimeoutSeconds).toBe(30);
  }, 60000);
});
