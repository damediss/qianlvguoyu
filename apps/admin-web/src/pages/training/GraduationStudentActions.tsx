import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import {
  Alert,
  Button,
  Col,
  Descriptions,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import { AuthImage } from '../../components/ApiAuthImage';
import {
  fetchUrlFileBlob,
  fileNameFromUrl,
  resolveFilePreviewKind,
  type FilePreviewKind,
} from '../../api/files';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';

type EditContext = {
  realname?: string;
  theoryScore?: unknown;
  skillScore?: unknown;
  examScore?: unknown;
  performanceScore?: unknown;
  graduateStatus?: number;
  qualified?: boolean;
  qualifyHint?: string;
  certificateNo?: string;
};

type MediaDrafts = {
  idCardHeader?: string;
  idCardFront?: string;
  idCardBack?: string;
  certificateFilePath?: string;
};

type MediaField = keyof MediaDrafts;

function resolveUploadedUrl(result: { id: number | string; url?: string | null }): string {
  return `/api/files/${result.id}/download?inline=true`;
}

function pickMediaUrl(...candidates: (string | undefined | null)[]): string {
  for (const raw of candidates) {
    if (!raw?.trim()) continue;
    return raw.trim();
  }
  return '';
}

function studentStatusTag(status?: number) {
  if (status === 1) return <Tag color="success">已结业</Tag>;
  if (status === 2) return <Tag color="error">不合格结业</Tag>;
  return <Tag>未结业</Tag>;
}

function scoreText(v: unknown) {
  if (v == null || v === '') return '—';
  return String(v);
}

const PHOTO_W = 90;
const PHOTO_H = 120;
/** 证书缩略图：与证件照同高，横向比例 */
const CERT_W = 170;
const CERT_H = 120;

function PhotoBlock({ title, url }: { title: string; url?: string }) {
  return (
    <div style={{ textAlign: 'center', width: PHOTO_W }}>
      <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 6, fontSize: 12 }}>
        {title}
      </Typography.Text>
      {url ? (
        <AuthImage
          url={url}
          alt={title}
          style={{
            width: PHOTO_W,
            height: PHOTO_H,
            objectFit: 'cover',
            borderRadius: 4,
            border: '1px solid #f0f0f0',
          }}
        />
      ) : (
        <div
          style={{
            width: PHOTO_W,
            height: PHOTO_H,
            margin: '0 auto',
            border: '1px dashed #d9d9d9',
            borderRadius: 4,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#999',
            fontSize: 12,
          }}
        >
          暂无
        </div>
      )}
    </div>
  );
}

/** 可上传且保留缩略图预览的证件/证书框 */
function EditableMediaBlock({
  title,
  url,
  wide,
  accept,
  uploading,
  onUpload,
  onPreview,
}: {
  title: string;
  url?: string;
  wide?: boolean;
  accept: string;
  uploading?: boolean;
  onUpload: (file: File) => Promise<void>;
  onPreview?: (url: string) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const w = wide ? CERT_W : PHOTO_W;
  const h = wide ? CERT_H : PHOTO_H;
  const [loading, setLoading] = useState(false);
  const [objectUrl, setObjectUrl] = useState<string>();
  const [previewKind, setPreviewKind] = useState<FilePreviewKind | 'blob-image'>('unsupported');

  useEffect(() => {
    if (!url) {
      setObjectUrl(undefined);
      setPreviewKind('unsupported');
      setLoading(false);
      return;
    }
    if (url.startsWith('blob:')) {
      setObjectUrl(url);
      setPreviewKind('blob-image');
      setLoading(false);
      return;
    }

    let cancelled = false;
    let localUrl: string | undefined;
    setLoading(true);
    setObjectUrl(undefined);
    setPreviewKind('unsupported');

    const fetchUrl = url.includes('?') ? `${url}&inline=true` : `${url}?inline=true`;
    fetchUrlFileBlob(fetchUrl, { silentError: true })
      .then((blob) => {
        if (cancelled) return;
        const kind = resolveFilePreviewKind(fileNameFromUrl(url), undefined, blob.type);
        if (kind === 'image' || kind === 'pdf') {
          localUrl = URL.createObjectURL(blob);
          setObjectUrl(localUrl);
          setPreviewKind(kind);
          return;
        }
        if (blob.type.startsWith('image/')) {
          localUrl = URL.createObjectURL(blob);
          setObjectUrl(localUrl);
          setPreviewKind('image');
          return;
        }
        if (blob.type === 'application/pdf') {
          localUrl = URL.createObjectURL(blob);
          setObjectUrl(localUrl);
          setPreviewKind('pdf');
          return;
        }
        setPreviewKind('unsupported');
      })
      .catch(() => {
        if (!cancelled) setPreviewKind('unsupported');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
      if (localUrl) URL.revokeObjectURL(localUrl);
    };
  }, [url]);

  const openPicker = () => {
    if (uploading) return;
    inputRef.current?.click();
  };

  const onFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    try {
      await onUpload(file);
    } catch {
      // 上层已提示
    }
  };

  const frameStyle = {
    width: w,
    height: h,
    margin: '0 auto' as const,
    border: url ? '1px solid #f0f0f0' : '1px dashed #bfbfbf',
    borderRadius: 6,
    background: '#fafafa',
    overflow: 'hidden' as const,
    position: 'relative' as const,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  };

  const openLargePreview = () => {
    if (url && onPreview) onPreview(url);
  };

  const renderThumb = () => {
    if (uploading || loading) {
      return <LoadingOutlined style={{ fontSize: 22, color: '#1a7fc1' }} />;
    }
    if (!url) {
      return (
        <div style={{ color: '#999', fontSize: 12, textAlign: 'center' }}>
          <PlusOutlined style={{ fontSize: 18, marginBottom: 4 }} />
          <div>上传</div>
        </div>
      );
    }
    if ((previewKind === 'image' || previewKind === 'blob-image') && objectUrl) {
      return (
        <img
          src={objectUrl}
          alt={title}
          style={{ width: '100%', height: '100%', objectFit: wide ? 'contain' : 'cover', display: 'block' }}
        />
      );
    }
    if (previewKind === 'pdf' && objectUrl) {
      return (
        <iframe
          title={`${title}预览`}
          src={`${objectUrl}#toolbar=0&navpanes=0&scrollbar=0&view=Fit`}
          style={{
            width: '100%',
            height: '100%',
            display: 'block',
            pointerEvents: 'none',
            background: '#fff',
            border: 'none',
          }}
        />
      );
    }
    return (
      <AuthImage
        url={url}
        alt={title}
        style={{ width: '100%', height: '100%', objectFit: wide ? 'contain' : 'cover', display: 'block' }}
      />
    );
  };

  return (
    <div style={{ textAlign: 'center', width: w }}>
      <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 6, fontSize: 12 }}>
        {title}
      </Typography.Text>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        style={{ display: 'none' }}
        onChange={(e) => void onFileChange(e)}
      />
      <div
        role="button"
        tabIndex={0}
        title={url ? (onPreview ? '点击预览' : '点击更换') : '点击上传'}
        onClick={() => {
          if (!url) {
            openPicker();
            return;
          }
          if (onPreview) {
            openLargePreview();
            return;
          }
          openPicker();
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (!url) openPicker();
            else if (onPreview) openLargePreview();
            else openPicker();
          }
        }}
        style={{
          ...frameStyle,
          cursor: uploading ? 'wait' : 'pointer',
        }}
      >
        {renderThumb()}
      </div>
      <div style={{ marginTop: 4, minHeight: 22 }}>
        {url ? (
          <Space size={8}>
            {onPreview ? (
              <Button
                type="link"
                size="small"
                style={{ padding: 0, height: 'auto', fontSize: 12 }}
                onClick={(e) => {
                  e.stopPropagation();
                  openLargePreview();
                }}
              >
                预览
              </Button>
            ) : null}
            <Button
              type="link"
              size="small"
              style={{ padding: 0, height: 'auto', fontSize: 12 }}
              disabled={uploading}
              onClick={(e) => {
                e.stopPropagation();
                openPicker();
              }}
            >
              更换
            </Button>
          </Space>
        ) : null}
      </div>
    </div>
  );
}

function CertificatePreviewBlock({
  url,
  title = '证书',
  onOpen,
}: {
  url?: string;
  title?: string;
  onOpen?: (fileUrl: string) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [objectUrl, setObjectUrl] = useState<string>();
  const [previewKind, setPreviewKind] = useState<FilePreviewKind>('unsupported');

  useEffect(() => {
    if (!url) {
      setObjectUrl(undefined);
      setPreviewKind('unsupported');
      setLoading(false);
      return;
    }

    let cancelled = false;
    let localUrl: string | undefined;
    setLoading(true);
    setObjectUrl(undefined);
    setPreviewKind('unsupported');

    const fetchUrl = url.includes('?') ? `${url}&inline=true` : `${url}?inline=true`;
    fetchUrlFileBlob(fetchUrl, { silentError: true })
      .then((blob) => {
        if (cancelled) return;
        const kind = resolveFilePreviewKind(fileNameFromUrl(url), undefined, blob.type);
        if (kind === 'image' || kind === 'pdf') {
          localUrl = URL.createObjectURL(blob);
          setObjectUrl(localUrl);
          setPreviewKind(kind);
          return;
        }
        setPreviewKind('unsupported');
      })
      .catch(() => {
        if (!cancelled) setPreviewKind('unsupported');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
      if (localUrl) URL.revokeObjectURL(localUrl);
    };
  }, [url]);

  if (!url) return null;

  const frameStyle = {
    width: CERT_W,
    height: CERT_H,
    border: '1px solid #f0f0f0',
    borderRadius: 4,
    background: '#fafafa',
    overflow: 'hidden' as const,
    cursor: 'pointer' as const,
  };

  const openPreview = () => onOpen?.(url);

  return (
    <div style={{ textAlign: 'center', width: CERT_W }}>
      <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 6, fontSize: 12 }}>
        {title}
      </Typography.Text>
      {loading ? (
        <div style={{ ...frameStyle, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'default' }}>
          <Spin size="small" />
        </div>
      ) : previewKind === 'image' && objectUrl ? (
        <div role="button" tabIndex={0} title="点击放大预览" onClick={openPreview} onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') openPreview();
        }}>
          <AuthImage
            url={objectUrl}
            alt={title}
            style={{ ...frameStyle, objectFit: 'contain', display: 'block' }}
          />
        </div>
      ) : previewKind === 'pdf' && objectUrl ? (
        <div
          role="button"
          tabIndex={0}
          title="点击放大预览"
          onClick={openPreview}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') openPreview();
          }}
          style={{ display: 'block' }}
        >
          <iframe
            title="证书预览"
            src={`${objectUrl}#toolbar=0&navpanes=0&scrollbar=0&view=Fit`}
            style={{ ...frameStyle, display: 'block', pointerEvents: 'none', background: '#fff' }}
          />
        </div>
      ) : (
        <div
          style={{
            ...frameStyle,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderStyle: 'dashed',
            color: '#999',
            fontSize: 12,
            cursor: 'default',
          }}
        >
          暂无
        </div>
      )}
    </div>
  );
}

export function useGraduationStudentActions(classId: number, onChanged?: () => void) {
  const queryClient = useQueryClient();
  const [detailOpen, setDetailOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [peopleId, setPeopleId] = useState<number | null>(null);
  const [editContext, setEditContext] = useState<EditContext | null>(null);
  const [mediaDrafts, setMediaDrafts] = useState<MediaDrafts>({});
  const [uploadingField, setUploadingField] = useState<MediaField | null>(null);
  const [certPreview, setCertPreview] = useState<FilePreviewTarget | null>(null);
  const [editForm] = Form.useForm();

  const openCertFile = (fileUrl: string, fileName?: string) => {
    setCertPreview({
      fileUrl,
      fileName: fileName || fileNameFromUrl(fileUrl) || '证书.pdf',
    });
  };

  const { data: detail, isLoading: detailLoading } = useQuery({
    queryKey: ['graduation-student-detail', classId, peopleId],
    queryFn: () => trainApi.getGraduationStudentDetail(classId, peopleId!),
    enabled: classId > 0 && !!peopleId && (detailOpen || editOpen),
  });

  const saveMu = useMutation({
    mutationFn: (payload: {
      realname?: string;
      phone?: string;
      idCard?: string;
      ticketNo?: string;
      idCardHeader?: string | null;
      idCardFront?: string | null;
      idCardBack?: string | null;
      certificateFilePath?: string | null;
    }) => trainApi.updateGraduationStudent(classId, peopleId!, payload),
    onSuccess: () => {
      message.success('学员信息已保存');
      setEditOpen(false);
      setEditContext(null);
      setMediaDrafts({});
      void queryClient.invalidateQueries({ queryKey: ['graduation-students', classId] });
      void queryClient.invalidateQueries({ queryKey: ['graduation-student-detail', classId, peopleId] });
      onChanged?.();
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const closeEdit = () => {
    setEditOpen(false);
    setEditContext(null);
    setMediaDrafts({});
    setUploadingField(null);
    if (!detailOpen) setPeopleId(null);
  };

  const openDetail = (id: number) => {
    setPeopleId(id);
    setDetailOpen(true);
  };

  const openEdit = (row: Record<string, unknown>) => {
    const id = Number(row.id);
    if (!Number.isFinite(id) || id <= 0) return;
    setPeopleId(id);
    setMediaDrafts({});
    setEditContext({
      realname: row.realname != null ? String(row.realname) : undefined,
      theoryScore: row.theoryScore,
      skillScore: row.skillScore ?? row.realScore,
      examScore: row.examScore,
      performanceScore: row.performanceScore,
      graduateStatus: typeof row.graduateStatus === 'number' ? row.graduateStatus : undefined,
      qualified: typeof row.qualified === 'boolean' ? row.qualified : undefined,
      qualifyHint: row.qualifyHint != null ? String(row.qualifyHint) : undefined,
      certificateNo: row.certificateNo != null ? String(row.certificateNo) : undefined,
    });
    editForm.setFieldsValue({
      realname: row.realname ?? '',
      phone: row.phone ?? '',
      idCard: row.idCard ?? '',
      ticketNo: row.ticketNo ?? '',
    });
    setEditOpen(true);
  };

  const handleMediaUpload = async (field: MediaField, file: File, kind: 'image' | 'file') => {
    if (kind === 'image') {
      const lower = file.name.toLowerCase();
      const okExt = /\.(jpe?g|jfif|png|webp|gif|bmp|heic|heif)$/i.test(lower);
      const okType = !file.type || file.type.startsWith('image/');
      if (!okExt && !okType) {
        message.error('请选择 JPG/PNG/WEBP 等图片文件');
        return;
      }
    } else {
      const lower = file.name.toLowerCase();
      const okExt = /\.(pdf|jpe?g|png|webp)$/i.test(lower);
      if (!okExt) {
        message.error('证书请上传 PDF 或图片');
        return;
      }
    }
    setUploadingField(field);
    const localPreview = URL.createObjectURL(file);
    setMediaDrafts((prev) => {
      const old = prev[field];
      if (old?.startsWith('blob:')) URL.revokeObjectURL(old);
      return { ...prev, [field]: localPreview };
    });
    try {
      const result = await uploadFile(file, 'public');
      const url = resolveUploadedUrl(result);
      setMediaDrafts((prev) => {
        const old = prev[field];
        if (old?.startsWith('blob:')) URL.revokeObjectURL(old);
        return { ...prev, [field]: url };
      });
      message.success('已上传，请点击保存生效');
    } catch (err) {
      setMediaDrafts((prev) => {
        const old = prev[field];
        if (old?.startsWith('blob:')) URL.revokeObjectURL(old);
        const next = { ...prev };
        delete next[field];
        return next;
      });
      message.error(err instanceof Error && err.message ? err.message : '上传失败');
    } finally {
      setUploadingField(null);
    }
  };

  const submitEdit = async () => {
    try {
      const values = await editForm.validateFields();
      saveMu.mutate({
        ...values,
        idCardHeader: mediaDrafts.idCardHeader,
        idCardFront: mediaDrafts.idCardFront,
        idCardBack: mediaDrafts.idCardBack,
        certificateFilePath: mediaDrafts.certificateFilePath,
      });
    } catch {
      // 表单校验
    }
  };

  const editDisplayName = detail?.realname || editContext?.realname || '';

  const summaryQualified =
    detail?.qualified ?? editContext?.qualified;
  const summaryQualifyHint =
    detail?.qualifyHint || editContext?.qualifyHint;
  const summaryStatus =
    detail?.graduateStatus ?? editContext?.graduateStatus;
  const summaryCert =
    detail?.certificateNo || editContext?.certificateNo;

  const actionColumn = {
    title: '操作',
    key: 'studentAct',
    width: 120,
    fixed: 'right' as const,
    render: (_: unknown, row: Record<string, unknown>) => (
      <Space size={4}>
        <Button type="link" size="small" onClick={() => openDetail(Number(row.id))}>
          详情
        </Button>
        <Button type="link" size="small" onClick={() => openEdit(row)}>
          修改
        </Button>
      </Space>
    ),
  };

  const modals = (
    <>
      <Modal
        title={detail?.realname ? `学员详情 · ${detail.realname}` : '学员详情'}
        open={detailOpen}
        onCancel={() => {
          setDetailOpen(false);
          setPeopleId(null);
        }}
        footer={[
          <Button
            key="edit"
            type="primary"
            onClick={() => {
              if (!detail) return;
              setMediaDrafts({});
              setEditContext({
                realname: detail.realname,
                theoryScore: detail.theoryScore,
                skillScore: detail.skillScore,
                examScore: detail.examScore,
                performanceScore: detail.performanceScore,
                graduateStatus: detail.graduateStatus,
                qualified: detail.qualified,
                qualifyHint: detail.qualifyHint,
                certificateNo: detail.certificateNo || undefined,
              });
              editForm.setFieldsValue({
                realname: detail.realname ?? '',
                phone: detail.phone ?? '',
                idCard: detail.idCard ?? '',
                ticketNo: detail.ticketNo ?? '',
              });
              setDetailOpen(false);
              setEditOpen(true);
            }}
          >
            修改
          </Button>,
          <Button
            key="close"
            onClick={() => {
              setDetailOpen(false);
              setPeopleId(null);
            }}
          >
            关闭
          </Button>,
        ]}
        width={920}
        destroyOnHidden
      >
        {detailLoading ? (
          <div style={{ padding: 48, textAlign: 'center' }}>
            <Spin />
          </div>
        ) : detail ? (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="姓名">{detail.realname || '—'}</Descriptions.Item>
              <Descriptions.Item label="手机">{detail.phone || '—'}</Descriptions.Item>
              <Descriptions.Item label="身份证">{detail.idCard || '—'}</Descriptions.Item>
              <Descriptions.Item label="准考证号">{detail.ticketNo || '—'}</Descriptions.Item>
              <Descriptions.Item label="性别">{detail.gender || '—'}</Descriptions.Item>
              <Descriptions.Item label="民族">{detail.nation || '—'}</Descriptions.Item>
              <Descriptions.Item label="学历">{detail.education || '—'}</Descriptions.Item>
              <Descriptions.Item label="政治面貌">{detail.political || '—'}</Descriptions.Item>
              <Descriptions.Item label="地址" span={2}>
                {detail.address || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="理论">{scoreText(detail.theoryScore)}</Descriptions.Item>
              <Descriptions.Item label="技能">{scoreText(detail.skillScore)}</Descriptions.Item>
              <Descriptions.Item label="综合">{scoreText(detail.examScore)}</Descriptions.Item>
              <Descriptions.Item label="工作业绩">{scoreText(detail.performanceScore)}</Descriptions.Item>
              <Descriptions.Item label="达标">
                {detail.qualified ? <Tag color="success">达标</Tag> : <Tag color="warning">{detail.qualifyHint || '未达标'}</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="结业状态">{studentStatusTag(detail.graduateStatus)}</Descriptions.Item>
              <Descriptions.Item label="证书编号" span={2}>
                {detail.certificateNo ? (
                  detail.certificateFilePath ? (
                    <Button
                      type="link"
                      size="small"
                      style={{ padding: 0, height: 'auto' }}
                      onClick={() => openCertFile(detail.certificateFilePath!, `${detail.certificateNo}.pdf`)}
                    >
                      {detail.certificateNo}
                    </Button>
                  ) : (
                    detail.certificateNo
                  )
                ) : (
                  '—'
                )}
              </Descriptions.Item>
              <Descriptions.Item label="职业">{detail.certificateOccupation || '—'}</Descriptions.Item>
              <Descriptions.Item label="工种">{detail.certificateWorkType || '—'}</Descriptions.Item>
              <Descriptions.Item label="等级">{detail.certificateLevel || '—'}</Descriptions.Item>
              <Descriptions.Item label="核发日期">{detail.certificateIssueDate || '—'}</Descriptions.Item>
              <Descriptions.Item label="发证机构" span={2}>
                {detail.certificateIssuingAuthority || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="评价机构" span={2}>
                {detail.certificateEvaluationOrg || '—'}
              </Descriptions.Item>
            </Descriptions>

            <div>
              <Typography.Text strong style={{ display: 'block', marginBottom: 12 }}>
                证件与证书照片
              </Typography.Text>
              <Space wrap size="middle" align="start">
                <PhotoBlock title="证件照" url={detail.idCardHeader} />
                <PhotoBlock title="身份证正面" url={detail.idCardFront} />
                <PhotoBlock title="身份证反面" url={detail.idCardBack} />
                <CertificatePreviewBlock
                  url={detail.certificateFilePath || undefined}
                  title="证书"
                  onOpen={(fileUrl) => openCertFile(fileUrl, detail.certificateNo ? `${detail.certificateNo}.pdf` : undefined)}
                />
              </Space>
            </div>
          </Space>
        ) : (
          <Typography.Text type="secondary">暂无学员详情</Typography.Text>
        )}
      </Modal>

      <FilePreviewModal
        open={Boolean(certPreview)}
        target={certPreview}
        onClose={() => setCertPreview(null)}
      />

      <Modal
        title={
          <div>
            <div style={{ fontSize: 16, fontWeight: 600, color: '#12263a' }}>
              {editDisplayName ? `修改学员 · ${editDisplayName}` : '修改学员'}
            </div>
            <Typography.Text type="secondary" style={{ fontSize: 12, fontWeight: 400 }}>
              结业名册基础信息校正
            </Typography.Text>
          </div>
        }
        open={editOpen}
        onCancel={closeEdit}
        onOk={() => void submitEdit()}
        okText="保存"
        cancelText="取消"
        confirmLoading={saveMu.isPending || uploadingField != null}
        destroyOnHidden
        width={720}
        styles={{ body: { paddingTop: 12 } }}
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="可修改姓名、手机、身份证、准考证号，以及证件照/身份证正反面/证书文件；成绩与结业状态请走结业流程。"
        />

        <div
          style={{
            marginBottom: 16,
            padding: '12px 14px',
            borderRadius: 8,
            background: 'linear-gradient(135deg, #f4f8fb 0%, #eef5fa 100%)',
            border: '1px solid #e2ebf2',
          }}
        >
          <Space wrap size={[8, 8]} style={{ marginBottom: 10 }}>
            {studentStatusTag(summaryStatus)}
            {summaryQualified === true ? (
              <Tag color="success">达标</Tag>
            ) : summaryQualified === false ? (
              <Tag color="warning">{summaryQualifyHint || '未达标'}</Tag>
            ) : null}
            {summaryCert ? <Tag color="blue">证书 {summaryCert}</Tag> : <Tag>暂无证书编号</Tag>}
          </Space>
          <Row gutter={[12, 8]}>
            <Col span={6}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>理论</Typography.Text>
              <div style={{ fontWeight: 600, color: '#1f2937' }}>
                {scoreText(detail?.theoryScore ?? editContext?.theoryScore)}
              </div>
            </Col>
            <Col span={6}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>技能</Typography.Text>
              <div style={{ fontWeight: 600, color: '#1f2937' }}>
                {scoreText(detail?.skillScore ?? editContext?.skillScore)}
              </div>
            </Col>
            <Col span={6}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>综合</Typography.Text>
              <div style={{ fontWeight: 600, color: '#1f2937' }}>
                {scoreText(detail?.examScore ?? editContext?.examScore)}
              </div>
            </Col>
            <Col span={6}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>工作业绩</Typography.Text>
              <div style={{ fontWeight: 600, color: '#1f2937' }}>
                {scoreText(detail?.performanceScore ?? editContext?.performanceScore)}
              </div>
            </Col>
          </Row>
        </div>

        <Form
          form={editForm}
          layout="vertical"
          requiredMark="optional"
          onFinish={(values) =>
            saveMu.mutate({
              ...values,
              idCardHeader: mediaDrafts.idCardHeader,
              idCardFront: mediaDrafts.idCardFront,
              idCardBack: mediaDrafts.idCardBack,
              certificateFilePath: mediaDrafts.certificateFilePath,
            })
          }
        >
          <Typography.Text strong style={{ display: 'block', marginBottom: 12 }}>
            可修改字段
          </Typography.Text>
          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="realname"
                label="姓名"
                rules={[{ required: true, message: '请输入姓名' }]}
              >
                <Input maxLength={32} placeholder="学员真实姓名" allowClear />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item name="phone" label="手机">
                <Input maxLength={20} placeholder="联系手机号" allowClear />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item name="idCard" label="身份证号">
                <Input maxLength={32} placeholder="15 或 18 位证件号" allowClear />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item name="ticketNo" label="准考证号">
                <Input maxLength={64} placeholder="考试准考证号" allowClear />
              </Form.Item>
            </Col>
          </Row>
        </Form>

        <div style={{ marginTop: 4, paddingTop: 12, borderTop: '1px solid #f0f0f0' }}>
          <Typography.Text strong style={{ display: 'block', marginBottom: 4 }}>
            证件与证书
          </Typography.Text>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12, fontSize: 12 }}>
            方框内展示缩略图；点击可预览。下方「更换」可重新上传，保存后生效。上传证件照前请确保已填写身份证号。
          </Typography.Text>
          <Space wrap size="middle" align="start">
            <EditableMediaBlock
              title="证件照"
              url={pickMediaUrl(mediaDrafts.idCardHeader, detail?.idCardHeader)}
              accept="image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.jfif,.png,.webp,.gif,.bmp"
              uploading={uploadingField === 'idCardHeader'}
              onUpload={(file) => handleMediaUpload('idCardHeader', file, 'image')}
              onPreview={(fileUrl) => openCertFile(fileUrl, '证件照')}
            />
            <EditableMediaBlock
              title="身份证正面"
              url={pickMediaUrl(mediaDrafts.idCardFront, detail?.idCardFront)}
              accept="image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.jfif,.png,.webp,.gif,.bmp"
              uploading={uploadingField === 'idCardFront'}
              onUpload={(file) => handleMediaUpload('idCardFront', file, 'image')}
              onPreview={(fileUrl) => openCertFile(fileUrl, '身份证正面')}
            />
            <EditableMediaBlock
              title="身份证反面"
              url={pickMediaUrl(mediaDrafts.idCardBack, detail?.idCardBack)}
              accept="image/jpeg,image/png,image/webp,image/gif,image/bmp,.jpg,.jpeg,.jfif,.png,.webp,.gif,.bmp"
              uploading={uploadingField === 'idCardBack'}
              onUpload={(file) => handleMediaUpload('idCardBack', file, 'image')}
              onPreview={(fileUrl) => openCertFile(fileUrl, '身份证反面')}
            />
            <EditableMediaBlock
              title="证书"
              wide
              url={pickMediaUrl(mediaDrafts.certificateFilePath, detail?.certificateFilePath)}
              accept=".pdf,application/pdf,image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
              uploading={uploadingField === 'certificateFilePath'}
              onUpload={(file) => handleMediaUpload('certificateFilePath', file, 'file')}
              onPreview={(fileUrl) =>
                openCertFile(fileUrl, detail?.certificateNo ? `${detail.certificateNo}.pdf` : undefined)
              }
            />
          </Space>
        </div>
      </Modal>
    </>
  );

  return { actionColumn, modals };
}
