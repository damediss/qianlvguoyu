import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tree,
  Typography,
  message,
  Popconfirm,
  Breadcrumb,
  Spin,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo, useEffect } from 'react';
import {
  treeWorkTypes,
  pageCourses,
  treeKnowledgeStructures,
  pageExamDetails,
  createExamDetail,
  updateExamDetail,
  deleteExamDetail,
} from '../../api/exam';
import { useAuthStore } from '../../stores/authStore';
import type {
  ExamWorkType,
  ExamCourse,
  ExamKnowledgeStructure,
  ExamExamDetail,
  ExamExamDetailCreateReq,
  ExamExamDetailQuery,
} from '../../types/exam';
import {
  EXAM_TYPE_MAP,
  DIFFICULTY_MAP,
  DIFFICULTY_COLOR_MAP,
  CHECK_STATUS_MAP,
} from '../../types/exam';

const { Title, Text } = Typography;
const { TextArea } = Input;

// ─── helpers ───────────────────────────────────────────────
interface TreeNode {
  key: string;
  title: string;
  isLeaf?: boolean;
  children?: TreeNode[];
  // metadata
  nodeType: 'workType' | 'course' | 'knowledge';
  workTypeId?: number;
  courseId?: number;
  knowledgeId?: number;
  level?: number;
}

function buildKnowledgeTree(
  nodes: ExamKnowledgeStructure[],
  courseId: number,
  workTypeId: number,
): TreeNode[] {
  return nodes.map((n) => ({
    key: `ks-${n.id}`,
    title: n.title,
    nodeType: 'knowledge' as const,
    workTypeId,
    courseId,
    knowledgeId: n.id,
    level: n.level,
    isLeaf: !n.children || n.children.length === 0,
    children: n.children ? buildKnowledgeTree(n.children, courseId, workTypeId) : undefined,
  }));
}

function flattenBreadcrumb(_node: TreeNode | undefined, _allNodes: TreeNode[]): string[] {
  // placeholder for future breadcrumb implementation
  return [];
}

// ─── QuestionFormModal ─────────────────────────────────────
interface QuestionFormModalProps {
  open: boolean;
  editing: ExamExamDetail | null;
  knowledgeTreeData: ExamKnowledgeStructure[];
  courseId: number | undefined;
  onClose: () => void;
}

function QuestionFormModal({ open, editing, knowledgeTreeData, courseId, onClose }: QuestionFormModalProps) {
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const treeSelectData = useMemo(() => {
    function convert(nodes: ExamKnowledgeStructure[]): any[] {
      return nodes.map((n) => ({
        title: n.title,
        value: n.id,
        children: n.children ? convert(n.children) : undefined,
      }));
    }
    return convert(knowledgeTreeData);
  }, [knowledgeTreeData]);

  const createMut = useMutation({
    mutationFn: createExamDetail,
    onSuccess: () => {
      message.success('创建成功');
      queryClient.invalidateQueries({ queryKey: ['examDetails'] });
      onClose();
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ExamExamDetailCreateReq }) =>
      updateExamDetail(id, payload),
    onSuccess: () => {
      message.success('更新成功');
      queryClient.invalidateQueries({ queryKey: ['examDetails'] });
      onClose();
    },
    onError: (e: Error) => message.error(e.message || '更新失败'),
  });

  const handleOk = useCallback(async () => {
    try {
      const values = await form.validateFields();
      if (editing) {
        updateMut.mutate({ id: editing.id, payload: values });
      } else {
        createMut.mutate(values);
      }
    } catch {
      // validation error
    }
  }, [form, editing, createMut, updateMut]);

  // Reset form when modal opens
  useEffect(() => {
    if (open) {
      if (editing) {
        form.setFieldsValue({
          type: editing.type,
          content: editing.content,
          answer: editing.answer,
          knowledgeId: editing.knowledgeId,
          difficulty: editing.difficulty,
          score: editing.score,
          resolve: editing.resolve,
          keyword: editing.keyword,
        });
      } else {
        form.resetFields();
      }
    }
  }, [open, editing, form]);

  return (
    <Modal
      title={editing ? '编辑试题' : '新建试题'}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={createMut.isPending || updateMut.isPending}
      width={720}
      destroyOnClose
    >
      <Form form={form} layout="vertical" initialValues={{ type: 'single', difficulty: 2 }}>
        <Form.Item name="type" label="题型" rules={[{ required: true, message: '请选择题型' }]}>
          <Select
            options={Object.entries(EXAM_TYPE_MAP).map(([k, v]) => ({ label: v, value: k }))}
            placeholder="选择题型"
          />
        </Form.Item>
        <Form.Item name="content" label="题干">
          <TextArea rows={4} placeholder="请输入题干内容" />
        </Form.Item>
        <Form.Item name="answer" label="答案">
          <TextArea rows={3} placeholder="请输入答案（JSON 或文本）" />
        </Form.Item>
        <Form.Item name="knowledgeId" label="知识点">
          <Select
            treeData={treeSelectData}
            showSearch
            treeDefaultExpandAll={false}
            placeholder="选择知识点"
            allowClear
            treeNodeFilterProp="title"
          />
        </Form.Item>
        <Form.Item name="difficulty" label="难度">
          <Select
            options={Object.entries(DIFFICULTY_MAP).map(([k, v]) => ({
              label: v,
              value: Number(k),
            }))}
            placeholder="选择难度"
          />
        </Form.Item>
        <Form.Item name="score" label="分值">
          <InputNumber min={0} max={9999} style={{ width: '100%' }} placeholder="分值" />
        </Form.Item>
        <Form.Item name="resolve" label="解析">
          <TextArea rows={3} placeholder="请输入解析" />
        </Form.Item>
        <Form.Item name="keyword" label="关键词">
          <Input placeholder="关键词，多个用逗号分隔" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

// ─── Main Page ─────────────────────────────────────────────
export function QuestionBankPage() {
  const queryClient = useQueryClient();
  const hasPerm = useAuthStore((s) => s.hasPermission);

  // ── left tree state ──
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [treeSearchValue, setTreeSearchValue] = useState('');

  // selected node metadata
  const [, setSelectedWorkTypeId] = useState<number | undefined>();
  const [selectedCourseId, setSelectedCourseId] = useState<number | undefined>();
  const [selectedKnowledgeId, setSelectedKnowledgeId] = useState<number | undefined>();
  const [selectedTitle, setSelectedTitle] = useState('');

  // ── right table state ──
  const [query, setQuery] = useState<ExamExamDetailQuery>({
    page: 1,
    pageSize: 20,
  });
  const [filterType, setFilterType] = useState<string | undefined>();
  const [filterDifficulty, setFilterDifficulty] = useState<number | undefined>();
  const [filterKeyword, setFilterKeyword] = useState('');

  // ── modal state ──
  const [modalOpen, setModalOpen] = useState(false);
  const [editingDetail, setEditingDetail] = useState<ExamExamDetail | null>(null);

  // ── Data loading ──
  const { data: workTypes = [] } = useQuery({
    queryKey: ['workTypeTree'],
    queryFn: treeWorkTypes,
  });

  // courses keyed by workTypeId
  const [coursesMap, setCoursesMap] = useState<Record<number, ExamCourse[]>>({});
  // knowledge trees keyed by courseId
  const [knowledgeMap, setKnowledgeMap] = useState<Record<number, ExamKnowledgeStructure[]>>({});

  // Load courses for a work type
  const loadCoursesForWorkType = useCallback(
    async (workTypeId: number) => {
      if (coursesMap[workTypeId]) return;
      try {
        const res = await pageCourses({ page: 1, pageSize: 200, title: '' });
        // filter courses by workTypeId
        const filtered = res.items.filter((c) => c.workTypeId === workTypeId);
        setCoursesMap((prev) => ({ ...prev, [workTypeId]: filtered }));
      } catch (e) {
        console.error('loadCourses error', e);
      }
    },
    [coursesMap],
  );

  // Load knowledge tree for a course
  const loadKnowledgeForCourse = useCallback(
    async (courseId: number) => {
      if (knowledgeMap[courseId]) return;
      try {
        const tree = await treeKnowledgeStructures(courseId);
        setKnowledgeMap((prev) => ({ ...prev, [courseId]: tree }));
      } catch (e) {
        console.error('loadKnowledge error', e);
      }
    },
    [knowledgeMap],
  );

  // Build tree data
  const treeData: TreeNode[] = useMemo(() => {
    return workTypes.map((wt) => {
      const courses = coursesMap[wt.id] || [];
      const courseNodes: TreeNode[] = courses.map((c) => {
        const knowledge = knowledgeMap[c.id] || [];
        const knowledgeNodes = buildKnowledgeTree(knowledge, c.id, wt.id);
        return {
          key: `course-${c.id}`,
          title: `${c.title}${c.level ? ` (${c.level}级)` : ''}`,
          nodeType: 'course' as const,
          workTypeId: wt.id,
          courseId: c.id,
          isLeaf: knowledgeNodes.length === 0,
          children: knowledgeNodes.length > 0 ? knowledgeNodes : undefined,
        };
      });
      return {
        key: `wt-${wt.id}`,
        title: wt.title,
        nodeType: 'workType' as const,
        workTypeId: wt.id,
        isLeaf: courseNodes.length === 0,
        children: courseNodes.length > 0 ? courseNodes : undefined,
      };
    });
  }, [workTypes, coursesMap, knowledgeMap]);

  // Handle tree expand — lazy load children
  const handleExpand = useCallback(
    async (keys: React.Key[], info: any) => {
      setExpandedKeys(keys);
      const node = info.node as TreeNode;
      if (info.expanded && node.nodeType === 'workType' && node.workTypeId) {
        await loadCoursesForWorkType(node.workTypeId);
      }
      if (info.expanded && node.nodeType === 'course' && node.courseId) {
        await loadKnowledgeForCourse(node.courseId);
      }
    },
    [loadCoursesForWorkType, loadKnowledgeForCourse],
  );

  // Handle tree select — update right side filter
  const handleSelect = useCallback(
    (keys: React.Key[], info: any) => {
      setSelectedKeys(keys);
      if (keys.length === 0) {
        setSelectedWorkTypeId(undefined);
        setSelectedCourseId(undefined);
        setSelectedKnowledgeId(undefined);
        setSelectedTitle('');
        setQuery((q) => ({ ...q, page: 1 }));
        return;
      }
      const node = info.node as TreeNode;
      setSelectedTitle(node.title);
      setSelectedWorkTypeId(node.workTypeId);
      setSelectedCourseId(node.courseId);
      setSelectedKnowledgeId(node.knowledgeId);
      setQuery((q) => ({ ...q, page: 1 }));
    },
    [],
  );

  // ── Exam details query ──
  const effectiveQuery: ExamExamDetailQuery = useMemo(
    () => ({
      ...query,
      courseId: selectedCourseId,
      knowledgeId: selectedKnowledgeId,
      type: filterType,
      difficulty: filterDifficulty,
      keyword: filterKeyword || undefined,
    }),
    [query, selectedCourseId, selectedKnowledgeId, filterType, filterDifficulty, filterKeyword],
  );

  const { data: examPage, isLoading: loadingDetails } = useQuery({
    queryKey: ['examDetails', effectiveQuery],
    queryFn: () => pageExamDetails(effectiveQuery),
  });

  // ── Delete mutation ──
  const deleteMut = useMutation({
    mutationFn: deleteExamDetail,
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['examDetails'] });
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  // ── Table columns ──
  const columns: ColumnsType<ExamExamDetail> = [
    {
      title: '序号',
      width: 60,
      render: (_, __, index) => ((query.page ?? 1) - 1) * (query.pageSize ?? 20) + index + 1,
    },
    {
      title: '来源',
      dataIndex: 'source',
      width: 80,
      render: (v: number) => <Tag>{v === 1 ? '导入' : '手工'}</Tag>,
    },
    {
      title: '题型',
      dataIndex: 'type',
      width: 100,
      render: (v: string) => EXAM_TYPE_MAP[v] || v,
    },
    {
      title: '题干与选项',
      dataIndex: 'content',
      ellipsis: true,
      render: (v: string) => (
        <div style={{ maxHeight: 80, overflow: 'hidden' }}>
          {v ? v.substring(0, 120) : '-'}
        </div>
      ),
    },
    {
      title: '知识点',
      dataIndex: 'knowledgeTitle',
      width: 120,
      ellipsis: true,
      render: (v: string) => v || '-',
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 80,
      render: (v: number) => (
        <Tag color={DIFFICULTY_COLOR_MAP[v] || 'default'}>
          {DIFFICULTY_MAP[v] || v}
        </Tag>
      ),
    },
    {
      title: '初审',
      dataIndex: 'checkStatus',
      width: 70,
      render: (v: number) => {
        const colors: Record<number, string> = { 0: 'default', 1: 'success', 2: 'error' };
        return <Tag color={colors[v]}>{CHECK_STATUS_MAP[v] || v}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 160,
      render: (v: string) => (v ? v.substring(0, 16) : '-'),
    },
    {
      title: '操作',
      width: 150,
      render: (_, record) => (
        <Space size="small">
          {hasPerm('exam:question:update') && (
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditingDetail(record);
                setModalOpen(true);
              }}
            >
              编辑
            </Button>
          )}
          {hasPerm('exam:question:delete') && (
            <Popconfirm
              title="确认删除此试题？"
              onConfirm={() => deleteMut.mutate(record.id)}
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  // ── Filter tree by search ──
  const filteredTreeData = useMemo(() => {
    if (!treeSearchValue) return treeData;
    // simple: just show all (antd Tree handles filter natively via expandedKeys)
    return treeData;
  }, [treeData, treeSearchValue]);

  // Get current knowledge tree for modal
  const currentKnowledgeTree = useMemo(() => {
    if (selectedCourseId && knowledgeMap[selectedCourseId]) {
      return knowledgeMap[selectedCourseId];
    }
    return [];
  }, [selectedCourseId, knowledgeMap]);

  return (
    <div style={{ display: 'flex', gap: 16, height: '100%' }}>
      {/* ── Left panel: Tree ── */}
      <Card
        style={{ width: 300, flexShrink: 0, overflow: 'auto' }}
        size="small"
        title={
          <Space direction="vertical" style={{ width: '100%' }}>
            <Text strong>分类导航</Text>
            <Input
              placeholder="搜索"
              prefix={<SearchOutlined />}
              value={treeSearchValue}
              onChange={(e) => setTreeSearchValue(e.target.value)}
              allowClear
            />
          </Space>
        }
      >
        <Spin spinning={false}>
          <Tree
            treeData={filteredTreeData}
            expandedKeys={expandedKeys}
            onExpand={handleExpand}
            selectedKeys={selectedKeys}
            onSelect={handleSelect}
            blockNode
            showLine
          />
        </Spin>
      </Card>

      {/* ── Right panel: Table ── */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {/* Breadcrumb */}
        {selectedTitle && (
          <Breadcrumb
            items={[
              { title: '试题管理' },
              { title: selectedTitle },
            ]}
          />
        )}

        {/* Filter bar */}
        <Space wrap>
          <Select
            style={{ width: 120 }}
            placeholder="题型"
            allowClear
            value={filterType}
            onChange={(v) => {
              setFilterType(v);
              setQuery((q) => ({ ...q, page: 1 }));
            }}
            options={Object.entries(EXAM_TYPE_MAP).map(([k, v]) => ({ label: v, value: k }))}
          />
          <Select
            style={{ width: 100 }}
            placeholder="难度"
            allowClear
            value={filterDifficulty}
            onChange={(v) => {
              setFilterDifficulty(v);
              setQuery((q) => ({ ...q, page: 1 }));
            }}
            options={Object.entries(DIFFICULTY_MAP).map(([k, v]) => ({
              label: v,
              value: Number(k),
            }))}
          />
          <Input.Search
            style={{ width: 200 }}
            placeholder="搜索题干"
            allowClear
            onSearch={(v) => {
              setFilterKeyword(v);
              setQuery((q) => ({ ...q, page: 1 }));
            }}
          />
          <Button
            icon={<ReloadOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['examDetails'] })}
          >
            刷新
          </Button>
          {hasPerm('exam:question:create') && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                setEditingDetail(null);
                setModalOpen(true);
              }}
            >
              新建试题
            </Button>
          )}
        </Space>

        {/* Table */}
        <Table<ExamExamDetail>
          rowKey="id"
          columns={columns}
          dataSource={examPage?.items ?? []}
          loading={loadingDetails}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: examPage?.total ?? 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setQuery((q) => ({ ...q, page, pageSize })),
          }}
          size="small"
          scroll={{ x: 1000 }}
        />
      </div>

      {/* ── Modal ── */}
      <QuestionFormModal
        open={modalOpen}
        editing={editingDetail}
        knowledgeTreeData={currentKnowledgeTree}
        courseId={selectedCourseId}
        onClose={() => {
          setModalOpen(false);
          setEditingDetail(null);
        }}
      />
    </div>
  );
}
