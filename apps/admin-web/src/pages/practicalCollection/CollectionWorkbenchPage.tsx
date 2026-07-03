import { ArrowLeftOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Modal, Spin, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import {
  completeCollection,
  exceptionRelease,
  getCollectionDetail,
  getCollectionWorkbench,
  joinQueue,
  markInfoError,
  markPendingRetry,
  removeQueue,
  startNext,
} from '../../api/certificationPracticalCollection';
import { CandidateConfirmCard } from '../../components/certification/collection/CandidateConfirmCard';
import { CandidateSearchBox } from '../../components/certification/collection/CandidateSearchBox';
import { CandidateStatusList } from '../../components/certification/collection/CandidateStatusList';
import { CollectionQueuePanel } from '../../components/certification/collection/CollectionQueuePanel';
import { CollectionStatsBar } from '../../components/certification/collection/CollectionStatsBar';
import { CurrentCandidateCollectionPanel } from '../../components/certification/collection/CurrentCandidateCollectionPanel';
import { ExceptionReleaseModal } from '../../components/certification/collection/ExceptionReleaseModal';
import { InfoErrorModal } from '../../components/certification/collection/InfoErrorModal';
import { PendingRetryModal } from '../../components/certification/collection/PendingRetryModal';
import { CollectionDetailPage } from '../certification/collection/CollectionDetailPage';
import { getFingerprintAdapter } from '../../services/fingerprint/FingerprintAdapterFactory';
import type { FingerprintDeviceStatus } from '../../services/fingerprint/FingerprintAdapter';
import type {
  CandidateCollectionDetailVO,
  CandidateCollectionItemVO,
  CandidateMatchVO,
  CollectionExceptionType,
  CollectionMatchMethod,
  CollectionReleaseScope,
  QueueItemVO,
  SnowflakeId,
} from '../../types/certificationPracticalCollection';

interface CollectionWorkbenchPageProps {
  taskId: SnowflakeId;
}

function itemToMatch(candidate: CandidateCollectionItemVO): CandidateMatchVO {
  return {
    candidateId: candidate.candidateId,
    taskId: candidate.taskId,
    name: candidate.name,
    idCardLast4: candidate.idCardLast4,
    maskedIdCardNo: candidate.maskedIdCardNo,
    unitName: candidate.unitName,
    workType: candidate.workType,
    level: candidate.level,
    admissionNo: candidate.admissionNo,
    currentStatus: candidate.overallStatus,
    currentStatusLabel: candidate.overallStatusLabel,
    alreadyQueued: candidate.queueStatus === 'QUEUED' || candidate.queueStatus === 'COLLECTING',
    queueNo: candidate.queueNo,
  };
}

export function CollectionWorkbenchPage({ taskId }: CollectionWorkbenchPageProps) {
  const queryClient = useQueryClient();
  const [online, setOnline] = useState(navigator.onLine);
  const [deviceStatus, setDeviceStatus] = useState<FingerprintDeviceStatus>();
  const [confirmCandidate, setConfirmCandidate] = useState<CandidateMatchVO | null>(null);
  const [detail, setDetail] = useState<CandidateCollectionDetailVO | null>(null);
  const [pendingRetryOpen, setPendingRetryOpen] = useState(false);
  const [infoErrorOpen, setInfoErrorOpen] = useState(false);
  const [infoErrorCandidateId, setInfoErrorCandidateId] = useState<SnowflakeId>();
  const [exceptionOpen, setExceptionOpen] = useState(false);

  const workbenchQuery = useQuery({
    queryKey: ['practical-collection-workbench', taskId],
    queryFn: () => getCollectionWorkbench(taskId),
    refetchInterval: online ? 10000 : false,
  });

  const refreshWorkbench = () => {
    queryClient.invalidateQueries({ queryKey: ['practical-collection-workbench', taskId] });
  };

  const deviceMutation = useMutation({
    mutationFn: () => getFingerprintAdapter().checkDevice(),
    onSuccess: setDeviceStatus,
  });

  useEffect(() => {
    deviceMutation.mutate();
    const onOnline = () => setOnline(true);
    const onOffline = () => setOnline(false);
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const startNextMutation = useMutation({
    mutationFn: () => startNext({ taskId }),
    onSuccess: (candidate) => {
      if (!candidate) {
        message.info('当前暂无排队考生');
      } else {
        message.success(`已开始采集：${candidate.name}`);
      }
      refreshWorkbench();
    },
  });

  const joinQueueMutation = useMutation({
    mutationFn: (candidate: CandidateMatchVO) => joinQueue({
      taskId,
      candidateId: candidate.candidateId,
      matchMethod: (candidate.matchMethod || 'MANUAL') as CollectionMatchMethod,
    }),
    onSuccess: () => {
      message.success('已加入现场队列');
      setConfirmCandidate(null);
      refreshWorkbench();
    },
  });

  const collectNowMutation = useMutation({
    mutationFn: async (candidate: CandidateMatchVO) => {
      await joinQueue({
        taskId,
        candidateId: candidate.candidateId,
        matchMethod: (candidate.matchMethod || 'MANUAL') as CollectionMatchMethod,
      });
      return startNext({ taskId });
    },
    onSuccess: (candidate) => {
      message.success(candidate ? `已开始采集：${candidate.name}` : '已入队，请点击开始下一位');
      setConfirmCandidate(null);
      refreshWorkbench();
    },
  });

  const removeQueueMutation = useMutation({
    mutationFn: (queue: QueueItemVO) => removeQueue({
      taskId,
      queueId: queue.queueId,
      removeReason: '现场队列调整',
    }),
    onSuccess: () => {
      message.success('已移出队列');
      refreshWorkbench();
    },
  });

  const detailMutation = useMutation({
    mutationFn: (candidateId: SnowflakeId) => getCollectionDetail(taskId, candidateId),
    onSuccess: setDetail,
  });

  const completeMutation = useMutation({
    mutationFn: () => completeCollection({
      taskId,
      candidateId: workbenchQuery.data!.currentCandidate!.candidateId,
    }),
    onSuccess: (result) => {
      message.success(result.message || '采集已完成');
      refreshWorkbench();
    },
  });

  const pendingRetryMutation = useMutation({
    mutationFn: (reason: string) => markPendingRetry({
      taskId,
      candidateId: workbenchQuery.data!.currentCandidate!.candidateId,
      reason,
    }),
    onSuccess: () => {
      message.success('已标记待补采');
      setPendingRetryOpen(false);
      refreshWorkbench();
    },
  });

  const infoErrorMutation = useMutation({
    mutationFn: (reason: string) => markInfoError({
      taskId,
      candidateId: infoErrorCandidateId || workbenchQuery.data!.currentCandidate!.candidateId,
      reason,
    }),
    onSuccess: () => {
      message.success('已标记信息异常');
      setInfoErrorOpen(false);
      setInfoErrorCandidateId(undefined);
      setConfirmCandidate(null);
      refreshWorkbench();
    },
  });

  const exceptionMutation = useMutation({
    mutationFn: (values: {
      exceptionType: CollectionExceptionType;
      releaseScope: CollectionReleaseScope;
      reason: string;
    }) => exceptionRelease({
      taskId,
      candidateId: workbenchQuery.data!.currentCandidate!.candidateId,
      exceptionType: values.exceptionType,
      releaseScope: values.releaseScope,
      reason: values.reason,
      deviceSn: deviceStatus?.deviceSn,
      deviceStatus: deviceStatus?.status,
    }),
    onSuccess: () => {
      message.success('异常放行已记录');
      setExceptionOpen(false);
      refreshWorkbench();
    },
  });

  const workbench = workbenchQuery.data;

  return (
    <div className="practical-collection-shell">
      <div className="collection-workbench">
        <div className="collection-topline">
          <Button icon={<ArrowLeftOutlined />} onClick={() => window.location.assign('/practical-collection')}>
            任务入口
          </Button>
          <Button icon={<ReloadOutlined />} loading={workbenchQuery.isFetching} onClick={() => workbenchQuery.refetch()}>
            刷新工作台
          </Button>
        </div>

        {workbenchQuery.isLoading ? (
          <div className="collection-loading"><Spin size="large" /></div>
        ) : null}

        {workbenchQuery.isError ? (
          <Alert type="error" showIcon message="工作台数据加载失败，请检查任务和网络后重试" />
        ) : null}

        {workbench ? (
          <>
            <CollectionStatsBar
              task={workbench.task}
              stats={workbench.stats}
              deviceStatus={deviceStatus}
              online={online}
              checkingDevice={deviceMutation.isPending}
              onCheckDevice={() => deviceMutation.mutate()}
            />

            <CandidateSearchBox
              taskId={taskId}
              onCandidateMatched={setConfirmCandidate}
              onCandidatesFound={() => undefined}
              onAfterStatusChanged={refreshWorkbench}
            />

            <div className="collection-workbench-grid">
              <CandidateStatusList
                candidates={workbench.candidates}
                onSelect={(candidate) => setConfirmCandidate(itemToMatch(candidate))}
              />
              <CurrentCandidateCollectionPanel
                taskId={taskId}
                candidate={workbench.currentCandidate}
                loading={
                  completeMutation.isPending
                  || pendingRetryMutation.isPending
                  || infoErrorMutation.isPending
                  || exceptionMutation.isPending
                }
                onRefresh={refreshWorkbench}
                onComplete={() => completeMutation.mutate()}
                onPendingRetry={() => setPendingRetryOpen(true)}
                onInfoError={() => {
                  setInfoErrorCandidateId(workbench.currentCandidate?.candidateId);
                  setInfoErrorOpen(true);
                }}
                onExceptionRelease={() => setExceptionOpen(true)}
              />
              <CollectionQueuePanel
                queue={workbench.queue}
                loading={startNextMutation.isPending || removeQueueMutation.isPending}
                onStartNext={() => startNextMutation.mutate()}
                onRemove={(queue) => removeQueueMutation.mutate(queue)}
                onViewDetail={(queue) => detailMutation.mutate(queue.candidateId)}
              />
            </div>

            <div className="collection-bottom-actions">
              <Button
                type="primary"
                size="large"
                loading={startNextMutation.isPending}
                onClick={() => startNextMutation.mutate()}
              >
                开始采集下一位
              </Button>
              <Button
                size="large"
                loading={completeMutation.isPending}
                disabled={!workbench.currentCandidate}
                onClick={() => completeMutation.mutate()}
              >
                完成当前考生采集
              </Button>
              <Button size="large" disabled={!workbench.currentCandidate} onClick={() => setPendingRetryOpen(true)}>
                标记待补采
              </Button>
              <Button
                size="large"
                danger
                disabled={!workbench.currentCandidate}
                onClick={() => {
                  setInfoErrorCandidateId(workbench.currentCandidate?.candidateId);
                  setInfoErrorOpen(true);
                }}
              >
                信息异常
              </Button>
              <Button size="large" danger disabled={!workbench.currentCandidate} onClick={() => setExceptionOpen(true)}>
                异常放行
              </Button>
            </div>
          </>
        ) : null}

        <CandidateConfirmCard
          open={Boolean(confirmCandidate)}
          candidate={confirmCandidate}
          loading={joinQueueMutation.isPending || collectNowMutation.isPending}
          onCancel={() => setConfirmCandidate(null)}
          onJoinQueue={(candidate) => joinQueueMutation.mutate(candidate)}
          onCollectNow={(candidate) => collectNowMutation.mutate(candidate)}
          onViewDetail={(candidate) => detailMutation.mutate(candidate.candidateId)}
          onInfoError={(candidate) => {
            setInfoErrorCandidateId(candidate.candidateId);
            setInfoErrorOpen(true);
          }}
        />

        <Modal
          title="采集详情"
          open={Boolean(detail)}
          width={860}
          footer={null}
          destroyOnHidden
          onCancel={() => setDetail(null)}
        >
          {detail ? <CollectionDetailPage taskId={taskId} candidateId={detail.candidateId} /> : null}
        </Modal>

        <PendingRetryModal
          open={pendingRetryOpen}
          loading={pendingRetryMutation.isPending}
          onCancel={() => setPendingRetryOpen(false)}
          onSubmit={(reason) => pendingRetryMutation.mutate(reason)}
        />
        <InfoErrorModal
          open={infoErrorOpen}
          loading={infoErrorMutation.isPending}
          onCancel={() => {
            setInfoErrorOpen(false);
            setInfoErrorCandidateId(undefined);
          }}
          onSubmit={(reason) => infoErrorMutation.mutate(reason)}
        />
        <ExceptionReleaseModal
          open={exceptionOpen}
          loading={exceptionMutation.isPending}
          onCancel={() => setExceptionOpen(false)}
          onSubmit={(values) => exceptionMutation.mutate(values)}
        />
      </div>
    </div>
  );
}
