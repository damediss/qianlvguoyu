import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { Modal, Tabs, Form, Input, InputNumber, Select, Switch, Button, Table, Tag, Space, message, Popconfirm, Divider, } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { pageFaqModelConfigs, createFaqModelConfig, updateFaqModelConfig, deleteFaqModelConfig, getFaqWeknoraConfig, saveFaqWeknoraConfig, } from '../../api/exam';
/** WeKnora & LLM 配置弹层 */
export function FaqConfigDialog({ open, onClose }) {
    const [activeTab, setActiveTab] = useState('webhook');
    const [weknoraForm] = Form.useForm();
    const [modelForm] = Form.useForm();
    const [models, setModels] = useState([]);
    const [modelLoading, setModelLoading] = useState(false);
    const [editingModel, setEditingModel] = useState(null);
    const [modelModalOpen, setModelModalOpen] = useState(false);
    const [savingWeknora, setSavingWeknora] = useState(false);
    const [savingModel, setSavingModel] = useState(false);
    // 加载 WeKnora 配置
    const loadWeknoraConfig = async () => {
        try {
            const config = await getFaqWeknoraConfig();
            weknoraForm.setFieldsValue({
                webhookSecret: config.webhookSecretMasked?.startsWith('***') ? '' : config.webhookSecretMasked,
                knowledgeBaseId: config.knowledgeBaseId,
                faqSetId: config.faqSetId,
                webhookEnabled: config.webhookEnabled === 1,
                autoExport: config.autoExport === 1,
            });
        }
        catch {
            // 首次可能无配置
        }
    };
    // 加载模型列表
    const loadModels = async () => {
        setModelLoading(true);
        try {
            const res = await pageFaqModelConfigs({ page: 1, pageSize: 50 });
            setModels(res.items);
        }
        catch {
            // silent
        }
        finally {
            setModelLoading(false);
        }
    };
    useEffect(() => {
        if (open) {
            loadWeknoraConfig();
            loadModels();
        }
    }, [open]);
    // 保存 WeKnora 配置
    const handleSaveWeknora = async () => {
        try {
            const values = await weknoraForm.validateFields();
            setSavingWeknora(true);
            await saveFaqWeknoraConfig({
                webhookSecret: values.webhookSecret || undefined,
                knowledgeBaseId: values.knowledgeBaseId || undefined,
                faqSetId: values.faqSetId || undefined,
                webhookEnabled: values.webhookEnabled ? 1 : 0,
                autoExport: values.autoExport ? 1 : 0,
            });
            message.success('WeKnora 配置已保存');
        }
        catch (e) {
            message.error(e.message || '保存失败');
        }
        finally {
            setSavingWeknora(false);
        }
    };
    // 保存模型配置
    const handleSaveModel = async () => {
        try {
            const values = await modelForm.validateFields();
            setSavingModel(true);
            if (editingModel) {
                await updateFaqModelConfig(editingModel.id, values);
                message.success('模型配置已更新');
            }
            else {
                await createFaqModelConfig({ ...values, status: values.status ?? 1 });
                message.success('模型配置已创建');
            }
            setModelModalOpen(false);
            setEditingModel(null);
            loadModels();
        }
        catch (e) {
            message.error(e.message || '保存失败');
        }
        finally {
            setSavingModel(false);
        }
    };
    // 删除模型
    const handleDeleteModel = async (id) => {
        try {
            await deleteFaqModelConfig(id);
            message.success('已删除');
            loadModels();
        }
        catch (e) {
            message.error(e.message || '删除失败');
        }
    };
    // 新增模型
    const handleAddModel = () => {
        setEditingModel(null);
        modelForm.resetFields();
        modelForm.setFieldsValue({ apiFormat: 'openai_compatible', temperature: 0.7, maxTokens: 2000, status: 1 });
        setModelModalOpen(true);
    };
    // 编辑模型
    const handleEditModel = (record) => {
        setEditingModel(record);
        modelForm.setFieldsValue(record);
        setModelModalOpen(true);
    };
    // 模型表格列
    const modelColumns = [
        { title: '编码', dataIndex: 'modelCode', width: 120 },
        { title: '名称', dataIndex: 'modelName', width: 120 },
        { title: 'API 格式', dataIndex: 'apiFormat', width: 100 },
        { title: '模型ID', dataIndex: 'modelId', width: 120 },
        { title: 'API Key', dataIndex: 'apiKeyMasked', width: 140 },
        {
            title: '状态', dataIndex: 'status', width: 80,
            render: (s) => _jsx(Tag, { color: s === 'enabled' ? 'green' : 'red', children: s === 'enabled' ? '启用' : '禁用' }),
        },
        {
            title: '操作', key: 'actions', width: 120,
            render: (_, row) => (_jsxs(Space, { children: [_jsx(Button, { size: "small", type: "link", icon: _jsx(EditOutlined, {}), onClick: () => handleEditModel(row), children: "\u7F16\u8F91" }), _jsx(Popconfirm, { title: "\u786E\u5B9A\u5220\u9664\uFF1F", onConfirm: () => handleDeleteModel(row.id), children: _jsx(Button, { size: "small", type: "link", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) })] })),
        },
    ];
    return (_jsxs(Modal, { title: "WeKnora & LLM \u914D\u7F6E", width: 800, open: open, onCancel: onClose, footer: null, destroyOnClose: true, children: [_jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                    {
                        key: 'webhook',
                        label: 'Webhook 回调',
                        children: (_jsxs(Form, { form: weknoraForm, layout: "vertical", children: [_jsxs("div", { style: { background: '#f5f5f5', padding: 12, borderRadius: 6, marginBottom: 16 }, children: [_jsx("strong", { children: "\u56DE\u8C03\u5730\u5740\uFF08\u53EA\u8BFB\uFF0C\u586B\u5165 WeKnora \u540E\u53F0\uFF09\uFF1A" }), _jsx(Input, { value: `${window.location.origin}/api/v1/exam/faq/webhook/weknora`, readOnly: true }), _jsx("p", { style: { fontSize: 12, color: '#888', marginTop: 4 }, children: "URL \u56FA\u5B9A\u4E0D\u968F\u79DF\u6237\u53D8\u5316\uFF0C\u79DF\u6237\u4FE1\u606F\u4ECE\u7B7E\u540D body \u4E2D\u89E3\u6790" })] }), _jsx(Form.Item, { name: "webhookSecret", label: "\u7B7E\u540D\u5BC6\u94A5\uFF08\u53CC\u65B9\u5171\u4EAB\uFF09", children: _jsx(Input.Password, { placeholder: "\u8F93\u5165 Webhook \u7B7E\u540D\u5BC6\u94A5" }) }), _jsx(Divider, {}), _jsx(Form.Item, { name: "knowledgeBaseId", label: "\u77E5\u8BC6\u5E93 ID", children: _jsx(Input, { placeholder: "WeKnora \u77E5\u8BC6\u5E93 ID" }) }), _jsx(Form.Item, { name: "faqSetId", label: "\u6570\u636E\u96C6 ID", children: _jsx(Input, { placeholder: "FAQ \u6570\u636E\u96C6 ID" }) }), _jsx(Form.Item, { name: "webhookEnabled", label: "Webhook \u63A5\u6536\u5F00\u5173", valuePropName: "checked", children: _jsx(Switch, {}) }), _jsx(Form.Item, { name: "autoExport", label: "\u8F6C\u6362\u6210\u529F\u540E\u81EA\u52A8\u5BFC\u51FA", valuePropName: "checked", children: _jsx(Switch, {}) }), _jsx(Button, { type: "primary", loading: savingWeknora, onClick: handleSaveWeknora, children: "\u4FDD\u5B58\u914D\u7F6E" })] })),
                    },
                    {
                        key: 'models',
                        label: '模型管理',
                        children: (_jsxs(_Fragment, { children: [_jsx("div", { style: { marginBottom: 12, textAlign: 'right' }, children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: handleAddModel, children: "\u65B0\u5EFA\u6A21\u578B" }) }), _jsx(Table, { rowKey: "id", size: "small", loading: modelLoading, dataSource: models, columns: modelColumns, pagination: false })] })),
                    },
                ] }), _jsx(Modal, { title: editingModel ? '编辑模型配置' : '新建模型配置', open: modelModalOpen, onCancel: () => { setModelModalOpen(false); setEditingModel(null); }, confirmLoading: savingModel, onOk: handleSaveModel, destroyOnClose: true, children: _jsxs(Form, { form: modelForm, layout: "vertical", children: [_jsx(Form.Item, { name: "modelCode", label: "\u6A21\u578B\u7F16\u7801", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u5982 deepseek-v3", disabled: !!editingModel }) }), _jsx(Form.Item, { name: "modelName", label: "\u663E\u793A\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u5982 DeepSeek V3" }) }), _jsx(Form.Item, { name: "baseUrl", label: "API Base URL", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u5982 https://api.deepseek.com/v1" }) }), _jsx(Form.Item, { name: "apiKey", label: "API Key", rules: [{ required: !editingModel }], children: _jsx(Input.Password, { placeholder: "\u8F93\u5165 API Key" }) }), _jsx(Form.Item, { name: "apiFormat", label: "API \u683C\u5F0F", rules: [{ required: true }], children: _jsx(Select, { options: [{ label: 'OpenAI 兼容', value: 'openai_compatible' }, { label: 'Anthropic', value: 'anthropic' }, { label: '自定义', value: 'custom' }] }) }), _jsx(Form.Item, { name: "modelId", label: "\u6A21\u578B ID", rules: [{ required: true }], children: _jsx(Input, { placeholder: "\u5982 deepseek-chat" }) }), _jsx(Form.Item, { name: "temperature", label: "\u91C7\u6837\u6E29\u5EA6", initialValue: 0.7, children: _jsx(InputNumber, { min: 0, max: 2, step: 0.1, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "maxTokens", label: "\u6700\u5927\u8F93\u51FA Token", initialValue: 2000, children: _jsx(InputNumber, { min: 100, max: 32000, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", initialValue: 1, children: _jsx(Select, { options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }] }) })] }) })] }));
}
