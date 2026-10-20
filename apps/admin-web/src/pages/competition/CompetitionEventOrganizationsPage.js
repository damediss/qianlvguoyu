import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { PlusOutlined, ReloadOutlined, SearchOutlined, } from '@ant-design/icons';
import { Button, Card, Form, Input, InputNumber, Modal, Result, Select, Space, Spin, Table, Tag, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { addCompetitionEventOrganization, getCompetitionEvent, pageCompetitionEventOrganizations, updateCompetitionEventOrganizationImportPermission, updateCompetitionEventOrganizationStatus, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';
const statusOptions = [
    { label: '已生效', value: 'active' },
    { label: '已暂停', value: 'suspended' },
];
const importPermissionOptions = [
    { label: '拒绝', value: 0 },
    { label: '允许', value: 1 },
];
const statusMeta = {
    invited: { label: '已邀请', color: 'default' },
    active: { label: '已生效', color: 'success' },
    suspended: { label: '已暂停', color: 'warning' },
    closed: { label: '已关闭', color: 'error' },
};
function idValue(id) {
    return String(id);
}
function statusTag(status) {
    const meta = status ? statusMeta[status] : undefined;
    return _jsx(Tag, { color: meta?.color, children: meta?.label || status || '-' });
}
function editableStatusValue(status) {
    return status === 'suspended' ? 'suspended' : 'active';
}
export function CompetitionEventOrganizationsPage({ eventId, onNavigate, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:event:view')
        && permissions.includes('competition:event-org:view');
    const canManage = permissions.includes('competition:event-org:manage');
    const [query, setQuery] = useState({ page: 1, pageSize: 10 });
    const [addOpen, setAddOpen] = useState(false);
    const [updatingStatusOrgId, setUpdatingStatusOrgId] = useState();
    const [updatingPermissionOrgId, setUpdatingPermissionOrgId] = useState();
    const [addForm] = Form.useForm();
    const queryClient = useQueryClient();
    const eventQuery = useQuery({
        queryKey: ['competition-event', eventId],
        queryFn: () => getCompetitionEvent(eventId),
        enabled: canView,
    });
    const organizationsQuery = useQuery({
        queryKey: ['competition-event-organizations', eventId, query],
        queryFn: () => pageCompetitionEventOrganizations(eventId, query),
        enabled: canView,
    });
    const addMutation = useMutation({
        mutationFn: (payload) => addCompetitionEventOrganization(eventId, payload),
        onSuccess: () => {
            message.success('参与企业添加成功');
            setAddOpen(false);
            addForm.resetFields();
            queryClient.invalidateQueries({ queryKey: ['competition-event-organizations', eventId] });
        },
    });
    const statusMutation = useMutation({
        mutationFn: ({ orgId, payload }) => updateCompetitionEventOrganizationStatus(eventId, orgId, payload),
        onMutate: ({ orgId }) => setUpdatingStatusOrgId(orgId),
        onSuccess: () => {
            message.success('参与状态已更新');
            queryClient.invalidateQueries({ queryKey: ['competition-event-organizations', eventId] });
        },
        onSettled: () => setUpdatingStatusOrgId(undefined),
    });
    const importPermissionMutation = useMutation({
        mutationFn: ({ orgId, payload }) => updateCompetitionEventOrganizationImportPermission(eventId, orgId, payload),
        onMutate: ({ orgId }) => setUpdatingPermissionOrgId(orgId),
        onSuccess: () => {
            message.success('名单导入权限已更新');
            queryClient.invalidateQueries({ queryKey: ['competition-event-organizations', eventId] });
        },
        onSettled: () => setUpdatingPermissionOrgId(undefined),
    });
    const columns = useMemo(() => [
        {
            title: '企业名称',
            dataIndex: 'orgName',
            render: (value, record) => value || idValue(record.orgId),
        },
        {
            title: '企业编码',
            key: 'orgCode',
            width: 180,
            render: (_, record) => record.orgCode || idValue(record.orgId),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 140,
            render: (value, record) => canManage ? (_jsx(Select, { size: "small", value: editableStatusValue(value), options: statusOptions, loading: updatingStatusOrgId === record.orgId, onChange: (status) => statusMutation.mutate({
                    orgId: record.orgId,
                    payload: { status },
                }), style: { width: 120 } })) : statusTag(value),
        },
        {
            title: '参赛名额',
            dataIndex: 'quota',
            width: 100,
            render: (value) => value ?? '-',
        },
        {
            title: '名单导入权限',
            dataIndex: 'importPermission',
            width: 150,
            render: (value, record) => canManage ? (_jsx(Select, { size: "small", value: value === 1 ? 1 : 0, options: importPermissionOptions, loading: updatingPermissionOrgId === record.orgId, onChange: (importPermission) => importPermissionMutation.mutate({
                    orgId: record.orgId,
                    payload: { importPermission },
                }), style: { width: 100 } })) : (value === 1 ? '允许' : '拒绝'),
        },
    ], [canManage, importPermissionMutation, statusMutation, updatingPermissionOrgId, updatingStatusOrgId]);
    if (!canView) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u8D5B\u4E8B\u67E5\u770B\u6743\u9650", subTitle: "\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u7533\u8BF7 competition:event:view \u6743\u9650\u3002" });
    }
    if (eventQuery.isLoading || organizationsQuery.isLoading) {
        return _jsx(Spin, { className: "competition-loading" });
    }
    if (eventQuery.isError || !eventQuery.data) {
        return _jsx(Result, { status: "404", title: "\u8D5B\u4E8B\u4E0D\u5B58\u5728\u6216\u65E0\u6CD5\u8BBF\u95EE", extra: _jsx(Button, { onClick: () => onNavigate('/competition/events'), children: "\u8FD4\u56DE\u8D5B\u4E8B\u5217\u8868" }) });
    }
    if (organizationsQuery.isError) {
        return _jsx(Result, { status: "error", title: "\u53C2\u4E0E\u4F01\u4E1A\u52A0\u8F7D\u5931\u8D25", extra: _jsx(Button, { onClick: () => organizationsQuery.refetch(), children: "\u91CD\u65B0\u52A0\u8F7D" }) });
    }
    const event = eventQuery.data;
    const canAdd = canManage && event.scope === 'platform';
    const organizations = organizationsQuery.data?.items || [];
    const handleAdd = (values) => {
        if (!values.orgName?.trim()) {
            message.error('请输入企业名称');
            return;
        }
        addMutation.mutate({
            orgName: values.orgName.trim(),
            quota: values.quota,
            importPermission: values.importPermission ?? 1,
        });
    };
    return (_jsxs(CompetitionEventFrame, { event: event, activeSection: "organizations", onNavigate: onNavigate, children: [_jsx(Card, { bordered: false, className: "competition-filter-card", children: _jsxs(Space, { style: { width: '100%', justifyContent: 'space-between' }, wrap: true, children: [_jsxs(Space, { wrap: true, children: [_jsx(Select, { allowClear: true, placeholder: "\u6309\u72B6\u6001\u7B5B\u9009", prefix: _jsx(SearchOutlined, {}), options: statusOptions, value: query.status === 'active' || query.status === 'suspended' ? query.status : undefined, onChange: (status) => setQuery((current) => ({ ...current, page: 1, status })), style: { width: 190 } }), _jsx(Button, { type: "primary", ghost: true, icon: _jsx(SearchOutlined, {}), onClick: () => organizationsQuery.refetch(), children: "\u67E5\u8BE2" })] }), _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => organizationsQuery.refetch(), children: "\u5237\u65B0" }), canAdd && (_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => setAddOpen(true), children: "\u6DFB\u52A0\u53C2\u4E0E\u4F01\u4E1A" }))] })] }) }), _jsx(Card, { bordered: false, className: "competition-table-card", children: _jsx(Table, { rowKey: (record) => idValue(record.id), loading: organizationsQuery.isFetching, dataSource: organizations, columns: columns, scroll: { x: 900 }, pagination: {
                        current: query.page,
                        pageSize: query.pageSize,
                        total: organizationsQuery.data?.total || 0,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 家企业`,
                        onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })),
                    } }) }), _jsx(Modal, { title: "\u6DFB\u52A0\u53C2\u4E0E\u4F01\u4E1A", open: addOpen, destroyOnClose: true, confirmLoading: addMutation.isPending, onCancel: () => { setAddOpen(false); addForm.resetFields(); }, onOk: () => addForm.submit(), children: _jsxs(Form, { form: addForm, layout: "vertical", initialValues: { importPermission: 1 }, onFinish: handleAdd, requiredMark: "optional", children: [_jsx(Form.Item, { name: "orgName", label: "\u4F01\u4E1A\u540D\u79F0", rules: [{ required: true, whitespace: true, max: 128, message: '请输入企业名称' }], children: _jsx(Input, { showCount: true, maxLength: 128, placeholder: "\u8BF7\u8F93\u5165\u53C2\u4E0E\u4F01\u4E1A\u540D\u79F0" }) }), _jsx(Form.Item, { name: "quota", label: "\u53C2\u8D5B\u540D\u989D", rules: [{ type: 'number', min: 0, message: '参赛名额不能小于 0' }], children: _jsx(InputNumber, { min: 0, precision: 0, style: { width: '100%' }, placeholder: "\u4E0D\u9650\u5236\u53EF\u7559\u7A7A" }) }), _jsx(Form.Item, { name: "importPermission", label: "\u540D\u5355\u5BFC\u5165\u6743\u9650", rules: [{ required: true, message: '请选择名单导入权限' }], children: _jsx(Select, { options: importPermissionOptions }) })] }) })] }));
}
