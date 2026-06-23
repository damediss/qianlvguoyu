import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UploadOutlined,
  DownloadOutlined,
  InboxOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tabs,
  Tree,
  TreeSelect,
  Typography,
  Upload,
  message,
  Popconfirm,
  Spin,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo } from 'react';
import {
  pageCourses,
  treeWorkTypes,
  treeKnowledgeStructures,
  createKnowledgeStructure,
  updateKnowledgeStructure,
  deleteKnowledgeStructure,
  batchDeleteKnowledgeStructures,
  importKnowledgeStructureExcel,
  exportKnowledgeStructures,
} from '../../api/exam';
import type {
  ExamKnowledgeStructure,
  ExamKnowledgeStructureCreateReq,
  ExamCourse,
  ExamWorkType,
} from '../../types/exam';

const { Text } = Typography;
const { TextArea } = Input;

// 重要等级选项
const IMPORTANT_OPTIONS = [
  { label: 'X', value: 0 },
  { label: 'Y', value: 1 },
  { label: 'Z', value: 2 },
];

const IMPORTANT_MAP: Record<number, string> = { 0: 'X', 1: 'Y', 2: 'Z' };

// 构建工种树数据
function buildTreeSelectData(nodes: ExamWorkType[]): any[] {
  return nodes.map((node) => ({
    title: node.title,
    value: node.id,
    children: node.children ? buildTreeSelectData(node.children) : undefined,
  }));
}

// 构建知识点树数据 (用于 antd Tree)
interface TreeNode {
  key: string;
  title: string;
  children?: TreeNode[];
  isLeaf?: boolean;
}

function buildTreeData(nodes: ExamKnowledgeStructure[]): TreeNode[] {
  return nodes.map((n) => ({
    key: String(n.id),
    title: n.title,
    children: n.children && n.children.length > 0 ? buildTreeData(n.children) : undefined,
    isLeaf: !n.children || n.children.length === 0,
  }));
}

// 构建知识点树 (用于 TreeSelect 上级选择)
function buildParentTreeData(nodes: ExamKnowledgeStructure[]): any[] {
  return nodes.map((n) => ({
    title: n.title,
    value: n.id,
    children: n.children && n.children.length > 0 ? buildParentTreeData(n.children) : undefined,
  }));
}

// 扁平化树节点 (用于查找节点详情)
function flattenTree(nodes: ExamKnowledgeStructure[]): ExamKnowledgeStructure[] {
  const result: ExamKnowledgeStructure[] = [];
  const walk = (list: ExamKnowledgeStructure[]) => {
    for (const n of list) {
      result.push(n);
      if (n.children && n.children.length > 0) walk(n.children);
    }
  };
  walk(nodes);
  return result;
}

// ─── 添加/编辑节点弹窗 ───────────────────────────────────────
interface NodeModalProps {
  open: boolean;
  editing: ExamKnowledgeStructure | null;
  courseId: number | undefined;
  activeType: number;
  treeData: ExamKnowledgeStructure[];
  onClose: () => void;
}

function NodeFormModal({ open, editing, courseId, activeType, treeData, onClose }: NodeModalProps) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const parentTreeData = useMemo(() => buildParentTreeData(treeData), [treeData]);

  const createMut = useMutation({
    mutationFn: createKnowledgeStructure,
    onSuccess: () => {
      message.success('创建成功');
      queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
      onClose();
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ExamKnowledgeStructureCreateReq }) =>
      updateKnowledgeStructure(id, payload),
    onSuccess: () => {
      message.success('更新成功');
      queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
      onClose();
    },
    onError: (e: Error) => message.error(e.message || '更新失败'),
  });

  const handleOk = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const payload: ExamKnowledgeStructureCreateReq = {
        title: values.title,
        type: activeType,
        courseId,
        parentId: values.parentId || 0,
        seriesNo: values.seriesNo,
        important: values.important ?? 0,
        intro: values.intro,
        amount: values.amount ?? 0,
        sortNumber: values.sortNumber ?? 100,
      };
      if (editing) {
        updateMut.mutate({ id: editing.id, payload });
      } else {
        createMut.mutate(payload);
      }
    } catch {
      // validation error
    }
  }, [form, editing, createMut, updateMut, courseId, activeType]);

  // Reset form when modal opens
  useMemo(() => {
    if (open) {
      if (editing) {
        form.setFieldsValue({
          title: editing.title,
          type: editing.type ?? activeType,
          intro: editing.intro,
          parentId: editing.parentId || undefined,
          important: editing.important ?? 0,
          amount: editing.amount ?? 0,
          seriesNo: editing.seriesNo,
          sortNumber: editing.sortNumber ?? 100,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ sortNumber: 100, important: 0, amount: 0 });
      }
    }
  }, [open, editing, form, activeType]);

  return (
    <Modal
      title={editing ? '编辑节点' : '添加节点'}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={createMut.isPending || updateMut.isPending}
      width={560}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
          <Input placeholder="请输入节点标题" />
        </Form.Item>
        <Form.Item name="type" label="类型">
          <Radio.Group>
            <Radio value={0}>理论</Radio>
            <Radio value={1}>实操</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="intro" label="简介">
          <TextArea rows={3} placeholder="请输入简介" />
        </Form.Item>
        <Form.Item name="parentId" label="上级节点">
          <TreeSelect
            treeData={parentTreeData}
            showSearch
            treeDefaultExpandAll={false}
            placeholder="选择上级节点"
            allowClear
            treeNodeFilterProp="title"
          />
        </Form.Item>
        <Form.Item name="important" label="重要等级">
          <Select options={IMPORTANT_OPTIONS} placeholder="选择重要等级" />
        </Form.Item>
        <Form.Item name="amount" label="单价">
          <InputNumber min={0} max={99999} style={{ width: '100%' }} placeholder="0" />
        </Form.Item>
        <Form.Item name="seriesNo" label="编号">
          <Input placeholder="如 01, 0101" />
        </Form.Item>
        <Form.Item name="sortNumber" label="排序号">
          <InputNumber min={0} max={9999} style={{ width: '100%' }} placeholder="100" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

// ─── 导入弹窗 ───────────────────────────────────────
interface ImportModalProps {
  open: boolean;
  courseId: number | undefined;
  activeType: number;
  onClose: () => void;
}

function ImportModal({ open, courseId, activeType, onClose }: ImportModalProps) {
  const [fileList, setFileList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const queryClient = useQueryClient();

  const handleImport = async () => {
    if (!courseId) {
      message.warning('请先选择科目');
      return;
    }
    if (fileList.length === 0) {
      message.warning('请选择要导入的文件');
      return;
    }
    const formData = new FormData();
    formData.append('file', fileList[0]);
    formData.append('courseId', String(courseId));
    formData.append('type', String(activeType));

    setLoading(true);
    try {
      const count = await importKnowledgeStructureExcel(formData);
      message.success(`成功导入 ${count} 个知识点`);
      setFileList([]);
      queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
      onClose();
    } catch (e: any) {
      message.error(e?.message || '导入失败');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFileList([]);
    onClose();
  };

  return (
    <Modal
      title="导入知识结构"
      open={open}
      onOk={handleImport}
      onCancel={handleClose}
      confirmLoading={loading}
      okText="开始导入"
      destroyOnHidden
    >
      <Upload.Dragger
        accept=".xls,.xlsx"
        maxCount={1}
        fileList={fileList}
        beforeUpload={() => false}
        onChange={({ fileList: fl }) => setFileList(fl)}
      >
        <p style={{ fontSize: 40, color: '#1677ff', marginBottom: 8 }}>
          <InboxOutlined />
        </p>
        <p style={{ fontSize: 14 }}>点击或将文件拖拽到这里上传</p>
        <p style={{ color: '#999', fontSize: 12 }}>支持 .xls / .xlsx 格式</p>
      </Upload.Dragger>
      <div style={{ marginTop: 12 }}>
        <a
          onClick={() => {
            message.info('请联系管理员获取导入模板');
          }}
        >
          <DownloadOutlined /> 下载导入模板
        </a>
      </div>
    </Modal>
  );
}

// ─── 主页面 ───────────────────────────────────────
export function KnowledgeStructurePage() {
  const queryClient = useQueryClient();

  // ── 左侧面板状态 ──
  const [courseQuery, setCourseQuery] = useState({ page: 1, pageSize: 10, title: '' });
  const [workTypeId, setWorkTypeId] = useState<number | undefined>();
  const [selectedCourseId, setSelectedCourseId] = useState<number | undefined>();
  const [selectedCourseTitle, setSelectedCourseTitle] = useState<string>('');

  // ── 右侧面板状态 ──
  const [activeType, setActiveType] = useState<number>(0); // 0=理论, 1=实操
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [checkedKeys, setCheckedKeys] = useState<React.Key[]>([]);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);

  // ── 弹窗状态 ──
  const [nodeModalOpen, setNodeModalOpen] = useState(false);
  const [editingNode, setEditingNode] = useState<ExamKnowledgeStructure | null>(null);
  const [importModalOpen, setImportModalOpen] = useState(false);

  // ── 数据加载 ──
  const workTypeQuery = useQuery({
    queryKey: ['exam-work-types-tree'],
    queryFn: treeWorkTypes,
  });

  const courseListQuery = useQuery({
    queryKey: ['ks-courses', courseQuery],
    queryFn: () =>
      pageCourses({
        ...courseQuery,
        workTypeId: workTypeId || undefined,
      }),
  });

  const treeQuery = useQuery({
    queryKey: ['knowledge-tree', selectedCourseId, activeType],
    queryFn: () => treeKnowledgeStructures(selectedCourseId, activeType),
    enabled: !!selectedCourseId,
  });

  const treeData: TreeNode[] = useMemo(
    () => (treeQuery.data ? buildTreeData(treeQuery.data) : []),
    [treeQuery.data],
  );

  const flatNodes = useMemo(
    () => (treeQuery.data ? flattenTree(treeQuery.data) : []),
    [treeQuery.data],
  );

  const selectedNode = useMemo(
    () => flatNodes.find((n) => String(n.id) === String(selectedKeys[0])) || null,
    [flatNodes, selectedKeys],
  );

  // ── Mutations ──
  const deleteMut = useMutation({
    mutationFn: deleteKnowledgeStructure,
    onSuccess: () => {
      message.success('删除成功');
      setSelectedKeys([]);
      queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const batchDeleteMut = useMutation({
    mutationFn: batchDeleteKnowledgeStructures,
    onSuccess: (n) => {
      message.success(`已删除 ${n} 个知识点`);
      setCheckedKeys([]);
      setSelectedKeys([]);
      queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
    },
    onError: (e: Error) => message.error(e.message || '批量删除失败'),
  });

  // ── 导出处理 ──
  const handleExport = async () => {
    if (!selectedCourseId) {
      message.warning('请先选择科目');
      return;
    }
    try {
      const resp = await exportKnowledgeStructures(selectedCourseId, activeType);
      const blob = new Blob([resp.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const disposition = resp.headers['content-disposition'];
      let filename = '知识点导出.xlsx';
      if (disposition) {
        const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;\n]+)/i);
        if (match) filename = decodeURIComponent(match[1]);
      }
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch (e: any) {
      message.error(e?.message || '导出失败');
    }
  };

  // ── 批量删除 ──
  const handleBatchDelete = () => {
    const ids = checkedKeys.map(Number);
    if (ids.length === 0) return;
    Modal.confirm({
      title: `确认删除 ${ids.length} 个知识点?`,
      content: '删除后无法恢复，且会级联删除子节点',
      okText: '确认删除',
      okButtonProps: { danger: true },
      onOk: () => batchDeleteMut.mutate(ids),
    });
  };

  // ── 表格列定义 ──
  const columns: ColumnsType<ExamCourse> = [
    {
      title: '科目名称',
      dataIndex: 'title',
      ellipsis: true,
      render: (text: string) => <Text strong>{text}</Text>,
    },
    {
      title: '科目编码',
      dataIndex: 'seriesNo',
      width: 120,
    },
  ];

  const courses = courseListQuery.data?.items || [];
  const courseTotal = courseListQuery.data?.total || 0;

  return (
    <div style={{ display: 'flex', gap: 16, height: '100%' }}>
      {/* ── 左侧面板：科目列表 ── */}
      <Card
        style={{ width: 320, flexShrink: 0, overflow: 'auto' }}
        size="small"
        title={<Text strong>科目列表</Text>}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <TreeSelect
            style={{ width: '100%' }}
            placeholder="按工种筛选"
            allowClear
            showSearch
            treeDefaultExpandAll={false}
            treeNodeFilterProp="title"
            treeData={buildTreeSelectData(workTypeQuery.data || [])}
            value={workTypeId}
            onChange={(v) => {
              setWorkTypeId(v);
              setCourseQuery((q) => ({ ...q, page: 1 }));
            }}
          />
          <Input.Search
            placeholder="搜索科目名称/编码"
            allowClear
            onSearch={(v) => {
              setCourseQuery((q) => ({ ...q, title: v, page: 1 }));
            }}
          />
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => courseListQuery.refetch()}
          >
            刷新
          </Button>
        </Space>

        <Table<ExamCourse>
          rowKey="id"
          columns={columns}
          dataSource={courses}
          loading={courseListQuery.isLoading}
          size="small"
          pagination={{
            current: courseQuery.page,
            pageSize: courseQuery.pageSize,
            total: courseTotal,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, pageSize) => setCourseQuery((q) => ({ ...q, page, pageSize })),
          }}
          onRow={(record) => ({
            onClick: () => {
              setSelectedCourseId(record.id);
              setSelectedCourseTitle(record.title);
              setSelectedKeys([]);
              setCheckedKeys([]);
            },
            style: {
              background: record.id === selectedCourseId ? '#e6f4ff' : undefined,
              cursor: 'pointer',
            },
          })}
          style={{ marginTop: 12 }}
        />
      </Card>

      {/* ── 右侧面板：知识点树 ── */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {selectedCourseId ? (
          <>
            <Card size="small" title={<Text strong>{selectedCourseTitle} — 知识结构</Text>}>
              <Tabs
                activeKey={String(activeType)}
                onChange={(key) => {
                  setActiveType(Number(key));
                  setSelectedKeys([]);
                  setCheckedKeys([]);
                }}
                items={[
                  { key: '0', label: '理论' },
                  { key: '1', label: '实操' },
                ]}
                style={{ marginBottom: 12 }}
              />

              <Space wrap style={{ marginBottom: 12 }}>
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  onClick={() => setImportModalOpen(true)}
                >
                  批量导入
                </Button>
                <Button
                  type="primary"
                  icon={<DownloadOutlined />}
                  onClick={handleExport}
                >
                  导出
                </Button>
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => {
                    setEditingNode(null);
                    setNodeModalOpen(true);
                  }}
                >
                  添加
                </Button>
                <Button
                  icon={<EditOutlined />}
                  disabled={!selectedNode}
                  onClick={() => {
                    if (selectedNode) {
                      setEditingNode(selectedNode);
                      setNodeModalOpen(true);
                    }
                  }}
                >
                  编辑
                </Button>
                <Popconfirm
                  title="确认删除此节点？"
                  description="会级联删除所有子节点"
                  onConfirm={() => {
                    if (selectedNode) deleteMut.mutate(selectedNode.id);
                  }}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button icon={<DeleteOutlined />} danger disabled={!selectedNode}>
                    删除
                  </Button>
                </Popconfirm>
                <Button
                  icon={<DeleteOutlined />}
                  danger
                  disabled={checkedKeys.length === 0}
                  onClick={handleBatchDelete}
                >
                  批量删除{checkedKeys.length > 0 ? `(${checkedKeys.length})` : ''}
                </Button>
              </Space>

              <Spin spinning={treeQuery.isLoading}>
                <Tree
                  treeData={treeData}
                  expandedKeys={expandedKeys}
                  onExpand={(keys) => setExpandedKeys(keys)}
                  selectedKeys={selectedKeys}
                  onSelect={(keys) => setSelectedKeys(keys)}
                  checkedKeys={checkedKeys}
                  onCheck={(keys) => setCheckedKeys(keys as React.Key[])}
                  checkable
                  showLine
                  style={{ minHeight: 200 }}
                />
              </Spin>
            </Card>
          </>
        ) : (
          <Card style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Text type="secondary">请在左侧选择一个科目</Text>
          </Card>
        )}
      </div>

      {/* ── 节点表单弹窗 ── */}
      <NodeFormModal
        open={nodeModalOpen}
        editing={editingNode}
        courseId={selectedCourseId}
        activeType={activeType}
        treeData={treeQuery.data || []}
        onClose={() => {
          setNodeModalOpen(false);
          setEditingNode(null);
        }}
      />

      {/* ── 导入弹窗 ── */}
      <ImportModal
        open={importModalOpen}
        courseId={selectedCourseId}
        activeType={activeType}
        onClose={() => setImportModalOpen(false)}
      />
    </div>
  );
}
