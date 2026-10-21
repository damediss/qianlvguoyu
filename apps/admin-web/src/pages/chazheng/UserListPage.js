import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DownloadOutlined, ReloadOutlined, SafetyCertificateOutlined, SearchOutlined, CheckCircleOutlined, } from "@ant-design/icons";
import { Button, Input, Select, Space, Table, Tag, Typography, message, Modal, } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, useCallback } from "react";
import { pageUsers, getYmdList, certCheck, exportUsers, getEnterprises, } from "../../api/chazheng";
const GENDER_MAP = { 0: "男", 1: "女" };
const GENDER_COLOR = { 0: "blue", 1: "pink" };
export function UserListPage() {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: "", ymd: "", enterpriseId: undefined });
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);
    const listQuery = useQuery({
        queryKey: ["chazheng-users", query],
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
            queryClient.invalidateQueries({ queryKey: ["chazheng-users"] });
        },
        onError: (err) => {
            message.error(err.message || "查证失败");
        },
    });
    const handleExport = useCallback(async () => {
        try {
            const response = await exportUsers(query.ymd || undefined);
            const blob = new Blob([response.data], {
                type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "查证用户_" + (query.ymd || "全部") + ".xlsx";
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            message.success("导出成功");
        }
        catch (err) {
            message.error(err?.message || "导出失败");
        }
    }, [query.ymd]);
    const handleExportSelected = useCallback(async () => {
        if (selectedRowKeys.length === 0) {
            message.warning("请先选择要导出的用户");
            return;
        }
        try {
            const ids = selectedRowKeys.map(Number);
            const response = await exportUsers(undefined, ids);
            const blob = new Blob([response.data], {
                type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "查证用户_已选.xlsx";
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            message.success("导出成功");
            setSelectedRowKeys([]);
        }
        catch (err) {
            message.error(err?.message || "导出失败");
        }
    }, [selectedRowKeys]);
    const handleCert = useCallback((record) => {
        Modal.confirm({
            title: "确认查证「" + record.userName + "」？",
            content: "将调用外部接口查验证书信息，可能耗时数秒。",
            okText: "确认查证",
            cancelText: "取消",
            onOk: () => certMutation.mutate(record.id),
        });
    }, [certMutation]);
    const items = listQuery.data?.items || [];
    const total = listQuery.data?.total || 0;
    const columns = [
        {
            title: "姓名",
            dataIndex: "userName",
            render: (text) => _jsx(Typography.Text, { strong: true, children: text }),
        },
        { title: "身份证号", dataIndex: "idCard", width: 180 },
        { title: "企业ID", dataIndex: "enterpriseId", width: 90 },
        {
            title: "性别",
            dataIndex: "gender",
            width: 70,
            render: (v) => {
                if (v == null)
                    return "-";
                return _jsx(Tag, { color: GENDER_COLOR[v] || "default", children: GENDER_MAP[v] || v });
            },
        },
        {
            title: "证书状态",
            dataIndex: "isCert",
            width: 90,
            render: (v) => v === 1 ? _jsx(Tag, { color: "success", children: "\u6709\u8BC1\u4E66" }) : _jsx(Tag, { children: "\u65E0\u8BC1\u4E66" }),
        },
        {
            title: "查证状态",
            dataIndex: "isChecked",
            width: 90,
            render: (v) => v === 1 ? _jsx(Tag, { color: "processing", children: "\u5DF2\u67E5\u8BC1" }) : _jsx(Tag, { children: "\u672A\u67E5\u8BC1" }),
        },
        { title: "批次", dataIndex: "ymd", width: 150 },
        {
            title: "操作",
            width: 120,
            render: (_, record) => (_jsx(Button, { type: "link", size: "small", icon: _jsx(CheckCircleOutlined, {}), loading: certMutation.isPending, onClick: () => handleCert(record), children: "\u67E5\u8BC1" })),
        },
    ];
    return (_jsxs("div", { className: "knowledge-page", children: [_jsxs("section", { className: "page-heading knowledge-heading", children: [_jsxs("div", { children: [_jsxs("div", { className: "page-title-line", children: [_jsx("span", { className: "page-title-icon", children: _jsx(SafetyCertificateOutlined, {}) }), _jsx(Typography.Title, { level: 1, children: "\u7528\u6237\u5217\u8868" })] }), _jsx(Typography.Text, { children: "\u67E5\u8BE2\u548C\u7BA1\u7406\u7528\u6237\u4FE1\u606F\uFF0C\u652F\u6301\u6309\u59D3\u540D\u3001\u8EAB\u4EFD\u8BC1\u53F7\u3001\u4F01\u4E1A\u7B5B\u9009\u3002" })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    queryClient.invalidateQueries({ queryKey: ["chazheng-users"] });
                                }, children: "\u5237\u65B0" }), _jsx(Button, { icon: _jsx(DownloadOutlined, {}), onClick: handleExport, children: "\u5BFC\u51FA" }), _jsxs(Button, { icon: _jsx(DownloadOutlined, {}), disabled: selectedRowKeys.length === 0, onClick: handleExportSelected, children: ["\u5BFC\u51FA\u9009\u4E2D (", selectedRowKeys.length, ")"] })] })] }), _jsxs("div", { style: { padding: "0 24px" }, children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u59D3\u540D\u3001\u8EAB\u4EFD\u8BC1\u53F7", allowClear: true, style: { width: 260 }, value: query.keyword, onChange: (e) => setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 })), onPressEnter: () => queryClient.invalidateQueries({ queryKey: ["chazheng-users"] }) }), _jsx(Select, { placeholder: "\u9009\u62E9\u4F01\u4E1A", allowClear: true, style: { width: 150 }, value: query.enterpriseId, onChange: (value) => setQuery((prev) => ({ ...prev, enterpriseId: value, page: 1 })), options: (enterpriseQuery.data || []).map((id) => ({
                                    label: "企业 " + id,
                                    value: id,
                                })) }), _jsx(Select, { placeholder: "\u9009\u62E9\u6279\u6B21", allowClear: true, style: { width: 180 }, value: query.ymd || undefined, onChange: (value) => setQuery((prev) => ({ ...prev, ymd: value || "", page: 1 })), options: (ymdQuery.data || []).map((ymd) => ({
                                    label: ymd,
                                    value: ymd,
                                })) }), _jsx(Button, { icon: _jsx(SearchOutlined, {}), onClick: () => queryClient.invalidateQueries({ queryKey: ["chazheng-users"] }), children: "\u641C\u7D22" })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: listQuery.isLoading, dataSource: items, columns: columns, rowSelection: {
                            selectedRowKeys,
                            onChange: (keys) => setSelectedRowKeys(keys),
                        }, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total,
                            showSizeChanger: true,
                            showTotal: (t) => "共 " + t + " 条",
                            onChange: (page, pageSize) => setQuery((prev) => ({ ...prev, page, pageSize })),
                        } })] })] }));
}
