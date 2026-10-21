import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { SearchOutlined, ScanOutlined } from '@ant-design/icons';
import { Button, Empty, Input, List, Space, Typography, message } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { matchByToken, searchCandidate } from '../../../api/certificationPracticalCollection';
import { getScanAdapter } from '../../../services/scan/ScanAdapterFactory';
export function CandidateSearchBox({ taskId, examSessionId, onCandidateMatched, onCandidatesFound, onAfterStatusChanged, }) {
    const [token, setToken] = useState('');
    const [keyword, setKeyword] = useState('');
    const [candidates, setCandidates] = useState([]);
    const tokenMutation = useMutation({
        mutationFn: (collectToken) => matchByToken({ taskId, examSessionId, collectToken }),
        onSuccess: (candidate) => {
            message.success(candidate.currentStatus === 'COMPLETED' ? '该考生已完成采集' : '已匹配考生');
            setCandidates([candidate]);
            onCandidateMatched(candidate);
            onAfterStatusChanged?.();
        },
    });
    const searchMutation = useMutation({
        mutationFn: (value) => searchCandidate({ taskId, examSessionId, keyword: value }),
        onSuccess: (items) => {
            setCandidates(items);
            onCandidatesFound(items);
            if (items.length === 1) {
                onCandidateMatched(items[0]);
            }
            else if (items.length === 0) {
                message.info('未找到匹配考生，请更换 token、后四位、姓名或准考证号');
            }
        },
    });
    const scanMutation = useMutation({
        mutationFn: async () => getScanAdapter().startScan(),
        onSuccess: (result) => {
            if (!result.success || !result.token) {
                message.warning(result.message || '未获取到扫码 token');
                return;
            }
            setToken(result.token);
            tokenMutation.mutate(result.token);
        },
    });
    const handleTokenMatch = () => {
        const value = token.trim();
        if (!value) {
            message.warning('请输入采集二维码 token');
            return;
        }
        tokenMutation.mutate(value);
    };
    const handleSearch = () => {
        const value = keyword.trim();
        if (!value) {
            message.warning('请输入身份证后四位、姓名或准考证号');
            return;
        }
        searchMutation.mutate(value);
    };
    const loading = tokenMutation.isPending || searchMutation.isPending || scanMutation.isPending;
    return (_jsxs("div", { className: "candidate-search-box", children: [_jsxs("div", { className: "candidate-search-actions", children: [_jsx(Button, { type: "primary", icon: _jsx(ScanOutlined, {}), loading: scanMutation.isPending || tokenMutation.isPending, onClick: () => scanMutation.mutate(), children: "\u626B\u7801\u5339\u914D" }), _jsx(Input.Search, { allowClear: true, value: token, size: "large", placeholder: "\u624B\u52A8\u8F93\u5165\u4E8C\u7EF4\u7801 token", enterButton: "\u5339\u914D token", onChange: (event) => setToken(event.target.value), onSearch: handleTokenMatch, loading: tokenMutation.isPending }), _jsx(Input.Search, { allowClear: true, value: keyword, size: "large", prefix: _jsx(SearchOutlined, {}), placeholder: "\u8EAB\u4EFD\u8BC1\u540E\u56DB\u4F4D / \u59D3\u540D / \u59D3\u540D+\u540E\u56DB\u4F4D / \u51C6\u8003\u8BC1\u53F7", enterButton: "\u641C\u7D22\u8003\u751F", onChange: (event) => setKeyword(event.target.value), onSearch: handleSearch, loading: searchMutation.isPending })] }), candidates.length > 1 ? (_jsxs("div", { className: "candidate-result-list", children: [_jsxs(Typography.Text, { type: "secondary", children: ["\u627E\u5230 ", candidates.length, " \u540D\u5019\u9009\uFF0C\u8BF7\u9009\u62E9\u786E\u8BA4"] }), _jsx(List, { dataSource: candidates, loading: loading, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u5019\u9009" }) }, renderItem: (candidate) => (_jsx(List.Item, { className: "candidate-result-item", onClick: () => onCandidateMatched(candidate), actions: [
                                _jsx(Button, { type: "link", onClick: () => onCandidateMatched(candidate), children: "\u786E\u8BA4" }, "select"),
                            ], children: _jsxs(Space, { size: 16, children: [_jsx("strong", { children: candidate.name }), _jsxs("span", { children: ["\u540E\u56DB\u4F4D ", candidate.idCardLast4 || '-'] }), _jsx("span", { children: candidate.unitName || '-' }), _jsx("span", { children: candidate.admissionNo || '-' })] }) })) })] })) : null] }));
}
