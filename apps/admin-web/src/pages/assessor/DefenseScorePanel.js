import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Spin, Tabs, Tag, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getDefenseScoreSheets, saveDefenseScoreSheet, submitDefenseScoreSheet, } from '../../api/defenseScoring';
import { fetchPlatformFileBlob } from '../../api/files';
import { DefensePerformanceScoreSheet, emptyPerformancePayload } from './DefensePerformanceScoreSheet';
import { DefenseThesisScoreSheet, emptyThesisPayload } from './DefenseThesisScoreSheet';
import { formatScore, performanceTotal, statusLabel, thesisFinalScore } from './defenseScoreCatalog';
function signatureFromSheet(sheet) {
    if (!sheet?.signatureFileId) {
        return null;
    }
    return { fileId: String(sheet.signatureFileId), hash: sheet.signatureHash || '' };
}
function isSubmitted(status) {
    return status === 'submitted';
}
export function DefenseScorePanel({ reportId }) {
    const queryClient = useQueryClient();
    const [activeTab, setActiveTab] = useState('THESIS');
    const [thesis, setThesis] = useState(emptyThesisPayload());
    const [performance, setPerformance] = useState(emptyPerformancePayload());
    const [thesisSign, setThesisSign] = useState(null);
    const [performanceSign, setPerformanceSign] = useState(null);
    const thesisSignRef = useRef(null);
    const performanceSignRef = useRef(null);
    const sheetsQuery = useQuery({
        queryKey: ['defense-score-sheets', reportId],
        queryFn: () => getDefenseScoreSheets(reportId),
    });
    useEffect(() => {
        const data = sheetsQuery.data;
        if (!data)
            return;
        setThesis({ ...emptyThesisPayload(), ...(data.thesis.thesis || {}) });
        setPerformance({ ...emptyPerformancePayload(), ...(data.performance.performance || {}) });
        setThesisSign(signatureFromSheet(data.thesis));
        setPerformanceSign(signatureFromSheet(data.performance));
    }, [sheetsQuery.data]);
    const thesisLocked = isSubmitted(sheetsQuery.data?.thesis.status);
    const performanceLocked = isSubmitted(sheetsQuery.data?.performance.status);
    const currentLocked = activeTab === 'THESIS' ? thesisLocked : performanceLocked;
    const currentSheet = activeTab === 'THESIS' ? sheetsQuery.data?.thesis : sheetsQuery.data?.performance;
    const currentSign = activeTab === 'THESIS' ? thesisSign : performanceSign;
    const currentSignRef = activeTab === 'THESIS' ? thesisSignRef : performanceSignRef;
    const candidate = sheetsQuery.data?.candidate;
    const previewQuery = useQuery({
        queryKey: ['defense-sign-preview', currentSign?.fileId],
        queryFn: async () => {
            const blob = await fetchPlatformFileBlob(currentSign.fileId);
            return URL.createObjectURL(blob);
        },
        enabled: Boolean(currentSign?.fileId),
    });
    const saveMutation = useMutation({
        mutationFn: async () => {
            if (activeTab === 'THESIS') {
                return saveDefenseScoreSheet(reportId, 'THESIS', {
                    thesis,
                    signatureFileId: thesisSign?.fileId,
                    signatureHash: thesisSign?.hash,
                });
            }
            return saveDefenseScoreSheet(reportId, 'PERFORMANCE', {
                performance,
                signatureFileId: performanceSign?.fileId,
                signatureHash: performanceSign?.hash,
            });
        },
        onSuccess: async () => {
            message.success('草稿已保存');
            await queryClient.invalidateQueries({ queryKey: ['defense-score-sheets', reportId] });
            await queryClient.invalidateQueries({ queryKey: ['my-defense-reports'] });
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '保存失败');
        },
    });
    const submitMutation = useMutation({
        mutationFn: async () => {
            const signed = await currentSignRef.current?.ensureSignature();
            if (!signed?.fileId || !signed.hash) {
                throw new Error('请先完成评审专家电子签名');
            }
            if (activeTab === 'THESIS') {
                return submitDefenseScoreSheet(reportId, 'THESIS', {
                    thesis,
                    signatureFileId: signed.fileId,
                    signatureHash: signed.hash,
                });
            }
            return submitDefenseScoreSheet(reportId, 'PERFORMANCE', {
                performance,
                signatureFileId: signed.fileId,
                signatureHash: signed.hash,
            });
        },
        onSuccess: async () => {
            message.success('评分已提交');
            await queryClient.invalidateQueries({ queryKey: ['defense-score-sheets', reportId] });
            await queryClient.invalidateQueries({ queryKey: ['my-defense-reports'] });
        },
        onError: (error) => {
            message.error(error instanceof Error ? error.message : '提交失败');
        },
    });
    const tabItems = useMemo(() => ([
        {
            key: 'THESIS',
            label: (_jsxs("span", { children: ["\u8BBA\u6587\u7B54\u8FA9", _jsx(Tag, { className: "defense-score-tab-tag", color: thesisLocked ? 'green' : sheetsQuery.data?.thesis.status === 'draft' ? 'gold' : 'default', children: statusLabel(sheetsQuery.data?.thesis.status) })] })),
        },
        {
            key: 'PERFORMANCE',
            label: (_jsxs("span", { children: ["\u4E1A\u7EE9\u8BC4\u4F30", _jsx(Tag, { className: "defense-score-tab-tag", color: performanceLocked ? 'green' : sheetsQuery.data?.performance.status === 'draft' ? 'gold' : 'default', children: statusLabel(sheetsQuery.data?.performance.status) })] })),
        },
    ]), [performanceLocked, sheetsQuery.data, thesisLocked]);
    if (sheetsQuery.isLoading) {
        return _jsx(Spin, { className: "defense-phone-spin" });
    }
    return (_jsxs("section", { className: "defense-score-page", children: [_jsxs("div", { className: "defense-score-toolbar", children: [_jsx("div", { className: "defense-phone-candidate", children: _jsx(Typography.Title, { level: 3, children: candidate?.candidateName || '考生' }) }), _jsx(Tabs, { activeKey: activeTab, onChange: (key) => setActiveTab(key), items: tabItems })] }), _jsx("div", { className: "defense-score-body", children: activeTab === 'THESIS' ? (_jsx(DefenseThesisScoreSheet, { candidate: candidate, assessorName: currentSheet?.assessorName, taskId: candidate?.taskId, value: thesis, disabled: thesisLocked, signature: thesisSign, signaturePreviewUrl: previewQuery.data, signatureRef: thesisSignRef, onChange: setThesis, onSignatureChange: setThesisSign })) : (_jsx(DefensePerformanceScoreSheet, { candidate: candidate, assessorName: currentSheet?.assessorName, taskId: candidate?.taskId, value: performance, disabled: performanceLocked, signature: performanceSign, signaturePreviewUrl: previewQuery.data, signatureRef: performanceSignRef, onChange: setPerformance, onSignatureChange: setPerformanceSign })) }), _jsxs("footer", { className: "defense-score-actions", children: [_jsxs("div", { className: "defense-score-actions-score", children: [_jsx("span", { children: activeTab === 'THESIS' ? '最终得分' : '合计' }), _jsx("strong", { children: formatScore(activeTab === 'THESIS' ? thesisFinalScore(thesis.contentScores, thesis.defenseScores) : performanceTotal(performance.items)) })] }), _jsx(Button, { disabled: currentLocked, loading: saveMutation.isPending, onClick: () => saveMutation.mutate(), children: "\u4FDD\u5B58\u8349\u7A3F" }), _jsx(Button, { type: "primary", disabled: currentLocked, loading: submitMutation.isPending, onClick: () => submitMutation.mutate(), children: "\u63D0\u4EA4\u8BC4\u5206" })] })] }));
}
