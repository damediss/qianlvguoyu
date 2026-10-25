/**
 * 我的编写/审定任务列表 —— 《二期编写协同实施设计》§10.4（TextbookMyTasksPage.tsx）
 * 菜单：textbook_my_writing（/wo-de-bian-xie，mode=writing）/ textbook_my_review（/wo-de-shen-ding，mode=review）
 * 同一组件复用，main.tsx pageMap 分别以显式 mode 注册，不靠组件自行判断。
 *
 * 统计卡直连 T3/T4（当前用户全量口径，不受筛选/分页影响）；空态区分「还没有分配任务」与「当前筛选无结果」。
 */
import { Button, Card, Empty, Input, Select, Space, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import {
  getReviewTaskSummary,
  getWritingTaskSummary,
  pageMyReviewTasks,
  pageMyWritingTasks,
} from '../../api/textbookCollaboration';
import { textbookLevelLabel } from '../../types/textbook';
import type { ReviewTaskSummaryVO, TextbookTaskVO, WritingTaskSummaryVO } from '../../types/textbookCollaboration';
import { OverdueMark, SectionProgress, TaskStatusTag } from './TextbookTaskMeta';

/** 隐藏路由跳转（与 TextbookProjectPage 相同约定：pushState + popstate，由 main.tsx shell 接管） */
function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

const WRITING_STATUS_OPTIONS = [
  { value: 'assigned', label: '待开始' },
  { value: 'writing', label: '编写中' },
  { value: 'returned', label: '已打回' },
  { value: 'reviewing', label: '待审定' },
  { value: 'approved', label: '已完成' },
  { value: 'overdue', label: '已逾期' },
];

const REVIEW_STATUS_OPTIONS = [
  { value: 'pending', label: '待审定' },
  { value: 'returned', label: '已打回' },
  { value: 'approved', label: '全部通过' },
  { value: 'all', label: '全部' },
];

interface SummaryCard {
  key: string;
  label: string;
  value: number;
  /** 点击卡片切换列表 status 筛选 */
  filterValue?: string;
}

function writingCards(summary: WritingTaskSummaryVO | undefined): SummaryCard[] {
  return [
    { key: 'assigned', label: '待开始', value: summary?.assignedCount ?? 0, filterValue: 'assigned' },
    { key: 'writing', label: '编写中', value: summary?.writingCount ?? 0, filterValue: 'writing' },
    { key: 'pendingReview', label: '待审定', value: summary?.pendingReviewCount ?? 0, filterValue: 'reviewing' },
    { key: 'returned', label: '已打回', value: summary?.returnedCount ?? 0, filterValue: 'returned' },
    { key: 'approved', label: '已完成', value: summary?.approvedCount ?? 0, filterValue: 'approved' },
  ];
}

function reviewCards(summary: ReviewTaskSummaryVO | undefined): SummaryCard[] {
  return [
    { key: 'pending', label: '待审定', value: summary?.pendingCount ?? 0, filterValue: 'pending' },
    { key: 'returned', label: '已打回', value: summary?.returnedCount ?? 0, filterValue: 'returned' },
    // 部分通过没有独立列表筛选，点击切到全部；相交口径不求和等于总数
    { key: 'partial', label: '部分通过', value: summary?.partialApprovedCount ?? 0, filterValue: 'all' },
    { key: 'approved', label: '全部通过', value: summary?.approvedCount ?? 0, filterValue: 'approved' },
  ];
}

export function TextbookMyTasksPage({ mode }: { mode: 'writing' | 'review' }) {
  const isWriting = mode === 'writing';
  const [statusFilter, setStatusFilter] = useState<string | undefined>(isWriting ? undefined : 'pending');
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const pageSize = 20;

  const query = useMemo(
    () => ({
      status: statusFilter && statusFilter !== 'all' ? statusFilter : undefined,
      keyword: keyword.trim() || undefined,
      overdueOnly: overdueOnly || undefined,
      page,
      pageSize,
    }),
    [statusFilter, keyword, overdueOnly, page],
  );

  // T1/T2 列表：只显示 API 返回的本人任务（后端按当前用户过滤）
  const listQuery = useQuery({
    queryKey: [isWriting ? 'textbook-my-writing-tasks' : 'textbook-my-review-tasks', query],
    queryFn: () => (isWriting ? pageMyWritingTasks(query) : pageMyReviewTasks(query)),
  });

  // T3/T4 统计卡：当前用户全量口径，不受 status/keyword/overdueOnly/分页影响（§6.2）
  const summaryQuery = useQuery<WritingTaskSummaryVO | ReviewTaskSummaryVO>({
    queryKey: [isWriting ? 'textbook-writing-summary' : 'textbook-review-summary'],
    queryFn: () => (isWriting ? getWritingTaskSummary() : getReviewTaskSummary()),
  });

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;
  const cards = isWriting
    ? writingCards(summaryQuery.data as WritingTaskSummaryVO | undefined)
    : reviewCards(summaryQuery.data as ReviewTaskSummaryVO | undefined);

  const goWorkbench = (task: TextbookTaskVO) => {
    navigateTo(`/textbook/${isWriting ? 'write' : 'review'}/${task.chapterId}`);
  };

  /** 主笔操作按状态显示（§10.4）；审定人按 submittedCount 显示 去审定/查看 */
  const renderAction = (_: unknown, task: TextbookTaskVO) => {
    if (isWriting) {
      switch (task.taskStatus) {
        case 'assigned':
          return (
            <Button size="small" type="link" onClick={() => goWorkbench(task)}>
              开始编写
            </Button>
          );
        case 'writing':
          return (
            <Button size="small" type="link" onClick={() => goWorkbench(task)}>
              继续编写
            </Button>
          );
        case 'returned':
          return (
            <Button size="small" type="link" danger onClick={() => goWorkbench(task)}>
              修改打回
            </Button>
          );
        case 'reviewing':
          return (
            <Button size="small" type="link" onClick={() => goWorkbench(task)}>
              查看提交
            </Button>
          );
        default:
          return (
            <Button size="small" type="link" onClick={() => goWorkbench(task)}>
              查看
            </Button>
          );
        }
    }
    return (
      <Button size="small" type="link" onClick={() => goWorkbench(task)}>
        {task.submittedCount > 0 ? '去审定' : '查看'}
      </Button>
    );
  };

  const columns: ColumnsType<TextbookTaskVO> = [
    {
      title: '教材',
      render: (_, t) => (
        <Space size={4}>
          <span>{t.workTypeTitle}</span>
          <span style={{ color: '#999' }}>
            {t.direction} · {textbookLevelLabel(t.levelCode)}
          </span>
        </Space>
      ),
    },
    {
      title: '章',
      render: (_, t) => `${t.chapterSourceNo ? `${t.chapterSourceNo} ` : ''}${t.chapterTitle}`,
    },
    {
      title: '截止',
      dataIndex: 'dueDate',
      width: 110,
      render: (v: string | null, t) => (
        <Space size={4}>
          <span>{v ?? '不限'}</span>
          <OverdueMark overdue={t.overdue} />
        </Space>
      ),
      // 逾期排序优先由后端默认排序保证（§6.2）
    },
    {
      title: '字数进度',
      width: 130,
      render: (_, t) => `${t.actualWords}/${t.targetWords ?? '-'}`,
    },
    {
      title: '节进度',
      width: 150,
      render: (_, t) =>
        t.sectionCount > 0 ? <SectionProgress approved={t.approvedCount} total={t.sectionCount} /> : '-',
    },
    {
      title: '状态',
      width: 90,
      render: (_, t) => (
        <Space size={4} wrap>
          <TaskStatusTag status={t.taskStatus} />
          {!isWriting && t.submittedCount > 0 && <Tag color="purple">{t.submittedCount} 节待审定</Tag>}
        </Space>
      ),
    },
    {
      title: '最近更新',
      dataIndex: 'latestSectionUpdatedAt',
      width: 150,
      render: (v: string | null) => v ?? '-',
    },
    { title: '操作', width: 110, fixed: 'right', render: renderAction },
  ];

  const emptyNode = total === 0 && !listQuery.isLoading ? (
    <Empty
      description={
        keyword || overdueOnly || (statusFilter && statusFilter !== 'all')
          ? '当前筛选无结果'
          : isWriting
            ? '还没有分配编写任务'
            : '还没有分配审定任务'
      }
    />
  ) : undefined;

  return (
    <Card
      title={isWriting ? '我的编写任务' : '我的审定任务'}
      extra={
        <Space wrap>
          <Input.Search
            style={{ width: 220 }}
            placeholder="教材/章名称"
            allowClear
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setPage(1);
            }}
          />
          <Select
            style={{ width: 120 }}
            placeholder="状态"
            allowClear
            value={statusFilter}
            options={isWriting ? WRITING_STATUS_OPTIONS : REVIEW_STATUS_OPTIONS}
            onChange={(v) => {
              setStatusFilter(v);
              setPage(1);
            }}
          />
          <Tooltip title="只看截止日期已过且未全部通过的章">
            <Button
              type={overdueOnly ? 'primary' : 'default'}
              danger={overdueOnly}
              onClick={() => {
                setOverdueOnly(!overdueOnly);
                setPage(1);
              }}
            >
              只看逾期
            </Button>
          </Tooltip>
        </Space>
      }
    >
      <Space size={12} wrap style={{ marginBottom: 16 }}>
        {cards.map((card) => (
          <Card
            key={card.key}
            size="small"
            hoverable={card.filterValue != null}
            style={{ minWidth: 120, cursor: card.filterValue != null ? 'pointer' : 'default' }}
            onClick={() => {
              if (card.filterValue == null) return;
              const next = statusFilter === card.filterValue ? undefined : card.filterValue;
              setStatusFilter(next);
              setPage(1);
            }}
          >
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {card.label}
            </Typography.Text>
            <div>
              <Typography.Text strong style={{ fontSize: 24 }} data-testid={`summary-${card.key}`}>
                {card.value}
              </Typography.Text>
            </div>
          </Card>
        ))}
      </Space>
      <Typography.Paragraph type="secondary" style={{ fontSize: 12 }}>
        统计卡为当前用户全部有效任务口径（各口径可能相交，不求和等于总数），不受筛选与分页影响。
      </Typography.Paragraph>

      <Table<TextbookTaskVO>
        rowKey="chapterId"
        size="middle"
        loading={listQuery.isLoading}
        dataSource={items}
        columns={columns}
        locale={{ emptyText: emptyNode }}
        pagination={{
          current: page,
          pageSize,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => setPage(p),
        }}
        scroll={{ x: 1100 }}
      />
    </Card>
  );
}
