import { ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Result, Select, Space, Spin, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { listCompetitionEventWorkTypes, pageCompetitionEvents } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import { useNavigationBlocker } from '../../hooks/useUnsavedChangesBlocker';
import type { CompetitionEvent } from '../../types/competition';
import './CompetitionEventManagementPage.css';
import { CompetitionNoticePage } from './CompetitionNoticePage';
import { CompetitionScoreRankingPage } from './CompetitionScoreRankingPage';
import { CompetitionStagePlanningPage } from './CompetitionStagePlanningPage';
import { CompetitionTheoryExamPage } from './CompetitionTheoryExamPage';

export type CompetitionEventModule = 'stages' | 'notices' | 'theory-exam' | 'scores';

const DEFAULT_COMPETITION_WORK_TYPE_LEVELS = [
  { value: '5', label: '\u521d\u7ea7\u5de5' },
  { value: '4', label: '\u4e2d\u7ea7\u5de5' },
  { value: '3', label: '\u9ad8\u7ea7\u5de5' },
  { value: '2', label: '\u6280\u5e08' },
  { value: '1', label: '\u9ad8\u7ea7\u6280\u5e08' },
];

const moduleMeta: Record<CompetitionEventModule, {
  title: string;
  description: string;
  permission: string[];
  eventLabel: (event: CompetitionEvent) => string;
}> = {
  stages: {
    title: '赛段编排',
    description: '选择赛事和工种后，按初赛、复赛、半决赛、决赛等晋级流程编排赛段、时间和晋级规则。',
    permission: ['competition:stage:view'],
    eventLabel: (event) => `${event.code} · ${event.name}`,
  },
  notices: {
    title: '公告设置',
    description: '选择赛事后，创建、编辑并发布赛事公告。',
    permission: ['competition:notice:view'],
    eventLabel: (event) => `${event.code} · ${event.name}`,
  },
  'theory-exam': {
    title: '理论考试',
    description: '选择赛事后，配置理论试卷、考场、排考和考试监控。',
    permission: ['competition:theory-paper:view', 'competition:theory-schedule:view', 'competition:theory-monitor:view', 'competition:theory-result:view'],
    eventLabel: (event) => `${event.code} · ${event.name}`,
  },
  scores: {
    title: '成绩与排名',
    description: '选择赛事后，汇总成绩并生成赛事排名。',
    permission: ['competition:score:view', 'competition:ranking:view'],
    eventLabel: (event) => `${event.code} · ${event.name}`,
  },
};

export function CompetitionEventModulePage({
  module,
  onNavigate,
  initialEventId,
  initialStageId,
  initialEventWorkTypeId,
}: {
  module: CompetitionEventModule;
  onNavigate: (path: string) => void;
  initialEventId?: string;
  initialStageId?: string;
  initialEventWorkTypeId?: string;
}) {
  const meta = moduleMeta[module];
  const { confirmNavigation } = useNavigationBlocker();
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = meta.permission.some((permission) => permissions.includes(permission));
  const eventsQuery = useQuery({
    queryKey: ['competition-events', 'module-selector'],
    queryFn: () => pageCompetitionEvents({ page: 1, pageSize: 100 }),
    enabled: canView,
  });
  const events = eventsQuery.data?.items || [];
  const [selectedEventId, setSelectedEventId] = useState<string | undefined>(initialEventId);
  useEffect(() => {
    if (events.length === 0) return;
    if (!selectedEventId || !events.some((event) => String(event.id) === selectedEventId)) {
      setSelectedEventId(String(events[0].id));
    }
  }, [events, selectedEventId]);
  const selectedEvent = useMemo(
    () => events.find((event) => String(event.id) === selectedEventId),
    [events, selectedEventId],
  );
  const eventWorkTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', selectedEventId, 'stage-planning-selector'],
    queryFn: () => listCompetitionEventWorkTypes(selectedEventId!),
    enabled: module === 'stages' && Boolean(selectedEventId),
  });
  const activeEventWorkTypes = useMemo(
    () => (eventWorkTypesQuery.data || []).filter((item) => item.status === 'active'),
    [eventWorkTypesQuery.data],
  );
  const [selectedEventWorkTypeId, setSelectedEventWorkTypeId] = useState<string>();
  useEffect(() => {
    if (!activeEventWorkTypes.length) {
      setSelectedEventWorkTypeId(undefined);
      return;
    }
    if (!selectedEventWorkTypeId || !activeEventWorkTypes.some((item) => String(item.id) === selectedEventWorkTypeId)) {
      setSelectedEventWorkTypeId(String(activeEventWorkTypes[0].id));
    }
  }, [activeEventWorkTypes, selectedEventWorkTypeId]);
  const [selectedEventWorkTypeLevelCode, setSelectedEventWorkTypeLevelCode] = useState<string>(
    DEFAULT_COMPETITION_WORK_TYPE_LEVELS[0].value,
  );
  useEffect(() => {
    if (
      !selectedEventWorkTypeLevelCode
      || !DEFAULT_COMPETITION_WORK_TYPE_LEVELS.some((level) => level.value === selectedEventWorkTypeLevelCode)
    ) {
      setSelectedEventWorkTypeLevelCode(DEFAULT_COMPETITION_WORK_TYPE_LEVELS[0].value);
    }
  }, [selectedEventWorkTypeLevelCode]);

  if (!canView) {
    return <Result status="403" title={`暂无${meta.title}查看权限`} />;
  }
  if (eventsQuery.isLoading) return <Spin className="competition-loading" />;
  if (eventsQuery.isError) return <Result status="error" title="赛事列表加载失败" />;

  return (
    <div className="competition-page">
      <section className="competition-page-heading">
        <div>
          <Typography.Title level={2}>{meta.title}</Typography.Title>
          <Typography.Paragraph type="secondary">{meta.description}</Typography.Paragraph>
        </div>
        <Button icon={<ReloadOutlined />} onClick={() => eventsQuery.refetch()}>刷新赛事</Button>
      </section>
      <Card
        bordered={false}
        className="competition-filter-card competition-filter-card--compact"
      >
        <Space align="center" size={12} wrap style={{ width: '100%' }}>
          <Typography.Text type="secondary">选择赛事</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedEventId}
            loading={eventsQuery.isFetching}
            disabled={!eventsQuery.isFetching && events.length === 0}
            placeholder="请选择赛事"
            style={{ width: 280, maxWidth: '100%' }}
            options={events.map((event) => ({
              value: String(event.id),
              label: meta.eventLabel(event),
            }))}
            onChange={async (eventId) => {
              if (!(await confirmNavigation())) return;
              setSelectedEventWorkTypeId(undefined);
              setSelectedEventWorkTypeLevelCode(DEFAULT_COMPETITION_WORK_TYPE_LEVELS[0].value);
              setSelectedEventId(eventId || undefined);
            }}
          />
          {module === 'stages' && (
            <>
              <Typography.Text type="secondary">选择工种</Typography.Text>
              <Select
                showSearch
                optionFilterProp="label"
                value={selectedEventWorkTypeId}
                loading={eventWorkTypesQuery.isFetching}
                disabled={!selectedEventId || (!eventWorkTypesQuery.isFetching && activeEventWorkTypes.length === 0)}
                placeholder="请选择工种"
                style={{ width: 240, maxWidth: '100%' }}
                options={activeEventWorkTypes.map((workType) => ({
                  value: String(workType.id),
                  label: workType.displayName || workType.workTypeName || String(workType.id),
                }))}
                onChange={(workTypeId) => {
                  setSelectedEventWorkTypeLevelCode(DEFAULT_COMPETITION_WORK_TYPE_LEVELS[0].value);
                  setSelectedEventWorkTypeId(workTypeId || undefined);
                }}
              />
              <Typography.Text type="secondary">选择等级</Typography.Text>
              <Select
                showSearch
                optionFilterProp="label"
                value={selectedEventWorkTypeLevelCode}
                disabled={!selectedEventWorkTypeId}
                placeholder="请选择等级"
                style={{ width: 180, maxWidth: '100%' }}
                options={DEFAULT_COMPETITION_WORK_TYPE_LEVELS}
                onChange={(levelCode) => setSelectedEventWorkTypeLevelCode(levelCode || DEFAULT_COMPETITION_WORK_TYPE_LEVELS[0].value)}
              />
            </>
          )}
        </Space>
      </Card>
      {selectedEvent && module === 'notices' && <CompetitionNoticePage eventId={String(selectedEvent.id)} onNavigate={onNavigate} embedded />}
      {selectedEvent && module === 'stages' && <CompetitionStagePlanningPage eventId={String(selectedEvent.id)} eventWorkTypeId={selectedEventWorkTypeId} onNavigate={onNavigate} embedded />}
      {selectedEvent && module === 'theory-exam' && (
        <CompetitionTheoryExamPage
          eventId={String(selectedEvent.id)}
          initialStageId={initialStageId}
          initialEventWorkTypeId={initialEventWorkTypeId}
          onNavigate={onNavigate}
          embedded
        />
      )}
      {selectedEvent && module === 'scores' && <CompetitionScoreRankingPage eventId={String(selectedEvent.id)} onNavigate={onNavigate} embedded />}
    </div>
  );
}
