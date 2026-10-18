import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ZhentiUploadModal } from '../src/pages/exam/components/ZhentiUploadModal';

const api = vi.hoisted(() => ({
  uploadZhenti: vi.fn(),
}));

vi.mock('../src/api/exam-zhenti', () => api);

describe('ZhentiUploadModal', () => {
  beforeEach(() => {
    api.uploadZhenti.mockReset().mockResolvedValue({ id: '1' });
  });

  function renderModal(onSuccess = vi.fn()) {
    render(
      <ZhentiUploadModal
        open
        examId="100"
        examName="铝电解工五级"
        onCancel={vi.fn()}
        onSuccess={onSuccess}
      />,
    );
    return onSuccess;
  }

  it('非 PDF 被前端拦截且不发起请求', async () => {
    renderModal();
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(input, {
      target: { files: [new File(['MZ'], 'evil.exe', { type: 'application/octet-stream' })] },
    });

    expect(await screen.findByText('仅支持 PDF 文件')).toBeInTheDocument();
    expect(api.uploadZhenti).not.toHaveBeenCalled();
  });

  it('超过 50MB 被前端拦截', async () => {
    renderModal();
    const file = new File(['%PDF-1.4'], 'big.pdf', { type: 'application/pdf' });
    Object.defineProperty(file, 'size', { value: 50 * 1024 * 1024 + 1 });
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(input, file);

    expect(await screen.findByText('单个 PDF 不能超过 50MB')).toBeInTheDocument();
    expect(api.uploadZhenti).not.toHaveBeenCalled();
  });

  it('合法 PDF 提交时带上考试项目与手填年份', async () => {
    const onSuccess = renderModal();
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(
      input,
      new File(['%PDF-1.4 content'], '真题.pdf', { type: 'application/pdf' }),
    );
    await userEvent.type(screen.getByPlaceholderText('例如：2024'), '2024');
    await userEvent.click(screen.getByRole('button', { name: /^上\s*传$/ }));

    await waitFor(() => expect(api.uploadZhenti).toHaveBeenCalledTimes(1));
    const payload = api.uploadZhenti.mock.calls[0][0];
    expect(payload.examId).toBe('100');
    expect(payload.year).toBe('2024');
    expect(payload.file.name).toBe('真题.pdf');
    expect(onSuccess).toHaveBeenCalled();
  });
});
