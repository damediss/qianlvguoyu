import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import './CompetitionPracticalSnapshotPaperPreview.css';
const DEFAULT_NEGATIVE_ITEMS_HTML = `
  <ol>
    <li>考生没有按规定穿戴劳保用品。</li>
    <li>操作过程中出现严重违章操作。</li>
    <li>造成人身伤害或设备损坏。</li>
  </ol>
`;
const PAPER_ALLOWED_TAGS = new Set([
    'P', 'BR', 'STRONG', 'EM', 'S', 'U', 'BLOCKQUOTE', 'UL', 'OL', 'LI',
    'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'PRE', 'CODE', 'TABLE', 'THEAD',
    'TBODY', 'TR', 'TH', 'TD', 'A', 'SPAN', 'DIV', 'SUP', 'SUB',
]);
function sanitizePaperHtml(html) {
    if (typeof DOMParser === 'undefined')
        return '';
    const document = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html');
    const root = document.body.firstElementChild;
    if (!root)
        return '';
    const visit = (parent) => {
        for (const node of Array.from(parent.childNodes)) {
            if (node.nodeType !== 1)
                continue;
            const child = node;
            const tagName = child.tagName.toUpperCase();
            if (!PAPER_ALLOWED_TAGS.has(tagName)) {
                if (['SCRIPT', 'STYLE', 'IFRAME', 'OBJECT', 'EMBED', 'SVG', 'MATH'].includes(tagName)) {
                    child.remove();
                    continue;
                }
                visit(child);
                while (child.firstChild)
                    parent.insertBefore(child.firstChild, child);
                child.remove();
                continue;
            }
            for (const attribute of Array.from(child.attributes)) {
                const name = attribute.name.toLowerCase();
                const allowed = (tagName === 'A' && ['href', 'title', 'target', 'rel'].includes(name))
                    || ['class', 'colspan', 'rowspan'].includes(name);
                if (!allowed || (name === 'href' && !/^(https?:|mailto:)/i.test(attribute.value))) {
                    child.removeAttribute(attribute.name);
                }
            }
            if (tagName === 'A') {
                child.setAttribute('target', '_blank');
                child.setAttribute('rel', 'noopener noreferrer');
            }
            visit(child);
        }
    };
    visit(root);
    return root.innerHTML;
}
function parseJson(value) {
    if (!value?.trim())
        return undefined;
    try {
        return JSON.parse(value);
    }
    catch {
        return undefined;
    }
}
function asRecord(value) {
    return value && typeof value === 'object' && !Array.isArray(value)
        ? value
        : undefined;
}
function asRecords(value) {
    return Array.isArray(value) ? value.map(asRecord).filter((item) => Boolean(item)) : [];
}
function stringValue(value) {
    return typeof value === 'string' ? value.trim() : '';
}
function firstContent(...values) {
    for (const value of values) {
        const text = stringValue(value);
        if (text)
            return text;
    }
    return '';
}
function stripHtml(value) {
    return value
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/<[^>]+>/g, ' ')
        .replace(/&nbsp;/gi, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}
function formatNumber(value) {
    if (value == null || value === '')
        return '-';
    const numeric = Number(value);
    if (!Number.isFinite(numeric))
        return String(value);
    return numeric.toFixed(2).replace(/\.00$/, '').replace(/(\.\d)0$/, '$1');
}
function formatDuration(value, fallback) {
    const numeric = Number(value);
    return Number.isFinite(numeric) && numeric > 0 ? String(Math.round(numeric)) : String(fallback);
}
function PaperRichText({ value, className = '' }) {
    const content = stringValue(value);
    if (!content)
        return null;
    if (!/<[a-z][\s\S]*>/i.test(content)) {
        return (_jsx("div", { className: `competition-paper-preview__rich-text ${className}`.trim(), children: content.split(/\r?\n/).map((line, index) => _jsx("div", { children: line || '\u00a0' }, `${line}-${index}`)) }));
    }
    return (_jsx("div", { className: `competition-paper-preview__rich-text ${className}`.trim(), dangerouslySetInnerHTML: { __html: sanitizePaperHtml(content) } }));
}
function PracticalQuestionTitle({ question, index }) {
    const questionTitle = stripHtml(question.stem || '') || '实操题';
    return _jsx(_Fragment, { children: `试题${question.sortOrder || index + 1}、${questionTitle}` });
}
function getPracticalQuestionPaperData(question, snapshotDurationMinutes) {
    const detail = asRecord(parseJson(question.practicalDetailJson));
    const contents = asRecords(detail?.assessmentContents);
    const contentHtml = contents
        .map((content) => stringValue(content.contentHtml))
        .filter(Boolean)
        .join('<p></p>');
    const totalScore = detail?.totalScore ?? question.score;
    return {
        assessmentRequirementsHtml: firstContent(detail?.assessmentRequirementsHtml, question.operationContent, contentHtml),
        candidatePrepareHtml: firstContent(detail?.candidatePrepareHtml, question.studentPrepare),
        examRoomPrepareHtml: firstContent(detail?.examRoomPrepareHtml, question.equipmentPrepare),
        negativeItemsHtml: firstContent(detail?.negativeItemsHtml) || DEFAULT_NEGATIVE_ITEMS_HTML,
        questionMaterialsHtml: firstContent(detail?.questionMaterialsHtml),
        durationMinutes: formatDuration(detail?.durationMinutes, snapshotDurationMinutes),
        score: formatNumber(question.score ?? totalScore),
    };
}
function PaperDetailLine({ label, children }) {
    return (_jsxs("div", { className: "competition-paper-preview__detail-line", children: [_jsx("span", { className: "competition-paper-preview__detail-label", children: label }), _jsx("div", { className: "competition-paper-preview__detail-value", children: children })] }));
}
function PaperSupportingInfo({ label, value }) {
    if (!value)
        return null;
    return (_jsxs("div", { className: "competition-paper-preview__supporting-info", children: [_jsxs("span", { className: "competition-paper-preview__supporting-label", children: [label, "\uFF1A"] }), _jsx(PaperRichText, { value: value })] }));
}
function PracticalSnapshotQuestion({ question, index, snapshotDurationMinutes, }) {
    const data = getPracticalQuestionPaperData(question, snapshotDurationMinutes);
    const negativeText = stripHtml(data.negativeItemsHtml);
    const negativeHasExplanation = /否定项说明|若考生|零分/.test(negativeText);
    return (_jsxs("section", { className: "competition-paper-preview__question", children: [_jsx("h3", { className: "competition-paper-preview__question-title", children: _jsx(PracticalQuestionTitle, { question: question, index: index }) }), _jsxs("p", { className: "competition-paper-preview__line", children: ["\uFF081\uFF09\u672C\u9898\u5206\u503C\uFF1A", _jsxs("strong", { children: [data.score, "\u5206"] })] }), _jsxs("p", { className: "competition-paper-preview__line", children: ["\uFF082\uFF09\u8003\u6838\u65F6\u95F4\uFF1A", data.durationMinutes, "min"] }), _jsx("p", { className: "competition-paper-preview__line", children: "\uFF083\uFF09\u8003\u6838\u65B9\u5F0F\uFF1A\u6A21\u62DF\u64CD\u4F5C" }), _jsx(PaperDetailLine, { label: "\uFF084\uFF09\u5177\u4F53\u8003\u6838\u8981\u6C42\uFF1A", children: data.assessmentRequirementsHtml
                    ? _jsx(PaperRichText, { value: data.assessmentRequirementsHtml })
                    : _jsx("span", { className: "competition-paper-preview__placeholder", children: "\u672A\u914D\u7F6E" }) }), _jsx(PaperSupportingInfo, { label: "\u8003\u573A\u51C6\u5907", value: data.examRoomPrepareHtml }), _jsx(PaperSupportingInfo, { label: "\u8003\u751F\u51C6\u5907", value: data.candidatePrepareHtml }), _jsx(PaperSupportingInfo, { label: "\u8BD5\u9898\u6750\u6599", value: data.questionMaterialsHtml }), _jsxs(PaperDetailLine, { label: "\uFF085\uFF09\u5426\u5B9A\u9879\u8BF4\u660E\uFF1A", children: [negativeHasExplanation && _jsx(PaperRichText, { value: data.negativeItemsHtml }), !negativeHasExplanation && (_jsxs(_Fragment, { children: [_jsx("p", { children: "\u82E5\u8003\u751F\u53D1\u751F\u4E0B\u5217\u60C5\u51B5\u4E4B\u4E00\u5E94\u53CA\u65F6\u7EC8\u6B62\u5176\u8003\u8BD5\uFF0C\u8003\u751F\u8BE5\u8003\u8BD5\u8BB0\u4E3A\u96F6\u5206\u3002" }), _jsx(PaperRichText, { value: data.negativeItemsHtml, className: "competition-paper-preview__negative-list" })] }))] })] }));
}
export function CompetitionPracticalSnapshotPaperPreview({ snapshot }) {
    const questions = [...(snapshot.questions || [])].sort((left, right) => (left.sortOrder || 0) - (right.sortOrder || 0));
    return (_jsx("div", { className: "competition-paper-preview", "data-testid": "competition-practical-snapshot-paper", children: _jsxs("article", { className: "competition-paper-preview__sheet", children: [_jsxs("header", { className: "competition-paper-preview__header", children: [_jsx("h1", { children: "\u804C\u4E1A\u6280\u80FD\u7B49\u7EA7\u8BA4\u5B9A" }), _jsx("h2", { children: snapshot.title || '技能现场试卷考核试卷' }), _jsxs("div", { className: "competition-paper-preview__candidate-fields", "aria-label": "\u8003\u751F\u4FE1\u606F", children: [_jsxs("span", { children: ["\u8003\u4EF6\u7F16\u53F7\uFF1A", _jsx("i", { "aria-hidden": "true" })] }), _jsxs("span", { children: ["\u59D3\u540D\uFF1A", _jsx("i", { "aria-hidden": "true" })] }), _jsxs("span", { children: ["\u51C6\u8003\u8BC1\u53F7\uFF1A", _jsx("i", { "aria-hidden": "true" })] })] })] }), _jsxs("section", { className: "competition-paper-preview__notice", children: [_jsx("h2", { children: "\u6CE8 \u610F \u4E8B \u9879" }), _jsx("p", { children: "\u4E00\u3001\u8BF7\u6839\u636E\u8BD5\u9898\u8003\u6838\u8981\u6C42\uFF0C\u5B8C\u6210\u8003\u8BD5\u5185\u5BB9\u3002" }), _jsx("p", { children: "\u4E8C\u3001\u8BF7\u670D\u4ECE\u8003\u8BC4\u4EBA\u5458\u6307\u6325\uFF0C\u4FDD\u8BC1\u8003\u6838\u5B89\u5168\u987A\u5229\u8FDB\u884C\u3002" })] }), questions.length > 0 ? questions.map((question, index) => (_jsx(PracticalSnapshotQuestion, { question: question, index: index, snapshotDurationMinutes: snapshot.durationMinutes }, question.id))) : (_jsx("p", { className: "competition-paper-preview__empty", children: "\u6682\u65E0\u9898\u76EE\u5185\u5BB9" }))] }) }));
}
