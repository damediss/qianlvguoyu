import { ReloadOutlined, SearchOutlined, SolutionOutlined } from '@ant-design/icons';
import { Button, Input, Space, Table, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { pageTalentTraining } from '../../api/industrial';
import type { IndustrialTalentTraining } from '../../types/industrial';

export function TalentTrainingPage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 10, keyword: '', workType: '' });

  const listQuery = useQuery({
    queryKey: ['industrial-talent-training', query],
    queryFn: () => pageTalentTraining(query),
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['industrial-talent-training'] });
  }, [queryClient]);

  const columns = useMemo<TableColumnsType<IndustrialTalentTraining>>(() => [
    { title: '企业名称', dataIndex: 'name', minWidth: 120, ellipsis: true },
    { title: '职称', dataIndex: 'level', width: 120 },
    {
      title: '工种',
      dataIndex: 'workType',
      width: 120,
      render: (_: unknown, record: IndustrialTalentTraining) => record.workType || record.work_type || '',
    },
    { title: '岗位', dataIndex: 'zhiwei', width: 120 },
    { title: '学历要求', dataIndex: 'education', width: 150 },
  ], []);

  const items = listQuery.data?.items || [];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><SolutionOutlined /></span>
            <Typography.Title level={1}>园区企业人才培养列表</Typography.Title>
          </div>
          <Typography.Text>人才培训计划与进度追踪，展示园区企业招聘培养需求。</Typography.Text>
        </div>
        <Space wrap>
          <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
        </Space>
      </section>

      <div style={{ padding: '0 24px 24px' }}>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索企业、职称、工种、岗位、学历"
            allowClear
            style={{ width: 320 }}
            value={query.keyword}
            onChange={(event) => setQuery((prev) => ({ ...prev, keyword: event.target.value, page: 1 }))}
            onPressEnter={refresh}
          />
          <Input
            placeholder="工种"
            allowClear
            style={{ width: 160 }}
            value={query.workType}
            onChange={(event) => setQuery((prev) => ({ ...prev, workType: event.target.value, page: 1 }))}
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
