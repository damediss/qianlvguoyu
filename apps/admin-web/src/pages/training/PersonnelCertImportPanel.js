import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 人员认定 — 导入 Excel 查证并回填（真实查证 API，不落库）
 */
import { useMemo, useState } from 'react';
import { Alert, Button, Card, Descriptions, Progress, Space, Table, Tag, Typography, Upload, message, } from 'antd';
import { CloudDownloadOutlined, FileExcelOutlined, InboxOutlined, SafetyCertificateOutlined, } from '@ant-design/icons';
import * as trainApi from '../../api/training';
import { downloadFilledWorkbook, fillWorkbookWithResults, parsePersonnelCertExcel, summarizeCertResults, } from '../../utils/personnelCertExcel';
import './PersonnelCertImportPanel.css';
const { Dragger } = Upload;
const { Text, Paragraph } = Typography;
const BATCH_SIZE = 20;
const STATUS_TAG = {
    pending: { label: '待查证', color: 'default' },
    verified: { label: '已回填', color: 'success' },
    not_found: { label: '无结果', color: 'warning' },
    invalid: { label: '无效行', color: 'error' },
    failed: { label: '失败', color: 'error' },
    skipped: { label: '已跳过', color: 'default' },
};
function maskIdCard(idCard) {
    if (!idCard)
        return '-';
    const s = idCard.trim();
    if (s.length <= 8)
        return s;
    return `${s.slice(0, 4)}********${s.slice(-4)}`;
}
function toLookupRow(detail) {
    return {
        rowIndex: detail.rowIndex,
        name: detail.name ?? '',
        idCard: detail.idCard ?? '',
        status: detail.status || 'failed',
        certLevel: detail.certLevel,
        certNo: detail.certNo,
        issueDate: detail.issueDate,
        message: detail.message,
    };
}
export function PersonnelCertImportPanel() {
    const [session, setSession] = useState(null);
    const [results, setResults] = useState([]);
    const [filledWorkbook, setFilledWorkbook] = useState(null);
    const [parsing, setParsing] = useState(false);
    const [verifying, setVerifying] = useState(false);
    const [progress, setProgress] = useState(0);
    const [downloading, setDownloading] = useState(false);
    const summary = useMemo(() => summarizeCertResults(results), [results]);
    const uploadProps = {
        accept: '.xlsx,.xls',
        maxCount: 1,
        showUploadList: false,
        beforeUpload: (file) => {
            void handleImport(file);
            return false;
        },
    };
    const handleImport = async (file) => {
        setParsing(true);
        setResults([]);
        setFilledWorkbook(null);
        setProgress(0);
        try {
            const parsed = await parsePersonnelCertExcel(file);
            setSession(parsed);
            message.success(`已解析 ${parsed.rows.length} 条人员记录`);
        }
        catch (err) {
            setSession(null);
            message.error(err instanceof Error ? err.message : '解析 Excel 失败');
        }
        finally {
            setParsing(false);
        }
    };
    const handleVerifyAndFill = async () => {
        if (!session)
            return;
        setVerifying(true);
        setProgress(0);
        setFilledWorkbook(null);
        try {
            const allResults = [];
            for (let i = 0; i < session.rows.length; i += BATCH_SIZE) {
                const chunk = session.rows.slice(i, i + BATCH_SIZE);
                const resp = await trainApi.verifyPersonnelCertLookup(chunk.map((r) => ({
                    rowIndex: r.rowIndex,
                    name: r.name,
                    idCard: r.idCard,
                })));
                allResults.push(...resp.details.map(toLookupRow));
                setProgress(Math.min(100, Math.round(((i + chunk.length) / session.rows.length) * 100)));
                setResults([...allResults]);
            }
            const wb = await fillWorkbookWithResults(session, allResults);
            setFilledWorkbook(wb);
            const s = summarizeCertResults(allResults);
            message.success(`查证完成：成功回填 ${s.verified} 人，无结果 ${s.notFound} 人` +
                (s.invalid || s.failed ? `，无效/失败 ${s.invalid + s.failed} 人` : ''));
        }
        catch (err) {
            message.error(err instanceof Error ? err.message : '查证回填失败');
        }
        finally {
            setVerifying(false);
            setProgress(100);
        }
    };
    const handleDownload = async () => {
        if (!session || !filledWorkbook)
            return;
        setDownloading(true);
        try {
            await downloadFilledWorkbook(filledWorkbook, session.fileName);
            message.success('已开始下载回填后的 Excel');
        }
        catch (err) {
            message.error(err instanceof Error ? err.message : '下载失败');
        }
        finally {
            setDownloading(false);
        }
    };
    const columns = [
        { title: '行号', dataIndex: 'rowIndex', width: 64, align: 'center' },
        { title: '姓名', dataIndex: 'name', width: 90 },
        {
            title: '身份证',
            dataIndex: 'idCard',
            width: 160,
            render: (v) => _jsx("span", { className: "id-card-mask", children: maskIdCard(v) }),
        },
        {
            title: '查证状态',
            dataIndex: 'status',
            width: 90,
            render: (v) => {
                const meta = STATUS_TAG[v] ?? STATUS_TAG.pending;
                return _jsx(Tag, { color: meta.color, children: meta.label });
            },
        },
        { title: '已有证书等级', dataIndex: 'certLevel', width: 220, ellipsis: true, render: (v) => v || '-' },
        { title: '证书编号', dataIndex: 'certNo', width: 140, ellipsis: true, render: (v) => v || '-' },
        { title: '发证日期', dataIndex: 'issueDate', width: 110, render: (v) => v || '-' },
        { title: '说明', dataIndex: 'message', ellipsis: true, render: (v) => v || '-' },
    ];
    const columnHint = session
        ? [
            `姓名 → 第 ${session.columnMap.nameCol + 1} 列`,
            `身份证 → 第 ${session.columnMap.idCardCol + 1} 列`,
            session.columnMap.certLevelCol != null ? `证书等级 → 第 ${session.columnMap.certLevelCol + 1} 列` : '未识别证书等级列（将跳过回填）',
            session.columnMap.certNoCol != null ? `证书编号 → 第 ${session.columnMap.certNoCol + 1} 列` : '未识别证书编号列',
            session.columnMap.issueDateCol != null ? `发证日期 → 第 ${session.columnMap.issueDateCol + 1} 列` : '未识别发证日期列',
        ].join('；')
        : '';
    return (_jsxs("div", { className: "personnel-cert-import-panel", children: [_jsxs(Paragraph, { type: "secondary", children: ["\u4E0A\u4F20\u4EBA\u5458 Excel\uFF08\u63A8\u8350\u300C\u9AD8\u547D\u4E2D\u7387\u8BA4\u5B9A\u91C7\u96C6\u6A21\u677F\u7CBE\u7B80\u7248\u300D\uFF09\uFF0C\u7CFB\u7EDF\u81EA\u52A8\u8BC6\u522B", _jsx("strong", { children: "\u59D3\u540D" }), "\u4E0E", _jsx("strong", { children: "\u8EAB\u4EFD\u8BC1\u53F7\u7801" }), "\u5217\uFF0C \u8C03\u7528\u771F\u5B9E\u67E5\u8BC1\u63A5\u53E3\u540E\u5C06", _jsx("strong", { children: "\u5DF2\u6709\u8BC1\u4E66\u7B49\u7EA7\u3001\u8BC1\u4E66\u7F16\u53F7\u3001\u53D1\u8BC1\u65E5\u671F" }), "\u5199\u56DE\u539F\u6587\u4EF6\u5BF9\u5E94\u884C\u5E76\u4E0B\u8F7D\u3002 \u67E5\u8BC1\u7ED3\u679C", _jsx("strong", { children: "\u4EC5\u56DE\u586B\u6587\u4EF6\uFF0C\u4E0D\u81EA\u52A8\u540C\u6B65\u5230\u4F01\u4E1A\u8C03\u7814" }), "\uFF1B\u5982\u9700\u843D\u5E93\u8BF7\u5230\u4F01\u4E1A\u8C03\u7814 \u2192 \u4EBA\u5458\u91C7\u96C6\u4E2D\u7EF4\u62A4\u540E\u518D\u67E5\u8BC1\u3002"] }), _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u771F\u5B9E\u67E5\u8BC1\u8BF4\u660E", description: "\u9700\u5728\u7CFB\u7EDF\u5B57\u5178\u914D\u7F6E chazheng_url / chazheng_datas\u3002\u4EBA\u6570\u8F83\u591A\u65F6\u5C06\u81EA\u52A8\u5206\u7247\uFF08\u6BCF\u6279\u6700\u591A 20 \u4EBA\uFF09\u8C03\u7528\u67E5\u8BC1\u63A5\u53E3\uFF1B\u4EC5 status=\u5DF2\u56DE\u586B \u7684\u884C\u4F1A\u5199\u5165 Excel \u5BF9\u5E94\u5217\u3002" }), _jsxs(Card, { size: "small", title: "1. \u5BFC\u5165 Excel", style: { marginBottom: 16 }, children: [_jsxs(Dragger, { ...uploadProps, disabled: parsing || verifying, children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(InboxOutlined, {}) }), _jsx("p", { className: "ant-upload-text", children: "\u70B9\u51FB\u6216\u62D6\u62FD\u4E0A\u4F20 .xlsx / .xls" }), _jsx("p", { className: "ant-upload-hint", children: "\u652F\u6301\u300C\u91C7\u96C6\u8868\u300DSheet\uFF1B\u8868\u5934\u9700\u542B\u300C\u59D3\u540D\u300D\u300C\u8EAB\u4EFD\u8BC1\u53F7\u7801\u300D" })] }), session && (_jsxs(Descriptions, { size: "small", column: 1, style: { marginTop: 16 }, bordered: true, children: [_jsx(Descriptions.Item, { label: "\u6587\u4EF6\u540D", children: _jsxs(Space, { children: [_jsx(FileExcelOutlined, {}), session.fileName] }) }), _jsx(Descriptions.Item, { label: "\u5DE5\u4F5C\u8868", children: session.sheetName }), _jsxs(Descriptions.Item, { label: "\u8868\u5934\u884C", children: ["\u7B2C ", session.headerRowIndex + 1, " \u884C"] }), _jsxs(Descriptions.Item, { label: "\u4EBA\u5458\u884C\u6570", children: [session.rows.length, " \u4EBA"] }), _jsx(Descriptions.Item, { label: "\u5217\u6620\u5C04", children: columnHint })] }))] }), _jsxs(Card, { size: "small", title: "2. \u67E5\u8BC1\u5E76\u56DE\u586B", style: { marginBottom: 16 }, children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { type: "primary", icon: _jsx(SafetyCertificateOutlined, {}), disabled: !session || verifying, loading: verifying, onClick: () => void handleVerifyAndFill(), children: "\u5F00\u59CB\u67E5\u8BC1\u56DE\u586B" }), _jsx(Button, { icon: _jsx(CloudDownloadOutlined, {}), disabled: !filledWorkbook, loading: downloading, onClick: () => void handleDownload(), children: "\u4E0B\u8F7D\u56DE\u586B\u6587\u4EF6" }), session && (_jsx(Button, { disabled: verifying, onClick: () => {
                                    setSession(null);
                                    setResults([]);
                                    setFilledWorkbook(null);
                                    setProgress(0);
                                }, children: "\u6E05\u7A7A" }))] }), verifying && (_jsxs("div", { style: { marginTop: 16 }, children: [_jsx(Text, { type: "secondary", children: "\u67E5\u8BC1\u8FDB\u5EA6" }), _jsx(Progress, { percent: progress, status: "active" })] })), results.length > 0 && (_jsxs(Descriptions, { size: "small", column: 5, style: { marginTop: 16 }, bordered: true, children: [_jsx(Descriptions.Item, { label: "\u603B\u6570", children: summary.total }), _jsx(Descriptions.Item, { label: "\u6210\u529F\u56DE\u586B", children: _jsx(Text, { type: "success", children: summary.verified }) }), _jsx(Descriptions.Item, { label: "\u65E0\u7ED3\u679C", children: _jsx(Text, { type: "warning", children: summary.notFound }) }), _jsx(Descriptions.Item, { label: "\u65E0\u6548\u884C", children: _jsx(Text, { type: "danger", children: summary.invalid }) }), _jsx(Descriptions.Item, { label: "\u5931\u8D25", children: _jsx(Text, { type: "danger", children: summary.failed }) })] }))] }), results.length > 0 && (_jsx(Card, { size: "small", title: "3. \u67E5\u8BC1\u7ED3\u679C\u9884\u89C8", children: _jsx(Table, { rowKey: (r) => `${r.rowIndex}-${r.idCard}`, size: "small", bordered: true, columns: columns, dataSource: results, scroll: { x: 980, y: 420 }, pagination: { defaultPageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` } }) }))] }));
}
