import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Button, Card, Col, Form, Input, Modal, Popconfirm, Row, Select, Space, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
export function TrainingSummaryPage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [searchForm] = Form.useForm();
    const [query, setQuery] = useState({ page: 1, pageSize: 20 });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const { data, isLoading } = useQuery({
        queryKey: ['training-summaries', query],
        queryFn: () => trainApi.pageSummaries(query),
    });
    const createMu = useMutation({
        mutationFn: (p) => trainApi.createSummary(p),
        onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-summaries'] }); },
        onError: () => message.error('创建失败'),
    });
    const updateMu = useMutation({
        mutationFn: ({ id, p }) => trainApi.updateSummary(id, p),
        onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-summaries'] }); },
        onError: () => message.error('更新失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteSummary(id),
        onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-summaries'] }); },
    });
    const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };
    const openCreate = () => { form.resetFields(); setEditingId(null); setModalOpen(true); };
    const openEdit = (r) => { setEditingId(r.id); form.setFieldsValue(r); setModalOpen(true); };
    const handleSubmit = async () => {
        const v = await form.validateFields();
        editingId ? updateMu.mutate({ id: editingId, p: v }) : createMu.mutate(v);
    };
    const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 70 },
        { title: '标题', dataIndex: 'title', width: 200, ellipsis: true },
        { title: '作者', dataIndex: 'author', width: 120 },
        { title: '标签', dataIndex: 'tags', width: 120, render: (v) => v ? v.split(',').map((t) => _jsx(Tag, { children: t.trim() }, t)) : null },
        { title: '状态', dataIndex: 'status', width: 100, render: (v) => v === 1 ? _jsx(Tag, { color: "success", children: "\u5DF2\u53D1\u5E03" }) : _jsx(Tag, { children: "\u8349\u7A3F" }) },
        { title: '创建时间', dataIndex: 'createTime', width: 160 },
        {
            title: '操作', key: 'act', width: 120, fixed: 'right',
            render: (_, r) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(r), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664?", onConfirm: () => deleteMu.mutate(r.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u7ECF\u9A8C\u8403\u53D6" }), _jsx(Typography.Text, { type: "secondary", children: "\u7BA1\u7406\u57F9\u8BAD\u7ECF\u9A8C\u603B\u7ED3\u4E0E\u77E5\u8BC6\u8403\u53D6\uFF0C\u652F\u6301\u7ECF\u9A8C\u6587\u6863\u7684\u521B\u5EFA\u3001\u7F16\u8F91\u548C\u53D1\u5E03" })] }) }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Form, { form: searchForm, layout: "inline", children: [_jsx(Form.Item, { name: "keyword", children: _jsx(Input, { placeholder: "\u641C\u7D22\u6807\u9898\u3001\u6807\u7B7E...", prefix: _jsx(SearchOutlined, {}), allowClear: true, onPressEnter: handleSearch, style: { width: 280 } }) }), _jsx(Form.Item, { name: "status", children: _jsx(Select, { allowClear: true, placeholder: "\u72B6\u6001", style: { width: 120 }, options: [{ value: 0, label: '草稿' }, { value: 1, label: '已发布' }] }) }), _jsx(Form.Item, { children: _jsx(Button, { type: "primary", onClick: handleSearch, children: "\u67E5\u8BE2" }) })] }) }), _jsxs(Card, { size: "small", children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['training-summaries'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u7ECF\u9A8C" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: isLoading, columns: columns, dataSource: data?.items ?? [], scroll: { x: 800 }, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total: data?.total ?? 0,
                            showSizeChanger: true,
                            pageSizeOptions: ['10', '20', '50', '100'],
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (p, ps) => {
                                setQuery((prev) => ({
                                    ...prev,
                                    page: typeof ps === 'number' && ps !== prev.pageSize ? 1 : p,
                                    pageSize: typeof ps === 'number' && ps > 0 ? ps : prev.pageSize,
                                }));
                            },
                        } })] }), _jsx(Modal, { title: editingId ? '编辑经验' : '新增经验', open: modalOpen, onOk: handleSubmit, onCancel: closeModal, confirmLoading: createMu.isPending || updateMu.isPending, destroyOnHidden: true, width: 700, children: _jsx(Form, { form: form, layout: "vertical", children: _jsxs(Row, { gutter: 24, children: [_jsx(Col, { span: 24, children: _jsx(Form.Item, { name: "title", label: "\u6807\u9898", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u6807\u9898" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "author", label: "\u4F5C\u8005", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "tags", label: "\u6807\u7B7E", children: _jsx(Input, { placeholder: "\u591A\u4E2A\u6807\u7B7E\u7528\u9017\u53F7\u5206\u9694" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: [{ value: 0, label: '草稿' }, { value: 1, label: '已发布' }] }) }) }), _jsx(Col, { span: 24, children: _jsx(Form.Item, { name: "content", label: "\u5185\u5BB9", children: _jsx(Input.TextArea, { rows: 6, placeholder: "\u8BF7\u8F93\u5165\u7ECF\u9A8C\u603B\u7ED3\u5185\u5BB9" }) }) })] }) }) })] }));
}
