import { Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  createExamEquipmentInfo,
  deleteExamEquipmentInfo,
  disableExamEquipmentInfo,
  enableExamEquipmentInfo,
  pageExamEquipmentInfo,
  reviewExamEquipmentInfo,
  submitExamEquipmentInfo,
  updateExamEquipmentInfo,
} from '../../../api/certificationInfo';
import type { ExamEquipmentInfo, ExamEquipmentInfoPayload } from '../../../types/certificationInfo';
import { InfoManagementTable } from './InfoManagementTable';

const columns: ColumnsType<ExamEquipmentInfo> = [
  { title: '设备名', dataIndex: 'equipmentType', width: 170 },
  { title: '设备编号', dataIndex: 'assetNo', width: 160 },
  { title: '设备数量', dataIndex: 'equipmentQuantity', width: 100 },
  { title: '设备状态', dataIndex: 'equipmentStatus', width: 130, render: (value) => <Tag>{statusLabel(value)}</Tag> },
  {
    title: '设备照片',
    dataIndex: 'equipmentPhotoFileId',
    width: 110,
    render: (value, record) => (value ? record.equipmentPhotoFileName || '已上传' : '-'),
  },
  { title: '备注', dataIndex: 'remark', width: 180, ellipsis: true },
];

export function ExamEquipmentInfoPage() {
  return (
    <InfoManagementTable<ExamEquipmentInfo, ExamEquipmentInfoPayload>
      title="考试设备"
      queryKey="certification-info-equipment"
      permissionPrefix="certification.info.equipment"
      rowKey="id"
      statusField="equipmentType"
      columns={columns}
      fields={[
        { name: 'equipmentType', label: '设备名', required: true },
        { name: 'assetNo', label: '设备编号', required: true },
        { name: 'equipmentQuantity', label: '设备数量', type: 'number', required: true },
        { name: 'equipmentPhotoFileId', label: '设备照片', type: 'imageUpload', fileNameField: 'equipmentPhotoFileName' },
        { name: 'equipmentPhotoFileName', label: '设备照片文件名', hidden: true },
        { name: 'materialsDescription', label: '材料说明', type: 'textarea' },
        { name: 'remark', label: '备注', type: 'textarea' },
      ]}
      statusOptions={[
        { label: '空闲中', value: 'idle' },
        { label: '使用中', value: 'in_use' },
        { label: '禁用', value: 'disabled' },
      ]}
      pageApi={pageExamEquipmentInfo}
      createApi={createExamEquipmentInfo}
      updateApi={updateExamEquipmentInfo}
      submitApi={submitExamEquipmentInfo}
      reviewApi={reviewExamEquipmentInfo}
      disableApi={disableExamEquipmentInfo}
      enableApi={enableExamEquipmentInfo}
      deleteApi={deleteExamEquipmentInfo}
    />
  );
}

function statusLabel(value: string) {
  return {
    idle: '空闲中',
    in_use: '使用中',
    disabled: '禁用',
  }[value] || value || '-';
}
