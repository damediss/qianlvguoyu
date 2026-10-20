import { jsx as _jsx } from "react/jsx-runtime";
import { Input } from 'antd';
export function FormalExamFillInput({ value, disabled, onChange, onCommit }) {
    return (_jsx(Input.TextArea, { className: "formal-exam-fill", value: value, disabled: disabled, autoSize: { minRows: 3, maxRows: 8 }, placeholder: "\u8BF7\u8F93\u5165\u7B54\u6848", onChange: (event) => onChange(event.target.value), onFocus: (event) => {
            event.target.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
        }, onBlur: onCommit }));
}
