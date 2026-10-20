import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 工作台草稿审核面板：默认锁定批次；"全部批次"开关开启后跨批次浏览（默认只显示待审，表格增加批次列）。
 * 批量通过/驳回、行内编辑、展开行选项高亮与原有行为一致；审核/编辑后回调刷新总览计数。
 */
import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Drawer, Form, Input, InputNumber, message, Select, Space, Switch, Table, Tag, } from 'antd';
import { editDraft, listDrafts, reviewDrafts } from '../theoryGenApi';
import { DIFFICULTY_LABEL, TYPE_LABEL, parseDraftOptions } from './constants';
const DraftReviewPanel = ({ lockedBatch, defaultAllMode, batchLabel, onClose, onChanged }) => {
    const qc = useQueryClient();
    const [allMode, setAllMode] = useState(defaultAllMode ?? false);
    const [reviewStatus, setReviewStatus] = useState();
    const [questionType, setQuestionType] = useState();
    const [selectedKeys, setSelectedKeys] = useState([]);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [editingDraft, setEditingDraft] = useState(null);
    const [editForm] = Form.useForm();
    // 全部批次模式默认只看待审，避免上千条全量草稿首屏翻页
    React.useEffect(() => {
        if (allMode && reviewStatus === undefined) {
            setReviewStatus('draft');
            setPage(1);
        }
    }, [allMode, reviewStatus]);
    const batchId = allMode ? undefined : lockedBatch?.id;
    const draftsQuery = useQuery({
        queryKey: ['theorygen-drafts', batchId ?? 'all', reviewStatus, questionType, page, pageSize],
        queryFn: () => listDrafts({ batchId, reviewStatus, questionType, page, pageSize }),
        enabled: allMode || !!lockedBatch,
    });
    const refresh = () => {
        qc.invalidateQueries({ queryKey: ['theorygen-drafts'] });
        onChanged();
    };
    const bulkReview = async (approve) => {
        if (!selectedKeys.length) {
            message.warning('请先勾选草稿');
            return;
        }
        const n = await reviewDrafts(selectedKeys, approve);
        message.success(`已${approve ? '通过' : '驳回'} ${n} 条`);
        setSelectedKeys([]);
        refresh();
    };
    const saveDraftEdit = async () => {
        if (!editingDraft)
            return;
        const values = await editForm.validateFields();
        await editDraft(editingDraft.id, values);
        message.success('已保存');
        setEditingDraft(null);
        refresh();
    };
    const columns = [
        ...(allMode ? [{
                title: '批次', dataIndex: 'batchId', key: 'batchId', width: 170, ellipsis: true,
                render: (v) => batchLabel(v),
            }] : []),
        { title: '题干', dataIndex: 'content', key: 'content', ellipsis: true },
        { title: '题型', dataIndex: 'questionType', key: 'questionType', width: 90,
            render: (v) => TYPE_LABEL[v] ?? v },
        { title: '答案', dataIndex: 'answer', key: 'answer', width: 80 },
        { title: '难度', dataIndex: 'difficulty', key: 'difficulty', width: 70,
            render: (v) => DIFFICULTY_LABEL[v] ?? v },
        { title: '审核', dataIndex: 'reviewStatus', key: 'reviewStatus', width: 80,
            render: (v) => (v === 'approved' ? _jsx(Tag, { color: "success", children: "\u5DF2\u901A\u8FC7" })
                : v === 'rejected' ? _jsx(Tag, { color: "error", children: "\u5DF2\u9A73\u56DE" }) : _jsx(Tag, { children: "\u5F85\u5BA1" })) },
        { title: '入库', dataIndex: 'publishStatus', key: 'publishStatus', width: 70,
            render: (v) => (v === 1 ? _jsx(Tag, { color: "success", children: "\u5DF2\u5165\u5E93" }) : _jsx(Tag, { children: "\u672A\u5165\u5E93" })) },
        { title: '操作', key: 'op', width: 70, render: (_, r) => (_jsx(Button, { size: "small", onClick: () => { setEditingDraft(r); editForm.setFieldsValue(r); }, children: "\u7F16\u8F91" })) },
    ];
    return (_jsxs("div", { className: "gen-active-wrap", children: [_jsxs("div", { className: "gen-section-title", children: [_jsx("span", { children: "\u8BD5\u9898\u8349\u7A3F\u5BA1\u6838" }), _jsx("span", { style: { color: '#888', fontWeight: 400, fontSize: 12 }, children: allMode ? '全部批次（默认只显示待审）' : lockedBatch ? `已锁定：${batchLabel(lockedBatch.id)}` : '未选择批次' }), _jsx("span", { style: { flex: 1 } }), _jsxs("span", { style: { fontWeight: 400, fontSize: 12, color: '#666' }, children: [_jsx(Switch, { size: "small", checked: allMode, onChange: setAllMode }), " \u5168\u90E8\u6279\u6B21"] }), _jsx(Button, { size: "small", onClick: onClose, children: "\u6536\u8D77" })] }), _jsxs(Space, { style: { marginBottom: 12 }, wrap: true, children: [_jsx(Select, { style: { width: 120 }, allowClear: true, placeholder: "\u5BA1\u6838\u72B6\u6001", value: reviewStatus, onChange: (v) => { setReviewStatus(v); setPage(1); }, options: [
                            { value: 'draft', label: '待审' },
                            { value: 'approved', label: '已通过' },
                            { value: 'rejected', label: '已驳回' },
                        ] }), _jsx(Select, { style: { width: 120 }, allowClear: true, placeholder: "\u9898\u578B", value: questionType, onChange: (v) => { setQuestionType(v); setPage(1); }, options: Object.entries(TYPE_LABEL).map(([v, l]) => ({ value: v, label: l })) }), _jsx(Button, { onClick: () => bulkReview(true), children: "\u6279\u91CF\u901A\u8FC7" }), _jsx(Button, { danger: true, onClick: () => bulkReview(false), children: "\u6279\u91CF\u9A73\u56DE" })] }), _jsx(Table, { rowKey: "id", size: "small", rowSelection: { selectedRowKeys: selectedKeys, onChange: (keys) => setSelectedKeys(keys) }, expandable: {
                    // 选项是重要审核内容：行展开显示全部选项，正确答案高亮（客观题才有选项）
                    expandedRowRender: (r) => {
                        const opts = parseDraftOptions(r.options);
                        return (_jsx("div", { style: { display: 'grid', gap: 4, padding: '4px 0' }, children: opts.map((o) => (_jsxs("div", { style: { color: r.answer?.includes(o.label) ? '#389e0d' : undefined }, children: [_jsxs("strong", { children: [o.label, "."] }), " ", o.content, r.answer?.includes(o.label) && (_jsx(Tag, { color: "success", style: { marginLeft: 8 }, children: "\u6B63\u786E\u7B54\u6848" }))] }, o.label))) }));
                    },
                    rowExpandable: (r) => parseDraftOptions(r.options).length > 0,
                }, loading: draftsQuery.isLoading, dataSource: draftsQuery.data?.items ?? [], columns: columns, pagination: {
                    current: page,
                    pageSize,
                    total: draftsQuery.data?.total ?? 0,
                    showSizeChanger: true,
                    pageSizeOptions: [20, 30, 40, 50],
                    showTotal: (t) => `共 ${t} 条`,
                    onChange: (p, s) => {
                        setPage(s !== pageSize ? 1 : p);
                        setPageSize(s);
                    },
                } }), _jsx(Drawer, { open: !!editingDraft, width: 560, title: "\u7F16\u8F91\u8349\u7A3F", onClose: () => setEditingDraft(null), extra: _jsx(Button, { type: "primary", onClick: saveDraftEdit, children: "\u4FDD\u5B58" }), children: editingDraft && (_jsxs(Form, { form: editForm, layout: "vertical", children: [_jsx(Form.Item, { name: "content", label: "\u9898\u5E72", children: _jsx(Input.TextArea, { rows: 3 }) }), _jsx(Form.Item, { name: "answer", label: "\u7B54\u6848", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "difficulty", label: "\u96BE\u5EA6\uFF081~5\uFF09", children: _jsx(InputNumber, { min: 1, max: 5 }) }), _jsx(Form.Item, { name: "analysis", label: "\u89E3\u6790", children: _jsx(Input.TextArea, { rows: 2 }) }), _jsx(Form.Item, { name: "referenceAnswer", label: "\u53C2\u8003\u7B54\u6848\uFF08\u4E3B\u89C2\u9898\uFF09", children: _jsx(Input.TextArea, { rows: 3 }) }), _jsx(Form.Item, { name: "scorePoints", label: "\u8BC4\u5206\u8981\u70B9\uFF08\u4E3B\u89C2\u9898\uFF09", children: _jsx(Input.TextArea, { rows: 2 }) }), _jsx(Form.Item, { name: "scoringStandard", label: "\u8BC4\u5206\u6807\u51C6\uFF08\u4E3B\u89C2\u9898\uFF09", children: _jsx(Input.TextArea, { rows: 2 }) })] })) })] }));
};
export default DraftReviewPanel;
