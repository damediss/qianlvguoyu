import {
  ReloadOutlined,
  SearchOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import {
  Button,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { pageAuditLogs } from '../../api/system';
import type { AuditLogVO } from '../../types/system';

const ACTION_COLORS: Record<string, string> = {
  CREATE: 'green',
  UPDATE: 'blue',
  DELETE: 'red',
  LOGIN: 'cyan',
  LOGOUT: 'orange',
};

export function AuditLogPage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({
    page: 1,
    pageSize: 20,
    module: '',
    action: '',
    targetType: '',
  });

  const listQuery = useQuery({
    queryKey: ['system-audit-logs', query],
    queryFn: () => pageAuditLogs(query),
  });

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;

  const columns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (v: string) => v ? new Date(v).toLocaleString('zh-CN') : '-',
    },
    {
      title: '模块',
      dataIndex: 'module',
      width: 120,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 200,
      render: (v: string) => (
        <Tag color={ACTION_COLORS[v] || 'default'}>{v}</Tag>
      ),
    },
    {
      title: '目标类型',
      dataIndex: 'targetType',
      width: 120,
    },
    {
      title: '目标ID',
      dataIndex: 'targetId',
      width: 140,
      render: (v: number) => v ? <Typography.Text code>{v}</Typography.Text> : '-',
    },
    {
      title: '用户ID',
      dataIndex: 'userId',
      width: 120,
    },
    {
      title: 'IP 地址',
      dataIndex: 'ip',
      width: 140,
    },
    {
      title: '请求ID',
      dataIndex: 'requestId',
      ellipsis: true,
      render: (text: string) => (
        <Typography.Text copyable={{ text }} style={{ fontSize: 12, color: '#64748b' }}>
          {text ? text.slice(0, 20) + '...' : '-'}
        </Typography.Text>
      ),
    },
  ];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><FileTextOutlined /></span>
            <Typography.Title level={1}>审计日志</Typography.Title>
          </div>
          <Typography.Text>
            记录系统中所有关键操作的审计轨迹，用于安全审计和问题排查。
          </Typography.Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => queryClient.invalidateQueries({ queryKey: ['system-audit-logs'] })}
        >
          刷新
        </Button>
      </section>

      <div style={{ padding: '0 24px' }}>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="模块名称"
            allowClear
            style={{ width: 180 }}
            value={query.module}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, module: e.target.value, page: 1 }))
            }
          />
          <Select
            placeholder="操作类型"
            allowClear
            style={{ width: 140 }}
            value={query.action || undefined}
            onChange={(v) => setQuery((prev) => ({ ...prev, action: v || '', page: 1 }))}
            options={[
              { label: 'CREATE', value: 'CREATE' },
              { label: 'UPDATE', value: 'UPDATE' },
              { label: 'DELETE', value: 'DELETE' },
              { label: 'LOGIN', value: 'LOGIN' },
              { label: 'LOGOUT', value: 'LOGOUT' },
            ]}
          />
          <Input
            placeholder="目标类型"
            allowClear
            style={{ width: 160 }}
            value={query.targetType}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, targetType: e.target.value, page: 1 }))
            }
          />
          <Button
            icon={<SearchOutlined />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['system-audit-logs'] })}
          >
            搜索
          </Button>
        </Space>
        <Table
          rowKey="id"
          size="middle"
          loading={listQuery.isLoading}
          dataSource={items}
          columns={columns}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (page, pageSize) =>
              setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>
    </div>
  );
}
