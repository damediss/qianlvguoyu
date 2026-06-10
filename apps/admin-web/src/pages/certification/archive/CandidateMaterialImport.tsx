import { DownloadOutlined, FolderOpenOutlined, InboxOutlined } from '@ant-design/icons';
import { Button, Card, message, Space, Upload } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRef, useState, type ChangeEvent } from 'react';
import { importArchiveCandidateMaterials, pageArchiveImportErrors } from '../../../api/certificationArchive';

export function CandidateMaterialImport({ archiveId }: { archiveId?: string }) {
  const queryClient = useQueryClient();
  const folderInputRef = useRef<HTMLInputElement>(null);
  const [packing, setPacking] = useState(false);
  const mutation = useMutation({
    mutationFn: async (file: File) => {
      const data = new FormData();
      data.append('file', file);
      return importArchiveCandidateMaterials(archiveId!, data);
    },
    onSuccess: async (batch) => {
      if (batch.status === 'PARSE_FAILED') {
        try {
          const errors = await pageArchiveImportErrors(batch.id, { page: 1, pageSize: 1 });
          const detail = errors.items?.[0]?.errorMessage;
          message.error(detail ? `导入解析失败：${detail}` : '导入解析失败，请到「导入批次」查看异常详情');
        } catch {
          message.error('导入解析失败，请到「导入批次」查看异常详情');
        }
      } else if (batch.status === 'PARSE_SUCCESS') {
        message.success(`导入成功：${batch.batchNo}`);
      } else {
        message.success(`导入批次已创建：${batch.batchNo}`);
      }
      queryClient.invalidateQueries({ queryKey: ['certification-archive-batches', archiveId] });
      queryClient.invalidateQueries({ queryKey: ['certification-archive-tasks'] });
    },
    onError: (error: Error) => {
      message.error(error.message || '导入失败，请检查材料格式或稍后重试');
    },
  });

  const uploadFile = (file: File) => {
    if (!archiveId) {
      message.warning('请先选择归档记录');
      return;
    }
    mutation.mutate(file);
  };

  const handleFolderChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const fileList = event.target.files;
    if (!fileList?.length) {
      return;
    }
    if (!archiveId) {
      message.warning('请先选择归档记录');
      event.target.value = '';
      return;
    }

    setPacking(true);
    const hide = message.loading('正在打包文件夹...', 0);
    try {
      const zipFile = await createZipFileFromFolder(Array.from(fileList));
      await mutation.mutateAsync(zipFile);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '文件夹打包失败');
    } finally {
      hide();
      setPacking(false);
      event.target.value = '';
    }
  };

  const uploading = packing || mutation.isPending;

  return (
    <Card size="small" title="考生材料批量导入">
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Button icon={<DownloadOutlined />} onClick={downloadCandidateMaterialTemplate}>
          下载模板
        </Button>
        <Upload.Dragger
          accept=".zip"
          maxCount={1}
          disabled={uploading}
          beforeUpload={(file) => {
            uploadFile(file);
            return false;
          }}
        >
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p className="ant-upload-text">选择 ZIP 文件</p>
          <p className="ant-upload-hint">支持 zip，最大 2GB</p>
        </Upload.Dragger>
        <input
          ref={folderInputRef}
          type="file"
          style={{ display: 'none' }}
          // @ts-expect-error webkitdirectory is supported by Chromium-based browsers
          webkitdirectory=""
          directory=""
          multiple
          onChange={handleFolderChange}
        />
        <Button
          block
          icon={<FolderOpenOutlined />}
          loading={uploading}
          onClick={() => folderInputRef.current?.click()}
        >
          选择文件夹
        </Button>
        <div style={{ color: 'rgba(0,0,0,0.45)', fontSize: 12 }}>
          文件夹需包含“考生清单.xlsx”及各考生子文件夹；系统将自动打包为标准 ZIP 后上传。
        </div>
      </Space>
    </Card>
  );
}

function downloadCandidateMaterialTemplate() {
  const zipBytes = createZip([
    {
      name: '考生清单.xlsx',
      data: createCandidateListXlsx(),
    },
    {
      name: '考生材料导入说明.txt',
      data: encodeText([
        '考生材料导入模板',
        '',
        '1. ZIP 根目录必须包含“考生清单.xlsx”。',
        '2. 清单第 1 行为表头，第 2 行开始填写：考生编号、姓名、身份证号、文件夹名称。',
        '3. 每名考生的材料放在清单“文件夹名称”对应的 ZIP 文件夹内。',
        '4. 材料文件名需使用：评分表、试卷、鉴定表、身份证、学历证明、工作证明、培训合格证。',
        '5. 支持 pdf、jpg、jpeg、png、doc、docx、xls、xlsx 文件。',
      ].join('\r\n')),
    },
    ...['评分表', '试卷', '鉴定表', '身份证', '学历证明', '工作证明', '培训合格证'].map((name) => ({
      name: `示例考生_450102199001011234/${name}.pdf`,
      data: createSamplePdf(),
    })),
  ]);
  const blob = new Blob([zipBytes], { type: 'application/zip' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = '考生材料导入模板.zip';
  link.click();
  URL.revokeObjectURL(url);
}

function createCandidateListXlsx() {
  const sheetRows = [
    ['考生编号', '姓名', '身份证号', '文件夹名称'],
    ['C001', '示例考生', '450102199001011234', '示例考生_450102199001011234'],
  ];
  const sheetData = sheetRows
    .map((row, rowIndex) => {
      const cells = row
        .map((value, colIndex) => {
          const ref = `${String.fromCharCode(65 + colIndex)}${rowIndex + 1}`;
          return `<c r="${ref}" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>`;
        })
        .join('');
      return `<row r="${rowIndex + 1}">${cells}</row>`;
    })
    .join('');

  return createZip([
    {
      name: '[Content_Types].xml',
      data: encodeText(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        + '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        + '<Default Extension="xml" ContentType="application/xml"/>'
        + '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        + '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        + '</Types>',
      ),
    },
    {
      name: '_rels/.rels',
      data: encodeText(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>'
        + '</Relationships>',
      ),
    },
    {
      name: 'xl/workbook.xml',
      data: encodeText(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        + 'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        + '<sheets><sheet name="考生清单" sheetId="1" r:id="rId1"/></sheets>'
        + '</workbook>',
      ),
    },
    {
      name: 'xl/_rels/workbook.xml.rels',
      data: encodeText(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>'
        + '</Relationships>',
      ),
    },
    {
      name: 'xl/worksheets/sheet1.xml',
      data: encodeText(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        + '<dimension ref="A1:D2"/>'
        + '<sheetData>'
        + sheetData
        + '</sheetData>'
        + '</worksheet>',
      ),
    },
  ]);
}

async function createZipFileFromFolder(files: File[]) {
  const entries = await Promise.all(
    files
      .map((file) => {
        const relativePath = file.webkitRelativePath || file.name;
        if (!relativePath) {
          return null;
        }
        const path = relativePath.replace(/\\/g, '/');
        if (path.endsWith('/')) {
          return null;
        }
        return { path, file };
      })
      .filter((entry): entry is { path: string; file: File } => entry !== null)
      .map(async ({ path, file }) => ({
        path,
        data: new Uint8Array(await file.arrayBuffer()),
      })),
  );

  if (entries.length === 0) {
    throw new Error('所选文件夹为空');
  }

  const prefix = detectStripPrefix(entries.map((entry) => entry.path));
  const zipEntries = entries
    .map((entry) => ({
      name: stripPrefix(entry.path, prefix),
      data: entry.data,
    }))
    .filter((entry) => entry.name.length > 0);

  if (!zipEntries.some((entry) => entry.name === '考生清单.xlsx')) {
    throw new Error('文件夹中缺少“考生清单.xlsx”');
  }

  const zipBytes = createZip(zipEntries);
  const folderName = entries[0]?.path.split('/')[0] || '考生材料';
  return new File([zipBytes], `${folderName}.zip`, { type: 'application/zip' });
}

function detectStripPrefix(paths: string[]) {
  if (paths.some((path) => path === '考生清单.xlsx')) {
    return '';
  }

  const excelPath = paths.find((path) => path.endsWith('/考生清单.xlsx'));
  if (!excelPath) {
    return '';
  }

  const segments = excelPath.split('/');
  if (segments.length !== 2 || segments[1] !== '考生清单.xlsx') {
    return '';
  }

  const prefix = `${segments[0]}/`;
  const allUnderRoot = paths.every((path) => path === segments[0] || path.startsWith(prefix));
  return allUnderRoot ? prefix : '';
}

function stripPrefix(path: string, prefix: string) {
  if (!prefix) {
    return path;
  }
  if (path.startsWith(prefix)) {
    return path.slice(prefix.length);
  }
  if (path === prefix.slice(0, -1)) {
    return '';
  }
  return path;
}

function createSamplePdf() {
  return encodeText([
    '%PDF-1.4',
    '1 0 obj',
    '<< /Type /Catalog /Pages 2 0 R >>',
    'endobj',
    '2 0 obj',
    '<< /Type /Pages /Count 0 >>',
    'endobj',
    'trailer',
    '<< /Root 1 0 R >>',
    '%%EOF',
  ].join('\n'));
}

function createZip(entries: Array<{ name: string; data: Uint8Array }>) {
  const localParts: Uint8Array[] = [];
  const centralParts: Uint8Array[] = [];
  let offset = 0;
  const { time, date } = dosDateTime(new Date());

  entries.forEach((entry) => {
    const name = encodeText(entry.name);
    const crc = crc32(entry.data);
    const localHeader = zipHeader(30);
    localHeader.setUint32(0, 0x04034b50, true);
    localHeader.setUint16(4, 20, true);
    localHeader.setUint16(6, 0x0800, true);
    localHeader.setUint16(8, 0, true);
    localHeader.setUint16(10, time, true);
    localHeader.setUint16(12, date, true);
    localHeader.setUint32(14, crc, true);
    localHeader.setUint32(18, entry.data.length, true);
    localHeader.setUint32(22, entry.data.length, true);
    localHeader.setUint16(26, name.length, true);
    localParts.push(new Uint8Array(localHeader.buffer), name, entry.data);

    const centralHeader = zipHeader(46);
    centralHeader.setUint32(0, 0x02014b50, true);
    centralHeader.setUint16(4, 20, true);
    centralHeader.setUint16(6, 20, true);
    centralHeader.setUint16(8, 0x0800, true);
    centralHeader.setUint16(10, 0, true);
    centralHeader.setUint16(12, time, true);
    centralHeader.setUint16(14, date, true);
    centralHeader.setUint32(16, crc, true);
    centralHeader.setUint32(20, entry.data.length, true);
    centralHeader.setUint32(24, entry.data.length, true);
    centralHeader.setUint16(28, name.length, true);
    centralHeader.setUint32(42, offset, true);
    centralParts.push(new Uint8Array(centralHeader.buffer), name);

    offset += localHeader.byteLength + name.length + entry.data.length;
  });

  const centralSize = totalLength(centralParts);
  const endHeader = zipHeader(22);
  endHeader.setUint32(0, 0x06054b50, true);
  endHeader.setUint16(8, entries.length, true);
  endHeader.setUint16(10, entries.length, true);
  endHeader.setUint32(12, centralSize, true);
  endHeader.setUint32(16, offset, true);

  return concatBytes([...localParts, ...centralParts, new Uint8Array(endHeader.buffer)]);
}

function zipHeader(size: number) {
  return new DataView(new ArrayBuffer(size));
}

function dosDateTime(value: Date) {
  const time = (value.getHours() << 11) | (value.getMinutes() << 5) | Math.floor(value.getSeconds() / 2);
  const date = ((value.getFullYear() - 1980) << 9) | ((value.getMonth() + 1) << 5) | value.getDate();
  return { time, date };
}

function totalLength(parts: Uint8Array[]) {
  return parts.reduce((sum, part) => sum + part.length, 0);
}

function concatBytes(parts: Uint8Array[]) {
  const output = new Uint8Array(totalLength(parts));
  let offset = 0;
  parts.forEach((part) => {
    output.set(part, offset);
    offset += part.length;
  });
  return output;
}

function crc32(bytes: Uint8Array) {
  let crc = 0xffffffff;
  bytes.forEach((byte) => {
    crc ^= byte;
    for (let i = 0; i < 8; i++) {
      crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
  });
  return (crc ^ 0xffffffff) >>> 0;
}

function encodeText(value: string) {
  return new TextEncoder().encode(value);
}

function escapeXml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
