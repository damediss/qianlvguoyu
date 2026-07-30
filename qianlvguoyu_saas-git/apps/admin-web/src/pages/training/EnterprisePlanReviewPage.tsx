import React, { useState } from 'react';
import { Card, Tabs, Typography } from 'antd';
import { EnterprisePlanReviewPanel } from './EnterprisePlanReviewPanel';
import { EnterpriseClassConfirmPanel } from './EnterpriseClassConfirmPanel';

const { Title } = Typography;

type ReviewNav = 'plan' | 'class';

export function EnterprisePlanReviewPage() {
  const [nav, setNav] = useState<ReviewNav>('plan');

  return (
    <Card
      title={<Title level={5} style={{ margin: 0 }}>企业审核</Title>}
      bordered={false}
    >
      <Tabs
        activeKey={nav}
        onChange={(key) => setNav(key as ReviewNav)}
        type="card"
        items={[
          { key: 'plan', label: '规划审核', children: <EnterprisePlanReviewPanel /> },
          { key: 'class', label: '分班确认', children: <EnterpriseClassConfirmPanel /> },
        ]}
      />
    </Card>
  );
}
