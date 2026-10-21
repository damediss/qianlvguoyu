import { jsx as _jsx } from "react/jsx-runtime";
import { useState } from 'react';
import { Card, Tabs, Typography } from 'antd';
import { EnterprisePlanReviewPanel } from './EnterprisePlanReviewPanel';
import { EnterpriseClassConfirmPanel } from './EnterpriseClassConfirmPanel';
const { Title } = Typography;
export function EnterprisePlanReviewPage() {
    const [nav, setNav] = useState('plan');
    return (_jsx(Card, { title: _jsx(Title, { level: 5, style: { margin: 0 }, children: "\u4F01\u4E1A\u5BA1\u6838" }), bordered: false, children: _jsx(Tabs, { activeKey: nav, onChange: (key) => setNav(key), type: "card", items: [
                { key: 'plan', label: '规划审核', children: _jsx(EnterprisePlanReviewPanel, {}) },
                { key: 'class', label: '分班确认', children: _jsx(EnterpriseClassConfirmPanel, {}) },
            ] }) }));
}
