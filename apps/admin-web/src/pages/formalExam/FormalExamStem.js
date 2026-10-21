import { jsx as _jsx } from "react/jsx-runtime";
import { sanitizeExamHtml } from './sanitizeExamHtml';
export function FormalExamStem({ html }) {
    const content = html?.trim() ? html : '';
    if (!content)
        return _jsx("p", { className: "formal-exam-stem", children: "-" });
    if (!/[<>]/.test(content)) {
        return _jsx("div", { className: "formal-exam-stem", children: content });
    }
    return (_jsx("div", { className: "formal-exam-stem", dangerouslySetInnerHTML: { __html: sanitizeExamHtml(content) } }));
}
