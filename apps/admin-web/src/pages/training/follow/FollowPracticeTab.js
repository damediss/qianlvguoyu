import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Alert, Button, Card, Space, Typography } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import { ClassPracticeAnalyticsPanel } from '../ClassPracticeAnalyticsModal';
import { ClassPracticeScopeModal } from '../ClassPracticeScopeModal';
/**
 * 跟班「刷题」Tab：试题配置（练习范围）+ 学员刷题详情。
 * 与「作业」Tab 分列（刷题=自助学情，作业=任务制）。
 */
export function FollowPracticeTab({ classId, classTitle, active }) {
    const [scopeOpen, setScopeOpen] = useState(false);
    return (_jsxs("div", { children: [_jsx(Card, { size: "small", style: { marginBottom: 12 }, children: _jsxs(Space, { wrap: true, style: { width: '100%', justifyContent: 'space-between' }, children: [_jsxs("div", { children: [_jsx(Typography.Text, { strong: true, children: "\u5B66\u5458\u5237\u9898\u8BE6\u60C5" }), _jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 0, marginTop: 4 }, children: "\u6309\u73ED\u7EA7\u67E5\u770B\u8FDB\u5EA6\u3001\u6B63\u786E\u7387\u3001\u7EC3\u4E60\u6B21\u6570\u4E0E\u6BCF\u65E5\u5237\u9898\uFF1B\u4E0E\u4F5C\u4E1A/\u968F\u5802\u7EC3\u5206\u901A\u9053\u7EDF\u8BA1\u3002\u4FDD\u5B58\u8BD5\u9898\u914D\u7F6E\u540E\u4F1A\u81EA\u52A8\u5F00\u901A\u540C\u6B65\u5B66\uFF08\u7ED1\u5B9A\u8003\u8BD5\u8BA1\u5212\u3001\u540D\u5355\u3001\u7070\u5EA6\uFF09\u3002" })] }), _jsx(Button, { type: "primary", icon: _jsx(SettingOutlined, {}), onClick: () => setScopeOpen(true), children: "\u8BD5\u9898\u914D\u7F6E" })] }) }), _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u8BF7\u5148\u914D\u7F6E\u672C\u73ED\u53EF\u5237\u79D1\u76EE\u4E0E\u77E5\u8BC6\u70B9\u3002\u4FDD\u5B58\u540E\u4F1A\u81EA\u52A8\u5F00\u901A\u540C\u6B65\u5B66\uFF1B\u5B66\u5458\u9700\u4ECE App \u73ED\u7EA7\u5165\u53E3\u5237\u9898\uFF08\u5E26 classId\uFF09\u540E\uFF0C\u6B64\u5904\u624D\u6709\u5B66\u60C5\u6570\u636E\u3002", action: _jsx(Button, { size: "small", onClick: () => setScopeOpen(true), children: "\u53BB\u914D\u7F6E" }) }), _jsx(ClassPracticeAnalyticsPanel, { classId: classId, enabled: active }), _jsx(ClassPracticeScopeModal, { open: scopeOpen, onClose: () => setScopeOpen(false), classId: classId, classTitle: classTitle, titlePrefix: "\u8BD5\u9898\u914D\u7F6E" })] }));
}
