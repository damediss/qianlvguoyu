import { Button, Form, Input, Modal, Select } from 'antd';
import type {
  CollectionExceptionType,
  CollectionReleaseScope,
} from '../../../types/certificationPracticalCollection';

interface ExceptionReleaseModalProps {
  open: boolean;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: {
    exceptionType: CollectionExceptionType;
    releaseScope: CollectionReleaseScope;
    reason: string;
  }) => void;
}

export function ExceptionReleaseModal({ open, loading, onCancel, onSubmit }: ExceptionReleaseModalProps) {
  const [form] = Form.useForm<{
    exceptionType: CollectionExceptionType;
    releaseScope: CollectionReleaseScope;
    reason: string;
  }>();

  return (
    <Modal
      title="异常放行"
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>取消</Button>,
        <Button
          key="submit"
          danger
          type="primary"
          loading={loading}
          onClick={() => {
            form.validateFields().then((values) => {
              Modal.confirm({
                title: '确认异常放行？',
                content: '异常放行会写入审计和异常记录，请确认现场已完成核验。',
                okText: '确认放行',
                okButtonProps: { danger: true },
                cancelText: '取消',
                onOk: () => onSubmit(values),
              });
            });
          }}
        >
          异常放行
        </Button>,
      ]}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{ exceptionType: 'OTHER', releaseScope: 'FINGERPRINT' }}
      >
        <Form.Item name="exceptionType" label="异常类型" rules={[{ required: true }]}>
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
        <Form.Item name="releaseScope" label="放行范围" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'FINGERPRINT', label: '指纹' },
              { value: 'SIGNATURE', label: '签名' },
              { value: 'ALL', label: '全部' },
              { value: 'INFO', label: '信息' },
              { value: 'OTHER', label: '其他' },
            ]}
          />
        </Form.Item>
        <Form.Item name="reason" label="放行原因" rules={[{ required: true, message: '请填写异常放行原因' }]}>
          <Input.TextArea rows={4} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </Modal>
  );
}
