import {
  BellOutlined,
  CalendarOutlined,
  HomeOutlined,
  TeamOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { Button, Space, Tag, Typography } from 'antd';
import type { ReactNode } from 'react';
import type { CompetitionEvent, CompetitionEventStatus } from '../../types/competition';
import { useAuthStore } from '../../stores/authStore';

type EventSection = 'overview' | 'organizations' | 'rosters' | 'notices';

interface CompetitionEventFrameProps {
  event: CompetitionEvent;
  activeSection: EventSection;
  onNavigate: (path: string) => void;
  actions?: ReactNode;
  children: ReactNode;
}

const statusMeta: Record<CompetitionEventStatus, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  published: { label: '已发布', color: 'success' },
  closed: { label: '已关闭', color: 'warning' },
  archived: { label: '已归档', color: 'error' },
};

const scopeMeta: Record<string, string> = {
  platform: '平台赛事',
  enterprise_internal: '企业内部赛',
};

const sections: Array<{
  key: EventSection;
  label: string;
  icon: ReactNode;
  path: (id: string) => string;
  permission?: string;
}> = [
  { key: 'overview', label: '计划总览', icon: <HomeOutlined />, path: (id) => `/competition/events/${id}` },
  { key: 'organizations', label: '参与企业', icon: <TeamOutlined />, path: (id) => `/competition/events/${id}/organizations`, permission: 'competition:event-org:view' },
  { key: 'rosters', label: '名单导入', icon: <UploadOutlined />, path: (id) => `/competition/events/${id}/rosters`, permission: 'competition:roster:view' },
  { key: 'notices', label: '公告设置', icon: <BellOutlined />, path: (id) => `/competition/events/${id}/notices`, permission: 'competition:notice:view' },
];

function formatDate(value?: string | null) {
  if (!value) return '未设置';
  return value.replace('T', ' ').slice(0, 16);
}

export function CompetitionEventFrame({
  event,
  activeSection,
  onNavigate,
  actions,
  children,
}: CompetitionEventFrameProps) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const visibleSections = sections.filter((section) => !section.permission || permissions.includes(section.permission));
  const status = statusMeta[event.status];
  const eventId = String(event.id);

  return (
    <div className="competition-page competition-workspace-page">
      <section className="competition-event-hero">
        <div className="competition-event-hero__main">
          <div className="competition-event-hero__title-row">
            <div className="competition-event-hero__mark">赛</div>
            <div>
              <Typography.Title level={2}>{event.name}</Typography.Title>
              <Space size={8} wrap>
                <Tag color={status.color}>{status.label}</Tag>
                <Typography.Text type="secondary">{event.code}</Typography.Text>
                <Typography.Text type="secondary">{scopeMeta[event.scope] || event.scope}</Typography.Text>
              </Space>
            </div>
          </div>
          <Typography.Paragraph className="competition-event-hero__description">
            {event.description || '围绕竞赛计划、参与企业、名单导入和公告设置，统一管理赛事运营链路。'}
          </Typography.Paragraph>
          <div className="competition-event-meta">
            <span><CalendarOutlined /> 报名/竞赛时间</span>
            <strong>{formatDate(event.startAt)} — {formatDate(event.endAt)}</strong>
          </div>
        </div>
        <div className="competition-event-hero__actions">{actions}</div>
      </section>

      <nav className="competition-workspace-nav" aria-label="赛事工作台导航">
        {visibleSections.map((section) => (
          <Button
            key={section.key}
            type={activeSection === section.key ? 'primary' : 'text'}
            icon={section.icon}
            onClick={() => onNavigate(section.path(eventId))}
          >
            {section.label}
          </Button>
        ))}
      </nav>

      {children}
    </div>
  );
}

export type { EventSection };
