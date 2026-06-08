import { DeleteOutlined, EyeOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  bindPreparationTaskEquipment,
  listPreparationTaskEquipment,
  removePreparationTaskEquipment,
} from '../../../api/certificationPreparation';
import { listAvailableEquipment } from '../../../api/certificationInfo';
import { FilePreviewModal, type FilePreviewTarget } from '../../../components/FilePreviewModal';
import { PermissionGuard } from '../../../directives/permission';
import type { PreparationTaskEquipment } from '../../../types/certificationPreparation';
import { dash, statusTag } from './shared';

interface EquipmentConfigProps {
  taskId?: string;
  onChanged?: () => void;
}

interface AssignFormValues {
  equipmentId: string;
  remark?: string;
}

export function EquipmentConfig({ taskId, onChanged }: EquipmentConfigProps) {
  const queryClient = useQueryClient();
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);
  const [assignForm] = Form.useForm<AssignFormValues>();

  const availableEquipmentQuery = useQuery({
    queryKey: ['certification-info-equipment-available'],
    queryFn: () => listAvailableEquipment(),
    enabled: assignModalOpen,
  });

  const taskEquipmentQuery = useQuery({
    queryKey: ['certification-preparation-task-equipment', taskId],
    queryFn: () => listPreparationTaskEquipment(taskId!),
    enabled: Boolean(taskId),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-equipment', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-info-equipment-available'] });
    queryClient.invalidateQueries({ queryKey: ['certification-info-equipment'] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    onChanged?.();
  };

  const assignMutation = useMutation({
    mutationFn: (values: AssignFormValues) => bindPreparationTaskEquipment(taskId!, {
      equipmentId: values.equipmentId,
      remark: values.remark,
    }),
    onSuccess: () => {
      message.success('设备已绑定到任务');
      setAssignModalOpen(false);
      assignForm.resetFields();
      refresh();
    },
  });

  const removeMutation = useMutation({
    mutationFn: (taskEquipmentId: string) => removePreparationTaskEquipment(taskId!, taskEquipmentId),
    onSuccess: () => {
      message.success('任务设备已删除');
      refresh();
    },
  });

  const columns: ColumnsType<PreparationTaskEquipment> = [
    { title: '设备名', dataIndex: 'equipmentName', width: 180 },
    { title: '设备编号', dataIndex: 'equipmentNo', width: 160 },
    { title: '设备数量', dataIndex: 'equipmentQuantity', width: 100 },
    { title: '备注', dataIndex: 'remark', ellipsis: true, render: dash },
    {
      title: '预览',
      width: 80,
      align: 'center',
      render: (_, record) =>
        record.equipmentPhotoFileId ? (
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() =>
              setPreviewTarget({
                fileId: record.equipmentPhotoFileId,
                fileName: record.equipmentPhotoFileName || '设备照片',
                fileType: 'image/*',
              })
            }
          >
            预览
          </Button>
        ) : (
          <Typography.Text type="secondary">-</Typography.Text>
        ),
    },
    { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
    {
      title: '操作',
      width: 110,
      render: (_, record) => (
        <PermissionGuard
          codes={['certification.preparation.equipment.bind', 'certification.preparation.equipment.assign']}
          mode="any"
        >
          <Popconfirm
            title="确认删除该任务设备？"
            okText="删除"
            cancelText="取消"
            onConfirm={() => removeMutation.mutate(record.id)}
          >
            <Button type="link" danger size="small" icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </PermissionGuard>
      ),
    },
  ];

  const availableOptions = (availableEquipmentQuery.data || []).map((item) => ({
    label: `${item.equipmentType}（${item.assetNo}，${item.equipmentQuantity || 1}）`,
    value: item.id,
  }));

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard
        title="任务设备"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
            <PermissionGuard
              codes={['certification.preparation.equipment.bind', 'certification.preparation.equipment.assign']}
              mode="any"
            >
              <Button
                type="primary"
                icon={<PlusOutlined />}
                disabled={!taskId}
                onClick={() => {
                  assignForm.resetFields();
                  setAssignModalOpen(true);
                }}
              >
                绑定设备
              </Button>
            </PermissionGuard>
          </Space>
        }
      >
        {!taskId ? (
          <Typography.Text type="secondary">请选择认定任务后配置设备。</Typography.Text>
        ) : (
          <Table
            rowKey="id"
            columns={columns}
            dataSource={taskEquipmentQuery.data || []}
            loading={taskEquipmentQuery.isFetching}
            pagination={false}
            scroll={{ x: 840 }}
          />
        )}
      </ProCard>

      <FilePreviewModal
        open={Boolean(previewTarget)}
        target={previewTarget}
        onClose={() => setPreviewTarget(null)}
      />

      <Modal
        title="绑定设备"
        open={assignModalOpen}
        onOk={() => assignForm.validateFields().then((values) => assignMutation.mutate(values))}
        onCancel={() => setAssignModalOpen(false)}
        confirmLoading={assignMutation.isPending}
        width={520}
      >
        <Form form={assignForm} layout="vertical">
          <Form.Item name="equipmentId" label="设备" rules={[{ required: true, message: '请选择设备' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              loading={availableEquipmentQuery.isFetching}
              placeholder="请选择空闲中的设备"
              options={availableOptions}
              notFoundContent={
                availableEquipmentQuery.isFetching
                  ? '加载中...'
                  : '暂无可用设备，请先在信息管理中新增并审核通过'
              }
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
