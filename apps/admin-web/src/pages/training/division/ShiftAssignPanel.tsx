/**
 * 二期：排课分班（L2）— 解析企业排班表 → 预检/生成 → 绑定培训班
 */
import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Collapse,
  Form,
  InputNumber,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery } from '@tanstack/react-query';
import { CalendarOutlined, LinkOutlined } from '@ant-design/icons';
import * as trainApi from '../../../api/training';
import type {
  ShiftAssignBinding,
  ShiftAssignRecordItem,
  ShiftAssignTaskResult,
} from '../../../types/training';

const { Text } = Typography;

interface Props {
  planId: number;
  trainInfoId: number | null;
  shiftFileName?: string;
}

export function ShiftAssignPanel({ planId, trainInfoId, shiftFileName }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [classMin, setClassMin] = useState(30);
  const [classMax, setClassMax] = useState(50);
  const [task, setTask] = useState<ShiftAssignTaskResult | null>(null);
  const [bindAlgo, setBindAlgo] = useState<string>();
  const [bindClassId, setBindClassId] = useState<number>();

  const payload = useMemo(() => ({
    planId,
    trainInfoId: trainInfoId ?? undefined,
    year,
    month,
    adapterType: 'T0',
    classMin,
    classMax,
  }), [planId, trainInfoId, year, month, classMin, classMax]);

  const classesQuery = useQuery({
    queryKey: ['shift-assign-info-classes', trainInfoId],
    queryFn: () => trainApi.pageInfoClasses({ trainInfoId: trainInfoId!, page: 1, pageSize: 100 }),
    enabled: !!trainInfoId,
  });

  const previewMu = useMutation({
    mutationFn: () => trainApi.previewShiftAssignAdapter(payload),
    onSuccess: (res) => {
      message.success(`预览成功：${res.personCount ?? 0} 人 / ${res.slotCount ?? 0} 人日槽`);
      if (res.warnings?.length) {
        message.warning(`有 ${res.warnings.length} 条解析警告`);
      }
    },
    onError: (e: Error) => message.error(e.message || '预览失败'),
  });

  const precheckMu = useMutation({
    mutationFn: () => trainApi.precheckShiftAssign(payload),
    onSuccess: (res) => {
      message.success(`预检完成：约 ${res.recordCount ?? 0} 条排课明细，算法班 ${res.algoClassNames?.length ?? 0} 个`);
    },
    onError: (e: Error) => message.error(e.message || '预检失败'),
  });

  const generateMu = useMutation({
    mutationFn: () => trainApi.generateShiftAssign(payload),
    onSuccess: (res) => {
      setTask(res);
      message.success(`已生成排课任务 #${res.taskId}，明细 ${res.recordCount ?? 0} 条`);
    },
    onError: (e: Error) => message.error(e.message || '生成失败'),
  });

  const bindMu = useMutation({
    mutationFn: () => trainApi.bindShiftAssignClass(task!.taskId, {
      algoClassName: bindAlgo!,
      trainClassId: bindClassId!,
    }),
    onSuccess: (res) => {
      setTask(res);
      message.success(`已绑定：${res.boundAlgoClassName} → 班级 #${res.boundTrainClassId}`);
    },
    onError: (e: Error) => message.error(e.message || '绑定失败'),
  });

  const recordColumns: ColumnsType<ShiftAssignRecordItem> = [
    { title: '姓名', dataIndex: 'personName', width: 90 },
    { title: '日期', dataIndex: 'assignDate', width: 110 },
    { title: '时段', dataIndex: 'slot', width: 70 },
    { title: '算法班', dataIndex: 'algoClassName', width: 110 },
    { title: '部门', dataIndex: 'dept', width: 100, ellipsis: true },
    { title: '班别', dataIndex: 'shiftClass', width: 80 },
    {
      title: '培训班',
      dataIndex: 'trainClassId',
      width: 90,
      render: (v?: number) => (v ? <Tag color="blue">#{v}</Tag> : <Text type="secondary">未绑</Text>),
    },
  ];

  const bindColumns: ColumnsType<ShiftAssignBinding> = [
    { title: '算法班', dataIndex: 'algoClassName' },
    { title: '明细数', dataIndex: 'recordCount', width: 90 },
    {
      title: '培训班ID',
      dataIndex: 'trainClassId',
      width: 110,
      render: (v?: number) => (v != null ? v : '-'),
    },
  ];

  const classOptions = (classesQuery.data?.items ?? []).map((c) => ({
    value: c.id,
    label: `${c.title || '未命名'} (#${c.id})`,
  }));

  return (
    <Collapse
      style={{ marginTop: 16 }}
      items={[
        {
          key: 'shift-assign',
          label: (
            <Space>
              <CalendarOutlined />
              <span>排课分班（按企业排班表 · L2）</span>
              {task?.taskId ? <Tag color="success">任务 #{task.taskId}</Tag> : <Tag>二期</Tag>}
            </Space>
          ),
          children: (
            <div>
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
                message="使用企业回传的排班表（标准「姓名 + 日期列」）生成上午/下午算法班，再绑定到已有培训班级。不替代上方组织分班。"
                description={shiftFileName ? `当前排班文件：${shiftFileName}` : '请确认规划已上传排班表'}
              />

              <Form layout="inline" style={{ marginBottom: 16 }}>
                <Form.Item label="年">
                  <InputNumber min={2020} max={2100} value={year} onChange={(v) => setYear(v || now.getFullYear())} />
                </Form.Item>
                <Form.Item label="月">
                  <InputNumber min={1} max={12} value={month} onChange={(v) => setMonth(v || 1)} />
                </Form.Item>
                <Form.Item label="班额">
                  <Space>
                    <InputNumber min={5} max={80} value={classMin} onChange={(v) => setClassMin(v || 30)} />
                    <span>~</span>
                    <InputNumber min={5} max={100} value={classMax} onChange={(v) => setClassMax(v || 50)} />
                  </Space>
                </Form.Item>
                <Form.Item>
                  <Space wrap>
                    <Button loading={previewMu.isPending} onClick={() => previewMu.mutate()}>
                      Adapter 预览
                    </Button>
                    <Button loading={precheckMu.isPending} onClick={() => precheckMu.mutate()}>
                      预检
                    </Button>
                    <Button
                      type="primary"
                      loading={generateMu.isPending}
                      disabled={!trainInfoId}
                      onClick={() => generateMu.mutate()}
                    >
                      生成排课分班
                    </Button>
                  </Space>
                </Form.Item>
              </Form>

              {!trainInfoId && (
                <Alert type="warning" showIcon style={{ marginBottom: 16 }} message="请先完成「培训信息」步骤，再生成排课分班以便绑定班级。" />
              )}

              {(precheckMu.data || previewMu.data) && !task && (
                <Alert
                  type="success"
                  showIcon
                  style={{ marginBottom: 16 }}
                  message={
                    precheckMu.data
                      ? `预检：算法班 ${precheckMu.data.algoClassNames?.join('、') || '无'}；未排入 ${precheckMu.data.unassignedPersons?.length ?? 0} 人`
                      : `预览：${previewMu.data?.personCount ?? 0} 人`
                  }
                />
              )}

              {task && (
                <>
                  <Space style={{ marginBottom: 12 }} wrap>
                    <Tag color="blue">版本 v{task.version}</Tag>
                    <Tag>明细 {task.recordCount ?? 0}</Tag>
                    <Tag>算法班 {(task.algoClassNames ?? []).length}</Tag>
                    <Tag color="orange">未排入 {task.unassignedPersons?.length ?? 0}</Tag>
                  </Space>

                  <Form layout="inline" style={{ marginBottom: 16 }}>
                    <Form.Item label="算法班">
                      <Select
                        style={{ width: 160 }}
                        placeholder="选择算法班"
                        options={(task.algoClassNames ?? []).map((n) => ({ value: n, label: n }))}
                        value={bindAlgo}
                        onChange={setBindAlgo}
                        allowClear
                      />
                    </Form.Item>
                    <Form.Item label="培训班级">
                      <Select
                        style={{ width: 240 }}
                        placeholder={trainInfoId ? '选择培训班' : '先生成培训信息'}
                        options={classOptions}
                        value={bindClassId}
                        onChange={setBindClassId}
                        allowClear
                        disabled={!trainInfoId}
                      />
                    </Form.Item>
                    <Form.Item>
                      <Button
                        icon={<LinkOutlined />}
                        type="primary"
                        ghost
                        loading={bindMu.isPending}
                        disabled={!bindAlgo || !bindClassId}
                        onClick={() => bindMu.mutate()}
                      >
                        绑定班级
                      </Button>
                    </Form.Item>
                  </Form>

                  <Table<ShiftAssignBinding>
                    size="small"
                    rowKey="algoClassName"
                    columns={bindColumns}
                    dataSource={task.bindings ?? []}
                    pagination={false}
                    style={{ marginBottom: 16 }}
                  />

                  <Table<ShiftAssignRecordItem>
                    size="small"
                    rowKey="id"
                    columns={recordColumns}
                    dataSource={task.records ?? []}
                    pagination={{ pageSize: 20, showSizeChanger: false }}
                    scroll={{ x: 800 }}
                  />
                </>
              )}
            </div>
          ),
        },
      ]}
    />
  );
}
