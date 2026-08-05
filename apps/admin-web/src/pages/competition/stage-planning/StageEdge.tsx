import { BaseEdge, EdgeLabelRenderer, getBezierPath } from '@xyflow/react';
import type { EdgeProps } from '@xyflow/react';
import type { StageEdge as StageEdgeModel } from '../../../types/competitionStagePlanning';

export function StageEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps<StageEdgeModel>) {
  const [path, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition });
  return (
    <>
      <BaseEdge id={id} path={path} markerEnd="url(#stage-edge-arrow)" className={`${data?.business ? 'stage-edge' : 'stage-edge stage-edge--marker'}${selected ? ' is-selected' : ''}`} data-testid={`stage-edge-${id.replace(/^edge:/, '')}`} />
      {data?.business && data.note && (
        <EdgeLabelRenderer>
          <div
            className={`stage-edge-label${selected ? ' is-selected' : ''}`}
            style={{ transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)` }}
            data-testid={`stage-edge-note-${id.replace(/^edge:/, '')}`}
          >
            {data.note}
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  );
}
