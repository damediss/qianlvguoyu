import { ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Result, Select, Space, Spin, Typography } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  listCompetitionEventWorkTypeLevels,
  listCompetitionEventWorkTypes,
  pageCompetitionEvents,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import { useNavigationBlocker } from '../../hooks/useUnsavedChangesBlocker';
import type { CompetitionEvent } from '../../types/competition';
import './CompetitionEventManagementPage.css';
import { CompetitionNoticePage } from './CompetitionNoticePage';
import { CompetitionFormalExamConsolePage } from './CompetitionFormalExamConsolePage';
import { CompetitionScoreRankingPage } from './CompetitionScoreRankingPage';
import { CompetitionStagePlanningPage } from './CompetitionStagePlanningPage';
import { CompetitionTheoryExamPage } from './CompetitionTheoryExamPage';

export type CompetitionEventModule = 'stages' | 'notices' | 'theory-exam' | 'formal-exam-console' | 'scores';

const STANDARD_COMPETITION_WORK_TYPE_LEVELS = [
  { value: '5', label: '\u521d\u7ea7\u5de5' },
  { value: '4', label: '\u4e2d\u7ea7\u5de5' },
  { value: '3', label: '\u9ad8\u7ea7\u5de5' },
  { value: '2', label: '\u6280\u5e08' },
  { value: '1', label: '\u9ad8\u7ea7\u6280\u5e08' },
] as const;

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
  'formal-exam-console': {
    title: '理论考试正式比赛控制台',
    description: '选择赛事、工种和技能等级后，进入对应赛段的理论考试正式比赛运行控制台，初始化运行实例并执行开考、监考和成绩处理。',
    permission: ['competition:exam-session:view'],
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
  const queryClient = useQueryClient();
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = meta.permission.some((permission) => permissions.includes(permission));
  const needsWorkTypeScope = module === 'stages' || module === 'formal-exam-console';
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
    enabled: needsWorkTypeScope && Boolean(selectedEventId),
  });
  const activeEventWorkTypes = useMemo(
    () => (eventWorkTypesQuery.data || []).filter((item) => item.status === 'active'),
    [eventWorkTypesQuery.data],
  );
  const [selectedEventWorkTypeId, setSelectedEventWorkTypeId] = useState<string | undefined>(
    initialEventWorkTypeId,
  );
  const [selectedEventWorkTypeLevelCode, setSelectedEventWorkTypeLevelCode] = useState<string>();
  useEffect(() => {
    if (!activeEventWorkTypes.length) {
      setSelectedEventWorkTypeId(undefined);
      setSelectedEventWorkTypeLevelCode(undefined);
      return;
    }
    if (!selectedEventWorkTypeId || !activeEventWorkTypes.some((item) => String(item.id) === selectedEventWorkTypeId)) {
      setSelectedEventWorkTypeId(String(activeEventWorkTypes[0].id));
      setSelectedEventWorkTypeLevelCode(undefined);
    }
  }, [activeEventWorkTypes, selectedEventWorkTypeId]);
  const eventWorkTypeLevelsQuery = useQuery({
    queryKey: [
      'competition-event-work-type-levels',
      selectedEventId,
      selectedEventWorkTypeId,
      'stage-planning-selector',
    ],
    queryFn: () => listCompetitionEventWorkTypeLevels(
      selectedEventId!, selectedEventWorkTypeId!),
    enabled: needsWorkTypeScope && Boolean(selectedEventId && selectedEventWorkTypeId),
  });
  const levelOptions = useMemo(() => {
    const configuredLevels = (eventWorkTypeLevelsQuery.data || [])
      .filter((level) => level && STANDARD_COMPETITION_WORK_TYPE_LEVELS.some(
        (item) => item.value === level.levelCode,
      ))
      .sort((left, right) => Number(right.levelCode) - Number(left.levelCode));
    return configuredLevels.map((level) => ({
      value: level.levelCode,
      label: level.levelName || STANDARD_COMPETITION_WORK_TYPE_LEVELS.find(
        (item) => item.value === level.levelCode,
      )?.label || level.levelCode,
    }));
  }, [eventWorkTypeLevelsQuery.data]);
  useEffect(() => {
    if (!levelOptions.length) return;
    if (!selectedEventWorkTypeLevelCode
      || !levelOptions.some((level) => level.value === selectedEventWorkTypeLevelCode)) {
      setSelectedEventWorkTypeLevelCode(levelOptions[0].value);
    }
  }, [levelOptions, selectedEventWorkTypeLevelCode]);

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
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            void eventsQuery.refetch();
            if (needsWorkTypeScope) {
              void queryClient.invalidateQueries({ queryKey: ['competition-event-work-types'] });
              void queryClient.invalidateQueries({ queryKey: ['competition-event-work-type-levels'] });
            }
            if (module === 'formal-exam-console') {
              void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-console-stages'] });
              void queryClient.invalidateQueries({ queryKey: ['competition-formal-exam-console-session'] });
            }
          }}
        >
          刷新赛事
        </Button>
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
              setSelectedEventWorkTypeLevelCode(undefined);
              setSelectedEventId(eventId || undefined);
            }}
          />
          {needsWorkTypeScope && (
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
                onChange={async (workTypeId) => {
                  if (!(await confirmNavigation())) return;
                  setSelectedEventWorkTypeLevelCode(undefined);
                  setSelectedEventWorkTypeId(workTypeId || undefined);
                }}
              />
              <Typography.Text type="secondary">选择等级</Typography.Text>
              <Select
                showSearch
                optionFilterProp="label"
                value={selectedEventWorkTypeLevelCode}
                disabled={!selectedEventWorkTypeId || levelOptions.length === 0}
                loading={eventWorkTypeLevelsQuery.isFetching}
                placeholder={levelOptions.length ? '请选择等级' : '请先在赛事总览选择等级'}
                style={{ width: 180, maxWidth: '100%' }}
                options={levelOptions}
                onChange={async (levelCode) => {
                  if (!(await confirmNavigation())) return;
                  setSelectedEventWorkTypeLevelCode(levelCode || undefined);
                }}
              />
              {selectedEventWorkTypeId && !eventWorkTypeLevelsQuery.isFetching && levelOptions.length === 0 && (
                <Typography.Text type="warning">请先在赛事总览为当前工种选择参赛等级</Typography.Text>
              )}
            </>
          )}
        </Space>
      </Card>
      {selectedEvent && module === 'notices' && <CompetitionNoticePage eventId={String(selectedEvent.id)} onNavigate={onNavigate} embedded />}
      {selectedEvent && module === 'stages' && selectedEventWorkTypeId && selectedEventWorkTypeLevelCode ? (
        <CompetitionStagePlanningPage
          key={`stage-planning:${selectedEvent.id}:${selectedEventWorkTypeId ?? 'event'}:${selectedEventWorkTypeLevelCode}`}
          eventId={String(selectedEvent.id)}
          eventWorkTypeId={selectedEventWorkTypeId}
          eventWorkTypeLevelCode={selectedEventWorkTypeLevelCode}
          onNavigate={onNavigate}
          onNavigateToScope={(workTypeId, levelCode) => {
            setSelectedEventWorkTypeId(String(workTypeId));
            setSelectedEventWorkTypeLevelCode(levelCode);
          }}
          embedded
        />
      ) : selectedEvent && module === 'stages' ? (
        <Card bordered={false} style={{ marginTop: 16 }}>
          <Typography.Text type="secondary">请先在赛事总览明确选择当前工种的参赛等级，再开始该等级的赛段编排。</Typography.Text>
        </Card>
      ) : null}
      {selectedEvent && module === 'theory-exam' && (
        <CompetitionTheoryExamPage
          eventId={String(selectedEvent.id)}
          initialStageId={initialStageId}
          initialEventWorkTypeId={initialEventWorkTypeId}
          onNavigate={onNavigate}
          embedded
        />
      )}
      {selectedEvent && module === 'formal-exam-console' && selectedEventWorkTypeId && selectedEventWorkTypeLevelCode ? (
        <CompetitionFormalExamConsolePage
          key={`formal-exam:${selectedEvent.id}:${selectedEventWorkTypeId}:${selectedEventWorkTypeLevelCode}`}
          eventId={String(selectedEvent.id)}
          eventWorkTypeId={selectedEventWorkTypeId}
          eventWorkTypeLevelCode={selectedEventWorkTypeLevelCode}
          initialStageId={initialStageId}
        />
      ) : selectedEvent && module === 'formal-exam-console' ? (
        <Card bordered={false} style={{ marginTop: 16 }}>
          <Typography.Text type="secondary">
            请先选择工种和技能等级。正式比赛按「赛事 + 工种 + 等级」隔离，与赛段编排、选手试卷配置同一范围；只点「刷新赛事」不会带出该工种等级下的赛段。
          </Typography.Text>
        </Card>
      ) : null}
      {selectedEvent && module === 'scores' && <CompetitionScoreRankingPage eventId={String(selectedEvent.id)} onNavigate={onNavigate} embedded />}
    </div>
  );
}
