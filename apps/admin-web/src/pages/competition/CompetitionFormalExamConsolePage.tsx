import { ReloadOutlined, SettingOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Empty, Result, Select, Space, Spin, Typography, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import {
  getCompetitionFormalExamSessionByStage,
  pageCompetitionStages,
  prepareCompetitionFormalExamSession,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type { CompetitionFormalExamSession, CompetitionId, CompetitionStage } from '../../types/competition';
import { CompetitionFormalExamSessionPanel } from './CompetitionFormalExamSessionPanel';

function idValue(value?: CompetitionId | null) {
  return value == null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

function stageLabel(stage: CompetitionStage) {
  return [stage.name, stage.eventWorkTypeLevelCode ? `等级${stage.eventWorkTypeLevelCode}` : undefined]
    .filter(Boolean)
    .join(' · ');
}

/** Standalone entry for the theory formal-exam runtime console. */
export function CompetitionFormalExamConsolePage({
  eventId,
  eventWorkTypeId,
  eventWorkTypeLevelCode,
  initialStageId,
}: {
  eventId: string;
  eventWorkTypeId?: string;
  eventWorkTypeLevelCode?: string;
  initialStageId?: string;
}) {
  const canView = useAuthStore((state) => state.permissionCodes.includes('competition:exam-session:view'));
  const canPrepare = useAuthStore((state) => state.permissionCodes.includes('competition:exam-session:prepare'));
  const queryClient = useQueryClient();
  const [selectedStageId, setSelectedStageId] = useState<string | undefined>(initialStageId);
  const scopeReady = Boolean(eventWorkTypeId && eventWorkTypeLevelCode?.trim());

  const stagesQuery = useQuery({
    queryKey: [
      'competition-formal-exam-console-stages',
      eventId,
      eventWorkTypeId ?? 'unset',
      eventWorkTypeLevelCode ?? 'unset',
    ],
    queryFn: () => pageCompetitionStages(eventId, {
      page: 1,
      pageSize: 500,
      eventWorkTypeId,
      eventWorkTypeLevelCode,
    }),
    enabled: canView && scopeReady,
  });
  const stages = stagesQuery.data?.items || [];
  const selectedStage = useMemo(
    () => stages.find((stage) => idValue(stage.id) === selectedStageId),
    [selectedStageId, stages],
  );

  useEffect(() => {
    if (!stages.length) {
      if (selectedStageId) setSelectedStageId(undefined);
      return;
    }
    if (!selectedStageId || !stages.some((stage) => idValue(stage.id) === selectedStageId)) {
      const requestedStage = initialStageId && stages.some((stage) => idValue(stage.id) === initialStageId)
        ? initialStageId
        : undefined;
      setSelectedStageId(requestedStage || idValue(stages[0].id));
    }
  }, [initialStageId, selectedStageId, stages]);

  const sessionQuery = useQuery({
    queryKey: ['competition-formal-exam-console-session', eventId, selectedStageId],
    queryFn: () => getCompetitionFormalExamSessionByStage(selectedStageId!),
    enabled: canView && scopeReady && Boolean(selectedStageId),
  });
  const prepareMutation = useMutation({
    mutationFn: () => prepareCompetitionFormalExamSession(selectedStageId!),
    onSuccess: (session: CompetitionFormalExamSession) => {
      queryClient.setQueryData(
        ['competition-formal-exam-console-session', eventId, selectedStageId],
        session,
      );
      message.success('正式比赛运行实例已初始化');
    },
    onError: (error) => message.error(errorText(error, '初始化正式比赛失败，请先完成选手试卷配置')),
  });

  if (!canView) {
    return <Result status="403" title="暂无理论比赛控制台查看权限" />;
  }
  if (stagesQuery.isError) {
    return <Result status="error" title="赛段列表加载失败" extra={<Button onClick={() => stagesQuery.refetch()}>重新加载</Button>} />;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%', marginTop: 16 }}>
      <Card size="small" bordered={false}>
        <Space wrap align="center" style={{ width: '100%' }}>
          <SettingOutlined />
          <Typography.Text type="secondary">选择赛段</Typography.Text>
          <Select
            showSearch
            optionFilterProp="label"
            value={selectedStageId}
            loading={stagesQuery.isFetching}
            disabled={!scopeReady || (!stagesQuery.isFetching && stages.length === 0)}
            placeholder={scopeReady ? '请选择需要控制的理论考试赛段' : '请先选择工种和技能等级'}
            style={{ width: 320, maxWidth: '100%' }}
            options={stages.map((stage) => ({ value: idValue(stage.id), label: stageLabel(stage) }))}
            onChange={setSelectedStageId}
          />
          <Button icon={<ReloadOutlined />} loading={stagesQuery.isFetching} onClick={() => stagesQuery.refetch()}>
            刷新赛段
          </Button>
          {selectedStage ? <Typography.Text type="secondary">当前控制范围：{stageLabel(selectedStage)}</Typography.Text> : null}
        </Space>
      </Card>

      {!scopeReady ? (
        <Card bordered={false}>
          <Empty description="请先选择工种和技能等级。正式比赛只读取该工种+等级下的赛段，只刷新赛事列表不会带出已配置的赛段。" />
        </Card>
      ) : null}

      {scopeReady && !stagesQuery.isFetching && stages.length === 0 ? (
        <Card bordered={false}>
          <Empty description="当前工种等级下暂无可控制的赛段。请确认已在「赛段编排」配置该工种等级的赛段，再点「刷新赛段」。" />
        </Card>
      ) : null}

      {selectedStageId && sessionQuery.isLoading ? (
        <Card bordered={false}><Spin /></Card>
      ) : null}

      {sessionQuery.isError ? (
        <Alert type="error" showIcon message={errorText(sessionQuery.error, '正式比赛运行实例加载失败')} />
      ) : null}

      {selectedStage && !sessionQuery.isLoading && !sessionQuery.isError && !sessionQuery.data ? (
        <Card bordered={false} title="正式比赛运行实例">
          <Space direction="vertical" size={12}>
            <Typography.Text type="secondary">
              当前赛段尚未生成理论考试正式比赛运行实例。请确认选手试卷配置已完成，再从本控制台初始化。
            </Typography.Text>
            {canPrepare ? (
              <Button
                type="primary"
                loading={prepareMutation.isPending}
                onClick={() => prepareMutation.mutate()}
              >
                初始化正式比赛
              </Button>
            ) : (
              <Typography.Text type="warning">当前账号没有初始化正式比赛的权限。</Typography.Text>
            )}
          </Space>
        </Card>
      ) : null}

      {sessionQuery.data ? (
        <CompetitionFormalExamSessionPanel
          key={idValue(sessionQuery.data.id)}
          open
          embedded
          loading={false}
          session={sessionQuery.data}
          onClose={() => undefined}
        />
      ) : null}
    </Space>
  );
}
