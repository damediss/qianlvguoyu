import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Segmented,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainSubsidy } from '../../api/training';
import type { TrainInfoClassListItem } from '../../types/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import { SubsidyClassWorkbench } from './subsidy/SubsidyClassWorkbench';
import './TrainingSubsidyDeclarePage.css';

/** 班级工种等级：5=五级 … 1=一级（与开班准备一致） */
const CLASS_LEVEL_LABELS: Record<number, string> = {
  5: '五级/初级工',
  4: '四级/中级工',
  3: '三级/高级工',
  2: '二级/技师',
  1: '一级/高级技师',
};

function formatClassLevel(level?: number | null) {
  if (level == null || Number.isNaN(Number(level))) return '—';
  const n = Number(level);
  return CLASS_LEVEL_LABELS[n] ?? `${n}级`;
}

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

const CLOSED_PHASES = new Set(['approved', 'granting', 'granted', 'closed']);

function phaseLabel(v?: string | null) {
  return PHASE_OPTS.find((p) => p.value === v)?.label || v || '—';
}

function money(v?: number | null) {
  if (v == null) return '—';
  return `¥${Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`;
}

function classSubsidyStatus(s?: TrainSubsidy | null): 'none' | 'open' | 'done' {
  if (!s) return 'none';
  if (CLOSED_PHASES.has(s.phase || '')) return 'done';
  return 'open';
}

/** 拉全部分页补贴单，供班级状态/看板（避免单页截断） */
async function fetchAllSubsidyItems(): Promise<{
  items: TrainSubsidy[];
  total: number;
  truncated: boolean;
}> {
  const pageSize = 200;
  const maxPages = 500; // 最多约 10 万条
  const items: TrainSubsidy[] = [];
  let page = 1;
  let total = Number.POSITIVE_INFINITY;
  while (items.length < total && page <= maxPages) {
    const res = await trainApi.pageSubsidies({ page, pageSize });
    const batch = res.items ?? [];
    items.push(...batch);
    total = typeof res.total === 'number' ? res.total : items.length;
    if (batch.length === 0) break;
    page += 1;
  }
  const truncated = items.length < total;
  return { items, total, truncated };
}

/** 状态筛选开启时拉全部分页班级再本地过滤，避免服务端分页 + 前端滤后 total 错乱 */
async function fetchAllInfoClasses(params: {
  keyword?: string;
  certFilter?: 'certified' | 'uncertified' | 'all';
}): Promise<{ list: TrainInfoClassListItem[]; total: number; truncated: boolean }> {
  const pageSize = 200;
  const maxPages = 500;
  const list: TrainInfoClassListItem[] = [];
  let page = 1;
  let total = Number.POSITIVE_INFINITY;
  while (list.length < total && page <= maxPages) {
    const res = await trainApi.pageInfoClassList({
      page,
      pageSize,
      keyword: params.keyword || undefined,
      certFilter: params.certFilter,
    });
    const batch = res.list ?? [];
    list.push(...batch);
    total = typeof res.total === 'number' ? res.total : list.length;
    if (batch.length === 0) break;
    page += 1;
  }
  const truncated = list.length < total;
  return { list, total: truncated ? list.length : total, truncated };
}

/** 运营流程 · 申报补贴：班级列表 → 本班材料工作台 */
export function TrainingSubsidyDeclarePage() {
  const qc = useQueryClient();
  const [mainTab, setMainTab] = useState<'classes' | 'ledger'>('classes');
  const [phase, setPhase] = useState<string | undefined>();
  const [regionFilter, setRegionFilter] = useState<string | undefined>();
  const [yearFilter, setYearFilter] = useState<number | undefined>();
  const [classFilter, setClassFilter] = useState<number | string | undefined>();
  const [keyword, setKeyword] = useState('');
  const [keywordInput, setKeywordInput] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [classPage, setClassPage] = useState(1);
  const [classPageSize, setClassPageSize] = useState(20);
  const [classKeyword, setClassKeyword] = useState('');
  const [classKwInput, setClassKwInput] = useState('');
  /** 默认只看已获证班（有证书回填） */
  const [certFilter, setCertFilter] = useState<'certified' | 'uncertified' | 'all'>('certified');
  const [subsidyStatusFilter, setSubsidyStatusFilter] = useState<'all' | 'none' | 'open' | 'done'>('all');
  const [createOpen, setCreateOpen] = useState(false);
  const [detail, setDetail] = useState<TrainSubsidy | null>(null);
  const [inWorkbench, setInWorkbench] = useState(false);
  const [workBusy, setWorkBusy] = useState(false);
  const [enteringClassId, setEnteringClassId] = useState<number | string | null>(null);
  const [form] = Form.useForm();
  const watchedRegion = Form.useWatch('regionName', form);
  const watchedYear = Form.useWatch('policyYear', form);
  const watchedProfileId = Form.useWatch('profileId', form);
  const watchedClassId = Form.useWatch('classId', form);

  const listQ = useQuery({
    queryKey: ['subsidies', phase, regionFilter, yearFilter, classFilter, keyword, page, pageSize],
    queryFn: () =>
      trainApi.pageSubsidies({
        page,
        pageSize,
        phase,
        regionCode: regionFilter,
        policyYear: yearFilter,
        classId: classFilter,
        keyword: keyword || undefined,
      }),
    enabled: !inWorkbench && mainTab === 'ledger',
  });

  const statsQ = useQuery({
    queryKey: ['subsidies-stats'],
    queryFn: async () => {
      const res = await fetchAllSubsidyItems();
      return {
        items: res.items,
        total: res.total,
        page: 1,
        pageSize: res.items.length,
        truncated: res.truncated,
      };
    },
    enabled: !inWorkbench,
  });

  const classListQ = useQuery({
    queryKey: [
      'subsidy-class-list',
      classPage,
      classPageSize,
      classKeyword,
      certFilter,
      subsidyStatusFilter,
    ],
    queryFn: async () => {
      if (subsidyStatusFilter === 'all') {
        return trainApi.pageInfoClassList({
          page: classPage,
          pageSize: classPageSize,
          keyword: classKeyword || undefined,
          certFilter,
        });
      }
      // 状态筛：全量班级 + 本地分页（依赖 subsidies-stats 映射）
      return fetchAllInfoClasses({
        keyword: classKeyword || undefined,
        certFilter,
      });
    },
    enabled: !inWorkbench,
  });

  const classesPickQ = useQuery({
    queryKey: ['info-classes-brief-all'],
    queryFn: () => fetchAllInfoClasses({ certFilter: 'all' }),
    enabled: createOpen,
  });

  const profilesQ = useQuery({
    queryKey: ['subsidy-profiles-active'],
    queryFn: () => trainApi.listSubsidyPackProfiles({ status: 'active', trainMode: 'skill_upgrade' }),
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['subsidies'] });
    void qc.invalidateQueries({ queryKey: ['subsidies-stats'] });
    void qc.invalidateQueries({ queryKey: ['subsidy-class-list'] });
  };

  const openDetail = async (row: TrainSubsidy) => {
    try {
      const fresh = await trainApi.getSubsidy(row.id);
      setDetail(fresh);
      return fresh;
    } catch (e) {
      message.error((e as Error).message || '加载补贴单失败，请稍后重试');
      throw e;
    }
  };

  const enterWorkbench = async (row: TrainSubsidy) => {
    const fresh = await openDetail(row);
    setInWorkbench(true);
    return fresh;
  };

  const exitWorkbench = () => {
    setInWorkbench(false);
    setDetail(null);
    invalidate();
  };

  const openClassWorkbench = async (classId: number | string) => {
    setWorkBusy(true);
    setEnteringClassId(classId);
    try {
      const row = await trainApi.ensureClassSubsidy(classId);
      setClassFilter(classId);
      invalidate();
      await enterWorkbench(row);
      message.success(`已进入材料工作台：${row.className || `班级 #${classId}`}`);
    } catch (e) {
      const msg = (e as Error).message || '打开本班补贴失败';
      message.error(msg);
      setCreateOpen(true);
      form.setFieldsValue({ classId, trainMode: 'skill_upgrade' });
      message.info('请手工选择申报地市与政策年后创建');
    } finally {
      setWorkBusy(false);
      setEnteringClassId(null);
    }
  };

  const openDetailById = async (id: number | string) => {
    try {
      const fresh = await trainApi.getSubsidy(id);
      if (fresh.classId != null) setClassFilter(fresh.classId);
      await enterWorkbench(fresh);
    } catch (e) {
      message.error((e as Error).message || '打开补贴单失败');
    }
  };

  const applySubsidyFocus = useCallback(() => {
    const focus = trainApi.consumeSubsidyDeclareFocus();
    if (!focus) return;
    if (focus.classId != null) {
      setClassFilter(focus.classId);
      setClassKwInput(String(focus.classId));
    }
    if (focus.subsidyId != null) {
      void openDetailById(focus.subsidyId);
      return;
    }
    if (focus.openCreate && focus.classId != null) {
      setCreateOpen(true);
      form.setFieldsValue({ classId: focus.classId, trainMode: 'skill_upgrade' });
      return;
    }
    if (focus.classId != null) {
      void openClassWorkbench(focus.classId);
    }
    // openClassWorkbench / openDetailById 稳定引用不足，刻意不列入 deps
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form]);

  useEffect(() => {
    applySubsidyFocus();
    const onFocusEvent = () => applySubsidyFocus();
    window.addEventListener(trainApi.SUBSIDY_DECLARE_FOCUS_EVENT, onFocusEvent);
    return () => window.removeEventListener(trainApi.SUBSIDY_DECLARE_FOCUS_EVENT, onFocusEvent);
  }, [applySubsidyFocus]);

  const createMu = useMutation({
    mutationFn: trainApi.createSubsidy,
    onSuccess: (row) => {
      message.success('已创建补贴单');
      setCreateOpen(false);
      form.resetFields();
      if (row.classId != null) setClassFilter(row.classId);
      invalidate();
      void enterWorkbench(row).catch(() => undefined);
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const classRows = useMemo(() => classesPickQ.data?.list ?? [], [classesPickQ.data]);
  const listClassRows = useMemo(() => classListQ.data?.list ?? [], [classListQ.data]);

  const classMap = useMemo(() => {
    const map = new Map<string, TrainInfoClassListItem>();
    for (const c of classRows) map.set(String(c.id), c);
    for (const c of listClassRows) map.set(String(c.id), c);
    return map;
  }, [classRows, listClassRows]);

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

  const subsidyByClass = useMemo(() => {
    const map = new Map<string, TrainSubsidy>();
    for (const s of statsQ.data?.items ?? []) {
      if (s.classId == null) continue;
      const key = String(s.classId);
      const prev = map.get(key);
      if (!prev) {
        map.set(key, s);
        continue;
      }
      if (CLOSED_PHASES.has(prev.phase || '') && !CLOSED_PHASES.has(s.phase || '')) {
        map.set(key, s);
      }
    }
    return map;
  }, [statsQ.data]);

  const displayClassRows = useMemo(() => {
    const rows = listClassRows;
    if (subsidyStatusFilter === 'all') return rows;
    const filtered = rows.filter((c) => {
      const st = classSubsidyStatus(subsidyByClass.get(String(c.id)));
      return st === subsidyStatusFilter;
    });
    const start = (classPage - 1) * classPageSize;
    return filtered.slice(start, start + classPageSize);
  }, [listClassRows, subsidyByClass, subsidyStatusFilter, classPage, classPageSize]);

  const displayClassTotal = useMemo(() => {
    if (subsidyStatusFilter === 'all') {
      return classListQ.data?.total ?? 0;
    }
    return listClassRows.filter((c) => {
      const st = classSubsidyStatus(subsidyByClass.get(String(c.id)));
      return st === subsidyStatusFilter;
    }).length;
  }, [subsidyStatusFilter, classListQ.data?.total, listClassRows, subsidyByClass]);

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
        <Button type="link" style={{ padding: 0, height: 'auto' }} onClick={() => void enterWorkbench(r)}>
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
    { title: '政策年', dataIndex: 'policyYear', width: 80, align: 'center' },
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
        <Button type="link" size="small" onClick={() => void enterWorkbench(r)}>
          办理
        </Button>
      ),
    },
  ];

  const classColumns: ColumnsType<TrainInfoClassListItem> = [
    {
      title: '班级',
      dataIndex: 'title',
      ellipsis: true,
      render: (v, r) => (
        <Button
          type="link"
          style={{ padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }}
          loading={enteringClassId != null && String(enteringClassId) === String(r.id)}
          onClick={() => void openClassWorkbench(r.id)}
        >
          {v || `班级 #${r.id}`}
        </Button>
      ),
    },
    { title: '企业', dataIndex: 'company_name', ellipsis: true, width: 160, render: (v) => v || '—' },
    { title: '工种', dataIndex: 'work_type_title', ellipsis: true, width: 140, render: (v) => v || '—' },
    {
      title: '等级',
      dataIndex: 'work_type_level',
      width: 120,
      ellipsis: true,
      render: (v?: number | null) => formatClassLevel(v),
    },
    {
      title: '获证/人数',
      key: 'certified_nums',
      width: 96,
      align: 'center',
      render: (_, r) => {
        const total = r.nums ?? 0;
        const certified = r.certified_nums ?? 0;
        return `${certified}/${total}`;
      },
    },
    {
      title: '培训期',
      width: 180,
      render: (_, r) =>
        r.date_start || r.date_end ? `${r.date_start || '—'} ~ ${r.date_end || '—'}` : '—',
    },
    {
      title: '补贴进度',
      width: 120,
      render: (_, r) => {
        const s = subsidyByClass.get(String(r.id));
        const st = classSubsidyStatus(s);
        if (st === 'none') return <Tag>未建单</Tag>;
        if (st === 'done') return <Tag color="success">{phaseLabel(s?.phase)}</Tag>;
        return <Tag color="processing">{phaseLabel(s?.phase)}</Tag>;
      },
    },
    {
      title: '材料包',
      width: 88,
      render: (_, r) => {
        const s = subsidyByClass.get(String(r.id));
        return s?.materialPackagePath ? <Tag color="success">已生成</Tag> : <Tag>未生成</Tag>;
      },
    },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      render: (_, r) => (
        <Button
          type="link"
          size="small"
          loading={enteringClassId != null && String(enteringClassId) === String(r.id)}
          onClick={() => void openClassWorkbench(r.id)}
        >
          进入材料工作台
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
    void trainApi
      .getClassSubsidy(classId)
      .then((existing) => {
        if (existing) {
          message.warning(`该班已有进行中补贴单 #${existing.id}，将打开已有单据`);
          setCreateOpen(false);
          form.resetFields();
          setClassFilter(classId);
          void enterWorkbench(existing).catch(() => undefined);
        }
      })
      .catch((e: Error) => {
        message.error(e.message || '查询本班补贴单失败');
      });
  };

  const applySearch = () => {
    setPage(1);
    setKeyword(keywordInput.trim());
  };

  const applyClassSearch = () => {
    setClassPage(1);
    setClassKeyword(classKwInput.trim());
  };

  if (inWorkbench && detail) {
    return (
      <SubsidyClassWorkbench
        subsidy={detail}
        onBack={exitWorkbench}
        onSubsidyChange={setDetail}
      />
    );
  }

  return (
    <div className="training-page training-subsidy-declare-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4} style={{ marginBottom: 4 }}>
            申报补贴
          </Typography.Title>
          <Typography.Text type="secondary">
            先选班级进入材料工作台：汇集本班跟班/结业材料 → 补齐盖章件 → 导出申报包
          </Typography.Text>
        </div>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              invalidate();
              void classListQ.refetch();
            }}
          >
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
        message="每个班材料不同，请逐班办理。点「进入材料工作台」自动打开/创建本班补贴单。"
      />

      {statsQ.data?.truncated || (classListQ.data as { truncated?: boolean } | undefined)?.truncated ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="数据量过大，看板/状态筛选可能未加载全部记录"
          description={
            statsQ.data?.truncated
              ? `补贴单已加载 ${statsQ.data.items.length} / ${statsQ.data.total}，进度标签可能不全。`
              : '班级列表在状态筛选下未全部加载，请缩小筛选条件后重试。'
          }
        />
      ) : null}

      <div className="subsidy-stat-row">
        <div className="subsidy-stat-card">
          <div className="label">班级总数</div>
          <div className="value">{classListQ.data?.total ?? '—'}</div>
        </div>
        <div className="subsidy-stat-card">
          <div className="label">已建补贴单</div>
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
      </div>

      <Tabs
        activeKey={mainTab}
        onChange={(k) => setMainTab(k as 'classes' | 'ledger')}
        items={[
          {
            key: 'classes',
            label: '班级办理',
            children: (
              <Card
                size="small"
                title={
                  certFilter === 'certified'
                    ? '已获证班级'
                    : certFilter === 'uncertified'
                      ? '未获证班级'
                      : '全部班级'
                }
              >
                <div className="subsidy-filter-bar">
                  <Segmented
                    value={certFilter}
                    onChange={(v) => {
                      setCertFilter(v as 'certified' | 'uncertified' | 'all');
                      setClassPage(1);
                    }}
                    options={[
                      { value: 'certified', label: '已获证班' },
                      { value: 'uncertified', label: '未获证班' },
                      { value: 'all', label: '全部' },
                    ]}
                  />
                  <Input
                    allowClear
                    prefix={<SearchOutlined />}
                    placeholder="搜索班级 / 企业 / 工种"
                    style={{ width: 280 }}
                    value={classKwInput}
                    onChange={(e) => setClassKwInput(e.target.value)}
                    onPressEnter={applyClassSearch}
                  />
                  <Button type="primary" onClick={applyClassSearch}>
                    查询
                  </Button>
                  <Select
                    style={{ width: 160 }}
                    value={subsidyStatusFilter}
                    onChange={(v) => {
                      setSubsidyStatusFilter(v as 'all' | 'none' | 'open' | 'done');
                      setClassPage(1);
                    }}
                    options={[
                      { value: 'all', label: '全部状态' },
                      { value: 'none', label: '未建单' },
                      { value: 'open', label: '办理中' },
                      { value: 'done', label: '已就绪/提交' },
                    ]}
                  />
                </div>
                <Table
                  rowKey="id"
                  size="middle"
                  loading={classListQ.isLoading || statsQ.isLoading || workBusy}
                  columns={classColumns}
                  dataSource={displayClassRows}
                  scroll={{ x: 1100 }}
                  locale={{
                    emptyText: (
                      <Empty
                        description={
                          certFilter === 'certified'
                            ? '暂无已获证班级，请先在毕业典礼完成证书回填'
                            : certFilter === 'uncertified'
                              ? '暂无未获证班级'
                              : '暂无班级，请先在开班申报中建班'
                        }
                      />
                    ),
                  }}
                  onRow={(r) => ({
                    onDoubleClick: () => void openClassWorkbench(r.id),
                    style: { cursor: 'pointer' },
                  })}
                  pagination={{
                    current: classPage,
                    pageSize: classPageSize,
                    total: displayClassTotal,
                    showSizeChanger: true,
                    showTotal: (t) => `共 ${t} 个班级`,
                    onChange: (p, ps) => {
                      if (typeof ps === 'number' && ps > 0 && ps !== classPageSize) {
                        setClassPageSize(ps);
                        setClassPage(1);
                      } else {
                        setClassPage(p);
                      }
                    },
                  }}
                />
              </Card>
            ),
          },
          {
            key: 'ledger',
            label: '补贴台账',
            children: (
              <Card size="small" title="跨班补贴台账">
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
                    placeholder="搜索班名 / 地市 / 备注"
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
                  locale={{ emptyText: <Empty description="暂无补贴单，请从「班级办理」进入" /> }}
                  pagination={{
                    current: page,
                    pageSize,
                    total: listQ.data?.total ?? 0,
                    showSizeChanger: true,
                    showTotal: (t) => `共 ${t} 条`,
                    onChange: (p, ps) => {
                      if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                        setPageSize(ps);
                        setPage(1);
                      } else {
                        setPage(p);
                      }
                    },
                  }}
                />
              </Card>
            ),
          },
        ]}
      />

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
              loading={classesPickQ.isLoading}
              options={classOptions}
              placeholder="选择班级（一班一单）"
              notFoundContent={
                classesPickQ.isError
                  ? '班级列表加载失败'
                  : classesPickQ.isLoading
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
            <Form.Item
              name="studentCount"
              label="人数"
              style={{ flex: 1 }}
              extra="可空：优先用班级计划人数；提交时若仍空则按花名册在册学员数"
            >
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
    </div>
  );
}
