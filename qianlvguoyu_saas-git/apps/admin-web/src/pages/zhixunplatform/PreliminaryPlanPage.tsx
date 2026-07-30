import { useState, useMemo, useCallback, useEffect } from 'react';
import { Typography, Select, Input, Button, Table, Tabs, InputNumber, message } from 'antd';
import {
  ArrowLeftOutlined,
  FileTextOutlined,
  InfoCircleOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  SaveOutlined,
  PlusOutlined,
  DeleteOutlined,
  SyncOutlined,
  CalendarOutlined,
  UserSwitchOutlined,
  ArrowRightOutlined,
  ThunderboltOutlined,
  NodeIndexOutlined,
  ApartmentOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './PreliminaryPlanPage.css';

// ============================================================
// 类型
// ============================================================
interface OccItem {
  id: number;
  occ: string;
  level: string;
  planCount: number;
  identified: number;
  batches: number;
  form: string;
  quarter: string;
}

interface AnnualItem {
  year: number;
  classes: number;
  trainees: number;
  focus: string;
  budget: string;
  note: string;
}

interface PlanData {
  planYears: number;
  trainMode: string;
  startYear: number;
  planStatus: string;
  planGoal: string;
  planVenue: string;
  occupations: OccItem[];
  annuals: AnnualItem[];
}

// ============================================================
// 模拟数据
// ============================================================
const DEFAULT_TASKS = [
  { id: 1, name: '2026年度安全培训计划编制', owner: '张明', enterprise: '广西建工集团', occupation: '安全员', planCount: 120 },
  { id: 2, name: '电工高级班认定材料整理', owner: '李芳', enterprise: '南宁供电局', occupation: '电工', planCount: 42 },
  { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', enterprise: '柳州钢铁有限公司', occupation: '焊工', planCount: 36 },
  { id: 4, name: '焊工中级班排课协调', owner: '赵丽', enterprise: '桂林旅游股份公司', occupation: '焊工', planCount: 28 },
  { id: 5, name: '培训档案年度归档', owner: '陈伟', enterprise: '梧州港务集团', occupation: '起重工', planCount: 15 },
];

const OCCUPATIONS = ['电工', '焊工', '起重工', '安全员', '架子工'];
const LEVELS = ['初级', '中级', '高级', '高级工', '技师', '高级技师'];
const TRAIN_FORMS = ['集中培训', '理实一体', '线上+线下', '企业内训'];

function defaultPlan(): PlanData {
  const y = new Date().getFullYear();
  return {
    planYears: 5, trainMode: '新型学徒制', startYear: y, planStatus: '草案',
    planGoal: '', planVenue: '总部培训中心', occupations: [], annuals: [],
  };
}

// ============================================================
// 主页面
// ============================================================
export function PreliminaryPlanPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [selectedProjectId, setSelectedProjectId] = useState<string>(() => String(currentProjectId || ''));
  const [plan, setPlan] = useState<PlanData>(defaultPlan);
  const [activeTab, setActiveTab] = useState('basic');
  const [occIdCounter, setOccIdCounter] = useState(1);

  const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;

  // 初始化 / 切换项目
  useEffect(() => {
    if (!currentProject) { setPlan(defaultPlan()); return; }
    const newPlan = defaultPlan();
    const occ1: OccItem = { id: 1, occ: currentProject.occupation || '电工', level: '高级工', planCount: 40, identified: 32, batches: 2, form: '理实一体', quarter: 'Q2' };
    const occ2: OccItem = { id: 2, occ: '焊工', level: '技师', planCount: 24, identified: 18, batches: 1, form: '集中培训', quarter: 'Q3' };
    newPlan.occupations = [occ1, occ2];
    newPlan.planGoal = `基于「${currentProject.enterprise || '企业'}」人才识别结果，制定 ${newPlan.startYear}–${newPlan.startYear + 4} 年技能人才梯队培养规划，覆盖${currentProject.occupation || '主要工种'}等岗位技能提升与认定衔接。`;
    setOccIdCounter(3);
    newPlan.annuals = buildAnnuals(newPlan);
    setPlan(newPlan);
  }, [selectedProjectId]);

  const readForm = useCallback(() => {
    // 表单值由 Ant Design Form 管理，这里只是占位
  }, []);

  const buildAnnuals = useCallback((p: PlanData): AnnualItem[] => {
    const years = p.planYears || 5;
    const start = p.startYear || new Date().getFullYear();
    const total = p.occupations.reduce((s, o) => s + (o.planCount || 0), 0);
    const perYear = Math.ceil(total / years) || 0;
    const annuals: AnnualItem[] = [];
    for (let i = 0; i < years; i++) {
      const y = start + i;
      const occs = p.occupations.map((o) => o.occ).filter(Boolean).slice(0, 2).join('、') || '—';
      annuals.push({ year: y, classes: Math.max(1, Math.ceil((p.occupations.length || 1) / years)), trainees: perYear, focus: occs, budget: (perYear * 0.35).toFixed(1), note: i === 0 ? '启动年' : '' });
    }
    return annuals;
  }, []);

  const updatePlan = useCallback((updater: (p: PlanData) => void) => {
    setPlan((prev) => { const next = { ...prev, occupations: [...prev.occupations], annuals: [...prev.annuals] }; updater(next); return next; });
  }, []);

  // 统计
  const stats = useMemo(() => ({
    occCount: plan.occupations.length,
    total: plan.occupations.reduce((s, o) => s + (o.planCount || 0), 0),
    identified: plan.occupations.reduce((s, o) => s + (o.identified || 0), 0),
    years: plan.planYears || 5,
  }), [plan]);

  const handleGenDraft = () => {
    if (!currentProject) return;
    updatePlan((p) => {
      const newId = occIdCounter;
      p.occupations = [
        { id: newId, occ: currentProject.occupation || '电工', level: '高级工', planCount: Math.max(30, currentProject.planCount || 40), identified: 32, batches: 2, form: '理实一体', quarter: 'Q2' },
        { id: newId + 1, occ: '安全员', level: '中级', planCount: 20, identified: 15, batches: 1, form: '集中培训', quarter: 'Q1' },
      ];
      setOccIdCounter(newId + 2);
      p.planGoal = `【智能草案】${currentProject.enterprise || '企业'} ${p.startYear} 年起 ${p.planYears} 年规划：以${currentProject.occupation || '电工'}为主体，分 ${p.planYears} 年梯次培养。`;
      p.annuals = buildAnnuals(p);
    });
    message.success('已根据人才识别数据生成规划草案');
  };

  const handleSaveBasic = () => {
    updatePlan((p) => { p.annuals = buildAnnuals(p); });
    message.success('方案概要已保存');
  };

  const handleAddOcc = () => {
    const newId = occIdCounter;
    setOccIdCounter(newId + 1);
    updatePlan((p) => { p.occupations.push({ id: newId, occ: '', level: '中级', planCount: 0, identified: 0, batches: 1, form: '集中培训', quarter: 'Q1' }); });
  };

  const handleDelOcc = (id: number) => {
    updatePlan((p) => { p.occupations = p.occupations.filter((o) => o.id !== id); });
  };

  const handleSyncAnnual = () => {
    updatePlan((p) => { p.annuals = buildAnnuals(p); });
    message.success('已按工种规划汇总年度计划');
  };

  const handleProjectChange = (v: string) => {
    setSelectedProjectId(v);
    setCurrentProjectId(v ? +v : null);
  };

  // ---- 工种表格列 ----
  const occColumns: ColumnsType<OccItem> = [
    { title: '工种', dataIndex: 'occ', render: (v, _, i) => <Input size="small" value={v} onChange={(e) => updatePlan((p) => { p.occupations[i].occ = e.target.value; })} /> },
    { title: '等级', dataIndex: 'level', render: (v, _, i) => <Select size="small" value={v} style={{ width: 90 }} onChange={(val) => updatePlan((p) => { p.occupations[i].level = val; })} options={LEVELS.map((l) => ({ value: l, label: l }))} /> },
    { title: '规划人数', dataIndex: 'planCount', render: (v, _, i) => <InputNumber size="small" min={0} value={v} style={{ width: 80 }} onChange={(val) => updatePlan((p) => { p.occupations[i].planCount = val || 0; })} /> },
    { title: '已识别人数', dataIndex: 'identified', render: (v, _, i) => <InputNumber size="small" min={0} value={v} style={{ width: 80 }} onChange={(val) => updatePlan((p) => { p.occupations[i].identified = val || 0; })} /> },
    { title: '计划批次', dataIndex: 'batches', render: (v, _, i) => <InputNumber size="small" min={1} value={v} style={{ width: 70 }} onChange={(val) => updatePlan((p) => { p.occupations[i].batches = val || 1; })} /> },
    { title: '培训形式', dataIndex: 'form', render: (v, _, i) => <Select size="small" value={v} style={{ width: 110 }} onChange={(val) => updatePlan((p) => { p.occupations[i].form = val; })} options={TRAIN_FORMS.map((f) => ({ value: f, label: f }))} /> },
    { title: '计划开班', dataIndex: 'quarter', render: (v, _, i) => <Input size="small" value={v} style={{ width: 70 }} placeholder="Q1-Q4" onChange={(e) => updatePlan((p) => { p.occupations[i].quarter = e.target.value; })} /> },
    { title: '操作', key: 'actions', render: (_, r) => <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleDelOcc(r.id)}>删除</Button> },
  ];

  // ---- 年度计划列 ----
  const annualColumns: ColumnsType<AnnualItem> = [
    { title: '年度', dataIndex: 'year', render: (v) => <strong>{v}</strong> },
    { title: '计划开班数', dataIndex: 'classes', render: (v, _, i) => <InputNumber size="small" min={0} value={v} onChange={(val) => updatePlan((p) => { p.annuals[i].classes = val || 0; })} /> },
    { title: '计划参训人次', dataIndex: 'trainees', render: (v, _, i) => <InputNumber size="small" min={0} value={v} onChange={(val) => updatePlan((p) => { p.annuals[i].trainees = val || 0; })} /> },
    { title: '重点工种', dataIndex: 'focus', render: (v, _, i) => <Input size="small" value={v} onChange={(e) => updatePlan((p) => { p.annuals[i].focus = e.target.value; })} /> },
    { title: '预算口径（万元）', dataIndex: 'budget', render: (v, _, i) => <InputNumber size="small" min={0} step={0.1} value={parseFloat(v)} onChange={(val) => updatePlan((p) => { p.annuals[i].budget = String(val ?? 0); })} /> },
    { title: '备注', dataIndex: 'note', render: (v, _, i) => <Input size="small" value={v} onChange={(e) => updatePlan((p) => { p.annuals[i].note = e.target.value; })} /> },
  ];

  return (
    <div className="prelim-plan-page">
      <header className="pp-topbar">
        <div className="pp-topbar-left">
          <a className="pp-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="pp-title">初步方案</h1>
            <span className="pp-sub">{currentProject ? `${currentProject.enterprise} · 培训规划草案 · 步骤 2 / 9` : '规划草案 · 分班排课草案 · 步骤 2 / 9'}</span>
          </div>
        </div>
        <span className="pp-process-badge"><FileTextOutlined /> 人才识别 → 初步方案 → 最终方案</span>
      </header>

      <main className="pp-page">
        {/* 项目选择卡片 */}
        <div className="pp-card">
          <div className="pp-card-head">
            <div className="pp-card-head-title"><FolderOpenOutlined /> 培训项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="pp-form-group">
            <label>选择企业培训项目（基于人才识别结果编制 3–5 年规划草案）</label>
            <Select
              placeholder="请选择项目…"
              value={selectedProjectId || undefined}
              onChange={handleProjectChange}
              allowClear
              style={{ width: '100%' }}
              options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))}
            />
          </div>
          {currentProject && (
            <div className="pp-project-banner">
              <div className="pp-banner-item"><label>企业</label><strong>{currentProject.enterprise || '—'}</strong></div>
              <div className="pp-banner-item"><label>项目</label><strong>{currentProject.name || '—'}</strong></div>
              <div className="pp-banner-item"><label>主工种</label><strong>{currentProject.occupation || '—'}</strong></div>
              <div className="pp-banner-item"><label>已确认人才</label><strong>32 人</strong></div>
              <div className="pp-banner-item"><label>方案状态</label><strong>{plan.planStatus}</strong></div>
              <div className="pp-banner-item"><label>负责人</label><strong>{currentProject.owner || '—'}</strong></div>
            </div>
          )}
        </div>

        {/* 空状态 / 工作区 */}
        {!currentProject ? (
          <div className="pp-empty">
            <FileTextOutlined style={{ fontSize: 48, color: '#E2E8F0', marginBottom: 16 }} />
            <p>请先选择<strong>企业培训项目</strong>，再编制初步培训规划方案。</p>
            <p style={{ marginTop: 8, fontSize: 13 }}>也可从任务档案带项目参数进入。</p>
          </div>
        ) : (
          <>
            <div className="pp-flow-hint">
              <InfoCircleOutlined style={{ color: '#2A6DFF' }} />
              {' '}依据需求规格：<strong>规划草案 → 分班规则/智能分班 → 排课安排/冲突检测</strong>（法·日程计划：导入名单 → 智能分班 → 排课 → 检测冲突）
            </div>

            {/* 统计卡片 */}
            <div className="pp-stat-row">
              <div className="pp-stat-card plan"><div className="pp-stat-val">{stats.occCount}</div><div className="pp-stat-lbl">规划工种数</div></div>
              <div className="pp-stat-card"><div className="pp-stat-val">{stats.total}</div><div className="pp-stat-lbl">规划参训人次</div></div>
              <div className="pp-stat-card success"><div className="pp-stat-val">{stats.identified}</div><div className="pp-stat-lbl">已识别人数衔接</div></div>
              <div className="pp-stat-card warn"><div className="pp-stat-val">{stats.years}</div><div className="pp-stat-lbl">规划周期（年）</div></div>
            </div>

            {/* 标签页 */}
            <div className="pp-card">
              <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                items={[
                  {
                    key: 'basic', label: '方案概要',
                    children: (
                      <div>
                        <div className="pp-card-head">
                          <div className="pp-card-head-title"><FileTextOutlined /> 规划方案概要</div>
                          <span className="pp-hint">培训计划制定：工种、等级、时间、地点、培养模式</span>
                        </div>
                        <div className="pp-form-grid">
                          <div className="pp-form-group">
                            <label>规划周期</label>
                            <Select value={plan.planYears} style={{ width: '100%' }} onChange={(v) => updatePlan((p) => { p.planYears = v; })} options={[3, 4, 5].map((n) => ({ value: n, label: `${n} 年` }))} />
                          </div>
                          <div className="pp-form-group">
                            <label>培养模式</label>
                            <Select value={plan.trainMode} style={{ width: '100%' }} onChange={(v) => updatePlan((p) => { p.trainMode = v; })} options={['新型学徒制', '岗前培训', '技能提升', '企业内训'].map((m) => ({ value: m, label: m }))} />
                          </div>
                          <div className="pp-form-group">
                            <label>规划起始年度</label>
                            <InputNumber value={plan.startYear} style={{ width: '100%' }} min={2024} max={2035} onChange={(v) => updatePlan((p) => { p.startYear = v || new Date().getFullYear(); })} />
                          </div>
                          <div className="pp-form-group">
                            <label>方案状态</label>
                            <Select value={plan.planStatus} style={{ width: '100%' }} onChange={(v) => updatePlan((p) => { p.planStatus = v; })} options={['草案', '编制中', '已定稿'].map((s) => ({ value: s, label: s }))} />
                          </div>
                          <div className="pp-form-group full">
                            <label>规划目标摘要</label>
                            <Input.TextArea value={plan.planGoal} rows={4} onChange={(e) => updatePlan((p) => { p.planGoal = e.target.value; })} placeholder="根据人才识别结果，描述企业技能人才梯队建设目标…" />
                          </div>
                          <div className="pp-form-group full">
                            <label>主要培训地点</label>
                            <Input value={plan.planVenue} onChange={(e) => updatePlan((p) => { p.planVenue = e.target.value; })} placeholder="如：总部培训中心、华东实训基地" />
                          </div>
                        </div>
                        <div className="pp-toolbar">
                          <Button className="btn-plan" icon={<ThunderboltOutlined />} onClick={handleGenDraft}>智能生成规划草案</Button>
                          <Button icon={<SaveOutlined />} onClick={handleSaveBasic}>保存概要</Button>
                        </div>
                      </div>
                    ),
                  },
                  {
                    key: 'occupation', label: '工种等级规划',
                    children: (
                      <>
                        <div className="pp-card-head">
                          <div className="pp-card-head-title"><FileTextOutlined /> 工种等级规划表</div>
                          <Button size="small" icon={<PlusOutlined />} onClick={handleAddOcc}>新增工种</Button>
                        </div>
                        <p style={{ fontSize: 13, color: '#64748B', marginBottom: 16 }}>对接人才识别需求清单，明确各工种等级参训规模与批次安排。</p>
                        <Table dataSource={plan.occupations} columns={occColumns} rowKey="id" pagination={false}
                          locale={{ emptyText: '暂无工种规划，请新增或智能生成草案' }}
                        />
                        <div className="pp-card" style={{ marginTop: 24 }}>
                          <div className="pp-card-head"><div className="pp-card-head-title"><UserSwitchOutlined /> 关联人才识别数据</div></div>
                          <div style={{ fontSize: 14, lineHeight: 1.8, color: '#64748B' }}>
                            <p>企业 <strong>{currentProject?.enterprise || '—'}</strong> 人才识别汇总：</p>
                            <ul style={{ margin: '8px 0 0 20px' }}>
                              <li>已确认 <strong style={{ color: '#10B981' }}>{stats.identified}</strong> 人</li>
                              <li>待确认 <strong style={{ color: '#F59E0B' }}>8</strong> 人</li>
                              <li>未识别 <strong style={{ color: '#EF4444' }}>4</strong> 人</li>
                            </ul>
                            <p style={{ marginTop: 12 }}><a onClick={() => { setCurrentProjectId(currentProject?.id || null); navigateTo('zhixun-talent-identification'); }} style={{ color: '#2A6DFF', cursor: 'pointer' }}>查看人才识别明细 →</a></p>
                          </div>
                        </div>
                      </>
                    ),
                  },
                  {
                    key: 'annual', label: '年度培训计划',
                    children: (
                      <>
                        <div className="pp-card-head">
                          <div className="pp-card-head-title"><CalendarOutlined /> 年度培训计划</div>
                          <Button size="small" icon={<SyncOutlined />} onClick={handleSyncAnnual}>按工种汇总</Button>
                        </div>
                        <Table dataSource={plan.annuals} columns={annualColumns} rowKey="year" pagination={false}
                          locale={{ emptyText: '请先维护工种规划，再按工种汇总年度计划' }}
                        />
                      </>
                    ),
                  },
                  {
                    key: 'scheduling', label: '分班排课工作台',
                    children: (
                      <div>
                        <div className="pp-card-head">
                          <div className="pp-card-head-title"><NodeIndexOutlined /> 分班排课（草案）</div>
                          <span className="pp-hint">完整功能请进入协同工作台</span>
                        </div>
                        <p style={{ fontSize: 14, color: '#64748B', lineHeight: 1.8, marginBottom: 20 }}>导入学员 → 分班规则 → 智能分班 → 排课安排 → 冲突检测 → 保存课表草案。</p>
                        <div className="pp-sub-stat-row">
                          <div className="pp-sub-stat"><div className="pp-sub-stat-v">0</div><div className="pp-sub-stat-l">待分班</div></div>
                          <div className="pp-sub-stat"><div className="pp-sub-stat-v">0</div><div className="pp-sub-stat-l">班级</div></div>
                          <div className="pp-sub-stat"><div className="pp-sub-stat-v">0</div><div className="pp-sub-stat-l">课程</div></div>
                          <div className="pp-sub-stat"><div className="pp-sub-stat-v">—</div><div className="pp-sub-stat-l">课表</div></div>
                        </div>
                        <div className="pp-toolbar">
                          <Button className="btn-plan" icon={<ApartmentOutlined />}>协同工作台</Button>
                          <Button type="primary" icon={<NodeIndexOutlined />}>分班管理</Button>
                          <Button type="primary" icon={<CalendarOutlined />}>排课管理</Button>
                        </div>
                      </div>
                    ),
                  },
                ]}
              />
            </div>

            {/* 底部操作 */}
            <div className="pp-footer-actions">
              <Button icon={<UserSwitchOutlined />} onClick={() => { setCurrentProjectId(currentProject?.id || null); navigateTo('zhixun-talent-identification'); }}>返回人才识别</Button>
              <Button onClick={() => navigateTo('zhixun-training-process')}>保存并返回流程</Button>
              <Button type="primary" icon={<ArrowRightOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-final-plan'); }}>完成初步方案，进入最终方案</Button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
