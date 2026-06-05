import { useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Steps, Table, Tag, Typography, message } from 'antd';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainInfoListItem, TrainInfoListEditReq } from '../../types/training';

/** 与 PHP trainingInformation 一致：0 进行中 / 1 待确认 / 2 已完成 */
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '进行中', color: 'processing' },
  1: { label: '待确认', color: 'default' },
  2: { label: '已完成', color: 'success' },
};

export function TrainingInfoPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainInfoListEditReq>();
  const [query, setQuery] = useState({ page: 1, limit: 10, passStatus: 1, headConfirm: 0 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [companyId, setCompanyId] = useState<number | undefined>();
  const [workTypeId, setWorkTypeId] = useState<number | undefined>();

  const { data: publicData } = useQuery({
    queryKey: ['demand-public-data'],
    queryFn: () => trainApi.getDemandPublicData(),
  });

  const { data, isLoading } = useQuery({
    queryKey: ['training-info', query, companyId, workTypeId],
    queryFn: () => trainApi.pageInfoLists({ ...query, companyId, workTypeId }),
  });

  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainInfoListEditReq }) => trainApi.updateInfoList(id, p),
    onSuccess: () => { message.success('更新成功'); closeModal(); queryClient.invalidateQueries({ queryKey: ['training-info'] }); },
    onError: () => message.error('更新失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteInfoList(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['training-info'] }); },
  });

  const closeModal = () => { setModalOpen(false); setEditingId(null); form.resetFields(); };

  const openEdit = (r: TrainInfoListItem) => {
    setEditingId(r.id);
    form.setFieldsValue({ courseNum: r.course_num, position: r.position, status: r.status });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const v = await form.validateFields();
    if (editingId) updateMu.mutate({ id: editingId, p: v });
  };

  const handleReset = () => {
    setCompanyId(undefined);
    setWorkTypeId(undefined);
    setQuery(prev => ({ ...prev, page: 1 }));
  };

  const companyOptions = (publicData?.company_list ?? []).map(c => ({ value: c.id, label: c.title }));

  const workTypeOptions = (publicData?.worktype_list ?? []).flatMap(g => [
    { label: g.title, options: (g.options ?? []).map(o => ({ value: o.id, label: o.title })) },
  ]);

  const columns: ColumnsType<TrainInfoListItem> = [
    {
      title: '序号', key: 'index', width: 60,
      render: (_, __, i) => (query.page - 1) * query.limit + i + 1,
    },
    { title: '标题', dataIndex: 'title', width: 180, ellipsis: true },
    { title: '接受培训公司', dataIndex: 'company_name', width: 160, ellipsis: true },
    { title: '培训机构', dataIndex: 'train_company_name', width: 120 },
    { title: '工种', dataIndex: 'work_type_title', width: 100 },
    { title: '人数', dataIndex: 'num', width: 70, align: 'center' },
    { title: '地点', dataIndex: 'position', width: 100 },
    { title: '课时', dataIndex: 'course_num', width: 70, align: 'center' },
    {
      title: '企业确认', dataIndex: 'company_has_confirm', width: 100,
      render: (v: number) => v === 1 ? <Tag color="success">已确认</Tag> : <Tag color="error">未确认</Tag>,
    },
    {
      title: '状态', dataIndex: 'status', width: 100,
      render: (v: number) => {
        const s = STATUS_MAP[v] ?? { label: '未知', color: 'default' };
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
    { title: '创建时间', dataIndex: 'create_time', width: 160 },
    {
      title: '操作', key: 'act', width: 150, fixed: 'right',
      render: (_, r) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(r)}>修改</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate(r.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="training-page">
      <section className="page-heading">
        <Typography.Title level={4}>培训信息</Typography.Title>
        <Typography.Text type="secondary">管理培训信息，跟踪培训项目状态和进度</Typography.Text>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Steps
          current={1}
          size="small"
          items={[
            { title: '企业需求' },
            { title: '培训信息' },
            { title: '培训方案' },
            { title: '申报' },
            { title: '开班' },
            { title: '补贴' },
          ]}
        />
      </Card>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            allowClear
            showSearch
            placeholder="企业筛选"
            style={{ width: 220 }}
            filterOption={(input, option) => (option?.label as string || '').includes(input)}
            value={companyId}
            onChange={v => { setCompanyId(v); setQuery(prev => ({ ...prev, page: 1 })); }}
            options={companyOptions}
          />
          <Select
            allowClear
            showSearch
            placeholder="工种筛选"
            style={{ width: 220 }}
            filterOption={(input, option) => (option?.label as string || '').includes(input)}
            value={workTypeId}
            onChange={v => { setWorkTypeId(v); setQuery(prev => ({ ...prev, page: 1 })); }}
            options={workTypeOptions}
          />
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </Card>

      <Card size="small">
        <Table
          rowKey="id"
          size="middle"
          loading={isLoading}
          columns={columns}
          dataSource={data?.list ?? []}
          scroll={{ x: 1300 }}
          pagination={{
            current: query.page,
            pageSize: query.limit,
            total: data?.total ?? 0,
            showSizeChanger: false,
            showTotal: t => `共 ${t} 条`,
            position: ['bottomRight'],
            onChange: p => setQuery(prev => ({ ...prev, page: p })),
          }}
        />
      </Card>

      <Modal
        title="编辑培训信息"
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={closeModal}
        confirmLoading={updateMu.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="courseNum" label="课时">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="position" label="地点">
            <Input placeholder="请输入地点" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={Object.entries(STATUS_MAP).map(([k, v]) => ({ value: Number(k), label: v.label }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
