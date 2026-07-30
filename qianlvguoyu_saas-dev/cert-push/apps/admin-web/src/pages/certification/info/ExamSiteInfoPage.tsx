import { Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  createExamSiteInfo,
  deleteExamSiteInfo,
  disableExamSiteInfo,
  enableExamSiteInfo,
  pageExamSiteInfo,
  reviewExamSiteInfo,
  submitExamSiteInfo,
  updateExamSiteInfo,
} from '../../../api/certificationInfo';
import type { ExamSiteInfo, ExamSiteInfoPayload } from '../../../types/certificationInfo';
import { InfoManagementTable } from './InfoManagementTable';

const columns: ColumnsType<ExamSiteInfo> = [
  { title: '考点名称', dataIndex: 'siteName', width: 180 },
  { title: '类型', dataIndex: 'siteType', width: 100, render: (value) => siteTypeLabel(value) },
  { title: '地址', dataIndex: 'address', width: 240 },
  { title: '考位', dataIndex: 'seatCount', width: 90 },
  { title: '备案号', dataIndex: 'filingNo', width: 150 },
  { title: '联系电话', dataIndex: 'maskedContactPhone', width: 130 },
  { title: '使用状态', dataIndex: 'status', width: 110, render: (value) => <Tag>{statusLabel(value)}</Tag> },
];

export function ExamSiteInfoPage() {
  return (
    <InfoManagementTable<ExamSiteInfo, ExamSiteInfoPayload>
      title="考场信息"
      queryKey="certification-info-sites"
      permissionPrefix="certification.info.site"
      rowKey="id"
      statusField="siteName"
      columns={columns}
      fields={[
        { name: 'siteName', label: '考点名称', required: true },
        {
          name: 'siteType',
          label: '类型',
          type: 'select',
          required: true,
          options: [
            { label: '理论', value: 'theory' },
            { label: '实操', value: 'practical' },
            { label: '综合', value: 'comprehensive' },
          ],
        },
        { name: 'address', label: '地址', required: true },
        { name: 'seatCount', label: '考位', type: 'number', required: true },
        { name: 'filingNo', label: '备案号' },
        { name: 'contactName', label: '联系人' },
        { name: 'contactPhone', label: '联系电话' },
        { name: 'equipmentCondition', label: '场地设备条件', type: 'textarea' },
        { name: 'materialsDescription', label: '材料说明', type: 'textarea' },
        {
          name: 'status',
          label: '使用状态',
          type: 'select',
          options: [
            { label: '在用', value: 'active' },
            { label: '维护中', value: 'maintenance' },
            { label: '停用', value: 'disabled' },
          ],
        },
        { name: 'remark', label: '备注', type: 'textarea' },
      ]}
      statusOptions={[
        { label: '在用', value: 'active' },
        { label: '维护中', value: 'maintenance' },
        { label: '停用', value: 'disabled' },
      ]}
      pageApi={pageExamSiteInfo}
      createApi={createExamSiteInfo}
      updateApi={updateExamSiteInfo}
      submitApi={submitExamSiteInfo}
      reviewApi={reviewExamSiteInfo}
      disableApi={disableExamSiteInfo}
      enableApi={enableExamSiteInfo}
      deleteApi={deleteExamSiteInfo}
    />
  );
}

function siteTypeLabel(value: string) {
  return { theory: '理论', practical: '实操', comprehensive: '综合' }[value] || value || '-';
}

function statusLabel(value: string) {
  return { active: '在用', maintenance: '维护中', disabled: '停用' }[value] || value || '-';
}
