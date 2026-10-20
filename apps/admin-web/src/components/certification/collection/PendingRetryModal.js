import { jsx as _jsx } from "react/jsx-runtime";
import { Button, Form, Input, Modal } from 'antd';
export function PendingRetryModal({ open, loading, onCancel, onSubmit }) {
    const [form] = Form.useForm();
    return (_jsx(Modal, { title: "\u6807\u8BB0\u5F85\u8865\u91C7", open: open, onCancel: onCancel, footer: [
            _jsx(Button, { onClick: onCancel, children: "\u53D6\u6D88" }, "cancel"),
            _jsx(Button, { type: "primary", loading: loading, onClick: () => form.validateFields().then((values) => onSubmit(values.reason)), children: "\u786E\u8BA4\u6807\u8BB0" }, "submit"),
        ], destroyOnHidden: true, children: _jsx(Form, { form: form, layout: "vertical", children: _jsx(Form.Item, { name: "reason", label: "\u5F85\u8865\u91C7\u539F\u56E0", rules: [{ required: true, message: '请填写待补采原因' }], children: _jsx(Input.TextArea, { rows: 4, maxLength: 500, showCount: true }) }) }) }));
}
