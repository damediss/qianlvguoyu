import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  DatePicker,
  Descriptions,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Spin,
  Switch,
  Tag,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../api/training';

type ApiId = number | string;

const { Text } = Typography;
const { RangePicker } = DatePicker;

function parseDay(v?: string | null): Dayjs | null {
  if (!v) return null;
  const d = dayjs(String(v).slice(0, 10));
  return d.isValid() ? d : null;
}

function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

type Props = {
  open: boolean;
  onClose: () => void;
  classId: ApiId | null;
  classTitle?: string;
};

export function ClassReviewSyncModal({ open, onClose, classId, classTitle }: Props) {
  const queryClient = useQueryClient();
  const [examId, setExamId] = useState<string | undefined>();
  const [enabled, setEnabled] = useState(true);
  const [remark, setRemark] = useState('');
  const [syncRange, setSyncRange] = useState<[Dayjs, Dayjs] | null>(null);

  const configQuery = useQuery({
    queryKey: ['class-review-sync-config', classId],
    queryFn: () => trainApi.getClassReviewSyncConfig(classId!),
    enabled: open && classId != null,
  });

  const optionsQuery = useQuery({
    queryKey: ['class-review-sync-exam-options', classId],
    queryFn: () => trainApi.listClassReviewSyncExamOptions(classId!),
    enabled: open && classId != null,
    staleTime: 60_000,
  });

  useEffect(() => {
    if (!open || !configQuery.data) return;
    const d = configQuery.data;
    setExamId(d.examId ?? undefined);
    setEnabled(!!d.enabled);
    setRemark(d.remark ?? '');
    const start = parseDay(d.syncStartTime);
    const end = parseDay(d.syncEndTime);
    if (start && end) setSyncRange([start, end]);
    else if (start || end) {
      const one = start ?? end!;
      setSyncRange([one, one]);
    } else setSyncRange(null);
  }, [open, configQuery.data]);

  const examOptions = useMemo(() => {
    const items = optionsQuery.data?.items ?? [];
    return items.map((it) => ({
      value: it.examId,
      label: (
        <Space size={6}>
          <span>{it.title}</span>
          {it.matched ? <Tag color="success">工种匹配</Tag> : it.workTypeMatched ? <Tag>工种相近</Tag> : null}
        </Space>
      ),
      search: it.title,
    }));
  }, [optionsQuery.data]);

  const saveMu = useMutation({
    mutationFn: () => {
      if (!examId) throw new Error('请选择同步学考试计划');
      return trainApi.saveClassReviewSyncConfig(classId!, {
        examId,
        enabled: enabled ? 1 : 0,
        syncStartTime: syncRange?.[0]?.format('YYYY-MM-DD') ?? null,
        syncEndTime: syncRange?.[1]?.format('YYYY-MM-DD') ?? null,
        remark: remark.trim() || null,
      });
    },
    onSuccess: (cfg) => {
      const inserted = cfg.enroll?.inserted ?? 0;
      const removedStale = cfg.enroll?.removedStale ?? 0;
      const reviewError = cfg.reviewError || cfg.enroll?.reviewError;
      if (cfg.enrollError) {
        message.warning(`绑定已保存，但名单同步失败：${cfg.enrollError}`);
      } else if (reviewError) {
        message.warning(`绑定已保存，但复习任务未开通：${reviewError}`);
      } else if (cfg.grayError) {
        message.warning(`绑定已保存，但灰度开通失败：${cfg.grayError}`);
      } else if (inserted > 0 || removedStale > 0) {
        const parts = [`同步学配置已保存`];
        if (inserted > 0) parts.push(`写入 ${inserted} 名学员`);
        if (removedStale > 0) parts.push(`已移除 ${removedStale} 名非本班考生`);
        message.success(`${parts.join('，')}；请学员在「复习任务」中进入同步练习`);
      } else {
        message.success('同步学配置已保存；请学员在「提升 → 复习任务」中进入同步练习');
      }
      void queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const enrollMu = useMutation({
    mutationFn: () => trainApi.enrollClassReviewSyncRoster(classId!),
    onSuccess: (r) => {
      const staleHint = (r.removedStale ?? 0) > 0 ? `，移除非本班 ${r.removedStale}` : '';
      message.success(
        `名单同步完成：新增 ${r.inserted}，已在册 ${r.existed}，无账号跳过 ${r.skippedNoAccount}${staleHint}`,
      );
      void queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
    },
    onError: (e: Error) => message.error(e.message || '同步名单失败'),
  });

  const grayMu = useMutation({
    mutationFn: () => trainApi.ensureClassReviewSyncGray(classId!),
    onSuccess: (r) => {
      message.success(r.message || '已处理灰度');
      void queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
    },
    onError: (e: Error) => message.error(e.message || '加入灰度失败'),
  });

  const cfg = configQuery.data;
  const roster = cfg?.roster;
  const canOpenActions = !!(cfg?.configured && cfg.examId);

  return (
    <Modal
      title={`同步学配置${classTitle ? ` · ${classTitle}` : ''}`}
      open={open}
      onCancel={onClose}
      width={720}
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={onClose}>
          关闭
        </Button>,
        <Button
          key="save"
          type="primary"
          loading={saveMu.isPending}
          disabled={!examId}
          onClick={() => saveMu.mutate()}
        >
          保存绑定
        </Button>,
      ]}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="通道 C · AI 同步学"
        description="为本班绑定一套同步学考试计划。保存时会自动把本班有账号学员写入考生名单，并按需加入灰度。"
      />

      <Spin spinning={configQuery.isLoading || optionsQuery.isLoading}>
        <Form layout="vertical">
          <Form.Item label="同步学考试计划" required>
            <Select
              showSearch
              allowClear
              placeholder={optionsQuery.data?.items?.length ? '请选择考试计划' : '本培训下暂无考试计划'}
              value={examId}
              options={examOptions}
              optionFilterProp="search"
              filterOption={(input, option) =>
                String(option?.search ?? '').toLowerCase().includes(input.toLowerCase())
              }
              onChange={(v) => setExamId(v)}
            />
          </Form.Item>

          <Form.Item label="启用">
            <Switch checked={enabled} onChange={setEnabled} checkedChildren="启用" unCheckedChildren="停用" />
            <Text type="secondary" style={{ marginLeft: 12 }}>
              停用后学员班级入口将隐藏
            </Text>
          </Form.Item>

          <Form.Item label="开练时间窗（可选）">
            <RangePicker
              value={syncRange}
              onChange={(v) => setSyncRange(v as [Dayjs, Dayjs] | null)}
              style={{ width: '100%' }}
            />
            <Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
              留空表示不限；窗口外学员入口隐藏
            </Text>
          </Form.Item>

          <Form.Item label="备注">
            <Input.TextArea
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              rows={2}
              maxLength={255}
              showCount
              placeholder="可选"
            />
          </Form.Item>
        </Form>

        {cfg && (
          <>
            <Descriptions
              size="small"
              bordered
              column={2}
              title="开通摘要"
              style={{ marginBottom: 12 }}
            >
              <Descriptions.Item label="灰度总开关">
                {cfg.grayEnabled ? <Tag color="warning">灰度中</Tag> : <Tag color="success">全量开放</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="本计划白名单">
                {cfg.examInGray ? <Tag color="success">已放行</Tag> : <Tag color="error">未加入</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="班级学员">{roster?.total ?? 0}</Descriptions.Item>
              <Descriptions.Item label="有账号">{roster?.withPlatformUser ?? 0}</Descriptions.Item>
              <Descriptions.Item label="已在考生名单">{roster?.inExamPeople ?? 0}</Descriptions.Item>
              <Descriptions.Item label="待同步名单">
                <Text type={(roster?.missingEnroll ?? 0) > 0 ? 'warning' : undefined}>
                  {roster?.missingEnroll ?? 0}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="无账号" span={2}>
                {roster?.missingAccount ?? 0}
                {(roster?.missingAccount ?? 0) > 0 && (
                  <Text type="secondary">（需学员先登录 App 绑定账号）</Text>
                )}
              </Descriptions.Item>
            </Descriptions>

            {(roster?.missingEnroll ?? 0) > 0 && (
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 12 }}
                message={`还有 ${roster?.missingEnroll} 名有账号学员未写入考生名单，学员将无法生成同步学计划`}
              />
            )}

            <Space wrap style={{ marginBottom: 8 }}>
              <Button
                disabled={!canOpenActions}
                loading={enrollMu.isPending}
                onClick={() => {
                  Modal.confirm({
                    title: '同步本班名单',
                    content: '将把本班有账号学员写入所选考试计划的考生名单（幂等，不自动建复习计划）。确定继续？',
                    onOk: () => enrollMu.mutateAsync(),
                  });
                }}
              >
                同步本班名单
              </Button>
              <Button
                disabled={!canOpenActions || !cfg.grayEnabled || cfg.examInGray}
                loading={grayMu.isPending}
                onClick={() => {
                  Modal.confirm({
                    title: '加入灰度',
                    content: '将把该考试计划写入同步学灰度白名单（计划级，可能影响同计划其他班级）。确定继续？',
                    onOk: () => grayMu.mutateAsync(),
                  });
                }}
              >
                加入灰度
              </Button>
              {cfg.statsPath && (
                <Button
                  type="link"
                  onClick={() => navigateTo(cfg.statsPath!)}
                >
                  查看同步学学情
                </Button>
              )}
            </Space>
          </>
        )}
      </Spin>
    </Modal>
  );
}
