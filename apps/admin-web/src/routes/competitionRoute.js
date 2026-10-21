function normalizePathname(pathname) {
    const path = pathname.split(/[?#]/, 1)[0] || '/';
    return path.length > 1 ? path.replace(/\/+$/, '') : path;
}
/** Whether the URL belongs to the competition workspace. */
export function isCompetitionPath(pathname) {
    return /^\/competition(?:\/|$)/.test(normalizePathname(pathname));
}
/** Build the existing theory workbench URL while preserving the current competition scope. */
export function buildCompetitionTheoryWorkbenchPath({ eventId, stageId, eventWorkTypeId, }) {
    const params = new URLSearchParams({ eventId: String(eventId) });
    if (stageId !== undefined && stageId !== null)
        params.set('stageId', String(stageId));
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
export function findCompetitionMenuCode(menuTree, pathname) {
    const currentPath = normalizePathname(pathname);
    let matched;
    const visit = (nodes) => {
        nodes.forEach((node) => {
            const menuPath = normalizePathname(node.path || '');
            if (menuPath.startsWith('/competition')
                && (currentPath === menuPath || currentPath.startsWith(`${menuPath}/`))
                && (!matched || menuPath.length > normalizePathname(matched.path || '').length)) {
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
/**
 * 全站 path → 菜单码反查（标签页 URL 联动用，《admin-web标签页外壳改造方案》§3.4）。
 * findCompetitionMenuCode 的泛化版：不限竞赛前缀，精确优先、其次最长前缀。
 * 调用方需自行排除隐藏路由（hiddenRoutes.isHiddenRoutePath）。
 */
export function findMenuCodeByPath(menuTree, pathname) {
    const currentPath = normalizePathname(pathname);
    if (currentPath === '/')
        return undefined;
    let matched;
    const visit = (nodes) => {
        nodes.forEach((node) => {
            const menuPath = normalizePathname(node.path || '');
            if (menuPath
                && menuPath !== '/'
                && (currentPath === menuPath || currentPath.startsWith(`${menuPath}/`))
                && (!matched || menuPath.length > normalizePathname(matched.path || '').length)) {
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
