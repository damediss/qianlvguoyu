import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { Input, Popover, theme } from 'antd';
import { CURATED_ICONS, iconFromName } from '../utils/icons';
import * as Icons from '@ant-design/icons';
export function IconPicker({ value, onChange, placeholder }) {
    const [open, setOpen] = useState(false);
    const [filter, setFilter] = useState('');
    const { token } = theme.useToken();
    const filteredIcons = CURATED_ICONS.filter((name) => name.toLowerCase().includes(filter.toLowerCase()));
    const handleSelect = (iconName) => {
        onChange?.(iconName);
        setOpen(false);
        setFilter('');
    };
    const content = (_jsxs("div", { style: { width: 280 }, children: [_jsx(Input, { placeholder: "\u641C\u7D22\u56FE\u6807...", value: filter, onChange: (e) => setFilter(e.target.value), allowClear: true, style: { marginBottom: 12 } }), _jsx("div", { style: {
                    display: 'grid',
                    gridTemplateColumns: 'repeat(6, 1fr)',
                    gap: 8,
                    maxHeight: 280,
                    overflowY: 'auto',
                }, children: filteredIcons.map((iconName) => {
                    const isSelected = value === iconName;
                    return (_jsxs("div", { onClick: () => handleSelect(iconName), style: {
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            padding: 8,
                            cursor: 'pointer',
                            borderRadius: 6,
                            border: `2px solid ${isSelected ? token.colorPrimary : 'transparent'}`,
                            backgroundColor: isSelected ? token.colorPrimaryBg : 'transparent',
                            transition: 'all 0.2s',
                        }, onMouseEnter: (e) => {
                            if (!isSelected) {
                                e.currentTarget.style.backgroundColor = token.colorBgTextHover;
                            }
                        }, onMouseLeave: (e) => {
                            if (!isSelected) {
                                e.currentTarget.style.backgroundColor = 'transparent';
                            }
                        }, children: [_jsx("span", { style: { fontSize: 20 }, children: iconFromName(iconName) }), _jsx("span", { style: {
                                    fontSize: 10,
                                    marginTop: 4,
                                    textAlign: 'center',
                                    wordBreak: 'break-all',
                                    lineHeight: 1.2,
                                }, children: iconName.replace('Outlined', '') })] }, iconName));
                }) }), filteredIcons.length === 0 && (_jsx("div", { style: { textAlign: 'center', padding: 16, color: token.colorTextSecondary }, children: "\u672A\u627E\u5230\u5339\u914D\u7684\u56FE\u6807" }))] }));
    return (_jsx(Popover, { content: content, trigger: "click", open: open, onOpenChange: setOpen, placement: "bottomLeft", children: _jsx(Input, { readOnly: true, value: value || '', placeholder: placeholder, prefix: value ? iconFromName(value) : null, suffix: value ? (_jsx("span", { onClick: (e) => {
                    e.stopPropagation();
                    onChange?.(undefined);
                }, style: { cursor: 'pointer', display: 'inline-flex', alignItems: 'center' }, children: _jsx(Icons.CloseCircleOutlined, {}) })) : null, style: { cursor: 'pointer' } }) }));
}
