/**
 * 开班准备 · 班级资料收集面板（复用报名人员 API）
 */
import { useEffect, useMemo, useRef, useState, type MouseEvent } from 'react';
import {
  Button,
  Input,
  Modal,
  Pagination,
  Select,
  Space,
  Switch,
  Table,
  Upload,
  message,
} from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { toBrowserPublicFileSrc, downloadExportFile, downloadUrlFile, isPublicExamFileUrl } from '../../api/files';
import type { PlatformUser, TrainSignUpPerson } from '../../types/training';
import { SignUpPeopleEditModal } from './SignUpPeopleEditModal';
import { usePaneActive, usePaneActiveRef } from '../../layouts/TabKeepAlive';
import './TrainingInformationPage.css';

const STATUS_MAP: Record<number, { label: string }> = {
  0: { label: '待审核' },
  1: { label: '已通过' },
  2: { label: '未通过' },
};

/** 与开班准备班级列表一致：5=五级 … 1=一级 */
const CLASS_LEVEL_LABELS: Record<number, string> = {
  5: '五级/初级工',
  4: '四级/中级工',
  3: '三级/高级工',
  2: '二级/技师',
  1: '一级/高级技师',
};

function classLevelLabel(level?: number | string | null): string {
  const n = Number(level);
  if (!Number.isFinite(n) || n <= 0) return '';
  return CLASS_LEVEL_LABELS[n] ?? `${n}级`;
}

function calcAgeFromIdCard(idCard?: string): number | undefined {
  if (!idCard) return undefined;
  let birth = '';
  if (idCard.length === 18) birth = idCard.slice(6, 14);
  else if (idCard.length === 15) birth = `19${idCard.slice(6, 12)}`;
  if (!birth) return undefined;
  const year = Number(birth.slice(0, 4));
  const month = Number(birth.slice(4, 6));
  const day = Number(birth.slice(6, 8));
  if (!year || !month || !day) return undefined;
  const today = dayjs();
  let age = today.year() - year;
  if (today.month() + 1 < month || (today.month() + 1 === month && today.date() < day)) age -= 1;
  return age >= 0 ? age : undefined;
}

function normalizeSignUpPerson(raw: Record<string, unknown>): TrainSignUpPerson {
  const pick = (...keys: string[]) => {
    for (const key of keys) {
      const v = raw[key];
      if (v !== undefined && v !== null && v !== '') return v;
    }
    return undefined;
  };
  const idCardNumber = pick('idCardNumber', 'id_card_number') != null
    ? String(pick('idCardNumber', 'id_card_number'))
    : undefined;
  const status = pick('status') != null ? Number(pick('status')) : undefined;
  return {
    id: Number(pick('id')),
    signUpId: pick('signUpId', 'sign_up_id') != null ? Number(pick('signUpId', 'sign_up_id')) : undefined,
    platformUserId: pick('platformUserId', 'platform_user_id') != null
      ? Number(pick('platformUserId', 'platform_user_id'))
      : undefined,
    name: pick('name') != null ? String(pick('name')) : undefined,
    phone: pick('phone') != null ? String(pick('phone')) : undefined,
    idCardNumber,
    education: pick('education') != null ? String(pick('education')) : undefined,
    status,
    statusCn: pick('statusCn', 'status_cn') != null
      ? String(pick('statusCn', 'status_cn'))
      : STATUS_MAP[status ?? 0]?.label,
    age: pick('age') != null ? Number(pick('age')) : calcAgeFromIdCard(idCardNumber),
    workTypeName: pick('workTypeName', 'work_type_name') != null
      ? String(pick('workTypeName', 'work_type_name'))
      : undefined,
    level: pick('level') != null ? Number(pick('level')) : undefined,
    hasPay: pick('hasPay', 'has_pay') != null ? Number(pick('hasPay', 'has_pay')) : 0,
    createTime: pick('createTime', 'create_time') != null
      ? String(pick('createTime', 'create_time'))
      : undefined,
  };
}

function renderPeopleStatus(status?: number, statusCn?: string) {
  const label = statusCn || STATUS_MAP[status ?? 0]?.label || '待审核';
  return <span className={`status-tag status-${status ?? 0}`}>{label}</span>;
}

export type ClassSignUpPeoplePanelProps = {
  signUpId: number | string;
  title?: string;
  /** 开班准备班级工种/等级，资料收集列表与班级列表保持一致 */
  classWorkTypeId?: number;
  classWorkTypeTitle?: string;
  classWorkTypeLevel?: number;
  onChanged?: () => void;
};

export function ClassSignUpPeoplePanel({
  signUpId,
  title,
  classWorkTypeTitle,
  classWorkTypeLevel,
  classWorkTypeId,
  onChanged,
}: ClassSignUpPeoplePanelProps) {
  const paneActiveRef = usePaneActiveRef(usePaneActive()); // 标签不可见时导出计时跳过 setState（§3.6）
  const [peopleKeyword, setPeopleKeyword] = useState('');
  const [peopleStatus, setPeopleStatus] = useState<number | undefined>();
  const [peoplePage, setPeoplePage] = useState(1);
  const [selectedPeopleIds, setSelectedPeopleIds] = useState<number[]>([]);
  const [peopleEditOpen, setPeopleEditOpen] = useState(false);
  const [editingPersonId, setEditingPersonId] = useState<number | null>(null);
  const [importOpen, setImportOpen] = useState(false);
  const [importLoading, setImportLoading] = useState(false);
  const [rosterImportOpen, setRosterImportOpen] = useState(false);
  const [rosterImportLoading, setRosterImportLoading] = useState(false);
  const [templateDownloading, setTemplateDownloading] = useState(false);
  const [importStudentOpen, setImportStudentOpen] = useState(false);
  const [importStudentLoading, setImportStudentLoading] = useState(false);
  const [importByIdCardOpen, setImportByIdCardOpen] = useState(false);
  const [importByIdCardLoading, setImportByIdCardLoading] = useState(false);
  const [idCardText, setIdCardText] = useState('');
  const [historySyncLoading, setHistorySyncLoading] = useState(false);
  const [studentKeyword, setStudentKeyword] = useState('');
  const [studentPage, setStudentPage] = useState(1);
  const [selectedStudentIds, setSelectedStudentIds] = useState<number[]>([]);
  const [qrModal, setQrModal] = useState<{
    title: string;
    qrcodeDataUrl?: string;
    qrcodeUrl?: string;
    appQrcodeDataUrl?: string;
    appQrcodeUrl?: string;
  } | null>(null);
  const [wxQrcodeLoading, setWxQrcodeLoading] = useState(false);
  const [defaultExportOpen, setDefaultExportOpen] = useState(false);
  const [defaultExportTitle, setDefaultExportTitle] = useState('');
  const [defaultExportUrl, setDefaultExportUrl] = useState('');
  const [exportElapsed, setExportElapsed] = useState(0);
  const [defaultExportTaskId, setDefaultExportTaskId] = useState<number | null>(null);
  const [exportDownloading, setExportDownloading] = useState(false);
  const defaultExportTimerRef = useRef<number | null>(null);
  const defaultExportCancelledRef = useRef(false);
  const defaultExportGenRef = useRef(0);

  useEffect(() => {
    setPeoplePage(1);
    setSelectedPeopleIds([]);
    setPeopleKeyword('');
    setPeopleStatus(undefined);
  }, [signUpId]);

  useEffect(() => () => {
    if (defaultExportTimerRef.current) {
      window.clearInterval(defaultExportTimerRef.current);
    }
  }, []);

  const { data: peopleData, isLoading: peopleLoading, refetch: refetchPeople } = useQuery({
    queryKey: ['class-sign-up-people', signUpId, peoplePage, peopleKeyword, peopleStatus],
    queryFn: () => trainApi.pageSignUpPeople({
      signUpId,
      page: peoplePage,
      pageSize: 20,
      keyword: peopleKeyword || undefined,
      status: peopleStatus,
    }),
    enabled: !!signUpId,
  });

  const { data: studentData, isLoading: studentLoading } = useQuery({
    queryKey: ['class-sign-up-import-students', studentPage, studentKeyword],
    queryFn: () => trainApi.pageSignUpImportStudents({
      page: studentPage,
      pageSize: 20,
      keyword: studentKeyword || undefined,
    }),
    enabled: importStudentOpen,
  });

  const notifyChanged = () => {
    void refetchPeople();
    onChanged?.();
  };

  const auditMu = useMutation({
    mutationFn: ({ ids, value }: { ids: number[]; value: number }) =>
      trainApi.batchUpdateSignUpPeople({ ids, field: 'status', value }),
    onSuccess: () => {
      message.success('审核完成');
      setSelectedPeopleIds([]);
      notifyChanged();
    },
  });

  const deletePeopleMu = useMutation({
    mutationFn: (ids: number[]) => trainApi.batchUpdateSignUpPeople({ ids, field: 'deleted', value: 1 }),
    onSuccess: (updated) => {
      if (!updated) {
        message.warning('未删除任何人员，请刷新后重试');
        return;
      }
      message.success(`已删除 ${updated} 人`);
      setSelectedPeopleIds([]);
      setPeopleEditOpen(false);
      setEditingPersonId(null);
      notifyChanged();
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error && err.message ? err.message : '批量删除失败';
      message.error(msg);
    },
  });

  const handleBatchDelete = () => {
    if (!selectedPeopleIds.length) {
      message.warning('请先勾选人员');
      return;
    }
    Modal.confirm({
      title: '确认批量删除',
      content: `将删除已勾选的 ${selectedPeopleIds.length} 人，删除后列表不再显示。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => deletePeopleMu.mutateAsync(selectedPeopleIds),
    });
  };

  const updatePayMu = useMutation({
    mutationFn: ({ id, value }: { id: number; value: number }) =>
      trainApi.updateSignUpPerson(id, { hasPay: value }),
    onSuccess: () => notifyChanged(),
    onError: () => {
      message.error('更新支付状态失败');
      refetchPeople();
    },
  });

  const peopleRows = useMemo(
    () => (peopleData?.items ?? []).map((row) => normalizeSignUpPerson(row as unknown as Record<string, unknown>)),
    [peopleData?.items],
  );

  const isPeopleChecked = selectedPeopleIds.length === 0;

  const showQrcode = async () => {
    try {
      const data = await trainApi.getSignUpQrcode(signUpId);
      setQrModal({
        title: title || data.title || '资料收集二维码',
        qrcodeDataUrl: data.qrcodeDataUrl,
        qrcodeUrl: data.qrcode,
        appQrcodeDataUrl: data.appQrcodeDataUrl,
        appQrcodeUrl: data.appQrcode,
      });
    } catch {
      message.error('获取二维码失败');
    }
  };

  const handleGenerateWxQrcode = async () => {
    setWxQrcodeLoading(true);
    try {
      const data = await trainApi.generateSignUpQrcode(signUpId);
      setQrModal((prev) => ({
        title: prev?.title || title || data.title || '资料收集二维码',
        qrcodeDataUrl: data.qrcodeDataUrl || prev?.qrcodeDataUrl,
        qrcodeUrl: data.qrcode || prev?.qrcodeUrl,
        appQrcodeDataUrl: data.appQrcodeDataUrl || prev?.appQrcodeDataUrl,
        appQrcodeUrl: data.appQrcode || prev?.appQrcodeUrl,
      }));
      message.success('小程序码已生成');
    } catch {
      message.error('生成小程序码失败');
    } finally {
      setWxQrcodeLoading(false);
    }
  };

  const handleExportPeople = async () => {
    try {
      await trainApi.exportSignUpPeople(signUpId, selectedPeopleIds.length ? selectedPeopleIds : undefined);
      message.success('导出成功');
    } catch {
      message.error('导出失败');
    }
  };

  const handleDefaultTemplateExport = async () => {
    // generation 计数器：每次点击递增，旧循环发现自己不是最新一代时自动退出
    const gen = ++defaultExportGenRef.current;
    setDefaultExportOpen(true);
    setDefaultExportTitle('默认模板正在导出，请稍后');
    setDefaultExportUrl('');
    setExportElapsed(0);
    setDefaultExportTaskId(null);
    defaultExportCancelledRef.current = false;
    if (defaultExportTimerRef.current) {
      window.clearInterval(defaultExportTimerRef.current);
    }
    defaultExportTimerRef.current = window.setInterval(() => {
      // 标签不可见时计时继续、跳过 setState（§3.6 乙组口径：不中断任务）
      if (paneActiveRef.current) setExportElapsed((v) => v + 1);
    }, 1000);
    try {
      const start = await trainApi.startDefaultTemplateExport({
        signUpId: Number(signUpId),
        ids: selectedPeopleIds.length ? selectedPeopleIds : undefined,
      });
      // await 返回后检查是否已被新一次点击取代
      if (gen !== defaultExportGenRef.current) {
        return;
      }
      const taskId = start.taskId;
      if (!taskId) {
        throw new Error('no task');
      }
      setDefaultExportTaskId(Number(taskId));
      setDefaultExportTitle(`当前进度：${start.jindu || '导出中'}`);
      let finished = false;
      let delay = 2000;
      while (!finished && !defaultExportCancelledRef.current && gen === defaultExportGenRef.current) {
        await new Promise((r) => setTimeout(r, delay));
        if (defaultExportCancelledRef.current || gen !== defaultExportGenRef.current) {
          break;
        }
        const check = await trainApi.checkDefaultTemplateExport({
          signUpId: Number(signUpId),
          taskId: Number(taskId),
        });
        // check 返回后再次检查代次，防止旧循环覆盖新循环的结果
        if (gen !== defaultExportGenRef.current) {
          break;
        }
        if (check.url) {
          setDefaultExportTitle('导出成功');
          setDefaultExportUrl(toBrowserPublicFileSrc(String(check.url)));
          finished = true;
          break;
        }
        if (check.error && !String(check.msg ?? '').includes('未找到压缩包')) {
          setDefaultExportTitle(`导出失败，${check.msg ?? ''}`);
          finished = true;
          break;
        }
        if (check.jindu) {
          setDefaultExportTitle(`当前进度：${check.jindu}`);
        }
        // 指数退避：2s → 4s → 8s，上限 15s
        delay = Math.min(delay * 2, 15000);
      }
      if (!finished && !defaultExportCancelledRef.current && gen === defaultExportGenRef.current) {
        setDefaultExportTitle('导出超时，请稍后在导出记录中重试或联系管理员');
      }
    } catch {
      if (gen === defaultExportGenRef.current) {
        setDefaultExportTitle('导出失败');
      }
    } finally {
      if (gen === defaultExportGenRef.current && defaultExportTimerRef.current) {
        window.clearInterval(defaultExportTimerRef.current);
        defaultExportTimerRef.current = null;
      }
    }
  };

  const handleCancelDefaultExport = async () => {
    defaultExportCancelledRef.current = true;
    const taskId = defaultExportTaskId;
    if (taskId != null) {
      try {
        await trainApi.cancelDefaultTemplateExport({
          signUpId: Number(signUpId),
          taskId,
        });
      } catch {
        // 忽略取消请求失败
      }
    }
    setDefaultExportTitle('已取消导出');
  };

  const handleDownloadExport = async () => {
    if (!defaultExportUrl || exportDownloading) return;
    if (isPublicExamFileUrl(defaultExportUrl)) {
      downloadExportFile(defaultExportUrl);
      return;
    }
    setExportDownloading(true);
    try {
      await downloadUrlFile(defaultExportUrl);
    } catch {
      message.error('下载失败，请稍后重试');
    } finally {
      setExportDownloading(false);
    }
  };

  const handleDownloadImportTemplate = async (event: MouseEvent) => {
    event.preventDefault();
    event.stopPropagation();
    if (templateDownloading) return;
    setTemplateDownloading(true);
    try {
      const blob = await trainApi.downloadSignUpPeopleImportTemplate();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = '报名批量导入模板.xlsx';
      anchor.click();
      URL.revokeObjectURL(url);
      message.success('模板下载成功');
    } catch {
      message.error('模板下载失败');
    } finally {
      setTemplateDownloading(false);
    }
  };

  const importUploadProps: UploadProps = {
    accept: '.xls,.xlsx',
    maxCount: 1,
    showUploadList: false,
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options;
      setImportLoading(true);
      try {
        const result = await trainApi.importSignUpPeople(signUpId, file as File);
        const sync = result.platformUserSync;
        const syncHint = sync
          ? `，App账号就绪 ${sync.loginReady ?? 0}（新建 ${sync.created ?? 0}/复用 ${sync.reused ?? 0}）`
          : '';
        const skippedHint = (result.skipped ?? 0) > 0 ? `，跳过重复 ${result.skipped} 人` : '';
        const errHint = result.errors?.length ? `；注意：${result.errors.slice(0, 2).join('；')}` : '';
        message.success(`导入成功: ${result.inserted ?? 0} 人${skippedHint}${syncHint}${errHint}`);
        setImportOpen(false);
        notifyChanged();
        onSuccess?.(result);
      } catch (err) {
        onError?.(err as Error);
      } finally {
        setImportLoading(false);
      }
    },
  };

  const rosterImportUploadProps: UploadProps = {
    accept: '.xls,.xlsx',
    maxCount: 1,
    showUploadList: false,
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options;
      setRosterImportLoading(true);
      try {
        const result = await trainApi.importClassPrepRoster(signUpId, file as File);
        const sync = result.platformUserSync;
        const syncHint = sync
          ? `，App账号就绪 ${sync.loginReady ?? 0}（新建 ${sync.created ?? 0}/复用 ${sync.reused ?? 0}）`
          : '';
        const skippedHint = (result.skipped ?? 0) > 0 ? `，跳过重复 ${result.skipped} 人` : '';
        const failedHint = (result.invalid ?? 0) > 0 ? `，失败 ${result.invalid} 人` : '';
        const errHint = result.errors?.length ? `；注意：${result.errors.slice(0, 5).join('；')}` : '';
        message.success(
          `导入成功 ${result.inserted ?? 0} 人${skippedHint}${failedHint}${syncHint}${errHint}`,
        );
        setRosterImportOpen(false);
        notifyChanged();
        onSuccess?.(result);
      } catch (err) {
        onError?.(err as Error);
        message.error((err as Error)?.message || '花名册导入失败');
      } finally {
        setRosterImportLoading(false);
      }
    },
  };

  const handleImportStudents = async () => {
    if (selectedStudentIds.length === 0) {
      message.error('请选择要导入的学员');
      return;
    }
    setImportStudentLoading(true);
    try {
      const result = await trainApi.importSignUpStudents(signUpId, selectedStudentIds);
      const sync = result.platformUserSync;
      const syncHint = sync
        ? `，App账号就绪 ${sync.loginReady ?? 0}（新建 ${sync.created ?? 0}/复用 ${sync.reused ?? 0}）`
        : '';
      const skippedHint = result.skipped > 0 ? `，跳过重复 ${result.skipped} 人` : '';
      const errHint = result.errors?.length ? `；注意：${result.errors.slice(0, 2).join('；')}` : '';
      message.success(`导入成功: ${result.inserted} 人${skippedHint}${syncHint}${errHint}`);
      setImportStudentOpen(false);
      setSelectedStudentIds([]);
      notifyChanged();
    } catch {
      // interceptor
    } finally {
      setImportStudentLoading(false);
    }
  };

  const showImportResult = (result: Awaited<ReturnType<typeof trainApi.importSignUpStudentsByIdCard>>) => {
    const sync = result.platformUserSync;
    const syncHint = sync
      ? `，App账号就绪 ${sync.loginReady ?? 0}（新建 ${sync.created ?? 0}/复用 ${sync.reused ?? 0}）`
      : '';
    const skippedHint = result.skipped > 0 ? `，跳过重复 ${result.skipped} 人` : '';
    const errHint = result.errors?.length ? `；${result.errors.slice(0, 3).join('；')}` : '';
    if (result.inserted > 0) {
      message.success(`导入成功: ${result.inserted} 人${skippedHint}${syncHint}${errHint}`);
    } else if (result.errors?.length) {
      message.warning(`未导入新人${skippedHint}${errHint}`);
    } else {
      message.success(`导入完成${skippedHint}${syncHint}`);
    }
  };

  const handleImportStudentsByIdCard = async () => {
    if (!idCardText.trim()) {
      message.error('请输入身份证号');
      return;
    }
    setImportByIdCardLoading(true);
    try {
      const result = await trainApi.importSignUpStudentsByIdCard(signUpId, idCardText);
      showImportResult(result);
      setImportByIdCardOpen(false);
      setIdCardText('');
      setImportStudentOpen(false);
      setSelectedStudentIds([]);
      notifyChanged();
    } catch {
      // interceptor
    } finally {
      setImportByIdCardLoading(false);
    }
  };

  const handleSyncHistory = () => {
    const ids = selectedPeopleIds.length ? selectedPeopleIds : undefined;
    const scopeText = ids?.length
      ? `已勾选的 ${ids.length} 人`
      : '当前计划全部学员';
    Modal.confirm({
      title: '同步历史信息收集',
      content: (
        <div>
          <p>将按身份证从历史信息收集/扫码填报中，为{scopeText}补全当前名单的空字段。</p>
          <p style={{ marginBottom: 0 }}>仅补空、不覆盖已有内容；不修改审核与支付状态。历史计划数据本身不会被改动。</p>
        </div>
      ),
      okText: '开始同步',
      cancelText: '取消',
      onOk: async () => {
        setHistorySyncLoading(true);
        try {
          const result = await trainApi.syncSignUpPeopleHistory(signUpId, ids);
          const parts = [
            `处理 ${result.total} 人`,
            `补全 ${result.merged} 人`,
          ];
          if (result.unchanged > 0) parts.push(`无需变更 ${result.unchanged}`);
          if (result.skippedNoHistory > 0) parts.push(`无历史 ${result.skippedNoHistory}`);
          if (result.skippedNoIdCard > 0) parts.push(`无身份证 ${result.skippedNoIdCard}`);
          const detail = result.messages?.length
            ? `；${result.messages.slice(0, 3).join('；')}`
            : '';
          message.success(`${parts.join('，')}${detail}`);
          notifyChanged();
        } catch {
          // interceptor
        } finally {
          setHistorySyncLoading(false);
        }
      },
    });
  };

  const peopleColumns: ColumnsType<TrainSignUpPerson> = [
    { title: 'ID', dataIndex: 'id', width: 72 },
    { title: '姓名', dataIndex: 'name', width: 90 },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '身份证', dataIndex: 'idCardNumber', width: 170 },
    { title: '年龄', dataIndex: 'age', width: 64, render: (v?: number) => v ?? '' },
    {
      title: '工种',
      dataIndex: 'workTypeName',
      ellipsis: true,
      render: () => classWorkTypeTitle || '-',
    },
    {
      title: '等级',
      dataIndex: 'level',
      width: 130,
      render: () => classLevelLabel(classWorkTypeLevel) || '-',
    },
    {
      title: '审核',
      dataIndex: 'status',
      width: 90,
      render: (v: number, row) => renderPeopleStatus(v, row.statusCn),
    },
    {
      title: '已支付',
      dataIndex: 'hasPay',
      width: 80,
      render: (v: number | undefined, row) => (
        <Switch
          checked={!!v}
          size="small"
          loading={updatePayMu.isPending && updatePayMu.variables?.id === row.id}
          onChange={(checked) => updatePayMu.mutate({ id: row.id, value: checked ? 1 : 0 })}
        />
      ),
    },
    {
      title: '操作',
      key: 'act',
      width: 72,
      render: (_, row) => (
        <Button
          type="link"
          size="small"
          onClick={() => {
            setEditingPersonId(row.id);
            setPeopleEditOpen(true);
          }}
        >
          编辑
        </Button>
      ),
    },
  ];

  return (
    <div className="people-modal-body class-sign-up-people-panel">
      <div className="people-search-row">
        <Input
          className="people-search-input"
          placeholder="关键字搜索"
          value={peopleKeyword}
          onChange={(e) => { setPeopleKeyword(e.target.value); setPeoplePage(1); }}
          allowClear
        />
        <Select
          className="people-search-status"
          allowClear
          placeholder="审核状态"
          value={peopleStatus}
          onChange={(v) => { setPeopleStatus(v); setPeoplePage(1); }}
          options={[
            { value: 0, label: '待审核' },
            { value: 1, label: '已通过' },
            { value: 2, label: '未通过' },
          ]}
        />
      </div>

      <div className="tools">
        <Space wrap size={[8, 8]}>
          <Button type="primary" onClick={showQrcode}>扫码二维码</Button>
          <Button
            type="primary"
            disabled={isPeopleChecked}
            onClick={() => auditMu.mutate({ ids: selectedPeopleIds, value: 1 })}
          >
            批量通过
          </Button>
          <Button
            danger
            disabled={isPeopleChecked}
            onClick={() => auditMu.mutate({ ids: selectedPeopleIds, value: 2 })}
          >
            批量拒绝
          </Button>
          <Button
            danger
            disabled={isPeopleChecked}
            loading={deletePeopleMu.isPending}
            onClick={handleBatchDelete}
          >
            批量删除
          </Button>
          <Button type="primary" onClick={handleDefaultTemplateExport}>默认模板导出</Button>
          <Button type="primary" onClick={handleExportPeople}>仅导出名单</Button>
          <Button type="primary" onClick={() => setImportOpen(true)}>批量导入</Button>
          <Button
            type="primary"
            onClick={() => {
              setStudentKeyword('');
              setStudentPage(1);
              setSelectedStudentIds([]);
              setImportStudentOpen(true);
            }}
          >
            从学员中导入
          </Button>
          <Button type="primary" onClick={() => setRosterImportOpen(true)}>
            花名册导入
          </Button>
          <Button type="primary" loading={historySyncLoading} onClick={handleSyncHistory}>
            同步历史信息收集
          </Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        loading={peopleLoading}
        columns={peopleColumns}
        dataSource={peopleRows}
        scroll={{ y: 360, x: 1100 }}
        pagination={false}
        size="small"
        rowSelection={{
          selectedRowKeys: selectedPeopleIds,
          onChange: (keys) => setSelectedPeopleIds(keys as number[]),
        }}
      />

      <div className="pagination-container" style={{ height: 56 }}>
        <Pagination
          current={peoplePage}
          pageSize={20}
          total={peopleData?.total ?? 0}
          onChange={setPeoplePage}
          showTotal={(total) => `共 ${total} 条`}
          showSizeChanger={false}
          size="small"
        />
      </div>

      <SignUpPeopleEditModal
        open={peopleEditOpen}
        personId={editingPersonId}
        classWorkTypeId={classWorkTypeId}
        classWorkTypeTitle={classWorkTypeTitle}
        classWorkTypeLevel={classWorkTypeLevel}
        onClose={() => { setPeopleEditOpen(false); setEditingPersonId(null); }}
        onSaved={() => {
          setPeopleEditOpen(false);
          setEditingPersonId(null);
          notifyChanged();
        }}
      />

      <Modal
        title="批量导入报名人员"
        open={importOpen}
        onCancel={() => setImportOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Upload.Dragger {...importUploadProps} disabled={importLoading}>
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p className="ant-upload-text">点击或拖拽 Excel 到此处上传</p>
          <p className="ant-upload-hint">
            <a onClick={handleDownloadImportTemplate}>
              {templateDownloading ? '模板下载中…' : '下载导入模板'}
            </a>
          </p>
        </Upload.Dragger>
      </Modal>

      <Modal
        title="花名册导入"
        open={rosterImportOpen}
        onCancel={() => setRosterImportOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Upload.Dragger {...rosterImportUploadProps} disabled={rosterImportLoading}>
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p className="ant-upload-text">点击或拖拽《新型学徒制培训学员花名册》到此处</p>
          <p className="ant-upload-hint">
            需含姓名、身份证号码、联系电话。导入本班资料收集并同步学员名单、开 App 账号。
          </p>
        </Upload.Dragger>
      </Modal>

      <Modal
        title="从学员中导入"
        open={importStudentOpen}
        onCancel={() => setImportStudentOpen(false)}
        onOk={handleImportStudents}
        confirmLoading={importStudentLoading}
        width={720}
        destroyOnHidden
      >
        <Space style={{ marginBottom: 12 }} wrap>
          <Input
            placeholder="搜索学员"
            value={studentKeyword}
            onChange={(e) => { setStudentKeyword(e.target.value); setStudentPage(1); }}
            allowClear
            style={{ width: 260 }}
          />
          <Button onClick={() => { setIdCardText(''); setImportByIdCardOpen(true); }}>
            按身份证号导入
          </Button>
        </Space>
        <Table
          rowKey="id"
          loading={studentLoading}
          size="small"
          dataSource={studentData?.items ?? []}
          pagination={false}
          scroll={{ y: 320 }}
          rowSelection={{
            selectedRowKeys: selectedStudentIds,
            onChange: (keys) => setSelectedStudentIds(keys as number[]),
          }}
          columns={[
            { title: 'ID', dataIndex: 'id', width: 80 },
            { title: '姓名', dataIndex: 'realname', width: 100 },
            { title: '手机', dataIndex: 'phone', width: 120 },
            { title: '身份证', dataIndex: 'idcard', ellipsis: true },
          ] as ColumnsType<PlatformUser>}
        />
        <Pagination
          style={{ marginTop: 12 }}
          current={studentPage}
          pageSize={20}
          total={studentData?.total ?? 0}
          onChange={setStudentPage}
          size="small"
          showSizeChanger={false}
        />
      </Modal>

      <Modal
        title="按身份证号导入"
        open={importByIdCardOpen}
        onCancel={() => { setImportByIdCardOpen(false); setIdCardText(''); }}
        footer={null}
        maskClosable={false}
        width={420}
        destroyOnHidden
      >
        <Input.TextArea
          autoSize={{ minRows: 4, maxRows: 10 }}
          value={idCardText}
          onChange={(e) => setIdCardText(e.target.value)}
          placeholder="请输入身份证,一行一个"
        />
        <div style={{ marginTop: 12, textAlign: 'right' }}>
          <Button type="primary" loading={importByIdCardLoading} onClick={handleImportStudentsByIdCard}>
            确定
          </Button>
        </div>
      </Modal>

      <Modal
        title={qrModal?.title || '二维码'}
        open={!!qrModal}
        onCancel={() => setQrModal(null)}
        footer={(
          <Space>
            <Button loading={wxQrcodeLoading} onClick={handleGenerateWxQrcode}>生成小程序码</Button>
            <Button type="primary" onClick={() => setQrModal(null)}>关闭</Button>
          </Space>
        )}
        width={480}
      >
        <div style={{ display: 'flex', gap: 24, justifyContent: 'center', flexWrap: 'wrap' }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ marginBottom: 8 }}>H5 / APP</div>
            {(qrModal?.appQrcodeDataUrl || qrModal?.qrcodeDataUrl || qrModal?.appQrcodeUrl || qrModal?.qrcodeUrl) ? (
              <img
                alt="qrcode"
                src={qrModal.appQrcodeDataUrl || qrModal.qrcodeDataUrl || qrModal.appQrcodeUrl || qrModal.qrcodeUrl}
                style={{ width: 180, height: 180 }}
              />
            ) : (
              <div style={{ width: 180, height: 180, lineHeight: '180px', background: '#f5f5f5' }}>暂无</div>
            )}
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ marginBottom: 8 }}>微信小程序</div>
            {(qrModal?.qrcodeDataUrl || qrModal?.qrcodeUrl) ? (
              <img
                alt="wx-qrcode"
                src={qrModal.qrcodeDataUrl || qrModal.qrcodeUrl}
                style={{ width: 180, height: 180 }}
              />
            ) : (
              <div style={{ width: 180, height: 180, lineHeight: '180px', background: '#f5f5f5' }}>点击生成</div>
            )}
          </div>
        </div>
      </Modal>

      <Modal
        title="默认模板导出"
        open={defaultExportOpen}
        onCancel={() => setDefaultExportOpen(false)}
        footer={(
          <Space>
            {!defaultExportUrl && defaultExportTaskId != null ? (
              <Button danger onClick={handleCancelDefaultExport}>取消导出</Button>
            ) : null}
            <Button type="primary" onClick={() => setDefaultExportOpen(false)}>关闭</Button>
          </Space>
        )}
      >
        <p>{defaultExportTitle}{!defaultExportUrl ? `（${exportElapsed}s）` : ''}</p>
        {defaultExportUrl ? (
          <Button type="primary" loading={exportDownloading} onClick={handleDownloadExport}>下载文件</Button>
        ) : null}
      </Modal>
    </div>
  );
}
