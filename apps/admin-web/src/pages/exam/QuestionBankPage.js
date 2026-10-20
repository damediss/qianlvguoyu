import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UploadOutlined, DownloadOutlined, InboxOutlined, } from '@ant-design/icons';
import { Badge, Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Tree, TreeSelect, Typography, message, Popconfirm, Spin, Upload, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo, useEffect } from 'react';
import { getQuestionBankTree, pageExamDetails, createExamDetail, updateExamDetail, deleteExamDetail, batchDeleteExamDetails, importExamDetails, exportExamDetails, } from '../../api/exam';
import { parseContentDispositionFilename } from '../../utils/download';
import { useAuthStore } from '../../stores/authStore';
import { EXAM_TYPE_MAP, EXAM_SOURCE_MAP, DIFFICULTY_MAP, DIFFICULTY_COLOR_MAP, CHECK_STATUS_MAP, } from '../../types/exam';
import { PathContextBar } from '../../components/ContextBar/PathContextBar';
import { BatchActionBar } from '../../components/BatchActionBar/BatchActionBar';
const { Text } = Typography;
const { TextArea } = Input;
/**
 * 把后端聚合树(`QuestionBankTreeNodeVO[]`)转成 antd `Tree` 期望的形状。
 * 树深 6 级:工种 → 课程 → 知识点 L0 → L1 → L2 → L3,每节点都带
 * `questionCount`,渲染时挂 Badge。
 */
function buildTree(nodes) {
    return nodes.map((n) => {
        const childNodes = (n.children ?? []).map((c) => {
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
function findNodeWithPath(nodes, key, path = []) {
    for (const n of nodes) {
        if (n.key === key)
            return { node: n, path };
        const sub = findNodeWithPath(n.children ?? [], key, [...path, n]);
        if (sub)
            return sub;
    }
    return null;
}
/**
 * Badge 颜色: 0 题灰色,1~9 蓝,10+ 橙。
 * (参考 `题库树接口契约.md` chip 风格一节)
 */
function countColor(n) {
    if (n <= 0)
        return '#bfbfbf';
    if (n < 10)
        return '#1677ff';
    return '#fa8c16';
}
/**
 * 把新聚合树里的 `knowledge` 子树转成 modal 用的
 * `ExamKnowledgeStructure[]` 形态(id/title/level/children)。
 * 缺省字段用安全占位,modal 只读 `id/title/level/children`。
 */
function toKnowledgeStructures(nodes) {
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
function QuestionFormModal({ open, editing, knowledgeTreeData, courseId, onClose }) {
    const [form] = Form.useForm();
    const queryClient = useQueryClient();
    const treeSelectData = useMemo(() => {
        function convert(nodes) {
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
        onError: (e) => message.error(e.message || '创建失败'),
    });
    const updateMut = useMutation({
        mutationFn: ({ id, payload }) => updateExamDetail(id, payload),
        onSuccess: () => {
            message.success('更新成功');
            queryClient.invalidateQueries({ queryKey: ['examDetails'] });
            onClose();
        },
        onError: (e) => message.error(e.message || '更新失败'),
    });
    const handleOk = useCallback(async () => {
        try {
            const values = await form.validateFields();
            if (editing) {
                updateMut.mutate({ id: editing.id, payload: values });
            }
            else {
                createMut.mutate(values);
            }
        }
        catch {
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
            }
            else {
                form.resetFields();
            }
        }
    }, [open, editing, form]);
    return (_jsx(Modal, { title: editing ? '编辑试题' : '新建试题', open: open, onOk: handleOk, onCancel: onClose, confirmLoading: createMut.isPending || updateMut.isPending, width: 720, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", initialValues: { type: 'single', difficulty: 2 }, children: [_jsx(Form.Item, { name: "type", label: "\u9898\u578B", rules: [{ required: true, message: '请选择题型' }], children: _jsx(Select, { options: Object.entries(EXAM_TYPE_MAP).map(([k, v]) => ({ label: v, value: k })), placeholder: "\u9009\u62E9\u9898\u578B" }) }), _jsx(Form.Item, { name: "content", label: "\u9898\u5E72", children: _jsx(TextArea, { rows: 4, placeholder: "\u8BF7\u8F93\u5165\u9898\u5E72\u5185\u5BB9" }) }), _jsx(Form.Item, { name: "answer", label: "\u7B54\u6848", children: _jsx(TextArea, { rows: 3, placeholder: "\u8BF7\u8F93\u5165\u7B54\u6848\uFF08JSON \u6216\u6587\u672C\uFF09" }) }), _jsx(Form.Item, { name: "knowledgeId", label: "\u77E5\u8BC6\u70B9", children: _jsx(TreeSelect, { treeData: treeSelectData, showSearch: true, treeDefaultExpandAll: false, placeholder: "\u9009\u62E9\u77E5\u8BC6\u70B9", allowClear: true, treeNodeFilterProp: "title" }) }), _jsx(Form.Item, { name: "difficulty", label: "\u96BE\u5EA6", children: _jsx(Select, { options: Object.entries(DIFFICULTY_MAP).map(([k, v]) => ({
                            label: v,
                            value: Number(k),
                        })), placeholder: "\u9009\u62E9\u96BE\u5EA6" }) }), _jsx(Form.Item, { name: "score", label: "\u5206\u503C", children: _jsx(InputNumber, { min: 0, max: 9999, style: { width: '100%' }, placeholder: "\u5206\u503C" }) }), _jsx(Form.Item, { name: "resolve", label: "\u89E3\u6790", children: _jsx(TextArea, { rows: 3, placeholder: "\u8BF7\u8F93\u5165\u89E3\u6790" }) }), _jsx(Form.Item, { name: "keyword", label: "\u5173\u952E\u8BCD", children: _jsx(Input, { placeholder: "\u5173\u952E\u8BCD\uFF0C\u591A\u4E2A\u7528\u9017\u53F7\u5206\u9694" }) })] }) }));
}
// ─── Main Page ─────────────────────────────────────────────
export function QuestionBankPage() {
    const queryClient = useQueryClient();
    const hasPerm = useAuthStore((s) => s.hasPermission);
    // ── left tree state ──
    const [expandedKeys, setExpandedKeys] = useState([]);
    const [selectedKeys, setSelectedKeys] = useState([]);
    const [treeSearchValue, setTreeSearchValue] = useState('');
    // selected node metadata
    const [selectedWorkTypeId, setSelectedWorkTypeId] = useState();
    const [selectedCourseId, setSelectedCourseId] = useState();
    const [selectedKnowledgeId, setSelectedKnowledgeId] = useState();
    // ── right table state ──
    const [query, setQuery] = useState({
        page: 1,
        pageSize: 20,
    });
    const [filterType, setFilterType] = useState();
    const [filterDifficulty, setFilterDifficulty] = useState();
    const [filterSource, setFilterSource] = useState();
    const [filterKeyword, setFilterKeyword] = useState('');
    // ── selection state ──
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);
    // ── modal state ──
    const [modalOpen, setModalOpen] = useState(false);
    const [editingDetail, setEditingDetail] = useState(null);
    // ── import modal state ──
    const [importModalOpen, setImportModalOpen] = useState(false);
    const [importSource, setImportSource] = useState(0); // 0=内部, 1=外部, 3=赛前复习
    const [importFileList, setImportFileList] = useState([]);
    const [importLoading, setImportLoading] = useState(false);
    const [exportLoading, setExportLoading] = useState(false);
    // ── Data loading: 单次拉取完整 6 级聚合树 ──
    const { data: bankTree = [], isLoading: loadingBankTree, } = useQuery({
        queryKey: ['exam', 'question-bank-tree'],
        queryFn: getQuestionBankTree,
        staleTime: 60_000,
    });
    // antd Tree 用结构
    const treeData = useMemo(() => buildTree(bankTree), [bankTree]);
    // 默认展开工种 + 课程两级(取后端返回的第一层工种 + 第二层课程的 key)
    const defaultExpandedKeys = useMemo(() => {
        const keys = [];
        for (const wt of bankTree) {
            keys.push(wt.key);
            for (const c of wt.children ?? [])
                keys.push(c.key);
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
    const handleExpand = useCallback((keys) => {
        setExpandedKeys(keys);
    }, []);
    // Handle tree select — update right side filter
    const handleSelect = useCallback((keys, info) => {
        setSelectedKeys(keys);
        // 切换工种/科目/知识点时清空勾选，避免带着旧科目的勾选导出（会查出 0 条报错）
        setSelectedRowKeys([]);
        if (keys.length === 0) {
            setSelectedWorkTypeId(undefined);
            setSelectedCourseId(undefined);
            setSelectedKnowledgeId(undefined);
            setQuery((q) => ({ ...q, page: 1 }));
            return;
        }
        const node = info.node;
        setSelectedWorkTypeId(node.workTypeId);
        setSelectedCourseId(node.courseId);
        setSelectedKnowledgeId(node.knowledgeId);
        setQuery((q) => ({ ...q, page: 1 }));
    }, []);
    // ── Exam details query ──
    const effectiveQuery = useMemo(() => ({
        ...query,
        workTypeId: selectedWorkTypeId,
        courseId: selectedCourseId,
        knowledgeId: selectedKnowledgeId,
        type: filterType,
        difficulty: filterDifficulty,
        source: filterSource,
        keyword: filterKeyword || undefined,
    }), [
        query,
        selectedWorkTypeId,
        selectedCourseId,
        selectedKnowledgeId,
        filterType,
        filterDifficulty,
        filterSource,
        filterKeyword,
    ]);
    const { data: examPage, isLoading: loadingDetails } = useQuery({
        queryKey: ['examDetails', effectiveQuery],
        queryFn: () => pageExamDetails(effectiveQuery),
    });
    // ── Breadcrumb titles (从新聚合树里按 selectedKey 找 title + 祖先链) ──
    const breadcrumbTitles = useMemo(() => {
        const titles = {};
        if (selectedKeys.length === 0)
            return titles;
        const keyStr = String(selectedKeys[0]);
        const found = findNodeWithPath(bankTree, keyStr);
        if (!found)
            return titles;
        // 从祖先链 + 当前节点定位 workType / course / knowledge 三级
        const chain = [...found.path, found.node];
        for (const n of chain) {
            if (n.nodeType === 'workType')
                titles.workType = n.title;
            else if (n.nodeType === 'course')
                titles.course = n.title;
            else if (n.nodeType === 'knowledge')
                titles.knowledge = n.title;
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
            preCompetition: items.filter((it) => it.source === 3).length,
        };
    }, [examPage?.items]);
    // ── Delete mutation ──
    const deleteMut = useMutation({
        mutationFn: deleteExamDetail,
        onSuccess: () => {
            message.success('删除成功');
            queryClient.invalidateQueries({ queryKey: ['examDetails'] });
        },
        onError: (e) => message.error(e.message || '删除失败'),
    });
    // ── Batch delete mutation ──
    const batchDeleteMut = useMutation({
        mutationFn: batchDeleteExamDetails,
        onSuccess: (n) => {
            message.success(`已删除 ${n} 道试题`);
            setSelectedRowKeys([]);
            queryClient.invalidateQueries({ queryKey: ['examDetails'] });
        },
        onError: (e) => message.error(e.message || '批量删除失败'),
    });
    const handleBatchDelete = () => {
        const ids = selectedRowKeys.map(Number);
        if (ids.length === 0)
            return;
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
        formData.append('file', importFileList[0].originFileObj);
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
        }
        catch (e) {
            message.error(e?.message || '导入失败');
        }
        finally {
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
            const selectedIds = selectedRowKeys.map(Number);
            const resp = await exportExamDetails(selectedCourseId, selectedIds.length > 0 ? selectedIds : undefined);
            const blob = new Blob([resp.data], {
                type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            });
            const filename = parseContentDispositionFilename(resp) || '命题导出.xlsx';
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
            const exportedCount = selectedRowKeys.length;
            message.success(exportedCount > 0 ? `已导出选中的 ${exportedCount} 道试题` : '导出成功');
        }
        catch (e) {
            message.error(e?.message || '导出失败');
        }
        finally {
            setExportLoading(false);
        }
    };
    // ── Table columns ──
    const columns = [
        {
            title: '序号',
            width: 60,
            render: (_, __, index) => ((query.page ?? 1) - 1) * (query.pageSize ?? 20) + index + 1,
        },
        {
            title: '来源',
            dataIndex: 'source',
            width: 70,
            render: (v) => (_jsx(Tag, { color: v === 0 ? 'blue' : v === 3 ? 'green' : 'orange', children: EXAM_SOURCE_MAP[v] ?? v })),
        },
        {
            title: '题型',
            dataIndex: 'type',
            width: 90,
            render: (v) => EXAM_TYPE_MAP[v] || v,
        },
        {
            title: '题干与选项',
            dataIndex: 'content',
            ellipsis: true,
            render: (v) => (_jsx("div", { style: { maxHeight: 80, overflow: 'hidden' }, children: v ? v.substring(0, 120) : '-' })),
        },
        {
            title: '知识点',
            dataIndex: 'knowledgeTitle',
            width: 190,
            ellipsis: true,
            render: (v) => v || '-',
        },
        {
            title: '难度',
            dataIndex: 'difficulty',
            width: 60,
            render: (v) => (_jsx(Tag, { color: DIFFICULTY_COLOR_MAP[v] || 'default', children: DIFFICULTY_MAP[v] || v })),
        },
        {
            title: '初审',
            dataIndex: 'checkStatus',
            width: 70,
            render: (v) => {
                const colors = { 0: 'default', 1: 'success', 2: 'error' };
                return _jsx(Tag, { color: colors[v], children: CHECK_STATUS_MAP[v] || v });
            },
        },
        {
            title: '更新时间',
            dataIndex: 'updateTime',
            width: 130,
            render: (v, record) => {
                const t = v || record.createTime;
                return t ? String(t).substring(0, 16) : '-';
            },
        },
        {
            title: '操作',
            width: 150,
            render: (_, record) => (_jsxs(Space, { size: "small", children: [hasPerm('exam:question:update') && (_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => {
                            setEditingDetail(record);
                            setModalOpen(true);
                        }, children: "\u7F16\u8F91" })), hasPerm('exam:question:delete') && (_jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u6B64\u8BD5\u9898\uFF1F", onConfirm: () => deleteMut.mutate(record.id), okText: "\u786E\u8BA4", cancelText: "\u53D6\u6D88", children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) }))] })),
        },
    ];
    // ── Filter tree by search ──
    // 行为:输入框非空时,递归遍历树,保留"标题命中关键词"的节点 + 它的所有祖先链;
    //      同时把命中的节点 key 自动加入 expandedKeys,确保能看到。
    //      输入清空时恢复原树。
    const filterTreeByKeyword = useCallback((nodes, keyword) => {
        const k = keyword.trim().toLowerCase();
        const matchedKeys = [];
        const walk = (n, ancestors) => {
            const selfHit = n.title.toLowerCase().includes(k);
            const keptChildren = [];
            for (const c of n.children ?? []) {
                const r = walk(c, [...ancestors, n]);
                if (r)
                    keptChildren.push(r);
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
        const out = [];
        for (const n of nodes) {
            const r = walk(n, []);
            if (r)
                out.push(r);
        }
        return { matched: out, matchedKeys };
    }, []);
    const filteredTreeData = useMemo(() => {
        if (!treeSearchValue.trim())
            return treeData;
        return filterTreeByKeyword(treeData, treeSearchValue).matched;
    }, [treeData, treeSearchValue, filterTreeByKeyword]);
    // 搜索时:把命中的 key 自动加入 expandedKeys,这样用户能直接看到命中节点
    useEffect(() => {
        const k = treeSearchValue.trim();
        if (!k)
            return;
        const { matchedKeys } = filterTreeByKeyword(treeData, k);
        if (matchedKeys.length === 0)
            return;
        setExpandedKeys((prev) => {
            const set = new Set(prev.map(String));
            for (const mk of matchedKeys)
                set.add(mk);
            return Array.from(set);
        });
    }, [treeSearchValue, treeData, filterTreeByKeyword]);
    // Get current knowledge tree for modal — 从新聚合树里找选中课程的子节点
    const currentKnowledgeTree = useMemo(() => {
        if (selectedCourseId === undefined)
            return [];
        // 找该 course 节点,转成 ExamKnowledgeStructure 形态给 modal
        let courseNode;
        for (const wt of bankTree) {
            for (const c of wt.children ?? []) {
                if (c.id === selectedCourseId) {
                    courseNode = c;
                    break;
                }
            }
            if (courseNode)
                break;
        }
        if (!courseNode)
            return [];
        return toKnowledgeStructures(courseNode.children ?? []);
    }, [selectedCourseId, bankTree]);
    return (_jsxs("div", { style: { display: 'flex', gap: 16, height: '100%' }, children: [_jsx(Card, { style: { width: 300, flexShrink: 0, overflow: 'auto' }, size: "small", title: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsx(Text, { strong: true, children: "\u5206\u7C7B\u5BFC\u822A" }), _jsx(Input, { placeholder: "\u641C\u7D22", prefix: _jsx(SearchOutlined, {}), value: treeSearchValue, onChange: (e) => setTreeSearchValue(e.target.value), allowClear: true })] }), children: _jsx(Spin, { spinning: loadingBankTree, children: _jsx(Tree, { treeData: filteredTreeData, expandedKeys: expandedKeys, onExpand: handleExpand, selectedKeys: selectedKeys, onSelect: handleSelect, blockNode: true, showLine: true, titleRender: (node) => {
                            const tn = node;
                            const isBold = tn.nodeType === 'workType' || tn.nodeType === 'course';
                            // 高亮搜索命中的子串(忽略大小写),用 <mark> 默认样式
                            const k = treeSearchValue.trim();
                            const titleNode = (() => {
                                if (!k) {
                                    return isBold ? _jsx(Text, { strong: true, children: tn.title }) : _jsx(Text, { children: tn.title });
                                }
                                const lower = tn.title.toLowerCase();
                                const idx = lower.indexOf(k.toLowerCase());
                                if (idx < 0) {
                                    return isBold ? _jsx(Text, { strong: true, children: tn.title }) : _jsx(Text, { children: tn.title });
                                }
                                const before = tn.title.slice(0, idx);
                                const hit = tn.title.slice(idx, idx + k.length);
                                const after = tn.title.slice(idx + k.length);
                                const content = (_jsxs(_Fragment, { children: [before, _jsx("mark", { style: { background: '#fff566', padding: '0 2px' }, children: hit }), after] }));
                                return isBold ? _jsx(Text, { strong: true, children: content }) : _jsx(Text, { children: content });
                            })();
                            return (_jsxs("span", { style: { display: 'inline-flex', alignItems: 'center', gap: 8 }, children: [titleNode, _jsx(Badge, { count: tn.questionCount, showZero: true, style: {
                                            backgroundColor: countColor(tn.questionCount),
                                            color: '#fff',
                                            boxShadow: 'none',
                                        }, title: `${tn.questionCount} 道题` })] }));
                        } }) }) }), _jsxs("div", { style: { flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }, children: [selectedRowKeys.length > 0 && (_jsx(BatchActionBar, { count: selectedRowKeys.length, onClear: () => setSelectedRowKeys([]), actions: [
                            {
                                key: 'batch-delete',
                                label: '批量删除',
                                danger: true,
                                onClick: handleBatchDelete,
                            },
                        ] })), _jsx(PathContextBar, { breadcrumb: {
                            workType: workTypeTitle,
                            course: courseTitle,
                            knowledge: knowledgeTitle,
                        }, stats: {
                            total: examPage?.total ?? 0,
                            internal: sourceCount.internal,
                            external: sourceCount.external,
                            preCompetition: sourceCount.preCompetition,
                            selected: selectedRowKeys.length,
                        }, filters: {
                            source: filterSource !== undefined ? String(filterSource) : undefined,
                            type: filterType
                                ? (Object.entries(EXAM_TYPE_MAP).find(([k]) => k === filterType)?.[1] ?? filterType)
                                : undefined,
                            difficulty: filterDifficulty !== undefined
                                ? (DIFFICULTY_MAP[filterDifficulty] ?? String(filterDifficulty))
                                : undefined,
                        }, onFilterRemove: (key) => {
                            if (key === 'source') {
                                setFilterSource(undefined);
                            }
                            else if (key === 'type') {
                                setFilterType(undefined);
                            }
                            else if (key === 'difficulty') {
                                setFilterDifficulty(undefined);
                            }
                            setQuery((q) => ({ ...q, page: 1 }));
                        } }), _jsxs(Space, { wrap: true, children: [_jsx(Select, { style: { width: 120 }, placeholder: "\u9898\u578B", allowClear: true, value: filterType, onChange: (v) => {
                                    setFilterType(v);
                                    setQuery((q) => ({ ...q, page: 1 }));
                                }, options: Object.entries(EXAM_TYPE_MAP).map(([k, v]) => ({ label: v, value: k })) }), _jsx(Select, { style: { width: 100 }, placeholder: "\u6765\u6E90", allowClear: true, value: filterSource, onChange: (v) => {
                                    setFilterSource(v);
                                    setQuery((q) => ({ ...q, page: 1 }));
                                }, options: [
                                    { value: 0, label: '内部' },
                                    { value: 1, label: '外部' },
                                    { value: 3, label: '赛前复习' },
                                ] }), _jsx(Select, { style: { width: 100 }, placeholder: "\u96BE\u5EA6", allowClear: true, value: filterDifficulty, onChange: (v) => {
                                    setFilterDifficulty(v);
                                    setQuery((q) => ({ ...q, page: 1 }));
                                }, options: Object.entries(DIFFICULTY_MAP).map(([k, v]) => ({
                                    label: v,
                                    value: Number(k),
                                })) }), _jsx(Input.Search, { style: { width: 200 }, placeholder: "\u641C\u7D22\u9898\u5E72", allowClear: true, onSearch: (v) => {
                                    setFilterKeyword(v);
                                    setQuery((q) => ({ ...q, page: 1 }));
                                } }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['examDetails'] }), children: "\u5237\u65B0" }), hasPerm('exam:question:delete') && (_jsx(Button, { danger: true, icon: _jsx(DeleteOutlined, {}), disabled: selectedRowKeys.length === 0, onClick: handleBatchDelete, children: _jsx(Badge, { count: selectedRowKeys.length, offset: [8, -2], size: "small", showZero: false, children: _jsx("span", { children: "\u6279\u91CF\u5220\u9664" }) }) })), hasPerm('exam:question:create') && (_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => {
                                    setEditingDetail(null);
                                    setModalOpen(true);
                                }, children: "\u65B0\u5EFA\u8BD5\u9898" })), hasPerm('exam:question:create') && (_jsxs(_Fragment, { children: [_jsx(Button, { icon: _jsx(UploadOutlined, {}), onClick: () => {
                                            if (!selectedCourseId) {
                                                message.warning('请先选择科目');
                                                return;
                                            }
                                            setImportSource(0);
                                            setImportFileList([]);
                                            setImportModalOpen(true);
                                        }, children: "\u5BFC\u5165\u5185\u90E8\u8BD5\u9898" }), _jsx(Button, { icon: _jsx(UploadOutlined, {}), onClick: () => {
                                            if (!selectedCourseId) {
                                                message.warning('请先选择科目');
                                                return;
                                            }
                                            setImportSource(1);
                                            setImportFileList([]);
                                            setImportModalOpen(true);
                                        }, children: "\u5BFC\u5165\u5916\u90E8\u8BD5\u9898" }), _jsx(Button, { icon: _jsx(UploadOutlined, {}), onClick: () => {
                                            if (!selectedCourseId) {
                                                message.warning('请先选择科目');
                                                return;
                                            }
                                            setImportSource(3);
                                            setImportFileList([]);
                                            setImportModalOpen(true);
                                        }, children: "\u5BFC\u5165\u8D5B\u524D\u590D\u4E60\u8BD5\u9898" })] })), hasPerm('exam:question:view') && (_jsxs(Button, { icon: _jsx(DownloadOutlined, {}), loading: exportLoading, onClick: handleExport, children: ["\u5BFC\u51FA\u8BD5\u9898", selectedRowKeys.length > 0 ? `（已选 ${selectedRowKeys.length} 题）` : ''] }))] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: examPage?.items ?? [], loading: loadingDetails, rowSelection: {
                            selectedRowKeys,
                            onChange: (keys) => setSelectedRowKeys(keys),
                        }, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total: examPage?.total ?? 0,
                            showSizeChanger: true,
                            showTotal: (total) => `共 ${total} 条`,
                            onChange: (page, pageSize) => setQuery((q) => ({ ...q, page, pageSize })),
                        }, size: "small", scroll: { x: 1000 } })] }), _jsx(QuestionFormModal, { open: modalOpen, editing: editingDetail, knowledgeTreeData: currentKnowledgeTree, courseId: selectedCourseId, onClose: () => {
                    setModalOpen(false);
                    setEditingDetail(null);
                } }), _jsxs(Modal, { title: `导入${EXAM_SOURCE_MAP[importSource] ?? ''}试题`, open: importModalOpen, onOk: handleImport, onCancel: () => {
                    setImportModalOpen(false);
                    setImportFileList([]);
                }, confirmLoading: importLoading, okText: "\u5F00\u59CB\u5BFC\u5165", destroyOnHidden: true, children: [_jsxs(Upload.Dragger, { accept: ".xls,.xlsx", maxCount: 1, fileList: importFileList, beforeUpload: () => false, onChange: ({ fileList }) => setImportFileList(fileList), children: [_jsx("p", { style: { fontSize: 40, color: '#1677ff', marginBottom: 8 }, children: _jsx(InboxOutlined, {}) }), _jsx("p", { style: { fontSize: 14 }, children: "\u70B9\u51FB\u6216\u5C06\u6587\u4EF6\u62D6\u62FD\u5230\u8FD9\u91CC\u4E0A\u4F20" }), _jsx("p", { style: { color: '#999', fontSize: 12 }, children: "\u652F\u6301 .xls / .xlsx \u683C\u5F0F" })] }), _jsx("div", { style: { marginTop: 12 }, children: _jsxs("a", { href: "/templates/exam-import-template.xlsx", download: "\u8BD5\u9898\u5BFC\u5165\u6A21\u677F-\u547D\u9898.xlsx", children: [_jsx(DownloadOutlined, {}), " \u4E0B\u8F7D\u5BFC\u5165\u6A21\u677F"] }) })] })] }));
}
