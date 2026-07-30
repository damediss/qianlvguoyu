import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import {
  Button,
  Checkbox,
  Collapse,
  Input,
  Select,
  Space,
  Tag,
  Typography,
} from 'antd';
import { useMemo, useState } from 'react';
import {
  ALL_COLLECTION_FIELDS,
  DEFAULT_COLLECTION_GROUPS,
  type ResearchCollectionFieldOptions,
  normalizeCollectionFieldOptions,
} from './researchCollectionFieldOptions';

const { Text } = Typography;

export interface ResearchCollectionFieldOptionsEditorProps {
  value?: ResearchCollectionFieldOptions;
  onChange?: (value: ResearchCollectionFieldOptions) => void;
}

export function ResearchCollectionFieldOptionsEditor({
  value,
  onChange,
}: ResearchCollectionFieldOptionsEditorProps) {
  const normalized = useMemo(() => normalizeCollectionFieldOptions(value), [value]);
  const fields = normalized.fields ?? {};
  const groups = normalized.groups ?? DEFAULT_COLLECTION_GROUPS;
  const [draftOption, setDraftOption] = useState<Record<string, string>>({});

  const emit = (next: ResearchCollectionFieldOptions) => {
    onChange?.(normalizeCollectionFieldOptions(next));
  };

  const patchField = (key: string, patch: Partial<(typeof fields)[string]>) => {
    emit({
      ...normalized,
      fields: {
        ...fields,
        [key]: {
          ...fields[key],
          ...patch,
        },
      },
    });
  };

  const moveGroup = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= groups.length) return;
    const nextGroups = groups.map((g) => ({ ...g }));
    const currentOrder = nextGroups[index].order;
    nextGroups[index].order = nextGroups[target].order;
    nextGroups[target].order = currentOrder;
    nextGroups.sort((a, b) => a.order - b.order);
    emit({ ...normalized, groups: nextGroups });
  };

  const moveField = (groupKey: string, fieldKey: string, direction: -1 | 1) => {
    const groupFields = ALL_COLLECTION_FIELDS
      .filter((def) => (fields[def.key]?.group || def.group) === groupKey)
      .map((def) => def.key);
    const sorted = [...groupFields].sort(
      (a, b) => (fields[a]?.order ?? 0) - (fields[b]?.order ?? 0),
    );
    const index = sorted.indexOf(fieldKey);
    const target = index + direction;
    if (index < 0 || target < 0 || target >= sorted.length) return;
    const a = sorted[index];
    const b = sorted[target];
    const orderA = fields[a]?.order ?? 0;
    const orderB = fields[b]?.order ?? 0;
    emit({
      ...normalized,
      fields: {
        ...fields,
        [a]: { ...fields[a], order: orderB },
        [b]: { ...fields[b], order: orderA },
      },
    });
  };

  const addOption = (key: string) => {
    const text = (draftOption[key] ?? '').trim();
    if (!text) return;
    const current = fields[key]?.options ?? [];
    if (current.includes(text)) {
      setDraftOption((prev) => ({ ...prev, [key]: '' }));
      return;
    }
    patchField(key, { options: [...current, text], inputType: 'select' });
    setDraftOption((prev) => ({ ...prev, [key]: '' }));
  };

  const removeOption = (key: string, option: string) => {
    const current = fields[key]?.options ?? [];
    patchField(key, { options: current.filter((item) => item !== option) });
  };

  return (
    <Collapse
      ghost
      defaultActiveKey={['field-options']}
      items={[
        {
          key: 'field-options',
          label: '人员采集字段配置（全部字段）',
          children: (
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Text type="secondary">
                可配置显示/隐藏、必填、标签、输入类型、下拉选项，并调整分组与字段顺序。开启采集后配置会快照到当前批次。
              </Text>
              {groups.map((group, groupIndex) => {
                const groupFields = ALL_COLLECTION_FIELDS.filter(
                  (def) => (fields[def.key]?.group || def.group) === group.key,
                ).sort(
                  (a, b) => (fields[a.key]?.order ?? a.order) - (fields[b.key]?.order ?? b.order),
                );
                return (
                  <div
                    key={group.key}
                    style={{
                      border: '1px solid #e8e8e8',
                      borderRadius: 8,
                      padding: 12,
                      background: '#fff',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                      <Input
                        value={group.label}
                        onChange={(e) => {
                          const nextGroups = groups.map((g) =>
                            g.key === group.key ? { ...g, label: e.target.value } : g,
                          );
                          emit({ ...normalized, groups: nextGroups });
                        }}
                        style={{ maxWidth: 260, fontWeight: 600 }}
                      />
                      <Space>
                        <Button
                          size="small"
                          icon={<ArrowUpOutlined />}
                          disabled={groupIndex === 0}
                          onClick={() => moveGroup(groupIndex, -1)}
                        />
                        <Button
                          size="small"
                          icon={<ArrowDownOutlined />}
                          disabled={groupIndex === groups.length - 1}
                          onClick={() => moveGroup(groupIndex, 1)}
                        />
                      </Space>
                    </div>
                    {groupFields.map((def, fieldIndex) => {
                      const config = fields[def.key];
                      const options = config?.options ?? [];
                      const inputType = config?.inputType || def.inputType;
                      return (
                        <div
                          key={def.key}
                          style={{
                            border: '1px solid #f0f0f0',
                            borderRadius: 8,
                            padding: 12,
                            marginBottom: 10,
                            background: '#fafafa',
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 8 }}>
                            <Input
                              value={config?.label || def.label}
                              disabled={def.locked}
                              onChange={(e) => patchField(def.key, { label: e.target.value })}
                              style={{ flex: 1 }}
                            />
                            <Space>
                              <Button
                                size="small"
                                icon={<ArrowUpOutlined />}
                                disabled={fieldIndex === 0}
                                onClick={() => moveField(group.key, def.key, -1)}
                              />
                              <Button
                                size="small"
                                icon={<ArrowDownOutlined />}
                                disabled={fieldIndex === groupFields.length - 1}
                                onClick={() => moveField(group.key, def.key, 1)}
                              />
                            </Space>
                          </div>
                          <Space wrap style={{ marginBottom: 8 }}>
                            <Select
                              size="small"
                              value={inputType}
                              disabled={def.locked}
                              style={{ width: 110 }}
                              options={[
                                { value: 'text', label: '文本' },
                                { value: 'select', label: '下拉' },
                                { value: 'date', label: '日期' },
                              ]}
                              onChange={(v) => patchField(def.key, { inputType: v })}
                            />
                            <Checkbox
                              checked={def.locked ? true : config?.visible !== false}
                              disabled={def.locked}
                              onChange={(e) => patchField(def.key, { visible: e.target.checked })}
                            >
                              显示
                            </Checkbox>
                            <Checkbox
                              checked={def.locked ? def.key === 'name' || def.key === 'idCardNumber' : !!config?.required}
                              disabled={def.locked && (def.key === 'name' || def.key === 'idCardNumber')}
                              onChange={(e) => patchField(def.key, { required: e.target.checked })}
                            >
                              必填
                            </Checkbox>
                            {inputType === 'select' && (
                              <Checkbox
                                checked={config?.allowCustom ?? def.defaultAllowCustom ?? false}
                                onChange={(e) => patchField(def.key, { allowCustom: e.target.checked })}
                              >
                                允许自定义
                              </Checkbox>
                            )}
                          </Space>
                          {inputType === 'select' && (
                            <>
                              <Space wrap style={{ marginBottom: 8 }}>
                                {options.map((option) => (
                                  <Tag
                                    key={option}
                                    closable
                                    onClose={(e) => {
                                      e.preventDefault();
                                      removeOption(def.key, option);
                                    }}
                                  >
                                    {option}
                                  </Tag>
                                ))}
                                {options.length === 0 && (
                                  <Text type="secondary">暂无选项，可添加或使用默认</Text>
                                )}
                              </Space>
                              <Space.Compact style={{ width: '100%' }}>
                                <Input
                                  placeholder="添加下拉选项"
                                  value={draftOption[def.key] ?? ''}
                                  onChange={(e) => setDraftOption((prev) => ({ ...prev, [def.key]: e.target.value }))}
                                  onPressEnter={() => addOption(def.key)}
                                />
                                <Button icon={<PlusOutlined />} onClick={() => addOption(def.key)}>
                                  添加
                                </Button>
                              </Space.Compact>
                            </>
                          )}
                        </div>
                      );
                    })}
                  </div>
                );
              })}
            </Space>
          ),
        },
      ]}
    />
  );
}
