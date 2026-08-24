import { useEffect, useState } from 'react';
import {
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { AuthImage } from '../../components/ApiAuthImage';
import {
  fetchUrlFileBlob,
  fileNameFromUrl,
  resolveFilePreviewKind,
  type FilePreviewKind,
} from '../../api/files';
import { FilePreviewModal, type FilePreviewTarget } from '../../components/FilePreviewModal';

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
    mutationFn: (payload: { realname?: string; phone?: string; idCard?: string; ticketNo?: string }) =>
      trainApi.updateGraduationStudent(classId, peopleId!, payload),
    onSuccess: () => {
      message.success('学员信息已保存');
      setEditOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['graduation-students', classId] });
      void queryClient.invalidateQueries({ queryKey: ['graduation-student-detail', classId, peopleId] });
      onChanged?.();
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const openDetail = (id: number) => {
    setPeopleId(id);
    setDetailOpen(true);
  };

  const openEdit = (row: Record<string, unknown>) => {
    const id = Number(row.id);
    if (!Number.isFinite(id) || id <= 0) return;
    setPeopleId(id);
    editForm.setFieldsValue({
      realname: row.realname ?? '',
      phone: row.phone ?? '',
      idCard: row.idCard ?? '',
      ticketNo: row.ticketNo ?? '',
    });
    setEditOpen(true);
  };

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
        title={detail?.realname ? `修改学员 · ${detail.realname}` : '修改学员'}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={() => editForm.submit()}
        confirmLoading={saveMu.isPending}
        destroyOnHidden
      >
        <Form
          form={editForm}
          layout="vertical"
          onFinish={(values) => saveMu.mutate(values)}
        >
          <Form.Item name="realname" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input maxLength={32} />
          </Form.Item>
          <Form.Item name="phone" label="手机">
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="idCard" label="身份证号">
            <Input maxLength={32} />
          </Form.Item>
          <Form.Item name="ticketNo" label="准考证号">
            <Input maxLength={64} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );

  return { actionColumn, modals };
}
