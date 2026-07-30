import { useState, useMemo, useCallback, useEffect } from 'react';
import { Typography, Select, Input, Button, Table, Tabs, InputNumber, message } from 'antd';
import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  InfoCircleOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  FileTextOutlined,
  FileProtectOutlined,
  CheckCircleOutlined,
  LockOutlined,
  PlusOutlined,
  DeleteOutlined,
  BellOutlined,
  SafetyCertificateOutlined,
  NodeIndexOutlined,
  CalendarOutlined,
  ApartmentOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './FinalPlanPage.css';

// ============================================================
// 类型
// ============================================================
interface OccFinal {
  id: number;
  occ: string;
  level: string;
  planCount: number;
  identified: number;
  batches: number;
  form: string;
  quarter: string;
  finalCount: number;
  firstBatch: boolean;
  institute: string;
  confirmed: boolean;
}

interface ResourceItem {
  id: number;
  type: string;
  name: string;
  content: string;
  date: string;
  owner: string;
  status: string;
}

interface FinalPlanData {
  refVersion: string;
  refMode: string;
  refYears: string;
  enterpriseConfirm: string;
  enterpriseNote: string;
  finalizeDate: string;
  finalStatus: string;
  firstOpenDate: string;
  firstOpenOcc: string;
  planClassCount: number;
  classSize: number;
  finalVenue: string;
  occFinal: OccFinal[];
  resources: ResourceItem[];
  locked: boolean;
  lockedVer: string;
  lockedAt: string;
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

const INSTITUTES = ['广西机电技师学院', '柳州职业技能培训中心', '华南技能培训学校'];

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

function defaultFinal(): FinalPlanData {
  return {
    refVersion: 'V1.0',
    refMode: '—',
    refYears: '5 年（2026 起）',
    enterpriseConfirm: '待确认',
    enterpriseNote: '',
    finalizeDate: todayStr(),
    finalStatus: '编制中',
    firstOpenDate: '',
    firstOpenOcc: '',
    planClassCount: 1,
    classSize: 30,
    finalVenue: '',
    occFinal: [],
    resources: [],
    locked: false,
    lockedVer: '',
    lockedAt: '',
  };
}

function buildInitialData(project: (typeof DEFAULT_TASKS)[number]): FinalPlanData {
  const occs: OccFinal[] = [
    { id: 1, occ: project.occupation || '电工', level: '高级工', planCount: 40, identified: 32, batches: 2, form: '理实一体', quarter: 'Q2', finalCount: 40, firstBatch: true, institute: INSTITUTES[0], confirmed: false },
    { id: 2, occ: '焊工', level: '技师', planCount: 24, identified: 18, batches: 1, form: '集中培训', quarter: 'Q3', finalCount: 24, firstBatch: true, institute: INSTITUTES[0], confirmed: false },
  ];
  const resources: ResourceItem[] = [
    { id: 1, type: '培训机构', name: INSTITUTES[0], content: '签订培训协议及授课承诺书', date: '', owner: '培训部', status: '待落实' },
    { id: 2, type: '师资', name: '陈教授', content: '首期主讲教师安排确认', date: '', owner: '培训机构', status: '待落实' },
    { id: 3, type: '场地', name: '总部培训中心', content: '场地预订与设备检查', date: '', owner: '培训部', status: '待落实' },
    { id: 4, type: '课件', name: '课程部课包', content: '课件审批通过', date: '', owner: '课程部', status: '审批中' },
  ];
  return {
    refVersion: 'V1.0',
    refMode: '新型学徒制',
    refYears: `5 年（2026 起）`,
    enterpriseConfirm: '待确认',
    enterpriseNote: '',
    finalizeDate: todayStr(),
    finalStatus: '编制中',
    firstOpenDate: '',
    firstOpenOcc: `${project.occupation || '电工'} 高级工`,
    planClassCount: 1,
    classSize: 30,
    finalVenue: '总部培训中心',
    occFinal: occs,
    resources,
    locked: false,
    lockedVer: '',
    lockedAt: '',
  };
}

// ============================================================
// 主页面
// ============================================================
export function FinalPlanPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [selectedProjectId, setSelectedProjectId] = useState<string>(() => String(currentProjectId || ''));
  const [plan, setPlan] = useState<FinalPlanData>(defaultFinal);
  const [resIdCounter, setResIdCounter] = useState(5);
  const [activeTab, setActiveTab] = useState('finalize');

  const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;

  useEffect(() => {
    if (!currentProject) { setPlan(defaultFinal()); return; }
    setPlan(buildInitialData(currentProject));
    setResIdCounter(5);
  }, [selectedProjectId]);

  const updatePlan = useCallback((updater: (p: FinalPlanData) => void) => {
    setPlan((prev) => {
      const next = { ...prev, occFinal: prev.occFinal.map((o) => ({ ...o })), resources: prev.resources.map((r) => ({ ...r })) };
      updater(next);
      return next;
    });
  }, []);

  const stats = useMemo(() => {
    const occ = plan.occFinal.length;
    const confirmed = plan.occFinal.filter((o) => o.confirmed).length;
    const resOk = plan.resources.filter((r) => r.status === '已落实').length;
    const resTotal = plan.resources.length;
    const pct = resTotal ? Math.round(resOk / resTotal * 100) : 0;
    return { occ, confirmed, pct, locked: plan.locked };
  }, [plan]);

  const readiness = useMemo(() => {
    return [
      { label: '企业已确认方案', ok: plan.enterpriseConfirm === '已确认' },
      { label: '工种等级终稿已全部确认', ok: plan.occFinal.length > 0 && plan.occFinal.every((o) => o.confirmed) },
      { label: '首期开班日期已确定', ok: !!plan.firstOpenDate },
      { label: '培训场地已落实', ok: plan.resources.some((r) => r.type === '场地' && r.status === '已落实') },
      { label: '师资已安排确认', ok: plan.resources.some((r) => r.type === '师资' && r.status === '已落实') },
      { label: '最终方案已锁定', ok: plan.locked },
    ];
  }, [plan]);

  const handleProjectChange = (v: string) => {
    setSelectedProjectId(v);
    setCurrentProjectId(v ? +v : null);
  };

  const handleConfirmOcc = (idx: number) => {
    updatePlan((p) => { p.occFinal[idx].confirmed = true; });
  };

  const handleAddResource = () => {
    if (plan.locked) return;
    const newId = resIdCounter;
    setResIdCounter(newId + 1);
    updatePlan((p) => { p.resources.push({ id: newId, type: '其他', name: '', content: '', date: '', owner: '培训部', status: '待落实' }); });
  };

  const handleDelResource = (id: number) => {
    updatePlan((p) => { p.resources = p.resources.filter((r) => r.id !== id); });
  };

  const handleLockFinal = () => {
    const notOk = readiness.filter((r) => !r.ok).length;
    if (notOk > 2 && !confirm(`仍有 ${notOk} 项未就绪，是否仍锁定最终方案？`)) return;
    updatePlan((p) => {
      p.locked = true;
      p.lockedVer = p.refVersion + '-FINAL';
      p.lockedAt = new Date().toLocaleString('zh-CN');
      p.finalStatus = '已锁定';
    });
    message.success('最终方案已锁定，可进入开班申报环节');
  };

  // ---- 工种表格列 ----
  const occColumns: ColumnsType<OccFinal> = [
    { title: '工种', dataIndex: 'occ', render: (v) => v || '—' },
    { title: '等级', dataIndex: 'level', render: (v) => v || '—' },
    { title: '定稿人数', dataIndex: 'finalCount', render: (v, _, i) => <InputNumber size="small" min={0} value={v} style={{ width: 80 }} disabled={plan.locked} onChange={(val) => updatePlan((p) => { p.occFinal[i].finalCount = val || 0; })} /> },
    { title: '首期开班', dataIndex: 'firstBatch', render: (v, _, i) => <Select size="small" value={String(v)} style={{ width: 70 }} disabled={plan.locked} onChange={(val) => updatePlan((p) => { p.occFinal[i].firstBatch = val === 'true'; })} options={[{ value: 'true', label: '是' }, { value: 'false', label: '否' }]} /> },
    { title: '培训机构', dataIndex: 'institute', render: (v, _, i) => <Select size="small" value={v} style={{ width: 180 }} disabled={plan.locked} onChange={(val) => updatePlan((p) => { p.occFinal[i].institute = val; })} options={INSTITUTES.map((ins) => ({ value: ins, label: ins }))} /> },
    { title: '确认', dataIndex: 'confirmed', render: (v) => <span className={`status-badge ${v ? 'st-ok' : 'st-warn'}`}>{v ? '已确认' : '待确认'}</span> },
    { title: '操作', key: 'actions', render: (_, r, i) => plan.locked ? '—' : <Button type="link" size="small" disabled={r.confirmed} onClick={() => handleConfirmOcc(i)}>确认</Button> },
  ];

  // ---- 资源表格列 ----
  const resColumns: ColumnsType<ResourceItem> = [
    { title: '资源类型', dataIndex: 'type', render: (v, _, i) => <Select size="small" value={v} style={{ width: 100 }} disabled={plan.locked} onChange={(val) => updatePlan((p) => { p.resources[i].type = val; })} options={['培训机构', '师资', '场地', '课件', '设备', '其他'].map((t) => ({ value: t, label: t }))} /> },
    { title: '名称/机构', dataIndex: 'name', render: (v, _, i) => <Input size="small" value={v} disabled={plan.locked} onChange={(e) => updatePlan((p) => { p.resources[i].name = e.target.value; })} /> },
    { title: '落实内容', dataIndex: 'content', render: (v, _, i) => <Input size="small" value={v} disabled={plan.locked} onChange={(e) => updatePlan((p) => { p.resources[i].content = e.target.value; })} /> },
    { title: '负责人', dataIndex: 'owner', render: (v, _, i) => <Input size="small" value={v} disabled={plan.locked} onChange={(e) => updatePlan((p) => { p.resources[i].owner = e.target.value; })} /> },
    { title: '状态', dataIndex: 'status', render: (v, _, i) => <Select size="small" value={v} style={{ width: 90 }} disabled={plan.locked} onChange={(val) => updatePlan((p) => { p.resources[i].status = val; })} options={['待落实', '落实中', '已落实', '审批中'].map((s) => ({ value: s, label: s }))} /> },
    { title: '操作', key: 'actions', render: (_, r) => plan.locked ? '—' : <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleDelResource(r.id)}>删除</Button> },
  ];

  return (
    <div className="fp-page-container">
      <header className="fp-topbar">
        <div className="fp-topbar-left">
          <a className="fp-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="fp-title">最终方案</h1>
            <span className="fp-sub">{currentProject ? `${currentProject.enterprise} · 方案定稿 · 步骤 3 / 9` : '方案定稿 · 分班排课定稿 · 步骤 3 / 9'}</span>
          </div>
        </div>
        <span className="fp-process-badge"><FileProtectOutlined /> 初步方案 → 最终方案 → 开班申报</span>
      </header>

      <main className="fp-page">
        {/* 项目选择卡片 */}
        <div className="fp-card">
          <div className="fp-card-head">
            <div className="fp-card-head-title"><FolderOpenOutlined /> 培训项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="fp-form-group">
            <label>选择企业培训项目（承接初步方案，完成定稿与资源确认）</label>
            <Select placeholder="请选择项目…" value={selectedProjectId || undefined} onChange={handleProjectChange} allowClear style={{ width: '100%' }}
              options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))} />
          </div>
          {currentProject && (
            <div className="fp-project-banner">
              <div className="fp-banner-item"><label>企业</label><strong>{currentProject.enterprise || '—'}</strong></div>
              <div className="fp-banner-item"><label>项目</label><strong>{currentProject.name || '—'}</strong></div>
              <div className="fp-banner-item"><label>初步方案</label><strong>{plan.refVersion}</strong></div>
              <div className="fp-banner-item"><label>企业确认</label><strong>{plan.enterpriseConfirm}</strong></div>
              <div className="fp-banner-item"><label>最终状态</label><strong>{plan.finalStatus}</strong></div>
              <div className="fp-banner-item"><label>负责人</label><strong>{currentProject.owner || '—'}</strong></div>
            </div>
          )}
        </div>

        {!currentProject ? (
          <div className="fp-empty">
            <FileProtectOutlined style={{ fontSize: 48, color: '#E2E8F0', marginBottom: 16 }} />
            <p>请先选择<strong>企业培训项目</strong>，完成最终方案定稿。</p>
            <p style={{ marginTop: 8, fontSize: 13 }}>需先完成 初步方案；也可从任务档案进入。</p>
          </div>
        ) : (
          <>
            <div className="fp-flow-hint">
              <InfoCircleOutlined style={{ color: '#059669' }} />
              {' '}依据需求规格：<strong>方案定稿确认 → 分班定稿 → 排课定稿与发布最终课表 → 版本锁定</strong>
            </div>

            {/* 统计卡片 */}
            <div className="fp-stat-row">
              <div className="fp-stat-card final"><div className="fp-stat-val">{stats.occ}</div><div className="fp-stat-lbl">定稿工种数</div></div>
              <div className="fp-stat-card"><div className="fp-stat-val">{stats.confirmed}/{stats.occ}</div><div className="fp-stat-lbl">工种已确认</div></div>
              <div className="fp-stat-card warn"><div className="fp-stat-val">{stats.pct}%</div><div className="fp-stat-lbl">资源落实率</div></div>
              <div className={`fp-stat-card ${plan.locked ? 'final' : 'danger'}`}>
                <div className="fp-stat-val">{plan.locked ? '已锁' : '未锁'}</div>
                <div className="fp-stat-lbl">方案锁定</div>
              </div>
            </div>

            {/* 标签页 */}
            <div className="fp-card">
              <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
                {
                  key: 'finalize', label: '方案定稿',
                  children: (
                    <div>
                      {plan.locked && (
                        <div className="fp-locked-banner">
                          <LockOutlined style={{ color: '#059669' }} />
                          <span>最终方案已锁定 · 版本 <strong>{plan.lockedVer}</strong> · 锁定时间 {plan.lockedAt}</span>
                        </div>
                      )}

                      <div className="fp-form-grid">
                        <div className="fp-form-group">
                          <label>引用初步方案版本</label>
                          <Input value={plan.refVersion} disabled />
                        </div>
                        <div className="fp-form-group">
                          <label>培养模式</label>
                          <Input value={plan.refMode} disabled />
                        </div>
                        <div className="fp-form-group">
                          <label>规划周期</label>
                          <Input value={plan.refYears} disabled />
                        </div>
                        <div className="fp-form-group">
                          <label>企业确认状态</label>
                          <Select value={plan.enterpriseConfirm} style={{ width: '100%' }} disabled={plan.locked}
                            onChange={(v) => updatePlan((p) => { p.enterpriseConfirm = v; })}
                            options={['待确认', '已确认', '有异议'].map((s) => ({ value: s, label: s }))} />
                        </div>
                        <div className="fp-form-group full">
                          <label>企业确认意见</label>
                          <Input.TextArea value={plan.enterpriseNote} rows={3} disabled={plan.locked}
                            placeholder="企业法务/培训负责人反馈意见…"
                            onChange={(e) => updatePlan((p) => { p.enterpriseNote = e.target.value; })} />
                        </div>
                        <div className="fp-form-group">
                          <label>定稿日期</label>
                          <Input type="date" value={plan.finalizeDate} disabled={plan.locked}
                            onChange={(e) => updatePlan((p) => { p.finalizeDate = e.target.value; })} />
                        </div>
                        <div className="fp-form-group">
                          <label>最终方案状态</label>
                          <Select value={plan.finalStatus} style={{ width: '100%' }} disabled={plan.locked}
                            onChange={(v) => updatePlan((p) => { p.finalStatus = v; })}
                            options={['编制中', '待锁定', '已锁定'].map((s) => ({ value: s, label: s }))} />
                        </div>
                        <div className="fp-form-group">
                          <label>计划首期开班日期</label>
                          <Input type="date" value={plan.firstOpenDate} disabled={plan.locked}
                            onChange={(e) => updatePlan((p) => { p.firstOpenDate = e.target.value; })} />
                        </div>
                        <div className="fp-form-group">
                          <label>首期开班工种</label>
                          <Input value={plan.firstOpenOcc} disabled={plan.locked} placeholder="如：电工 高级工"
                            onChange={(e) => updatePlan((p) => { p.firstOpenOcc = e.target.value; })} />
                        </div>
                        <div className="fp-form-group">
                          <label>计划班级数</label>
                          <InputNumber min={1} value={plan.planClassCount} style={{ width: '100%' }} disabled={plan.locked}
                            onChange={(v) => updatePlan((p) => { p.planClassCount = v || 1; })} />
                        </div>
                        <div className="fp-form-group">
                          <label>每期规模（人/班）</label>
                          <InputNumber min={10} value={plan.classSize} style={{ width: '100%' }} disabled={plan.locked}
                            onChange={(v) => updatePlan((p) => { p.classSize = v || 30; })} />
                        </div>
                        <div className="fp-form-group full">
                          <label>上课地点（终稿）</label>
                          <Input value={plan.finalVenue} disabled={plan.locked} placeholder="企业确认后的培训场地"
                            onChange={(e) => updatePlan((p) => { p.finalVenue = e.target.value; })} />
                        </div>
                      </div>

                      {/* 工种等级终稿确认 */}
                      <div className="fp-card">
                        <div className="fp-card-head">
                          <div className="fp-card-head-title"><CheckCircleOutlined /> 工种等级终稿确认</div>
                        </div>
                        <Table dataSource={plan.occFinal} columns={occColumns} rowKey="id" pagination={false}
                          locale={{ emptyText: '请先在初步方案中维护工种规划' }} />
                      </div>

                      {/* 资源落实表 */}
                      <div className="fp-card">
                        <div className="fp-card-head">
                          <div className="fp-card-head-title"><SafetyCertificateOutlined /> 培训资源落实表</div>
                          <Button size="small" icon={<PlusOutlined />} disabled={plan.locked} onClick={handleAddResource}>新增资源项</Button>
                        </div>
                        <Table dataSource={plan.resources} columns={resColumns} rowKey="id" pagination={false}
                          locale={{ emptyText: '暂无资源项' }} />
                      </div>

                      {/* 开班就绪检查清单 */}
                      <div className="fp-card">
                        <div className="fp-card-head">
                          <div className="fp-card-head-title"><CheckCircleOutlined /> 开班就绪检查清单</div>
                        </div>
                        <ul className="fp-checklist">
                          {readiness.map((it, i) => (
                            <li key={i}>
                              <span>{it.label}</span>
                              <span className={`status-badge ${it.ok ? 'st-ok' : 'st-no'}`}>{it.ok ? '已完成' : '未完成'}</span>
                            </li>
                          ))}
                        </ul>
                        <div className="fp-toolbar">
                          <Button className="btn-final" icon={<LockOutlined />} onClick={handleLockFinal} style={{ display: plan.locked ? 'none' : undefined }}>锁定最终方案</Button>
                          <Button type="primary" icon={<FileTextOutlined />} onClick={() => message.success('已生成培养方案文档')}>生成培养方案文档</Button>
                          <Button icon={<BellOutlined />} onClick={() => message.success('已将开班安排反馈企业')}>反馈企业开班安排</Button>
                        </div>
                      </div>
                    </div>
                  ),
                },
                {
                  key: 'scheduling', label: '分班排课定稿',
                  children: (
                    <div>
                      <div className="fp-card-head">
                        <div className="fp-card-head-title"><NodeIndexOutlined /> 分班排课（定稿）</div>
                        <span className="fp-hint">确认分班定稿并发布最终课表</span>
                      </div>
                      <p style={{ fontSize: 14, color: '#64748B', lineHeight: 1.8, marginBottom: 20 }}>
                        完成分班定稿 → 排课与冲突检测 → 发布最终课表。
                      </p>
                      <div className="fp-sub-stat-row">
                        <div className="fp-sub-stat"><div className="fp-sub-stat-v">0</div><div className="fp-sub-stat-l">待分班</div></div>
                        <div className="fp-sub-stat"><div className="fp-sub-stat-v">0</div><div className="fp-sub-stat-l">班级</div></div>
                        <div className="fp-sub-stat"><div className="fp-sub-stat-v">0</div><div className="fp-sub-stat-l">课程</div></div>
                        <div className="fp-sub-stat"><div className="fp-sub-stat-v">—</div><div className="fp-sub-stat-l">课表</div></div>
                      </div>
                      <div className="fp-toolbar">
                        <Button className="btn-final" icon={<ApartmentOutlined />}>协同工作台（定稿）</Button>
                        <Button type="primary" icon={<NodeIndexOutlined />}>分班管理</Button>
                        <Button type="primary" icon={<CalendarOutlined />}>排课管理</Button>
                      </div>
                    </div>
                  ),
                },
              ]} />
            </div>

            {/* 底部操作 */}
            <div className="fp-footer-actions">
              <Button icon={<FileTextOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-preliminary-plan'); }}>返回初步方案</Button>
              <Button onClick={() => navigateTo('zhixun-training-process')}>保存并返回流程</Button>
              <Button className="btn-final" icon={<ArrowRightOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-class-declaration'); }}>完成最终方案，进入开班申报</Button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
