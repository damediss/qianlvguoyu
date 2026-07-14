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
import type { ClassStudent, TrainInfoClassListItem } from '../../types/training';

const { Paragraph } = Typography;

const CLASS_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待确认', color: 'warning' },
  1: { label: '已确认', color: 'success' },
  2: { label: '已驳回', color: 'error' },
};

type ClassTab = 'pending' | 'confirmed' | 'rejected' | 'all';

function tabConfirmStatus(tab: ClassTab): number | undefined {
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
  const [activeTab, setActiveTab] = useState<ClassTab>('pending');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [titleFilter, setTitleFilter] = useState('');

  const [detailOpen, setDetailOpen] = useState(false);
  const [currentClass, setCurrentClass] = useState<TrainInfoClassListItem | null>(null);
  const [studentPage, setStudentPage] = useState(1);
  const [actionLoading, setActionLoading] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const listQuery = useQuery({
    queryKey: ['enterprise-class-confirm', activeTab, page, pageSize, titleFilter],
    queryFn: () => trainApi.pageInfoClassList({
      page,
      pageSize,
      companyConfirmStatus: tabConfirmStatus(activeTab),
    }),
  });

  const studentsQuery = useQuery({
    queryKey: ['enterprise-class-students', currentClass?.id, studentPage],
    queryFn: () => trainApi.listClassStudents(currentClass!.id, studentPage, 20),
    enabled: detailOpen && !!currentClass?.id,
  });

  const refreshList = () => {
    queryClient.invalidateQueries({ queryKey: ['enterprise-class-confirm'] });
  };

  const rawList = listQuery.data?.list ?? [];
  const list = activeTab === 'all'
    ? rawList
    : rawList.filter((row) => (row.company_confirm_status ?? 0) === tabConfirmStatus(activeTab));

  const filteredList = titleFilter.trim()
    ? list.filter((row) => {
      const kw = titleFilter.trim().toLowerCase();
      return (row.title ?? '').toLowerCase().includes(kw)
        || (row.train_title ?? '').toLowerCase().includes(kw);
    })
    : list;

  const openDetail = (row: TrainInfoClassListItem) => {
    setCurrentClass(row);
    setStudentPage(1);
    setDetailOpen(true);
  };

  const runAction = async (fn: () => Promise<unknown>, successMsg: string) => {
    setActionLoading(true);
    try {
      await fn();
      message.success(successMsg);
      refreshList();
      if (currentClass?.id) {
        queryClient.invalidateQueries({ queryKey: ['enterprise-class-students', currentClass.id] });
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const columns: ColumnsType<TrainInfoClassListItem> = [
    { title: '班级名称', dataIndex: 'title', ellipsis: true },
    { title: '培训项目', dataIndex: 'train_title', width: 180, ellipsis: true },
    { title: '工种', dataIndex: 'work_type_title', width: 120, ellipsis: true },
    { title: '人数', dataIndex: 'nums', width: 70 },
    {
      title: '培训时间',
      width: 200,
      render: (_, row) => {
        if (!row.date_start && !row.date_end) return '-';
        return `${row.date_start ?? ''} ~ ${row.date_end ?? ''}`;
      },
    },
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
        <Button type="link" icon={<EyeOutlined />} onClick={() => openDetail(row)}>
          查看
        </Button>
      ),
    },
  ];

  const studentColumns: ColumnsType<ClassStudent> = [
    { title: '姓名', dataIndex: 'realname', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '身份证号', dataIndex: 'id_card', ellipsis: true },
    { title: '班长', dataIndex: 'is_leader_cn', width: 80 },
    { title: '班委', dataIndex: 'is_committee_cn', width: 80 },
  ];

  const confirmStatus = currentClass?.company_confirm_status ?? 0;

  return (
    <>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        查看培训机构为本企业划分的培训班级及学员名单，确认或驳回分班安排。
      </Paragraph>

      <Tabs
        activeKey={activeTab}
        onChange={(key) => {
          setActiveTab(key as ClassTab);
          setPage(1);
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
          value={titleFilter}
          onChange={(e) => setTitleFilter(e.target.value)}
        />
        <Button onClick={() => listQuery.refetch()}>刷新</Button>
      </Space>

      <Table<TrainInfoClassListItem>
        rowKey="id"
        columns={columns}
        dataSource={filteredList}
        loading={listQuery.isLoading}
        scroll={{ x: 1100 }}
        pagination={{
          current: page,
          pageSize,
          total: activeTab === 'all' ? filteredList.length : (listQuery.data?.total ?? 0),
          onChange: (p, ps) => { setPage(p); setPageSize(ps); },
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      <Drawer
        title={currentClass ? `分班确认：${currentClass.title}` : '分班确认'}
        width={880}
        open={detailOpen}
        onClose={() => { setDetailOpen(false); setCurrentClass(null); }}
        extra={confirmStatus === 0 ? (
          <Space>
            <Button
              type="primary"
              icon={<CheckOutlined />}
              loading={actionLoading}
              onClick={() => runAction(
                () => trainApi.companyConfirmInfoClass(currentClass!.id),
                '已确认分班',
              )}
            >
              确认分班
            </Button>
            <Button
              danger
              icon={<CloseOutlined />}
              onClick={() => setRejectOpen(true)}
            >
              驳回
            </Button>
          </Space>
        ) : null}
      >
        {currentClass && (
          <>
            <Alert
              type="info"
              showIcon
              icon={<TeamOutlined />}
              message="机构分班安排"
              description="请核对班级信息及学员名单，确认后机构将按此分班开展培训。"
              style={{ marginBottom: 16 }}
            />

            <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="班级名称">{currentClass.title}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={CLASS_STATUS_MAP[confirmStatus]?.color}>
                  {CLASS_STATUS_MAP[confirmStatus]?.label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="培训项目">{currentClass.train_title ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="工种">{currentClass.work_type_title ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="计划人数">{currentClass.nums ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="培训地点">{currentClass.position ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="开始日期">{currentClass.date_start ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="结束日期">{currentClass.date_end ?? '-'}</Descriptions.Item>
              {currentClass.company_reject_reason && (
                <Descriptions.Item label="驳回理由" span={2}>
                  {currentClass.company_reject_reason}
                </Descriptions.Item>
              )}
              {currentClass.company_confirm_username && (
                <Descriptions.Item label="确认信息" span={2}>
                  {currentClass.company_confirm_username} · {formatTime(currentClass.company_confirm_time)}
                </Descriptions.Item>
              )}
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
        title="驳回分班"
        open={rejectOpen}
        onCancel={() => { setRejectOpen(false); setRejectReason(''); }}
        onOk={async () => {
          if (!currentClass) return;
          if (!rejectReason.trim()) {
            message.warning('请填写驳回理由');
            return;
          }
          await runAction(
            () => trainApi.companyRejectInfoClass(currentClass.id, rejectReason.trim()),
            '已驳回',
          );
          setRejectOpen(false);
          setRejectReason('');
          setDetailOpen(false);
          setCurrentClass(null);
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
