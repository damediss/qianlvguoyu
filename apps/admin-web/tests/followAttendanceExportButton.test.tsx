import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AttendanceExportButton } from '../src/pages/training/follow/AttendanceExportButton';

const api = vi.hoisted(() => ({
  exportClassAttendances: vi.fn(),
}));

vi.mock('../src/api/training', async (importOriginal) => ({
  ...(await importOriginal<object>()),
  exportClassAttendances: api.exportClassAttendances,
}));

let permittedCodes: string[] = [];

vi.mock('../src/stores/authStore', () => ({
  useAuthStore: (selector: (s: { hasPermission: (code: string) => boolean }) => unknown) =>
    selector({ hasPermission: (code: string) => permittedCodes.includes(code) }),
}));

describe('AttendanceExportButton（跟班考勤 导出考勤表）', () => {
  beforeEach(() => {
    permittedCodes = ['train:attendance:export'];
    api.exportClassAttendances.mockReset();
    api.exportClassAttendances.mockResolvedValue(undefined);
  });

  it('F1 有权限且有学员：按钮可点，点击触发导出', async () => {
    render(<AttendanceExportButton classId={101} studentTotal={12} />);

    const btn = screen.getByRole('button', { name: /导出考勤表/ });
    expect(btn).toBeEnabled();

    await userEvent.click(btn);
    expect(api.exportClassAttendances).toHaveBeenCalledWith(101);
  });

  it('F2 无 train:attendance:export 权限：按钮不渲染', () => {
    permittedCodes = [];
    render(<AttendanceExportButton classId={101} studentTotal={12} />);

    expect(screen.queryByRole('button', { name: /导出考勤表/ })).toBeNull();
  });

  it('F3 班级无学员：按钮禁用，悬浮提示班级暂无学员', () => {
    render(<AttendanceExportButton classId={101} studentTotal={0} />);

    const btn = screen.getByRole('button', { name: /导出考勤表/ });
    expect(btn).toBeDisabled();
  });

  it('F3b classId 为空：按钮禁用', () => {
    render(<AttendanceExportButton classId={null} studentTotal={12} />);

    expect(screen.getByRole('button', { name: /导出考勤表/ })).toBeDisabled();
    expect(api.exportClassAttendances).not.toHaveBeenCalled();
  });
});
