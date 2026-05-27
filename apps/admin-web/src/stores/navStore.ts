import { create } from 'zustand';

interface NavState {
  selectedKey: string;
  setSelectedKey: (key: string) => void;
  editingTaskId: number | null;
  setEditingTaskId: (id: number | null) => void;
  currentProjectId: number | null;
  setCurrentProjectId: (id: number | null) => void;
}

export const useNavStore = create<NavState>((set) => ({
  selectedKey: 'knowledge',
  setSelectedKey: (key) => set({ selectedKey: key }),
  editingTaskId: null,
  setEditingTaskId: (id) => set({ editingTaskId: id }),
  currentProjectId: null,
  setCurrentProjectId: (id) => set({ currentProjectId: id }),
}));
