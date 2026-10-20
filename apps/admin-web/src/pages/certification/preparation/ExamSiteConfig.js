import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, TimePicker, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useState } from 'react';
import { assignPreparationExamSite, listPreparationTaskExamStaff, listPreparationTaskExamSites, removePreparationTaskExamSite, } from '../../../api/certificationPreparation';
import { listAvailableExamSites } from '../../../api/certificationInfo';
import { PermissionGate } from '../../../directives/permission';
import { dash, dateText, formatDate, formGridStyle, statusTag } from './shared';
const examTypeOptions = [
    { label: '理论', value: 'theory' },
    { label: '实操', value: 'practical' },
];
function examTypeTag(type) {
    if (type === 'practical') {
        return _jsx(Tag, { color: "green", children: "\u5B9E\u64CD" });
    }
    if (type === 'theory') {
        return _jsx(Tag, { color: "blue", children: "\u7406\u8BBA" });
    }
    return dash(type);
}
export function ExamSiteConfig({ taskId, onChanged }) {
    const queryClient = useQueryClient();
    const [assignModalOpen, setAssignModalOpen] = useState(false);
    const [assignForm] = Form.useForm();
    const availableSitesQuery = useQuery({
        queryKey: ['certification-info-sites-available'],
        queryFn: () => listAvailableExamSites(),
        enabled: assignModalOpen,
    });
    const taskSitesQuery = useQuery({
        queryKey: ['certification-preparation-task-sites', taskId],
        queryFn: () => listPreparationTaskExamSites(taskId),
        enabled: Boolean(taskId),
    });
    const taskStaffQuery = useQuery({
        queryKey: ['certification-preparation-task-exam-staff', taskId],
        queryFn: () => listPreparationTaskExamStaff(taskId),
        enabled: Boolean(taskId && assignModalOpen),
    });
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-sites', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-task-exam-staff', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-info-sites-available'] });
        onChanged?.();
    };
    const assignMutation = useMutation({
        mutationFn: (values) => assignPreparationExamSite(taskId, {
            siteId: values.siteId,
            examDate: formatDate(values.examDate),
            startTime: formatTime(values.timeRange?.[0]),
            endTime: formatTime(values.timeRange?.[1]),
            examType: values.examType,
            arrangedCount: values.arrangedCount,
            contactName: values.contactName,
            contactPhone: values.contactPhone,
            remark: values.remark,
        }),
        onSuccess: () => {
            message.success('考场已绑定到任务');
            setAssignModalOpen(false);
            assignForm.resetFields();
            refresh();
        },
    });
    const removeMutation = useMutation({
        mutationFn: (taskSiteId) => removePreparationTaskExamSite(taskId, taskSiteId),
        onSuccess: () => {
            message.success('任务考场已移除');
            refresh();
        },
    });
    const taskSiteColumns = [
        { title: '考场', dataIndex: 'siteName', width: 180 },
        { title: '考试日期', dataIndex: 'examDate', width: 120, render: dateText },
        { title: '时间', width: 140, render: (_, row) => `${row.startTime || '-'} - ${row.endTime || '-'}` },
        { title: '考试类型', dataIndex: 'examType', width: 100, render: examTypeTag },
        { title: '安排人数', dataIndex: 'arrangedCount', width: 100, render: dash },
        { title: '考位', dataIndex: 'seatCount', width: 90, render: dash },
        { title: '负责人', dataIndex: 'contactName', width: 110, render: dash },
        { title: '联系电话', dataIndex: 'contactPhone', width: 140, render: dash },
        { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
        {
            title: '操作',
            width: 110,
            render: (_, record) => (_jsx(PermissionGate, { code: "certification.preparation.site.assign", children: _jsx(Popconfirm, { title: "\u786E\u8BA4\u79FB\u9664\u8BE5\u4EFB\u52A1\u8003\u573A\uFF1F", okText: "\u79FB\u9664", cancelText: "\u53D6\u6D88", onConfirm: () => removeMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), children: "\u79FB\u9664" }) }) })),
        },
    ];
    function openAssign(site) {
        assignForm.resetFields();
        assignForm.setFieldsValue({
            siteId: site?.id,
            arrangedCount: site?.seatCount,
            examType: site?.siteType === 'practical' ? 'practical' : 'theory',
        });
        setAssignModalOpen(true);
    }
    function onTaskStaffChange(taskStaffId) {
        const staff = (taskStaffQuery.data || []).find((item) => item.id === taskStaffId);
        assignForm.setFieldsValue({
            contactName: staff?.staffName,
            contactPhone: staff?.phone || staff?.maskedPhone,
        });
    }
    function submitAssign() {
        assignForm.validateFields().then((values) => {
            const staff = (taskStaffQuery.data || []).find((item) => item.id === values.taskStaffId);
            if (!staff) {
                assignForm.setFields([{ name: 'taskStaffId', errors: ['请从本任务已配置的考务人员中选择'] }]);
                return;
            }
            values.contactName = staff.staffName;
            values.contactPhone = staff.phone || staff.maskedPhone;
            const site = (availableSitesQuery.data || []).find((item) => item.id === values.siteId);
            if (site?.seatCount !== undefined && values.arrangedCount !== undefined && values.arrangedCount > site.seatCount) {
                assignForm.setFields([{ name: 'arrangedCount', errors: ['安排人数不得超过考场考位数量'] }]);
                return;
            }
            const [startTime, endTime] = values.timeRange || [];
            if (startTime && endTime && !endTime.isAfter(startTime)) {
                assignForm.setFields([{ name: 'timeRange', errors: ['结束时间须晚于开始时间'] }]);
                return;
            }
            assignMutation.mutate(values);
        });
    }
    const availableSiteOptions = (availableSitesQuery.data || []).map((item) => ({
        label: `${item.siteName}（${item.seatCount || 0} 位）`,
        value: item.id,
    }));
    const taskStaffOptions = (taskStaffQuery.data || []).map((item) => ({
        label: `${item.staffName}${item.maskedPhone ? `（${item.maskedPhone}）` : ''}`,
        value: item.id,
    }));
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsx(ProCard, { title: "\u4EFB\u52A1\u8003\u573A", extra: _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }), _jsx(PermissionGate, { code: "certification.preparation.site.assign", children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), disabled: !taskId, onClick: () => openAssign(), children: "\u7ED1\u5B9A\u8003\u573A" }) })] }), children: !taskId ? (_jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u540E\u7ED1\u5B9A\u8003\u573A\u3002" })) : (_jsxs(_Fragment, { children: [_jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 12 }, children: "\u8003\u573A\u57FA\u7840\u4FE1\u606F\u8BF7\u5728\u201C\u4FE1\u606F\u7BA1\u7406 \u2192 \u8003\u573A\u4FE1\u606F\u201D\u4E2D\u7EF4\u62A4\u5E76\u5BA1\u6838\u901A\u8FC7\u540E\uFF0C\u518D\u7ED1\u5B9A\u5230\u672C\u4EFB\u52A1\u3002" }), _jsx(Table, { rowKey: "id", columns: taskSiteColumns, dataSource: taskSitesQuery.data || [], loading: taskSitesQuery.isFetching, pagination: false, scroll: { x: 1080 } })] })) }), _jsx(Modal, { title: "\u7ED1\u5B9A\u8003\u573A\u5230\u8BA4\u5B9A\u4EFB\u52A1", open: assignModalOpen, onOk: submitAssign, onCancel: () => setAssignModalOpen(false), confirmLoading: assignMutation.isPending, width: 680, children: _jsxs(Form, { form: assignForm, layout: "vertical", initialValues: { examType: 'theory' }, children: [_jsx(Form.Item, { name: "siteId", label: "\u8003\u573A", rules: [{ required: true, message: '请选择考场' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", loading: availableSitesQuery.isFetching, placeholder: "\u8BF7\u9009\u62E9\u5DF2\u5BA1\u6838\u901A\u8FC7\u7684\u8003\u573A", options: availableSiteOptions, notFoundContent: availableSitesQuery.isFetching
                                    ? '加载中...'
                                    : '暂无可用考场，请先在信息管理中添加并审核通过' }) }), _jsxs("div", { style: formGridStyle, children: [_jsx(Form.Item, { name: "examDate", label: "\u8003\u8BD5\u65E5\u671F", children: _jsx(DatePicker, { style: { width: '100%' }, disabledDate: (current) => current && current < dayjs().startOf('day') }) }), _jsx(Form.Item, { name: "arrangedCount", label: "\u5B89\u6392\u4EBA\u6570", rules: [{ type: 'number', min: 1, message: '请输入有效人数' }], children: _jsx(InputNumber, { min: 1, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "timeRange", label: "\u8003\u8BD5\u65F6\u95F4", children: _jsx(TimePicker.RangePicker, { format: "HH:mm", minuteStep: 5, style: { width: '100%' }, placeholder: ['开始时间', '结束时间'] }) }), _jsxs(Space.Compact, { style: { width: '100%' }, children: [_jsx(Form.Item, { name: "taskStaffId", label: "\u8003\u8BD5\u8D1F\u8D23\u4EBA", rules: [{ required: true, message: '请从本任务考务人员中选择' }], style: { width: '50%', marginBottom: 0 }, children: _jsx(Select, { showSearch: true, optionFilterProp: "label", loading: taskStaffQuery.isFetching, placeholder: "\u8BF7\u9009\u62E9\u8D1F\u8D23\u4EBA", options: taskStaffOptions, onChange: onTaskStaffChange, notFoundContent: taskStaffQuery.isFetching
                                                    ? '加载中...'
                                                    : '请先在“考务配置”中绑定考务人员' }) }), _jsx(Form.Item, { name: "examType", label: "\u8003\u8BD5\u7C7B\u578B", rules: [{ required: true, message: '请选择考试类型' }], style: { width: '50%', marginBottom: 0 }, children: _jsx(Select, { options: examTypeOptions }) })] }), _jsx(Form.Item, { name: "contactName", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "contactPhone", hidden: true, children: _jsx(Input, {}) })] }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input, {}) })] }) })] }));
}
function formatTime(value) {
    return value?.format('HH:mm');
}
