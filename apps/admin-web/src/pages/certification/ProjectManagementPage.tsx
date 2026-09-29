import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  FileDoneOutlined,
  FolderOpenOutlined,
  DownOutlined,
  UpOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  SendOutlined,
} from '@ant-design/icons';
import {
  Button,
  Alert,
  Card,
  Collapse,
  DatePicker,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Progress,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { useCallback, useEffect, useMemo, useRef, useState, type Key } from 'react';
import {
  createCertificationTask,
  deleteCertificationTask,
  finalReviewCertificationDocument,
  getCertificationTask,
  listCertificationMemberUsers,
  getCertificationWorkbench,
  initialReviewCertificationDocument,
  saveCertificationDocument,
  saveCertificationMembers,
  saveCertificationRisks,
  saveCertificationScheduleItems,
  saveCertificationWbsItems,
  submitCertificationDocument,
  toggleCertificationRiskStatus,
  updateCertificationTask,
  pageCertificationTasks,
} from '../../api/certification';
import { treeWorkTypes } from '../../api/exam';
import { PermissionGate } from '../../directives/permission';
import { useNavStore } from '../../stores/navStore';
import type {
  CertificationDocument,
  CertificationDocumentType,
  CertificationMember,
  CertificationMemberUser,
  CertificationPhase,
  CertificationRisk,
  CertificationScheduleItem,
  CertificationTask,
  CertificationTaskPayload,
  CertificationTaskQuery,
  CertificationWorkbench,
  CertificationWbsItem,
} from '../../types/certification';
import type { ExamWorkType } from '../../types/exam';
import { LevelScopeRangeSelect } from '../../components/LevelScopeRangeSelect';
import { flattenWorkTypeOptions, uniqueWorkTypeIdForTitle, workTypeTitleForId } from '../../utils/workTypeOptions';
import { CharterEditor, type CharterDraft } from './charter/CharterEditor';
import { CharterViewer } from './charter/CharterViewer';
import { WbsEditor } from './wbs/WbsEditor';
import { WbsViewer } from './wbs/WbsViewer';
import {
  applyContactIds,
  buildExamInfoFromTask,
  buildScheduleTaskFields,
  buildScheduleTaskPayload,
  validateSchedulePlan,
  type ScheduleExamInfoItem,
  type ScheduleTaskFields,
} from './schedule/constants';
import { ScheduleEditor } from './schedule/ScheduleEditor';
import { ScheduleViewer } from './schedule/ScheduleViewer';
import { TaskFlowPanel } from './TaskFlowPanel';
type EditorType = 'member' | 'wbs' | 'schedule' | 'risk';

interface PendingOrder {
  id: string;
  orderNo: string;
  taskName: string;
  companyName: string;
  workTypeName: string;
  levelCode: string;
  levelName: string;
  plannedPeopleCount: number;
  examDate: string;
}

interface TaskFormValues {
  taskNo?: string;
  taskName: string;
  taskManagerName?: string;
  companyName?: string;
  certificationType?: string;
  scopes?: Array<{
    workTypeId?: string;
    workTypeName?: string;
    levelRange?: string;
    legacyLevelCode?: string;
    legacyLevelName?: string;
  }>;
  plannedPeopleCount?: number;
  planStartDate?: Dayjs;
  planEndDate?: Dayjs;
  examDate?: Dayjs;
  sourceType?: string;
  status?: string;
  currentPhase?: string;
  remark?: string;
}

function hasDuplicateScopeLevel(
  scopes: TaskFormValues['scopes'],
  currentIndex: number,
): boolean {
  const currentScope = scopes?.[currentIndex];
  const workTypeName = currentScope?.workTypeName?.trim();
  const workTypeKey = currentScope?.workTypeId || (workTypeName ? `legacy:${workTypeName}` : undefined);
  const currentLevels = new Set(levelCodesInRange(currentScope?.levelRange));
  if (!workTypeKey || currentLevels.size === 0) {
    return false;
  }

  return (scopes ?? []).some((scope, index) => (
    index !== currentIndex
    && (scope?.workTypeId || (scope?.workTypeName?.trim() ? `legacy:${scope.workTypeName.trim()}` : undefined)) === workTypeKey
    && levelCodesInRange(scope.levelRange).some((levelCode) => currentLevels.has(levelCode))
  ));
}

interface EditorFormValues {
  [key: string]: string | number | boolean | Dayjs | undefined;
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

const documentMeta: Record<CertificationDocumentType, { label: string; view: string; edit: string; submit: string }> = {
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

const pendingOrders: PendingOrder[] = [
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
  const tabsAnchorRef = useRef<HTMLDivElement | null>(null);
  const [query, setQuery] = useState<CertificationTaskQuery>({ page: 1, pageSize: 10 });
  const [selectedTaskId, setSelectedTaskId] = useState<string>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [activeView, setActiveView] = useState('workbench');
  const [charterMode, setCharterMode] = useState<'view' | 'edit'>('view');
  const [wbsMode, setWbsMode] = useState<'view' | 'edit'>('view');
  const [scheduleMode, setScheduleMode] = useState<'view' | 'edit'>('view');
  const [collapsedTaskList, setCollapsedTaskList] = useState(true);
  const [collapsedTabs, setCollapsedTabs] = useState(false);
  const [collapsedPendingOrders, setCollapsedPendingOrders] = useState(false);
  const [taskModalOpen, setTaskModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<CertificationTask | null>(null);
  const [editorType, setEditorType] = useState<EditorType | null>(null);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [memberDraft, setMemberDraft] = useState<CertificationMember[]>([]);
  const [wbsDraft, setWbsDraft] = useState<CertificationWbsItem[]>([]);
  const [scheduleDraft, setScheduleDraft] = useState<CertificationScheduleItem[]>([]);
  const [scheduleTaskDraft, setScheduleTaskDraft] = useState<ScheduleTaskFields>({});
  const [scheduleExamDraft, setScheduleExamDraft] = useState<ScheduleExamInfoItem[]>([]);
  const [riskDraft, setRiskDraft] = useState<CertificationRisk[]>([]);
  const [taskForm] = Form.useForm<TaskFormValues>();
  const [editorForm] = Form.useForm<EditorFormValues>();

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

  const workTypeOptions = useMemo(
    () => flattenWorkTypeOptions(workTypeQuery.data ?? []),
    [workTypeQuery.data],
  );
  const watchedScopes = Form.useWatch('scopes', taskForm) as TaskFormValues['scopes'];
  const hasUnmappedLegacyWorkType = watchedScopes?.some((scope) =>
    !scope.workTypeId && Boolean(scope.workTypeName?.trim()),
  ) ?? false;

  useEffect(() => {
    if (!taskModalOpen || !workTypeQuery.data?.length) return;
    const scopes = taskForm.getFieldValue('scopes') as TaskFormValues['scopes'];
    if (!scopes?.length) return;
    let changed = false;
    const mappedScopes = scopes.map((scope) => {
      if (scope.workTypeId || !scope.workTypeName?.trim()) return scope;
      const id = uniqueWorkTypeIdForTitle(workTypeQuery.data!, scope.workTypeName);
      if (!id) return scope;
      changed = true;
      return { ...scope, workTypeId: id, workTypeName: workTypeTitleForId(workTypeQuery.data!, id) };
    });
    if (changed) taskForm.setFieldValue('scopes', mappedScopes);
  }, [taskForm, taskModalOpen, workTypeQuery.data]);

  const tasks = useMemo(() => tasksQuery.data?.items || [], [tasksQuery.data]);

  const memberUsersQuery = useQuery({
    queryKey: ['certification-member-users'],
    queryFn: listCertificationMemberUsers,
  });
  const memberUsers = useMemo(
    () => memberUsersQuery.data || [],
    [memberUsersQuery.data],
  );

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
    queryFn: () => getCertificationWorkbench(selectedTaskId!),
    enabled: Boolean(selectedTaskId),
  });

  const workbench = workbenchQuery.data;
  const selectedTask = workbench?.task || tasks.find((task) => task.id === selectedTaskId);

  const handlePhaseClick = (phase: CertificationPhase) => {
    const targetMap: Record<string, string> = {
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
      queryClient.setQueriesData({ queryKey: ['certification-tasks'] }, (old: unknown) => {
        const data = old as { items?: CertificationTask[] } | undefined;
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
    mutationFn: ({ id, payload }: { id: string; payload: CertificationTaskPayload }) =>
      updateCertificationTask(id, payload),
    onSuccess: (task) => {
      message.success('认定项目任务已更新');
      setTaskModalOpen(false);
      setEditingTask(null);
      taskForm.resetFields();
      // 同步更新列表与工作台缓存，确保 scopes 等字段回显一致
      queryClient.setQueriesData({ queryKey: ['certification-tasks'] }, (old: unknown) => {
        const data = old as { items?: CertificationTask[] } | undefined;
        if (!data?.items) {
          return old;
        }
        return {
          ...data,
          items: data.items.map((item) => (item.id === task.id ? task : item)),
        };
      });
      queryClient.setQueryData(['certification-workbench', task.id], (old: unknown) => {
        if (!old) {
          return old;
        }
        const wb = old as CertificationWorkbench;
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
    mutationFn: async (ids: string[]) => {
      await Promise.all(ids.map((id) => deleteCertificationTask(id)));
    },
    onSuccess: () => {
      message.success('批量删除已完成');
      setSelectedRowKeys([]);
      refreshTasks();
    },
  });

  const saveDocumentMutation = useMutation({
    mutationFn: ({ type, payload }: { type: CertificationDocumentType; payload: { contentText?: string; contentJson?: string } }) =>
      saveCertificationDocument(selectedTaskId!, type, payload),
    onSuccess: () => {
      message.success('文档内容已保存');
      refreshTasks();
    },
  });

  const submitDocumentMutation = useMutation({
    mutationFn: (type: CertificationDocumentType) => submitCertificationDocument(selectedTaskId!, type),
    onSuccess: () => {
      message.success('文档已提交');
      refreshTasks();
    },
  });

  const reviewDocumentMutation = useMutation({
    mutationFn: ({
      type,
      stage,
      approved,
      comment,
    }: {
      type: CertificationDocumentType;
      stage: 'initial' | 'final';
      approved: boolean;
      comment?: string;
    }) => {
      const payload = { approved, comment };
      return stage === 'initial'
        ? initialReviewCertificationDocument(selectedTaskId!, type, payload)
        : finalReviewCertificationDocument(selectedTaskId!, type, payload);
    },
    onSuccess: () => {
      message.success('Review completed');
      refreshTasks();
    },
  });

  const reviewDocument = useCallback((
    type: CertificationDocumentType,
    stage: 'initial' | 'final',
    approved: boolean,
  ) => {
    if (approved) {
      reviewDocumentMutation.mutate({ type, stage, approved, comment: 'approved' });
      return;
    }
    let comment = '';
    Modal.confirm({
      title: 'Enter rejection reason',
      content: (
        <Input.TextArea
          rows={4}
          maxLength={1000}
          showCount
          onChange={(event) => {
            comment = event.target.value;
          }}
        />
      ),
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
    mutationFn: () => saveCertificationMembers(selectedTaskId!, memberDraft),
    onSuccess: () => {
      message.success('项目成员已保存');
      refreshTasks();
    },
  });

  const saveWbsMutation = useMutation({
    mutationFn: () => saveCertificationWbsItems(selectedTaskId!, wbsDraft),
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
    onError: (error: Error) => {
      message.warning(error.message || '保存失败');
    },
  });

  const toggleScheduleCompletedMutation = useMutation({
    mutationFn: (items: CertificationScheduleItem[]) => {
      const merged = applyContactIds(items, scheduleTaskDraft);
      return saveCertificationScheduleItems(selectedTaskId!, merged);
    },
    onSuccess: (items) => {
      setScheduleDraft(items);
      message.success('进度已更新');
      refreshTasks();
    },
  });

  const saveRiskMutation = useMutation({
    mutationFn: () => saveCertificationRisks(selectedTaskId!, riskDraft),
    onSuccess: () => {
      message.success('风险清单已保存');
      refreshTasks();
    },
  });

  const toggleRiskMutation = useMutation({
    mutationFn: ({ riskId, status }: { riskId: string; status: string }) =>
      toggleCertificationRiskStatus(selectedTaskId!, riskId, { status }),
    onSuccess: () => {
      message.success('风险状态已更新');
      refreshTasks();
    },
  });

  const openCreate = useCallback((order?: PendingOrder) => {
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

  const openEdit = useCallback(async (task: CertificationTask) => {
    setEditingTask(task);
    setTaskModalOpen(true);
    taskForm.resetFields();
    let detail = task;
    try {
      detail = await getCertificationTask(task.id);
      setEditingTask(detail);
      queryClient.setQueryData(['certification-task', task.id], detail);
    } catch {
      message.warning('获取任务详情失败，已使用列表数据');
    }
    taskForm.setFieldsValue(buildTaskFormValues(detail, workTypeQuery.data ?? []));
  }, [queryClient, taskForm, workTypeQuery.data]);

  const handleTaskSubmit = useCallback(() => {
    taskForm.validateFields().then(() => {
      const values = taskForm.getFieldsValue(true) as TaskFormValues;
      const payload: CertificationTaskPayload = {
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
      } else {
        createMutation.mutate(payload);
      }
    });
  }, [createMutation, editingTask, taskForm, updateMutation]);

  const handleDelete = useCallback((task: CertificationTask) => {
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

  const openEditor = useCallback((type: EditorType, record?: unknown, index?: number) => {
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

  const taskColumns: ColumnsType<CertificationTask> = [
    {
      title: '任务编号',
      dataIndex: 'taskNo',
      width: 150,
      fixed: 'left',
      render: (value: string, record) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => setSelectedTaskId(record.id)}>
          {value}
        </Button>
      ),
    },
    { title: '任务名称', dataIndex: 'taskName', width: 230, ellipsis: true },
    {
      title: '资料进度',
      width: 120,
      render: (_, record) => {
        const percent = percentOf(record.documentCompletedCount, record.documentTotalCount);
        return <Progress percent={percent} size="small" />;
      },
    },
    {
      title: '操作',
      width: 190,
      align: 'center',
      fixed: 'right',
      render: (_, record) => (
        <Space size={0} wrap={false}>
          <Button
            type="link"
            size="small"
            style={{ paddingInline: 0 }}
            icon={<FolderOpenOutlined />}
            onClick={() => {
              setSelectedTaskId(record.id);
              setCollapsedTabs(false);
              setActiveView('workbench');
              requestAnimationFrame(() => {
                tabsAnchorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
              });
            }}
          >
            工作台
          </Button>
          <PermissionGate code="certification.project.task.create">
            <Button type="link" size="small" style={{ paddingInline: 0 }} icon={<EditOutlined />} onClick={() => openEdit(record)}>
              编辑
            </Button>
          </PermissionGate>
          <PermissionGate code="certification.project.task.delete">
            <Button type="link" size="small" style={{ paddingInline: 0 }} danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>
              删除
            </Button>
          </PermissionGate>
        </Space>
      ),
    },
  ];

  const orderColumns: ColumnsType<PendingOrder> = [
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
      render: (_, record) => (
        <PermissionGate code="certification.project.task.create">
          <Button type="link" icon={<PlusOutlined />} onClick={() => openCreate(record)}>
            接收建项
          </Button>
        </PermissionGate>
      ),
    },
  ];

  return (
    <div className="knowledge-page">
      <div className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <div className="page-title-icon"><FileDoneOutlined /></div>
            <div>
              <Typography.Title level={1}>认定项目管理</Typography.Title>
              <Typography.Text type="secondary">任务准备、资料编制、风险控制与流程推进</Typography.Text>
            </div>
          </div>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={refreshTasks}>刷新</Button>
          <Button icon={<DownloadOutlined />} onClick={() => exportTasks(tasks)}>导出</Button>
          <PermissionGate code="certification.project.task.create">
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>新建任务</Button>
          </PermissionGate>
        </Space>
      </div>

      <Card
        title="任务栏"
        styles={{
          body: collapsedTaskList
            ? { display: 'none', padding: 0 }
            : { paddingTop: 12 },
        }}
        extra={(
          <Button
            type="text"
            icon={collapsedTaskList ? <DownOutlined /> : <UpOutlined />}
            onClick={() => setCollapsedTaskList((prev) => !prev)}
          >
            {collapsedTaskList ? '展开' : '折叠'}
          </Button>
        )}
      >
        {!collapsedTaskList ? (
          <>
            <Space wrap style={{ marginBottom: 12 }}>
              <Input
                allowClear
                prefix={<SearchOutlined />}
                placeholder="搜索任务编号、名称、经理"
                style={{ width: 260 }}
                value={query.keyword}
                onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value, page: 1 }))}
              />
              <Select
                allowClear
                placeholder="任务阶段"
                style={{ width: 150 }}
                options={stageOptions}
                value={query.currentPhase}
                onChange={(currentPhase) => setQuery((prev) => ({ ...prev, currentPhase, page: 1 }))}
              />
              <Select
                allowClear
                placeholder="任务状态"
                style={{ width: 130 }}
                options={statusOptions}
                value={query.status}
                onChange={(status) => setQuery((prev) => ({ ...prev, status, page: 1 }))}
              />
              <Select
                allowClear
                placeholder="等级"
                style={{ width: 140 }}
                options={levelOptions}
                value={query.levelCode}
                onChange={(levelCode) => setQuery((prev) => ({ ...prev, levelCode, page: 1 }))}
              />
              <PermissionGate code="certification.project.task.delete">
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  disabled={selectedRowKeys.length === 0}
                  loading={batchDeleteMutation.isPending}
                  onClick={handleBatchDelete}
                >
                  批量删除
                </Button>
              </PermissionGate>
            </Space>
            <Table
              rowKey="id"
              size="middle"
              columns={taskColumns}
              dataSource={tasks}
              loading={tasksQuery.isLoading}
              rowSelection={{ selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys) }}
              scroll={{ x: 760 }}
              pagination={{
                current: query.page,
                pageSize: query.pageSize,
                total: tasksQuery.data?.total || 0,
                showSizeChanger: true,
                onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
              }}
              onRow={(record) => ({
                onClick: () => setSelectedTaskId(record.id),
              })}
            />
          </>
        ) : null}
      </Card>

      <TaskFlowPanel phases={workbench?.phases || []} onPhaseClick={handlePhaseClick} />

      <Card
        title="任务资料与工作台"
        styles={{
          body: collapsedTabs
            ? { display: 'none', padding: 0 }
            : { paddingTop: 12 },
        }}
        extra={(
          <Button
            type="text"
            icon={collapsedTabs ? <DownOutlined /> : <UpOutlined />}
            onClick={() => setCollapsedTabs((prev) => !prev)}
          >
            {collapsedTabs ? '展开' : '折叠'}
          </Button>
        )}
      >
        {!collapsedTabs ? (
          <>
            <div ref={tabsAnchorRef} />
            <Tabs
              activeKey={activeView}
              onChange={setActiveView}
              items={[
                { key: 'workbench', label: '任务工作台', children: renderWorkbenchPanel() },
                { key: 'charter', label: '任务章程', children: renderCharter() },
                { key: 'members', label: '项目成员', children: renderMembers() },
                { key: 'wbs', label: '项目WBS', children: renderWbs() },
                { key: 'schedule', label: '项目进度', children: renderSchedule() },
                { key: 'risk', label: '风险管理', children: renderRisk() },
              ]}
            />
          </>
        ) : null}
      </Card>

      {renderPendingOrdersPanel()}

      <Modal
        title={editingTask ? '编辑认定任务' : '新建认定任务'}
        open={taskModalOpen}
        onCancel={() => {
          setTaskModalOpen(false);
          setEditingTask(null);
          taskForm.resetFields();
        }}
        onOk={handleTaskSubmit}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnHidden
        width={820}
        okText="保存"
        cancelText="取消"
      >
        <Form form={taskForm} layout="vertical">
          {hasUnmappedLegacyWorkType ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message="历史工种名称尚未映射"
              description="该名称无法唯一匹配当前租户的工种树节点。请在工种下拉框中重新选择有效节点后保存。"
            />
          ) : null}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 12 }}>
            <Form.Item name="taskNo" label="任务编号" rules={[{ required: true, message: '请输入任务编号' }]}>
              <Input disabled={Boolean(editingTask)} />
            </Form.Item>
            <Form.Item name="taskName" label="任务名称" rules={[{ required: true, message: '请输入任务名称' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="taskManagerName" label="任务经理（选填）">
              <Input />
            </Form.Item>
            <Form.Item name="companyName" label="企业名称">
              <Input />
            </Form.Item>
            <Form.Item name="certificationType" label="认定类型">
              <Select options={[
                { label: '有题库认定', value: 'with_question_bank' },
                { label: '企业专项认定', value: 'enterprise_special' },
              ]} />
            </Form.Item>
            <div style={{ gridColumn: 'span 3' }}>
              <Form.List name="scopes">
                {(fields, { add, remove }) => (
                  <Space direction="vertical" style={{ width: '100%' }} size={8}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <Typography.Text strong>认定范围（工种/等级）</Typography.Text>
                      <Button icon={<PlusOutlined />} onClick={() => add({ workTypeName: '', levelRange: undefined })}>新增范围</Button>
                    </div>
                    {fields.map(({ key, name, ...restField }) => (
                      <div
                        key={key}
                        style={{
                          display: 'grid',
                          gridTemplateColumns: 'minmax(0, 1fr) minmax(280px, 1fr) 64px',
                          gap: 12,
                          alignItems: 'start',
                        }}
                      >
                        <Form.Item
                          {...restField}
                          name={[name, 'workTypeId']}
                          label="工种"
                          rules={[
                            { required: true, message: '请选择工种' },
                            {
                              validator: async () => {
                                if (!hasDuplicateScopeLevel(taskForm.getFieldValue('scopes'), name)) {
                                  return;
                                }
                                throw new Error('同一工种的等级范围不能重叠');
                              },
                            },
                          ]}
                        >
                          <Select
                            showSearch
                            optionFilterProp="label"
                            options={workTypeOptions}
                            loading={workTypeQuery.isLoading}
                            allowClear
                            placeholder="请选择工种"
                            notFoundContent={workTypeQuery.isError ? '工种数据加载失败' : undefined}
                            onChange={(workTypeId) => {
                              taskForm.setFieldValue(
                                ['scopes', name, 'workTypeName'],
                                workTypeTitleForId(workTypeQuery.data ?? [], workTypeId),
                              );
                            }}
                          />
                        </Form.Item>
                        <Form.Item name={[name, 'workTypeName']} hidden><Input /></Form.Item>
                        <Form.Item
                          {...restField}
                          name={[name, 'levelRange']}
                          label="等级范围"
                          rules={[{
                            validator: async () => {
                              if (!hasDuplicateScopeLevel(taskForm.getFieldValue('scopes'), name)) {
                                return;
                              }
                              throw new Error('同一工种的等级范围不能重叠');
                            },
                          }]}
                        >
                          <LevelScopeRangeSelect placeholderStart="起始等级" placeholderEnd="结束等级" />
                        </Form.Item>
                        <div style={{ paddingTop: 30 }}>
                          <Button type="text" danger icon={<DeleteOutlined />} onClick={() => remove(name)} />
                        </div>
                      </div>
                    ))}
                  </Space>
                )}
              </Form.List>
            </div>
            <Form.Item name="plannedPeopleCount" label="计划人数">
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="planStartDate" label="计划开始">
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="planEndDate" label="计划结束">
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="examDate" label="考试日期">
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="sourceType" label="来源">
              <Select options={[
                { label: '手动新增', value: 'manual' },
                { label: '任务订单', value: 'order' },
              ]} />
            </Form.Item>
            {editingTask ? (
              <>
                <Form.Item name="currentPhase" label="任务阶段">
                  <Select options={stageOptions} placeholder="选择任务阶段" />
                </Form.Item>
                <Form.Item name="status" label="任务状态">
                  <Select options={statusOptions} placeholder="选择任务状态" />
                </Form.Item>
              </>
            ) : null}
          </div>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} maxLength={1000} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editorType ? `${editingIndex === null ? '新增' : '编辑'}${editorTitle(editorType)}` : ''}
        open={editorType !== null}
        onCancel={() => setEditorType(null)}
        onOk={handleEditorSubmit}
        destroyOnHidden
        width={760}
        okText="保存"
        cancelText="取消"
      >
        <Form form={editorForm} layout="vertical">
          {editorType === 'member' && renderMemberFields(memberUsers, (userId) => {
            const user = memberUsers.find((item) => String(item.id) === userId);
            if (!user) return;
            editorForm.setFieldsValue({
              userId,
              memberName: user.displayName,
              roleCode: 'certification_admin',
              roleName: '认定管理员',
              phone: user.phone,
            });
          })}
          {editorType === 'schedule' && renderScheduleFields()}
          {editorType === 'risk' && renderRiskFields()}
        </Form>
      </Modal>
    </div>
  );

  function renderWorkbenchPanel() {
    return (
      renderWorkbench(selectedTask, workbench, workbenchQuery.isLoading)
    );
  }

  function renderPendingOrdersPanel() {
    if (!PENDING_ORDERS_PANEL_ENABLED) {
      return null;
    }
    return (
      <Card
        title="待接收任务"
        styles={{ body: { paddingTop: 12 } }}
        extra={(
          <Button
            type="text"
            icon={collapsedPendingOrders ? <DownOutlined /> : <UpOutlined />}
            onClick={() => setCollapsedPendingOrders((prev) => !prev)}
          >
            {collapsedPendingOrders ? '展开' : '折叠'}
          </Button>
        )}
      >
        {!collapsedPendingOrders ? (
          <PermissionGate code="certification.project.task.select">
            <Table rowKey="id" columns={orderColumns} dataSource={pendingOrders} pagination={false} />
          </PermissionGate>
        ) : null}
      </Card>
    );
  }

  function renderCharter() {
    const document = findDocument(workbench?.documents || [], 'charter');
    const lockedForEdit = !isDocumentEditable(document?.status);
    return (
      <PermissionGate code={documentMeta.charter.view}>
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <DocumentToolbar
            document={document}
            type="charter"
            onSubmit={() => {
              submitDocumentMutation.mutate('charter', {
                onSuccess: () => setCharterMode('view'),
              });
            }}
            submitLoading={submitDocumentMutation.isPending}
            onInitialReview={(approved) => reviewDocument('charter', 'initial', approved)}
            onFinalReview={(approved) => reviewDocument('charter', 'final', approved)}
            reviewLoading={reviewDocumentMutation.isPending}
          />
          {charterMode === 'view' ? (
            <>
              <CharterViewer task={selectedTask} document={document} />
              <PermissionGate code={documentMeta.charter.edit}>
                <Button
                  icon={<EditOutlined />}
                  disabled={lockedForEdit || submitDocumentMutation.isPending}
                  onClick={() => setCharterMode('edit')}
                >
                  编辑
                </Button>
              </PermissionGate>
            </>
          ) : (
            <PermissionGate code={documentMeta.charter.edit}>
              <CharterEditor
                task={selectedTask}
                document={document}
                saving={saveDocumentMutation.isPending}
                onSave={(draft: CharterDraft) => {
                  saveDocumentMutation.mutate(
                    {
                      type: 'charter',
                      payload: {
                        contentJson: JSON.stringify(draft),
                        contentText: draft.background || draft.taskName || '',
                      },
                    },
                    {
                      onSuccess: () => setCharterMode('view'),
                    },
                  );
                }}
              />
              <Button onClick={() => setCharterMode('view')}>返回查看</Button>
            </PermissionGate>
          )}
        </Space>
      </PermissionGate>
    );
  }

  function renderMembers() {
    const document = findDocument(workbench?.documents || [], 'members');
    const lockedForEdit = !isDocumentEditable(document?.status);
    const columns: ColumnsType<CertificationMember> = [
      { title: '姓名', dataIndex: 'memberName', width: 120 },
      { title: '角色', dataIndex: 'roleName', width: 130, render: dash },
      { title: '部门', dataIndex: 'departmentName', width: 120, render: dash },
      { title: '职责', dataIndex: 'responsibility', ellipsis: true, render: dash },
      { title: '投入周期', width: 200, render: (_, row) => `${row.startDate || '-'} 至 ${row.endDate || '-'}` },
      { title: '投入比例', dataIndex: 'workloadPercent', width: 100, render: dash },
      { title: '手机号', dataIndex: 'maskedPhone', width: 120, render: dash },
      { title: '操作', width: 120, render: (_, row, index) => renderRowActions('member', row, index, memberDraft, setMemberDraft, lockedForEdit) },
    ];
    return (
      <PermissionGate code={documentMeta.members.view}>
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <DocumentToolbar
            type="members"
            document={document}
            onSubmit={() => submitDocumentMutation.mutate('members')}
            submitLoading={submitDocumentMutation.isPending}
            onInitialReview={(approved) => reviewDocument('members', 'initial', approved)}
            onFinalReview={(approved) => reviewDocument('members', 'final', approved)}
            reviewLoading={reviewDocumentMutation.isPending}
          />
          <Space>
            <PermissionGate code={documentMeta.members.edit}>
              <Button icon={<PlusOutlined />} disabled={lockedForEdit} onClick={() => openEditor('member')}>新增成员</Button>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                disabled={lockedForEdit}
                loading={saveMembersMutation.isPending}
                style={{ marginLeft: 10 }}
                onClick={() => saveMembersMutation.mutate()}
              >
                保存成员
              </Button>
            </PermissionGate>
          </Space>
          <Table rowKey={(row, index) => row.id || String(index)} columns={columns} dataSource={memberDraft} pagination={false} />
        </Space>
      </PermissionGate>
    );
  }

  function renderWbs() {
    const document = findDocument(workbench?.documents || [], 'wbs');
    const lockedForEdit = !isDocumentEditable(document?.status);
    const submitWbs = () => {
      submitDocumentMutation.mutate('wbs', {
        onSuccess: () => setWbsMode('view'),
      });
    };
    return (
      <PermissionGate code={documentMeta.wbs.view}>
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <DocumentToolbar
            type="wbs"
            document={document}
            onSubmit={submitWbs}
            submitLoading={submitDocumentMutation.isPending}
            onInitialReview={(approved) => reviewDocument('wbs', 'initial', approved)}
            onFinalReview={(approved) => reviewDocument('wbs', 'final', approved)}
            reviewLoading={reviewDocumentMutation.isPending}
          />
          {wbsMode === 'view' ? (
            <WbsViewer
              task={selectedTask}
              document={document}
              items={wbsDraft}
              lockedForEdit={lockedForEdit}
              saveLoading={saveWbsMutation.isPending}
              onEdit={lockedForEdit ? undefined : () => setWbsMode('edit')}
              onSaveDraft={() => saveWbsMutation.mutate()}
            />
          ) : (
            <PermissionGate code={documentMeta.wbs.edit}>
              <WbsEditor
                items={wbsDraft}
                saving={saveWbsMutation.isPending}
                onChange={setWbsDraft}
                onSave={() => saveWbsMutation.mutate()}
                onPreview={() => setWbsMode('view')}
                onCancel={() => {
                  resetWbsDraft();
                  setWbsMode('view');
                }}
              />
            </PermissionGate>
          )}
        </Space>
      </PermissionGate>
    );
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
    return (
      <PermissionGate code={documentMeta.schedule.view}>
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <DocumentToolbar
            type="schedule"
            document={document}
            onSubmit={submitSchedule}
            submitLoading={submitDocumentMutation.isPending}
            onInitialReview={(approved) => reviewDocument('schedule', 'initial', approved)}
            onFinalReview={(approved) => reviewDocument('schedule', 'final', approved)}
            reviewLoading={reviewDocumentMutation.isPending}
          />
          {scheduleMode === 'view' ? (
            <ScheduleViewer
              task={selectedTask}
              document={document}
              examInfos={scheduleExamDraft}
              items={scheduleDraft}
              members={memberDraft}
              lockedForEdit={lockedForEdit}
              onEdit={lockedForEdit ? undefined : () => setScheduleMode('edit')}
              onToggleCompleted={
                lockedForEdit
                  ? undefined
                  : (items) => toggleScheduleCompletedMutation.mutate(items)
              }
            />
          ) : (
            <PermissionGate code={documentMeta.schedule.edit}>
              <ScheduleEditor
                task={selectedTask}
                taskFields={scheduleTaskDraft}
                examInfos={scheduleExamDraft}
                items={scheduleDraft}
                members={memberDraft}
                saving={saveScheduleMutation.isPending}
                onTaskFieldsChange={setScheduleTaskDraft}
                onExamInfosChange={setScheduleExamDraft}
                onItemsChange={setScheduleDraft}
                onSave={() => saveScheduleMutation.mutate()}
                onPreview={() => setScheduleMode('view')}
                onCancel={() => {
                  resetScheduleDraft();
                  setScheduleMode('view');
                }}
              />
            </PermissionGate>
          )}
        </Space>
      </PermissionGate>
    );
  }

  function renderRisk() {
    const document = findDocument(workbench?.documents || [], 'risk');
    const lockedForEdit = !isDocumentEditable(document?.status);
    const columns: ColumnsType<CertificationRisk> = [
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
        render: (_, row, index) => (
          <Space size={4}>
            {renderRowActions('risk', row, index, riskDraft, setRiskDraft, lockedForEdit)}
            <PermissionGate code={documentMeta.risk.edit}>
              <Button
                type="link"
                size="small"
                disabled={lockedForEdit || !row.id}
                onClick={() => row.id && toggleRiskMutation.mutate({ riskId: row.id, status: row.status === 'closed' ? 'open' : 'closed' })}
              >
                {row.status === 'closed' ? '重开' : '关闭'}
              </Button>
            </PermissionGate>
          </Space>
        ),
      },
    ];
    return (
      <PermissionGate code={documentMeta.risk.view}>
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <DocumentToolbar
            type="risk"
            document={document}
            onSubmit={() => submitDocumentMutation.mutate('risk')}
            submitLoading={submitDocumentMutation.isPending}
            onInitialReview={(approved) => reviewDocument('risk', 'initial', approved)}
            onFinalReview={(approved) => reviewDocument('risk', 'final', approved)}
            reviewLoading={reviewDocumentMutation.isPending}
          />
          <Space>
            <PermissionGate code={documentMeta.risk.edit}>
              <Button icon={<PlusOutlined />} disabled={lockedForEdit} onClick={() => openEditor('risk')}>新增风险</Button>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                disabled={lockedForEdit}
                loading={saveRiskMutation.isPending}
                style={{ marginLeft: 10 }}
                onClick={() => saveRiskMutation.mutate()}
              >
                保存风险
              </Button>
            </PermissionGate>
          </Space>
          <Table rowKey={(row, index) => row.id || String(index)} columns={columns} dataSource={riskDraft} pagination={false} />
        </Space>
      </PermissionGate>
    );
  }

  function renderRowActions<T>(
    type: EditorType,
    row: T,
    index: number,
    rows: T[],
    setter: (items: T[]) => void,
    lockedForEdit?: boolean,
  ) {
    return (
      <PermissionGate code={documentMeta[type === 'member' ? 'members' : type === 'wbs' ? 'wbs' : type === 'schedule' ? 'schedule' : 'risk'].edit}>
        <Space size={4}>
          <Button type="link" size="small" icon={<EditOutlined />} disabled={lockedForEdit} onClick={() => openEditor(type, row, index)}>编辑</Button>
          <Popconfirm title="确认删除该行？" okText="删除" cancelText="取消" disabled={lockedForEdit} onConfirm={() => setter(rows.filter((_, i) => i !== index))}>
            <Button type="link" size="small" danger disabled={lockedForEdit} icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      </PermissionGate>
    );
  }
}

function DocumentToolbar({
  document,
  type,
  onSubmit,
  submitLoading,
  onInitialReview,
  onFinalReview,
  reviewLoading,
}: {
  document?: CertificationDocument;
  type: CertificationDocumentType;
  onSubmit: () => void;
  submitLoading: boolean;
  onInitialReview: (approved: boolean) => void;
  onFinalReview: (approved: boolean) => void;
  reviewLoading: boolean;
}) {
  const lockedForEdit = !isDocumentEditable(document?.status);
  const initialReviewing = document?.status === 'initial_reviewing';
  const finalReviewing = document?.status === 'final_reviewing';
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
      <Space>
        <Typography.Text strong>{documentMeta[type].label}</Typography.Text>
        {renderDocumentStatus(document?.status)}
        <Typography.Text type="secondary">版本 {document?.versionNo || 1}</Typography.Text>
        <Progress percent={document?.completionPercent || 0} size="small" style={{ width: 160 }} />
      </Space>
      <Space>
      <PermissionGate code="certification.project.document.submit">
        <Button type="primary" icon={<SendOutlined />} loading={submitLoading} disabled={lockedForEdit} onClick={onSubmit}>
          提交
        </Button>
      </PermissionGate>
        {initialReviewing ? (
          <PermissionGate code="certification.project.document.initialReview">
            <Button icon={<CheckCircleOutlined />} loading={reviewLoading} onClick={() => onInitialReview(true)}>初审通过</Button>
            <Button danger icon={<CloseCircleOutlined />} loading={reviewLoading} onClick={() => onInitialReview(false)}>初审驳回</Button>
          </PermissionGate>
        ) : null}
        {finalReviewing ? (
          <PermissionGate code="certification.project.document.finalReview">
            <Button icon={<CheckCircleOutlined />} loading={reviewLoading} onClick={() => onFinalReview(true)}>复核通过</Button>
            <Button danger icon={<CloseCircleOutlined />} loading={reviewLoading} onClick={() => onFinalReview(false)}>复核驳回</Button>
          </PermissionGate>
        ) : null}
      </Space>
    </div>
  );
}

function renderWorkbench(selectedTask?: CertificationTask, workbench?: NonNullable<ReturnType<typeof normalizeWorkbench>>, loading?: boolean) {
  if (loading) {
    return <Card loading />;
  }
  if (!selectedTask) {
    return <Empty description="暂无选中任务" />;
  }
  const documents = workbench?.documents || [];
  const risks = workbench?.risks || [];
  const scopes = resolveTaskScopes(selectedTask);
  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <Descriptions bordered size="small" column={4}>
        <Descriptions.Item label="任务编号">{selectedTask.taskNo}</Descriptions.Item>
        <Descriptions.Item label="任务经理">{selectedTask.taskManagerName}</Descriptions.Item>
        <Descriptions.Item label="当前阶段">{renderTaskPhase(selectedTask.currentPhase, selectedTask.currentPhaseName)}</Descriptions.Item>
        <Descriptions.Item label="任务状态">{renderTaskStatus(selectedTask.status, selectedTask.statusName)}</Descriptions.Item>
        <Descriptions.Item label="计划人数">{selectedTask.plannedPeopleCount || '-'}</Descriptions.Item>
        <Descriptions.Item label="考试日期">{selectedTask.examDate || '-'}</Descriptions.Item>
      </Descriptions>
      <Collapse
        size="small"
        items={[{
          key: 'scopes',
          label: (
            <Space size={8} wrap>
              <Typography.Text strong>认定范围（{scopes.length}）</Typography.Text>
              <Typography.Text type="secondary">{scopeSummary(scopes)}</Typography.Text>
            </Space>
          ),
          children: (
            <Table
              rowKey={(row, index) => row.id || `${row.workTypeName}-${row.levelCode}-${index}`}
              size="small"
              pagination={false}
              dataSource={scopes}
              locale={{ emptyText: '暂无认定范围' }}
              columns={[
                { title: '工种', dataIndex: 'workTypeName', render: dash },
                { title: '等级', dataIndex: 'levelName', render: dash },
              ]}
            />
          ),
        }]}
      />
      <Table
        rowKey="documentType"
        size="small"
        pagination={false}
        dataSource={documents}
        columns={[
          { title: '资料', dataIndex: 'documentName' },
          { title: '状态', dataIndex: 'status', width: 120, render: renderDocumentStatus },
          { title: '完成度', dataIndex: 'completionPercent', width: 180, render: (value: number) => <Progress percent={value || 0} size="small" /> },
          { title: '最后保存', dataIndex: 'lastSavedAt', width: 180, render: dash },
        ]}
      />
      <Table
        rowKey={(row, index) => row.id || String(index)}
        size="small"
        pagination={false}
        dataSource={risks}
        columns={[
          { title: '风险', dataIndex: 'riskDescription', ellipsis: true },
          { title: '等级', dataIndex: 'riskLevelCode', width: 120, render: renderRiskLevel },
          { title: '负责人', dataIndex: 'ownerName', width: 120, render: dash },
          { title: '状态', dataIndex: 'status', width: 90, render: renderItemStatus },
        ]}
      />
    </Space>
  );
}

function normalizeWorkbench(workbench: unknown) {
  return workbench as {
    documents: CertificationDocument[];
    risks: CertificationRisk[];
  };
}

function renderMemberFields(users: CertificationMemberUser[], onUserSelect: (userId: string) => void) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
      <Form.Item name="userId" label="注册用户" rules={[{ required: true, message: '请选择认定管理员' }]}>
        <Select
          showSearch
          optionFilterProp="label"
          options={users.map((user) => ({ label: `${user.displayName}（${user.username}）`, value: String(user.id) }))}
          onChange={onUserSelect}
        />
      </Form.Item>
      <Form.Item name="memberName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}><Input /></Form.Item>
      <Form.Item name="roleName" label="角色"><Input /></Form.Item>
      <Form.Item name="roleCode" label="角色编码"><Input /></Form.Item>
      <Form.Item name="departmentName" label="部门"><Input /></Form.Item>
      <Form.Item name="startDate" label="投入开始"><DatePicker style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="endDate" label="投入结束"><DatePicker style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="inputFrequency" label="投入频率"><Select options={[{ label: '全程', value: 'full' }, { label: '按需', value: 'on_demand' }]} /></Form.Item>
      <Form.Item name="workloadPercent" label="投入比例"><InputNumber min={0} max={100} style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="phone" label="手机号" rules={[{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号' }]}><Input /></Form.Item>
      <Form.Item name="responsibility" label="职责"><Input /></Form.Item>
    </div>
  );
}

function renderScheduleFields() {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
      <Form.Item name="dayNo" label="第几天"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="planDate" label="计划日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="startDate" label="开始日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="endDate" label="结束日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="itemName" label="事项名称" rules={[{ required: true, message: '请输入事项名称' }]}><Input /></Form.Item>
      <Form.Item name="itemDescription" label="事项说明"><Input /></Form.Item>
      <Form.Item name="required" label="是否必做"><Select options={[{ label: '必做', value: 'true' }, { label: '可选', value: 'false' }]} /></Form.Item>
      <Form.Item name="completed" label="是否完成"><Select options={[{ label: '完成', value: 'true' }, { label: '未完成', value: 'false' }]} /></Form.Item>
    </div>
  );
}

function renderRiskFields() {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
      <Form.Item name="riskDescription" label="风险描述" rules={[{ required: true, message: '请输入风险描述' }]}><Input /></Form.Item>
      <Form.Item name="ownerName" label="负责人"><Input /></Form.Item>
      <Form.Item name="probabilityCode" label="概率" rules={[{ required: true, message: '请选择概率' }]}><Select options={riskFactorOptions()} /></Form.Item>
      <Form.Item name="impactCode" label="影响" rules={[{ required: true, message: '请选择影响' }]}><Select options={riskFactorOptions()} /></Form.Item>
      <Form.Item name="expectedCloseDate" label="预计关闭"><DatePicker style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="status" label="状态"><Select options={[{ label: '开启', value: 'open' }, { label: '关闭', value: 'closed' }]} /></Form.Item>
      <Form.Item name="responsePlan" label="应对计划"><Input.TextArea rows={4} maxLength={1000} showCount /></Form.Item>
      <Form.Item name="remark" label="备注"><Input /></Form.Item>
    </div>
  );
}

function riskFactorOptions() {
  return [
    { label: '低', value: 'low' },
    { label: '中', value: 'medium' },
    { label: '高', value: 'high' },
  ];
}

function editorTitle(type: EditorType) {
  return {
    member: '项目成员',
    wbs: 'WBS事项',
    schedule: '进度事项',
    risk: '风险事项',
  }[type];
}

function toEditorValues(type: EditorType, record?: unknown): EditorFormValues {
  const item = (record || {}) as Record<string, unknown>;
  const values: EditorFormValues = {};
  Object.entries(item).forEach(([key, value]) => {
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' || dayjs.isDayjs(value)) {
      values[key] = value;
    }
  });
  ['startDate', 'endDate', 'planDate', 'expectedCloseDate'].forEach((field) => {
    if (typeof values[field] === 'string') {
      values[field] = dayjs(values[field] as string);
    }
  });
  if (type === 'risk' && !values.status) {
    values.status = 'open';
  }
  return values;
}

function upsertDraft<T>(rows: T[], index: number | null, item: T, setter: (items: T[]) => void) {
  if (index === null) {
    setter([...rows, item]);
    return;
  }
  setter(rows.map((row, i) => (i === index ? item : row)));
}

function toMember(values: Record<string, unknown>): CertificationMember {
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

function toSchedule(values: Record<string, unknown>): CertificationScheduleItem {
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

function toRisk(values: Record<string, unknown>): CertificationRisk {
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

function localRiskLevel(probabilityCode: string, impactCode: string) {
  const scoreMap: Record<string, number> = { low: 1, medium: 2, high: 3 };
  const score = (scoreMap[probabilityCode] || 1) + (scoreMap[impactCode] || 1) - 1;
  const code = score >= 5 ? 'high' : score === 4 ? 'medium_high' : score === 3 ? 'medium' : score === 2 ? 'medium_low' : 'low';
  return { code, score };
}

function findDocument(documents: CertificationDocument[], type: CertificationDocumentType) {
  return documents.find((document) => document.documentType === type);
}

function isDocumentEditable(status?: string) {
  return !status || status === 'draft' || status === 'initial_rejected' || status === 'final_rejected';
}

// 任务状态的标签颜色
function renderTaskStatus(status: string, statusName?: string) {
  const labelMap: Record<string, string> = {
    pending_complete: '待补充完整',
    in_progress: '进行中',
    pending_review: '待审核',
    rejected: '已驳回',
    completed: '已完成',
  };
  const colorMap: Record<string, string> = {
    pending_complete: 'warning',
    in_progress: 'processing',
    pending_review: 'warning',
    rejected: 'error',
    completed: 'success',
  };
  const color = colorMap[status] ?? 'default';
  const label = statusName || labelMap[status] || status || '-';
  return <Tag color={color}>{label}</Tag>;
}

// 任务阶段（4 值）的标签颜色
function renderTaskPhase(phase?: string, phaseName?: string) {
  const labelMap: Record<string, string> = {
    task_creation: '任务创建阶段',
    preparation: '认定筹备阶段',
    archive: '认定归档阶段',
    certification_completed: '认定完成',
  };
  const colorMap: Record<string, string> = {
    task_creation: 'processing',
    preparation: 'gold',
    archive: 'purple',
    certification_completed: 'success',
  };
  const normalized = phase === 'project_creation' ? 'task_creation' : phase;
  const color = colorMap[normalized ?? ''] ?? 'default';
  const label = phaseName || labelMap[normalized ?? ''] || normalized || '-';
  return <Tag color={color}>{label}</Tag>;
}

function renderDocumentStatus(status?: string) {
  const nextLabelMap: Record<string, string> = {
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
  return <Tag color={nextColor}>{nextLabel}</Tag>;
}

function renderItemStatus(value?: string) {
  const labelMap: Record<string, string> = {
    pending: '待开始',
    running: '进行中',
    completed: '已完成',
    open: '开启',
    closed: '关闭',
  };
  const color = value === 'completed' || value === 'closed' ? 'success' : value === 'running' || value === 'open' ? 'processing' : 'default';
  return <Tag color={color}>{value ? labelMap[value] || value : '-'}</Tag>;
}

function renderRiskFactor(value?: string) {
  const labelMap: Record<string, string> = { low: '低', medium: '中', high: '高' };
  return labelMap[value || ''] || '-';
}

function renderRiskLevel(value?: string) {
  const labelMap: Record<string, string> = {
    low: '低',
    medium_low: '中低',
    medium: '中',
    medium_high: '中高',
    high: '高',
  };
  const color = value === 'high' ? 'error' : value === 'medium_high' ? 'warning' : value === 'medium' ? 'processing' : 'default';
  return <Tag color={color}>{labelMap[value || ''] || '-'}</Tag>;
}

function percentOf(done?: number, total?: number) {
  return total ? Math.round(((done || 0) / total) * 100) : 0;
}

function dash(value?: unknown) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function stringValue(value: unknown) {
  return typeof value === 'string' ? value : value === undefined || value === null ? undefined : String(value);
}

function numberValue(value: unknown) {
  return typeof value === 'number' ? value : value === undefined || value === null || value === '' ? undefined : Number(value);
}

function formatDate(value: unknown) {
  if (!value) {
    return undefined;
  }
  if (dayjs.isDayjs(value)) {
    return value.format('YYYY-MM-DD');
  }
  return String(value);
}

function resolveLevelCode(levelCode?: string, levelName?: string) {
  const normalizedCode = levelCode?.trim();
  if (normalizedCode && levelOptions.some((option) => option.value === normalizedCode)) {
    return normalizedCode;
  }
  const normalizedName = levelName?.trim();
  return levelOptions.find((option) => option.label === normalizedName)?.value;
}

function exactLevelRange(levelCode: string) {
  const option = levelOptions.find((item) => item.value === levelCode);
  return option ? `${option.label}->${option.label}` : undefined;
}

function levelCodesInRange(levelRange?: string) {
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

function levelRangesFromCodes(levelCodes: string[]) {
  const indexes = [...new Set(levelCodes)]
    .map((levelCode) => levelOptions.findIndex((option) => option.value === levelCode))
    .filter((index) => index >= 0)
    .sort((left, right) => left - right);
  const ranges: string[] = [];
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

function toScopeFormValues(
  task: CertificationTask,
  workTypeTree: ExamWorkType[],
): NonNullable<TaskFormValues['scopes']> {
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

  const grouped = new Map<string, {
    workTypeId?: string;
    workTypeName: string;
    levelCodes: string[];
    legacyScopes: Array<{ levelCode?: string; levelName?: string }>;
  }>();
  for (const scope of task.scopes) {
    const scopeName = scope.workTypeName ?? '';
    const workTypeId = scope.workTypeId || uniqueWorkTypeIdForTitle(workTypeTree, scopeName);
    const workTypeName = workTypeTitleForId(workTypeTree, workTypeId) ?? scopeName;
    const groupKey = workTypeId ? `id:${workTypeId}` : `name:${scopeName}`;
    const group = grouped.get(groupKey) ?? { workTypeId, workTypeName, levelCodes: [], legacyScopes: [] };
    const levelCode = resolveLevelCode(scope.levelCode, scope.levelName);
    if (levelCode) {
      group.levelCodes.push(levelCode);
    } else {
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

function toScopePayload(scopes?: TaskFormValues['scopes']) {
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

function buildTaskFormValues(
  task: CertificationTask,
  workTypeTree: ExamWorkType[],
): TaskFormValues {
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
function normalizeTaskStatus(status?: string): string | undefined {
  const allowed = new Set(['pending_complete', 'in_progress', 'pending_review', 'rejected', 'completed']);
  if (status && allowed.has(status)) {
    return status;
  }
  return 'in_progress';
}

// 任务阶段（current_phase）只允许 4 个取值，兼容历史 project_creation 编码
function normalizeTaskPhase(phase?: string): string | undefined {
  if (phase === 'project_creation') {
    return 'task_creation';
  }
  const allowed = new Set(['task_creation', 'preparation', 'archive', 'certification_completed']);
  if (phase && allowed.has(phase)) {
    return phase;
  }
  return 'task_creation';
}

function resolveTaskScopes(task: CertificationTask) {
  return task.scopes || [];
}

function scopeSummary(scopes: CertificationTask['scopes']) {
  if (!scopes?.length) {
    return '暂无范围';
  }
  const labels = scopes
    .map((scope) => [scope.workTypeName, scope.levelName].filter(Boolean).join('/'))
    .filter(Boolean);
  return labels.length > 3 ? `${labels.slice(0, 3).join('、')} 等${labels.length}项` : labels.join('、');
}

function exportTasks(tasks: CertificationTask[]) {
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

function summarizeScopeField(scopes: CertificationTask['scopes'], field: 'workTypeName' | 'levelName', separator: string) {
  return Array.from(new Set((scopes || [])
    .map((scope) => scope[field])
    .filter((value): value is string => Boolean(value))))
    .join(separator);
}
