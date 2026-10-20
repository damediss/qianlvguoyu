import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, EyeOutlined, PlusOutlined, SendOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Descriptions, Form, Input, Modal, Popconfirm, Select, Space, Spin, Table, Tag, Typography, message, } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents, } from '../../api/competition';
import { createCompetitionMakeExamPlan, deleteCompetitionMakeExamPlan, getCompetitionMakeExamPlan, listCompetitionMakeExamSnapshots, pageCompetitionMakeExamPlans, pageCompetitionQuestions, precheckCompetitionMakeExamPlan, previewCompetitionMakeExamSnapshot, publishCompetitionMakeExamPlan, updateCompetitionMakeExamPlan, } from '../../api/competitionQuestion';
import { CompetitionPracticalSnapshotPaperPreview } from './CompetitionPracticalSnapshotPaperPreview';
function idFromUrl() {
    return new URLSearchParams(window.location.search).get('eventId') || '';
}
function readableError(error, fallback) {
    return error instanceof Error ? error.message : fallback;
}
function plainText(value) {
    return (value ?? '').replace(/<[^>]*>/g, ' ').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim();
}
function scoreOf(question) {
    return Number(question?.practicalDetail?.totalScore ?? 0);
}
function durationOf(question) {
    return Number(question?.practicalDetail?.durationMinutes ?? 0);
}
function scoreMethodLabel(value) {
    return value === 'additive' ? '加分制' : value === 'deductive' ? '扣分制' : '未配置';
}
export function CompetitionPracticalScoringSchemePage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [eventId, setEventId] = useState(idFromUrl);
    const [workTypeId, setWorkTypeId] = useState('');
    const [levelId, setLevelId] = useState('');
    const [selectedIds, setSelectedIds] = useState([]);
    const [editing, setEditing] = useState(null);
    const [formOpen, setFormOpen] = useState(false);
    const [precheck, setPrecheck] = useState(null);
    const [previewPlanId, setPreviewPlanId] = useState(null);
    const [previewSnapshotId, setPreviewSnapshotId] = useState(null);
    const eventsQuery = useQuery({
        queryKey: ['competition-events', 'practical-scoring-scheme'],
        queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }),
    });
    const events = eventsQuery.data?.items ?? [];
    const workTypesQuery = useQuery({
        queryKey: ['competition-event-work-types', 'practical-scoring-scheme', eventId],
        queryFn: () => listCompetitionEventWorkTypes(eventId),
        enabled: Boolean(eventId),
    });
    const workTypes = workTypesQuery.data ?? [];
    const levelsQuery = useQuery({
        queryKey: ['competition-event-work-type-levels', 'practical-scoring-scheme', eventId, workTypeId],
        queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId),
        enabled: Boolean(eventId && workTypeId),
    });
    const levels = levelsQuery.data ?? [];
    const questionsQuery = useQuery({
        queryKey: ['competition-practical-scheme-source-projects', eventId, workTypeId, levelId],
        queryFn: () => pageCompetitionQuestions(eventId, {
            page: 1,
            pageSize: 500,
            eventWorkTypeId: workTypeId,
            eventWorkTypeLevelId: levelId,
            contentType: 'practical',
            questionType: 'practical',
        }),
        enabled: Boolean(eventId && workTypeId && levelId),
    });
    const questions = questionsQuery.data?.items ?? [];
    const questionById = useMemo(() => new Map(questions.map(question => [String(question.id), question])), [questions]);
    const selectedQuestions = useMemo(() => selectedIds.map(id => questionById.get(id)).filter((question) => Boolean(question)), [questionById, selectedIds]);
    const scoreTotalCents = selectedQuestions.reduce((total, question) => total + Math.round(scoreOf(question) * 100), 0);
    const scoreTotal = scoreTotalCents / 100;
    const durationTotal = selectedQuestions.reduce((total, question) => total + durationOf(question), 0);
    const plansQuery = useQuery({
        queryKey: ['competition-make-plans', 'practical-scoring-scheme', eventId, workTypeId, levelId],
        queryFn: () => pageCompetitionMakeExamPlans(eventId, {
            page: 1,
            pageSize: 100,
            eventWorkTypeId: workTypeId,
            eventWorkTypeLevelId: levelId,
            contentType: 'practical',
            simpleMode: true,
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
    const buildRequest = (values) => ({
        eventWorkTypeId: workTypeId,
        eventWorkTypeLevelId: levelId,
        contentType: 'practical',
        title: values.title.trim(),
        examType: 'PRACTICAL_SCORING_SCHEME',
        durationMinutes: durationTotal,
        totalScore: 100,
        passScore: 0,
        comments: values.comments?.trim(),
        details: [],
        paperCount: 1,
        practicalQuestionIds: selectedIds,
    });
    const openNew = (source) => {
        const sourceIds = source?.practicalQuestionIds?.map(String) ?? [];
        setEditing(null);
        setSelectedIds(sourceIds);
        setPrecheck(null);
        form.resetFields();
        form.setFieldsValue({
            title: source ? `${source.title}（新版本）` : '实操评分方案',
            comments: source?.comments ?? undefined,
        });
        setFormOpen(true);
    };
    const openEdit = async (record) => {
        try {
            const detail = await getCompetitionMakeExamPlan(eventId, record.id);
            setEditing(detail);
            setSelectedIds((detail.practicalQuestionIds ?? []).map(String));
            setPrecheck(null);
            form.setFieldsValue({ title: detail.title, comments: detail.comments ?? undefined });
            setFormOpen(true);
        }
        catch (error) {
            message.error(readableError(error, '读取评分方案失败'));
        }
    };
    const precheckMutation = useMutation({
        mutationFn: async (values) => precheckCompetitionMakeExamPlan(eventId, buildRequest(values)),
        onSuccess: result => {
            setPrecheck(result);
            if (result.ready)
                message.success('方案配置完整，可以保存');
            else
                message.error(result.errors.join('；') || '方案校验未通过');
        },
        onError: error => message.error(readableError(error, '评分方案校验失败')),
    });
    const saveMutation = useMutation({
        mutationFn: async (values) => {
            const body = buildRequest(values);
            const result = await precheckCompetitionMakeExamPlan(eventId, body);
            setPrecheck(result);
            if (!result.ready)
                throw new Error(result.errors.join('；') || '评分方案校验未通过');
            return editing
                ? updateCompetitionMakeExamPlan(eventId, editing.id, body)
                : createCompetitionMakeExamPlan(eventId, body);
        },
        onSuccess: async () => {
            message.success(editing ? '评分方案草稿已更新' : '评分方案草稿已创建');
            setFormOpen(false);
            setEditing(null);
            setSelectedIds([]);
            form.resetFields();
            await queryClient.invalidateQueries({ queryKey: ['competition-make-plans', 'practical-scoring-scheme'] });
        },
        onError: error => message.error(readableError(error, '评分方案保存失败')),
    });
    const publishMutation = useMutation({
        mutationFn: (planId) => publishCompetitionMakeExamPlan(eventId, planId),
        onSuccess: async () => {
            message.success('评分方案已发布，发布快照不可修改');
            await queryClient.invalidateQueries({ queryKey: ['competition-make-plans', 'practical-scoring-scheme'] });
        },
        onError: error => message.error(readableError(error, '评分方案发布失败')),
    });
    const deleteMutation = useMutation({
        mutationFn: (planId) => deleteCompetitionMakeExamPlan(eventId, planId),
        onSuccess: async () => {
            message.success('评分方案草稿已删除');
            await queryClient.invalidateQueries({ queryKey: ['competition-make-plans', 'practical-scoring-scheme'] });
        },
        onError: error => message.error(readableError(error, '评分方案删除失败')),
    });
    const moveSelected = (questionId, offset) => {
        const index = selectedIds.indexOf(questionId);
        const target = index + offset;
        if (index < 0 || target < 0 || target >= selectedIds.length)
            return;
        const next = [...selectedIds];
        [next[index], next[target]] = [next[target], next[index]];
        setSelectedIds(next);
        setPrecheck(null);
    };
    const selectedRowSelection = {
        selectedRowKeys: selectedIds,
        preserveSelectedRowKeys: true,
        onChange: (keys) => {
            const nextKeys = keys.map(String);
            if (nextKeys.length > 50) {
                message.warning('一个评分方案最多选择50个实操项目');
                return;
            }
            const retained = selectedIds.filter(id => nextKeys.includes(id));
            setSelectedIds([...retained, ...nextKeys.filter(id => !retained.includes(id))]);
            setPrecheck(null);
        },
        getCheckboxProps: (question) => ({
            disabled: question.status !== 'enabled' || scoreOf(question) <= 0 || durationOf(question) <= 0,
        }),
    };
    const flattenElements = (question) => (question.practicalDetail?.assessmentContents ?? [])
        .flatMap(content => (content.elements ?? []).map(element => ({
        key: `${content.id}-${element.id}`,
        content: plainText(content.contentHtml),
        element: plainText(element.elementHtml),
        standard: plainText(element.standardHtml),
        itemName: element.itemName,
        maxScore: element.maxScore,
        maxDeduction: element.maxDeduction,
        deductionStep: element.deductionStep,
        allocationEnabled: element.allocationEnabled,
    })));
    const planRows = plansQuery.data?.items ?? [];
    const currentEvent = events.find(event => String(event.id) === eventId);
    const currentWorkType = workTypes.find(workType => String(workType.id) === workTypeId);
    const currentLevel = levels.find(level => String(level.id) === levelId);
    return (_jsxs(Card, { title: "\u5B9E\u64CD\u8BC4\u5206\u65B9\u6848", extra: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), disabled: !eventId || !workTypeId || !levelId, onClick: () => openNew(), children: "\u65B0\u5EFA\u65B9\u6848" }), children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u5148\u4ECE\u5F53\u524D\u8D5B\u4E8B\u5DE5\u79CD\u7B49\u7EA7\u4E0B\u7684\u5B9E\u64CD\u9898\u5E93\u9009\u62E9\u9879\u76EE\uFF0C\u518D\u6309\u987A\u5E8F\u7EC4\u6210\u4E00\u4EFD\u8BC4\u5206\u65B9\u6848\u3002", description: "\u65B9\u6848\u6309\u6240\u9009\u9879\u76EE\u7684\u539F\u59CB\u5206\u503C\u6C47\u603B\uFF0C\u5408\u8BA1\u5FC5\u987B\u4E3A100\u5206\uFF0C\u4E0D\u505A\u6BD4\u4F8B\u6362\u7B97\u3002\u4FDD\u5B58\u540E\u751F\u6210\u8349\u7A3F\u5FEB\u7167\uFF1B\u53D1\u5E03\u540E\u4F5C\u4E3A\u4E0D\u53EF\u53D8\u7248\u672C\u4F9B\u8D5B\u6BB5\u8FD0\u884C\u89C4\u5219\u7ED1\u5B9A\u3002\u8003\u8BD5\u65F6\u957F\u3001\u542F\u505C\u65B9\u5F0F\u548C\u65F6\u95F4\u52A0\u6263\u5206\u4ECD\u5728\u8D5B\u6BB5\u8FD0\u884C\u89C4\u5219\u4E2D\u914D\u7F6E\u3002" }), _jsxs(Space, { wrap: true, style: { marginBottom: 16 }, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: 260 }, placeholder: "\u9009\u62E9\u8D5B\u4E8B", value: eventId || undefined, loading: eventsQuery.isLoading, options: events.map((event) => ({ value: String(event.id), label: `${event.name}（${event.code}）` })), onChange: value => { setEventId(value); setWorkTypeId(''); setLevelId(''); setSelectedIds([]); setPrecheck(null); } }), _jsx(Select, { style: { width: 230 }, placeholder: "\u9009\u62E9\u5DE5\u79CD", value: workTypeId || undefined, loading: workTypesQuery.isLoading, options: workTypes.map((item) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) })), onChange: value => { setWorkTypeId(value); setLevelId(''); setSelectedIds([]); setPrecheck(null); } }), _jsx(Select, { style: { width: 190 }, placeholder: "\u9009\u62E9\u7B49\u7EA7", value: levelId || undefined, loading: levelsQuery.isLoading, options: levels.map((item) => ({ value: String(item.id), label: `${item.levelName}（${item.levelCode}）` })), onChange: value => { setLevelId(value); setSelectedIds([]); setPrecheck(null); } })] }), currentEvent && currentWorkType && currentLevel && (_jsxs(Typography.Paragraph, { type: "secondary", children: ["\u5F53\u524D\u8303\u56F4\uFF1A", currentEvent.name, " / ", currentWorkType.displayName || currentWorkType.workTypeName || currentWorkType.workTypeId, " / ", currentLevel.levelName] })), _jsx(Table, { rowKey: "id", loading: plansQuery.isLoading || plansQuery.isFetching, dataSource: planRows, pagination: { pageSize: 20, showSizeChanger: false }, scroll: { x: 'max-content' }, columns: [
                    { title: '方案名称', dataIndex: 'title', ellipsis: true },
                    { title: '实操项目数', dataIndex: 'practicalQuestionIds', width: 110, render: (ids) => ids?.length ?? 0 },
                    { title: '方案总分', dataIndex: 'totalScore', width: 100, render: (value) => `${value} 分` },
                    { title: '参考时长', dataIndex: 'durationMinutes', width: 110, render: (value) => `${value} 分钟` },
                    { title: '版本', dataIndex: 'planVersion', width: 90, render: (value) => `v${value}` },
                    { title: '状态', dataIndex: 'status', width: 100, render: (value) => _jsx(Tag, { color: value === 'published' ? 'green' : 'gold', children: value === 'published' ? '已发布' : value === 'draft' ? '草稿' : value }) },
                    {
                        title: '操作',
                        key: 'actions',
                        width: 330,
                        fixed: 'right',
                        render: (_, record) => (_jsxs(Space, { wrap: true, size: [8, 0], children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => { setPreviewPlanId(record.id); setPreviewSnapshotId(null); }, children: "\u9884\u89C8" }), record.status === 'draft' && _jsx(Button, { type: "link", size: "small", onClick: () => void openEdit(record), children: "\u7F16\u8F91" }), record.status === 'draft' && _jsx(Popconfirm, { title: "\u53D1\u5E03\u540E\u65B9\u6848\u5FEB\u7167\u4E0D\u53EF\u4FEE\u6539\uFF0C\u5E76\u53EF\u4F9B\u5339\u914D\u8D5B\u6BB5\u4F7F\u7528\u3002\u786E\u8BA4\u53D1\u5E03\uFF1F", onConfirm: () => publishMutation.mutate(record.id), children: _jsx(Button, { type: "link", size: "small", icon: _jsx(SendOutlined, {}), children: "\u53D1\u5E03" }) }), record.status === 'published' && _jsx(Button, { type: "link", size: "small", onClick: () => openNew(record), children: "\u590D\u5236\u65B0\u7248\u672C" }), record.status === 'draft' && _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u6B64\u8BC4\u5206\u65B9\u6848\u8349\u7A3F\uFF1F", onConfirm: () => deleteMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
                    },
                ] }), _jsx(Modal, { title: `${editing ? '编辑' : '新建'}实操评分方案`, open: formOpen, onCancel: () => setFormOpen(false), onOk: () => form.submit(), confirmLoading: saveMutation.isPending, destroyOnClose: true, width: 1100, children: _jsxs(Form, { form: form, layout: "vertical", onFinish: values => saveMutation.mutate(values), onValuesChange: () => setPrecheck(null), children: [_jsxs(Space, { wrap: true, align: "start", children: [_jsx(Form.Item, { name: "title", label: "\u65B9\u6848\u540D\u79F0", rules: [{ required: true, whitespace: true, message: '请输入方案名称' }], children: _jsx(Input, { maxLength: 100, style: { width: 360 } }) }), _jsx(Form.Item, { label: "\u65B9\u6848\u603B\u5206", children: _jsx(Input, { value: "100 \u5206\uFF08\u56FA\u5B9A\uFF09", readOnly: true, style: { width: 180 } }) }), _jsx(Form.Item, { label: "\u6240\u9009\u9879\u76EE\u53C2\u8003\u65F6\u957F", children: _jsx(Input, { value: `${durationTotal} 分钟`, readOnly: true, style: { width: 180 } }) })] }), _jsx(Typography.Title, { level: 5, children: "\u9009\u62E9\u5B9E\u64CD\u9879\u76EE" }), _jsx(Table, { rowKey: "id", size: "small", loading: questionsQuery.isLoading || questionsQuery.isFetching, dataSource: questions, rowSelection: selectedRowSelection, pagination: { pageSize: 8, showSizeChanger: false }, expandable: {
                                expandedRowRender: question => (_jsx(Table, { size: "small", rowKey: "key", pagination: false, dataSource: flattenElements(question), columns: [
                                        { title: '考评内容', dataIndex: 'content', width: 180, ellipsis: true },
                                        { title: '评分项', dataIndex: 'itemName', width: 150, render: (value, row) => value || row.element || '-' },
                                        { title: '评分标准', dataIndex: 'standard', ellipsis: true },
                                        { title: '配分', dataIndex: 'maxScore', width: 80 },
                                        { title: '最大扣分', dataIndex: 'maxDeduction', width: 90 },
                                        { title: '扣分步长', dataIndex: 'deductionStep', width: 90 },
                                        { title: '参与评分', dataIndex: 'allocationEnabled', width: 90, render: (value) => value ? '是' : '否' },
                                    ] })),
                            }, columns: [
                                { title: '实操项目', dataIndex: 'stem', ellipsis: true },
                                { title: '原始分值', width: 100, render: (_, row) => `${scoreOf(row)} 分` },
                                { title: '参考时长', width: 100, render: (_, row) => `${durationOf(row)} 分钟` },
                                { title: '评分方式', width: 100, render: (_, row) => scoreMethodLabel(row.practicalDetail?.scoringMethod) },
                                { title: '评分项', width: 90, render: (_, row) => row.scoreItems?.length ?? 0 },
                                { title: '状态', dataIndex: 'status', width: 90, render: (value) => _jsx(Tag, { color: value === 'enabled' ? 'green' : 'default', children: value === 'enabled' ? '启用' : '停用' }) },
                            ] }), _jsxs(Space, { style: { marginTop: 16, marginBottom: 8 }, children: [_jsx(Typography.Text, { strong: true, children: "\u65B9\u6848\u7EC4\u6210\u987A\u5E8F" }), _jsx(Typography.Text, { type: "secondary", children: "\u8C03\u6574\u540E\u7684\u987A\u5E8F\u4F1A\u56FA\u5316\u5230\u53D1\u5E03\u5FEB\u7167\u4E2D\u3002" })] }), _jsx(Table, { rowKey: "id", size: "small", pagination: false, dataSource: selectedQuestions, locale: { emptyText: '请先从上方选择一个或多个实操项目' }, columns: [
                                { title: '顺序', width: 70, render: (_, row) => selectedIds.indexOf(row.id) + 1 },
                                { title: '实操项目', dataIndex: 'stem', ellipsis: true },
                                { title: '原始分值', width: 100, render: (_, row) => `${scoreOf(row)} 分` },
                                { title: '参考时长', width: 100, render: (_, row) => `${durationOf(row)} 分钟` },
                                {
                                    title: '操作',
                                    width: 130,
                                    render: (_, row) => {
                                        const index = selectedIds.indexOf(row.id);
                                        return _jsxs(Space, { children: [_jsx(Button, { "aria-label": "\u4E0A\u79FB", size: "small", icon: _jsx(ArrowUpOutlined, {}), disabled: index <= 0, onClick: () => moveSelected(row.id, -1) }), _jsx(Button, { "aria-label": "\u4E0B\u79FB", size: "small", icon: _jsx(ArrowDownOutlined, {}), disabled: index < 0 || index >= selectedIds.length - 1, onClick: () => moveSelected(row.id, 1) }), _jsx(Button, { "aria-label": "\u79FB\u9664", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => { setSelectedIds(selectedIds.filter(id => id !== row.id)); setPrecheck(null); } })] });
                                    },
                                },
                            ] }), _jsxs(Descriptions, { size: "small", bordered: true, column: 3, style: { marginTop: 12, marginBottom: 12 }, children: [_jsx(Descriptions.Item, { label: "\u9879\u76EE\u6570\u91CF", children: selectedIds.length }), _jsx(Descriptions.Item, { label: "\u6240\u9009\u539F\u59CB\u5206\u503C\u5408\u8BA1", children: _jsxs(Typography.Text, { type: scoreTotalCents === 10000 ? 'success' : 'danger', children: [scoreTotal, " / 100 \u5206"] }) }), _jsxs(Descriptions.Item, { label: "\u53C2\u8003\u65F6\u957F\u5408\u8BA1", children: [durationTotal, " \u5206\u949F"] })] }), selectedIds.length > 0 && scoreTotalCents !== 10000 && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u6240\u9009\u9879\u76EE\u539F\u59CB\u5206\u503C\u5FC5\u987B\u521A\u597D\u5408\u8BA1100\u5206\uFF1B\u65B9\u6848\u4E0D\u4F1A\u81EA\u52A8\u7F29\u653E\u9879\u76EE\u5206\u503C\u3002" })), precheck && _jsx(Alert, { type: precheck.ready ? 'success' : 'error', showIcon: true, style: { marginBottom: 12 }, message: precheck.ready ? '方案校验通过' : precheck.errors.join('；') || '方案校验未通过' }), _jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsx(Button, { loading: precheckMutation.isPending, disabled: !selectedIds.length || scoreTotalCents !== 10000 || !durationTotal, onClick: () => form.validateFields().then(values => precheckMutation.mutate(values)).catch(() => undefined), children: "\u6821\u9A8C\u65B9\u6848" }), _jsx(Form.Item, { name: "comments", label: "\u5907\u6CE8", style: { marginBottom: 0 }, children: _jsx(Input.TextArea, { rows: 2, maxLength: 500 }) })] })] }) }), _jsx(Modal, { title: "\u5B9E\u64CD\u8BC4\u5206\u65B9\u6848\u5FEB\u7167\u9884\u89C8", open: Boolean(previewPlanId), onCancel: () => { setPreviewPlanId(null); setPreviewSnapshotId(null); }, footer: null, width: 1000, styles: { body: { maxHeight: 'calc(100vh - 180px)', overflowY: 'auto', padding: 0 } }, children: snapshotsQuery.isLoading ? _jsx(Spin, {}) : snapshotsQuery.data ? _jsxs(_Fragment, { children: [_jsx(Space, { wrap: true, style: { padding: '16px 20px 0' }, children: snapshotsQuery.data.map((snapshot) => (_jsxs(Button, { type: snapshot.id === previewSnapshotId ? 'primary' : 'default', onClick: () => setPreviewSnapshotId(snapshot.id), children: [snapshot.paperCode, " \u5377 / v", snapshot.versionNo, " ", _jsx(Tag, { children: snapshot.status })] }, snapshot.id))) }), previewQuery.isLoading ? _jsx(Spin, {}) : previewQuery.data
                            ? _jsx(CompetitionPracticalSnapshotPaperPreview, { snapshot: previewQuery.data })
                            : _jsx(Alert, { type: "warning", message: "\u65E0\u6CD5\u52A0\u8F7D\u8BC4\u5206\u65B9\u6848\u5FEB\u7167" })] }) : _jsx(Alert, { type: "warning", message: "\u6682\u65E0\u5FEB\u7167" }) })] }));
}
