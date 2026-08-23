import {
  CheckCircleOutlined,
  DownloadOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Result,
  Select,
  Space,
  Spin,
  Steps,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { UploadFile, UploadProps } from 'antd';
import axios from 'axios';
import dayjs from 'dayjs';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  confirmCompetitionRosterImport,
  downloadCompetitionRosterTemplate,
  getCompetitionEvent,
  getCompetitionRosterImport,
  listCompetitionEventWorkTypes,
  pageCompetitionEventOrganizations,
  precheckCompetitionRoster,
} from '../../api/competition';
import { uploadPlatformFile } from '../../api/files';
import type {
  CompetitionEventWorkType,
  CompetitionRosterItem,
  CompetitionRosterImportPageQuery,
  CompetitionRosterValidationStatus,
} from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';

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

function isHandledApiError(error: unknown): boolean {
  return axios.isAxiosError(error) && Boolean(error.response);
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
  const [eventWorkTypeId, setEventWorkTypeId] = useState<string>();
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

  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: canView,
  });

  const organizationOptions = useMemo(() => (
    (organizationsQuery.data?.items || []).map((organization) => ({
      value: idValue(organization.orgId),
      label: organization.orgName
        ? `${organization.orgName}（${organization.orgCode || organization.orgId}）`
        : idValue(organization.orgId),
    }))
  ), [organizationsQuery.data?.items]);

  const workTypeOptions = useMemo(() => (
    (workTypesQuery.data || [])
      .filter((item: CompetitionEventWorkType) => item.status === 'active')
      .map((item) => ({
        value: idValue(item.id),
        label: item.displayName || item.workTypeName || idValue(item.workTypeId),
      }))
  ), [workTypesQuery.data]);

  useEffect(() => {
    if (!orgId && organizationOptions.length === 1) setOrgId(organizationOptions[0].value);
  }, [orgId, organizationOptions]);

  const detailQuery = useQuery({
    queryKey: ['competition-roster-import', selectedBatchId, itemQuery],
    queryFn: () => getCompetitionRosterImport(selectedBatchId!, itemQuery),
    enabled: canView && Boolean(selectedBatchId),
  });

  const precheckMutation = useMutation({
    mutationFn: async () => {
      if (!selectedFile || !orgId || !eventWorkTypeId) {
        throw new Error('请选择企业、赛事工种和 Excel 文件');
      }
      const uploaded = await uploadPlatformFile(selectedFile, 'private');
      if (!uploaded.id) throw new Error('文件上传未返回 fileId');
      return precheckCompetitionRoster(eventId, {
        fileId: uploaded.id,
        orgId,
        eventWorkTypeId,
      });
    },
    onSuccess: (detail) => {
      message.success('名单预检完成，请在下方确认导入');
      setSelectedFile(undefined);
      setUploadFileList([]);
      setSelectedBatchId(idValue(detail.batchId || detail.id));
      setItemQuery({ page: 1, pageSize: 20 });
      queryClient.invalidateQueries({ queryKey: ['competition-roster-import', detail.batchId || detail.id] });
    },
    onError: (error) => {
      if (!isHandledApiError(error)) {
        message.error(error instanceof Error ? error.message : '名单预检失败');
      }
    },
  });

  const confirmMutation = useMutation({
    mutationFn: () => confirmCompetitionRosterImport(selectedBatchId!),
    onSuccess: () => {
      message.success('名单已确认，选手已进入选手管理');
      queryClient.invalidateQueries({ queryKey: ['competition-roster-import', selectedBatchId] });
      queryClient.invalidateQueries({ queryKey: ['competition-players'] });
    },
    onError: (error) => {
      if (!isHandledApiError(error)) {
        message.error(error instanceof Error ? error.message : '名单确认失败');
      }
    },
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

  const downloadTemplate = async () => {
    try {
      const blob = await downloadCompetitionRosterTemplate();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = '赛事名单导入模板.xlsx';
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '模板下载失败');
    }
  };

  const itemColumns = [
    { title: '工号', dataIndex: 'employeeNo', width: 160 },
    { title: '姓名', dataIndex: 'name', width: 140 },
    { title: '工种', dataIndex: 'workType', width: 180, render: (value: string | null | undefined) => value || '-' },
    { title: '等级', dataIndex: 'level', width: 120, render: (value: string | null | undefined) => value || '-' },
    {
      title: '校验状态', dataIndex: 'validationStatus', width: 120,
      render: (value: CompetitionRosterValidationStatus) => (
        <Tag color={validationStatusMeta[value]?.color}>{validationStatusMeta[value]?.label || value}</Tag>
      ),
    },
    { title: '错误原因', dataIndex: 'errorMessage', render: (value: string | null | undefined) => value || '-' },
  ];

  if (!canView) {
    return <Result status="403" title="暂无名单查看权限" subTitle="请联系管理员申请 competition:roster:view 权限。" />;
  }
  if (eventQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventQuery.isError || !eventQuery.data) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回赛事</Button>} />;
  }

  const detail = detailQuery.data;
  const items = detail?.items?.items || [];
  const step = !orgId ? 0 : !eventWorkTypeId ? 1 : 2;

  return (
    <CompetitionEventFrame event={eventQuery.data} activeSection="rosters" onNavigate={onNavigate}>
      {canImport ? (
        <Card bordered={false} className="competition-detail-card competition-roster-import-wizard">
          <Steps
            current={step}
            items={[{ title: '选择企业' }, { title: '选择工种' }, { title: '选择文件导入' }]}
          />
          <div className="competition-roster-import-wizard__body">
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <div>
                <div className="competition-card-heading">
                  <div>
                    <Typography.Title level={4}>选择参赛企业</Typography.Title>
                    <Typography.Text type="secondary">先确定本次名单归属的赛事参与企业。</Typography.Text>
                  </div>
                </div>
                <Select
                  showSearch
                  optionFilterProp="label"
                  value={orgId}
                  onChange={(value) => { setOrgId(value); setSelectedBatchId(undefined); }}
                  options={organizationOptions}
                  placeholder="请选择企业"
                  style={{ width: 360, maxWidth: '100%' }}
                  loading={organizationsQuery.isLoading}
                />
              </div>
              <div>
                <div className="competition-card-heading">
                  <div>
                    <Typography.Title level={4}>选择参赛工种</Typography.Title>
                    <Typography.Text type="secondary">导入后每名选手会直接归属到所选赛事工种。</Typography.Text>
                  </div>
                </div>
                <Select
                  showSearch
                  optionFilterProp="label"
                  value={eventWorkTypeId}
                  onChange={(value) => { setEventWorkTypeId(value); setSelectedBatchId(undefined); }}
                  options={workTypeOptions}
                  placeholder="请选择赛事工种"
                  style={{ width: 360, maxWidth: '100%' }}
                  loading={workTypesQuery.isLoading}
                  disabled={!orgId}
                />
              </div>
              <div>
                <div className="competition-card-heading">
                  <div>
                    <Typography.Title level={4}>选择文件导入</Typography.Title>
                    <Typography.Text type="secondary">支持 .xlsx / .xls；模板中已填写的工种必须与本步骤选择一致，留空时按本步骤选择导入。</Typography.Text>
                  </div>
                </div>
                <Space wrap>
                  <Upload {...uploadProps} disabled={!orgId || !eventWorkTypeId}>
                    <Button icon={<UploadOutlined />} disabled={!orgId || !eventWorkTypeId}>选择文件</Button>
                  </Upload>
                  <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>下载模板</Button>
                  <Button
                    type="primary"
                    icon={<UploadOutlined />}
                    loading={precheckMutation.isPending}
                    disabled={!selectedFile || !orgId || !eventWorkTypeId}
                    onClick={() => precheckMutation.mutate()}
                  >
                    开始导入
                  </Button>
                  <Button icon={<ReloadOutlined />} onClick={() => { void organizationsQuery.refetch(); void workTypesQuery.refetch(); }}>刷新选项</Button>
                </Space>
              </div>
            </Space>
          </div>
        </Card>
      ) : (
        <Alert type="info" showIcon message="当前账号仅可查看名单预检结果，暂无导入权限。" />
      )}

      {selectedBatchId && (
        <Card
          title="导入预览"
          bordered={false}
          className="competition-detail-card"
          extra={detail?.status === 'pending_confirm' && canConfirm ? (
            <Button type="primary" icon={<CheckCircleOutlined />} loading={confirmMutation.isPending} onClick={() => confirmMutation.mutate()}>
              确认导入
            </Button>
          ) : null}
        >
          {detailQuery.isLoading ? <Spin /> : detailQuery.isError ? <Result status="error" title="导入预览加载失败" /> : !detail ? <Empty description="暂无导入预览" /> : <>
            <Descriptions column={{ xs: 1, sm: 2, md: 4 }}>
              <Descriptions.Item label="企业">{detail.orgName || idValue(detail.orgId)}</Descriptions.Item>
              <Descriptions.Item label="赛事工种">{workTypeOptions.find((item) => item.value === idValue(detail.eventWorkTypeId || eventWorkTypeId || ''))?.label || idValue(detail.eventWorkTypeId || eventWorkTypeId || '-')}</Descriptions.Item>
              <Descriptions.Item label="状态"><Tag color={detail.status === 'pending_confirm' ? 'warning' : detail.status === 'confirmed' ? 'success' : 'error'}>{detail.status === 'pending_confirm' ? '待确认' : detail.status === 'confirmed' ? '已确认' : '校验失败'}</Tag></Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDate(detail.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="总数">{detail.totalCount}</Descriptions.Item>
              <Descriptions.Item label="有效数量">{detail.validCount}</Descriptions.Item>
              <Descriptions.Item label="无效数量">{detail.invalidCount}</Descriptions.Item>
              <Descriptions.Item label="说明">{detail.summary || '-'}</Descriptions.Item>
            </Descriptions>
            <Table<CompetitionRosterItem>
              rowKey={(record) => idValue(record.id)}
              loading={detailQuery.isFetching}
              dataSource={items}
              columns={itemColumns}
              scroll={{ x: 900 }}
              pagination={{
                current: itemQuery.page,
                pageSize: itemQuery.pageSize,
                total: detail.items?.total || 0,
                showSizeChanger: true,
                onChange: (page, pageSize) =>
                  setItemQuery((prev) => ({
                    ...prev,
                    page: typeof pageSize === 'number' && pageSize !== prev.pageSize ? 1 : page,
                    pageSize: typeof pageSize === 'number' && pageSize > 0 ? pageSize : prev.pageSize,
                  })),
              }}
            />
          </>}
        </Card>
      )}
    </CompetitionEventFrame>
  );
}
