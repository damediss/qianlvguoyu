import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CheckCircleOutlined, DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, UserAddOutlined } from '@ant-design/icons';
import { Button, Card, DatePicker, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { assignCertificationExamSessionCandidates, completeCertificationExamSessionCollectionConfig, createCertificationExamSession, deleteCertificationExamSession, listCertificationExamSessionCandidates, listCertificationExamSessions, updateCertificationExamSession, } from '../../../api/certificationPreparation';
import { PermissionGate } from '../../../directives/permission';
const statusLabels = {
    DRAFT: '草稿',
    CONFIGURED: '已配置',
    IN_PROGRESS: '进行中',
    ENDED: '已结束',
};
const participationStatusLabels = {
    UNASSIGNED: '未分配',
    ASSIGNED: '参加',
    CANCELLED: '取消',
    EXEMPT: '免考',
};
export function ExamSessionConfig({ taskId, onChanged }) {
    const queryClient = useQueryClient();
    const [editing, setEditing] = useState();
    const [candidateSession, setCandidateSession] = useState();
    const [selectedCandidateIds, setSelectedCandidateIds] = useState([]);
    const [open, setOpen] = useState(false);
    const [form] = Form.useForm();
    const sessionsQuery = useQuery({
        queryKey: ['certification-exam-sessions', taskId],
        queryFn: () => listCertificationExamSessions(taskId),
        enabled: Boolean(taskId),
    });
    const invalidate = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-exam-sessions', taskId] });
        onChanged?.();
    };
    const saveMutation = useMutation({
        mutationFn: (payload) => editing ? updateCertificationExamSession(editing.id, payload) : createCertificationExamSession(taskId, payload),
        onSuccess: () => {
            message.success(editing ? '考试场次已更新' : '考试场次已创建');
            setOpen(false);
            invalidate();
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (sessionId) => deleteCertificationExamSession(sessionId),
        onSuccess: () => {
            message.success('考试场次已删除');
            invalidate();
        },
    });
    const candidatesQuery = useQuery({
        queryKey: ['certification-exam-session-candidates', candidateSession?.id],
        queryFn: () => listCertificationExamSessionCandidates(candidateSession.id),
        enabled: Boolean(candidateSession),
    });
    const assignCandidatesMutation = useMutation({
        mutationFn: () => assignCertificationExamSessionCandidates(candidateSession.id, {
            candidateIds: selectedCandidateIds,
        }),
        onSuccess: () => {
            message.success('场次考生名单已保存');
            queryClient.invalidateQueries({ queryKey: ['certification-exam-session-candidates', candidateSession?.id] });
            setCandidateSession(undefined);
            invalidate();
        },
    });
    const completeConfigMutation = useMutation({
        mutationFn: (sessionId) => completeCertificationExamSessionCollectionConfig(sessionId),
        onSuccess: () => {
            message.success('采集配置已完成，信息采集任务已生成');
            invalidate();
        },
    });
    useEffect(() => {
        const assigned = (candidatesQuery.data || [])
            .filter((candidate) => candidate.participationStatus === 'ASSIGNED')
            .map((candidate) => candidate.candidateId);
        setSelectedCandidateIds(assigned);
    }, [candidatesQuery.data]);
    const openCreate = () => {
        setEditing(undefined);
        form.resetFields();
        form.setFieldsValue({
            examType: 'THEORY',
            status: 'DRAFT',
        });
        setOpen(true);
    };
    const openEdit = (record) => {
        setEditing(record);
        form.setFieldsValue({
            examType: record.examType,
            sessionName: record.sessionName,
            examVenue: record.examVenue,
            examDate: record.examDate ? dayjs(record.examDate) : undefined,
            startTime: record.startTime ? dayjs(record.startTime) : undefined,
            endTime: record.endTime ? dayjs(record.endTime) : undefined,
            status: record.status,
            remark: record.remark,
        });
        setOpen(true);
    };
    const submit = async () => {
        const values = await form.validateFields();
        saveMutation.mutate({
            ...values,
            examDate: values.examDate?.format('YYYY-MM-DD'),
            startTime: values.startTime?.format('YYYY-MM-DDTHH:mm:ss'),
            endTime: values.endTime?.format('YYYY-MM-DDTHH:mm:ss'),
            status: values.status || 'DRAFT',
            identityCheckRequired: true,
            fingerprintRequired: true,
            signatureRequired: true,
        });
    };
    const columns = [
        { title: '场次名称', dataIndex: 'sessionName', width: 220 },
        { title: '考场场地', dataIndex: 'examVenue', width: 180, render: (value) => value || '-' },
        {
            title: '考试类型',
            dataIndex: 'examType',
            width: 100,
            render: (value) => value === 'THEORY' ? _jsx(Tag, { color: "blue", children: "\u7406\u8BBA" }) : _jsx(Tag, { color: "orange", children: "\u5B9E\u64CD" }),
        },
        { title: '考试日期', dataIndex: 'examDate', width: 120, render: (value) => value || '-' },
        { title: '开始时间', dataIndex: 'startTime', width: 170, render: (value) => value || '-' },
        { title: '结束时间', dataIndex: 'endTime', width: 170, render: (value) => value || '-' },
        {
            title: '业务环节',
            width: 220,
            render: (_, record) => record.examType === 'PRACTICE' ? '信息采集 + 实操评分' : '信息采集',
        },
        { title: '状态', dataIndex: 'status', width: 100, render: (value) => statusLabels[value] || value },
        {
            title: '操作',
            width: 156,
            align: 'center',
            render: (_, record) => (_jsxs(Space, { size: 2, children: [_jsx(PermissionGate, { code: "certification.preparation.candidate.exam-session.edit", children: _jsx(Button, { type: "link", size: "small", title: "\u8BBE\u7F6E\u8003\u751F", "aria-label": "\u8BBE\u7F6E\u8003\u751F", icon: _jsx(UserAddOutlined, {}), onClick: () => {
                                setSelectedCandidateIds([]);
                                setCandidateSession(record);
                            }, style: { paddingInline: 5 } }) }), _jsx(PermissionGate, { code: "certification.preparation.exam-session.edit", children: _jsx(Popconfirm, { title: "\u5B8C\u6210\u91C7\u96C6\u914D\u7F6E\u540E\uFF0C\u8BE5\u573A\u6B21\u5C06\u51FA\u73B0\u5728\u4FE1\u606F\u91C7\u96C6\u4EFB\u52A1\u5217\u8868\u4E2D\uFF0C\u786E\u8BA4\u7EE7\u7EED\uFF1F", onConfirm: () => completeConfigMutation.mutate(record.id), disabled: record.status === 'CONFIGURED', children: _jsx(Button, { type: "link", size: "small", title: record.status === 'CONFIGURED' ? '已完成配置' : '完成采集配置', "aria-label": record.status === 'CONFIGURED' ? '已完成配置' : '完成采集配置', icon: _jsx(CheckCircleOutlined, {}), disabled: record.status === 'CONFIGURED', loading: completeConfigMutation.isPending && completeConfigMutation.variables === record.id, style: { paddingInline: 5 } }) }) }), _jsx(PermissionGate, { code: "certification.preparation.exam-session.edit", children: _jsx(Button, { type: "link", size: "small", title: "\u7F16\u8F91", "aria-label": "\u7F16\u8F91", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), style: { paddingInline: 5 } }) }), _jsx(PermissionGate, { code: "certification.preparation.exam-session.delete", children: _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664\u8BE5\u8003\u8BD5\u573A\u6B21\uFF1F", onConfirm: () => deleteMutation.mutate(record.id), children: _jsx(Button, { type: "link", danger: true, size: "small", title: "\u5220\u9664", "aria-label": "\u5220\u9664", icon: _jsx(DeleteOutlined, {}), style: { paddingInline: 5 } }) }) })] })),
        },
    ];
    return (_jsxs(Card, { title: "\u8003\u8BD5\u573A\u6B21", extra: (_jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => sessionsQuery.refetch(), children: "\u5237\u65B0" }), _jsx(PermissionGate, { code: "certification.preparation.exam-session.edit", children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, disabled: !taskId, children: "\u65B0\u589E\u573A\u6B21" }) })] })), children: [_jsx(Table, { rowKey: "id", loading: sessionsQuery.isLoading, dataSource: sessionsQuery.data || [], columns: columns, pagination: false, scroll: { x: 1280 }, locale: { emptyText: taskId ? '暂无考试场次，请先新增理论或实操场次' : '请先选择认定任务' } }), _jsx(Modal, { title: editing ? '编辑考试场次' : '新增考试场次', open: open, onOk: submit, onCancel: () => setOpen(false), confirmLoading: saveMutation.isPending, destroyOnClose: true, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { name: "examType", label: "\u8003\u8BD5\u7C7B\u578B", rules: [{ required: true }], children: _jsx(Select, { options: [{ label: '理论', value: 'THEORY' }, { label: '实操', value: 'PRACTICE' }] }) }), _jsx(Form.Item, { name: "sessionName", label: "\u573A\u6B21\u540D\u79F0", rules: [{ required: true, message: '请输入场次名称' }], children: _jsx(Input, { placeholder: "\u5982\uFF1A\u7406\u8BBA\u8003\u8BD5\u7B2C\u4E00\u573A" }) }), _jsx(Form.Item, { name: "examVenue", label: "\u8003\u573A\u573A\u5730", rules: [{ max: 200, message: '考场场地不能超过200个字符' }], children: _jsx(Input, { placeholder: "\u5982\uFF1A\u767E\u8272\u94F6\u6D77\u94DD\u4F1A\u8BAE\u5BA4\u3001\u7406\u8BBA\u4E00\u53F7\u8003\u573A" }) }), _jsx(Form.Item, { name: "examDate", label: "\u8003\u8BD5\u65E5\u671F", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "startTime", label: "\u5F00\u59CB\u65F6\u95F4", children: _jsx(DatePicker, { showTime: { format: 'HH:mm:ss' }, format: "YYYY-MM-DD HH:mm:ss", style: { width: '100%' } }) }), _jsx(Form.Item, { name: "endTime", label: "\u7ED3\u675F\u65F6\u95F4", dependencies: ['startTime'], rules: [{
                                    validator: async (_, value) => {
                                        const startTime = form.getFieldValue('startTime');
                                        if (!value || !startTime || value.isAfter(startTime)) {
                                            return;
                                        }
                                        throw new Error('结束时间必须晚于开始时间');
                                    },
                                }], children: _jsx(DatePicker, { showTime: { format: 'HH:mm:ss' }, format: "YYYY-MM-DD HH:mm:ss", style: { width: '100%' }, disabledDate: (current) => {
                                    const startTime = form.getFieldValue('startTime');
                                    return Boolean(startTime && current && current.isBefore(startTime, 'day'));
                                } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: Object.entries(statusLabels).map(([value, label]) => ({ value, label })) }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 3 }) })] }) }), _jsxs(Modal, { title: candidateSession ? `设置考生名单：${candidateSession.sessionName}` : '设置考生名单', open: Boolean(candidateSession), onOk: () => assignCandidatesMutation.mutate(), onCancel: () => setCandidateSession(undefined), confirmLoading: assignCandidatesMutation.isPending, width: 780, destroyOnClose: true, children: [_jsx(Table, { rowKey: "candidateId", size: "small", loading: candidatesQuery.isLoading, dataSource: candidatesQuery.data || [], rowSelection: {
                            selectedRowKeys: selectedCandidateIds,
                            onChange: (keys) => setSelectedCandidateIds(keys.map(String)),
                        }, pagination: false, scroll: { y: 420 }, columns: [
                            { title: '姓名', dataIndex: 'candidateName', width: 140 },
                            { title: '身份证号', dataIndex: 'idCardNo', width: 220 },
                            { title: '准考证号', dataIndex: 'admissionTicketNo', width: 150, render: (value) => value || '-' },
                            {
                                title: '参加状态',
                                dataIndex: 'participationStatus',
                                render: (value) => participationStatusLabels[value] || value || '-',
                            },
                        ], locale: { emptyText: '当前场次暂无可配置考生，请先导入考生名单' } }), _jsxs(Typography.Text, { type: "secondary", children: ["\u5DF2\u9009\u62E9 ", selectedCandidateIds.length, " \u540D\u8003\u751F"] })] })] }));
}
