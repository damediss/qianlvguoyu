import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Timeline,
  Tooltip,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CloudDownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { SubsidyPackProfile, TrainSubsidy } from '../../api/training';
import type { TrainInfoClassListItem } from '../../types/training';
import { downloadUrlFile } from '../../api/files';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import './TrainingSubsidyDeclarePage.css';

const PHASE_OPTS = [
  { value: 'draft', label: '草稿' },
  { value: 'checking', label: '核验中' },
  { value: 'ready', label: '材料就绪' },
  { value: 'submitted', label: '已提交' },
  { value: 'rejected', label: '已退回' },
  { value: 'approved', label: '已通过' },
  { value: 'granting', label: '拨付中' },
  { value: 'granted', label: '已拨付' },
  { value: 'closed', label: '已关闭' },
];

const PHASE_COLOR: Record<string, string> = {
  draft: 'default',
  checking: 'processing',
  ready: 'cyan',
  submitted: 'blue',
  rejected: 'error',
  approved: 'success',
  granting: 'gold',
  granted: 'green',
  closed: 'default',
};

const REGION_OPTS = [
  { value: '来宾市', label: '来宾市', code: '来宾' },
  { value: '玉林市', label: '玉林市', code: '玉林' },
  { value: '河池市南丹县', label: '河池市南丹县', code: '河池南丹' },
];

const RENDERER_LABEL: Record<string, string> = {
  manual_upload: '人工上传',
  follow_photo: '跟班照片册',
  docx_var: 'Word 填空',
  xlsx_row: 'Excel 填表',
  file_copy: '文件归档',
  pdf_merge: 'PDF 合并',
};

type ChecklistItem = {
  slotCode: string;
  title: string;
  required: boolean;
  ok: boolean;
  source?: string;
};

type GenResult = {
  ok: boolean;
  message?: string;
  missing?: string[];
};

function phaseLabel(v?: string | null) {
  return PHASE_OPTS.find((p) => p.value === v)?.label || v || '—';
}

function trainModeLabel(v?: string | null) {
  if (v === 'skill_upgrade') return '岗位技能提升';
  if (v === 'apprentice') return '新型学徒';
  return v || '—';
}

function money(v?: number | null) {
  if (v == null) return '—';
  return `¥${Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`;
}

/** 运营流程 · 申报补贴（S1 台账 + P1/P2 配方与生成） */
export function TrainingSubsidyDeclarePage() {
  const qc = useQueryClient();
  const [phase, setPhase] = useState<string | undefined>();
  const [regionFilter, setRegionFilter] = useState<string | undefined>();
  const [yearFilter, setYearFilter] = useState<number | undefined>();
  const [keyword, setKeyword] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [createOpen, setCreateOpen] = useState(false);
  const [detail, setDetail] = useState<TrainSubsidy | null>(null);
  const [lastGen, setLastGen] = useState<GenResult | null>(null);
  const [form] = Form.useForm();
  const watchedRegion = Form.useWatch('regionName', form);
  const watchedYear = Form.useWatch('policyYear', form);
  const watchedProfileId = Form.useWatch('profileId', form);
  const watchedClassId = Form.useWatch('classId', form);

  const listQ = useQuery({
    queryKey: ['subsidies', phase, regionFilter, yearFilter, keyword, page, pageSize],
    queryFn: () =>
      trainApi.pageSubsidies({
        page,
        pageSize,
        phase,
        regionCode: regionFilter,
        policyYear: yearFilter,
        keyword: keyword || undefined,
      }),
  });

  const statsQ = useQuery({
    queryKey: ['subsidies-stats'],
    queryFn: () => trainApi.pageSubsidies({ page: 1, pageSize: 200 }),
  });

  const classesQ = useQuery({
    queryKey: ['info-classes-brief'],
    queryFn: () => trainApi.pageInfoClassList({ page: 1, pageSize: 200 }),
    enabled: createOpen,
  });

  const profilesQ = useQuery({
    queryKey: ['subsidy-profiles-active'],
    queryFn: () => trainApi.listSubsidyPackProfiles({ status: 'active', trainMode: 'skill_upgrade' }),
  });

  const detailProfileQ = useQuery({
    queryKey: ['subsidy-profile', detail?.profileId],
    queryFn: () => trainApi.getSubsidyPackProfile(detail!.profileId!),
    enabled: !!detail?.profileId,
  });

  const eventsQ = useQuery({
    queryKey: ['subsidy-events', detail?.id],
    queryFn: () => trainApi.listSubsidyEvents(detail!.id),
    enabled: !!detail?.id,
  });

  const slotFilesQ = useQuery({
    queryKey: ['subsidy-slot-files', detail?.id],
    queryFn: () => trainApi.listSubsidySlotFiles(detail!.id),
    enabled: !!detail?.id,
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['subsidies'] });
    void qc.invalidateQueries({ queryKey: ['subsidies-stats'] });
    if (detail?.id) {
      void qc.invalidateQueries({ queryKey: ['subsidy-events', detail.id] });
      void qc.invalidateQueries({ queryKey: ['subsidy-slot-files', detail.id] });
      void qc.invalidateQueries({ queryKey: ['subsidy-profile', detail.profileId] });
    }
  };

  const openDetail = async (row: TrainSubsidy) => {
    setLastGen(null);
    setDetail(row);
    try {
      const fresh = await trainApi.getSubsidy(row.id);
      setDetail(fresh);
    } catch {
      /* keep list row */
    }
  };

  const createMu = useMutation({
    mutationFn: trainApi.createSubsidy,
    onSuccess: (row) => {
      message.success('已创建补贴单');
      setCreateOpen(false);
      form.resetFields();
      invalidate();
      void openDetail(row);
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const transitionMu = useMutation({
    mutationFn: (p: { id: number | string; toPhase: string; remark?: string }) =>
      trainApi.transitionSubsidy(p.id, p.toPhase, p.remark),
    onSuccess: (row) => {
      message.success('状态已更新');
      setDetail(row);
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '流转失败'),
  });

  const genMu = useMutation({
    mutationFn: (id: number | string) => trainApi.generateSubsidyMaterials(id),
    onSuccess: async (res, id) => {
      setLastGen({ ok: res.ok, message: res.message, missing: res.missing });
      if (!res.ok) {
        message.warning(res.message || '缺项未打包');
        invalidate();
        const fresh = await trainApi.getSubsidy(id);
        setDetail(fresh);
        return;
      }
      message.success(res.message || '已生成');
      invalidate();
      const fresh = await trainApi.getSubsidy(id);
      setDetail(fresh);
      if (res.filePath || res.url) {
        try {
          await downloadUrlFile(res.url || res.filePath!, `补贴材料_${id}.zip`);
        } catch {
          message.info('材料已生成，可稍后从材料包路径下载');
        }
      }
    },
    onError: (e: Error) => message.error(e.message || '生成失败'),
  });

  const classRows = useMemo(() => classesQ.data?.list ?? [], [classesQ.data]);

  const classMap = useMemo(() => {
    const map = new Map<string, TrainInfoClassListItem>();
    for (const c of classRows) {
      map.set(String(c.id), c);
    }
    return map;
  }, [classRows]);

  const classOptions = useMemo(() => {
    return classRows.map((c) => {
      const bits = [
        c.title || '班级',
        c.company_name || null,
        c.date_end ? `结业 ${c.date_end}` : null,
        c.nums != null ? `${c.nums}人` : null,
      ].filter(Boolean);
      return { value: c.id, label: `${bits.join(' · ')} (#${c.id})` };
    });
  }, [classRows]);

  const profiles = profilesQ.data || [];

  const matchedProfile = useMemo(() => {
    if (watchedProfileId) {
      return profiles.find((p) => String(p.id) === String(watchedProfileId));
    }
    if (!watchedRegion) return undefined;
    const region = REGION_OPTS.find((r) => r.value === watchedRegion);
    const year = watchedYear ?? new Date().getFullYear();
    return profiles.find(
      (p) =>
        p.status === 'active' &&
        (p.regionName === watchedRegion ||
          p.regionCode === region?.code ||
          (region && p.regionName.includes(region.code))) &&
        p.policyYear === year,
    );
  }, [profiles, watchedProfileId, watchedRegion, watchedYear]);

  const phaseCounts = useMemo(() => {
    const rows = statsQ.data?.items ?? [];
    const map: Record<string, number> = { __all: rows.length };
    for (const r of rows) {
      const k = r.phase || 'draft';
      map[k] = (map[k] || 0) + 1;
    }
    return map;
  }, [statsQ.data]);

  const checklist = useMemo((): ChecklistItem[] => {
    if (!detail?.checklistJson) return [];
    try {
      return JSON.parse(detail.checklistJson) as ChecklistItem[];
    } catch {
      return [];
    }
  }, [detail?.checklistJson]);

  const profile: SubsidyPackProfile | undefined = detailProfileQ.data;

  const slotRows = useMemo(() => {
    if (profile?.slots?.length) {
      return profile.slots.map((s) => {
        const c = checklist.find((x) => x.slotCode === s.slotCode);
        const file = (slotFilesQ.data || []).find((f) => f.slotCode === s.slotCode);
        return {
          ...s,
          ok: c?.ok,
          source: c?.source,
          fileName: file?.fileName || file?.filePath,
          hasFile: !!file,
        };
      });
    }
    return checklist.map((c) => ({
      slotCode: c.slotCode,
      title: c.title,
      required: c.required,
      renderer: 'manual_upload',
      ok: c.ok,
      source: c.source,
      fileName: (slotFilesQ.data || []).find((f) => f.slotCode === c.slotCode)?.fileName,
      hasFile: (slotFilesQ.data || []).some((f) => f.slotCode === c.slotCode),
    }));
  }, [profile, checklist, slotFilesQ.data]);

  const requiredTotal = slotRows.filter((s) => s.required).length;
  const requiredOk = slotRows.filter((s) => s.required && (s.ok || s.hasFile)).length;
  const packProgress = requiredTotal ? Math.round((requiredOk / requiredTotal) * 100) : 0;

  const stats = useMemo(() => {
    const rows = statsQ.data?.items ?? [];
    return {
      total: rows.length,
      draft: rows.filter((r) => r.phase === 'draft' || r.phase === 'checking').length,
      ready: rows.filter((r) => r.phase === 'ready' || r.phase === 'submitted').length,
      done: rows.filter((r) => ['approved', 'granting', 'granted'].includes(r.phase || '')).length,
    };
  }, [statsQ.data]);

  const columns: ColumnsType<TrainSubsidy> = [
    {
      title: '班级',
      dataIndex: 'className',
      width: 260,
      ellipsis: true,
      render: (v, r) => (
        <Button type="link" style={{ padding: 0, height: 'auto' }} onClick={() => void openDetail(r)}>
          {v || `班级 #${r.classId}` || '—'}
        </Button>
      ),
    },
    {
      title: '地市',
      dataIndex: 'regionName',
      width: 80,
      render: (v, r) => v || r.regionCode || '—',
    },
    {
      title: '政策年',
      dataIndex: 'policyYear',
      width: 80,
      align: 'center',
    },
    {
      title: '人数 / 金额',
      width: 140,
      render: (_, r) => (
        <span>
          {r.studentCount ?? '—'}人 · {money(r.subsidyAmount)}
        </span>
      ),
    },
    {
      title: '阶段',
      dataIndex: 'phase',
      width: 100,
      render: (v) => <Tag color={PHASE_COLOR[v] || 'default'}>{phaseLabel(v)}</Tag>,
    },
    {
      title: '配方',
      width: 160,
      ellipsis: true,
      render: (_, r) => {
        const p = profiles.find((x) => String(x.id) === String(r.profileId));
        const title = p?.title || (r.profileId ? `#${r.profileId}` : '—');
        return (
          <Space size={4}>
            <span>{title}</span>
            {r.profileLocked ? <Tag color="orange">已锁</Tag> : null}
          </Space>
        );
      },
    },
    {
      title: '材料',
      width: 90,
      render: (_, r) =>
        r.materialPackagePath ? <Tag color="success">已生成</Tag> : <Tag>未生成</Tag>,
    },
    {
      title: '操作',
      width: 88,
      fixed: 'right',
      render: (_, r) => (
        <Button type="link" size="small" onClick={() => void openDetail(r)}>
          办理
        </Button>
      ),
    },
  ];

  const onClassChange = (classId: number | string) => {
    const c = classMap.get(String(classId));
    if (!c) return;
    if (c.nums != null) form.setFieldValue('studentCount', c.nums);
    if (c.date_end) {
      const y = Number(String(c.date_end).slice(0, 4));
      if (y >= 2020 && y <= 2035) form.setFieldValue('policyYear', y);
    }
  };

  const applySearch = () => {
    setPage(1);
    setKeyword(keywordInput.trim());
  };

  return (
    <div className="training-page training-subsidy-declare-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4} style={{ marginBottom: 4 }}>
            申报补贴
          </Typography.Title>
          <Typography.Text type="secondary">
            一班一单 · 配方锁定 · 齐套上传 · 一键生成材料包（试点：来宾 / 玉林 / 南丹 × 岗位技能提升）
          </Typography.Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => invalidate()}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            新建补贴单
          </Button>
        </Space>
      </section>

      <TrainingProcessSteps current={8} />

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="办理路径：选班建单 → 按配方上传必填槽位（照片优先用跟班照片册）→ 一键生成 ZIP → 提交人社登记"
      />

      <div className="subsidy-stat-row">
        <div className="subsidy-stat-card">
          <div className="label">台账总数</div>
          <div className="value">{stats.total}</div>
        </div>
        <div className="subsidy-stat-card">
          <div className="label">草稿 / 核验</div>
          <div className="value">{stats.draft}</div>
        </div>
        <div className="subsidy-stat-card">
          <div className="label">就绪 / 已提交</div>
          <div className="value">{stats.ready}</div>
        </div>
        <div className="subsidy-stat-card">
          <div className="label">通过 / 拨付</div>
          <div className="value">{stats.done}</div>
        </div>
      </div>

      <Card size="small" title="补贴台账">
        <div className="subsidy-phase-chips">
          <Tag
            className="subsidy-phase-chip"
            color={!phase ? 'blue' : 'default'}
            onClick={() => {
              setPhase(undefined);
              setPage(1);
            }}
          >
            全部 {phaseCounts.__all ?? 0}
          </Tag>
          {PHASE_OPTS.map((p) => (
            <Tag
              key={p.value}
              className="subsidy-phase-chip"
              color={phase === p.value ? PHASE_COLOR[p.value] || 'blue' : 'default'}
              onClick={() => {
                setPhase(p.value);
                setPage(1);
              }}
            >
              {p.label} {phaseCounts[p.value] ?? 0}
            </Tag>
          ))}
        </div>

        <div className="subsidy-filter-bar">
          <Select
            allowClear
            placeholder="申报地市"
            style={{ width: 160 }}
            options={REGION_OPTS.map((r) => ({ value: r.code, label: r.label }))}
            value={regionFilter}
            onChange={(v) => {
              setRegionFilter(v);
              setPage(1);
            }}
          />
          <Select
            allowClear
            placeholder="政策年"
            style={{ width: 120 }}
            options={[2024, 2025, 2026].map((y) => ({ value: y, label: String(y) }))}
            value={yearFilter}
            onChange={(v) => {
              setYearFilter(v);
              setPage(1);
            }}
          />
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="搜索地市 / 备注"
            style={{ width: 220 }}
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            onPressEnter={applySearch}
          />
          <Button onClick={applySearch}>查询</Button>
        </div>

        <Table
          rowKey="id"
          size="small"
          loading={listQ.isLoading}
          columns={columns}
          dataSource={listQ.data?.items ?? []}
          scroll={{ x: 960 }}
          locale={{ emptyText: <Empty description="暂无补贴单，点击右上角新建" /> }}
          pagination={{
            current: page,
            pageSize,
            total: listQ.data?.total ?? 0,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, ps) => {
              setPage(p);
              setPageSize(ps);
            },
          }}
        />
      </Card>

      <Modal
        title="新建补贴单"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={createMu.isPending}
        destroyOnClose
        width={560}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ trainMode: 'skill_upgrade' }}
          onFinish={(v) => {
            const region = REGION_OPTS.find((r) => r.value === v.regionName);
            createMu.mutate({
              ...v,
              regionCode: region?.code || v.regionName,
            });
          }}
        >
          <Form.Item name="classId" label="班级" rules={[{ required: true, message: '请选择班级' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              loading={classesQ.isLoading}
              options={classOptions}
              placeholder="选择班级（一班一单）"
              notFoundContent={
                classesQ.isError
                  ? '班级列表加载失败'
                  : classesQ.isLoading
                    ? '加载中…'
                    : '暂无班级，请先在开班申报中建班'
              }
              onChange={onClassChange}
            />
          </Form.Item>
          <Form.Item name="trainMode" label="培训模式" rules={[{ required: true }]}>
            <Select options={[{ value: 'skill_upgrade', label: '岗位技能提升' }]} />
          </Form.Item>
          <Form.Item name="regionName" label="申报地市" extra="可空：从培训单城市带出；空且无法解析时会创建失败">
            <Select allowClear options={REGION_OPTS} placeholder="来宾 / 玉林 / 南丹" />
          </Form.Item>
          <Form.Item name="policyYear" label="政策年" extra="可空：按班级结业日推导">
            <InputNumber style={{ width: '100%' }} min={2020} max={2035} placeholder="如 2024" />
          </Form.Item>
          <Form.Item name="profileId" label="指定配方" extra="可空：按 地市×模式×政策年 自动匹配 active 配方">
            <Select
              allowClear
              options={profiles.map((p) => ({
                value: p.id,
                label: `${p.title}（${p.regionCode} · ${p.policyYear} · ${p.profileVersion}）`,
              }))}
            />
          </Form.Item>
          {matchedProfile && (
            <div className="profile-hint">
              将使用配方：<b>{matchedProfile.title}</b>
              （{matchedProfile.regionName} · {matchedProfile.policyYear} · {matchedProfile.profileVersion}）
              · {matchedProfile.slots?.length ?? 0} 个槽位
            </div>
          )}
          {!matchedProfile && watchedClassId && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 12 }}
              message="当前条件可能匹配不到 active 配方，建议指定配方或核对地市/政策年"
            />
          )}
          <div style={{ display: 'flex', gap: 12 }}>
            <Form.Item name="studentCount" label="人数" style={{ flex: 1 }}>
              <InputNumber style={{ width: '100%' }} min={0} />
            </Form.Item>
            <Form.Item name="subsidyAmount" label="补贴金额" style={{ flex: 1 }}>
              <InputNumber style={{ width: '100%' }} min={0} />
            </Form.Item>
          </div>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        width={880}
        title={
          detail ? (
            <Space>
              <span>{detail.className || `补贴单 #${detail.id}`}</span>
              <Tag color={PHASE_COLOR[detail.phase || ''] || 'default'}>{phaseLabel(detail.phase)}</Tag>
              {detail.profileLocked ? <Tag color="orange">配方已锁定</Tag> : <Tag>配方未锁定</Tag>}
            </Space>
          ) : (
            '详情'
          )
        }
        open={!!detail}
        onClose={() => {
          setDetail(null);
          setLastGen(null);
        }}
        destroyOnClose
      >
        {detail && (
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Descriptions size="small" column={3} bordered>
              <Descriptions.Item label="班级">{detail.className || detail.classId}</Descriptions.Item>
              <Descriptions.Item label="地市">{detail.regionName || detail.regionCode}</Descriptions.Item>
              <Descriptions.Item label="模式">{trainModeLabel(detail.trainMode)}</Descriptions.Item>
              <Descriptions.Item label="政策年">{detail.policyYear ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="人数">{detail.studentCount ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="金额">{money(detail.subsidyAmount)}</Descriptions.Item>
              <Descriptions.Item label="配方" span={2}>
                {profile?.title || detail.profileId || '—'}
                {detail.profileVersion ? ` · ${detail.profileVersion}` : ''}
              </Descriptions.Item>
              <Descriptions.Item label="材料包">
                {detail.materialPackagePath ? (
                  <Tooltip title={detail.materialPackagePath}>
                    <Tag color="success">已生成</Tag>
                  </Tooltip>
                ) : (
                  <Tag>尚未生成</Tag>
                )}
              </Descriptions.Item>
            </Descriptions>

            <div className="subsidy-detail-actions">
              {detail.phase === 'draft' && (
                <Button
                  loading={transitionMu.isPending}
                  onClick={() => transitionMu.mutate({ id: detail.id, toPhase: 'checking' })}
                >
                  进入核验
                </Button>
              )}
              {detail.phase === 'checking' && (
                <Button
                  loading={transitionMu.isPending}
                  onClick={() => transitionMu.mutate({ id: detail.id, toPhase: 'draft', remark: '退回草稿' })}
                >
                  退回草稿
                </Button>
              )}
              {detail.phase === 'ready' && (
                <Button
                  loading={transitionMu.isPending}
                  onClick={() => transitionMu.mutate({ id: detail.id, toPhase: 'checking', remark: '退回核验' })}
                >
                  退回核验
                </Button>
              )}
              {(detail.phase === 'draft' || detail.phase === 'checking' || detail.phase === 'ready') && (
                <Button
                  loading={transitionMu.isPending}
                  onClick={() =>
                    transitionMu.mutate({ id: detail.id, toPhase: 'submitted', remark: '提交人社' })
                  }
                >
                  提交人社
                </Button>
              )}
              {detail.phase === 'submitted' && (
                <>
                  <Button
                    type="primary"
                    loading={transitionMu.isPending}
                    onClick={() => transitionMu.mutate({ id: detail.id, toPhase: 'approved' })}
                  >
                    审批通过
                  </Button>
                  <Button
                    danger
                    loading={transitionMu.isPending}
                    onClick={() =>
                      transitionMu.mutate({ id: detail.id, toPhase: 'rejected', remark: '退回' })
                    }
                  >
                    退回
                  </Button>
                </>
              )}
              {detail.phase === 'rejected' && (
                <Button
                  loading={transitionMu.isPending}
                  onClick={() => transitionMu.mutate({ id: detail.id, toPhase: 'checking', remark: '重新核验' })}
                >
                  重新核验
                </Button>
              )}
              <Button
                type="primary"
                icon={<ThunderboltOutlined />}
                loading={genMu.isPending}
                onClick={() => genMu.mutate(detail.id)}
              >
                一键生成材料包
              </Button>
              {detail.materialPackagePath && (
                <Button
                  icon={<CloudDownloadOutlined />}
                  onClick={() => {
                    void downloadUrlFile(detail.materialPackagePath!, `补贴材料_${detail.id}.zip`).catch(() =>
                      message.error('下载失败，请检查文件路径'),
                    );
                  }}
                >
                  下载材料包
                </Button>
              )}
            </div>

            {lastGen && !lastGen.ok && (
              <Alert
                type="warning"
                showIcon
                message={lastGen.message || '缺项未打包'}
                description={
                  lastGen.missing?.length
                    ? `缺项：${lastGen.missing.join('、')}`
                    : '请补齐必填槽位后重试'
                }
              />
            )}

            <Card size="small" title="齐套进度">
              <Space direction="vertical" style={{ width: '100%' }} size={8}>
                <div>
                  必填槽位 {requiredOk}/{requiredTotal || '—'}
                  {profile ? ` · 配方「${profile.title}」` : ''}
                </div>
                <Progress
                  percent={requiredTotal ? packProgress : 0}
                  status={requiredTotal && requiredOk >= requiredTotal ? 'success' : 'active'}
                  size="small"
                />
              </Space>
            </Card>

            <Tabs
              items={[
                {
                  key: 'slots',
                  label: `材料槽位 (${slotRows.length})`,
                  children: (
                    <>
                      <Table
                        size="small"
                        pagination={false}
                        rowKey="slotCode"
                        loading={detailProfileQ.isLoading && !!detail.profileId}
                        dataSource={slotRows}
                        locale={{ emptyText: '暂无槽位：请确认配方已匹配，或先执行一次生成以写入齐套快照' }}
                        columns={[
                          {
                            title: '材料',
                            dataIndex: 'title',
                            render: (v, r) => (
                              <div>
                                <div>{v || r.slotCode}</div>
                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                  {r.slotCode}
                                  {r.renderer ? ` · ${RENDERER_LABEL[r.renderer] || r.renderer}` : ''}
                                </Typography.Text>
                              </div>
                            ),
                          },
                          {
                            title: '必填',
                            width: 64,
                            align: 'center',
                            render: (_, r) => (r.required ? <Tag color="red">必填</Tag> : <Tag>可选</Tag>),
                          },
                          {
                            title: '状态',
                            width: 100,
                            render: (_, r) => {
                              if (r.ok) return <Tag color="success">齐套</Tag>;
                              if (r.hasFile) return <Tag color="processing">已传</Tag>;
                              if (r.required) return <Tag color="warning">缺项</Tag>;
                              return <Tag>未传</Tag>;
                            },
                          },
                          {
                            title: '文件',
                            width: 160,
                            render: (_, r) =>
                              r.fileName ? (
                                <Tooltip title={r.fileName}>
                                  <div className="slot-file-name">{r.fileName}</div>
                                </Tooltip>
                              ) : r.source === 'follow_photo' ? (
                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                  跟班照片册
                                </Typography.Text>
                              ) : (
                                '—'
                              ),
                          },
                          {
                            title: '操作',
                            width: 110,
                            render: (_, r) =>
                              r.slotCode ? (
                                <Upload
                                  showUploadList={false}
                                  beforeUpload={(file) => {
                                    void trainApi
                                      .uploadSubsidySlotFile(detail.id, r.slotCode, file)
                                      .then(() => {
                                        message.success(`已上传：${r.title || r.slotCode}`);
                                        invalidate();
                                      })
                                      .catch((e: Error) => message.error(e.message || '上传失败'));
                                    return false;
                                  }}
                                >
                                  <Button size="small">
                                    {r.renderer === 'follow_photo' ? '补传册' : r.hasFile ? '重传' : '上传'}
                                  </Button>
                                </Upload>
                              ) : null,
                          },
                        ]}
                      />
                      <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
                        培训照片优先引用跟班「培训照片」生成结果；证书等槽位首期以人工上传为主。首次成功生成后锁定配方版本。
                      </Typography.Paragraph>
                    </>
                  ),
                },
                {
                  key: 'events',
                  label: `时间线 (${eventsQ.data?.length ?? 0})`,
                  children:
                    (eventsQ.data?.length ?? 0) === 0 ? (
                      <Empty description="暂无事件" />
                    ) : (
                      <Timeline
                        items={(eventsQ.data || []).map((e) => ({
                          children: (
                            <div>
                              <Space size={8} wrap>
                                <Tag>{e.action}</Tag>
                                {e.fromPhase || e.toPhase ? (
                                  <span>
                                    {phaseLabel(e.fromPhase)} → {phaseLabel(e.toPhase)}
                                  </span>
                                ) : null}
                              </Space>
                              {e.remark ? (
                                <div style={{ color: '#595959', fontSize: 13, marginTop: 4 }}>{e.remark}</div>
                              ) : null}
                              <div style={{ color: '#8c8c8c', fontSize: 12, marginTop: 2 }}>{e.createdAt}</div>
                            </div>
                          ),
                        }))}
                      />
                    ),
                },
              ]}
            />
          </Space>
        )}
      </Drawer>
    </div>
  );
}
