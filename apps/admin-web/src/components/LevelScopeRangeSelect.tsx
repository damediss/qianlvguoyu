import { Select, Space } from 'antd';
import { useMemo } from 'react';

const LEVELS = ['初级', '中级', '高级', '技师', '高级技师'] as const;

type Level = (typeof LEVELS)[number];

export interface LevelScopeRangeSelectProps {
  value?: string;
  onChange?: (value?: string) => void;
  placeholderStart?: string;
  placeholderEnd?: string;
  allowClear?: boolean;
}

function normalizeLevel(level?: string): Level | undefined {
  return (LEVELS as readonly string[]).includes(level || '') ? (level as Level) : undefined;
}

function parseLevelScope(value?: string): { start?: Level; end?: Level } {
  const raw = (value || '').trim();
  if (!raw) return {};

  // 支持既有数据：`初级->高级` 或 `初级,中级,高级`
  if (raw.includes('->')) {
    const [a, b] = raw.split('->').map((s) => s.trim());
    return { start: normalizeLevel(a), end: normalizeLevel(b) };
  }

  const parts = raw.split(/[，,、\s]+/g).map((s) => s.trim()).filter(Boolean);
  const normalized = parts.map(normalizeLevel).filter(Boolean) as Level[];
  if (normalized.length === 0) return {};

  const indexes = normalized.map((l) => LEVELS.indexOf(l)).filter((i) => i >= 0).sort((x, y) => x - y);
  return {
    start: LEVELS[indexes[0]],
    end: LEVELS[indexes[indexes.length - 1]],
  };
}

function formatLevelScope(start?: Level, end?: Level) {
  if (!start && !end) return undefined;
  const s = start || end;
  const e = end || start;
  if (!s || !e) return undefined;
  return `${s}->${e}`;
}

export function LevelScopeRangeSelect({
  value,
  onChange,
  placeholderStart = '起始等级',
  placeholderEnd = '结束等级',
  allowClear = true,
}: LevelScopeRangeSelectProps) {
  const parsed = useMemo(() => parseLevelScope(value), [value]);
  const start = parsed.start;
  const end = parsed.end;

  const startOptions = useMemo(() => LEVELS.map((l) => ({ label: l, value: l })), []);
  const endOptions = useMemo(() => {
    const startIndex = start ? LEVELS.indexOf(start) : 0;
    return LEVELS.slice(Math.max(startIndex, 0)).map((l) => ({ label: l, value: l }));
  }, [start]);

  const emit = (nextStart?: Level, nextEnd?: Level) => {
    const normalized = formatLevelScope(nextStart, nextEnd);
    onChange?.(normalized);
  };

  return (
    <Space.Compact style={{ width: '100%' }}>
      <Select<Level>
        value={start}
        allowClear={allowClear}
        placeholder={placeholderStart}
        options={startOptions}
        onChange={(v) => {
          const nextStart = v as Level | undefined;
          const nextEnd = end && nextStart && LEVELS.indexOf(end) < LEVELS.indexOf(nextStart) ? nextStart : end;
          emit(nextStart, nextEnd);
        }}
        style={{ width: '50%' }}
      />
      <Select<Level>
        value={end}
        allowClear={allowClear}
        placeholder={placeholderEnd}
        options={endOptions}
        onChange={(v) => emit(start, v as Level | undefined)}
        style={{ width: '50%' }}
      />
    </Space.Compact>
  );
}

