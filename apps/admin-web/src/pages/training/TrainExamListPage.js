import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { Button, Card, Input, Popconfirm, Select, Space, Table, Tag, Typography, message, } from 'antd';
import { DeleteOutlined, EditOutlined, LineChartOutlined, PlusOutlined, TeamOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pageExams, deleteExam, } from '../../api/training';
import { TrainExamListFormModal } from './TrainExamListFormModal';
import { TrainExamPeopleModal } from './TrainExamPeopleModal';
import { useAuthStore } from '../../stores/authStore';
/** 统计隐藏路由跳转（与 main.tsx 约定：pushState + popstate） */
function navigateTo(path) {
    window.history.pushState({}, '', path);
    window.dispatchEvent(new PopStateEvent('popstate'));
}
const TYPE_OPTIONS = [
    { value: 0, label: '线上' },
    { value: 1, label: '线下' },
];
const SOURCE_OPTIONS = [
    { value: 0, label: '内部' },
    { value: 1, label: '外部' },
    { value: 3, label: '赛前复习' },
];
/** 列表查询状态持久化：进入 AI 同步学统计页后列表组件被卸载，返回时从 sessionStorage 恢复当前页/筛选 */
const LIST_STATE_KEY = 'train_exam_list_state';
function readListState() {
    try {
        const raw = sessionStorage.getItem(LIST_STATE_KEY);
        return raw ? JSON.parse(raw) : {};
    }
    catch {
        return {};
    }
}
export function TrainExamListPage() {
    const queryClient = useQueryClient();
    const hasPerm = useAuthStore((s) => s.hasPermission);
    const canViewStats = hasPerm('exam:review-sync:stats:view');
    const [page, setPage] = useState(() => readListState().page ?? 1);
    const [pageSize, setPageSize] = useState(() => readListState().pageSize ?? 10);
    const [keyword, setKeyword] = useState(() => readListState().keyword ?? '');
    const [type, setType] = useState(() => readListState().type);
    const [source, setSource] = useState(() => readListState().source);
    const [formOpen, setFormOpen] = useState(false);
    const [editingId, setEditingId] = useState();
    const [peopleExamId, setPeopleExamId] = useState();
    const [peopleExamTitle, setPeopleExamTitle] = useState('');
    const query = {
        page,
        pageSize,
        keyword: keyword || undefined,
        type,
        source,
    };
    const { data, isLoading } = useQuery({
        queryKey: ['train-exam-list', query],
        queryFn: () => pageExams(query),
    });
    useEffect(() => {
        sessionStorage.setItem(LIST_STATE_KEY, JSON.stringify({ page, pageSize, keyword, type, source }));
    }, [page, pageSize, keyword, type, source]);
    // 恢复的页码可能超出数据总页数（记录被删/每页条数调大后），收敛到最后一页
    useEffect(() => {
        if (!data)
            return;
        const maxPage = Math.max(1, Math.ceil((data.total ?? 0) / pageSize));
        if (page > maxPage)
            setPage(maxPage);
    }, [data, pageSize, page]);
    const deleteMu = useMutation({
        mutationFn: (id) => deleteExam(id),
        onSuccess: () => {
            message.success('删除成功');
            queryClient.invalidateQueries({ queryKey: ['train-exam-list'] });
        },
        onError: () => message.error('删除失败'),
    });
    const onReset = () => {
        setKeyword('');
        setType(undefined);
        setSource(undefined);
        setPage(1);
    };
    const columns = [
        { title: '标题', dataIndex: 'title', ellipsis: true, width: 220 },
        { title: '等级', dataIndex: 'level', width: 80 },
        {
            title: '类型', dataIndex: 'type', width: 80,
            render: (v) => (v === 1 ? _jsx(Tag, { children: "\u7EBF\u4E0B" }) : _jsx(Tag, { color: "blue", children: "\u7EBF\u4E0A" })),
        },
        {
            title: '来源', dataIndex: 'source', width: 80,
            render: (v) => v === 1 ? _jsx(Tag, { children: "\u5916\u90E8" })
                : v === 3 ? _jsx(Tag, { color: "blue", children: "\u8D5B\u524D\u590D\u4E60" })
                    : _jsx(Tag, { color: "green", children: "\u5185\u90E8" }),
        },
        { title: '考试日期', dataIndex: 'examDate', width: 120 },
        { title: '考试时长(分)', dataIndex: 'examTime', width: 110 },
        {
            title: '关联课程数', dataIndex: 'courseIdList', width: 110,
            render: (v) => (Array.isArray(v) ? v.length : 0),
        },
        { title: '创建时间', dataIndex: 'createTime', width: 160 },
        {
            title: '操作', key: 'act', width: 300, fixed: 'right',
            render: (_, r) => {
                // 单库化（Q2）：train_exam_list 为合并库唯一表，来源不再区分，编辑/考生/删除全量放行
                return (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => { setEditingId(r.id); setFormOpen(true); }, children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(TeamOutlined, {}), onClick: () => { setPeopleExamId(r.id); setPeopleExamTitle(r.title ?? ''); }, children: "\u8003\u751F" }), canViewStats && (_jsx(Button, { type: "link", size: "small", icon: _jsx(LineChartOutlined, {}), onClick: () => r.id != null && navigateTo(`/training/exam-lists/${r.id}/review-sync-stats`), children: "AI\u540C\u6B65\u5B66\u7EDF\u8BA1" })), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664\u8BE5\u8003\u8BD5\u8BA1\u5212?", onConfirm: () => r.id != null && deleteMu.mutate(r.id), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] }));
            },
        },
    ];
    return (_jsxs("div", { className: "training-page", children: [_jsxs("section", { className: "page-heading", children: [_jsx(Typography.Title, { level: 4, children: "\u8003\u8BD5\u8BA1\u5212" }), _jsx(Typography.Text, { type: "secondary", children: "\u7BA1\u7406\u8003\u8BD5\u8BA1\u5212\uFF0C\u5173\u8054\u57F9\u8BAD/\u8BFE\u7A0B/\u8003\u751F" })] }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsxs(Space, { wrap: true, children: [_jsx(Input, { allowClear: true, placeholder: "\u641C\u7D22\u6807\u9898/\u673A\u6784", style: { width: 200 }, value: keyword, onChange: e => { setKeyword(e.target.value); setPage(1); } }), _jsx(Select, { allowClear: true, placeholder: "\u7C7B\u578B", style: { width: 120 }, value: type, onChange: v => { setType(v); setPage(1); }, options: TYPE_OPTIONS }), _jsx(Select, { allowClear: true, placeholder: "\u6765\u6E90", style: { width: 120 }, value: source, onChange: v => { setSource(v); setPage(1); }, options: SOURCE_OPTIONS }), _jsx(Button, { onClick: onReset, children: "\u91CD\u7F6E" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => { setEditingId(undefined); setFormOpen(true); }, children: "\u65B0\u589E\u8003\u8BD5\u8BA1\u5212" })] }) }), _jsx(Card, { size: "small", children: _jsx(Table, { rowKey: "id", size: "middle", loading: isLoading, columns: columns, dataSource: data?.items ?? [], scroll: { x: 1100 }, pagination: {
                        current: page,
                        pageSize,
                        total: data?.total ?? 0,
                        showSizeChanger: true,
                        pageSizeOptions: [10, 20, 30, 40, 50],
                        showTotal: t => `共 ${t} 条`,
                        position: ['bottomRight'],
                        onChange: (p, size) => {
                            // 切换每页条数后回到第一页，避免当前页超出总页数
                            setPage(size !== pageSize ? 1 : p);
                            setPageSize(size);
                        },
                    } }) }), _jsx(TrainExamListFormModal, { open: formOpen, editingId: editingId, onClose: () => { setFormOpen(false); setEditingId(undefined); } }), _jsx(TrainExamPeopleModal, { open: !!peopleExamId, examId: peopleExamId ?? 0, examTitle: peopleExamTitle, onClose: () => { setPeopleExamId(undefined); setPeopleExamTitle(''); } })] }));
}
