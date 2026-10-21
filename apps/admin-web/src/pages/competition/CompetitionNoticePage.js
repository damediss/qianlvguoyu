import { jsxs as _jsxs, jsx as _jsx, Fragment as _Fragment } from "react/jsx-runtime";
import { EditOutlined, EyeOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SendOutlined, } from '@ant-design/icons';
import { Button, Card, Modal, Result, Select, Space, Spin, Table, Tag, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import { getCompetitionEvent, pageCompetitionNotices, publishCompetitionNotice, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import { CompetitionNoticeEditorDrawer, contentTypeMeta, } from './CompetitionNoticeEditorDrawer';
const statusOptions = [
    { label: '草稿', value: 'draft' },
    { label: '已发布', value: 'published' },
];
const statusMeta = {
    draft: { label: '草稿', color: 'default' },
    published: { label: '已发布', color: 'success' },
};
const contentTypeOptions = Object.entries(contentTypeMeta).map(([value, meta]) => ({
    label: meta.label,
    value: value,
}));
function formatDate(value) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}
export function CompetitionNoticePage({ eventId, onNavigate, embedded = false, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:notice:view');
    const canManage = permissions.includes('competition:notice:publish');
    const [query, setQuery] = useState({ page: 1, pageSize: 10 });
    const [noticeMode, setNoticeMode] = useState();
    const [selectedNoticeId, setSelectedNoticeId] = useState();
    const queryClient = useQueryClient();
    const eventQuery = useQuery({
        queryKey: ['competition-event', eventId],
        queryFn: () => getCompetitionEvent(eventId),
        enabled: canView,
    });
    const noticesQuery = useQuery({
        queryKey: ['competition-notices', eventId, query],
        queryFn: () => pageCompetitionNotices(eventId, query),
        enabled: canView,
    });
    const publishMutation = useMutation({
        mutationFn: (noticeId) => publishCompetitionNotice(noticeId),
        onSuccess: () => {
            message.success('公告已发布');
            queryClient.invalidateQueries({ queryKey: ['competition-notices', eventId] });
        },
    });
    const openNotice = (notice, mode) => {
        setSelectedNoticeId(notice.id);
        setNoticeMode(mode);
    };
    const publish = (notice) => {
        if (!canManage || notice.status !== 'draft')
            return;
        const type = notice.contentType || 'markdown';
        const pdf = notice.assets?.find((asset) => asset.assetType === 'pdf');
        Modal.confirm({
            title: '发布公告？',
            content: (_jsxs("div", { children: [_jsxs("p", { children: ["\u516C\u544A\u300C", notice.title, "\u300D\u53D1\u5E03\u540E\u5C06\u4E0D\u53EF\u4FEE\u6539\u3002"] }), _jsxs("p", { children: ["\u53D1\u5E03\u7C7B\u578B\uFF1A", contentTypeMeta[type].label] }), pdf && _jsxs("p", { children: ["\u8D5B\u4E8B\u6587\u4EF6\uFF1A", pdf.originalName] })] })),
            okText: '发布',
            cancelText: '取消',
            onOk: () => publishMutation.mutateAsync(notice.id),
        });
    };
    const columns = useMemo(() => [
        {
            title: '标题',
            dataIndex: 'title',
            render: (value, record) => (_jsx(Button, { type: "link", className: "competition-event-name", onClick: () => openNotice(record, 'view'), children: value })),
        },
        {
            title: '类型',
            dataIndex: 'contentType',
            width: 140,
            render: (value) => {
                const type = value || 'markdown';
                return _jsx(Tag, { color: contentTypeMeta[type].color, children: contentTypeMeta[type].label });
            },
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (value) => (_jsx(Tag, { color: statusMeta[value]?.color, children: statusMeta[value]?.label || value })),
        },
        { title: '版本', dataIndex: 'version', width: 80 },
        { title: '创建时间', dataIndex: 'createdAt', width: 175, render: formatDate },
        { title: '发布时间', dataIndex: 'publishedAt', width: 175, render: formatDate },
        {
            title: '操作',
            key: 'actions',
            width: 220,
            fixed: 'right',
            render: (_, record) => (_jsxs(Space, { size: 0, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => openNotice(record, 'view'), children: "\u67E5\u770B" }), canManage && record.status === 'draft' && (_jsxs(_Fragment, { children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openNotice(record, 'edit'), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(SendOutlined, {}), onClick: () => publish(record), children: "\u53D1\u5E03" })] }))] })),
        },
    ], [canManage]);
    if (!canView) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u516C\u544A\u67E5\u770B\u6743\u9650", subTitle: "\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u7533\u8BF7 competition:notice:view \u6743\u9650\u3002" });
    }
    if (eventQuery.isLoading || noticesQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (eventQuery.isError || !eventQuery.data) {
        return _jsx(Result, { status: "404", title: "\u8D5B\u4E8B\u4E0D\u5B58\u5728\u6216\u65E0\u6CD5\u8BBF\u95EE", extra: _jsx(Button, { onClick: () => onNavigate('/competition/events'), children: "\u8FD4\u56DE\u8BA1\u5212" }) });
    }
    if (noticesQuery.isError) {
        return _jsx(Result, { status: "error", title: "\u516C\u544A\u52A0\u8F7D\u5931\u8D25", extra: _jsx(Button, { onClick: () => noticesQuery.refetch(), children: "\u91CD\u65B0\u52A0\u8F7D" }) });
    }
    const content = (_jsxs(_Fragment, { children: [_jsx(Card, { bordered: false, className: "competition-filter-card", children: _jsxs(Space, { style: { width: '100%', justifyContent: 'space-between' }, wrap: true, children: [_jsxs(Space, { wrap: true, children: [_jsx(Select, { allowClear: true, placeholder: "\u6309\u72B6\u6001\u7B5B\u9009", options: statusOptions, value: query.status, onChange: (status) => setQuery((current) => ({ ...current, page: 1, status })), style: { width: 150 } }), _jsx(Select, { allowClear: true, placeholder: "\u6309\u7C7B\u578B\u7B5B\u9009", options: contentTypeOptions, value: query.contentType, onChange: (contentType) => setQuery((current) => ({ ...current, page: 1, contentType })), style: { width: 180 } }), _jsx(Button, { type: "primary", ghost: true, icon: _jsx(SearchOutlined, {}), onClick: () => noticesQuery.refetch(), children: "\u67E5\u8BE2" })] }), _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => noticesQuery.refetch(), children: "\u5237\u65B0" }), canManage && (_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => {
                                        setSelectedNoticeId(undefined);
                                        setNoticeMode('create');
                                    }, children: "\u521B\u5EFA\u516C\u544A" }))] })] }) }), _jsx(Card, { bordered: false, className: "competition-table-card", children: _jsx(Table, { rowKey: (record) => String(record.id), loading: noticesQuery.isFetching, dataSource: noticesQuery.data?.items || [], columns: columns, scroll: { x: 1080 }, pagination: {
                        current: query.page,
                        pageSize: query.pageSize,
                        total: noticesQuery.data?.total || 0,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 条公告`,
                        onChange: (page, pageSize) => setQuery((current) => ({ ...current, page, pageSize })),
                    } }) }), _jsx(CompetitionNoticeEditorDrawer, { eventId: eventId, mode: noticeMode, noticeId: selectedNoticeId, onClose: () => {
                    setNoticeMode(undefined);
                    setSelectedNoticeId(undefined);
                }, onSaved: () => queryClient.invalidateQueries({ queryKey: ['competition-notices', eventId] }) })] }));
    return embedded ? content : _jsx(CompetitionEventFrame, { event: eventQuery.data, activeSection: "notices", onNavigate: onNavigate, children: content });
}
