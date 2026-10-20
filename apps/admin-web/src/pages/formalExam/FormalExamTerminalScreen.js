import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { formalExamLocalAnswerRepository } from './idb/FormalExamLocalAnswerRepository';
import { formatSubmitClock } from './formalExamTime';
function examTypeLabel(value) {
    if (value === 'theory')
        return '理论考试';
    if (value === 'practical')
        return '实操考试';
    return value || '考试';
}
function copyOf(status) {
    if (status === 'auto_submitted') {
        return {
            headline: '考试时间已到',
            detail: '系统已自动提交试卷',
            hint: '请等待现场工作人员下一步安排',
        };
    }
    if (status === 'forced_submitted') {
        return {
            headline: '本场考试已由工作人员结束提交',
            detail: null,
            hint: '请联系现场工作人员',
        };
    }
    return {
        headline: '试卷提交成功',
        detail: null,
        hint: '请等待现场工作人员下一步安排',
    };
}
export function FormalExamTerminalScreen({ context, }) {
    const candidate = context.candidateExam;
    const copy = copyOf(candidate.status);
    const [unsyncedCount, setUnsyncedCount] = useState(0);
    useEffect(() => {
        let cancelled = false;
        void formalExamLocalAnswerRepository
            .countUnsynced(String(context.examSession.id), String(candidate.id))
            .then((count) => {
            if (!cancelled)
                setUnsyncedCount(count);
        })
            .catch(() => {
            if (!cancelled)
                setUnsyncedCount(0);
        });
        return () => {
            cancelled = true;
        };
    }, [candidate.id, context.examSession.id]);
    return (_jsx("div", { className: "formal-exam-player", children: _jsxs("div", { className: "formal-exam-player__card formal-exam-submitted", children: [_jsx("h1", { className: "formal-exam-player__title", children: examTypeLabel(context.examSession.examType) }), _jsx("p", { className: "formal-exam-submitted__mark", "aria-hidden": "true", children: "\u2713" }), _jsx("p", { className: "formal-exam-player__status", children: copy.headline }), copy.detail ? _jsx("p", { className: "formal-exam-player__subtitle", children: copy.detail }) : null, _jsxs("div", { className: "formal-exam-player__meta", children: [_jsxs("p", { children: ["\u59D3\u540D\uFF1A", candidate.candidateName || '-'] }), _jsxs("p", { children: ["\u9009\u624B\u7F16\u53F7\uFF1A", candidate.candidateNo || '-'] }), _jsxs("p", { children: ["\u63D0\u4EA4\u65F6\u95F4\uFF1A", formatSubmitClock(candidate.submitTime)] })] }), unsyncedCount > 0 ? (_jsxs("p", { className: "formal-exam-player__unsynced", children: ["\u672C\u8BBE\u5907\u4ECD\u4FDD\u7559 ", unsyncedCount, " \u9053\u672A\u540C\u6B65\u7684\u672C\u5730\u4F5C\u7B54\u8BB0\u5F55\u3002 \u8FD9\u4E9B\u8BB0\u5F55\u672A\u81EA\u52A8\u8BA1\u5165\u6B63\u5F0F\u63D0\u4EA4\u7ED3\u679C\u3002 \u8BF7\u52FF\u5378\u8F7D\u6216\u6E05\u9664\u5E94\u7528\uFF0C\u5E76\u8054\u7CFB\u73B0\u573A\u5DE5\u4F5C\u4EBA\u5458\u3002"] })) : null, _jsx("p", { className: "formal-exam-player__hint", children: copy.hint })] }) }));
}
