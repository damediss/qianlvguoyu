export type ResearchCollectionInputType = 'text' | 'select' | 'date';

export interface ResearchCollectionFieldConfig {
  inputType?: ResearchCollectionInputType;
  label?: string;
  required?: boolean;
  visible?: boolean;
  allowCustom?: boolean;
  order?: number;
  group?: string;
  options?: string[];
}

export interface ResearchCollectionGroupConfig {
  key: string;
  label: string;
  order: number;
}

export interface ResearchCollectionFieldOptions {
  version?: number;
  groups?: ResearchCollectionGroupConfig[];
  fields?: Record<string, ResearchCollectionFieldConfig>;
}

export const SCHEMA_VERSION = 2;

export const DEFAULT_EDUCATION_OPTIONS = ['初中', '高中', '中专', '大专', '本科', '研究生'];
export const DEFAULT_LEVEL_OPTIONS = ['初级工', '中级工', '高级工', '技师', '高级技师'];

export interface CollectionFieldDef {
  key: string;
  label: string;
  inputType: ResearchCollectionInputType;
  group: string;
  order: number;
  locked?: boolean;
  defaultRequired?: boolean;
  defaultOptions?: string[];
  defaultAllowCustom?: boolean;
  readonly?: boolean;
}

export interface ResolvedCollectionField {
  key: string;
  def: CollectionFieldDef;
  config: ResearchCollectionFieldConfig;
}

export const DEFAULT_COLLECTION_GROUPS: ResearchCollectionGroupConfig[] = [
  { key: 'basic', label: '基本信息', order: 1 },
  { key: 'certificate', label: '技能等级证书', order: 2 },
  { key: 'title', label: '职称信息', order: 3 },
];

export const ALL_COLLECTION_FIELDS: CollectionFieldDef[] = [
  { key: 'name', label: '姓名', inputType: 'text', group: 'basic', order: 1, locked: true, defaultRequired: true },
  { key: 'idCardNumber', label: '身份证号码', inputType: 'text', group: 'basic', order: 2, locked: true, defaultRequired: true },
  { key: 'phone', label: '手机号', inputType: 'text', group: 'basic', order: 3 },
  { key: 'companyName', label: '所在单位', inputType: 'text', group: 'basic', order: 4, readonly: true },
  { key: 'major', label: '所学专业（初中、高中学历的填无）', inputType: 'select', group: 'basic', order: 5, defaultAllowCustom: true },
  { key: 'education', label: '学历层次', inputType: 'select', group: 'basic', order: 6, defaultOptions: DEFAULT_EDUCATION_OPTIONS },
  { key: 'workTime', label: '参加工作时间', inputType: 'date', group: 'basic', order: 7 },
  { key: 'workTypeName', label: '原工种', inputType: 'select', group: 'basic', order: 8 },
  { key: 'department', label: '部门', inputType: 'select', group: 'basic', order: 9 },
  { key: 'job', label: '实际岗位名称', inputType: 'select', group: 'basic', order: 10 },
  { key: 'workDateStart', label: '入职时间', inputType: 'date', group: 'basic', order: 11 },
  { key: 'certLevel', label: '已有证书等级', inputType: 'select', group: 'certificate', order: 12, defaultOptions: DEFAULT_LEVEL_OPTIONS },
  { key: 'certNo', label: '证书编号', inputType: 'text', group: 'certificate', order: 13 },
  { key: 'certIssueDate', label: '发证日期', inputType: 'date', group: 'certificate', order: 14 },
  { key: 'titleType', label: '职称', inputType: 'select', group: 'title', order: 15, defaultAllowCustom: true },
  { key: 'titleMajor', label: '职称专业', inputType: 'text', group: 'title', order: 16 },
  { key: 'titleCertDate', label: '职称获证时间', inputType: 'date', group: 'title', order: 17 },
];

/** @deprecated use ALL_COLLECTION_FIELDS */
export const CONFIGURABLE_COLLECTION_FIELDS = ALL_COLLECTION_FIELDS.filter((f) => f.inputType === 'select' && !f.locked);

function defaultOptionsForField(def: CollectionFieldDef): string[] {
  if (def.defaultOptions?.length) return [...def.defaultOptions];
  if (def.key === 'education') return [...DEFAULT_EDUCATION_OPTIONS];
  if (def.key === 'certLevel') return [...DEFAULT_LEVEL_OPTIONS];
  return [];
}

export function mergeFieldConfig(def: CollectionFieldDef, incoming?: ResearchCollectionFieldConfig | null): ResearchCollectionFieldConfig {
  let inputType = incoming?.inputType || def.inputType;
  if ((def.key === 'department' || def.key === 'job') && def.inputType === 'select') {
    inputType = 'select';
  }
  const savedOptions = incoming?.options?.length ? [...incoming.options] : [];
  if (savedOptions.length > 0 && def.inputType === 'select') {
    inputType = 'select';
  }
  const required = (def.locked && (def.key === 'name' || def.key === 'idCardNumber'))
    || (incoming?.required ?? def.defaultRequired ?? false);
  const visible = (def.locked || def.key === 'workDateStart') ? true : incoming?.visible !== false;
  let allowCustom = incoming?.allowCustom ?? def.defaultAllowCustom ?? false;
  const options = inputType === 'select'
    ? (savedOptions.length ? savedOptions : defaultOptionsForField(def))
    : [];
  if ((def.key === 'department' || def.key === 'job') && inputType === 'select' && options.length > 0) {
    allowCustom = false;
  }
  return {
    inputType,
    label: incoming?.label?.trim() || def.label,
    required,
    visible,
    allowCustom,
    order: def.key === 'workDateStart' ? def.order : (incoming?.order && incoming.order > 0 ? incoming.order : def.order),
    group: def.key === 'workDateStart' ? def.group : (incoming?.group || def.group),
    options,
  };
}

export function buildDefaultFieldOptions(): ResearchCollectionFieldOptions {
  const fields: Record<string, ResearchCollectionFieldConfig> = {};
  ALL_COLLECTION_FIELDS.forEach((def) => {
    fields[def.key] = mergeFieldConfig(def);
  });
  return {
    version: SCHEMA_VERSION,
    groups: DEFAULT_COLLECTION_GROUPS.map((g) => ({ ...g })),
    fields,
  };
}

export const buildEmptyFieldOptionsForm = buildDefaultFieldOptions;

export function normalizeCollectionFieldOptions(raw?: ResearchCollectionFieldOptions | null): ResearchCollectionFieldOptions {
  const incomingFields = raw?.fields ?? {};
  const fields: Record<string, ResearchCollectionFieldConfig> = {};
  ALL_COLLECTION_FIELDS.forEach((def) => {
    fields[def.key] = mergeFieldConfig(def, incomingFields[def.key]);
  });

  const incomingGroups = new Map((raw?.groups ?? []).map((g) => [g.key, g]));
  const groups = DEFAULT_COLLECTION_GROUPS.map((def) => {
    const saved = incomingGroups.get(def.key);
    return {
      key: def.key,
      label: saved?.label?.trim() || def.label,
      order: saved?.order && saved.order > 0 ? saved.order : def.order,
    };
  }).sort((a, b) => a.order - b.order);

  return {
    version: raw?.version && raw.version > 0 ? raw.version : SCHEMA_VERSION,
    groups,
    fields,
  };
}

export function mergeFieldOptionsForForm(raw?: ResearchCollectionFieldOptions | null): ResearchCollectionFieldOptions {
  return normalizeCollectionFieldOptions(raw);
}

export function packFieldOptionsForSave(raw?: ResearchCollectionFieldOptions | null): ResearchCollectionFieldOptions {
  return normalizeCollectionFieldOptions(raw);
}

export function getFieldDef(key: string): CollectionFieldDef | undefined {
  return ALL_COLLECTION_FIELDS.find((f) => f.key === key);
}

export function getFieldConfig(
  fieldOptions: ResearchCollectionFieldOptions | undefined,
  fieldKey: string,
  fallbackLabel: string,
): ResearchCollectionFieldConfig | undefined {
  const normalized = normalizeCollectionFieldOptions(fieldOptions);
  const def = getFieldDef(fieldKey);
  const config = normalized.fields?.[fieldKey];
  if (!def || !config || config.visible === false) return undefined;
  return {
    ...config,
    label: config.label || fallbackLabel,
  };
}

export function getOrderedGroups(fieldOptions?: ResearchCollectionFieldOptions | null): ResearchCollectionGroupConfig[] {
  return normalizeCollectionFieldOptions(fieldOptions).groups ?? DEFAULT_COLLECTION_GROUPS;
}

export function getOrderedVisibleFields(
  fieldOptions?: ResearchCollectionFieldOptions | null,
  groupKey?: string,
): ResolvedCollectionField[] {
  const normalized = normalizeCollectionFieldOptions(fieldOptions);
  const groupOrder = new Map((normalized.groups ?? []).map((g) => [g.key, g.order]));
  const result: ResolvedCollectionField[] = [];
  ALL_COLLECTION_FIELDS.forEach((def) => {
    const config = normalized.fields?.[def.key] ?? mergeFieldConfig(def);
    if (!def.locked && config.visible === false) return;
    if (groupKey && (config.group || def.group) !== groupKey) return;
    result.push({ key: def.key, def, config: { ...config, group: config.group || def.group } });
  });
  result.sort((a, b) => {
    const ga = groupOrder.get(a.config.group || a.def.group) ?? 99;
    const gb = groupOrder.get(b.config.group || b.def.group) ?? 99;
    if (ga !== gb) return ga - gb;
    return (a.config.order ?? a.def.order) - (b.config.order ?? b.def.order);
  });
  return result;
}

export function isCertificateFieldKey(key: string): boolean {
  return key === 'certLevel' || key === 'certNo' || key === 'certIssueDate';
}

const SELECT_FIELD_KEYS = ALL_COLLECTION_FIELDS.filter((f) => f.inputType === 'select').map((f) => f.key);

export function normalizePersonSelectValue(value: unknown): string | undefined {
  if (Array.isArray(value)) {
    const first = value.find((item) => String(item).trim());
    return first != null ? String(first).trim() : undefined;
  }
  if (value == null) return undefined;
  const text = String(value).trim();
  return text || undefined;
}

export function normalizePersonFormValues(
  values: Record<string, unknown>,
): Record<string, unknown> {
  const next: Record<string, unknown> = { ...values };
  SELECT_FIELD_KEYS.forEach((key) => {
    if (key in next) {
      next[key] = normalizePersonSelectValue(next[key]);
    }
  });
  return next;
}

export function validatePersonFormValues(
  values: Record<string, unknown>,
  fieldOptions?: ResearchCollectionFieldOptions | null,
): string | null {
  const fields = getOrderedVisibleFields(fieldOptions);
  for (const { key, config } of fields) {
    if (!config.required) continue;
    if (isCertificateFieldKey(key)) {
      const certificates = values.certificates as Array<Record<string, unknown>> | undefined;
      const first = certificates?.[0];
      const certKey = key === 'certLevel' ? 'level' : key === 'certNo' ? 'certNo' : 'issueDate';
      const val = first?.[certKey];
      if (!String(val ?? '').trim()) return `${config.label}不能为空`;
      continue;
    }
    const val = values[key];
    if (!String(val ?? '').trim()) return `${config.label}不能为空`;
  }
  if (!String(values.name ?? '').trim()) return '姓名不能为空';
  if (!String(values.idCardNumber ?? '').trim()) return '身份证号码不能为空';
  return null;
}
