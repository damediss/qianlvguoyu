import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 督导检查单 + 整改闭环 + 满意度录入（L5 / M4）
 */
import { useMemo, useState } from 'react';
import { Alert, Button, Card, Col, DatePicker, Drawer, Form, Input, Modal, Radio, Row, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { CheckOutlined, CloseOutlined, PlusOutlined, ReloadOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../../api/training';
const RESULT_OPTIONS = [
    { value: 'pass', label: '合格' },
    { value: 'fail', label: '不合格' },
    { value: 'na', label: '不适用' },
];
const CONCLUSION_OPTIONS = [
    { value: 'pass', label: '通过' },
    { value: 'conditional', label: '有条件通过' },
    { value: 'fail', label: '不通过' },
];
function supervisionStatusTag(status) {
    if (status === 'draft')
        return _jsx(Tag, { children: "\u8349\u7A3F" });
    if (status === 'submitted')
        return _jsx(Tag, { color: "processing", children: "\u5DF2\u63D0\u4EA4" });
    if (status === 'closed')
        return _jsx(Tag, { color: "success", children: "\u5DF2\u5173\u95ED" });
    return _jsx(Tag, { children: status || '—' });
}
function issueStatusTag(status) {
    if (status === 'open')
        return _jsx(Tag, { color: "error", children: "\u5F85\u5904\u7406" });
    if (status === 'doing')
        return _jsx(Tag, { color: "processing", children: "\u6574\u6539\u4E2D" });
    if (status === 'done')
        return _jsx(Tag, { color: "warning", children: "\u5F85\u590D\u6838" });
    if (status === 'closed')
        return _jsx(Tag, { color: "success", children: "\u5DF2\u5173\u95ED" });
    return _jsx(Tag, { children: status || '—' });
}
function severityTag(level) {
    if (level === 'critical')
        return _jsx(Tag, { color: "error", children: "\u4E25\u91CD" });
    if (level === 'warn')
        return _jsx(Tag, { color: "warning", children: "\u8B66\u793A" });
    return _jsx(Tag, { children: "\u4FE1\u606F" });
}
function nextIssueActions(status) {
    if (status === 'open')
        return [{ status: 'doing', label: '开始整改' }, { status: 'closed', label: '直接关闭' }];
    if (status === 'doing')
        return [{ status: 'done', label: '完成整改' }];
    if (status === 'done')
        return [{ status: 'closed', label: '复核关闭' }];
    return [];
}
export function FollowSupervisionPanel({ classId, bizDate }) {
    const qc = useQueryClient();
    const dayStr = bizDate ? bizDate.format('YYYY-MM-DD') : undefined;
    const [activeId, setActiveId] = useState(null);
    const [issueModalOpen, setIssueModalOpen] = useState(false);
    const [surveyModalOpen, setSurveyModalOpen] = useState(false);
    const [issueForm] = Form.useForm();
    const [surveyForm] = Form.useForm();
    const [submitForm] = Form.useForm();
    const listQuery = useQuery({
        queryKey: ['follow-supervisions', classId, dayStr],
        queryFn: () => trainApi.listFollowSupervisions(classId, { bizDate: dayStr }),
    });
    const detailQuery = useQuery({
        queryKey: ['follow-supervision', activeId],
        queryFn: () => trainApi.getFollowSupervision(activeId),
        enabled: activeId != null,
    });
    const issuesQuery = useQuery({
        queryKey: ['follow-issues', classId],
        queryFn: () => trainApi.listFollowIssues(classId),
    });
    const surveysQuery = useQuery({
        queryKey: ['follow-surveys', classId],
        queryFn: () => trainApi.listFollowSurveys(classId),
    });
    const invalidate = () => {
        void qc.invalidateQueries({ queryKey: ['follow-supervisions', classId] });
        void qc.invalidateQueries({ queryKey: ['follow-supervision'] });
        void qc.invalidateQueries({ queryKey: ['follow-issues', classId] });
        void qc.invalidateQueries({ queryKey: ['follow-surveys', classId] });
        void qc.invalidateQueries({ queryKey: ['follow-op-logs', classId] });
        void qc.invalidateQueries({ queryKey: ['follow-quality-metrics', classId] });
    };
    const createMut = useMutation({
        mutationFn: () => trainApi.createFollowSupervision(classId, {
            bizDate: dayStr || dayjs().format('YYYY-MM-DD'),
        }),
        onSuccess: (row) => {
            message.success('已创建督导检查草稿');
            setActiveId(row.id);
            invalidate();
        },
        onError: (e) => message.error(e.message || '创建失败'),
    });
    const patchItemMut = useMutation({
        mutationFn: ({ itemId, payload }) => trainApi.patchFollowSupervisionItem(itemId, payload),
        onSuccess: () => {
            void qc.invalidateQueries({ queryKey: ['follow-supervision', activeId] });
        },
        onError: (e) => message.error(e.message || '保存检查项失败'),
    });
    const submitMut = useMutation({
        mutationFn: (payload) => trainApi.submitFollowSupervision(activeId, payload),
        onSuccess: () => {
            message.success('已提交；不合格项已自动生成整改单');
            invalidate();
        },
        onError: (e) => message.error(e.message || '提交失败'),
    });
    const closeMut = useMutation({
        mutationFn: () => trainApi.closeFollowSupervision(activeId),
        onSuccess: () => {
            message.success('检查单已关闭');
            invalidate();
        },
        onError: (e) => message.error(e.message || '关闭失败'),
    });
    const createIssueMut = useMutation({
        mutationFn: (payload) => trainApi.createFollowIssue(classId, payload),
        onSuccess: () => {
            message.success('整改单已开立');
            setIssueModalOpen(false);
            issueForm.resetFields();
            invalidate();
        },
        onError: (e) => message.error(e.message || '开立失败'),
    });
    const transitionMut = useMutation({
        mutationFn: ({ id, status, resolveNote }) => trainApi.transitionFollowIssue(id, { status, resolveNote }),
        onSuccess: () => {
            message.success('状态已更新');
            invalidate();
        },
        onError: (e) => message.error(e.message || '流转失败'),
    });
    const surveyMut = useMutation({
        mutationFn: (payload) => trainApi.createFollowSurvey(classId, payload),
        onSuccess: () => {
            message.success('满意度已录入');
            setSurveyModalOpen(false);
            surveyForm.resetFields();
            invalidate();
        },
        onError: (e) => message.error(e.message || '录入失败'),
    });
    const detail = detailQuery.data;
    const isDraft = detail?.status === 'draft';
    const listColumns = useMemo(() => [
        { title: '业务日', dataIndex: 'bizDate', width: 110 },
        {
            title: '检查人',
            dataIndex: 'inspectorName',
            width: 100,
            render: (v) => v || '—',
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (v) => supervisionStatusTag(v),
        },
        {
            title: '结论',
            dataIndex: 'conclusion',
            width: 100,
            render: (v) => {
                if (v === 'pass')
                    return _jsx(Tag, { color: "success", children: "\u901A\u8FC7" });
                if (v === 'fail')
                    return _jsx(Tag, { color: "error", children: "\u4E0D\u901A\u8FC7" });
                if (v === 'conditional')
                    return _jsx(Tag, { color: "warning", children: "\u6709\u6761\u4EF6" });
                return '—';
            },
        },
        {
            title: '操作',
            width: 90,
            render: (_, row) => (_jsx(Button, { type: "link", size: "small", onClick: () => setActiveId(row.id), children: "\u6253\u5F00" })),
        },
    ], []);
    const itemColumns = [
        { title: '检查项', dataIndex: 'itemTitle', ellipsis: true },
        {
            title: '结果',
            dataIndex: 'result',
            width: 150,
            render: (v, row) => isDraft ? (_jsx(Select, { size: "small", style: { width: 120 }, value: v || undefined, placeholder: "\u9009\u62E9", options: RESULT_OPTIONS, onChange: (result) => patchItemMut.mutate({ itemId: row.id, payload: { result } }) })) : (RESULT_OPTIONS.find((o) => o.value === v)?.label || '—'),
        },
        {
            title: '备注',
            dataIndex: 'remark',
            render: (v, row) => isDraft ? (_jsx(Input, { size: "small", defaultValue: v || undefined, placeholder: "\u53EF\u9009", onBlur: (e) => {
                    const next = e.target.value.trim();
                    if (next !== (v || '')) {
                        patchItemMut.mutate({ itemId: row.id, payload: { remark: next || undefined } });
                    }
                } })) : (v || '—'),
        },
    ];
    const issueColumns = [
        {
            title: '严重度',
            dataIndex: 'severity',
            width: 80,
            render: (v) => severityTag(v),
        },
        { title: '标题', dataIndex: 'title', ellipsis: true },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (v) => issueStatusTag(v),
        },
        {
            title: '责任人',
            dataIndex: 'assigneeName',
            width: 90,
            render: (v) => v || '—',
        },
        {
            title: '操作',
            width: 200,
            render: (_, row) => (_jsx(Space, { wrap: true, size: 0, children: nextIssueActions(row.status).map((a) => (_jsx(Button, { type: "link", size: "small", onClick: () => {
                        if (a.status === 'closed' || a.status === 'done') {
                            Modal.confirm({
                                title: a.label,
                                content: (_jsx(Input.TextArea, { id: `issue-note-${row.id}`, rows: 3, placeholder: "\u5904\u7406\u8BF4\u660E\uFF08\u53EF\u9009\uFF09" })),
                                onOk: () => {
                                    const el = document.getElementById(`issue-note-${row.id}`);
                                    transitionMut.mutate({
                                        id: row.id,
                                        status: a.status,
                                        resolveNote: el?.value?.trim() || undefined,
                                    });
                                },
                            });
                        }
                        else {
                            transitionMut.mutate({ id: row.id, status: a.status });
                        }
                    }, children: a.label }, a.status))) })),
        },
    ];
    const avgSatisfaction = useMemo(() => {
        const rows = surveysQuery.data || [];
        if (!rows.length)
            return null;
        const sum = rows.reduce((acc, r) => acc + (Number(r.score) || 0), 0);
        return Math.round((sum / rows.length) * 100) / 100;
    }, [surveysQuery.data]);
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: "middle", children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u7763\u5BFC\u68C0\u67E5\uFF1A\u8349\u7A3F\u52FE\u9009 \u2192 \u63D0\u4EA4\uFF08\u4E0D\u5408\u683C\u81EA\u52A8\u5F00\u6574\u6539\uFF09\u2192 \u6574\u6539\u6D41\u8F6C \u2192 \u5173\u95ED\u68C0\u67E5\u5355", description: "\u68C0\u67E5\u9879\u9ED8\u8BA4\u542B\u8BBE\u5907\u3001\u7EAA\u5F8B\u3001\u5230\u8BFE\u3001\u5F71\u50CF\u7559\u75D5\u3001\u8003\u52E4\u3001\u4F5C\u4E1A\u7763\u4FC3\uFF1B\u63D0\u4EA4\u4F1A\u5199\u5165\u8FC7\u7A0B\u52A8\u6001\u3002" }), _jsx(Card, { size: "small", title: "\u7763\u5BFC\u68C0\u67E5\u5355", extra: _jsxs(Space, { children: [_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), loading: createMut.isPending, onClick: () => createMut.mutate(), children: "\u65B0\u5EFA\u68C0\u67E5" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => void listQuery.refetch(), children: "\u5237\u65B0" })] }), children: _jsx(Table, { rowKey: "id", size: "small", loading: listQuery.isLoading, dataSource: listQuery.data || [], columns: listColumns, pagination: { pageSize: 8, showSizeChanger: false }, locale: { emptyText: dayStr ? `${dayStr} 暂无督导检查` : '暂无督导检查' } }) }), _jsxs(Row, { gutter: [12, 12], children: [_jsx(Col, { xs: 24, lg: 16, children: _jsx(Card, { size: "small", title: "\u6574\u6539\u95ED\u73AF", extra: _jsx(Button, { size: "small", icon: _jsx(PlusOutlined, {}), onClick: () => setIssueModalOpen(true), children: "\u5F00\u7ACB\u6574\u6539" }), children: _jsx(Table, { rowKey: "id", size: "small", loading: issuesQuery.isLoading, dataSource: issuesQuery.data || [], columns: issueColumns, pagination: { pageSize: 8, showSizeChanger: false }, locale: { emptyText: '暂无整改单' } }) }) }), _jsx(Col, { xs: 24, lg: 8, children: _jsxs(Card, { size: "small", title: "\u6EE1\u610F\u5EA6", extra: _jsx(Button, { size: "small", onClick: () => setSurveyModalOpen(true), children: "\u5F55\u5165" }), children: [_jsxs(Typography.Paragraph, { style: { marginBottom: 8 }, children: ["\u6837\u672C ", surveysQuery.data?.length ?? 0, " \u4EFD \u00B7 \u5747\u5206", ' ', _jsx(Typography.Text, { strong: true, children: avgSatisfaction ?? '—' })] }), _jsx(Table, { rowKey: "id", size: "small", loading: surveysQuery.isLoading, dataSource: (surveysQuery.data || []).slice(0, 5), pagination: false, columns: [
                                        { title: '评分', dataIndex: 'score', width: 60 },
                                        {
                                            title: '对象',
                                            dataIndex: 'respondentName',
                                            ellipsis: true,
                                            render: (v) => v || '匿名',
                                        },
                                    ], locale: { emptyText: '暂无答卷' } })] }) })] }), _jsx(Drawer, { title: detail ? `督导检查 · ${detail.bizDate}` : '督导检查', open: activeId != null, onClose: () => setActiveId(null), width: 720, destroyOnHidden: true, extra: detail ? (_jsxs(Space, { children: [supervisionStatusTag(detail.status), isDraft && (_jsx(Button, { type: "primary", icon: _jsx(CheckOutlined, {}), loading: submitMut.isPending, onClick: () => {
                                submitForm.setFieldsValue({
                                    conclusion: detail.conclusion || 'pass',
                                    remark: detail.remark,
                                });
                                Modal.confirm({
                                    title: '提交督导检查',
                                    width: 480,
                                    content: (_jsxs(Form, { form: submitForm, layout: "vertical", style: { marginTop: 12 }, children: [_jsx(Form.Item, { name: "conclusion", label: "\u7ED3\u8BBA", rules: [{ required: true }], children: _jsx(Radio.Group, { options: CONCLUSION_OPTIONS }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2 }) })] })),
                                    onOk: async () => {
                                        const values = await submitForm.validateFields();
                                        await submitMut.mutateAsync(values);
                                    },
                                });
                            }, children: "\u63D0\u4EA4" })), detail.status === 'submitted' && (_jsx(Button, { icon: _jsx(CloseOutlined, {}), loading: closeMut.isPending, onClick: () => closeMut.mutate(), children: "\u5173\u95ED\u68C0\u67E5\u5355" }))] })) : null, children: detailQuery.isLoading ? (_jsx(Typography.Text, { type: "secondary", children: "\u52A0\u8F7D\u4E2D\u2026" })) : detail ? (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: "middle", children: [_jsxs(Typography.Text, { type: "secondary", children: ["\u68C0\u67E5\u4EBA ", detail.inspectorName || '—', detail.openIssueCount != null ? ` · 关联未闭环整改 ${detail.openIssueCount}` : ''] }), _jsx(Table, { rowKey: "id", size: "small", dataSource: detail.items || [], columns: itemColumns, pagination: false })] })) : (_jsx(Typography.Text, { type: "secondary", children: "\u672A\u627E\u5230\u68C0\u67E5\u5355" })) }), _jsx(Modal, { title: "\u5F00\u7ACB\u6574\u6539\u5355", open: issueModalOpen, onCancel: () => setIssueModalOpen(false), onOk: async () => {
                    const values = await issueForm.validateFields();
                    createIssueMut.mutate({
                        title: values.title,
                        description: values.description,
                        severity: values.severity,
                        assigneeName: values.assigneeName,
                        dueDate: values.dueDate ? values.dueDate.format('YYYY-MM-DD') : undefined,
                    });
                }, confirmLoading: createIssueMut.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: issueForm, layout: "vertical", initialValues: { severity: 'warn' }, children: [_jsx(Form.Item, { name: "title", label: "\u6807\u9898", rules: [{ required: true, message: '请输入标题' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "severity", label: "\u4E25\u91CD\u5EA6", children: _jsx(Select, { options: [
                                    { value: 'info', label: '信息' },
                                    { value: 'warn', label: '警示' },
                                    { value: 'critical', label: '严重' },
                                ] }) }), _jsx(Form.Item, { name: "assigneeName", label: "\u8D23\u4EFB\u4EBA", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "dueDate", label: "\u622A\u6B62\u65E5\u671F", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "description", label: "\u8BF4\u660E", children: _jsx(Input.TextArea, { rows: 3 }) })] }) }), _jsx(Modal, { title: "\u5F55\u5165\u6EE1\u610F\u5EA6", open: surveyModalOpen, onCancel: () => setSurveyModalOpen(false), onOk: async () => {
                    const values = await surveyForm.validateFields();
                    surveyMut.mutate({
                        score: values.score,
                        respondentName: values.respondentName,
                        commentText: values.commentText,
                        bizDate: dayStr || dayjs().format('YYYY-MM-DD'),
                    });
                }, confirmLoading: surveyMut.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: surveyForm, layout: "vertical", initialValues: { score: 5 }, children: [_jsx(Form.Item, { name: "score", label: "\u8BC4\u5206\uFF081\uFF5E5\uFF09", rules: [{ required: true }], children: _jsx(Select, { options: [1, 2, 3, 4, 5].map((n) => ({ value: n, label: `${n} 分` })) }) }), _jsx(Form.Item, { name: "respondentName", label: "\u8BC4\u4EF7\u4EBA", children: _jsx(Input, { placeholder: "\u53EF\u9009" }) }), _jsx(Form.Item, { name: "commentText", label: "\u610F\u89C1", children: _jsx(Input.TextArea, { rows: 3 }) })] }) })] }));
}
