import { Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  createExamStaffInfo,
  deleteExamStaffInfo,
  disableExamStaffInfo,
  enableExamStaffInfo,
  pageExamStaffInfo,
  reviewExamStaffInfo,
  submitExamStaffInfo,
  updateExamStaffInfo,
} from '../../../api/certificationInfo';
import type { ExamStaffInfo, ExamStaffInfoPayload } from '../../../types/certificationInfo';
import { InfoManagementTable } from './InfoManagementTable';

const columns: ColumnsType<ExamStaffInfo> = [
  { title: '姓名', dataIndex: 'staffName', width: 140 },
  {
    title: '任务编号',
    dataIndex: 'assignedTaskNos',
    width: 180,
    ellipsis: true,
    render: (value?: string) => value || '-',
  },
  { title: '在岗状态', dataIndex: 'workStatus', width: 130, render: (value) => <Tag>{statusLabel(value)}</Tag> },
  { title: '联系电话', dataIndex: 'maskedPhone', width: 160, render: (value) => value || '-' },
];

export function ExamStaffInfoPage() {
  return (
    <InfoManagementTable<ExamStaffInfo, ExamStaffInfoPayload>
      title="考务信息"
      queryKey="certification-info-exam-staff"
      permissionPrefix="certification.info.exam-staff"
      rowKey="id"
      statusField="staffName"
      columns={columns}
      auditColumnIndex={2}
      fields={[
        { name: 'staffName', label: '姓名', required: true },
        { name: 'phone', label: '联系电话', required: true },
      ]}
      statusOptions={[
        { label: '空闲', value: 'idle' },
        { label: '任务中', value: 'assigned' },
      ]}
      pageApi={pageExamStaffInfo}
      createApi={createExamStaffInfo}
      updateApi={updateExamStaffInfo}
      submitApi={submitExamStaffInfo}
      reviewApi={reviewExamStaffInfo}
      disableApi={disableExamStaffInfo}
      enableApi={enableExamStaffInfo}
      deleteApi={deleteExamStaffInfo}
    />
  );
}

function statusLabel(value: string) {
  return {
    idle: '空闲',
    assigned: '任务中',
    disabled: '停用',
  }[value] || value || '-';
}
