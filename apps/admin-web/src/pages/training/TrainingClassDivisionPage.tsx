/**
 * 分班 — 自动/手动创建班级草稿 → 企业审核通过后建班
 * 选规划 → 一键自动或手动组班 → 编辑草稿 → 提交企业审核
 */
import { useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Select,
  Space,
  Statistic,
  Steps,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  DownloadOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SendOutlined,
  ThunderboltOutlined,
  UploadOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
import * as trainApi from '../../api/training';
import type {
  DemandPublicData,
  DivisionAddPersonReq,
  DivisionAutoPackClass,
  DivisionAutoPackClassMember,
  DivisionAutoPackUnassigned,
  DivisionPackPatchReq,
  DivisionPackTaskResult,
  DivisionPlanItem,
  DivisionRosterItem,
} from '../../types/training';
import { useNavStore } from '../../stores/navStore';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import './TrainingClassDivisionPage.css';

const { Text } = Typography;

const STRATEGY_LABEL: Record<string, string> = {
  by_job_level: '按工种/等级',
  by_job_level_slot: '工种等级+上下午时段',
  by_shift_team: '按班组/倒班',
  by_roster_order: '按名单顺序',
  manual: '手动组班',
};

const STATUS_COLOR: Record<string, string> = {
  draft: 'processing',
  pending: 'warning',
  rejected: 'error',
  materialized: 'success',
  superseded: 'default',
};

function clonePack(pack: DivisionPackTaskResult): DivisionPackTaskResult {
  return JSON.parse(JSON.stringify(pack)) as DivisionPackTaskResult;
}

function normalizeClasses(classes: DivisionAutoPackClass[]): DivisionAutoPackClass[] {
  return classes.map((c, idx) => ({
    ...c,
    tempId: c.tempId || `c-${idx + 1}`,
    nums: c.members?.length ?? c.nums ?? 0,
    members: (c.members ?? []).map((m) => ({ ...m })),
  }));
}

function resolveClassJobName(row: DivisionAutoPackClass): string | undefined {
  if (row.jobName?.trim()) return row.jobName.trim();
  const names = Array.from(new Set(
    (row.members ?? [])
      .map((m) => m.jobName?.trim())
      .filter((v): v is string => !!v),
  ));
  return names.length === 1 ? names[0] : undefined;
}

const CLASS_LEVEL_OPTIONS = ['初级工', '中级工', '高级工', '技师', '高级技师'].map((v) => ({
  value: v,
  label: v,
}));

function resolveClassLevelName(row: DivisionAutoPackClass): string | undefined {
  if (row.levelName?.trim()) return row.levelName.trim();
  const names = Array.from(new Set(
    (row.members ?? [])
      .map((m) => m.levelName?.trim())
      .filter((v): v is string => !!v),
  ));
  return names.length === 1 ? names[0] : undefined;
}

function buildPackPatch(pack: DivisionPackTaskResult): DivisionPackPatchReq {
  return {
    classes: pack.classes.map((c) => ({
      tempId: c.tempId,
      title: c.title,
      group: c.group,
      jobName: (c.jobName ?? '').trim() || resolveClassJobName(c) || undefined,
      levelName: (c.levelName ?? '').trim() || resolveClassLevelName(c) || undefined,
      members: c.members ?? [],
    })),
    unassigned: pack.unassigned,
  };
}

export function TrainingClassDivisionPage() {
  const queryClient = useQueryClient();
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const now = new Date();

  const [step, setStep] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [readyOnly, setReadyOnly] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [selectedPlan, setSelectedPlan] = useState<DivisionPlanItem | null>(null);
  const [trainInfoId, setTrainInfoId] = useState<number | null>(null);
  const [trainInfoTitle, setTrainInfoTitle] = useState('');
  const [titlePrefix, setTitlePrefix] = useState('');
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [classMax, setClassMax] = useState(50);
  const [strategy, setStrategy] = useState<'by_job_level' | 'by_job_level_slot' | 'by_shift_team' | 'by_roster_order'>('by_job_level');
  const [useShiftFilter, setUseShiftFilter] = useState(true);

  const [packResult, setPackResult] = useState<DivisionPackTaskResult | null>(null);
  const packResultRef = useRef<DivisionPackTaskResult | null>(null);
  packResultRef.current = packResult;
  const [draftDirty, setDraftDirty] = useState(false);
  const [editClassIdx, setEditClassIdx] = useState<number | null>(null);
  const [classMemberKeyword, setClassMemberKeyword] = useState('');
  const [resultTab, setResultTab] = useState('classes');

  const [poolOpen, setPoolOpen] = useState(false);
  const [poolSelectedIds, setPoolSelectedIds] = useState<number[]>([]);
  const [rosterPage, setRosterPage] = useState(1);
  const [rosterPageSize, setRosterPageSize] = useState(20);
  const [poolKeywordInput, setPoolKeywordInput] = useState('');
  const [poolJobNameInput, setPoolJobNameInput] = useState('');
  const [poolKeyword, setPoolKeyword] = useState('');
  const [poolJobName, setPoolJobName] = useState('');
  const [addPersonOpen, setAddPersonOpen] = useState(false);
  const [addPersonForm] = Form.useForm<DivisionAddPersonReq>();
  const [newClassTitle, setNewClassTitle] = useState('');

  const plansQuery = useQuery({
    queryKey: ['division-plans', page, pageSize, keyword, readyOnly],
    queryFn: () => trainApi.pageDivisionPlans({
      page,
      pageSize,
      keyword: keyword || undefined,
      readyOnly,
    }),
  });

  const readyQuery = useQuery({
    queryKey: ['division-ready', selectedPlan?.planId],
    queryFn: () => trainApi.getDivisionReady(selectedPlan!.planId),
    enabled: !!selectedPlan?.planId,
  });

  const workTypePublicQuery = useQuery({
    queryKey: ['division-worktype-public'],
    queryFn: () => trainApi.getDemandPublicData(['worktype']),
    staleTime: 5 * 60 * 1000,
  });

  const savedPackTasksQuery = useQuery({
    queryKey: ['division-pack-tasks', 'plan-entry'],
    queryFn: async () => {
      const pageSize = 500;
      const first = await trainApi.pageDivisionPackTasks({
        page: 1,
        pageSize,
      });
      const totalPages = Math.ceil((first.total ?? 0) / pageSize);
      if (totalPages <= 1) return first;

      const rest = [];
      for (let p = 2; p <= totalPages; p++) {
        rest.push(trainApi.pageDivisionPackTasks({
          page: p,
          pageSize,
        }));
      }
      const pages = await Promise.all(rest);
      return {
        ...first,
        page: 1,
        pageSize: first.total,
        items: [
          ...first.items,
          ...pages.flatMap((item) => item.items),
        ],
      };
    },
    enabled: step === 0,
  });

  const rosterQuery = useQuery({
    queryKey: [
      'division-roster',
      selectedPlan?.planId,
      trainInfoId,
      poolOpen,
      poolKeyword,
      poolJobName,
    ],
    queryFn: async () => {
      const pageSize = 500;
      const baseParams = {
        planId: selectedPlan!.planId,
        trainInfoId: trainInfoId ?? undefined,
        assignStatus: 0,
        keyword: poolKeyword || undefined,
        jobName: poolJobName || undefined,
      };
      const first = await trainApi.pageDivisionRoster({
        ...baseParams,
        page: 1,
        pageSize,
      });
      const totalPages = Math.ceil((first.total ?? 0) / pageSize);
      if (totalPages <= 1) return first;

      const rest = [];
      for (let p = 2; p <= totalPages; p++) {
        rest.push(trainApi.pageDivisionRoster({
          ...baseParams,
          page: p,
          pageSize,
        }));
      }
      const pages = await Promise.all(rest);
      return {
        ...first,
        page: 1,
        pageSize: first.total,
        items: [
          ...first.items,
          ...pages.flatMap((item) => item.items),
        ],
      };
    },
    enabled: !!selectedPlan?.planId && (poolOpen || step === 2),
  });

  const materialsOk = !!(
    selectedPlan
    && (selectedPlan.rosterFileId || readyQuery.data?.rosterFileId)
    && (selectedPlan.shiftFileId || readyQuery.data?.shiftFileId)
  );
  const missing = readyQuery.data?.missing ?? [];
  const rosterFileName = selectedPlan?.rosterFileName || readyQuery.data?.rosterFileName;
  const shiftFileName = selectedPlan?.shiftFileName || readyQuery.data?.shiftFileName;

  const editable = !!packResult && (packResult.status === 'draft' || packResult.status === 'rejected');
  const editingClass = editClassIdx != null && packResult ? packResult.classes[editClassIdx] : null;
  const classMemberItems = useMemo(() => {
    const rows = editingClass?.members ?? [];
    const kw = classMemberKeyword.trim().toLowerCase();
    if (!kw) return rows;
    return rows.filter((row) => (row.personName ?? '').toLowerCase().includes(kw)
      || (row.idCard ?? '').toLowerCase().includes(kw));
  }, [editingClass?.members, classMemberKeyword]);
  const savedPackTaskByPlanId = useMemo(() => {
    const priority: Record<string, number> = {
      draft: 0,
      rejected: 1,
      pending: 2,
      materialized: 3,
    };
    const map = new Map<number, DivisionPackTaskResult>();
    for (const task of savedPackTasksQuery.data?.items ?? []) {
      if (!task.planId || task.status === 'superseded') continue;
      const current = map.get(task.planId);
      const currentPriority = current ? priority[current.status] ?? 99 : 99;
      const taskPriority = priority[task.status] ?? 99;
      const isNewerSameStatus = current
        && taskPriority === currentPriority
        && (task.updateTime ?? task.createTime ?? '') > (current.updateTime ?? current.createTime ?? '');
      if (!current || taskPriority < currentPriority || isNewerSameStatus) {
        map.set(task.planId, task);
      }
    }
    return map;
  }, [savedPackTasksQuery.data?.items]);

  const enterPlan = (row: DivisionPlanItem) => {
    if (!row.rosterFileId || !row.shiftFileId) {
      message.warning('需同时具备筛选名单与排班表');
      return;
    }
    setSelectedPlan(row);
    setTrainInfoId(null);
    setTrainInfoTitle('');
    setTitlePrefix(row.orgName || row.title);
    setPackResult(null);
    setDraftDirty(false);
    setPoolSelectedIds([]);
    setResultTab('classes');
    setStep(1);
  };

  const applyLocalPack = (next: DivisionPackTaskResult) => {
    const normalized = {
      ...next,
      classes: normalizeClasses(next.classes ?? []),
      unassigned: next.unassigned ?? [],
      classCount: next.classes?.length ?? 0,
      assignedCount: (next.classes ?? []).reduce((s, c) => s + (c.members?.length ?? c.nums ?? 0), 0),
      unassignedCount: next.unassigned?.length ?? 0,
    };
    setPackResult(normalized);
  };

  const openSavedPackTask = async (taskId: number) => {
    try {
      const detail = await trainApi.getDivisionPackTask(taskId);
      const plan = (plansQuery.data?.items ?? []).find((item) => item.planId === detail.planId);
      setSelectedPlan(plan ?? {
        planId: detail.planId,
        planNo: `#${detail.planId}`,
        title: `分班方案 #${detail.taskId}`,
        targetOrgId: detail.companyId ?? 0,
        status: 4,
        statusLabel: '企业已确认',
      });
      setTrainInfoId(detail.trainInfoId);
      setTrainInfoTitle('');
      setTitlePrefix(detail.classNamePrefix ?? '');
      setClassMax(detail.classMax ?? 50);
      if (detail.year) setYear(detail.year);
      if (detail.month) setMonth(detail.month);
      if (['by_job_level', 'by_job_level_slot', 'by_shift_team', 'by_roster_order'].includes(detail.strategy)) {
        setStrategy(detail.strategy as 'by_job_level' | 'by_job_level_slot' | 'by_shift_team' | 'by_roster_order');
      }
      applyLocalPack(detail);
      setDraftDirty(false);
      setEditClassIdx(null);
      setResultTab('classes');
      setStep(2);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '打开分班方案失败');
    }
  };

  const ensureTrainInfo = async (): Promise<number> => {
    if (trainInfoId) return trainInfoId;
    const res = await trainApi.ensureDivisionTrainInfo(selectedPlan!.planId, {
      title: titlePrefix || undefined,
      reuseExisting: true,
    });
    setTrainInfoId(res.trainInfoId);
    setTrainInfoTitle(res.title);
    queryClient.invalidateQueries({ queryKey: ['division-plans'] });
    return res.trainInfoId;
  };

  const packMu = useMutation({
    mutationFn: async () => {
      const tid = await ensureTrainInfo();
      return trainApi.autoPackDivision({
        planId: selectedPlan!.planId,
        trainInfoId: tid,
        strategy,
        classMax,
        classNamePrefix: titlePrefix ? `${titlePrefix}-` : undefined,
        submitForCompanyConfirm: false,
        useShiftFilter,
        excludeNoIdCard: false,
        year,
        month,
        autoParseRoster: true,
      });
    },
    onSuccess: (res) => {
      applyLocalPack(res);
      setTrainInfoId(res.trainInfoId);
      setDraftDirty(false);
      setStep(2);
      setResultTab('classes');
      message.success(`已生成分班草稿：${res.classCount} 个班 / ${res.assignedCount} 人，请核对后提交企业审核`);
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
      queryClient.invalidateQueries({ queryKey: ['division-plans'] });
      queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
    },
    onError: (e: Error) => message.error(e.message || '自动分班失败'),
  });

  const manualPackMu = useMutation({
    mutationFn: async () => {
      const tid = await ensureTrainInfo();
      return trainApi.manualPackDivision({
        planId: selectedPlan!.planId,
        trainInfoId: tid,
        classMax,
        classNamePrefix: titlePrefix ? `${titlePrefix}-` : undefined,
        classes: [{ title: `${titlePrefix || '培训'}-1班`, rosterIds: [] }],
      });
    },
    onSuccess: (res) => {
      applyLocalPack(res);
      setTrainInfoId(res.trainInfoId);
      setDraftDirty(false);
      setStep(2);
      setResultTab('classes');
      message.success('已创建手动分班草稿，请添加班级与学员后提交企业审核');
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
      queryClient.invalidateQueries({ queryKey: ['division-plans'] });
      queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
    },
    onError: (e: Error) => message.error(e.message || '创建手动草稿失败'),
  });

  const saveMu = useMutation({
    mutationFn: () => {
      const pack = packResultRef.current;
      if (!pack) throw new Error('无草稿');
      return trainApi.patchDivisionPackTask(pack.taskId, buildPackPatch(pack));
    },
    onSuccess: (res) => {
      applyLocalPack(res);
      setDraftDirty(false);
      message.success('草稿已保存');
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const submitMu = useMutation({
    mutationFn: async () => {
      const pack = packResultRef.current;
      if (!pack) throw new Error('无草稿');
      if (draftDirty) {
        await trainApi.patchDivisionPackTask(pack.taskId, buildPackPatch(pack));
      }
      return trainApi.submitDivisionPackTask(pack.taskId);
    },
    onSuccess: (res) => {
      applyLocalPack(res);
      setDraftDirty(false);
      message.success('已提交企业审核，通过后将自动创建培训班级');
      queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
      queryClient.invalidateQueries({ queryKey: ['enterprise-pack-confirm'] });
    },
    onError: (e: Error) => message.error(e.message || '提交失败'),
  });

  const syncPlatformUsersMu = useMutation({
    mutationFn: () => {
      const pack = packResultRef.current;
      if (!pack) throw new Error('无方案');
      return trainApi.syncDivisionPackPlatformUsers(pack.taskId);
    },
    onSuccess: (res) => {
      const sync = res.platformUserSync;
      message.success(
        `补开完成：新建 ${sync.created}，复用 ${sync.reused}，更新 ${sync.updated}，跳过 ${sync.skipped}`
        + (sync.created > 0 ? '；新建默认密码 123123' : '')
        + (sync.warnings?.length ? `；告警 ${sync.warnings.length} 条` : ''),
      );
      if (sync.warnings?.length) {
        Modal.warning({
          title: '部分学员未开账号',
          content: (
            <div style={{ maxHeight: 280, overflow: 'auto' }}>
              {sync.warnings.map((w, i) => (
                <div key={`${w.idCard}-${i}`}>{w.name || '-'}：{w.reason}</div>
              ))}
            </div>
          ),
        });
      }
    },
    onError: (e: Error) => message.error(e.message || '补开失败'),
  });

  const addPersonMu = useMutation({
    mutationFn: async (payload: DivisionAddPersonReq) => {
      const pack = packResultRef.current;
      if (!pack || editClassIdx == null) throw new Error('未选择班级');
      const tempId = pack.classes[editClassIdx]?.tempId;
      if (!tempId) throw new Error('班级 tempId 缺失，请先保存草稿');
      if (draftDirty) {
        await trainApi.patchDivisionPackTask(pack.taskId, buildPackPatch(pack));
      }
      return trainApi.addPersonToDivisionPackClass(pack.taskId, tempId, payload);
    },
    onSuccess: (res) => {
      applyLocalPack(res);
      setDraftDirty(false);
      setAddPersonOpen(false);
      addPersonForm.resetFields();
      message.success(res.createdRoster ? '已入库并加入班级' : '已加入班级');
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
    },
    onError: (e: Error) => message.error(e.message || '加人失败'),
  });

  const importMembersMu = useMutation({
    mutationFn: async (file: File) => {
      const pack = packResultRef.current;
      if (!pack || editClassIdx == null) throw new Error('未选择班级');
      const tempId = pack.classes[editClassIdx]?.tempId;
      if (!tempId) throw new Error('班级 tempId 缺失，请先保存草稿');
      if (draftDirty) {
        await trainApi.patchDivisionPackTask(pack.taskId, buildPackPatch(pack));
      }
      return trainApi.importMembersToDivisionPackClass(pack.taskId, tempId, file);
    },
    onSuccess: (res) => {
      applyLocalPack(res.task);
      setDraftDirty(false);
      const skipN = res.skipped?.length ?? 0;
      if ((res.added ?? 0) === 0 && skipN > 0) {
        const sample = res.skipped?.slice(0, 3).map((s) => `${s.personName || ''}：${s.reason || ''}`).join('；');
        message.warning(`未导入成功（跳过 ${skipN} 人）。${sample || ''}`);
      } else {
        message.success(`导入成功 ${res.added} 人${skipN ? `，跳过 ${skipN} 人` : ''}`);
      }
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
    },
    onError: (e: Error) => message.error(e.message || '导入失败'),
  });

  const updateDraft = (mutator: (draft: DivisionPackTaskResult) => void) => {
    setPackResult((prev) => {
      if (!prev || (prev.status !== 'draft' && prev.status !== 'rejected')) return prev;
      const next = clonePack(prev);
      mutator(next);
      next.classes = normalizeClasses(next.classes);
      next.classCount = next.classes.length;
      next.assignedCount = next.classes.reduce((s, c) => s + (c.members?.length ?? 0), 0);
      next.unassignedCount = next.unassigned.length;
      packResultRef.current = next;
      return next;
    });
    setDraftDirty(true);
  };

  const moveMemberToUnassigned = (classIdx: number, rosterId: number) => {
    updateDraft((draft) => {
      const cls = draft.classes[classIdx];
      if (!cls?.members) return;
      const member = cls.members.find((m) => m.rosterId === rosterId);
      if (!member) return;
      cls.members = cls.members.filter((m) => m.rosterId !== rosterId);
        draft.unassigned = [
        ...draft.unassigned.filter((u) => u.rosterId !== rosterId),
        {
          rosterId: member.rosterId,
          personName: member.personName,
          idCard: member.idCard,
          workshop: member.workshop,
          jobName: member.jobName,
          levelName: member.levelName,
          shiftTeam: member.shiftTeam,
          reason: '机构调整：移出班级',
        },
      ];
    });
  };

  const moveUnassignedToClass = (rosterId: number, targetClassIdx: number) => {
    updateDraft((draft) => {
      const person = draft.unassigned.find((u) => u.rosterId === rosterId);
      if (!person || person.rosterId == null) return;
      const cls = draft.classes[targetClassIdx];
      if (!cls) return;
      if ((cls.members?.length ?? 0) >= draft.classMax) {
        message.warning(`目标班已达上限 ${draft.classMax}`);
        return;
      }
      cls.members = [
        ...(cls.members ?? []),
        {
          rosterId: person.rosterId,
          personName: person.personName,
          idCard: person.idCard,
          workshop: person.workshop,
          jobName: person.jobName,
          levelName: person.levelName,
          shiftTeam: person.shiftTeam,
        },
      ];
      draft.unassigned = draft.unassigned.filter((u) => u.rosterId !== rosterId);
    });
  };

  const moveMemberBetweenClasses = (fromIdx: number, rosterId: number, toIdx: number) => {
    if (fromIdx === toIdx) return;
    updateDraft((draft) => {
      const from = draft.classes[fromIdx];
      const to = draft.classes[toIdx];
      if (!from?.members || !to) return;
      const member = from.members.find((m) => m.rosterId === rosterId);
      if (!member) return;
      if ((to.members?.length ?? 0) >= draft.classMax) {
        message.warning(`目标班已达上限 ${draft.classMax}`);
        return;
      }
      from.members = from.members.filter((m) => m.rosterId !== rosterId);
      to.members = [...(to.members ?? []), member];
    });
  };

  const pendingTotal = selectedPlan?.pendingRosterCount;

  const planColumns: ColumnsType<DivisionPlanItem> = [
    { title: '规划编号', dataIndex: 'planNo', width: 150 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '企业', dataIndex: 'orgName', width: 140, ellipsis: true },
    {
      title: '材料',
      width: 150,
      render: (_, row) => (
        <Space size={4} wrap>
          <Tag color={row.rosterFileId ? 'success' : 'default'}>名单</Tag>
          <Tag color={row.shiftFileId ? 'success' : 'default'}>排班</Tag>
        </Space>
      ),
    },
    {
      title: '待分',
      dataIndex: 'pendingRosterCount',
      width: 72,
      align: 'right',
      render: (v?: number) => (v != null && v > 0 ? <Text strong>{v}</Text> : (v ?? '-')),
    },
    {
      title: '已建培训',
      dataIndex: 'linkedTrainInfoCount',
      width: 88,
      align: 'right',
      render: (v?: number) => v ?? 0,
    },
    {
      title: '操作',
      width: 210,
      fixed: 'right',
      render: (_, row) => {
        const ok = !!(row.rosterFileId && row.shiftFileId);
        const savedTask = savedPackTaskByPlanId.get(row.planId);
        const canEditSaved = savedTask?.status === 'draft' || savedTask?.status === 'rejected';
        return (
          <Space size={4} wrap>
            <Button type="link" size="small" disabled={!ok} onClick={(e) => { e.stopPropagation(); enterPlan(row); }}>
              开始分班
            </Button>
            {savedTask && (
              <>
                <Tag color={STATUS_COLOR[savedTask.status] || 'default'} style={{ marginInlineEnd: 0 }}>
                  {savedTask.statusLabel || savedTask.status}
                </Tag>
                <Button type="link" size="small" onClick={(e) => { e.stopPropagation(); openSavedPackTask(savedTask.taskId); }}>
                  {canEditSaved ? '继续编辑' : '分班草稿'}
                </Button>
              </>
            )}
          </Space>
        );
      },
    },
  ];

  const rosterColumns: ColumnsType<DivisionRosterItem> = [
    { title: '姓名', dataIndex: 'personName', width: 100 },
    { title: '身份证', dataIndex: 'idCard', width: 180 },
    { title: '班组', dataIndex: 'shiftTeam', width: 100, render: (v) => v || '-' },
    { title: '工种', dataIndex: 'jobName', width: 120, render: (v) => v || '-' },
  ];

  const classOptions = useMemo(
    () => (packResult?.classes ?? []).map((c, idx) => ({
      value: idx,
      label: `${c.title}（${c.members?.length ?? c.nums}人）`,
    })),
    [packResult],
  );

  const poolRosterItems = useMemo(() => {
    const rows = rosterQuery.data?.items ?? [];
    const kw = poolKeywordInput.trim().toLowerCase();
    const job = poolJobNameInput.trim().toLowerCase();
    if (!kw && !job) return rows;
    return rows.filter((row) => {
      const keywordMatched = !kw
        || (row.personName ?? '').toLowerCase().includes(kw)
        || (row.idCard ?? '').toLowerCase().includes(kw);
      const jobMatched = !job || (row.jobName ?? '').toLowerCase().includes(job);
      return keywordMatched && jobMatched;
    });
  }, [rosterQuery.data?.items, poolKeywordInput, poolJobNameInput]);

  const poolJobOptions = useMemo(() => {
    const names = new Set<string>();
    for (const row of rosterQuery.data?.items ?? []) {
      const name = row.jobName?.trim();
      if (name) names.add(name);
    }
    return Array.from(names)
      .sort((a, b) => a.localeCompare(b, 'zh-CN'))
      .map((name) => ({ label: name, value: name }));
  }, [rosterQuery.data?.items]);

  const classJobOptions = useMemo(() => {
    const names = new Set<string>();
    const groups = (workTypePublicQuery.data as DemandPublicData | undefined)?.worktype_list ?? [];
    for (const g of groups) {
      for (const o of g.options ?? []) {
        const title = o.title?.trim();
        if (title) names.add(title);
      }
    }
    for (const opt of poolJobOptions) names.add(opt.value);
    for (const cls of packResult?.classes ?? []) {
      const own = cls.jobName?.trim();
      if (own) names.add(own);
      for (const m of cls.members ?? []) {
        const name = m.jobName?.trim();
        if (name) names.add(name);
      }
    }
    for (const u of packResult?.unassigned ?? []) {
      const name = u.jobName?.trim();
      if (name) names.add(name);
    }
    return Array.from(names)
      .sort((a, b) => a.localeCompare(b, 'zh-CN'))
      .map((name) => ({ label: name, value: name }));
  }, [workTypePublicQuery.data, poolJobOptions, packResult]);

  const applyPoolFilters = () => {
    setRosterPage(1);
    setPoolSelectedIds([]);
    setPoolKeyword(poolKeywordInput.trim());
    setPoolJobName(poolJobNameInput.trim());
  };

  const resetPoolFilters = () => {
    setRosterPage(1);
    setRosterPageSize(20);
    setPoolSelectedIds([]);
    setPoolKeywordInput('');
    setPoolJobNameInput('');
    setPoolKeyword('');
    setPoolJobName('');
  };

  const confirmPack = () => {
    if (!materialsOk) {
      message.warning(`材料未齐：${missing.join('、') || '筛选名单/排班表'}`);
      return;
    }
    Modal.confirm({
      title: '生成分班草稿？',
      width: 480,
      content: (
        <div style={{ marginTop: 8 }}>
          <p style={{ marginBottom: 8 }}>
            策略「{STRATEGY_LABEL[strategy]}」：
            {strategy === 'by_job_level'
              ? '按名单工种与等级分班；排班表仅过滤当月难以上课人员。缺工种/等级进未分入。'
              : strategy === 'by_job_level_slot'
                ? '按工种/等级分组后，结合排班表按每天上午/下午可上课情况拆成多个时段班（如 电工-高级-上午1班）。'
                : '按选定规则切班。'}
            每班不超过 {classMax} 人，生成可编辑草稿，不会立即建班。
          </p>
        </div>
      ),
      okText: '生成草稿',
      onOk: () => packMu.mutateAsync(),
    });
  };

  const confirmManualPack = () => {
    if (!materialsOk) {
      message.warning(`材料未齐：${missing.join('、') || '筛选名单/排班表'}`);
      return;
    }
    Modal.confirm({
      title: '创建手动分班草稿？',
      content: '将创建可编辑的班级草稿（不立即建班）。可多班，支持从待分池勾选、上传名单、手填加人；提交企业审核通过后才创建正式班级。新建草稿会作废同培训信息下未完成的旧草稿。',
      okText: '创建草稿',
      onOk: () => manualPackMu.mutateAsync(),
    });
  };

  const addEmptyClass = () => {
    const title = newClassTitle.trim() || `${titlePrefix || '培训'}-${(packResult?.classes.length ?? 0) + 1}班`;
    updateDraft((draft) => {
      draft.classes.push({
        tempId: `c-${Math.random().toString(36).slice(2, 10)}`,
        title,
        group: '手动',
        nums: 0,
        members: [],
      });
    });
    setNewClassTitle('');
    message.success(`已添加班级「${title}」，请保存草稿`);
  };

  const addFromPoolToEditingClass = () => {
    if (editClassIdx == null || !packResult) return;
    // 手动组班允许同一人进多个班：仅排除「当前编辑班」已有人员
    const occupiedInCurrent = new Set(
      (packResult.classes[editClassIdx]?.members ?? []).map((m) => m.rosterId),
    );
    const candidates = poolRosterItems.filter(
      (r) => poolSelectedIds.includes(r.id) && !occupiedInCurrent.has(r.id),
    );
    if (candidates.length === 0) {
      message.warning('请选择尚未在本班的待分人员（可同时属于其他班）');
      return;
    }
    const cls = packResult.classes[editClassIdx];
    const room = (packResult.classMax || 50) - (cls.members?.length ?? 0);
    if (candidates.length > room) {
      message.warning(`本班仅剩 ${room} 个名额`);
      return;
    }
    updateDraft((draft) => {
      const target = draft.classes[editClassIdx];
      if (!target) return;
      target.members = [
        ...(target.members ?? []),
        ...candidates.map((r) => ({
          rosterId: r.id,
          personName: r.personName,
          idCard: r.idCard,
          workshop: r.workshop,
          jobName: r.jobName,
          levelName: r.levelName,
          shiftTeam: r.shiftTeam,
          phone: r.phone,
        })),
      ];
    });
    setPoolOpen(false);
    setPoolSelectedIds([]);
    message.success(`已加入 ${candidates.length} 人，请保存草稿`);
  };

  const isManualPack = packResult?.strategy === 'manual';

  const confirmSubmit = () => {
    if (!packResult) return;
    Modal.confirm({
      title: '提交企业审核？',
      content: `将提交 ${packResult.classCount} 个班（${packResult.assignedCount} 人）给企业审核。审核通过后系统才会创建培训班级。`,
      okText: '提交审核',
      onOk: () => submitMu.mutateAsync(),
    });
  };

  return (
    <div className="training-class-division-page training-page">
      <TrainingProcessSteps current={2} />

      <section className="page-heading">
        <div>
          <Typography.Title level={4} style={{ marginBottom: 4 }}>分班</Typography.Title>
          <Text type="secondary">
            根据人员名单与排班表自动划分培训班（默认按工种×等级切班，每班≤{classMax}）
          </Text>
        </div>
      </section>

      <Card bordered={false} className="division-steps-card">
        <Steps
          current={step}
          size="small"
          items={[
            { title: '选择规划' },
            { title: '配置并分班' },
            { title: '编辑并提交' },
          ]}
          onChange={(s) => {
            if (s === 0) { setStep(0); return; }
            if (!selectedPlan) { message.warning('请先选择规划'); return; }
            if (s >= 1 && !materialsOk) {
              message.warning('需同时具备筛选名单与排班表');
              return;
            }
            if (s >= 2 && !packResult) {
              message.warning('请先生成分班草稿（自动或手动）');
              return;
            }
            setStep(s);
          }}
        />
      </Card>

      {step === 0 && (
        <Card bordered={false}>
          <div className="division-toolbar">
            <Input.Search
              allowClear
              placeholder="搜索规划标题 / 编号 / 企业"
              style={{ width: 280 }}
              onSearch={(v) => { setKeyword(v); setPage(1); }}
            />
            <Checkbox
              checked={readyOnly}
              onChange={(e) => { setReadyOnly(e.target.checked); setPage(1); }}
            >
              仅材料已齐
            </Checkbox>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                plansQuery.refetch();
                savedPackTasksQuery.refetch();
              }}
            >
              刷新
            </Button>
          </div>
          <Alert
            className="division-plan-hint"
            type="info"
            showIcon
                    message="一键分班会按企业最新上传的筛选名单重新解析人员；生成草稿后请再提交企业审核。"
          />
          <Table<DivisionPlanItem>
            rowKey="planId"
            loading={plansQuery.isLoading}
            columns={planColumns}
            dataSource={plansQuery.data?.items ?? []}
            scroll={{ x: 900 }}
            onRow={(row) => ({
              className: row.rosterFileId && row.shiftFileId ? 'division-row-ready' : undefined,
              style: { cursor: row.rosterFileId && row.shiftFileId ? 'pointer' : 'default' },
              onDoubleClick: () => enterPlan(row),
            })}
            locale={{ emptyText: readyOnly ? '暂无材料已齐的规划，可取消「仅材料已齐」查看全部' : '暂无规划' }}
            pagination={{
              current: page,
              pageSize,
              total: plansQuery.data?.total ?? 0,
              showSizeChanger: true,
              showTotal: (t) => `共 ${t} 条`,
              onChange: (p, ps) => { setPage(p); setPageSize(ps); },
            }}
          />
        </Card>
      )}

      {step === 1 && selectedPlan && (
        <Card bordered={false}>
          <div className="division-context">
            <div className="division-context-main">
              <p className="division-context-title">
                {selectedPlan.planNo} · {selectedPlan.title}
              </p>
              <div className="division-context-meta">
                企业 {selectedPlan.orgName || '-'}
                {pendingTotal != null ? ` · 待分池 ${pendingTotal} 人` : ''}
                {trainInfoId ? ` · 培训信息 #${trainInfoId}` : ''}
              </div>
              <Space size={4} style={{ marginTop: 6 }} wrap>
                <Tag color={selectedPlan.rosterFileId || readyQuery.data?.rosterFileId ? 'success' : 'error'}>
                  名单{rosterFileName ? ` · ${rosterFileName}` : ''}
                </Tag>
                <Tag color={selectedPlan.shiftFileId || readyQuery.data?.shiftFileId ? 'success' : 'error'}>
                  排班{shiftFileName ? ` · ${shiftFileName}` : ''}
                </Tag>
              </Space>
            </div>
            <Space wrap>
              <Button
                size="small"
                icon={<DownloadOutlined />}
                disabled={!selectedPlan.rosterFileId}
                onClick={() => trainApi.downloadThreeYearPlanFile(selectedPlan.planId, 'enterprise-roster')}
              >
                下载名单
              </Button>
              <Button
                size="small"
                icon={<DownloadOutlined />}
                disabled={!selectedPlan.shiftFileId}
                onClick={() => trainApi.downloadThreeYearPlanFile(selectedPlan.planId, 'enterprise-shift')}
              >
                下载排班
              </Button>
              <Button size="small" onClick={() => setStep(0)}>重选规划</Button>
            </Space>
          </div>

          {!materialsOk && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={`材料未齐：${missing.join('、') || '筛选名单/排班表'}，请先在企业审核中回传。`}
            />
          )}

          <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="班级名前缀">
              <Input
                value={titlePrefix}
                onChange={(e) => setTitlePrefix(e.target.value)}
                placeholder="如：来铝"
                style={{ maxWidth: 220 }}
              />
            </Descriptions.Item>
            <Descriptions.Item label="培训信息">
              {trainInfoId
                ? <Text type="success">#{trainInfoId} {trainInfoTitle || ''}（分班时自动复用）</Text>
                : <Text type="secondary">点击分班时自动生成/复用</Text>}
            </Descriptions.Item>
            <Descriptions.Item label="说明">
              <Text type="secondary">先出草稿，企业通过后再建班</Text>
            </Descriptions.Item>
          </Descriptions>

          <Form layout="vertical">
            <div className="division-config-grid">
              <Form.Item label="培训/排班年" required>
                <InputNumber style={{ width: '100%' }} min={2020} max={2100} value={year} onChange={(v) => setYear(v || now.getFullYear())} />
              </Form.Item>
              <Form.Item label="排班月" required>
                <InputNumber style={{ width: '100%' }} min={1} max={12} value={month} onChange={(v) => setMonth(v || 1)} />
              </Form.Item>
              <Form.Item label="每班上限" required>
                <InputNumber style={{ width: '100%' }} min={1} max={200} value={classMax} onChange={(v) => setClassMax(v || 50)} />
              </Form.Item>
              <Form.Item label="分班策略">
                <Select
                  value={strategy}
                  onChange={setStrategy}
                  options={[
                    { value: 'by_job_level', label: '按工种/等级切班（推荐）' },
                    { value: 'by_shift_team', label: '按班组/倒班切班' },
                    { value: 'by_job_level_slot', label: '工种/等级 + 上下午时段（高级）' },
                    { value: 'by_roster_order', label: '按名单顺序切班' },
                  ]}
                />
              </Form.Item>
            </div>
            <Checkbox checked={useShiftFilter} onChange={(e) => setUseShiftFilter(e.target.checked)}>
              过滤当月无可上课日人员（全夜/全休等）
            </Checkbox>
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                默认按工种×等级分班；排班只做过滤。名单带年份时，所选年无人将直接报错。
              </Text>
            </div>
          </Form>

          <div className="division-actions">
            <Button
              type="primary"
              size="large"
              icon={<ThunderboltOutlined />}
              loading={packMu.isPending}
              disabled={!materialsOk}
              onClick={confirmPack}
            >
              一键自动分班
            </Button>
            <Button
              size="large"
              icon={<UserAddOutlined />}
              loading={manualPackMu.isPending}
              disabled={!materialsOk}
              onClick={confirmManualPack}
            >
              手动创建班级
            </Button>
            <Text type="secondary">自动/手动均生成草稿，企业通过后才建班；新建草稿会作废同培训下旧草稿</Text>
          </div>
        </Card>
      )}

      {step === 2 && selectedPlan && packResult && (
        <Card bordered={false}>
          <div className="division-context" style={{ marginBottom: 16 }}>
            <div className="division-context-main">
              <p className="division-context-title">
                {selectedPlan.planNo} · {selectedPlan.title}
              </p>
              <div className="division-context-meta">
                方案 #{packResult.taskId}
                {' · '}策略 {STRATEGY_LABEL[packResult.strategy] || packResult.strategy}
                {' · '}每班上限 {packResult.classMax}
                {trainInfoId ? ` · 培训信息 #${trainInfoId}` : ''}
              </div>
            </div>
            <Space wrap>
              <Tag color={STATUS_COLOR[packResult.status] || 'default'}>
                {packResult.statusLabel || packResult.status}
              </Tag>
              {draftDirty && <Tag color="orange">有未保存修改</Tag>}
            </Space>
          </div>

          {packResult.status === 'rejected' && packResult.rejectReason && (
            <Alert
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              message="企业已驳回"
              description={packResult.rejectReason}
            />
          )}
          {packResult.status === 'pending' && (
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
              message="已提交企业审核，通过后将自动创建培训班级。审核前不可再编辑。"
            />
          )}
          {packResult.status === 'materialized' && (
            <Alert
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
              message="企业已通过，培训班级已创建"
              description={packResult.materialized
                ? `已建 ${packResult.materialized.classCount} 班 / ${packResult.materialized.peopleCount} 人。若学员无法登录 App，可点击「补开 App 账号」。`
                : '若学员无法登录 App，可点击「补开 App 账号」。'}
            />
          )}
          {editable && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message="当前为草稿：可改班级名称、调整学员归属；保存后提交企业审核，通过后才真正建班。"
            />
          )}

          <Card size="small" className="division-result-stats" bordered={false} style={{ background: '#f8fafc' }}>
            <Space size={48} wrap>
              <Statistic title="班级数" value={packResult.classCount} suffix="个" />
              <Statistic title="已分配" value={packResult.assignedCount} suffix="人" />
              <Statistic
                title="未分入"
                value={packResult.unassignedCount}
                suffix="人"
                valueStyle={packResult.unassignedCount > 0 ? { color: '#d46b08' } : undefined}
              />
            </Space>
          </Card>

          {editable && isManualPack && (
            <Space style={{ marginBottom: 12, marginTop: 12 }} wrap>
              <Input
                style={{ width: 220 }}
                placeholder="新班级名称"
                value={newClassTitle}
                onChange={(e) => setNewClassTitle(e.target.value)}
              />
              <Button icon={<PlusOutlined />} onClick={addEmptyClass}>新建班级</Button>
              <Text type="secondary">手动组班：同一人可进入多个班；编辑学员支持待分池勾选、上传名单、手填加人</Text>
            </Space>
          )}

          <Tabs
            activeKey={resultTab}
            onChange={setResultTab}
            items={[
              {
                key: 'classes',
                label: `班级草稿 (${packResult.classes?.length ?? 0})`,
                children: (
                  <Table
                    rowKey={(r) => r.tempId || r.title}
                    size="small"
                    pagination={false}
                    dataSource={packResult.classes}
                    columns={[
                      {
                        title: '班级名称',
                        dataIndex: 'title',
                        render: (title: string, _row, idx) => (
                          editable ? (
                            <Input
                              value={title}
                              onChange={(e) => {
                                updateDraft((d) => {
                                  if (d.classes[idx]) d.classes[idx].title = e.target.value;
                                });
                              }}
                            />
                          ) : title
                        ),
                      },
                      { title: '分组', dataIndex: 'group', width: 120 },
                      {
                        title: '工种',
                        dataIndex: 'jobName',
                        width: 160,
                        render: (_v, row, idx) => {
                          if (!editable) {
                            const value = resolveClassJobName(row);
                            return value ? <Tag>{value}</Tag> : <Text type="secondary">-</Text>;
                          }
                          return (
                            <Select
                              mode="tags"
                              maxCount={1}
                              allowClear
                              showSearch
                              placeholder="选择或输入工种"
                              style={{ width: '100%' }}
                              value={row.jobName?.trim() ? [row.jobName.trim()] : []}
                              options={classJobOptions}
                              optionFilterProp="label"
                              tokenSeparators={[]}
                              onChange={(vals) => {
                                const next = (vals?.[0] ?? '').trim();
                                updateDraft((d) => {
                                  if (d.classes[idx]) d.classes[idx].jobName = next || undefined;
                                });
                              }}
                            />
                          );
                        },
                      },
                      {
                        title: '等级',
                        dataIndex: 'levelName',
                        width: 130,
                        render: (_v, row, idx) => {
                          const current = resolveClassLevelName(row);
                          if (!editable) {
                            return current ? <Tag>{current}</Tag> : <Text type="secondary">-</Text>;
                          }
                          return (
                            <Select
                              mode="tags"
                              maxCount={1}
                              allowClear
                              showSearch
                              placeholder="选择或输入等级"
                              style={{ width: '100%' }}
                              value={current ? [current] : []}
                              options={CLASS_LEVEL_OPTIONS}
                              optionFilterProp="label"
                              tokenSeparators={[]}
                              onChange={(vals) => {
                                const next = (vals?.[0] ?? '').trim();
                                updateDraft((d) => {
                                  if (!d.classes[idx]) return;
                                  d.classes[idx].levelName = next || undefined;
                                });
                              }}
                            />
                          );
                        },
                      },
                      {
                        title: '人数',
                        width: 160,
                        render: (_, row) => {
                          const nums = row.members?.length ?? row.nums ?? 0;
                          return (
                            <div className="division-fill">
                              <span>{nums}</span>
                              <Progress
                                percent={Math.min(100, Math.round((nums / (packResult.classMax || 50)) * 100))}
                                size="small"
                                showInfo={false}
                              />
                              <Text type="secondary" style={{ fontSize: 12 }}>/{packResult.classMax}</Text>
                            </div>
                          );
                        },
                      },
                      {
                        title: '操作',
                        width: 100,
                        render: (_v, _row, idx) => (
                          <Button
                            type="link"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => {
                              setClassMemberKeyword('');
                              setEditClassIdx(idx);
                            }}
                          >
                            {editable ? '编辑学员' : '查看学员'}
                          </Button>
                        ),
                      },
                    ]}
                  />
                ),
              },
              {
                key: 'unassigned',
                label: `未分入 (${packResult.unassignedCount})`,
                children: (packResult.unassigned?.length ?? 0) === 0 ? (
                  <Alert type="success" showIcon message="全部可分人员均已入班草稿" />
                ) : (
                  <Table
                    rowKey={(r) => `${r.rosterId}-${r.reason}`}
                    size="small"
                    pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 人` }}
                    dataSource={packResult.unassigned}
                    columns={[
                      { title: '姓名', dataIndex: 'personName', width: 90 },
                      { title: '单位', dataIndex: 'workshop', width: 120, ellipsis: true, render: (v) => v || '-' },
                      { title: '工种', dataIndex: 'jobName', width: 110, ellipsis: true, render: (v) => v || '-' },
                      { title: '等级', dataIndex: 'levelName', width: 90, render: (v) => v || '-' },
                      { title: '身份证', dataIndex: 'idCard', width: 170 },
                      { title: '原因', dataIndex: 'reason' },
                      ...(editable ? [{
                        title: '操作',
                        width: 180,
                        render: (_: unknown, row: DivisionAutoPackUnassigned) => (
                          <Select
                            placeholder="分入班级"
                            style={{ width: 160 }}
                            options={classOptions}
                            onChange={(idx: number) => {
                              if (row.rosterId != null) moveUnassignedToClass(row.rosterId, idx);
                            }}
                          />
                        ),
                      } as ColumnsType<DivisionAutoPackUnassigned>[number]] : []),
                    ]}
                  />
                ),
              },
              ...(packResult.warnings?.length
                ? [{
                    key: 'warnings',
                    label: `警告 (${packResult.warnings.length})`,
                    children: (
                      <Alert
                        type="warning"
                        showIcon
                        message={`共 ${packResult.warnings.length} 条提示`}
                        description={(
                          <ul style={{ margin: '8px 0 0', paddingLeft: 18 }}>
                            {packResult.warnings.slice(0, 20).map((w, i) => (
                              <li key={i}>{w.message}</li>
                            ))}
                          </ul>
                        )}
                      />
                    ),
                  }]
                : []),
            ]}
          />

          <Space style={{ marginTop: 20 }} wrap>
            <Button onClick={() => setStep(1)}>返回配置</Button>
            {editable && (
              <>
                <Button loading={saveMu.isPending} disabled={!draftDirty} onClick={() => saveMu.mutate()}>
                  保存草稿
                </Button>
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  loading={submitMu.isPending}
                  onClick={confirmSubmit}
                >
                  提交企业审核
                </Button>
              </>
            )}
            {/*{packResult.status === 'pending' && (
              <Button onClick={() => navigateTo('company_plan_review')}>
                去企业分班确认
              </Button>
            )}*/}
            {packResult.status === 'materialized' && (
              <>
                <Button type="primary" onClick={() => navigateTo('train_people-list')}>去班级人员</Button>
                <Button
                  loading={syncPlatformUsersMu.isPending}
                  onClick={() => syncPlatformUsersMu.mutate()}
                >
                  补开 App 账号
                </Button>
                <Button onClick={() => navigateTo('company_plan_review')}>去企业分班确认</Button>
              </>
            )}
            <Button onClick={() => {
              setStep(0);
              setPackResult(null);
              setSelectedPlan(null);
              setTrainInfoId(null);
              setDraftDirty(false);
            }}
            >
              处理下一规划
            </Button>
          </Space>
        </Card>
      )}

      <Drawer
        title={editingClass ? `${editable ? '编辑' : '查看'}：${editingClass.title}` : '班级学员'}
        width={960}
        open={editClassIdx != null}
        onClose={() => {
          setClassMemberKeyword('');
          setEditClassIdx(null);
        }}
      >
        {editingClass && (
          <>
            {editable && isManualPack && (
              <Space style={{ marginBottom: 12 }} wrap>
                <Button
                  icon={<UserAddOutlined />}
                  onClick={() => {
                    setPoolSelectedIds([]);
                    setRosterPage(1);
                    setRosterPageSize(20);
                    setPoolKeywordInput('');
                    setPoolJobNameInput('');
                    setPoolKeyword('');
                    setPoolJobName('');
                    setPoolOpen(true);
                  }}
                >
                  从待分池勾选
                </Button>
                <Upload
                  accept=".xlsx,.xls"
                  showUploadList={false}
                  beforeUpload={(file) => {
                    importMembersMu.mutate(file);
                    return false;
                  }}
                >
                  <Button icon={<UploadOutlined />} loading={importMembersMu.isPending}>
                    上传名单
                  </Button>
                </Upload>
                <Button
                  onClick={async () => {
                    try {
                      const res = await trainApi.downloadDivisionMemberImportTemplate();
                      const blob = res.data as Blob;
                      const url = URL.createObjectURL(blob);
                      const a = document.createElement('a');
                      a.href = url;
                      a.download = 'class-member-import-template.xlsx';
                      a.click();
                      URL.revokeObjectURL(url);
                    } catch (e) {
                      message.error((e as Error).message || '下载模板失败');
                    }
                  }}
                >
                  下载导入模板
                </Button>
                <Button
                  type="primary"
                  ghost
                  icon={<PlusOutlined />}
                  onClick={() => {
                    addPersonForm.resetFields();
                    setAddPersonOpen(true);
                  }}
                >
                  手填加人
                </Button>
              </Space>
            )}
            <Input.Search
              allowClear
              style={{ width: 280, marginBottom: 12 }}
              placeholder="姓名 / 身份证号"
              value={classMemberKeyword}
              onChange={(e) => setClassMemberKeyword(e.target.value)}
            />
            <Table<DivisionAutoPackClassMember>
              rowKey="rosterId"
              size="small"
              pagination={false}
              dataSource={classMemberItems}
              locale={{ emptyText: '该班暂无学员' }}
              columns={[
                { title: '姓名', dataIndex: 'personName', width: 90 },
                { title: '单位', dataIndex: 'workshop', width: 120, ellipsis: true, render: (v) => v || '-' },
                { title: '工种', dataIndex: 'jobName', width: 110, ellipsis: true, render: (v) => v || '-' },
                { title: '等级', dataIndex: 'levelName', width: 90, render: (v) => v || '-' },
                { title: '班组', dataIndex: 'shiftTeam', width: 90, render: (v) => v || '-' },
                { title: '身份证', dataIndex: 'idCard', width: 170 },
                ...(editable && editClassIdx != null ? [{
                  title: '调整',
                  width: 220,
                  render: (_: unknown, row: DivisionAutoPackClassMember) => (
                    <Space>
                      <Select
                        placeholder="调到其他班"
                        style={{ width: 140 }}
                        options={classOptions.filter((o) => o.value !== editClassIdx)}
                        onChange={(toIdx: number) => moveMemberBetweenClasses(editClassIdx, row.rosterId, toIdx)}
                      />
                      <Button
                        type="link"
                        danger
                        size="small"
                        onClick={() => moveMemberToUnassigned(editClassIdx, row.rosterId)}
                      >
                        移出
                      </Button>
                    </Space>
                  ),
                } as ColumnsType<DivisionAutoPackClassMember>[number]] : []),
              ]}
            />
          </>
        )}
      </Drawer>

      <Modal
        title="从待分池勾选加人"
        open={poolOpen}
        onCancel={() => setPoolOpen(false)}
        onOk={addFromPoolToEditingClass}
        okText="加入本班"
        width={800}
        destroyOnClose
      >
        <Space style={{ marginBottom: 12 }} wrap>
          <Input.Search
            allowClear
            style={{ width: 260 }}
            placeholder="姓名 / 身份证号"
            value={poolKeywordInput}
            onChange={(e) => {
              setRosterPage(1);
              setPoolKeywordInput(e.target.value);
            }}
            onSearch={applyPoolFilters}
          />
          <Select
            allowClear
            showSearch
            style={{ width: 220 }}
            placeholder="工种"
            value={poolJobNameInput}
            options={poolJobOptions}
            optionFilterProp="label"
            onChange={(value) => {
              setRosterPage(1);
              setPoolJobNameInput(value ?? '');
            }}
          />
          <Button onClick={applyPoolFilters}>筛选</Button>
          <Button onClick={resetPoolFilters}>重置</Button>
          {poolSelectedIds.length > 0 && (
            <Text type="secondary">已选 {poolSelectedIds.length} 人</Text>
          )}
        </Space>
        <Table<DivisionRosterItem>
          rowKey="id"
          size="small"
          loading={rosterQuery.isLoading}
          columns={rosterColumns}
          dataSource={poolRosterItems}
          rowSelection={{
            selectedRowKeys: poolSelectedIds,
            onChange: (keys) => setPoolSelectedIds(keys as number[]),
            preserveSelectedRowKeys: true,
          }}
          pagination={{
            current: rosterPage,
            pageSize: rosterPageSize,
            total: poolRosterItems.length,
            showTotal: (t) => `共 ${t} 人`,
            showSizeChanger: true,
            pageSizeOptions: [20, 50, 100, 200, 500],
            onChange: (p, ps) => {
              setRosterPage(p);
              setRosterPageSize(ps);
            },
          }}
        />
      </Modal>

      <Modal
        title="手填加人"
        open={addPersonOpen}
        onCancel={() => setAddPersonOpen(false)}
        onOk={() => addPersonForm.submit()}
        confirmLoading={addPersonMu.isPending}
        destroyOnClose
      >
        <Form
          form={addPersonForm}
          layout="vertical"
          onFinish={(values) => addPersonMu.mutate(values)}
        >
          <Form.Item name="personName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input placeholder="必填" />
          </Form.Item>
          <Form.Item name="idCard" label="身份证" extra="强烈建议填写；无证时同名多人须补证">
            <Input placeholder="建议填写" />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input />
          </Form.Item>
          <Form.Item name="jobName" label="工种">
            <Input />
          </Form.Item>
          <Form.Item name="levelName" label="等级">
            <Input />
          </Form.Item>
          <Form.Item name="workshop" label="车间/单位">
            <Input />
          </Form.Item>
          <Form.Item name="shiftTeam" label="班组">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
