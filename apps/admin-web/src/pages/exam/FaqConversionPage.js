import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useCallback, useEffect, useState, useRef } from 'react';
import { Card, Form, Select, Checkbox, Button, Table, Tag, Space, Drawer, message, Tooltip, Popconfirm, } from 'antd';
import { PlayCircleOutlined, PauseCircleOutlined, ReloadOutlined, EyeOutlined, SettingOutlined, RobotOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined, MinusCircleOutlined, LoadingOutlined, DownloadOutlined, } from '@ant-design/icons';
import { submitFaqConversion, listFaqBatches, getFaqBatchProgress, pageFaqRecords, getFaqRecordDetail, pauseFaqBatch, resumeFaqBatch, retryFaqBatch, listEnabledFaqModels, exportFaqBatch, } from '../../api/exam';
import { treeWorkTypes, pageCourses } from '../../api/exam';
import { usePaneActive, usePaneActiveRef } from '../../layouts/TabKeepAlive';
import { FaqConfigDialog } from './FaqConfigDialog';
// 题型映射
const TYPE_LABELS = { single: '单选题', multi: '多选题', judge: '判断题' };
const STATUS_CONFIG = {
    pending: { color: 'default', icon: _jsx(ClockCircleOutlined, {}), label: '待转换' },
    processing: { color: 'processing', icon: _jsx(LoadingOutlined, { spin: true }), label: '转换中' },
    success: { color: 'success', icon: _jsx(CheckCircleOutlined, {}), label: '成功' },
    failed: { color: 'error', icon: _jsx(CloseCircleOutlined, {}), label: '失败' },
    skipped: { color: 'warning', icon: _jsx(MinusCircleOutlined, {}), label: '跳过' },
};
/** 工种题库结构化问答管理主页面 */
export function FaqConversionPage() {
    const [form] = Form.useForm();
    const paneActiveRef = usePaneActiveRef(usePaneActive()); // 标签不可见时暂停 20s 进度轮询（§3.6）
    const [batches, setBatches] = useState([]);
    const [selectedBatchId, setSelectedBatchId] = useState(null);
    const [records, setRecords] = useState([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [models, setModels] = useState([]);
    const [configOpen, setConfigOpen] = useState(false);
    const [detailRecord, setDetailRecord] = useState(null);
    const [detailOpen, setDetailOpen] = useState(false);
    const [workTypeTree, setWorkTypeTree] = useState([]);
    const [courseOptions, setCourseOptions] = useState([]);
    // 轮询状态
    const [pollingBatchId, setPollingBatchId] = useState(null);
    const pollRef = useRef(null);
    // 加载工种树
    useEffect(() => {
        treeWorkTypes().then((res) => {
            const opts = [];
            const walk = (nodes) => {
                for (const n of nodes) {
                    opts.push({ label: n.title, value: n.id });
                    if (n.children)
                        walk(n.children);
                }
            };
            walk(res);
            setWorkTypeTree(opts);
        }).catch(() => { });
    }, []);
    // 工种联动加载科目列表
    const workTypeId = Form.useWatch('workTypeId', form);
    useEffect(() => {
        if (!workTypeId) {
            setCourseOptions([]);
            form.setFieldsValue({ courseIds: [] });
            return;
        }
        pageCourses({ workTypeId, page: 1, pageSize: 200 }).then((res) => {
            setCourseOptions(res.items.map((c) => ({ label: c.title, value: c.id })));
        }).catch(() => {
            setCourseOptions([]);
        });
        form.setFieldsValue({ courseIds: [] });
    }, [workTypeId]);
    // 加载模型列表
    useEffect(() => {
        listEnabledFaqModels().then(setModels).catch(() => { });
    }, []);
    // 加载批次列表
    const loadBatches = useCallback(async () => {
        try {
            const res = await listFaqBatches({ page: 1, pageSize: 20 });
            setBatches(res.items);
        }
        catch {
            // silent
        }
    }, []);
    useEffect(() => { loadBatches(); }, [loadBatches]);
    // 轮询进度
    useEffect(() => {
        if (pollRef.current)
            clearInterval(pollRef.current);
        if (!pollingBatchId)
            return;
        pollRef.current = setInterval(async () => {
            if (!paneActiveRef.current)
                return; // 标签不可见：跳过本轮请求，服务端转换照常进行
            try {
                const progress = await getFaqBatchProgress(pollingBatchId);
                setBatches((prev) => prev.map((b) => b.id === pollingBatchId ? progress : b));
                if (progress.status === 'completed' || progress.status === 'canceled') {
                    setPollingBatchId(null);
                    message.success('批次转换完成');
                    loadBatches();
                }
                else if (progress.status === 'paused') {
                    setPollingBatchId(null);
                }
            }
            catch {
                // silent
            }
        }, 20000);
        return () => { if (pollRef.current)
            clearInterval(pollRef.current); };
    }, [pollingBatchId, loadBatches]);
    // 加载记录
    const loadRecords = useCallback(async () => {
        if (!selectedBatchId)
            return;
        setLoading(true);
        try {
            const res = await pageFaqRecords({ batchId: selectedBatchId, page, pageSize });
            setRecords(res.items);
            setTotal(res.total);
        }
        catch {
            // silent
        }
        finally {
            setLoading(false);
        }
    }, [selectedBatchId, page, pageSize]);
    useEffect(() => { loadRecords(); }, [loadRecords]);
    // 发起转换
    const handleSubmit = async () => {
        try {
            const values = await form.validateFields();
            setSubmitting(true);
            const batch = await submitFaqConversion({
                workTypeId: values.workTypeId,
                courseIds: values.courseIds,
                questionTypes: values.questionTypes,
                modelCode: values.modelCode,
                concurrencyLimit: values.concurrencyLimit || 5,
            });
            message.success(`已创建批次，共 ${batch.totalExams} 道试题`);
            setSelectedBatchId(batch.id);
            setPollingBatchId(batch.id);
            loadBatches();
            form.resetFields();
        }
        catch (e) {
            message.error(e.message || '提交失败');
        }
        finally {
            setSubmitting(false);
        }
    };
    // 查看明细
    const handleViewDetail = async (id) => {
        try {
            const detail = await getFaqRecordDetail(id);
            setDetailRecord(detail);
            setDetailOpen(true);
        }
        catch {
            message.error('加载详情失败');
        }
    };
    // 导出批次成功的 FAQ
    const handleExport = async (batchId) => {
        try {
            const blob = await exportFaqBatch(batchId);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `faq_batch_${batchId}.zip`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        }
        catch (e) {
            message.error(e.message || '导出失败，请确认该批次有成功转换的记录');
        }
    };
    // 记录表格列
    const recordColumns = [
        { title: 'ID', dataIndex: 'id', width: 100, ellipsis: true },
        {
            title: '题型', dataIndex: 'sourceType', width: 80,
            render: (t) => TYPE_LABELS[t] || t,
        },
        {
            title: '标准问', dataIndex: 'standardQuestion', ellipsis: true,
        },
        {
            title: '标签', dataIndex: 'tagName', width: 200, ellipsis: true,
        },
        {
            title: '状态', dataIndex: 'conversionStatus', width: 100,
            render: (s) => {
                const cfg = STATUS_CONFIG[s] || { color: 'default', label: s };
                return _jsx(Tag, { color: cfg.color, icon: cfg.icon, children: cfg.label });
            },
        },
        {
            title: '操作', key: 'actions', width: 80,
            render: (_, row) => (_jsx(Tooltip, { title: "\u67E5\u770B\u8BE6\u60C5", children: _jsx(Button, { size: "small", type: "link", icon: _jsx(EyeOutlined, {}), onClick: () => handleViewDetail(row.id) }) })),
        },
    ];
    return (_jsxs("div", { style: { padding: 24 }, children: [_jsxs(Card, { title: _jsxs(_Fragment, { children: [_jsx(RobotOutlined, {}), " \u5DE5\u79CD\u9898\u5E93\u7ED3\u6784\u5316\u95EE\u7B54\u7BA1\u7406"] }), extra: _jsx(Button, { icon: _jsx(SettingOutlined, {}), onClick: () => setConfigOpen(true), children: "WeKnora & LLM \u914D\u7F6E" }), children: [_jsxs(Form, { form: form, layout: "inline", style: { marginBottom: 16 }, children: [_jsx(Form.Item, { name: "workTypeId", label: "\u5DE5\u79CD", rules: [{ required: true, message: '请选择工种' }], children: _jsx(Select, { options: workTypeTree, placeholder: "\u9009\u62E9\u5DE5\u79CD", style: { width: 200 }, showSearch: true, optionFilterProp: "label" }) }), _jsx(Form.Item, { name: "courseIds", label: "\u79D1\u76EE", children: _jsx(Select, { mode: "multiple", options: courseOptions, placeholder: workTypeId ? '选择科目（留空=全部）' : '请先选择工种', disabled: !workTypeId, allowClear: true, showSearch: true, optionFilterProp: "label", maxTagCount: 3, style: { width: 200 } }) }), _jsx(Form.Item, { name: "questionTypes", label: "\u9898\u578B", initialValue: ['single', 'multi', 'judge'], children: _jsx(Checkbox.Group, { options: [{ label: '单选', value: 'single' }, { label: '多选', value: 'multi' }, { label: '判断', value: 'judge' }] }) }), _jsx(Form.Item, { name: "modelCode", label: "LLM \u6A21\u578B", rules: [{ required: true, message: '请选择模型' }], children: _jsx(Select, { placeholder: "\u9009\u62E9\u6A21\u578B", style: { width: 200 }, children: models.map((m) => (_jsx(Select.Option, { value: m.modelCode, children: m.modelName }, m.modelCode))) }) }), _jsx(Form.Item, { children: _jsx(Button, { type: "primary", icon: _jsx(PlayCircleOutlined, {}), loading: submitting, onClick: handleSubmit, children: "\u5F00\u59CB\u8F6C\u6362" }) })] }), _jsx(Card, { size: "small", title: "\u8F6C\u6362\u6279\u6B21", style: { marginBottom: 16 }, children: _jsx(Table, { size: "small", rowKey: "id", dataSource: batches, pagination: { pageSize: 5, showSizeChanger: false }, onRow: (record) => ({ onClick: () => setSelectedBatchId(record.id), style: { cursor: 'pointer' } }), rowClassName: (record) => record.id === selectedBatchId ? 'ant-table-row-selected' : '', columns: [
                                { title: '批次ID', dataIndex: 'id', width: 120, ellipsis: true },
                                { title: '工种', dataIndex: 'workTypeName', width: 120 },
                                { title: '模型', dataIndex: 'modelName', width: 120 },
                                { title: '总数', dataIndex: 'totalExams', width: 60 },
                                { title: '成功', dataIndex: 'successCount', width: 60 },
                                { title: '失败', dataIndex: 'failedCount', width: 60 },
                                { title: '跳过', dataIndex: 'skippedCount', width: 60 },
                                {
                                    title: '状态', dataIndex: 'status', width: 80,
                                    render: (s) => {
                                        const colors = { running: 'processing', paused: 'warning', completed: 'success', canceled: 'default' };
                                        const labels = { running: '运行中', paused: '暂停', completed: '完成', canceled: '取消' };
                                        return _jsx(Tag, { color: colors[s] || 'default', children: labels[s] || s });
                                    },
                                },
                                {
                                    title: '操作', key: 'batchActions', width: 260,
                                    render: (_, row) => (_jsxs(Space, { size: "small", children: [row.status === 'running' && (_jsx(Button, { size: "small", icon: _jsx(PauseCircleOutlined, {}), onClick: (e) => { e.stopPropagation(); pauseFaqBatch(row.id).then(() => { setPollingBatchId(null); loadBatches(); }); }, children: "\u6682\u505C" })), row.status === 'paused' && (_jsx(Button, { size: "small", type: "primary", icon: _jsx(PlayCircleOutlined, {}), onClick: (e) => { e.stopPropagation(); resumeFaqBatch(row.id).then(() => { setPollingBatchId(row.id); loadBatches(); }); }, children: "\u7EE7\u7EED" })), _jsx(Popconfirm, { title: "\u786E\u5B9A\u91CD\u8BD5\u5931\u8D25\u8BB0\u5F55\uFF1F", onConfirm: (e) => { e?.stopPropagation(); retryFaqBatch(row.id).then(loadRecords); }, children: _jsx(Button, { size: "small", icon: _jsx(ReloadOutlined, {}), onClick: (e) => e.stopPropagation(), children: "\u91CD\u8BD5\u5931\u8D25" }) }), _jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), disabled: !row.successCount || row.successCount === 0, onClick: (e) => { e.stopPropagation(); handleExport(row.id); }, children: "\u5BFC\u51FA" })] })),
                                },
                            ] }) }), selectedBatchId && (_jsx(Card, { size: "small", title: `转换明细（批次 ${selectedBatchId}）`, children: _jsx(Table, { size: "small", rowKey: "id", loading: loading, dataSource: records, columns: recordColumns, pagination: {
                                current: page,
                                pageSize,
                                total,
                                showSizeChanger: true,
                                onChange: (p, ps) => {
                                    if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                                        setPageSize(ps);
                                        setPage(1);
                                    }
                                    else {
                                        setPage(p);
                                    }
                                },
                            } }) }))] }), _jsx(Drawer, { title: "\u8F6C\u6362\u8BB0\u5F55\u8BE6\u60C5", width: 600, open: detailOpen, onClose: () => setDetailOpen(false), children: detailRecord && (_jsxs("div", { style: { marginBottom: 16 }, children: [_jsx("h3", { children: "\u6807\u51C6\u95EE" }), _jsx("p", { children: detailRecord.standardQuestion }), _jsx("h3", { children: "\u7B54\u6848" }), Array.isArray(detailRecord.answers) && (_jsx("ul", { children: detailRecord.answers.map((a, i) => _jsx("li", { children: a }, i)) })), _jsx("h3", { children: "\u76F8\u4F3C\u95EE" }), Array.isArray(detailRecord.similarQuestions) && (_jsx("ul", { children: detailRecord.similarQuestions.map((a, i) => _jsx("li", { children: a }, i)) })), _jsx("h3", { children: "\u53CD\u4F8B" }), Array.isArray(detailRecord.negativeQuestions) && (_jsx("ul", { children: detailRecord.negativeQuestions.map((a, i) => _jsx("li", { children: a }, i)) })), Boolean(detailRecord.errorMessage) && (_jsxs(_Fragment, { children: [_jsx("h3", { children: "\u9519\u8BEF\u4FE1\u606F" }), _jsx(Tag, { color: "error", children: String(detailRecord.errorMessage) })] }))] })) }), _jsx(FaqConfigDialog, { open: configOpen, onClose: () => { setConfigOpen(false); listEnabledFaqModels().then(setModels).catch(() => { }); } })] }));
}
export default FaqConversionPage;
