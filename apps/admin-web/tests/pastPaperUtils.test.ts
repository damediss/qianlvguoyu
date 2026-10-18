import { describe, expect, it } from 'vitest';
import {
  buildZhentiQuery,
  canRetryImportRow,
  extractErrorMessage,
  formatFileSize,
  validatePdfFile,
  validateZipFile,
  zhentiStatusTag,
} from '../src/pages/exam/pastPaperUtils';

describe('pastPaperUtils', () => {
  describe('formatFileSize', () => {
    it('B/KB/MB 边界与一位小数', () => {
      expect(formatFileSize(0)).toBe('0 B');
      expect(formatFileSize(1023)).toBe('1023 B');
      expect(formatFileSize(1024)).toBe('1.0 KB');
      expect(formatFileSize(1536)).toBe('1.5 KB');
      expect(formatFileSize(1024 * 1024 - 1)).toBe('1024.0 KB');
      expect(formatFileSize(1024 * 1024)).toBe('1.0 MB');
      expect(formatFileSize(3.5 * 1024 * 1024)).toBe('3.5 MB');
    });

    it('空值显示 -', () => {
      expect(formatFileSize(null)).toBe('-');
      expect(formatFileSize(undefined)).toBe('-');
      expect(formatFileSize(-1)).toBe('-');
    });
  });

  describe('buildZhentiQuery', () => {
    it('未选考试项目返回 null', () => {
      expect(buildZhentiQuery({ page: 1, pageSize: 20, statusFilter: 'all' })).toBeNull();
    });

    it('空筛选值不落参', () => {
      expect(
        buildZhentiQuery({
          examId: '100',
          page: 2,
          pageSize: 50,
          keyword: '   ',
          year: '',
          statusFilter: 'all',
        }),
      ).toEqual({ examId: '100', page: 2, pageSize: 50 });
    });

    it('关键字去空格、年份与状态正确落参', () => {
      expect(
        buildZhentiQuery({
          examId: '100',
          page: 1,
          pageSize: 20,
          keyword: ' 电解 ',
          year: '2024',
          statusFilter: '0',
        }),
      ).toEqual({ examId: '100', page: 1, pageSize: 20, keyword: '电解', year: '2024', status: 0 });
    });

    it('待完善筛选映射为 metaPending=1', () => {
      expect(
        buildZhentiQuery({ examId: '100', page: 1, pageSize: 20, statusFilter: 'pending' }),
      ).toEqual({ examId: '100', page: 1, pageSize: 20, metaPending: 1 });
    });
  });

  describe('上传预检', () => {
    it('PDF 扩展名与 50MB 上限', () => {
      expect(validatePdfFile({ name: 'a.pdf', size: 1024 })).toBeNull();
      expect(validatePdfFile({ name: 'a.PDF', size: 1024 })).toBeNull();
      expect(validatePdfFile({ name: 'a.exe', size: 1024 })).toBe('仅支持 PDF 文件');
      expect(validatePdfFile({ name: 'a.pdf', size: 0 })).toBe('文件为空，请重新选择');
      expect(validatePdfFile({ name: 'a.pdf', size: 50 * 1024 * 1024 + 1 })).toBe(
        '单个 PDF 不能超过 50MB',
      );
      expect(validatePdfFile({ name: 'a.pdf', size: 50 * 1024 * 1024 })).toBeNull();
    });

    it('zip 扩展名与 200MB 上限', () => {
      expect(validateZipFile({ name: 'a.zip', size: 1024 })).toBeNull();
      expect(validateZipFile({ name: 'a.rar', size: 1024 })).toBe('仅支持 zip 压缩包');
      expect(validateZipFile({ name: 'a.zip', size: 200 * 1024 * 1024 + 1 })).toBe(
        'zip 压缩包不能超过 200MB',
      );
    });
  });

  describe('状态与补录', () => {
    it('待完善优先于上下架', () => {
      expect(zhentiStatusTag({ status: 1, metaPending: 1 })).toEqual({
        text: '待完善',
        color: 'orange',
      });
      expect(zhentiStatusTag({ status: 1, metaPending: 0 })).toEqual({
        text: '上架',
        color: 'green',
      });
      expect(zhentiStatusTag({ status: 0, metaPending: 0 })).toEqual({
        text: '下架',
        color: 'default',
      });
    });

    it('只有已入库失败行可补录', () => {
      expect(canRetryImportRow({ fileName: 'a.pdf', ok: false, id: '1' })).toBe(true);
      expect(canRetryImportRow({ fileName: 'a.exe', ok: false })).toBe(false);
      expect(canRetryImportRow({ fileName: 'a.pdf', ok: true, id: '1' })).toBe(false);
    });
  });

  it('extractErrorMessage 优先后端 message', () => {
    expect(extractErrorMessage({ response: { data: { message: '已存在相同文件：A' } } })).toBe(
      '已存在相同文件：A',
    );
    expect(extractErrorMessage(new Error('网络错误'), '兜底')).toBe('网络错误');
    expect(extractErrorMessage({}, '兜底')).toBe('兜底');
  });
});
