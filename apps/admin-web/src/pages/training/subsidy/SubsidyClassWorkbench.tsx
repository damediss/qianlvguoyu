import { useMemo, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Progress,
  Space,
  Tag,
  Timeline,
  Typography,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  FileDoneOutlined,
  HistoryOutlined,
  SyncOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
import type { TrainSubsidy } from '../../../api/training';
import { downloadUrlFile } from '../../../api/files';
import { TrainingProcessSteps } from '../TrainingProcessSteps';
import { ExportDockBar } from './components/ExportDockBar';
import { MaterialTreePanel } from './components/MaterialTreePanel';
import { MaterialWorkspacePanel } from './components/MaterialWorkspacePanel';
import '../TrainingSubsidyDeclarePage.css';

const PHASE_OPTS = [
  { value: 'draft', label: '草稿' },
  { value: 'checking', label: '核验中' },
  { value: 'ready', label: '材料就绪' },
  { value: 'submitted', label: '已提交' },
  { value: 'rejected', label: '已退回' },
  { value: 'approved', label: '已通过' },
  { value: 'granting', label: '拨付中' },
  { value: 'granted', label: '已拨付' },
  { value: 'closed', label: '已关闭' },
];

const PHASE_COLOR: Record<string, string> = {
  draft: 'default',
  checking: 'processing',
  ready: 'cyan',
  submitted: 'blue',
  rejected: 'error',
  approved: 'success',
  granting: 'gold',
  granted: 'green',
  closed: 'default',
};

type Props = {
  subsidy: TrainSubsidy;
  onBack: () => void;
  onSubsidyChange: (row: TrainSubsidy) => void;
};

function phaseLabel(v?: string | null) {
  return PHASE_OPTS.find((p) => p.value === v)?.label || v || '—';
}

function trainModeLabel(v?: string | null) {
  if (v === 'skill_upgrade') return '岗位技能提升';
  if (v === 'apprentice') return '新型学徒';
  return v || '—';
}

function money(v?: number | null) {
  if (v == null) return '—';
  return `¥${Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`;
}

export function SubsidyClassWorkbench({ subsidy, onBack, onSubsidyChange }: Props) {
  const qc = useQueryClient();
  const detail = subsidy;
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(null);
  const [exportSelectedIds, setExportSelectedIds] = useState<Set<string>>(new Set());
  const [eventsOpen, setEventsOpen] = useState(false);
  const [replacingId, setReplacingId] = useState<string | null>(null);

  const instancesQ = useQuery({
    queryKey: ['subsidy-material-instances', detail.id],
    queryFn: () => trainApi.listSubsidyMaterialInstances(detail.id),
  });

  const eventsQ = useQuery({
    queryKey: ['subsidy-events', detail.id],
    queryFn: () => trainApi.listSubsidyEvents(detail.id),
    enabled: eventsOpen,
  });

  const classMatsQ = useQuery({
    queryKey: ['subsidy-class-materials', detail.id],
    queryFn: () => trainApi.getSubsidyClassMaterials(detail.id),
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['subsidies'] });
    void qc.invalidateQueries({ queryKey: ['subsidy-material-instances', detail.id] });
    void qc.invalidateQueries({ queryKey: ['subsidy-class-materials', detail.id] });
    void qc.invalidateQueries({ queryKey: ['subsidy-events', detail.id] });
  };

  const syncMu = useMutation({
    mutationFn: () => trainApi.syncSubsidyClassMaterials(detail.id),
    onSuccess: (res) => {
      message.success(res.message || '已从本班刷新材料');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '刷新失败'),
  });

  const claimFormsMu = useMutation({
    mutationFn: () => trainApi.generateSubsidyClaimForms(detail.id),
    onSuccess: async (res) => {
      message.success(res.message || '已生成个人申领表');
      invalidate();
      // 私有 COS 无签名直链会 403，必须走鉴权的 /api/files/{id}/download（filePath）
      if (res.filePath || res.url) {
        try {
          await downloadUrlFile(
            res.filePath || res.url!,
            res.downloadFileName || `个人申领表_${detail.id}.zip`,
          );
        } catch {
          message.info('申领表已生成，可在材料清单中导出下载');
        }
      } else if ((res.claimFormsGenerated ?? 0) > 0) {
        message.info('申领表已入库，打包下载未返回，请用下方导出');
      }
    },
    onError: (e: Error) => message.error(e.message || '生成申领表失败'),
  });

  const transitionMu = useMutation({
    mutationFn: (p: { toPhase: string; remark?: string }) =>
      trainApi.transitionSubsidy(detail.id, p.toPhase, p.remark),
    onSuccess: (row) => {
      message.success('状态已更新');
      onSubsidyChange(row);
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '流转失败'),
  });

  const exportMu = useMutation({
    mutationFn: (ids: string[]) => trainApi.exportSubsidyMaterials(detail.id, ids),
    onSuccess: async (res) => {
      if (!res.ok) {
        message.warning(res.message || '导出未完成');
        return;
      }
      message.success(res.message || '已导出');
      if (res.skipped?.length) {
        message.info(`跳过 ${res.skipped.length} 项无文件材料`);
      }
      invalidate();
      const fresh = await trainApi.getSubsidy(detail.id);
      onSubsidyChange(fresh);
      // 私有 COS 无签名直链会 403，必须走鉴权的 /api/files/{id}/download（filePath）
      if (res.filePath || res.url) {
        try {
          await downloadUrlFile(res.filePath || res.url!, `补贴材料_${detail.id}.zip`);
        } catch {
          message.info('材料已生成，可稍后下载');
        }
      }
    },
    onError: (e: Error) => message.error(e.message || '导出失败'),
  });

  const genMu = useMutation({
    mutationFn: () => trainApi.generateSubsidyMaterials(detail.id),
    onSuccess: async (res) => {
      if (!res.ok) {
        message.warning(res.message || '缺项未打包');
        invalidate();
        return;
      }
      message.success(res.message || '已生成（全量）');
      invalidate();
      const fresh = await trainApi.getSubsidy(detail.id);
      onSubsidyChange(fresh);
    },
    onError: (e: Error) => message.error(e.message || '生成失败'),
  });

  const confirmMu = useMutation({
    mutationFn: (instanceId: string) => trainApi.confirmSubsidyMaterialInstance(instanceId),
    onSuccess: () => {
      message.success('已确认');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '确认失败'),
  });

  const groups = instancesQ.data?.groups ?? [];
  const items = instancesQ.data?.items ?? [];
  const selectedItem = useMemo(() => {
    if (!selectedInstanceId) return null;
    return items.find((i) => String(i.id) === selectedInstanceId) ?? null;
  }, [items, selectedInstanceId]);

  const handleReplaceFile = async (instanceId: string, file: File) => {
    setReplacingId(instanceId);
    try {
      await trainApi.replaceSubsidyMaterialFile(instanceId, file);
      message.success('已替换文件');
      setSelectedInstanceId(instanceId);
      invalidate();
    } catch (e) {
      message.error((e as Error).message || '上传失败');
    } finally {
      setReplacingId(null);
    }
  };

  const toggleExport = (id: string, checked: boolean) => {
    setExportSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) next.add(id);
      else next.delete(id);
      return next;
    });
  };

  const toggleAllExport = (checked: boolean, ids: string[]) => {
    setExportSelectedIds(checked ? new Set(ids) : new Set());
  };

  const packProgress = instancesQ.data?.requiredTotal
    ? Math.round(((instancesQ.data.confirmedRequired ?? 0) / instancesQ.data.requiredTotal) * 100)
    : 0;

  return (
    <div className="training-page training-subsidy-declare-page subsidy-class-workbench">
      <div className="subsidy-workbench-header">
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
            返回班级列表
          </Button>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {detail.className || `班级 #${detail.classId}`}
          </Typography.Title>
          <Tag color={PHASE_COLOR[detail.phase || ''] || 'default'}>{phaseLabel(detail.phase)}</Tag>
          {detail.profileLocked ? <Tag color="orange">配方已锁定</Tag> : <Tag>配方未锁定</Tag>}
          <Tag color="processing">材料工作台</Tag>
        </Space>
        <Typography.Text type="secondary">
          {[detail.regionName || detail.regionCode, trainModeLabel(detail.trainMode), detail.policyYear ? `${detail.policyYear}年` : null]
            .filter(Boolean)
            .join(' · ')}
        </Typography.Text>
      </div>

      <TrainingProcessSteps current={8} />

      <Card size="small" style={{ marginBottom: 12 }}>
        <Space wrap>
          <Button icon={<SyncOutlined />} loading={syncMu.isPending} onClick={() => syncMu.mutate()}>
            从本班刷新材料
          </Button>
          <Button
            type="primary"
            icon={<FileDoneOutlined />}
            loading={claimFormsMu.isPending}
            title="仅生成毕业典礼已结业且已回填证书的学员"
            onClick={() => claimFormsMu.mutate()}
          >
            一键生成个人申领表
          </Button>
          <Button icon={<HistoryOutlined />} onClick={() => setEventsOpen(true)}>
            时间线
          </Button>
          {detail.phase === 'draft' && (
            <Button loading={transitionMu.isPending} onClick={() => transitionMu.mutate({ toPhase: 'checking' })}>
              进入核验
            </Button>
          )}
          {(detail.phase === 'draft' || detail.phase === 'checking' || detail.phase === 'ready') && (
            <Button loading={transitionMu.isPending} onClick={() => transitionMu.mutate({ toPhase: 'submitted', remark: '提交人社' })}>
              提交人社
            </Button>
          )}
          <Button icon={<ThunderboltOutlined />} loading={genMu.isPending} onClick={() => genMu.mutate()}>
            全量生成（旧）
          </Button>
        </Space>
        <div style={{ marginTop: 10 }}>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            学员 {classMatsQ.data?.studentCount ?? '—'} 人 · 证书 {classMatsQ.data?.certificateWithFile ?? 0}/
            {classMatsQ.data?.certificateTotal ?? 0} · 照片{' '}
            {classMatsQ.data?.photoPackReady ? '已就绪' : '未生成'}
          </Typography.Text>
        </div>
      </Card>

      <div className="subsidy-material-kit-layout">
        <Card size="small" className="subsidy-material-kit-left" title="本班材料清单" loading={instancesQ.isLoading}>
          <MaterialTreePanel
            groups={groups}
            selectedInstanceId={selectedInstanceId}
            exportSelectedIds={exportSelectedIds}
            onSelectInstance={setSelectedInstanceId}
            onToggleExport={toggleExport}
            onToggleAllExport={toggleAllExport}
            onReplaceFile={handleReplaceFile}
            replaceLoading={replacingId != null}
          />
        </Card>
        <Card size="small" className="subsidy-material-kit-right" title="预览与操作">
          <MaterialWorkspacePanel
            item={selectedItem}
            confirmLoading={confirmMu.isPending}
            replaceLoading={replacingId != null && replacingId === selectedInstanceId}
            onReplaceFile={
              selectedInstanceId
                ? (file) => void handleReplaceFile(selectedInstanceId, file)
                : undefined
            }
            onConfirm={
              selectedInstanceId
                ? () => confirmMu.mutate(selectedInstanceId)
                : undefined
            }
            onOfficeSaved={() => {
              void qc.invalidateQueries({ queryKey: ['subsidy-material-instances', detail.id] });
            }}
          />
        </Card>
      </div>

      <Card size="small" style={{ marginTop: 12 }}>
        <Descriptions size="small" column={4}>
          <Descriptions.Item label="已确认必填">
            {instancesQ.data?.confirmedRequired ?? 0}/{instancesQ.data?.requiredTotal ?? '—'}
          </Descriptions.Item>
          <Descriptions.Item label="有文件">{instancesQ.data?.hasFileCount ?? 0}/{instancesQ.data?.total ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="材料包">
            {detail.materialPackagePath ? <Tag color="success">已生成</Tag> : <Tag>未生成</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="进度">
            <Progress percent={packProgress} size="small" style={{ maxWidth: 160, marginBottom: 0 }} />
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <ExportDockBar
        total={items.length}
        selectedCount={exportSelectedIds.size}
        exporting={exportMu.isPending}
        lastPackagePath={detail.materialPackagePath}
        onExport={() => exportMu.mutate([...exportSelectedIds])}
        onDownloadLast={
          detail.materialPackagePath
            ? () => {
                void downloadUrlFile(detail.materialPackagePath!, `补贴材料_${detail.id}.zip`).catch(() =>
                  message.error('下载失败'),
                );
              }
            : undefined
        }
      />

      <Drawer title="补贴单时间线" width={480} open={eventsOpen} onClose={() => setEventsOpen(false)}>
        {(eventsQ.data?.length ?? 0) === 0 ? (
          <Typography.Text type="secondary">暂无事件</Typography.Text>
        ) : (
          <Timeline
            items={(eventsQ.data || []).map((e) => ({
              children: (
                <div>
                  <Space size={8} wrap>
                    <Tag>{e.action}</Tag>
                    {e.fromPhase || e.toPhase ? (
                      <span>
                        {phaseLabel(e.fromPhase)} → {phaseLabel(e.toPhase)}
                      </span>
                    ) : null}
                  </Space>
                  {e.remark ? <div style={{ marginTop: 4, fontSize: 13 }}>{e.remark}</div> : null}
                  <div style={{ color: '#8c8c8c', fontSize: 12 }}>{e.createdAt}</div>
                </div>
              ),
            }))}
          />
        )}
      </Drawer>
    </div>
  );
}
