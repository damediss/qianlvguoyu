import type { MenuVO } from '../types/api';

function normalizePathname(pathname: string) {
  const path = pathname.split(/[?#]/, 1)[0] || '/';
  return path.length > 1 ? path.replace(/\/+$/, '') : path;
}

/** Whether the URL belongs to the competition workspace. */
export function isCompetitionPath(pathname: string) {
  return /^\/competition(?:\/|$)/.test(normalizePathname(pathname));
}

export interface CompetitionTheoryWorkbenchContext {
  eventId: string | number;
  stageId?: string | number;
  eventWorkTypeId?: string | number;
}

/** Build the existing theory workbench URL while preserving the current competition scope. */
export function buildCompetitionTheoryWorkbenchPath({
  eventId,
  stageId,
  eventWorkTypeId,
}: CompetitionTheoryWorkbenchContext) {
  const params = new URLSearchParams({ eventId: String(eventId) });
  if (stageId !== undefined && stageId !== null) params.set('stageId', String(stageId));
  if (eventWorkTypeId !== undefined && eventWorkTypeId !== null) {
    params.set('eventWorkTypeId', String(eventWorkTypeId));
  }
  return `/competition/theory-exams?${params.toString()}`;
}

/**
 * Resolve the menu code from the server-owned menu tree instead of maintaining
 * another client-side whitelist of competition child URLs.
 *
 * Exact paths win. For an event-detail URL such as `/competition/events/123`,
 * the longest matching menu base path (`/competition/events`) is used.
 */
export function findCompetitionMenuCode(menuTree: MenuVO[], pathname: string) {
  const currentPath = normalizePathname(pathname);
  let matched: MenuVO | undefined;

  const visit = (nodes: MenuVO[]) => {
    nodes.forEach((node) => {
      const menuPath = normalizePathname(node.path || '');
      if (
        menuPath.startsWith('/competition')
        && (currentPath === menuPath || currentPath.startsWith(`${menuPath}/`))
        && (!matched || menuPath.length > normalizePathname(matched.path || '').length)
      ) {
        matched = node;
      }
      if (node.children?.length) {
        visit(node.children);
      }
    });
  };

  visit(menuTree);
  return matched?.code;
}
