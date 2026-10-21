import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
/** 与 PHP trainingInformation 一致：0 进行中 / 1 待确认 / 2 已完成 */
const STATUS_MAP = {
    0: { label: '进行中', color: 'processing' },
    1: { label: '待确认', color: 'default' },
    2: { label: '已完成', color: 'success' },
};
function formatWorkTypeTitle(item) {
    const title = item.work_type_title ?? '-';
    const level = item.work_type_level;
    const levelText = level && level > 0 ? `${level}级` : '-';
    return `${title}(${levelText})`;
}
export function TrainingInfoPage() {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize] = useState(10);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [editingTitle, setEditingTitle] = useState('');
    const [companyId, setCompanyId] = useState();
    const [workTypeId, setWorkTypeId] = useState();
    const listQuery = useMemo(() => ({
        page: currentPage,
        limit: pageSize,
        yundemandList: true,
        companyId,
        workTypeId,
    }), [currentPage, pageSize, companyId, workTypeId]);
    const { data: publicData } = useQuery({
        queryKey: ['demand-public-data', 'training-info'],
        queryFn: () => trainApi.getDemandPublicData(['company', 'worktype']),
        staleTime: 5 * 60 * 1000,
    });
    const { data, isLoading } = useQuery({
        queryKey: ['training-info', listQuery],
        queryFn: () => trainApi.pageInfoLists(listQuery),
    });
    const updateMu = useMutation({
        mutationFn: ({ id, p }) => trainApi.updateInfoList(id, p),
        onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-info'] }); },
        onError: () => message.error('更新失败'),
    });
    const deleteMu = useMutation({
        mutationFn: (id) => trainApi.deleteInfoList(id),
        onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-info'] }); },
    });
    const closeModal = () => { setModalOpen(false); setEditingId(null); setEditingTitle(''); form.resetFields(); };
    const openEdit = (r) => {
        setEditingId(r.id);
        setEditingTitle(r.title);
        form.setFieldsValue({ courseNum: r.course_num, position: r.position, status: r.status });
        setModalOpen(true);
    };
    const handleSubmit = async () => {
        const v = await form.validateFields();
        if (editingId)
            updateMu.mutate({ id: editingId, p: v });
    };
    const handleReset = () => {
        setCompanyId(undefined);
        setWorkTypeId(undefined);
        setCurrentPage(1);
    };
    const companyOptions = (publicData?.company_list ?? []).map(c => ({ value: c.id, label: c.title }));
    const workTypeOptions = (publicData?.worktype_list ?? []).flatMap(g => [
        { label: g.title, options: (g.options ?? []).map(o => ({ value: o.id, label: o.title })) },
    ]);
    const columns = [
        {
            title: '序号', key: 'index', width: 60,
            render: (_, __, i) => (currentPage - 1) * pageSize + i + 1,
        },
        { title: '培训信息标题', dataIndex: 'title', width: 180, ellipsis: true },
        { title: '接受培训公司', dataIndex: 'company_name', width: 160, ellipsis: true },
        { title: '培训机构', dataIndex: 'train_company_name', width: 120 },
        { title: '工种', dataIndex: 'work_type_title', width: 140, render: (_, row) => formatWorkTypeTitle(row) },
        { title: '人数', dataIndex: 'num', width: 70, align: 'center' },
        { title: '地点', dataIndex: 'position', width: 100 },
        { title: '课时', dataIndex: 'course_num', width: 70, align: 'center' },
        {
            title: '企业确认', dataIndex: 'company_has_confirm', width: 100,
            render: (v) => v === 1 ? _jsx(Tag, { color: "success", children: "\u5DF2\u786E\u8BA4" }) : _jsx(Tag, { color: "error", children: "\u672A\u786E\u8BA4" }),
        },
        {
            title: '状态', dataIndex: 'status', width: 100,
            render: (v) => {
                const s = STATUS_MAP[v] ?? { label: '未知', color: 'default' };
                return _jsx(Tag, { color: s.color, children: s.label });
            },
        },
        { title: '创建时间', dataIndex: 'create_time', width: 160 },
        {
            title: '操作', key: 'act', width: 150, fixed: 'right',
            render: (_, r) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(r), children: "\u4FEE\u6539" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664?", onConfirm: () => deleteMu.mutate(r.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    return (_jsxs("div", { className: "training-page", children: [_jsxs("section", { className: "page-heading", children: [_jsx(Typography.Title, { level: 4, children: "\u57F9\u8BAD\u4FE1\u606F" }), _jsx(Typography.Text, { type: "secondary", children: "\u7BA1\u7406\u57F9\u8BAD\u4FE1\u606F\uFF0C\u8DDF\u8E2A\u57F9\u8BAD\u9879\u76EE\u72B6\u6001\u548C\u8FDB\u5EA6" })] }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Space, { wrap: true, children: [_jsx(Select, { allowClear: true, showSearch: true, placeholder: "\u4F01\u4E1A\u7B5B\u9009", style: { width: 220 }, filterOption: (input, option) => (option?.label || '').includes(input), value: companyId, onChange: v => { setCompanyId(v); setCurrentPage(1); }, options: companyOptions }), _jsx(Select, { allowClear: true, showSearch: true, placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD", style: { width: 220 }, filterOption: (input, option) => (option?.label || '').includes(input), value: workTypeId, onChange: v => { setWorkTypeId(v); setCurrentPage(1); }, options: workTypeOptions }), _jsx(Button, { type: "primary", onClick: handleReset, children: "\u91CD\u7F6E" })] }) }), _jsx(Card, { size: "small", children: _jsx(Table, { rowKey: "id", size: "middle", loading: isLoading, columns: columns, dataSource: data?.list ?? [], scroll: { x: 1300 }, pagination: {
                        current: currentPage,
                        pageSize,
                        total: data?.total ?? 0,
                        showSizeChanger: false,
                        showTotal: t => `共 ${t} 条`,
                        position: ['bottomRight'],
                        onChange: p => setCurrentPage(p),
                    } }) }), _jsx(Modal, { title: editingTitle ? `${editingTitle}-信息编辑` : '编辑培训信息', open: modalOpen, onOk: handleSubmit, onCancel: closeModal, confirmLoading: updateMu.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { name: "courseNum", label: "\u8BFE\u65F6", children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "position", label: "\u5730\u70B9", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u5730\u70B9" }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: Object.entries(STATUS_MAP).map(([k, v]) => ({ value: Number(k), label: v.label })) }) })] }) })] }));
}
