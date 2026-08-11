import { InboxOutlined } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Modal, Space, Typography, Upload, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { getAssessorLedgerImport, startAssessorLedgerImport } from '../../../api/certificationInfo';
import { uploadPlatformFile } from '../../../api/files';

export function AssessorLedgerImportButton() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [batchId, setBatchId] = useState<string>();
  const notifiedStatus = useRef<string>();
  const batchQuery = useQuery({
    queryKey: ['assessor-ledger-import', batchId],
    queryFn: () => getAssessorLedgerImport(batchId!),
    enabled: Boolean(batchId),
    refetchInterval: (query) => ['queued', 'processing'].includes(query.state.data?.status || '') ? 1500 : false,
  });

  useEffect(() => {
    const status = batchQuery.data?.status;
    if (!status || notifiedStatus.current === status || !['completed', 'failed'].includes(status)) return;
    notifiedStatus.current = status;
    if (status === 'completed') {
      message.success('考评员台账导入完成');
      queryClient.invalidateQueries({ queryKey: ['certification-info-assessors'] });
    } else {
      message.error('考评员台账导入失败');
    }
  }, [batchQuery.data?.status, queryClient]);

  return (
    <>
      <Button icon={<InboxOutlined />} onClick={() => setOpen(true)}>导入台账</Button>
      <Modal title="导入考评员台账" open={open} onCancel={() => setOpen(false)} footer={null} destroyOnClose>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Typography.Text type="secondary">
            仅支持 .xlsx。姓名或证号已存在的记录会直接跳过；账号为姓名拼音首字母加身份证后五位，初始密码为 12345678。
          </Typography.Text>
          <Upload.Dragger
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            maxCount={1}
            disabled={Boolean(batchId && ['queued', 'processing'].includes(batchQuery.data?.status || ''))}
            beforeUpload={async (file) => {
              if (!file.name.toLowerCase().endsWith('.xlsx')) {
                message.warning('请选择 .xlsx 文件');
                return Upload.LIST_IGNORE;
              }
              try {
                const uploaded = await uploadPlatformFile(file, 'private', 15 * 60 * 1000);
                const batch = await startAssessorLedgerImport(String(uploaded.id));
                notifiedStatus.current = undefined;
                setBatchId(batch.id);
                message.info('台账已上传，正在后台导入');
              } catch {
                // apiClient has already shown the business error.
              }
              return Upload.LIST_IGNORE;
            }}
          >
            <p className="ant-upload-drag-icon"><InboxOutlined /></p>
            <p className="ant-upload-text">点击或拖拽上传考评员台账</p>
          </Upload.Dragger>
          {batchQuery.data ? (
            <Alert
              type={batchQuery.data.status === 'failed' ? 'error' : batchQuery.data.status === 'completed' ? 'success' : 'info'}
              message={`状态：${labelOf(batchQuery.data.status)}`}
              description={batchQuery.data.resultMessage || '正在读取台账…'}
              showIcon
            />
          ) : null}
        </Space>
      </Modal>
    </>
  );
}

function labelOf(status: string) {
  return { queued: '排队中', processing: '导入中', completed: '已完成', failed: '失败' }[status] || status;
}
