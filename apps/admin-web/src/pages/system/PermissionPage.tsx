import {
  ReloadOutlined,
  SafetyCertificateOutlined,
  UnorderedListOutlined,
  TableOutlined,
} from '@ant-design/icons';
import {
  Button,
  Collapse,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { getPermissionMatrix, getPermissionList } from '../../api/system';
import type { PermissionMatrixVO, PermissionItemVO, RoleBriefVO } from '../../types/system';

export function PermissionPage() {
  const queryClient = useQueryClient();
  const [viewMode, setViewMode] = useState<'matrix' | 'list'>('matrix');

  const matrixQuery = useQuery({
    queryKey: ['system-perm-matrix'],
    queryFn: getPermissionMatrix,
  });

  const listQuery = useQuery({
    queryKey: ['system-perm-list'],
    queryFn: getPermissionList,
  });

  const matrixData = matrixQuery.data || [];
  const listData = listQuery.data || [];

  // Matrix view columns
  const matrixPermColumns = [
    {
      title: '权限名称',
      dataIndex: 'name',
      width: 140,
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    {
      title: '权限码',
      dataIndex: 'code',
      width: 180,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 100,
      render: (v: string) => {
        const map: Record<string, { color: string; text: string }> = {
          view: { color: 'blue', text: '查看' },
          create: { color: 'green', text: '创建' },
          update: { color: 'orange', text: '更新' },
          delete: { color: 'red', text: '删除' },
        };
        const item = map[v] || { color: 'default', text: v };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v: string) =>
        v === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    {
      title: '绑定角色',
      dataIndex: 'boundRoles',
      render: (roles: RoleBriefVO[]) =>
        roles?.length ? (
          <Space size={4} wrap>
            {roles.map((r) => (
              <Tag key={r.id} color="processing">{r.name}</Tag>
            ))}
          </Space>
        ) : (
          <Typography.Text type="secondary">未绑定</Typography.Text>
        ),
    },
  ];

  // List view columns
  const listColumns = [
    {
      title: '序号',
      width: 70,
      render: (_: unknown, __: unknown, index: number) => index + 1,
    },
    {
      title: '权限名称',
      dataIndex: 'name',
      width: 140,
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    {
      title: '权限码',
      dataIndex: 'code',
      width: 180,
      render: (text: string) => <Typography.Text code>{text}</Typography.Text>,
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 80,
      render: (v: string) => {
        const map: Record<string, { color: string; text: string }> = {
          view: { color: 'blue', text: '查看' },
          create: { color: 'green', text: '创建' },
          update: { color: 'orange', text: '更新' },
          delete: { color: 'red', text: '删除' },
        };
        const item = map[v] || { color: 'default', text: v };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '所属模块',
      dataIndex: 'menuName',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v: string) =>
        v === 'enabled' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    {
      title: '绑定角色',
      dataIndex: 'boundRoles',
      render: (roles: RoleBriefVO[]) =>
        roles?.length ? (
          <Space size={4} wrap>
            {roles.map((r) => (
              <Tag key={r.id} color="processing">{r.name}</Tag>
            ))}
          </Space>
        ) : (
          <Typography.Text type="secondary">未绑定</Typography.Text>
        ),
    },
  ];

  // Stats
  const totalPermissions = matrixData.reduce((acc, g) => acc + g.permissions.length, 0);
  const totalModules = matrixData.length;

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><SafetyCertificateOutlined /></span>
            <Typography.Title level={1}>权限管理</Typography.Title>
          </div>
          <Typography.Text>
            查看系统中所有权限点的分布和角色绑定情况。共 {totalModules} 个模块，{totalPermissions} 个权限点。
          </Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ['system-perm-matrix'] });
              queryClient.invalidateQueries({ queryKey: ['system-perm-list'] });
            }}
          >
            刷新
          </Button>
          <Select
            value={viewMode}
            onChange={setViewMode}
            style={{ width: 130 }}
            options={[
              {
                label: (
                  <Space size={4}>
                    <TableOutlined /> 矩阵视图
                  </Space>
                ),
                value: 'matrix',
              },
              {
                label: (
                  <Space size={4}>
                    <UnorderedListOutlined /> 列表视图
                  </Space>
                ),
                value: 'list',
              },
            ]}
          />
        </Space>
      </section>

      <div style={{ padding: '0 24px' }}>
        {viewMode === 'matrix' ? (
          <Collapse
            defaultActiveKey={matrixData.map((g: PermissionMatrixVO) => String(g.menuId))}
            items={matrixData.map((group: PermissionMatrixVO) => ({
              key: String(group.menuId),
              label: (
                <Space>
                  <Typography.Text strong>{group.menuName}</Typography.Text>
                  <Tag>{group.permissions.length} 个权限</Tag>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {group.menuCode}
                  </Typography.Text>
                </Space>
              ),
              children: (
                <Table
                  rowKey="id"
                  size="small"
                  dataSource={group.permissions}
                  columns={matrixPermColumns}
                  pagination={false}
                />
              ),
            }))}
          />
        ) : (
          <Table
            rowKey="id"
            size="middle"
            loading={listQuery.isLoading}
            dataSource={listData}
            columns={listColumns}
            pagination={{
              pageSize: 50,
              showSizeChanger: true,
              showTotal: (t) => `共 ${t} 条`,
            }}
          />
        )}
      </div>
    </div>
  );
}
