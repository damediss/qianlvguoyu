import {
  CloudUploadOutlined,
  DeleteOutlined,
  DownloadOutlined,
  ExportOutlined,
  EyeOutlined,
  FileSearchOutlined,
  PlusOutlined,
  ReloadOutlined,
  UserSwitchOutlined,
  TableOutlined,
} from '@ant-design/icons';
import {
  Button,
  Form,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useRef } from 'react';
import {
  pageTaskXimus,
  getTaskXimuDetails,
  getTaskXimuDaily,
  importTaskXimu,
  assignTaskXimuExpert,
  deleteTaskXimu,
  exportTaskXimu,
  getPublicData,
} from '../../api/exam';
import { useAuthStore } from '../../stores/authStore';
import { parseContentDispositionFilename } from '../../utils/download';
import type {
  ExamTaskXimu,
  ExamTaskXimuDetail,
  ExamTaskXimuDaily,
} from '../../types/exam';

const LEVEL_OPTS = [
  { label: '初级工', value: 5 },
  { label: '中级工', value: 4 },
  { label: '高级工', value: 3 },
  { label: '技师', value: 2 },
  { label: '高级技师', value: 1 },
];
const LEVEL_MAP: Record<number, string> = {
  1: '高级技师', 2: '技师', 3: '高级工', 4: '中级工', 5: '初级工',
};
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'default' },
  1: { label: '初审审核中', color: 'processing' },
  2: { label: '终审审核中', color: 'warning' },
  3: { label: '完成', color: 'success' },
};
const TYPE_OPTS = [
  { label: '理论', value: 0 },
  { label: '实操', value: 1 },
];

export function DevTaskDetailPage() {
  const qc = useQueryClient();
  const hasPerm = useAuthStore((s) => s.hasPermission);
  const token = useAuthStore((s) => s.token);

  // Page state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // Dialog states
  const [importOpen, setImportOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [fileOpen, setFileOpen] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [dailyOpen, setDailyOpen] = useState(false);

  // Working row
  const [currentRow, setCurrentRow] = useState<ExamTaskXimu | null>(null);
  const [assignRole, setAssignRole] = useState(0);
  const [selectedExpertId, setSelectedExpertId] = useState<number | undefined>();

  // Import form
  const [importWorkTypeId, setImportWorkTypeId] = useState<number | undefined>();
  const [importLevel, setImportLevel] = useState<number | undefined>();
  const [importTypes, setImportTypes] = useState<number>(0);
  const [importing, setImporting] = useState(false);

  // Detail data
  const [detailData, setDetailData] = useState<ExamTaskXimuDetail[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailKey, setDetailKey] = useState(0);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);

  // Daily data
  const [dailyData, setDailyData] = useState<ExamTaskXimuDaily[]>([]);
  const [dailyLoading, setDailyLoading] = useState(false);

  // Current file URL
  const [fileUrl, setFileUrl] = useState('');
  const [fileDownUrl, setFileDownUrl] = useState('');

  // Queries
  const listQuery = useQuery({
    queryKey: ['task-ximus', page, pageSize],
    queryFn: () => pageTaskXimus({ page, pageSize }),
  });

  const publicQuery = useQuery({
    queryKey: ['public-data'],
    queryFn: getPublicData,
    staleTime: 300_000,
  });

  const delMut = useMutation({
    mutationFn: deleteTaskXimu,
    onSuccess: () => {
      message.success('删除成功');
      qc.invalidateQueries({ queryKey: ['task-ximus'] });
    },
  });

  const assignMut = useMutation({
    mutationFn: (args: { taskId: number; expertId: number; role: number }) =>
      assignTaskXimuExpert(args.taskId, { taskId: args.taskId, expertId: args.expertId, role: args.role }),
    onSuccess: () => {
      message.success('指派成功');
      setAssignOpen(false);
      qc.invalidateQueries({ queryKey: ['task-ximus'] });
    },
  });

  const records = listQuery.data?.items ?? [];
  const total = listQuery.data?.total ?? 0;

  // Handlers
  const handleImport = async () => {
    if (!importWorkTypeId || !importLevel) {
      message.warning('请选择工种和等级');
      return;
    }
    setImporting(true);
    try {
      // Trigger file input
      const input = document.getElementById('ximu-upload-input') as HTMLInputElement;
      input?.click();
    } finally {
      setImporting(false);
    }
  };

  const handleFileSelected = useCallback(async (info: any) => {
    const file = info.file as File;
    if (!file) return;
    if (!importWorkTypeId || !importLevel) {
      message.warning('请先选择工种和等级');
      return;
    }
    setImporting(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      fd.append('workTypeId', String(importWorkTypeId));
      fd.append('level', String(importLevel));
      fd.append('types', String(importTypes));
      await importTaskXimu(fd);
      message.success('导入成功');
      setImportOpen(false);
      qc.invalidateQueries({ queryKey: ['task-ximus'] });
    } catch (e: any) {
      message.error(e?.message || '导入失败');
    } finally {
      setImporting(false);
    }
  }, [importWorkTypeId, importLevel, importTypes, qc]);

  const handleShowDetails = async (row: ExamTaskXimu) => {
    setCurrentRow(row);
    setDetailOpen(true);
    setDetailLoading(true);
    setDetailKey((k) => k + 1);
    try {
      const data = await getTaskXimuDetails(row.id);
      setDetailData(data);
      // 数据加载完成后，将所有父行 ID 设为展开
      setExpandedKeys(data.map((d) => d.id));
    } catch {
      message.error('获取细目列表失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const handleShowFile = (row: ExamTaskXimu) => {
    setCurrentRow(row);
    setFileUrl('https://view.officeapps.live.com/op/view.aspx?src=' + encodeURIComponent(row.fileUrl));
    setFileDownUrl(row.fileUrl);
    setFileOpen(true);
  };

  const handleAssign = (row: ExamTaskXimu, role: number) => {
    setCurrentRow(row);
    setAssignRole(role);
    setAssignOpen(true);
    setSelectedExpertId(undefined);
  };

  const handleShowDaily = async (row: ExamTaskXimu, role: number) => {
    setCurrentRow(row);
    setDailyOpen(true);
    setDailyLoading(true);
    try {
      const data = await getTaskXimuDaily(row.id, role);
      setDailyData(data);
    } catch {
      message.error('获取每日审核量失败');
    } finally {
      setDailyLoading(false);
    }
  };

  const handleExport = async (row: ExamTaskXimu) => {
    try {
      const resp = await exportTaskXimu(row.id);
      const blob = resp.data as Blob;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = parseContentDispositionFilename(resp) ?? `${row.id}_${row.workTypeCn}_${row.levelCn}_细目.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('导出失败');
    }
  };

  const handleDelete = (row: ExamTaskXimu) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除该细目表任务吗？关联的明细和审核记录也将被删除。`,
      okText: '删除',
      okButtonProps: { danger: true },
      onOk: () => delMut.mutate(row.id),
    });
  };

  // Table columns
  const columns: ColumnsType<ExamTaskXimu> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    {
      title: '工种',
      key: 'workType',
      width: 220,
      render: (_, r) => (
        <Tag>{r.workTypeCn}({r.levelCn}) - {r.typesCn}</Tag>
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
                style={{ cursor: hasPerm('task:ximu:assign-expert') ? 'pointer' : 'default' }}
                color="default"
                onClick={() => hasPerm('task:ximu:assign-expert') && handleAssign(r, 0)}
              >
                初审:{r.expertStartName}
              </Tag>
            ) : (
              <>
                <Tag color="green">初审:{hasPerm('task:ximu:assign-expert') ? r.expertStartName : '***'}</Tag>
                <a onClick={() => handleShowDaily(r, 0)} style={{ fontSize: 12 }}>每日审核量</a>
              </>
            )}
          </Space>
          <Space size={4}>
            {r.expertEndName === '待指派' ? (
              <Tag
                style={{ cursor: hasPerm('task:ximu:assign-expert') ? 'pointer' : 'default' }}
                color="default"
                onClick={() => hasPerm('task:ximu:assign-expert') && handleAssign(r, 1)}
              >
                终审:{r.expertEndName}
              </Tag>
            ) : (
              <>
                <Tag color="red">终审:{hasPerm('task:ximu:assign-expert') ? r.expertEndName : '***'}</Tag>
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
      render: (v: string, r: ExamTaskXimu) => (
        <Tag color={STATUS_MAP[r.status]?.color ?? 'default'}>{v}</Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'addtimeCn', width: 120 },
    {
      title: '操作',
      key: 'actions',
      width: 300,
      fixed: 'right',
      render: (_, r) => (
        <Space wrap size={4}>
          <Button size="small" icon={<TableOutlined />} onClick={() => handleShowDetails(r)}>
            细目表列表
          </Button>
          <Button size="small" icon={<EyeOutlined />} onClick={() => handleShowFile(r)}>
            细目表文件
          </Button>
          {hasPerm('task:ximu:export') && (
            <Button size="small" icon={<ExportOutlined />} onClick={() => handleExport(r)}>
              导出
            </Button>
          )}
          {r.status === 0 && hasPerm('task:ximu:delete') && (
            <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r)}>
              删除
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const detailCols: ColumnsType<ExamTaskXimuDetail> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '编号', dataIndex: 'sno', width: 100 },
    { title: '一级', dataIndex: 'sno0', width: 140 },
    { title: '二级', dataIndex: 'sno1', width: 140 },
    { title: '三级', dataIndex: 'title', width: 210 },
    { title: '代码', dataIndex: 'snoItem', width: 160 },
    { title: '内容', dataIndex: 'snoTitle' },
  ];

  const dailyCols: ColumnsType<ExamTaskXimuDaily> = [
    { title: '日期', dataIndex: 'ymd', width: 120 },
    { title: '题量', dataIndex: 'nums', width: 80 },
  ];

  return (
    <div style={{ padding: 16 }}>
      <Typography.Title level={4} style={{ marginBottom: 12 }}>
        <TableOutlined /> 细目表
      </Typography.Title>

      <Space style={{ marginBottom: 12 }}>
        {hasPerm('task:ximu:import') && (
          <Button icon={<CloudUploadOutlined />} onClick={() => setImportOpen(true)}>
            导入
          </Button>
        )}
        <Button icon={<ReloadOutlined />} onClick={() => qc.invalidateQueries({ queryKey: ['task-ximus'] })}>
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
          onChange: (p, ps) => { setPage(p); setPageSize(ps); },
        }}
      />

      {/* Import dialog */}
      <Modal
        title="导入细目表"
        open={importOpen}
        onCancel={() => { setImportOpen(false); setImportWorkTypeId(undefined); setImportLevel(undefined); }}
        footer={null}
        width={500}
      >
        <Form layout="vertical">
          <Form.Item label="工种" required>
            <Select
              showSearch
              filterOption={(input, option) =>
                (option?.label as string)?.includes(input) ?? false
              }
              options={publicQuery.data?.workTypeList?.flatMap((g: any) =>
                (g.children ?? []).map((c: any) => ({ label: g.title + ' - ' + c.title, value: c.id }))
              ) ?? []}
              value={importWorkTypeId}
              onChange={setImportWorkTypeId}
              placeholder="请选择工种"
            />
          </Form.Item>
          <Form.Item label="等级" required>
            <Select
              options={LEVEL_OPTS}
              value={importLevel}
              onChange={setImportLevel}
              placeholder="请选择等级"
            />
          </Form.Item>
          <Form.Item label="类型" required>
            <Select
              options={TYPE_OPTS}
              value={importTypes}
              onChange={setImportTypes}
            />
          </Form.Item>
          <Form.Item label="文件">
            <Upload
              accept=".xlsx,.xls"
              maxCount={1}
              showUploadList={false}
              customRequest={(options) => {
                const { file, onSuccess } = options;
                handleFileSelected({ file });
                onSuccess?.('ok');
              }}
            >
              <Button icon={<CloudUploadOutlined />} loading={importing}>
                选择文件上传
              </Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      {/* Detail dialog */}
      <Modal
        title="细目表列表"
        open={detailOpen}
        onCancel={() => { setDetailOpen(false); setDetailData([]); setDetailLoading(false); setExpandedKeys([]); setCurrentRow(null); }}
        width="85%"
        footer={null}
      >
        <Table
          rowKey="id"
          key={detailKey}
          columns={detailCols}
          dataSource={detailData}
          loading={detailLoading}
          expandable={{
            expandedRowKeys: expandedKeys,
            onExpandedRowsChange: (keys) => setExpandedKeys(keys as React.Key[]),
          }}
          rowClassName={(record) => {
            if (String(record.id) === '初审') return 'row-start';
            if (String(record.id) === '终审') return 'row-end';
            return '';
          }}
          scroll={{ x: 800, y: 500 }}
          pagination={false}
        />
      </Modal>

      {/* File preview dialog */}
      <Modal
        title="细目表文件预览"
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

      {/* Assign expert dialog */}
      <Modal
        title="指派专家"
        open={assignOpen}
        onCancel={() => setAssignOpen(false)}
        width={455}
        footer={[
          <Button key="cancel" onClick={() => setAssignOpen(false)}>取消</Button>,
          <Button key="confirm" type="primary" disabled={!selectedExpertId}
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
            {assignRole > 0 ? '终审' : '初审'}
          </Tag>
          <Select
            showSearch
            style={{ width: 200 }}
            value={selectedExpertId}
            onChange={(val) => setSelectedExpertId(val)}
            filterOption={(input, option) =>
              (option?.label as string)?.includes(input) ?? false
            }
            options={(publicQuery.data?.expertList ?? []).map((e: any) => ({
              label: e.nickname,
              value: e.id,
            }))}
            placeholder="选择专家"
          />
        </div>
      </Modal>

      {/* Daily audit dialog */}
      <Modal
        title="每日审核量"
        open={dailyOpen}
        onCancel={() => { setDailyOpen(false); setDailyData([]); }}
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
