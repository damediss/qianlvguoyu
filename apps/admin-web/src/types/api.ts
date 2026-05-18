export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface LoginReq {
  tenantCode: string;
  username: string;
  password: string;
}

export interface LoginVO {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tenantId: number;
  user: CurrentUserVO;
}

export interface CurrentUserVO {
  id: number;
  tenantId: number;
  username: string;
  displayName: string;
  userType: string;
  roles: string[];
  permissions: string[];
}
