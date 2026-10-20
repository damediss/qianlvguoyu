import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Descriptions, Empty, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { getArchiveBasic } from '../../../api/certificationArchive';
import { dateTimeText, formatDeviceSnapshot, formatEvaluatorSnapshot, formatExamStaffSnapshot, formatRoomSnapshot, formatScopeSnapshot, } from './shared';
export function BasicArchive({ archiveId }) {
    const query = useQuery({
        queryKey: ['certification-archive-basic', archiveId],
        queryFn: () => getArchiveBasic(archiveId),
        enabled: !!archiveId,
    });
    if (!archiveId) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u5DF2\u521B\u5EFA\u5F52\u6863\u8BB0\u5F55\u7684\u4EFB\u52A1" });
    }
    if (!query.data) {
        return _jsx(Empty, { description: query.isLoading ? '加载中' : '尚未生成基础信息快照' });
    }
    const snapshot = query.data;
    return (_jsxs(Descriptions, { bordered: true, column: 2, size: "small", children: [_jsx(Descriptions.Item, { label: "\u5F52\u6863\u65F6\u95F4", children: dateTimeText(snapshot.createdAt) }), _jsx(Descriptions.Item, { label: "\u8BA4\u5B9A\u5B8C\u6210\u65F6\u95F4", children: dateTimeText(snapshot.completedAt) }), _jsx(Descriptions.Item, { label: "\u8BA4\u5B9A\u4EBA\u6570", children: snapshot.candidateCount }), _jsx(Descriptions.Item, { label: "\u5FEB\u7167\u8BB0\u5F55", children: snapshot.id }), _jsx(Descriptions.Item, { label: "\u8BA4\u5B9A\u8303\u56F4", span: 2, children: _jsx(Typography.Paragraph, { style: { whiteSpace: 'pre-wrap', marginBottom: 0 }, children: formatScopeSnapshot(snapshot.scopeJson) }) }), _jsx(Descriptions.Item, { label: "\u8003\u52A1\u914D\u7F6E", span: 2, children: _jsx(Typography.Paragraph, { style: { whiteSpace: 'pre-wrap', marginBottom: 0 }, children: formatExamStaffSnapshot(snapshot.examStaffJson) }) }), _jsx(Descriptions.Item, { label: "\u8BBE\u5907\u914D\u7F6E", span: 2, children: _jsx(Typography.Paragraph, { style: { whiteSpace: 'pre-wrap', marginBottom: 0 }, children: formatDeviceSnapshot(snapshot.deviceJson) }) }), _jsx(Descriptions.Item, { label: "\u8003\u573A\u914D\u7F6E", span: 2, children: _jsx(Typography.Paragraph, { style: { whiteSpace: 'pre-wrap', marginBottom: 0 }, children: formatRoomSnapshot(snapshot.roomJson) }) }), _jsx(Descriptions.Item, { label: "\u8003\u8BC4\u5458\u914D\u7F6E", span: 2, children: _jsx(Typography.Paragraph, { style: { whiteSpace: 'pre-wrap', marginBottom: 0 }, children: formatEvaluatorSnapshot(snapshot.evaluatorJson) }) })] }));
}
