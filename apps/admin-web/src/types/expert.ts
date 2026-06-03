export interface ExpertTask {
  id: number;
  taskId: number;
  tag: string;
  tagCn: string;
  title: string;
  nums: number;
  addtimeCn: string;
  endtimeCn: string;
  endtime: number;
  status: string;
}

export interface ExpertTaskQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}

export interface ExpertAuditItem {
  id: number;
  task_id: number;
  exam_type?: string;
  exam_content?: string;
  exam_a?: string;
  exam_b?: string;
  exam_c?: string;
  exam_d?: string;
  exam_e?: string;
  exam_f?: string;
  exam_g?: string;
  exam_h?: string;
  exam_answer?: string;
  jianding?: string;
  content?: string;
  pingfen?: string;
  kaochang?: string;
  kaosheng?: string;
  zhishidian?: string;
  answer?: string;
  pic?: string;
  sno?: string;
  sno_0?: string;
  sno_1?: string;
  title?: string;
  sno_item?: string;
  sno_title?: string;
  [key: string]: unknown;
}

export interface XimuTreeNode {
  label: string;
  id: string;
  disabled: boolean;
  task_id?: number;
  title?: string;
  list?: XimuListItem[];
  children?: XimuTreeNode[];
}

export interface XimuListItem {
  id: number;
  task_id: number;
  sno: string;
  sno_item: string;
  sno_title: string;
  title: string;
}

export interface XimuListResponse {
  taskXimu: { id: number; types: number; status: number };
  treeData: XimuTreeNode[];
  ximuTotal: number;
}

export interface ExpertSubmitReq {
  id: number;
  taskId: number;
  role: number;
  examType?: string;
  examContent?: string;
  examA?: string;
  examB?: string;
  examC?: string;
  examD?: string;
  examE?: string;
  examF?: string;
  examG?: string;
  examH?: string;
  examAnswer?: string;
  jianding?: string;
  content?: string;
  pingfen?: string;
  kaochang?: string;
  kaosheng?: string;
  answer?: string;
  pic?: string;
  sno?: string;
  snoItem?: string;
  snoTitle?: string;
}

// ---- 题库审核指派 - 专家端 3 子菜单(理论/实操/主观) ----
// 与上面 ExpertTask 区分(老流程用 ExpertTask;新流程用下列 ExpertMyTask*)
import type { TaskAuditType, TaskAuditRole } from './exam';

export interface ExpertMyTaskVO {
  id: number;
  taskId: number;
  taskType: TaskAuditType;
  taskTypeCn: string;
  workTypeCn: string;
  levelCn: string;
  nums: number;
  finishedNums: number;
  addtimeCn: string;
  endtimeCn: string;
  role: TaskAuditRole;
  roleCn: string;
  status: number;
  statusCn: string;
}

export interface ExpertMyTaskQuery {
  page?: number;
  pageSize?: number;
  role?: TaskAuditRole;
  status?: number;
}

export interface ExpertAuditFormItem {
  id: number;
  taskId: number;
  taskType: TaskAuditType;
  // 理论
  examType?: string;
  examContent?: string;
  examA?: string;
  examB?: string;
  examC?: string;
  examD?: string;
  examE?: string;
  examF?: string;
  examG?: string;
  examH?: string;
  examAnswer?: string;
  isblod?: number;
  // 实操
  jianding?: string;
  content?: string;
  pingfen?: string;
  kaochang?: string;
  kaosheng?: string;
  // 主观
  zhishidian?: string;
  answer?: string;
  isJisuan?: number;
  pic?: string;
  // 通用
  [key: string]: unknown;
}

export interface ExpertTaskDetailVO {
  task: ExpertMyTaskVO;
  items: ExpertAuditFormItem[];
}

export interface ExpertTaskSubmitItem {
  examId: number;
  // 理论
  examContent?: string;
  examA?: string;
  examB?: string;
  examC?: string;
  examD?: string;
  examE?: string;
  examF?: string;
  examG?: string;
  examH?: string;
  examAnswer?: string;
  isblod?: number;
  // 实操
  content?: string;
  pingfen?: string;
  kaochang?: string;
  kaosheng?: string;
  // 主观
  answer?: string;
  pic?: string;
}

export interface ExpertTaskSubmitReq {
  taskId: number;
  role: TaskAuditRole;
  items: ExpertTaskSubmitItem[];
}

export interface ExpertTaskSubmitResp {
  count: number;   // 提交数
  total: number;   // 任务总题数
  isdone: number;  // 1=全部提交完成,0=部分
}
