import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckCircleOutlined, CloseCircleOutlined, DeleteOutlined, DownloadOutlined, EditOutlined, FileDoneOutlined, FolderOpenOutlined, DownOutlined, UpOutlined, PlusOutlined, ReloadOutlined, SaveOutlined, SearchOutlined, SendOutlined, } from '@ant-design/icons';
import { Button, Alert, Card, Collapse, DatePicker, Descriptions, Empty, Form, Input, InputNumber, Modal, Popconfirm, Progress, Select, Space, Table, Tabs, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createCertificationTask, deleteCertificationTask, finalReviewCertificationDocument, getCertificationTask, listCertificationMemberUsers, getCertificationWorkbench, initialReviewCertificationDocument, saveCertificationDocument, saveCertificationMembers, saveCertificationRisks, saveCertificationScheduleItems, saveCertificationWbsItems, submitCertificationDocument, toggleCertificationRiskStatus, updateCertificationTask, pageCertificationTasks, } from '../../api/certification';
import { treeWorkTypes } from '../../api/exam';
import { PermissionGate } from '../../directives/permission';
import { useNavStore } from '../../stores/navStore';
import { LevelScopeRangeSelect } from '../../components/LevelScopeRangeSelect';
import { flattenWorkTypeOptions, uniqueWorkTypeIdForTitle, workTypeTitleForId } from '../../utils/workTypeOptions';
import { CharterEditor } from './charter/CharterEditor';
import { CharterViewer } from './charter/CharterViewer';
import { WbsEditor } from './wbs/WbsEditor';
import { WbsViewer } from './wbs/WbsViewer';
import { applyContactIds, buildExamInfoFromTask, buildScheduleTaskFields, buildScheduleTaskPayload, validateSchedulePlan, } from './schedule/constants';
import { ScheduleEditor } from './schedule/ScheduleEditor';
import { ScheduleViewer } from './schedule/ScheduleViewer';
import { TaskFlowPanel } from './TaskFlowPanel';
function hasDuplicateScopeLevel(scopes, currentIndex) {
    const currentScope = scopes?.[currentIndex];
    const workTypeName = currentScope?.workTypeName?.trim();
    const workTypeKey = currentScope?.workTypeId || (workTypeName ? `legacy:${workTypeName}` : undefined);
    const currentLevels = new Set(levelCodesInRange(currentScope?.levelRange));
    if (!workTypeKey || currentLevels.size === 0) {
        return false;
    }
    return (scopes ?? []).some((scope, index) => (index !== currentIndex
        && (scope?.workTypeId || (scope?.workTypeName?.trim() ? `legacy:${scope.workTypeName.trim()}` : undefined)) === workTypeKey
        && levelCodesInRange(scope.levelRange).some((levelCode) => currentLevels.has(levelCode))));
}
// 任务阶段（current_phase）按四阶段流程对齐：任务创建 → 认定筹备 → 认定归档 → 认定完成
const stageOptions = [
    { label: '任务创建阶段', value: 'task_creation' },
    { label: '认定筹备阶段', value: 'preparation' },
    { label: '认定归档阶段', value: 'archive' },
    { label: '认定完成', value: 'certification_completed' },
];
// 任务状态（status）映射阶段内的工作进度
const statusOptions = [
    { label: '待补充完整', value: 'pending_complete' },
    { label: '进行中', value: 'in_progress' },
    { label: '待审核', value: 'pending_review' },
    { label: '已驳回', value: 'rejected' },
    { label: '已完成', value: 'completed' },
];
const levelOptions = [
    { label: '初级', value: 'junior' },
    { label: '中级', value: 'intermediate' },
    { label: '高级', value: 'senior' },
    { label: '技师', value: 'technician' },
    { label: '高级技师', value: 'senior_technician' },
];
const documentMeta = {
    charter: {
        label: '任务章程',
        view: 'certification.project.charter.view',
        edit: 'certification.project.charter.edit',
        submit: 'certification.project.charter.submit',
    },
    members: {
        label: '项目成员',
        view: 'certification.project.members.view',
        edit: 'certification.project.members.edit',
        submit: 'certification.project.members.submit',
    },
    wbs: {
        label: '项目WBS',
        view: 'certification.project.wbs.view',
        edit: 'certification.project.wbs.edit',
        submit: 'certification.project.wbs.submit',
    },
    schedule: {
        label: '项目进度',
        view: 'certification.project.schedule.view',
        edit: 'certification.project.schedule.edit',
        submit: 'certification.project.schedule.submit',
    },
    risk: {
        label: '风险管理',
        view: 'certification.project.risk.view',
        edit: 'certification.project.risk.edit',
        submit: 'certification.project.risk.submit',
    },
};
/** 待接收任务面板：预留订单接入能力，当前关闭展示 */
const PENDING_ORDERS_PANEL_ENABLED = false;
const pendingOrders = [
    {
        id: 'O-20260529-001',
        orderNo: 'ORDER-2026-0529-01',
        taskName: '电解铝高级工职业技能认定',
        companyName: '千铝国昱',
        workTypeName: '电工',
        levelCode: 'senior',
        levelName: '高级',
        plannedPeopleCount: 60,
        examDate: '2026-06-18',
    },
    {
        id: 'O-20260529-002',
        orderNo: 'ORDER-2026-0529-02',
        taskName: '焊工中级工企业专项认定',
        companyName: '国昱制造',
        workTypeName: '焊工',
        levelCode: 'intermediate',
        levelName: '中级',
        plannedPeopleCount: 42,
        examDate: '2026-06-25',
    },
];
export function ProjectManagementPage() {
    const queryClient = useQueryClient();
    const navigateTo = useNavStore((state) => state.setSelectedKey);
    const tabsAnchorRef = useRef(null);
    const [query, setQuery] = useState({ page: 1, pageSize: 10 });
    const [selectedTaskId, setSelectedTaskId] = useState();
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);
    const [activeView, setActiveView] = useState('workbench');
    const [charterMode, setCharterMode] = useState('view');
    const [wbsMode, setWbsMode] = useState('view');
    const [scheduleMode, setScheduleMode] = useState('view');
    const [collapsedTaskList, setCollapsedTaskList] = useState(true);
    const [collapsedTabs, setCollapsedTabs] = useState(false);
    const [collapsedPendingOrders, setCollapsedPendingOrders] = useState(false);
    const [taskModalOpen, setTaskModalOpen] = useState(false);
    const [editingTask, setEditingTask] = useState(null);
    const [editorType, setEditorType] = useState(null);
    const [editingIndex, setEditingIndex] = useState(null);
    const [memberDraft, setMemberDraft] = useState([]);
    const [wbsDraft, setWbsDraft] = useState([]);
    const [scheduleDraft, setScheduleDraft] = useState([]);
    const [scheduleTaskDraft, setScheduleTaskDraft] = useState({});
    const [scheduleExamDraft, setScheduleExamDraft] = useState([]);
    const [riskDraft, setRiskDraft] = useState([]);
    const [taskForm] = Form.useForm();
    const [editorForm] = Form.useForm();
    const tasksQuery = useQuery({
        queryKey: ['certification-tasks', query],
        queryFn: () => pageCertificationTasks(query),
    });
    const workTypeQuery = useQuery({
        queryKey: ['exam-work-types-tree'],
        queryFn: treeWorkTypes,
        enabled: taskModalOpen,
        staleTime: 5 * 60 * 1000,
    });
    const workTypeOptions = useMemo(() => flattenWorkTypeOptions(workTypeQuery.data ?? []), [workTypeQuery.data]);
    const watchedScopes = Form.useWatch('scopes', taskForm);
    const hasUnmappedLegacyWorkType = watchedScopes?.some((scope) => !scope.workTypeId && Boolean(scope.workTypeName?.trim())) ?? false;
    useEffect(() => {
        if (!taskModalOpen || !workTypeQuery.data?.length)
            return;
        const scopes = taskForm.getFieldValue('scopes');
        if (!scopes?.length)
            return;
        let changed = false;
        const mappedScopes = scopes.map((scope) => {
            if (scope.workTypeId || !scope.workTypeName?.trim())
                return scope;
            const id = uniqueWorkTypeIdForTitle(workTypeQuery.data, scope.workTypeName);
            if (!id)
                return scope;
            changed = true;
            return { ...scope, workTypeId: id, workTypeName: workTypeTitleForId(workTypeQuery.data, id) };
        });
        if (changed)
            taskForm.setFieldValue('scopes', mappedScopes);
    }, [taskForm, taskModalOpen, workTypeQuery.data]);
    const tasks = useMemo(() => tasksQuery.data?.items || [], [tasksQuery.data]);
    const memberUsersQuery = useQuery({
        queryKey: ['certification-member-users'],
        queryFn: listCertificationMemberUsers,
    });
    const memberUsers = useMemo(() => memberUsersQuery.data || [], [memberUsersQuery.data]);
    useEffect(() => {
        if (!selectedTaskId && tasks.length > 0) {
            setSelectedTaskId(tasks[0].id);
        }
        if (selectedTaskId && tasks.length > 0 && !tasks.some((task) => task.id === selectedTaskId)) {
            setSelectedTaskId(tasks[0].id);
        }
    }, [selectedTaskId, tasks]);
    const workbenchQuery = useQuery({
        queryKey: ['certification-workbench', selectedTaskId],
        queryFn: () => getCertificationWorkbench(selectedTaskId),
        enabled: Boolean(selectedTaskId),
    });
    const workbench = workbenchQuery.data;
    const selectedTask = workbench?.task || tasks.find((task) => task.id === selectedTaskId);
    const handlePhaseClick = (phase) => {
        const targetMap = {
            preparation: 'certification_preparation',
            archive: 'certification_archive',
            certification_completed: 'certification_task_archive',
        };
        const phaseCode = phase.phaseCode === 'project_creation' ? 'task_creation' : phase.phaseCode;
        if (phaseCode === 'task_creation') {
            setCollapsedTabs(false);
            setActiveView('workbench');
            requestAnimationFrame(() => {
                tabsAnchorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
            return;
        }
        const target = targetMap[phaseCode];
        if (target) {
            navigateTo(target);
        }
    };
    useEffect(() => {
        if (!workbench) {
            return;
        }
        setMemberDraft(workbench.members || []);
        setWbsDraft(workbench.wbsItems || []);
        setScheduleDraft(workbench.scheduleItems || []);
        setScheduleTaskDraft(buildScheduleTaskFields(workbench.task, workbench.scheduleItems || []));
        setScheduleExamDraft(buildExamInfoFromTask(workbench.task));
        setRiskDraft(workbench.risks || []);
    }, [workbench]);
    useEffect(() => {
        setCharterMode('view');
        setWbsMode('view');
        setScheduleMode('view');
    }, [selectedTaskId]);
    const refreshTasks = useCallback(() => {
        queryClient.invalidateQueries({ queryKey: ['certification-tasks'] });
        if (selectedTaskId) {
            queryClient.invalidateQueries({ queryKey: ['certification-workbench', selectedTaskId] });
        }
    }, [queryClient, selectedTaskId]);
    const createMutation = useMutation({
        mutationFn: createCertificationTask,
        onSuccess: (task) => {
            message.success('认定项目任务已创建');
            setTaskModalOpen(false);
            setSelectedTaskId(task.id);
            taskForm.resetFields();
            // 立即写入缓存，避免“保存后立刻再编辑”读到旧数据（尤其是 scopes）
            queryClient.setQueriesData({ queryKey: ['certification-tasks'] }, (old) => {
                const data = old;
                if (!data?.items) {
                    return old;
                }
                const exists = data.items.some((item) => item.id === task.id);
                return {
                    ...data,
                    items: exists ? data.items.map((item) => (item.id === task.id ? task : item)) : [task, ...data.items],
                };
            });
            queryClient.setQueryData(['certification-task', task.id], task);
            if (selectedTaskId === task.id) {
                queryClient.invalidateQueries({ queryKey: ['certification-workbench', task.id] });
            }
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updateCertificationTask(id, payload),
        onSuccess: (task) => {
            message.success('认定项目任务已更新');
            setTaskModalOpen(false);
            setEditingTask(null);
            taskForm.resetFields();
            // 同步更新列表与工作台缓存，确保 scopes 等字段回显一致
            queryClient.setQueriesData({ queryKey: ['certification-tasks'] }, (old) => {
                const data = old;
                if (!data?.items) {
                    return old;
                }
                return {
                    ...data,
                    items: data.items.map((item) => (item.id === task.id ? task : item)),
                };
            });
            queryClient.setQueryData(['certification-workbench', task.id], (old) => {
                if (!old) {
                    return old;
                }
                const wb = old;
                return { ...wb, task };
            });
            queryClient.setQueryData(['certification-task', task.id], task);
            queryClient.invalidateQueries({ queryKey: ['certification-workbench', task.id] });
        },
    });
    const deleteMutation = useMutation({
        mutationFn: deleteCertificationTask,
        onSuccess: () => {
            message.success('认定项目任务已删除');
            setSelectedRowKeys([]);
            refreshTasks();
        },
    });
    const batchDeleteMutation = useMutation({
        mutationFn: async (ids) => {
            await Promise.all(ids.map((id) => deleteCertificationTask(id)));
        },
        onSuccess: () => {
            message.success('批量删除已完成');
            setSelectedRowKeys([]);
            refreshTasks();
        },
    });
    const saveDocumentMutation = useMutation({
        mutationFn: ({ type, payload }) => saveCertificationDocument(selectedTaskId, type, payload),
        onSuccess: () => {
            message.success('文档内容已保存');
            refreshTasks();
        },
    });
    const submitDocumentMutation = useMutation({
        mutationFn: (type) => submitCertificationDocument(selectedTaskId, type),
        onSuccess: () => {
            message.success('文档已提交');
            refreshTasks();
        },
    });
    const reviewDocumentMutation = useMutation({
        mutationFn: ({ type, stage, approved, comment, }) => {
            const payload = { approved, comment };
            return stage === 'initial'
                ? initialReviewCertificationDocument(selectedTaskId, type, payload)
                : finalReviewCertificationDocument(selectedTaskId, type, payload);
        },
        onSuccess: () => {
            message.success('Review completed');
            refreshTasks();
        },
    });
    const reviewDocument = useCallback((type, stage, approved) => {
        if (approved) {
            reviewDocumentMutation.mutate({ type, stage, approved, comment: 'approved' });
            return;
        }
        let comment = '';
        Modal.confirm({
            title: 'Enter rejection reason',
            content: (_jsx(Input.TextArea, { rows: 4, maxLength: 1000, showCount: true, onChange: (event) => {
                    comment = event.target.value;
                } })),
            okText: '确认驳回',
            cancelText: '取消',
            onOk: () => {
                if (!comment.trim()) {
                    message.warning('驳回原因不能为空');
                    return Promise.reject();
                }
                return reviewDocumentMutation.mutateAsync({ type, stage, approved: false, comment });
            },
        });
    }, [reviewDocumentMutation]);
    const saveMembersMutation = useMutation({
        mutationFn: () => saveCertificationMembers(selectedTaskId, memberDraft),
        onSuccess: () => {
            message.success('项目成员已保存');
            refreshTasks();
        },
    });
    const saveWbsMutation = useMutation({
        mutationFn: () => saveCertificationWbsItems(selectedTaskId, wbsDraft),
        onSuccess: () => {
            message.success('项目WBS已保存');
            setWbsMode('view');
            refreshTasks();
        },
    });
    const resetWbsDraft = useCallback(() => {
        setWbsDraft(workbench?.wbsItems || []);
    }, [workbench?.wbsItems]);
    const resetScheduleDraft = useCallback(() => {
        setScheduleDraft(workbench?.scheduleItems || []);
        if (workbench?.task) {
            setScheduleTaskDraft(buildScheduleTaskFields(workbench.task, workbench.scheduleItems || []));
            setScheduleExamDraft(buildExamInfoFromTask(workbench.task));
        }
    }, [workbench?.scheduleItems, workbench?.task]);
    const saveScheduleMutation = useMutation({
        mutationFn: async () => {
            if (!selectedTaskId || !selectedTask) {
                throw new Error('未选择任务');
            }
            const error = validateSchedulePlan(scheduleTaskDraft, scheduleExamDraft, scheduleDraft);
            if (error) {
                throw new Error(error);
            }
            const items = applyContactIds(scheduleDraft, scheduleTaskDraft);
            await updateCertificationTask(selectedTaskId, buildScheduleTaskPayload(selectedTask, scheduleTaskDraft, scheduleExamDraft));
            return saveCertificationScheduleItems(selectedTaskId, items);
        },
        onSuccess: () => {
            message.success('进度计划已保存');
            setScheduleMode('view');
            refreshTasks();
        },
        onError: (error) => {
            message.warning(error.message || '保存失败');
        },
    });
    const toggleScheduleCompletedMutation = useMutation({
        mutationFn: (items) => {
            const merged = applyContactIds(items, scheduleTaskDraft);
            return saveCertificationScheduleItems(selectedTaskId, merged);
        },
        onSuccess: (items) => {
            setScheduleDraft(items);
            message.success('进度已更新');
            refreshTasks();
        },
    });
    const saveRiskMutation = useMutation({
        mutationFn: () => saveCertificationRisks(selectedTaskId, riskDraft),
        onSuccess: () => {
            message.success('风险清单已保存');
            refreshTasks();
        },
    });
    const toggleRiskMutation = useMutation({
        mutationFn: ({ riskId, status }) => toggleCertificationRiskStatus(selectedTaskId, riskId, { status }),
        onSuccess: () => {
            message.success('风险状态已更新');
            refreshTasks();
        },
    });
    const openCreate = useCallback((order) => {
        setEditingTask(null);
        taskForm.resetFields();
        const now = dayjs();
        const orderLevelCode = resolveLevelCode(order?.levelCode, order?.levelName);
        const orderWorkTypeId = uniqueWorkTypeIdForTitle(workTypeQuery.data ?? [], order?.workTypeName);
        const scopes = order
            ? [{
                    workTypeId: orderWorkTypeId,
                    workTypeName: order.workTypeName,
                    levelRange: orderLevelCode ? exactLevelRange(orderLevelCode) : undefined,
                    legacyLevelCode: orderLevelCode ? undefined : order.levelCode,
                    legacyLevelName: orderLevelCode ? undefined : order.levelName,
                }]
            : [{ workTypeId: undefined, workTypeName: '', levelRange: undefined }];
        taskForm.setFieldsValue({
            taskNo: order ? order.orderNo.replace('ORDER', 'RD') : `RD-${now.format('YYYYMMDD-HHmm')}`,
            taskName: order?.taskName,
            companyName: order?.companyName,
            scopes,
            plannedPeopleCount: order?.plannedPeopleCount,
            planStartDate: now,
            planEndDate: order ? dayjs(order.examDate) : now.add(14, 'day'),
            examDate: order ? dayjs(order.examDate) : now.add(14, 'day'),
            certificationType: 'with_question_bank',
            sourceType: order ? 'order' : 'manual',
            status: 'pending_complete',
            currentPhase: 'preparation',
        });
        setTaskModalOpen(true);
    }, [taskForm, workTypeQuery.data]);
    const openEdit = useCallback(async (task) => {
        setEditingTask(task);
        setTaskModalOpen(true);
        taskForm.resetFields();
        let detail = task;
        try {
            detail = await getCertificationTask(task.id);
            setEditingTask(detail);
            queryClient.setQueryData(['certification-task', task.id], detail);
        }
        catch {
            message.warning('获取任务详情失败，已使用列表数据');
        }
        taskForm.setFieldsValue(buildTaskFormValues(detail, workTypeQuery.data ?? []));
    }, [queryClient, taskForm, workTypeQuery.data]);
    const handleTaskSubmit = useCallback(() => {
        taskForm.validateFields().then(() => {
            const values = taskForm.getFieldsValue(true);
            const payload = {
                taskNo: values.taskNo,
                taskName: values.taskName,
                taskManagerName: values.taskManagerName,
                companyName: values.companyName,
                certificationType: values.certificationType,
                scopes: toScopePayload(values.scopes),
                plannedPeopleCount: values.plannedPeopleCount,
                planStartDate: formatDate(values.planStartDate),
                planEndDate: formatDate(values.planEndDate),
                examDate: formatDate(values.examDate),
                sourceType: values.sourceType,
                status: values.status,
                currentPhase: values.currentPhase,
                remark: values.remark,
            };
            if (editingTask) {
                updateMutation.mutate({ id: editingTask.id, payload });
            }
            else {
                createMutation.mutate(payload);
            }
        });
    }, [createMutation, editingTask, taskForm, updateMutation]);
    const handleDelete = useCallback((task) => {
        Modal.confirm({
            title: `确认删除 ${task.taskName}？`,
            content: '删除后任务及准备资料将不再出现在项目管理列表中。',
            okText: '确认删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => deleteMutation.mutate(task.id),
        });
    }, [deleteMutation]);
    const handleBatchDelete = useCallback(() => {
        const ids = selectedRowKeys.map(String);
        Modal.confirm({
            title: `确认删除选中的 ${ids.length} 个任务？`,
            content: '批量删除会移除所选任务的项目管理记录。',
            okText: '批量删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => batchDeleteMutation.mutate(ids),
        });
    }, [batchDeleteMutation, selectedRowKeys]);
    const openEditor = useCallback((type, record, index) => {
        setEditorType(type);
        setEditingIndex(index ?? null);
        editorForm.resetFields();
        editorForm.setFieldsValue(toEditorValues(type, record));
    }, [editorForm]);
    const handleEditorSubmit = useCallback(() => {
        editorForm.validateFields().then((values) => {
            if (!editorType) {
                return;
            }
            if (editorType === 'member') {
                upsertDraft(memberDraft, editingIndex, toMember(values), setMemberDraft);
            }
            if (editorType === 'schedule') {
                upsertDraft(scheduleDraft, editingIndex, toSchedule(values), setScheduleDraft);
            }
            if (editorType === 'risk') {
                upsertDraft(riskDraft, editingIndex, toRisk(values), setRiskDraft);
            }
            setEditorType(null);
            setEditingIndex(null);
            editorForm.resetFields();
        });
    }, [editingIndex, editorForm, editorType, memberDraft, riskDraft, scheduleDraft]);
    const taskColumns = [
        {
            title: '任务编号',
            dataIndex: 'taskNo',
            width: 150,
            fixed: 'left',
            render: (value, record) => (_jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setSelectedTaskId(record.id), children: value })),
        },
        { title: '任务名称', dataIndex: 'taskName', width: 230, ellipsis: true },
        {
            title: '资料进度',
            width: 120,
            render: (_, record) => {
                const percent = percentOf(record.documentCompletedCount, record.documentTotalCount);
                return _jsx(Progress, { percent: percent, size: "small" });
            },
        },
        {
            title: '操作',
            width: 190,
            align: 'center',
            fixed: 'right',
            render: (_, record) => (_jsxs(Space, { size: 0, wrap: false, children: [_jsx(Button, { type: "link", size: "small", style: { paddingInline: 0 }, icon: _jsx(FolderOpenOutlined, {}), onClick: () => {
                            setSelectedTaskId(record.id);
                            setCollapsedTabs(false);
                            setActiveView('workbench');
                            requestAnimationFrame(() => {
                                tabsAnchorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                            });
                        }, children: "\u5DE5\u4F5C\u53F0" }), _jsx(PermissionGate, { code: "certification.project.task.create", children: _jsx(Button, { type: "link", size: "small", style: { paddingInline: 0 }, icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), children: "\u7F16\u8F91" }) }), _jsx(PermissionGate, { code: "certification.project.task.delete", children: _jsx(Button, { type: "link", size: "small", style: { paddingInline: 0 }, danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(record), children: "\u5220\u9664" }) })] })),
        },
    ];
    const orderColumns = [
        { title: '订单编号', dataIndex: 'orderNo', width: 180 },
        { title: '任务名称', dataIndex: 'taskName', ellipsis: true },
        { title: '企业', dataIndex: 'companyName', width: 120 },
        { title: '工种', dataIndex: 'workTypeName', width: 100 },
        { title: '等级', dataIndex: 'levelName', width: 90 },
        { title: '计划人数', dataIndex: 'plannedPeopleCount', width: 90 },
        { title: '考试日期', dataIndex: 'examDate', width: 120 },
        {
            title: '操作',
            width: 120,
            render: (_, record) => (_jsx(PermissionGate, { code: "certification.project.task.create", children: _jsx(Button, { type: "link", icon: _jsx(PlusOutlined, {}), onClick: () => openCreate(record), children: "\u63A5\u6536\u5EFA\u9879" }) })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("div", { className: "page-heading knowledge-heading", children: [_jsx("div", { children: _jsxs("div", { className: "page-title-line", children: [_jsx("div", { className: "page-title-icon", children: _jsx(FileDoneOutlined, {}) }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 1, children: "\u8BA4\u5B9A\u9879\u76EE\u7BA1\u7406" }), _jsx(Typography.Text, { type: "secondary", children: "\u4EFB\u52A1\u51C6\u5907\u3001\u8D44\u6599\u7F16\u5236\u3001\u98CE\u9669\u63A7\u5236\u4E0E\u6D41\u7A0B\u63A8\u8FDB" })] })] }) }), _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refreshTasks, children: "\u5237\u65B0" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: () => exportTasks(tasks), children: "\u5BFC\u51FA" }), _jsx(PermissionGate, { code: "certification.project.task.create", children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => openCreate(), children: "\u65B0\u5EFA\u4EFB\u52A1" }) })] })] }), _jsx(Card, { title: "\u4EFB\u52A1\u680F", styles: {
                    body: collapsedTaskList
                        ? { display: 'none', padding: 0 }
                        : { paddingTop: 12 },
                }, extra: (_jsx(Button, { type: "text", icon: collapsedTaskList ? _jsx(DownOutlined, {}) : _jsx(UpOutlined, {}), onClick: () => setCollapsedTaskList((prev) => !prev), children: collapsedTaskList ? '展开' : '折叠' })), children: !collapsedTaskList ? (_jsxs(_Fragment, { children: [_jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u4EFB\u52A1\u7F16\u53F7\u3001\u540D\u79F0\u3001\u7ECF\u7406", style: { width: 260 }, value: query.keyword, onChange: (event) => setQuery((prev) => ({ ...prev, keyword: event.target.value, page: 1 })) }), _jsx(Select, { allowClear: true, placeholder: "\u4EFB\u52A1\u9636\u6BB5", style: { width: 150 }, options: stageOptions, value: query.currentPhase, onChange: (currentPhase) => setQuery((prev) => ({ ...prev, currentPhase, page: 1 })) }), _jsx(Select, { allowClear: true, placeholder: "\u4EFB\u52A1\u72B6\u6001", style: { width: 130 }, options: statusOptions, value: query.status, onChange: (status) => setQuery((prev) => ({ ...prev, status, page: 1 })) }), _jsx(Select, { allowClear: true, placeholder: "\u7B49\u7EA7", style: { width: 140 }, options: levelOptions, value: query.levelCode, onChange: (levelCode) => setQuery((prev) => ({ ...prev, levelCode, page: 1 })) }), _jsx(PermissionGate, { code: "certification.project.task.delete", children: _jsx(Button, { danger: true, icon: _jsx(DeleteOutlined, {}), disabled: selectedRowKeys.length === 0, loading: batchDeleteMutation.isPending, onClick: handleBatchDelete, children: "\u6279\u91CF\u5220\u9664" }) })] }), _jsx(Table, { rowKey: "id", size: "middle", columns: taskColumns, dataSource: tasks, loading: tasksQuery.isLoading, rowSelection: { selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys) }, scroll: { x: 760 }, pagination: {
                                current: query.page,
                                pageSize: query.pageSize,
                                total: tasksQuery.data?.total || 0,
                                showSizeChanger: true,
                                onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                            }, onRow: (record) => ({
                                onClick: () => setSelectedTaskId(record.id),
                            }) })] })) : null }), _jsx(TaskFlowPanel, { phases: workbench?.phases || [], onPhaseClick: handlePhaseClick }), _jsx(Card, { title: "\u4EFB\u52A1\u8D44\u6599\u4E0E\u5DE5\u4F5C\u53F0", styles: {
                    body: collapsedTabs
                        ? { display: 'none', padding: 0 }
                        : { paddingTop: 12 },
                }, extra: (_jsx(Button, { type: "text", icon: collapsedTabs ? _jsx(DownOutlined, {}) : _jsx(UpOutlined, {}), onClick: () => setCollapsedTabs((prev) => !prev), children: collapsedTabs ? '展开' : '折叠' })), children: !collapsedTabs ? (_jsxs(_Fragment, { children: [_jsx("div", { ref: tabsAnchorRef }), _jsx(Tabs, { activeKey: activeView, onChange: setActiveView, items: [
                                { key: 'workbench', label: '任务工作台', children: renderWorkbenchPanel() },
                                { key: 'charter', label: '任务章程', children: renderCharter() },
                                { key: 'members', label: '项目成员', children: renderMembers() },
                                { key: 'wbs', label: '项目WBS', children: renderWbs() },
                                { key: 'schedule', label: '项目进度', children: renderSchedule() },
                                { key: 'risk', label: '风险管理', children: renderRisk() },
                            ] })] })) : null }), renderPendingOrdersPanel(), _jsx(Modal, { title: editingTask ? '编辑认定任务' : '新建认定任务', open: taskModalOpen, onCancel: () => {
                    setTaskModalOpen(false);
                    setEditingTask(null);
                    taskForm.resetFields();
                }, onOk: handleTaskSubmit, confirmLoading: createMutation.isPending || updateMutation.isPending, destroyOnHidden: true, width: 820, okText: "\u4FDD\u5B58", cancelText: "\u53D6\u6D88", children: _jsxs(Form, { form: taskForm, layout: "vertical", children: [hasUnmappedLegacyWorkType ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u5386\u53F2\u5DE5\u79CD\u540D\u79F0\u5C1A\u672A\u6620\u5C04", description: "\u8BE5\u540D\u79F0\u65E0\u6CD5\u552F\u4E00\u5339\u914D\u5F53\u524D\u79DF\u6237\u7684\u5DE5\u79CD\u6811\u8282\u70B9\u3002\u8BF7\u5728\u5DE5\u79CD\u4E0B\u62C9\u6846\u4E2D\u91CD\u65B0\u9009\u62E9\u6709\u6548\u8282\u70B9\u540E\u4FDD\u5B58\u3002" })) : null, _jsxs("div", { style: { display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 12 }, children: [_jsx(Form.Item, { name: "taskNo", label: "\u4EFB\u52A1\u7F16\u53F7", rules: [{ required: true, message: '请输入任务编号' }], children: _jsx(Input, { disabled: Boolean(editingTask) }) }), _jsx(Form.Item, { name: "taskName", label: "\u4EFB\u52A1\u540D\u79F0", rules: [{ required: true, message: '请输入任务名称' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "taskManagerName", label: "\u4EFB\u52A1\u7ECF\u7406\uFF08\u9009\u586B\uFF09", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "companyName", label: "\u4F01\u4E1A\u540D\u79F0", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "certificationType", label: "\u8BA4\u5B9A\u7C7B\u578B", children: _jsx(Select, { options: [
                                            { label: '有题库认定', value: 'with_question_bank' },
                                            { label: '企业专项认定', value: 'enterprise_special' },
                                        ] }) }), _jsx("div", { style: { gridColumn: 'span 3' }, children: _jsx(Form.List, { name: "scopes", children: (fields, { add, remove }) => (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 8, children: [_jsxs("div", { style: { display: 'flex', alignItems: 'center', justifyContent: 'space-between' }, children: [_jsx(Typography.Text, { strong: true, children: "\u8BA4\u5B9A\u8303\u56F4\uFF08\u5DE5\u79CD/\u7B49\u7EA7\uFF09" }), _jsx(Button, { icon: _jsx(PlusOutlined, {}), onClick: () => add({ workTypeName: '', levelRange: undefined }), children: "\u65B0\u589E\u8303\u56F4" })] }), fields.map(({ key, name, ...restField }) => (_jsxs("div", { style: {
                                                        display: 'grid',
                                                        gridTemplateColumns: 'minmax(0, 1fr) minmax(280px, 1fr) 64px',
                                                        gap: 12,
                                                        alignItems: 'start',
                                                    }, children: [_jsx(Form.Item, { ...restField, name: [name, 'workTypeId'], label: "\u5DE5\u79CD", rules: [
                                                                { required: true, message: '请选择工种' },
                                                                {
                                                                    validator: async () => {
                                                                        if (!hasDuplicateScopeLevel(taskForm.getFieldValue('scopes'), name)) {
                                                                            return;
                                                                        }
                                                                        throw new Error('同一工种的等级范围不能重叠');
                                                                    },
                                                                },
                                                            ], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", options: workTypeOptions, loading: workTypeQuery.isLoading, allowClear: true, placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD", notFoundContent: workTypeQuery.isError ? '工种数据加载失败' : undefined, onChange: (workTypeId) => {
                                                                    taskForm.setFieldValue(['scopes', name, 'workTypeName'], workTypeTitleForId(workTypeQuery.data ?? [], workTypeId));
                                                                } }) }), _jsx(Form.Item, { name: [name, 'workTypeName'], hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { ...restField, name: [name, 'levelRange'], label: "\u7B49\u7EA7\u8303\u56F4", rules: [{
                                                                    validator: async () => {
                                                                        if (!hasDuplicateScopeLevel(taskForm.getFieldValue('scopes'), name)) {
                                                                            return;
                                                                        }
                                                                        throw new Error('同一工种的等级范围不能重叠');
                                                                    },
                                                                }], children: _jsx(LevelScopeRangeSelect, { placeholderStart: "\u8D77\u59CB\u7B49\u7EA7", placeholderEnd: "\u7ED3\u675F\u7B49\u7EA7" }) }), _jsx("div", { style: { paddingTop: 30 }, children: _jsx(Button, { type: "text", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => remove(name) }) })] }, key)))] })) }) }), _jsx(Form.Item, { name: "plannedPeopleCount", label: "\u8BA1\u5212\u4EBA\u6570", children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "planStartDate", label: "\u8BA1\u5212\u5F00\u59CB", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "planEndDate", label: "\u8BA1\u5212\u7ED3\u675F", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "examDate", label: "\u8003\u8BD5\u65E5\u671F", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "sourceType", label: "\u6765\u6E90", children: _jsx(Select, { options: [
                                            { label: '手动新增', value: 'manual' },
                                            { label: '任务订单', value: 'order' },
                                        ] }) }), editingTask ? (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "currentPhase", label: "\u4EFB\u52A1\u9636\u6BB5", children: _jsx(Select, { options: stageOptions, placeholder: "\u9009\u62E9\u4EFB\u52A1\u9636\u6BB5" }) }), _jsx(Form.Item, { name: "status", label: "\u4EFB\u52A1\u72B6\u6001", children: _jsx(Select, { options: statusOptions, placeholder: "\u9009\u62E9\u4EFB\u52A1\u72B6\u6001" }) })] })) : null] }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 3, maxLength: 1000 }) })] }) }), _jsx(Modal, { title: editorType ? `${editingIndex === null ? '新增' : '编辑'}${editorTitle(editorType)}` : '', open: editorType !== null, onCancel: () => setEditorType(null), onOk: handleEditorSubmit, destroyOnHidden: true, width: 760, okText: "\u4FDD\u5B58", cancelText: "\u53D6\u6D88", children: _jsxs(Form, { form: editorForm, layout: "vertical", children: [editorType === 'member' && renderMemberFields(memberUsers, (userId) => {
                            const user = memberUsers.find((item) => String(item.id) === userId);
                            if (!user)
                                return;
                            editorForm.setFieldsValue({
                                userId,
                                memberName: user.displayName,
                                roleCode: 'certification_admin',
                                roleName: '认定管理员',
                                phone: user.phone,
                            });
                        }), editorType === 'schedule' && renderScheduleFields(), editorType === 'risk' && renderRiskFields()] }) })] }));
    function renderWorkbenchPanel() {
        return (renderWorkbench(selectedTask, workbench, workbenchQuery.isLoading));
    }
    function renderPendingOrdersPanel() {
        if (!PENDING_ORDERS_PANEL_ENABLED) {
            return null;
        }
        return (_jsx(Card, { title: "\u5F85\u63A5\u6536\u4EFB\u52A1", styles: { body: { paddingTop: 12 } }, extra: (_jsx(Button, { type: "text", icon: collapsedPendingOrders ? _jsx(DownOutlined, {}) : _jsx(UpOutlined, {}), onClick: () => setCollapsedPendingOrders((prev) => !prev), children: collapsedPendingOrders ? '展开' : '折叠' })), children: !collapsedPendingOrders ? (_jsx(PermissionGate, { code: "certification.project.task.select", children: _jsx(Table, { rowKey: "id", columns: orderColumns, dataSource: pendingOrders, pagination: false }) })) : null }));
    }
    function renderCharter() {
        const document = findDocument(workbench?.documents || [], 'charter');
        const lockedForEdit = !isDocumentEditable(document?.status);
        return (_jsx(PermissionGate, { code: documentMeta.charter.view, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(DocumentToolbar, { document: document, type: "charter", onSubmit: () => {
                            submitDocumentMutation.mutate('charter', {
                                onSuccess: () => setCharterMode('view'),
                            });
                        }, submitLoading: submitDocumentMutation.isPending, onInitialReview: (approved) => reviewDocument('charter', 'initial', approved), onFinalReview: (approved) => reviewDocument('charter', 'final', approved), reviewLoading: reviewDocumentMutation.isPending }), charterMode === 'view' ? (_jsxs(_Fragment, { children: [_jsx(CharterViewer, { task: selectedTask, document: document }), _jsx(PermissionGate, { code: documentMeta.charter.edit, children: _jsx(Button, { icon: _jsx(EditOutlined, {}), disabled: lockedForEdit || submitDocumentMutation.isPending, onClick: () => setCharterMode('edit'), children: "\u7F16\u8F91" }) })] })) : (_jsxs(PermissionGate, { code: documentMeta.charter.edit, children: [_jsx(CharterEditor, { task: selectedTask, document: document, saving: saveDocumentMutation.isPending, onSave: (draft) => {
                                    saveDocumentMutation.mutate({
                                        type: 'charter',
                                        payload: {
                                            contentJson: JSON.stringify(draft),
                                            contentText: draft.background || draft.taskName || '',
                                        },
                                    }, {
                                        onSuccess: () => setCharterMode('view'),
                                    });
                                } }), _jsx(Button, { onClick: () => setCharterMode('view'), children: "\u8FD4\u56DE\u67E5\u770B" })] }))] }) }));
    }
    function renderMembers() {
        const document = findDocument(workbench?.documents || [], 'members');
        const lockedForEdit = !isDocumentEditable(document?.status);
        const columns = [
            { title: '姓名', dataIndex: 'memberName', width: 120 },
            { title: '角色', dataIndex: 'roleName', width: 130, render: dash },
            { title: '部门', dataIndex: 'departmentName', width: 120, render: dash },
            { title: '职责', dataIndex: 'responsibility', ellipsis: true, render: dash },
            { title: '投入周期', width: 200, render: (_, row) => `${row.startDate || '-'} 至 ${row.endDate || '-'}` },
            { title: '投入比例', dataIndex: 'workloadPercent', width: 100, render: dash },
            { title: '手机号', dataIndex: 'maskedPhone', width: 120, render: dash },
            { title: '操作', width: 120, render: (_, row, index) => renderRowActions('member', row, index, memberDraft, setMemberDraft, lockedForEdit) },
        ];
        return (_jsx(PermissionGate, { code: documentMeta.members.view, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(DocumentToolbar, { type: "members", document: document, onSubmit: () => submitDocumentMutation.mutate('members'), submitLoading: submitDocumentMutation.isPending, onInitialReview: (approved) => reviewDocument('members', 'initial', approved), onFinalReview: (approved) => reviewDocument('members', 'final', approved), reviewLoading: reviewDocumentMutation.isPending }), _jsx(Space, { children: _jsxs(PermissionGate, { code: documentMeta.members.edit, children: [_jsx(Button, { icon: _jsx(PlusOutlined, {}), disabled: lockedForEdit, onClick: () => openEditor('member'), children: "\u65B0\u589E\u6210\u5458" }), _jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), disabled: lockedForEdit, loading: saveMembersMutation.isPending, style: { marginLeft: 10 }, onClick: () => saveMembersMutation.mutate(), children: "\u4FDD\u5B58\u6210\u5458" })] }) }), _jsx(Table, { rowKey: (row, index) => row.id || String(index), columns: columns, dataSource: memberDraft, pagination: false })] }) }));
    }
    function renderWbs() {
        const document = findDocument(workbench?.documents || [], 'wbs');
        const lockedForEdit = !isDocumentEditable(document?.status);
        const submitWbs = () => {
            submitDocumentMutation.mutate('wbs', {
                onSuccess: () => setWbsMode('view'),
            });
        };
        return (_jsx(PermissionGate, { code: documentMeta.wbs.view, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(DocumentToolbar, { type: "wbs", document: document, onSubmit: submitWbs, submitLoading: submitDocumentMutation.isPending, onInitialReview: (approved) => reviewDocument('wbs', 'initial', approved), onFinalReview: (approved) => reviewDocument('wbs', 'final', approved), reviewLoading: reviewDocumentMutation.isPending }), wbsMode === 'view' ? (_jsx(WbsViewer, { task: selectedTask, document: document, items: wbsDraft, lockedForEdit: lockedForEdit, saveLoading: saveWbsMutation.isPending, onEdit: lockedForEdit ? undefined : () => setWbsMode('edit'), onSaveDraft: () => saveWbsMutation.mutate() })) : (_jsx(PermissionGate, { code: documentMeta.wbs.edit, children: _jsx(WbsEditor, { items: wbsDraft, saving: saveWbsMutation.isPending, onChange: setWbsDraft, onSave: () => saveWbsMutation.mutate(), onPreview: () => setWbsMode('view'), onCancel: () => {
                                resetWbsDraft();
                                setWbsMode('view');
                            } }) }))] }) }));
    }
    function renderSchedule() {
        const document = findDocument(workbench?.documents || [], 'schedule');
        const lockedForEdit = !isDocumentEditable(document?.status);
        const submitSchedule = () => {
            const incompleteCount = scheduleDraft.filter((item) => !item.completed).length;
            if (scheduleDraft.length > 0 && incompleteCount > 0) {
                message.warning(`请先确认准备期关键安排全部完成，当前还有 ${incompleteCount} 项未勾选`);
                return;
            }
            submitDocumentMutation.mutate('schedule', {
                onSuccess: () => setScheduleMode('view'),
            });
        };
        return (_jsx(PermissionGate, { code: documentMeta.schedule.view, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(DocumentToolbar, { type: "schedule", document: document, onSubmit: submitSchedule, submitLoading: submitDocumentMutation.isPending, onInitialReview: (approved) => reviewDocument('schedule', 'initial', approved), onFinalReview: (approved) => reviewDocument('schedule', 'final', approved), reviewLoading: reviewDocumentMutation.isPending }), scheduleMode === 'view' ? (_jsx(ScheduleViewer, { task: selectedTask, document: document, examInfos: scheduleExamDraft, items: scheduleDraft, members: memberDraft, lockedForEdit: lockedForEdit, onEdit: lockedForEdit ? undefined : () => setScheduleMode('edit'), onToggleCompleted: lockedForEdit
                            ? undefined
                            : (items) => toggleScheduleCompletedMutation.mutate(items) })) : (_jsx(PermissionGate, { code: documentMeta.schedule.edit, children: _jsx(ScheduleEditor, { task: selectedTask, taskFields: scheduleTaskDraft, examInfos: scheduleExamDraft, items: scheduleDraft, members: memberDraft, saving: saveScheduleMutation.isPending, onTaskFieldsChange: setScheduleTaskDraft, onExamInfosChange: setScheduleExamDraft, onItemsChange: setScheduleDraft, onSave: () => saveScheduleMutation.mutate(), onPreview: () => setScheduleMode('view'), onCancel: () => {
                                resetScheduleDraft();
                                setScheduleMode('view');
                            } }) }))] }) }));
    }
    function renderRisk() {
        const document = findDocument(workbench?.documents || [], 'risk');
        const lockedForEdit = !isDocumentEditable(document?.status);
        const columns = [
            { title: '编号', dataIndex: 'riskNo', width: 90, render: dash },
            { title: '风险描述', dataIndex: 'riskDescription', ellipsis: true },
            { title: '概率', dataIndex: 'probabilityCode', width: 90, render: renderRiskFactor },
            { title: '影响', dataIndex: 'impactCode', width: 90, render: renderRiskFactor },
            { title: '等级', dataIndex: 'riskLevelCode', width: 120, render: renderRiskLevel },
            { title: '负责人', dataIndex: 'ownerName', width: 100, render: dash },
            { title: '预计关闭', dataIndex: 'expectedCloseDate', width: 120, render: dash },
            { title: '状态', dataIndex: 'status', width: 90, render: renderItemStatus },
            {
                title: '操作',
                width: 210,
                render: (_, row, index) => (_jsxs(Space, { size: 4, children: [renderRowActions('risk', row, index, riskDraft, setRiskDraft, lockedForEdit), _jsx(PermissionGate, { code: documentMeta.risk.edit, children: _jsx(Button, { type: "link", size: "small", disabled: lockedForEdit || !row.id, onClick: () => row.id && toggleRiskMutation.mutate({ riskId: row.id, status: row.status === 'closed' ? 'open' : 'closed' }), children: row.status === 'closed' ? '重开' : '关闭' }) })] })),
            },
        ];
        return (_jsx(PermissionGate, { code: documentMeta.risk.view, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(DocumentToolbar, { type: "risk", document: document, onSubmit: () => submitDocumentMutation.mutate('risk'), submitLoading: submitDocumentMutation.isPending, onInitialReview: (approved) => reviewDocument('risk', 'initial', approved), onFinalReview: (approved) => reviewDocument('risk', 'final', approved), reviewLoading: reviewDocumentMutation.isPending }), _jsx(Space, { children: _jsxs(PermissionGate, { code: documentMeta.risk.edit, children: [_jsx(Button, { icon: _jsx(PlusOutlined, {}), disabled: lockedForEdit, onClick: () => openEditor('risk'), children: "\u65B0\u589E\u98CE\u9669" }), _jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), disabled: lockedForEdit, loading: saveRiskMutation.isPending, style: { marginLeft: 10 }, onClick: () => saveRiskMutation.mutate(), children: "\u4FDD\u5B58\u98CE\u9669" })] }) }), _jsx(Table, { rowKey: (row, index) => row.id || String(index), columns: columns, dataSource: riskDraft, pagination: false })] }) }));
    }
    function renderRowActions(type, row, index, rows, setter, lockedForEdit) {
        return (_jsx(PermissionGate, { code: documentMeta[type === 'member' ? 'members' : type === 'wbs' ? 'wbs' : type === 'schedule' ? 'schedule' : 'risk'].edit, children: _jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), disabled: lockedForEdit, onClick: () => openEditor(type, row, index), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u884C\uFF1F", okText: "\u5220\u9664", cancelText: "\u53D6\u6D88", disabled: lockedForEdit, onConfirm: () => setter(rows.filter((_, i) => i !== index)), children: _jsx(Button, { type: "link", size: "small", danger: true, disabled: lockedForEdit, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] }) }));
    }
}
function DocumentToolbar({ document, type, onSubmit, submitLoading, onInitialReview, onFinalReview, reviewLoading, }) {
    const lockedForEdit = !isDocumentEditable(document?.status);
    const initialReviewing = document?.status === 'initial_reviewing';
    const finalReviewing = document?.status === 'final_reviewing';
    return (_jsxs("div", { style: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }, children: [_jsxs(Space, { children: [_jsx(Typography.Text, { strong: true, children: documentMeta[type].label }), renderDocumentStatus(document?.status), _jsxs(Typography.Text, { type: "secondary", children: ["\u7248\u672C ", document?.versionNo || 1] }), _jsx(Progress, { percent: document?.completionPercent || 0, size: "small", style: { width: 160 } })] }), _jsxs(Space, { children: [_jsx(PermissionGate, { code: "certification.project.document.submit", children: _jsx(Button, { type: "primary", icon: _jsx(SendOutlined, {}), loading: submitLoading, disabled: lockedForEdit, onClick: onSubmit, children: "\u63D0\u4EA4" }) }), initialReviewing ? (_jsxs(PermissionGate, { code: "certification.project.document.initialReview", children: [_jsx(Button, { icon: _jsx(CheckCircleOutlined, {}), loading: reviewLoading, onClick: () => onInitialReview(true), children: "\u521D\u5BA1\u901A\u8FC7" }), _jsx(Button, { danger: true, icon: _jsx(CloseCircleOutlined, {}), loading: reviewLoading, onClick: () => onInitialReview(false), children: "\u521D\u5BA1\u9A73\u56DE" })] })) : null, finalReviewing ? (_jsxs(PermissionGate, { code: "certification.project.document.finalReview", children: [_jsx(Button, { icon: _jsx(CheckCircleOutlined, {}), loading: reviewLoading, onClick: () => onFinalReview(true), children: "\u590D\u6838\u901A\u8FC7" }), _jsx(Button, { danger: true, icon: _jsx(CloseCircleOutlined, {}), loading: reviewLoading, onClick: () => onFinalReview(false), children: "\u590D\u6838\u9A73\u56DE" })] })) : null] })] }));
}
function renderWorkbench(selectedTask, workbench, loading) {
    if (loading) {
        return _jsx(Card, { loading: true });
    }
    if (!selectedTask) {
        return _jsx(Empty, { description: "\u6682\u65E0\u9009\u4E2D\u4EFB\u52A1" });
    }
    const documents = workbench?.documents || [];
    const risks = workbench?.risks || [];
    const scopes = resolveTaskScopes(selectedTask);
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsxs(Descriptions, { bordered: true, size: "small", column: 4, children: [_jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u7F16\u53F7", children: selectedTask.taskNo }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u7ECF\u7406", children: selectedTask.taskManagerName }), _jsx(Descriptions.Item, { label: "\u5F53\u524D\u9636\u6BB5", children: renderTaskPhase(selectedTask.currentPhase, selectedTask.currentPhaseName) }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1\u72B6\u6001", children: renderTaskStatus(selectedTask.status, selectedTask.statusName) }), _jsx(Descriptions.Item, { label: "\u8BA1\u5212\u4EBA\u6570", children: selectedTask.plannedPeopleCount || '-' }), _jsx(Descriptions.Item, { label: "\u8003\u8BD5\u65E5\u671F", children: selectedTask.examDate || '-' })] }), _jsx(Collapse, { size: "small", items: [{
                        key: 'scopes',
                        label: (_jsxs(Space, { size: 8, wrap: true, children: [_jsxs(Typography.Text, { strong: true, children: ["\u8BA4\u5B9A\u8303\u56F4\uFF08", scopes.length, "\uFF09"] }), _jsx(Typography.Text, { type: "secondary", children: scopeSummary(scopes) })] })),
                        children: (_jsx(Table, { rowKey: (row, index) => row.id || `${row.workTypeName}-${row.levelCode}-${index}`, size: "small", pagination: false, dataSource: scopes, locale: { emptyText: '暂无认定范围' }, columns: [
                                { title: '工种', dataIndex: 'workTypeName', render: dash },
                                { title: '等级', dataIndex: 'levelName', render: dash },
                            ] })),
                    }] }), _jsx(Table, { rowKey: "documentType", size: "small", pagination: false, dataSource: documents, columns: [
                    { title: '资料', dataIndex: 'documentName' },
                    { title: '状态', dataIndex: 'status', width: 120, render: renderDocumentStatus },
                    { title: '完成度', dataIndex: 'completionPercent', width: 180, render: (value) => _jsx(Progress, { percent: value || 0, size: "small" }) },
                    { title: '最后保存', dataIndex: 'lastSavedAt', width: 180, render: dash },
                ] }), _jsx(Table, { rowKey: (row, index) => row.id || String(index), size: "small", pagination: false, dataSource: risks, columns: [
                    { title: '风险', dataIndex: 'riskDescription', ellipsis: true },
                    { title: '等级', dataIndex: 'riskLevelCode', width: 120, render: renderRiskLevel },
                    { title: '负责人', dataIndex: 'ownerName', width: 120, render: dash },
                    { title: '状态', dataIndex: 'status', width: 90, render: renderItemStatus },
                ] })] }));
}
function normalizeWorkbench(workbench) {
    return workbench;
}
function renderMemberFields(users, onUserSelect) {
    return (_jsxs("div", { style: { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }, children: [_jsx(Form.Item, { name: "userId", label: "\u6CE8\u518C\u7528\u6237", rules: [{ required: true, message: '请选择认定管理员' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", options: users.map((user) => ({ label: `${user.displayName}（${user.username}）`, value: String(user.id) })), onChange: onUserSelect }) }), _jsx(Form.Item, { name: "memberName", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "roleName", label: "\u89D2\u8272", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "roleCode", label: "\u89D2\u8272\u7F16\u7801", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "departmentName", label: "\u90E8\u95E8", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "startDate", label: "\u6295\u5165\u5F00\u59CB", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "endDate", label: "\u6295\u5165\u7ED3\u675F", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "inputFrequency", label: "\u6295\u5165\u9891\u7387", children: _jsx(Select, { options: [{ label: '全程', value: 'full' }, { label: '按需', value: 'on_demand' }] }) }), _jsx(Form.Item, { name: "workloadPercent", label: "\u6295\u5165\u6BD4\u4F8B", children: _jsx(InputNumber, { min: 0, max: 100, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "phone", label: "\u624B\u673A\u53F7", rules: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "responsibility", label: "\u804C\u8D23", children: _jsx(Input, {}) })] }));
}
function renderScheduleFields() {
    return (_jsxs("div", { style: { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }, children: [_jsx(Form.Item, { name: "dayNo", label: "\u7B2C\u51E0\u5929", children: _jsx(InputNumber, { min: 1, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "planDate", label: "\u8BA1\u5212\u65E5\u671F", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "startDate", label: "\u5F00\u59CB\u65E5\u671F", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "endDate", label: "\u7ED3\u675F\u65E5\u671F", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "itemName", label: "\u4E8B\u9879\u540D\u79F0", rules: [{ required: true, message: '请输入事项名称' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "itemDescription", label: "\u4E8B\u9879\u8BF4\u660E", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "required", label: "\u662F\u5426\u5FC5\u505A", children: _jsx(Select, { options: [{ label: '必做', value: 'true' }, { label: '可选', value: 'false' }] }) }), _jsx(Form.Item, { name: "completed", label: "\u662F\u5426\u5B8C\u6210", children: _jsx(Select, { options: [{ label: '完成', value: 'true' }, { label: '未完成', value: 'false' }] }) })] }));
}
function renderRiskFields() {
    return (_jsxs("div", { style: { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }, children: [_jsx(Form.Item, { name: "riskDescription", label: "\u98CE\u9669\u63CF\u8FF0", rules: [{ required: true, message: '请输入风险描述' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "ownerName", label: "\u8D1F\u8D23\u4EBA", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "probabilityCode", label: "\u6982\u7387", rules: [{ required: true, message: '请选择概率' }], children: _jsx(Select, { options: riskFactorOptions() }) }), _jsx(Form.Item, { name: "impactCode", label: "\u5F71\u54CD", rules: [{ required: true, message: '请选择影响' }], children: _jsx(Select, { options: riskFactorOptions() }) }), _jsx(Form.Item, { name: "expectedCloseDate", label: "\u9884\u8BA1\u5173\u95ED", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: [{ label: '开启', value: 'open' }, { label: '关闭', value: 'closed' }] }) }), _jsx(Form.Item, { name: "responsePlan", label: "\u5E94\u5BF9\u8BA1\u5212", children: _jsx(Input.TextArea, { rows: 4, maxLength: 1000, showCount: true }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input, {}) })] }));
}
function riskFactorOptions() {
    return [
        { label: '低', value: 'low' },
        { label: '中', value: 'medium' },
        { label: '高', value: 'high' },
    ];
}
function editorTitle(type) {
    return {
        member: '项目成员',
        wbs: 'WBS事项',
        schedule: '进度事项',
        risk: '风险事项',
    }[type];
}
function toEditorValues(type, record) {
    const item = (record || {});
    const values = {};
    Object.entries(item).forEach(([key, value]) => {
        if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' || dayjs.isDayjs(value)) {
            values[key] = value;
        }
    });
    ['startDate', 'endDate', 'planDate', 'expectedCloseDate'].forEach((field) => {
        if (typeof values[field] === 'string') {
            values[field] = dayjs(values[field]);
        }
    });
    if (type === 'risk' && !values.status) {
        values.status = 'open';
    }
    return values;
}
function upsertDraft(rows, index, item, setter) {
    if (index === null) {
        setter([...rows, item]);
        return;
    }
    setter(rows.map((row, i) => (i === index ? item : row)));
}
function toMember(values) {
    const phone = stringValue(values.phone);
    return {
        userId: stringValue(values.userId),
        memberName: stringValue(values.memberName) || '',
        roleCode: stringValue(values.roleCode),
        roleName: stringValue(values.roleName),
        departmentName: stringValue(values.departmentName),
        responsibility: stringValue(values.responsibility),
        startDate: formatDate(values.startDate),
        endDate: formatDate(values.endDate),
        inputFrequency: stringValue(values.inputFrequency),
        workloadPercent: numberValue(values.workloadPercent),
        phone,
        maskedPhone: phone && phone.length === 11 ? `${phone.slice(0, 3)}****${phone.slice(7)}` : phone,
    };
}
function toSchedule(values) {
    return {
        dayNo: numberValue(values.dayNo),
        planDate: formatDate(values.planDate),
        startDate: formatDate(values.startDate),
        endDate: formatDate(values.endDate),
        itemName: stringValue(values.itemName) || '',
        itemDescription: stringValue(values.itemDescription),
        required: values.required === true || values.required === 'true',
        completed: values.completed === true || values.completed === 'true',
    };
}
function toRisk(values) {
    const probabilityCode = stringValue(values.probabilityCode) || 'low';
    const impactCode = stringValue(values.impactCode) || 'low';
    const level = localRiskLevel(probabilityCode, impactCode);
    return {
        riskDescription: stringValue(values.riskDescription) || '',
        probabilityCode,
        impactCode,
        riskLevelCode: level.code,
        riskLevelScore: level.score,
        responsePlan: stringValue(values.responsePlan),
        ownerName: stringValue(values.ownerName),
        status: stringValue(values.status) || 'open',
        expectedCloseDate: formatDate(values.expectedCloseDate),
        remark: stringValue(values.remark),
    };
}
function localRiskLevel(probabilityCode, impactCode) {
    const scoreMap = { low: 1, medium: 2, high: 3 };
    const score = (scoreMap[probabilityCode] || 1) + (scoreMap[impactCode] || 1) - 1;
    const code = score >= 5 ? 'high' : score === 4 ? 'medium_high' : score === 3 ? 'medium' : score === 2 ? 'medium_low' : 'low';
    return { code, score };
}
function findDocument(documents, type) {
    return documents.find((document) => document.documentType === type);
}
function isDocumentEditable(status) {
    return !status || status === 'draft' || status === 'initial_rejected' || status === 'final_rejected';
}
// 任务状态的标签颜色
function renderTaskStatus(status, statusName) {
    const labelMap = {
        pending_complete: '待补充完整',
        in_progress: '进行中',
        pending_review: '待审核',
        rejected: '已驳回',
        completed: '已完成',
    };
    const colorMap = {
        pending_complete: 'warning',
        in_progress: 'processing',
        pending_review: 'warning',
        rejected: 'error',
        completed: 'success',
    };
    const color = colorMap[status] ?? 'default';
    const label = statusName || labelMap[status] || status || '-';
    return _jsx(Tag, { color: color, children: label });
}
// 任务阶段（4 值）的标签颜色
function renderTaskPhase(phase, phaseName) {
    const labelMap = {
        task_creation: '任务创建阶段',
        preparation: '认定筹备阶段',
        archive: '认定归档阶段',
        certification_completed: '认定完成',
    };
    const colorMap = {
        task_creation: 'processing',
        preparation: 'gold',
        archive: 'purple',
        certification_completed: 'success',
    };
    const normalized = phase === 'project_creation' ? 'task_creation' : phase;
    const color = colorMap[normalized ?? ''] ?? 'default';
    const label = phaseName || labelMap[normalized ?? ''] || normalized || '-';
    return _jsx(Tag, { color: color, children: label });
}
function renderDocumentStatus(status) {
    const nextLabelMap = {
        draft: '草稿',
        submitted: '已提交',
        initial_reviewing: '初审中',
        initial_rejected: '初审驳回',
        final_reviewing: '复核中',
        final_rejected: '复核驳回',
        archived: '已归档',
    };
    const nextLabel = status ? nextLabelMap[status] || status : '草稿';
    const nextColor = status === 'archived'
        ? 'success'
        : status === 'initial_rejected' || status === 'final_rejected'
            ? 'error'
            : status === 'submitted' || status === 'initial_reviewing' || status === 'final_reviewing'
                ? 'processing'
                : 'default';
    return _jsx(Tag, { color: nextColor, children: nextLabel });
}
function renderItemStatus(value) {
    const labelMap = {
        pending: '待开始',
        running: '进行中',
        completed: '已完成',
        open: '开启',
        closed: '关闭',
    };
    const color = value === 'completed' || value === 'closed' ? 'success' : value === 'running' || value === 'open' ? 'processing' : 'default';
    return _jsx(Tag, { color: color, children: value ? labelMap[value] || value : '-' });
}
function renderRiskFactor(value) {
    const labelMap = { low: '低', medium: '中', high: '高' };
    return labelMap[value || ''] || '-';
}
function renderRiskLevel(value) {
    const labelMap = {
        low: '低',
        medium_low: '中低',
        medium: '中',
        medium_high: '中高',
        high: '高',
    };
    const color = value === 'high' ? 'error' : value === 'medium_high' ? 'warning' : value === 'medium' ? 'processing' : 'default';
    return _jsx(Tag, { color: color, children: labelMap[value || ''] || '-' });
}
function percentOf(done, total) {
    return total ? Math.round(((done || 0) / total) * 100) : 0;
}
function dash(value) {
    return value === undefined || value === null || value === '' ? '-' : String(value);
}
function stringValue(value) {
    return typeof value === 'string' ? value : value === undefined || value === null ? undefined : String(value);
}
function numberValue(value) {
    return typeof value === 'number' ? value : value === undefined || value === null || value === '' ? undefined : Number(value);
}
function formatDate(value) {
    if (!value) {
        return undefined;
    }
    if (dayjs.isDayjs(value)) {
        return value.format('YYYY-MM-DD');
    }
    return String(value);
}
function resolveLevelCode(levelCode, levelName) {
    const normalizedCode = levelCode?.trim();
    if (normalizedCode && levelOptions.some((option) => option.value === normalizedCode)) {
        return normalizedCode;
    }
    const normalizedName = levelName?.trim();
    return levelOptions.find((option) => option.label === normalizedName)?.value;
}
function exactLevelRange(levelCode) {
    const option = levelOptions.find((item) => item.value === levelCode);
    return option ? `${option.label}->${option.label}` : undefined;
}
function levelCodesInRange(levelRange) {
    const [startName, endName] = (levelRange || '').split('->').map((part) => part.trim());
    const startIndex = levelOptions.findIndex((option) => option.label === startName);
    const endIndex = levelOptions.findIndex((option) => option.label === (endName || startName));
    if (startIndex < 0 || endIndex < 0) {
        return [];
    }
    return levelOptions
        .slice(Math.min(startIndex, endIndex), Math.max(startIndex, endIndex) + 1)
        .map((option) => option.value);
}
function levelRangesFromCodes(levelCodes) {
    const indexes = [...new Set(levelCodes)]
        .map((levelCode) => levelOptions.findIndex((option) => option.value === levelCode))
        .filter((index) => index >= 0)
        .sort((left, right) => left - right);
    const ranges = [];
    let start = indexes[0];
    let previous = indexes[0];
    for (const index of indexes.slice(1)) {
        if (index !== previous + 1) {
            const first = levelOptions[start];
            const last = levelOptions[previous];
            ranges.push(`${first.label}->${last.label}`);
            start = index;
        }
        previous = index;
    }
    if (start !== undefined && previous !== undefined) {
        const first = levelOptions[start];
        const last = levelOptions[previous];
        ranges.push(`${first.label}->${last.label}`);
    }
    return ranges;
}
function toScopeFormValues(task, workTypeTree) {
    if (!task.scopes?.length) {
        const levelCode = resolveLevelCode(task.levelCode, task.levelName);
        const workTypeId = uniqueWorkTypeIdForTitle(workTypeTree, task.workTypeName);
        return [{
                workTypeId,
                workTypeName: workTypeTitleForId(workTypeTree, workTypeId) ?? task.workTypeName ?? '',
                levelRange: levelCode ? exactLevelRange(levelCode) : undefined,
                legacyLevelCode: levelCode ? undefined : task.levelCode,
                legacyLevelName: levelCode ? undefined : task.levelName,
            }];
    }
    const grouped = new Map();
    for (const scope of task.scopes) {
        const scopeName = scope.workTypeName ?? '';
        const workTypeId = scope.workTypeId || uniqueWorkTypeIdForTitle(workTypeTree, scopeName);
        const workTypeName = workTypeTitleForId(workTypeTree, workTypeId) ?? scopeName;
        const groupKey = workTypeId ? `id:${workTypeId}` : `name:${scopeName}`;
        const group = grouped.get(groupKey) ?? { workTypeId, workTypeName, levelCodes: [], legacyScopes: [] };
        const levelCode = resolveLevelCode(scope.levelCode, scope.levelName);
        if (levelCode) {
            group.levelCodes.push(levelCode);
        }
        else {
            group.legacyScopes.push({ levelCode: scope.levelCode, levelName: scope.levelName });
        }
        grouped.set(groupKey, group);
    }
    const formScopes = Array.from(grouped.values()).flatMap((group) => [
        ...levelRangesFromCodes(group.levelCodes).map((levelRange) => ({
            workTypeId: group.workTypeId,
            workTypeName: group.workTypeName,
            levelRange,
        })),
        ...group.legacyScopes.map((scope) => ({
            workTypeId: group.workTypeId,
            workTypeName: group.workTypeName,
            legacyLevelCode: scope.levelCode,
            legacyLevelName: scope.levelName,
        })),
    ]);
    return formScopes.length ? formScopes : [{ workTypeName: '', levelRange: undefined }];
}
function toScopePayload(scopes) {
    return (scopes ?? []).flatMap((scope) => {
        const workTypeName = scope.workTypeName?.trim();
        const workTypeId = scope.workTypeId;
        const levelCodes = levelCodesInRange(scope.levelRange);
        if (levelCodes.length) {
            return levelCodes.map((levelCode) => ({
                workTypeId,
                workTypeName,
                levelCode,
                levelName: levelOptions.find((option) => option.value === levelCode)?.label,
            }));
        }
        const legacyScope = {
            workTypeId,
            workTypeName,
            levelCode: scope.legacyLevelCode,
            levelName: scope.legacyLevelName?.trim(),
        };
        return legacyScope.workTypeName || legacyScope.levelCode || legacyScope.levelName ? [legacyScope] : [];
    });
}
function buildTaskFormValues(task, workTypeTree) {
    return {
        taskNo: task.taskNo,
        taskName: task.taskName,
        taskManagerName: task.taskManagerName ?? undefined,
        companyName: task.companyName,
        certificationType: task.certificationType,
        scopes: toScopeFormValues(task, workTypeTree),
        plannedPeopleCount: task.plannedPeopleCount,
        planStartDate: task.planStartDate ? dayjs(task.planStartDate) : undefined,
        planEndDate: task.planEndDate ? dayjs(task.planEndDate) : undefined,
        examDate: task.examDate ? dayjs(task.examDate) : undefined,
        sourceType: task.sourceType,
        status: normalizeTaskStatus(task.status),
        currentPhase: normalizeTaskPhase(task.currentPhase),
        remark: task.remark,
    };
}
// 任务状态（status）允许的取值
function normalizeTaskStatus(status) {
    const allowed = new Set(['pending_complete', 'in_progress', 'pending_review', 'rejected', 'completed']);
    if (status && allowed.has(status)) {
        return status;
    }
    return 'in_progress';
}
// 任务阶段（current_phase）只允许 4 个取值，兼容历史 project_creation 编码
function normalizeTaskPhase(phase) {
    if (phase === 'project_creation') {
        return 'task_creation';
    }
    const allowed = new Set(['task_creation', 'preparation', 'archive', 'certification_completed']);
    if (phase && allowed.has(phase)) {
        return phase;
    }
    return 'task_creation';
}
function resolveTaskScopes(task) {
    return task.scopes || [];
}
function scopeSummary(scopes) {
    if (!scopes?.length) {
        return '暂无范围';
    }
    const labels = scopes
        .map((scope) => [scope.workTypeName, scope.levelName].filter(Boolean).join('/'))
        .filter(Boolean);
    return labels.length > 3 ? `${labels.slice(0, 3).join('、')} 等${labels.length}项` : labels.join('、');
}
function exportTasks(tasks) {
    const header = ['任务编号', '任务名称', '任务经理', '工种', '等级', '当前阶段', '任务状态', '考试日期'];
    const lines = tasks.map((task) => [
        task.taskNo,
        task.taskName,
        task.taskManagerName,
        summarizeScopeField(task.scopes, 'workTypeName', '、'),
        summarizeScopeField(task.scopes, 'levelName', '、'),
        task.currentPhaseName || '',
        task.statusName || '',
        task.examDate || '',
    ]);
    const csv = [header, ...lines]
        .map((line) => line.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
        .join('\n');
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `certification-projects-${dayjs().format('YYYYMMDD-HHmmss')}.csv`;
    link.click();
    URL.revokeObjectURL(url);
}
function summarizeScopeField(scopes, field, separator) {
    return Array.from(new Set((scopes || [])
        .map((scope) => scope[field])
        .filter((value) => Boolean(value))))
        .join(separator);
}
