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
  captchaToken: string;
}

export interface CaptchaGenerateVO {
  captchaId: string;
  bgImage: string;
  sliderImage: string;
  sliderY: number;
}

export interface CaptchaVerifyVO {
  success: boolean;
  captchaToken: string | null;
  message: string | null;
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
  orgId?: number;
  orgType?: string;
  /** 对齐 yun_shang：org=培训机构，company=企业 */
  loginRole?: 'org' | 'company';
  roles: string[];
  permissions: string[];
}

export interface MenuVO {
  id: number;
  parentId: number;
  menuType: string;
  code: string;
  name: string;
  path: string;
  component: string;
  permissionCode: string;
  icon: string;
  sortOrder: number;
  children: MenuVO[];
}
