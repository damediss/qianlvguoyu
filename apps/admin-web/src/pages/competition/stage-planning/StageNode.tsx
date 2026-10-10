import { Handle, Position } from '@xyflow/react';
import { Tag } from 'antd';
import type { NodeProps } from '@xyflow/react';
import { stageStatusLabel, stageTypeDefinition, stageTypeDisplayOrder, type StageNode as StageNodeModel } from '../../../types/competitionStagePlanning';

type StageNodeProps = NodeProps<StageNodeModel> & {
  stagePlanConfirmed?: boolean;
};

export function StageNode({
  id,
  data,
  selected,
  stagePlanConfirmed = false,
}: StageNodeProps) {
  if (data.kind === 'marker') {
    return (
      <div className="stage-marker-node" data-testid={`stage-marker-${data.marker}`}>
        <span className="stage-marker-node__dot" />
        <span>{data.label}</span>
      </div>
    );
  }
  const definition = stageTypeDefinition(data.stageType);
  const displayOrder = stageTypeDisplayOrder(data.stageType);
  return (
    <div
      className={`stage-canvas-node${selected ? ' is-selected' : ''}`}
      data-testid={`stage-node-${data.stageType}`}
      data-stage-id={data.stageId ? String(data.stageId) : undefined}
    >
      <Handle id={`${id}-target`} type="target" position={Position.Left} data-testid={`stage-handle-${data.stageType}-target`} />
      <div className="stage-canvas-node__header">
        <span className="stage-canvas-node__index">{displayOrder ?? data.sortOrder}</span>
        <strong>{definition?.name || data.name}</strong>
        <Tag>{stageStatusLabel(data.status, stagePlanConfirmed)}</Tag>
      </div>
      <div className="stage-canvas-node__body">
        <span>{data.startAt && data.endAt ? `${data.startAt.replace('T', ' ').slice(0, 16)} — ${data.endAt.replace('T', ' ').slice(0, 16)}` : '时间待补充'}</span>
        <span className="stage-canvas-node__description">{data.description || '暂无说明'}</span>
      </div>
      <Handle id={`${id}-source`} type="source" position={Position.Right} data-testid={`stage-handle-${data.stageType}-source`} />
    </div>
  );
}
