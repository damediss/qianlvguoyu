import { Button, DatePicker, Divider, Form, Input, Select, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  ResearchCollectionFieldInput,
  dateFieldGetValueProps,
  dateFieldNormalize,
} from './ResearchCollectionFieldInput';
import {
  ALL_COLLECTION_FIELDS,
  getFieldDef,
  getOrderedGroups,
  getOrderedVisibleFields,
  isCertificateFieldKey,
  type ResearchCollectionFieldOptions,
} from './researchCollectionFieldOptions';

const LEVEL_OPTIONS = ['初级工', '中级工', '高级工', '技师', '高级技师'];

export interface ResearchCollectionPersonFormProps {
  fieldOptions?: ResearchCollectionFieldOptions;
  companyName?: string;
}

function renderBasicField(
  fieldKey: string,
  fieldOptions?: ResearchCollectionFieldOptions,
  companyName?: string,
) {
  const def = getFieldDef(fieldKey);
  if (!def) return null;
  const config = getOrderedVisibleFields(fieldOptions).find((f) => f.key === fieldKey)?.config;
  if (!config && !def.locked) return null;

  if (fieldKey === 'name') {
    return (
      <Form.Item key={fieldKey} name="name" label={config?.label || def.label} rules={[{ required: true, message: '请输入姓名' }]}>
        <Input />
      </Form.Item>
    );
  }
  if (fieldKey === 'idCardNumber' || fieldKey === 'phone') {
    return (
      <Form.Item key={fieldKey} name={fieldKey} label={config?.label || def.label} rules={fieldKey === 'idCardNumber' ? [{ required: true, message: '请输入身份证号码' }] : undefined}>
        <Input />
      </Form.Item>
    );
  }
  if (fieldKey === 'companyName') {
    return (
      <Form.Item key={fieldKey} name="companyName" label={config?.label || def.label} initialValue={companyName}>
        <Input readOnly />
      </Form.Item>
    );
  }
  if (config?.inputType === 'date') {
    return (
      <Form.Item
        key={fieldKey}
        name={fieldKey}
        label={config.label || def.label}
        rules={config.required ? [{ required: true, message: `请选择${config.label || def.label}` }] : undefined}
        getValueProps={dateFieldGetValueProps}
        normalize={dateFieldNormalize}
      >
        <DatePicker style={{ width: '100%' }} />
      </Form.Item>
    );
  }
  return (
    <ResearchCollectionFieldInput
      key={fieldKey}
      fieldKey={fieldKey}
      fallbackLabel={def.label}
      fieldOptions={fieldOptions}
      name={fieldKey}
    />
  );
}

export function ResearchCollectionPersonForm({
  fieldOptions,
  companyName,
}: ResearchCollectionPersonFormProps) {
  const groups = getOrderedGroups(fieldOptions);
  const certFieldKeys = ALL_COLLECTION_FIELDS.filter((f) => isCertificateFieldKey(f.key)).map((f) => f.key);
  const certFields = getOrderedVisibleFields(fieldOptions, 'certificate').filter((f) => isCertificateFieldKey(f.key));

  return (
    <>
      {groups.map((group) => {
        if (group.key === 'certificate') return null;
        const fields = getOrderedVisibleFields(fieldOptions, group.key).filter((f) => !isCertificateFieldKey(f.key));
        if (fields.length === 0) return null;
        return (
          <div key={group.key}>
            {group.key !== 'basic' && <Divider orientation="left" plain>{group.label}</Divider>}
            {fields.map(({ key }) => renderBasicField(key, fieldOptions, companyName))}
          </div>
        );
      })}

      {certFields.length > 0 && (
        <>
          <Divider orientation="left" plain>
            {groups.find((g) => g.key === 'certificate')?.label || '技能等级证书'}
          </Divider>
          <Form.List name="certificates">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Space key={field.key} style={{ display: 'flex', marginBottom: 8 }} align="baseline" wrap>
                    {certFieldKeys.includes('certLevel') && certFields.some((f) => f.key === 'certLevel') && (
                      <Form.Item {...field} name={[field.name, 'level']} label={certFields.find((f) => f.key === 'certLevel')?.config.label || '已有证书等级'}>
                        <Select
                          allowClear
                          style={{ width: 130 }}
                          options={LEVEL_OPTIONS.map((v) => ({ value: v, label: v }))}
                        />
                      </Form.Item>
                    )}
                    {certFieldKeys.includes('certNo') && certFields.some((f) => f.key === 'certNo') && (
                      <Form.Item {...field} name={[field.name, 'certNo']} label={certFields.find((f) => f.key === 'certNo')?.config.label || '证书编号'}>
                        <Input style={{ width: 160 }} />
                      </Form.Item>
                    )}
                    {certFieldKeys.includes('certIssueDate') && certFields.some((f) => f.key === 'certIssueDate') && (
                      <Form.Item
                        {...field}
                        name={[field.name, 'issueDate']}
                        label={certFields.find((f) => f.key === 'certIssueDate')?.config.label || '发证日期'}
                        getValueProps={(v) => ({ value: v ? dayjs(v) : undefined })}
                        normalize={(v) => (v ? v.format('YYYY-MM-DD') : undefined)}
                      >
                        <DatePicker style={{ width: 150 }} />
                      </Form.Item>
                    )}
                    {fields.length > 1 && (
                      <Button type="link" danger onClick={() => remove(field.name)}>
                        删除
                      </Button>
                    )}
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  添加证书
                </Button>
              </>
            )}
          </Form.List>
        </>
      )}
    </>
  );
}
