import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  DownloadOutlined,
  ReloadOutlined,
  UserAddOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  Modal,
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
  downloadCompetitionRosterTemplate,
  getCompetitionEvent,
  getCompetitionRosterImport,
  pageCompetitionEventOrganizations,
  pageCompetitionRosterImports,
  precheckCompetitionRoster,
  precheckSingleCompetitionRoster,
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

const ID_CARD_WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
const ID_CARD_CHECK_CODES = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'];

function normalizeIdCard(value: string) {
  return value.trim().replace(/\s+/g, '').toUpperCase();
}

function isValidIdCard(value?: string) {
  if (!value) return false;
  const normalized = normalizeIdCard(value);
  if (!/^\d{17}[0-9X]$/.test(normalized)) return false;
  const sum = ID_CARD_WEIGHTS.reduce(
    (total, weight, index) => total + Number(normalized[index]) * weight,
    0,
  );
  return ID_CARD_CHECK_CODES[sum % 11] === normalized[17];
}

interface SingleRosterFormValues {
  employeeNo: string;
  name: string;
  idCardNo: string;
  mobile: string;
  workType?: string;
  level?: string;
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
  const [singleOpen, setSingleOpen] = useState(false);
  const [singleForm] = Form.useForm<SingleRosterFormValues>();
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

  const singlePrecheckMutation = useMutation({
    mutationFn: (values: SingleRosterFormValues) => {
      if (!orgId) throw new Error('请选择参赛企业');
      return precheckSingleCompetitionRoster(eventId, { orgId, ...values });
    },
    onSuccess: (detail) => {
      message.success('单个选手预检完成，请确认名单');
      setSingleOpen(false);
      singleForm.resetFields();
      setSelectedBatchId(idValue(detail.batchId || detail.id));
      setItemQuery({ page: 1, pageSize: 20 });
      queryClient.invalidateQueries({ queryKey: ['competition-roster-imports', eventId] });
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '单个选手录入失败'),
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

  const downloadTemplate = async () => {
    try {
      const blob = await downloadCompetitionRosterTemplate();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'competition-roster-template.xlsx';
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '模板下载失败');
    }
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
    {
      title: '批次号',
      dataIndex: 'batchNo',
      width: 180,
      ellipsis: true,
      render: (value: string, record: CompetitionRosterImport) => (
        <Button
          type="link"
          className="competition-roster-batch-link"
          title={value}
          onClick={() => { setSelectedBatchId(idValue(record.batchId)); setItemQuery({ page: 1, pageSize: 20 }); }}
        >
          {value}
        </Button>
      ),
    },
    {
      title: '参与企业',
      dataIndex: 'orgName',
      width: 260,
      render: (value: string | null | undefined, record: CompetitionRosterImport) => {
        const name = value || idValue(record.orgId);
        return <span className="competition-roster-org-name" title={name}>{name}</span>;
      },
    },
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
          <Form layout="inline" className="competition-roster-import-form" onFinish={() => precheckMutation.mutate()}>
            <Form.Item label="参赛企业" required>
              <Select value={orgId} onChange={setOrgId} options={organizationOptions} placeholder="请选择企业" style={{ width: 250 }} loading={organizationsQuery.isLoading} />
            </Form.Item>
            <Form.Item>
              <Button
                size="small"
                shape="circle"
                icon={<ReloadOutlined />}
                aria-label="刷新"
                title="刷新"
                loading={batchesQuery.isFetching}
                onClick={() => batchesQuery.refetch()}
              />
            </Form.Item>
            <Form.Item>
              <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>下载模板</Button>
            </Form.Item>
            <Form.Item>
              <Button icon={<UserAddOutlined />} disabled={!orgId} onClick={() => setSingleOpen(true)}>单个录入</Button>
            </Form.Item>
            <Form.Item required className="competition-roster-upload-actions">
              <Space size={6}>
                <Upload {...uploadProps}>
                  <Button icon={<UploadOutlined />}>选择文件</Button>
                </Upload>
                <Button
                  type="primary"
                  htmlType="submit"
                  className="competition-roster-upload-submit"
                  loading={precheckMutation.isPending}
                  disabled={!selectedFile || !orgId}
                  style={{ width: 64 }}
                >
                  上传
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>
      ) : (
        <section className="competition-page-heading">
          <Button
            size="small"
            shape="circle"
            icon={<ReloadOutlined />}
            aria-label="刷新"
            title="刷新"
            loading={batchesQuery.isFetching}
            onClick={() => batchesQuery.refetch()}
          />
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
          scroll={{ x: 1120 }}
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

      <Modal
        title="单个选手录入"
        open={singleOpen}
        destroyOnClose
        confirmLoading={singlePrecheckMutation.isPending}
        onCancel={() => { setSingleOpen(false); singleForm.resetFields(); }}
        onOk={() => singleForm.submit()}
      >
        <Form<SingleRosterFormValues> form={singleForm} layout="vertical" onFinish={(values) => singlePrecheckMutation.mutate(values)}>
          <Form.Item label="参赛企业"><Input value={organizationOptions.find((item) => item.value === orgId)?.label || orgId} disabled /></Form.Item>
          <Form.Item name="employeeNo" label="工号" rules={[{ required: true, whitespace: true, message: '请输入工号' }]}><Input maxLength={64} /></Form.Item>
          <Form.Item name="name" label="姓名" rules={[{ required: true, whitespace: true, message: '请输入姓名' }]}><Input maxLength={128} /></Form.Item>
          <Form.Item
            name="idCardNo"
            label="身份证号"
            normalize={(value: string) => normalizeIdCard(value || '')}
            rules={[
              { required: true, whitespace: true, message: '请输入身份证号' },
              {
                validator: (_, value: string) => (
                  !value || isValidIdCard(value)
                    ? Promise.resolve()
                    : Promise.reject(new Error('请输入校验码正确的18位身份证号'))
                ),
              },
            ]}
          >
            <Input maxLength={18} />
          </Form.Item>
          <Form.Item name="mobile" label="手机号" rules={[{ required: true, pattern: /^1\d{10}$/, message: '请输入11位手机号' }]}><Input maxLength={11} /></Form.Item>
          <Form.Item name="workType" label="工种"><Input maxLength={64} /></Form.Item>
          <Form.Item name="level" label="等级"><Input maxLength={32} /></Form.Item>
        </Form>
      </Modal>
    </CompetitionEventFrame>
  );
}
