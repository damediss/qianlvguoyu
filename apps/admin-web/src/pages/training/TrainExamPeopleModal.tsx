import { useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Switch,
  Table,
  Typography,
  message,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  IdcardOutlined,
  ImportOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  pageExamPeople,
  updateExamPeopleGrade,
  importExamPeopleByIdCard,
  importExamPeopleFromTrain,
  batchDeleteExamPeople,
} from '../../api/training';
import type { TrainExamPeople } from '../../types/training';

interface Props {
  open: boolean;
  examId: number;
  examTitle: string;
  onClose: () => void;
}

export function TrainExamPeopleModal({ open, examId, examTitle, onClose }: Props) {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [importOpen, setImportOpen] = useState(false);
  const [importText, setImportText] = useState('');
  const [importFromTrainOpen, setImportFromTrainOpen] = useState(false);
  const [trainId, setTrainId] = useState<number | undefined>();
  const [gradeOpen, setGradeOpen] = useState(false);
  const [editing, setEditing] = useState<TrainExamPeople | null>(null);
  const [gradeForm] = Form.useForm();

  const { data, isLoading } = useQuery({
    queryKey: ['train-exam-people', examId, page, pageSize, keyword],
    queryFn: () => pageExamPeople(examId, { page, pageSize, keyword: keyword || undefined }),
    enabled: open && examId > 0,
  });

  const importMu = useMutation({
    mutationFn: () => importExamPeopleByIdCard(examId, importText),
    onSuccess: r => {
      message.success(`导入 ${r.count} 人`);
      setImportOpen(false);
      setImportText('');
      queryClient.invalidateQueries({ queryKey: ['train-exam-people', examId] });
    },
    onError: () => message.error('导入失败'),
  });
  const importTrainMu = useMutation({
    mutationFn: () => importExamPeopleFromTrain(examId, trainId as number),
    onSuccess: r => {
      message.success(`从培训导入 ${r.count} 人`);
      setImportFromTrainOpen(false);
      setTrainId(undefined);
      queryClient.invalidateQueries({ queryKey: ['train-exam-people', examId] });
    },
    onError: () => message.error('导入失败'),
  });
  const batchDelMu = useMutation({
    mutationFn: () => batchDeleteExamPeople(selectedIds),
    onSuccess: () => {
      message.success('删除成功');
      setSelectedIds([]);
      queryClient.invalidateQueries({ queryKey: ['train-exam-people', examId] });
    },
    onError: () => message.error('删除失败'),
  });
  const gradeMu = useMutation({
    mutationFn: (v: { inWhiteList?: number; normalScore?: number; score?: number; totalScore?: number }) =>
      updateExamPeopleGrade(editing!.id, v),
    onSuccess: () => {
      message.success('成绩已更新');
      setGradeOpen(false);
      setEditing(null);
      queryClient.invalidateQueries({ queryKey: ['train-exam-people', examId] });
    },
    onError: () => message.error('更新失败'),
  });

  const openGrade = (p: TrainExamPeople) => {
    setEditing(p);
    gradeForm.setFieldsValue({
      inWhiteList: p.inWhiteList === 1,
      normalScore: p.normalScore,
      score: p.score,
      totalScore: p.totalScore,
    });
    setGradeOpen(true);
  };

  const columns: ColumnsType<TrainExamPeople> = [
    { title: '姓名', dataIndex: 'realname', width: 120 },
    { title: '身份证号', dataIndex: 'idCardNum', width: 200 },
    { title: '手机', dataIndex: 'phone', width: 130 },
    { title: '工种', dataIndex: 'workTypeId', width: 90 },
    { title: '平时成绩', dataIndex: 'normalScore', width: 100 },
    { title: '考试成绩', dataIndex: 'score', width: 100 },
    { title: '总分', dataIndex: 'totalScore', width: 100 },
    {
      title: '白名单', dataIndex: 'inWhiteList', width: 90,
      render: (v?: number) => (v === 1 ? '是' : '否'),
    },
    {
      title: '操作', key: 'act', width: 100, fixed: 'right',
      render: (_, r) => (
        <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openGrade(r)}>
          改成绩
        </Button>
      ),
    },
  ];

  return (
    <>
      <Modal
        title={`考生管理 — ${examTitle || ''}`}
        open={open}
        width={1100}
        onCancel={onClose}
        footer={null}
        destroyOnHidden
      >
        <Space wrap style={{ marginBottom: 12 }}>
          <Input.Search
            allowClear placeholder="搜索姓名/手机" style={{ width: 220 }}
            value={keyword}
            onChange={e => { setKeyword(e.target.value); setPage(1); }}
            onSearch={v => { setKeyword(v); setPage(1); }}
          />
          <Button icon={<IdcardOutlined />} onClick={() => setImportOpen(true)}>身份证导入</Button>
          <Button icon={<ImportOutlined />} onClick={() => setImportFromTrainOpen(true)}>从培训导入</Button>
          <Popconfirm
            title={`确定删除选中的 ${selectedIds.length} 名考生?`}
            onConfirm={() => batchDelMu.mutate()}
            disabled={selectedIds.length === 0}
          >
            <Button danger icon={<DeleteOutlined />} disabled={selectedIds.length === 0}>
              批量删除 {selectedIds.length > 0 && `(${selectedIds.length})`}
            </Button>
          </Popconfirm>
        </Space>
        <Table
          rowKey="id"
          size="small"
          loading={isLoading}
          columns={columns}
          dataSource={data?.items ?? []}
          scroll={{ x: 1000 }}
          rowSelection={{
            selectedRowKeys: selectedIds,
            onChange: keys => setSelectedIds(keys.map(k => Number(k))),
          }}
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
      </Modal>

      {/* 身份证导入 */}
      <Modal
        title="身份证号导入考生"
        open={importOpen}
        onOk={() => importMu.mutate()}
        onCancel={() => setImportOpen(false)}
        confirmLoading={importMu.isPending}
        destroyOnHidden
      >
        <Typography.Paragraph type="secondary">每行一个 18 位身份证号，自动跳过重复项</Typography.Paragraph>
        <Input.TextArea
          rows={8}
          value={importText}
          onChange={e => setImportText(e.target.value)}
          placeholder={'452202200001011234\n110101199001011235\n...'}
        />
      </Modal>

      {/* 从培训导入 */}
      <Modal
        title="从培训导入考生"
        open={importFromTrainOpen}
        onOk={() => trainId && importTrainMu.mutate()}
        onCancel={() => { setImportFromTrainOpen(false); setTrainId(undefined); }}
        confirmLoading={importTrainMu.isPending}
        destroyOnHidden
      >
        <Typography.Paragraph type="secondary">选择一个培训，将该培训的所有学员导入到本考试</Typography.Paragraph>
        <InputNumber
          min={1} style={{ width: '100%' }} placeholder="培训 ID（暂时手工输入）"
          value={trainId} onChange={v => setTrainId(v == null ? undefined : Number(v))}
        />
      </Modal>

      {/* 改成绩 */}
      <Modal
        title={`改成绩 — ${editing?.realname ?? ''}`}
        open={gradeOpen}
        onOk={() => gradeForm.submit()}
        onCancel={() => { setGradeOpen(false); setEditing(null); }}
        confirmLoading={gradeMu.isPending}
        destroyOnHidden
      >
        <Form form={gradeForm} layout="vertical" onFinish={v => {
          gradeMu.mutate({
            inWhiteList: v.inWhiteList ? 1 : 0,
            normalScore: v.normalScore,
            score: v.score,
            totalScore: v.totalScore,
          });
        }}>
          <Form.Item name="inWhiteList" label="白名单" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="normalScore" label="平时成绩">
            <InputNumber min={0} step={0.5} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="score" label="考试成绩">
            <InputNumber min={0} step={0.5} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="totalScore" label="总分">
            <InputNumber min={0} step={0.5} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
