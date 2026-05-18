export interface ExamWorkType {
  id: number;
  title: string;
  seriesNo: string;
  levelsNo: string;
  parentId: number | null;
  userId: number | null;
  status: number;
  sortNumber: number;
  tenantId: number;
  createTime: string;
  children?: ExamWorkType[];
}

export interface ExamWorkTypeCreateReq {
  title: string;
  seriesNo?: string;
  levelsNo?: string;
  parentId?: number;
  status?: number;
  sortNumber?: number;
}

export interface ExamWorkTypeUpdateReq {
  title: string;
  seriesNo?: string;
  levelsNo?: string;
  parentId?: number;
  status?: number;
  sortNumber?: number;
}

export interface ExamWorkTypeQuery {
  page?: number;
  pageSize?: number;
  title?: string;
}
