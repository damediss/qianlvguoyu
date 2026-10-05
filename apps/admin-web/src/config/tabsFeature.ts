/**
 * 标签页灰度开关（《admin-web标签页外壳改造方案》§7）。
 * 双通道：编译期环境变量 VITE_ADMIN_TABS + localStorage `guoyu_admin_tabs_switch` 运行时覆盖。
 * off 时 AppShell 走旧渲染分支（无 TabBar、无标签持久化读写），新旧路径在开关存活期内并行保留。
 */
export function isTabsFeatureEnabled() {
  try {
    const override = localStorage.getItem('guoyu_admin_tabs_switch');
    if (override != null && override !== '') return override !== 'off';
  } catch {
    // 忽略 storage 异常
  }
  let envValue = 'on';
  try {
    envValue = String((import.meta as unknown as { env?: Record<string, unknown> }).env?.VITE_ADMIN_TABS ?? 'on');
  } catch {
    // 忽略 env 异常
  }
  return envValue !== 'off';
}
