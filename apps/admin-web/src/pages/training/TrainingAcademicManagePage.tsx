import { useState } from 'react';
import {
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  DownloadOutlined,
  UploadOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadPlatformFile } from '../../api/files';
import { ApiAuthImage } from '../../components/ApiAuthImage';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';
import type { TrainTeacherBank, TrainTeacherBankCreateReq } from '../../types/training';

const STATUS_OPTIONS = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' },
];

const GENDER_OPTIONS = [
  { value: 1, label: '男' },
  { value: 0, label: '女' },
];

const SOURCE_LABEL: Record<string, string> = {
  legacy: '老系统迁移',
  excel: 'Excel 导入',
  manual: '手工录入',
};

/** 仅展示合法身份证号；占位文案显示为 — */
function displayIdCardNumber(value?: string | null): string {
  const v = value?.trim();
  if (!v) return '—';
  if (/^[0-9Xx]{15}$/.test(v) || /^[0-9Xx]{18}$/.test(v)) return v;
  return '—';
}

function genderLabel(v?: number | null): string {
  if (v === 1) return '男';
  if (v === 0) return '女';
  return '—';
}

function filePreviewUrl(fileId?: number | null): string | undefined {
  if (fileId == null) return undefined;
  return `/api/files/${fileId}/download?inline=true`;
}

const FILE_SLOTS: Array<{ type: string; label: string; field: keyof TrainTeacherBank }> = [
  { type: 'ID_FRONT', label: '身份证正面', field: 'idCardFrontFileId' },
  { type: 'ID_BACK', label: '身份证反面', field: 'idCardBackFileId' },
  { type: 'GRADUATE', label: '毕业证', field: 'graduateFileId' },
  { type: 'EDUCATION', label: '学历证', field: 'educationFileId' },
  { type: 'TEACHER_QUAL', label: '教师资格证', field: 'teacherQualFileId' },
  { type: 'TITLE', label: '职称证', field: 'titleCertFileId' },
  { type: 'SKILL', label: '技能等级证书', field: 'skillCertFileId' },
];

function fillTeacherForm(
  form: ReturnType<typeof Form.useForm<TrainTeacherBankCreateReq>>[0],
  row: TrainTeacherBank,
) {
  form.setFieldsValue({
    name: row.name,
    phone: row.phone,
    idCardNumber: displayIdCardNumber(row.idCardNumber) === '—' ? undefined : row.idCardNumber,
    company: row.company,
    gender: row.gender,
    nation: row.nation,
    birthday: row.birthday,
    startDate: row.startDate,
    endDate: row.endDate,
    education: row.education,
    major: row.major,
    skillName: row.skillName,
    qualificationLevel: row.qualificationLevel,
    professionalTitle: row.professionalTitle,
    certText: row.certText,
    teacherQualNo: row.teacherQualNo,
    titleCertNo: row.titleCertNo,
    skillCertNo: row.skillCertNo,
    status: row.status ?? 1,
    address: row.address,
  });
}

type LicensePhotosProps = {
  teacher: TrainTeacherBank;
  teacherId: number;
  onPreview: (target: FilePreviewTarget) => void;
  onUploaded: () => void;
};

function LicensePhotos({ teacher, teacherId, onPreview, onUploaded }: LicensePhotosProps) {
  const bindMu = useMutation({
    mutationFn: ({ type, fileId }: { type: string; fileId: number }) =>
      trainApi.bindTeacherBankFile(teacherId, type, fileId),
    onSuccess: () => {
      message.success('证照已更新');
      onUploaded();
    },
    onError: (e: Error) => message.error(e.message || '绑定失败'),
  });

  const clearMu = useMutation({
    mutationFn: (type: string) => trainApi.clearTeacherBankFile(teacherId, type),
    onSuccess: () => {
      message.success('已清除');
      onUploaded();
    },
    onError: (e: Error) => message.error(e.message || '清除失败'),
  });

  return (
    <Row gutter={[12, 12]}>
      {FILE_SLOTS.map((slot) => {
        const fileId = teacher[slot.field] as number | null | undefined;
        const url = filePreviewUrl(fileId);
        return (
          <Col key={slot.type} xs={24} sm={12} md={8}>
            <Card
              size="small"
              title={slot.label}
              extra={
                fileId ? (
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    #{fileId}
                  </Typography.Text>
                ) : null
              }
              styles={{ body: { padding: 12 } }}
            >
              <div
                style={{
                  height: 140,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: '#fafafa',
                  borderRadius: 6,
                  overflow: 'hidden',
                  cursor: fileId ? 'pointer' : 'default',
                  marginBottom: 8,
                }}
                onClick={() => {
                  if (!fileId) return;
                  onPreview({ fileId: String(fileId), fileName: slot.label });
                }}
              >
                {url ? (
                  <ApiAuthImage
                    url={url}
                    alt={slot.label}
                    style={{ maxWidth: '100%', maxHeight: 140, objectFit: 'contain' }}
                    preview={false}
                  />
                ) : (
                  <Typography.Text type="secondary">暂无影像</Typography.Text>
                )}
              </div>
              <Space size="small" wrap>
                <Upload
                  accept="image/*,.pdf"
                  showUploadList={false}
                  beforeUpload={async (file) => {
                    try {
                      const uploaded = await uploadPlatformFile(file, 'private');
                      bindMu.mutate({ type: slot.type, fileId: uploaded.id });
                    } catch (e) {
                      message.error(e instanceof Error ? e.message : '上传失败');
                    }
                    return false;
                  }}
                >
                  <Button size="small" loading={bindMu.isPending}>
                    {fileId ? '更换' : '上传'}
                  </Button>
                </Upload>
                {fileId ? (
                  <>
                    <Button
                      size="small"
                      onClick={() =>
                        onPreview({ fileId: String(fileId), fileName: slot.label })
                      }
                    >
                      查看
                    </Button>
                    <Button
                      size="small"
                      danger
                      loading={clearMu.isPending}
                      onClick={() => clearMu.mutate(slot.type)}
                    >
                      清除
                    </Button>
                  </>
                ) : null}
              </Space>
            </Card>
          </Col>
        );
      })}
    </Row>
  );
}

export function TrainingAcademicManagePage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<TrainTeacherBankCreateReq>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [previewTarget, setPreviewTarget] = useState<FilePreviewTarget | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['teacher-banks', page, pageSize, keyword],
    queryFn: () => trainApi.pageTeacherBanks({ page, pageSize, keyword: keyword || undefined }),
  });

  const { data: detail, isLoading: detailLoading } = useQuery({
    queryKey: ['teacher-bank-detail', detailId],
    queryFn: () => trainApi.getTeacherBank(detailId!),
    enabled: detailId != null,
  });

  const { data: editingDetail, isLoading: editingLoading } = useQuery({
    queryKey: ['teacher-bank-detail', editingId],
    queryFn: () => trainApi.getTeacherBank(editingId!),
    enabled: editingId != null && modalOpen,
  });

  const invalidateTeacher = (id?: number | null) => {
    queryClient.invalidateQueries({ queryKey: ['teacher-banks'] });
    if (id != null) {
      queryClient.invalidateQueries({ queryKey: ['teacher-bank-detail', id] });
    } else {
      queryClient.invalidateQueries({ queryKey: ['teacher-bank-detail'] });
    }
  };

  const createMu = useMutation({
    mutationFn: (payload: TrainTeacherBankCreateReq) => trainApi.createTeacherBank(payload),
    onSuccess: () => {
      message.success('添加成功');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      invalidateTeacher();
    },
    onError: (e: Error) => message.error(e.message || '添加失败'),
  });

  const updateMu = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: TrainTeacherBankCreateReq }) =>
      trainApi.updateTeacherBank(id, payload),
    onSuccess: (_res, vars) => {
      message.success('保存成功');
      setModalOpen(false);
      setEditingId(null);
      form.resetFields();
      invalidateTeacher(vars.id);
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number) => trainApi.deleteTeacherBank(id),
    onSuccess: () => {
      message.success('删除成功');
      invalidateTeacher();
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const importMu = useMutation({
    mutationFn: (file: File) => trainApi.importTeacherBanks(file),
    onSuccess: (res) => {
      message.success(
        `导入完成：成功 ${res.success}（新建 ${res.created} / 更新 ${res.updated} / 临时 ${res.provisional}），失败 ${res.failed?.length ?? 0}`,
      );
      invalidateTeacher();
    },
    onError: (e: Error) => message.error(e.message || '导入失败'),
  });

  const migrateMu = useMutation({
    mutationFn: (file: File) => trainApi.migrateTeacherBanksFromLegacy(file, true),
    onSuccess: (res) => {
      message.success(
        `迁移完成：成功 ${res.success}（新建 ${res.created} / 更新 ${res.updated}），失败 ${res.failed?.length ?? 0}，警告 ${res.warnings?.length ?? 0}`,
      );
      invalidateTeacher();
    },
    onError: (e: Error) => message.error(e.message || '迁移失败'),
  });

  const openCreate = () => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 1 });
    setModalOpen(true);
  };

  const openEdit = async (id: number) => {
    setEditingId(id);
    setModalOpen(true);
    try {
      const row = await trainApi.getTeacherBank(id);
      fillTeacherForm(form, row);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
      setModalOpen(false);
      setEditingId(null);
    }
  };

  const submit = async () => {
    const v = await form.validateFields();
    if (editingId != null) {
      updateMu.mutate({ id: editingId, payload: v });
    } else {
      createMu.mutate(v);
    }
  };

  const columns: ColumnsType<TrainTeacherBank> = [
    { title: 'ID', dataIndex: 'id', width: 72 },
    {
      title: '姓名',
      dataIndex: 'name',
      width: 100,
      render: (v: string, row) => (
        <Space size={4}>
          <span>{v}</span>
          {row.provisional ? <Tag color="orange">待补身份证</Tag> : null}
        </Space>
      ),
    },
    { title: '手机号', dataIndex: 'phone', width: 120, render: (v?: string) => v || '—' },
    {
      title: '身份证号',
      width: 170,
      render: (_, row) => displayIdCardNumber(row.idCardNumber),
    },
    { title: '学历', dataIndex: 'education', width: 100, render: (v?: string) => v || '—' },
    { title: '专业', dataIndex: 'major', ellipsis: true, render: (v?: string) => v || '—' },
    {
      title: '职务',
      dataIndex: 'professionalTitle',
      width: 110,
      ellipsis: true,
      render: (v?: string) => v || '—',
    },
    {
      title: '证照',
      dataIndex: 'fileReadyCount',
      width: 72,
      render: (v?: number) => `${v ?? 0}/7`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 72,
      render: (v?: number) => (v === 0 ? '停用' : '启用'),
    },
    {
      title: '操作',
      key: 'act',
      width: 220,
      fixed: 'right',
      render: (_, row) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setDetailId(row.id)}>
            详情
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => void openEdit(row.id)}>
            修改
          </Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteMu.mutate(row.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const teacherFormFields = (
    <Form form={form} layout="vertical">
      <Row gutter={16}>
        <Col span={8}>
          <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input maxLength={20} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="phone" label="手机号">
            <Input maxLength={11} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="idCardNumber" label="身份证号">
            <Input maxLength={18} placeholder="15 或 18 位" />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="gender" label="性别">
            <Select allowClear options={GENDER_OPTIONS} placeholder="请选择" />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="nation" label="民族">
            <Input maxLength={32} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="birthday" label="出生日期">
            <Input maxLength={32} placeholder="如 1990-01-01" />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="startDate" label="身份证有效期起">
            <Input maxLength={32} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="endDate" label="身份证有效期止">
            <Input maxLength={32} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item name="company" label="工作单位">
            <Input maxLength={255} />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item name="address" label="地址">
            <Input maxLength={500} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="education" label="学历">
            <Input maxLength={50} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="major" label="专业">
            <Input maxLength={50} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="professionalTitle" label="专业技术职务">
            <Input maxLength={255} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="qualificationLevel" label="职业资格等级">
            <Input maxLength={255} />
          </Form.Item>
        </Col>
        <Col span={16}>
          <Form.Item name="skillName" label="职业技能名称">
            <Input maxLength={512} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="teacherQualNo" label="教师资格证号">
            <Input maxLength={128} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="titleCertNo" label="职称证号">
            <Input maxLength={128} />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="skillCertNo" label="技能等级证号">
            <Input maxLength={255} />
          </Form.Item>
        </Col>
        <Col span={24}>
          <Form.Item name="certText" label="资格证书原文">
            <Input.TextArea rows={3} placeholder="持有何种职业资格证书等原文" />
          </Form.Item>
        </Col>
      </Row>
    </Form>
  );

  return (
    <div className="training-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4}>教务管理</Typography.Title>
          <Typography.Text type="secondary">
            师资库：Excel 导入、老系统 JSON 迁移、证照影像管理（train_teacher_bank）
          </Typography.Text>
        </div>
      </section>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="姓名/手机/身份证/学历/专业"
            prefix={<SearchOutlined />}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={() => {
              setPage(1);
              void refetch();
            }}
            style={{ width: 260 }}
            allowClear
          />
          <Button
            icon={<SearchOutlined />}
            onClick={() => {
              setPage(1);
              void refetch();
            }}
          >
            查询
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            添加
          </Button>
          <Button
            icon={<DownloadOutlined />}
            onClick={() =>
              void trainApi.downloadTeacherBankImportTemplate().catch((e: Error) =>
                message.error(e.message || '下载失败'),
              )
            }
          >
            下载模板
          </Button>
          <Upload
            accept=".xlsx,.xls"
            showUploadList={false}
            beforeUpload={(file) => {
              importMu.mutate(file);
              return false;
            }}
          >
            <Button icon={<UploadOutlined />} loading={importMu.isPending}>
              导入 Excel
            </Button>
          </Upload>
          <Upload
            accept=".json,application/json"
            showUploadList={false}
            beforeUpload={(file) => {
              Modal.confirm({
                title: '从老系统 JSON 迁移？',
                content: '将按身份证 upsert；图片会尝试从 URL 转存。耗时可能较长。',
                onOk: () => migrateMu.mutateAsync(file),
              });
              return false;
            }}
          >
            <Button loading={migrateMu.isPending}>迁移 legacy JSON</Button>
          </Upload>
          <Button icon={<ReloadOutlined />} onClick={() => void refetch()}>
            刷新
          </Button>
        </Space>
      </Card>

      <Card size="small">
        <Table
          rowKey="id"
          loading={isLoading}
          columns={columns}
          dataSource={data?.items ?? []}
          scroll={{ x: 1200 }}
          pagination={{
            current: page,
            pageSize,
            total: data?.total ?? 0,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            onChange: (p, size) => {
              if (typeof size === 'number' && size > 0 && size !== pageSize) {
                setPageSize(size);
                setPage(1);
              } else {
                setPage(p);
              }
            },
            showTotal: (t) => `共 ${t} 人`,
          }}
        />
      </Card>

      <Modal
        title={editingId != null ? '修改师资信息' : '添加师资'}
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          setEditingId(null);
        }}
        onOk={() => void submit()}
        confirmLoading={createMu.isPending || updateMu.isPending}
        destroyOnHidden
        width={920}
        styles={{ body: { maxHeight: '72vh', overflowY: 'auto' } }}
      >
        {teacherFormFields}
        {editingId != null ? (
          <Card
            size="small"
            title={`证照影像（${editingDetail?.fileReadyCount ?? 0}/7）`}
            style={{ marginTop: 8 }}
            loading={editingLoading && !editingDetail}
          >
            {editingDetail ? (
              <LicensePhotos
                teacher={editingDetail}
                teacherId={editingId}
                onPreview={setPreviewTarget}
                onUploaded={() => invalidateTeacher(editingId)}
              />
            ) : (
              <Typography.Text type="secondary">加载证照中…</Typography.Text>
            )}
          </Card>
        ) : (
          <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
            新建保存后，可在详情或修改页上传证照照片。
          </Typography.Paragraph>
        )}
      </Modal>

      <Drawer
        title={detail?.name ? `师资详情 · ${detail.name}` : '师资详情'}
        width={960}
        open={detailId != null}
        onClose={() => setDetailId(null)}
        destroyOnHidden
        extra={
          detailId != null ? (
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={() => {
                const id = detailId;
                setDetailId(null);
                void openEdit(id);
              }}
            >
              修改
            </Button>
          ) : null
        }
      >
        {detailLoading || !detail ? (
          <Typography.Text type="secondary">加载中…</Typography.Text>
        ) : (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Card size="small" title="基本信息">
              <Space wrap style={{ marginBottom: 12 }}>
                {detail.provisional ? <Tag color="orange">待补身份证</Tag> : null}
                <Tag>{detail.status === 0 ? '停用' : '启用'}</Tag>
                {detail.source ? (
                  <Tag color="blue">{SOURCE_LABEL[detail.source] || detail.source}</Tag>
                ) : null}
                {detail.legacyId != null ? <Tag>legacy_id={detail.legacyId}</Tag> : null}
                <Tag>证照 {detail.fileReadyCount ?? 0}/7</Tag>
              </Space>
              <Descriptions column={{ xs: 1, sm: 2, md: 3 }} size="small" bordered>
                <Descriptions.Item label="姓名">{detail.name || '—'}</Descriptions.Item>
                <Descriptions.Item label="手机">{detail.phone || '—'}</Descriptions.Item>
                <Descriptions.Item label="身份证号">
                  {displayIdCardNumber(detail.idCardNumber)}
                </Descriptions.Item>
                <Descriptions.Item label="性别">{genderLabel(detail.gender)}</Descriptions.Item>
                <Descriptions.Item label="民族">{detail.nation || '—'}</Descriptions.Item>
                <Descriptions.Item label="出生日期">{detail.birthday || '—'}</Descriptions.Item>
                <Descriptions.Item label="身份证有效期起">{detail.startDate || '—'}</Descriptions.Item>
                <Descriptions.Item label="身份证有效期止">{detail.endDate || '—'}</Descriptions.Item>
                <Descriptions.Item label="工作单位" span={2}>
                  {detail.company || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="地址" span={3}>
                  {detail.address || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="学历">{detail.education || '—'}</Descriptions.Item>
                <Descriptions.Item label="专业">{detail.major || '—'}</Descriptions.Item>
                <Descriptions.Item label="专业技术职务">
                  {detail.professionalTitle || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="职业资格等级">
                  {detail.qualificationLevel || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="职业技能名称" span={2}>
                  {detail.skillName || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="教师资格证号">
                  {detail.teacherQualNo || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="职称证号">{detail.titleCertNo || '—'}</Descriptions.Item>
                <Descriptions.Item label="技能等级证号">
                  {detail.skillCertNo || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="资格证书原文" span={3}>
                  <Typography.Paragraph
                    style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}
                    type={detail.certText ? undefined : 'secondary'}
                  >
                    {detail.certText || '—'}
                  </Typography.Paragraph>
                </Descriptions.Item>
              </Descriptions>
            </Card>

            <Card size="small" title={`证照影像（${detail.fileReadyCount ?? 0}/7）`}>
              <LicensePhotos
                teacher={detail}
                teacherId={detail.id}
                onPreview={setPreviewTarget}
                onUploaded={() => invalidateTeacher(detail.id)}
              />
            </Card>
          </Space>
        )}
      </Drawer>

      <FilePreviewModal
        open={previewTarget != null}
        target={previewTarget}
        onClose={() => setPreviewTarget(null)}
      />
    </div>
  );
}
