import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Alert, Button, Card, Descriptions, Input, InputNumber, Select, Space, Spin, Table, Tabs, Tag, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { confirmCompetitionFinalPodium, confirmCompetitionPromotionReference, getCompetitionFinalPodium, getCompetitionPromotionReference, getCompetitionStageSettlement, listCompetitionEventWorkTypes, listCompetitionExecutionPlans, listCompetitionStageSettlements, saveCompetitionFinalPodium, generateCompetitionPromotionReference, } from '../../../api/competition';
import { useAuthStore } from '../../../stores/authStore';
export function StageSettlementPromotionPanel({ eventId, stage }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = permissions.includes('competition:settlement:view');
    const canManage = permissions.includes('competition:settlement:manage');
    const canPublish = permissions.includes('competition:settlement:publish');
    const queryClient = useQueryClient();
    const workTypesQuery = useQuery({
        queryKey: ['competition-event-work-types', eventId],
        queryFn: () => listCompetitionEventWorkTypes(eventId),
        enabled: canView,
    });
    const workTypes = workTypesQuery.data ?? [];
    const [workTypeId, setWorkTypeId] = useState();
    const [planId, setPlanId] = useState();
    const [settlementId, setSettlementId] = useState();
    useEffect(() => {
        if (!workTypeId && workTypes[0])
            setWorkTypeId(workTypes[0].id);
    }, [workTypeId, workTypes]);
    const plansQuery = useQuery({
        queryKey: ['competition-stage-execution-plans', stage.id, workTypeId],
        queryFn: () => listCompetitionExecutionPlans(stage.id, workTypeId),
        enabled: canView && Boolean(workTypeId),
    });
    const plans = plansQuery.data ?? [];
    useEffect(() => {
        const next = plans.find((plan) => plan.status === 'published') ?? plans[0];
        if (next && String(next.id) !== String(planId))
            setPlanId(next.id);
        if (!next)
            setPlanId(undefined);
    }, [planId, plans]);
    const settlementsQuery = useQuery({
        queryKey: ['competition-stage-settlements', planId],
        queryFn: () => listCompetitionStageSettlements(planId),
        enabled: canView && Boolean(planId),
    });
    const settlements = settlementsQuery.data ?? [];
    useEffect(() => {
        const next = settlements[0];
        if (next && String(next.id) !== String(settlementId))
            setSettlementId(next.id);
        if (!next)
            setSettlementId(undefined);
    }, [settlementId, settlements]);
    const settlementQuery = useQuery({
        queryKey: ['competition-stage-settlement', settlementId],
        queryFn: () => getCompetitionStageSettlement(settlementId),
        enabled: canView && Boolean(settlementId),
    });
    const settlement = settlementQuery.data;
    const referenceQuery = useQuery({
        queryKey: ['competition-promotion-reference', settlementId],
        queryFn: () => getCompetitionPromotionReference(settlementId),
        enabled: canView && Boolean(settlementId),
    });
    const podiumQuery = useQuery({
        queryKey: ['competition-final-podium', settlementId],
        queryFn: () => getCompetitionFinalPodium(settlementId),
        enabled: canView && Boolean(settlementId),
    });
    const [podiumDraft, setPodiumDraft] = useState({});
    useEffect(() => {
        if (podiumQuery.data)
            setPodiumDraft(podiumQuery.data);
    }, [podiumQuery.data]);
    const refreshSettlement = async () => {
        await Promise.all([
            queryClient.invalidateQueries({ queryKey: ['competition-stage-settlements', planId] }),
            queryClient.invalidateQueries({ queryKey: ['competition-stage-settlement', settlementId] }),
            queryClient.invalidateQueries({ queryKey: ['competition-promotion-reference', settlementId] }),
            queryClient.invalidateQueries({ queryKey: ['competition-final-podium', settlementId] }),
        ]);
    };
    const generateReferenceMutation = useMutation({
        mutationFn: () => generateCompetitionPromotionReference(settlementId),
        onSuccess: refreshSettlement,
        onError: (error) => message.error(error instanceof Error ? error.message : '参考名单生成失败'),
    });
    const confirmReferenceMutation = useMutation({
        mutationFn: () => confirmCompetitionPromotionReference(settlementId),
        onSuccess: refreshSettlement,
        onError: (error) => message.error(error instanceof Error ? error.message : '参考名单确认失败'),
    });
    const savePodiumMutation = useMutation({
        mutationFn: () => saveCompetitionFinalPodium(settlementId, {
            firstContestantMemberId: podiumDraft.firstContestantMemberId ?? null,
            secondContestantMemberId: podiumDraft.secondContestantMemberId ?? null,
            thirdContestantMemberId: podiumDraft.thirdContestantMemberId ?? null,
            specialExplanation: podiumDraft.specialExplanation ?? null,
        }),
        onSuccess: refreshSettlement,
        onError: (error) => message.error(error instanceof Error ? error.message : '前三名保存失败'),
    });
    const confirmPodiumMutation = useMutation({
        mutationFn: () => confirmCompetitionFinalPodium(settlementId),
        onSuccess: refreshSettlement,
        onError: (error) => message.error(error instanceof Error ? error.message : '前三名确认失败'),
    });
    const reference = referenceQuery.data;
    const podiumEditable = canManage && settlement?.status === 'draft' && (!podiumDraft.status || podiumDraft.status === 'draft');
    const referenceRows = useMemo(() => reference?.items ?? [], [reference]);
    if (!canView || workTypesQuery.isError)
        return null;
    if (workTypesQuery.isLoading)
        return _jsx(Card, { size: "small", style: { marginTop: 16 }, children: _jsx(Spin, {}) });
    return (_jsxs(Card, { size: "small", title: "\u664B\u7EA7\u786E\u8BA4\u5DE5\u4F5C\u53F0", style: { marginTop: 16 }, children: [_jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Select, { "aria-label": "\u7ED3\u7B97\u5DE5\u79CD", value: workTypeId, placeholder: "\u9009\u62E9\u5DE5\u79CD", style: { width: 220 }, options: workTypes.map((item) => ({ value: item.id, label: item.displayName || item.workTypeName || String(item.id) })), onChange: setWorkTypeId }), _jsx(Select, { "aria-label": "\u6267\u884C\u65B9\u6848", value: planId, placeholder: "\u9009\u62E9\u6267\u884C\u65B9\u6848", style: { width: 220 }, loading: plansQuery.isFetching, options: plans.map((item) => ({ value: item.id, label: `方案 V${item.planVersion} · ${item.status}` })), onChange: setPlanId }), _jsx(Select, { "aria-label": "\u7ED3\u7B97\u5FEB\u7167", value: settlementId, placeholder: "\u9009\u62E9\u7ED3\u7B97\u5FEB\u7167", style: { width: 220 }, loading: settlementsQuery.isFetching, options: settlements.map((item) => ({ value: item.id, label: `结算 V${item.settlementVersion} · ${item.status}` })), onChange: setSettlementId })] }), !settlement ? (_jsx(Alert, { type: "info", message: "\u5F53\u524D\u5DE5\u79CD\u548C\u6267\u884C\u65B9\u6848\u6682\u65E0\u7ED3\u7B97\u5FEB\u7167" })) : (_jsx(Tabs, { items: [
                    {
                        key: 'reference',
                        label: '参考名单确认',
                        children: (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsxs(Descriptions, { size: "small", column: 3, bordered: true, children: [_jsx(Descriptions.Item, { label: "\u89C4\u5219", children: settlement.promotionRuleType || 'LEGACY' }), _jsx(Descriptions.Item, { label: "\u664B\u7EA7\u767E\u5206\u6BD4", children: settlement.promotionPercentage == null ? '-' : `${settlement.promotionPercentage}%` }), _jsx(Descriptions.Item, { label: "\u8BA1\u7B97\u76EE\u6807\u4EBA\u6570", children: reference?.calculatedTargetCount ?? settlement.calculatedTargetCount ?? '-' })] }), !reference ? (_jsx(Button, { type: "primary", disabled: !canManage || settlement.status !== 'draft', loading: generateReferenceMutation.isPending, onClick: () => generateReferenceMutation.mutate(), children: "\u751F\u6210\u53C2\u8003\u540D\u5355" })) : (_jsxs(_Fragment, { children: [_jsxs(Space, { children: [_jsx(Tag, { color: reference.status === 'confirmed' ? 'success' : 'processing', children: reference.status }), _jsxs("span", { children: ["\u57FA\u6570 ", reference.baseCount, "\uFF0C\u76EE\u6807\u4EBA\u6570 ", reference.calculatedTargetCount] }), _jsx(Button, { disabled: !canPublish || reference.status !== 'draft', loading: confirmReferenceMutation.isPending, onClick: () => confirmReferenceMutation.mutate(), children: "\u786E\u8BA4\u5E76\u9501\u5B9A" })] }), _jsx(Table, { size: "small", rowKey: "id", pagination: { pageSize: 8, showSizeChanger: false }, dataSource: referenceRows, columns: [
                                                { title: '选手', dataIndex: 'contestantMemberId' },
                                                { title: '来源排名', dataIndex: 'sourceRank' },
                                                { title: '选择状态', dataIndex: 'selectionStatus', render: (value) => _jsx(Tag, { color: value === 'selected' ? 'success' : 'default', children: value }) },
                                                { title: '计算说明', dataIndex: 'reason' },
                                            ] })] }))] })),
                    },
                    {
                        key: 'podium',
                        label: '决赛前三名确认',
                        children: (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsx(Alert, { type: "info", message: "\u4EC5\u6700\u7EC8\u8D5B\u6BB5\u4F7F\u7528\uFF1B\u6709\u6548\u9009\u624B\u4E0D\u8DB3 3 \u4EBA\u65F6\u5FC5\u987B\u586B\u5199\u7279\u6B8A\u8BF4\u660E\u3002" }), _jsx(Space, { wrap: true, children: [['第一名', 'firstContestantMemberId'], ['第二名', 'secondContestantMemberId'], ['第三名', 'thirdContestantMemberId']].map(([label, key]) => (_jsx(InputNumber, { "aria-label": label, placeholder: label, min: 1, disabled: !podiumEditable, value: podiumDraft[key], onChange: (value) => setPodiumDraft((current) => ({ ...current, [key]: value ?? null })) }, key))) }), _jsx(Input.TextArea, { "aria-label": "\u524D\u4E09\u540D\u7279\u6B8A\u8BF4\u660E", placeholder: "\u6709\u6548\u9009\u624B\u4E0D\u8DB3 3 \u4EBA\u65F6\u586B\u5199\u7279\u6B8A\u8BF4\u660E", disabled: !podiumEditable, value: podiumDraft.specialExplanation ?? '', onChange: (event) => setPodiumDraft((current) => ({ ...current, specialExplanation: event.target.value })), rows: 3 }), _jsxs(Space, { children: [_jsx(Button, { type: "primary", disabled: !podiumEditable, loading: savePodiumMutation.isPending, onClick: () => savePodiumMutation.mutate(), children: "\u4FDD\u5B58\u524D\u4E09\u540D" }), _jsx(Button, { disabled: !canPublish || !podiumDraft.id || podiumDraft.status !== 'draft', loading: confirmPodiumMutation.isPending, onClick: () => confirmPodiumMutation.mutate(), children: "\u786E\u8BA4\u5E76\u9501\u5B9A" }), podiumDraft.status && _jsx(Tag, { color: podiumDraft.status === 'confirmed' ? 'success' : 'processing', children: podiumDraft.status })] })] })),
                    },
                ] }))] }));
}
