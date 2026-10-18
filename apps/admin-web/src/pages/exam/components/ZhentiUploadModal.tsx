import { Alert, Button, Form, Input, Modal, Progress, Space, Typography, Upload, message } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import type { UploadFile } from 'antd';
import { uploadZhenti } from '../../../api/exam-zhenti';
import { extractErrorMessage, validatePdfFile } from '../pastPaperUtils';

interface Props {
  open: boolean;
  examId?: string;
  examName?: string;
  onCancel: () => void;
  onSuccess: () => void;
}

/** D2 单个上传：前端预检扩展名与 50MB，超限/类型不符不发起请求。 */
export function ZhentiUploadModal({ open, examId, examName, onCancel, onSuccess }: Props) {
  const [form] = Form.useForm<{ title?: string; year?: string }>();
  const [file, setFile] = useState<File | null>(null);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [progress, setProgress] = useState<number | null>(null);

  useEffect(() => {
    if (!open) {
      setFile(null);
      setFileList([]);
      setError(null);
      setProgress(null);
      form.resetFields();
    }
  }, [open, form]);

  const handleSelect = (candidate: File) => {
    const invalid = validatePdfFile(candidate);
    if (invalid) {
      setError(invalid);
      setFile(null);
      setFileList([]);
      return false;
    }
    setError(null);
    setFile(candidate);
    setFileList([{ uid: '-1', name: candidate.name, status: 'done', size: candidate.size }]);
    return false;
  };

  const handleSubmit = async () => {
    if (!examId) {
      setError('请先选择考试项目');
      return;
    }
    if (!file) {
      setError('请选择 PDF 文件');
      return;
    }
    const invalid = validatePdfFile(file);
    if (invalid) {
      setError(invalid);
      return;
    }
    const values = await form.validateFields();
    setError(null);
    setProgress(0);
    try {
      await uploadZhenti(
        { file, examId, title: values.title?.trim(), year: values.year?.trim() },
        setProgress,
      );
      message.success('上传成功');
      onSuccess();
    } catch (err) {
      setError(extractErrorMessage(err, '上传失败，请重试'));
    } finally {
      setProgress(null);
    }
  };

  return (
    <Modal
      open={open}
      title="上传真题 PDF"
      okText={progress === null ? '上传' : '上传中…'}
      cancelText="取消"
      confirmLoading={progress !== null}
      onCancel={onCancel}
      onOk={handleSubmit}
      destroyOnHidden
      width={560}
    >
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Typography.Text type="secondary">
          考试项目：<Typography.Text strong>{examName || '-'}</Typography.Text>
        </Typography.Text>
        <Upload
          accept=".pdf"
          maxCount={1}
          fileList={fileList}
          beforeUpload={(candidate) => handleSelect(candidate as File)}
          onRemove={() => {
            setFile(null);
            setFileList([]);
            return true;
          }}
        >
          <Button icon={<UploadOutlined />}>选择 PDF 文件</Button>
        </Upload>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          仅支持 PDF，单个文件不超过 50MB；文件名建议：工种（别名）等级科目-年份.pdf
        </Typography.Text>
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            label="标题"
            name="title"
            rules={[{ max: 200, message: '标题最多 200 字' }]}
            extra="留空按文件名解析"
          >
            <Input placeholder="例如：铝电解工（铝及铝合金熔铸工）五级理论科目" />
          </Form.Item>
          <Form.Item
            label="年份"
            name="year"
            rules={[{ max: 10, message: '年份最多 10 字' }]}
            extra="留空按文件名解析；解析不出会进入待完善队列"
          >
            <Input placeholder="例如：2024" />
          </Form.Item>
        </Form>
        {progress !== null && <Progress percent={progress} size="small" />}
        {error && <Alert type="error" showIcon message={error} />}
      </Space>
    </Modal>
  );
}
