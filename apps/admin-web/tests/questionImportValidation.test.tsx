import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import QuestionImportModal from '../src/pages/exam/QuestionImportModal';

const api = vi.hoisted(() => ({
  validateExamDetailsImport: vi.fn(),
  importExamDetails: vi.fn(),
}));

vi.mock('../src/api/exam', () => api);

const HARD_ERROR = {
  fileSha256: 'a'.repeat(64),
  totalRows: 2,
  validRows: 1,
  hardErrorCount: 1,
  warningCount: 0,
  errors: [
    {
      severity: 'error',
      ruleCode: 'EXCEL_ERROR_LITERAL',
      sheet: 'Sheet1',
      row: 4,
      column: 3,
      field: '选项A',
      rawValue: '#REF!',
      matchedText: '#REF!',
      message: '该单元格包含表格错误值 #REF!，请修正后重新导入',
    },
  ],
  warnings: [],
  truncated: false,
};

const WARNING_ONLY = {
  ...HARD_ERROR,
  hardErrorCount: 0,
  warningCount: 1,
  errors: [],
  warnings: [
    {
      severity: 'warning',
      ruleCode: 'EXCEL_SCIENTIFIC_NOTATION_SUSPECT',
      sheet: 'Sheet1',
      row: 3,
      column: 3,
      field: '选项A',
      rawValue: '1.2E+3',
      matchedText: '1.2E+3',
      message: '该处疑似被 Excel 转成了科学计数法，请核对原始数值',
    },
  ],
};

function renderModal() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <ConfigProvider locale={zhCN}>
      <QueryClientProvider client={client}>
        <QuestionImportModal
          open
          courseId={9}
          source={0}
          sourceLabel="内部"
          onCancel={() => undefined}
        />
      </QueryClientProvider>
    </ConfigProvider>,
  );
}

function uploadFile(name = '试题.xlsx') {
  const input = document.querySelector('input[type="file"]') as HTMLInputElement;
  fireEvent.change(input, {
    target: {
      files: [new File(['xlsx-bytes'], name,
        { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })],
    },
  });
}

describe('QuestionImportModal 导入预检', () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset());
    api.importExamDetails.mockResolvedValue({
      fileSha256: 'b'.repeat(64),
      importedCount: 398,
      warningCount: 6,
      warnings: [],
      truncated: false,
    });
  });

  it('hard error 展示行列并禁用导入按钮', async () => {
    api.validateExamDetailsImport.mockResolvedValue(HARD_ERROR);
    renderModal();
    uploadFile();
    await userEvent.click(screen.getByRole('button', { name: '检查文件格式' }));
    expect(await screen.findByText(/格式错误 1 处/)).toBeInTheDocument();
    expect(screen.getByText(/第 4 行第 3 列/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '开始导入' })).toBeDisabled();
  });

  it('warning 必须先勾选已知悉；导入携带预检哈希并消费新返回契约', async () => {
    api.validateExamDetailsImport.mockResolvedValue(WARNING_ONLY);
    renderModal();
    uploadFile();
    await userEvent.click(screen.getByRole('button', { name: '检查文件格式' }));

    expect(await screen.findByText(/提醒 1 条/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '开始导入' })).toBeDisabled();

    await userEvent.click(screen.getByRole('checkbox', { name: '已查看以上格式提醒，继续导入' }));
    expect(screen.getByRole('button', { name: '开始导入' })).toBeEnabled();

    await userEvent.click(screen.getByRole('button', { name: '开始导入' }));
    await waitFor(() => expect(api.importExamDetails).toHaveBeenCalledTimes(1));
    const formData = api.importExamDetails.mock.calls[0][0] as FormData;
    expect(formData.get('validationFileSha256')).toBe(WARNING_ONLY.fileSha256);
    expect(formData.get('courseId')).toBe('9');
    expect(formData.get('source')).toBe('0');
    expect(await screen.findByText(/成功导入 398 道试题/)).toBeInTheDocument();
  });

  it('改选另一个文件会清空预检状态并解除禁用', async () => {
    api.validateExamDetailsImport.mockResolvedValue(HARD_ERROR);
    renderModal();
    uploadFile();
    await userEvent.click(screen.getByRole('button', { name: '检查文件格式' }));
    expect(await screen.findByText(/格式错误 1 处/)).toBeInTheDocument();

    uploadFile('另一个文件.xlsx');
    await waitFor(() =>
      expect(screen.queryByText(/格式错误 1 处/)).not.toBeInTheDocument(),
    );
    expect(screen.getByRole('button', { name: '开始导入' })).toBeEnabled();
    expect(api.importExamDetails).not.toHaveBeenCalled();
  });

  it('未选文件时不发请求并提示', async () => {
    renderModal();
    await userEvent.click(screen.getByRole('button', { name: '检查文件格式' }));
    expect(await screen.findByText('请选择要导入的文件')).toBeInTheDocument();
    expect(api.validateExamDetailsImport).not.toHaveBeenCalled();
  });
});
