import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CloudOutlined, SafetyCertificateOutlined, WifiOutlined } from '@ant-design/icons';
import { Button, Space, Tag, Typography } from 'antd';
export function CollectionStatsBar({ task, stats, deviceStatus, online, onCheckDevice, checkingDevice, }) {
    const items = [
        ['总人数', stats?.candidateTotal ?? task?.candidateTotal ?? 0],
        ['已到场', stats?.arrivedCount ?? task?.arrivedCount ?? 0],
        ['已入队', stats?.queuedCount ?? task?.queuedCount ?? 0],
        ['采集中', stats?.collectingCount ?? task?.collectingCount ?? 0],
        ['已完成', stats?.completedCount ?? task?.completedCount ?? 0],
        ['待补采', stats?.pendingRetryCount ?? task?.pendingRetryCount ?? 0],
        ['异常', stats?.exceptionCount ?? task?.exceptionCount ?? 0],
    ];
    const deviceColor = deviceStatus?.connected ? 'success' : deviceStatus?.status === 'ERROR' ? 'error' : 'warning';
    return (_jsxs("div", { className: "collection-stats-bar", children: [_jsxs("div", { className: "collection-task-title", children: [_jsx(Typography.Title, { level: 3, children: task?.taskName || '实操信息采集工作台' }), _jsxs(Typography.Text, { children: [task?.taskNo || '-', " \u00B7 ", task?.projectName || '认定任务', " \u00B7 ", task?.examDate || '-'] })] }), _jsx("div", { className: "collection-stat-strip", children: items.map(([label, value]) => (_jsxs("div", { className: "collection-stat-cell", children: [_jsx("span", { children: label }), _jsx("strong", { children: value })] }, label))) }), _jsxs(Space, { className: "collection-device-strip", size: 8, wrap: true, children: [_jsx(Tag, { icon: _jsx(WifiOutlined, {}), color: online ? 'success' : 'error', children: online ? '网络在线' : '网络离线' }), _jsx(Tag, { icon: _jsx(SafetyCertificateOutlined, {}), color: deviceColor, children: deviceStatus?.message || '指纹设备未检测' }), _jsx(Button, { icon: _jsx(CloudOutlined, {}), loading: checkingDevice, onClick: onCheckDevice, children: "\u68C0\u6D4B\u8BBE\u5907" })] })] }));
}
