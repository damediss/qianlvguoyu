import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Space, Spin, Typography, message } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import {
  fetchPlatformFileBlob,
  fetchUrlFileBlob,
  resolveFilePreviewKind,
} from '../../../../api/files';

type Props = {
  fileId?: string | number | null;
  filePath?: string | null;
  fileName?: string | null;
  /**
   * 保存单元格：由上层调后端 POI patch，保留边框格式。
   * key 为 A1 地址。
   */
  onSaveCells: (cells: Record<string, string>) => void | Promise<void>;
  saveLoading?: boolean;
  editable?: boolean;
};

type WorkbookHandle = {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  XLSX: any;
  sheetName: string;
  tableId: string;
};

function cellAddrFromTdId(id: string, tableId: string): string | null {
  const prefix = `${tableId}-`;
  if (!id.startsWith(prefix)) return null;
  const addr = id.slice(prefix.length);
  return /^[A-Z]+\d+$/i.test(addr) ? addr.toUpperCase() : null;
}

function parseEditedText(raw: string): string {
  return (raw || '').replace(/\u00a0/g, ' ').trimEnd();
}

/**
 * 补贴材料 xlsx 内嵌编辑。
 * 展示用 SheetJS HTML；保存只提交单元格文本，由后端 POI 写回以保留边框。
 */
export function MaterialSpreadsheetEditor({
  fileId,
  filePath,
  fileName,
  onSaveCells,
  saveLoading,
  editable = true,
}: Props) {
  const hostRef = useRef<HTMLDivElement>(null);
  const bookRef = useRef<WorkbookHandle | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
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
      if (hostRef.current) hostRef.current.innerHTML = '';

      try {
        const blob = filePath
          ? await fetchUrlFileBlob(filePath)
          : await fetchPlatformFileBlob(String(fileId));
        if (cancelled) return;

        const kind = resolveFilePreviewKind(fileName || '', undefined, blob.type);
        if (kind !== 'spreadsheet') {
          setError('当前不是 Excel 表格，请用 OnlyOffice 编辑或下载后修改');
          return;
        }

        const XLSX = await import('xlsx');
        const buffer = await blob.arrayBuffer();
        const workbook = XLSX.read(buffer, { type: 'array', cellStyles: true });
        const sheetName = workbook.SheetNames[0];
        if (!sheetName) {
          setError('表格为空');
          return;
        }
        if (cancelled || !hostRef.current) return;

        const tableId = `material-edit-table-${String(fileId || 'x').replace(/\W/g, '')}`;
        const sheet = workbook.Sheets[sheetName];
        hostRef.current.innerHTML = XLSX.utils.sheet_to_html(sheet, { id: tableId });

        const table = hostRef.current.querySelector('table');
        if (table) {
          table.classList.add('material-edit-spreadsheet-table');
          table.querySelectorAll('td').forEach((td) => {
            const el = td as HTMLTableCellElement;
            if (editable) {
              el.contentEditable = 'true';
              el.spellcheck = false;
              el.addEventListener('input', () => setDirty(true));
            }
          });
        }

        if (workbook.SheetNames.length > 1) {
          const hint = document.createElement('p');
          hint.className = 'material-edit-sheet-hint';
          hint.textContent = `共 ${workbook.SheetNames.length} 个工作表，当前编辑：${sheetName}`;
          hostRef.current.appendChild(hint);
        }

        bookRef.current = { XLSX, sheetName, tableId };
        setKindOk(true);
      } catch (e) {
        if (!cancelled) setError((e as Error).message || '加载表格失败');
      } finally {
        if (!cancelled) setLoading(false);
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

    const cells: Record<string, string> = {};
    host.querySelectorAll(`td[id^="${handle.tableId}-"]`).forEach((node) => {
      const td = node as HTMLTableCellElement;
      const addr = cellAddrFromTdId(td.id, handle.tableId);
      if (!addr) return;
      cells[addr] = parseEditedText(td.innerText || '');
    });

    if (Object.keys(cells).length === 0) {
      message.warning('没有可保存的单元格');
      return;
    }

    try {
      await onSaveCells(cells);
      setDirty(false);
    } catch (e) {
      message.error((e as Error).message || '保存失败');
      throw e;
    }
  };

  if (error && !kindOk) {
    return <Alert type="warning" showIcon message="无法在线编辑表格" description={error} />;
  }

  return (
    <div className="material-spreadsheet-editor">
      {editable ? (
        <div className="material-spreadsheet-editor-toolbar">
          <Space wrap>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={saveLoading}
              disabled={!dirty && !saveLoading}
              onClick={() => void handleSave()}
            >
              保存修改
            </Button>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {dirty
                ? '有未保存的修改'
                : '点击单元格编辑；保存时按模板底稿写回，保留边框格式'}
            </Typography.Text>
          </Space>
        </div>
      ) : null}
      <Spin spinning={loading} tip="加载表格…">
        <div ref={hostRef} className="material-file-preview-body material-spreadsheet-edit-body" />
      </Spin>
    </div>
  );
}
