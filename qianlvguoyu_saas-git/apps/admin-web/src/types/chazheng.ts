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
}

export interface ChazhengUserQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  enterpriseId?: number;
  ymd?: string;
}

export interface CertProgress {
  total: number;
  checked: number;
}