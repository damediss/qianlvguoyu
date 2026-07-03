import { ReloadOutlined } from '@ant-design/icons';
import { Button, Empty, List, Space, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { getCollectionExceptions } from '../../../api/certificationPracticalCollection';
import type { CollectionExceptionVO, SnowflakeId } from '../../../types/certificationPracticalCollection';

interface CollectionExceptionPageProps {
  taskId?: SnowflakeId;
}

export function CollectionExceptionPage({ taskId }: CollectionExceptionPageProps) {
  const exceptionsQuery = useQuery({
    queryKey: ['practical-collection-exceptions', taskId],
    queryFn: () => getCollectionExceptions({ page: 1, pageSize: 50, taskId }),
  });

  return (
    <div className="collection-exception-page">
      <div className="collection-section-heading">
        <div>
          <Typography.Title level={4}>异常记录</Typography.Title>
          <Typography.Text type="secondary">查看待补采、信息异常和异常放行留痕</Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} loading={exceptionsQuery.isFetching} onClick={() => exceptionsQuery.refetch()}>
          刷新
        </Button>
      </div>
      <List<CollectionExceptionVO>
        loading={exceptionsQuery.isFetching}
        dataSource={exceptionsQuery.data?.items || []}
        locale={{ emptyText: <Empty description="暂无异常记录" /> }}
        renderItem={(item) => (
          <List.Item>
            <List.Item.Meta
              title={(
                <Space wrap>
                  <span>{item.candidateName || `考生 ${item.candidateId}`}</span>
                  <Tag color="red">{item.exceptionTypeLabel || item.exceptionType}</Tag>
                  <Tag>{item.exceptionStatusLabel || item.exceptionStatus}</Tag>
                </Space>
              )}
              description={(
                <Space direction="vertical" size={2}>
                  <span>后四位：{item.idCardLast4 || '-'}</span>
                  <span>原因：{item.reason}</span>
                  <span>时间：{item.handledAt || item.createdAt || '-'}</span>
                </Space>
              )}
            />
          </List.Item>
        )}
      />
    </div>
  );
}
