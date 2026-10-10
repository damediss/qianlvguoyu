import { ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Radio,
  Row,
  Select,
  Space,
  Switch,
  Tag,
  Typography,
  message,
} from 'antd';
import type { InputNumberProps } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import {
  getCompetitionPracticalExecutionConfig,
  listCompetitionPracticalScoringSchemes,
  saveCompetitionPracticalExecutionConfig,
  validateCompetitionPracticalExecutionConfig,
} from '../../api/competitionPracticalExecutionConfig';
import type { CompetitionId } from '../../types/competition';
import type {
  CompetitionPracticalExecutionConfig,
  CompetitionPracticalExecutionConfigRequest,
  CompetitionPracticalScoringScheme,
} from '../../types/competitionPracticalExecutionConfig';

export type PracticalExecutionConfigFormValues = {
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId: CompetitionId;
  scoringSchemeId?: CompetitionId;
  examName: string;
  durationMinutes?: number;
  startMode: 'UNIFIED_START' | 'MANUAL_START';
  allowEarlySubmit: boolean;
  timeScoringEnabled: boolean;
  overtimeRuleType: 'NONE' | 'STEP_DEDUCTION' | 'FIXED_DEDUCTION';
  overtimeStepMinutes?: number;
  overtimeScorePerStep?: number;
  overtimeFixedDeduction?: number;
  overtimeMaxDeduction?: number;
  earlyFinishRuleType: 'NONE' | 'STEP_BONUS' | 'FIXED_BONUS';
  earlyFinishStepMinutes?: number;
  earlyFinishScorePerStep?: number;
  earlyFinishFixedBonus?: number;
  earlyFinishMaxBonus?: number;
  overtimeAction: 'ALLOW_CONTINUE' | 'AUTO_FINISH';
  maxDurationMinutes?: number;
  finalScoreUpperLimit?: number;
};

export interface CompetitionPracticalExecutionConfigPanelProps {
  eventId: CompetitionId;
  stageId: CompetitionId;
  stageName?: string;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeLevelId?: CompetitionId;
  editable: boolean;
}

const defaults = (eventWorkTypeId: CompetitionId, levelId?: CompetitionId): PracticalExecutionConfigFormValues => ({
  eventWorkTypeId,
  eventWorkTypeLevelId: levelId ?? '',
  examName: '实操考试',
  durationMinutes: 90,
  startMode: 'UNIFIED_START',
  allowEarlySubmit: false,
  timeScoringEnabled: false,
  overtimeRuleType: 'NONE',
  earlyFinishRuleType: 'NONE',
  overtimeAction: 'ALLOW_CONTINUE',
});

function fromConfig(config: CompetitionPracticalExecutionConfig): PracticalExecutionConfigFormValues {
  return {
    eventWorkTypeId: config.eventWorkTypeId,
    eventWorkTypeLevelId: config.eventWorkTypeLevelId,
    scoringSchemeId: config.scoringSchemeId,
    examName: config.examName,
    durationMinutes: config.durationSeconds / 60,
    startMode: config.startMode,
    allowEarlySubmit: config.allowEarlySubmit,
    timeScoringEnabled: config.timeScoringEnabled,
    overtimeRuleType: config.overtimeRuleType,
    overtimeStepMinutes: config.overtimeStepSeconds == null ? undefined : config.overtimeStepSeconds / 60,
    overtimeScorePerStep: config.overtimeScorePerStep ?? undefined,
    overtimeFixedDeduction: config.overtimeFixedDeduction ?? undefined,
    overtimeMaxDeduction: config.overtimeMaxDeduction ?? undefined,
    earlyFinishRuleType: config.earlyFinishRuleType,
    earlyFinishStepMinutes: config.earlyFinishStepSeconds == null ? undefined : config.earlyFinishStepSeconds / 60,
    earlyFinishScorePerStep: config.earlyFinishScorePerStep ?? undefined,
    earlyFinishFixedBonus: config.earlyFinishFixedBonus ?? undefined,
    earlyFinishMaxBonus: config.earlyFinishMaxBonus ?? undefined,
    overtimeAction: config.overtimeAction,
    maxDurationMinutes: config.maxDurationSeconds == null ? undefined : config.maxDurationSeconds / 60,
    finalScoreUpperLimit: config.finalScoreUpperLimit,
  };
}

export function buildPracticalRulePreview(
  values: Partial<PracticalExecutionConfigFormValues>,
  scheme?: CompetitionPracticalScoringScheme,
) {
  const duration = values.durationMinutes ?? 0;
  const lines = [`本场实操${values.examName ? `“${values.examName}”` : ''}标准考试时间为${duration}分钟。`];
  if (!values.timeScoringEnabled) {
    lines.push('实际用时只记录，不参与最终成绩计算。');
  } else {
    if (values.overtimeRuleType === 'STEP_DEDUCTION') {
      lines.push(`超过标准时间后，每超${values.overtimeStepMinutes ?? 0}分钟扣${values.overtimeScorePerStep ?? 0}分，最大扣${values.overtimeMaxDeduction ?? 0}分；不足一步按整步向上取整。`);
    } else if (values.overtimeRuleType === 'FIXED_DEDUCTION') {
      lines.push(`超过标准时间后固定扣${values.overtimeFixedDeduction ?? 0}分。`);
    } else {
      lines.push('不进行超时扣分。');
    }
    if (values.earlyFinishRuleType === 'STEP_BONUS') {
      lines.push(`每提前${values.earlyFinishStepMinutes ?? 0}分钟完成加${values.earlyFinishScorePerStep ?? 0}分，最大奖励${values.earlyFinishMaxBonus ?? 0}分；只按完整步长计算。`);
    } else if (values.earlyFinishRuleType === 'FIXED_BONUS') {
      lines.push(`提前完成固定奖励${values.earlyFinishFixedBonus ?? 0}分。`);
    } else {
      lines.push('不进行提前完成奖励。');
    }
  }
  if (values.overtimeAction === 'AUTO_FINISH') {
    lines.push(`达到${values.maxDurationMinutes ?? 0}分钟后由服务端自动结束考试。`);
  } else {
    lines.push('超过标准时间后允许继续考试。');
  }
  const upper = values.finalScoreUpperLimit ?? scheme?.totalScore ?? 0;
  lines.push(`最终成绩范围为0至${upper}分；服务端以开始、结束时间和暂停时长计算。`);
  return lines;
}

function toRequest(values: PracticalExecutionConfigFormValues): CompetitionPracticalExecutionConfigRequest {
  const seconds = (minutes?: number) => minutes == null ? null : Math.round(minutes * 60);
  return {
    eventWorkTypeId: values.eventWorkTypeId,
    eventWorkTypeLevelId: values.eventWorkTypeLevelId,
    scoringSchemeId: values.scoringSchemeId!,
    examName: values.examName.trim(),
    durationSeconds: seconds(values.durationMinutes)!,
    startMode: values.startMode,
    allowEarlySubmit: values.allowEarlySubmit,
    timeScoringEnabled: values.timeScoringEnabled,
    overtimeRuleType: values.overtimeRuleType,
    overtimeStepSeconds: seconds(values.overtimeStepMinutes),
    overtimeScorePerStep: values.overtimeScorePerStep ?? null,
    overtimeFixedDeduction: values.overtimeFixedDeduction ?? null,
    overtimeMaxDeduction: values.overtimeMaxDeduction ?? null,
    earlyFinishRuleType: values.earlyFinishRuleType,
    earlyFinishStepSeconds: seconds(values.earlyFinishStepMinutes),
    earlyFinishScorePerStep: values.earlyFinishScorePerStep ?? null,
    earlyFinishFixedBonus: values.earlyFinishFixedBonus ?? null,
    earlyFinishMaxBonus: values.earlyFinishMaxBonus ?? null,
    overtimeAction: values.overtimeAction,
    maxDurationSeconds: seconds(values.maxDurationMinutes),
    finalScoreUpperLimit: values.finalScoreUpperLimit!,
  };
}

function scoreInput(min = 0) {
  return <InputNumber min={min} precision={2} style={{ width: 140 }} />;
}

function MinutesInput({ min = 1, ...inputProps }: InputNumberProps) {
  return (
    <Space.Compact style={{ width: 180 }}>
      <InputNumber
        {...inputProps}
        min={min}
        precision={0}
        style={{ ...inputProps.style, width: 'calc(100% - 60px)' }}
      />
      <Input value="分钟" readOnly tabIndex={-1} style={{ width: 60, textAlign: 'center' }} />
    </Space.Compact>
  );
}

function minutesInput(min = 1) {
  return <MinutesInput min={min} />;
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请刷新后重试';
}

export function CompetitionPracticalExecutionConfigPanel({
  eventId,
  stageId,
  stageName,
  eventWorkTypeId,
  eventWorkTypeLevelId,
  editable,
}: CompetitionPracticalExecutionConfigPanelProps) {
  const [form] = Form.useForm<PracticalExecutionConfigFormValues>();
  const queryClient = useQueryClient();
  const [previewValues, setPreviewValues] = useState<Partial<PracticalExecutionConfigFormValues>>(
    defaults(eventWorkTypeId, eventWorkTypeLevelId),
  );
  const configQuery = useQuery({
    queryKey: ['competition-practical-execution-config', stageId],
    queryFn: () => getCompetitionPracticalExecutionConfig(stageId),
    enabled: Boolean(stageId && eventWorkTypeLevelId),
  });
  const schemesQuery = useQuery({
    queryKey: ['competition-practical-scoring-schemes', eventId, eventWorkTypeId, eventWorkTypeLevelId],
    queryFn: () => listCompetitionPracticalScoringSchemes({
      eventId, eventWorkTypeId, eventWorkTypeLevelId: eventWorkTypeLevelId!,
    }),
    enabled: Boolean(eventId && eventWorkTypeId && eventWorkTypeLevelId),
  });
  const schemes = schemesQuery.data ?? [];
  const currentScheme = useMemo(
    () => schemes.find((item) => String(item.id) === String(previewValues.scoringSchemeId)),
    [previewValues.scoringSchemeId, schemes],
  );
  const configuredSchemeUnavailable = Boolean(configQuery.data?.scoringSchemeId
    && !schemes.some(item => String(item.id) === String(configQuery.data?.scoringSchemeId)));
  const schemeOptions = schemes.map(scheme => ({
    value: scheme.id,
    label: `${scheme.title} ${scheme.paperCode}卷 v${scheme.versionNo} · ${scheme.totalScore}分 · 来源赛事${scheme.sourceEventId}`,
    disabled: false,
  }));
  if (configuredSchemeUnavailable && configQuery.data?.scoringScheme) {
    const legacyScheme = configQuery.data.scoringScheme;
    schemeOptions.push({
      value: legacyScheme.id,
      label: `${legacyScheme.title} ${legacyScheme.paperCode}卷 v${legacyScheme.versionNo}（旧配置，需改选）`,
      disabled: true,
    });
  }

  useEffect(() => {
    if (configQuery.isLoading) return;
    const initial = configQuery.data ? fromConfig(configQuery.data) : defaults(eventWorkTypeId, eventWorkTypeLevelId);
    form.setFieldsValue(initial);
    setPreviewValues(initial);
  }, [configQuery.data, configQuery.isLoading, eventWorkTypeId, eventWorkTypeLevelId, form]);

  const saveMutation = useMutation({
    mutationFn: (values: PracticalExecutionConfigFormValues) => saveCompetitionPracticalExecutionConfig(stageId, toRequest(values)),
    onSuccess: async (config) => {
      message.success('赛段实操运行规则已保存');
      queryClient.setQueryData(['competition-practical-execution-config', stageId], config);
      await queryClient.invalidateQueries({ queryKey: ['competition-practical-execution-config', stageId] });
    },
    onError: (error) => message.error(errorMessage(error)),
  });
  const validateMutation = useMutation({
    mutationFn: (request: CompetitionPracticalExecutionConfigRequest) =>
      validateCompetitionPracticalExecutionConfig(stageId, request),
    onSuccess: (result) => {
      if (result.valid) message.success('配置校验通过');
      else message.warning(result.message || result.errors.join('；'));
    },
    onError: (error) => message.error(errorMessage(error)),
  });

  const timeScoringEnabled = Form.useWatch('timeScoringEnabled', form) ?? false;
  const overtimeRuleType = Form.useWatch('overtimeRuleType', form) ?? 'NONE';
  const earlyFinishRuleType = Form.useWatch('earlyFinishRuleType', form) ?? 'NONE';
  const overtimeAction = Form.useWatch('overtimeAction', form) ?? 'ALLOW_CONTINUE';

  const onValuesChange = (changed: Partial<PracticalExecutionConfigFormValues>, values: PracticalExecutionConfigFormValues) => {
    if (Object.prototype.hasOwnProperty.call(changed, 'scoringSchemeId')) {
      const scheme = schemes.find((item) => String(item.id) === String(values.scoringSchemeId));
      if (scheme) {
        form.setFieldValue('finalScoreUpperLimit', scheme.totalScore);
        values.finalScoreUpperLimit = scheme.totalScore;
      }
    }
    setPreviewValues({ ...values });
  };

  const onValidate = async () => {
    try {
    const values = await form.validateFields();
      validateMutation.mutate(toRequest(values));
    } catch (error) {
      if (error instanceof Error) message.error(error.message);
    }
  };

  const refresh = async () => {
    const [configResult] = await Promise.all([configQuery.refetch(), schemesQuery.refetch()]);
    if (configResult.isError) return;
    const initial = configResult.data
      ? fromConfig(configResult.data)
      : defaults(eventWorkTypeId, eventWorkTypeLevelId);
    form.resetFields();
    form.setFieldsValue(initial);
    setPreviewValues(initial);
  };

  if (configQuery.isError || schemesQuery.isError) {
    return <Alert type="error" showIcon message="实操运行规则加载失败" description={errorMessage(configQuery.error ?? schemesQuery.error)}
      action={<Button onClick={() => void refresh()}>重新加载</Button>} />;
  }

  return (
    <div data-testid="competition-practical-execution-config-panel">
      {configuredSchemeUnavailable && <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 12 }}
        message="当前赛段绑定的是旧实操组卷快照"
        description="旧执行快照继续有效；如需修改本赛段运行规则，请先在实操评分方案中发布新方案，再在此重新绑定。"
      />}
      <Space style={{ marginBottom: 12 }} wrap>
        <Tag color="orange">赛段：{stageName || String(stageId)}</Tag>
        <Tag>单位：分钟（服务端保存为秒）</Tag>
        <Button data-testid="runtime-config-refresh" icon={<ReloadOutlined />} onClick={() => void refresh()}>
          刷新
        </Button>
      </Space>
      <Form<PracticalExecutionConfigFormValues>
        form={form}
        layout="vertical"
        disabled={!editable || configQuery.isFetching || schemesQuery.isFetching}
        initialValues={defaults(eventWorkTypeId, eventWorkTypeLevelId)}
        onValuesChange={onValuesChange}
        onFinish={(values) => saveMutation.mutate(values)}
      >
        <Row gutter={[16, 16]}>
          <Col xs={24} xl={12}>
            <Card title="实操基础配置" size="small" className="competition-filter-card">
              <Form.Item name="examName" label="实操考试名称" rules={[{ required: true, whitespace: true, message: '请输入实操考试名称' }]}>
                <Input maxLength={128} />
              </Form.Item>
              <Form.Item name="scoringSchemeId" label="绑定实操评分方案" rules={[{ required: true, message: '请选择已发布的实操评分方案' }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  loading={schemesQuery.isLoading}
                  placeholder="选择已发布的实操评分方案"
                  options={schemeOptions}
                />
              </Form.Item>
              <Form.Item label="评分方案标准总分">
                <Typography.Text strong data-testid="practical-standard-total-score">
                  {currentScheme?.totalScore ?? configQuery.data?.standardTotalScore ?? '选择评分方案后自动读取'} 分
                </Typography.Text>
                <Typography.Text type="secondary" style={{ marginLeft: 8 }}>由已发布评分方案确定</Typography.Text>
              </Form.Item>
              <Row gutter={12}>
                <Col span={12}>
                  <Form.Item name="durationMinutes" label="标准考核时长（分钟）" rules={[{ required: true, message: '请输入标准考核时长' }]}>
                    {minutesInput()}
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="startMode" label="开始方式" rules={[{ required: true }]}>
                    <Select options={[
                      { value: 'UNIFIED_START', label: '统一开始' },
                      { value: 'MANUAL_START', label: '单独开始 / 裁判开始' },
                    ]} />
                  </Form.Item>
                </Col>
              </Row>
              <Form.Item name="allowEarlySubmit" label="允许提前交卷" valuePropName="checked">
                <Switch checkedChildren="允许" unCheckedChildren="不允许" />
              </Form.Item>
            </Card>
          </Col>
          <Col xs={24} xl={12}>
            <Card title="时间与成绩规则" size="small" className="competition-filter-card">
              <Form.Item name="timeScoringEnabled" label="时间参与成绩" valuePropName="checked">
                <Switch checkedChildren="参与" unCheckedChildren="只记录" />
              </Form.Item>
              {!timeScoringEnabled && (
                <Alert type="info" showIcon message="实际用时只记录，不增加或扣减成绩。" />
              )}
              {timeScoringEnabled && (
                <>
                  <Typography.Text strong>超时扣分</Typography.Text>
                  <Row gutter={12} style={{ marginTop: 8 }}>
                    <Col span={24}>
                      <Form.Item name="overtimeRuleType" label="超时规则">
                        <Select options={[
                          { value: 'NONE', label: '不扣分' },
                          { value: 'STEP_DEDUCTION', label: '按时间步长扣分' },
                          { value: 'FIXED_DEDUCTION', label: '超时固定扣分' },
                        ]} />
                      </Form.Item>
                    </Col>
                    {overtimeRuleType === 'STEP_DEDUCTION' && <>
                      <Col span={8}><Form.Item name="overtimeStepMinutes" label="超时步长" rules={[{ required: true, type: 'number', min: 1 }]}>{minutesInput()}</Form.Item></Col>
                      <Col span={8}><Form.Item name="overtimeScorePerStep" label="每步扣分" rules={[{ required: true, type: 'number', min: 0 }]}>{scoreInput()}</Form.Item></Col>
                      <Col span={8}><Form.Item name="overtimeMaxDeduction" label="最大扣分" rules={[{ required: true, type: 'number', min: 0 }]}>{scoreInput()}</Form.Item></Col>
                    </>}
                    {overtimeRuleType === 'FIXED_DEDUCTION' && <Col span={12}>
                      <Form.Item name="overtimeFixedDeduction" label="超时固定扣分" rules={[{ required: true, type: 'number', min: 0 }]}>{scoreInput()}</Form.Item>
                    </Col>}
                  </Row>
                  <Typography.Text strong>提前完成奖励</Typography.Text>
                  <Row gutter={12} style={{ marginTop: 8 }}>
                    <Col span={24}>
                      <Form.Item name="earlyFinishRuleType" label="奖励规则">
                        <Select options={[
                          { value: 'NONE', label: '不奖励' },
                          { value: 'STEP_BONUS', label: '按完整步长奖励' },
                          { value: 'FIXED_BONUS', label: '提前完成固定奖励' },
                        ]} />
                      </Form.Item>
                    </Col>
                    {earlyFinishRuleType === 'STEP_BONUS' && <>
                      <Col span={8}><Form.Item name="earlyFinishStepMinutes" label="提前步长" rules={[{ required: true, type: 'number', min: 1 }]}>{minutesInput()}</Form.Item></Col>
                      <Col span={8}><Form.Item name="earlyFinishScorePerStep" label="每步加分" rules={[{ required: true, type: 'number', min: 0 }]}>{scoreInput()}</Form.Item></Col>
                      <Col span={8}><Form.Item name="earlyFinishMaxBonus" label="最大奖励" rules={[{ required: true, type: 'number', min: 0 }]}>{scoreInput()}</Form.Item></Col>
                    </>}
                    {earlyFinishRuleType === 'FIXED_BONUS' && <Col span={12}>
                      <Form.Item name="earlyFinishFixedBonus" label="提前完成固定奖励" rules={[{ required: true, type: 'number', min: 0 }]}>{scoreInput()}</Form.Item>
                    </Col>}
                  </Row>
                </>
              )}
              <Form.Item name="finalScoreUpperLimit" label="最终成绩上限" rules={[{ required: true, type: 'number', min: 0.01 }]}>
                {scoreInput(0.01)}
              </Form.Item>
            </Card>
          </Col>
          <Col span={24}>
            <Card title="超时处理" size="small" className="competition-filter-card">
              <Form.Item name="overtimeAction">
                <Radio.Group options={[
                  { value: 'ALLOW_CONTINUE', label: '超时后允许继续考试' },
                  { value: 'AUTO_FINISH', label: '达到最大允许时间后自动结束' },
                ]} />
              </Form.Item>
              {overtimeAction === 'AUTO_FINISH' && (
                <Form.Item name="maxDurationMinutes" label="最大允许作答时间（分钟）" rules={[{ required: true, type: 'number', min: 1 }]}>
                  {minutesInput()}
                </Form.Item>
              )}
              <Alert
                type="warning"
                showIcon
                message="标准考核时长用于计算时间成绩；最大允许时间只决定考试停止时点。"
              />
            </Card>
          </Col>
          <Col span={24}>
            <Card title="当前规则预览" size="small" className="competition-filter-card">
              <Space direction="vertical" size={4} data-testid="practical-rule-preview">
                {buildPracticalRulePreview(previewValues, currentScheme).map((line) => (
                  <Typography.Text key={line}>{line}</Typography.Text>
                ))}
              </Space>
              <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
                <SafetyCertificateOutlined /> 计分使用服务端记录的开始时间、结束时间和暂停时长；Pad 只显示倒计时。
              </Typography.Paragraph>
            </Card>
          </Col>
        </Row>
        {!eventWorkTypeLevelId && <Alert style={{ marginTop: 16 }} type="warning" showIcon message="当前工种等级尚未解析，暂时不能保存运行规则。" />}
        <Space style={{ marginTop: 16 }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={saveMutation.isPending}
            disabled={!editable || !eventWorkTypeLevelId || configQuery.isLoading || schemesQuery.isLoading}
            data-testid="practical-runtime-save"
          >保存运行规则</Button>
          <Button
            onClick={() => void onValidate()}
            loading={validateMutation.isPending}
            disabled={!editable || !eventWorkTypeLevelId}
            data-testid="practical-runtime-validate"
          >校验配置</Button>
        </Space>
      </Form>
    </div>
  );
}
