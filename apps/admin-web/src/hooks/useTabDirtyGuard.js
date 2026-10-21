import { useUnsavedChangesBlocker } from './useUnsavedChangesBlocker';
/**
 * 页面级 dirty 关闭确认框架（§3.8 / Q10 试点接入入口）。
 * dirty=true 期间注册守卫：关闭该页标签、浏览器前进/后退、beforeunload 均弹确认；
 * scope 自动落到本页所在标签（§3.8），多标签互不串台。
 * 全站全量接入为二期滚动项，一期仅高风险表单页试点。
 */
export function useTabDirtyGuard(dirty, message) {
    useUnsavedChangesBlocker(dirty, message ? `${message}，未保存的修改将会丢失，确定要离开吗？` : undefined);
}
