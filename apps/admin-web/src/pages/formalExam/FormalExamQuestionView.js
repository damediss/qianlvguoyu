import { jsxs as _jsxs, jsx as _jsx } from "react/jsx-runtime";
import { EXAM_TYPE_MAP } from '../../types/exam';
import { FormalExamChoiceOptions } from './FormalExamChoiceOptions';
import { FormalExamFillInput } from './FormalExamFillInput';
import { FormalExamStem } from './FormalExamStem';
export function FormalExamQuestionView({ question, answer, disabled, onChoiceChange, onFillChange, onFillCommit, }) {
    const type = question.type;
    const typeLabel = EXAM_TYPE_MAP[type] || (type === 'multiple' ? EXAM_TYPE_MAP.multi : undefined);
    const typeTone = type === 'single' ? 'single'
        : type === 'multi' || type === 'multiple' ? 'multi'
            : type === 'judge' ? 'judge' : undefined;
    return (_jsxs("div", { className: "formal-exam-question", children: [_jsxs("div", { className: "formal-exam-question__title", children: [_jsxs("span", { children: [question.order, "."] }), _jsx(FormalExamStem, { html: question.stem }), typeLabel ? _jsx("span", { className: `formal-exam-question__type${typeTone ? ` formal-exam-question__type--${typeTone}` : ''}`, children: typeLabel }) : null] }), type === 'fill' ? (_jsx(FormalExamFillInput, { value: answer.blanks?.[0] || '', disabled: disabled, onChange: onFillChange, onCommit: onFillCommit })) : (_jsx(FormalExamChoiceOptions, { multiple: type === 'multi', options: question.options || [], selectedOptionIds: answer.selectedOptionIds || [], disabled: disabled, onChange: onChoiceChange }))] }));
}
