/* ===== System Module TypeScript Types ===== */

// ---- Tenant ----
export interface TenantVO {
  id: number;
  code: string;
  name: string;
  status: string;
  contactName: string;
  contactPhone: string;
  remark: string;
  createdAt: string;
  updatedAt: string;
}

export interface TenantCreateReq {
  code: string;
  name: string;
  status?: string;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
}

export interface TenantUpdateReq {
  name: string;
  status: string;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
}

export interface TenantQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
}

// ---- User ----
export interface UserVO {
  id: number;
  tenantId: number;
  orgId: number;
  postId: number;
  postName: string;
  username: string;
  displayName: string;
  userType: string;
  phone: string;
  email: string;
  status: string;
  lastLoginAt: string;
  roleIds: number[];
  roleCodes: string[];
  createdAt: string;
  updatedAt: string;
}

export interface UserCreateReq {
  orgId?: number;
  postId?: number;
  username: string;
  password: string;
  displayName: string;
  userType: string;
  phone?: string;
  email?: string;
  status?: string;
  roleIds: number[];
}

export interface UserUpdateReq {
  orgId?: number;
  postId?: number;
  displayName: string;
  userType: string;
  phone?: string;
  email?: string;
  status: string;
  roleIds: number[];
}

export interface UserQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  orgId?: number;
  userType?: string;
  status?: string;
}

export interface ResetPasswordReq {
  newPassword: string;
}

// ---- Organization ----
export interface OrgTreeVO {
  id: number;
  parentId: number;
  orgType: string;
  code: string;
  name: string;
  sortOrder: number;
  status: string;
  children: OrgTreeVO[];
}

export interface OrgCreateReq {
  parentId: number;
  orgType: string;
  code: string;
  name: string;
  sortOrder?: number;
  status?: string;
}

export interface OrgUpdateReq {
  parentId: number;
  orgType: string;
  name: string;
  sortOrder?: number;
  status: string;
}

// ---- Post ----
export interface PostVO {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  sortOrder: number;
  status: string;
  remark: string;
  createdAt: string;
  updatedAt: string;
}

export interface PostCreateReq {
  code: string;
  name: string;
  sortOrder?: number;
  status?: string;
  remark?: string;
}

export interface PostUpdateReq {
  name: string;
  sortOrder?: number;
  status?: string;
  remark?: string;
}

export interface PostQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
}

// ---- Role ----
export interface RoleVO {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  roleType: string;
  status: string;
  remark: string;
  menuIds: number[];
  permissionIds: number[];
  createdAt: string;
  updatedAt: string;
}

export interface RoleCreateReq {
  code: string;
  name: string;
  roleType: string;
  status?: string;
  remark?: string;
}

export interface RoleUpdateReq {
  name: string;
  roleType: string;
  status: string;
  remark?: string;
}

export interface RoleQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  roleType?: string;
  status?: string;
}

export interface RolePermissionBindReq {
  menuIds: number[];
  permissionIds: number[];
}

// ---- Menu ----
export interface MenuTreeVO {
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
  visible: number;
  status: string;
  children: MenuTreeVO[];
}

export interface MenuCreateReq {
  parentId: number;
  menuType: string;
  code: string;
  name: string;
  path?: string;
  component?: string;
  permissionCode?: string;
  icon?: string;
  sortOrder?: number;
  visible?: number;
  status?: string;
}

export interface MenuUpdateReq {
  parentId: number;
  menuType: string;
  name: string;
  path?: string;
  component?: string;
  permissionCode?: string;
  icon?: string;
  sortOrder?: number;
  visible?: number;
  status: string;
}

// ---- Dict ----
export interface DictVO {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface DictItemVO {
  id: number;
  tenantId: number;
  dictId: number;
  itemValue: string;
  itemLabel: string;
  sortOrder: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface DictCreateReq {
  code: string;
  name: string;
  status?: string;
}

export interface DictUpdateReq {
  name: string;
  status: string;
}

export interface DictQuery {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
}

export interface DictItemCreateReq {
  itemValue: string;
  itemLabel: string;
  sortOrder?: number;
  status?: string;
}

export interface DictItemUpdateReq {
  itemLabel: string;
  sortOrder?: number;
  status: string;
}

// ---- AuditLog ----
export interface AuditLogVO {
  id: number;
  tenantId: number;
  userId: number;
  module: string;
  action: string;
  targetType: string;
  targetId: number;
  requestId: string;
  ip: string;
  userAgent: string;
  detailJson: string;
  createdAt: string;
}

export interface AuditLogQuery {
  page?: number;
  pageSize?: number;
  module?: string;
  action?: string;
  targetType?: string;
  userId?: number;
}

// ---- Permission ----
export interface RoleBriefVO {
  id: number;
  code: string;
  name: string;
}

export interface PermissionItemVO {
  id: number;
  code: string;
  name: string;
  action: string;
  status: string;
  menuName: string;
  boundRoles: RoleBriefVO[];
}

export interface PermissionMatrixVO {
  menuId: number;
  menuName: string;
  menuCode: string;
  menuSortOrder: number;
  permissions: PermissionItemVO[];
}
