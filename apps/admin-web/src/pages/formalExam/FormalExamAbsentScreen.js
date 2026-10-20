import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
function examTypeLabel(value) {
    if (value === 'theory')
        return '理论考试';
    if (value === 'practical')
        return '实操考试';
    return value || '考试';
}
export function FormalExamAbsentScreen({ context, }) {
    const candidate = context.candidateExam;
    return (_jsx("div", { className: "formal-exam-player", children: _jsxs("div", { className: "formal-exam-player__card formal-exam-absent", children: [_jsx("h1", { className: "formal-exam-player__title", children: examTypeLabel(context.examSession.examType) }), _jsx("p", { className: "formal-exam-player__status", children: "\u672C\u573A\u8003\u8BD5\u5DF2\u7ED3\u675F" }), _jsx("p", { className: "formal-exam-player__subtitle", children: "\u60A8\u672A\u5728\u89C4\u5B9A\u65F6\u95F4\u5185\u8FDB\u5165\u8003\u8BD5\uFF0C" }), _jsx("p", { className: "formal-exam-player__subtitle", children: "\u7CFB\u7EDF\u5DF2\u8BB0\u5F55\u4E3A\u7F3A\u8003\u3002" }), _jsxs("div", { className: "formal-exam-player__meta", children: [_jsxs("p", { children: ["\u59D3\u540D\uFF1A", candidate.candidateName || '-'] }), _jsxs("p", { children: ["\u9009\u624B\u7F16\u53F7\uFF1A", candidate.candidateNo || '-'] })] }), _jsx("p", { className: "formal-exam-player__hint", children: "\u5982\u6709\u7591\u95EE\uFF0C\u8BF7\u8054\u7CFB\u73B0\u573A\u5DE5\u4F5C\u4EBA\u5458\u3002" })] }) }));
}
