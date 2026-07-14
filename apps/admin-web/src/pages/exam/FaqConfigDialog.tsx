import { useEffect, useState } from 'react';
import {
  Modal, Tabs, Form, Input, InputNumber, Select, Switch, Button, Table, Tag, Space,
  message, Popconfirm, Divider,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  pageFaqModelConfigs, createFaqModelConfig, updateFaqModelConfig, deleteFaqModelConfig,
  getFaqWeknoraConfig, saveFaqWeknoraConfig,
} from '../../api/exam';
import type { FaqModelConfigVO, FaqWeknoraConfigVO } from '../../types/exam';

interface Props {
  open: boolean;
  onClose: () => void;
}

/** WeKnora & LLM 配置弹层 */
export function FaqConfigDialog({ open, onClose }: Props) {
  const [activeTab, setActiveTab] = useState('webhook');
  const [weknoraForm] = Form.useForm();
  const [modelForm] = Form.useForm();
  const [models, setModels] = useState<FaqModelConfigVO[]>([]);
  const [modelLoading, setModelLoading] = useState(false);
  const [editingModel, setEditingModel] = useState<FaqModelConfigVO | null>(null);
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
    } catch {
      // 首次可能无配置
    }
  };

  // 加载模型列表
  const loadModels = async () => {
    setModelLoading(true);
    try {
      const res = await pageFaqModelConfigs({ page: 1, pageSize: 50 });
      setModels(res.items);
    } catch {
      // silent
    } finally {
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
    } catch (e: any) {
      message.error(e.message || '保存失败');
    } finally {
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
      } else {
        await createFaqModelConfig({ ...values, status: values.status ?? 1 });
        message.success('模型配置已创建');
      }
      setModelModalOpen(false);
      setEditingModel(null);
      loadModels();
    } catch (e: any) {
      message.error(e.message || '保存失败');
    } finally {
      setSavingModel(false);
    }
  };

  // 删除模型
  const handleDeleteModel = async (id: string) => {
    try {
      await deleteFaqModelConfig(id);
      message.success('已删除');
      loadModels();
    } catch (e: any) {
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
  const handleEditModel = (record: FaqModelConfigVO) => {
    setEditingModel(record);
    modelForm.setFieldsValue(record);
    setModelModalOpen(true);
  };

  // 模型表格列
  const modelColumns: ColumnsType<FaqModelConfigVO> = [
    { title: '编码', dataIndex: 'modelCode', width: 120 },
    { title: '名称', dataIndex: 'modelName', width: 120 },
    { title: 'API 格式', dataIndex: 'apiFormat', width: 100 },
    { title: '模型ID', dataIndex: 'modelId', width: 120 },
    { title: 'API Key', dataIndex: 'apiKeyMasked', width: 140 },
    {
      title: '状态', dataIndex: 'status', width: 80,
      render: (s: string) => <Tag color={s === 'enabled' ? 'green' : 'red'}>{s === 'enabled' ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '操作', key: 'actions', width: 120,
      render: (_: unknown, row: FaqModelConfigVO) => (
        <Space>
          <Button size="small" type="link" icon={<EditOutlined />} onClick={() => handleEditModel(row)}>编辑</Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDeleteModel(row.id)}>
            <Button size="small" type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Modal
      title="WeKnora & LLM 配置"
      width={800}
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnClose
    >
      <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
        {
          key: 'webhook',
          label: 'Webhook 回调',
          children: (
            <Form form={weknoraForm} layout="vertical">
              <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6, marginBottom: 16 }}>
                <strong>回调地址（只读，填入 WeKnora 后台）：</strong>
                <Input value={`${window.location.origin}/api/v1/exam/faq/webhook/weknora`} readOnly />
                <p style={{ fontSize: 12, color: '#888', marginTop: 4 }}>
                  URL 固定不随租户变化，租户信息从签名 body 中解析
                </p>
              </div>
              <Form.Item name="webhookSecret" label="签名密钥（双方共享）">
                <Input.Password placeholder="输入 Webhook 签名密钥" />
              </Form.Item>
              <Divider />
              <Form.Item name="knowledgeBaseId" label="知识库 ID">
                <Input placeholder="WeKnora 知识库 ID" />
              </Form.Item>
              <Form.Item name="faqSetId" label="数据集 ID">
                <Input placeholder="FAQ 数据集 ID" />
              </Form.Item>
              <Form.Item name="webhookEnabled" label="Webhook 接收开关" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="autoExport" label="转换成功后自动导出" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Button type="primary" loading={savingWeknora} onClick={handleSaveWeknora}>保存配置</Button>
            </Form>
          ),
        },
        {
          key: 'models',
          label: '模型管理',
          children: (
            <>
              <div style={{ marginBottom: 12, textAlign: 'right' }}>
                <Button type="primary" icon={<PlusOutlined />} onClick={handleAddModel}>新建模型</Button>
              </div>
              <Table rowKey="id" size="small" loading={modelLoading} dataSource={models} columns={modelColumns} pagination={false} />
            </>
          ),
        },
      ]} />

      {/* 模型编辑弹层 */}
      <Modal
        title={editingModel ? '编辑模型配置' : '新建模型配置'}
        open={modelModalOpen}
        onCancel={() => { setModelModalOpen(false); setEditingModel(null); }}
        confirmLoading={savingModel}
        onOk={handleSaveModel}
        destroyOnClose
      >
        <Form form={modelForm} layout="vertical">
          <Form.Item name="modelCode" label="模型编码" rules={[{ required: true }]}>
            <Input placeholder="如 deepseek-v3" disabled={!!editingModel} />
          </Form.Item>
          <Form.Item name="modelName" label="显示名称" rules={[{ required: true }]}>
            <Input placeholder="如 DeepSeek V3" />
          </Form.Item>
          <Form.Item name="baseUrl" label="API Base URL" rules={[{ required: true }]}>
            <Input placeholder="如 https://api.deepseek.com/v1" />
          </Form.Item>
          <Form.Item name="apiKey" label="API Key" rules={[{ required: !editingModel }]}>
            <Input.Password placeholder="输入 API Key" />
          </Form.Item>
          <Form.Item name="apiFormat" label="API 格式" rules={[{ required: true }]}>
            <Select options={[{ label: 'OpenAI 兼容', value: 'openai_compatible' }, { label: 'Anthropic', value: 'anthropic' }, { label: '自定义', value: 'custom' }]} />
          </Form.Item>
          <Form.Item name="modelId" label="模型 ID" rules={[{ required: true }]}>
            <Input placeholder="如 deepseek-chat" />
          </Form.Item>
          <Form.Item name="temperature" label="采样温度" initialValue={0.7}>
            <InputNumber min={0} max={2} step={0.1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="maxTokens" label="最大输出 Token" initialValue={2000}>
            <InputNumber min={100} max={32000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '禁用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>
    </Modal>
  );
}
