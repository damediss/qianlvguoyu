import { Alert, Card, InputNumber, Typography } from 'antd';
import type { CompetitionId, CompetitionWorkTypeStageConfig } from '../../../types/competition';
import type { StageType } from '../../../types/competitionStagePlanning';

export function StagePromotionRulePanel({
  stage,
  config,
  editable,
  workTypeSelected,
  loading = false,
  onPercentageChange,
}: {
  stage: { id: CompetitionId; name: string; stageType: StageType };
  config?: CompetitionWorkTypeStageConfig;
  editable: boolean;
  workTypeSelected: boolean;
  loading?: boolean;
  onPercentageChange?: (percentage: number | null) => void;
}) {
  if (!workTypeSelected) {
    return (
      <Card size="small" title="晋级规则" data-testid="stage-promotion-rule-panel">
        <Alert type="info" showIcon message="请先在页面上方选择赛事工种" />
      </Card>
    );
  }

  if (stage.stageType === 'final') {
    return (
      <Card size="small" title="晋级规则" data-testid="stage-promotion-rule-panel">
        <Alert
          type="info"
          showIcon
          message="决赛不配置晋级百分比"
          description="决赛结算后，由赛事管理员确认唯一第1、2、3名；有效选手不足3人时需填写特殊说明。"
        />
      </Card>
    );
  }

  const existingPercentage = config?.promotionRuleType === 'PERCENTAGE'
    ? config.promotionPercentage ?? undefined
    : undefined;
  const isLegacyRule = config?.promotionRuleType && config.promotionRuleType !== 'PERCENTAGE';

  return (
    <Card size="small" title="晋级规则" loading={loading} data-testid="stage-promotion-rule-panel">
      <Typography.Text type="secondary">{stage.name}按百分比晋级</Typography.Text>
      <InputNumber
        aria-label={`${stage.name}晋级百分比`}
        min={1}
        max={100}
        precision={0}
        addonAfter="%"
        value={existingPercentage}
        disabled={!editable || loading}
        onChange={(value) => onPercentageChange?.(value ?? null)}
        style={{ width: '100%', marginTop: 8 }}
      />
      <Typography.Paragraph type="secondary" style={{ margin: '8px 0 0' }}>
        预计晋级人数将在参考名单生成时，按有效选手基数向上取整；确认后锁定基数和目标人数。
      </Typography.Paragraph>
      {isLegacyRule && (
        <Alert
          type="warning"
          showIcon
          message="当前为历史固定人数规则"
          description="填写百分比并保存草稿后，将以新的百分比规则保存；历史已开始或已有成绩的赛事不会自动切换。"
        />
      )}
    </Card>
  );
}
