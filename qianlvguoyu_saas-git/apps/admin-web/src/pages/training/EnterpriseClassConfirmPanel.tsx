import React, { useState } from 'react';
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Input,
  Modal,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CheckOutlined,
  CloseOutlined,
  EyeOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import * as trainApi from '../../api/training';
import type {
  ClassStudent,
  DivisionAutoPackClassMember,
  DivisionPackTaskResult,
  DivisionPackTaskStatus,
  TrainInfoClassListItem,
} from '../../types/training';

const { Paragraph, Text } = Typography;

const PACK_STATUS_MAP: Record<string, { label: string; color: string }> = {
  pending: { label: '待确认', color: 'warning' },
  rejected: { label: '已驳回', color: 'error' },
  materialized: { label: '已通过并建班', color: 'success' },
  draft: { label: '机构草稿', color: 'default' },
  superseded: { label: '已作废', color: 'default' },
};

const CLASS_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待确认', color: 'warning' },
  1: { label: '已确认', color: 'success' },
  2: { label: '已驳回', color: 'error' },
};

type PackTab = 'pending' | 'rejected' | 'materialized' | 'all';
type LegacyTab = 'pending' | 'confirmed' | 'rejected' | 'all';
type MainTab = 'pack' | 'legacy';

function packTabStatus(tab: PackTab): DivisionPackTaskStatus | undefined {
  if (tab === 'pending') return 'pending';
  if (tab === 'rejected') return 'rejected';
  if (tab === 'materialized') return 'materialized';
  return undefined;
}

function legacyTabStatus(tab: LegacyTab): number | undefined {
  if (tab === 'pending') return 0;
  if (tab === 'confirmed') return 1;
  if (tab === 'rejected') return 2;
  return undefined;
}

function formatTime(value?: string): string {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

export function EnterpriseClassConfirmPanel() {
  const queryClient = useQueryClient();
  const [mainTab, setMainTab] = useState<MainTab>('pack');

  const [packTab, setPackTab] = useState<PackTab>('pending');
  const [packPage, setPackPage] = useState(1);
  const [packPageSize, setPackPageSize] = useState(10);
  const [packKeyword, setPackKeyword] = useState('');

  const [detailOpen, setDetailOpen] = useState(false);
  const [currentTask, setCurrentTask] = useState<DivisionPackTaskResult | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const [legacyTab, setLegacyTab] = useState<LegacyTab>('pending');
  const [legacyPage, setLegacyPage] = useState(1);
  const [legacyPageSize, setLegacyPageSize] = useState(10);
  const [legacyTitleFilter, setLegacyTitleFilter] = useState('');
  const [legacyDetailOpen, setLegacyDetailOpen] = useState(false);
  const [currentClass, setCurrentClass] = useState<TrainInfoClassListItem | null>(null);
  const [studentPage, setStudentPage] = useState(1);

  const packListQuery = useQuery({
    queryKey: ['enterprise-pack-confirm', packTab, packPage, packPageSize],
    queryFn: () => trainApi.pageDivisionPackTasks({
      page: packPage,
      pageSize: packPageSize,
      status: packTabStatus(packTab),
    }),
    enabled: mainTab === 'pack',
  });

  const packDetailQuery = useQuery({
    queryKey: ['enterprise-pack-detail', currentTask?.taskId],
    queryFn: () => trainApi.getDivisionPackTask(currentTask!.taskId),
    enabled: detailOpen && !!currentTask?.taskId,
  });

  const legacyListQuery = useQuery({
    queryKey: ['enterprise-class-confirm', legacyTab, legacyPage, legacyPageSize, legacyTitleFilter],
    queryFn: () => trainApi.pageInfoClassList({
      page: legacyPage,
      pageSize: legacyPageSize,
      companyConfirmStatus: legacyTabStatus(legacyTab),
    }),
    enabled: mainTab === 'legacy',
  });

  const studentsQuery = useQuery({
    queryKey: ['enterprise-class-students', currentClass?.id, studentPage],
    queryFn: () => trainApi.listClassStudents(currentClass!.id, studentPage, 20),
    enabled: legacyDetailOpen && !!currentClass?.id,
  });

  const refreshPack = () => {
    queryClient.invalidateQueries({ queryKey: ['enterprise-pack-confirm'] });
    queryClient.invalidateQueries({ queryKey: ['enterprise-pack-detail'] });
    queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
  };

  const refreshLegacy = () => {
    queryClient.invalidateQueries({ queryKey: ['enterprise-class-confirm'] });
  };

  const detail = packDetailQuery.data ?? currentTask;
  const packItems = (packListQuery.data?.items ?? []).filter((row) => {
    if (packTab === 'all' && (row.status === 'draft' || row.status === 'superseded')) {
      return false;
    }
    if (!packKeyword.trim()) return true;
    const kw = packKeyword.trim().toLowerCase();
    return String(row.taskId).includes(kw)
      || String(row.trainInfoId).includes(kw)
      || (row.classNamePrefix ?? '').toLowerCase().includes(kw);
  });

  const rawLegacy = legacyListQuery.data?.list ?? [];
  const legacyList = legacyTab === 'all'
    ? rawLegacy
    : rawLegacy.filter((row) => (row.company_confirm_status ?? 0) === legacyTabStatus(legacyTab));
  const filteredLegacy = legacyTitleFilter.trim()
    ? legacyList.filter((row) => {
      const kw = legacyTitleFilter.trim().toLowerCase();
      return (row.title ?? '').toLowerCase().includes(kw)
        || (row.train_title ?? '').toLowerCase().includes(kw);
    })
    : legacyList;

  const runPackAction = async (fn: () => Promise<unknown>, successMsg: string) => {
    setActionLoading(true);
    try {
      await fn();
      message.success(successMsg);
      refreshPack();
      setDetailOpen(false);
      setCurrentTask(null);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const runLegacyAction = async (fn: () => Promise<unknown>, successMsg: string) => {
    setActionLoading(true);
    try {
      await fn();
      message.success(successMsg);
      refreshLegacy();
      if (currentClass?.id) {
        queryClient.invalidateQueries({ queryKey: ['enterprise-class-students', currentClass.id] });
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const packColumns: ColumnsType<DivisionPackTaskResult> = [
    { title: '方案号', dataIndex: 'taskId', width: 90 },
    { title: '培训信息', dataIndex: 'trainInfoId', width: 100 },
    {
      title: '班级',
      width: 100,
      render: (_, row) => `${row.classCount} 班`,
    },
    {
      title: '人数',
      width: 90,
      render: (_, row) => row.assignedCount,
    },
    {
      title: '策略',
      width: 120,
      render: (_, row) => {
        if (row.strategy === 'manual') return '手动组班';
        if (row.strategy === 'by_job_level') return '工种/等级';
        if (row.strategy === 'by_job_level_slot') return '工种/等级+时段';
        if (row.strategy === 'by_shift_team') return '按班组';
        return '名单顺序';
      },
    },
    {
      title: '状态',
      width: 130,
      render: (_, row) => {
        const meta = PACK_STATUS_MAP[row.status] ?? { label: row.statusLabel || row.status, color: 'default' };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    {
      title: '提交时间',
      width: 170,
      render: (_, row) => formatTime(row.updateTime),
    },
    {
      title: '操作',
      width: 100,
      fixed: 'right',
      render: (_, row) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => {
            setCurrentTask(row);
            setDetailOpen(true);
          }}
        >
          查看
        </Button>
      ),
    },
  ];

  const legacyColumns: ColumnsType<TrainInfoClassListItem> = [
    { title: '班级名称', dataIndex: 'title', ellipsis: true },
    { title: '培训项目', dataIndex: 'train_title', width: 180, ellipsis: true },
    { title: '工种', dataIndex: 'work_type_title', width: 120, ellipsis: true },
    { title: '人数', dataIndex: 'nums', width: 70 },
    {
      title: '状态',
      width: 100,
      render: (_, row) => {
        const code = row.company_confirm_status ?? 0;
        const meta = CLASS_STATUS_MAP[code] ?? { label: '未知', color: 'default' };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    {
      title: '操作',
      width: 100,
      fixed: 'right',
      render: (_, row) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => {
            setCurrentClass(row);
            setStudentPage(1);
            setLegacyDetailOpen(true);
          }}
        >
          查看
        </Button>
      ),
    },
  ];

  const studentColumns: ColumnsType<ClassStudent> = [
    { title: '姓名', dataIndex: 'realname', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '身份证号', dataIndex: 'id_card', ellipsis: true },
  ];

  const memberColumns: ColumnsType<DivisionAutoPackClassMember> = [
    { title: '姓名', dataIndex: 'personName', width: 90 },
    { title: '单位', dataIndex: 'workshop', width: 120, ellipsis: true, render: (v) => v || '-' },
    { title: '工种', dataIndex: 'jobName', width: 110, ellipsis: true, render: (v) => v || '-' },
    { title: '等级', dataIndex: 'levelName', width: 90, render: (v) => v || '-' },
    { title: '班组', dataIndex: 'shiftTeam', width: 90, render: (v) => v || '-' },
    { title: '身份证', dataIndex: 'idCard', width: 170 },
  ];

  const confirmStatus = currentClass?.company_confirm_status ?? 0;

  return (
    <>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        优先审核机构提交的分班方案；通过后系统才创建培训班级。旧版「已建班级确认」仍保留在下方标签中。
      </Paragraph>

      <Tabs
        activeKey={mainTab}
        onChange={(key) => setMainTab(key as MainTab)}
        items={[
          { key: 'pack', label: '分班方案审核' },
          { key: 'legacy', label: '班级确认（兼容）' },
        ]}
      />

      {mainTab === 'pack' && (
        <>
          <Tabs
            activeKey={packTab}
            onChange={(key) => {
              setPackTab(key as PackTab);
              setPackPage(1);
            }}
            items={[
              { key: 'pending', label: '待确认' },
              { key: 'rejected', label: '已驳回' },
              { key: 'materialized', label: '已通过建班' },
              { key: 'all', label: '全部' },
            ]}
          />

          <Space style={{ marginBottom: 16 }} wrap>
            <Input
              placeholder="搜索方案号 / 培训信息"
              allowClear
              style={{ width: 240 }}
              value={packKeyword}
              onChange={(e) => setPackKeyword(e.target.value)}
            />
            <Button onClick={() => packListQuery.refetch()}>刷新</Button>
          </Space>

          <Table<DivisionPackTaskResult>
            rowKey="taskId"
            columns={packColumns}
            dataSource={packItems}
            loading={packListQuery.isLoading}
            scroll={{ x: 900 }}
            pagination={{
              current: packPage,
              pageSize: packPageSize,
              total: packListQuery.data?.total ?? 0,
              onChange: (p, ps) => { setPackPage(p); setPackPageSize(ps); },
              showTotal: (t) => `共 ${t} 条`,
            }}
          />
        </>
      )}

      {mainTab === 'legacy' && (
        <>
          <Tabs
            activeKey={legacyTab}
            onChange={(key) => {
              setLegacyTab(key as LegacyTab);
              setLegacyPage(1);
            }}
            items={[
              { key: 'pending', label: '待确认' },
              { key: 'confirmed', label: '已确认' },
              { key: 'rejected', label: '已驳回' },
              { key: 'all', label: '全部记录' },
            ]}
          />
          <Space style={{ marginBottom: 16 }} wrap>
            <Input
              placeholder="搜索班级或培训项目"
              allowClear
              style={{ width: 240 }}
              value={legacyTitleFilter}
              onChange={(e) => setLegacyTitleFilter(e.target.value)}
            />
            <Button onClick={() => legacyListQuery.refetch()}>刷新</Button>
          </Space>
          <Table<TrainInfoClassListItem>
            rowKey="id"
            columns={legacyColumns}
            dataSource={filteredLegacy}
            loading={legacyListQuery.isLoading}
            scroll={{ x: 900 }}
            pagination={{
              current: legacyPage,
              pageSize: legacyPageSize,
              total: legacyTab === 'all' ? filteredLegacy.length : (legacyListQuery.data?.total ?? 0),
              onChange: (p, ps) => { setLegacyPage(p); setLegacyPageSize(ps); },
              showTotal: (t) => `共 ${t} 条`,
            }}
          />
        </>
      )}

      <Drawer
        title={detail ? `分班方案 #${detail.taskId}` : '分班方案'}
        width={920}
        open={detailOpen}
        onClose={() => { setDetailOpen(false); setCurrentTask(null); }}
        extra={detail?.status === 'pending' ? (
          <Space>
            <Button
              type="primary"
              icon={<CheckOutlined />}
              loading={actionLoading}
              onClick={() => runPackAction(
                () => trainApi.companyConfirmDivisionPackTask(detail.taskId),
                '已通过，培训班级已创建',
              )}
            >
              通过并建班
            </Button>
            <Button danger icon={<CloseOutlined />} onClick={() => setRejectOpen(true)}>
              驳回
            </Button>
          </Space>
        ) : null}
      >
        {detail && (
          <>
            <Alert
              type="info"
              showIcon
              icon={<TeamOutlined />}
              message="机构分班方案"
              description="请核对班级与学员名单。通过后系统将创建培训班级；驳回后机构可调整再提交。"
              style={{ marginBottom: 16 }}
            />
            <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="状态">
                <Tag color={PACK_STATUS_MAP[detail.status]?.color}>
                  {PACK_STATUS_MAP[detail.status]?.label || detail.statusLabel}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="培训信息">#{detail.trainInfoId}</Descriptions.Item>
              <Descriptions.Item label="班级数">{detail.classCount}</Descriptions.Item>
              <Descriptions.Item label="人数">{detail.assignedCount}</Descriptions.Item>
              <Descriptions.Item label="每班上限">{detail.classMax}</Descriptions.Item>
              <Descriptions.Item label="策略">
                {detail.strategy === 'manual' ? '手动组班'
                  : detail.strategy === 'by_job_level' ? '按工种/等级'
                    : detail.strategy === 'by_job_level_slot' ? '工种/等级+上下午时段'
                      : detail.strategy === 'by_shift_team' ? '按班组/倒班'
                        : '按名单顺序'}
              </Descriptions.Item>
              {detail.submitUsername && (
                <Descriptions.Item label="提交人" span={2}>
                  {detail.submitUsername} · {formatTime(detail.updateTime)}
                </Descriptions.Item>
              )}
              {detail.rejectReason && (
                <Descriptions.Item label="驳回理由" span={2}>
                  <Text type="danger">{detail.rejectReason}</Text>
                </Descriptions.Item>
              )}
              {detail.confirmUsername && (
                <Descriptions.Item label="审核人" span={2}>
                  {detail.confirmUsername} · {formatTime(detail.confirmTime)}
                </Descriptions.Item>
              )}
            </Descriptions>

            {(detail.classes ?? []).map((cls) => (
              <div key={cls.tempId || cls.title} style={{ marginBottom: 20 }}>
                <Typography.Title level={5} style={{ marginBottom: 8 }}>
                  {cls.title}
                  <Text type="secondary" style={{ marginLeft: 8, fontSize: 13, fontWeight: 400 }}>
                    {cls.members?.length ?? cls.nums} 人
                    {cls.group ? ` · ${cls.group}` : ''}
                  </Text>
                </Typography.Title>
                <Table<DivisionAutoPackClassMember>
                  rowKey="rosterId"
                  size="small"
                  pagination={false}
                  columns={memberColumns}
                  dataSource={cls.members ?? []}
                />
              </div>
            ))}

            {(detail.unassigned?.length ?? 0) > 0 && (
              <>
                <Typography.Title level={5}>未分入人员</Typography.Title>
                <Table
                  rowKey={(r) => `${r.rosterId}-${r.reason}`}
                  size="small"
                  pagination={{ pageSize: 8 }}
                  dataSource={detail.unassigned}
                  columns={[
                    { title: '姓名', dataIndex: 'personName', width: 100 },
                    { title: '身份证', dataIndex: 'idCard', width: 180 },
                    { title: '原因', dataIndex: 'reason' },
                  ]}
                />
              </>
            )}
          </>
        )}
      </Drawer>

      <Drawer
        title={currentClass ? `分班确认：${currentClass.title}` : '分班确认'}
        width={880}
        open={legacyDetailOpen}
        onClose={() => { setLegacyDetailOpen(false); setCurrentClass(null); }}
        extra={confirmStatus === 0 ? (
          <Space>
            <Button
              type="primary"
              icon={<CheckOutlined />}
              loading={actionLoading}
              onClick={() => runLegacyAction(
                () => trainApi.companyConfirmInfoClass(Number(currentClass!.id)),
                '已确认分班',
              )}
            >
              确认分班
            </Button>
            <Button
              danger
              icon={<CloseOutlined />}
              onClick={() => {
                setRejectOpen(true);
              }}
            >
              驳回
            </Button>
          </Space>
        ) : null}
      >
        {currentClass && (
          <>
            <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="班级名称">{currentClass.title}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={CLASS_STATUS_MAP[confirmStatus]?.color}>
                  {CLASS_STATUS_MAP[confirmStatus]?.label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="培训项目">{currentClass.train_title ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="计划人数">{currentClass.nums ?? '-'}</Descriptions.Item>
            </Descriptions>
            <Table<ClassStudent>
              rowKey="id"
              size="small"
              title={() => '班级学员名单'}
              columns={studentColumns}
              dataSource={studentsQuery.data?.list ?? []}
              loading={studentsQuery.isLoading}
              pagination={{
                current: studentPage,
                pageSize: 20,
                total: studentsQuery.data?.total ?? 0,
                onChange: (p) => setStudentPage(p),
              }}
            />
          </>
        )}
      </Drawer>

      <Modal
        title={mainTab === 'pack' || detailOpen ? '驳回分班方案' : '驳回分班'}
        open={rejectOpen}
        onCancel={() => { setRejectOpen(false); setRejectReason(''); }}
        onOk={async () => {
          if (!rejectReason.trim()) {
            message.warning('请填写驳回理由');
            return;
          }
          if (detailOpen && detail) {
            await runPackAction(
              () => trainApi.companyRejectDivisionPackTask(detail.taskId, rejectReason.trim()),
              '已驳回方案',
            );
          } else if (currentClass) {
            await runLegacyAction(
              () => trainApi.companyRejectInfoClass(Number(currentClass.id), rejectReason.trim()),
              '已驳回',
            );
            setLegacyDetailOpen(false);
            setCurrentClass(null);
          }
          setRejectOpen(false);
          setRejectReason('');
        }}
        confirmLoading={actionLoading}
        destroyOnClose
      >
        <Input.TextArea
          rows={4}
          placeholder="请填写驳回理由，机构将据此调整分班"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>
    </>
  );
}
