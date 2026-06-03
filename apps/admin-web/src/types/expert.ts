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
