import { create } from 'zustand';
import { useTabStore } from './tabStore';

/**
 * 导航状态（《admin-web标签页外壳改造方案》§3.2 navStore 兼容层）。
 * selectedKey 保留为 tabStore.activeKey 的镜像（localStorage 旧 key 亦保留为灰度回滚降级读源，Q8）；
 * setSelectedKey 委托 tabStore.openTab——既有 70+ 处页面内跳转调用点零改动获得"激活/新建标签"语义。
 */
interface NavState {
  selectedKey: string;
  setSelectedKey: (key: string) => void;
  editingTaskId: number | null;
  setEditingTaskId: (id: number | null) => void;
  currentProjectId: number | null;
  setCurrentProjectId: (id: number | null) => void;
}

const SELECTED_MENU_KEY = 'guoyu_admin_selected_menu';

function readSelectedKey() {
  return localStorage.getItem(SELECTED_MENU_KEY) || 'knowledge';
}

export const useNavStore = create<NavState>((set) => ({
  selectedKey: readSelectedKey(),
  setSelectedKey: (key) => {
    localStorage.setItem(SELECTED_MENU_KEY, key);
    // 委托标签数据层（别名归一/幂等/URL 联动均在 tabStore 内）
    useTabStore.getState().openTab(key);
    // 镜像写归一后的激活 key，保持与标签栏一致
    const activeKey = useTabStore.getState().activeKey;
    set({ selectedKey: activeKey });
  },
  editingTaskId: null,
  setEditingTaskId: (id) => set({ editingTaskId: id }),
  currentProjectId: null,
  setCurrentProjectId: (id) => set({ currentProjectId: id }),
}));
