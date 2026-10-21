import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckCircleOutlined, DownloadOutlined, ReloadOutlined, UploadOutlined, } from '@ant-design/icons';
import { Alert, Button, Card, Descriptions, Empty, Result, Space, Spin, Steps, Table, Tag, Typography, Upload, message, } from 'antd';
import axios from 'axios';
import dayjs from 'dayjs';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { confirmCompetitionRosterImport, downloadCompetitionRosterTemplate, getCompetitionEvent, getCompetitionRosterImport, precheckCompetitionRoster, } from '../../api/competition';
import { uploadPlatformFile } from '../../api/files';
import { useAuthStore } from '../../stores/authStore';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';
const validationStatusMeta = {
    valid: { label: '有效', color: 'success' },
    invalid: { label: '无效', color: 'error' },
};
function formatDate(value) {
    return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}
function idValue(value) {
    return String(value);
}
function isHandledApiError(error) {
    return axios.isAxiosError(error) && Boolean(error.response);
}
export function CompetitionRosterImportPage({ eventId, onNavigate, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:roster:view');
    const canImport = permissions.includes('competition:roster:import');
    const canConfirm = permissions.includes('competition:roster:confirm');
    const initialBatchId = new URLSearchParams(window.location.search).get('batchId') || undefined;
    const [selectedBatchId, setSelectedBatchId] = useState(initialBatchId);
    const [selectedFile, setSelectedFile] = useState();
    const [uploadFileList, setUploadFileList] = useState([]);
    const [itemQuery, setItemQuery] = useState({ page: 1, pageSize: 20 });
    const queryClient = useQueryClient();
    const eventQuery = useQuery({
        queryKey: ['competition-event', eventId],
        queryFn: () => getCompetitionEvent(eventId),
        enabled: canView,
    });
    const detailQuery = useQuery({
        queryKey: ['competition-roster-import', selectedBatchId, itemQuery],
        queryFn: () => getCompetitionRosterImport(selectedBatchId, itemQuery),
        enabled: canView && Boolean(selectedBatchId),
    });
    const precheckMutation = useMutation({
        mutationFn: async () => {
            if (!selectedFile) {
                throw new Error('请选择 Excel 文件');
            }
            const uploaded = await uploadPlatformFile(selectedFile, 'private');
            if (!uploaded.id)
                throw new Error('文件上传未返回 fileId');
            return precheckCompetitionRoster(eventId, {
                fileId: uploaded.id,
            });
        },
        onSuccess: (detail) => {
            message.success('名单预检完成，请在下方确认导入');
            setSelectedFile(undefined);
            setUploadFileList([]);
            setSelectedBatchId(idValue(detail.batchId || detail.id));
            setItemQuery({ page: 1, pageSize: 20 });
            queryClient.invalidateQueries({ queryKey: ['competition-roster-import', detail.batchId || detail.id] });
        },
        onError: (error) => {
            if (!isHandledApiError(error)) {
                message.error(error instanceof Error ? error.message : '名单预检失败');
            }
        },
    });
    const confirmMutation = useMutation({
        mutationFn: () => confirmCompetitionRosterImport(selectedBatchId),
        onSuccess: () => {
            message.success('名单已确认，选手已进入选手管理');
            queryClient.invalidateQueries({ queryKey: ['competition-roster-import', selectedBatchId] });
            queryClient.invalidateQueries({ queryKey: ['competition-players'] });
        },
        onError: (error) => {
            if (!isHandledApiError(error)) {
                message.error(error instanceof Error ? error.message : '名单确认失败');
            }
        },
    });
    const uploadProps = {
        accept: '.xlsx,.xls',
        maxCount: 1,
        fileList: uploadFileList,
        beforeUpload: (file) => {
            if (!/\.(xlsx|xls)$/i.test(file.name)) {
                message.error('仅支持 .xlsx 或 .xls 文件');
                return Upload.LIST_IGNORE;
            }
            setSelectedBatchId(undefined);
            setSelectedFile(file);
            setUploadFileList([file]);
            setItemQuery({ page: 1, pageSize: 20 });
            return false;
        },
        onRemove: () => {
            setSelectedFile(undefined);
            setUploadFileList([]);
        },
    };
    const downloadTemplate = async () => {
        try {
            const blob = await downloadCompetitionRosterTemplate();
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = '赛事名单导入模板.xlsx';
            anchor.click();
            URL.revokeObjectURL(url);
        }
        catch (error) {
            message.error(error instanceof Error ? error.message : '模板下载失败');
        }
    };
    const itemColumns = [
        { title: '企业', dataIndex: 'orgName', width: 220, render: (value) => value || '-' },
        { title: '姓名', dataIndex: 'name', width: 140 },
        { title: '报名工种', dataIndex: 'workType', width: 180, render: (value) => value || '-' },
        { title: '等级', dataIndex: 'level', width: 120, render: (value) => value || '-' },
        {
            title: '校验状态', dataIndex: 'validationStatus', width: 120,
            render: (value) => (_jsx(Tag, { color: validationStatusMeta[value]?.color, children: validationStatusMeta[value]?.label || value })),
        },
        { title: '错误原因', dataIndex: 'errorMessage', render: (value) => value || '-' },
    ];
    if (!canView) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u540D\u5355\u67E5\u770B\u6743\u9650", subTitle: "\u8BF7\u8054\u7CFB\u7BA1\u7406\u5458\u7533\u8BF7 competition:roster:view \u6743\u9650\u3002" });
    }
    if (eventQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (eventQuery.isError || !eventQuery.data) {
        return _jsx(Result, { status: "404", title: "\u8D5B\u4E8B\u4E0D\u5B58\u5728\u6216\u65E0\u6CD5\u8BBF\u95EE", extra: _jsx(Button, { onClick: () => onNavigate('/competition/events'), children: "\u8FD4\u56DE\u8D5B\u4E8B" }) });
    }
    const detail = detailQuery.data;
    const items = detail?.items?.items || [];
    const step = selectedBatchId ? 2 : selectedFile ? 1 : 0;
    return (_jsxs(CompetitionEventFrame, { event: eventQuery.data, activeSection: "rosters", onNavigate: onNavigate, children: [canImport ? (_jsxs(Card, { bordered: false, className: "competition-detail-card competition-roster-import-wizard", children: [_jsx(Steps, { current: step, items: [{ title: '选择文件' }, { title: '校验企业、工种与等级' }, { title: '确认导入' }] }), _jsx("div", { className: "competition-roster-import-wizard__body", children: _jsx(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: _jsxs("div", { children: [_jsx("div", { className: "competition-card-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u4E0A\u4F20\u540D\u5355\u6587\u4EF6" }), _jsx(Typography.Text, { type: "secondary", children: "\u7CFB\u7EDF\u4F1A\u6309\u6BCF\u4E00\u884C\u7684\u4F01\u4E1A\u540D\u79F0\u3001\u62A5\u540D\u5DE5\u79CD\u548C\u62A5\u540D\u7B49\u7EA7\u81EA\u52A8\u5339\u914D\uFF1B\u540C\u4E00\u9009\u624B\u53EF\u5728\u6587\u4EF6\u4E2D\u5360\u591A\u884C\u62A5\u540D\u4E0D\u540C\u5DE5\u79CD\u3002" })] }) }), _jsxs(Space, { wrap: true, children: [_jsx(Upload, { ...uploadProps, disabled: !canImport, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), disabled: !canImport, children: "\u9009\u62E9\u6587\u4EF6" }) }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: downloadTemplate, children: "\u4E0B\u8F7D\u6A21\u677F" }), _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), loading: precheckMutation.isPending, disabled: !selectedFile, onClick: () => precheckMutation.mutate(), children: "\u5F00\u59CB\u6821\u9A8C" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => { void eventQuery.refetch(); }, children: "\u5237\u65B0\u8D5B\u4E8B" })] })] }) }) })] })) : (_jsx(Alert, { type: "info", showIcon: true, message: "\u5F53\u524D\u8D26\u53F7\u4EC5\u53EF\u67E5\u770B\u540D\u5355\u9884\u68C0\u7ED3\u679C\uFF0C\u6682\u65E0\u5BFC\u5165\u6743\u9650\u3002" })), selectedBatchId && (_jsx(Card, { title: "\u5BFC\u5165\u9884\u89C8", bordered: false, className: "competition-detail-card", extra: detail?.status === 'pending_confirm' && canConfirm ? (_jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), loading: confirmMutation.isPending, onClick: () => confirmMutation.mutate(), children: "\u786E\u8BA4\u5BFC\u5165" })) : null, children: detailQuery.isLoading ? _jsx(Spin, {}) : detailQuery.isError ? _jsx(Result, { status: "error", title: "\u5BFC\u5165\u9884\u89C8\u52A0\u8F7D\u5931\u8D25" }) : !detail ? _jsx(Empty, { description: "\u6682\u65E0\u5BFC\u5165\u9884\u89C8" }) : _jsxs(_Fragment, { children: [_jsxs(Descriptions, { column: { xs: 1, sm: 2, md: 4 }, children: [_jsx(Descriptions.Item, { label: "\u4F01\u4E1A", children: detail.orgName || '按明细行匹配' }), _jsx(Descriptions.Item, { label: "\u8D5B\u4E8B\u5DE5\u79CD", children: detail.eventWorkTypeName || (detail.eventWorkTypeId ? idValue(detail.eventWorkTypeId) : '按明细行匹配') }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: _jsx(Tag, { color: detail.status === 'pending_confirm' ? 'warning' : detail.status === 'confirmed' ? 'success' : 'error', children: detail.status === 'pending_confirm' ? '待确认' : detail.status === 'confirmed' ? '已确认' : '校验失败' }) }), _jsx(Descriptions.Item, { label: "\u521B\u5EFA\u65F6\u95F4", children: formatDate(detail.createdAt) }), _jsx(Descriptions.Item, { label: "\u603B\u6570", children: detail.totalCount }), _jsx(Descriptions.Item, { label: "\u6709\u6548\u6570\u91CF", children: detail.validCount }), _jsx(Descriptions.Item, { label: "\u65E0\u6548\u6570\u91CF", children: detail.invalidCount }), _jsx(Descriptions.Item, { label: "\u8BF4\u660E", children: detail.summary || '-' })] }), _jsx(Table, { rowKey: (record) => idValue(record.id), loading: detailQuery.isFetching, dataSource: items, columns: itemColumns, scroll: { x: 1150 }, pagination: {
                                current: itemQuery.page,
                                pageSize: itemQuery.pageSize,
                                total: detail.items?.total || 0,
                                showSizeChanger: true,
                                onChange: (page, pageSize) => setItemQuery((prev) => ({
                                    ...prev,
                                    page: typeof pageSize === 'number' && pageSize !== prev.pageSize ? 1 : page,
                                    pageSize: typeof pageSize === 'number' && pageSize > 0 ? pageSize : prev.pageSize,
                                })),
                            } })] }) }))] }));
}
