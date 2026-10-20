import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { PlusOutlined, EditOutlined, DeleteOutlined, UploadOutlined, DownloadOutlined, InboxOutlined, ReloadOutlined, } from '@ant-design/icons';
import { Button, Card, Form, Input, InputNumber, Modal, Radio, Select, Space, Table, Tabs, Tree, TreeSelect, Typography, Upload, message, Popconfirm, Spin, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo } from 'react';
import { pageCourses, treeWorkTypes, treeKnowledgeStructures, createKnowledgeStructure, updateKnowledgeStructure, deleteKnowledgeStructure, batchDeleteKnowledgeStructures, importKnowledgeStructureExcel, exportKnowledgeStructures, } from '../../api/exam';
const { Text } = Typography;
const { TextArea } = Input;
// 重要等级选项
const IMPORTANT_OPTIONS = [
    { label: 'X', value: 0 },
    { label: 'Y', value: 1 },
    { label: 'Z', value: 2 },
];
const IMPORTANT_MAP = { 0: 'X', 1: 'Y', 2: 'Z' };
// 构建工种树数据
function buildTreeSelectData(nodes) {
    return nodes.map((node) => ({
        title: node.title,
        value: node.id,
        children: node.children ? buildTreeSelectData(node.children) : undefined,
    }));
}
function buildTreeData(nodes) {
    return nodes.map((n) => ({
        key: String(n.id),
        title: n.title,
        children: n.children && n.children.length > 0 ? buildTreeData(n.children) : undefined,
        isLeaf: !n.children || n.children.length === 0,
    }));
}
// 构建知识点树 (用于 TreeSelect 上级选择)
function buildParentTreeData(nodes) {
    return nodes.map((n) => ({
        title: n.title,
        value: n.id,
        children: n.children && n.children.length > 0 ? buildParentTreeData(n.children) : undefined,
    }));
}
// 扁平化树节点 (用于查找节点详情)
function flattenTree(nodes) {
    const result = [];
    const walk = (list) => {
        for (const n of list) {
            result.push(n);
            if (n.children && n.children.length > 0)
                walk(n.children);
        }
    };
    walk(nodes);
    return result;
}
function NodeFormModal({ open, editing, courseId, activeType, treeData, onClose }) {
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
        onError: (e) => message.error(e.message || '创建失败'),
    });
    const updateMut = useMutation({
        mutationFn: ({ id, payload }) => updateKnowledgeStructure(id, payload),
        onSuccess: () => {
            message.success('更新成功');
            queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
            onClose();
        },
        onError: (e) => message.error(e.message || '更新失败'),
    });
    const handleOk = useCallback(async () => {
        try {
            const values = await form.validateFields();
            const payload = {
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
            }
            else {
                createMut.mutate(payload);
            }
        }
        catch {
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
            }
            else {
                form.resetFields();
                form.setFieldsValue({ sortNumber: 100, important: 0, amount: 0 });
            }
        }
    }, [open, editing, form, activeType]);
    return (_jsx(Modal, { title: editing ? '编辑节点' : '添加节点', open: open, onOk: handleOk, onCancel: onClose, confirmLoading: createMut.isPending || updateMut.isPending, width: 560, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { name: "title", label: "\u6807\u9898", rules: [{ required: true, message: '请输入标题' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8282\u70B9\u6807\u9898" }) }), _jsx(Form.Item, { name: "type", label: "\u7C7B\u578B", children: _jsxs(Radio.Group, { children: [_jsx(Radio, { value: 0, children: "\u7406\u8BBA" }), _jsx(Radio, { value: 1, children: "\u5B9E\u64CD" })] }) }), _jsx(Form.Item, { name: "intro", label: "\u7B80\u4ECB", children: _jsx(TextArea, { rows: 3, placeholder: "\u8BF7\u8F93\u5165\u7B80\u4ECB" }) }), _jsx(Form.Item, { name: "parentId", label: "\u4E0A\u7EA7\u8282\u70B9", children: _jsx(TreeSelect, { treeData: parentTreeData, showSearch: true, treeDefaultExpandAll: false, placeholder: "\u9009\u62E9\u4E0A\u7EA7\u8282\u70B9", allowClear: true, treeNodeFilterProp: "title" }) }), _jsx(Form.Item, { name: "important", label: "\u91CD\u8981\u7B49\u7EA7", children: _jsx(Select, { options: IMPORTANT_OPTIONS, placeholder: "\u9009\u62E9\u91CD\u8981\u7B49\u7EA7" }) }), _jsx(Form.Item, { name: "amount", label: "\u5355\u4EF7", children: _jsx(InputNumber, { min: 0, max: 99999, style: { width: '100%' }, placeholder: "0" }) }), _jsx(Form.Item, { name: "seriesNo", label: "\u7F16\u53F7", children: _jsx(Input, { placeholder: "\u5982 01, 0101" }) }), _jsx(Form.Item, { name: "sortNumber", label: "\u6392\u5E8F\u53F7", children: _jsx(InputNumber, { min: 0, max: 9999, style: { width: '100%' }, placeholder: "100" }) })] }) }));
}
function ImportModal({ open, courseId, activeType, onClose }) {
    const [fileList, setFileList] = useState([]);
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
        formData.append('file', fileList[0].originFileObj);
        formData.append('courseId', String(courseId));
        formData.append('type', String(activeType));
        setLoading(true);
        try {
            const count = await importKnowledgeStructureExcel(formData);
            message.success(`成功导入 ${count} 个知识点`);
            setFileList([]);
            queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
            onClose();
        }
        catch (e) {
            message.error(e?.message || '导入失败');
        }
        finally {
            setLoading(false);
        }
    };
    const handleClose = () => {
        setFileList([]);
        onClose();
    };
    return (_jsxs(Modal, { title: "\u5BFC\u5165\u77E5\u8BC6\u7ED3\u6784", open: open, onOk: handleImport, onCancel: handleClose, confirmLoading: loading, okText: "\u5F00\u59CB\u5BFC\u5165", destroyOnHidden: true, children: [_jsxs(Upload.Dragger, { accept: ".xls,.xlsx", maxCount: 1, fileList: fileList, beforeUpload: () => false, onChange: ({ fileList: fl }) => setFileList(fl), children: [_jsx("p", { style: { fontSize: 40, color: '#1677ff', marginBottom: 8 }, children: _jsx(InboxOutlined, {}) }), _jsx("p", { style: { fontSize: 14 }, children: "\u70B9\u51FB\u6216\u5C06\u6587\u4EF6\u62D6\u62FD\u5230\u8FD9\u91CC\u4E0A\u4F20" }), _jsx("p", { style: { color: '#999', fontSize: 12 }, children: "\u652F\u6301 .xls / .xlsx \u683C\u5F0F" })] }), _jsx("div", { style: { marginTop: 12 }, children: _jsxs("a", { href: "/templates/knowledge-import-template.xlsx", download: "\u77E5\u8BC6\u7ED3\u6784\u5BFC\u5165\u6A21\u677F.xlsx", children: [_jsx(DownloadOutlined, {}), " \u4E0B\u8F7D\u5BFC\u5165\u6A21\u677F"] }) })] }));
}
// ─── 主页面 ───────────────────────────────────────
export function KnowledgeStructurePage() {
    const queryClient = useQueryClient();
    // ── 左侧面板状态 ──
    const [courseQuery, setCourseQuery] = useState({ page: 1, pageSize: 10, title: '' });
    const [workTypeId, setWorkTypeId] = useState();
    const [selectedCourseId, setSelectedCourseId] = useState();
    const [selectedCourseTitle, setSelectedCourseTitle] = useState('');
    // ── 右侧面板状态 ──
    const [activeType, setActiveType] = useState(0); // 0=理论, 1=实操
    const [selectedKeys, setSelectedKeys] = useState([]);
    const [checkedKeys, setCheckedKeys] = useState([]);
    const [expandedKeys, setExpandedKeys] = useState([]);
    // ── 弹窗状态 ──
    const [nodeModalOpen, setNodeModalOpen] = useState(false);
    const [editingNode, setEditingNode] = useState(null);
    const [importModalOpen, setImportModalOpen] = useState(false);
    // ── 数据加载 ──
    const workTypeQuery = useQuery({
        queryKey: ['exam-work-types-tree'],
        queryFn: treeWorkTypes,
    });
    const courseListQuery = useQuery({
        queryKey: ['ks-courses', courseQuery],
        queryFn: () => pageCourses({
            ...courseQuery,
            workTypeId: workTypeId || undefined,
        }),
    });
    const treeQuery = useQuery({
        queryKey: ['knowledge-tree', selectedCourseId, activeType],
        queryFn: () => treeKnowledgeStructures(selectedCourseId, activeType),
        enabled: !!selectedCourseId,
    });
    const treeData = useMemo(() => (treeQuery.data ? buildTreeData(treeQuery.data) : []), [treeQuery.data]);
    const flatNodes = useMemo(() => (treeQuery.data ? flattenTree(treeQuery.data) : []), [treeQuery.data]);
    const selectedNode = useMemo(() => flatNodes.find((n) => String(n.id) === String(selectedKeys[0])) || null, [flatNodes, selectedKeys]);
    // ── Mutations ──
    const deleteMut = useMutation({
        mutationFn: deleteKnowledgeStructure,
        onSuccess: () => {
            message.success('删除成功');
            setSelectedKeys([]);
            queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
        },
        onError: (e) => message.error(e.message || '删除失败'),
    });
    const batchDeleteMut = useMutation({
        mutationFn: batchDeleteKnowledgeStructures,
        onSuccess: (n) => {
            message.success(`已删除 ${n} 个知识点`);
            setCheckedKeys([]);
            setSelectedKeys([]);
            queryClient.invalidateQueries({ queryKey: ['knowledge-tree'] });
        },
        onError: (e) => message.error(e.message || '批量删除失败'),
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
                if (match)
                    filename = decodeURIComponent(match[1]);
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
        }
        catch (e) {
            message.error(e?.message || '导出失败');
        }
    };
    // ── 批量删除 ──
    const handleBatchDelete = () => {
        const ids = checkedKeys.map(Number);
        if (ids.length === 0)
            return;
        Modal.confirm({
            title: `确认删除 ${ids.length} 个知识点?`,
            content: '删除后无法恢复，且会级联删除子节点',
            okText: '确认删除',
            okButtonProps: { danger: true },
            onOk: () => batchDeleteMut.mutate(ids),
        });
    };
    // ── 表格列定义 ──
    const columns = [
        {
            title: '科目名称',
            dataIndex: 'title',
            ellipsis: true,
            render: (text) => _jsx(Text, { strong: true, children: text }),
        },
        {
            title: '科目编码',
            dataIndex: 'seriesNo',
            width: 120,
        },
    ];
    const courses = courseListQuery.data?.items || [];
    const courseTotal = courseListQuery.data?.total || 0;
    return (_jsxs("div", { style: { display: 'flex', gap: 16, height: '100%' }, children: [_jsxs(Card, { style: { width: 320, flexShrink: 0, overflow: 'auto' }, size: "small", title: _jsx(Text, { strong: true, children: "\u79D1\u76EE\u5217\u8868" }), children: [_jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsx(TreeSelect, { style: { width: '100%' }, placeholder: "\u6309\u5DE5\u79CD\u7B5B\u9009", allowClear: true, showSearch: true, treeDefaultExpandAll: false, treeNodeFilterProp: "title", treeData: buildTreeSelectData(workTypeQuery.data || []), value: workTypeId, onChange: (v) => {
                                    setWorkTypeId(v);
                                    setCourseQuery((q) => ({ ...q, page: 1 }));
                                } }), _jsx(Input.Search, { placeholder: "\u641C\u7D22\u79D1\u76EE\u540D\u79F0/\u7F16\u7801", allowClear: true, onSearch: (v) => {
                                    setCourseQuery((q) => ({ ...q, title: v, page: 1 }));
                                } }), _jsx(Button, { size: "small", icon: _jsx(ReloadOutlined, {}), onClick: () => courseListQuery.refetch(), children: "\u5237\u65B0" })] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: courses, loading: courseListQuery.isLoading, size: "small", pagination: {
                            current: courseQuery.page,
                            pageSize: courseQuery.pageSize,
                            total: courseTotal,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setCourseQuery((q) => ({ ...q, page, pageSize })),
                        }, onRow: (record) => ({
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
                        }), style: { marginTop: 12 } })] }), _jsx("div", { style: { flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }, children: selectedCourseId ? (_jsx(_Fragment, { children: _jsxs(Card, { size: "small", title: _jsxs(Text, { strong: true, children: [selectedCourseTitle, " \u2014 \u77E5\u8BC6\u7ED3\u6784"] }), children: [_jsx(Tabs, { activeKey: String(activeType), onChange: (key) => {
                                    setActiveType(Number(key));
                                    setSelectedKeys([]);
                                    setCheckedKeys([]);
                                }, items: [
                                    { key: '0', label: '理论' },
                                    { key: '1', label: '实操' },
                                ], style: { marginBottom: 12 } }), _jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), onClick: () => setImportModalOpen(true), children: "\u6279\u91CF\u5BFC\u5165" }), _jsx(Button, { type: "primary", icon: _jsx(DownloadOutlined, {}), onClick: handleExport, children: "\u5BFC\u51FA" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => {
                                            setEditingNode(null);
                                            setNodeModalOpen(true);
                                        }, children: "\u6DFB\u52A0" }), _jsx(Button, { icon: _jsx(EditOutlined, {}), disabled: !selectedNode, onClick: () => {
                                            if (selectedNode) {
                                                setEditingNode(selectedNode);
                                                setNodeModalOpen(true);
                                            }
                                        }, children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u6B64\u8282\u70B9\uFF1F", description: "\u4F1A\u7EA7\u8054\u5220\u9664\u6240\u6709\u5B50\u8282\u70B9", onConfirm: () => {
                                            if (selectedNode)
                                                deleteMut.mutate(selectedNode.id);
                                        }, okText: "\u786E\u8BA4", cancelText: "\u53D6\u6D88", children: _jsx(Button, { icon: _jsx(DeleteOutlined, {}), danger: true, disabled: !selectedNode, children: "\u5220\u9664" }) }), _jsxs(Button, { icon: _jsx(DeleteOutlined, {}), danger: true, disabled: checkedKeys.length === 0, onClick: handleBatchDelete, children: ["\u6279\u91CF\u5220\u9664", checkedKeys.length > 0 ? `(${checkedKeys.length})` : ''] })] }), _jsx(Spin, { spinning: treeQuery.isLoading, children: _jsx(Tree, { treeData: treeData, expandedKeys: expandedKeys, onExpand: (keys) => setExpandedKeys(keys), selectedKeys: selectedKeys, onSelect: (keys) => setSelectedKeys(keys), checkedKeys: checkedKeys, onCheck: (keys) => setCheckedKeys(keys), checkable: true, showLine: true, style: { minHeight: 200 } }) })] }) })) : (_jsx(Card, { style: { height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }, children: _jsx(Text, { type: "secondary", children: "\u8BF7\u5728\u5DE6\u4FA7\u9009\u62E9\u4E00\u4E2A\u79D1\u76EE" }) })) }), _jsx(NodeFormModal, { open: nodeModalOpen, editing: editingNode, courseId: selectedCourseId, activeType: activeType, treeData: treeQuery.data || [], onClose: () => {
                    setNodeModalOpen(false);
                    setEditingNode(null);
                } }), _jsx(ImportModal, { open: importModalOpen, courseId: selectedCourseId, activeType: activeType, onClose: () => setImportModalOpen(false) })] }));
}
