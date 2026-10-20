import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, ReadOutlined, } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, TreeSelect, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pageCourses, treeWorkTypes, createCourse, updateCourse, deleteCourse, } from '../../api/exam';
const LEVEL_OPTIONS = [
    { label: '初级', value: 0 },
    { label: '中级', value: 1 },
    { label: '高级', value: 2 },
    { label: '技师', value: 3 },
    { label: '高级技师', value: 4 },
];
const LEVEL_MAP = {
    0: { label: '初级', color: 'green' },
    1: { label: '中级', color: 'blue' },
    2: { label: '高级', color: 'orange' },
    3: { label: '技师', color: 'purple' },
    4: { label: '高级技师', color: 'red' },
};
function buildTreeSelectData(nodes) {
    return nodes.map((node) => ({
        title: node.title,
        value: node.id,
        children: node.children ? buildTreeSelectData(node.children) : undefined,
    }));
}
export function CoursePage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, title: '' });
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['exam-courses', query],
        queryFn: () => pageCourses(query),
    });
    const treeQuery = useQuery({
        queryKey: ['exam-work-types-tree'],
        queryFn: treeWorkTypes,
    });
    const createMutation = useMutation({
        mutationFn: createCourse,
        onSuccess: () => {
            message.success('科目已创建');
            setModalOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['exam-courses'] });
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateCourse(id, payload),
        onSuccess: () => {
            message.success('科目已更新');
            setModalOpen(false);
            setEditingId(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['exam-courses'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deleteCourse,
        onSuccess: () => {
            message.success('科目已删除');
            queryClient.invalidateQueries({ queryKey: ['exam-courses'] });
        },
    });
    const openCreate = useCallback(() => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ status: true, sortNumber: 1, isGeneral: false });
        setModalOpen(true);
    }, [form]);
    const openEdit = useCallback((record) => {
        setEditingId(record.id);
        form.setFieldsValue({
            title: record.title,
            workTypeId: record.workTypeId || undefined,
            level: record.level,
            seriesNoGroup: record.seriesNoGroup || '',
            seriesNo: record.seriesNo || '',
            userInChargeName: record.userInChargeName || '',
            lastLevelGetYear: record.lastLevelGetYear,
            workYearLimit: record.workYearLimit,
            minLevel: record.minLevel,
            minLevelYear: record.minLevelYear,
            startAge: record.startAge,
            isGeneral: record.isGeneral === 1,
            examCondition: record.examCondition || '',
            comments: record.comments || '',
            status: record.status === 1,
            sortNumber: record.sortNumber,
        });
        setModalOpen(true);
    }, [form]);
    const handleSubmit = useCallback(() => {
        form.validateFields().then((values) => {
            const payload = {
                title: values.title,
                workTypeId: values.workTypeId || undefined,
                level: values.level ?? 0,
                seriesNoGroup: values.seriesNoGroup || undefined,
                seriesNo: values.seriesNo || undefined,
                userInChargeName: values.userInChargeName || undefined,
                lastLevelGetYear: values.lastLevelGetYear ?? 0,
                workYearLimit: values.workYearLimit,
                minLevel: values.minLevel,
                minLevelYear: values.minLevelYear,
                startAge: values.startAge ?? 0,
                isGeneral: values.isGeneral ? 1 : 0,
                examCondition: values.examCondition || undefined,
                comments: values.comments || undefined,
                status: values.status ? 1 : 0,
                sortNumber: values.sortNumber ?? 1,
            };
            if (editingId) {
                updateMutation.mutate({ id: editingId, payload });
            }
            else {
                createMutation.mutate(payload);
            }
        });
    }, [form, editingId, createMutation, updateMutation]);
    const handleDelete = useCallback((record) => {
        Modal.confirm({
            title: `确认删除「${record.title}」？`,
            content: '删除后可在数据库中恢复（逻辑删除）。',
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => deleteMutation.mutate(record.id),
        });
    }, [deleteMutation]);
    const treeData = treeQuery.data || [];
    const workTypeTreeData = buildTreeSelectData(treeData);
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const columns = [
        {
            title: '科目名称',
            dataIndex: 'title',
            render: (text) => (_jsx(Typography.Text, { strong: true, children: text })),
        },
        { title: '科目编码', dataIndex: 'seriesNo', width: 140 },
        {
            title: '级别',
            dataIndex: 'level',
            width: 100,
            render: (v) => {
                const info = LEVEL_MAP[v];
                return info ? _jsx(Tag, { color: info.color, children: info.label }) : _jsx(Tag, { children: v });
            },
        },
        { title: '拥有者', dataIndex: 'userInChargeName', width: 120 },
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
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(ReadOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u79D1\u76EE\u7BA1\u7406" })] }), _jsx(Typography.Text, { children: "\u7EF4\u62A4\u79D1\u76EE\u4FE1\u606F\uFF0C\u652F\u6301\u6309\u5DE5\u79CD\u5173\u8054\u3001\u7B49\u7EA7\u5206\u7C7B\u3001\u7F16\u7801\u548C\u6761\u4EF6\u7BA1\u7406\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    queryClient.invalidateQueries({ queryKey: ['exam-courses'] });
                                }, children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u79D1\u76EE" })] })] }), _jsxs("div", { style: { padding: '0 24px' }, children: [_jsxs(Space, { style: { marginBottom: 16 }, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u79D1\u76EE\u540D\u79F0\u3001\u7F16\u7801\u3001\u62E5\u6709\u8005", allowClear: true, style: { width: 300 }, value: query.title, onChange: (e) => setQuery((prev) => ({ ...prev, title: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ['exam-courses'] }) }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['exam-courses'] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] }), _jsx(Modal, { title: editingId ? '编辑科目' : '新增科目', open: modalOpen, width: 640, onOk: handleSubmit, onCancel: () => {
                    setModalOpen(false);
                    setEditingId(null);
                    form.resetFields();
                }, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", style: { marginTop: 16 }, children: [_jsx(Form.Item, { name: "title", label: "\u79D1\u76EE\u540D\u79F0", rules: [{ required: true, message: '请输入科目名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u79D1\u76EE\u540D\u79F0", maxLength: 255 }) }), _jsx(Form.Item, { name: "workTypeId", label: "\u5173\u8054\u5DE5\u79CD", children: _jsx(TreeSelect, { allowClear: true, showSearch: true, treeNodeFilterProp: "title", placeholder: "\u4E0D\u9009\u5219\u4E3A\u72EC\u7ACB\u79D1\u76EE", treeData: workTypeTreeData, treeDefaultExpandAll: true }) }), _jsx(Form.Item, { name: "level", label: "\u7EA7\u522B", children: _jsx(Select, { allowClear: true, placeholder: "\u8BF7\u9009\u62E9\u7EA7\u522B", options: LEVEL_OPTIONS }) }), _jsx(Form.Item, { name: "seriesNoGroup", label: "\u79D1\u76EE\u7F16\u7801(\u7F16\u7EC4)", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u79D1\u76EE\u7F16\u7801\u7F16\u7EC4", maxLength: 200 }) }), _jsx(Form.Item, { name: "seriesNo", label: "\u79D1\u76EE\u7F16\u7801", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u79D1\u76EE\u7F16\u7801", maxLength: 200 }) }), _jsx(Form.Item, { name: "userInChargeName", label: "\u62E5\u6709\u8005\u540D\u79F0", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u62E5\u6709\u8005\u540D\u79F0", maxLength: 255 }) }), _jsxs(Space, { size: 12, children: [_jsx(Form.Item, { name: "workYearLimit", label: "\u5DE5\u4F5C\u5E74\u9650\u8981\u6C42", children: _jsx(InputNumber, { min: 0, max: 99, style: { width: 160 } }) }), _jsx(Form.Item, { name: "startAge", label: "\u5F00\u59CB\u5E74\u9F84", children: _jsx(InputNumber, { min: 0, max: 99, style: { width: 160 } }) }), _jsx(Form.Item, { name: "lastLevelGetYear", label: "\u4E0A\u7EA7\u83B7\u5F97\u5E74\u4EFD", children: _jsx(InputNumber, { min: 0, max: 99, style: { width: 160 } }) })] }), _jsxs(Space, { size: 12, children: [_jsx(Form.Item, { name: "minLevel", label: "\u6700\u4F4E\u8BC1\u4E66\u8981\u6C42", children: _jsx(InputNumber, { min: 0, max: 4, style: { width: 160 } }) }), _jsx(Form.Item, { name: "minLevelYear", label: "\u6700\u4F4E\u8BC1\u4E66\u83B7\u5F97\u5E74", children: _jsx(InputNumber, { min: 0, max: 99, style: { width: 160 } }) })] }), _jsx(Form.Item, { name: "examCondition", label: "\u8003\u8BD5\u6761\u4EF6", children: _jsx(Input.TextArea, { placeholder: "\u8BF7\u8F93\u5165\u8003\u8BD5\u6761\u4EF6", rows: 3 }) }), _jsx(Form.Item, { name: "comments", label: "\u63CF\u8FF0", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u63CF\u8FF0", maxLength: 255 }) }), _jsxs(Space, { size: 12, children: [_jsx(Form.Item, { name: "status", label: "\u72B6\u6001", valuePropName: "checked", children: _jsx(Switch, { checkedChildren: "\u542F\u7528", unCheckedChildren: "\u505C\u7528" }) }), _jsx(Form.Item, { name: "isGeneral", label: "\u901A\u7528\u8D44\u6E90", valuePropName: "checked", children: _jsx(Switch, { checkedChildren: "\u662F", unCheckedChildren: "\u5426" }) })] }), _jsx(Form.Item, { name: "sortNumber", label: "\u6392\u5E8F\u53F7", children: _jsx(InputNumber, { min: 1, max: 9999, style: { width: '100%' } }) })] }) })] }));
}
