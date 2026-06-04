import type { CertificationWbsItem } from '../../../types/certification';

export const wbsCategoryOptions = [
  { label: '规划', value: 'plan', name: '规划' },
  { label: '执行', value: 'exec', name: '执行' },
  { label: '审核', value: 'review', name: '审核' },
] as const;

export const wbsStatusOptions = [
  { label: '待开始', value: 'pending' },
  { label: '进行中', value: 'running' },
  { label: '已完成', value: 'completed' },
];

export function resolveCategoryCode(categoryName?: string, categoryCode?: string) {
  if (categoryCode) {
    return categoryCode;
  }
  const matched = wbsCategoryOptions.find((item) => item.name === categoryName);
  return matched?.value;
}

export function resolveCategoryName(categoryName?: string, categoryCode?: string) {
  if (categoryName) {
    return categoryName;
  }
  const matched = wbsCategoryOptions.find((item) => item.value === categoryCode);
  return matched?.name;
}

export function formatManpowerInput(item: CertificationWbsItem) {
  const role = item.manpowerRoleCode?.trim();
  const count = item.manpowerCount;
  if (role && count != null) {
    return `${role} ${count} 人`;
  }
  return role || (count != null ? `${count} 人` : '-');
}

export function formatEstimateHours(value?: number) {
  if (value == null || Number.isNaN(value)) {
    return '-';
  }
  return `${value}h`;
}

export function summarizeWbsItems(items: CertificationWbsItem[]) {
  const totalHours = items.reduce((sum, item) => sum + (item.estimateHours || 0), 0);
  const totalDays = items.reduce((sum, item) => sum + (item.durationDays || 0), 0);
  const totalCost = items.reduce((sum, item) => sum + (item.costAmount || 0), 0);
  return {
    count: items.length,
    totalHours,
    totalDays,
    totalCost,
  };
}

export function createEmptyWbsItem(index: number): CertificationWbsItem {
  return {
    wbsCode: String((index + 1) * 10).padStart(4, '0'),
    categoryCode: 'plan',
    categoryName: '规划',
    itemName: '',
    activityDescription: '',
    estimateHours: undefined,
    manpowerCount: undefined,
    manpowerRoleCode: '',
    resourceInput: '',
    costAmount: undefined,
    durationDays: undefined,
    status: 'pending',
  };
}

export function validateWbsItems(items: CertificationWbsItem[]) {
  if (items.length === 0) {
    return '请至少添加一条 WBS 事项';
  }
  for (let i = 0; i < items.length; i += 1) {
    const row = items[i];
    if (!row.wbsCode?.trim()) {
      return `第 ${i + 1} 条：请填写 WBS 编码`;
    }
    if (!row.itemName?.trim()) {
      return `第 ${i + 1} 条：请填写事项名称`;
    }
  }
  return null;
}
