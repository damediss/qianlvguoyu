import { useMemo, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Steps, Table, Tag, Typography, message } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { DemandPending, TrainInfoList, TrainInfoListQuery, DemandConvertReq, DemandPublicData, CompanyOption, WorkTypeGroup, DemandTrain, TrainStudent, SolutionByDemand } from '../../types/training';

const workLevels = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

export function RequirementsManagementPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<number>(0); // 0=待办, 1=全部, -1=已办
  const [form] = Form.useForm<DemandConvertReq>();
  const [modalOpen, setModalOpen] = useState(false);
  const [pendingQuery] = useState({ page: 1, pageSize: 30 });

  // Filters for 已办 (train_info_list)
  const [infoQuery, setInfoQuery] = useState<TrainInfoListQuery & { passStatus?: number; companyId?: number; workTypeId?: number }>({ page: 1, pageSize: 10, passStatus: 1 });
  const [companyIdFilter, setCompanyIdFilter] = useState<number | undefined>();
  const [workTypeIdFilter, setWorkTypeIdFilter] = useState<number | undefined>();

  // Query for 全部 (all demands)
  const [allQuery, setAllQuery] = useState({ page: 1, pageSize: 10 });

  // Edit demand dialog
  const [editDemandOpen, setEditDemandOpen] = useState(false);
  const [editDemandForm] = Form.useForm();
  const [editingDemand, setEditingDemand] = useState<DemandTrain | null>(null);

  // Solution detail dialog
  const [solutionOpen, setSolutionOpen] = useState(false);
  const [solution, setSolution] = useState<SolutionByDemand | null>(null);
  const [currentDemandId, setCurrentDemandId] = useState<number>(0);

  // Reject dialog
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  // Student list dialog
  const [studentOpen, setStudentOpen] = useState(false);
  const [students, setStudents] = useState<TrainStudent[]>([]);

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

  // 全部需求列表
  const { data: allDemands, isLoading: allDemandsLoading } = useQuery({
    queryKey: ['demand-trains', allQuery],
    queryFn: () => trainApi.pageDemandTrains(allQuery),
    enabled: activeTab === 1,
  });

  // 已办（培训信息列表）
  const { data: infoList, isLoading: infoListLoading } = useQuery({
    queryKey: ['training-info', infoQuery],
    queryFn: () => trainApi.pageInfoLists(infoQuery),
    enabled: activeTab === -1,
  });

  const convertMu = useMutation({
    mutationFn: (p: DemandConvertReq) => trainApi.convertDemandToTraining(p),
    onSuccess: () => { message.success('提交成功'); setModalOpen(false); form.resetFields(); queryClient.invalidateQueries({ queryKey: ['demand-pendings'] }); queryClient.invalidateQueries({ queryKey: ['training-info'] }); queryClient.invalidateQueries({ queryKey: ['demand-trains'] }); },
    onError: () => message.error('提交失败'),
  });

  // Edit demand mutation
  const editDemandMu = useMutation({
    mutationFn: (p: { id: number; content: string; nums: number }) =>
      trainApi.updateDemandTrain(p.id, { content: p.content, nums: p.nums } as DemandTrain),
    onSuccess: () => { message.success('保存成功'); setEditDemandOpen(false); queryClient.invalidateQueries({ queryKey: ['demand-trains'] }); },
    onError: () => message.error('保存失败'),
  });

  // Delete demand mutation
  const deleteDemandMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteDemandTrain(id),
    onSuccess: () => { message.success('删除成功'); queryClient.invalidateQueries({ queryKey: ['demand-trains'] }); },
    onError: () => message.error('删除失败'),
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

  // Edit demand handlers
  const openEditDemand = (row: DemandTrain) => {
    editDemandForm.setFieldsValue({ content: row.content, nums: row.nums });
    setEditingDemand(row);
    setEditDemandOpen(true);
  };

  const handleEditDemandSubmit = async () => {
    const v = await editDemandForm.validateFields();
    if (editingDemand) {
      editDemandMu.mutate({ id: editingDemand.id, content: v.content, nums: v.nums });
    }
  };

  // Solution handlers
  const handleProgram = async (row: DemandTrain) => {
    setCurrentDemandId(row.id);
    try {
      const d = await trainApi.getSolutionByDemandId(row.id);
      setSolution(d);
      setSolutionOpen(true);
    } catch { message.error('获取方案失败'); }
  };

  const handleAgree = async () => {
    if (!solution?.train_info_id) return;
    try {
      await trainApi.companyConfirmInfoList(solution.train_info_id);
      message.success('操作成功');
      setSolutionOpen(false);
      queryClient.invalidateQueries({ queryKey: ['demand-trains'] });
    } catch { message.error('操作失败'); }
  };

  const handleRejectOpen = () => { setRejectReason(''); setRejectOpen(true); };

  const handleRejectSubmit = async () => {
    if (!rejectReason.trim()) { message.error('请输入拒绝理由'); return; }
    if (!solution?.train_info_id) return;
    try {
      await trainApi.companyRejectInfoList(solution.train_info_id, rejectReason);
      message.success('操作成功');
      setRejectOpen(false);
      setSolutionOpen(false);
      queryClient.invalidateQueries({ queryKey: ['demand-trains'] });
    } catch { message.error('操作失败'); }
  };

  // Student list
  const handleStudentList = async (row: DemandTrain) => {
    try {
      const list = await trainApi.getDemandStudents(row.id);
      setStudents(list);
      setStudentOpen(true);
    } catch { message.error('获取学员列表失败'); }
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

  // All demands columns (matches Vue DemandTrain.vue)
  const statusMap: Record<number, { color: string; text: string }> = {
    0: { color: 'orange', text: '待处理' },
    1: { color: 'blue', text: '已处理' },
  };

  const allColumns: ColumnsType<DemandTrain> = [
    { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
    { title: '创建日期', dataIndex: 'addtime', width: 150, render: (v: number) => v ? new Date(v * 1000).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '-' },
    { title: '培训内容', dataIndex: 'content' },
    { title: '人数', dataIndex: 'nums', width: 80, align: 'center' },
    {
      title: '需求状态', dataIndex: 'status', width: 100, align: 'center',
      render: (v: number) => {
        const s = statusMap[v] ?? { color: 'default', text: '-' };
        return <Tag color={s.color}>{s.text}</Tag>;
      },
    },
    {
      title: '方案状态', key: 'solutionCn', width: 100, align: 'center',
      render: (_, r) => {
        if (r.status === 0) return <Tag>-</Tag>;
        return <Tag color={r.solutionCn === '已确认' ? 'green' : 'orange'}>{r.solutionCn || '-'}</Tag>;
      },
    },
    {
      title: '操作', key: 'act', width: 200, fixed: 'right',
      render: (_, r) => (
        <Space size="small">
          {r.status === 0 ? (
            <>
              <Button type="link" size="small" onClick={() => openEditDemand(r)}>编辑</Button>
              <Popconfirm title="确定删除?" onConfirm={() => deleteDemandMu.mutate(r.id)}>
                <Button type="link" size="small" danger>删除</Button>
              </Popconfirm>
            </>
          ) : (
            <>
              <Button type="link" size="small" onClick={() => handleProgram(r)}>处理方案</Button>
              <Button type="primary" size="small" onClick={() => handleStudentList(r)}>报名人员</Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  // Done (info list) columns
  const doneColumns: ColumnsType<TrainInfoList> = [
    { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
    { title: '标题', dataIndex: 'title', width: 200 },
    { title: '接受培训公司', dataIndex: 'companyName' },
    { title: '培训机构', dataIndex: 'trainCompanyName', width: 150 },
    { title: '工种', dataIndex: 'workTypeId', width: 150, render: (v: number) => workTypeNameMap.get(v) ?? (v ?? '-') },
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

  // 工种 ID → 名称 映射（父级+子级全量）
  const workTypeNameMap = useMemo(() => {
    const map = new Map<number, string>();
    const list = (publicData as DemandPublicData | undefined)?.worktype_list ?? [];
    for (const g of list) {
      map.set(g.id, g.title);
      for (const o of (g.options ?? [])) {
        map.set(o.id, o.title);
      }
    }
    return map;
  }, [publicData]);

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
          <Button type={activeTab === 1 ? 'primary' : 'default'} onClick={() => setActiveTab(1)}>全部</Button>
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

      {/* All demands tab */}
      {activeTab === 1 && (
        <Card size="small">
          <Space style={{ marginBottom: 16 }}>
            <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ['demand-trains'] })}>刷新</Button>
          </Space>
          <Table rowKey="id" size="middle" loading={allDemandsLoading} columns={allColumns}
            dataSource={allDemands?.items ?? []}
            scroll={{ x: 1000 }}
            pagination={{
              current: allQuery.page, pageSize: allQuery.pageSize, total: allDemands?.total ?? 0,
              showSizeChanger: true, showTotal: (t) => `共 ${t} 条`,
              onChange: (p, ps) => setAllQuery({ page: p, pageSize: ps }),
            }}
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

      {/* Edit Demand Dialog */}
      <Modal title="编辑需求" open={editDemandOpen} onOk={handleEditDemandSubmit}
        onCancel={() => { setEditDemandOpen(false); editDemandForm.resetFields(); }}
        confirmLoading={editDemandMu.isPending} destroyOnClose>
        <Form form={editDemandForm} layout="vertical">
          <Form.Item name="content" label="培训内容" rules={[{ required: true, message: '请输入培训内容' }]}>
            <Input placeholder="请输入培训内容" />
          </Form.Item>
          <Form.Item name="nums" label="人数" rules={[{ required: true, message: '请输入人数' }]}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入人数" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Solution Detail Dialog */}
      <Modal title="培训方案" open={solutionOpen} onCancel={() => { setSolutionOpen(false); setSolution(null); }}
        footer={solution && !(solution.train_info_status === 1) ? (
          <Space>
            <Button onClick={() => { setSolutionOpen(false); setSolution(null); }}>取消</Button>
            <Button type="primary" onClick={handleAgree}>同意</Button>
            <Button danger onClick={handleRejectOpen}>不同意</Button>
          </Space>
        ) : null}
        width="60%">
        {solution && (
          <div>
            <p style={{ marginTop: 10 }}><strong>方案标题：</strong>{solution.title}</p>
            {solution.reject_reason && <p style={{ marginTop: 10, color: '#ff4d4f' }}><strong>企业拒绝理由：</strong>{solution.reject_reason}</p>}
            <p style={{ marginTop: 10 }}><strong>费用明细：</strong></p>
            <Table dataSource={solution.badgeList ?? []} rowKey="title" size="small" bordered pagination={false}
              columns={[
                { title: '费用名称', dataIndex: 'title' },
                { title: '预算', dataIndex: 'badge', width: 180, render: (v: number) => v?.toLocaleString() },
                { title: '实际支出', dataIndex: 'expend', width: 180, render: (v: number) => v?.toLocaleString() },
              ]} />
            {solution.file && (
              <p style={{ marginTop: 10 }}>
                <strong>方案文档：</strong>
                <a href={solution.file} target="_blank" rel="noopener noreferrer">下载</a>
                {solution.fileType && <Tag style={{ marginLeft: 8 }}>{solution.fileType}</Tag>}
              </p>
            )}
            {solution.file && (
              <div style={{ height: 400, overflow: 'auto', border: '1px solid #e0e0e0', marginTop: 10 }}>
                {solution.fileType === 'pdf' ? (
                  <iframe src={solution.file} style={{ width: '100%', height: '100%', border: 'none' }} title="PDF预览" />
                ) : (solution.fileType === 'docx' || solution.fileType === 'pptx') ? (
                  <iframe src={`https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(solution.file)}`}
                    style={{ width: '100%', height: '100%', border: 'none' }} title="文档预览" />
                ) : null}
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* Reject Reason Dialog */}
      <Modal title="驳回方案" open={rejectOpen} onOk={handleRejectSubmit}
        onCancel={() => setRejectOpen(false)}>
        <Form.Item label="拒绝理由">
          <Input.TextArea rows={4} value={rejectReason} onChange={(e) => setRejectReason(e.target.value)}
            placeholder="请输入拒绝理由" />
        </Form.Item>
      </Modal>

      {/* Student List Dialog */}
      <Modal title="报名学员列表" open={studentOpen} onCancel={() => { setStudentOpen(false); setStudents([]); }}
        footer={null} width="60%">
        <Table dataSource={students} rowKey="id" size="small" bordered pagination={false}
          columns={[
            { title: '序号', key: 'idx', width: 60, align: 'center', render: (_, __, i) => i + 1 },
            { title: '姓名', dataIndex: 'name' },
            { title: '手机号', dataIndex: 'phone' },
            { title: '性别', dataIndex: 'gender' },
            { title: '身份证号', dataIndex: 'idCardNumber' },
          ]} />
      </Modal>
    </div>
  );
}
