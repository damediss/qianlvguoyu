import { Button, Space, Typography } from 'antd';

const { Text } = Typography;

export interface BatchActionItem {
  key: string;
  label: string;
  danger?: boolean;
  onClick: () => void;
}

export interface BatchActionBarProps {
  /** 已选条目数,组件不感知是否 > 0,由父组件控制 v-if */
  count: number;
  /** 取消选择按钮回调 */
  onClear: () => void;
  /** 可执行的动作(目前 v1 只有批量删除) */
  actions: BatchActionItem[];
}

/**
 * 批量操作条:有选中行时出现在表格上方。
 *
 * 视觉:浅蓝底 (#e6f4ff) + 虚线蓝色下边线。
 * 父组件需要在 count > 0 时才挂载(组件本身只负责视觉呈现)。
 */
export function BatchActionBar({ count, onClear, actions }: BatchActionBarProps) {
  return (
    <div
      style={{
        background: '#e6f4ff',
        border: '1px dashed #91caff',
        borderRadius: 6,
        padding: '8px 12px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 8,
      }}
    >
      <Space size="middle" wrap>
        <Text>
          已选 <Text strong style={{ color: '#1677ff' }}>{count}</Text> 题
        </Text>
        {actions.map((a, idx) => (
          <Space key={a.key} size={4}>
            {idx > 0 && <Text type="secondary">|</Text>}
            <Button
              type="link"
              size="small"
              danger={a.danger}
              onClick={a.onClick}
              style={{ padding: 0 }}
            >
              {a.label}
            </Button>
          </Space>
        ))}
      </Space>
      <Button type="link" size="small" onClick={onClear}>
        取消选择
      </Button>
    </div>
  );
}