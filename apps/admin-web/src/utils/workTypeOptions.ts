import type { ExamWorkType } from '../types/exam';
import type { WorkTypeGroup } from '../types/training';

export interface WorkTypeOption {
  value: string;
  label: string;
  title: string;
}

/** 培训需求 worktype_list 的 Select 选项：平铺项或分组项 */
export type DemandWorkTypeSelectOption =
  | { value: number; label: string }
  | { label: string; options: { value: number; label: string }[] };

/**
 * 培训需求工种分组数据转 Select 选项。
 * 后端把无子工种的顶级工种（如"电工"）回填"自己"为唯一选项：
 * 唯一选项即组本身时拍平成普通选项，避免"组标题+同名选项"重复观感。
 */
export function buildDemandWorkTypeOptions(
  groups?: WorkTypeGroup[],
): DemandWorkTypeSelectOption[] {
  return (groups ?? []).map((g) => {
    const opts = (g.options ?? []).map((o) => ({ value: o.id, label: o.title }));
    if (opts.length === 1 && opts[0].value === g.id) {
      return opts[0];
    }
    return { label: g.title, options: opts };
  });
}

/** 分组/平铺混合选项拉平（反查 id→label、回显等场景用） */
export function flattenDemandWorkTypeOptions(
  options: DemandWorkTypeSelectOption[],
): { value: number; label: string }[] {
  return options.flatMap((o) => ('options' in o ? o.options : [o]));
}

/** 将课程管理工种树转换为有效叶子工种选项，值使用稳定 ID，标签显示完整路径。 */
export function flattenWorkTypeOptions(nodes: ExamWorkType[]): WorkTypeOption[] {
  const options: WorkTypeOption[] = [];
  visitWorkTypes(nodes, '', options, false);
  return options;
}

/** 考评员授权可选树中任一有效节点，并展示父级路径。 */
export function flattenAssessorWorkTypeOptions(nodes: ExamWorkType[]): WorkTypeOption[] {
  const options: WorkTypeOption[] = [];
  visitWorkTypes(nodes, '', options, true);
  return options;
}

export function workTypeTitleForId(nodes: ExamWorkType[], id?: string | number | null): string | undefined {
  if (id == null) return undefined;
  const match = flattenAssessorWorkTypeOptions(nodes).find((option) => option.value === String(id));
  return match?.title;
}

/** Resolve legacy names by exact title, defaulting duplicate titles to their unique top-level occupation. */
export function uniqueWorkTypeIdForTitle(nodes: ExamWorkType[], title?: string): string | undefined {
  const normalized = title?.trim();
  if (!normalized) return undefined;
  const matches: ExamWorkType[] = [];
  const collectMatches = (items: ExamWorkType[]) => {
    items.forEach((node) => {
      if (node.status !== 1) return;
      if (node.title?.trim() === normalized) matches.push(node);
      collectMatches(node.children ?? []);
    });
  };
  collectMatches(nodes);
  if (matches.length === 1) return String(matches[0].id);

  const topLevelMatches = matches.filter((node) => node.parentId == null || node.parentId <= 0);
  return topLevelMatches.length === 1 ? String(topLevelMatches[0].id) : undefined;
}

function visitWorkTypes(
  nodes: ExamWorkType[],
  parentPath: string,
  options: WorkTypeOption[],
  includeParents: boolean,
) {
  nodes.forEach((node) => {
    if (node.status !== 1 || !node.title) return;
    const path = parentPath ? `${parentPath} / ${node.title}` : node.title;
    const activeChildren = (node.children ?? []).filter((child) => child.status === 1);
    if (includeParents || activeChildren.length === 0) {
      options.push({ value: String(node.id), label: path, title: node.title });
    }
    visitWorkTypes(activeChildren, path, options, includeParents);
  });
}
