import { useCallback, useEffect, useMemo, useState } from 'react';
import { CollectionWorkbenchPage } from './CollectionWorkbenchPage';
import { CollectionTaskSwitchModal } from '../../components/certification/collection/CollectionTaskSwitchModal';
import {
  readLastCollectionScope,
  switchCollectionTask,
} from '../../services/practicalCollection/tabletCollectionService';
import type { CollectionTaskVO } from '../../types/certificationPracticalCollection';
import './PracticalCollectionApp.css';

function scopeFromLocation(): { taskId: string; examSessionId?: string } | undefined {
  const match = window.location.pathname.match(/^\/practical-collection\/workbench\/([^/]+)/);
  if (!match?.[1]) {
    return undefined;
  }
  const taskId = decodeURIComponent(match[1]).trim();
  if (!taskId) return undefined;
  const examSessionId = new URLSearchParams(window.location.search).get('examSessionId')?.trim() || undefined;
  return { taskId, examSessionId };
}

export function PracticalCollectionApp() {
  const initialScope = useMemo(() => scopeFromLocation() || readLastCollectionScope(), []);
  const [currentScope, setCurrentScope] = useState(initialScope);
  const [taskSelectorVisible, setTaskSelectorVisible] = useState(!initialScope);

  useEffect(() => {
    if (!initialScope) {
      return;
    }
    switchCollectionTask(initialScope.taskId, initialScope.examSessionId);
  }, [initialScope]);

  useEffect(() => {
    const previous = document.body.style.minWidth;
    document.body.style.minWidth = '1024px';
    return () => {
      document.body.style.minWidth = previous;
    };
  }, []);

  const handleTaskChange = useCallback((task: CollectionTaskVO) => {
    const nextScope = {
      taskId: String(task.taskId),
      examSessionId: task.examSessionId ? String(task.examSessionId) : undefined,
    };
    setCurrentScope(nextScope);
    switchCollectionTask(task.taskId, task.examSessionId);
    setTaskSelectorVisible(false);
  }, []);

  if (!currentScope) {
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
      key={`${currentScope.taskId}:${currentScope.examSessionId || 'legacy'}`}
      taskId={currentScope.taskId}
      examSessionId={currentScope.examSessionId}
      onTaskChange={handleTaskChange}
      taskSelectorVisible={taskSelectorVisible}
      onTaskSelectorVisibleChange={setTaskSelectorVisible}
    />
  );
}
