import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 细目表管理页 —— 《职业技能标准细目表生成功能实现方案》v1.2 §10.2
 * 菜单：智能课程材料管理 > 细目表管理（code xi_mu_biao_guan_li）
 * 两态：任务列表态 / 树编辑态（选中任务进入）
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Button, Card, Checkbox, Input, InputNumber, Modal, Popconfirm, Progress, Radio, Select, Space, Table, Tabs, Tag, Tooltip, Typography, Upload, message, } from 'antd';
import { ArrowLeftOutlined, ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, DownloadOutlined, EditOutlined, LoadingOutlined, PlusOutlined, ReloadOutlined, RobotOutlined, UploadOutlined, } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { hasPermission } from '../../directives/permission';
import { usePaneActive, usePaneActiveRef } from '../../layouts/TabKeepAlive';
import { createNode, deleteNode, deleteTask, downloadExport, downloadOriginalDocx, fetchTask, fetchTaskPage, fetchTree, moveNode, resummarizeNode, reviewDone, triggerSummarize, updateNode, uploadStandardDocx, } from './api';
import './XiMuBiaoGuanLiPage.css';
const STATUS_TEXT = {
    uploaded: { text: '已上传', color: 'default' },
    parsed: { text: '已解析', color: 'blue' },
    llm_running: { text: 'AI概括中', color: 'processing' },
    reviewing: { text: '待审核', color: 'orange' },
    confirmed: { text: '已确认', color: 'green' },
    exported: { text: '已导出', color: 'purple' },
};
const SOURCE_TEXT = {
    extract: '规则抽取',
    llm: 'AI概括',
    manual: '人工新增',
};
const REVIEW_TEXT = {
    pending: { text: '待审核', color: 'orange' },
    approved: { text: '已审核', color: 'green' },
    modified: { text: '已修改', color: 'blue' },
};
function buildTreeRows(flat) {
    const byParent = new Map();
    for (const n of flat) {
        const key = n.parentId || '0';
        if (!byParent.has(key))
            byParent.set(key, []);
        byParent.get(key).push(n);
    }
    const sort = (list) => [...list].sort((a, b) => a.sortNumber - b.sortNumber || a.nodeKey.localeCompare(b.nodeKey));
    const attach = (parentId) => sort(byParent.get(parentId) || []).map((n) => {
        const children = attach(n.id);
        return children.length > 0 ? { ...n, children } : { ...n };
    });
    return attach('0');
}
export default function XiMuBiaoGuanLiPage() {
    const [activeTaskId, setActiveTaskId] = useState(null);
    return activeTaskId == null ? (_jsx(TaskListView, { onEnter: setActiveTaskId })) : (_jsx(TreeEditView, { taskId: activeTaskId, onBack: () => setActiveTaskId(null) }));
}
// ==================================================================
// 任务列表态
// ==================================================================
function TaskListView({ onEnter }) {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询/进度 tick（§3.6）
    const paneActiveRef = usePaneActiveRef(paneActive); // 长生命周期定时器闭包读 ref
    const [page, setPage] = useState(1);
    const [keyword, setKeyword] = useState('');
    const canGenerate = hasPermission('ximubiaoguanli:generate');
    const canExport = hasPermission('ximubiaoguanli:export');
    const [uploading, setUploading] = useState(false);
    const [uploadPercent, setUploadPercent] = useState(0);
    const uploadTimerRef = useRef(null);
    // AI 概括感知：上一轮 leafTotal 快照、增量提示（+N）、触发中的任务 id
    const prevLeafRef = useRef(new Map());
    const [leafDeltas, setLeafDeltas] = useState({});
    const [summarizingId, setSummarizingId] = useState(null);
    // 组件卸载时清理模拟进度定时器
    useEffect(() => () => {
        if (uploadTimerRef.current !== null)
            window.clearInterval(uploadTimerRef.current);
    }, []);
    const { data, isLoading, refetch } = useQuery({
        queryKey: ['standard-tasks', page, keyword],
        queryFn: () => fetchTaskPage({ page, pageSize: 20, keyword: keyword || undefined }),
        // 每次回到列表页强制拉最新（上传后跳预览树再返回、或他处触发概括后返回，均需看到新状态）
        refetchOnMount: 'always',
    });
    // LLM 概括中的任务轮询状态（标签不可见时定时器照常建立、仅跳过 refetch，切回后自动恢复，§3.6）
    useEffect(() => {
        const hasRunning = (data?.items || []).some((t) => t.status === 'llm_running');
        if (!hasRunning)
            return;
        const timer = setInterval(() => {
            if (paneActiveRef.current)
                refetch();
        }, 5000);
        return () => clearInterval(timer);
    }, [data, refetch]);
    // AI 概括感知：对比上一轮 leafTotal 快照，计算增量触发 +N 渐隐动画
    useEffect(() => {
        const items = data?.items || [];
        const deltas = {};
        for (const t of items) {
            const prev = prevLeafRef.current.get(t.id);
            if (t.status === 'llm_running' && prev !== undefined && t.leafTotal > prev) {
                deltas[t.id] = t.leafTotal - prev;
            }
            prevLeafRef.current.set(t.id, t.leafTotal);
        }
        if (Object.keys(deltas).length > 0) {
            setLeafDeltas(deltas);
            const timer = window.setTimeout(() => setLeafDeltas({}), 2600);
            return () => window.clearTimeout(timer);
        }
    }, [data]);
    // 触发概括后：按钮 loading 保持到任务状态变为 llm_running
    useEffect(() => {
        if (!summarizingId)
            return;
        const t = (data?.items || []).find((x) => x.id === summarizingId);
        if (t && t.status === 'llm_running')
            setSummarizingId(null);
    }, [data, summarizingId]);
    const handleUpload = async (file) => {
        // 解析为同步接口、服务端无进度回调：用模拟进度条提示「处理中」，上限 95%，完成后跳 100%
        setUploading(true);
        setUploadPercent(0);
        if (uploadTimerRef.current !== null)
            window.clearInterval(uploadTimerRef.current);
        uploadTimerRef.current = window.setInterval(() => {
            // 标签不可见时计时继续、跳过 setState（§3.6 乙组口径：不中断任务）
            if (paneActiveRef.current) {
                setUploadPercent((p) => p >= 95 ? p : Math.min(95, p + Math.max(1, Math.round((95 - p) / 10))));
            }
        }, 300);
        try {
            const task = await uploadStandardDocx(file);
            message.success(`解析完成：${task.workTypeTitle}（${task.levels.length} 个等级）`);
            // 等列表缓存刷新完成再进预览树，保证返回列表时能看到新任务
            await queryClient.invalidateQueries({ queryKey: ['standard-tasks'] });
            onEnter(task.id);
        }
        catch (e) {
            message.error(e?.response?.data?.message || e?.message || '上传解析失败');
        }
        finally {
            if (uploadTimerRef.current !== null) {
                window.clearInterval(uploadTimerRef.current);
                uploadTimerRef.current = null;
            }
            setUploadPercent(100);
            window.setTimeout(() => {
                setUploading(false);
                setUploadPercent(0);
            }, 500);
        }
    };
    const handleSummarize = async (task, req) => {
        setSummarizingId(task.id);
        try {
            await triggerSummarize(task.id, req);
            message.success('LLM 概括已触发（异步执行，状态自动刷新）');
            refetch();
        }
        catch (e) {
            message.error(e?.response?.data?.message || e?.message || '触发失败');
            setSummarizingId(null);
            throw e;
        }
    };
    const handleExport = async (task, kind) => {
        try {
            await downloadExport(task.id, kind);
            message.success('导出成功');
        }
        catch (e) {
            message.error(e?.message || '导出失败');
        }
    };
    const [sqlTask, setSqlTask] = useState(null);
    const [summarizeTask, setSummarizeTask] = useState(null);
    const [downloadingId, setDownloadingId] = useState(null);
    const handleDownloadOriginal = async (task) => {
        if (!task.fileId) {
            message.warning('该任务没有关联的原始文件');
            return;
        }
        setDownloadingId(task.id);
        try {
            await downloadOriginalDocx(task.fileId);
        }
        catch (e) {
            message.error(e?.message || '下载失败');
        }
        finally {
            setDownloadingId(null);
        }
    };
    const columns = [
        { title: '职业名称', dataIndex: 'workTypeTitle', width: 140 },
        { title: '编码', dataIndex: 'seriesNo', width: 110 },
        { title: '版本', dataIndex: 'version', width: 70, render: (v) => (v ? `${v}年版` : '-') },
        { title: '等级数', width: 70, render: (_, t) => t.levels.length },
        {
            title: '工种',
            dataIndex: 'directions',
            width: 140,
            render: (dirs) => dirs.length === 1 && dirs[0] === '默认' ? '（不分工种）' : dirs.join('、'),
        },
        {
            title: '原始文件',
            dataIndex: 'fileName',
            width: 100,
            ellipsis: true,
            render: (name) => name ? (_jsx(Tooltip, { title: name, children: name })) : ('-'),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (s) => (_jsx(Tag, { icon: s === 'llm_running' ? _jsx(LoadingOutlined, { spin: true }) : undefined, color: STATUS_TEXT[s]?.color, children: STATUS_TEXT[s]?.text || s })),
        },
        {
            title: '待审/总数',
            width: 130,
            render: (_, t) => {
                const running = t.status === 'llm_running';
                const delta = leafDeltas[t.id];
                return (_jsxs(Space, { size: 4, children: [_jsxs("span", { className: running ? 'xmb-leaf-pulse' : undefined, children: [t.leafPending, "/", t.leafTotal] }), running && delta !== undefined && delta > 0 && (_jsxs("span", { className: "xmb-leaf-delta", children: ["+", delta] }, `${t.id}-${t.leafTotal}`))] }));
            },
        },
        {
            title: '解析警告',
            dataIndex: 'parseWarnings',
            ellipsis: true,
            render: (w) => w ? (_jsx(Tooltip, { title: _jsx("pre", { style: { maxWidth: 480, whiteSpace: 'pre-wrap' }, children: w }), children: _jsxs(Typography.Text, { type: "warning", children: [w.split('\n').length, " \u6761"] }) })) : ('-'),
        },
        {
            title: '上传时间',
            dataIndex: 'createdAt',
            width: 130,
            render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
        },
        {
            title: '操作',
            width: 380,
            fixed: 'right',
            render: (_, t) => (_jsxs(Space, { size: 4, wrap: true, children: [_jsx(Button, { size: "small", type: "link", icon: _jsx(DownloadOutlined, {}), disabled: !t.fileId, loading: downloadingId === t.id, onClick: () => handleDownloadOriginal(t), children: "\u4E0B\u8F7D\u539F\u4EF6" }), _jsx(Button, { size: "small", type: "link", onClick: () => onEnter(t.id), children: "\u9884\u89C8/\u5BA1\u6838\u6811" }), canGenerate && (_jsx(Button, { size: "small", type: "link", icon: _jsx(RobotOutlined, {}), disabled: t.status === 'llm_running' || summarizingId === t.id, loading: summarizingId === t.id, onClick: () => setSummarizeTask(t), children: "AI\u6982\u62EC" })), canExport && (_jsxs(_Fragment, { children: [_jsx(Button, { size: "small", type: "link", onClick: () => handleExport(t, 'excel'), children: "\u7406\u8BBAExcel" }), _jsx(Button, { size: "small", type: "link", onClick: () => handleExport(t, 'skill-excel'), children: "\u6280\u80FDExcel" }), _jsx(Button, { size: "small", type: "link", onClick: () => setSqlTask(t), children: "\u5165\u5E93SQL" })] })), canGenerate && (_jsx(Popconfirm, { title: "\u5220\u9664\u8BE5\u89E3\u6790\u4EFB\u52A1\u53CA\u5176\u5168\u90E8\u8282\u70B9\uFF1F", onConfirm: async () => {
                            await deleteTask(t.id);
                            message.success('已删除');
                            refetch();
                        }, children: _jsx(Button, { size: "small", type: "link", danger: true, icon: _jsx(DeleteOutlined, {}) }) }))] })),
        },
    ];
    return (_jsxs(Card, { title: "\u7EC6\u76EE\u8868\u7BA1\u7406\uFF08\u56FD\u5BB6\u804C\u4E1A\u6280\u80FD\u6807\u51C6 \u2192 \u8003\u8BC4\u8981\u7D20\u7EC6\u76EE\u8868\uFF09", extra: _jsxs(Space, { children: [_jsx(Input.Search, { placeholder: "\u804C\u4E1A\u540D\u79F0", allowClear: true, style: { width: 200 }, onSearch: (v) => {
                        setKeyword(v);
                        setPage(1);
                    } }), canGenerate && (_jsxs(Space, { size: 8, children: [_jsx(Upload, { accept: ".docx", showUploadList: false, disabled: uploading, customRequest: ({ file }) => handleUpload(file), children: _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), loading: uploading, disabled: uploading, children: uploading ? '解析中…' : '上传标准 docx' }) }), uploading && (_jsxs(_Fragment, { children: [_jsx(Progress, { percent: uploadPercent, size: "small", status: "active", style: { width: 160, margin: 0 } }), _jsx("span", { className: "xmb-uploading-hint", children: "\u6B63\u5728\u89E3\u6790\u6807\u51C6\u6587\u6863\uFF0C\u8BF7\u52FF\u5173\u95ED\u9875\u9762\u2026" })] }))] }))] }), children: [(data?.items || []).some((t) => t.status === 'llm_running') && (_jsx(Alert, { type: "info", showIcon: true, icon: _jsx(LoadingOutlined, { spin: true }), message: "AI \u6982\u62EC\u8FDB\u884C\u4E2D\uFF1A\u6BCF 5 \u79D2\u81EA\u52A8\u5237\u65B0\uFF0C\u5168\u91CF\u6982\u62EC\u7EA6\u9700\u6570\u5206\u949F\uFF0C\u53EF\u79BB\u5F00\u672C\u9875\u7A0D\u540E\u56DE\u6765\u67E5\u770B\u3002", style: { marginBottom: 12 } })), _jsx(Table, { rowKey: "id", loading: isLoading, dataSource: data?.items || [], columns: columns, scroll: { x: 1560 }, pagination: {
                    current: page,
                    pageSize: 20,
                    total: data?.total || 0,
                    onChange: setPage,
                    showTotal: (t) => `共 ${t} 条`,
                } }), _jsx(SqlExportModal, { open: !!sqlTask, taskId: sqlTask?.id || '', onClose: () => setSqlTask(null) }), _jsx(SummarizeModal, { task: summarizeTask, onClose: () => setSummarizeTask(null), onStart: (req) => handleSummarize(summarizeTask, req) })] }));
}
// ==================================================================
// 树编辑态
// ==================================================================
// 入库 SQL 导出模式选择（create=新建课程 / reuse=复用已有课程）
// ==================================================================
function SqlExportModal({ open, taskId, scope, onClose }) {
    const [mode, setMode] = useState('create');
    const [loading, setLoading] = useState(false);
    const doExport = async () => {
        setLoading(true);
        try {
            await downloadExport(taskId, 'sql', { ...scope, mode });
            message.success('导出成功');
            onClose();
        }
        catch (e) {
            message.error(e?.message || '导出失败');
        }
        finally {
            setLoading(false);
        }
    };
    return (_jsxs(Modal, { open: open, onCancel: onClose, onOk: doExport, confirmLoading: loading, title: "\u5BFC\u51FA\u5165\u5E93SQL", okText: "\u5BFC\u51FA", children: [_jsx(Radio.Group, { value: mode, onChange: (e) => setMode(e.target.value), children: _jsxs(Space, { direction: "vertical", children: [_jsx(Radio, { value: "create", children: "\u65B0\u5EFA\u8BFE\u7A0B\uFF08\u751F\u6210 exam_work_type + exam_course\uFF09" }), _jsx(Radio, { value: "reuse", children: "\u590D\u7528\u5DF2\u6709\u8BFE\u7A0B\uFF08\u6309\u5DE5\u79CD/\u7B49\u7EA7\u5339\u914D\u5DF2\u6709 course_id\uFF09" })] }) }), mode === 'reuse' && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginTop: 10 }, message: "\u590D\u7528\u6A21\u5F0F\u6267\u884C\u524D\u8BF7\u5148\u5907\u4EFD\u76EE\u6807\u5E93\uFF1A\u5C06\u5220\u9664\u5339\u914D\u8BFE\u7A0B\u4E0B\u5168\u90E8\u65E7\u77E5\u8BC6\u6811\uFF0C\u518D\u5199\u5165\u65B0\u6811\u3002" }))] }));
}
// ==================================================================
// AI 概括范围选择弹窗：工种×等级矩阵 + 重要程度档位（默认 XY）
// ==================================================================
function SummarizeModal({ task, onClose, onStart }) {
    const [selected, setSelected] = useState(new Set());
    const [importants, setImportants] = useState(['X', 'Y']);
    const [loading, setLoading] = useState(false);
    // 有效组合集合：directionLevels 为空（旧任务）时全部等级×工种有效
    const validKeys = useMemo(() => {
        const set = new Set();
        if (!task)
            return set;
        const matrix = task.directionLevels && Object.keys(task.directionLevels).length > 0
            ? task.directionLevels
            : null;
        for (const level of task.levels) {
            for (const dir of task.directions) {
                if (matrix && !matrix[dir]?.includes(level.levelCode))
                    continue;
                set.add(`${level.levelCode}|${dir}`);
            }
        }
        return set;
    }, [task]);
    // 打开弹窗时重置默认：全部有效组合 + XYZ
    useEffect(() => {
        if (task) {
            setSelected(new Set(validKeys));
            setImportants(['X', 'Y', 'Z']);
        }
    }, [task, validKeys]);
    if (!task)
        return null;
    const toggle = (key, checked) => {
        setSelected((prev) => {
            const next = new Set(prev);
            if (checked)
                next.add(key);
            else
                next.delete(key);
            return next;
        });
    };
    const doStart = async () => {
        if (selected.size === 0) {
            message.warning('请至少选择一个工种×等级组合');
            return;
        }
        if (importants.length === 0) {
            message.warning('请至少选择一个重要程度');
            return;
        }
        setLoading(true);
        try {
            await onStart({
                scopes: [...selected].map((k) => {
                    const [lc, dir] = k.split('|');
                    return { levelCode: Number(lc), direction: dir };
                }),
                importants,
            });
            onClose();
        }
        catch {
            // onStart 内部已提示错误，弹窗保持打开便于调整
        }
        finally {
            setLoading(false);
        }
    };
    return (_jsx(Modal, { open: true, title: `AI 概括范围选择（${task.workTypeTitle}）`, okText: "\u5F00\u59CB\u6982\u62EC", confirmLoading: loading, onCancel: onClose, onOk: doStart, width: 640, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsxs("div", { children: [_jsxs(Space, { style: { marginBottom: 4 }, children: [_jsx(Typography.Text, { strong: true, children: "\u5DE5\u79CD \u00D7 \u7B49\u7EA7" }), _jsx(Button, { type: "link", size: "small", onClick: () => setSelected(new Set(validKeys)), children: "\u5168\u9009" }), _jsx(Button, { type: "link", size: "small", onClick: () => setSelected(new Set()), children: "\u6E05\u7A7A" })] }), _jsxs("table", { className: "xmb-scope-matrix", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "\u5DE5\u79CD \\ \u7B49\u7EA7" }), task.levels.map((l) => (_jsx("th", { children: l.levelName }, l.levelCode)))] }) }), _jsx("tbody", { children: task.directions.map((dir) => (_jsxs("tr", { children: [_jsx("td", { children: dir === '默认' ? '（不分工种）' : dir }), task.levels.map((l) => {
                                                const key = `${l.levelCode}|${dir}`;
                                                return (_jsx("td", { children: validKeys.has(key) ? (_jsx(Checkbox, { checked: selected.has(key), onChange: (e) => toggle(key, e.target.checked) })) : (_jsx("span", { className: "xmb-scope-na", children: "\u2014" })) }, key));
                                            })] }, dir))) })] })] }), _jsxs("div", { children: [_jsx(Typography.Text, { strong: true, children: "\u91CD\u8981\u7A0B\u5EA6\uFF08\u4EC5\u4F5C\u7528\u4E8E\u7406\u8BBA\u8003\u8BC4\u70B9\uFF09" }), _jsx("div", { style: { marginTop: 4 }, children: _jsx(Checkbox.Group, { value: importants, onChange: (v) => setImportants(v), options: [
                                    { value: 'X', label: 'X（核心必考）' },
                                    { value: 'Y', label: 'Y（重要）' },
                                    { value: 'Z', label: 'Z（一般了解）' },
                                ] }) })] }), _jsx(Alert, { type: "info", showIcon: true, message: "\u7406\u8BBA\u8003\u8BC4\u70B9\u6309\u5C0F\u8282\u8003\u8BC4\u6743\u91CD\u5206\u914D\uFF08\u6BCF\u5F20\u7EC6\u76EE\u8868\u7EA6 200 \u4E2A\uFF0C\u6743\u91CD\u8D8A\u5927\u7684\u5C0F\u8282\u751F\u6210\u8D8A\u591A\uFF09\uFF1B\u6280\u80FD\u8003\u8BC4\u70B9\u6309\u539F\u6587\u9010\u6761\u751F\u6210\u3002" })] }) }));
}
// ==================================================================
function TreeEditView({ taskId, onBack }) {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    const canGenerate = hasPermission('ximubiaoguanli:generate');
    const canExport = hasPermission('ximubiaoguanli:export');
    const [scope, setScope] = useState(null);
    const [dimension, setDimension] = useState('theory');
    const [editing, setEditing] = useState(null);
    const [addingUnder, setAddingUnder] = useState(null);
    const [resummarizing, setResummarizing] = useState(null);
    const [sqlOpen, setSqlOpen] = useState(false);
    const [summarizeOpen, setSummarizeOpen] = useState(false);
    const [summarizing, setSummarizing] = useState(false);
    const warningsRef = useRef(null);
    const { data: task, refetch: refetchTask } = useQuery({
        queryKey: ['standard-task', taskId],
        queryFn: () => fetchTask(taskId),
        refetchInterval: (q) => (paneActive && q.state.data?.status === 'llm_running' ? 5000 : false),
    });
    // 可选范围 = levels × directions，按 1.4 节工种可用等级矩阵过滤，旧任务无矩阵则保留全组合
    const scopes = useMemo(() => {
        if (!task)
            return [];
        const out = [];
        const constraints = task.directionLevels && Object.keys(task.directionLevels).length > 0
            ? task.directionLevels
            : null;
        for (const level of task.levels) {
            for (const direction of task.directions) {
                if (constraints && !constraints[direction]?.includes(level.levelCode)) {
                    continue;
                }
                out.push({
                    levelCode: level.levelCode,
                    direction,
                    label: task.directions.length > 1 ? `${level.levelName}-${direction}` : level.levelName,
                });
            }
        }
        return out;
    }, [task]);
    const activeScope = useMemo(() => {
        if (!scopes.length)
            return null;
        const match = scopes.find((s) => s.levelCode === scope?.levelCode && s.direction === scope?.direction);
        return match ?? scopes[0];
    }, [scopes, scope]);
    const { data: flatNodes, isLoading, refetch } = useQuery({
        queryKey: ['standard-tree', taskId, activeScope?.levelCode, activeScope?.direction, dimension],
        queryFn: () => fetchTree(taskId, {
            levelCode: activeScope.levelCode,
            direction: activeScope.direction,
            dimension,
        }),
        enabled: !!activeScope,
    });
    // LLM 概括完成后自动刷新树
    useEffect(() => {
        if (task?.status === 'reviewing') {
            refetch();
        }
    }, [task?.status, refetch]);
    // 技能视图对齐「技能要求权重表」：技能细目表从职业功能（L1）起，
    // 不出现「基本要求 A 支」和「工作要求 B 根」结构行（技能导出同样从 L1 起）
    const treeData = useMemo(() => {
        const flat = flatNodes || [];
        if (dimension !== 'skill') {
            return buildTreeRows(flat);
        }
        const hidden = new Set(flat
            .filter((n) => n.nodeKey === 'A' || n.nodeKey.startsWith('A-') || n.nodeKey === 'B')
            .map((n) => n.id));
        const rerooted = flat
            .filter((n) => !hidden.has(n.id))
            // 父节点被隐藏的 L1 职业功能提升为根节点
            .map((n) => (n.parentId && hidden.has(n.parentId) ? { ...n, parentId: '' } : n));
        return buildTreeRows(rerooted);
    }, [flatNodes, dimension]);
    const warnings = useMemo(() => (task?.parseWarnings || '').split('\n').filter(Boolean), [task]);
    const refreshAll = useCallback(() => {
        refetch();
        refetchTask();
        queryClient.invalidateQueries({ queryKey: ['standard-tasks'] });
    }, [refetch, refetchTask, queryClient]);
    // 触发概括后：按钮 loading 保持到任务状态变为 llm_running（任务查询 5s 轮询会自动翻转状态）
    useEffect(() => {
        if (summarizing && task?.status === 'llm_running')
            setSummarizing(false);
    }, [summarizing, task?.status]);
    const handleSummarize = async (req) => {
        if (!task)
            return;
        setSummarizing(true);
        try {
            await triggerSummarize(task.id, req);
            message.success('LLM 概括已触发（异步执行，状态自动刷新）');
            refetchTask();
        }
        catch (e) {
            message.error(e?.response?.data?.message || e?.message || '触发失败');
            setSummarizing(false);
            throw e;
        }
    };
    const handleExport = async (kind) => {
        if (!task)
            return;
        try {
            await downloadExport(task.id, kind, activeScope
                ? { levelCode: activeScope.levelCode, direction: activeScope.direction }
                : undefined);
            message.success('导出成功');
            refreshAll();
        }
        catch (e) {
            message.error(e?.message || '导出失败');
        }
    };
    const handleResummarize = async (node) => {
        setResummarizing(node.id);
        try {
            await resummarizeNode(node.id, dimension);
            message.success('重新生成完成');
            refreshAll();
        }
        catch (e) {
            message.error(e?.response?.data?.message || e?.message || '重新生成失败');
        }
        finally {
            setResummarizing(null);
        }
    };
    const columns = [
        {
            title: '标题',
            dataIndex: 'title',
            width: 340,
            render: (v, n) => (_jsxs(Space, { size: 4, children: [_jsx("span", { children: v }), n.needLlm === 1 && n.nodeLevel === 2 && (_jsx(Tooltip, { title: "\u7A7A\u5C0F\u8282\uFF1Adocx \u65E0\u5217\u4E3E\u9879\uFF0C\u9700 LLM \u6982\u62EC\u6216\u4EBA\u5DE5\u8865\u53F6", children: _jsx(Tag, { color: "red", children: "\u7A7A\u5C0F\u8282" }) }))] })),
        },
        { title: '编码', dataIndex: 'nodeKey', width: 110 },
        {
            title: '层级',
            dataIndex: 'nodeLevel',
            width: 60,
            render: (l) => `L${l}`,
        },
        {
            title: '维度',
            dataIndex: 'dimension',
            width: 70,
            render: (d, n) => n.nodeLevel === 3 ? (d === 'skill' ? _jsx(Tag, { color: "geekblue", children: "\u6280\u80FD" }) : _jsx(Tag, { children: "\u7406\u8BBA" })) : '-',
        },
        {
            title: '重要程度',
            dataIndex: 'important',
            width: 90,
            render: (v, n) => n.nodeLevel === 3 ? (canGenerate ? (_jsx(Select, { size: "small", value: v || 'X', style: { width: 64 }, options: ['X', 'Y', 'Z'].map((x) => ({ value: x, label: x })), onChange: async (val) => {
                    await updateNode(n.id, { important: val });
                    refetch();
                } })) : (v || 'X')) : ('-'),
        },
        {
            title: '权重',
            dataIndex: 'weight',
            width: 100,
            render: (v, n) => n.nodeLevel >= 1 && n.nodeLevel <= 2 ? (canGenerate ? (_jsx(InputNumber, { size: "small", value: v, min: 0, max: 1, step: 0.01, style: { width: 84 }, placeholder: "0.00", onBlur: async (e) => {
                    const val = e.target.value === '' ? null : Number(e.target.value);
                    if (val !== v) {
                        await updateNode(n.id, { weight: val ?? undefined });
                        refetch();
                    }
                } })) : (v ?? '-')) : ('-'),
        },
        {
            title: '来源',
            dataIndex: 'source',
            width: 90,
            render: (s) => SOURCE_TEXT[s] || s,
        },
        {
            title: '审核状态',
            dataIndex: 'reviewStatus',
            width: 90,
            render: (s) => (_jsx(Tag, { color: REVIEW_TEXT[s]?.color, children: REVIEW_TEXT[s]?.text || s })),
        },
        {
            title: '操作',
            width: 240,
            fixed: 'right',
            render: (_, n) => (_jsxs(Space, { size: 0, children: [canGenerate && (_jsx(Button, { size: "small", type: "link", icon: _jsx(EditOutlined, {}), onClick: () => setEditing(n) })), canGenerate && n.nodeLevel === 2 && (_jsxs(_Fragment, { children: [_jsx(Button, { size: "small", type: "link", icon: _jsx(PlusOutlined, {}), onClick: () => setAddingUnder(n), children: "\u52A0\u53F6" }), _jsx(Button, { size: "small", type: "link", icon: _jsx(ReloadOutlined, {}), loading: resummarizing === n.id, onClick: () => handleResummarize(n), children: "\u91CD\u65B0\u751F\u6210" })] })), canGenerate && (_jsxs(_Fragment, { children: [_jsx(Button, { size: "small", type: "link", icon: _jsx(ArrowUpOutlined, {}), onClick: async () => { await moveNode(n.id, 'up'); refetch(); } }), _jsx(Button, { size: "small", type: "link", icon: _jsx(ArrowDownOutlined, {}), onClick: async () => { await moveNode(n.id, 'down'); refetch(); } }), _jsx(Popconfirm, { title: "\u5220\u9664\u8BE5\u8282\u70B9\uFF08\u542B\u5B50\u6811\uFF09\uFF1F", onConfirm: async () => {
                                    await deleteNode(n.id);
                                    message.success('已删除');
                                    refreshAll();
                                }, children: _jsx(Button, { size: "small", type: "link", danger: true, icon: _jsx(DeleteOutlined, {}) }) })] }))] })),
        },
    ];
    if (!task) {
        return _jsx(Card, { loading: true });
    }
    // 职业编码/年版可能缺失（如重冶备料工 docx 无编码节），空值部分不显示
    const titleMeta = [task.seriesNo, task.version ? `${task.version}年版` : '']
        .filter(Boolean)
        .join('，');
    return (_jsxs(Card, { title: _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: onBack, children: "\u8FD4\u56DE\u5217\u8868" }), _jsxs("span", { children: [task.workTypeTitle, titleMeta ? `（${titleMeta}）` : ''] }), _jsx(Tag, { color: STATUS_TEXT[task.status]?.color, children: STATUS_TEXT[task.status]?.text }), _jsxs("span", { className: task.status === 'llm_running' ? 'xmb-leaf-pulse' : undefined, children: ["\u5F85\u5BA1 ", task.leafPending, "/", task.leafTotal] })] }), extra: _jsxs(Space, { children: [canGenerate && (_jsx(Button, { type: "primary", icon: _jsx(RobotOutlined, {}), disabled: task.status === 'llm_running', loading: summarizing, onClick: () => setSummarizeOpen(true), children: "AI\u6982\u62EC" })), canGenerate && (_jsx(Popconfirm, { title: "\u5C06\u5168\u90E8\u5F85\u5BA1\u53F6\u8282\u70B9\u6807\u4E3A\u5DF2\u5BA1\u6838\uFF1F", onConfirm: async () => {
                        try {
                            await reviewDone(task.id);
                            message.success('已全部标为已审核');
                            refreshAll();
                        }
                        catch (e) {
                            message.error(e?.response?.data?.message || e?.message || '操作失败');
                        }
                    }, children: _jsx(Button, { children: "\u5168\u90E8\u6807\u5DF2\u5BA1" }) })), canExport && (_jsxs(_Fragment, { children: [_jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: () => handleExport('excel'), children: "\u5BFC\u51FA\u7406\u8BBA\u7EC6\u76EE\u8868" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: () => handleExport('skill-excel'), children: "\u5BFC\u51FA\u6280\u80FD\u7EC6\u76EE\u8868" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: () => setSqlOpen(true), children: "\u5BFC\u51FA\u5165\u5E93SQL" })] }))] }), children: [task.status === 'llm_running' && (_jsx(Alert, { type: "info", showIcon: true, message: "LLM \u6982\u62EC\u8FDB\u884C\u4E2D\uFF0C\u72B6\u6001\u6BCF 5 \u79D2\u81EA\u52A8\u5237\u65B0\u2026", style: { marginBottom: 12 } })), warnings.length > 0 && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: `解析警告 ${warnings.length} 条`, description: _jsx("div", { ref: warningsRef, style: { maxHeight: 120, overflow: 'auto' }, children: warnings.map((w, i) => (_jsx("div", { children: w }, i))) }) })), _jsxs(Space, { style: { marginBottom: 12 }, wrap: true, children: [_jsx("span", { children: "\u7406\u8BBA/\u6280\u80FD\u53F6\uFF1A" }), _jsx(Select, { value: dimension, style: { width: 140 }, onChange: setDimension, options: [
                            { value: 'theory', label: '理论考评点' },
                            { value: 'skill', label: '技能考评点（技）' },
                        ] })] }), _jsx(Tabs, { activeKey: activeScope ? `${activeScope.levelCode}|${activeScope.direction}` : undefined, onChange: (key) => {
                    const [levelCode, direction] = key.split('|');
                    setScope({ levelCode: Number(levelCode), direction });
                }, items: scopes.map((s) => ({
                    key: `${s.levelCode}|${s.direction}`,
                    label: s.label,
                })) }), _jsx(Table, { rowKey: "id", loading: isLoading, dataSource: treeData, columns: columns, expandable: { defaultExpandAllRows: true }, pagination: false, scroll: { x: 1300 }, size: "small" }), _jsx(NodeEditModal, { node: editing, onClose: () => setEditing(null), onSaved: () => {
                    setEditing(null);
                    refetch();
                } }), _jsx(NodeCreateModal, { taskId: taskId, parent: addingUnder, dimension: dimension, onClose: () => setAddingUnder(null), onSaved: () => {
                    setAddingUnder(null);
                    refreshAll();
                } }), _jsx(SqlExportModal, { open: sqlOpen, taskId: taskId, scope: activeScope
                    ? { levelCode: activeScope.levelCode, direction: activeScope.direction }
                    : undefined, onClose: () => setSqlOpen(false) }), _jsx(SummarizeModal, { task: summarizeOpen ? task : null, onClose: () => setSummarizeOpen(false), onStart: handleSummarize })] }));
}
// ==================================================================
// 编辑 / 新增弹窗
// ==================================================================
function NodeEditModal({ node, onClose, onSaved }) {
    const [title, setTitle] = useState('');
    useEffect(() => {
        setTitle(node?.title || '');
    }, [node]);
    if (!node)
        return null;
    return (_jsxs(Modal, { open: true, title: `编辑节点 ${node.nodeKey}`, onCancel: onClose, onOk: async () => {
            await updateNode(node.id, { title });
            message.success('已保存');
            onSaved();
        }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u6807\u9898\uFF08\u542B\u7F16\u7801\u540E\u7F00\uFF0C\u53EF\u76F4\u63A5\u4FEE\u6539\uFF09" }), _jsx(Input.TextArea, { rows: 3, value: title, onChange: (e) => setTitle(e.target.value) })] }));
}
function NodeCreateModal({ taskId, parent, dimension, onClose, onSaved }) {
    const [title, setTitle] = useState('');
    const [important, setImportant] = useState('X');
    useEffect(() => {
        setTitle('');
        setImportant('X');
    }, [parent]);
    if (!parent)
        return null;
    return (_jsx(Modal, { open: true, title: `在「${parent.title}」下新增${dimension === 'skill' ? '技能' : '理论'}考评点`, onCancel: onClose, onOk: async () => {
            if (!title.trim()) {
                message.warning('请填写考评点名称');
                return;
            }
            await createNode({
                taskId,
                parentId: parent.id,
                title: title.trim(),
                important,
                dimension,
            });
            message.success('已新增');
            onSaved();
        }, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: "\u8003\u8BC4\u70B9\u540D\u79F0" }), _jsx(Input.TextArea, { rows: 2, value: title, onChange: (e) => setTitle(e.target.value) })] }), _jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", children: "\u91CD\u8981\u7A0B\u5EA6" }), _jsx(Select, { value: important, style: { width: 120, marginLeft: 8 }, onChange: setImportant, options: ['X', 'Y', 'Z'].map((x) => ({ value: x, label: x })) })] })] }) }));
}
