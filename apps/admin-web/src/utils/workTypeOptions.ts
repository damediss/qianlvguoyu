import type { ExamWorkType } from '../types/exam';

export interface WorkTypeOption {
  value: string;
  label: string;
}

/** 将课程管理中的工种树转换为下拉选项，并按工种名称去重。 */
export function flattenWorkTypeOptions(nodes: ExamWorkType[]): WorkTypeOption[] {
  const seen = new Set<string>();
  const options: WorkTypeOption[] = [];

  const visit = (items: ExamWorkType[]) => {
    items.forEach((node) => {
      if (node.title && !seen.has(node.title)) {
        seen.add(node.title);
        options.push({ value: node.title, label: node.title });
      }
      visit(node.children ?? []);
    });
  };

  visit(nodes);
  return options;
}
