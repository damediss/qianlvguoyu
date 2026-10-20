import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 跟班 · 日志·督导
 * 过程动态（自动）/ 工作日志（手写）/ 督导检查与整改（L5）
 */
import { useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Form, Input, Modal, Segmented, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, ThunderboltOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../../api/training';
import { FollowSupervisionPanel } from './FollowSupervisionPanel';
const CATEGORY_OPTIONS = [
    { value: 'attendance', label: '考勤' },
    { value: 'evidence', label: '影像' },
    { value: 'schedule', label: '课表' },
    { value: 'work_log', label: '工作日志' },
    { value: 'task', label: '任务' },
    { value: 'role', label: '角色' },
    { value: 'supervision', label: '督导' },
];
function severityTag(level) {
    if (level === 'critical')
        return _jsx(Tag, { color: "error", children: "\u4E25\u91CD" });
    if (level === 'warn')
        return _jsx(Tag, { color: "warning", children: "\u8B66\u793A" });
    return _jsx(Tag, { children: "\u4FE1\u606F" });
}
function statusTag(status) {
    if (status === 'draft')
        return _jsx(Tag, { children: "\u8349\u7A3F" });
    return _jsx(Tag, { color: "success", children: "\u5DF2\u53D1\u5E03" });
}
export function FollowLogsSupervisionTab({ classId, defaultBizDate }) {
    const qc = useQueryClient();
    const [section, setSection] = useState('ops');
    const [bizDate, setBizDate] = useState(defaultBizDate ?? dayjs());
    const [category, setCategory] = useState();
    const [warnOnly, setWarnOnly] = useState(false);
    const [logModalOpen, setLogModalOpen] = useState(false);
    const [editing, setEditing] = useState(null);
    const [logForm] = Form.useForm();
    const dayStr = bizDate ? bizDate.format('YYYY-MM-DD') : undefined;
    const opQuery = useQuery({
        queryKey: ['follow-op-logs', classId, dayStr, category, warnOnly],
        queryFn: () => trainApi.listFollowOpLogs(classId, {
            bizDate: dayStr,
            category,
            minSeverity: warnOnly ? 'warn' : undefined,
        }),
        enabled: section === 'ops',
    });
    const workQuery = useQuery({
        queryKey: ['follow-work-logs', classId, dayStr],
        queryFn: () => trainApi.listFollowWorkLogs(classId, { bizDate: dayStr }),
        enabled: section === 'work',
    });
    const invalidate = () => {
        void qc.invalidateQueries({ queryKey: ['follow-op-logs', classId] });
        void qc.invalidateQueries({ queryKey: ['follow-work-logs', classId] });
    };
    const createMut = useMutation({
        mutationFn: (payload) => trainApi.createFollowWorkLog(classId, payload),
        onSuccess: () => {
            message.success('日志已保存');
            setLogModalOpen(false);
            logForm.resetFields();
            invalidate();
        },
        onError: (e) => message.error(e.message || '保存失败'),
    });
    const updateMut = useMutation({
        mutationFn: ({ id, payload }) => trainApi.updateFollowWorkLog(id, payload),
        onSuccess: () => {
            message.success('已更新');
            setLogModalOpen(false);
            setEditing(null);
            logForm.resetFields();
            invalidate();
        },
        onError: (e) => message.error(e.message || '更新失败'),
    });
    const deleteMut = useMutation({
        mutationFn: (id) => trainApi.deleteFollowWorkLog(id),
        onSuccess: () => {
            message.success('已删除');
            invalidate();
        },
        onError: (e) => message.error(e.message || '删除失败'),
    });
    const draftMut = useMutation({
        mutationFn: () => {
            if (!dayStr)
                throw new Error('请先选择业务日');
            return trainApi.draftFollowWorkLogFromOps(classId, dayStr);
        },
        onSuccess: (row) => {
            message.success('已根据当日动态生成草稿');
            setSection('work');
            setEditing(row);
            logForm.setFieldsValue({
                date: dayjs(row.bizDate),
                author: row.authorName,
                attendanceSummary: row.attendanceSummary,
                exceptionNote: row.exceptionNote,
                content: row.content,
                status: row.status || 'draft',
            });
            setLogModalOpen(true);
            invalidate();
        },
        onError: (e) => message.error(e.message || '生成草稿失败'),
    });
    const openCreate = () => {
        setEditing(null);
        logForm.setFieldsValue({
            date: bizDate ?? dayjs(),
            author: '班主任',
            attendanceSummary: '',
            exceptionNote: '',
            content: '',
            status: 'published',
        });
        setLogModalOpen(true);
    };
    const openEdit = (row) => {
        setEditing(row);
        logForm.setFieldsValue({
            date: dayjs(row.bizDate),
            author: row.authorName,
            attendanceSummary: row.attendanceSummary,
            exceptionNote: row.exceptionNote,
            content: row.content,
            status: row.status || 'published',
        });
        setLogModalOpen(true);
    };
    const handleSave = async () => {
        const values = await logForm.validateFields();
        const payload = {
            bizDate: values.date ? dayjs(values.date).format('YYYY-MM-DD') : dayStr || dayjs().format('YYYY-MM-DD'),
            authorName: values.author,
            attendanceSummary: values.attendanceSummary,
            exceptionNote: values.exceptionNote,
            content: values.content,
            status: values.status || 'published',
        };
        if (editing) {
            updateMut.mutate({ id: editing.id, payload });
        }
        else {
            createMut.mutate(payload);
        }
    };
    const opColumns = useMemo(() => [
        {
            title: '时间',
            dataIndex: 'createdAt',
            width: 170,
            render: (v) => v || '—',
        },
        {
            title: '级别',
            dataIndex: 'severityLevel',
            width: 80,
            render: (v) => severityTag(v),
        },
        {
            title: '分类',
            dataIndex: 'eventCategory',
            width: 90,
        },
        {
            title: '操作',
            dataIndex: 'title',
            width: 160,
        },
        {
            title: '摘要',
            dataIndex: 'summary',
            ellipsis: true,
            render: (v) => v || '—',
        },
        {
            title: '操作人',
            dataIndex: 'actorName',
            width: 120,
            render: (_, row) => [row.actorName, row.actorRole].filter(Boolean).join(' · ') || '—',
        },
        {
            title: '事件码',
            dataIndex: 'eventCode',
            width: 180,
            render: (v) => _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: v }),
        },
    ], []);
    const workColumns = useMemo(() => [
        { title: '日期', dataIndex: 'bizDate', width: 120 },
        { title: '记录人', dataIndex: 'authorName', width: 120 },
        {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (v) => statusTag(v),
        },
        {
            title: '出勤概况',
            dataIndex: 'attendanceSummary',
            ellipsis: true,
            width: 180,
            render: (v) => v || '—',
        },
        {
            title: '内容',
            dataIndex: 'content',
            ellipsis: true,
            render: (v) => v || '—',
        },
        {
            title: '写入时间',
            dataIndex: 'createdAt',
            width: 170,
        },
        {
            title: '操作',
            width: 140,
            render: (_, row) => (_jsxs(Space, { children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(row), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => {
                            Modal.confirm({
                                title: '删除该工作日志？',
                                onOk: () => deleteMut.mutate(row.id),
                            });
                        }, children: "\u5220\u9664" })] })),
        },
    ], [deleteMut]);
    return (_jsxs("div", { children: [_jsxs(Space, { wrap: true, style: { marginBottom: 12 }, size: "middle", children: [_jsx(Segmented, { value: section, onChange: (v) => setSection(v), options: [
                            { label: '过程动态', value: 'ops' },
                            { label: '工作日志', value: 'work' },
                            { label: '督导检查', value: 'supervision' },
                        ] }), _jsx(DatePicker, { value: bizDate, onChange: (d) => setBizDate(d), allowClear: true, placeholder: "\u4E1A\u52A1\u65E5" }), section === 'ops' && (_jsxs(_Fragment, { children: [_jsx(Select, { style: { width: 140 }, value: category, onChange: setCategory, options: CATEGORY_OPTIONS, placeholder: "\u5206\u7C7B", allowClear: true }), _jsx(Select, { style: { width: 120 }, value: warnOnly ? 'warn' : 'all', onChange: (v) => setWarnOnly(v === 'warn'), options: [
                                    { value: 'all', label: '全部级别' },
                                    { value: 'warn', label: '仅警示+' },
                                ] }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => void opQuery.refetch(), children: "\u5237\u65B0" })] })), section === 'work' && (_jsxs(_Fragment, { children: [_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u5DE5\u4F5C\u65E5\u5FD7" }), _jsx(Button, { icon: _jsx(ThunderboltOutlined, {}), loading: draftMut.isPending, onClick: () => draftMut.mutate(), disabled: !dayStr, children: "\u6839\u636E\u5F53\u65E5\u52A8\u6001\u751F\u6210\u8349\u7A3F" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => void workQuery.refetch(), children: "\u5237\u65B0" })] }))] }), section === 'ops' && (_jsxs(_Fragment, { children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u8FC7\u7A0B\u52A8\u6001\u7531\u7CFB\u7EDF\u81EA\u52A8\u8BB0\u5F55\u8DDF\u73ED\u57DF\u7EA6\u5B9A\u5199\u64CD\u4F5C\uFF08\u8003\u52E4\u3001\u5F71\u50CF\u3001\u8BFE\u8868\u3001\u4F5C\u4E1A\u3001\u5DE5\u4F5C\u65E5\u5FD7\u7B49\uFF09\uFF0C\u4E0D\u542B\u9875\u9762\u6D4F\u89C8\u4E0E\u672A\u767B\u8BB0\u63A5\u53E3\u3002", description: "\u9ED8\u8BA4\u6309\u300C\u64CD\u4F5C\u53D1\u751F\u65E5\u300D\u7B5B\u9009\uFF1B\u5E03\u7F6E\u4F5C\u4E1A\u82E5\u6302\u4E86\u5176\u5B83\u8BFE\u6B21\uFF0C\u4ECD\u4F1A\u51FA\u73B0\u5728\u4ECA\u5929\u3002\u53EF\u6E05\u7A7A\u65E5\u671F\u67E5\u770B\u66F4\u591A\u3002" }), _jsx(Table, { rowKey: "id", size: "small", loading: opQuery.isLoading, dataSource: opQuery.data || [], columns: opColumns, pagination: { pageSize: 20, showSizeChanger: false }, locale: {
                            emptyText: dayStr
                                ? '当日暂无过程动态；完成跟班操作后将自动出现'
                                : '暂无过程动态；完成跟班操作后将自动出现',
                        }, rowClassName: (row) => row.severityLevel === 'warn' || row.severityLevel === 'critical' ? 'follow-op-warn-row' : '' })] })), section === 'work' && (_jsx(Table, { rowKey: "id", size: "small", loading: workQuery.isLoading, dataSource: workQuery.data || [], columns: workColumns, pagination: false, locale: { emptyText: '暂无工作日志' } })), section === 'supervision' && _jsx(FollowSupervisionPanel, { classId: classId, bizDate: bizDate }), _jsx(Modal, { title: editing ? '编辑工作日志' : '新增工作日志', open: logModalOpen, onCancel: () => {
                    setLogModalOpen(false);
                    setEditing(null);
                }, onOk: handleSave, confirmLoading: createMut.isPending || updateMut.isPending, destroyOnHidden: true, width: 640, children: _jsxs(Form, { form: logForm, layout: "vertical", children: [_jsx(Form.Item, { name: "date", label: "\u65E5\u671F", rules: [{ required: true }], children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "author", label: "\u8BB0\u5F55\u4EBA", rules: [{ required: true, message: '请输入记录人' }], children: _jsx(Input, { placeholder: "\u6559\u52A1\u4EBA\u5458" }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", initialValue: "published", children: _jsx(Select, { options: [
                                    { value: 'published', label: '已发布' },
                                    { value: 'draft', label: '草稿' },
                                ] }) }), _jsx(Form.Item, { name: "attendanceSummary", label: "\u51FA\u52E4\u6982\u51B5", children: _jsx(Input.TextArea, { rows: 2, placeholder: "\u5F53\u65E5\u51FA\u52E4\u6982\u51B5\u2026" }) }), _jsx(Form.Item, { name: "exceptionNote", label: "\u5F02\u5E38\u4E0E\u5904\u7406", children: _jsx(Input.TextArea, { rows: 2, placeholder: "\u5F02\u5E38\u5B66\u5458\u3001\u6C9F\u901A\u4E0E\u5904\u7406\u2026" }) }), _jsx(Form.Item, { name: "content", label: "\u5185\u5BB9", rules: [
                                {
                                    validator: async (_, value) => {
                                        const a = logForm.getFieldValue('attendanceSummary');
                                        const e = logForm.getFieldValue('exceptionNote');
                                        if (!value && !a && !e) {
                                            throw new Error('请填写内容、出勤概况或异常说明至少一项');
                                        }
                                    },
                                },
                            ], children: _jsx(Input.TextArea, { rows: 5, placeholder: "\u5DE5\u4F5C\u65E5\u5FD7\u6B63\u6587\u2026" }) })] }) })] }));
}
