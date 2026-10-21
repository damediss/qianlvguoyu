import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 企业人才需求采集 — 对齐 yun_shang company/DemandPeople.vue
 */
import { useMemo, useState } from 'react';
import { Button, Form, Input, Modal, Pagination, Select, Space, Table, Typography, message, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import './EnterpriseTalentDemandPage.css';
const EDUCATION_OPTIONS = [
    '不限',
    '高中及以下',
    '中专/技校',
    '大专',
    '本科',
    '硕士',
    '博士',
];
function formatAddtime(ts) {
    if (!ts)
        return '-';
    const d = new Date(ts * 1000);
    return d.toLocaleString('zh-CN', { hour12: false });
}
export function EnterpriseTalentDemandPage() {
    const queryClient = useQueryClient();
    const orgId = useAuthStore((s) => s.currentUser?.orgId);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);
    const [formOpen, setFormOpen] = useState(false);
    const [editing, setEditing] = useState(null);
    const [form] = Form.useForm();
    const [submitting, setSubmitting] = useState(false);
    const queryParams = useMemo(() => ({ page, pageSize, orgId }), [page, pageSize, orgId]);
    const { data, isLoading } = useQuery({
        queryKey: ['enterprise-demand-employees', queryParams],
        queryFn: () => trainApi.pageDemandEmployees(queryParams),
        enabled: orgId != null,
    });
    const openCreate = () => {
        setEditing(null);
        form.setFieldsValue({ work: '', education: '不限', major: '' });
        setFormOpen(true);
    };
    const openEdit = (row) => {
        setEditing(row);
        form.setFieldsValue({
            work: row.work ?? '',
            education: row.education ?? '不限',
            major: row.major ?? '',
        });
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
                await trainApi.updateDemandEmployee(editing.id, {
                    orgId,
                    work: v.work,
                    education: v.education,
                    major: v.major,
                });
            }
            else {
                await trainApi.createDemandEmployee({
                    orgId,
                    work: v.work,
                    education: v.education,
                    major: v.major,
                });
            }
            message.success('提交成功');
            setFormOpen(false);
            queryClient.invalidateQueries({ queryKey: ['enterprise-demand-employees'] });
        }
        catch {
            message.error('提交失败');
        }
        finally {
            setSubmitting(false);
        }
    };
    const handleDelete = (ids) => {
        Modal.confirm({
            title: '提示',
            content: '此操作将永久删除，是否继续？',
            onOk: async () => {
                if (ids.length === 1) {
                    await trainApi.deleteDemandEmployee(ids[0]);
                }
                else {
                    await trainApi.batchDeleteDemandEmployees(ids);
                }
                message.success('删除成功');
                setSelectedRowKeys([]);
                queryClient.invalidateQueries({ queryKey: ['enterprise-demand-employees'] });
            },
        });
    };
    const columns = [
        { title: '创建日期', dataIndex: 'addtime', width: 170, render: (v) => formatAddtime(v) },
        { title: '岗位', dataIndex: 'work', ellipsis: true },
        { title: '学历', dataIndex: 'education', width: 120 },
        { title: '专业', dataIndex: 'major', width: 140, ellipsis: true },
        {
            title: '操作',
            key: 'act',
            width: 120,
            fixed: 'right',
            render: (_, row) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", onClick: () => openEdit(row), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, onClick: () => handleDelete([row.id]), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs("div", { className: "enterprise-talent-demand-page", children: [_jsx(Typography.Title, { level: 4, children: "\u4EBA\u624D\u9700\u6C42" }), _jsx("div", { className: "enterprise-talent-demand-toolbar", children: _jsxs(Space, { children: [_jsx(Button, { type: "primary", onClick: openCreate, children: "\u6DFB\u52A0" }), _jsx(Button, { danger: true, disabled: selectedRowKeys.length === 0, onClick: () => handleDelete(selectedRowKeys), children: "\u5220\u9664" })] }) }), _jsx(Table, { rowKey: "id", loading: isLoading, dataSource: data?.items ?? [], columns: columns, rowSelection: {
                    selectedRowKeys,
                    onChange: (keys) => setSelectedRowKeys(keys),
                }, pagination: false, scroll: { x: 720 } }), _jsx(Pagination, { className: "enterprise-talent-demand-pagination", total: data?.total ?? 0, current: page, pageSize: pageSize, showSizeChanger: true, showTotal: (t) => `共 ${t} 条`, onChange: (p, ps) => {
                    if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                        setPageSize(ps);
                        setPage(1);
                    }
                    else {
                        setPage(p);
                    }
                } }), _jsx(Modal, { title: "\u4EBA\u624D\u9700\u6C42\u4FE1\u606F", open: formOpen, onCancel: () => setFormOpen(false), onOk: handleSubmit, confirmLoading: submitting, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { name: "work", label: "\u5C97\u4F4D\u540D\u79F0", rules: [{ required: true, message: '请输入岗位名称' }], children: _jsx(Input, { maxLength: 255 }) }), _jsx(Form.Item, { name: "education", label: "\u5B66\u5386\u8981\u6C42", rules: [{ required: true, message: '请选择学历要求' }], children: _jsx(Select, { options: EDUCATION_OPTIONS.map((v) => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "major", label: "\u4E13\u4E1A\u8981\u6C42", children: _jsx(Input, { maxLength: 255 }) })] }) })] }));
}
