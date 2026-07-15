/**
 * 人员认定 — 导入 Excel 查证并回填（真实查证 API，不落库）
 */
import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Progress,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadProps } from 'antd';
import {
  CloudDownloadOutlined,
  FileExcelOutlined,
  InboxOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import * as trainApi from '../../api/training';
import type {
  CertFillStatus,
  CertLookupRowResult,
  PersonnelCertExcelSession,
} from '../../utils/personnelCertExcel';
import {
  downloadFilledWorkbook,
  fillWorkbookWithResults,
  parsePersonnelCertExcel,
  summarizeCertResults,
} from '../../utils/personnelCertExcel';
import './PersonnelCertImportPanel.css';

const { Dragger } = Upload;
const { Text, Paragraph } = Typography;

const BATCH_SIZE = 20;

const STATUS_TAG: Record<CertFillStatus, { label: string; color: string }> = {
  pending: { label: '待查证', color: 'default' },
  verified: { label: '已回填', color: 'success' },
  not_found: { label: '无结果', color: 'warning' },
  invalid: { label: '无效行', color: 'error' },
  failed: { label: '失败', color: 'error' },
  skipped: { label: '已跳过', color: 'default' },
};

function maskIdCard(idCard?: string): string {
  if (!idCard) return '-';
  const s = idCard.trim();
  if (s.length <= 8) return s;
  return `${s.slice(0, 4)}********${s.slice(-4)}`;
}

function toLookupRow(detail: {
  rowIndex: number;
  name?: string;
  idCard?: string;
  status: string;
  certLevel?: string;
  certNo?: string;
  issueDate?: string;
  message?: string;
}): CertLookupRowResult {
  return {
    rowIndex: detail.rowIndex,
    name: detail.name ?? '',
    idCard: detail.idCard ?? '',
    status: (detail.status as CertFillStatus) || 'failed',
    certLevel: detail.certLevel,
    certNo: detail.certNo,
    issueDate: detail.issueDate,
    message: detail.message,
  };
}

export function PersonnelCertImportPanel() {
  const [session, setSession] = useState<PersonnelCertExcelSession | null>(null);
  const [results, setResults] = useState<CertLookupRowResult[]>([]);
  const [filledWorkbook, setFilledWorkbook] = useState<PersonnelCertExcelSession['workbook'] | null>(null);
  const [parsing, setParsing] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [downloading, setDownloading] = useState(false);

  const summary = useMemo(() => summarizeCertResults(results), [results]);

  const uploadProps: UploadProps = {
    accept: '.xlsx,.xls',
    maxCount: 1,
    showUploadList: false,
    beforeUpload: (file) => {
      void handleImport(file);
      return false;
    },
  };

  const handleImport = async (file: File) => {
    setParsing(true);
    setResults([]);
    setFilledWorkbook(null);
    setProgress(0);
    try {
      const parsed = await parsePersonnelCertExcel(file);
      setSession(parsed);
      message.success(`已解析 ${parsed.rows.length} 条人员记录`);
    } catch (err) {
      setSession(null);
      message.error(err instanceof Error ? err.message : '解析 Excel 失败');
    } finally {
      setParsing(false);
    }
  };

  const handleVerifyAndFill = async () => {
    if (!session) return;
    setVerifying(true);
    setProgress(0);
    setFilledWorkbook(null);
    try {
      const allResults: CertLookupRowResult[] = [];
      for (let i = 0; i < session.rows.length; i += BATCH_SIZE) {
        const chunk = session.rows.slice(i, i + BATCH_SIZE);
        const resp = await trainApi.verifyPersonnelCertLookup(
          chunk.map((r) => ({
            rowIndex: r.rowIndex,
            name: r.name,
            idCard: r.idCard,
          })),
        );
        allResults.push(...resp.details.map(toLookupRow));
        setProgress(Math.min(100, Math.round(((i + chunk.length) / session.rows.length) * 100)));
        setResults([...allResults]);
      }
      const wb = await fillWorkbookWithResults(session, allResults);
      setFilledWorkbook(wb);
      const s = summarizeCertResults(allResults);
      message.success(
        `查证完成：成功回填 ${s.verified} 人，无结果 ${s.notFound} 人` +
          (s.invalid || s.failed ? `，无效/失败 ${s.invalid + s.failed} 人` : ''),
      );
    } catch (err) {
      message.error(err instanceof Error ? err.message : '查证回填失败');
    } finally {
      setVerifying(false);
      setProgress(100);
    }
  };

  const handleDownload = async () => {
    if (!session || !filledWorkbook) return;
    setDownloading(true);
    try {
      await downloadFilledWorkbook(filledWorkbook, session.fileName);
      message.success('已开始下载回填后的 Excel');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '下载失败');
    } finally {
      setDownloading(false);
    }
  };

  const columns: ColumnsType<CertLookupRowResult> = [
    { title: '行号', dataIndex: 'rowIndex', width: 64, align: 'center' },
    { title: '姓名', dataIndex: 'name', width: 90 },
    {
      title: '身份证',
      dataIndex: 'idCard',
      width: 160,
      render: (v: string) => <span className="id-card-mask">{maskIdCard(v)}</span>,
    },
    {
      title: '查证状态',
      dataIndex: 'status',
      width: 90,
      render: (v: CertFillStatus) => {
        const meta = STATUS_TAG[v] ?? STATUS_TAG.pending;
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
    { title: '已有证书等级', dataIndex: 'certLevel', width: 120, render: (v) => v || '-' },
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

  return (
    <div className="personnel-cert-import-panel">
      <Paragraph type="secondary">
        上传人员 Excel（推荐「高命中率认定采集模板精简版」），系统自动识别<strong>姓名</strong>与<strong>身份证号码</strong>列，
        调用真实查证接口后将<strong>已有证书等级、证书编号、发证日期</strong>写回原文件对应行并下载。
        查证结果<strong>仅回填文件，不自动同步到企业调研</strong>；如需落库请到企业调研 → 人员采集中维护后再查证。
      </Paragraph>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="真实查证说明"
        description="需在系统字典配置 chazheng_url / chazheng_datas。人数较多时将自动分片（每批最多 20 人）调用查证接口；仅 status=已回填 的行会写入 Excel 对应列。"
      />

      <Card size="small" title="1. 导入 Excel" style={{ marginBottom: 16 }}>
        <Dragger {...uploadProps} disabled={parsing || verifying}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">点击或拖拽上传 .xlsx / .xls</p>
          <p className="ant-upload-hint">支持「采集表」Sheet；表头需含「姓名」「身份证号码」</p>
        </Dragger>
        {session && (
          <Descriptions size="small" column={1} style={{ marginTop: 16 }} bordered>
            <Descriptions.Item label="文件名">
              <Space>
                <FileExcelOutlined />
                {session.fileName}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="工作表">{session.sheetName}</Descriptions.Item>
            <Descriptions.Item label="表头行">第 {session.headerRowIndex + 1} 行</Descriptions.Item>
            <Descriptions.Item label="人员行数">{session.rows.length} 人</Descriptions.Item>
            <Descriptions.Item label="列映射">{columnHint}</Descriptions.Item>
          </Descriptions>
        )}
      </Card>

      <Card size="small" title="2. 查证并回填" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Button
            type="primary"
            icon={<SafetyCertificateOutlined />}
            disabled={!session || verifying}
            loading={verifying}
            onClick={() => void handleVerifyAndFill()}
          >
            开始查证回填
          </Button>
          <Button
            icon={<CloudDownloadOutlined />}
            disabled={!filledWorkbook}
            loading={downloading}
            onClick={() => void handleDownload()}
          >
            下载回填文件
          </Button>
          {session && (
            <Button
              disabled={verifying}
              onClick={() => {
                setSession(null);
                setResults([]);
                setFilledWorkbook(null);
                setProgress(0);
              }}
            >
              清空
            </Button>
          )}
        </Space>
        {verifying && (
          <div style={{ marginTop: 16 }}>
            <Text type="secondary">查证进度</Text>
            <Progress percent={progress} status="active" />
          </div>
        )}
        {results.length > 0 && (
          <Descriptions size="small" column={5} style={{ marginTop: 16 }} bordered>
            <Descriptions.Item label="总数">{summary.total}</Descriptions.Item>
            <Descriptions.Item label="成功回填">
              <Text type="success">{summary.verified}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="无结果">
              <Text type="warning">{summary.notFound}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="无效行">
              <Text type="danger">{summary.invalid}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="失败">
              <Text type="danger">{summary.failed}</Text>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Card>

      {results.length > 0 && (
        <Card size="small" title="3. 查证结果预览">
          <Table<CertLookupRowResult>
            rowKey={(r) => `${r.rowIndex}-${r.idCard}`}
            size="small"
            bordered
            columns={columns}
            dataSource={results}
            scroll={{ x: 980, y: 420 }}
            pagination={{ defaultPageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
          />
        </Card>
      )}
    </div>
  );
}
