import { ExportOutlined, LeftOutlined, LogoutOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, List, Spin, Tag, Typography, message } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { logout } from '../../api/auth';
import { getMyDefenseReport, listMyDefenseReports } from '../../api/defenseScoring';
import { fetchPlatformFileBlob } from '../../api/files';
import { useAuthStore } from '../../stores/authStore';
import type { DefenseAssessorReportListItem } from '../../types/defenseScoring';
import { DefensePdfViewer, openPdfExternally } from './DefensePdfViewer';
import { DefenseScorePanel } from './DefenseScorePanel';
import { DEFENSE_LEVEL_LANES, defenseLevelLane, formatScore, statusLabel } from './defenseScoreCatalog';
import './DefenseReviewApp.css';

type DefenseView = 'list' | 'pdf' | 'score';

export function DefenseReviewApp() {
  const currentUser = useAuthStore((state) => state.currentUser);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [selectedReportId, setSelectedReportId] = useState<string>();
  const [view, setView] = useState<DefenseView>('list');

  useEffect(() => {
    const previous = document.querySelector('meta[name="viewport"]');
    const content = previous?.getAttribute('content');
    document.body.classList.add('defense-review-portrait');
    return () => {
      if (content) previous?.setAttribute('content', content);
      document.body.classList.remove('defense-review-portrait');
    };
  }, []);

  useEffect(() => {
    const previous = document.querySelector('meta[name="viewport"]');
    previous?.setAttribute(
      'content',
      view === 'pdf'
        ? 'width=device-width, initial-scale=1, maximum-scale=4, user-scalable=yes, viewport-fit=cover'
        : 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover',
    );
  }, [view]);

  const listQuery = useQuery({
    queryKey: ['my-defense-reports'],
    queryFn: listMyDefenseReports,
  });

  const detailQuery = useQuery({
    queryKey: ['my-defense-report', selectedReportId],
    queryFn: () => getMyDefenseReport(selectedReportId!),
    enabled: Boolean(selectedReportId) && view === 'pdf',
  });

  async function handleLogout() {
    try {
      await logout();
    } catch {
      // 后端退出失败时仍清理本机登录态
    } finally {
      clearAuth();
      window.location.assign('/assessor-defense');
    }
  }

  function openList() {
    setSelectedReportId(undefined);
    setView('list');
  }

  const selected = listQuery.data?.find((item) => item.reportId === selectedReportId);
  const pdfFileId = detailQuery.data?.fileId || selected?.fileId;
  const pdfFileName = detailQuery.data?.fileName || selected?.fileName;
  const levelLanes = useMemo(() => {
    const items = listQuery.data || [];
    return DEFENSE_LEVEL_LANES.map((lane) => ({
      ...lane,
      items: items.filter((item) => defenseLevelLane(item.levelName) === lane.key),
    }));
  }, [listQuery.data]);

  async function handleOpenExternal() {
    if (!pdfFileId) {
      message.error('报告文件尚未加载完成');
      return;
    }
    try {
      const blob = await fetchPlatformFileBlob(pdfFileId);
      await openPdfExternally(blob, pdfFileName);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '无法打开外部阅读器');
    }
  }

  return (
    <div className={`defense-phone-shell${view === 'score' ? ' defense-phone-shell-score' : ''}`}>
      <header className="defense-phone-header">
        {view !== 'list' ? (
          <Button className="defense-phone-back" icon={<LeftOutlined />} onClick={openList}>
            返回列表
          </Button>
        ) : null}
        <div className="defense-phone-title">
          <Typography.Text type="secondary">考评员综合答辩端</Typography.Text>
          <Typography.Title level={4}>{currentUser?.displayName || currentUser?.username || '考评员'}</Typography.Title>
        </div>
        <div className="defense-phone-actions">
          <Button icon={<ReloadOutlined />} onClick={() => listQuery.refetch()} />
          <Button icon={<LogoutOutlined />} onClick={() => void handleLogout()}>退出</Button>
        </div>
      </header>

      {view === 'score' && selectedReportId ? (
        <DefenseScorePanel reportId={selectedReportId} />
      ) : view === 'pdf' && selectedReportId ? (
        <section className="defense-phone-reader">
          <div className="defense-phone-candidate">
            <Typography.Title level={3}>{detailQuery.data?.candidateName || selected?.candidateName || '考生'}</Typography.Title>
            <Typography.Text type="secondary">
              {[detailQuery.data?.maskedIdCardNo || selected?.maskedIdCardNo, detailQuery.data?.workTypeName || selected?.workTypeName, detailQuery.data?.levelName || selected?.levelName]
                .filter(Boolean)
                .join(' · ') || '业绩评估报告'}
            </Typography.Text>
          </div>
          <DefensePdfViewer fileId={pdfFileId} />
        </section>
      ) : (
        <section className="defense-phone-list">
          {listQuery.isLoading ? (
            <Spin className="defense-phone-spin" />
          ) : (
            <div className="defense-phone-lanes">
              {levelLanes.map((lane) => (
                <section key={lane.key} className={`defense-phone-lane defense-phone-lane-${lane.key}`}>
                  <div className="defense-phone-lane-bar" role="separator">
                    <span className="defense-phone-lane-label">{lane.label}</span>
                    <span className="defense-phone-lane-count">{lane.items.length}人</span>
                  </div>
                  {lane.items.length === 0 ? (
                    <div className="defense-phone-lane-empty">本级别暂无考生</div>
                  ) : (
                    <List
                      dataSource={lane.items}
                      renderItem={(item) => (
                        <ReportListCard
                          item={item}
                          onOpenPdf={() => {
                            setSelectedReportId(item.reportId);
                            setView('pdf');
                          }}
                          onOpenScore={() => {
                            setSelectedReportId(item.reportId);
                            setView('score');
                          }}
                        />
                      )}
                    />
                  )}
                </section>
              ))}
            </div>
          )}
        </section>
      )}

      {view === 'pdf' ? (
        <footer className="defense-phone-footer">
          <Button icon={<ExportOutlined />} onClick={() => void handleOpenExternal()}>
            外部阅读器打开
          </Button>
          <Button type="primary" onClick={() => setView('score')}>
            答辩评分
          </Button>
        </footer>
      ) : null}
    </div>
  );
}

function ReportListCard({
  item,
  onOpenPdf,
  onOpenScore,
}: {
  item: DefenseAssessorReportListItem;
  onOpenPdf: () => void;
  onOpenScore: () => void;
}) {
  return (
    <List.Item
      className="defense-phone-card"
      extra={(
        <div className="defense-phone-card-actions">
          <Button type="primary" onClick={onOpenPdf}>
            考前阅卷
          </Button>
          <Button onClick={onOpenScore}>
            答辩评分
          </Button>
        </div>
      )}
    >
      <div className="defense-phone-card-main">
        <Typography.Title level={5}>{item.candidateName || '考生'}</Typography.Title>
        <Typography.Text type="secondary">{item.admissionTicketNo || item.maskedIdCardNo || '-'}</Typography.Text>
        <div className="defense-phone-meta">
          {item.workTypeName ? <Tag>{item.workTypeName}</Tag> : null}
          {item.levelName ? <Tag color={defenseLevelLane(item.levelName) === 'senior_technician' ? 'volcano' : 'blue'}>{item.levelName}</Tag> : null}
          <Tag color={item.thesisStatus === 'submitted' ? 'green' : item.thesisStatus === 'draft' ? 'gold' : 'default'}>
            论文{statusLabel(item.thesisStatus)}
            {item.thesisStatus === 'submitted' ? ` ${formatScore(item.thesisFinalScore)}` : ''}
          </Tag>
          <Tag color={item.performanceStatus === 'submitted' ? 'green' : item.performanceStatus === 'draft' ? 'gold' : 'default'}>
            业绩{statusLabel(item.performanceStatus)}
            {item.performanceStatus === 'submitted' ? ` ${formatScore(item.performanceFinalScore)}` : ''}
          </Tag>
        </div>
        <Typography.Text type="secondary">{item.fileName || '业绩评估报告.pdf'}</Typography.Text>
      </div>
    </List.Item>
  );
}

