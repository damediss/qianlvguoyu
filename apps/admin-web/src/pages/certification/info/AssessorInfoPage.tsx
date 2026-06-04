import { Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  createAssessorInfo,
  deleteAssessorInfo,
  disableAssessorInfo,
  enableAssessorInfo,
  pageAssessorInfo,
  reviewAssessorInfo,
  submitAssessorInfo,
  updateAssessorInfo,
} from '../../../api/certificationInfo';
import type { AssessorInfo, AssessorInfoPayload } from '../../../types/certificationInfo';
import { InfoManagementTable } from './InfoManagementTable';

const columns: ColumnsType<AssessorInfo> = [
  { title: '姓名', dataIndex: 'assessorName', width: 120 },
  { title: '在岗状态', dataIndex: 'workStatus', width: 130, render: (value) => <Tag>{statusLabel(value)}</Tag> },
  { title: '证号', dataIndex: 'certificateNo', width: 160 },
  { title: '工种', dataIndex: 'workTypeScope', width: 180 },
  { title: '等级范围', dataIndex: 'levelScope', width: 120 },
  { title: '有效期至', dataIndex: 'validUntil', width: 120 },
  { title: '联系电话', dataIndex: 'maskedPhone', width: 130 },
];

export function AssessorInfoPage() {
  return (
    <InfoManagementTable<AssessorInfo, AssessorInfoPayload>
      title="考评员信息"
      queryKey="certification-info-assessors"
      permissionPrefix="certification.info.assessor"
      rowKey="id"
      statusField="assessorName"
      columns={columns}
      auditColumnIndex={1}
      fields={[
        { name: 'assessorName', label: '姓名', required: true },
        { name: 'certificateNo', label: '证号', required: true },
        { name: 'workTypeScope', label: '工种', required: true },
        { name: 'levelScope', label: '等级范围', type: 'levelRange' },
        { name: 'validUntil', label: '有效期至', type: 'date', required: true },
        { name: 'phone', label: '联系电话' },
        { name: 'materialsDescription', label: '材料说明', type: 'textarea' },
        { name: 'remark', label: '备注', type: 'textarea' },
      ]}
      statusOptions={[
        { label: '空闲', value: 'idle' },
        { label: '认定筹备中', value: 'preparing' },
        { label: '认定任务中', value: 'assigned' },
        { label: '停用', value: 'disabled' },
        { label: '过期', value: 'expired' },
      ]}
      pageApi={pageAssessorInfo}
      createApi={createAssessorInfo}
      updateApi={updateAssessorInfo}
      submitApi={submitAssessorInfo}
      reviewApi={reviewAssessorInfo}
      disableApi={disableAssessorInfo}
      enableApi={enableAssessorInfo}
      deleteApi={deleteAssessorInfo}
    />
  );
}

function statusLabel(value: string) {
  return {
    idle: '空闲',
    preparing: '认定筹备中',
    assigned: '认定任务中',
    disabled: '停用',
    expired: '过期',
  }[value] || value || '-';
}
