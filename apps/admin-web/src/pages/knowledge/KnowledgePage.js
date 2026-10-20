import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ApartmentOutlined, CheckCircleOutlined, CloudUploadOutlined, DatabaseOutlined, FileProtectOutlined, FileSearchOutlined, FileTextOutlined, LockOutlined, PlusOutlined, ReloadOutlined, RobotOutlined, SafetyCertificateOutlined, SearchOutlined, } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Alert, Button, Drawer, Empty, Form, Input, InputNumber, Modal, Select, Space, Statistic, Table, Tabs, Tag, Timeline, Tooltip, Typography, Upload, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import { createAnswerDraft, createSpace, deleteSpace, ingestDocument, pageDocuments, pageSpaces, searchKnowledge, uploadFile, } from '../../api/knowledge';
const scopeOptions = [
    { label: '企业知识库', value: 'enterprise' },
    { label: '部门知识库', value: 'department' },
    { label: '课程知识库', value: 'course' },
];
const roleOptions = [
    { label: '平台管理员', value: 'PLATFORM_ADMIN' },
    { label: '培训负责人', value: 'TRAINING_MANAGER' },
    { label: '课程人员', value: 'COURSE_STAFF' },
    { label: '企业管理员', value: 'ENTERPRISE_ADMIN' },
];
const categoryOptions = [
    { label: '制度流程', value: 'policy' },
    { label: '课程资料', value: 'course' },
    { label: '合同资料', value: 'contract' },
    { label: '项目资料', value: 'project' },
    { label: '客户资料', value: 'customer' },
];
function scopeLabel(value) {
    return scopeOptions.find((item) => item.value === value)?.label || value || '-';
}
function categoryLabel(value) {
    return categoryOptions.find((item) => item.value === value)?.label || value || '-';
}
function roleLabel(value) {
    return roleOptions.find((item) => item.value === value)?.label || value || value;
}
function statusTag(status) {
    if (status === 'enabled' || status === 'indexed') {
        return _jsx(Tag, { color: "success", children: status === 'enabled' ? '已启用' : '已索引' });
    }
    if (status === 'failed') {
        return _jsx(Tag, { color: "error", children: "\u5931\u8D25" });
    }
    return _jsx(Tag, { color: "processing", children: status || '-' });
}
export function KnowledgePage() {
    const queryClient = useQueryClient();
    const [activeTab, setActiveTab] = useState('spaces');
    const [keyword, setKeyword] = useState('');
    const [selectedSpace, setSelectedSpace] = useState();
    const [spaceOpen, setSpaceOpen] = useState(false);
    const [ingestOpen, setIngestOpen] = useState(false);
    const [uploadedFileId, setUploadedFileId] = useState();
    const [uploadList, setUploadList] = useState([]);
    const [searchResults, setSearchResults] = useState([]);
    const [answerDraft, setAnswerDraft] = useState();
    const [spaceForm] = Form.useForm();
    const [ingestForm] = Form.useForm();
    const [searchForm] = Form.useForm();
    const spacesQuery = useQuery({
        queryKey: ['knowledge-spaces', keyword],
        queryFn: () => pageSpaces({ page: 1, pageSize: 50, keyword }),
    });
    const spaces = spacesQuery.data?.items || [];
    const currentSpace = selectedSpace || spaces[0];
    const documentsQuery = useQuery({
        queryKey: ['knowledge-documents', currentSpace?.id],
        queryFn: () => pageDocuments(currentSpace.id, { page: 1, pageSize: 20 }),
        enabled: Boolean(currentSpace?.id),
    });
    const createSpaceMutation = useMutation({
        mutationFn: createSpace,
        onSuccess: (space) => {
            message.success('知识库已创建');
            setSelectedSpace(space);
            setSpaceOpen(false);
            spaceForm.resetFields();
            queryClient.invalidateQueries({ queryKey: ['knowledge-spaces'] });
        },
    });
    const deleteSpaceMutation = useMutation({
        mutationFn: deleteSpace,
        onSuccess: () => {
            message.success('知识库已删除');
            setSelectedSpace(undefined);
            queryClient.invalidateQueries({ queryKey: ['knowledge-spaces'] });
        },
    });
    const ingestMutation = useMutation({
        mutationFn: ({ spaceId, payload }) => ingestDocument(spaceId, payload),
        onSuccess: (document) => {
            message.success(document.status === 'indexed' ? '资料已入库' : '资料入库失败，请查看错误信息');
            setIngestOpen(false);
            setUploadedFileId(undefined);
            setUploadList([]);
            ingestForm.resetFields();
            queryClient.invalidateQueries({ queryKey: ['knowledge-documents'] });
        },
    });
    const searchMutation = useMutation({
        mutationFn: searchKnowledge,
        onSuccess: setSearchResults,
    });
    const answerMutation = useMutation({
        mutationFn: createAnswerDraft,
        onSuccess: setAnswerDraft,
    });
    const uploadProps = {
        fileList: uploadList,
        maxCount: 1,
        beforeUpload: async (file) => {
            const uploaded = await uploadFile(file);
            setUploadedFileId(uploaded.id);
            setUploadList([{ uid: String(uploaded.id), name: uploaded.originalName, status: 'done' }]);
            ingestForm.setFieldsValue({ title: uploaded.originalName });
            message.success('文件已上传');
            return Upload.LIST_IGNORE;
        },
        onRemove: () => {
            setUploadedFileId(undefined);
            setUploadList([]);
        },
    };
    const documentRows = documentsQuery.data?.items || [];
    const indexedCount = useMemo(() => documentRows.filter((doc) => doc.indexStatus === 'indexed').length, [documentRows]);
    const sensitiveCount = useMemo(() => documentRows.filter((doc) => doc.accessScope === 'restricted').length, [documentRows]);
    const roleCount = currentSpace?.visibleRoleCodes.length || 0;
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(DatabaseOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u4F01\u4E1A\u77E5\u8BC6\u5E93" })] }), _jsx(Typography.Text, { children: "\u7EDF\u4E00 RAG \u5E95\u5EA7\uFF0C\u6309\u79DF\u6237\u3001\u7A7A\u95F4\u548C\u6587\u6863\u7EA7 ACL \u7BA1\u7406\u8D44\u6599\u5165\u5E93\u3001\u68C0\u7D22\u5F15\u7528\u4E0E AI \u8349\u7A3F\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ['knowledge-spaces'] }), children: "\u5237\u65B0" }), _jsx(Button, { icon: _jsx(CloudUploadOutlined, {}), disabled: !currentSpace, onClick: () => setIngestOpen(true), children: "\u4E0A\u4F20\u8D44\u6599" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => setSpaceOpen(true), children: "\u65B0\u5EFA\u77E5\u8BC6\u5E93" })] })] }), _jsxs("div", { className: "knowledge-overview", children: [_jsx(ProCard, { className: "overview-card", bordered: true, children: _jsx(Statistic, { title: "\u77E5\u8BC6\u5E93\u7A7A\u95F4", value: spaces.length, prefix: _jsx(ApartmentOutlined, {}) }) }), _jsx(ProCard, { className: "overview-card", bordered: true, children: _jsx(Statistic, { title: "\u5F53\u524D\u8D44\u6599", value: documentRows.length, prefix: _jsx(FileTextOutlined, {}) }) }), _jsx(ProCard, { className: "overview-card", bordered: true, children: _jsx(Statistic, { title: "\u5DF2\u7D22\u5F15\u8D44\u6599", value: indexedCount, prefix: _jsx(CheckCircleOutlined, {}) }) }), _jsx(ProCard, { className: "overview-card", bordered: true, children: _jsx(Statistic, { title: "\u53D7\u63A7\u8BBF\u95EE", value: sensitiveCount, suffix: `/ ${roleCount} 角色`, prefix: _jsx(LockOutlined, {}) }) })] }), _jsxs("div", { className: "knowledge-grid", children: [_jsx("main", { className: "knowledge-main", children: _jsxs(ProCard, { bordered: true, className: "work-section pro-work-section", title: "\u77E5\u8BC6\u5E93\u5DE5\u4F5C\u53F0", extra: _jsx(Input, { className: "section-search", prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u77E5\u8BC6\u5E93\u540D\u79F0\u6216\u7F16\u7801", allowClear: true, onChange: (event) => setKeyword(event.target.value) }), children: [_jsx(Tabs, { className: "work-tabs", activeKey: activeTab, onChange: setActiveTab, items: [
                                        { key: 'spaces', label: '知识库空间' },
                                        { key: 'ingest', label: '资料入库' },
                                        { key: 'search', label: '检索问答' },
                                    ] }), activeTab === 'spaces' && (_jsx(Table, { rowKey: "id", size: "middle", loading: spacesQuery.isLoading, dataSource: spaces, pagination: false, onRow: (record) => ({
                                        onClick: () => setSelectedSpace(record),
                                    }), rowClassName: (record) => (record.id === currentSpace?.id ? 'selected-row' : ''), columns: [
                                        {
                                            title: '知识库',
                                            dataIndex: 'name',
                                            render: (text, record) => (_jsxs("div", { className: "name-cell", children: [_jsx("span", { className: "name-cell-icon", children: _jsx(DatabaseOutlined, {}) }), _jsxs("span", { children: [_jsx(Typography.Text, { strong: true, children: text }), _jsx(Typography.Text, { type: "secondary", children: record.code })] })] })),
                                        },
                                        { title: '空间类型', dataIndex: 'scopeType', width: 120, render: scopeLabel },
                                        {
                                            title: '可见角色',
                                            dataIndex: 'visibleRoleCodes',
                                            render: (roles) => (_jsx(Space, { size: [4, 4], wrap: true, children: roles?.map((role) => _jsx(Tag, { children: roleLabel(role) }, role)) })),
                                        },
                                        { title: '状态', dataIndex: 'status', width: 104, render: statusTag },
                                        {
                                            title: '更新时间',
                                            dataIndex: 'updatedAt',
                                            width: 156,
                                            render: (value) => dayjs(value).format('YYYY-MM-DD HH:mm'),
                                        },
                                        {
                                            title: '操作',
                                            width: 90,
                                            render: (_, record) => (_jsx(Button, { danger: true, type: "link", size: "small", onClick: (event) => {
                                                    event.stopPropagation();
                                                    Modal.confirm({
                                                        title: '删除知识库',
                                                        content: `确认删除 ${record.name}？`,
                                                        onOk: () => deleteSpaceMutation.mutate(record.id),
                                                    });
                                                }, children: "\u5220\u9664" })),
                                        },
                                    ] })), activeTab === 'ingest' && (_jsxs(_Fragment, { children: [_jsxs("div", { className: "section-title-row", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 3, children: "\u8D44\u6599\u5165\u5E93" }), _jsx(Typography.Text, { type: "secondary", children: "\u4E0A\u4F20\u540E\u89E3\u6790\u3001\u5207\u7247\u5E76\u5EFA\u7ACB\u7D22\u5F15\u4EFB\u52A1\uFF0C\u654F\u611F\u8D44\u6599\u5FC5\u987B\u9650\u5236\u6587\u6863\u53EF\u89C1\u8303\u56F4\u3002" })] }), _jsx(Button, { type: "primary", icon: _jsx(CloudUploadOutlined, {}), disabled: !currentSpace, onClick: () => setIngestOpen(true), children: "\u4E0A\u4F20\u5230\u5F53\u524D\u77E5\u8BC6\u5E93" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: documentsQuery.isLoading, dataSource: documentRows, pagination: false, locale: { emptyText: _jsx(Empty, { description: "\u5F53\u524D\u77E5\u8BC6\u5E93\u8FD8\u6CA1\u6709\u8D44\u6599" }) }, columns: [
                                                {
                                                    title: '资料',
                                                    dataIndex: 'title',
                                                    render: (text, record) => (_jsxs("div", { className: "name-cell", children: [_jsx("span", { className: "name-cell-icon file", children: _jsx(FileProtectOutlined, {}) }), _jsxs("span", { children: [_jsx(Typography.Text, { strong: true, children: text }), _jsxs(Typography.Text, { type: "secondary", children: ["fileId ", record.fileId] })] })] })),
                                                },
                                                { title: '分类', dataIndex: 'categoryCode', width: 110, render: categoryLabel },
                                                {
                                                    title: '权限',
                                                    dataIndex: 'accessScope',
                                                    width: 130,
                                                    render: (value, record) => value === 'restricted' ? (_jsx(Tag, { icon: _jsx(LockOutlined, {}), color: "error", children: record.sensitiveLevel })) : (_jsx(Tag, { color: "processing", children: "\u7EE7\u627F\u7A7A\u95F4" })),
                                                },
                                                { title: '切片数', dataIndex: 'chunkCount', width: 90 },
                                                { title: '索引', dataIndex: 'indexStatus', width: 110, render: statusTag },
                                                {
                                                    title: '更新时间',
                                                    dataIndex: 'updatedAt',
                                                    width: 156,
                                                    render: (value) => dayjs(value).format('YYYY-MM-DD HH:mm'),
                                                },
                                            ] })] })), activeTab === 'search' && (_jsxs("section", { className: "search-workspace", children: [_jsx(Form, { form: searchForm, layout: "vertical", initialValues: { topK: 5, query: '合同金额' }, onFinish: (values) => {
                                                const payload = {
                                                    query: values.query,
                                                    question: values.query,
                                                    spaceIds: currentSpace ? [currentSpace.id] : undefined,
                                                    topK: values.topK,
                                                };
                                                searchMutation.mutate(payload);
                                                answerMutation.mutate(payload);
                                            }, children: _jsxs("div", { className: "search-command", children: [_jsx(Form.Item, { name: "query", label: "\u68C0\u7D22\u95EE\u9898", rules: [{ required: true }], className: "search-input-item", children: _jsx(Input, { prefix: _jsx(FileSearchOutlined, {}), placeholder: "\u8F93\u5165\u8981\u67E5\u8BE2\u7684\u95EE\u9898\uFF0C\u4F8B\u5982\uFF1A\u5408\u540C\u91D1\u989D\u662F\u591A\u5C11\uFF1F" }) }), _jsx(Form.Item, { name: "topK", label: "\u7247\u6BB5\u6570", children: _jsx(InputNumber, { min: 1, max: 20 }) }), _jsx(Button, { type: "primary", htmlType: "submit", loading: searchMutation.isPending || answerMutation.isPending, children: "\u68C0\u7D22\u5E76\u751F\u6210\u8349\u7A3F" })] }) }), _jsxs("div", { className: "search-result-grid", children: [_jsxs("div", { className: "result-list", children: [_jsx(Typography.Title, { level: 4, children: "\u5F15\u7528\u7247\u6BB5" }), searchResults.length === 0 ? (_jsx(Empty, { description: "\u6682\u65E0\u68C0\u7D22\u7ED3\u679C" })) : searchResults.map((item) => (_jsxs("article", { className: "citation-row", children: [_jsx("div", { className: "citation-score", children: item.score.toFixed(0) }), _jsxs("div", { children: [_jsx(Typography.Text, { strong: true, children: item.citationLabel }), _jsx("p", { children: item.content })] })] }, item.chunkId)))] }), _jsxs("div", { className: "answer-panel", children: [_jsxs("div", { className: "answer-title", children: [_jsx(RobotOutlined, {}), _jsx(Typography.Title, { level: 4, children: "\u95EE\u7B54\u8349\u7A3F" })] }), answerDraft ? (_jsxs(_Fragment, { children: [_jsx(Alert, { type: answerDraft.evidenceSufficient ? 'success' : 'warning', showIcon: true, message: answerDraft.evidenceSufficient ? 'AI 生成，仅供人工确认' : '证据不足' }), _jsx(Typography.Paragraph, { children: answerDraft.answer })] })) : (_jsx(Empty, { description: "\u63D0\u4EA4\u95EE\u9898\u540E\u751F\u6210\u8349\u7A3F" }))] })] })] }))] }) }), _jsx("aside", { className: "knowledge-inspector", children: currentSpace ? (_jsxs(_Fragment, { children: [_jsxs("div", { className: "inspector-title", children: [_jsx(DatabaseOutlined, {}), _jsxs("div", { children: [_jsx(Typography.Title, { level: 3, children: currentSpace.name }), _jsx(Typography.Text, { type: "secondary", children: scopeLabel(currentSpace.scopeType) })] })] }), _jsxs("div", { className: "inspector-actions", children: [_jsx(Tooltip, { title: "\u5411\u5F53\u524D\u77E5\u8BC6\u5E93\u4E0A\u4F20\u8D44\u6599", children: _jsx(Button, { type: "primary", icon: _jsx(CloudUploadOutlined, {}), disabled: !currentSpace, onClick: () => setIngestOpen(true), children: "\u4E0A\u4F20\u8D44\u6599" }) }), _jsx(Button, { icon: _jsx(FileSearchOutlined, {}), onClick: () => setActiveTab('search'), children: "\u68C0\u7D22\u95EE\u7B54" })] }), _jsxs("div", { className: "metric-strip", children: [_jsxs("div", { children: [_jsx("strong", { children: documentRows.length }), _jsx("span", { children: "\u8D44\u6599" })] }), _jsxs("div", { children: [_jsx("strong", { children: sensitiveCount }), _jsx("span", { children: "\u654F\u611F" })] }), _jsxs("div", { children: [_jsx("strong", { children: roleCount }), _jsx("span", { children: "\u89D2\u8272" })] })] }), _jsxs("section", { className: "permission-section", children: [_jsx(Typography.Title, { level: 4, children: "\u6743\u9650\u6A21\u578B" }), _jsxs(Space, { direction: "vertical", size: 8, children: [_jsx(Tag, { icon: _jsx(SafetyCertificateOutlined, {}), color: "success", children: "\u79DF\u6237\u9694\u79BB" }), _jsxs(Tag, { color: "processing", children: ["\u7A7A\u95F4\u89D2\u8272\uFF1A", currentSpace.visibleRoleCodes.map(roleLabel).join(' / ') || '仅负责人'] }), _jsx(Tag, { icon: _jsx(LockOutlined, {}), color: "error", children: "\u654F\u611F\u6587\u6863\uFF1Arestricted + \u6587\u6863 ACL" })] })] }), _jsx(Timeline, { className: "index-timeline", items: [
                                        { color: 'green', children: '文件由平台文件服务统一管理' },
                                        { color: 'green', children: 'Tika 解析文本并按段落切片' },
                                        { color: 'blue', children: '向量库字段已预留，后续接 Qdrant' },
                                        { color: 'gray', children: 'AI 草稿必须人工确认' },
                                    ] })] })) : (_jsx(Empty, { description: "\u8BF7\u9009\u62E9\u6216\u521B\u5EFA\u77E5\u8BC6\u5E93" })) })] }), _jsx(Drawer, { title: "\u65B0\u5EFA\u77E5\u8BC6\u5E93", open: spaceOpen, width: 480, onClose: () => setSpaceOpen(false), destroyOnHidden: true, children: _jsxs(Form, { form: spaceForm, layout: "vertical", initialValues: { scopeType: 'enterprise', status: 'enabled', visibleRoleCodes: ['PLATFORM_ADMIN'] }, onFinish: (values) => createSpaceMutation.mutate(values), children: [_jsx(Form.Item, { name: "name", label: "\u77E5\u8BC6\u5E93\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u4F8B\u5982\uFF1A\u4F01\u4E1A\u5236\u5EA6\u77E5\u8BC6\u5E93" }) }), _jsx(Form.Item, { name: "code", label: "\u77E5\u8BC6\u5E93\u7F16\u7801", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u4F8B\u5982\uFF1Aenterprise-policy" }) }), _jsx(Form.Item, { name: "scopeType", label: "\u7A7A\u95F4\u7C7B\u578B", rules: [{ required: true }], children: _jsx(Select, { options: scopeOptions }) }), _jsx(Form.Item, { name: "categoryCode", label: "\u9ED8\u8BA4\u5206\u7C7B", children: _jsx(Select, { allowClear: true, options: categoryOptions }) }), _jsx(Form.Item, { name: "visibleRoleCodes", label: "\u53EF\u89C1\u89D2\u8272", children: _jsx(Select, { mode: "multiple", options: roleOptions }) }), _jsx(Form.Item, { name: "description", label: "\u8BF4\u660E", children: _jsx(Input.TextArea, { rows: 3 }) }), _jsx(Button, { type: "primary", htmlType: "submit", loading: createSpaceMutation.isPending, block: true, children: "\u521B\u5EFA" })] }) }), _jsx(Drawer, { title: "\u4E0A\u4F20\u8D44\u6599\u5165\u5E93", open: ingestOpen, width: 520, onClose: () => setIngestOpen(false), destroyOnHidden: true, children: _jsxs(Form, { form: ingestForm, layout: "vertical", initialValues: {
                        sourceType: 'file',
                        categoryCode: currentSpace?.categoryCode,
                        accessScope: 'inherit',
                        sensitiveLevel: 'normal',
                        visibleRoleCodes: ['PLATFORM_ADMIN'],
                    }, onFinish: (values) => {
                        if (!currentSpace || !uploadedFileId) {
                            message.warning('请先选择知识库并上传文件');
                            return;
                        }
                        ingestMutation.mutate({
                            spaceId: currentSpace.id,
                            payload: {
                                ...values,
                                fileId: uploadedFileId,
                                visibleUserIds: values.visibleUserIdsText
                                    ? String(values.visibleUserIdsText).split(',').map((item) => Number(item.trim())).filter(Boolean)
                                    : undefined,
                            },
                        });
                    }, children: [_jsxs(Upload.Dragger, { ...uploadProps, className: "upload-zone", children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(CloudUploadOutlined, {}) }), _jsx("p", { className: "ant-upload-text", children: "\u4E0A\u4F20 PDF\u3001Word\u3001Excel\u3001PPT\u3001Markdown \u6216\u6587\u672C\u8D44\u6599" })] }), _jsx(Form.Item, { name: "title", label: "\u8D44\u6599\u6807\u9898", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "categoryCode", label: "\u8D44\u6599\u5206\u7C7B", children: _jsx(Select, { allowClear: true, options: categoryOptions }) }), _jsx(Form.Item, { name: "accessScope", label: "\u8BBF\u95EE\u8303\u56F4", rules: [{ required: true }], children: _jsx(Select, { options: [
                                    { label: '继承知识库空间权限', value: 'inherit' },
                                    { label: '敏感资料，仅文档 ACL 可见', value: 'restricted' },
                                ] }) }), _jsx(Form.Item, { name: "sensitiveLevel", label: "\u654F\u611F\u7B49\u7EA7", children: _jsx(Select, { options: [
                                    { label: '普通', value: 'normal' },
                                    { label: '内部', value: 'internal' },
                                    { label: '保密', value: 'confidential' },
                                ] }) }), _jsx(Form.Item, { name: "visibleUserIdsText", label: "\u7B7E\u7F72\u4EBA/\u8D1F\u8D23\u4EBA\u7528\u6237 ID", children: _jsx(Input, { placeholder: "\u591A\u4E2A\u7528\u6237 ID \u7528\u82F1\u6587\u9017\u53F7\u5206\u9694" }) }), _jsx(Form.Item, { name: "visibleRoleCodes", label: "\u6388\u6743\u89D2\u8272", children: _jsx(Select, { mode: "multiple", options: roleOptions }) }), _jsx(Button, { type: "primary", htmlType: "submit", loading: ingestMutation.isPending, block: true, children: "\u5165\u5E93\u5E76\u5EFA\u7ACB\u7D22\u5F15" })] }) })] }));
}
