import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { InboxOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Modal, Space, Typography, Upload, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { getAssessorLedgerImport, startAssessorLedgerImport } from '../../../api/certificationInfo';
import { uploadPlatformFile } from '../../../api/files';
import { usePaneActive } from '../../../layouts/TabKeepAlive';
export function AssessorLedgerImportButton() {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    const [open, setOpen] = useState(false);
    const [batchId, setBatchId] = useState();
    const notifiedStatus = useRef();
    const batchQuery = useQuery({
        queryKey: ['assessor-ledger-import', batchId],
        queryFn: () => getAssessorLedgerImport(batchId),
        enabled: Boolean(batchId),
        refetchInterval: (query) => paneActive && ['queued', 'processing'].includes(query.state.data?.status || '') ? 1500 : false,
    });
    useEffect(() => {
        const status = batchQuery.data?.status;
        if (!status || notifiedStatus.current === status || !['completed', 'failed'].includes(status))
            return;
        notifiedStatus.current = status;
        if (status === 'completed') {
            message.success('考评员台账导入完成');
            queryClient.invalidateQueries({ queryKey: ['certification-info-assessors'] });
        }
        else {
            message.error('考评员台账导入失败');
        }
    }, [batchQuery.data?.status, queryClient]);
    return (_jsxs(_Fragment, { children: [_jsx(Button, { icon: _jsx(InboxOutlined, {}), onClick: () => setOpen(true), children: "\u5BFC\u5165\u53F0\u8D26" }), _jsx(Modal, { title: "\u5BFC\u5165\u8003\u8BC4\u5458\u53F0\u8D26", open: open, onCancel: () => setOpen(false), footer: null, destroyOnClose: true, children: _jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(Typography.Text, { type: "secondary", children: "\u4EC5\u652F\u6301 .xlsx\u3002\u59D3\u540D\u6216\u8BC1\u53F7\u5DF2\u5B58\u5728\u7684\u8BB0\u5F55\u4F1A\u76F4\u63A5\u8DF3\u8FC7\uFF1B\u8D26\u53F7\u4E3A\u59D3\u540D\u62FC\u97F3\u9996\u5B57\u6BCD\u52A0\u8EAB\u4EFD\u8BC1\u540E\u4E94\u4F4D\uFF0C\u521D\u59CB\u5BC6\u7801\u4E3A 12345678\u3002" }), _jsxs(Upload.Dragger, { accept: ".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", maxCount: 1, disabled: Boolean(batchId && ['queued', 'processing'].includes(batchQuery.data?.status || '')), beforeUpload: async (file) => {
                                if (!file.name.toLowerCase().endsWith('.xlsx')) {
                                    message.warning('请选择 .xlsx 文件');
                                    return Upload.LIST_IGNORE;
                                }
                                try {
                                    const uploaded = await uploadPlatformFile(file, 'private', 15 * 60 * 1000);
                                    const batch = await startAssessorLedgerImport(String(uploaded.id));
                                    notifiedStatus.current = undefined;
                                    setBatchId(batch.id);
                                    message.info('台账已上传，正在后台导入');
                                }
                                catch {
                                    // apiClient has already shown the business error.
                                }
                                return Upload.LIST_IGNORE;
                            }, children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(InboxOutlined, {}) }), _jsx("p", { className: "ant-upload-text", children: "\u70B9\u51FB\u6216\u62D6\u62FD\u4E0A\u4F20\u8003\u8BC4\u5458\u53F0\u8D26" })] }), batchQuery.data ? (_jsx(Alert, { type: batchQuery.data.status === 'failed' ? 'error' : batchQuery.data.status === 'completed' ? 'success' : 'info', message: `状态：${labelOf(batchQuery.data.status)}`, description: batchQuery.data.resultMessage || '正在读取台账…', showIcon: true })) : null] }) })] }));
}
function labelOf(status) {
    return { queued: '排队中', processing: '导入中', completed: '已完成', failed: '失败' }[status] || status;
}
