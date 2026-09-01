import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Spin,
  Switch,
  Tree,
  Typography,
  message,
} from 'antd';
import type { DataNode } from 'antd/es/tree';
import type { Key } from 'rc-tree/es/interface';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../api/training';

type ApiId = number | string;

type KnowledgeNode = {
  id: number;
  title: string;
  courseId?: number;
  parentId?: number;
  children?: KnowledgeNode[];
};

const { Text } = Typography;
const { RangePicker } = DatePicker;

function toKnowledgeTree(nodes: KnowledgeNode[]): DataNode[] {
  return (nodes ?? []).map((n) => ({
    key: String(n.id),
    title: n.title,
    children: n.children?.length ? toKnowledgeTree(n.children) : undefined,
  }));
}

function collectKeys(nodes: DataNode[]): string[] {
  const out: string[] = [];
  const walk = (list: DataNode[]) => {
    for (const n of list) {
      out.push(String(n.key));
      if (n.children?.length) walk(n.children);
    }
  };
  walk(nodes);
  return out;
}

function parseScopeTime(v?: string | null): Dayjs | null {
  if (!v) return null;
  const d = dayjs(String(v).slice(0, 10));
  return d.isValid() ? d : null;
}

type Props = {
  open: boolean;
  onClose: () => void;
  classId: ApiId | null;
  classTitle?: string;
  /** 弹窗标题前缀，默认「练习范围」；跟班侧用「试题配置」 */
  titlePrefix?: string;
};

export function ClassPracticeScopeModal({ open, onClose, classId, classTitle, titlePrefix = '练习范围' }: Props) {
  const queryClient = useQueryClient();
  const [enabled, setEnabled] = useState(true);
  const [remark, setRemark] = useState('');
  const [practiceRange, setPracticeRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [courseIds, setCourseIds] = useState<number[]>([]);
  /** knowledgeId -> courseId，切换科目时用于清理残留 */
  const [knowledgeCourseMap, setKnowledgeCourseMap] = useState<Record<number, number>>({});
  const [activeCourseId, setActiveCourseId] = useState<number | undefined>();

  const knowledgeIds = useMemo(
    () => Object.keys(knowledgeCourseMap).map(Number).filter((n) => !Number.isNaN(n)),
    [knowledgeCourseMap],
  );

  const scopeQuery = useQuery({
    queryKey: ['class-practice-scope', classId],
    queryFn: () => trainApi.getClassPracticeScope(classId!),
    enabled: open && classId != null,
  });

  const coursesQuery = useQuery({
    queryKey: ['practice-scope-courses', open],
    queryFn: async () => {
      const all = await trainApi.listPracticeScopeCourses();
      return all.items ?? [];
    },
    enabled: open,
    staleTime: 5 * 60 * 1000,
  });

  const treeQuery = useQuery({
    queryKey: ['practice-scope-knowledge-tree', activeCourseId],
    queryFn: () => trainApi.listPracticeScopeKnowledgeTree(activeCourseId!),
    enabled: open && activeCourseId != null,
  });

  useEffect(() => {
    if (!open || !scopeQuery.data) return;
    const d = scopeQuery.data;
    setEnabled(d.enabled !== 0);
    setRemark(d.remark ?? '');
    const start = parseScopeTime(d.practiceStartTime);
    const end = parseScopeTime(d.practiceEndTime);
    if (start && end) {
      setPracticeRange([start, end]);
    } else if (start || end) {
      const one = start ?? end!;
      setPracticeRange([one, one]);
    } else {
      setPracticeRange(null);
    }
    setCourseIds(d.courseIds ?? []);
    const map: Record<number, number> = {};
    for (const k of d.knowledges ?? []) {
      if (k.knowledgeId != null && k.courseId != null) {
        map[k.knowledgeId] = k.courseId;
      }
    }
    setKnowledgeCourseMap(map);
    setActiveCourseId(d.courseIds?.[0]);
  }, [open, scopeQuery.data]);

  useEffect(() => {
    if (!open) return;
    if (courseIds.length === 0) {
      setActiveCourseId(undefined);
      return;
    }
    if (activeCourseId == null || !courseIds.includes(activeCourseId)) {
      setActiveCourseId(courseIds[0]);
    }
  }, [open, courseIds, activeCourseId]);

  const courseOptions = useMemo(() => {
    return (coursesQuery.data ?? []).map((c) => ({
      value: c.id,
      label: `${c.title}${c.level != null ? `（等级${c.level}）` : ''}`,
    }));
  }, [coursesQuery.data]);

  const treeData = useMemo(
    () => toKnowledgeTree((treeQuery.data ?? []) as KnowledgeNode[]),
    [treeQuery.data],
  );
  const checkedKeysForActive = useMemo(() => {
    if (activeCourseId == null) return [];
    return Object.entries(knowledgeCourseMap)
      .filter(([, cid]) => cid === activeCourseId)
      .map(([kid]) => String(kid));
  }, [knowledgeCourseMap, activeCourseId]);

  const saveMu = useMutation({
    mutationFn: () => trainApi.saveClassPracticeScope(classId!, {
      enabled: enabled ? 1 : 0,
      remark: remark.trim() || undefined,
      practiceStartTime: practiceRange?.[0]?.format('YYYY-MM-DD') ?? null,
      practiceEndTime: practiceRange?.[1]?.format('YYYY-MM-DD') ?? null,
      courseIds,
      knowledgeIds,
    }),
    onSuccess: (res) => {
      const auto = res?.reviewSyncAuto;
      if (auto?.ok === false) {
        message.warning(`试题配置已保存，但同步学未自动开通：${auto.message || '请到「同步学」Tab 手动配置'}`);
      } else if (auto?.message) {
        message.success(`试题配置已保存。${auto.message}`);
      } else {
        message.success('试题配置已保存');
      }
      queryClient.invalidateQueries({ queryKey: ['class-practice-scope', classId] });
      queryClient.invalidateQueries({ queryKey: ['class-review-sync-config', classId] });
      onClose();
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const suggestMu = useMutation({
    mutationFn: () => trainApi.suggestClassPracticeCourses(classId!),
    onSuccess: (res) => {
      const ids = (res.courses ?? []).map((c) => c.courseId).filter(Boolean);
      if (!ids.length) {
        message.warning('未找到与班级工种匹配的科目，请手动选择');
        return;
      }
      setCourseIds(ids);
      setKnowledgeCourseMap({});
      setActiveCourseId(ids[0]);
      message.success(`已填充 ${ids.length} 个推荐科目`);
    },
    onError: (e: Error) => message.error(e.message || '推荐失败'),
  });

  const onCourseChange = (ids: number[]) => {
    const next = new Set(ids);
    setCourseIds(ids);
    setKnowledgeCourseMap((prev) => {
      const kept: Record<number, number> = {};
      for (const [kid, cid] of Object.entries(prev)) {
        if (next.has(cid)) kept[Number(kid)] = cid;
      }
      return kept;
    });
  };

  const onCheckKnowledge = (checked: Key[] | { checked: Key[]; halfChecked: Key[] }) => {
    if (activeCourseId == null) return;
    const keys = Array.isArray(checked) ? checked : checked.checked;
    const activeKeys = new Set(keys.map(String));
    const treeKeys = new Set(collectKeys(treeData));
    setKnowledgeCourseMap((prev) => {
      const next: Record<number, number> = {};
      for (const [kid, cid] of Object.entries(prev)) {
        const id = Number(kid);
        if (!treeKeys.has(String(id))) {
          next[id] = cid;
        }
      }
      for (const key of activeKeys) {
        const id = Number(key);
        if (!Number.isNaN(id)) next[id] = activeCourseId;
      }
      return next;
    });
  };

  return (
    <Modal
      title={classTitle ? `${titlePrefix}：${classTitle}` : `班级${titlePrefix}`}
      open={open}
      width={720}
      onCancel={onClose}
      destroyOnHidden
      footer={[
        <Button key="cancel" onClick={onClose}>取消</Button>,
        <Button key="save" type="primary" loading={saveMu.isPending} onClick={() => saveMu.mutate()}>
          保存
        </Button>,
      ]}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="配置本班可刷科目、知识点与刷题日期。保存后将自动绑定同步学考试计划并开通名单/灰度；未设置日期则不过期；过了截止日学员无法从班级入口刷题。"
      />
      {scopeQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 32 }}><Spin /></div>
      ) : (
        <Form layout="vertical">
          <Form.Item label="启用练习范围">
            <Switch checked={enabled} onChange={setEnabled} />
            <Text type="secondary" style={{ marginLeft: 12 }}>
              停用后禁止按班刷题（全局刷题入口仍可用）
            </Text>
          </Form.Item>
          <Form.Item
            label="刷题日期"
            extra="按日设置，可不填。清空表示不过期；截止日期当天仍可刷题。"
          >
            <RangePicker
              allowClear
              style={{ width: '100%' }}
              value={practiceRange}
              onChange={(vals) => {
                if (vals?.[0] && vals?.[1]) {
                  setPracticeRange([vals[0], vals[1]]);
                } else {
                  setPracticeRange(null);
                }
              }}
              placeholder={['开始日期（可选）', '截止日期（可选）']}
            />
          </Form.Item>
          <Form.Item
            label={
              <Space>
                <span>科目白名单</span>
                <Button size="small" loading={suggestMu.isPending} onClick={() => suggestMu.mutate()}>
                  按班级工种推荐
                </Button>
              </Space>
            }
          >
            <Select
              mode="multiple"
              allowClear
              placeholder="选择可刷科目"
              options={courseOptions}
              value={courseIds}
              onChange={onCourseChange}
              optionFilterProp="label"
              loading={coursesQuery.isLoading}
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item
            label="知识点白名单（可选）"
            extra="不选知识点表示所选科目下不限知识点；按科目切换树勾选。取消科目会自动清掉该科知识点。"
          >
            {courseIds.length === 0 ? (
              <Text type="secondary">请先选择科目</Text>
            ) : (
              <>
                <Select
                  style={{ width: '100%', marginBottom: 8 }}
                  options={courseIds.map((id) => {
                    const opt = courseOptions.find((o) => o.value === id);
                    return { value: id, label: opt?.label ?? `#${id}` };
                  })}
                  value={activeCourseId}
                  onChange={(v) => setActiveCourseId(v)}
                />
                {treeQuery.isLoading ? (
                  <Spin />
                ) : treeData.length === 0 ? (
                  <Text type="secondary">该科目暂无知识点树</Text>
                ) : (
                  <div style={{ maxHeight: 280, overflow: 'auto', border: '1px solid #f0f0f0', padding: 8 }}>
                    <Tree
                      checkable
                      defaultExpandAll
                      treeData={treeData}
                      checkedKeys={checkedKeysForActive}
                      onCheck={onCheckKnowledge}
                    />
                  </div>
                )}
                <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                  已选知识点合计 {knowledgeIds.length} 个
                </Text>
              </>
            )}
          </Form.Item>
          <Form.Item label="备注">
            <Input.TextArea
              rows={2}
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              placeholder="可选"
              maxLength={500}
            />
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}
