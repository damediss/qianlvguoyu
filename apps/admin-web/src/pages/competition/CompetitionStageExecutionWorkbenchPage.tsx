import { Alert, Card, Result, Spin, message } from 'antd';
import * as React from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { getCompetitionStageWorkspace, replaceCompetitionStageContents } from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type { CompetitionStage, CompetitionStageContentType } from '../../types/competition';
import { StageContentPanel, type StageContentConfig } from './stage-planning/StageContentPanel';

function errorText(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请刷新后重试';
}

export function CompetitionStageExecutionWorkbenchPage({ stage }: { stage: CompetitionStage }) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:stage-plan:view');
  const canManage = permissions.includes('competition:stage-plan:manage');

  const stageWorkspaceQuery = useQuery({
    queryKey: ['competition-stage-workspace', stage.id],
    queryFn: () => getCompetitionStageWorkspace(stage.id),
    enabled: canView,
  });

  const contentSaveMutation = useMutation({
    mutationFn: (contents: StageContentConfig[]) => replaceCompetitionStageContents(
      stage.id,
      contents.map(({ contentType, weight }) => ({
        contentType: contentType as CompetitionStageContentType,
        status: 'enabled' as const,
        weight,
        providerCode: contentType === 'simulation' ? 'simulation-placeholder' : undefined,
      })),
    ),
    onSuccess: async () => {
      await stageWorkspaceQuery.refetch();
      message.success('赛段内容与权重已保存');
    },
    onError: (error) => message.error(errorText(error)),
  });

  if (!canView) return <Result status="403" title="暂无执行工作台查看权限" />;
  if (stageWorkspaceQuery.isLoading) return <Spin className="competition-loading" />;
  if (stageWorkspaceQuery.isError) return <Alert type="error" message="赛段内容加载失败" />;

  const workspaceStage = stageWorkspaceQuery.data?.stage || stage;
  const stageEditable = canManage && (workspaceStage.status === 'draft' || workspaceStage.status === 'configuring');

  return (
    <Card bordered={false} style={{ marginTop: 16 }}>
      <StageContentPanel
        stage={workspaceStage}
        contents={stageWorkspaceQuery.data?.contents || []}
        contentEditable={stageEditable}
        contentSaving={contentSaveMutation.isPending}
        onSave={(contents) => contentSaveMutation.mutate(contents)}
      />
    </Card>
  );
}
