import { useState } from 'react';
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined, SearchOutlined, TrophyOutlined, NotificationOutlined, EyeOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';

export function TrainingGraduationPage() {
  const queryClient = useQueryClient();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState({ page: 1, pageSize: 20 });
  const [studentModalOpen, setStudentModalOpen] = useState(false);
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['graduation-candidates', query],
    queryFn: () => trainApi.listGraduationCandidates(query),
  });

  const { data: studentData, isLoading: studentLoading } = useQuery({
    queryKey: ['graduation-students', selectedClassId],
    queryFn: () => trainApi.listGraduationStudents(selectedClassId!),
    enabled: !!selectedClassId,
  });

  const graduateMu = useMutation({
    mutationFn: (classId: number) => trainApi.graduateClass(classId),
    onSuccess: (data) => { message.success(`毕业完成，共 ${data.graduateCount} 名学员`); queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] }); },
    onError: () => message.error('毕业操作失败'),
  });

  const notifyMu = useMutation({
    mutationFn: (classId: number) => trainApi.notifyGraduation(classId),
    onSuccess: (data) => message.success(data.message),
    onError: () => message.error('发送通知失败'),
  });

  const handleSearch = () => setQuery({ ...query, page: 1, ...searchForm.getFieldsValue() });

  const columns: ColumnsType<Record<string, unknown>> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '班级名称', dataIndex: 'title', width: 180, ellipsis: true },
    { title: '培训项目', dataIndex: 'trainTitle', width: 200, ellipsis: true },
    { title: '企业', dataIndex: 'companyName', width: 150 },
    { title: '工种', dataIndex: 'workTypeName', width: 120 },
    { title: '学员人数', dataIndex: 'studentCount', width: 100 },
    { title: '开始日期', dataIndex: 'dateStart', width: 110 },
    { title: '结束日期', dataIndex: 'dateEnd', width: 110 },
    {
      title: '操作', key: 'act', width: 280, fixed: 'right',
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />}
            onClick={() => { setSelectedClassId(r.id as number); setStudentModalOpen(true); }}>
            查看学员
          </Button>
          <Popconfirm title="确认毕业?" description="执行毕业将更新培训状态为已完成" onConfirm={() => graduateMu.mutate(r.id as number)}>
            <Button type="link" size="small" icon={<TrophyOutlined />}>毕业</Button>
          </Popconfirm>
          <Button type="link" size="small" icon={<NotificationOutlined />}
            onClick={() => notifyMu.mutate(r.id as number)}>通知</Button>
        </Space>
      ),
    },
  ];

  const studentColumns: ColumnsType<Record<string, unknown>> = [
    { title: '姓名', dataIndex: 'realname', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 130 },
    { title: '身份证号', dataIndex: 'idCard', width: 180 },
    { title: '平时成绩', dataIndex: 'normalScore', width: 90 },
    { title: '考试成绩', dataIndex: 'examScore', width: 90 },
    { title: '实操成绩', dataIndex: 'realScore', width: 90 },
    { title: '班组长', dataIndex: 'isLeader', width: 80, render: (v: number) => v === 1 ? <Tag color="blue">是</Tag> : '否' },
    { title: '班委', dataIndex: 'isCommittee', width: 80, render: (v: number) => v === 1 ? <Tag color="green">是</Tag> : '否' },
  ];

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>毕业管理</Typography.Title>
          <Typography.Text type="secondary">管理培训班级毕业流程，查看学员成绩并执行毕业操作</Typography.Text>
        </div>
      </section>

      <TrainingProcessSteps current={6} />

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item name="keyword">
            <Input placeholder="搜索班级名称..." prefix={<SearchOutlined />} allowClear onPressEnter={handleSearch} style={{ width: 260 }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={handleSearch}>查询</Button>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] })}>刷新</Button>
        </Space>
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns}
          dataSource={(data?.list ?? []) as Record<string, unknown>[]}
          scroll={{ x: 1200 }}
          pagination={{ current: query.page, pageSize: query.pageSize, total: data?.total ?? 0, showSizeChanger: true, showTotal: t => `共 ${t} 条`, onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }) }}
        />
      </Card>

      <Modal title="班级学员成绩" open={studentModalOpen} onCancel={() => { setStudentModalOpen(false); setSelectedClassId(null); }}
        footer={null} width={900} destroyOnHidden>
        <Table rowKey="id" size="small" loading={studentLoading} columns={studentColumns}
          dataSource={(studentData?.list ?? []) as Record<string, unknown>[]}
          pagination={false} scroll={{ x: 800 }}
        />
      </Modal>
    </div>
  );
}
