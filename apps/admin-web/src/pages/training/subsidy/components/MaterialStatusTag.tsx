import { Tag } from 'antd';

const STATUS_MAP: Record<string, { color: string; label: string }> = {
  pending: { color: 'default', label: '未生成' },
  filled: { color: 'processing', label: '已填待审' },
  confirmed: { color: 'success', label: '已确认' },
  upload_only: { color: 'blue', label: '仅上传' },
  from_class: { color: 'cyan', label: '来自本班' },
};

type Props = {
  status?: string | null;
};

export function MaterialStatusTag({ status }: Props) {
  const cfg = STATUS_MAP[status || ''] || { color: 'default', label: status || '—' };
  return <Tag color={cfg.color}>{cfg.label}</Tag>;
}
