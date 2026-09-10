import { Alert, DatePicker, Divider, Empty, Form, Input, Spin, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import type { CompetitionId, CompetitionStageContent, CompetitionWorkTypeStageConfig } from '../../../types/competition';
import {
  isBusinessStageNode,
  type StageBusinessNodeData,
  type StageContentConfig,
  type StageEdge,
  type StageNode,
} from '../../../types/competitionStagePlanning';
import { StageContentPanel } from './StageContentPanel';
import { StagePromotionRulePanel } from './StagePromotionRulePanel';
import './stagePlanning.css';

export function isDateOccupiedByOtherStage(current: Dayjs, currentNodeId: string, nodes: StageNode[]) {
  if (!current.isValid()) return false;
  const currentDate = current.startOf('day');

  return nodes.some((candidate) => {
    if (candidate.id === currentNodeId || !isBusinessStageNode(candidate)) return false;
    const dates = [candidate.data.startAt, candidate.data.endAt]
      .filter((value): value is string => Boolean(value))
      .map((value) => dayjs(value));
    if (dates.length === 0 || dates.some((value) => !value.isValid())) return false;

    const start = dates.reduce((earliest, value) => value.isBefore(earliest) ? value : earliest);
    const end = dates.reduce((latest, value) => value.isAfter(latest) ? value : latest);
    return !currentDate.isBefore(start.startOf('day')) && !currentDate.isAfter(end.endOf('day'));
  });
}

function validDateTime(value?: string | null) {
  if (!value) return undefined;
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed : undefined;
}

function laterDateTime(first?: Dayjs, second?: Dayjs) {
  if (!first) return second;
  if (!second) return first;
  return first.isAfter(second) ? first : second;
}

function earlierDateTime(first?: Dayjs, second?: Dayjs) {
  if (!first) return second;
  if (!second) return first;
  return first.isBefore(second) ? first : second;
}

function disabledTimeForRange(current: Dayjs, lowerBound?: Dayjs, upperBound?: Dayjs) {
  const lowerOnCurrentDay = lowerBound?.isSame(current, 'day') ? lowerBound : undefined;
  const upperOnCurrentDay = upperBound?.isSame(current, 'day') ? upperBound : undefined;
  const hours = Array.from({ length: 24 }, (_, hour) => hour);
  const minutes = Array.from({ length: 60 }, (_, minute) => minute);
  const seconds = Array.from({ length: 60 }, (_, second) => second);

  return {
    disabledHours: () => hours.filter((hour) => (
      (lowerOnCurrentDay && hour < lowerOnCurrentDay.hour())
      || (upperOnCurrentDay && hour > upperOnCurrentDay.hour())
    )),
    disabledMinutes: (selectedHour: number) => minutes.filter((minute) => (
      (lowerOnCurrentDay
        && selectedHour === lowerOnCurrentDay.hour()
        && minute < lowerOnCurrentDay.minute())
      || (upperOnCurrentDay
        && selectedHour === upperOnCurrentDay.hour()
        && minute > upperOnCurrentDay.minute())
    )),
    disabledSeconds: (selectedHour: number, selectedMinute: number) => seconds.filter((second) => (
      (lowerOnCurrentDay
        && selectedHour === lowerOnCurrentDay.hour()
        && selectedMinute === lowerOnCurrentDay.minute()
        && second < lowerOnCurrentDay.second())
      || (upperOnCurrentDay
        && selectedHour === upperOnCurrentDay.hour()
        && selectedMinute === upperOnCurrentDay.minute()
        && second > upperOnCurrentDay.second())
    )),
  };
}

type StageInspectorContent = CompetitionStageContent | StageContentConfig;

interface StageInspectorProps {
  node?: StageNode;
  edge?: StageEdge;
  nodes: StageNode[];
  eventStartAt?: string | null;
  eventEndAt?: string | null;
  stagePlanConfirmed?: boolean;
  editable: boolean;
  contentVisible?: boolean;
  contentEditable?: boolean;
  contentLoading?: boolean;
  contents?: StageInspectorContent[];
  promotionVisible?: boolean;
  promotionEditable?: boolean;
  eventWorkTypeId?: CompetitionId;
  promotionConfigs?: CompetitionWorkTypeStageConfig[];
  promotionLoading?: boolean;
  onPromotionPercentageChange?: (
    stageType: StageBusinessNodeData['stageType'],
    stageId: CompetitionId,
    stageName: string,
    percentage: number | null,
  ) => void;
  onNodeChange: (nodeId: string, patch: Partial<StageBusinessNodeData>) => void;
  onEdgeChange: (edgeId: string, patch: { note?: string }) => void;
  onContentChange?: (nodeId: string, contents: StageContentConfig[]) => void;
  onOpenWorkbench?: (stageId: CompetitionId) => void;
}

export function StageInspector({
  node,
  edge,
  nodes,
  eventStartAt,
  eventEndAt,
  stagePlanConfirmed = false,
  editable,
  contentVisible = true,
  contentEditable = editable,
  contentLoading = false,
  contents = [],
  promotionVisible = false,
  promotionEditable = editable,
  eventWorkTypeId,
  promotionConfigs = [],
  promotionLoading = false,
  onPromotionPercentageChange,
  onNodeChange,
  onEdgeChange,
  onContentChange,
}: StageInspectorProps) {
  if (node && isBusinessStageNode(node)) {
    const startValue = validDateTime(node.data.startAt);
    const endValue = validDateTime(node.data.endAt);
    const eventStartValue = validDateTime(eventStartAt);
    const eventEndValue = validDateTime(eventEndAt);
    const rangeValue = startValue || endValue ? [startValue || null, endValue || null] as [Dayjs | null, Dayjs | null] : null;
    const contentValues = node.data.contents ?? contents;
    const stageId = node.data.stageId ?? node.id;
    const stageContentEditable = contentEditable
      && (node.data.status === 'draft' || node.data.status === 'configuring');
    const isDateDisabled = (current: Dayjs, from?: Dayjs) => {
      if (isDateOccupiedByOtherStage(current, node.id, nodes)) return true;
      if (eventStartValue && current.isBefore(eventStartValue, 'day')) return true;
      if (eventEndValue && current.isAfter(eventEndValue, 'day')) return true;
      if (from && current.isBefore(from, 'day')) return true;
      if (!from && endValue?.isValid() && current.isAfter(endValue, 'day')) return true;
      return false;
    };

    return (
      <aside className="stage-planning-inspector" data-testid="stage-inspector">
        <Typography.Title level={4} className="stage-planning-inspector__stage-title">
          {node.data.name}
        </Typography.Title>
        <Form layout="vertical">
          <Form.Item label="时间范围">
            <DatePicker.RangePicker
              showTime
              value={rangeValue}
              disabled={!editable}
              placeholder={['开始时间', '结束时间']}
              format="YYYY-MM-DD HH:mm:ss"
              disabledDate={(current, info) => isDateDisabled(current, info.from)}
              disabledTime={(current, range, info) => {
                const lowerBound = laterDateTime(
                  eventStartValue,
                  range === 'end' ? (info.from || startValue) : undefined,
                );
                const upperBound = earlierDateTime(
                  eventEndValue,
                  range === 'start' ? endValue : undefined,
                );
                return disabledTimeForRange(current, lowerBound, upperBound);
              }}
              style={{ width: '100%' }}
              onChange={(values) => onNodeChange(node.id, {
                startAt: values?.[0]?.format('YYYY-MM-DDTHH:mm:ss') || null,
                endAt: values?.[1]?.format('YYYY-MM-DDTHH:mm:ss') || null,
              })}
              data-testid="stage-inspector-time-range"
            />
          </Form.Item>
        </Form>

        {promotionVisible && (
          <StagePromotionRulePanel
            stage={{ id: stageId, name: node.data.name, stageType: node.data.stageType }}
            config={promotionConfigs.find((item) => item.stageType === node.data.stageType)}
            editable={promotionEditable}
            workTypeSelected={Boolean(eventWorkTypeId)}
            loading={promotionLoading}
            onPercentageChange={(percentage) => onPromotionPercentageChange?.(
              node.data.stageType,
              stageId,
              node.data.name,
              percentage,
            )}
          />
        )}

        {contentVisible && (
          <>
            <Divider className="stage-planning-inspector__divider" />
            {contentLoading && <Spin size="small" className="stage-planning-inspector__content-loading" />}
            <StageContentPanel
              stage={{ id: stageId, name: node.data.name }}
              contents={contentValues}
              contentEditable={stageContentEditable}
              contentSaving={false}
              compact
              showActions={false}
              onChange={(nextContents) => onContentChange?.(node.id, nextContents)}
            />
            {node.data.contents !== undefined && (
              <Alert
                type="info"
                showIcon
                message="内容与权重已加入当前赛段编排草稿"
                description="点击页面顶部“保存赛段编排”后统一生效。"
              />
            )}
          </>
        )}
        <Form layout="vertical">
          <Form.Item label="赛段说明">
            <Input.TextArea
              value={node.data.description || ''}
              disabled={!editable}
              rows={3}
              maxLength={2000}
              showCount
              onChange={(event) => onNodeChange(node.id, { description: event.target.value })}
              data-testid="stage-inspector-description"
            />
          </Form.Item>
        </Form>
      </aside>
    );
  }
  if (edge?.data?.business) {
    return (
      <aside className="stage-planning-inspector" data-testid="stage-inspector">
        <Typography.Title level={5}>流转说明</Typography.Title>
        <Typography.Paragraph type="secondary">
          该说明只保存在当前浏览器，不会跨浏览器或设备共享。
        </Typography.Paragraph>
        <Input.TextArea
          rows={6}
          maxLength={500}
          showCount
          value={edge.data.note || ''}
          disabled={!editable}
          onChange={(event) => onEdgeChange(edge.id, { note: event.target.value })}
          data-testid="stage-edge-note-editor"
        />
      </aside>
    );
  }
  return (
    <aside className="stage-planning-inspector stage-planning-inspector--empty" data-testid="stage-inspector-empty">
      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="选择节点或连线查看属性" />
    </aside>
  );
}
