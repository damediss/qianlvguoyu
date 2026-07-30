import { useState, useMemo, useCallback, useEffect } from 'react';
import { Typography, Select, Input, Button, Table, Tabs, message } from 'antd';
import {
  ArrowLeftOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  TrophyOutlined,
  BarChartOutlined,
  FileZipOutlined,
  BellOutlined,
  FileAddOutlined,
  NodeIndexOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './GraduationPage.css';

// ============================================================
// 类型
// ============================================================
interface Graduate {
  studentId: number;
  name: string;
  classId: number;
  className: string;
  rendingResult: string;
  graduateCertStatus: string;
  graduateCertNo: string;
  graduateDate: string;
  archived: boolean;
  notified: boolean;
}

interface ClassSummary {
  classId: number;
  className: string;
  total: number;
  issued: number;
  pending: number;
  graduateDate: string;
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

const FLOW_LABELS = ['结课统计', '材料整理', '结课通知', '证书归档'];
const FLOW_ICONS = [BarChartOutlined, FolderOpenOutlined, BellOutlined, FileZipOutlined];

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

// ============================================================
// Mock 数据
// ============================================================
const MOCK_STUDENTS = [
  { studentId: 1, name: '张建国', classId: 1, className: '安全员高级工1班' },
  { studentId: 2, name: '李秀英', classId: 1, className: '安全员高级工1班' },
  { studentId: 3, name: '王志强', classId: 1, className: '安全员高级工1班' },
  { studentId: 4, name: '陈丽华', classId: 1, className: '安全员高级工1班' },
  { studentId: 5, name: '刘明辉', classId: 1, className: '安全员高级工1班' },
  { studentId: 6, name: '赵红梅', classId: 2, className: '安全员中级工1班' },
  { studentId: 7, name: '周建华', classId: 2, className: '安全员中级工1班' },
  { studentId: 8, name: '吴桂芳', classId: 2, className: '安全员中级工1班' },
];

function buildMockGraduates(projectId: number): Graduate[] {
  const task = DEFAULT_TASKS.find((t) => t.id === projectId);
  if (!task) return [];
  const cls1 = `${task.occupation || '电工'}高级工1班`;
  const cls2 = `${task.occupation || '电工'}中级工1班`;
  return MOCK_STUDENTS.map((s, i) => {
    const isIssued = i < 5;
    return {
      studentId: s.studentId,
      name: s.name,
      classId: s.classId,
      className: s.classId === 1 ? cls1 : cls2,
      rendingResult: '通过',
      graduateCertStatus: isIssued ? '已发证' : '待发证',
      graduateCertNo: isIssued ? `JY${projectId}${String(s.studentId).padStart(4, '0')}` : '',
      graduateDate: isIssued ? '2026-08-01' : '',
      archived: isIssued,
      notified: isIssued,
    };
  });
}

// ============================================================
// 主页面
// ============================================================
export function GraduationPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
  const [graduates, setGraduates] = useState<Graduate[]>([]);
  const [statsDone, setStatsDone] = useState(false);
  const [packageReady, setPackageReady] = useState(false);
  const [notified, setNotified] = useState(false);
  const [activeTab, setActiveTab] = useState('class');
  const [filterClass, setFilterClass] = useState('');
  const [filterCert, setFilterCert] = useState('');

  const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;

  useEffect(() => {
    if (selectedProjectId) {
      setGraduates(buildMockGraduates(+selectedProjectId));
      setStatsDone(false); setPackageReady(false); setNotified(false);
    } else {
      setGraduates([]);
    }
  }, [selectedProjectId]);

  const handleProjectChange = (v: string) => {
    setSelectedProjectId(v);
    setCurrentProjectId(v ? +v : null);
    setFilterClass(''); setFilterCert('');
  };

  const updateGraduates = useCallback((updater: (prev: Graduate[]) => Graduate[]) => {
    setGraduates((prev) => updater(prev.map((g) => ({ ...g }))));
  }, []);

  const classSummaries = useMemo(() => {
    const map: Record<number, ClassSummary> = {};
    graduates.forEach((g) => {
      if (!map[g.classId]) {
        map[g.classId] = { classId: g.classId, className: g.className, total: 0, issued: 0, pending: 0, graduateDate: g.graduateDate || '' };
      }
      const c = map[g.classId];
      c.total++;
      if (g.graduateCertStatus === '已发证') c.issued++;
      else c.pending++;
      if (g.graduateDate) c.graduateDate = g.graduateDate;
    });
    return Object.values(map);
  }, [graduates]);

  const stats = useMemo(() => {
    const total = graduates.length;
    const issued = graduates.filter((g) => g.graduateCertStatus === '已发证').length;
    const pending = total - issued;
    const archived = graduates.filter((g) => g.archived).length;
    return { total, issued, pending, archived };
  }, [graduates]);

  const flowStep = useMemo(() => {
    const arcCount = graduates.filter((g) => g.archived).length;
    if (arcCount === graduates.length && graduates.length) return 4;
    if (graduates.every((g) => g.graduateCertStatus === '已发证') && graduates.length) return 3;
    if (notified) return 2;
    if (packageReady || statsDone) return 1;
    return 0;
  }, [graduates, notified, packageReady, statsDone]);

  const classNames = useMemo(() => [...new Set(graduates.map((g) => g.className))], [graduates]);

  const filteredGraduates = useMemo(() => {
    return graduates.filter((g) => {
      if (filterClass && g.className !== filterClass) return false;
      if (filterCert && g.graduateCertStatus !== filterCert) return false;
      return true;
    });
  }, [graduates, filterClass, filterCert]);

  const handleStats = () => {
    setStatsDone(true);
    const issued = graduates.filter((g) => g.graduateCertStatus === '已发证').length;
    message.success(`结课统计完成：应结业 ${graduates.length} 人，已发证 ${issued} 人，待发 ${graduates.length - issued} 人`);
  };

  const handlePackage = () => { setPackageReady(true); message.success('结课材料包已整理（含考勤、过程留痕、考核与获证汇总）'); };
  const handleNotify = () => {
    setNotified(true);
    updateGraduates((prev) => prev.map((g) => ({ ...g, notified: true })));
    message.success(`已向企业与学员发送结课通知（${graduates.length} 人）`);
  };

  const handleBatchCert = () => {
    const pending = graduates.filter((g) => g.graduateCertStatus !== '已发证');
    if (!pending.length) { message.warning('暂无待发证学员'); return; }
    const today = todayStr();
    updateGraduates((prev) =>
      prev.map((g) => {
        if (g.graduateCertStatus === '已发证') return g;
        return { ...g, graduateCertStatus: '已发证', graduateCertNo: `JY${selectedProjectId}${String(g.studentId).padStart(4, '0')}`, graduateDate: today };
      }),
    );
    message.success(`已为 ${pending.length} 名学员发放结业证书`);
  };

  const handleArchive = () => {
    const issued = graduates.filter((g) => g.graduateCertStatus === '已发证');
    if (!issued.length) { message.warning('请先发放结业证'); return; }
    updateGraduates((prev) => prev.map((g) => (g.graduateCertStatus === '已发证' ? { ...g, archived: true } : g)));
    message.success(`已归档 ${issued.length} 名学员培训档案`);
  };

  const handleIssueOne = (studentId: number) => {
    updateGraduates((prev) =>
      prev.map((g) => {
        if (g.studentId !== studentId || g.graduateCertStatus === '已发证') return g;
        return { ...g, graduateCertStatus: '已发证', graduateCertNo: `JY${selectedProjectId}${String(g.studentId).padStart(4, '0')}`, graduateDate: todayStr() };
      }),
    );
  };

  const classColumns: ColumnsType<ClassSummary> = [
    { title: '班级', dataIndex: 'className', render: (v) => <strong>{v}</strong> },
    { title: '应结业', dataIndex: 'total' },
    { title: '已发证', dataIndex: 'issued' },
    { title: '待发', dataIndex: 'pending' },
    { title: '结课日期', dataIndex: 'graduateDate', render: (v) => v || '—' },
    { title: '材料包', key: 'package', render: () => <span className={`status-badge ${packageReady ? 'st-done' : 'st-draft'}`}>{packageReady ? '已整理' : '待整理'}</span> },
  ];

  const studentColumns: ColumnsType<Graduate> = [
    { title: '姓名', dataIndex: 'name', render: (v) => <strong>{v}</strong> },
    { title: '班级', dataIndex: 'className' },
    { title: '认定结果', dataIndex: 'rendingResult' },
    {
      title: '结业证', key: 'cert',
      render: (_, r) => (
        <div>
          <span className={`status-badge ${r.graduateCertStatus === '已发证' ? 'st-done' : 'st-pending'}`}>{r.graduateCertStatus}</span>
          {r.graduateCertNo && <br />}
          {r.graduateCertNo && <small>{r.graduateCertNo}</small>}
        </div>
      ),
    },
    { title: '结业日期', dataIndex: 'graduateDate', render: (v) => v || '—' },
    { title: '档案', dataIndex: 'archived', render: (v) => <span className={`status-badge ${v ? 'st-done' : 'st-draft'}`}>{v ? '已归档' : '未归档'}</span> },
    {
      title: '操作', key: 'actions',
      render: (_, r) => (
        <Button size="small" disabled={r.graduateCertStatus === '已发证'} onClick={() => handleIssueOne(r.studentId)}>
          {r.graduateCertStatus === '已发证' ? '已发证' : '发证'}
        </Button>
      ),
    },
  ];

  return (
    <div className="jy-page">
      <header className="jy-topbar">
        <div className="jy-topbar-left">
          <a className="jy-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="jy-title">培训结业</h1>
            <span className="jy-sub">{currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 8 / 9` : '培训结业与档案归档 · 步骤 8 / 9'}</span>
          </div>
        </div>
        <span className="jy-process-badge"><TrophyOutlined /> … → 培训结业 → 项目回款 → …</span>
      </header>

      <main className="jy-page-body">
        {/* 项目选择 */}
        <div className="jy-card">
          <div className="jy-card-head">
            <div className="jy-card-head-title"><FolderOpenOutlined /> 培训项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="jy-form-group">
            <label>选择企业培训项目</label>
            <Select placeholder="请选择项目…" value={selectedProjectId || undefined} onChange={handleProjectChange} allowClear style={{ width: '100%' }}
              options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))} />
          </div>
          {currentProject && (
            <div className="jy-project-banner">
              <div className="jy-banner-item"><label>企业</label><strong>{currentProject.enterprise || '—'}</strong></div>
              <div className="jy-banner-item"><label>培训项目</label><strong>{currentProject.name || '—'}</strong></div>
              <div className="jy-banner-item"><label>应结业</label><strong>{graduates.length} 人</strong></div>
              <div className="jy-banner-item"><label>已发证</label><strong>{graduates.filter((g) => g.graduateCertStatus === '已发证').length} 人</strong></div>
              <div className="jy-banner-item"><label>结课材料</label><strong>{packageReady ? '已整理' : '未整理'}</strong></div>
            </div>
          )}
        </div>

        {!currentProject ? (
          <div className="jy-empty">
            <InfoCircleOutlined style={{ fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' }} />
            <p>请先选择<strong>企业培训项目</strong>，再办理结业统计、证书发放与档案归档。</p>
            <p style={{ marginTop: 8, fontSize: 13 }}>建议先完成培训与认定；也可从任务档案进入。</p>
          </div>
        ) : graduates.length === 0 ? (
          <div className="jy-alert warn">
            <ExclamationCircleOutlined />
            <div>本项目尚无学员数据，请先完成 分班。</div>
          </div>
        ) : (
          <>
            {/* 统计卡片 */}
            <div className="jy-stat-row">
              <div className="jy-stat-card"><div className="jy-stat-val">{stats.total}</div><div className="jy-stat-lbl">应结业人数</div></div>
              <div className="jy-stat-card"><div className="jy-stat-val success">{stats.issued}</div><div className="jy-stat-lbl">证书已发</div></div>
              <div className="jy-stat-card"><div className="jy-stat-val warn">{stats.pending}</div><div className="jy-stat-lbl">待发证</div></div>
              <div className="jy-stat-card"><div className="jy-stat-val">{stats.archived}</div><div className="jy-stat-lbl">已归档</div></div>
            </div>

            {/* 结业流程 + 表 */}
            <div className="jy-card">
              <div className="jy-card-head">
                <div className="jy-card-head-title"><NodeIndexOutlined /> 结业流程</div>
                <span className="jy-hint">结课统计 → 材料整理 → 结课通知 → 证书/档案归档</span>
              </div>
              <div className="jy-flow-steps">
                {FLOW_LABELS.map((label, i) => {
                  const Icon = FLOW_ICONS[i];
                  let cls = 'jy-flow-step';
                  if (i < flowStep) cls += ' done';
                  else if (i === flowStep) cls += ' active';
                  return (
                    <div key={i} className={cls}>
                      <Icon style={{ display: 'block', fontSize: 18, marginBottom: 6 }} />
                      {label}
                    </div>
                  );
                })}
              </div>
              <div className="jy-toolbar">
                <Button type="primary" icon={<BarChartOutlined />} onClick={handleStats}>结课统计</Button>
                <Button icon={<FileZipOutlined />} onClick={handlePackage}>整理结课材料包</Button>
                <Button icon={<BellOutlined />} onClick={handleNotify}>发送结课通知</Button>
                <Button style={{ background: '#10B981', color: '#fff', borderColor: '#10B981' }} icon={<TrophyOutlined />} onClick={handleBatchCert}>批量发放结业证</Button>
                <Button icon={<FolderOpenOutlined />} onClick={handleArchive}>学员档案归档</Button>
              </div>

              <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
                {
                  key: 'class', label: '班级汇总',
                  children: (
                    <Table dataSource={classSummaries} columns={classColumns} rowKey="classId" pagination={false}
                      locale={{ emptyText: '暂无班级' }} />
                  ),
                },
                {
                  key: 'student', label: '学员明细',
                  children: (
                    <div>
                      <div className="jy-filter-bar">
                        <div className="jy-form-group" style={{ minWidth: 160, margin: 0 }}>
                          <label>班级</label>
                          <Select placeholder="全部" value={filterClass || undefined} onChange={(v) => setFilterClass(v || '')} allowClear style={{ width: '100%' }}
                            options={classNames.map((n) => ({ value: n, label: n }))} />
                        </div>
                        <div className="jy-form-group" style={{ minWidth: 160, margin: 0 }}>
                          <label>证书状态</label>
                          <Select placeholder="全部" value={filterCert || undefined} onChange={(v) => setFilterCert(v || '')} allowClear style={{ width: '100%' }}
                            options={['待发证', '已发证'].map((v) => ({ value: v, label: v }))} />
                        </div>
                      </div>
                      <Table dataSource={filteredGraduates} columns={studentColumns} rowKey="studentId" pagination={false}
                        locale={{ emptyText: '暂无学员' }} />
                    </div>
                  ),
                },
              ]} />
            </div>

            {/* 底部操作 */}
            <div className="jy-footer-actions">
              <Button icon={<FileAddOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-declaration-service'); }}>返回申报</Button>
              <Button onClick={() => navigateTo('zhixun-training-process')}>保存并返回流程</Button>
              <Button type="primary" style={{ background: '#10B981', borderColor: '#10B981' }} icon={<ArrowLeftOutlined style={{ transform: 'rotate(180deg)' }} />}
                onClick={() => {
                  if (!currentProject) { message.warning('请先选择项目'); return; }
                  setCurrentProjectId(+selectedProjectId);
                  navigateTo('zhixun-refund');
                }}>
                完成培训结业，进入项目回款
              </Button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
