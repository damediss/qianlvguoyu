import { useState } from 'react';
import { Alert, Button, Checkbox, Modal, Space, Typography, Upload, message } from 'antd';
import { DownloadOutlined, InboxOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import { importExamDetails, validateExamDetailsImport } from '../../api/exam';
import type { QuestionImportValidateResult } from '../../types/questionImport';

const { Text } = Typography;

interface Props {
  open: boolean;
  courseId: number | null | undefined;
  source: number;
  sourceLabel?: string;
  onCancel: () => void;
}

/**
 * 主题库导入弹窗（方案 §14.4.3）：先只读预检，hard error 禁止导入，
 * warning 必须勾选已知悉；正式导入携带预检文件哈希，防止 A 检 B 导。
 */
const QuestionImportModal: React.FC<Props> = ({
  open,
  courseId,
  source,
  sourceLabel,
  onCancel,
}) => {
  const queryClient = useQueryClient();
  const [fileList, setFileList] = useState<any[]>([]);
  const [validation, setValidation] = useState<QuestionImportValidateResult | null>(null);
  const [acked, setAcked] = useState(false);
  const [validating, setValidating] = useState(false);
  const [importing, setImporting] = useState(false);

  const reset = () => {
    setFileList([]);
    setValidation(null);
    setAcked(false);
  };

  const close = () => {
    reset();
    onCancel();
  };

  const validate = async () => {
    if (!courseId) {
      message.warning('请先选择科目');
      return;
    }
    if (fileList.length === 0) {
      message.warning('请选择要导入的文件');
      return;
    }
    const formData = new FormData();
    formData.append('file', fileList[0].originFileObj as Blob);
    formData.append('courseId', String(courseId));
    setValidating(true);
    try {
      const result = await validateExamDetailsImport(formData);
      setValidation(result);
      setAcked(result.warningCount === 0);
      if (result.hardErrorCount > 0) {
        message.error(`发现 ${result.hardErrorCount} 处格式错误，修正后才能导入`);
      } else if (result.warningCount > 0) {
        message.warning(`发现 ${result.warningCount} 条格式提醒，请核对后勾选确认`);
      } else {
        message.success('文件格式检查通过');
      }
    } catch (error: any) {
      message.error(error?.message || '文件格式检查失败');
    } finally {
      setValidating(false);
    }
  };

  const submit = async () => {
    if (!courseId) {
      message.warning('请先选择科目');
      return;
    }
    if (fileList.length === 0) {
      message.warning('请选择要导入的文件');
      return;
    }
    const formData = new FormData();
    formData.append('file', fileList[0].originFileObj as Blob);
    formData.append('courseId', String(courseId));
    formData.append('source', String(source));
    if (validation?.fileSha256) {
      formData.append('validationFileSha256', validation.fileSha256);
    }
    setImporting(true);
    try {
      const result = await importExamDetails(formData);
      message.success(
        `成功导入 ${result.importedCount} 道试题`
          + (result.warningCount > 0 ? `，另有 ${result.warningCount} 条格式提醒` : ''),
      );
      queryClient.invalidateQueries({ queryKey: ['examDetails'] });
      queryClient.invalidateQueries({ queryKey: ['exam', 'question-bank-tree'] });
      reset();
      onCancel();
    } catch (error: any) {
      message.error(error?.message || '导入失败');
    } finally {
      setImporting(false);
    }
  };

  const blocked = !!validation
    && (validation.hardErrorCount > 0 || (validation.warningCount > 0 && !acked));

  return (
    <Modal
      title={`导入${sourceLabel ?? ''}试题`}
      open={open}
      onOk={submit}
      onCancel={close}
      confirmLoading={importing}
      okText="开始导入"
      okButtonProps={{ disabled: blocked }}
      destroyOnHidden
    >
      <Upload.Dragger
        accept=".xls,.xlsx"
        maxCount={1}
        fileList={fileList}
        beforeUpload={() => false}
        onChange={({ fileList: next }) => {
          setFileList(next);
          setValidation(null);
          setAcked(false);
        }}
      >
        <p style={{ fontSize: 40, color: '#1677ff', marginBottom: 8 }}>
          <InboxOutlined />
        </p>
        <p style={{ fontSize: 14 }}>点击或将文件拖拽到这里上传</p>
        <p style={{ color: '#999', fontSize: 12 }}>支持 .xls / .xlsx 格式</p>
      </Upload.Dragger>
      <div style={{ marginTop: 12 }}>
        <a href="/templates/exam-import-template.xlsx" download="试题导入模板-命题.xlsx">
          <DownloadOutlined /> 下载导入模板
        </a>
      </div>
      <Space style={{ marginTop: 12 }}>
        <Button onClick={validate} loading={validating}>
          检查文件格式
        </Button>
        <Text type="secondary">先检查可提前发现数值截断、错误值等问题</Text>
      </Space>
      {validation && (
        <div style={{ marginTop: 12 }}>
          <Alert
            type={validation.hardErrorCount > 0 ? 'error' : 'info'}
            showIcon
            message={`共 ${validation.totalRows} 行，可导入 ${validation.validRows} 行；`
              + `格式错误 ${validation.hardErrorCount} 处，提醒 ${validation.warningCount} 条`}
          />
          {validation.errors.slice(0, 10).map((issue, index) => (
            <div key={`err-${index}`} style={{ marginTop: 6, color: '#cf1322' }}>
              第 {issue.row} 行第 {issue.column} 列（{issue.field}）：{issue.message}
            </div>
          ))}
          {validation.warnings.slice(0, 20).map((issue, index) => (
            <div key={`warn-${index}`} style={{ marginTop: 6, color: '#ad6800' }}>
              第 {issue.row} 行第 {issue.column} 列（{issue.field}）：{issue.message}
            </div>
          ))}
          {validation.truncated && (
            <Text type="secondary">仅展示前若干条，完整数量见上方统计。</Text>
          )}
          {validation.warningCount > 0 && validation.hardErrorCount === 0 && (
            <Checkbox
              style={{ marginTop: 8, display: 'block' }}
              checked={acked}
              onChange={(event) => setAcked(event.target.checked)}
            >
              已查看以上格式提醒，继续导入
            </Checkbox>
          )}
        </div>
      )}
    </Modal>
  );
};

export default QuestionImportModal;
