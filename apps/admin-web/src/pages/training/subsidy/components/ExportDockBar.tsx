import { Button, Space, Typography } from 'antd';
import { CloudDownloadOutlined, ExportOutlined } from '@ant-design/icons';

type Props = {
  total: number;
  selectedCount: number;
  exporting?: boolean;
  lastPackagePath?: string | null;
  onExport: () => void;
  onDownloadLast?: () => void;
};

export function ExportDockBar({
  total,
  selectedCount,
  exporting,
  lastPackagePath,
  onExport,
  onDownloadLast,
}: Props) {
  return (
    <div className="subsidy-export-dock">
      <Space wrap>
        <Typography.Text>
          导出已选 <b>{selectedCount}</b> / {total} 项
        </Typography.Text>
        <Button
          type="primary"
          icon={<ExportOutlined />}
          loading={exporting}
          disabled={selectedCount === 0}
          onClick={onExport}
        >
          按勾选导出 ZIP
        </Button>
        {lastPackagePath && onDownloadLast ? (
          <Button icon={<CloudDownloadOutlined />} onClick={onDownloadLast}>
            下载上次材料包
          </Button>
        ) : null}
      </Space>
    </div>
  );
}
