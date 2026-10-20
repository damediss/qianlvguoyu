import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMemo, useState, useRef, useEffect } from 'react';
import { Button, DatePicker, Form, Image, Input, InputNumber, Modal, Pagination, Select, Space, Spin, Switch, Table, Typography, Upload, message, } from 'antd';
import { InboxOutlined, PlusOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import { FilePreviewModal } from '../../components/FilePreviewModal';
import { AuthImage } from '../../components/ApiAuthImage';
import { fileNameFromUrl, downloadExportFile } from '../../api/files';
import { SignUpPeopleEditModal } from './SignUpPeopleEditModal';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import { usePaneActive, usePaneActiveRef } from '../../layouts/TabKeepAlive';
import './TrainingInformationPage.css';
const { RangePicker } = DatePicker;
const DATE_FORMAT = 'YYYY-MM-DD';
const APP_SIGN_UP_H5_URL = 'https://h5.gjjd168.com/#/pages/signUp/self';
function buildAppSignUpQrcodeUrl(signUpId) {
    return `${APP_SIGN_UP_H5_URL}?id=${signUpId}`;
}
/** 优先使用后端返回的 base64，避免 CDN 静态资源访问问题 */
function resolveWxQrcodeDisplayUrl(qrcodeDataUrl, qrcodeUrl, fallbackUrl) {
    if (qrcodeDataUrl)
        return qrcodeDataUrl;
    const url = qrcodeUrl || fallbackUrl || '';
    return toProxiedCdnUrl(url);
}
/** 将 CDN 绝对地址转为同源相对路径，便于 dev 代理与 img 加载 */
function fileTypeFromPath(path) {
    if (!path || !path.includes('.'))
        return '';
    const ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
    const slash = ext.indexOf('/');
    return slash >= 0 ? ext.substring(0, slash) : ext;
}
function toProxiedCdnUrl(url) {
    if (!url)
        return url;
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
function parseTrainTimeRange(value) {
    if (!value?.trim())
        return undefined;
    const matches = value.match(/\d{4}-\d{2}-\d{2}/g);
    if (!matches?.length)
        return undefined;
    const start = dayjs(matches[0], DATE_FORMAT);
    const end = dayjs(matches[matches.length - 1], DATE_FORMAT);
    if (!start.isValid() || !end.isValid())
        return undefined;
    return start.isAfter(end) ? [end, start] : [start, end];
}
function parseExamTime(value) {
    if (!value?.trim())
        return undefined;
    const match = value.match(/\d{4}-\d{2}-\d{2}/);
    if (!match)
        return undefined;
    const date = dayjs(match[0], DATE_FORMAT);
    return date.isValid() ? date : undefined;
}
function formatTrainTimeRange(range) {
    if (!range?.[0] || !range[1])
        return '';
    return `${range[0].format(DATE_FORMAT)} ~ ${range[1].format(DATE_FORMAT)}`;
}
function toSignUpPayload(values) {
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
const STATUS_MAP = {
    0: { label: '待审核' },
    1: { label: '已通过' },
    2: { label: '未通过' },
};
const LEVEL_LABELS = ['五级/初级工', '四级/中级工', '三级/高级工', '二级/技师', '一级/高级技师'];
function calcAgeFromIdCard(idCard) {
    if (!idCard)
        return undefined;
    let birth = '';
    if (idCard.length === 18)
        birth = idCard.slice(6, 14);
    else if (idCard.length === 15)
        birth = `19${idCard.slice(6, 12)}`;
    if (!birth)
        return undefined;
    const year = Number(birth.slice(0, 4));
    const month = Number(birth.slice(4, 6));
    const day = Number(birth.slice(6, 8));
    if (!year || !month || !day)
        return undefined;
    const today = dayjs();
    let age = today.year() - year;
    if (today.month() + 1 < month || (today.month() + 1 === month && today.date() < day))
        age -= 1;
    return age >= 0 ? age : undefined;
}
function normalizeSignUpPlanFiles(raw) {
    if (!Array.isArray(raw)) {
        return [];
    }
    return raw
        .map((item) => {
        if (typeof item === 'string' && item) {
            return { fileUrl: item };
        }
        if (item && typeof item === 'object') {
            const row = item;
            const fileUrl = String(row.fileUrl ?? row.file_url ?? '');
            if (!fileUrl)
                return null;
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
        .filter((item) => item != null);
}
function normalizeSignUpPlan(raw) {
    return {
        ...raw,
        files: normalizeSignUpPlanFiles(raw.files),
    };
}
function normalizeSignUpPerson(raw) {
    const pick = (...keys) => {
        for (const key of keys) {
            const v = raw[key];
            if (v !== undefined && v !== null && v !== '')
                return v;
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
function renderPeopleStatus(status, statusCn) {
    const label = statusCn || STATUS_MAP[status ?? 0]?.label || '待审核';
    return _jsx("span", { className: `status-tag status-${status ?? 0}`, children: label });
}
const SIGN_UP_FORM_INITIAL = {
    sortNumber: 0,
};
export function TrainingInformationPage() {
    const queryClient = useQueryClient();
    const paneActiveRef = usePaneActiveRef(usePaneActive()); // 标签不可见时暂停导出计时/轮询 setState（§3.6）
    const [form] = Form.useForm();
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const [modalOpen, setModalOpen] = useState(false);
    const [modalTitle, setModalTitle] = useState('添加');
    const [editing, setEditing] = useState(null);
    const [peopleModalOpen, setPeopleModalOpen] = useState(false);
    const [activeSignUp, setActiveSignUp] = useState(null);
    const [peopleKeyword, setPeopleKeyword] = useState('');
    const [peopleStatus, setPeopleStatus] = useState();
    const [peoplePage, setPeoplePage] = useState(1);
    const [selectedPeopleIds, setSelectedPeopleIds] = useState([]);
    const [qrModal, setQrModal] = useState(null);
    const [wxQrcodeLoading, setWxQrcodeLoading] = useState(false);
    const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
    const [deleteTargetId, setDeleteTargetId] = useState(null);
    const [peopleEditOpen, setPeopleEditOpen] = useState(false);
    const [editingPersonId, setEditingPersonId] = useState(null);
    const [importOpen, setImportOpen] = useState(false);
    const [importLoading, setImportLoading] = useState(false);
    const [templateDownloading, setTemplateDownloading] = useState(false);
    const [importStudentOpen, setImportStudentOpen] = useState(false);
    const [importStudentLoading, setImportStudentLoading] = useState(false);
    const [importByIdCardOpen, setImportByIdCardOpen] = useState(false);
    const [importByIdCardLoading, setImportByIdCardLoading] = useState(false);
    const [idCardText, setIdCardText] = useState('');
    const [studentKeyword, setStudentKeyword] = useState('');
    const [studentPage, setStudentPage] = useState(1);
    const [selectedStudentIds, setSelectedStudentIds] = useState([]);
    const [toFuxiOpen, setToFuxiOpen] = useState(false);
    const [defaultExportOpen, setDefaultExportOpen] = useState(false);
    const [defaultExportTitle, setDefaultExportTitle] = useState('');
    const [defaultExportUrl, setDefaultExportUrl] = useState('');
    const [exportElapsed, setExportElapsed] = useState(0);
    const defaultExportTimerRef = useRef(null);
    const defaultExportGenRef = useRef(0);
    const [orcFailOpen, setOrcFailOpen] = useState(false);
    const [orcFailData, setOrcFailData] = useState([]);
    const [orcFailLoading, setOrcFailLoading] = useState(false);
    const [filesOpen, setFilesOpen] = useState(false);
    const [filesData, setFilesData] = useState([]);
    const [filePreviewTarget, setFilePreviewTarget] = useState(null);
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
            signUpId: activeSignUp.id,
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
        mutationFn: (payload) => editing ? trainApi.updateSignUp(editing.id, payload) : trainApi.createSignUp(payload),
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
        mutationFn: (id) => trainApi.deleteSignUp(id),
        onSuccess: async () => {
            message.success('删除成功');
            setDeleteConfirmOpen(false);
            setDeleteTargetId(null);
            await queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
            refetch();
        },
    });
    const auditMu = useMutation({
        mutationFn: ({ ids, value }) => trainApi.batchUpdateSignUpPeople({ ids, field: 'status', value }),
        onSuccess: () => {
            message.success('审核完成');
            setSelectedPeopleIds([]);
            refetchPeople();
        },
    });
    const deletePeopleMu = useMutation({
        mutationFn: (ids) => trainApi.batchUpdateSignUpPeople({ ids, field: 'deleted', value: 1 }),
        onSuccess: (updated) => {
            if (!updated) {
                message.warning('未删除任何人员，请刷新后重试');
                return;
            }
            message.success(`已删除 ${updated} 人`);
            setSelectedPeopleIds([]);
            refetchPeople();
        },
        onError: (err) => {
            const msg = err instanceof Error && err.message ? err.message : '批量删除失败';
            message.error(msg);
        },
    });
    const handleBatchDeletePeople = () => {
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
        mutationFn: ({ id, value }) => trainApi.updateSignUpPerson(id, { hasPay: value }),
        onSuccess: () => {
            refetchPeople();
        },
        onError: () => {
            message.error('更新支付状态失败');
            refetchPeople();
        },
    });
    const planRows = useMemo(() => (data?.items ?? []).map((row) => normalizeSignUpPlan(row)), [data?.items]);
    const peopleRows = useMemo(() => (peopleData?.items ?? []).map((row) => normalizeSignUpPerson(row)), [peopleData?.items]);
    const showPlanFiles = (row) => {
        setFilesData(row.files ?? []);
        setFilesOpen(true);
    };
    const closePlanFiles = () => {
        setFilesOpen(false);
        setFilesData([]);
    };
    const previewPlanFile = (file) => {
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
            const sync = result.platformUserSync;
            const syncHint = sync
                ? `，App账号就绪 ${sync.loginReady ?? 0}（新建 ${sync.created ?? 0}/复用 ${sync.reused ?? 0}）`
                : '';
            const skippedHint = result.skipped > 0 ? `，跳过重复 ${result.skipped} 人` : '';
            const errHint = result.errors?.length ? `；注意：${result.errors.slice(0, 2).join('；')}` : '';
            message.success(`导入成功: ${result.inserted} 人${skippedHint}${syncHint}${errHint}`);
            closeImportStudentModal();
            setSelectedPeopleIds([]);
            refetchPeople();
        }
        catch {
            setImportStudentLoading(false);
        }
    };
    const handleImportStudentsByIdCard = async () => {
        if (!activeSignUp?.id) {
            message.error('请先选择报名计划');
            return;
        }
        if (!idCardText.trim()) {
            message.error('请输入身份证号');
            return;
        }
        setImportByIdCardLoading(true);
        try {
            const result = await trainApi.importSignUpStudentsByIdCard(activeSignUp.id, idCardText);
            const sync = result.platformUserSync;
            const syncHint = sync
                ? `，App账号就绪 ${sync.loginReady ?? 0}（新建 ${sync.created ?? 0}/复用 ${sync.reused ?? 0}）`
                : '';
            const skippedHint = result.skipped > 0 ? `，跳过重复 ${result.skipped} 人` : '';
            const errHint = result.errors?.length ? `；${result.errors.slice(0, 3).join('；')}` : '';
            if (result.inserted > 0) {
                message.success(`导入成功: ${result.inserted} 人${skippedHint}${syncHint}${errHint}`);
            }
            else if (result.errors?.length) {
                message.warning(`未导入新人${skippedHint}${errHint}`);
            }
            else {
                message.success(`导入完成${skippedHint}${syncHint}`);
            }
            setImportByIdCardOpen(false);
            setIdCardText('');
            closeImportStudentModal();
            setSelectedPeopleIds([]);
            refetchPeople();
        }
        catch {
            // interceptor
        }
        finally {
            setImportByIdCardLoading(false);
        }
    };
    const closeImportModal = () => {
        setImportOpen(false);
        setImportLoading(false);
        setTemplateDownloading(false);
    };
    const handleDownloadImportTemplate = async (event) => {
        event.preventDefault();
        event.stopPropagation();
        if (templateDownloading)
            return;
        setTemplateDownloading(true);
        try {
            const blob = await trainApi.downloadSignUpPeopleImportTemplate();
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = '报名批量导入模板.xlsx';
            anchor.click();
            URL.revokeObjectURL(url);
        }
        catch {
            message.error('模板下载失败');
        }
        finally {
            setTemplateDownloading(false);
        }
    };
    const handleImportUpload = async (file) => {
        if (!activeSignUp?.id) {
            message.error('请先选择报名计划');
            return Upload.LIST_IGNORE;
        }
        setImportLoading(true);
        try {
            const result = await trainApi.importSignUpPeople(activeSignUp.id, file);
            const sync = result.platformUserSync;
            const syncHint = sync
                ? `，App账号就绪 ${sync.loginReady ?? 0}（新建 ${sync.created ?? 0}/复用 ${sync.reused ?? 0}）`
                : '';
            const skippedHint = result.skipped > 0 ? `，跳过重复 ${result.skipped} 人` : '';
            const errHint = result.errors?.length ? `；注意：${result.errors.slice(0, 2).join('；')}` : '';
            message.success(`导入成功: ${result.inserted} 人${skippedHint}${syncHint}${errHint}`);
            closeImportModal();
            setSelectedPeopleIds([]);
            refetchPeople();
            queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
        }
        catch {
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
    const openEdit = (row) => {
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
    const openPeople = (row) => {
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
    const openPeopleEdit = (row) => {
        setEditingPersonId(row.id);
        setPeopleEditOpen(true);
    };
    const handleExportPeople = () => {
        if (!activeSignUp?.id)
            return;
        const ids = selectedPeopleIds.length ? selectedPeopleIds : undefined;
        trainApi.exportSignUpPeople(activeSignUp.id, ids);
    };
    useEffect(() => {
        if (!defaultExportOpen) {
            setExportElapsed(0);
            return undefined;
        }
        const tick = window.setInterval(() => {
            // 标签不可见时计时继续、跳过 setState（§3.6 乙组口径）
            if (paneActiveRef.current)
                setExportElapsed((v) => v + 1);
        }, 1000);
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
        if (!activeSignUp?.id)
            return;
        const gen = ++defaultExportGenRef.current;
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
            // await 返回后检查是否已被新一次点击取代
            if (gen !== defaultExportGenRef.current) {
                return;
            }
            setDefaultExportTitle(`当前进度：${res.jindu}`);
            let pollCount = 0;
            defaultExportTimerRef.current = window.setInterval(async () => {
                if (gen !== defaultExportGenRef.current) {
                    if (defaultExportTimerRef.current != null) {
                        window.clearInterval(defaultExportTimerRef.current);
                        defaultExportTimerRef.current = null;
                    }
                    return;
                }
                if (!paneActiveRef.current)
                    return; // 标签不可见：跳过本轮进度请求，服务端导出照常进行
                pollCount += 1;
                try {
                    const check = await trainApi.checkDefaultTemplateExport({
                        signUpId: activeSignUp.id,
                        taskId: res.taskId,
                        signUpTitle: activeSignUp.title,
                    });
                    if (gen !== defaultExportGenRef.current) {
                        return;
                    }
                    if (check.url) {
                        if (defaultExportTimerRef.current != null) {
                            window.clearInterval(defaultExportTimerRef.current);
                            defaultExportTimerRef.current = null;
                        }
                        setDefaultExportTitle('导出成功');
                        const downloadUrl = toProxiedCdnUrl(check.url);
                        setDefaultExportUrl(downloadUrl);
                        downloadExportFile(downloadUrl);
                        queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
                    }
                    else if (check.error && !String(check.msg ?? '').includes('未找到压缩包')) {
                        if (defaultExportTimerRef.current != null) {
                            window.clearInterval(defaultExportTimerRef.current);
                            defaultExportTimerRef.current = null;
                        }
                        setDefaultExportTitle(`导出失败，${check.msg ?? ''}`);
                    }
                    else if (check.jindu) {
                        setDefaultExportTitle(`当前进度：${check.jindu}`);
                    }
                    if (pollCount >= 180 && defaultExportTimerRef.current != null) {
                        window.clearInterval(defaultExportTimerRef.current);
                        defaultExportTimerRef.current = null;
                        setDefaultExportTitle('导出超时，请稍后重试');
                    }
                }
                catch {
                    if (gen === defaultExportGenRef.current) {
                        if (defaultExportTimerRef.current != null) {
                            window.clearInterval(defaultExportTimerRef.current);
                            defaultExportTimerRef.current = null;
                        }
                        setDefaultExportTitle('导出失败');
                    }
                }
            }, 1000);
        }
        catch {
            if (gen === defaultExportGenRef.current) {
                setDefaultExportTitle('导出失败');
            }
        }
    };
    const handleOpenOrcFailLog = async () => {
        if (!activeSignUp?.id)
            return;
        setOrcFailOpen(true);
        setOrcFailLoading(true);
        try {
            const res = await trainApi.pageSignUpOrcs({ signUpId: activeSignUp.id, page: 1, pageSize: 100 });
            setOrcFailData(res.items ?? []);
        }
        catch {
            message.error('获取失败记录失败');
        }
        finally {
            setOrcFailLoading(false);
        }
    };
    const handleDelete = (row) => {
        setDeleteTargetId(row.id);
        setDeleteConfirmOpen(true);
    };
    const confirmDelete = () => {
        if (deleteTargetId != null) {
            deleteMu.mutate(deleteTargetId);
        }
    };
    const showQrcode = async (row) => {
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
        }
        catch {
            message.error('获取二维码失败');
        }
    };
    const fetchWxMiniQrcode = async () => {
        if (!qrModal?.signUpId)
            return;
        setWxQrcodeLoading(true);
        try {
            const res = await trainApi.generateSignUpQrcode(qrModal.signUpId);
            const wxUrl = resolveWxQrcodeDisplayUrl(res.qrcodeDataUrl, res.qrcode);
            if (wxUrl) {
                setQrModal((prev) => (prev ? { ...prev, wxQrcodeUrl: wxUrl } : prev));
                queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
                message.success('小程序码已生成');
            }
            else {
                message.error('生成失败');
            }
        }
        catch {
            // 具体错误信息由 apiClient 拦截器统一提示
        }
        finally {
            setWxQrcodeLoading(false);
        }
    };
    const coverPreview = Form.useWatch('cover', form);
    const companies = publicData?.company_list ?? [];
    const trainInfos = publicData?.traininfo_list ?? [];
    const isPeopleChecked = selectedPeopleIds.length === 0;
    const coverUploadProps = {
        showUploadList: false,
        accept: '.png,.jpg,.jpeg',
        className: 'cover-uploader',
        beforeUpload: (file) => {
            const isJPGorPNG = file.type === 'image/jpeg' || file.type === 'image/png';
            const isLt2M = file.size / 1024 / 1024 < 2;
            if (!isJPGorPNG)
                message.error('上传封面图片只能是 JPG/PNG 格式!');
            if (!isLt2M)
                message.error('上传封面图片大小不能超过 2MB!');
            return isJPGorPNG && isLt2M;
        },
        customRequest: async ({ file, onSuccess, onError }) => {
            try {
                const result = await uploadFile(file, 'public');
                const url = `/api/files/${result.id}/download`;
                form.setFieldValue('cover', url);
                message.success('上传成功');
                onSuccess?.(result);
            }
            catch {
                message.error('上传失败');
                onError?.(new Error('upload failed'));
            }
        },
    };
    const planColumns = [
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
            render: (v) => v ? (_jsx("div", { className: "cover-table-cell", children: _jsx(AuthImage, { url: v, alt: "cover", className: "cover-table-thumb" }) })) : '-',
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
            render: (_, row) => (_jsxs(Space, { size: 0, wrap: true, children: [_jsx(Button, { type: "link", size: "small", className: "operate-btn", onClick: () => showQrcode(row), children: "\u4E8C\u7EF4\u7801" }), _jsx(Button, { type: "link", size: "small", className: "operate-btn", onClick: () => openPeople(row), children: "\u62A5\u540D\u5217\u8868" }), _jsx(Button, { type: "link", size: "small", className: "operate-btn", onClick: () => openEdit(row), children: "\u4FEE\u6539" }), _jsx(Button, { type: "link", size: "small", className: "operate-btn operate-btn-danger", onClick: () => handleDelete(row), children: "\u5220\u9664" }), (row.files?.length ?? 0) > 0 && (_jsx(Button, { type: "link", size: "small", className: "operate-btn", onClick: () => showPlanFiles(row), children: "\u67E5\u770B\u6587\u4EF6" }))] })),
        },
    ];
    const peopleColumns = [
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
        { title: '年龄', dataIndex: 'age', width: 70, render: (v) => v ?? '' },
        { title: '工种', dataIndex: 'workTypeName', render: (v) => v || '' },
        { title: '工种等级', dataIndex: 'level', width: 120, render: (v) => (v != null ? (LEVEL_LABELS[v] ?? v) : '') },
        { title: '报名时间', dataIndex: 'createTime', width: 160 },
        {
            title: '审核状态', dataIndex: 'status', width: 100,
            render: (v, row) => renderPeopleStatus(v, row.statusCn),
        },
        {
            title: '是否已支付', dataIndex: 'hasPay', width: 110,
            render: (v, row) => (_jsx(Switch, { checked: !!v, size: "small", loading: updatePayMu.isPending && updatePayMu.variables?.id === row.id, onChange: (checked) => updatePayMu.mutate({ id: row.id, value: checked ? 1 : 0 }) })),
        },
        {
            title: '操作', key: 'act', width: 80,
            render: (_, row) => (_jsx(Button, { type: "link", size: "small", className: "operate-btn", onClick: () => openPeopleEdit(row), children: "\u7F16\u8F91" })),
        },
    ];
    const orcFailColumns = [
        { title: '时间', dataIndex: 'createTime', width: 160 },
        { title: '用户ID', dataIndex: 'platformUserId', width: 100 },
        { title: '状态', dataIndex: 'status', width: 80, render: (v) => (v === 1 ? '成功' : '失败') },
        { title: '失败信息', dataIndex: 'failReason', ellipsis: true },
        { title: '识别数据', dataIndex: 'orcData', ellipsis: true },
    ];
    const handleSave = async () => {
        try {
            const values = await form.validateFields();
            saveMu.mutate(toSignUpPayload(values));
        }
        catch {
            // 表单校验失败
        }
    };
    return (_jsxs("div", { className: "training-list-container", children: [_jsxs("section", { className: "page-heading", style: { marginBottom: 16 }, children: [_jsx(Typography.Title, { level: 4, children: "\u4FE1\u606F\u6536\u96C6" }), _jsx(Typography.Text, { type: "secondary", children: "\u521B\u5EFA\u62A5\u540D\u8BA1\u5212\u3001\u751F\u6210\u626B\u7801\u4E8C\u7EF4\u7801\uFF0C\u6536\u96C6\u5B66\u5458\u62A5\u540D\u8D44\u6599" })] }), _jsx(TrainingProcessSteps, { current: 4 }), _jsx("div", { className: "top-bar", children: _jsxs("div", { children: [_jsx(Input, { placeholder: "\u6807\u9898\u641C\u7D22", value: keyword, onChange: (e) => { setKeyword(e.target.value); setPage(1); }, style: { width: 200 }, allowClear: true }), _jsx(Button, { type: "primary", style: { marginLeft: 10 }, onClick: openCreate, children: "+ \u6DFB\u52A0" })] }) }), _jsx(Table, { rowKey: "id", loading: isLoading, columns: planColumns, dataSource: planRows, scroll: { y: 670, x: 1200 }, pagination: false }), _jsx("div", { className: "pagination-container", children: _jsx(Pagination, { current: page, pageSize: 20, total: data?.total ?? 0, onChange: setPage, showTotal: (total) => `共 ${total} 条`, showSizeChanger: false }) }), _jsx(Modal, { title: modalTitle, open: modalOpen, width: "50%", onCancel: () => { setModalOpen(false); setEditing(null); form.resetFields(); }, onOk: handleSave, confirmLoading: saveMu.isPending, destroyOnHidden: true, okText: "\u786E\u8BA4", cancelText: "\u53D6\u6D88", children: _jsxs(Form, { form: form, layout: "horizontal", className: "sign-up-form", labelCol: { flex: '120px' }, wrapperCol: { flex: 1 }, colon: false, initialValues: SIGN_UP_FORM_INITIAL, children: [_jsx(Form.Item, { name: "title", label: "\u6807\u9898", rules: [{ required: true, message: '请输入标题' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u6807\u9898", maxLength: 255 }) }), _jsx(Form.Item, { name: "cover", label: "\u5C01\u9762", getValueFromEvent: (e) => {
                                // antd Upload 默认把 fileList 提交给表单，里头是对象数组；
                                // 这里抽出我们需要的 URL 字符串，避免后端 JSON 反序列化失败
                                const fileList = Array.isArray(e?.fileList) ? e.fileList : [];
                                const last = fileList[fileList.length - 1];
                                if (!last)
                                    return '';
                                if (typeof last.url === 'string' && last.url)
                                    return last.url;
                                if (typeof last.thumbUrl === 'string' && last.thumbUrl)
                                    return last.thumbUrl;
                                if (last.response && typeof last.response === 'object' && last.response.id) {
                                    return `/api/files/${last.response.id}/download`;
                                }
                                return '';
                            }, children: _jsx(Upload, { ...coverUploadProps, children: coverPreview ? (_jsx(AuthImage, { url: coverPreview, alt: "cover", className: "cover", style: { width: 178, height: 178, display: 'block', objectFit: 'cover' } })) : (_jsx("div", { className: "cover-uploader-icon", children: _jsx(PlusOutlined, {}) })) }) }), _jsx(Form.Item, { name: "trainTimeRange", label: "\u57F9\u8BAD\u65F6\u95F4", rules: [{ required: true, message: '请选择培训时间' }], children: _jsx(RangePicker, { style: { width: 400 }, format: DATE_FORMAT, placeholder: ['开始日期', '结束日期'] }) }), _jsx(Form.Item, { name: "examTimeDate", label: "\u8003\u8BD5\u65F6\u95F4", rules: [{ required: true, message: '请选择考试时间' }], children: _jsx(DatePicker, { style: { width: 400 }, format: DATE_FORMAT, placeholder: "\u9009\u62E9\u8003\u8BD5\u65E5\u671F" }) }), _jsx(Form.Item, { name: "orgId", label: "\u516C\u53F8\u540D\u79F0", rules: [{ required: true, message: '请选择公司名称' }], children: _jsx(Select, { allowClear: true, showSearch: true, placeholder: "\u8BF7\u9009\u62E9", style: { width: 400 }, filterOption: (input, option) => (option?.label || '').includes(input), options: companies.map((c) => ({ value: c.id, label: c.title })) }) }), _jsx(Form.Item, { name: "trainInfoId", label: "\u57F9\u8BAD\u4FE1\u606F", children: _jsx(Select, { allowClear: true, showSearch: true, placeholder: "\u8BF7\u9009\u62E9\u57F9\u8BAD\u4FE1\u606F", style: { width: 400 }, filterOption: (input, option) => (option?.label || '').includes(input), options: trainInfos.map((t) => ({ value: t.id, label: t.title })) }) }), _jsx(Form.Item, { name: "sortNumber", label: "\u6392\u5E8F", children: _jsx(InputNumber, { min: 0, precision: 0, style: { width: 400 }, placeholder: "0" }) })] }) }), _jsx(Modal, { title: "\u53CB\u60C5\u63D0\u793A", open: deleteConfirmOpen, width: "50%", onCancel: () => { setDeleteConfirmOpen(false); setDeleteTargetId(null); }, onOk: confirmDelete, confirmLoading: deleteMu.isPending, okText: "\u786E\u5B9A", cancelText: "\u53D6\u6D88", children: "\u786E\u5B9A\u8981\u5220\u9664\u9009\u4E2D\u9879\u5417?" }), _jsx(Modal, { title: "\u62A5\u540D\u4E8C\u7EF4\u7801", open: !!qrModal, onCancel: () => { setQrModal(null); setWxQrcodeLoading(false); }, footer: null, width: 360, centered: true, maskClosable: false, keyboard: false, children: qrModal && (_jsxs("div", { className: "qrcode-modal-body", children: [_jsx("div", { className: "qrcode-title", children: qrModal.title }), _jsxs("div", { className: "qrcode-row", children: [_jsxs("div", { className: "qrcode-col", children: [_jsx(Image, { src: qrModal.appQrcodeUrl, width: 120, height: 120, style: { objectFit: 'fill' }, preview: false }), _jsx("div", { className: "text-C", children: "APP\u626B" })] }), _jsx("div", { className: "qrcode-col", children: qrModal.wxQrcodeUrl ? (_jsxs(_Fragment, { children: [_jsx(Image, { src: qrModal.wxQrcodeUrl, width: 120, height: 120, style: { objectFit: 'fill' } }), _jsx("div", { className: "text-C", children: "\u5FAE\u4FE1\u626B" })] })) : (_jsxs(_Fragment, { children: [_jsx("div", { className: "wx-qrcode-placeholder", children: _jsx(Button, { type: "primary", loading: wxQrcodeLoading, onClick: fetchWxMiniQrcode, children: "\u83B7\u53D6" }) }), _jsx("div", { className: "text-C", children: "\u5FAE\u4FE1\u626B" })] })) })] })] })) }), _jsx(Modal, { title: "\u62A5\u540D\u4EBA\u5458\u5217\u8868", open: peopleModalOpen, width: "80%", centered: true, onCancel: closePeopleModal, footer: null, destroyOnHidden: true, children: _jsxs("div", { className: "people-modal-body", children: [_jsxs("div", { className: "people-search-row", children: [_jsx(Input, { className: "people-search-input", placeholder: "\u5173\u952E\u5B57\u641C\u7D22", value: peopleKeyword, onChange: (e) => { setPeopleKeyword(e.target.value); setPeoplePage(1); }, allowClear: true }), _jsx(Select, { className: "people-search-status", allowClear: true, placeholder: "\u8BF7\u9009\u62E9\u72B6\u6001", value: peopleStatus, onChange: (v) => { setPeopleStatus(v); setPeoplePage(1); }, options: [
                                        { value: 0, label: '待审核' },
                                        { value: 1, label: '已通过' },
                                    ] })] }), _jsx("div", { className: "tools", children: _jsxs(Space, { wrap: true, size: [8, 8], children: [_jsx(Button, { type: isPeopleChecked ? 'default' : 'primary', disabled: isPeopleChecked, onClick: () => auditMu.mutate({ ids: selectedPeopleIds, value: 1 }), children: "\u6279\u91CF\u901A\u8FC7" }), _jsx(Button, { type: isPeopleChecked ? 'default' : 'primary', danger: !isPeopleChecked, disabled: isPeopleChecked, onClick: () => auditMu.mutate({ ids: selectedPeopleIds, value: 2 }), children: "\u6279\u91CF\u62D2\u7EDD" }), _jsx(Button, { type: isPeopleChecked ? 'default' : 'primary', danger: !isPeopleChecked, disabled: isPeopleChecked, onClick: handleBatchDeletePeople, children: "\u6279\u91CF\u5220\u9664" }), _jsx(Button, { type: "primary", onClick: handleDefaultTemplateExport, children: "\u9ED8\u8BA4\u6A21\u677F\u5BFC\u51FA" }), _jsx(Button, { type: "primary", disabled: !activeSignUp?.id, onClick: handleExportPeople, children: "\u4EC5\u5BFC\u51FA\u540D\u5355" }), _jsx(Button, { type: "primary", onClick: () => setImportOpen(true), children: "\u6279\u91CF\u5BFC\u5165" }), _jsx(Button, { type: "primary", onClick: openImportStudentModal, children: "\u4ECE\u5B66\u5458\u4E2D\u5BFC\u5165" }), _jsx(Button, { type: "primary", onClick: () => setToFuxiOpen(true), children: "\u6279\u91CF\u52A0\u5165\u590D\u4E60\u8BA1\u5212" }), _jsx(Button, { type: "primary", onClick: handleOpenOrcFailLog, children: "\u83B7\u53D6\u8EAB\u4EFD\u8BC1\u8BC6\u522B\u5931\u8D25\u8BB0\u5F55" })] }) }), _jsx(Table, { rowKey: "id", loading: peopleLoading, columns: peopleColumns, dataSource: peopleRows, scroll: { y: 450, x: 1400 }, pagination: false, rowSelection: {
                                selectedRowKeys: selectedPeopleIds,
                                onChange: (keys) => setSelectedPeopleIds(keys),
                            } }), _jsx("div", { className: "pagination-container", style: { height: 60 }, children: _jsx(Pagination, { current: peoplePage, pageSize: 20, total: peopleData?.total ?? 0, onChange: setPeoplePage, showTotal: (total) => `共 ${total} 条`, showSizeChanger: false }) })] }) }), _jsx(SignUpPeopleEditModal, { open: peopleEditOpen, personId: editingPersonId, onClose: () => { setPeopleEditOpen(false); setEditingPersonId(null); }, onSaved: () => {
                    setPeopleEditOpen(false);
                    setEditingPersonId(null);
                    refetchPeople();
                } }), _jsx(Modal, { title: "\u6279\u91CF\u5BFC\u5165\u62A5\u540D\u4EBA\u5458", open: importOpen, width: "40%", onCancel: closeImportModal, footer: null, destroyOnHidden: true, maskClosable: false, children: _jsx(Spin, { spinning: importLoading, children: _jsxs(Upload.Dragger, { accept: ".xlsx,.xls", maxCount: 1, disabled: importLoading, showUploadList: false, beforeUpload: (file) => {
                            handleImportUpload(file);
                            return false;
                        }, children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(InboxOutlined, {}) }), _jsxs("p", { className: "ant-upload-text", children: ["\u5C06\u6587\u4EF6\u62D6\u5230\u6B64\u5904\uFF0C\u6216", _jsx("em", { children: "\u70B9\u51FB\u4E0A\u4F20" })] }), _jsx("p", { className: "ant-upload-hint", style: { color: '#ccc', marginTop: 10 }, children: "\u63D0\u793A:\u4E0A\u4F20\u8FC7\u7A0B\u4E2D,\u8BF7\u4E0D\u8981\u505A\u4EFB\u4F55\u64CD\u4F5C" }), _jsxs("p", { className: "ant-upload-hint", children: ["\u53EA\u80FD\u4E0A\u4F20xlsx/xls\u6587\u4EF6", ' ', _jsx("a", { href: "#", onClick: handleDownloadImportTemplate, children: templateDownloading ? '下载中...' : '下载模板' })] })] }) }) }), _jsxs(Modal, { title: "\u6279\u91CF\u5BFC\u5165\u62A5\u540D\u4EBA\u5458", open: importStudentOpen, width: "50%", onCancel: closeImportStudentModal, footer: null, destroyOnHidden: true, maskClosable: false, children: [_jsxs("div", { className: "tools", children: [_jsx(Input, { placeholder: "\u5173\u952E\u5B57\u641C\u7D22", value: studentKeyword, onChange: (e) => { setStudentKeyword(e.target.value); setStudentPage(1); }, style: { width: 200, marginRight: 10 }, allowClear: true }), _jsx(Button, { style: { marginRight: 10 }, onClick: () => { setIdCardText(''); setImportByIdCardOpen(true); }, children: "\u6309\u8EAB\u4EFD\u8BC1\u53F7\u5BFC\u5165" }), _jsx(Button, { type: "primary", loading: importStudentLoading, onClick: handleImportStudents, children: "\u5BFC\u5165\u9009\u4E2D\u5B66\u5458" })] }), _jsx(Table, { rowKey: "id", loading: studentLoading, columns: [
                            { title: 'ID', dataIndex: 'id', width: 100 },
                            { title: '身份证号', dataIndex: 'idcard', width: 180 },
                            { title: '姓名', dataIndex: 'realname', width: 100 },
                            { title: '手机号', dataIndex: 'phone', width: 120 },
                            { title: '所属企业', dataIndex: 'companyName', render: (v) => v || '-' },
                        ], dataSource: studentData?.items ?? [], scroll: { y: 350 }, pagination: {
                            current: studentPage,
                            pageSize: 20,
                            total: studentData?.total ?? 0,
                            onChange: setStudentPage,
                            showSizeChanger: false,
                            showTotal: (total) => `共 ${total} 条`,
                        }, rowSelection: {
                            selectedRowKeys: selectedStudentIds,
                            onChange: (keys) => setSelectedStudentIds(keys),
                            getCheckboxProps: (row) => ({
                                disabled: !row.idcard,
                            }),
                        } })] }), _jsxs(Modal, { title: "\u6309\u8EAB\u4EFD\u8BC1\u53F7\u5BFC\u5165", open: importByIdCardOpen, onCancel: () => { setImportByIdCardOpen(false); setIdCardText(''); }, footer: null, maskClosable: false, width: 420, destroyOnHidden: true, children: [_jsx(Input.TextArea, { autoSize: { minRows: 4, maxRows: 10 }, value: idCardText, onChange: (e) => setIdCardText(e.target.value), placeholder: "\u8BF7\u8F93\u5165\u8EAB\u4EFD\u8BC1,\u4E00\u884C\u4E00\u4E2A" }), _jsx("div", { style: { marginTop: 12, textAlign: 'right' }, children: _jsx(Button, { type: "primary", loading: importByIdCardLoading, onClick: handleImportStudentsByIdCard, children: "\u786E\u5B9A" }) })] }), _jsx(Modal, { title: "\u6279\u91CF\u52A0\u5165\u590D\u4E60\u8BA1\u5212", open: toFuxiOpen, width: "40%", onCancel: () => setToFuxiOpen(false), onOk: () => message.info('批量加入复习计划功能正在对接中'), okText: "\u786E\u8BA4", cancelText: "\u53D6\u6D88", children: _jsx(Form, { layout: "horizontal", labelCol: { flex: '120px' }, wrapperCol: { flex: 1 }, colon: false, children: _jsx(Form.Item, { label: "\u8003\u8BD5/\u590D\u4E60\u8BA1\u5212", children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u8BA1\u5212", style: { width: 400 }, disabled: true, options: [] }) }) }) }), _jsx(Modal, { title: "\u9ED8\u8BA4\u6A21\u677F\u5BFC\u51FA", open: defaultExportOpen, width: "40%", onCancel: closeDefaultExportModal, footer: null, maskClosable: false, children: _jsx("div", { children: defaultExportTitle ? (_jsxs("div", { children: [defaultExportTitle, defaultExportUrl ? (_jsxs(_Fragment, { children: [' ', _jsx("a", { onClick: (e) => {
                                            e.preventDefault();
                                            downloadExportFile(defaultExportUrl);
                                        }, href: "#", children: "\u4E0B\u8F7D" })] })) : null] })) : (_jsxs("div", { children: ["\u9ED8\u8BA4\u6A21\u677F\u6B63\u5728\u5BFC\u51FA\uFF0C\u8BF7\u7A0D\u540E", exportElapsed > 0 ? `${exportElapsed}秒` : ''] })) }) }), _jsx(Modal, { title: "\u67E5\u770B\u6587\u4EF6", open: filesOpen, width: "40%", onCancel: closePlanFiles, footer: null, destroyOnHidden: true, maskClosable: false, children: _jsx(Table, { rowKey: (row, index) => `${row.fileUrl}-${index}`, columns: [
                        {
                            title: '文件名',
                            dataIndex: 'fileUrl',
                            ellipsis: true,
                            render: (url) => fileNameFromUrl(url) || url,
                        },
                        {
                            title: '操作',
                            width: 80,
                            render: (_, row) => (_jsx(Button, { type: "link", size: "small", className: "operate-btn", onClick: () => previewPlanFile(row), children: "\u9884\u89C8" })),
                        },
                    ], dataSource: filesData, scroll: { y: 350 }, pagination: false, bordered: true }) }), _jsx(FilePreviewModal, { open: Boolean(filePreviewTarget), target: filePreviewTarget, onClose: () => setFilePreviewTarget(null) }), _jsx(Modal, { title: "\u8EAB\u4EFD\u8BC1\u8BC6\u522B\u5931\u8D25\u8BB0\u5F55", open: orcFailOpen, width: "70%", onCancel: () => { setOrcFailOpen(false); setOrcFailData([]); }, footer: null, destroyOnHidden: true, children: _jsx(Table, { rowKey: "id", loading: orcFailLoading, columns: orcFailColumns, dataSource: orcFailData, scroll: { y: 350 }, pagination: false }) })] }));
}
