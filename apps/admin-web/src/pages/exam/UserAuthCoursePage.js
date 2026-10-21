import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SafetyOutlined, SearchOutlined, MinusCircleOutlined, } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pageUserAuthCourses, createUserAuthCourse, updateUserAuthCourse, deleteUserAuthCourse, pageCourses, } from '../../api/exam';
const TYPE_OPTIONS = [
    { label: '命题科目', value: 0 },
    { label: '审题科目', value: 1 },
    { label: '终审科目', value: 2 },
];
const TYPE_MAP = {
    0: '命题',
    1: '审题',
    2: '终审',
};
export function UserAuthCoursePage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: '' });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingKey, setEditingKey] = useState(null);
    const [form] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['exam-user-auth-courses', query],
        queryFn: () => pageUserAuthCourses(query),
    });
    const coursesQuery = useQuery({
        queryKey: ['exam-courses-all'],
        queryFn: () => pageCourses({ page: 1, pageSize: 200 }),
        staleTime: 60000,
    });
    const createMutation = useMutation({
        mutationFn: createUserAuthCourse,
        onSuccess: () => {
            message.success('专家授权已创建');
            setModalOpen(false);
            setEditingKey(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ oldRealname, oldIdCard, payload, }) => updateUserAuthCourse(oldRealname, oldIdCard, payload),
        onSuccess: () => {
            message.success('专家授权已更新');
            setModalOpen(false);
            setEditingKey(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: ({ realname, idCard }) => deleteUserAuthCourse(realname, idCard),
        onSuccess: () => {
            message.success('专家授权已删除');
            queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] });
        },
    });
    const openCreate = useCallback(() => {
        setEditingKey(null);
        form.resetFields();
        form.setFieldsValue({ status: true, sortNumber: 1, items: [] });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback(async (record) => {
        setEditingKey({ realname: record.realname, idCard: record.idCard });
        form.setFieldsValue({
            realname: record.realname,
            idCard: record.idCard,
            status: record.status === 1,
            sortNumber: record.sortNumber,
            items: record.courses.map((c) => ({
                courseId: c.courseId,
                type: c.type,
            })),
        });
        setModalOpen(true);
    }, [form]);
    const handleSubmit = useCallback(() => {
        form.validateFields().then((values) => {
            const courseList = coursesQuery.data?.items || [];
            const items = (values.items || []).map((item) => {
                const course = courseList.find((c) => c.id === item.courseId);
                return {
                    courseId: item.courseId,
                    courseTitle: course?.title || '',
                    type: item.type ?? 0,
                };
            });
            const payload = {
                realname: values.realname,
                idCard: values.idCard || '',
                items,
                status: values.status ? 1 : 0,
                sortNumber: values.sortNumber ?? 1,
            };
            if (editingKey) {
                updateMutation.mutate({
                    oldRealname: editingKey.realname,
                    oldIdCard: editingKey.idCard,
                    payload,
                });
            }
            else {
                createMutation.mutate(payload);
            }
        });
    }, [form, editingKey, createMutation, updateMutation, coursesQuery.data]);
    const handleDelete = useCallback((record) => {
        Modal.confirm({
            title: `确认删除「${record.realname}」的授权？`,
            content: '将删除该专家的所有科目授权记录。',
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => deleteMutation.mutate({ realname: record.realname, idCard: record.idCard }),
        });
    }, [deleteMutation]);
    const courseOptions = (coursesQuery.data?.items || []).map((c) => ({
        label: c.title,
        value: c.id,
    }));
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const columns = [
        {
            title: '专家姓名',
            dataIndex: 'realname',
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        { title: '身份证号', dataIndex: 'idCard', width: 180 },
        {
            title: '授权科目',
            dataIndex: 'courses',
            width: 360,
            render: (courses) => {
                if (!courses || courses.length === 0)
                    return _jsx(Tag, { children: "\u65E0" });
                return (_jsx(Space, { size: 4, wrap: true, children: courses.map((c, i) => (_jsxs(Tag, { color: "blue", children: [c.courseTitle, "-", TYPE_MAP[c.type] || c.type] }, i))) }));
            },
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (v) => v === 1 ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }),
        },
        { title: '排序号', dataIndex: 'sortNumber', width: 80 },
        {
            title: '操作',
            width: 120,
            render: (_, record) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(SafetyOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u4E13\u5BB6\u6388\u6743\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7BA1\u7406\u4E13\u5BB6\u4E0E\u79D1\u76EE\u7684\u6388\u6743\u5173\u7CFB\uFF0C\u652F\u6301\u547D\u9898\u3001\u5BA1\u9898\u3001\u7EC8\u5BA1\u4E09\u79CD\u7C7B\u578B\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] });
                                }, children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u6388\u6743" })] })] }), _jsxs("div", { style: { padding: '0 24px' }, children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u4E13\u5BB6\u59D3\u540D\u3001\u8EAB\u4EFD\u8BC1\u53F7\u3001\u79D1\u76EE", allowClear: true, style: { width: 300 }, value: query.keyword, onChange: (e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] }) }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['exam-user-auth-courses'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: (r) => `${r.realname}::${r.idCard}`, size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] }), _jsx(Modal, { title: editingKey ? '编辑专家授权' : '新增专家授权', open: modalOpen, width: 640, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingKey(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsxs(Space, { size: 12, style: { width: '100%' }, children: [_jsx(Form.Item, { name: "realname", label: "\u4E13\u5BB6\u59D3\u540D", rules: [{ required: true, message: '请输入专家姓名' }], style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u4E13\u5BB6\u59D3\u540D", maxLength: 255 }) }), _jsx(Form.Item, { name: "idCard", label: "\u8EAB\u4EFD\u8BC1\u53F7", style: { flex: 1 }, children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8EAB\u4EFD\u8BC1\u53F7", maxLength: 50 }) })] }), _jsx(Form.List, { name: "items", children: (fields, { add, remove }) => (_jsxs(_Fragment, { children: [_jsxs("div", { style: { marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }, children: [_jsx(Typography.Text, { strong: true, children: "\u6388\u6743\u79D1\u76EE" }), _jsx(Button, { type: "dashed", size: "small", icon: _jsx(PlusOutlined, {}), onClick: () => add({ courseId: undefined, type: 0 }), children: "\u6DFB\u52A0\u79D1\u76EE" })] }), fields.map(({ key, name, ...rest }) => (_jsxs(Space, { size: 12, style: { display: 'flex', marginBottom: 8 }, align: "baseline", children: [_jsx(Form.Item, { ...rest, name: [name, 'courseId'], rules: [{ required: true, message: '请选择科目' }], style: { width: 280 }, children: _jsx(Select, { showSearch: true, placeholder: "\u9009\u62E9\u79D1\u76EE", options: courseOptions, filterOption: (input, option) => option?.label?.toLowerCase().includes(input.toLowerCase()) }) }), _jsx(Form.Item, { ...rest, name: [name, 'type'], rules: [{ required: true, message: '请选择类型' }], style: { width: 160 }, children: _jsx(Select, { placeholder: "\u7C7B\u578B", options: TYPE_OPTIONS }) }), _jsx(Button, { type: "text", danger: true, icon: _jsx(MinusCircleOutlined, {}), onClick: () => remove(name) })] }, key))), fields.length === 0 && (_jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u81F3\u5C11\u6DFB\u52A0\u4E00\u4E2A\u6388\u6743\u79D1\u76EE" }))] })) }), _jsx(Space, { size: 12, children: _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", valuePropName: "checked", children: _jsx(Switch, { checkedChildren: "\u542F\u7528", unCheckedChildren: "\u505C\u7528" }) }) }), _jsx(Form.Item, { name: "sortNumber", label: "\u6392\u5E8F\u53F7", children: _jsx(InputNumber, { min: 1, max: 9999, style: { width: '100%' } }) })] }) })] }));
}
