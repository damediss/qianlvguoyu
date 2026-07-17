import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Result,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Upload,
  message,
} from 'antd';
import type { UploadFile, UploadProps } from 'antd';
import dayjs from 'dayjs';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  confirmCompetitionRosterImport,
  getCompetitionEvent,
  getCompetitionRosterImport,
  pageCompetitionEventOrganizations,
  pageCompetitionRosterImports,
  precheckCompetitionRoster,
} from '../../api/competition';
import { uploadPlatformFile } from '../../api/files';
import type {
  CompetitionRosterImport,
  CompetitionRosterImportDetail,
  CompetitionRosterImportListQuery,
  CompetitionRosterImportStatus,
  CompetitionRosterItem,
  CompetitionRosterImportPageQuery,
  CompetitionRosterValidationStatus,
} from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';

const importStatusMeta: Record<CompetitionRosterImportStatus, { label: string; color: string }> = {
  validating: { label: '校验中', color: 'processing' },
  validation_failed: { label: '校验失败', color: 'error' },
  pending_confirm: { label: '待确认', color: 'warning' },
  confirmed: { label: '已确认', color: 'success' },
};

const validationStatusMeta: Record<CompetitionRosterValidationStatus, { label: string; color: string }> = {
  valid: { label: '有效', color: 'success' },
  invalid: { label: '无效', color: 'error' },
};

function formatDate(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function idValue(value: string | number) {
  return String(value);
}

export function CompetitionRosterImportPage({
  eventId,
  onNavigate,
}: {
  eventId: string;
  onNavigate: (path: string) => void;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:roster:view');
  const canImport = permissions.includes('competition:roster:import');
  const canConfirm = permissions.includes('competition:roster:confirm');
  const canViewOrganizations = permissions.includes('competition:event-org:view');
  const initialBatchId = new URLSearchParams(window.location.search).get('batchId') || undefined;
  const [selectedBatchId, setSelectedBatchId] = useState<string | undefined>(initialBatchId);
  const [selectedFile, setSelectedFile] = useState<File>();
  const [uploadFileList, setUploadFileList] = useState<UploadFile[]>([]);
  const [orgId, setOrgId] = useState<string>();
  const [query, setQuery] = useState<CompetitionRosterImportListQuery>({ page: 1, pageSize: 10 });
  const [itemQuery, setItemQuery] = useState<CompetitionRosterImportPageQuery>({ page: 1, pageSize: 20 });
  const queryClient = useQueryClient();

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canView,
  });

  const organizationsQuery = useQuery({
    queryKey: ['competition-event-organizations', eventId, 'active'],
    queryFn: () => pageCompetitionEventOrganizations(eventId, { page: 1, pageSize: 500, status: 'active' }),
    enabled: canView && canViewOrganizations,
  });

  const organizationOptions = useMemo(() => {
    return (organizationsQuery.data?.items || []).map((organization) => ({
      value: idValue(organization.orgId),
      label: organization.orgName ? `${organization.orgName}（${organization.orgCode || organization.orgId}）` : idValue(organization.orgId),
    }));
  }, [organizationsQuery.data?.items]);

  useEffect(() => {
    if (!orgId && organizationOptions.length === 1) setOrgId(organizationOptions[0].value);
  }, [orgId, organizationOptions]);

  const batchesQuery = useQuery({
    queryKey: ['competition-roster-imports', eventId, query],
    queryFn: () => pageCompetitionRosterImports(eventId, query),
    enabled: canView,
  });

  const detailQuery = useQuery({
    queryKey: ['competition-roster-import', selectedBatchId, itemQuery],
    queryFn: () => getCompetitionRosterImport(selectedBatchId!, itemQuery),
    enabled: canView && Boolean(selectedBatchId),
  });

  const precheckMutation = useMutation({
    mutationFn: async () => {
      if (!selectedFile || !orgId) throw new Error('请选择 Excel 文件和参赛企业');
      const uploaded = await uploadPlatformFile(selectedFile, 'private');
      if (!uploaded.id) throw new Error('文件上传未返回 fileId');
      return precheckCompetitionRoster(eventId, { fileId: uploaded.id, orgId });
    },
    onSuccess: (detail) => {
      message.success('名单预检完成');
      setSelectedFile(undefined);
      setUploadFileList([]);
      setSelectedBatchId(idValue(detail.batchId || detail.id));
      setItemQuery({ page: 1, pageSize: 20 });
      queryClient.invalidateQueries({ queryKey: ['competition-roster-imports', eventId] });
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '名单预检失败'),
  });

  const confirmMutation = useMutation({
    mutationFn: () => confirmCompetitionRosterImport(selectedBatchId!),
    onSuccess: () => {
      message.success('名单批次已确认');
      queryClient.invalidateQueries({ queryKey: ['competition-roster-import', selectedBatchId] });
      queryClient.invalidateQueries({ queryKey: ['competition-roster-imports', eventId] });
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '名单确认失败'),
  });

  const uploadProps: UploadProps = {
    accept: '.xlsx,.xls',
    maxCount: 1,
    fileList: uploadFileList,
    beforeUpload: (file) => {
      if (!/\.(xlsx|xls)$/i.test(file.name)) {
        message.error('仅支持 .xlsx 或 .xls 文件');
        return Upload.LIST_IGNORE;
      }
      setSelectedFile(file);
      setUploadFileList([file]);
      return false;
    },
    onRemove: () => {
      setSelectedFile(undefined);
      setUploadFileList([]);
    },
  };

  const confirmBatch = () => {
    const detail = detailQuery.data;
    if (!detail || detail.status !== 'pending_confirm' || !canConfirm) return;
    confirmMutation.mutate();
  };

  const itemColumns = [
    { title: '工号', dataIndex: 'employeeNo', width: 160 },
    { title: '姓名', dataIndex: 'name', width: 140 },
    { title: '工种', dataIndex: 'workType', width: 160, render: (value: string | null | undefined) => value || '-' },
    { title: '等级', dataIndex: 'level', width: 120, render: (value: string | null | undefined) => value || '-' },
    {
      title: '校验状态', dataIndex: 'validationStatus', width: 120,
      render: (value: CompetitionRosterValidationStatus) => <Tag color={validationStatusMeta[value]?.color}>{validationStatusMeta[value]?.label || value}</Tag>,
    },
    { title: '错误原因', dataIndex: 'errorMessage', render: (value: string | null | undefined) => value || '-' },
  ];

  if (!canView) {
    return <Result status="403" title="暂无名单查看权限" subTitle="请联系管理员申请 competition:roster:view 权限。" />;
  }

  const batchColumns = [
    { title: '批次号', dataIndex: 'batchNo', render: (value: string, record: CompetitionRosterImport) => <Button type="link" onClick={() => { setSelectedBatchId(idValue(record.batchId)); setItemQuery({ page: 1, pageSize: 20 }); }}>{value}</Button> },
    { title: '参与企业', dataIndex: 'orgName', render: (value: string | null | undefined, record: CompetitionRosterImport) => value || idValue(record.orgId) },
    { title: '状态', dataIndex: 'status', width: 120, render: (value: CompetitionRosterImportStatus) => <Tag color={importStatusMeta[value]?.color}>{importStatusMeta[value]?.label || value}</Tag> },
    { title: '总数', dataIndex: 'totalCount', width: 80 },
    { title: '有效', dataIndex: 'validCount', width: 80 },
    { title: '无效', dataIndex: 'invalidCount', width: 80 },
    { title: '创建时间', dataIndex: 'createdAt', width: 175, render: formatDate },
  ];

  const detail = detailQuery.data;
  const items = detail?.items?.items || [];

  if (eventQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventQuery.isError || !eventQuery.data) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回计划</Button>} />;
  }

  return (
    <CompetitionEventFrame event={eventQuery.data} activeSection="rosters" onNavigate={onNavigate}>
      {canImport ? (
        <Card bordered={false} className="competition-filter-card">
          <Form layout="inline" onFinish={() => precheckMutation.mutate()}>
            <Form.Item label="参赛企业" required>
              <Select value={orgId} onChange={setOrgId} options={organizationOptions} placeholder="请选择企业" style={{ width: 260 }} loading={organizationsQuery.isLoading} />
            </Form.Item>
            <Form.Item>
              <Button icon={<ReloadOutlined />} loading={batchesQuery.isFetching} onClick={() => batchesQuery.refetch()}>刷新</Button>
            </Form.Item>
            <Form.Item required>
              <Upload {...uploadProps}>
                <Button icon={<UploadOutlined />}>选择文件</Button>
              </Upload>
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={precheckMutation.isPending} disabled={!selectedFile || !orgId} style={{ width: 80 }}>上传</Button>
            </Form.Item>
          </Form>
        </Card>
      ) : (
        <section className="competition-page-heading">
          <Button icon={<ReloadOutlined />} loading={batchesQuery.isFetching} onClick={() => batchesQuery.refetch()}>刷新</Button>
        </section>
      )}

      <Card title="批次列表" bordered={false} className="competition-table-card">
        <Space style={{ marginBottom: 16 }} wrap>
          <Select allowClear value={query.status} placeholder="按状态筛选" options={Object.entries(importStatusMeta).map(([value, meta]) => ({ value, label: meta.label }))} onChange={(status) => setQuery((current) => ({ ...current, page: 1, status }))} style={{ width: 160 }} />
          <Select allowClear value={query.orgId} placeholder="按企业筛选" options={organizationOptions} onChange={(value) => setQuery((current) => ({ ...current, page: 1, orgId: value }))} style={{ width: 260 }} />
        </Space>
        <Table<CompetitionRosterImport>
          rowKey={(record) => idValue(record.batchId)}
          loading={batchesQuery.isFetching}
          dataSource={batchesQuery.data?.items || []}
          columns={batchColumns}
          scroll={{ x: 950 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: batchesQuery.data?.total || 0, showSizeChanger: true, onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })) }}
        />
      </Card>

      {selectedBatchId && (
        <Card title="批次详情" bordered={false} className="competition-detail-card" extra={detail?.status === 'pending_confirm' && canConfirm ? <Button type="primary" icon={<CheckCircleOutlined />} loading={confirmMutation.isPending} onClick={confirmBatch}>确认名单</Button> : null}>
          {detailQuery.isLoading ? <Spin /> : detailQuery.isError ? <Result status="error" title="批次详情加载失败" /> : !detail ? <Empty description="暂无批次详情" /> : <>
            <Descriptions column={{ xs: 1, sm: 2, md: 4 }}>
              <Descriptions.Item label="批次号">{detail.batchNo}</Descriptions.Item>
              <Descriptions.Item label="企业">{detail.orgName || idValue(detail.orgId)}</Descriptions.Item>
              <Descriptions.Item label="状态"><Tag color={importStatusMeta[detail.status]?.color}>{importStatusMeta[detail.status]?.label || detail.status}</Tag></Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDate(detail.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="总数">{detail.totalCount}</Descriptions.Item>
              <Descriptions.Item label="有效数量">{detail.validCount}</Descriptions.Item>
              <Descriptions.Item label="无效数量">{detail.invalidCount}</Descriptions.Item>
              <Descriptions.Item label="说明">{detail.summary || '-'}</Descriptions.Item>
            </Descriptions>
            <Table<CompetitionRosterItem> rowKey={(record) => idValue(record.id)} loading={detailQuery.isFetching} dataSource={items} columns={itemColumns} scroll={{ x: 900 }} pagination={{ current: itemQuery.page, pageSize: itemQuery.pageSize, total: detail.items?.total || 0, showSizeChanger: true, onChange: (page, pageSize) => setItemQuery({ page, pageSize }) }} />
          </>}
        </Card>
      )}
    </CompetitionEventFrame>
  );
}
