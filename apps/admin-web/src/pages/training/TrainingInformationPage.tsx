import { useState } from 'react';
import {
  Button, Card, Drawer, Form, Image, Input, InputNumber, Modal, Popconfirm, Select, Space,
  Table, Tag, Typography, message,
} from 'antd';
import { PlusOutlined, ReloadOutlined, QrcodeOutlined, EditOutlined, DeleteOutlined, ExportOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainSignUpSelf, TrainSignUpSelfCreateReq, TrainSignUpPerson } from '../../types/training';

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'default' },
  1: { label: '已通过', color: 'success' },
  2: { label: '拒绝', color: 'error' },
};

export function TrainingInformationPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainSignUpSelfCreateReq>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TrainSignUpSelf | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [activeSignUp, setActiveSignUp] = useState<TrainSignUpSelf | null>(null);
  const [peopleKeyword, setPeopleKeyword] = useState('');
  const [peopleStatus, setPeopleStatus] = useState<number | undefined>();
  const [peoplePage, setPeoplePage] = useState(1);
  const [selectedPeopleIds, setSelectedPeopleIds] = useState<number[]>([]);
  const [qrModal, setQrModal] = useState<{ title: string; url: string; path: string } | null>(null);

  const { data: publicData } = useQuery({
    queryKey: ['training-public-data', 'company', 'traininfo'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'traininfo']),
  });

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['sign-up-collection', page, keyword],
    queryFn: () => trainApi.pageSignUpCollection({ page, pageSize: 20, keyword: keyword || undefined }),
  });

  const { data: peopleData, isLoading: peopleLoading, refetch: refetchPeople } = useQuery({
    queryKey: ['sign-up-people', activeSignUp?.id, peoplePage, peopleKeyword, peopleStatus],
    queryFn: () => trainApi.pageSignUpPeople({
      signUpId: activeSignUp!.id,
      page: peoplePage,
      pageSize: 20,
      keyword: peopleKeyword || undefined,
      status: peopleStatus,
    }),
    enabled: !!activeSignUp?.id,
  });

  const saveMu = useMutation({
    mutationFn: (payload: TrainSignUpSelfCreateReq) =>
      editing ? trainApi.updateSignUp(editing.id, payload) : trainApi.createSignUp(payload),
    onSuccess: () => {
      message.success(editing ? '更新成功' : '创建成功');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
    },
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteSignUp(id),
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['sign-up-collection'] });
    },
  });

  const auditMu = useMutation({
    mutationFn: ({ ids, value }: { ids: number[]; value: number }) =>
      trainApi.batchUpdateSignUpPeople({ ids, field: 'status', value }),
    onSuccess: () => {
      message.success('审核完成');
      setSelectedPeopleIds([]);
      refetchPeople();
    },
  });

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (row: TrainSignUpSelf) => {
    setEditing(row);
    form.setFieldsValue({
      title: row.title,
      cover: row.cover,
      trainTime: row.trainTime,
      examTime: row.examTime,
      orgId: row.orgId,
      trainInfoId: row.trainInfoId,
      sortNumber: row.sortNumber,
    });
    setModalOpen(true);
  };

  const openPeople = (row: TrainSignUpSelf) => {
    setActiveSignUp(row);
    setPeoplePage(1);
    setPeopleKeyword('');
    setPeopleStatus(undefined);
    setSelectedPeopleIds([]);
    setDrawerOpen(true);
  };

  const showQrcode = async (row: TrainSignUpSelf) => {
    try {
      const res = await trainApi.getSignUpQrcode(row.id);
      setQrModal({ title: res.title, url: res.qrcode || row.wxMiniQrcode || '', path: res.registrationPath });
      if (!res.qrcode && !row.wxMiniQrcode) {
        message.info('尚未生成小程序码，请配置微信 access_token 或在 CDN 放置 wxqrcode 文件');
      }
    } catch {
      message.error('获取二维码失败');
    }
  };

  const planColumns: ColumnsType<TrainSignUpSelf> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '封面', dataIndex: 'cover', width: 90,
      render: (v: string) => v ? <Image src={v} width={56} height={36} style={{ objectFit: 'cover' }} /> : '-',
    },
    { title: '培训时间', dataIndex: 'trainTime', width: 140 },
    { title: '考试时间', dataIndex: 'examTime', width: 140 },
    { title: '绑定企业', dataIndex: 'orgName', width: 140, ellipsis: true },
    { title: '报名人数', dataIndex: 'peopleCount', width: 90 },
    { title: '排序', dataIndex: 'sortNumber', width: 70 },
    {
      title: '操作', key: 'act', width: 280, fixed: 'right',
      render: (_, row) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<QrcodeOutlined />} onClick={() => showQrcode(row)}>二维码</Button>
          <Button type="link" size="small" onClick={() => openPeople(row)}>报名列表</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>修改</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate(row.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const peopleColumns: ColumnsType<TrainSignUpPerson> = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '身份证号', dataIndex: 'idCardNumber', width: 180 },
    { title: '学历', dataIndex: 'education', width: 100 },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v: number) => {
        const s = STATUS_MAP[v ?? 0] ?? STATUS_MAP[0];
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
  ];

  const companies = publicData?.company_list ?? [];
  const trainInfos = publicData?.traininfo_list ?? [];

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>信息收集</Typography.Title>
          <Typography.Text type="secondary">报名计划 CRUD、学员审核、导出与扫码报名（对齐 information.vue）</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="标题搜索"
            prefix={<SearchOutlined />}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={() => { setPage(1); refetch(); }}
            style={{ width: 220 }}
            allowClear
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>添加</Button>
          <Button icon={<ReloadOutlined />} onClick={() => refetch()}>刷新</Button>
        </Space>
      </Card>

      <Card size="small">
        <Table
          rowKey="id"
          loading={isLoading}
          columns={planColumns}
          dataSource={data?.records ?? []}
          scroll={{ x: 1200 }}
          pagination={{
            current: page,
            pageSize: 20,
            total: data?.total ?? 0,
            onChange: setPage,
          }}
        />
      </Card>

      <Modal
        title={editing ? '修改报名计划' : '新增报名计划'}
        open={modalOpen}
        onCancel={() => { setModalOpen(false); setEditing(null); }}
        onOk={() => form.validateFields().then((v) => saveMu.mutate(v))}
        confirmLoading={saveMu.isPending}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="cover" label="封面 URL">
            <Input placeholder="图片地址" />
          </Form.Item>
          <Form.Item name="trainTime" label="培训时间" rules={[{ required: true }]}>
            <Input placeholder="如 2026-06-01 至 2026-06-15" />
          </Form.Item>
          <Form.Item name="examTime" label="考试时间">
            <Input />
          </Form.Item>
          <Form.Item name="orgId" label="绑定企业">
            <Select allowClear placeholder="请选择企业" options={companies.map((c) => ({ value: c.id, label: c.title }))} />
          </Form.Item>
          <Form.Item name="trainInfoId" label="培训信息">
            <Select allowClear placeholder="请选择培训信息" options={trainInfos.map((t) => ({ value: t.id, label: t.title }))} />
          </Form.Item>
          <Form.Item name="sortNumber" label="排序号">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={activeSignUp ? `报名列表 - ${activeSignUp.title}` : '报名列表'}
        width={960}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        destroyOnClose
      >
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            placeholder="姓名/手机/身份证"
            value={peopleKeyword}
            onChange={(e) => setPeopleKeyword(e.target.value)}
            onPressEnter={() => { setPeoplePage(1); refetchPeople(); }}
            style={{ width: 200 }}
            allowClear
          />
          <Select
            allowClear
            placeholder="审核状态"
            style={{ width: 120 }}
            value={peopleStatus}
            onChange={setPeopleStatus}
            options={[
              { value: 0, label: '待审核' },
              { value: 1, label: '已通过' },
              { value: 2, label: '拒绝' },
            ]}
          />
          <Button onClick={() => { setPeoplePage(1); refetchPeople(); }}>查询</Button>
          <Button
            type="primary"
            disabled={!selectedPeopleIds.length}
            onClick={() => auditMu.mutate({ ids: selectedPeopleIds, value: 1 })}
          >
            批量通过
          </Button>
          <Button
            danger
            disabled={!selectedPeopleIds.length}
            onClick={() => auditMu.mutate({ ids: selectedPeopleIds, value: 2 })}
          >
            批量拒绝
          </Button>
          <Button
            icon={<ExportOutlined />}
            disabled={!activeSignUp?.id}
            onClick={() => activeSignUp && trainApi.exportSignUpPeople(activeSignUp.id)}
          >
            导出 Excel
          </Button>
        </Space>
        <Table
          rowKey="id"
          loading={peopleLoading}
          columns={peopleColumns}
          dataSource={peopleData?.records ?? []}
          rowSelection={{
            selectedRowKeys: selectedPeopleIds,
            onChange: (keys) => setSelectedPeopleIds(keys as number[]),
          }}
          pagination={{
            current: peoplePage,
            pageSize: 20,
            total: peopleData?.total ?? 0,
            onChange: setPeoplePage,
          }}
        />
      </Drawer>

      <Modal
        title="报名二维码"
        open={!!qrModal}
        onCancel={() => setQrModal(null)}
        footer={null}
      >
        {qrModal && (
          <div style={{ textAlign: 'center' }}>
            <Typography.Paragraph>{qrModal.title}</Typography.Paragraph>
            {qrModal.url ? (
              <Image src={qrModal.url} width={200} />
            ) : (
              <Typography.Text type="secondary">小程序路径：{qrModal.path}</Typography.Text>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
