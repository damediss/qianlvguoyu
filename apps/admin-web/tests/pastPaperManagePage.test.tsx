import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PastPaperManagePage } from '../src/pages/exam/PastPaperManagePage';

const api = vi.hoisted(() => ({
  pageZhentis: vi.fn(),
  previewZhenti: vi.fn(),
  updateZhenti: vi.fn(),
  deleteZhenti: vi.fn(),
  uploadZhenti: vi.fn(),
  batchImportZhentis: vi.fn(),
  getZhentiImportLog: vi.fn(),
}));

const trainingApi = vi.hoisted(() => ({
  pageExams: vi.fn(),
}));

vi.mock('../src/api/exam-zhenti', () => api);
vi.mock('../src/api/training', () => trainingApi);

let permittedCodes: string[] = [];

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (selector: (s: { hasPermission: (code: string) => boolean }) => unknown) =>
    selector({ hasPermission: (code: string) => permittedCodes.includes(code) }),
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <PastPaperManagePage />
    </QueryClientProvider>,
  );
}

const RECORD = {
  id: '9001',
  examId: '100',
  title: '铝电解工（铝及铝合金熔铸工）五级理论科目',
  year: '2024',
  fileSize: 1835008,
  sort: 0,
  status: 1,
  metaPending: 0,
  createdBy: '张三',
  createdById: '1',
  createdAt: '2026-10-18 10:00:00',
};

async function selectAntOption(comboboxIndex: number, optionTitle: string) {
  await userEvent.click(screen.getAllByRole('combobox')[comboboxIndex]);
  const option = await waitFor(() => {
    const element = document.querySelector(
      `.ant-select-item-option[title="${optionTitle}"]`,
    ) as HTMLElement | null;
    if (!element) {
      throw new Error(`option ${optionTitle} not ready`);
    }
    return element;
  });
  await userEvent.click(option);
}

async function selectExam() {
  await selectAntOption(0, '铝电解工五级');
  await waitFor(() => expect(api.pageZhentis).toHaveBeenCalled());
}

describe('PastPaperManagePage', () => {
  beforeEach(() => {
    permittedCodes = ['exam:zhenti:view', 'exam:zhenti:manage'];
    api.pageZhentis.mockReset().mockResolvedValue({
      records: [RECORD],
      total: 1,
      page: 1,
      size: 20,
      years: ['2024'],
    });
    api.previewZhenti.mockReset();
    api.updateZhenti.mockReset().mockResolvedValue({ id: '9001' });
    api.deleteZhenti.mockReset().mockResolvedValue(undefined);
    api.uploadZhenti.mockReset();
    api.batchImportZhentis.mockReset();
    api.getZhentiImportLog.mockReset();
    trainingApi.pageExams.mockReset().mockResolvedValue({
      items: [{ id: 100, title: '铝电解工五级' }],
      total: 1,
      page: 1,
      pageSize: 20,
    });
  });

  it('未选考试项目：空态提示 + 上传/导入按钮禁用', async () => {
    renderPage();
    expect(await screen.findByText('请先选择考试项目')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /上传 PDF/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /批量导入/ })).toBeDisabled();
  });

  it('选择考试项目后渲染列表与全部行操作', async () => {
    renderPage();
    await selectExam();

    expect(await screen.findByText('铝电解工（铝及铝合金熔铸工）五级理论科目')).toBeInTheDocument();
    expect(screen.getByText('1.8 MB')).toBeInTheDocument();
    expect(screen.getByText('上架')).toBeInTheDocument();
    expect(screen.getByText('张三')).toBeInTheDocument();

    for (const name of [/预\s*览/, /^重命名$/, /^改年份$/, /^下\s*架$/, /删\s*除/]) {
      expect(screen.getByRole('button', { name })).toBeInTheDocument();
    }
    expect(api.pageZhentis).toHaveBeenCalledWith({
      examId: '100',
      page: 1,
      pageSize: 20,
    });
  });

  it('待完善筛选触发 metaPending=1 的请求', async () => {
    renderPage();
    await selectExam();

    await selectAntOption(2, '待完善');

    await waitFor(() =>
      expect(api.pageZhentis).toHaveBeenLastCalledWith({
        examId: '100',
        page: 1,
        pageSize: 20,
        metaPending: 1,
      }),
    );
  });

  it('409 冲突在弹窗内展示后端 msg', async () => {
    api.uploadZhenti.mockRejectedValue({
      response: { data: { message: '已存在相同文件：已有真题' } },
    });
    renderPage();
    await selectExam();

    await userEvent.click(screen.getByRole('button', { name: /上传 PDF/ }));
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(
      input,
      new File(['%PDF-1.4 test'], '真题-2024.pdf', { type: 'application/pdf' }),
    );
    await userEvent.click(screen.getByRole('button', { name: /^上\s*传$/ }));

    expect(await screen.findByText('已存在相同文件：已有真题')).toBeInTheDocument();
  });

  it('仅查看权限：查看类操作存在，管理类按钮不渲染', async () => {
    permittedCodes = ['exam:zhenti:view'];
    renderPage();
    await selectExam();

    expect(await screen.findByRole('button', { name: /预\s*览/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^重命名$/ })).toBeNull();
    expect(screen.queryByRole('button', { name: /删\s*除/ })).toBeNull();
    expect(screen.getByText(/当前账号仅有查看权限/)).toBeInTheDocument();
  });
});
