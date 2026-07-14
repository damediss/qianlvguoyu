import { DatePicker, Form, Input, Select } from 'antd';
import type { FormItemProps } from 'antd';
import dayjs from 'dayjs';
import {
  getFieldConfig,
  type ResearchCollectionFieldOptions,
} from './researchCollectionFieldOptions';

export interface ResearchCollectionFieldInputProps extends FormItemProps {
  fieldKey: string;
  fallbackLabel: string;
  fieldOptions?: ResearchCollectionFieldOptions;
  placeholder?: string;
  readOnly?: boolean;
}

export function ResearchCollectionFieldInput({
  fieldKey,
  fallbackLabel,
  fieldOptions,
  placeholder,
  readOnly,
  ...formItemProps
}: ResearchCollectionFieldInputProps) {
  const config = getFieldConfig(fieldOptions, fieldKey, fallbackLabel);
  if (!config) return null;

  const label = config.label || fallbackLabel;
  const rules = [
    ...(formItemProps.rules ?? []),
    ...(config.required ? [{ required: true, message: `请填写${label}` }] : []),
  ];

  return (
    <Form.Item {...formItemProps} label={label} rules={rules}>
      {readOnly ? (
        <Input readOnly placeholder={placeholder} />
      ) : config.inputType === 'date' ? (
        <DatePicker style={{ width: '100%' }} placeholder={placeholder ?? `请选择${label}`} />
      ) : config.inputType === 'select' && config.options?.length ? (
        <Select
          showSearch
          allowClear
          placeholder={placeholder ?? `请选择${label}`}
          options={config.options?.map((option) => ({ value: option, label: option }))}
          mode={config.allowCustom ? 'tags' : undefined}
          maxTagCount={config.allowCustom ? 1 : undefined}
        />
      ) : (
        <Input placeholder={placeholder ?? `请输入${label}`} />
      )}
    </Form.Item>
  );
}

export function dateFieldGetValueProps(value?: string) {
  return { value: value ? dayjs(value) : undefined };
}

export function dateFieldNormalize(value: dayjs.Dayjs | null) {
  return value ? value.format('YYYY-MM-DD') : undefined;
}
