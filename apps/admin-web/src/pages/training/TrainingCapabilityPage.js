import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Button, Card, Col, Form, Input, InputNumber, Modal, Popconfirm, Row, Space, Table, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
export function TrainingCapabilityPage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [searchForm] = Form.useForm();
    const [query, setQuery] = useState({ page: 1, pageSize: 20 });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const { data, isLoading } = useQuery({
        queryKey: ['training-user-abilities', query],
        queryFn: () => trainApi.pageUserAbilities(query),
    });
    const createMu = useMutation({
        mutationFn: (p) => trainApi.createUserAbility(p),
        onSuccess: () => { message.success('创建成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-user-abilities'] }); },
        onError: () => message.error('创建失败'),
    });
    const updateMu = useMutation({
        mutationFn: ({ id, p }) => trainApi.updateUserAbility(id, p),
        onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-user-abilities'] }); },
        onError: () => message.error('更新失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteUserAbility(id),
        onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-user-abilities'] }); },
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
        { title: '用户姓名', dataIndex: 'userName', width: 120 },
        { title: '技能名称', dataIndex: 'skillName', width: 150, ellipsis: true },
        { title: '技能等级', dataIndex: 'skillLevel', width: 100 },
        { title: '证书编号', dataIndex: 'certificateNo', width: 150 },
        { title: '颁发机构', dataIndex: 'issuingAuthority', width: 150 },
        { title: '有效期', dataIndex: 'expireDate', width: 120 },
        {
            title: '操作', key: 'act', width: 120, fixed: 'right',
            render: (_, r) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(r), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664?", onConfirm: () => deleteMu.mutate(r.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u80FD\u529B\u77E9\u9635" }), _jsx(Typography.Text, { type: "secondary", children: "\u7BA1\u7406\u5B66\u5458\u6280\u80FD\u80FD\u529B\u4FE1\u606F\uFF0C\u652F\u6301\u6280\u80FD\u7B49\u7EA7\u3001\u8BC1\u4E66\u7B49\u6570\u636E\u7EF4\u62A4" })] }) }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Form, { form: searchForm, layout: "inline", children: [_jsx(Form.Item, { name: "keyword", children: _jsx(Input, { placeholder: "\u641C\u7D22\u7528\u6237\u59D3\u540D\u3001\u6280\u80FD...", prefix: _jsx(SearchOutlined, {}), allowClear: true, onPressEnter: handleSearch, style: { width: 240 } }) }), _jsx(Form.Item, { children: _jsx(Button, { type: "primary", onClick: handleSearch, children: "\u67E5\u8BE2" }) })] }) }), _jsxs(Card, { size: "small", children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['training-user-abilities'] }), children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u80FD\u529B" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: isLoading, columns: columns, dataSource: data?.items ?? [], scroll: { x: 900 }, pagination: {
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
                        } })] }), _jsx(Modal, { title: editingId ? '编辑能力' : '新增能力', open: modalOpen, onOk: handleSubmit, onCancel: closeModal, confirmLoading: createMu.isPending || updateMu.isPending, destroyOnHidden: true, width: 600, children: _jsx(Form, { form: form, layout: "vertical", children: _jsxs(Row, { gutter: 24, children: [_jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "userName", label: "\u7528\u6237\u59D3\u540D", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "skillName", label: "\u6280\u80FD\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "skillLevel", label: "\u6280\u80FD\u7B49\u7EA7", children: _jsx(InputNumber, { min: 1, max: 10, style: { width: '100%' }, placeholder: "1-10" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "certificateNo", label: "\u8BC1\u4E66\u7F16\u53F7", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "issuingAuthority", label: "\u9881\u53D1\u673A\u6784", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "expireDate", label: "\u6709\u6548\u671F", children: _jsx(Input, { placeholder: "YYYY-MM-DD" }) }) })] }) }) })] }));
}
