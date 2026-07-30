import { useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Select, Space, Steps, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { DemandPending, TrainInfoList, TrainInfoListQuery, DemandConvertReq, DemandPublicData, CompanyOption, WorkTypeGroup } from '../../types/training';

const workLevels = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

export function RequirementsManagementPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<number>(0); // 0=待办, -1=已办
  const [form] = Form.useForm<DemandConvertReq>();
  const [modalOpen, setModalOpen] = useState(false);
  const [pendingQuery] = useState({ page: 1, pageSize: 30 });

  // Filters for 已办 (train_info_list)
  const [infoQuery, setInfoQuery] = useState<TrainInfoListQuery & { passStatus?: number; companyId?: number; workTypeId?: number }>({ page: 1, pageSize: 10, passStatus: 1 });
  const [companyIdFilter, setCompanyIdFilter] = useState<number | undefined>();
  const [workTypeIdFilter, setWorkTypeIdFilter] = useState<number | undefined>();

  // 公共数据
  const { data: publicData } = useQuery({
    queryKey: ['demand-public-data'],
    queryFn: () => trainApi.getDemandPublicData(),
    staleTime: 5 * 60 * 1000,
  });

  // 待处理需求列表
  const { data: pendings, isLoading: pendingsLoading } = useQuery({
    queryKey: ['demand-pendings', pendingQuery],
    queryFn: () => trainApi.pageDemandPendings(pendingQuery),
    enabled: activeTab === 0,
  });

  // 已办（培训信息列表）
  const { data: infoList, isLoading: infoListLoading } = useQuery({
    queryKey: ['training-info', infoQuery],
    queryFn: () => trainApi.pageInfoLists(infoQuery),
    enabled: activeTab === -1,
  });

  const convertMu = useMutation({
    mutationFn: (p: DemandConvertReq) => trainApi.convertDemandToTraining(p),
    onSuccess: () => { message.success('提交成功'); setModalOpen(false); form.resetFields(); queryClient.invalidateQueries({ queryKey: ['demand-pendings'] }); queryClient.invalidateQueries({ queryKey: ['training-info'] }); },
    onError: () => message.error('提交失败'),
  });

  const handleDemand = (row: DemandPending) => {
    form.setFieldsValue({
      companyId: row.orgId,
      num: row.nums,
      traindemandId: row.id,
      title: '',
      workTypeId: undefined as unknown as number,
      workTypeLevel: undefined as unknown as number,
      trainCompanyName: '千铝工投',
    });
    setModalOpen(true);
  };

  const openCreate = () => {
    form.resetFields();
    form.setFieldsValue({ trainCompanyName: '千铝工投', num: 0 });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const v = await form.validateFields();
    convertMu.mutate(v);
  };

  const handleInfoSearch = () => {
    setInfoQuery({ ...infoQuery, page: 1, companyId: companyIdFilter, workTypeId: workTypeIdFilter });
  };

  const handleInfoReset = () => {
    setCompanyIdFilter(undefined);
    setWorkTypeIdFilter(undefined);
    setInfoQuery({ page: 1, pageSize: 10, passStatus: 1 });
  };

  // Pending columns
  const pendingColumns: ColumnsType<DemandPending> = [
    { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
    { title: '接受培训单位', dataIndex: 'company', width: 300, align: 'center' },
    { title: '单位类型', dataIndex: 'companyType', width: 150, align: 'center' },
    { title: '内容', dataIndex: 'content' },
    { title: '人数', dataIndex: 'nums', width: 80, align: 'center' },
    { title: '创建时间', dataIndex: 'addtimeCn', width: 180, align: 'center' },
    {
      title: '操作', key: 'act', width: 80, fixed: 'right',
      render: (_, r) => <Button type="primary" size="small" onClick={() => handleDemand(r)}>处理</Button>,
    },
  ];

  // Done (info list) columns
  const doneColumns: ColumnsType<TrainInfoList> = [
    { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
    { title: '标题', dataIndex: 'title', width: 200 },
    { title: '接受培训公司', dataIndex: 'companyName' },
    { title: '培训机构', dataIndex: 'trainCompanyName', width: 150 },
    { title: '工种', dataIndex: 'workTypeId', width: 150, render: (v) => v ?? '-' },
    { title: '人数', dataIndex: 'num', width: 60, align: 'center' },
    {
      title: '是否立项', dataIndex: 'passStatus', width: 100, align: 'center',
      render: (v) => v === 0 ? <Tag>不立项</Tag> : <Tag color="blue">立项</Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime', width: 174 },
  ];

  const stepsItems = [
    { title: '需求管理' },
    { title: '培训信息' },
    { title: '班级人员' },
    { title: '开班申报' },
    { title: '课表配置' },
    { title: '毕业' },
  ];

  const companyOptions = (publicData as DemandPublicData | undefined)?.company_list?.map((c: CompanyOption) => ({ value: c.id, label: c.title })) ?? [];
  const workTypeGroupedOptions = (publicData as DemandPublicData | undefined)?.worktype_list?.map((g: WorkTypeGroup) => ({
    label: g.title,
    options: (g.options ?? []).map((o) => ({ value: o.id, label: o.title })),
  })) ?? [];

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>需求管理</Typography.Title>
          <Typography.Text type="secondary">管理企业培训需求，支持需求处理与培训开班转化</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Steps current={0} size="small" items={stepsItems} style={{ maxWidth: '100%' }} />
      </Card>

      {/* Tab switches */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space>
          <Button type={activeTab === 0 ? 'primary' : 'default'} onClick={() => setActiveTab(0)}>待办</Button>
          <Button type={activeTab === -1 ? 'primary' : 'default'} onClick={() => setActiveTab(-1)}>已办</Button>
          {activeTab === -1 && (
            <>
              <Select
                value={companyIdFilter}
                onChange={(v) => setCompanyIdFilter(v)}
                placeholder="企业筛选"
                allowClear
                showSearch
                filterOption={(input, option) => (option?.label as string ?? '').includes(input)}
                options={companyOptions}
                style={{ width: 200 }}
              />
              <Select
                value={workTypeIdFilter}
                onChange={(v) => setWorkTypeIdFilter(v)}
                placeholder="请选择工种"
                allowClear
                showSearch
                filterOption={(input, option) => (option?.label as string ?? '').includes(input)}
                options={workTypeGroupedOptions}
                style={{ width: 200 }}
              />
              <Button type="primary" onClick={handleInfoReset}>重置</Button>
              <Button type="primary" onClick={openCreate}>添加</Button>
            </>
          )}
        </Space>
      </Card>

      {/* Pending tab */}
      {activeTab === 0 && (
        <Card size="small">
          <Space style={{ marginBottom: 16 }}>
            <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['demand-pendings'] })}>刷新</Button>
          </Space>
          <Table rowKey="id" size="middle" loading={pendingsLoading} columns={pendingColumns}
            dataSource={pendings?.items ?? []}
            scroll={{ x: 900 }}
            pagination={false}
          />
        </Card>
      )}

      {/* Done tab */}
      {activeTab === -1 && (
        <Card size="small">
          <Table rowKey="id" size="middle" loading={infoListLoading} columns={doneColumns}
            dataSource={infoList?.items ?? []}
            scroll={{ x: 1000 }}
            pagination={{
              current: infoQuery.page, pageSize: infoQuery.pageSize, total: infoList?.total ?? 0,
              showSizeChanger: true, showTotal: (t) => `共 ${t} 条`,
              onChange: (p, ps) => setInfoQuery({ ...infoQuery, page: p, pageSize: ps }),
            }}
          />
        </Card>
      )}

      {/* Convert dialog */}
      <Modal title="培训需求管理" open={modalOpen} onOk={handleSubmit} onCancel={() => { setModalOpen(false); form.resetFields(); }}
        confirmLoading={convertMu.isPending} destroyOnClose width={600}>
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="请输入标题" />
          </Form.Item>
          <Form.Item name="companyId" label="接受培训的单位" rules={[{ required: true, message: '请选择接受培训的单位' }]}>
            <Select showSearch filterOption={(input, option) => (option?.label as string ?? '').includes(input)}
              placeholder="请选择接受培训的单位" options={companyOptions} />
          </Form.Item>
          <Form.Item name="trainCompanyName" label="培训机构">
            <Select placeholder="请选择培训机构"
              options={['千铝工投', '广西现代职业技术学院', '广西工业技师学院'].map((v) => ({ value: v, label: v }))} />
          </Form.Item>
          <Form.Item name="workTypeId" label="工种" rules={[{ required: true, message: '请选择工种' }]}>
            <Select showSearch filterOption={(input, option) => (option?.label as string ?? '').includes(input)}
              placeholder="请选择工种" options={workTypeGroupedOptions} />
          </Form.Item>
          <Form.Item name="workTypeLevel" label="等级" rules={[{ required: true, message: '请选择工种等级' }]}>
            <Select placeholder="请选择等级" options={workLevels} />
          </Form.Item>
          <Form.Item name="num" label="人数" rules={[{ required: true, message: '请输入人数' }]}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入人数" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
