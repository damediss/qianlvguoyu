import { Alert, Button, Card, Descriptions, Input, InputNumber, Select, Space, Spin, Table, Tabs, Tag, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  confirmCompetitionFinalPodium,
  confirmCompetitionPromotionReference,
  getCompetitionFinalPodium,
  getCompetitionPromotionReference,
  getCompetitionStageSettlement,
  listCompetitionEventWorkTypes,
  listCompetitionExecutionPlans,
  listCompetitionStageSettlements,
  saveCompetitionFinalPodium,
  generateCompetitionPromotionReference,
} from '../../../api/competition';
import { useAuthStore } from '../../../stores/authStore';
import type { CompetitionId, CompetitionStage, CompetitionStageFinalPodium } from '../../../types/competition';

export function StageSettlementPromotionPanel({ eventId, stage }: { eventId: CompetitionId; stage: CompetitionStage }) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canView = permissions.includes('competition:settlement:view');
  const canManage = permissions.includes('competition:settlement:manage');
  const canPublish = permissions.includes('competition:settlement:publish');
  const queryClient = useQueryClient();
  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: canView,
  });
  const workTypes = workTypesQuery.data ?? [];
  const [workTypeId, setWorkTypeId] = useState<CompetitionId>();
  const [planId, setPlanId] = useState<CompetitionId>();
  const [settlementId, setSettlementId] = useState<CompetitionId>();

  useEffect(() => {
    if (!workTypeId && workTypes[0]) setWorkTypeId(workTypes[0].id);
  }, [workTypeId, workTypes]);

  const plansQuery = useQuery({
    queryKey: ['competition-stage-execution-plans', stage.id, workTypeId],
    queryFn: () => listCompetitionExecutionPlans(stage.id, workTypeId!),
    enabled: canView && Boolean(workTypeId),
  });
  const plans = plansQuery.data ?? [];
  useEffect(() => {
    const next = plans.find((plan) => plan.status === 'published') ?? plans[0];
    if (next && String(next.id) !== String(planId)) setPlanId(next.id);
    if (!next) setPlanId(undefined);
  }, [planId, plans]);

  const settlementsQuery = useQuery({
    queryKey: ['competition-stage-settlements', planId],
    queryFn: () => listCompetitionStageSettlements(planId!),
    enabled: canView && Boolean(planId),
  });
  const settlements = settlementsQuery.data ?? [];
  useEffect(() => {
    const next = settlements[0];
    if (next && String(next.id) !== String(settlementId)) setSettlementId(next.id);
    if (!next) setSettlementId(undefined);
  }, [settlementId, settlements]);

  const settlementQuery = useQuery({
    queryKey: ['competition-stage-settlement', settlementId],
    queryFn: () => getCompetitionStageSettlement(settlementId!),
    enabled: canView && Boolean(settlementId),
  });
  const settlement = settlementQuery.data;
  const referenceQuery = useQuery({
    queryKey: ['competition-promotion-reference', settlementId],
    queryFn: () => getCompetitionPromotionReference(settlementId!),
    enabled: canView && Boolean(settlementId),
  });
  const podiumQuery = useQuery({
    queryKey: ['competition-final-podium', settlementId],
    queryFn: () => getCompetitionFinalPodium(settlementId!),
    enabled: canView && Boolean(settlementId),
  });
  const [podiumDraft, setPodiumDraft] = useState<CompetitionStageFinalPodium>({} as CompetitionStageFinalPodium);
  useEffect(() => {
    if (podiumQuery.data) setPodiumDraft(podiumQuery.data);
  }, [podiumQuery.data]);

  const refreshSettlement = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['competition-stage-settlements', planId] }),
      queryClient.invalidateQueries({ queryKey: ['competition-stage-settlement', settlementId] }),
      queryClient.invalidateQueries({ queryKey: ['competition-promotion-reference', settlementId] }),
      queryClient.invalidateQueries({ queryKey: ['competition-final-podium', settlementId] }),
    ]);
  };
  const generateReferenceMutation = useMutation({
    mutationFn: () => generateCompetitionPromotionReference(settlementId!),
    onSuccess: refreshSettlement,
    onError: (error) => message.error(error instanceof Error ? error.message : '参考名单生成失败'),
  });
  const confirmReferenceMutation = useMutation({
    mutationFn: () => confirmCompetitionPromotionReference(settlementId!),
    onSuccess: refreshSettlement,
    onError: (error) => message.error(error instanceof Error ? error.message : '参考名单确认失败'),
  });
  const savePodiumMutation = useMutation({
    mutationFn: () => saveCompetitionFinalPodium(settlementId!, {
      firstContestantMemberId: podiumDraft.firstContestantMemberId ?? null,
      secondContestantMemberId: podiumDraft.secondContestantMemberId ?? null,
      thirdContestantMemberId: podiumDraft.thirdContestantMemberId ?? null,
      specialExplanation: podiumDraft.specialExplanation ?? null,
    }),
    onSuccess: refreshSettlement,
    onError: (error) => message.error(error instanceof Error ? error.message : '前三名保存失败'),
  });
  const confirmPodiumMutation = useMutation({
    mutationFn: () => confirmCompetitionFinalPodium(settlementId!),
    onSuccess: refreshSettlement,
    onError: (error) => message.error(error instanceof Error ? error.message : '前三名确认失败'),
  });

  const reference = referenceQuery.data;
  const podiumEditable = canManage && settlement?.status === 'draft' && (!podiumDraft.status || podiumDraft.status === 'draft');
  const referenceRows = useMemo(() => reference?.items ?? [], [reference]);

  if (!canView || workTypesQuery.isError) return null;
  if (workTypesQuery.isLoading) return <Card size="small" style={{ marginTop: 16 }}><Spin /></Card>;

  return (
    <Card size="small" title="晋级确认工作台" style={{ marginTop: 16 }}>
      <Space wrap style={{ marginBottom: 12 }}>
        <Select
          aria-label="结算工种"
          value={workTypeId}
          placeholder="选择工种"
          style={{ width: 220 }}
          options={workTypes.map((item) => ({ value: item.id, label: item.displayName || item.workTypeName || String(item.id) }))}
          onChange={setWorkTypeId}
        />
        <Select
          aria-label="执行方案"
          value={planId}
          placeholder="选择执行方案"
          style={{ width: 220 }}
          loading={plansQuery.isFetching}
          options={plans.map((item) => ({ value: item.id, label: `方案 V${item.planVersion} · ${item.status}` }))}
          onChange={setPlanId}
        />
        <Select
          aria-label="结算快照"
          value={settlementId}
          placeholder="选择结算快照"
          style={{ width: 220 }}
          loading={settlementsQuery.isFetching}
          options={settlements.map((item) => ({ value: item.id, label: `结算 V${item.settlementVersion} · ${item.status}` }))}
          onChange={setSettlementId}
        />
      </Space>
      {!settlement ? (
        <Alert type="info" message="当前工种和执行方案暂无结算快照" />
      ) : (
        <Tabs items={[
          {
            key: 'reference',
            label: '参考名单确认',
            children: (
              <Space direction="vertical" style={{ width: '100%' }}>
                <Descriptions size="small" column={3} bordered>
                  <Descriptions.Item label="规则">{settlement.promotionRuleType || 'LEGACY'}</Descriptions.Item>
                  <Descriptions.Item label="晋级百分比">{settlement.promotionPercentage == null ? '-' : `${settlement.promotionPercentage}%`}</Descriptions.Item>
                  <Descriptions.Item label="计算目标人数">{reference?.calculatedTargetCount ?? settlement.calculatedTargetCount ?? '-'}</Descriptions.Item>
                </Descriptions>
                {!reference ? (
                  <Button type="primary" disabled={!canManage || settlement.status !== 'draft'} loading={generateReferenceMutation.isPending} onClick={() => generateReferenceMutation.mutate()}>生成参考名单</Button>
                ) : (
                  <>
                    <Space>
                      <Tag color={reference.status === 'confirmed' ? 'success' : 'processing'}>{reference.status}</Tag>
                      <span>基数 {reference.baseCount}，目标人数 {reference.calculatedTargetCount}</span>
                      <Button disabled={!canPublish || reference.status !== 'draft'} loading={confirmReferenceMutation.isPending} onClick={() => confirmReferenceMutation.mutate()}>确认并锁定</Button>
                    </Space>
                    <Table
                      size="small"
                      rowKey="id"
                      pagination={{ pageSize: 8 }}
                      dataSource={referenceRows}
                      columns={[
                        { title: '选手', dataIndex: 'contestantMemberId' },
                        { title: '来源排名', dataIndex: 'sourceRank' },
                        { title: '选择状态', dataIndex: 'selectionStatus', render: (value: string) => <Tag color={value === 'selected' ? 'success' : 'default'}>{value}</Tag> },
                        { title: '计算说明', dataIndex: 'reason' },
                      ]}
                    />
                  </>
                )}
              </Space>
            ),
          },
          {
            key: 'podium',
            label: '决赛前三名确认',
            children: (
              <Space direction="vertical" style={{ width: '100%' }}>
                <Alert type="info" message="仅最终赛段使用；有效选手不足 3 人时必须填写特殊说明。" />
                <Space wrap>
                  {[['第一名', 'firstContestantMemberId'], ['第二名', 'secondContestantMemberId'], ['第三名', 'thirdContestantMemberId']].map(([label, key]) => (
                    <InputNumber
                      key={key}
                      aria-label={label}
                      placeholder={label}
                      min={1}
                      disabled={!podiumEditable}
                      value={podiumDraft[key as keyof CompetitionStageFinalPodium] as number | undefined}
                      onChange={(value) => setPodiumDraft((current) => ({ ...current, [key]: value ?? null }))}
                    />
                  ))}
                </Space>
                <Input.TextArea
                  aria-label="前三名特殊说明"
                  placeholder="有效选手不足 3 人时填写特殊说明"
                  disabled={!podiumEditable}
                  value={podiumDraft.specialExplanation ?? ''}
                  onChange={(event) => setPodiumDraft((current) => ({ ...current, specialExplanation: event.target.value }))}
                  rows={3}
                />
                <Space>
                  <Button type="primary" disabled={!podiumEditable} loading={savePodiumMutation.isPending} onClick={() => savePodiumMutation.mutate()}>保存前三名</Button>
                  <Button disabled={!canPublish || !podiumDraft.id || podiumDraft.status !== 'draft'} loading={confirmPodiumMutation.isPending} onClick={() => confirmPodiumMutation.mutate()}>确认并锁定</Button>
                  {podiumDraft.status && <Tag color={podiumDraft.status === 'confirmed' ? 'success' : 'processing'}>{podiumDraft.status}</Tag>}
                </Space>
              </Space>
            ),
          },
        ]} />
      )}
    </Card>
  );
}
