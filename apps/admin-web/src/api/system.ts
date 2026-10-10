import { apiClient, unwrap } from './client';
import type { PageResponse } from '../types/api';
import type {
  TenantVO, TenantCreateReq, TenantUpdateReq, TenantQuery,
  UserVO, UserCreateReq, UserUpdateReq, UserQuery, ResetPasswordReq,
  OrgTreeVO, OrgSimpleVO, OrgCreateReq, OrgUpdateReq,
  PostVO, PostCreateReq, PostUpdateReq, PostQuery,
  RoleVO, RoleCreateReq, RoleUpdateReq, RoleQuery, RolePermissionBindReq,
  MenuTreeVO, MenuCreateReq, MenuUpdateReq, MenuBatchVisibleReq,
  DictVO, DictItemVO, DictCreateReq, DictUpdateReq, DictQuery,
  DictItemCreateReq, DictItemUpdateReq,
  AuditLogVO, AuditLogQuery,
  PermissionMatrixVO, PermissionItemVO,
} from '../types/system';

// ---- Tenant ----
export function pageTenants(params: TenantQuery) {
  return unwrap<PageResponse<TenantVO>>(apiClient.get('/system/tenants', { params }));
}

export function getTenant(id: number) {
  return unwrap<TenantVO>(apiClient.get(`/system/tenants/${id}`));
}

export function createTenant(payload: TenantCreateReq) {
  return unwrap<TenantVO>(apiClient.post('/system/tenants', payload));
}

export function updateTenant(id: number, payload: TenantUpdateReq) {
  return unwrap<TenantVO>(apiClient.put(`/system/tenants/${id}`, payload));
}

export function deleteTenant(id: number) {
  return unwrap<void>(apiClient.delete(`/system/tenants/${id}`));
}

// ---- User ----
export function pageUsers(params: UserQuery) {
  return unwrap<PageResponse<UserVO>>(apiClient.get('/system/users', { params }));
}

export function getUser(id: number) {
  return unwrap<UserVO>(apiClient.get(`/system/users/${id}`));
}

export function createUser(payload: UserCreateReq) {
  return unwrap<UserVO>(apiClient.post('/system/users', payload));
}

export function updateUser(id: number, payload: UserUpdateReq) {
  return unwrap<UserVO>(apiClient.put(`/system/users/${id}`, payload));
}

export function resetPassword(id: number, payload: ResetPasswordReq) {
  return unwrap<void>(apiClient.post(`/system/users/${id}/reset-password`, payload));
}

export function deleteUser(id: number) {
  return unwrap<void>(apiClient.delete(`/system/users/${id}`));
}

// ---- Organization ----
export function getOrgTree() {
  return unwrap<OrgTreeVO[]>(apiClient.get('/system/orgs/tree'));
}

export function listOrgs() {
  return unwrap<OrgSimpleVO[]>(apiClient.get('/system/orgs/list'));
}

export function createOrg(payload: OrgCreateReq) {
  return unwrap<OrgTreeVO>(apiClient.post('/system/orgs', payload));
}

export function updateOrg(id: number, payload: OrgUpdateReq) {
  return unwrap<OrgTreeVO>(apiClient.put(`/system/orgs/${id}`, payload));
}

export function deleteOrg(id: number) {
  return unwrap<void>(apiClient.delete(`/system/orgs/${id}`));
}

// ---- Post ----
export function pagePosts(params: PostQuery) {
  return unwrap<PageResponse<PostVO>>(apiClient.get('/system/posts', { params }));
}

export function listAllPosts() {
  return unwrap<PostVO[]>(apiClient.get('/system/posts/all'));
}

export function getPost(id: number) {
  return unwrap<PostVO>(apiClient.get(`/system/posts/${id}`));
}

export function createPost(payload: PostCreateReq) {
  return unwrap<PostVO>(apiClient.post('/system/posts', payload));
}

export function updatePost(id: number, payload: PostUpdateReq) {
  return unwrap<PostVO>(apiClient.put(`/system/posts/${id}`, payload));
}

export function deletePost(id: number) {
  return unwrap<void>(apiClient.delete(`/system/posts/${id}`));
}

// ---- Role ----
export function pageRoles(params: RoleQuery) {
  return unwrap<PageResponse<RoleVO>>(apiClient.get('/system/roles', { params }));
}

export function getRole(id: number | string) {
  return unwrap<RoleVO>(apiClient.get(`/system/roles/${id}`));
}

export function createRole(payload: RoleCreateReq) {
  return unwrap<RoleVO>(apiClient.post('/system/roles', payload));
}

export function updateRole(id: number | string, payload: RoleUpdateReq) {
  return unwrap<RoleVO>(apiClient.put(`/system/roles/${id}`, payload));
}

export function bindRolePermissions(id: number | string, payload: RolePermissionBindReq) {
  return unwrap<RoleVO>(apiClient.put(`/system/roles/${id}/permissions`, payload));
}

export function deleteRole(id: number | string) {
  return unwrap<void>(apiClient.delete(`/system/roles/${id}`));
}

// ---- Menu ----
export function getMenuTree() {
  return unwrap<MenuTreeVO[]>(apiClient.get('/system/menus/tree'));
}

export function createMenu(payload: MenuCreateReq) {
  return unwrap<MenuTreeVO>(apiClient.post('/system/menus', payload));
}

export function updateMenu(id: number | string, payload: MenuUpdateReq) {
  return unwrap<MenuTreeVO>(apiClient.put(`/system/menus/${id}`, payload));
}

export function deleteMenu(id: number | string, opts?: { cascade?: boolean }) {
  return unwrap<number>(apiClient.delete(`/system/menus/${id}`, {
    params: opts?.cascade ? { cascade: true } : undefined,
  }));
}

export function batchUpdateMenuVisible(payload: MenuBatchVisibleReq) {
  return unwrap<void>(apiClient.put('/system/menus/batch-visible', payload));
}

// ---- Dict ----
export function pageDicts(params: DictQuery) {
  return unwrap<PageResponse<DictVO>>(apiClient.get('/system/dicts', { params }));
}

export function createDict(payload: DictCreateReq) {
  return unwrap<DictVO>(apiClient.post('/system/dicts', payload));
}

export function updateDict(id: number, payload: DictUpdateReq) {
  return unwrap<DictVO>(apiClient.put(`/system/dicts/${id}`, payload));
}

export function getDictItems(code: string) {
  return unwrap<DictItemVO[]>(apiClient.get(`/system/dicts/${code}/items`));
}

export function createDictItem(dictId: number, payload: DictItemCreateReq) {
  return unwrap<DictItemVO>(apiClient.post(`/system/dicts/${dictId}/items`, payload));
}

export function updateDictItem(itemId: number, payload: DictItemUpdateReq) {
  return unwrap<DictItemVO>(apiClient.put(`/system/dict-items/${itemId}`, payload));
}

// ---- AuditLog ----
export function pageAuditLogs(params: AuditLogQuery) {
  return unwrap<PageResponse<AuditLogVO>>(apiClient.get('/system/audit-logs', { params }));
}

// ---- Permission ----
export function getPermissionMatrix() {
  return unwrap<PermissionMatrixVO[]>(apiClient.get('/system/permissions/matrix'));
}

export function getPermissionList() {
  return unwrap<PermissionItemVO[]>(apiClient.get('/system/permissions'));
}
