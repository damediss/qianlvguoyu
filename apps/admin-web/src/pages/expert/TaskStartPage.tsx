import { useState, useCallback, useEffect } from 'react';
import { Button, Card, Col, Form, Input, Modal, Row, Select, Space, Spin, Table, Tag, Tree, message } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '../../api/expert';
import type { ExpertTask, ExpertTaskQuery, ExpertAuditItem, ExpertSubmitReq, XimuTreeNode, ExamshicaoListResponse } from '../../types/expert';

const TAG_COLORS: Record<string, string> = {
  exam: 'blue',
  examshicao: 'orange',
  examsub: 'purple',
  ximu: 'green',
};

export function TaskStartPage() {
  const [query, setQuery] = useState<ExpertTaskQuery>({ page: 1, pageSize: 20, status: 0 });
  const [auditOpen, setAuditOpen] = useState(false);
  const [auditType, setAuditType] = useState('');
  const [auditTitle, setAuditTitle] = useState('');
  const [auditTaskId, setAuditTaskId] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['expert-start-tasks', query],
    queryFn: () => api.pageStartTasks(query),
  });

  const handleAudit = useCallback((row: ExpertTask) => {
    setAuditOpen(true);
    setAuditType(row.tag);
    setAuditTitle(row.title + ' - ' + row.tagCn);
    setAuditTaskId(row.taskId);
  }, []);

  const columns = [
    { title: '名称', dataIndex: 'title', width: 200 },
    {
      title: '类型',
      dataIndex: 'tagCn',
      width: 140,
      render: (_: string, row: ExpertTask) => <Tag color={TAG_COLORS[row.tag] || 'default'}>{row.tagCn}</Tag>,
    },
    { title: '题量', dataIndex: 'nums', width: 80 },
    { title: '指派时间', dataIndex: 'addtimeCn', width: 180 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s: string) => <Tag color={s === '完成' ? 'green' : 'blue'}>{s}</Tag>,
    },
    {
      title: '操作',
      width: 100,
      render: (_: unknown, row: ExpertTask) =>
        row.endtime === 0 ? (
          <Button type="primary" size="small" onClick={() => handleAudit(row)}>
            处理
          </Button>
        ) : null,
    },
  ];

  const closeAudit = useCallback(() => {
    setAuditOpen(false);
    setAuditType('');
    queryClient.invalidateQueries({ queryKey: ['expert-start-tasks'] });
  }, [queryClient]);

  return (
    <Card>
      <Space style={{ marginBottom: 16 }}>
        <Form.Item label="状态" style={{ marginBottom: 0 }}>
          <Select
            value={query.status}
            style={{ width: 150 }}
            onChange={(v) => setQuery({ ...query, status: v, page: 1 })}
            options={[
              { label: '全部', value: 0 },
              { label: '进行中', value: 1 },
              { label: '完成', value: 2 },
            ]}
          />
        </Form.Item>
      </Space>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data?.items || []}
        loading={isLoading}
        pagination={{
          current: query.page,
          pageSize: query.pageSize,
          total: data?.total || 0,
          onChange: (p, ps) => setQuery({ ...query, page: p, pageSize: ps }),
        }}
      />
      {auditOpen && (
        <AuditModal
          type={auditType}
          title={auditTitle}
          taskId={auditTaskId}
          role={0}
          open={auditOpen}
          onClose={closeAudit}
        />
      )}
    </Card>
  );
}

function AuditModal({
  type,
  title,
  taskId,
  role,
  open,
  onClose,
}: {
  type: string;
  title: string;
  taskId: number;
  role: number;
  open: boolean;
  onClose: () => void;
}) {
  switch (type) {
    case 'exam':
      return <ExamAuditPanel title={title} taskId={taskId} role={role} open={open} onClose={onClose} />;
    case 'examshicao':
      return <ExamshicaoAuditPanel title={title} taskId={taskId} role={role} open={open} onClose={onClose} />;
    case 'examsub':
      return <ExamsubAuditPanel title={title} taskId={taskId} role={role} open={open} onClose={onClose} />;
    case 'ximu':
      return <XimuAuditPanel title={title} taskId={taskId} role={role} open={open} onClose={onClose} />;
    default:
      return null;
  }
}

function ExamAuditPanel({
  title: panelTitle,
  taskId,
  role,
  open,
  onClose,
}: {
  title: string;
  taskId: number;
  role: number;
  open: boolean;
  onClose: () => void;
}) {
  const [submitting, setSubmitting] = useState(false);
  const { data: list } = useQuery({
    queryKey: ['expert-exam-list', taskId, role],
    queryFn: () => api.getExamList(taskId, role),
    enabled: open,
  });
  const items = list || [];
  const [idx, setIdx] = useState(0);
  const [form] = Form.useForm();

  const item = items[idx];

  const submit = async () => {
    if (submitting || !item) return;
    setSubmitting(true);
    const values = await form.validateFields();
    const payload: ExpertSubmitReq = { id: item.id, taskId, role, ...values };
    const res = await api.submitExam(payload);
    setSubmitting(false);
    if (res.isdone) {
      message.success('审核完成');
      onClose();
    } else {
      message.success('提交成功');
      form.resetFields();
      setIdx(idx + 1);
    }
  };

  return (
    <Modal title={panelTitle + `(${items.length})`} open={open} onCancel={onClose} footer={null} width={500}>
      {item ? (
        <Form form={form} layout="vertical">
          <Form.Item label="题目类型">
            <Input value={item.exam_type} disabled />
          </Form.Item>
          <Form.Item label="题目内容" name="examContent" rules={[{ required: true }]} initialValue={item.exam_content}>
            <Input.TextArea rows={3} />
          </Form.Item>
          {item.exam_type === '判断题' ? (
            <Form.Item label="参考答案" name="examAnswer" rules={[{ required: true }]} initialValue={item.exam_answer}>
              <Select options={[{ label: '对', value: 'A' }, { label: '错', value: 'B' }]} />
            </Form.Item>
          ) : (
            <>
              {['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'].map((l) => {
                const key = 'exam_' + l.toLowerCase();
                if (!item[key]) return null;
                return (
                  <Form.Item key={l} label={l} name={key} initialValue={item[key]}>
                    <Input />
                  </Form.Item>
                );
              })}
              <Form.Item label="参考答案" name="examAnswer" rules={[{ required: true }]} initialValue={item.exam_answer}>
                <Input />
              </Form.Item>
            </>
          )}
          <Button type="primary" loading={submitting} onClick={submit}>
            提交
          </Button>
        </Form>
      ) : (
        <p>暂无审核数据</p>
      )}
    </Modal>
  );
}

function ExamshicaoAuditPanel({
  title: panelTitle,
  taskId,
  role,
  open,
  onClose,
}: {
  title: string;
  taskId: number;
  role: number;
  open: boolean;
  onClose: () => void;
}) {
  const [submitting, setSubmitting] = useState(false);
  const { data } = useQuery({
    queryKey: ['expert-examshicao-list', taskId, role],
    queryFn: () => api.getExamshicaoList(taskId, role),
    enabled: open,
  });
  const items = data?.list || [];
  const [idx, setIdx] = useState(0);
  const [form] = Form.useForm();

  const item = items[idx];

  useEffect(() => {
    if (item) {
      form.setFieldsValue({
        content: item.content,
        kaochang: item.kaochang,
        kaosheng: item.kaosheng,
        pingfen: item.pingfen || [],
      });
    }
  }, [idx, item, form]);

  const submit = async () => {
    if (submitting || !item) return;
    setSubmitting(true);
    const values = await form.validateFields();
    const payload: ExpertSubmitReq = {
      id: item.id,
      taskId,
      role,
      jianding: item.jianding,
      content: values.content,
      pingfen: JSON.stringify(values.pingfen),
      kaochang: values.kaochang,
      kaosheng: values.kaosheng,
    };
    const res = await api.submitExamshicao(payload);
    setSubmitting(false);
    if (res.isdone) {
      message.success('审核完成');
      onClose();
    } else {
      message.success('提交成功');
      form.resetFields();
      setIdx(idx + 1);
    }
  };

  return (
    <Modal
      title={panelTitle + `(${items.length})`}
      open={open}
      onCancel={onClose}
      width={1000}
      footer={[
        <Button key="cancel" onClick={onClose}>取消</Button>,
        <Button key="submit" type="primary" loading={submitting} onClick={submit}>提交</Button>,
      ]}
    >
      {item ? (
        <Form form={form} layout="horizontal" labelCol={{ flex: '0 0 80px' }} wrapperCol={{ flex: 'auto' }} labelAlign="left">
          <Form.Item label="考评点">
            <Input value={item.jianding} disabled />
          </Form.Item>
          <Form.Item label="题目内容" name="content" rules={[{ required: true }]}>
            <Input.TextArea rows={2} autoSize={{ minRows: 2, maxRows: 4 }} />
          </Form.Item>
          <Form.Item label="考场准备" name="kaochang">
            <Input.TextArea rows={1} autoSize={{ minRows: 1, maxRows: 3 }} />
          </Form.Item>
          <Form.Item label="考生准备" name="kaosheng">
            <Input.TextArea rows={1} autoSize={{ minRows: 1, maxRows: 3 }} />
          </Form.Item>
          <Form.Item label="评分标准">
            <div style={{ display: 'flex', gap: 4, marginBottom: 8 }}>
              <Input value="要素" disabled style={{ flex: 1 }} />
              <Input value="配分" disabled style={{ width: 80 }} />
              <Input value="评分标准" disabled style={{ flex: 2 }} />
              <Input value="最大扣分" disabled style={{ width: 80 }} />
              <Input value="步长" disabled style={{ width: 60 }} />
            </div>
            <Form.List name="pingfen">
              {(fields) => (
                <>
                  {fields.map((field) => (
                    <div key={field.key} style={{ display: 'flex', gap: 4, marginBottom: 4 }}>
                      <Form.Item {...field} name={[field.name, 'yaosu']} style={{ flex: 1, marginBottom: 0 }}>
                        <Input />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'peifen']} style={{ width: 80, marginBottom: 0 }}>
                        <Input />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'biaozhun']} style={{ flex: 2, marginBottom: 0 }}>
                        <Input.TextArea rows={2} autoSize={{ minRows: 2, maxRows: 4 }} />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'koufen']} style={{ width: 80, marginBottom: 0 }}>
                        <Input />
                      </Form.Item>
                      <Form.Item {...field} name={[field.name, 'buchang']} style={{ width: 60, marginBottom: 0 }}>
                        <Input />
                      </Form.Item>
                    </div>
                  ))}
                </>
              )}
            </Form.List>
          </Form.Item>
        </Form>
      ) : (
        <p>暂无审核数据</p>
      )}
    </Modal>
  );
}

function ExamsubAuditPanel({
  title: panelTitle,
  taskId,
  role,
  open,
  onClose,
}: {
  title: string;
  taskId: number;
  role: number;
  open: boolean;
  onClose: () => void;
}) {
  const [submitting, setSubmitting] = useState(false);
  const { data: list } = useQuery({
    queryKey: ['expert-examsub-list', taskId, role],
    queryFn: () => api.getExamsubList(taskId, role),
    enabled: open,
  });
  const items = list || [];
  const [idx, setIdx] = useState(0);
  const [form] = Form.useForm();

  const item = items[idx];

  const submit = async () => {
    if (submitting || !item) return;
    setSubmitting(true);
    const values = await form.validateFields();
    const payload: ExpertSubmitReq = { id: item.id, taskId, role, ...values };
    const res = await api.submitExamsub(payload);
    setSubmitting(false);
    if (res.isdone) {
      message.success('审核完成');
      onClose();
    } else {
      message.success('提交成功');
      form.resetFields();
      setIdx(idx + 1);
    }
  };

  return (
    <Modal title={panelTitle + `(${items.length})`} open={open} onCancel={onClose} footer={null} width={600}>
      {item ? (
        <Form form={form} layout="vertical">
          <Form.Item label="知识点">
            <Input value={item.zhishidian} disabled />
          </Form.Item>
          <Form.Item label="题目内容" name="content" rules={[{ required: true }]} initialValue={item.content}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="参考答案" name="answer" rules={[{ required: true }]} initialValue={item.answer}>
            <Input.TextArea rows={4} />
          </Form.Item>
          <Button type="primary" loading={submitting} onClick={submit}>
            提交
          </Button>
        </Form>
      ) : (
        <p>暂无审核数据</p>
      )}
    </Modal>
  );
}

function XimuAuditPanel({
  title: panelTitle,
  taskId,
  role,
  open,
  onClose,
}: {
  title: string;
  taskId: number;
  role: number;
  open: boolean;
  onClose: () => void;
}) {
  const [submittedIds, setSubmittedIds] = useState<Set<number>>(new Set());
  const [snoTitleValues, setSnoTitleValues] = useState<Map<number, string>>(new Map());
  const [submittingIds, setSubmittingIds] = useState<Set<number>>(new Set());
  const [treeData, setTreeData] = useState<XimuTreeNode[]>([]);
  const [selectedNode, setSelectedNode] = useState<XimuTreeNode | null>(null);

  const { data, isLoading: treeLoading } = useQuery({
    queryKey: ['expert-ximu-list', taskId, role],
    queryFn: () => api.getXimuList(taskId, role),
    enabled: open,
  });

  useEffect(() => {
    if (data) {
      setTreeData(data.treeData);
      setSubmittedIds(new Set());
      setSnoTitleValues(new Map());
      setSubmittingIds(new Set());
      setSelectedNode(null);
    }
  }, [data]);

  const findNodeById = useCallback(
    (nodes: XimuTreeNode[], id: string): XimuTreeNode | null => {
      for (const n of nodes) {
        if (n.id === id) return n;
        if (n.children) {
          const found = findNodeById(n.children, id);
          if (found) return found;
        }
      }
      return null;
    },
    [],
  );

  const handleSelect = useCallback(
    (payload: React.Key[] | { checked: React.Key[]; halfChecked: React.Key[] }) => {
      const keys = Array.isArray(payload) ? payload : payload.checked;
      if (keys.length === 0) {
        setSelectedNode(null);
        return;
      }
      const key = keys[keys.length - 1] as string;
      const node = findNodeById(treeData, key);
      if (!node || node.disabled) return;
      setSelectedNode(node);
      // init snoTitleValues from list
      if (node?.list) {
        setSnoTitleValues(prev => {
          const m = new Map(prev);
          for (const item of node.list!) {
            if (!m.has(item.id)) {
              m.set(item.id, item.sno_title);
            }
          }
          return m;
        });
      }
    },
    [treeData, findNodeById],
  );

  const handleSubmitItem = async (item: any) => {
    if (submittingIds.has(item.id) || submittedIds.has(item.id)) return;
    setSubmittingIds(prev => new Set(prev).add(item.id));
    const currentTitle = snoTitleValues.get(item.id) || item.sno_title;
    const payload: ExpertSubmitReq = {
      id: item.id,
      taskId,
      role,
      sno: item.sno,
      snoItem: item.sno_item,
      snoTitle: currentTitle,
    };
    const res = await api.submitXimu(payload);
    setSubmittingIds(prev => {
      const n = new Set(prev);
      n.delete(item.id);
      return n;
    });
    setSubmittedIds(prev => new Set(prev).add(item.id));

    // check if all items are submitted
    const allSubmitted = new Set(submittedIds);
    allSubmitted.add(item.id);
    const countAll = (nodes: XimuTreeNode[]): { total: number; done: number } => {
      let total = 0, done = 0;
      for (const n of nodes) {
        if (n.list) {
          total += n.list.length;
          done += n.list.filter(li => allSubmitted.has(li.id)).length;
        }
        if (n.children) {
          const sub = countAll(n.children);
          total += sub.total;
          done += sub.done;
        }
      }
      return { total, done };
    };
    const { total, done } = countAll(treeData);
    if (done >= total && total > 0) {
      message.success('审核完成');
      onClose();
    } else {
      message.success('提交成功');
    }
  };

  return (
    <Modal
      title={panelTitle}
      open={open}
      onCancel={onClose}
      footer={null}
      width={1200}
      destroyOnHidden
    >
      {treeLoading ? (
        <Spin style={{ display: 'block', textAlign: 'center', padding: 40 }} />
      ) : treeData.length === 0 ? (
        <p style={{ textAlign: 'center', padding: 40 }}>暂无审核数据</p>
      ) : (
        <Row gutter={16}>
          <Col span={12} style={{ maxHeight: '60vh', overflow: 'auto', borderRight: '1px solid #f0f0f0', fontSize: 13 }}>
            <Tree
              treeData={treeData as any}
              fieldNames={{ title: 'label', key: 'id' }}
              defaultExpandAll
              checkable
              checkStrictly
              checkedKeys={selectedNode ? [selectedNode.id] : []}
              onCheck={(checked) => handleSelect(checked)}
              titleRender={(node: any) => (
                <span>
                  {node.label}
                  {node.title ? <span style={{ marginLeft: 6, color: '#1677ff' }}>{node.title}</span> : null}
                </span>
              )}
            />
          </Col>
          <Col span={12}>
            {selectedNode && selectedNode.list && selectedNode.list.length > 0 ? (
              <div style={{ maxHeight: '60vh', overflow: 'auto' }}>
                {selectedNode.list.map((item) => (
                  <div
                    key={item.id}
                    style={{
                      border: submittedIds.has(item.id) ? '1px solid #d9f7be' : '1px solid #f0f0f0',
                      background: submittedIds.has(item.id) ? '#f6ffed' : '#fff',
                      padding: 8,
                      marginBottom: 8,
                      borderRadius: 4,
                    }}
                  >
                    <div style={{ marginBottom: 4, fontSize: 12, color: '#666' }}>
                      编号: {item.sno_item}
                      {submittedIds.has(item.id) && <Tag color="green" style={{ marginLeft: 4 }}>已提交</Tag>}
                    </div>
                    <Input.TextArea
                      rows={2}
                      style={{ fontSize: 12 }}
                      value={snoTitleValues.get(item.id) || item.sno_title}
                      onChange={(e) =>
                        setSnoTitleValues(prev => new Map(prev).set(item.id, e.target.value))
                      }
                      disabled={submittedIds.has(item.id)}
                    />
                    <Button
                      type="primary"
                      size="small"
                      style={{ marginTop: 4 }}
                      loading={submittingIds.has(item.id)}
                      onClick={() => handleSubmitItem(item)}
                      disabled={submittedIds.has(item.id)}
                    >
                      {submittedIds.has(item.id) ? '已提交' : '提交'}
                    </Button>
                  </div>
                ))}
              </div>
            ) : selectedNode ? (
              <p style={{ color: '#999' }}>该节点下无审核数据</p>
            ) : (
              <p style={{ color: '#999', textAlign: 'center', marginTop: 40 }}>
                请在左侧树中选择一个叶子节点
              </p>
            )}
          </Col>
        </Row>
      )}
    </Modal>
  );
}


export default TaskStartPage;
