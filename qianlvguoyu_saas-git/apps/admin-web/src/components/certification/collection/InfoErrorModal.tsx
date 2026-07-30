import { Button, Form, Input, Modal } from 'antd';

interface InfoErrorModalProps {
  open: boolean;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (reason: string) => void;
}

export function InfoErrorModal({ open, loading, onCancel, onSubmit }: InfoErrorModalProps) {
  const [form] = Form.useForm<{ reason: string }>();

  return (
    <Modal
      title="标记信息异常"
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>取消</Button>,
        <Button
          key="submit"
          danger
          type="primary"
          loading={loading}
          onClick={() => form.validateFields().then((values) => onSubmit(values.reason))}
        >
          确认异常
        </Button>,
      ]}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="reason" label="信息异常原因" rules={[{ required: true, message: '请填写信息异常原因' }]}>
          <Input.TextArea rows={4} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </Modal>
  );
}
