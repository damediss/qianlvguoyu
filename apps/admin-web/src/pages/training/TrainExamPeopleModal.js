import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Space, Switch, Table, Typography, message, } from 'antd';
import { DeleteOutlined, EditOutlined, IdcardOutlined, ImportOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pageExamPeople, updateExamPeopleGrade, importExamPeopleByIdCard, importExamPeopleFromTrain, batchDeleteExamPeople, } from '../../api/training';
export function TrainExamPeopleModal({ open, examId, examTitle, onClose }) {
    const queryClient = useQueryClient();
    const [page, setPage] = useState(1);
    const [pageSize] = useState(10);
    const [keyword, setKeyword] = useState('');
    const [selectedIds, setSelectedIds] = useState([]);
    const [importOpen, setImportOpen] = useState(false);
    const [importText, setImportText] = useState('');
    const [importFromTrainOpen, setImportFromTrainOpen] = useState(false);
    const [trainId, setTrainId] = useState();
    const [gradeOpen, setGradeOpen] = useState(false);
    const [editing, setEditing] = useState(null);
    const [gradeForm] = Form.useForm();
    const { data, isLoading } = useQuery({
        queryKey: ['train-exam-people', examId, page, pageSize, keyword],
        queryFn: () => pageExamPeople(examId, { page, pageSize, keyword: keyword || undefined }),
        enabled: open && examId > 0,
    });
    const importMu = useMutation({
        mutationFn: () => importExamPeopleByIdCard(examId, importText),
        onSuccess: r => {
            message.success(`导入 ${r.count} 人`);
            setImportOpen(false);
            setImportText('');
            queryClient.invalidateQueries({ queryKey: ['train-exam-people', examId] });
        },
        onError: () => message.error('导入失败'),
    });
    const importTrainMu = useMutation({
        mutationFn: () => importExamPeopleFromTrain(examId, trainId),
        onSuccess: r => {
            message.success(`从培训导入 ${r.count} 人`);
            setImportFromTrainOpen(false);
            setTrainId(undefined);
            queryClient.invalidateQueries({ queryKey: ['train-exam-people', examId] });
        },
        onError: () => message.error('导入失败'),
    });
    const batchDelMu = useMutation({
        mutationFn: () => batchDeleteExamPeople(selectedIds),
        onSuccess: () => {
            message.success('删除成功');
            setSelectedIds([]);
            queryClient.invalidateQueries({ queryKey: ['train-exam-people', examId] });
        },
        onError: () => message.error('删除失败'),
    });
    const gradeMu = useMutation({
        mutationFn: (v) => updateExamPeopleGrade(editing.id, v),
        onSuccess: () => {
            message.success('成绩已更新');
            setGradeOpen(false);
            setEditing(null);
            queryClient.invalidateQueries({ queryKey: ['train-exam-people', examId] });
        },
        onError: () => message.error('更新失败'),
    });
    const openGrade = (p) => {
        setEditing(p);
        gradeForm.setFieldsValue({
            inWhiteList: p.inWhiteList === 1,
            normalScore: p.normalScore,
            score: p.score,
            totalScore: p.totalScore,
        });
        setGradeOpen(true);
    };
    const columns = [
        { title: '姓名', dataIndex: 'realname', width: 120 },
        { title: '身份证号', dataIndex: 'idCardNum', width: 200 },
        { title: '手机', dataIndex: 'phone', width: 130 },
        { title: '工种', dataIndex: 'workTypeId', width: 90 },
        { title: '平时成绩', dataIndex: 'normalScore', width: 100 },
        { title: '考试成绩', dataIndex: 'score', width: 100 },
        { title: '总分', dataIndex: 'totalScore', width: 100 },
        {
            title: '白名单', dataIndex: 'inWhiteList', width: 90,
            render: (v) => (v === 1 ? '是' : '否'),
        },
        {
            title: '操作', key: 'act', width: 100, fixed: 'right',
            render: (_, r) => (_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openGrade(r), children: "\u6539\u6210\u7EE9" })),
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsxs(Modal, { title: `考生管理 — ${examTitle || ''}`, open: open, width: 1100, onCancel: onClose, footer: null, destroyOnHidden: true, children: [_jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u59D3\u540D/\u624B\u673A", style: { width: 220 }, value: keyword, onChange: e => { setKeyword(e.target.value); setPage(1); }, onSearch: v => { setKeyword(v); setPage(1); } }), _jsx(Button, { icon: _jsx(IdcardOutlined, {}), onClick: () => setImportOpen(true), children: "\u8EAB\u4EFD\u8BC1\u5BFC\u5165" }), _jsx(Button, { icon: _jsx(ImportOutlined, {}), onClick: () => setImportFromTrainOpen(true), children: "\u4ECE\u57F9\u8BAD\u5BFC\u5165" }), _jsx(Popconfirm, { title: `确定删除选中的 ${selectedIds.length} 名考生?`, onConfirm: () => batchDelMu.mutate(), disabled: selectedIds.length === 0, children: _jsxs(Button, { danger: true, icon: _jsx(DeleteOutlined, {}), disabled: selectedIds.length === 0, children: ["\u6279\u91CF\u5220\u9664 ", selectedIds.length > 0 && `(${selectedIds.length})`] }) })] }), _jsx(Table, { rowKey: "id", size: "small", loading: isLoading, columns: columns, dataSource: data?.items ?? [], scroll: { x: 1000 }, rowSelection: {
                            selectedRowKeys: selectedIds,
                            onChange: keys => setSelectedIds(keys.map(k => Number(k))),
                        }, pagination: {
                            current: page,
                            pageSize,
                            total: data?.total ?? 0,
                            showSizeChanger: false,
                            showTotal: t => `共 ${t} 条`,
                            position: ['bottomRight'],
                            onChange: p => setPage(p),
                        } })] }), _jsxs(Modal, { title: "\u8EAB\u4EFD\u8BC1\u53F7\u5BFC\u5165\u8003\u751F", open: importOpen, onOk: () => importMu.mutate(), onCancel: () => setImportOpen(false), confirmLoading: importMu.isPending, destroyOnHidden: true, children: [_jsx(Typography.Paragraph, { type: "secondary", children: "\u6BCF\u884C\u4E00\u4E2A 18 \u4F4D\u8EAB\u4EFD\u8BC1\u53F7\uFF0C\u81EA\u52A8\u8DF3\u8FC7\u91CD\u590D\u9879" }), _jsx(Input.TextArea, { rows: 8, value: importText, onChange: e => setImportText(e.target.value), placeholder: '452202200001011234\n110101199001011235\n...' })] }), _jsxs(Modal, { title: "\u4ECE\u57F9\u8BAD\u5BFC\u5165\u8003\u751F", open: importFromTrainOpen, onOk: () => trainId && importTrainMu.mutate(), onCancel: () => { setImportFromTrainOpen(false); setTrainId(undefined); }, confirmLoading: importTrainMu.isPending, destroyOnHidden: true, children: [_jsx(Typography.Paragraph, { type: "secondary", children: "\u9009\u62E9\u4E00\u4E2A\u57F9\u8BAD\uFF0C\u5C06\u8BE5\u57F9\u8BAD\u7684\u6240\u6709\u5B66\u5458\u5BFC\u5165\u5230\u672C\u8003\u8BD5" }), _jsx(InputNumber, { min: 1, style: { width: '100%' }, placeholder: "\u57F9\u8BAD ID\uFF08\u6682\u65F6\u624B\u5DE5\u8F93\u5165\uFF09", value: trainId, onChange: v => setTrainId(v == null ? undefined : Number(v)) })] }), _jsx(Modal, { title: `改成绩 — ${editing?.realname ?? ''}`, open: gradeOpen, onOk: () => gradeForm.submit(), onCancel: () => { setGradeOpen(false); setEditing(null); }, confirmLoading: gradeMu.isPending, destroyOnHidden: true, children: _jsxs(Form, { form: gradeForm, layout: "vertical", onFinish: v => {
                        gradeMu.mutate({
                            inWhiteList: v.inWhiteList ? 1 : 0,
                            normalScore: v.normalScore,
                            score: v.score,
                            totalScore: v.totalScore,
                        });
                    }, children: [_jsx(Form.Item, { name: "inWhiteList", label: "\u767D\u540D\u5355", valuePropName: "checked", children: _jsx(Switch, {}) }), _jsx(Form.Item, { name: "normalScore", label: "\u5E73\u65F6\u6210\u7EE9", children: _jsx(InputNumber, { min: 0, step: 0.5, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "score", label: "\u8003\u8BD5\u6210\u7EE9", children: _jsx(InputNumber, { min: 0, step: 0.5, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "totalScore", label: "\u603B\u5206", children: _jsx(InputNumber, { min: 0, step: 0.5, style: { width: '100%' } }) })] }) })] }));
}
