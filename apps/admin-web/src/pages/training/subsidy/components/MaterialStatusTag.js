import { jsx as _jsx } from "react/jsx-runtime";
import { Tag } from 'antd';
const STATUS_MAP = {
    pending: { color: 'default', label: '未生成' },
    filled: { color: 'processing', label: '已填待审' },
    confirmed: { color: 'success', label: '已确认' },
    upload_only: { color: 'blue', label: '仅上传' },
    from_class: { color: 'cyan', label: '来自本班' },
};
export function MaterialStatusTag({ status }) {
    const cfg = STATUS_MAP[status || ''] || { color: 'default', label: status || '—' };
    return _jsx(Tag, { color: cfg.color, children: cfg.label });
}
