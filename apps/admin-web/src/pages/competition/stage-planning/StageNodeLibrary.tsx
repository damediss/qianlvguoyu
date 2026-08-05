import { Typography } from 'antd';
import { STAGE_DEFINITIONS, stageTypeDisplayOrder, type StageType } from '../../../types/competitionStagePlanning';
import './stagePlanning.css';

interface StageNodeLibraryProps {
  usedTypes: StageType[];
  editable: boolean;
}

export function StageNodeLibrary({ usedTypes, editable }: StageNodeLibraryProps) {
  return (
    <aside className="stage-planning-library" data-testid="stage-node-library">
      <Typography.Title level={5}>节点库</Typography.Title>
      <Typography.Paragraph type="secondary" className="stage-planning-library__hint">
        每种预设赛段同一赛事最多配置一次。
      </Typography.Paragraph>
      <div className="stage-planning-library__items">
        {STAGE_DEFINITIONS.map((definition) => {
          const used = usedTypes.includes(definition.stageType);
          const disabled = !editable || used;
          const onDragStart = (event: React.DragEvent<HTMLDivElement>) => {
            if (disabled) {
              event.preventDefault();
              return;
            }
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData('application/x-competition-stage-type', definition.stageType);
          };
          return (
            <div
              key={definition.stageType}
              className={`stage-library-item${disabled ? ' is-disabled' : ''}`}
              draggable={!disabled}
              onDragStart={onDragStart}
              data-testid={`stage-library-${definition.stageType}`}
              aria-disabled={disabled}
            >
              <span className="stage-library-item__order">{stageTypeDisplayOrder(definition.stageType)}</span>
              <span>
                <strong>{definition.name}</strong>
                <small>{used ? '已添加' : '拖入画布'}</small>
              </span>
            </div>
          );
        })}
      </div>
      {!editable && <Typography.Text type="warning">当前赛事只读，节点库不可编辑。</Typography.Text>}
    </aside>
  );
}
