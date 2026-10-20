import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState } from 'react';
import { Alert, Button, Descriptions, Drawer, Input, Modal, Space, Table, Tabs, Tag, Typography, message, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckOutlined, CloseOutlined, EyeOutlined, TeamOutlined, } from '@ant-design/icons';
import * as trainApi from '../../api/training';
const { Paragraph, Text } = Typography;
const PACK_STATUS_MAP = {
    pending: { label: '待确认', color: 'warning' },
    rejected: { label: '已驳回', color: 'error' },
    materialized: { label: '已通过并建班', color: 'success' },
    draft: { label: '机构草稿', color: 'default' },
    superseded: { label: '已作废', color: 'default' },
};
const CLASS_STATUS_MAP = {
    0: { label: '待确认', color: 'warning' },
    1: { label: '已确认', color: 'success' },
    2: { label: '已驳回', color: 'error' },
};
function packTabStatus(tab) {
    if (tab === 'pending')
        return 'pending';
    if (tab === 'rejected')
        return 'rejected';
    if (tab === 'materialized')
        return 'materialized';
    return undefined;
}
function legacyTabStatus(tab) {
    if (tab === 'pending')
        return 0;
    if (tab === 'confirmed')
        return 1;
    if (tab === 'rejected')
        return 2;
    return undefined;
}
function formatTime(value) {
    if (!value)
        return '-';
    return value.replace('T', ' ').slice(0, 19);
}
export function EnterpriseClassConfirmPanel() {
    const queryClient = useQueryClient();
    const [mainTab, setMainTab] = useState('pack');
    const [packTab, setPackTab] = useState('pending');
    const [packPage, setPackPage] = useState(1);
    const [packPageSize, setPackPageSize] = useState(10);
    const [packKeyword, setPackKeyword] = useState('');
    const [detailOpen, setDetailOpen] = useState(false);
    const [currentTask, setCurrentTask] = useState(null);
    const [actionLoading, setActionLoading] = useState(false);
    const [rejectOpen, setRejectOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const [legacyTab, setLegacyTab] = useState('pending');
    const [legacyPage, setLegacyPage] = useState(1);
    const [legacyPageSize, setLegacyPageSize] = useState(10);
    const [legacyTitleFilter, setLegacyTitleFilter] = useState('');
    const [legacyDetailOpen, setLegacyDetailOpen] = useState(false);
    const [currentClass, setCurrentClass] = useState(null);
    const [studentPage, setStudentPage] = useState(1);
    const [studentPageSize, setStudentPageSize] = useState(20);
    const packListQuery = useQuery({
        queryKey: ['enterprise-pack-confirm', packTab, packPage, packPageSize],
        queryFn: () => trainApi.pageDivisionPackTasks({
            page: packPage,
            pageSize: packPageSize,
            status: packTabStatus(packTab),
        }),
        enabled: mainTab === 'pack',
    });
    const packDetailQuery = useQuery({
        queryKey: ['enterprise-pack-detail', currentTask?.taskId],
        queryFn: () => trainApi.getDivisionPackTask(currentTask.taskId),
        enabled: detailOpen && !!currentTask?.taskId,
    });
    const legacyListQuery = useQuery({
        queryKey: ['enterprise-class-confirm', legacyTab, legacyPage, legacyPageSize, legacyTitleFilter],
        queryFn: () => trainApi.pageInfoClassList({
            page: legacyPage,
            pageSize: legacyPageSize,
            companyConfirmStatus: legacyTabStatus(legacyTab),
        }),
        enabled: mainTab === 'legacy',
    });
    const studentsQuery = useQuery({
        queryKey: ['enterprise-class-students', currentClass?.id, studentPage, studentPageSize],
        queryFn: () => trainApi.listClassStudents(currentClass.id, studentPage, studentPageSize),
        enabled: legacyDetailOpen && !!currentClass?.id,
    });
    const refreshPack = () => {
        queryClient.invalidateQueries({ queryKey: ['enterprise-pack-confirm'] });
        queryClient.invalidateQueries({ queryKey: ['enterprise-pack-detail'] });
        queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
    };
    const refreshLegacy = () => {
        queryClient.invalidateQueries({ queryKey: ['enterprise-class-confirm'] });
    };
    const detail = packDetailQuery.data ?? currentTask;
    const packItems = (packListQuery.data?.items ?? []).filter((row) => {
        if (packTab === 'all' && (row.status === 'draft' || row.status === 'superseded')) {
            return false;
        }
        if (!packKeyword.trim())
            return true;
        const kw = packKeyword.trim().toLowerCase();
        return String(row.taskId).includes(kw)
            || String(row.trainInfoId).includes(kw)
            || (row.classNamePrefix ?? '').toLowerCase().includes(kw);
    });
    const rawLegacy = legacyListQuery.data?.list ?? [];
    const legacyList = legacyTab === 'all'
        ? rawLegacy
        : rawLegacy.filter((row) => (row.company_confirm_status ?? 0) === legacyTabStatus(legacyTab));
    const filteredLegacy = legacyTitleFilter.trim()
        ? legacyList.filter((row) => {
            const kw = legacyTitleFilter.trim().toLowerCase();
            return (row.title ?? '').toLowerCase().includes(kw)
                || (row.train_title ?? '').toLowerCase().includes(kw);
        })
        : legacyList;
    const formatPlatformUserSyncMsg = (result, fallback) => {
        const sync = result?.platformUserSync
            ?? result?.materialized?.platformUserSync;
        if (!sync)
            return fallback;
        const parts = [
            fallback,
            `学员账号：新建 ${sync.created ?? 0}，复用 ${sync.reused ?? 0}，更新 ${sync.updated ?? 0}，跳过 ${sync.skipped ?? 0}`,
        ];
        if ((sync.created ?? 0) > 0) {
            parts.push('新建账号默认密码 123123');
        }
        if (sync.warnings?.length) {
            parts.push(`有 ${sync.warnings.length} 条同步告警，请到学员信息核对`);
        }
        return parts.join('；');
    };
    const runPackAction = async (fn, successMsg, formatMsg) => {
        setActionLoading(true);
        try {
            const result = await fn();
            message.success(formatMsg ? formatMsg(result) : successMsg);
            refreshPack();
            setDetailOpen(false);
            setCurrentTask(null);
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '操作失败');
        }
        finally {
            setActionLoading(false);
        }
    };
    const runLegacyAction = async (fn, successMsg) => {
        setActionLoading(true);
        try {
            await fn();
            message.success(successMsg);
            refreshLegacy();
            if (currentClass?.id) {
                queryClient.invalidateQueries({ queryKey: ['enterprise-class-students', currentClass.id] });
            }
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '操作失败');
        }
        finally {
            setActionLoading(false);
        }
    };
    const packColumns = [
        { title: '方案号', dataIndex: 'taskId', width: 90 },
        { title: '培训信息', dataIndex: 'trainInfoId', width: 100 },
        {
            title: '班级',
            width: 100,
            render: (_, row) => `${row.classCount} 班`,
        },
        {
            title: '人数',
            width: 90,
            render: (_, row) => row.assignedCount,
        },
        {
            title: '策略',
            width: 120,
            render: (_, row) => {
                if (row.strategy === 'manual')
                    return '手动组班';
                if (row.strategy === 'by_job_level')
                    return '工种/等级';
                if (row.strategy === 'by_job_level_slot')
                    return '工种/等级+时段';
                if (row.strategy === 'by_shift_team')
                    return '按班组';
                return '名单顺序';
            },
        },
        {
            title: '状态',
            width: 130,
            render: (_, row) => {
                const meta = PACK_STATUS_MAP[row.status] ?? { label: row.statusLabel || row.status, color: 'default' };
                return _jsx(Tag, { color: meta.color, children: meta.label });
            },
        },
        {
            title: '提交时间',
            width: 170,
            render: (_, row) => formatTime(row.updateTime),
        },
        {
            title: '操作',
            width: 100,
            fixed: 'right',
            render: (_, row) => (_jsx(Button, { type: "link", icon: _jsx(EyeOutlined, {}), onClick: () => {
                    setCurrentTask(row);
                    setDetailOpen(true);
                }, children: "\u67E5\u770B" })),
        },
    ];
    const legacyColumns = [
        { title: '班级名称', dataIndex: 'title', ellipsis: true },
        { title: '培训项目', dataIndex: 'train_title', width: 180, ellipsis: true },
        { title: '工种', dataIndex: 'work_type_title', width: 120, ellipsis: true },
        { title: '人数', dataIndex: 'nums', width: 70 },
        {
            title: '状态',
            width: 100,
            render: (_, row) => {
                const code = row.company_confirm_status ?? 0;
                const meta = CLASS_STATUS_MAP[code] ?? { label: '未知', color: 'default' };
                return _jsx(Tag, { color: meta.color, children: meta.label });
            },
        },
        {
            title: '操作',
            width: 100,
            fixed: 'right',
            render: (_, row) => (_jsx(Button, { type: "link", icon: _jsx(EyeOutlined, {}), onClick: () => {
                    setCurrentClass(row);
                    setStudentPage(1);
                    setLegacyDetailOpen(true);
                }, children: "\u67E5\u770B" })),
        },
    ];
    const studentColumns = [
        { title: '姓名', dataIndex: 'realname', width: 100 },
        { title: '手机号', dataIndex: 'phone', width: 120 },
        { title: '身份证号', dataIndex: 'id_card', ellipsis: true },
    ];
    const memberColumns = [
        { title: '姓名', dataIndex: 'personName', width: 90 },
        { title: '单位', dataIndex: 'workshop', width: 120, ellipsis: true, render: (v) => v || '-' },
        { title: '工种', dataIndex: 'jobName', width: 110, ellipsis: true, render: (v) => v || '-' },
        { title: '等级', dataIndex: 'levelName', width: 90, render: (v) => v || '-' },
        { title: '班组', dataIndex: 'shiftTeam', width: 90, render: (v) => v || '-' },
        { title: '身份证', dataIndex: 'idCard', width: 170 },
    ];
    const confirmStatus = currentClass?.company_confirm_status ?? 0;
    return (_jsxs(_Fragment, { children: [_jsx(Paragraph, { type: "secondary", style: { marginBottom: 16 }, children: "\u4F18\u5148\u5BA1\u6838\u673A\u6784\u63D0\u4EA4\u7684\u5206\u73ED\u65B9\u6848\uFF1B\u901A\u8FC7\u540E\u7CFB\u7EDF\u624D\u521B\u5EFA\u57F9\u8BAD\u73ED\u7EA7\u3002\u65E7\u7248\u300C\u5DF2\u5EFA\u73ED\u7EA7\u786E\u8BA4\u300D\u4ECD\u4FDD\u7559\u5728\u4E0B\u65B9\u6807\u7B7E\u4E2D\u3002" }), _jsx(Tabs, { activeKey: mainTab, onChange: (key) => setMainTab(key), items: [
                    { key: 'pack', label: '分班方案审核' },
                    { key: 'legacy', label: '班级确认（兼容）' },
                ] }), mainTab === 'pack' && (_jsxs(_Fragment, { children: [_jsx(Tabs, { activeKey: packTab, onChange: (key) => {
                            setPackTab(key);
                            setPackPage(1);
                        }, items: [
                            { key: 'pending', label: '待确认' },
                            { key: 'rejected', label: '已驳回' },
                            { key: 'materialized', label: '已通过建班' },
                            { key: 'all', label: '全部' },
                        ] }), _jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { placeholder: "\u641C\u7D22\u65B9\u6848\u53F7 / \u57F9\u8BAD\u4FE1\u606F", allowClear: true, style: { width: 240 }, value: packKeyword, onChange: (e) => setPackKeyword(e.target.value) }), _jsx(Button, { onClick: () => packListQuery.refetch(), children: "\u5237\u65B0" })] }), _jsx(Table, { rowKey: "taskId", columns: packColumns, dataSource: packItems, loading: packListQuery.isLoading, scroll: { x: 900 }, pagination: {
                            current: packPage,
                            pageSize: packPageSize,
                            total: packListQuery.data?.total ?? 0,
                            onChange: (p, ps) => { setPackPage(p); setPackPageSize(ps); },
                            showTotal: (t) => `共 ${t} 条`,
                        } })] })), mainTab === 'legacy' && (_jsxs(_Fragment, { children: [_jsx(Tabs, { activeKey: legacyTab, onChange: (key) => {
                            setLegacyTab(key);
                            setLegacyPage(1);
                        }, items: [
                            { key: 'pending', label: '待确认' },
                            { key: 'confirmed', label: '已确认' },
                            { key: 'rejected', label: '已驳回' },
                            { key: 'all', label: '全部记录' },
                        ] }), _jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { placeholder: "\u641C\u7D22\u73ED\u7EA7\u6216\u57F9\u8BAD\u9879\u76EE", allowClear: true, style: { width: 240 }, value: legacyTitleFilter, onChange: (e) => setLegacyTitleFilter(e.target.value) }), _jsx(Button, { onClick: () => legacyListQuery.refetch(), children: "\u5237\u65B0" })] }), _jsx(Table, { rowKey: "id", columns: legacyColumns, dataSource: filteredLegacy, loading: legacyListQuery.isLoading, scroll: { x: 900 }, pagination: {
                            current: legacyPage,
                            pageSize: legacyPageSize,
                            total: legacyTab === 'all' ? filteredLegacy.length : (legacyListQuery.data?.total ?? 0),
                            onChange: (p, ps) => { setLegacyPage(p); setLegacyPageSize(ps); },
                            showTotal: (t) => `共 ${t} 条`,
                        } })] })), _jsx(Drawer, { title: detail ? `分班方案 #${detail.taskId}` : '分班方案', width: 920, open: detailOpen, onClose: () => { setDetailOpen(false); setCurrentTask(null); }, extra: detail?.status === 'pending' ? (_jsxs(Space, { children: [_jsx(Button, { type: "primary", icon: _jsx(CheckOutlined, {}), loading: actionLoading, onClick: () => runPackAction(() => trainApi.companyConfirmDivisionPackTask(detail.taskId), '已通过，培训班级已创建', (result) => formatPlatformUserSyncMsg(result, '已通过，培训班级已创建')), children: "\u901A\u8FC7\u5E76\u5EFA\u73ED" }), _jsx(Button, { danger: true, icon: _jsx(CloseOutlined, {}), onClick: () => setRejectOpen(true), children: "\u9A73\u56DE" })] })) : null, children: detail && (_jsxs(_Fragment, { children: [_jsx(Alert, { type: "info", showIcon: true, icon: _jsx(TeamOutlined, {}), message: "\u673A\u6784\u5206\u73ED\u65B9\u6848", description: "\u8BF7\u6838\u5BF9\u73ED\u7EA7\u4E0E\u5B66\u5458\u540D\u5355\u3002\u901A\u8FC7\u540E\u7CFB\u7EDF\u5C06\u521B\u5EFA\u57F9\u8BAD\u73ED\u7EA7\u5E76\u4E3A\u5B66\u5458\u5F00\u901A App \u8D26\u53F7\uFF08\u9ED8\u8BA4\u5BC6\u7801 123123\uFF09\uFF1B\u9A73\u56DE\u540E\u673A\u6784\u53EF\u8C03\u6574\u518D\u63D0\u4EA4\u3002", style: { marginBottom: 16 } }), _jsxs(Descriptions, { column: 2, size: "small", bordered: true, style: { marginBottom: 16 }, children: [_jsx(Descriptions.Item, { label: "\u72B6\u6001", children: _jsx(Tag, { color: PACK_STATUS_MAP[detail.status]?.color, children: PACK_STATUS_MAP[detail.status]?.label || detail.statusLabel }) }), _jsxs(Descriptions.Item, { label: "\u57F9\u8BAD\u4FE1\u606F", children: ["#", detail.trainInfoId] }), _jsx(Descriptions.Item, { label: "\u73ED\u7EA7\u6570", children: detail.classCount }), _jsx(Descriptions.Item, { label: "\u4EBA\u6570", children: detail.assignedCount }), _jsx(Descriptions.Item, { label: "\u6BCF\u73ED\u4E0A\u9650", children: detail.classMax }), _jsx(Descriptions.Item, { label: "\u7B56\u7565", children: detail.strategy === 'manual' ? '手动组班'
                                        : detail.strategy === 'by_job_level' ? '按工种/等级'
                                            : detail.strategy === 'by_job_level_slot' ? '工种/等级+上下午时段'
                                                : detail.strategy === 'by_shift_team' ? '按班组/倒班'
                                                    : '按名单顺序' }), detail.submitUsername && (_jsxs(Descriptions.Item, { label: "\u63D0\u4EA4\u4EBA", span: 2, children: [detail.submitUsername, " \u00B7 ", formatTime(detail.updateTime)] })), detail.rejectReason && (_jsx(Descriptions.Item, { label: "\u9A73\u56DE\u7406\u7531", span: 2, children: _jsx(Text, { type: "danger", children: detail.rejectReason }) })), detail.confirmUsername && (_jsxs(Descriptions.Item, { label: "\u5BA1\u6838\u4EBA", span: 2, children: [detail.confirmUsername, " \u00B7 ", formatTime(detail.confirmTime)] }))] }), (detail.classes ?? []).map((cls) => (_jsxs("div", { style: { marginBottom: 20 }, children: [_jsxs(Typography.Title, { level: 5, style: { marginBottom: 8 }, children: [cls.title, _jsxs(Text, { type: "secondary", style: { marginLeft: 8, fontSize: 13, fontWeight: 400 }, children: [cls.members?.length ?? cls.nums, " \u4EBA", cls.group ? ` · ${cls.group}` : ''] })] }), _jsx(Table, { rowKey: "rosterId", size: "small", pagination: false, columns: memberColumns, dataSource: cls.members ?? [] })] }, cls.tempId || cls.title))), (detail.unassigned?.length ?? 0) > 0 && (_jsxs(_Fragment, { children: [_jsx(Typography.Title, { level: 5, children: "\u672A\u5206\u5165\u4EBA\u5458" }), _jsx(Table, { rowKey: (r) => `${r.rosterId}-${r.reason}`, size: "small", pagination: { pageSize: 8, showSizeChanger: false }, dataSource: detail.unassigned, columns: [
                                        { title: '姓名', dataIndex: 'personName', width: 100 },
                                        { title: '身份证', dataIndex: 'idCard', width: 180 },
                                        { title: '原因', dataIndex: 'reason' },
                                    ] })] }))] })) }), _jsx(Drawer, { title: currentClass ? `分班确认：${currentClass.title}` : '分班确认', width: 880, open: legacyDetailOpen, onClose: () => { setLegacyDetailOpen(false); setCurrentClass(null); }, extra: confirmStatus === 0 ? (_jsxs(Space, { children: [_jsx(Button, { type: "primary", icon: _jsx(CheckOutlined, {}), loading: actionLoading, onClick: () => runLegacyAction(() => trainApi.companyConfirmInfoClass(Number(currentClass.id)), '已确认分班'), children: "\u786E\u8BA4\u5206\u73ED" }), _jsx(Button, { danger: true, icon: _jsx(CloseOutlined, {}), onClick: () => {
                                setRejectOpen(true);
                            }, children: "\u9A73\u56DE" })] })) : null, children: currentClass && (_jsxs(_Fragment, { children: [_jsxs(Descriptions, { column: 2, size: "small", bordered: true, style: { marginBottom: 16 }, children: [_jsx(Descriptions.Item, { label: "\u73ED\u7EA7\u540D\u79F0", children: currentClass.title }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: _jsx(Tag, { color: CLASS_STATUS_MAP[confirmStatus]?.color, children: CLASS_STATUS_MAP[confirmStatus]?.label }) }), _jsx(Descriptions.Item, { label: "\u57F9\u8BAD\u9879\u76EE", children: currentClass.train_title ?? '-' }), _jsx(Descriptions.Item, { label: "\u8BA1\u5212\u4EBA\u6570", children: currentClass.nums ?? '-' })] }), _jsx(Table, { rowKey: "id", size: "small", title: () => '班级学员名单', columns: studentColumns, dataSource: studentsQuery.data?.list ?? [], loading: studentsQuery.isLoading, pagination: {
                                current: studentPage,
                                pageSize: studentPageSize,
                                total: studentsQuery.data?.total ?? 0,
                                onChange: (p, ps) => {
                                    if (ps !== studentPageSize) {
                                        setStudentPageSize(ps);
                                        setStudentPage(1);
                                    }
                                    else {
                                        setStudentPage(p);
                                    }
                                },
                            } })] })) }), _jsx(Modal, { title: mainTab === 'pack' || detailOpen ? '驳回分班方案' : '驳回分班', open: rejectOpen, onCancel: () => { setRejectOpen(false); setRejectReason(''); }, onOk: async () => {
                    if (!rejectReason.trim()) {
                        message.warning('请填写驳回理由');
                        return;
                    }
                    if (detailOpen && detail) {
                        await runPackAction(() => trainApi.companyRejectDivisionPackTask(detail.taskId, rejectReason.trim()), '已驳回方案');
                    }
                    else if (currentClass) {
                        await runLegacyAction(() => trainApi.companyRejectInfoClass(Number(currentClass.id), rejectReason.trim()), '已驳回');
                        setLegacyDetailOpen(false);
                        setCurrentClass(null);
                    }
                    setRejectOpen(false);
                    setRejectReason('');
                }, confirmLoading: actionLoading, destroyOnClose: true, children: _jsx(Input.TextArea, { rows: 4, placeholder: "\u8BF7\u586B\u5199\u9A73\u56DE\u7406\u7531\uFF0C\u673A\u6784\u5C06\u636E\u6B64\u8C03\u6574\u5206\u73ED", value: rejectReason, onChange: (e) => setRejectReason(e.target.value) }) })] }));
}
