import { useMemo, useState } from 'react';
import { Card, Col, Empty, Input, Pagination, Row, Space, Spin, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import type { TrainInfoClassListItem } from '../../types/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import { FollowClassWorkbench } from './follow/FollowClassWorkbench';
import './TrainingFollowClassPage.css';

export function TrainingFollowClassPage() {
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [activeClass, setActiveClass] = useState<TrainInfoClassListItem | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['follow-class-list', { page, keyword }],
    queryFn: () =>
      trainApi.pageInfoClassList({
        page,
        pageSize: 20,
        keyword: keyword.trim() || undefined,
      }),
    enabled: !activeClass,
  });

  const rows = useMemo(() => data?.list ?? [], [data?.list]);

  if (activeClass) {
    return (
      <div className="training-list-container training-follow-class-page">
        <FollowClassWorkbench classItem={activeClass} onBack={() => setActiveClass(null)} />
      </div>
    );
  }

  return (
    <div className="training-list-container training-follow-class-page">
      <section className="page-heading">
        <div>
          <Typography.Title level={4} style={{ marginBottom: 4 }}>
            跟班
          </Typography.Title>
          <Typography.Text type="secondary">
            开班后过程管控：选择班级进入工作台（考勤、角色、日志等）
          </Typography.Text>
        </div>
      </section>

      <TrainingProcessSteps current={5} />

      <Card
        size="small"
        title="在培班级"
        extra={
          <Space>
            <Input.Search
              allowClear
              placeholder="搜索班级 / 企业 / 工种"
              style={{ width: 260 }}
              onSearch={(v) => {
                setKeyword(v);
                setPage(1);
              }}
            />
          </Space>
        }
      >
        <Spin spinning={isLoading}>
          {rows.length === 0 && !isLoading ? (
            <Empty description="暂无班级数据" />
          ) : (
            <Row gutter={[16, 16]} className="follow-class-card-grid">
              {rows.map((row) => (
                <Col key={String(row.id)} xs={24} sm={12} md={8} lg={6}>
                  <button
                    type="button"
                    className="follow-class-card"
                    onClick={() => setActiveClass(row)}
                  >
                    <div className="follow-class-card-body">
                      <Typography.Text className="follow-class-card-title" ellipsis={{ tooltip: row.title || '未命名班级' }}>
                        {row.title || '未命名班级'}
                      </Typography.Text>
                      <Typography.Text type="secondary" className="follow-class-card-company" ellipsis={{ tooltip: row.company_name || '—' }}>
                        {row.company_name || '—'}
                      </Typography.Text>
                      <Typography.Text type="secondary" className="follow-class-card-meta">
                        {row.nums ?? 0} 人
                      </Typography.Text>
                    </div>
                  </button>
                </Col>
              ))}
            </Row>
          )}
        </Spin>

        {(data?.total ?? 0) > 0 && (
          <div className="follow-class-card-pagination">
            <Pagination
              current={page}
              pageSize={20}
              total={data?.total ?? 0}
              showTotal={(total) => `共 ${total} 个班级`}
              onChange={setPage}
              showSizeChanger={false}
            />
          </div>
        )}
      </Card>
    </div>
  );
}
