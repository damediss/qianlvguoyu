import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { FormalExamStem } from './FormalExamStem';
export function FormalExamChoiceOptions({ multiple, options, selectedOptionIds, disabled, onChange, }) {
    const selected = new Set(selectedOptionIds);
    return (_jsx("div", { className: "formal-exam-options", role: "group", children: options.map((option) => {
            const checked = selected.has(option.optionId);
            return (_jsxs("button", { type: "button", className: `formal-exam-option${checked ? ' is-selected' : ''}`, disabled: disabled, "aria-pressed": checked, onClick: () => {
                    if (disabled)
                        return;
                    if (!multiple) {
                        onChange(checked ? [] : [option.optionId]);
                        return;
                    }
                    const next = new Set(selected);
                    if (checked)
                        next.delete(option.optionId);
                    else
                        next.add(option.optionId);
                    onChange([...next]);
                }, children: [_jsx("span", { className: "formal-exam-option__mark", children: checked ? '●' : '○' }), _jsxs("span", { className: "formal-exam-option__label", children: [option.label, "."] }), _jsx(FormalExamStem, { html: option.content })] }, option.optionId));
        }) }));
}
