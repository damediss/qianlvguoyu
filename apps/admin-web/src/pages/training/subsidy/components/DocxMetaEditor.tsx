import { useEffect, useState } from 'react';
import { Alert, Button, Form, Input, Space, Spin, Typography, message } from 'antd';
import * as trainApi from '../../../../api/training';
import type { SubsidyMaterialDocxMeta } from '../../../../api/training';

type Props = {
  instanceId: string | number;
  onSaved?: () => void;
  onCancel?: () => void;
  /** 仅当 OnlyOffice 已启用时传入 */
  onOpenFullEditor?: () => void;
  onlyOfficeAvailable?: boolean;
};

function isSameCellLabeled(line: { label: string; text: string; kind: string }) {
  if (line.kind !== 'meta') return false;
  const t = (line.text || '').trim();
  return t.startsWith(line.label);
}

export function DocxMetaEditor({
  instanceId,
  onSaved,
  onCancel,
  onOpenFullEditor,
  onlyOfficeAvailable = false,
}: Props) {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [meta, setMeta] = useState<SubsidyMaterialDocxMeta | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [form] = Form.useForm<Record<string, string>>();

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await trainApi.getSubsidyMaterialDocxMeta(instanceId);
        if (cancelled) return;
        setMeta(data);
        const values: Record<string, string> = {};
        for (const line of data.lines || []) {
          values[`p_${line.index}`] =
            line.kind === 'meta' || line.kind === 'caption' ? line.value : line.text;
        }
        form.setFieldsValue(values);
      } catch (e) {
        if (!cancelled) {
          setError((e as Error).message || '加载 Word 字段失败');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [instanceId, form]);

  const handleSave = async () => {
    if (!meta?.lines?.length) {
      message.warning('没有可保存的字段');
      return;
    }
    const values = await form.validateFields();
    const paragraphs: Record<number, string> = {};
    for (const line of meta.lines) {
      const raw = values[`p_${line.index}`] ?? '';
      if (line.kind === 'meta') {
        paragraphs[line.index] = isSameCellLabeled(line)
          ? `${line.label}：${raw}`
          : String(raw);
      } else if (line.kind === 'caption') {
        const t = String(raw).trim();
        paragraphs[line.index] = t.startsWith('图') ? t : `${line.label}：${t}`;
      } else {
        paragraphs[line.index] = String(raw);
      }
    }
    setSaving(true);
    try {
      await trainApi.patchSubsidyMaterialDocx(instanceId, paragraphs);
      message.success('已保存 Word（保留图片与版式）');
      onSaved?.();
    } catch (e) {
      message.error((e as Error).message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin tip="解析 Word 字段…" />
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        message="无法解析 Word 字段"
        description={
          <div>
            <div>{error}</div>
            <div style={{ marginTop: 8 }}>请用「下载」修改后「替换上传」，或检查后端是否已部署最新填数/编辑接口。</div>
            {onCancel ? (
              <Button type="link" style={{ paddingLeft: 0 }} onClick={onCancel}>
                退出编辑
              </Button>
            ) : null}
          </div>
        }
      />
    );
  }

  if (!meta?.lines?.length) {
    return (
      <Alert
        type="info"
        showIcon
        message="未识别到可编辑字段"
        description={
          <div>
            <div>
              当前文档未匹配到「培训机构 / 培训时间 / 经办人」等栏位。请「下载」后本地修改，再「替换上传」。
            </div>
            {onlyOfficeAvailable && onOpenFullEditor ? (
              <Button type="link" style={{ paddingLeft: 0 }} onClick={onOpenFullEditor}>
                改用完整排版编辑（OnlyOffice）
              </Button>
            ) : null}
            {onCancel ? (
              <Button type="link" onClick={onCancel}>
                退出编辑
              </Button>
            ) : null}
          </div>
        }
      />
    );
  }

  return (
    <div className="material-docx-meta-editor" style={{ padding: '12px 16px' }}>
      <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
        字段编辑（OnlyOffice 不可用或无需改版式时使用）。修改后保存即可写回 docx，保留图片与表格。
        {meta?.fileName ? (
          <>
            {' '}
            文件：<Typography.Text code>{meta.fileName}</Typography.Text>
          </>
        ) : null}
        {onlyOfficeAvailable && onOpenFullEditor ? (
          <>
            {' '}
            <Button type="link" size="small" onClick={onOpenFullEditor}>
              改用完整排版编辑（OnlyOffice）
            </Button>
          </>
        ) : null}
      </Typography.Paragraph>
      <Form form={form} layout="vertical" size="middle">
        {(meta?.lines || []).map((line) => (
          <Form.Item
            key={line.index}
            name={`p_${line.index}`}
            label={line.kind === 'title' ? '标题' : line.kind === 'caption' ? line.label : line.label}
          >
            {line.kind === 'caption' || (line.value && line.value.length > 40) ? (
              <Input.TextArea rows={2} />
            ) : (
              <Input />
            )}
          </Form.Item>
        ))}
      </Form>
      <Space>
        <Button type="primary" loading={saving} onClick={() => void handleSave()}>
          保存
        </Button>
        {onCancel ? (
          <Button disabled={saving} onClick={onCancel}>
            取消
          </Button>
        ) : null}
      </Space>
    </div>
  );
}
