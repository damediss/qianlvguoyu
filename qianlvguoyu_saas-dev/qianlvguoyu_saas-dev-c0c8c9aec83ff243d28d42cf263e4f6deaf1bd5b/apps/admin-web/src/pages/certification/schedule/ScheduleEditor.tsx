import { DeleteOutlined, EyeOutlined, PlusOutlined, SaveOutlined, StopOutlined } from '@ant-design/icons';
import { Button, DatePicker, Input, InputNumber, Modal, Select, Space, Typography, message } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import type { CSSProperties } from 'react';
import type { CertificationMember, CertificationScheduleItem, CertificationTask } from '../../../types/certification';
import {
  certificationTypeOptions,
  createEmptyExamInfoItem,
  createEmptyScheduleItem,
  defaultScheduleTemplate,
  formatPreparationDays,
  levelOptions,
  scheduleStatusOptions,
  type ScheduleExamInfoItem,
  type ScheduleTaskFields,
  updateExamItem,
  validateSchedulePlan,
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

const gridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
  gap: 16,
};

const sectionTitleStyle: CSSProperties = {
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
};

const timelineItemStyle: CSSProperties = {
  display: 'flex',
  gap: 16,
  alignItems: 'flex-start',
  padding: 20,
  background: '#f8fafc',
  borderRadius: 10,
  border: '1px solid #e5e7eb',
  marginBottom: 16,
};

const examRowStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr)) 40px',
  gap: 16,
  alignItems: 'end',
  padding: 20,
  background: '#f8fafc',
  borderRadius: 10,
  border: '1px solid #e5e7eb',
  marginBottom: 16,
};

function requiredLabel(text: string) {
  return (
    <span>
      {text}
      <span style={{ color: '#ff4d4f', marginLeft: 4 }}>*</span>
    </span>
  );
}

function updateItem(items: CertificationScheduleItem[], index: number, patch: Partial<CertificationScheduleItem>) {
  return items.map((row, i) => (i === index ? { ...row, ...patch } : row));
}

function toDateValue(value?: string): Dayjs | null {
  return value ? dayjs(value) : null;
}

export function ScheduleEditor({
  task,
  taskFields,
  examInfos,
  items,
  members,
  saving,
  onTaskFieldsChange,
  onExamInfosChange,
  onItemsChange,
  onSave,
  onPreview,
  onCancel,
}: {
  task?: CertificationTask;
  taskFields: ScheduleTaskFields;
  examInfos: ScheduleExamInfoItem[];
  items: CertificationScheduleItem[];
  members: CertificationMember[];
  saving?: boolean;
  onTaskFieldsChange: (fields: ScheduleTaskFields) => void;
  onExamInfosChange: (items: ScheduleExamInfoItem[]) => void;
  onItemsChange: (items: CertificationScheduleItem[]) => void;
  onSave: () => void;
  onPreview: () => void;
  onCancel: () => void;
}) {
  const memberOptions = members
    .filter((member) => member.id)
    .map((member) => ({ label: member.memberName, value: member.id! }));

  const patchTask = (patch: Partial<ScheduleTaskFields>) => onTaskFieldsChange({ ...taskFields, ...patch });

  const handleSave = () => {
    const error = validateSchedulePlan(taskFields, examInfos, items);
    if (error) {
      message.warning(error);
      return;
    }
    onSave();
  };

  const handleDeleteSchedule = (index: number) => {
    Modal.confirm({
      title: '确定要删除该安排吗？',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => onItemsChange(items.filter((_, i) => i !== index)),
    });
  };

  const handleDeleteExam = (index: number) => {
    Modal.confirm({
      title: '确定要删除该考试信息吗？',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => onExamInfosChange(examInfos.filter((_, i) => i !== index)),
    });
  };

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <Typography.Title level={2} style={{ margin: 0, color: '#fff' }}>
          编辑进度计划
        </Typography.Title>
        <Typography.Paragraph style={{ margin: '8px 0 0', color: 'rgba(255,255,255,0.9)' }}>
          请按「基本信息 → 考试信息 → 准备期关键安排」填写进度计划
        </Typography.Paragraph>
      </div>

      <div style={toolbarStyle}>
        <Space wrap>
          <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
            保存修改
          </Button>
          <Button icon={<EyeOutlined />} onClick={onPreview}>
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
                onOk: onCancel,
              });
            }}
          >
            取消
          </Button>
        </Space>
        <Typography.Text type="secondary">准备时间：{formatPreparationDays(taskFields.examDate, taskFields.planStartDate)}</Typography.Text>
      </div>

      <div style={bodyStyle}>
        <div style={{ marginBottom: 32 }}>
          <div style={sectionTitleStyle}>
            <span style={sectionNumberStyle}>1</span>
            <Typography.Title level={4} style={{ margin: 0 }}>基本信息</Typography.Title>
          </div>
          <div style={gridStyle}>
            <label>
              <div style={{ marginBottom: 6 }}>{requiredLabel('考试日期')}</div>
              <DatePicker
                style={{ width: '100%' }}
                value={toDateValue(taskFields.examDate)}
                onChange={(value) => patchTask({ examDate: value ? value.format('YYYY-MM-DD') : undefined })}
              />
            </label>
            <label>
              <div style={{ marginBottom: 6 }}>{requiredLabel('启动日期')}</div>
              <DatePicker
                style={{ width: '100%' }}
                value={toDateValue(taskFields.planStartDate)}
                onChange={(value) => patchTask({ planStartDate: value ? value.format('YYYY-MM-DD') : undefined })}
              />
            </label>
            <label>
              <div style={{ marginBottom: 6 }}>{requiredLabel('准备时间')}</div>
              <Input readOnly value={formatPreparationDays(taskFields.examDate, taskFields.planStartDate)} />
            </label>
            <label>
              <div style={{ marginBottom: 6 }}>{requiredLabel('认定状态')}</div>
              <Select
                style={{ width: '100%' }}
                value={taskFields.status}
                options={scheduleStatusOptions}
                onChange={(value) => patchTask({ status: value })}
              />
            </label>
            <label style={{ gridColumn: 'span 2' }}>
              <div style={{ marginBottom: 6 }}>{requiredLabel('对外联系人')}</div>
              <Select
                style={{ width: '100%' }}
                value={taskFields.externalContactMemberId}
                options={memberOptions}
                placeholder="请选择项目成员"
                onChange={(value) => patchTask({ externalContactMemberId: value })}
              />
            </label>
            <label style={{ gridColumn: 'span 2' }}>
              <div style={{ marginBottom: 6 }}>{requiredLabel('认定负责人')}</div>
              <Select
                style={{ width: '100%' }}
                value={taskFields.certificationOwnerMemberId}
                options={memberOptions}
                placeholder="请选择项目成员"
                onChange={(value) => patchTask({ certificationOwnerMemberId: value })}
              />
            </label>
          </div>
        </div>

        <div style={{ marginBottom: 32 }}>
          <div style={sectionTitleStyle}>
            <span style={sectionNumberStyle}>2</span>
            <Typography.Title level={4} style={{ margin: 0 }}>考试信息</Typography.Title>
            <Button
              size="small"
              icon={<PlusOutlined />}
              style={{ marginLeft: 'auto' }}
              onClick={() => onExamInfosChange([...examInfos, createEmptyExamInfoItem(task)])}
            >
              新增考试信息
            </Button>
          </div>

          {examInfos.length === 0 ? (
            <Typography.Text type="secondary">暂无考试信息，请点击「新增考试信息」添加</Typography.Text>
          ) : (
            examInfos.map((row, index) => (
              <div key={row.id || `exam-${index}`} style={examRowStyle}>
                <label>
                  <div style={{ marginBottom: 6 }}>{requiredLabel('认定类型')}</div>
                  <Select
                    style={{ width: '100%' }}
                    value={row.certificationType}
                    options={certificationTypeOptions}
                    onChange={(value) => onExamInfosChange(updateExamItem(examInfos, index, { certificationType: value }))}
                  />
                </label>
                <label>
                  <div style={{ marginBottom: 6 }}>{requiredLabel('工种')}</div>
                  <Input
                    value={row.workTypeName}
                    placeholder="请输入工种"
                    onChange={(e) => onExamInfosChange(updateExamItem(examInfos, index, { workTypeName: e.target.value }))}
                  />
                </label>
                <label>
                  <div style={{ marginBottom: 6 }}>{requiredLabel('等级')}</div>
                  <Select
                    style={{ width: '100%' }}
                    value={row.levelCode}
                    options={levelOptions}
                    placeholder="请选择等级"
                    onChange={(value, option) => {
                      const label = Array.isArray(option) ? undefined : option?.label;
                      onExamInfosChange(updateExamItem(examInfos, index, {
                        levelCode: value,
                        levelName: typeof label === 'string' ? label : row.levelName,
                      }));
                    }}
                  />
                </label>
                <label>
                  <div style={{ marginBottom: 6 }}>{requiredLabel('认定人数')}</div>
                  <InputNumber
                    min={0}
                    style={{ width: '100%' }}
                    value={row.plannedPeopleCount}
                    onChange={(value) => onExamInfosChange(updateExamItem(examInfos, index, { plannedPeopleCount: value ?? undefined }))}
                  />
                </label>
                <Button
                  danger
                  type="text"
                  icon={<DeleteOutlined />}
                  disabled={examInfos.length <= 1}
                  onClick={() => handleDeleteExam(index)}
                />
              </div>
            ))
          )}
        </div>

        <div>
          <div style={sectionTitleStyle}>
            <span style={sectionNumberStyle}>3</span>
            <Typography.Title level={4} style={{ margin: 0 }}>准备期关键安排</Typography.Title>
            <Space style={{ marginLeft: 'auto' }}>
              {items.length === 0 ? (
                <Button size="small" onClick={() => onItemsChange(defaultScheduleTemplate())}>
                  载入默认模板
                </Button>
              ) : null}
              <Button size="small" icon={<PlusOutlined />} onClick={() => onItemsChange([...items, createEmptyScheduleItem(items.length)])}>
                新增安排
              </Button>
            </Space>
          </div>

          {items.map((item, index) => (
            <div key={item.id || `schedule-edit-${index}`} style={timelineItemStyle}>
              <Input
                style={{ width: 120 }}
                type="number"
                min={1}
                value={item.dayNo}
                placeholder="天数"
                onChange={(e) => onItemsChange(updateItem(items, index, { dayNo: Number(e.target.value) || undefined }))}
              />
              <Input
                style={{ flex: 1 }}
                value={item.itemName}
                placeholder="请输入安排内容"
                onChange={(e) => onItemsChange(updateItem(items, index, { itemName: e.target.value }))}
              />
              <Button danger type="text" icon={<DeleteOutlined />} onClick={() => handleDeleteSchedule(index)} />
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}
