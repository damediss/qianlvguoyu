import { EyeOutlined, SaveOutlined, StopOutlined } from '@ant-design/icons';
import { Button, Checkbox, Form, Input, Modal, Select, Space, Typography } from 'antd';
import { useEffect, useMemo, type CSSProperties } from 'react';
import type { CertificationDocument, CertificationTask } from '../../../types/certification';

type CharterStatus = 'running' | 'pending_review' | 'completed';

export interface CharterDraft {
  taskName: string;
  taskNo: string;
  taskManagerName: string;
  taskStatus?: CharterStatus;
  background: string;
  goals: {
    quality?: string;
    schedule?: string;
    cost?: string;
    delivery?: string;
  };
  milestones: Array<{
    enabled: boolean;
    title: string;
    desc?: string;
  }>;
}

const statusOptions = [
  { label: '进行中', value: 'running' },
  { label: '待审核', value: 'pending_review' },
  { label: '已完成', value: 'completed' },
];

const containerStyle: CSSProperties = {
  maxWidth: 900,
  margin: '0 auto',
  borderRadius: 12,
  overflow: 'hidden',
  background: '#fff',
  boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
  border: '1px solid #eef2f7',
};

const headerStyle: CSSProperties = {
  padding: 32,
  color: '#fff',
  background: 'linear-gradient(135deg, #fa8c16 0%, #d46b08 100%)',
};

const bodyStyle: CSSProperties = {
  padding: 32,
};

const sectionHeaderStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 12,
  marginBottom: 20,
  paddingBottom: 12,
  borderBottom: '2px solid #e5e7eb',
};

const sectionNumberStyle: CSSProperties = {
  width: 32,
  height: 32,
  borderRadius: 999,
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: '#fff',
  background: '#1890ff',
  fontWeight: 700,
  fontSize: 14,
};

const toolbarStyle: CSSProperties = {
  padding: '20px 32px',
  background: '#f8fafc',
  borderTop: '1px solid #e5e7eb',
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: 12,
  flexWrap: 'wrap',
};

const itemCardStyle: CSSProperties = {
  padding: 16,
  background: '#f8fafc',
  borderRadius: 8,
  border: '1px solid #e5e7eb',
};

function parseDraft(document?: CertificationDocument): Partial<CharterDraft> | undefined {
  if (!document) {
    return undefined;
  }
  const raw = document.contentJson || document.contentText;
  if (!raw) {
    return undefined;
  }
  try {
    return JSON.parse(raw) as Partial<CharterDraft>;
  } catch {
    return undefined;
  }
}

function buildDefaultDraft(task?: CertificationTask): CharterDraft {
  return {
    taskName: task?.taskName || '',
    taskNo: task?.taskNo || '',
    taskManagerName: task?.taskManagerName || '',
    taskStatus: (task?.status as CharterStatus) || 'running',
    background: '',
    goals: {
      quality: '',
      schedule: '',
      cost: '',
      delivery: '',
    },
    milestones: [
      { enabled: true, title: '完成任务基本信息、背景与总体目标确认', desc: '确认任务章程、任务背景和总体目标，确保任务方向正确。' },
      { enabled: false, title: '完成成员安排、WBS 分解和资源估算', desc: '' },
      { enabled: false, title: '完成进度计划与考试前准备事项确认', desc: '' },
      { enabled: false, title: '完成风险识别与资料导出，进入审核流转', desc: '' },
    ],
  };
}

function previewText(values: Partial<CharterDraft>) {
  const goals = values.goals || {};
  const milestones = values.milestones || [];
  const milestoneText = milestones.length === 0
    ? '无'
    : milestones.map((item, index) => `${index + 1}. ${item.enabled ? '启用' : '未启用'}｜${item.title || '-'}${item.desc ? `\n   ${item.desc}` : ''}`).join('\n');
  return [
    '任务策划任务书',
    '',
    `任务名称：${values.taskName || '-'}`,
    `任务编号：${values.taskNo || '-'}`,
    `任务经理：${values.taskManagerName || '-'}`,
    `任务状态：${values.taskStatus || '-'}`,
    '',
    '一、任务背景与目的',
    values.background || '-',
    '',
    '二、总体目标',
    `质量目标：${goals.quality || '-'}`,
    `工期目标：${goals.schedule || '-'}`,
    `费用目标：${goals.cost || '-'}`,
    `交付目标：${goals.delivery || '-'}`,
    '',
    '三、节点目标',
    milestoneText,
  ].join('\n');
}

export function CharterEditor({
  task,
  document,
  saving,
  onSave,
}: {
  task?: CertificationTask;
  document?: CertificationDocument;
  saving?: boolean;
  onSave: (draft: CharterDraft) => void;
}) {
  const [form] = Form.useForm<CharterDraft>();

  const initial = useMemo(() => {
    const base = buildDefaultDraft(task);
    const existing = parseDraft(document);
    return {
      ...base,
      ...existing,
      goals: { ...base.goals, ...(existing?.goals || {}) },
      milestones: existing?.milestones?.length ? existing.milestones : base.milestones,
    };
  }, [document, task]);

  useEffect(() => {
    form.setFieldsValue(initial);
  }, [form, initial]);

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <Typography.Title level={2} style={{ margin: 0, color: '#fff' }}>编辑任务策划任务书</Typography.Title>
        <Typography.Paragraph style={{ margin: '8px 0 0', color: 'rgba(255,255,255,0.9)' }}>
          请填写或修改以下表单内容，完成后点击保存
        </Typography.Paragraph>
      </div>

      <div style={bodyStyle}>
        <Form form={form} layout="vertical" requiredMark={false}>
          <div style={{ marginBottom: 32 }}>
            <div style={sectionHeaderStyle}>
              <span style={sectionNumberStyle}>1</span>
              <Typography.Title level={3} style={{ margin: 0 }}>基本信息</Typography.Title>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 20 }}>
              <Form.Item name="taskName" label={<span>任务名称<span style={{ color: '#ff4d4f', marginLeft: 4 }}>*</span></span>} rules={[{ required: true, message: '请输入任务名称' }]}>
                <Input placeholder="请输入任务名称" />
              </Form.Item>
              <Form.Item name="taskNo" label={<span>任务编号<span style={{ color: '#ff4d4f', marginLeft: 4 }}>*</span></span>} rules={[{ required: true, message: '请输入任务编号' }]}>
                <Input placeholder="请输入任务编号" />
              </Form.Item>
              <Form.Item name="taskManagerName" label={<span>任务经理<span style={{ color: '#ff4d4f', marginLeft: 4 }}>*</span></span>} rules={[{ required: true, message: '请输入任务经理姓名' }]}>
                <Input placeholder="请输入任务经理姓名" />
              </Form.Item>
              <Form.Item name="taskStatus" label="任务状态">
                <Select options={statusOptions} placeholder="请选择任务状态" />
              </Form.Item>
            </div>
          </div>

          <div style={{ marginBottom: 32 }}>
            <div style={sectionHeaderStyle}>
              <span style={sectionNumberStyle}>2</span>
              <Typography.Title level={3} style={{ margin: 0 }}>任务背景与目的</Typography.Title>
            </div>
            <Form.Item
              name="background"
              label={<span>背景描述<span style={{ color: '#ff4d4f', marginLeft: 4 }}>*</span></span>}
              rules={[{ required: true, message: '请填写背景描述' }]}
              extra={<span style={{ color: '#6b7280', fontSize: 12 }}>请详细说明任务的背景、目的和意义</span>}
            >
              <Input.TextArea placeholder="请描述任务背景和目的" autoSize={{ minRows: 5, maxRows: 10 }} />
            </Form.Item>
          </div>

          <div style={{ marginBottom: 32 }}>
            <div style={sectionHeaderStyle}>
              <span style={sectionNumberStyle}>3</span>
              <Typography.Title level={3} style={{ margin: 0 }}>任务总体目标</Typography.Title>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={itemCardStyle}>
                <Form.Item name={['goals', 'quality']} label="质量目标" style={{ marginBottom: 0 }}>
                  <Input placeholder="请输入质量目标" />
                </Form.Item>
              </div>
              <div style={itemCardStyle}>
                <Form.Item name={['goals', 'schedule']} label="工期目标" style={{ marginBottom: 0 }}>
                  <Input placeholder="请输入工期目标" />
                </Form.Item>
              </div>
              <div style={itemCardStyle}>
                <Form.Item name={['goals', 'cost']} label="费用目标" style={{ marginBottom: 0 }}>
                  <Input placeholder="请输入费用目标" />
                </Form.Item>
              </div>
              <div style={itemCardStyle}>
                <Form.Item name={['goals', 'delivery']} label="交付目标" style={{ marginBottom: 0 }}>
                  <Input placeholder="请输入交付目标" />
                </Form.Item>
              </div>
            </div>
          </div>

          <div>
            <div style={sectionHeaderStyle}>
              <span style={sectionNumberStyle}>4</span>
              <Typography.Title level={3} style={{ margin: 0 }}>节点目标</Typography.Title>
            </div>

            <Form.List name="milestones">
              {(fields) => (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {fields.map((field, index) => (
                    <Form.Item
                      key={field.key}
                      shouldUpdate={(prev, next) => prev?.milestones?.[index]?.enabled !== next?.milestones?.[index]?.enabled}
                      noStyle
                    >
                      {({ getFieldValue }) => {
                        const enabled = Boolean(getFieldValue(['milestones', index, 'enabled']));
                        return (
                          <div
                            style={{
                              ...itemCardStyle,
                              display: 'flex',
                              gap: 12,
                              alignItems: 'flex-start',
                              borderColor: enabled ? '#52c41a' : '#e5e7eb',
                              background: enabled ? '#f6ffed' : '#f8fafc',
                              transition: 'all 0.25s',
                            }}
                          >
                            <div
                              style={{
                                width: 24,
                                height: 24,
                                borderRadius: 999,
                                background: '#1890ff',
                                color: '#fff',
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontSize: 12,
                                fontWeight: 700,
                                flexShrink: 0,
                                marginTop: 3,
                              }}
                            >
                              {index + 1}
                            </div>
                            <Form.Item name={[field.name, 'enabled']} valuePropName="checked" style={{ margin: '3px 0 0' }}>
                              <Checkbox />
                            </Form.Item>
                            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
                              <Form.Item
                                name={[field.name, 'title']}
                                rules={[{ required: true, message: '请输入节点目标标题' }]}
                                style={{ marginBottom: 0 }}
                              >
                                <Input placeholder="节点目标标题" />
                              </Form.Item>
                              <Form.Item name={[field.name, 'desc']} style={{ marginBottom: 0 }}>
                                <Input.TextArea placeholder="节点目标详细描述（可选）" autoSize={{ minRows: 3, maxRows: 6 }} />
                              </Form.Item>
                            </div>
                          </div>
                        );
                      }}
                    </Form.Item>
                  ))}
                </div>
              )}
            </Form.List>
          </div>
        </Form>
      </div>

      <div style={toolbarStyle}>
        <Space wrap>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            loading={saving}
            onClick={() => {
              form.validateFields().then((values) => {
                onSave({
                  ...values,
                  goals: values.goals || { quality: '', schedule: '', cost: '', delivery: '' },
                  milestones: (values.milestones || []).map((item) => ({
                    enabled: Boolean(item.enabled),
                    title: item.title || '',
                    desc: item.desc || '',
                  })),
                });
              });
            }}
          >
            保存修改
          </Button>
          <Button
            icon={<EyeOutlined />}
            onClick={() => {
              const values = form.getFieldsValue(true);
              Modal.info({
                title: '任务策划任务书预览',
                width: 860,
                content: (
                  <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {previewText(values)}
                  </pre>
                ),
              });
            }}
          >
            预览
          </Button>
          <Button
            icon={<StopOutlined />}
            onClick={() => {
              Modal.confirm({
                title: '确定要取消编辑吗？',
                content: '未保存的修改将丢失。',
                okText: '确定取消',
                cancelText: '继续编辑',
                onOk: () => form.setFieldsValue(initial),
              });
            }}
          >
            取消
          </Button>
        </Space>

        <Button
          type="link"
          onClick={() => {
            Modal.confirm({
              title: '返回查看页',
              content: '当前页面为“编辑”，查看页尚未接入路由。你可以先保存后再切换到其他资料页。',
              okText: '知道了',
              cancelButtonProps: { style: { display: 'none' } },
            });
          }}
        >
          ← 返回查看页
        </Button>
      </div>
    </div>
  );
}

