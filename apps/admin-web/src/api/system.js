import { apiClient, unwrap } from './client';
// ---- Tenant ----
export function pageTenants(params) {
    return unwrap(apiClient.get('/system/tenants', { params }));
}
export function getTenant(id) {
    return unwrap(apiClient.get(`/system/tenants/${id}`));
}
export function createTenant(payload) {
    return unwrap(apiClient.post('/system/tenants', payload));
}
export function updateTenant(id, payload) {
    return unwrap(apiClient.put(`/system/tenants/${id}`, payload));
}
export function deleteTenant(id) {
    return unwrap(apiClient.delete(`/system/tenants/${id}`));
}
// ---- User ----
export function pageUsers(params) {
    return unwrap(apiClient.get('/system/users', { params }));
}
export function getUser(id) {
    return unwrap(apiClient.get(`/system/users/${id}`));
}
export function createUser(payload) {
    return unwrap(apiClient.post('/system/users', payload));
}
export function updateUser(id, payload) {
    return unwrap(apiClient.put(`/system/users/${id}`, payload));
}
export function resetPassword(id, payload) {
    return unwrap(apiClient.post(`/system/users/${id}/reset-password`, payload));
}
export function deleteUser(id) {
    return unwrap(apiClient.delete(`/system/users/${id}`));
}
// ---- Organization ----
export function getOrgTree() {
    return unwrap(apiClient.get('/system/orgs/tree'));
}
export function listOrgs() {
    return unwrap(apiClient.get('/system/orgs/list'));
}
export function createOrg(payload) {
    return unwrap(apiClient.post('/system/orgs', payload));
}
export function updateOrg(id, payload) {
    return unwrap(apiClient.put(`/system/orgs/${id}`, payload));
}
export function deleteOrg(id) {
    return unwrap(apiClient.delete(`/system/orgs/${id}`));
}
// ---- Post ----
export function pagePosts(params) {
    return unwrap(apiClient.get('/system/posts', { params }));
}
export function listAllPosts() {
    return unwrap(apiClient.get('/system/posts/all'));
}
export function getPost(id) {
    return unwrap(apiClient.get(`/system/posts/${id}`));
}
export function createPost(payload) {
    return unwrap(apiClient.post('/system/posts', payload));
}
export function updatePost(id, payload) {
    return unwrap(apiClient.put(`/system/posts/${id}`, payload));
}
export function deletePost(id) {
    return unwrap(apiClient.delete(`/system/posts/${id}`));
}
// ---- Role ----
export function pageRoles(params) {
    return unwrap(apiClient.get('/system/roles', { params }));
}
export function getRole(id) {
    return unwrap(apiClient.get(`/system/roles/${id}`));
}
export function createRole(payload) {
    return unwrap(apiClient.post('/system/roles', payload));
}
export function updateRole(id, payload) {
    return unwrap(apiClient.put(`/system/roles/${id}`, payload));
}
export function bindRolePermissions(id, payload) {
    return unwrap(apiClient.put(`/system/roles/${id}/permissions`, payload));
}
export function deleteRole(id) {
    return unwrap(apiClient.delete(`/system/roles/${id}`));
}
// ---- Menu ----
export function getMenuTree() {
    return unwrap(apiClient.get('/system/menus/tree'));
}
export function createMenu(payload) {
    return unwrap(apiClient.post('/system/menus', payload));
}
export function updateMenu(id, payload) {
    return unwrap(apiClient.put(`/system/menus/${id}`, payload));
}
export function deleteMenu(id, opts) {
    return unwrap(apiClient.delete(`/system/menus/${id}`, {
        params: opts?.cascade ? { cascade: true } : undefined,
    }));
}
export function batchUpdateMenuVisible(payload) {
    return unwrap(apiClient.put('/system/menus/batch-visible', payload));
}
// ---- Dict ----
export function pageDicts(params) {
    return unwrap(apiClient.get('/system/dicts', { params }));
}
export function createDict(payload) {
    return unwrap(apiClient.post('/system/dicts', payload));
}
export function updateDict(id, payload) {
    return unwrap(apiClient.put(`/system/dicts/${id}`, payload));
}
export function getDictItems(code) {
    return unwrap(apiClient.get(`/system/dicts/${code}/items`));
}
export function createDictItem(dictId, payload) {
    return unwrap(apiClient.post(`/system/dicts/${dictId}/items`, payload));
}
export function updateDictItem(itemId, payload) {
    return unwrap(apiClient.put(`/system/dict-items/${itemId}`, payload));
}
// ---- AuditLog ----
export function pageAuditLogs(params) {
    return unwrap(apiClient.get('/system/audit-logs', { params }));
}
// ---- Permission ----
export function getPermissionMatrix() {
    return unwrap(apiClient.get('/system/permissions/matrix'));
}
export function getPermissionList() {
    return unwrap(apiClient.get('/system/permissions'));
}
