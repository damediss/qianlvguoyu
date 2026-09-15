/**
 * 知识归属修正工作台（方案 §7.4）：三页签
 * ① 去重清理（生成清单 → 软删除 → 可回滚）
 * ② 知识修正（LLM 三采样自一致分类 → apply/keep/flag 清单 → 置信分层写回）
 * ③ 质量报告（自一致率/置信分布/flag 规模）
 */
import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert, Button, Card, Descriptions, message, Popconfirm, Progress, Space, Statistic, Table, Tabs, Tag,
} from 'antd';
import {
  dedupStats, generateDedupList, applyDedup, rollbackDedup,
  submitClassify, resetClassify, classifyProgress, fixList, exportFixList, applyFix, rollbackFix,
  qualityReport,
} from './knowledgeFixApi';
import type { DedupStats, FixBatch, FixItem } from './knowledgeFixApi';

const STATUS_TAG: Record<string, { color: string; text: string }> = {
  apply: { color: 'success', text: '应用' },
  keep: { color: 'processing', text: '保留' },
  flag: { color: 'warning', text: '待关注' },
  pending: { color: 'default', text: '待分类' },
  applied: { color: 'success', text: '已写回' },
};

/** 判断批次是否仍在跑（进度轮询持续条件） */
const batchRunning = (b: FixBatch | null | undefined) =>
  !!b && (b.status === 'running' || b.status === 'pending');

const KnowledgeFixPage: React.FC = () => {
  const qc = useQueryClient();

  // ---------------- 去重清理 ----------------
  const [dedupGenerating, setDedupGenerating] = useState(false);
  const [dedupApplying, setDedupApplying] = useState(false);
  const [lastDedupBackup, setLastDedupBackup] = useState<string | null>(null);

  const dedupStatsQuery = useQuery({
    queryKey: ['knowledgefix-dedup-stats'],
    queryFn: dedupStats,
  });

  const doGenerateDedup = async () => {
    setDedupGenerating(true);
    try {
      const stats = await generateDedupList();
      message.success(`去重清单已生成：重复题组 ${stats.dupGroups}，待软删 ${stats.deleteRows} 行` +
        (stats.manualReviewRows > 0 ? `，人工复核 ${stats.manualReviewRows} 行` : ''));
      qc.invalidateQueries({ queryKey: ['knowledgefix-dedup-stats'] });
    } catch {
      /* 拦截器统一提示 */
    } finally {
      setDedupGenerating(false);
    }
  };

  const doApplyDedup = async () => {
    setDedupApplying(true);
    try {
      const r = await applyDedup();
      setLastDedupBackup(r.backupTable);
      message.success(`软删除完成：${r.affectedRows} 行（备份表 ${r.backupTable ?? '-'}，可回滚）`);
      qc.invalidateQueries({ queryKey: ['knowledgefix-dedup-stats'] });
    } catch {
      /* 拦截器统一提示 */
    } finally {
      setDedupApplying(false);
    }
  };

  const doRollbackDedup = async () => {
    if (!lastDedupBackup) {
      message.warning('本次会话无备份表记录，请使用库内备份表名回滚');
      return;
    }
    try {
      const n = await rollbackDedup(lastDedupBackup);
      message.success(`已回滚 ${n} 行`);
      qc.invalidateQueries({ queryKey: ['knowledgefix-dedup-stats'] });
    } catch {
      /* 拦截器统一提示 */
    }
  };

  const dedupColumns = [
    { title: '指标', dataIndex: 'k', key: 'k' },
    { title: '数值', dataIndex: 'v', key: 'v' },
  ];
  const s = dedupStatsQuery.data;
  const dedupRows = [
    { k: '5 月以来导入行数（存活）', v: s?.mayTotalRows ?? '-' },
    { k: '重复题组数（同课程口径）', v: s?.dupGroups ?? '-' },
    { k: '待执行删除行', v: s?.deleteRows ?? '-' },
    { k: '已软删行（可回滚）', v: s?.alreadyDeletedRows ?? '-' },
    { k: '人工复核行（有引用）', v: s?.manualReviewRows ?? '-' },
  ];

  // ---------------- 知识修正 ----------------
  const [fixApplying, setFixApplying] = useState(false);

  const progressQuery = useQuery({
    queryKey: ['knowledgefix-progress'],
    queryFn: classifyProgress,
    refetchInterval: (q) => (batchRunning(q.state.data) ? 3000 : false),
  });
  const batch = progressQuery.data;

  const reportQuery = useQuery({
    queryKey: ['knowledgefix-report'],
    queryFn: qualityReport,
  });
  const report = reportQuery.data ?? batch;

  const [fixStatus, setFixStatus] = useState<string | undefined>();
  const [fixPage, setFixPage] = useState(1);
  const fixQuery = useQuery({
    queryKey: ['knowledgefix-fix-list', fixStatus, fixPage],
    queryFn: () => fixList({ status: fixStatus, page: fixPage, pageSize: 20 }),
  });

  const doSubmitClassify = async () => {
    try {
      const b = await submitClassify();
      message.success(`分类批次已提交：${b.total} 行（三采样自一致 + 低置信升级旗舰）`);
      qc.invalidateQueries({ queryKey: ['knowledgefix-progress'] });
    } catch {
      /* 拦截器统一提示 */
    }
  };

  const doResetClassify = async () => {
    try {
      const n = await resetClassify();
      message.success(`已复位 ${n} 行（待关注/未分类 → 待分类），可重新提交批次`);
      qc.invalidateQueries({ queryKey: ['knowledgefix-progress'] });
      qc.invalidateQueries({ queryKey: ['knowledgefix-fix-list'] });
    } catch {
      /* 拦截器统一提示 */
    }
  };

  const doApplyFix = async () => {
    setFixApplying(true);
    try {
      const r = await applyFix();
      message.success(`知识修正写回完成：${r.affectedRows} 行（备份表 ${r.backupTable ?? '-'}）`);
      qc.invalidateQueries({ queryKey: ['knowledgefix-fix-list'] });
      qc.invalidateQueries({ queryKey: ['knowledgefix-report'] });
    } catch {
      /* 拦截器统一提示 */
    } finally {
      setFixApplying(false);
    }
  };

  const doExportFix = async () => {
    const blob = await exportFixList();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'knowledge-fix-list.xlsx';
    a.click();
    URL.revokeObjectURL(url);
  };

  const fixColumns = [
    { title: 'detail_id', dataIndex: 'detailId', key: 'detailId', width: 110 },
    { title: '原知识点', dataIndex: 'oldKnowledgeId', key: 'oldKnowledgeId', width: 110 },
    { title: '新知识点', dataIndex: 'newKnowledgeId', key: 'newKnowledgeId', width: 110,
      render: (v: number | null) => (v == null ? '-' : v) },
    { title: '置信', dataIndex: 'confidence', key: 'confidence', width: 70,
      render: (v: number | null) => (v == null ? '-' : v.toFixed(2)) },
    { title: '依据', dataIndex: 'basis', key: 'basis', width: 90 },
    { title: '理由', dataIndex: 'llmReason', key: 'llmReason', ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90,
      render: (v: string) => {
        const t = STATUS_TAG[v] ?? { color: 'default', text: v };
        return <Tag color={t.color}>{t.text}</Tag>;
      } },
  ];

  return (
    <div className="ximu-page">
      <Alert
        type="warning"
        showIcon
        message="生产数据操作：所有写操作自动备份、可整体回滚；执行前请确认当前窗口为审批允许的时段。"
        style={{ marginBottom: 12 }}
      />
      <Tabs items={[
        {
          key: 'dedup', label: '去重清理',
          children: (
            <>
              <Space style={{ marginBottom: 16 }} wrap>
                <Button loading={dedupGenerating} onClick={doGenerateDedup}>
                  {dedupGenerating ? '清单生成中…' : '生成/刷新去重清单'}
                </Button>
                <Popconfirm
                  title="确认执行软删除？"
                  description="将按清单把冗余副本置为 deleted=1（已自动备份，可回滚）"
                  onConfirm={doApplyDedup}
                  disabled={dedupApplying || !s || (s?.deleteRows ?? 0) === 0}
                >
                  <Button danger loading={dedupApplying} disabled={dedupApplying || !s || (s?.deleteRows ?? 0) === 0}>
                    执行软删除（deleted=1）
                  </Button>
                </Popconfirm>
                {lastDedupBackup && (
                  <Popconfirm title="确认回滚本次软删除？" onConfirm={doRollbackDedup}>
                    <Button size="small">回滚本次去重</Button>
                  </Popconfirm>
                )}
              </Space>
              {dedupApplying && <Progress percent={70} status="active" style={{ marginBottom: 12 }} />}
              <Descriptions column={2} size="small" style={{ marginBottom: 12 }} title="统计">
                <Descriptions.Item label="5 月以来导入">{s?.mayTotalRows ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="重复题组">{s?.dupGroups ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="待删冗余">{s?.deleteRows ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="已软删">{s?.alreadyDeletedRows ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="人工复核">{s?.manualReviewRows ?? '-'}</Descriptions.Item>
              </Descriptions>
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 12 }}
                message="判重口径：同一课程内同题多份才算重复；跨课程同题各自保留（课程是题库使用单位）。无知识归属的行按独立桶处理。"
              />
              <Table rowKey="k" size="small" dataSource={dedupRows} columns={dedupColumns} pagination={false} />
            </>
          ),
        },
        {
          key: 'fix', label: '知识修正',
          children: (
            <>
              <Space style={{ marginBottom: 16 }} wrap>
                <Button type="primary" onClick={doSubmitClassify} disabled={batchRunning(batch)}>
                  提交 LLM 分类批次
                </Button>
                <Popconfirm
                  title="确认复位未达标行以重跑？"
                  description="将「待关注/未分类」行复位为待分类并清空上一轮结论（已写回与待写回行不受影响）"
                  onConfirm={doResetClassify}
                  disabled={batchRunning(batch)}
                >
                  <Button disabled={batchRunning(batch)}>复位并重跑（未达标行）</Button>
                </Popconfirm>
                <Button onClick={doExportFix}>导出修正清单 xlsx</Button>
                <Popconfirm
                  title="确认执行知识修正写回？"
                  description="仅 apply 行更新 knowledge_id（已自动备份，可回滚）"
                  onConfirm={doApplyFix}
                  disabled={fixApplying || !batch || batch.status !== 'done' || (batch.applyCnt ?? 0) === 0}
                >
                  <Button danger loading={fixApplying}
                    disabled={fixApplying || !batch || batch.status !== 'done' || (batch.applyCnt ?? 0) === 0}>
                    执行写回（仅 apply 行）
                  </Button>
                </Popconfirm>
              </Space>
              {batchRunning(batch) && (
                <Progress
                  percent={batch && batch.total > 0
                    ? Math.round(((batch.done ?? 0) + (batch.failed ?? 0)) / batch.total * 100)
                    : 0}
                  status="active"
                  style={{ marginBottom: 12 }}
                />
              )}
              {batch && (
                <>
                  <Descriptions column={5} size="small" style={{ marginBottom: 8 }} title={`批次 ${batch.batchNo.slice(-8)}`}>
                    <Descriptions.Item label="总数">{batch.total}</Descriptions.Item>
                    <Descriptions.Item label="已分类（进过模型）">{batch.done ?? 0}</Descriptions.Item>
                    <Descriptions.Item label="未分类">{batch.unclassified ?? 0}</Descriptions.Item>
                    <Descriptions.Item label="apply">{batch.applyCnt ?? 0}</Descriptions.Item>
                    <Descriptions.Item label="keep">{batch.keepCnt ?? 0}</Descriptions.Item>
                    <Descriptions.Item label="flag">{batch.flagCnt ?? 0}</Descriptions.Item>
                    <Descriptions.Item label="失败重试次数">{batch.failed ?? 0}</Descriptions.Item>
                  </Descriptions>
                  {batch.status === 'failed' && (
                    <Alert
                      type="error"
                      showIcon
                      style={{ marginBottom: 12 }}
                      message="未达质量门禁，禁止写回"
                      description={batch.failReason ?? '未分类占比 >5% 或自一致率 <80%；可点「复位并重跑」后重新提交'}
                    />
                  )}
                  {batch.status === 'failed' && <div style={{ marginBottom: 12 }} />}
                </>
              )}
              <Space style={{ marginBottom: 12 }} wrap>
                {['apply', 'keep', 'flag', 'pending'].map((st) => (
                  <Button key={st} size="small" type={fixStatus === st ? 'primary' : 'default'}
                    onClick={() => { setFixStatus(st); setFixPage(1); }}>
                    {STATUS_TAG[st]?.text ?? st}
                  </Button>
                ))}
                <Button size="small" onClick={() => { setFixStatus(undefined); setFixPage(1); }}>全部</Button>
              </Space>
              <Table rowKey="rowId" size="small"
                loading={fixQuery.isLoading}
                dataSource={fixQuery.data?.records ?? []}
                columns={fixColumns}
                pagination={{
                  current: fixPage, pageSize: 20,
                  onChange: (p) => setFixPage(p),
                }} />
            </>
          ),
        },
        {
          key: 'report', label: '质量报告',
          children: report ? (
            <Descriptions column={2} bordered size="small" title={`批次 ${report.batchNo.slice(-8)} 质量报告`}>
              <Descriptions.Item label="总行数">{report.total}</Descriptions.Item>
              <Descriptions.Item label="状态">{report.status}</Descriptions.Item>
              <Descriptions.Item label="apply（已按新知识点修正）">{report.applyCnt ?? 0}</Descriptions.Item>
              <Descriptions.Item label="keep（判定归属正确保留）">{report.keepCnt ?? 0}</Descriptions.Item>
              <Descriptions.Item label="flag（置信不足保持原节点）">{report.flagCnt ?? 0}</Descriptions.Item>
              <Descriptions.Item label="失败重试次数">{report.failed ?? 0}</Descriptions.Item>
              <Descriptions.Item label="未分类（未进模型）">{report.unclassified ?? 0}</Descriptions.Item>
              <Descriptions.Item label="自一致率（三采样全一致占比）">
                {report.selfConsistencyRate == null ? '-' : `${(report.selfConsistencyRate * 100).toFixed(1)}%`}
              </Descriptions.Item>
            </Descriptions>
          ) : (
            <Alert type="info" showIcon message="尚无批次数据：提交分类批次后此处输出质量报告。" />
          ),
        },
      ]} />
    </div>
  );
};

export default KnowledgeFixPage;
