import { BookOutlined, ExperimentOutlined, SettingOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Checkbox, Divider, InputNumber, Progress, Space, Tag, Typography } from 'antd';
import * as React from 'react';
import type {
  CompetitionStage,
  CompetitionStageContent,
  CompetitionStageContentType,
} from '../../../types/competition';
import type { StageContentConfig } from '../../../types/competitionStagePlanning';
import './stageContentPanel.css';

export interface StageWeights {
  theory: number;
  simulation: number;
  practical: number;
}

export type { StageContentConfig } from '../../../types/competitionStagePlanning';

export type StageContentValue = Pick<CompetitionStageContent, 'contentType' | 'status' | 'weight'> | StageContentConfig;

export interface StageContentOption {
  code: CompetitionStageContentType;
  label: string;
  icon: React.ReactNode;
  color: string;
  providerHint?: string;
}

export const STAGE_CONTENT_OPTIONS: StageContentOption[] = [
  {
    code: 'theory',
    label: '理论考试',
    icon: <BookOutlined />,
    color: '#1677ff',
  },
  {
    code: 'simulation',
    label: '仿真操作',
    icon: <ExperimentOutlined />,
    color: '#722ed1',
    providerHint: '成绩在成绩与排名中按身份证号导入',
  },
  {
    code: 'practical',
    label: '实操考评',
    icon: <SettingOutlined />,
    color: '#fa8c16',
  },
];

export function stageContentTypeLabel(code: string) {
  return STAGE_CONTENT_OPTIONS.find((item) => item.code === code)?.label || code;
}

function isEnabledContent(content: StageContentValue) {
  return !('status' in content) || content.status === 'enabled';
}

export function selectedContentTypes(contents: StageContentValue[]) {
  return contents
    .filter(isEnabledContent)
    .map((item) => item.contentType)
    .filter((code, index, values) => values.indexOf(code) === index);
}

export function weightsFromContents(contents: StageContentValue[]): StageWeights {
  return contents.reduce<StageWeights>((weights, content) => {
    if (isEnabledContent(content)) {
      weights[content.contentType] = Number(content.weight || 0);
    }
    return weights;
  }, { theory: 0, simulation: 0, practical: 0 });
}

export function weightTotal(weights: StageWeights) {
  return Number((weights.theory + weights.simulation + weights.practical).toFixed(2));
}

export function stageContentConfigsAreValid(contents: StageContentConfig[]) {
  const selected = contents.map((item) => item.contentType);
  const weights = contents.reduce<StageWeights>((result, item) => {
    result[item.contentType] = item.weight;
    return result;
  }, { theory: 0, simulation: 0, practical: 0 });
  if (selected.length === 0) return false;
  return selected.every((code) => weights[code] > 0)
    && STAGE_CONTENT_OPTIONS.every((option) => selected.includes(option.code) || weights[option.code] === 0)
    && weightTotal(weights) === 100;
}

export interface StageContentPanelProps {
  stage: Pick<CompetitionStage, 'id' | 'name'>;
  contents: StageContentValue[];
  contentEditable: boolean;
  contentSaving: boolean;
  onSave?: (contents: StageContentConfig[]) => void;
  onChange?: (contents: StageContentConfig[]) => void;
  compact?: boolean;
  showActions?: boolean;
}

export function StageContentPanel({
  stage,
  contents,
  contentEditable,
  contentSaving,
  onSave,
  onChange,
  compact = false,
  showActions = true,
}: StageContentPanelProps) {
  const [selected, setSelected] = React.useState<CompetitionStageContentType[]>(() => selectedContentTypes(contents));
  const [weights, setWeights] = React.useState<StageWeights>(() => weightsFromContents(contents));
  const [dirty, setDirty] = React.useState(false);

  const total = weightTotal(weights);
  const currentConfigs = () => selected.map((contentType) => ({ contentType, weight: weights[contentType] }));
  const validWeights = stageContentConfigsAreValid(currentConfigs());

  React.useEffect(() => {
    setSelected(selectedContentTypes(contents));
    setWeights(weightsFromContents(contents));
    setDirty(false);
  }, [contents]);

  const changeContent = (code: CompetitionStageContentType, checked: boolean) => {
    const nextSelected = selected.includes(code) === checked
      ? selected
      : checked
        ? [...selected, code]
        : selected.filter((item) => item !== code);
    const nextWeights = { ...weights, [code]: checked ? weights[code] : 0 };
    setSelected(nextSelected);
    setWeights(nextWeights);
    onChange?.(nextSelected.map((contentType) => ({ contentType, weight: nextWeights[contentType] })));
    setDirty(true);
  };

  const changeWeight = (code: CompetitionStageContentType, value: number | null) => {
    const nextWeights = { ...weights, [code]: value ?? 0 };
    setWeights(nextWeights);
    onChange?.(selected.map((contentType) => ({ contentType, weight: nextWeights[contentType] })));
    setDirty(true);
  };

  const resetFromServer = () => {
    setSelected(selectedContentTypes(contents));
    setWeights(weightsFromContents(contents));
    setDirty(false);
  };

  return (
    <div className={`stage-content-panel${compact ? ' stage-content-panel--compact' : ''}`} data-testid="stage-content-panel">
      <div className="stage-content-panel__header">
        <div>
          <Typography.Title level={5} style={{ margin: 0 }}>赛段内容</Typography.Title>
        </div>
        <Space wrap>
          {!contentEditable && <Tag>当前赛段只读</Tag>}
        </Space>
      </div>

      <div className="stage-content-panel__options">
        {STAGE_CONTENT_OPTIONS.map((option) => {
          const checked = selected.includes(option.code);
          return (
            <Card
              key={option.code}
              size="small"
              hoverable={contentEditable}
              className={`stage-content-option${checked ? ' stage-content-option--selected' : ''}`}
              style={checked ? { borderColor: option.color, boxShadow: `0 0 0 2px ${option.color}18` } : undefined}
              onClick={() => contentEditable && changeContent(option.code, !checked)}
            >
              <div className="stage-content-option__topline">
                <Checkbox
                  checked={checked}
                  disabled={!contentEditable}
                  onChange={(event) => changeContent(option.code, event.target.checked)}
                  onClick={(event) => event.stopPropagation()}
                />
                <span className="stage-content-option__icon" style={{ color: option.color }}>{option.icon}</span>
                <Typography.Text strong>{option.label}</Typography.Text>
                {checked && <Tag color="success">已启用</Tag>}
              </div>
              {option.providerHint && <div className="stage-content-option__hint">{option.providerHint}</div>}
            </Card>
          );
        })}
      </div>

      {selected.includes('simulation') && (
        <Alert
          type="info"
          showIcon
          message="仿真成绩通过 Excel 导入"
          description="勾选仿真并保存权重后可以确认赛段编排。仿真分数在成绩与排名中按身份证号导入，再按内容权重计入综合成绩。"
        />
      )}

      {(selected.includes('practical') || selected.includes('simulation')) && validWeights && (
        <Alert
          type="info"
          showIcon
          message="实操与仿真成绩来源"
          description={`保存权重后，在成绩与排名中按身份证号导入 ${[
            selected.includes('practical') ? '实操' : '',
            selected.includes('simulation') ? '仿真' : '',
          ].filter(Boolean).join('、')} xlsx。系统按身份证匹配选手分数，再按这里的内容权重计算综合成绩与排名。理论成绩仍来自正式考试。`}
        />
      )}

      <Divider />

      <div className="stage-content-panel__weight-head">
        <div>
          <Typography.Title level={5} style={{ margin: 0 }}>内容权重占比</Typography.Title>
        </div>
        <Space size={16} align="center">
          <Progress
            type="circle"
            percent={Math.min(total, 100)}
            size={46}
            status={total === 100 ? 'success' : total > 100 ? 'exception' : 'normal'}
            format={() => `${total}%`}
          />
          <div>
            <Typography.Text type={total === 100 ? 'success' : 'danger'} strong>
              权重合计 {total}%
            </Typography.Text>
            <div><Typography.Text type="secondary">{validWeights ? '配置有效' : '待完善配置'}</Typography.Text></div>
          </div>
        </Space>
      </div>

      {selected.length === 0 && (
        <Alert type="warning" showIcon message="请至少启用一项比赛内容。" />
      )}

      <div className="stage-content-weight-list">
        {STAGE_CONTENT_OPTIONS.map((option) => {
          const enabled = selected.includes(option.code);
          return (
            <div key={option.code} className={`stage-content-weight-row${enabled ? '' : ' stage-content-weight-row--disabled'}`}>
              <div className="stage-content-weight-row__label">
                <span className="stage-content-option__icon" style={{ color: option.color }}>{option.icon}</span>
                <Typography.Text strong={enabled}>{option.label}</Typography.Text>
                {!enabled && <Typography.Text type="secondary">未启用</Typography.Text>}
              </div>
              <InputNumber
                min={0}
                max={100}
                precision={2}
                step={5}
                value={weights[option.code]}
                disabled={!enabled || !contentEditable}
                onChange={(value) => changeWeight(option.code, value)}
                addonAfter="%"
                style={{ width: 150 }}
                data-testid={`stage-content-weight-${option.code}`}
              />
            </div>
          );
        })}
      </div>

      {selected.length > 0 && total !== 100 && (
        <Alert type="error" showIcon message={`当前权重合计为 ${total}%，还差 ${total > 100 ? total - 100 : 100 - total}%。`} />
      )}

      {showActions && (
        <div className="stage-content-panel__actions">
          <Space>
            {contentEditable && onSave && (
              <Button
                type="primary"
                loading={contentSaving}
                disabled={!dirty || selected.length === 0 || !validWeights}
                onClick={() => onSave(currentConfigs())}
                data-testid="stage-content-save"
              >
                保存赛段配置
              </Button>
            )}
            {dirty && <Button onClick={resetFromServer}>撤销修改</Button>}
          </Space>
        </div>
      )}
    </div>
  );
}
