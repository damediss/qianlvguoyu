import {

  PlayCircleOutlined,

  ReloadOutlined,

  SafetyCertificateOutlined,

  ScanOutlined,

} from '@ant-design/icons';

import { Alert, Button, Card, Descriptions, Input, Modal, Space, Spin, Tag, Typography, message } from 'antd';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useEffect, useMemo, useState } from 'react';

import {

  getCollectionDetail,

  getCollectionWorkbench,

  openCandidateCollection,

  cancelCurrentCollection,

  scanMatchCurrent,

} from '../../api/certificationPracticalCollection';

import { CollectionTaskSelectorBar } from '../../components/certification/collection/CollectionTaskSelectorBar';

import { CollectionTaskSwitchModal } from '../../components/certification/collection/CollectionTaskSwitchModal';

import { CurrentCandidateCollectionPanel } from '../../components/certification/collection/CurrentCandidateCollectionPanel';

import { getFingerprintAdapter } from '../../services/fingerprint/FingerprintAdapterFactory';

import { getScanAdapter, hasAndroidScanBridge } from '../../services/scan/ScanAdapterFactory';

import { parseCollectionToken } from '../../services/scan/scanToken';

import { parseScanQRCodeResult } from '../../services/scan/parseScanQRCodeResult';

import {

  hasCollectedCollectionData,

  normalizeActiveCollectionCandidate,

  toCandidateCollectionContext,

} from '../../utils/collectionCandidate';

import type { FingerprintDeviceStatus } from '../../services/fingerprint/FingerprintAdapter';

import type {

  CandidateCollectionContext,

  CandidateCollectionDetailVO,

  CandidateCollectionItemVO,

  CollectionEntrySource,

  ScanMatchResultVO,

  SnowflakeId,

} from '../../types/certificationPracticalCollection';

import { CollectionOverallStatusText } from '../../types/certificationPracticalCollection';



interface CollectionWorkbenchPageProps {

  taskId: SnowflakeId;

  onTaskChange: (taskId: SnowflakeId) => void;

  taskSelectorVisible?: boolean;

  onTaskSelectorVisibleChange?: (visible: boolean) => void;

}



type TabletCandidateStatus = 'CURRENT' | 'COMPLETED' | 'PENDING' | 'EXCEPTION';



const tabletStatusText: Record<TabletCandidateStatus, string> = {

  CURRENT: '当前',

  COMPLETED: '已完成',

  PENDING: '未采集',

  EXCEPTION: '异常',

};



const tabletStatusColor: Record<TabletCandidateStatus, string> = {

  CURRENT: 'blue',

  COMPLETED: 'green',

  PENDING: 'default',

  EXCEPTION: 'red',

};



function isSameId(left?: SnowflakeId | null, right?: SnowflakeId | null) {

  return left !== undefined && left !== null && right !== undefined && right !== null && String(left) === String(right);

}



function getTabletCandidateStatus(

  candidate: CandidateCollectionItemVO,

  currentCandidateId?: SnowflakeId | null,

): TabletCandidateStatus {

  if (isSameId(candidate.candidateId, currentCandidateId)) {

    return 'CURRENT';

  }

  if (candidate.overallStatus === 'COMPLETED') {

    return 'COMPLETED';

  }

  if (

    candidate.overallStatus === 'PENDING_RETRY'

    || candidate.overallStatus === 'INFO_ERROR'

    || candidate.overallStatus === 'EXCEPTION_RELEASED'

    || candidate.fingerprintStatus === 'FAILED'

    || candidate.fingerprintStatus === 'EXCEPTION_RELEASED'

    || candidate.signatureStatus === 'FAILED'

    || candidate.signatureStatus === 'EXCEPTION_RELEASED'

    || (candidate.exceptionStatus && candidate.exceptionStatus !== 'NONE')

  ) {

    return 'EXCEPTION';

  }

  if (candidate.overallStatus === 'COLLECTING') {

    return 'CURRENT';

  }

  return 'PENDING';

}



function configuredScanMode() {

  return String(import.meta.env.VITE_SCAN_MODE || 'MANUAL').toUpperCase();

}



export function CollectionWorkbenchPage({

  taskId,

  onTaskChange,

  taskSelectorVisible: taskSelectorVisibleProp,

  onTaskSelectorVisibleChange,

}: CollectionWorkbenchPageProps) {

  const queryClient = useQueryClient();

  const [online, setOnline] = useState(navigator.onLine);

  const [deviceStatus, setDeviceStatus] = useState<FingerprintDeviceStatus>();

  const [activeCandidate, setActiveCandidate] = useState<CandidateCollectionDetailVO | null>(null);

  const [activeCollectionContext, setActiveCollectionContext] = useState<CandidateCollectionContext | null>(null);

  const [candidateKeyword, setCandidateKeyword] = useState('');

  const [debugScanOpen, setDebugScanOpen] = useState(false);

  const [debugScanToken, setDebugScanToken] = useState('');

  const [internalTaskSelectorVisible, setInternalTaskSelectorVisible] = useState(false);

  const [taskSwitching, setTaskSwitching] = useState(false);



  const taskSelectorVisible = taskSelectorVisibleProp ?? internalTaskSelectorVisible;

  const setTaskSelectorVisible = onTaskSelectorVisibleChange ?? setInternalTaskSelectorVisible;



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



  const setCurrentCollectionCandidate = (

    candidate: CandidateCollectionDetailVO,

    entrySource: CollectionEntrySource,

  ) => {

    const normalized = normalizeActiveCollectionCandidate(candidate);

    setActiveCandidate(normalized);

    setActiveCollectionContext(toCandidateCollectionContext(normalized, entrySource));

  };



  const clearCurrentCandidate = () => {

    setActiveCandidate(null);

    setActiveCollectionContext(null);

  };



  const releaseCurrentCollectingCandidate = async (candidate: CandidateCollectionDetailVO) => {

    if (candidate.overallStatus !== 'COLLECTING') {

      clearCurrentCandidate();

      return;

    }

    try {

      await cancelCurrentCollection(taskId, candidate.candidateId);

      clearCurrentCandidate();

      refreshWorkbench();

      message.success('已取消当前采集');

    } catch (error) {

      message.error(error instanceof Error ? error.message : '取消当前采集失败，请稍后重试');

      throw error;

    }

  };



  const handleCancelCurrent = () => {

    if (!activeCandidate) {

      return;

    }

    if (activeCandidate.overallStatus === 'COLLECTING') {

      Modal.confirm({

        title: '取消当前采集？',

        content: hasCollectedCollectionData(activeCandidate)

          ? '取消后将释放该考生的「采集中」占用，已提交的指纹/签名记录会保留，可稍后再继续采集。'

          : '取消后将释放该考生的「采集中」占用，可切换到其他考生开始采集。',

        okText: '确认取消',

        cancelText: '继续采集',

        onOk: () => releaseCurrentCollectingCandidate(activeCandidate),

      });

      return;

    }

    if (hasCollectedCollectionData(activeCandidate)) {

      Modal.confirm({

        title: '取消当前采集？',

        content: '当前考生已有采集记录，取消后仅清空本机当前考生，不会删除已提交记录。',

        okText: '确认取消',

        cancelText: '继续采集',

        onOk: clearCurrentCandidate,

      });

      return;

    }

    clearCurrentCandidate();

  };



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



  const refreshActiveCandidate = async () => {

    refreshWorkbench();

    if (!activeCandidate) {

      return;

    }

    try {

      const detail = await getCollectionDetail(taskId, activeCandidate.candidateId);

      setCurrentCollectionCandidate(detail, activeCollectionContext?.entrySource || 'QR_SCAN');

    } catch {

      message.warning('当前考生信息刷新失败，请稍后重试');

    }

  };



  const scanMatchMutation = useMutation({

    mutationFn: async (manualToken?: string): Promise<ScanMatchResultVO | null> => {

      let token = parseCollectionToken(manualToken);

      if (!token) {

        const scanResult = await getScanAdapter().startScan();

        if (!scanResult.success) {

          if (scanResult.code === 'CANCELLED') {

            return null;

          }

          if (scanResult.code === 'PERMISSION_DENIED') {

            throw new Error('未授予摄像头权限，请在系统设置中开启后重试');

          }

          throw new Error(scanResult.message || '扫码失败，请重试');

        }

        const parsed = scanResult.parsed || parseScanQRCodeResult(scanResult.rawText);

        token = parseCollectionToken(scanResult.token || parsed.token || scanResult.rawText);

      }

      if (!token) {

        throw new Error('未识别到有效的二维码 token');

      }

      return scanMatchCurrent(taskId, token);

    },

    onSuccess: (result) => {

      if (!result) {

        return;

      }

      setDebugScanOpen(false);

      setDebugScanToken('');

      if (result.alreadyCompleted) {

        Modal.confirm({

          title: '该考生已完成采集',

          content: '该考生已完成采集，是否查看采集信息？系统默认不会覆盖已完成的采集数据。',

          okText: '查看采集信息',

          cancelText: '取消',

          onOk: () => {

            setCurrentCollectionCandidate(result.currentCandidate, 'QR_SCAN');

            refreshWorkbench();

          },

        });

        return;

      }

      setCurrentCollectionCandidate(result.currentCandidate, 'QR_SCAN');

      message.success(`已匹配考生：${result.currentCandidate.name}`);

      refreshWorkbench();

    },

    onError: (error) => {

      const errorMessage = error instanceof Error ? error.message : '扫码匹配失败，请重试';

      if (errorMessage.includes('当前已有正在采集')) {

        Modal.warning({

          title: '扫码入口冲突',

          content: errorMessage,

          okText: '我知道了',

        });

        return;

      }

      if (

        errorMessage.includes('不属于当前任务')

        || errorMessage.includes('采集二维码无效')

      ) {

        message.error('该考生不属于当前采集任务，请切换任务后重试');

        return;

      }

      if (errorMessage.includes('NOT_FOUND')) {

        message.error('未匹配到考生，请确认二维码是否正确');

        return;

      }

      message.error(errorMessage);

    },

  });



  const openCandidateMutation = useMutation({

    mutationFn: (candidate: CandidateCollectionItemVO) => openCandidateCollection(taskId, candidate.candidateId),

    onSuccess: (detail) => {

      setCurrentCollectionCandidate(detail, 'CANDIDATE_ROW');

      message.success(`已进入考生采集：${detail.name}`);

      refreshWorkbench();

    },

    onError: (error) => {

      message.error(error instanceof Error ? error.message : '打开考生采集失败，请稍后重试');

    },

  });



  const workbench = workbenchQuery.data;

  const currentCandidateId = activeCandidate?.candidateId;

  const deviceColor = deviceStatus?.connected ? 'success' : deviceStatus?.status === 'ERROR' ? 'error' : 'warning';

  const filteredCandidates = useMemo(() => {

    const keyword = candidateKeyword.trim().toLowerCase();

    if (!keyword) {

      return workbench?.candidates || [];

    }

    return (workbench?.candidates || []).filter((candidate) => [

      candidate.name,

      candidate.admissionNo,

      candidate.unitName,

      candidate.workType,

      candidate.maskedIdCardNo,

    ].some((value) => String(value || '').toLowerCase().includes(keyword)));

  }, [candidateKeyword, workbench?.candidates]);



  const openScanEntry = () => {

    if (configuredScanMode() === 'MOCK' || hasAndroidScanBridge()) {

      scanMatchMutation.mutate(undefined);

      return;

    }

    setDebugScanToken('');

    setDebugScanOpen(true);

  };



  const handleScanClick = () => {

    const currentCollectingCandidate = getCurrentCollectingCandidate();

    if (currentCollectingCandidate) {

      showCollectingConflict(currentCollectingCandidate);

      return;

    }

    const needsSwitchConfirm = activeCandidate

      && activeCandidate.overallStatus !== 'COMPLETED'

      && hasCollectedCollectionData(activeCandidate);

    if (needsSwitchConfirm) {

      Modal.confirm({

        title: '切换考生确认',

        content: '当前考生采集尚未完成，切换后可能丢失未提交数据，是否确认切换？',

        okText: '确认切换',

        cancelText: '继续当前采集',

        onOk: openScanEntry,

      });

      return;

    }

    openScanEntry();

  };



  const handleDebugScanSubmit = () => {

    const token = parseCollectionToken(debugScanToken);

    if (!token) {

      message.warning('请输入二维码 token');

      return;

    }

    scanMatchMutation.mutate(token);

  };



  const getCurrentCollectingCandidate = (): CandidateCollectionDetailVO | CandidateCollectionItemVO | null => {

    if (activeCandidate?.overallStatus === 'COLLECTING') {

      return activeCandidate;

    }

    if (workbench?.currentCandidate?.overallStatus === 'COLLECTING') {

      return workbench.currentCandidate;

    }

    return (workbench?.candidates || []).find((candidate) => candidate.overallStatus === 'COLLECTING') || null;

  };



  const showCollectingConflict = (

    current: CandidateCollectionDetailVO | CandidateCollectionItemVO,

    selected?: CandidateCollectionItemVO,

  ) => {

    Modal.warning({

      title: '当前已有采集中考生',

      content: (

        <Descriptions column={1} size="small" bordered>

          <Descriptions.Item label="当前考生">{current.name || '-'}</Descriptions.Item>

          <Descriptions.Item label="当前证件号">

            {current.maskedIdCardNo || (current.idCardLast4 ? `后四位 ${current.idCardLast4}` : '-')}

          </Descriptions.Item>

          {selected ? (

            <Descriptions.Item label="本次选择">{selected.name || '-'}</Descriptions.Item>

          ) : null}

          <Descriptions.Item label="处理方式">请先完成、取消当前或标记异常当前考生，刷新确认现场状态后再切换到其他考生。</Descriptions.Item>

        </Descriptions>

      ),

      okText: '我知道了',

    });

  };



  const validateCandidateEntry = (candidate: CandidateCollectionItemVO) => {

    if (candidate.collectionEligible === false) {

      Modal.warning({

        title: '资料未满足实操采集准入条件',

        content: `该考生的信息收集尚未完成。${candidate.missingDocumentCodes?.length ? `缺少：${candidate.missingDocumentCodes.join('、')}。` : ''}${candidate.invalidDocumentCodes?.length ? `资料异常：${candidate.invalidDocumentCodes.join('、')}。` : ''}请先前往“认定筹备—信息收集”补充资料。`,

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

          <Descriptions.Item label="证件号">{candidate.maskedIdCardNo || (candidate.idCardLast4 ? `后四位 ${candidate.idCardLast4}` : '-')}</Descriptions.Item>

          <Descriptions.Item label="当前任务">{workbench?.task.taskName || '-'}</Descriptions.Item>

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



  const performTaskSwitch = (nextTaskId: SnowflakeId) => {

    if (isSameId(nextTaskId, taskId)) {

      setTaskSelectorVisible(false);

      return;

    }

    setTaskSwitching(true);

    clearCurrentCandidate();

    setCandidateKeyword('');

    setDebugScanOpen(false);

    setDebugScanToken('');

    onTaskChange(nextTaskId);

    setTaskSelectorVisible(false);

    setTaskSwitching(false);

    message.success('已切换采集任务');

  };



  const handleTaskSwitchRequest = (nextTaskId: SnowflakeId) => {

    if (isSameId(nextTaskId, taskId)) {

      setTaskSelectorVisible(false);

      return;

    }

    if (activeCandidate && hasCollectedCollectionData(activeCandidate)) {

      Modal.confirm({

        title: '切换任务确认',

        content: '当前考生已有未提交的采集数据，切换任务后本机临时状态将清空，是否继续？',

        okText: '确认切换',

        cancelText: '继续当前采集',

        onOk: () => performTaskSwitch(nextTaskId),

      });

      return;

    }

    if (activeCandidate) {

      Modal.confirm({

        title: '切换任务确认',

        content: '切换任务后将清空当前选中的考生，是否继续？',

        okText: '确认切换',

        cancelText: '取消',

        onOk: () => performTaskSwitch(nextTaskId),

      });

      return;

    }

    performTaskSwitch(nextTaskId);

  };



  return (

    <div className="practical-collection-shell">

      <div className="collection-workbench">

        <div className="collection-tablet-topline">

          <CollectionTaskSelectorBar

            task={workbench?.task}

            loading={workbenchQuery.isLoading}

            onSwitchClick={() => setTaskSelectorVisible(true)}

          />

          <Space className="collection-tablet-status-strip" size={8} wrap>

            <Tag color={online ? 'success' : 'error'}>{online ? '网络在线' : '网络离线'}</Tag>

            <Tag icon={<SafetyCertificateOutlined />} color={deviceColor}>

              {deviceStatus?.message || '指纹设备未检测'}

            </Tag>

            <Button loading={deviceMutation.isPending} onClick={() => deviceMutation.mutate()}>

              检测设备

            </Button>

            <Button icon={<ReloadOutlined />} loading={workbenchQuery.isFetching} onClick={() => workbenchQuery.refetch()}>

              刷新

            </Button>

          </Space>

        </div>



        {workbenchQuery.isLoading ? (

          <div className="collection-loading"><Spin size="large" /></div>

        ) : null}



        {workbenchQuery.isError ? (

          <Alert type="error" showIcon message="工作台数据加载失败，请检查任务和网络后重试" />

        ) : null}



        {workbench ? (

          <div className="collection-tablet-layout">

            <div className="collection-tablet-main">

              <CurrentCandidateCollectionPanel

                taskId={taskId}

                candidate={activeCandidate}

                collectionContext={activeCollectionContext}

                onRefresh={refreshActiveCandidate}

                onCancelCurrent={handleCancelCurrent}

                onClearCurrent={clearCurrentCandidate}

                onAfterStatusChanged={refreshWorkbench}

              />

            </div>



            <div className="collection-tablet-side">

              <Card className="tablet-scan-card" title="快速扫码入口" bordered={false}>

                <Typography.Paragraph type="secondary" className="tablet-scan-hint">

                  扫码匹配仅在当前任务「{workbench.task.taskName}」内生效

                </Typography.Paragraph>

                <Button

                  block

                  type="primary"

                  size="large"

                  icon={<ScanOutlined />}

                  className="tablet-scan-button"

                  loading={scanMatchMutation.isPending}

                  onClick={handleScanClick}

                >

                  扫码匹配考生

                </Button>

              </Card>



              <Card

                className="tablet-candidate-list-card"

                title="当前任务考生名单"

                bordered={false}

                extra={<Tag>{filteredCandidates.length}/{workbench.candidates.length}</Tag>}

              >

                <Input

                  allowClear

                  value={candidateKeyword}

                  placeholder="筛选姓名 / 准考证号"

                  onChange={(event) => setCandidateKeyword(event.target.value)}

                />

                <div className="tablet-candidate-list">

                  {filteredCandidates.map((candidate) => {

                    const status = getTabletCandidateStatus(candidate, currentCandidateId);

                    const statusLabel = status === 'CURRENT' && candidate.overallStatus === 'COLLECTING'

                      ? '采集中'

                      : tabletStatusText[status];

                    return (

                      <div

                        className="tablet-candidate-item"

                        key={String(candidate.candidateId)}

                        role="button"

                        tabIndex={0}

                        onClick={() => confirmOpenCandidateCollection(candidate)}

                        onKeyDown={(event) => {

                          if (event.key === 'Enter' || event.key === ' ') {

                            event.preventDefault();

                            confirmOpenCandidateCollection(candidate);

                          }

                        }}

                      >

                        <div className="tablet-candidate-item-main">

                          <strong>{candidate.name}</strong>

                          <span>

                            {candidate.admissionNo || '无准考证号'}

                            {candidate.maskedIdCardNo ? ` · ${candidate.maskedIdCardNo}` : ''}

                          </span>

                          <span>

                            {candidate.unitName || '未填写单位'}

                            {candidate.workType ? ` · ${candidate.workType}` : ''}

                            {candidate.level ? ` · ${candidate.level}` : ''}

                          </span>

                        </div>

                        <Space size={8}>

                          <Tag color={tabletStatusColor[status]}>{statusLabel}</Tag>

                          {candidate.collectionEligible !== true ? (
                            <Tag color="orange">

                            资料：{candidate.documentStatus === 'INVALID' ? '资料异常' : '待补充'}

                            </Tag>
                          ) : null}

                          <Button

                            size="large"

                            type={candidate.overallStatus === 'COMPLETED' ? 'default' : 'primary'}

                            icon={<PlayCircleOutlined />}

                            loading={openCandidateMutation.isPending}
                            disabled={candidate.collectionEligible === false}

                            onClick={(event) => {

                              event.stopPropagation();

                              confirmOpenCandidateCollection(candidate);

                            }}

                          >

                            {candidate.overallStatus === 'COMPLETED' ? '查看/继续' : '采集'}

                          </Button>

                        </Space>

                      </div>

                    );

                  })}

                  {filteredCandidates.length === 0 ? (

                    <div className="tablet-candidate-empty">暂无匹配考生</div>

                  ) : null}

                </div>

              </Card>

            </div>

          </div>

        ) : null}



        <CollectionTaskSwitchModal

          open={taskSelectorVisible}

          currentTaskId={taskId}

          switching={taskSwitching}

          onSelect={handleTaskSwitchRequest}

          onClose={() => setTaskSelectorVisible(false)}

        />



        <Modal

          title="调试用：二维码 token 输入"

          open={debugScanOpen}

          okText="确认匹配"

          cancelText="取消"

          confirmLoading={scanMatchMutation.isPending}

          onCancel={() => {

            setDebugScanOpen(false);

            setDebugScanToken('');

          }}

          onOk={handleDebugScanSubmit}

          destroyOnHidden

        >

          <Alert

            type="info"

            showIcon

            message="当前为浏览器调试环境，未检测到 Android 扫码桥接。可手动输入 token 进行联调。"

            className="tablet-debug-scan-alert"

          />

          <Input

            autoFocus

            value={debugScanToken}

            placeholder="请输入或粘贴二维码 token"

            onChange={(event) => setDebugScanToken(event.target.value)}

            onPressEnter={handleDebugScanSubmit}

          />

        </Modal>

      </div>

    </div>

  );

}


