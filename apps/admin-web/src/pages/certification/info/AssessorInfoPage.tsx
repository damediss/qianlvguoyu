import { Button, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import {
  createAssessorInfo,
  createAssessorAccount,
  deleteAssessorInfo,
  disableAssessorInfo,
  enableAssessorInfo,
  pageAssessorInfo,
  reviewAssessorInfo,
  submitAssessorInfo,
  updateAssessorInfo,
} from '../../../api/certificationInfo';
import type { AssessorInfo, AssessorInfoPayload } from '../../../types/certificationInfo';
import { FilePreviewModal, type FilePreviewTarget } from '../../../components/FilePreviewModal';
import { AssessorLedgerImportButton } from './AssessorLedgerImportButton';
import { InfoManagementTable } from './InfoManagementTable';

const columns: ColumnsType<AssessorInfo> = [
  { title: '姓名', dataIndex: 'assessorName', width: 120 },
  { title: '平板账号', dataIndex: 'userId', width: 110, render: (value) => <Tag color={value ? 'success' : 'warning'}>{value ? '已创建' : '未绑定'}</Tag> },
  { title: '在岗状态', dataIndex: 'workStatus', width: 130, render: (value) => <Tag>{statusLabel(value)}</Tag> },
  { title: '证号', dataIndex: 'certificateNo', width: 160 },
  {
    title: '考评员证', dataIndex: 'certificatePhotoFileId', width: 110,
    render: (value, record) => <AssessorCertificatePreview fileId={value} fileName={record.certificatePhotoFileName} />,
  },
  { title: '工种', dataIndex: 'workTypeScope', width: 180 },
  {
    title: '授权维护', dataIndex: 'workTypeMappingWarning', width: 110,
    render: (warning) => warning ? <Tag color="warning">需修正</Tag> : '-',
  },
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
        { name: 'identityNo', label: '身份证号' },
        { name: 'accountUsername', label: '平板登录账号', createRequired: true },
        { name: 'accountPassword', label: '平板登录密码', type: 'password', createRequired: true },
        { name: 'certificateNo', label: '证号', required: true },
        { name: 'certificatePhotoFileId', label: '考评员证', type: 'imageUpload', fileNameField: 'certificatePhotoFileName' },
        { name: 'certificatePhotoFileName', label: '考评员证文件名', hidden: true },
        { name: 'workTypeIds', label: '工种', required: true, type: 'work-type-multiple', placeholder: '请选择父级或子级工种，可多选' },
        { name: 'workTypeScopes', label: '工种名称兼容字段', hidden: true },
        { name: 'levelScope', label: '等级范围', type: 'levelRange' },
        { name: 'validUntil', label: '有效期至', type: 'date', required: true },
        { name: 'phone', label: '联系电话' },
        { name: 'materialsDescription', label: '材料说明', type: 'textarea' },
        { name: 'remark', label: '备注', type: 'textarea' },
      ]}
      editableAuditStatuses={['draft', 'rejected', 'disabled']}
      statusOptions={[
        { label: '待审核', value: 'pending_review' },
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
      createAccountApi={createAssessorAccount}
      toolbarExtra={<AssessorLedgerImportButton />}
    />
  );
}

function AssessorCertificatePreview({ fileId, fileName }: { fileId?: string; fileName?: string }) {
  const [target, setTarget] = useState<FilePreviewTarget | null>(null);
  if (!fileId) return '-';
  return (
    <>
      <Button type="link" size="small" style={{ paddingInline: 0 }} onClick={() => setTarget({
        fileId, fileName: fileName || '考评员证', fileType: 'image/*',
      })}>
        查看考评员证
      </Button>
      <FilePreviewModal open={Boolean(target)} target={target} onClose={() => setTarget(null)} />
    </>
  );
}

function statusLabel(value: string) {
  return {
    pending_review: '待审核',
    idle: '空闲',
    preparing: '认定筹备中',
    assigned: '认定任务中',
    disabled: '停用',
    expired: '过期',
  }[value] || value || '-';
}
