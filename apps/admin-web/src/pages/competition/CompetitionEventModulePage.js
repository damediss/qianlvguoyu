import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Result, Select, Space, Spin, Typography } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents, } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import { useNavigationBlocker } from '../../hooks/useUnsavedChangesBlocker';
import './CompetitionEventManagementPage.css';
import { CompetitionNoticePage } from './CompetitionNoticePage';
import { CompetitionFormalExamConsolePage } from './CompetitionFormalExamConsolePage';
import { CompetitionPracticalConsolePage } from './CompetitionPracticalConsolePage';
import { CompetitionScoreRankingPage } from './CompetitionScoreRankingPage';
import { CompetitionStagePlanningPage } from './CompetitionStagePlanningPage';
import { CompetitionTheoryExamPage } from './CompetitionTheoryExamPage';
const STANDARD_COMPETITION_WORK_TYPE_LEVELS = [
    { value: '5', label: '\u521d\u7ea7\u5de5' },
    { value: '4', label: '\u4e2d\u7ea7\u5de5' },
    { value: '3', label: '\u9ad8\u7ea7\u5de5' },
    { value: '2', label: '\u6280\u5e08' },
    { value: '1', label: '\u9ad8\u7ea7\u6280\u5e08' },
];
const moduleMeta = {
    stages: {
        title: '赛段编排',
        description: '选择赛事和工种后，按初赛、复赛、半决赛、决赛等晋级流程编排赛段、时间和晋级规则。',
        permission: ['competition:stage:view'],
        eventLabel: (event) => `${event.code} · ${event.name}`,
    },
    notices: {
        title: '公告设置',
        description: '选择赛事后，创建、编辑并发布赛事公告。',
        permission: ['competition:notice:view'],
        eventLabel: (event) => `${event.code} · ${event.name}`,
    },
    'theory-exam': {
        title: '理论考试',
        description: '选择赛事后，配置理论试卷、考场、排考和考试监控。',
        permission: ['competition:theory-paper:view', 'competition:theory-schedule:view', 'competition:theory-monitor:view', 'competition:theory-result:view'],
        eventLabel: (event) => `${event.code} · ${event.name}`,
    },
    'formal-exam-console': {
        title: '理论考试正式比赛控制台',
        description: '选择赛事、工种和技能等级后，进入对应赛段的理论考试正式比赛运行控制台，初始化运行实例并执行开考、监考和成绩处理。',
        permission: ['competition:exam-session:view'],
        eventLabel: (event) => `${event.name} · ${event.code}`,
    },
    'practical-exam-console': {
        title: '实操比赛控制台',
        description: '按赛事工种、技能等级和赛段初始化正式实操比赛，将三名裁判、选手和考评表冻结为评分任务。',
        permission: ['competition:practical-console:view'],
        eventLabel: (event) => `${event.code} · ${event.name}`,
    },
    scores: {
        title: '成绩与排名',
        description: '选择赛事后，汇总成绩并生成赛事排名。',
        permission: ['competition:score:view', 'competition:ranking:view'],
        eventLabel: (event) => `${event.code} · ${event.name}`,
    },
};
export function CompetitionEventModulePage({ module, onNavigate, initialEventId, initialStageId, initialEventWorkTypeId, }) {
    const meta = moduleMeta[module];
    const { confirmNavigation } = useNavigationBlocker();
    const queryClient = useQueryClient();
    const permissions = useAuthStore((state) => state.permissionCodes);
    const canView = meta.permission.some((permission) => permissions.includes(permission));
    const needsWorkTypeScope = module === 'stages' || module === 'formal-exam-console'
        || module === 'practical-exam-console';
    const eventsQuery = useQuery({
        queryKey: ['competition-events', 'module-selector'],
        queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 100 }),
        enabled: canView,
    });
    const events = eventsQuery.data?.items || [];
    const [selectedEventId, setSelectedEventId] = useState(initialEventId);
    useEffect(() => {
        if (events.length === 0)
            return;
        if (!selectedEventId || !events.some((event) => String(event.id) === selectedEventId)) {
            setSelectedEventId(String(events[0].id));
        }
    }, [events, selectedEventId]);
    const selectedEvent = useMemo(() => events.find((event) => String(event.id) === selectedEventId), [events, selectedEventId]);
    const eventWorkTypesQuery = useQuery({
        queryKey: ['competition-event-work-types', selectedEventId, 'stage-planning-selector'],
        queryFn: () => listCompetitionEventWorkTypes(selectedEventId),
        enabled: needsWorkTypeScope && Boolean(selectedEventId),
    });
    const activeEventWorkTypes = useMemo(() => (eventWorkTypesQuery.data || []).filter((item) => item.status === 'active'), [eventWorkTypesQuery.data]);
    const [selectedEventWorkTypeId, setSelectedEventWorkTypeId] = useState(initialEventWorkTypeId);
    const [selectedEventWorkTypeLevelCode, setSelectedEventWorkTypeLevelCode] = useState();
    useEffect(() => {
        if (!activeEventWorkTypes.length) {
            setSelectedEventWorkTypeId(undefined);
            setSelectedEventWorkTypeLevelCode(undefined);
            return;
        }
        if (!selectedEventWorkTypeId || !activeEventWorkTypes.some((item) => String(item.id) === selectedEventWorkTypeId)) {
            setSelectedEventWorkTypeId(String(activeEventWorkTypes[0].id));
            setSelectedEventWorkTypeLevelCode(undefined);
        }
    }, [activeEventWorkTypes, selectedEventWorkTypeId]);
    // 渲染期派生有效工种：跨赛事切换/URL 参数混合时，未在当前赛事工种列表中的 id 在本帧即钳为 undefined，
    // 查询与子组件不会以混合状态发出请求（effect 事后纠偏存在一帧竞态，会打出 404/422/400 控制台噪音）
    const effectiveEventWorkTypeId = activeEventWorkTypes.some((item) => String(item.id) === selectedEventWorkTypeId) ? selectedEventWorkTypeId : undefined;
    const eventWorkTypeLevelsQuery = useQuery({
        queryKey: [
            'competition-event-work-type-levels',
            selectedEventId,
            effectiveEventWorkTypeId,
            'stage-planning-selector',
        ],
        queryFn: () => listCompetitionEventWorkTypeLevels(selectedEventId, effectiveEventWorkTypeId),
        enabled: needsWorkTypeScope && Boolean(selectedEventId && effectiveEventWorkTypeId),
    });
    const levelOptions = useMemo(() => {
        const configuredLevels = (eventWorkTypeLevelsQuery.data || [])
            .filter((level) => level && STANDARD_COMPETITION_WORK_TYPE_LEVELS.some((item) => item.value === level.levelCode))
            .sort((left, right) => Number(right.levelCode) - Number(left.levelCode));
        return configuredLevels.map((level) => ({
            value: level.levelCode,
            label: level.levelName || STANDARD_COMPETITION_WORK_TYPE_LEVELS.find((item) => item.value === level.levelCode)?.label || level.levelCode,
        }));
    }, [eventWorkTypeLevelsQuery.data]);
    useEffect(() => {
        if (!levelOptions.length)
            return;
        if (!selectedEventWorkTypeLevelCode
            || !levelOptions.some((level) => level.value === selectedEventWorkTypeLevelCode)) {
            setSelectedEventWorkTypeLevelCode(levelOptions[0].value);
        }
    }, [levelOptions, selectedEventWorkTypeLevelCode]);
    // 等级同理：工种切换后旧等级在本帧即失效，子组件（赛段编排/控制台）不会收到混合范围
    const effectiveEventWorkTypeLevelCode = levelOptions.some((level) => level.value === selectedEventWorkTypeLevelCode) ? selectedEventWorkTypeLevelCode : undefined;
    const scopeControls = [
        _jsx(Typography.Text, { type: "secondary", children: "\u9009\u62E9\u8D5B\u4E8B" }, "event-label"),
        _jsx(Select, { showSearch: true, optionFilterProp: "label", value: selectedEventId, loading: eventsQuery.isFetching, disabled: !eventsQuery.isFetching && events.length === 0, placeholder: "\u8BF7\u9009\u62E9\u8D5B\u4E8B", style: { width: 280, maxWidth: '100%' }, options: events.map((event) => ({
                value: String(event.id),
                label: meta.eventLabel(event),
            })), onChange: async (eventId) => {
                if (!(await confirmNavigation()))
                    return;
                setSelectedEventWorkTypeId(undefined);
                setSelectedEventWorkTypeLevelCode(undefined);
                setSelectedEventId(eventId || undefined);
            } }, "event-select"),
        ...(needsWorkTypeScope ? [
            _jsx(Typography.Text, { type: "secondary", children: "\u9009\u62E9\u5DE5\u79CD" }, "work-type-label"),
            _jsx(Select, { showSearch: true, optionFilterProp: "label", value: effectiveEventWorkTypeId, loading: eventWorkTypesQuery.isFetching, disabled: !selectedEventId || (!eventWorkTypesQuery.isFetching && activeEventWorkTypes.length === 0), placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD", style: { width: 160, maxWidth: '100%' }, options: activeEventWorkTypes.map((workType) => ({
                    value: String(workType.id),
                    label: workType.displayName || workType.workTypeName || String(workType.id),
                })), onChange: async (workTypeId) => {
                    if (!(await confirmNavigation()))
                        return;
                    setSelectedEventWorkTypeLevelCode(undefined);
                    setSelectedEventWorkTypeId(workTypeId || undefined);
                } }, "work-type-select"),
            _jsx(Typography.Text, { type: "secondary", children: "\u9009\u62E9\u7B49\u7EA7" }, "level-label"),
            _jsx(Select, { showSearch: true, optionFilterProp: "label", value: effectiveEventWorkTypeLevelCode, disabled: !effectiveEventWorkTypeId || levelOptions.length === 0, loading: eventWorkTypeLevelsQuery.isFetching, placeholder: levelOptions.length ? '请选择等级' : '请先在赛事总览选择等级', style: { width: 180, maxWidth: '100%' }, options: levelOptions, onChange: async (levelCode) => {
                    if (!(await confirmNavigation()))
                        return;
                    setSelectedEventWorkTypeLevelCode(levelCode || undefined);
                } }, "level-select"),
            effectiveEventWorkTypeId && !eventWorkTypeLevelsQuery.isFetching && levelOptions.length === 0 ? (_jsx(Typography.Text, { type: "warning", children: "\u8BF7\u5148\u5728\u8D5B\u4E8B\u603B\u89C8\u4E3A\u5F53\u524D\u5DE5\u79CD\u9009\u62E9\u53C2\u8D5B\u7B49\u7EA7" }, "level-warning")) : null,
        ] : []),
    ];
    const mergeFormalExamControls = module === 'formal-exam-console'
        && Boolean(effectiveEventWorkTypeId && effectiveEventWorkTypeLevelCode);
    if (!canView) {
        return _jsx(Result, { status: "403", title: `暂无${meta.title}查看权限` });
    }
    if (eventsQuery.isLoading)
        return _jsx(Spin, { className: "competition-loading" });
    if (eventsQuery.isError)
        return _jsx(Result, { status: "error", title: "\u8D5B\u4E8B\u5217\u8868\u52A0\u8F7D\u5931\u8D25" });
    return (_jsxs("div", { className: "competition-page", children: [_jsxs("section", { className: "competition-page-heading", children: [module !== 'formal-exam-console' ? (_jsxs("div", { children: [_jsx(Typography.Title, { level: 2, children: meta.title }), _jsx(Typography.Paragraph, { type: "secondary", children: meta.description })] })) : null, _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                            void eventsQuery.refetch();
                            if (needsWorkTypeScope) {
                                void queryClient.invalidateQueries({ queryKey: ['competition-event-work-types'] });
                                void queryClient.invalidateQueries({ queryKey: ['competition-event-work-type-levels'] });
                            }
                        }, children: "\u5237\u65B0\u8D5B\u4E8B" })] }), !mergeFormalExamControls ? (_jsx(Card, { bordered: false, className: "competition-filter-card competition-filter-card--compact", children: _jsx(Space, { align: "center", size: 12, wrap: true, style: { width: '100%' }, children: scopeControls }) })) : null, selectedEvent && module === 'notices' && _jsx(CompetitionNoticePage, { eventId: String(selectedEvent.id), onNavigate: onNavigate, embedded: true }), selectedEvent && module === 'stages' && effectiveEventWorkTypeId && effectiveEventWorkTypeLevelCode ? (_jsx(CompetitionStagePlanningPage, { eventId: String(selectedEvent.id), eventWorkTypeId: effectiveEventWorkTypeId, eventWorkTypeLevelCode: effectiveEventWorkTypeLevelCode, onNavigate: onNavigate, onNavigateToScope: (workTypeId, levelCode) => {
                    setSelectedEventWorkTypeId(String(workTypeId));
                    setSelectedEventWorkTypeLevelCode(levelCode);
                }, embedded: true }, `stage-planning:${selectedEvent.id}:${effectiveEventWorkTypeId ?? 'event'}:${effectiveEventWorkTypeLevelCode}`)) : selectedEvent && module === 'stages' ? (_jsx(Card, { bordered: false, style: { marginTop: 16 }, children: _jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u5148\u5728\u8D5B\u4E8B\u603B\u89C8\u660E\u786E\u9009\u62E9\u5F53\u524D\u5DE5\u79CD\u7684\u53C2\u8D5B\u7B49\u7EA7\uFF0C\u518D\u5F00\u59CB\u8BE5\u7B49\u7EA7\u7684\u8D5B\u6BB5\u7F16\u6392\u3002" }) })) : null, selectedEvent && module === 'theory-exam' && (_jsx(CompetitionTheoryExamPage, { eventId: String(selectedEvent.id), initialStageId: initialStageId, initialEventWorkTypeId: initialEventWorkTypeId, onNavigate: onNavigate, embedded: true })), selectedEvent && module === 'formal-exam-console' && effectiveEventWorkTypeId && effectiveEventWorkTypeLevelCode ? (_jsx(CompetitionFormalExamConsolePage, { eventId: String(selectedEvent.id), eventWorkTypeId: effectiveEventWorkTypeId, eventWorkTypeLevelCode: effectiveEventWorkTypeLevelCode, initialStageId: initialStageId, scopeControls: scopeControls }, `formal-exam:${selectedEvent.id}:${effectiveEventWorkTypeId}:${effectiveEventWorkTypeLevelCode}`)) : selectedEvent && module === 'formal-exam-console' ? (_jsx(Card, { bordered: false, style: { marginTop: 16 }, children: _jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u5148\u9009\u62E9\u5DE5\u79CD\u548C\u6280\u80FD\u7B49\u7EA7\u3002\u6B63\u5F0F\u6BD4\u8D5B\u6309\u300C\u8D5B\u4E8B + \u5DE5\u79CD + \u7B49\u7EA7\u300D\u9694\u79BB\uFF0C\u4E0E\u8D5B\u6BB5\u7F16\u6392\u3001\u9009\u624B\u8BD5\u5377\u914D\u7F6E\u540C\u4E00\u8303\u56F4\uFF1B\u53EA\u70B9\u300C\u5237\u65B0\u8D5B\u4E8B\u300D\u4E0D\u4F1A\u5E26\u51FA\u8BE5\u5DE5\u79CD\u7B49\u7EA7\u4E0B\u7684\u8D5B\u6BB5\u3002" }) })) : null, selectedEvent && module === 'practical-exam-console' && effectiveEventWorkTypeId && effectiveEventWorkTypeLevelCode ? (_jsx(CompetitionPracticalConsolePage, { eventId: String(selectedEvent.id), eventWorkTypeId: effectiveEventWorkTypeId, eventWorkTypeLevelCode: effectiveEventWorkTypeLevelCode, initialStageId: initialStageId }, `practical-exam:${selectedEvent.id}:${effectiveEventWorkTypeId}:${effectiveEventWorkTypeLevelCode}`)) : null, selectedEvent && module === 'scores' && _jsx(CompetitionScoreRankingPage, { eventId: String(selectedEvent.id), onNavigate: onNavigate, embedded: true })] }));
}
