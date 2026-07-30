import type { ReactNode } from 'react';
import { useAuthStore } from '../stores/authStore';

export function hasPermission(code?: string): boolean {
  return code ? useAuthStore.getState().hasPermission(code) : true;
}

interface PermissionGateProps {
  code?: string;
  children: ReactNode;
  fallback?: ReactNode;
}

export function PermissionGate({ code, children, fallback = null }: PermissionGateProps) {
  if (hasPermission(code)) {
    return <>{children}</>;
  }
  return <>{fallback}</>;
}

interface PermissionGuardProps {
  codes: string[];
  mode?: 'all' | 'any';
  children: ReactNode;
  fallback?: ReactNode;
}

export function PermissionGuard({ codes, mode = 'all', children, fallback = null }: PermissionGuardProps) {
  const permitted = mode === 'all'
    ? codes.every((code) => hasPermission(code))
    : codes.some((code) => hasPermission(code));

  if (permitted) {
    return <>{children}</>;
  }
  return <>{fallback}</>;
}
