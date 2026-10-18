import { Form, Input, InputNumber, Modal, message } from 'antd';
import { useEffect } from 'react';
import { updateZhenti } from '../../../api/exam-zhenti';
import type { ZhentiItem } from '../../../types/examZhenti';

interface Props {
  open: boolean;
  record: ZhentiItem | null;
  focusField?: 'title' | 'year';
  onCancel: () => void;
  onSuccess: () => void;
}

/** 重命名 / 改年份：一次提交只改发生变化的字段（D4 局部更新）。 */
export function ZhentiEditModal({ open, record, focusField, onCancel, onSuccess }: Props) {
  const [form] = Form.useForm<{ title: string; year?: string; sort: number }>();

  useEffect(() => {
    if (open && record) {
      form.setFieldsValue({
        title: record.title,
        year: record.year ?? '',
        sort: record.sort ?? 0,
      });
    }
  }, [open, record, form]);

  const handleOk = async () => {
    if (!record) return;
    const values = await form.validateFields();
    await updateZhenti({
      id: record.id,
      title: values.title.trim(),
      year: values.year?.trim() ?? '',
      sort: values.sort,
    });
    message.success('已保存');
    onSuccess();
  };

  return (
    <Modal
      open={open}
      title="编辑真题信息"
      okText="保存"
      cancelText="取消"
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnHidden
      afterOpenChange={(visible) => {
        if (visible && focusField) {
          form.getFieldInstance(focusField === 'title' ? 'title' : 'year')?.focus?.();
        }
      }}
    >
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          label="标题"
          name="title"
          rules={[
            { required: true, message: '请输入标题' },
            { max: 200, message: '标题最多 200 字' },
          ]}
        >
          <Input placeholder="例如：铝电解工（铝及铝合金熔铸工）五级理论科目" />
        </Form.Item>
        <Form.Item
          label="年份"
          name="year"
          rules={[{ max: 10, message: '年份最多 10 字' }]}
          extra="留空表示尚未补齐，仍会留在待完善队列"
        >
          <Input placeholder="例如：2024" />
        </Form.Item>
        <Form.Item label="排序" name="sort" extra="数字越小越靠前">
          <InputNumber min={0} max={9999} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
