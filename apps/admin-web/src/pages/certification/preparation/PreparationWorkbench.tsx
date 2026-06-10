import {
  CheckCircleOutlined,
  DatabaseOutlined,
  FileProtectOutlined,
  ReadOutlined,
  ScheduleOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Descriptions, Empty, List, Modal, Progress, Space, Statistic, Typography, message } from 'antd';
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
  onOpenSection: (key: string) => void;
  onChanged?: () => void;
}

export function PreparationWorkbench({ taskId, task, onOpenSection, onChanged }: PreparationWorkbenchProps) {
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
  const quickEntryHeight = 72;
  const quickActions = [
    { key: 'collection', icon: <DatabaseOutlined />, title: '信息收集', description: '学员导入 · 申报材料一键生成' },
    { key: 'staff', icon: <TeamOutlined />, title: '考务配置', description: '考务人员绑定与备注' },
    { key: 'equipment', icon: <DatabaseOutlined />, title: '配置设备', description: '考试设备绑定与数量' },
    { key: 'sites', icon: <ScheduleOutlined />, title: '考场配置', description: '考场信息与容量配置' },
    { key: 'assessors', icon: <TeamOutlined />, title: '考评员配置', description: '考评员分配与任务发放' },
    { key: 'checklist', icon: <CheckCircleOutlined />, title: '清单核查', description: '材料勾选与确认人' },
  ];

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={16}>
      <div className="knowledge-overview">
        <ProCard className="overview-card">
          <Statistic title="学员数量" value={overview.candidateCount || 0} prefix={<ReadOutlined />} />
        </ProCard>
        <ProCard className="overview-card">
          <Statistic title="资料完成率" value={overview.documentCompletionRate || 0} suffix="%" prefix={<FileProtectOutlined />} />
        </ProCard>
        <ProCard className="overview-card">
          <Statistic title="已配置考场" value={overview.examSiteCount || 0} prefix={<ScheduleOutlined />} />
        </ProCard>
        <ProCard className="overview-card">
          <Statistic title="已配置考务" value={overview.examStaffCount || 0} prefix={<TeamOutlined />} />
        </ProCard>
        <ProCard className="overview-card">
          <Statistic title="已配置考评员" value={overview.assessorCount || 0} prefix={<TeamOutlined />} />
        </ProCard>
      </div>

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

      <ProCard title="快捷入口">
        <List
          grid={{ gutter: 12, xs: 1, sm: 2, md: 3, lg: 4 }}
          dataSource={quickActions}
          renderItem={(item) => (
            <List.Item style={{ height: '100%' }}>
              <Button
                block
                icon={item.icon}
                onClick={() => onOpenSection(item.key)}
                style={{
                  height: quickEntryHeight,
                  textAlign: 'left',
                  alignItems: 'flex-start',
                  padding: '10px 15px',
                }}
              >
                <div style={{ lineHeight: 1.4 }}>
                  <div style={{ fontWeight: 600 }}>{item.title}</div>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {item.description}
                  </Typography.Text>
                </div>
              </Button>
            </List.Item>
          )}
        />
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
