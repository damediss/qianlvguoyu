import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ArrowDownOutlined, ArrowUpOutlined, PlusOutlined, } from '@ant-design/icons';
import { Button, Checkbox, Collapse, Input, Select, Space, Tag, Typography, } from 'antd';
import { useMemo, useState } from 'react';
import { ALL_COLLECTION_FIELDS, DEFAULT_COLLECTION_GROUPS, normalizeCollectionFieldOptions, } from './researchCollectionFieldOptions';
const { Text } = Typography;
export function ResearchCollectionFieldOptionsEditor({ value, onChange, }) {
    const normalized = useMemo(() => normalizeCollectionFieldOptions(value), [value]);
    const fields = normalized.fields ?? {};
    const groups = normalized.groups ?? DEFAULT_COLLECTION_GROUPS;
    const [draftOption, setDraftOption] = useState({});
    const emit = (next) => {
        onChange?.(normalizeCollectionFieldOptions(next));
    };
    const patchField = (key, patch) => {
        emit({
            ...normalized,
            fields: {
                ...fields,
                [key]: {
                    ...fields[key],
                    ...patch,
                },
            },
        });
    };
    const moveGroup = (index, direction) => {
        const target = index + direction;
        if (target < 0 || target >= groups.length)
            return;
        const nextGroups = groups.map((g) => ({ ...g }));
        const currentOrder = nextGroups[index].order;
        nextGroups[index].order = nextGroups[target].order;
        nextGroups[target].order = currentOrder;
        nextGroups.sort((a, b) => a.order - b.order);
        emit({ ...normalized, groups: nextGroups });
    };
    const moveField = (groupKey, fieldKey, direction) => {
        const groupFields = ALL_COLLECTION_FIELDS
            .filter((def) => (fields[def.key]?.group || def.group) === groupKey)
            .map((def) => def.key);
        const sorted = [...groupFields].sort((a, b) => (fields[a]?.order ?? 0) - (fields[b]?.order ?? 0));
        const index = sorted.indexOf(fieldKey);
        const target = index + direction;
        if (index < 0 || target < 0 || target >= sorted.length)
            return;
        const a = sorted[index];
        const b = sorted[target];
        const orderA = fields[a]?.order ?? 0;
        const orderB = fields[b]?.order ?? 0;
        emit({
            ...normalized,
            fields: {
                ...fields,
                [a]: { ...fields[a], order: orderB },
                [b]: { ...fields[b], order: orderA },
            },
        });
    };
    const addOption = (key) => {
        const text = (draftOption[key] ?? '').trim();
        if (!text)
            return;
        const current = fields[key]?.options ?? [];
        if (current.includes(text)) {
            setDraftOption((prev) => ({ ...prev, [key]: '' }));
            return;
        }
        patchField(key, { options: [...current, text], inputType: 'select' });
        setDraftOption((prev) => ({ ...prev, [key]: '' }));
    };
    const removeOption = (key, option) => {
        const current = fields[key]?.options ?? [];
        patchField(key, { options: current.filter((item) => item !== option) });
    };
    return (_jsx(Collapse, { ghost: true, defaultActiveKey: ['field-options'], items: [
            {
                key: 'field-options',
                label: '人员采集字段配置（全部字段）',
                children: (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(Text, { type: "secondary", children: "\u53EF\u914D\u7F6E\u663E\u793A/\u9690\u85CF\u3001\u5FC5\u586B\u3001\u6807\u7B7E\u3001\u8F93\u5165\u7C7B\u578B\u3001\u4E0B\u62C9\u9009\u9879\uFF0C\u5E76\u8C03\u6574\u5206\u7EC4\u4E0E\u5B57\u6BB5\u987A\u5E8F\u3002\u5F00\u542F\u91C7\u96C6\u540E\u914D\u7F6E\u4F1A\u5FEB\u7167\u5230\u5F53\u524D\u6279\u6B21\u3002" }), groups.map((group, groupIndex) => {
                            const groupFields = ALL_COLLECTION_FIELDS.filter((def) => (fields[def.key]?.group || def.group) === group.key).sort((a, b) => (fields[a.key]?.order ?? a.order) - (fields[b.key]?.order ?? b.order));
                            return (_jsxs("div", { style: {
                                    border: '1px solid #e8e8e8',
                                    borderRadius: 8,
                                    padding: 12,
                                    background: '#fff',
                                }, children: [_jsxs("div", { style: { display: 'flex', justifyContent: 'space-between', marginBottom: 12 }, children: [_jsx(Input, { value: group.label, onChange: (e) => {
                                                    const nextGroups = groups.map((g) => g.key === group.key ? { ...g, label: e.target.value } : g);
                                                    emit({ ...normalized, groups: nextGroups });
                                                }, style: { maxWidth: 260, fontWeight: 600 } }), _jsxs(Space, { children: [_jsx(Button, { size: "small", icon: _jsx(ArrowUpOutlined, {}), disabled: groupIndex === 0, onClick: () => moveGroup(groupIndex, -1) }), _jsx(Button, { size: "small", icon: _jsx(ArrowDownOutlined, {}), disabled: groupIndex === groups.length - 1, onClick: () => moveGroup(groupIndex, 1) })] })] }), groupFields.map((def, fieldIndex) => {
                                        const config = fields[def.key];
                                        const options = config?.options ?? [];
                                        const inputType = config?.inputType || def.inputType;
                                        return (_jsxs("div", { style: {
                                                border: '1px solid #f0f0f0',
                                                borderRadius: 8,
                                                padding: 12,
                                                marginBottom: 10,
                                                background: '#fafafa',
                                            }, children: [_jsxs("div", { style: { display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 8 }, children: [_jsx(Input, { value: config?.label || def.label, disabled: def.locked, onChange: (e) => patchField(def.key, { label: e.target.value }), style: { flex: 1 } }), _jsxs(Space, { children: [_jsx(Button, { size: "small", icon: _jsx(ArrowUpOutlined, {}), disabled: fieldIndex === 0, onClick: () => moveField(group.key, def.key, -1) }), _jsx(Button, { size: "small", icon: _jsx(ArrowDownOutlined, {}), disabled: fieldIndex === groupFields.length - 1, onClick: () => moveField(group.key, def.key, 1) })] })] }), _jsxs(Space, { wrap: true, style: { marginBottom: 8 }, children: [_jsx(Select, { size: "small", value: inputType, disabled: def.locked, style: { width: 110 }, options: [
                                                                { value: 'text', label: '文本' },
                                                                { value: 'select', label: '下拉' },
                                                                { value: 'date', label: '日期' },
                                                            ], onChange: (v) => patchField(def.key, { inputType: v }) }), _jsx(Checkbox, { checked: def.locked ? true : config?.visible !== false, disabled: def.locked, onChange: (e) => patchField(def.key, { visible: e.target.checked }), children: "\u663E\u793A" }), _jsx(Checkbox, { checked: def.locked ? def.key === 'name' || def.key === 'idCardNumber' : !!config?.required, disabled: def.locked && (def.key === 'name' || def.key === 'idCardNumber'), onChange: (e) => patchField(def.key, { required: e.target.checked }), children: "\u5FC5\u586B" }), inputType === 'select' && (_jsx(Checkbox, { checked: config?.allowCustom ?? def.defaultAllowCustom ?? false, onChange: (e) => patchField(def.key, { allowCustom: e.target.checked }), children: "\u5141\u8BB8\u81EA\u5B9A\u4E49" }))] }), inputType === 'select' && (_jsxs(_Fragment, { children: [_jsxs(Space, { wrap: true, style: { marginBottom: 8 }, children: [options.map((option) => (_jsx(Tag, { closable: true, onClose: (e) => {
                                                                        e.preventDefault();
                                                                        removeOption(def.key, option);
                                                                    }, children: option }, option))), options.length === 0 && (_jsx(Text, { type: "secondary", children: "\u6682\u65E0\u9009\u9879\uFF0C\u53EF\u6DFB\u52A0\u6216\u4F7F\u7528\u9ED8\u8BA4" }))] }), _jsxs(Space.Compact, { style: { width: '100%' }, children: [_jsx(Input, { placeholder: "\u6DFB\u52A0\u4E0B\u62C9\u9009\u9879", value: draftOption[def.key] ?? '', onChange: (e) => setDraftOption((prev) => ({ ...prev, [def.key]: e.target.value })), onPressEnter: () => addOption(def.key) }), _jsx(Button, { icon: _jsx(PlusOutlined, {}), onClick: () => addOption(def.key), children: "\u6DFB\u52A0" })] })] }))] }, def.key));
                                    })] }, group.key));
                        })] })),
            },
        ] }));
}
