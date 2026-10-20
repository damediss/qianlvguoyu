import { jsxs as _jsxs, jsx as _jsx, Fragment as _Fragment } from "react/jsx-runtime";
import { CloudUploadOutlined, DeleteOutlined, DownloadOutlined, ExportOutlined, EyeOutlined, ReloadOutlined, TableOutlined, } from '@ant-design/icons';
import { Button, Form, Modal, Select, Space, Table, Tag, Typography, Upload, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useCallback } from 'react';
import { pageTaskXimus, getTaskXimuDetails, getTaskXimuDaily, importTaskXimu, assignTaskXimuExpert, deleteTaskXimu, exportTaskXimu, getPublicData, } from '../../api/exam';
import { useAuthStore } from '../../stores/authStore';
import { parseContentDispositionFilename } from '../../utils/download';
const LEVEL_OPTS = [
    { label: '初级工', value: 5 },
    { label: '中级工', value: 4 },
    { label: '高级工', value: 3 },
    { label: '技师', value: 2 },
    { label: '高级技师', value: 1 },
];
const LEVEL_MAP = {
    1: '高级技师', 2: '技师', 3: '高级工', 4: '中级工', 5: '初级工',
};
const STATUS_MAP = {
    0: { label: '待审核', color: 'default' },
    1: { label: '初审审核中', color: 'processing' },
    2: { label: '终审审核中', color: 'warning' },
    3: { label: '完成', color: 'success' },
};
const TYPE_OPTS = [
    { label: '理论', value: 0 },
    { label: '实操', value: 1 },
];
export function DevTaskDetailPage() {
    const qc = useQueryClient();
    const hasPerm = useAuthStore((s) => s.hasPermission);
    const token = useAuthStore((s) => s.token);
    // Page state
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    // Dialog states
    const [importOpen, setImportOpen] = useState(false);
    const [detailOpen, setDetailOpen] = useState(false);
    const [fileOpen, setFileOpen] = useState(false);
    const [assignOpen, setAssignOpen] = useState(false);
    const [dailyOpen, setDailyOpen] = useState(false);
    // Working row
    const [currentRow, setCurrentRow] = useState(null);
    const [assignRole, setAssignRole] = useState(0);
    const [selectedExpertId, setSelectedExpertId] = useState();
    // Import form
    const [importWorkTypeId, setImportWorkTypeId] = useState();
    const [importLevel, setImportLevel] = useState();
    const [importTypes, setImportTypes] = useState(0);
    const [importing, setImporting] = useState(false);
    // Detail data
    const [detailData, setDetailData] = useState([]);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailKey, setDetailKey] = useState(0);
    const [expandedKeys, setExpandedKeys] = useState([]);
    // Daily data
    const [dailyData, setDailyData] = useState([]);
    const [dailyLoading, setDailyLoading] = useState(false);
    // Current file URL
    const [fileUrl, setFileUrl] = useState('');
    const [fileDownUrl, setFileDownUrl] = useState('');
    // Queries
    const listQuery = useQuery({
        queryKey: ['task-ximus', page, pageSize],
        queryFn: () => pageTaskXimus({ page, pageSize }),
    });
    const publicQuery = useQuery({
        queryKey: ['public-data'],
        queryFn: getPublicData,
        staleTime: 300_000,
    });
    const delMut = useMutation({
        mutationFn: deleteTaskXimu,
        onSuccess: () => {
            message.success('删除成功');
            qc.invalidateQueries({ queryKey: ['task-ximus'] });
        },
    });
    const assignMut = useMutation({
        mutationFn: (args) => assignTaskXimuExpert(args.taskId, { taskId: args.taskId, expertId: args.expertId, role: args.role }),
        onSuccess: () => {
            message.success('指派成功');
            setAssignOpen(false);
            qc.invalidateQueries({ queryKey: ['task-ximus'] });
        },
    });
    const records = listQuery.data?.items ?? [];
    const total = listQuery.data?.total ?? 0;
    // Handlers
    const handleImport = async () => {
        if (!importWorkTypeId || !importLevel) {
            message.warning('请选择工种和等级');
            return;
        }
        setImporting(true);
        try {
            // Trigger file input
            const input = document.getElementById('ximu-upload-input');
            input?.click();
        }
        finally {
            setImporting(false);
        }
    };
    const handleFileSelected = useCallback(async (info) => {
        const file = info.file;
        if (!file)
            return;
        if (!importWorkTypeId || !importLevel) {
            message.warning('请先选择工种和等级');
            return;
        }
        setImporting(true);
        try {
            const fd = new FormData();
            fd.append('file', file);
            fd.append('workTypeId', String(importWorkTypeId));
            fd.append('level', String(importLevel));
            fd.append('types', String(importTypes));
            await importTaskXimu(fd);
            message.success('导入成功');
            setImportOpen(false);
            qc.invalidateQueries({ queryKey: ['task-ximus'] });
        }
        catch (e) {
            message.error(e?.message || '导入失败');
        }
        finally {
            setImporting(false);
        }
    }, [importWorkTypeId, importLevel, importTypes, qc]);
    const handleShowDetails = async (row) => {
        setCurrentRow(row);
        setDetailOpen(true);
        setDetailLoading(true);
        setDetailKey((k) => k + 1);
        try {
            const data = await getTaskXimuDetails(row.id);
            setDetailData(data);
            // 数据加载完成后，将所有父行 ID 设为展开
            setExpandedKeys(data.map((d) => d.id));
        }
        catch {
            message.error('获取细目列表失败');
        }
        finally {
            setDetailLoading(false);
        }
    };
    const handleShowFile = (row) => {
        setCurrentRow(row);
        setFileUrl('https://view.officeapps.live.com/op/view.aspx?src=' + encodeURIComponent(row.fileUrl));
        setFileDownUrl(row.fileUrl);
        setFileOpen(true);
    };
    const handleAssign = (row, role) => {
        setCurrentRow(row);
        setAssignRole(role);
        setAssignOpen(true);
        setSelectedExpertId(undefined);
    };
    const handleShowDaily = async (row, role) => {
        setCurrentRow(row);
        setDailyOpen(true);
        setDailyLoading(true);
        try {
            const data = await getTaskXimuDaily(row.id, role);
            setDailyData(data);
        }
        catch {
            message.error('获取每日审核量失败');
        }
        finally {
            setDailyLoading(false);
        }
    };
    const handleExport = async (row) => {
        try {
            const resp = await exportTaskXimu(row.id);
            const blob = resp.data;
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = parseContentDispositionFilename(resp) ?? `${row.id}_${row.workTypeCn}_${row.levelCn}_细目.xlsx`;
            a.click();
            URL.revokeObjectURL(url);
        }
        catch {
            message.error('导出失败');
        }
    };
    const handleDelete = (row) => {
        Modal.confirm({
            title: '确认删除',
            content: `确定要删除该细目表任务吗？关联的明细和审核记录也将被删除。`,
            okText: '删除',
            okButtonProps: { danger: true },
            onOk: () => delMut.mutate(row.id),
        });
    };
    // Table columns
    const columns = [
        { title: 'ID', dataIndex: 'id', width: 70 },
        {
            title: '工种',
            key: 'workType',
            width: 220,
            render: (_, r) => (_jsxs(Tag, { children: [r.workTypeCn, "(", r.levelCn, ") - ", r.typesCn] })),
        },
        { title: '题量', dataIndex: 'nums', width: 80 },
        {
            title: '审核专家',
            key: 'experts',
            width: 280,
            render: (_, r) => (_jsxs(Space, { direction: "vertical", size: 4, children: [_jsx(Space, { size: 4, children: r.expertStartName === '待指派' ? (_jsxs(Tag, { style: { cursor: hasPerm('task:ximu:assign-expert') ? 'pointer' : 'default' }, color: "default", onClick: () => hasPerm('task:ximu:assign-expert') && handleAssign(r, 0), children: ["\u521D\u5BA1:", r.expertStartName] })) : (_jsxs(_Fragment, { children: [_jsxs(Tag, { color: "green", children: ["\u521D\u5BA1:", hasPerm('task:ximu:assign-expert') ? r.expertStartName : '***'] }), _jsx("a", { onClick: () => handleShowDaily(r, 0), style: { fontSize: 12 }, children: "\u6BCF\u65E5\u5BA1\u6838\u91CF" })] })) }), _jsx(Space, { size: 4, children: r.expertEndName === '待指派' ? (_jsxs(Tag, { style: { cursor: hasPerm('task:ximu:assign-expert') ? 'pointer' : 'default' }, color: "default", onClick: () => hasPerm('task:ximu:assign-expert') && handleAssign(r, 1), children: ["\u7EC8\u5BA1:", r.expertEndName] })) : (_jsxs(_Fragment, { children: [_jsxs(Tag, { color: "red", children: ["\u7EC8\u5BA1:", hasPerm('task:ximu:assign-expert') ? r.expertEndName : '***'] }), _jsx("a", { onClick: () => handleShowDaily(r, 1), style: { fontSize: 12 }, children: "\u6BCF\u65E5\u5BA1\u6838\u91CF" })] })) })] })),
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
            width: 300,
            fixed: 'right',
            render: (_, r) => (_jsxs(Space, { wrap: true, size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(TableOutlined, {}), onClick: () => handleShowDetails(r), children: "\u7EC6\u76EE\u8868\u5217\u8868" }), _jsx(Button, { size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => handleShowFile(r), children: "\u7EC6\u76EE\u8868\u6587\u4EF6" }), hasPerm('task:ximu:export') && (_jsx(Button, { size: "small", icon: _jsx(ExportOutlined, {}), onClick: () => handleExport(r), children: "\u5BFC\u51FA" })), r.status === 0 && hasPerm('task:ximu:delete') && (_jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r), children: "\u5220\u9664" }))] })),
        },
    ];
    const detailCols = [
        { title: 'ID', dataIndex: 'id', width: 60 },
        { title: '编号', dataIndex: 'sno', width: 100 },
        { title: '一级', dataIndex: 'sno0', width: 140 },
        { title: '二级', dataIndex: 'sno1', width: 140 },
        { title: '三级', dataIndex: 'title', width: 210 },
        { title: '代码', dataIndex: 'snoItem', width: 160 },
        { title: '内容', dataIndex: 'snoTitle' },
    ];
    const dailyCols = [
        { title: '日期', dataIndex: 'ymd', width: 120 },
        { title: '题量', dataIndex: 'nums', width: 80 },
    ];
    return (_jsxs("div", { style: { padding: 16 }, children: [_jsxs(Typography.Title, { level: 4, style: { marginBottom: 12 }, children: [_jsx(TableOutlined, {}), " \u7EC6\u76EE\u8868"] }), _jsxs(Space, { style: { marginBottom: 12 }, children: [hasPerm('task:ximu:import') && (_jsx(Button, { icon: _jsx(CloudUploadOutlined, {}), onClick: () => setImportOpen(true), children: "\u5BFC\u5165" })), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => qc.invalidateQueries({ queryKey: ['task-ximus'] }), children: "\u5237\u65B0" })] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: records, loading: listQuery.isFetching, scroll: { x: 1200 }, pagination: {
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
                } }), _jsx(Modal, { title: "\u5BFC\u5165\u7EC6\u76EE\u8868", open: importOpen, onCancel: () => { setImportOpen(false); setImportWorkTypeId(undefined); setImportLevel(undefined); }, footer: null, width: 500, children: _jsxs(Form, { layout: "vertical", children: [_jsx(Form.Item, { label: "\u5DE5\u79CD", required: true, children: _jsx(Select, { showSearch: true, filterOption: (input, option) => option?.label?.includes(input) ?? false, options: publicQuery.data?.workTypeList?.flatMap((g) => (g.children ?? []).map((c) => ({ label: g.title + ' - ' + c.title, value: c.id }))) ?? [], value: importWorkTypeId, onChange: setImportWorkTypeId, placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD" }) }), _jsx(Form.Item, { label: "\u7B49\u7EA7", required: true, children: _jsx(Select, { options: LEVEL_OPTS, value: importLevel, onChange: setImportLevel, placeholder: "\u8BF7\u9009\u62E9\u7B49\u7EA7" }) }), _jsx(Form.Item, { label: "\u7C7B\u578B", required: true, children: _jsx(Select, { options: TYPE_OPTS, value: importTypes, onChange: setImportTypes }) }), _jsx(Form.Item, { label: "\u6587\u4EF6", children: _jsx(Upload, { accept: ".xlsx,.xls", maxCount: 1, showUploadList: false, customRequest: (options) => {
                                    const { file, onSuccess } = options;
                                    handleFileSelected({ file });
                                    onSuccess?.('ok');
                                }, children: _jsx(Button, { icon: _jsx(CloudUploadOutlined, {}), loading: importing, children: "\u9009\u62E9\u6587\u4EF6\u4E0A\u4F20" }) }) })] }) }), _jsx(Modal, { title: "\u7EC6\u76EE\u8868\u5217\u8868", open: detailOpen, onCancel: () => { setDetailOpen(false); setDetailData([]); setDetailLoading(false); setExpandedKeys([]); setCurrentRow(null); }, width: "85%", footer: null, children: _jsx(Table, { rowKey: "id", columns: detailCols, dataSource: detailData, loading: detailLoading, expandable: {
                        expandedRowKeys: expandedKeys,
                        onExpandedRowsChange: (keys) => setExpandedKeys(keys),
                    }, rowClassName: (record) => {
                        if (String(record.id) === '初审')
                            return 'row-start';
                        if (String(record.id) === '终审')
                            return 'row-end';
                        return '';
                    }, scroll: { x: 800, y: 500 }, pagination: false }, detailKey) }), _jsxs(Modal, { title: "\u7EC6\u76EE\u8868\u6587\u4EF6\u9884\u89C8", open: fileOpen, onCancel: () => { setFileOpen(false); setFileUrl(''); setFileDownUrl(''); }, width: "85%", footer: null, children: [_jsx("div", { style: { marginBottom: 8 }, children: _jsx("a", { href: fileDownUrl, target: "_blank", rel: "noopener noreferrer", children: _jsx(Button, { icon: _jsx(DownloadOutlined, {}), size: "small", children: "\u4E0B\u8F7D\u6587\u4EF6" }) }) }), _jsx("iframe", { src: fileUrl, style: { width: '100%', height: 600, border: '1px solid #e0e0e0' } })] }), _jsx(Modal, { title: "\u6307\u6D3E\u4E13\u5BB6", open: assignOpen, onCancel: () => setAssignOpen(false), width: 455, footer: [
                    _jsx(Button, { onClick: () => setAssignOpen(false), children: "\u53D6\u6D88" }, "cancel"),
                    _jsx(Button, { type: "primary", disabled: !selectedExpertId, onClick: () => {
                            if (currentRow && selectedExpertId) {
                                assignMut.mutate({ taskId: currentRow.id, expertId: selectedExpertId, role: assignRole });
                            }
                        }, children: "\u786E\u5B9A" }, "confirm"),
                ], children: _jsxs("div", { style: { display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }, children: [_jsxs("span", { children: ["\u4E3A\u300C", currentRow?.workTypeCn, "(", currentRow?.levelCn, ")\u300D\u6307\u6D3E"] }), _jsx(Tag, { style: { marginRight: 2 }, color: assignRole > 0 ? 'red' : 'green', children: assignRole > 0 ? '终审' : '初审' }), _jsx(Select, { showSearch: true, style: { width: 200 }, value: selectedExpertId, onChange: (val) => setSelectedExpertId(val), filterOption: (input, option) => option?.label?.includes(input) ?? false, options: (publicQuery.data?.expertList ?? []).map((e) => ({
                                label: e.nickname,
                                value: e.id,
                            })), placeholder: "\u9009\u62E9\u4E13\u5BB6" })] }) }), _jsx(Modal, { title: "\u6BCF\u65E5\u5BA1\u6838\u91CF", open: dailyOpen, onCancel: () => { setDailyOpen(false); setDailyData([]); }, width: 350, footer: null, children: _jsx(Table, { rowKey: "ymd", columns: dailyCols, dataSource: dailyData, loading: dailyLoading, pagination: false, size: "small" }) }), _jsx("style", { children: `
        .row-start { background-color: #e1f3d8 !important; }
        .row-end { background-color: #fde2e2 !important; }
      ` })] }));
}
