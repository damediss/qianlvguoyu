import { useState } from 'react';
import { Button, Tooltip, message } from 'antd';
import { ExportOutlined } from '@ant-design/icons';
import * as trainApi from '../../../api/training';
import { useAuthStore } from '../../../stores/authStore';

/**
 * 跟班考勤页签「导出考勤表」按钮。
 * 权限 train:attendance:export 控制显隐；班级无学员时禁用。
 */
export function AttendanceExportButton({
  classId,
  studentTotal,
}: {
  classId: number | null;
  studentTotal: number;
}) {
  const hasPerm = useAuthStore((s) => s.hasPermission);
  const [exporting, setExporting] = useState(false);

  if (!hasPerm('train:attendance:export')) {
    return null;
  }

  const handleExport = async () => {
    if (classId == null) {
      return;
    }
    setExporting(true);
    try {
      await trainApi.exportClassAttendances(classId);
    } catch (e) {
      message.error((e as Error).message || '导出考勤表失败');
    } finally {
      setExporting(false);
    }
  };

  return (
    <Tooltip title={studentTotal === 0 ? '班级暂无学员' : undefined}>
      <Button
        icon={<ExportOutlined />}
        loading={exporting}
        disabled={classId == null || studentTotal === 0}
        onClick={handleExport}
      >
        导出考勤表
      </Button>
    </Tooltip>
  );
}
