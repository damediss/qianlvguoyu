import { useState, useEffect } from 'react';
import { Button, Card, DatePicker, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, ExportOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import * as trainApi from '../../api/training';
import { downloadUrlFile } from '../../api/files';
import { TrainingProcessSteps } from './TrainingProcessSteps';

export function TrainingSchedulePage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm();
  const [classId, setClassId] = useState<number>(0);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [scheduleItems, setScheduleItems] = useState<Record<string, unknown>[]>([]);

  const { data: scheduleData, isLoading } = useQuery({
    queryKey: ['class-schedule', classId],
    queryFn: () => trainApi.getClassSchedule(classId),
    enabled: classId > 0,
  });

  useEffect(() => {
    if (scheduleData) setScheduleItems(scheduleData as Record<string, unknown>[]);
  }, [scheduleData]);

  const { data: classData } = useQuery({
    queryKey: ['info-classes', 'schedule-all'],
    queryFn: async () => {
      const pageSize = 200;
      const maxPages = 50;
      const list: NonNullable<Awaited<ReturnType<typeof trainApi.pageInfoClassList>>['list']> = [];
      let page = 1;
      let total = Number.POSITIVE_INFINITY;
      while (list.length < total && page <= maxPages) {
        const res = await trainApi.pageInfoClassList({ page, pageSize });
        const batch = res.list ?? [];
        list.push(...batch);
        total = typeof res.total === 'number' ? res.total : list.length;
        if (batch.length === 0) break;
        page += 1;
      }
      return { list, total: list.length };
    },
  });

  const saveMu = useMutation({
    mutationFn: () => trainApi.saveClassSchedule(classId, scheduleItems),
    onSuccess: () => { message.success('课表保存成功'); queryClient.invalidateQueries({ queryKey: ['class-schedule', classId] }); },
    onError: () => message.error('保存失败'),
  });

  const exportMu = useMutation({
    mutationFn: () => trainApi.exportClassSchedule(classId),
    onSuccess: async (data) => {
      if (data.downUrl) {
        await downloadUrlFile(data.downUrl, data.fileName);
      }
      message.success('导出成功');
    },
    onError: () => message.error('导出失败'),
  });

  const openAdd = () => { form.resetFields(); setEditingIndex(null); setModalOpen(true); };
  const openEdit = (index: number) => { setEditingIndex(index); form.setFieldsValue(scheduleItems[index]); setModalOpen(true); };
  const handleDelete = (index: number) => setScheduleItems(scheduleItems.filter((_, i) => i !== index));
  const handleSubmit = async () => {
    const v = await form.validateFields();
    const item = { ...v, scheduleDate: v.scheduleDate ? dayjs(v.scheduleDate).format('YYYY-MM-DD') : undefined };
    if (editingIndex !== null) {
      const newItems = [...scheduleItems];
      newItems[editingIndex] = item;
      setScheduleItems(newItems);
    } else {
      setScheduleItems([...scheduleItems, item]);
    }
    setModalOpen(false);
    form.resetFields();
  };

  const columns: ColumnsType<Record<string, unknown>> = [
    { title: '序号', key: 'idx', width: 60, render: (_, __, i) => i + 1 },
    { title: '日期', dataIndex: 'scheduleDate', width: 120 },
    { title: '星期', dataIndex: 'dayOfWeek', width: 70, render: (v: number) => ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][v ?? 0] ?? '' },
    { title: '课程名称', dataIndex: 'courseName', width: 180 },
    { title: '讲师', dataIndex: 'teacherName', width: 120 },
    { title: '教室', dataIndex: 'roomName', width: 120 },
    { title: '开始时间', dataIndex: 'startTime', width: 100 },
    { title: '结束时间', dataIndex: 'endTime', width: 100 },
    {
      title: '操作', key: 'act', width: 120, fixed: 'right',
      render: (_, __, i) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(i)}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(i)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const classOptions = (classData?.list ?? []).map((c) => ({
    value: c.id,
    label: `${c.title} (${c.train_title ?? ''})`,
  }));

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>课表配置</Typography.Title>
          <Typography.Text type="secondary">为班级配置培训课程安排，支持添加、编辑和导出课表</Typography.Text>
        </div>
      </section>

      <TrainingProcessSteps current={5} />

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space>
          <span>选择班级：</span>
          <Select showSearch allowClear placeholder="请选择要排课的班级" style={{ width: 360 }}
            options={classOptions} value={classId || undefined}
            onChange={(v) => setClassId(v ?? 0)}
            filterOption={(input, option) => (option?.label as string ?? '').includes(input)}
          />
          <Button type="primary" disabled={classId <= 0} icon={<PlusOutlined />} onClick={openAdd}>添加课时</Button>
          <Button disabled={classId <= 0 || scheduleItems.length === 0} icon={<ReloadOutlined />}
            loading={saveMu.isPending} onClick={() => saveMu.mutate()}>保存课表</Button>
          <Button disabled={classId <= 0 || scheduleItems.length === 0} icon={<ExportOutlined />}
            loading={exportMu.isPending} onClick={() => exportMu.mutate()}>导出CSV</Button>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['class-schedule', classId] })}>刷新</Button>
        </Space>
      </Card>

      <Card size="small">
        <Table rowKey="id" size="middle" loading={isLoading} columns={columns} dataSource={scheduleItems}
          pagination={false} scroll={{ x: 900 }}
          locale={{ emptyText: classId <= 0 ? '请先选择班级' : '暂无课表数据，请添加课时' }}
        />
      </Card>

      <Modal title={editingIndex !== null ? '编辑课时' : '添加课时'} open={modalOpen} onOk={handleSubmit} onCancel={() => { setModalOpen(false); form.resetFields(); }}
        destroyOnHidden width={560}>
        <Form form={form} layout="vertical">
          <Space style={{ width: '100%' }} direction="vertical">
            <Form.Item name="scheduleDate" label="日期" rules={[{ required: true }]}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="dayOfWeek" label="星期">
              <Select placeholder="请选择" allowClear options={[1, 2, 3, 4, 5, 6, 7].map(d => ({ value: d, label: `周${['一', '二', '三', '四', '五', '六', '日'][d - 1]}` }))} />
            </Form.Item>
            <Form.Item name="courseName" label="课程名称" rules={[{ required: true }]}>
              <Input placeholder="请输入课程名称" />
            </Form.Item>
            <Form.Item name="teacherName" label="讲师">
              <Input placeholder="请输入讲师姓名" />
            </Form.Item>
            <Form.Item name="roomName" label="教室">
              <Input placeholder="请输入教室名称" />
            </Form.Item>
            <Space>
              <Form.Item name="startTime" label="开始时间" rules={[{ required: true }]}>
                <Input placeholder="08:00" style={{ width: 140 }} />
              </Form.Item>
              <Form.Item name="endTime" label="结束时间" rules={[{ required: true }]}>
                <Input placeholder="10:00" style={{ width: 140 }} />
              </Form.Item>
            </Space>
            <Form.Item name="status" label="状态">
              <Select placeholder="请选择" allowClear options={[{ value: 0, label: '未开始' }, { value: 1, label: '进行中' }, { value: 2, label: '已完成' }]} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
