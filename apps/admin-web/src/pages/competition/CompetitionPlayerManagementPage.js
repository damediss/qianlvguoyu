import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, TeamOutlined, } from '@ant-design/icons';
import { Button, Card, Form, Input, Modal, Result, Select, Space, Spin, Table, Tag, Typography, message, } from 'antd';
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { createCompetitionPlayer, deleteCompetitionPlayer, getCompetitionPlayerForEdit, listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEventOrganizations, pageCompetitionEvents, pageCompetitionPlayers, updateCompetitionPlayer, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
const idCardWeights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
const idCardCheckCodes = '10X98765432';
function normalizeIdCardNo(value) {
    const normalized = value?.trim().toUpperCase().replace(/\s/g, '');
    return normalized || undefined;
}
function normalizeMobile(value) {
    const normalized = value?.trim().replace(/\s/g, '');
    return normalized || undefined;
}
function isValidIdCardNo(value) {
    const normalized = normalizeIdCardNo(value);
    if (!normalized || !/^\d{17}[0-9X]$/.test(normalized))
        return false;
    const checksum = idCardWeights.reduce((sum, weight, index) => sum + Number(normalized[index]) * weight, 0);
    return idCardCheckCodes[checksum % 11] === normalized[17];
}
function isValidMobile(value) {
    const normalized = normalizeMobile(value);
    return Boolean(normalized && /^1[3-9]\d{9}$/.test(normalized));
}
function errorText(error, fallback) {
    const responseMessage = error?.response?.data?.message;
    if (typeof responseMessage === 'string' && responseMessage.trim())
        return responseMessage;
    return error instanceof Error && error.message.trim() ? error.message : fallback;
}
function idValue(value) {
    return value == null ? '' : String(value);
}
function statusTag(status) {
    const meta = {
        pending_audit: { label: '待审核', color: 'warning' },
        eligible_unactivated: { label: '待激活', color: 'processing' },
        eligible_activated: { label: '已激活', color: 'success' },
        rejected: { label: '已驳回', color: 'error' },
        withdrawn: { label: '已撤回', color: 'default' },
    };
    const value = status ? meta[status] : undefined;
    return _jsx(Tag, { color: value?.color, children: value?.label || status || '-' });
}
export function CompetitionPlayerManagementPage() {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:player:view');
    const canCreate = permissions.includes('competition:player:create');
    const canUpdate = permissions.includes('competition:player:update');
    const canDelete = permissions.includes('competition:player:delete');
    const canViewOrganizations = permissions.includes('competition:event-org:view');
    const [selectedEventId, setSelectedEventId] = useState();
    const [selectedOrgId, setSelectedOrgId] = useState();
    const [selectedWorkTypeId, setSelectedWorkTypeId] = useState();
    const [keyword, setKeyword] = useState('');
    const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: undefined });
    const [editing, setEditing] = useState();
    const [editLoadingId, setEditLoadingId] = useState();
    const [formOpen, setFormOpen] = useState(false);
    const [selectedWorkTypes, setSelectedWorkTypes] = useState([]);
    const [form] = Form.useForm();
    const queryClient = useQueryClient();
    const eventsQuery = useQuery({
        queryKey: ['competition-events', 'players'],
        queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 500 }),
        enabled: canView,
    });
    const events = eventsQuery.data?.items || [];
    useEffect(() => {
        if (!selectedEventId && events.length)
            setSelectedEventId(String(events[0].id));
    }, [events, selectedEventId]);
    const organizationsQuery = useQuery({
        queryKey: ['competition-event-organizations', selectedEventId, 'active'],
        queryFn: () => pageCompetitionEventOrganizations(selectedEventId, { page: 1, pageSize: 500, status: 'active' }),
        enabled: canView && canViewOrganizations && Boolean(selectedEventId),
    });
    const workTypesQuery = useQuery({
        queryKey: ['competition-event-work-types', selectedEventId],
        queryFn: () => listCompetitionEventWorkTypes(selectedEventId),
        enabled: canView && Boolean(selectedEventId),
    });
    const orgOptions = useMemo(() => ((organizationsQuery.data?.items || []).map((item) => ({
        value: idValue(item.orgId),
        label: item.orgName || idValue(item.orgId),
    }))), [organizationsQuery.data?.items]);
    const workTypeOptions = useMemo(() => ((workTypesQuery.data || [])
        .filter((item) => item.status === 'active')
        .map((item) => ({ value: idValue(item.id), label: item.displayName || item.workTypeName || idValue(item.workTypeId) }))), [workTypesQuery.data]);
    const workTypeLevelQueries = useQueries({
        queries: workTypeOptions.map((workType) => ({
            queryKey: ['competition-event-work-type-levels', selectedEventId, workType.value],
            queryFn: () => listCompetitionEventWorkTypeLevels(selectedEventId, workType.value),
            enabled: canView && Boolean(selectedEventId),
        })),
    });
    const levelsByWorkType = useMemo(() => {
        const result = {};
        workTypeOptions.forEach((workType, index) => {
            const levels = (workTypeLevelQueries[index]?.data || []);
            result[workType.value] = levels.map((level) => ({
                value: idValue(level.id),
                label: level.levelName ? `${level.levelName}（${level.levelCode}）` : level.levelCode,
            }));
        });
        return result;
    }, [workTypeLevelQueries, workTypeOptions]);
    useEffect(() => {
        if (selectedOrgId && !orgOptions.some((item) => item.value === selectedOrgId))
            setSelectedOrgId(undefined);
    }, [orgOptions, selectedOrgId]);
    useEffect(() => {
        if (selectedWorkTypeId && !workTypeOptions.some((item) => item.value === selectedWorkTypeId))
            setSelectedWorkTypeId(undefined);
    }, [selectedWorkTypeId, workTypeOptions]);
    // 渲染期派生有效工种：跨赛事切换/URL 参数混合时，未在当前赛事工种列表中的 id 在本帧即钳为 undefined，
    // 查询不会发出（effect 事后纠偏存在一帧竞态，会打出 404/422 控制台噪音）
    const effectiveWorkTypeId = workTypeOptions.some((item) => item.value === selectedWorkTypeId)
        ? selectedWorkTypeId
        : undefined;
    const playersQuery = useQuery({
        queryKey: ['competition-players', selectedEventId, selectedOrgId, effectiveWorkTypeId, query],
        queryFn: () => pageCompetitionPlayers({
            page: query.page,
            pageSize: query.pageSize,
            eventId: selectedEventId,
            orgId: selectedOrgId,
            eventWorkTypeId: effectiveWorkTypeId,
            keyword: query.keyword,
        }),
        enabled: canView && Boolean(selectedEventId),
    });
    const saveMutation = useMutation({
        mutationFn: (values) => {
            if (!selectedEventId)
                throw new Error('请选择赛事任务');
            if (!selectedWorkTypes.length)
                throw new Error('请至少选择一个赛事工种');
            if (selectedWorkTypes.some((selection) => !selection.eventWorkTypeLevelId)) {
                throw new Error('请为每个赛事工种选择对应等级');
            }
            const firstWorkType = selectedWorkTypes[0];
            const firstLevel = levelsByWorkType[firstWorkType.eventWorkTypeId]?.find((level) => level.value === firstWorkType.eventWorkTypeLevelId);
            const workTypes = selectedWorkTypes.map((selection) => ({
                eventWorkTypeId: selection.eventWorkTypeId,
                eventWorkTypeLevelId: selection.eventWorkTypeLevelId,
            }));
            if (editing) {
                const idCardNo = normalizeIdCardNo(values.idCardNo);
                const mobile = normalizeMobile(values.mobile);
                const payload = {
                    orgId: values.orgId,
                    eventWorkTypeId: firstWorkType.eventWorkTypeId,
                    name: values.name,
                    idCardNo,
                    mobile,
                    level: firstLevel?.label,
                    workTypes,
                };
                return updateCompetitionPlayer(editing.registrationId, payload);
            }
            const idCardNo = normalizeIdCardNo(values.idCardNo);
            const mobile = normalizeMobile(values.mobile);
            const payload = {
                eventId: selectedEventId,
                orgId: values.orgId,
                eventWorkTypeId: firstWorkType.eventWorkTypeId,
                name: values.name,
                idCardNo: idCardNo || '',
                mobile: mobile || '',
                level: firstLevel?.label,
                workTypes,
            };
            return createCompetitionPlayer(payload);
        },
        onSuccess: () => {
            message.success(editing ? '选手信息已更新' : '选手已新增');
            setFormOpen(false);
            setEditing(undefined);
            setSelectedWorkTypes([]);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['competition-players'] });
        },
        onError: (error) => message.error(errorText(error, '保存选手失败')),
    });
    const deleteMutation = useMutation({
        mutationFn: (registrationId) => deleteCompetitionPlayer(registrationId),
        onSuccess: () => {
            message.success('选手已删除');
            queryClient.invalidateQueries({ queryKey: ['competition-players'] });
        },
        onError: (error) => message.error(errorText(error, '删除选手失败')),
    });
    const openCreate = () => {
        setEditing(undefined);
        form.resetFields();
        setSelectedWorkTypes(effectiveWorkTypeId
            ? [{ eventWorkTypeId: effectiveWorkTypeId }]
            : []);
        form.setFieldsValue({ orgId: selectedOrgId });
        setFormOpen(true);
    };
    const openEdit = async (record) => {
        setEditLoadingId(record.registrationId);
        try {
            const detail = await getCompetitionPlayerForEdit(record.registrationId);
            setEditing(detail);
            const detailWorkTypes = detail.workTypes && detail.workTypes.length
                ? detail.workTypes.map((workType) => ({
                    eventWorkTypeId: idValue(workType.eventWorkTypeId),
                    eventWorkTypeLevelId: workType.eventWorkTypeLevelId == null
                        ? undefined : idValue(workType.eventWorkTypeLevelId),
                }))
                : detail.eventWorkTypeId
                    ? [{
                            eventWorkTypeId: idValue(detail.eventWorkTypeId),
                            eventWorkTypeLevelId: detail.eventWorkTypeLevelId == null
                                ? undefined : idValue(detail.eventWorkTypeLevelId),
                        }]
                    : [];
            setSelectedWorkTypes(detailWorkTypes);
            form.setFieldsValue({
                orgId: idValue(detail.orgId),
                name: detail.name || '',
                idCardNo: detail.idCardNo && detail.idCardNo !== '-' ? detail.idCardNo : '',
                mobile: detail.mobile && detail.mobile !== '-' ? detail.mobile : '',
            });
            setFormOpen(true);
        }
        catch (error) {
            message.error(errorText(error, '选手详情加载失败'));
        }
        finally {
            setEditLoadingId(undefined);
        }
    };
    const columns = [
        { title: '赛事任务', dataIndex: 'eventName', width: 180, ellipsis: true },
        { title: '企业', dataIndex: 'orgName', width: 170, ellipsis: true },
        {
            title: '工种 / 等级', key: 'workTypes', width: 260,
            render: (_, record) => {
                const workTypes = record.workTypes?.length
                    ? record.workTypes
                    : record.eventWorkTypeId
                        ? [{
                                eventWorkTypeId: record.eventWorkTypeId,
                                eventWorkTypeName: record.eventWorkTypeName,
                                levelName: record.eventWorkTypeLevelName || record.level,
                                level: record.level,
                            }]
                        : [];
                return _jsx(Space, { wrap: true, size: [4, 4], children: workTypes.map((workType) => (_jsxs(Tag, { children: [workType.eventWorkTypeName || idValue(workType.eventWorkTypeId), workType.levelName || workType.level ? ` · ${workType.levelName || workType.level}` : ''] }, idValue(workType.eventWorkTypeId)))) });
            },
        },
        { title: '姓名', dataIndex: 'name', width: 100 },
        { title: '身份证号', dataIndex: 'idCardNo', width: 150 },
        { title: '手机号', dataIndex: 'mobile', width: 130 },
        { title: '参赛状态', dataIndex: 'status', width: 100, render: statusTag },
        {
            title: '操作', key: 'actions', fixed: 'right', width: 140,
            render: (_, record) => (_jsxs(Space, { size: 0, children: [canUpdate && _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), loading: editLoadingId === record.registrationId, disabled: editLoadingId != null, onClick: () => { void openEdit(record); }, children: "\u7F16\u8F91" }), canDelete && _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), loading: deleteMutation.isPending && deleteMutation.variables === record.registrationId, onClick: () => Modal.confirm({ title: '确认删除该选手？', content: '删除后将从本赛事选手名单中移除，选手身份不会被删除。', okText: '删除', okButtonProps: { danger: true }, onOk: () => deleteMutation.mutate(record.registrationId) }), children: "\u5220\u9664" })] })),
        },
    ];
    if (!canView)
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u9009\u624B\u7BA1\u7406\u6743\u9650" });
    if (eventsQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (eventsQuery.isError)
        return _jsx(Result, { status: "error", title: "\u8D5B\u4E8B\u4EFB\u52A1\u52A0\u8F7D\u5931\u8D25" });
    return (_jsxs("div", { className: "competition-page", children: [_jsxs("section", { className: "competition-page-heading", children: [_jsxs("div", { children: [_jsxs(Typography.Title, { level: 2, children: [_jsx(TeamOutlined, {}), " \u9009\u624B\u4FE1\u606F"] }), _jsx(Typography.Paragraph, { type: "secondary", children: "\u6309\u8D5B\u4E8B\u4EFB\u52A1\u5212\u5206\u67E5\u770B\u9009\u624B\uFF0C\u652F\u6301\u6309\u4F01\u4E1A\u3001\u8D5B\u4E8B\u5DE5\u79CD\u7B5B\u9009\uFF0C\u5E76\u7EF4\u62A4\u9009\u624B\u57FA\u7840\u4FE1\u606F\u3002" })] }), _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => { void eventsQuery.refetch(); void playersQuery.refetch(); }, children: "\u5237\u65B0" }), canCreate && _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), disabled: !selectedEventId, onClick: openCreate, children: "\u65B0\u589E\u9009\u624B" })] })] }), _jsx(Card, { bordered: false, className: "competition-filter-card", children: _jsxs(Space, { wrap: true, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", value: selectedEventId, options: events.map((event) => ({ value: idValue(event.id), label: `${event.code} · ${event.name}` })), onChange: (value) => { setSelectedEventId(value); setQuery((current) => ({ ...current, page: 1 })); setSelectedOrgId(undefined); setSelectedWorkTypeId(undefined); setSelectedWorkTypes([]); }, placeholder: "\u9009\u62E9\u8D5B\u4E8B\u4EFB\u52A1", style: { width: 280 } }), _jsx(Select, { allowClear: true, showSearch: true, optionFilterProp: "label", value: selectedOrgId, options: orgOptions, onChange: (value) => { setSelectedOrgId(value); setQuery((current) => ({ ...current, page: 1 })); }, placeholder: "\u6309\u4F01\u4E1A\u7B5B\u9009", style: { width: 220 }, loading: organizationsQuery.isLoading }), _jsx(Select, { allowClear: true, showSearch: true, optionFilterProp: "label", value: effectiveWorkTypeId, options: workTypeOptions, onChange: (value) => { setSelectedWorkTypeId(value); setQuery((current) => ({ ...current, page: 1 })); }, placeholder: "\u6309\u5DE5\u79CD\u7B5B\u9009", style: { width: 200 }, loading: workTypesQuery.isLoading }), _jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u59D3\u540D", value: keyword, onChange: (event) => setKeyword(event.target.value), onPressEnter: () => setQuery((current) => ({ ...current, page: 1, keyword: keyword.trim() || undefined })), style: { width: 220 } }), _jsx(Button, { type: "primary", ghost: true, icon: _jsx(SearchOutlined, {}), onClick: () => setQuery((current) => ({ ...current, page: 1, keyword: keyword.trim() || undefined })), children: "\u67E5\u8BE2" })] }) }), _jsx(Card, { bordered: false, className: "competition-table-card", children: _jsx(Table, { rowKey: (record) => idValue(record.registrationId), loading: playersQuery.isFetching, dataSource: playersQuery.data?.items || [], columns: columns, scroll: { x: 1500 }, pagination: { current: query.page, pageSize: query.pageSize, total: playersQuery.data?.total || 0, showSizeChanger: true, onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })) } }) }), _jsx(Modal, { title: editing ? '编辑选手' : '新增选手', open: formOpen, destroyOnClose: true, width: 700, confirmLoading: saveMutation.isPending, onCancel: () => { setFormOpen(false); setEditing(undefined); setSelectedWorkTypes([]); form.resetFields(); }, onOk: () => form.submit(), children: _jsxs(Form, { form: form, layout: "vertical", onFinish: (values) => saveMutation.mutate(values), children: [_jsx(Form.Item, { label: "\u8D5B\u4E8B\u4EFB\u52A1", children: _jsx(Input, { value: events.find((event) => idValue(event.id) === selectedEventId)?.name || selectedEventId, disabled: true }) }), _jsx(Form.Item, { name: "orgId", label: "\u4F01\u4E1A", rules: [{ required: true, message: '请选择企业' }], children: _jsx(Select, { options: orgOptions, showSearch: true, optionFilterProp: "label", placeholder: "\u8BF7\u9009\u62E9\u4F01\u4E1A" }) }), _jsxs(Form.Item, { label: "\u62A5\u540D\u5DE5\u79CD", required: true, help: "\u53EF\u9009\u62E9\u591A\u4E2A\u5DE5\u79CD\uFF1B\u6BCF\u4E2A\u5DE5\u79CD\u5FC5\u987B\u5355\u72EC\u9009\u62E9\u5176\u5BF9\u5E94\u7B49\u7EA7\u3002", validateStatus: selectedWorkTypes.length > 0 && selectedWorkTypes.every((item) => item.eventWorkTypeLevelId) ? undefined : 'warning', children: [_jsx(Select, { mode: "multiple", value: selectedWorkTypes.map((item) => item.eventWorkTypeId), options: workTypeOptions, showSearch: true, optionFilterProp: "label", placeholder: "\u8BF7\u9009\u62E9\u4E00\u4E2A\u6216\u591A\u4E2A\u8D5B\u4E8B\u5DE5\u79CD", onChange: (values) => {
                                        setSelectedWorkTypes((current) => values.map((value) => (current.find((item) => item.eventWorkTypeId === value)
                                            || { eventWorkTypeId: value })));
                                    } }), _jsx(Space, { direction: "vertical", size: 8, style: { width: '100%', marginTop: 12 }, children: selectedWorkTypes.map((selection) => {
                                        const workType = workTypeOptions.find((item) => item.value === selection.eventWorkTypeId);
                                        const levels = levelsByWorkType[selection.eventWorkTypeId] || [];
                                        const loading = workTypeLevelQueries[workTypeOptions.findIndex((item) => item.value === selection.eventWorkTypeId)]?.isLoading;
                                        return (_jsxs(Space, { style: { display: 'flex' }, align: "center", children: [_jsx(Typography.Text, { style: { minWidth: 150 }, children: workType?.label || selection.eventWorkTypeId }), _jsx(Select, { value: selection.eventWorkTypeLevelId, options: levels, loading: loading, disabled: !levels.length, allowClear: true, placeholder: levels.length ? '选择对应等级' : '该工种暂无已配置等级', style: { minWidth: 250 }, onChange: (value) => setSelectedWorkTypes((current) => current.map((item) => (item.eventWorkTypeId === selection.eventWorkTypeId
                                                        ? { ...item, eventWorkTypeLevelId: value }
                                                        : item))) })] }, selection.eventWorkTypeId));
                                    }) })] }), _jsxs("div", { className: "competition-player-identity-fields", children: [_jsx(Form.Item, { name: "name", label: "\u59D3\u540D", rules: [{ required: true, whitespace: true, message: '请输入姓名' }], children: _jsx(Input, { maxLength: 128 }) }), _jsx(Form.Item, { name: "idCardNo", label: "\u8EAB\u4EFD\u8BC1\u53F7", rules: [
                                        { required: !editing, whitespace: !editing, message: '请输入身份证号' },
                                        {
                                            validator: (_, value) => (!value?.trim() || isValidIdCardNo(value)
                                                ? Promise.resolve()
                                                : Promise.reject(new Error('请输入有效的18位身份证号'))),
                                        },
                                    ], extra: editing ? '请核对完整信息；不修改请保持原值' : undefined, children: _jsx(Input, { maxLength: 18 }) }), _jsx(Form.Item, { name: "mobile", label: "\u624B\u673A\u53F7", rules: [
                                        { required: !editing, whitespace: !editing, message: '请输入手机号' },
                                        {
                                            validator: (_, value) => (!value?.trim() || isValidMobile(value)
                                                ? Promise.resolve()
                                                : Promise.reject(new Error('请输入有效的11位手机号'))),
                                        },
                                    ], extra: editing ? '请核对完整信息；不修改请保持原值' : undefined, children: _jsx(Input, { maxLength: 11, inputMode: "numeric" }) })] })] }) })] }));
}
