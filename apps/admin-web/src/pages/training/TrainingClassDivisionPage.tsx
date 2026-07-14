/**
 * 分班 — 占位页（功能待开发）
 */
import { Card, Empty, Typography } from 'antd';

export function TrainingClassDivisionPage() {
  return (
    <div className="training-page">
      <section className="page-heading">
        <Typography.Title level={4}>分班</Typography.Title>
        <Typography.Text type="secondary">按培训项目划分班级并分配学员</Typography.Text>
      </section>

      <Card bordered={false}>
        <Empty description="分班功能开发中" />
      </Card>
    </div>
  );
}
