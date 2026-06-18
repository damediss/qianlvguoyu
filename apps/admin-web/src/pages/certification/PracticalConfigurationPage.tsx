import { ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Empty, Select, Space, Typography } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { pagePreparationTasks } from '../../api/certificationPreparation';
import { PermissionGate } from '../../directives/permission';
import type { PreparationTaskOverview } from '../../types/certificationPreparation';
import { PracticalScoringConfig } from './preparation/PracticalScoringConfig';

export function PracticalConfigurationPage() {
  const queryClient = useQueryClient();
  const [selectedTaskId, setSelectedTaskId] = useState<string>();
  const tasksQuery = useQuery({
    queryKey: ['certification-practical-config-tasks'],
    queryFn: () => pagePreparationTasks({ page: 1, pageSize: 200, currentPhase: 'preparation' }),
  });
  const tasks = tasksQuery.data?.items || [];

  useEffect(() => {
    if (!selectedTaskId && tasks.length > 0) setSelectedTaskId(tasks[0].id);
    if (selectedTaskId && tasks.length > 0 && !tasks.some((task) => task.id === selectedTaskId)) {
      setSelectedTaskId(tasks[0].id);
    }
  }, [selectedTaskId, tasks]);

  const selectedTask = useMemo(
    () => tasks.find((task) => task.id === selectedTaskId),
    [selectedTaskId, tasks],
  );

  const taskLabel = (task: PreparationTaskOverview) =>
    `${task.taskNo} · ${task.taskName} · ${task.workTypeName || '-'} / ${task.levelName || '-'}`;

  return (
    <PermissionGate code="certification.practical.progress" fallback={<Empty description="暂无实操管理权限" />}>
      <div className="knowledge-page">
        <div className="page-heading knowledge-heading">
          <div>
            <Typography.Title level={1}>实操管理</Typography.Title>
            <Typography.Text type="secondary">
              监管实操考试开考、评分提交和完成进度
            </Typography.Text>
          </div>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                queryClient.invalidateQueries({ queryKey: ['certification-practical-config-tasks'] });
                queryClient.invalidateQueries({ queryKey: ['practical-scoring-progress', selectedTaskId] });
              }}
            >
              刷新
            </Button>
          </Space>
        </div>

        <Card title="选择认定任务" style={{ marginBottom: 16 }}>
          <Select
            showSearch
            optionFilterProp="label"
            loading={tasksQuery.isFetching}
            value={selectedTaskId}
            placeholder="请选择认定任务"
            style={{ width: 'min(720px, 100%)' }}
            options={tasks.map((task) => ({ value: task.id, label: taskLabel(task) }))}
            onChange={setSelectedTaskId}
          />
          {selectedTask ? (
            <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
              当前任务：{taskLabel(selectedTask)}
            </Typography.Paragraph>
          ) : null}
        </Card>

        <PracticalScoringConfig
          taskId={selectedTaskId}
          showSetup={false}
          showTemplate={false}
          showProgress
        />
      </div>
    </PermissionGate>
  );
}
