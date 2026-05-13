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
  tokenType: string;
  expiresIn: number;
}

export interface CurrentUserVO {
  userId: number;
  tenantId: number;
  username: string;
  displayName: string;
  userType: string;
  roleCodes: string[];
  permissions: string[];
}
