export interface ChazhengUser {
  id: number;
  enterpriseId: number | null;
  userName: string;
  idCard: string;
  gender: number | null;
  datas: string | null;
  isCert: number;
  isChecked: number;
  ymd: string;
  status: number;
  sortNumber: number;
  createTime: string;
  certs: ChazhengCert[];
}

export interface ChazhengCert {
  certNo: string;
  certName: string;
  certDate: string;
  /** 证书图片文件ID（有值时前端可预览证书图片） */
  licenseFileId?: number | string | null;
}

export interface ChazhengUserQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  enterpriseId?: number;
  ymd?: string;
  /** 0=未成功（未查证/查证失败），1=已查证成功 */
  isChecked?: number;
}

export interface CertProgress {
  total: number;
  checked: number;
}

/** 查证批次进度（GET /chazheng/cert/batch/{batchId}/progress） */
export interface CertCheckBatchProgress {
  batchId: number;
  total: number;
  completed: number;
  success: number;
  successNoCert: number;
  failed: number;
  pending: number;
  percent: number;
  status: string;
  elapsed: string;
  estimateRemaining: string;
}