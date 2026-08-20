import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import { DownloadOutlined, InboxOutlined, UploadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type {
  DemandPublicData,
  ExamScoreImportBatch,
  ExamScoreImportClassOption,
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

type WritableRow = {
  key: string;
  peopleId: string;
  rowNo: number;
  name?: string;
  idCard?: string;
  examScore?: number | null;
  skillScore?: number | null;
  theoryFallback?: boolean;
  status: string;
  classTitle?: string;
  rosterName?: string;
  oldExamScore?: number | null;
  oldRealScore?: number | null;
  nameMismatch?: boolean;
  jobMismatch?: boolean;
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
        examScore: item.examScore,
        skillScore: item.skillScore,
        theoryFallback: item.theoryFallback,
        status: item.status,
        classTitle: t.classTitle,
        rosterName: t.rosterName,
        oldExamScore: t.oldExamScore,
        oldRealScore: t.oldRealScore,
        nameMismatch: t.nameMismatch,
        jobMismatch: t.jobMismatch,
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
  const queryClient = useQueryClient();
  const [companyId, setCompanyId] = useState<number | undefined>();
  const [trainInfoId, setTrainInfoId] = useState<number | undefined>();
  const [classIds, setClassIds] = useState<string[]>([]);
  const [preview, setPreview] = useState<ExamScoreImportPreview | null>(null);
  const [selectedPeopleIds, setSelectedPeopleIds] = useState<string[]>([]);
  const [previewing, setPreviewing] = useState(false);

  const publicQuery = useQuery({
    queryKey: ['exam-score-import-public'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'traininfo']),
  });
  const publicData = publicQuery.data as DemandPublicData | undefined;

  const classQuery = useQuery({
    queryKey: ['exam-score-import-classes', companyId, trainInfoId],
    queryFn: () =>
      trainApi.listExamScoreImportClasses({
        companyId,
        trainInfoId,
      }),
  });

  const batchQuery = useQuery({
    queryKey: ['exam-score-import-batches'],
    queryFn: () => trainApi.pageExamScoreImportBatches({ page: 1, pageSize: 20 }),
  });

  const companyOptions = useMemo(
    () => (publicData?.company_list ?? []).map((c) => ({ value: c.id, label: c.title })),
    [publicData],
  );
  const trainInfoOptions = useMemo(() => {
    const list = publicData?.traininfo_list ?? [];
    return list
      .filter((t) => !companyId || t.company_id == null || Number(t.company_id) === companyId)
      .map((t) => ({ value: t.id, label: t.title }));
  }, [publicData, companyId]);
  const classOptions = useMemo(
    () =>
      (classQuery.data ?? []).map((c: ExamScoreImportClassOption) => ({
        value: String(c.id),
        label: [c.title, c.companyName, c.trainTitle].filter(Boolean).join(' · '),
      })),
    [classQuery.data],
  );

  const writableRows = useMemo(() => flattenWritable(preview?.items ?? []), [preview]);
  const unmatchedItems = useMemo(
    () => (preview?.items ?? []).filter((i) => i.status === 'UNMATCHED_NOT_IN_CLASS' || i.status === 'NO_ID_CARD'),
    [preview],
  );
  const conflictItems = useMemo(
    () => (preview?.items ?? []).filter((i) => i.status === 'SHEET_DUP_CONFLICT' || i.status === 'EMPTY_SCORE'),
    [preview],
  );

  const commitMu = useMutation({
    mutationFn: () =>
      trainApi.commitExamScoreImport(
        preview!.previewId,
        selectedPeopleIds,
      ),
    onSuccess: (res) => {
      message.success(`导入成功: 写入 ${res.written} 人，覆盖 ${res.overwritten}，跳过 ${res.skipped}`);
      setPreview(null);
      setSelectedPeopleIds([]);
      queryClient.invalidateQueries({ queryKey: ['exam-score-import-batches'] });
    },
  });

  const handlePreview = async (file: File) => {
    if (classIds.length === 0) {
      message.warning('请先选择班级');
      return Upload.LIST_IGNORE;
    }
    setPreviewing(true);
    try {
      const data = await trainApi.previewExamScoreImport(classIds, file);
      setPreview(data);
      setSelectedPeopleIds(flattenWritable(data.items).map((r) => r.peopleId));
    } catch (e) {
      message.error((e as Error).message || '解析失败');
    } finally {
      setPreviewing(false);
    }
    return Upload.LIST_IGNORE;
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

  const handleExportFailures = async (batchId: number | string) => {
    try {
      const blob = await trainApi.downloadExamScoreImportFailures(batchId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = '成绩导入失败清单.xlsx';
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      message.error('导出失败');
    }
  };

  const writableColumns: ColumnsType<WritableRow> = [
    { title: '行号', dataIndex: 'rowNo', width: 70 },
    { title: '成绩单姓名', dataIndex: 'name', width: 110 },
    { title: '花名册姓名', dataIndex: 'rosterName', width: 110 },
    { title: '证件号码', dataIndex: 'idCard', width: 180 },
    { title: '班级', dataIndex: 'classTitle', ellipsis: true },
    {
      title: '综合→结业',
      dataIndex: 'examScore',
      width: 110,
      render: (v, r) => (
        <span>
          {v ?? '-'}
          {r.theoryFallback ? <Tag style={{ marginLeft: 4 }}>以理论代综合</Tag> : null}
        </span>
      ),
    },
    { title: '技能', dataIndex: 'skillScore', width: 80, render: (v) => v ?? '-' },
    { title: '原综合', dataIndex: 'oldExamScore', width: 80, render: (v) => v ?? '-' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 130,
      render: (v) => statusTag(v),
    },
  ];

  const sheetColumns: ColumnsType<ExamScoreImportItem> = [
    { title: '行号', dataIndex: 'rowNo', width: 70 },
    { title: '姓名', dataIndex: 'name', width: 110 },
    { title: '证件号码', dataIndex: 'idCard', width: 180 },
    { title: '综合', dataIndex: 'examScore', width: 80, render: (v) => v ?? '-' },
    { title: '理论', dataIndex: 'theoryScore', width: 80, render: (v) => v ?? '-' },
    { title: '技能', dataIndex: 'skillScore', width: 80, render: (v) => v ?? '-' },
    { title: '状态', dataIndex: 'status', width: 130, render: (v) => statusTag(v) },
  ];

  const gapColumns: ColumnsType<ExamScoreImportRosterGap> = [
    { title: '班级', dataIndex: 'classTitle' },
    { title: '花名册姓名', dataIndex: 'rosterName', width: 120 },
    { title: '证件号码', dataIndex: 'idCard', width: 180 },
  ];

  const batchColumns: ColumnsType<ExamScoreImportBatch> = [
    { title: '计划名称', dataIndex: 'planName', ellipsis: true },
    { title: '计划编码', dataIndex: 'planCode', width: 200 },
    { title: '文件', dataIndex: 'fileName', width: 180, ellipsis: true },
    { title: '写入/覆盖', width: 110, render: (_, r) => `${r.writtenCount}/${r.overwrittenCount}` },
    { title: '未匹配', dataIndex: 'unmatchedCount', width: 80 },
    { title: '操作人', dataIndex: 'operatorUsername', width: 100 },
    { title: '时间', dataIndex: 'createdAt', width: 170 },
    {
      title: '操作',
      key: 'act',
      width: 120,
      render: (_, r) => (
        <Button type="link" size="small" onClick={() => handleExportFailures(r.id)}>
          失败清单
        </Button>
      ),
    },
  ];

  return (
    <div className="training-page training-follow-exam-page">
      <TrainingProcessSteps current={6} />
      <section className="page-heading">
        <Typography.Title level={4} style={{ marginBottom: 4 }}>
          考试成绩导入
        </Typography.Title>
        <Typography.Text type="secondary">
          上传人社成绩花名册，按身份证写入已选班级花名册，供结业使用。
        </Typography.Text>
      </section>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="写入开班花名册成绩，供结业使用；跟班模拟考分数不变。未选班级不会匹配或改分。"
      />

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder="企业"
            style={{ width: 220 }}
            options={companyOptions}
            value={companyId}
            onChange={(v) => {
              setCompanyId(v);
              setTrainInfoId(undefined);
              setClassIds([]);
              setPreview(null);
            }}
          />
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder="培训项目"
            style={{ width: 260 }}
            options={trainInfoOptions}
            value={trainInfoId}
            onChange={(v) => {
              setTrainInfoId(v);
              setClassIds([]);
              setPreview(null);
            }}
          />
          <Select
            mode="multiple"
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder="选择班级（必填，可多选）"
            style={{ minWidth: 320 }}
            options={classOptions}
            value={classIds}
            loading={classQuery.isLoading}
            onChange={(v) => {
              setClassIds(v);
              setPreview(null);
            }}
          />
          <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
            标准模板
          </Button>
        </Space>
      </Card>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Upload.Dragger
          accept=".xlsx,.xls"
          maxCount={1}
          showUploadList={false}
          disabled={classIds.length === 0 || previewing}
          beforeUpload={(file) => {
            void handlePreview(file);
            return false;
          }}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">
            {classIds.length === 0 ? '请先选择班级，再上传成绩单' : '点击或拖拽上传人社成绩花名册'}
          </p>
          <p className="ant-upload-hint">支持 xlsx/xls；自动识别人社花名册或扁平模板</p>
        </Upload.Dragger>
      </Card>

      {preview && (
        <Card
          size="small"
          style={{ marginBottom: 16 }}
          title="预览确认"
          extra={
            <Space>
              <Button onClick={() => { setPreview(null); setSelectedPeopleIds([]); }}>取消</Button>
              <Button
                type="primary"
                icon={<UploadOutlined />}
                loading={commitMu.isPending}
                disabled={selectedPeopleIds.length === 0}
                onClick={() => commitMu.mutate()}
              >
                确认导入（{selectedPeopleIds.length}）
              </Button>
            </Space>
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
          </Space>
          <Tabs
            items={[
              {
                key: 'writable',
                label: `可写入 (${writableRows.length})`,
                children: (
                  <Table<WritableRow>
                    size="small"
                    rowKey="key"
                    columns={writableColumns}
                    dataSource={writableRows}
                    pagination={false}
                    scroll={{ x: 1100 }}
                    rowClassName={(r) =>
                      r.nameMismatch || r.jobMismatch || r.status === 'MATCHED_NAME_WARN' || r.status === 'MATCHED_JOB_WARN'
                        ? 'exam-score-import-warn-row'
                        : ''
                    }
                    rowSelection={{
                      selectedRowKeys: selectedPeopleIds,
                      onChange: (keys) => setSelectedPeopleIds(keys.map(String)),
                    }}
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
                label: `冲突/空分 (${conflictItems.length})`,
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
      )}

      <Card size="small" title="导入历史">
        <Table<ExamScoreImportBatch>
          size="small"
          rowKey={(r) => String(r.id)}
          loading={batchQuery.isLoading}
          columns={batchColumns}
          dataSource={batchQuery.data?.items ?? []}
          pagination={false}
        />
      </Card>
    </div>
  );
}
