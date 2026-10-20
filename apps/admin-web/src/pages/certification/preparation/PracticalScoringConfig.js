import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, ReloadOutlined, RollbackOutlined, UploadOutlined, } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Input, InputNumber, Modal, Progress, Select, Space, Table, Tag, Tooltip, Typography, Upload, message, } from 'antd';
import './practical-scoring-expand.css';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { listAllPreparationCandidates, listPreparationAssessorGroups, } from '../../../api/certificationPreparation';
import { batchLinkPracticalCandidateTemplates, completePracticalScoringTask, requestPracticalCompleteSign, adjustPracticalScoringTaskScore, exportPracticalSummary, getPracticalScoringTaskDetail, getPracticalScoringProgress, getPracticalScoringSetup, importPracticalPaper, markPracticalCandidateAbsent, returnPracticalScoringTask, savePracticalQuestion, } from '../../../api/practicalScoring';
import { parseContentDispositionFilename } from '../../../utils/download';
import { usePaneActive } from '../../../layouts/TabKeepAlive';
export function PracticalScoringConfig({ taskId, showSetup = true, showTemplate = true, showProgress = true, }) {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    const [selectedScopeId, setSelectedScopeId] = useState();
    const [selectedCandidateIds, setSelectedCandidateIds] = useState([]);
    const [configModalOpen, setConfigModalOpen] = useState(false);
    const [batchGroupId, setBatchGroupId] = useState();
    const [batchPaperId, setBatchPaperId] = useState();
    const [returningTask, setReturningTask] = useState();
    const [returnReason, setReturnReason] = useState('');
    const [adjustingTask, setAdjustingTask] = useState();
    const [adjustDeductions, setAdjustDeductions] = useState({});
    const [adjustRemarks, setAdjustRemarks] = useState({});
    const [adjustNote, setAdjustNote] = useState('');
    const [adjustReason, setAdjustReason] = useState('');
    const [selectedSummaryCandidateIds, setSelectedSummaryCandidateIds] = useState([]);
    const [editingQuestion, setEditingQuestion] = useState();
    const [editQuestionTitle, setEditQuestionTitle] = useState('');
    const [editTemplateItems, setEditTemplateItems] = useState([]);
    const showConfig = showSetup || showTemplate;
    const setupQuery = useQuery({
        queryKey: ['practical-scoring-setup', taskId],
        queryFn: () => getPracticalScoringSetup(taskId),
        enabled: Boolean(taskId && showConfig),
    });
    const progressQuery = useQuery({
        queryKey: ['practical-scoring-progress', taskId],
        queryFn: () => getPracticalScoringProgress(taskId),
        enabled: Boolean(taskId && (showConfig || showProgress)),
        refetchInterval: paneActive && showProgress ? 5000 : false,
    });
    const adjustingTaskQuery = useQuery({
        queryKey: ['practical-scoring-admin-task', adjustingTask?.assessorTaskId],
        queryFn: () => getPracticalScoringTaskDetail(adjustingTask.assessorTaskId),
        enabled: Boolean(adjustingTask),
    });
    const groupsQuery = useQuery({
        queryKey: ['certification-preparation-assessor-groups', taskId],
        queryFn: () => listPreparationAssessorGroups(taskId),
        enabled: Boolean(taskId && showConfig),
    });
    const setup = setupQuery.data;
    const selectedScope = useMemo(() => setup?.scopes.find((scope) => scope.id === selectedScopeId), [selectedScopeId, setup?.scopes]);
    const candidateQuery = useQuery({
        queryKey: ['practical-scoring-candidates', taskId, selectedScopeId, selectedScope?.levelCode],
        queryFn: () => listAllPreparationCandidates(taskId, {
            status: 'active',
            documentComplete: true,
            ...(selectedScope?.levelCode ? { levelCode: selectedScope.levelCode } : {}),
        }),
        enabled: Boolean(taskId && showConfig),
    });
    const groupDetailById = useMemo(() => new Map((groupsQuery.data || []).map((group) => [String(group.id), group])), [groupsQuery.data]);
    const resolveMemberCount = (groupId) => {
        const detail = groupDetailById.get(String(groupId));
        if (typeof detail?.memberCount === 'number') {
            return detail.memberCount;
        }
        const setupGroup = setup?.groups.find((group) => String(group.id) === String(groupId));
        return setupGroup?.memberCount ?? 0;
    };
    const configByCandidate = useMemo(() => new Map((setup?.configs || [])
        .filter((config) => config.candidateId)
        .map((config) => [config.candidateId, config])), [setup?.configs]);
    const progressByCandidate = useMemo(() => new Map((progressQuery.data?.candidates || []).map((candidate) => [candidate.candidateId, candidate])), [progressQuery.data?.candidates]);
    const abnormalCandidates = useMemo(() => (progressQuery.data?.candidates || []).filter((candidate) => candidate.status === 'review_required'), [progressQuery.data?.candidates]);
    const submittedCandidateCount = useMemo(() => (progressQuery.data?.candidates || []).filter((candidate) => candidate.status === 'completed').length, [progressQuery.data?.candidates]);
    const totalCandidateCount = progressQuery.data?.candidates.length || 0;
    const completeSign = progressQuery.data?.completeSign;
    const scoresSettled = totalCandidateCount > 0
        && (progressQuery.data?.candidates || []).every((row) => row.status === 'completed' || row.status === 'absent');
    const signStatus = completeSign?.status || 'none';
    const waitingSign = signStatus === 'pending';
    const allSigned = signStatus === 'signed';
    const completeDisabled = !scoresSettled || waitingSign || signStatus === 'consumed';
    const completeDisabledReason = !scoresSettled
        ? '仅全部考生实操成绩已完成或已标记缺考时可执行'
        : waitingSign
            ? `等待三名考评员签字确认（${completeSign?.signedCount || 0}/${completeSign?.requiredCount || 3}）`
            : signStatus === 'consumed'
                ? '评分任务已完成'
                : undefined;
    const assessorGroupsByCandidate = useMemo(() => {
        const groups = new Map();
        for (const task of progressQuery.data?.scoringTasks || []) {
            let byCandidate = groups.get(task.candidateId);
            if (!byCandidate) {
                byCandidate = new Map();
                groups.set(task.candidateId, byCandidate);
            }
            const byAssessor = byCandidate.get(task.assessorId);
            if (byAssessor) {
                byAssessor.tasks.push(task);
            }
            else {
                byCandidate.set(task.assessorId, { id: task.assessorId, name: task.assessorName, tasks: [task] });
            }
        }
        return groups;
    }, [progressQuery.data?.scoringTasks]);
    const candidateRows = useMemo(() => {
        const rows = candidateQuery.data?.items || [];
        if (!selectedScope)
            return rows;
        return rows.filter((candidate) => scopeMatchesCandidate(selectedScope, candidate));
    }, [candidateQuery.data?.items, selectedScope]);
    const selectedCandidateRows = useMemo(() => candidateRows.filter((candidate) => selectedCandidateIds.includes(candidate.id)), [candidateRows, selectedCandidateIds]);
    const resolvedConfigByCandidate = useMemo(() => new Map(candidateRows.map((candidate) => {
        const directConfig = configByCandidate.get(candidate.id);
        if (directConfig)
            return [candidate.id, directConfig];
        const scope = findScopeForCandidate(setup?.scopes || [], candidate);
        const scopeConfig = scope ? (setup?.configs || []).find((config) => (!config.candidateId && String(config.scopeId) === String(scope.id))) : undefined;
        return [candidate.id, scopeConfig];
    })), [candidateRows, configByCandidate, setup?.configs, setup?.scopes]);
    const availableBatchPapers = useMemo(() => (setup?.papers || []).filter((paper) => (paper.questions.length > 0
        && paper.questions.every((question) => question.templateReady)
        && selectedCandidateRows.every((candidate) => paperOptionsForCandidate(setup?.papers || [], candidate, findScopeForCandidate(setup?.scopes || [], candidate)).some((option) => option.id === paper.id)))), [selectedCandidateRows, setup?.papers, setup?.scopes]);
    const isCandidateUnavailable = (candidate) => (Boolean(resolvedConfigByCandidate.get(candidate.id)) || Boolean(progressByCandidate.get(candidate.id)?.totalTasks));
    const refresh = () => {
        if (showConfig) {
            queryClient.invalidateQueries({ queryKey: ['practical-scoring-setup', taskId] });
            queryClient.invalidateQueries({ queryKey: ['practical-scoring-candidates', taskId] });
            queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-groups', taskId] });
        }
        if (showProgress || showConfig) {
            queryClient.invalidateQueries({ queryKey: ['practical-scoring-progress', taskId] });
        }
    };
    useEffect(() => {
        const detail = adjustingTaskQuery.data;
        if (!detail)
            return;
        setAdjustDeductions(Object.fromEntries(detail.scoreItems.map((item) => [scoreItemKey(item), item.deductedScore || 0])));
        setAdjustRemarks(Object.fromEntries(detail.scoreItems.map((item) => [scoreItemKey(item), item.remark || ''])));
        setAdjustNote(detail.progressNote || '');
    }, [adjustingTaskQuery.data]);
    useEffect(() => {
        const availableIds = new Set(candidateRows
            .filter((candidate) => !isCandidateUnavailable(candidate))
            .map((candidate) => candidate.id));
        setSelectedCandidateIds((previous) => previous.filter((candidateId) => availableIds.has(candidateId)));
    }, [candidateRows, progressByCandidate, resolvedConfigByCandidate]);
    useEffect(() => {
        if (batchPaperId && !availableBatchPapers.some((paper) => paper.id === batchPaperId)) {
            setBatchPaperId(undefined);
        }
    }, [availableBatchPapers, batchPaperId]);
    const adjustTotalScore = useMemo(() => {
        const detail = adjustingTaskQuery.data;
        if (!detail)
            return 0;
        return detail.scoreItems.reduce((sum, item) => (sum + Math.max(0, item.maxScore - (adjustDeductions[scoreItemKey(item)] ?? item.deductedScore ?? 0))), 0);
    }, [adjustDeductions, adjustingTaskQuery.data]);
    const batchCandidateLinkMutation = useMutation({
        mutationFn: () => batchLinkPracticalCandidateTemplates(taskId, {
            candidateIds: selectedCandidateIds,
            paperId: batchPaperId,
            groupId: batchGroupId,
        }),
        onSuccess: (result) => {
            message.success(`已为 ${result.configuredCandidates} 名学员生成 ${result.createdTasks} 个评分任务`);
            if (result.skippedCandidates > 0) {
                message.warning(`${result.skippedCandidates} 名已配置或已生成评分任务的学员未处理`);
            }
            setConfigModalOpen(false);
            setSelectedCandidateIds([]);
            setBatchGroupId(undefined);
            setBatchPaperId(undefined);
            refresh();
        },
        onError: (error) => {
            const reason = error?.response?.data?.message
                || error?.message
                || '批量配置实操试卷失败，请稍后重试';
            message.error(reason);
        },
    });
    const returnMutation = useMutation({
        mutationFn: () => returnPracticalScoringTask(returningTask.assessorTaskId, returnReason),
        onSuccess: () => {
            message.success('评分任务已退回原考评员');
            setReturningTask(undefined);
            setReturnReason('');
            refresh();
        },
    });
    const adjustMutation = useMutation({
        mutationFn: () => {
            const detail = adjustingTaskQuery.data;
            return adjustPracticalScoringTaskScore(detail.task.id, {
                version: detail.task.version,
                reason: adjustReason,
                note: adjustNote || undefined,
                items: detail.scoreItems.map((item) => ({
                    scoreItemId: item.id,
                    itemNo: item.itemNo,
                    deductedScore: adjustDeductions[scoreItemKey(item)] ?? item.deductedScore ?? 0,
                    remark: adjustRemarks[scoreItemKey(item)] || undefined,
                })),
            });
        },
        onSuccess: () => {
            message.success('评分已修改并重新提交');
            setAdjustingTask(undefined);
            setAdjustReason('');
            setAdjustNote('');
            setAdjustDeductions({});
            setAdjustRemarks({});
            refresh();
        },
    });
    const candidateAbsentMutation = useMutation({
        mutationFn: (candidateId) => markPracticalCandidateAbsent(taskId, candidateId),
        onSuccess: () => {
            message.success('已标记缺考');
            refresh();
        },
    });
    const importPaperMutation = useMutation({
        mutationFn: (file) => importPracticalPaper(taskId, file),
        onSuccess: (paper) => {
            message.success(`已导入 ${paper.title}，生成 ${paper.questions} 道实操题、${paper.scoreItems} 个评分项`);
            refresh();
        },
        onError: (error) => {
            const reason = error?.response?.data?.message
                || error?.message
                || '导入评分记录表失败，请检查表格格式';
            message.error(reason);
        },
    });
    const editQuestionMutation = useMutation({
        mutationFn: () => savePracticalQuestion(editingQuestion.paper.id, editingQuestion.question.id, { title: editQuestionTitle, items: editTemplateItems }),
        onSuccess: () => {
            message.success('评分记录表已修改');
            setEditingQuestion(undefined);
            setEditQuestionTitle('');
            setEditTemplateItems([]);
            refresh();
        },
        onError: (error) => {
            const reason = error?.response?.data?.message
                || error?.message
                || '修改评分记录表失败，请检查题目名称和评分项配分';
            message.error(reason);
        },
    });
    const exportSummaryMutation = useMutation({
        mutationFn: () => exportPracticalSummary(taskId, selectedSummaryCandidateIds),
        onSuccess: (resp) => {
            const filename = parseContentDispositionFilename({ headers: resp.headers })
                || `评分记录汇总表_${taskId}.xlsx`;
            const url = window.URL.createObjectURL(resp.data);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            message.success('实操评分表已生成');
        },
    });
    const completeTaskMutation = useMutation({
        mutationFn: () => completePracticalScoringTask(taskId),
        onSuccess: (result) => {
            message.success(`评分任务已完成，已将 ${result.releasedAssessors} 名考评员改为空闲（${result.releasedGroups} 个考评组历史绑定已保留）`);
            refresh();
        },
    });
    const requestCompleteSignMutation = useMutation({
        mutationFn: () => requestPracticalCompleteSign(taskId),
        onSuccess: (result) => {
            message.success(`已通知考评员签字确认（${result.signedCount}/${result.requiredCount}）`);
            refresh();
        },
    });
    const openQuestionEditor = (paper, question) => {
        setEditingQuestion({ paper, question });
        setEditQuestionTitle(question.title || '');
        setEditTemplateItems((question.templateItems || []).map((item) => ({ ...item })));
    };
    const updateEditTemplateItem = (itemNo, patch) => {
        setEditTemplateItems((previous) => previous.map((item) => (item.itemNo === itemNo ? { ...item, ...patch } : item)));
    };
    const addEditTemplateItem = () => {
        const nextItemNo = editTemplateItems.reduce((max, item) => Math.max(max, item.itemNo), 0) + 1;
        setEditTemplateItems((previous) => [
            ...previous,
            {
                itemNo: nextItemNo,
                itemName: `评分要素${nextItemNo}`,
                maxScore: 1,
                criterion: '',
                maxDeduction: 0,
                deductionStep: 1,
            },
        ]);
    };
    const editTemplateTotal = editTemplateItems.reduce((sum, item) => sum + Number(item.maxScore || 0), 0);
    const editTargetScore = editingQuestion?.question.score;
    const editScoreMatches = editTargetScore == null
        || Math.abs(editTemplateTotal - Number(editTargetScore)) < 0.0001;
    if (!taskId)
        return _jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u3002" });
    const candidateColumns = [
        { title: '学员', dataIndex: 'candidateName', width: 120, fixed: 'left' },
        { title: '证件号', dataIndex: 'maskedIdCardNo', width: 180, render: (v, row) => v || row.idCardNo || '-' },
        { title: '手机号', dataIndex: 'maskedPhone', width: 140, render: (v, row) => v || row.phone || '-' },
        { title: '工种', dataIndex: 'workTypeName', width: 120, render: (v) => v || '-' },
        { title: '等级', dataIndex: 'levelName', width: 100, render: (v, row) => v || row.levelCode || '-' },
        { title: '单位', dataIndex: 'orgName', width: 160, ellipsis: true, render: (v) => v || '-' },
        {
            title: '已绑定考评组',
            width: 180,
            render: (_, row) => {
                const config = resolvedConfigByCandidate.get(row.id);
                if (!config)
                    return '-';
                return groupDetailById.get(String(config.groupId))?.groupName
                    || setup?.groups.find((group) => String(group.id) === String(config.groupId))?.groupName
                    || '-';
            },
        },
        {
            title: '配置状态',
            width: 180,
            render: (_, row) => {
                const configured = resolvedConfigByCandidate.get(row.id);
                const progress = progressByCandidate.get(row.id);
                if (progress?.totalTasks)
                    return _jsx(Tag, { color: "success", children: "\u5DF2\u751F\u6210\u8BC4\u5206\u4EFB\u52A1" });
                if (configured)
                    return _jsx(Tag, { color: "processing", children: "\u5DF2\u914D\u7F6E" });
                return _jsx(Tag, { children: "\u672A\u914D\u7F6E" });
            },
        },
    ];
    const progressCandidateColumns = [
        { title: '考生编号', dataIndex: 'candidateNo', width: 120, render: (v) => v || '-' },
        { title: '考生', dataIndex: 'candidateName', width: 120 },
        {
            title: '评分提交',
            width: 180,
            render: (_, row) => (_jsx(Progress, { percent: row.totalTasks ? Math.round(row.submittedTasks * 100 / row.totalTasks) : 0, format: () => `${row.submittedTasks}/${row.totalTasks}` })),
        },
        { title: '试题完成', width: 150, render: (_, row) => `${row.completedQuestions}/${row.totalQuestions}` },
        { title: '实操总分', dataIndex: 'totalScore', width: 110, render: (v) => formatScore(v) ?? '待三人齐交' },
        {
            title: '开考协同',
            width: 220,
            render: (_, row) => {
                const sessionStatus = row.startSession?.status;
                const absentDisabled = row.status === 'completed' || row.status === 'absent' || sessionStatus === 'completed';
                return (_jsxs(Space, { children: [row.startSession ? (_jsx(Tag, { color: sessionStatus === 'completed' ? 'success' : sessionStatus === 'absent' ? 'warning' : sessionStatus === 'started' ? 'processing' : 'default', children: sessionStatus === 'completed'
                                ? '考试已完成'
                                : sessionStatus === 'absent'
                                    ? '缺考'
                                    : sessionStatus === 'started'
                                        ? '考试中'
                                        : `${row.startSession.candidateConfirmed ? '已选择考生' : '待确认'} · ${row.startSession.confirmedAssessors}/3` })) : _jsx(Tag, { children: "\u5F85\u751F\u6210\u4EFB\u52A1" }), _jsx(Button, { size: "small", danger: true, disabled: absentDisabled, loading: candidateAbsentMutation.isPending, onClick: (event) => {
                                event.stopPropagation();
                                Modal.confirm({
                                    title: '确认标记缺考？',
                                    content: `${row.candidateName} 的实操评分数据将按 0.0 导出。`,
                                    okText: '确认缺考',
                                    cancelText: '取消',
                                    okButtonProps: { danger: true },
                                    onOk: () => candidateAbsentMutation.mutateAsync(row.candidateId),
                                });
                            }, children: "\u7F3A\u8003" })] }));
            },
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (v) => {
                if (v === 'review_required')
                    return _jsx(Tag, { color: "error", children: "\u5206\u5DEE\u5F02\u5E38" });
                if (v === 'absent')
                    return _jsx(Tag, { color: "warning", children: "\u7F3A\u8003" });
                return _jsx(Tag, { color: v === 'completed' ? 'success' : 'processing', children: v === 'completed' ? '已完成' : '评分中' });
            },
        },
    ];
    const paperColumns = [
        {
            title: '评分记录表',
            dataIndex: 'title',
            ellipsis: true,
            render: (value) => value || '未命名评分记录表',
        },
        {
            title: '试题数',
            width: 90,
            render: (_, paper) => paper.questions.length,
        },
        {
            title: '评分模板',
            width: 120,
            render: (_, paper) => {
                const ready = paper.questions.filter((question) => question.templateReady).length;
                return _jsxs(Tag, { color: ready === paper.questions.length && ready > 0 ? 'success' : 'warning', children: [ready, "/", paper.questions.length, " \u5DF2\u914D\u7F6E"] });
            },
        },
    ];
    const questionColumnsForPaper = (paper) => [
        {
            title: '题目名称',
            dataIndex: 'title',
            ellipsis: true,
            render: (value) => value || _jsx(Typography.Text, { type: "danger", children: "\u672A\u547D\u540D\u9898\u76EE" }),
        },
        { title: '题目分值', dataIndex: 'score', width: 100, render: (value) => value ?? '-' },
        {
            title: '评分项',
            width: 100,
            render: (_, question) => `${question.templateItems?.length || 0} 项`,
        },
        {
            title: '操作',
            width: 100,
            render: (_, question) => (_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openQuestionEditor(paper, question), children: "\u4FEE\u6539" })),
        },
    ];
    return (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [showConfig && (_jsx(ProCard, { title: "\u5B66\u5458\u5217\u8868", extra: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }), children: _jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsxs(Space, { wrap: true, children: [_jsx(Select, { allowClear: true, showSearch: true, optionFilterProp: "label", value: selectedScopeId, placeholder: "\u5DE5\u79CD\u7B49\u7EA7\u7B5B\u9009", style: { width: 260 }, options: (setup?.scopes || []).map((scope) => ({
                                        value: scope.id,
                                        label: `${scope.workTypeName || '-'} / ${scope.levelName || scope.levelCode || '-'}`,
                                    })), onChange: setSelectedScopeId }), _jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: (file) => {
                                        importPaperMutation.mutate(file);
                                        return false;
                                    }, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), loading: importPaperMutation.isPending, children: "\u5BFC\u5165\u8BC4\u5206\u8BB0\u5F55\u8868" }) }), _jsxs(Button, { type: "primary", disabled: selectedCandidateIds.length === 0, onClick: () => {
                                        setBatchGroupId(undefined);
                                        setBatchPaperId(undefined);
                                        setConfigModalOpen(true);
                                    }, children: ["\u914D\u7F6E", selectedCandidateIds.length ? `（${selectedCandidateIds.length}）` : ''] })] }), _jsx(Table, { rowKey: "id", columns: candidateColumns, dataSource: candidateRows, loading: candidateQuery.isFetching || setupQuery.isFetching || progressQuery.isFetching, scroll: { x: 1000 }, rowSelection: {
                                selectedRowKeys: selectedCandidateIds,
                                onChange: (keys) => setSelectedCandidateIds(keys.map(String)),
                                getCheckboxProps: (row) => ({
                                    disabled: isCandidateUnavailable(row),
                                }),
                            }, pagination: { pageSize: 10, showSizeChanger: false }, locale: {
                                emptyText: selectedScopeId ? '当前工种等级下暂无已完成申报材料的学员' : '暂无已完成申报材料的学员',
                            } })] }) })), showTemplate && (_jsxs(ProCard, { title: "\u8BC4\u5206\u8BB0\u5F55\u8868\u914D\u7F6E", children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u5BFC\u5165\u540E\u53EF\u76F4\u63A5\u4FEE\u6539\u9898\u76EE\u540D\u79F0\u548C\u8BC4\u5206\u660E\u7EC6", description: "\u9898\u76EE\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A\uFF0C\u8BC4\u5206\u9879\u914D\u5206\u5408\u8BA1\u5FC5\u987B\u7B49\u4E8E\u9898\u76EE\u5206\u503C\u3002\u5C1A\u672A\u5F00\u59CB\u8BC4\u5206\u7684\u4EFB\u52A1\u4F1A\u540C\u6B65\u66F4\u65B0\uFF1B\u5DF2\u5F00\u59CB\u8BC4\u5206\u7684\u4EFB\u52A1\u9700\u5148\u5904\u7406\u540E\u518D\u4FEE\u6539\u3002", style: { marginBottom: 16 } }), _jsx(Table, { rowKey: "id", columns: paperColumns, dataSource: setup?.papers || [], loading: setupQuery.isFetching, pagination: { pageSize: 8, showSizeChanger: false }, expandable: {
                            expandedRowRender: (paper) => (_jsx(Table, { rowKey: "id", columns: questionColumnsForPaper(paper), dataSource: paper.questions, pagination: false, size: "small", locale: { emptyText: '该评分记录表暂无题目' } })),
                            rowExpandable: (paper) => paper.questions.length > 0,
                        }, locale: { emptyText: '暂无可修改的实操评分记录表，请先导入评分记录表' } })] })), _jsx(Modal, { title: `批量配置实操考试（已选 ${selectedCandidateIds.length} 名学员）`, open: configModalOpen, okText: "\u786E\u8BA4\u914D\u7F6E", cancelText: "\u53D6\u6D88", confirmLoading: batchCandidateLinkMutation.isPending, onCancel: () => setConfigModalOpen(false), onOk: () => {
                    if (!batchGroupId) {
                        message.warning('请选择考评组');
                        return;
                    }
                    if (!batchPaperId) {
                        message.warning('请选择实操试卷');
                        return;
                    }
                    batchCandidateLinkMutation.mutate();
                }, children: _jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u5DF2\u914D\u7F6E\u6216\u5DF2\u751F\u6210\u8BC4\u5206\u4EFB\u52A1\u7684\u5B66\u5458\u4E0D\u53EF\u52FE\u9009\uFF1B\u672C\u6B21\u5C06\u4E3A\u6240\u9009\u5B66\u5458\u7EDF\u4E00\u7ED1\u5B9A\u4EE5\u4E0B\u8003\u8BC4\u7EC4\u548C\u8BD5\u5377\u3002" }), _jsxs("div", { children: [_jsx(Typography.Text, { strong: true, children: "\u8003\u8BC4\u7EC4" }), _jsx(Select, { showSearch: true, optionFilterProp: "label", value: batchGroupId, placeholder: "\u8BF7\u9009\u62E9\u6EE1\u8DB3\u4E09\u4EBA\u6210\u7EC4\u6761\u4EF6\u7684\u8003\u8BC4\u7EC4", style: { width: '100%', marginTop: 8 }, options: (groupsQuery.data || []).map((group) => {
                                        const count = resolveMemberCount(group.id);
                                        return {
                                            value: group.id,
                                            disabled: count !== 3,
                                            label: `${group.groupName}（${count}/3人${group.leaderAssessorName ? ` · 组长${group.leaderAssessorName}` : ''}）`,
                                        };
                                    }), onChange: setBatchGroupId })] }), _jsxs("div", { children: [_jsx(Typography.Text, { strong: true, children: "\u5B9E\u64CD\u8BD5\u5377" }), _jsx(Select, { showSearch: true, optionFilterProp: "label", value: batchPaperId, placeholder: "\u8BF7\u9009\u62E9\u53EF\u7528\u4E8E\u6240\u6709\u5DF2\u9009\u5B66\u5458\u7684\u5B9E\u64CD\u8BD5\u5377", style: { width: '100%', marginTop: 8 }, options: availableBatchPapers.map((paper) => ({ value: paper.id, label: paper.title })), notFoundContent: "\u6CA1\u6709\u53EF\u7EDF\u4E00\u7528\u4E8E\u6240\u9009\u5B66\u5458\u7684\u5DF2\u5B8C\u6210\u6A21\u677F\u8BD5\u5377", onChange: setBatchPaperId })] })] }) }), _jsx(Modal, { title: `修改评分记录表${editingQuestion?.question.title ? ` · ${editingQuestion.question.title}` : ''}`, open: Boolean(editingQuestion), width: 1120, okText: "\u4FDD\u5B58\u4FEE\u6539", cancelText: "\u53D6\u6D88", confirmLoading: editQuestionMutation.isPending, okButtonProps: {
                    disabled: !editQuestionTitle.trim() || editTemplateItems.length === 0 || !editScoreMatches,
                }, onCancel: () => {
                    setEditingQuestion(undefined);
                    setEditQuestionTitle('');
                    setEditTemplateItems([]);
                }, onOk: () => editQuestionMutation.mutate(), children: _jsxs(Space, { direction: "vertical", size: 14, style: { width: '100%' }, children: [_jsxs("div", { children: [_jsx(Typography.Text, { strong: true, children: "\u9898\u76EE\u540D\u79F0" }), _jsx(Input, { value: editQuestionTitle, maxLength: 120, showCount: true, placeholder: "\u8BF7\u8F93\u5165\u9898\u76EE\u540D\u79F0", style: { marginTop: 8 }, onChange: (event) => setEditQuestionTitle(event.target.value) })] }), _jsxs(Space, { children: [_jsx(Typography.Text, { strong: true, children: "\u8BC4\u5206\u9879\u660E\u7EC6" }), _jsxs(Typography.Text, { type: editingQuestion && Math.abs(editTemplateTotal - Number(editingQuestion.question.score || 0)) < 0.0001
                                        ? 'success'
                                        : 'danger', children: ["\u914D\u5206\u5408\u8BA1 ", editTemplateTotal, " / \u9898\u76EE\u5206\u503C ", editingQuestion?.question.score ?? '-'] })] }), _jsx(Table, { rowKey: "itemNo", size: "small", pagination: false, scroll: { x: 950 }, dataSource: editTemplateItems, columns: [
                                { title: '序号', dataIndex: 'itemNo', width: 70 },
                                {
                                    title: '评分要素',
                                    width: 180,
                                    render: (_, item) => (_jsx(Input, { value: item.itemName, onChange: (event) => updateEditTemplateItem(item.itemNo, { itemName: event.target.value }) })),
                                },
                                {
                                    title: '配分',
                                    width: 110,
                                    render: (_, item) => (_jsx(InputNumber, { min: 0.01, precision: 2, style: { width: '100%' }, value: item.maxScore, onChange: (value) => updateEditTemplateItem(item.itemNo, { maxScore: Number(value || 0) }) })),
                                },
                                {
                                    title: '评分标准',
                                    width: 300,
                                    render: (_, item) => (_jsx(Input, { value: item.criterion, onChange: (event) => updateEditTemplateItem(item.itemNo, { criterion: event.target.value }) })),
                                },
                                {
                                    title: '最大扣分',
                                    width: 110,
                                    render: (_, item) => (_jsx(InputNumber, { min: 0, precision: 2, style: { width: '100%' }, value: item.maxDeduction, onChange: (value) => updateEditTemplateItem(item.itemNo, { maxDeduction: Number(value || 0) }) })),
                                },
                                {
                                    title: '步长',
                                    width: 100,
                                    render: (_, item) => (_jsx(InputNumber, { min: 0.01, precision: 2, style: { width: '100%' }, value: item.deductionStep, onChange: (value) => updateEditTemplateItem(item.itemNo, { deductionStep: Number(value || 0) }) })),
                                },
                                {
                                    title: '操作',
                                    width: 70,
                                    render: (_, item) => (_jsx(Button, { type: "link", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => setEditTemplateItems((previous) => previous.filter((row) => row.itemNo !== item.itemNo)) })),
                                },
                            ] }), _jsx(Button, { type: "dashed", icon: _jsx(PlusOutlined, {}), onClick: addEditTemplateItem, children: "\u65B0\u589E\u8BC4\u5206\u9879" })] }) }), showProgress && (_jsxs(ProCard, { title: "\u8BC4\u5206\u8FDB\u5EA6", children: [_jsxs(Space, { wrap: true, style: { marginBottom: 16 }, children: [_jsx(Tooltip, { title: completeDisabled ? completeDisabledReason : undefined, children: _jsx("span", { children: _jsx(Button, { type: "primary", danger: true, disabled: completeDisabled, loading: completeTaskMutation.isPending || requestCompleteSignMutation.isPending, onClick: () => {
                                            if (!allSigned) {
                                                Modal.confirm({
                                                    title: '确认完成评分任务？',
                                                    content: (_jsxs(Space, { direction: "vertical", size: 8, children: [_jsx(Typography.Paragraph, { style: { marginBottom: 0 }, children: "\u4EC5\u5168\u90E8\u8003\u751F\u5B9E\u64CD\u6210\u7EE9\u5DF2\u5B8C\u6210\u6216\u5DF2\u6807\u8BB0\u7F3A\u8003\u65F6\u53EF\u6267\u884C\u3002\u5B8C\u6210\u540E\u4F1A\u628A\u672C\u4EFB\u52A1\u8003\u8BC4\u5458\u4ECE\u300C\u8BA4\u5B9A\u4EFB\u52A1\u4E2D\u300D\u6539\u4E3A\u300C\u7A7A\u95F2\u300D\uFF0C\u8003\u8BC4\u7EC4\u7ED1\u5B9A\u548C\u5386\u53F2\u8BC4\u5206\u8BB0\u5F55\u4F1A\u4FDD\u7559\u3002" }), _jsx(Typography.Paragraph, { strong: true, style: { marginBottom: 0 }, children: "\u4E09\u540D\u8003\u8BC4\u5458\u7B7E\u5B57\u786E\u8BA4\u540E\uFF0C\u5B8C\u6210\u4EFB\u52A1\u6309\u94AE\u624D\u4F1A\u751F\u6548\u3002" })] })),
                                                    okText: '通知考评员签字',
                                                    cancelText: '取消',
                                                    onOk: () => requestCompleteSignMutation.mutateAsync(),
                                                });
                                                return;
                                            }
                                            Modal.confirm({
                                                title: '确认完成评分任务？',
                                                content: '三名考评员已签字确认。完成后会把本任务考评员从「认定任务中」改为「空闲」，考评组绑定和历史评分记录会保留，签名将写入评分记录表。',
                                                okText: '完成任务并释放考评员',
                                                cancelText: '取消',
                                                okButtonProps: { danger: true },
                                                onOk: () => completeTaskMutation.mutateAsync(),
                                            });
                                        }, children: "\u5B8C\u6210\u4EFB\u52A1" }) }) }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), disabled: selectedSummaryCandidateIds.length === 0, loading: exportSummaryMutation.isPending, onClick: () => exportSummaryMutation.mutate(), children: "\u5BFC\u51FA\u5B9E\u64CD\u8BC4\u5206\u8868" })] }), waitingSign && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u4E09\u540D\u8003\u8BC4\u5458\u7B7E\u5B57\u786E\u8BA4", description: `已通知实操评分端弹出电子签名。当前 ${completeSign?.signedCount || 0}/${completeSign?.requiredCount || 3} 名考评员已签字，全部签字完成后“完成任务”按钮将可用。` })), allSigned && (_jsx(Alert, { type: "success", showIcon: true, style: { marginBottom: 16 }, message: "\u4E09\u540D\u8003\u8BC4\u5458\u5DF2\u7B7E\u5B57\u786E\u8BA4", description: "\u73B0\u5728\u53EF\u4EE5\u70B9\u51FB\u201C\u5B8C\u6210\u4EFB\u52A1\u201D\uFF0C\u628A\u8003\u8BC4\u5458\u6539\u4E3A\u7A7A\u95F2\u5E76\u4FDD\u7559\u5386\u53F2\u7ED1\u5B9A\u3002" })), abnormalCandidates.length > 0 && (_jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 16 }, message: `发现 ${abnormalCandidates.length} 名考生成绩分差异常`, description: "\u8BF7\u5C55\u5F00\u8003\u751F\u884C\u67E5\u770B\u6807\u8BB0\u4E3A\u5206\u5DEE\u5F02\u5E38\u7684\u8BD5\u9898\uFF0C\u5728\u5BF9\u5E94\u8003\u8BC4\u5458\u4EFB\u52A1\u4E0A\u70B9\u51FB\u4FEE\u6539\u5206\u6570\u5E76\u91CD\u65B0\u63D0\u4EA4\u3002\u504F\u5DEE\u4E0D\u8D85\u8FC710\u5206\u540E\uFF0C\u7EC4\u957F\u7AEF\u624D\u80FD\u5B8C\u6210\u8003\u8BD5\u3002" })), _jsxs(Space, { size: 24, style: { marginBottom: 16 }, children: [_jsx(Progress, { type: "circle", size: 92, style: { marginLeft: 7 }, percent: totalCandidateCount
                                    ? Math.round(submittedCandidateCount * 100 / totalCandidateCount)
                                    : 0 }), _jsxs(Typography.Text, { children: ["\u8003\u751F\u5DF2\u63D0\u4EA4 ", submittedCandidateCount, " / ", totalCandidateCount, " \u4EBA"] })] }), _jsx(Table, { rowKey: "candidateId", columns: progressCandidateColumns, dataSource: progressQuery.data?.candidates || [], pagination: false, scroll: { x: 850 }, rowSelection: {
                            selectedRowKeys: selectedSummaryCandidateIds,
                            onChange: (keys) => setSelectedSummaryCandidateIds(keys.map(String)),
                            getCheckboxProps: (row) => ({
                                disabled: row.status !== 'completed' && row.status !== 'absent',
                            }),
                        }, expandable: {
                            expandedRowRender: (row) => (_jsx(AssessorBreakdown, { assessorGroups: assessorGroupsByCandidate.get(row.candidateId), onReturnTask: setReturningTask, onAdjustTask: (task) => {
                                    setAdjustReason('');
                                    setAdjustingTask(task);
                                } })),
                            rowExpandable: (row) => (assessorGroupsByCandidate.get(row.candidateId)?.size ?? 0) > 0,
                            expandRowByClick: true,
                        } })] })), _jsxs(Modal, { title: "\u9000\u56DE\u8BC4\u5206\u4EFB\u52A1", open: Boolean(returningTask), okButtonProps: { danger: true, disabled: !returnReason.trim() }, confirmLoading: returnMutation.isPending, onCancel: () => setReturningTask(undefined), onOk: () => returnMutation.mutate(), children: [_jsxs(Typography.Paragraph, { children: [returningTask?.candidateName, " / ", returningTask?.questionTitle, " / ", returningTask?.assessorName] }), _jsx(Input.TextArea, { rows: 4, value: returnReason, onChange: (event) => setReturnReason(event.target.value), placeholder: "\u5FC5\u987B\u586B\u5199\u9000\u56DE\u539F\u56E0\uFF0C\u539F\u6210\u7EE9\u5C06\u7ACB\u5373\u5931\u6548" })] }), _jsx(Modal, { title: "\u4FEE\u6539\u5F02\u5E38\u8BC4\u5206", open: Boolean(adjustingTask), width: 760, okText: "\u4FDD\u5B58\u5E76\u91CD\u65B0\u63D0\u4EA4", confirmLoading: adjustMutation.isPending, okButtonProps: {
                    disabled: !adjustingTaskQuery.data || !adjustReason.trim(),
                }, onCancel: () => {
                    setAdjustingTask(undefined);
                    setAdjustReason('');
                }, onOk: () => adjustMutation.mutate(), children: _jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsxs(Typography.Paragraph, { children: [adjustingTask?.candidateName, " / ", adjustingTask?.questionTitle, " / ", adjustingTask?.assessorName] }), adjustingTask?.reviewRequired ? (_jsx(Alert, { type: "error", showIcon: true, message: `当前试题三人评分分差${adjustingTask.scoreDeviation ?? '-'}分，超过10分` })) : null, _jsx(Table, { rowKey: scoreItemKey, loading: adjustingTaskQuery.isFetching, dataSource: adjustingTaskQuery.data?.scoreItems || [], pagination: false, columns: [
                                { title: '序号', dataIndex: 'itemNo', width: 70 },
                                { title: '评分项', render: (_, item) => scoreStandardContent(item) },
                                { title: '满分', dataIndex: 'maxScore', width: 80 },
                                {
                                    title: '扣分',
                                    width: 150,
                                    render: (_, item) => {
                                        const key = scoreItemKey(item);
                                        return (_jsx(InputNumber, { min: 0, max: item.maxDeduction, step: item.deductionStep, value: adjustDeductions[key] ?? item.deductedScore ?? 0, onChange: (value) => setAdjustDeductions((prev) => ({
                                                ...prev,
                                                [key]: Number(value || 0),
                                            })) }));
                                    },
                                },
                                {
                                    title: '得分',
                                    width: 90,
                                    render: (_, item) => Math.max(0, item.maxScore - (adjustDeductions[scoreItemKey(item)] ?? item.deductedScore ?? 0)),
                                },
                                {
                                    title: '说明',
                                    width: 220,
                                    render: (_, item) => {
                                        const key = scoreItemKey(item);
                                        return (_jsx(Input, { value: adjustRemarks[key] || '', onChange: (event) => setAdjustRemarks((prev) => ({
                                                ...prev,
                                                [key]: event.target.value,
                                            })) }));
                                    },
                                },
                            ] }), _jsxs(Typography.Text, { strong: true, children: ["\u8C03\u6574\u540E\u672C\u9898\u5F97\u5206\uFF1A", adjustTotalScore] }), _jsx(Input.TextArea, { rows: 2, value: adjustNote, onChange: (event) => setAdjustNote(event.target.value), placeholder: "\u8003\u8BC4\u8BF4\u660E\uFF08\u9009\u586B\uFF09" }), _jsx(Input.TextArea, { rows: 3, value: adjustReason, onChange: (event) => setAdjustReason(event.target.value), placeholder: "\u5FC5\u987B\u586B\u5199\u4FEE\u6539\u539F\u56E0\uFF0C\u7528\u4E8E\u5BA1\u8BA1\u7559\u75D5" })] }) })] }));
}
function AssessorBreakdown({ assessorGroups, onReturnTask, onAdjustTask }) {
    const slots = useMemo(() => {
        if (!assessorGroups)
            return [];
        return Array.from(assessorGroups.values()).sort((a, b) => a.id.localeCompare(b.id));
    }, [assessorGroups]);
    if (slots.length === 0) {
        return _jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u8003\u8BC4\u5458\u8BC4\u5206\u4EFB\u52A1" });
    }
    // 补齐到 3 列占位，保证三栏对齐
    const padded = [...slots];
    while (padded.length < 3)
        padded.push(null);
    return (_jsx("div", { className: "practical-candidate-assessors", children: padded.map((assessor, index) => {
            if (!assessor) {
                return (_jsx("div", { className: "practical-candidate-assessor practical-candidate-assessor--empty", children: "\u8BE5\u4F4D\u7F6E\u6682\u65E0\u8003\u8BC4\u5458" }, `empty-${index}`));
            }
            const submitted = assessor.tasks.filter((task) => task.status === 'submitted').length;
            const total = assessor.tasks.length;
            const totalScore = assessor.tasks.reduce((sum, task) => sum + (task.totalScore || 0), 0);
            const allSubmitted = total > 0 && submitted === total;
            const anyReturned = assessor.tasks.some((task) => task.status === 'returned');
            let statusColor = 'processing';
            let statusText = '评分中';
            if (allSubmitted) {
                statusColor = 'success';
                statusText = '已提交';
            }
            else if (anyReturned) {
                statusColor = 'error';
                statusText = '已退回';
            }
            return (_jsxs("div", { className: "practical-candidate-assessor", children: [_jsxs("div", { className: "practical-assessor-head", children: [_jsx("span", { className: "assessor-name", children: assessor.name }), _jsx(Tag, { color: statusColor, children: statusText })] }), _jsxs("div", { className: "practical-assessor-progress", children: [_jsx(Progress, { percent: total ? Math.round((submitted * 100) / total) : 0, size: "small", style: { flex: 1, margin: 0 } }), _jsxs("span", { children: [submitted, "/", total, " \u5DF2\u63D0\u4EA4"] })] }), _jsx("div", { className: "practical-assessor-tasks", children: assessor.tasks.map((task) => {
                            const taskColor = task.status === 'submitted'
                                ? 'success'
                                : task.status === 'returned'
                                    ? 'error'
                                    : 'processing';
                            const canReturn = task.status === 'submitted';
                            const reviewRequired = Boolean(task.reviewRequired);
                            return (_jsxs("div", { className: `practical-assessor-task${reviewRequired ? ' is-review-required' : ''}`, children: [_jsx("span", { className: "task-title", children: task.questionTitle }), _jsx("span", { className: "task-score", children: formatScore(task.totalScore) ?? '-' }), _jsx(Tag, { color: taskColor, children: statusName(task.status) }), reviewRequired ? (_jsxs(Tag, { color: "error", children: ["\u5206\u5DEE\u5F02\u5E38", task.scoreDeviation != null ? ` ${task.scoreDeviation}分` : ''] })) : null, onAdjustTask ? (_jsx(Button, { type: "link", size: "small", disabled: !canReturn, onClick: (event) => {
                                            event.stopPropagation();
                                            onAdjustTask(task);
                                        }, children: "\u4FEE\u6539\u5206\u6570" })) : null, onReturnTask ? (_jsx(Button, { type: "link", size: "small", danger: true, disabled: !canReturn, icon: _jsx(RollbackOutlined, {}), onClick: (event) => {
                                            event.stopPropagation();
                                            onReturnTask(task);
                                        }, children: "\u9000\u56DE" })) : null] }, task.assessorTaskId));
                        }) }), _jsxs("div", { className: "practical-assessor-total", children: [_jsx("span", { children: "\u5408\u8BA1\u5206" }), _jsx("strong", { children: formatScore(totalScore) })] })] }, assessor.id));
        }) }));
}
function scopeMatchesCandidate(scope, candidate) {
    const sameLevel = !scope.levelCode || scope.levelCode === candidate.levelCode;
    const sameWorkType = candidate.workTypeId
        ? scope.workTypeId === candidate.workTypeId || scope.workTypeName === candidate.workTypeName
        : scope.workTypeName === candidate.workTypeName;
    return sameLevel && sameWorkType;
}
function findScopeForCandidate(scopes, candidate) {
    return scopes.find((scope) => scopeMatchesCandidate(scope, candidate));
}
function paperOptionsForCandidate(papers, candidate, scope) {
    return papers.filter((paper) => {
        const workTypeId = candidate.workTypeId || scope?.workTypeId;
        const sameWorkType = !paper.workTypeId || paper.workTypeId === workTypeId;
        const sameLevel = !paper.level || paper.level === candidate.levelCode || paper.level === candidate.levelName;
        return sameWorkType && sameLevel;
    });
}
function scoreItemKey(item) {
    return item.id || String(item.itemNo);
}
function scoreStandardContent(item) {
    return item.scoringStandardContent || item.criterion || item.itemName || '-';
}
function formatScore(value) {
    if (value === undefined || value === null)
        return undefined;
    const numeric = Number(value);
    return Number.isFinite(numeric) ? numeric.toFixed(1) : undefined;
}
function statusName(status) {
    return { assigned: '待评分', draft: '草稿', submitted: '已提交', returned: '已退回', absent: '缺考' }[status] || status;
}
