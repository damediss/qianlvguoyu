// 管理端 3 页面共享的"题库审核指派"表格组件
// 复用 DevTaskDetailPage 的 UI 风格(antd Table + Modal + Drawer + Tag)
// 根据 taskType 切换 API 调用,根据 permissionPrefix 切换权限点

import {
  DeleteOutlined,
  ExportOutlined,
  ReloadOutlined,
  TableOutlined,
} from '@ant-design/icons';
import {
  Button,
  Drawer,
  Descriptions,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  pageTaskExamAudits,
  getTaskExamAuditDetails,
  deleteTaskExamAudit,
  assignTaskExamAuditExpert,
  getTaskExamAuditDaily,
  exportTaskExamAudit,
  pageTaskExamshicaoAudits,
  getTaskExamshicaoAuditDetails,
  deleteTaskExamshicaoAudit,
  assignTaskExamshicaoAuditExpert,
  getTaskExamshicaoAuditDaily,
  exportTaskExamshicaoAudit,
  pageTaskExamsubAudits,
  getTaskExamsubAuditDetails,
  deleteTaskExamsubAudit,
  assignTaskExamsubAuditExpert,
  getTaskExamsubAuditDaily,
  exportTaskExamsubAudit,
  getPublicData,
} from '../../../api/exam';
import { useAuthStore } from '../../../stores/authStore';
import type {
  TaskAuditVO,
  TaskAuditDetailItem,
  TaskAuditDetailChild,
  TaskAuditDaily,
  TaskAuditAssignExpertReq,
  TaskAuditType,
  TaskAuditRole,
  TaskAuditDetailColumnLabels,
} from '../../../types/exam';
import { STATUS_MAP, ROLE_MAP } from './constants';

export interface TaskAuditAssignTableProps {
  taskType: TaskAuditType;
  permissionPrefix: string; // e.g. "task:exam-audit"
  title: string;             // 页面标题
  detailColumnLabels: TaskAuditDetailColumnLabels;
}

// API 函数映射表(按 taskType 分发)
const API_MAP = {
  exam: {
    page: pageTaskExamAudits,
    details: getTaskExamAuditDetails,
    del: deleteTaskExamAudit,
    assign: assignTaskExamAuditExpert,
    daily: getTaskExamAuditDaily,
    export: exportTaskExamAudit,
    queryKey: 'task-exam-audits',
  },
  examshicao: {
    page: pageTaskExamshicaoAudits,
    details: getTaskExamshicaoAuditDetails,
    del: deleteTaskExamshicaoAudit,
    assign: assignTaskExamshicaoAuditExpert,
    daily: getTaskExamshicaoAuditDaily,
    export: exportTaskExamshicaoAudit,
    queryKey: 'task-examshicao-audits',
  },
  examsub: {
    page: pageTaskExamsubAudits,
    details: getTaskExamsubAuditDetails,
    del: deleteTaskExamsubAudit,
    assign: assignTaskExamsubAuditExpert,
    daily: getTaskExamsubAuditDaily,
    export: exportTaskExamsubAudit,
    queryKey: 'task-examsub-audits',
  },
} as const;

export function TaskAuditAssignTable({
  taskType,
  permissionPrefix,
  title,
  detailColumnLabels,
}: TaskAuditAssignTableProps) {
  const api = API_MAP[taskType];
  const qc = useQueryClient();
  const hasPerm = useAuthStore((s) => s.hasPermission);

  // 分页
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // 当前行
  const [currentRow, setCurrentRow] = useState<TaskAuditVO | null>(null);

  // 详情 Drawer
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailData, setDetailData] = useState<TaskAuditDetailItem[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);

  // 指派专家 Modal
  const [assignOpen, setAssignOpen] = useState(false);
  const [assignRole, setAssignRole] = useState<TaskAuditRole>(0);
  const [selectedExpertId, setSelectedExpertId] = useState<number | undefined>();

  // 日统计 Modal
  const [dailyOpen, setDailyOpen] = useState(false);
  const [dailyData, setDailyData] = useState<TaskAuditDaily[]>([]);
  const [dailyLoading, setDailyLoading] = useState(false);

  // 公共数据(工种 + 专家)
  const publicQuery = useQuery({
    queryKey: ['public-data'],
    queryFn: getPublicData,
    staleTime: 300_000,
  });

  // 任务列表
  const listQuery = useQuery({
    queryKey: [api.queryKey, page, pageSize],
    queryFn: () => api.page({ page, pageSize }),
  });

  // 删除
  const delMut = useMutation({
    mutationFn: (id: number) => api.del(id),
    onSuccess: () => {
      message.success('删除成功');
      qc.invalidateQueries({ queryKey: [api.queryKey] });
    },
  });

  // 指派
  const assignMut = useMutation({
    mutationFn: (args: { taskId: number; expertId: number; role: TaskAuditRole }) => {
      const payload: TaskAuditAssignExpertReq = { taskId: args.taskId, expertId: args.expertId, role: args.role };
      return api.assign(args.taskId, payload);
    },
    onSuccess: () => {
      message.success('指派成功');
      setAssignOpen(false);
      qc.invalidateQueries({ queryKey: [api.queryKey] });
    },
  });

  // 导出
  const handleExport = async (row: TaskAuditVO) => {
    try {
      const resp = await api.export(row.id);
      const blob = resp.data as Blob;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${taskType}_${row.id}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('导出失败');
    }
  };

  // 显示详情
  const handleShowDetails = async (row: TaskAuditVO) => {
    setCurrentRow(row);
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const data = await api.details(row.id);
      setDetailData(data);
    } catch {
      message.error('获取明细失败');
    } finally {
      setDetailLoading(false);
    }
  };

  // 指派
  const handleAssign = (row: TaskAuditVO, role: TaskAuditRole) => {
    setCurrentRow(row);
    setAssignRole(role);
    setAssignOpen(true);
    setSelectedExpertId(undefined);
  };

  // 日统计
  const handleShowDaily = async (row: TaskAuditVO, role: TaskAuditRole) => {
    setCurrentRow(row);
    setDailyOpen(true);
    setDailyLoading(true);
    try {
      const data = await api.daily(row.id, role);
      setDailyData(data);
    } catch {
      message.error('获取每日审核量失败');
    } finally {
      setDailyLoading(false);
    }
  };

  // 删除
  const handleDelete = (row: TaskAuditVO) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除该任务吗?关联的明细和审核记录也将被删除。',
      okText: '删除',
      okButtonProps: { danger: true },
      onOk: () => delMut.mutate(row.id),
    });
  };

  // 列表项
  const records: TaskAuditVO[] = listQuery.data?.items ?? [];
  const total = listQuery.data?.total ?? 0;

  // 详情列
  const detailColumns: ColumnsType<TaskAuditDetailItem> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: detailColumnLabels.examType ?? '题型', dataIndex: 'examType', width: 100 },
    {
      title: detailColumnLabels.examContent ?? '题干',
      dataIndex: 'examContent',
      width: 240,
      render: (v: string | undefined, r: TaskAuditDetailItem) => v || r.content || '-',
    },
    {
      title: detailColumnLabels.examAnswer ?? '答案',
      dataIndex: 'examAnswer',
      width: 120,
      render: (v: string | undefined, r: TaskAuditDetailItem) => v || r.answer || '-',
    },
    { title: '重点', dataIndex: 'isblod', width: 60, render: (v: number | undefined) => (v ? <Tag color="red">是</Tag> : '否') },
  ];

  // 详情行渲染:显示初/终审 child
  const expandedDetailColumns: ColumnsType<TaskAuditDetailChild> = [
    { title: '审核阶段', dataIndex: 'id', width: 100 },
    {
      title: '审核专家',
      dataIndex: 'expertName',
      width: 120,
      render: (v: string | undefined) => v || '-',
    },
    {
      title: detailColumnLabels.examContent ?? '审核内容',
      dataIndex: 'auditContent',
      render: (v: string | undefined, r: TaskAuditDetailChild) =>
        v || r.auditAnswer || r.auditPingfen || r.auditKaochang || r.auditKaosheng || '-',
    },
    {
      title: '提交时间',
      dataIndex: 'addtimeCn',
      width: 160,
      render: (v: string | undefined) => v || '-',
    },
  ];

  // 列表列
  const columns: ColumnsType<TaskAuditVO> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '工种',
      key: 'workType',
      width: 220,
      render: (_, r) => (
        <Tag>{r.workTypeCn}({r.levelCn})</Tag>
      ),
    },
    { title: '题量', dataIndex: 'nums', width: 80 },
    {
      title: '审核专家',
      key: 'experts',
      width: 280,
      render: (_, r) => (
        <Space direction="vertical" size={4}>
          <Space size={4}>
            {r.expertStartName === '待指派' ? (
              <Tag
                style={{ cursor: hasPerm(`${permissionPrefix}:assign-expert`) ? 'pointer' : 'default' }}
                color="default"
                onClick={() => hasPerm(`${permissionPrefix}:assign-expert`) && handleAssign(r, 0)}
              >
                初审:{r.expertStartName}
              </Tag>
            ) : (
              <>
                <Tag color="green">初审:{hasPerm(`${permissionPrefix}:assign-expert`) ? r.expertStartName : '***'}</Tag>
                <a onClick={() => handleShowDaily(r, 0)} style={{ fontSize: 12 }}>每日审核量</a>
              </>
            )}
          </Space>
          <Space size={4}>
            {r.expertEndName === '待指派' ? (
              <Tag
                style={{ cursor: hasPerm(`${permissionPrefix}:assign-expert`) ? 'pointer' : 'default' }}
                color="default"
                onClick={() => hasPerm(`${permissionPrefix}:assign-expert`) && handleAssign(r, 1)}
              >
                终审:{r.expertEndName}
              </Tag>
            ) : (
              <>
                <Tag color="red">终审:{hasPerm(`${permissionPrefix}:assign-expert`) ? r.expertEndName : '***'}</Tag>
                <a onClick={() => handleShowDaily(r, 1)} style={{ fontSize: 12 }}>每日审核量</a>
              </>
            )}
          </Space>
        </Space>
      ),
    },
    {
      title: '审核状态',
      dataIndex: 'statusCn',
      width: 120,
      render: (v: string, r: TaskAuditVO) => (
        <Tag color={STATUS_MAP[r.status as keyof typeof STATUS_MAP]?.color ?? 'default'}>{v}</Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'addtimeCn', width: 120 },
    {
      title: '操作',
      key: 'actions',
      width: 320,
      fixed: 'right',
      render: (_, r) => (
        <Space wrap size={4}>
          <Button size="small" icon={<TableOutlined />} onClick={() => handleShowDetails(r)}>
            明细
          </Button>
          {hasPerm(`${permissionPrefix}:export`) && (
            <Button size="small" icon={<ExportOutlined />} onClick={() => handleExport(r)}>
              导出
            </Button>
          )}
          {r.status === 0 && hasPerm(`${permissionPrefix}:delete`) && (
            <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r)}>
              删除
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // 日统计列
  const dailyCols: ColumnsType<TaskAuditDaily> = [
    { title: '日期', dataIndex: 'ymd', width: 120 },
    { title: '题量', dataIndex: 'nums', width: 80 },
  ];

  return (
    <div style={{ padding: 16 }}>
      <Typography.Title level={4} style={{ marginBottom: 12 }}>
        <TableOutlined /> {title}
      </Typography.Title>

      <Space style={{ marginBottom: 12 }}>
        <Button icon={<ReloadOutlined />} onClick={() => qc.invalidateQueries({ queryKey: [api.queryKey] })}>
          刷新
        </Button>
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={records}
        loading={listQuery.isFetching}
        scroll={{ x: 1200 }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
      />

      {/* 详情 Drawer */}
      <Drawer
        title={`任务明细 - ID: ${currentRow?.id ?? '-'}`}
        open={detailOpen}
        onClose={() => {
          setDetailOpen(false);
          setDetailData([]);
        }}
        width="85%"
        destroyOnClose
      >
        <Descriptions bordered column={2} size="small" style={{ marginBottom: 12 }}>
          <Descriptions.Item label="工种">{currentRow?.workTypeCn}</Descriptions.Item>
          <Descriptions.Item label="级别">{currentRow?.levelCn}</Descriptions.Item>
          <Descriptions.Item label="题量">{currentRow?.nums}</Descriptions.Item>
          <Descriptions.Item label="状态">{currentRow?.statusCn}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{currentRow?.addtimeCn}</Descriptions.Item>
          <Descriptions.Item label="任务ID">{currentRow?.id}</Descriptions.Item>
        </Descriptions>

        <Table
          rowKey="id"
          columns={detailColumns}
          dataSource={detailData}
          loading={detailLoading}
          expandable={{
            expandedRowRender: (record) => (
              <Table
                rowKey="id"
                columns={expandedDetailColumns}
                dataSource={record.children ?? []}
                pagination={false}
                size="small"
                rowClassName={(r) => {
                  if (r.id === '初审') return 'row-start';
                  if (r.id === '终审') return 'row-end';
                  return '';
                }}
              />
            ),
          }}
          pagination={false}
          scroll={{ x: 800, y: 500 }}
        />
      </Drawer>

      {/* 指派专家 Modal */}
      <Modal
        title="指派专家"
        open={assignOpen}
        onCancel={() => setAssignOpen(false)}
        width={455}
        footer={[
          <Button key="cancel" onClick={() => setAssignOpen(false)}>取消</Button>,
          <Button
            key="confirm"
            type="primary"
            disabled={!selectedExpertId}
            onClick={() => {
              if (currentRow && selectedExpertId) {
                assignMut.mutate({ taskId: currentRow.id, expertId: selectedExpertId, role: assignRole });
              }
            }}
          >确定</Button>,
        ]}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
          <span>为「{currentRow?.workTypeCn}({currentRow?.levelCn})」指派</span>
          <Tag style={{ marginRight: 2 }} color={assignRole > 0 ? 'red' : 'green'}>
            {ROLE_MAP[assignRole]?.label ?? (assignRole > 0 ? '终审' : '初审')}
          </Tag>
          <Select
            showSearch
            style={{ width: 200 }}
            value={selectedExpertId}
            onChange={(val) => setSelectedExpertId(val)}
            filterOption={(input, option) =>
              (option?.label as string)?.includes(input) ?? false
            }
            options={(publicQuery.data?.expertList ?? []).map((e) => ({
              label: e.nickname,
              value: e.id,
            }))}
            placeholder="选择专家"
          />
        </div>
      </Modal>

      {/* 日统计 Modal */}
      <Modal
        title="每日审核量"
        open={dailyOpen}
        onCancel={() => {
          setDailyOpen(false);
          setDailyData([]);
        }}
        width={350}
        footer={null}
      >
        <Table
          rowKey="ymd"
          columns={dailyCols}
          dataSource={dailyData}
          loading={dailyLoading}
          pagination={false}
          size="small"
        />
      </Modal>

      <style>{`
        .row-start { background-color: #e1f3d8 !important; }
        .row-end { background-color: #fde2e2 !important; }
      `}</style>
    </div>
  );
}
