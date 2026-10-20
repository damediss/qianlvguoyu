import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { assignPreparationExamStaff, listPreparationTaskExamStaff, removePreparationTaskExamStaff, updatePreparationTaskExamStaffRole, } from '../../../api/certificationPreparation';
import { listAvailableExamStaff } from '../../../api/certificationInfo';
import { PermissionGate } from '../../../directives/permission';
import { dash, statusTag } from './shared';
const EXAM_STAFF_ROLE_OPTIONS = [
    { value: 'internal_supervisor', label: '内督', color: 'gold' },
    { value: 'exam_staff', label: '考务员', color: 'blue' },
    { value: 'invigilator', label: '监考员', color: 'green' },
    { value: 'worker', label: '工作人员', color: 'default' },
];
function roleOption(role) {
    return EXAM_STAFF_ROLE_OPTIONS.find((item) => item.value === role) ?? EXAM_STAFF_ROLE_OPTIONS[1];
}
function roleLabel(role, roleName) {
    return roleName || roleOption(role).label;
}
export function ExamStaffConfig({ taskId, onChanged }) {
    const queryClient = useQueryClient();
    const [assignModalOpen, setAssignModalOpen] = useState(false);
    const [assignForm] = Form.useForm();
    const availableStaffQuery = useQuery({
        queryKey: ['certification-info-exam-staff-available'],
        queryFn: () => listAvailableExamStaff(),
        enabled: assignModalOpen,
    });
    const taskStaffQuery = useQuery({
        queryKey: ['certification-preparation-task-exam-staff', taskId],
        queryFn: () => listPreparationTaskExamStaff(taskId),
        enabled: Boolean(taskId),
    });
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-exam-staff', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-info-exam-staff-available'] });
        queryClient.invalidateQueries({ queryKey: ['certification-info-exam-staff'] });
        onChanged?.();
    };
    const assignMutation = useMutation({
        mutationFn: (values) => {
            const option = roleOption(values.taskRole);
            return assignPreparationExamStaff(taskId, {
                staffId: values.staffId,
                taskRole: option.value,
                roleName: option.label,
                remark: values.remark,
            });
        },
        onSuccess: () => {
            message.success('考务人员已绑定到任务');
            setAssignModalOpen(false);
            assignForm.resetFields();
            refresh();
        },
    });
    const updateRoleMutation = useMutation({
        mutationFn: ({ taskStaffId, taskRole }) => {
            const option = roleOption(taskRole);
            return updatePreparationTaskExamStaffRole(taskId, taskStaffId, {
                taskRole: option.value,
                roleName: option.label,
            });
        },
        onSuccess: () => {
            message.success('角色已更新');
            refresh();
        },
    });
    const removeMutation = useMutation({
        mutationFn: (taskStaffId) => removePreparationTaskExamStaff(taskId, taskStaffId),
        onSuccess: () => {
            message.success('任务考务人员已删除');
            refresh();
        },
    });
    const taskStaffColumns = [
        { title: '姓名', dataIndex: 'staffName', width: 140 },
        { title: '联系电话', dataIndex: 'maskedPhone', width: 160, render: dash },
        {
            title: '角色',
            dataIndex: 'taskRole',
            width: 160,
            render: (_, record) => {
                const option = roleOption(record.taskRole);
                return _jsx(Tag, { color: option.color, children: roleLabel(record.taskRole, record.roleName) });
            },
        },
        { title: '备注', dataIndex: 'remark', ellipsis: true, render: dash },
        { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
        {
            title: '操作',
            width: 240,
            render: (_, record) => (_jsx(PermissionGate, { code: "certification.preparation.staff.assign", children: _jsxs(Space, { size: 8, children: [_jsx(Select, { size: "small", value: record.taskRole || (record.isLeader ? 'internal_supervisor' : 'exam_staff'), options: EXAM_STAFF_ROLE_OPTIONS.map(({ value, label }) => ({ value, label })), style: { width: 116 }, loading: updateRoleMutation.isPending, onChange: (taskRole) => updateRoleMutation.mutate({ taskStaffId: record.id, taskRole }) }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u8003\u52A1\u4EBA\u5458\uFF1F", okText: "\u5220\u9664", cancelText: "\u53D6\u6D88", onConfirm: () => removeMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] }) })),
        },
    ];
    const availableStaffOptions = (availableStaffQuery.data || []).map((item) => ({
        label: `${item.staffName}${item.maskedPhone ? `（${item.maskedPhone}）` : ''}`,
        value: item.id,
    }));
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsx(ProCard, { title: "\u4EFB\u52A1\u8003\u52A1\u4EBA\u5458", extra: _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }), _jsx(PermissionGate, { code: "certification.preparation.staff.assign", children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), disabled: !taskId, onClick: () => {
                                    assignForm.resetFields();
                                    assignForm.setFieldsValue({ taskRole: 'exam_staff' });
                                    setAssignModalOpen(true);
                                }, children: "\u7ED1\u5B9A\u8003\u52A1\u4EBA\u5458" }) })] }), children: !taskId ? (_jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u540E\u7ED1\u5B9A\u8003\u52A1\u4EBA\u5458\u3002" })) : (_jsxs(_Fragment, { children: [_jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 12 }, children: "\u8003\u52A1\u4EBA\u5458\u57FA\u7840\u4FE1\u606F\u8BF7\u5728\u201C\u4FE1\u606F\u7BA1\u7406 \u2192 \u8003\u52A1\u4FE1\u606F\u201D\u4E2D\u7EF4\u62A4\u5E76\u5BA1\u6838\u901A\u8FC7\u540E\uFF0C\u518D\u7ED1\u5B9A\u5230\u672C\u4EFB\u52A1\u3002" }), _jsx(Table, { rowKey: "id", columns: taskStaffColumns, dataSource: taskStaffQuery.data || [], loading: taskStaffQuery.isFetching, pagination: false, scroll: { x: 900 } })] })) }), _jsx(Modal, { title: "\u7ED1\u5B9A\u8003\u52A1\u4EBA\u5458\u5230\u8BA4\u5B9A\u4EFB\u52A1", open: assignModalOpen, onOk: () => assignForm.validateFields().then((values) => assignMutation.mutate(values)), onCancel: () => setAssignModalOpen(false), confirmLoading: assignMutation.isPending, width: 520, children: _jsxs(Form, { form: assignForm, layout: "vertical", initialValues: { taskRole: 'exam_staff' }, children: [_jsx(Form.Item, { name: "staffId", label: "\u8003\u52A1\u4EBA\u5458", rules: [{ required: true, message: '请选择考务人员' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", loading: availableStaffQuery.isFetching, placeholder: "\u8BF7\u9009\u62E9\u5DF2\u5BA1\u6838\u901A\u8FC7\u7684\u8003\u52A1\u4EBA\u5458", options: availableStaffOptions, notFoundContent: availableStaffQuery.isFetching
                                    ? '加载中...'
                                    : '暂无可用考务人员，请先在信息管理中添加并审核通过' }) }), _jsx(Form.Item, { name: "taskRole", label: "\u89D2\u8272", rules: [{ required: true, message: '请选择角色' }], children: _jsx(Select, { options: EXAM_STAFF_ROLE_OPTIONS.map(({ value, label }) => ({ value, label })) }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2, placeholder: "\u53EF\u9009\uFF0C\u5982\u5C97\u4F4D\u804C\u8D23\u8BF4\u660E" }) })] }) })] }));
}
