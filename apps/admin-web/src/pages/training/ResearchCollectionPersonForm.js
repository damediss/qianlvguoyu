import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Button, DatePicker, Divider, Form, Input, Select, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { ResearchCollectionFieldInput, dateFieldGetValueProps, dateFieldNormalize, } from './ResearchCollectionFieldInput';
import { ALL_COLLECTION_FIELDS, getFieldDef, getOrderedGroups, getOrderedVisibleFields, isCertificateFieldKey, } from './researchCollectionFieldOptions';
const LEVEL_OPTIONS = ['初级工', '中级工', '高级工', '技师', '高级技师'];
function renderBasicField(fieldKey, fieldOptions, companyName) {
    const def = getFieldDef(fieldKey);
    if (!def)
        return null;
    const config = getOrderedVisibleFields(fieldOptions).find((f) => f.key === fieldKey)?.config;
    if (!config && !def.locked)
        return null;
    if (fieldKey === 'name') {
        return (_jsx(Form.Item, { name: "name", label: config?.label || def.label, rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, {}) }, fieldKey));
    }
    if (fieldKey === 'idCardNumber' || fieldKey === 'phone') {
        return (_jsx(Form.Item, { name: fieldKey, label: config?.label || def.label, rules: fieldKey === 'idCardNumber' ? [{ required: true, message: '请输入身份证号码' }] : undefined, children: _jsx(Input, {}) }, fieldKey));
    }
    if (fieldKey === 'companyName') {
        return (_jsx(Form.Item, { name: "companyName", label: config?.label || def.label, initialValue: companyName, children: _jsx(Input, { readOnly: true }) }, fieldKey));
    }
    if (config?.inputType === 'date') {
        return (_jsx(Form.Item, { name: fieldKey, label: config.label || def.label, rules: config.required ? [{ required: true, message: `请选择${config.label || def.label}` }] : undefined, getValueProps: dateFieldGetValueProps, normalize: dateFieldNormalize, children: _jsx(DatePicker, { style: { width: '100%' } }) }, fieldKey));
    }
    return (_jsx(ResearchCollectionFieldInput, { fieldKey: fieldKey, fallbackLabel: def.label, fieldOptions: fieldOptions, name: fieldKey }, fieldKey));
}
export function ResearchCollectionPersonForm({ fieldOptions, companyName, }) {
    const groups = getOrderedGroups(fieldOptions);
    const certFieldKeys = ALL_COLLECTION_FIELDS.filter((f) => isCertificateFieldKey(f.key)).map((f) => f.key);
    const certFields = getOrderedVisibleFields(fieldOptions, 'certificate').filter((f) => isCertificateFieldKey(f.key));
    return (_jsxs(_Fragment, { children: [groups.map((group) => {
                if (group.key === 'certificate')
                    return null;
                const fields = getOrderedVisibleFields(fieldOptions, group.key).filter((f) => !isCertificateFieldKey(f.key));
                if (fields.length === 0)
                    return null;
                return (_jsxs("div", { children: [group.key !== 'basic' && _jsx(Divider, { orientation: "left", plain: true, children: group.label }), fields.map(({ key }) => renderBasicField(key, fieldOptions, companyName))] }, group.key));
            }), certFields.length > 0 && (_jsxs(_Fragment, { children: [_jsx(Divider, { orientation: "left", plain: true, children: groups.find((g) => g.key === 'certificate')?.label || '技能等级证书' }), _jsx(Form.List, { name: "certificates", children: (fields, { add, remove }) => (_jsxs(_Fragment, { children: [fields.map((field) => (_jsxs(Space, { style: { display: 'flex', marginBottom: 8 }, align: "baseline", wrap: true, children: [certFieldKeys.includes('certLevel') && certFields.some((f) => f.key === 'certLevel') && (_jsx(Form.Item, { ...field, name: [field.name, 'level'], label: certFields.find((f) => f.key === 'certLevel')?.config.label || '已有证书等级', children: _jsx(Select, { allowClear: true, style: { width: 130 }, options: LEVEL_OPTIONS.map((v) => ({ value: v, label: v })) }) })), certFieldKeys.includes('certNo') && certFields.some((f) => f.key === 'certNo') && (_jsx(Form.Item, { ...field, name: [field.name, 'certNo'], label: certFields.find((f) => f.key === 'certNo')?.config.label || '证书编号', children: _jsx(Input, { style: { width: 160 } }) })), certFieldKeys.includes('certIssueDate') && certFields.some((f) => f.key === 'certIssueDate') && (_jsx(Form.Item, { ...field, name: [field.name, 'issueDate'], label: certFields.find((f) => f.key === 'certIssueDate')?.config.label || '发证日期', getValueProps: (v) => ({ value: v ? dayjs(v) : undefined }), normalize: (v) => (v ? v.format('YYYY-MM-DD') : undefined), children: _jsx(DatePicker, { style: { width: 150 } }) })), fields.length > 1 && (_jsx(Button, { type: "link", danger: true, onClick: () => remove(field.name), children: "\u5220\u9664" }))] }, field.key))), _jsx(Button, { type: "dashed", onClick: () => add(), block: true, icon: _jsx(PlusOutlined, {}), children: "\u6DFB\u52A0\u8BC1\u4E66" })] })) })] }))] }));
}
