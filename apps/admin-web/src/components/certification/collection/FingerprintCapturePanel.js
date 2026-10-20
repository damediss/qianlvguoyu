import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { SafetyCertificateOutlined } from '@ant-design/icons';
import { Button, Space, Tag, Typography } from 'antd';
export function FingerprintCapturePanel({ status, loading, onCheckDevice }) {
    return (_jsxs("div", { className: "fingerprint-capture-panel", children: [_jsxs(Space, { direction: "vertical", size: 8, children: [_jsx(Typography.Text, { strong: true, children: "\u6307\u7EB9\u8BBE\u5907" }), _jsx(Tag, { color: status?.connected ? 'success' : 'warning', children: status?.message || '未检测' })] }), _jsx(Button, { icon: _jsx(SafetyCertificateOutlined, {}), loading: loading, onClick: onCheckDevice, children: "\u68C0\u6D4B\u6307\u7EB9\u8BBE\u5907" })] }));
}
