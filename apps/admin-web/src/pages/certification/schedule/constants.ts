import dayjs from 'dayjs';
import type {
  CertificationMember,
  CertificationScheduleItem,
  CertificationTask,
  CertificationTaskScope,
  CertificationTaskScopePayload,
} from '../../../types/certification';

export const certificationTypeOptions = [
  { label: '认定工作流程（有题库）', value: 'with_question_bank' },
  { label: '认定工作流程（无题库）', value: 'without_question_bank' },
  { label: '企业专项认定', value: 'enterprise_special' },
];

export const scheduleStatusOptions = [
  { label: '进行中', value: 'running' },
  { label: '认定准备已完成', value: 'project_prepared' },
  { label: '认定筹备已完成', value: 'preparation_completed' },
  { label: '认定归档已完成', value: 'archive_completed' },
  { label: '待审核', value: 'pending_review' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已完成', value: 'completed' },
];

export const levelOptions = [
  { label: '初级', value: 'junior' },
  { label: '中级', value: 'intermediate' },
  { label: '高级', value: 'senior' },
  { label: '技师', value: 'technician' },
  { label: '高级技师', value: 'senior_technician' },
];

/** 考试信息行，对应 certification_task_scope */
export interface ScheduleExamInfoItem {
  id?: string;
  certificationType?: string;
  workTypeName?: string;
  levelCode?: string;
  levelName?: string;
  plannedPeopleCount?: number;
}

export interface ScheduleTaskFields {
  examDate?: string;
  planStartDate?: string;
  status?: string;
  externalContactMemberId?: string;
  certificationOwnerMemberId?: string;
}

export function certificationTypeLabel(value?: string) {
  return certificationTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

export function scheduleStatusLabel(value?: string, statusName?: string) {
  if (statusName) {
    return statusName;
  }
  return scheduleStatusOptions.find((item) => item.value === value)?.label || value || '-';
}

export function scopeToExamInfo(scope: CertificationTaskScope, task?: CertificationTask): ScheduleExamInfoItem {
  return {
    id: scope.id,
    certificationType: scope.certificationType || task?.certificationType,
    workTypeName: scope.workTypeName,
    levelCode: scope.levelCode,
    levelName: scope.levelName,
    plannedPeopleCount: scope.plannedPeopleCount ?? task?.plannedPeopleCount,
  };
}

export function buildExamInfoFromTask(task?: CertificationTask): ScheduleExamInfoItem[] {
  if (!task) {
    return [];
  }
  if (task.scopes && task.scopes.length > 0) {
    return task.scopes.map((scope) => scopeToExamInfo(scope, task));
  }
  if (task.workTypeName || task.levelCode || task.levelName || task.certificationType) {
    return [{
      certificationType: task.certificationType,
      workTypeName: task.workTypeName,
      levelCode: task.levelCode,
      levelName: task.levelName,
      plannedPeopleCount: task.plannedPeopleCount,
    }];
  }
  return [];
}

export function buildScheduleTaskFields(
  task?: CertificationTask,
  items: CertificationScheduleItem[] = [],
): ScheduleTaskFields {
  const first = items[0];
  return {
    examDate: task?.examDate,
    planStartDate: task?.planStartDate,
    status: task?.status,
    externalContactMemberId: first?.externalContactMemberId,
    certificationOwnerMemberId: first?.certificationOwnerMemberId,
  };
}

export function calcPreparationDays(examDate?: string, planStartDate?: string) {
  if (!examDate || !planStartDate) {
    return null;
  }
  const days = dayjs(examDate).diff(dayjs(planStartDate), 'day');
  if (Number.isNaN(days) || days < 0) {
    return null;
  }
  return days;
}

export function formatPreparationDays(examDate?: string, planStartDate?: string) {
  const days = calcPreparationDays(examDate, planStartDate);
  return days == null ? '-' : `${days}天`;
}

export function formatDayLabel(item: CertificationScheduleItem) {
  if (item.startDate && item.endDate && item.startDate !== item.endDate) {
    const start = dayjs(item.startDate).date();
    const end = dayjs(item.endDate).date();
    return `第 ${start}-${end} 天`;
  }
  if (item.dayNo != null) {
    return `第 ${item.dayNo} 天`;
  }
  if (item.startDate) {
    return `第 ${dayjs(item.startDate).date()} 天`;
  }
  return '安排项';
}

export function memberNameById(members: CertificationMember[], memberId?: string) {
  if (!memberId) {
    return '-';
  }
  return members.find((member) => member.id === memberId)?.memberName || '-';
}

export function summarizeScheduleItems(items: CertificationScheduleItem[]) {
  const requiredItems = items.filter((item) => item.required !== false);
  const pool = requiredItems.length > 0 ? requiredItems : items;
  const completed = pool.filter((item) => item.completed).length;
  const total = pool.length;
  const percent = total === 0 ? 0 : Math.round((completed / total) * 100);
  return { completed, total, percent };
}

export function summarizeExamPeopleCount(examInfos: ScheduleExamInfoItem[]) {
  return examInfos.reduce((sum, row) => sum + (row.plannedPeopleCount ?? 0), 0);
}

export function applyContactIds(
  items: CertificationScheduleItem[],
  fields: ScheduleTaskFields,
): CertificationScheduleItem[] {
  return items.map((item) => ({
    ...item,
    externalContactMemberId: fields.externalContactMemberId,
    certificationOwnerMemberId: fields.certificationOwnerMemberId,
  }));
}

export function examInfoToScopePayload(examInfos: ScheduleExamInfoItem[]): CertificationTaskScopePayload[] {
  return examInfos.map((row) => ({
    certificationType: row.certificationType,
    workTypeName: row.workTypeName,
    levelCode: row.levelCode,
    levelName: row.levelName,
    plannedPeopleCount: row.plannedPeopleCount,
  }));
}

export function buildScheduleTaskPayload(
  task: CertificationTask,
  fields: ScheduleTaskFields,
  examInfos: ScheduleExamInfoItem[],
) {
  const totalPeople = summarizeExamPeopleCount(examInfos);
  const primaryType = examInfos[0]?.certificationType || task.certificationType;
  return {
    taskName: task.taskName,
    taskManagerName: task.taskManagerName,
    companyName: task.companyName,
    certificationType: primaryType,
    plannedPeopleCount: totalPeople > 0 ? totalPeople : task.plannedPeopleCount,
    planStartDate: fields.planStartDate,
    planEndDate: task.planEndDate,
    examDate: fields.examDate,
    sourceType: task.sourceType,
    status: fields.status,
    remark: task.remark,
    scopes: examInfoToScopePayload(examInfos),
  };
}

export function createEmptyExamInfoItem(task?: CertificationTask): ScheduleExamInfoItem {
  return {
    certificationType: task?.certificationType || 'with_question_bank',
    workTypeName: '',
    levelCode: undefined,
    levelName: '',
    plannedPeopleCount: undefined,
  };
}

export function createEmptyScheduleItem(index: number): CertificationScheduleItem {
  return {
    dayNo: index + 1,
    itemName: '',
    itemDescription: '',
    required: true,
    completed: false,
  };
}

export function defaultScheduleTemplate(): CertificationScheduleItem[] {
  return [
    { dayNo: 1, itemName: '与企业确认考试时间', required: true, completed: false },
    { dayNo: 2, itemName: '企业学员进行 APP 报名、材料收集', itemDescription: '第 2-4 天', required: true, completed: false },
    { dayNo: 5, itemName: '确认是否增加考点协议及系统配置', itemDescription: '第 5-6 天', required: true, completed: false },
    { dayNo: 7, itemName: '完成资料补充与导出包整理', itemDescription: '第 7-9 天', required: true, completed: false },
    { dayNo: 10, itemName: '外发审核并根据意见修订', itemDescription: '第 10-12 天', required: true, completed: false },
  ];
}

export function validateSchedulePlan(
  fields: ScheduleTaskFields,
  examInfos: ScheduleExamInfoItem[],
  items: CertificationScheduleItem[],
) {
  if (!fields.examDate) {
    return '请选择考试日期';
  }
  if (!fields.planStartDate) {
    return '请选择启动日期';
  }
  if (dayjs(fields.planStartDate).isAfter(dayjs(fields.examDate), 'day')) {
    return '启动日期不能晚于考试日期';
  }
  if (!fields.externalContactMemberId) {
    return '请选择对外联系人';
  }
  if (!fields.certificationOwnerMemberId) {
    return '请选择认定负责人';
  }
  if (examInfos.length === 0) {
    return '请至少添加一条考试信息';
  }
  for (let i = 0; i < examInfos.length; i += 1) {
    const row = examInfos[i];
    if (!row.certificationType) {
      return `第 ${i + 1} 条考试信息：请选择认定类型`;
    }
    if (!row.workTypeName?.trim()) {
      return `第 ${i + 1} 条考试信息：请输入工种`;
    }
    if (!row.levelCode && !row.levelName?.trim()) {
      return `第 ${i + 1} 条考试信息：请选择或填写等级`;
    }
    if (row.plannedPeopleCount == null || row.plannedPeopleCount < 0) {
      return `第 ${i + 1} 条考试信息：请输入认定人数`;
    }
  }
  if (items.length === 0) {
    return '请至少添加一条准备期关键安排';
  }
  for (let i = 0; i < items.length; i += 1) {
    if (!items[i].itemName?.trim()) {
      return `第 ${i + 1} 条安排的事项名称不能为空`;
    }
  }
  return null;
}

export function updateExamItem(items: ScheduleExamInfoItem[], index: number, patch: Partial<ScheduleExamInfoItem>) {
  return items.map((row, i) => (i === index ? { ...row, ...patch } : row));
}
