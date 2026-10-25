/**
 * 教材编写协同共享展示组件 —— 《二期编写协同实施设计》§10.1（TextbookTaskMeta.tsx）
 * 状态 Tag、进度、逾期等纯展示组件；一期工程/大纲页与新页面共用同一份状态文案（ubiquitous language）。
 */
import { Tag, Tooltip } from 'antd';
import type { TextbookProjectStatus } from '../../types/textbook';
import type {
  TextbookSectionContentStatus,
  TextbookTaskStatus,
} from '../../types/textbookCollaboration';

// ------------------------------------------------------------------
// 工程状态（§3.1：reviewed 文案为「大纲已评审」，与节审定区分）
// ------------------------------------------------------------------

export const TEXTBOOK_PROJECT_STATUS_META: Record<TextbookProjectStatus, { text: string; color: string }> = {
  parsed: { text: '已解析', color: 'default' },
  outlined: { text: '大纲已生成', color: 'blue' },
  reviewed: { text: '大纲已评审', color: 'cyan' },
  writing: { text: '编写中', color: 'geekblue' },
  done: { text: '编写完成', color: 'green' },
};

export function ProjectStatusTag({ status }: { status: string }) {
  const meta = TEXTBOOK_PROJECT_STATUS_META[status as TextbookProjectStatus];
  return <Tag color={meta?.color}>{meta?.text || status}</Tag>;
}

// ------------------------------------------------------------------
// 章任务状态（§3.3：冗余查询字段，正文状态才是事实源）
// ------------------------------------------------------------------

const TASK_STATUS_META: Record<TextbookTaskStatus, { text: string; color: string }> = {
  assigned: { text: '待开始', color: 'default' },
  writing: { text: '编写中', color: 'processing' },
  reviewing: { text: '待审定', color: 'purple' },
  returned: { text: '有打回', color: 'error' },
  approved: { text: '已完成', color: 'success' },
};

export function TaskStatusTag({ status }: { status: TextbookTaskStatus | null | undefined }) {
  if (!status) return <Tag>未分配</Tag>;
  const meta = TASK_STATUS_META[status];
  return <Tag color={meta?.color}>{meta?.text || status}</Tag>;
}

// ------------------------------------------------------------------
// 节正文状态（§3.2）
// ------------------------------------------------------------------

const CONTENT_STATUS_META: Record<TextbookSectionContentStatus, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  submitted: { text: '待审定', color: 'processing' },
  approved: { text: '已通过', color: 'success' },
  returned: { text: '已打回', color: 'error' },
};

export function contentStatusText(status: TextbookSectionContentStatus | null | undefined): string {
  if (!status) return '未开始';
  return CONTENT_STATUS_META[status]?.text || status;
}

export function ContentStatusTag({ status }: { status: TextbookSectionContentStatus | null | undefined }) {
  if (!status) return <Tag>未开始</Tag>;
  const meta = CONTENT_STATUS_META[status];
  return <Tag color={meta?.color}>{meta?.text || status}</Tag>;
}

// ------------------------------------------------------------------
// 审定动作（W6 时间线）
// ------------------------------------------------------------------

const REVIEW_ACTION_META: Record<string, string> = {
  submit: '提交审定',
  approve: '通过',
  return: '打回',
  reopen: '撤销通过',
};

export function reviewActionText(action: string): string {
  return REVIEW_ACTION_META[action] || action;
}

// ------------------------------------------------------------------
// 进度 / 逾期
// ------------------------------------------------------------------

/** 节进度：approved/total + 进度条；total=0 显示「未分配」外由调用方处理 */
export function SectionProgress({ approved, total }: { approved: number; total: number }) {
  const percent = total > 0 ? Math.round((approved / total) * 100) : 0;
  return (
    <Tooltip title={`已通过 ${approved} / 共 ${total} 节`}>
      <span style={{ whiteSpace: 'nowrap' }}>
        <span style={{ marginRight: 6 }}>{approved}/{total}</span>
        <span
          aria-label={`节进度 ${approved}/${total}`}
          style={{
            display: 'inline-block',
            width: 64,
            height: 6,
            background: '#f0f0f0',
            borderRadius: 3,
            overflow: 'hidden',
            verticalAlign: 'middle',
          }}
        >
          <span style={{ display: 'block', height: '100%', width: `${percent}%`, background: '#52c41a' }} />
        </span>
      </span>
    </Tooltip>
  );
}

/** 逾期红字（§3.4：dueDate < 今天 && 状态未通过，不改状态、不阻止编写） */
export function OverdueMark({ overdue }: { overdue: boolean }) {
  if (!overdue) return null;
  return <span style={{ color: '#cf1322', fontWeight: 600 }}>已逾期</span>;
}

/** 是否逾期：截止日早于今天且章未完成（页面展示口径，服务端 TaskVO.overdue 优先） */
export function isTaskOverdue(dueDate: string | null | undefined, taskStatus: TextbookTaskStatus | null | undefined): boolean {
  if (!dueDate || taskStatus === 'approved') return false;
  const today = new Date();
  const due = new Date(`${dueDate}T00:00:00`);
  return due.getTime() < new Date(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}T00:00:00`).getTime();
}
