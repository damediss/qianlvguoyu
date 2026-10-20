import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Checkbox, Input, Select } from 'antd';
import { DefenseExpertSignature } from './DefenseExpertSignature';
import { THESIS_CONTENT_ITEMS, THESIS_DEFENSE_ITEMS, THESIS_REJECT_ITEMS, formatScore, scoreOptions, sumScores, thesisFinalScore, } from './defenseScoreCatalog';
function pad(source, size, fallback) {
    const next = Array.from({ length: size }, (_, index) => source?.[index] ?? fallback);
    return next;
}
export function emptyThesisPayload() {
    return {
        paperTitle: '',
        rejectFlags: THESIS_REJECT_ITEMS.map(() => false),
        rejectNote: '',
        contentScores: THESIS_CONTENT_ITEMS.map(() => null),
        questions: ['', '', ''],
        defenseScores: THESIS_DEFENSE_ITEMS.map(() => null),
    };
}
export function DefenseThesisScoreSheet({ candidate, assessorName, taskId, value, disabled, signature, signaturePreviewUrl, signatureRef, onChange, onSignatureChange, }) {
    const rejectFlags = pad(value.rejectFlags, THESIS_REJECT_ITEMS.length, false);
    const contentScores = pad(value.contentScores, THESIS_CONTENT_ITEMS.length, null);
    const questions = pad(value.questions, 3, '');
    const defenseScores = pad(value.defenseScores, THESIS_DEFENSE_ITEMS.length, null);
    const contentSubtotal = sumScores(contentScores);
    const defenseSubtotal = sumScores(defenseScores);
    const finalScore = thesisFinalScore(contentScores, defenseScores);
    const occupation = [candidate?.workTypeName, candidate?.levelName].filter(Boolean).join('  ');
    const patch = (partial) => {
        onChange({ ...value, ...partial });
    };
    return (_jsxs("div", { className: "defense-paper", children: [_jsx("h2", { className: "defense-paper-title", children: "\u6280\u672F\u603B\u7ED3\uFF08\u8BBA\u6587\uFF09\u7B54\u8FA9\u8BC4\u5BA1\u8BC4\u5206\u8868" }), _jsxs("p", { className: "defense-paper-meta", children: ["\u7533\u62A5\u804C\u4E1A\uFF1A", occupation || '—'] }), _jsxs("table", { className: "defense-paper-table", children: [_jsxs("colgroup", { children: [_jsx("col", { style: { width: '15%' } }), _jsx("col", { style: { width: '22%' } }), _jsx("col", { style: { width: '16%' } }), _jsx("col", { style: { width: '47%' } })] }), _jsxs("tbody", { children: [_jsxs("tr", { children: [_jsx("th", { children: "\u8003\u751F\u59D3\u540D" }), _jsx("td", { children: candidate?.candidateName || '—' }), _jsx("th", { children: "\u51C6\u8003\u8BC1\u53F7" }), _jsx("td", { children: candidate?.admissionTicketNo || '—' })] }), _jsxs("tr", { children: [_jsx("th", { children: "\u8BBA\u6587\u9898\u76EE" }), _jsx("td", { colSpan: 3, children: _jsx(Input, { variant: "borderless", disabled: disabled, placeholder: "\u8BF7\u8F93\u5165\u8BBA\u6587\u9898\u76EE", value: value.paperTitle || '', onChange: (event) => patch({ paperTitle: event.target.value }) }) })] }), _jsxs("tr", { children: [_jsx("th", { children: "\u5426\u5B9A\u9879" }), _jsx("td", { colSpan: 3, children: _jsx("div", { className: "defense-paper-rejects", children: THESIS_REJECT_ITEMS.map((item, index) => (_jsxs(Checkbox, { disabled: disabled, checked: Boolean(rejectFlags[index]), onChange: (event) => {
                                                    const next = [...rejectFlags];
                                                    next[index] = event.target.checked;
                                                    patch({ rejectFlags: next });
                                                }, children: [index + 1, ".", item] }, item))) }) })] }), _jsxs("tr", { children: [_jsx("th", { children: "\u5426\u5B9A\u8BF4\u660E" }), _jsx("td", { colSpan: 3, children: _jsx(Input.TextArea, { autoSize: { minRows: 2, maxRows: 4 }, disabled: disabled, placeholder: "\u52FE\u9009\u5426\u5B9A\u9879\u540E\u8BF7\u8BF4\u660E\u539F\u56E0", value: value.rejectNote || '', onChange: (event) => patch({ rejectNote: event.target.value }) }) })] }), _jsxs("tr", { children: [_jsxs("th", { rowSpan: THESIS_CONTENT_ITEMS.length + 2, className: "defense-paper-section", children: ["\u8BBA\u6587\u5185\u5BB9\u90E8\u5206", _jsx("span", { children: "\uFF0840%\uFF09" })] }), _jsx("th", { children: "\u8BC4\u5B9A\u9879\u76EE" }), _jsx("th", { children: "\u6EE1\u5206" }), _jsx("th", { children: "\u5B9E\u9645\u6210\u7EE9(\u5206)" })] }), THESIS_CONTENT_ITEMS.map((item, index) => (_jsxs("tr", { children: [_jsxs("td", { children: [index + 1, ".", item.label] }), _jsx("td", { className: "defense-paper-center", children: item.maxScore }), _jsx("td", { children: _jsx(Select, { className: "defense-paper-score", disabled: disabled, allowClear: true, placeholder: "\u9009\u62E9", getPopupContainer: () => document.body, value: contentScores[index] ?? undefined, options: scoreOptions(item.maxScore).map((score) => ({ label: String(score), value: score })), onChange: (score) => {
                                                const next = [...contentScores];
                                                next[index] = typeof score === 'number' ? score : null;
                                                patch({ contentScores: next });
                                            } }) })] }, item.key))), _jsxs("tr", { className: "defense-paper-subtotal", children: [_jsx("th", { children: "\u5C0F    \u8BA1" }), _jsx("td", { className: "defense-paper-center", children: "100" }), _jsx("td", { className: "defense-paper-center", children: formatScore(contentSubtotal) })] }), _jsxs("tr", { children: [_jsxs("th", { rowSpan: THESIS_DEFENSE_ITEMS.length + 6, className: "defense-paper-section", children: ["\u8BBA\u6587\u7B54\u8FA9\u90E8\u5206", _jsx("span", { children: "\uFF0860%\uFF09" })] }), _jsx("th", { colSpan: 3, children: "\u95EE\u9898" })] }), questions.map((question, index) => (_jsx("tr", { children: _jsx("td", { colSpan: 3, children: _jsx(Input, { variant: "borderless", disabled: disabled, prefix: `${index + 1}.`, placeholder: "\u8BF7\u8F93\u5165\u7B54\u8FA9\u95EE\u9898", value: question, onChange: (event) => {
                                            const next = [...questions];
                                            next[index] = event.target.value;
                                            patch({ questions: next });
                                        } }) }) }, `q-${index}`))), _jsxs("tr", { children: [_jsx("th", { children: "\u8BC4\u5B9A\u9879\u76EE" }), _jsx("th", { children: "\u6EE1\u5206" }), _jsx("th", { children: "\u5B9E\u9645\u6210\u7EE9(\u5206)" })] }), THESIS_DEFENSE_ITEMS.map((item, index) => (_jsxs("tr", { children: [_jsxs("td", { children: [index + 1, ".", item.label] }), _jsx("td", { className: "defense-paper-center", children: item.maxScore }), _jsx("td", { children: _jsx(Select, { className: "defense-paper-score", disabled: disabled, allowClear: true, placeholder: "\u9009\u62E9", getPopupContainer: () => document.body, value: defenseScores[index] ?? undefined, options: scoreOptions(item.maxScore).map((score) => ({ label: String(score), value: score })), onChange: (score) => {
                                                const next = [...defenseScores];
                                                next[index] = typeof score === 'number' ? score : null;
                                                patch({ defenseScores: next });
                                            } }) })] }, item.key))), _jsxs("tr", { className: "defense-paper-subtotal", children: [_jsx("th", { children: "\u5C0F    \u8BA1" }), _jsx("td", { className: "defense-paper-center", children: "100" }), _jsx("td", { className: "defense-paper-center", children: formatScore(defenseSubtotal) })] }), _jsxs("tr", { className: "defense-paper-final", children: [_jsx("th", { children: "\u6700\u7EC8\u5F97\u5206" }), _jsx("td", { colSpan: 2, children: "\u6700\u7EC8\u5F97\u5206=\u8BBA\u6587\u5185\u5BB9\u90E8\u5206\uFF0840%\uFF09+\u8BBA\u6587\u7B54\u8FA9\u90E8\u5206\uFF0860%\uFF09" }), _jsx("td", { className: "defense-paper-center", children: formatScore(finalScore) })] }), _jsxs("tr", { children: [_jsx("th", { children: "\u8BC4\u5BA1\u4E13\u5BB6" }), _jsx("td", { colSpan: 3, children: _jsx(DefenseExpertSignature, { ref: signatureRef, taskId: taskId, signerName: assessorName, disabled: disabled, previewUrl: signaturePreviewUrl, value: signature, onChange: onSignatureChange }) })] })] })] })] }));
}
