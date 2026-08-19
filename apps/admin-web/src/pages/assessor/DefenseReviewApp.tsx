import { LeftOutlined, LogoutOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Empty, List, Spin, Tag, Typography, message } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { logout } from '../../api/auth';
import { getMyDefenseReport, listMyDefenseReports } from '../../api/defenseScoring';
import { fetchPlatformFileBlob } from '../../api/files';
import { useAuthStore } from '../../stores/authStore';
import './DefenseReviewApp.css';

export function DefenseReviewApp() {
  const currentUser = useAuthStore((state) => state.currentUser);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const [selectedReportId, setSelectedReportId] = useState<string>();

  useEffect(() => {
    const previous = document.querySelector('meta[name="viewport"]');
    const content = previous?.getAttribute('content');
    previous?.setAttribute(
      'content',
      'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover',
    );
    document.body.classList.add('defense-review-portrait');
    return () => {
      if (content) previous?.setAttribute('content', content);
      document.body.classList.remove('defense-review-portrait');
    };
  }, []);

  const listQuery = useQuery({
    queryKey: ['my-defense-reports'],
    queryFn: listMyDefenseReports,
  });

  const detailQuery = useQuery({
    queryKey: ['my-defense-report', selectedReportId],
    queryFn: () => getMyDefenseReport(selectedReportId!),
    enabled: Boolean(selectedReportId),
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

  const selected = listQuery.data?.find((item) => item.reportId === selectedReportId);

  return (
    <div className="defense-phone-shell">
      <header className="defense-phone-header">
        <div>
          <Typography.Text type="secondary">考评员阅卷端</Typography.Text>
          <Typography.Title level={4}>{currentUser?.displayName || currentUser?.username || '考评员'}</Typography.Title>
        </div>
        <div className="defense-phone-actions">
          <Button icon={<ReloadOutlined />} onClick={() => listQuery.refetch()} />
          <Button icon={<LogoutOutlined />} onClick={() => void handleLogout()}>退出</Button>
        </div>
      </header>

      {selectedReportId ? (
        <section className="defense-phone-reader">
          <Button
            className="defense-phone-back"
            icon={<LeftOutlined />}
            onClick={() => setSelectedReportId(undefined)}
          >
            返回列表
          </Button>
          <div className="defense-phone-candidate">
            <Typography.Title level={3}>{detailQuery.data?.candidateName || selected?.candidateName || '考生'}</Typography.Title>
            <Typography.Text type="secondary">
              {[detailQuery.data?.maskedIdCardNo || selected?.maskedIdCardNo, detailQuery.data?.workTypeName || selected?.workTypeName, detailQuery.data?.levelName || selected?.levelName]
                .filter(Boolean)
                .join(' · ') || '业绩评估报告'}
            </Typography.Text>
          </div>
          <MobilePdfViewer fileId={detailQuery.data?.fileId || selected?.fileId} />
        </section>
      ) : (
        <section className="defense-phone-list">
          {listQuery.isLoading ? (
            <Spin className="defense-phone-spin" />
          ) : (listQuery.data || []).length === 0 ? (
            <Empty description="暂无待阅览的业绩评估报告" />
          ) : (
            <List
              dataSource={listQuery.data}
              renderItem={(item) => (
                <List.Item className="defense-phone-card" onClick={() => setSelectedReportId(item.reportId)}>
                  <div>
                    <Typography.Title level={5}>{item.candidateName || '考生'}</Typography.Title>
                    <Typography.Text type="secondary">{item.maskedIdCardNo || '-'}</Typography.Text>
                    <div className="defense-phone-meta">
                      {item.workTypeName ? <Tag>{item.workTypeName}</Tag> : null}
                      {item.levelName ? <Tag>{item.levelName}</Tag> : null}
                    </div>
                    <Typography.Text type="secondary">{item.fileName || '业绩评估报告.pdf'}</Typography.Text>
                  </div>
                </List.Item>
              )}
            />
          )}
        </section>
      )}
    </div>
  );
}

function MobilePdfViewer({ fileId }: { fileId?: string }) {
  const query = useQuery({
    queryKey: ['defense-pdf', fileId],
    queryFn: async () => {
      const blob = await fetchPlatformFileBlob(fileId!);
      return URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
    },
    enabled: Boolean(fileId),
  });

  if (!fileId || query.isLoading) {
    return <Spin className="defense-phone-spin" />;
  }
  if (!query.data) {
    return <Empty description="报告加载失败" />;
  }
  return (
    <div className="defense-pdf-wrap">
      <iframe title="业绩评估报告" src={`${query.data}#toolbar=0&navpanes=0&view=FitH`} />
      <Button
        className="defense-pdf-open"
        block
        onClick={() => {
          const opened = window.open(query.data, '_blank');
          if (!opened) {
            message.warning('请允许弹出窗口后使用系统阅读器打开');
          }
        }}
      >
        使用系统阅读器打开
      </Button>
    </div>
  );
}
