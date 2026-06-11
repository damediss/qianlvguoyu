import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
  DownloadOutlined,
  InboxOutlined,
} from '@ant-design/icons';
import {
  Badge,
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
  Spin,
  Upload,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo, useEffect } from 'react';
import {
  getQuestionBankTree,
  pageExamDetails,
  createExamDetail,
  updateExamDetail,
  deleteExamDetail,
  batchDeleteExamDetails,
  importExamDetails,
  exportExamDetails,
} from '../../api/exam';
import { useAuthStore } from '../../stores/authStore';
import type {
  ExamKnowledgeStructure,
  QuestionBankTreeNodeVO,
  QuestionBankTreeNodeType,
  ExamExamDetail,
  ExamExamDetailCreateReq,
  ExamExamDetailQuery,
} from '../../types/exam';
import {
  EXAM_TYPE_MAP,
  EXAM_SOURCE_MAP,
  DIFFICULTY_MAP,
  DIFFICULTY_COLOR_MAP,
  CHECK_STATUS_MAP,
} from '../../types/exam';
import { PathContextBar } from '../../components/ContextBar/PathContextBar';
import { BatchActionBar } from '../../components/BatchActionBar/BatchActionBar';

const { Text } = Typography;
const { TextArea } = Input;

// ─── helpers ───────────────────────────────────────────────
interface TreeNode {
  key: string;
  title: string;
  isLeaf?: boolean;
  children?: TreeNode[];
  // metadata
  nodeType: QuestionBankTreeNodeType;
  workTypeId?: number;
  courseId?: number;
  knowledgeId?: number;
  level?: number;
  /** 后端预计算的子树递归题数(0=灰, 1~9=蓝, 10+=橙) */
  questionCount: number;
}

/**
 * 把后端聚合树(`QuestionBankTreeNodeVO[]`)转成 antd `Tree` 期望的形状。
 * 树深 6 级:工种 → 课程 → 知识点 L0 → L1 → L2 → L3,每节点都带
 * `questionCount`,渲染时挂 Badge。
 */
function buildTree(nodes: QuestionBankTreeNodeVO[]): TreeNode[] {
  return nodes.map((n) => {
    const childNodes: TreeNode[] = (n.children ?? []).map((c) => {
      const subChildren = buildTree([c]);
      return subChildren[0];
    });
    return {
      key: n.key,
      title: n.title,
      nodeType: n.nodeType,
      workTypeId: n.nodeType === 'workType' ? n.id : n.workTypeId,
      courseId: n.nodeType === 'course' ? n.id : n.courseId,
      knowledgeId: n.nodeType === 'knowledge' ? n.id : undefined,
      level: n.level,
      questionCount: n.questionCount,
      isLeaf: childNodes.length === 0,
      children: childNodes.length > 0 ? childNodes : undefined,
    };
  });
}

/**
 * 在聚合树中按 key 找到节点,同时回溯整条祖先链
 * (workType → course → [knowledge ...])。给 PathContextBar 用。
 */
function findNodeWithPath(
  nodes: QuestionBankTreeNodeVO[],
  key: string,
  path: QuestionBankTreeNodeVO[] = [],
): { node: QuestionBankTreeNodeVO; path: QuestionBankTreeNodeVO[] } | null {
  for (const n of nodes) {
    if (n.key === key) return { node: n, path };
    const sub = findNodeWithPath(n.children ?? [], key, [...path, n]);
    if (sub) return sub;
  }
  return null;
}

/**
 * Badge 颜色: 0 题灰色,1~9 蓝,10+ 橙。
 * (参考 `题库树接口契约.md` chip 风格一节)
 */
function countColor(n: number): string {
  if (n <= 0) return '#bfbfbf';
  if (n < 10) return '#1677ff';
  return '#fa8c16';
}

/**
 * 把新聚合树里的 `knowledge` 子树转成 modal 用的
 * `ExamKnowledgeStructure[]` 形态(id/title/level/children)。
 * 缺省字段用安全占位,modal 只读 `id/title/level/children`。
 */
function toKnowledgeStructures(nodes: QuestionBankTreeNodeVO[]): ExamKnowledgeStructure[] {
  return nodes
    .filter((n) => n.nodeType === 'knowledge')
    .map((n) => ({
      id: n.id,
      title: n.title,
      level: n.level ?? 0,
      type: 0,
      useType: null,
      courseId: n.courseId ?? null,
      parentId: 0,
      cover: '',
      intro: '',
      seriesNo: '',
      important: 0,
      userId: null,
      status: n.status ?? 1,
      sortNumber: n.sortNumber ?? 0,
      tenantId: 0,
      amount: 0,
      createTime: '',
      children: toKnowledgeStructures(n.children ?? []),
    }));
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

  // ── right table state ──
  const [query, setQuery] = useState<ExamExamDetailQuery>({
    page: 1,
    pageSize: 20,
  });
  const [filterType, setFilterType] = useState<string | undefined>();
  const [filterDifficulty, setFilterDifficulty] = useState<number | undefined>();
  const [filterSource, setFilterSource] = useState<number | undefined>();
  const [filterKeyword, setFilterKeyword] = useState('');

  // ── selection state ──
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // ── modal state ──
  const [modalOpen, setModalOpen] = useState(false);
  const [editingDetail, setEditingDetail] = useState<ExamExamDetail | null>(null);

  // ── import modal state ──
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importSource, setImportSource] = useState<number>(0); // 0=内部, 1=外部
  const [importFileList, setImportFileList] = useState<any[]>([]);
  const [importLoading, setImportLoading] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);

  // ── Data loading: 单次拉取完整 6 级聚合树 ──
  const {
    data: bankTree = [],
    isLoading: loadingBankTree,
  } = useQuery({
    queryKey: ['exam', 'question-bank-tree'],
    queryFn: getQuestionBankTree,
    staleTime: 60_000,
  });

  // antd Tree 用结构
  const treeData: TreeNode[] = useMemo(() => buildTree(bankTree), [bankTree]);

  // 默认展开工种 + 课程两级(取后端返回的第一层工种 + 第二层课程的 key)
  const defaultExpandedKeys = useMemo<React.Key[]>(() => {
    const keys: React.Key[] = [];
    for (const wt of bankTree) {
      keys.push(wt.key);
      for (const c of wt.children ?? []) keys.push(c.key);
    }
    return keys;
  }, [bankTree]);

  // 首次拿到数据后,把 expandedKeys 初始化成"工种+课程两级"
  useEffect(() => {
    if (bankTree.length > 0 && expandedKeys.length === 0) {
      setExpandedKeys(defaultExpandedKeys);
    }
  }, [bankTree, defaultExpandedKeys, expandedKeys.length]);

  // Handle tree expand — 直接更新 expandedKeys(数据全在内存,无需 lazy load)
  const handleExpand = useCallback((keys: React.Key[]) => {
    setExpandedKeys(keys);
  }, []);

  // Handle tree select — update right side filter
  const handleSelect = useCallback(
    (keys: React.Key[], info: any) => {
      setSelectedKeys(keys);
      if (keys.length === 0) {
        setSelectedWorkTypeId(undefined);
        setSelectedCourseId(undefined);
        setSelectedKnowledgeId(undefined);
        setQuery((q) => ({ ...q, page: 1 }));
        return;
      }
      const node = info.node as TreeNode;
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
      source: filterSource,
      keyword: filterKeyword || undefined,
    }),
    [
      query,
      selectedCourseId,
      selectedKnowledgeId,
      filterType,
      filterDifficulty,
      filterSource,
      filterKeyword,
    ],
  );

  const { data: examPage, isLoading: loadingDetails } = useQuery({
    queryKey: ['examDetails', effectiveQuery],
    queryFn: () => pageExamDetails(effectiveQuery),
  });

  // ── Breadcrumb titles (从新聚合树里按 selectedKey 找 title + 祖先链) ──
  const breadcrumbTitles = useMemo(() => {
    const titles: { workType?: string; course?: string; knowledge?: string } = {};
    if (selectedKeys.length === 0) return titles;

    const keyStr = String(selectedKeys[0]);
    const found = findNodeWithPath(bankTree, keyStr);
    if (!found) return titles;

    // 从祖先链 + 当前节点定位 workType / course / knowledge 三级
    const chain = [...found.path, found.node];
    for (const n of chain) {
      if (n.nodeType === 'workType') titles.workType = n.title;
      else if (n.nodeType === 'course') titles.course = n.title;
      else if (n.nodeType === 'knowledge') titles.knowledge = n.title;
    }
    return titles;
  }, [selectedKeys, bankTree]);

  const workTypeTitle = breadcrumbTitles.workType;
  const courseTitle = breadcrumbTitles.course;
  const knowledgeTitle = breadcrumbTitles.knowledge;

  // ── Source counts (从当前页数据粗略统计) ──
  const sourceCount = useMemo(() => {
    const items = examPage?.items ?? [];
    return {
      internal: items.filter((it) => it.source === 0).length,
      external: items.filter((it) => it.source === 1).length,
    };
  }, [examPage?.items]);

  // ── Delete mutation ──
  const deleteMut = useMutation({
    mutationFn: deleteExamDetail,
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['examDetails'] });
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  // ── Batch delete mutation ──
  const batchDeleteMut = useMutation({
    mutationFn: batchDeleteExamDetails,
    onSuccess: (n) => {
      message.success(`已删除 ${n} 道试题`);
      setSelectedRowKeys([]);
      queryClient.invalidateQueries({ queryKey: ['examDetails'] });
    },
    onError: (e: Error) => message.error(e.message || '批量删除失败'),
  });

  const handleBatchDelete = () => {
    const ids = selectedRowKeys.map(Number);
    if (ids.length === 0) return;
    Modal.confirm({
      title: `确认删除 ${ids.length} 道试题?`,
      content: '删除后无法恢复,且会级联影响已组卷的试卷',
      okText: '确认删除',
      okButtonProps: { danger: true },
      onOk: () => batchDeleteMut.mutate(ids),
    });
  };

  // ── Import handler ──
  const handleImport = async () => {
    if (!selectedCourseId) {
      message.warning('请先选择科目');
      return;
    }
    if (importFileList.length === 0) {
      message.warning('请选择要导入的文件');
      return;
    }
    const formData = new FormData();
    formData.append('file', importFileList[0]);
    formData.append('courseId', String(selectedCourseId));
    formData.append('source', String(importSource));

    setImportLoading(true);
    try {
      const count = await importExamDetails(formData);
      message.success(`成功导入 ${count} 道试题`);
      setImportModalOpen(false);
      setImportFileList([]);
      queryClient.invalidateQueries({ queryKey: ['examDetails'] });
      queryClient.invalidateQueries({ queryKey: ['exam', 'question-bank-tree'] });
    } catch (e: any) {
      message.error(e?.message || '导入失败');
    } finally {
      setImportLoading(false);
    }
  };

  // ── Export handler ──
  const handleExport = async () => {
    if (!selectedCourseId) {
      message.warning('请先选择科目');
      return;
    }
    setExportLoading(true);
    try {
      const resp = await exportExamDetails(selectedCourseId);
      const blob = new Blob([resp.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      // Extract filename from Content-Disposition header
      const disposition = resp.headers['content-disposition'];
      let filename = '题库导出.xlsx';
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
    } finally {
      setExportLoading(false);
    }
  };

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
      render: (v: number) => (
        <Tag color={v === 0 ? 'blue' : 'orange'}>{EXAM_SOURCE_MAP[v] ?? v}</Tag>
      ),
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
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 160,
      render: (v: string, record: ExamExamDetail) => {
        const t = v || record.createTime;
        return t ? String(t).substring(0, 16) : '-';
      },
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
  // 行为:输入框非空时,递归遍历树,保留"标题命中关键词"的节点 + 它的所有祖先链;
  //      同时把命中的节点 key 自动加入 expandedKeys,确保能看到。
  //      输入清空时恢复原树。
  const filterTreeByKeyword = useCallback(
    (nodes: TreeNode[], keyword: string): { matched: TreeNode[]; matchedKeys: string[] } => {
      const k = keyword.trim().toLowerCase();
      const matchedKeys: string[] = [];
      const walk = (n: TreeNode, ancestors: TreeNode[]): TreeNode | null => {
        const selfHit = n.title.toLowerCase().includes(k);
        const keptChildren: TreeNode[] = [];
        for (const c of n.children ?? []) {
          const r = walk(c, [...ancestors, n]);
          if (r) keptChildren.push(r);
        }
        if (selfHit) {
          matchedKeys.push(n.key);
          // 命中:自己保留,children 也全保留(让用户看到完整子树)
          return { ...n, children: n.children };
        }
        if (keptChildren.length > 0) {
          // 不是自身命中,但子树里有命中 → 作为"祖先"保留,只展示命中分支
          return { ...n, children: keptChildren };
        }
        return null;
      };
      const out: TreeNode[] = [];
      for (const n of nodes) {
        const r = walk(n, []);
        if (r) out.push(r);
      }
      return { matched: out, matchedKeys };
    },
    [],
  );

  const filteredTreeData = useMemo(() => {
    if (!treeSearchValue.trim()) return treeData;
    return filterTreeByKeyword(treeData, treeSearchValue).matched;
  }, [treeData, treeSearchValue, filterTreeByKeyword]);

  // 搜索时:把命中的 key 自动加入 expandedKeys,这样用户能直接看到命中节点
  useEffect(() => {
    const k = treeSearchValue.trim();
    if (!k) return;
    const { matchedKeys } = filterTreeByKeyword(treeData, k);
    if (matchedKeys.length === 0) return;
    setExpandedKeys((prev) => {
      const set = new Set(prev.map(String));
      for (const mk of matchedKeys) set.add(mk);
      return Array.from(set);
    });
  }, [treeSearchValue, treeData, filterTreeByKeyword]);

  // Get current knowledge tree for modal — 从新聚合树里找选中课程的子节点
  const currentKnowledgeTree = useMemo<ExamKnowledgeStructure[]>(() => {
    if (selectedCourseId === undefined) return [];
    // 找该 course 节点,转成 ExamKnowledgeStructure 形态给 modal
    let courseNode: QuestionBankTreeNodeVO | undefined;
    for (const wt of bankTree) {
      for (const c of wt.children ?? []) {
        if (c.id === selectedCourseId) {
          courseNode = c;
          break;
        }
      }
      if (courseNode) break;
    }
    if (!courseNode) return [];
    return toKnowledgeStructures(courseNode.children ?? []);
  }, [selectedCourseId, bankTree]);

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
        <Spin spinning={loadingBankTree}>
          <Tree
            treeData={filteredTreeData}
            expandedKeys={expandedKeys}
            onExpand={handleExpand}
            selectedKeys={selectedKeys}
            onSelect={handleSelect}
            blockNode
            showLine
            titleRender={(node: any) => {
              const tn = node as TreeNode;
              const isBold = tn.nodeType === 'workType' || tn.nodeType === 'course';
              // 高亮搜索命中的子串(忽略大小写),用 <mark> 默认样式
              const k = treeSearchValue.trim();
              const titleNode = (() => {
                if (!k) {
                  return isBold ? <Text strong>{tn.title}</Text> : <Text>{tn.title}</Text>;
                }
                const lower = tn.title.toLowerCase();
                const idx = lower.indexOf(k.toLowerCase());
                if (idx < 0) {
                  return isBold ? <Text strong>{tn.title}</Text> : <Text>{tn.title}</Text>;
                }
                const before = tn.title.slice(0, idx);
                const hit = tn.title.slice(idx, idx + k.length);
                const after = tn.title.slice(idx + k.length);
                const content = (
                  <>
                    {before}
                    <mark style={{ background: '#fff566', padding: '0 2px' }}>{hit}</mark>
                    {after}
                  </>
                );
                return isBold ? <Text strong>{content}</Text> : <Text>{content}</Text>;
              })();
              return (
                <span
                  style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}
                >
                  {titleNode}
                  <Badge
                    count={tn.questionCount}
                    showZero
                    style={{
                      backgroundColor: countColor(tn.questionCount),
                      color: '#fff',
                      boxShadow: 'none',
                    }}
                    title={`${tn.questionCount} 道题`}
                  />
                </span>
              );
            }}
          />
        </Spin>
      </Card>

      {/* ── Right panel: Table ── */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {/* Batch action bar (visible when there are selected rows) */}
        {selectedRowKeys.length > 0 && (
          <BatchActionBar
            count={selectedRowKeys.length}
            onClear={() => setSelectedRowKeys([])}
            actions={[
              {
                key: 'batch-delete',
                label: '批量删除',
                danger: true,
                onClick: handleBatchDelete,
              },
            ]}
          />
        )}

        {/* Path context bar: 路径摘要 + 统计 + 筛选 chip */}
        <PathContextBar
          breadcrumb={{
            workType: workTypeTitle,
            course: courseTitle,
            knowledge: knowledgeTitle,
          }}
          stats={{
            total: examPage?.total ?? 0,
            internal: sourceCount.internal,
            external: sourceCount.external,
            selected: selectedRowKeys.length,
          }}
          filters={{
            source: filterSource !== undefined ? String(filterSource) : undefined,
            type: filterType
              ? (Object.entries(EXAM_TYPE_MAP).find(([k]) => k === filterType)?.[1] ?? filterType)
              : undefined,
            difficulty:
              filterDifficulty !== undefined
                ? (DIFFICULTY_MAP[filterDifficulty] ?? String(filterDifficulty))
                : undefined,
          }}
          onFilterRemove={(key) => {
            if (key === 'source') {
              setFilterSource(undefined);
            } else if (key === 'type') {
              setFilterType(undefined);
            } else if (key === 'difficulty') {
              setFilterDifficulty(undefined);
            }
            setQuery((q) => ({ ...q, page: 1 }));
          }}
        />

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
            placeholder="来源"
            allowClear
            value={filterSource}
            onChange={(v) => {
              setFilterSource(v);
              setQuery((q) => ({ ...q, page: 1 }));
            }}
            options={[
              { value: 0, label: '内部' },
              { value: 1, label: '外部' },
            ]}
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
          {hasPerm('exam:question:delete') && (
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={selectedRowKeys.length === 0}
              onClick={handleBatchDelete}
            >
              <Badge
                count={selectedRowKeys.length}
                offset={[8, -2]}
                size="small"
                showZero={false}
              >
                <span>批量删除</span>
              </Badge>
            </Button>
          )}
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
          {hasPerm('exam:question:create') && (
            <>
              <Button
                icon={<UploadOutlined />}
                onClick={() => {
                  if (!selectedCourseId) {
                    message.warning('请先选择科目');
                    return;
                  }
                  setImportSource(0);
                  setImportFileList([]);
                  setImportModalOpen(true);
                }}
              >
                导入内部试题
              </Button>
              <Button
                icon={<UploadOutlined />}
                onClick={() => {
                  if (!selectedCourseId) {
                    message.warning('请先选择科目');
                    return;
                  }
                  setImportSource(1);
                  setImportFileList([]);
                  setImportModalOpen(true);
                }}
              >
                导入外部试题
              </Button>
            </>
          )}
          {hasPerm('exam:question:view') && (
            <Button
              icon={<DownloadOutlined />}
              loading={exportLoading}
              onClick={handleExport}
            >
              导出试题
            </Button>
          )}
        </Space>

        {/* Table */}
        <Table<ExamExamDetail>
          rowKey="id"
          columns={columns}
          dataSource={examPage?.items ?? []}
          loading={loadingDetails}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys as React.Key[]),
          }}
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

      {/* ── Question Form Modal ── */}
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

      {/* ── Import Modal ── */}
      <Modal
        title={importSource === 0 ? '导入内部试题' : '导入外部试题'}
        open={importModalOpen}
        onOk={handleImport}
        onCancel={() => {
          setImportModalOpen(false);
          setImportFileList([]);
        }}
        confirmLoading={importLoading}
        okText="开始导入"
        destroyOnClose
      >
        <Upload.Dragger
          accept=".xls,.xlsx"
          maxCount={1}
          fileList={importFileList}
          beforeUpload={() => false}
          onChange={({ fileList }) => setImportFileList(fileList)}
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
    </div>
  );
}
