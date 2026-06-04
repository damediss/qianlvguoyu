import { DeleteOutlined, EyeOutlined, PlusOutlined, SaveOutlined, StopOutlined } from '@ant-design/icons';
import { Button, Input, InputNumber, Modal, Select, Space, Typography, message } from 'antd';
import type { CSSProperties } from 'react';
import type { CertificationWbsItem } from '../../../types/certification';
import {
  createEmptyWbsItem,
  validateWbsItems,
  wbsCategoryOptions,
  wbsStatusOptions,
} from './constants';

const containerStyle: CSSProperties = {
  maxWidth: 1200,
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

const toolbarStyle: CSSProperties = {
  padding: '20px 32px',
  background: '#f8fafc',
  borderBottom: '1px solid #e5e7eb',
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: 12,
  flexWrap: 'wrap',
};

const bodyStyle: CSSProperties = {
  padding: 32,
};

const taskCardStyle: CSSProperties = {
  background: '#f8fafc',
  border: '1px solid #e5e7eb',
  borderRadius: 12,
  padding: 24,
  marginBottom: 20,
};

const taskHeaderStyle: CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  marginBottom: 20,
  paddingBottom: 16,
  borderBottom: '1px dashed #e5e7eb',
};

const taskNumberStyle: CSSProperties = {
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

const gridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
  gap: 16,
};

function requiredLabel(text: string) {
  return (
    <span>
      {text}
      <span style={{ color: '#ff4d4f', marginLeft: 4 }}>*</span>
    </span>
  );
}

function updateItem(items: CertificationWbsItem[], index: number, patch: Partial<CertificationWbsItem>) {
  return items.map((row, i) => (i === index ? { ...row, ...patch } : row));
}

export function WbsEditor({
  items,
  saving,
  onChange,
  onSave,
  onPreview,
  onCancel,
}: {
  items: CertificationWbsItem[];
  saving?: boolean;
  onChange: (items: CertificationWbsItem[]) => void;
  onSave: () => void;
  onPreview: () => void;
  onCancel: () => void;
}) {
  const handleSave = () => {
    const error = validateWbsItems(items);
    if (error) {
      message.warning(error);
      return;
    }
    onSave();
  };

  const handleDelete = (index: number) => {
    Modal.confirm({
      title: '确定要删除该任务吗？',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => onChange(items.filter((_, i) => i !== index)),
    });
  };

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <Typography.Title level={2} style={{ margin: 0, color: '#fff' }}>编辑任务 WBS</Typography.Title>
        <Typography.Paragraph style={{ margin: '8px 0 0', color: 'rgba(255,255,255,0.9)' }}>
          请填写或修改工作任务分解信息，字段与数据库表 certification_wbs_item 一致
        </Typography.Paragraph>
      </div>

      <div style={toolbarStyle}>
        <Space wrap>
          <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>保存修改</Button>
          <Button icon={<EyeOutlined />} onClick={onPreview}>预览</Button>
          <Button
            icon={<StopOutlined />}
            onClick={() => {
              Modal.confirm({
                title: '确定要取消编辑吗？',
                content: '未保存的修改将丢失。',
                okText: '确定取消',
                cancelText: '继续编辑',
                onOk: onCancel,
              });
            }}
          >
            取消
          </Button>
        </Space>
        <Button type="link" onClick={onPreview}>← 返回查看页</Button>
      </div>

      <div style={bodyStyle}>
        {items.map((item, index) => (
          <div key={item.id || `wbs-${index}`} style={taskCardStyle}>
            <div style={taskHeaderStyle}>
              <span style={taskNumberStyle}>{index + 1}</span>
              <Button
                type="text"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleDelete(index)}
              />
            </div>
            <div style={gridStyle}>
              <label>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>{requiredLabel('WBS编码')}</div>
                <Input
                  value={item.wbsCode}
                  placeholder="例如：0110"
                  onChange={(e) => onChange(updateItem(items, index, { wbsCode: e.target.value }))}
                />
              </label>
              <label>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>{requiredLabel('任务分类')}</div>
                <Select
                  style={{ width: '100%' }}
                  value={item.categoryCode || undefined}
                  placeholder="请选择"
                  options={wbsCategoryOptions.map((option) => ({ label: option.label, value: option.value }))}
                  onChange={(value) => {
                    const matched = wbsCategoryOptions.find((option) => option.value === value);
                    onChange(updateItem(items, index, {
                      categoryCode: value,
                      categoryName: matched?.name,
                    }));
                  }}
                />
              </label>
              <label>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>{requiredLabel('事项名称')}</div>
                <Input
                  value={item.itemName}
                  placeholder="请输入事项名称"
                  onChange={(e) => onChange(updateItem(items, index, { itemName: e.target.value }))}
                />
              </label>
              <label>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>状态</div>
                <Select
                  style={{ width: '100%' }}
                  value={item.status || 'pending'}
                  options={wbsStatusOptions}
                  onChange={(value) => onChange(updateItem(items, index, { status: value }))}
                />
              </label>
              <label>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>{requiredLabel('估算工时')}</div>
                <InputNumber
                  min={0}
                  style={{ width: '100%' }}
                  value={item.estimateHours}
                  placeholder="例如：24"
                  onChange={(value) => onChange(updateItem(items, index, { estimateHours: value ?? undefined }))}
                />
              </label>
              <label>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>{requiredLabel('工期（天）')}</div>
                <InputNumber
                  min={0}
                  style={{ width: '100%' }}
                  value={item.durationDays}
                  placeholder="例如：2"
                  onChange={(value) => onChange(updateItem(items, index, { durationDays: value ?? undefined }))}
                />
              </label>
              <label>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>{requiredLabel('费用估算(元)')}</div>
                <InputNumber
                  min={0}
                  style={{ width: '100%' }}
                  value={item.costAmount}
                  placeholder="例如：2000"
                  onChange={(value) => onChange(updateItem(items, index, { costAmount: value ?? undefined }))}
                />
              </label>
              <label>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>人力数量</div>
                <InputNumber
                  min={0}
                  style={{ width: '100%' }}
                  value={item.manpowerCount}
                  placeholder="例如：1"
                  onChange={(value) => onChange(updateItem(items, index, { manpowerCount: value ?? undefined }))}
                />
              </label>
              <label style={{ gridColumn: 'span 2' }}>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>{requiredLabel('活动描述')}</div>
                <Input.TextArea
                  rows={2}
                  value={item.activityDescription}
                  placeholder="请输入包含的活动"
                  onChange={(e) => onChange(updateItem(items, index, { activityDescription: e.target.value }))}
                />
              </label>
              <label style={{ gridColumn: 'span 2' }}>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>{requiredLabel('人力角色')}</div>
                <Input
                  value={item.manpowerRoleCode}
                  placeholder="例如：任务经理"
                  onChange={(e) => onChange(updateItem(items, index, { manpowerRoleCode: e.target.value }))}
                />
              </label>
              <label style={{ gridColumn: 'span 2' }}>
                <div style={{ marginBottom: 6, fontWeight: 500 }}>其他资源投入</div>
                <Input
                  value={item.resourceInput}
                  placeholder="请输入其他资源"
                  onChange={(e) => onChange(updateItem(items, index, { resourceInput: e.target.value }))}
                />
              </label>
            </div>
          </div>
        ))}

        <div
          style={{
            display: 'flex',
            justifyContent: 'center',
            padding: 20,
            border: '2px dashed #e5e7eb',
            borderRadius: 10,
          }}
        >
          <Button
            icon={<PlusOutlined />}
            onClick={() => onChange([...items, createEmptyWbsItem(items.length)])}
          >
            添加新任务
          </Button>
        </div>
      </div>

      <div style={{ ...toolbarStyle, borderTop: '1px solid #e5e7eb', borderBottom: 'none' }}>
        <Space wrap>
          <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>保存修改</Button>
          <Button icon={<StopOutlined />} onClick={() => Modal.confirm({
            title: '确定要取消编辑吗？',
            content: '未保存的修改将丢失。',
            okText: '确定取消',
            cancelText: '继续编辑',
            onOk: onCancel,
          })}
          >
            取消
          </Button>
        </Space>
      </div>
    </div>
  );
}
