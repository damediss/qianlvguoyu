import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Checkbox, Select } from 'antd';
import { DefenseExpertSignature } from './DefenseExpertSignature';
import { PERFORMANCE_CATEGORIES, formatScore, performanceTotal, scoreOptions, } from './defenseScoreCatalog';
function scoreMap(value) {
    const map = new Map();
    for (const item of value.items || []) {
        map.set(item.key, item.score ?? null);
    }
    return map;
}
export function emptyPerformancePayload() {
    return {
        items: PERFORMANCE_CATEGORIES.flatMap((category) => category.items.map((item) => ({ key: item.key, score: null }))),
        disqualify: false,
        remark: '',
    };
}
export function DefensePerformanceScoreSheet({ candidate, assessorName, taskId, value, disabled, signature, signaturePreviewUrl, signatureRef, onChange, onSignatureChange, }) {
    const scores = scoreMap(value);
    const total = performanceTotal(value.items);
    const occupation = candidate?.workTypeName || '—';
    const level = candidate?.levelName || '—';
    const setScore = (key, score) => {
        const nextItems = PERFORMANCE_CATEGORIES.flatMap((category) => category.items).map((item) => ({
            key: item.key,
            score: item.key === key ? score : (scores.get(item.key) ?? null),
        }));
        onChange({ ...value, items: nextItems });
    };
    return (_jsxs("div", { className: "defense-paper", children: [_jsx("h2", { className: "defense-paper-title", children: "\u6280\u5E08\u3001\u9AD8\u7EA7\u6280\u5E08\u4E1A\u7EE9\u8BC4\u4F30\u62A5\u544A\u8BC4\u5206\u8868" }), _jsxs("p", { className: "defense-paper-meta", children: ["\u7533\u62A5\u804C\u4E1A\uFF08\u5DE5\u79CD\uFF09\uFF1A", occupation, _jsxs("span", { className: "defense-paper-meta-gap", children: ["\u7EA7\u522B\uFF1A", level] })] }), _jsxs("table", { className: "defense-paper-table", children: [_jsxs("colgroup", { children: [_jsx("col", { style: { width: '11%' } }), _jsx("col", { style: { width: '16%' } }), _jsx("col", { style: { width: '9%' } }), _jsx("col", { style: { width: '14%' } }), _jsx("col", { style: { width: '32%' } }), _jsx("col", { style: { width: '18%' } })] }), _jsxs("tbody", { children: [_jsxs("tr", { children: [_jsx("th", { colSpan: 2, children: "\u8003\u751F\u59D3\u540D" }), _jsx("td", { children: candidate?.candidateName || '—' }), _jsx("th", { children: "\u51C6\u8003\u8BC1\u53F7" }), _jsx("td", { colSpan: 2, className: "defense-paper-ticket", children: candidate?.admissionTicketNo || '—' })] }), _jsxs("tr", { children: [_jsx("th", { colSpan: 2, children: "\u9879 \u76EE" }), _jsx("th", { children: "\u914D\u5206" }), _jsx("th", { colSpan: 2, children: "\u8BC4 \u5206 \u5185 \u5BB9" }), _jsx("th", { children: "\u8BC4\u5206\u7ED3\u679C" })] }), PERFORMANCE_CATEGORIES.flatMap((category) => (category.items.map((item, index) => (_jsxs("tr", { children: [index === 0 ? (_jsxs("th", { rowSpan: category.items.length, className: "defense-paper-category", children: [category.label, _jsxs("span", { children: ["\uFF08", category.maxScore, "\u5206\uFF09"] })] })) : null, _jsx("td", { children: item.label }), _jsx("td", { className: "defense-paper-center", children: item.maxScore }), _jsxs("td", { colSpan: 2, className: "defense-paper-criteria", children: [item.criteria, item.disqualify ? (_jsx("div", { className: "defense-paper-disqualify", children: _jsx(Checkbox, { disabled: disabled, checked: Boolean(value.disqualify), onChange: (event) => onChange({ ...value, disqualify: event.target.checked }), children: "\u4E0D\u7B26\u5408\u7533\u62A5\u6761\u4EF6\uFF08\u5426\u5B9A\u9879\uFF0C\u7EFC\u5408\u79D1\u76EE\u4E0D\u5408\u683C\uFF09" }) })) : null] }), _jsx("td", { children: _jsx(Select, { className: "defense-paper-score", disabled: disabled, allowClear: true, placeholder: "\u9009\u62E9", getPopupContainer: () => document.body, value: scores.get(item.key) ?? undefined, options: scoreOptions(item.maxScore).map((score) => ({ label: String(score), value: score })), onChange: (score) => setScore(item.key, typeof score === 'number' ? score : null) }) })] }, item.key))))), _jsxs("tr", { className: `defense-paper-subtotal${value.disqualify ? ' is-rejected' : ''}`, children: [_jsx("th", { colSpan: 2, children: "\u5408 \u8BA1" }), _jsx("td", { className: "defense-paper-center", children: "100" }), _jsxs("td", { colSpan: 2, className: "defense-paper-criteria", children: ["\u5404\u9879\u5747\u8003\u6838\u4EFB\u73B0\u804C\u4EE5\u6765\u7684\u60C5\u51B5\uFF0C\u6BCF\u9879\u6700\u9AD8\u5F97\u5206\u4E0D\u5F97\u8D85\u8FC7\u6807\u51C6\u5206\u3002", value.disqualify ? ' 任职年限不符合申报条件，综合科目成绩判定不合格。' : ''] }), _jsx("td", { className: "defense-paper-center", children: formatScore(total) })] }), _jsxs("tr", { children: [_jsx("th", { children: "\u8BC4\u5BA1\u4E13\u5BB6" }), _jsx("td", { colSpan: 5, children: _jsx(DefenseExpertSignature, { ref: signatureRef, taskId: taskId, signerName: assessorName, disabled: disabled, previewUrl: signaturePreviewUrl, value: signature, onChange: onSignatureChange }) })] })] })] })] }));
}
