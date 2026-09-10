import { useMemo, useRef, useState, type Key } from 'react';
import {
  Alert,
  Button,
  Card,
  Dropdown,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { MenuProps } from 'antd';
import {
  DownOutlined,
  DownloadOutlined,
  FolderOpenOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { CertifiedRosterScope, GraduationCertImportPreview } from '../../api/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';

type Props = {
  processCurrent?: number;
  pageTitle?: string;
  pageSubtitle?: string;
};

const ROSTER_SCOPE_MENU_ITEMS: MenuProps['items'] = [
  { key: 'certified', label: '已获证人员名册' },
  { key: 'all', label: '班级全部人员名册' },
];

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

function classStatusTag(status?: string) {
  if (status === 'done') return <Tag color="success">已结业</Tag>;
  if (status === 'partial') return <Tag color="warning">部分结业</Tag>;
  return <Tag>未结业</Tag>;
}

async function downloadCertifiedRoster(opts: {
  classId?: number | string;
  classIds?: Array<number | string>;
  classTitle?: string;
  scope: CertifiedRosterScope;
}) {
  try {
    if (opts.classIds != null && opts.classIds.length > 0) {
      await trainApi.exportCertifiedRosters(opts.classIds, opts.scope);
      return;
    }
    if (opts.classId != null) {
      await trainApi.exportCertifiedRoster(opts.classId, opts.classTitle, opts.scope);
      return;
    }
    message.warning('请选择班级');
  } catch (e) {
    message.error((e as Error).message || '导出失败');
  }
}

/** 隐藏路由跳转（与 main.tsx 约定：pushState + popstate） */
function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export function TrainingGraduationPage({
  processCurrent = 7,
  pageTitle = '毕业典礼',
  pageSubtitle = '获证人员名册导出、证书文件夹回填；点击班级进入详情',
}: Props) {
  const queryClient = useQueryClient();
  const [searchForm] = Form.useForm();
  const [query, setQuery] = useState<{
    page: number;
    pageSize: number;
    keyword?: string;
    graduateStatus?: string;
    trainTitle?: string[];
    companyName?: string[];
    workTypeName?: string[];
  }>({ page: 1, pageSize: 20 });
  const [selectedClassKeys, setSelectedClassKeys] = useState<Key[]>([]);
  const [certImportOpen, setCertImportOpen] = useState(false);
  const [certFiles, setCertFiles] = useState<File[]>([]);
  const [certPreview, setCertPreview] = useState<GraduationCertImportPreview | null>(null);
  const [certPreviewing, setCertPreviewing] = useState(false);
  const [certCommitting, setCertCommitting] = useState(false);
  const [certPreviewFilter, setCertPreviewFilter] = useState<'all' | 'matched' | 'blocked' | 'failed'>('all');
  const [certPreviewPageSize, setCertPreviewPageSize] = useState(10);
  const folderInputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['graduation-candidates', query],
    queryFn: () =>
      trainApi.listGraduationCandidates({
        page: query.page,
        pageSize: query.pageSize,
        keyword: query.keyword,
        graduateStatus: query.graduateStatus,
        trainTitle: query.trainTitle,
        companyName: query.companyName,
        workTypeName: query.workTypeName,
      }),
  });

  const EMPTY_FILTER = '__EMPTY__';

  const toFilterOptions = (values?: string[]) =>
    (values ?? []).map((v) => ({
      text: v === EMPTY_FILTER ? '（空）' : v,
      value: v,
    }));

  const trainTitleFilters = useMemo(
    () => toFilterOptions(data?.facets?.trainTitles),
    [data?.facets?.trainTitles],
  );
  const companyFilters = useMemo(
    () => toFilterOptions(data?.facets?.companyNames),
    [data?.facets?.companyNames],
  );
  const workTypeFilters = useMemo(
    () => toFilterOptions(data?.facets?.workTypeNames),
    [data?.facets?.workTypeNames],
  );

  const handleSearch = () => {
    const values = searchForm.getFieldsValue();
    setQuery((prev) => ({
      ...prev,
      page: 1,
      keyword: values.keyword || undefined,
      graduateStatus: values.graduateStatus || undefined,
    }));
  };

  const readFilterValues = (raw: unknown): string[] | undefined => {
    if (!Array.isArray(raw) || raw.length === 0) return undefined;
    return raw.map(String).filter(Boolean);
  };

  const selectedClassLabels = useMemo(() => {
    const list = (data?.list ?? []) as Record<string, unknown>[];
    const byId = new Map(list.map((r) => [String(r.id), String(r.title || r.id)]));
    return selectedClassKeys.map((k) => ({
      id: String(k),
      title: byId.get(String(k)) || String(k),
    }));
  }, [data?.list, selectedClassKeys]);

  const mergeCertFiles = (incoming: FileList | File[]) => {
    const next = Array.from(incoming).filter((f) => {
      const n = f.name.toLowerCase();
      return n.endsWith('.pdf') || n.endsWith('.zip');
    });
    if (next.length === 0) {
      message.warning('请选择 PDF 或 ZIP 文件');
      return;
    }
    setCertFiles((prev) => {
      const map = new Map(prev.map((f) => [`${f.name}:${f.size}:${f.lastModified}`, f]));
      next.forEach((f) => map.set(`${f.name}:${f.size}:${f.lastModified}`, f));
      return Array.from(map.values());
    });
    setCertPreview(null);
  };

  const openCertImport = () => {
    if (selectedClassKeys.length === 0) {
      message.warning('请先勾选要回填的班级');
      return;
    }
    setCertFiles([]);
    setCertPreview(null);
    setCertPreviewFilter('all');
    setCertImportOpen(true);
  };

  const runCertPreview = async () => {
    if (selectedClassKeys.length === 0) {
      message.warning('请先勾选班级');
      return;
    }
    if (certFiles.length === 0) {
      message.warning('请先添加证书文件夹或 PDF/ZIP');
      return;
    }
    setCertPreviewing(true);
    try {
      let res = await trainApi.previewGraduationCertImport(
        selectedClassKeys.map(String),
        certFiles,
      );
      if (res.async && res.jobId) {
        message.info(res.message || '文件较多，后台解析中…');
        const jobId = res.jobId;
        const started = Date.now();
        while (Date.now() - started < 280000) {
          await new Promise((r) => setTimeout(r, 800));
          const job = await trainApi.getGraduationCertImportPreviewJob(jobId);
          if (job.message) {
            message.open({
              key: 'cert-preview-job',
              type: 'loading',
              content: job.message,
              duration: 0,
            });
          }
          if (job.status === 'done') {
            message.destroy('cert-preview-job');
            res = job;
            break;
          }
          if (job.status === 'failed') {
            message.destroy('cert-preview-job');
            throw new Error(job.error || job.message || '预览失败');
          }
        }
        if (res.async && !res.previewId) {
          throw new Error('预览超时，请减少单次上传数量后重试');
        }
      }
      setCertPreview(res);
      setCertPreviewFilter('all');
      message.success(
        `预览完成：可回填 ${res.matched ?? 0}，需处理 ${res.blocked ?? 0}，失败 ${res.failed ?? 0}`,
      );
    } catch (e) {
      message.destroy('cert-preview-job');
      message.error((e as Error).message || '预览失败');
    } finally {
      setCertPreviewing(false);
    }
  };

  const runCertCommit = async () => {
    if (!certPreview?.previewId) {
      message.warning('请先预览匹配');
      return;
    }
    if ((certPreview.matched ?? 0) <= 0) {
      message.warning('没有可回填的匹配项');
      return;
    }
    setCertCommitting(true);
    try {
      const res = await trainApi.commitGraduationCertImport(certPreview.previewId);
      message.success(
        `回填完成：新建 ${res.written}，更新 ${res.updated}，跳过 ${res.skipped}`,
      );
      if (res.errors?.length) {
        Modal.warning({
          title: '部分跳过',
          content: res.errors.slice(0, 8).join('\n'),
        });
      }
      setCertImportOpen(false);
      setCertFiles([]);
      setCertPreview(null);
      void queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] });
      void queryClient.invalidateQueries({ queryKey: ['graduation-students'] });
    } catch (e) {
      message.error((e as Error).message || '回填失败');
    } finally {
      setCertCommitting(false);
    }
  };

  const certStatusTag = (status?: string) => {
    if (status === 'MATCHED') return <Tag color="success">可写入</Tag>;
    if (status === 'PARSE_FAILED') return <Tag color="error">解析失败</Tag>;
    if (status === 'NOT_IN_SCOPE') return <Tag color="warning">未在已选班</Tag>;
    if (status === 'DUP_IN_SELECTION') return <Tag color="error">身份证冲突</Tag>;
    if (status === 'DUP_CERT_NO' || status === 'EXISTING_CERT_CONFLICT') {
      return <Tag color="error">证号冲突</Tag>;
    }
    return <Tag>{status || '—'}</Tag>;
  };

  const certPreviewBucket = (item: { canCommit?: boolean; status?: string }) => {
    if (item.canCommit) return 'matched' as const;
    if (item.status === 'PARSE_FAILED') return 'failed' as const;
    return 'blocked' as const;
  };

  const filteredCertPreviewItems = useMemo(() => {
    const items = certPreview?.items ?? [];
    if (certPreviewFilter === 'all') return items;
    return items.filter((it) => certPreviewBucket(it) === certPreviewFilter);
  }, [certPreview?.items, certPreviewFilter]);

  const toggleCertPreviewFilter = (next: 'matched' | 'blocked' | 'failed') => {
    setCertPreviewFilter((prev) => (prev === next ? 'all' : next));
  };

  const columns: ColumnsType<Record<string, unknown>> = useMemo(
    () => [
      { title: 'ID', dataIndex: 'id', width: 80 },
      { title: '班级名称', dataIndex: 'title', width: 180, ellipsis: true },
      {
        title: '工种',
        dataIndex: 'workTypeName',
        width: 120,
        ellipsis: true,
        filters: workTypeFilters,
        filteredValue: query.workTypeName ?? null,
        filterSearch: true,
        onFilter: () => true,
        render: (v) => v || '—',
      },
      {
        title: '等级',
        dataIndex: 'workTypeLevel',
        width: 120,
        ellipsis: true,
        render: (v?: number | null) => formatClassLevel(v),
      },
      {
        title: '培训项目',
        dataIndex: 'trainTitle',
        width: 180,
        ellipsis: true,
        filters: trainTitleFilters,
        filteredValue: query.trainTitle ?? null,
        filterSearch: true,
        onFilter: () => true,
        render: (v) => v || '—',
      },
      {
        title: '企业',
        dataIndex: 'companyName',
        width: 140,
        ellipsis: true,
        filters: companyFilters,
        filteredValue: query.companyName ?? null,
        filterSearch: true,
        onFilter: () => true,
        render: (v) => v || '—',
      },
      {
        title: '结业进度',
        width: 110,
        render: (_, r) => `${r.graduatedCount ?? 0}/${r.studentCount ?? 0}`,
      },
      {
        title: '证书回填',
        width: 120,
        render: (_, r) => {
          const backfill = Number(r.certBackfillCount ?? 0);
          const total = Number(r.studentCount ?? 0);
          if (backfill <= 0) {
            return <Tag>未回填</Tag>;
          }
          return (
            <Tag color="success">
              已回填 {backfill}/{total}
            </Tag>
          );
        },
      },
      {
        title: '状态',
        dataIndex: 'graduateStatus',
        width: 110,
        render: (v?: string) => classStatusTag(v),
      },
      { title: '开始', dataIndex: 'dateStart', width: 110 },
      { title: '结束', dataIndex: 'dateEnd', width: 110 },
      {
        title: '操作',
        key: 'act',
        width: 160,
        fixed: 'right',
        render: (_, r) => (
          <Dropdown
            menu={{
              items: ROSTER_SCOPE_MENU_ITEMS,
              onClick: ({ key }) =>
                void downloadCertifiedRoster({
                  classId: r.id as number,
                  classTitle: String(r.title || r.trainTitle || ''),
                  scope: key as CertifiedRosterScope,
                }),
            }}
          >
            <Button type="link" size="small" icon={<DownloadOutlined />}>
              导出人员名册 <DownOutlined />
            </Button>
          </Dropdown>
        ),
      },
    ],
    [trainTitleFilters, companyFilters, workTypeFilters, query.trainTitle, query.companyName, query.workTypeName],
  );

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>{pageTitle}</Typography.Title>
          <Typography.Text type="secondary">{pageSubtitle}</Typography.Text>
        </div>
      </section>

      <TrainingProcessSteps current={processCurrent} />

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="点击班级进入详情查看学员；可勾选班级上传证书文件夹回填证号，或导出获证人员名册。"
      />

      <Card size="small" style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item name="keyword">
            <Input
              placeholder="搜索班级名称..."
              prefix={<SearchOutlined />}
              allowClear
              onPressEnter={handleSearch}
              style={{ width: 240 }}
            />
          </Form.Item>
          <Form.Item name="graduateStatus">
            <Select
              allowClear
              placeholder="结业状态"
              style={{ width: 140 }}
              options={[
                { value: 'pending', label: '未结业' },
                { value: 'partial', label: '部分结业' },
                { value: 'done', label: '已结业' },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={handleSearch}>
              查询
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card size="small">
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 12,
            marginBottom: 16,
            flexWrap: 'wrap',
          }}
        >
          <Space wrap>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => queryClient.invalidateQueries({ queryKey: ['graduation-candidates'] })}
            >
              刷新
            </Button>
            <Dropdown
              menu={{
                items: ROSTER_SCOPE_MENU_ITEMS,
                onClick: ({ key }) =>
                  void downloadCertifiedRoster({
                    classIds: selectedClassKeys.map(String),
                    scope: key as CertifiedRosterScope,
                  }),
              }}
              disabled={selectedClassKeys.length === 0}
            >
              <Button icon={<DownloadOutlined />} disabled={selectedClassKeys.length === 0}>
                导出已选名册 <DownOutlined />
              </Button>
            </Dropdown>
            {selectedClassKeys.length > 0 ? (
              <Button type="link" onClick={() => setSelectedClassKeys([])}>
                清空选择
              </Button>
            ) : null}
            <Typography.Text type="secondary">
              已选 {selectedClassKeys.length} 个班
            </Typography.Text>
          </Space>
          <Button
            icon={<FolderOpenOutlined />}
            disabled={selectedClassKeys.length === 0}
            title={selectedClassKeys.length === 0 ? '请先勾选要回填的班级' : undefined}
            onClick={openCertImport}
          >
            上传证书文件夹
          </Button>
        </div>
        <Table
          rowKey="id"
          size="middle"
          loading={isLoading}
          columns={columns}
          dataSource={(data?.list ?? []) as Record<string, unknown>[]}
          scroll={{ x: 1420 }}
          rowSelection={{
            selectedRowKeys: selectedClassKeys,
            onChange: (keys) => setSelectedClassKeys(keys),
            preserveSelectedRowKeys: true,
          }}
          onRow={(row) => ({
            style: { cursor: 'pointer' },
            onClick: (e) => {
              const el = e.target as HTMLElement | null;
              if (
                el?.closest(
                  '.ant-checkbox-wrapper, .ant-checkbox, input, button, a, .ant-table-filter-trigger, .ant-dropdown',
                )
              ) {
                return;
              }
              const id = Number(row.id);
              if (!Number.isFinite(id) || id <= 0) return;
              navigateTo(`/training/graduation/classes/${id}`);
            },
          })}
          onChange={(pagination, filters) => {
            const nextTrain = readFilterValues(filters.trainTitle);
            const nextCompany = readFilterValues(filters.companyName);
            const nextWorkType = readFilterValues(filters.workTypeName);
            setQuery((prev) => {
              const filtersChanged =
                JSON.stringify(nextTrain ?? []) !== JSON.stringify(prev.trainTitle ?? [])
                || JSON.stringify(nextCompany ?? []) !== JSON.stringify(prev.companyName ?? [])
                || JSON.stringify(nextWorkType ?? []) !== JSON.stringify(prev.workTypeName ?? []);
              const nextPageSize =
                typeof pagination.pageSize === 'number' && pagination.pageSize > 0
                  ? pagination.pageSize
                  : prev.pageSize;
              const sizeChanged = nextPageSize !== prev.pageSize;
              return {
                ...prev,
                trainTitle: nextTrain,
                companyName: nextCompany,
                workTypeName: nextWorkType,
                pageSize: nextPageSize,
                page: filtersChanged || sizeChanged ? 1 : (pagination.current ?? prev.page),
              };
            });
          }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: data?.total ?? 0,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            showTotal: (t) => `共 ${t} 条`,
          }}
        />
      </Card>

      <Modal
        title="上传证书文件夹并回填"
        open={certImportOpen}
        onCancel={() => {
          if (certPreviewing || certCommitting) return;
          setCertImportOpen(false);
        }}
        width={960}
        destroyOnHidden
        footer={[
          <Button
            key="cancel"
            onClick={() => setCertImportOpen(false)}
            disabled={certPreviewing || certCommitting}
          >
            取消
          </Button>,
          <Button
            key="preview"
            loading={certPreviewing}
            disabled={certCommitting || certFiles.length === 0}
            onClick={() => void runCertPreview()}
          >
            预览匹配
          </Button>,
          <Button
            key="commit"
            type="primary"
            loading={certCommitting}
            disabled={certPreviewing || !certPreview || (certPreview.matched ?? 0) <= 0}
            onClick={() => void runCertCommit()}
          >
            确认回填
          </Button>,
        ]}
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="按证书 PDF 内身份证号匹配已选班级花名册；姓名不作主键。可多次添加文件夹或 PDF/ZIP。"
        />
        <Typography.Paragraph style={{ marginBottom: 8 }}>
          已选班级（{selectedClassLabels.length}）：
          {selectedClassLabels.map((c) => (
            <Tag key={c.id} style={{ marginBottom: 4 }}>
              {c.title}（{c.id}）
            </Tag>
          ))}
        </Typography.Paragraph>
        <Space wrap style={{ marginBottom: 12 }}>
          <Button icon={<FolderOpenOutlined />} onClick={() => folderInputRef.current?.click()}>
            添加证书文件夹
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => fileInputRef.current?.click()}>
            添加 PDF/ZIP
          </Button>
          {certFiles.length > 0 ? (
            <Button
              type="link"
              onClick={() => {
                setCertFiles([]);
                setCertPreview(null);
                setCertPreviewFilter('all');
              }}
            >
              清空文件
            </Button>
          ) : null}
          <Typography.Text type="secondary">已选文件 {certFiles.length} 个</Typography.Text>
        </Space>
        <input
          ref={folderInputRef}
          type="file"
          multiple
          style={{ display: 'none' }}
          {...({ webkitdirectory: '', directory: '' } as Record<string, string>)}
          onChange={(e) => {
            if (e.target.files?.length) mergeCertFiles(e.target.files);
            e.target.value = '';
          }}
        />
        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept=".pdf,.zip,application/pdf,application/zip"
          style={{ display: 'none' }}
          onChange={(e) => {
            if (e.target.files?.length) mergeCertFiles(e.target.files);
            e.target.value = '';
          }}
        />
        {certFiles.length > 0 ? (
          <Typography.Paragraph type="secondary" ellipsis={{ rows: 2 }} style={{ marginBottom: 12 }}>
            {certFiles
              .slice(0, 12)
              .map((f) => f.name)
              .join('、')}
            {certFiles.length > 12 ? ` 等 ${certFiles.length} 个` : ''}
          </Typography.Paragraph>
        ) : null}

        {certPreview ? (
          <>
            <Space style={{ marginBottom: 8 }} wrap>
              <Tag
                color="success"
                style={{
                  cursor: 'pointer',
                  outline: certPreviewFilter === 'matched' ? '2px solid var(--ant-color-success)' : undefined,
                  outlineOffset: 1,
                }}
                onClick={() => toggleCertPreviewFilter('matched')}
              >
                可回填 {certPreview.matched ?? 0}
              </Tag>
              <Tag
                color="warning"
                style={{
                  cursor: 'pointer',
                  outline: certPreviewFilter === 'blocked' ? '2px solid var(--ant-color-warning)' : undefined,
                  outlineOffset: 1,
                }}
                onClick={() => toggleCertPreviewFilter('blocked')}
              >
                需处理 {certPreview.blocked ?? 0}
              </Tag>
              <Tag
                color="error"
                style={{
                  cursor: 'pointer',
                  outline: certPreviewFilter === 'failed' ? '2px solid var(--ant-color-error)' : undefined,
                  outlineOffset: 1,
                }}
                onClick={() => toggleCertPreviewFilter('failed')}
              >
                失败 {certPreview.failed ?? 0}
              </Tag>
              <Typography.Text type="secondary">
                共 {certPreview.totalFiles} 个文件
                {certPreview.ocrEnabled ? ' · OCR已启用' : ' · OCR未配置'}
                {certPreviewFilter !== 'all'
                  ? ` · 当前筛选 ${filteredCertPreviewItems.length} 条（再点标签可取消）`
                  : ''}
              </Typography.Text>
            </Space>
            <Table
              rowKey={(r) => `${r.fileName}-${r.idCard || ''}-${r.status}`}
              size="small"
              pagination={{
                pageSize: certPreviewPageSize,
                showSizeChanger: true,
                pageSizeOptions: ['10', '20', '50', '100'],
                showTotal: (t) => `共 ${t} 条`,
                onChange: (_p, ps) => {
                  if (typeof ps === 'number' && ps > 0) {
                    setCertPreviewPageSize(ps);
                  }
                },
              }}
              scroll={{ x: 1400, y: 320 }}
              dataSource={filteredCertPreviewItems}
              columns={[
                { title: '文件', dataIndex: 'fileName', ellipsis: true, width: 200 },
                { title: '证面姓名', dataIndex: 'parsedName', width: 88 },
                { title: '证书编号', dataIndex: 'certificateNo', width: 180, ellipsis: true },
                { title: '身份证', dataIndex: 'idCard', width: 150 },
                { title: '职业', dataIndex: 'occupation', width: 100, ellipsis: true },
                { title: '工种', dataIndex: 'workType', width: 100, ellipsis: true },
                { title: '等级', dataIndex: 'level', width: 110 },
                { title: '等级(归一)', dataIndex: 'levelNormalized', width: 110 },
                { title: '发证机构', dataIndex: 'issuingAuthority', width: 140, ellipsis: true },
                { title: '评价机构', dataIndex: 'evaluationOrg', width: 140, ellipsis: true },
                { title: '核发日期', dataIndex: 'issueDate', width: 110 },
                {
                  title: '查询地址',
                  dataIndex: 'queryUrlList',
                  width: 160,
                  ellipsis: true,
                  render: (v?: string[]) => (v?.length ? v.join('；') : '—'),
                },
                {
                  title: '匹配学员',
                  width: 150,
                  render: (_, r) =>
                    r.realname
                      ? `${r.realname}${r.classTitle ? ` · ${r.classTitle}` : ''}`
                      : '—',
                },
                {
                  title: '状态',
                  width: 100,
                  render: (_, r) => certStatusTag(r.status),
                },
                {
                  title: '说明',
                  width: 180,
                  ellipsis: true,
                  render: (_, r) => r.warning || r.message || '—',
                },
              ]}
            />
          </>
        ) : null}
      </Modal>
    </div>
  );
}
