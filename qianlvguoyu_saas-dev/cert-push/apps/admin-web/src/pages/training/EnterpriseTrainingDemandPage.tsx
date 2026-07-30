/**
 * 企业培训需求采集 — 对齐 yun_shang company/DemandTrain.vue
 */
import { useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import type { DemandTrain, SolutionByDemand, TrainStudent } from '../../types/training';
import './EnterpriseTrainingDemandPage.css';

function formatAddtime(ts?: number): string {
  if (!ts) return '-';
  const d = new Date(ts * 1000);
  return d.toLocaleString('zh-CN', { hour12: false });
}

const DEMAND_STATUS: Record<number, { label: string; color: string }> = {
  0: { label: '待处理', color: 'default' },
  1: { label: '已处理', color: 'success' },
};

export function EnterpriseTrainingDemandPage() {
  const queryClient = useQueryClient();
  const orgId = useAuthStore((s) => s.currentUser?.orgId);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<DemandTrain | null>(null);
  const [form] = Form.useForm<{ content: string; nums: number }>();
  const [submitting, setSubmitting] = useState(false);

  const [programOpen, setProgramOpen] = useState(false);
  const [currentDemandId, setCurrentDemandId] = useState<number | null>(null);
  const [program, setProgram] = useState<SolutionByDemand | null>(null);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const [studentOpen, setStudentOpen] = useState(false);
  const [students, setStudents] = useState<TrainStudent[]>([]);

  const queryParams = useMemo(
    () => ({ page, pageSize, orgId }),
    [page, pageSize, orgId],
  );

  const { data, isLoading } = useQuery({
    queryKey: ['enterprise-demand-trains', queryParams],
    queryFn: () => trainApi.pageDemandTrains(queryParams),
    enabled: orgId != null,
  });

  const openCreate = () => {
    setEditing(null);
    form.setFieldsValue({ content: '', nums: 0 });
    setFormOpen(true);
  };

  const openEdit = (row: DemandTrain) => {
    setEditing(row);
    form.setFieldsValue({ content: row.content ?? '', nums: row.nums ?? 0 });
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
        await trainApi.updateDemandTrain(editing.id, {
          id: editing.id,
          orgId,
          content: v.content,
          nums: v.nums,
        });
      } else {
        await trainApi.createDemandTrain({
          id: 0,
          orgId,
          content: v.content,
          nums: v.nums,
          status: 0,
        });
      }
      message.success('提交成功');
      setFormOpen(false);
      queryClient.invalidateQueries({ queryKey: ['enterprise-demand-trains'] });
    } catch {
      message.error('提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (ids: number[]) => {
    Modal.confirm({
      title: '提示',
      content: '此操作将永久删除，是否继续？',
      onOk: async () => {
        if (ids.length === 1) {
          await trainApi.deleteDemandTrain(ids[0]);
        } else {
          await trainApi.batchDeleteDemandTrains(ids);
        }
        message.success('删除成功');
        setSelectedRowKeys([]);
        queryClient.invalidateQueries({ queryKey: ['enterprise-demand-trains'] });
      },
    });
  };

  const loadProgram = async (demandId: number) => {
    setCurrentDemandId(demandId);
    try {
      const info = await trainApi.getSolutionByDemandId(demandId);
      setProgram(info);
      setProgramOpen(true);
    } catch {
      message.error('获取培训方案失败');
    }
  };

  const handleAgree = () => {
    const trainInfoId = program?.train_info_id;
    if (!trainInfoId) {
      message.error('缺少培训信息');
      return;
    }
    Modal.confirm({
      title: '确认同意此项培训方案吗？',
      onOk: async () => {
        setActionLoading(true);
        try {
          await trainApi.companyConfirmInfoList(trainInfoId);
          message.success('操作成功');
          if (currentDemandId) await loadProgram(currentDemandId);
          queryClient.invalidateQueries({ queryKey: ['enterprise-demand-trains'] });
        } catch {
          message.error('操作失败');
        } finally {
          setActionLoading(false);
        }
      },
    });
  };

  const handleRejectSubmit = async () => {
    const trainInfoId = program?.train_info_id;
    if (!trainInfoId) {
      message.error('缺少培训信息');
      return;
    }
    if (!rejectReason.trim()) {
      message.error('请输入拒绝理由');
      return;
    }
    setActionLoading(true);
    try {
      await trainApi.companyRejectInfoList(trainInfoId, rejectReason.trim());
      message.success('操作成功');
      setRejectOpen(false);
      setRejectReason('');
      if (currentDemandId) await loadProgram(currentDemandId);
      queryClient.invalidateQueries({ queryKey: ['enterprise-demand-trains'] });
    } catch {
      message.error('操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const openStudents = async (row: DemandTrain) => {
    setCurrentDemandId(row.id);
    try {
      const list = await trainApi.getDemandStudents(row.id);
      setStudents(list);
      setStudentOpen(true);
    } catch {
      message.error('加载学员列表失败');
    }
  };

  const columns: ColumnsType<DemandTrain> = [
    { title: '创建日期', dataIndex: 'addtime', width: 170, render: (v: number) => formatAddtime(v) },
    { title: '培训内容', dataIndex: 'content', ellipsis: true },
    { title: '人数', dataIndex: 'nums', width: 90 },
    {
      title: '需求状态',
      dataIndex: 'status',
      width: 100,
      render: (v: number) => {
        const s = DEMAND_STATUS[v] ?? { label: '未知', color: 'default' };
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
    {
      title: '方案状态',
      dataIndex: 'solutionCn',
      width: 100,
      render: (v: string) => v || '-',
    },
    {
      title: '操作',
      key: 'act',
      width: 200,
      fixed: 'right',
      render: (_, row) => {
        if (row.status === 0) {
          return (
            <Space size="small">
              <Button type="link" size="small" onClick={() => openEdit(row)}>编辑</Button>
              <Button type="link" size="small" danger onClick={() => handleDelete([row.id])}>删除</Button>
            </Space>
          );
        }
        if (row.solution == null || row.solution === 0) {
          return (
            <Button type="link" size="small" onClick={() => loadProgram(row.id)}>处理方案</Button>
          );
        }
        return (
          <Button type="primary" size="small" onClick={() => openStudents(row)}>报名人员</Button>
        );
      },
    },
  ];

  const canConfirm = program && program.train_info_status !== 1;

  return (
    <div className="enterprise-demand-page">
      <Typography.Title level={4}>企业培训需求</Typography.Title>
      <div className="enterprise-demand-toolbar">
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
        scroll={{ x: 900 }}
      />
      <Pagination
        className="enterprise-demand-pagination"
        total={data?.total ?? 0}
        current={page}
        pageSize={pageSize}
        showSizeChanger
        showTotal={(t) => `共 ${t} 条`}
        onChange={(p, ps) => { setPage(p); setPageSize(ps); }}
      />

      <Modal
        title="培训需求信息"
        open={formOpen}
        onCancel={() => setFormOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="content" label="培训内容" rules={[{ required: true, message: '请输入培训内容' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="nums" label="人数" rules={[{ required: true }]}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="培训方案"
        open={programOpen}
        width={720}
        onCancel={() => { setProgramOpen(false); setProgram(null); }}
        footer={null}
        destroyOnClose
      >
        {canConfirm && (
          <div className="program-actions">
            <Button type="primary" loading={actionLoading} onClick={handleAgree}>同意</Button>
            <Button loading={actionLoading} onClick={() => setRejectOpen(true)}>不同意</Button>
          </div>
        )}
        {program && (
          <div className="program-body">
            <p><strong>方案标题：</strong>{program.title}</p>
            <p><strong>费用明细：</strong></p>
            <Table
              size="small"
              pagination={false}
              rowKey="title"
              dataSource={program.badgeList ?? []}
              columns={[
                { title: '费用名称', dataIndex: 'title' },
                { title: '预算', dataIndex: 'badge', width: 120 },
              ]}
            />
            {program.file && (
              <p className="mar-t10">
                <strong>方案详情：</strong>
                <a href={program.file} target="_blank" rel="noreferrer">下载</a>
              </p>
            )}
            {program.reject_reason && (
              <p className="mar-t10"><strong>驳回理由：</strong>{program.reject_reason}</p>
            )}
          </div>
        )}
      </Modal>

      <Modal
        title="培训方案拒绝"
        open={rejectOpen}
        onCancel={() => { setRejectOpen(false); setRejectReason(''); }}
        onOk={handleRejectSubmit}
        confirmLoading={actionLoading}
      >
        <Input.TextArea
          rows={4}
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
          placeholder="请输入拒绝理由"
        />
      </Modal>

      <Modal
        title="报名学员列表"
        open={studentOpen}
        onCancel={() => { setStudentOpen(false); setStudents([]); }}
        footer={null}
        width={640}
      >
        <Table
          size="small"
          rowKey="id"
          pagination={false}
          dataSource={students}
          columns={[
            { title: '序号', width: 60, render: (_, __, i) => i + 1 },
            { title: '姓名', dataIndex: 'name' },
            { title: '手机号', dataIndex: 'phone' },
            { title: '性别', dataIndex: 'gender' },
            { title: '身份证号', dataIndex: 'idCardNumber' },
          ]}
        />
      </Modal>
    </div>
  );
}
