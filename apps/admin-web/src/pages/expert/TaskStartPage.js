import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useCallback, useEffect } from 'react';
import { Button, Card, Col, Form, Input, Modal, Row, Select, Space, Spin, Table, Tag, Tree, message } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as api from '../../api/expert';
const TAG_COLORS = {
    exam: 'blue',
    examshicao: 'orange',
    examsub: 'purple',
    ximu: 'green',
};
export function TaskStartPage() {
    const [query, setQuery] = useState({ page: 1, pageSize: 20, status: 0 });
    const [auditOpen, setAuditOpen] = useState(false);
    const [auditType, setAuditType] = useState('');
    const [auditTitle, setAuditTitle] = useState('');
    const [auditTaskId, setAuditTaskId] = useState(0);
    const queryClient = useQueryClient();
    const { data, isLoading } = useQuery({
        queryKey: ['expert-start-tasks', query],
        queryFn: () => api.pageStartTasks(query),
    });
    const handleAudit = useCallback((row) => {
        setAuditOpen(true);
        setAuditType(row.tag);
        setAuditTitle(row.title + ' - ' + row.tagCn);
        setAuditTaskId(row.taskId);
    }, []);
    const columns = [
        { title: '名称', dataIndex: 'title', width: 200 },
        {
            title: '类型',
            dataIndex: 'tagCn',
            width: 140,
            render: (_, row) => _jsx(Tag, { color: TAG_COLORS[row.tag] || 'default', children: row.tagCn }),
        },
        { title: '题量', dataIndex: 'nums', width: 80 },
        { title: '指派时间', dataIndex: 'addtimeCn', width: 180 },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (s) => _jsx(Tag, { color: s === '完成' ? 'green' : 'blue', children: s }),
        },
        {
            title: '操作',
            width: 100,
            render: (_, row) => row.endtime === 0 ? (_jsx(Button, { type: "primary", size: "small", onClick: () => handleAudit(row), children: "\u5904\u7406" })) : null,
        },
    ];
    const closeAudit = useCallback(() => {
        setAuditOpen(false);
        setAuditType('');
        queryClient.invalidateQueries({ queryKey: ['expert-start-tasks'] });
    }, [queryClient]);
    return (_jsxs(Card, { children: [_jsx(Space, { style: { marginBottom: 16 }, children: _jsx(Form.Item, { label: "\u72B6\u6001", style: { marginBottom: 0 }, children: _jsx(Select, { value: query.status, style: { width: 150 }, onChange: (v) => setQuery({ ...query, status: v, page: 1 }), options: [
                            { label: '全部', value: 0 },
                            { label: '进行中', value: 1 },
                            { label: '完成', value: 2 },
                        ] }) }) }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: data?.items || [], loading: isLoading, pagination: {
                    current: query.page,
                    pageSize: query.pageSize,
                    total: data?.total || 0,
                    onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }),
                } }), auditOpen && (_jsx(AuditModal, { type: auditType, title: auditTitle, taskId: auditTaskId, role: 0, open: auditOpen, onClose: closeAudit }))] }));
}
function AuditModal({ type, title, taskId, role, open, onClose, }) {
    switch (type) {
        case 'exam':
            return _jsx(ExamAuditPanel, { title: title, taskId: taskId, role: role, open: open, onClose: onClose });
        case 'examshicao':
            return _jsx(ExamshicaoAuditPanel, { title: title, taskId: taskId, role: role, open: open, onClose: onClose });
        case 'examsub':
            return _jsx(ExamsubAuditPanel, { title: title, taskId: taskId, role: role, open: open, onClose: onClose });
        case 'ximu':
            return _jsx(XimuAuditPanel, { title: title, taskId: taskId, role: role, open: open, onClose: onClose });
        default:
            return null;
    }
}
function ExamAuditPanel({ title: panelTitle, taskId, role, open, onClose, }) {
    const [submitting, setSubmitting] = useState(false);
    const { data: list } = useQuery({
        queryKey: ['expert-exam-list', taskId, role],
        queryFn: () => api.getExamList(taskId, role),
        enabled: open,
    });
    const items = list || [];
    const [idx, setIdx] = useState(0);
    const [form] = Form.useForm();
    const item = items[idx];
    const submit = async () => {
        if (submitting || !item)
            return;
        setSubmitting(true);
        const values = await form.validateFields();
        const payload = { id: item.id, taskId, role, ...values };
        const res = await api.submitExam(payload);
        setSubmitting(false);
        if (res.isdone) {
            message.success('审核完成');
            onClose();
        }
        else {
            message.success('提交成功');
            form.resetFields();
            setIdx(idx + 1);
        }
    };
    return (_jsx(Modal, { title: panelTitle + `(${items.length})`, open: open, onCancel: onClose, footer: null, width: 500, children: item ? (_jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { label: "\u9898\u76EE\u7C7B\u578B", children: _jsx(Input, { value: item.exam_type, disabled: true }) }), _jsx(Form.Item, { label: "\u9898\u76EE\u5185\u5BB9", name: "examContent", rules: [{ required: true }], initialValue: item.exam_content, children: _jsx(Input.TextArea, { rows: 3 }) }), item.exam_type === '判断题' ? (_jsx(Form.Item, { label: "\u53C2\u8003\u7B54\u6848", name: "examAnswer", rules: [{ required: true }], initialValue: item.exam_answer, children: _jsx(Select, { options: [{ label: '对', value: 'A' }, { label: '错', value: 'B' }] }) })) : (_jsxs(_Fragment, { children: [['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'].map((l) => {
                            const key = 'exam_' + l.toLowerCase();
                            if (!item[key])
                                return null;
                            return (_jsx(Form.Item, { label: l, name: key, initialValue: item[key], children: _jsx(Input, {}) }, l));
                        }), _jsx(Form.Item, { label: "\u53C2\u8003\u7B54\u6848", name: "examAnswer", rules: [{ required: true }], initialValue: item.exam_answer, children: _jsx(Input, {}) })] })), _jsx(Button, { type: "primary", loading: submitting, onClick: submit, children: "\u63D0\u4EA4" })] })) : (_jsx("p", { children: "\u6682\u65E0\u5BA1\u6838\u6570\u636E" })) }));
}
function ExamshicaoAuditPanel({ title: panelTitle, taskId, role, open, onClose, }) {
    const [submitting, setSubmitting] = useState(false);
    const { data } = useQuery({
        queryKey: ['expert-examshicao-list', taskId, role],
        queryFn: () => api.getExamshicaoList(taskId, role),
        enabled: open,
    });
    const items = data?.list || [];
    const [idx, setIdx] = useState(0);
    const [form] = Form.useForm();
    const item = items[idx];
    useEffect(() => {
        if (item) {
            form.setFieldsValue({
                content: item.content,
                kaochang: item.kaochang,
                kaosheng: item.kaosheng,
                pingfen: item.pingfen || [],
            });
        }
    }, [idx, item, form]);
    const submit = async () => {
        if (submitting || !item)
            return;
        setSubmitting(true);
        const values = await form.validateFields();
        const payload = {
            id: item.id,
            taskId,
            role,
            jianding: item.jianding,
            content: values.content,
            pingfen: JSON.stringify(values.pingfen),
            kaochang: values.kaochang,
            kaosheng: values.kaosheng,
        };
        const res = await api.submitExamshicao(payload);
        setSubmitting(false);
        if (res.isdone) {
            message.success('审核完成');
            onClose();
        }
        else {
            message.success('提交成功');
            form.resetFields();
            setIdx(idx + 1);
        }
    };
    return (_jsx(Modal, { title: panelTitle + `(${items.length})`, open: open, onCancel: onClose, width: 1000, footer: [
            _jsx(Button, { onClick: onClose, children: "\u53D6\u6D88" }, "cancel"),
            _jsx(Button, { type: "primary", loading: submitting, onClick: submit, children: "\u63D0\u4EA4" }, "submit"),
        ], children: item ? (_jsxs(Form, { form: form, layout: "horizontal", labelCol: { flex: '0 0 80px' }, wrapperCol: { flex: 'auto' }, labelAlign: "left", children: [_jsx(Form.Item, { label: "\u8003\u8BC4\u70B9", children: _jsx(Input, { value: item.jianding, disabled: true }) }), _jsx(Form.Item, { label: "\u9898\u76EE\u5185\u5BB9", name: "content", rules: [{ required: true }], children: _jsx(Input.TextArea, { rows: 2, autoSize: { minRows: 2, maxRows: 4 } }) }), _jsx(Form.Item, { label: "\u8003\u573A\u51C6\u5907", name: "kaochang", children: _jsx(Input.TextArea, { rows: 1, autoSize: { minRows: 1, maxRows: 3 } }) }), _jsx(Form.Item, { label: "\u8003\u751F\u51C6\u5907", name: "kaosheng", children: _jsx(Input.TextArea, { rows: 1, autoSize: { minRows: 1, maxRows: 3 } }) }), _jsxs(Form.Item, { label: "\u8BC4\u5206\u6807\u51C6", children: [_jsxs("div", { style: { display: 'flex', gap: 4, marginBottom: 8 }, children: [_jsx(Input, { value: "\u8981\u7D20", disabled: true, style: { flex: 1 } }), _jsx(Input, { value: "\u914D\u5206", disabled: true, style: { width: 80 } }), _jsx(Input, { value: "\u8BC4\u5206\u6807\u51C6", disabled: true, style: { flex: 2 } }), _jsx(Input, { value: "\u6700\u5927\u6263\u5206", disabled: true, style: { width: 80 } }), _jsx(Input, { value: "\u6B65\u957F", disabled: true, style: { width: 60 } })] }), _jsx(Form.List, { name: "pingfen", children: (fields) => (_jsx(_Fragment, { children: fields.map((field) => (_jsxs("div", { style: { display: 'flex', gap: 4, marginBottom: 4 }, children: [_jsx(Form.Item, { ...field, name: [field.name, 'yaosu'], style: { flex: 1, marginBottom: 0 }, children: _jsx(Input, {}) }), _jsx(Form.Item, { ...field, name: [field.name, 'peifen'], style: { width: 80, marginBottom: 0 }, children: _jsx(Input, {}) }), _jsx(Form.Item, { ...field, name: [field.name, 'biaozhun'], style: { flex: 2, marginBottom: 0 }, children: _jsx(Input.TextArea, { rows: 2, autoSize: { minRows: 2, maxRows: 4 } }) }), _jsx(Form.Item, { ...field, name: [field.name, 'koufen'], style: { width: 80, marginBottom: 0 }, children: _jsx(Input, {}) }), _jsx(Form.Item, { ...field, name: [field.name, 'buchang'], style: { width: 60, marginBottom: 0 }, children: _jsx(Input, {}) })] }, field.key))) })) })] })] })) : (_jsx("p", { children: "\u6682\u65E0\u5BA1\u6838\u6570\u636E" })) }));
}
function ExamsubAuditPanel({ title: panelTitle, taskId, role, open, onClose, }) {
    const [submitting, setSubmitting] = useState(false);
    const { data: list } = useQuery({
        queryKey: ['expert-examsub-list', taskId, role],
        queryFn: () => api.getExamsubList(taskId, role),
        enabled: open,
    });
    const items = list || [];
    const [idx, setIdx] = useState(0);
    const [form] = Form.useForm();
    const item = items[idx];
    const submit = async () => {
        if (submitting || !item)
            return;
        setSubmitting(true);
        const values = await form.validateFields();
        const payload = { id: item.id, taskId, role, ...values };
        const res = await api.submitExamsub(payload);
        setSubmitting(false);
        if (res.isdone) {
            message.success('审核完成');
            onClose();
        }
        else {
            message.success('提交成功');
            form.resetFields();
            setIdx(idx + 1);
        }
    };
    return (_jsx(Modal, { title: panelTitle + `(${items.length})`, open: open, onCancel: onClose, footer: null, width: 600, children: item ? (_jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { label: "\u77E5\u8BC6\u70B9", children: _jsx(Input, { value: item.zhishidian, disabled: true }) }), _jsx(Form.Item, { label: "\u9898\u76EE\u5185\u5BB9", name: "content", rules: [{ required: true }], initialValue: item.content, children: _jsx(Input.TextArea, { rows: 3 }) }), _jsx(Form.Item, { label: "\u53C2\u8003\u7B54\u6848", name: "answer", rules: [{ required: true }], initialValue: item.answer, children: _jsx(Input.TextArea, { rows: 4 }) }), _jsx(Button, { type: "primary", loading: submitting, onClick: submit, children: "\u63D0\u4EA4" })] })) : (_jsx("p", { children: "\u6682\u65E0\u5BA1\u6838\u6570\u636E" })) }));
}
function XimuAuditPanel({ title: panelTitle, taskId, role, open, onClose, }) {
    const [submittedIds, setSubmittedIds] = useState(new Set());
    const [snoTitleValues, setSnoTitleValues] = useState(new Map());
    const [submittingIds, setSubmittingIds] = useState(new Set());
    const [treeData, setTreeData] = useState([]);
    const [selectedNode, setSelectedNode] = useState(null);
    const { data, isLoading: treeLoading } = useQuery({
        queryKey: ['expert-ximu-list', taskId, role],
        queryFn: () => api.getXimuList(taskId, role),
        enabled: open,
    });
    useEffect(() => {
        if (data) {
            setTreeData(data.treeData);
            setSubmittedIds(new Set());
            setSnoTitleValues(new Map());
            setSubmittingIds(new Set());
            setSelectedNode(null);
        }
    }, [data]);
    const findNodeById = useCallback((nodes, id) => {
        for (const n of nodes) {
            if (n.id === id)
                return n;
            if (n.children) {
                const found = findNodeById(n.children, id);
                if (found)
                    return found;
            }
        }
        return null;
    }, []);
    const handleSelect = useCallback((payload) => {
        const keys = Array.isArray(payload) ? payload : payload.checked;
        if (keys.length === 0) {
            setSelectedNode(null);
            return;
        }
        const key = keys[keys.length - 1];
        const node = findNodeById(treeData, key);
        if (!node || node.disabled)
            return;
        setSelectedNode(node);
        // init snoTitleValues from list
        if (node?.list) {
            setSnoTitleValues(prev => {
                const m = new Map(prev);
                for (const item of node.list) {
                    if (!m.has(item.id)) {
                        m.set(item.id, item.sno_title);
                    }
                }
                return m;
            });
        }
    }, [treeData, findNodeById]);
    const handleSubmitItem = async (item) => {
        if (submittingIds.has(item.id) || submittedIds.has(item.id))
            return;
        setSubmittingIds(prev => new Set(prev).add(item.id));
        const currentTitle = snoTitleValues.get(item.id) || item.sno_title;
        const payload = {
            id: item.id,
            taskId,
            role,
            sno: item.sno,
            snoItem: item.sno_item,
            snoTitle: currentTitle,
        };
        const res = await api.submitXimu(payload);
        setSubmittingIds(prev => {
            const n = new Set(prev);
            n.delete(item.id);
            return n;
        });
        setSubmittedIds(prev => new Set(prev).add(item.id));
        // check if all items are submitted
        const allSubmitted = new Set(submittedIds);
        allSubmitted.add(item.id);
        const countAll = (nodes) => {
            let total = 0, done = 0;
            for (const n of nodes) {
                if (n.list) {
                    total += n.list.length;
                    done += n.list.filter(li => allSubmitted.has(li.id)).length;
                }
                if (n.children) {
                    const sub = countAll(n.children);
                    total += sub.total;
                    done += sub.done;
                }
            }
            return { total, done };
        };
        const { total, done } = countAll(treeData);
        if (done >= total && total > 0) {
            message.success('审核完成');
            onClose();
        }
        else {
            message.success('提交成功');
        }
    };
    return (_jsx(Modal, { title: panelTitle, open: open, onCancel: onClose, footer: null, width: 1200, destroyOnHidden: true, children: treeLoading ? (_jsx(Spin, { style: { display: 'block', textAlign: 'center', padding: 40 } })) : treeData.length === 0 ? (_jsx("p", { style: { textAlign: 'center', padding: 40 }, children: "\u6682\u65E0\u5BA1\u6838\u6570\u636E" })) : (_jsxs(Row, { gutter: 16, children: [_jsx(Col, { span: 12, style: { maxHeight: '60vh', overflow: 'auto', borderRight: '1px solid #f0f0f0', fontSize: 13 }, children: _jsx(Tree, { treeData: treeData, fieldNames: { title: 'label', key: 'id' }, defaultExpandAll: true, checkable: true, checkStrictly: true, checkedKeys: selectedNode ? [selectedNode.id] : [], onCheck: (checked) => handleSelect(checked), titleRender: (node) => (_jsxs("span", { children: [node.label, node.title ? _jsx("span", { style: { marginLeft: 6, color: '#1677ff' }, children: node.title }) : null] })) }) }), _jsx(Col, { span: 12, children: selectedNode && selectedNode.list && selectedNode.list.length > 0 ? (_jsx("div", { style: { maxHeight: '60vh', overflow: 'auto' }, children: selectedNode.list.map((item) => (_jsxs("div", { style: {
                                border: submittedIds.has(item.id) ? '1px solid #d9f7be' : '1px solid #f0f0f0',
                                background: submittedIds.has(item.id) ? '#f6ffed' : '#fff',
                                padding: 8,
                                marginBottom: 8,
                                borderRadius: 4,
                            }, children: [_jsxs("div", { style: { marginBottom: 4, fontSize: 12, color: '#666' }, children: ["\u7F16\u53F7: ", item.sno_item, submittedIds.has(item.id) && _jsx(Tag, { color: "green", style: { marginLeft: 4 }, children: "\u5DF2\u63D0\u4EA4" })] }), _jsx(Input.TextArea, { rows: 2, style: { fontSize: 12 }, value: snoTitleValues.get(item.id) || item.sno_title, onChange: (e) => setSnoTitleValues(prev => new Map(prev).set(item.id, e.target.value)), disabled: submittedIds.has(item.id) }), _jsx(Button, { type: "primary", size: "small", style: { marginTop: 4 }, loading: submittingIds.has(item.id), onClick: () => handleSubmitItem(item), disabled: submittedIds.has(item.id), children: submittedIds.has(item.id) ? '已提交' : '提交' })] }, item.id))) })) : selectedNode ? (_jsx("p", { style: { color: '#999' }, children: "\u8BE5\u8282\u70B9\u4E0B\u65E0\u5BA1\u6838\u6570\u636E" })) : (_jsx("p", { style: { color: '#999', textAlign: 'center', marginTop: 40 }, children: "\u8BF7\u5728\u5DE6\u4FA7\u6811\u4E2D\u9009\u62E9\u4E00\u4E2A\u53F6\u5B50\u8282\u70B9" })) })] })) }));
}
export default TaskStartPage;
