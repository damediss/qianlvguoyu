/**
 * 工作台批次总览表：进行中区（置顶、不分页）+ 历史分页区。
 * 状态动效沿用 xmb-status-*；完成行由父级传入 flashBatchId 触发一次高亮闪烁。
 */
import React from 'react';
import { Button, Progress, Space, Table, Tag, Tooltip } from 'antd';
import type { TheoryGenBatch } from '../theoryGenApi';
import { STATUS_TAG } from './constants';

export type BatchAction = 'pause' | 'resume' | 'retry' | 'publish' | 'export';

interface Props {
  activeItems: TheoryGenBatch[];
  historyItems: TheoryGenBatch[];
  historyLoading: boolean;
  historyPage: number;
  historyTotal: number;
  onHistoryPageChange: (page: number, pageSize: number) => void;
  /** workTypeId → 工种名 */
  workTypeName: (id?: number | null) => string;
  flashBatchId?: string;
  publishingBatchId?: string;
  exportingBatchId?: string;
  onOpenDraft: (b: TheoryGenBatch) => void;
  onAction: (action: BatchAction, b: TheoryGenBatch) => void;
}

const LEVEL_TEXT: Record<number, string> = {
  1: '一级/高级技师', 2: '二级/技师', 3: '三级/高级工', 4: '四级/中级工', 5: '五级/初级工',
};

const statusTag = (b: TheoryGenBatch) => {
  const t = STATUS_TAG[b.status] ?? { color: 'default', text: b.status };
  const animCls = b.status === 'pending' ? 'xmb-status-waiting'
    : b.status === 'running' ? 'xmb-status-running' : undefined;
  const tag = <Tag color={t.color} className={animCls}>{t.text}</Tag>;
  // 失败原因挂在状态徽标上（自动补跑耗尽等关键信息不再丢失）
  return b.status === 'failed' && b.failReason
    ? <Tooltip title={b.failReason} overlayStyle={{ maxWidth: 420 }}>{tag}</Tooltip>
    : tag;
};

const columns = (p: Props) => [
  {
    title: '批次', dataIndex: 'id', key: 'id', width: 150,
    render: (id: string, b: TheoryGenBatch) => {
      const time = b.startedAt ?? b.createdAt ?? b.finishedAt;
      return (
        <div style={{ lineHeight: 1.4 }}>
          <div>{time ? new Date(time).toLocaleString('zh-CN', { hour12: false }).slice(5, 16) : '-'}</div>
          <div style={{ color: '#999', fontSize: 12 }}>批次…{id.slice(-8)}</div>
        </div>
      );
    },
  },
  {
    title: '工种/等级', key: 'workType', width: 190, ellipsis: true,
    render: (_: unknown, b: TheoryGenBatch) =>
      `${p.workTypeName(b.workTypeId)} · ${b.level ? LEVEL_TEXT[b.level] ?? `${b.level}级` : '-'}`,
  },
  { title: '模型', dataIndex: 'modelId', key: 'modelId', width: 130, ellipsis: true },
  { title: '状态', key: 'status', width: 100, render: (_: unknown, b: TheoryGenBatch) => statusTag(b) },
  {
    title: '生成进度', key: 'progress', width: 220,
    render: (_: unknown, b: TheoryGenBatch) => {
      const pct = b.totalPoints > 0 ? Math.floor((b.donePoints / b.totalPoints) * 100) : 0;
      const live = b.status === 'running' || b.status === 'pending';
      return (
        <div style={{ lineHeight: 1.5 }}>
          <Progress percent={pct} size="small" status={live ? 'active' : 'normal'}
            format={() => `${b.donePoints}/${b.totalPoints} 点`} />
          <span style={{ fontSize: 12, color: '#666' }}>
            已产题 {b.totalQuestions} 题{(b.subjectiveCount ?? 0) > 0 ? `（主观 ${b.subjectiveCount}）` : ''}
          </span>
        </div>
      );
    },
  },
  {
    title: '审核进度', key: 'review', width: 190,
    render: (_: unknown, b: TheoryGenBatch) => (
      <span style={{ fontSize: 12 }}>
        <span style={{ color: '#389e0d' }}>通过 {b.approvedCount ?? 0}</span>
        {' · '}
        <span style={{ color: '#d46b08' }}>待审 {b.pendingReviewCount ?? 0}</span>
        {' · '}
        <span style={{ color: '#cf1322' }}>驳回 {b.rejectedCount ?? 0}</span>
      </span>
    ),
  },
  {
    title: '入库', key: 'publish', width: 100,
    render: (_: unknown, b: TheoryGenBatch) => ((b.publishedCount ?? 0) > 0
      ? <Tag color="success">已入库 {b.publishedCount}</Tag>
      : <Tag>未入库</Tag>),
  },
  {
    title: '操作', key: 'op', width: 250, fixed: 'right' as const,
    render: (_: unknown, b: TheoryGenBatch) => (
      <Space size={4} wrap>
        <Button size="small" type="primary" onClick={() => p.onOpenDraft(b)}>草稿</Button>
        {b.status === 'running' && (
          <Button size="small" onClick={() => p.onAction('pause', b)}>暂停</Button>
        )}
        {b.status === 'paused' && (
          <Button size="small" onClick={() => p.onAction('resume', b)}>继续</Button>
        )}
        {b.status === 'failed' && (
          <Button size="small" onClick={() => p.onAction('retry', b)}>重试失败</Button>
        )}
        {/* 自动补跑轮次耗尽后仍缺口的完成批次：人工兜底补跑（后端仅跑缺口点） */}
        {b.status === 'done' && b.donePoints < b.totalPoints && (
          <Button size="small" onClick={() => p.onAction('retry', b)}>补跑缺口</Button>
        )}
        <Button size="small" loading={p.publishingBatchId === b.id}
          onClick={() => p.onAction('publish', b)}>入库</Button>
        <Button size="small" loading={p.exportingBatchId === b.id}
          onClick={() => p.onAction('export', b)}>导出试题表</Button>
      </Space>
    ),
  },
];

const GenBatchOverviewTable: React.FC<Props> = (props) => {
  const { activeItems, historyItems, historyLoading, historyPage, historyTotal,
    onHistoryPageChange, flashBatchId } = props;
  const rowCls = (b: TheoryGenBatch): string => (flashBatchId === b.id ? 'gen-row-flash' : '');

  return (
    <>
      <div className="gen-section-title">
        进行中批次
        <span style={{ color: '#888', fontWeight: 400, fontSize: 12 }}>
          （等待/生成中的批次置顶显示，进度实时刷新）
        </span>
      </div>
      <Table rowKey="id" size="small" rowClassName={rowCls}
        dataSource={activeItems}
        columns={columns(props)}
        pagination={false}
        locale={{ emptyText: '当前没有进行中的批次' }}
        scroll={{ x: 1280 }} />
      <div className="gen-section-title" style={{ marginTop: 16 }}>历史批次</div>
      <Table rowKey="id" size="small" rowClassName={rowCls}
        loading={historyLoading}
        dataSource={historyItems}
        columns={columns(props)}
        scroll={{ x: 1280 }}
        pagination={{
          current: historyPage,
          pageSize: 10,
          total: historyTotal,
          showSizeChanger: false,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: (p, s) => onHistoryPageChange(p, s),
        }} />
    </>
  );
};

export default GenBatchOverviewTable;
