import {
  CheckCircleOutlined,
} from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Descriptions, Empty, List, Modal, Progress, Space, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  completePreparation,
  getPreparationCompleteCheck,
  getPreparationTaskOverview,
} from '../../../api/certificationPreparation';
import { PermissionGate } from '../../../directives/permission';
import type { PreparationCompleteCheck, PreparationTaskOverview } from '../../../types/certificationPreparation';
import { dateText, dateTimeText, statusTag } from './shared';

interface PreparationWorkbenchProps {
  taskId?: string;
  task?: PreparationTaskOverview;
  onChanged?: () => void;
}

export function PreparationWorkbench({ taskId, task, onChanged }: PreparationWorkbenchProps) {
  const queryClient = useQueryClient();
  const overviewQuery = useQuery({
    queryKey: ['certification-preparation-overview', taskId],
    queryFn: () => getPreparationTaskOverview(taskId!),
    enabled: Boolean(taskId),
  });
  const overview = overviewQuery.data || task;

  const checkMutation = useMutation({
    mutationFn: () => getPreparationCompleteCheck(taskId!),
    onSuccess: (result) => showCheckResult(result, false),
  });

  const completeMutation = useMutation({
    mutationFn: () => completePreparation(taskId!),
    onSuccess: (result) => {
      showCheckResult(result, true);
      queryClient.invalidateQueries({ queryKey: ['certification-preparation-tasks'] });
      queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
      queryClient.invalidateQueries({ queryKey: ['certification-tasks'] });
      onChanged?.();
    },
  });

  if (!taskId || !overview) {
    return <Empty description="请选择认定任务后查看筹备工作台" />;
  }

  // 是否已经离开“认定筹备”阶段：进入归档或认定完成阶段
  const preparationCompleted = ['archive', 'certification_completed'].includes(overview.currentPhase || '');
  // 只有任务当前阶段=preparation 时才允许触发“完成筹备”
  const canCompletePreparation = overview.currentPhase === 'preparation';
  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <ProCard
        title="筹备概览"
        extra={
          <Space>
            <Button loading={checkMutation.isPending} onClick={() => checkMutation.mutate()}>
              筹备完成检查
            </Button>
            <PermissionGate code="certification.preparation.complete">
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                loading={completeMutation.isPending}
                disabled={preparationCompleted || !canCompletePreparation}
                onClick={() => completeMutation.mutate()}
              >
                {preparationCompleted ? '已完成筹备' : '完成筹备'}
              </Button>
            </PermissionGate>
          </Space>
        }
      >
        <Descriptions bordered size="small" column={4}>
          <Descriptions.Item label="任务编号">{overview.taskNo}</Descriptions.Item>
          <Descriptions.Item label="任务名称" span={2}>{overview.taskName}</Descriptions.Item>
          <Descriptions.Item label="任务阶段">{statusTag(overview.currentPhase)}</Descriptions.Item>
          <Descriptions.Item label="任务状态">{statusTag(overview.status)}</Descriptions.Item>
          <Descriptions.Item label="批次">{overview.batchName || '-'}</Descriptions.Item>
          <Descriptions.Item label="工种">{overview.workTypeName || '-'}</Descriptions.Item>
          <Descriptions.Item label="等级">{overview.levelName || '-'}</Descriptions.Item>
          <Descriptions.Item label="材料状态">{statusTag(overview.materialStatus)}</Descriptions.Item>
          <Descriptions.Item label="计划开始">{dateText(overview.planStartDate)}</Descriptions.Item>
          <Descriptions.Item label="计划结束">{dateText(overview.planEndDate)}</Descriptions.Item>
          <Descriptions.Item label="最后更新" span={2}>{dateTimeText(overview.lastUpdatedAt)}</Descriptions.Item>
        </Descriptions>
        <div style={{ marginTop: 16 }}>
          <Typography.Text type="secondary">资料完成率</Typography.Text>
          <Progress percent={overview.documentCompletionRate || 0} />
        </div>
      </ProCard>

    </Space>
  );
}

function showCheckResult(result: PreparationCompleteCheck, completionAttempt: boolean) {
  const title = result.passed
    ? completionAttempt ? '筹备已完成' : '筹备检查通过'
    : completionAttempt ? '筹备尚未满足完成条件' : '筹备检查未通过';
  if (result.passed && completionAttempt) {
    message.success('认定筹备已完成，任务可进入后续实施阶段');
  }
  Modal.info({
    title,
    width: 680,
    content: (
      <List
        size="small"
        dataSource={result.items || []}
        renderItem={(item) => (
          <List.Item>
            <Space>
              {item.passed ? <CheckCircleOutlined style={{ color: '#16a34a' }} /> : <CheckCircleOutlined style={{ color: '#dc2626' }} />}
              <Typography.Text strong>{item.name}</Typography.Text>
              <Typography.Text type={item.passed ? 'secondary' : 'danger'}>{item.message}</Typography.Text>
            </Space>
          </List.Item>
        )}
      />
    ),
  });
}
