/**
 * 工作台草稿审核面板：默认锁定批次；"全部批次"开关开启后跨批次浏览（默认只显示待审，表格增加批次列）。
 * 批量通过/驳回、行内编辑、展开行选项高亮与原有行为一致；审核/编辑后回调刷新总览计数。
 */
import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Button, Drawer, Form, Input, InputNumber, message, Select, Space, Switch, Table, Tag,
} from 'antd';
import { editDraft, listDrafts, reviewDrafts } from '../theoryGenApi';
import type { TheoryDraft, TheoryGenBatch } from '../theoryGenApi';
import { DIFFICULTY_LABEL, TYPE_LABEL, parseDraftOptions } from './constants';

interface Props {
  /** 锁定的批次；未锁定时（全部批次模式）为 undefined */
  lockedBatch?: TheoryGenBatch;
  /** 无锁定批次打开时默认进入"全部批次"模式 */
  defaultAllMode?: boolean;
  /** 全部批次模式下草稿行的批次标签（找不到时回退批次短号） */
  batchLabel: (batchId: string) => string;
  onClose: () => void;
  /** 审核/编辑成功后回调（刷新总览表计数） */
  onChanged: () => void;
}

const DraftReviewPanel: React.FC<Props> = ({ lockedBatch, defaultAllMode, batchLabel, onClose, onChanged }) => {
  const qc = useQueryClient();
  const [allMode, setAllMode] = useState(defaultAllMode ?? false);
  const [reviewStatus, setReviewStatus] = useState<string | undefined>();
  const [questionType, setQuestionType] = useState<string | undefined>();
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [editingDraft, setEditingDraft] = useState<TheoryDraft | null>(null);
  const [editForm] = Form.useForm();

  // 全部批次模式默认只看待审，避免上千条全量草稿首屏翻页
  React.useEffect(() => {
    if (allMode && reviewStatus === undefined) {
      setReviewStatus('draft');
      setPage(1);
    }
  }, [allMode, reviewStatus]);

  const batchId = allMode ? undefined : lockedBatch?.id;
  const draftsQuery = useQuery({
    queryKey: ['theorygen-drafts', batchId ?? 'all', reviewStatus, questionType, page, pageSize],
    queryFn: () => listDrafts({ batchId, reviewStatus, questionType, page, pageSize }),
    enabled: allMode || !!lockedBatch,
  });

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['theorygen-drafts'] });
    onChanged();
  };

  const bulkReview = async (approve: boolean) => {
    if (!selectedKeys.length) {
      message.warning('请先勾选草稿');
      return;
    }
    const n = await reviewDrafts(selectedKeys as string[], approve);
    message.success(`已${approve ? '通过' : '驳回'} ${n} 条`);
    setSelectedKeys([]);
    refresh();
  };

  const saveDraftEdit = async () => {
    if (!editingDraft) return;
    const values = await editForm.validateFields();
    await editDraft(editingDraft.id, values);
    message.success('已保存');
    setEditingDraft(null);
    refresh();
  };

  const columns = [
    ...(allMode ? [{
      title: '批次', dataIndex: 'batchId', key: 'batchId', width: 170, ellipsis: true,
      render: (v: string) => batchLabel(v),
    }] : []),
    { title: '题干', dataIndex: 'content', key: 'content', ellipsis: true },
    { title: '题型', dataIndex: 'questionType', key: 'questionType', width: 90,
      render: (v: string) => TYPE_LABEL[v] ?? v },
    { title: '答案', dataIndex: 'answer', key: 'answer', width: 80 },
    { title: '难度', dataIndex: 'difficulty', key: 'difficulty', width: 70,
      render: (v: number) => DIFFICULTY_LABEL[v] ?? v },
    { title: '审核', dataIndex: 'reviewStatus', key: 'reviewStatus', width: 80,
      render: (v: string) => (v === 'approved' ? <Tag color="success">已通过</Tag>
        : v === 'rejected' ? <Tag color="error">已驳回</Tag> : <Tag>待审</Tag>) },
    { title: '入库', dataIndex: 'publishStatus', key: 'publishStatus', width: 70,
      render: (v: number) => (v === 1 ? <Tag color="success">已入库</Tag> : <Tag>未入库</Tag>) },
    { title: '操作', key: 'op', width: 70, render: (_: unknown, r: TheoryDraft) => (
      <Button size="small" onClick={() => { setEditingDraft(r); editForm.setFieldsValue(r); }}>编辑</Button>
    ) },
  ];

  return (
    <div className="gen-active-wrap">
      <div className="gen-section-title">
        <span>试题草稿审核</span>
        <span style={{ color: '#888', fontWeight: 400, fontSize: 12 }}>
          {allMode ? '全部批次（默认只显示待审）' : lockedBatch ? `已锁定：${batchLabel(lockedBatch.id)}` : '未选择批次'}
        </span>
        <span style={{ flex: 1 }} />
        <span style={{ fontWeight: 400, fontSize: 12, color: '#666' }}>
          <Switch size="small" checked={allMode} onChange={setAllMode} /> 全部批次
        </span>
        <Button size="small" onClick={onClose}>收起</Button>
      </div>
      <Space style={{ marginBottom: 12 }} wrap>
        <Select style={{ width: 120 }} allowClear placeholder="审核状态"
          value={reviewStatus}
          onChange={(v) => { setReviewStatus(v); setPage(1); }}
          options={[
            { value: 'draft', label: '待审' },
            { value: 'approved', label: '已通过' },
            { value: 'rejected', label: '已驳回' },
          ]} />
        <Select style={{ width: 120 }} allowClear placeholder="题型"
          value={questionType}
          onChange={(v) => { setQuestionType(v); setPage(1); }}
          options={Object.entries(TYPE_LABEL).map(([v, l]) => ({ value: v, label: l }))} />
        <Button onClick={() => bulkReview(true)}>批量通过</Button>
        <Button danger onClick={() => bulkReview(false)}>批量驳回</Button>
      </Space>
      <Table rowKey="id" size="small"
        rowSelection={{ selectedRowKeys: selectedKeys, onChange: (keys) => setSelectedKeys(keys) }}
        expandable={{
          // 选项是重要审核内容：行展开显示全部选项，正确答案高亮（客观题才有选项）
          expandedRowRender: (r: TheoryDraft) => {
            const opts = parseDraftOptions(r.options);
            return (
              <div style={{ display: 'grid', gap: 4, padding: '4px 0' }}>
                {opts.map((o) => (
                  <div key={o.label}
                    style={{ color: r.answer?.includes(o.label) ? '#389e0d' : undefined }}>
                    <strong>{o.label}.</strong> {o.content}
                    {r.answer?.includes(o.label) && (
                      <Tag color="success" style={{ marginLeft: 8 }}>正确答案</Tag>
                    )}
                  </div>
                ))}
              </div>
            );
          },
          rowExpandable: (r: TheoryDraft) => parseDraftOptions(r.options).length > 0,
        }}
        loading={draftsQuery.isLoading}
        dataSource={draftsQuery.data?.items ?? []}
        columns={columns}
        pagination={{
          current: page,
          pageSize,
          total: draftsQuery.data?.total ?? 0,
          showSizeChanger: true,
          pageSizeOptions: [20, 30, 40, 50],
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: (p, s) => {
            setPage(s !== pageSize ? 1 : p);
            setPageSize(s);
          },
        }} />
      <Drawer open={!!editingDraft} width={560}
        title="编辑草稿"
        onClose={() => setEditingDraft(null)}
        extra={<Button type="primary" onClick={saveDraftEdit}>保存</Button>}>
        {editingDraft && (
          <Form form={editForm} layout="vertical">
            <Form.Item name="content" label="题干">
              <Input.TextArea rows={3} />
            </Form.Item>
            <Form.Item name="answer" label="答案">
              <Input />
            </Form.Item>
            <Form.Item name="difficulty" label="难度（1~5）">
              <InputNumber min={1} max={5} />
            </Form.Item>
            <Form.Item name="analysis" label="解析">
              <Input.TextArea rows={2} />
            </Form.Item>
            <Form.Item name="referenceAnswer" label="参考答案（主观题）">
              <Input.TextArea rows={3} />
            </Form.Item>
            <Form.Item name="scorePoints" label="评分要点（主观题）">
              <Input.TextArea rows={2} />
            </Form.Item>
            <Form.Item name="scoringStandard" label="评分标准（主观题）">
              <Input.TextArea rows={2} />
            </Form.Item>
          </Form>
        )}
      </Drawer>
    </div>
  );
};

export default DraftReviewPanel;
