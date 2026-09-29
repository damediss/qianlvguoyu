import { ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Drawer,
  Result,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMemo, useState } from 'react';
import {
  bindCompetitionPracticalPaper,
  listCompetitionEventWorkTypeLevels,
  listCompetitionPracticalPaperBindings,
  listCompetitionPracticalAssessmentSheets,
} from '../../api/competition';
import { listCompetitionMakeExamSnapshotsForScope } from '../../api/competitionQuestion';
import { useAuthStore } from '../../stores/authStore';
import type { CompetitionId, CompetitionPracticalAssessmentSheet } from '../../types/competition';
import type { CompetitionMakeExamSnapshot } from '../../types/competitionQuestion';
import { CompetitionRefereeAssignmentPanel } from './CompetitionRefereeAssignmentPanel';
import { CompetitionPracticalExecutionConfigPanel } from './CompetitionPracticalExecutionConfigPanel';

function idValue(value?: CompetitionId | null) {
  return value === undefined || value === null ? '' : String(value);
}

function errorText(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

function plainText(value?: string | null) {
  return (value ?? '').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
}

type AssessmentScoreItem = {
  itemNo: number;
  itemName?: string;
  elementHtml?: string;
  criterion?: string;
  standardHtml?: string;
  maxScore?: number;
  maxDeduction?: number | null;
  deductionStep?: number | null;
};

export interface CompetitionPracticalConfigPageProps {
  eventId: CompetitionId;
  stageId: CompetitionId;
  stageName?: string;
  eventWorkTypeId: CompetitionId;
  eventWorkTypeName?: string;
  eventWorkTypeLevelCode?: string;
  eventWorkTypeLevelName?: string;
  initialTab?: 'paper' | 'assessment' | 'referee' | 'runtime';
}

/**
 * Practical paper configuration opened from a stage node's context menu.
 *
 * Binding a published practical paper also creates its immutable assessment
 * sheet snapshot. The second tab exposes both the summary and per-question
 * scoring tables used by the console and referee devices.
 */
export function CompetitionPracticalConfigPage({
  eventId,
  stageId,
  stageName,
  eventWorkTypeId,
  eventWorkTypeName,
  eventWorkTypeLevelCode,
  eventWorkTypeLevelName,
  initialTab = 'paper',
}: CompetitionPracticalConfigPageProps) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const queryClient = useQueryClient();
  const canViewPaper = permissions.includes('competition:make-plan:view')
    || permissions.includes('competition:theory-paper:view');
  const canViewReferee = permissions.includes('competition:referee-assignment:view');
  const canViewStagePlan = permissions.includes('competition:stage-plan:view');
  const canManageStagePlan = permissions.includes('competition:stage-plan:manage');
  const canViewAssessment = permissions.includes('competition:practical-assessment:view')
    || canViewStagePlan;
  const [previewSheet, setPreviewSheet] = useState<CompetitionPracticalAssessmentSheet | null>(null);

  const levelsQuery = useQuery({
    queryKey: ['competition-practical-config-levels', eventId, eventWorkTypeId],
    queryFn: () => listCompetitionEventWorkTypeLevels(eventId, eventWorkTypeId),
    enabled: (canViewPaper || canViewReferee || canViewStagePlan) && Boolean(eventWorkTypeId),
  });
  const selectedLevel = levelsQuery.data?.find((level) => level.levelCode === eventWorkTypeLevelCode);
  const eventWorkTypeLevelId = idValue(selectedLevel?.id);
  const scopeReady = Boolean(stageId && eventWorkTypeId && eventWorkTypeLevelId);

  const snapshotsQuery = useQuery({
    queryKey: ['competition-practical-config-snapshots', eventId, eventWorkTypeId, eventWorkTypeLevelId],
    queryFn: () => listCompetitionMakeExamSnapshotsForScope(eventId, {
      eventWorkTypeId,
      eventWorkTypeLevelId,
      contentType: 'practical',
    }),
    enabled: canViewPaper && scopeReady,
  });

  const bindingsQuery = useQuery({
    queryKey: ['competition-practical-paper-bindings', stageId, eventWorkTypeId, eventWorkTypeLevelId],
    queryFn: () => listCompetitionPracticalPaperBindings(stageId, {
      eventWorkTypeId,
      eventWorkTypeLevelId,
    }),
    enabled: canViewStagePlan && scopeReady,
  });

  const assessmentSheetsQuery = useQuery({
    queryKey: ['competition-practical-assessment-sheets', stageId, eventWorkTypeId, eventWorkTypeLevelId],
    queryFn: () => listCompetitionPracticalAssessmentSheets(stageId, {
      eventWorkTypeId,
      eventWorkTypeLevelId,
    }),
    enabled: canViewAssessment && scopeReady,
  });

  const practicalSnapshots = useMemo(
    () => (snapshotsQuery.data ?? []).filter(
      (snapshot) => snapshot.status === 'published' && snapshot.questionCount > 0,
    ),
    [snapshotsQuery.data],
  );
  const boundPaperIds = useMemo(
    () => new Set((bindingsQuery.data ?? []).map((binding) => idValue(binding.paperSnapshotId))),
    [bindingsQuery.data],
  );

  const bindMutation = useMutation({
    mutationFn: (paperSnapshotId: CompetitionId) => bindCompetitionPracticalPaper(stageId, {
      eventWorkTypeId,
      eventWorkTypeLevelId,
      paperSnapshotId,
    }),
    onSuccess: async () => {
      message.success('实操试卷已绑定到当前赛段');
      await queryClient.invalidateQueries({
        queryKey: ['competition-practical-paper-bindings', stageId, eventWorkTypeId, eventWorkTypeLevelId],
      });
      await queryClient.invalidateQueries({
        queryKey: ['competition-practical-assessment-sheets', stageId, eventWorkTypeId, eventWorkTypeLevelId],
      });
    },
    onError: (error) => message.error(errorText(error, '实操试卷绑定失败，请刷新后重试')),
  });

  const bindPaper = (snapshotId: CompetitionId) => {
    if (!canManageStagePlan) {
      message.warning('暂无赛段编排管理权限');
      return;
    }
    if (!scopeReady) {
      message.warning('当前工种等级尚未解析，暂时不能绑定实操试卷');
      return;
    }
    if (boundPaperIds.has(idValue(snapshotId))) {
      message.info('该试卷已绑定到当前赛段');
      return;
    }
    bindMutation.mutate(snapshotId);
  };

  if ((!canViewPaper || !canViewStagePlan) && !canViewReferee) {
    return (
      <Result
        status="403"
        title="暂无实操配置查看权限"
        subTitle="请联系管理员申请赛段编排、实操试卷或裁判配置权限。"
      />
    );
  }

  return (
    <div data-testid="competition-practical-config-page">
      <Card bordered={false} className="competition-filter-card competition-filter-card--workspace">
        <div className="competition-filter-card__head">
          <div>
            <Typography.Title level={4}>实操配置</Typography.Title>
            <Typography.Text type="secondary">
              绑定已发布实操试卷时自动生成考评表；考评表包含试题总评分表及每道题的评分明细表。
            </Typography.Text>
          </div>
          <Tag color="orange">实操考评</Tag>
        </div>
        <Space wrap className="competition-filter-card__body">
          <Tag>赛段：{stageName || idValue(stageId)}</Tag>
          <Tag>工种：{eventWorkTypeName || idValue(eventWorkTypeId)}</Tag>
          <Tag>
            等级：{eventWorkTypeLevelName
              ? `${eventWorkTypeLevelName}（${eventWorkTypeLevelCode}）`
              : eventWorkTypeLevelCode || '未选择'}
          </Tag>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              void snapshotsQuery.refetch();
              void bindingsQuery.refetch();
              void assessmentSheetsQuery.refetch();
            }}
          >刷新</Button>
        </Space>
      </Card>

      {!eventWorkTypeLevelId && (
        <Alert
          style={{ marginTop: 16 }}
          type="warning"
          showIcon
          message="当前工种等级尚未解析，暂时不能加载实操试卷。"
        />
      )}

      <Tabs
        style={{ marginTop: 16 }}
        defaultActiveKey={initialTab}
        items={[
          ...(canViewPaper && canViewStagePlan ? [{
            key: 'paper',
            label: '试卷绑定',
            children: <Card size="small" title="实操试卷绑定">
        <Alert
          style={{ marginBottom: 12 }}
          type="info"
          showIcon
          message="点击试卷即可绑定"
          description="仅展示当前赛事工种等级下已有题目的已发布实操组卷快照；绑定只建立赛段关联，不依赖执行方案、绑定场次或赛事评分规则。"
        />
        <Table<CompetitionMakeExamSnapshot>
          rowKey={(row) => idValue(row.id)}
          size="small"
          pagination={false}
          loading={snapshotsQuery.isLoading || snapshotsQuery.isFetching
            || bindingsQuery.isLoading || bindingsQuery.isFetching || bindMutation.isPending}
          dataSource={practicalSnapshots}
          onRow={(record) => ({
            onClick: () => bindPaper(record.id),
            style: canManageStagePlan && !bindMutation.isPending
              ? { cursor: 'pointer' }
              : undefined,
          })}
          locale={{ emptyText: '当前范围暂无已发布实操组卷快照' }}
          columns={[
            {
              title: '试卷',
              dataIndex: 'paperCode',
              width: 90,
              render: (value: string) => <Tag color="orange">{value} 卷</Tag>,
            },
            { title: '版本', dataIndex: 'versionNo', width: 80, render: (value: number) => `v${value}` },
            { title: '组卷计划', dataIndex: 'title', ellipsis: true },
            { title: '题量', dataIndex: 'questionCount', width: 80 },
            { title: '总分', dataIndex: 'totalScore', width: 80 },
            {
              title: '时长',
              dataIndex: 'durationMinutes',
              width: 90,
              render: (value: number) => `${value} 分钟`,
            },
            {
              title: '绑定状态',
              width: 120,
              render: (_, row) => boundPaperIds.has(idValue(row.id))
                ? <Tag color="success">已绑定</Tag>
                : <Tag>{canManageStagePlan ? '点击绑定' : '只读'}</Tag>,
            },
          ]}
        />
            </Card>,
          }] : []),
          ...(canViewAssessment ? [{
            key: 'assessment',
            label: `考评表配置（${assessmentSheetsQuery.data?.length ?? 0}）`,
            children: <Card size="small" title="考评表配置">
              <Alert
                style={{ marginBottom: 12 }}
                type="success"
                showIcon
                message="考评表随试卷绑定自动生成"
                description="考评表使用已发布试卷题目快照，正式比赛初始化后仍保持版本不变。点击查看可核对总评分表和逐题评分表。"
              />
              <Table<CompetitionPracticalAssessmentSheet>
                rowKey={(row) => idValue(row.id)}
                size="small"
                pagination={false}
                loading={assessmentSheetsQuery.isLoading || assessmentSheetsQuery.isFetching}
                dataSource={assessmentSheetsQuery.data ?? []}
                locale={{ emptyText: '尚未生成考评表，请先在“试卷绑定”中绑定实操试卷' }}
                columns={[
                  { title: '考评表编号', dataIndex: 'sheetCode', width: 170 },
                  { title: '考评表名称', dataIndex: 'title', ellipsis: true },
                  { title: '试卷', dataIndex: 'paperCode', width: 90 },
                  { title: '版本', dataIndex: 'versionNo', width: 70, render: (value: number) => `v${value}` },
                  { title: '题量', dataIndex: 'questionCount', width: 70 },
                  { title: '总分', dataIndex: 'totalScore', width: 70 },
                  { title: '状态', dataIndex: 'status', width: 90, render: () => <Tag color="success">已发布</Tag> },
                  {
                    title: '操作',
                    width: 90,
                    render: (_, row) => <Button type="link" onClick={() => setPreviewSheet(row)}>查看</Button>,
                  },
                ]}
              />
            </Card>,
          }] : []),
          ...(canViewStagePlan ? [{
            key: 'runtime',
            label: '运行规则',
            children: <Card size="small" title="赛段实操运行规则">
              <Alert
                style={{ marginBottom: 12 }}
                type="info"
                showIcon
                message="这里配置本赛段如何考试；评分内容来自已发布的实操评分方案。"
                description="运行规则会随赛段执行计划固化，已发布计划使用自己的配置快照。"
              />
              <CompetitionPracticalExecutionConfigPanel
                eventId={eventId}
                stageId={stageId}
                stageName={stageName}
                eventWorkTypeId={eventWorkTypeId}
                eventWorkTypeLevelId={eventWorkTypeLevelId || undefined}
                editable={canManageStagePlan}
              />
            </Card>,
          }] : []),
          ...(canViewReferee ? [{
            key: 'referee',
            label: '裁判配置',
            children: <CompetitionRefereeAssignmentPanel stageId={stageId} />,
          }] : []),
        ]}
      />
      <Drawer
        width={960}
        open={Boolean(previewSheet)}
        title={previewSheet ? `${previewSheet.title}（${previewSheet.sheetCode}）` : '考评表'}
        onClose={() => setPreviewSheet(null)}
      >
        {previewSheet && <AssessmentSheetPreview sheet={previewSheet} />}
      </Drawer>
    </div>
  );
}

function AssessmentSheetPreview({ sheet }: { sheet: CompetitionPracticalAssessmentSheet }) {
  const summary = sheet.sheet.summary ?? [];
  const questions = sheet.sheet.questions ?? [];
  return <Space direction="vertical" size="large" style={{ width: '100%' }}>
    <div>
      <Typography.Title level={5}>试题总评分表</Typography.Title>
      <Table
        rowKey={(row) => idValue(row.paperQuestionId)}
        size="small"
        pagination={false}
        dataSource={summary}
        columns={[
          { title: '序号', dataIndex: 'sortOrder', width: 70 },
          { title: '试题名称', dataIndex: 'title' },
          { title: '配分', dataIndex: 'score', width: 80 },
          { title: '得分', width: 80, render: () => '—' },
          { title: '权重', dataIndex: 'weight', width: 80, render: (value: number) => `${value}%` },
          { title: '最后得分', width: 100, render: () => '—' },
          { title: '备注', width: 100, render: () => '—' },
        ]}
      />
    </div>
    {questions.map((question) => <Card
      key={idValue(question.paperQuestionId)}
      size="small"
      title={`第 ${question.sortOrder} 题 · ${question.title}`}
      extra={<Tag color="orange">配分 {String(question.score)}</Tag>}
    >
      <Typography.Text type="secondary">逐题评分表（现场检测、扣分、得分和备注在裁判端填写）</Typography.Text>
      <Table<AssessmentScoreItem>
        style={{ marginTop: 12 }}
        rowKey={(row) => String(row.itemNo)}
        size="small"
        pagination={false}
        scroll={{ x: 1050 }}
        dataSource={Array.isArray(question.scoreItems) ? question.scoreItems as AssessmentScoreItem[] : []}
        columns={[
          { title: '序号', dataIndex: 'itemNo', width: 65 },
          { title: '考试内容', width: 150, render: () => question.title },
          { title: '评分要素', width: 180, render: (_, row) => plainText(row.itemName || row.elementHtml) || '-' },
          { title: '配分', dataIndex: 'maxScore', width: 70 },
          { title: '评分标准', width: 240, render: (_, row) => plainText(row.criterion || row.standardHtml) || '-' },
          { title: '最大扣分', dataIndex: 'maxDeduction', width: 90, render: (value) => value ?? '-' },
          { title: '步长', dataIndex: 'deductionStep', width: 70, render: (value) => value ?? '-' },
          { title: '检测结果', width: 90, render: () => '—' },
          { title: '扣分', width: 70, render: () => '—' },
          { title: '得分', width: 70, render: () => '—' },
          { title: '备注', width: 90, render: () => '—' },
        ]}
      />
    </Card>)}
  </Space>;
}
