import {
  ArrowLeftOutlined,
  ClusterOutlined,
  CopyOutlined,
  DownloadOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  LinkOutlined,
  MonitorOutlined,
  PlayCircleOutlined,
  QrcodeOutlined,
  ReloadOutlined,
  SearchOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Input,
  List,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { TableColumnsType } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { downloadUrlFile } from '../../../api/files';
import {
  exceptionRelease,
  exportCollectionForms,
  exportCollectionPdf,
  exportQrCodes,
  generateQrCodes,
  getCollectionDetail,
  getCollectionExceptions,
  getCollectionWorkbench,
  markInfoError,
  openCandidateCollection,
} from '../../../api/certificationPracticalCollection';
import { CurrentCandidateCollectionPanel } from '../../../components/certification/collection/CurrentCandidateCollectionPanel';
import { useNavStore } from '../../../stores/navStore';
import {
  normalizeActiveCollectionCandidate,
  toCandidateCollectionContext,
} from '../../../utils/collectionCandidate';
import type {
  CandidateCollectionContext,
  CandidateCollectionDetailVO,
  CandidateCollectionItemVO,
  CollectionEntrySource,
  CollectionExceptionStatus,
  CollectionExceptionVO,
  CollectionOverallStatus,
  FingerprintStatus,
  SignatureStatus,
  SnowflakeId,
} from '../../../types/certificationPracticalCollection';
import {
  CollectionExceptionStatusText,
  CollectionOverallStatusText,
  CollectionQueueStatusText,
  FingerprintCollectStatusText,
  FingerprintStatusText,
  SignatureStatusText,
} from '../../../types/certificationPracticalCollection';
import { CollectionExceptionPage } from './CollectionExceptionPage';
import { ExceptionReleaseModal } from '../../../components/certification/collection/ExceptionReleaseModal';
import { InfoErrorModal } from '../../../components/certification/collection/InfoErrorModal';
import './CollectionTaskListPage.css';

interface CollectionManagePageProps {
  taskId?: string;
}

type OverallFilter = 'all' | CollectionOverallStatus;
type ExceptionFilter = 'all' | 'hasException' | CollectionExceptionStatus;

interface ManageStats {
  candidateTotal: number;
  qrIssuedCount: number;
  arrivedCount: number;
  queuedCount: number;
  collectingCount: number;
  completedCount: number;
  pendingRetryCount: number;
  infoErrorCount: number;
  exceptionCount: number;
  pendingCollectCount: number;
}

const overallFilterOptions: Array<{ label: string; value: OverallFilter }> = [
  { label: '全部状态', value: 'all' },
  { label: '未发放', value: 'NOT_ISSUED' },
  { label: '已发放', value: 'ISSUED' },
  { label: '未到场', value: 'NOT_ARRIVED' },
  { label: '已到场', value: 'ARRIVED' },
  { label: '已入队', value: 'QUEUED' },
  { label: '采集中', value: 'COLLECTING' },
  { label: '待补采', value: 'PENDING_RETRY' },
  { label: '信息异常', value: 'INFO_ERROR' },
  { label: '异常放行', value: 'EXCEPTION_RELEASED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
];

const exceptionFilterOptions: Array<{ label: string; value: ExceptionFilter }> = [
  { label: '全部异常', value: 'all' },
  { label: '有异常', value: 'hasException' },
  { label: '待补采', value: 'PENDING_RETRY' },
  { label: '信息异常', value: 'INFO_ERROR' },
  { label: '已放行', value: 'RELEASED' },
  { label: '无异常', value: 'NONE' },
];

const SignatureRecordStatusText: Record<string, string> = {
  ACTIVE: '有效',
  REVOKED: '已撤销',
};

function toNumber(value: unknown) {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function shortHash(value?: string) {
  if (!value) return '-';
  if (value.length <= 18) return value;
  return `${value.slice(0, 10)}...${value.slice(-6)}`;
}

function statusColor(value?: string) {
  if (!value) return 'default';
  if (['COMPLETED', 'DONE', 'SUCCESS', 'ACTIVE'].includes(value)) return 'green';
  if (['COLLECTING', 'CAPTURING', 'SIGNING', 'ARRIVED', 'QUEUED', 'ISSUED'].includes(value)) return 'blue';
  if (['PENDING_RETRY'].includes(value)) return 'orange';
  if (['INFO_ERROR', 'FAILED'].includes(value)) return 'red';
  if (['EXCEPTION_RELEASED', 'RELEASED'].includes(value)) return 'purple';
  return 'default';
}

function statusTag(value?: string, label?: string, textMap?: Record<string, string>) {
  const text = label || (value ? textMap?.[value] || value : '-');
  return <Tag color={statusColor(value)}>{text}</Tag>;
}

function normalizeStats(
  workbench?: Awaited<ReturnType<typeof getCollectionWorkbench>>,
): ManageStats {
  const stats = workbench?.stats;
  const task = workbench?.task;
  const candidates = workbench?.candidates || [];
  const candidateTotal = toNumber(stats?.candidateTotal || task?.candidateTotal || candidates.length);
  const completedCount = toNumber(stats?.completedCount || task?.completedCount);
  const exceptionCount = toNumber(stats?.exceptionCount || task?.exceptionCount);
  return {
    candidateTotal,
    qrIssuedCount: toNumber(stats?.qrIssuedCount || task?.qrIssuedCount),
    arrivedCount: toNumber(stats?.arrivedCount || task?.arrivedCount),
    queuedCount: toNumber(stats?.queuedCount || task?.queuedCount),
    collectingCount: toNumber(stats?.collectingCount || task?.collectingCount),
    completedCount,
    pendingRetryCount: toNumber(stats?.pendingRetryCount || task?.pendingRetryCount),
    infoErrorCount: toNumber(stats?.infoErrorCount),
    exceptionCount,
    pendingCollectCount: Math.max(candidateTotal - completedCount - exceptionCount, 0),
  };
}

function candidateMatchesKeyword(candidate: CandidateCollectionItemVO, keyword: string) {
  const text = keyword.trim().toLowerCase();
  if (!text) return true;
  return [
    candidate.name,
    candidate.admissionNo,
    candidate.maskedIdCardNo,
    candidate.idCardLast4,
    candidate.unitName,
  ].some((value) => String(value || '').toLowerCase().includes(text));
}

function candidateMatchesException(candidate: CandidateCollectionItemVO, filter: ExceptionFilter) {
  if (filter === 'all') return true;
  if (filter === 'hasException') return candidate.exceptionStatus !== 'NONE';
  return candidate.exceptionStatus === filter;
}

function fallbackText(value?: unknown) {
  return value === null || value === undefined || value === '' ? '-' : String(value);
}

function isSameId(left?: SnowflakeId | null, right?: SnowflakeId | null) {
  return left !== undefined && left !== null && right !== undefined && right !== null && String(left) === String(right);
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
}

export function CollectionManagePage({ taskId }: CollectionManagePageProps) {
  const queryClient = useQueryClient();
  const setSelectedKey = useNavStore((state) => state.setSelectedKey);
  const [keyword, setKeyword] = useState('');
  const [overallFilter, setOverallFilter] = useState<OverallFilter>('all');
  const [exceptionFilter, setExceptionFilter] = useState<ExceptionFilter>('all');
  const [detailCandidate, setDetailCandidate] = useState<CandidateCollectionItemVO | null>(null);
  const [infoErrorCandidate, setInfoErrorCandidate] = useState<CandidateCollectionItemVO | null>(null);
  const [releaseCandidate, setReleaseCandidate] = useState<CandidateCollectionItemVO | null>(null);
  const [collectionCandidate, setCollectionCandidate] = useState<CandidateCollectionDetailVO | null>(null);
  const [collectionContext, setCollectionContext] = useState<CandidateCollectionContext | null>(null);
  const [exceptionModalOpen, setExceptionModalOpen] = useState(false);
  const [selectedCandidateIds, setSelectedCandidateIds] = useState<SnowflakeId[]>([]);

  const workbenchQuery = useQuery({
    queryKey: ['practical-collection-workbench', taskId],
    queryFn: () => getCollectionWorkbench(taskId!),
    enabled: Boolean(taskId),
  });

  const exceptionsQuery = useQuery({
    queryKey: ['practical-collection-exceptions', taskId, 'manage-overview'],
    queryFn: () => getCollectionExceptions({ page: 1, pageSize: 5, taskId }),
    enabled: Boolean(taskId),
  });

  const detailQuery = useQuery({
    queryKey: ['practical-collection-detail', taskId, detailCandidate?.candidateId],
    queryFn: () => getCollectionDetail(taskId!, detailCandidate!.candidateId),
    enabled: Boolean(taskId && detailCandidate?.candidateId),
  });

  const setCurrentCollectionCandidate = (
    candidate: CandidateCollectionDetailVO,
    entrySource: CollectionEntrySource,
  ) => {
    const normalized = normalizeActiveCollectionCandidate(candidate);
    setCollectionCandidate(normalized);
    setCollectionContext(toCandidateCollectionContext(normalized, entrySource));
  };

  const clearCollectionCandidate = () => {
    setCollectionCandidate(null);
    setCollectionContext(null);
  };

  const refreshAll = () => {
    queryClient.invalidateQueries({ queryKey: ['practical-collection-workbench', taskId] });
    queryClient.invalidateQueries({ queryKey: ['practical-collection-exceptions', taskId] });
    if (detailCandidate) {
      queryClient.invalidateQueries({ queryKey: ['practical-collection-detail', taskId, detailCandidate.candidateId] });
    }
    if (collectionCandidate) {
      queryClient.invalidateQueries({ queryKey: ['practical-collection-detail', taskId, collectionCandidate.candidateId] });
    }
  };

  const refreshCollectionCandidate = async () => {
    refreshAll();
    if (!collectionCandidate) {
      return;
    }
    try {
      const detail = await getCollectionDetail(taskId!, collectionCandidate.candidateId);
      setCurrentCollectionCandidate(detail, collectionContext?.entrySource || 'CANDIDATE_ROW');
    } catch {
      message.warning('当前考生采集数据刷新失败，请稍后重试');
    }
  };

  const generateQrMutation = useMutation({
    mutationFn: () => generateQrCodes(taskId!),
    onSuccess: (result) => {
      message.success(result.message || '二维码 token 已生成');
      refreshAll();
    },
  });

  const exportQrMutation = useMutation({
    mutationFn: () => exportQrCodes(taskId!),
    onSuccess: (result) => {
      message.success(`二维码已导出：${result.filename}`);
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '二维码导出失败，请稍后重试');
    },
  });

  const exportCollectionFormsMutation = useMutation({
    mutationFn: (candidateIds: SnowflakeId[]) => exportCollectionForms(taskId!, candidateIds),
    onSuccess: (result) => {
      message.success(`采集表已生成：${result.filename}`);
      setSelectedCandidateIds([]);
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '采集表生成失败，请补齐资料后重试');
    },
  });

  const exportCollectionPdfMutation = useMutation({
    mutationFn: (candidateIds: SnowflakeId[]) => exportCollectionPdf(taskId!, candidateIds),
    onSuccess: async (result) => {
      try {
        await downloadUrlFile(result.downloadUrl, result.fileName);
        message.success(`打印版 PDF 已生成：${result.fileName}`);
        setSelectedCandidateIds([]);
      } catch (error) {
        message.error(error instanceof Error ? error.message : 'PDF 下载失败，请稍后重试');
      }
    },
    onError: (error) => {
      message.error(error instanceof Error ? error.message : '打印版 PDF 生成失败，请补齐资料后重试');
    },
  });

  const markInfoErrorMutation = useMutation({
    mutationFn: (reason: string) => markInfoError({
      taskId: taskId!,
      candidateId: infoErrorCandidate!.candidateId,
      reason,
    }),
    onSuccess: () => {
      message.success('已标记信息异常');
      setInfoErrorCandidate(null);
      refreshAll();
    },
  });

  const releaseMutation = useMutation({
    mutationFn: (values: Parameters<typeof exceptionRelease>[0]) => exceptionRelease(values),
    onSuccess: () => {
      message.success('异常放行已记录');
      setReleaseCandidate(null);
      refreshAll();
    },
  });

  const openCandidateMutation = useMutation({
    mutationFn: (candidate: CandidateCollectionItemVO) => openCandidateCollection(taskId!, candidate.candidateId),
    onSuccess: (detail) => {
      setCurrentCollectionCandidate(detail, 'CANDIDATE_ROW');
      message.success(`已进入考生采集：${detail.name}`);
      refreshAll();
    },
    onError: (error) => {
      const errorMessage = error instanceof Error ? error.message : '打开考生采集失败，请稍后重试';
      if (errorMessage.includes('当前已有正在采集')) {
        Modal.warning({
          title: '列表入口冲突',
          content: errorMessage,
          okText: '我知道了',
        });
        return;
      }
      message.error(errorMessage);
    },
  });

  const workbench = workbenchQuery.data;
  const task = workbench?.task;
  const stats = normalizeStats(workbench);
  const candidates = workbench?.candidates || [];
  const recentExceptions = exceptionsQuery.data?.items || [];
  const tabletUrl = taskId ? `${window.location.origin}/practical-collection/workbench/${encodeURIComponent(taskId)}` : '';
  const lastUpdated = workbenchQuery.dataUpdatedAt ? formatDateTime(new Date(workbenchQuery.dataUpdatedAt).toISOString()) : '-';

  const filteredCandidates = useMemo(() => candidates.filter((candidate) => {
    if (!candidateMatchesKeyword(candidate, keyword)) return false;
    if (overallFilter !== 'all' && candidate.overallStatus !== overallFilter) return false;
    return candidateMatchesException(candidate, exceptionFilter);
  }), [candidates, exceptionFilter, keyword, overallFilter]);

  const goBackToList = () => {
    setSelectedKey('certification_practical_collection');
    window.location.assign('/certification/practical-collection');
  };

  const handleCopyTabletUrl = () => {
    if (!tabletUrl) {
      message.warning('缺少任务 ID，无法生成平板端链接');
      return;
    }
    copyText(tabletUrl)
      .then(() => message.success('平板端链接已复制'))
      .catch(() => message.error('复制失败，请手动复制链接'));
  };

  const getCurrentCollectingCandidate = (): CandidateCollectionDetailVO | CandidateCollectionItemVO | null => {
    if (collectionCandidate?.overallStatus === 'COLLECTING') {
      return collectionCandidate;
    }
    if (workbench?.currentCandidate?.overallStatus === 'COLLECTING') {
      return workbench.currentCandidate;
    }
    return candidates.find((candidate) => candidate.overallStatus === 'COLLECTING') || null;
  };

  const showCollectingConflict = (
    current: CandidateCollectionDetailVO | CandidateCollectionItemVO,
    selected: CandidateCollectionItemVO,
  ) => {
    Modal.warning({
      title: '当前已有采集中考生',
      content: (
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="当前考生">{current.name || '-'}</Descriptions.Item>
          <Descriptions.Item label="当前证件号">
            {current.maskedIdCardNo || (current.idCardLast4 ? `后四位 ${current.idCardLast4}` : '-')}
          </Descriptions.Item>
          <Descriptions.Item label="本次选择">{selected.name || '-'}</Descriptions.Item>
          <Descriptions.Item label="处理方式">请先完成或标记异常当前考生，刷新确认现场状态后再切换到其他考生。</Descriptions.Item>
        </Descriptions>
      ),
      okText: '我知道了',
    });
  };

  const validateCandidateEntry = (candidate: CandidateCollectionItemVO) => {
    if (candidate.collectionEligible === false) {
      Modal.warning({
        title: '资料未满足实操采集准入条件',
        content: (
          <div>
            <div>该考生的信息收集尚未完成，暂不能进入实操信息采集。</div>
            {candidate.missingDocumentCodes?.length ? <div>缺少资料：{candidate.missingDocumentCodes.join('、')}</div> : null}
            {candidate.invalidDocumentCodes?.length ? <div>资料异常：{candidate.invalidDocumentCodes.join('、')}</div> : null}
            <div>请先前往“认定筹备—信息收集”补充资料。</div>
          </div>
        ),
        okText: '我知道了',
      });
      return false;
    }
    const currentCollectingCandidate = getCurrentCollectingCandidate();
    if (currentCollectingCandidate && !isSameId(currentCollectingCandidate.candidateId, candidate.candidateId)) {
      showCollectingConflict(currentCollectingCandidate, candidate);
      return false;
    }
    return true;
  };

  const confirmOpenCandidateCollection = (candidate: CandidateCollectionItemVO) => {
    if (!validateCandidateEntry(candidate)) {
      return;
    }
    Modal.confirm({
      title: '确认进入考生采集',
      content: (
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="考生姓名">{candidate.name || '-'}</Descriptions.Item>
          <Descriptions.Item label="证件号">
            {candidate.maskedIdCardNo || (candidate.idCardLast4 ? `后四位 ${candidate.idCardLast4}` : '-')}
          </Descriptions.Item>
          <Descriptions.Item label="当前任务">{task?.taskName || '-'}</Descriptions.Item>
          <Descriptions.Item label="采集状态">
            {candidate.overallStatusLabel || CollectionOverallStatusText[candidate.overallStatus]}
          </Descriptions.Item>
        </Descriptions>
      ),
      okText: candidate.overallStatus === 'COMPLETED' ? '查看/继续采集' : '开始采集',
      cancelText: '取消',
      onOk: () => openCandidateMutation.mutateAsync(candidate),
    });
  };

  const candidateColumns: TableColumnsType<CandidateCollectionItemVO> = [
    {
      title: '姓名',
      dataIndex: 'name',
      width: 110,
      fixed: 'left',
      render: (value: string) => <Typography.Text strong>{value || '-'}</Typography.Text>,
    },
    {
      title: '考号',
      dataIndex: 'admissionNo',
      width: 140,
      render: fallbackText,
    },
    {
      title: '身份证号',
      width: 150,
      render: (_, candidate) => candidate.maskedIdCardNo || (candidate.idCardLast4 ? `后四位 ${candidate.idCardLast4}` : '-'),
    },
    {
      title: '单位',
      dataIndex: 'unitName',
      width: 150,
      render: fallbackText,
    },
    {
      title: '工种/等级',
      width: 170,
      render: (_, candidate) => `${candidate.workType || '-'} / ${candidate.level || '-'}`,
    },
    {
      title: '采集状态',
      dataIndex: 'overallStatus',
      width: 120,
      render: (value: CollectionOverallStatus, candidate) => (
        statusTag(value, candidate.overallStatusLabel, CollectionOverallStatusText)
      ),
    },
    {
      title: '资料状态',
      dataIndex: 'documentStatus',
      width: 220,
      render: (_value: string, candidate) => {
        const complete = candidate.collectionEligible === true;
        const label = complete ? '已完成' : candidate.documentStatus === 'INVALID' ? '资料异常' : '待补充';
        return (
          <Space direction="vertical" size={0}>
            <Tag color={complete ? 'green' : candidate.documentStatus === 'INVALID' ? 'red' : 'orange'}>{label}</Tag>
            {!complete && candidate.missingDocumentCodes?.length ? (
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                缺少：{candidate.missingDocumentCodes.join('、')}
              </Typography.Text>
            ) : null}
            {!complete && candidate.invalidDocumentCodes?.length ? (
              <Typography.Text type="danger" style={{ fontSize: 12 }}>
                异常：{candidate.invalidDocumentCodes.join('、')}
              </Typography.Text>
            ) : null}
          </Space>
        );
      },
    },
    {
      title: '指纹状态',
      dataIndex: 'fingerprintStatus',
      width: 110,
      render: (value: FingerprintStatus, candidate) => (
        statusTag(value, candidate.fingerprintStatusLabel, FingerprintStatusText)
      ),
    },
    {
      title: '签名状态',
      dataIndex: 'signatureStatus',
      width: 110,
      render: (value: SignatureStatus, candidate) => (
        statusTag(value, candidate.signatureStatusLabel, SignatureStatusText)
      ),
    },
    {
      title: '队列状态',
      dataIndex: 'queueStatus',
      width: 110,
      render: (value: string, candidate) => (
        statusTag(value, candidate.queueStatusLabel, CollectionQueueStatusText)
      ),
    },
    {
      title: '异常状态',
      dataIndex: 'exceptionStatus',
      width: 110,
      render: (value: CollectionExceptionStatus, candidate) => (
        statusTag(value, candidate.exceptionStatusLabel, CollectionExceptionStatusText)
      ),
    },
    {
      title: '操作',
      width: 260,
      fixed: 'right',
      render: (_, candidate) => (
        <Space size={4} className="collection-manage-row-actions">
          <Button
            size="small"
            type={candidate.overallStatus === 'COMPLETED' ? 'default' : 'primary'}
            icon={<PlayCircleOutlined />}
            loading={openCandidateMutation.isPending}
            onClick={(event) => {
              event.stopPropagation();
              confirmOpenCandidateCollection(candidate);
            }}
          >
            {candidate.overallStatus === 'COMPLETED' ? '查看/继续' : '采集'}
          </Button>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={(event) => {
              event.stopPropagation();
              setDetailCandidate(candidate);
            }}
          >
            详情
          </Button>
          <Button
            size="small"
            icon={<WarningOutlined />}
            onClick={(event) => {
              event.stopPropagation();
              setInfoErrorCandidate(candidate);
            }}
          >
            异常
          </Button>
          <Button
            size="small"
            danger
            onClick={(event) => {
              event.stopPropagation();
              setReleaseCandidate(candidate);
            }}
          >
            异常放行
          </Button>
        </Space>
      ),
    },
  ];

  if (!taskId) {
    return (
      <div className="knowledge-page collection-manage-page">
        <Alert
          type="warning"
          showIcon
          message="任务 ID 缺失"
          description="无法打开 PC 采集管理台，请返回实操采集首页后重新进入。"
        />
        <Button icon={<ArrowLeftOutlined />} onClick={goBackToList}>
          返回实操采集首页
        </Button>
      </div>
    );
  }

  const detail = detailQuery.data;

  return (
    <div className="knowledge-page collection-manage-page">
      <div className="page-heading knowledge-heading collection-manage-heading">
        <div>
          <div className="page-title-line">
            <div className="page-title-icon"><ClusterOutlined /></div>
            <div>
              <Typography.Title level={1}>实操采集管理台</Typography.Title>
              <Typography.Text type="secondary">
                {task?.taskName || '正在加载任务'} · 当前任务 ID：{taskId}
              </Typography.Text>
            </div>
          </div>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={goBackToList}>
            返回实操采集首页
          </Button>
          <Button icon={<ReloadOutlined />} loading={workbenchQuery.isFetching || exceptionsQuery.isFetching} onClick={refreshAll}>
            刷新
          </Button>
        </Space>
      </div>

      {workbenchQuery.isError ? (
        <Alert type="error" showIcon message="管理台数据加载失败，请检查任务和网络后重试" />
      ) : null}

      <Card className="collection-manage-task-card" loading={workbenchQuery.isLoading}>
        <Descriptions column={{ xs: 1, sm: 2, lg: 5 }} size="small">
          <Descriptions.Item label="任务名称">{task?.taskName || '-'}</Descriptions.Item>
          <Descriptions.Item label="任务编号">{task?.taskNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="企业">{task?.projectName || '-'}</Descriptions.Item>
          <Descriptions.Item label="工种/等级">{task?.workType || '-'} / {task?.level || '-'}</Descriptions.Item>
          <Descriptions.Item label="认定日期">{task?.examDate || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <div className="collection-manage-stats">
        <Card><Statistic title="总人数" value={stats.candidateTotal} /></Card>
        <Card><Statistic title="已发放" value={stats.qrIssuedCount} /></Card>
        <Card><Statistic title="已到场" value={stats.arrivedCount} /></Card>
        <Card><Statistic title="已入队" value={stats.queuedCount} /></Card>
        <Card><Statistic title="采集中" value={stats.collectingCount} /></Card>
        <Card><Statistic title="已完成" value={stats.completedCount} /></Card>
        <Card><Statistic title="待采集" value={stats.pendingCollectCount} /></Card>
        <Card><Statistic title="异常" value={stats.exceptionCount} valueStyle={{ color: stats.exceptionCount ? '#cf1322' : undefined }} /></Card>
      </div>

      <div className="collection-manage-grid">
        <Card
          className="collection-manage-candidate-card"
          title="当前任务考生状态"
          extra={<Typography.Text type="secondary">共 {filteredCandidates.length} / {candidates.length} 人</Typography.Text>}
        >
          <Space wrap className="collection-manage-table-toolbar">
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索姓名 / 考号 / 身份证 / 单位"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              style={{ width: 280 }}
            />
            <Select
              value={overallFilter}
              options={overallFilterOptions}
              onChange={setOverallFilter}
              style={{ width: 140 }}
            />
            <Select
              value={exceptionFilter}
              options={exceptionFilterOptions}
              onChange={setExceptionFilter}
              style={{ width: 140 }}
            />
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              disabled={!selectedCandidateIds.length}
              loading={exportCollectionFormsMutation.isPending}
              onClick={() => {
                if (!selectedCandidateIds.length) {
                  message.warning('请先选择需要生成采集表的考生。');
                  return;
                }
                exportCollectionFormsMutation.mutate(selectedCandidateIds);
              }}
              >
              生成 Excel
            </Button>
            <Button
              icon={<DownloadOutlined />}
              disabled={!selectedCandidateIds.length}
              loading={exportCollectionPdfMutation.isPending}
              onClick={() => {
                if (!selectedCandidateIds.length) {
                  message.warning('请先选择需要生成打印版 PDF 的考生。');
                  return;
                }
                exportCollectionPdfMutation.mutate(selectedCandidateIds);
              }}
            >
              生成打印版 PDF
            </Button>
          </Space>
          <Table<CandidateCollectionItemVO>
            rowKey={(candidate) => String(candidate.candidateId)}
            columns={candidateColumns}
            dataSource={filteredCandidates}
            loading={workbenchQuery.isFetching}
            pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => `共 ${total} 名考生` }}
            scroll={{ x: 1540 }}
            locale={{ emptyText: <Empty description="暂无考生" /> }}
            rowSelection={{
              selectedRowKeys: selectedCandidateIds.map(String),
              onChange: (keys) => setSelectedCandidateIds(keys.map(String)),
              getCheckboxProps: (candidate) => ({
                disabled: candidate.collectionEligible !== true || candidate.overallStatus !== 'COMPLETED',
                title: candidate.collectionEligible !== true ? '资料未完成，不能生成采集表' : '实操采集未完成，不能生成采集表',
              }),
            }}
            onRow={(candidate) => ({
              onClick: () => confirmOpenCandidateCollection(candidate),
            })}
          />
        </Card>

        <Space direction="vertical" size={12} className="collection-manage-side">
          <Card title={<Space><MonitorOutlined />现场监控</Space>}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="当前采集考生">
                {workbench?.currentCandidate?.name || '暂无'}
              </Descriptions.Item>
              <Descriptions.Item label="当前队列人数">{workbench?.queue?.length || 0}</Descriptions.Item>
              <Descriptions.Item label="平板在线状态">待接入</Descriptions.Item>
              <Descriptions.Item label="最近更新时间">{lastUpdated}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title={<Space><QrcodeOutlined />平板端入口管理</Space>}>
            <Alert
              type="info"
              showIcon
              message="该页面用于平板端现场采集，PC 端仅用于调试或预览。"
              className="collection-manage-inline-alert"
            />
            <Typography.Paragraph copyable={{ text: tabletUrl }} className="collection-tablet-url">
              {tabletUrl || '-'}
            </Typography.Paragraph>
            <Space wrap>
              <Button icon={<CopyOutlined />} onClick={handleCopyTabletUrl}>
                复制平板端链接
              </Button>
              <Button icon={<LinkOutlined />} onClick={() => window.open(tabletUrl, '_blank', 'noopener,noreferrer')}>
                打开平板端页面（调试）
              </Button>
              <Button
                icon={<QrcodeOutlined />}
                loading={generateQrMutation.isPending}
                onClick={() => generateQrMutation.mutate()}
              >
                生成二维码
              </Button>
              <Button
                icon={<DownloadOutlined />}
                loading={exportQrMutation.isPending}
                onClick={() => exportQrMutation.mutate()}
              >
                二维码导出
              </Button>
            </Space>
          </Card>

          <Card
            title={<Space><ExclamationCircleOutlined />异常概览</Space>}
            extra={<Button type="link" onClick={() => setExceptionModalOpen(true)}>查看异常记录</Button>}
          >
            <div className="collection-exception-summary">
              <Statistic title="异常人数" value={stats.exceptionCount} />
              <Statistic title="待补采人数" value={stats.pendingRetryCount} />
              <Statistic title="信息异常人数" value={stats.infoErrorCount} />
            </div>
            <List<CollectionExceptionVO>
              size="small"
              loading={exceptionsQuery.isFetching}
              dataSource={recentExceptions}
              locale={{ emptyText: <Empty description="暂无最近异常" /> }}
              renderItem={(item) => (
                <List.Item>
                  <Space direction="vertical" size={2}>
                    <Space wrap>
                      <Typography.Text strong>{item.candidateName || `考生 ${item.candidateId}`}</Typography.Text>
                      {statusTag(item.exceptionStatus, item.exceptionStatusLabel, CollectionExceptionStatusText)}
                    </Space>
                    <Typography.Text type="secondary">{item.reason || '-'}</Typography.Text>
                  </Space>
                </List.Item>
              )}
            />
          </Card>
        </Space>
      </div>

      <Drawer
        title="考生详情"
        open={Boolean(detailCandidate)}
        width={620}
        destroyOnHidden
        onClose={() => setDetailCandidate(null)}
      >
        {detailCandidate ? (
          <Space direction="vertical" size={16} className="collection-detail-drawer">
            <Descriptions title="基础信息" column={1} bordered size="small">
              <Descriptions.Item label="考生 ID">{fallbackText(detail?.candidateId || detailCandidate.candidateId)}</Descriptions.Item>
              <Descriptions.Item label="姓名">{detail?.name || detailCandidate.name || '-'}</Descriptions.Item>
              <Descriptions.Item label="考号">{detail?.admissionNo || detailCandidate.admissionNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="身份证号">
                {detail?.maskedIdCardNo || detailCandidate.maskedIdCardNo || (detailCandidate.idCardLast4 ? `后四位 ${detailCandidate.idCardLast4}` : '-')}
              </Descriptions.Item>
              <Descriptions.Item label="单位">{detail?.unitName || detailCandidate.unitName || '-'}</Descriptions.Item>
              <Descriptions.Item label="工种/等级">
                {detail?.workType || detailCandidate.workType || '-'} / {detail?.level || detailCandidate.level || '-'}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="采集状态" column={1} bordered size="small">
              <Descriptions.Item label="采集状态">
                {statusTag(detail?.overallStatus || detailCandidate.overallStatus, detail?.overallStatusLabel || detailCandidate.overallStatusLabel, CollectionOverallStatusText)}
              </Descriptions.Item>
              <Descriptions.Item label="指纹状态">
                {statusTag(detail?.fingerprintStatus || detailCandidate.fingerprintStatus, detail?.fingerprintStatusLabel || detailCandidate.fingerprintStatusLabel, FingerprintStatusText)}
              </Descriptions.Item>
              <Descriptions.Item label="签名状态">
                {statusTag(detail?.signatureStatus || detailCandidate.signatureStatus, detail?.signatureStatusLabel || detailCandidate.signatureStatusLabel, SignatureStatusText)}
              </Descriptions.Item>
              <Descriptions.Item label="队列状态">
                {statusTag(detail?.queueStatus || detailCandidate.queueStatus, detail?.queueStatusLabel || detailCandidate.queueStatusLabel, CollectionQueueStatusText)}
              </Descriptions.Item>
              <Descriptions.Item label="异常状态">
                {statusTag(detail?.exceptionStatus || detailCandidate.exceptionStatus, detail?.exceptionStatusLabel || detailCandidate.exceptionStatusLabel, CollectionExceptionStatusText)}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="指纹信息" column={1} bordered size="small">
              <Descriptions.Item label="最近指纹">
                {detail?.latestFingerprint
                  ? statusTag(detail.latestFingerprint.collectStatus, detail.latestFingerprint.collectStatusLabel, FingerprintCollectStatusText)
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="质量分">{fallbackText(detail?.latestFingerprint?.qualityScore)}</Descriptions.Item>
              <Descriptions.Item label="设备">{detail?.latestFingerprint?.deviceModel || detail?.latestFingerprint?.deviceSn || '-'}</Descriptions.Item>
              <Descriptions.Item label="采集时间">{formatDateTime(detail?.latestFingerprint?.collectedAt)}</Descriptions.Item>
              <Descriptions.Item label="指纹文件 ID">{fallbackText(detail?.latestFingerprint?.fingerprintImageFileId)}</Descriptions.Item>
            </Descriptions>

            <Descriptions title="签名信息" column={1} bordered size="small">
              <Descriptions.Item label="签名人">{detail?.latestSignature?.signerName || '-'}</Descriptions.Item>
              <Descriptions.Item label="签名状态">
                {detail?.latestSignature
                  ? statusTag(detail.latestSignature.recordStatus, detail.latestSignature.recordStatusLabel, SignatureRecordStatusText)
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="签名文件 ID">{fallbackText(detail?.latestSignature?.signatureFileId)}</Descriptions.Item>
              <Descriptions.Item label="签名 Hash">{shortHash(detail?.latestSignature?.signatureHash)}</Descriptions.Item>
              <Descriptions.Item label="签名时间">{formatDateTime(detail?.latestSignature?.signedAt)}</Descriptions.Item>
            </Descriptions>

            <Descriptions title="时间信息" column={1} bordered size="small">
              <Descriptions.Item label="到场时间">{formatDateTime(detail?.arrivedAt)}</Descriptions.Item>
              <Descriptions.Item label="开始采集时间">{formatDateTime(detail?.collectingStartedAt)}</Descriptions.Item>
              <Descriptions.Item label="完成时间">{formatDateTime(detail?.completedAt)}</Descriptions.Item>
            </Descriptions>

            <List<CollectionExceptionVO>
              header={<strong>异常记录</strong>}
              loading={detailQuery.isFetching}
              dataSource={detail?.exceptions || []}
              locale={{ emptyText: <Empty description="暂无异常记录" /> }}
              renderItem={(item) => (
                <List.Item>
                  <Space direction="vertical" size={2}>
                    <Space wrap>
                      {statusTag(item.exceptionStatus, item.exceptionStatusLabel, CollectionExceptionStatusText)}
                      <Tag>{item.exceptionTypeLabel || item.exceptionType}</Tag>
                    </Space>
                    <Typography.Text type="secondary">{item.reason || '-'}</Typography.Text>
                    <Typography.Text type="secondary">{formatDateTime(item.handledAt || item.createdAt)}</Typography.Text>
                  </Space>
                </List.Item>
              )}
            />
          </Space>
        ) : null}
      </Drawer>

      <Drawer
        className="collection-candidate-drawer"
        title={collectionCandidate ? `${collectionCandidate.name} · 考生采集` : '考生采集'}
        open={Boolean(collectionCandidate)}
        width={960}
        destroyOnHidden
        onClose={clearCollectionCandidate}
      >
        {collectionCandidate ? (
          <CurrentCandidateCollectionPanel
            taskId={taskId}
            candidate={collectionCandidate}
            collectionContext={collectionContext}
            onRefresh={refreshCollectionCandidate}
            onCancelCurrent={clearCollectionCandidate}
            onClearCurrent={clearCollectionCandidate}
            onAfterStatusChanged={refreshAll}
          />
        ) : null}
      </Drawer>

      <InfoErrorModal
        open={Boolean(infoErrorCandidate)}
        loading={markInfoErrorMutation.isPending}
        onCancel={() => setInfoErrorCandidate(null)}
        onSubmit={(reason) => markInfoErrorMutation.mutate(reason)}
      />

      <ExceptionReleaseModal
        open={Boolean(releaseCandidate)}
        loading={releaseMutation.isPending}
        onCancel={() => setReleaseCandidate(null)}
        onSubmit={(values) => {
          if (!releaseCandidate) return;
          releaseMutation.mutate({
            taskId,
            candidateId: releaseCandidate.candidateId,
            exceptionType: values.exceptionType,
            releaseScope: values.releaseScope,
            reason: values.reason,
          });
        }}
      />

      <Modal
        title={task ? `${task.taskName} · 异常记录` : '异常记录'}
        open={exceptionModalOpen}
        width={920}
        footer={null}
        destroyOnHidden
        onCancel={() => setExceptionModalOpen(false)}
      >
        <CollectionExceptionPage taskId={taskId} />
      </Modal>
    </div>
  );
}
