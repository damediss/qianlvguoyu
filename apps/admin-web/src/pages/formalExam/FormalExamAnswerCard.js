import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Drawer } from 'antd';
import { isAnsweredLocal } from './formalExamAnswerState';
export function FormalExamAnswerCard({ open, questions, states, currentId, onClose, onJump, }) {
    return (_jsx(Drawer, { title: "\u7B54\u9898\u5361", placement: "bottom", height: "72%", open: open, onClose: onClose, className: "formal-exam-card-drawer", destroyOnHidden: true, children: _jsx("div", { className: "formal-exam-card", children: questions.map((question) => {
                const state = states[question.questionSnapshotId];
                const answered = state ? isAnsweredLocal(question.type, state.answer) : false;
                const marked = Boolean(state?.marked);
                const pending = answered && state?.saveStatus !== 'saved';
                const current = question.questionSnapshotId === currentId;
                const className = [
                    'formal-exam-card__item',
                    answered ? 'is-answered' : '',
                    pending ? 'is-pending' : '',
                    marked ? 'is-marked' : '',
                    current ? 'is-current' : '',
                ].filter(Boolean).join(' ');
                return (_jsxs("button", { type: "button", className: className, onClick: () => onJump(question.questionSnapshotId), children: [question.order, marked ? _jsx("span", { className: "formal-exam-card__flag", children: "\u6807" }) : null] }, question.questionSnapshotId));
            }) }) }));
}
