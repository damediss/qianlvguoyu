/**
 * 隐藏路由清单（《admin-web标签页外壳改造方案》附录 A）。
 * URL 联动双向排除：命中任一匹配器的 path，标签激活时不写入 URL，
 * popstate 反查时也不为它开标签（直接交给对应 shell 的覆盖机制）。
 */
const HIDDEN_ROUTE_MATCHERS: RegExp[] = [
  // 竞赛工作区（routes/competitionRoute.ts isCompetitionPath 同口径）
  /^\/competition(?:\/|$)/,
  // AI 同步学统计（main.tsx ReviewSyncStatsShell）
  /^\/training\/exam-lists\/\d+\/review-sync-stats/,
  // 毕业典礼班级详情（main.tsx GraduationClassDetailShell）
  /^\/training\/graduation\/classes\/\d+\/?$/,
  // 采集管理（main.tsx CollectionManageShell）
  /^\/certification\/practical-collection\/[^/]+\/manage\/?$/,
  // 教材大纲编辑（main.tsx TextbookOutlineShell）
  /^\/textbook\/outline\/[^/]+\/?$/,
  // 教材编写管理（二期，main.tsx TextbookCollaborationShell）
  /^\/textbook\/collaboration\/[^/]+\/?$/,
  // 教材主笔编写工作台（二期，main.tsx TextbookWriteShell）
  /^\/textbook\/write\/[^/]+\/?$/,
  // 教材审定工作台（二期，main.tsx TextbookReviewShell）
  /^\/textbook\/review\/[^/]+\/?$/,
];

function normalizePathname(pathname: string) {
  const path = (pathname || '').split(/[?#]/, 1)[0] || '/';
  return path.length > 1 ? path.replace(/\/+$/, '') : path;
}

/** path（可含 query）是否命中隐藏路由 */
export function isHiddenRoutePath(pathname: string) {
  const path = normalizePathname(pathname);
  if (path === '/') return false;
  return HIDDEN_ROUTE_MATCHERS.some((matcher) => matcher.test(path));
}
