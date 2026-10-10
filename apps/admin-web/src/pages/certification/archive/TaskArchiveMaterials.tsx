import { DeleteOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { Alert, Button, Empty, Form, message, Select, Space, Table, Upload } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { deleteArchiveMaterial, downloadArchiveFile, listArchiveMaterials, uploadArchiveMaterial } from '../../../api/certificationArchive';
import type { CertificationArchiveMaterial } from '../../../types/certificationArchive';
import { fileSizeText, taskMaterialOptions, dateTimeText } from './shared';

export function TaskArchiveMaterials({ archiveId }: { archiveId?: string }) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ['certification-archive-materials', archiveId],
    queryFn: () => listArchiveMaterials(archiveId!),
    enabled: !!archiveId,
  });
  const uploadedTypes = new Set((query.data || []).map((item) => item.materialType));
  const materialOptions = taskMaterialOptions.map((option) => ({
    ...option,
    disabled: uploadedTypes.has(option.value),
  }));

  const uploadMutation = useMutation({
    mutationFn: async (values: { materialType: string; file: File }) => {
      const option = taskMaterialOptions.find((item) => item.value === values.materialType);
      const data = new FormData();
      data.append('materialType', values.materialType);
      data.append('materialName', option?.label || values.materialType);
      data.append('file', values.file);
      return uploadArchiveMaterial(archiveId!, data);
    },
    onSuccess: () => {
      message.success('材料已上传');
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['certification-archive-materials', archiveId] });
      queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteArchiveMaterial(archiveId!, id),
    onSuccess: () => {
      message.success('材料已删除');
      queryClient.invalidateQueries({ queryKey: ['certification-archive-materials', archiveId] });
      queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
    },
  });

  const columns: ColumnsType<CertificationArchiveMaterial> = [
    { title: '材料名称', dataIndex: 'materialName', width: 260 },
    { title: '材料类型', dataIndex: 'materialType', width: 180 },
    { title: '材料分组', dataIndex: 'materialGroup', width: 130 },
    { title: '文件名', dataIndex: 'fileName', width: 220, ellipsis: true },
    { title: '大小', dataIndex: 'fileSize', width: 100, render: fileSizeText },
    { title: '上传时间', dataIndex: 'uploadedAt', width: 170, render: dateTimeText },
    {
      title: '操作',
      width: 160,
      render: (_, row) => (
        <Space>
          <Button size="small" icon={<DownloadOutlined />} onClick={() => downloadArchiveFile(row.id, row.fileName)}>下载</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => deleteMutation.mutate(row.id)}>删除</Button>
        </Space>
      ),
    },
  ];

  if (!archiveId) {
    return <Empty description="请选择归档记录" />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Alert
        type={uploadedTypes.size === taskMaterialOptions.length ? 'success' : 'info'}
        showIcon
        message={`认定材料已上传 ${uploadedTypes.size} / ${taskMaterialOptions.length} 类`}
        description="每类材料在当前任务中只能上传一次；如需重新上传，请先删除原材料。"
      />
      <Form form={form} layout="inline" onFinish={uploadMutation.mutate}>
        <Form.Item name="materialType" rules={[{ required: true, message: '请选择材料类型' }]}>
          <Select placeholder="材料类型" style={{ width: 320 }} options={materialOptions} />
        </Form.Item>
        <Form.Item
          name="file"
          valuePropName="file"
          getValueFromEvent={(event) => event?.fileList?.[0]?.originFileObj}
          rules={[{ required: true, message: '请选择文件' }]}
        >
          <Upload beforeUpload={() => false} maxCount={1}>
            <Button icon={<UploadOutlined />}>选择文件</Button>
          </Upload>
        </Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          disabled={uploadedTypes.size === taskMaterialOptions.length}
          loading={uploadMutation.isPending}
        >
          上传
        </Button>
      </Form>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={query.data || []}
        loading={query.isLoading}
        pagination={false}
        scroll={{ x: 1100 }}
      />
    </Space>
  );
}
