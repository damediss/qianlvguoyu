import { useState } from 'react';
import {
  Button,
  Card,
  Input,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  LineChartOutlined,
  PlusOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  pageExams,
  deleteExam,
} from '../../api/training';
import type { TrainExam, TrainExamQuery } from '../../types/training';
import { TrainExamListFormModal } from './TrainExamListFormModal';
import { TrainExamPeopleModal } from './TrainExamPeopleModal';
import { useAuthStore } from '../../stores/authStore';

/** 统计隐藏路由跳转（与 main.tsx 约定：pushState + popstate） */
function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

const TYPE_OPTIONS = [
  { value: 0, label: '线上' },
  { value: 1, label: '线下' },
];
const SOURCE_OPTIONS = [
  { value: 0, label: '内部' },
  { value: 1, label: '外部' },
];

export function TrainExamListPage() {
  const queryClient = useQueryClient();
  const hasPerm = useAuthStore((s) => s.hasPermission);
  const canViewStats = hasPerm('exam:review-sync:stats:view');
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [type, setType] = useState<number | undefined>();
  const [source, setSource] = useState<number | undefined>();
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | undefined>();
  const [peopleExamId, setPeopleExamId] = useState<number | undefined>();
  const [peopleExamTitle, setPeopleExamTitle] = useState('');

  const query: TrainExamQuery = {
    page,
    pageSize,
    keyword: keyword || undefined,
    type,
    source,
  };
  const { data, isLoading } = useQuery({
    queryKey: ['train-exam-list', query],
    queryFn: () => pageExams(query),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => deleteExam(id),
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['train-exam-list'] });
    },
    onError: () => message.error('删除失败'),
  });

  const onReset = () => {
    setKeyword('');
    setType(undefined);
    setSource(undefined);
    setPage(1);
  };

  const columns: ColumnsType<TrainExam> = [
    { title: '标题', dataIndex: 'title', ellipsis: true, width: 220 },
    { title: '等级', dataIndex: 'level', width: 80 },
    {
      title: '类型', dataIndex: 'type', width: 80,
      render: (v?: number) => (v === 1 ? <Tag>线下</Tag> : <Tag color="blue">线上</Tag>),
    },
    {
      title: '来源', dataIndex: 'source', width: 80,
      render: (v?: number) => (v === 1 ? <Tag>外部</Tag> : <Tag color="green">内部</Tag>),
    },
    { title: '考试日期', dataIndex: 'examDate', width: 120 },
    { title: '考试时长(分)', dataIndex: 'examTime', width: 110 },
    {
      title: '关联课程数', dataIndex: 'courseIdList', width: 110,
      render: (v?: number[]) => (Array.isArray(v) ? v.length : 0),
    },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', key: 'act', width: 300, fixed: 'right',
      render: (_, r) => {
        // 单库化（Q2）：train_exam_list 为合并库唯一表，来源不再区分，编辑/考生/删除全量放行
        return (
          <Space size="small">
            <Button
              type="link" size="small" icon={<EditOutlined />}
              onClick={() => { setEditingId(r.id); setFormOpen(true); }}
            >编辑</Button>
            <Button
              type="link" size="small" icon={<TeamOutlined />}
              onClick={() => { setPeopleExamId(r.id); setPeopleExamTitle(r.title ?? ''); }}
            >考生</Button>
            {canViewStats && (
              <Button
                type="link" size="small" icon={<LineChartOutlined />}
                onClick={() => r.id != null && navigateTo(`/training/exam-lists/${r.id}/review-sync-stats`)}
              >AI同步学统计</Button>
            )}
            <Popconfirm
              title="确定删除该考试计划?"
              onConfirm={() => r.id != null && deleteMu.mutate(r.id)}
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          </Space>
        );
      },
    },
  ];

  return (
    <div className="training-page">
      <section className="page-heading">
        <Typography.Title level={4}>考试计划</Typography.Title>
        <Typography.Text type="secondary">管理考试计划，关联培训/课程/考生</Typography.Text>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            allowClear placeholder="搜索标题/机构" style={{ width: 200 }}
            value={keyword} onChange={e => { setKeyword(e.target.value); setPage(1); }}
          />
          <Select
            allowClear placeholder="类型" style={{ width: 120 }}
            value={type} onChange={v => { setType(v); setPage(1); }} options={TYPE_OPTIONS}
          />
          <Select
            allowClear placeholder="来源" style={{ width: 120 }}
            value={source} onChange={v => { setSource(v); setPage(1); }} options={SOURCE_OPTIONS}
          />
          <Button onClick={onReset}>重置</Button>
          <Button
            type="primary" icon={<PlusOutlined />}
            onClick={() => { setEditingId(undefined); setFormOpen(true); }}
          >新增考试计划</Button>
        </Space>
      </Card>

      <Card size="small">
        <Table
          rowKey="id"
          size="middle"
          loading={isLoading}
          columns={columns}
          dataSource={data?.items ?? []}
          scroll={{ x: 1100 }}
          pagination={{
            current: page,
            pageSize,
            total: data?.total ?? 0,
            showSizeChanger: false,
            showTotal: t => `共 ${t} 条`,
            position: ['bottomRight'],
            onChange: p => setPage(p),
          }}
        />
      </Card>

      <TrainExamListFormModal
        open={formOpen}
        editingId={editingId}
        onClose={() => { setFormOpen(false); setEditingId(undefined); }}
      />
      <TrainExamPeopleModal
        open={!!peopleExamId}
        examId={peopleExamId ?? 0}
        examTitle={peopleExamTitle}
        onClose={() => { setPeopleExamId(undefined); setPeopleExamTitle(''); }}
      />
    </div>
  );
}
