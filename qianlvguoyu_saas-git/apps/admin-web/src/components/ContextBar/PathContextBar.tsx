import { CloseOutlined, EnvironmentOutlined, FilterOutlined, BarChartOutlined } from '@ant-design/icons';
import { Card, Space, Tag, Typography } from 'antd';
import type { ReactNode } from 'react';

const { Text } = Typography;

/**
 * 当前路径摘要 + 题目统计 + 筛选 chip 一体化展示条。
 *
 * - 左侧:路径 chip(工种 › 课程 › 知识点)
 * - 中段:统计(共 X 题 · 内部 · 外部 · 已选)
 * - 右侧:筛选 chip(可移除)
 *
 * 当无任何路径/筛选/统计时,组件会渲染一个空白占位行,
 * 调用方可以靠 v-if 控制是否显示。
 */
export interface PathContextBarProps {
  breadcrumb: { workType?: string; course?: string; knowledge?: string };
  stats: { total: number; internal: number; external: number; selected: number };
  filters: { source?: string; type?: string; difficulty?: string };
  onFilterRemove: (key: 'source' | 'type' | 'difficulty') => void;
}

const SOURCE_LABEL: Record<string, string> = {
  '0': '内部',
  '1': '外部',
};

function renderChip(
  label: string,
  value: string,
  onRemove?: () => void,
): ReactNode {
  const inner = (
    <span>
      {label}: <Text strong>{value}</Text>
    </span>
  );
  if (!onRemove) return <Tag>{inner}</Tag>;
  return (
    <Tag closable onClose={onRemove} closeIcon={<CloseOutlined />}>
      {inner}
    </Tag>
  );
}

export function PathContextBar({ breadcrumb, stats, filters, onFilterRemove }: PathContextBarProps) {
  const pathNodes = [breadcrumb.workType, breadcrumb.course, breadcrumb.knowledge].filter(
    (s): s is string => Boolean(s),
  );

  const sourceLabel = filters.source !== undefined ? SOURCE_LABEL[String(filters.source)] ?? String(filters.source) : undefined;
  const typeLabel = filters.type ?? undefined;
  const difficultyLabel = filters.difficulty ?? undefined;

  const hasAnything =
    pathNodes.length > 0
    || stats.total > 0
    || filters.source !== undefined
    || filters.type !== undefined
    || filters.difficulty !== undefined;

  if (!hasAnything) return null;

  return (
    <Card size="small" style={{ background: '#fafafa' }}>
      <Space size="middle" wrap>
        {/* 路径 chip */}
        {pathNodes.length > 0 && (
          <Space size={4} wrap>
            <Text type="secondary">
              <EnvironmentOutlined /> 当前范围:
            </Text>
            {pathNodes.map((node, idx) => {
              const isLast = idx === pathNodes.length - 1;
              return (
                <Space key={`${node}-${idx}`} size={4}>
                  <Tag color={isLast ? 'blue' : 'default'}>{node}</Tag>
                  {!isLast && <Text type="secondary">›</Text>}
                </Space>
              );
            })}
          </Space>
        )}

        {/* 分隔 */}
        {pathNodes.length > 0 && <Divider />}

        {/* 统计 */}
        <Space size="small">
          <Text type="secondary">
            <BarChartOutlined /> 题目统计
          </Text>
          <Text>
            共 <Text strong>{stats.total}</Text> 题 · 内部{' '}
            <Text strong>{stats.internal}</Text> · 外部{' '}
            <Text strong>{stats.external}</Text> · 已选{' '}
            <Text strong style={{ color: stats.selected > 0 ? '#1677ff' : undefined }}>
              {stats.selected}
            </Text>
          </Text>
        </Space>

        {/* 分隔 */}
        {(filters.source !== undefined || filters.type !== undefined || filters.difficulty !== undefined) && (
          <Divider />
        )}

        {/* 筛选 chip */}
        {(filters.source !== undefined
          || filters.type !== undefined
          || filters.difficulty !== undefined) && (
          <Space size={4} wrap>
            <Text type="secondary">
              <FilterOutlined /> 筛选
            </Text>
            {filters.source !== undefined && sourceLabel
              ? renderChip('来源', sourceLabel, () => onFilterRemove('source'))
              : null}
            {filters.type !== undefined && typeLabel
              ? renderChip('题型', typeLabel, () => onFilterRemove('type'))
              : null}
            {filters.difficulty !== undefined && difficultyLabel
              ? renderChip('难度', difficultyLabel, () => onFilterRemove('difficulty'))
              : null}
          </Space>
        )}
      </Space>
    </Card>
  );
}

function Divider() {
  return (
    <span
      aria-hidden
      style={{
        display: 'inline-block',
        width: 1,
        height: 18,
        background: '#f0f0f0',
        margin: '0 4px',
      }}
    />
  );
}