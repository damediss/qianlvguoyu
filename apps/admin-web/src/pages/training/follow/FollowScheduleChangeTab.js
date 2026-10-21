import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Alert, Button, Checkbox, Descriptions, Drawer, Empty, Input, Modal, Space, Table, Tag, Typography, message, } from 'antd';
import { CheckOutlined, CloseOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
function parseJson(raw) {
    if (!raw)
        return null;
    try {
        return JSON.parse(raw);
    }
    catch {
        return null;
    }
}
function fieldRows(before, after) {
    const keys = [
        'scheduleDate',
        'startTime',
        'endTime',
        'roomName',
        'teacherName',
        'courseName',
        'dayOfWeek',
    ];
    return keys
        .map((k) => ({
        key: k,
        before: before?.[k] == null || before[k] === '' ? '—' : String(before[k]),
        after: after?.[k] == null || after[k] === '' ? '—' : String(after[k]),
        changed: before && after ? String(before[k] ?? '') !== String(after[k] ?? '') : false,
    }))
        .filter((r) => r.before !== '—' || r.after !== '—');
}
const STATUS_COLOR = {
    pending: 'gold',
    approved: 'green',
    rejected: 'red',
    cancelled: 'default',
};
export function FollowScheduleChangeTab({ classId }) {
    const queryClient = useQueryClient();
    const [statusFilter, setStatusFilter] = useState('pending');
    const [detail, setDetail] = useState(null);
    const [force, setForce] = useState(false);
    const [rejectOpen, setRejectOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const { data, isLoading, refetch } = useQuery({
        queryKey: ['schedule-change-requests', classId, statusFilter],
        queryFn: () => trainApi.listScheduleChangeRequests({ classId, status: statusFilter }),
        enabled: classId > 0,
    });
    const list = data ?? [];
    const approveMu = useMutation({
        mutationFn: (id) => trainApi.approveScheduleChangeRequest(id, force),
        onSuccess: () => {
            message.success('已通过并更新课表');
            setDetail(null);
            setForce(false);
            void queryClient.invalidateQueries({ queryKey: ['schedule-change-requests'] });
            void queryClient.invalidateQueries({ queryKey: ['follow-schedule', classId] });
            void queryClient.invalidateQueries({ queryKey: ['follow-schedule-status', classId] });
            void queryClient.invalidateQueries({ queryKey: ['follow-teacher-days', classId] });
        },
        onError: (e) => message.error(e.message || '审批失败'),
    });
    const rejectMu = useMutation({
        mutationFn: (id) => trainApi.rejectScheduleChangeRequest(id, { rejectReason: rejectReason || undefined }),
        onSuccess: () => {
            message.success('已驳回');
            setRejectOpen(false);
            setRejectReason('');
            setDetail(null);
            void queryClient.invalidateQueries({ queryKey: ['schedule-change-requests'] });
        },
        onError: (e) => message.error(e.message || '驳回失败'),
    });
    const before = useMemo(() => parseJson(detail?.beforeSnapshot), [detail]);
    const after = useMemo(() => parseJson(detail?.afterPayload), [detail]);
    const diffs = useMemo(() => fieldRows(before, after), [before, after]);
    const columns = [
        {
            title: '类型',
            dataIndex: 'changeTypeName',
            width: 110,
            render: (v, r) => v || r.changeType,
        },
        {
            title: '课次',
            dataIndex: 'scheduleId',
            width: 90,
            render: (v) => (v != null ? `#${v}` : '加课'),
        },
        {
            title: '原因',
            dataIndex: 'reason',
            ellipsis: true,
            render: (v) => v || '—',
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (s, r) => _jsx(Tag, { color: STATUS_COLOR[s] || 'default', children: r.statusName || s }),
        },
        {
            title: '申请时间',
            dataIndex: 'createdAt',
            width: 160,
            render: (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '—'),
        },
        {
            title: '操作',
            width: 160,
            render: (_, r) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", onClick: () => setDetail(r), children: "\u8BE6\u60C5" }), r.status === 'pending' ? (_jsx(_Fragment, { children: _jsx(Button, { type: "link", size: "small", onClick: () => {
                                setDetail(r);
                                setForce(false);
                            }, children: "\u5BA1\u6279" }) })) : null] })),
        },
    ];
    return (_jsxs("div", { children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "App \u8001\u5E08\u8C03\u8BFE\u7533\u8BF7\u5BA1\u6279", description: "\u901A\u8FC7\u540E\u5373\u65F6\u5199\u5165\u7ED3\u6784\u5316\u8BFE\u8868\u5E76\u6807\u8BB0\u300C\u5DF2\u8C03\u8BFE\u300D\u3002\u7D27\u6025\u76F4\u63A5\u6539\u8BFE\u4ECD\u5728\u300C\u8BFE\u7A0B\u8868\u300DTab\u3002" }), _jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Button, { type: statusFilter === 'pending' ? 'primary' : 'default', size: "small", onClick: () => setStatusFilter('pending'), children: "\u5F85\u5BA1\u6279" }), _jsx(Button, { type: statusFilter === undefined ? 'primary' : 'default', size: "small", onClick: () => setStatusFilter(undefined), children: "\u5168\u90E8" }), _jsx(Button, { type: statusFilter === 'approved' ? 'primary' : 'default', size: "small", onClick: () => setStatusFilter('approved'), children: "\u5DF2\u901A\u8FC7" }), _jsx(Button, { type: statusFilter === 'rejected' ? 'primary' : 'default', size: "small", onClick: () => setStatusFilter('rejected'), children: "\u5DF2\u9A73\u56DE" }), _jsx(Button, { size: "small", icon: _jsx(ReloadOutlined, {}), onClick: () => void refetch(), children: "\u5237\u65B0" })] }), _jsx(Table, { rowKey: (r) => String(r.id), loading: isLoading, columns: columns, dataSource: list, pagination: { pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 条` }, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u8C03\u8BFE\u7533\u8BF7" }) }, size: "small" }), _jsx(Drawer, { title: "\u8C03\u8BFE\u7533\u8BF7\u8BE6\u60C5", width: 520, open: !!detail, onClose: () => {
                    setDetail(null);
                    setForce(false);
                }, extra: detail?.status === 'pending' ? (_jsxs(Space, { children: [_jsx(Button, { danger: true, icon: _jsx(CloseOutlined, {}), onClick: () => {
                                setRejectReason('');
                                setRejectOpen(true);
                            }, children: "\u9A73\u56DE" }), _jsx(Button, { type: "primary", icon: _jsx(CheckOutlined, {}), loading: approveMu.isPending, onClick: () => detail && approveMu.mutate(detail.id), children: "\u901A\u8FC7" })] })) : null, children: detail ? (_jsxs(_Fragment, { children: [_jsxs(Descriptions, { column: 1, size: "small", bordered: true, style: { marginBottom: 16 }, children: [_jsx(Descriptions.Item, { label: "\u7C7B\u578B", children: detail.changeTypeName || detail.changeType }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: _jsx(Tag, { color: STATUS_COLOR[detail.status] || 'default', children: detail.statusName || detail.status }) }), _jsx(Descriptions.Item, { label: "\u8BFE\u6B21 ID", children: detail.scheduleId ?? '—' }), _jsx(Descriptions.Item, { label: "\u7533\u8BF7\u4EBA\u89D2\u8272", children: detail.applicantRoleCode || '—' }), _jsx(Descriptions.Item, { label: "\u539F\u56E0", children: detail.reason || '—' }), detail.rejectReason ? (_jsx(Descriptions.Item, { label: "\u9A73\u56DE\u539F\u56E0", children: detail.rejectReason })) : null] }), detail.changeType === 'cancel' ? (_jsx(Alert, { type: "warning", showIcon: true, message: "\u505C\u8BFE\uFF1A\u901A\u8FC7\u540E\u5C06\u5220\u9664\u8BE5\u8BFE\u6B21" })) : (_jsx(Table, { size: "small", pagination: false, rowKey: "key", dataSource: diffs, columns: [
                                { title: '字段', dataIndex: 'key', width: 120 },
                                { title: '变更前', dataIndex: 'before' },
                                {
                                    title: '变更后',
                                    dataIndex: 'after',
                                    render: (v, r) => r.changed ? _jsx(Typography.Text, { type: "danger", children: v }) : v,
                                },
                            ] })), detail.status === 'pending' ? (_jsx("div", { style: { marginTop: 16 }, children: _jsx(Checkbox, { checked: force, onChange: (e) => setForce(e.target.checked), children: "\u5F3A\u5236\u901A\u8FC7\uFF08\u5FFD\u7565\u6559\u5E08/\u6559\u5BA4\u51B2\u7A81\uFF09" }) })) : null] })) : null }), _jsx(Modal, { title: "\u9A73\u56DE\u8C03\u8BFE\u7533\u8BF7", open: rejectOpen, onCancel: () => setRejectOpen(false), onOk: () => detail && rejectMu.mutate(detail.id), confirmLoading: rejectMu.isPending, okText: "\u786E\u8BA4\u9A73\u56DE", okButtonProps: { danger: true }, children: _jsx(Input.TextArea, { rows: 3, maxLength: 500, placeholder: "\u9A73\u56DE\u539F\u56E0\uFF08\u9009\u586B\uFF09", value: rejectReason, onChange: (e) => setRejectReason(e.target.value) }) })] }));
}
