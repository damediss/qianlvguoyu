import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, PlusOutlined, SaveOutlined, StopOutlined } from '@ant-design/icons';
import { Button, Input, InputNumber, Modal, Select, Space, Typography, message } from 'antd';
import { createEmptyWbsItem, validateWbsItems, wbsCategoryOptions, wbsStatusOptions, } from './constants';
const containerStyle = {
    maxWidth: 1200,
    margin: '0 auto',
    borderRadius: 12,
    overflow: 'hidden',
    background: '#fff',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    border: '1px solid #eef2f7',
};
const headerStyle = {
    padding: 32,
    color: '#fff',
    background: 'linear-gradient(135deg, #fa8c16 0%, #d46b08 100%)',
};
const toolbarStyle = {
    padding: '20px 32px',
    background: '#f8fafc',
    borderBottom: '1px solid #e5e7eb',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    flexWrap: 'wrap',
};
const bodyStyle = {
    padding: 32,
};
const taskCardStyle = {
    background: '#f8fafc',
    border: '1px solid #e5e7eb',
    borderRadius: 12,
    padding: 24,
    marginBottom: 20,
};
const taskHeaderStyle = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
    paddingBottom: 16,
    borderBottom: '1px dashed #e5e7eb',
};
const taskNumberStyle = {
    width: 32,
    height: 32,
    borderRadius: 999,
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#fff',
    background: '#1890ff',
    fontWeight: 700,
    fontSize: 14,
};
const gridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
    gap: 16,
};
function requiredLabel(text) {
    return (_jsxs("span", { children: [text, _jsx("span", { style: { color: '#ff4d4f', marginLeft: 4 }, children: "*" })] }));
}
function updateItem(items, index, patch) {
    return items.map((row, i) => (i === index ? { ...row, ...patch } : row));
}
export function WbsEditor({ items, saving, onChange, onSave, onPreview, onCancel, }) {
    const handleSave = () => {
        const error = validateWbsItems(items);
        if (error) {
            message.warning(error);
            return;
        }
        onSave();
    };
    const handleDelete = (index) => {
        Modal.confirm({
            title: '确定要删除该任务吗？',
            okText: '删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => onChange(items.filter((_, i) => i !== index)),
        });
    };
    return (_jsxs("div", { style: containerStyle, children: [_jsxs("div", { style: headerStyle, children: [_jsx(Typography.Title, { level: 2, style: { margin: 0, color: '#fff' }, children: "\u7F16\u8F91\u4EFB\u52A1 WBS" }), _jsx(Typography.Paragraph, { style: { margin: '8px 0 0', color: 'rgba(255,255,255,0.9)' }, children: "\u8BF7\u586B\u5199\u6216\u4FEE\u6539\u5DE5\u4F5C\u4EFB\u52A1\u5206\u89E3\u4FE1\u606F\uFF0C\u5B57\u6BB5\u4E0E\u6570\u636E\u5E93\u8868 certification_wbs_item \u4E00\u81F4" })] }), _jsxs("div", { style: toolbarStyle, children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), loading: saving, onClick: handleSave, children: "\u4FDD\u5B58\u4FEE\u6539" }), _jsx(Button, { icon: _jsx(StopOutlined, {}), onClick: () => {
                                    Modal.confirm({
                                        title: '确定要取消编辑吗？',
                                        content: '未保存的修改将丢失。',
                                        okText: '确定取消',
                                        cancelText: '继续编辑',
                                        onOk: onCancel,
                                    });
                                }, children: "\u53D6\u6D88" })] }), _jsx(Button, { type: "link", onClick: onPreview, children: "\u2190 \u8FD4\u56DE\u67E5\u770B\u9875" })] }), _jsxs("div", { style: bodyStyle, children: [items.map((item, index) => (_jsxs("div", { style: taskCardStyle, children: [_jsxs("div", { style: taskHeaderStyle, children: [_jsx("span", { style: taskNumberStyle, children: index + 1 }), _jsx(Button, { type: "text", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(index) })] }), _jsxs("div", { style: gridStyle, children: [_jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: requiredLabel('WBS编码') }), _jsx(Input, { value: item.wbsCode, placeholder: "\u4F8B\u5982\uFF1A0110", onChange: (e) => onChange(updateItem(items, index, { wbsCode: e.target.value })) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: requiredLabel('任务分类') }), _jsx(Select, { style: { width: '100%' }, value: item.categoryCode || undefined, placeholder: "\u8BF7\u9009\u62E9", options: wbsCategoryOptions.map((option) => ({ label: option.label, value: option.value })), onChange: (value) => {
                                                    const matched = wbsCategoryOptions.find((option) => option.value === value);
                                                    onChange(updateItem(items, index, {
                                                        categoryCode: value,
                                                        categoryName: matched?.name,
                                                    }));
                                                } })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: requiredLabel('事项名称') }), _jsx(Input, { value: item.itemName, placeholder: "\u8BF7\u8F93\u5165\u4E8B\u9879\u540D\u79F0", onChange: (e) => onChange(updateItem(items, index, { itemName: e.target.value })) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: "\u72B6\u6001" }), _jsx(Select, { style: { width: '100%' }, value: item.status || 'pending', options: wbsStatusOptions, onChange: (value) => onChange(updateItem(items, index, { status: value })) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: requiredLabel('估算工时') }), _jsx(InputNumber, { min: 0, style: { width: '100%' }, value: item.estimateHours, placeholder: "\u4F8B\u5982\uFF1A24", onChange: (value) => onChange(updateItem(items, index, { estimateHours: value ?? undefined })) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: requiredLabel('工期（天）') }), _jsx(InputNumber, { min: 0, style: { width: '100%' }, value: item.durationDays, placeholder: "\u4F8B\u5982\uFF1A2", onChange: (value) => onChange(updateItem(items, index, { durationDays: value ?? undefined })) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: requiredLabel('费用估算(元)') }), _jsx(InputNumber, { min: 0, style: { width: '100%' }, value: item.costAmount, placeholder: "\u4F8B\u5982\uFF1A2000", onChange: (value) => onChange(updateItem(items, index, { costAmount: value ?? undefined })) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: "\u4EBA\u529B\u6570\u91CF" }), _jsx(InputNumber, { min: 0, style: { width: '100%' }, value: item.manpowerCount, placeholder: "\u4F8B\u5982\uFF1A1", onChange: (value) => onChange(updateItem(items, index, { manpowerCount: value ?? undefined })) })] }), _jsxs("label", { style: { gridColumn: 'span 2' }, children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: requiredLabel('活动描述') }), _jsx(Input.TextArea, { rows: 2, value: item.activityDescription, placeholder: "\u8BF7\u8F93\u5165\u5305\u542B\u7684\u6D3B\u52A8", onChange: (e) => onChange(updateItem(items, index, { activityDescription: e.target.value })) })] }), _jsxs("label", { style: { gridColumn: 'span 2' }, children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: requiredLabel('人力角色') }), _jsx(Input, { value: item.manpowerRoleCode, placeholder: "\u4F8B\u5982\uFF1A\u4EFB\u52A1\u7ECF\u7406", onChange: (e) => onChange(updateItem(items, index, { manpowerRoleCode: e.target.value })) })] }), _jsxs("label", { style: { gridColumn: 'span 2' }, children: [_jsx("div", { style: { marginBottom: 6, fontWeight: 500 }, children: "\u5176\u4ED6\u8D44\u6E90\u6295\u5165" }), _jsx(Input, { value: item.resourceInput, placeholder: "\u8BF7\u8F93\u5165\u5176\u4ED6\u8D44\u6E90", onChange: (e) => onChange(updateItem(items, index, { resourceInput: e.target.value })) })] })] })] }, item.id || `wbs-${index}`))), _jsx("div", { style: {
                            display: 'flex',
                            justifyContent: 'center',
                            padding: 20,
                            border: '2px dashed #e5e7eb',
                            borderRadius: 10,
                        }, children: _jsx(Button, { icon: _jsx(PlusOutlined, {}), onClick: () => onChange([...items, createEmptyWbsItem(items.length)]), children: "\u6DFB\u52A0\u65B0\u4EFB\u52A1" }) })] })] }));
}
