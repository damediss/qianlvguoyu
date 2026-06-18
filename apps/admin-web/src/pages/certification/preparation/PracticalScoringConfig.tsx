import {
  DownloadOutlined,
  LinkOutlined,
  ReloadOutlined,
  RollbackOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Empty,
  Input,
  InputNumber,
  Modal,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import './practical-scoring-expand.css';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  listAllPreparationCandidates,
  listPreparationAssessorGroups,
} from '../../../api/certificationPreparation';
import {
  adjustPracticalScoringTaskScore,
  confirmPracticalCandidate,
  exportPracticalSummary,
  getPracticalScoringTaskDetail,
  getPracticalScoringProgress,
  getPracticalScoringSetup,
  importPracticalPaper,
  linkPracticalCandidateTemplate,
  returnPracticalScoringTask,
} from '../../../api/practicalScoring';
import { parseContentDispositionFilename } from '../../../utils/download';
import type {
  PreparationAssessorGroup,
  PreparationCandidate,
} from '../../../types/certificationPreparation';
import type {
  PracticalCandidateProgress,
  PracticalPaperOption,
  PracticalScoreItem,
  PracticalScopeOption,
  PracticalTaskProgressItem,
} from '../../../types/practicalScoring';

interface Props {
  taskId?: string;
  showSetup?: boolean;
  showTemplate?: boolean;
  showProgress?: boolean;
}

interface CandidateLinkValues {
  candidateId: string;
  scopeId?: string;
  paperId: string;
  groupId: string;
  workTypeId?: number | null;
}

export function PracticalScoringConfig({
  taskId,
  showSetup = true,
  showTemplate = true,
  showProgress = true,
}: Props) {
  const queryClient = useQueryClient();
  const [selectedGroupId, setSelectedGroupId] = useState<string>();
  const [selectedScopeId, setSelectedScopeId] = useState<string>();
  const [selectedPaperByCandidate, setSelectedPaperByCandidate] = useState<Record<string, string | undefined>>({});
  const [rowErrors, setRowErrors] = useState<Record<string, string | undefined>>({});
  const [returningTask, setReturningTask] = useState<PracticalTaskProgressItem>();
  const [returnReason, setReturnReason] = useState('');
  const [adjustingTask, setAdjustingTask] = useState<PracticalTaskProgressItem>();
  const [adjustDeductions, setAdjustDeductions] = useState<Record<number, number>>({});
  const [adjustRemarks, setAdjustRemarks] = useState<Record<number, string>>({});
  const [adjustNote, setAdjustNote] = useState('');
  const [adjustReason, setAdjustReason] = useState('');
  const [selectedSummaryCandidateIds, setSelectedSummaryCandidateIds] = useState<string[]>([]);
  const showConfig = showSetup || showTemplate;

  const setupQuery = useQuery({
    queryKey: ['practical-scoring-setup', taskId],
    queryFn: () => getPracticalScoringSetup(taskId!),
    enabled: Boolean(taskId && showConfig),
  });
  const progressQuery = useQuery({
    queryKey: ['practical-scoring-progress', taskId],
    queryFn: () => getPracticalScoringProgress(taskId!),
    enabled: Boolean(taskId && (showConfig || showProgress)),
    refetchInterval: showProgress ? 5000 : false,
  });
  const adjustingTaskQuery = useQuery({
    queryKey: ['practical-scoring-admin-task', adjustingTask?.assessorTaskId],
    queryFn: () => getPracticalScoringTaskDetail(adjustingTask!.assessorTaskId),
    enabled: Boolean(adjustingTask),
  });
  const groupsQuery = useQuery({
    queryKey: ['certification-preparation-assessor-groups', taskId],
    queryFn: () => listPreparationAssessorGroups(taskId!),
    enabled: Boolean(taskId && showConfig),
  });

  const setup = setupQuery.data;
  const selectedScope = useMemo(
    () => setup?.scopes.find((scope) => scope.id === selectedScopeId),
    [selectedScopeId, setup?.scopes],
  );
  const candidateQuery = useQuery({
    queryKey: ['practical-scoring-candidates', taskId, selectedScopeId, selectedScope?.levelCode],
    queryFn: () => listAllPreparationCandidates(taskId!, {
      status: 'active',
      documentComplete: true,
      ...(selectedScope?.levelCode ? { levelCode: selectedScope.levelCode } : {}),
    }),
    enabled: Boolean(taskId && showConfig),
  });

  const groupDetailById = useMemo(
    () => new Map((groupsQuery.data || []).map((group) => [group.id, group])),
    [groupsQuery.data],
  );
  const configByCandidate = useMemo(
    () => new Map((setup?.configs || [])
      .filter((config) => config.candidateId)
      .map((config) => [config.candidateId!, config])),
    [setup?.configs],
  );
  const progressByCandidate = useMemo(
    () => new Map((progressQuery.data?.candidates || []).map((candidate) => [candidate.candidateId, candidate])),
    [progressQuery.data?.candidates],
  );
  const abnormalCandidates = useMemo(
    () => (progressQuery.data?.candidates || []).filter((candidate) => candidate.status === 'review_required'),
    [progressQuery.data?.candidates],
  );
  const assessorGroupsByCandidate = useMemo(() => {
    const groups = new Map<string, Map<string, { id: string; name: string; tasks: PracticalTaskProgressItem[] }>>();
    for (const task of progressQuery.data?.scoringTasks || []) {
      let byCandidate = groups.get(task.candidateId);
      if (!byCandidate) {
        byCandidate = new Map();
        groups.set(task.candidateId, byCandidate);
      }
      const byAssessor = byCandidate.get(task.assessorId);
      if (byAssessor) {
        byAssessor.tasks.push(task);
      } else {
        byCandidate.set(task.assessorId, { id: task.assessorId, name: task.assessorName, tasks: [task] });
      }
    }
    return groups;
  }, [progressQuery.data?.scoringTasks]);
  const candidateRows = useMemo(() => {
    const rows = candidateQuery.data?.items || [];
    if (!selectedScope) return rows;
    return rows.filter((candidate) => scopeMatchesCandidate(selectedScope, candidate));
  }, [candidateQuery.data?.items, selectedScope]);

  const refresh = () => {
    if (showConfig) {
      queryClient.invalidateQueries({ queryKey: ['practical-scoring-setup', taskId] });
      queryClient.invalidateQueries({ queryKey: ['practical-scoring-candidates', taskId] });
      queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-groups', taskId] });
    }
    if (showProgress || showConfig) {
      queryClient.invalidateQueries({ queryKey: ['practical-scoring-progress', taskId] });
    }
  };

  useEffect(() => {
    const detail = adjustingTaskQuery.data;
    if (!detail) return;
    setAdjustDeductions(Object.fromEntries(
      detail.scoreItems.map((item) => [item.itemNo, item.deductedScore || 0]),
    ));
    setAdjustRemarks(Object.fromEntries(
      detail.scoreItems.map((item) => [item.itemNo, item.remark || '']),
    ));
    setAdjustNote(detail.progressNote || '');
  }, [adjustingTaskQuery.data]);

  const adjustTotalScore = useMemo(() => {
    const detail = adjustingTaskQuery.data;
    if (!detail) return 0;
    return detail.scoreItems.reduce((sum, item) => (
      sum + Math.max(0, item.maxScore - (adjustDeductions[item.itemNo] ?? item.deductedScore ?? 0))
    ), 0);
  }, [adjustDeductions, adjustingTaskQuery.data]);

  const candidateLinkMutation = useMutation({
    mutationFn: (values: CandidateLinkValues) =>
      linkPracticalCandidateTemplate(taskId!, values.candidateId, {
        scopeId: values.scopeId,
        paperId: values.paperId,
        groupId: values.groupId,
        workTypeId: values.workTypeId,
      }),
    onSuccess: (result, values) => {
      message.success(`已生成 ${result.createdTasks} 个评分任务，已存在 ${result.existingTasks} 个`);
      setRowErrors((prev) => ({ ...prev, [values.candidateId]: undefined }));
      refresh();
    },
    onError: (error: unknown, values) => {
      const reason =
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '配置实操试卷失败，请稍后重试';
      setRowErrors((prev) => ({ ...prev, [values.candidateId]: reason }));
      message.error(reason);
    },
  });
  const returnMutation = useMutation({
    mutationFn: () => returnPracticalScoringTask(returningTask!.assessorTaskId, returnReason),
    onSuccess: () => {
      message.success('评分任务已退回原考评员');
      setReturningTask(undefined);
      setReturnReason('');
      refresh();
    },
  });
  const adjustMutation = useMutation({
    mutationFn: () => {
      const detail = adjustingTaskQuery.data!;
      return adjustPracticalScoringTaskScore(detail.task.id, {
        version: detail.task.version,
        reason: adjustReason,
        note: adjustNote || undefined,
        items: detail.scoreItems.map((item) => ({
          itemNo: item.itemNo,
          deductedScore: adjustDeductions[item.itemNo] ?? item.deductedScore ?? 0,
          remark: adjustRemarks[item.itemNo] || undefined,
        })),
      });
    },
    onSuccess: () => {
      message.success('评分已修改并重新提交');
      setAdjustingTask(undefined);
      setAdjustReason('');
      setAdjustNote('');
      setAdjustDeductions({});
      setAdjustRemarks({});
      refresh();
    },
  });
  const candidateConfirmMutation = useMutation({
    mutationFn: (candidateId: string) => confirmPracticalCandidate(taskId!, candidateId),
    onSuccess: (session) => {
      message.success(session.status === 'started' ? '组长已开始考试' : '开考状态已更新');
      refresh();
    },
  });
  const importPaperMutation = useMutation({
    mutationFn: (file: File) => importPracticalPaper(taskId!, file),
    onSuccess: (paper) => {
      message.success(`已导入 ${paper.title}，生成 ${paper.questions} 道实操题、${paper.scoreItems} 个评分项`);
      refresh();
    },
    onError: (error: unknown) => {
      const reason =
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '导入评分记录表失败，请检查表格格式';
      message.error(reason);
    },
  });
  const exportSummaryMutation = useMutation({
    mutationFn: () => exportPracticalSummary(taskId!, selectedSummaryCandidateIds),
    onSuccess: (resp) => {
      const filename = parseContentDispositionFilename({ headers: resp.headers as Record<string, string> })
        || `评分记录汇总表_${taskId}.xlsx`;
      const url = window.URL.createObjectURL(resp.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('评分记录汇总表已生成');
    },
  });

  if (!taskId) return <Typography.Text type="secondary">请选择认定任务。</Typography.Text>;

  const configureCandidate = (candidate: PreparationCandidate) => {
    const paperId = selectedPaperByCandidate[candidate.id] || configByCandidate.get(candidate.id)?.paperId;
    const scope = selectedScope && scopeMatchesCandidate(selectedScope, candidate)
      ? selectedScope
      : findScopeForCandidate(setup?.scopes || [], candidate);
    if (!selectedGroupId) {
      message.warning('请选择三人考评组');
      return;
    }
    if (!paperId) {
      message.warning('请选择实操试卷');
      return;
    }
    if (!scope) {
      message.warning('未找到匹配学员工种等级的任务范围');
      return;
    }
    const paper = setup?.papers.find((item) => item.id === paperId);
    candidateLinkMutation.mutate({
      candidateId: candidate.id,
      scopeId: scope.id,
      paperId,
      groupId: selectedGroupId,
      workTypeId: toNullableNumber(candidate.workTypeId || scope.workTypeId || paper?.workTypeId),
    });
  };

  const groupColumns: ColumnsType<PreparationAssessorGroup> = [
    { title: '考评组', dataIndex: 'groupName', ellipsis: true },
    { title: '组长', dataIndex: 'leaderAssessorName', width: 120, render: (value) => value || '-' },
    {
      title: '负责工种等级',
      dataIndex: 'workTypeScopes',
      render: (scopes: PreparationAssessorGroup['workTypeScopes'], row) =>
        scopes?.length
          ? scopes.map((scope) => `${scope.workTypeName || '-'} / ${scope.levelName || scope.levelCode || '-'}`).join('、')
          : row.workTypeName || '-',
    },
    {
      title: '成员',
      width: 100,
      render: (_, row) => {
        const setupGroup = setup?.groups.find((group) => group.id === row.id);
        const count = setupGroup?.memberCount || 0;
        return <Tag color={count === 3 ? 'success' : 'warning'}>{count}/3</Tag>;
      },
    },
    {
      title: '选择',
      width: 100,
      render: (_, row) => {
        const setupGroup = setup?.groups.find((group) => group.id === row.id);
        return (
          <Button
            type={selectedGroupId === row.id ? 'primary' : 'default'}
            size="small"
            disabled={(setupGroup?.memberCount || 0) !== 3}
            onClick={() => setSelectedGroupId(row.id)}
          >
            选择
          </Button>
        );
      },
    },
  ];

  const candidateColumns: ColumnsType<PreparationCandidate> = [
    { title: '学员', dataIndex: 'candidateName', width: 120, fixed: 'left' },
    { title: '证件号', dataIndex: 'maskedIdCardNo', width: 180, render: (v, row) => v || row.idCardNo || '-' },
    { title: '手机号', dataIndex: 'maskedPhone', width: 140, render: (v, row) => v || row.phone || '-' },
    { title: '工种', dataIndex: 'workTypeName', width: 120, render: (v) => v || '-' },
    { title: '等级', dataIndex: 'levelName', width: 100, render: (v, row) => v || row.levelCode || '-' },
    { title: '单位', dataIndex: 'orgName', width: 160, ellipsis: true, render: (v) => v || '-' },
    {
      title: '实操试卷',
      width: 260,
      render: (_, row) => {
        const configured = configByCandidate.get(row.id);
        const rowScope = selectedScope && scopeMatchesCandidate(selectedScope, row)
          ? selectedScope
          : findScopeForCandidate(setup?.scopes || [], row);
        return (
          <Select
            showSearch
            optionFilterProp="label"
            placeholder="请选择试卷"
            value={selectedPaperByCandidate[row.id] || configured?.paperId}
            style={{ width: '100%' }}
            options={paperOptionsForCandidate(setup?.papers || [], row, rowScope).map((paper) => ({
              value: paper.id,
              label: paper.title,
              disabled: paper.questions.length === 0 || paper.questions.some((question) => !question.templateReady),
            }))}
            onChange={(paperId) => setSelectedPaperByCandidate((prev) => ({ ...prev, [row.id]: paperId }))}
          />
        );
      },
    },
    {
      title: '配置状态',
      width: 180,
      render: (_, row) => {
        const error = rowErrors[row.id];
        const configured = configByCandidate.get(row.id);
        const progress = progressByCandidate.get(row.id);
        if (error) return <Tag color="error">配置失败</Tag>;
        if (progress?.totalTasks) return <Tag color="success">已生成评分任务</Tag>;
        if (configured) return <Tag color="processing">已配置</Tag>;
        return <Tag>未配置</Tag>;
      },
    },
    {
      title: '操作',
      width: 130,
      fixed: 'right',
      render: (_, row) => (
        <Button
          type="link"
          icon={<LinkOutlined />}
          loading={candidateLinkMutation.isPending}
          onClick={() => configureCandidate(row)}
        >
          配置试卷
        </Button>
      ),
    },
  ];

  const progressCandidateColumns: ColumnsType<PracticalCandidateProgress> = [
    { title: '考生编号', dataIndex: 'candidateNo', width: 120, render: (v) => v || '-' },
    { title: '考生', dataIndex: 'candidateName', width: 120 },
    {
      title: '评分提交',
      width: 180,
      render: (_, row) => (
        <Progress
          percent={row.totalTasks ? Math.round(row.submittedTasks * 100 / row.totalTasks) : 0}
          format={() => `${row.submittedTasks}/${row.totalTasks}`}
        />
      ),
    },
    { title: '试题完成', width: 150, render: (_, row) => `${row.completedQuestions}/${row.totalQuestions}` },
    { title: '实操总分', dataIndex: 'totalScore', width: 110, render: (v) => v ?? '待三人齐交' },
    {
      title: '开考协同',
      width: 220,
      render: (_, row) => row.startSession ? (
        <Space>
          <Tag color={row.startSession.status === 'completed' ? 'success' : row.startSession.status === 'started' ? 'processing' : 'default'}>
            {row.startSession.status === 'completed'
              ? '考试已完成'
              : row.startSession.status === 'started'
                ? '考试中'
                : `${row.startSession.candidateConfirmed ? '已选择考生' : '待组长开始'} · ${row.startSession.confirmedAssessors}/3`}
          </Tag>
          {!row.startSession.candidateConfirmed && (
            <Button
              size="small"
              loading={candidateConfirmMutation.isPending}
              onClick={(event) => {
                event.stopPropagation();
                candidateConfirmMutation.mutate(row.candidateId);
              }}
            >
              组长开始
            </Button>
          )}
        </Space>
      ) : <Tag>待生成任务</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v) => {
        if (v === 'review_required') return <Tag color="error">分差异常</Tag>;
        return <Tag color={v === 'completed' ? 'success' : 'processing'}>{v === 'completed' ? '已完成' : '评分中'}</Tag>;
      },
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {showConfig && (
        <ProCard
          title="区域一：考评组配置区"
          extra={<Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>}
        >
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Select
              showSearch
              optionFilterProp="label"
              value={selectedGroupId}
              placeholder="请选择满足三人成组条件的考评组"
              style={{ width: 'min(520px, 100%)' }}
              options={(setup?.groups || []).map((group) => {
                const detail = groupDetailById.get(group.id);
                return {
                  value: group.id,
                  disabled: group.memberCount !== 3,
                  label: `${group.groupName}（${group.memberCount}/3人${detail?.leaderAssessorName ? ` · 组长${detail.leaderAssessorName}` : ''}）`,
                };
              })}
              onChange={setSelectedGroupId}
            />
            <Table
              rowKey="id"
              size="small"
              loading={groupsQuery.isFetching || setupQuery.isFetching}
              dataSource={groupsQuery.data || []}
              columns={groupColumns}
              pagination={false}
              locale={{ emptyText: <Empty description="暂无考评组" /> }}
            />
          </Space>
        </ProCard>
      )}

      {showConfig && (
        <ProCard title="区域二：学员列表区">
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Space wrap>
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                value={selectedScopeId}
                placeholder="工种等级筛选"
                style={{ width: 260 }}
                options={(setup?.scopes || []).map((scope) => ({
                  value: scope.id,
                  label: `${scope.workTypeName || '-'} / ${scope.levelName || scope.levelCode || '-'}`,
                }))}
                onChange={setSelectedScopeId}
              />
              <Upload
                accept=".xlsx,.xls"
                showUploadList={false}
                beforeUpload={(file) => {
                  importPaperMutation.mutate(file);
                  return false;
                }}
              >
                <Button icon={<UploadOutlined />} loading={importPaperMutation.isPending}>
                  导入评分记录表
                </Button>
              </Upload>
              {selectedGroupId ? (
                <Tag color="processing">
                  已选考评组：{groupDetailById.get(selectedGroupId)?.groupName
                    || setup?.groups.find((group) => group.id === selectedGroupId)?.groupName
                    || selectedGroupId}
                </Tag>
              ) : null}
            </Space>
            <Table
              rowKey="id"
              columns={candidateColumns}
              dataSource={candidateRows}
              loading={candidateQuery.isFetching || setupQuery.isFetching || progressQuery.isFetching}
              scroll={{ x: 1310 }}
              expandable={{
                expandedRowRender: (row) => rowErrors[row.id]
                  ? <Typography.Text type="danger">{rowErrors[row.id]}</Typography.Text>
                  : null,
                rowExpandable: (row) => Boolean(rowErrors[row.id]),
              }}
              pagination={{ pageSize: 10 }}
              locale={{
                emptyText: selectedScopeId ? '当前工种等级下暂无已完成申报材料的学员' : '暂无已完成申报材料的学员',
              }}
            />
          </Space>
        </ProCard>
      )}

      {showProgress && (
        <ProCard title="评分进度">
          <Space wrap style={{ marginBottom: 16 }}>
            <Button
              icon={<DownloadOutlined />}
              disabled={selectedSummaryCandidateIds.length === 0}
              loading={exportSummaryMutation.isPending}
              onClick={() => exportSummaryMutation.mutate()}
            >
              导出汇总表
            </Button>
          </Space>
          {abnormalCandidates.length > 0 && (
            <Alert
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              message={`发现 ${abnormalCandidates.length} 名考生成绩分差异常`}
              description="请展开考生行查看标记为分差异常的试题，在对应考评员任务上点击修改分数并重新提交。偏差不超过10分后，组长端才能完成考试。"
            />
          )}
          <Space size={24} style={{ marginBottom: 16 }}>
            <Progress
              type="circle"
              size={92}
              percent={progressQuery.data?.totalTasks
                ? Math.round(progressQuery.data.submittedTasks * 100 / progressQuery.data.totalTasks)
                : 0}
            />
            <Typography.Text>
              已提交 {progressQuery.data?.submittedTasks || 0} / {progressQuery.data?.totalTasks || 0} 份，
              已完成 {progressQuery.data?.completedQuestions || 0} / {progressQuery.data?.totalQuestions || 0} 道试题
            </Typography.Text>
          </Space>
          <Table
            rowKey="candidateId"
            columns={progressCandidateColumns}
            dataSource={progressQuery.data?.candidates || []}
            pagination={false}
            scroll={{ x: 850 }}
            rowSelection={{
              selectedRowKeys: selectedSummaryCandidateIds,
              onChange: (keys) => setSelectedSummaryCandidateIds(keys.map(String)),
              getCheckboxProps: (row) => ({
                disabled: row.status !== 'completed' || row.startSession?.status !== 'completed',
              }),
            }}
            expandable={{
              expandedRowRender: (row) => (
                <AssessorBreakdown
                  assessorGroups={assessorGroupsByCandidate.get(row.candidateId)}
                  onReturnTask={setReturningTask}
                  onAdjustTask={(task) => {
                    setAdjustReason('');
                    setAdjustingTask(task);
                  }}
                />
              ),
              rowExpandable: (row) => (assessorGroupsByCandidate.get(row.candidateId)?.size ?? 0) > 0,
              expandRowByClick: true,
            }}
          />
        </ProCard>
      )}

      <Modal
        title="退回评分任务"
        open={Boolean(returningTask)}
        okButtonProps={{ danger: true, disabled: !returnReason.trim() }}
        confirmLoading={returnMutation.isPending}
        onCancel={() => setReturningTask(undefined)}
        onOk={() => returnMutation.mutate()}
      >
        <Typography.Paragraph>
          {returningTask?.candidateName} / {returningTask?.questionTitle} / {returningTask?.assessorName}
        </Typography.Paragraph>
        <Input.TextArea
          rows={4}
          value={returnReason}
          onChange={(event) => setReturnReason(event.target.value)}
          placeholder="必须填写退回原因，原成绩将立即失效"
        />
      </Modal>

      <Modal
        title="修改异常评分"
        open={Boolean(adjustingTask)}
        width={760}
        okText="保存并重新提交"
        confirmLoading={adjustMutation.isPending}
        okButtonProps={{
          disabled: !adjustingTaskQuery.data || !adjustReason.trim(),
        }}
        onCancel={() => {
          setAdjustingTask(undefined);
          setAdjustReason('');
        }}
        onOk={() => adjustMutation.mutate()}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Typography.Paragraph>
            {adjustingTask?.candidateName} / {adjustingTask?.questionTitle} / {adjustingTask?.assessorName}
          </Typography.Paragraph>
          {adjustingTask?.reviewRequired ? (
            <Alert
              type="error"
              showIcon
              message={`当前试题三人评分分差${adjustingTask.scoreDeviation ?? '-'}分，超过10分`}
            />
          ) : null}
          <Table<PracticalScoreItem>
            rowKey="itemNo"
            loading={adjustingTaskQuery.isFetching}
            dataSource={adjustingTaskQuery.data?.scoreItems || []}
            pagination={false}
            columns={[
              { title: '序号', dataIndex: 'itemNo', width: 70 },
              { title: '评分项', dataIndex: 'itemName' },
              { title: '满分', dataIndex: 'maxScore', width: 80 },
              {
                title: '扣分',
                width: 150,
                render: (_, item) => (
                  <InputNumber
                    min={0}
                    max={item.maxDeduction}
                    step={item.deductionStep}
                    value={adjustDeductions[item.itemNo] ?? item.deductedScore ?? 0}
                    onChange={(value) => setAdjustDeductions((prev) => ({
                      ...prev,
                      [item.itemNo]: Number(value || 0),
                    }))}
                  />
                ),
              },
              {
                title: '得分',
                width: 90,
                render: (_, item) => Math.max(0, item.maxScore - (adjustDeductions[item.itemNo] ?? item.deductedScore ?? 0)),
              },
              {
                title: '说明',
                width: 220,
                render: (_, item) => (
                  <Input
                    value={adjustRemarks[item.itemNo] || ''}
                    onChange={(event) => setAdjustRemarks((prev) => ({
                      ...prev,
                      [item.itemNo]: event.target.value,
                    }))}
                  />
                ),
              },
            ]}
          />
          <Typography.Text strong>调整后本题得分：{adjustTotalScore}</Typography.Text>
          <Input.TextArea
            rows={2}
            value={adjustNote}
            onChange={(event) => setAdjustNote(event.target.value)}
            placeholder="考评说明（选填）"
          />
          <Input.TextArea
            rows={3}
            value={adjustReason}
            onChange={(event) => setAdjustReason(event.target.value)}
            placeholder="必须填写修改原因，用于审计留痕"
          />
        </Space>
      </Modal>
    </Space>
  );
}

interface AssessorBreakdownProps {
  assessorGroups?: Map<string, { id: string; name: string; tasks: PracticalTaskProgressItem[] }>;
  onReturnTask?: (task: PracticalTaskProgressItem) => void;
  onAdjustTask?: (task: PracticalTaskProgressItem) => void;
}

function AssessorBreakdown({ assessorGroups, onReturnTask, onAdjustTask }: AssessorBreakdownProps) {
  const slots = useMemo(() => {
    if (!assessorGroups) return [];
    return Array.from(assessorGroups.values()).sort((a, b) => a.id.localeCompare(b.id));
  }, [assessorGroups]);

  if (slots.length === 0) {
    return <Typography.Text type="secondary">暂无考评员评分任务</Typography.Text>;
  }

  // 补齐到 3 列占位，保证三栏对齐
  const padded: Array<typeof slots[number] | null> = [...slots];
  while (padded.length < 3) padded.push(null);

  return (
    <div className="practical-candidate-assessors">
      {padded.map((assessor, index) => {
        if (!assessor) {
          return (
            <div
              key={`empty-${index}`}
              className="practical-candidate-assessor practical-candidate-assessor--empty"
            >
              该位置暂无考评员
            </div>
          );
        }
        const submitted = assessor.tasks.filter((task) => task.status === 'submitted').length;
        const total = assessor.tasks.length;
        const totalScore = assessor.tasks.reduce((sum, task) => sum + (task.totalScore || 0), 0);
        const allSubmitted = total > 0 && submitted === total;
        const anyReturned = assessor.tasks.some((task) => task.status === 'returned');
        let statusColor: string = 'processing';
        let statusText = '评分中';
        if (allSubmitted) {
          statusColor = 'success';
          statusText = '已提交';
        } else if (anyReturned) {
          statusColor = 'error';
          statusText = '已退回';
        }
        return (
          <div key={assessor.id} className="practical-candidate-assessor">
            <div className="practical-assessor-head">
              <span className="assessor-name">{assessor.name}</span>
              <Tag color={statusColor}>{statusText}</Tag>
            </div>
            <div className="practical-assessor-progress">
              <Progress
                percent={total ? Math.round((submitted * 100) / total) : 0}
                size="small"
                style={{ flex: 1, margin: 0 }}
              />
              <span>{submitted}/{total} 已提交</span>
            </div>
            <div className="practical-assessor-tasks">
              {assessor.tasks.map((task) => {
                const taskColor = task.status === 'submitted'
                  ? 'success'
                  : task.status === 'returned'
                    ? 'error'
                    : 'processing';
                const canReturn = task.status === 'submitted';
                const reviewRequired = Boolean(task.reviewRequired);
                return (
                  <div
                    key={task.assessorTaskId}
                    className={`practical-assessor-task${reviewRequired ? ' is-review-required' : ''}`}
                  >
                    <span className="task-title">{task.questionTitle}</span>
                    <span className="task-score">{task.totalScore ?? '-'}</span>
                    <Tag color={taskColor}>{statusName(task.status)}</Tag>
                    {reviewRequired ? (
                      <Tag color="error">分差异常{task.scoreDeviation != null ? ` ${task.scoreDeviation}分` : ''}</Tag>
                    ) : null}
                    {onAdjustTask ? (
                      <Button
                        type="link"
                        size="small"
                        disabled={!canReturn}
                        onClick={(event) => {
                          event.stopPropagation();
                          onAdjustTask(task);
                        }}
                      >
                        修改分数
                      </Button>
                    ) : null}
                    {onReturnTask ? (
                      <Button
                        type="link"
                        size="small"
                        danger
                        disabled={!canReturn}
                        icon={<RollbackOutlined />}
                        onClick={(event) => {
                          event.stopPropagation();
                          onReturnTask(task);
                        }}
                      >
                        退回
                      </Button>
                    ) : null}
                  </div>
                );
              })}
            </div>
            <div className="practical-assessor-total">
              <span>合计分</span>
              <strong>{totalScore}</strong>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function scopeMatchesCandidate(scope: PracticalScopeOption, candidate: PreparationCandidate) {
  const sameLevel = !scope.levelCode || scope.levelCode === candidate.levelCode;
  const sameWorkType = candidate.workTypeId
    ? scope.workTypeId === candidate.workTypeId || scope.workTypeName === candidate.workTypeName
    : scope.workTypeName === candidate.workTypeName;
  return sameLevel && sameWorkType;
}

function findScopeForCandidate(scopes: PracticalScopeOption[], candidate: PreparationCandidate) {
  return scopes.find((scope) => scopeMatchesCandidate(scope, candidate));
}

function paperOptionsForCandidate(
  papers: PracticalPaperOption[],
  candidate: PreparationCandidate,
  scope?: PracticalScopeOption,
) {
  return papers.filter((paper) => {
    const workTypeId = candidate.workTypeId || scope?.workTypeId;
    const sameWorkType = !paper.workTypeId || paper.workTypeId === workTypeId;
    const sameLevel = !paper.level || paper.level === candidate.levelCode || paper.level === candidate.levelName;
    return sameWorkType && sameLevel;
  });
}

function toNullableNumber(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function statusName(status: string) {
  return { assigned: '待评分', draft: '草稿', submitted: '已提交', returned: '已退回' }[status] || status;
}
