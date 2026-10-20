import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { Alert, Button, Form, Input, Space, Spin, Typography, message } from 'antd';
import * as trainApi from '../../../../api/training';
function isSameCellLabeled(line) {
    if (line.kind !== 'meta')
        return false;
    const t = (line.text || '').trim();
    return t.startsWith(line.label);
}
export function DocxMetaEditor({ instanceId, onSaved, onCancel, onOpenFullEditor, onlyOfficeAvailable = false, }) {
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [meta, setMeta] = useState(null);
    const [error, setError] = useState(null);
    const [form] = Form.useForm();
    useEffect(() => {
        let cancelled = false;
        const load = async () => {
            setLoading(true);
            setError(null);
            try {
                const data = await trainApi.getSubsidyMaterialDocxMeta(instanceId);
                if (cancelled)
                    return;
                setMeta(data);
                const values = {};
                for (const line of data.lines || []) {
                    values[`p_${line.index}`] =
                        line.kind === 'meta' || line.kind === 'caption' ? line.value : line.text;
                }
                form.setFieldsValue(values);
            }
            catch (e) {
                if (!cancelled) {
                    setError(e.message || '加载 Word 字段失败');
                }
            }
            finally {
                if (!cancelled)
                    setLoading(false);
            }
        };
        void load();
        return () => {
            cancelled = true;
        };
    }, [instanceId, form]);
    const handleSave = async () => {
        if (!meta?.lines?.length) {
            message.warning('没有可保存的字段');
            return;
        }
        const values = await form.validateFields();
        const paragraphs = {};
        for (const line of meta.lines) {
            const raw = values[`p_${line.index}`] ?? '';
            if (line.kind === 'meta') {
                paragraphs[line.index] = isSameCellLabeled(line)
                    ? `${line.label}：${raw}`
                    : String(raw);
            }
            else if (line.kind === 'caption') {
                const t = String(raw).trim();
                paragraphs[line.index] = t.startsWith('图') ? t : `${line.label}：${t}`;
            }
            else {
                paragraphs[line.index] = String(raw);
            }
        }
        setSaving(true);
        try {
            await trainApi.patchSubsidyMaterialDocx(instanceId, paragraphs);
            message.success('已保存 Word（保留图片与版式）');
            onSaved?.();
        }
        catch (e) {
            message.error(e.message || '保存失败');
        }
        finally {
            setSaving(false);
        }
    };
    if (loading) {
        return (_jsx("div", { style: { padding: 24, textAlign: 'center' }, children: _jsx(Spin, { tip: "\u89E3\u6790 Word \u5B57\u6BB5\u2026" }) }));
    }
    if (error) {
        return (_jsx(Alert, { type: "error", showIcon: true, message: "\u65E0\u6CD5\u89E3\u6790 Word \u5B57\u6BB5", description: _jsxs("div", { children: [_jsx("div", { children: error }), _jsx("div", { style: { marginTop: 8 }, children: "\u8BF7\u7528\u300C\u4E0B\u8F7D\u300D\u4FEE\u6539\u540E\u300C\u66FF\u6362\u4E0A\u4F20\u300D\uFF0C\u6216\u68C0\u67E5\u540E\u7AEF\u662F\u5426\u5DF2\u90E8\u7F72\u6700\u65B0\u586B\u6570/\u7F16\u8F91\u63A5\u53E3\u3002" }), onCancel ? (_jsx(Button, { type: "link", style: { paddingLeft: 0 }, onClick: onCancel, children: "\u9000\u51FA\u7F16\u8F91" })) : null] }) }));
    }
    if (!meta?.lines?.length) {
        return (_jsx(Alert, { type: "info", showIcon: true, message: "\u672A\u8BC6\u522B\u5230\u53EF\u7F16\u8F91\u5B57\u6BB5", description: _jsxs("div", { children: [_jsx("div", { children: "\u5F53\u524D\u6587\u6863\u672A\u5339\u914D\u5230\u300C\u57F9\u8BAD\u673A\u6784 / \u57F9\u8BAD\u65F6\u95F4 / \u7ECF\u529E\u4EBA\u300D\u7B49\u680F\u4F4D\u3002\u8BF7\u300C\u4E0B\u8F7D\u300D\u540E\u672C\u5730\u4FEE\u6539\uFF0C\u518D\u300C\u66FF\u6362\u4E0A\u4F20\u300D\u3002" }), onlyOfficeAvailable && onOpenFullEditor ? (_jsx(Button, { type: "link", style: { paddingLeft: 0 }, onClick: onOpenFullEditor, children: "\u4F7F\u7528\u5B8C\u6574\u6392\u7248\u7F16\u8F91\uFF08OnlyOffice\uFF09" })) : null, onCancel ? (_jsx(Button, { type: "link", onClick: onCancel, children: "\u9000\u51FA\u7F16\u8F91" })) : null] }) }));
    }
    return (_jsxs("div", { className: "material-docx-meta-editor", style: { padding: '12px 16px' }, children: [_jsxs(Typography.Paragraph, { type: "secondary", style: { marginBottom: 12 }, children: ["\u4FEE\u6539\u4E0B\u5217\u5B57\u6BB5\u540E\u4FDD\u5B58\u5373\u53EF\uFF08\u65E0\u9700 OnlyOffice\uFF09\u3002\u540E\u7AEF\u4F1A\u5199\u56DE docx\uFF0C\u4FDD\u7559\u56FE\u7247\u4E0E\u8868\u683C\u7248\u5F0F\u3002", meta?.fileName ? (_jsxs(_Fragment, { children: [' ', "\u6587\u4EF6\uFF1A", _jsx(Typography.Text, { code: true, children: meta.fileName })] })) : null, onlyOfficeAvailable && onOpenFullEditor ? (_jsxs(_Fragment, { children: [' ', _jsx(Button, { type: "link", size: "small", onClick: onOpenFullEditor, children: "\u5B8C\u6574\u6392\u7248\u7F16\u8F91\uFF08OnlyOffice\uFF09" })] })) : null] }), _jsx(Form, { form: form, layout: "vertical", size: "middle", children: (meta?.lines || []).map((line) => (_jsx(Form.Item, { name: `p_${line.index}`, label: line.kind === 'title' ? '标题' : line.kind === 'caption' ? line.label : line.label, children: line.kind === 'caption' || (line.value && line.value.length > 40) ? (_jsx(Input.TextArea, { rows: 2 })) : (_jsx(Input, {})) }, line.index))) }), _jsxs(Space, { children: [_jsx(Button, { type: "primary", loading: saving, onClick: () => void handleSave(), children: "\u4FDD\u5B58" }), onCancel ? (_jsx(Button, { disabled: saving, onClick: onCancel, children: "\u53D6\u6D88" })) : null] })] }));
}
