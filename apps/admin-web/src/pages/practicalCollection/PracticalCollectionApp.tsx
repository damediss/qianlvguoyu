import { useCallback, useEffect, useMemo, useState } from 'react';
import { CollectionWorkbenchPage } from './CollectionWorkbenchPage';
import { CollectionTaskSwitchModal } from '../../components/certification/collection/CollectionTaskSwitchModal';
import { switchCollectionTask } from '../../services/practicalCollection/tabletCollectionService';
import type { SnowflakeId } from '../../types/certificationPracticalCollection';
import './PracticalCollectionApp.css';

function taskIdFromPath(): string | undefined {
  const match = window.location.pathname.match(/^\/practical-collection\/workbench\/([^/]+)/);
  if (!match?.[1]) {
    return undefined;
  }
  return decodeURIComponent(match[1]).trim() || undefined;
}

export function PracticalCollectionApp() {
  const initialTaskId = useMemo(() => taskIdFromPath(), []);
  const [currentTaskId, setCurrentTaskId] = useState<string | undefined>(initialTaskId);
  const [taskSelectorVisible, setTaskSelectorVisible] = useState(!initialTaskId);

  useEffect(() => {
    const previous = document.body.style.minWidth;
    document.body.style.minWidth = '1024px';
    return () => {
      document.body.style.minWidth = previous;
    };
  }, []);

  const handleTaskChange = useCallback((taskId: SnowflakeId) => {
    const nextTaskId = String(taskId);
    setCurrentTaskId(nextTaskId);
    switchCollectionTask(taskId);
    setTaskSelectorVisible(false);
  }, []);

  if (!currentTaskId) {
    return (
      <div className="practical-collection-shell practical-collection-landing">
        <CollectionTaskSwitchModal
          open={taskSelectorVisible}
          forceSelect
          onSelect={handleTaskChange}
        />
      </div>
    );
  }

  return (
    <CollectionWorkbenchPage
      key={currentTaskId}
      taskId={currentTaskId}
      onTaskChange={handleTaskChange}
      taskSelectorVisible={taskSelectorVisible}
      onTaskSelectorVisibleChange={setTaskSelectorVisible}
    />
  );
}
