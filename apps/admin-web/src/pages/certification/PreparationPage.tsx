import {
  DatabaseOutlined,
  DownOutlined,
  FolderOpenOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  ScheduleOutlined,
  TeamOutlined,
  UpOutlined,
} from '@ant-design/icons';
import { Button, Card, Empty, Form, Input, Progress, Select, Space, Table, Tabs, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { pagePreparationTasks } from '../../api/certificationPreparation';
import { PermissionGate } from '../../directives/permission';
import type { PreparationTaskOverview, PreparationTaskQuery } from '../../types/certificationPreparation';
import { AssessorConfig } from './preparation/AssessorConfig';
import { AssessorWorkbench } from './preparation/AssessorWorkbench';
import { EquipmentConfig } from './preparation/EquipmentConfig';
import { ExamSiteConfig } from './preparation/ExamSiteConfig';
import { ExamStaffConfig } from './preparation/ExamStaffConfig';
import {
  CollectionSubTabKey,
  InformationCollection,
  isCollectionSubTabKey,
} from './preparation/InformationCollection';
import { PreparationWorkbench } from './preparation/PreparationWorkbench';
import { dateText, dateTimeText, statusTag } from './preparation/shared';

type PreparationTabKey = 'workbench' | 'collection' | 'equipment' | 'sites' | 'staff' | 'assessors' | 'assessorWorkbench';

export function PreparationPage() {
  const queryClient = useQueryClient();
  const tabsAnchorRef = useRef<HTMLDivElement | null>(null);
  const [query, setQuery] = useState<PreparationTaskQuery>({ page: 1, pageSize: 10, currentPhase: 'preparation' });
  const [selectedTaskId, setSelectedTaskId] = useState<string>();
  const [activeTab, setActiveTab] = useState<PreparationTabKey>('workbench');
  const [collectionSubTab, setCollectionSubTab] = useState<CollectionSubTabKey>('candidates');
  const [collapsedTaskList, setCollapsedTaskList] = useState(false);
  const [collapsedTabs, setCollapsedTabs] = useState(false);

  const openSection = (key: string) => {
    if (isCollectionSubTabKey(key)) {
      setCollectionSubTab(key);
      setActiveTab('collection');
      return;
    }
    setActiveTab(key as PreparationTabKey);
  };

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
  }, [selectedTaskId, tasks]);

  const selectedTask = useMemo(
    () => tasks.find((task) => task.id === selectedTaskId),
    [selectedTaskId, tasks],
  );

  const refreshAll = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-preparation'] });
    queryClient.invalidateQueries({ queryKey: ['certification-preparation-tasks'] });
    if (selectedTaskId) {
      queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', selectedTaskId] });
    }
  };

  const columns: ColumnsType<PreparationTaskOverview> = [
    {
      title: '任务编号',
      dataIndex: 'taskNo',
      width: 160,
      fixed: 'left',
      render: (value: string, record) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => setSelectedTaskId(record.id)}>
          {value}
        </Button>
      ),
    },
    { title: '任务名称', dataIndex: 'taskName', width: 230, ellipsis: true },
    { title: '批次', dataIndex: 'batchName', width: 160, ellipsis: true },
    { title: '工种', dataIndex: 'workTypeName', width: 110, render: (value) => value || '-' },
    { title: '等级', dataIndex: 'levelName', width: 100, render: (value) => value || '-' },
    { title: '计划开始', dataIndex: 'planStartDate', width: 120, render: dateText },
    { title: '计划结束', dataIndex: 'planEndDate', width: 120, render: dateText },
    { title: '学员', dataIndex: 'candidateCount', width: 80 },
    {
      title: '资料完成率',
      dataIndex: 'documentCompletionRate',
      width: 150,
      render: (value: number) => <Progress percent={value || 0} size="small" />,
    },
    { title: '申报材料', dataIndex: 'materialStatus', width: 110, render: statusTag },
    { title: '考场', dataIndex: 'examSiteCount', width: 80 },
    { title: '考务', dataIndex: 'examStaffCount', width: 80 },
    { title: '考评员', dataIndex: 'assessorCount', width: 90 },
    { title: '状态', dataIndex: 'preparationStatus', width: 110, render: statusTag },
    { title: '最近更新', dataIndex: 'lastUpdatedAt', width: 170, render: dateTimeText },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<FolderOpenOutlined />}
          onClick={() => {
            setSelectedTaskId(record.id);
            setCollapsedTabs(false);
            setActiveTab('workbench');
            requestAnimationFrame(() => {
              tabsAnchorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
          }}
        >
          工作台
        </Button>
      ),
    },
  ];

  const tabItems = [
    {
      key: 'workbench',
      label: '筹备工作台',
      icon: <SafetyCertificateOutlined />,
      children: <PreparationWorkbench taskId={selectedTaskId} task={selectedTask} onOpenSection={openSection} />,
    },
    {
      key: 'collection',
      label: '信息收集',
      icon: <DatabaseOutlined />,
      children: (
        <InformationCollection
          taskId={selectedTaskId}
          activeSubTab={collectionSubTab}
          onSubTabChange={setCollectionSubTab}
          onChanged={refreshAll}
        />
      ),
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
      key: 'assessorWorkbench',
      label: '考评员工作台',
      icon: <SafetyCertificateOutlined />,
      children: <AssessorWorkbench />,
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
          title="认定任务"
          styles={{
            body: collapsedTaskList
              ? { display: 'none', padding: 0 }
              : { paddingTop: 12 },
          }}
          extra={(
            <Button
              type="text"
              icon={collapsedTaskList ? <DownOutlined /> : <UpOutlined />}
              onClick={() => setCollapsedTaskList((prev) => !prev)}
            >
              {collapsedTaskList ? '展开' : '折叠'}
            </Button>
          )}
        >
          {!collapsedTaskList ? (
            <>
              <Form
                layout="inline"
                style={{ marginBottom: 16 }}
                initialValues={{ currentPhase: 'preparation' }}
                onFinish={(values) => setQuery({ ...query, ...values, page: 1 })}
              >
                <Form.Item name="keyword">
                  <Input.Search allowClear placeholder="任务编号、名称、工种" style={{ width: 280 }} />
                </Form.Item>
                <Form.Item name="status">
                  <Select
                    allowClear
                    placeholder="任务状态"
                    style={{ width: 140 }}
                    options={[
                      { label: '进行中', value: 'running' },
                      { label: '已完成', value: 'completed' },
                      { label: '待审核', value: 'pending_review' },
                    ]}
                  />
                </Form.Item>
                <Form.Item name="currentPhase">
                  <Select
                    allowClear
                    placeholder="阶段"
                    style={{ width: 150 }}
                    options={[
                      { label: '筹备期', value: 'preparation' },
                      { label: '全部阶段', value: '' },
                    ]}
                  />
                </Form.Item>
                <Button htmlType="submit">查询</Button>
              </Form>
              <Table
                rowKey="id"
                columns={columns}
                dataSource={tasks}
                loading={tasksQuery.isFetching}
                rowClassName={(record) => record.id === selectedTaskId ? 'selected-row' : ''}
                onRow={(record) => ({ onClick: () => setSelectedTaskId(record.id) })}
                scroll={{ x: 1780 }}
                pagination={{
                  current: query.page,
                  pageSize: query.pageSize,
                  total: tasksQuery.data?.total || 0,
                  showSizeChanger: true,
                  onChange: (page, pageSize) => setQuery({ ...query, page, pageSize }),
                }}
              />
            </>
          ) : null}
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
              <div ref={tabsAnchorRef} />
              <Tabs
                activeKey={activeTab}
                onChange={(key) => setActiveTab(key as PreparationTabKey)}
                items={tabItems}
              />
            </>
          ) : null}
        </Card>
      </div>
    </PermissionGate>
  );
}
