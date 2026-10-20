import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * AI 课件生成管理 —— 方案 v1.2 §14
 * 六个视图合一(操作平铺,不用路由跳转):项目列表 / 创建课件弹窗 / 大纲确认(按章分组) /
 * 生成进度 / 页明细抽屉 / 导出问题清单。
 * 权限码:courseware:project:list/create/confirm/generate/retry/export
 */
import { useEffect, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Drawer, Input, Modal, Popconfirm, Progress, Select, Space, Table, Tag, Typography, Upload, message, } from 'antd';
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, DownloadOutlined, FilePptOutlined, PlusOutlined, ReloadOutlined, UploadOutlined, } from '@ant-design/icons';
import { hasPermission } from '../../directives/permission';
import { fetchPlatformFileBlob, uploadPlatformFile } from '../../api/files';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import { addOutlinePage, cancelCoursewareProject, confirmCoursewareOutline, createCoursewareProject, deleteCoursewareProject, deleteOutlinePage, exportCoursewareProject, getCoursewareOutline, getCoursewareProject, getCoursewareProgress, getCoursewareSlide, listCoursewareProjects, reorderOutline, retryCoursewareSlide, updateOutlinePage, } from './coursewareApi';
// ------------------------------------------------------------------
// 常量与工具
// ------------------------------------------------------------------
const STATUS_TEXT = {
    draft: { text: '草稿', color: 'default' },
    parsing: { text: '正在解析文档', color: 'processing' },
    digesting: { text: '正在整理素材', color: 'processing' },
    outline_generating: { text: '正在生成大纲', color: 'processing' },
    outline_ready: { text: '大纲待确认', color: 'gold' },
    generating: { text: '正在生成页面', color: 'processing' },
    ready: { text: '已完成', color: 'success' },
    partial: { text: '部分失败', color: 'warning' },
    failed: { text: '失败', color: 'error' },
    cancelled: { text: '已取消', color: 'default' },
};
const DENSITY_OPTIONS = [
    { value: 'concise', label: '精简(要点更少更短)' },
    { value: 'medium', label: '适中(默认)' },
    { value: 'detailed', label: '详细(要点更多更全)' },
];
const CONTENT_LAYOUT_OPTIONS = [
    { value: 'content_bullets', label: '要点页' },
    { value: 'content_table', label: '表格页' },
    { value: 'content_kpi', label: '数据页' },
];
const TERMINAL = new Set(['ready', 'partial', 'failed', 'cancelled']);
function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}
function issueTag(issue) {
    return issue.severity === 'error'
        ? _jsx(Tag, { color: "error", children: "\u9700\u5904\u7406" })
        : _jsx(Tag, { color: "warning", children: "\u5EFA\u8BAE\u68C0\u67E5" });
}
// ------------------------------------------------------------------
// 主页面
// ------------------------------------------------------------------
export default function AiCoursewarePage() {
    const queryClient = useQueryClient();
    const [view, setView] = useState({ kind: 'list' });
    const [createOpen, setCreateOpen] = useState(false);
    return (_jsxs("div", { style: { padding: 16 }, children: [view.kind === 'list' && (_jsx(ProjectListView, { onCreate: () => setCreateOpen(true), onOpen: (project) => setView(project.status === 'outline_ready'
                    || ['parsing', 'digesting', 'outline_generating'].includes(project.status)
                    ? { kind: 'outline', projectId: project.id }
                    : { kind: 'progress', projectId: project.id }), onProgress: (id) => setView({ kind: 'progress', projectId: id }) })), view.kind === 'outline' && (_jsx(OutlineView, { projectId: view.projectId, onBack: () => setView({ kind: 'list' }), onConfirmed: (id) => setView({ kind: 'progress', projectId: id }) })), view.kind === 'progress' && (_jsx(ProgressView, { projectId: view.projectId, onBack: () => setView({ kind: 'list' }) })), _jsx(CreateProjectModal, { open: createOpen, onClose: () => setCreateOpen(false), onCreated: (project) => {
                    setCreateOpen(false);
                    queryClient.invalidateQueries({ queryKey: ['courseware-projects'] });
                    setView({ kind: 'outline', projectId: project.id });
                } })] }));
}
// ------------------------------------------------------------------
// 视图 1:项目列表
// ------------------------------------------------------------------
function ProjectListView({ onCreate, onOpen, onProgress }) {
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const queryClient = useQueryClient();
    const { data, isLoading } = useQuery({
        queryKey: ['courseware-projects', page, pageSize],
        queryFn: () => listCoursewareProjects({ page, pageSize }),
    });
    const columns = [
        { title: '课件标题', dataIndex: 'title', ellipsis: true },
        { title: '受众', dataIndex: 'audience', width: 110 },
        {
            title: '状态', dataIndex: 'status', width: 130,
            render: (s, row) => (_jsxs(Space, { size: 4, children: [_jsx(Tag, { color: STATUS_TEXT[s]?.color, children: STATUS_TEXT[s]?.text ?? s }), (s === 'generating') && _jsxs("span", { children: [row.readyPages + row.failedPages, "/", row.totalPages] })] })),
        },
        {
            title: '进度', width: 120,
            render: (_, row) => row.totalPages > 0
                ? `${row.readyPages} 成 / ${row.failedPages} 败 / 共 ${row.totalPages} 页`
                : '—',
        },
        { title: '创建时间', dataIndex: 'createdAt', width: 170 },
        {
            title: '操作', width: 210,
            render: (_, row) => (_jsxs(Space, { size: 8, wrap: true, children: [row.status === 'outline_ready' && (_jsx(Button, { size: "small", type: "link", onClick: () => onOpen(row), children: "\u786E\u8BA4\u5927\u7EB2" })), ['parsing', 'digesting', 'outline_generating'].includes(row.status) && (_jsx(Button, { size: "small", type: "link", onClick: () => onOpen(row), children: "\u67E5\u770B\u8FDB\u5EA6" })), ['generating', 'ready', 'partial', 'failed', 'cancelled'].includes(row.status) && (_jsx(Button, { size: "small", type: "link", onClick: () => onProgress(row.id), children: row.status === 'generating' ? '生成进度' : '页面与导出' })), hasPermission('courseware:project:create') && row.status !== 'generating' && (_jsx(Popconfirm, { title: "\u5220\u9664\u8BE5\u8BFE\u4EF6\u9879\u76EE?", onConfirm: async () => {
                            await deleteCoursewareProject(row.id);
                            message.success('已删除');
                            queryClient.invalidateQueries({ queryKey: ['courseware-projects'] });
                        }, children: _jsx(Button, { size: "small", type: "link", danger: true, icon: _jsx(DeleteOutlined, {}) }) }))] })),
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginBottom: 12, width: '100%', justifyContent: 'space-between' }, children: [_jsx(Typography.Title, { level: 5, style: { margin: 0 }, children: "AI \u8BFE\u4EF6\u751F\u6210" }), hasPermission('courseware:project:create') && (_jsx(Button, { type: "primary", icon: _jsx(FilePptOutlined, {}), onClick: onCreate, "data-testid": "courseware-create-btn", children: "\u4E0A\u4F20\u6807\u51C6\u6587\u6863\u751F\u6210\u8BFE\u4EF6" }))] }), _jsx(Table, { rowKey: "id", size: "middle", loading: isLoading, dataSource: data?.items ?? [], columns: columns, pagination: {
                    current: page, pageSize, total: data?.total ?? 0,
                    showSizeChanger: true,
                    onChange: (p, ps) => {
                        if (ps !== pageSize) {
                            setPageSize(ps);
                            setPage(1);
                        }
                        else {
                            setPage(p);
                        }
                    },
                } })] }));
}
// ------------------------------------------------------------------
// 视图 2:创建课件弹窗
// ------------------------------------------------------------------
function CreateProjectModal({ open, onClose, onCreated }) {
    const [file, setFile] = useState(null);
    const [title, setTitle] = useState('');
    const [audience, setAudience] = useState('学员自学');
    const [density, setDensity] = useState('medium');
    const [submitting, setSubmitting] = useState(false);
    const submit = async () => {
        if (!file) {
            message.warning('请先选择标准文档(.docx)');
            return;
        }
        setSubmitting(true);
        try {
            const uploaded = await uploadPlatformFile(file, 'private');
            const project = await createCoursewareProject({
                sourceFileId: String(uploaded.id),
                title: title.trim() || undefined,
                audience: audience.trim() || undefined,
                contentDensity: density,
            });
            message.success('已创建,正在解析文档');
            onCreated(project);
        }
        finally {
            setSubmitting(false);
        }
    };
    return (_jsx(Modal, { title: "\u4E0A\u4F20\u6807\u51C6\u6587\u6863\u751F\u6210\u8BFE\u4EF6", open: open, onCancel: onClose, onOk: submit, confirmLoading: submitting, okText: "\u5F00\u59CB\u751F\u6210\u5927\u7EB2", destroyOnHidden: true, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsxs(Upload.Dragger, { accept: ".docx", maxCount: 1, beforeUpload: (f) => {
                        setFile(f);
                        if (!title.trim()) {
                            setTitle(f.name.replace(/\.docx$/i, ''));
                        }
                        return false;
                    }, onRemove: () => setFile(null), children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(UploadOutlined, {}) }), _jsx("p", { className: "ant-upload-text", children: "\u70B9\u51FB\u6216\u62D6\u62FD\u6807\u51C6\u6587\u6863(.docx)\u5230\u6B64\u5904" }), _jsx("p", { className: "ant-upload-hint", children: "\u53EA\u652F\u6301\u6587\u5B57\u7248 .docx;\u4E0A\u4F20\u540E\u5148\u6574\u7406\u7D20\u6750\u548C\u5927\u7EB2,\u4F60\u786E\u8BA4\u540E\u624D\u5F00\u59CB\u751F\u6210\u9875\u9762" })] }), _jsx(Input, { addonBefore: "\u8BFE\u4EF6\u6807\u9898", value: title, onChange: (e) => setTitle(e.target.value), maxLength: 120 }), _jsx(Input, { addonBefore: "\u53D7\u4F17", value: audience, onChange: (e) => setAudience(e.target.value), maxLength: 100, placeholder: "\u9ED8\u8BA4:\u5B66\u5458\u81EA\u5B66" }), _jsxs("div", { children: [_jsx("span", { style: { marginRight: 8 }, children: "\u6587\u5B57\u91CF" }), _jsx(Select, { value: density, onChange: setDensity, options: DENSITY_OPTIONS, style: { width: 240 } })] })] }) }));
}
// ------------------------------------------------------------------
// 视图 3:大纲确认(按章分组,平铺编辑)
// ------------------------------------------------------------------
function OutlineView({ projectId, onBack, onConfirmed }) {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    const [confirming, setConfirming] = useState(false);
    const [pages, setPages] = useState([]);
    // 项目状态轮询:大纲未就绪时每 3s 刷新,就绪后停止
    const { data: project } = useQuery({
        queryKey: ['courseware-project', projectId],
        queryFn: () => getCoursewareProject(projectId),
        refetchInterval: (q) => {
            if (!paneActive)
                return false;
            return q.state.data && ['outline_ready', ...TERMINAL].includes(q.state.data.status) ? false : 3000;
        },
    });
    const outlineQuery = useQuery({
        queryKey: ['courseware-outline', projectId],
        queryFn: () => getCoursewareOutline(projectId),
        enabled: project?.status === 'outline_ready',
    });
    useEffect(() => {
        if (outlineQuery.data) {
            setPages(outlineQuery.data);
        }
    }, [outlineQuery.data]);
    const busy = project && ['parsing', 'digesting', 'outline_generating'].includes(project.status);
    const ready = project?.status === 'outline_ready';
    const chapters = useMemo(() => {
        const groups = new Map();
        for (const p of pages) {
            const key = p.chapterNo || 0;
            if (!groups.has(key)) {
                groups.set(key, []);
            }
            groups.get(key).push(p);
        }
        return [...groups.entries()].sort((a, b) => a[0] - b[0]);
    }, [pages]);
    const patchLocal = (id, patch) => {
        setPages((prev) => prev.map((p) => (p.id === id ? { ...p, ...patch } : p)));
    };
    const saveField = async (id, patch) => {
        await updateOutlinePage(id, patch);
        message.success('已保存');
    };
    const move = async (index, dir) => {
        const target = index + dir;
        if (target < 0 || target >= pages.length) {
            return;
        }
        const next = [...pages];
        [next[index], next[target]] = [next[target], next[index]];
        setPages(next);
        await reorderOutline(projectId, next.map((p) => p.id));
        queryClient.invalidateQueries({ queryKey: ['courseware-outline', projectId] });
    };
    const confirm = async () => {
        setConfirming(true);
        try {
            await confirmCoursewareOutline(projectId);
            message.success('大纲已确认,开始生成页面');
            onConfirmed(projectId);
        }
        finally {
            setConfirming(false);
        }
    };
    if (busy) {
        return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsx(Button, { onClick: onBack, children: "\u8FD4\u56DE\u5217\u8868" }), _jsx(Alert, { type: "info", showIcon: true, message: STATUS_TEXT[project?.status ?? '']?.text ?? '处理中', description: "\u6B63\u5728\u89E3\u6790\u6587\u6863\u5E76\u6574\u7406\u7D20\u6750\u3001\u751F\u6210\u5927\u7EB2,\u901A\u5E38\u9700\u8981 1~2 \u5206\u949F,\u672C\u9875\u4F1A\u81EA\u52A8\u5237\u65B0\u3002" }), project?.status === 'failed' && (_jsx(Alert, { type: "error", message: "\u5904\u7406\u5931\u8D25", description: project.error }))] }));
    }
    if (project?.status === 'failed') {
        return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsx(Button, { onClick: onBack, children: "\u8FD4\u56DE\u5217\u8868" }), _jsx(Alert, { type: "error", message: "\u5927\u7EB2\u751F\u6210\u5931\u8D25", description: project.error })] }));
    }
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsxs(Space, { style: { justifyContent: 'space-between', width: '100%' }, children: [_jsxs(Space, { children: [_jsx(Button, { onClick: onBack, children: "\u8FD4\u56DE\u5217\u8868" }), _jsxs(Typography.Title, { level: 5, style: { margin: 0 }, children: ["\u786E\u8BA4\u8BFE\u4EF6\u5927\u7EB2(\u5171 ", pages.length, " \u9875)"] })] }), hasPermission('courseware:project:confirm') && ready && (_jsx(Button, { type: "primary", loading: confirming, onClick: confirm, "data-testid": "outline-confirm-btn", children: "\u786E\u8BA4\u5927\u7EB2,\u5F00\u59CB\u751F\u6210" }))] }), _jsx(Alert, { type: "info", showIcon: true, message: "\u6807\u9898\u3001\u8981\u70B9\u90FD\u53EF\u4EE5\u76F4\u63A5\u6539;\u53EF\u4EE5\u589E\u5220\u9875\u3001\u8C03\u6574\u987A\u5E8F\u3002\u786E\u8BA4\u540E\u624D\u5F00\u59CB\u751F\u6210\u9875\u9762(\u751F\u6210\u540E\u624D\u6D88\u8017\u5927\u6A21\u578B\u8C03\u7528)\u3002" }), chapters.map(([chapterNo, chapterPages]) => (_jsxs("div", { children: [_jsx(ChapterHeader, { chapterNo: chapterNo, chapterTitle: chapterPages[0]?.chapterTitle ?? '', pages: chapterPages, editable: ready && hasPermission('courseware:project:confirm'), onRename: async (name) => {
                            for (const p of chapterPages) {
                                patchLocal(p.id, { chapterTitle: name });
                            }
                            await Promise.all(chapterPages.map((p) => updateOutlinePage(p.id, { chapterTitle: name })));
                            message.success('章标题已保存');
                        }, onAdd: async () => {
                            const created = await addOutlinePage(projectId, {
                                chapterNo,
                                title: '新页面(请编辑标题)',
                                keyPoints: [],
                            });
                            message.success('已新增页面');
                            setPages((prev) => [...prev, created]
                                .sort((a, b) => a.position - b.position));
                            queryClient.invalidateQueries({ queryKey: ['courseware-outline', projectId] });
                        } }), chapterPages.map((p) => {
                        const globalIndex = pages.findIndex((x) => x.id === p.id);
                        return (_jsx(OutlinePageCard, { page: p, editable: ready && hasPermission('courseware:project:confirm'), onPatch: patchLocal, onSave: saveField, onMove: (dir) => move(globalIndex, dir), onDelete: async () => {
                                await deleteOutlinePage(p.id);
                                message.success('已删除');
                                setPages((prev) => prev.filter((x) => x.id !== p.id));
                                queryClient.invalidateQueries({ queryKey: ['courseware-outline', projectId] });
                            } }, p.id));
                    })] }, chapterNo)))] }));
}
function ChapterHeader({ chapterNo, chapterTitle, pages, editable, onRename, onAdd }) {
    const [editing, setEditing] = useState(false);
    const [name, setName] = useState(chapterTitle);
    useEffect(() => setName(chapterTitle), [chapterTitle]);
    if (chapterNo === 0) {
        return _jsx(Typography.Title, { level: 5, children: "\u5C01\u9762\u4E0E\u76EE\u5F55" });
    }
    return (_jsxs(Space, { style: { marginTop: 8 }, children: [editing ? (_jsx(Input, { value: name, onChange: (e) => setName(e.target.value), onBlur: async () => {
                    setEditing(false);
                    if (name.trim() && name !== chapterTitle) {
                        await onRename(name.trim());
                    }
                }, onPressEnter: () => document.activeElement instanceof HTMLElement
                    && document.activeElement.blur(), style: { width: 320 } })) : (_jsxs(Typography.Title, { level: 5, style: { margin: 0, cursor: editable ? 'pointer' : 'default' }, onClick: () => editable && setEditing(true), children: ["\u7B2C ", chapterNo, " \u7AE0:", chapterTitle || '(未命名)'] })), editable && (_jsx(Button, { size: "small", icon: _jsx(PlusOutlined, {}), onClick: onAdd, "data-testid": `chapter-add-${chapterNo}`, children: "\u672C\u7AE0\u52A0\u9875" }))] }));
}
function OutlinePageCard({ page, editable, onPatch, onSave, onMove, onDelete }) {
    const roleText = {
        cover: '封面', toc: '目录', section: '章节页', content: '内容页', review: '复习思考题',
    };
    return (_jsx("div", { "data-testid": `outline-card-${page.id}`, style: {
            border: '1px solid #f0f0f0', borderRadius: 8, padding: 12, marginTop: 8,
            background: '#fff',
        }, children: _jsxs(Space, { style: { width: '100%', justifyContent: 'space-between' }, align: "start", children: [_jsxs(Space, { direction: "vertical", style: { flex: 1, width: '100%' }, size: 4, children: [_jsxs(Space, { size: 8, children: [_jsx(Tag, { children: page.position }), _jsx(Tag, { color: "blue", children: roleText[page.pageRole] ?? page.pageRole }), page.fixed && _jsx(Tag, { children: "\u56FA\u5B9A\u9875" }), editable ? (_jsx(Input, { value: page.title, onChange: (e) => onPatch(page.id, { title: e.target.value }), onBlur: () => onSave(page.id, { title: page.title }), style: { width: 480 }, maxLength: 120, "data-testid": `outline-title-${page.id}` })) : (_jsx(Typography.Text, { strong: true, children: page.title }))] }), page.pageRole === 'content' && (_jsxs(Space, { size: 8, wrap: true, children: [_jsx("span", { style: { color: '#888' }, children: "\u8981\u70B9(\u6BCF\u884C\u4E00\u6761):" }), editable ? (_jsx(Input.TextArea, { value: (page.keyPoints ?? []).join('\n'), onChange: (e) => onPatch(page.id, {
                                        keyPoints: e.target.value.split('\n').filter((s) => s.trim()),
                                    }), onBlur: () => onSave(page.id, { keyPoints: page.keyPoints }), autoSize: { minRows: 2, maxRows: 6 }, style: { width: 560 } })) : (_jsx("ul", { style: { margin: 0, paddingInlineStart: 18 }, children: (page.keyPoints ?? []).map((k, i) => _jsx("li", { children: k }, i)) })), editable && (_jsx(Select, { value: page.layoutId, onChange: (v) => {
                                        onPatch(page.id, { layoutId: v });
                                        onSave(page.id, { layoutId: v });
                                    }, options: CONTENT_LAYOUT_OPTIONS, style: { width: 120 } }))] })), page.pageRole !== 'content' && (page.keyPoints?.length ?? 0) > 0 && (_jsx("ul", { style: { margin: 0, paddingInlineStart: 18 }, children: page.keyPoints.map((k, i) => _jsx("li", { children: k }, i)) }))] }), editable && (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(ArrowUpOutlined, {}), onClick: () => onMove(-1) }), _jsx(Button, { size: "small", icon: _jsx(ArrowDownOutlined, {}), onClick: () => onMove(1) }), !page.fixed && (_jsx(Popconfirm, { title: "\u5220\u9664\u8BE5\u9875?", onConfirm: onDelete, children: _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), "data-testid": `outline-delete-${page.id}` }) }))] }))] }) }));
}
// ------------------------------------------------------------------
// 视图 4:生成进度 + 页列表 + 导出
// ------------------------------------------------------------------
function ProgressView({ projectId, onBack }) {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    const [detailId, setDetailId] = useState(null);
    const [exportIssues, setExportIssues] = useState(null);
    const [exporting, setExporting] = useState(false);
    const { data: progress } = useQuery({
        queryKey: ['courseware-progress', projectId],
        queryFn: () => getCoursewareProgress(projectId),
        // 终态停止轮询(方案 §14)；标签不可见时暂停（§3.6）
        refetchInterval: (q) => {
            if (!paneActive)
                return false;
            return q.state.data && TERMINAL.has(q.state.data.status) ? false : 3000;
        },
    });
    const { data: project } = useQuery({
        queryKey: ['courseware-project', projectId],
        queryFn: () => getCoursewareProject(projectId),
    });
    const exportPptx = async () => {
        setExporting(true);
        try {
            const result = await exportCoursewareProject(projectId);
            if (result.blocked) {
                setExportIssues(result.issues);
                return;
            }
            if (result.warnings?.length) {
                Modal.info({
                    title: '导出成功,以下建议可在 PowerPoint 中检查后调整',
                    content: (_jsx("ul", { style: { paddingInlineStart: 18 }, children: result.warnings.slice(0, 10).map((w, i) => _jsx("li", { children: w.message }, i)) })),
                });
            }
            if (result.fileId) {
                const blob = await fetchPlatformFileBlob(String(result.fileId));
                downloadBlob(blob, `${project?.title ?? '课件'}.pptx`);
                message.success('PPTX 已开始下载');
            }
        }
        finally {
            setExporting(false);
        }
    };
    const slideStatusTag = (s) => {
        const map = {
            pending: { text: '排队中', color: 'default' },
            generating: { text: '生成中', color: 'processing' },
            ready: { text: '已完成', color: 'success' },
            failed: { text: '失败', color: 'error' },
            cancelled: { text: '已取消', color: 'default' },
        };
        return _jsx(Tag, { color: map[s]?.color, children: map[s]?.text ?? s });
    };
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsxs(Space, { style: { width: '100%', justifyContent: 'space-between' }, children: [_jsxs(Space, { children: [_jsx(Button, { onClick: onBack, children: "\u8FD4\u56DE\u5217\u8868" }), _jsxs(Typography.Title, { level: 5, style: { margin: 0 }, children: [project?.title ?? '课件', " \u2014 \u9875\u9762\u4E0E\u5BFC\u51FA"] })] }), _jsxs(Space, { children: [progress?.status === 'generating'
                                && hasPermission('courseware:project:generate') && (_jsx(Popconfirm, { title: "\u53D6\u6D88\u751F\u6210?\u5DF2\u5B8C\u6210\u7684\u9875\u9762\u4F1A\u4FDD\u7559", onConfirm: async () => {
                                    await cancelCoursewareProject(projectId);
                                    message.success('已取消');
                                    queryClient.invalidateQueries({ queryKey: ['courseware-progress', projectId] });
                                }, children: _jsx(Button, { danger: true, children: "\u53D6\u6D88\u751F\u6210" }) })), ['ready', 'partial', 'cancelled'].includes(progress?.status ?? '')
                                && hasPermission('courseware:project:export') && (_jsx(Button, { type: "primary", icon: _jsx(DownloadOutlined, {}), loading: exporting, onClick: exportPptx, "data-testid": "courseware-export-btn", children: "\u5BFC\u51FA PPTX" }))] })] }), progress && (_jsx(Progress, { percent: progress.totalPages > 0
                    ? Math.round(((progress.readyPages + progress.failedPages) / progress.totalPages) * 100)
                    : 0, status: progress.status === 'failed' ? 'exception' : undefined, format: () => `${progress.readyPages + progress.failedPages}/${progress.totalPages} 页` })), progress?.error && _jsx(Alert, { type: "warning", showIcon: true, message: progress.error }), _jsx(Table, { rowKey: "id", size: "small", dataSource: progress?.slides ?? [], pagination: false, columns: [
                    { title: '页序', dataIndex: 'position', width: 70 },
                    { title: '标题', dataIndex: 'title', ellipsis: true },
                    {
                        title: '状态', dataIndex: 'status', width: 100,
                        render: (s) => slideStatusTag(s),
                    },
                    { title: '说明', dataIndex: 'error', ellipsis: true,
                        render: (e) => e ?? '—' },
                    {
                        title: '操作', width: 160,
                        render: (_, row) => (_jsxs(Space, { size: 4, children: [row.status === 'ready' && (_jsx(Button, { size: "small", type: "link", onClick: () => setDetailId(row.id), "data-testid": `slide-view-${row.id}`, children: "\u67E5\u770B\u5185\u5BB9" })), row.status === 'failed' && hasPermission('courseware:project:retry') && (_jsx(Button, { size: "small", type: "link", icon: _jsx(ReloadOutlined, {}), "data-testid": `slide-retry-${row.id}`, onClick: async () => {
                                        await retryCoursewareSlide(row.id);
                                        message.success('已重新生成该页,正在生成中');
                                        // 重试异步执行(约 10 秒),项目已是终态轮询已停:
                                        // 先立即刷新看到"生成中",再轮询到该页终态自动更新列表(用户口径 2026-09-22)
                                        await queryClient.refetchQueries({
                                            queryKey: ['courseware-progress', projectId],
                                        });
                                        const deadline = Date.now() + 120_000;
                                        while (Date.now() < deadline) {
                                            await new Promise((r) => setTimeout(r, 3000));
                                            const fresh = await queryClient.fetchQuery({
                                                queryKey: ['courseware-progress', projectId],
                                                queryFn: () => getCoursewareProgress(projectId),
                                                staleTime: 0,
                                            });
                                            const target = fresh.slides.find((x) => x.id === row.id);
                                            if (!target || target.status === 'ready' || target.status === 'failed'
                                                || target.status === 'cancelled') {
                                                await queryClient.refetchQueries({
                                                    queryKey: ['courseware-progress', projectId],
                                                });
                                                break;
                                            }
                                        }
                                    }, children: "\u91CD\u8BD5" }))] })),
                    },
                ] }), _jsx(SlideDetailDrawer, { slideId: detailId, onClose: () => setDetailId(null) }), _jsxs(Modal, { title: "\u6709\u9875\u9762\u5B58\u5728\u95EE\u9898,\u6682\u4E0D\u80FD\u5BFC\u51FA", open: exportIssues !== null, onCancel: () => setExportIssues(null), footer: _jsx(Button, { type: "primary", onClick: () => setExportIssues(null), children: "\u77E5\u9053\u4E86" }), children: [_jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 8 }, message: "\u8BF7\u5148\u5904\u7406\u4E0B\u5217\u9875\u9762\u7684\u95EE\u9898(\u53EF\u5728\u9875\u5217\u8868\u91CD\u8BD5\u8BE5\u9875)" }), _jsx("ul", { style: { paddingInlineStart: 18 }, children: (exportIssues ?? []).map((issue, i) => (_jsx("li", { children: _jsxs(Space, { size: 4, children: [issueTag(issue), _jsx("span", { children: issue.message })] }) }, i))) })] })] }));
}
// ------------------------------------------------------------------
// 视图 5/6:页明细抽屉(blocks 文本化呈现 + issues + 学习提示)
// ------------------------------------------------------------------
function SlideDetailDrawer({ slideId, onClose }) {
    const { data: slide } = useQuery({
        queryKey: ['courseware-slide', slideId],
        queryFn: () => getCoursewareSlide(slideId),
        enabled: slideId !== null,
    });
    return (_jsxs(Drawer, { title: slide ? `第 ${slide.position} 页:${slide.title ?? ''}` : '页面内容', open: slideId !== null, onClose: onClose, width: 640, children: [slide?.issues && slide.issues.length > 0 && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u672C\u9875\u6709\u5EFA\u8BAE\u68C0\u67E5\u7684\u5730\u65B9", description: (_jsx("ul", { style: { paddingInlineStart: 18, marginBottom: 0 }, children: slide.issues.map((issue, i) => (_jsx("li", { children: issue.message }, i))) })) })), (slide?.blocks ?? []).map((block, i) => (_jsxs("div", { style: { marginBottom: 12 }, children: [block.type === 'text' && (_jsx(Typography.Title, { level: 5, children: block.text })), block.type === 'bullets' && (_jsx("ul", { style: { paddingInlineStart: 18 }, children: (block.items ?? []).map((item, j) => _jsx("li", { children: item }, j)) })), block.type === 'table' && (_jsx(Table, { size: "small", pagination: false, rowKey: (r) => Object.values(r).join('|'), columns: (block.header ?? []).map((h, j) => ({
                            title: h, dataIndex: String(j),
                        })), dataSource: (block.rows ?? []).map((row) => Object.fromEntries(row.map((cell, j) => [String(j), cell]))) })), block.type === 'kpi' && (_jsxs(Tag, { color: "blue", style: { fontSize: 14, padding: '6px 12px' }, children: [block.value, " \u00B7 ", block.label, block.note ? `(${block.note})` : ''] }))] }, i))), slide?.speakerNotes && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5B66\u4E60\u63D0\u793A", description: slide.speakerNotes }))] }));
}
