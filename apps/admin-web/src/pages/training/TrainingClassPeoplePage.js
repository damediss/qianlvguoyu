import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 班级人员 — 对齐 yun_shang classMembers.vue
 */
import { useMemo, useState, useEffect } from 'react';
import { Button, Card, DatePicker, Form, Input, Modal, Pagination, Select, Space, Table, Typography, Upload, message, } from 'antd';
import { EditOutlined, InboxOutlined, PlusOutlined, } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { apiClient, unwrap } from '../../api/client';
import { uploadFile } from '../../api/knowledge';
import { FilePreviewModal } from '../../components/FilePreviewModal';
import { AuthImage } from '../../components/ApiAuthImage';
import { fileNameFromUrl } from '../../api/files';
import { useAuthStore } from '../../stores/authStore';
import './TrainingClassPeoplePage.css';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import { ClassSignUpPeoplePanel } from './ClassSignUpPeoplePanel';
import { ClassRolesModal } from './ClassRolesPanel';
const { Dragger } = Upload;
/** 与需求管理等页一致的工种等级选项 */
const WORK_LEVELS = [
    { value: 5, label: '五级/初级工' },
    { value: 4, label: '四级/中级工' },
    { value: 3, label: '三级/高级工' },
    { value: 2, label: '二级/技师' },
    { value: 1, label: '一级/高级技师' },
];
function workLevelLabel(level) {
    const n = Number(level);
    if (!Number.isFinite(n) || n <= 0)
        return '';
    return WORK_LEVELS.find((o) => o.value === n)?.label ?? `${n}级`;
}
function toPositiveInt(value) {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? n : undefined;
}
function toApiId(value) {
    if (value == null || value === '')
        return null;
    if (typeof value === 'number') {
        return Number.isFinite(value) ? value : null;
    }
    const s = String(value).trim();
    if (!s)
        return null;
    if (/^\d+$/.test(s)) {
        const n = Number(s);
        return Number.isSafeInteger(n) ? n : s;
    }
    const n = Number(s);
    return Number.isFinite(n) ? n : null;
}
function resolveFileUrl(path) {
    if (!path)
        return '';
    if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('/api/'))
        return path;
    return path.startsWith('/') ? path : `/${path}`;
}
/** 兼容接口 snake_case / camelCase 字段 */
function normalizeClassStudent(raw) {
    const pick = (...keys) => {
        for (const key of keys) {
            const v = raw[key];
            if (v !== undefined && v !== null && v !== '')
                return v;
        }
        return undefined;
    };
    return {
        id: toApiId(pick('id')) ?? 0,
        realname: String(pick('realname', 'name') ?? ''),
        phone: pick('phone') != null ? String(pick('phone')) : undefined,
        train_id: toApiId(pick('train_id', 'trainId')) ?? undefined,
        train_class_id: toApiId(pick('train_class_id', 'trainClassId')) ?? undefined,
        id_card: pick('id_card', 'idCard') != null ? String(pick('id_card', 'idCard')) : undefined,
        is_leader: pick('is_leader', 'isLeader') != null ? Number(pick('is_leader', 'isLeader')) : undefined,
        is_leader_cn: pick('is_leader_cn', 'isLeaderCn') != null ? String(pick('is_leader_cn', 'isLeaderCn')) : undefined,
        is_committee: pick('is_committee', 'isCommittee') != null ? Number(pick('is_committee', 'isCommittee')) : undefined,
        is_committee_cn: pick('is_committee_cn', 'isCommitteeCn') != null ? String(pick('is_committee_cn', 'isCommitteeCn')) : undefined,
        normal_score: pick('normal_score', 'normalScore') != null ? Number(pick('normal_score', 'normalScore')) : undefined,
        header: pick('header', 'idcardHeader', 'idcard_header') != null ? String(pick('header', 'idcardHeader', 'idcard_header')) : undefined,
    };
}
function normalizeClassStudentList(data) {
    const rows = (data?.list ?? data?.lists ?? []);
    return rows.map((row) => normalizeClassStudent(row));
}
// ==================== 班级人员弹窗（对齐 yun_shang classMembers.vue） ====================
function StudentListModal({ open, onClose, classId, trainInfoId, workTypeTitle, workTypeLevel, workTypeId, mode = 'roster', onProfileChanged, }) {
    const [collectionSignUpId, setCollectionSignUpId] = useState(null);
    const [collectionTitle, setCollectionTitle] = useState();
    /** idle→loading→ready|error，避免打开瞬间误显示「未就绪」 */
    const [collectionStatus, setCollectionStatus] = useState('idle');
    const [collectionError, setCollectionError] = useState(null);
    const [editOpen, setEditOpen] = useState(false);
    const [editForm] = Form.useForm();
    const [importOpen, setImportOpen] = useState(false);
    const [importByIdCardOpen, setImportByIdCardOpen] = useState(false);
    const [importKeyword, setImportKeyword] = useState('');
    const [selectedSignUpIds, setSelectedSignUpIds] = useState([]);
    const [selectedStudentIds, setSelectedStudentIds] = useState([]);
    const [idCardText, setIdCardText] = useState('');
    const [currentPage, setCurrentPage] = useState(1);
    const pageSize = 20;
    const normalizedClassId = classId != null ? toApiId(classId) : null;
    const ensureCollectionMu = useMutation({
        mutationFn: (id) => trainApi.ensureClassCollection(id),
        onSuccess: (res) => {
            const sid = toApiId(res.signUpId);
            if (sid == null) {
                setCollectionStatus('error');
                setCollectionError('后端未返回资料收集计划 ID');
                message.error('打开资料收集失败：未返回计划 ID');
                return;
            }
            setCollectionSignUpId(sid);
            setCollectionTitle(res.title);
            setCollectionStatus('ready');
            setCollectionError(null);
            onProfileChanged?.();
        },
        onError: (err) => {
            const ax = err;
            const msg = ax.response?.data?.message || ax.message || '打开资料收集失败';
            setCollectionStatus('error');
            setCollectionError(msg);
            message.error(msg);
        },
    });
    useEffect(() => {
        if (!open) {
            setCurrentPage(1);
            setCollectionSignUpId(null);
            setCollectionTitle(undefined);
            setCollectionStatus('idle');
            setCollectionError(null);
            setSelectedStudentIds([]);
            return;
        }
        setCurrentPage(1);
        if (mode === 'collection' && normalizedClassId != null) {
            setCollectionSignUpId(null);
            setCollectionTitle(undefined);
            setCollectionError(null);
            setCollectionStatus('loading');
            ensureCollectionMu.mutate(normalizedClassId);
        }
        else {
            setCollectionSignUpId(null);
            setCollectionTitle(undefined);
            setCollectionStatus('idle');
            setCollectionError(null);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, classId, mode]);
    const { data, isLoading, refetch, isError } = useQuery({
        queryKey: ['class-students', normalizedClassId, currentPage, pageSize],
        queryFn: () => trainApi.listClassStudents(normalizedClassId, currentPage, pageSize),
        enabled: open && mode === 'roster' && normalizedClassId != null,
    });
    const { data: signUpData } = useQuery({
        queryKey: ['sign-up-self', trainInfoId, normalizedClassId, importKeyword],
        queryFn: () => trainApi.listSignUpSelf(trainInfoId, importKeyword || undefined, normalizedClassId ?? undefined),
        enabled: importOpen && !!trainInfoId,
    });
    const saveMu = useMutation({
        mutationFn: (p) => trainApi.saveClassStudent(p),
        onSuccess: () => { message.success('成功'); setEditOpen(false); refetch(); },
        onError: (e) => message.error(e.message || '保存失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (ids) => trainApi.deleteClassStudents(ids),
        onSuccess: (_res, ids) => {
            message.success(ids.length > 1 ? `已删除 ${ids.length} 人` : '删除成功');
            setSelectedStudentIds((prev) => prev.filter((id) => !ids.map(String).includes(String(id))));
            refetch();
        },
        onError: () => message.error('删除失败'),
    });
    const importMu = useMutation({
        mutationFn: (p) => trainApi.importStudents(p),
        onSuccess: (res) => {
            const sync = res?.platformUserSync;
            const created = sync?.created ?? 0;
            const skipped = sync?.skipped ?? 0;
            if (created > 0) {
                message.success(`导入成功，新建学员账号 ${created} 人（默认密码 123123）`);
            }
            else if (skipped > 0) {
                message.warning(`导入完成，但有 ${skipped} 人未能开通学员账号，请查看失败原因`);
            }
            else {
                message.success('导入成功');
            }
            setImportOpen(false);
            setSelectedSignUpIds([]);
            refetch();
        },
        onError: () => message.error('导入失败'),
    });
    const importByIdCardMu = useMutation({
        mutationFn: (p) => trainApi.importStudentsByIdCard(p),
        onSuccess: (res) => {
            const sync = res?.platformUserSync;
            const created = sync?.created ?? 0;
            const skipped = sync?.skipped ?? 0;
            if (created > 0) {
                message.success(`导入成功，新建学员账号 ${created} 人（默认密码 123123）`);
            }
            else if (skipped > 0) {
                message.warning(`导入完成，但有 ${skipped} 人未能开通学员账号，请查看失败原因`);
            }
            else {
                message.success('导入成功');
            }
            setImportByIdCardOpen(false);
            setIdCardText('');
            refetch();
        },
        onError: () => message.error('导入失败'),
    });
    const openEditStudent = (row) => {
        editForm.setFieldsValue({
            id: row.id,
            realname: row.realname,
            phone: row.phone,
            id_card: row.id_card,
            normal_score: row.normal_score,
            is_leader: String(row.is_leader ?? 0),
            is_committee: String(row.is_committee ?? 0),
        });
        setEditOpen(true);
    };
    const formatYesNo = (cn, val) => cn ?? (val === 1 ? '是' : '否');
    const formatNormalScore = (score) => {
        if (score == null || Number.isNaN(Number(score)))
            return '0.00';
        return Number(score).toFixed(2);
    };
    const renderIdCardPhoto = (row) => {
        const url = row.header?.trim();
        if (!url)
            return _jsx("div", { className: "student-photo-cell" });
        return (_jsx("div", { className: "student-photo-cell", children: _jsx(AuthImage, { url: url, alt: "\u8BC1\u4EF6\u7167", className: "student-id-photo", width: 72, height: 96, style: { objectFit: 'cover', borderRadius: 2 } }) }));
    };
    const handleDeleteStudent = (row) => {
        Modal.confirm({
            title: '友情提示',
            content: '确定要删除该学员吗？',
            onOk: () => deleteMu.mutateAsync([row.id]),
        });
    };
    const handleBatchDeleteStudents = () => {
        if (!selectedStudentIds.length) {
            message.warning('请先勾选要删除的学员');
            return;
        }
        Modal.confirm({
            title: '友情提示',
            content: `确定删除已选的 ${selectedStudentIds.length} 名学员吗？不会删除学员 App 账号。`,
            okButtonProps: { danger: true },
            onOk: () => deleteMu.mutateAsync(selectedStudentIds),
        });
    };
    const studentColumns = [
        { title: 'ID', dataIndex: 'id', key: 'id', width: 72, align: 'center' },
        { title: '姓名', dataIndex: 'realname', key: 'realname', width: 100, align: 'left' },
        { title: '手机号', dataIndex: 'phone', key: 'phone', width: 124, align: 'center' },
        { title: '身份证号', dataIndex: 'id_card', key: 'id_card', width: 188, align: 'center' },
        {
            title: '平时分',
            dataIndex: 'normal_score',
            key: 'normal_score',
            width: 88,
            align: 'center',
            render: (v) => formatNormalScore(v),
        },
        {
            title: '证件照',
            key: 'idcard_header',
            width: 100,
            align: 'center',
            render: (_, row) => renderIdCardPhoto(row),
        },
        {
            title: '是否班长',
            key: 'is_leader_cn',
            width: 88,
            align: 'center',
            render: (_, row) => formatYesNo(row.is_leader_cn, row.is_leader),
        },
        {
            title: '是否学委',
            key: 'is_committee_cn',
            width: 88,
            align: 'center',
            render: (_, row) => formatYesNo(row.is_committee_cn, row.is_committee),
        },
        {
            title: '操作',
            key: 'act',
            width: 120,
            align: 'center',
            fixed: 'right',
            render: (_, row) => (_jsxs("div", { className: "student-row-actions", children: [_jsx("button", { type: "button", className: "student-action-link", onClick: () => openEditStudent(row), children: "\u7F16\u8F91" }), _jsx("button", { type: "button", className: "student-action-link student-action-link-danger", onClick: () => handleDeleteStudent(row), children: "\u5220\u9664" })] })),
        },
    ];
    const signUpColumns = [
        { title: '姓名', dataIndex: 'name', align: 'center' },
        { title: '手机号', dataIndex: 'phone', align: 'center' },
    ];
    const studentRows = useMemo(() => normalizeClassStudentList(data), [data]);
    const handleImportSubmit = () => {
        if (!selectedSignUpIds.length) {
            message.error('请选择要导入的人员');
            return;
        }
        if (!trainInfoId || normalizedClassId == null) {
            message.error('班级未关联培训信息，无法从报名导入');
            return;
        }
        importMu.mutate({ traininfoId: trainInfoId, classId: normalizedClassId, studentIds: selectedSignUpIds });
    };
    const handleImportByIdCardSubmit = () => {
        if (!idCardText.trim()) {
            message.error('请输入身份证号');
            return;
        }
        if (!trainInfoId || normalizedClassId == null) {
            message.error('班级未关联培训信息，无法按身份证导入');
            return;
        }
        importByIdCardMu.mutate({ traininfoId: trainInfoId, classId: normalizedClassId, studentIds: idCardText });
    };
    return (_jsxs(_Fragment, { children: [_jsx(Modal, { title: mode === 'collection' ? '资料收集' : '班级人员', open: open, onCancel: onClose, footer: null, width: "88%", centered: true, destroyOnHidden: true, rootClassName: "class-student-list-modal", styles: { body: { padding: '12px 16px 16px', maxHeight: '78vh', overflow: 'hidden' } }, children: mode === 'collection' ? (collectionStatus === 'loading' || (collectionStatus === 'idle' && !collectionSignUpId) ? (_jsx("div", { style: { padding: 24, textAlign: 'center' }, children: "\u6B63\u5728\u51C6\u5907\u8D44\u6599\u6536\u96C6\u2026" })) : collectionStatus === 'ready' && collectionSignUpId != null ? (_jsx("div", { className: "class-student-table-scroll", style: { maxHeight: '68vh', overflow: 'auto' }, children: _jsx(ClassSignUpPeoplePanel, { signUpId: collectionSignUpId, title: collectionTitle, classWorkTypeTitle: workTypeTitle, classWorkTypeLevel: workTypeLevel, classWorkTypeId: workTypeId, onChanged: () => {
                            onProfileChanged?.();
                        } }) })) : (_jsxs("div", { style: { padding: 24, textAlign: 'center' }, children: [_jsx("div", { style: { marginBottom: 12 }, children: collectionError || '资料收集计划未就绪' }), normalizedClassId != null ? (_jsx(Button, { type: "primary", loading: ensureCollectionMu.isPending, onClick: () => {
                                setCollectionError(null);
                                setCollectionStatus('loading');
                                ensureCollectionMu.mutate(normalizedClassId);
                            }, children: "\u91CD\u8BD5" })) : null] }))) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "student-toolbar", children: [_jsx(Button, { className: "student-toolbar-btn", disabled: !trainInfoId, title: !trainInfoId ? '班级未关联培训信息' : undefined, onClick: () => setImportOpen(true), children: "\u6279\u91CF\u5BFC\u5165" }), _jsx(Button, { className: "student-toolbar-btn", disabled: !trainInfoId, title: !trainInfoId ? '班级未关联培训信息' : undefined, onClick: () => setImportByIdCardOpen(true), children: "\u6309\u8EAB\u4EFD\u8BC1\u53F7\u5BFC\u5165" }), _jsx(Button, { className: "student-toolbar-btn", danger: true, disabled: !selectedStudentIds.length, loading: deleteMu.isPending, onClick: handleBatchDeleteStudents, children: "\u6279\u91CF\u5220\u9664" }), isError ? (_jsx(Button, { className: "student-toolbar-btn", onClick: () => refetch(), children: "\u91CD\u65B0\u52A0\u8F7D" })) : null] }), _jsx("div", { className: "class-student-table-scroll", children: _jsx(Table, { className: "class-student-table", rowKey: "id", bordered: true, size: "middle", loading: isLoading, columns: studentColumns, dataSource: studentRows, rowSelection: {
                                    columnWidth: 48,
                                    selectedRowKeys: selectedStudentIds,
                                    onChange: (keys) => setSelectedStudentIds(keys),
                                }, scroll: { x: 1100, y: 480 }, pagination: false, locale: { emptyText: isError ? '加载失败，请点击重新加载' : '暂无学员，可从报名或身份证导入' } }) }), _jsx(Pagination, { className: "student-pagination", current: currentPage, pageSize: pageSize, total: data?.total ?? 0, showSizeChanger: false, showQuickJumper: true, showTotal: (t) => `共 ${t} 条`, onChange: (p) => setCurrentPage(p) })] })) }), _jsx(Modal, { title: "\u73ED\u7EA7\u4EBA\u5458\u7F16\u8F91", open: editOpen, onCancel: () => setEditOpen(false), onOk: () => {
                    void editForm.validateFields().then((values) => saveMu.mutate(values));
                }, confirmLoading: saveMu.isPending, okText: "\u786E\u5B9A", cancelText: "\u53D6\u6D88", width: "35%", destroyOnHidden: true, className: "training-class-people-page", wrapClassName: "training-class-people-page", children: _jsxs(Form, { form: editForm, labelCol: { style: { width: 80 } }, wrapperCol: { span: 16 }, children: [_jsx(Form.Item, { name: "id", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "realname", label: "\u59D3\u540D", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u59D3\u540D" }) }), _jsx(Form.Item, { name: "phone", label: "\u624B\u673A\u53F7", rules: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号' }], children: _jsx(Input, { maxLength: 11, inputMode: "numeric", placeholder: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7" }) }), _jsx(Form.Item, { name: "id_card", label: "\u8EAB\u4EFD\u8BC1\u53F7", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8EAB\u4EFD\u8BC1\u53F7" }) }), _jsx(Form.Item, { name: "normal_score", label: "\u5E73\u65F6\u5206", children: _jsx(Input, { type: "number", placeholder: "\u8BF7\u8F93\u5165\u5E73\u65F6\u5206" }) }), _jsx(Form.Item, { name: "is_leader", label: "\u662F\u5426\u73ED\u957F", children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9", options: [{ value: '1', label: '是' }, { value: '0', label: '否' }] }) }), _jsx(Form.Item, { name: "is_committee", label: "\u662F\u5426\u5B66\u59D4", children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9", options: [{ value: '1', label: '是' }, { value: '0', label: '否' }] }) })] }) }), _jsxs(Modal, { title: "\u73ED\u7EA7\u4EBA\u5458\u5BFC\u5165", open: importOpen, onCancel: () => { setImportOpen(false); setSelectedSignUpIds([]); setImportKeyword(''); }, footer: null, width: "35%", destroyOnHidden: true, className: "training-class-people-page", wrapClassName: "training-class-people-page", children: [_jsxs("div", { className: "mar-b10", children: [_jsx(Input, { placeholder: "\u5173\u952E\u641C\u7D22", value: importKeyword, onChange: (e) => setImportKeyword(e.target.value), allowClear: true, className: "mar-r20", style: { width: 200 } }), _jsx(Button, { type: "primary", loading: importMu.isPending, onClick: handleImportSubmit, children: "\u786E\u8BA4\u5BFC\u5165" })] }), _jsx(Table, { rowKey: "id", size: "small", bordered: true, columns: signUpColumns, dataSource: signUpData?.lists ?? [], rowSelection: { columnWidth: 55, selectedRowKeys: selectedSignUpIds, onChange: (keys) => setSelectedSignUpIds(keys) }, scroll: { y: 400 }, pagination: false })] }), _jsx(Modal, { title: "\u73ED\u7EA7\u4EBA\u5458\u5BFC\u5165", open: importByIdCardOpen, onCancel: () => { setImportByIdCardOpen(false); setIdCardText(''); }, footer: null, maskClosable: false, width: "35%", destroyOnHidden: true, className: "training-class-people-page", wrapClassName: "training-class-people-page", children: _jsxs("div", { children: [_jsx(Input.TextArea, { className: "idcard-import-textarea", autoSize: { minRows: 4, maxRows: 10 }, value: idCardText, onChange: (e) => setIdCardText(e.target.value), placeholder: "\u8BF7\u8F93\u5165\u8EAB\u4EFD\u8BC1,\u4E00\u884C\u4E00\u4E2A" }), _jsx("div", { className: "idcard-import-actions", children: _jsx(Button, { type: "primary", loading: importByIdCardMu.isPending, onClick: handleImportByIdCardSubmit, children: "\u786E\u5B9A" }) })] }) })] }));
}
// ==================== 教务/负责人弹窗（对齐 yun_shang classMembers.vue） ====================
function StaffModal({ open, onClose, classId, trainInfoId, tag, title: modalTitle, }) {
    const [editOpen, setEditOpen] = useState(false);
    const [editTitle, setEditTitle] = useState('');
    const [editForm] = Form.useForm();
    const [importOpen, setImportOpen] = useState(false);
    const [selectedTeacherIds, setSelectedTeacherIds] = useState([]);
    const { data, isLoading, refetch } = useQuery({
        queryKey: ['class-other', classId, tag],
        queryFn: () => trainApi.listClassOthers(trainInfoId, classId, tag),
        enabled: open && !!classId && !!trainInfoId,
    });
    const { data: importData } = useQuery({
        queryKey: ['other-import-data', tag],
        queryFn: () => trainApi.listOtherImportData(tag),
        enabled: importOpen,
    });
    const saveMu = useMutation({
        mutationFn: (p) => trainApi.saveClassOther(p),
        onSuccess: () => { message.success('成功'); setEditOpen(false); refetch(); },
        onError: () => message.error('保存失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (ids) => trainApi.deleteClassOthers(ids),
        onSuccess: () => { message.success('删除成功'); refetch(); },
        onError: () => message.error('删除失败'),
    });
    const importMu = useMutation({
        mutationFn: (p) => trainApi.importOthers(p),
        onSuccess: () => { message.success('导入成功'); setImportOpen(false); setSelectedTeacherIds([]); refetch(); },
        onError: () => message.error('导入失败'),
    });
    const openAdd = () => {
        editForm.resetFields();
        // tag=fuzhe → type=inCharge；教务走教师库导入，不会进入此分支
        editForm.setFieldsValue({
            id: 0,
            train_id: trainInfoId,
            classid: classId,
            type: tag === 'teacher' ? 'staff' : 'inCharge',
        });
        setEditTitle(modalTitle);
        setEditOpen(true);
    };
    const openEdit = (row) => {
        const titleMap = { teacher: '教务人员', fuzhe: '负责人' };
        setEditTitle(`${titleMap[tag] ?? modalTitle}编辑`);
        editForm.setFieldsValue({
            id: row.id,
            train_id: row.train_id,
            classid: row.train_class_id,
            realname: row.realname,
            phone: row.phone,
            id_card: row.id_card,
            type: row.type,
        });
        setEditOpen(true);
    };
    const handleDeleteStaff = (row) => {
        Modal.confirm({
            title: '友情提示',
            content: '确定要删除该人员吗？',
            onOk: () => deleteMu.mutate([row.id]),
        });
    };
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 72, align: 'center' },
        { title: '姓名', dataIndex: 'realname', align: 'left' },
        { title: '手机号', dataIndex: 'phone', width: 140, align: 'center' },
        {
            title: '操作',
            key: 'act',
            width: 120,
            align: 'center',
            render: (_, row) => (_jsxs("div", { className: "staff-row-actions", children: [_jsx("button", { type: "button", className: "staff-action-link", onClick: () => openEdit(row), children: "\u7F16\u8F91" }), _jsx("button", { type: "button", className: "staff-action-link staff-action-link-danger", onClick: () => handleDeleteStaff(row), children: "\u5220\u9664" })] })),
        },
    ];
    const teacherColumns = [
        { title: '姓名', dataIndex: 'name', align: 'left' },
        { title: '手机号', dataIndex: 'phone', width: 200, align: 'center' },
    ];
    return (_jsxs(_Fragment, { children: [_jsxs(Modal, { title: modalTitle, open: open, onCancel: onClose, footer: null, width: "52%", centered: true, destroyOnHidden: true, rootClassName: "class-staff-modal", styles: { body: { padding: '12px 16px 16px' } }, children: [_jsx("div", { className: "staff-toolbar", children: tag === 'teacher' ? (_jsx(Button, { className: "staff-toolbar-btn", onClick: () => setImportOpen(true), children: "\u6279\u91CF\u5BFC\u5165" })) : (_jsx(Button, { className: "staff-toolbar-btn", onClick: openAdd, children: "\u65B0\u589E" })) }), _jsx(Table, { className: "class-staff-table", rowKey: "id", bordered: true, size: "middle", loading: isLoading, columns: columns, dataSource: data?.list ?? [], scroll: { y: 420 }, pagination: false, locale: { emptyText: '暂无数据' } })] }), _jsx(Modal, { title: editTitle || modalTitle, open: editOpen, onOk: () => saveMu.mutate(editForm.getFieldsValue()), onCancel: () => { setEditOpen(false); setEditTitle(''); editForm.resetFields(); }, confirmLoading: saveMu.isPending, okText: "\u786E\u5B9A", cancelText: "\u53D6\u6D88", width: "35%", destroyOnHidden: true, rootClassName: "class-staff-modal", children: _jsxs(Form, { form: editForm, labelCol: { style: { width: 80 } }, wrapperCol: { span: 16 }, children: [_jsx(Form.Item, { name: "id", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "train_id", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "classid", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "type", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "realname", label: "\u59D3\u540D", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u59D3\u540D" }) }), _jsx(Form.Item, { name: "phone", label: "\u624B\u673A\u53F7", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7" }) }), _jsx(Form.Item, { name: "id_card", label: "\u8EAB\u4EFD\u8BC1\u53F7", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8EAB\u4EFD\u8BC1\u53F7" }) })] }) }), _jsxs(Modal, { title: "\u6279\u91CF\u5BFC\u5165", open: importOpen, onCancel: () => { setImportOpen(false); setSelectedTeacherIds([]); }, footer: null, maskClosable: false, width: 520, destroyOnHidden: true, rootClassName: "class-staff-modal", children: [_jsx("div", { className: "staff-import-actions", children: _jsx(Button, { type: "primary", loading: importMu.isPending, onClick: () => {
                                if (!selectedTeacherIds.length) {
                                    message.error('请选择要导入的人员');
                                    return;
                                }
                                importMu.mutate({ traininfoId: trainInfoId, classId: classId, tag, ids: selectedTeacherIds });
                            }, children: "\u786E\u8BA4\u5BFC\u5165" }) }), _jsx(Table, { className: "class-staff-table", rowKey: "id", bordered: true, size: "middle", columns: teacherColumns, dataSource: importData?.list ?? [], rowSelection: {
                            columnWidth: 48,
                            selectedRowKeys: selectedTeacherIds,
                            onChange: (keys) => setSelectedTeacherIds(keys),
                        }, scroll: { y: 420 }, pagination: false, locale: { emptyText: '暂无数据' } })] })] }));
}
// ==================== 培训任务弹窗 ====================
function TrainTaskModal({ open, onClose, classId, kejianOptions, }) {
    const [editOpen, setEditOpen] = useState(false);
    const [editForm] = Form.useForm();
    const [editingId, setEditingId] = useState(null);
    const { data, isLoading, refetch } = useQuery({
        queryKey: ['class-tasks', classId],
        queryFn: () => trainApi.listClassTasks(classId),
        enabled: open && !!classId,
    });
    const saveMu = useMutation({
        mutationFn: (p) => trainApi.saveClassTask(p),
        onSuccess: () => { message.success('成功'); setEditOpen(false); refetch(); },
        onError: () => message.error('保存失败'),
    });
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 60, align: 'center' },
        { title: '课件名称', dataIndex: 'title', align: 'left' },
        { title: '日期', dataIndex: 'date', width: 160, align: 'center' },
        { title: '状态', dataIndex: 'status_cn', width: 160, align: 'center' },
        { title: '完成次数', dataIndex: 'times_cn', width: 160, align: 'center' },
        {
            title: '操作', key: 'act', width: 100, fixed: 'right', align: 'center',
            render: (_, r) => (_jsx(Button, { type: "link", size: "small", className: "link-action", onClick: () => {
                    setEditingId(r.id);
                    editForm.setFieldsValue({ ck_id: r.ck_id, date: r.date ? dayjs(r.date) : undefined });
                    setEditOpen(true);
                }, children: "\u7F16\u8F91" })),
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsxs(Modal, { title: "\u57F9\u8BAD\u4EFB\u52A1", open: open, onCancel: onClose, footer: null, width: "60%", destroyOnHidden: true, children: [_jsx("div", { className: "mar-b10", children: _jsx(Button, { onClick: () => {
                                setEditingId(null);
                                editForm.resetFields();
                                setEditOpen(true);
                            }, children: "\u65B0\u589E" }) }), _jsx(Table, { rowKey: "id", bordered: true, loading: isLoading, columns: columns, dataSource: data?.list ?? [], scroll: { y: 400 }, pagination: false, locale: { emptyText: '暂无培训任务' } })] }), _jsx(Modal, { title: "\u57F9\u8BAD\u4EFB\u52A1\u4FE1\u606F", open: editOpen, onOk: () => {
                    const v = editForm.getFieldsValue();
                    const date = v.date ? (dayjs.isDayjs(v.date) ? v.date.format('YYYY-MM-DD') : v.date) : undefined;
                    saveMu.mutate({ ...v, date, id: editingId ?? undefined, classId: classId });
                }, onCancel: () => setEditOpen(false), confirmLoading: saveMu.isPending, okText: "\u786E\u5B9A", cancelText: "\u53D6\u6D88", width: "50%", destroyOnHidden: true, children: _jsxs(Form, { form: editForm, labelCol: { style: { width: 80 } }, wrapperCol: { span: 16 }, children: [_jsx(Form.Item, { name: "ck_id", label: "\u8BFE\u4EF6\u540D\u79F0", children: _jsx(Select, { showSearch: true, filterOption: (input, option) => (option?.label || '').includes(input), placeholder: "\u8BF7\u9009\u8BFE\u4EF6\u540D\u79F0", allowClear: true, options: kejianOptions }) }), _jsx(Form.Item, { name: "date", label: "\u65E5\u671F", children: _jsx(DatePicker, { style: { width: '100%' }, format: "YYYY-MM-DD" }) })] }) })] }));
}
// ==================== 主页面 ====================
export function TrainingClassPeoplePage() {
    const processStep = 3;
    const queryClient = useQueryClient();
    const canCreateTrainInfo = useAuthStore((s) => s.permissionCodes.includes('train:info-list:create'));
    const canUpdateTrainInfo = useAuthStore((s) => s.permissionCodes.includes('train:info-list:update'));
    const [form] = Form.useForm();
    const [trainInfoForm] = Form.useForm();
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize] = useState(20);
    const [keywordInput, setKeywordInput] = useState('');
    const [keyword, setKeyword] = useState('');
    const [companyIdFilter, setCompanyIdFilter] = useState();
    const [workTypeIdFilter, setWorkTypeIdFilter] = useState();
    const [modalOpen, setModalOpen] = useState(false);
    const [isCreateMode, setIsCreateMode] = useState(true);
    const [trainInfoModalOpen, setTrainInfoModalOpen] = useState(false);
    const [editingTrainInfoId, setEditingTrainInfoId] = useState(null);
    const [trainInfoDetailLoading, setTrainInfoDetailLoading] = useState(false);
    const selectedTrainInfoId = Form.useWatch('trainInfoId', form);
    /** 本页刚创建的培训，避免 publicData 刷新前 Select 无选项 */
    const [extraTrainInfoOptions, setExtraTrainInfoOptions] = useState([]);
    /** 手填企业名，并入下拉（含本页刚录入、以及历史培训信息里的自由文本企业） */
    const [extraCompanyNames, setExtraCompanyNames] = useState([]);
    const [companySearch, setCompanySearch] = useState('');
    const [kcbiaoSrc, setKcbiaoSrc] = useState('');
    const [uploadOpen, setUploadOpen] = useState(false);
    const [previewTarget, setPreviewTarget] = useState(null);
    const [studentModal, setStudentModal] = useState(false);
    const [studentModalMode, setStudentModalMode] = useState('roster');
    const [staffModal, setStaffModal] = useState(false);
    const [chargeModal, setChargeModal] = useState(false);
    const [rolesModal, setRolesModal] = useState(false);
    const [taskModal, setTaskModal] = useState(false);
    const [selectedClass, setSelectedClass] = useState(null);
    const { data: publicData } = useQuery({
        queryKey: ['class-people-public-data'],
        queryFn: () => trainApi.getDemandPublicData(['company', 'worktype', 'traininfo', 'kejian']),
        staleTime: 5 * 60 * 1000,
    });
    const { data: listData, isLoading, isError, refetch } = useQuery({
        queryKey: ['info-classes', currentPage, pageSize, companyIdFilter, workTypeIdFilter, keyword],
        queryFn: () => trainApi.pageInfoClassList({
            page: currentPage,
            pageSize,
            companyId: companyIdFilter,
            workTypeId: workTypeIdFilter,
            keyword: keyword || undefined,
        }),
    });
    const saveMu = useMutation({
        mutationFn: (p) => trainApi.saveInfoClass(p),
        onSuccess: () => {
            message.success('成功');
            closeModal();
            queryClient.invalidateQueries({ queryKey: ['info-classes'] });
        },
        onError: () => message.error('保存失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.batchDeleteInfoClasses([id]),
        onSuccess: () => {
            message.success('删除成功');
            queryClient.invalidateQueries({ queryKey: ['info-classes'] });
        },
        onError: () => message.error('删除失败'),
    });
    const createTrainInfoMu = useMutation({
        mutationFn: (payload) => unwrap(apiClient.post('/training/info-lists', payload, { silentError: true })),
        onSuccess: async (created) => {
            message.success('培训信息已创建');
            const createdId = String(created.id);
            const createdTitle = String(created.title ?? '').trim()
                || String(trainInfoForm.getFieldValue('title') ?? '').trim()
                || `培训#${createdId}`;
            setExtraTrainInfoOptions((prev) => [
                { value: createdId, label: createdTitle },
                ...prev.filter((o) => String(o.value) !== createdId),
            ]);
            await queryClient.invalidateQueries({ queryKey: ['class-people-public-data'] });
            await queryClient.invalidateQueries({ queryKey: ['exam-score-import-public'] });
            const patch = { trainInfoId: createdId };
            const classVals = form.getFieldsValue();
            if (classVals.nums == null && created.num != null) {
                patch.nums = created.num;
            }
            if (!classVals.dateStart && created.startDate) {
                patch.dateStart = dayjs(created.startDate);
            }
            if (!classVals.dateEnd && created.endDate) {
                patch.dateEnd = dayjs(created.endDate);
            }
            form.setFieldsValue(patch);
            setTrainInfoModalOpen(false);
            trainInfoForm.resetFields();
            setEditingTrainInfoId(null);
        },
        onError: (err) => {
            const status = err?.response?.status;
            const dataMsg = err?.response?.data?.message;
            const msg = dataMsg || (err instanceof Error ? err.message : '');
            if (status === 403 || /权限|forbidden|access denied/i.test(msg)) {
                message.error('无创建培训信息权限（需要 train:info-list:create）');
                return;
            }
            message.error(msg || '创建培训信息失败');
        },
    });
    const updateTrainInfoMu = useMutation({
        mutationFn: ({ id, payload }) => trainApi.updateInfoList(id, payload),
        onSuccess: async (updated, vars) => {
            message.success('培训信息已保存');
            const updatedId = String(updated.id ?? vars.id);
            const label = String(updated.title ?? '').trim()
                || String(trainInfoForm.getFieldValue('title') ?? '').trim()
                || `培训#${updatedId}`;
            setExtraTrainInfoOptions((prev) => {
                const next = prev.filter((o) => String(o.value) !== String(vars.id) && String(o.value) !== updatedId);
                return [{ value: updatedId, label }, ...next];
            });
            if (String(form.getFieldValue('trainInfoId') ?? '') === String(vars.id)) {
                form.setFieldValue('trainInfoId', updatedId);
            }
            await queryClient.invalidateQueries({ queryKey: ['class-people-public-data'] });
            await queryClient.invalidateQueries({ queryKey: ['exam-score-import-public'] });
            await queryClient.invalidateQueries({ queryKey: ['info-classes'] });
            setTrainInfoModalOpen(false);
            trainInfoForm.resetFields();
            setEditingTrainInfoId(null);
        },
        onError: (err) => {
            const status = err?.response?.status;
            const dataMsg = err?.response?.data?.message;
            const msg = dataMsg || (err instanceof Error ? err.message : '');
            if (status === 403 || /权限|forbidden|access denied/i.test(msg)) {
                message.error('无编辑培训信息权限（需要 train:info-list:update）');
                return;
            }
            message.error(msg || '保存培训信息失败');
        },
    });
    const workTypeGroupedOptions = useMemo(() => {
        return publicData?.worktype_list?.map((g) => ({
            label: g.title,
            options: (g.options ?? []).map((o) => ({ value: o.id, label: o.title })),
        })) ?? [];
    }, [publicData]);
    const companyOptions = useMemo(() => {
        return publicData?.company_list?.map((c) => ({
            value: c.id,
            label: c.title,
        })) ?? [];
    }, [publicData]);
    const companyNameOptions = useMemo(() => {
        const seen = new Set();
        const names = [];
        const add = (raw) => {
            const n = String(raw ?? '').trim();
            if (!n)
                return;
            const key = n.toLowerCase();
            if (seen.has(key))
                return;
            seen.add(key);
            names.push(n);
        };
        for (const c of companyOptions)
            add(c.label);
        for (const t of publicData?.traininfo_list ?? []) {
            add(t.company_name);
        }
        for (const n of extraCompanyNames)
            add(n);
        add(companySearch);
        return names.map((value) => ({ value, label: value }));
    }, [companyOptions, publicData, extraCompanyNames, companySearch]);
    const trainInfoOptions = useMemo(() => {
        const map = new Map();
        for (const t of publicData?.traininfo_list ?? []) {
            const id = String(t.id);
            const title = String(t.title ?? '').trim();
            map.set(id, { value: id, label: title || `培训#${id}` });
        }
        for (const o of extraTrainInfoOptions) {
            const id = String(o.value);
            const label = String(o.label ?? '').trim();
            const existing = map.get(id);
            // 本页刚建的标题优先，避免 publicData 刷新前/空标题时 Select 只显示 id
            if (!existing || !existing.label || existing.label.startsWith('培训#')) {
                map.set(id, { value: id, label: label || existing?.label || `培训#${id}` });
            }
            else if (label && !label.startsWith('培训#')) {
                map.set(id, { value: id, label });
            }
        }
        return Array.from(map.values());
    }, [publicData, extraTrainInfoOptions]);
    const kejianOptions = useMemo(() => {
        return publicData?.kejian_list?.map((k) => ({
            value: Number(k.id),
            label: k.title,
        })) ?? [];
    }, [publicData]);
    const closeModal = () => {
        setModalOpen(false);
        setTrainInfoModalOpen(false);
        setEditingTrainInfoId(null);
        setKcbiaoSrc('');
        form.resetFields();
        trainInfoForm.resetFields();
    };
    const openCreate = () => {
        form.resetFields();
        setKcbiaoSrc('');
        setIsCreateMode(true);
        setModalOpen(true);
    };
    const openEdit = (row) => {
        setIsCreateMode(false);
        form.setFieldsValue({
            id: row.id,
            title: row.title,
            trainInfoId: row.train_info_id != null ? String(row.train_info_id) : undefined,
            workTypeId: toPositiveInt(row.work_type_id),
            workTypeLevel: toPositiveInt(row.work_type_level),
            nums: row.nums,
            dateStart: row.date_start ? dayjs(row.date_start) : undefined,
            dateEnd: row.date_end ? dayjs(row.date_end) : undefined,
            kcbiaoFile: row.kcbiao_file,
            kcbiaoFileDisplay: row.kcbiao_file,
            companyContactName: row.company_contact_name,
            companyContactPhone: row.company_contact_phone,
        });
        setKcbiaoSrc(row.kcbiao_src ?? resolveFileUrl(row.kcbiao_file));
        setModalOpen(true);
    };
    const openCreateTrainInfo = () => {
        if (!canCreateTrainInfo) {
            message.error('无创建培训信息权限（需要 train:info-list:create）');
            return;
        }
        setEditingTrainInfoId(null);
        trainInfoForm.resetFields();
        setCompanySearch('');
        setTrainInfoModalOpen(true);
    };
    const openEditTrainInfo = async () => {
        const id = toApiId(form.getFieldValue('trainInfoId'));
        if (id == null) {
            message.warning('请先选择培训信息');
            return;
        }
        if (!canUpdateTrainInfo) {
            message.error('无编辑培训信息权限（需要 train:info-list:update）');
            return;
        }
        setTrainInfoDetailLoading(true);
        try {
            const detail = await trainApi.getInfoList(id);
            const raw = detail;
            setEditingTrainInfoId(id);
            setCompanySearch('');
            trainInfoForm.setFieldsValue({
                title: detail.title,
                companyName: detail.companyName ?? raw.company_name,
            });
            const companyName = String(detail.companyName ?? raw.company_name ?? '').trim();
            if (companyName
                && !companyOptions.some((c) => String(c.label).trim() === companyName)) {
                setExtraCompanyNames((prev) => prev.some((n) => n.toLowerCase() === companyName.toLowerCase())
                    ? prev
                    : [...prev, companyName]);
            }
            setTrainInfoModalOpen(true);
        }
        catch (err) {
            const status = err?.response?.status;
            const dataMsg = err?.response?.data?.message;
            const msg = dataMsg || (err instanceof Error ? err.message : '');
            if (status === 403 || /权限|forbidden|access denied/i.test(msg)) {
                message.error('无查看培训信息权限（需要 train:info-list:view）');
                return;
            }
            message.error(msg || '加载培训信息失败');
        }
        finally {
            setTrainInfoDetailLoading(false);
        }
    };
    const handleSubmitTrainInfo = async () => {
        try {
            const v = await trainInfoForm.validateFields();
            if (v.startDate && v.endDate && dayjs(v.endDate).isBefore(dayjs(v.startDate), 'day')) {
                message.error('结束日期不能早于开始日期');
                return;
            }
            const companyName = String(v.companyName ?? '').trim();
            if (!companyName) {
                message.error('请选择或输入企业');
                return;
            }
            const matchedCompany = companyOptions.find((c) => String(c.label).trim() === companyName);
            if (!matchedCompany) {
                setExtraCompanyNames((prev) => prev.some((n) => n.toLowerCase() === companyName.toLowerCase())
                    ? prev
                    : [...prev, companyName]);
            }
            const payload = {
                title: v.title,
                companyId: matchedCompany?.value,
                companyName,
                num: v.num,
                startDate: v.startDate ? dayjs(v.startDate).format('YYYY-MM-DD') : undefined,
                endDate: v.endDate ? dayjs(v.endDate).format('YYYY-MM-DD') : undefined,
            };
            if (editingTrainInfoId != null) {
                updateTrainInfoMu.mutate({ id: editingTrainInfoId, payload });
                return;
            }
            createTrainInfoMu.mutate({ ...payload, status: 0 });
        }
        catch {
            // Form validation errors are shown inline
        }
    };
    const handleSubmit = async () => {
        try {
            const v = await form.validateFields();
            if (v.dateStart && v.dateEnd && dayjs(v.dateEnd).isBefore(dayjs(v.dateStart), 'day')) {
                message.error('结束日期不能早于开始日期');
                return;
            }
            const payload = {
                id: v.id,
                title: v.title,
                trainInfoId: v.trainInfoId,
                workTypeId: toPositiveInt(v.workTypeId),
                workTypeLevel: toPositiveInt(v.workTypeLevel),
                nums: Number(v.nums),
                dateStart: dayjs.isDayjs(v.dateStart) ? v.dateStart.format('YYYY-MM-DD') : String(v.dateStart),
                dateEnd: dayjs.isDayjs(v.dateEnd) ? v.dateEnd.format('YYYY-MM-DD') : String(v.dateEnd),
                kcbiaoFile: v.kcbiaoFile,
                companyContactName: String(v.companyContactName ?? '').trim(),
                companyContactPhone: String(v.companyContactPhone ?? '').trim(),
            };
            saveMu.mutate(payload);
        }
        catch {
            // Form validation errors are shown inline
        }
    };
    const kcbiaoUploadProps = {
        accept: '.xls,.xlsx',
        maxCount: 1,
        showUploadList: false,
        customRequest: async (options) => {
            const { file, onSuccess, onError } = options;
            try {
                const result = await uploadFile(file, 'public');
                const url = `/api/files/${result.id}/download`;
                form.setFieldsValue({ kcbiaoFile: url, kcbiaoFileDisplay: url });
                message.success('上传成功');
                setUploadOpen(false);
                onSuccess?.(result, file);
            }
            catch (err) {
                message.error('上传失败');
                onError?.(err);
            }
        },
    };
    const openPreview = (src) => {
        const url = resolveFileUrl(src);
        if (!url)
            return;
        setPreviewTarget({
            fileUrl: url,
            fileName: fileNameFromUrl(url),
        });
    };
    const handleFilterChange = () => {
        setCurrentPage(1);
    };
    const handleReset = () => {
        setKeywordInput('');
        setKeyword('');
        setCompanyIdFilter(undefined);
        setWorkTypeIdFilter(undefined);
        setCurrentPage(1);
    };
    const applyKeywordSearch = () => {
        setKeyword(keywordInput.trim());
        setCurrentPage(1);
    };
    const showCreateTrainInfoBtn = canCreateTrainInfo && isCreateMode;
    const showEditTrainInfoBtn = canUpdateTrainInfo && selectedTrainInfoId != null;
    const trainInfoSelectExtraPx = (showCreateTrainInfoBtn ? 88 : 0) + (showEditTrainInfoBtn ? 72 : 0);
    const openSubDialog = (type, row) => {
        const id = toApiId(row.id);
        const trainInfoId = toApiId(row.train_info_id) ?? 0;
        if (id == null) {
            message.error('班级 ID 无效');
            return;
        }
        setSelectedClass({
            id,
            trainInfoId,
            title: row.title,
            workTypeTitle: row.work_type_title,
            workTypeLevel: toPositiveInt(row.work_type_level),
            workTypeId: toPositiveInt(row.work_type_id),
        });
        switch (type) {
            case 'students':
                setStudentModalMode('roster');
                setStudentModal(true);
                break;
            case 'collection':
                setStudentModalMode('collection');
                setStudentModal(true);
                break;
            case 'staff':
                setStaffModal(true);
                break;
            case 'charge':
                setChargeModal(true);
                break;
            case 'roles':
                setRolesModal(true);
                break;
            case 'tasks':
                setTaskModal(true);
                break;
        }
    };
    const closeStudentModal = () => {
        setStudentModal(false);
    };
    const formatCreateTime = (row) => {
        if (row.addtime_cn)
            return row.addtime_cn;
        if (row.addtime)
            return dayjs.unix(row.addtime).format('YYYY-MM-DD HH:mm:ss');
        return '-';
    };
    const formatDateRange = (row) => {
        if (!row.date_start && !row.date_end)
            return '-';
        return `${row.date_start ?? '—'} ~ ${row.date_end ?? '—'}`;
    };
    const handleDeleteClass = (row) => {
        Modal.confirm({
            title: '友情提示',
            content: (_jsxs("div", { children: [_jsxs("p", { children: ["\u786E\u5B9A\u5220\u9664\u73ED\u7EA7\u300C", row.title, "\u300D\u5417\uFF1F\u5C06\u540C\u65F6\uFF1A"] }), _jsxs("ul", { style: { margin: '8px 0 0', paddingLeft: 20 }, children: [_jsx("li", { children: "\u5220\u9664\u672C\u73ED\u82B1\u540D\u518C\u3001\u57F9\u8BAD\u4EFB\u52A1\u3001\u8D44\u6599\u6536\u96C6\u8BB0\u5F55" }), _jsx("li", { children: "\u628A\u5BF9\u5E94\u540D\u5355\u4EBA\u5458\u56DE\u6EDA\u4E3A\u300C\u5F85\u5206\u73ED\u300D\uFF0C\u4FBF\u4E8E\u91CD\u65B0\u5206\u73ED" }), _jsx("li", { children: "\u89E3\u7ED1\u6392\u8BFE\u8BB0\u5F55\u4E2D\u7684\u73ED\u7EA7\u5173\u8054" })] }), _jsx("p", { style: { marginTop: 8, color: '#666' }, children: "\u4E0D\u4F1A\u5220\u9664\u5B66\u5458 App \u8D26\u53F7\uFF08\u5B66\u5458\u4FE1\u606F\u4ECD\u4FDD\u7559\uFF09\u3002" })] })),
            onOk: () => deleteMu.mutate(row.id),
        });
    };
    const renderRowActions = (row) => (_jsxs("div", { className: "class-row-actions", children: [_jsxs("div", { className: "class-row-actions-line", children: [_jsx("button", { type: "button", className: "class-action-link", onClick: () => openSubDialog('students', row), children: "\u5B66\u5458\u5217\u8868" }), _jsx("button", { type: "button", className: "class-action-link", onClick: () => openSubDialog('collection', row), children: "\u8D44\u6599\u6536\u96C6" }), _jsx("button", { type: "button", className: "class-action-link", onClick: () => openSubDialog('roles', row), children: "\u73ED\u7EA7\u89D2\u8272" })] }), _jsxs("div", { className: "class-row-actions-line", children: [_jsx("button", { type: "button", className: "class-action-link", onClick: () => openSubDialog('charge', row), children: "\u8D1F\u8D23\u4EBA" }), _jsx("button", { type: "button", className: "class-action-link", onClick: () => openEdit(row), children: "\u7F16\u8F91" }), _jsx("button", { type: "button", className: "class-action-link class-action-link-danger", onClick: () => handleDeleteClass(row), children: "\u5220\u9664" })] })] }));
    const columns = [
        {
            title: '序号',
            key: 'index',
            width: 60,
            align: 'center',
            render: (_, __, i) => (currentPage - 1) * pageSize + i + 1,
        },
        {
            title: '班级名称',
            dataIndex: 'title',
            align: 'left',
            width: 280,
            className: 'class-wrap-cell',
            onCell: () => ({ className: 'class-wrap-cell' }),
        },
        {
            title: '公司',
            dataIndex: 'company_name',
            align: 'left',
            width: 180,
            className: 'class-wrap-cell',
            onCell: () => ({ className: 'class-wrap-cell' }),
        },
        { title: '工种', dataIndex: 'work_type_title', align: 'center', width: 140 },
        {
            title: '等级',
            dataIndex: 'work_type_level',
            align: 'center',
            width: 130,
            render: (v) => workLevelLabel(v) || '-',
        },
        { title: '人数', dataIndex: 'nums', width: 72, align: 'center' },
        {
            title: '培训周期',
            key: 'dateRange',
            width: 200,
            align: 'center',
            render: (_, row) => formatDateRange(row),
        },
        {
            title: '资料进度',
            key: 'profile',
            width: 120,
            align: 'center',
            render: (_, row) => {
                const approved = Number(row.profile_approved ?? 0);
                const total = Number(row.profile_total ?? 0);
                if (!row.collection_sign_up_id && total === 0) {
                    return (_jsx("button", { type: "button", className: "class-action-link", onClick: () => openSubDialog('collection', row), children: "\u53BB\u6536\u96C6" }));
                }
                return (_jsxs("button", { type: "button", className: "class-action-link", style: { color: row.profile_ready ? '#389e0d' : undefined }, onClick: () => openSubDialog('collection', row), title: "\u6253\u5F00\u8D44\u6599\u6536\u96C6", children: [approved, "/", total, row.profile_ready ? ' ✓' : ''] }));
            },
        },
        {
            title: '创建时间',
            key: 'createTime',
            width: 174,
            align: 'center',
            render: (_, row) => formatCreateTime(row),
        },
        {
            title: '操作',
            key: 'act',
            width: 280,
            align: 'center',
            fixed: 'right',
            render: (_, row) => renderRowActions(row),
        },
    ];
    return (_jsxs("div", { className: "training-class-people-page training-page", children: [_jsxs("section", { className: "page-heading", children: [_jsx(Typography.Title, { level: 4, children: "\u5F00\u73ED\u914D\u7F6E" }), _jsx(Typography.Text, { type: "secondary", children: "\u7EF4\u62A4\u5DF2\u5206\u73ED\u73ED\u7EA7\u7684\u5B66\u5458\u82B1\u540D\u518C\u3001\u6559\u52A1\u8D1F\u8D23\u4EBA\u3001\u57F9\u8BAD\u4EFB\u52A1\u4E0E\u8D44\u6599\u6536\u96C6\uFF1B\u8BD5\u9898\u914D\u7F6E\u4E0E\u5237\u9898\u5B66\u60C5\u8BF7\u5230\u300C\u8DDF\u73ED\u300D\u5DE5\u4F5C\u53F0" })] }), _jsx(TrainingProcessSteps, { current: processStep }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsx("div", { className: "search-filter-bar", children: _jsxs("div", { className: "filter-actions", children: [_jsx(Input.Search, { className: "filter-keyword", allowClear: true, placeholder: "\u641C\u7D22\u73ED\u7EA7\u540D\u79F0", value: keywordInput, onChange: (e) => setKeywordInput(e.target.value), onSearch: applyKeywordSearch, enterButton: "\u67E5\u8BE2" }), _jsx(Select, { className: "filter-select", allowClear: true, showSearch: true, placeholder: "\u4F01\u4E1A\u7B5B\u9009", value: companyIdFilter, onChange: (v) => {
                                    setCompanyIdFilter(v);
                                    handleFilterChange();
                                }, options: companyOptions, optionFilterProp: "label" }), _jsx(Select, { className: "filter-select", allowClear: true, showSearch: true, placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD", value: workTypeIdFilter, onChange: (v) => {
                                    setWorkTypeIdFilter(v);
                                    handleFilterChange();
                                }, options: workTypeGroupedOptions, optionFilterProp: "label" }), _jsx(Button, { onClick: handleReset, children: "\u91CD\u7F6E" }), _jsx(Button, { type: "primary", onClick: openCreate, children: "\u521B\u5EFA\u73ED\u7EA7" }), isError ? _jsx(Button, { onClick: () => refetch(), children: "\u91CD\u65B0\u52A0\u8F7D" }) : null] }) }) }), _jsx(Table, { className: "class-people-table", rowKey: "id", bordered: true, size: "middle", loading: isLoading, columns: columns, dataSource: listData?.list ?? [], scroll: { x: 1280, y: 600 }, pagination: false, locale: { emptyText: isError ? '加载失败，请点击重新加载' : '暂无班级，可点击「创建班级」或先完成上游分班' } }), _jsx(Pagination, { className: "class-people-pagination", current: currentPage, pageSize: pageSize, total: listData?.total ?? 0, showSizeChanger: false, showQuickJumper: true, showTotal: (t) => `共 ${t} 条`, onChange: (p) => setCurrentPage(p) }), _jsx(Modal, { title: "\u73ED\u7EA7\u4FE1\u606F", open: modalOpen, onOk: handleSubmit, onCancel: closeModal, confirmLoading: saveMu.isPending, okText: "\u786E\u5B9A", cancelText: "\u53D6\u6D88", destroyOnHidden: true, width: "50%", children: _jsxs(Form, { form: form, labelCol: { style: { width: 120 } }, wrapperCol: { span: 16 }, children: [_jsx(Form.Item, { name: "id", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "title", label: "\u73ED\u7EA7\u540D\u79F0", rules: [{ required: true, message: '请填写班级名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u73ED\u7EA7\u540D\u79F0" }) }), _jsx(Form.Item, { label: "\u9009\u62E9\u57F9\u8BAD\u4FE1\u606F", required: true, children: _jsxs(Space.Compact, { style: { width: '100%' }, children: [_jsx(Form.Item, { name: "trainInfoId", noStyle: true, rules: [{ required: true, message: '请选择培训信息' }], children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u57F9\u8BAD\u4FE1\u606F", options: trainInfoOptions, showSearch: true, optionFilterProp: "label", optionLabelProp: "label", style: {
                                                width: trainInfoSelectExtraPx > 0 ? `calc(100% - ${trainInfoSelectExtraPx}px)` : '100%',
                                            }, onChange: (id) => {
                                                if (form.getFieldValue('workTypeId'))
                                                    return;
                                                const matched = publicData?.traininfo_list
                                                    ?.find((t) => String(t.id) === String(id));
                                                if (matched?.work_type_id) {
                                                    form.setFieldValue('workTypeId', matched.work_type_id);
                                                }
                                            } }) }), showCreateTrainInfoBtn ? (_jsx(Button, { icon: _jsx(PlusOutlined, {}), onClick: openCreateTrainInfo, children: "\u65B0\u5EFA" })) : null, showEditTrainInfoBtn ? (_jsx(Button, { icon: _jsx(EditOutlined, {}), onClick: openEditTrainInfo, loading: trainInfoDetailLoading, children: "\u7F16\u8F91" })) : null] }) }), _jsx(Form.Item, { name: "workTypeId", label: "\u9884\u7EA6\u5DE5\u79CD", rules: [{ required: true, message: '请选择工种' }], children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u672C\u73ED\u5DE5\u79CD", options: workTypeGroupedOptions, showSearch: true, optionFilterProp: "label" }) }), _jsx(Form.Item, { name: "workTypeLevel", label: "\u9884\u7EA6\u5DE5\u79CD\u7B49\u7EA7", rules: [{ required: true, message: '请选择工种等级' }], children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u672C\u73ED\u7B49\u7EA7", options: WORK_LEVELS }) }), _jsx(Form.Item, { name: "nums", label: "\u73ED\u7EA7\u4EBA\u6570", rules: [{ required: true, message: '请填写班级人数' }], children: _jsx(Input, { type: "number", placeholder: "\u8BF7\u8F93\u5165\u73ED\u7EA7\u4EBA\u6570", min: 1 }) }), _jsx(Form.Item, { name: "companyContactName", label: "\u5355\u4F4D\u8054\u7CFB\u4EBA", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u5355\u4F4D\u8054\u7CFB\u4EBA\uFF08\u7528\u4E8E\u5DE5\u4F5C\u5E74\u9650\u8BC1\u660E\uFF09", maxLength: 64 }) }), _jsx(Form.Item, { name: "companyContactPhone", label: "\u8054\u7CFB\u65B9\u5F0F", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8054\u7CFB\u65B9\u5F0F\uFF08\u7528\u4E8E\u5DE5\u4F5C\u5E74\u9650\u8BC1\u660E\uFF09", maxLength: 32 }) }), _jsx(Form.Item, { name: "dateStart", label: "\u5F00\u59CB\u65E5\u671F", rules: [{ required: true, message: '请选择开始日期' }], children: _jsx(DatePicker, { style: { width: '100%' }, format: "YYYY-MM-DD", placeholder: "\u9009\u62E9\u5F00\u59CB\u65E5\u671F" }) }), _jsx(Form.Item, { name: "dateEnd", label: "\u7ED3\u675F\u65E5\u671F", rules: [{ required: true, message: '请选择结束日期' }], children: _jsx(DatePicker, { style: { width: '100%' }, format: "YYYY-MM-DD", placeholder: "\u9009\u62E9\u7ED3\u675F\u65E5\u671F" }) }), _jsxs(Form.Item, { label: "\u8BFE\u7A0B\u8868", children: [_jsx(Form.Item, { noStyle: true, shouldUpdate: (p, c) => p.kcbiaoFileDisplay !== c.kcbiaoFileDisplay || p.kcbiaoFile !== c.kcbiaoFile, children: () => {
                                        const fileVal = form.getFieldValue('kcbiaoFileDisplay');
                                        return (_jsxs("div", { className: "kcbiao-upload-row", children: [_jsx(Form.Item, { name: "kcbiaoFileDisplay", noStyle: true, children: _jsx(Input, { readOnly: true, placeholder: "\u8BF7\u4E0A\u4F20\u8BFE\u8868", style: { flex: 1 } }) }), _jsx(Button, { onClick: () => setUploadOpen(true), children: "\u4E0A\u4F20" }), fileVal ? (_jsx(Button, { type: "link", style: { marginLeft: 20 }, onClick: () => openPreview(kcbiaoSrc || form.getFieldValue('kcbiaoFile')), children: "\u9884\u89C8" })) : null] }));
                                    } }), _jsx(Form.Item, { name: "kcbiaoFile", hidden: true, children: _jsx(Input, {}) })] })] }) }), _jsxs(Modal, { title: editingTrainInfoId != null ? '编辑培训信息' : '新建培训信息', open: trainInfoModalOpen, onOk: handleSubmitTrainInfo, onCancel: () => {
                    setTrainInfoModalOpen(false);
                    setEditingTrainInfoId(null);
                    trainInfoForm.resetFields();
                }, confirmLoading: createTrainInfoMu.isPending || updateTrainInfoMu.isPending, okText: editingTrainInfoId != null ? '保存' : '创建并选用', cancelText: "\u53D6\u6D88", destroyOnHidden: true, width: 520, children: [_jsx(Typography.Paragraph, { type: "secondary", style: { marginTop: 0 }, children: editingTrainInfoId != null
                            ? '修改标题、企业后保存。工种和等级请在班级信息里按班填写，互不影响。'
                            : '创建后自动选入上方「选择培训信息」。同一企业可挂多个班级，各班工种和等级分别设置。' }), _jsxs(Form, { form: trainInfoForm, labelCol: { style: { width: 100 } }, wrapperCol: { span: 16 }, children: [_jsx(Form.Item, { name: "title", label: "\u57F9\u8BAD\u4FE1\u606F", rules: [{ required: true, message: '请填写培训信息' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u57F9\u8BAD\u4FE1\u606F\u6807\u9898", maxLength: 255 }) }), _jsx(Form.Item, { name: "companyName", label: "\u4F01\u4E1A\u540D\u79F0", rules: [{ required: true, message: '请选择或输入企业' }], children: _jsx(Select, { allowClear: true, showSearch: true, style: { width: '100%' }, placeholder: "\u8BF7\u9009\u62E9\u6216\u8F93\u5165\u4F01\u4E1A", options: companyNameOptions, searchValue: companySearch, onSearch: setCompanySearch, filterOption: (input, option) => String(option?.label ?? '')
                                        .toLowerCase()
                                        .includes(input.trim().toLowerCase()), onChange: (v) => {
                                        setCompanySearch('');
                                        const name = String(v ?? '').trim();
                                        if (name
                                            && !companyOptions.some((c) => String(c.label).trim() === name)) {
                                            setExtraCompanyNames((prev) => prev.some((n) => n.toLowerCase() === name.toLowerCase())
                                                ? prev
                                                : [...prev, name]);
                                        }
                                    }, onBlur: () => {
                                        const typed = companySearch.trim();
                                        if (!typed)
                                            return;
                                        trainInfoForm.setFieldValue('companyName', typed);
                                        if (!companyOptions.some((c) => String(c.label).trim() === typed)) {
                                            setExtraCompanyNames((prev) => prev.some((n) => n.toLowerCase() === typed.toLowerCase())
                                                ? prev
                                                : [...prev, typed]);
                                        }
                                        setCompanySearch('');
                                    }, onInputKeyDown: (e) => {
                                        if (e.key !== 'Enter')
                                            return;
                                        const typed = companySearch.trim();
                                        if (!typed)
                                            return;
                                        e.preventDefault();
                                        trainInfoForm.setFieldValue('companyName', typed);
                                        if (!companyOptions.some((c) => String(c.label).trim() === typed)) {
                                            setExtraCompanyNames((prev) => prev.some((n) => n.toLowerCase() === typed.toLowerCase())
                                                ? prev
                                                : [...prev, typed]);
                                        }
                                        setCompanySearch('');
                                    } }) })] })] }), _jsx(Modal, { title: "\u4E0A\u4F20\u6587\u4EF6", open: uploadOpen, width: 500, onCancel: () => setUploadOpen(false), footer: null, maskClosable: false, destroyOnHidden: true, children: _jsxs(Dragger, { ...kcbiaoUploadProps, children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(InboxOutlined, {}) }), _jsxs("p", { className: "ant-upload-text", children: ["\u5C06\u6587\u4EF6\u62D6\u5230\u6B64\u5904\uFF0C\u6216", _jsx("em", { children: "\u70B9\u51FB\u4E0A\u4F20" })] }), _jsx("p", { style: { color: '#ccc', marginTop: 10 }, children: "\u63D0\u793A:\u4E0A\u4F20\u8FC7\u7A0B\u4E2D,\u8BF7\u4E0D\u8981\u505A\u4EFB\u4F55\u64CD\u4F5C" }), _jsx("p", { className: "ant-upload-hint", children: "\u53EA\u80FD\u4E0A\u4F20.xls,.xlsx\u6587\u4EF6" })] }) }), _jsx(FilePreviewModal, { open: previewTarget != null, target: previewTarget, onClose: () => setPreviewTarget(null) }), _jsx(StudentListModal, { open: studentModal, onClose: closeStudentModal, classId: selectedClass?.id ?? null, trainInfoId: selectedClass?.trainInfoId ?? null, workTypeTitle: selectedClass?.workTypeTitle, workTypeLevel: selectedClass?.workTypeLevel, workTypeId: selectedClass?.workTypeId, mode: studentModalMode, onProfileChanged: () => queryClient.invalidateQueries({ queryKey: ['info-classes'] }) }), _jsx(StaffModal, { open: staffModal, onClose: () => setStaffModal(false), classId: selectedClass?.id ?? null, trainInfoId: selectedClass?.trainInfoId ?? null, tag: "teacher", title: "\u6559\u52A1\u4EBA\u5458" }), _jsx(StaffModal, { open: chargeModal, onClose: () => setChargeModal(false), classId: selectedClass?.id ?? null, trainInfoId: selectedClass?.trainInfoId ?? null, tag: "fuzhe", title: "\u8D1F\u8D23\u4EBA" }), _jsx(ClassRolesModal, { open: rolesModal, onClose: () => setRolesModal(false), classId: selectedClass?.id ?? null }), _jsx(TrainTaskModal, { open: taskModal, onClose: () => setTaskModal(false), classId: selectedClass?.id ?? null, kejianOptions: kejianOptions })] }));
}
