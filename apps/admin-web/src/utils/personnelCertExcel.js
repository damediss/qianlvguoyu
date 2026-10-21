/** 人员认定 Excel 导入查证回填（前端 exceljs 解析/写回；查证走后端 verify-lookup） */
const NAME_ALIASES = ['姓名', 'name', '人员姓名'];
const ID_CARD_ALIASES = ['身份证号码', '身份证号', '身份证', 'id_card', 'idcard'];
const CERT_LEVEL_ALIASES = ['已有证书等级', '证书等级', '等级'];
const CERT_NO_ALIASES = ['证书编号', '编号', 'paper_no'];
const ISSUE_DATE_ALIASES = ['发证日期', '发证时间', 'cert_issue_dates'];
function normalizeHeader(value) {
    return String(value ?? '').trim().replace(/\s+/g, '');
}
function findColumnIndex(headers, aliases) {
    const normalized = headers.map(normalizeHeader);
    for (const alias of aliases) {
        const idx = normalized.findIndex((h) => h === alias || h.includes(alias));
        if (idx >= 0)
            return idx;
    }
    return undefined;
}
function isValidIdCard(idCard) {
    const s = idCard.trim();
    return /^(\d{15}|\d{17}[\dXx])$/.test(s);
}
/** 仅用于本地开发/单测；业务路径请调用 POST /training/personnel-cert/verify-lookup */
export function mockLookupCertificate(name, idCard) {
    const card = idCard.trim();
    if (!name.trim()) {
        return { status: 'invalid', message: '姓名为空' };
    }
    if (!isValidIdCard(card)) {
        return { status: 'invalid', message: '身份证格式无效' };
    }
    const last = card.slice(-1);
    if (last === '0') {
        return { status: 'not_found', message: '未查到证书信息（Mock）' };
    }
    const levelPool = ['五级/初级工', '四级/中级工', '三级/高级工', '二级/技师', '一级/高级技师'];
    const levelIdx = parseInt(last, 10) % levelPool.length;
    const year = 2018 + (parseInt(card.slice(-2), 10) % 7);
    const month = String((parseInt(card.slice(-3, -1), 10) % 12) + 1).padStart(2, '0');
    const day = String((parseInt(card.slice(-4, -2), 10) % 28) + 1).padStart(2, '0');
    return {
        status: 'verified',
        certLevel: levelPool[levelIdx],
        certNo: `MOCK${card.slice(-6)}${levelIdx + 1}`,
        issueDate: `${year}-${month}-${day}`,
        message: '查证成功（Mock 数据）',
    };
}
/** Excel 序列日 → yyyy/M/d（45376 → 2024/3/25） */
function excelSerialToDateText(serial) {
    const utcDays = Math.floor(serial) - 25569;
    const date = new Date(utcDays * 86400 * 1000);
    if (Number.isNaN(date.getTime()))
        return String(serial);
    return `${date.getUTCFullYear()}/${date.getUTCMonth() + 1}/${date.getUTCDate()}`;
}
function formatJsDate(value) {
    return `${value.getFullYear()}/${value.getMonth() + 1}/${value.getDate()}`;
}
/** 去掉数字格式里的字面量与颜色段，避免 "General"、"[Red]" 之类被误判为日期格式 */
function stripNumFmtLiterals(numFmt) {
    return numFmt.replace(/"[^"]*"/g, '').replace(/\[[^\]]*\]/g, '');
}
function isDateFormat(numFmt) {
    if (!numFmt)
        return false;
    return /[ymdhs]/i.test(stripNumFmtLiterals(numFmt));
}
/**
 * exceljs 再写出时常丢失日期数字格式，Excel 会把 45376 当普通数字显示。
 * 下载前把日期单元格转成可读文本，避免「2024/3/25 → 45376」。
 */
function normalizeSheetDatesToText(workbook) {
    for (const sheet of workbook.worksheets) {
        sheet.eachRow({ includeEmpty: false }, (row) => {
            row.eachCell({ includeEmpty: false }, (cell) => {
                const value = cell.value;
                if (value instanceof Date) {
                    const text = formatJsDate(value);
                    cell.value = text;
                    return;
                }
                if (typeof value !== 'number')
                    return;
                const formatted = typeof cell.text === 'string' ? cell.text.trim() : '';
                const looksFormattedDate = /^\d{4}[\/\-年]\d{1,2}[\/\-月]\d{1,2}/.test(formatted)
                    || /^\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4}/.test(formatted);
                // 典型业务日期序列区间（约 1950–2100），且带日期格式或已有日期形格式化文本
                const inDateSerialRange = value >= 18264 && value <= 73050;
                if (!isDateFormat(cell.numFmt) && !(looksFormattedDate && inDateSerialRange))
                    return;
                const text = formatted && looksFormattedDate
                    ? formatted.replace(/年|月/g, '/').replace(/日/g, '')
                    : excelSerialToDateText(value);
                cell.value = text;
            });
        });
    }
}
/** 查证回填的发证日期：若误传序列号则转文本 */
function normalizeIssueDateText(value) {
    if (value == null || value === '')
        return undefined;
    const trimmed = String(value).trim();
    if (/^\d+(\.\d+)?$/.test(trimmed)) {
        const serial = Number(trimmed);
        if (serial >= 18264 && serial <= 73050) {
            return excelSerialToDateText(serial);
        }
    }
    return trimmed;
}
/**
 * 清除单元格批注/备注。
 * 采集模板常见「Administrator: 2020年11月」这类历史批注；SheetJS 重写后会原样带出，
 * 在 Excel 中显示为带红三角的批注框（非查证逻辑写入）。
 */
function stripWorkbookComments(workbook) {
    for (const sheet of workbook.worksheets) {
        sheet.eachRow({ includeEmpty: false }, (row) => {
            row.eachCell({ includeEmpty: false }, (cell) => {
                if (cell.note) {
                    cell.note = undefined;
                }
            });
        });
    }
}
/** 单元格值 → 展示文本（对齐原 SheetJS raw:false 的语义） */
function toDisplayValue(value) {
    if (value == null)
        return '';
    if (value instanceof Date)
        return formatJsDate(value);
    if (typeof value === 'object') {
        const obj = value;
        if (typeof obj.text === 'string')
            return obj.text;
        if (obj.result != null)
            return toDisplayValue(obj.result);
        return '';
    }
    return value;
}
export async function parsePersonnelCertExcel(file) {
    const buffer = await file.arrayBuffer();
    const ExcelJSRuntime = (await import('exceljs')).default;
    const workbook = new ExcelJSRuntime.Workbook();
    await workbook.xlsx.load(buffer);
    const sheetName = workbook.worksheets.some((ws) => ws.name === '采集表')
        ? '采集表'
        : workbook.worksheets[0]?.name;
    if (!sheetName) {
        throw new Error('Excel 中没有工作表');
    }
    const sheet = workbook.getWorksheet(sheetName);
    if (!sheet) {
        throw new Error(`工作表 ${sheetName} 不存在`);
    }
    const matrix = [];
    sheet.eachRow({ includeEmpty: false }, (row, rowNumber) => {
        const line = [];
        for (let c = 1; c <= row.cellCount; c += 1) {
            line.push(toDisplayValue(row.getCell(c).value));
        }
        while (matrix.length < rowNumber - 1) {
            matrix.push([]);
        }
        matrix[rowNumber - 1] = line;
    });
    if (matrix.length === 0) {
        throw new Error('Excel 内容为空');
    }
    let headerRowIndex = -1;
    let columnMap = null;
    for (let r = 0; r < Math.min(matrix.length, 10); r += 1) {
        const headers = (matrix[r] ?? []).map((c) => String(c ?? ''));
        const nameCol = findColumnIndex(headers, NAME_ALIASES);
        const idCardCol = findColumnIndex(headers, ID_CARD_ALIASES);
        if (nameCol != null && idCardCol != null) {
            headerRowIndex = r;
            columnMap = {
                nameCol,
                idCardCol,
                certLevelCol: findColumnIndex(headers, CERT_LEVEL_ALIASES),
                certNoCol: findColumnIndex(headers, CERT_NO_ALIASES),
                issueDateCol: findColumnIndex(headers, ISSUE_DATE_ALIASES),
            };
            break;
        }
    }
    if (!columnMap || headerRowIndex < 0) {
        throw new Error('未找到表头行，请确认 Excel 包含「姓名」「身份证号码」列');
    }
    const rows = [];
    for (let r = headerRowIndex + 1; r < matrix.length; r += 1) {
        const line = matrix[r] ?? [];
        const name = String(line[columnMap.nameCol] ?? '').trim();
        const idCard = String(line[columnMap.idCardCol] ?? '').trim();
        if (!name && !idCard)
            continue;
        rows.push({
            rowIndex: r + 1,
            name,
            idCard,
        });
    }
    if (rows.length === 0) {
        throw new Error('未解析到有效人员行，请检查数据是否从表头下一行开始');
    }
    return {
        fileName: file.name,
        sheetName,
        headerRowIndex,
        columnMap,
        workbook,
        rows,
        results: [],
    };
}
export function runMockCertLookup(rows) {
    return rows.map((row) => {
        const lookup = mockLookupCertificate(row.name, row.idCard);
        return {
            rowIndex: row.rowIndex,
            name: row.name,
            idCard: row.idCard,
            ...lookup,
        };
    });
}
/** 将查证结果写回 workbook 对应单元格 */
export async function fillWorkbookWithResults(session, results) {
    const { workbook, sheetName, columnMap } = session;
    const sheet = workbook.getWorksheet(sheetName);
    if (!sheet) {
        throw new Error(`工作表 ${sheetName} 不存在`);
    }
    const resultMap = new Map(results.map((r) => [r.rowIndex, r]));
    for (const row of session.rows) {
        const result = resultMap.get(row.rowIndex);
        if (!result || result.status !== 'verified')
            continue;
        const writeCell = (col, value) => {
            if (col == null || value == null || value === '')
                return;
            sheet.getCell(row.rowIndex, col + 1).value = value;
        };
        writeCell(columnMap.certLevelCol, result.certLevel);
        writeCell(columnMap.certNoCol, result.certNo);
        writeCell(columnMap.issueDateCol, normalizeIssueDateText(result.issueDate));
    }
    stripWorkbookComments(workbook);
    normalizeSheetDatesToText(workbook);
    return workbook;
}
export async function downloadFilledWorkbook(workbook, fileName) {
    stripWorkbookComments(workbook);
    normalizeSheetDatesToText(workbook);
    const out = await workbook.xlsx.writeBuffer();
    const blob = new Blob([out], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const base = fileName.replace(/\.(xlsx|xls)$/i, '');
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${base}-查证回填.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
}
export function summarizeCertResults(results) {
    return {
        total: results.length,
        verified: results.filter((r) => r.status === 'verified').length,
        notFound: results.filter((r) => r.status === 'not_found').length,
        invalid: results.filter((r) => r.status === 'invalid').length,
        failed: results.filter((r) => r.status === 'failed').length,
        skipped: results.filter((r) => r.status === 'skipped').length,
    };
}
