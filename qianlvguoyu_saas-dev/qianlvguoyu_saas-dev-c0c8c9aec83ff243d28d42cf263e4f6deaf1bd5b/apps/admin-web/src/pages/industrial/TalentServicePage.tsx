import { ReloadOutlined, SearchOutlined, TeamOutlined } from '@ant-design/icons';
import { Button, Input, Space, Table, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { pageTalentService } from '../../api/industrial';
import type { IndustrialTalentService } from '../../types/industrial';

export function TalentServicePage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: '', source: '' });

  const listQuery = useQuery({
    queryKey: ['industrial-talent-service', query],
    queryFn: () => pageTalentService(query),
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['industrial-talent-service'] });
  }, [queryClient]);

  const columns = useMemo<TableColumnsType<IndustrialTalentService>>(() => [
    { title: '标题', dataIndex: 'title', minWidth: 120, ellipsis: true },
    { title: '来源', dataIndex: 'source', width: 120 },
    { title: '开始日期', dataIndex: 'date', width: 150 },
  ], []);

  const items = listQuery.data?.items || [];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><TeamOutlined /></span>
            <Typography.Title level={1}>人才服务</Typography.Title>
          </div>
          <Typography.Text>技师认定、职称论文、证书补贴等服务事项展示。</Typography.Text>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px 24px' }}>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索标题、来源、日期"
            allowClear
            style={{ width: 320 }}
            value={query.keyword}
            onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value, page: 1 }))}
            onPressEnter={refresh}
          />
          <Input
            placeholder="来源"
            allowClear
            style={{ width: 160 }}
            value={query.source}
            onChange={(event) => setQuery((prev) => ({ ...prev, source: event.target.value, page: 1 }))}
            onPressEnter={refresh}
          />
          <Button icon={<SearchOutlined />} onClick={refresh}>搜索</Button>
        </Space>

        <Table
          rowKey="id"
          size="middle"
          bordered
          loading={listQuery.isLoading}
          dataSource={items}
          columns={columns}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total: listQuery.data?.total || 0,
            showSizeChanger: true,
            showTotal: (count) => `共 ${count} 条`,
            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>
    </div>
  );
}
