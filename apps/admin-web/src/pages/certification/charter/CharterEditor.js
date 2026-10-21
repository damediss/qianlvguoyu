import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EyeOutlined, SaveOutlined, StopOutlined } from '@ant-design/icons';
import { Button, Checkbox, Form, Input, Modal, Select, Space, Typography } from 'antd';
import { useEffect, useMemo } from 'react';
const statusOptions = [
    { label: '进行中', value: 'running' },
    { label: '待审核', value: 'pending_review' },
    { label: '已完成', value: 'completed' },
];
const containerStyle = {
    maxWidth: 900,
    margin: '0 auto',
    borderRadius: 12,
    overflow: 'hidden',
    background: '#fff',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    border: '1px solid #eef2f7',
};
const headerStyle = {
    padding: 32,
    color: '#fff',
    background: 'linear-gradient(135deg, #fa8c16 0%, #d46b08 100%)',
};
const bodyStyle = {
    padding: 32,
};
const sectionHeaderStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 20,
    paddingBottom: 12,
    borderBottom: '2px solid #e5e7eb',
};
const sectionNumberStyle = {
    width: 32,
    height: 32,
    borderRadius: 999,
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#fff',
    background: '#1890ff',
    fontWeight: 700,
    fontSize: 14,
};
const toolbarStyle = {
    padding: '20px 32px',
    background: '#f8fafc',
    borderTop: '1px solid #e5e7eb',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    flexWrap: 'wrap',
};
const itemCardStyle = {
    padding: 16,
    background: '#f8fafc',
    borderRadius: 8,
    border: '1px solid #e5e7eb',
};
function parseDraft(document) {
    if (!document) {
        return undefined;
    }
    const raw = document.contentJson || document.contentText;
    if (!raw) {
        return undefined;
    }
    try {
        return JSON.parse(raw);
    }
    catch {
        return undefined;
    }
}
function buildDefaultDraft(task) {
    return {
        taskName: task?.taskName || '',
        taskNo: task?.taskNo || '',
        taskManagerName: task?.taskManagerName || '',
        taskStatus: task?.status || 'running',
        background: '',
        goals: {
            quality: '',
            schedule: '',
            cost: '',
            delivery: '',
        },
        milestones: [
            { enabled: true, title: '完成任务基本信息、背景与总体目标确认', desc: '确认任务章程、任务背景和总体目标，确保任务方向正确。' },
            { enabled: false, title: '完成成员安排、WBS 分解和资源估算', desc: '' },
            { enabled: false, title: '完成进度计划与考试前准备事项确认', desc: '' },
            { enabled: false, title: '完成风险识别与资料导出，进入审核流转', desc: '' },
        ],
    };
}
function previewText(values) {
    const goals = values.goals || {};
    const milestones = values.milestones || [];
    const milestoneText = milestones.length === 0
        ? '无'
        : milestones.map((item, index) => `${index + 1}. ${item.enabled ? '启用' : '未启用'}｜${item.title || '-'}${item.desc ? `\n   ${item.desc}` : ''}`).join('\n');
    return [
        '任务策划任务书',
        '',
        `任务名称：${values.taskName || '-'}`,
        `任务编号：${values.taskNo || '-'}`,
        `任务经理：${values.taskManagerName || '-'}`,
        `任务状态：${values.taskStatus || '-'}`,
        '',
        '一、任务背景与目的',
        values.background || '-',
        '',
        '二、总体目标',
        `质量目标：${goals.quality || '-'}`,
        `工期目标：${goals.schedule || '-'}`,
        `费用目标：${goals.cost || '-'}`,
        `交付目标：${goals.delivery || '-'}`,
        '',
        '三、节点目标',
        milestoneText,
    ].join('\n');
}
export function CharterEditor({ task, document, saving, onSave, }) {
    const [form] = Form.useForm();
    const initial = useMemo(() => {
        const base = buildDefaultDraft(task);
        const existing = parseDraft(document);
        return {
            ...base,
            ...existing,
            goals: { ...base.goals, ...(existing?.goals || {}) },
            milestones: existing?.milestones?.length ? existing.milestones : base.milestones,
        };
    }, [document, task]);
    useEffect(() => {
        form.setFieldsValue(initial);
    }, [form, initial]);
    return (_jsxs("div", { style: containerStyle, children: [_jsxs("div", { style: headerStyle, children: [_jsx(Typography.Title, { level: 2, style: { margin: 0, color: '#fff' }, children: "\u7F16\u8F91\u4EFB\u52A1\u7B56\u5212\u4EFB\u52A1\u4E66" }), _jsx(Typography.Paragraph, { style: { margin: '8px 0 0', color: 'rgba(255,255,255,0.9)' }, children: "\u8BF7\u586B\u5199\u6216\u4FEE\u6539\u4EE5\u4E0B\u8868\u5355\u5185\u5BB9\uFF0C\u5B8C\u6210\u540E\u70B9\u51FB\u4FDD\u5B58" })] }), _jsx("div", { style: bodyStyle, children: _jsxs(Form, { form: form, layout: "vertical", requiredMark: false, children: [_jsxs("div", { style: { marginBottom: 32 }, children: [_jsxs("div", { style: sectionHeaderStyle, children: [_jsx("span", { style: sectionNumberStyle, children: "1" }), _jsx(Typography.Title, { level: 3, style: { margin: 0 }, children: "\u57FA\u672C\u4FE1\u606F" })] }), _jsxs("div", { style: { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 20 }, children: [_jsx(Form.Item, { name: "taskName", label: _jsxs("span", { children: ["\u4EFB\u52A1\u540D\u79F0", _jsx("span", { style: { color: '#ff4d4f', marginLeft: 4 }, children: "*" })] }), rules: [{ required: true, message: '请输入任务名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u540D\u79F0" }) }), _jsx(Form.Item, { name: "taskNo", label: _jsxs("span", { children: ["\u4EFB\u52A1\u7F16\u53F7", _jsx("span", { style: { color: '#ff4d4f', marginLeft: 4 }, children: "*" })] }), rules: [{ required: true, message: '请输入任务编号' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u7F16\u53F7" }) }), _jsx(Form.Item, { name: "taskManagerName", label: "\u4EFB\u52A1\u7ECF\u7406\uFF08\u9009\u586B\uFF09", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u7ECF\u7406\u59D3\u540D\uFF08\u9009\u586B\uFF09" }) }), _jsx(Form.Item, { name: "taskStatus", label: "\u4EFB\u52A1\u72B6\u6001", children: _jsx(Select, { options: statusOptions, placeholder: "\u8BF7\u9009\u62E9\u4EFB\u52A1\u72B6\u6001" }) })] })] }), _jsxs("div", { style: { marginBottom: 32 }, children: [_jsxs("div", { style: sectionHeaderStyle, children: [_jsx("span", { style: sectionNumberStyle, children: "2" }), _jsx(Typography.Title, { level: 3, style: { margin: 0 }, children: "\u4EFB\u52A1\u80CC\u666F\u4E0E\u76EE\u7684" })] }), _jsx(Form.Item, { name: "background", label: _jsxs("span", { children: ["\u80CC\u666F\u63CF\u8FF0", _jsx("span", { style: { color: '#ff4d4f', marginLeft: 4 }, children: "*" })] }), rules: [{ required: true, message: '请填写背景描述' }], extra: _jsx("span", { style: { color: '#6b7280', fontSize: 12 }, children: "\u8BF7\u8BE6\u7EC6\u8BF4\u660E\u4EFB\u52A1\u7684\u80CC\u666F\u3001\u76EE\u7684\u548C\u610F\u4E49" }), children: _jsx(Input.TextArea, { placeholder: "\u8BF7\u63CF\u8FF0\u4EFB\u52A1\u80CC\u666F\u548C\u76EE\u7684", autoSize: { minRows: 5, maxRows: 10 } }) })] }), _jsxs("div", { style: { marginBottom: 32 }, children: [_jsxs("div", { style: sectionHeaderStyle, children: [_jsx("span", { style: sectionNumberStyle, children: "3" }), _jsx(Typography.Title, { level: 3, style: { margin: 0 }, children: "\u4EFB\u52A1\u603B\u4F53\u76EE\u6807" })] }), _jsxs("div", { style: { display: 'flex', flexDirection: 'column', gap: 16 }, children: [_jsx("div", { style: itemCardStyle, children: _jsx(Form.Item, { name: ['goals', 'quality'], label: "\u8D28\u91CF\u76EE\u6807", style: { marginBottom: 0 }, children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8D28\u91CF\u76EE\u6807" }) }) }), _jsx("div", { style: itemCardStyle, children: _jsx(Form.Item, { name: ['goals', 'schedule'], label: "\u5DE5\u671F\u76EE\u6807", style: { marginBottom: 0 }, children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u5DE5\u671F\u76EE\u6807" }) }) }), _jsx("div", { style: itemCardStyle, children: _jsx(Form.Item, { name: ['goals', 'cost'], label: "\u8D39\u7528\u76EE\u6807", style: { marginBottom: 0 }, children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8D39\u7528\u76EE\u6807" }) }) }), _jsx("div", { style: itemCardStyle, children: _jsx(Form.Item, { name: ['goals', 'delivery'], label: "\u4EA4\u4ED8\u76EE\u6807", style: { marginBottom: 0 }, children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u4EA4\u4ED8\u76EE\u6807" }) }) })] })] }), _jsxs("div", { children: [_jsxs("div", { style: sectionHeaderStyle, children: [_jsx("span", { style: sectionNumberStyle, children: "4" }), _jsx(Typography.Title, { level: 3, style: { margin: 0 }, children: "\u8282\u70B9\u76EE\u6807" })] }), _jsx(Form.List, { name: "milestones", children: (fields) => (_jsx("div", { style: { display: 'flex', flexDirection: 'column', gap: 12 }, children: fields.map((field, index) => (_jsx(Form.Item, { shouldUpdate: (prev, next) => prev?.milestones?.[index]?.enabled !== next?.milestones?.[index]?.enabled, noStyle: true, children: ({ getFieldValue }) => {
                                                const enabled = Boolean(getFieldValue(['milestones', index, 'enabled']));
                                                return (_jsxs("div", { style: {
                                                        ...itemCardStyle,
                                                        display: 'flex',
                                                        gap: 12,
                                                        alignItems: 'flex-start',
                                                        borderColor: enabled ? '#52c41a' : '#e5e7eb',
                                                        background: enabled ? '#f6ffed' : '#f8fafc',
                                                        transition: 'all 0.25s',
                                                    }, children: [_jsx("div", { style: {
                                                                width: 24,
                                                                height: 24,
                                                                borderRadius: 999,
                                                                background: '#1890ff',
                                                                color: '#fff',
                                                                display: 'inline-flex',
                                                                alignItems: 'center',
                                                                justifyContent: 'center',
                                                                fontSize: 12,
                                                                fontWeight: 700,
                                                                flexShrink: 0,
                                                                marginTop: 3,
                                                            }, children: index + 1 }), _jsx(Form.Item, { name: [field.name, 'enabled'], valuePropName: "checked", style: { margin: '3px 0 0' }, children: _jsx(Checkbox, {}) }), _jsxs("div", { style: { flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }, children: [_jsx(Form.Item, { name: [field.name, 'title'], rules: [{ required: true, message: '请输入节点目标标题' }], style: { marginBottom: 0 }, children: _jsx(Input, { placeholder: "\u8282\u70B9\u76EE\u6807\u6807\u9898" }) }), _jsx(Form.Item, { name: [field.name, 'desc'], style: { marginBottom: 0 }, children: _jsx(Input.TextArea, { placeholder: "\u8282\u70B9\u76EE\u6807\u8BE6\u7EC6\u63CF\u8FF0\uFF08\u53EF\u9009\uFF09", autoSize: { minRows: 3, maxRows: 6 } }) })] })] }));
                                            } }, field.key))) })) })] })] }) }), _jsxs("div", { style: toolbarStyle, children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), loading: saving, onClick: () => {
                                    form.validateFields().then((values) => {
                                        onSave({
                                            ...values,
                                            goals: values.goals || { quality: '', schedule: '', cost: '', delivery: '' },
                                            milestones: (values.milestones || []).map((item) => ({
                                                enabled: Boolean(item.enabled),
                                                title: item.title || '',
                                                desc: item.desc || '',
                                            })),
                                        });
                                    });
                                }, children: "\u4FDD\u5B58\u4FEE\u6539" }), _jsx(Button, { icon: _jsx(EyeOutlined, {}), onClick: () => {
                                    const values = form.getFieldsValue(true);
                                    Modal.info({
                                        title: '任务策划任务书预览',
                                        width: 860,
                                        content: (_jsx("pre", { style: { margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }, children: previewText(values) })),
                                    });
                                }, children: "\u9884\u89C8" }), _jsx(Button, { icon: _jsx(StopOutlined, {}), onClick: () => {
                                    Modal.confirm({
                                        title: '确定要取消编辑吗？',
                                        content: '未保存的修改将丢失。',
                                        okText: '确定取消',
                                        cancelText: '继续编辑',
                                        onOk: () => form.setFieldsValue(initial),
                                    });
                                }, children: "\u53D6\u6D88" })] }), _jsx(Button, { type: "link", onClick: () => {
                            Modal.confirm({
                                title: '返回查看页',
                                content: '当前页面为“编辑”，查看页尚未接入路由。你可以先保存后再切换到其他资料页。',
                                okText: '知道了',
                                cancelButtonProps: { style: { display: 'none' } },
                            });
                        }, children: "\u2190 \u8FD4\u56DE\u67E5\u770B\u9875" })] })] }));
}
