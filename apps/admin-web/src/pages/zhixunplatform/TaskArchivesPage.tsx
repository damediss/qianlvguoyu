import { useState, useMemo, useCallback, useEffect, type ReactNode } from 'react';
import { message, Modal } from 'antd';
import {
  ArrowLeftOutlined,
  PlusOutlined,
  SearchOutlined,
  RedoOutlined,
  ExportOutlined,
  EyeOutlined,
  EditOutlined,
  UserSwitchOutlined,
  FileTextOutlined,
  FileProtectOutlined,
  AuditOutlined,
  ToolOutlined,
  SafetyCertificateOutlined,
  FileAddOutlined,
  TrophyOutlined,
  DollarOutlined,
  FolderOpenOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  InboxOutlined,
  CloseOutlined,
  NodeIndexOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TaskArchivesPage.css';

// ============================================================
// 类型
// ============================================================
interface TaskConfigItem {
  configured: boolean;
}

interface TaskRecord {
  id: number;
  name: string;
  owner: string;
  deadline: string;
  priority: string;
  enterprise: string;
  occupation: string;
  skillLevel: string;
  region: string;
  projectType: string;
  status: string;
  planCount: number;
  startDate?: string;
  endDate?: string;
  description?: string;
  taskConfig?: Record<string, TaskConfigItem>;
  fiveM?: Record<string, TaskConfigItem>;
}

interface TaskAction {
  title: string;
  icon: ReactNode;
  danger?: boolean;
  onClick: (task: TaskRecord) => void;
}

// ============================================================
// 常量
// ============================================================
const STORAGE_KEY = 'zhixun_tasks';
const CONFIG_TOTAL = 9;

const CONFIG_LABELS: Record<string, string> = {
  humanTeacher: '师资与教务',
  humanStudent: '参训学员',
  machine: '机构与场地',
  materialCourse: '课程与课件',
  materialCost: '预算与费用',
  lawSchedule: '分班排课',
  lawMode: '培养方案',
  lawRule: '业务规则',
  environment: '开班与合规',
};

const DEFAULT_TASKS: TaskRecord[] = [
  { id: 1, name: '2026年度安全培训计划编制', owner: '张明', deadline: '2026-06-15', priority: '高', enterprise: '广西建工集团', occupation: '安全员', skillLevel: '高级', region: '南宁', projectType: '安全培训', status: '进行中', planCount: 120, startDate: '2026-03-01', endDate: '2026-06-30' },
  { id: 2, name: '电工高级班认定材料整理', owner: '李芳', deadline: '2026-05-25', priority: '高', enterprise: '南宁供电局', occupation: '电工', skillLevel: '高级', region: '南宁', projectType: '资质认证', status: '进行中', planCount: 42 },
  { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', deadline: '2026-05-30', priority: '中', enterprise: '柳州钢铁有限公司', occupation: '焊工', skillLevel: '中级', region: '柳州', projectType: '新员工培训', status: '草稿', planCount: 36 },
  { id: 4, name: '焊工中级班排课协调', owner: '赵丽', deadline: '2026-05-22', priority: '中', enterprise: '桂林旅游股份公司', occupation: '焊工', skillLevel: '中级', region: '桂林', projectType: '资质认证', status: '待审批', planCount: 28 },
  { id: 5, name: '培训档案年度归档', owner: '陈伟', deadline: '2026-06-30', priority: '低', enterprise: '梧州港务集团', occupation: '起重工', skillLevel: '初级', region: '梧州', projectType: '管理研修', status: '草稿', planCount: 15 },
];

const PRIORITY_CLASS: Record<string, string> = { '高': 'priority-high', '中': 'priority-medium', '低': 'priority-low' };
const STATUS_CLASS: Record<string, string> = { '进行中': 'status-active', '草稿': 'status-draft', '待审批': 'status-pending', '已完成': 'status-done' };

function taskCode(id: number) {
  return 'TASK-' + String(id).padStart(4, '0');
}

function isOverdue(task: TaskRecord) {
  if (!task.deadline || task.status === '已完成') return false;
  return new Date(task.deadline) < new Date(new Date().toDateString());
}

function getConfigProgress(task: TaskRecord) {
  const cfg = task.taskConfig || task.fiveM;
  if (!cfg) return { done: 0, total: CONFIG_TOTAL };
  const done = Object.values(cfg).filter((c) => c && c.configured).length;
  return { done, total: CONFIG_TOTAL };
}

function loadTasks(): TaskRecord[] | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch { /* ignore */ }
  return null;
}

function saveTasks(tasks: TaskRecord[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks));
}

// ============================================================
// 主页面
// ============================================================
export function TaskArchivesPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const setEditingTaskId = useNavStore((s) => s.setEditingTaskId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
  const [tasks, setTasks] = useState<TaskRecord[]>(() => {
    return loadTasks() || DEFAULT_TASKS;
  });
  const [quickFilter, setQuickFilter] = useState('all');
  const [searchText, setSearchText] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterPriority, setFilterPriority] = useState('');
  const [filterOccupation, setFilterOccupation] = useState('');
  const [filterRegion, setFilterRegion] = useState('');
  const [drawerTask, setDrawerTask] = useState<TaskRecord | null>(null);

  // localStorage 持久化：每次 tasks 变化时写入
  useEffect(() => {
    saveTasks(tasks);
  }, [tasks]);

  // URL ?id= 打开详情
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const idParam = params.get('id');
    if (idParam) {
      const id = parseInt(idParam);
      const task = tasks.find((t) => t.id === id);
      if (task) setDrawerTask(task);
    }
  }, [tasks]);

  const stats = useMemo(() => {
    return {
      all: tasks.length,
      active: tasks.filter((t) => t.status === '进行中').length,
      draft: tasks.filter((t) => ['草稿', '待审批'].includes(t.status)).length,
      overdue: tasks.filter((t) => isOverdue(t)).length,
    };
  }, [tasks]);

  const filteredTasks = useMemo(() => {
    return tasks.filter((t) => {
      if (quickFilter === '进行中' && t.status !== '进行中') return false;
      if (quickFilter === 'draft' && !['草稿', '待审批'].includes(t.status)) return false;
      if (quickFilter === 'overdue' && !isOverdue(t)) return false;
      if (filterStatus && t.status !== filterStatus) return false;
      if (filterPriority && t.priority !== filterPriority) return false;
      if (filterOccupation && t.occupation !== filterOccupation) return false;
      if (filterRegion && t.region !== filterRegion) return false;
      if (searchText) {
        const hay = [t.name, t.enterprise, t.owner, t.occupation, taskCode(t.id)].join(' ').toLowerCase();
        if (!hay.includes(searchText.toLowerCase())) return false;
      }
      return true;
    });
  }, [tasks, quickFilter, searchText, filterStatus, filterPriority, filterOccupation, filterRegion]);

  const resetFilters = useCallback(() => {
    setSearchText('');
    setFilterStatus('');
    setFilterPriority('');
    setFilterOccupation('');
    setFilterRegion('');
    setQuickFilter('all');
  }, []);

  const handleDelete = useCallback(
    (id: number) => {
      Modal.confirm({
        title: '确定删除该任务？',
        onOk: () => {
          setTasks((prev) => prev.filter((t) => t.id !== id));
          message.success('任务已删除');
        },
      });
    },
    [],
  );

  const handleEditFromDrawer = useCallback(() => {
    if (drawerTask) {
      setEditingTaskId(drawerTask.id);
      navigateTo('zhixun-create-task');
    }
  }, [drawerTask, setEditingTaskId, navigateTo]);

  const handleProcessFromDrawer = useCallback(() => {
    if (drawerTask) {
      setCurrentProjectId(drawerTask.id);
      navigateTo('zhixun-training-implementation');
    }
  }, [drawerTask, setCurrentProjectId, navigateTo]);

  const navigateProject = useCallback(
    (task: TaskRecord, key: string) => {
      setCurrentProjectId(task.id);
      navigateTo(key);
    },
    [navigateTo, setCurrentProjectId],
  );

  const taskActions: TaskAction[] = [
    { title: '查看', icon: <EyeOutlined />, onClick: (task) => setDrawerTask(task) },
    { title: '编辑', icon: <EditOutlined />, onClick: (task) => { setEditingTaskId(task.id); navigateTo('zhixun-create-task'); } },
    { title: '人才识别', icon: <UserSwitchOutlined />, onClick: (task) => navigateProject(task, 'zhixun-talent-identification') },
    { title: '初步方案', icon: <FileTextOutlined />, onClick: (task) => navigateProject(task, 'zhixun-preliminary-plan') },
    { title: '最终方案', icon: <FileProtectOutlined />, onClick: (task) => navigateProject(task, 'zhixun-final-plan') },
    { title: '开班申报', icon: <AuditOutlined />, onClick: (task) => navigateProject(task, 'zhixun-class-declaration') },
    { title: '培训实施', icon: <ToolOutlined />, onClick: (task) => navigateProject(task, 'zhixun-training-implementation') },
    { title: '认定准备', icon: <SafetyCertificateOutlined />, onClick: (task) => navigateProject(task, 'zhixun-certification') },
    { title: '申报服务', icon: <FileAddOutlined />, onClick: (task) => navigateProject(task, 'zhixun-declaration-service') },
    { title: '培训结业', icon: <TrophyOutlined />, onClick: (task) => navigateProject(task, 'zhixun-graduation') },
    { title: '项目回款', icon: <DollarOutlined />, onClick: (task) => navigateProject(task, 'zhixun-refund') },
    { title: '资料档案', icon: <FolderOpenOutlined />, onClick: (task) => navigateProject(task, 'zhixun-material-archives') },
    { title: '删除', icon: <DeleteOutlined />, danger: true, onClick: (task) => handleDelete(task.id) },
  ];

  return (
    <div className="task-archives-page">
      {/* 顶部栏 */}
      <header className="ta-topbar">
        <div className="ta-topbar-left">
          <a className="ta-back-link" onClick={() => navigateTo('zhixun-task-management')}>
            <ArrowLeftOutlined /> 返回任务管理
          </a>
          <div>
            <h1 className="ta-title">任务档案</h1>
            <span className="ta-sub">培训任务台账 · 查询、筛选与详情</span>
          </div>
        </div>
        <div className="ta-topbar-actions">
          <button type="button" className="ta-btn ta-btn-secondary" onClick={() => navigateTo('zhixun-material-archives')}>
            <FolderOpenOutlined /> 资料档案
          </button>
          <button type="button" className="ta-btn ta-btn-primary" onClick={() => navigateTo('zhixun-create-task')}>
            <PlusOutlined />
            新建任务
          </button>
        </div>
      </header>

      <main className="ta-page">
        {/* 统计卡片 */}
        <div className="ta-stat-row">
          <div className={`ta-stat-card${quickFilter === 'all' ? ' active' : ''}`} onClick={() => setQuickFilter('all')}>
            <div className="ta-stat-val">{stats.all}</div>
            <div className="ta-stat-lbl">全部任务</div>
          </div>
          <div className={`ta-stat-card${quickFilter === '进行中' ? ' active' : ''}`} onClick={() => setQuickFilter('进行中')}>
            <div className="ta-stat-val">{stats.active}</div>
            <div className="ta-stat-lbl">进行中</div>
          </div>
          <div className={`ta-stat-card${quickFilter === 'draft' ? ' active' : ''}`} onClick={() => setQuickFilter('draft')}>
            <div className="ta-stat-val">{stats.draft}</div>
            <div className="ta-stat-lbl">草稿 / 待审批</div>
          </div>
          <div className={`ta-stat-card${quickFilter === 'overdue' ? ' active' : ''}`} onClick={() => setQuickFilter('overdue')}>
            <div className="ta-stat-val">{stats.overdue}</div>
            <div className="ta-stat-lbl">已逾期</div>
          </div>
        </div>

        {/* 筛选工具栏 */}
        <div className="ta-toolbar">
          <div className="ta-search-box">
            <SearchOutlined />
            <input
              type="text"
              placeholder="搜索任务名称、企业、负责人…"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </div>
          <select className="ta-filter-select" value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="">全部状态</option>
            <option value="进行中">进行中</option>
            <option value="草稿">草稿</option>
            <option value="待审批">待审批</option>
            <option value="已完成">已完成</option>
          </select>
          <select className="ta-filter-select" value={filterPriority} onChange={(e) => setFilterPriority(e.target.value)}>
            <option value="">全部优先级</option>
            <option value="高">高</option>
            <option value="中">中</option>
            <option value="低">低</option>
          </select>
          <select className="ta-filter-select" value={filterOccupation} onChange={(e) => setFilterOccupation(e.target.value)}>
            <option value="">全部工种</option>
            <option value="电工">电工</option>
            <option value="焊工">焊工</option>
            <option value="起重工">起重工</option>
            <option value="安全员">安全员</option>
          </select>
          <select className="ta-filter-select" value={filterRegion} onChange={(e) => setFilterRegion(e.target.value)}>
            <option value="">全部区域</option>
            <option value="南宁">南宁</option>
            <option value="桂林">桂林</option>
            <option value="柳州">柳州</option>
            <option value="玉林">玉林</option>
            <option value="梧州">梧州</option>
          </select>
          <button type="button" className="ta-btn ta-btn-secondary" onClick={resetFilters}>
            <RedoOutlined /> 重置
          </button>
          <button type="button" className="ta-btn ta-btn-secondary" onClick={() => message.info(`导出演示：共 ${filteredTasks.length} 条任务`)}>
            <ExportOutlined /> 导出
          </button>
        </div>

        {/* 任务列表 */}
        <div className="ta-card">
          {filteredTasks.length > 0 ? (
            <div className="ta-table-wrap">
              <table className="ta-table">
                <thead>
                  <tr>
                    <th>任务</th>
                    <th>企业</th>
                    <th>工种 / 等级</th>
                    <th>区域</th>
                    <th>负责人</th>
                    <th>截止日期</th>
                    <th>优先级</th>
                    <th>状态</th>
                    <th>配置</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredTasks.map((task) => {
                    const prog = getConfigProgress(task);
                    const pct = Math.round((prog.done / prog.total) * 100);
                    const overdue = isOverdue(task);

                    return (
                      <tr key={task.id} className="clickable" onClick={() => setDrawerTask(task)}>
                        <td>
                          <div className="task-name">{task.name}</div>
                          <div className="task-code">{taskCode(task.id)}</div>
                        </td>
                        <td>{task.enterprise || '—'}</td>
                        <td>{task.occupation || '—'}{task.skillLevel ? ' · ' + task.skillLevel : ''}</td>
                        <td>{task.region || '—'}</td>
                        <td>{task.owner}</td>
                        <td className={overdue ? 'overdue-text' : ''}>
                          {task.deadline || '—'}
                          {overdue && <ExclamationCircleOutlined title="已逾期" />}
                        </td>
                        <td><span className={`priority-badge ${PRIORITY_CLASS[task.priority] || 'priority-medium'}`}>{task.priority}</span></td>
                        <td><span className={`status-badge ${STATUS_CLASS[task.status || '草稿'] || 'status-draft'}`}>{task.status || '草稿'}</span></td>
                        <td className="progress-wrap">
                          <span className="progress-text">{prog.done}/{prog.total}</span>
                          <div className="progress-bar">
                            <div className="progress-fill" style={{ width: `${pct}%` }} />
                          </div>
                        </td>
                        <td>
                          <div className="action-btns" onClick={(e) => e.stopPropagation()}>
                            {taskActions.map((action) => (
                              <button
                                key={action.title}
                                type="button"
                                className={`action-btn${action.danger ? ' delete' : ''}`}
                                title={action.title}
                                onClick={() => action.onClick(task)}
                              >
                                {action.icon}
                              </button>
                            ))}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="ta-empty">
              <InboxOutlined />
              <p>暂无符合条件的任务</p>
              <button type="button" className="ta-btn ta-btn-primary" onClick={() => navigateTo('zhixun-create-task')}>
                创建第一个任务
              </button>
            </div>
          )}
        </div>
      </main>

      {/* 详情抽屉 */}
      <div
        className={`ta-drawer-overlay${drawerTask ? ' show' : ''}`}
        onClick={(e) => {
          if (e.currentTarget === e.target) setDrawerTask(null);
        }}
      >
        <aside className="ta-drawer" aria-hidden={!drawerTask}>
          {drawerTask && (
            <>
              <div className="ta-drawer-head">
                <div>
                  <h2>{drawerTask.name}</h2>
                  <span className="task-code">{taskCode(drawerTask.id)}</span>
                </div>
                <button type="button" className="ta-drawer-close" onClick={() => setDrawerTask(null)}>
                  <CloseOutlined />
                </button>
              </div>
              <div className="ta-drawer-body">
                <div className="ta-detail-section">
                  <h4>基本信息</h4>
                  <div className="ta-detail-grid">
                    <div className="ta-detail-item"><label>负责人</label><span>{drawerTask.owner}</span></div>
                    <div className="ta-detail-item"><label>截止日期</label><span>{drawerTask.deadline || '—'}</span></div>
                    <div className="ta-detail-item"><label>优先级</label><span>{drawerTask.priority}</span></div>
                    <div className="ta-detail-item"><label>状态</label><span>{drawerTask.status || '草稿'}</span></div>
                  </div>
                  {drawerTask.description && <p className="ta-detail-description">{drawerTask.description}</p>}
                </div>
                <div className="ta-detail-section">
                  <h4>项目信息</h4>
                  <div className="ta-detail-grid">
                    <div className="ta-detail-item"><label>委托企业</label><span>{drawerTask.enterprise || '—'}</span></div>
                    <div className="ta-detail-item"><label>所属区域</label><span>{drawerTask.region || '—'}</span></div>
                    <div className="ta-detail-item"><label>培训工种</label><span>{drawerTask.occupation || '—'} · {drawerTask.skillLevel || '—'}</span></div>
                    <div className="ta-detail-item"><label>项目类型</label><span>{drawerTask.projectType || '—'}</span></div>
                    <div className="ta-detail-item"><label>计划人数</label><span>{drawerTask.planCount || '—'} 人</span></div>
                    <div className="ta-detail-item"><label>计划周期</label><span>{drawerTask.startDate || '—'} ~ {drawerTask.endDate || '—'}</span></div>
                  </div>
                </div>
                <div className="ta-detail-section">
                  <h4>任务配置（{getConfigProgress(drawerTask).done}/{getConfigProgress(drawerTask).total}）</h4>
                  <div className="ta-config-tags">
                    {Object.keys(CONFIG_LABELS).map((key) => {
                      const cfg = (drawerTask.taskConfig || drawerTask.fiveM)?.[key];
                      const done = cfg && cfg.configured;
                      return (
                        <span key={key} className={`ta-config-tag${done ? ' done' : ''}`}>
                          {CONFIG_LABELS[key]}{done ? ' ✓' : ''}
                        </span>
                      );
                    })}
                  </div>
                </div>
              </div>
              <div className="ta-drawer-actions">
                <button type="button" className="ta-btn ta-btn-primary" onClick={handleEditFromDrawer}>
                  <EditOutlined /> 编辑任务
                </button>
                <button type="button" className="ta-btn ta-btn-secondary" onClick={handleProcessFromDrawer}>
                  <NodeIndexOutlined /> 培训流程
                </button>
              </div>
            </>
          )}
        </aside>
      </div>
    </div>
  );
}
