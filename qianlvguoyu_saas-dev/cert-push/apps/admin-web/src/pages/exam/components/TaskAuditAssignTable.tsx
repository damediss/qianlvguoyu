// 管理端 3 页面共享的"题库审核指派"表格组件
// 复用 DevTaskDetailPage 的 UI 风格(antd Table + Modal + Drawer + Tag)
// 根据 taskType 切换 API 调用,根据 permissionPrefix 切换权限点

import {
  DeleteOutlined,
  DownloadOutlined,
  ExportOutlined,
  EyeOutlined,
  ReloadOutlined,
  TableOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import {
  Button,
  Drawer,
  Descriptions,
  Form,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  TreeSelect,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { CSSProperties } from 'react';
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
  importTaskExam,
  importTaskExamshicao,
  importTaskExamsub,
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
  PingfenItem,
} from '../../../types/exam';
import { STATUS_MAP, ROLE_MAP, LEVEL_OPTS } from './constants';
import { parseContentDispositionFilename } from '../../../utils/download';

// ===========================================================================
// 评分子表(pingfen JSON 数组)渲染
// 对应 PHP /yuntiku/task-examshicao-list 实操题"评分"列展开样式
// 6 列: 序号 / 要素 / 配分 / 评分标准 / 最大扣分 / 步长
// 缺字段填空(向后兼容 2 列简版)
// ===========================================================================
const PINGFEN_SUBHEADERS = ['序号', '要素', '配分', '评分标准', '最大扣分', '步长'] as const;
const PINGFEN_SUBWIDTHS = ['8%', '20%', '10%', '40%', '12%', '10%'] as const;
const PINGFEN_TABLE_STYLE: CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
  fontSize: 12,
};
const PINGFEN_TH_STYLE: CSSProperties = {
  border: '1px solid #d9d9d9',
  padding: '4px 8px',
  background: '#fafafa',
  fontWeight: 500,
  textAlign: 'left',
};
const PINGFEN_TD_STYLE: CSSProperties = {
  border: '1px solid #d9d9d9',
  padding: '4px 8px',
  verticalAlign: 'top',
};

function safeParsePingfen(v: unknown): PingfenItem[] | null {
  if (!v) return null;
  let raw: unknown = v;
  if (typeof v === 'string') {
    try {
      raw = JSON.parse(v);
    } catch {
      return null;
    }
  }
  if (!Array.isArray(raw)) return null;
  return raw as PingfenItem[];
}

function PingfenSubTable({ value }: { value: unknown }) {
  const items = safeParsePingfen(value);
  if (!items || items.length === 0) {
    return <span style={{ color: '#999' }}>-</span>;
  }
  return (
    <table style={PINGFEN_TABLE_STYLE}>
      <thead>
        <tr>
          {PINGFEN_SUBHEADERS.map((h, i) => (
            <th key={h} style={{ ...PINGFEN_TH_STYLE, width: PINGFEN_SUBWIDTHS[i] }}>{h}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {items.map((it, i) => (
          <tr key={i}>
            <td style={PINGFEN_TD_STYLE}>{i + 1}</td>
            <td style={PINGFEN_TD_STYLE}>{it.yaosu ?? '-'}</td>
            <td style={PINGFEN_TD_STYLE}>{it.peifen ?? '-'}</td>
            <td style={PINGFEN_TD_STYLE}>{it.biaozhun ?? '-'}</td>
            <td style={PINGFEN_TD_STYLE}>{it.koufen ?? '-'}</td>
            <td style={PINGFEN_TD_STYLE}>{it.buchang ?? '-'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function renderPingfenCell(v: unknown) {
  if (!v) return '-';
  return <PingfenSubTable value={v} />;
}

// 模板下载 URL(沿用 PHP 老 CDN 路径,3 题型各自一份)
const TEMPLATE_URLS: Record<TaskAuditType, string> = {
  exam: 'https://cdn.app.gjjd168.com/template/题库审核导入模板_理论.xlsx',
  examshicao: 'https://cdn.app.gjjd168.com/template/题库审核导入模板_实操.xlsx',
  examsub: 'https://cdn.app.gjjd168.com/template/题库审核导入模板_主观.xlsx',
};

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
    import: importTaskExam,
    queryKey: 'task-exam-audits',
  },
  examshicao: {
    page: pageTaskExamshicaoAudits,
    details: getTaskExamshicaoAuditDetails,
    del: deleteTaskExamshicaoAudit,
    assign: assignTaskExamshicaoAuditExpert,
    daily: getTaskExamshicaoAuditDaily,
    export: exportTaskExamshicaoAudit,
    import: importTaskExamshicao,
    queryKey: 'task-examshicao-audits',
  },
  examsub: {
    page: pageTaskExamsubAudits,
    details: getTaskExamsubAuditDetails,
    del: deleteTaskExamsubAudit,
    assign: assignTaskExamsubAuditExpert,
    daily: getTaskExamsubAuditDaily,
    export: exportTaskExamsubAudit,
    import: importTaskExamsub,
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

  // 导入任务 Modal
  const [importOpen, setImportOpen] = useState(false);
  const [importWorkTypeId, setImportWorkTypeId] = useState<number | null>(null);
  const [importLevel, setImportLevel] = useState<number | null>(null);
  const [importFileList, setImportFileList] = useState<any[]>([]);

  // 文件预览 Modal
  const [fileOpen, setFileOpen] = useState(false);
  const [fileUrl, setFileUrl] = useState('');
  const [fileDownUrl, setFileDownUrl] = useState('');

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

  // 导入
  const importMut = useMutation({
    mutationFn: async () => {
      if (!importWorkTypeId || !importLevel || importFileList.length === 0) {
        throw new Error('请完整填写工种、等级并选择文件');
      }
      const fd = new FormData();
      fd.append('file', importFileList[0].originFileObj as Blob);
      fd.append('workTypeId', String(importWorkTypeId));
      fd.append('level', String(importLevel));
      return api.import(fd);
    },
    onSuccess: (count: number) => {
      message.success(`成功导入 ${count} 条数据`);
      handleImportClose();
      qc.invalidateQueries({ queryKey: [api.queryKey] });
    },
    onError: (e: Error) => {
      message.error(e.message || '导入失败');
    },
  });

  const handleImportOpen = () => {
    setImportWorkTypeId(null);
    setImportLevel(null);
    setImportFileList([]);
    setImportOpen(true);
  };

  const handleImportClose = () => {
    setImportOpen(false);
    setImportWorkTypeId(null);
    setImportLevel(null);
    setImportFileList([]);
  };

  // 文件预览
  const handleShowFile = (row: TaskAuditVO) => {
    setCurrentRow(row);
    setFileUrl('https://view.officeapps.live.com/op/view.aspx?src=' + encodeURIComponent(row.fileUrl));
    setFileDownUrl(row.fileUrl);
    setFileOpen(true);
  };

  // 导出
  const handleExport = async (row: TaskAuditVO) => {
    try {
      const resp = await api.export(row.id);
      const blob = resp.data as Blob;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = parseContentDispositionFilename(resp) ?? `${row.id}_${row.workTypeCn}_${row.levelCn}_${taskType}.xlsx`;
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
    const isInProgress = row.status !== 0;
    const statusText = row.statusCn ?? '当前';
    Modal.confirm({
      title: '确认删除',
      content: isInProgress
        ? `该任务当前状态为「${statusText}」,删除将级联删除明细和已有的审核/专家分配记录(专家已提交的工作将丢失),不可恢复。确定删除吗?`
        : '确定要删除该任务吗?关联的明细和审核记录也将被删除。',
      okText: '删除',
      okButtonProps: { danger: true },
      onOk: () => delMut.mutate(row.id),
    });
  };

  // 列表项
  const records: TaskAuditVO[] = listQuery.data?.items ?? [];
  const total = listQuery.data?.total ?? 0;

  // 详情列: 按 taskType 三套(对应 PHP /yuntiku/task-* -list 各题型)
  //   exam        - ID / 题型 / 题干 / A~H / 答案 / 重点
  //   examshicao  - ID / 考评点 / 题干 / 考场准备 / 考生准备 / 评分(子表)
  //   examsub     - ID / 知识点 / 题干 / 参考答案 / 是否计算题
  const detailColumns: ColumnsType<TaskAuditDetailItem> = (() => {
    if (taskType === 'exam') {
      return [
        { title: 'ID', dataIndex: 'id', width: 60 },
        { title: detailColumnLabels.examType ?? '题型', dataIndex: 'examType', width: 100 },
        {
          title: detailColumnLabels.examContent ?? '题干',
          dataIndex: 'examContent',
          width: 240,
          render: (v: string | undefined, r: TaskAuditDetailItem) => v || r.content || '-',
        },
        { title: detailColumnLabels.examA ?? 'A', dataIndex: 'examA', width: 100, render: (v: string | undefined) => v || '-' },
        { title: detailColumnLabels.examB ?? 'B', dataIndex: 'examB', width: 100, render: (v: string | undefined) => v || '-' },
        { title: detailColumnLabels.examC ?? 'C', dataIndex: 'examC', width: 100, render: (v: string | undefined) => v || '-' },
        { title: detailColumnLabels.examD ?? 'D', dataIndex: 'examD', width: 100, render: (v: string | undefined) => v || '-' },
        { title: detailColumnLabels.examE ?? 'E', dataIndex: 'examE', width: 100, render: (v: string | undefined) => v || '-' },
        { title: detailColumnLabels.examF ?? 'F', dataIndex: 'examF', width: 100, render: (v: string | undefined) => v || '-' },
        { title: detailColumnLabels.examG ?? 'G', dataIndex: 'examG', width: 100, render: (v: string | undefined) => v || '-' },
        { title: detailColumnLabels.examH ?? 'H', dataIndex: 'examH', width: 100, render: (v: string | undefined) => v || '-' },
        {
          title: detailColumnLabels.examAnswer ?? '答案',
          dataIndex: 'examAnswer',
          width: 120,
          render: (v: string | undefined, r: TaskAuditDetailItem) => v || r.answer || '-',
        },
        { title: detailColumnLabels.isblod ?? '重点', dataIndex: 'isblod', width: 80, render: (v: number | undefined) => (v ? <Tag color="red">是</Tag> : '否') },
      ];
    }
    if (taskType === 'examshicao') {
      return [
        { title: 'ID', dataIndex: 'id', width: 60 },
        { title: detailColumnLabels.jianding ?? '考评点', dataIndex: 'jianding', width: 100, render: (v: string | undefined) => v || '-' },
        {
          title: detailColumnLabels.content ?? detailColumnLabels.examContent ?? '题干',
          dataIndex: 'content',
          width: 280,
          render: (v: string | undefined) => v || '-',
        },
        {
          title: detailColumnLabels.kaochang ?? '考场准备',
          dataIndex: 'kaochang',
          width: 200,
          render: (v: string | undefined) => v || '-',
        },
        {
          title: detailColumnLabels.kaosheng ?? '考生准备',
          dataIndex: 'kaosheng',
          width: 200,
          render: (v: string | undefined) => v || '-',
        },
        {
          title: detailColumnLabels.pingfen ?? '评分',
          dataIndex: 'pingfen',
          width: 600,
          render: renderPingfenCell,
        },
      ];
    }
    // examsub
    return [
      { title: 'ID', dataIndex: 'id', width: 60 },
      { title: detailColumnLabels.zhishidian ?? '知识点', dataIndex: 'zhishidian', width: 120, render: (v: string | undefined) => v || '-' },
      {
        title: detailColumnLabels.content ?? detailColumnLabels.examContent ?? '题干',
        dataIndex: 'content',
        width: 300,
        render: (v: string | undefined) => v || '-',
      },
      {
        title: detailColumnLabels.answer ?? detailColumnLabels.examAnswer ?? '参考答案',
        dataIndex: 'answer',
        width: 300,
        render: (v: string | undefined) => v || '-',
      },
      { title: detailColumnLabels.nanyicd ?? '难易程度', dataIndex: 'nanyicd', width: 100, render: (v: string | undefined) => v || '-' },
      { title: detailColumnLabels.isJisuan ?? '是否计算题', dataIndex: 'isJisuan', width: 100, render: (v: number | undefined) => (v ? <Tag color="blue">是</Tag> : '否') },
    ];
  })();

  // 详情行渲染:显示初/终审 child — 按 taskType 各自一套列
  const expandedDetailColumns: ColumnsType<TaskAuditDetailChild> = (() => {
    const base: ColumnsType<TaskAuditDetailChild> = [
      { title: '审核阶段', dataIndex: 'id', width: 100 },
      {
        title: '审核专家',
        dataIndex: 'expertName',
        width: 120,
        render: (v: string | undefined) => v || '-',
      },
    ];
    if (taskType === 'exam') {
      return [
        ...base,
        { title: '审核题干', dataIndex: 'examContent', width: 300, render: (v: string | undefined) => v || '-' },
        { title: '审核答案', dataIndex: 'examAnswer', width: 120, render: (v: string | undefined) => v || '-' },
        { title: '提交时间', dataIndex: 'addtimeCn', width: 160, render: (v: string | undefined) => v || '-' },
      ];
    }
    if (taskType === 'examshicao') {
      return [
        ...base,
        { title: '审核内容', dataIndex: 'content', width: 200, render: (v: string | undefined) => v || '-' },
        { title: '评分标准', dataIndex: 'pingfen', width: 300, render: renderPingfenCell },
        { title: '考场准备', dataIndex: 'kaochang', width: 200, render: (v: string | undefined) => v || '-' },
        { title: '考生准备', dataIndex: 'kaosheng', width: 200, render: (v: string | undefined) => v || '-' },
        { title: '提交时间', dataIndex: 'addtimeCn', width: 160, render: (v: string | undefined) => v || '-' },
      ];
    }
    // examsub
    return [
      ...base,
      { title: '审核内容', dataIndex: 'content', width: 300, render: (v: string | undefined) => v || '-' },
      { title: '审核答案', dataIndex: 'answer', width: 300, render: (v: string | undefined) => v || '-' },
      { title: '提交时间', dataIndex: 'addtimeCn', width: 160, render: (v: string | undefined) => v || '-' },
    ];
  })();

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
          {r.fileUrl && (
            <Button size="small" icon={<EyeOutlined />} onClick={() => handleShowFile(r)}>
              预览
            </Button>
          )}
          {hasPerm(`${permissionPrefix}:export`) && (
            <Button size="small" icon={<ExportOutlined />} onClick={() => handleExport(r)}>
              导出
            </Button>
          )}
          {hasPerm(`${permissionPrefix}:delete`) && (
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
        {hasPerm(`${permissionPrefix}:import`) && (
          <Button icon={<UploadOutlined />} type="primary" onClick={handleImportOpen}>
            导入
          </Button>
        )}
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
          scroll={{
            x: taskType === 'exam' ? 1600 : taskType === 'examshicao' ? 1500 : 1200,
            y: 500,
          }}
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

      {/* 导入任务 Modal */}
      <Modal
        title="导入任务"
        open={importOpen}
        onCancel={handleImportClose}
        width={520}
        footer={[
          <Button key="cancel" onClick={handleImportClose}>取消</Button>,
          <Button
            key="submit"
            type="primary"
            loading={importMut.isPending}
            disabled={!importWorkTypeId || !importLevel || importFileList.length === 0}
            onClick={() => importMut.mutate()}
          >确定导入</Button>,
        ]}
      >
        <Form layout="vertical" style={{ paddingTop: 8 }}>
          <Form.Item label="工种" required>
            <TreeSelect
              treeData={(publicQuery.data?.workTypeList ?? []) as any}
              fieldNames={{ label: 'title', value: 'id', children: 'children' }}
              value={importWorkTypeId ?? undefined}
              onChange={(v) => setImportWorkTypeId(typeof v === 'number' ? v : null)}
              placeholder="选择工种(支持搜索)"
              showSearch
              treeNodeFilterProp="title"
              treeDefaultExpandAll={false}
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item label="等级" required>
            <Select
              value={importLevel ?? undefined}
              onChange={(v) => setImportLevel(v)}
              placeholder="选择等级"
              style={{ width: '100%' }}
              options={LEVEL_OPTS}
            />
          </Form.Item>
          <Form.Item
            label="Excel 文件"
            required
            extra={<a href={TEMPLATE_URLS[taskType]} target="_blank" rel="noreferrer">下载模板</a>}
          >
            <Upload
              beforeUpload={() => false}
              fileList={importFileList}
              onChange={({ fileList }) => setImportFileList(fileList.slice(-1))}
              accept=".xlsx,.xls"
              maxCount={1}
            >
              <Button icon={<UploadOutlined />}>选择文件</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      {/* 文件预览 Modal */}
      <Modal
        title="文件预览"
        open={fileOpen}
        onCancel={() => { setFileOpen(false); setFileUrl(''); setFileDownUrl(''); }}
        width="85%"
        footer={null}
      >
        <div style={{ marginBottom: 8 }}>
          <a href={fileDownUrl} target="_blank" rel="noopener noreferrer">
            <Button icon={<DownloadOutlined />} size="small">下载文件</Button>
          </a>
        </div>
        <iframe src={fileUrl} style={{ width: '100%', height: 600, border: '1px solid #e0e0e0' }} />
      </Modal>
    </div>
  );
}
