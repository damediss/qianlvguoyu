import { jsx as _jsx } from "react/jsx-runtime";
import { DatePicker, Form, Input, Select } from 'antd';
import dayjs from 'dayjs';
import { getFieldConfig, } from './researchCollectionFieldOptions';
export function ResearchCollectionFieldInput({ fieldKey, fallbackLabel, fieldOptions, placeholder, readOnly, ...formItemProps }) {
    const config = getFieldConfig(fieldOptions, fieldKey, fallbackLabel);
    if (!config)
        return null;
    const label = config.label || fallbackLabel;
    const rules = [
        ...(formItemProps.rules ?? []),
        ...(config.required ? [{ required: true, message: `请填写${label}` }] : []),
    ];
    return (_jsx(Form.Item, { ...formItemProps, label: label, rules: rules, children: readOnly ? (_jsx(Input, { readOnly: true, placeholder: placeholder })) : config.inputType === 'date' ? (_jsx(DatePicker, { style: { width: '100%' }, placeholder: placeholder ?? `请选择${label}` })) : config.inputType === 'select' && config.options?.length ? (_jsx(Select, { showSearch: true, allowClear: true, placeholder: placeholder ?? `请选择${label}`, options: config.options?.map((option) => ({ value: option, label: option })), mode: config.allowCustom ? 'tags' : undefined, maxTagCount: config.allowCustom ? 1 : undefined })) : (_jsx(Input, { placeholder: placeholder ?? `请输入${label}` })) }));
}
export function dateFieldGetValueProps(value) {
    return { value: value ? dayjs(value) : undefined };
}
export function dateFieldNormalize(value) {
    return value ? value.format('YYYY-MM-DD') : undefined;
}
