import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EyeOutlined, EditOutlined, PlusOutlined, SendOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Spin, Table, Tag, Typography, message, } from 'antd';
import { useEffect, useState } from 'react';
import { createCompetitionMakeExamPlan, deleteCompetitionMakeExamPlan, getCompetitionMakeExamPlan, listCompetitionMakeExamSnapshots, pageCompetitionMakeExamPlans, previewCompetitionMakeExamSnapshot, publishCompetitionMakeExamPlan, updateCompetitionMakeExamPlan, precheckCompetitionMakeExamPlan, pageCompetitionQuestions, } from '../../api/competitionQuestion';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents } from '../../api/competition';
import { getCompetitionQuestionBlueprintTree, pageCompetitionQuestionBlueprints } from '../../api/competitionQuestionBlueprint';
import { CompetitionPracticalSnapshotPaperPreview } from './CompetitionPracticalSnapshotPaperPreview';
const typeLabels = {
    single: '单选题',
    multi: '多选题',
    judge: '判断题',
    fill: '填空题',
    practical: '实操题',
};
function getEventIdFromUrl() {
    return new URLSearchParams(window.location.search).get('eventId') || '';
}
export function CompetitionMakeExamPlanPage({ contentType = 'theory', simpleMode = false }) {
    const queryClient = useQueryClient();
    const [eventId, setEventId] = useState(getEventIdFromUrl);
    const [workTypeId, setWorkTypeId] = useState('');
    const [levelId, setLevelId] = useState('');
    const [editing, setEditing] = useState(null);
    const [formOpen, setFormOpen] = useState(false);
    const [previewPlanId, setPreviewPlanId] = useState(null);
    const [previewSnapshotId, setPreviewSnapshotId] = useState(null);
    const [theoryPrecheck, setTheoryPrecheck] = useState(null);
    const [practicalPrecheck, setPracticalPrecheck] = useState(null);
    const [form] = Form.useForm();
    const eventsQuery = useQuery({ queryKey: ['competition-events', 'make-plan'], queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }) });
    const events = eventsQuery.data?.items ?? [];
    const workTypesQuery = useQuery({ queryKey: ['competition-event-work-types', 'make-plan', eventId], queryFn: () => listCompetitionEventWorkTypes(eventId), enabled: Boolean(eventId) });
    const workTypes = workTypesQuery.data ?? [];
    const levelsQuery = useQuery({ queryKey: ['competition-event-levels', 'make-plan', eventId, workTypeId], queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId), enabled: Boolean(eventId && workTypeId) });
    const levels = levelsQuery.data ?? [];
    const blueprintQuery = useQuery({
        queryKey: ['competition-plan-active-blueprint', eventId, workTypeId, levelId, contentType],
        queryFn: () => pageCompetitionQuestionBlueprints(eventId, {
            page: 1, pageSize: 20, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType, status: 'active',
        }),
        enabled: Boolean(!simpleMode && eventId && workTypeId && levelId),
    });
    const activeBlueprint = blueprintQuery.data?.items?.[0];
    const selectedBlueprintId = simpleMode ? undefined : editing ? editing.blueprintId || undefined : activeBlueprint?.id;
    const blueprintTreeQuery = useQuery({
        queryKey: ['competition-plan-blueprint-tree', eventId, selectedBlueprintId],
        queryFn: () => getCompetitionQuestionBlueprintTree(eventId, selectedBlueprintId),
        enabled: Boolean(eventId && selectedBlueprintId),
    });
    const practicalQuestionsQuery = useQuery({
        queryKey: ['competition-practical-question-options', eventId, workTypeId, levelId, selectedBlueprintId],
        queryFn: () => pageCompetitionQuestions(eventId, {
            page: 1, pageSize: 500, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId,
            contentType: 'practical', questionType: 'practical',
            blueprintId: undefined,
        }),
        enabled: contentType === 'practical' && (simpleMode || !selectedBlueprintId) && Boolean(eventId && workTypeId && levelId),
    });
    const practicalQuestions = practicalQuestionsQuery.data?.items ?? [];
    const plansQuery = useQuery({
        queryKey: ['competition-make-plans', eventId, workTypeId, levelId, contentType, simpleMode],
        queryFn: () => pageCompetitionMakeExamPlans(eventId, {
            page: 1, pageSize: 100, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType, simpleMode,
        }),
        enabled: Boolean(eventId && workTypeId && levelId),
    });
    const snapshotsQuery = useQuery({
        queryKey: ['competition-make-plan-snapshots', eventId, previewPlanId],
        queryFn: () => listCompetitionMakeExamSnapshots(eventId, previewPlanId),
        enabled: Boolean(eventId && previewPlanId),
    });
    const previewQuery = useQuery({
        queryKey: ['competition-make-snapshot-preview', eventId, previewSnapshotId],
        queryFn: () => previewCompetitionMakeExamSnapshot(eventId, previewSnapshotId),
        enabled: Boolean(eventId && previewSnapshotId),
    });
    useEffect(() => { if (!eventId && events[0])
        setEventId(String(events[0].id)); }, [eventId, events]);
    useEffect(() => { if (!workTypeId && workTypes[0])
        setWorkTypeId(String(workTypes[0].id)); }, [workTypeId, workTypes]);
    useEffect(() => { if (!levelId && levels[0])
        setLevelId(String(levels[0].id)); }, [levelId, levels]);
    useEffect(() => {
        if (snapshotsQuery.data?.length && !snapshotsQuery.data.some(item => item.id === previewSnapshotId)) {
            setPreviewSnapshotId(snapshotsQuery.data[0].id);
        }
    }, [previewSnapshotId, snapshotsQuery.data]);
    const buildRequest = (values) => {
        const requestBlueprintId = simpleMode
            ? undefined
            : values.blueprintId || editing?.blueprintId || (!editing ? activeBlueprint?.id : undefined);
        if (contentType === 'practical') {
            if (simpleMode) {
                const seed = new Uint32Array(1);
                window.crypto?.getRandomValues(seed);
                return {
                    eventWorkTypeId: values.eventWorkTypeId,
                    eventWorkTypeLevelId: values.eventWorkTypeLevelId,
                    contentType,
                    title: values.title,
                    examType: values.examType,
                    durationMinutes: Number(values.durationMinutes),
                    totalScore: Number(values.totalScore),
                    passScore: values.passScore,
                    randomSeed: String(seed[0] || Math.floor(Math.random() * 4_294_967_296)),
                    comments: values.comments,
                    details: values.details,
                };
            }
            const selected = practicalQuestions.find((question) => question.id === values.practicalQuestionId);
            const practicalDetail = selected?.practicalDetail;
            const blueprintPractical = Boolean(requestBlueprintId);
            return {
                eventWorkTypeId: values.eventWorkTypeId,
                eventWorkTypeLevelId: values.eventWorkTypeLevelId,
                practicalQuestionId: blueprintPractical ? undefined : values.practicalQuestionId,
                blueprintId: requestBlueprintId,
                contentType,
                title: values.title,
                examType: values.examType,
                durationMinutes: Number(values.durationMinutes),
                totalScore: Number(values.totalScore),
                passScore: values.passScore,
                paperCount: Number(values.paperCount || 1),
                randomSeed: values.randomSeed,
                comments: values.comments,
                details: blueprintPractical
                    ? []
                    : [{ questionType: 'practical', questionCount: 1, point: Number(practicalDetail?.totalScore || values.totalScore), sortOrder: 1 }],
            };
        }
        // Do not spread form values here: an old form instance may still contain
        // practical topicRules after a tab switch, but theory requests must never
        // send them to the server.
        return {
            eventWorkTypeId: values.eventWorkTypeId,
            eventWorkTypeLevelId: values.eventWorkTypeLevelId,
            blueprintId: requestBlueprintId,
            contentType,
            title: values.title,
            examType: values.examType,
            durationMinutes: Number(values.durationMinutes),
            totalScore: Number(values.totalScore),
            passScore: values.passScore,
            paperCount: Number(values.paperCount || 1),
            comments: values.comments,
            details: values.details,
            randomSeed: simpleMode ? (() => {
                const seed = new Uint32Array(1);
                window.crypto?.getRandomValues(seed);
                return String(seed[0] || Math.floor(Math.random() * 4_294_967_296));
            })() : values.randomSeed,
        };
    };
    const precheckMutation = useMutation({
        mutationFn: (values) => precheckCompetitionMakeExamPlan(eventId, buildRequest(values)),
        onSuccess: (result) => {
            if (contentType === 'practical')
                setPracticalPrecheck(result);
            else
                setTheoryPrecheck(result);
        },
        onError: (error) => { message.error(error instanceof Error ? error.message : '组卷预检失败'); },
    });
    const saveMutation = useMutation({
        mutationFn: async (values) => {
            const body = buildRequest(values);
            const precheck = await precheckCompetitionMakeExamPlan(eventId, body);
            if (contentType === 'theory')
                setTheoryPrecheck(precheck);
            else
                setPracticalPrecheck(precheck);
            if (!precheck.ready) {
                const shortages = precheck.topics
                    .filter((topic) => topic.shortage > 0)
                    .map((topic) => `${topic.path} 缺 ${topic.shortage} 题`);
                const detailShortages = precheck.details
                    .filter((detail) => detail.shortage > 0)
                    .map((detail) => `${typeLabels[detail.questionType] || detail.questionType} 缺 ${detail.shortage} 题`);
                const practicalShortages = (precheck.practicalAllocations || [])
                    .filter((item) => item.candidateQuestionCount <= 0)
                    .map((item) => `${item.path} 缺少可用完整实操题`);
                const reason = [...precheck.errors, ...shortages, ...detailShortages, ...practicalShortages].join('；')
                    || (simpleMode ? '题库题量不足，请调整题量或题库配置' : '候选题不足，请调整细目表或题库');
                throw new Error(`组卷预检未通过：${reason}`);
            }
            return editing ? updateCompetitionMakeExamPlan(eventId, editing.id, body) : createCompetitionMakeExamPlan(eventId, body);
        },
        onSuccess: () => {
            message.success(editing ? '组卷计划已更新并生成新草稿快照' : '组卷计划已保存并生成草稿快照');
            setFormOpen(false);
            setEditing(null);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['competition-make-plans'] });
        },
        onError: (error) => { message.error(error instanceof Error ? error.message : '组卷预检或保存失败'); },
    });
    const publishMutation = useMutation({
        mutationFn: (planId) => publishCompetitionMakeExamPlan(eventId, planId),
        onSuccess: () => { message.success('组卷计划及快照已发布'); queryClient.invalidateQueries({ queryKey: ['competition-make-plans'] }); },
    });
    const deleteMutation = useMutation({
        mutationFn: (planId) => deleteCompetitionMakeExamPlan(eventId, planId),
        onSuccess: () => { message.success('组卷计划已删除'); queryClient.invalidateQueries({ queryKey: ['competition-make-plans'] }); },
    });
    const openCreate = () => {
        setEditing(null);
        setTheoryPrecheck(null);
        setPracticalPrecheck(null);
        form.resetFields();
        const firstPractical = contentType === 'practical' && !simpleMode ? practicalQuestions[0] : undefined;
        const defaultDetails = contentType === 'theory'
            ? [{ questionType: 'single', questionCount: 10, point: 10, sortOrder: 1 }]
            : selectedBlueprintId && !simpleMode ? [] : [{ questionType: 'practical', questionCount: 1, point: 100, sortOrder: 1 }];
        form.setFieldsValue({
            eventWorkTypeId: workTypeId,
            eventWorkTypeLevelId: levelId,
            durationMinutes: 90,
            totalScore: 100,
            passScore: 60,
            paperCount: 1,
            details: defaultDetails,
            practicalQuestionId: firstPractical?.id,
            blueprintId: simpleMode ? undefined : activeBlueprint?.id,
            randomSeed: undefined,
        });
        if (firstPractical?.practicalDetail) {
            form.setFieldsValue({
                durationMinutes: firstPractical.practicalDetail.durationMinutes || 60,
                totalScore: Number(firstPractical.practicalDetail.totalScore || 100),
            });
        }
        setFormOpen(true);
    };
    const openEdit = async (record) => {
        const detail = await getCompetitionMakeExamPlan(eventId, record.id);
        setEditing(detail);
        setTheoryPrecheck(null);
        setPracticalPrecheck(null);
        form.setFieldsValue({
            eventWorkTypeId: detail.eventWorkTypeId,
            eventWorkTypeLevelId: detail.eventWorkTypeLevelId,
            practicalQuestionId: detail.practicalQuestionId || undefined,
            // Do not silently migrate a legacy plan to the currently active
            // blueprint when it is opened for review/editing.
            blueprintId: detail.blueprintId || undefined,
            title: detail.title,
            examType: detail.examType || undefined,
            durationMinutes: detail.durationMinutes,
            totalScore: Number(detail.totalScore),
            passScore: Number(detail.passScore),
            paperCount: detail.paperCount || 1,
            randomSeed: detail.randomSeed || undefined,
            comments: detail.comments || undefined,
            details: detail.details.map(item => ({
                questionType: item.questionType,
                questionCount: item.questionCount,
                point: Number(item.point),
                difficultyStart: item.difficultyStart || undefined,
                difficultyEnd: item.difficultyEnd || undefined,
                sortOrder: item.sortOrder,
            })),
        });
        if (contentType === 'practical' && detail.practicalQuestionId) {
            const selected = practicalQuestions.find((question) => question.id === detail.practicalQuestionId);
            if (selected?.practicalDetail) {
                form.setFieldsValue({
                    durationMinutes: selected.practicalDetail.durationMinutes || detail.durationMinutes,
                    totalScore: Number(selected.practicalDetail.totalScore || detail.totalScore),
                });
            }
        }
        setFormOpen(true);
    };
    const rows = plansQuery.data?.items ?? [];
    const simplePrecheck = contentType === 'theory' ? theoryPrecheck : practicalPrecheck;
    const questionTypeOptions = contentType === 'theory'
        ? ['single', 'multi', 'judge', 'fill'].map(value => ({ value, label: typeLabels[value] }))
        : [{ value: 'practical', label: '实操题' }];
    return (_jsxs(Card, { title: simpleMode
            ? `竞赛简易${contentType === 'theory' ? '理论' : '实操'}组卷`
            : contentType === 'theory' ? '竞赛理论组卷计划' : '竞赛实操组卷计划', extra: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), disabled: !eventId || !workTypeId || !levelId, onClick: openCreate, children: simpleMode ? '新建简易计划' : '新建计划' }), children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: simpleMode
                    ? '简易组卷不需要细目表。配置题型、题量和单题分值后，系统会从当前赛事、工种和等级下的启用试题中随机抽题；实操评分项会按单题分值等比例换算。'
                    : contentType === 'practical'
                        ? '实操题量由细目表的层级选考关系决定：下级为必考时每个下级各抽1题；一级必考、二级任选一项时，每个一级只形成1个题位，再从该一级的二级范围中任选一个并抽1道完整题。'
                        : '理论组卷按组卷规则和细目表三级权重自动抽题；理论分布不可手工修改。' }), !simpleMode && contentType === 'theory' && !activeBlueprint && eventId && workTypeId && levelId && _jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u5F53\u524D\u8D5B\u4E8B\u5DE5\u79CD\u7B49\u7EA7\u6CA1\u6709\u542F\u7528\u7684\u7406\u8BBA\u7EC6\u76EE\u8868\uFF1B\u7406\u8BBA\u81EA\u52A8\u7EC4\u5377\u9700\u8981\u5148\u5BFC\u5165\u5E76\u542F\u7528\u7248\u672C\u3002" }), _jsxs(Space, { wrap: true, style: { marginBottom: 16 }, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: 250 }, placeholder: "\u9009\u62E9\u8D5B\u4E8B", value: eventId || undefined, options: events.map((event) => ({ value: String(event.id), label: `${event.name}（${event.code}）` })), onChange: (value) => { setEventId(value); setWorkTypeId(''); setLevelId(''); } }), _jsx(Select, { style: { width: 220 }, placeholder: "\u9009\u62E9\u5DE5\u79CD", value: workTypeId || undefined, options: workTypes.map((item) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) })), onChange: (value) => { setWorkTypeId(value); setLevelId(''); } }), _jsx(Select, { style: { width: 180 }, placeholder: "\u9009\u62E9\u7B49\u7EA7", value: levelId || undefined, options: levels.map((item) => ({ value: String(item.id), label: `${item.levelName}（${item.levelCode}）` })), onChange: setLevelId })] }), _jsx(Table, { rowKey: "id", loading: plansQuery.isLoading || plansQuery.isFetching, dataSource: rows, pagination: { pageSize: 20, showSizeChanger: false }, scroll: { x: 'max-content' }, columns: [
                    { title: '计划标题', dataIndex: 'title', ellipsis: true },
                    { title: '时长', dataIndex: 'durationMinutes', width: 90, render: (value) => `${value} 分钟` },
                    { title: '总分', dataIndex: 'totalScore', width: 80 },
                    { title: '及格分', dataIndex: 'passScore', width: 90 },
                    { title: '快照', dataIndex: 'paperCount', width: 90, render: (value) => `${value} 套` },
                    { title: '状态', dataIndex: 'status', width: 100, render: (value) => _jsx(Tag, { color: value === 'published' ? 'green' : 'gold', children: value === 'published' ? '已发布' : value === 'draft' ? '草稿' : value }) },
                    { title: '操作', key: 'actions', width: 250, fixed: 'right', render: (_, record) => (_jsxs(Space, { wrap: true, size: [8, 0], style: { maxWidth: '100%' }, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => { setPreviewPlanId(record.id); setPreviewSnapshotId(null); }, children: "\u5FEB\u7167\u9884\u89C8" }), record.status === 'draft' && _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => void openEdit(record), children: "\u7F16\u8F91" }), record.status === 'draft' && _jsx(Popconfirm, { title: "\u53D1\u5E03\u540E\u5FEB\u7167\u5C06\u5141\u8BB8\u8D5B\u6BB5\u4F7F\u7528\uFF0C\u786E\u8BA4\u53D1\u5E03\uFF1F", onConfirm: () => publishMutation.mutate(record.id), children: _jsx(Button, { type: "link", size: "small", icon: _jsx(SendOutlined, {}), children: "\u53D1\u5E03" }) }), record.status === 'draft' && _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u6B64\u7EC4\u5377\u8BA1\u5212\uFF1F", onConfirm: () => deleteMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })) },
                ] }), _jsx(Modal, { title: `${editing ? '编辑' : '新建'}${simpleMode ? '简易' : ''}${contentType === 'theory' ? '理论' : '实操'}组卷计划`, open: formOpen, onCancel: () => setFormOpen(false), onOk: () => form.submit(), confirmLoading: saveMutation.isPending, destroyOnClose: true, width: 860, children: _jsxs(Form, { form: form, layout: "vertical", onFinish: (values) => saveMutation.mutate(values), children: [_jsx(Form.Item, { name: "eventWorkTypeId", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "eventWorkTypeLevelId", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "randomSeed", hidden: true, children: _jsx(Input, {}) }), !simpleMode && _jsx(Form.Item, { name: "blueprintId", label: "\u7EC6\u76EE\u8868\u7248\u672C", rules: [{ required: contentType === 'theory' || Boolean(editing?.blueprintId), message: '请选择已启用的细目表版本' }], children: _jsx(Select, { disabled: Boolean(editing) || (!activeBlueprint && !editing), options: (() => {
                                    const blueprint = selectedBlueprintId && blueprintTreeQuery.data?.blueprint?.id === selectedBlueprintId
                                        ? blueprintTreeQuery.data.blueprint
                                        : activeBlueprint;
                                    return blueprint ? [{ value: blueprint.id, label: `v${blueprint.versionNo} ${blueprint.title}` }] : [];
                                })(), placeholder: editing?.blueprintId ? '沿用原细目表版本' : '请先启用理论/实操细目表' }) }), contentType === 'practical' && selectedBlueprintId && _jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginTop: 4, marginBottom: 8 }, children: [_jsx(Typography.Text, { strong: true, children: "\u5B9E\u64CD\u81EA\u52A8\u7EC4\u5377\u7ED3\u6784\u9884\u89C8" }), _jsx(Button, { loading: precheckMutation.isPending, onClick: () => form.validateFields().then(values => precheckMutation.mutate(values)).catch(() => undefined), children: "\u9884\u68C0\u5E76\u67E5\u770B\u7ED3\u6784" })] }), _jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 8 }, children: "\u7CFB\u7EDF\u6309\u5C42\u7EA7\u5173\u7CFB\u751F\u6210\u9898\u4F4D\uFF1A\u4E0B\u7EA7\u5FC5\u8003\u9879\u5206\u522B\u6210\u9898\uFF1B\u201C\u4E00\u7EA7\u5FC5\u8003\u3001\u4E8C\u7EA7\u4EFB\u9009\u4E00\u9879\u201D\u5219\u4E00\u7EA7\u53EA\u62101\u9898\uFF0C\u5E76\u5728\u8BE5\u4E00\u7EA7\u7684\u4E8C\u7EA7\u5019\u9009\u4E2D\u4EFB\u9009\u4E00\u4E2A\u8303\u56F4\u3002\u6700\u7EC8\u9898\u4F4D\u4F7F\u7528\u5BF9\u5E94\u5FC5\u8003\u8303\u56F4\u7684\u6574\u5377\u6BD4\u91CD\uFF0C\u4EFB\u9009\u5019\u9009\u7684\u6743\u91CD\u4E0D\u4F1A\u76F8\u52A0\uFF1B\u8BD5\u9898\u91CF\u4EC5\u4F5C\u4E3A\u9898\u5E93\u50A8\u5907\u63D0\u793A\u3002" }), practicalPrecheck && _jsxs(_Fragment, { children: [practicalPrecheck.errors.length > 0 && _jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 8 }, message: practicalPrecheck.errors.join('；') }), practicalPrecheck.warnings.length > 0 && _jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 8 }, message: practicalPrecheck.warnings.join('；') }), _jsx(Table, { size: "small", rowKey: "selectionGroup", pagination: false, dataSource: practicalPrecheck.practicalAllocations || [], expandable: {
                                                expandedRowRender: (row) => _jsx(Table, { size: "small", rowKey: "blueprintNodeId", pagination: false, dataSource: row.candidateNodes, columns: [
                                                        { title: '候选结构', dataIndex: 'path', ellipsis: true },
                                                        { title: '候选权重', dataIndex: 'scoreWeight', width: 90, render: (value) => `${(Number(value) * 100).toFixed(2)}%` },
                                                        { title: '末级考评点', dataIndex: 'assessmentPointIds', width: 110, render: (value) => value?.length || 0 },
                                                        { title: '可用完整实操题', dataIndex: 'candidateQuestionCount', width: 120 },
                                                        { title: '状态', dataIndex: 'selected', width: 70, render: (value) => value ? _jsx(Tag, { color: "green", children: "\u5DF2\u9009" }) : '-' },
                                                    ] }),
                                            }, columns: [
                                                { title: '选择组', dataIndex: 'selectionGroup', width: 150 },
                                                { title: '模式', dataIndex: 'selectionMode', width: 100, render: (value) => value === 'REQUIRED' ? '必考' : '任选一项' },
                                                { title: '最终结构', dataIndex: 'path', ellipsis: true },
                                                { title: '整组权重', dataIndex: 'scoreWeight', width: 90, render: (value) => `${(Number(value) * 100).toFixed(2)}%` },
                                                { title: '目标分值', dataIndex: 'targetScore', width: 90 },
                                                { title: '可用题数', dataIndex: 'candidateQuestionCount', width: 90 },
                                                { title: '参考时长', dataIndex: 'referenceDurationMinutes', width: 100, render: (value) => value == null ? '-' : `${value} 分钟` },
                                                { title: '已选题目', dataIndex: 'selectedQuestionId', width: 110, render: (value) => value || '待生成' },
                                            ] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u6700\u7EC8\u9898\u91CF\uFF1A", practicalPrecheck.practicalQuestionCount ?? practicalPrecheck.practicalAllocations?.length ?? 0, " \u9898\uFF1B\u6240\u9009\u9898\u76EE\u53C2\u8003\u65F6\u957F\u5408\u8BA1\uFF1A", practicalPrecheck.practicalReferenceDurationMinutes ?? 0, " \u5206\u949F\u3002"] })] })] }), _jsxs(Space, { wrap: true, align: "start", children: [_jsx(Form.Item, { name: "title", label: "\u8BA1\u5212\u6807\u9898", rules: [{ required: true }], children: _jsx(Input, { style: { width: 300 } }) }), _jsx(Form.Item, { name: "examType", label: "\u8003\u8BD5\u7C7B\u578B", children: _jsx(Input, { style: { width: 180 }, placeholder: "\u4F8B\u5982\uFF1A\u521D\u8D5B\u7406\u8BBA" }) }), contentType === 'practical' && !simpleMode && !activeBlueprint && _jsx(Form.Item, { name: "practicalQuestionId", label: "\u5B9E\u64CD\u9898", rules: [{ required: true, message: '请选择实操题' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: 360 }, loading: practicalQuestionsQuery.isLoading, placeholder: "\u9009\u62E9\u5F53\u524D\u8D5B\u4E8B/\u5DE5\u79CD/\u7B49\u7EA7\u4E0B\u7684\u542F\u7528\u5B9E\u64CD\u9898", options: practicalQuestions.map((question) => ({ value: question.id, label: `${question.sortOrder}. ${question.stem}` })), onChange: (value) => {
                                            const selected = practicalQuestions.find((question) => question.id === value);
                                            if (selected?.practicalDetail) {
                                                form.setFieldsValue({
                                                    durationMinutes: selected.practicalDetail.durationMinutes || 60,
                                                    totalScore: Number(selected.practicalDetail.totalScore || 100),
                                                });
                                            }
                                        } }) }), contentType === 'practical' && !simpleMode && _jsx(Form.Item, { name: "paperCount", label: "\u751F\u6210\u5957\u6570", rules: [{ required: true }], children: _jsx(InputNumber, { min: 1, max: 50, precision: 0 }) }), _jsx(Form.Item, { name: "durationMinutes", label: "\u8003\u8BD5\u65F6\u957F\uFF08\u5206\u949F\uFF09", rules: [{ required: true, type: 'number', min: 1 }], extra: contentType === 'practical' ? '计划时长独立配置；题目参考时长合计超出时仅提示' : undefined, children: _jsx(InputNumber, { min: 1, precision: 0 }) }), _jsx(Form.Item, { name: "totalScore", label: "\u8BA1\u5212\u603B\u5206\uFF08\u6BCF\u5957\u5377\uFF09", rules: [{ required: true, type: 'number', min: 0.01 }], extra: simpleMode ? '必须等于题量 × 单题分值的合计' : contentType === 'practical' ? '系统按最终必考题位或其任选组对应的整卷比重精确分配分值' : '每套卷实际题目分值合计必须等于此值', children: _jsx(InputNumber, { min: 0.01, precision: 2 }) }), _jsx(Form.Item, { name: "passScore", label: "\u53CA\u683C\u5206", rules: [{ required: true }], children: _jsx(InputNumber, { min: 0 }) })] }), (contentType === 'theory' || (simpleMode && contentType === 'practical')) && _jsxs(_Fragment, { children: [_jsx(Typography.Text, { strong: true, children: contentType === 'theory' ? '组卷规则（题型、题量和单题分值）' : '组卷规则（实操题量和单题分值）' }), _jsx(Form.List, { name: "details", children: (fields, { add, remove }) => _jsxs(_Fragment, { children: [fields.map((field, index) => _jsxs(Space, { align: "start", style: { display: 'flex', marginTop: 12 }, children: [_jsx(Form.Item, { ...field, name: [field.name, 'sortOrder'], label: index === 0 ? '序号' : undefined, rules: [{ required: true }], children: _jsx(InputNumber, { min: 1 }) }), _jsx(Form.Item, { ...field, name: [field.name, 'questionType'], label: index === 0 ? '题型' : undefined, rules: [{ required: true }], children: _jsx(Select, { style: { width: 130 }, options: questionTypeOptions }) }), _jsx(Form.Item, { ...field, name: [field.name, 'questionCount'], label: index === 0 ? '题量' : undefined, rules: [{ required: true }], children: _jsx(InputNumber, { min: 1 }) }), _jsx(Form.Item, { ...field, name: [field.name, 'point'], label: index === 0 ? '单题分值' : undefined, rules: [{ required: true }], children: _jsx(InputNumber, { min: 0.01 }) }), contentType === 'theory' && _jsx(Form.Item, { ...field, name: [field.name, 'difficultyStart'], label: index === 0 ? '难度起' : undefined, children: _jsx(InputNumber, { min: 1, max: 5, placeholder: "\u4E0D\u9650" }) }), contentType === 'theory' && _jsx(Form.Item, { ...field, name: [field.name, 'difficultyEnd'], label: index === 0 ? '难度止' : undefined, children: _jsx(InputNumber, { min: 1, max: 5, placeholder: "\u4E0D\u9650" }) }), contentType === 'theory' && _jsx(Button, { danger: true, type: "link", style: { marginTop: index === 0 ? 30 : 0 }, onClick: () => remove(field.name), children: "\u5220\u9664" })] }, field.key)), contentType === 'theory' && _jsx(Button, { type: "dashed", block: true, icon: _jsx(PlusOutlined, {}), onClick: () => add({ questionType: 'single', questionCount: 1, point: 1, sortOrder: fields.length + 1 }), children: "\u589E\u52A0\u9898\u578B" })] }) }), simpleMode && _jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginTop: 16, marginBottom: 8 }, children: [_jsx(Typography.Text, { strong: true, children: "\u968F\u673A\u62BD\u9898\u9884\u68C0" }), _jsx(Button, { loading: precheckMutation.isPending, onClick: () => form.validateFields().then(values => precheckMutation.mutate(values)).catch(() => undefined), children: "\u67E5\u770B\u53EF\u7528\u9898\u91CF" }), simplePrecheck && _jsx(Tag, { color: simplePrecheck.ready ? 'green' : 'red', children: simplePrecheck.ready ? '可组卷' : '题量不足' })] }), simplePrecheck && _jsxs(_Fragment, { children: [simplePrecheck.errors.length > 0 && _jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 8 }, message: simplePrecheck.errors.join('；') }), simplePrecheck.warnings.length > 0 && _jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 8 }, message: simplePrecheck.warnings.join('；') }), _jsx(Table, { size: "small", rowKey: (row) => String(row.sortOrder ?? row.ruleIndex ?? row.questionType), pagination: false, dataSource: simplePrecheck.details, columns: [
                                                        { title: '题型', dataIndex: 'questionType', render: (value) => typeLabels[value] || value },
                                                        { title: '需求题量', dataIndex: 'required', width: 100 },
                                                        { title: '可用题量', dataIndex: 'available', width: 100 },
                                                        { title: '缺口', dataIndex: 'shortage', width: 80, render: (value) => value > 0 ? _jsx(Typography.Text, { type: "danger", children: value }) : value },
                                                    ] })] })] }), selectedBlueprintId && _jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginTop: 16, marginBottom: 8 }, children: [_jsx(Typography.Text, { strong: true, children: "\u7406\u8BBA\u62BD\u9898\u5206\u5E03\uFF08\u6839\u636E\u7EC6\u76EE\u8868\u4E09\u7EA7\u6743\u91CD\u81EA\u52A8\u8BA1\u7B97\uFF09" }), _jsx(Button, { loading: precheckMutation.isPending, onClick: () => form.validateFields().then(values => precheckMutation.mutate(values)).catch(() => undefined), children: "\u9884\u68C0\u5E76\u67E5\u770B\u81EA\u52A8\u5206\u914D" })] }), _jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 8 }, children: "\u60A8\u53EA\u9700\u914D\u7F6E\u9898\u578B\u3001\u9898\u91CF\u3001\u5355\u9898\u5206\u503C\u548C\u96BE\u5EA6\u3002\u7CFB\u7EDF\u5C06\u6309\u7167\u7EC6\u76EE\u8868\u4E09\u7EA7\u6743\u91CD\u81EA\u52A8\u5206\u914D\u9898\u76EE\uFF0C\u7406\u8BBA\u5206\u5E03\u4E0D\u53EF\u624B\u5DE5\u4FEE\u6539\u3002" }), theoryPrecheck && _jsxs(_Fragment, { children: [theoryPrecheck.errors.length > 0 && _jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 8 }, message: theoryPrecheck.errors.join('；') }), theoryPrecheck.warnings.length > 0 && _jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 8 }, message: theoryPrecheck.warnings.join('；') }), _jsx(Table, { size: "small", rowKey: "blueprintNodeId", pagination: false, dataSource: theoryPrecheck.theoryAllocations, expandable: {
                                                        expandedRowRender: (row) => _jsx(Table, { size: "small", rowKey: "ruleIndex", pagination: false, dataSource: row.details, columns: [
                                                                { title: '规则', dataIndex: 'sortOrder', width: 70 },
                                                                { title: '题型', dataIndex: 'questionType', width: 90, render: (value) => typeLabels[value] || value },
                                                                { title: '难度', render: (_, cell) => cell.difficultyStart == null && cell.difficultyEnd == null ? '不限' : `${cell.difficultyStart ?? ''}-${cell.difficultyEnd ?? ''}` },
                                                                { title: '自动题量', dataIndex: 'required', width: 90 },
                                                                { title: '可用题量', dataIndex: 'available', width: 90 },
                                                                { title: '缺口', dataIndex: 'shortage', width: 70, render: (value) => value > 0 ? _jsx(Typography.Text, { type: "danger", children: value }) : value },
                                                            ] }),
                                                    }, columns: [
                                                        { title: '三级考评范围', dataIndex: 'path', ellipsis: true },
                                                        { title: '权重', dataIndex: 'weight', width: 80, render: (value) => `${(Number(value) * 100).toFixed(2)}%` },
                                                        { title: '自动题量', dataIndex: 'questionCount', width: 90 },
                                                        { title: '可用题量', dataIndex: 'available', width: 90 },
                                                        { title: '缺口', dataIndex: 'shortage', width: 70, render: (value) => value > 0 ? _jsx(Typography.Text, { type: "danger", children: value }) : value },
                                                    ] })] })] })] }), _jsx(Form.Item, { name: "comments", label: "\u5907\u6CE8", style: { marginTop: 16 }, children: _jsx(Input.TextArea, { rows: 3 }) })] }) }), _jsx(Modal, { title: "\u7EC4\u5377\u5FEB\u7167\u9884\u89C8", open: Boolean(previewPlanId), onCancel: () => { setPreviewPlanId(null); setPreviewSnapshotId(null); }, footer: null, width: 1000, styles: { body: { maxHeight: 'calc(100vh - 180px)', overflowY: 'auto', padding: 0 } }, children: snapshotsQuery.isLoading ? _jsx(Spin, {}) : snapshotsQuery.data ? _jsxs(_Fragment, { children: [_jsx(Space, { wrap: true, style: { padding: '16px 20px 0', marginBottom: 0 }, children: snapshotsQuery.data.map(snapshot => _jsxs(Button, { type: snapshot.id === previewSnapshotId ? 'primary' : 'default', onClick: () => setPreviewSnapshotId(snapshot.id), children: [snapshot.paperCode, " \u5377 / v", snapshot.versionNo, " ", _jsx(Tag, { children: snapshot.status })] }, snapshot.id)) }), previewQuery.isLoading ? _jsx(Spin, {}) : previewQuery.data ? _jsx(_Fragment, { children: previewQuery.data.contentType === 'practical' ? _jsx(CompetitionPracticalSnapshotPaperPreview, { snapshot: previewQuery.data }) : _jsxs(_Fragment, { children: [_jsxs(Descriptions, { column: 4, size: "small", style: { marginBottom: 16 }, children: [_jsxs(Descriptions.Item, { label: "\u8BD5\u5377", children: [previewQuery.data.paperCode, " \u5377"] }), _jsx(Descriptions.Item, { label: "\u9898\u91CF", children: previewQuery.data.questionCount }), _jsx(Descriptions.Item, { label: "\u603B\u5206", children: previewQuery.data.totalScore }), _jsxs(Descriptions.Item, { label: "\u65F6\u957F", children: [previewQuery.data.durationMinutes, " \u5206\u949F"] })] }), _jsx(Table, { size: "small", rowKey: "id", pagination: { pageSize: 20 }, dataSource: previewQuery.data.questions, columns: [
                                            { title: '题号', dataIndex: 'sortOrder', width: 70 },
                                            { title: '题型', dataIndex: 'questionType', width: 90, render: (value) => typeLabels[value] || value },
                                            { title: '题干/操作内容', dataIndex: 'stem', ellipsis: true, render: (value, row) => _jsx(Typography.Text, { ellipsis: { tooltip: true }, children: value || row.operationContent }) },
                                            { title: '分值', dataIndex: 'score', width: 80 },
                                            { title: '答案（管理预览）', dataIndex: 'referenceAnswer', width: 180, render: (value, row) => value || row.fillAnswer || '-' },
                                        ] })] }) }) : _jsx(Alert, { type: "warning", message: "\u65E0\u6CD5\u52A0\u8F7D\u5FEB\u7167" })] }) : _jsx(Alert, { type: "warning", message: "\u6682\u65E0\u5FEB\u7167" }) })] }));
}
