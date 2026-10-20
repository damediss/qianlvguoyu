import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Space, Typography } from 'antd';
import { CloudDownloadOutlined, ExportOutlined } from '@ant-design/icons';
export function ExportDockBar({ total, selectedCount, exporting, lastPackagePath, onExport, onDownloadLast, }) {
    return (_jsx("div", { className: "subsidy-export-dock", children: _jsxs(Space, { wrap: true, children: [_jsxs(Typography.Text, { children: ["\u5BFC\u51FA\u5DF2\u9009 ", _jsx("b", { children: selectedCount }), " / ", total, " \u9879"] }), _jsx(Button, { type: "primary", icon: _jsx(ExportOutlined, {}), loading: exporting, disabled: selectedCount === 0, onClick: onExport, children: "\u6309\u52FE\u9009\u5BFC\u51FA ZIP" }), lastPackagePath && onDownloadLast ? (_jsx(Button, { icon: _jsx(CloudDownloadOutlined, {}), onClick: onDownloadLast, children: "\u4E0B\u8F7D\u4E0A\u6B21\u6750\u6599\u5305" })) : null] }) }));
}
