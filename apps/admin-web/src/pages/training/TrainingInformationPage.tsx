import { useMemo, useState, useRef, useEffect, type MouseEvent } from 'react';
import {
  Button, DatePicker, Form, Image, Input, InputNumber, Modal, Pagination, Select, Space, Spin,
  Switch, Table, Typography, Upload, message,
} from 'antd';
import { InboxOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import type { TrainSignUpSelf, TrainSignUpSelfCreateReq, TrainSignUpPerson, TrainSignUpOrcSelf, TrainSignUpSelfFile, PlatformUser } from '../../types/training';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';
import { AuthImage } from '../../components/ApiAuthImage';
import { fileNameFromUrl } from '../../api/files';
import { SignUpPeopleEditModal } from './SignUpPeopleEditModal';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import './TrainingInformationPage.css';

const { RangePicker } = DatePicker;
const DATE_FORMAT = 'YYYY-MM-DD';
const APP_SIGN_UP_H5_URL = 'https://h5.gjjd168.com/#/pages/signUp/self';

interface QrModalState {
  signUpId: number;
  title: string;
  appQrcodeUrl: string;
  wxQrcodeUrl: string;
  path: string;
}

function buildAppSignUpQrcodeUrl(signUpId: number): string {
  return `${APP_SIGN_UP_H5_URL}?id=${signUpId}`;
}

/** 优先使用后端返回的 base64，避免 CDN 静态资源访问问题 */
function resolveWxQrcodeDisplayUrl(
  qrcodeDataUrl?: string,
  qrcodeUrl?: string,
  fallbackUrl?: string,
): string {
  if (qrcodeDataUrl) return qrcodeDataUrl;
  const url = qrcodeUrl || fallbackUrl || '';
  return toProxiedCdnUrl(url);
}

/** 将 CDN 绝对地址转为同源相对路径，便于 dev 代理与 img 加载 */
function fileTypeFromPath(path?: string): string {
  if (!path || !path.includes('.')) return '';
  const ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
  const slash = ext.indexOf('/');
  return slash >= 0 ? ext.substring(0, slash) : ext;
}

function toProxiedCdnUrl(url: string): string {
  if (!url) return url;
  const marker = '/exam/files/';
  const idx = url.indexOf(marker);
  if (idx >= 0) {
    return `/api${url.substring(idx)}`;
  }
  if (url.startsWith('/api/')) {
    return url;
  }
  return url;
}

interface SignUpPlanFormValues {
  title: string;
  cover?: string;
  trainTimeRange?: [Dayjs, Dayjs];
  examTimeDate?: Dayjs;
  orgId: number;
  trainInfoId?: number;
  sortNumber?: number;
}

function parseTrainTimeRange(value?: string): [Dayjs, Dayjs] | undefined {
  if (!value?.trim()) return undefined;
  const matches = value.match(/\d{4}-\d{2}-\d{2}/g);
  if (!matches?.length) return undefined;
  const start = dayjs(matches[0], DATE_FORMAT);
  const end = dayjs(matches[matches.length - 1], DATE_FORMAT);
  if (!start.isValid() || !end.isValid()) return undefined;
  return start.isAfter(end) ? [end, start] : [start, end];
}

function parseExamTime(value?: string): Dayjs | undefined {
  if (!value?.trim()) return undefined;
  const match = value.match(/\d{4}-\d{2}-\d{2}/);
  if (!match) return undefined;
  const date = dayjs(match[0], DATE_FORMAT);
  return date.isValid() ? date : undefined;
}

function formatTrainTimeRange(range?: [Dayjs, Dayjs]): string {
  if (!range?.[0] || !range[1]) return '';
  return `${range[0].format(DATE_FORMAT)} ~ ${range[1].format(DATE_FORMAT)}`;
}

function toSignUpPayload(values: SignUpPlanFormValues): TrainSignUpSelfCreateReq {
  return {
    title: values.title,
    cover: values.cover,
    trainTime: formatTrainTimeRange(values.trainTimeRange),
    examTime: values.examTimeDate?.format(DATE_FORMAT) ?? '',
    orgId: values.orgId,
    trainInfoId: values.trainInfoId,
    sortNumber: values.sortNumber,
  };
}

const STATUS_MAP: Record<number, { label: string }> = {
  0: { label: '待审核' },
  1: { label: '已通过' },
  2: { label: '未通过' },
};

const LEVEL_LABELS = ['五级/初级工', '四级/中级工', '三级/高级工', '二级/技师', '一级/高级技师'];

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

function normalizeSignUpPlanFiles(raw: unknown): TrainSignUpSelfFile[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((item) => {
      if (typeof item === 'string' && item) {
        return { fileUrl: item };
      }
      if (item && typeof item === 'object') {
        const row = item as Record<string, unknown>;
        const fileUrl = String(row.fileUrl ?? row.file_url ?? '');
        if (!fileUrl) return null;
        const fileName = row.fileName ?? row.file_name;
        const fileType = row.fileType ?? row.file_type;
        return {
          fileUrl,
          fileName: fileName != null ? String(fileName) : undefined,
          fileType: fileType != null ? String(fileType) : undefined,
        };
      }
      return null;
    })
    .filter((item): item is TrainSignUpSelfFile => item != null);
}

function normalizeSignUpPlan(raw: Record<string, unknown>): TrainSignUpSelf {
  return {
    ...(raw as unknown as TrainSignUpSelf),
    files: normalizeSignUpPlanFiles(raw.files),
  };
}

function normalizeSignUpPerson(raw: Record<string, unknown>): TrainSignUpPerson {
  const pick = (...keys: string[]) => {
    for (const key of keys) {
      const v = raw[key];
      if (v !== undefined && v !== null && v !== '') return v;
    }
    return undefined;
  };
  const idCardNumber = pick('idCardNumber', 'id_card_number') != null ? String(pick('idCardNumber', 'id_card_number')) : undefined;
  const status = pick('status') != null ? Number(pick('status')) : undefined;
  return {
    id: Number(pick('id')),
    signUpId: pick('signUpId', 'sign_up_id') != null ? Number(pick('signUpId', 'sign_up_id')) : undefined,
    platformUserId: pick('platformUserId', 'platform_user_id') != null ? Number(pick('platformUserId', 'platform_user_id')) : undefined,
    name: pick('name') != null ? String(pick('name')) : undefined,
    phone: pick('phone') != null ? String(pick('phone')) : undefined,
    idCardNumber,
    education: pick('education') != null ? String(pick('education')) : undefined,
    status,
    statusCn: pick('statusCn', 'status_cn') != null
      ? String(pick('statusCn', 'status_cn'))
      : STATUS_MAP[status ?? 0]?.label,
    age: pick('age') != null ? Number(pick('age')) : calcAgeFromIdCard(idCardNumber),
    workTypeName: pick('workTypeName', 'work_type_name') != null ? String(pick('workTypeName', 'work_type_name')) : undefined,
    level: pick('level') != null ? Number(pick('level')) : undefined,
    hasPay: pick('hasPay', 'has_pay') != null ? Number(pick('hasPay', 'has_pay')) : 0,
    createTime: pick('createTime', 'create_time') != null ? String(pick('createTime', 'create_time')) : undefined,
  };
}

function renderPeopleStatus(status?: number, statusCn?: string) {
  const label = statusCn || STATUS_MAP[status ?? 0]?.label || '待审核';
  return <span className={`status-tag status-${status ?? 0}`}>{label}</span>;
}

const SIGN_UP_FORM_INITIAL: Partial<SignUpPlanFormValues> = {
  sortNumber: 0,
};

export function TrainingInformationPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<SignUpPlanFormValues>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [modalTitle, setModalTitle] = useState('添加');
  const [editing, setEditing] = useState<TrainSignUpSelf | null>(null);
  const [peopleModalOpen, setPeopleModalOpen] = useState(false);
  const [activeSignUp, setActiveSignUp] = useState<TrainSignUpSelf | null>(null);
  const [peopleKeyword, setPeopleKeyword] = useState('');
  const [peopleStatus, setPeopleStatus] = useState<number | undefined>();
  const [peoplePage, setPeoplePage] = useState(1);
  const [selectedPeopleIds, setSelectedPeopleIds] = useState<number[]>([]);
  const [qrModal, setQrModal] = useState<QrModalState | null>(null);
  const [wxQrcodeLoading, setWxQrcodeLoading] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
  const [peopleEditOpen, setPeopleEditOpen] = useState(false);
  const [editingPersonId, setEditingPersonId] = useState<number | null>(null);
  const [importOpen, setImportOpen] = useState(false);
  const [importLoading, setImportLoading] = useState(false);
  const [templateDownloading, setTemplateDownloading] = useState(false);
  const [importStudentOpen, setImportStudentOpen] = useState(false);
  const [importStudentLoading, setImportStudentLoading] = useState(false);
  const [studentKeyword, setStudentKeyword] = useState('');
  const [studentPage, setStudentPage] = useState(1);
  const [selectedStudentIds, setSelectedStudentIds] = useState<number[]>([]);
  const [toFuxiOpen, setToFuxiOpen] = useState(false);
  const [defaultExportOpen, setDefaultExportOpen] = useState(false);
  const [defaultExportTitle, setDefaultExportTitle] = useState('');
  const [defaultExportUrl, setDefaultExportUrl] = useState('');
  const [exportElapsed, setExportElapsed] = useState(0);
  const defaultExportTimerRef = useRef<number | null>(null);
  const [orcFailOpen, setOrcFailOpen] = useState(false);
  const [orcFailData, setOrcFailData] = useState<TrainSignUpOrcSelf[]>([]);
  const [orcFailLoading, setOrcFailLoading] = useState(false);
  const [filesOpen, setFilesOpen] = useState(false);
  const [filesData, setFilesData] = useState<TrainSignUpSelfFile[]>([]);
  const [filePreviewTarget, setFilePreviewTarget] = useState<FilePreviewTarget | null>(null);

  const { data: publicData } = useQuery({
    queryKey: ['training-public-data', 'company', 'traininfo'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'traininfo']),
  });

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['sign-up-collection', page, keyword],
    queryFn: () => trainApi.pageSignUpCollection({
      page,
      pageSize: 20,
      keyword: keyword || undefined,
    }),
  });

  const { data: peopleData, isLoading: peopleLoading, refetch: refetchPeople } = useQuery({
    queryKey: ['sign-up-people', activeSignUp?.id, peoplePage, peopleKeyword, peopleStatus],
    queryFn: () => trainApi.pageSignUpPeople({
      signUpId: activeSignUp!.id,
      page: peoplePage,
      pageSize: 20,
      keyword: peopleKeyword || undefined,
      status: peopleStatus,
    }),
    enabled: !!activeSignUp?.id,
  });

  const { data: studentData, isLoading: studentLoading } = useQuery({
    queryKey: ['sign-up-import-students', studentPage, studentKeyword],
    queryFn: () => trainApi.pageSignUpImportStudents({
      page: studentPage,
      pageSize: 20,
      keyword: studentKeyword || undefined,
    }),
    enabled: importStudentOpen,
  });

  const saveMu = useMutation({
    mutationFn: (payload: TrainSignUpSelfCreateReq) =>
      editing ? trainApi.updateSignUp(editing.id, payload) : trainApi.createSignUp(payload),
    onSuccess: () => {
      message.success(editing ? '更新成功' : '创建成功');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
    },
    onError: () => {
      // 具体错误信息由 apiClient 拦截器统一提示
    },
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteSignUp(id),
    onSuccess: async () => {
      message.success('删除成功');
      setDeleteConfirmOpen(false);
      setDeleteTargetId(null);
      await queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
      refetch();
    },
  });

  const auditMu = useMutation({
    mutationFn: ({ ids, value }: { ids: number[]; value: number }) =>
      trainApi.batchUpdateSignUpPeople({ ids, field: 'status', value }),
    onSuccess: () => {
      message.success('审核完成');
      setSelectedPeopleIds([]);
      refetchPeople();
    },
  });

  const deletePeopleMu = useMutation({
    mutationFn: (ids: number[]) => trainApi.batchUpdateSignUpPeople({ ids, field: 'deleted', value: 1 }),
    onSuccess: () => {
      message.success('批量删除成功');
      setSelectedPeopleIds([]);
      refetchPeople();
    },
  });

  const updatePayMu = useMutation({
    mutationFn: ({ id, value }: { id: number; value: number }) =>
      trainApi.updateSignUpPerson(id, { hasPay: value }),
    onSuccess: () => {
      refetchPeople();
    },
    onError: () => {
      message.error('更新支付状态失败');
      refetchPeople();
    },
  });

  const planRows = useMemo(
    () => (data?.items ?? []).map((row) => normalizeSignUpPlan(row as unknown as Record<string, unknown>)),
    [data?.items],
  );

  const peopleRows = useMemo(
    () => (peopleData?.items ?? []).map((row) => normalizeSignUpPerson(row as unknown as Record<string, unknown>)),
    [peopleData?.items],
  );

  const showPlanFiles = (row: TrainSignUpSelf) => {
    setFilesData(row.files ?? []);
    setFilesOpen(true);
  };

  const closePlanFiles = () => {
    setFilesOpen(false);
    setFilesData([]);
  };

  const previewPlanFile = (file: TrainSignUpSelfFile) => {
    const fileName = file.fileName || fileNameFromUrl(file.fileUrl);
    const fileType = file.fileType || fileTypeFromPath(fileName) || fileTypeFromPath(file.fileUrl);
    setFilePreviewTarget({
      fileUrl: file.fileUrl,
      fileName,
      fileType,
    });
  };

  const closeImportStudentModal = () => {
    setImportStudentOpen(false);
    setImportStudentLoading(false);
    setStudentKeyword('');
    setStudentPage(1);
    setSelectedStudentIds([]);
  };

  const openImportStudentModal = () => {
    setStudentKeyword('');
    setStudentPage(1);
    setSelectedStudentIds([]);
    setImportStudentOpen(true);
  };

  const handleImportStudents = async () => {
    if (!activeSignUp?.id) {
      message.error('请先选择报名计划');
      return;
    }
    if (selectedStudentIds.length === 0) {
      message.error('请选择要导入的学员');
      return;
    }
    setImportStudentLoading(true);
    try {
      const result = await trainApi.importSignUpStudents(activeSignUp.id, selectedStudentIds);
      const skippedHint = result.skipped > 0 ? `，跳过 ${result.skipped} 人` : '';
      message.success(`导入成功: ${result.inserted} 人${skippedHint}`);
      closeImportStudentModal();
      setSelectedPeopleIds([]);
      refetchPeople();
    } catch {
      setImportStudentLoading(false);
    }
  };

  const closeImportModal = () => {
    setImportOpen(false);
    setImportLoading(false);
    setTemplateDownloading(false);
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
    } catch {
      message.error('模板下载失败');
    } finally {
      setTemplateDownloading(false);
    }
  };

  const handleImportUpload = async (file: File) => {
    if (!activeSignUp?.id) {
      message.error('请先选择报名计划');
      return Upload.LIST_IGNORE;
    }
    setImportLoading(true);
    try {
      const result = await trainApi.importSignUpPeople(activeSignUp.id, file);
      const skippedHint = result.skipped > 0 ? `，跳过 ${result.skipped} 人` : '';
      message.success(`导入成功，新增 ${result.inserted} 人${skippedHint}`);
      closeImportModal();
      setSelectedPeopleIds([]);
      refetchPeople();
      queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
    } catch {
      setImportLoading(false);
    }
    return Upload.LIST_IGNORE;
  };

  const openCreate = () => {
    setEditing(null);
    setModalTitle('添加');
    form.resetFields();
    form.setFieldsValue(SIGN_UP_FORM_INITIAL);
    setModalOpen(true);
  };

  const openEdit = (row: TrainSignUpSelf) => {
    setEditing(row);
    setModalTitle('修改');
    form.setFieldsValue({
      title: row.title,
      cover: row.cover,
      trainTimeRange: parseTrainTimeRange(row.trainTime),
      examTimeDate: parseExamTime(row.examTime),
      orgId: row.orgId,
      trainInfoId: row.trainInfoId,
      sortNumber: row.sortNumber,
    });
    setModalOpen(true);
  };

  const openPeople = (row: TrainSignUpSelf) => {
    setActiveSignUp(row);
    setPeoplePage(1);
    setPeopleKeyword('');
    setPeopleStatus(undefined);
    setSelectedPeopleIds([]);
    setPeopleModalOpen(true);
  };

  const closePeopleModal = () => {
    setPeopleModalOpen(false);
    setActiveSignUp(null);
    setPeoplePage(1);
    setPeopleKeyword('');
    setPeopleStatus(undefined);
    setSelectedPeopleIds([]);
    setPeopleEditOpen(false);
    setEditingPersonId(null);
  };

  const openPeopleEdit = (row: TrainSignUpPerson) => {
    setEditingPersonId(row.id);
    setPeopleEditOpen(true);
  };

  const handleExportPeople = () => {
    if (!activeSignUp?.id) return;
    const ids = selectedPeopleIds.length ? selectedPeopleIds : undefined;
    trainApi.exportSignUpPeople(activeSignUp.id, ids);
  };

  useEffect(() => {
    if (!defaultExportOpen) {
      setExportElapsed(0);
      return undefined;
    }
    const tick = window.setInterval(() => setExportElapsed((v) => v + 1), 1000);
    return () => window.clearInterval(tick);
  }, [defaultExportOpen]);

  useEffect(() => () => {
    if (defaultExportTimerRef.current != null) {
      window.clearInterval(defaultExportTimerRef.current);
    }
  }, []);

  const closeDefaultExportModal = () => {
    if (defaultExportTimerRef.current != null) {
      window.clearInterval(defaultExportTimerRef.current);
      defaultExportTimerRef.current = null;
    }
    setDefaultExportOpen(false);
    setDefaultExportTitle('');
    setDefaultExportUrl('');
    setExportElapsed(0);
  };

  const handleDefaultTemplateExport = async () => {
    if (!activeSignUp?.id) return;
    setDefaultExportOpen(true);
    setDefaultExportTitle('');
    setDefaultExportUrl('');
    setExportElapsed(0);
    if (defaultExportTimerRef.current != null) {
      window.clearInterval(defaultExportTimerRef.current);
      defaultExportTimerRef.current = null;
    }
    try {
      const res = await trainApi.startDefaultTemplateExport({
        signUpId: activeSignUp.id,
        ids: selectedPeopleIds.length ? selectedPeopleIds : undefined,
        status: peopleStatus,
      });
      setDefaultExportTitle(`当前进度：${res.jindu}`);
      defaultExportTimerRef.current = window.setInterval(async () => {
        try {
          const check = await trainApi.checkDefaultTemplateExport({
            signUpId: activeSignUp.id,
            taskId: res.taskId,
            signUpTitle: activeSignUp.title,
          });
          if (check.url) {
            if (defaultExportTimerRef.current != null) {
              window.clearInterval(defaultExportTimerRef.current);
              defaultExportTimerRef.current = null;
            }
            setDefaultExportTitle('导出成功');
            setDefaultExportUrl(check.url);
            window.open(check.url, '_blank');
            queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
          } else if (check.jindu) {
            setDefaultExportTitle(`当前进度：${check.jindu}`);
          } else if (check.error) {
            if (defaultExportTimerRef.current != null) {
              window.clearInterval(defaultExportTimerRef.current);
              defaultExportTimerRef.current = null;
            }
            setDefaultExportTitle(`导出失败，${check.msg ?? ''}`);
          }
        } catch {
          if (defaultExportTimerRef.current != null) {
            window.clearInterval(defaultExportTimerRef.current);
            defaultExportTimerRef.current = null;
          }
          setDefaultExportTitle('导出失败');
        }
      }, 1000);
    } catch {
      setDefaultExportTitle('导出失败');
    }
  };

  const handleOpenOrcFailLog = async () => {
    if (!activeSignUp?.id) return;
    setOrcFailOpen(true);
    setOrcFailLoading(true);
    try {
      const res = await trainApi.pageSignUpOrcs({ signUpId: activeSignUp.id, page: 1, pageSize: 100 });
      setOrcFailData(res.items ?? []);
    } catch {
      message.error('获取失败记录失败');
    } finally {
      setOrcFailLoading(false);
    }
  };

  const handleDelete = (row: TrainSignUpSelf) => {
    setDeleteTargetId(row.id);
    setDeleteConfirmOpen(true);
  };

  const confirmDelete = () => {
    if (deleteTargetId != null) {
      deleteMu.mutate(deleteTargetId);
    }
  };

  const showQrcode = async (row: TrainSignUpSelf) => {
    try {
      const res = await trainApi.getSignUpQrcode(row.id);
      // 优先使用服务端生成的 APP 二维码（base64），回退到 URL
      const appQrcodeUrl = res.appQrcodeDataUrl || res.appQrcode || '';
      const wxQrcodeUrl = resolveWxQrcodeDisplayUrl(res.qrcodeDataUrl, res.qrcode, row.wxMiniQrcode);
      setQrModal({
        signUpId: row.id,
        title: res.title || row.title || '',
        appQrcodeUrl,
        wxQrcodeUrl,
        path: res.registrationPath,
      });
      if (!appQrcodeUrl) {
        message.warning('APP 二维码生成失败');
      }
      if (!wxQrcodeUrl) {
        message.info('尚未生成小程序码，可点击「获取」生成');
      }
    } catch {
      message.error('获取二维码失败');
    }
  };

  const fetchWxMiniQrcode = async () => {
    if (!qrModal?.signUpId) return;
    setWxQrcodeLoading(true);
    try {
      const res = await trainApi.generateSignUpQrcode(qrModal.signUpId);
      const wxUrl = resolveWxQrcodeDisplayUrl(res.qrcodeDataUrl, res.qrcode);
      if (wxUrl) {
        setQrModal((prev) => (prev ? { ...prev, wxQrcodeUrl: wxUrl } : prev));
        queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
        message.success('小程序码已生成');
      } else {
        message.error('生成失败');
      }
    } catch {
      // 具体错误信息由 apiClient 拦截器统一提示
    } finally {
      setWxQrcodeLoading(false);
    }
  };

  const coverPreview = Form.useWatch('cover', form);
  const companies = publicData?.company_list ?? [];
  const trainInfos = publicData?.traininfo_list ?? [];
  const isPeopleChecked = selectedPeopleIds.length === 0;

  const coverUploadProps: UploadProps = {
    showUploadList: false,
    accept: '.png,.jpg,.jpeg',
    className: 'cover-uploader',
    beforeUpload: (file) => {
      const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png';
      const isLt2M = file.size / 1024 / 1024 < 2;
      if (!isJPGorPNG) message.error('上传封面图片只能是 JPG/PNG 格式!');
      if (!isLt2M) message.error('上传封面图片大小不能超过 2MB!');
      return isJPGorPNG && isLt2M;
    },
    customRequest: async ({ file, onSuccess, onError }) => {
      try {
        const result = await uploadFile(file as File, 'public');
        const url = `/api/files/${result.id}/download`;
        form.setFieldValue('cover', url);
        message.success('上传成功');
        onSuccess?.(result);
      } catch {
        message.error('上传失败');
        onError?.(new Error('upload failed'));
      }
    },
  };

  const planColumns: ColumnsType<TrainSignUpSelf> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 90,
      onCell: () => ({ style: { whiteSpace: 'nowrap' } }),
      onHeaderCell: () => ({ style: { whiteSpace: 'nowrap' } }),
    },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '封面', dataIndex: 'cover', width: 90,
      render: (v: string) => v ? (
        <div className="cover-table-cell">
          <AuthImage url={v} alt="cover" className="cover-table-thumb" />
        </div>
      ) : '-',
    },
    { title: '培训时间', dataIndex: 'trainTime', width: 140 },
    { title: '考试时间', dataIndex: 'examTime', width: 140 },
    {
      title: '绑定公司',
      dataIndex: 'orgName',
      width: 220,
      onCell: () => ({ style: { whiteSpace: 'nowrap' } }),
    },
    { title: '排序号', dataIndex: 'sortNumber', width: 90 },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', key: 'act', width: 380,
      render: (_, row) => (
        <Space size={0} wrap>
          <Button type="link" size="small" className="operate-btn" onClick={() => showQrcode(row)}>二维码</Button>
          <Button type="link" size="small" className="operate-btn" onClick={() => openPeople(row)}>报名列表</Button>
          <Button type="link" size="small" className="operate-btn" onClick={() => openEdit(row)}>修改</Button>
          <Button type="link" size="small" className="operate-btn operate-btn-danger" onClick={() => handleDelete(row)}>删除</Button>
          {(row.files?.length ?? 0) > 0 && (
            <Button type="link" size="small" className="operate-btn" onClick={() => showPlanFiles(row)}>查看文件</Button>
          )}
        </Space>
      ),
    },
  ];

  const peopleColumns: ColumnsType<TrainSignUpPerson> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 90,
      onCell: () => ({ style: { whiteSpace: 'nowrap' } }),
      onHeaderCell: () => ({ style: { whiteSpace: 'nowrap' } }),
    },
    { title: '用户ID', dataIndex: 'platformUserId' },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '身份证', dataIndex: 'idCardNumber', width: 180 },
    { title: '年龄', dataIndex: 'age', width: 70, render: (v: number | undefined) => v ?? '' },
    { title: '工种', dataIndex: 'workTypeName', render: (v: string) => v || '' },
    { title: '工种等级', dataIndex: 'level', width: 120, render: (v: number | undefined) => (v != null ? (LEVEL_LABELS[v] ?? v) : '') },
    { title: '报名时间', dataIndex: 'createTime', width: 160 },
    {
      title: '审核状态', dataIndex: 'status', width: 100,
      render: (v: number, row) => renderPeopleStatus(v, row.statusCn),
    },
    {
      title: '是否已支付', dataIndex: 'hasPay', width: 110,
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
      title: '操作', key: 'act', width: 80,
      render: (_, row) => (
        <Button type="link" size="small" className="operate-btn" onClick={() => openPeopleEdit(row)}>
          编辑
        </Button>
      ),
    },
  ];

  const orcFailColumns: ColumnsType<TrainSignUpOrcSelf> = [
    { title: '时间', dataIndex: 'createTime', width: 160 },
    { title: '用户ID', dataIndex: 'platformUserId', width: 100 },
    { title: '状态', dataIndex: 'status', width: 80, render: (v: number) => (v === 1 ? '成功' : '失败') },
    { title: '失败信息', dataIndex: 'failReason', ellipsis: true },
    { title: '识别数据', dataIndex: 'orcData', ellipsis: true },
  ];

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      saveMu.mutate(toSignUpPayload(values));
    } catch {
      // 表单校验失败
    }
  };

  return (
    <div className="training-list-container">
      <section className="page-heading" style={{ marginBottom: 16 }}>
        <Typography.Title level={4}>信息收集</Typography.Title>
        <Typography.Text type="secondary">创建报名计划、生成扫码二维码，收集学员报名资料</Typography.Text>
      </section>

      <TrainingProcessSteps current={5} />

      <div className="top-bar">
        <div>
          <Input
            placeholder="标题搜索"
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(1); }}
            style={{ width: 200 }}
            allowClear
          />
          <Button type="primary" style={{ marginLeft: 10 }} onClick={openCreate}>+ 添加</Button>
        </div>
      </div>

      <Table
        rowKey="id"
        loading={isLoading}
        columns={planColumns}
        dataSource={planRows}
        scroll={{ y: 670, x: 1200 }}
        pagination={false}
      />

      <div className="pagination-container">
        <Pagination
          current={page}
          pageSize={20}
          total={data?.total ?? 0}
          onChange={setPage}
          showTotal={(total) => `共 ${total} 条`}
          showSizeChanger={false}
        />
      </div>

      <Modal
        title={modalTitle}
        open={modalOpen}
        width="50%"
        onCancel={() => { setModalOpen(false); setEditing(null); form.resetFields(); }}
        onOk={handleSave}
        confirmLoading={saveMu.isPending}
        destroyOnHidden
        okText="确认"
        cancelText="取消"
      >
        <Form
          form={form}
          layout="horizontal"
          className="sign-up-form"
          labelCol={{ flex: '120px' }}
          wrapperCol={{ flex: 1 }}
          colon={false}
          initialValues={SIGN_UP_FORM_INITIAL}
        >
          <Form.Item
            name="title"
            label="标题"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input placeholder="请输入标题" maxLength={255} />
          </Form.Item>

          <Form.Item
            name="cover"
            label="封面"
            getValueFromEvent={(e) => {
              // antd Upload 默认把 fileList 提交给表单，里头是对象数组；
              // 这里抽出我们需要的 URL 字符串，避免后端 JSON 反序列化失败
              const fileList = Array.isArray(e?.fileList) ? e.fileList : [];
              const last = fileList[fileList.length - 1];
              if (!last) return '';
              if (typeof last.url === 'string' && last.url) return last.url;
              if (typeof last.thumbUrl === 'string' && last.thumbUrl) return last.thumbUrl;
              if (last.response && typeof last.response === 'object' && last.response.id) {
                return `/api/files/${last.response.id}/download`;
              }
              return '';
            }}
          >
            <Upload {...coverUploadProps}>
              {coverPreview ? (
                <AuthImage
                  url={coverPreview}
                  alt="cover"
                  className="cover"
                  style={{ width: 178, height: 178, display: 'block', objectFit: 'cover' }}
                />
              ) : (
                <div className="cover-uploader-icon">
                  <PlusOutlined />
                </div>
              )}
            </Upload>
          </Form.Item>

          <Form.Item
            name="trainTimeRange"
            label="培训时间"
            rules={[{ required: true, message: '请选择培训时间' }]}
          >
            <RangePicker
              style={{ width: 400 }}
              format={DATE_FORMAT}
              placeholder={['开始日期', '结束日期']}
            />
          </Form.Item>

          <Form.Item
            name="examTimeDate"
            label="考试时间"
            rules={[{ required: true, message: '请选择考试时间' }]}
          >
            <DatePicker
              style={{ width: 400 }}
              format={DATE_FORMAT}
              placeholder="选择考试日期"
            />
          </Form.Item>

          <Form.Item
            name="orgId"
            label="公司名称"
            rules={[{ required: true, message: '请选择公司名称' }]}
          >
            <Select
              allowClear
              showSearch
              placeholder="请选择"
              style={{ width: 400 }}
              filterOption={(input, option) => (option?.label as string || '').includes(input)}
              options={companies.map((c) => ({ value: c.id, label: c.title }))}
            />
          </Form.Item>

          <Form.Item name="trainInfoId" label="培训信息">
            <Select
              allowClear
              showSearch
              placeholder="请选择培训信息"
              style={{ width: 400 }}
              filterOption={(input, option) => (option?.label as string || '').includes(input)}
              options={trainInfos.map((t) => ({ value: t.id, label: t.title }))}
            />
          </Form.Item>

          <Form.Item name="sortNumber" label="排序">
            <InputNumber min={0} precision={0} style={{ width: 400 }} placeholder="0" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="友情提示"
        open={deleteConfirmOpen}
        width="50%"
        onCancel={() => { setDeleteConfirmOpen(false); setDeleteTargetId(null); }}
        onOk={confirmDelete}
        confirmLoading={deleteMu.isPending}
        okText="确定"
        cancelText="取消"
      >
        确定要删除选中项吗?
      </Modal>

      <Modal
        title="报名二维码"
        open={!!qrModal}
        onCancel={() => { setQrModal(null); setWxQrcodeLoading(false); }}
        footer={null}
        width={360}
        centered
        maskClosable={false}
        keyboard={false}
      >
        {qrModal && (
          <div className="qrcode-modal-body">
            <div className="qrcode-title">{qrModal.title}</div>
            <div className="qrcode-row">
              <div className="qrcode-col">
                <Image src={qrModal.appQrcodeUrl} width={120} height={120} style={{ objectFit: 'fill' }} preview={false} />
                <div className="text-C">APP扫</div>
              </div>
              <div className="qrcode-col">
                {qrModal.wxQrcodeUrl ? (
                  <>
                    <Image src={qrModal.wxQrcodeUrl} width={120} height={120} style={{ objectFit: 'fill' }} />
                    <div className="text-C">微信扫</div>
                  </>
                ) : (
                  <>
                    <div className="wx-qrcode-placeholder">
                      <Button type="primary" loading={wxQrcodeLoading} onClick={fetchWxMiniQrcode}>
                        获取
                      </Button>
                    </div>
                    <div className="text-C">微信扫</div>
                  </>
                )}
              </div>
            </div>
          </div>
        )}
      </Modal>

      <Modal
        title="报名人员列表"
        open={peopleModalOpen}
        width="80%"
        centered
        onCancel={closePeopleModal}
        footer={null}
        destroyOnHidden
      >
        <div className="people-modal-body">
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
              placeholder="请选择状态"
              value={peopleStatus}
              onChange={(v) => { setPeopleStatus(v); setPeoplePage(1); }}
              options={[
                { value: 0, label: '待审核' },
                { value: 1, label: '已通过' },
              ]}
            />
          </div>

          <div className="tools">
            <Space wrap size={[8, 8]}>
              <Button
                type={isPeopleChecked ? 'default' : 'primary'}
                disabled={isPeopleChecked}
                onClick={() => auditMu.mutate({ ids: selectedPeopleIds, value: 1 })}
              >
                批量通过
              </Button>
              <Button
                type={isPeopleChecked ? 'default' : 'primary'}
                danger={!isPeopleChecked}
                disabled={isPeopleChecked}
                onClick={() => auditMu.mutate({ ids: selectedPeopleIds, value: 2 })}
              >
                批量拒绝
              </Button>
              <Button
                type={isPeopleChecked ? 'default' : 'primary'}
                danger={!isPeopleChecked}
                disabled={isPeopleChecked}
                onClick={() => deletePeopleMu.mutate(selectedPeopleIds)}
              >
                批量删除
              </Button>
              <Button type="primary" onClick={handleDefaultTemplateExport}>
                默认模板导出
              </Button>
              <Button type="primary" disabled={!activeSignUp?.id} onClick={handleExportPeople}>
                仅导出名单
              </Button>
              <Button type="primary" onClick={() => setImportOpen(true)}>
                批量导入
              </Button>
              <Button type="primary" onClick={openImportStudentModal}>
                从学员中导入
              </Button>
              <Button type="primary" onClick={() => setToFuxiOpen(true)}>
                批量加入复习计划
              </Button>
              <Button type="primary" onClick={handleOpenOrcFailLog}>
                获取身份证识别失败记录
              </Button>
            </Space>
          </div>

          <Table
            rowKey="id"
            loading={peopleLoading}
            columns={peopleColumns}
            dataSource={peopleRows}
            scroll={{ y: 450, x: 1400 }}
            pagination={false}
            rowSelection={{
              selectedRowKeys: selectedPeopleIds,
              onChange: (keys) => setSelectedPeopleIds(keys as number[]),
            }}
          />

          <div className="pagination-container" style={{ height: 60 }}>
            <Pagination
              current={peoplePage}
              pageSize={20}
              total={peopleData?.total ?? 0}
              onChange={setPeoplePage}
              showTotal={(total) => `共 ${total} 条`}
              showSizeChanger={false}
            />
          </div>
        </div>
      </Modal>

      <SignUpPeopleEditModal
        open={peopleEditOpen}
        personId={editingPersonId}
        onClose={() => { setPeopleEditOpen(false); setEditingPersonId(null); }}
        onSaved={() => {
          setPeopleEditOpen(false);
          setEditingPersonId(null);
          refetchPeople();
        }}
      />

      <Modal
        title="批量导入报名人员"
        open={importOpen}
        width="40%"
        onCancel={closeImportModal}
        footer={null}
        destroyOnHidden
        maskClosable={false}
      >
        <Spin spinning={importLoading}>
          <Upload.Dragger
            accept=".xlsx,.xls"
            maxCount={1}
            disabled={importLoading}
            showUploadList={false}
            beforeUpload={(file) => {
              handleImportUpload(file as File);
              return false;
            }}
          >
            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
            <p className="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
            <p className="ant-upload-hint" style={{ color: '#ccc', marginTop: 10 }}>提示:上传过程中,请不要做任何操作</p>
            <p className="ant-upload-hint">
              只能上传xlsx/xls文件{' '}
              <a href="#" onClick={handleDownloadImportTemplate}>
                {templateDownloading ? '下载中...' : '下载模板'}
              </a>
            </p>
          </Upload.Dragger>
        </Spin>
      </Modal>

      <Modal
        title="批量导入报名人员"
        open={importStudentOpen}
        width="50%"
        onCancel={closeImportStudentModal}
        footer={null}
        destroyOnHidden
        maskClosable={false}
      >
        <div className="tools">
          <Input
            placeholder="关键字搜索"
            value={studentKeyword}
            onChange={(e) => { setStudentKeyword(e.target.value); setStudentPage(1); }}
            style={{ width: 200, marginRight: 10 }}
            allowClear
          />
          <Button type="primary" loading={importStudentLoading} onClick={handleImportStudents}>
            导入选中学员
          </Button>
        </div>
        <Table
          rowKey="id"
          loading={studentLoading}
          columns={[
            { title: 'ID', dataIndex: 'id', width: 100 },
            { title: '身份证号', dataIndex: 'idcard', width: 180 },
            { title: '姓名', dataIndex: 'realname', width: 100 },
            { title: '手机号', dataIndex: 'phone', width: 120 },
            { title: '所属企业', dataIndex: 'companyName', render: (v: string) => v || '-' },
          ]}
          dataSource={studentData?.items ?? []}
          scroll={{ y: 350 }}
          pagination={{
            current: studentPage,
            pageSize: 20,
            total: studentData?.total ?? 0,
            onChange: setStudentPage,
            showSizeChanger: false,
            showTotal: (total) => `共 ${total} 条`,
          }}
          rowSelection={{
            selectedRowKeys: selectedStudentIds,
            onChange: (keys) => setSelectedStudentIds(keys as number[]),
            getCheckboxProps: (row: PlatformUser) => ({
              disabled: !row.idcard,
            }),
          }}
        />
      </Modal>

      <Modal
        title="批量加入复习计划"
        open={toFuxiOpen}
        width="40%"
        onCancel={() => setToFuxiOpen(false)}
        onOk={() => message.info('批量加入复习计划功能正在对接中')}
        okText="确认"
        cancelText="取消"
      >
        <Form layout="horizontal" labelCol={{ flex: '120px' }} wrapperCol={{ flex: 1 }} colon={false}>
          <Form.Item label="考试/复习计划">
            <Select placeholder="请选择计划" style={{ width: 400 }} disabled options={[]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="默认模板导出"
        open={defaultExportOpen}
        width="40%"
        onCancel={closeDefaultExportModal}
        footer={null}
        maskClosable={false}
      >
        <div>
          {defaultExportTitle ? (
            <div>
              {defaultExportTitle}
              {defaultExportUrl ? (
                <>
                  {' '}
                  <a href={defaultExportUrl} target="_blank" rel="noreferrer">下载</a>
                </>
              ) : null}
            </div>
          ) : (
            <div>
              默认模板正在导出，请稍后
              {exportElapsed > 0 ? `${exportElapsed}秒` : ''}
            </div>
          )}
        </div>
      </Modal>

      <Modal
        title="查看文件"
        open={filesOpen}
        width="40%"
        onCancel={closePlanFiles}
        footer={null}
        destroyOnHidden
        maskClosable={false}
      >
        <Table
          rowKey={(row, index) => `${row.fileUrl}-${index}`}
          columns={[
            {
              title: '文件名',
              dataIndex: 'fileUrl',
              ellipsis: true,
              render: (url: string) => fileNameFromUrl(url) || url,
            },
            {
              title: '操作',
              width: 80,
              render: (_, row) => (
                <Button type="link" size="small" className="operate-btn" onClick={() => previewPlanFile(row)}>
                  预览
                </Button>
              ),
            },
          ]}
          dataSource={filesData}
          scroll={{ y: 350 }}
          pagination={false}
          bordered
        />
      </Modal>

      <FilePreviewModal
        open={Boolean(filePreviewTarget)}
        target={filePreviewTarget}
        onClose={() => setFilePreviewTarget(null)}
      />

      <Modal
        title="身份证识别失败记录"
        open={orcFailOpen}
        width="70%"
        onCancel={() => { setOrcFailOpen(false); setOrcFailData([]); }}
        footer={null}
        destroyOnHidden
      >
        <Table
          rowKey="id"
          loading={orcFailLoading}
          columns={orcFailColumns}
          dataSource={orcFailData}
          scroll={{ y: 350 }}
          pagination={false}
        />
      </Modal>
    </div>
  );
}
