import { jsx as _jsx } from "react/jsx-runtime";
import { useCallback, useEffect, useMemo, useState } from 'react';
import { CollectionWorkbenchPage } from './CollectionWorkbenchPage';
import { CollectionTaskSwitchModal } from '../../components/certification/collection/CollectionTaskSwitchModal';
import { readLastCollectionScope, switchCollectionTask, } from '../../services/practicalCollection/tabletCollectionService';
import './PracticalCollectionApp.css';
function scopeFromLocation() {
    const match = window.location.pathname.match(/^\/practical-collection\/workbench\/([^/]+)/);
    if (!match?.[1]) {
        return undefined;
    }
    const taskId = decodeURIComponent(match[1]).trim();
    if (!taskId)
        return undefined;
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
    const handleTaskChange = useCallback((task) => {
        const nextScope = {
            taskId: String(task.taskId),
            examSessionId: task.examSessionId ? String(task.examSessionId) : undefined,
        };
        setCurrentScope(nextScope);
        switchCollectionTask(task.taskId, task.examSessionId);
        setTaskSelectorVisible(false);
    }, []);
    if (!currentScope) {
        return (_jsx("div", { className: "practical-collection-shell practical-collection-landing", children: _jsx(CollectionTaskSwitchModal, { open: taskSelectorVisible, forceSelect: true, onSelect: handleTaskChange }) }));
    }
    return (_jsx(CollectionWorkbenchPage, { taskId: currentScope.taskId, examSessionId: currentScope.examSessionId, onTaskChange: handleTaskChange, taskSelectorVisible: taskSelectorVisible, onTaskSelectorVisibleChange: setTaskSelectorVisible }, `${currentScope.taskId}:${currentScope.examSessionId || 'legacy'}`));
}
