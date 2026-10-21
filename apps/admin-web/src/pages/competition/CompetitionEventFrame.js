import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CalendarOutlined, HomeOutlined, TeamOutlined, UploadOutlined, } from '@ant-design/icons';
import { Button, Space, Tag, Typography } from 'antd';
import { useAuthStore } from '../../stores/authStore';
const statusMeta = {
    draft: { label: '创建中', color: 'default' },
    published: { label: '已发布', color: 'success' },
    closed: { label: '已关闭', color: 'warning' },
    archived: { label: '已归档', color: 'error' },
};
const scopeMeta = {
    platform: '平台赛事',
    enterprise_internal: '企业内部赛',
};
const sections = [
    { key: 'overview', label: '计划总览', icon: _jsx(HomeOutlined, {}), path: (id) => `/competition/events/${id}` },
    { key: 'organizations', label: '参与企业', icon: _jsx(TeamOutlined, {}), path: (id) => `/competition/events/${id}/organizations`, permission: 'competition:event-org:view' },
    { key: 'rosters', label: '名单导入', icon: _jsx(UploadOutlined, {}), path: (id) => `/competition/events/${id}/rosters`, permission: 'competition:roster:view' },
];
function formatDate(value) {
    if (!value)
        return '未设置';
    return value.replace('T', ' ').slice(0, 16);
}
export function CompetitionEventFrame({ event, activeSection, onNavigate, actions, children, }) {
    const permissions = useAuthStore((state) => state.permissionCodes);
    const visibleSections = sections.filter((section) => (!section.permission || permissions.includes(section.permission))
        && (!section.anyPermissions || section.anyPermissions.some((permission) => permissions.includes(permission))));
    const status = statusMeta[event.status];
    const eventId = String(event.id);
    return (_jsxs("div", { className: "competition-page competition-workspace-page", children: [_jsxs("section", { className: "competition-event-hero", children: [_jsxs("div", { className: "competition-event-hero__main", children: [_jsxs("div", { className: "competition-event-hero__title-row", children: [_jsx("div", { className: "competition-event-hero__mark", children: "\u8D5B" }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 2, children: event.name }), _jsxs(Space, { size: 8, wrap: true, children: [_jsx(Tag, { color: status.color, children: status.label }), _jsx(Typography.Text, { type: "secondary", children: event.code }), _jsx(Typography.Text, { type: "secondary", children: scopeMeta[event.scope] || event.scope })] })] })] }), _jsx(Typography.Paragraph, { className: "competition-event-hero__description", children: event.description || '围绕竞赛计划、参与企业、名单导入和公告设置，统一管理赛事运营链路。' }), _jsxs("div", { className: "competition-event-meta", children: [_jsxs("span", { children: [_jsx(CalendarOutlined, {}), " \u62A5\u540D/\u7ADE\u8D5B\u65F6\u95F4"] }), _jsxs("strong", { children: [formatDate(event.startAt), " \u2014 ", formatDate(event.endAt)] })] })] }), _jsx("div", { className: "competition-event-hero__actions", children: actions })] }), _jsx("nav", { className: "competition-workspace-nav", "aria-label": "\u8D5B\u4E8B\u5DE5\u4F5C\u53F0\u5BFC\u822A", children: visibleSections.map((section) => (_jsx(Button, { type: activeSection === section.key ? 'primary' : 'text', icon: section.icon, onClick: () => onNavigate(section.path(eventId)), children: section.label }, section.key))) }), children] }));
}
