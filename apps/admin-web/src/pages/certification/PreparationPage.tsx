import {
  CheckSquareOutlined,
  DatabaseOutlined,
  DownOutlined,
  FileProtectOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  ScheduleOutlined,
  TeamOutlined,
  UpOutlined,
} from '@ant-design/icons';
import { Button, Card, Empty, Select, Space, Tabs, Tag, Typography } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { pagePreparationTasks } from '../../api/certificationPreparation';
import { PermissionGate } from '../../directives/permission';
import type { PreparationTaskQuery } from '../../types/certificationPreparation';
import { AssessorConfig } from './preparation/AssessorConfig';
import { CandidateImport } from './preparation/CandidateImport';
import { ChecklistReview } from './preparation/ChecklistReview';
import { EquipmentConfig } from './preparation/EquipmentConfig';
import { ExamSiteConfig } from './preparation/ExamSiteConfig';
import { ExamStaffConfig } from './preparation/ExamStaffConfig';
import { ExamSessionConfig } from './preparation/ExamSessionConfig';
import { PreparationWorkbench } from './preparation/PreparationWorkbench';
import { PracticalScoringConfig } from './preparation/PracticalScoringConfig';
import { DefenseScoringConfig } from './preparation/DefenseScoringConfig';
import './PreparationPage.css';

type PreparationTabKey = 'workbench' | 'collection' | 'sessions' | 'checklist' | 'equipment' | 'sites' | 'staff' | 'assessors' | 'scoring' | 'defense';

const MORE_TAB_KEYS: PreparationTabKey[] = ['staff', 'equipment', 'sites', 'checklist'];
const MORE_TAB_PANE_KEY = '__more__';

export function PreparationPage() {
  const queryClient = useQueryClient();
  // 认定筹备模块：固定按任务阶段 = preparation 过滤
  const query: PreparationTaskQuery = { page: 1, pageSize: 500, currentPhase: 'preparation' };
  const [selectedTaskId, setSelectedTaskId] = useState<string>();
  const [activeTab, setActiveTab] = useState<PreparationTabKey>('workbench');
  const [collapsedTabs, setCollapsedTabs] = useState(false);
  const [moreSelectOpen, setMoreSelectOpen] = useState(false);

  const tasksQuery = useQuery({
    queryKey: ['certification-preparation-tasks', query],
    queryFn: () => pagePreparationTasks(query),
  });

  const tasks = tasksQuery.data?.items || [];

  useEffect(() => {
    if (!selectedTaskId && tasks.length > 0) {
      setSelectedTaskId(tasks[0].id);
    }
    if (selectedTaskId && tasks.length > 0 && !tasks.some((task) => task.id === selectedTaskId)) {
      setSelectedTaskId(tasks[0].id);
    }
    if (selectedTaskId && !tasksQuery.isFetching && tasks.length === 0) {
      setSelectedTaskId(undefined);
    }
  }, [selectedTaskId, tasks, tasksQuery.isFetching]);

  const selectedTask = useMemo(
    () => tasks.find((task) => task.id === selectedTaskId),
    [selectedTaskId, tasks],
  );

  const taskOptions = useMemo(
    () => tasks.map((task) => ({
      value: task.id,
      label: [task.taskNo, task.taskName].filter(Boolean).join(' · '),
    })),
    [tasks],
  );

  const refreshAll = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation'] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-tasks'] });
    if (selectedTaskId) {
      queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', selectedTaskId] });
      queryClient.invalidateQueries({ queryKey: ['practical-scoring-setup', selectedTaskId] });
      queryClient.invalidateQueries({ queryKey: ['defense-scoring-setup', selectedTaskId] });
    }
  };

  const tabItems = [
    {
      key: 'workbench',
      label: '筹备工作台',
      icon: <SafetyCertificateOutlined />,
      children: (
        <PreparationWorkbench
          taskId={selectedTaskId}
          task={selectedTask}
          onChanged={refreshAll}
        />
      ),
    },
    {
      key: 'collection',
      label: '考生导入',
      icon: <DatabaseOutlined />,
      children: <CandidateImport taskId={selectedTaskId} onChanged={refreshAll} />,
    },
    {
      key: 'sessions',
      label: '考试场次',
      icon: <ScheduleOutlined />,
      children: <ExamSessionConfig taskId={selectedTaskId} onChanged={refreshAll} />,
    },
    {
      key: 'staff',
      label: '考务配置',
      icon: <TeamOutlined />,
      children: <ExamStaffConfig taskId={selectedTaskId} onChanged={refreshAll} />,
    },
    {
      key: 'equipment',
      label: '配置设备',
      icon: <DatabaseOutlined />,
      children: <EquipmentConfig taskId={selectedTaskId} onChanged={refreshAll} />,
    },
    {
      key: 'sites',
      label: '考场配置',
      icon: <ScheduleOutlined />,
      children: <ExamSiteConfig taskId={selectedTaskId} onChanged={refreshAll} />,
    },
    {
      key: 'assessors',
      label: '考评员配置',
      icon: <TeamOutlined />,
      children: <AssessorConfig taskId={selectedTaskId} task={selectedTask} onChanged={refreshAll} />,
    },
    {
      key: 'scoring',
      label: '实操配置',
      icon: <CheckSquareOutlined />,
      children: <PracticalScoringConfig taskId={selectedTaskId} showProgress={false} />,
    },
    {
      key: 'defense',
      label: '综合答辩配置',
      icon: <FileProtectOutlined />,
      children: <DefenseScoringConfig taskId={selectedTaskId} />,
    },
    {
      key: 'checklist',
      label: '清单核查',
      icon: <CheckSquareOutlined />,
      children: <ChecklistReview taskId={selectedTaskId} onChanged={refreshAll} />,
    },
  ];
  const tabOrder: PreparationTabKey[] = [
    'workbench',
    'collection',
    'sessions',
    'assessors',
    'scoring',
    'defense',
    'staff',
    'equipment',
    'sites',
    'checklist',
  ];
  const orderedTabItems = [...tabItems].sort(
    (a, b) => tabOrder.indexOf(a.key as PreparationTabKey) - tabOrder.indexOf(b.key as PreparationTabKey),
  );
  const moreTabKeySet = new Set<string>(MORE_TAB_KEYS);
  const isMoreTab = moreTabKeySet.has(activeTab);
  const moreTabItems = MORE_TAB_KEYS
    .map((key) => orderedTabItems.find((item) => item.key === key))
    .filter((item): item is (typeof orderedTabItems)[number] => Boolean(item));
  const primaryTabItems = orderedTabItems.filter((item) => !moreTabKeySet.has(item.key));
  const activeMoreItem = moreTabItems.find((item) => item.key === activeTab);
  const visibleTabItems = [
    ...primaryTabItems,
    {
      key: MORE_TAB_PANE_KEY,
      label: (
        <Select
          className="preparation-more-select"
          placeholder="更多"
          value={isMoreTab ? activeTab : undefined}
          open={moreSelectOpen}
          options={moreTabItems.map((item) => ({
            value: item.key,
            label: (
              <Space size={6}>
                {item.icon}
                {item.label}
              </Space>
            ),
          }))}
          variant="borderless"
          suffixIcon={<DownOutlined />}
          popupMatchSelectWidth={false}
          getPopupContainer={() => document.body}
          onOpenChange={setMoreSelectOpen}
          onClick={(event) => {
            event.stopPropagation();
            setMoreSelectOpen(true);
          }}
          onMouseDown={(event) => event.stopPropagation()}
          onChange={(key) => {
            setActiveTab(key as PreparationTabKey);
            setMoreSelectOpen(false);
          }}
        />
      ),
      children: activeMoreItem?.children ?? <Empty description="请从「更多」中选择模块" />,
    },
  ];

  return (
    <PermissionGate code="certification.preparation.task.view" fallback={<Empty description="暂无认定筹备访问权限" />}>
      <div className="knowledge-page">
        <div className="page-heading knowledge-heading">
          <div>
            <div className="page-title-line">
              <div className="page-title-icon"><SafetyCertificateOutlined /></div>
              <div>
                <Typography.Title level={1}>认定筹备</Typography.Title>
                <Typography.Text type="secondary">承接认定任务，完成信息收集、考场和考评员准备</Typography.Text>
              </div>
            </div>
            {selectedTask && (
              <Space wrap>
                <Tag color="processing">{selectedTask.taskNo}</Tag>
                <Typography.Text strong>{selectedTask.taskName}</Typography.Text>
                <Typography.Text type="secondary">{selectedTask.workTypeName || '-'} / {selectedTask.levelName || '-'}</Typography.Text>
              </Space>
            )}
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refreshAll}>刷新</Button>
          </Space>
        </div>

        <Card
          className="preparation-task-selector"
          size="small"
          title="当前认定任务"
          extra={<Typography.Text type="secondary">共 {tasksQuery.data?.total || 0} 项</Typography.Text>}
        >
          <Space wrap size={[12, 8]} style={{ width: '100%' }}>
            <Typography.Text type="secondary">选择任务</Typography.Text>
            <Select
              showSearch
              allowClear
              value={selectedTaskId}
              loading={tasksQuery.isFetching}
              disabled={!tasksQuery.isFetching && tasks.length === 0}
              placeholder="请选择认定任务"
              optionFilterProp="label"
              options={taskOptions}
              onChange={(value) => setSelectedTaskId(value || undefined)}
              style={{ width: 'min(100%, 520px)' }}
            />
          </Space>
        </Card>

        <Card
          title="筹备资料与工作台"
          styles={{
            body: collapsedTabs
              ? { display: 'none', padding: 0 }
              : { paddingTop: 12 },
          }}
          extra={(
            <Button
              type="text"
              icon={collapsedTabs ? <DownOutlined /> : <UpOutlined />}
              onClick={() => setCollapsedTabs((prev) => !prev)}
            >
              {collapsedTabs ? '展开' : '折叠'}
            </Button>
          )}
        >
          {!collapsedTabs ? (
            <>
              <Tabs
                className="preparation-workbench-tabs"
                activeKey={isMoreTab ? MORE_TAB_PANE_KEY : activeTab}
                onChange={(key) => {
                  if (key === MORE_TAB_PANE_KEY) {
                    setMoreSelectOpen(true);
                    return;
                  }
                  setMoreSelectOpen(false);
                  setActiveTab(key as PreparationTabKey);
                }}
                items={visibleTabItems}
              />
            </>
          ) : null}
        </Card>
      </div>
    </PermissionGate>
  );
}
