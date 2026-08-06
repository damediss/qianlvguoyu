import {
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
  StopOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Result,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import {
  createCompetitionTheorySchedule,
  deleteCompetitionTheoryRoom,
  deleteCompetitionTheorySession,
  downloadCompetitionTheoryRoster,
  getCompetitionEvent,
  getCompetitionTheoryRequirement,
  getCompetitionTheorySchedule,
  getCompetitionTheorySchedulePreflight,
  importCompetitionTheoryPaperPair,
  listCompetitionEventWorkTypes,
  listCompetitionScoreRules,
  listCompetitionTheoryCourses,
  listCompetitionTheoryMonitor,
  listCompetitionTheoryPaperSnapshots,
  listCompetitionTheoryResults,
  listCompetitionTheoryRooms,
  listCompetitionTheorySchedules,
  listCompetitionTheorySourcePapers,
  pageCompetitionStages,
  performCompetitionTheoryMonitorAction,
  publishCompetitionTheorySchedule,
  randomAllocateCompetitionTheorySchedule,
  saveCompetitionTheoryRoom,
  saveCompetitionTheorySession,
  updateCompetitionTheoryRequirement,
  withdrawCompetitionTheorySchedule,
} from '../../api/competition';
import { useAuthStore } from '../../stores/authStore';
import type {
  CompetitionId,
  CompetitionTheoryMonitor,
  CompetitionTheoryRequirementRequest,
  CompetitionTheoryRoom,
  CompetitionTheorySession,
} from '../../types/competition';
import { CompetitionEventFrame } from './CompetitionEventFrame';
import './CompetitionEventManagementPage.css';

type RequirementFormValues = CompetitionTheoryRequirementRequest;
type RoomFormValues = Omit<CompetitionTheoryRoom, 'id' | 'eventId'>;
type SessionFormValues = {
  sessionCode: 'A' | 'B';
  sessionName: string;
  paperSnapshotId: CompetitionId;
  timeRange: [Dayjs, Dayjs];
  roomIds: CompetitionId[];
};
type MonitorAction = 'force-offline' | 'force-submit' | 'restore-absence';

const monitorActionLabel: Record<MonitorAction, string> = {
  'force-offline': '强制下线',
  'force-submit': '强制交卷',
  'restore-absence': '恢复暂定缺考',
};

function idValue(value?: CompetitionId | null) {
  return value === undefined || value === null ? '' : String(value);
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-';
}

function statusTag(status?: string | null) {
  const color = status === 'published' || status === 'submitted_manual' || status === 'submitted_auto'
    ? 'success'
    : status === 'draft' || status === 'waiting' || status === 'ready'
      ? 'default'
      : status === 'in_progress' || status === 'open'
        ? 'processing'
        : status === 'provisional_absent'
          ? 'warning'
          : 'error';
  return <Tag color={color}>{status || '-'}</Tag>;
}

function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function CompetitionTheoryExamPage({
  eventId,
  onNavigate,
  initialStageId,
  initialEventWorkTypeId,
  embedded = false,
}: {
  eventId: string;
  onNavigate: (path: string) => void;
  initialStageId?: string;
  initialEventWorkTypeId?: string;
  embedded?: boolean;
}) {
  const permissions = useAuthStore((state) => state.permissionCodes);
  const canViewEvent = permissions.includes('competition:event:view');
  const canViewPaper = permissions.includes('competition:theory-paper:view');
  const canManagePaper = permissions.includes('competition:theory-paper:manage');
  const canViewSchedule = permissions.includes('competition:theory-schedule:view');
  const canManageSchedule = permissions.includes('competition:theory-schedule:manage');
  const canPublishSchedule = permissions.includes('competition:theory-schedule:publish');
  const canExportRoster = permissions.includes('competition:theory-roster:export');
  const canViewMonitor = permissions.includes('competition:theory-monitor:view');
  const canForceOffline = permissions.includes('competition:theory-monitor:force-offline');
  const canForceSubmit = permissions.includes('competition:theory-monitor:force-submit');
  const canRestoreAbsence = permissions.includes('competition:theory-monitor:restore-absence');
  const canViewResults = permissions.includes('competition:theory-result:view');
  const canReadDimensions = permissions.includes('competition:stage:view')
    && permissions.includes('competition:event-work-type:view')
    && permissions.includes('competition:score-rule:view');
  const hasWorkspacePermission = canViewPaper || canViewSchedule || canViewMonitor || canViewResults;

  const [activeTab, setActiveTab] = useState('paper');
  const [stageId, setStageId] = useState<string | undefined>(initialStageId);
  const [eventWorkTypeId, setEventWorkTypeId] = useState<string | undefined>(initialEventWorkTypeId);
  const [scoreRuleId, setScoreRuleId] = useState<string>();
  const [courseId, setCourseId] = useState<string>();
  const [selectedVersionId, setSelectedVersionId] = useState<string>();
  const [requirementOpen, setRequirementOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [roomOpen, setRoomOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState<CompetitionTheoryRoom>();
  const [sessionOpen, setSessionOpen] = useState(false);
  const [editingSession, setEditingSession] = useState<CompetitionTheorySession>();
  const [monitorAction, setMonitorAction] = useState<{ action: MonitorAction; record: CompetitionTheoryMonitor }>();
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [requirementForm] = Form.useForm<RequirementFormValues>();
  const [importForm] = Form.useForm<{ paperAId: CompetitionId; paperBId: CompetitionId }>();
  const [roomForm] = Form.useForm<RoomFormValues>();
  const [sessionForm] = Form.useForm<SessionFormValues>();
  const [actionForm] = Form.useForm<{ reason: string; confirmation: string }>();
  const [withdrawForm] = Form.useForm<{ reason: string }>();
  const queryClient = useQueryClient();
  const scopeReady = Boolean(stageId && eventWorkTypeId && scoreRuleId);

  const eventQuery = useQuery({
    queryKey: ['competition-event', eventId],
    queryFn: () => getCompetitionEvent(eventId),
    enabled: canViewEvent,
  });
  const stagesQuery = useQuery({
    queryKey: ['competition-stages', eventId],
    queryFn: () => pageCompetitionStages(eventId, { page: 1, pageSize: 500 }),
    enabled: canViewEvent && canReadDimensions,
  });
  const workTypesQuery = useQuery({
    queryKey: ['competition-event-work-types', eventId],
    queryFn: () => listCompetitionEventWorkTypes(eventId),
    enabled: canViewEvent && canReadDimensions,
  });
  const rulesQuery = useQuery({
    queryKey: ['competition-score-rules', eventId, stageId, eventWorkTypeId],
    queryFn: () => listCompetitionScoreRules(eventId, { stageId, eventWorkTypeId }),
    enabled: canReadDimensions && Boolean(stageId && eventWorkTypeId),
  });

  useEffect(() => {
    const stages = stagesQuery.data?.items || [];
    if (!stages.length) return;
    if (!stageId || !stages.some((stage) => idValue(stage.id) === stageId)) {
      setStageId(idValue(stages[0].id));
    }
  }, [stageId, stagesQuery.data?.items]);
  useEffect(() => {
    const workTypes = workTypesQuery.data || [];
    if (!workTypes.length) return;
    if (!eventWorkTypeId || !workTypes.some((workType) => idValue(workType.id) === eventWorkTypeId)) {
      setEventWorkTypeId(idValue(workTypes[0].id));
    }
  }, [eventWorkTypeId, workTypesQuery.data]);
  useEffect(() => {
    const rules = rulesQuery.data || [];
    if (!rules.some((rule) => idValue(rule.id) === scoreRuleId)) {
      const preferred = rules.find((rule) => rule.status === 'published') || rules[0];
      setScoreRuleId(preferred ? idValue(preferred.id) : undefined);
    }
  }, [rulesQuery.data, scoreRuleId]);

  const requirementQuery = useQuery({
    queryKey: ['competition-theory-requirement', scoreRuleId],
    queryFn: () => getCompetitionTheoryRequirement(scoreRuleId!),
    enabled: canViewPaper && Boolean(scoreRuleId),
  });
  const coursesQuery = useQuery({
    queryKey: ['competition-theory-courses', eventId, eventWorkTypeId],
    queryFn: () => listCompetitionTheoryCourses(eventId, eventWorkTypeId!),
    enabled: canViewPaper && Boolean(eventWorkTypeId),
  });
  useEffect(() => {
    const courses = coursesQuery.data || [];
    if (!courses.some((course) => idValue(course.courseId) === courseId)) {
      setCourseId(courses[0] ? idValue(courses[0].courseId) : undefined);
    }
  }, [courseId, coursesQuery.data]);
  const sourcePapersQuery = useQuery({
    queryKey: ['competition-theory-source-papers', eventId, eventWorkTypeId, courseId],
    queryFn: () => listCompetitionTheorySourcePapers(eventId, eventWorkTypeId!, courseId!),
    enabled: canViewPaper && Boolean(eventWorkTypeId && courseId),
  });
  const snapshotsQuery = useQuery({
    queryKey: ['competition-theory-paper-snapshots', eventId, stageId, eventWorkTypeId, scoreRuleId],
    queryFn: () => listCompetitionTheoryPaperSnapshots(eventId, { stageId, eventWorkTypeId, scoreRuleId }),
    enabled: canViewPaper && scopeReady,
  });
  const roomsQuery = useQuery({
    queryKey: ['competition-theory-rooms', eventId],
    queryFn: () => listCompetitionTheoryRooms(eventId),
    enabled: canViewSchedule,
  });
  const schedulesQuery = useQuery({
    queryKey: ['competition-theory-schedules', eventId, stageId, eventWorkTypeId],
    queryFn: () => listCompetitionTheorySchedules(eventId, { stageId, eventWorkTypeId }),
    enabled: canViewSchedule && Boolean(stageId && eventWorkTypeId),
  });
  useEffect(() => {
    const versions = schedulesQuery.data || [];
    if (!versions.some((version) => idValue(version.id) === selectedVersionId)) {
      setSelectedVersionId(versions[0] ? idValue(versions[0].id) : undefined);
    }
  }, [schedulesQuery.data, selectedVersionId]);
  const scheduleQuery = useQuery({
    queryKey: ['competition-theory-schedule', selectedVersionId],
    queryFn: () => getCompetitionTheorySchedule(selectedVersionId!),
    enabled: canViewSchedule && Boolean(selectedVersionId),
  });
  const preflightQuery = useQuery({
    queryKey: ['competition-theory-preflight', selectedVersionId],
    queryFn: () => getCompetitionTheorySchedulePreflight(selectedVersionId!),
    enabled: canViewSchedule && Boolean(selectedVersionId),
  });
  const monitorQuery = useQuery({
    queryKey: ['competition-theory-monitor', selectedVersionId],
    queryFn: () => listCompetitionTheoryMonitor(selectedVersionId!),
    enabled: canViewMonitor && Boolean(selectedVersionId) && activeTab === 'monitor',
    refetchInterval: 5000,
  });
  const resultsQuery = useQuery({
    queryKey: ['competition-theory-results', selectedVersionId],
    queryFn: () => listCompetitionTheoryResults(selectedVersionId!),
    enabled: canViewResults && Boolean(selectedVersionId) && activeTab === 'monitor',
  });

  const refreshSchedule = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['competition-theory-schedules'] }),
      queryClient.invalidateQueries({ queryKey: ['competition-theory-schedule', selectedVersionId] }),
      queryClient.invalidateQueries({ queryKey: ['competition-theory-preflight', selectedVersionId] }),
    ]);
  };

  const requirementMutation = useMutation({
    mutationFn: (payload: RequirementFormValues) => updateCompetitionTheoryRequirement(scoreRuleId!, payload),
    onSuccess: () => {
      message.success('组卷要求已保存');
      setRequirementOpen(false);
      queryClient.invalidateQueries({ queryKey: ['competition-theory-requirement', scoreRuleId] });
    },
  });
  const importMutation = useMutation({
    mutationFn: (values: { paperAId: CompetitionId; paperBId: CompetitionId }) => importCompetitionTheoryPaperPair(eventId, {
      scoreRuleId: scoreRuleId!, courseId: courseId!, ...values,
    }),
    onSuccess: () => {
      message.success('A/B 卷已固化为竞赛快照');
      setImportOpen(false);
      importForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-theory-paper-snapshots'] });
    },
  });
  const roomMutation = useMutation({
    mutationFn: (payload: RoomFormValues) => saveCompetitionTheoryRoom(eventId, editingRoom?.id, payload),
    onSuccess: () => {
      message.success('考场已保存');
      setRoomOpen(false);
      setEditingRoom(undefined);
      roomForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-theory-rooms', eventId] });
    },
  });
  const createScheduleMutation = useMutation({
    mutationFn: () => createCompetitionTheorySchedule(eventId, {
      stageId: stageId!, eventWorkTypeId: eventWorkTypeId!, scoreRuleId: scoreRuleId!,
    }),
    onSuccess: async (version) => {
      message.success('排考版本已创建');
      setSelectedVersionId(idValue(version.id));
      await refreshSchedule();
    },
  });
  const sessionMutation = useMutation({
    mutationFn: (values: SessionFormValues) => saveCompetitionTheorySession(selectedVersionId!, editingSession?.id, {
      sessionCode: values.sessionCode,
      sessionName: values.sessionName,
      paperSnapshotId: values.paperSnapshotId,
      startAt: values.timeRange[0].format('YYYY-MM-DDTHH:mm:ss'),
      endAt: values.timeRange[1].format('YYYY-MM-DDTHH:mm:ss'),
      roomIds: values.roomIds,
    }),
    onSuccess: async () => {
      message.success('考试场次已保存');
      setSessionOpen(false);
      setEditingSession(undefined);
      sessionForm.resetFields();
      await refreshSchedule();
    },
  });
  const scheduleActionMutation = useMutation({
    mutationFn: (action: 'allocate' | 'publish') => action === 'allocate'
      ? randomAllocateCompetitionTheorySchedule(selectedVersionId!)
      : publishCompetitionTheorySchedule(selectedVersionId!),
    onSuccess: async (_, action) => {
      message.success(action === 'allocate' ? '随机分配已完成' : '排考已发布');
      await refreshSchedule();
    },
  });
  const withdrawMutation = useMutation({
    mutationFn: ({ reason }: { reason: string }) => withdrawCompetitionTheorySchedule(selectedVersionId!, reason),
    onSuccess: async () => {
      message.success('排考已撤回，历史版本与原因已保留');
      setWithdrawOpen(false);
      withdrawForm.resetFields();
      await refreshSchedule();
    },
  });
  const monitorActionMutation = useMutation({
    mutationFn: ({ action, attemptId, reason }: { action: MonitorAction; attemptId: CompetitionId; reason: string }) =>
      performCompetitionTheoryMonitorAction(attemptId, action, reason),
    onSuccess: (_, variables) => {
      message.success(`${monitorActionLabel[variables.action]}已执行`);
      setMonitorAction(undefined);
      actionForm.resetFields();
      queryClient.invalidateQueries({ queryKey: ['competition-theory-monitor', selectedVersionId] });
      queryClient.invalidateQueries({ queryKey: ['competition-theory-results', selectedVersionId] });
    },
  });

  const selectedSchedule = scheduleQuery.data;
  const paperOptions = (sourcePapersQuery.data || []).map((paper) => ({
    label: `${paper.paperTitle}（${paper.questionCount}题 / ${paper.totalScore}分）`, value: idValue(paper.paperId),
  }));
  const snapshotOptions = (snapshotsQuery.data || []).map((snapshot) => ({
    label: `${snapshot.paperCode}卷 v${snapshot.versionNo} · ${snapshot.sourceTitle}`,
    value: idValue(snapshot.id),
  }));
  const roomOptions = (roomsQuery.data || []).filter((room) => room.status === 'enabled').map((room) => ({
    label: `${room.name}（${room.capacity}人）`, value: idValue(room.id),
  }));
  const versionOptions = (schedulesQuery.data || []).map((version) => ({
    label: `v${version.versionNo} · ${version.status}`, value: idValue(version.id),
  }));

  const paperContent = (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {!canViewPaper ? <Result status="403" title="暂无试卷管理查看权限" /> : (
        <>
          <Card size="small" title="组卷要求" extra={canManagePaper && <Button onClick={() => {
            const requirement = requirementQuery.data;
            requirementForm.setFieldsValue(requirement ? {
              templateCode: requirement.templateCode,
              durationMinutes: requirement.durationMinutes,
              singleCount: requirement.singleCount,
              singleScore: requirement.singleScore,
              multiCount: requirement.multiCount,
              multiScore: requirement.multiScore,
              judgeCount: requirement.judgeCount,
              judgeScore: requirement.judgeScore,
              totalScore: requirement.totalScore,
              passScore: requirement.passScore,
            } : { templateCode: 'A_B', durationMinutes: 60, singleCount: 40, singleScore: 1, multiCount: 10, multiScore: 2, judgeCount: 40, judgeScore: 1, totalScore: 100, passScore: 60 });
            setRequirementOpen(true);
          }}>配置要求</Button>}>
            {requirementQuery.data ? (
              <Descriptions size="small" column={4}>
                <Descriptions.Item label="模板">{requirementQuery.data.templateCode}</Descriptions.Item>
                <Descriptions.Item label="时长">{requirementQuery.data.durationMinutes} 分钟</Descriptions.Item>
                <Descriptions.Item label="单选">{requirementQuery.data.singleCount} × {requirementQuery.data.singleScore}</Descriptions.Item>
                <Descriptions.Item label="多选">{requirementQuery.data.multiCount} × {requirementQuery.data.multiScore}</Descriptions.Item>
                <Descriptions.Item label="判断">{requirementQuery.data.judgeCount} × {requirementQuery.data.judgeScore}</Descriptions.Item>
                <Descriptions.Item label="总分">{requirementQuery.data.totalScore}</Descriptions.Item>
                <Descriptions.Item label="及格线">{requirementQuery.data.passScore}</Descriptions.Item>
              </Descriptions>
            ) : <Alert type="warning" showIcon message="当前评分规则尚未配置理论组卷要求" />}
          </Card>
          <Card size="small" title="A/B 卷快照" extra={canManagePaper && <Button type="primary" icon={<PlusOutlined />} disabled={!requirementQuery.data || !courseId} onClick={() => setImportOpen(true)}>导入 A/B 卷</Button>}>
            <Space wrap style={{ marginBottom: 12 }}>
              <Select style={{ width: 360 }} placeholder="选择题库课程" value={courseId} onChange={(value) => { setCourseId(value); importForm.resetFields(); }} options={(coursesQuery.data || []).map((course) => ({ label: `${course.title}${course.version ? ` · ${course.version}` : ''}`, value: idValue(course.courseId) }))} />
            </Space>
            <Table rowKey={(row) => idValue(row.id)} size="small" pagination={false} dataSource={snapshotsQuery.data || []} columns={[
              { title: '试卷', dataIndex: 'paperCode', width: 70, render: (value) => <Tag color={value === 'A' ? 'blue' : 'purple'}>{value} 卷</Tag> },
              { title: '版本', dataIndex: 'versionNo', width: 80, render: (value) => `v${value}` },
              { title: '来源标题', dataIndex: 'sourceTitle' },
              { title: '题量', dataIndex: 'questionCount', width: 80 },
              { title: '总分', dataIndex: 'totalScore', width: 80 },
              { title: '时长', dataIndex: 'durationMinutes', width: 90, render: (value) => `${value} 分钟` },
              { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
              { title: '固化时间', dataIndex: 'importedAt', width: 175, render: formatDate },
            ]} />
          </Card>
        </>
      )}
    </Space>
  );

  const scheduleToolbar = (
    <Space wrap>
      <Select style={{ width: 190 }} placeholder="选择排考版本" value={selectedVersionId} onChange={setSelectedVersionId} options={versionOptions} />
      {canManageSchedule && <Button icon={<PlusOutlined />} disabled={!scopeReady} loading={createScheduleMutation.isPending} onClick={() => createScheduleMutation.mutate()}>新建版本</Button>}
      {canManageSchedule && <Button disabled={!selectedVersionId || selectedSchedule?.status !== 'draft'} onClick={() => { setEditingSession(undefined); sessionForm.resetFields(); setSessionOpen(true); }}>新增场次</Button>}
      {canManageSchedule && <Button disabled={!selectedVersionId || selectedSchedule?.status !== 'draft'} onClick={() => scheduleActionMutation.mutate('allocate')}>随机分配</Button>}
      {canPublishSchedule && <Button type="primary" icon={<SendOutlined />} disabled={!preflightQuery.data?.readyToPublish || selectedSchedule?.status !== 'draft'} onClick={() => Modal.confirm({ title: '确认发布排考？', content: '发布后选手端将读取本版本，既有已发布版本会按服务端规则处理。', okText: '确认发布', onOk: () => scheduleActionMutation.mutateAsync('publish') })}>发布</Button>}
      {canPublishSchedule && <Button danger icon={<StopOutlined />} disabled={selectedSchedule?.status !== 'published'} onClick={() => setWithdrawOpen(true)}>撤回</Button>}
    </Space>
  );

  const scheduleContent = !canViewSchedule ? <Result status="403" title="暂无排考查看权限" /> : (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card size="small" title="排考版本" extra={scheduleToolbar}>
        {selectedSchedule ? (
          <Descriptions size="small" column={5}>
            <Descriptions.Item label="版本">v{selectedSchedule.versionNo}</Descriptions.Item>
            <Descriptions.Item label="状态">{statusTag(selectedSchedule.status)}</Descriptions.Item>
            <Descriptions.Item label="应排/已排">{selectedSchedule.eligibleCandidateCount} / {selectedSchedule.assignedCandidateCount}</Descriptions.Item>
            <Descriptions.Item label="总容量">{selectedSchedule.totalCapacity}</Descriptions.Item>
            <Descriptions.Item label="容量差">{selectedSchedule.capacityGap}</Descriptions.Item>
          </Descriptions>
        ) : <Alert type="info" showIcon message="当前范围尚无排考版本" />}
        {preflightQuery.data && !preflightQuery.data.readyToPublish && (
          <Alert style={{ marginTop: 12 }} type="warning" showIcon message="发布前检查未通过" description={preflightQuery.data.issues.join('；') || '请完成场次与人员分配。'} />
        )}
      </Card>
      <Card size="small" title="考场" extra={canManageSchedule && <Button onClick={() => { setEditingRoom(undefined); roomForm.setFieldsValue({ status: 'enabled', capacity: 30 } as RoomFormValues); setRoomOpen(true); }}>新增考场</Button>}>
        <Table rowKey={(row) => idValue(row.id)} size="small" pagination={false} dataSource={roomsQuery.data || []} columns={[
          { title: '名称', dataIndex: 'name' }, { title: '地址', dataIndex: 'address' }, { title: '容量', dataIndex: 'capacity', width: 90 },
          { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
          { title: '备注', dataIndex: 'remark' },
          { title: '操作', width: 150, render: (_, room: CompetitionTheoryRoom) => canManageSchedule && <Space size={0}><Button type="link" size="small" onClick={() => { setEditingRoom(room); roomForm.setFieldsValue({ name: room.name, address: room.address, capacity: room.capacity, status: room.status, remark: room.remark || undefined }); setRoomOpen(true); }}>编辑</Button><Button danger type="link" size="small" onClick={() => Modal.confirm({ title: `删除考场“${room.name}”？`, okButtonProps: { danger: true }, onOk: async () => { await deleteCompetitionTheoryRoom(room.id); message.success('考场已删除'); queryClient.invalidateQueries({ queryKey: ['competition-theory-rooms', eventId] }); } })}>删除</Button></Space> },
        ]} />
      </Card>
      <Card size="small" title="考试场次">
        <Table rowKey={(row) => idValue(row.id)} size="small" pagination={false} dataSource={selectedSchedule?.sessions || []} columns={[
          { title: '卷别', dataIndex: 'sessionCode', width: 70 }, { title: '场次', dataIndex: 'sessionName' },
          { title: '开始', dataIndex: 'startAt', width: 175, render: formatDate }, { title: '结束', dataIndex: 'endAt', width: 175, render: formatDate },
          { title: '考场', dataIndex: 'rooms', render: (rooms) => rooms.map((room: { roomName: string }) => room.roomName).join('、') || '-' },
          { title: '容量', dataIndex: 'totalCapacity', width: 80 }, { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
          { title: '操作', width: 150, render: (_, session: CompetitionTheorySession) => canManageSchedule && selectedSchedule?.status === 'draft' && <Space size={0}><Button type="link" size="small" onClick={() => { setEditingSession(session); sessionForm.setFieldsValue({ sessionCode: session.sessionCode, sessionName: session.sessionName, paperSnapshotId: idValue(session.paperSnapshotId), timeRange: [dayjs(session.startAt), dayjs(session.endAt)], roomIds: session.rooms.map((room) => idValue(room.roomId)) }); setSessionOpen(true); }}>编辑</Button><Button danger type="link" size="small" onClick={() => Modal.confirm({ title: `删除场次“${session.sessionName}”？`, okButtonProps: { danger: true }, onOk: async () => { await deleteCompetitionTheorySession(selectedVersionId!, session.id); message.success('场次已删除'); await refreshSchedule(); } })}>删除</Button></Space> },
        ]} />
      </Card>
    </Space>
  );

  const rosterContent = !canViewSchedule ? <Result status="403" title="暂无名册查看权限" /> : (
    <Card size="small" title="考试名册" extra={<Space>{canExportRoster && <><Button icon={<DownloadOutlined />} disabled={!selectedVersionId} onClick={async () => downloadBlob(await downloadCompetitionTheoryRoster(selectedVersionId!, 'xlsx'), 'competition-theory-roster.xlsx')}>Excel</Button><Button icon={<DownloadOutlined />} disabled={!selectedVersionId} onClick={async () => downloadBlob(await downloadCompetitionTheoryRoster(selectedVersionId!, 'pdf'), 'competition-theory-roster.pdf')}>PDF</Button></>}</Space>}>
      <Table rowKey={(row) => idValue(row.assignmentId)} size="small" dataSource={selectedSchedule?.assignments || []} pagination={{ pageSize: 20 }} columns={[
        { title: '选手号', dataIndex: 'contestantNo', width: 120 }, { title: '姓名', dataIndex: 'contestantName', width: 110 },
        { title: '单位', dataIndex: 'orgName' }, { title: '工种', dataIndex: 'workTypeName', width: 140 },
        { title: '场次', dataIndex: 'sessionName', width: 140 }, { title: '考场', dataIndex: 'roomName', width: 140 },
        { title: '座位', dataIndex: 'seatNo', width: 90 }, { title: '开始时间', dataIndex: 'startAt', width: 175, render: formatDate },
      ]} />
    </Card>
  );

  const actionAllowed = (action: MonitorAction) => action === 'force-offline' ? canForceOffline : action === 'force-submit' ? canForceSubmit : canRestoreAbsence;
  const monitorContent = !canViewMonitor && !canViewResults ? <Result status="403" title="暂无监考或结果查看权限" /> : (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Alert showIcon type="info" message="监考列表每 5 秒刷新；仅展示状态与最后保存时间，不加载答卷、标准答案或解析。" />
      {canViewMonitor && <Card size="small" title="实时监考" extra={<Button icon={<ReloadOutlined />} onClick={() => monitorQuery.refetch()}>刷新</Button>}>
        <Table rowKey={(row) => idValue(row.attemptId)} size="small" loading={monitorQuery.isFetching} dataSource={monitorQuery.data || []} pagination={{ pageSize: 20 }} columns={[
          { title: '选手号', dataIndex: 'contestantNo', width: 110 }, { title: '姓名', dataIndex: 'contestantName', width: 100 },
          { title: '单位', dataIndex: 'orgName' }, { title: '场次/座位', render: (_, row: CompetitionTheoryMonitor) => `${row.sessionName} / ${row.roomName} ${row.seatNo}` },
          { title: '答题状态', dataIndex: 'status', width: 140, render: statusTag },
          { title: '会话', dataIndex: 'examSessionOnline', width: 90, render: (online: boolean) => <Tag color={online ? 'success' : 'default'}>{online ? '在线' : '离线'}</Tag> },
          { title: '最后保存', dataIndex: 'lastSavedAt', width: 175, render: formatDate },
          { title: '操作', width: 260, render: (_, row: CompetitionTheoryMonitor) => <Space size={0}>{(['force-offline', 'force-submit', 'restore-absence'] as MonitorAction[]).filter(actionAllowed).map((action) => <Button key={action} type="link" danger={action !== 'restore-absence'} size="small" disabled={action === 'restore-absence' && row.status !== 'provisional_absent'} onClick={() => { actionForm.resetFields(); setMonitorAction({ action, record: row }); }}>{monitorActionLabel[action]}</Button>)}</Space> },
        ]} />
      </Card>}
      {canViewResults && <Card size="small" title="结果摘要（成绩未发布）">
        <Table rowKey={(row) => idValue(row.attemptId)} size="small" loading={resultsQuery.isLoading} dataSource={resultsQuery.data || []} pagination={{ pageSize: 20 }} columns={[
          { title: '选手号', dataIndex: 'contestantNo' }, { title: '姓名', dataIndex: 'contestantName' }, { title: '状态', dataIndex: 'status', render: statusTag },
          { title: '缺考', dataIndex: 'absent', render: (value: boolean) => value ? <Tag color="error">是</Tag> : '否' },
          { title: '客观题总分', dataIndex: 'totalScore', render: (value) => value ?? '-' }, { title: '确认状态', dataIndex: 'confirmationStatus', render: statusTag },
          { title: '发布状态', dataIndex: 'published', render: () => <Tag>未发布</Tag> }, { title: '交卷时间', dataIndex: 'submittedAt', render: formatDate },
        ]} />
      </Card>}
    </Space>
  );

  if (!canViewEvent || !hasWorkspacePermission) {
    return <Result status="403" title="暂无理论考试工作台权限" />;
  }
  if (eventQuery.isLoading) return <Spin className="competition-loading" />;
  if (!eventQuery.data) return <Result status="404" title="赛事不存在或无法访问" />;

  const content = (
    <>
      <Card bordered={false} className="competition-filter-card competition-filter-card--workspace">
        <div className="competition-filter-card__head">
          <div><Typography.Title level={4}>理论试卷与考试执行</Typography.Title><Typography.Text type="secondary">按阶段、工种与已发布评分规则管理竞赛专用理论考试</Typography.Text></div>
          <Tag color="blue"><SafetyCertificateOutlined /> 专用考试身份域</Tag>
        </div>
        <Space wrap className="competition-filter-card__body">
          <Select style={{ width: 220 }} placeholder="选择阶段" value={stageId} onChange={(value) => { setStageId(value); setScoreRuleId(undefined); }} options={(stagesQuery.data?.items || []).map((stage) => ({ label: stage.name, value: idValue(stage.id) }))} />
          <Select style={{ width: 220 }} placeholder="选择工种" value={eventWorkTypeId} onChange={(value) => { setEventWorkTypeId(value); setScoreRuleId(undefined); }} options={(workTypesQuery.data || []).map((workType) => ({ label: workType.displayName || workType.workTypeName || idValue(workType.id), value: idValue(workType.id) }))} />
          <Select style={{ width: 260 }} placeholder="选择评分规则" value={scoreRuleId} onChange={setScoreRuleId} options={(rulesQuery.data || []).map((rule) => ({ label: `v${rule.ruleVersion} · ${rule.status}`, value: idValue(rule.id) }))} />
          <Select style={{ width: 190 }} placeholder="选择排考版本" allowClear value={selectedVersionId} onChange={setSelectedVersionId} options={versionOptions} />
        </Space>
      </Card>
      {!canReadDimensions && <Alert type="warning" showIcon message="缺少阶段、工种或评分规则查看权限，无法选择考试范围。" />}
      <Card bordered={false} className="competition-table-card" style={{ marginTop: 16 }}>
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
          { key: 'paper', label: '试卷', children: paperContent },
          { key: 'schedule', label: '排考', children: scheduleContent },
          { key: 'roster', label: '名册', children: rosterContent },
          { key: 'monitor', label: '监考', children: monitorContent },
        ]} />
      </Card>

      <Modal title="配置组卷要求" open={requirementOpen} onCancel={() => setRequirementOpen(false)} onOk={() => requirementForm.submit()} confirmLoading={requirementMutation.isPending} destroyOnClose>
        <Form form={requirementForm} layout="vertical" onFinish={(values) => requirementMutation.mutate(values)}>
          <Form.Item name="templateCode" label="模板编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Space wrap>
            <Form.Item name="durationMinutes" label="时长（分钟）" rules={[{ required: true }]}><InputNumber min={1} /></Form.Item>
            <Form.Item name="totalScore" label="总分" rules={[{ required: true }]}><InputNumber min={1} max={100} /></Form.Item>
            <Form.Item name="passScore" label="及格线" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          </Space>
          <Space wrap>
            <Form.Item name="singleCount" label="单选题数" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
            <Form.Item name="singleScore" label="单选分值" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
            <Form.Item name="multiCount" label="多选题数" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
            <Form.Item name="multiScore" label="多选分值" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
            <Form.Item name="judgeCount" label="判断题数" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
            <Form.Item name="judgeScore" label="判断分值" rules={[{ required: true }]}><InputNumber min={0} /></Form.Item>
          </Space>
        </Form>
      </Modal>
      <Modal title="导入 A/B 卷快照" open={importOpen} onCancel={() => setImportOpen(false)} onOk={() => importForm.submit()} confirmLoading={importMutation.isPending} destroyOnClose>
        <Alert style={{ marginBottom: 16 }} type="info" showIcon message="导入后固化题目、选项、标准答案和来源版本；选手端与监考端不会获得标准答案。" />
        <Form form={importForm} layout="vertical" onFinish={(values) => {
          if (idValue(values.paperAId) === idValue(values.paperBId)) return message.error('A/B 卷必须选择不同试卷');
          importMutation.mutate(values);
        }}>
          <Form.Item name="paperAId" label="A 卷来源" rules={[{ required: true }]}><Select options={paperOptions} /></Form.Item>
          <Form.Item name="paperBId" label="B 卷来源" rules={[{ required: true }]}><Select options={paperOptions} /></Form.Item>
        </Form>
      </Modal>
      <Modal title={editingRoom ? '编辑考场' : '新增考场'} open={roomOpen} onCancel={() => setRoomOpen(false)} onOk={() => roomForm.submit()} confirmLoading={roomMutation.isPending} destroyOnClose>
        <Form form={roomForm} layout="vertical" onFinish={(values) => roomMutation.mutate(values)}>
          <Form.Item name="name" label="考场名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="address" label="地址" rules={[{ required: true }]}><Input /></Form.Item>
          <Space wrap><Form.Item name="capacity" label="容量" rules={[{ required: true }]}><InputNumber min={1} max={10000} /></Form.Item><Form.Item name="status" label="状态" rules={[{ required: true }]}><Select style={{ width: 140 }} options={[{ label: '启用', value: 'enabled' }, { label: '停用', value: 'disabled' }]} /></Form.Item></Space>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
      <Modal title={editingSession ? '编辑考试场次' : '新增考试场次'} open={sessionOpen} onCancel={() => setSessionOpen(false)} onOk={() => sessionForm.submit()} confirmLoading={sessionMutation.isPending} destroyOnClose>
        <Form form={sessionForm} layout="vertical" onFinish={(values) => sessionMutation.mutate(values)}>
          <Space wrap><Form.Item name="sessionCode" label="卷别" rules={[{ required: true }]}><Select style={{ width: 120 }} options={[{ label: 'A 卷', value: 'A' }, { label: 'B 卷', value: 'B' }]} /></Form.Item><Form.Item name="sessionName" label="场次名称" rules={[{ required: true }]}><Input style={{ width: 280 }} /></Form.Item></Space>
          <Form.Item name="paperSnapshotId" label="试卷快照" rules={[{ required: true }]}><Select options={snapshotOptions} /></Form.Item>
          <Form.Item name="timeRange" label="考试时间" rules={[{ required: true }]}><DatePicker.RangePicker showTime style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="roomIds" label="考场" rules={[{ required: true }]}><Select mode="multiple" options={roomOptions} /></Form.Item>
        </Form>
      </Modal>
      <Modal title="撤回排考版本" open={withdrawOpen} onCancel={() => setWithdrawOpen(false)} onOk={() => withdrawForm.submit()} confirmLoading={withdrawMutation.isPending} okButtonProps={{ danger: true }} destroyOnClose>
        <Form form={withdrawForm} layout="vertical" onFinish={(values) => withdrawMutation.mutate(values)}><Form.Item name="reason" label="撤回原因" rules={[{ required: true, min: 2 }]}><Input.TextArea rows={4} /></Form.Item></Form>
      </Modal>
      <Modal title={monitorAction ? monitorActionLabel[monitorAction.action] : '监考操作'} open={Boolean(monitorAction)} onCancel={() => setMonitorAction(undefined)} onOk={() => actionForm.submit()} confirmLoading={monitorActionMutation.isPending} okButtonProps={{ danger: monitorAction?.action !== 'restore-absence' }} destroyOnClose>
        <Alert style={{ marginBottom: 16 }} type="warning" showIcon message="此操作会写入审计日志。请输入原因，并键入“确认”完成二次确认。" />
        <Form form={actionForm} layout="vertical" onFinish={(values) => monitorAction && monitorActionMutation.mutate({ action: monitorAction.action, attemptId: monitorAction.record.attemptId, reason: values.reason })}>
          <Form.Item name="reason" label="操作原因" rules={[{ required: true, min: 2 }]}><Input.TextArea rows={4} /></Form.Item>
          <Form.Item name="confirmation" label="二次确认" rules={[{ required: true }, { validator: (_, value) => value === '确认' ? Promise.resolve() : Promise.reject(new Error('请输入“确认”')) }]}><Input placeholder="确认" /></Form.Item>
        </Form>
      </Modal>
    </>
  );
  return embedded ? content : <CompetitionEventFrame event={eventQuery.data} activeSection="theory-exam" onNavigate={onNavigate}>{content}</CompetitionEventFrame>;
}
