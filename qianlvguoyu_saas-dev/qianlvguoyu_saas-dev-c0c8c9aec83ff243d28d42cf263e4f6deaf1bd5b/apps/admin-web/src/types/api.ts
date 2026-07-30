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

export interface MenuVO {
  id: number;
  parentId: number;
  menuType: string;
  code: string;
  name: string;
  path?: string;
  component?: string;
  permissionCode?: string;
  icon?: string;
  sortOrder?: number;
  children?: MenuVO[];
}
