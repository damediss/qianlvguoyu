import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Descriptions, Space, Tag, Typography, message } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
import type { ClassReviewSyncConfig } from '../../../api/training';
import { ClassReviewSyncModal } from '../ClassReviewSyncModal';
import { StatsOverviewPage } from '../../exam/reviewSyncStats/StatsOverviewPage';

type ApiId = number | string;

type Props = {
  classId: ApiId;
  classTitle?: string;
  /** Tab 激活时才拉配置摘要 */
  active: boolean;
};

/**
 * 跟班「同步学」Tab：通道 C 绑定 exam + 开通名单/灰度。
 * Tab 激活时自动把本班学员写入考生名单（未绑定则匹配/新建计划）。
 * 学情直接嵌在本页下方，无需跳转。
 */
export function FollowReviewSyncTab({ classId, classTitle, active }: Props) {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [ensureState, setEnsureState] = useState<'idle' | 'loading' | 'done' | 'error'>('idle');
  const [ensureMessage, setEnsureMessage] = useState<string | null>(null);

  const configQuery = useQuery({
    queryKey: ['class-review-sync-config', classId],
    queryFn: () => trainApi.getClassReviewSyncConfig(classId),
    enabled: active && classId != null,
  });

  useEffect(() => {
    if (!active || classId == null) return;
    let cancelled = false;
    setEnsureState('loading');
    setEnsureMessage(null);
    void trainApi.ensureClassReviewSyncRoster(classId)
      .then((r) => {
        if (cancelled) return;
        if (r.config?.configured) {
          queryClient.setQueryData<ClassReviewSyncConfig>(
            ['class-review-sync-config', classId],
            r.config,
          );
        }
        void queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
        const inserted = r.enroll?.inserted ?? 0;
        if (r.ok === false) {
          setEnsureState('error');
          setEnsureMessage(r.message || '自动同步学员失败，请打开同步学配置处理');
          message.warning(r.message || '自动同步学员失败，请打开同步学配置处理');
          return;
        }
        setEnsureState('done');
        setEnsureMessage(r.message || null);
        if (r.action === 'created' || inserted > 0) {
          message.success(r.message || `已将本班学员同步到同步学（新增 ${inserted}）`);
        }
      })
      .catch((e: Error) => {
        if (cancelled) return;
        setEnsureState('error');
        setEnsureMessage(e.message || '自动同步学员失败');
        message.warning(e.message || '自动同步学员失败');
      });
    return () => {
      cancelled = true;
    };
  }, [active, classId, queryClient]);

  const cfg = configQuery.data;
  const roster = cfg?.roster;
  const resolve = cfg?.resolve;
  const reasonLabel =
    resolve?.reasonCode === 'DISABLED'
      ? '已停用'
      : resolve?.reasonCode === 'WINDOW'
        ? '不在开练窗口'
        : resolve?.reasonCode === 'EXAM_GONE'
          ? '考试计划已失效'
          : resolve?.reasonCode === 'NOT_CONFIGURED'
            ? '未配置'
            : null;

  const examIdNum = useMemo(() => {
    const raw = cfg?.examId;
    if (raw == null || raw === '') return 0;
    const n = Number(raw);
    return Number.isFinite(n) && n > 0 ? n : 0;
  }, [cfg?.examId]);

  const showStats = active && !!cfg?.configured && !cfg?.examGone && examIdNum > 0;
  const stillProvisioning = !cfg?.configured && (ensureState === 'idle' || ensureState === 'loading' || configQuery.isLoading);
  const provisionFailed = !cfg?.configured && ensureState === 'error';
  const provisionEmpty = !cfg?.configured && ensureState === 'done' && !configQuery.isFetching;

  return (
    <div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <div>
            <Typography.Text strong>通道 C · AI 同步学</Typography.Text>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0, marginTop: 4 }}>
              打开本 Tab 或保存配置时，会自动把本班有账号学员写入同步学考生名单。
            </Typography.Paragraph>
          </div>
          <Button type="primary" icon={<SettingOutlined />} onClick={() => setModalOpen(true)}>
            同步学配置
          </Button>
        </Space>
      </Card>

      {stillProvisioning ? (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 12 }}
          message="正在自动开通同步学并同步本班学员…"
          description="优先复用「班级名-同步学」计划；若无匹配则新建。也可点右上角手动改绑。"
        />
      ) : provisionFailed ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="自动开通同步学失败"
          description={ensureMessage || '请打开「同步学配置」手动绑定考试计划（可选择已有「班级名-同步学」）。'}
        />
      ) : provisionEmpty ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="本班尚未绑定同步学考试计划"
          description="考试计划列表里若已有「本班名-同步学」，请点右上角「同步学配置」手动绑定，避免学情对不上。"
        />
      ) : (
        <Card size="small" loading={configQuery.isLoading} style={{ marginBottom: 12 }}>
          <Descriptions column={2} size="small">
            <Descriptions.Item label="考试计划">
              {cfg?.examTitle || cfg?.examId || '—'}
              {cfg?.examGone ? <Tag color="error" style={{ marginLeft: 8 }}>已失效</Tag> : null}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              {cfg?.enabled ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>}
              {reasonLabel ? <Tag style={{ marginLeft: 4 }}>{reasonLabel}</Tag> : null}
              {resolve?.configured && !reasonLabel ? (
                <Tag color="processing" style={{ marginLeft: 4 }}>入口可解析</Tag>
              ) : null}
            </Descriptions.Item>
            <Descriptions.Item label="开练窗口">
              {cfg?.syncStartTime || cfg?.syncEndTime
                ? `${cfg?.syncStartTime || '不限'} ~ ${cfg?.syncEndTime || '不限'}`
                : '不限'}
            </Descriptions.Item>
            <Descriptions.Item label="灰度">
              {!cfg?.grayEnabled
                ? <Tag color="success">全量开放</Tag>
                : cfg?.examInGray
                  ? <Tag color="success">已放行</Tag>
                  : <Tag color="warning">未加入白名单</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="名单差额">
              在册 {roster?.inExamPeople ?? 0} / 有账号 {roster?.withPlatformUser ?? 0}
              {(roster?.missingEnroll ?? 0) > 0 && (
                <Typography.Text type="warning" style={{ marginLeft: 8 }}>
                  待同步 {roster?.missingEnroll}
                </Typography.Text>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="学情">
              {showStats ? '见下方' : '—'}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      {showStats ? (
        <Card size="small" style={{ marginBottom: 12 }} title={null}>
          <StatsOverviewPage
            examId={examIdNum}
            examTitle={cfg?.examTitle ?? undefined}
            embedded
            enabled={active}
          />
        </Card>
      ) : cfg?.configured && cfg?.examGone ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="考试计划已失效，暂无法展示同步学学情"
          description="请打开「同步学配置」重新绑定有效计划。"
        />
      ) : null}

      <Alert
        type="info"
        showIcon
        message="无 App 账号的学员不会写入考生名单；花名册新增或补开账号后会再次自动同步。刷题学情与同步学指标仍分通道统计。"
      />

      <ClassReviewSyncModal
        open={modalOpen}
        onClose={() => {
          setModalOpen(false);
          void configQuery.refetch();
        }}
        classId={classId}
        classTitle={classTitle}
      />
    </div>
  );
}
