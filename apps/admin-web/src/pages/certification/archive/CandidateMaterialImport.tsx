import { DownloadOutlined, InboxOutlined } from '@ant-design/icons';
import { Button, Card, message, Space, Upload } from 'antd';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { importArchiveCandidateMaterials } from '../../../api/certificationArchive';

export function CandidateMaterialImport({ archiveId }: { archiveId?: string }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: async (file: File) => {
      const data = new FormData();
      data.append('file', file);
      return importArchiveCandidateMaterials(archiveId!, data);
    },
    onSuccess: (batch) => {
      message.success(`导入批次已创建：${batch.batchNo}`);
      queryClient.invalidateQueries({ queryKey: ['certification-archive-batches', archiveId] });
    },
  });

  return (
    <Card size="small" title="考生材料批量导入">
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Button icon={<DownloadOutlined />} onClick={downloadCandidateMaterialTemplate}>
          下载模板
        </Button>
        <Upload.Dragger
          accept=".zip"
          maxCount={1}
          beforeUpload={(file) => {
            if (!archiveId) {
              message.warning('请先选择归档记录');
              return Upload.LIST_IGNORE;
            }
            mutation.mutate(file);
            return false;
          }}
        >
          <p className="ant-upload-drag-icon"><InboxOutlined /></p>
          <p className="ant-upload-text">选择 ZIP 文件</p>
          <p className="ant-upload-hint">支持 zip，最大 2GB</p>
        </Upload.Dragger>
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
