import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined } from '@ant-design/icons';
import { Button, Empty, List, Space, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { getCollectionExceptions } from '../../../api/certificationPracticalCollection';
export function CollectionExceptionPage({ taskId, examSessionId }) {
    const exceptionsQuery = useQuery({
        queryKey: ['practical-collection-exceptions', taskId, examSessionId],
        queryFn: () => getCollectionExceptions({ page: 1, pageSize: 50, taskId, examSessionId }),
    });
    return (_jsxs("div", { className: "collection-exception-page", children: [_jsxs("div", { className: "collection-section-heading", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: "\u5F02\u5E38\u8BB0\u5F55" }), _jsx(Typography.Text, { type: "secondary", children: "\u67E5\u770B\u5F85\u8865\u91C7\u3001\u5F02\u5E38\u548C\u514D\u8003\u653E\u884C\u7559\u75D5" })] }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: exceptionsQuery.isFetching, onClick: () => exceptionsQuery.refetch(), children: "\u5237\u65B0" })] }), _jsx(List, { loading: exceptionsQuery.isFetching, dataSource: exceptionsQuery.data?.items || [], locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u5F02\u5E38\u8BB0\u5F55" }) }, renderItem: (item) => (_jsx(List.Item, { children: _jsx(List.Item.Meta, { title: (_jsxs(Space, { wrap: true, children: [_jsx("span", { children: item.candidateName || `考生 ${item.candidateId}` }), _jsx(Tag, { color: "red", children: item.exceptionTypeLabel || item.exceptionType }), _jsx(Tag, { children: item.exceptionStatusLabel || item.exceptionStatus })] })), description: (_jsxs(Space, { direction: "vertical", size: 2, children: [_jsxs("span", { children: ["\u540E\u56DB\u4F4D\uFF1A", item.idCardLast4 || '-'] }), _jsxs("span", { children: ["\u539F\u56E0\uFF1A", item.reason] }), _jsxs("span", { children: ["\u65F6\u95F4\uFF1A", item.handledAt || item.createdAt || '-'] })] })) }) })) })] }));
}
