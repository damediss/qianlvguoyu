import type { Directive, DirectiveBinding } from 'react';
import { useAuthStore } from '../stores/authStore';

/**
 * v-permission directive for React — DOM-level permission control.
 * Usage: <Button v-permission="'exam:work-type:delete'">Delete</Button>
 *
 * Since React doesn't have native directives, export a utility hook
 * and a helper component.
 */
export function hasPermission(code: string): boolean {
  return useAuthStore.getState().hasPermission(code);
}

/**
 * Permission gate component — wraps children and only renders if the user
 * has the required permission code.
 */
import type { ReactNode } from 'react';

interface PermissionGateProps {
  code: string;
  children: ReactNode;
  fallback?: ReactNode;
}

export function PermissionGate({ code, children, fallback = null }: PermissionGateProps) {
  if (hasPermission(code)) {
    return <>{children}</>;
  }
  return <>{fallback}</>;
}

/**
 * React component that conditionally renders based on multiple permissions.
 * By default requires ALL (AND logic). Set mode="any" for OR logic.
 */
interface PermissionGuardProps {
  codes: string[];
  mode?: 'all' | 'any';
  children: ReactNode;
  fallback?: ReactNode;
}

export function PermissionGuard({ codes, mode = 'all', children, fallback = null }: PermissionGuardProps) {
  const permitted = mode === 'all'
    ? codes.every((c) => hasPermission(c))
    : codes.some((c) => hasPermission(c));

  if (permitted) {
    return <>{children}</>;
  }
  return <>{fallback}</>;
}
