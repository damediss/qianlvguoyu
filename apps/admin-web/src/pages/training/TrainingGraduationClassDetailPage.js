import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 毕业典礼 · 班级详情（结业办理）
 * 隐藏路由：/training/graduation/classes/:classId
 */
import { useMemo, useState } from 'react';
import { Button, Card, Dropdown, Input, Space, Table, Tag, Typography, message, } from 'antd';
import { ArrowLeftOutlined, DownOutlined, DownloadOutlined, SearchOutlined, } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { FilePreviewModal } from '../../components/FilePreviewModal';
import { useGraduationStudentActions } from './GraduationStudentActions';
import { useNavStore } from '../../stores/navStore';
function navigateTo(path) {
    window.history.pushState({}, '', path);
    window.dispatchEvent(new PopStateEvent('popstate'));
}
const ROSTER_SCOPE_MENU_ITEMS = [
    { key: 'certified', label: '已获证人员名册' },
    { key: 'all', label: '班级全部人员名册' },
];
function classStatusTag(status) {
    if (status === 'done')
        return _jsx(Tag, { color: "success", children: "\u5DF2\u7ED3\u4E1A" });
    if (status === 'partial')
        return _jsx(Tag, { color: "warning", children: "\u90E8\u5206\u7ED3\u4E1A" });
    return _jsx(Tag, { children: "\u672A\u7ED3\u4E1A" });
}
function studentStatusTag(status) {
    if (status === 1)
        return _jsx(Tag, { color: "success", children: "\u5DF2\u7ED3\u4E1A" });
    if (status === 2)
        return _jsx(Tag, { color: "error", children: "\u4E0D\u5408\u683C\u7ED3\u4E1A" });
    return _jsx(Tag, { children: "\u672A\u7ED3\u4E1A" });
}
async function downloadCertifiedRoster(classId, classTitle, scope) {
    try {
        await trainApi.exportCertifiedRoster(classId, classTitle, scope);
    }
    catch (e) {
        message.error(e.message || '导出失败');
    }
}
export function TrainingGraduationClassDetailPage({ classId }) {
    const queryClient = useQueryClient();
    const navigateMenu = useNavStore((s) => s.setSelectedKey);
    const [keyword, setKeyword] = useState('');
    const [certPreview, setCertPreview] = useState(null);
    const openClassSubsidy = async () => {
        try {
            const existing = await trainApi.getClassSubsidy(classId);
            if (existing) {
                trainApi.stashSubsidyDeclareFocus({ classId, subsidyId: existing.id });
                navigateMenu('train_subsidy_declare');
                return;
            }
            const created = await trainApi.ensureClassSubsidy(classId);
            message.success('已为本班创建补贴草稿');
            trainApi.stashSubsidyDeclareFocus({ classId, subsidyId: created.id });
            navigateMenu('train_subsidy_declare');
        }
        catch (e) {
            message.error(e.message || '打开补贴失败');
            trainApi.stashSubsidyDeclareFocus({ classId, openCreate: true });
            navigateMenu('train_subsidy_declare');
        }
    };
    const { data: studentData, isLoading: studentLoading } = useQuery({
        queryKey: ['graduation-students', classId],
        queryFn: () => trainApi.listGraduationStudents(classId),
        enabled: classId > 0,
    });
    const invalidateClass = () => {
        void queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] });
        void queryClient.invalidateQueries({ queryKey: ['graduation-students', classId] });
    };
    const { actionColumn, modals: studentActionModals } = useGraduationStudentActions(classId, invalidateClass);
    const filteredStudents = useMemo(() => {
        const list = (studentData?.list ?? []);
        const q = keyword.trim().toLowerCase();
        if (!q)
            return list;
        return list.filter((r) => {
            const fields = [r.realname, r.phone, r.idCard, r.certificateNo, r.ticketNo];
            return fields.some((v) => String(v ?? '').toLowerCase().includes(q));
        });
    }, [studentData?.list, keyword]);
    const studentColumns = useMemo(() => [
        { title: '姓名', dataIndex: 'realname', width: 90 },
        { title: '手机', dataIndex: 'phone', width: 120 },
        {
            title: '理论',
            dataIndex: 'theoryScore',
            width: 70,
            render: (v) => (v == null || v === '' ? '—' : v),
        },
        {
            title: '技能',
            dataIndex: 'skillScore',
            width: 70,
            render: (v, r) => {
                const score = v ?? r.realScore;
                return score == null || score === '' ? '—' : score;
            },
        },
        {
            title: '综合',
            dataIndex: 'examScore',
            width: 70,
            render: (v) => (v == null || v === '' ? '—' : v),
        },
        {
            title: '工作业绩',
            dataIndex: 'performanceScore',
            width: 90,
            render: (v) => (v == null || v === '' ? '—' : v),
        },
        {
            title: '达标',
            dataIndex: 'qualified',
            width: 90,
            render: (v, r) => v ? _jsx(Tag, { color: "success", children: "\u8FBE\u6807" }) : _jsx(Tag, { color: "warning", children: String(r.qualifyHint || '未达标') }),
        },
        {
            title: '结业状态',
            dataIndex: 'graduateStatus',
            width: 110,
            render: (v) => studentStatusTag(v),
        },
        {
            title: '证书编号',
            width: 200,
            render: (_, r) => {
                if (!r.hasCertificate) {
                    return _jsx(Typography.Text, { type: "secondary", children: "\u672A\u5EFA\u8BC1" });
                }
                const no = String(r.certificateNo || '已建证');
                const path = r.certificateFilePath ? String(r.certificateFilePath) : '';
                return path ? (_jsx(Button, { type: "link", size: "small", style: { padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }, onClick: () => setCertPreview({
                        fileUrl: path,
                        fileName: `${no}.pdf`,
                    }), children: no })) : (_jsx(Typography.Text, { children: no }));
            },
        },
        {
            title: '证面等级',
            dataIndex: 'certificateLevel',
            width: 120,
            ellipsis: true,
            render: (v) => v || '—',
        },
        {
            title: '证面工种',
            dataIndex: 'certificateWorkType',
            width: 110,
            ellipsis: true,
            render: (v) => v || '—',
        },
        // {
        //   title: '模板',
        //   dataIndex: 'templateTitle',
        //   width: 120,
        //   ellipsis: true,
        //   render: (v?: string) => v || '—',
        // },
        actionColumn,
    ], [actionColumn]);
    const goBack = () => {
        if (window.history.length > 1) {
            window.history.back();
            return;
        }
        navigateTo('/training/graduation-ceremony');
    };
    return (_jsxs("div", { className: "training-page", children: [_jsx("section", { className: "page-heading", style: { marginBottom: 12 }, children: _jsxs(Space, { align: "start", style: { width: '100%', justifyContent: 'space-between' }, children: [_jsxs("div", { children: [_jsx(Button, { type: "link", icon: _jsx(ArrowLeftOutlined, {}), onClick: goBack, style: { paddingLeft: 0 }, children: "\u8FD4\u56DE\u73ED\u7EA7\u5217\u8868" }), _jsxs(Typography.Title, { level: 4, style: { margin: 0 }, children: ["\u73ED\u7EA7\u8BE6\u60C5 \u00B7 ", studentData?.classTitle || `班级 ${classId}`] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u73ED\u7EA7\u72B6\u6001 ", classStatusTag(studentData?.graduateStatus), " \u00B7 \u6210\u7EE9\u4E0E\u8003\u8BD5\u6A21\u5757\u4E00\u81F4\uFF08\u7406\u8BBA/\u6280\u80FD/\u7EFC\u5408/\u5DE5\u4F5C\u4E1A\u7EE9\uFF09\uFF1B\u8FBE\u6807=\u7406\u8BBA\u4E0E\u6280\u80FD\u5747\u226560"] })] }), _jsxs(Space, { children: [_jsx(Button, { onClick: () => void openClassSubsidy(), children: "\u7533\u62A5\u8865\u8D34" }), _jsx(Dropdown, { menu: {
                                        items: ROSTER_SCOPE_MENU_ITEMS,
                                        onClick: ({ key }) => void downloadCertifiedRoster(classId, studentData?.classTitle || undefined, key),
                                    }, children: _jsxs(Button, { icon: _jsx(DownloadOutlined, {}), children: ["\u5BFC\u51FA\u4EBA\u5458\u540D\u518C ", _jsx(DownOutlined, {})] }) })] })] }) }), _jsx(Card, { size: "small", style: { marginBottom: 12 }, children: _jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u59D3\u540D / \u624B\u673A / \u8EAB\u4EFD\u8BC1 / \u51C6\u8003\u8BC1\u53F7 / \u8BC1\u4E66\u7F16\u53F7", value: keyword, onChange: (e) => setKeyword(e.target.value), style: { maxWidth: 420 } }) }), _jsx(Card, { size: "small", title: "\u5B66\u5458\u540D\u5355", children: _jsx(Table, { rowKey: "id", size: "middle", loading: studentLoading, columns: studentColumns, dataSource: filteredStudents, pagination: { pageSize: 20, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'] }, scroll: { x: 1220 } }) }), studentActionModals, _jsx(FilePreviewModal, { open: Boolean(certPreview), target: certPreview, onClose: () => setCertPreview(null) })] }));
}
