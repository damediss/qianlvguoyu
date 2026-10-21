import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CheckCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Empty, Form, Progress, Select, Space, Table, Tag, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { getPreparationChecklist, listPreparationTaskExamStaff, updatePreparationChecklist, } from '../../../api/certificationPreparation';
import { PermissionGuard } from '../../../directives/permission';
import { dateTimeText } from './shared';
export function ChecklistReview({ taskId, onChanged }) {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const [checkedItemIds, setCheckedItemIds] = useState([]);
    const checklistQuery = useQuery({
        queryKey: ['certification-preparation-checklist', taskId],
        queryFn: () => getPreparationChecklist(taskId),
        enabled: Boolean(taskId),
    });
    const staffQuery = useQuery({
        queryKey: ['certification-preparation-task-exam-staff', taskId],
        queryFn: () => listPreparationTaskExamStaff(taskId),
        enabled: Boolean(taskId),
    });
    const checklist = checklistQuery.data;
    const items = checklist?.items || [];
    const allChecked = items.length > 0 && checkedItemIds.length === items.length;
    useEffect(() => {
        if (!checklist) {
            setCheckedItemIds([]);
            form.resetFields();
            return;
        }
        setCheckedItemIds(checklist.items.filter((item) => item.checked).map((item) => item.id));
        form.setFieldsValue({ confirmerTaskStaffId: checklist.confirmerTaskStaffId });
    }, [checklist, form]);
    const staffOptions = useMemo(() => (staffQuery.data || []).map((staff) => ({
        label: `${staff.staffName}${staff.roleName ? ` / ${staff.roleName}` : ''}`,
        value: staff.id,
    })), [staffQuery.data]);
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-checklist', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-exam-staff', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
        onChanged?.();
    };
    const confirmMutation = useMutation({
        mutationFn: (values) => updatePreparationChecklist(taskId, {
            checkedItemIds: checkedItemIds.map(String),
            confirmerTaskStaffId: values.confirmerTaskStaffId,
        }),
        onSuccess: () => {
            message.success('清单核查已确认');
            refresh();
        },
    });
    const columns = [
        { title: '序号', dataIndex: 'itemNo', width: 80 },
        { title: '名称', dataIndex: 'itemName', minWidth: 260, ellipsis: true },
        { title: '数量', dataIndex: 'quantityText', width: 120, render: (value) => value || '-' },
        {
            title: '核查状态',
            dataIndex: 'checked',
            width: 120,
            render: (value) => value ? _jsx(Tag, { color: "success", children: "\u5DF2\u6838" }) : _jsx(Tag, { children: "\u5F85\u6838" }),
        },
    ];
    if (!taskId) {
        return _jsx(Empty, { description: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u540E\u8FDB\u884C\u6E05\u5355\u6838\u67E5" });
    }
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsx(ProCard, { title: "\u6E05\u5355\u6838\u67E5", extra: _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }), _jsx(Button, { disabled: !items.length, onClick: () => setCheckedItemIds(items.map((item) => item.id)), children: "\u5168\u90E8\u52FE\u9009" })] }), children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsxs("div", { className: "knowledge-overview", children: [_jsxs(ProCard, { className: "overview-card", children: [_jsx(Typography.Text, { type: "secondary", children: "\u6750\u6599\u603B\u6570" }), _jsx(Typography.Title, { level: 3, style: { margin: 0 }, children: checklist?.totalCount || 0 })] }), _jsxs(ProCard, { className: "overview-card", children: [_jsx(Typography.Text, { type: "secondary", children: "\u5DF2\u6838\u6750\u6599" }), _jsx(Typography.Title, { level: 3, style: { margin: 0 }, children: checkedItemIds.length })] }), _jsxs(ProCard, { className: "overview-card", children: [_jsx(Typography.Text, { type: "secondary", children: "\u786E\u8BA4\u4EBA" }), _jsx(Typography.Title, { level: 5, style: { margin: '6px 0 0' }, children: checklist?.confirmerName || '-' })] }), _jsxs(ProCard, { className: "overview-card", children: [_jsx(Typography.Text, { type: "secondary", children: "\u786E\u8BA4\u65F6\u95F4" }), _jsx(Typography.Title, { level: 5, style: { margin: '6px 0 0' }, children: dateTimeText(checklist?.confirmedAt) })] })] }), _jsx(Progress, { percent: items.length ? Math.round((checkedItemIds.length * 100) / items.length) : 0 }), !allChecked ? (_jsx(Alert, { type: "warning", showIcon: true, message: "\u5168\u90E8\u6750\u6599\u52FE\u9009\u5B8C\u6BD5\u540E\uFF0C\u624D\u53EF\u4EE5\u9009\u62E9\u786E\u8BA4\u4EBA\u5E76\u5B8C\u6210\u6E05\u5355\u6838\u67E5\u3002" })) : null, _jsx(Table, { rowKey: "id", columns: columns, dataSource: items, loading: checklistQuery.isFetching, pagination: false, rowSelection: {
                                selectedRowKeys: checkedItemIds,
                                onChange: setCheckedItemIds,
                            }, scroll: { x: 720, y: 520 } })] }) }), _jsx(ProCard, { title: "\u6838\u67E5\u786E\u8BA4", children: _jsxs(Form, { form: form, layout: "inline", onFinish: (values) => confirmMutation.mutate(values), children: [_jsx(Form.Item, { name: "confirmerTaskStaffId", label: "\u786E\u8BA4\u4EBA", rules: [{ required: true, message: '请选择确认人' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", placeholder: "\u8BF7\u9009\u62E9\u4EFB\u52A1\u8003\u52A1\u4EBA\u5458", style: { width: 260 }, loading: staffQuery.isFetching, options: staffOptions, disabled: !allChecked }) }), _jsx(PermissionGuard, { codes: ['certification.preparation.checklist.confirm'], children: _jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), htmlType: "submit", loading: confirmMutation.isPending, disabled: !allChecked, children: "\u786E\u8BA4\u6E05\u5355" }) })] }) })] }));
}
