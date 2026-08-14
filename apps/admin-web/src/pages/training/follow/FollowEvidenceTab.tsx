/**
 * 跟班 · 课堂影像留痕 + 培训照片生成
 * 课次齐套表为主；点「查看」弹出该课次影像墙。
 */
import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Empty,
  Image,
  Input,
  Modal,
  Progress,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiAuthImage, ApiAuthVideo } from '../../../components/ApiAuthImage';
import { downloadUrlFile } from '../../../api/files';
import * as trainApi from '../../../api/training';
import type { FollowEvidence, FollowEvidenceCoverageItem } from '../../../api/training';

type Props = {
  classId: number;
};

function readVideoDurationSeconds(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.onloadedmetadata = () => {
      const sec = Math.round(video.duration || 0);
      URL.revokeObjectURL(url);
      resolve(sec);
    };
    video.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('无法读取视频时长'));
    };
    video.src = url;
  });
}

function statusTag(v?: string) {
  if (v === 'approved') return <Tag color="success">已通过</Tag>;
  if (v === 'rejected') return <Tag color="error">已驳回</Tag>;
  return <Tag color="processing">待审</Tag>;
}

export function FollowEvidenceTab({ classId }: Props) {
  const qc = useQueryClient();
  const [wallSession, setWallSession] = useState<FollowEvidenceCoverageItem | null>(null);
  const [wallStatus, setWallStatus] = useState<string | undefined>();
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadScheduleId, setUploadScheduleId] = useState<number | undefined>();
  const [uploadMediaType, setUploadMediaType] = useState<'photo' | 'video'>('photo');
  const [caption, setCaption] = useState('');
  const [rejectOpen, setRejectOpen] = useState<FollowEvidence | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [batchRejectOpen, setBatchRejectOpen] = useState(false);
  const [batchRejectReason, setBatchRejectReason] = useState('');
  const [videoPreview, setVideoPreview] = useState<FollowEvidence | null>(null);
  const [selectedIds, setSelectedIds] = useState<Array<string | number>>([]);

  const coverageQ = useQuery({
    queryKey: ['follow-evidence-coverage', classId],
    queryFn: () => trainApi.getFollowEvidenceCoverage(classId),
  });

  const wallScheduleId = wallSession?.scheduleId;

  const wallQ = useQuery({
    queryKey: ['follow-evidences-wall', classId, wallScheduleId, wallStatus],
    queryFn: () =>
      trainApi.listFollowEvidences(classId, {
        scheduleId: wallScheduleId,
        reviewStatus: wallStatus,
      }),
    enabled: wallScheduleId != null,
  });

  const latestQ = useQuery({
    queryKey: ['follow-photo-pack-latest', classId],
    queryFn: () => trainApi.getLatestFollowPhotoPack(classId),
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['follow-evidence-coverage', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-evidences-wall', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-photo-pack-latest', classId] });
  };

  const openWall = (row: FollowEvidenceCoverageItem) => {
    setWallStatus(undefined);
    setSelectedIds([]);
    setWallSession(row);
  };

  const closeWall = () => {
    setWallSession(null);
    setWallStatus(undefined);
    setSelectedIds([]);
    setBatchRejectOpen(false);
    setBatchRejectReason('');
  };

  const uploadMu = useMutation({
    mutationFn: async (file: File) => {
      if (uploadScheduleId == null) {
        throw new Error('请选择课次');
      }
      let durationSeconds: number | undefined;
      if (uploadMediaType === 'video') {
        durationSeconds = await readVideoDurationSeconds(file);
        if (durationSeconds < 13 || durationSeconds > 17) {
          throw new Error(`视频须约 15 秒（允许 13～17），当前 ${durationSeconds} 秒`);
        }
      }
      return trainApi.uploadFollowEvidence(classId, file, {
        scheduleId: uploadScheduleId,
        caption: caption || undefined,
        mediaType: uploadMediaType,
        durationSeconds,
      });
    },
    onSuccess: () => {
      message.success('已上传，待审核');
      setUploadOpen(false);
      setCaption('');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '上传失败'),
  });

  const reviewMu = useMutation({
    mutationFn: (p: { id: number | string; reviewStatus: string; rejectReason?: string }) =>
      trainApi.reviewFollowEvidence(p.id, { reviewStatus: p.reviewStatus, rejectReason: p.rejectReason }),
    onSuccess: () => {
      message.success('已更新审核状态');
      setRejectOpen(null);
      setRejectReason('');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '审核失败'),
  });

  const batchReviewMu = useMutation({
    mutationFn: async (p: {
      ids: Array<number | string>;
      reviewStatus: 'approved' | 'rejected';
      rejectReason?: string;
    }) => {
      let ok = 0;
      const errors: string[] = [];
      for (const id of p.ids) {
        try {
          await trainApi.reviewFollowEvidence(id, {
            reviewStatus: p.reviewStatus,
            rejectReason: p.rejectReason,
          });
          ok += 1;
        } catch (e) {
          errors.push(`${id}: ${e instanceof Error ? e.message : '失败'}`);
        }
      }
      return { ok, fail: p.ids.length - ok, errors };
    },
    onSuccess: (res, vars) => {
      const action = vars.reviewStatus === 'approved' ? '通过' : '驳回';
      if (res.fail === 0) {
        message.success(`已批量${action} ${res.ok} 条`);
      } else {
        message.warning(`批量${action}：成功 ${res.ok}，失败 ${res.fail}`);
      }
      setSelectedIds([]);
      setBatchRejectOpen(false);
      setBatchRejectReason('');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '批量审核失败'),
  });

  const deleteMu = useMutation({
    mutationFn: (id: number | string) => trainApi.deleteFollowEvidence(id),
    onSuccess: () => {
      message.success('已删除');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const genMu = useMutation({
    mutationFn: () => trainApi.generateFollowPhotoPack(classId),
    onSuccess: async (res) => {
      message.success('培训照片已生成，开始下载');
      invalidate();
      const path = res.filePath || res.url;
      const name = `${res.title || '培训照片'}.docx`;
      if (path) {
        try {
          await downloadUrlFile(path, name);
        } catch (e) {
          message.error(e instanceof Error ? e.message : '下载失败，请稍后点「下载最近生成」');
        }
      }
    },
    onError: (e: Error) => message.error(e.message || '生成失败'),
  });

  const downloadLatest = async () => {
    const latest = latestQ.data;
    if (!latest?.exists) {
      message.warning('尚无生成记录');
      return;
    }
    const path = latest.filePath || latest.url;
    if (!path) {
      message.error('缺少文件路径');
      return;
    }
    try {
      await downloadUrlFile(path, `${latest.title || '培训照片'}.docx`);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '下载失败');
    }
  };

  const scheduleOptions = useMemo(() => {
    const items = coverageQ.data?.items ?? [];
    return items.map((it) => ({
      value: it.scheduleId,
      label: `${it.scheduleDate || ''} ${it.courseName || '课次'}（${it.teachFormLabel || it.teachForm || '—'}）学时${it.hoursDisplay || '—'}`,
    }));
  }, [coverageQ.data]);

  const minPhotos = coverageQ.data?.minPhotos ?? 5;
  const minVideos = coverageQ.data?.minVideos ?? 3;

  /** 弹窗内用最新 coverage 行刷新进度（审核后） */
  const liveSession = useMemo(() => {
    if (!wallSession) return null;
    const fresh = (coverageQ.data?.items ?? []).find(
      (it) => String(it.scheduleId) === String(wallSession.scheduleId),
    );
    return fresh || wallSession;
  }, [coverageQ.data, wallSession]);

  const wallItems = wallQ.data ?? [];
  const wallPhotos = wallItems.filter((i) => i.mediaType !== 'video');
  const wallVideos = wallItems.filter((i) => i.mediaType === 'video');
  const pendingItems = wallItems.filter((i) => i.reviewStatus === 'pending');
  const pendingIds = pendingItems.map((i) => i.id);
  const selectedPendingIds = selectedIds.filter((id) =>
    pendingItems.some((p) => String(p.id) === String(id)),
  );
  const allPendingSelected =
    pendingIds.length > 0 && pendingIds.every((id) => selectedIds.some((s) => String(s) === String(id)));

  const toggleSelect = (id: number | string, checked: boolean) => {
    setSelectedIds((prev) => {
      if (checked) {
        if (prev.some((x) => String(x) === String(id))) return prev;
        return [...prev, id];
      }
      return prev.filter((x) => String(x) !== String(id));
    });
  };

  const selectAllPending = () => {
    setSelectedIds(pendingIds);
  };

  const clearSelection = () => setSelectedIds([]);

  const runBatchApprove = () => {
    if (!selectedPendingIds.length) {
      message.warning('请先勾选待审影像');
      return;
    }
    Modal.confirm({
      title: `批量通过 ${selectedPendingIds.length} 条？`,
      content: '仅对待审影像生效',
      onOk: () =>
        batchReviewMu.mutateAsync({
          ids: selectedPendingIds,
          reviewStatus: 'approved',
        }),
    });
  };

  const openBatchReject = () => {
    if (!selectedPendingIds.length) {
      message.warning('请先勾选待审影像');
      return;
    }
    setBatchRejectReason('');
    setBatchRejectOpen(true);
  };

  const coverageColumns: ColumnsType<FollowEvidenceCoverageItem> = [
    { title: '日期', dataIndex: 'scheduleDate', width: 110 },
    { title: '课程', dataIndex: 'courseName', ellipsis: true },
    {
      title: '形式',
      dataIndex: 'teachFormLabel',
      width: 80,
      render: (v, row) => v || row.teachForm || '—',
    },
    {
      title: '学时',
      width: 90,
      render: (_, row) => row.hoursDisplay || row.classHours || '—',
    },
    {
      title: '照片',
      width: 130,
      render: (_, row) => (
        <span>
          已审 {row.approvedCount}/{row.minPhotos ?? row.minRequired ?? 5}
          {row.pendingCount ? ` · 待审 ${row.pendingCount}` : ''}
        </span>
      ),
    },
    {
      title: '视频',
      width: 130,
      render: (_, row) => (
        <span>
          已审 {row.videoApprovedCount ?? 0}/{row.minVideos ?? 3}
          {row.videoPendingCount ? ` · 待审 ${row.videoPendingCount}` : ''}
        </span>
      ),
    },
    {
      title: '齐套',
      width: 80,
      render: (_, row) =>
        row.coverageOk ? <Tag color="success">达标</Tag> : <Tag color="warning">缺项</Tag>,
    },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      render: (_, row) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => openWall(row)}>
            查看
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setUploadScheduleId(row.scheduleId);
              setUploadMediaType('photo');
              setUploadOpen(true);
            }}
          >
            补传
          </Button>
        </Space>
      ),
    },
  ];

  const cov = coverageQ.data;
  const photoPct = liveSession
    ? Math.min(
        100,
        Math.round(
          ((Number(liveSession.approvedCount) || 0) / (liveSession.minPhotos ?? minPhotos ?? 5)) * 100,
        ),
      )
    : 0;
  const videoPct = liveSession
    ? Math.min(
        100,
        Math.round(
          ((Number(liveSession.videoApprovedCount) || 0) /
            (liveSession.minVideos ?? minVideos ?? 3)) *
            100,
        ),
      )
    : 0;

  const renderMediaCard = (row: FollowEvidence) => {
    const isVideo = row.mediaType === 'video';
    const isPending = row.reviewStatus === 'pending';
    const checked = selectedIds.some((id) => String(id) === String(row.id));
    return (
      <div
        key={String(row.id)}
        style={{
          border: checked ? '1px solid #1677ff' : '1px solid #f0f0f0',
          borderRadius: 8,
          overflow: 'hidden',
          background: '#fff',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: checked ? '0 0 0 1px rgba(22,119,255,0.2)' : undefined,
        }}
      >
        <div
          style={{
            height: 140,
            background: '#111',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative',
            cursor: isVideo ? 'pointer' : 'default',
          }}
          onClick={() => {
            if (isVideo) setVideoPreview(row);
          }}
        >
          {isPending && (
            <div
              style={{ position: 'absolute', right: 8, top: 8, zIndex: 2 }}
              onClick={(e) => e.stopPropagation()}
            >
              <Checkbox
                checked={checked}
                onChange={(e) => toggleSelect(row.id, e.target.checked)}
              />
            </div>
          )}
          {isVideo ? (
            <div style={{ color: '#fff', textAlign: 'center' }}>
              <div style={{ fontSize: 28, lineHeight: 1 }}>▶</div>
              <div style={{ fontSize: 12, marginTop: 6, opacity: 0.85 }}>
                {row.durationSeconds ?? 15}s · 点击播放
              </div>
            </div>
          ) : row.id ? (
            <Image.PreviewGroup>
              <ApiAuthImage
                url={`/training/follow-evidences/${row.id}/preview`}
                width="100%"
                height={140}
                style={{ objectFit: 'cover', width: '100%', height: 140 }}
              />
            </Image.PreviewGroup>
          ) : (
            '—'
          )}
          <div style={{ position: 'absolute', left: 8, top: 8 }}>{statusTag(row.reviewStatus)}</div>
        </div>
        <div style={{ padding: '8px 10px', flex: 1 }}>
          <Typography.Text type="secondary" style={{ fontSize: 12 }} ellipsis>
            {row.caption || (isVideo ? '课堂视频' : '课堂照片')}
          </Typography.Text>
        </div>
        <div style={{ padding: '0 8px 8px', display: 'flex', flexWrap: 'wrap', gap: 4 }}>
          {isPending && (
            <>
              <Button
                size="small"
                type="link"
                icon={<CheckOutlined />}
                onClick={() => reviewMu.mutate({ id: row.id, reviewStatus: 'approved' })}
              >
                通过
              </Button>
              <Button
                size="small"
                type="link"
                danger
                icon={<CloseOutlined />}
                onClick={() => setRejectOpen(row)}
              >
                驳回
              </Button>
            </>
          )}
          <Button
            size="small"
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => {
              Modal.confirm({
                title: '删除该影像？',
                onOk: () => deleteMu.mutateAsync(row.id),
              });
            }}
          >
            删除
          </Button>
        </div>
      </div>
    );
  };

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert
        type="info"
        showIcon
        message={`每节课须：照片 ${minPhotos} 张 + 视频 ${minVideos} 段×约 15 秒（审核通过后算达标）。点「查看」打开该课次影像墙。`}
      />

      <Card
        size="small"
        title="课次齐套"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => invalidate()}>
              刷新
            </Button>
            <Button
              type="primary"
              icon={<ThunderboltOutlined />}
              loading={genMu.isPending}
              onClick={() => genMu.mutate()}
            >
              生成培训照片
            </Button>
            {latestQ.data?.exists && (latestQ.data.filePath || latestQ.data.url) && (
              <Button icon={<DownloadOutlined />} onClick={() => void downloadLatest()}>
                下载最近生成
              </Button>
            )}
          </Space>
        }
      >
        <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
          应采 {cov?.requiredSessions ?? 0} 课次 · 达标 {cov?.okSessions ?? 0} · 缺项{' '}
          {cov?.missingSessions ?? 0}
          {cov?.ready ? ' · 已齐套' : ''}
        </Typography.Paragraph>
        <Table
          size="small"
          rowKey="scheduleId"
          loading={coverageQ.isFetching}
          columns={coverageColumns}
          dataSource={cov?.items ?? []}
          pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 课次` }}
          scroll={{ x: 900 }}
          onRow={(row) => ({
            onDoubleClick: () => openWall(row),
          })}
        />
      </Card>

      {/* 课次影像墙弹窗 */}
      <Modal
        title={
          liveSession
            ? `影像墙 · ${liveSession.scheduleDate || ''} ${liveSession.courseName || '课次'}`
            : '影像墙'
        }
        open={!!wallSession}
        onCancel={closeWall}
        footer={null}
        width={960}
        destroyOnHidden
        styles={{ body: { maxHeight: '72vh', overflowY: 'auto' } }}
      >
        {liveSession && (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: 16,
                background: '#fafafa',
                borderRadius: 8,
                padding: 12,
              }}
            >
              <div>
                <Typography.Text type="secondary">课次信息</Typography.Text>
                <div style={{ marginTop: 4 }}>
                  {liveSession.teachFormLabel || liveSession.teachForm || '—'} · 学时{' '}
                  {liveSession.hoursDisplay || liveSession.classHours || '—'}
                  {liveSession.startTime
                    ? ` · ${liveSession.startTime}${liveSession.endTime ? `–${liveSession.endTime}` : ''}`
                    : ''}
                </div>
                <div style={{ marginTop: 8 }}>
                  {liveSession.coverageOk ? (
                    <Tag color="success">已达标</Tag>
                  ) : (
                    <Tag color="warning">缺项</Tag>
                  )}
                </div>
              </div>
              <div>
                <div style={{ marginBottom: 8 }}>
                  <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                    <span>
                      照片 {liveSession.approvedCount}/{liveSession.minPhotos ?? minPhotos}
                    </span>
                    <Typography.Text type="secondary">
                      待审 {liveSession.pendingCount || 0}
                    </Typography.Text>
                  </Space>
                  <Progress percent={photoPct} size="small" status={liveSession.photoOk ? 'success' : 'active'} />
                </div>
                <div>
                  <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                    <span>
                      视频 {liveSession.videoApprovedCount ?? 0}/{liveSession.minVideos ?? minVideos}
                    </span>
                    <Typography.Text type="secondary">
                      待审 {liveSession.videoPendingCount || 0}
                    </Typography.Text>
                  </Space>
                  <Progress percent={videoPct} size="small" status={liveSession.videoOk ? 'success' : 'active'} />
                </div>
              </div>
            </div>

            <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
              <Space wrap>
                <Select
                  allowClear
                  placeholder="审核状态"
                  style={{ width: 140 }}
                  value={wallStatus}
                  onChange={(v) => {
                    setWallStatus(v);
                    setSelectedIds([]);
                  }}
                  options={[
                    { value: 'pending', label: '待审' },
                    { value: 'approved', label: '已通过' },
                    { value: 'rejected', label: '已驳回' },
                  ]}
                />
                <Checkbox
                  checked={allPendingSelected}
                  indeterminate={selectedPendingIds.length > 0 && !allPendingSelected}
                  disabled={!pendingIds.length}
                  onChange={(e) => {
                    if (e.target.checked) selectAllPending();
                    else clearSelection();
                  }}
                >
                  全选待审（{pendingIds.length}）
                </Checkbox>
                {selectedPendingIds.length > 0 && (
                  <Typography.Text type="secondary">已选 {selectedPendingIds.length}</Typography.Text>
                )}
              </Space>
              <Space wrap>
                <Button
                  icon={<CheckOutlined />}
                  type="primary"
                  disabled={!selectedPendingIds.length}
                  loading={batchReviewMu.isPending}
                  onClick={runBatchApprove}
                >
                  批量通过
                </Button>
                <Button
                  danger
                  icon={<CloseOutlined />}
                  disabled={!selectedPendingIds.length}
                  loading={batchReviewMu.isPending}
                  onClick={openBatchReject}
                >
                  批量驳回
                </Button>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={() => {
                    void wallQ.refetch();
                    invalidate();
                  }}
                >
                  刷新
                </Button>
                <Button
                  type="primary"
                  ghost
                  icon={<PlusOutlined />}
                  onClick={() => {
                    setUploadScheduleId(liveSession.scheduleId);
                    setUploadMediaType('photo');
                    setUploadOpen(true);
                  }}
                >
                  补传影像
                </Button>
              </Space>
            </Space>

            <Spin spinning={wallQ.isFetching}>
              {!wallItems.length ? (
                <Empty description="本课次暂无影像，可点补传" />
              ) : (
                <Space direction="vertical" style={{ width: '100%' }} size="large">
                  <div>
                    <Typography.Title level={5} style={{ margin: '0 0 12px' }}>
                      照片（{wallPhotos.length}）
                    </Typography.Title>
                    {wallPhotos.length ? (
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
                          gap: 12,
                        }}
                      >
                        {wallPhotos.map(renderMediaCard)}
                      </div>
                    ) : (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无照片" />
                    )}
                  </div>
                  <div>
                    <Typography.Title level={5} style={{ margin: '0 0 12px' }}>
                      视频（{wallVideos.length}）
                    </Typography.Title>
                    {wallVideos.length ? (
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
                          gap: 12,
                        }}
                      >
                        {wallVideos.map(renderMediaCard)}
                      </div>
                    ) : (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无视频" />
                    )}
                  </div>
                </Space>
              )}
            </Spin>
          </Space>
        )}
      </Modal>

      <Modal
        title="补传课堂影像"
        open={uploadOpen}
        onCancel={() => setUploadOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div>
            <Typography.Text type="secondary">课次</Typography.Text>
            <Select
              style={{ width: '100%', marginTop: 8 }}
              options={scheduleOptions}
              value={uploadScheduleId}
              onChange={setUploadScheduleId}
              placeholder="选择课次"
            />
          </div>
          <div>
            <Typography.Text type="secondary">类型</Typography.Text>
            <Select
              style={{ width: '100%', marginTop: 8 }}
              value={uploadMediaType}
              onChange={(v) => setUploadMediaType(v)}
              options={[
                { value: 'photo', label: '照片' },
                { value: 'video', label: '视频（约 15 秒）' },
              ]}
            />
          </div>
          <Input
            placeholder="说明（可选）"
            value={caption}
            onChange={(e) => setCaption(e.target.value)}
          />
          <Upload
            accept={uploadMediaType === 'video' ? 'video/*' : 'image/*'}
            showUploadList={false}
            beforeUpload={(file) => {
              uploadMu.mutate(file);
              return false;
            }}
          >
            <Button type="primary" loading={uploadMu.isPending} block>
              {uploadMediaType === 'video' ? '选择视频上传' : '选择图片上传'}
            </Button>
          </Upload>
        </Space>
      </Modal>

      <Modal
        title="驳回影像"
        open={!!rejectOpen}
        onCancel={() => setRejectOpen(null)}
        onOk={() => {
          if (!rejectOpen) return;
          if (!rejectReason.trim()) {
            message.warning('请填写驳回原因');
            return;
          }
          reviewMu.mutate({
            id: rejectOpen.id,
            reviewStatus: 'rejected',
            rejectReason: rejectReason.trim(),
          });
        }}
        confirmLoading={reviewMu.isPending}
        destroyOnHidden
      >
        <Input.TextArea
          rows={3}
          placeholder="驳回原因"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>

      <Modal
        title={`批量驳回（${selectedPendingIds.length} 条）`}
        open={batchRejectOpen}
        onCancel={() => setBatchRejectOpen(false)}
        onOk={() => {
          if (!batchRejectReason.trim()) {
            message.warning('请填写驳回原因');
            return;
          }
          batchReviewMu.mutate({
            ids: selectedPendingIds,
            reviewStatus: 'rejected',
            rejectReason: batchRejectReason.trim(),
          });
        }}
        confirmLoading={batchReviewMu.isPending}
        destroyOnHidden
        okButtonProps={{ danger: true }}
      >
        <Input.TextArea
          rows={3}
          placeholder="统一驳回原因"
          value={batchRejectReason}
          onChange={(e) => setBatchRejectReason(e.target.value)}
        />
      </Modal>

      <Modal
        title={`视频预览${videoPreview?.durationSeconds ? `（${videoPreview.durationSeconds}s）` : ''}`}
        open={!!videoPreview}
        onCancel={() => setVideoPreview(null)}
        footer={null}
        width={720}
        destroyOnHidden
        zIndex={1100}
      >
        {videoPreview?.id ? (
          <ApiAuthVideo
            url={`/training/follow-evidences/${videoPreview.id}/preview`}
            width="100%"
            height={400}
            style={{ objectFit: 'contain', maxHeight: 480 }}
          />
        ) : null}
      </Modal>
    </Space>
  );
}
