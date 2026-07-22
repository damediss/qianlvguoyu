import { useMemo, useState } from 'react';
import {
  Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Typography, message,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined, TeamOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listOrgs } from '../../api/system';
import * as trainApi from '../../api/training';
import { ResearchCollectionDrawer } from './ResearchCollectionDrawer';
import { ResearchCollectionFieldOptionsEditor } from './ResearchCollectionFieldOptionsEditor';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import {
  buildEmptyFieldOptionsForm,
  mergeFieldOptionsForForm,
  normalizeCollectionFieldOptions,
} from './researchCollectionFieldOptions';
import type {
  TrainResearchOrg,
  TrainResearchOrgCreateReq,
  TrainResearchOrgQuery,
  TrainResearchOrgUpdateReq,
  ResearchCollectionFieldOptions,
} from '../../types/training';

const { Title } = Typography;

const STATUS_OPTIONS = [
  { value: 1, label: '启用' },
  { value: 0, label: '禁用' },
];

export function EnterpriseResearchPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainResearchOrgCreateReq & { id?: number; collectionFieldOptions?: ResearchCollectionFieldOptions }>();
  const [searchForm] = Form.useForm<{ title?: string; type?: string }>();

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [filters, setFilters] = useState<{ title?: string; type?: string }>({});
  const [collectionDrawerOpen, setCollectionDrawerOpen] = useState(false);
  const [activeResearchOrg, setActiveResearchOrg] = useState<TrainResearchOrg | null>(null);

  const listQuery: TrainResearchOrgQuery = useMemo(
    () => ({
      page: currentPage,
      pageSize,
      ...filters,
    }),
    [currentPage, pageSize, filters],
  );

  const { data, isLoading } = useQuery({
    queryKey: ['research-orgs', listQuery],
    queryFn: () => trainApi.pageResearchOrgs(listQuery),
  });

  const { data: orgList = [] } = useQuery({
    queryKey: ['orgs-simple'],
    queryFn: () => listOrgs(),
  });

  const createMu = useMutation({
    mutationFn: (payload: TrainResearchOrgCreateReq) => trainApi.createResearchOrg(payload),
    onSuccess: () => {
      message.success('创建成功');
      closeModal();
      setCurrentPage(1);
      queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
    },
    onError: () => message.error('创建失败'),
  });

  const updateMu = useMutation({
    mutationFn: ({ id, p }: { id: number; p: TrainResearchOrgUpdateReq }) => trainApi.updateResearchOrg(id, p),
    onSuccess: () => {
      message.success('更新成功');
      closeModal();
      queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
    },
    onError: () => message.error('更新失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteResearchOrg(id),
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
    },
    onError: () => message.error('删除失败'),
  });

  const closeModal = () => {
    setModalOpen(false);
    setEditingId(null);
    form.resetFields();
  };

  const openCreate = () => {
    setEditingId(null);
    form.setFieldsValue({
      status: 1,
      sortNumber: 100,
      collectionFieldOptions: buildEmptyFieldOptionsForm(),
    });
    setModalOpen(true);
  };

  const openEdit = async (r: TrainResearchOrg) => {
    setEditingId(r.id);
    let fieldOptions = r.collectionFieldOptions;
    if (!fieldOptions) {
      try {
        const detail = await trainApi.getResearchOrg(r.id);
        fieldOptions = detail.collectionFieldOptions;
      } catch {
        fieldOptions = undefined;
      }
    }
    form.setFieldsValue({
      title: r.title,
      targetOrgId: r.targetOrgId,
      type: r.type,
      provinceId: r.provinceId,
      provinceName: r.provinceName,
      cityId: r.cityId,
      cityName: r.cityName,
      areaId: r.areaId,
      areaName: r.areaName,
      sortNumber: r.sortNumber,
      status: r.status,
      collectionFieldOptions: mergeFieldOptionsForForm(fieldOptions),
    });
    setModalOpen(true);
  };

  const openCollection = (r: TrainResearchOrg) => {
    setActiveResearchOrg(r);
    setCollectionDrawerOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const payload = {
      ...values,
      collectionFieldOptions: normalizeCollectionFieldOptions(values.collectionFieldOptions),
    };
    if (editingId) {
      updateMu.mutate({ id: editingId, p: payload });
    } else {
      createMu.mutate(payload);
    }
  };

  const handleSearch = (values: { title?: string; type?: string }) => {
    setCurrentPage(1);
    setFilters(values);
  };

  const handleReset = () => {
    searchForm.resetFields();
    setCurrentPage(1);
    setFilters({});
  };

  const list = data?.items ?? [];
  const total = data?.total ?? 0;

  const columns: ColumnsType<TrainResearchOrg> = [
    {
      title: '序号',
      key: 'index',
      width: 60,
      render: (_, __, i) => (currentPage - 1) * pageSize + i + 1,
    },
    { title: '企业/机构名称', dataIndex: 'title', width: 180, ellipsis: true },
    { title: '系统企业', dataIndex: 'targetOrgName', width: 160, ellipsis: true, render: (v) => v || '-' },
    { title: '类型', dataIndex: 'type', width: 120, ellipsis: true },
    {
      title: '地区',
      width: 180,
      render: (_, r) => {
        const parts = [r.provinceName, r.cityName, r.areaName].filter(Boolean);
        return parts.length ? parts.join(' / ') : '-';
      },
    },
    {
      title: '已采集',
      dataIndex: 'personnelCount',
      width: 80,
      align: 'center',
      render: (v: number | undefined, r) => (r.activeSignUpId ? (v ?? 0) : '-'),
    },
    { title: '排序', dataIndex: 'sortNumber', width: 70, align: 'center' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v: number) => (v === 1 ? '启用' : '禁用'),
    },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作',
      key: 'act',
      width: 200,
      fixed: 'right',
      render: (_, r) => (
        <Space>
          <Button type="link" size="small" icon={<TeamOutlined />} onClick={() => openCollection(r)}>
            人员采集
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(r)}>
            编辑
          </Button>
          <Popconfirm title="确定删除吗？" onConfirm={() => deleteMu.mutate(r.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      <TrainingProcessSteps current={0} />
    <Card
      title={<Title level={5} style={{ margin: 0 }}>企业调研</Title>}
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增调研企业
        </Button>
      }
    >
      <Form form={searchForm} layout="inline" onFinish={handleSearch} style={{ marginBottom: 16 }}>
        <Form.Item name="title" label="企业名称">
          <Input placeholder="请输入" allowClear style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="type" label="类型">
          <Input placeholder="请输入" allowClear style={{ width: 160 }} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
            查询
          </Button>
          <Button style={{ marginLeft: 8 }} icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
        </Form.Item>
      </Form>

      <Table<TrainResearchOrg>
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={isLoading}
        pagination={{
          current: currentPage,
          pageSize,
          total,
          onChange: setCurrentPage,
          showSizeChanger: false,
          showTotal: (t) => `共 ${t} 条`,
        }}
        scroll={{ x: 1100 }}
      />

      <Modal
        title={editingId ? '编辑调研企业' : '新增调研企业'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={closeModal}
        confirmLoading={createMu.isPending || updateMu.isPending}
        width={760}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="企业/机构名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item
            name="targetOrgId"
            label="关联系统企业"
            rules={[{ required: true, message: '请选择系统企业' }]}
            extra="用于人员采集、三年规划与企业审核权限"
          >
            <Select
              showSearch
              allowClear
              placeholder="请选择系统企业"
              optionFilterProp="label"
              options={orgList.map((o) => ({ value: o.id, label: o.name }))}
            />
          </Form.Item>
          <Form.Item name="type" label="类型">
            <Input placeholder="请输入" />
          </Form.Item>
          {/* <Form.Item name="provinceName" label="省">
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item name="cityName" label="市">
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item name="areaName" label="区县">
            <Input placeholder="请输入" />
          </Form.Item> */}
          <Form.Item name="sortNumber" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            valuePropName="checked"
            getValueProps={(value) => ({ checked: value === 1 })}
            getValueFromEvent={(checked) => (checked ? 1 : 0)}
          >
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
          <Form.Item name="collectionFieldOptions" label={null}>
            <ResearchCollectionFieldOptionsEditor />
          </Form.Item>
        </Form>
      </Modal>

      <ResearchCollectionDrawer
        open={collectionDrawerOpen}
        researchOrg={activeResearchOrg}
        onClose={() => {
          setCollectionDrawerOpen(false);
          setActiveResearchOrg(null);
          queryClient.invalidateQueries({ queryKey: ['research-orgs'] });
        }}
      />
    </Card>
    </>
  );
}
