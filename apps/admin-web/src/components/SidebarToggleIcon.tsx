/**
 * 侧边栏折叠切换图标（"分栏"造型）：圆角矩形外框 + 左侧窄条面板。
 * collapsed 时整体水平镜像，示意面板已收起；颜色随 currentColor，悬停反色由 CSS 控制。
 */
export function SidebarToggleIcon({ collapsed }: { collapsed: boolean }) {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      style={{
        transform: collapsed ? 'scaleX(-1)' : undefined,
        transition: 'transform 0.2s ease',
      }}
    >
      <rect x="3" y="4" width="18" height="16" rx="3" stroke="currentColor" strokeWidth="1.8" />
      <path d="M9.5 4v16" stroke="currentColor" strokeWidth="1.8" />
      <rect x="4.6" y="5.6" width="3.3" height="12.8" rx="1" fill="currentColor" opacity="0.25" />
    </svg>
  );
}
