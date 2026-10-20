import { jsxs as _jsxs, jsx as _jsx, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 二期：排课分班（L2）— 解析企业排班表 → 预检/生成 → 绑定培训班
 */
import { useMemo, useState } from 'react';
import { Alert, Button, Collapse, Form, InputNumber, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import { CalendarOutlined, LinkOutlined } from '@ant-design/icons';
import * as trainApi from '../../../api/training';
const { Text } = Typography;
export function ShiftAssignPanel({ planId, trainInfoId, shiftFileName }) {
    const now = new Date();
    const [year, setYear] = useState(now.getFullYear());
    const [month, setMonth] = useState(now.getMonth() + 1);
    const [classMin, setClassMin] = useState(30);
    const [classMax, setClassMax] = useState(50);
    const [task, setTask] = useState(null);
    const [bindAlgo, setBindAlgo] = useState();
    const [bindClassId, setBindClassId] = useState();
    const payload = useMemo(() => ({
        planId,
        trainInfoId: trainInfoId ?? undefined,
        year,
        month,
        adapterType: 'T0',
        classMin,
        classMax,
    }), [planId, trainInfoId, year, month, classMin, classMax]);
    const classesQuery = useQuery({
        queryKey: ['shift-assign-info-classes', trainInfoId],
        queryFn: async () => {
            const pageSize = 200;
            const maxPages = 50;
            const items = [];
            let page = 1;
            let total = Number.POSITIVE_INFINITY;
            while (items.length < total && page <= maxPages) {
                const res = await trainApi.pageInfoClasses({
                    trainInfoId: trainInfoId,
                    page,
                    pageSize,
                });
                const batch = res.items ?? [];
                items.push(...batch);
                total = typeof res.total === 'number' ? res.total : items.length;
                if (batch.length === 0)
                    break;
                page += 1;
            }
            return { items, total: items.length };
        },
        enabled: !!trainInfoId,
    });
    const previewMu = useMutation({
        mutationFn: () => trainApi.previewShiftAssignAdapter(payload),
        onSuccess: (res) => {
            message.success(`预览成功：${res.personCount ?? 0} 人 / ${res.slotCount ?? 0} 人日槽`);
            if (res.warnings?.length) {
                message.warning(`有 ${res.warnings.length} 条解析警告`);
            }
        },
        onError: (e) => message.error(e.message || '预览失败'),
    });
    const precheckMu = useMutation({
        mutationFn: () => trainApi.precheckShiftAssign(payload),
        onSuccess: (res) => {
            message.success(`预检完成：约 ${res.recordCount ?? 0} 条排课明细，算法班 ${res.algoClassNames?.length ?? 0} 个`);
        },
        onError: (e) => message.error(e.message || '预检失败'),
    });
    const generateMu = useMutation({
        mutationFn: () => trainApi.generateShiftAssign(payload),
        onSuccess: (res) => {
            setTask(res);
            message.success(`已生成排课任务 #${res.taskId}，明细 ${res.recordCount ?? 0} 条`);
        },
        onError: (e) => message.error(e.message || '生成失败'),
    });
    const bindMu = useMutation({
        mutationFn: () => trainApi.bindShiftAssignClass(task.taskId, {
            algoClassName: bindAlgo,
            trainClassId: bindClassId,
        }),
        onSuccess: (res) => {
            setTask(res);
            message.success(`已绑定：${res.boundAlgoClassName} → 班级 #${res.boundTrainClassId}`);
        },
        onError: (e) => message.error(e.message || '绑定失败'),
    });
    const recordColumns = [
        { title: '姓名', dataIndex: 'personName', width: 90 },
        { title: '日期', dataIndex: 'assignDate', width: 110 },
        { title: '时段', dataIndex: 'slot', width: 70 },
        { title: '算法班', dataIndex: 'algoClassName', width: 110 },
        { title: '部门', dataIndex: 'dept', width: 100, ellipsis: true },
        { title: '班别', dataIndex: 'shiftClass', width: 80 },
        {
            title: '培训班',
            dataIndex: 'trainClassId',
            width: 90,
            render: (v) => (v ? _jsxs(Tag, { color: "blue", children: ["#", v] }) : _jsx(Text, { type: "secondary", children: "\u672A\u7ED1" })),
        },
    ];
    const bindColumns = [
        { title: '算法班', dataIndex: 'algoClassName' },
        { title: '明细数', dataIndex: 'recordCount', width: 90 },
        {
            title: '培训班ID',
            dataIndex: 'trainClassId',
            width: 110,
            render: (v) => (v != null ? v : '-'),
        },
    ];
    const classOptions = (classesQuery.data?.items ?? []).map((c) => ({
        value: c.id,
        label: `${c.title || '未命名'} (#${c.id})`,
    }));
    return (_jsx(Collapse, { style: { marginTop: 16 }, items: [
            {
                key: 'shift-assign',
                label: (_jsxs(Space, { children: [_jsx(CalendarOutlined, {}), _jsx("span", { children: "\u6392\u8BFE\u5206\u73ED\uFF08\u6309\u4F01\u4E1A\u6392\u73ED\u8868 \u00B7 L2\uFF09" }), task?.taskId ? _jsxs(Tag, { color: "success", children: ["\u4EFB\u52A1 #", task.taskId] }) : _jsx(Tag, { children: "\u4E8C\u671F" })] })),
                children: (_jsxs("div", { children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u4F7F\u7528\u4F01\u4E1A\u56DE\u4F20\u7684\u6392\u73ED\u8868\uFF08\u6807\u51C6\u300C\u59D3\u540D + \u65E5\u671F\u5217\u300D\uFF09\u751F\u6210\u4E0A\u5348/\u4E0B\u5348\u7B97\u6CD5\u73ED\uFF0C\u518D\u7ED1\u5B9A\u5230\u5DF2\u6709\u57F9\u8BAD\u73ED\u7EA7\u3002\u4E0D\u66FF\u4EE3\u4E0A\u65B9\u7EC4\u7EC7\u5206\u73ED\u3002", description: shiftFileName ? `当前排班文件：${shiftFileName}` : '请确认规划已上传排班表' }), _jsxs(Form, { layout: "inline", style: { marginBottom: 16 }, children: [_jsx(Form.Item, { label: "\u5E74", children: _jsx(InputNumber, { min: 2020, max: 2100, value: year, onChange: (v) => setYear(v || now.getFullYear()) }) }), _jsx(Form.Item, { label: "\u6708", children: _jsx(InputNumber, { min: 1, max: 12, value: month, onChange: (v) => setMonth(v || 1) }) }), _jsx(Form.Item, { label: "\u73ED\u989D", children: _jsxs(Space, { children: [_jsx(InputNumber, { min: 5, max: 80, value: classMin, onChange: (v) => setClassMin(v || 30) }), _jsx("span", { children: "~" }), _jsx(InputNumber, { min: 5, max: 100, value: classMax, onChange: (v) => setClassMax(v || 50) })] }) }), _jsx(Form.Item, { children: _jsxs(Space, { wrap: true, children: [_jsx(Button, { loading: previewMu.isPending, onClick: () => previewMu.mutate(), children: "Adapter \u9884\u89C8" }), _jsx(Button, { loading: precheckMu.isPending, onClick: () => precheckMu.mutate(), children: "\u9884\u68C0" }), _jsx(Button, { type: "primary", loading: generateMu.isPending, disabled: !trainInfoId, onClick: () => generateMu.mutate(), children: "\u751F\u6210\u6392\u8BFE\u5206\u73ED" })] }) })] }), !trainInfoId && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u8BF7\u5148\u5B8C\u6210\u300C\u57F9\u8BAD\u4FE1\u606F\u300D\u6B65\u9AA4\uFF0C\u518D\u751F\u6210\u6392\u8BFE\u5206\u73ED\u4EE5\u4FBF\u7ED1\u5B9A\u73ED\u7EA7\u3002" })), (precheckMu.data || previewMu.data) && !task && (_jsx(Alert, { type: "success", showIcon: true, style: { marginBottom: 16 }, message: precheckMu.data
                                ? `预检：算法班 ${precheckMu.data.algoClassNames?.join('、') || '无'}；未排入 ${precheckMu.data.unassignedPersons?.length ?? 0} 人`
                                : `预览：${previewMu.data?.personCount ?? 0} 人` })), task && (_jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginBottom: 12 }, wrap: true, children: [_jsxs(Tag, { color: "blue", children: ["\u7248\u672C v", task.version] }), _jsxs(Tag, { children: ["\u660E\u7EC6 ", task.recordCount ?? 0] }), _jsxs(Tag, { children: ["\u7B97\u6CD5\u73ED ", (task.algoClassNames ?? []).length] }), _jsxs(Tag, { color: "orange", children: ["\u672A\u6392\u5165 ", task.unassignedPersons?.length ?? 0] })] }), _jsxs(Form, { layout: "inline", style: { marginBottom: 16 }, children: [_jsx(Form.Item, { label: "\u7B97\u6CD5\u73ED", children: _jsx(Select, { style: { width: 160 }, placeholder: "\u9009\u62E9\u7B97\u6CD5\u73ED", options: (task.algoClassNames ?? []).map((n) => ({ value: n, label: n })), value: bindAlgo, onChange: setBindAlgo, allowClear: true }) }), _jsx(Form.Item, { label: "\u57F9\u8BAD\u73ED\u7EA7", children: _jsx(Select, { style: { width: 240 }, placeholder: trainInfoId ? '选择培训班' : '先生成培训信息', options: classOptions, value: bindClassId, onChange: setBindClassId, allowClear: true, disabled: !trainInfoId }) }), _jsx(Form.Item, { children: _jsx(Button, { icon: _jsx(LinkOutlined, {}), type: "primary", ghost: true, loading: bindMu.isPending, disabled: !bindAlgo || !bindClassId, onClick: () => bindMu.mutate(), children: "\u7ED1\u5B9A\u73ED\u7EA7" }) })] }), _jsx(Table, { size: "small", rowKey: "algoClassName", columns: bindColumns, dataSource: task.bindings ?? [], pagination: false, style: { marginBottom: 16 } }), _jsx(Table, { size: "small", rowKey: "id", columns: recordColumns, dataSource: task.records ?? [], pagination: { pageSize: 20, showSizeChanger: false }, scroll: { x: 800 } })] }))] })),
            },
        ] }));
}
