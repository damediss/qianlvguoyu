import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ZhentiImportModal } from '../src/pages/exam/components/ZhentiImportModal';

const api = vi.hoisted(() => ({
  batchImportZhentis: vi.fn(),
  getZhentiImportLog: vi.fn(),
  updateZhenti: vi.fn(),
}));

vi.mock('../src/api/exam-zhenti', () => api);

describe('ZhentiImportModal', () => {
  beforeEach(() => {
    api.batchImportZhentis.mockReset();
    api.getZhentiImportLog.mockReset();
    api.updateZhenti.mockReset().mockResolvedValue({ id: '9002' });
  });

  async function submitZip() {
    render(
      <ZhentiImportModal
        open
        examId="100"
        examName="铝电解工五级"
        onCancel={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(input, new File(['zip'], '真题.zip', { type: 'application/zip' }));
    await userEvent.click(screen.getByRole('button', { name: '开始导入' }));
  }

  it('同步结果：成功行与待补录行分别渲染，补录后重试调 D4', async () => {
    api.batchImportZhentis.mockResolvedValue({
      mode: 'sync',
      results: [
        {
          fileName: '铝电解工五级理论科目-2024.pdf',
          ok: true,
          id: '9001',
          title: '铝电解工五级理论科目',
          year: '2024',
        },
        {
          fileName: '无年份真题.pdf',
          ok: false,
          pending: true,
          id: '9002',
          title: '无年份真题',
          year: null,
          reason: '无法从文件名解析年份或工种等级，请补录标题与年份',
        },
      ],
    });

    await submitZip();

    expect(await screen.findByText('铝电解工五级理论科目-2024.pdf')).toBeInTheDocument();
    expect(screen.getByText('无年份真题.pdf')).toBeInTheDocument();
    expect(screen.getByText('成功')).toBeInTheDocument();
    expect(screen.getByText('待补录')).toBeInTheDocument();

    const yearInput = screen.getByPlaceholderText('年份');
    await userEvent.type(yearInput, '2024');
    await userEvent.click(screen.getByRole('button', { name: '补录后重试' }));

    await waitFor(() =>
      expect(api.updateZhenti).toHaveBeenCalledWith({
        id: '9002',
        title: '无年份真题',
        year: '2024',
      }),
    );
  });

  it('未入库失败行不可补录，提示重新上传', async () => {
    api.batchImportZhentis.mockResolvedValue({
      mode: 'sync',
      results: [{ fileName: 'evil.exe', ok: false, reason: '文件类型非法，仅支持 PDF' }],
    });

    await submitZip();

    expect(await screen.findByText(/该文件未入库，请重新上传/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '补录后重试' })).toBeNull();
  });

  it('前端拦截非 zip 文件且不发起请求', async () => {
    render(
      <ZhentiImportModal
        open
        examId="100"
        examName="铝电解工五级"
        onCancel={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, {
      target: { files: [new File(['rar'], '真题.rar', { type: 'application/octet-stream' })] },
    });

    expect(await screen.findByText('仅支持 zip 压缩包')).toBeInTheDocument();
    expect(api.batchImportZhentis).not.toHaveBeenCalled();
  });
});
