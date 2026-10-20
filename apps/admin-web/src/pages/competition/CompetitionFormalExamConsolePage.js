import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined, SettingOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Empty, Result, Select, Space, Spin, Tag, Typography, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { getCompetitionFormalExamSessionByStage, pageCompetitionStages, prepareCompetitionFormalExamSession, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import { CompetitionFormalExamSessionPanel } from './CompetitionFormalExamSessionPanel';
function idValue(value) {
    return value == null ? '' : String(value);
}
function errorText(error, fallback) {
    return error instanceof Error && error.message.trim() ? error.message : fallback;
}
function stageLabel(stage) {
    return [stage.name, stage.eventWorkTypeLevelCode ? `等级${stage.eventWorkTypeLevelCode}` : undefined]
        .filter(Boolean)
        .join(' · ');
}
/** Standalone entry for the theory formal-exam runtime console. */
export function CompetitionFormalExamConsolePage({ eventId, eventWorkTypeId, eventWorkTypeLevelCode, initialStageId, }) {
    const canView = useAuthStore((state) => state.permissionCodes.includes('competition:exam-session:view'));
    const canPrepare = useAuthStore((state) => state.permissionCodes.includes('competition:exam-session:prepare'));
    const queryClient = useQueryClient();
    const [selectedStageId, setSelectedStageId] = useState(initialStageId);
    const scopeReady = Boolean(eventWorkTypeId && eventWorkTypeLevelCode?.trim());
    const stagesQuery = useQuery({
        queryKey: [
            'competition-formal-exam-console-stages',
            eventId,
            eventWorkTypeId ?? 'unset',
            eventWorkTypeLevelCode ?? 'unset',
        ],
        queryFn: () => pageCompetitionStages(eventId, {
            page: 1,
            pageSize: 500,
            eventWorkTypeId,
            eventWorkTypeLevelCode,
        }),
        enabled: canView && scopeReady,
    });
    const stages = stagesQuery.data?.items || [];
    const selectedStage = useMemo(() => stages.find((stage) => idValue(stage.id) === selectedStageId), [selectedStageId, stages]);
    useEffect(() => {
        if (!stages.length) {
            if (selectedStageId)
                setSelectedStageId(undefined);
            return;
        }
        if (!selectedStageId || !stages.some((stage) => idValue(stage.id) === selectedStageId)) {
            const requestedStage = initialStageId && stages.some((stage) => idValue(stage.id) === initialStageId)
                ? initialStageId
                : undefined;
            setSelectedStageId(requestedStage || idValue(stages[0].id));
        }
    }, [initialStageId, selectedStageId, stages]);
    const sessionQuery = useQuery({
        queryKey: ['competition-formal-exam-console-session', eventId, selectedStageId],
        queryFn: () => getCompetitionFormalExamSessionByStage(selectedStageId),
        enabled: canView && scopeReady && Boolean(selectedStageId),
    });
    const selectedSession = sessionQuery.data
        && idValue(sessionQuery.data.stageId) === selectedStageId
        ? sessionQuery.data
        : undefined;
    const prepareMutation = useMutation({
        mutationFn: () => prepareCompetitionFormalExamSession(selectedStageId),
        onSuccess: (session) => {
            queryClient.setQueryData(['competition-formal-exam-console-session', eventId, selectedStageId], session);
            message.success('正式比赛运行实例已初始化');
        },
        onError: (error) => message.error(errorText(error, '初始化正式比赛失败，请先完成选手试卷配置')),
    });
    if (!canView) {
        return _jsx(Result, { status: "403", title: "\u6682\u65E0\u7406\u8BBA\u6BD4\u8D5B\u63A7\u5236\u53F0\u67E5\u770B\u6743\u9650" });
    }
    if (stagesQuery.isError) {
        return _jsx(Result, { status: "error", title: "\u8D5B\u6BB5\u5217\u8868\u52A0\u8F7D\u5931\u8D25", extra: _jsx(Button, { onClick: () => stagesQuery.refetch(), children: "\u91CD\u65B0\u52A0\u8F7D" }) });
    }
    return (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%', marginTop: 16 }, children: [_jsx(Card, { size: "small", bordered: false, children: _jsxs(Space, { wrap: true, align: "center", style: { width: '100%' }, children: [_jsx(SettingOutlined, {}), _jsx(Typography.Text, { type: "secondary", children: "\u9009\u62E9\u8D5B\u6BB5" }), _jsx(Select, { showSearch: true, optionFilterProp: "label", value: selectedStageId, loading: stagesQuery.isFetching, disabled: !scopeReady || (!stagesQuery.isFetching && stages.length === 0), placeholder: scopeReady ? '请选择需要控制的理论考试赛段' : '请先选择工种和技能等级', style: { width: 320, maxWidth: '100%' }, options: stages.map((stage) => ({ value: idValue(stage.id), label: stageLabel(stage) })), onChange: setSelectedStageId }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: stagesQuery.isFetching, onClick: () => stagesQuery.refetch(), children: "\u5237\u65B0\u8D5B\u6BB5" }), selectedStage ? (_jsxs(Space, { size: 8, children: [_jsxs(Typography.Text, { type: "secondary", children: ["\u5F53\u524D\u63A7\u5236\u8303\u56F4\uFF1A", stageLabel(selectedStage)] }), sessionQuery.isFetching && !selectedSession ? _jsx(Tag, { children: "\u6B63\u5728\u8BFB\u53D6\u6BD4\u8D5B\u72B6\u6001" }) : null, !sessionQuery.isFetching && selectedSession ? _jsx(Tag, { color: "success", children: "\u5DF2\u521D\u59CB\u5316" }) : null, !sessionQuery.isFetching && !selectedSession && !sessionQuery.isError ? _jsx(Tag, { color: "warning", children: "\u672A\u521D\u59CB\u5316" }) : null] })) : null] }) }), !scopeReady ? (_jsx(Card, { bordered: false, children: _jsx(Empty, { description: "\u8BF7\u5148\u9009\u62E9\u5DE5\u79CD\u548C\u6280\u80FD\u7B49\u7EA7\u3002\u6B63\u5F0F\u6BD4\u8D5B\u53EA\u8BFB\u53D6\u8BE5\u5DE5\u79CD+\u7B49\u7EA7\u4E0B\u7684\u8D5B\u6BB5\uFF0C\u53EA\u5237\u65B0\u8D5B\u4E8B\u5217\u8868\u4E0D\u4F1A\u5E26\u51FA\u5DF2\u914D\u7F6E\u7684\u8D5B\u6BB5\u3002" }) })) : null, scopeReady && !stagesQuery.isFetching && stages.length === 0 ? (_jsx(Card, { bordered: false, children: _jsx(Empty, { description: "\u5F53\u524D\u5DE5\u79CD\u7B49\u7EA7\u4E0B\u6682\u65E0\u53EF\u63A7\u5236\u7684\u8D5B\u6BB5\u3002\u8BF7\u786E\u8BA4\u5DF2\u5728\u300C\u8D5B\u6BB5\u7F16\u6392\u300D\u914D\u7F6E\u8BE5\u5DE5\u79CD\u7B49\u7EA7\u7684\u8D5B\u6BB5\uFF0C\u518D\u70B9\u300C\u5237\u65B0\u8D5B\u6BB5\u300D\u3002" }) })) : null, selectedStageId && (sessionQuery.isLoading || (sessionQuery.isFetching && !selectedSession)) ? (_jsx(Card, { bordered: false, children: _jsx(Spin, {}) })) : null, sessionQuery.isError ? (_jsx(Alert, { type: "error", showIcon: true, message: errorText(sessionQuery.error, '正式比赛运行实例加载失败') })) : null, selectedStage && !sessionQuery.isLoading && !sessionQuery.isFetching && !sessionQuery.isError && !selectedSession ? (_jsx(Card, { bordered: false, "data-testid": "formal-exam-console-uninitialized", title: _jsxs(Space, { children: [_jsx("span", { children: "\u6B63\u5F0F\u6BD4\u8D5B\u8FD0\u884C\u5B9E\u4F8B" }), _jsx(Tag, { color: "warning", children: "\u672A\u521D\u59CB\u5316" })] }), children: _jsxs(Space, { direction: "vertical", size: 12, children: [_jsx(Typography.Text, { type: "secondary", children: "\u5F53\u524D\u8D5B\u6BB5\u5C1A\u672A\u751F\u6210\u7406\u8BBA\u8003\u8BD5\u6B63\u5F0F\u6BD4\u8D5B\u8FD0\u884C\u5B9E\u4F8B\u3002\u8BF7\u786E\u8BA4\u9009\u624B\u8BD5\u5377\u914D\u7F6E\u5DF2\u5B8C\u6210\uFF0C\u518D\u4ECE\u672C\u63A7\u5236\u53F0\u521D\u59CB\u5316\u3002" }), canPrepare ? (_jsx(Button, { type: "primary", loading: prepareMutation.isPending, onClick: () => prepareMutation.mutate(), children: "\u521D\u59CB\u5316\u6B63\u5F0F\u6BD4\u8D5B" })) : (_jsx(Typography.Text, { type: "warning", children: "\u5F53\u524D\u8D26\u53F7\u6CA1\u6709\u521D\u59CB\u5316\u6B63\u5F0F\u6BD4\u8D5B\u7684\u6743\u9650\u3002" }))] }) })) : null, selectedSession ? (_jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, "data-testid": "formal-exam-console-initialized", children: [_jsx(Alert, { type: "success", showIcon: true, message: "\u6B63\u5F0F\u6BD4\u8D5B\u5DF2\u521D\u59CB\u5316", description: "\u5F53\u524D\u9875\u9762\u4FDD\u7559\u5E76\u5C55\u793A\u8BE5\u6BD4\u8D5B\u7684\u63A7\u5236\u53F0\u6570\u636E\u9762\u677F\uFF0C\u53EF\u7EE7\u7EED\u67E5\u770B\u5B9E\u65F6\u72B6\u6001\u548C\u6267\u884C\u63A7\u5236\u3002" }), _jsx(CompetitionFormalExamSessionPanel, { open: true, embedded: true, loading: false, session: selectedSession, onClose: () => undefined }, idValue(selectedSession.id))] })) : null] }));
}
