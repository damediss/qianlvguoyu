import { useState } from 'react';
import {
  Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography, message,
} from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { PlatformUser, PlatformUserSaveReq } from '../../types/training';

export function TrainingStudentPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<PlatformUserSaveReq>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PlatformUser | null>(null);
  const [phoneEditable, setPhoneEditable] = useState(false);

  const { data: publicData } = useQuery({
    queryKey: ['training-public-data', 'company'],
    queryFn: () => trainApi.getDemandPublicData(['company']),
  });

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['platform-users', page, keyword],
    queryFn: () => trainApi.pagePlatformUsers({ page, pageSize: 20, keyword: keyword || undefined }),
  });

  const saveMu = useMutation({
    mutationFn: (payload: PlatformUserSaveReq) => trainApi.savePlatformUser(payload),
    onSuccess: () => {
      message.success('保存成功');
      setModalOpen(false);
      setEditing(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['platform-users'] });
    },
    onError: () => message.error('保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deletePlatformUser(id),
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['platform-users'] });
    },
  });

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setPhoneEditable(false);
    setModalOpen(true);
  };

  const openEdit = (row: PlatformUser) => {
    setEditing(row);
    setPhoneEditable(false);
    form.setFieldsValue({
      id: row.id,
      realname: row.realname,
      idcard: row.idcard,
      phone: row.phone,
      companyId: row.companyId,
    });
    setModalOpen(true);
  };

  const columns: ColumnsType<PlatformUser & { companyName?: string; addtimeCn?: string }> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '身份证号', dataIndex: 'idcard', width: 180 },
    { title: '姓名', dataIndex: 'realname', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '所属企业', dataIndex: 'companyName', ellipsis: true },
    { title: '创建时间', dataIndex: 'addtimeCn', width: 170 },
    {
      title: '操作', key: 'act', width: 140, fixed: 'right',
      render: (_, row) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>修改</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate(row.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const companies = publicData?.company_list ?? [];

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>学员管理</Typography.Title>
          <Typography.Text type="secondary">对接 platform_user / platform_user_detail（对齐 student.vue）</Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="关键字搜索"
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
          columns={columns}
          dataSource={data?.items ?? []}
          scroll={{ x: 1000 }}
          pagination={{
            current: page,
            pageSize: 20,
            total: data?.total ?? 0,
            onChange: setPage,
          }}
        />
      </Card>

      <Modal
        title={editing ? '修改学员' : '添加学员'}
        open={modalOpen}
        onCancel={() => { setModalOpen(false); setEditing(null); setPhoneEditable(false); }}
        onOk={() => form.validateFields().then((v) => saveMu.mutate({ ...v, id: editing?.id }))}
        confirmLoading={saveMu.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" autoComplete="off">
          <div aria-hidden style={{ position: 'absolute', width: 0, height: 0, overflow: 'hidden' }}>
            <input tabIndex={-1} autoComplete="username" name="username" />
            <input tabIndex={-1} type="password" autoComplete="current-password" name="current-password" />
          </div>
          <Form.Item name="realname" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input autoComplete="off" />
          </Form.Item>
          <Form.Item name="idcard" label="身份证号" rules={[{ required: true, len: 18, message: '请输入18位身份证号' }]}>
            <Input maxLength={18} autoComplete="off" />
          </Form.Item>
          <Form.Item
            name="phone"
            label="手机号"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号' },
            ]}
          >
            <Input
              maxLength={11}
              inputMode="numeric"
              autoComplete="off"
              readOnly={!phoneEditable}
              onFocus={() => {
                window.setTimeout(() => setPhoneEditable(true), 200);
              }}
            />
          </Form.Item>
          <Form.Item name="password" label="密码" extra="留空则使用默认 123123">
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item name="companyId" label="所属企业">
            <Select allowClear placeholder="请选择" options={companies.map((c) => ({ value: c.id, label: c.title }))} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
