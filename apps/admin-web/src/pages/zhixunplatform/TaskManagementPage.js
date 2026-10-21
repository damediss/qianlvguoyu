import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Typography } from 'antd';
import { PlusCircleOutlined, ArrowRightOutlined, FolderOpenOutlined, InfoCircleOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TaskManagementPage.css';
// ============================================================
// 样式常量
// ============================================================
const S = {
    page: { padding: '24px 20px 48px', maxWidth: 1200, margin: '0 auto' },
    heading: { marginBottom: 24 },
    tabsWrap: { display: 'flex', gap: 8, marginBottom: 24 },
    entryCard: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 24,
        padding: '28px 32px',
        background: 'linear-gradient(135deg, #E8F0FF 0%, #fff 60%)',
        border: '2px dashed rgba(42, 109, 255, 0.25)',
        borderRadius: 20,
        textDecoration: 'none',
        color: 'inherit',
        cursor: 'pointer',
        transition: 'all 0.25s ease',
        marginBottom: 16,
    },
    entryTextH3: { fontSize: 18, fontWeight: 700, color: '#2A6DFF', margin: '0 0 6px' },
    entryTextP: { fontSize: 13, color: '#64748B', maxWidth: 520, margin: 0 },
    entryBtn: {
        flexShrink: 0,
        padding: '14px 28px',
        background: '#2A6DFF',
        color: '#fff',
        borderRadius: 12,
        fontWeight: 600,
        fontSize: 14,
        display: 'inline-flex',
        alignItems: 'center',
        gap: 10,
        border: 'none',
        cursor: 'pointer',
        whiteSpace: 'nowrap',
    },
    infoText: { marginTop: 14, fontSize: 13, color: '#64748B' },
};
// ============================================================
// Tab 按钮
// ============================================================
function TabBtn({ active, children, onClick }) {
    return (_jsx("button", { className: `tp-tab-btn${active ? ' active' : ''}`, onClick: onClick, children: children }));
}
// ============================================================
// 入口卡片
// ============================================================
function EntryCard({ icon, title, desc, btnText, onClick, }) {
    return (_jsxs("div", { className: "tp-entry-card", style: S.entryCard, onClick: onClick, children: [_jsxs("div", { children: [_jsxs("h3", { style: S.entryTextH3, children: [icon, " ", title] }), _jsx("p", { style: S.entryTextP, children: desc })] }), _jsxs("span", { style: S.entryBtn, children: [btnText, " ", _jsx(ArrowRightOutlined, {})] })] }));
}
// ============================================================
// 主页面
// ============================================================
export function TaskManagementPage() {
    const [activeTab, setActiveTab] = useState('create');
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    return (_jsxs("div", { style: S.page, children: [_jsxs("section", { style: S.heading, children: [_jsx("h2", { style: { fontSize: 26, fontWeight: 700, margin: '0 0 6px' }, children: "\u4EFB\u52A1\u7BA1\u7406" }), _jsx(Typography.Text, { type: "secondary", children: "\u521B\u5EFA\u57F9\u8BAD\u4EFB\u52A1\u5E76\u7BA1\u7406\u4EFB\u52A1\u6863\u6848" })] }), _jsxs("div", { style: S.tabsWrap, children: [_jsx(TabBtn, { active: activeTab === 'create', onClick: () => setActiveTab('create'), children: "\u521B\u5EFA\u4EFB\u52A1" }), _jsx(TabBtn, { active: activeTab === 'archive', onClick: () => setActiveTab('archive'), children: "\u4EFB\u52A1\u6863\u6848" })] }), activeTab === 'create' && (_jsxs("div", { children: [_jsx(EntryCard, { icon: _jsx(PlusCircleOutlined, {}), title: "\u521B\u5EFA\u57F9\u8BAD\u4EFB\u52A1", desc: "\u5B8C\u6210\u57FA\u672C\u4FE1\u606F\u3001\u9879\u76EE\u5173\u8054\u4E0E\u4EFB\u52A1\u914D\u7F6E\uFF0C\u63D0\u4EA4\u540E\u540C\u6B65\u81F3\u4EFB\u52A1\u6863\u6848\u3002", btnText: "\u8FDB\u5165\u521B\u5EFA\u9875", onClick: () => navigateTo('zhixun-create-task') }), _jsxs("p", { style: S.infoText, children: [_jsx(InfoCircleOutlined, { style: { color: '#2A6DFF', marginRight: 6 } }), "\u5B66\u5458\u540D\u5355\u3001\u5E08\u8D44\u5B89\u6392\u7B49\u914D\u7F6E\u9879\u5728\u521B\u5EFA\u9875\u4E2D\u6309\u9700\u586B\u5199\uFF0C\u63D0\u4EA4\u540E\u53EF\u7EE7\u7EED\u5B8C\u5584\u3002"] })] })), activeTab === 'archive' && (_jsx("div", { children: _jsx(EntryCard, { icon: _jsx(FolderOpenOutlined, {}), title: "\u4EFB\u52A1\u6863\u6848\u53F0\u8D26", desc: "\u67E5\u8BE2\u3001\u7B5B\u9009\u5168\u90E8\u57F9\u8BAD\u4EFB\u52A1\uFF0C\u67E5\u770B\u8BE6\u60C5\u4E0E\u914D\u7F6E\u8FDB\u5EA6\uFF0C\u652F\u6301\u7F16\u8F91\u3001\u5220\u9664\u4E0E\u5BFC\u51FA\u6F14\u793A\u3002", btnText: "\u8FDB\u5165\u6863\u6848\u9875", onClick: () => navigateTo('zhixun-task-archives') }) }))] }));
}
