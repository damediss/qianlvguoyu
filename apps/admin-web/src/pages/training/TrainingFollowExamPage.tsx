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
import { useQuery } from '@tanstack/react-query';
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

export function TrainingFollowExamPage() {
  const [companyKeyword, setCompanyKeyword] = useState('');
  const [activeCompanyId, setActiveCompanyId] = useState<string | null>(null);
  const [classKeyword, setClassKeyword] = useState('');
  const [selectedClassIds, setSelectedClassIds] = useState<string[]>([]);
  const [rosterClass, setRosterClass] = useState<ExamScoreImportClassOption | null>(null);
  const [rosterKeyword, setRosterKeyword] = useState('');
  const [rosterPage, setRosterPage] = useState(1);
  const [rosterPageSize, setRosterPageSize] = useState(50);
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
    ],
    queryFn: () =>
      trainApi.pageExamScoreImportClassPeople(rosterClass!.id, {
        page: rosterPage,
        pageSize: rosterPageSize,
        keyword: rosterKeyword.trim() || undefined,
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

  const filteredClasses = useMemo(() => {
    const kw = classKeyword.trim().toLowerCase();
    if (!kw) return companyClasses;
    return companyClasses.filter((c) => {
      const blob = [c.title, c.trainTitle, c.workTypeTitle].filter(Boolean).join(' ').toLowerCase();
      return blob.includes(kw);
    });
  }, [companyClasses, classKeyword]);

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
    setRosterClass(row);
  };

  const classColumns: ColumnsType<ExamScoreImportClassOption> = [
    {
      title: '班级',
      dataIndex: 'title',
      ellipsis: true,
      render: (v, row) => (
        <Typography.Link>{v || `班级#${row.id}`}</Typography.Link>
      ),
    },
    { title: '培训项目', dataIndex: 'trainTitle', ellipsis: true, render: (v) => v || '—' },
    {
      title: '工种',
      width: 160,
      render: (_, r) =>
        [r.workTypeTitle, r.workTypeLevel != null ? `Lv${r.workTypeLevel}` : null]
          .filter(Boolean)
          .join(' ') || '—',
    },
  ];

  const peopleColumns: ColumnsType<ExamScoreImportClassPeople> = [
    { title: '姓名', dataIndex: 'realname', width: 110, render: (v) => v || '—' },
    { title: '证件号码', dataIndex: 'idCard', width: 180, render: (v) => v || '—' },
    { title: '手机', dataIndex: 'phone', width: 120, render: (v) => v || '—' },
    {
      title: '理论',
      dataIndex: 'theoryScore',
      width: 90,
      render: (v) => (v == null || v === '' ? '—' : v),
    },
    {
      title: '技能',
      dataIndex: 'skillScore',
      width: 90,
      render: (v) => (v == null || v === '' ? '—' : v),
    },
    {
      title: '综合',
      dataIndex: 'examScore',
      width: 90,
      render: (v) => (v == null || v === '' ? '—' : v),
    },
    {
      title: '工作业绩',
      dataIndex: 'performanceScore',
      width: 100,
      render: (v) => (v == null || v === '' ? '—' : v),
    },
  ];

  const writableColumns: ColumnsType<WritableRow> = [
    { title: '行号', dataIndex: 'rowNo', width: 70 },
    { title: '成绩单姓名', dataIndex: 'name', width: 110 },
    { title: '花名册姓名', dataIndex: 'rosterName', width: 110 },
    { title: '证件号码', dataIndex: 'idCard', width: 180 },
    { title: '班级', dataIndex: 'classTitle', ellipsis: true },
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
    { title: '姓名', dataIndex: 'name', width: 110 },
    { title: '证件号码', dataIndex: 'idCard', width: 180 },
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
              onChange={(e) => setCompanyKeyword(e.target.value)}
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
            pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (n) => `共 ${n} 家企业` }}
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
                  onChange={(e) => setClassKeyword(e.target.value)}
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
              pagination={{ pageSize: 10, showTotal: (n) => `共 ${n} 个班` }}
              onRow={(row) => ({
                style: { cursor: 'pointer' },
                onClick: (e) => {
                  const el = e.target as HTMLElement | null;
                  if (el?.closest('.ant-checkbox-wrapper, .ant-checkbox, input, button')) {
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
                        scroll={{ x: 1100 }}
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
        width={820}
        open={rosterClass != null}
        onClose={() => setRosterClass(null)}
        destroyOnClose
      >
        {rosterClass ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Typography.Text type="secondary">
              {[rosterClass.trainTitle, rosterClass.workTypeTitle, rosterClass.workTypeLevel != null ? `Lv${rosterClass.workTypeLevel}` : null]
                .filter(Boolean)
                .join(' · ') || '—'}
            </Typography.Text>
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
              pagination={{
                current: rosterPage,
                pageSize: rosterPageSize,
                total: classPeopleQuery.data?.total ?? 0,
                showSizeChanger: true,
                showTotal: (n) => `共 ${n} 人`,
                onChange: (page, size) => {
                  setRosterPage(page);
                  setRosterPageSize(size);
                },
              }}
              scroll={{ x: 780 }}
            />
          </Space>
        ) : null}
      </Drawer>
    </div>
  );
}
