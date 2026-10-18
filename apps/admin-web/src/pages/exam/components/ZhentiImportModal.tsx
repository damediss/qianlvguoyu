import {
  Alert,
  Button,
  Input,
  Modal,
  Progress,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { batchImportZhentis, getZhentiImportLog, updateZhenti } from '../../../api/exam-zhenti';
import type { ZhentiImportFileResult, ZhentiImportLog } from '../../../types/examZhenti';
import {
  canRetryImportRow,
  extractErrorMessage,
  validateZipFile,
} from '../pastPaperUtils';

interface Props {
  open: boolean;
  examId?: string;
  examName?: string;
  onCancel: () => void;
  onSuccess: () => void;
}

interface Draft {
  title?: string;
  year?: string;
}

/**
 * D3 批量导入：≤50 同步返回逐文件结果；>50 转 importId 轮询 D6。
 * 解析失败（缺年份/工种等级）的文件已入库，可就地补录标题/年份后重试（D4）；
 * 未入库的硬失败（类型/超限/重复）提示重新上传。
 */
export function ZhentiImportModal({ open, examId, examName, onCancel, onSuccess }: Props) {
  const [file, setFile] = useState<File | null>(null);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [results, setResults] = useState<ZhentiImportFileResult[] | null>(null);
  const [importId, setImportId] = useState<string | undefined>();
  const [asyncProgress, setAsyncProgress] = useState<{ total: number; finished: number } | null>(null);
  const [asyncStatus, setAsyncStatus] = useState<string | undefined>();
  const [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [retrying, setRetrying] = useState<number | null>(null);
  const successRef = useRef(onSuccess);
  successRef.current = onSuccess;

  useEffect(() => {
    if (!open) {
      setFile(null);
      setFileList([]);
      setError(null);
      setProgress(null);
      setResults(null);
      setImportId(undefined);
      setAsyncProgress(null);
      setAsyncStatus(undefined);
      setDrafts({});
      setRetrying(null);
    }
  }, [open]);

  useEffect(() => {
    if (!importId) return;
    let cancelled = false;
    const tick = async () => {
      try {
        const log = (await getZhentiImportLog({ importId })) as ZhentiImportLog;
        if (cancelled) return;
        setAsyncStatus(log.status);
        setAsyncProgress({ total: log.total, finished: log.finished });
        if (log.status !== 'running') {
          setResults(log.results ?? []);
          setImportId(undefined);
          successRef.current();
        }
      } catch (err) {
        if (!cancelled) {
          setError(extractErrorMessage(err, '导入进度查询失败'));
          setImportId(undefined);
        }
      }
    };
    void tick();
    const timer = window.setInterval(tick, 2000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [importId]);

  const handleSelect = (candidate: File) => {
    const invalid = validateZipFile(candidate);
    if (invalid) {
      setError(invalid);
      setFile(null);
      setFileList([]);
      return false;
    }
    setError(null);
    setFile(candidate);
    setFileList([{ uid: '-1', name: candidate.name, status: 'done', size: candidate.size }]);
    return false;
  };

  const handleSubmit = async () => {
    if (!examId) {
      setError('请先选择考试项目');
      return;
    }
    if (!file) {
      setError('请选择 zip 压缩包');
      return;
    }
    setError(null);
    setResults(null);
    setProgress(0);
    try {
      const res = await batchImportZhentis(file, examId, setProgress);
      if (res.mode === 'async' && res.importId) {
        setImportId(res.importId);
        setAsyncStatus('running');
        setAsyncProgress({ total: 0, finished: 0 });
      } else {
        setResults(res.results ?? []);
        onSuccess();
      }
    } catch (err) {
      setError(extractErrorMessage(err, '批量导入失败，请重试'));
    } finally {
      setProgress(null);
    }
  };

  const handleRetry = async (index: number) => {
    const row = results?.[index];
    if (!row?.id) return;
    const draft = drafts[index] ?? {};
    setRetrying(index);
    try {
      await updateZhenti({
        id: row.id,
        title: (draft.title ?? row.title ?? '').trim(),
        year: (draft.year ?? row.year ?? '').trim(),
      });
      setResults((prev) =>
        prev?.map((item, i) =>
          i === index
            ? {
                ...item,
                ok: true,
                pending: false,
                reason: undefined,
                title: draft.title ?? item.title,
                year: draft.year ?? item.year,
              }
            : item,
        ) ?? null,
      );
      message.success('补录成功，已上架');
      onSuccess();
    } catch (err) {
      message.error(extractErrorMessage(err, '补录失败，请重试'));
    } finally {
      setRetrying(null);
    }
  };

  const columns: ColumnsType<ZhentiImportFileResult> = [
    { title: '文件名', dataIndex: 'fileName', width: 220, ellipsis: true },
    {
      title: '解析标题',
      dataIndex: 'title',
      width: 240,
      render: (_, row, index) =>
        canRetryImportRow(row) ? (
          <Input
            size="small"
            placeholder="标题"
            value={drafts[index]?.title ?? row.title ?? ''}
            onChange={(event) =>
              setDrafts((prev) => ({
                ...prev,
                [index]: { ...prev[index], title: event.target.value },
              }))
            }
          />
        ) : (
          <span>{row.title || '-'}</span>
        ),
    },
    {
      title: '年份',
      dataIndex: 'year',
      width: 110,
      render: (_, row, index) =>
        canRetryImportRow(row) ? (
          <Input
            size="small"
            placeholder="年份"
            value={drafts[index]?.year ?? row.year ?? ''}
            onChange={(event) =>
              setDrafts((prev) => ({
                ...prev,
                [index]: { ...prev[index], year: event.target.value },
              }))
            }
          />
        ) : (
          <span>{row.year || '-'}</span>
        ),
    },
    {
      title: '结果',
      width: 100,
      render: (_, row) =>
        row.ok ? (
          <Tag color="green">成功</Tag>
        ) : row.pending ? (
          <Tag color="orange">待补录</Tag>
        ) : (
          <Tag color="red">失败</Tag>
        ),
    },
    {
      title: '原因 / 操作',
      render: (_, row, index) =>
        canRetryImportRow(row) ? (
          <Space>
            <Typography.Text type="warning">缺年份或工种等级</Typography.Text>
            <Button
              size="small"
              type="link"
              loading={retrying === index}
              onClick={() => handleRetry(index)}
            >
              补录后重试
            </Button>
          </Space>
        ) : row.ok ? (
          <Typography.Text type="secondary">已导入</Typography.Text>
        ) : (
          <Typography.Text type="secondary">
            {row.reason || '导入失败'}（该文件未入库，请重新上传）
          </Typography.Text>
        ),
    },
  ];

  return (
    <Modal
      open={open}
      title="批量导入真题（zip）"
      okText={progress === null ? '开始导入' : '上传中…'}
      cancelText={results || asyncStatus ? '关闭' : '取消'}
      confirmLoading={progress !== null}
      onCancel={onCancel}
      onOk={results || asyncStatus === 'running' ? onCancel : handleSubmit}
      okButtonProps={{ style: results || asyncStatus === 'running' ? { display: 'none' } : undefined }}
      destroyOnHidden
      width={880}
    >
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="将多份真题 PDF 打成一个 zip（不要带文件夹层级），文件名即元数据来源。"
          description={
            <span>
              命名建议：<Typography.Text code>工种（别名）等级科目-年份.pdf</Typography.Text>，
              示例：<Typography.Text code>铝电解工（铝及铝合金熔铸工）五级理论科目-2024.pdf</Typography.Text>。
              zip ≤200MB，内 PDF 单个 ≤50MB、数量 ≤100 个；非 PDF 文件跳过并计入失败明细。
              ≤50 个文件同步返回结果，超过 50 个自动转后台处理并可查看进度。
            </span>
          }
        />
        <Typography.Text type="secondary">
          考试项目：<Typography.Text strong>{examName || '-'}</Typography.Text>
        </Typography.Text>
        {!results && !asyncStatus && (
          <>
            <Upload
              accept=".zip"
              maxCount={1}
              fileList={fileList}
              beforeUpload={(candidate) => handleSelect(candidate as File)}
              onRemove={() => {
                setFile(null);
                setFileList([]);
                return true;
              }}
            >
              <Button icon={<UploadOutlined />}>选择 zip 文件</Button>
            </Upload>
            {progress !== null && <Progress percent={progress} size="small" />}
          </>
        )}
        {asyncStatus === 'running' && (
          <Alert
            type="info"
            showIcon
            message="正在后台导入，请勿关闭页面"
            description={
              <Progress
                percent={
                  asyncProgress && asyncProgress.total > 0
                    ? Math.round((asyncProgress.finished / asyncProgress.total) * 100)
                    : 0
                }
                format={() =>
                  asyncProgress ? `${asyncProgress.finished}/${asyncProgress.total}` : '0/0'
                }
              />
            }
          />
        )}
        {asyncStatus === 'failed' && <Alert type="error" showIcon message="后台导入中断，请重新发起" />}
        {error && <Alert type="error" showIcon message={error} />}
        {results && (
          <Table
            rowKey={(row) => row.fileName}
            size="small"
            columns={columns}
            dataSource={results}
            pagination={false}
            scroll={{ y: 320 }}
          />
        )}
      </Space>
    </Modal>
  );
}
