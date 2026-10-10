/**
 * 工作台发起区：工种 → 等级 → 细目表批次 三级联动 + 配额 + 提交生成。
 * 批次候选走服务端按 workTypeId+level 过滤（不再全量拉取，细目表超过 200 条也不缺候选）。
 */
import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, InputNumber, message, Select, Space } from 'antd';
import { listBlueprints, listTheoryModelConfigs, submitGenBatch } from '../theoryGenApi';
import type { TheoryBlueprint } from '../theoryGenApi';
import { LEVEL_NAME, QUOTA_DEFAULTS, bpOptionLabel } from './constants';

const LEVEL_OPTIONS = [5, 4, 3, 2, 1].map((v) => ({ value: v, label: `${v}级/${LEVEL_NAME[v]}` }));

interface Props {
  /** 工种下拉选项（页面按同名消歧规则生成后传入） */
  workTypeOptions: Array<{ value: number; label: string }>;
  /** 从"细目表导入"页签行内"生成"按钮带入的批次 */
  preset?: TheoryBlueprint;
  onSubmitted: (batchId: string) => void;
}

const GenStartBar: React.FC<Props> = ({ workTypeOptions, preset, onSubmitted }) => {
  const qc = useQueryClient();
  const [workTypeId, setWorkTypeId] = useState<number | undefined>();
  const [level, setLevel] = useState<number>(5);
  const [blueprintId, setBlueprintId] = useState<string | undefined>();
  const [modelConfigId, setModelConfigId] = useState<string | undefined>();
  const [quotaSingle, setQuotaSingle] = useState<number | null>(null);
  const [quotaJudge, setQuotaJudge] = useState<number | null>(null);
  const [quotaMulti, setQuotaMulti] = useState<number | null>(null);
  const [quotaSub5, setQuotaSub5] = useState<number | null>(null);
  const [quotaSub10, setQuotaSub10] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const bpQuery = useQuery({
    queryKey: ['theorygen-blueprints', 'pick', workTypeId, level],
    queryFn: () => listBlueprints({ workTypeId, level, page: 1, pageSize: 50 }),
    enabled: !!workTypeId && !!level,
  });

  const mcQuery = useQuery({
    queryKey: ['theorygen-model-configs'],
    queryFn: listTheoryModelConfigs,
  });

  // 模型默认选中 theory-gen-glm（未配置时取第一个启用项）
  React.useEffect(() => {
    if (!modelConfigId && mcQuery.data?.length) {
      const preferred = mcQuery.data.find((c) => c.modelCode === 'theory-gen-glm');
      setModelConfigId((preferred ?? mcQuery.data[0]).id);
    }
  }, [mcQuery.data, modelConfigId]);

  // 页签间跳转带入批次：定位到该批次的工种/等级并选中
  React.useEffect(() => {
    if (!preset) {
      return;
    }
    setWorkTypeId(preset.workTypeId);
    setLevel(preset.level);
    setBlueprintId(preset.id);
  }, [preset]);

  const selectedBp = bpQuery.data?.items.find((b) => b.id === blueprintId);
  const multiDisabled = selectedBp ? selectedBp.level === 5 || selectedBp.level === 4 : false;
  // 主观题配额仅二级/一级（指引：二级/一级客观+主观结合，主观分值占比 40%~60%）
  const subjectiveEnabled = selectedBp ? selectedBp.level <= 2 : false;

  // 选中批次变化 → 按等级填入推荐题量
  React.useEffect(() => {
    const d = selectedBp ? QUOTA_DEFAULTS[selectedBp.level] : undefined;
    setQuotaSingle(d?.single ?? null);
    setQuotaJudge(d?.judge ?? null);
    setQuotaMulti(d?.multi ?? null);
    setQuotaSub5(d?.sub5 ?? null);
    setQuotaSub10(d?.sub10 ?? null);
  }, [selectedBp]);

  const submit = async () => {
    if (!blueprintId || !modelConfigId) {
      message.warning('请选择细目表批次与模型');
      return;
    }
    const quota: Record<string, number> = {};
    if (quotaSingle != null) quota.single = quotaSingle;
    if (quotaJudge != null) quota.judge = quotaJudge;
    if (quotaMulti != null) quota.multi = quotaMulti;
    if (subjectiveEnabled) {
      if (quotaSub5 != null) quota.sub5 = quotaSub5;
      if (quotaSub10 != null) quota.sub10 = quotaSub10;
    }
    setSubmitting(true);
    try {
      const b = await submitGenBatch(blueprintId, modelConfigId,
        Object.keys(quota).length ? quota : undefined);
      message.success('批次已提交');
      onSubmitted(b.id);
      qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
      qc.invalidateQueries({ queryKey: ['theorygen-blueprints'] });
    } catch {
      // 错误提示由拦截器统一处理
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="gen-start-bar">
      <Space style={{ marginBottom: 8 }} wrap>
        <Select style={{ width: 220 }} placeholder="1. 选择工种（可搜索）" showSearch
          optionFilterProp="label"
          options={workTypeOptions}
          value={workTypeId}
          onChange={(v) => { setWorkTypeId(v); setBlueprintId(undefined); }} />
        <Select style={{ width: 160 }} placeholder="2. 选择等级" options={LEVEL_OPTIONS}
          value={level}
          onChange={(v) => { setLevel(v); setBlueprintId(undefined); }} />
        <Select style={{ width: 360 }} placeholder="3. 选择细目表批次" showSearch
          optionFilterProp="label"
          loading={bpQuery.isFetching}
          disabled={!workTypeId}
          value={blueprintId}
          onChange={(v) => setBlueprintId(v)}
          options={(bpQuery.data?.items ?? []).map((b) => ({
            value: b.id, label: bpOptionLabel(b) }))} />
        <Select style={{ width: 300 }} placeholder="选择模型配置"
          value={modelConfigId}
          onChange={(v) => setModelConfigId(v)}
          loading={mcQuery.isLoading}
          options={(mcQuery.data ?? []).map((c) => ({
            value: c.id,
            label: `${c.modelName}（${c.modelId}${c.thinking === 'enabled' ? ' · 思考开' : ''}）`,
          }))} />
      </Space>
      <Space style={{ marginBottom: 8 }} wrap>
        <InputNumber min={0} addonBefore="单选" addonAfter="道/点" style={{ width: 170 }}
          value={quotaSingle ?? undefined} onChange={(v) => setQuotaSingle(v)} />
        <InputNumber min={0} addonBefore="判断" addonAfter="道/点" style={{ width: 170 }}
          value={quotaJudge ?? undefined} onChange={(v) => setQuotaJudge(v)} />
        <InputNumber min={0} addonBefore="多选" addonAfter="道/点" style={{ width: 170 }}
          disabled={multiDisabled} value={quotaMulti ?? undefined}
          onChange={(v) => setQuotaMulti(v)} />
        <InputNumber min={1} addonBefore="主观5分制" addonAfter="点/道" style={{ width: 200 }}
          disabled={!subjectiveEnabled} value={quotaSub5 ?? undefined}
          onChange={(v) => setQuotaSub5(v)} />
        <InputNumber min={1} addonBefore="主观10分制" addonAfter="点/道" style={{ width: 200 }}
          disabled={!subjectiveEnabled} value={quotaSub10 ?? undefined}
          onChange={(v) => setQuotaSub10(v)} />
        <Button type="primary" loading={submitting} onClick={submit}>提交生成</Button>
      </Space>
      <div style={{ marginBottom: 8, color: '#888', fontSize: 12 }}>
        「道/点」是每个考评点要生成的题目数量，已按通用规范自动填入推荐值：五/四级 单选4+判断2；
        三~一级 单选3+多选2+判断2。可调大，不建议调低。「主观题 点/道」仅二级、一级需要：
        主观题按"每 N 个考评点 1 道"轮转生成，默认每 3 点 1 道 5 分制题 + 每 3 点 1 道 10 分制题
        （120 点时各 40 道，全库不少于 920 题）。间隔可调小（题更多），调大会低于题量下限而被拒绝。
      </div>
    </div>
  );
};

export default GenStartBar;
