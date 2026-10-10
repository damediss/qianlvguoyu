import { Card, Descriptions, Empty, Space, Tag, Typography } from 'antd';
import type { CertificationDocument, CertificationTask } from '../../../types/certification';
import type { CharterDraft } from './CharterEditor';

function safeParse(document?: CertificationDocument): CharterDraft | null {
  const raw = document?.contentJson || document?.contentText;
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as CharterDraft;
  } catch {
    return null;
  }
}

function statusTag(value?: string) {
  const label = value === 'running' ? '进行中' : value === 'pending_review' ? '待审核' : value === 'completed' ? '已完成' : value || '-';
  const color = value === 'running' ? 'processing' : value === 'completed' ? 'success' : value === 'pending_review' ? 'warning' : 'default';
  return <Tag color={color}>{label}</Tag>;
}

export function CharterViewer({
  task,
  document,
}: {
  task?: CertificationTask;
  document?: CertificationDocument;
}) {
  const draft = safeParse(document);

  if (!task) {
    return <Empty description="请选择任务后查看任务策划任务书" />;
  }

  if (!draft) {
    return (
      <Card>
        <Empty
          description="暂无任务策划任务书内容（可点击“编辑”开始填写）"
        />
      </Card>
    );
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={12}>
      <Card styles={{ body: { paddingBottom: 8 } }} title="基本信息" size="small">
        <Descriptions column={3} size="small" bordered>
          <Descriptions.Item label="任务名称">{draft.taskName || task.taskName}</Descriptions.Item>
          <Descriptions.Item label="任务编号">{draft.taskNo || task.taskNo}</Descriptions.Item>
          <Descriptions.Item label="任务经理">{draft.taskManagerName || task.taskManagerName}</Descriptions.Item>
          <Descriptions.Item label="任务状态">{statusTag(draft.taskStatus || task.status)}</Descriptions.Item>
          <Descriptions.Item label="章程版本">V{document?.versionNo || 1}</Descriptions.Item>
          <Descriptions.Item label="最后保存">{document?.lastSavedAt || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="任务背景与目的" size="small">
        <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
          {draft.background || '-'}
        </Typography.Paragraph>
      </Card>

      <Card title="任务总体目标" size="small">
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="质量目标">{draft.goals?.quality || '-'}</Descriptions.Item>
          <Descriptions.Item label="工期目标">{draft.goals?.schedule || '-'}</Descriptions.Item>
          <Descriptions.Item label="费用目标">{draft.goals?.cost || '-'}</Descriptions.Item>
          <Descriptions.Item label="交付目标">{draft.goals?.delivery || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="节点目标" size="small">
        <Space direction="vertical" style={{ width: '100%' }} size={8}>
          {(draft.milestones || []).map((m, idx) => (
            <div
              key={`${idx}-${m.title}`}
              style={{
                border: '1px solid #e5e7eb',
                borderRadius: 8,
                padding: 12,
                background: m.enabled ? '#f6ffed' : '#f8fafc',
              }}
            >
              <Space align="baseline" size={10} style={{ width: '100%', justifyContent: 'space-between' }}>
                <Typography.Text strong>
                  {idx + 1}. {m.title || '-'}
                </Typography.Text>
                <Tag color={m.enabled ? 'success' : 'default'}>{m.enabled ? '启用' : '未启用'}</Tag>
              </Space>
              {m.desc ? (
                <Typography.Paragraph style={{ margin: '6px 0 0', whiteSpace: 'pre-wrap' }}>
                  {m.desc}
                </Typography.Paragraph>
              ) : null}
            </div>
          ))}
        </Space>
      </Card>
    </Space>
  );
}

