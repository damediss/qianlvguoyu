import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 管理端需求管理 — 对齐 yun_shang requirementsManagement.vue
 * 待办：企业原始需求 → 处理立项
 * 已办：train_info_list 列表（筛选 + 手动添加立项）
 */
import { useMemo, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Pagination, Select, Table, Typography, message, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import './RequirementsManagementPage.css';
const WORK_LEVELS = [
    { value: 5, label: '五级/初级工' },
    { value: 4, label: '四级/中级工' },
    { value: 3, label: '三级/高级工' },
    { value: 2, label: '二级/技师' },
    { value: 1, label: '一级/高级技师' },
];
const TRAIN_COMPANY_OPTIONS = [
    { value: '千铝工投', label: '千铝工投' },
    { value: '广西现代职业技术学院', label: '广西现代职业技术学院' },
    { value: '广西工业技师学院', label: '广西工业技师学院' },
];
function formatWorkTypeTitle(item) {
    const title = item.work_type_title ?? '-';
    const level = item.work_type_level;
    const levelText = level && level > 0 ? `${level}级` : '-';
    return `${title}(${levelText})`;
}
export function RequirementsManagementPage() {
    const queryClient = useQueryClient();
    /** 0=待办, -1=已办（与 Vue headConfirmSwitch 一致） */
    const [headConfirmSwitch, setHeadConfirmSwitch] = useState(0);
    const [convertForm] = Form.useForm();
    const [modalOpen, setModalOpen] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [companyIdFilter, setCompanyIdFilter] = useState();
    const [workTypeIdFilter, setWorkTypeIdFilter] = useState();
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const pendingQuery = { page: 1, pageSize: 30 };
    const infoListQuery = useMemo(() => ({
        page: currentPage,
        pageSize,
        passStatus: 1,
        headConfirm: headConfirmSwitch,
        companyId: companyIdFilter,
        workTypeId: workTypeIdFilter,
        groupTypeNe: 1,
    }), [currentPage, pageSize, headConfirmSwitch, companyIdFilter, workTypeIdFilter]);
    const { data: publicData } = useQuery({
        queryKey: ['demand-public-data', 'requirements'],
        queryFn: () => trainApi.getDemandPublicData(['company', 'worktype']),
        staleTime: 5 * 60 * 1000,
    });
    const { data: pendings, isLoading: pendingsLoading } = useQuery({
        queryKey: ['demand-pendings', pendingQuery],
        queryFn: () => trainApi.pageDemandPendings(pendingQuery),
        enabled: headConfirmSwitch === 0,
    });
    const { data: infoList, isLoading: infoListLoading } = useQuery({
        queryKey: ['training-info-requirements', infoListQuery],
        queryFn: () => trainApi.pageInfoLists(infoListQuery),
        enabled: headConfirmSwitch === -1,
    });
    const companyOptions = publicData?.company_list?.map((c) => ({
        value: c.id,
        label: c.title,
    })) ?? [];
    const workTypeGroupedOptions = publicData?.worktype_list?.map((g) => ({
        label: g.title,
        options: (g.options ?? []).map((o) => ({ value: o.id, label: o.title })),
    })) ?? [];
    const resetConvertForm = () => {
        convertForm.resetFields();
        convertForm.setFieldsValue({
            trainCompanyName: '千铝工投',
            num: 0,
            traindemandId: 0,
        });
    };
    const handleSwitch = () => {
        const next = headConfirmSwitch === 0 ? -1 : 0;
        setHeadConfirmSwitch(next);
        if (next === -1) {
            setCurrentPage(1);
        }
    };
    const openConvertModal = async (prefill) => {
        await queryClient.ensureQueryData({
            queryKey: ['demand-public-data', 'requirements'],
            queryFn: () => trainApi.getDemandPublicData(['company', 'worktype']),
        });
        resetConvertForm();
        if (prefill) {
            convertForm.setFieldsValue({
                trainCompanyName: '千铝工投',
                ...prefill,
            });
        }
        setModalOpen(true);
    };
    const handleDemand = (row) => {
        openConvertModal({
            companyId: row.orgId,
            num: row.nums,
            traindemandId: row.id,
        });
    };
    const handleTrainDemand = () => {
        openConvertModal();
    };
    const handleTrainDemandClose = () => {
        setModalOpen(false);
        resetConvertForm();
    };
    const submitTrainDemand = async () => {
        try {
            const values = await convertForm.validateFields();
            if (submitting)
                return;
            setSubmitting(true);
            await trainApi.convertDemandToTraining({
                ...values,
                traindemandId: values.traindemandId || undefined,
            });
            message.success('提交成功');
            handleTrainDemandClose();
            await queryClient.invalidateQueries({ queryKey: ['demand-pendings'] });
            await queryClient.invalidateQueries({ queryKey: ['training-info-requirements'] });
            if (headConfirmSwitch === 0) {
                setHeadConfirmSwitch(-1);
                setCurrentPage(1);
            }
        }
        catch (e) {
            if (e && typeof e === 'object' && 'errorFields' in e) {
                return;
            }
            message.error('提交失败');
        }
        finally {
            setSubmitting(false);
        }
    };
    const handleReset = () => {
        setCompanyIdFilter(undefined);
        setWorkTypeIdFilter(undefined);
        setCurrentPage(1);
    };
    const pendingColumns = [
        { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
        { title: '接受培训单位', dataIndex: 'company', width: 300, align: 'center' },
        { title: '单位类型', dataIndex: 'companyType', width: 150, align: 'center' },
        { title: '内容', dataIndex: 'content', align: 'center' },
        {
            title: '发布人',
            dataIndex: 'publisher',
            width: 200,
            align: 'center',
            ellipsis: true,
            render: (v, row) => v || row.company || '-',
        },
        { title: '人数', dataIndex: 'nums', width: 200, align: 'center' },
        { title: '创建时间', dataIndex: 'addtimeCn', width: 200, align: 'center' },
        {
            title: '操作',
            key: 'act',
            width: 80,
            fixed: 'right',
            align: 'center',
            render: (_, row) => (_jsx(Button, { type: "primary", size: "small", onClick: () => handleDemand(row), children: "\u5904\u7406" })),
        },
    ];
    const doneColumns = [
        { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
        { title: '标题', dataIndex: 'title', align: 'center', width: 200 },
        { title: '接受培训公司', dataIndex: 'company_name', align: 'center' },
        { title: '培训机构', dataIndex: 'train_company_name', align: 'center', width: 150 },
        {
            title: '工种',
            key: 'workTypeTitle',
            align: 'center',
            width: 150,
            render: (_, row) => formatWorkTypeTitle(row),
        },
        { title: '人数', dataIndex: 'num', width: 60, align: 'center' },
        {
            title: '是否立项',
            dataIndex: 'pass_status',
            width: 100,
            align: 'center',
            render: (v) => (v === 0 ? '不立项' : '立项'),
        },
        { title: '创建时间', dataIndex: 'create_time', align: 'center', width: 174 },
        { title: '操作', key: 'act', width: 60, fixed: 'right', align: 'center', render: () => null },
    ];
    return (_jsxs("div", { className: "requirements-management-page training-page training-table-container", children: [_jsxs("section", { className: "page-heading", children: [_jsx(Typography.Title, { level: 4, children: "\u9700\u6C42\u7BA1\u7406" }), _jsx(Typography.Text, { type: "secondary", children: "\u5904\u7406\u4F01\u4E1A\u57F9\u8BAD\u9700\u6C42\uFF0C\u8DDF\u8E2A\u7ACB\u9879\u4E0E\u57F9\u8BAD\u9879\u76EE\u8FDB\u5EA6" })] }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsx("div", { className: "search-filter-bar", children: _jsxs("div", { className: "filter-actions", children: [_jsx(Button, { type: headConfirmSwitch === 0 ? 'primary' : 'default', onClick: handleSwitch, children: "\u5F85\u529E" }), _jsx(Button, { className: "tab-btn-spacer", type: headConfirmSwitch === -1 ? 'primary' : 'default', onClick: handleSwitch, children: "\u5DF2\u529E" }), headConfirmSwitch === -1 && (_jsxs(_Fragment, { children: [_jsx(Select, { className: "filter-select", value: companyIdFilter, onChange: (v) => {
                                            setCompanyIdFilter(v);
                                            setCurrentPage(1);
                                        }, placeholder: "\u4F01\u4E1A\u7B5B\u9009", allowClear: true, showSearch: true, optionFilterProp: "label", options: companyOptions }), _jsx(Select, { className: "filter-select", value: workTypeIdFilter, onChange: (v) => {
                                            setWorkTypeIdFilter(v);
                                            setCurrentPage(1);
                                        }, placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD", allowClear: true, showSearch: true, optionFilterProp: "label", options: workTypeGroupedOptions }), _jsx(Button, { type: "primary", onClick: handleReset, children: "\u91CD\u7F6E" }), _jsx(Button, { type: "primary", onClick: handleTrainDemand, children: "\u6DFB\u52A0" })] }))] }) }) }), headConfirmSwitch === 0 && (_jsx(Table, { className: "requirements-table", rowKey: "id", bordered: true, size: "middle", loading: pendingsLoading, columns: pendingColumns, dataSource: pendings?.items ?? [], scroll: { x: 900, y: 600 }, pagination: false })), headConfirmSwitch === -1 && (_jsxs(_Fragment, { children: [_jsx(Table, { className: "requirements-table", rowKey: "id", bordered: true, size: "middle", loading: infoListLoading, columns: doneColumns, dataSource: infoList?.list ?? [], scroll: { x: 1000, y: 600 }, pagination: false }), _jsx(Pagination, { className: "requirements-pagination", current: currentPage, pageSize: pageSize, total: infoList?.total ?? 0, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showQuickJumper: true, showTotal: (t) => `共 ${t} 条`, onChange: (p, ps) => {
                            if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                                setPageSize(ps);
                                setCurrentPage(1);
                            }
                            else {
                                setCurrentPage(p);
                            }
                        } })] })), _jsx(Modal, { title: "\u57F9\u8BAD\u9700\u6C42\u7BA1\u7406", open: modalOpen, onOk: submitTrainDemand, onCancel: handleTrainDemandClose, confirmLoading: submitting, destroyOnHidden: true, width: "50%", okText: "\u786E \u5B9A", cancelText: "\u53D6 \u6D88", children: _jsxs(Form, { form: convertForm, className: "convert-form", labelCol: { span: 6 }, wrapperCol: { span: 16 }, children: [_jsx(Form.Item, { name: "title", label: "\u6807\u9898", rules: [{ required: true, message: '请输入标题' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u6807\u9898" }) }), _jsx(Form.Item, { name: "companyId", label: "\u63A5\u53D7\u57F9\u8BAD\u7684\u5355\u4F4D", rules: [{ required: true, message: '请选择接受培训的单位' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", placeholder: "\u8BF7\u9009\u62E9\u63A5\u53D7\u57F9\u8BAD\u7684\u5355\u4F4D", options: companyOptions }) }), _jsx(Form.Item, { name: "trainCompanyName", label: "\u57F9\u8BAD\u673A\u6784", children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u57F9\u8BAD\u673A\u6784", options: TRAIN_COMPANY_OPTIONS }) }), _jsx(Form.Item, { name: "workTypeId", label: "\u5DE5\u79CD", rules: [{ required: true, message: '请选择工种' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD", options: workTypeGroupedOptions }) }), _jsx(Form.Item, { name: "workTypeLevel", label: "\u7B49\u7EA7", rules: [{ required: true, message: '请选择工种等级' }], children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u7B49\u7EA7", options: WORK_LEVELS }) }), _jsx(Form.Item, { name: "num", label: "\u4EBA\u6570", rules: [{ required: true, message: '请输入正确的人数' }], children: _jsx(InputNumber, { min: 1, style: { width: '100%' }, placeholder: "\u8BF7\u8F93\u5165\u4EBA\u6570" }) }), _jsx(Form.Item, { name: "traindemandId", hidden: true, children: _jsx(InputNumber, {}) })] }) })] }));
}
