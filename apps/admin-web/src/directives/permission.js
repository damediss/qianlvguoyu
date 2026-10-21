import { Fragment as _Fragment, jsx as _jsx } from "react/jsx-runtime";
import { useAuthStore } from '../stores/authStore';
/**
 * v-permission directive for React — DOM-level permission control.
 * Usage: <Button v-permission="'exam:work-type:delete'">Delete</Button>
 *
 * Since React doesn't have native directives, export a utility hook
 * and a helper component.
 */
export function hasPermission(code) {
    return useAuthStore.getState().hasPermission(code);
}
export function PermissionGate({ code, children, fallback = null }) {
    if (hasPermission(code)) {
        return _jsx(_Fragment, { children: children });
    }
    return _jsx(_Fragment, { children: fallback });
}
export function PermissionGuard({ codes, mode = 'all', children, fallback = null }) {
    const permitted = mode === 'all'
        ? codes.every((c) => hasPermission(c))
        : codes.some((c) => hasPermission(c));
    if (permitted) {
        return _jsx(_Fragment, { children: children });
    }
    return _jsx(_Fragment, { children: fallback });
}
