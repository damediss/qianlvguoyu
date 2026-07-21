/**
 * 分班 — 名单+排班自动建班（每班≤50）
 * 选规划 → 配置并一键分班 → 编辑草稿 → 提交企业审核 → 通过后建班
 */
import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Collapse,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Select,
  Space,
  Statistic,
  Steps,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CheckOutlined,
  DownloadOutlined,
  EditOutlined,
  ReloadOutlined,
  SendOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import * as trainApi from '../../api/training';
import type {
  DivisionAutoPackClass,
  DivisionAutoPackClassMember,
  DivisionAutoPackUnassigned,
  DivisionPackTaskResult,
  DivisionPlanItem,
  DivisionRosterItem,
} from '../../types/training';
import { useNavStore } from '../../stores/navStore';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import './TrainingClassDivisionPage.css';

const { Text } = Typography;

const STRATEGY_LABEL: Record<string, string> = {
  by_job_level: '按工种/等级',
  by_job_level_slot: '工种等级+上下午时段',
  by_shift_team: '按班组/倒班',
  by_roster_order: '按名单顺序',
};

const STATUS_COLOR: Record<string, string> = {
  draft: 'processing',
  pending: 'warning',
  rejected: 'error',
  materialized: 'success',
  superseded: 'default',
};

function clonePack(pack: DivisionPackTaskResult): DivisionPackTaskResult {
  return JSON.parse(JSON.stringify(pack)) as DivisionPackTaskResult;
}

function normalizeClasses(classes: DivisionAutoPackClass[]): DivisionAutoPackClass[] {
  return classes.map((c, idx) => ({
    ...c,
    tempId: c.tempId || `c-${idx + 1}`,
    nums: c.members?.length ?? c.nums ?? 0,
    members: (c.members ?? []).map((m) => ({ ...m })),
  }));
}

export function TrainingClassDivisionPage() {
  const queryClient = useQueryClient();
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const now = new Date();

  const [step, setStep] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [readyOnly, setReadyOnly] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [selectedPlan, setSelectedPlan] = useState<DivisionPlanItem | null>(null);
  const [trainInfoId, setTrainInfoId] = useState<number | null>(null);
  const [trainInfoTitle, setTrainInfoTitle] = useState('');
  const [titlePrefix, setTitlePrefix] = useState('');
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [classMax, setClassMax] = useState(50);
  const [strategy, setStrategy] = useState<'by_job_level' | 'by_job_level_slot' | 'by_shift_team' | 'by_roster_order'>('by_job_level');
  const [useShiftFilter, setUseShiftFilter] = useState(true);

  const [packResult, setPackResult] = useState<DivisionPackTaskResult | null>(null);
  const [draftDirty, setDraftDirty] = useState(false);
  const [editClassIdx, setEditClassIdx] = useState<number | null>(null);
  const [resultTab, setResultTab] = useState('classes');

  const [compatClassTitle, setCompatClassTitle] = useState('');
  const [selectedRosterIds, setSelectedRosterIds] = useState<number[]>([]);
  const [rosterPage, setRosterPage] = useState(1);

  const plansQuery = useQuery({
    queryKey: ['division-plans', page, pageSize, keyword, readyOnly],
    queryFn: () => trainApi.pageDivisionPlans({
      page,
      pageSize,
      keyword: keyword || undefined,
      readyOnly,
    }),
  });

  const readyQuery = useQuery({
    queryKey: ['division-ready', selectedPlan?.planId],
    queryFn: () => trainApi.getDivisionReady(selectedPlan!.planId),
    enabled: !!selectedPlan?.planId,
  });

  const rosterQuery = useQuery({
    queryKey: ['division-roster', selectedPlan?.planId, trainInfoId, rosterPage],
    queryFn: () => trainApi.pageDivisionRoster({
      planId: selectedPlan!.planId,
      trainInfoId: trainInfoId ?? undefined,
      assignStatus: 0,
      page: rosterPage,
      pageSize: 100,
    }),
    enabled: !!selectedPlan?.planId && step === 1,
  });

  const materialsOk = !!(
    selectedPlan
    && (selectedPlan.rosterFileId || readyQuery.data?.rosterFileId)
    && (selectedPlan.shiftFileId || readyQuery.data?.shiftFileId)
  );
  const missing = readyQuery.data?.missing ?? [];
  const rosterFileName = selectedPlan?.rosterFileName || readyQuery.data?.rosterFileName;
  const shiftFileName = selectedPlan?.shiftFileName || readyQuery.data?.shiftFileName;

  const editable = !!packResult && (packResult.status === 'draft' || packResult.status === 'rejected');
  const editingClass = editClassIdx != null && packResult ? packResult.classes[editClassIdx] : null;

  const enterPlan = (row: DivisionPlanItem) => {
    if (!row.rosterFileId || !row.shiftFileId) {
      message.warning('需同时具备筛选名单与排班表');
      return;
    }
    setSelectedPlan(row);
    setTrainInfoId(null);
    setTrainInfoTitle('');
    setTitlePrefix(row.orgName || row.title);
    setCompatClassTitle(`${row.orgName || row.title}-培训班`);
    setPackResult(null);
    setDraftDirty(false);
    setSelectedRosterIds([]);
    setResultTab('classes');
    setStep(1);
  };

  const applyLocalPack = (next: DivisionPackTaskResult) => {
    const normalized = {
      ...next,
      classes: normalizeClasses(next.classes ?? []),
      unassigned: next.unassigned ?? [],
      classCount: next.classes?.length ?? 0,
      assignedCount: (next.classes ?? []).reduce((s, c) => s + (c.members?.length ?? c.nums ?? 0), 0),
      unassignedCount: next.unassigned?.length ?? 0,
    };
    setPackResult(normalized);
  };

  const ensureTrainInfo = async (): Promise<number> => {
    if (trainInfoId) return trainInfoId;
    const res = await trainApi.ensureDivisionTrainInfo(selectedPlan!.planId, {
      title: titlePrefix || undefined,
      reuseExisting: true,
    });
    setTrainInfoId(res.trainInfoId);
    setTrainInfoTitle(res.title);
    queryClient.invalidateQueries({ queryKey: ['division-plans'] });
    return res.trainInfoId;
  };

  const packMu = useMutation({
    mutationFn: async () => {
      const tid = await ensureTrainInfo();
      return trainApi.autoPackDivision({
        planId: selectedPlan!.planId,
        trainInfoId: tid,
        strategy,
        classMax,
        classNamePrefix: titlePrefix ? `${titlePrefix}-` : undefined,
        submitForCompanyConfirm: false,
        useShiftFilter,
        excludeNoIdCard: false,
        year,
        month,
        autoParseRoster: true,
      });
    },
    onSuccess: (res) => {
      applyLocalPack(res);
      setTrainInfoId(res.trainInfoId);
      setDraftDirty(false);
      setStep(2);
      setResultTab('classes');
      message.success(`已生成分班草稿：${res.classCount} 个班 / ${res.assignedCount} 人，请核对后提交企业审核`);
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
      queryClient.invalidateQueries({ queryKey: ['division-plans'] });
      queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
    },
    onError: (e: Error) => message.error(e.message || '自动分班失败'),
  });

  const saveMu = useMutation({
    mutationFn: () => {
      if (!packResult) throw new Error('无草稿');
      return trainApi.patchDivisionPackTask(packResult.taskId, {
        classes: packResult.classes.map((c) => ({
          tempId: c.tempId,
          title: c.title,
          group: c.group,
          members: c.members ?? [],
        })),
        unassigned: packResult.unassigned,
      });
    },
    onSuccess: (res) => {
      applyLocalPack(res);
      setDraftDirty(false);
      message.success('草稿已保存');
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const submitMu = useMutation({
    mutationFn: async () => {
      if (!packResult) throw new Error('无草稿');
      if (draftDirty) {
        await trainApi.patchDivisionPackTask(packResult.taskId, {
          classes: packResult.classes.map((c) => ({
            tempId: c.tempId,
            title: c.title,
            group: c.group,
            members: c.members ?? [],
          })),
          unassigned: packResult.unassigned,
        });
      }
      return trainApi.submitDivisionPackTask(packResult.taskId);
    },
    onSuccess: (res) => {
      applyLocalPack(res);
      setDraftDirty(false);
      message.success('已提交企业审核，通过后将自动创建培训班级');
      queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
      queryClient.invalidateQueries({ queryKey: ['enterprise-pack-confirm'] });
    },
    onError: (e: Error) => message.error(e.message || '提交失败'),
  });

  const parseMu = useMutation({
    mutationFn: async () => {
      const tid = await ensureTrainInfo();
      return trainApi.parseDivisionRoster({
        planId: selectedPlan!.planId,
        trainInfoId: tid,
        mode: 'enterprise_roster',
        fallbackToPlanItem: false,
      });
    },
    onSuccess: (res) => {
      message.success(`已解析 ${res.total} 人`);
      setSelectedRosterIds([]);
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
      queryClient.invalidateQueries({ queryKey: ['division-plans'] });
    },
    onError: (e: Error) => message.error(e.message || '解析名单失败'),
  });

  const splitMu = useMutation({
    mutationFn: async () => {
      const tid = await ensureTrainInfo();
      return trainApi.submitDivisionOrgSplit({
        planId: selectedPlan!.planId,
        trainInfoId: tid,
        submitForCompanyConfirm: true,
        classes: [{
          title: compatClassTitle.trim() || `${selectedPlan?.orgName || '培训'}-1班`,
          rosterIds: selectedRosterIds,
        }],
      });
    },
    onSuccess: (res) => {
      message.success(`手工分班成功：${res.classCount} 个班级`);
      setSelectedRosterIds([]);
      queryClient.invalidateQueries({ queryKey: ['division-roster'] });
      queryClient.invalidateQueries({ queryKey: ['division-plans'] });
      queryClient.invalidateQueries({ queryKey: ['enterprise-class-confirm'] });
    },
    onError: (e: Error) => message.error(e.message || '分班提交失败'),
  });

  const updateDraft = (mutator: (draft: DivisionPackTaskResult) => void) => {
    if (!packResult || !editable) return;
    const next = clonePack(packResult);
    mutator(next);
    next.classes = normalizeClasses(next.classes);
    next.classCount = next.classes.length;
    next.assignedCount = next.classes.reduce((s, c) => s + (c.members?.length ?? 0), 0);
    next.unassignedCount = next.unassigned.length;
    setPackResult(next);
    setDraftDirty(true);
  };

  const moveMemberToUnassigned = (classIdx: number, rosterId: number) => {
    updateDraft((draft) => {
      const cls = draft.classes[classIdx];
      if (!cls?.members) return;
      const member = cls.members.find((m) => m.rosterId === rosterId);
      if (!member) return;
      cls.members = cls.members.filter((m) => m.rosterId !== rosterId);
        draft.unassigned = [
        ...draft.unassigned.filter((u) => u.rosterId !== rosterId),
        {
          rosterId: member.rosterId,
          personName: member.personName,
          idCard: member.idCard,
          workshop: member.workshop,
          jobName: member.jobName,
          levelName: member.levelName,
          shiftTeam: member.shiftTeam,
          reason: '机构调整：移出班级',
        },
      ];
    });
  };

  const moveUnassignedToClass = (rosterId: number, targetClassIdx: number) => {
    updateDraft((draft) => {
      const person = draft.unassigned.find((u) => u.rosterId === rosterId);
      if (!person || person.rosterId == null) return;
      const cls = draft.classes[targetClassIdx];
      if (!cls) return;
      if ((cls.members?.length ?? 0) >= draft.classMax) {
        message.warning(`目标班已达上限 ${draft.classMax}`);
        return;
      }
      cls.members = [
        ...(cls.members ?? []),
        {
          rosterId: person.rosterId,
          personName: person.personName,
          idCard: person.idCard,
          workshop: person.workshop,
          jobName: person.jobName,
          levelName: person.levelName,
          shiftTeam: person.shiftTeam,
        },
      ];
      draft.unassigned = draft.unassigned.filter((u) => u.rosterId !== rosterId);
    });
  };

  const moveMemberBetweenClasses = (fromIdx: number, rosterId: number, toIdx: number) => {
    if (fromIdx === toIdx) return;
    updateDraft((draft) => {
      const from = draft.classes[fromIdx];
      const to = draft.classes[toIdx];
      if (!from?.members || !to) return;
      const member = from.members.find((m) => m.rosterId === rosterId);
      if (!member) return;
      if ((to.members?.length ?? 0) >= draft.classMax) {
        message.warning(`目标班已达上限 ${draft.classMax}`);
        return;
      }
      from.members = from.members.filter((m) => m.rosterId !== rosterId);
      to.members = [...(to.members ?? []), member];
    });
  };

  const pendingTotal = selectedPlan?.pendingRosterCount;

  const planColumns: ColumnsType<DivisionPlanItem> = [
    { title: '规划编号', dataIndex: 'planNo', width: 150 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '企业', dataIndex: 'orgName', width: 140, ellipsis: true },
    {
      title: '材料',
      width: 150,
      render: (_, row) => (
        <Space size={4} wrap>
          <Tag color={row.rosterFileId ? 'success' : 'default'}>名单</Tag>
          <Tag color={row.shiftFileId ? 'success' : 'default'}>排班</Tag>
        </Space>
      ),
    },
    {
      title: '待分',
      dataIndex: 'pendingRosterCount',
      width: 72,
      align: 'right',
      render: (v?: number) => (v != null && v > 0 ? <Text strong>{v}</Text> : (v ?? '-')),
    },
    {
      title: '已建培训',
      dataIndex: 'linkedTrainInfoCount',
      width: 88,
      align: 'right',
      render: (v?: number) => v ?? 0,
    },
    {
      title: '操作',
      width: 96,
      fixed: 'right',
      render: (_, row) => {
        const ok = !!(row.rosterFileId && row.shiftFileId);
        return (
          <Button type="link" size="small" disabled={!ok} onClick={(e) => { e.stopPropagation(); enterPlan(row); }}>
            开始分班
          </Button>
        );
      },
    },
  ];

  const rosterColumns: ColumnsType<DivisionRosterItem> = [
    { title: '姓名', dataIndex: 'personName', width: 100 },
    { title: '身份证', dataIndex: 'idCard', width: 180 },
    { title: '班组', dataIndex: 'shiftTeam', width: 100, render: (v) => v || '-' },
    { title: '工种', dataIndex: 'jobName', width: 120, render: (v) => v || '-' },
  ];

  const classOptions = useMemo(
    () => (packResult?.classes ?? []).map((c, idx) => ({
      value: idx,
      label: `${c.title}（${c.members?.length ?? c.nums}人）`,
    })),
    [packResult],
  );

  const confirmPack = () => {
    if (!materialsOk) {
      message.warning(`材料未齐：${missing.join('、') || '筛选名单/排班表'}`);
      return;
    }
    Modal.confirm({
      title: '生成分班草稿？',
      width: 480,
      content: (
        <div style={{ marginTop: 8 }}>
          <p style={{ marginBottom: 8 }}>
            策略「{STRATEGY_LABEL[strategy]}」：
            {strategy === 'by_job_level'
              ? '按名单工种与等级分班；排班表仅过滤当月难以上课人员。缺工种/等级进未分入。'
              : strategy === 'by_job_level_slot'
                ? '按工种/等级分组后，结合排班表按每天上午/下午可上课情况拆成多个时段班（如 电工-高级-上午1班）。'
                : '按选定规则切班。'}
            每班不超过 {classMax} 人，生成可编辑草稿，不会立即建班。
          </p>
        </div>
      ),
      okText: '生成草稿',
      onOk: () => packMu.mutateAsync(),
    });
  };

  const confirmSubmit = () => {
    if (!packResult) return;
    Modal.confirm({
      title: '提交企业审核？',
      content: `将提交 ${packResult.classCount} 个班（${packResult.assignedCount} 人）给企业审核。审核通过后系统才会创建培训班级。`,
      okText: '提交审核',
      onOk: () => submitMu.mutateAsync(),
    });
  };

  return (
    <div className="training-class-division-page training-page">
      <TrainingProcessSteps current={3} />

      <section className="page-heading">
        <div>
          <Typography.Title level={4} style={{ marginBottom: 4 }}>分班</Typography.Title>
          <Text type="secondary">
            根据人员名单与排班表自动划分培训班（默认按班组切班，每班≤{classMax}）
          </Text>
        </div>
      </section>

      <Card bordered={false} className="division-steps-card">
        <Steps
          current={step}
          size="small"
          items={[
            { title: '选择规划' },
            { title: '配置并分班' },
            { title: '编辑并提交' },
          ]}
          onChange={(s) => {
            if (s === 0) { setStep(0); return; }
            if (!selectedPlan) { message.warning('请先选择规划'); return; }
            if (s >= 1 && !materialsOk) {
              message.warning('需同时具备筛选名单与排班表');
              return;
            }
            if (s >= 2 && !packResult) {
              message.warning('请先执行自动分班生成草稿');
              return;
            }
            setStep(s);
          }}
        />
      </Card>

      {step === 0 && (
        <Card bordered={false}>
          <div className="division-toolbar">
            <Input.Search
              allowClear
              placeholder="搜索规划标题 / 编号 / 企业"
              style={{ width: 280 }}
              onSearch={(v) => { setKeyword(v); setPage(1); }}
            />
            <Checkbox
              checked={readyOnly}
              onChange={(e) => { setReadyOnly(e.target.checked); setPage(1); }}
            >
              仅材料已齐
            </Checkbox>
            <Button icon={<ReloadOutlined />} onClick={() => plansQuery.refetch()}>刷新</Button>
          </div>
          <Alert
            className="division-plan-hint"
            type="info"
            showIcon
                    message="一键分班会按企业最新上传的筛选名单重新解析人员；生成草稿后请再提交企业审核。"
          />
          <Table<DivisionPlanItem>
            rowKey="planId"
            loading={plansQuery.isLoading}
            columns={planColumns}
            dataSource={plansQuery.data?.items ?? []}
            scroll={{ x: 900 }}
            onRow={(row) => ({
              className: row.rosterFileId && row.shiftFileId ? 'division-row-ready' : undefined,
              style: { cursor: row.rosterFileId && row.shiftFileId ? 'pointer' : 'default' },
              onDoubleClick: () => enterPlan(row),
            })}
            locale={{ emptyText: readyOnly ? '暂无材料已齐的规划，可取消「仅材料已齐」查看全部' : '暂无规划' }}
            pagination={{
              current: page,
              pageSize,
              total: plansQuery.data?.total ?? 0,
              showSizeChanger: true,
              showTotal: (t) => `共 ${t} 条`,
              onChange: (p, ps) => { setPage(p); setPageSize(ps); },
            }}
          />
        </Card>
      )}

      {step === 1 && selectedPlan && (
        <Card bordered={false}>
          <div className="division-context">
            <div className="division-context-main">
              <p className="division-context-title">
                {selectedPlan.planNo} · {selectedPlan.title}
              </p>
              <div className="division-context-meta">
                企业 {selectedPlan.orgName || '-'}
                {pendingTotal != null ? ` · 待分池 ${pendingTotal} 人` : ''}
                {trainInfoId ? ` · 培训信息 #${trainInfoId}` : ''}
              </div>
              <Space size={4} style={{ marginTop: 6 }} wrap>
                <Tag color={selectedPlan.rosterFileId || readyQuery.data?.rosterFileId ? 'success' : 'error'}>
                  名单{rosterFileName ? ` · ${rosterFileName}` : ''}
                </Tag>
                <Tag color={selectedPlan.shiftFileId || readyQuery.data?.shiftFileId ? 'success' : 'error'}>
                  排班{shiftFileName ? ` · ${shiftFileName}` : ''}
                </Tag>
              </Space>
            </div>
            <Space wrap>
              <Button
                size="small"
                icon={<DownloadOutlined />}
                disabled={!selectedPlan.rosterFileId}
                onClick={() => trainApi.downloadThreeYearPlanFile(selectedPlan.planId, 'enterprise-roster')}
              >
                下载名单
              </Button>
              <Button
                size="small"
                icon={<DownloadOutlined />}
                disabled={!selectedPlan.shiftFileId}
                onClick={() => trainApi.downloadThreeYearPlanFile(selectedPlan.planId, 'enterprise-shift')}
              >
                下载排班
              </Button>
              <Button size="small" onClick={() => setStep(0)}>重选规划</Button>
            </Space>
          </div>

          {!materialsOk && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message={`材料未齐：${missing.join('、') || '筛选名单/排班表'}，请先在企业审核中回传。`}
            />
          )}

          <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="班级名前缀">
              <Input
                value={titlePrefix}
                onChange={(e) => setTitlePrefix(e.target.value)}
                placeholder="如：来铝"
                style={{ maxWidth: 220 }}
              />
            </Descriptions.Item>
            <Descriptions.Item label="培训信息">
              {trainInfoId
                ? <Text type="success">#{trainInfoId} {trainInfoTitle || ''}（分班时自动复用）</Text>
                : <Text type="secondary">点击分班时自动生成/复用</Text>}
            </Descriptions.Item>
            <Descriptions.Item label="说明">
              <Text type="secondary">先出草稿，企业通过后再建班</Text>
            </Descriptions.Item>
          </Descriptions>

          <Form layout="vertical">
            <div className="division-config-grid">
              <Form.Item label="培训/排班年" required>
                <InputNumber style={{ width: '100%' }} min={2020} max={2100} value={year} onChange={(v) => setYear(v || now.getFullYear())} />
              </Form.Item>
              <Form.Item label="排班月" required>
                <InputNumber style={{ width: '100%' }} min={1} max={12} value={month} onChange={(v) => setMonth(v || 1)} />
              </Form.Item>
              <Form.Item label="每班上限" required>
                <InputNumber style={{ width: '100%' }} min={1} max={200} value={classMax} onChange={(v) => setClassMax(v || 50)} />
              </Form.Item>
              <Form.Item label="分班策略">
                <Select
                  value={strategy}
                  onChange={setStrategy}
                  options={[
                    { value: 'by_job_level', label: '按工种/等级切班（推荐）' },
                    { value: 'by_shift_team', label: '按班组/倒班切班' },
                    { value: 'by_job_level_slot', label: '工种/等级 + 上下午时段（高级）' },
                    { value: 'by_roster_order', label: '按名单顺序切班' },
                  ]}
                />
              </Form.Item>
            </div>
            <Checkbox checked={useShiftFilter} onChange={(e) => setUseShiftFilter(e.target.checked)}>
              过滤当月无可上课日人员（全夜/全休等）
            </Checkbox>
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                默认按工种×等级分班；排班只做过滤。名单带年份时，所选年无人将直接报错。
              </Text>
            </div>
          </Form>

          <div className="division-actions">
            <Button
              type="primary"
              size="large"
              icon={<ThunderboltOutlined />}
              loading={packMu.isPending}
              disabled={!materialsOk}
              onClick={confirmPack}
            >
              一键自动分班
            </Button>
            <Text type="secondary">每次都会按企业最新名单重新解析，生成草稿后不立即建班</Text>
          </div>

          <Collapse
            items={[
              {
                key: 'manual',
                label: (
                  <Space>
                    <span>兼容：勾选名单直接建班（旧路径）</span>
                    {selectedRosterIds.length > 0 && <Tag color="blue">已选 {selectedRosterIds.length}</Tag>}
                  </Space>
                ),
                children: (
                  <div>
                    <Alert
                      type="warning"
                      showIcon
                      style={{ marginBottom: 12 }}
                      message="此路径会立即创建班级并进入企业确认，建议优先使用上方「一键自动分班」草稿流程。"
                    />
                    <Space style={{ marginBottom: 12 }} wrap>
                      <Button loading={parseMu.isPending} onClick={() => parseMu.mutate()}>解析确认名单</Button>
                      <Input
                        style={{ width: 240 }}
                        value={compatClassTitle}
                        onChange={(e) => setCompatClassTitle(e.target.value)}
                        placeholder="班级名称"
                      />
                      <Button
                        type="primary"
                        icon={<CheckOutlined />}
                        loading={splitMu.isPending}
                        disabled={selectedRosterIds.length === 0 || !compatClassTitle.trim()}
                        onClick={() => {
                          Modal.confirm({
                            title: '手工提交分班？',
                            content: `将 ${selectedRosterIds.length} 人写入「${compatClassTitle.trim()}」并提交企业确认。`,
                            onOk: () => splitMu.mutateAsync(),
                          });
                        }}
                      >
                        保存并提交企业确认
                      </Button>
                    </Space>
                    <Table<DivisionRosterItem>
                      rowKey="id"
                      size="small"
                      loading={rosterQuery.isLoading || parseMu.isPending}
                      columns={rosterColumns}
                      dataSource={rosterQuery.data?.items ?? []}
                      locale={{ emptyText: '暂无待分人员，可先点「解析确认名单」' }}
                      rowSelection={{
                        selectedRowKeys: selectedRosterIds,
                        onChange: (keys) => setSelectedRosterIds(keys as number[]),
                        preserveSelectedRowKeys: true,
                      }}
                      pagination={{
                        current: rosterPage,
                        pageSize: 100,
                        total: rosterQuery.data?.total ?? 0,
                        showTotal: (t) => `共 ${t} 人`,
                        onChange: (p) => setRosterPage(p),
                      }}
                    />
                  </div>
                ),
              },
            ]}
          />
        </Card>
      )}

      {step === 2 && selectedPlan && packResult && (
        <Card bordered={false}>
          <div className="division-context" style={{ marginBottom: 16 }}>
            <div className="division-context-main">
              <p className="division-context-title">
                {selectedPlan.planNo} · {selectedPlan.title}
              </p>
              <div className="division-context-meta">
                方案 #{packResult.taskId}
                {' · '}策略 {STRATEGY_LABEL[packResult.strategy] || packResult.strategy}
                {' · '}每班上限 {packResult.classMax}
                {trainInfoId ? ` · 培训信息 #${trainInfoId}` : ''}
              </div>
            </div>
            <Space wrap>
              <Tag color={STATUS_COLOR[packResult.status] || 'default'}>
                {packResult.statusLabel || packResult.status}
              </Tag>
              {draftDirty && <Tag color="orange">有未保存修改</Tag>}
            </Space>
          </div>

          {packResult.status === 'rejected' && packResult.rejectReason && (
            <Alert
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              message="企业已驳回"
              description={packResult.rejectReason}
            />
          )}
          {packResult.status === 'pending' && (
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
              message="已提交企业审核，通过后将自动创建培训班级。审核前不可再编辑。"
            />
          )}
          {packResult.status === 'materialized' && (
            <Alert
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
              message="企业已通过，培训班级已创建"
              description={packResult.materialized
                ? `已建 ${packResult.materialized.classCount} 班 / ${packResult.materialized.peopleCount} 人`
                : undefined}
            />
          )}
          {editable && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message="当前为草稿：可改班级名称、调整学员归属；保存后提交企业审核，通过后才真正建班。"
            />
          )}

          <Card size="small" className="division-result-stats" bordered={false} style={{ background: '#f8fafc' }}>
            <Space size={48} wrap>
              <Statistic title="班级数" value={packResult.classCount} suffix="个" />
              <Statistic title="已分配" value={packResult.assignedCount} suffix="人" />
              <Statistic
                title="未分入"
                value={packResult.unassignedCount}
                suffix="人"
                valueStyle={packResult.unassignedCount > 0 ? { color: '#d46b08' } : undefined}
              />
            </Space>
          </Card>

          <Tabs
            activeKey={resultTab}
            onChange={setResultTab}
            items={[
              {
                key: 'classes',
                label: `班级草稿 (${packResult.classes?.length ?? 0})`,
                children: (
                  <Table
                    rowKey={(r) => r.tempId || r.title}
                    size="small"
                    pagination={false}
                    dataSource={packResult.classes}
                    columns={[
                      {
                        title: '班级名称',
                        dataIndex: 'title',
                        render: (title: string, _row, idx) => (
                          editable ? (
                            <Input
                              value={title}
                              onChange={(e) => {
                                updateDraft((d) => {
                                  if (d.classes[idx]) d.classes[idx].title = e.target.value;
                                });
                              }}
                            />
                          ) : title
                        ),
                      },
                      { title: '分组', dataIndex: 'group', width: 120 },
                      {
                        title: '人数',
                        width: 160,
                        render: (_, row) => {
                          const nums = row.members?.length ?? row.nums ?? 0;
                          return (
                            <div className="division-fill">
                              <span>{nums}</span>
                              <Progress
                                percent={Math.min(100, Math.round((nums / (packResult.classMax || 50)) * 100))}
                                size="small"
                                showInfo={false}
                              />
                              <Text type="secondary" style={{ fontSize: 12 }}>/{packResult.classMax}</Text>
                            </div>
                          );
                        },
                      },
                      {
                        title: '操作',
                        width: 100,
                        render: (_v, _row, idx) => (
                          <Button
                            type="link"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => setEditClassIdx(idx)}
                          >
                            {editable ? '编辑学员' : '查看学员'}
                          </Button>
                        ),
                      },
                    ]}
                  />
                ),
              },
              {
                key: 'unassigned',
                label: `未分入 (${packResult.unassignedCount})`,
                children: (packResult.unassigned?.length ?? 0) === 0 ? (
                  <Alert type="success" showIcon message="全部可分人员均已入班草稿" />
                ) : (
                  <Table
                    rowKey={(r) => `${r.rosterId}-${r.reason}`}
                    size="small"
                    pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 人` }}
                    dataSource={packResult.unassigned}
                    columns={[
                      { title: '姓名', dataIndex: 'personName', width: 90 },
                      { title: '单位', dataIndex: 'workshop', width: 120, ellipsis: true, render: (v) => v || '-' },
                      { title: '工种', dataIndex: 'jobName', width: 110, ellipsis: true, render: (v) => v || '-' },
                      { title: '等级', dataIndex: 'levelName', width: 90, render: (v) => v || '-' },
                      { title: '身份证', dataIndex: 'idCard', width: 170 },
                      { title: '原因', dataIndex: 'reason' },
                      ...(editable ? [{
                        title: '操作',
                        width: 180,
                        render: (_: unknown, row: DivisionAutoPackUnassigned) => (
                          <Select
                            placeholder="分入班级"
                            style={{ width: 160 }}
                            options={classOptions}
                            onChange={(idx: number) => {
                              if (row.rosterId != null) moveUnassignedToClass(row.rosterId, idx);
                            }}
                          />
                        ),
                      } as ColumnsType<DivisionAutoPackUnassigned>[number]] : []),
                    ]}
                  />
                ),
              },
              ...(packResult.warnings?.length
                ? [{
                    key: 'warnings',
                    label: `警告 (${packResult.warnings.length})`,
                    children: (
                      <Alert
                        type="warning"
                        showIcon
                        message={`共 ${packResult.warnings.length} 条提示`}
                        description={(
                          <ul style={{ margin: '8px 0 0', paddingLeft: 18 }}>
                            {packResult.warnings.slice(0, 20).map((w, i) => (
                              <li key={i}>{w.message}</li>
                            ))}
                          </ul>
                        )}
                      />
                    ),
                  }]
                : []),
            ]}
          />

          <Space style={{ marginTop: 20 }} wrap>
            <Button onClick={() => setStep(1)}>返回配置</Button>
            {editable && (
              <>
                <Button loading={saveMu.isPending} disabled={!draftDirty} onClick={() => saveMu.mutate()}>
                  保存草稿
                </Button>
                <Button
                  type="primary"
                  icon={<SendOutlined />}
                  loading={submitMu.isPending}
                  onClick={confirmSubmit}
                >
                  提交企业审核
                </Button>
              </>
            )}
            {packResult.status === 'pending' && (
              <Button onClick={() => navigateTo('company_plan_review')}>
                去企业分班确认
              </Button>
            )}
            {packResult.status === 'materialized' && (
              <>
                <Button type="primary" onClick={() => navigateTo('train_people-list')}>去班级人员</Button>
                <Button onClick={() => navigateTo('company_plan_review')}>去企业分班确认</Button>
              </>
            )}
            <Button onClick={() => {
              setStep(0);
              setPackResult(null);
              setSelectedPlan(null);
              setTrainInfoId(null);
              setDraftDirty(false);
            }}
            >
              处理下一规划
            </Button>
          </Space>
        </Card>
      )}

      <Drawer
        title={editingClass ? `${editable ? '编辑' : '查看'}：${editingClass.title}` : '班级学员'}
        width={960}
        open={editClassIdx != null}
        onClose={() => setEditClassIdx(null)}
      >
        {editingClass && (
          <Table<DivisionAutoPackClassMember>
            rowKey="rosterId"
            size="small"
            pagination={false}
            dataSource={editingClass.members ?? []}
            locale={{ emptyText: '该班暂无学员' }}
            columns={[
              { title: '姓名', dataIndex: 'personName', width: 90 },
              { title: '单位', dataIndex: 'workshop', width: 120, ellipsis: true, render: (v) => v || '-' },
              { title: '工种', dataIndex: 'jobName', width: 110, ellipsis: true, render: (v) => v || '-' },
              { title: '等级', dataIndex: 'levelName', width: 90, render: (v) => v || '-' },
              { title: '班组', dataIndex: 'shiftTeam', width: 90, render: (v) => v || '-' },
              { title: '身份证', dataIndex: 'idCard', width: 170 },
              ...(editable && editClassIdx != null ? [{
                title: '调整',
                width: 220,
                render: (_: unknown, row: DivisionAutoPackClassMember) => (
                  <Space>
                    <Select
                      placeholder="调到其他班"
                      style={{ width: 140 }}
                      options={classOptions.filter((o) => o.value !== editClassIdx)}
                      onChange={(toIdx: number) => moveMemberBetweenClasses(editClassIdx, row.rosterId, toIdx)}
                    />
                    <Button
                      type="link"
                      danger
                      size="small"
                      onClick={() => moveMemberToUnassigned(editClassIdx, row.rosterId)}
                    >
                      移出
                    </Button>
                  </Space>
                ),
              } as ColumnsType<DivisionAutoPackClassMember>[number]] : []),
            ]}
          />
        )}
      </Drawer>
    </div>
  );
}
