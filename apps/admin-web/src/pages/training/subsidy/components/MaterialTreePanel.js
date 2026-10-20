import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Checkbox, Empty, Modal, Space, Typography, Upload, message } from 'antd';
import { MaterialStatusTag } from './MaterialStatusTag';
export function MaterialTreePanel({ groups, selectedInstanceId, exportSelectedIds, onSelectInstance, onToggleExport, onToggleAllExport, onReplaceFile, onDeleteInstance, replaceLoading, deleteLoadingId, }) {
    const allIds = groups.flatMap((g) => g.items.map((i) => String(i.id)));
    const allChecked = allIds.length > 0 && allIds.every((id) => exportSelectedIds.has(id));
    const indeterminate = exportSelectedIds.size > 0 && !allChecked;
    if (!groups.length) {
        return _jsx(Empty, { description: "\u6682\u65E0\u6750\u6599\u9879\uFF0C\u8BF7\u786E\u8BA4\u914D\u65B9\u5DF2\u5339\u914D" });
    }
    const confirmDelete = (item) => {
        if (!onDeleteInstance)
            return;
        const id = String(item.id);
        Modal.confirm({
            title: '删除该材料？',
            content: item.required
                ? `「${item.title}」为配方必填项，删除后将从本班清单中移除（可之后用模板填数等方式再补回）。`
                : `确认删除「${item.title}」？删除后不可在清单中恢复该条（历史版本一并不再展示）。`,
            okText: '删除',
            okButtonProps: { danger: true },
            cancelText: '取消',
            onOk: async () => {
                try {
                    await onDeleteInstance(id);
                    message.success('已删除');
                }
                catch (e) {
                    message.error(e.message || '删除失败');
                    throw e;
                }
            },
        });
    };
    return (_jsxs("div", { className: "material-tree-panel", children: [_jsxs("div", { className: "material-tree-toolbar", children: [_jsx(Checkbox, { checked: allChecked, indeterminate: indeterminate, onChange: (e) => onToggleAllExport(e.target.checked, allIds), children: "\u5168\u9009\u5BFC\u51FA" }), _jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: ["\u5DF2\u9009 ", exportSelectedIds.size, "/", allIds.length] })] }), _jsx(Typography.Paragraph, { type: "secondary", style: { fontSize: 12, margin: '0 0 8px' }, children: "\u9ED8\u8BA4\u4EC5\u540C\u6B65\u57F9\u8BAD\u7167\u7247\u4E0E\u8BC1\u4E66\u6750\u6599\uFF1B\u7533\u8BF7\u8868 / \u82B1\u540D\u518C / \u6C47\u603B\u8868\u8BF7\u7528\u4E0A\u65B9\u300C\u4ECE\u6A21\u677F\u751F\u6210\u300D\u6216\u300C\u66FF\u6362\u4E0A\u4F20\u300D\u3002" }), groups.map((group) => (_jsxs("div", { className: "material-tree-group", children: [_jsxs("div", { className: "material-tree-group-title", children: [group.label, " (", group.count, ")"] }), group.items.map((item) => {
                        const id = String(item.id);
                        const active = selectedInstanceId === id;
                        return (_jsxs("div", { className: `material-tree-item${active ? ' material-tree-item--active' : ''}`, children: [_jsx(Checkbox, { checked: exportSelectedIds.has(id), onChange: (e) => {
                                        e.stopPropagation();
                                        onToggleExport(id, e.target.checked);
                                    }, onClick: (e) => e.stopPropagation() }), _jsxs("button", { type: "button", className: "material-tree-item-body", onClick: () => onSelectInstance(id), children: [_jsxs("div", { className: "material-tree-item-title", children: [item.required ? _jsx("span", { className: "material-required-dot", children: "*" }) : null, item.title] }), _jsxs(Space, { size: 4, wrap: true, children: [_jsx(MaterialStatusTag, { status: item.status }), item.hasFile ? (_jsx(Typography.Text, { type: "secondary", style: { fontSize: 11 }, children: "\u6709\u6587\u4EF6" })) : null] })] }), _jsxs("div", { className: "material-tree-item-actions", children: [_jsx(Upload, { showUploadList: false, openFileDialogOnClick: true, beforeUpload: (file) => {
                                                onReplaceFile(id, file);
                                                return false;
                                            }, children: _jsx(Button, { size: "small", type: "link", loading: replaceLoading && active, children: "\u66FF\u6362" }) }), onDeleteInstance ? (_jsx(Button, { size: "small", type: "link", danger: true, loading: deleteLoadingId === id, onClick: (e) => {
                                                e.stopPropagation();
                                                confirmDelete(item);
                                            }, children: "\u5220\u9664" })) : null] })] }, id));
                    })] }, group.materialType)))] }));
}
