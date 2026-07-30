/** 使用 SheetJS 将 xlsx/xls 渲染为 HTML 表格（本地预览，无需 Office Online） */
export async function renderSpreadsheetPreview(blob: Blob, container: HTMLElement): Promise<void> {
  const XLSX = await import('xlsx');
  const buffer = await blob.arrayBuffer();
  const workbook = XLSX.read(buffer, { type: 'array' });
  const sheetName = workbook.SheetNames[0];
  if (!sheetName) {
    container.innerHTML = '<p>表格为空</p>';
    return;
  }
  const sheet = workbook.Sheets[sheetName];
  container.innerHTML = XLSX.utils.sheet_to_html(sheet, { id: 'spreadsheet-preview-table' });
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
  if (workbook.SheetNames.length > 1) {
    const hint = document.createElement('p');
    hint.style.color = '#888';
    hint.style.marginTop = '8px';
    hint.textContent = `共 ${workbook.SheetNames.length} 个工作表，当前预览：${sheetName}`;
    container.appendChild(hint);
  }
}
