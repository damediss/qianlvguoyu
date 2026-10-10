import { Alert, Button, Modal, Result, Spin, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  confirmCompetitionWorkTypeStagePlan,
  getCompetitionEvent,
  getCompetitionStageWorkspace,
  listCompetitionEventWorkTypeLevels,
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
import { CompetitionPlayerPaperAssignmentPage } from './CompetitionPlayerPaperAssignmentPage';
import { CompetitionPracticalConfigPage } from './CompetitionPracticalConfigPage';
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
  CompetitionStageContentType,
  CompetitionWorkTypeStageConfig,
} from '../../types/competition';

function errorText(error: unknown, fallback = '保存失败，请刷新后重试') {
  return error instanceof Error ? error.message : fallback;
}
export function CompetitionStagePlanningPage({
  eventId,
  eventWorkTypeId,
  eventWorkTypeLevelCode,
  onNavigate,
  embedded = false,
}: {
  eventId: string;
  eventWorkTypeId?: CompetitionId;
  eventWorkTypeLevelCode?: string;
  onNavigate: (path: string) => void;
  onNavigateToScope?: (eventWorkTypeId: CompetitionId, levelCode: string) => void;
  embedded?: boolean;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const tenantId = useAuthStore((state) => state.tenantId) ?? 'unknown';
  const canView = permissions.includes('competition:event:view') && permissions.includes('competition:stage:view');
  const canManage = permissions.includes('competition:stage:manage');
  const canManageWorkTypeStages = permissions.includes('competition:event-work-type:manage');
  const canViewPlayerAssignment = permissions.includes('competition:stage-paper-assignment:view');
  const canViewRefereeAssignment = permissions.includes('competition:referee-assignment:view');
  const queryClient = useQueryClient();
  const [validationOpen, setValidationOpen] = useState(false);
  const [validationIssues, setValidationIssues] = useState<StageValidationIssue[]>([]);
  const [theoryWorkbench, setTheoryWorkbench] = useState<{
    stageId: string;
    eventWorkTypeId?: string;
    eventWorkTypeLevelCode?: string;
  } | null>(null);
  const [practicalWorkbench, setPracticalWorkbench] = useState<{
    stageId: string;
    stageName: string;
    eventWorkTypeId: string;
    eventWorkTypeName?: string;
    eventWorkTypeLevelCode?: string;
    eventWorkTypeLevelName?: string;
    initialTab: 'paper' | 'referee' | 'runtime';
  } | null>(null);
  const [playerAssignmentStageId, setPlayerAssignmentStageId] = useState<string | null>(null);
  const [playerAssignmentContentType, setPlayerAssignmentContentType] = useState<
    'theory' | 'practical'
  >('theory');
  const [playerAssignmentDirty, setPlayerAssignmentDirty] = useState(false);
  const [promotionDraft, setPromotionDraft] = useState<CompetitionWorkTypeStageConfig[]>([]);
  const [promotionDirty, setPromotionDirty] = useState(false);
  const fitViewRef = useRef<() => void>(() => undefined);

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canView,
  });
  const event = eventQuery.data;
  const stagesQuery = useQuery({
    queryKey: [
      'competition-stages',
      eventId,
      'planning',
      eventWorkTypeId ?? 'event',
      eventWorkTypeLevelCode ?? 'event',
    ],
    queryFn: () => pageCompetitionStages(eventId, {
      page: 1,
      pageSize: 100,
      eventWorkTypeId,
      eventWorkTypeLevelCode,
    }),
    enabled: canView,
  });
  const eventStages = useMemo(() => stagesQuery.data?.items || [], [stagesQuery.data?.items]);
  const canViewPromotionRules = permissions.includes('competition:event-work-type:view');
  const eventWorkTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId, 'stage-planning-status'],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: canView && canViewPromotionRules && Boolean(eventWorkTypeId),
  });
  const selectedEventWorkType = eventWorkTypesQuery.data?.find(
    (item) => String(item.id) === String(eventWorkTypeId),
  );
  const hasLevelScope = Boolean(eventWorkTypeId && eventWorkTypeLevelCode?.trim());
  const eventWorkTypeLevelsQuery = useQuery({
    queryKey: [
      'competition-event-work-type-levels',
      eventId,
      eventWorkTypeId,
      'stage-planning-selector',
    ],
    queryFn: () => listCompetitionEventWorkTypeLevels(eventId, eventWorkTypeId!),
    enabled: canView && canViewPromotionRules && hasLevelScope,
  });
  const selectedEventWorkTypeLevel = eventWorkTypeLevelsQuery.data?.find(
    (item) => item.levelCode === eventWorkTypeLevelCode,
  );
  const stagePlanConfirmed = eventWorkTypeId
    ? event?.stagePlanStatus === 'confirmed'
      || (hasLevelScope
        ? selectedEventWorkTypeLevel?.stagePlanStatus === 'confirmed'
        : selectedEventWorkType?.stagePlanStatus === 'confirmed')
    : event?.stagePlanStatus === 'confirmed';
  const workTypeStatusReady = !eventWorkTypeId || Boolean(eventWorkTypesQuery.data);
  const levelStatusReady = !hasLevelScope || Boolean(eventWorkTypeLevelsQuery.data);
  const editable = Boolean(
    event
    && ['draft', 'published'].includes(event.status)
    && workTypeStatusReady
    && levelStatusReady
    && !stagePlanConfirmed
    && canManage,
  );
  const promotionScopeQuery = useQuery({
    queryKey: [
      'competition-work-type-stages',
      eventId,
      eventWorkTypeId,
      eventWorkTypeLevelCode ?? 'legacy',
    ],
    queryFn: () => listCompetitionWorkTypeStages(eventId, eventWorkTypeId!, eventWorkTypeLevelCode),
    enabled: canViewPromotionRules && Boolean(eventWorkTypeId),
  });
  // The stage endpoint is already scoped to the selected work type and level. Do not
  // fall back to event-level rows while the promotion configuration is loading.
  const serverStages = eventStages;
  const initialNotes = useMemo(
    () => readStageCanvasNotes(tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelCode),
    [eventId, eventWorkTypeId, eventWorkTypeLevelCode, tenantId],
  );
  const draft = useStagePlanningDraft({
    eventId: `${eventId}:${eventWorkTypeId ?? 'event'}:${eventWorkTypeLevelCode ?? 'event'}`,
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
    queryKey: [
      'competition-stage-workspace',
      eventId,
      eventWorkTypeId ?? 'event',
      eventWorkTypeLevelCode ?? 'event',
      selectedStageId,
    ],
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
  const onStageContentTypeClick = useCallback((
    nodeId: string,
    contentType: CompetitionStageContentType,
    practicalTab: 'paper' | 'runtime' = 'paper',
  ) => {
    const node = draft.nodes.find((item) => item.id === nodeId);
    if (!node || !isBusinessStageNode(node) || node.data.stageId === undefined || node.data.stageId === null) {
      message.warning('请先保存当前赛段编排，再进入配置工作台');
      return;
    }
    if (contentType === 'theory') {
      setTheoryWorkbench({
        stageId: String(node.data.stageId),
        eventWorkTypeId: eventWorkTypeId === undefined || eventWorkTypeId === null
          ? undefined
          : String(eventWorkTypeId),
        eventWorkTypeLevelCode,
      });
      return;
    }
    if (contentType === 'practical' && eventWorkTypeId !== undefined && eventWorkTypeId !== null) {
      setPracticalWorkbench({
        stageId: String(node.data.stageId),
        stageName: node.data.name,
        eventWorkTypeId: String(eventWorkTypeId),
        eventWorkTypeName: selectedEventWorkType?.displayName || selectedEventWorkType?.workTypeName || undefined,
        eventWorkTypeLevelCode,
        eventWorkTypeLevelName: selectedEventWorkTypeLevel?.levelName,
        initialTab: practicalTab,
      });
    }
  }, [draft.nodes, eventWorkTypeId, eventWorkTypeLevelCode, selectedEventWorkType, selectedEventWorkTypeLevel]);
  const onStageRefereeClick = useCallback((nodeId: string) => {
    if (!canViewRefereeAssignment) return;
    const node = draft.nodes.find((item) => item.id === nodeId);
    if (!node || !isBusinessStageNode(node) || node.data.stageId === undefined || node.data.stageId === null) {
      message.warning('请先保存当前赛段编排，再进入裁判配置');
      return;
    }
    if (eventWorkTypeId === undefined || eventWorkTypeId === null) {
      message.warning('当前页面未选择赛事工种，暂时不能进入裁判配置');
      return;
    }
    setPracticalWorkbench({
      stageId: String(node.data.stageId),
      stageName: node.data.name,
      eventWorkTypeId: String(eventWorkTypeId),
      eventWorkTypeName: selectedEventWorkType?.displayName || selectedEventWorkType?.workTypeName || undefined,
      eventWorkTypeLevelCode,
      eventWorkTypeLevelName: selectedEventWorkTypeLevel?.levelName,
      initialTab: 'referee',
    });
  }, [canViewRefereeAssignment, draft.nodes, eventWorkTypeId, eventWorkTypeLevelCode, selectedEventWorkType, selectedEventWorkTypeLevel]);
  const onStageCandidateClick = useCallback((
    nodeId: string,
    contentType: 'theory' | 'practical' = 'theory',
  ) => {
    if (!canViewPlayerAssignment) return;
    const node = draft.nodes.find((item) => item.id === nodeId);
    if (!node || !isBusinessStageNode(node) || node.data.stageId === undefined || node.data.stageId === null) {
      message.warning('请先保存当前赛段编排，再进入选手配置');
      return;
    }
    setPlayerAssignmentStageId(String(node.data.stageId));
    setPlayerAssignmentContentType(contentType);
  }, [canViewPlayerAssignment, draft.nodes]);
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
  }, [eventWorkTypeId, eventWorkTypeLevelCode, promotionScopeQuery.data]);
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
        eventWorkTypeLevelCode,
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
  }, [eventId, eventWorkTypeId, eventWorkTypeLevelCode]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const draftPayload = draft.payload;
      const payload = draftPayload;
      const savedStages = eventWorkTypeId
        ? await saveCompetitionStageDraftForWorkType(
          eventId, eventWorkTypeId, payload, eventWorkTypeLevelCode)
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
          eventWorkTypeLevelCode,
        );
      }
      return { savedStages, savedPromotionConfigs };
    },
    onMutate: () => draft.setSaving(true),
    onSuccess: ({ savedStages, savedPromotionConfigs }) => {
      const notes = draft.currentNotes;
      writeStageCanvasNotes(tenantId, eventId, notes, eventWorkTypeId, eventWorkTypeLevelCode);
      draft.acceptSavedStages(savedStages, notes);
      queryClient.setQueryData([
        'competition-stages',
        eventId,
        'planning',
        eventWorkTypeId ?? 'event',
        eventWorkTypeLevelCode ?? 'event',
      ], {
        items: savedStages,
        page: 1,
        pageSize: 100,
        total: savedStages.length,
      });
      if (savedPromotionConfigs && eventWorkTypeId) {
        setPromotionDraft(savedPromotionConfigs);
        setPromotionDirty(false);
        queryClient.setQueryData(
          [
            'competition-work-type-stages',
            eventId,
            eventWorkTypeId,
            eventWorkTypeLevelCode ?? 'legacy',
          ],
          savedPromotionConfigs,
        );
      }
      queryClient.invalidateQueries({ queryKey: ['competition-stages', eventId] });
      message.success('赛段编排已保存');
    },
    onError: (error) => {
      draft.setSaving(false);
      message.error(errorText(error));
    },
  });

  const confirmMutation = useMutation<CompetitionEvent | CompetitionEventWorkType, Error>({
    mutationFn: () => eventWorkTypeId
      ? confirmCompetitionWorkTypeStagePlan(eventId, eventWorkTypeId, eventWorkTypeLevelCode)
      : confirmCompetitionStagePlan(eventId),
    onSuccess: (confirmedScope) => {
      if (eventWorkTypeId) {
        queryClient.invalidateQueries({ queryKey: ['competition-event-work-types', eventId] });
        queryClient.invalidateQueries({
          queryKey: ['competition-event-work-type-levels', eventId, eventWorkTypeId],
        });
        queryClient.invalidateQueries({ queryKey: ['competition-stages', eventId] });
        message.success(eventWorkTypeLevelCode
          ? '当前工种当前等级赛段编排已确认并锁定'
          : '当前工种赛段编排已确认并锁定');
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
      content: '当前画布未保存的修改将恢复为最近一次保存的状态。',
      onOk: () => {
        draft.discard();
        setPromotionDraft(promotionScopeQuery.data ?? []);
        setPromotionDirty(false);
      },
    });
  };

  const onConfirm = () => {
    if (hasUnsavedChanges) {
      message.warning('请先保存赛段编排，再确认当前范围');
      return;
    }
    const validation = draft.validate();
    if (validation.some((item) => item.level === 'error')) {
      message.error('请先修复编排校验中的阻断问题');
      return;
    }
    const confirmationScope = eventWorkTypeId
      ? (eventWorkTypeLevelCode ? '当前工种当前等级' : '当前工种')
      : '当前赛事';
    Modal.confirm({
      title: '确认并锁定赛段编排？',
      content: `确认后${confirmationScope}的赛段结构、时间和说明将不能再编辑，请确认当前编排已检查无误。`,
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
    || (hasLevelScope && (
      promotionScopeQuery.isLoading
      || eventWorkTypesQuery.isLoading
      || eventWorkTypeLevelsQuery.isLoading
    ))
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
  if (hasLevelScope && canViewPromotionRules && eventWorkTypeLevelsQuery.isError) {
    return <Result status="error" title="工种等级编排状态加载失败" extra={<Button onClick={() => eventWorkTypeLevelsQuery.refetch()}>重新加载</Button>} />;
  }

  const content = (
    <>
      {event.status === 'draft' && <Alert type="info" showIcon message="请先在竞赛计划中填写基础信息并发布赛事。发布后即可激活选手，再继续完成赛段编排与选手配置。" action={<Button onClick={() => onNavigate('/competition/events/' + eventId)}>去发布赛事</Button>} />}
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
        onConfirm={canManageWorkTypeStages ? onConfirm : undefined}
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
          onStageCandidateClick={onStageCandidateClick}
          onStageRefereeClick={onStageRefereeClick}
          candidateEnabled={canViewPlayerAssignment}
          refereeEnabled={canViewRefereeAssignment}
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
          promotionEditable={editable && canManageWorkTypeStages}
          eventWorkTypeId={eventWorkTypeId}
          promotionConfigs={promotionDraft}
          promotionLoading={promotionScopeQuery.isLoading || promotionScopeQuery.isFetching}
          onPromotionPercentageChange={canManageWorkTypeStages ? updatePromotionPercentage : undefined}
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
        title="试卷工作台"
        open={Boolean(theoryWorkbench)}
        onCancel={() => setTheoryWorkbench(null)}
        footer={null}
        centered
        width="min(1440px, calc(100vw - 48px))"
        className="stage-theory-workbench-modal"
        styles={{ body: { maxHeight: 'calc(100vh - 132px)', overflowY: 'auto', padding: 0 } }}
        destroyOnClose
        data-testid="stage-paper-workbench-modal"
      >
        {theoryWorkbench && (
          <CompetitionTheoryExamPage
            eventId={eventId}
            initialStageId={theoryWorkbench.stageId}
            initialEventWorkTypeId={theoryWorkbench.eventWorkTypeId}
            initialEventWorkTypeLevelCode={theoryWorkbench.eventWorkTypeLevelCode}
            onNavigate={onNavigate}
            embedded
            stagePlanningEditor
          />
        )}
      </Modal>

      <Modal
        title="实操配置"
        open={Boolean(practicalWorkbench)}
        onCancel={() => setPracticalWorkbench(null)}
        footer={null}
        centered
        width="min(1440px, calc(100vw - 48px))"
        className="stage-practical-workbench-modal"
        styles={{ body: { maxHeight: 'calc(100vh - 132px)', overflowY: 'auto', padding: 0 } }}
        destroyOnClose
        data-testid="stage-practical-workbench-modal"
      >
        {practicalWorkbench && (
          <CompetitionPracticalConfigPage
            eventId={eventId}
            stageId={practicalWorkbench.stageId}
            stageName={practicalWorkbench.stageName}
            eventWorkTypeId={practicalWorkbench.eventWorkTypeId}
            eventWorkTypeName={practicalWorkbench.eventWorkTypeName}
            eventWorkTypeLevelCode={practicalWorkbench.eventWorkTypeLevelCode}
            eventWorkTypeLevelName={practicalWorkbench.eventWorkTypeLevelName}
            initialTab={practicalWorkbench.initialTab}
          />
        )}
      </Modal>

      <Modal
        title={playerAssignmentContentType === 'practical'
          ? '实操选手配置 · 试卷分配'
          : '选手配置 · 试卷分配'}
        open={Boolean(playerAssignmentStageId)}
        onCancel={() => {
          const close = () => {
            setPlayerAssignmentStageId(null);
            setPlayerAssignmentContentType('theory');
            setPlayerAssignmentDirty(false);
          };
          if (playerAssignmentDirty) Modal.confirm({ title: '放弃未保存的选手分配调整？', onOk: close });
          else close();
        }}
        footer={null}
        centered
        width="min(1500px, calc(100vw - 32px))"
        className="stage-player-paper-assignment-modal"
        styles={{ body: { maxHeight: 'calc(100vh - 112px)', overflowY: 'auto', padding: 0 } }}
        destroyOnClose
        data-testid="stage-player-paper-assignment-modal"
      >
        {playerAssignmentStageId && (
          <CompetitionPlayerPaperAssignmentPage
            stageId={playerAssignmentStageId}
            contentType={playerAssignmentContentType}
            onDirtyChange={setPlayerAssignmentDirty}
          />
        )}
      </Modal>

    </>
  );

  return embedded
    ? content
    : <CompetitionEventFrame event={event} activeSection="overview" onNavigate={onNavigate}>{content}</CompetitionEventFrame>;
}
