/**
 * 开班申报 — 对齐 yun_shang classApplication.vue
 * 班级列表 + 材料模板管理 + 班级材料挂载 + 生成材料包
 */
import { useMemo, useState } from 'react';
import {
  Alert,
  AutoComplete,
  Button,
  Card,
  Empty,
  Form,
  Input,
  Modal,
  Pagination,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import { InboxOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import { useQuery } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';
import { fileNameFromUrl, downloadUrlFile } from '../../api/files';
import type {
  TrainFileTempItem,
  TrainFileTempSavePayload,
} from '../../api/training';
import type {
  DemandPublicData,
  TrainInfoClassListItem,
} from '../../types/training';
import './TrainingClassDeclarePage.css';
import { TrainingProcessSteps } from './TrainingProcessSteps';

const { Dragger } = Upload;

const FILE_TEMP_TITLES = ['开班申请表', '导入表', '开班花名册', '课时导入表'] as const;

function materialCoverage(rows: TrainFileTempItem[]) {
  const titles = new Set(rows.map((r) => r.title).filter(Boolean));
  const missing = FILE_TEMP_TITLES.filter((t) => !titles.has(t));
  return { titles, missing, ready: missing.length === 0 };
}

function errMsg(e: unknown, fallback: string): string {
  return e instanceof Error && e.message ? e.message : fallback;
}

function uploadAcceptByTitle(title?: string): string {
  if (title === '开班申请表') return '.docx';
  if (title === '导入表' || title === '开班花名册' || title === '课时导入表') return '.xlsx,.xls';
  return '.docx,.xlsx,.xls';
}

function isExcelExt(ext?: string): boolean {
  return ext === 'xlsx' || ext === 'xls';
}

function fileTypeHint(title?: string): string {
  if (title === '开班申请表') {
    return '上传已含 ${pxdw}、${company}、${worktype}、${level}、${nums}、${pxtime}、${teacher1} 等占位符的 Word 即可；生成材料时自动填入班级信息。「设置变量后的Word」可选，用于覆盖原件。';
  }
  if (title === '导入表' || title === '开班花名册' || title === '课时导入表') {
    return `上传 Excel 模板（.xlsx/.xls），须为人社部门提供的「${title}」标准表头`;
  }
  return '请先选择文件名称，再上传对应格式的模板文件';
}

const GUANGXI_CITIES = [
  '南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市',
  '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市',
];

function parseFileTempIds(raw: unknown): number[] {
  if (raw == null || raw === '') return [];
  if (Array.isArray(raw)) return raw.map((v) => Number(v)).filter((n) => !Number.isNaN(n));
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed.map((v) => Number(v)).filter((n) => !Number.isNaN(n));
    } catch {
      return raw.replace(/[\[\]]/g, '').split(',').map((s) => Number(s.trim())).filter((n) => !Number.isNaN(n));
    }
  }
  return [];
}

function fileUrl(path?: string): string {
  if (!path) return '';
  if (path.startsWith('http') || path.startsWith('/api/')) return path;
  return path.startsWith('/') ? path : `/${path}`;
}

export function TrainingClassDeclarePage() {
  const [companyIdFilter, setCompanyIdFilter] = useState<number | undefined>();
  const [workTypeIdFilter, setWorkTypeIdFilter] = useState<number | undefined>();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);
  const [generating, setGenerating] = useState(false);

  const [fileTempOpen, setFileTempOpen] = useState(false);
  const [fileTempImport, setFileTempImport] = useState(false);
  const [fileTempRows, setFileTempRows] = useState<TrainFileTempItem[]>([]);
  const [selectedTempKeys, setSelectedTempKeys] = useState<React.Key[]>([]);

  const [fileInfoOpen, setFileInfoOpen] = useState(false);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadField, setUploadField] = useState<'pathSource' | 'pathUsed'>('pathSource');
  const [uploadAccept, setUploadAccept] = useState('.docx,.xlsx');
  const [formFileTemp] = Form.useForm<TrainFileTempSavePayload & { ext?: string }>();

  const [classMaterialOpen, setClassMaterialOpen] = useState(false);
  const [classId, setClassId] = useState(0);
  const [classTitle, setClassTitle] = useState('');
  const [materialRows, setMaterialRows] = useState<TrainFileTempItem[]>([]);
  const attachedTempIds = useMemo(() => new Set(materialRows.map((r) => r.id)), [materialRows]);
  const coverage = useMemo(() => materialCoverage(materialRows), [materialRows]);

  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);

  const { data: publicData } = useQuery({
    queryKey: ['demand-public-data', 'class-declare'],
    queryFn: () => trainApi.getDemandPublicData(['company', 'worktype']),
    staleTime: 5 * 60 * 1000,
  });

  const { data: classData, isLoading, refetch } = useQuery({
    queryKey: ['info-classes-declare', currentPage, pageSize, companyIdFilter, workTypeIdFilter],
    queryFn: () =>
      trainApi.pageInfoClassList({
        page: currentPage,
        pageSize,
        companyId: companyIdFilter,
        workTypeId: workTypeIdFilter,
      }),
  });

  const workTypeOptions = useMemo(() => {
    const groups = (publicData as DemandPublicData | undefined)?.worktype_list ?? [];
    return groups.flatMap((g: { title: string; options?: { id: number; title: string }[] }) =>
      (g.options ?? []).map((o: { id: number; title: string }) => ({
        value: o.id,
        label: o.title,
      })),
    );
  }, [publicData]);

  const loadFileTemps = async (ids: number[] = [], forMaterial = false) => {
    const res = await trainApi.listFileTemps({ type: 1, ids: ids.length ? ids : undefined });
    const list = res?.list ?? [];
    if (forMaterial) {
      setMaterialRows(ids.length ? list : []);
    } else {
      setFileTempRows(list);
      setFileTempOpen(true);
    }
  };

  const handleReset = () => {
    setCompanyIdFilter(undefined);
    setWorkTypeIdFilter(undefined);
    setCurrentPage(1);
    refetch();
  };

  const openTemplateManage = () => {
    setFileTempImport(false);
    setSelectedTempKeys([]);
    loadFileTemps([]);
  };

  const resetFileTempForm = () => {
    formFileTemp.resetFields();
    formFileTemp.setFieldsValue({ type: 1, id: undefined, city: '', title: '', pathSource: '', pathUsed: '' });
  };

  const handleFileAdd = () => {
    resetFileTempForm();
    setFileInfoOpen(true);
  };

  const handleFileEdit = (row: TrainFileTempItem) => {
    formFileTemp.setFieldsValue({
      id: row.id,
      type: 1,
      city: row.city,
      title: row.title,
      pathSource: row.path_source,
      pathUsed: row.path_used,
      ext: row.ext,
    });
    setFileInfoOpen(true);
  };

  const handleFileDelete = (row: TrainFileTempItem) => {
    Modal.confirm({
      title: '友情提示',
      content: '确定要删除该文件吗？',
      onOk: async () => {
        const res = await trainApi.batchDeleteFileTemps([row.id]);
        if (res?.count) {
          message.success('删除成功');
          loadFileTemps([]);
        }
      },
    });
  };

  const fileUploadSubmit = async () => {
    const v = await formFileTemp.validateFields();
    if (!v.title) {
      message.error('请填写文件名称');
      return;
    }
    if (!v.pathSource) {
      message.error('请上传文件');
      return;
    }
    if (v.title === '开班申请表' && v.ext !== 'docx') {
      message.error('开班申请表须上传 Word 文档（.docx）');
      return;
    }
    if (v.title !== '开班申请表' && !isExcelExt(v.ext)) {
      message.error(`${v.title}须上传 Excel 文件（.xlsx/.xls）`);
      return;
    }
    const res = await trainApi.saveFileTemp({
      id: v.id,
      type: 1,
      city: v.city,
      title: v.title,
      pathSource: v.pathSource,
      pathUsed: v.pathUsed,
      ext: v.ext,
    });
    if (res?.info) {
      message.success('成功');
      setFileInfoOpen(false);
      resetFileTempForm();
      loadFileTemps([]);
    }
  };

  const openUpload = (field: 'pathSource' | 'pathUsed', ext: string) => {
    setUploadField(field);
    setUploadAccept(ext);
    setUploadOpen(true);
  };

  const uploadProps: UploadProps = {
    multiple: false,
    maxCount: 1,
    showUploadList: false,
    accept: uploadAccept,
    className: 'upload-demos',
    customRequest: async ({ file, onSuccess, onError }) => {
      try {
        const result = await uploadFile(file as File, 'public');
        const url = `/api/files/${result.id}/download`;
        formFileTemp.setFieldValue(uploadField, url);
        if (uploadField === 'pathSource') {
          const name = (file as File).name;
          const ext = name.includes('.') ? name.split('.').pop()?.toLowerCase() : '';
          formFileTemp.setFieldValue('ext', ext);
        }
        message.success('上传成功');
        setUploadOpen(false);
        onSuccess?.(result);
      } catch (e) {
        message.error('上传失败');
        onError?.(e as Error);
      }
    },
  };

  const handleFileImport = async () => {
    if (!selectedTempKeys.length) {
      message.error('请选择要导入的模板');
      return;
    }
    const ids = selectedTempKeys.map((k) => Number(k));
    try {
      const res = await trainApi.attachFileTemps(classId, ids);
      if (res?.count != null || res?.ids) {
        message.success('导入成功（同名模板已自动替换）');
        setFileTempOpen(false);
        setFileTempRows([]);
        const newIds = res.ids ?? res.fileTemps ?? ids;
        await loadFileTemps(newIds, true);
        refetch();
      }
    } catch (e) {
      message.error(errMsg(e, '导入失败'));
    }
  };

  const createFile = async (row: { id: number | string; title?: string }) => {
    setGenerating(true);
    try {
      const res = await trainApi.generateMaterials(Number(row.id));
      if (res?.downUrl) {
        await downloadUrlFile(res.downUrl, `${row.title || '班级'}_申报材料.zip`);
        message.success('材料已生成');
      } else {
        message.warning('未返回下载地址');
      }
    } catch (e) {
      message.error(errMsg(e, '生成失败'));
    } finally {
      setGenerating(false);
    }
  };

  const manageFile = (row: TrainInfoClassListItem) => {
    setClassId(Number(row.id));
    setClassTitle(row.title || '');
    const ids = parseFileTempIds(row.file_temps);
    loadFileTemps(ids, true);
    setClassMaterialOpen(true);
  };

  const handleMaterialDelete = (row: TrainFileTempItem) => {
    Modal.confirm({
      title: '友情提示',
      content: `确定从本班移除「${row.title}」吗？模板本身不会删除。`,
      onOk: async () => {
        try {
          const res = await trainApi.detachFileTemp(classId, row.id);
          if (res?.fileTemps) {
            message.success('已移除');
            await loadFileTemps(res.fileTemps, true);
            refetch();
          }
        } catch (e) {
          message.error(errMsg(e, '移除失败'));
        }
      },
    });
  };

  const handleMaterialAdd = async () => {
    setFileTempImport(true);
    setSelectedTempKeys([]);
    await loadFileTemps([]);
  };

  const onPreviewFile = (row: TrainFileTempItem, field: 'urlSource' | 'urlUsed' = 'urlSource') => {
    const url = field === 'urlUsed'
      ? (row.urlUsed || row.path_used || row.urlSource || row.path_source)
      : (row.urlSource || row.path_source);
    if (!url) {
      message.warning('暂无可预览文件');
      return;
    }
    const resolved = fileUrl(url);
    setPreviewTarget({
      fileUrl: resolved,
      fileName: fileNameFromUrl(resolved) || row.title || 'preview',
      fileType: row.ext,
    });
  };

  const classColumns: ColumnsType<TrainInfoClassListItem> = [
    { title: '序号', width: 60, align: 'center', render: (_v, _r, i) => (currentPage - 1) * pageSize + i + 1 },
    { title: '班级名称', dataIndex: 'title', align: 'center' },
    { title: '公司', dataIndex: 'company_name', align: 'center' },
    { title: '工种', dataIndex: 'work_type_title', align: 'center' },
    { title: '人数', dataIndex: 'nums', width: 80, align: 'center' },
    { title: '地点', dataIndex: 'position', align: 'center' },
    { title: '课时', dataIndex: 'course_num', align: 'center' },
    {
      title: '操作',
      width: 220,
      align: 'center',
      render: (_, row) => {
        const attached = parseFileTempIds(row.file_temps).length;
        return (
          <Space>
            <Button type="link" className="link-action" loading={generating} onClick={() => createFile(row)}>
              生成材料
            </Button>
            <Button type="link" className="link-action" onClick={() => manageFile(row)}>
              材料管理{attached > 0 ? `(${attached})` : ''}
            </Button>
          </Space>
        );
      },
    },
  ];

  const tempColumns: ColumnsType<TrainFileTempItem> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '城市', dataIndex: 'city', width: 100 },
    { title: '文件名称', dataIndex: 'title' },
    { title: '文件类型', dataIndex: 'ext', width: 100 },
    {
      title: '原文件',
      width: 160,
      render: (_, row) => (
        <Button type="link" onClick={() => onPreviewFile(row, 'urlSource')}>
          预览
        </Button>
      ),
    },
    {
      title: '使用文件',
      width: 160,
      render: (_, row) =>
        row.ext === 'docx' ? (
          <Space>
            <a href={fileUrl(row.urlSource)} target="_blank" rel="noreferrer" className="mar-r10">
              下载
            </a>
            {row.path_used ? (
              <Button type="link" onClick={() => onPreviewFile(row, 'urlUsed')}>
                预览
              </Button>
            ) : null}
          </Space>
        ) : null,
    },
    ...(!fileTempImport
      ? [{
          title: '操作',
          width: 150,
          align: 'center' as const,
          render: (_: unknown, row: TrainFileTempItem) => (
            <Space>
              <Button type="link" onClick={() => handleFileEdit(row)}>编辑</Button>
              <Button type="link" onClick={() => handleFileDelete(row)}>删除</Button>
            </Space>
          ),
        }]
      : []),
  ];

  const materialColumns: ColumnsType<TrainFileTempItem> = [
    { title: '城市', dataIndex: 'city', width: 100, render: (v) => v || '-' },
    { title: '文件名称', dataIndex: 'title' },
    {
      title: '文件类型',
      dataIndex: 'ext',
      width: 100,
      render: (ext: string) => (ext ? <Tag>{ext}</Tag> : '-'),
    },
    {
      title: '文件',
      width: 160,
      render: (_, row) => {
        const canPreviewUsed = Boolean(row.path_used || row.urlUsed);
        const canPreviewSource = Boolean(row.path_source || row.urlSource);
        if (row.ext === 'docx') {
          if (canPreviewUsed) {
            return <Button type="link" onClick={() => onPreviewFile(row, 'urlUsed')}>预览</Button>;
          }
          if (canPreviewSource) {
            return <Button type="link" onClick={() => onPreviewFile(row, 'urlSource')}>预览</Button>;
          }
          return <Typography.Text type="secondary">未上传</Typography.Text>;
        }
        if (isExcelExt(row.ext) && canPreviewSource) {
          return <Button type="link" onClick={() => onPreviewFile(row, 'urlSource')}>预览</Button>;
        }
        return <Typography.Text type="secondary">-</Typography.Text>;
      },
    },
    {
      title: '操作',
      width: 100,
      align: 'center',
      render: (_, row) => (
        <Button type="link" danger onClick={() => handleMaterialDelete(row)}>移除</Button>
      ),
    },
  ];

  const importTempColumns: ColumnsType<TrainFileTempItem> = [
    ...tempColumns.filter((c) => c.title !== '操作'),
    {
      title: '状态',
      width: 100,
      align: 'center',
      render: (_, row) =>
        attachedTempIds.has(row.id) ? <Tag color="blue">已挂载</Tag> : <Tag>可选</Tag>,
    },
  ];

  return (
    <div className="training-class-declare-page training-page training-table-container">
      <section className="page-heading">
        <Typography.Title level={4}>开班申报</Typography.Title>
        <Typography.Text type="secondary">管理开班申报材料，配置模板并生成申报文件</Typography.Text>
      </section>

      <TrainingProcessSteps current={4} />

      <Card size="small" style={{ marginBottom: 16 }}>
      <div className="filter-bar">
        <Space wrap>
          <Select
            placeholder="企业筛选"
            allowClear
            showSearch
            style={{ width: 200 }}
            value={companyIdFilter}
            onChange={(v) => { setCompanyIdFilter(v); setCurrentPage(1); }}
            options={(publicData as DemandPublicData | undefined)?.company_list?.map((c: { id: number; title: string }) => ({
              value: c.id,
              label: c.title,
            }))}
            filterOption={(input, opt) => (opt?.label as string ?? '').includes(input)}
          />
          <Select
            placeholder="请选择工种"
            allowClear
            showSearch
            style={{ width: 200 }}
            value={workTypeIdFilter}
            onChange={(v) => { setWorkTypeIdFilter(v); setCurrentPage(1); }}
            options={workTypeOptions}
            filterOption={(input, opt) => (opt?.label as string ?? '').includes(input)}
          />
          <Button type="primary" onClick={handleReset}>重置</Button>
          <Button type="primary" onClick={openTemplateManage}>材料模板管理</Button>
        </Space>
      </div>
      </Card>

      <div className="table-wrap">
        <Table
          rowKey="id"
          bordered
          loading={isLoading}
          columns={classColumns}
          dataSource={classData?.list ?? []}
          pagination={false}
          scroll={{ y: 600 }}
        />
      </div>

      <div className="pagination-bar">
        <Pagination
          current={currentPage}
          pageSize={pageSize}
          total={classData?.total ?? 0}
          showSizeChanger={false}
          showTotal={(t) => `共 ${t} 条`}
          onChange={(p) => setCurrentPage(p)}
        />
      </div>

      {/* 材料模板列表 */}
      <Modal
        title="材料模板列表"
        open={fileTempOpen}
        width="50%"
        onCancel={() => { setFileTempRows([]); setFileTempOpen(false); setFileTempImport(false); }}
        footer={null}
        destroyOnHidden
      >
        <Alert
          type="info"
          showIcon
          className="mar-b10"
          message="Word 与 Excel 需分别新建模板"
          description="开班申请表：上传一份已插入 ${pxdw}、${company}、${worktype}、${level}、${nums}、${pxtime}、${teacher1} 等变量的 Word 即可；生成材料时自动写入班级信息。也可另传「设置变量后的Word」覆盖。Excel 模板（导入表/花名册/课时表）须单独新建。"
        />
        {fileTempImport ? (
          <Button className="mar-b10" type="primary" onClick={handleFileImport}>确认导入</Button>
        ) : (
          <Button className="mar-b10" type="primary" onClick={handleFileAdd}>新增</Button>
        )}
        <Table
          rowKey="id"
          bordered
          size="small"
          columns={fileTempImport ? importTempColumns : tempColumns}
          dataSource={fileTempRows}
          scroll={{ y: 300 }}
          pagination={false}
          locale={{ emptyText: <Empty description={fileTempImport ? '暂无可用模板，请先在材料模板管理中新增' : '暂无模板'} /> }}
          rowSelection={fileTempImport ? {
            selectedRowKeys: selectedTempKeys,
            onChange: (keys) => setSelectedTempKeys(keys),
            getCheckboxProps: (row) => ({
              disabled: attachedTempIds.has(row.id),
            }),
          } : undefined}
        />
      </Modal>

      {/* 模板编辑 */}
      <Modal
        title="文件模板信息"
        open={fileInfoOpen}
        width="50%"
        onCancel={() => { setFileInfoOpen(false); resetFileTempForm(); }}
        onOk={fileUploadSubmit}
        okText="确定"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={formFileTemp} labelCol={{ span: 8 }} wrapperCol={{ span: 16 }}>
          <Form.Item name="id" hidden><Input /></Form.Item>
          <Form.Item name="type" hidden><Input /></Form.Item>
          <Form.Item name="ext" hidden><Input /></Form.Item>
          <Form.Item noStyle shouldUpdate={(p, c) => p.title !== c.title}>
            {() => (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
                message={fileTypeHint(formFileTemp.getFieldValue('title'))}
              />
            )}
          </Form.Item>
          <Form.Item name="title" label="文件名称" rules={[{ required: true }]}>
            <Select
              placeholder="请选择名称"
              options={FILE_TEMP_TITLES.map((t) => ({ value: t, label: t }))}
              onChange={() => {
                formFileTemp.setFieldsValue({ pathSource: '', pathUsed: '', ext: '' });
              }}
            />
          </Form.Item>
          <Form.Item name="pathSource" label="文件" rules={[{ required: true }]}>
            <Input
              readOnly
              addonAfter={
                <Button
                  type="link"
                  onClick={() => {
                    const title = formFileTemp.getFieldValue('title');
                    if (!title) {
                      message.error('请先选择文件名称');
                      return;
                    }
                    openUpload('pathSource', uploadAcceptByTitle(title));
                  }}
                >
                  上传
                </Button>
              }
            />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(p, c) => p.title !== c.title}>
            {() =>
              formFileTemp.getFieldValue('title') === '开班申请表' ? (
                <Form.Item name="city" label="地市" rules={[{ required: true, message: '请选择或输入地市' }]}>
                  <AutoComplete
                    allowClear
                    placeholder="请选择或输入地市"
                    options={GUANGXI_CITIES.map((c) => ({ value: c }))}
                    filterOption={(input, option) =>
                      String(option?.value ?? '')
                        .toLowerCase()
                        .includes(input.trim().toLowerCase())
                    }
                  />
                </Form.Item>
              ) : null
            }
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(p, c) => p.title !== c.title || p.ext !== c.ext}>
            {() => {
              const title = formFileTemp.getFieldValue('title');
              const fileExt = formFileTemp.getFieldValue('ext');
              if (title !== '开班申请表' || fileExt !== 'docx') return null;
              return (
                <Form.Item
                  name="pathUsed"
                  label="设置变量后的Word"
                  extra="可选。默认使用上方「文件」；若需单独一份含占位符的 Word，可在此上传覆盖"
                >
                  <Input
                    readOnly
                    addonAfter={
                      <Button type="link" onClick={() => openUpload('pathUsed', '.docx')}>上传</Button>
                    }
                  />
                </Form.Item>
              );
            }}
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="上传文件"
        open={uploadOpen}
        width={500}
        onCancel={() => setUploadOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Dragger {...uploadProps}>
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p className="ant-upload-text">将文件拖到此处，或<em>点击上传</em></p>
          <p style={{ color: '#ccc', marginTop: 10 }}>提示:上传过程中,请不要做任何操作</p>
          <p className="ant-upload-hint">只能上传{uploadAccept}文件</p>
        </Dragger>
      </Modal>

      <FilePreviewModal
        open={previewTarget != null}
        target={previewTarget}
        onClose={() => setPreviewTarget(null)}
      />

      {/* 班级材料管理 */}
      <Modal
        title={classTitle ? `班级材料管理 — ${classTitle}` : '班级材料管理'}
        open={classMaterialOpen}
        width="60%"
        onCancel={() => {
          setClassMaterialOpen(false);
          setFileTempImport(false);
          setMaterialRows([]);
          setClassTitle('');
        }}
        footer={null}
        destroyOnHidden
      >
        <Alert
          className="mar-b10"
          showIcon
          type={coverage.ready ? 'success' : 'warning'}
          message={coverage.ready ? '四类模板已齐全，可生成申报材料包' : '模板尚未齐全'}
          description={
            coverage.ready
              ? '包含：开班申请表、导入表、开班花名册、课时导入表'
              : `缺少：${coverage.missing.join('、')}。请点击「新增」从模板库挂载。`
          }
        />
        <Space className="mar-b10" wrap>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleMaterialAdd}>新增</Button>
          <Button
            type="primary"
            loading={generating}
            disabled={!materialRows.length}
            onClick={() => createFile({ id: classId, title: classTitle })}
          >
            生成材料
          </Button>
        </Space>
        <Table
          rowKey="id"
          bordered
          size="small"
          columns={materialColumns}
          dataSource={materialRows}
          scroll={{ y: 300 }}
          pagination={false}
          locale={{
            emptyText: (
              <Empty
                description="尚未挂载模板"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              >
                <Button type="primary" onClick={handleMaterialAdd}>从模板库导入</Button>
              </Empty>
            ),
          }}
        />
      </Modal>
    </div>
  );
}
