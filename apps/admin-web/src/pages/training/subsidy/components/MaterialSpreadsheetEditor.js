import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Space, Spin, Typography, message } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import { fetchPlatformFileBlob, fetchUrlFileBlob, resolveFilePreviewKind, } from '../../../../api/files';
function cellAddrFromTdId(id, tableId) {
    const prefix = `${tableId}-`;
    if (!id.startsWith(prefix))
        return null;
    const addr = id.slice(prefix.length);
    return /^[A-Z]+\d+$/i.test(addr) ? addr.toUpperCase() : null;
}
function parseEditedText(raw) {
    return (raw || '').replace(/\u00a0/g, ' ').trimEnd();
}
function escapeHtml(value) {
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
function cellToText(value) {
    if (value == null)
        return '';
    if (value instanceof Date) {
        return `${value.getFullYear()}/${value.getMonth() + 1}/${value.getDate()}`;
    }
    if (typeof value === 'object') {
        const obj = value;
        if (typeof obj.text === 'string')
            return obj.text;
        if (obj.result != null)
            return cellToText(obj.result);
        return '';
    }
    return String(value);
}
/**
 * 表格预览：把工作表渲染成 HTML table（替代 SheetJS 的 sheet_to_html）。
 * 单元格文本一律转义 —— 上传的表格内容属不可信输入，直接拼进 innerHTML 会是存储型 XSS。
 */
function worksheetToHtmlTable(sheet, tableId) {
    const rows = [];
    sheet.eachRow({ includeEmpty: false }, (row, rowNumber) => {
        const cells = [];
        for (let c = 1; c <= row.cellCount; c += 1) {
            const addr = `${row.getCell(c).address.replace(/\d+/g, '')}${rowNumber}`;
            const text = escapeHtml(cellToText(row.getCell(c).value));
            cells.push(`<td id="${tableId}-${addr}">${text}</td>`);
        }
        rows.push(`<tr>${cells.join('')}</tr>`);
    });
    return `<table id="${tableId}"><tbody>${rows.join('')}</tbody></table>`;
}
/**
 * 补贴材料 xlsx 内嵌编辑。
 * 展示用 SheetJS HTML；保存只提交单元格文本，由后端 POI 写回以保留边框。
 */
export function MaterialSpreadsheetEditor({ fileId, filePath, fileName, onSaveCells, saveLoading, editable = true, }) {
    const hostRef = useRef(null);
    const bookRef = useRef(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [dirty, setDirty] = useState(false);
    const [kindOk, setKindOk] = useState(false);
    useEffect(() => {
        if (!fileId && !filePath) {
            setError('暂无文件');
            setKindOk(false);
            return;
        }
        let cancelled = false;
        bookRef.current = null;
        setDirty(false);
        async function load() {
            setLoading(true);
            setError(null);
            setKindOk(false);
            if (hostRef.current)
                hostRef.current.innerHTML = '';
            try {
                const blob = filePath
                    ? await fetchUrlFileBlob(filePath)
                    : await fetchPlatformFileBlob(String(fileId));
                if (cancelled)
                    return;
                const kind = resolveFilePreviewKind(fileName || '', undefined, blob.type);
                if (kind !== 'spreadsheet') {
                    setError('当前不是 Excel 表格，请用 OnlyOffice 编辑或下载后修改');
                    return;
                }
                const buffer = await blob.arrayBuffer();
                const ExcelJSRuntime = (await import('exceljs')).default;
                const workbook = new ExcelJSRuntime.Workbook();
                await workbook.xlsx.load(buffer);
                const sheet = workbook.worksheets[0];
                if (!sheet) {
                    setError('表格为空');
                    return;
                }
                if (cancelled || !hostRef.current)
                    return;
                const tableId = `material-edit-table-${String(fileId || 'x').replace(/\W/g, '')}`;
                hostRef.current.innerHTML = worksheetToHtmlTable(sheet, tableId);
                const table = hostRef.current.querySelector('table');
                if (table) {
                    table.classList.add('material-edit-spreadsheet-table');
                    table.querySelectorAll('td').forEach((td) => {
                        const el = td;
                        if (editable) {
                            el.contentEditable = 'true';
                            el.spellcheck = false;
                            el.addEventListener('input', () => setDirty(true));
                        }
                    });
                }
                const sheetCount = workbook.worksheets.length;
                if (sheetCount > 1) {
                    const hint = document.createElement('p');
                    hint.className = 'material-edit-sheet-hint';
                    hint.textContent = `共 ${sheetCount} 个工作表，当前编辑：${sheet.name}`;
                    hostRef.current.appendChild(hint);
                }
                bookRef.current = { sheetName: sheet.name, tableId };
                setKindOk(true);
            }
            catch (e) {
                if (!cancelled)
                    setError(e.message || '加载表格失败');
            }
            finally {
                if (!cancelled)
                    setLoading(false);
            }
        }
        void load();
        return () => {
            cancelled = true;
        };
    }, [fileId, filePath, fileName, editable]);
    const handleSave = async () => {
        const handle = bookRef.current;
        const host = hostRef.current;
        if (!handle || !host) {
            message.warning('表格尚未加载完成');
            return;
        }
        const cells = {};
        host.querySelectorAll(`td[id^="${handle.tableId}-"]`).forEach((node) => {
            const td = node;
            const addr = cellAddrFromTdId(td.id, handle.tableId);
            if (!addr)
                return;
            cells[addr] = parseEditedText(td.innerText || '');
        });
        if (Object.keys(cells).length === 0) {
            message.warning('没有可保存的单元格');
            return;
        }
        try {
            await onSaveCells(cells);
            setDirty(false);
        }
        catch (e) {
            message.error(e.message || '保存失败');
            throw e;
        }
    };
    if (error && !kindOk) {
        return _jsx(Alert, { type: "warning", showIcon: true, message: "\u65E0\u6CD5\u5728\u7EBF\u7F16\u8F91\u8868\u683C", description: error });
    }
    return (_jsxs("div", { className: "material-spreadsheet-editor", children: [editable ? (_jsx("div", { className: "material-spreadsheet-editor-toolbar", children: _jsxs(Space, { wrap: true, children: [_jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), loading: saveLoading, disabled: !dirty && !saveLoading, onClick: () => void handleSave(), children: "\u4FDD\u5B58\u4FEE\u6539" }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: dirty
                                ? '有未保存的修改'
                                : '点击单元格编辑；保存时按模板底稿写回，保留边框格式' })] }) })) : null, _jsx(Spin, { spinning: loading, tip: "\u52A0\u8F7D\u8868\u683C\u2026", children: _jsx("div", { ref: hostRef, className: "material-file-preview-body material-spreadsheet-edit-body" }) })] }));
}
