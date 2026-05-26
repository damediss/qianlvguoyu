import React from 'react';
import { Typography } from 'antd';
import { TableOutlined } from '@ant-design/icons';

export function DevTaskDetailPage() {
  return (
    <div style={{ padding: 24 }}>
      <Typography.Title level={3}><TableOutlined /> 细目表</Typography.Title>
      <Typography.Text type="secondary">开发任务细目表页面待开发</Typography.Text>
    </div>
  );
}
