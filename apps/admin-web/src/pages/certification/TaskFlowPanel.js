import { jsx as _jsx } from "react/jsx-runtime";
import { Card, Empty, Steps, Tag } from 'antd';
export function TaskFlowPanel({ phases, onPhaseClick }) {
    return (_jsx(Card, { title: "\u4EFB\u52A1\u6D41\u7A0B", styles: { body: { paddingTop: 12 } }, children: phases.length === 0 ? (_jsx(Empty, { description: "\u6682\u65E0\u6D41\u7A0B\u6570\u636E" })) : (_jsx(TaskFlowSteps, { phases: phases, onPhaseClick: onPhaseClick })) }));
}
function TaskFlowSteps({ phases, onPhaseClick }) {
    return (_jsx(Steps, { current: -1, onChange: (index) => {
            const phase = phases[index];
            if (phase && isClickableStatus(phase.status)) {
                onPhaseClick(phase);
            }
        }, items: phases.map((phase) => ({
            title: phase.phaseName,
            status: resolveStepsStatus(phase.status),
            disabled: !isClickableStatus(phase.status),
            description: renderPhaseStatus(phase.status),
        })) }));
}
function isClickableStatus(status) {
    return status === 'completed' || status === 'running' || status === 'pending_complete';
}
function resolveStepsStatus(status) {
    if (status === 'completed') {
        return 'finish';
    }
    if (status === 'running' || status === 'pending_complete') {
        return 'process';
    }
    return 'wait';
}
function renderPhaseStatus(status) {
    const labelMap = {
        pending: '待开始',
        running: '进行中',
        pending_complete: '待补充完整',
        completed: '已完成',
    };
    const color = status === 'completed'
        ? 'success'
        : status === 'pending_complete'
            ? 'warning'
            : status === 'running'
                ? 'processing'
                : 'default';
    return _jsx(Tag, { color: color, children: status ? labelMap[status] || status : '-' });
}
