import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Input,
  Modal,
  Space,
  Table,
  Tabs,
  Tag,
  Timeline,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CheckOutlined,
  CloseOutlined,
  CloudDownloadOutlined,
  EyeOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import * as trainApi from '../../api/training';
import type { ThreeYearPlanMaterialKind } from '../../api/training';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';
import type {
  ThreeYearPlan,
  ThreeYearPlanRejectHistoryItem,
} from '../../types/training';

const { Paragraph } = Typography;

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  3: { label: '待企业审核', color: 'warning' },
  4: { label: '企业已确认', color: 'success' },
  5: { label: '已驳回', color: 'error' },
};

type ReviewTab = 'pending' | 'confirmed' | 'rejected' | 'all';

function formatTime(value?: string): string {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

function tabStatus(tab: ReviewTab): number | undefined {
  if (tab === 'pending') return 3;
  if (tab === 'confirmed') return 4;
  if (tab === 'rejected') return 5;
  return undefined;
}

function fileExtFromName(fileName?: string): string | undefined {
  if (!fileName?.includes('.')) return undefined;
  return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
}

function renderRejectHistory(items?: ThreeYearPlanRejectHistoryItem[]) {
  if (!items || items.length === 0) {
    return <span style={{ color: '#999' }}>暂无驳回历史</span>;
  }
  return (
    <Timeline
      items={items
        .slice()
        .reverse()
        .map((item, idx) => ({
          key: idx,
          color: 'red',
          children: (
            <div>
              <div style={{ fontWeight: 600 }}>{item.reason || '（未填写理由）'}</div>
              <div style={{ fontSize: 12, color: '#999' }}>
                {item.rejectedByUsername || '企业'}
                {item.rejectedAt ? ` · ${formatTime(item.rejectedAt)}` : ''}
              </div>
            </div>
          ),
        }))}
    />
  );
}

export function EnterprisePlanReviewPanel() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<ReviewTab>('pending');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [titleFilter, setTitleFilter] = useState('');

  const [detailOpen, setDetailOpen] = useState(false);
  const [currentPlan, setCurrentPlan] = useState<ThreeYearPlan | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);

  const listQuery = useQuery({
    queryKey: ['enterprise-plan-review', activeTab, page, pageSize, titleFilter],
    queryFn: () => trainApi.pageThreeYearPlans({
      page,
      pageSize,
      status: tabStatus(activeTab),
      title: titleFilter || undefined,
    }),
  });

  const planDetailQuery = useQuery({
    queryKey: ['enterprise-plan-review-detail', currentPlan?.id],
    queryFn: () => trainApi.getThreeYearPlan(currentPlan!.id),
    enabled: detailOpen && !!currentPlan?.id,
    staleTime: 30_000,
  });

  const plan = planDetailQuery.data ?? currentPlan;

  const refreshList = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['enterprise-plan-review'] });
  }, [queryClient]);

  const openDetail = (row: ThreeYearPlan) => {
    setCurrentPlan(row);
    setDetailOpen(true);
  };

  const closeDetail = () => {
    setDetailOpen(false);
    setCurrentPlan(null);
    setPreviewTarget(null);
  };

  const openMaterialPreview = (
    planId: number,
    kind: ThreeYearPlanMaterialKind,
    fileName?: string,
    fallbackLabel?: string,
  ) => {
    setPreviewTarget({
      fileUrl: trainApi.threeYearPlanMaterialPreviewUrl(planId, kind),
      fileName: fileName || fallbackLabel,
      fileType: fileExtFromName(fileName),
    });
  };

  const invalidateDetail = (planId: number) => {
    queryClient.invalidateQueries({ queryKey: ['enterprise-plan-review-detail', planId] });
  };

  const runAction = async (fn: () => Promise<unknown>, successMsg: string) => {
    setActionLoading(true);
    try {
      await fn();
      message.success(successMsg);
      refreshList();
      if (currentPlan?.id) {
        invalidateDetail(currentPlan.id);
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  useEffect(() => {
    const latest = planDetailQuery.data;
    if (latest) {
      setCurrentPlan(latest);
    }
  }, [planDetailQuery.data]);

  const rawItems = listQuery.data?.items ?? [];
  const list = activeTab === 'all'
    ? rawItems.filter((p) => Number(p.status) >= 3)
    : rawItems;

  const columns: ColumnsType<ThreeYearPlan> = [
    { title: '规划编号', dataIndex: 'planNo', width: 180 },
    { title: '方案标题', dataIndex: 'title', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (status: number, row) => {
        const meta = STATUS_MAP[status] ?? { label: row.statusLabel ?? '未知', color: 'default' };
        return <Tag color={meta.color}>{row.statusLabel ?? meta.label}</Tag>;
      },
    },
    {
      title: '机构方案材料',
      width: 220,
      render: (_, row) => (
        <Space size="small" wrap>
          {row.status >= 3 && (
            <>
              <Button
                type="link"
                size="small"
                icon={<CloudDownloadOutlined />}
                onClick={() => trainApi.downloadThreeYearPlanFile(row.id, 'reviewed')}
              >
                规划表
              </Button>
              {row.submitSchemeFileId ? (
                <Button
                  type="link"
                  size="small"
                  onClick={() => trainApi.downloadThreeYearPlanFile(row.id, 'submit-scheme')}
                >
                  方案文档
                </Button>
              ) : null}
            </>
          )}
        </Space>
      ),
    },
    { title: '提交时间', dataIndex: 'updateTime', width: 170, render: formatTime },
    {
      title: '驳回理由',
      dataIndex: 'rejectReason',
      width: 220,
      ellipsis: true,
      render: (v: string) => v || '-',
    },
    {
      title: '操作',
      width: 100,
      fixed: 'right',
      render: (_, row) => (
        <Button type="link" icon={<EyeOutlined />} onClick={() => openDetail(row)}>
          审核
        </Button>
      ),
    },
  ];

  const renderDrawerActions = () => {
    if (!plan || plan.status !== 3) return null;
    return (
      <Space>
        <Button
          type="primary"
          icon={<CheckOutlined />}
          loading={actionLoading}
          onClick={() => runAction(
            () => trainApi.companyConfirmThreeYearPlan(plan.id),
            '已确认方案',
          )}
        >
          确认方案
        </Button>
        <Button
          danger
          icon={<CloseOutlined />}
          onClick={() => setRejectOpen(true)}
        >
          驳回
        </Button>
      </Space>
    );
  };

  return (
    <>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        查看培训机构提交的三年规划方案及相关材料，确认或驳回后进入下一流程。
      </Paragraph>

      <Tabs
        activeKey={activeTab}
        onChange={(key) => {
          setActiveTab(key as ReviewTab);
          setPage(1);
        }}
        items={[
          { key: 'pending', label: '待审核' },
          { key: 'confirmed', label: '已确认' },
          { key: 'rejected', label: '已驳回' },
          { key: 'all', label: '全部记录' },
        ]}
      />

      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          placeholder="搜索方案标题"
          allowClear
          style={{ width: 240 }}
          value={titleFilter}
          onChange={(e) => setTitleFilter(e.target.value)}
          onPressEnter={() => { setPage(1); listQuery.refetch(); }}
        />
        <Button onClick={() => { setPage(1); listQuery.refetch(); }}>查询</Button>
      </Space>

      <Table<ThreeYearPlan>
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={listQuery.isLoading}
        scroll={{ x: 1000 }}
        pagination={{
          current: page,
          pageSize,
          total: activeTab === 'all' ? list.length : (listQuery.data?.total ?? 0),
          onChange: (p, ps) => { setPage(p); setPageSize(ps); },
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      <Drawer
        title={plan ? `方案审核：${plan.title}` : '方案审核'}
        width={960}
        open={detailOpen}
        onClose={closeDetail}
        extra={renderDrawerActions()}
      >
        {plan && (
          <>
            {plan.status === 5 && plan.rejectReason && (
              <Alert
                type="error"
                showIcon
                style={{ marginBottom: 16 }}
                message="该方案已被驳回"
                description={
                  <div>
                    <p style={{ marginBottom: 4, color: '#666' }}>驳回理由：</p>
                    <p style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>{plan.rejectReason}</p>
                  </div>
                }
              />
            )}
            <Alert
              type="info"
              showIcon
              icon={<FileTextOutlined />}
              message="机构提交方案"
              description="以下为培训机构提交企业审核时固化的方案材料，请预览或下载查阅后确认或驳回。"
              style={{ marginBottom: 16 }}
            />

            <Card size="small" title="方案材料" style={{ marginBottom: 16 }}>
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                {plan.submitPersonnelFileId ? (
                  <Space wrap>
                    <Typography.Text>
                      企业审核人员名单
                      {plan.submitPersonnelFileName ? `（${plan.submitPersonnelFileName}）` : ''}
                    </Typography.Text>
                    <Button
                      type="link"
                      size="small"
                      icon={<EyeOutlined />}
                      onClick={() => openMaterialPreview(
                        plan.id,
                        'submit-personnel',
                        plan.submitPersonnelFileName,
                        '企业审核人员名单',
                      )}
                    >
                      预览
                    </Button>
                    <Button
                      type="link"
                      size="small"
                      icon={<CloudDownloadOutlined />}
                      onClick={() => trainApi.downloadThreeYearPlanFile(plan.id, 'submit-personnel')}
                    >
                      下载
                    </Button>
                  </Space>
                ) : null}
                {plan.submitSchemeFileId ? (
                  <Space wrap>
                    <Typography.Text>
                      方案文档
                      {plan.submitSchemeFileName ? `（${plan.submitSchemeFileName}）` : ''}
                    </Typography.Text>
                    <Button
                      type="link"
                      size="small"
                      icon={<EyeOutlined />}
                      onClick={() => openMaterialPreview(
                        plan.id,
                        'submit-scheme',
                        plan.submitSchemeFileName,
                        '方案文档',
                      )}
                    >
                      预览
                    </Button>
                    <Button
                      type="link"
                      size="small"
                      icon={<CloudDownloadOutlined />}
                      onClick={() => trainApi.downloadThreeYearPlanFile(plan.id, 'submit-scheme')}
                    >
                      下载
                    </Button>
                  </Space>
                ) : null}
                {!plan.submitPersonnelFileId && !plan.submitSchemeFileId ? (
                  <Typography.Text type="secondary">暂无方案材料</Typography.Text>
                ) : null}
              </Space>
            </Card>

            <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="规划编号">{plan.planNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_MAP[plan.status]?.color}>
                  {plan.statusLabel ?? STATUS_MAP[plan.status]?.label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="目标企业">{plan.targetOrgName}</Descriptions.Item>
              <Descriptions.Item label="规划年份">{plan.planYears}</Descriptions.Item>
              <Descriptions.Item label="推荐认定人数">{plan.recommendedRows ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="提交时间">{formatTime(plan.updateTime)}</Descriptions.Item>
              {plan.rejectReason && (
                <Descriptions.Item label="驳回理由" span={2}>{plan.rejectReason}</Descriptions.Item>
              )}
              {plan.companyConfirmUsername && (
                <Descriptions.Item label="确认信息" span={2}>
                  {plan.companyConfirmUsername} · {formatTime(plan.companyConfirmTime)}
                </Descriptions.Item>
              )}
            </Descriptions>

            {plan.rejectHistory && plan.rejectHistory.length > 0 && (
              <Card size="small" title="驳回历史" style={{ marginTop: 16 }}>
                {renderRejectHistory(plan.rejectHistory)}
              </Card>
            )}
          </>
        )}
      </Drawer>

      <FilePreviewModal
        open={previewTarget != null}
        target={previewTarget}
        onClose={() => setPreviewTarget(null)}
      />

      <Modal
        title="驳回方案"
        open={rejectOpen}
        onCancel={() => { setRejectOpen(false); setRejectReason(''); }}
        onOk={async () => {
          if (!plan) return;
          if (!rejectReason.trim()) {
            message.warning('请填写驳回理由');
            return;
          }
          await runAction(
            () => trainApi.companyRejectThreeYearPlan(plan.id, rejectReason.trim()),
            '已驳回',
          );
          setRejectOpen(false);
          setRejectReason('');
        }}
        confirmLoading={actionLoading}
        destroyOnClose
      >
        <Input.TextArea
          rows={4}
          placeholder="请填写驳回理由，机构将据此调整方案"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>
    </>
  );
}
