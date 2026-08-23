import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Breadcrumb,
  Button,
  Card,
  Drawer,
  Input,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  DownloadOutlined,
  RightOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type {
  DemandPublicData,
  ExamScoreImportClassOption,
  ExamScoreImportClassPeople,
  ExamScoreImportItem,
  ExamScoreImportPreview,
  ExamScoreImportRosterGap,
} from '../../types/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import './TrainingFollowExamPage.css';

const WRITABLE = new Set([
  'MATCHED',
  'MATCHED_NAME_WARN',
  'MATCHED_JOB_WARN',
  'MULTI_CLASS',
]);

const STATUS_LABEL: Record<string, { text: string; color?: string }> = {
  MATCHED: { text: '可写入', color: 'success' },
  MATCHED_NAME_WARN: { text: '姓名不一致', color: 'warning' },
  MATCHED_JOB_WARN: { text: '工种/等级不一致', color: 'warning' },
  MULTI_CLASS: { text: '多班命中', color: 'processing' },
  UNMATCHED_NOT_IN_CLASS: { text: '未在本班' },
  NO_ID_CARD: { text: '无证件号', color: 'error' },
  EMPTY_SCORE: { text: '空分' },
  SHEET_DUP_CONFLICT: { text: '表内冲突', color: 'error' },
};

type CompanyRow = {
  companyId: string;
  companyName: string;
  classCount: number;
  trainCount: number;
  unassigned?: boolean;
};

/** 班级/培训未挂企业时的虚拟入口 */
const UNASSIGNED_COMPANY_ID = '0';

/** 企业/组织雪花 ID：一律按字符串比较，禁止 Number() */
function toIdKey(value: unknown): string | null {
  if (value == null || value === '') return null;
  const s = String(value).trim();
  if (!s || s === 'null' || s === 'undefined' || s === 'NaN') return null;
  if (s === '0') return null;
  return s;
}

type WritableRow = {
  key: string;
  peopleId: string;
  rowNo: number;
  name?: string;
  idCard?: string;
  ticketNo?: string;
  theoryScore?: number | null;
  skillScore?: number | null;
  examScore?: number | null;
  performanceScore?: number | null;
  theoryFallback?: boolean;
  status: string;
  classTitle?: string;
  rosterName?: string;
  oldExamScore?: number | null;
  oldRealScore?: number | null;
  nameMismatch?: boolean;
  jobMismatch?: boolean;
  mergeNote?: string | null;
  examTimeMissing?: boolean;
};

function flattenWritable(items: ExamScoreImportItem[]): WritableRow[] {
  const rows: WritableRow[] = [];
  for (const item of items) {
    if (!WRITABLE.has(item.status)) continue;
    for (const t of item.targets ?? []) {
      rows.push({
        key: String(t.peopleId),
        peopleId: String(t.peopleId),
        rowNo: item.rowNo,
        name: item.name,
        idCard: item.idCard,
        ticketNo: item.ticketNo,
        theoryScore: item.theoryScore,
        skillScore: item.skillScore,
        examScore: item.examScore,
        performanceScore: item.performanceScore,
        theoryFallback: item.theoryFallback,
        status: item.status,
        classTitle: t.classTitle,
        rosterName: t.rosterName,
        oldExamScore: t.oldExamScore,
        oldRealScore: t.oldRealScore,
        nameMismatch: t.nameMismatch,
        jobMismatch: t.jobMismatch,
        mergeNote: item.mergeNote,
        examTimeMissing: item.examTimeMissing,
      });
    }
  }
  return rows;
}

function statusTag(code?: string) {
  const meta = STATUS_LABEL[code ?? ''] ?? { text: code || '-' };
  return <Tag color={meta.color}>{meta.text}</Tag>;
}

const PASS_LINE = 60;

function renderExamScoreCell(v: number | string | null | undefined) {
  if (v == null || v === '') return '—';
  const n = typeof v === 'number' ? v : Number(v);
  if (Number.isNaN(n)) return v;
  if (n < PASS_LINE) {
    return <span className="exam-score-fail">{n}</span>;
  }
  return <span className="exam-score-pass">{n}</span>;
}

function examResultTag(status?: string) {
  if (status === 'PASS') return <Tag color="success">合格</Tag>;
  if (status === 'FAIL') return <Tag color="error">不合格</Tag>;
  if (status === 'ABSENT') return <Tag>缺考</Tag>;
  return <Tag>—</Tag>;
}

function examRosterRowClass(status?: string) {
  if (status === 'PASS') return 'exam-roster-pass-row';
  if (status === 'FAIL') return 'exam-roster-fail-row';
  if (status === 'ABSENT') return 'exam-roster-absent-row';
  return '';
}

/** 百分比展示：分母为 0 时显示 — */
function formatRate(numerator: number, denominator: number, digits = 1) {
  if (denominator <= 0) return '—';
  const pct = (numerator / denominator) * 100;
  return `${pct.toFixed(digits)}%`;
}

function buildExamRosterRateSummary(stats: {
  total: number;
  passed: number;
  failed: number;
  absent: number;
  theoryPass: number;
  theoryFail: number;
  theoryAbsent: number;
  skillPass: number;
  skillFail: number;
  skillAbsent: number;
}) {
  const attended = stats.passed + stats.failed;
  return {
    /** 合格 / 合计 */
    passRate: formatRate(stats.passed, stats.total),
    /** 参考（有成绩） / 合计 */
    attendRate: formatRate(attended, stats.total),
    /** 缺考 / 合计 */
    absentRate: formatRate(stats.absent, stats.total),
    /** 合格 / 参考人数（剔除缺考） */
    passAmongAttended: formatRate(stats.passed, attended),
    theoryPassRate: formatRate(stats.theoryPass, stats.theoryPass + stats.theoryFail),
    skillPassRate: formatRate(stats.skillPass, stats.skillPass + stats.skillFail),
  };
}

export function TrainingFollowExamPage() {
  const queryClient = useQueryClient();
  const [companyKeyword, setCompanyKeyword] = useState('');
  const [activeCompanyId, setActiveCompanyId] = useState<string | null>(null);
  const [classKeyword, setClassKeyword] = useState('');
  const [trainTitleFilters, setTrainTitleFilters] = useState<string[]>([]);
  const [companyPage, setCompanyPage] = useState(1);
  const [companyPageSize, setCompanyPageSize] = useState(20);
  const [classPage, setClassPage] = useState(1);
  const [classPageSize, setClassPageSize] = useState(10);
  const [selectedClassIds, setSelectedClassIds] = useState<string[]>([]);
  const [rosterClass, setRosterClass] = useState<ExamScoreImportClassOption | null>(null);
  const [rosterKeyword, setRosterKeyword] = useState('');
  const [rosterPage, setRosterPage] = useState(1);
  const [rosterPageSize, setRosterPageSize] = useState(50);
  const [rosterResultFilter, setRosterResultFilter] = useState<string | null>(null);
  const [preview, setPreview] = useState<ExamScoreImportPreview | null>(null);
  const [lastCommit, setLastCommit] = useState<{
    written: number;
    overwritten: number;
    skipped: number;
  } | null>(null);
  const [importing, setImporting] = useState(false);
  const [committing, setCommitting] = useState(false);
  const [uploadFileName, setUploadFileName] = useState<string | null>(null);
  const pendingUploadFilesRef = useRef<File[]>([]);

  /** 与开班准备同源：企业/培训需求全量；班级用于统计已开班数 */
  const publicQuery = useQuery({
    queryKey: ['exam-score-import-public'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'traininfo']),
  });
  const publicData = publicQuery.data as DemandPublicData | undefined;

  const allClassesQuery = useQuery({
    queryKey: ['exam-score-import-classes-all'],
    queryFn: () => trainApi.listExamScoreImportClasses({}),
  });

  const companyClassesQuery = useQuery({
    queryKey: ['exam-score-import-classes', activeCompanyId],
    queryFn: () =>
      trainApi.listExamScoreImportClasses({
        companyId: activeCompanyId!,
      }),
    enabled: activeCompanyId != null,
  });

  const classPeopleQuery = useQuery({
    queryKey: [
      'exam-score-import-class-people',
      rosterClass?.id,
      rosterPage,
      rosterPageSize,
      rosterKeyword,
      rosterResultFilter,
    ],
    queryFn: () =>
      trainApi.pageExamScoreImportClassPeople(rosterClass!.id, {
        page: rosterPage,
        pageSize: rosterPageSize,
        keyword: rosterKeyword.trim() || undefined,
        examResultStatus: rosterResultFilter || undefined,
      }),
    enabled: rosterClass != null,
  });

  const companies = useMemo((): CompanyRow[] => {
    const classCountByCompany = new Map<string, number>();
    const trainInfoCompany = new Map<string, string>();
    for (const t of publicData?.traininfo_list ?? []) {
      const tid = toIdKey(t.id);
      const cid = toIdKey(t.company_id);
      if (tid && cid) {
        trainInfoCompany.set(tid, cid);
      }
    }
    let unassignedClassCount = 0;
    for (const c of allClassesQuery.data ?? []) {
      let cid = toIdKey(c.companyId);
      if (!cid) {
        cid = trainInfoCompany.get(toIdKey(c.trainInfoId) ?? '') ?? null;
      }
      if (!cid) {
        unassignedClassCount += 1;
        continue;
      }
      classCountByCompany.set(cid, (classCountByCompany.get(cid) ?? 0) + 1);
    }

    const trainCountByCompany = new Map<string, number>();
    let unassignedTrainCount = 0;
    for (const t of publicData?.traininfo_list ?? []) {
      const cid = toIdKey(t.company_id);
      if (!cid) {
        unassignedTrainCount += 1;
        continue;
      }
      trainCountByCompany.set(cid, (trainCountByCompany.get(cid) ?? 0) + 1);
    }

    const map = new Map<string, CompanyRow>();
    for (const c of publicData?.company_list ?? []) {
      const cid = toIdKey(c.id);
      if (!cid) continue;
      map.set(cid, {
        companyId: cid,
        companyName: (c.title || `企业#${cid}`).trim(),
        classCount: classCountByCompany.get(cid) ?? 0,
        trainCount: trainCountByCompany.get(cid) ?? 0,
      });
    }

    for (const [cid, count] of classCountByCompany) {
      if (map.has(cid)) continue;
      const sample = (allClassesQuery.data ?? []).find((x) => toIdKey(x.companyId) === cid);
      map.set(cid, {
        companyId: cid,
        companyName: (sample?.companyName || `企业#${cid}`).trim(),
        classCount: count,
        trainCount: trainCountByCompany.get(cid) ?? 0,
      });
    }
    for (const [cid, count] of trainCountByCompany) {
      if (map.has(cid)) continue;
      const sample = (publicData?.traininfo_list ?? []).find((x) => toIdKey(x.company_id) === cid);
      map.set(cid, {
        companyId: cid,
        companyName: (sample?.company_name || `企业#${cid}`).trim(),
        classCount: classCountByCompany.get(cid) ?? 0,
        trainCount: count,
      });
    }

    const rows = [...map.values()].sort((a, b) =>
      a.companyName.localeCompare(b.companyName, 'zh-CN'),
    );
    if (unassignedClassCount > 0 || unassignedTrainCount > 0) {
      rows.unshift({
        companyId: UNASSIGNED_COMPANY_ID,
        companyName: '未归属企业（请补全企业后重试）',
        classCount: unassignedClassCount,
        trainCount: unassignedTrainCount,
        unassigned: true,
      });
    }
    return rows;
  }, [publicData, allClassesQuery.data]);

  const filteredCompanies = useMemo(() => {
    const kw = companyKeyword.trim().toLowerCase();
    if (!kw) return companies;
    return companies.filter(
      (c) => c.companyName.toLowerCase().includes(kw) || c.companyId.includes(kw),
    );
  }, [companies, companyKeyword]);

  /** 优先用企业筛选接口；若因精度问题为空则回退本地按字符串归类 */
  const companyClasses = useMemo(() => {
    const fromApi = companyClassesQuery.data ?? [];
    if (activeCompanyId == null) return [];
    if (fromApi.length > 0) return fromApi;

    const trainInfoCompany = new Map<string, string>();
    for (const t of publicData?.traininfo_list ?? []) {
      const tid = toIdKey(t.id);
      const cid = toIdKey(t.company_id);
      if (tid && cid) trainInfoCompany.set(tid, cid);
    }
    return (allClassesQuery.data ?? []).filter((c) => {
      let cid = toIdKey(c.companyId);
      if (!cid) cid = trainInfoCompany.get(toIdKey(c.trainInfoId) ?? '') ?? null;
      if (activeCompanyId === UNASSIGNED_COMPANY_ID) {
        return !cid;
      }
      return cid === activeCompanyId;
    });
  }, [activeCompanyId, companyClassesQuery.data, allClassesQuery.data, publicData]);

  const trainTitleFilterOptions = useMemo(() => {
    const set = new Set<string>();
    let hasEmpty = false;
    for (const c of companyClasses) {
      const t = (c.trainTitle || '').trim();
      if (!t) {
        hasEmpty = true;
        continue;
      }
      set.add(t);
    }
    const opts = [...set].sort((a, b) => a.localeCompare(b, 'zh-CN')).map((t) => ({
      text: t,
      value: t,
    }));
    if (hasEmpty) {
      opts.push({ text: '（未关联项目）', value: '__EMPTY__' });
    }
    return opts;
  }, [companyClasses]);

  const filteredClasses = useMemo(() => {
    let list = companyClasses;
    if (trainTitleFilters.length > 0) {
      const allowEmpty = trainTitleFilters.includes('__EMPTY__');
      const titles = new Set(trainTitleFilters.filter((t) => t !== '__EMPTY__'));
      list = list.filter((c) => {
        const t = (c.trainTitle || '').trim();
        if (!t) return allowEmpty;
        return titles.has(t);
      });
    }
    const kw = classKeyword.trim().toLowerCase();
    if (!kw) return list;
    return list.filter((c) => {
      const blob = [c.title, c.trainTitle, c.workTypeTitle].filter(Boolean).join(' ').toLowerCase();
      return blob.includes(kw);
    });
  }, [companyClasses, classKeyword, trainTitleFilters]);

  const activeCompany = useMemo(
    () => companies.find((c) => c.companyId === activeCompanyId) ?? null,
    [companies, activeCompanyId],
  );

  const writableRows = useMemo(() => flattenWritable(preview?.items ?? []), [preview]);
  const unmatchedItems = useMemo(
    () =>
      (preview?.items ?? []).filter(
        (i) => i.status === 'UNMATCHED_NOT_IN_CLASS' || i.status === 'NO_ID_CARD',
      ),
    [preview],
  );
  const conflictItems = useMemo(
    () => (preview?.items ?? []).filter((i) => i.status === 'SHEET_DUP_CONFLICT'),
    [preview],
  );
  const emptyScoreItems = useMemo(
    () => (preview?.items ?? []).filter((i) => i.status === 'EMPTY_SCORE'),
    [preview],
  );

  const enterCompany = (companyId: string) => {
    setActiveCompanyId(companyId);
    setClassKeyword('');
    setTrainTitleFilters([]);
    setClassPage(1);
    setPreview(null);
    setLastCommit(null);
    setUploadFileName(null);
    setSelectedClassIds([]);
  };

  const backToCompanies = () => {
    setActiveCompanyId(null);
    setPreview(null);
    setLastCommit(null);
    setUploadFileName(null);
    setSelectedClassIds([]);
    setClassKeyword('');
    setTrainTitleFilters([]);
    setClassPage(1);
    setCompanyPage(1);
  };

  useEffect(() => {
    if (activeCompanyId == null) return;
    if (!companyClassesQuery.isSuccess) return;
    if (companyClasses.length === 0) {
      setSelectedClassIds([]);
      return;
    }
    setSelectedClassIds((prev) =>
      prev.length === 0 ? companyClasses.map((c) => String(c.id)) : prev,
    );
  }, [activeCompanyId, companyClasses, companyClassesQuery.isSuccess]);

  const targetClassIds = selectedClassIds;
  const canConfirmWrite =
    targetClassIds.length > 0 && writableRows.length > 0 && preview != null && !committing && !importing;

  const runPreviewImport = async (files: File[]) => {
    if (targetClassIds.length === 0) {
      message.warning('请先勾选班级后再上传成绩表');
      return;
    }
    const list = files.filter(Boolean);
    if (list.length === 0) {
      message.warning('请选择成绩表文件');
      return;
    }
    setImporting(true);
    setPreview(null);
    setLastCommit(null);
    setUploadFileName(
      list.length === 1 ? list[0].name : `${list[0].name} 等${list.length}个文件`,
    );
    try {
      const data = await trainApi.previewExamScoreImport(targetClassIds, list);
      setPreview(data);
      const peopleIds = flattenWritable(data.items).map((r) => r.peopleId);
      if (peopleIds.length === 0) {
        message.warning('没有可写入的成绩行，请查看匹配明细后再确认');
      } else {
        message.success(
          `已解析 ${list.length} 个成绩表，可写入 ${peopleIds.length} 人，请确认后写入`,
        );
      }
    } catch (e) {
      setUploadFileName(null);
      message.error((e as Error).message || '解析失败');
    } finally {
      setImporting(false);
    }
  };

  const runConfirmWrite = async () => {
    if (!preview) return;
    if (targetClassIds.length === 0) {
      message.warning('请先勾选班级');
      return;
    }
    const peopleIds = writableRows.map((r) => r.peopleId);
    if (peopleIds.length === 0) {
      message.warning('没有可写入的成绩行');
      return;
    }
    setCommitting(true);
    try {
      const res = await trainApi.commitExamScoreImport(preview.previewId, peopleIds);
      setLastCommit({
        written: res.written,
        overwritten: res.overwritten,
        skipped: res.skipped,
      });
      message.success(
        `写入完成：成功 ${res.written} 人，覆盖 ${res.overwritten}，跳过 ${res.skipped}`,
      );
      void queryClient.invalidateQueries({ queryKey: ['exam-score-import-classes'] });
      void queryClient.invalidateQueries({ queryKey: ['exam-score-import-classes-all'] });
      if (rosterClass != null) {
        void classPeopleQuery.refetch();
      }
    } catch (e) {
      message.error((e as Error).message || '写入失败');
    } finally {
      setCommitting(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const blob = await trainApi.downloadExamScoreImportTemplate();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = '成绩导入标准模板.xlsx';
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('模板下载失败');
    }
  };

  const companyColumns: ColumnsType<CompanyRow> = [
    {
      title: '企业名称',
      dataIndex: 'companyName',
      ellipsis: true,
      render: (v, row) =>
        row.unassigned ? <Typography.Text type="warning">{v}</Typography.Text> : v,
    },
    { title: '班级数', dataIndex: 'classCount', width: 100 },
    { title: '培训项目数', dataIndex: 'trainCount', width: 120 },
    {
      title: '操作',
      key: 'act',
      width: 120,
      render: (_, row) => (
        <Button type="link" size="small" onClick={() => enterCompany(row.companyId)}>
          进入 <RightOutlined />
        </Button>
      ),
    },
  ];

  const openClassRoster = (row: ExamScoreImportClassOption) => {
    setRosterKeyword('');
    setRosterPage(1);
    setRosterResultFilter(null);
    setRosterClass(row);
  };

  const classColumns: ColumnsType<ExamScoreImportClassOption> = [
    {
      title: '班级',
      width: 140,
      dataIndex: 'title',
      ellipsis: true,
      render: (v, row) => (
        <Typography.Link>{v || `班级#${row.id}`}</Typography.Link>
      ),
    },
    {
      title: '工种',
      width: 140,
      render: (_, r) =>
        [r.workTypeTitle, r.workTypeLevel != null ? `Lv${r.workTypeLevel}` : null]
          .filter(Boolean)
          .join(' ') || '—',
    },
    {
      title: '人数',
      dataIndex: 'studentCount',
      width: 64,
      align: 'right',
      render: (v) => (v == null ? '—' : v),
    },
    {
      title: '成绩导入',
      width: 110,
      render: (_, r) => {
        const total = r.studentCount ?? 0;
        const scored = r.scoredCount ?? 0;
        const status = r.scoreImportStatus ?? 'NONE';
        if (status === 'DONE') {
          return <Tag color="success">已导入 {scored}/{total}</Tag>;
        }
        if (status === 'PARTIAL') {
          return <Tag color="warning">部分 {scored}/{total}</Tag>;
        }
        return <Tag>未导入</Tag>;
      },
    },
    {
      title: '通过',
      dataIndex: 'passedCount',
      width: 64,
      align: 'right',
      render: (v) =>
        v == null ? '—' : (
          <Typography.Text type="success">{v}</Typography.Text>
        ),
    },
    {
      title: '通过率',
      width: 80,
      align: 'right',
      render: (_, r) => {
        const total = r.studentCount ?? 0;
        const passed = r.passedCount ?? 0;
        if (total <= 0) return '—';
        return (
          <Typography.Text type="success" strong>
            {((passed / total) * 100).toFixed(1)}%
          </Typography.Text>
        );
      },
    },
    {
      title: '缺考',
      dataIndex: 'absentCount',
      width: 64,
      align: 'right',
      render: (v) => (v == null ? '—' : v),
    },
    {
      title: '不合格',
      dataIndex: 'failedCount',
      width: 72,
      align: 'right',
      render: (v) =>
        v == null ? '—' : (
          <Typography.Text type="danger">{v}</Typography.Text>
        ),
    },
    {
      title: '培训项目',
      width: 140,
      dataIndex: 'trainTitle',
      ellipsis: true,
      filters: trainTitleFilterOptions,
      filteredValue: trainTitleFilters.length > 0 ? trainTitleFilters : null,
      filterSearch: true,
      onFilter: () => true,
      render: (v) => v || '—',
    },
  ];

  const peopleColumns: ColumnsType<ExamScoreImportClassPeople> = [
    {
      title: '准考证号',
      dataIndex: 'ticketNo',
      width: 158,
      ellipsis: true,
      render: (v) => v || '—',
    },
    { title: '姓名', dataIndex: 'realname', width: 60, render: (v) => v || '—' },
    { title: '证件号码', dataIndex: 'idCard', width: 140, render: (v) => v || '—' },
   
    { title: '手机', dataIndex: 'phone', width: 120, render: (v) => v || '—' },
    {
      title: '理论',
      dataIndex: 'theoryScore',
      width: 80,
      render: (v) => renderExamScoreCell(v),
    },
    {
      title: '技能',
      dataIndex: 'skillScore',
      width: 80,
      render: (v) => renderExamScoreCell(v),
    },
    {
      title: '综合',
      dataIndex: 'examScore',
      width: 80,
      render: (v) => (v == null || v === '' ? '—' : v),
    },
    {
      title: '工作业绩',
      dataIndex: 'performanceScore',
      width: 90,
      render: (v) => (v == null || v === '' ? '—' : v),
    },
    {
      title: '结果',
      dataIndex: 'examResultStatus',
      width: 100,
      fixed: 'right',
      filters: [
        { text: '合格', value: 'PASS' },
        { text: '不合格', value: 'FAIL' },
        { text: '缺考', value: 'ABSENT' },
      ],
      filteredValue: rosterResultFilter ? [rosterResultFilter] : null,
      filterMultiple: false,
      onFilter: () => true,
      render: (v) => examResultTag(v),
    },
  ];

  const writableColumns: ColumnsType<WritableRow> = [
    { title: '行号', dataIndex: 'rowNo', width: 70 },
    { title: '证件号码', dataIndex: 'idCard', width: 170 },
    { title: '成绩单姓名', dataIndex: 'name', width: 100 },
    { title: '花名册姓名', dataIndex: 'rosterName', width: 100 },
    {
      title: '准考证号',
      dataIndex: 'ticketNo',
      width: 140,
      ellipsis: true,
      render: (v) => v || '—',
    },
    { title: '班级', dataIndex: 'classTitle', width: 120, ellipsis: true },
    {
      title: '理论',
      dataIndex: 'theoryScore',
      width: 80,
      render: (v) => v ?? '—',
    },
    {
      title: '技能',
      dataIndex: 'skillScore',
      width: 80,
      render: (v) => v ?? '—',
    },
    {
      title: '综合',
      dataIndex: 'examScore',
      width: 100,
      render: (v) => (v == null || v === '' ? '—' : v),
    },
    {
      title: '工作业绩',
      dataIndex: 'performanceScore',
      width: 100,
      render: (v) => v ?? '—',
    },
    {
      title: '合并',
      dataIndex: 'mergeNote',
      width: 140,
      ellipsis: true,
      render: (v, r) =>
        v ? (
          <span>
            {v}
            {r.examTimeMissing ? <Tag style={{ marginLeft: 4 }}>时间缺失</Tag> : null}
          </span>
        ) : (
          '—'
        ),
    },
    { title: '状态', dataIndex: 'status', width: 130, render: (v) => statusTag(v) },
  ];

  const sheetColumns: ColumnsType<ExamScoreImportItem> = [
    { title: '行号', dataIndex: 'rowNo', width: 70 },
    { title: '姓名', dataIndex: 'name', width: 100 },
    { title: '证件号码', dataIndex: 'idCard', width: 170 },
    {
      title: '准考证号',
      dataIndex: 'ticketNo',
      width: 140,
      ellipsis: true,
      render: (v) => v || '—',
    },
    { title: '理论', dataIndex: 'theoryScore', width: 80, render: (v) => v ?? '—' },
    { title: '技能', dataIndex: 'skillScore', width: 80, render: (v) => v ?? '—' },
    { title: '综合', dataIndex: 'examScore', width: 80, render: (v) => v ?? '—' },
    { title: '工作业绩', dataIndex: 'performanceScore', width: 100, render: (v) => v ?? '—' },
    { title: '状态', dataIndex: 'status', width: 130, render: (v) => statusTag(v) },
  ];

  const gapColumns: ColumnsType<ExamScoreImportRosterGap> = [
    { title: '班级', dataIndex: 'classTitle' },
    { title: '花名册姓名', dataIndex: 'rosterName', width: 120 },
    { title: '证件号码', dataIndex: 'idCard', width: 180 },
  ];

  return (
    <div className="training-page training-follow-exam-page">
      <TrainingProcessSteps current={6} />
      <section className="page-heading">
        <Typography.Title level={4} style={{ marginBottom: 4 }}>
          考试成绩导入
        </Typography.Title>
        <Typography.Text type="secondary">
          先选企业与班级，上传成绩表后核对匹配明细，再确认写入花名册供结业使用。
        </Typography.Text>
      </section>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
          message="写入开班花名册成绩，供结业使用；跟班模拟考分数不变。上传仅做匹配预览，需点「确认写入」才会落库。未勾选班级时不可导入/确认。若班级在「未归属企业」，请先到开班准备为培训需求选择企业。"
        />

      {activeCompanyId == null ? (
        <Card
          size="small"
          title="全部企业"
          extra={
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索企业"
              value={companyKeyword}
              onChange={(e) => {
                setCompanyKeyword(e.target.value);
                setCompanyPage(1);
              }}
              style={{ width: 220 }}
            />
          }
        >
          <Table<CompanyRow>
            size="small"
            rowKey={(r) => String(r.companyId)}
            loading={publicQuery.isLoading || allClassesQuery.isLoading}
            columns={companyColumns}
            dataSource={filteredCompanies}
            pagination={{
              current: companyPage,
              pageSize: companyPageSize,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50', '100'],
              showTotal: (n) => `共 ${n} 家企业`,
              onChange: (page, size) => {
                setCompanyPage(page);
                if (typeof size === 'number' && size > 0) {
                  setCompanyPageSize(size);
                }
              },
            }}
            onRow={(row) => ({
              onClick: () => enterCompany(row.companyId),
              style: { cursor: 'pointer' },
            })}
          />
        </Card>
      ) : (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Breadcrumb
            items={[
              {
                title: (
                  <a onClick={backToCompanies}>
                    <ArrowLeftOutlined /> 全部企业
                  </a>
                ),
              },
              { title: activeCompany?.companyName || `企业#${activeCompanyId}` },
            ]}
          />

          <Card
            size="small"
            title={`班级列表（${companyClasses.length}）`}
            loading={companyClassesQuery.isLoading}
            extra={
              <Space wrap size={8}>
                <Input
                  allowClear
                  prefix={<SearchOutlined />}
                  placeholder="搜索班级/项目"
                  value={classKeyword}
                  onChange={(e) => {
                    setClassKeyword(e.target.value);
                    setClassPage(1);
                  }}
                  style={{ width: 180 }}
                />
                <Button
                  size="small"
                  onClick={() => setSelectedClassIds(companyClasses.map((c) => String(c.id)))}
                >
                  全选班级
                </Button>
                <Button size="small" icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
                  标准模板
                </Button>
                <Upload
                  accept=".xlsx,.xls"
                  multiple
                  maxCount={20}
                  showUploadList={false}
                  disabled={targetClassIds.length === 0 || importing || committing}
                  beforeUpload={(file, fileList) => {
                    pendingUploadFilesRef.current.push(file);
                    if (pendingUploadFilesRef.current.length >= fileList.length) {
                      const batch = [...pendingUploadFilesRef.current];
                      pendingUploadFilesRef.current = [];
                      void runPreviewImport(batch);
                    }
                    return false;
                  }}
                >
                  <Button
                    size="small"
                    type="primary"
                    icon={<UploadOutlined />}
                    loading={importing}
                    disabled={targetClassIds.length === 0 || committing}
                  >
                    导入成绩
                  </Button>
                </Upload>
                <Button
                  size="small"
                  type="primary"
                  ghost
                  loading={committing}
                  disabled={!canConfirmWrite}
                  onClick={() => void runConfirmWrite()}
                >
                  确认写入
                </Button>
              </Space>
            }
          >
            <Table<ExamScoreImportClassOption>
              size="small"
              rowKey={(r) => String(r.id)}
              columns={classColumns}
              dataSource={filteredClasses}
              scroll={{ x: 900 }}
              pagination={{
                current: classPage,
                pageSize: classPageSize,
                showSizeChanger: true,
                pageSizeOptions: ['10', '20', '50', '100'],
                showTotal: (n) => `共 ${n} 个班`,
              }}
              onChange={(pagination, filters) => {
                const raw = filters.trainTitle;
                const next = Array.isArray(raw)
                  ? raw.map(String).filter(Boolean)
                  : [];
                const filtersChanged =
                  next.length !== trainTitleFilters.length
                  || next.some((v, i) => v !== trainTitleFilters[i]);
                if (filtersChanged) {
                  setTrainTitleFilters(next);
                  setClassPage(1);
                } else if (pagination.current != null) {
                  setClassPage(pagination.current);
                }
                if (
                  typeof pagination.pageSize === 'number'
                  && pagination.pageSize > 0
                  && pagination.pageSize !== classPageSize
                ) {
                  setClassPageSize(pagination.pageSize);
                  setClassPage(1);
                }
              }}
              onRow={(row) => ({
                style: { cursor: 'pointer' },
                onClick: (e) => {
                  const el = e.target as HTMLElement | null;
                  if (
                    el?.closest(
                      '.ant-checkbox-wrapper, .ant-checkbox, input, button, .ant-table-filter-trigger, .ant-dropdown',
                    )
                  ) {
                    return;
                  }
                  openClassRoster(row);
                },
              })}
              rowSelection={{
                selectedRowKeys: selectedClassIds,
                onChange: (keys) => {
                  setSelectedClassIds(keys.map(String));
                  setPreview(null);
                  setLastCommit(null);
                  setUploadFileName(null);
                },
              }}
            />
            <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
              已选 {targetClassIds.length} 个班参与匹配
              {uploadFileName ? `；当前成绩表：${uploadFileName}` : '；可多选成绩表上传，再点确认写入'}
              {targetClassIds.length === 0 ? '（未选班级时不可导入/确认）' : ''}
            </Typography.Paragraph>
            {lastCommit ? (
              <Alert
                style={{ marginTop: 12 }}
                type="success"
                showIcon
                message={`本次写入：成功 ${lastCommit.written}，覆盖 ${lastCommit.overwritten}，跳过 ${lastCommit.skipped}`}
              />
            ) : null}
          </Card>

          {preview ? (
            <Card
              size="small"
              title="匹配明细（确认后写入）"
              extra={
                <Button
                  type="primary"
                  loading={committing}
                  disabled={!canConfirmWrite}
                  onClick={() => void runConfirmWrite()}
                >
                  确认写入
                </Button>
              }
            >
              <Space wrap style={{ marginBottom: 12 }}>
                {preview.layout === 'RENSHE_ROSTER' && preview.planName ? (
                  <Tag color="blue">计划：{preview.planName}</Tag>
                ) : null}
                {preview.planCode ? <Tag>编码：{preview.planCode}</Tag> : null}
                {preview.examTimeText ? <Tag>{preview.examTimeText}</Tag> : null}
                <span>成绩单 {preview.totalRows} 人</span>
                <span>可写 {preview.matched}</span>
                <span>姓名黄标 {preview.nameWarn}</span>
                <span>未在本班 {preview.unmatched}</span>
                <span>本班未出现 {preview.classMissingOnSheet}</span>
                <span>缺证 {preview.classMissingIdCard}</span>
                <span>同场冲突 {preview.sheetDup}</span>
                <span>空分 {preview.emptyScore}</span>
              </Space>
              <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
                多场成绩按考试时间合并（空不覆盖）：补考无实操时继承初考技能；仅同一考试时间内分值不一致才进「同场冲突」。
              </Typography.Paragraph>
              <Tabs
                items={[
                  {
                    key: 'writable',
                    label: `待写入 (${writableRows.length})`,
                    children: (
                      <Table<WritableRow>
                        size="small"
                        rowKey="key"
                        columns={writableColumns}
                        dataSource={writableRows}
                        pagination={false}
                        scroll={{ x: 1280 }}
                        rowClassName={(r) =>
                          r.nameMismatch ||
                          r.jobMismatch ||
                          r.status === 'MATCHED_NAME_WARN' ||
                          r.status === 'MATCHED_JOB_WARN'
                            ? 'exam-score-import-warn-row'
                            : ''
                        }
                      />
                    ),
                  },
                  {
                    key: 'unmatched',
                    label: `未在本班 (${unmatchedItems.length})`,
                    children: (
                      <Table<ExamScoreImportItem>
                        size="small"
                        rowKey={(r) => `${r.rowNo}-${r.idCard ?? r.name}`}
                        columns={sheetColumns}
                        dataSource={unmatchedItems}
                        pagination={false}
                      />
                    ),
                  },
                  {
                    key: 'missing',
                    label: `本班未出现 (${preview.classMissingPeople.length})`,
                    children: (
                      <Table<ExamScoreImportRosterGap>
                        size="small"
                        rowKey={(r) => String(r.peopleId)}
                        columns={gapColumns}
                        dataSource={preview.classMissingPeople}
                        pagination={false}
                      />
                    ),
                  },
                  {
                    key: 'conflict',
                    label: `同场冲突 (${conflictItems.length})`,
                    children: (
                      <Table<ExamScoreImportItem>
                        size="small"
                        rowKey={(r) => `${r.rowNo}-${r.idCard ?? r.name}-c`}
                        columns={sheetColumns}
                        dataSource={conflictItems}
                        pagination={false}
                      />
                    ),
                  },
                  {
                    key: 'empty',
                    label: `空分 (${emptyScoreItems.length})`,
                    children: (
                      <Table<ExamScoreImportItem>
                        size="small"
                        rowKey={(r) => `${r.rowNo}-${r.idCard ?? r.name}-e`}
                        columns={sheetColumns}
                        dataSource={emptyScoreItems}
                        pagination={false}
                      />
                    ),
                  },
                  {
                    key: 'no-id',
                    label: `班内缺证 (${preview.missingIdCardPeople.length})`,
                    children: (
                      <Table<ExamScoreImportRosterGap>
                        size="small"
                        rowKey={(r) => String(r.peopleId)}
                        columns={gapColumns}
                        dataSource={preview.missingIdCardPeople}
                        pagination={false}
                      />
                    ),
                  },
                ]}
              />
            </Card>
          ) : null}
        </Space>
      )}

      <Drawer
        title={
          rosterClass
            ? `${rosterClass.title || `班级#${rosterClass.id}`} · 人员考试信息`
            : '人员考试信息'
        }
        width={860}
        open={rosterClass != null}
        onClose={() => {
          setRosterClass(null);
          setRosterResultFilter(null);
          setRosterKeyword('');
          setRosterPage(1);
        }}
        destroyOnClose
        rootClassName="training-follow-exam-roster-drawer"
      >
        {rosterClass ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              {[rosterClass.trainTitle, rosterClass.workTypeTitle, rosterClass.workTypeLevel != null ? `Lv${rosterClass.workTypeLevel}` : null]
                .filter(Boolean)
                .join(' · ') || '—'}
            </Typography.Text>
            {classPeopleQuery.data?.stats ? (
              (() => {
                const stats = classPeopleQuery.data.stats;
                const rates = buildExamRosterRateSummary(stats);
                return (
                  <div className="exam-roster-stats">
                    <span className="stat-item">
                      合计 <Typography.Text strong>{stats.total}</Typography.Text>
                    </span>
                    <span className="stat-item">
                      合格{' '}
                      <Typography.Text type="success" strong>
                        {stats.passed}
                      </Typography.Text>
                    </span>
                    <span className="stat-item">
                      不合格{' '}
                      <Typography.Text type="danger" strong>
                        {stats.failed}
                      </Typography.Text>
                    </span>
                    <span className="stat-item">
                      缺考 <Typography.Text strong>{stats.absent}</Typography.Text>
                    </span>
                    <Typography.Text type="secondary">|</Typography.Text>
                    <span className="stat-item">
                      通过率{' '}
                      <Typography.Text type="success" strong>
                        {rates.passRate}
                      </Typography.Text>
                    </span>
                    <span className="stat-item">
                      参考通过率 <Typography.Text strong>{rates.passAmongAttended}</Typography.Text>
                    </span>
                    <span className="stat-item">
                      参考率 <Typography.Text strong>{rates.attendRate}</Typography.Text>
                    </span>
                    <span className="stat-item">
                      缺考率 <Typography.Text strong>{rates.absentRate}</Typography.Text>
                    </span>
                    <Typography.Text type="secondary">|</Typography.Text>
                    <span className="stat-item">
                      理论 过{stats.theoryPass}/不{stats.theoryFail}/缺{stats.theoryAbsent}
                      （及格率 {rates.theoryPassRate}）
                    </span>
                    <span className="stat-item">
                      技能 过{stats.skillPass}/不{stats.skillFail}/缺{stats.skillAbsent}
                      （及格率 {rates.skillPassRate}）
                    </span>
                  </div>
                );
              })()
            ) : null}
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
              合格标准：理论≥60 且 技能≥60；理论与技能均无成绩记为缺考。通过率=合格/合计；参考通过率=合格/(合格+不合格)；参考率=(合格+不合格)/合计。行底色：绿=合格、红=不合格、灰=缺考。
            </Typography.Paragraph>
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索姓名 / 证件号 / 手机"
              value={rosterKeyword}
              onChange={(e) => {
                setRosterKeyword(e.target.value);
                setRosterPage(1);
              }}
              style={{ maxWidth: 320 }}
            />
            <Table<ExamScoreImportClassPeople>
              size="small"
              rowKey={(r) => String(r.id)}
              loading={classPeopleQuery.isLoading}
              columns={peopleColumns}
              dataSource={classPeopleQuery.data?.items ?? []}
              rowClassName={(r) => examRosterRowClass(r.examResultStatus)}
              onChange={(pagination, filters) => {
                const raw = filters.examResultStatus;
                const next =
                  Array.isArray(raw) && raw.length > 0 ? String(raw[0]) : null;
                if (next !== rosterResultFilter) {
                  setRosterResultFilter(next);
                  setRosterPage(1);
                } else if (pagination.current != null) {
                  setRosterPage(pagination.current);
                }
                if (pagination.pageSize != null && pagination.pageSize !== rosterPageSize) {
                  setRosterPageSize(pagination.pageSize);
                  setRosterPage(1);
                }
              }}
              pagination={{
                current: rosterPage,
                pageSize: rosterPageSize,
                total: classPeopleQuery.data?.total ?? 0,
                showSizeChanger: true,
                showTotal: (n) => `共 ${n} 人`,
              }}
              scroll={{ x: 1000 }}
            />
          </Space>
        ) : null}
      </Drawer>
    </div>
  );
}
