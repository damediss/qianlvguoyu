import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 企业培训需求采集 — 对齐 yun_shang company/DemandTrain.vue
 */
import { useMemo, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Pagination, Space, Table, Tag, Typography, message, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { downloadUrlFile, fileNameFromUrl } from '../../api/files';
import { SolutionDocumentPreview } from '../../components/SolutionDocumentPreview';
import { useAuthStore } from '../../stores/authStore';
import './EnterpriseTrainingDemandPage.css';
import { TRAINING_PROCESS_STEP_ROUTES } from './TrainingProcessSteps';
const ENTERPRISE_DEMAND_STEP_ROUTES = [
    'train_demand-train',
    ...TRAINING_PROCESS_STEP_ROUTES.slice(1),
];
function formatAddtime(ts) {
    if (!ts)
        return '-';
    const d = new Date(ts * 1000);
    return d.toLocaleString('zh-CN', { hour12: false });
}
const DEMAND_STATUS = {
    0: { label: '待处理', color: 'default' },
    1: { label: '已处理', color: 'success' },
};
function resolveFileUrl(path) {
    if (!path)
        return '';
    if (path.startsWith('http://') || path.startsWith('https://'))
        return path;
    if (path.startsWith('/'))
        return path;
    return `/${path}`;
}
function fileTypeFromPath(path) {
    if (!path || !path.includes('.'))
        return '';
    const ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
    const slash = ext.indexOf('/');
    return slash >= 0 ? ext.substring(0, slash) : ext;
}
export function EnterpriseTrainingDemandPage() {
    const queryClient = useQueryClient();
    const orgId = useAuthStore((s) => s.currentUser?.orgId);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);
    const [formOpen, setFormOpen] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();
    const [submitting, setSubmitting] = useState(false);
    const [programOpen, setProgramOpen] = useState(false);
    const [currentDemandId, setCurrentDemandId] = useState(null);
    const [program, setProgram] = useState(null);
    const [rejectOpen, setRejectOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const [actionLoading, setActionLoading] = useState(false);
    const [studentOpen, setStudentOpen] = useState(false);
    const [students, setStudents] = useState([]);
    const queryParams = useMemo(() => ({ page, pageSize, orgId }), [page, pageSize, orgId]);
    const { data, isLoading } = useQuery({
        queryKey: ['enterprise-demand-trains', queryParams],
        queryFn: () => trainApi.pageDemandTrains(queryParams),
        enabled: orgId != null,
    });
    const openCreate = () => {
        setEditing(null);
        form.setFieldsValue({ content: '', nums: 0 });
        setFormOpen(true);
    };
    const openEdit = (row) => {
        setEditing(row);
        form.setFieldsValue({ content: row.content ?? '', nums: row.nums ?? 0 });
        setFormOpen(true);
    };
    const handleSubmit = async () => {
        const v = await form.validateFields();
        if (!orgId) {
            message.error('未获取到企业信息');
            return;
        }
        setSubmitting(true);
        try {
            if (editing?.id) {
                await trainApi.updateDemandTrain(editing.id, {
                    id: editing.id,
                    orgId,
                    content: v.content,
                    nums: v.nums,
                });
            }
            else {
                await trainApi.createDemandTrain({
                    id: 0,
                    orgId,
                    content: v.content,
                    nums: v.nums,
                    status: 0,
                });
            }
            message.success('提交成功');
            setFormOpen(false);
            queryClient.invalidateQueries({ queryKey: ['enterprise-demand-trains'] });
        }
        catch {
            message.error('提交失败');
        }
        finally {
            setSubmitting(false);
        }
    };
    const handleDelete = async (ids) => {
        Modal.confirm({
            title: '提示',
            content: '此操作将永久删除，是否继续？',
            onOk: async () => {
                if (ids.length === 1) {
                    await trainApi.deleteDemandTrain(ids[0]);
                }
                else {
                    await trainApi.batchDeleteDemandTrains(ids);
                }
                message.success('删除成功');
                setSelectedRowKeys([]);
                queryClient.invalidateQueries({ queryKey: ['enterprise-demand-trains'] });
            },
        });
    };
    const loadProgram = async (demandId) => {
        setCurrentDemandId(demandId);
        try {
            const info = await trainApi.getSolutionByDemandId(demandId);
            const file = resolveFileUrl(info.file || info.file_path);
            setProgram({
                ...info,
                file,
                file_name: info.file_name || fileNameFromUrl(file),
                fileType: info.fileType || fileTypeFromPath(info.file_name) || fileTypeFromPath(file),
            });
            setProgramOpen(true);
        }
        catch {
            // 接口异常时也作为「方案制作中」处理
            setProgram({
                id: 0,
                title: '',
                badgeList: [],
                status: 'pending',
            });
            setProgramOpen(true);
        }
    };
    const handleDownloadProgram = async () => {
        if (!program?.file) {
            return;
        }
        try {
            const ext = program.fileType ? `.${program.fileType}` : '';
            const storedName = program.file_name;
            const urlName = fileNameFromUrl(program.file);
            const downloadName = storedName || (urlName && urlName !== 'download' ? urlName : `${program.title || '培训方案'}${ext}`);
            await downloadUrlFile(program.file, downloadName);
        }
        catch {
            message.error('下载失败');
        }
    };
    const handleAgree = () => {
        const trainInfoId = program?.train_info_id;
        if (!trainInfoId) {
            message.error('缺少培训信息');
            return;
        }
        Modal.confirm({
            title: '确认同意此项培训方案吗？',
            onOk: async () => {
                setActionLoading(true);
                try {
                    await trainApi.companyConfirmInfoList(trainInfoId);
                    message.success('操作成功');
                    if (currentDemandId)
                        await loadProgram(currentDemandId);
                    queryClient.invalidateQueries({ queryKey: ['enterprise-demand-trains'] });
                }
                catch {
                    message.error('操作失败');
                }
                finally {
                    setActionLoading(false);
                }
            },
        });
    };
    const handleRejectSubmit = async () => {
        const trainInfoId = program?.train_info_id;
        if (!trainInfoId) {
            message.error('缺少培训信息');
            return;
        }
        if (!rejectReason.trim()) {
            message.error('请输入拒绝理由');
            return;
        }
        setActionLoading(true);
        try {
            await trainApi.companyRejectInfoList(trainInfoId, rejectReason.trim());
            message.success('操作成功');
            setRejectOpen(false);
            setRejectReason('');
            if (currentDemandId)
                await loadProgram(currentDemandId);
            queryClient.invalidateQueries({ queryKey: ['enterprise-demand-trains'] });
        }
        catch {
            message.error('操作失败');
        }
        finally {
            setActionLoading(false);
        }
    };
    const openStudents = async (row) => {
        setCurrentDemandId(row.id);
        try {
            const list = await trainApi.getDemandStudents(row.id);
            setStudents(list);
            setStudentOpen(true);
        }
        catch {
            message.error('加载学员列表失败');
        }
    };
    const columns = [
        { title: '创建日期', dataIndex: 'addtime', width: 170, render: (v) => formatAddtime(v) },
        { title: '培训内容', dataIndex: 'content', ellipsis: true },
        {
            title: '发布人',
            dataIndex: 'publisher',
            width: 180,
            ellipsis: true,
            render: (v) => v || '-',
        },
        { title: '人数', dataIndex: 'nums', width: 90 },
        {
            title: '需求状态',
            dataIndex: 'status',
            width: 100,
            render: (v) => {
                const s = DEMAND_STATUS[v] ?? { label: '未知', color: 'default' };
                return _jsx(Tag, { color: s.color, children: s.label });
            },
        },
        {
            title: '方案状态',
            dataIndex: 'solutionCn',
            width: 100,
            render: (v) => v || '-',
        },
        {
            title: '操作',
            key: 'act',
            width: 260,
            fixed: 'right',
            render: (_, row) => {
                if (row.status === 0) {
                    return (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", onClick: () => openEdit(row), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, onClick: () => handleDelete([row.id]), children: "\u5220\u9664" })] }));
                }
                if (row.solution == null || row.solution === 0) {
                    return (_jsx(Button, { type: "link", size: "small", onClick: () => loadProgram(row.id), children: "\u5904\u7406\u65B9\u6848" }));
                }
                return (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "primary", size: "small", onClick: () => openStudents(row), children: "\u62A5\u540D\u4EBA\u5458" }), row.solution === 1 && (_jsx(Button, { type: "link", size: "small", onClick: () => loadProgram(row.id), children: "\u67E5\u770B\u65B9\u6848" }))] }));
            },
        },
    ];
    const canConfirm = program && program.train_info_status !== 1;
    return (_jsxs("div", { className: "enterprise-demand-page training-page", children: [_jsx("section", { className: "page-heading", children: _jsx(Typography.Title, { level: 4, children: "\u57F9\u8BAD\u9700\u6C42" }) }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsx("div", { className: "enterprise-demand-toolbar", children: _jsxs(Space, { children: [_jsx(Button, { type: "primary", onClick: openCreate, children: "\u6DFB\u52A0" }), _jsx(Button, { danger: true, disabled: selectedRowKeys.length === 0, onClick: () => handleDelete(selectedRowKeys), children: "\u5220\u9664" })] }) }) }), _jsx(Table, { rowKey: "id", loading: isLoading, dataSource: data?.items ?? [], columns: columns, rowSelection: {
                    selectedRowKeys,
                    onChange: (keys) => setSelectedRowKeys(keys),
                }, pagination: false, scroll: { x: 1080 } }), _jsx(Pagination, { className: "enterprise-demand-pagination", total: data?.total ?? 0, current: page, pageSize: pageSize, showSizeChanger: true, showTotal: (t) => `共 ${t} 条`, onChange: (p, ps) => {
                    if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                        setPageSize(ps);
                        setPage(1);
                    }
                    else {
                        setPage(p);
                    }
                } }), _jsx(Modal, { title: "\u57F9\u8BAD\u9700\u6C42\u4FE1\u606F", open: formOpen, onCancel: () => setFormOpen(false), onOk: handleSubmit, confirmLoading: submitting, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { name: "content", label: "\u57F9\u8BAD\u5185\u5BB9", rules: [{ required: true, message: '请输入培训内容' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "nums", label: "\u4EBA\u6570", rules: [{ required: true }], children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) })] }) }), _jsxs(Modal, { title: "\u57F9\u8BAD\u65B9\u6848", open: programOpen, width: "60%", onCancel: () => { setProgramOpen(false); setProgram(null); }, footer: null, destroyOnHidden: true, children: [canConfirm && program?.status !== 'pending' && (_jsxs("div", { className: "program-actions", children: [_jsx(Button, { type: "primary", loading: actionLoading, onClick: handleAgree, children: "\u540C\u610F" }), _jsx(Button, { loading: actionLoading, onClick: () => setRejectOpen(true), children: "\u4E0D\u540C\u610F" })] })), program && program.status !== 'pending' && (_jsxs("div", { className: "program-body", children: [_jsxs("p", { children: [_jsx("strong", { children: "\u65B9\u6848\u6807\u9898\uFF1A" }), program.title] }), _jsx("p", { children: _jsx("strong", { children: "\u8D39\u7528\u660E\u7EC6\uFF1A" }) }), _jsx(Table, { size: "small", pagination: false, rowKey: "title", dataSource: program.badgeList ?? [], columns: [
                                    { title: '费用名称', dataIndex: 'title' },
                                    { title: '预算', dataIndex: 'badge', width: 120 },
                                ] }), program.file && (_jsxs("p", { className: "mar-t10", children: [_jsx("strong", { children: "\u65B9\u6848\u8BE6\u60C5\uFF1A" }), ' ', _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => void handleDownloadProgram(), children: "\u4E0B\u8F7D" })] })), program.file ? (_jsx(SolutionDocumentPreview, { fileUrl: program.file, fileName: program.file_name, fileType: program.fileType })) : null, program.reject_reason && (_jsxs("p", { className: "mar-t10", children: [_jsx("strong", { children: "\u9A73\u56DE\u7406\u7531\uFF1A" }), program.reject_reason] }))] })), program && program.status === 'pending' && (_jsx("div", { className: "program-body", style: { textAlign: 'center', padding: '32px 0' }, children: _jsx(Typography.Text, { type: "secondary", style: { fontSize: 15 }, children: "\u65B9\u6848\u6B63\u5728\u5236\u4F5C\u4E2D\uFF0C\u8BF7\u7A0D\u540E\u67E5\u770B" }) }))] }), _jsx(Modal, { title: "\u57F9\u8BAD\u65B9\u6848\u62D2\u7EDD", open: rejectOpen, onCancel: () => { setRejectOpen(false); setRejectReason(''); }, onOk: handleRejectSubmit, confirmLoading: actionLoading, children: _jsx(Input.TextArea, { rows: 4, value: rejectReason, onChange: (e) => setRejectReason(e.target.value), placeholder: "\u8BF7\u8F93\u5165\u62D2\u7EDD\u7406\u7531" }) }), _jsx(Modal, { title: "\u62A5\u540D\u5B66\u5458\u5217\u8868", open: studentOpen, onCancel: () => { setStudentOpen(false); setStudents([]); }, footer: null, width: 640, children: _jsx(Table, { size: "small", rowKey: "id", pagination: false, dataSource: students, columns: [
                        { title: '序号', width: 60, render: (_, __, i) => i + 1 },
                        { title: '姓名', dataIndex: 'name' },
                        { title: '手机号', dataIndex: 'phone' },
                        { title: '性别', dataIndex: 'gender' },
                        { title: '身份证号', dataIndex: 'idCardNumber' },
                    ] }) })] }));
}
