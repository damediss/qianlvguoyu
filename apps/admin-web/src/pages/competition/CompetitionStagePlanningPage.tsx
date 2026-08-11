import { Button, Modal, Result, Spin, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  confirmCompetitionWorkTypeStagePlan,
  getCompetitionEvent,
  getCompetitionStageWorkspace,
  listCompetitionEventWorkTypes,
  listCompetitionWorkTypeStages,
  pageCompetitionStages,
  replaceCompetitionStageContents,
  replaceCompetitionWorkTypeStages,
} from '../../api/competition';
import { confirmCompetitionStagePlan, saveCompetitionStageDraft, saveCompetitionStageDraftForWorkType } from '../../api/competitionStagePlanning';
import { useAuthStore } from '../../stores/authStore';
import {
  isBusinessStageNode,
  type StageValidationIssue,
} from '../../types/competitionStagePlanning';
import { useUnsavedChangesBlocker } from '../../hooks/useUnsavedChangesBlocker';
import './CompetitionEventManagementPage.css';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import { CompetitionTheoryExamPage } from './CompetitionTheoryExamPage';
import { selectedContentTypes, stageContentConfigsAreValid } from './stage-planning/StageContentPanel';
import { StageInspector } from './stage-planning/StageInspector';
import { StageNodeLibrary } from './stage-planning/StageNodeLibrary';
import { StagePlanningCanvas } from './stage-planning/StagePlanningCanvas';
import { StagePlanningToolbar } from './stage-planning/StagePlanningToolbar';
import { StageValidationPanel } from './stage-planning/StageValidationPanel';
import { StageSettlementPromotionPanel } from './stage-planning/StageSettlementPromotionPanel';
import { readStageCanvasNotes, writeStageCanvasNotes } from './stage-planning/stageCanvasStorage';
import { useStagePlanningDraft } from './stage-planning/useStagePlanningDraft';
import type {
  CompetitionEvent,
  CompetitionEventWorkType,
  CompetitionId,
  CompetitionStage,
  CompetitionStageContentType,
  CompetitionWorkTypeStageConfig,
} from '../../types/competition';
import type { StageDraftSavePayload } from '../../types/competitionStagePlanning';

function errorText(error: unknown, fallback = '保存失败，请刷新后重试') {
  return error instanceof Error ? error.message : fallback;
}

function mergeWorkTypeStageDraft(
  eventStages: CompetitionStage[],
  draftPayload: StageDraftSavePayload,
): StageDraftSavePayload {
  const byId = new Map(draftPayload.stages.filter((stage) => stage.id).map((stage) => [String(stage.id), stage]));
  const byType = new Map<string, StageDraftSavePayload['stages'][number]>(
    draftPayload.stages.map((stage) => [stage.stageType, stage]),
  );
  const merged = eventStages.map((stage) => byId.get(String(stage.id)) ?? byType.get(stage.stageType) ?? {
    id: String(stage.id),
    stageType: stage.stageType as StageDraftSavePayload['stages'][number]['stageType'],
    startAt: stage.startAt ?? undefined,
    endAt: stage.endAt ?? undefined,
    description: stage.description ?? undefined,
  });
  const newStages = draftPayload.stages.filter((stage) => !stage.id);
  return { stages: [...merged, ...newStages] };
}

export function CompetitionStagePlanningPage({
  eventId,
  eventWorkTypeId,
  onNavigate,
  embedded = false,
}: {
  eventId: string;
  eventWorkTypeId?: CompetitionId;
  onNavigate: (path: string) => void;
  embedded?: boolean;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const tenantId = useAuthStore((state) => state.tenantId) ?? 'unknown';
  const canView = permissions.includes('competition:event:view') && permissions.includes('competition:stage:view');
  const canManage = permissions.includes('competition:stage:manage');
  const queryClient = useQueryClient();
  const [validationOpen, setValidationOpen] = useState(false);
  const [validationIssues, setValidationIssues] = useState<StageValidationIssue[]>([]);
  const [theoryWorkbench, setTheoryWorkbench] = useState<{
    stageId: string;
    eventWorkTypeId?: string;
  } | null>(null);
  const [promotionDraft, setPromotionDraft] = useState<CompetitionWorkTypeStageConfig[]>([]);
  const [promotionDirty, setPromotionDirty] = useState(false);
  const fitViewRef = useRef<() => void>(() => undefined);

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canView,
  });
  const stagesQuery = useQuery({
    queryKey: ['competition-stages', eventId, 'planning'],
    queryFn: () => pageCompetitionStages(eventId, { page: 1, pageSize: 100 }),
    enabled: canView,
  });
  const eventStages = useMemo(() => stagesQuery.data?.items || [], [stagesQuery.data?.items]);
  const event = eventQuery.data;
  const canViewPromotionRules = permissions.includes('competition:event-work-type:view');
  const eventWorkTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId, 'stage-planning-status'],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: canView && canViewPromotionRules && Boolean(eventWorkTypeId),
  });
  const selectedEventWorkType = eventWorkTypesQuery.data?.find(
    (item) => String(item.id) === String(eventWorkTypeId),
  );
  const stagePlanConfirmed = eventWorkTypeId
    ? selectedEventWorkType?.stagePlanStatus === 'confirmed' || event?.stagePlanStatus === 'confirmed'
    : event?.stagePlanStatus === 'confirmed';
  const workTypeStatusReady = !eventWorkTypeId || Boolean(eventWorkTypesQuery.data);
  const editable = Boolean(
    event
    && event.status === 'draft'
    && workTypeStatusReady
    && !stagePlanConfirmed
    && canManage,
  );
  const promotionScopeQuery = useQuery({
    queryKey: ['competition-work-type-stages', eventId, eventWorkTypeId],
    queryFn: () => listCompetitionWorkTypeStages(eventId, eventWorkTypeId!),
    enabled: canViewPromotionRules && Boolean(eventWorkTypeId),
  });
  const serverStages = useMemo(() => {
    if (!eventWorkTypeId || !canViewPromotionRules) return eventStages;
    if (!promotionScopeQuery.data) return [];
    const enabledStageIds = new Set(
      promotionScopeQuery.data
        .filter((item) => item.enabled === 1)
        .map((item) => String(item.stageId)),
    );
    return eventStages.filter((stage) => enabledStageIds.has(String(stage.id)));
  }, [canViewPromotionRules, eventStages, eventWorkTypeId, promotionScopeQuery.data]);
  const initialNotes = useMemo(
    () => readStageCanvasNotes(tenantId, eventId, eventWorkTypeId),
    [eventId, eventWorkTypeId, tenantId],
  );
  const draft = useStagePlanningDraft({
    eventId: `${eventId}:${eventWorkTypeId ?? 'event'}`,
    serverStages,
    stageCatalog: eventStages,
    initialNotes,
    editable,
    eventStartAt: event?.startAt,
    eventEndAt: event?.endAt,
  });
  const selectedStageId = draft.selectedNode && isBusinessStageNode(draft.selectedNode)
    ? draft.selectedNode.data.stageId
    : undefined;
  const selectedStage = serverStages.find((stage) => String(stage.id) === String(selectedStageId));
  const selectedStageWorkspaceQuery = useQuery({
    queryKey: ['competition-stage-workspace', selectedStageId],
    queryFn: () => getCompetitionStageWorkspace(selectedStageId!),
    enabled: Boolean(selectedStageId) && permissions.includes('competition:stage-plan:view'),
  });
  const selectedStageContents = draft.selectedNode && isBusinessStageNode(draft.selectedNode)
    ? draft.selectedNode.data.contents ?? selectedStageWorkspaceQuery.data?.contents ?? []
    : [];
  const stageContentTypesByNodeId = useMemo(() => {
    if (!draft.selectedNode || !isBusinessStageNode(draft.selectedNode)) return {};
    return { [draft.selectedNode.id]: selectedContentTypes(selectedStageContents) };
  }, [draft.selectedNode, selectedStageContents]);
  const onStageContentTypeClick = useCallback((nodeId: string, contentType: CompetitionStageContentType) => {
    if (contentType !== 'theory') return;
    const node = draft.nodes.find((item) => item.id === nodeId);
    if (!node || !isBusinessStageNode(node) || node.data.stageId === undefined || node.data.stageId === null) {
      message.warning('请先保存当前赛段草稿，再进入理论工作台');
      return;
    }
    setTheoryWorkbench({
      stageId: String(node.data.stageId),
      eventWorkTypeId: eventWorkTypeId === undefined || eventWorkTypeId === null
        ? undefined
        : String(eventWorkTypeId),
    });
  }, [draft.nodes, eventWorkTypeId]);
  useEffect(() => {
    if (!eventWorkTypeId) {
      setPromotionDraft([]);
      setPromotionDirty(false);
      return;
    }
    if (promotionScopeQuery.data) {
      setPromotionDraft(promotionScopeQuery.data);
      setPromotionDirty(false);
    }
  }, [eventWorkTypeId, promotionScopeQuery.data]);
  const hasUnsavedChanges = draft.dirty || promotionDirty;
  useUnsavedChangesBlocker(hasUnsavedChanges);

  const updatePromotionPercentage = useCallback((
    stageType: CompetitionWorkTypeStageConfig['stageType'],
    stageId: CompetitionId,
    stageName: string,
    percentage: number | null,
  ) => {
    if (!eventWorkTypeId) return;
    setPromotionDraft((current) => {
      const existing = current.find((item) => item.stageType === stageType);
      const next: CompetitionWorkTypeStageConfig = {
        id: existing?.id,
        eventId,
        eventWorkTypeId,
        stageId: existing?.stageId ?? stageId,
        stageType,
        stageName,
        enabled: 1,
        sortOrder: existing?.sortOrder,
        promotionRuleType: percentage == null ? 'LEGACY' : 'PERCENTAGE',
        promotionPercentage: percentage,
        promotionQuota: null,
        legacyRule: percentage == null ? 1 : 0,
      };
      return existing
        ? current.map((item) => item.stageType === stageType ? next : item)
        : [...current, next];
    });
    setPromotionDirty(true);
  }, [eventId, eventWorkTypeId]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const draftPayload = draft.payload;
      const payload = eventWorkTypeId
        ? mergeWorkTypeStageDraft(eventStages, draftPayload)
        : draftPayload;
      const savedStages = eventWorkTypeId
        ? await saveCompetitionStageDraftForWorkType(eventId, eventWorkTypeId, payload)
        : await saveCompetitionStageDraft(eventId, payload);
      const stagesByType = new Map(savedStages.map((stage) => [stage.stageType, stage]));
      await Promise.all(draftPayload.stages
        .filter((stage) => stage.contents !== undefined)
        .map((stage) => {
          const savedStage = stagesByType.get(stage.stageType);
          if (!savedStage || !stage.contents) return undefined;
          return replaceCompetitionStageContents(savedStage.id, stage.contents.map(({ contentType, weight }) => ({
            contentType,
            status: 'enabled' as const,
            weight,
            providerCode: contentType === 'simulation' ? 'simulation-placeholder' : undefined,
          })));
        }));
      let savedPromotionConfigs: CompetitionWorkTypeStageConfig[] | undefined;
      const visibleStageTypes = new Set<string>(draftPayload.stages.map((stage) => stage.stageType));
      if (eventWorkTypeId && (promotionDirty || draft.dirty)) {
        const promotionByStageType = new Map(promotionDraft.map((item) => [item.stageType, item]));
        savedPromotionConfigs = await replaceCompetitionWorkTypeStages(
          eventId,
          eventWorkTypeId,
          savedStages.map((stage) => {
            const configured = promotionByStageType.get(stage.stageType);
            const newFinalStage = stage.stageType === 'final' && !configured?.id;
            return {
              stageId: stage.id,
              // API 仍要求该兼容字段；启用范围由画布当前保留的节点决定。
              enabled: visibleStageTypes.has(stage.stageType),
              sortOrder: stage.sortOrder,
              promotionRuleType: newFinalStage ? 'FINAL_PODIUM' : configured?.promotionRuleType ?? 'LEGACY',
              promotionPercentage: newFinalStage ? null : configured?.promotionPercentage ?? null,
              promotionQuota: newFinalStage ? null : configured?.promotionQuota ?? null,
            };
          }),
        );
      }
      return { savedStages, savedPromotionConfigs, visibleStageTypes: [...visibleStageTypes] };
    },
    onMutate: () => draft.setSaving(true),
    onSuccess: ({ savedStages, savedPromotionConfigs, visibleStageTypes }) => {
      const notes = draft.currentNotes;
      writeStageCanvasNotes(tenantId, eventId, notes, eventWorkTypeId);
      const savedPlanningStages = eventWorkTypeId
        ? savedStages.filter((stage) => visibleStageTypes.includes(stage.stageType))
        : savedStages;
      draft.acceptSavedStages(savedPlanningStages, notes);
      queryClient.setQueryData(['competition-stages', eventId, 'planning'], {
        items: savedStages,
        page: 1,
        pageSize: 100,
        total: savedStages.length,
      });
      if (savedPromotionConfigs && eventWorkTypeId) {
        setPromotionDraft(savedPromotionConfigs);
        setPromotionDirty(false);
        queryClient.setQueryData(
          ['competition-work-type-stages', eventId, eventWorkTypeId],
          savedPromotionConfigs,
        );
      }
      queryClient.invalidateQueries({ queryKey: ['competition-stages', eventId] });
      message.success('赛段草稿已保存');
    },
    onError: (error) => {
      draft.setSaving(false);
      message.error(errorText(error));
    },
  });

  const confirmMutation = useMutation<CompetitionEvent | CompetitionEventWorkType, Error>({
    mutationFn: () => eventWorkTypeId
      ? confirmCompetitionWorkTypeStagePlan(eventId, eventWorkTypeId)
      : confirmCompetitionStagePlan(eventId),
    onSuccess: (confirmedScope) => {
      if (eventWorkTypeId) {
        queryClient.invalidateQueries({ queryKey: ['competition-event-work-types', eventId] });
        message.success('当前工种赛段编排已确认并锁定');
        return;
      }
      const confirmedEvent = confirmedScope as Awaited<ReturnType<typeof confirmCompetitionStagePlan>>;
      queryClient.setQueryData(['competition-event', eventId], confirmedEvent);
      queryClient.invalidateQueries({ queryKey: ['competition-events'] });
      message.success('赛段编排已确认并锁定');
    },
    onError: (error) => message.error(errorText(error, '确认失败，请刷新后重试')),
  });

  const onSave = () => {
    const validation = draft.validate();
    if (validation.some((item) => item.level === 'error')) {
      message.error('请先修复编排校验中的阻断问题');
      return;
    }
    const invalidContent = draft.nodes.find((node) => (
      isBusinessStageNode(node)
      && node.data.contents !== undefined
      && !stageContentConfigsAreValid(node.data.contents)
    ));
    if (invalidContent) {
      message.error('请先完善赛段内容并确保权重合计为 100%');
      draft.setSelection({ nodeId: invalidContent.id });
      return;
    }
    if (promotionDirty && !eventWorkTypeId) {
      message.error('请先选择工种，再保存晋级规则');
      return;
    }
    const invalidPromotionPercentage = promotionDraft.some((item) => (
      item.promotionRuleType === 'PERCENTAGE'
      && (!Number.isInteger(item.promotionPercentage) || (item.promotionPercentage ?? 0) < 1 || (item.promotionPercentage ?? 0) > 100)
    ));
    if (invalidPromotionPercentage) {
      message.error('晋级百分比必须为 1～100 的整数');
      return;
    }
    saveMutation.mutate();
  };

  const onDiscard = () => {
    Modal.confirm({
      title: '放弃未保存修改？',
      content: '当前画布草稿将恢复为最近一次保存的状态。',
      onOk: () => {
        draft.discard();
        setPromotionDraft(promotionScopeQuery.data ?? []);
        setPromotionDirty(false);
      },
    });
  };

  const onConfirm = () => {
    if (hasUnsavedChanges) {
      message.warning('请先保存赛段草稿，再确认编排');
      return;
    }
    const validation = draft.validate();
    if (validation.some((item) => item.level === 'error')) {
      message.error('请先修复编排校验中的阻断问题');
      return;
    }
    Modal.confirm({
      title: '确认并锁定赛段编排？',
      content: '确认后当前赛事的赛段结构、时间和说明将不能再编辑，请确认草稿已检查无误。',
      okText: '确定锁定',
      cancelText: '取消',
      onOk: () => confirmMutation.mutate(),
    });
  };

  const onSelectIssue = (issue: StageValidationIssue) => {
    if (issue.targetType === 'node' && issue.targetId) draft.setSelection({ nodeId: issue.targetId });
    if (issue.targetType === 'edge' && issue.targetId) draft.setSelection({ edgeId: issue.targetId });
  };

  const onValidate = () => {
    const result = draft.validate();
    setValidationIssues(result);
    setValidationOpen(true);
  };

  const onSelectValidationIssue = (issue: StageValidationIssue) => {
    onSelectIssue(issue);
    setValidationOpen(false);
  };

  const onFitViewReady = useCallback((fitView: () => void) => {
    fitViewRef.current = fitView;
  }, []);

  if (!canView) {
    return <Result status="403" title="暂无赛段查看权限" subTitle="请联系管理员申请 competition:stage:view 权限。" />;
  }
  if (
    eventQuery.isLoading
    || stagesQuery.isLoading
    || (Boolean(eventWorkTypeId) && (promotionScopeQuery.isLoading || eventWorkTypesQuery.isLoading))
  ) {
    return <Spin className="competition-loading" />;
  }
  if (eventQuery.isError || !event) {
    return <Result status="404" title="赛事不存在或无法访问" extra={<Button onClick={() => onNavigate('/competition/events')}>返回赛事列表</Button>} />;
  }
  if (stagesQuery.isError) {
    return <Result status="error" title="赛段加载失败" extra={<Button onClick={() => stagesQuery.refetch()}>重新加载</Button>} />;
  }
  if (eventWorkTypeId && canViewPromotionRules && promotionScopeQuery.isError) {
    return <Result status="error" title="工种赛段配置加载失败" extra={<Button onClick={() => promotionScopeQuery.refetch()}>重新加载</Button>} />;
  }
  if (eventWorkTypeId && canViewPromotionRules && eventWorkTypesQuery.isError) {
    return <Result status="error" title="工种编排状态加载失败" extra={<Button onClick={() => eventWorkTypesQuery.refetch()}>重新加载</Button>} />;
  }

  const content = (
    <>
      <StagePlanningToolbar
        editable={editable}
        dirty={hasUnsavedChanges}
        saving={draft.saving || saveMutation.isPending}
        errorCount={draft.issues.filter((item) => item.level === 'error').length}
        warningCount={draft.issues.filter((item) => item.level === 'warning').length}
        onAutoLayout={draft.autoLayout}
        onValidate={onValidate}
        onSave={onSave}
        onDiscard={onDiscard}
        onFitView={() => fitViewRef.current()}
        onConfirm={onConfirm}
        confirming={confirmMutation.isPending}
      />

      <div className="stage-planning-shell">
        <StageNodeLibrary usedTypes={draft.usedTypes} editable={editable} />
        <StagePlanningCanvas
          nodes={draft.nodes}
          edges={draft.edges}
          editable={editable}
          stagePlanConfirmed={stagePlanConfirmed}
          onNodesChange={draft.onNodesChange}
          onEdgesChange={draft.onEdgesChange}
          onConnect={draft.onConnect}
          onSelectionChange={draft.onSelectionChange}
          onAddStage={draft.addStage}
          onStageContextMenu={draft.selectNode}
          onStageContentTypeClick={onStageContentTypeClick}
          stageContentTypesByNodeId={stageContentTypesByNodeId}
          onFitViewReady={onFitViewReady}
        />
        <StageInspector
          node={draft.selectedNode}
          edge={draft.selectedEdge}
          nodes={draft.nodes}
          eventStartAt={event.startAt}
          eventEndAt={event.endAt}
          stagePlanConfirmed={stagePlanConfirmed}
          editable={editable}
          contentVisible={permissions.includes('competition:stage-plan:view')}
          contentEditable={editable && permissions.includes('competition:stage-plan:manage')}
          contentLoading={selectedStageWorkspaceQuery.isLoading || selectedStageWorkspaceQuery.isFetching}
          contents={selectedStageContents}
          promotionVisible={canViewPromotionRules}
          eventWorkTypeId={eventWorkTypeId}
          promotionConfigs={promotionDraft}
          promotionLoading={promotionScopeQuery.isLoading || promotionScopeQuery.isFetching}
          onPromotionPercentageChange={updatePromotionPercentage}
          onNodeChange={draft.updateNode}
          onEdgeChange={draft.updateEdge}
          onContentChange={(nodeId, contents) => draft.updateNode(nodeId, { contents })}
        />
      </div>

      {selectedStage && <StageSettlementPromotionPanel eventId={eventId} stage={selectedStage} />}

      <Modal
        title="编排校验结果"
        open={validationOpen}
        onCancel={() => setValidationOpen(false)}
        footer={null}
        width={560}
        destroyOnClose
      >
        <StageValidationPanel issues={validationIssues} onSelectIssue={onSelectValidationIssue} />
      </Modal>

      <Modal
        title="理论配置"
        open={Boolean(theoryWorkbench)}
        onCancel={() => setTheoryWorkbench(null)}
        footer={null}
        centered
        width="min(1440px, calc(100vw - 48px))"
        className="stage-theory-workbench-modal"
        styles={{ body: { maxHeight: 'calc(100vh - 132px)', overflowY: 'auto', padding: 0 } }}
        destroyOnClose
        data-testid="stage-theory-workbench-modal"
      >
        {theoryWorkbench && (
          <CompetitionTheoryExamPage
            eventId={eventId}
            initialStageId={theoryWorkbench.stageId}
            initialEventWorkTypeId={theoryWorkbench.eventWorkTypeId}
            onNavigate={onNavigate}
            embedded
            stagePlanningEditor
          />
        )}
      </Modal>
    </>
  );

  return embedded
    ? content
    : <CompetitionEventFrame event={event} activeSection="overview" onNavigate={onNavigate}>{content}</CompetitionEventFrame>;
}
