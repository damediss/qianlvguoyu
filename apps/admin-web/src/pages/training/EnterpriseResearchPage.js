import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Typography, message, } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, TeamOutlined, } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { ResearchCollectionDrawer } from './ResearchCollectionDrawer';
import { ResearchCollectionFieldOptionsEditor } from './ResearchCollectionFieldOptionsEditor';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import { buildEmptyFieldOptionsForm, mergeFieldOptionsForForm, normalizeCollectionFieldOptions, } from './researchCollectionFieldOptions';
const { Title } = Typography;
const NEW_ORG_PREFIX = '__new__:';
const SYSTEM_COMPANIES_QUERY_KEY = ['system-companies'];
const STATUS_FILTER_OPTIONS = [
    { value: '', label: '全部' },
    { value: 1, label: '启用' },
    { value: 0, label: '禁用' },
];
/**
 * 关联系统企业：可搜索选择；输入不存在的名称后回车或点「xxx（添加）」即落库并选中。
 * 不用失焦自动创建，避免搜索筛选后点选已有企业时误建新企业。
 */
function SystemCompanySelect({ value, onChange, }) {
    const queryClient = useQueryClient();
    const [searchText, setSearchText] = useState('');
    const { data: orgList = [] } = useQuery({
        queryKey: SYSTEM_COMPANIES_QUERY_KEY,
        queryFn: () => trainApi.listSystemCompanies(),
    });
    const ensureMu = useMutation({
        mutationFn: (name) => trainApi.ensureSystemCompany(name),
        onSuccess: (org) => {
            message.success(`已添加系统企业：${org.name}`);
            void queryClient.invalidateQueries({ queryKey: SYSTEM_COMPANIES_QUERY_KEY });
            onChange?.(org.id);
            setSearchText('');
        },
        onError: (e) => message.error(e.message || '添加企业失败'),
    });
    const typed = searchText.trim();
    const exact = typed ? orgList.find((o) => o.name === typed) : undefined;
    const options = useMemo(() => {
        const opts = orgList.map((o) => ({
            value: o.id,
            label: o.name,
        }));
        if (typed && !exact) {
            opts.unshift({ value: `${NEW_ORG_PREFIX}${typed}`, label: `${typed}（添加）` });
        }
        return opts;
    }, [orgList, typed, exact]);
    const commitTypedName = (name) => {
        const n = name.trim();
        if (!n || ensureMu.isPending)
            return;
        const hit = orgList.find((o) => o.name === n);
        if (hit) {
            onChange?.(hit.id);
            setSearchText('');
            return;
        }
        ensureMu.mutate(n);
    };
    return (_jsx(Select, { showSearch: true, allowClear: true, placeholder: "\u8BF7\u9009\u62E9\u6216\u8F93\u5165\u7CFB\u7EDF\u4F01\u4E1A", loading: ensureMu.isPending, value: value ?? undefined, searchValue: searchText, options: options, filterOption: (input, option) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase()), notFoundContent: ensureMu.isPending ? '添加中…' : '无匹配，回车可添加新企业', onSearch: setSearchText, onChange: (v) => {
            if (v == null) {
                setSearchText('');
                onChange?.(undefined);
                return;
            }
            if (typeof v === 'string' && v.startsWith(NEW_ORG_PREFIX)) {
                commitTypedName(v.slice(NEW_ORG_PREFIX.length));
                return;
            }
            setSearchText('');
            onChange?.(Number(v));
        }, onInputKeyDown: (e) => {
            if (e.key === 'Enter' && typed && !exact) {
                e.preventDefault();
                commitTypedName(typed);
            }
        } }));
}
export function EnterpriseResearchPage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [searchForm] = Form.useForm();
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize] = useState(10);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [filters, setFilters] = useState({});
    const [collectionDrawerOpen, setCollectionDrawerOpen] = useState(false);
    const [activeResearchOrg, setActiveResearchOrg] = useState(null);
    const [editingInitialStatus, setEditingInitialStatus] = useState(null);
    const listQuery = useMemo(() => ({
        page: currentPage,
        pageSize,
        ...filters,
    }), [currentPage, pageSize, filters]);
    const { data, isLoading } = useQuery({
        queryKey: ['research-orgs', listQuery],
        queryFn: () => trainApi.pageResearchOrgs(listQuery),
    });
    const createMu = useMutation({
        mutationFn: (payload) => trainApi.createResearchOrg(payload),
        onSuccess: () => {
            message.success('创建成功');
            closeModal();
            setCurrentPage(1);
            queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
        },
        onError: () => message.error('创建失败'),
    });
    const updateMu = useMutation({
        mutationFn: ({ id, p }) => trainApi.updateResearchOrg(id, p),
        onSuccess: () => {
            message.success('更新成功');
            closeModal();
            queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
        },
        onError: () => message.error('更新失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteResearchOrg(id),
        onSuccess: () => {
            message.success('删除成功');
            queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
        },
        onError: () => message.error('删除失败'),
    });
    const closeModal = () => {
        setModalOpen(false);
        setEditingId(null);
        form.resetFields();
    };
    const openCreate = () => {
        setEditingId(null);
        setEditingInitialStatus(null);
        form.setFieldsValue({
            status: 1,
            sortNumber: 100,
            collectionFieldOptions: buildEmptyFieldOptionsForm(),
        });
        setModalOpen(true);
    };
    const openEdit = async (r) => {
        setEditingId(r.id);
        setEditingInitialStatus(r.status ?? 1);
        let fieldOptions = r.collectionFieldOptions;
        if (!fieldOptions) {
            try {
                const detail = await trainApi.getResearchOrg(r.id);
                fieldOptions = detail.collectionFieldOptions;
            }
            catch {
                fieldOptions = undefined;
            }
        }
        form.setFieldsValue({
            title: r.title,
            targetOrgId: r.targetOrgId,
            type: r.type,
            provinceId: r.provinceId,
            provinceName: r.provinceName,
            cityId: r.cityId,
            cityName: r.cityName,
            areaId: r.areaId,
            areaName: r.areaName,
            sortNumber: r.sortNumber,
            status: r.status,
            collectionFieldOptions: mergeFieldOptionsForForm(fieldOptions),
        });
        setModalOpen(true);
    };
    const openCollection = (r) => {
        setActiveResearchOrg(r);
        setCollectionDrawerOpen(true);
    };
    const handleSubmit = async () => {
        const values = await form.validateFields();
        const payload = {
            ...values,
            collectionFieldOptions: normalizeCollectionFieldOptions(values.collectionFieldOptions),
        };
        const disabling = editingId && editingInitialStatus === 1 && values.status === 0;
        if (disabling) {
            const ok = await new Promise((resolve) => {
                Modal.confirm({
                    title: '确认禁用',
                    content: '禁用后将停止后台与扫码采集，已采数据仍可查看。确定禁用吗？',
                    onOk: () => resolve(true),
                    onCancel: () => resolve(false),
                });
            });
            if (!ok)
                return;
        }
        if (editingId) {
            updateMu.mutate({ id: editingId, p: payload });
        }
        else {
            createMu.mutate(payload);
        }
    };
    const handleSearch = (values) => {
        setCurrentPage(1);
        const next = {
            title: values.title,
            type: values.type,
        };
        if (values.status === 0 || values.status === 1) {
            next.status = values.status;
        }
        setFilters(next);
    };
    const handleReset = () => {
        searchForm.resetFields();
        setCurrentPage(1);
        setFilters({});
    };
    const list = data?.items ?? [];
    const total = data?.total ?? 0;
    const columns = [
        {
            title: '序号',
            key: 'index',
            width: 60,
            render: (_, __, i) => (currentPage - 1) * pageSize + i + 1,
        },
        { title: '企业/机构名称', dataIndex: 'title', width: 180, ellipsis: true },
        { title: '系统企业', dataIndex: 'targetOrgName', width: 160, ellipsis: true, render: (v) => v || '-' },
        { title: '类型', dataIndex: 'type', width: 120, ellipsis: true },
        {
            title: '地区',
            width: 180,
            render: (_, r) => {
                const parts = [r.provinceName, r.cityName, r.areaName].filter(Boolean);
                return parts.length ? parts.join(' / ') : '-';
            },
        },
        {
            title: '已采集',
            dataIndex: 'personnelCount',
            width: 80,
            align: 'center',
            render: (v, r) => (r.activeSignUpId ? (v ?? 0) : '-'),
        },
        { title: '排序', dataIndex: 'sortNumber', width: 70, align: 'center' },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (v) => v === 1 ? _jsx(Tag, { color: "success", children: "\u542F\u7528" }) : v === 0 ? _jsx(Tag, { children: "\u7981\u7528" }) : '—',
        },
        { title: '创建时间', dataIndex: 'createTime', width: 160 },
        {
            title: '操作',
            key: 'act',
            width: 200,
            fixed: 'right',
            render: (_, r) => (_jsxs(Space, { children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(TeamOutlined, {}), onClick: () => openCollection(r), children: "\u4EBA\u5458\u91C7\u96C6" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(r), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664\u5417\uFF1F", onConfirm: () => deleteMu.mutate(r.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(TrainingProcessSteps, { current: 0 }), _jsxs(Card, { title: _jsx(Title, { level: 5, style: { margin: 0 }, children: "\u4F01\u4E1A\u8C03\u7814" }), extra: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u8C03\u7814\u4F01\u4E1A" }), children: [_jsxs(Form, { form: searchForm, layout: "inline", onFinish: handleSearch, style: { marginBottom: 16 }, children: [_jsx(Form.Item, { name: "title", label: "\u4F01\u4E1A\u540D\u79F0", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165", allowClear: true, style: { width: 200 } }) }), _jsx(Form.Item, { name: "type", label: "\u7C7B\u578B", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165", allowClear: true, style: { width: 160 } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", initialValue: "", children: _jsx(Select, { allowClear: true, placeholder: "\u5168\u90E8", style: { width: 120 }, options: STATUS_FILTER_OPTIONS }) }), _jsxs(Form.Item, { children: [_jsx(Button, { type: "primary", htmlType: "submit", icon: _jsx(SearchOutlined, {}), children: "\u67E5\u8BE2" }), _jsx(Button, { style: { marginLeft: 8 }, icon: _jsx(ReloadOutlined, {}), onClick: handleReset, children: "\u91CD\u7F6E" })] })] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: list, loading: isLoading, pagination: {
                            current: currentPage,
                            pageSize,
                            total,
                            onChange: setCurrentPage,
                            showSizeChanger: false,
                            showTotal: (t) => `共 ${t} 条`,
                        }, scroll: { x: 1100 } }), _jsx(Modal, { title: editingId ? '编辑调研企业' : '新增调研企业', open: modalOpen, onOk: handleSubmit, onCancel: closeModal, confirmLoading: createMu.isPending || updateMu.isPending, width: 760, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { name: "title", label: "\u4F01\u4E1A/\u673A\u6784\u540D\u79F0", rules: [{ required: true, message: '请输入名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { name: "targetOrgId", label: "\u5173\u8054\u7CFB\u7EDF\u4F01\u4E1A", rules: [{ required: true, message: '请选择或输入系统企业' }], extra: "\u53EF\u641C\u7D22\u9009\u62E9\uFF1B\u8F93\u5165\u65B0\u540D\u79F0\u540E\u56DE\u8F66\uFF0C\u6216\u70B9\u9009\u300Cxxx\uFF08\u6DFB\u52A0\uFF09\u300D\u5373\u53EF\u843D\u5E93\u5E76\u9009\u4E2D\u3002\u7528\u4E8E\u4EBA\u5458\u91C7\u96C6\u3001\u4E09\u5E74\u89C4\u5212\u4E0E\u4F01\u4E1A\u5BA1\u6838\u6743\u9650", children: _jsx(SystemCompanySelect, {}) }), _jsx(Form.Item, { name: "type", label: "\u7C7B\u578B", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { name: "sortNumber", label: "\u6392\u5E8F", children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", valuePropName: "checked", getValueProps: (value) => ({ checked: value === 1 }), getValueFromEvent: (checked) => (checked ? 1 : 0), children: _jsx(Switch, { checkedChildren: "\u542F\u7528", unCheckedChildren: "\u7981\u7528" }) }), _jsx(Form.Item, { name: "collectionFieldOptions", label: null, children: _jsx(ResearchCollectionFieldOptionsEditor, {}) })] }) }), _jsx(ResearchCollectionDrawer, { open: collectionDrawerOpen, researchOrg: activeResearchOrg, onClose: () => {
                            setCollectionDrawerOpen(false);
                            setActiveResearchOrg(null);
                            queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
                        } })] })] }));
}
