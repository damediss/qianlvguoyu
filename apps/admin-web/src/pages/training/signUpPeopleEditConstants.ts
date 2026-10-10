/** 对齐 yun_shang information.vue 人员编辑下拉选项 */

export const POLITICAL_OPTIONS = ['群众', '中共党员', '中共预备党员', '共青团员', '民主党派'];

export const HOUSE_TYPE_OPTIONS = ['农业户口', '非农业户口', '居民户口'];

export const EDUCATION_OPTIONS = [
  '小学', '初中', '普通高中', '技工学校', '职业高中', '中等专科',
  '大学专科', '大学本科', '硕士研究生', '博士研究生', '其他',
];

export const TITLE_TYPE_OPTIONS = ['无', '助理工程师', '工程师', '高级工程师', '其他'];

export const WORK_LEVEL_LABELS = ['五级/初级工', '四级/中级工', '三级/高级工', '二级/技师', '一级/高级技师'];

export const WORK_LEVEL_OPTIONS = WORK_LEVEL_LABELS.map((label, value) => ({ label, value }));

export const STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '未通过' },
];

export const PAY_OPTIONS = [
  { value: 0, label: '待支付' },
  { value: 1, label: '已支付' },
];

/** 按预约等级筛选申报条件（item_value / reqLevel，不再使用 PHP 遗留 id 区间） */
export function filterDeclarationConditions<
  T extends { id?: number | string; title: string; value?: string; reqLevel?: number },
>(all: T[], reqLevel?: number): T[] {
  if (reqLevel == null || all.length === 0) return all;
  const filtered = all.filter((item) => {
    if (item.reqLevel != null) return Number(item.reqLevel) === Number(reqLevel);
    if (item.value != null && String(item.value).length > 0) {
      return String(item.value).startsWith(`${reqLevel}-`);
    }
    return true;
  });
  return filtered.length > 0 ? filtered : all;
}
