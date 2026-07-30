/** Returns true when a JWT is malformed or past exp (with 30s skew). */
export function isJwtExpired(token: string): boolean {
  const segments = token.split('.');
  if (segments.length !== 3) {
    return false;
  }

  try {
    const segment = segments[1];
    if (!segment) {
      return true;
    }
    const payload = JSON.parse(atob(segment.replace(/-/g, '+').replace(/_/g, '/'))) as { exp?: number };
    if (!payload.exp) {
      return false;
    }
    return Date.now() >= payload.exp * 1000 - 30_000;
  } catch {
    return true;
  }
}
