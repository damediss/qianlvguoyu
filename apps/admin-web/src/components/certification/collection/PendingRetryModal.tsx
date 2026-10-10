import { Button, Form, Input, Modal } from 'antd';

interface PendingRetryModalProps {
  open: boolean;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (reason: string) => void;
}

export function PendingRetryModal({ open, loading, onCancel, onSubmit }: PendingRetryModalProps) {
  const [form] = Form.useForm<{ reason: string }>();

  return (
    <Modal
      title="标记待补采"
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>取消</Button>,
        <Button
          key="submit"
          type="primary"
          loading={loading}
          onClick={() => form.validateFields().then((values) => onSubmit(values.reason))}
        >
          确认标记
        </Button>,
      ]}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="reason" label="待补采原因" rules={[{ required: true, message: '请填写待补采原因' }]}>
          <Input.TextArea rows={4} maxLength={500} showCount />
        </Form.Item>
      </Form>
    </Modal>
  );
}
