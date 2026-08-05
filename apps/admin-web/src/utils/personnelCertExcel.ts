/** 人员认定 Excel 导入查证回填（前端 SheetJS 解析/写回；查证走后端 verify-lookup） */

import type { WorkBook } from 'xlsx';

export type CertFillStatus = 'pending' | 'verified' | 'not_found' | 'invalid' | 'failed' | 'skipped';

export interface ExcelColumnMap {
  nameCol: number;
  idCardCol: number;
  certLevelCol?: number;
  certNoCol?: number;
  issueDateCol?: number;
}

export interface ParsedExcelPersonRow {
  /** Excel 行号（1-based） */
  rowIndex: number;
  name: string;
  idCard: string;
}

export interface CertLookupRowResult {
  rowIndex: number;
  name: string;
  idCard: string;
  status: CertFillStatus;
  certLevel?: string;
  certNo?: string;
  issueDate?: string;
  message?: string;
}

export interface PersonnelCertExcelSession {
  fileName: string;
  sheetName: string;
  headerRowIndex: number;
  columnMap: ExcelColumnMap;
  workbook: WorkBook;
  rows: ParsedExcelPersonRow[];
  results: CertLookupRowResult[];
}

const NAME_ALIASES = ['姓名', 'name', '人员姓名'];
const ID_CARD_ALIASES = ['身份证号码', '身份证号', '身份证', 'id_card', 'idcard'];
const CERT_LEVEL_ALIASES = ['已有证书等级', '证书等级', '等级'];
const CERT_NO_ALIASES = ['证书编号', '编号', 'paper_no'];
const ISSUE_DATE_ALIASES = ['发证日期', '发证时间', 'cert_issue_dates'];

function normalizeHeader(value: unknown): string {
  return String(value ?? '').trim().replace(/\s+/g, '');
}

function findColumnIndex(headers: string[], aliases: string[]): number | undefined {
  const normalized = headers.map(normalizeHeader);
  for (const alias of aliases) {
    const idx = normalized.findIndex((h) => h === alias || h.includes(alias));
    if (idx >= 0) return idx;
  }
  return undefined;
}

function isValidIdCard(idCard: string): boolean {
  const s = idCard.trim();
  return /^(\d{15}|\d{17}[\dXx])$/.test(s);
}

/** 仅用于本地开发/单测；业务路径请调用 POST /training/personnel-cert/verify-lookup */
export function mockLookupCertificate(name: string, idCard: string): Omit<CertLookupRowResult, 'rowIndex' | 'name' | 'idCard'> {
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
function excelSerialToDateText(serial: number): string {
  const utcDays = Math.floor(serial) - 25569;
  const date = new Date(utcDays * 86400 * 1000);
  if (Number.isNaN(date.getTime())) return String(serial);
  return `${date.getUTCFullYear()}/${date.getUTCMonth() + 1}/${date.getUTCDate()}`;
}

function formatJsDate(value: Date): string {
  return `${value.getFullYear()}/${value.getMonth() + 1}/${value.getDate()}`;
}

/**
 * SheetJS 再写出时常丢失日期数字格式，Excel 会把 45376 当普通数字显示。
 * 下载前把日期单元格转成可读文本，避免「2024/3/25 → 45376」。
 */
function normalizeSheetDatesToText(workbook: WorkBook, XLSX: typeof import('xlsx')): void {
  for (const name of workbook.SheetNames) {
    const sheet = workbook.Sheets[name];
    if (!sheet?.['!ref']) continue;
    const range = XLSX.utils.decode_range(sheet['!ref']);
    for (let r = range.s.r; r <= range.e.r; r += 1) {
      for (let c = range.s.c; c <= range.e.c; c += 1) {
        const addr = XLSX.utils.encode_cell({ r, c });
        const cell = sheet[addr];
        if (!cell) continue;

        if (cell.t === 'd' && cell.v instanceof Date) {
          const text = formatJsDate(cell.v);
          sheet[addr] = { t: 's', v: text, w: text };
          continue;
        }

        if (cell.t !== 'n' || typeof cell.v !== 'number') continue;

        const numFmt = typeof cell.z === 'string' ? cell.z : '';
        const isDateFmt = numFmt ? XLSX.SSF.is_date(numFmt) : false;
        const formatted = typeof cell.w === 'string' ? cell.w.trim() : '';
        const looksFormattedDate = /^\d{4}[\/\-年]\d{1,2}[\/\-月]\d{1,2}/.test(formatted)
          || /^\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4}/.test(formatted);

        // 典型业务日期序列区间（约 1950–2100），且带日期格式或已有日期形格式化文本
        const inDateSerialRange = cell.v >= 18264 && cell.v <= 73050;
        if (!isDateFmt && !(looksFormattedDate && inDateSerialRange)) continue;

        const text = formatted && looksFormattedDate
          ? formatted.replace(/年|月/g, '/').replace(/日/g, '')
          : excelSerialToDateText(cell.v);
        sheet[addr] = { t: 's', v: text, w: text };
      }
    }
  }
}

/** 查证回填的发证日期：若误传序列号则转文本 */
function normalizeIssueDateText(value?: string): string | undefined {
  if (value == null || value === '') return undefined;
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
function stripWorkbookComments(workbook: WorkBook): void {
  for (const name of workbook.SheetNames) {
    const sheet = workbook.Sheets[name] as Record<string, unknown> | undefined;
    if (!sheet) continue;
    delete sheet['!comments'];
    for (const key of Object.keys(sheet)) {
      if (key.startsWith('!')) continue;
      const cell = sheet[key] as { c?: unknown } | undefined;
      if (cell && cell.c != null) {
        delete cell.c;
      }
    }
  }
}

export async function parsePersonnelCertExcel(file: File): Promise<PersonnelCertExcelSession> {
  const XLSX = await import('xlsx');
  const buffer = await file.arrayBuffer();
  // cellNF：保留日期数字格式，便于写出前识别并转成文本
  const workbook = XLSX.read(buffer, { type: 'array', cellDates: true, cellNF: true });
  const sheetName =
    workbook.SheetNames.includes('采集表') ? '采集表' : workbook.SheetNames[0];
  if (!sheetName) {
    throw new Error('Excel 中没有工作表');
  }
  const sheet = workbook.Sheets[sheetName];
  const matrix = XLSX.utils.sheet_to_json<(string | number | null)[]>(sheet, {
    header: 1,
    defval: '',
    raw: false,
    dateNF: 'yyyy/m/d',
  }) as (string | number | null)[][];

  if (matrix.length === 0) {
    throw new Error('Excel 内容为空');
  }

  let headerRowIndex = -1;
  let columnMap: ExcelColumnMap | null = null;
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

  const rows: ParsedExcelPersonRow[] = [];
  for (let r = headerRowIndex + 1; r < matrix.length; r += 1) {
    const line = matrix[r] ?? [];
    const name = String(line[columnMap.nameCol] ?? '').trim();
    const idCard = String(line[columnMap.idCardCol] ?? '').trim();
    if (!name && !idCard) continue;
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

export function runMockCertLookup(rows: ParsedExcelPersonRow[]): CertLookupRowResult[] {
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
export async function fillWorkbookWithResults(
  session: PersonnelCertExcelSession,
  results: CertLookupRowResult[],
): Promise<WorkBook> {
  const XLSX = await import('xlsx');
  const { workbook, sheetName, columnMap } = session;
  const sheet = workbook.Sheets[sheetName];
  if (!sheet) {
    throw new Error(`工作表 ${sheetName} 不存在`);
  }

  const resultMap = new Map(results.map((r) => [r.rowIndex, r]));

  for (const row of session.rows) {
    const result = resultMap.get(row.rowIndex);
    if (!result || result.status !== 'verified') continue;

    const excelRow = row.rowIndex - 1;
    const writeCell = (col: number | undefined, value?: string) => {
      if (col == null || value == null || value === '') return;
      const addr = XLSX.utils.encode_cell({ r: excelRow, c: col });
      sheet[addr] = { t: 's', v: value, w: value };
    };

    writeCell(columnMap.certLevelCol, result.certLevel);
    writeCell(columnMap.certNoCol, result.certNo);
    writeCell(columnMap.issueDateCol, normalizeIssueDateText(result.issueDate));
  }

  if (sheet['!ref']) {
    const range = XLSX.utils.decode_range(sheet['!ref']);
    for (const row of session.rows) {
      const excelRow = row.rowIndex - 1;
      if (excelRow > range.e.r) range.e.r = excelRow;
    }
    sheet['!ref'] = XLSX.utils.encode_range(range);
  }

  stripWorkbookComments(workbook);
  normalizeSheetDatesToText(workbook, XLSX);
  return workbook;
}

export async function downloadFilledWorkbook(workbook: WorkBook, fileName: string): Promise<void> {
  const XLSX = await import('xlsx');
  stripWorkbookComments(workbook);
  normalizeSheetDatesToText(workbook, XLSX);
  const out = XLSX.write(workbook, { bookType: 'xlsx', type: 'array', cellDates: true });
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

export function summarizeCertResults(results: CertLookupRowResult[]) {
  return {
    total: results.length,
    verified: results.filter((r) => r.status === 'verified').length,
    notFound: results.filter((r) => r.status === 'not_found').length,
    invalid: results.filter((r) => r.status === 'invalid').length,
    failed: results.filter((r) => r.status === 'failed').length,
    skipped: results.filter((r) => r.status === 'skipped').length,
  };
}
