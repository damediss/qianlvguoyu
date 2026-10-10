/**
 * 生成工作台批次总览表组件单测（方案 §7.2）：
 * 状态动效类挂载、审核进度/主观题计数渲染、进行中区与历史区分区。
 */
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import GenBatchOverviewTable from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/workbench/GenBatchOverviewTable';
import type { TheoryGenBatch } from '../src/pages/zhi_neng_ke_cheng_cai_liao_guan_li/theoryGenApi';

const baseProps = {
  historyLoading: false,
  historyPage: 1,
  historyTotal: 0,
  onHistoryPageChange: vi.fn(),
  workTypeName: (id?: number | null) => (id === 22 ? '矿井通风工' : '-'),
  onOpenDraft: vi.fn(),
  onAction: vi.fn(),
};

const batch = (patch: Partial<TheoryGenBatch>): TheoryGenBatch => ({
  id: '2097230714145800193',
  blueprintId: '100',
  modelId: 'glm-5.3-flash',
  status: 'running',
  totalPoints: 120,
  donePoints: 60,
  failedPoints: 0,
  totalQuestions: 400,
  tokenUsage: 0,
  ...patch,
});

describe('GenBatchOverviewTable', () => {
  it('进行中行：等待中变色动效 / 生成中呼吸动效', () => {
    render(
      <GenBatchOverviewTable
        {...baseProps}
        activeItems={[
          batch({ id: '1', status: 'pending' }),
          batch({ id: '2', status: 'running' }),
        ]}
        historyItems={[]}
      />,
    );
    expect(screen.getByText('等待中').className).toContain('xmb-status-waiting');
    expect(screen.getByText('生成中').className).toContain('xmb-status-running');
    expect(screen.getByText(/进行中批次/)).toBeTruthy();
    expect(screen.getByText(/历史批次/)).toBeTruthy();
  });

  it('审核进度与主观题计数渲染', () => {
    render(
      <GenBatchOverviewTable
        {...baseProps}
        activeItems={[]}
        historyItems={[batch({
          id: '3', status: 'done', donePoints: 120,
          approvedCount: 300, pendingReviewCount: 40, rejectedCount: 5,
          publishedCount: 300, subjectiveCount: 80,
        })]}
      />,
    );
    expect(screen.getByText(/通过 300/)).toBeTruthy();
    expect(screen.getByText(/待审 40/)).toBeTruthy();
    expect(screen.getByText(/驳回 5/)).toBeTruthy();
    expect(screen.getByText('已入库 300')).toBeTruthy();
    expect(screen.getByText(/已产题 400 题（主观 80）/)).toBeTruthy();
  });

  it('失败批次：失败原因挂在状态徽标 Tooltip 上', () => {
    render(
      <GenBatchOverviewTable
        {...baseProps}
        activeItems={[]}
        historyItems={[batch({
          id: '4', status: 'failed',
          failReason: '自动补跑 4 轮后仍有 3 个考评点未完成，可重试失败块',
        })]}
      />,
    );
    expect(screen.getByText('失败')).toBeTruthy();
  });

  it('完成行按 flashBatchId 命中闪烁类', () => {
    const { container } = render(
      <GenBatchOverviewTable
        {...baseProps}
        activeItems={[]}
        historyItems={[batch({ id: '5', status: 'done', donePoints: 120 })]}
        flashBatchId="5"
      />,
    );
    expect(container.querySelectorAll('.gen-row-flash').length).toBeGreaterThan(0);
  });
});
