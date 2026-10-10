import { Button, Form, Input, Modal, Select } from 'antd';
import type {
  CollectionExceptionType,
} from '../../../types/certificationPracticalCollection';

interface ExceptionReleaseModalProps {
  open: boolean;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: {
    exceptionType: CollectionExceptionType;
    reason: string;
  }) => void;
}

export function ExceptionReleaseModal({ open, loading, onCancel, onSubmit }: ExceptionReleaseModalProps) {
  const [form] = Form.useForm<{
    exceptionType: CollectionExceptionType;
    reason: string;
  }>();

  return (
    <Modal
      title="免考放行"
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>取消</Button>,
        <Button
          key="submit"
          type="primary"
          loading={loading}
          onClick={() => {
            form.validateFields().then((values) => {
              Modal.confirm({
                title: '确认免考放行？',
                content: '免考放行后，该考生采集状态将变为已放行，并写入审计记录。',
                okText: '确认放行',
                cancelText: '取消',
                onOk: () => onSubmit(values),
              });
            });
          }}
        >
          免考放行
        </Button>,
      ]}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{ exceptionType: 'OTHER' }}
      >
        <Form.Item name="exceptionType" label="放行类型" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'DEVICE_ERROR', label: '设备异常' },
              { value: 'FINGERPRINT_UNAVAILABLE', label: '无法采集指纹' },
              { value: 'SIGNATURE_UNAVAILABLE', label: '无法签名' },
              { value: 'INFO_ERROR', label: '信息异常' },
              { value: 'OTHER', label: '其他' },
            ]}
          />
        </Form.Item>
        <Form.Item name="reason" label="放行原因" rules={[{ required: true, message: '请填写免考放行原因' }]}>
          <Input.TextArea rows={4} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </Modal>
  );
}
