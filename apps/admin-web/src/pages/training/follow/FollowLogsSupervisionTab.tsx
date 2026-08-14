/**
 * 跟班 · 日志·督导
 * 过程动态（自动）/ 工作日志（手写）/ 督导检查与整改（L5）
 */
import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  Modal,
  Segmented,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../../api/training';
import type { FollowOpLog, FollowWorkLog } from '../../../api/training';
import { FollowSupervisionPanel } from './FollowSupervisionPanel';

type Props = {
  classId: number;
  defaultBizDate?: Dayjs | null;
};

type Section = 'ops' | 'work' | 'supervision';

const CATEGORY_OPTIONS = [
  { value: 'attendance', label: '考勤' },
  { value: 'evidence', label: '影像' },
  { value: 'schedule', label: '课表' },
  { value: 'work_log', label: '工作日志' },
  { value: 'task', label: '任务' },
  { value: 'role', label: '角色' },
  { value: 'supervision', label: '督导' },
];

function severityTag(level?: string) {
  if (level === 'critical') return <Tag color="error">严重</Tag>;
  if (level === 'warn') return <Tag color="warning">警示</Tag>;
  return <Tag>信息</Tag>;
}

function statusTag(status?: string) {
  if (status === 'draft') return <Tag>草稿</Tag>;
  return <Tag color="success">已发布</Tag>;
}

export function FollowLogsSupervisionTab({ classId, defaultBizDate }: Props) {
  const qc = useQueryClient();
  const [section, setSection] = useState<Section>('ops');
  const [bizDate, setBizDate] = useState<Dayjs | null>(defaultBizDate ?? dayjs());
  const [category, setCategory] = useState<string | undefined>();
  const [warnOnly, setWarnOnly] = useState(false);

  const [logModalOpen, setLogModalOpen] = useState(false);
  const [editing, setEditing] = useState<FollowWorkLog | null>(null);
  const [logForm] = Form.useForm();

  const dayStr = bizDate ? bizDate.format('YYYY-MM-DD') : undefined;

  const opQuery = useQuery({
    queryKey: ['follow-op-logs', classId, dayStr, category, warnOnly],
    queryFn: () =>
      trainApi.listFollowOpLogs(classId, {
        bizDate: dayStr,
        category,
        minSeverity: warnOnly ? 'warn' : undefined,
      }),
    enabled: section === 'ops',
  });

  const workQuery = useQuery({
    queryKey: ['follow-work-logs', classId, dayStr],
    queryFn: () => trainApi.listFollowWorkLogs(classId, { bizDate: dayStr }),
    enabled: section === 'work',
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['follow-op-logs', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-work-logs', classId] });
  };

  const createMut = useMutation({
    mutationFn: (payload: Parameters<typeof trainApi.createFollowWorkLog>[1]) =>
      trainApi.createFollowWorkLog(classId, payload),
    onSuccess: () => {
      message.success('日志已保存');
      setLogModalOpen(false);
      logForm.resetFields();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '保存失败'),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, payload }: { id: string | number; payload: Parameters<typeof trainApi.updateFollowWorkLog>[1] }) =>
      trainApi.updateFollowWorkLog(id, payload),
    onSuccess: () => {
      message.success('已更新');
      setLogModalOpen(false);
      setEditing(null);
      logForm.resetFields();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '更新失败'),
  });

  const deleteMut = useMutation({
    mutationFn: (id: string | number) => trainApi.deleteFollowWorkLog(id),
    onSuccess: () => {
      message.success('已删除');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '删除失败'),
  });

  const draftMut = useMutation({
    mutationFn: () => {
      if (!dayStr) throw new Error('请先选择业务日');
      return trainApi.draftFollowWorkLogFromOps(classId, dayStr);
    },
    onSuccess: (row) => {
      message.success('已根据当日动态生成草稿');
      setSection('work');
      setEditing(row);
      logForm.setFieldsValue({
        date: dayjs(row.bizDate),
        author: row.authorName,
        attendanceSummary: row.attendanceSummary,
        exceptionNote: row.exceptionNote,
        content: row.content,
        status: row.status || 'draft',
      });
      setLogModalOpen(true);
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '生成草稿失败'),
  });

  const openCreate = () => {
    setEditing(null);
    logForm.setFieldsValue({
      date: bizDate ?? dayjs(),
      author: '班主任',
      attendanceSummary: '',
      exceptionNote: '',
      content: '',
      status: 'published',
    });
    setLogModalOpen(true);
  };

  const openEdit = (row: FollowWorkLog) => {
    setEditing(row);
    logForm.setFieldsValue({
      date: dayjs(row.bizDate),
      author: row.authorName,
      attendanceSummary: row.attendanceSummary,
      exceptionNote: row.exceptionNote,
      content: row.content,
      status: row.status || 'published',
    });
    setLogModalOpen(true);
  };

  const handleSave = async () => {
    const values = await logForm.validateFields();
    const payload = {
      bizDate: values.date ? dayjs(values.date).format('YYYY-MM-DD') : dayStr || dayjs().format('YYYY-MM-DD'),
      authorName: values.author,
      attendanceSummary: values.attendanceSummary,
      exceptionNote: values.exceptionNote,
      content: values.content,
      status: values.status || 'published',
    };
    if (editing) {
      updateMut.mutate({ id: editing.id, payload });
    } else {
      createMut.mutate(payload);
    }
  };

  const opColumns: ColumnsType<FollowOpLog> = useMemo(
    () => [
      {
        title: '时间',
        dataIndex: 'createdAt',
        width: 170,
        render: (v?: string) => v || '—',
      },
      {
        title: '级别',
        dataIndex: 'severityLevel',
        width: 80,
        render: (v?: string) => severityTag(v),
      },
      {
        title: '分类',
        dataIndex: 'eventCategory',
        width: 90,
      },
      {
        title: '操作',
        dataIndex: 'title',
        width: 160,
      },
      {
        title: '摘要',
        dataIndex: 'summary',
        ellipsis: true,
        render: (v?: string) => v || '—',
      },
      {
        title: '操作人',
        dataIndex: 'actorName',
        width: 120,
        render: (_: unknown, row) =>
          [row.actorName, row.actorRole].filter(Boolean).join(' · ') || '—',
      },
      {
        title: '事件码',
        dataIndex: 'eventCode',
        width: 180,
        render: (v: string) => <Typography.Text type="secondary" style={{ fontSize: 12 }}>{v}</Typography.Text>,
      },
    ],
    [],
  );

  const workColumns: ColumnsType<FollowWorkLog> = useMemo(
    () => [
      { title: '日期', dataIndex: 'bizDate', width: 120 },
      { title: '记录人', dataIndex: 'authorName', width: 120 },
      {
        title: '状态',
        dataIndex: 'status',
        width: 90,
        render: (v?: string) => statusTag(v),
      },
      {
        title: '出勤概况',
        dataIndex: 'attendanceSummary',
        ellipsis: true,
        width: 180,
        render: (v?: string) => v || '—',
      },
      {
        title: '内容',
        dataIndex: 'content',
        ellipsis: true,
        render: (v?: string) => v || '—',
      },
      {
        title: '写入时间',
        dataIndex: 'createdAt',
        width: 170,
      },
      {
        title: '操作',
        width: 140,
        render: (_: unknown, row) => (
          <Space>
            <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>
              编辑
            </Button>
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => {
                Modal.confirm({
                  title: '删除该工作日志？',
                  onOk: () => deleteMut.mutate(row.id),
                });
              }}
            >
              删除
            </Button>
          </Space>
        ),
      },
    ],
    [deleteMut],
  );

  return (
    <div>
      <Space wrap style={{ marginBottom: 12 }} size="middle">
        <Segmented
          value={section}
          onChange={(v) => setSection(v as Section)}
          options={[
            { label: '过程动态', value: 'ops' },
            { label: '工作日志', value: 'work' },
            { label: '督导检查', value: 'supervision' },
          ]}
        />
        <DatePicker
          value={bizDate}
          onChange={(d) => setBizDate(d)}
          allowClear
          placeholder="业务日"
        />
        {section === 'ops' && (
          <>
            <Select
              style={{ width: 140 }}
              value={category}
              onChange={setCategory}
              options={CATEGORY_OPTIONS}
              placeholder="分类"
              allowClear
            />
            <Select
              style={{ width: 120 }}
              value={warnOnly ? 'warn' : 'all'}
              onChange={(v) => setWarnOnly(v === 'warn')}
              options={[
                { value: 'all', label: '全部级别' },
                { value: 'warn', label: '仅警示+' },
              ]}
            />
            <Button icon={<ReloadOutlined />} onClick={() => void opQuery.refetch()}>
              刷新
            </Button>
          </>
        )}
        {section === 'work' && (
          <>
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              新增工作日志
            </Button>
            <Button
              icon={<ThunderboltOutlined />}
              loading={draftMut.isPending}
              onClick={() => draftMut.mutate()}
              disabled={!dayStr}
            >
              根据当日动态生成草稿
            </Button>
            <Button icon={<ReloadOutlined />} onClick={() => void workQuery.refetch()}>
              刷新
            </Button>
          </>
        )}
      </Space>

      {section === 'ops' && (
        <>
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message="过程动态由系统自动记录跟班域约定写操作（考勤、影像、课表、作业、工作日志等），不含页面浏览与未登记接口。"
            description="默认按「操作发生日」筛选；布置作业若挂了其它课次，仍会出现在今天。可清空日期查看更多。"
          />
          <Table
            rowKey="id"
            size="small"
            loading={opQuery.isLoading}
            dataSource={opQuery.data || []}
            columns={opColumns}
            pagination={{ pageSize: 20 }}
            locale={{
              emptyText: dayStr
                ? '当日暂无过程动态；完成跟班操作后将自动出现'
                : '暂无过程动态；完成跟班操作后将自动出现',
            }}
            rowClassName={(row) =>
              row.severityLevel === 'warn' || row.severityLevel === 'critical' ? 'follow-op-warn-row' : ''
            }
          />
        </>
      )}

      {section === 'work' && (
        <Table
          rowKey="id"
          size="small"
          loading={workQuery.isLoading}
          dataSource={workQuery.data || []}
          columns={workColumns}
          pagination={false}
          locale={{ emptyText: '暂无工作日志' }}
        />
      )}

      {section === 'supervision' && <FollowSupervisionPanel classId={classId} bizDate={bizDate} />}

      <Modal
        title={editing ? '编辑工作日志' : '新增工作日志'}
        open={logModalOpen}
        onCancel={() => {
          setLogModalOpen(false);
          setEditing(null);
        }}
        onOk={handleSave}
        confirmLoading={createMut.isPending || updateMut.isPending}
        destroyOnHidden
        width={640}
      >
        <Form form={logForm} layout="vertical">
          <Form.Item name="date" label="日期" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="author" label="记录人" rules={[{ required: true, message: '请输入记录人' }]}>
            <Input placeholder="班主任 / 跟班教务" />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue="published">
            <Select
              options={[
                { value: 'published', label: '已发布' },
                { value: 'draft', label: '草稿' },
              ]}
            />
          </Form.Item>
          <Form.Item name="attendanceSummary" label="出勤概况">
            <Input.TextArea rows={2} placeholder="当日出勤概况…" />
          </Form.Item>
          <Form.Item name="exceptionNote" label="异常与处理">
            <Input.TextArea rows={2} placeholder="异常学员、沟通与处理…" />
          </Form.Item>
          <Form.Item
            name="content"
            label="内容"
            rules={[
              {
                validator: async (_, value) => {
                  const a = logForm.getFieldValue('attendanceSummary');
                  const e = logForm.getFieldValue('exceptionNote');
                  if (!value && !a && !e) {
                    throw new Error('请填写内容、出勤概况或异常说明至少一项');
                  }
                },
              },
            ]}
          >
            <Input.TextArea rows={5} placeholder="工作日志正文…" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
