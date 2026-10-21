import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ExportOutlined, LeftOutlined, LogoutOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, List, Spin, Tag, Typography, message } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { logout } from '../../api/auth';
import { getMyDefenseReport, listMyDefenseReports } from '../../api/defenseScoring';
import { fetchPlatformFileBlob } from '../../api/files';
import { useAuthStore } from '../../stores/authStore';
import { DefensePdfViewer, openPdfExternally } from './DefensePdfViewer';
import { DefenseScorePanel } from './DefenseScorePanel';
import { DEFENSE_LEVEL_LANES, defenseLevelLane, formatScore, statusLabel } from './defenseScoreCatalog';
import './DefenseReviewApp.css';
export function DefenseReviewApp() {
    const currentUser = useAuthStore((state) => state.currentUser);
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const [selectedReportId, setSelectedReportId] = useState();
    const [view, setView] = useState('list');
    useEffect(() => {
        const previous = document.querySelector('meta[name="viewport"]');
        const content = previous?.getAttribute('content');
        document.body.classList.add('defense-review-portrait');
        return () => {
            if (content)
                previous?.setAttribute('content', content);
            document.body.classList.remove('defense-review-portrait');
        };
    }, []);
    useEffect(() => {
        const previous = document.querySelector('meta[name="viewport"]');
        previous?.setAttribute('content', view === 'pdf'
            ? 'width=device-width, initial-scale=1, maximum-scale=4, user-scalable=yes, viewport-fit=cover'
            : 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover');
    }, [view]);
    const listQuery = useQuery({
        queryKey: ['my-defense-reports'],
        queryFn: listMyDefenseReports,
    });
    const detailQuery = useQuery({
        queryKey: ['my-defense-report', selectedReportId],
        queryFn: () => getMyDefenseReport(selectedReportId),
        enabled: Boolean(selectedReportId) && view === 'pdf',
    });
    async function handleLogout() {
        try {
            await logout();
        }
        catch {
            // 后端退出失败时仍清理本机登录态
        }
        finally {
            clearAuth();
            window.location.assign('/assessor-defense');
        }
    }
    function openList() {
        setSelectedReportId(undefined);
        setView('list');
    }
    const selected = listQuery.data?.find((item) => item.reportId === selectedReportId);
    const pdfFileId = detailQuery.data?.fileId || selected?.fileId;
    const pdfFileName = detailQuery.data?.fileName || selected?.fileName;
    const levelLanes = useMemo(() => {
        const items = listQuery.data || [];
        return DEFENSE_LEVEL_LANES.map((lane) => ({
            ...lane,
            items: items.filter((item) => defenseLevelLane(item.levelName) === lane.key),
        }));
    }, [listQuery.data]);
    async function handleOpenExternal() {
        if (!pdfFileId) {
            message.error('报告文件尚未加载完成');
            return;
        }
        try {
            const blob = await fetchPlatformFileBlob(pdfFileId);
            await openPdfExternally(blob, pdfFileName);
        }
        catch (error) {
            message.error(error instanceof Error ? error.message : '无法打开外部阅读器');
        }
    }
    return (_jsxs("div", { className: `defense-phone-shell${view === 'score' ? ' defense-phone-shell-score' : ''}`, children: [_jsxs("header", { className: "defense-phone-header", children: [view !== 'list' ? (_jsx(Button, { className: "defense-phone-back", icon: _jsx(LeftOutlined, {}), onClick: openList, children: "\u8FD4\u56DE\u5217\u8868" })) : null, _jsxs("div", { className: "defense-phone-title", children: [_jsx(Typography.Text, { type: "secondary", children: "\u8003\u8BC4\u5458\u7EFC\u5408\u7B54\u8FA9\u7AEF" }), _jsx(Typography.Title, { level: 4, children: currentUser?.displayName || currentUser?.username || '考评员' })] }), _jsxs("div", { className: "defense-phone-actions", children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => listQuery.refetch() }), _jsx(Button, { icon: _jsx(LogoutOutlined, {}), onClick: () => void handleLogout(), children: "\u9000\u51FA" })] })] }), view === 'score' && selectedReportId ? (_jsx(DefenseScorePanel, { reportId: selectedReportId })) : view === 'pdf' && selectedReportId ? (_jsxs("section", { className: "defense-phone-reader", children: [_jsxs("div", { className: "defense-phone-candidate", children: [_jsx(Typography.Title, { level: 3, children: detailQuery.data?.candidateName || selected?.candidateName || '考生' }), _jsx(Typography.Text, { type: "secondary", children: [detailQuery.data?.maskedIdCardNo || selected?.maskedIdCardNo, detailQuery.data?.workTypeName || selected?.workTypeName, detailQuery.data?.levelName || selected?.levelName]
                                    .filter(Boolean)
                                    .join(' · ') || '业绩评估报告' })] }), _jsx(DefensePdfViewer, { fileId: pdfFileId })] })) : (_jsx("section", { className: "defense-phone-list", children: listQuery.isLoading ? (_jsx(Spin, { className: "defense-phone-spin" })) : (_jsx("div", { className: "defense-phone-lanes", children: levelLanes.map((lane) => (_jsxs("section", { className: `defense-phone-lane defense-phone-lane-${lane.key}`, children: [_jsxs("div", { className: "defense-phone-lane-bar", role: "separator", children: [_jsx("span", { className: "defense-phone-lane-label", children: lane.label }), _jsxs("span", { className: "defense-phone-lane-count", children: [lane.items.length, "\u4EBA"] })] }), lane.items.length === 0 ? (_jsx("div", { className: "defense-phone-lane-empty", children: "\u672C\u7EA7\u522B\u6682\u65E0\u8003\u751F" })) : (_jsx(List, { dataSource: lane.items, renderItem: (item) => (_jsx(ReportListCard, { item: item, onOpenPdf: () => {
                                        setSelectedReportId(item.reportId);
                                        setView('pdf');
                                    }, onOpenScore: () => {
                                        setSelectedReportId(item.reportId);
                                        setView('score');
                                    } })) }))] }, lane.key))) })) })), view === 'pdf' ? (_jsxs("footer", { className: "defense-phone-footer", children: [_jsx(Button, { icon: _jsx(ExportOutlined, {}), onClick: () => void handleOpenExternal(), children: "\u5916\u90E8\u9605\u8BFB\u5668\u6253\u5F00" }), _jsx(Button, { type: "primary", onClick: () => setView('score'), children: "\u7B54\u8FA9\u8BC4\u5206" })] })) : null] }));
}
function ReportListCard({ item, onOpenPdf, onOpenScore, }) {
    return (_jsx(List.Item, { className: "defense-phone-card", extra: (_jsxs("div", { className: "defense-phone-card-actions", children: [_jsx(Button, { type: "primary", onClick: onOpenPdf, children: "\u8003\u524D\u9605\u5377" }), _jsx(Button, { onClick: onOpenScore, children: "\u7B54\u8FA9\u8BC4\u5206" })] })), children: _jsxs("div", { className: "defense-phone-card-main", children: [_jsx(Typography.Title, { level: 5, children: item.candidateName || '考生' }), _jsx(Typography.Text, { type: "secondary", children: item.admissionTicketNo || item.maskedIdCardNo || '-' }), _jsxs("div", { className: "defense-phone-meta", children: [item.workTypeName ? _jsx(Tag, { children: item.workTypeName }) : null, item.levelName ? _jsx(Tag, { color: defenseLevelLane(item.levelName) === 'senior_technician' ? 'volcano' : 'blue', children: item.levelName }) : null, _jsxs(Tag, { color: item.thesisStatus === 'submitted' ? 'green' : item.thesisStatus === 'draft' ? 'gold' : 'default', children: ["\u8BBA\u6587", statusLabel(item.thesisStatus), item.thesisStatus === 'submitted' ? ` ${formatScore(item.thesisFinalScore)}` : ''] }), _jsxs(Tag, { color: item.performanceStatus === 'submitted' ? 'green' : item.performanceStatus === 'draft' ? 'gold' : 'default', children: ["\u4E1A\u7EE9", statusLabel(item.performanceStatus), item.performanceStatus === 'submitted' ? ` ${formatScore(item.performanceFinalScore)}` : ''] })] }), _jsx(Typography.Text, { type: "secondary", children: item.fileName || '业绩评估报告.pdf' })] }) }));
}
