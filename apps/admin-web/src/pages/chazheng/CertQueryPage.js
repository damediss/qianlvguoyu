import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CloudUploadOutlined, DownloadOutlined, ReloadOutlined, SearchOutlined, IdcardOutlined, DashboardOutlined, CheckCircleOutlined, RedoOutlined, } from "@ant-design/icons";
import { Button, Input, Select, Space, Table, Tag, Tooltip, Typography, Descriptions, Upload, Modal, message, Progress, } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, useCallback, useRef, useEffect } from "react";
import { pageUsers, getYmdList, importUsers, triggerCertCheckByYmd, certCheck, exportUsers, getEnterprises, downloadTemplate, getCertProgress, getCertBatchProgress, } from "../../api/chazheng";
import { usePaneActive, usePaneActiveRef } from "../../layouts/TabKeepAlive";
export function CertQueryPage() {
    const queryClient = useQueryClient();
    const paneActiveRef = usePaneActiveRef(usePaneActive()); // 标签不可见时暂停 2s 查证轮询（§3.6）
    const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: "", ymd: "", enterpriseId: undefined });
    const [importing, setImporting] = useState(false);
    const [exportOpen, setExportOpen] = useState(false);
    const [exportYmd, setExportYmd] = useState(undefined);
    const [progressOpen, setProgressOpen] = useState(false);
    const [progressData, setProgressData] = useState({ total: 0, checked: 0 });
    const pollRef = useRef(null);
    // 「重新查证」：本批次进度弹窗（按返回的查证 batchId 轮询）
    const [retryStarting, setRetryStarting] = useState(false);
    const [batchProgressOpen, setBatchProgressOpen] = useState(false);
    const [batchProgress, setBatchProgress] = useState(null);
    const [batchProgressYmd, setBatchProgressYmd] = useState("");
    const batchPollRef = useRef(null);
    const listQuery = useQuery({
        queryKey: ["chazheng-certs", query],
        queryFn: () => pageUsers(query),
    });
    const ymdQuery = useQuery({
        queryKey: ["chazheng-ymd-list"],
        queryFn: getYmdList,
    });
    const enterpriseQuery = useQuery({
        queryKey: ["chazheng-enterprises"],
        queryFn: getEnterprises,
    });
    const certMutation = useMutation({
        mutationFn: certCheck,
        onSuccess: (data) => {
            message.success("查证完成：" + data.userName);
            queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
        },
        onError: (err) => {
            message.error(err.message || "查证失败");
        },
    });
    const openProgress = useCallback(() => {
        setProgressOpen(true);
        setProgressData({ total: 0, checked: 0 });
        const poll = async () => {
            if (!paneActiveRef.current)
                return; // 标签不可见：跳过本轮请求，服务端查证照常进行
            try {
                const data = await getCertProgress();
                setProgressData(data);
                if (data.total > 0 && data.checked >= data.total) {
                    if (pollRef.current)
                        clearInterval(pollRef.current);
                    pollRef.current = null;
                    message.success("查证全部完成");
                    queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
                }
            }
            catch {
                // ignore poll errors
            }
        };
        poll();
        pollRef.current = setInterval(poll, 2000);
    }, [queryClient]);
    const handleImport = useCallback(async (file) => {
        setImporting(true);
        try {
            const result = await importUsers(file);
            message.success("导入成功：" + result.imported + " 条记录，批次 " + result.ymd);
            queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
            queryClient.invalidateQueries({ queryKey: ["chazheng-ymd-list"] });
            try {
                const batch = await triggerCertCheckByYmd(result.ymd, file.name);
                message.success("批量查证已启动：" + batch.total + " 条");
                openProgress();
            }
            catch (err) {
                message.error(err?.message || "批量查证启动失败");
            }
        }
        catch (err) {
            message.error(err?.message || "导入失败");
        }
        finally {
            setImporting(false);
        }
        return false;
    }, [queryClient, openProgress]);
    const handleDownloadTemplate = useCallback(async () => {
        try {
            const response = await downloadTemplate();
            const blob = new Blob([response.data], {
                type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "import_user_example.xlsx";
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        }
        catch (err) {
            message.error("下载模板失败");
        }
    }, []);
    const closeProgress = useCallback(() => {
        setProgressOpen(false);
        if (pollRef.current) {
            clearInterval(pollRef.current);
            pollRef.current = null;
        }
    }, []);
    useEffect(() => {
        return () => {
            if (pollRef.current)
                clearInterval(pollRef.current);
            if (batchPollRef.current)
                clearInterval(batchPollRef.current);
        };
    }, []);
    const handleExport = useCallback(async () => {
        if (!exportYmd) {
            message.warning("请先选择要导出的批次");
            return;
        }
        try {
            const response = await exportUsers(exportYmd);
            const blob = new Blob([response.data], {
                type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "查证用户_" + exportYmd + ".xlsx";
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            message.success("导出成功");
            setExportOpen(false);
        }
        catch (err) {
            message.error(err?.message || "导出失败");
        }
    }, [exportYmd]);
    const handleCert = useCallback((record) => {
        Modal.confirm({
            title: "确认查证「" + record.userName + "」？",
            content: "将调用外部接口查验证书信息，可能耗时数秒。",
            okText: "确认查证",
            cancelText: "取消",
            onOk: () => certMutation.mutate(record.id),
        });
    }, [certMutation]);
    /** 本批次查证进度轮询：终态（completed/aborted）停止并刷新列表 */
    const startBatchPoll = useCallback((batchId, ymd) => {
        if (batchPollRef.current)
            clearInterval(batchPollRef.current);
        setBatchProgressYmd(ymd);
        setBatchProgress(null);
        setBatchProgressOpen(true);
        const poll = async () => {
            if (!paneActiveRef.current)
                return; // 标签不可见：跳过本轮请求，服务端查证照常进行
            try {
                const data = await getCertBatchProgress(batchId);
                setBatchProgress(data);
                if (data.status !== "processing") {
                    if (batchPollRef.current)
                        clearInterval(batchPollRef.current);
                    batchPollRef.current = null;
                    queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
                    queryClient.invalidateQueries({ queryKey: ["chazheng-ymd-list"] });
                    if (data.failed > 0) {
                        message.warning(`重新查证结束：成功 ${data.success + data.successNoCert} 人，失败 ${data.failed} 人`);
                    }
                    else {
                        message.success(`重新查证完成：成功 ${data.success + data.successNoCert} 人`);
                    }
                }
            }
            catch {
                // 轮询失败不打断，下一轮继续
            }
        };
        poll();
        batchPollRef.current = setInterval(poll, 2000);
    }, [queryClient]);
    const closeBatchProgress = useCallback(() => {
        setBatchProgressOpen(false);
        if (batchPollRef.current) {
            clearInterval(batchPollRef.current);
            batchPollRef.current = null;
        }
    }, []);
    /** 重新查证：仅重查所选批次中未成功（未查证/查证失败）的人员，确认后按本批进度轮询 */
    const handleRetryBatch = useCallback(async () => {
        const ymd = query.ymd;
        if (!ymd) {
            message.warning("请先选择批次");
            return;
        }
        setRetryStarting(true);
        try {
            const pending = await pageUsers({ ymd, isChecked: 0, page: 1, pageSize: 1 });
            const count = pending.total;
            if (!count) {
                message.info("该批次没有未成功的人员，无需重新查证");
                return;
            }
            Modal.confirm({
                title: "确认重新查证？",
                content: `批次 ${ymd}，共 ${count} 人未查证成功，将重新调用外部接口查证并覆盖其原有查证结果。`,
                okText: "开始重查",
                cancelText: "取消",
                onOk: async () => {
                    try {
                        const batch = await triggerCertCheckByYmd(ymd, undefined, true);
                        message.success("重新查证已启动：" + batch.total + " 条");
                        startBatchPoll(batch.batchId, ymd);
                    }
                    catch (err) {
                        message.error(err?.message || "重新查证启动失败");
                        throw err; // 保持弹窗，便于直接重试
                    }
                },
            });
        }
        catch (err) {
            message.error(err?.message || "重查人数统计失败");
        }
        finally {
            setRetryStarting(false);
        }
    }, [query.ymd, startBatchPoll]);
    const renderCertContent = (certs) => {
        if (!certs || certs.length === 0)
            return _jsx(Typography.Text, { type: "secondary", children: "\u65E0\u8BC1\u4E66" });
        return (_jsx(Descriptions, { size: "small", column: 1, style: { maxWidth: 400 }, children: certs.map((cert, idx) => (_jsxs(Descriptions.Item, { label: "证书 " + (idx + 1), children: ["\u7F16\u53F7: ", cert.certNo || "-", _jsx("br", {}), "\u540D\u79F0: ", cert.certName || "-", _jsx("br", {}), "\u53D1\u8BC1\u65E5\u671F: ", cert.certDate || "-"] }, idx))) }));
    };
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const pct = progressData.total > 0 ? Math.round((progressData.checked / progressData.total) * 100) : 0;
    const columns = [
        {
            title: "姓名",
            dataIndex: "userName",
            width: 100,
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        { title: "身份证号", dataIndex: "idCard", width: 180 },
        { title: "企业ID", dataIndex: "enterpriseId", width: 90 },
        {
            title: "证书数量",
            dataIndex: "certs",
            width: 100,
            render: (certs) => {
                const count = certs?.length || 0;
                return count > 0 ? _jsxs(Tag, { color: "blue", children: [count, " \u4E2A"] }) : _jsx(Tag, { children: "\u65E0" });
            },
        },
        {
            title: "证书编号",
            dataIndex: "certs",
            width: 160,
            render: (certs) => {
                if (!certs || certs.length === 0)
                    return "-";
                return (_jsx(Space, { size: 2, wrap: true, children: certs.map((c, i) => (_jsx(Tag, { color: "geekblue", children: c.certNo || "-" }, i))) }));
            },
        },
        {
            title: "证书名称",
            dataIndex: "certs",
            width: 200,
            render: (certs) => {
                if (!certs || certs.length === 0)
                    return "-";
                return (_jsx(Space, { size: 2, wrap: true, children: certs.map((c, i) => (_jsx(Tag, { color: "purple", children: c.certName || "-" }, i))) }));
            },
        },
        {
            title: "发证日期",
            dataIndex: "certs",
            width: 120,
            render: (certs) => {
                if (!certs || certs.length === 0)
                    return "-";
                return (_jsx(Space, { size: 2, direction: "vertical", children: certs.map((c, i) => (_jsx(Typography.Text, { children: c.certDate || "-" }, i))) }));
            },
        },
        {
            title: "查证状态",
            dataIndex: "isChecked",
            width: 100,
            render: (v) => v === 1 ? _jsx(Tag, { color: "processing", children: "\u5DF2\u67E5\u8BC1" }) : _jsx(Tag, { children: "\u672A\u67E5\u8BC1" }),
        },
        { title: "批次", dataIndex: "ymd", width: 150 },
        {
            title: "操作",
            width: 120,
            render: (_, record) => (_jsx(Button, { type: "link", size: "small", icon: _jsx(CheckCircleOutlined, {}), loading: certMutation.isPending, onClick: () => handleCert(record), children: "\u67E5\u8BC1" })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(IdcardOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u8BC1\u4E66\u67E5\u8BE2" })] }), _jsx(Typography.Text, { children: "\u5BFC\u5165\u67E5\u8BC1\u7528\u6237\uFF0C\u67E5\u770B\u548C\u7BA1\u7406\u8BC1\u4E66\u4FE1\u606F\uFF0C\u652F\u6301\u6279\u91CF\u67E5\u8BC1\u548C\u5BFC\u51FA\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
                                }, children: "\u5237\u65B0" }), _jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: handleImport, disabled: importing, children: _jsx(Button, { icon: _jsx(CloudUploadOutlined, {}), loading: importing, children: "\u5BFC\u5165 Excel" }) }), _jsx(Button, { type: "link", size: "small", onClick: handleDownloadTemplate, children: "\u4E0B\u8F7D\u6A21\u677F" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: () => setExportOpen(true), children: "\u5BFC\u51FA" }), _jsx(Button, { icon: _jsx(DashboardOutlined, {}), onClick: openProgress, children: "\u67E5\u8BC1\u8FDB\u5EA6" })] })] }), _jsxs("div", { style: { padding: "0 24px" }, children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u59D3\u540D\u3001\u8EAB\u4EFD\u8BC1\u53F7", allowClear: true, style: { width: 260 }, value: query.keyword, onChange: (e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] }) }), _jsx(Select, { placeholder: "\u9009\u62E9\u4F01\u4E1A", allowClear: true, style: { width: 150 }, value: query.enterpriseId, onChange: (value) => setQuery((prev) => ({ ...prev, enterpriseId: value, page: 1 })), options: (enterpriseQuery.data || []).map((id) => ({
                                    label: "企业 " + id,
                                    value: id,
                                })) }), _jsx(Select, { placeholder: "\u9009\u62E9\u6279\u6B21", allowClear: true, style: { width: 180 }, value: query.ymd || undefined, onChange: (value) => setQuery((prev) => ({ ...prev, ymd: value || "", page: 1 })), options: (ymdQuery.data || []).map((ymd) => ({
                                    label: ymd,
                                    value: ymd,
                                })) }), _jsx(Tooltip, { title: query.ymd ? undefined : "请先选择批次", children: _jsx(Button, { icon: _jsx(RedoOutlined, {}), loading: retryStarting, disabled: !query.ymd, onClick: handleRetryBatch, children: "\u91CD\u65B0\u67E5\u8BC1" }) }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, expandable: {
                            expandedRowRender: (record) => renderCertContent(record.certs),
                            rowExpandable: (record) => record.certs && record.certs.length > 0,
                        }, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => "共 " + t + " 条",
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] }), _jsx(Modal, { title: "\u5BFC\u51FA\u8BC1\u4E66\u67E5\u8BE2", open: exportOpen, onOk: handleExport, onCancel: () => setExportOpen(false), okText: "\u5BFC\u51FA", cancelText: "\u53D6\u6D88", children: _jsx(Select, { placeholder: "\u9009\u62E9\u6279\u6B21", allowClear: true, style: { width: "100%" }, value: exportYmd, onChange: (value) => setExportYmd(value), options: (ymdQuery.data || []).map((ymd) => ({
                        label: ymd,
                        value: ymd,
                    })) }) }), _jsxs(Modal, { title: "\u67E5\u8BC1\u8FDB\u5EA6", open: progressOpen, onCancel: closeProgress, footer: null, children: [_jsx(Progress, { percent: pct, format: () => progressData.checked + " / " + progressData.total }), pct === 100 && _jsx(Typography.Text, { type: "success", children: "\u67E5\u8BC1\u5168\u90E8\u5B8C\u6210" })] }), _jsxs(Modal, { title: "重新查证进度" + (batchProgressYmd ? "（批次 " + batchProgressYmd + "）" : ""), open: batchProgressOpen, onCancel: closeBatchProgress, footer: null, children: [_jsx(Progress, { percent: batchProgress?.percent ?? 0, status: batchProgress && batchProgress.failed > 0 ? "exception" : undefined, format: () => (batchProgress?.completed ?? 0) + " / " + (batchProgress?.total ?? 0) }), batchProgress ? (_jsxs(Space, { direction: "vertical", size: 2, children: [_jsxs(Typography.Text, { children: ["\u67E5\u8BC1\u6210\u529F\uFF08\u6709\u8BC1\uFF09", batchProgress.success, " \u4EBA \u00B7 \u65E0\u8BC1\u4E66", " ", batchProgress.successNoCert, " \u4EBA \u00B7 \u5931\u8D25 ", batchProgress.failed, " \u4EBA"] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u5DF2\u7528\u65F6 ", batchProgress.elapsed, batchProgress.status === "processing"
                                        ? ` · 预计剩余 ${batchProgress.estimateRemaining}`
                                        : ""] }), batchProgress.status === "aborted" && (_jsx(Typography.Text, { type: "danger", children: "\u6279\u6B21\u56E0\u63A5\u53E3\u8FDE\u7EED\u6545\u969C\u5DF2\u4E2D\u6B62\uFF0C\u672A\u5B8C\u6210\u7684\u4EBA\u5458\u53EF\u518D\u6B21\u70B9\u51FB\u300C\u91CD\u65B0\u67E5\u8BC1\u300D\u7EED\u67E5\u3002" }))] })) : (_jsx(Typography.Text, { type: "secondary", children: "\u6B63\u5728\u83B7\u53D6\u8FDB\u5EA6\u2026" }))] })] }));
}
