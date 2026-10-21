import { create } from 'zustand';
import { useTabStore } from './tabStore';
const SELECTED_MENU_KEY = 'guoyu_admin_selected_menu';
function readSelectedKey() {
    return localStorage.getItem(SELECTED_MENU_KEY) || 'knowledge';
}
export const useNavStore = create((set) => ({
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
