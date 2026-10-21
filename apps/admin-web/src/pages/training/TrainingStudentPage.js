import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message, } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
export function TrainingStudentPage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const [modalOpen, setModalOpen] = useState(false);
    const [editing, setEditing] = useState(null);
    const [phoneEditable, setPhoneEditable] = useState(false);
    const { data: publicData } = useQuery({
        queryKey: ['training-public-data', 'company'],
        queryFn: () => trainApi.getDemandPublicData(['company']),
    });
    const { data, isLoading, refetch } = useQuery({
        queryKey: ['platform-users', page, keyword],
        queryFn: () => trainApi.pagePlatformUsers({ page, pageSize: 20, keyword: keyword || undefined }),
    });
    const saveMu = useMutation({
        mutationFn: (payload) => trainApi.savePlatformUser(payload),
        onSuccess: () => {
            message.success('保存成功');
            setModalOpen(false);
            setEditing(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['platform-users'] });
        },
        onError: () => message.error('保存失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deletePlatformUser(id),
        onSuccess: () => {
            message.success('删除成功');
            queryClient.invalidateQueries({ queryKey: ['platform-users'] });
        },
    });
    const openCreate = () => {
        setEditing(null);
        form.resetFields();
        setPhoneEditable(false);
        setModalOpen(true);
    };
    const openEdit = (row) => {
        setEditing(row);
        setPhoneEditable(false);
        form.setFieldsValue({
            id: row.id,
            realname: row.realname,
            idcard: row.idcard,
            phone: row.phone,
            companyId: row.companyId,
        });
        setModalOpen(true);
    };
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 80 },
        { title: '身份证号', dataIndex: 'idcard', width: 180 },
        { title: '姓名', dataIndex: 'realname', width: 100 },
        { title: '手机号', dataIndex: 'phone', width: 120 },
        { title: '所属企业', dataIndex: 'companyName', ellipsis: true },
        { title: '创建时间', dataIndex: 'addtimeCn', width: 170 },
        {
            title: '操作', key: 'act', width: 140, fixed: 'right',
            render: (_, row) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(row), children: "\u4FEE\u6539" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664?", onConfirm: () => deleteMu.mutate(row.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    const companies = publicData?.company_list ?? [];
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u5B66\u5458\u7BA1\u7406" }), _jsx(Typography.Text, { type: "secondary", children: "\u5BF9\u63A5 platform_user / platform_user_detail\uFF08\u5BF9\u9F50 student.vue\uFF09" })] }) }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Space, { wrap: true, children: [_jsx(Input, { placeholder: "\u5173\u952E\u5B57\u641C\u7D22", prefix: _jsx(SearchOutlined, {}), value: keyword, onChange: (e) => setKeyword(e.target.value), onPressEnter: () => { setPage(1); refetch(); }, style: { width: 220 }, allowClear: true }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u6DFB\u52A0" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => refetch(), children: "\u5237\u65B0" })] }) }), _jsx(Card, { size: "small", children: _jsx(Table, { rowKey: "id", loading: isLoading, columns: columns, dataSource: data?.items ?? [], scroll: { x: 1000 }, pagination: {
                        current: page,
                        pageSize: 20,
                        total: data?.total ?? 0,
                        onChange: setPage,
                    } }) }), _jsx(Modal, { title: editing ? '修改学员' : '添加学员', open: modalOpen, onCancel: () => { setModalOpen(false); setEditing(null); setPhoneEditable(false); }, onOk: () => form.validateFields().then((v) => saveMu.mutate({ ...v, id: editing?.id })), confirmLoading: saveMu.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", autoComplete: "off", children: [_jsxs("div", { "aria-hidden": true, style: { position: 'absolute', width: 0, height: 0, overflow: 'hidden' }, children: [_jsx("input", { tabIndex: -1, autoComplete: "username", name: "username" }), _jsx("input", { tabIndex: -1, type: "password", autoComplete: "current-password", name: "current-password" })] }), _jsx(Form.Item, { name: "realname", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, { autoComplete: "off" }) }), _jsx(Form.Item, { name: "idcard", label: "\u8EAB\u4EFD\u8BC1\u53F7", rules: [{ required: true, len: 18, message: '请输入18位身份证号' }], children: _jsx(Input, { maxLength: 18, autoComplete: "off" }) }), _jsx(Form.Item, { name: "phone", label: "\u624B\u673A\u53F7", rules: [
                                { required: true, message: '请输入手机号' },
                                { pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号' },
                            ], children: _jsx(Input, { maxLength: 11, inputMode: "numeric", autoComplete: "off", readOnly: !phoneEditable, onFocus: () => {
                                    window.setTimeout(() => setPhoneEditable(true), 200);
                                } }) }), _jsx(Form.Item, { name: "password", label: "\u5BC6\u7801", extra: "\u7559\u7A7A\u5219\u4F7F\u7528\u9ED8\u8BA4 123123", children: _jsx(Input.Password, { autoComplete: "new-password" }) }), _jsx(Form.Item, { name: "companyId", label: "\u6240\u5C5E\u4F01\u4E1A", children: _jsx(Select, { allowClear: true, placeholder: "\u8BF7\u9009\u62E9", options: companies.map((c) => ({ value: c.id, label: c.title })) }) })] }) })] }));
}
