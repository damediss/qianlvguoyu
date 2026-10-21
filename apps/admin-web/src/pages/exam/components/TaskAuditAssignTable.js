import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
// 管理端 3 页面共享的"题库审核指派"表格组件
// 复用 DevTaskDetailPage 的 UI 风格(antd Table + Modal + Drawer + Tag)
// 根据 taskType 切换 API 调用,根据 permissionPrefix 切换权限点
import { DeleteOutlined, DownloadOutlined, ExportOutlined, EyeOutlined, ReloadOutlined, TableOutlined, UploadOutlined, } from '@ant-design/icons';
import { Button, Drawer, Descriptions, Form, Modal, Select, Space, Table, Tag, TreeSelect, Typography, Upload, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { pageTaskExamAudits, getTaskExamAuditDetails, deleteTaskExamAudit, assignTaskExamAuditExpert, getTaskExamAuditDaily, exportTaskExamAudit, pageTaskExamshicaoAudits, getTaskExamshicaoAuditDetails, deleteTaskExamshicaoAudit, assignTaskExamshicaoAuditExpert, getTaskExamshicaoAuditDaily, exportTaskExamshicaoAudit, pageTaskExamsubAudits, getTaskExamsubAuditDetails, deleteTaskExamsubAudit, assignTaskExamsubAuditExpert, getTaskExamsubAuditDaily, exportTaskExamsubAudit, importTaskExam, importTaskExamshicao, importTaskExamsub, getPublicData, } from '../../../api/exam';
import { useAuthStore } from '../../../stores/authStore';
import { STATUS_MAP, ROLE_MAP, LEVEL_OPTS } from './constants';
import { parseContentDispositionFilename } from '../../../utils/download';
// ===========================================================================
// 评分子表(pingfen JSON 数组)渲染
// 对应 PHP /yuntiku/task-examshicao-list 实操题"评分"列展开样式
// 6 列: 序号 / 要素 / 配分 / 评分标准 / 最大扣分 / 步长
// 缺字段填空(向后兼容 2 列简版)
// ===========================================================================
const PINGFEN_SUBHEADERS = ['序号', '要素', '配分', '评分标准', '最大扣分', '步长'];
const PINGFEN_SUBWIDTHS = ['8%', '20%', '10%', '40%', '12%', '10%'];
const PINGFEN_TABLE_STYLE = {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: 12,
};
const PINGFEN_TH_STYLE = {
    border: '1px solid #d9d9d9',
    padding: '4px 8px',
    background: '#fafafa',
    fontWeight: 500,
    textAlign: 'left',
};
const PINGFEN_TD_STYLE = {
    border: '1px solid #d9d9d9',
    padding: '4px 8px',
    verticalAlign: 'top',
};
function safeParsePingfen(v) {
    if (!v)
        return null;
    let raw = v;
    if (typeof v === 'string') {
        try {
            raw = JSON.parse(v);
        }
        catch {
            return null;
        }
    }
    if (!Array.isArray(raw))
        return null;
    return raw;
}
function PingfenSubTable({ value }) {
    const items = safeParsePingfen(value);
    if (!items || items.length === 0) {
        return _jsx("span", { style: { color: '#999' }, children: "-" });
    }
    return (_jsxs("table", { style: PINGFEN_TABLE_STYLE, children: [_jsx("thead", { children: _jsx("tr", { children: PINGFEN_SUBHEADERS.map((h, i) => (_jsx("th", { style: { ...PINGFEN_TH_STYLE, width: PINGFEN_SUBWIDTHS[i] }, children: h }, h))) }) }), _jsx("tbody", { children: items.map((it, i) => (_jsxs("tr", { children: [_jsx("td", { style: PINGFEN_TD_STYLE, children: i + 1 }), _jsx("td", { style: PINGFEN_TD_STYLE, children: it.yaosu ?? '-' }), _jsx("td", { style: PINGFEN_TD_STYLE, children: it.peifen ?? '-' }), _jsx("td", { style: PINGFEN_TD_STYLE, children: it.biaozhun ?? '-' }), _jsx("td", { style: PINGFEN_TD_STYLE, children: it.koufen ?? '-' }), _jsx("td", { style: PINGFEN_TD_STYLE, children: it.buchang ?? '-' })] }, i))) })] }));
}
function renderPingfenCell(v) {
    if (!v)
        return '-';
    return _jsx(PingfenSubTable, { value: v });
}
// 模板下载 URL(沿用 PHP 老 CDN 路径,3 题型各自一份)
const TEMPLATE_URLS = {
    exam: 'https://cdn.app.gjjd168.com/template/题库审核导入模板_理论.xlsx',
    examshicao: 'https://cdn.app.gjjd168.com/template/题库审核导入模板_实操.xlsx',
    examsub: 'https://cdn.app.gjjd168.com/template/题库审核导入模板_主观.xlsx',
};
// API 函数映射表(按 taskType 分发)
const API_MAP = {
    exam: {
        page: pageTaskExamAudits,
        details: getTaskExamAuditDetails,
        del: deleteTaskExamAudit,
        assign: assignTaskExamAuditExpert,
        daily: getTaskExamAuditDaily,
        export: exportTaskExamAudit,
        import: importTaskExam,
        queryKey: 'task-exam-audits',
    },
    examshicao: {
        page: pageTaskExamshicaoAudits,
        details: getTaskExamshicaoAuditDetails,
        del: deleteTaskExamshicaoAudit,
        assign: assignTaskExamshicaoAuditExpert,
        daily: getTaskExamshicaoAuditDaily,
        export: exportTaskExamshicaoAudit,
        import: importTaskExamshicao,
        queryKey: 'task-examshicao-audits',
    },
    examsub: {
        page: pageTaskExamsubAudits,
        details: getTaskExamsubAuditDetails,
        del: deleteTaskExamsubAudit,
        assign: assignTaskExamsubAuditExpert,
        daily: getTaskExamsubAuditDaily,
        export: exportTaskExamsubAudit,
        import: importTaskExamsub,
        queryKey: 'task-examsub-audits',
    },
};
export function TaskAuditAssignTable({ taskType, permissionPrefix, title, detailColumnLabels, }) {
    const api = API_MAP[taskType];
    const qc = useQueryClient();
    const hasPerm = useAuthStore((s) => s.hasPermission);
    // 分页
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    // 当前行
    const [currentRow, setCurrentRow] = useState(null);
    // 详情 Drawer
    const [detailOpen, setDetailOpen] = useState(false);
    const [detailData, setDetailData] = useState([]);
    const [detailLoading, setDetailLoading] = useState(false);
    // 指派专家 Modal
    const [assignOpen, setAssignOpen] = useState(false);
    const [assignRole, setAssignRole] = useState(0);
    const [selectedExpertId, setSelectedExpertId] = useState();
    // 日统计 Modal
    const [dailyOpen, setDailyOpen] = useState(false);
    const [dailyData, setDailyData] = useState([]);
    const [dailyLoading, setDailyLoading] = useState(false);
    // 导入任务 Modal
    const [importOpen, setImportOpen] = useState(false);
    const [importWorkTypeId, setImportWorkTypeId] = useState(null);
    const [importLevel, setImportLevel] = useState(null);
    const [importFileList, setImportFileList] = useState([]);
    // 文件预览 Modal
    const [fileOpen, setFileOpen] = useState(false);
    const [fileUrl, setFileUrl] = useState('');
    const [fileDownUrl, setFileDownUrl] = useState('');
    // 公共数据(工种 + 专家)
    const publicQuery = useQuery({
        queryKey: ['public-data'],
        queryFn: getPublicData,
        staleTime: 300_000,
    });
    // 任务列表
    const listQuery = useQuery({
        queryKey: [api.queryKey, page, pageSize],
        queryFn: () => api.page({ page, pageSize }),
    });
    // 删除
    const delMut = useMutation({
        mutationFn: (id) => api.del(id),
        onSuccess: () => {
            message.success('删除成功');
            qc.invalidateQueries({ queryKey: [api.queryKey] });
        },
    });
    // 指派
    const assignMut = useMutation({
        mutationFn: (args) => {
            const payload = { taskId: args.taskId, expertId: args.expertId, role: args.role };
            return api.assign(args.taskId, payload);
        },
        onSuccess: () => {
            message.success('指派成功');
            setAssignOpen(false);
            qc.invalidateQueries({ queryKey: [api.queryKey] });
        },
    });
    // 导入
    const importMut = useMutation({
        mutationFn: async () => {
            if (!importWorkTypeId || !importLevel || importFileList.length === 0) {
                throw new Error('请完整填写工种、等级并选择文件');
            }
            const fd = new FormData();
            fd.append('file', importFileList[0].originFileObj);
            fd.append('workTypeId', String(importWorkTypeId));
            fd.append('level', String(importLevel));
            return api.import(fd);
        },
        onSuccess: (count) => {
            message.success(`成功导入 ${count} 条数据`);
            handleImportClose();
            qc.invalidateQueries({ queryKey: [api.queryKey] });
        },
        onError: (e) => {
            message.error(e.message || '导入失败');
        },
    });
    const handleImportOpen = () => {
        setImportWorkTypeId(null);
        setImportLevel(null);
        setImportFileList([]);
        setImportOpen(true);
    };
    const handleImportClose = () => {
        setImportOpen(false);
        setImportWorkTypeId(null);
        setImportLevel(null);
        setImportFileList([]);
    };
    // 文件预览
    const handleShowFile = (row) => {
        setCurrentRow(row);
        setFileUrl('https://view.officeapps.live.com/op/view.aspx?src=' + encodeURIComponent(row.fileUrl));
        setFileDownUrl(row.fileUrl);
        setFileOpen(true);
    };
    // 导出
    const handleExport = async (row) => {
        try {
            const resp = await api.export(row.id);
            const blob = resp.data;
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = parseContentDispositionFilename(resp) ?? `${row.id}_${row.workTypeCn}_${row.levelCn}_${taskType}.xlsx`;
            a.click();
            URL.revokeObjectURL(url);
        }
        catch {
            message.error('导出失败');
        }
    };
    // 显示详情
    const handleShowDetails = async (row) => {
        setCurrentRow(row);
        setDetailOpen(true);
        setDetailLoading(true);
        try {
            const data = await api.details(row.id);
            setDetailData(data);
        }
        catch {
            message.error('获取明细失败');
        }
        finally {
            setDetailLoading(false);
        }
    };
    // 指派
    const handleAssign = (row, role) => {
        setCurrentRow(row);
        setAssignRole(role);
        setAssignOpen(true);
        setSelectedExpertId(undefined);
    };
    // 日统计
    const handleShowDaily = async (row, role) => {
        setCurrentRow(row);
        setDailyOpen(true);
        setDailyLoading(true);
        try {
            const data = await api.daily(row.id, role);
            setDailyData(data);
        }
        catch {
            message.error('获取每日审核量失败');
        }
        finally {
            setDailyLoading(false);
        }
    };
    // 删除
    const handleDelete = (row) => {
        const isInProgress = row.status !== 0;
        const statusText = row.statusCn ?? '当前';
        Modal.confirm({
            title: '确认删除',
            content: isInProgress
                ? `该任务当前状态为「${statusText}」,删除将级联删除明细和已有的审核/专家分配记录(专家已提交的工作将丢失),不可恢复。确定删除吗?`
                : '确定要删除该任务吗?关联的明细和审核记录也将被删除。',
            okText: '删除',
            okButtonProps: { danger: true },
            onOk: () => delMut.mutate(row.id),
        });
    };
    // 列表项
    const records = listQuery.data?.items ?? [];
    const total = listQuery.data?.total ?? 0;
    // 详情列: 按 taskType 三套(对应 PHP /yuntiku/task-* -list 各题型)
    //   exam        - ID / 题型 / 题干 / A~H / 答案 / 重点
    //   examshicao  - ID / 考评点 / 题干 / 考场准备 / 考生准备 / 评分(子表)
    //   examsub     - ID / 知识点 / 题干 / 参考答案 / 是否计算题
    const detailColumns = (() => {
        if (taskType === 'exam') {
            return [
                { title: 'ID', dataIndex: 'id', width: 60 },
                { title: detailColumnLabels.examType ?? '题型', dataIndex: 'examType', width: 100 },
                {
                    title: detailColumnLabels.examContent ?? '题干',
                    dataIndex: 'examContent',
                    width: 240,
                    render: (v, r) => v || r.content || '-',
                },
                { title: detailColumnLabels.examA ?? 'A', dataIndex: 'examA', width: 100, render: (v) => v || '-' },
                { title: detailColumnLabels.examB ?? 'B', dataIndex: 'examB', width: 100, render: (v) => v || '-' },
                { title: detailColumnLabels.examC ?? 'C', dataIndex: 'examC', width: 100, render: (v) => v || '-' },
                { title: detailColumnLabels.examD ?? 'D', dataIndex: 'examD', width: 100, render: (v) => v || '-' },
                { title: detailColumnLabels.examE ?? 'E', dataIndex: 'examE', width: 100, render: (v) => v || '-' },
                { title: detailColumnLabels.examF ?? 'F', dataIndex: 'examF', width: 100, render: (v) => v || '-' },
                { title: detailColumnLabels.examG ?? 'G', dataIndex: 'examG', width: 100, render: (v) => v || '-' },
                { title: detailColumnLabels.examH ?? 'H', dataIndex: 'examH', width: 100, render: (v) => v || '-' },
                {
                    title: detailColumnLabels.examAnswer ?? '答案',
                    dataIndex: 'examAnswer',
                    width: 120,
                    render: (v, r) => v || r.answer || '-',
                },
                { title: detailColumnLabels.isblod ?? '重点', dataIndex: 'isblod', width: 80, render: (v) => (v ? _jsx(Tag, { color: "red", children: "\u662F" }) : '否') },
            ];
        }
        if (taskType === 'examshicao') {
            return [
                { title: 'ID', dataIndex: 'id', width: 60 },
                { title: detailColumnLabels.jianding ?? '考评点', dataIndex: 'jianding', width: 100, render: (v) => v || '-' },
                {
                    title: detailColumnLabels.content ?? detailColumnLabels.examContent ?? '题干',
                    dataIndex: 'content',
                    width: 280,
                    render: (v) => v || '-',
                },
                {
                    title: detailColumnLabels.kaochang ?? '考场准备',
                    dataIndex: 'kaochang',
                    width: 200,
                    render: (v) => v || '-',
                },
                {
                    title: detailColumnLabels.kaosheng ?? '考生准备',
                    dataIndex: 'kaosheng',
                    width: 200,
                    render: (v) => v || '-',
                },
                {
                    title: detailColumnLabels.pingfen ?? '评分',
                    dataIndex: 'pingfen',
                    width: 600,
                    render: renderPingfenCell,
                },
            ];
        }
        // examsub
        return [
            { title: 'ID', dataIndex: 'id', width: 60 },
            { title: detailColumnLabels.zhishidian ?? '知识点', dataIndex: 'zhishidian', width: 120, render: (v) => v || '-' },
            {
                title: detailColumnLabels.content ?? detailColumnLabels.examContent ?? '题干',
                dataIndex: 'content',
                width: 300,
                render: (v) => v || '-',
            },
            {
                title: detailColumnLabels.answer ?? detailColumnLabels.examAnswer ?? '参考答案',
                dataIndex: 'answer',
                width: 300,
                render: (v) => v || '-',
            },
            { title: detailColumnLabels.nanyicd ?? '难易程度', dataIndex: 'nanyicd', width: 100, render: (v) => v || '-' },
            { title: detailColumnLabels.isJisuan ?? '是否计算题', dataIndex: 'isJisuan', width: 100, render: (v) => (v ? _jsx(Tag, { color: "blue", children: "\u662F" }) : '否') },
        ];
    })();
    // 详情行渲染:显示初/终审 child — 按 taskType 各自一套列
    const expandedDetailColumns = (() => {
        const base = [
            { title: '审核阶段', dataIndex: 'id', width: 100 },
            {
                title: '审核专家',
                dataIndex: 'expertName',
                width: 120,
                render: (v) => v || '-',
            },
        ];
        if (taskType === 'exam') {
            return [
                ...base,
                { title: '审核题干', dataIndex: 'examContent', width: 300, render: (v) => v || '-' },
                { title: '审核答案', dataIndex: 'examAnswer', width: 120, render: (v) => v || '-' },
                { title: '提交时间', dataIndex: 'addtimeCn', width: 160, render: (v) => v || '-' },
            ];
        }
        if (taskType === 'examshicao') {
            return [
                ...base,
                { title: '审核内容', dataIndex: 'content', width: 200, render: (v) => v || '-' },
                { title: '评分标准', dataIndex: 'pingfen', width: 300, render: renderPingfenCell },
                { title: '考场准备', dataIndex: 'kaochang', width: 200, render: (v) => v || '-' },
                { title: '考生准备', dataIndex: 'kaosheng', width: 200, render: (v) => v || '-' },
                { title: '提交时间', dataIndex: 'addtimeCn', width: 160, render: (v) => v || '-' },
            ];
        }
        // examsub
        return [
            ...base,
            { title: '审核内容', dataIndex: 'content', width: 300, render: (v) => v || '-' },
            { title: '审核答案', dataIndex: 'answer', width: 300, render: (v) => v || '-' },
            { title: '提交时间', dataIndex: 'addtimeCn', width: 160, render: (v) => v || '-' },
        ];
    })();
    // 列表列
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 70 },
        {
            title: '工种',
            key: 'workType',
            width: 220,
            render: (_, r) => (_jsxs(Tag, { children: [r.workTypeCn, "(", r.levelCn, ")"] })),
        },
        { title: '题量', dataIndex: 'nums', width: 80 },
        {
            title: '审核专家',
            key: 'experts',
            width: 280,
            render: (_, r) => (_jsxs(Space, { direction: "vertical", size: 4, children: [_jsx(Space, { size: 4, children: r.expertStartName === '待指派' ? (_jsxs(Tag, { style: { cursor: hasPerm(`${permissionPrefix}:assign-expert`) ? 'pointer' : 'default' }, color: "default", onClick: () => hasPerm(`${permissionPrefix}:assign-expert`) && handleAssign(r, 0), children: ["\u521D\u5BA1:", r.expertStartName] })) : (_jsxs(_Fragment, { children: [_jsxs(Tag, { color: "green", children: ["\u521D\u5BA1:", hasPerm(`${permissionPrefix}:assign-expert`) ? r.expertStartName : '***'] }), _jsx("a", { onClick: () => handleShowDaily(r, 0), style: { fontSize: 12 }, children: "\u6BCF\u65E5\u5BA1\u6838\u91CF" })] })) }), _jsx(Space, { size: 4, children: r.expertEndName === '待指派' ? (_jsxs(Tag, { style: { cursor: hasPerm(`${permissionPrefix}:assign-expert`) ? 'pointer' : 'default' }, color: "default", onClick: () => hasPerm(`${permissionPrefix}:assign-expert`) && handleAssign(r, 1), children: ["\u7EC8\u5BA1:", r.expertEndName] })) : (_jsxs(_Fragment, { children: [_jsxs(Tag, { color: "red", children: ["\u7EC8\u5BA1:", hasPerm(`${permissionPrefix}:assign-expert`) ? r.expertEndName : '***'] }), _jsx("a", { onClick: () => handleShowDaily(r, 1), style: { fontSize: 12 }, children: "\u6BCF\u65E5\u5BA1\u6838\u91CF" })] })) })] })),
        },
        {
            title: '审核状态',
            dataIndex: 'statusCn',
            width: 120,
            render: (v, r) => (_jsx(Tag, { color: STATUS_MAP[r.status]?.color ?? 'default', children: v })),
        },
        { title: '创建时间', dataIndex: 'addtimeCn', width: 120 },
        {
            title: '操作',
            key: 'actions',
            width: 320,
            fixed: 'right',
            render: (_, r) => (_jsxs(Space, { wrap: true, size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(TableOutlined, {}), onClick: () => handleShowDetails(r), children: "\u660E\u7EC6" }), r.fileUrl && (_jsx(Button, { size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => handleShowFile(r), children: "\u9884\u89C8" })), hasPerm(`${permissionPrefix}:export`) && (_jsx(Button, { size: "small", icon: _jsx(ExportOutlined, {}), onClick: () => handleExport(r), children: "\u5BFC\u51FA" })), hasPerm(`${permissionPrefix}:delete`) && (_jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r), children: "\u5220\u9664" }))] })),
        },
    ];
    // 日统计列
    const dailyCols = [
        { title: '日期', dataIndex: 'ymd', width: 120 },
        { title: '题量', dataIndex: 'nums', width: 80 },
    ];
    return (_jsxs("div", { style: { padding: 16 }, children: [_jsxs(Typography.Title, { level: 4, style: { marginBottom: 12 }, children: [_jsx(TableOutlined, {}), " ", title] }), _jsxs(Space, { style: { marginBottom: 12 }, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => qc.invalidateQueries({ queryKey: [api.queryKey] }), children: "\u5237\u65B0" }), hasPerm(`${permissionPrefix}:import`) && (_jsx(Button, { icon: _jsx(UploadOutlined, {}), type: "primary", onClick: handleImportOpen, children: "\u5BFC\u5165" }))] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: records, loading: listQuery.isFetching, scroll: { x: 1200 }, pagination: {
                    current: page,
                    pageSize,
                    total,
                    showSizeChanger: true,
                    showTotal: (t) => `共 ${t} 条`,
                    onChange: (p, ps) => {
                        if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                            setPageSize(ps);
                            setPage(1);
                        }
                        else {
                            setPage(p);
                        }
                    },
                } }), _jsxs(Drawer, { title: `任务明细 - ID: ${currentRow?.id ?? '-'}`, open: detailOpen, onClose: () => {
                    setDetailOpen(false);
                    setDetailData([]);
                }, width: "85%", destroyOnHidden: true, children: [_jsxs(Descriptions, { bordered: true, column: 2, size: "small", style: { marginBottom: 12 }, children: [_jsx(Descriptions.Item, { label: "\u5DE5\u79CD", children: currentRow?.workTypeCn }), _jsx(Descriptions.Item, { label: "\u7EA7\u522B", children: currentRow?.levelCn }), _jsx(Descriptions.Item, { label: "\u9898\u91CF", children: currentRow?.nums }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: currentRow?.statusCn }), _jsx(Descriptions.Item, { label: "\u521B\u5EFA\u65F6\u95F4", children: currentRow?.addtimeCn }), _jsx(Descriptions.Item, { label: "\u4EFB\u52A1ID", children: currentRow?.id })] }), _jsx(Table, { rowKey: "id", columns: detailColumns, dataSource: detailData, loading: detailLoading, expandable: {
                            expandedRowRender: (record) => (_jsx(Table, { rowKey: "id", columns: expandedDetailColumns, dataSource: record.children ?? [], pagination: false, size: "small", rowClassName: (r) => {
                                    if (r.id === '初审')
                                        return 'row-start';
                                    if (r.id === '终审')
                                        return 'row-end';
                                    return '';
                                } })),
                        }, pagination: false, scroll: {
                            x: taskType === 'exam' ? 1600 : taskType === 'examshicao' ? 1500 : 1200,
                            y: 500,
                        } })] }), _jsx(Modal, { title: "\u6307\u6D3E\u4E13\u5BB6", open: assignOpen, onCancel: () => setAssignOpen(false), width: 455, footer: [
                    _jsx(Button, { onClick: () => setAssignOpen(false), children: "\u53D6\u6D88" }, "cancel"),
                    _jsx(Button, { type: "primary", disabled: !selectedExpertId, onClick: () => {
                            if (currentRow && selectedExpertId) {
                                assignMut.mutate({ taskId: currentRow.id, expertId: selectedExpertId, role: assignRole });
                            }
                        }, children: "\u786E\u5B9A" }, "confirm"),
                ], children: _jsxs("div", { style: { display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }, children: [_jsxs("span", { children: ["\u4E3A\u300C", currentRow?.workTypeCn, "(", currentRow?.levelCn, ")\u300D\u6307\u6D3E"] }), _jsx(Tag, { style: { marginRight: 2 }, color: assignRole > 0 ? 'red' : 'green', children: ROLE_MAP[assignRole]?.label ?? (assignRole > 0 ? '终审' : '初审') }), _jsx(Select, { showSearch: true, style: { width: 200 }, value: selectedExpertId, onChange: (val) => setSelectedExpertId(val), filterOption: (input, option) => option?.label?.includes(input) ?? false, options: (publicQuery.data?.expertList ?? []).map((e) => ({
                                label: e.nickname,
                                value: e.id,
                            })), placeholder: "\u9009\u62E9\u4E13\u5BB6" })] }) }), _jsx(Modal, { title: "\u6BCF\u65E5\u5BA1\u6838\u91CF", open: dailyOpen, onCancel: () => {
                    setDailyOpen(false);
                    setDailyData([]);
                }, width: 350, footer: null, children: _jsx(Table, { rowKey: "ymd", columns: dailyCols, dataSource: dailyData, loading: dailyLoading, pagination: false, size: "small" }) }), _jsx("style", { children: `
        .row-start { background-color: #e1f3d8 !important; }
        .row-end { background-color: #fde2e2 !important; }
      ` }), _jsx(Modal, { title: "\u5BFC\u5165\u4EFB\u52A1", open: importOpen, onCancel: handleImportClose, width: 520, footer: [
                    _jsx(Button, { onClick: handleImportClose, children: "\u53D6\u6D88" }, "cancel"),
                    _jsx(Button, { type: "primary", loading: importMut.isPending, disabled: !importWorkTypeId || !importLevel || importFileList.length === 0, onClick: () => importMut.mutate(), children: "\u786E\u5B9A\u5BFC\u5165" }, "submit"),
                ], children: _jsxs(Form, { layout: "vertical", style: { paddingTop: 8 }, children: [_jsx(Form.Item, { label: "\u5DE5\u79CD", required: true, children: _jsx(TreeSelect, { treeData: (publicQuery.data?.workTypeList ?? []), fieldNames: { label: 'title', value: 'id', children: 'children' }, value: importWorkTypeId ?? undefined, onChange: (v) => setImportWorkTypeId(typeof v === 'number' ? v : null), placeholder: "\u9009\u62E9\u5DE5\u79CD(\u652F\u6301\u641C\u7D22)", showSearch: true, treeNodeFilterProp: "title", treeDefaultExpandAll: false, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u7B49\u7EA7", required: true, children: _jsx(Select, { value: importLevel ?? undefined, onChange: (v) => setImportLevel(v), placeholder: "\u9009\u62E9\u7B49\u7EA7", style: { width: '100%' }, options: LEVEL_OPTS }) }), _jsx(Form.Item, { label: "Excel \u6587\u4EF6", required: true, extra: _jsx("a", { href: TEMPLATE_URLS[taskType], target: "_blank", rel: "noreferrer", children: "\u4E0B\u8F7D\u6A21\u677F" }), children: _jsx(Upload, { beforeUpload: () => false, fileList: importFileList, onChange: ({ fileList }) => setImportFileList(fileList.slice(-1)), accept: ".xlsx,.xls", maxCount: 1, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), children: "\u9009\u62E9\u6587\u4EF6" }) }) })] }) }), _jsxs(Modal, { title: "\u6587\u4EF6\u9884\u89C8", open: fileOpen, onCancel: () => { setFileOpen(false); setFileUrl(''); setFileDownUrl(''); }, width: "85%", footer: null, children: [_jsx("div", { style: { marginBottom: 8 }, children: _jsx("a", { href: fileDownUrl, target: "_blank", rel: "noopener noreferrer", children: _jsx(Button, { icon: _jsx(DownloadOutlined, {}), size: "small", children: "\u4E0B\u8F7D\u6587\u4EF6" }) }) }), _jsx("iframe", { src: fileUrl, style: { width: '100%', height: 600, border: '1px solid #e0e0e0' } })] })] }));
}
