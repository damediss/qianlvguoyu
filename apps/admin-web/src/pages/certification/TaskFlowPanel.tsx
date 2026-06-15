import { Card, Empty, Steps, Tag } from 'antd';
import type { CertificationPhase } from '../../types/certification';

interface TaskFlowPanelProps {
  phases: CertificationPhase[];
  onPhaseClick: (phase: CertificationPhase) => void;
}

export function TaskFlowPanel({ phases, onPhaseClick }: TaskFlowPanelProps) {
  return (
    <Card title="任务流程" styles={{ body: { paddingTop: 12 } }}>
      {phases.length === 0 ? (
        <Empty description="暂无流程数据" />
      ) : (
        <TaskFlowSteps phases={phases} onPhaseClick={onPhaseClick} />
      )}
    </Card>
  );
}

function TaskFlowSteps({ phases, onPhaseClick }: TaskFlowPanelProps) {
  return (
    <Steps
      current={-1}
      onChange={(index) => {
        const phase = phases[index];
        if (phase && isClickableStatus(phase.status)) {
          onPhaseClick(phase);
        }
      }}
      items={phases.map((phase) => ({
        title: phase.phaseName,
        status: phase.status === 'completed' ? 'finish' : phase.status === 'running' ? 'process' : 'wait',
        disabled: !isClickableStatus(phase.status),
        description: renderPhaseStatus(phase.status),
      }))}
    />
  );
}

function isClickableStatus(status?: string) {
  return status === 'completed' || status === 'running';
}

function renderPhaseStatus(status?: string) {
  const labelMap: Record<string, string> = {
    pending: '待开始',
    running: '进行中',
    completed: '已完成',
  };
  const color = status === 'completed' ? 'success' : status === 'running' ? 'processing' : 'default';
  return <Tag color={color}>{status ? labelMap[status] || status : '-'}</Tag>;
}
