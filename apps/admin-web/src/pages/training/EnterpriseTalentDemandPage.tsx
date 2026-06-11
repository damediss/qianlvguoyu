/**
 * 企业人才需求采集 — 对齐 yun_shang company/DemandPeople.vue
 */
import { useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Pagination,
  Select,
  Space,
  Table,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import type { DemandEmployee } from '../../types/training';
import './EnterpriseTalentDemandPage.css';

const EDUCATION_OPTIONS = [
  '不限',
  '高中及以下',
  '中专/技校',
  '大专',
  '本科',
  '硕士',
  '博士',
];

function formatAddtime(ts?: number): string {
  if (!ts) return '-';
  const d = new Date(ts * 1000);
  return d.toLocaleString('zh-CN', { hour12: false });
}

export function EnterpriseTalentDemandPage() {
  const queryClient = useQueryClient();
  const orgId = useAuthStore((s) => s.currentUser?.orgId);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<DemandEmployee | null>(null);
  const [form] = Form.useForm<{ work: string; education: string; major: string }>();
  const [submitting, setSubmitting] = useState(false);

  const queryParams = useMemo(
    () => ({ page, pageSize, orgId }),
    [page, pageSize, orgId],
  );

  const { data, isLoading } = useQuery({
    queryKey: ['enterprise-demand-employees', queryParams],
    queryFn: () => trainApi.pageDemandEmployees(queryParams),
    enabled: orgId != null,
  });

  const openCreate = () => {
    setEditing(null);
    form.setFieldsValue({ work: '', education: '不限', major: '' });
    setFormOpen(true);
  };

  const openEdit = (row: DemandEmployee) => {
    setEditing(row);
    form.setFieldsValue({
      work: row.work ?? '',
      education: row.education ?? '不限',
      major: row.major ?? '',
    });
    setFormOpen(true);
  };

  const handleSubmit = async () => {
    const v = await form.validateFields();
    if (!orgId) {
      message.error('未获取到企业信息');
      return;
    }
    setSubmitting(true);
    try {
      if (editing?.id) {
        await trainApi.updateDemandEmployee(editing.id, {
          orgId,
          work: v.work,
          education: v.education,
          major: v.major,
        });
      } else {
        await trainApi.createDemandEmployee({
          orgId,
          work: v.work,
          education: v.education,
          major: v.major,
        });
      }
      message.success('提交成功');
      setFormOpen(false);
      queryClient.invalidateQueries({ queryKey: ['enterprise-demand-employees'] });
    } catch {
      message.error('提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = (ids: number[]) => {
    Modal.confirm({
      title: '提示',
      content: '此操作将永久删除，是否继续？',
      onOk: async () => {
        if (ids.length === 1) {
          await trainApi.deleteDemandEmployee(ids[0]);
        } else {
          await trainApi.batchDeleteDemandEmployees(ids);
        }
        message.success('删除成功');
        setSelectedRowKeys([]);
        queryClient.invalidateQueries({ queryKey: ['enterprise-demand-employees'] });
      },
    });
  };

  const columns: ColumnsType<DemandEmployee> = [
    { title: '创建日期', dataIndex: 'addtime', width: 170, render: (v: number) => formatAddtime(v) },
    { title: '岗位', dataIndex: 'work', ellipsis: true },
    { title: '学历', dataIndex: 'education', width: 120 },
    { title: '专业', dataIndex: 'major', width: 140, ellipsis: true },
    {
      title: '操作',
      key: 'act',
      width: 120,
      fixed: 'right',
      render: (_, row) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEdit(row)}>编辑</Button>
          <Button type="link" size="small" danger onClick={() => handleDelete([row.id])}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="enterprise-talent-demand-page">
      <Typography.Title level={4}>人才需求</Typography.Title>
      <div className="enterprise-talent-demand-toolbar">
        <Space>
          <Button type="primary" onClick={openCreate}>添加</Button>
          <Button
            danger
            disabled={selectedRowKeys.length === 0}
            onClick={() => handleDelete(selectedRowKeys)}
          >
            删除
          </Button>
        </Space>
      </div>
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.items ?? []}
        columns={columns}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as number[]),
        }}
        pagination={false}
        scroll={{ x: 720 }}
      />
      <Pagination
        className="enterprise-talent-demand-pagination"
        total={data?.total ?? 0}
        current={page}
        pageSize={pageSize}
        showSizeChanger
        showTotal={(t) => `共 ${t} 条`}
        onChange={(p, ps) => { setPage(p); setPageSize(ps); }}
      />

      <Modal
        title="人才需求信息"
        open={formOpen}
        onCancel={() => setFormOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="work" label="岗位名称" rules={[{ required: true, message: '请输入岗位名称' }]}>
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item name="education" label="学历要求" rules={[{ required: true, message: '请选择学历要求' }]}>
            <Select options={EDUCATION_OPTIONS.map((v) => ({ label: v, value: v }))} />
          </Form.Item>
          <Form.Item name="major" label="专业要求">
            <Input maxLength={255} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
