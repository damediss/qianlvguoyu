import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Card, Col, Row } from 'antd';
import { ToolOutlined } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
const ResourceCenterPage = () => {
    const setSelectedKey = useNavStore((s) => s.setSelectedKey);
    return (_jsx(Row, { gutter: 16, children: _jsx(Col, { span: 8, children: _jsxs(Card, { hoverable: true, title: _jsxs("span", { children: [_jsx(ToolOutlined, { style: { marginRight: 8 } }), "\u77E5\u8BC6\u5F52\u5C5E\u4FEE\u6B63"] }), onClick: () => setSelectedKey('knowledge_fix'), children: [_jsx("p", { children: "\u5BF9 2026-05-14 \u4EE5\u6765\u5BFC\u5165 exam_exam_detail \u7684\u9898\u76EE\uFF1A\u91CD\u590D\u5BFC\u5165\u6570\u636E\u8F6F\u5220\u9664\u53BB\u91CD\uFF1B LLM \u5224\u5B9A\u77E5\u8BC6\u70B9\u5F52\u5C5E\u5E76\u6309\u7F6E\u4FE1\u5206\u5C42\u4FEE\u6B63\uFF08apply/keep/flag \u4E09\u7C7B\uFF0C\u5168\u7A0B\u5907\u4EFD\u53EF\u56DE\u6EDA\uFF09\u3002" }), _jsx(Button, { type: "primary", size: "small", children: "\u8FDB\u5165\u5DE5\u4F5C\u53F0" })] }) }) }));
};
export default ResourceCenterPage;
