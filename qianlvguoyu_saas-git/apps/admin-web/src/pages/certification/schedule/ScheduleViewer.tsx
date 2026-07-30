import { EditOutlined } from '@ant-design/icons';
import { Button, Card, Checkbox, Empty, Progress, Space, Table, Tag, Typography } from 'antd';
import type { CSSProperties } from 'react';
import type { ColumnsType } from 'antd/es/table';
import { PermissionGate } from '../../../directives/permission';
import type { CertificationDocument, CertificationMember, CertificationScheduleItem, CertificationTask } from '../../../types/certification';
import {
  buildExamInfoFromTask,
  certificationTypeLabel,
  formatDayLabel,
  formatPreparationDays,
  memberNameById,
  scheduleStatusLabel,
  summarizeExamPeopleCount,
  summarizeScheduleItems,
  type ScheduleExamInfoItem,
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
  background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
};

const metaGridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(5, minmax(0, 1fr))',
  borderBottom: '1px solid #e5e7eb',
};

const metaItemStyle: CSSProperties = {
  padding: '20px 24px',
  borderRight: '1px solid #e5e7eb',
};

const bodyStyle: CSSProperties = {
  padding: 32,
};

function draftLabel(document?: CertificationDocument) {
  if (!document?.lastSavedAt) {
    return '未保存草稿';
  }
  if (document.status === 'submitted') {
    return '已提交审核';
  }
  if (document.status === 'approved') {
    return '审核已通过';
  }
  return '已保存草稿';
}

export function ScheduleViewer({
  task,
  document,
  examInfos,
  items,
  members,
  lockedForEdit,
  onEdit,
  onToggleCompleted,
}: {
  task?: CertificationTask;
  document?: CertificationDocument;
  examInfos?: ScheduleExamInfoItem[];
  items: CertificationScheduleItem[];
  members: CertificationMember[];
  lockedForEdit?: boolean;
  onEdit?: () => void;
  onToggleCompleted?: (items: CertificationScheduleItem[]) => void;
}) {
  if (!task) {
    return <Empty description="请选择任务后查看进度计划" />;
  }

  const rows = examInfos && examInfos.length > 0 ? examInfos : buildExamInfoFromTask(task);
  const summary = summarizeScheduleItems(items);
  const prepDays = formatPreparationDays(task.examDate, task.planStartDate);
  const externalId = items[0]?.externalContactMemberId;
  const ownerId = items[0]?.certificationOwnerMemberId;
  const primaryExam = rows[0];
  const totalPeople = summarizeExamPeopleCount(rows);

  const columns: ColumnsType<ScheduleExamInfoItem> = [
    { title: '认定状态', width: 100, render: () => scheduleStatusLabel(task.status, task.statusName) },
    { title: '认定类型', width: 180, render: (_, row) => certificationTypeLabel(row.certificationType) },
    { title: '任务名称', width: 180, render: () => task.taskName },
    { title: '工种', dataIndex: 'workTypeName', width: 90, render: (value) => value || '-' },
    { title: '等级', dataIndex: 'levelName', width: 80, render: (value) => value || '-' },
    { title: '认定人数', dataIndex: 'plannedPeopleCount', width: 90, render: (value) => value ?? '-' },
    { title: '考试日期', width: 110, render: () => task.examDate || '-' },
    { title: '启动日期', width: 110, render: () => task.planStartDate || '-' },
    { title: '时长', width: 80, render: () => prepDays },
    { title: '对外联系人', width: 110, render: () => memberNameById(members, externalId) },
    { title: '认定负责人', width: 110, render: () => memberNameById(members, ownerId) },
  ];

  const toggleItem = (index: number, completed: boolean) => {
    if (!onToggleCompleted || lockedForEdit) {
      return;
    }
    onToggleCompleted(items.map((row, i) => (i === index ? { ...row, completed } : row)));
  };

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <Typography.Title level={2} style={{ margin: 0, color: '#fff' }}>
          任务进度计划表
        </Typography.Title>
        <Typography.Paragraph style={{ margin: '8px 0 0', color: 'rgba(255,255,255,0.9)' }}>
          当前为查看态页面，用于查看考试日期、启动日期、准备时间与关键安排，并可进入编辑页调整时间计划
        </Typography.Paragraph>
      </div>

      <div style={metaGridStyle}>
        {[
          { label: '任务名称', value: task.taskName },
          { label: '任务编号', value: task.taskNo },
          { label: '任务经理', value: task.taskManagerName },
          { label: '当前状态', value: scheduleStatusLabel(task.status, task.statusName) },
          { label: '草稿状态', value: draftLabel(document) },
        ].map((item, index, list) => (
          <div key={item.label} style={{ ...metaItemStyle, borderRight: index === list.length - 1 ? 'none' : metaItemStyle.borderRight }}>
            <Typography.Text type="secondary" style={{ display: 'block', fontSize: 12, marginBottom: 6 }}>
              {item.label}
            </Typography.Text>
            <Typography.Text strong>{item.value}</Typography.Text>
          </div>
        ))}
      </div>

      <div style={bodyStyle}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, minmax(0, 1fr))', gap: 16, marginBottom: 24 }}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 22, fontWeight: 700, color: '#1890ff' }}>
              {certificationTypeLabel(primaryExam?.certificationType || task.certificationType)}
              {rows.length > 1 ? ` 等${rows.length}项` : ''}
            </div>
            <Typography.Text type="secondary">认定类型</Typography.Text>
          </Card>
          <Card size="small" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 22, fontWeight: 700, color: '#1890ff' }}>{task.examDate || '-'}</div>
            <Typography.Text type="secondary">考试日期</Typography.Text>
          </Card>
          <Card size="small" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 22, fontWeight: 700, color: '#1890ff' }}>{task.planStartDate || '-'}</div>
            <Typography.Text type="secondary">启动日期</Typography.Text>
          </Card>
          <Card size="small" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 22, fontWeight: 700, color: '#1890ff' }}>{prepDays}</div>
            <Typography.Text type="secondary">准备时间</Typography.Text>
          </Card>
        </div>

        <Card size="small" style={{ marginBottom: 24, background: '#f8fafc' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 8 }}>
            <Typography.Text strong>准备事项完成情况</Typography.Text>
            <Typography.Text type="secondary">
              {summary.completed}/{summary.total} 准备事项已完成
            </Typography.Text>
          </Space>
          <Progress percent={summary.percent} showInfo={false} strokeColor={{ from: '#1890ff', to: '#60a5fa' }} />
        </Card>

        <Card
          size="small"
          title="进度总表"
          extra={<Typography.Text type="secondary">字段来源：认定状态 / 认定类型 / 任务名称 / 考试日期 / 启动日期 / 时长 / 准备时间</Typography.Text>}
          style={{ marginBottom: 24 }}
        >
          <Table
            rowKey={(row, index) => row.id || `exam-summary-${index}`}
            size="small"
            pagination={false}
            dataSource={rows}
            locale={{ emptyText: '暂无考试信息' }}
            columns={columns}
          />
        </Card>

        {rows.length > 1 ? (
          <Card size="small" style={{ marginBottom: 24 }}>
            <Typography.Text type="secondary">合计认定人数：{totalPeople}</Typography.Text>
          </Card>
        ) : null}

        <Card size="small" title="准备期关键安排" extra={<Typography.Text type="secondary">支持勾选完成情况并回写工作台</Typography.Text>}>
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            {items.length === 0 ? (
              <Typography.Text type="secondary">暂无关键安排，请点击「编辑表单」补充</Typography.Text>
            ) : (
              items.map((item, index) => {
                const checked = Boolean(item.completed);
                return (
                  <label
                    key={item.id || `schedule-${index}`}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 12,
                      padding: 16,
                      borderRadius: 8,
                      border: `1px solid ${checked ? '#b7eb8f' : '#e5e7eb'}`,
                      background: checked ? '#f6ffed' : '#f8fafc',
                      cursor: lockedForEdit || !onToggleCompleted ? 'default' : 'pointer',
                    }}
                  >
                    <Checkbox
                      checked={checked}
                      disabled={lockedForEdit || !onToggleCompleted}
                      onChange={(event) => toggleItem(index, event.target.checked)}
                    />
                    <span style={{ flex: 1 }}>
                      <Tag color="blue" style={{ marginRight: 8 }}>{formatDayLabel(item)}</Tag>
                      {item.itemName}
                    </span>
                  </label>
                );
              })
            )}
          </Space>
        </Card>

        {onEdit ? (
          <div style={{ marginTop: 20 }}>
            <PermissionGate code="certification.project.schedule.edit">
              <Button icon={<EditOutlined />} disabled={lockedForEdit} onClick={onEdit}>
                编辑表单
              </Button>
            </PermissionGate>
          </div>
        ) : null}
      </div>
    </div>
  );
}
