import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMemo, useRef, useState } from 'react';
import { Alert, Button, Card, Dropdown, Form, Input, Modal, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { DownOutlined, DownloadOutlined, FolderOpenOutlined, ReloadOutlined, SearchOutlined, UploadOutlined, } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';
const ROSTER_SCOPE_MENU_ITEMS = [
    { key: 'certified', label: '已获证人员名册' },
    { key: 'all', label: '班级全部人员名册' },
];
/** 班级工种等级：5=五级 … 1=一级（与开班准备一致） */
const CLASS_LEVEL_LABELS = {
    5: '五级/初级工',
    4: '四级/中级工',
    3: '三级/高级工',
    2: '二级/技师',
    1: '一级/高级技师',
};
function formatClassLevel(level) {
    if (level == null || Number.isNaN(Number(level)))
        return '—';
    const n = Number(level);
    return CLASS_LEVEL_LABELS[n] ?? `${n}级`;
}
function classStatusTag(status) {
    if (status === 'done')
        return _jsx(Tag, { color: "success", children: "\u5DF2\u7ED3\u4E1A" });
    if (status === 'partial')
        return _jsx(Tag, { color: "warning", children: "\u90E8\u5206\u7ED3\u4E1A" });
    return _jsx(Tag, { children: "\u672A\u7ED3\u4E1A" });
}
async function downloadCertifiedRoster(opts) {
    try {
        if (opts.classIds != null && opts.classIds.length > 0) {
            await trainApi.exportCertifiedRosters(opts.classIds, opts.scope);
            return;
        }
        if (opts.classId != null) {
            await trainApi.exportCertifiedRoster(opts.classId, opts.classTitle, opts.scope);
            return;
        }
        message.warning('请选择班级');
    }
    catch (e) {
        message.error(e.message || '导出失败');
    }
}
/** 隐藏路由跳转（与 main.tsx 约定：pushState + popstate） */
function navigateTo(path) {
    window.history.pushState({}, '', path);
    window.dispatchEvent(new PopStateEvent('popstate'));
}
export function TrainingGraduationPage({ processCurrent = 7, pageTitle = '毕业典礼', pageSubtitle = '获证人员名册导出、证书文件夹回填；点击班级进入详情', }) {
    const queryClient = useQueryClient();
    const [searchForm] = Form.useForm();
    const [query, setQuery] = useState({ page: 1, pageSize: 20 });
    const [selectedClassKeys, setSelectedClassKeys] = useState([]);
    const [certImportOpen, setCertImportOpen] = useState(false);
    const [certFiles, setCertFiles] = useState([]);
    const [certPreview, setCertPreview] = useState(null);
    const [certPreviewing, setCertPreviewing] = useState(false);
    const [certCommitting, setCertCommitting] = useState(false);
    const [certPreviewFilter, setCertPreviewFilter] = useState('all');
    const [certPreviewPageSize, setCertPreviewPageSize] = useState(10);
    const folderInputRef = useRef(null);
    const fileInputRef = useRef(null);
    const { data, isLoading } = useQuery({
        queryKey: ['graduation-candidates', query],
        queryFn: () => trainApi.listGraduationCandidates({
            page: query.page,
            pageSize: query.pageSize,
            keyword: query.keyword,
            graduateStatus: query.graduateStatus,
            trainTitle: query.trainTitle,
            companyName: query.companyName,
            workTypeName: query.workTypeName,
        }),
    });
    const EMPTY_FILTER = '__EMPTY__';
    const toFilterOptions = (values) => (values ?? []).map((v) => ({
        text: v === EMPTY_FILTER ? '（空）' : v,
        value: v,
    }));
    const trainTitleFilters = useMemo(() => toFilterOptions(data?.facets?.trainTitles), [data?.facets?.trainTitles]);
    const companyFilters = useMemo(() => toFilterOptions(data?.facets?.companyNames), [data?.facets?.companyNames]);
    const workTypeFilters = useMemo(() => toFilterOptions(data?.facets?.workTypeNames), [data?.facets?.workTypeNames]);
    const handleSearch = () => {
        const values = searchForm.getFieldsValue();
        setQuery((prev) => ({
            ...prev,
            page: 1,
            keyword: values.keyword || undefined,
            graduateStatus: values.graduateStatus || undefined,
        }));
    };
    const readFilterValues = (raw) => {
        if (!Array.isArray(raw) || raw.length === 0)
            return undefined;
        return raw.map(String).filter(Boolean);
    };
    const selectedClassLabels = useMemo(() => {
        const list = (data?.list ?? []);
        const byId = new Map(list.map((r) => [String(r.id), String(r.title || r.id)]));
        return selectedClassKeys.map((k) => ({
            id: String(k),
            title: byId.get(String(k)) || String(k),
        }));
    }, [data?.list, selectedClassKeys]);
    const mergeCertFiles = (incoming) => {
        const next = Array.from(incoming).filter((f) => {
            const n = f.name.toLowerCase();
            return n.endsWith('.pdf') || n.endsWith('.zip');
        });
        if (next.length === 0) {
            message.warning('请选择 PDF 或 ZIP 文件');
            return;
        }
        setCertFiles((prev) => {
            const map = new Map(prev.map((f) => [`${f.name}:${f.size}:${f.lastModified}`, f]));
            next.forEach((f) => map.set(`${f.name}:${f.size}:${f.lastModified}`, f));
            return Array.from(map.values());
        });
        setCertPreview(null);
    };
    const openCertImport = () => {
        if (selectedClassKeys.length === 0) {
            message.warning('请先勾选要回填的班级');
            return;
        }
        setCertFiles([]);
        setCertPreview(null);
        setCertPreviewFilter('all');
        setCertImportOpen(true);
    };
    const runCertPreview = async () => {
        if (selectedClassKeys.length === 0) {
            message.warning('请先勾选班级');
            return;
        }
        if (certFiles.length === 0) {
            message.warning('请先添加证书文件夹或 PDF/ZIP');
            return;
        }
        setCertPreviewing(true);
        try {
            let res = await trainApi.previewGraduationCertImport(selectedClassKeys.map(String), certFiles);
            if (res.async && res.jobId) {
                message.info(res.message || '文件较多，后台解析中…');
                const jobId = res.jobId;
                const started = Date.now();
                while (Date.now() - started < 280000) {
                    await new Promise((r) => setTimeout(r, 800));
                    const job = await trainApi.getGraduationCertImportPreviewJob(jobId);
                    if (job.message) {
                        message.open({
                            key: 'cert-preview-job',
                            type: 'loading',
                            content: job.message,
                            duration: 0,
                        });
                    }
                    if (job.status === 'done') {
                        message.destroy('cert-preview-job');
                        res = job;
                        break;
                    }
                    if (job.status === 'failed') {
                        message.destroy('cert-preview-job');
                        throw new Error(job.error || job.message || '预览失败');
                    }
                }
                if (res.async && !res.previewId) {
                    throw new Error('预览超时，请减少单次上传数量后重试');
                }
            }
            setCertPreview(res);
            setCertPreviewFilter('all');
            message.success(`预览完成：可回填 ${res.matched ?? 0}，需处理 ${res.blocked ?? 0}，失败 ${res.failed ?? 0}`);
        }
        catch (e) {
            message.destroy('cert-preview-job');
            message.error(e.message || '预览失败');
        }
        finally {
            setCertPreviewing(false);
        }
    };
    const runCertCommit = async () => {
        if (!certPreview?.previewId) {
            message.warning('请先预览匹配');
            return;
        }
        if ((certPreview.matched ?? 0) <= 0) {
            message.warning('没有可回填的匹配项');
            return;
        }
        setCertCommitting(true);
        try {
            const res = await trainApi.commitGraduationCertImport(certPreview.previewId);
            message.success(`回填完成：新建 ${res.written}，更新 ${res.updated}，跳过 ${res.skipped}`);
            if (res.errors?.length) {
                Modal.warning({
                    title: '部分跳过',
                    content: res.errors.slice(0, 8).join('\n'),
                });
            }
            setCertImportOpen(false);
            setCertFiles([]);
            setCertPreview(null);
            void queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] });
            void queryClient.invalidateQueries({ queryKey: ['graduation-students'] });
        }
        catch (e) {
            message.error(e.message || '回填失败');
        }
        finally {
            setCertCommitting(false);
        }
    };
    const certStatusTag = (status) => {
        if (status === 'MATCHED')
            return _jsx(Tag, { color: "success", children: "\u53EF\u5199\u5165" });
        if (status === 'PARSE_FAILED')
            return _jsx(Tag, { color: "error", children: "\u89E3\u6790\u5931\u8D25" });
        if (status === 'NOT_IN_SCOPE')
            return _jsx(Tag, { color: "warning", children: "\u672A\u5728\u5DF2\u9009\u73ED" });
        if (status === 'DUP_IN_SELECTION')
            return _jsx(Tag, { color: "error", children: "\u8EAB\u4EFD\u8BC1\u51B2\u7A81" });
        if (status === 'DUP_CERT_NO' || status === 'EXISTING_CERT_CONFLICT') {
            return _jsx(Tag, { color: "error", children: "\u8BC1\u53F7\u51B2\u7A81" });
        }
        return _jsx(Tag, { children: status || '—' });
    };
    const certPreviewBucket = (item) => {
        if (item.canCommit)
            return 'matched';
        if (item.status === 'PARSE_FAILED')
            return 'failed';
        return 'blocked';
    };
    const filteredCertPreviewItems = useMemo(() => {
        const items = certPreview?.items ?? [];
        if (certPreviewFilter === 'all')
            return items;
        return items.filter((it) => certPreviewBucket(it) === certPreviewFilter);
    }, [certPreview?.items, certPreviewFilter]);
    const toggleCertPreviewFilter = (next) => {
        setCertPreviewFilter((prev) => (prev === next ? 'all' : next));
    };
    const columns = useMemo(() => [
        { title: 'ID', dataIndex: 'id', width: 80 },
        { title: '班级名称', dataIndex: 'title', width: 180, ellipsis: true },
        {
            title: '工种',
            dataIndex: 'workTypeName',
            width: 120,
            ellipsis: true,
            filters: workTypeFilters,
            filteredValue: query.workTypeName ?? null,
            filterSearch: true,
            onFilter: () => true,
            render: (v) => v || '—',
        },
        {
            title: '等级',
            dataIndex: 'workTypeLevel',
            width: 120,
            ellipsis: true,
            render: (v) => formatClassLevel(v),
        },
        {
            title: '培训项目',
            dataIndex: 'trainTitle',
            width: 180,
            ellipsis: true,
            filters: trainTitleFilters,
            filteredValue: query.trainTitle ?? null,
            filterSearch: true,
            onFilter: () => true,
            render: (v) => v || '—',
        },
        {
            title: '企业',
            dataIndex: 'companyName',
            width: 140,
            ellipsis: true,
            filters: companyFilters,
            filteredValue: query.companyName ?? null,
            filterSearch: true,
            onFilter: () => true,
            render: (v) => v || '—',
        },
        {
            title: '结业进度',
            width: 110,
            render: (_, r) => `${r.graduatedCount ?? 0}/${r.studentCount ?? 0}`,
        },
        {
            title: '证书回填',
            width: 120,
            render: (_, r) => {
                const backfill = Number(r.certBackfillCount ?? 0);
                const total = Number(r.studentCount ?? 0);
                if (backfill <= 0) {
                    return _jsx(Tag, { children: "\u672A\u56DE\u586B" });
                }
                return (_jsxs(Tag, { color: "success", children: ["\u5DF2\u56DE\u586B ", backfill, "/", total] }));
            },
        },
        {
            title: '状态',
            dataIndex: 'graduateStatus',
            width: 110,
            render: (v) => classStatusTag(v),
        },
        { title: '开始', dataIndex: 'dateStart', width: 110 },
        { title: '结束', dataIndex: 'dateEnd', width: 110 },
        {
            title: '操作',
            key: 'act',
            width: 160,
            fixed: 'right',
            render: (_, r) => (_jsx(Dropdown, { menu: {
                    items: ROSTER_SCOPE_MENU_ITEMS,
                    onClick: ({ key }) => void downloadCertifiedRoster({
                        classId: r.id,
                        classTitle: String(r.title || r.trainTitle || ''),
                        scope: key,
                    }),
                }, children: _jsxs(Button, { type: "link", size: "small", icon: _jsx(DownloadOutlined, {}), children: ["\u5BFC\u51FA\u4EBA\u5458\u540D\u518C ", _jsx(DownOutlined, {})] }) })),
        },
    ], [trainTitleFilters, companyFilters, workTypeFilters, query.trainTitle, query.companyName, query.workTypeName]);
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: pageTitle }), _jsx(Typography.Text, { type: "secondary", children: pageSubtitle })] }) }), _jsx(TrainingProcessSteps, { current: processCurrent }), _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u70B9\u51FB\u73ED\u7EA7\u8FDB\u5165\u8BE6\u60C5\u67E5\u770B\u5B66\u5458\uFF1B\u53EF\u52FE\u9009\u73ED\u7EA7\u4E0A\u4F20\u8BC1\u4E66\u6587\u4EF6\u5939\u56DE\u586B\u8BC1\u53F7\uFF0C\u6216\u5BFC\u51FA\u83B7\u8BC1\u4EBA\u5458\u540D\u518C\u3002" }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Form, { form: searchForm, layout: "inline", children: [_jsx(Form.Item, { name: "keyword", children: _jsx(Input, { placeholder: "\u641C\u7D22\u73ED\u7EA7\u540D\u79F0...", prefix: _jsx(SearchOutlined, {}), allowClear: true, onPressEnter: handleSearch, style: { width: 240 } }) }), _jsx(Form.Item, { name: "graduateStatus", children: _jsx(Select, { allowClear: true, placeholder: "\u7ED3\u4E1A\u72B6\u6001", style: { width: 140 }, options: [
                                    { value: 'pending', label: '未结业' },
                                    { value: 'partial', label: '部分结业' },
                                    { value: 'done', label: '已结业' },
                                ] }) }), _jsx(Form.Item, { children: _jsx(Button, { type: "primary", onClick: handleSearch, children: "\u67E5\u8BE2" }) })] }) }), _jsxs(Card, { size: "small", children: [_jsxs("div", { style: {
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            gap: 12,
                            marginBottom: 16,
                            flexWrap: 'wrap',
                        }, children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] }), children: "\u5237\u65B0" }), _jsx(Dropdown, { menu: {
                                            items: ROSTER_SCOPE_MENU_ITEMS,
                                            onClick: ({ key }) => void downloadCertifiedRoster({
                                                classIds: selectedClassKeys.map(String),
                                                scope: key,
                                            }),
                                        }, disabled: selectedClassKeys.length === 0, children: _jsxs(Button, { icon: _jsx(DownloadOutlined, {}), disabled: selectedClassKeys.length === 0, children: ["\u5BFC\u51FA\u5DF2\u9009\u540D\u518C ", _jsx(DownOutlined, {})] }) }), selectedClassKeys.length > 0 ? (_jsx(Button, { type: "link", onClick: () => setSelectedClassKeys([]), children: "\u6E05\u7A7A\u9009\u62E9" })) : null, _jsxs(Typography.Text, { type: "secondary", children: ["\u5DF2\u9009 ", selectedClassKeys.length, " \u4E2A\u73ED"] })] }), _jsx(Button, { icon: _jsx(FolderOpenOutlined, {}), disabled: selectedClassKeys.length === 0, title: selectedClassKeys.length === 0 ? '请先勾选要回填的班级' : undefined, onClick: openCertImport, children: "\u4E0A\u4F20\u8BC1\u4E66\u6587\u4EF6\u5939" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: isLoading, columns: columns, dataSource: (data?.list ?? []), scroll: { x: 1420 }, rowSelection: {
                            selectedRowKeys: selectedClassKeys,
                            onChange: (keys) => setSelectedClassKeys(keys),
                            preserveSelectedRowKeys: true,
                        }, onRow: (row) => ({
                            style: { cursor: 'pointer' },
                            onClick: (e) => {
                                const el = e.target;
                                if (el?.closest('.ant-checkbox-wrapper, .ant-checkbox, input, button, a, .ant-table-filter-trigger, .ant-dropdown')) {
                                    return;
                                }
                                const id = Number(row.id);
                                if (!Number.isFinite(id) || id <= 0)
                                    return;
                                navigateTo(`/training/graduation/classes/${id}`);
                            },
                        }), onChange: (pagination, filters) => {
                            const nextTrain = readFilterValues(filters.trainTitle);
                            const nextCompany = readFilterValues(filters.companyName);
                            const nextWorkType = readFilterValues(filters.workTypeName);
                            setQuery((prev) => {
                                const filtersChanged = JSON.stringify(nextTrain ?? []) !== JSON.stringify(prev.trainTitle ?? [])
                                    || JSON.stringify(nextCompany ?? []) !== JSON.stringify(prev.companyName ?? [])
                                    || JSON.stringify(nextWorkType ?? []) !== JSON.stringify(prev.workTypeName ?? []);
                                const nextPageSize = typeof pagination.pageSize === 'number' && pagination.pageSize > 0
                                    ? pagination.pageSize
                                    : prev.pageSize;
                                const sizeChanged = nextPageSize !== prev.pageSize;
                                return {
                                    ...prev,
                                    trainTitle: nextTrain,
                                    companyName: nextCompany,
                                    workTypeName: nextWorkType,
                                    pageSize: nextPageSize,
                                    page: filtersChanged || sizeChanged ? 1 : (pagination.current ?? prev.page),
                                };
                            });
                        }, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total: data?.total ?? 0,
                            showSizeChanger: true,
                            pageSizeOptions: ['10', '20', '50', '100'],
                            showTotal: (t) => `共 ${t} 条`,
                        } })] }), _jsxs(Modal, { title: "\u4E0A\u4F20\u8BC1\u4E66\u6587\u4EF6\u5939\u5E76\u56DE\u586B", open: certImportOpen, onCancel: () => {
                    if (certPreviewing || certCommitting)
                        return;
                    setCertImportOpen(false);
                }, width: 960, destroyOnHidden: true, footer: [
                    _jsx(Button, { onClick: () => setCertImportOpen(false), disabled: certPreviewing || certCommitting, children: "\u53D6\u6D88" }, "cancel"),
                    _jsx(Button, { loading: certPreviewing, disabled: certCommitting || certFiles.length === 0, onClick: () => void runCertPreview(), children: "\u9884\u89C8\u5339\u914D" }, "preview"),
                    _jsx(Button, { type: "primary", loading: certCommitting, disabled: certPreviewing || !certPreview || (certPreview.matched ?? 0) <= 0, onClick: () => void runCertCommit(), children: "\u786E\u8BA4\u56DE\u586B" }, "commit"),
                ], children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u6309\u8BC1\u4E66 PDF \u5185\u8EAB\u4EFD\u8BC1\u53F7\u5339\u914D\u5DF2\u9009\u73ED\u7EA7\u82B1\u540D\u518C\uFF1B\u59D3\u540D\u4E0D\u4F5C\u4E3B\u952E\u3002\u53EF\u591A\u6B21\u6DFB\u52A0\u6587\u4EF6\u5939\u6216 PDF/ZIP\u3002" }), _jsxs(Typography.Paragraph, { style: { marginBottom: 8 }, children: ["\u5DF2\u9009\u73ED\u7EA7\uFF08", selectedClassLabels.length, "\uFF09\uFF1A", selectedClassLabels.map((c) => (_jsxs(Tag, { style: { marginBottom: 4 }, children: [c.title, "\uFF08", c.id, "\uFF09"] }, c.id)))] }), _jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Button, { icon: _jsx(FolderOpenOutlined, {}), onClick: () => folderInputRef.current?.click(), children: "\u6DFB\u52A0\u8BC1\u4E66\u6587\u4EF6\u5939" }), _jsx(Button, { icon: _jsx(UploadOutlined, {}), onClick: () => fileInputRef.current?.click(), children: "\u6DFB\u52A0 PDF/ZIP" }), certFiles.length > 0 ? (_jsx(Button, { type: "link", onClick: () => {
                                    setCertFiles([]);
                                    setCertPreview(null);
                                    setCertPreviewFilter('all');
                                }, children: "\u6E05\u7A7A\u6587\u4EF6" })) : null, _jsxs(Typography.Text, { type: "secondary", children: ["\u5DF2\u9009\u6587\u4EF6 ", certFiles.length, " \u4E2A"] })] }), _jsx("input", { ref: folderInputRef, type: "file", multiple: true, style: { display: 'none' }, ...{ webkitdirectory: '', directory: '' }, onChange: (e) => {
                            if (e.target.files?.length)
                                mergeCertFiles(e.target.files);
                            e.target.value = '';
                        } }), _jsx("input", { ref: fileInputRef, type: "file", multiple: true, accept: ".pdf,.zip,application/pdf,application/zip", style: { display: 'none' }, onChange: (e) => {
                            if (e.target.files?.length)
                                mergeCertFiles(e.target.files);
                            e.target.value = '';
                        } }), certFiles.length > 0 ? (_jsxs(Typography.Paragraph, { type: "secondary", ellipsis: { rows: 2 }, style: { marginBottom: 12 }, children: [certFiles
                                .slice(0, 12)
                                .map((f) => f.name)
                                .join('、'), certFiles.length > 12 ? ` 等 ${certFiles.length} 个` : ''] })) : null, certPreview ? (_jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginBottom: 8 }, wrap: true, children: [_jsxs(Tag, { color: "success", style: {
                                            cursor: 'pointer',
                                            outline: certPreviewFilter === 'matched' ? '2px solid var(--ant-color-success)' : undefined,
                                            outlineOffset: 1,
                                        }, onClick: () => toggleCertPreviewFilter('matched'), children: ["\u53EF\u56DE\u586B ", certPreview.matched ?? 0] }), _jsxs(Tag, { color: "warning", style: {
                                            cursor: 'pointer',
                                            outline: certPreviewFilter === 'blocked' ? '2px solid var(--ant-color-warning)' : undefined,
                                            outlineOffset: 1,
                                        }, onClick: () => toggleCertPreviewFilter('blocked'), children: ["\u9700\u5904\u7406 ", certPreview.blocked ?? 0] }), _jsxs(Tag, { color: "error", style: {
                                            cursor: 'pointer',
                                            outline: certPreviewFilter === 'failed' ? '2px solid var(--ant-color-error)' : undefined,
                                            outlineOffset: 1,
                                        }, onClick: () => toggleCertPreviewFilter('failed'), children: ["\u5931\u8D25 ", certPreview.failed ?? 0] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u5171 ", certPreview.totalFiles, " \u4E2A\u6587\u4EF6", certPreview.ocrEnabled ? ' · OCR已启用' : ' · OCR未配置', certPreviewFilter !== 'all'
                                                ? ` · 当前筛选 ${filteredCertPreviewItems.length} 条（再点标签可取消）`
                                                : ''] })] }), _jsx(Table, { rowKey: (r) => `${r.fileName}-${r.idCard || ''}-${r.status}`, size: "small", pagination: {
                                    pageSize: certPreviewPageSize,
                                    showSizeChanger: true,
                                    pageSizeOptions: ['10', '20', '50', '100'],
                                    showTotal: (t) => `共 ${t} 条`,
                                    onChange: (_p, ps) => {
                                        if (typeof ps === 'number' && ps > 0) {
                                            setCertPreviewPageSize(ps);
                                        }
                                    },
                                }, scroll: { x: 1400, y: 320 }, dataSource: filteredCertPreviewItems, columns: [
                                    { title: '文件', dataIndex: 'fileName', ellipsis: true, width: 200 },
                                    { title: '证面姓名', dataIndex: 'parsedName', width: 88 },
                                    { title: '证书编号', dataIndex: 'certificateNo', width: 180, ellipsis: true },
                                    { title: '身份证', dataIndex: 'idCard', width: 150 },
                                    { title: '职业', dataIndex: 'occupation', width: 100, ellipsis: true },
                                    { title: '工种', dataIndex: 'workType', width: 100, ellipsis: true },
                                    { title: '等级', dataIndex: 'level', width: 110 },
                                    { title: '等级(归一)', dataIndex: 'levelNormalized', width: 110 },
                                    { title: '发证机构', dataIndex: 'issuingAuthority', width: 140, ellipsis: true },
                                    { title: '评价机构', dataIndex: 'evaluationOrg', width: 140, ellipsis: true },
                                    { title: '核发日期', dataIndex: 'issueDate', width: 110 },
                                    {
                                        title: '查询地址',
                                        dataIndex: 'queryUrlList',
                                        width: 160,
                                        ellipsis: true,
                                        render: (v) => (v?.length ? v.join('；') : '—'),
                                    },
                                    {
                                        title: '匹配学员',
                                        width: 150,
                                        render: (_, r) => r.realname
                                            ? `${r.realname}${r.classTitle ? ` · ${r.classTitle}` : ''}`
                                            : '—',
                                    },
                                    {
                                        title: '状态',
                                        width: 100,
                                        render: (_, r) => certStatusTag(r.status),
                                    },
                                    {
                                        title: '说明',
                                        width: 180,
                                        ellipsis: true,
                                        render: (_, r) => r.warning || r.message || '—',
                                    },
                                ] })] })) : null] })] }));
}
