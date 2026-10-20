import { Alert, Button, Card, Input, Select, Space, Table, Tag, Typography, message } from 'antd';
import { ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { fetchScopePlans, scopeBatchSave } from '../../api/exam-zhenti';
import { useAuthStore } from '../../stores/authStore';
import type { ZhentiScopePlanItem } from '../../types/examZhenti';
import { extractErrorMessage } from './pastPaperUtils';

type ScopeFilter = 'all' | 'unbound' | 'multi' | 'invalid';

const FILTER_OPTIONS: { value: ScopeFilter; label: string }[] = [
  { value: 'all', label: '全部计划' },
  { value: 'unbound', label: '只看未绑定' },
  { value: 'multi', label: '只看多绑定' },
  { value: 'invalid', label: '只看失效工种' },
];

/**
 * 真题共享范围（方案 v2.1 §7，Q5 已拍板长期维护）。
 *
 * <p>编辑模型 = 课程绑定：只改 courseIdList，由后端重推导工种/等级，
 * 不直接改 work_type_id（避免派生关系漂移）；变更立即生效，无缓存。
 */
export function PastPaperScopePage() {
  const queryClient = useQueryClient();
  const canManage = useAuthStore((state) => state.hasPermission('exam:zhenti:manage'));

  const [keyword, setKeyword] = useState('');
  const [filter, setFilter] = useState<ScopeFilter>('all');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(50);
  const [drafts, setDrafts] = useState<Record<string, number[]>>({});
  const [saving, setSaving] = useState(false);

  const query = useMemo(
    () => ({ page, pageSize, keyword: keyword || undefined, filter }),
    [page, pageSize, keyword, filter],
  );
  const listQuery = useQuery({
    queryKey: ['zhenti-scope-plans', query],
    queryFn: () => fetchScopePlans(query),
  });
  const records = listQuery.data?.records ?? [];
  const courses = listQuery.data?.courses ?? [];

  const dirtyIds = Object.keys(drafts);

  const startEdit = (record: ZhentiScopePlanItem) => {
    const current = record.bindings
      .map((binding) => binding.courseId)
      .filter((id): id is number => id != null);
    setDrafts((prev) => ({ ...prev, [record.examId]: current }));
  };

  const cancelEdit = (examId: string) => {
    setDrafts((prev) => {
      const next = { ...prev };
      delete next[examId];
      return next;
    });
  };

  const saveRows = async (items: { examId: string; courseIdList: number[] }[]) => {
    if (items.length === 0 || saving) {
      return;
    }
    setSaving(true);
    try {
      await scopeBatchSave(items);
      message.success('已保存，共享范围即时生效');
      setDrafts((prev) => {
        const next = { ...prev };
        for (const item of items) {
          delete next[item.examId];
        }
        return next;
      });
      void queryClient.invalidateQueries({ queryKey: ['zhenti-scope-plans'] });
      void queryClient.invalidateQueries({ queryKey: ['zhenti-scope-info'] });
    } catch (err) {
      message.error(extractErrorMessage(err, '保存失败，请重试'));
    } finally {
      setSaving(false);
    }
  };

  const columns: ColumnsType<ZhentiScopePlanItem> = [
    {
      title: '考试计划',
      dataIndex: 'title',
      ellipsis: { showTitle: false },
      render: (value: string) => (
        <Typography.Text ellipsis={{ tooltip: value }} style={{ maxWidth: 260 }}>
          {value}
        </Typography.Text>
      ),
    },
    {
      title: '共享组绑定（工种 · 等级）',
      width: 320,
      render: (_, record) => {
        if (record.bindings.length === 0) {
          return <Tag color="orange">未绑定</Tag>;
        }
        return (
          <Space size={4} wrap>
            {record.bindings.map((binding, index) => (
              <Tag key={index} color={binding.valid ? 'blue' : 'red'}>
                {binding.valid
                  ? `${binding.workTypeTitle} · ${binding.levelLabel ?? '-'}`
                  : `失效工种#${binding.workTypeId ?? '-'} · ${binding.levelLabel ?? '-'}`}
              </Tag>
            ))}
            {record.multiBinding && <Tag color="warning">多绑定</Tag>}
          </Space>
        );
      },
    },
    {
      title: '课程绑定（保存后重推导工种/等级）',
      width: 360,
      render: (_, record) => {
        const editing = record.examId in drafts;
        const value = editing
          ? drafts[record.examId]
          : record.bindings
              .map((binding) => binding.courseId)
              .filter((id): id is number => id != null);
        return (
          <Select
            mode="multiple"
            allowClear
            style={{ width: '100%' }}
            placeholder="选择课程，将自动回填工种/等级"
            disabled={!canManage || !editing}
            value={value}
            onChange={(next) =>
              setDrafts((prev) => ({ ...prev, [record.examId]: next }))
            }
            options={courses.map((course) => ({
              value: course.id,
              label: course.title,
            }))}
          />
        );
      },
    },
    {
      title: '操作',
      width: 150,
      fixed: 'right',
      render: (_, record) => {
        if (!(record.examId in drafts)) {
          return (
            <Button
              type="link"
              size="small"
              disabled={!canManage}
              onClick={() => startEdit(record)}
            >
              编辑
            </Button>
          );
        }
        return (
          <Space size={0}>
            <Button
              type="link"
              size="small"
              loading={saving}
              onClick={() =>
                saveRows([{ examId: record.examId, courseIdList: drafts[record.examId] }])
              }
            >
              保存
            </Button>
            <Button type="link" size="small" onClick={() => cancelEdit(record.examId)}>
              取消
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <Card
      title="真题共享范围"
      extra={
        <Space>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            disabled={!canManage || dirtyIds.length === 0}
            loading={saving}
            onClick={() =>
              saveRows(
                dirtyIds.map((examId) => ({ examId, courseIdList: drafts[examId] })),
              )
            }
          >
            保存全部修改{dirtyIds.length > 0 ? `（${dirtyIds.length}）` : ''}
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => listQuery.refetch()}
          >
            刷新
          </Button>
        </Space>
      }
    >
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="考试计划按「课程绑定」推导工种与等级；同工种同等级的真题自动共享。绑定保存后立即生效（无缓存）。失效工种需改绑到有效课程。"
        />
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="搜索计划标题"
            style={{ width: 240 }}
            onSearch={(value) => {
              setKeyword(value);
              setPage(1);
            }}
          />
          <Select<ScopeFilter>
            style={{ width: 160 }}
            value={filter}
            options={FILTER_OPTIONS}
            onChange={(value) => {
              setFilter(value);
              setPage(1);
            }}
          />
        </Space>
        <Table<ZhentiScopePlanItem>
          rowKey="examId"
          size="middle"
          loading={listQuery.isLoading || listQuery.isFetching}
          columns={columns}
          dataSource={records}
          scroll={{ x: 960 }}
          pagination={{
            current: page,
            pageSize,
            total: listQuery.data?.total ?? 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (nextPage, nextSize) => {
              setPage(nextPage);
              setPageSize(nextSize);
            },
          }}
        />
        {!canManage && (
          <Typography.Text type="secondary">
            当前账号仅有查看权限，绑定编辑与保存不可用。
          </Typography.Text>
        )}
      </Space>
    </Card>
  );
}
