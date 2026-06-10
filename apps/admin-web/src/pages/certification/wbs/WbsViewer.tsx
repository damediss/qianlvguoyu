import { EditOutlined, SaveOutlined } from '@ant-design/icons';
import { Button, Empty, Select, Typography } from 'antd';
import type { CSSProperties } from 'react';
import { useMemo, useState } from 'react';
import { PermissionGate } from '../../../directives/permission';
import type { CertificationDocument, CertificationTask, CertificationWbsItem } from '../../../types/certification';
import {
  formatEstimateHours,
  formatManpowerInput,
  resolveCategoryName,
  summarizeWbsItems,
  wbsCategoryOptions,
} from './constants';

const colors = {
  primary: '#1890ff',
  primaryStrong: '#096dd9',
  primarySoft: '#e6f7ff',
  success: '#52c41a',
  successSoft: '#f6ffed',
  text: '#1f2937',
  textMuted: '#6b7280',
  border: '#e5e7eb',
};

const containerStyle: CSSProperties = {
  maxWidth: 1200,
  margin: '0 auto',
  background: '#fff',
  borderRadius: 12,
  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
  overflow: 'hidden',
  border: '1px solid #eef2f7',
};

const headerStyle: CSSProperties = {
  background: `linear-gradient(135deg, ${colors.primary} 0%, ${colors.primaryStrong} 100%)`,
  color: '#fff',
  padding: 32,
};

const metaSectionStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(5, 1fr)',
  borderBottom: `1px solid ${colors.border}`,
};

const metaItemStyle: CSSProperties = {
  padding: '20px 24px',
  borderRight: `1px solid ${colors.border}`,
};

const toolbarStyle: CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: 12,
  flexWrap: 'wrap',
  padding: '20px 32px',
  background: '#f8fafc',
  borderBottom: `1px solid ${colors.border}`,
};

const bodyStyle: CSSProperties = {
  padding: 32,
};

const kpiGridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
  gap: 20,
  marginBottom: 32,
};

const kpiCardStyle: CSSProperties = {
  background: '#f8fafc',
  borderRadius: 10,
  padding: 24,
  textAlign: 'center',
  border: `1px solid ${colors.border}`,
};

const filterSectionStyle: CSSProperties = {
  background: '#f8fafc',
  borderRadius: 10,
  padding: 20,
  marginBottom: 24,
};

const tableSectionStyle: CSSProperties = {
  background: '#fff',
  borderRadius: 10,
  border: `1px solid ${colors.border}`,
  overflow: 'hidden',
};

const tableHeaderStyle: CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: '20px 24px',
  borderBottom: `1px solid ${colors.border}`,
};

function dash(value?: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return String(value);
}

function documentDraftLabel(document?: CertificationDocument) {
  if (document?.status === 'submitted') {
    return '已提交';
  }
  if (document?.status === 'approved') {
    return '已通过';
  }
  if (document?.status === 'rejected') {
    return '已驳回';
  }
  if (document?.lastSavedAt) {
    return `已保存 ${document.lastSavedAt}`;
  }
  return '未保存草稿';
}

function taskStatusLabel(task?: CertificationTask) {
  if (task?.statusName) {
    return task.statusName;
  }
  const map: Record<string, string> = {
    in_progress: '进行中',
    pending_review: '待审核',
    rejected: '已驳回',
    completed: '已完成',
  };
  return task?.status ? map[task.status] || task.status : '-';
}

function CategoryBadge({ name }: { name?: string }) {
  const label = name || '-';
  const style: CSSProperties = {
    display: 'inline-block',
    padding: '4px 10px',
    borderRadius: 4,
    fontSize: 12,
    fontWeight: 500,
  };
  if (label === '规划') {
    return <span style={{ ...style, background: colors.primarySoft, color: colors.primary }}>{label}</span>;
  }
  if (label === '执行') {
    return <span style={{ ...style, background: colors.successSoft, color: colors.success }}>{label}</span>;
  }
  if (label === '审核') {
    return <span style={{ ...style, background: '#f3e8ff', color: '#9333ea' }}>{label}</span>;
  }
  return <span style={{ ...style, background: '#f3f4f6', color: colors.textMuted }}>{label}</span>;
}

export function WbsViewer({
  task,
  document,
  items,
  lockedForEdit,
  saveLoading,
  onEdit,
  onSaveDraft,
}: {
  task?: CertificationTask;
  document?: CertificationDocument;
  items: CertificationWbsItem[];
  lockedForEdit?: boolean;
  saveLoading?: boolean;
  onEdit?: () => void;
  onSaveDraft?: () => void;
}) {
  const [categoryFilter, setCategoryFilter] = useState<string>('all');

  const filteredItems = useMemo(() => {
    if (categoryFilter === 'all') {
      return items;
    }
    return items.filter((item) => resolveCategoryName(item.categoryName, item.categoryCode) === categoryFilter);
  }, [categoryFilter, items]);

  const summary = useMemo(() => summarizeWbsItems(filteredItems), [filteredItems]);

  if (!task) {
    return <Empty description="请选择任务后查看任务 WBS" />;
  }

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <Typography.Title level={2} style={{ margin: 0, color: '#fff', fontSize: 24 }}>
          任务 WBS 表
        </Typography.Title>
        <Typography.Paragraph style={{ margin: '8px 0 0', color: 'rgba(255,255,255,0.9)', fontSize: 14 }}>
          当前为查看态页面，用于查看任务分解、工时、资源和费用估算，并可进入编辑页面继续调整任务内容
        </Typography.Paragraph>
      </div>

      <div style={metaSectionStyle}>
        {[
          { label: '任务名称', value: task.taskName },
          { label: '任务编号', value: task.taskNo },
          { label: '任务经理', value: task.taskManagerName },
          { label: '当前状态', value: taskStatusLabel(task) },
          { label: '草稿状态', value: documentDraftLabel(document) },
        ].map((item, index, arr) => (
          <div
            key={item.label}
            style={{
              ...metaItemStyle,
              borderRight: index === arr.length - 1 ? 'none' : metaItemStyle.borderRight,
            }}
          >
            <span style={{ display: 'block', fontSize: 12, color: colors.textMuted, marginBottom: 6 }}>{item.label}</span>
            <strong style={{ fontSize: 15, color: colors.text, fontWeight: 600 }}>{dash(item.value)}</strong>
          </div>
        ))}
      </div>

      <div style={toolbarStyle}>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          {onEdit ? (
            <PermissionGate code="certification.project.wbs.edit">
              <Button type="primary" icon={<EditOutlined />} disabled={lockedForEdit} onClick={onEdit}>
                编辑表单
              </Button>
            </PermissionGate>
          ) : null}
          {onSaveDraft ? (
            <PermissionGate code="certification.project.wbs.edit">
              <Button icon={<SaveOutlined />} disabled={lockedForEdit} loading={saveLoading} onClick={onSaveDraft}>
                保存草稿
              </Button>
            </PermissionGate>
          ) : null}
        </div>
      </div>

      <div style={bodyStyle}>
        <div style={kpiGridStyle}>
          <div style={kpiCardStyle}>
            <div style={{ fontSize: 32, fontWeight: 700, color: colors.primary, marginBottom: 8 }}>{summary.count}</div>
            <div style={{ fontSize: 13, color: colors.textMuted }}>当前筛选任务数</div>
          </div>
          <div style={kpiCardStyle}>
            <div style={{ fontSize: 32, fontWeight: 700, color: colors.primary, marginBottom: 8 }}>{summary.totalHours}h</div>
            <div style={{ fontSize: 13, color: colors.textMuted }}>预计总工时</div>
          </div>
          <div style={kpiCardStyle}>
            <div style={{ fontSize: 32, fontWeight: 700, color: colors.primary, marginBottom: 8 }}>{summary.totalDays}天</div>
            <div style={{ fontSize: 13, color: colors.textMuted }}>预计总工期</div>
          </div>
          <div style={kpiCardStyle}>
            <div style={{ fontSize: 32, fontWeight: 700, color: colors.primary, marginBottom: 8 }}>￥{summary.totalCost}</div>
            <div style={{ fontSize: 13, color: colors.textMuted }}>费用估算</div>
          </div>
        </div>

        <div style={filterSectionStyle}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxWidth: 220 }}>
            <label style={{ fontSize: 13, color: colors.textMuted }}>按任务分类筛选</label>
            <Select
              value={categoryFilter}
              onChange={setCategoryFilter}
              options={[
                { label: '全部', value: 'all' },
                ...wbsCategoryOptions.map((item) => ({ label: item.label, value: item.name })),
              ]}
            />
          </div>
        </div>

        <div style={tableSectionStyle}>
          <div style={tableHeaderStyle}>
            <Typography.Title level={5} style={{ margin: 0, fontSize: 16, color: colors.text }}>
              工作分解结构
            </Typography.Title>
            <span style={{ color: colors.textMuted, fontSize: 14 }}>支持按任务分类筛选</span>
          </div>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
              <thead>
                <tr>
                  {['序号', '任务分类', '任务名称', '包含活动', '工时估算', '人力投入', '其他资源投入', '费用估算(元)', '工期（天）'].map((title) => (
                    <th
                      key={title}
                      style={{
                        background: '#f8fafc',
                        padding: '14px 16px',
                        textAlign: 'left',
                        fontWeight: 600,
                        color: colors.text,
                        borderBottom: `1px solid ${colors.border}`,
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {title}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filteredItems.length === 0 ? (
                  <tr>
                    <td colSpan={9} style={{ padding: 32, textAlign: 'center', color: colors.textMuted }}>
                      暂无 WBS 数据，可点击「编辑表单」开始填写
                    </td>
                  </tr>
                ) : (
                  filteredItems.map((row, index) => (
                    <tr key={row.id || `${row.wbsCode}-${index}`}>
                      <td style={cellStyle}>{index + 1}</td>
                      <td style={cellStyle}>
                        <CategoryBadge name={resolveCategoryName(row.categoryName, row.categoryCode)} />
                      </td>
                      <td style={{ ...cellStyle, color: colors.text }}>{dash(row.itemName)}</td>
                      <td style={cellStyle}>{dash(row.activityDescription)}</td>
                      <td style={cellStyle}>{formatEstimateHours(row.estimateHours)}</td>
                      <td style={cellStyle}>{formatManpowerInput(row)}</td>
                      <td style={cellStyle}>{dash(row.resourceInput)}</td>
                      <td style={cellStyle}>{row.costAmount == null ? '-' : row.costAmount}</td>
                      <td style={cellStyle}>{row.durationDays == null ? '-' : row.durationDays}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

const cellStyle: CSSProperties = {
  padding: '14px 16px',
  borderBottom: `1px solid ${colors.border}`,
  color: colors.textMuted,
};
