import { describe, expect, it } from 'vitest';
import ExcelJS from 'exceljs';
import {
  fillWorkbookWithResults,
  parsePersonnelCertExcel,
  runMockCertLookup,
  type PersonnelCertExcelSession,
} from '../src/utils/personnelCertExcel';

/**
 * P1-7 迁移回归：xlsx（SheetJS）→ exceljs 重写 personnelCertExcel 后，
 * 用「构造工作簿 → 解析 → mock 查证 → 回填 → 再解析」闭环验证解析/表头定位/回填语义未变。
 */

/** jsdom 的 File/Blob 可能缺 arrayBuffer()，用 FileReader 兜底 */
function blobToArrayBuffer(blob: Blob): Promise<ArrayBuffer> {
  if (typeof blob.arrayBuffer === 'function') {
    return blob.arrayBuffer();
  }
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as ArrayBuffer);
    reader.onerror = () => reject(reader.error);
    reader.readAsArrayBuffer(blob);
  });
}

function toFile(buffer: ArrayBuffer, name: string): File {
  const file = new File([buffer], name);
  if (typeof file.arrayBuffer !== 'function') {
    const blob = new Blob([buffer]);
    Object.defineProperty(file, 'arrayBuffer', {
      value: () => blobToArrayBuffer(blob),
    });
  }
  return file;
}

async function buildRosterFile(): Promise<File> {
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet('采集表');
  sheet.addRow(['姓名', '身份证号码', '已有证书等级', '证书编号', '发证日期']);
  sheet.addRow(['张三', '450103199001011234', '', '', '']);
  sheet.addRow(['', '', '', '', '']);
  sheet.addRow(['李四', '450103199001010120', '', '', '']);
  const buffer = (await workbook.xlsx.writeBuffer()) as unknown as ArrayBuffer;
  return toFile(buffer, '花名册.xlsx');
}

describe('personnelCertExcel（exceljs 迁移回归）', () => {
  it('解析：定位「采集表」表头与五列映射，跳过空行，行号按 Excel 1-based', async () => {
    const session = await parsePersonnelCertExcel(await buildRosterFile());

    expect(session.sheetName).toBe('采集表');
    expect(session.headerRowIndex).toBe(0);
    expect(session.columnMap).toEqual({
      nameCol: 0,
      idCardCol: 1,
      certLevelCol: 2,
      certNoCol: 3,
      issueDateCol: 4,
    });
    expect(session.rows).toHaveLength(2);
    expect(session.rows[0]).toEqual({
      rowIndex: 2,
      name: '张三',
      idCard: '450103199001011234',
    });
    expect(session.rows[1]).toEqual({
      rowIndex: 4,
      name: '李四',
      idCard: '450103199001010120',
    });
  });

  it('mock 查证：合法 18 位非 0 尾号 verified，尾号 0 not_found', async () => {
    const session = await parsePersonnelCertExcel(await buildRosterFile());
    const results = runMockCertLookup(session.rows);

    expect(results[0].status).toBe('verified');
    expect(results[0].certNo).toBeTruthy();
    expect(results[1].status).toBe('not_found');
  });

  it('回填：verified 结果写入等级/编号/发证日期列，写出的文件可再次解析', async () => {
    const session = await parsePersonnelCertExcel(await buildRosterFile());
    const results = runMockCertLookup(session.rows);
    const workbook = await fillWorkbookWithResults(session, results);

    const sheet = workbook.getWorksheet('采集表');
    expect(sheet).toBeTruthy();
    const verified = results[0];
    expect(sheet!.getCell(2, session.columnMap.certLevelCol! + 1).value).toBe(verified.certLevel);
    expect(sheet!.getCell(2, session.columnMap.certNoCol! + 1).value).toBe(verified.certNo);
    expect(sheet!.getCell(2, session.columnMap.issueDateCol! + 1).value).toBe(verified.issueDate);
    // not_found 行不回填
    expect(sheet!.getCell(4, session.columnMap.certNoCol! + 1).value).toBe('');

    const outBuffer = (await workbook.xlsx.writeBuffer()) as unknown as ArrayBuffer;
    const roundTrip: PersonnelCertExcelSession = await parsePersonnelCertExcel(
      toFile(outBuffer, '回填.xlsx'),
    );
    expect(roundTrip.rows).toHaveLength(2);
    expect(roundTrip.rows[0].name).toBe('张三');
  });
});
