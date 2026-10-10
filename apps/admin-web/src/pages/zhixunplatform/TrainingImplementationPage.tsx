import { useState, useMemo, useCallback, useEffect } from 'react';
import { Typography, Select, Input, Button, Table, Modal, Tabs, message } from 'antd';
import {
  ArrowLeftOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  ToolOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  VideoCameraOutlined,
  FileImageOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  SaveOutlined,
  InfoCircleOutlined,
  CalendarOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './TrainingImplementationPage.css';

// ============================================================
// 类型
// ============================================================
interface Session {
  id: number;
  date: string;
  courseName: string;
  timeRange: string;
  expected: number;
  present: number;
  photos: number;
  videos: number;
  checked: boolean;
}

interface ClassRecord {
  classId: number;
  className: string;
  studentCount: number;
  progress: number;
  attendanceRate: number;
  status: string;
  sessions: Session[];
  evidenceCount: { photos: number; videos: number };
}

interface LogEntry {
  id: number;
  classId: number;
  className: string;
  date: string;
  author: string;
  content: string;
}

interface ExcEntry {
  id: number;
  classId: number;
  className: string;
  date: string;
  student: string;
  type: string;
  desc: string;
  resolved: boolean;
}

// ============================================================
// 常量
// ============================================================
const DEFAULT_TASKS = [
  { id: 1, name: '2026年度安全培训计划编制', owner: '张明', enterprise: '广西建工集团', occupation: '安全员', planCount: 120 },
  { id: 2, name: '电工高级班认定材料整理', owner: '李芳', enterprise: '南宁供电局', occupation: '电工', planCount: 42 },
  { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', enterprise: '柳州钢铁有限公司', occupation: '焊工', planCount: 36 },
  { id: 4, name: '焊工中级班排课协调', owner: '赵丽', enterprise: '桂林旅游股份公司', occupation: '焊工', planCount: 28 },
  { id: 5, name: '培训档案年度归档', owner: '陈伟', enterprise: '梧州港务集团', occupation: '起重工', planCount: 15 },
];

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

// ============================================================
// Mock 数据
// ============================================================
function buildMockSessions(classId: number, _studentCount: number): Session[] {
  return [
    { id: classId * 1000 + 1, date: '2026-07-01', courseName: '安全生产基础知识', timeRange: '09:00-12:00', expected: 30, present: 30, photos: 0, videos: 0, checked: true },
    { id: classId * 1000 + 2, date: '2026-07-02', courseName: '安全操作规程', timeRange: '09:00-12:00', expected: 30, present: 29, photos: 0, videos: 0, checked: true },
    { id: classId * 1000 + 3, date: '2026-07-03', courseName: '应急救援演练', timeRange: '14:00-17:00', expected: 30, present: 28, photos: 0, videos: 0, checked: true },
    { id: classId * 1000 + 4, date: '2026-07-08', courseName: '安全法律法规', timeRange: '09:00-12:00', expected: 30, present: 30, photos: 0, videos: 0, checked: false },
    { id: classId * 1000 + 5, date: '2026-07-10', courseName: '事故案例分析', timeRange: '09:00-12:00', expected: 30, present: 30, photos: 0, videos: 0, checked: false },
  ];
}

function buildMockRecords(projectId: number): ClassRecord[] {
  const task = DEFAULT_TASKS.find((t) => t.id === projectId);
  if (!task) return [];
  const s1 = buildMockSessions(1, 30);
  const s2 = buildMockSessions(2, 25);
  return [
    {
      classId: 1, className: `${task.occupation || '电工'}高级工1班`, studentCount: 30,
      progress: 60, attendanceRate: 96.7, status: '正常',
      sessions: s1, evidenceCount: { photos: 2, videos: 1 },
    },
    {
      classId: 2, className: `${task.occupation || '电工'}中级工1班`, studentCount: 25,
      progress: 40, attendanceRate: 92, status: '正常',
      sessions: s2, evidenceCount: { photos: 1, videos: 0 },
    },
  ];
}

// ============================================================
// 主页面
// ============================================================
export function TrainingImplementationPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
  const [records, setRecords] = useState<ClassRecord[]>([]);
  const [filterClass, setFilterClass] = useState('');
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [exceptions, setExceptions] = useState<ExcEntry[]>([]);
  const [logIdCounter, setLogIdCounter] = useState(1);
  const [excIdCounter, setExcIdCounter] = useState(1);
  const [activeTab, setActiveTab] = useState('overview');

  // modals
  const [detailModal, setDetailModal] = useState<ClassRecord | null>(null);
  const [logModalOpen, setLogModalOpen] = useState(false);
  const [excModalOpen, setExcModalOpen] = useState(false);
  const [logDate, setLogDate] = useState(todayStr());
  const [logClassId, setLogClassId] = useState<number | undefined>();
  const [logContent, setLogContent] = useState('');
  const [excDate, setExcDate] = useState(todayStr());
  const [excClassId, setExcClassId] = useState<number | undefined>();
  const [excStudent, setExcStudent] = useState('');
  const [excType, setExcType] = useState('缺勤');
  const [excDesc, setExcDesc] = useState('');

  const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;

  useEffect(() => {
    if (selectedProjectId) {
      setRecords(buildMockRecords(+selectedProjectId));
      setLogs([]);
      setExceptions([]);
      setLogIdCounter(1);
      setExcIdCounter(1);
    } else {
      setRecords([]);
    }
  }, [selectedProjectId]);

  const handleProjectChange = (v: string) => {
    setSelectedProjectId(v);
    setCurrentProjectId(v ? +v : null);
    setFilterClass('');
  };

  const filteredRecords = useMemo(() => {
    if (!filterClass) return records;
    return records.filter((r) => String(r.classId) === filterClass);
  }, [records, filterClass]);

  const stats = useMemo(() => {
    const list = records;
    const att = list.length ? Math.round(list.reduce((s, r) => s + (r.attendanceRate || 0), 0) / list.length * 10) / 10 : 0;
    const prog = list.length ? Math.round(list.reduce((s, r) => s + (r.progress || 0), 0) / list.length) : 0;
    const pendingExc = exceptions.filter((e) => !e.resolved).length;
    return { classes: list.length, attendance: att, progress: prog, exceptions: pendingExc };
  }, [records, exceptions]);

  const refreshMetrics = useCallback((r: ClassRecord) => {
    const sessions = r.sessions || [];
    let totalExp = 0, totalPres = 0;
    sessions.forEach((s) => { totalExp += s.expected || 0; totalPres += s.present ?? s.expected; });
    r.attendanceRate = totalExp ? Math.round((totalPres / totalExp) * 1000) / 10 : 0;
    const done = sessions.filter((s) => s.checked || (s.present >= (s.expected || 0) * 0.8)).length;
    r.progress = sessions.length ? Math.round((done / sessions.length) * 100) : 0;
    r.status = r.attendanceRate < 80 ? '异常' : (r.attendanceRate < 90 ? '关注' : '正常');
  }, []);

  const updateRecords = useCallback((updater: (prev: ClassRecord[]) => ClassRecord[]) => {
    setRecords((prev) => {
      const next = updater(prev.map((r) => ({ ...r, sessions: r.sessions.map((s) => ({ ...s })) })));
      next.forEach(refreshMetrics);
      return next;
    });
  }, [refreshMetrics]);

  const handleBatchCheckIn = () => {
    const today = todayStr();
    let n = 0;
    updateRecords((prev) =>
      prev.map((r) => {
        r.sessions.forEach((s) => {
          if (s.date === today) { s.checked = true; s.present = s.expected; n++; }
        });
        return r;
      }),
    );
    message.success(n ? `已为 ${n} 个今日课次完成批量签到` : '今日无待签到课次');
  };

  const handleCheckIn = (classId: number) => {
    const today = todayStr();
    updateRecords((prev) =>
      prev.map((r) => {
        if (r.classId !== classId) return r;
        const session = r.sessions.find((s) => s.date === today) || r.sessions[0];
        if (session) { session.checked = true; session.present = session.expected; }
        return r;
      }),
    );
    const r = records.find((x) => x.classId === classId);
    message.success(`已为「${r?.className || ''}」完成课次签到`);
  };

  const handleUploadPhoto = () => {
    const target = filteredRecords[0] || records[0];
    if (!target) { message.warning('请先选择项目并确保有班级'); return; }
    updateRecords((prev) =>
      prev.map((r) => {
        if (r.classId !== target.classId) return r;
        r.evidenceCount.photos += 1;
        return r;
      }),
    );
    message.success('课堂照片上传成功（演示）');
  };

  const handleUploadVideo = () => {
    const target = filteredRecords[0] || records[0];
    if (!target) { message.warning('请先选择项目并确保有班级'); return; }
    updateRecords((prev) =>
      prev.map((r) => {
        if (r.classId !== target.classId) return r;
        r.evidenceCount.videos += 1;
        return r;
      }),
    );
    message.success('课堂视频上传成功（演示）');
  };

  const handleAddLog = () => {
    if (!logClassId || !logContent.trim()) { message.warning('请填写完整信息'); return; }
    const r = records.find((x) => x.classId === logClassId);
    const entry: LogEntry = {
      id: logIdCounter, classId: logClassId, className: r?.className || '—',
      date: logDate, author: '班主任', content: logContent.trim(),
    };
    setLogIdCounter((c) => c + 1);
    setLogs((prev) => [entry, ...prev]);
    setLogContent('');
    setLogModalOpen(false);
    message.success('跟班日志已保存');
  };

  const handleAddException = () => {
    if (!excClassId) { message.warning('请选择班级'); return; }
    const r = records.find((x) => x.classId === excClassId);
    const entry: ExcEntry = {
      id: excIdCounter, classId: excClassId, className: r?.className || '—',
      date: excDate, student: excStudent.trim(), type: excType, desc: excDesc.trim(), resolved: false,
    };
    setExcIdCounter((c) => c + 1);
    setExceptions((prev) => [entry, ...prev]);
    if (r) {
      updateRecords((prev) => prev.map((x) => (x.classId === r.classId ? { ...x, status: '关注' } : x)));
    }
    setExcStudent(''); setExcDesc('');
    setExcModalOpen(false);
    message.success('异常已登记');
  };

  const handleResolveExc = (id: number) => {
    setExceptions((prev) => prev.map((e) => (e.id === id ? { ...e, resolved: true } : e)));
    message.success('异常已标记为已处理');
  };

  const filteredLogs = useMemo(() => {
    let list = [...logs].sort((a, b) => b.date.localeCompare(a.date));
    if (filterClass) list = list.filter((l) => String(l.classId) === filterClass);
    return list;
  }, [logs, filterClass]);

  const filteredExc = useMemo(() => {
    let list = [...exceptions].sort((a, b) => b.date.localeCompare(a.date));
    if (filterClass) list = list.filter((e) => String(e.classId) === filterClass);
    return list;
  }, [exceptions, filterClass]);

  // 班级概览列
  const classColumns: ColumnsType<ClassRecord> = [
    { title: '班级', dataIndex: 'className', render: (v) => <strong>{v}</strong> },
    { title: '学员', dataIndex: 'studentCount', render: (v) => `${v} 人` },
    {
      title: '课程进度', key: 'progress',
      render: (_, r) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div className="tpx-progress-bar"><div className="tpx-progress-fill" style={{ width: `${r.progress || 0}%` }} /></div>
          <span style={{ fontSize: 13, fontWeight: 600 }}>{r.progress || 0}%</span>
        </div>
      ),
    },
    { title: '签到率', dataIndex: 'attendanceRate', render: (v) => `${v || 0}%` },
    { title: '留痕', key: 'evidence', render: (_, r) => { const ev = r.evidenceCount || { photos: 0, videos: 0 }; return `${ev.photos}图 ${ev.videos}视频`; } },
    { title: '状态', dataIndex: 'status', render: (v) => <span className={`status-badge ${v === '异常' ? 'st-danger' : v === '关注' ? 'st-warn' : 'st-normal'}`}>{v || '正常'}</span> },
    {
      title: '操作', key: 'actions',
      render: (_, r) => (
        <div className="action-btns" onClick={(e) => e.stopPropagation()}>
          <button className="action-btn" title="详情" onClick={() => setDetailModal(r)}><EyeOutlined /></button>
          <button className="action-btn" title="签到" onClick={() => handleCheckIn(r.classId)}><CheckCircleOutlined /></button>
        </div>
      ),
    },
  ];

  return (
    <div className="tpx-page">
      <header className="tpx-topbar">
        <div className="tpx-topbar-left">
          <a className="tpx-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="tpx-title">培训实施</h1>
            <span className="tpx-sub">{currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 5 / 9` : '按项目跟踪培训过程 · 步骤 5 / 9'}</span>
          </div>
        </div>
        <span className="tpx-process-badge"><ToolOutlined /> … → 培训实施 → 认定准备 → …</span>
      </header>

      <main className="tpx-page-body">
        {/* 项目选择 */}
        <div className="tpx-card">
          <div className="tpx-card-head">
            <div className="tpx-card-head-title"><FolderOpenOutlined /> 培训项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="tpx-form-group">
            <label>选择企业培训项目（培训针对已开班班级）</label>
            <Select placeholder="请选择项目…" value={selectedProjectId || undefined} onChange={handleProjectChange} allowClear style={{ width: '100%' }}
              options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))} />
          </div>
          {currentProject && (
            <div className="tpx-project-banner">
              <div className="tpx-banner-item"><label>企业</label><strong>{currentProject.enterprise || '—'}</strong></div>
              <div className="tpx-banner-item"><label>培训项目</label><strong>{currentProject.name || '—'}</strong></div>
              <div className="tpx-banner-item"><label>在培班级</label><strong>{records.length} 个</strong></div>
              <div className="tpx-banner-item"><label>工种</label><strong>{currentProject.occupation || '—'}</strong></div>
              <div className="tpx-banner-item"><label>负责人</label><strong>{currentProject.owner || '—'}</strong></div>
            </div>
          )}
        </div>

        {!currentProject ? (
          <div className="tpx-empty">
            <InfoCircleOutlined style={{ fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' }} />
            <p>请先选择<strong>企业培训项目</strong>，再管理各班级培训实施与考勤。</p>
            <p style={{ marginTop: 8, fontSize: 13 }}>需先完成 开班；也可从任务档案进入。</p>
          </div>
        ) : records.length === 0 ? (
          <div className="tpx-alert warn">
            <ExclamationCircleOutlined />
            <div>本项目尚无在培班级，请先 开班。</div>
          </div>
        ) : (
          <>
            {/* 统计卡片 */}
            <div className="tpx-stat-row">
              <div className="tpx-stat-card"><div className="tpx-stat-val">{stats.classes}</div><div className="tpx-stat-lbl">在培班级</div></div>
              <div className="tpx-stat-card"><div className="tpx-stat-val success">{stats.attendance}%</div><div className="tpx-stat-lbl">平均签到率</div></div>
              <div className="tpx-stat-card"><div className="tpx-stat-val">{stats.progress}%</div><div className="tpx-stat-lbl">课程进度</div></div>
              <div className="tpx-stat-card"><div className="tpx-stat-val warn">{stats.exceptions}</div><div className="tpx-stat-lbl">待处理异常</div></div>
            </div>

            {/* 培训过程 */}
            <div className="tpx-card">
              <div className="tpx-card-head">
                <div className="tpx-card-head-title"><ToolOutlined /> 培训过程</div>
                <span className="tpx-hint">考勤管理 · 过程留痕 · 跟班日志 · 异常处理</span>
              </div>
              <div className="tpx-toolbar">
                <div className="tpx-form-group" style={{ minWidth: 180, margin: 0 }}>
                  <label>筛选班级</label>
                  <Select placeholder="全部班级" value={filterClass || undefined} onChange={(v) => setFilterClass(v || '')} allowClear style={{ width: '100%' }}
                    options={records.map((r) => ({ value: String(r.classId), label: r.className }))} />
                </div>
              </div>

              <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
                {
                  key: 'overview', label: '班级概览',
                  children: (
                    <Table dataSource={filteredRecords} columns={classColumns} rowKey="classId" pagination={false}
                      locale={{ emptyText: '暂无在培班级' }} />
                  ),
                },
                {
                  key: 'attendance', label: '课次考勤',
                  children: (
                    <div>
                      <div className="tpx-toolbar">
                        <Button type="primary" icon={<CheckCircleOutlined />} onClick={handleBatchCheckIn}>今日课次批量签到</Button>
                      </div>
                      {filteredRecords.length === 0 ? <p style={{ color: '#64748B' }}>暂无课次</p> : (
                        filteredRecords.map((r) => {
                          const sessions = [...r.sessions].sort((a, b) => (a.date + a.timeRange).localeCompare(b.date + b.timeRange));
                          return (
                            <div key={r.classId} className="tpx-att-card">
                              <strong>{r.className}</strong>
                              {sessions.map((s) => (
                                <div key={s.id} className="tpx-att-row">
                                  <span style={{ minWidth: 100 }}>{s.date}</span>
                                  <span style={{ flex: 1 }}>{s.courseName} · {s.timeRange}</span>
                                  <span>应到 {s.expected} / 实到{' '}
                                    <Input size="small" type="number" min={0} max={s.expected} value={s.present}
                                      style={{ width: 56 }} onChange={(e) => {
                                        updateRecords((prev) => prev.map((x) => {
                                          if (x.classId !== r.classId) return x;
                                          const sess = x.sessions.find((ss) => ss.id === s.id);
                                          if (sess) sess.present = parseInt(e.target.value) || 0;
                                          return x;
                                        }));
                                      }} />
                                  </span>
                                  <label className="tpx-check-label">
                                    <input type="checkbox" checked={s.checked}
                                      onChange={(e) => {
                                        updateRecords((prev) => prev.map((x) => {
                                          if (x.classId !== r.classId) return x;
                                          const sess = x.sessions.find((ss) => ss.id === s.id);
                                          if (sess) sess.checked = e.target.checked;
                                          return x;
                                        }));
                                      }} />
                                    {' '}已签到
                                  </label>
                                </div>
                              ))}
                            </div>
                          );
                        })
                      )}
                    </div>
                  ),
                },
                {
                  key: 'evidence', label: '过程留痕',
                  children: (
                    <div>
                      <div className="tpx-toolbar">
                        <Button icon={<FileImageOutlined />} onClick={handleUploadPhoto}>上传课堂照片</Button>
                        <Button icon={<VideoCameraOutlined />} onClick={handleUploadVideo}>上传课堂视频</Button>
                      </div>
                      {(() => {
                        const totalP = filteredRecords.reduce((s, r) => s + (r.evidenceCount?.photos || 0), 0);
                        const totalV = filteredRecords.reduce((s, r) => s + (r.evidenceCount?.videos || 0), 0);
                        if (!totalP && !totalV) {
                          return <p style={{ color: '#64748B' }}>暂无留痕，请上传课堂照片或视频（对应需求：签到表、图片、视频上传）</p>;
                        }
                        return (
                          <div>
                            <p style={{ marginBottom: 12, fontSize: 14 }}>累计：{totalP} 张照片，{totalV} 个视频（演示）</p>
                            <div className="tpx-evidence-grid">
                              {filteredRecords.map((r) => {
                                const n = (r.evidenceCount?.photos || 0) + (r.evidenceCount?.videos || 0);
                                const items = [];
                                for (let i = 0; i < Math.min(n, 3); i++) {
                                  const isVid = i >= (r.evidenceCount?.photos || 0);
                                  items.push(
                                    <div key={i} className="tpx-evidence-thumb">
                                      {isVid ? <VideoCameraOutlined style={{ fontSize: 28, color: '#2A6DFF' }} /> : <FileImageOutlined style={{ fontSize: 28, color: '#2A6DFF' }} />}
                                      <span>{r.className}</span>
                                    </div>,
                                  );
                                }
                                return items;
                              })}
                            </div>
                          </div>
                        );
                      })()}
                    </div>
                  ),
                },
                {
                  key: 'logs', label: '跟班日志',
                  children: (
                    <div>
                      <div className="tpx-toolbar">
                        <Button type="primary" icon={<EditOutlined />}
                          onClick={() => { setLogDate(todayStr()); setLogModalOpen(true); }}>新增跟班日志</Button>
                      </div>
                      {filteredLogs.length === 0 ? <p style={{ color: '#64748B' }}>暂无跟班日志</p> : (
                        filteredLogs.map((l) => (
                          <div key={l.id} className="tpx-log-item">
                            <div className="tpx-log-meta">{l.date} · {l.className} · {l.author}</div>
                            {l.content}
                          </div>
                        ))
                      )}
                    </div>
                  ),
                },
                {
                  key: 'exceptions', label: '异常处理',
                  children: (
                    <div>
                      <div className="tpx-toolbar">
                        <Button type="primary" icon={<ExclamationCircleOutlined />}
                          onClick={() => { setExcDate(todayStr()); setExcModalOpen(true); }}>登记异常</Button>
                      </div>
                      {filteredExc.length === 0 ? <p style={{ color: '#64748B' }}>暂无异常记录</p> : (
                        filteredExc.map((e) => (
                          <div key={e.id} className="tpx-exc-item">
                            <div className="tpx-exc-meta">
                              {e.date} · {e.className} · {e.type} {e.student ? '· ' + e.student : ''}
                              {e.resolved
                                ? <span className="status-badge st-normal">已处理</span>
                                : <span className="status-badge st-warn">待处理</span>}
                            </div>
                            {e.desc || '—'}
                            {!e.resolved && (
                              <Button size="small" style={{ marginTop: 8 }} onClick={() => handleResolveExc(e.id)}>标记已处理</Button>
                            )}
                          </div>
                        ))
                      )}
                    </div>
                  ),
                },
              ]} />
            </div>

            {/* 底部操作 */}
            <div className="tpx-footer-actions">
              <Button icon={<CalendarOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-class-declaration'); }}>返回开班</Button>
              <Button onClick={() => navigateTo('zhixun-training-process')}>保存并返回流程</Button>
              <Button type="primary" icon={<SaveOutlined />} style={{ background: '#10B981', borderColor: '#10B981' }}
                onClick={() => {
                  if (!currentProject) { message.warning('请先选择项目'); return; }
                  message.success(`项目「${currentProject.name}」培训过程已保存，进入认定环节`);
                  setCurrentProjectId(currentProject.id);
                  navigateTo('zhixun-certification');
                }}>
                保存培训进度
              </Button>
            </div>

            {/* 班级详情弹窗 */}
            <Modal open={!!detailModal} onCancel={() => setDetailModal(null)} title={detailModal?.className + ' · 培训详情'} footer={<Button onClick={() => setDetailModal(null)}>关闭</Button>}>
              {detailModal && (
                <div>
                  <p><strong>学员：</strong>{detailModal.studentCount} 人 · <strong>进度：</strong>{detailModal.progress}% · <strong>签到率：</strong>{detailModal.attendanceRate}%</p>
                  <p style={{ marginTop: 12 }}><strong>课次：</strong>{(detailModal.sessions || []).length} 节</p>
                  <p style={{ marginTop: 8 }}><strong>留痕：</strong>{detailModal.evidenceCount?.photos || 0} 照片 / {detailModal.evidenceCount?.videos || 0} 视频</p>
                  <p style={{ marginTop: 8 }}><strong>状态：</strong>{detailModal.status}</p>
                </div>
              )}
            </Modal>

            {/* 日志弹窗 */}
            <Modal open={logModalOpen} onCancel={() => setLogModalOpen(false)} title="新增跟班日志"
              footer={
                <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                  <Button onClick={() => setLogModalOpen(false)}>取消</Button>
                  <Button type="primary" onClick={handleAddLog}>保存</Button>
                </div>
              }>
              <div className="tpx-modal-form">
                <div className="tpx-form-grid">
                  <div className="tpx-form-group">
                    <label>班级</label>
                    <Select value={logClassId} onChange={setLogClassId} style={{ width: '100%' }} placeholder="选择班级"
                      options={records.map((r) => ({ value: r.classId, label: r.className }))} />
                  </div>
                  <div className="tpx-form-group">
                    <label>日期</label>
                    <Input type="date" value={logDate} onChange={(e) => setLogDate(e.target.value)} />
                  </div>
                  <div className="tpx-form-group" style={{ gridColumn: '1/-1' }}>
                    <label>日志内容</label>
                    <Input.TextArea rows={3} value={logContent} onChange={(e) => setLogContent(e.target.value)} placeholder="考勤情况、课堂表现、异常学员沟通等" />
                  </div>
                </div>
              </div>
            </Modal>

            {/* 异常弹窗 */}
            <Modal open={excModalOpen} onCancel={() => setExcModalOpen(false)} title="登记培训异常"
              footer={
                <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                  <Button onClick={() => setExcModalOpen(false)}>取消</Button>
                  <Button type="primary" onClick={handleAddException}>保存</Button>
                </div>
              }>
              <div className="tpx-modal-form">
                <div className="tpx-form-grid">
                  <div className="tpx-form-group">
                    <label>班级</label>
                    <Select value={excClassId} onChange={setExcClassId} style={{ width: '100%' }} placeholder="选择班级"
                      options={records.map((r) => ({ value: r.classId, label: r.className }))} />
                  </div>
                  <div className="tpx-form-group">
                    <label>日期</label>
                    <Input type="date" value={excDate} onChange={(e) => setExcDate(e.target.value)} />
                  </div>
                  <div className="tpx-form-group">
                    <label>学员姓名</label>
                    <Input placeholder="可选" value={excStudent} onChange={(e) => setExcStudent(e.target.value)} />
                  </div>
                  <div className="tpx-form-group">
                    <label>异常类型</label>
                    <Select value={excType} onChange={setExcType} style={{ width: '100%' }}
                      options={['缺勤', '迟到', '课堂违纪', '伤病请假', '其他'].map((v) => ({ value: v, label: v }))} />
                  </div>
                  <div className="tpx-form-group" style={{ gridColumn: '1/-1' }}>
                    <label>说明</label>
                    <Input.TextArea rows={3} value={excDesc} onChange={(e) => setExcDesc(e.target.value)} />
                  </div>
                </div>
              </div>
            </Modal>
          </>
        )}
      </main>
    </div>
  );
}
