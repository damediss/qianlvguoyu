import { jsxs as _jsxs, jsx as _jsx, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, MinusCircleOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, Modal, Result, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { treeWorkTypes } from '../../api/exam';
import { createCompetitionReferee, deleteCompetitionReferee, getCompetitionRefereeForEdit, pageCompetitionReferees, updateCompetitionReferee, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
const LEVEL_OPTIONS = [
    { value: '5', label: '初级工' },
    { value: '4', label: '中级工' },
    { value: '3', label: '高级工' },
    { value: '2', label: '技师' },
    { value: '1', label: '高级技师' },
];
function idValue(value) {
    return value === undefined || value === null ? '' : String(value);
}
function errorText(error, fallback) {
    return error instanceof Error ? error.message : fallback;
}
function validIdCard(value) {
    const normalized = value.trim().toUpperCase();
    if (!/^\d{17}[0-9X]$/.test(normalized))
        return false;
    const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
    const codes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'];
    const sum = weights.reduce((total, weight, index) => total + Number(normalized[index]) * weight, 0);
    return codes[sum % 11] === normalized[17];
}
function flattenWorkTypes(nodes) {
    return nodes.flatMap((node) => node.children?.length ? flattenWorkTypes(node.children) : [node]);
}
export function CompetitionRefereeManagementPage() {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:referee:view');
    const canCreate = permissions.includes('competition:referee:create');
    const canUpdate = permissions.includes('competition:referee:update');
    const canDelete = permissions.includes('competition:referee:delete');
    const queryClient = useQueryClient();
    const [workTypeId, setWorkTypeId] = useState();
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form] = Form.useForm();
    const workTypesQuery = useQuery({
        queryKey: ['exam-work-types', 'competition-referee-management'],
        queryFn: treeWorkTypes,
        enabled: canView,
    });
    const refereesQuery = useQuery({
        queryKey: ['competition-referees', workTypeId, keyword, page, pageSize],
        queryFn: () => pageCompetitionReferees({
            page,
            pageSize,
            workTypeId,
            keyword: keyword || undefined,
        }),
        enabled: canView,
    });
    const editQuery = useQuery({
        queryKey: ['competition-referee', editingId],
        queryFn: () => getCompetitionRefereeForEdit(editingId),
        enabled: modalOpen && editingId !== null,
    });
    const workTypes = useMemo(() => flattenWorkTypes(workTypesQuery.data ?? []), [workTypesQuery.data]);
    const workTypeOptions = useMemo(() => {
        // The referee API serializes Long IDs as strings while the exam tree
        // returns Integer IDs as numbers. AntD Select compares values strictly.
        const options = new Map();
        workTypes.forEach((item) => {
            const value = idValue(item.id);
            if (value)
                options.set(value, { label: item.title || value, value });
        });
        // Keep the edit form readable even if the work type tree arrives later
        // than the referee detail, or a historical work type is no longer in it.
        (editQuery.data?.qualifications ?? []).forEach((item) => {
            const value = idValue(item.workTypeId);
            if (value && item.workTypeName && !options.has(value)) {
                options.set(value, { label: item.workTypeName, value });
            }
        });
        return Array.from(options.values());
    }, [editQuery.data?.qualifications, workTypes]);
    useEffect(() => {
        if (!editQuery.data || !modalOpen || editingId === null)
            return;
        form.setFieldsValue({
            qualifications: (editQuery.data.qualifications ?? []).map((item) => ({
                // Keep the edit value type consistent with the normalized options.
                workTypeId: idValue(item.workTypeId),
                levelFromCode: item.levelFromCode || item.levelCode || undefined,
                levelToCode: item.levelToCode || item.levelCode || undefined,
            })),
            name: editQuery.data.name,
            idCardNo: editQuery.data.idCardNo,
            mobile: editQuery.data.mobile,
        });
    }, [editQuery.data, editingId, form, modalOpen]);
    const createMutation = useMutation({
        mutationFn: (payload) => createCompetitionReferee(payload),
        onSuccess: async () => {
            message.success('裁判已新增，账号已按手机号自动创建');
            setModalOpen(false);
            await queryClient.invalidateQueries({ queryKey: ['competition-referees'] });
        },
        onError: (error) => message.error(errorText(error, '裁判新增失败，请刷新后重试')),
    });
    const updateMutation = useMutation({
        mutationFn: (payload) => updateCompetitionReferee(editingId, payload),
        onSuccess: async () => {
            message.success('裁判信息已更新');
            setModalOpen(false);
            await queryClient.invalidateQueries({ queryKey: ['competition-referees'] });
        },
        onError: (error) => message.error(errorText(error, '裁判更新失败，请刷新后重试')),
    });
    const deleteMutation = useMutation({
        mutationFn: (profileId) => deleteCompetitionReferee(profileId),
        onSuccess: async () => {
            message.success('裁判已删除');
            await queryClient.invalidateQueries({ queryKey: ['competition-referees'] });
        },
        onError: (error) => message.error(errorText(error, '裁判删除失败，请刷新后重试')),
    });
    const openCreate = () => {
        setEditingId(null);
        form.resetFields();
        form.setFieldsValue({ qualifications: [{}] });
        setModalOpen(true);
    };
    const openEdit = (referee) => {
        setEditingId(referee.id);
        form.resetFields();
        setModalOpen(true);
    };
    const closeModal = () => {
        if (createMutation.isPending || updateMutation.isPending)
            return;
        setModalOpen(false);
        setEditingId(null);
        form.resetFields();
    };
    const submit = async () => {
        const values = await form.validateFields();
        if (editingId === null)
            createMutation.mutate(values);
        else
            updateMutation.mutate(values);
    };
    const columns = useMemo(() => [
        { title: '姓名', dataIndex: 'name', width: 110, fixed: 'left' },
        { title: '身份证号', dataIndex: 'idCardNo', width: 180 },
        { title: '手机号', dataIndex: 'mobile', width: 135 },
        {
            title: '相关工种及等级',
            width: 360,
            render: (_, row) => (_jsx(Space, { wrap: true, size: [4, 4], children: (row.qualifications ?? []).map((item) => (_jsxs(Tag, { children: [item.workTypeName || idValue(item.workTypeId), " \u00B7 ", item.levelRange
                            || `${item.levelFromName || item.levelFromCode}-${item.levelToName || item.levelToCode}`] }, `${idValue(item.workTypeId)}-${item.levelFromCode}-${item.levelToCode}`))) })),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (value) => _jsx(Tag, { color: value === 'active' ? 'success' : 'default', children: value === 'active' ? '启用' : value || '-' }),
        },
        {
            title: '操作',
            key: 'actions',
            width: 150,
            fixed: 'right',
            render: (_, row) => (_jsxs(Space, { children: [canUpdate && _jsx(Button, { type: "link", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(row), children: "\u7F16\u8F91" }), row.accountUsername && _jsx(Tag, { color: "blue", children: row.accountUsername }), canDelete && _jsx(Button, { type: "link", danger: true, icon: _jsx(DeleteOutlined, {}), loading: deleteMutation.isPending && deleteMutation.variables === row.id, onClick: () => Modal.confirm({
                            title: '确认删除该裁判？',
                            content: '已绑定选手的裁判不能删除，请先解除所有赛段绑定。',
                            okText: '删除',
                            cancelText: '取消',
                            onOk: () => deleteMutation.mutateAsync(row.id),
                        }), children: "\u5220\u9664" })] })),
        },
    ], [canDelete, canUpdate, deleteMutation]);
    if (!canView) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u88C1\u5224\u7BA1\u7406\u67E5\u770B\u6743\u9650", subTitle: "\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u7533\u8BF7\u7ADE\u8D5B\u88C1\u5224\u7BA1\u7406\u6743\u9650\u3002" });
    }
    return (_jsxs("div", { "data-testid": "competition-referee-management-page", children: [_jsxs(Card, { bordered: false, className: "competition-filter-card", children: [_jsxs("div", { className: "competition-filter-card__head", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u88C1\u5224\u4FE1\u606F" }), _jsx(Typography.Text, { type: "secondary", children: "\u88C1\u5224\u6863\u6848\u6309\u79DF\u6237\u7EDF\u4E00\u7EF4\u62A4\uFF0C\u53EF\u53C2\u4E0E\u591A\u573A\u8D5B\u4E8B\u548C\u591A\u4E2A\u5DE5\u79CD\uFF1B\u7B49\u7EA7\u6309\u8303\u56F4\u586B\u5199\uFF0C\u4F8B\u5982\u201C\u521D\u7EA7\u5DE5-\u4E2D\u7EA7\u5DE5\u201D\u6216\u201C\u521D\u7EA7\u5DE5-\u9AD8\u7EA7\u5DE5\u201D\u3002\u65B0\u589E\u88C1\u5224\u540E\u81EA\u52A8\u521B\u5EFA\u624B\u673A\u53F7\u767B\u5F55\u8D26\u53F7\uFF0C\u521D\u59CB\u5BC6\u7801\u4E3A 12345678\u3002\u5206\u7EC4\u8BF7\u5230\u300C\u88C1\u5224\u5C0F\u7EC4\u300D\u3002" })] }), canCreate && _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u88C1\u5224" })] }), _jsxs(Space, { wrap: true, className: "competition-filter-card__body", children: [_jsx(Select, { allowClear: true, placeholder: "\u6309\u5DE5\u79CD\u7B5B\u9009", style: { minWidth: 220 }, value: workTypeId, loading: workTypesQuery.isLoading, options: workTypeOptions, onChange: (value) => {
                                    setWorkTypeId(value);
                                    setPage(1);
                                } }), _jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u59D3\u540D\u3001\u8EAB\u4EFD\u8BC1\u53F7\u6216\u624B\u673A\u53F7", style: { width: 260 }, onSearch: (value) => {
                                    setKeyword(value.trim());
                                    setPage(1);
                                }, onChange: (event) => {
                                    if (!event.target.value) {
                                        setKeyword('');
                                        setPage(1);
                                    }
                                } }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => void refereesQuery.refetch(), children: "\u5237\u65B0" })] })] }), _jsx(Card, { size: "small", style: { marginTop: 16 }, children: _jsx(Table, { rowKey: (row) => idValue(row.id), scroll: { x: 1100 }, loading: refereesQuery.isLoading || refereesQuery.isFetching, dataSource: refereesQuery.data?.items ?? [], columns: columns, pagination: {
                        current: refereesQuery.data?.page ?? page,
                        pageSize: refereesQuery.data?.pageSize ?? pageSize,
                        total: refereesQuery.data?.total ?? 0,
                        showSizeChanger: true,
                        onChange: (nextPage, nextPageSize) => {
                            setPage(nextPage);
                            setPageSize(nextPageSize);
                        },
                    }, locale: { emptyText: '暂无裁判信息' } }) }), _jsx(Modal, { title: editingId === null ? '新增裁判' : '编辑裁判', open: modalOpen, onCancel: closeModal, onOk: () => void submit(), confirmLoading: createMutation.isPending || updateMutation.isPending || editQuery.isLoading, destroyOnClose: true, width: 720, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { name: "name", label: "\u59D3\u540D", rules: [{ required: true, whitespace: true, message: '请输入姓名' }], children: _jsx(Input, { maxLength: 128 }) }), _jsx(Form.Item, { name: "idCardNo", label: "\u8EAB\u4EFD\u8BC1\u53F7", rules: [
                                { required: true, whitespace: true, message: '请输入身份证号' },
                                { validator: (_, value) => !value || validIdCard(value) ? Promise.resolve() : Promise.reject(new Error('身份证号格式或校验码不正确')) },
                            ], children: _jsx(Input, { maxLength: 18 }) }), _jsx(Form.Item, { name: "mobile", label: "\u624B\u673A\u53F7", rules: [
                                { required: true, whitespace: true, message: '请输入手机号' },
                                { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号' },
                            ], children: _jsx(Input, { maxLength: 11 }) }), _jsx(Form.List, { name: "qualifications", rules: [{
                                    validator: async (_, values) => {
                                        if (!values || values.length === 0)
                                            throw new Error('至少添加一项工种等级资质');
                                    },
                                }], children: (fields, { add, remove }, { errors }) => (_jsxs(_Fragment, { children: [_jsxs("div", { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, children: [_jsx(Typography.Text, { strong: true, children: "\u76F8\u5173\u5DE5\u79CD\u4E0E\u5DE5\u79CD\u7B49\u7EA7" }), _jsx(Button, { type: "dashed", icon: _jsx(PlusOutlined, {}), onClick: () => add(), children: "\u6DFB\u52A0\u8D44\u8D28" })] }), fields.map((field) => {
                                        const selectedWorkTypeId = form.getFieldValue(['qualifications', field.name, 'workTypeId']);
                                        return (_jsxs(Space, { align: "baseline", style: { display: 'flex', marginTop: 12 }, children: [_jsx(Form.Item, { ...field, name: [field.name, 'workTypeId'], rules: [{ required: true, message: '请选择工种' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: 330 }, placeholder: "\u9009\u62E9\u5DE5\u79CD", options: workTypeOptions, onChange: () => {
                                                            form.setFieldValue(['qualifications', field.name, 'levelFromCode'], undefined);
                                                            form.setFieldValue(['qualifications', field.name, 'levelToCode'], undefined);
                                                        } }) }), _jsx(Form.Item, { ...field, name: [field.name, 'levelFromCode'], rules: [{ required: true, message: '请选择起始等级' }], children: _jsx(Select, { style: { width: 135 }, placeholder: "\u8D77\u59CB\u7B49\u7EA7", options: LEVEL_OPTIONS.filter((item) => {
                                                            const workType = workTypes.find((candidate) => idValue(candidate.id) === idValue(selectedWorkTypeId));
                                                            const levelsNo = workType?.levelsNo || '';
                                                            return !levelsNo || levelsNo.includes(item.value);
                                                        }) }) }), _jsx(Form.Item, { ...field, name: [field.name, 'levelToCode'], rules: [{ required: true, message: '请选择结束等级' }], children: _jsx(Select, { style: { width: 135 }, placeholder: "\u7ED3\u675F\u7B49\u7EA7", options: LEVEL_OPTIONS.filter((item) => {
                                                            const workType = workTypes.find((candidate) => idValue(candidate.id) === idValue(selectedWorkTypeId));
                                                            const levelsNo = workType?.levelsNo || '';
                                                            return !levelsNo || levelsNo.includes(item.value);
                                                        }) }) }), _jsx(Button, { type: "text", danger: true, icon: _jsx(MinusCircleOutlined, {}), disabled: fields.length <= 1, onClick: () => remove(field.name) })] }, field.key));
                                    }), _jsx(Form.ErrorList, { errors: errors })] })) })] }) })] }));
}
