import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Form, Input, Modal, Select } from 'antd';
export function ExceptionReleaseModal({ open, loading, onCancel, onSubmit }) {
    const [form] = Form.useForm();
    return (_jsx(Modal, { title: "\u514D\u8003\u653E\u884C", open: open, onCancel: onCancel, footer: [
            _jsx(Button, { onClick: onCancel, children: "\u53D6\u6D88" }, "cancel"),
            _jsx(Button, { type: "primary", loading: loading, onClick: () => {
                    form.validateFields().then((values) => {
                        Modal.confirm({
                            title: '确认免考放行？',
                            content: '免考放行后，该考生采集状态将变为已放行，并写入审计记录。',
                            okText: '确认放行',
                            cancelText: '取消',
                            onOk: () => onSubmit(values),
                        });
                    });
                }, children: "\u514D\u8003\u653E\u884C" }, "submit"),
        ], destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", initialValues: { exceptionType: 'OTHER' }, children: [_jsx(Form.Item, { name: "exceptionType", label: "\u653E\u884C\u7C7B\u578B", rules: [{ required: true }], children: _jsx(Select, { options: [
                            { value: 'DEVICE_ERROR', label: '设备异常' },
                            { value: 'FINGERPRINT_UNAVAILABLE', label: '无法采集指纹' },
                            { value: 'SIGNATURE_UNAVAILABLE', label: '无法签名' },
                            { value: 'INFO_ERROR', label: '信息异常' },
                            { value: 'OTHER', label: '其他' },
                        ] }) }), _jsx(Form.Item, { name: "reason", label: "\u653E\u884C\u539F\u56E0", rules: [{ required: true, message: '请填写免考放行原因' }], children: _jsx(Input.TextArea, { rows: 4, maxLength: 1000, showCount: true }) })] }) }));
}
