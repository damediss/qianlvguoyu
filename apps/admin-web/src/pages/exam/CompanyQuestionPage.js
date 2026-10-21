import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, UploadOutlined, DownloadOutlined, InboxOutlined, } from '@ant-design/icons';
import { Badge, Button, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, message, Popconfirm, Upload, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback, useMemo, useEffect } from 'react';
import { pageCompanyQuestions, createCompanyQuestion, updateCompanyQuestion, deleteCompanyQuestion, batchDeleteCompanyQuestions, importCompanyQuestions, exportCompanyQuestions, } from '../../api/exam';
import { listOrgs } from '../../api/system';
import { useAuthStore } from '../../stores/authStore';
import { EXAM_TYPE_MAP, DIFFICULTY_MAP, DIFFICULTY_COLOR_MAP, } from '../../types/exam';
const { TextArea } = Input;
function CompanyQuestionFormModal({ open, editing, onClose }) {
    const [form] = Form.useForm();
    const queryClient = useQueryClient();
    const { data: orgList } = useQuery({
        queryKey: ['company-question-org-list', open],
        queryFn: () => listOrgs(),
        enabled: open,
    });
    const orgOptions = useMemo(() => (orgList ?? []).map((o) => ({ value: o.id, label: o.name })), [orgList]);
    const createMut = useMutation({
        mutationFn: createCompanyQuestion,
        onSuccess: () => {
            message.success('创建成功');
            queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
            onClose();
        },
        onError: (e) => message.error(e.message || '创建失败'),
    });
    const updateMut = useMutation({
        mutationFn: ({ id, payload }) => updateCompanyQuestion(id, payload),
        onSuccess: () => {
            message.success('更新成功');
            queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
            onClose();
        },
        onError: (e) => message.error(e.message || '更新失败'),
    });
    const handleOk = useCallback(async () => {
        try {
            const values = await form.validateFields();
            if (editing) {
                updateMut.mutate({ id: editing.id, payload: values });
            }
            else {
                createMut.mutate(values);
            }
        }
        catch {
            // validation error
        }
    }, [form, editing, createMut, updateMut]);
    useEffect(() => {
        if (open) {
            if (editing) {
                form.setFieldsValue({
                    type: editing.type,
                    content: editing.content,
                    answer: editing.answer,
                    orgId: editing.orgId,
                    orgName: editing.orgName,
                    difficulty: editing.difficulty,
                    score: editing.score,
                    resolve: editing.resolve,
                    keyword: editing.keyword,
                });
            }
            else {
                form.resetFields();
            }
        }
    }, [open, editing, form]);
    return (_jsx(Modal, { title: editing ? '编辑试题' : '新建试题', open: open, onOk: handleOk, onCancel: onClose, confirmLoading: createMut.isPending || updateMut.isPending, width: 720, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", initialValues: { type: 'single', difficulty: 2 }, children: [_jsx(Form.Item, { name: "type", label: "\u9898\u578B", rules: [{ required: true, message: '请选择题型' }], children: _jsx(Select, { options: Object.entries(EXAM_TYPE_MAP).map(([k, v]) => ({ label: v, value: k })), placeholder: "\u9009\u62E9\u9898\u578B" }) }), _jsx(Form.Item, { name: "content", label: "\u9898\u5E72", children: _jsx(TextArea, { rows: 4, placeholder: "\u8BF7\u8F93\u5165\u9898\u5E72\u5185\u5BB9" }) }), _jsx(Form.Item, { name: "answer", label: "\u7B54\u6848", children: _jsx(TextArea, { rows: 3, placeholder: "\u8BF7\u8F93\u5165\u7B54\u6848" }) }), _jsx(Form.Item, { name: "orgId", label: "\u7ED1\u5B9A\u516C\u53F8", rules: [{ required: true, message: '请选择公司' }], children: _jsx(Select, { showSearch: true, allowClear: true, placeholder: "\u8BF7\u9009\u62E9\u516C\u53F8", optionFilterProp: "label", options: orgOptions, onChange: (value) => {
                            const org = orgList?.find((o) => o.id === value);
                            form.setFieldsValue({ orgName: org?.name });
                        } }) }), _jsx(Form.Item, { name: "orgName", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "difficulty", label: "\u96BE\u5EA6", children: _jsx(Select, { options: Object.entries(DIFFICULTY_MAP).map(([k, v]) => ({
                            label: v,
                            value: Number(k),
                        })), placeholder: "\u9009\u62E9\u96BE\u5EA6" }) }), _jsx(Form.Item, { name: "score", label: "\u5206\u503C", children: _jsx(InputNumber, { min: 0, max: 9999, style: { width: '100%' }, placeholder: "\u5206\u503C" }) }), _jsx(Form.Item, { name: "resolve", label: "\u89E3\u6790", children: _jsx(TextArea, { rows: 3, placeholder: "\u8BF7\u8F93\u5165\u89E3\u6790" }) }), _jsx(Form.Item, { name: "keyword", label: "\u5173\u952E\u8BCD", children: _jsx(Input, { placeholder: "\u5173\u952E\u8BCD\uFF0C\u591A\u4E2A\u7528\u9017\u53F7\u5206\u9694" }) })] }) }));
}
// ─── Main Page ─────────────────────────────────────────────
export function CompanyQuestionPage() {
    const queryClient = useQueryClient();
    const hasPerm = useAuthStore((s) => s.hasPermission);
    // ── table state ──
    const [query, setQuery] = useState({ page: 1, pageSize: 20 });
    const [filterOrgId, setFilterOrgId] = useState();
    const [filterType, setFilterType] = useState();
    const [filterDifficulty, setFilterDifficulty] = useState();
    const [filterKeyword, setFilterKeyword] = useState('');
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);
    // ── modal state ──
    const [modalOpen, setModalOpen] = useState(false);
    const [editingDetail, setEditingDetail] = useState(null);
    const [importModalOpen, setImportModalOpen] = useState(false);
    const [importFileList, setImportFileList] = useState([]);
    const [importLoading, setImportLoading] = useState(false);
    const [exportLoading, setExportLoading] = useState(false);
    // ── org list for filter dropdown ──
    const { data: orgList } = useQuery({
        queryKey: ['company-question-filter-orgs'],
        queryFn: () => listOrgs(),
    });
    const orgFilterOptions = useMemo(() => (orgList ?? []).map((o) => ({ value: o.id, label: o.name })), [orgList]);
    // ── effective query ──
    const effectiveQuery = useMemo(() => ({
        ...query,
        orgId: filterOrgId,
        type: filterType,
        difficulty: filterDifficulty,
        keyword: filterKeyword || undefined,
    }), [query, filterOrgId, filterType, filterDifficulty, filterKeyword]);
    const { data: examPage, isLoading: loadingDetails } = useQuery({
        queryKey: ['companyQuestions', effectiveQuery],
        queryFn: () => pageCompanyQuestions(effectiveQuery),
    });
    // ── delete mutation ──
    const deleteMut = useMutation({
        mutationFn: deleteCompanyQuestion,
        onSuccess: () => {
            message.success('删除成功');
            queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
        },
        onError: (e) => message.error(e.message || '删除失败'),
    });
    // ── batch delete mutation ──
    const batchDeleteMut = useMutation({
        mutationFn: batchDeleteCompanyQuestions,
        onSuccess: (n) => {
            message.success(`已删除 ${n} 道试题`);
            setSelectedRowKeys([]);
            queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
        },
        onError: (e) => message.error(e.message || '批量删除失败'),
    });
    const handleBatchDelete = () => {
        const ids = selectedRowKeys.map(Number);
        if (ids.length === 0)
            return;
        Modal.confirm({
            title: `确认删除 ${ids.length} 道试题?`,
            content: '删除后无法恢复',
            okText: '确认删除',
            okButtonProps: { danger: true },
            onOk: () => batchDeleteMut.mutate(ids),
        });
    };
    // ── import handler ──
    const handleImport = async () => {
        if (!filterOrgId) {
            message.warning('请先在筛选栏选择公司');
            return;
        }
        if (importFileList.length === 0) {
            message.warning('请选择要导入的文件');
            return;
        }
        const org = orgList?.find((o) => o.id === filterOrgId);
        const formData = new FormData();
        formData.append('file', importFileList[0].originFileObj);
        formData.append('orgId', String(filterOrgId));
        formData.append('orgName', org?.name ?? '');
        formData.append('source', '0');
        setImportLoading(true);
        try {
            const count = await importCompanyQuestions(formData);
            message.success(`成功导入 ${count} 道试题`);
            setImportModalOpen(false);
            setImportFileList([]);
            queryClient.invalidateQueries({ queryKey: ['companyQuestions'] });
        }
        catch (e) {
            message.error(e?.message || '导入失败');
        }
        finally {
            setImportLoading(false);
        }
    };
    // ── export handler ──
    const handleExport = async () => {
        setExportLoading(true);
        try {
            const resp = await exportCompanyQuestions(filterOrgId);
            const blob = new Blob([resp.data], {
                type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            });
            const disposition = resp.headers['content-disposition'];
            let filename = '公司试题导出.xlsx';
            if (disposition) {
                const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;\n]+)/i);
                if (match)
                    filename = decodeURIComponent(match[1]);
            }
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
            message.success('导出成功');
        }
        catch (e) {
            message.error(e?.message || '导出失败');
        }
        finally {
            setExportLoading(false);
        }
    };
    // ── Table columns ──
    const columns = [
        {
            title: '序号',
            width: 60,
            render: (_, __, index) => ((query.page ?? 1) - 1) * (query.pageSize ?? 20) + index + 1,
        },
        {
            title: '题型',
            dataIndex: 'type',
            width: 90,
            render: (v) => EXAM_TYPE_MAP[v] || v,
        },
        {
            title: '题干与选项',
            dataIndex: 'content',
            ellipsis: true,
            render: (v) => (_jsx("div", { style: { maxHeight: 80, overflow: 'hidden' }, children: v ? v.substring(0, 120) : '-' })),
        },
        {
            title: '难度',
            dataIndex: 'difficulty',
            width: 60,
            render: (v) => (_jsx(Tag, { color: DIFFICULTY_COLOR_MAP[v] || 'default', children: DIFFICULTY_MAP[v] || v })),
        },
        {
            title: '公司名称',
            dataIndex: 'orgName',
            width: 250,
            ellipsis: true,
            render: (v) => v || '-',
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            width: 130,
            render: (v) => (v ? String(v).substring(0, 16) : '-'),
        },
        {
            title: '操作',
            width: 150,
            render: (_, record) => (_jsxs(Space, { size: "small", children: [hasPerm('exam:question:update') && (_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => {
                            setEditingDetail(record);
                            setModalOpen(true);
                        }, children: "\u7F16\u8F91" })), hasPerm('exam:question:delete') && (_jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u6B64\u8BD5\u9898\uFF1F", onConfirm: () => deleteMut.mutate(record.id), okText: "\u786E\u8BA4", cancelText: "\u53D6\u6D88", children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) }))] })),
        },
    ];
    return (_jsxs("div", { style: { display: 'flex', flexDirection: 'column', gap: 12 }, children: [_jsxs(Space, { wrap: true, children: [_jsx(Select, { style: { width: 200 }, placeholder: "\u516C\u53F8", allowClear: true, showSearch: true, optionFilterProp: "label", value: filterOrgId, onChange: (v) => {
                            setFilterOrgId(v);
                            setQuery((q) => ({ ...q, page: 1 }));
                        }, options: orgFilterOptions }), _jsx(Select, { style: { width: 120 }, placeholder: "\u9898\u578B", allowClear: true, value: filterType, onChange: (v) => {
                            setFilterType(v);
                            setQuery((q) => ({ ...q, page: 1 }));
                        }, options: Object.entries(EXAM_TYPE_MAP).map(([k, v]) => ({ label: v, value: k })) }), _jsx(Select, { style: { width: 100 }, placeholder: "\u96BE\u5EA6", allowClear: true, value: filterDifficulty, onChange: (v) => {
                            setFilterDifficulty(v);
                            setQuery((q) => ({ ...q, page: 1 }));
                        }, options: Object.entries(DIFFICULTY_MAP).map(([k, v]) => ({
                            label: v,
                            value: Number(k),
                        })) }), _jsx(Input.Search, { style: { width: 200 }, placeholder: "\u641C\u7D22\u9898\u5E72", allowClear: true, onSearch: (v) => {
                            setFilterKeyword(v);
                            setQuery((q) => ({ ...q, page: 1 }));
                        } }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['companyQuestions'] }), children: "\u5237\u65B0" }), hasPerm('exam:question:delete') && (_jsx(Button, { danger: true, icon: _jsx(DeleteOutlined, {}), disabled: selectedRowKeys.length === 0, onClick: handleBatchDelete, children: _jsx(Badge, { count: selectedRowKeys.length, offset: [8, -2], size: "small", showZero: false, children: _jsx("span", { children: "\u6279\u91CF\u5220\u9664" }) }) })), hasPerm('exam:question:create') && (_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => {
                            setEditingDetail(null);
                            setModalOpen(true);
                        }, children: "\u65B0\u5EFA\u8BD5\u9898" })), hasPerm('exam:question:create') && (_jsx(Button, { icon: _jsx(UploadOutlined, {}), onClick: () => {
                            if (!filterOrgId) {
                                message.warning('请先在筛选栏选择公司');
                                return;
                            }
                            setImportFileList([]);
                            setImportModalOpen(true);
                        }, children: "\u5BFC\u5165\u8BD5\u9898" })), hasPerm('exam:question:view') && (_jsx(Button, { icon: _jsx(DownloadOutlined, {}), loading: exportLoading, onClick: handleExport, children: "\u5BFC\u51FA\u8BD5\u9898" }))] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: examPage?.items ?? [], loading: loadingDetails, rowSelection: {
                    selectedRowKeys,
                    onChange: (keys) => setSelectedRowKeys(keys),
                }, pagination: {
                    current: query.page,
                    pageSize: query.pageSize,
                    total: examPage?.total ?? 0,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条`,
                    onChange: (page, pageSize) => setQuery((q) => ({ ...q, page, pageSize })),
                }, size: "small", scroll: { x: 1000 } }), _jsx(CompanyQuestionFormModal, { open: modalOpen, editing: editingDetail, onClose: () => {
                    setModalOpen(false);
                    setEditingDetail(null);
                } }), _jsx(Modal, { title: `导入公司试题${filterOrgId ? ` — ${orgList?.find((o) => o.id === filterOrgId)?.name ?? ''}` : ''}`, open: importModalOpen, onOk: handleImport, onCancel: () => {
                    setImportModalOpen(false);
                    setImportFileList([]);
                }, confirmLoading: importLoading, okText: "\u5F00\u59CB\u5BFC\u5165", destroyOnHidden: true, children: _jsxs(Upload.Dragger, { accept: ".xls,.xlsx", maxCount: 1, fileList: importFileList, beforeUpload: () => false, onChange: ({ fileList }) => setImportFileList(fileList), children: [_jsx("p", { style: { fontSize: 40, color: '#1677ff', marginBottom: 8 }, children: _jsx(InboxOutlined, {}) }), _jsx("p", { style: { fontSize: 14 }, children: "\u70B9\u51FB\u6216\u5C06\u6587\u4EF6\u62D6\u62FD\u5230\u8FD9\u91CC\u4E0A\u4F20" }), _jsx("p", { style: { color: '#999', fontSize: 12 }, children: "\u652F\u6301 .xls / .xlsx \u683C\u5F0F" })] }) })] }));
}
