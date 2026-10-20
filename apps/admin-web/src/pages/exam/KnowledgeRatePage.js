import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, MinusCircleOutlined, } from '@ant-design/icons';
import { Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, TreeSelect, Typography, message, Popconfirm, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pageKnowledgeRates, createKnowledgeRate, updateKnowledgeRate, deleteKnowledgeRate, pageCourses, treeKnowledgeStructures, } from '../../api/exam';
export function KnowledgeRatePage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, courseId: undefined });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['knowledge-rates', query],
        queryFn: () => pageKnowledgeRates(query),
    });
    const courseQuery = useQuery({
        queryKey: ['exam-courses-all'],
        queryFn: () => pageCourses({ page: 1, pageSize: 200 }),
    });
    // 知识点树 (用于 TreeSelect)
    const knowledgeQuery = useQuery({
        queryKey: ['knowledge-structures-tree', query.courseId],
        queryFn: () => treeKnowledgeStructures(query.courseId),
    });
    const createMutation = useMutation({
        mutationFn: createKnowledgeRate,
        onSuccess: () => {
            message.success('知识比重已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['knowledge-rates'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateKnowledgeRate(id, payload),
        onSuccess: () => {
            message.success('知识比重已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['knowledge-rates'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deleteKnowledgeRate,
        onSuccess: () => {
            message.success('知识比重已删除');
            queryClient.invalidateQueries({ queryKey: ['knowledge-rates'] });
        },
    });
    const handleAdd = useCallback(() => {
        setEditingId(null);
        form.resetFields();
        setModalOpen(true);
    }, [form]);
    const handleEdit = useCallback(async (record) => {
        setEditingId(record.id);
        // 加载详情
        const { getKnowledgeRate } = await import('../../api/exam');
        const detail = await getKnowledgeRate(record.id);
        form.setFieldsValue({
            title: detail.title,
            courseId: detail.courseId,
            type: detail.type,
            isStandard: detail.isStandard,
            comments: detail.comments,
            detailList: detail.detailList?.map((d) => ({
                knowledgeId: d.knowledgeId,
                rate: d.rate,
            })),
        });
        setModalOpen(true);
    }, [form]);
    const handleSubmit = useCallback(async () => {
        try {
            const values = await form.validateFields();
            const payload = { ...values };
            if (editingId) {
                updateMutation.mutate({ id: editingId, payload });
            }
            else {
                createMutation.mutate(payload);
            }
        }
        catch {
            // validation error
        }
    }, [form, editingId, createMutation, updateMutation]);
    // Convert tree data for TreeSelect (already in tree shape)
    const toTreeSelectData = (nodes) => nodes.map((n) => ({
        title: n.title,
        value: n.id,
        children: n.children?.length ? toTreeSelectData(n.children) : undefined,
    }));
    const knowledgeTreeData = toTreeSelectData(knowledgeQuery.data || []);
    const courses = courseQuery.data?.items || [];
    const columns = [
        { title: '序号', width: 60, render: (_, __, i) => i + 1 },
        { title: '名称', dataIndex: 'title', width: 180 },
        {
            title: '科目', dataIndex: 'courseId', width: 150,
            render: (v) => {
                const c = courses.find((c) => c.id === v);
                return c?.title || v;
            },
        },
        {
            title: '是否标准', dataIndex: 'isStandard', width: 90,
            render: (v) => (v === 1 ? _jsx(Tag, { color: "blue", children: "\u662F" }) : _jsx(Tag, { children: "\u5426" })),
        },
        { title: '排序号', dataIndex: 'sortNumber', width: 80 },
        { title: '备注', dataIndex: 'comments', width: 150, ellipsis: true },
        { title: '创建时间', dataIndex: 'createTime', width: 170 },
        {
            title: '操作', width: 120, fixed: 'right',
            render: (_, record) => (_jsxs(Space, { children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => handleEdit(record), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664?", onConfirm: () => deleteMutation.mutate(record.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    return (_jsxs("div", { children: [_jsxs(Space, { style: { marginBottom: 12 }, children: [_jsx(Select, { placeholder: "\u7B5B\u9009\u79D1\u76EE", allowClear: true, style: { width: 200 }, value: query.courseId, onChange: (v) => setQuery({ ...query, courseId: v, page: 1 }), options: courses.map((c) => ({ label: c.title, value: c.id })), showSearch: true, optionFilterProp: "label" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: handleAdd, children: "\u6DFB\u52A0" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => listQuery.refetch(), children: "\u5237\u65B0" })] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: listQuery.data?.items || [], loading: listQuery.isLoading, scroll: { x: 1000 }, pagination: {
                    current: query.page,
                    pageSize: query.pageSize,
                    total: listQuery.data?.total || 0,
                    showSizeChanger: true,
                    onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }),
                } }), _jsx(Modal, { title: editingId ? '编辑知识比重' : '添加知识比重', open: modalOpen, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, width: 700, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { name: "title", label: "\u540D\u79F0", rules: [{ required: true, message: '请输入名称' }], children: _jsx(Input, {}) }), _jsxs("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }, children: [_jsx(Form.Item, { name: "courseId", label: "\u79D1\u76EE", rules: [{ required: true }], children: _jsx(Select, { placeholder: "\u9009\u62E9\u79D1\u76EE", options: courses.map((c) => ({ label: c.title, value: c.id })), showSearch: true, optionFilterProp: "label" }) }), _jsx(Form.Item, { name: "isStandard", label: "\u662F\u5426\u6807\u51C6", children: _jsx(Select, { options: [
                                            { label: '是', value: 1 },
                                            { label: '否', value: 0 },
                                        ] }) })] }), _jsx(Form.Item, { name: "comments", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2 }) }), _jsx(Typography.Title, { level: 5, children: "\u77E5\u8BC6\u70B9\u6BD4\u91CD\u914D\u7F6E" }), _jsx(Form.List, { name: "detailList", children: (fields, { add, remove }) => (_jsxs(_Fragment, { children: [fields.map((field) => (_jsx(Card, { size: "small", style: { marginBottom: 8 }, children: _jsxs("div", { style: { display: 'flex', gap: 12, alignItems: 'flex-start' }, children: [_jsx(Form.Item, { name: [field.name, 'knowledgeId'], label: "\u77E5\u8BC6\u70B9", rules: [{ required: true }], style: { flex: 1, marginBottom: 0 }, children: _jsx(TreeSelect, { treeData: knowledgeTreeData, placeholder: "\u9009\u62E9\u77E5\u8BC6\u70B9", treeDefaultExpandAll: true, showSearch: true, treeNodeFilterProp: "title" }) }), _jsx(Form.Item, { name: [field.name, 'rate'], label: "\u767E\u5206\u6BD4(%)", rules: [{ required: true }], style: { width: 120, marginBottom: 0 }, children: _jsx(InputNumber, { min: 0, max: 100, style: { width: '100%' } }) }), _jsx(MinusCircleOutlined, { onClick: () => remove(field.name), style: { color: '#ff4d4f', marginTop: 36 } })] }) }, field.key))), _jsx(Button, { type: "dashed", onClick: () => add(), block: true, icon: _jsx(PlusOutlined, {}), children: "\u6DFB\u52A0\u77E5\u8BC6\u70B9\u6BD4\u91CD" })] })) })] }) })] }));
}
