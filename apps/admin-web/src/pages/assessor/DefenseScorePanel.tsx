import { Button, Spin, Tabs, Tag, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  getDefenseScoreSheets,
  saveDefenseScoreSheet,
  submitDefenseScoreSheet,
} from '../../api/defenseScoring';
import { fetchPlatformFileBlob } from '../../api/files';
import type {
  DefensePerformancePayload,
  DefenseScoreSheetType,
  DefenseScoreSheetView,
  DefenseThesisPayload,
} from '../../types/defenseScoring';
import { DefensePerformanceScoreSheet, emptyPerformancePayload } from './DefensePerformanceScoreSheet';
import { DefenseThesisScoreSheet, emptyThesisPayload } from './DefenseThesisScoreSheet';
import type { DefenseExpertSignatureHandle, DefenseSignatureValue } from './DefenseExpertSignature';
import { formatScore, performanceTotal, statusLabel, thesisFinalScore } from './defenseScoreCatalog';

interface Props {
  reportId: string;
}

function signatureFromSheet(sheet?: DefenseScoreSheetView | null): DefenseSignatureValue | null {
  if (!sheet?.signatureFileId) {
    return null;
  }
  return { fileId: String(sheet.signatureFileId), hash: sheet.signatureHash || '' };
}

function isSubmitted(status?: string) {
  return status === 'submitted';
}

export function DefenseScorePanel({ reportId }: Props) {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<DefenseScoreSheetType>('THESIS');
  const [thesis, setThesis] = useState<DefenseThesisPayload>(emptyThesisPayload());
  const [performance, setPerformance] = useState<DefensePerformancePayload>(emptyPerformancePayload());
  const [thesisSign, setThesisSign] = useState<DefenseSignatureValue | null>(null);
  const [performanceSign, setPerformanceSign] = useState<DefenseSignatureValue | null>(null);
  const thesisSignRef = useRef<DefenseExpertSignatureHandle>(null);
  const performanceSignRef = useRef<DefenseExpertSignatureHandle>(null);

  const sheetsQuery = useQuery({
    queryKey: ['defense-score-sheets', reportId],
    queryFn: () => getDefenseScoreSheets(reportId),
  });

  useEffect(() => {
    const data = sheetsQuery.data;
    if (!data) return;
    setThesis({ ...emptyThesisPayload(), ...(data.thesis.thesis || {}) });
    setPerformance({ ...emptyPerformancePayload(), ...(data.performance.performance || {}) });
    setThesisSign(signatureFromSheet(data.thesis));
    setPerformanceSign(signatureFromSheet(data.performance));
  }, [sheetsQuery.data]);

  const thesisLocked = isSubmitted(sheetsQuery.data?.thesis.status);
  const performanceLocked = isSubmitted(sheetsQuery.data?.performance.status);
  const currentLocked = activeTab === 'THESIS' ? thesisLocked : performanceLocked;
  const currentSheet = activeTab === 'THESIS' ? sheetsQuery.data?.thesis : sheetsQuery.data?.performance;
  const currentSign = activeTab === 'THESIS' ? thesisSign : performanceSign;
  const currentSignRef = activeTab === 'THESIS' ? thesisSignRef : performanceSignRef;
  const candidate = sheetsQuery.data?.candidate;

  const previewQuery = useQuery({
    queryKey: ['defense-sign-preview', currentSign?.fileId],
    queryFn: async () => {
      const blob = await fetchPlatformFileBlob(currentSign!.fileId);
      return URL.createObjectURL(blob);
    },
    enabled: Boolean(currentSign?.fileId),
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (activeTab === 'THESIS') {
        return saveDefenseScoreSheet(reportId, 'THESIS', {
          thesis,
          signatureFileId: thesisSign?.fileId,
          signatureHash: thesisSign?.hash,
        });
      }
      return saveDefenseScoreSheet(reportId, 'PERFORMANCE', {
        performance,
        signatureFileId: performanceSign?.fileId,
        signatureHash: performanceSign?.hash,
      });
    },
    onSuccess: async () => {
      message.success('草稿已保存');
      await queryClient.invalidateQueries({ queryKey: ['defense-score-sheets', reportId] });
      await queryClient.invalidateQueries({ queryKey: ['my-defense-reports'] });
    },
    onError: (error: unknown) => {
      message.error(error instanceof Error ? error.message : '保存失败');
    },
  });

  const submitMutation = useMutation({
    mutationFn: async () => {
      const signed = await currentSignRef.current?.ensureSignature();
      if (!signed?.fileId || !signed.hash) {
        throw new Error('请先完成评审专家电子签名');
      }
      if (activeTab === 'THESIS') {
        return submitDefenseScoreSheet(reportId, 'THESIS', {
          thesis,
          signatureFileId: signed.fileId,
          signatureHash: signed.hash,
        });
      }
      return submitDefenseScoreSheet(reportId, 'PERFORMANCE', {
        performance,
        signatureFileId: signed.fileId,
        signatureHash: signed.hash,
      });
    },
    onSuccess: async () => {
      message.success('评分已提交');
      await queryClient.invalidateQueries({ queryKey: ['defense-score-sheets', reportId] });
      await queryClient.invalidateQueries({ queryKey: ['my-defense-reports'] });
    },
    onError: (error: unknown) => {
      message.error(error instanceof Error ? error.message : '提交失败');
    },
  });

  const tabItems = useMemo(() => ([
    {
      key: 'THESIS',
      label: (
        <span>
          论文答辩
          <Tag className="defense-score-tab-tag" color={thesisLocked ? 'green' : sheetsQuery.data?.thesis.status === 'draft' ? 'gold' : 'default'}>
            {statusLabel(sheetsQuery.data?.thesis.status)}
          </Tag>
        </span>
      ),
    },
    {
      key: 'PERFORMANCE',
      label: (
        <span>
          业绩评估
          <Tag className="defense-score-tab-tag" color={performanceLocked ? 'green' : sheetsQuery.data?.performance.status === 'draft' ? 'gold' : 'default'}>
            {statusLabel(sheetsQuery.data?.performance.status)}
          </Tag>
        </span>
      ),
    },
  ]), [performanceLocked, sheetsQuery.data, thesisLocked]);

  if (sheetsQuery.isLoading) {
    return <Spin className="defense-phone-spin" />;
  }

  return (
    <section className="defense-score-page">
      <div className="defense-score-toolbar">
        <div className="defense-phone-candidate">
          <Typography.Title level={3}>{candidate?.candidateName || '考生'}</Typography.Title>
        </div>
        <Tabs
          activeKey={activeTab}
          onChange={(key) => setActiveTab(key as DefenseScoreSheetType)}
          items={tabItems}
        />
      </div>
      <div className="defense-score-body">
        {activeTab === 'THESIS' ? (
          <DefenseThesisScoreSheet
            candidate={candidate}
            assessorName={currentSheet?.assessorName}
            taskId={candidate?.taskId}
            value={thesis}
            disabled={thesisLocked}
            signature={thesisSign}
            signaturePreviewUrl={previewQuery.data}
            signatureRef={thesisSignRef}
            onChange={setThesis}
            onSignatureChange={setThesisSign}
          />
        ) : (
          <DefensePerformanceScoreSheet
            candidate={candidate}
            assessorName={currentSheet?.assessorName}
            taskId={candidate?.taskId}
            value={performance}
            disabled={performanceLocked}
            signature={performanceSign}
            signaturePreviewUrl={previewQuery.data}
            signatureRef={performanceSignRef}
            onChange={setPerformance}
            onSignatureChange={setPerformanceSign}
          />
        )}
      </div>
      <footer className="defense-score-actions">
        <div className="defense-score-actions-score">
          <span>{activeTab === 'THESIS' ? '最终得分' : '合计'}</span>
          <strong>
            {formatScore(activeTab === 'THESIS' ? thesisFinalScore(thesis.contentScores, thesis.defenseScores) : performanceTotal(performance.items))}
          </strong>
        </div>
        <Button disabled={currentLocked} loading={saveMutation.isPending} onClick={() => saveMutation.mutate()}>
          保存草稿
        </Button>
        <Button
          type="primary"
          disabled={currentLocked}
          loading={submitMutation.isPending}
          onClick={() => submitMutation.mutate()}
        >
          提交评分
        </Button>
      </footer>
    </section>
  );
}
