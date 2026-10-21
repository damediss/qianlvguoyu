import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 培训方案 — 对齐 yun_shang trainingProgram.vue
 * 单表 + 编辑弹窗 + 查看弹窗；接口 /api/training/solutions/*
 */
import { useMemo, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Pagination, Select, Space, Table, Upload, Typography, message, } from 'antd';
import { PlusOutlined, DeleteOutlined, InboxOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadPlatformFile, resolveUploadFile, downloadUrlFile, fileNameFromUrl } from '../../api/files';
import { SolutionDocumentPreview } from '../../components/SolutionDocumentPreview';
import './TrainingProgramPage.css';
const { Dragger } = Upload;
const BADGE_FEE_OPTIONS = ['劳务费', '住宿费', '耗材', '场地成本', '设备成本', '其他'];
const TABLE_HEADER_STYLE = { background: '#f8f9fa' };
const PRESET_FEE_OPTIONS = BADGE_FEE_OPTIONS.map((t) => ({ value: t, label: t }));
/** 费用名下拉：支持在预置选项之外手动输入并保存。
 *  antd v5 的 Select 在 showSearch 下，键入文字后若不选中选项，onChange 不会触发；
 *  因此用 onSearch + onBlur/onKeyDown 把"非预置"输入值手动提交给表单。
 */
function FeeNameSelect({ value, onChange }) {
    const [extras, setExtras] = useState([]);
    const [searchText, setSearchText] = useState('');
    const options = useMemo(() => {
        const all = [...PRESET_FEE_OPTIONS];
        if (value && !all.some((o) => o.value === value) && !extras.some((o) => o.value === value)) {
            all.push({ value, label: value });
        }
        if (searchText && !all.some((o) => o.value === searchText)) {
            all.push({ value: searchText, label: searchText });
        }
        return all;
    }, [value, extras, searchText]);
    const commit = (v) => {
        const trimmed = v.trim();
        if (!trimmed)
            return;
        if (!PRESET_FEE_OPTIONS.some((o) => o.value === trimmed) && !extras.some((o) => o.value === trimmed)) {
            setExtras((prev) => [...prev, { value: trimmed, label: trimmed }]);
        }
        if (trimmed !== value)
            onChange?.(trimmed);
    };
    return (_jsx(Select, { placeholder: "\u9009\u62E9\u6216\u8F93\u5165\u8D39\u7528\u540D", style: { width: 240 }, showSearch: true, allowClear: true, value: value, searchValue: searchText, options: options, filterOption: (input, option) => (option?.label ?? '').includes(input), onSearch: (t) => setSearchText(t), onChange: (v) => {
            const next = (v ?? '');
            setSearchText('');
            commit(next);
        }, onBlur: () => {
            if (searchText && searchText !== value)
                commit(searchText);
        }, onKeyDown: (e) => {
            if (e.key === 'Enter' && searchText && searchText !== value) {
                e.preventDefault();
                commit(searchText);
            }
        } }));
}
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
    return path.substring(path.lastIndexOf('.') + 1).toLowerCase();
}
export function TrainingProgramPage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize] = useState(10);
    const [editOpen, setEditOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [viewOpen, setViewOpen] = useState(false);
    const [trainProgram, setTrainProgram] = useState(null);
    const [filePathTip, setFilePathTip] = useState('');
    const { data: publicData } = useQuery({
        queryKey: ['demand-public-data', 'program'],
        queryFn: () => trainApi.getDemandPublicData(['company', 'traininfo']),
        staleTime: 5 * 60 * 1000,
    });
    const { data: listData, isLoading } = useQuery({
        queryKey: ['solution-list', currentPage, pageSize],
        queryFn: () => trainApi.pageSolutionList({ page: currentPage, pageSize }),
    });
    const saveMu = useMutation({
        mutationFn: (p) => trainApi.saveSolution(p),
        onSuccess: (res) => {
            if (res?.info) {
                message.success('提交成功');
                closeEdit();
                queryClient.invalidateQueries({ queryKey: ['solution-list'] });
            }
        },
        onError: () => message.error('提交失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteSolutionById(id),
        onSuccess: () => {
            message.success('删除成功！');
            queryClient.invalidateQueries({ queryKey: ['solution-list'] });
        },
        onError: () => message.error('删除失败'),
    });
    const companyOptions = publicData?.company_list?.map((c) => ({
        value: c.id,
        label: c.title,
    })) ?? [];
    const selectedCompanyId = Form.useWatch('companyId', form);
    const allTraininfoOptions = publicData?.traininfo_list ?? [];
    const traininfoOptions = useMemo(() => {
        if (!selectedCompanyId)
            return [];
        const cid = Number(selectedCompanyId);
        return allTraininfoOptions
            .filter((t) => t.company_id != null && Number(t.company_id) === cid)
            .map((t) => ({ value: t.id, label: t.title }));
    }, [allTraininfoOptions, selectedCompanyId]);
    const handleCompanyChange = (companyId) => {
        const traininfoId = form.getFieldValue('traininfoId');
        if (!traininfoId)
            return;
        const cid = companyId != null ? Number(companyId) : undefined;
        const stillValid = allTraininfoOptions.some((t) => t.id === traininfoId && cid != null && Number(t.company_id) === cid);
        if (!stillValid) {
            form.setFieldsValue({ traininfoId: undefined });
        }
    };
    const closeEdit = () => {
        setEditOpen(false);
        setEditingId(null);
        setFilePathTip('');
        form.resetFields();
    };
    const openAdd = () => {
        form.resetFields();
        form.setFieldsValue({
            badgeList: [{ title: '', badge: undefined, expend: undefined }],
        });
        setEditingId(null);
        setFilePathTip('');
        setEditOpen(true);
    };
    const mapDetailToForm = (d, id) => {
        const badges = (d.badgeList ?? []).map((b) => ({
            title: b.title,
            badge: b.badge != null ? Number(b.badge) : undefined,
            expend: b.expend != null ? Number(b.expend) : undefined,
        }));
        form.setFieldsValue({
            id,
            title: d.title,
            companyId: d.company_id,
            traininfoId: d.train_info_id,
            filePath: d.file_path,
            badgeList: badges.length > 0 ? badges : [{ title: '', badge: undefined, expend: undefined }],
        });
        setFilePathTip(d.file_path ?? '');
        setEditingId(id);
        setEditOpen(true);
    };
    const handleView = async (row) => {
        try {
            const d = await trainApi.getSolutionDetail(row.id);
            const file = resolveFileUrl(d.file || d.file_path);
            setTrainProgram({
                ...d,
                file,
                file_name: d.file_name || fileNameFromUrl(file),
                fileType: d.fileType || fileTypeFromPath(d.file_name) || fileTypeFromPath(file),
                badgeList: (d.badgeList ?? []).map((b) => ({
                    ...b,
                    badge: Number(b.badge),
                    expend: Number(b.expend),
                })),
            });
            setViewOpen(true);
        }
        catch {
            message.error('获取信息失败');
        }
    };
    const handleEdit = async (row) => {
        try {
            const d = await trainApi.getSolutionDetail(row.id);
            mapDetailToForm(d, row.id);
        }
        catch {
            message.error('获取信息失败');
        }
    };
    const handleDelete = (row) => {
        Modal.confirm({
            title: '提示',
            content: '此操作将永久删除该记录，是否继续？',
            okText: '确定',
            cancelText: '取消',
            onOk: () => deleteMu.mutateAsync(row.id),
        });
    };
    const confirmSave = async () => {
        try {
            const v = await form.validateFields();
            const company = companyOptions.find((c) => c.value === v.companyId);
            const payload = {
                id: editingId ?? undefined,
                title: v.title,
                companyId: v.companyId,
                companyName: company?.label ?? '',
                traininfoId: v.traininfoId,
                filePath: v.filePath || filePathTip,
                badgeList: (v.badgeList ?? []),
            };
            saveMu.mutate(payload);
        }
        catch {
            message.error('表单错误');
        }
    };
    const uploadProps = {
        multiple: false,
        accept: '.pdf,.docx',
        maxCount: 1,
        showUploadList: false,
        className: 'upload-demos',
        customRequest: async ({ file, onSuccess, onError }) => {
            const rawFile = resolveUploadFile(file);
            if (!(rawFile instanceof Blob)) {
                message.error('无法读取文件，请重新选择');
                onError?.(new Error('invalid file'));
                return;
            }
            const fileName = rawFile instanceof File ? rawFile.name : 'upload.bin';
            const ok = fileName.endsWith('.pdf') ||
                fileName.endsWith('.docx') ||
                rawFile.type === 'application/pdf' ||
                rawFile.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
            if (!ok) {
                message.error('只能上传 .pdf、.docx 文件');
                onError?.(new Error('invalid type'));
                return;
            }
            if (rawFile.size <= 0) {
                message.error('文件为空，请重新选择');
                onError?.(new Error('empty file'));
                return;
            }
            try {
                const result = await uploadPlatformFile(rawFile, 'public');
                const url = `/api/files/${result.id}/download`;
                form.setFieldsValue({ filePath: url });
                setFilePathTip(url);
                message.success('上传成功');
                onSuccess?.(result);
            }
            catch {
                message.error('上传失败');
                onError?.(new Error('upload failed'));
            }
        },
    };
    const handleDownloadSolution = async () => {
        if (!trainProgram?.file) {
            return;
        }
        try {
            const ext = trainProgram.fileType ? `.${trainProgram.fileType}` : '';
            const storedName = trainProgram.file_name;
            const urlName = fileNameFromUrl(trainProgram.file);
            const downloadName = storedName || (urlName && urlName !== 'download' ? urlName : `${trainProgram.title || '培训方案'}${ext}`);
            await downloadUrlFile(trainProgram.file, downloadName);
        }
        catch {
            message.error('下载失败');
        }
    };
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 60, align: 'center' },
        { title: '方案标题', dataIndex: 'title', align: 'center', width: 100 },
        { title: '企业', dataIndex: 'companyName', align: 'center' },
        { title: '培训需求', dataIndex: 'trainTitle', align: 'center', width: 180 },
        { title: '预算', dataIndex: 'badge', align: 'center', width: 100 },
        { title: '实际支出', dataIndex: 'expend', align: 'center', width: 100 },
        { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
        {
            title: '操作',
            align: 'center',
            width: 220,
            render: (_, row) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", className: "link-view", onClick: () => handleView(row), children: "\u67E5\u770B\u65B9\u6848" }), _jsx(Button, { type: "link", className: "link-edit", onClick: () => handleEdit(row), children: "\u4FEE\u6539" }), _jsx(Button, { type: "link", className: "link-delete", onClick: () => handleDelete(row), children: "\u5220\u9664" })] })),
        },
    ];
    const viewColumns = [
        { title: '费用名称', dataIndex: 'title' },
        { title: '预算', dataIndex: 'badge', width: 180 },
    ];
    return (_jsxs("div", { className: "training-program-page training-page expense-table-container", children: [_jsxs("section", { className: "page-heading", children: [_jsx(Typography.Title, { level: 4, children: "\u57F9\u8BAD\u65B9\u6848" }), _jsx(Typography.Text, { type: "secondary", children: "\u7BA1\u7406\u57F9\u8BAD\u65B9\u6848\u4E0E\u9884\u7B97\uFF0C\u5173\u8054\u4F01\u4E1A\u57F9\u8BAD\u9700\u6C42" })] }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsx(Button, { type: "primary", onClick: openAdd, children: "+ \u6DFB\u52A0" }) }), _jsx(Table, { rowKey: "id", loading: isLoading, columns: columns, dataSource: listData?.items ?? [], pagination: false, scroll: { y: 600 }, style: { width: '100%' } }), _jsx("div", { className: "pagination-bar", children: _jsx(Pagination, { current: currentPage, pageSize: pageSize, total: listData?.total ?? 0, showSizeChanger: false, showTotal: (t) => `共 ${t} 条`, onChange: (p) => setCurrentPage(p) }) }), _jsx(Modal, { title: editingId ? '修改' : '新增', open: editOpen, onCancel: closeEdit, onOk: confirmSave, confirmLoading: saveMu.isPending, okText: "\u786E\u8BA4", cancelText: "\u53D6\u6D88", width: "50%", destroyOnHidden: true, children: _jsxs(Form, { form: form, labelCol: { span: 6 }, wrapperCol: { span: 18 }, children: [_jsx(Form.Item, { name: "id", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "title", label: "\u65B9\u6848\u6807\u9898", rules: [{ required: true, message: '请输入方案标题' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u65B9\u6848\u6807\u9898" }) }), _jsx(Form.Item, { name: "companyId", label: "\u53D7\u8BAD\u5355\u4F4D", rules: [{ required: true, message: '请选择受训企业' }], children: _jsx(Select, { showSearch: true, allowClear: true, placeholder: "\u53D7\u8BAD\u5355\u4F4D", options: companyOptions, filterOption: (input, opt) => (opt?.label ?? '').includes(input), onChange: handleCompanyChange }) }), _jsx(Form.Item, { name: "traininfoId", label: "\u7ED1\u5B9A\u57F9\u8BAD\u9700\u6C42", rules: [{ required: true, message: '请选择绑定的需求' }], children: _jsx(Select, { showSearch: true, allowClear: true, placeholder: selectedCompanyId ? '培训需求' : '请先选择受训单位', disabled: !selectedCompanyId, options: traininfoOptions, filterOption: (input, opt) => (opt?.label ?? '').includes(input) }) }), _jsx(Form.Item, { name: "filePath", hidden: true, children: _jsx(Input, {}) }), _jsxs(Form.Item, { label: "\u65B9\u6848\u6587\u6863", children: [_jsxs(Dragger, { ...uploadProps, children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(InboxOutlined, {}) }), _jsxs("p", { className: "ant-upload-text", children: ["\u5C06\u6587\u4EF6\u62D6\u5230\u6B64\u5904\uFF0C\u6216", _jsx("em", { children: "\u70B9\u51FB\u4E0A\u4F20" })] }), _jsx("p", { className: "ant-upload-hint", style: { color: '#ccc', marginTop: 10 }, children: "\u63D0\u793A:\u4E0A\u4F20\u8FC7\u7A0B\u4E2D,\u8BF7\u4E0D\u8981\u505A\u4EFB\u4F55\u64CD\u4F5C" })] }), _jsxs("div", { style: { marginTop: 8, fontSize: 12, color: 'rgba(0,0,0,0.45)' }, children: ["\u53EA\u80FD\u4E0A\u4F20 .pdf\u3001.docx \u6587\u4EF6", filePathTip ? _jsx("span", { className: "upload-tip-path", children: filePathTip }) : null] })] }), _jsxs(Form.Item, { label: "\u9884\u7B97\u7BA1\u7406", colon: false, children: [_jsxs("div", { className: "icon-add", onClick: () => {
                                        const list = form.getFieldValue('badgeList') ?? [];
                                        form.setFieldsValue({ badgeList: [...list, { title: '', badge: undefined, expend: undefined }] });
                                    }, children: [_jsx(PlusOutlined, { style: { fontSize: 16 } }), _jsx("span", { className: "mar-l5", children: "\u6DFB\u52A0\u8D39\u7528\u9879" })] }), _jsx(Form.List, { name: "badgeList", children: (fields, { remove }) => (_jsx(Table, { className: "budget-table", bordered: true, size: "small", pagination: false, dataSource: fields, rowKey: "key", columns: [
                                            {
                                                title: '费用名',
                                                render: (_, field) => (_jsx(Form.Item, { name: [field.name, 'title'], rules: [{ required: true, message: '请选择或输入费用名' }], noStyle: true, children: _jsx(FeeNameSelect, {}) })),
                                            },
                                            {
                                                title: '预算(元)',
                                                width: 180,
                                                render: (_, field) => (_jsx(Form.Item, { name: [field.name, 'badge'], noStyle: true, children: _jsx(InputNumber, { style: { width: '100%' }, min: 0 }) })),
                                            },
                                            {
                                                title: '实际支出(元)',
                                                width: 180,
                                                render: (_, field) => (_jsx(Form.Item, { name: [field.name, 'expend'], noStyle: true, children: _jsx(InputNumber, { style: { width: '100%' }, min: 0 }) })),
                                            },
                                            {
                                                title: '操作',
                                                width: 60,
                                                align: 'center',
                                                render: (_, field) => fields.length > 1 ? (_jsx(DeleteOutlined, { style: { fontSize: 20, cursor: 'pointer', color: '#ff4d4f' }, onClick: () => remove(field.name) })) : null,
                                            },
                                        ] })) })] })] }) }), _jsx(Modal, { title: "\u57F9\u8BAD\u65B9\u6848", open: viewOpen, onCancel: () => { setViewOpen(false); setTrainProgram(null); }, footer: null, width: "60%", destroyOnHidden: true, children: trainProgram && (_jsxs("div", { children: [_jsxs("p", { className: "mar-t10", children: [_jsx("strong", { children: "\u65B9\u6848\u6807\u9898\uFF1A" }), trainProgram.title] }), _jsx("p", { className: "mar-t10", children: _jsx("strong", { children: "\u8D39\u7528\u660E\u7EC6\uFF1A" }) }), trainProgram.reject_reason ? (_jsxs("p", { className: "mar-t10", children: [_jsx("strong", { children: "\u4F01\u4E1A\u62D2\u7EDD\u7406\u7531\uFF1A" }), trainProgram.reject_reason] })) : null, _jsx(Table, { bordered: true, size: "small", pagination: false, rowKey: "title", dataSource: trainProgram.badgeList ?? [], columns: viewColumns, style: { marginTop: 10 } }), trainProgram.file ? (_jsxs("p", { className: "mar-t10", children: [_jsx("strong", { children: "\u65B9\u6848\u8BE6\u60C5\uFF1A" }), ' ', _jsx(Button, { type: "link", className: "link-view", style: { padding: 0 }, onClick: () => void handleDownloadSolution(), children: "\u4E0B\u8F7D" })] })) : null, trainProgram.file ? (_jsx(SolutionDocumentPreview, { fileUrl: trainProgram.file, fileName: trainProgram.file_name, fileType: trainProgram.fileType })) : null] })) })] }));
}
