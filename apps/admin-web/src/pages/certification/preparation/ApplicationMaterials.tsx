import { ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Descriptions, Modal, Space, Statistic, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import {
  generatePreparationMaterials,
  getPreparationMaterialBatch,
  listAllPreparationCandidates,
  pagePreparationMaterials,
} from '../../../api/certificationPreparation';
import { PermissionGate } from '../../../directives/permission';
import type { PreparationMaterialBatch, PreparationMaterialItem, PreparationMaterialQuery } from '../../../types/certificationPreparation';
import { dash, statusTag } from './shared';

interface ApplicationMaterialsProps {
  taskId?: string;
  onChanged?: () => void;
}

export function ApplicationMaterials({ taskId, onChanged }: ApplicationMaterialsProps) {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState<PreparationMaterialQuery>({ page: 1, pageSize: 10 });
  const [detailBatchId, setDetailBatchId] = useState<string>();

  const candidatesQuery = useQuery({
    queryKey: ['certification-preparation-candidates', taskId, 'materials-ready'],
    queryFn: () => listAllPreparationCandidates(taskId!, { status: 'active' }),
    enabled: Boolean(taskId),
  });

  const materialsQuery = useQuery({
    queryKey: ['certification-preparation-materials', taskId, query],
    queryFn: () => pagePreparationMaterials(taskId!, query),
    enabled: Boolean(taskId),
  });

  const detailQuery = useQuery({
    queryKey: ['certification-preparation-material-detail', taskId, detailBatchId],
    queryFn: () => getPreparationMaterialBatch(taskId!, detailBatchId!),
    enabled: Boolean(taskId && detailBatchId),
  });

  const readiness = useMemo(() => {
    const items = candidatesQuery.data?.items || [];
    const total = items.length;
    const ready = items.filter((item) => item.documentComplete).length;
    const incomplete = total - ready;
    return { total, ready, incomplete };
  }, [candidatesQuery.data?.items]);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-materials', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates', taskId] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
    onChanged?.();
  };

  const generateMutation = useMutation({
    mutationFn: () =>
      generatePreparationMaterials(taskId!, {
        generateScope: 'all',
        remark: '一键生成申报材料',
      }),
    onSuccess: (batch) => {
      message.success(`申报材料已生成：成功 ${batch.successCount || 0}，失败 ${batch.failedCount || 0}`);
      refresh();
    },
    onError: (error) => {
      message.error(apiErrorMessage(error, '申报材料生成失败，请确认学员资料已齐全后重试'));
    },
  });

  function handleOneClickGenerate() {
    if (readiness.total === 0) {
      message.warning('请先在「学员导入」中导入学员并上传资料');
      return;
    }
    if (readiness.ready === 0) {
      message.warning('暂无资料齐全的学员，请先在学员列表中完成资料上传');
      return;
    }
    if (readiness.incomplete > 0) {
      message.warning(`瀛樺湪 ${readiness.incomplete} 鍚嶅鍛樼殑蹇呬紶璧勬枡鏈鏍搁€氳繃锛屾殏涓嶈兘鐢熸垚鐢虫姤鏉愭枡`);
      return;
    }
    Modal.confirm({
      title: '一键生成申报材料',
      content: (
        <Typography.Paragraph>
          将基于 {readiness.ready} 名资料齐全学员的导入资料，自动生成申报材料包。
          {readiness.incomplete > 0 ? `另有 ${readiness.incomplete} 名学员资料未齐全，将不参与本次生成。` : null}
        </Typography.Paragraph>
      ),
      okText: '确认生成',
      cancelText: '取消',
      onOk: () => generateMutation.mutateAsync(),
    });
  }

  const columns: ColumnsType<PreparationMaterialBatch> = [
    { title: '批次号', dataIndex: 'batchNo', width: 180, fixed: 'left' },
    { title: '生成范围', dataIndex: 'generateScope', width: 120, render: (value) => (value === 'all' ? '资料齐全学员' : dash(value)) },
    { title: '考生数', dataIndex: 'candidateCount', width: 90, render: dash },
    { title: '成功', dataIndex: 'successCount', width: 90, render: dash },
    { title: '失败', dataIndex: 'failedCount', width: 90, render: dash },
    { title: '状态', dataIndex: 'status', width: 110, render: statusTag },
    { title: '文件包', dataIndex: 'packageFileName', minWidth: 180, render: dash },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => setDetailBatchId(record.id)}>
          查看明细
        </Button>
      ),
    },
  ];

  const itemColumns: ColumnsType<PreparationMaterialItem> = [
    { title: '考生ID', dataIndex: 'candidateId', width: 90, render: dash },
    { title: '材料编码', dataIndex: 'itemCode', width: 110, render: dash },
    { title: '材料名称', dataIndex: 'itemName', minWidth: 180, render: dash },
    { title: '文件', dataIndex: 'fileName', minWidth: 180, render: dash },
    { title: '版本', dataIndex: 'versionNo', width: 80, render: dash },
    { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
  ];

  if (!taskId) {
    return (
      <ProCard>
        <Typography.Text type="secondary">请选择认定任务后生成申报材料。</Typography.Text>
      </ProCard>
    );
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard title="申报材料生成">
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="根据学员导入环节已上传的资料（报名信息表、两寸证件照），一键生成每位学员的申报材料包。"
        />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 16, marginBottom: 20 }}>
          <Statistic title="任务学员总数" value={readiness.total} />
          <Statistic title="资料齐全（可生成）" value={readiness.ready} valueStyle={{ color: '#16a34a' }} />
          <Statistic title="资料未齐全" value={readiness.incomplete} valueStyle={{ color: readiness.incomplete > 0 ? '#d97706' : undefined }} />
        </div>
        <Descriptions bordered size="small" column={1} style={{ marginBottom: 20 }}>
          <Descriptions.Item label="生成说明">
            系统读取每位学员在导入环节上传的资料，自动汇总生成申报材料；仅「资料齐全」的学员参与生成。
          </Descriptions.Item>
        </Descriptions>
        <Space>
          <PermissionGate code="certification.preparation.material.generate">
            <Button
              type="primary"
              size="large"
              icon={<ThunderboltOutlined />}
              loading={generateMutation.isPending}
              onClick={handleOneClickGenerate}
            >
              一键生成申报材料
            </Button>
          </PermissionGate>
          <Button icon={<ReloadOutlined />} onClick={refresh}>
            刷新
          </Button>
        </Space>
      </ProCard>

      <ProCard
        title="生成记录"
        extra={<Typography.Text type="secondary">历史生成批次与文件包</Typography.Text>}
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={materialsQuery.data?.items || []}
          loading={materialsQuery.isFetching}
          scroll={{ x: 1000 }}
          locale={{ emptyText: '暂无生成记录，请先完成学员资料上传后一键生成' }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: materialsQuery.data?.total || 0,
            showSizeChanger: true,
            onChange: (page, pageSize) => setQuery({ ...query, page, pageSize }),
          }}
        />
      </ProCard>

      <Modal
        title="申报材料明细"
        open={Boolean(detailBatchId)}
        onCancel={() => setDetailBatchId(undefined)}
        width={760}
        footer={
          <Button onClick={() => setDetailBatchId(undefined)}>关闭</Button>
        }
      >
        <Typography.Paragraph type="secondary">
          批次文件包：{detailQuery.data?.packageFileName || '暂无'}，文件ID：{detailQuery.data?.packageFileId || '-'}
        </Typography.Paragraph>
        <Table
          rowKey="id"
          size="small"
          columns={itemColumns}
          dataSource={detailQuery.data?.items || []}
          loading={detailQuery.isFetching}
          pagination={false}
        />
      </Modal>
    </Space>
  );
}

function apiErrorMessage(error: unknown, fallback: string) {
  const response = (error as { response?: { data?: { message?: string } } })?.response;
  return response?.data?.message || (error instanceof Error ? error.message : undefined) || fallback;
}
