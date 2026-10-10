/**
 * 跟班 · 课堂影像留痕（手动添加）+ 培训照片生成
 * 不再按课表齐套；管理员直接添加图片并填写图注。
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AutoComplete,
  Button,
  Card,
  DatePicker,
  Empty,
  Form,
  Image,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  DeleteOutlined,
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { ApiAuthImage } from '../../../components/ApiAuthImage';
import { downloadUrlFile } from '../../../api/files';
import * as trainApi from '../../../api/training';
import type { FollowEvidence, FollowPhotoPackMeta } from '../../../api/training';

const CAPTION_OPTIONS = [
  { value: '开班' },
  { value: '公共课' },
  { value: '实操课' },
];

type Props = {
  classId: number;
};

export function FollowEvidenceTab({ classId }: Props) {
  const qc = useQueryClient();
  const [addOpen, setAddOpen] = useState(false);
  const [genOpen, setGenOpen] = useState(false);
  const [caption, setCaption] = useState('');
  const [bizDate, setBizDate] = useState<Dayjs | null>(dayjs());
  const [dayPeriod, setDayPeriod] = useState<'上午' | '下午' | '全天'>('上午');
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [genForm] = Form.useForm<FollowPhotoPackMeta>();

  const clearPendingImage = () => {
    setPreviewUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return null;
    });
    setPendingFile(null);
  };

  const resetAddForm = () => {
    setCaption('');
    setBizDate(dayjs());
    setDayPeriod('上午');
    clearPendingImage();
  };

  const closeAddModal = () => {
    setAddOpen(false);
    resetAddForm();
  };

  const listQ = useQuery({
    queryKey: ['follow-evidences-manual', classId],
    queryFn: () =>
      trainApi.listFollowEvidences(classId, {
        mediaType: 'photo',
      }),
  });

  const latestPackQ = useQuery({
    queryKey: ['follow-photo-pack-latest', classId],
    queryFn: () => trainApi.getLatestFollowPhotoPack(classId),
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['follow-evidences-manual', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-photo-pack-latest', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-evidence-coverage', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-quality-metrics', classId] });
  };

  const photos = useMemo(() => {
    const list = [...(listQ.data ?? [])].filter((r) => r.mediaType !== 'video');
    list.sort((a, b) => {
      const da = String(a.bizDate ?? '');
      const db = String(b.bizDate ?? '');
      if (da !== db) return da.localeCompare(db);
      const sa = Number(a.sortNo ?? 0);
      const sb = Number(b.sortNo ?? 0);
      if (sa !== sb) return sa - sb;
      return String(a.id).localeCompare(String(b.id), undefined, { numeric: true });
    });
    return list;
  }, [listQ.data]);

  const addMu = useMutation({
    mutationFn: async (payload: {
      file: File;
      caption: string;
      bizDate: Dayjs;
      dayPeriod: '上午' | '下午' | '全天';
    }) => {
      return trainApi.uploadFollowEvidence(classId, payload.file, {
        bizDate: payload.bizDate.format('YYYY-MM-DD'),
        caption: payload.caption.trim(),
        teachFormOverride: payload.dayPeriod,
        mediaType: 'photo',
      });
    },
    onSuccess: () => {
      message.success('已添加');
      closeAddModal();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '添加失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number | string) => trainApi.deleteFollowEvidence(id),
    onSuccess: () => {
      message.success('已删除');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const pickImageFile = (file: File) => {
    if (!file.type.startsWith('image/')) {
      message.warning('请选择图片文件');
      return;
    }
    setPreviewUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return URL.createObjectURL(file);
    });
    setPendingFile(file);
  };

  const extractClipboardImage = (clipboardData: DataTransfer | null): File | null => {
    if (!clipboardData) return null;
    const items = clipboardData.items;
    for (let i = 0; i < items.length; i += 1) {
      const item = items[i];
      if (!item.type.startsWith('image/')) continue;
      const blob = item.getAsFile();
      if (!blob) continue;
      const ext = item.type.includes('png')
        ? 'png'
        : item.type.includes('webp')
          ? 'webp'
          : item.type.includes('gif')
            ? 'gif'
            : 'jpg';
      return new File([blob], blob.name || `paste-${Date.now()}.${ext}`, {
        type: blob.type || item.type || 'image/png',
      });
    }
    return null;
  };

  const pickImageRef = useRef(pickImageFile);
  pickImageRef.current = pickImageFile;

  useEffect(() => {
    if (!addOpen) return;
    const onPaste = (e: ClipboardEvent) => {
      const file = extractClipboardImage(e.clipboardData);
      if (!file) return;
      e.preventDefault();
      pickImageRef.current(file);
    };
    window.addEventListener('paste', onPaste);
    return () => window.removeEventListener('paste', onPaste);
  }, [addOpen]);

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const confirmAdd = () => {
    if (!pendingFile) {
      message.warning('请先选择或粘贴图片');
      return;
    }
    if (!bizDate) {
      message.warning('请选择日期');
      return;
    }
    if (!caption.trim()) {
      message.warning('请填写图注说明');
      return;
    }
    addMu.mutate({
      file: pendingFile,
      caption: caption.trim(),
      bizDate,
      dayPeriod,
    });
  };

  const genMu = useMutation({
    mutationFn: (payload: FollowPhotoPackMeta) => trainApi.generateFollowPhotoPack(classId, payload),
    onSuccess: () => {
      message.success('培训照片已生成，可点击「下载照片」获取文件');
      setGenOpen(false);
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '生成失败'),
  });

  const downloadMu = useMutation({
    mutationFn: async () => {
      const res = await trainApi.getLatestFollowPhotoPack(classId);
      if (!res.exists) {
        throw new Error('尚未生成培训照片，请先点击「生成培训照片」');
      }
      const path = res.filePath || res.url;
      if (!path) {
        throw new Error('培训照片文件路径缺失');
      }
      await downloadUrlFile(path, `${res.title || '培训照片'}.docx`);
      return res;
    },
    onSuccess: () => message.success('开始下载培训照片'),
    onError: (e: Error) => message.error(e.message || '下载失败'),
  });

  const openGenerateModal = async () => {
    setGenOpen(true);
    try {
      const defaults = await trainApi.getFollowPhotoPackDefaults(classId);
      genForm.setFieldsValue({
        title: defaults.title || '',
        workType: defaults.workType || '',
        level: defaults.level || '',
        trainTime: defaults.trainTime || '',
        trainUnit: defaults.trainUnit || '',
        trainAddress: defaults.trainAddress || '',
      });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载默认信息失败');
    }
  };

  const submitGenerate = async () => {
    const values = await genForm.validateFields();
    if (!String(values.title || '').trim()) {
      message.warning('请填写标题');
      return;
    }
    genMu.mutate({
      title: String(values.title || '').trim(),
      workType: String(values.workType || '').trim(),
      level: String(values.level || '').trim(),
      trainTime: String(values.trainTime || '').trim(),
      trainUnit: String(values.trainUnit || '').trim(),
      trainAddress: String(values.trainAddress || '').trim(),
    });
  };

  const periodLabel = (row: FollowEvidence) => {
    const t = String(row.teachFormOverride || row.teachForm || '').trim();
    if (t === '上午' || t === '下午' || t === '全天') return t;
    return '—';
  };

  const columns: ColumnsType<FollowEvidence> = [
    {
      title: '序号',
      width: 70,
      render: (_, __, index) => `图${index + 1}`,
    },
    {
      title: '预览',
      width: 100,
      render: (_, row) =>
        row.id ? (
          <Image.PreviewGroup>
            <ApiAuthImage
              url={`/training/follow-evidences/${row.id}/preview`}
              width={72}
              height={54}
              style={{ objectFit: 'cover', borderRadius: 4 }}
            />
          </Image.PreviewGroup>
        ) : (
          '—'
        ),
    },
    {
      title: '图注说明',
      dataIndex: 'caption',
      ellipsis: true,
      render: (v?: string) => v || '—',
    },
    {
      title: '日期',
      width: 120,
      dataIndex: 'bizDate',
      render: (v?: string) => v || '—',
    },
    {
      title: '时段',
      width: 80,
      render: (_, row) => periodLabel(row),
    },
    {
      title: '状态',
      width: 90,
      render: (_, row) =>
        row.reviewStatus === 'approved' ? (
          <Tag color="success">已通过</Tag>
        ) : (
          <Tag>{row.reviewStatus || '—'}</Tag>
        ),
    },
    {
      title: '操作',
      width: 90,
      fixed: 'right',
      render: (_, row) => (
        <Button
          type="link"
          size="small"
          danger
          icon={<DeleteOutlined />}
          onClick={() => {
            Modal.confirm({
              title: '删除该照片？',
              onOk: () => deleteMu.mutateAsync(row.id),
            });
          }}
        >
          删除
        </Button>
      ),
    },
  ];

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Card
        size="small"
        title={`已添加照片（${photos.length}）`}
        extra={
          <Space wrap>
            <Button icon={<ReloadOutlined />} onClick={() => invalidate()}>
              刷新
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                resetAddForm();
                setAddOpen(true);
              }}
            >
              添加照片
            </Button>
            <Button
              type="primary"
              ghost
              icon={<ThunderboltOutlined />}
              disabled={!photos.length}
              loading={genMu.isPending}
              onClick={() => void openGenerateModal()}
            >
              生成培训照片
            </Button>
            <Button
              icon={<DownloadOutlined />}
              disabled={!latestPackQ.data?.exists}
              loading={downloadMu.isPending || latestPackQ.isFetching}
              onClick={() => downloadMu.mutate()}
            >
              下载照片
            </Button>
          </Space>
        }
      >
        <Table
          size="small"
          rowKey={(r) => String(r.id)}
          loading={listQ.isFetching}
          columns={columns}
          dataSource={photos}
          pagination={{ pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 张` }}
          locale={{
            emptyText: <Empty description="暂无照片，请点击「添加照片」" />,
          }}
          scroll={{ x: 720 }}
        />
      </Card>

      <Modal
        title="添加照片"
        open={addOpen}
        onCancel={closeAddModal}
        destroyOnHidden
        okText="确认添加"
        confirmLoading={addMu.isPending}
        onOk={confirmAdd}
        okButtonProps={{ disabled: !pendingFile }}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div>
            <Typography.Text type="secondary">图注说明</Typography.Text>
            <AutoComplete
              style={{ width: '100%', marginTop: 8 }}
              options={CAPTION_OPTIONS}
              placeholder="选择或输入，如：开班"
              value={caption}
              onChange={(v) => setCaption(String(v ?? '').slice(0, 100))}
              filterOption={(input, option) =>
                String(option?.value ?? '')
                  .toLowerCase()
                  .includes(input.trim().toLowerCase())
              }
              allowClear
            />
          </div>
          <div>
            <Typography.Text type="secondary">日期</Typography.Text>
            <div style={{ marginTop: 8 }}>
              <DatePicker
                style={{ width: '100%' }}
                value={bizDate}
                onChange={(d) => setBizDate(d)}
                allowClear={false}
              />
            </div>
          </div>
          <div>
            <Typography.Text type="secondary">时段</Typography.Text>
            <Select
              style={{ width: '100%', marginTop: 8 }}
              value={dayPeriod}
              onChange={setDayPeriod}
              options={[
                { value: '上午', label: '上午' },
                { value: '下午', label: '下午' },
                { value: '全天', label: '全天' },
              ]}
            />
          </div>
          <div
            style={{
              border: '1px dashed #d9d9d9',
              borderRadius: 8,
              padding: 12,
              textAlign: 'center',
              background: '#fafafa',
            }}
          >
            {previewUrl ? (
              <div>
                <Image
                  src={previewUrl}
                  alt="预览"
                  style={{ maxHeight: 220, objectFit: 'contain' }}
                />
                <div style={{ marginTop: 12 }}>
                  <Space>
                    <Upload
                      accept="image/*"
                      showUploadList={false}
                      beforeUpload={(file) => {
                        pickImageFile(file);
                        return false;
                      }}
                    >
                      <Button>重新选择</Button>
                    </Upload>
                    <Button danger onClick={clearPendingImage}>
                      清除图片
                    </Button>
                  </Space>
                </div>
              </div>
            ) : (
              <>
                <Typography.Text type="secondary">
                  先选择或粘贴图片（Ctrl+V / ⌘+V），再填写说明后确认添加
                </Typography.Text>
                <div style={{ marginTop: 12 }}>
                  <Upload
                    accept="image/*"
                    showUploadList={false}
                    beforeUpload={(file) => {
                      pickImageFile(file);
                      return false;
                    }}
                  >
                    <Button type="primary" icon={<PlusOutlined />}>
                      选择图片
                    </Button>
                  </Upload>
                </div>
              </>
            )}
          </div>
        </Space>
      </Modal>

      <Modal
        title="生成培训照片"
        open={genOpen}
        onCancel={() => setGenOpen(false)}
        okText="生成"
        confirmLoading={genMu.isPending}
        onOk={() => void submitGenerate()}
        destroyOnHidden
        width={560}
      >
        <Form form={genForm} layout="vertical" style={{ marginTop: 8 }}>
          <Form.Item
            name="title"
            label="标题"
            rules={[{ required: true, message: '请填写标题' }]}
          >
            <Input placeholder="如：华友锂业浸出工高级培训照片" maxLength={120} />
          </Form.Item>
          <Form.Item name="workType" label="培训工种">
            <Input placeholder="如：浸出工" maxLength={80} />
          </Form.Item>
          <Form.Item name="level" label="培训级别">
            <Input placeholder="如：高级工" maxLength={40} />
          </Form.Item>
          <Form.Item name="trainTime" label="培训时间">
            <Input placeholder="如：2024-06-01至2024-06-30" maxLength={80} />
          </Form.Item>
          <Form.Item name="trainUnit" label="培训单位">
            <Input placeholder="培训机构名称" maxLength={120} />
          </Form.Item>
          <Form.Item name="trainAddress" label="培训地址">
            <Input placeholder="培训地点" maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
