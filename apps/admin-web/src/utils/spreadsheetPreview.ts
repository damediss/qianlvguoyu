/** 使用 exceljs 将 xlsx 渲染为 HTML 表格（本地预览，无需 Office Online） */

// P1-7：随 xlsx（SheetJS）依赖移除迁移到 exceljs。
// 单元格文本一律 HTML 转义 —— 上传的表格内容属不可信输入，直接拼 innerHTML 是存储型 XSS。
// 运行时按需动态导入，避免把 ~1.4MB 的 exceljs 打进主包。
import type ExcelJS from 'exceljs';

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function cellToText(value: unknown): string {
  if (value == null) return '';
  if (value instanceof Date) {
    return `${value.getFullYear()}/${value.getMonth() + 1}/${value.getDate()}`;
  }
  if (typeof value === 'object') {
    const obj = value as { text?: unknown; result?: unknown };
    if (typeof obj.text === 'string') return obj.text;
    if (obj.result != null) return cellToText(obj.result);
    return '';
  }
  return String(value);
}

export async function renderSpreadsheetPreview(blob: Blob, container: HTMLElement): Promise<void> {
  const buffer = await blob.arrayBuffer();
  const ExcelJSRuntime = (await import('exceljs')).default;
  const workbook = new ExcelJSRuntime.Workbook();
  await workbook.xlsx.load(buffer);
  const sheet = workbook.worksheets[0];
  if (!sheet) {
    container.innerHTML = '<p>表格为空</p>';
    return;
  }

  const rows: string[] = [];
  sheet.eachRow({ includeEmpty: false }, (row) => {
    const cells: string[] = [];
    for (let c = 1; c <= row.cellCount; c += 1) {
      cells.push(`<td>${escapeHtml(cellToText(row.getCell(c).value))}</td>`);
    }
    rows.push(`<tr>${cells.join('')}</tr>`);
  });
  container.innerHTML = `<table id="spreadsheet-preview-table"><tbody>${rows.join('')}</tbody></table>`;

  const table = container.querySelector('table');
  if (table) {
    table.style.borderCollapse = 'collapse';
    table.style.width = '100%';
    table.style.fontSize = '13px';
    table.querySelectorAll('td, th').forEach((cell) => {
      const el = cell as HTMLElement;
      el.style.border = '1px solid #d9d9d9';
      el.style.padding = '4px 8px';
      el.style.whiteSpace = 'pre-wrap';
    });
  }
  if (workbook.worksheets.length > 1) {
    const hint = document.createElement('p');
    hint.style.color = '#888';
    hint.style.marginTop = '8px';
    hint.textContent = `共 ${workbook.worksheets.length} 个工作表，当前预览：${sheet.name}`;
    container.appendChild(hint);
  }
}
