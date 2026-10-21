import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, DownloadOutlined, EditOutlined, EyeOutlined, PlusOutlined, UploadOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Descriptions, Form, Input, InputNumber, Modal, Popconfirm, Row, Select, Space, Spin, Table, Tag, Typography, Upload, message, } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { createCompetitionQuestion, batchBindCompetitionQuestions, deleteCompetitionQuestion, downloadCompetitionQuestionTemplate, getCompetitionQuestion, getCompetitionQuestionImportBatch, importCompetitionQuestions, importCompetitionSimplePracticalDetails, importCompetitionSimpleTheoryQuestions, pageCompetitionQuestions, updateCompetitionQuestion, } from '../../api/competitionQuestion';
import { listCompetitionEventWorkTypeLevels, listCompetitionEventWorkTypes, pageCompetitionEvents, } from '../../api/competition';
import { getCompetitionQuestionBlueprintTree, pageCompetitionQuestionBlueprints } from '../../api/competitionQuestionBlueprint';
import { SimplePracticalQuestionCreateModal } from './SimplePracticalQuestionCreateModal';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import { PracticalQuestionEditorModal } from './PracticalQuestionEditorModal';
const OPTION_KEYS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
const typeLabels = {
    single: '单选题',
    multi: '多选题',
    judge: '判断题',
    fill: '填空题',
    practical: '实操题',
};
const typeColors = {
    single: 'blue',
    multi: 'purple',
    judge: 'orange',
    fill: 'cyan',
    practical: 'green',
};
const judgeAnswerOptions = [
    { value: 'T', label: 'T（正确）' },
    { value: 'F', label: 'F（错误）' },
];
function questionTypeTag(value) {
    const type = value?.trim().toLowerCase() || '';
    return _jsx(Tag, { color: typeColors[type] || 'default', children: typeLabels[type] || value || '-' });
}
function parseOptionsJson(optionsJson) {
    if (!optionsJson)
        return {};
    try {
        const parsed = JSON.parse(optionsJson);
        if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
            return {};
        const source = parsed;
        return OPTION_KEYS.reduce((options, key) => {
            if (typeof source[key] === 'string')
                options[key] = source[key];
            return options;
        }, {});
    }
    catch {
        return {};
    }
}
function isChoiceQuestion(questionType) {
    return questionType === 'single' || questionType === 'multi';
}
function serializeOptions(questionType, options) {
    if (!isChoiceQuestion(questionType))
        return undefined;
    const entries = OPTION_KEYS
        .map((key) => [key, options?.[key]?.trim() || ''])
        .filter(([, value]) => value.length > 0);
    return entries.length > 0 ? JSON.stringify(Object.fromEntries(entries)) : undefined;
}
function getEventIdFromUrl() {
    return new URLSearchParams(window.location.search).get('eventId') || '';
}
function plainText(value) {
    return value?.replace(/<br\s*\/?\s*>/gi, ' ').replace(/<[^>]*>/g, ' ').replace(/&nbsp;/gi, ' ').replace(/\s+/g, ' ').trim() || '-';
}
function practicalScoringMethodLabel(value) {
    return value === 'additive' ? '加分制' : value === 'deductive' ? '扣分制' : value || '-';
}
export function CompetitionQuestionBankPage({ contentType, simpleMode = false }) {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停导入批次轮询（§3.6）
    const [eventId, setEventId] = useState(getEventIdFromUrl);
    const [workTypeId, setWorkTypeId] = useState('');
    const [levelId, setLevelId] = useState('');
    const [keyword, setKeyword] = useState('');
    const [selectedSheet, setSelectedSheet] = useState('');
    const [questionType, setQuestionType] = useState();
    const [bindingStatus, setBindingStatus] = useState();
    const [blueprintNodeId, setBlueprintNodeId] = useState();
    const [selectedQuestionIds, setSelectedQuestionIds] = useState([]);
    const [bindingOpen, setBindingOpen] = useState(false);
    const [bindingTargetNodeId, setBindingTargetNodeId] = useState();
    const [page, setPage] = useState(1);
    const [editing, setEditing] = useState(null);
    const [formOpen, setFormOpen] = useState(false);
    const [simplePracticalCreateOpen, setSimplePracticalCreateOpen] = useState(false);
    const [previewing, setPreviewing] = useState(null);
    const [batchId, setBatchId] = useState(null);
    const [form] = Form.useForm();
    const selectedQuestionType = Form.useWatch('questionType', form);
    const effectiveQuestionType = selectedQuestionType || (contentType === 'theory' ? 'single' : 'practical');
    const eventsQuery = useQuery({
        queryKey: ['competition-events', 'question-bank'],
        queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 200 }),
    });
    const events = eventsQuery.data?.items ?? [];
    const workTypesQuery = useQuery({
        queryKey: ['competition-event-work-types', eventId],
        queryFn: () => listCompetitionEventWorkTypes(eventId),
        enabled: Boolean(eventId),
    });
    const workTypes = workTypesQuery.data ?? [];
    const levelsQuery = useQuery({
        queryKey: ['competition-event-levels', eventId, workTypeId],
        queryFn: () => listCompetitionEventWorkTypeLevels(eventId, workTypeId),
        enabled: Boolean(eventId && workTypeId),
    });
    const levels = levelsQuery.data ?? [];
    const blueprintQuery = useQuery({
        queryKey: ['competition-active-blueprint', eventId, workTypeId, levelId, contentType],
        queryFn: () => pageCompetitionQuestionBlueprints(eventId, {
            page: 1, pageSize: 20, eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId, contentType, status: 'active',
        }),
        enabled: !simpleMode && Boolean(eventId && workTypeId && levelId),
    });
    const activeBlueprint = simpleMode ? undefined : blueprintQuery.data?.items?.[0];
    const blueprintTreeQuery = useQuery({
        queryKey: ['competition-active-blueprint-tree', eventId, activeBlueprint?.id],
        queryFn: () => getCompetitionQuestionBlueprintTree(eventId, activeBlueprint.id),
        enabled: Boolean(eventId && activeBlueprint?.id && formOpen),
    });
    const blueprintNodes = useMemo(() => {
        const rows = [];
        const walk = (nodes, prefix = '') => nodes.forEach(node => {
            const path = prefix ? `${prefix} / ${node.name}` : node.name;
            if (!node.children?.length)
                rows.push({ value: node.id, label: `${node.fullCode} ${path}` });
            else
                walk(node.children, path);
        });
        if (blueprintTreeQuery.data?.tree)
            walk(blueprintTreeQuery.data.tree);
        return rows;
    }, [blueprintTreeQuery.data]);
    const questionsQuery = useQuery({
        queryKey: ['competition-questions', eventId, workTypeId, levelId, contentType, questionType, keyword, bindingStatus, blueprintNodeId, activeBlueprint?.id, simpleMode, page],
        queryFn: () => pageCompetitionQuestions(eventId, {
            page,
            pageSize: 20,
            eventWorkTypeId: workTypeId,
            eventWorkTypeLevelId: levelId,
            contentType,
            questionType,
            keyword: keyword || undefined,
            blueprintId: activeBlueprint?.id,
            blueprintNodeId,
            bindingStatus,
            unboundOnly: simpleMode || undefined,
        }),
        enabled: Boolean(eventId && workTypeId && levelId),
    });
    const previewQuery = useQuery({
        queryKey: ['competition-question', eventId, previewing],
        queryFn: () => getCompetitionQuestion(eventId, previewing),
        enabled: Boolean(eventId && previewing),
    });
    const batchQuery = useQuery({
        queryKey: ['competition-question-import-batch', eventId, batchId],
        queryFn: () => getCompetitionQuestionImportBatch(eventId, batchId),
        enabled: Boolean(eventId && batchId),
        refetchInterval: paneActive && batchId ? 1500 : false,
    });
    useEffect(() => {
        if (!eventId && events[0])
            setEventId(String(events[0].id));
    }, [eventId, events]);
    useEffect(() => {
        if (!workTypeId && workTypes[0])
            setWorkTypeId(String(workTypes[0].id));
    }, [workTypeId, workTypes]);
    useEffect(() => {
        if (!levelId && levels[0])
            setLevelId(String(levels[0].id));
    }, [levelId, levels]);
    useEffect(() => {
        if (batchQuery.data?.status === 'succeeded') {
            queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
        }
    }, [batchQuery.data?.status, queryClient]);
    const saveMutation = useMutation({
        mutationFn: async (values) => {
            const questionType = values.questionType;
            const referenceAnswer = isChoiceQuestion(questionType) || questionType === 'judge'
                ? values.referenceAnswer?.trim().toUpperCase()
                : undefined;
            const body = {
                eventWorkTypeId: values.eventWorkTypeId,
                eventWorkTypeLevelId: values.eventWorkTypeLevelId,
                contentType,
                questionType,
                stem: values.stem,
                referenceAnswer,
                fillAnswer: questionType === 'fill' ? values.fillAnswer?.trim() : undefined,
                optionsJson: serializeOptions(questionType, values.options),
                difficulty: values.difficulty,
                status: values.status,
            };
            // Historical questions may remain unbound and editable.  Do not send
            // the current active blueprint implicitly during an edit; only a newly
            // selected node creates/replaces a binding.  New questions still carry
            // the required blueprint/node from the form.
            if (!editing) {
                body.blueprintId = values.blueprintId;
                body.blueprintNodeId = values.blueprintNodeId;
            }
            else if (values.blueprintNodeId && values.blueprintNodeId !== editing.blueprintNodeId) {
                body.blueprintId = activeBlueprint?.id;
                body.blueprintNodeId = values.blueprintNodeId;
            }
            return editing
                ? updateCompetitionQuestion(eventId, editing.id, body)
                : createCompetitionQuestion(eventId, body);
        },
        onSuccess: () => {
            message.success(editing ? '竞赛试题已更新' : '竞赛试题已创建');
            setEditing(null);
            setFormOpen(false);
            form.resetFields();
            queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (id) => deleteCompetitionQuestion(eventId, id),
        onSuccess: () => {
            message.success('竞赛试题已删除');
            queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
        },
    });
    const importMutation = useMutation({
        mutationFn: (file) => simpleMode
            ? contentType === 'practical'
                ? importCompetitionSimplePracticalDetails(eventId, {
                    eventWorkTypeId: workTypeId,
                    eventWorkTypeLevelId: levelId,
                }, file)
                : importCompetitionSimpleTheoryQuestions(eventId, {
                    eventWorkTypeId: workTypeId,
                    eventWorkTypeLevelId: levelId,
                }, file)
            : importCompetitionQuestions(eventId, {
                eventWorkTypeId: workTypeId,
                eventWorkTypeLevelId: levelId,
                contentType,
                blueprintId: activeBlueprint.id,
                selectedSheet: selectedSheet.trim() || undefined,
            }, file),
        onSuccess: (batch) => {
            setBatchId(batch.id);
            message.success('文件已接收，正在后台校验');
        },
    });
    const bindingMutation = useMutation({
        mutationFn: () => batchBindCompetitionQuestions(eventId, {
            blueprintId: activeBlueprint.id,
            blueprintNodeId: bindingTargetNodeId,
            questionIds: selectedQuestionIds,
        }),
        onSuccess: () => {
            message.success(`已绑定 ${selectedQuestionIds.length} 道历史试题`);
            setSelectedQuestionIds([]);
            setBindingTargetNodeId(undefined);
            setBindingOpen(false);
            queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
            queryClient.invalidateQueries({ queryKey: ['competition-blueprint-coverage'] });
        },
    });
    const openEdit = (record) => {
        if (contentType === 'practical') {
            setEditing(record);
            setFormOpen(true);
            return;
        }
        setEditing(record);
        setFormOpen(true);
        form.setFieldsValue({
            eventWorkTypeId: record.eventWorkTypeId,
            eventWorkTypeLevelId: record.eventWorkTypeLevelId,
            questionType: record.questionType,
            stem: record.stem,
            referenceAnswer: record.questionType === 'judge'
                ? record.referenceAnswer?.trim().toUpperCase() || undefined
                : record.referenceAnswer || undefined,
            fillAnswer: record.fillAnswer || undefined,
            options: parseOptionsJson(record.optionsJson),
            difficulty: record.difficulty || undefined,
            status: record.status,
            blueprintId: record.blueprintId || undefined,
            blueprintNodeId: record.blueprintNodeId || undefined,
        });
    };
    const handleQuestionTypeChange = (value) => {
        form.setFieldsValue({
            questionType: value,
            referenceAnswer: undefined,
            fillAnswer: undefined,
            options: undefined,
        });
    };
    const uploadProps = {
        accept: '.xlsx,.xls',
        showUploadList: false,
        beforeUpload: (file) => {
            importMutation.mutate(file);
            return Upload.LIST_IGNORE;
        },
    };
    const typeOptions = useMemo(() => contentType === 'theory'
        ? ['single', 'multi', 'judge', 'fill'].map(value => ({ value, label: typeLabels[value] }))
        : [{ value: 'practical', label: '实操题' }], [contentType]);
    const data = questionsQuery.data;
    const downloadTemplate = async () => {
        const blob = await downloadCompetitionQuestionTemplate(contentType, simpleMode);
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = simpleMode
            ? contentType === 'practical' ? '竞赛简易实操评分明细表模板.xlsx' : '竞赛简易理论试题导入模板.xlsx'
            : contentType === 'theory' ? '竞赛理论试题模板.xlsx' : '竞赛实操试题模板.xlsx';
        anchor.click();
        URL.revokeObjectURL(url);
    };
    const answerRules = (type) => {
        if (type === 'judge') {
            return [
                { required: true, message: '请输入参考答案' },
                { pattern: /^[TF]$/i, message: '判断题参考答案只能是 T 或 F' },
            ];
        }
        if (type === 'single' || type === 'multi') {
            return [
                { required: true, message: '请输入参考答案' },
                {
                    validator: (_rule, value) => {
                        const letters = String(value ?? '').toUpperCase().replace(/[^A-H]/g, '');
                        const options = form.getFieldValue('options');
                        const selected = letters.split('');
                        if (type === 'single' && letters.length !== 1) {
                            return Promise.reject(new Error('单选题参考答案只能填写一个 A-H 选项'));
                        }
                        if (type === 'multi' && letters.length < 2) {
                            return Promise.reject(new Error('多选题参考答案至少填写两个 A-H 选项'));
                        }
                        if (new Set(selected).size !== selected.length) {
                            return Promise.reject(new Error('参考答案不能重复选择同一选项'));
                        }
                        if (selected.some((key) => !options?.[key]?.trim())) {
                            return Promise.reject(new Error('参考答案对应的选项必须填写内容'));
                        }
                        return Promise.resolve();
                    },
                },
            ];
        }
        return [{ required: true, message: '请输入回答' }];
    };
    const renderTheoryAnswerFields = () => {
        if (isChoiceQuestion(effectiveQuestionType)) {
            return (_jsxs(_Fragment, { children: [_jsx(Typography.Text, { strong: true, children: "\u9009\u9879" }), _jsx(Row, { gutter: [12, 0], style: { marginTop: 8 }, children: OPTION_KEYS.map((key) => (_jsx(Col, { span: 12, children: _jsx(Form.Item, { name: ['options', key], label: `选项 ${key}`, children: _jsx(Input, { placeholder: `请输入选项 ${key}` }) }) }, key))) }), _jsx(Form.Item, { name: "referenceAnswer", label: "\u53C2\u8003\u7B54\u6848", dependencies: ['options'], rules: answerRules(effectiveQuestionType), extra: effectiveQuestionType === 'single' ? '请输入一个选项字母，例如 A' : '请输入两个或以上选项字母，例如 AC', children: _jsx(Input, { placeholder: effectiveQuestionType === 'single' ? '例如：A' : '例如：AC' }) })] }));
        }
        if (effectiveQuestionType === 'judge') {
            return (_jsx(Form.Item, { name: "referenceAnswer", label: "\u7B54\u6848\u9009\u9879", rules: answerRules(effectiveQuestionType), extra: "\u8BF7\u9009\u62E9 T\uFF08\u6B63\u786E\uFF09\u6216 F\uFF08\u9519\u8BEF\uFF09", children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9 T \u6216 F", options: judgeAnswerOptions }) }));
        }
        return (_jsx(Form.Item, { name: "fillAnswer", label: "\u56DE\u7B54", rules: answerRules('fill'), children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u586B\u7A7A\u9898\u56DE\u7B54" }) }));
    };
    return (_jsxs(Card, { title: simpleMode
            ? contentType === 'practical' ? '简易实操评分项' : '简易理论试题'
            : `竞赛${contentType === 'theory' ? '理论' : '实操'}试题`, extra: (_jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: downloadTemplate, children: "\u4E0B\u8F7D\u5BFC\u5165\u6A21\u677F" }), contentType === 'practical' && !simpleMode && _jsx(Input, { allowClear: true, value: selectedSheet, onChange: (event) => setSelectedSheet(event.target.value), placeholder: "\u5B9E\u64CD\u5DE5\u4F5C\u8868\uFF08\u53EF\u9009\uFF09", style: { width: 170 } }), !simpleMode && _jsx(Button, { disabled: !activeBlueprint || selectedQuestionIds.length === 0, onClick: () => setBindingOpen(true), children: "\u6279\u91CF\u7ED1\u5B9A\u8003\u8BC4\u70B9" }), simpleMode && contentType === 'practical' && _jsx(Button, { icon: _jsx(PlusOutlined, {}), disabled: !eventId || !workTypeId || !levelId, onClick: () => setSimplePracticalCreateOpen(true), children: "\u65B0\u589E\u9879\u76EE" }), _jsx(Upload, { ...uploadProps, disabled: !eventId || !workTypeId || !levelId || (!simpleMode && !activeBlueprint) || importMutation.isPending, children: _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), children: simpleMode
                            ? contentType === 'practical' ? '导入评分明细表' : '批量导入试题'
                            : '导入试题' }) }), !simpleMode && _jsx(Button, { icon: _jsx(PlusOutlined, {}), disabled: !activeBlueprint, onClick: () => {
                        setEditing(null);
                        form.resetFields();
                        form.setFieldsValue({
                            eventWorkTypeId: workTypeId, eventWorkTypeLevelId: levelId,
                            questionType: contentType === 'theory' ? 'single' : 'practical', status: 'enabled', difficulty: 2,
                            blueprintId: activeBlueprint?.id,
                        });
                        setFormOpen(true);
                    }, children: "\u65B0\u589E\u8BD5\u9898" })] })), children: [simpleMode
                ? _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: contentType === 'practical'
                        ? '实操评分项按赛事、工种和等级管理，无需配置细目表。每次导入一个考核项目，上传包含评分明细的 Excel 表。'
                        : '简易理论试题按赛事、工种和等级管理，导入时无需配置细目表。当前支持单选、多选、判断和填空题。' })
                : !activeBlueprint && eventId && workTypeId && levelId
                    ? _jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u5F53\u524D\u8303\u56F4\u6CA1\u6709\u542F\u7528\u7684\u7ADE\u8D5B\u7EC6\u76EE\u8868\uFF0C\u8BF7\u5148\u5BFC\u5165\u5E76\u542F\u7528\u7406\u8BBA/\u5B9E\u64CD\u7EC6\u76EE\u8868\uFF1B\u672A\u7ED1\u5B9A\u9898\u76EE\u4E0D\u4F1A\u8FDB\u5165\u81EA\u52A8\u7EC4\u5377\u3002" })
                    : _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u7ADE\u8D5B\u9898\u5E93\u4E0E\u8BFE\u7A0B\u9898\u5E93\u5B8C\u5168\u72EC\u7ACB\uFF1B\u9898\u5E93\u5BFC\u5165\u5FC5\u987B\u7ED1\u5B9A\u5F53\u524D\u542F\u7528\u7EC6\u76EE\u8868\u7684\u672B\u7EA7\u8003\u8BC4\u70B9\uFF0C\u5386\u53F2\u672A\u7ED1\u5B9A\u9898\u76EE\u4ECD\u53EF\u7F16\u8F91\u4F46\u4E0D\u4F1A\u8FDB\u5165\u81EA\u52A8\u7EC4\u5377\u3002" }), _jsxs(Space, { wrap: true, style: { marginBottom: 16 }, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: 240 }, placeholder: "\u9009\u62E9\u8D5B\u4E8B", value: eventId || undefined, options: events.map((event) => ({ value: String(event.id), label: `${event.name}（${event.code}）` })), onChange: (value) => { setEventId(value); setWorkTypeId(''); setLevelId(''); setBindingStatus(undefined); setBlueprintNodeId(undefined); setSelectedQuestionIds([]); setPage(1); } }), _jsx(Select, { style: { width: 210 }, placeholder: "\u9009\u62E9\u5DE5\u79CD", value: workTypeId || undefined, options: workTypes.map((item) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) })), onChange: (value) => { setWorkTypeId(value); setLevelId(''); setBindingStatus(undefined); setBlueprintNodeId(undefined); setSelectedQuestionIds([]); setPage(1); } }), _jsx(Select, { style: { width: 180 }, placeholder: "\u9009\u62E9\u7B49\u7EA7", value: levelId || undefined, options: levels.map((item) => ({ value: String(item.id), label: `${item.levelName}（${item.levelCode}）` })), onChange: (value) => { setLevelId(value); setBindingStatus(undefined); setBlueprintNodeId(undefined); setSelectedQuestionIds([]); setPage(1); } }), !(simpleMode && contentType === 'practical') && _jsx(Select, { allowClear: true, style: { width: 140 }, placeholder: "\u9898\u578B", value: questionType, options: typeOptions, onChange: (value) => { setQuestionType(value); setPage(1); } }), !simpleMode && _jsx(Select, { allowClear: true, style: { width: 130 }, placeholder: "\u7ED1\u5B9A\u72B6\u6001", value: bindingStatus, disabled: !activeBlueprint, options: [{ value: 'bound', label: '已绑定' }, { value: 'unbound', label: '未绑定' }], onChange: (value) => { setBindingStatus(value); setPage(1); } }), !simpleMode && _jsx(Select, { allowClear: true, showSearch: true, optionFilterProp: "label", style: { width: 260 }, placeholder: "\u8003\u8BC4\u70B9", value: blueprintNodeId, disabled: !activeBlueprint, options: blueprintNodes, onChange: (value) => { setBlueprintNodeId(value); setPage(1); } }), _jsx(Input.Search, { allowClear: true, placeholder: simpleMode && contentType === 'practical' ? '搜索考核项目' : '搜索题干', style: { width: 240 }, value: keyword, onChange: (event) => setKeyword(event.target.value), onSearch: () => setPage(1) })] }), _jsx(Table, { rowKey: "id", loading: questionsQuery.isLoading || questionsQuery.isFetching, rowSelection: simpleMode ? undefined : { selectedRowKeys: selectedQuestionIds, onChange: (keys) => setSelectedQuestionIds(keys.map(String)) }, dataSource: data?.items ?? [], pagination: { current: data?.page ?? page, pageSize: data?.pageSize ?? 20, total: data?.total ?? 0, onChange: setPage, showSizeChanger: false }, columns: simpleMode && contentType === 'practical' ? [
                    { title: '考核项目', dataIndex: 'stem', ellipsis: true, render: (value) => _jsx(Typography.Text, { ellipsis: { tooltip: true }, children: value }) },
                    { title: '考核内容', width: 260, render: (_, record) => {
                            const content = record.practicalDetail?.assessmentContents?.map(item => plainText(item.contentHtml)).filter(item => item !== '-') ?? [];
                            return _jsx(Typography.Text, { ellipsis: { tooltip: content.join('；') }, children: content.join('；') || plainText(record.operationContent) });
                        } },
                    { title: '评分项', width: 90, render: (_, record) => record.scoreItems?.length ?? 0 },
                    { title: '计分方式', width: 100, render: (_, record) => practicalScoringMethodLabel(record.practicalDetail?.scoringMethod) },
                    { title: '总分', width: 90, render: (_, record) => record.practicalDetail?.totalScore ?? record.scoreItems?.reduce((total, item) => total + Number(item.maxScore || 0), 0) ?? '-' },
                    { title: '状态', dataIndex: 'status', width: 90, render: (value) => value === 'enabled' ? _jsx(Tag, { color: "green", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }) },
                    { title: '操作', key: 'actions', width: 180, render: (_, record) => (_jsxs(Space, { size: 0, style: { whiteSpace: 'nowrap' }, children: [_jsx(Button, { type: "link", size: "small", style: { paddingInline: 0 }, icon: _jsx(EyeOutlined, {}), onClick: () => setPreviewing(record.id), children: "\u67E5\u770B\u8BC4\u5206\u660E\u7EC6" }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u6B64\u5B9E\u64CD\u8BC4\u5206\u9879\uFF1F", onConfirm: () => deleteMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", style: { paddingInline: 0 }, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })) },
                ] : [
                    { title: '序号', dataIndex: 'sortOrder', width: 80 },
                    { title: '题型', dataIndex: 'questionType', width: 100, render: questionTypeTag },
                    { title: '题干', dataIndex: 'stem', ellipsis: true, render: (value) => _jsx(Typography.Text, { ellipsis: { tooltip: true }, children: value }) },
                    { title: '难度', dataIndex: 'difficulty', width: 90, render: (value) => value ? ['-', '简单', '中等', '困难', '较难', '极难'][value] || value : '-' },
                    ...(!simpleMode ? [{ title: '绑定状态', dataIndex: 'bindingStatus', width: 100, render: (value) => value === 'bound' ? _jsx(Tag, { color: "green", children: "\u5DF2\u7ED1\u5B9A" }) : _jsx(Tag, { color: "orange", children: "\u672A\u7ED1\u5B9A" }) }] : []),
                    { title: '状态', dataIndex: 'status', width: 90, render: (value) => value === 'enabled' ? _jsx(Tag, { color: "green", children: "\u542F\u7528" }) : _jsx(Tag, { children: "\u505C\u7528" }) },
                    { title: '操作', key: 'actions', width: 170, render: (_, record) => (_jsxs(Space, { size: 0, style: { whiteSpace: 'nowrap' }, children: [_jsx(Button, { type: "link", size: "small", style: { paddingInline: 0 }, icon: _jsx(EyeOutlined, {}), onClick: () => setPreviewing(record.id), children: "\u9884\u89C8" }), !simpleMode && _jsx(Button, { type: "link", size: "small", style: { paddingInline: 0 }, icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u6B64\u7ADE\u8D5B\u8BD5\u9898\uFF1F", onConfirm: () => deleteMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", style: { paddingInline: 0 }, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })) },
                ] }), _jsxs(Modal, { title: `批量绑定 ${selectedQuestionIds.length} 道历史试题`, open: bindingOpen, onCancel: () => { setBindingOpen(false); setBindingTargetNodeId(undefined); }, onOk: () => bindingMutation.mutate(), okButtonProps: { disabled: !bindingTargetNodeId }, confirmLoading: bindingMutation.isPending, children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u53EA\u80FD\u9009\u62E9\u5F53\u524D\u542F\u7528\u7EC6\u76EE\u8868\u7684\u672B\u7EA7\u8003\u8BC4\u70B9\uFF1B\u7ED1\u5B9A\u540E\u9898\u76EE\u624D\u4F1A\u8FDB\u5165\u65B0\u7684\u81EA\u52A8\u7EC4\u5377\u3002", style: { marginBottom: 12 } }), _jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: '100%' }, value: bindingTargetNodeId, options: blueprintNodes, onChange: setBindingTargetNodeId, placeholder: "\u9009\u62E9\u672B\u7EA7\u8003\u8BC4\u70B9" })] }), contentType === 'practical' && (_jsx(PracticalQuestionEditorModal, { open: formOpen, eventId: eventId, workTypeId: workTypeId, levelId: levelId, workTypes: workTypes, levels: levels, editing: editing, blueprintId: activeBlueprint?.id, blueprintNodes: blueprintNodes, onClose: () => { setEditing(null); setFormOpen(false); form.resetFields(); }, onSaved: () => { setEditing(null); setFormOpen(false); form.resetFields(); queryClient.invalidateQueries({ queryKey: ['competition-questions'] }); } })), simpleMode && contentType === 'practical' && (_jsx(SimplePracticalQuestionCreateModal, { open: simplePracticalCreateOpen, eventId: eventId, workTypeId: workTypeId, levelId: levelId, onClose: () => setSimplePracticalCreateOpen(false), onSaved: () => {
                    setSimplePracticalCreateOpen(false);
                    queryClient.invalidateQueries({ queryKey: ['competition-questions'] });
                } })), contentType === 'theory' && _jsx(Modal, { title: editing ? '编辑竞赛试题' : '新增竞赛试题', open: formOpen, onCancel: () => { setEditing(null); setFormOpen(false); form.resetFields(); }, onOk: () => form.submit(), confirmLoading: saveMutation.isPending, destroyOnClose: true, width: 760, children: _jsxs(Form, { form: form, layout: "vertical", onFinish: (values) => saveMutation.mutate(values), children: [_jsxs(Space, { wrap: true, align: "start", children: [_jsx(Form.Item, { name: "eventWorkTypeId", label: "\u5DE5\u79CD", rules: [{ required: true }], children: _jsx(Select, { style: { width: 220 }, options: workTypes.map(item => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) })) }) }), _jsx(Form.Item, { name: "eventWorkTypeLevelId", label: "\u7B49\u7EA7", rules: [{ required: true }], children: _jsx(Select, { style: { width: 170 }, options: levels.map(item => ({ value: String(item.id), label: item.levelName })) }) }), _jsx(Form.Item, { name: "blueprintId", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "questionType", label: "\u9898\u578B", rules: [{ required: true }], children: _jsx(Select, { style: { width: 130 }, options: typeOptions, onChange: (value) => handleQuestionTypeChange(value) }) }), activeBlueprint && _jsx(Form.Item, { name: "blueprintNodeId", label: "\u8003\u8BC4\u70B9", rules: [{ required: !editing, message: '请选择末级考评点' }], children: _jsx(Select, { allowClear: true, showSearch: true, optionFilterProp: "label", style: { width: 300 }, options: blueprintNodes, placeholder: editing ? '如需绑定可选择末级考评点' : '选择细目表末级考评点' }) }), _jsx(Form.Item, { label: "\u5E8F\u53F7", extra: editing ? '按创建顺序生成，编辑时不可修改' : '保存后按创建顺序自动生成', children: _jsx(Input, { disabled: true, value: editing ? String(editing.sortOrder) : undefined, placeholder: "\u81EA\u52A8\u751F\u6210" }) }), _jsx(Form.Item, { name: "difficulty", label: "\u96BE\u5EA6", children: _jsx(InputNumber, { min: 1, max: 5 }) })] }), _jsx(Form.Item, { name: "stem", label: "\u9898\u5E72", rules: [{ required: true }], children: _jsx(Input.TextArea, { rows: 4 }) }), renderTheoryAnswerFields(), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: [{ value: 'enabled', label: '启用' }, { value: 'disabled', label: '停用' }] }) })] }) }), _jsx(Modal, { title: simpleMode && contentType === 'practical' ? '实操评分明细' : '竞赛试题预览', open: Boolean(previewing), onCancel: () => setPreviewing(null), footer: null, width: simpleMode && contentType === 'practical' ? 1120 : 720, children: previewQuery.isLoading ? _jsx(Spin, {}) : previewQuery.data ? _jsxs(_Fragment, { children: [_jsxs(Descriptions, { column: 3, size: "small", style: { marginBottom: 16 }, children: [!simpleMode && _jsx(Descriptions.Item, { label: "\u9898\u53F7", children: previewQuery.data.sortOrder }), !simpleMode && _jsx(Descriptions.Item, { label: "\u9898\u578B", children: questionTypeTag(previewQuery.data.questionType) }), simpleMode && contentType === 'practical' && _jsx(Descriptions.Item, { label: "\u8003\u6838\u9879\u76EE", children: previewQuery.data.stem }), simpleMode && contentType === 'practical' && _jsx(Descriptions.Item, { label: "\u8BA1\u5206\u65B9\u5F0F", children: practicalScoringMethodLabel(previewQuery.data.practicalDetail?.scoringMethod) }), simpleMode && contentType === 'practical' && _jsx(Descriptions.Item, { label: "\u603B\u5206", children: previewQuery.data.practicalDetail?.totalScore ?? '-' })] }), !simpleMode && _jsx(Typography.Paragraph, { children: previewQuery.data.stem }), previewQuery.data.optionsJson && _jsxs(Typography.Paragraph, { type: "secondary", children: ["\u9009\u9879\uFF1A", previewQuery.data.optionsJson] }), contentType === 'practical' && _jsx(_Fragment, { children: simpleMode ? _jsx(Table, { size: "small", rowKey: "id", pagination: false, dataSource: previewQuery.data.practicalDetail?.assessmentContents?.flatMap(content => content.elements.map(element => ({
                                    ...element,
                                    assessmentContent: plainText(content.contentHtml),
                                    standard: element.standardHtml,
                                    scoringMethod: previewQuery.data.practicalDetail?.scoringMethod,
                                    stepText: element.elementHtml || String(element.elementOrder ?? ''),
                                }))) ?? [], columns: [
                                    { title: '考核项目', dataIndex: 'stem', render: () => previewQuery.data.stem },
                                    { title: '考核内容', dataIndex: 'assessmentContent' },
                                    { title: '评分项', dataIndex: 'itemName' },
                                    { title: '评分标准', dataIndex: 'standard', render: (value) => plainText(value) },
                                    { title: '计分方式', dataIndex: 'scoringMethod', render: (value) => practicalScoringMethodLabel(value) },
                                    { title: '步骤', dataIndex: 'stepText', render: (value) => plainText(value) },
                                    { title: '分值', dataIndex: 'maxScore' },
                                ] }) : _jsxs(_Fragment, { children: [_jsxs(Typography.Paragraph, { children: [_jsx("b", { children: "\u64CD\u4F5C\u5185\u5BB9\uFF1A" }), previewQuery.data.operationContent || '-'] }), _jsxs(Typography.Paragraph, { children: [_jsx("b", { children: "\u8003\u751F\u51C6\u5907\uFF1A" }), previewQuery.data.studentPrepare || '-'] }), _jsxs(Typography.Paragraph, { children: [_jsx("b", { children: "\u8BBE\u5907\u51C6\u5907\uFF1A" }), previewQuery.data.equipmentPrepare || '-'] }), _jsx(Table, { size: "small", rowKey: "id", pagination: false, dataSource: previewQuery.data.scoreItems, columns: [{ title: '序号', dataIndex: 'itemNo' }, { title: '评分要素', dataIndex: 'itemName' }, { title: '评分标准', dataIndex: 'criterion' }, { title: '最大分值', dataIndex: 'maxScore' }, { title: '最大扣分', dataIndex: 'maxDeduction' }, { title: '步长', dataIndex: 'deductionStep' }] })] }) })] }) : _jsx(Alert, { type: "warning", message: "\u65E0\u6CD5\u52A0\u8F7D\u9898\u76EE" }) }), _jsx(Modal, { title: "\u5BFC\u5165\u8FDB\u5EA6", open: Boolean(batchId), onCancel: () => setBatchId(null), footer: null, children: batchQuery.data ? _jsx(_Fragment, { children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsxs(Typography.Text, { children: ["\u6587\u4EF6\uFF1A", batchQuery.data.originalFilename] }), batchQuery.data.selectedSheet && _jsxs(Typography.Text, { children: ["\u5DE5\u4F5C\u8868\uFF1A", batchQuery.data.selectedSheet] }), _jsxs(Typography.Text, { children: ["\u72B6\u6001\uFF1A", batchQuery.data.status === 'processing' ? '解析中' : batchQuery.data.status === 'succeeded' ? '导入成功' : batchQuery.data.status === 'failed' ? '导入失败' : '排队中'] }), _jsxs(Typography.Text, { children: ["\u603B\u6570\uFF1A", batchQuery.data.totalCount, "\uFF0C\u6210\u529F\uFF1A", batchQuery.data.successCount, "\uFF0C\u5931\u8D25\uFF1A", batchQuery.data.failureCount] }), batchQuery.data.metrics && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5BFC\u5165\u8017\u65F6", description: `总耗时 ${(batchQuery.data.metrics.totalMs / 1000).toFixed(1)}s；解析 ${(batchQuery.data.metrics.excelParseMs / 1000).toFixed(1)}s；校验 ${(batchQuery.data.metrics.validationMs / 1000).toFixed(1)}s；落库 ${(batchQuery.data.metrics.persistMs / 1000).toFixed(1)}s；提交 ${(batchQuery.data.metrics.commitMs / 1000).toFixed(1)}s。题目批次 ${batchQuery.data.metrics.questionBatchCount}，绑定批次 ${batchQuery.data.metrics.bindingBatchCount}，SQL ${batchQuery.data.metrics.totalSqlCount}，平均每批 ${batchQuery.data.metrics.avgBatchWriteMs ?? 0}ms` })), batchQuery.data.status === 'failed' && batchQuery.data.errorDetail && _jsx(Alert, { type: "error", showIcon: true, message: "\u9519\u8BEF\u660E\u7EC6", description: batchQuery.data.errorDetail })] }) }) : _jsx(Spin, {}) })] }));
}
export function CompetitionSimplePracticalQuestionPage() {
    return _jsx(CompetitionQuestionBankPage, { contentType: "practical", simpleMode: true });
}
