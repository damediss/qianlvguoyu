import { create } from 'zustand';

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
    set({ selectedKey: key });
  },
  editingTaskId: null,
  setEditingTaskId: (id) => set({ editingTaskId: id }),
  currentProjectId: null,
  setCurrentProjectId: (id) => set({ currentProjectId: id }),
}));
