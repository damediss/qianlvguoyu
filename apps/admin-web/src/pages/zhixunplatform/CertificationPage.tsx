import { useState, useMemo, useCallback, useEffect } from 'react';
import { Typography, Select, Input, Button, Table, Modal, message } from 'antd';
import {
  ArrowLeftOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  SafetyCertificateOutlined,
  CheckCircleOutlined,
  EditOutlined,
  FileExcelOutlined,
  CloudUploadOutlined,
  TrophyOutlined,
  NodeIndexOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './CertificationPage.css';

// ============================================================
// 类型
// ============================================================
interface Candidate {
  studentId: number;
  name: string;
  classId: number;
  className: string;
  occupation: string;
  conditionOk: boolean | null;
  theoryScore: number | null;
  practicalScore: number | null;
  result: string;
  rsbStatus: string;
  certNo: string;
  certStatus: string;
}

// ============================================================
// 常量
// ============================================================
const DEFAULT_TASKS = [
  { id: 1, name: '2026年度安全培训计划编制', owner: '张明', enterprise: '广西建工集团', occupation: '安全员', skillLevel: '高级', planCount: 120 },
  { id: 2, name: '电工高级班认定材料整理', owner: '李芳', enterprise: '南宁供电局', occupation: '电工', skillLevel: '高级', planCount: 42 },
  { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', enterprise: '柳州钢铁有限公司', occupation: '焊工', skillLevel: '中级', planCount: 36 },
  { id: 4, name: '焊工中级班排课协调', owner: '赵丽', enterprise: '桂林旅游股份公司', occupation: '焊工', skillLevel: '中级', planCount: 28 },
  { id: 5, name: '培训档案年度归档', owner: '陈伟', enterprise: '梧州港务集团', occupation: '起重工', skillLevel: '初级', planCount: 15 },
];

const FLOW_LABELS = ['条件审核', '理论/实操', '获证花名册', '人社提交', '证书发放'];
const FLOW_ICONS = [CheckCircleOutlined, EditOutlined, FileExcelOutlined, CloudUploadOutlined, TrophyOutlined];
const PASS_SCORE = 60;

function computeResult(s: Candidate): string {
  if (s.conditionOk === false) return '未通过';
  if (s.theoryScore == null || s.practicalScore == null) return '待考';
  if (s.theoryScore >= PASS_SCORE && s.practicalScore >= PASS_SCORE) return '通过';
  return '未通过';
}

// ============================================================
// Mock 数据
// ============================================================
const MOCK_STUDENTS = [
  { studentId: 1, name: '张建国' },
  { studentId: 2, name: '李秀英' },
  { studentId: 3, name: '王志强' },
  { studentId: 4, name: '陈丽华' },
  { studentId: 5, name: '刘明辉' },
  { studentId: 6, name: '赵红梅' },
  { studentId: 7, name: '周建华' },
  { studentId: 8, name: '吴桂芳' },
  { studentId: 9, name: '黄伟民' },
  { studentId: 10, name: '孙晓燕' },
  { studentId: 11, name: '钱国栋' },
  { studentId: 12, name: '杨玉兰' },
];

function buildMockCandidates(projectId: number): Candidate[] {
  const task = DEFAULT_TASKS.find((t) => t.id === projectId);
  if (!task) return [];
  const cls1 = `${task.occupation || '电工'}高级工1班`;
  const cls2 = `${task.occupation || '电工'}中级工1班`;
  const list: Candidate[] = [];
  MOCK_STUDENTS.slice(0, 8).forEach((s, i) => {
    const theory = i < 4 ? 55 + Math.floor(Math.random() * 45) : null;
    const practical = i < 4 ? 55 + Math.floor(Math.random() * 45) : null;
    const cond = i < 6 ? true : null;
    list.push({
      studentId: s.studentId, name: s.name,
      classId: i < 5 ? 1 : 2, className: i < 5 ? cls1 : cls2,
      occupation: task.occupation || '—',
      conditionOk: cond, theoryScore: theory, practicalScore: practical,
      result: '待考', rsbStatus: '未提交', certNo: '', certStatus: '未发证',
    });
  });
  list.forEach((c) => { c.result = computeResult(c); });
  return list;
}

// ============================================================
// 主页面
// ============================================================
export function CertificationPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
  const [candidates, setCandidates] = useState<Candidate[]>([]);
  const [filterClass, setFilterClass] = useState('');
  const [filterResult, setFilterResult] = useState('');
  const [examModalOpen, setExamModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [theoryScore, setTheoryScore] = useState('');
  const [practicalScore, setPracticalScore] = useState('');
  const [conditionVal, setConditionVal] = useState('');

  const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;

  useEffect(() => {
    if (selectedProjectId) {
      setCandidates(buildMockCandidates(+selectedProjectId));
    } else {
      setCandidates([]);
    }
  }, [selectedProjectId]);

  const handleProjectChange = (v: string) => {
    setSelectedProjectId(v);
    setCurrentProjectId(v ? +v : null);
    setFilterClass('');
    setFilterResult('');
  };

  const classNames = useMemo(() => [...new Set(candidates.map((c) => c.className))], [candidates]);

  const filteredList = useMemo(() => {
    return candidates.filter((s) => {
      if (filterClass && s.className !== filterClass) return false;
      if (filterResult && s.result !== filterResult) return false;
      return true;
    });
  }, [candidates, filterClass, filterResult]);

  const stats = useMemo(() => {
    const total = candidates.length;
    const pending = candidates.filter((s) => s.result === '待考').length;
    const passed = candidates.filter((s) => s.result === '通过').length;
    const rate = total ? Math.round((passed / total) * 1000) / 10 : 0;
    const certified = candidates.filter((s) => s.certStatus === '已发证').length;
    return { total, pending, passRate: rate, certified };
  }, [candidates]);

  const flowStep = useMemo(() => {
    if (!candidates.length) return 0;
    const cond = candidates.filter((s) => s.conditionOk !== null).length / candidates.length;
    const exam = candidates.filter((s) => s.result !== '待考').length / candidates.length;
    const pass = candidates.filter((s) => s.result === '通过').length / candidates.length;
    const rsb = candidates.filter((s) => s.rsbStatus !== '未提交').length / candidates.length;
    const cert = candidates.filter((s) => s.certStatus === '已发证').length / candidates.length;
    if (cert > 0.5) return 5;
    if (rsb > 0.3) return 4;
    if (pass > 0.3) return 3;
    if (exam > 0.5) return 2;
    if (cond > 0.5) return 1;
    return 0;
  }, [candidates]);

  const updateCandidates = useCallback((updater: (prev: Candidate[]) => Candidate[]) => {
    setCandidates((prev) => {
      const next = updater(prev.map((c) => ({ ...c })));
      next.forEach((c) => { c.result = computeResult(c); });
      return next;
    });
  }, []);

  const handleCheckCondition = () => {
    if (!candidates.length) { message.warning('暂无学员'); return; }
    updateCandidates((prev) =>
      prev.map((s) => ({ ...s, conditionOk: s.conditionOk === null ? Math.random() > 0.08 : s.conditionOk })),
    );
    message.success('已完成批量认定条件审核');
  };

  const handleBatchExam = () => {
    const eligible = candidates.filter((s) => s.conditionOk !== false);
    if (!eligible.length) { message.warning('请先完成条件审核'); return; }
    updateCandidates((prev) =>
      prev.map((s) => {
        if (s.result !== '待考' || s.conditionOk === false) return s;
        return { ...s, theoryScore: 55 + Math.floor(Math.random() * 45), practicalScore: 55 + Math.floor(Math.random() * 45) };
      }),
    );
    message.success(`已为 ${eligible.length} 名符合条件学员录入考核成绩`);
  };

  const handleGenRoster = () => {
    const passed = candidates.filter((s) => s.result === '通过');
    if (!passed.length) { message.warning('暂无通过学员，无法生成花名册'); return; }
    message.success(`已生成获证花名册：${passed.length} 人`);
  };

  const handleSubmitRsb = () => {
    const passed = candidates.filter((s) => s.result === '通过' && s.rsbStatus === '未提交');
    if (!passed.length) { message.warning('暂无待提交人社的通过学员'); return; }
    updateCandidates((prev) =>
      prev.map((s) => {
        if (s.result === '通过' && s.rsbStatus === '未提交') return { ...s, rsbStatus: '审批中' };
        return s;
      }),
    );
    message.success(`已向人社系统提交 ${passed.length} 人认定材料`);
  };

  const handleIssueCert = () => {
    const ready = candidates.filter((s) => s.result === '通过' && (s.rsbStatus === '审批中' || s.rsbStatus === '已通过'));
    if (!ready.length) { message.warning('暂无待发证学员'); return; }
    updateCandidates((prev) =>
      prev.map((s, i) => {
        if (s.result === '通过' && (s.rsbStatus === '审批中' || s.rsbStatus === '已通过')) {
          return { ...s, rsbStatus: '已通过', certStatus: '已发证', certNo: 'GX' + (selectedProjectId || '0') + String(s.studentId).slice(-4) + (1000 + i) };
        }
        return s;
      }),
    );
    message.success(`已为 ${ready.length} 名学员登记证书发放`);
  };

  const openExamModal = (studentId: number) => {
    const s = candidates.find((x) => x.studentId === studentId);
    if (!s) return;
    setEditingId(studentId);
    setTheoryScore(s.theoryScore != null ? String(s.theoryScore) : '');
    setPracticalScore(s.practicalScore != null ? String(s.practicalScore) : '');
    setConditionVal(s.conditionOk === true ? 'true' : s.conditionOk === false ? 'false' : '');
    setExamModalOpen(true);
  };

  const saveExamModal = () => {
    if (editingId == null) return;
    updateCandidates((prev) =>
      prev.map((s) => {
        if (s.studentId !== editingId) return s;
        return {
          ...s,
          conditionOk: conditionVal === '' ? null : conditionVal === 'true',
          theoryScore: theoryScore === '' ? null : parseInt(theoryScore, 10),
          practicalScore: practicalScore === '' ? null : parseInt(practicalScore, 10),
        };
      }),
    );
    setExamModalOpen(false);
    message.success('考核成绩已保存');
  };

  const columns: ColumnsType<Candidate> = [
    { title: '姓名', dataIndex: 'name', render: (v) => <strong>{v}</strong> },
    { title: '班级', dataIndex: 'className', render: (v) => v || '—' },
    { title: '工种', dataIndex: 'occupation', render: (v) => v || '—' },
    {
      title: '条件', dataIndex: 'conditionOk',
      render: (v) => {
        if (v === true) return <span className="status-badge st-pass">符合</span>;
        if (v === false) return <span className="status-badge st-fail">不符合</span>;
        return <span className="status-badge st-pending">待审核</span>;
      },
    },
    { title: '理论', dataIndex: 'theoryScore', render: (v) => v != null ? v : '—' },
    { title: '实操', dataIndex: 'practicalScore', render: (v) => v != null ? v : '—' },
    {
      title: '结果', dataIndex: 'result',
      render: (v) => {
        if (v === '通过') return <span className="status-badge st-pass">通过</span>;
        if (v === '未通过') return <span className="status-badge st-fail">未通过</span>;
        return <span className="status-badge st-pending">待考</span>;
      },
    },
    {
      title: '人社', dataIndex: 'rsbStatus',
      render: (v) => {
        const map: Record<string, string> = { '未提交': 'st-pending', '已提交': 'st-rsb', '审批中': 'st-rsb', '已通过': 'st-pass' };
        return <span className={`status-badge ${map[v] || 'st-pending'}`}>{v}</span>;
      },
    },
    {
      title: '证书', key: 'cert',
      render: (_, r) => (
        <div>
          {r.certStatus === '已发证' ? <span className="status-badge st-cert">已发证</span> : <span className="status-badge st-pending">未发证</span>}
          {r.certNo && <br />}
          {r.certNo && <small>{r.certNo}</small>}
        </div>
      ),
    },
    {
      title: '操作', key: 'actions',
      render: (_, r) => (
        <div className="action-btns" onClick={(e) => e.stopPropagation()}>
          <button className="action-btn" title="录入成绩" onClick={() => openExamModal(r.studentId)}><EditOutlined /></button>
        </div>
      ),
    },
  ];

  return (
    <div className="rd-page">
      <header className="rd-topbar">
        <div className="rd-topbar-left">
          <a className="rd-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="rd-title">认定准备</h1>
            <span className="rd-sub">{currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 6 / 9` : '按项目组织认定准备与获证 · 步骤 6 / 9'}</span>
          </div>
        </div>
        <span className="rd-process-badge"><SafetyCertificateOutlined /> … → 认定准备 → 申报服务 → …</span>
      </header>

      <main className="rd-page-body">
        {/* 项目选择 */}
        <div className="rd-card">
          <div className="rd-card-head">
            <div className="rd-card-head-title"><FolderOpenOutlined /> 培训项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="rd-form-group">
            <label>选择企业培训项目（认定针对该项目已分班学员）</label>
            <Select placeholder="请选择项目…" value={selectedProjectId || undefined} onChange={handleProjectChange} allowClear style={{ width: '100%' }}
              options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))} />
          </div>
          {currentProject && (
            <div className="rd-project-banner">
              <div className="rd-banner-item"><label>企业</label><strong>{currentProject.enterprise || '—'}</strong></div>
              <div className="rd-banner-item"><label>培训项目</label><strong>{currentProject.name || '—'}</strong></div>
              <div className="rd-banner-item"><label>工种 / 等级</label><strong>{currentProject.occupation || '—'}{currentProject.skillLevel ? ' · ' + currentProject.skillLevel : ''}</strong></div>
              <div className="rd-banner-item"><label>参考人数</label><strong>{candidates.length} 人</strong></div>
              <div className="rd-banner-item"><label>已通过</label><strong>{candidates.filter((s) => s.result === '通过').length} 人</strong></div>
            </div>
          )}
        </div>

        {!currentProject ? (
          <div className="rd-empty">
            <InfoCircleOutlined style={{ fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' }} />
            <p>请先选择<strong>企业培训项目</strong>，再办理技能等级认定与获证花名册。</p>
            <p style={{ marginTop: 8, fontSize: 13 }}>需先完成 人才识别、培训；也可从任务档案进入。</p>
          </div>
        ) : candidates.length === 0 ? (
          <div className="rd-alert warn">
            <ExclamationCircleOutlined />
            <div>本项目尚无已分班学员，请先 分班 并完成培训。</div>
          </div>
        ) : (
          <>
            {/* 统计卡片 */}
            <div className="rd-stat-row">
              <div className="rd-stat-card"><div className="rd-stat-val">{stats.total}</div><div className="rd-stat-lbl">参考人数</div></div>
              <div className="rd-stat-card"><div className="rd-stat-val warn">{stats.pending}</div><div className="rd-stat-lbl">待认定</div></div>
              <div className="rd-stat-card"><div className="rd-stat-val success">{stats.passRate}%</div><div className="rd-stat-lbl">通过率</div></div>
              <div className="rd-stat-card"><div className="rd-stat-val">{stats.certified}</div><div className="rd-stat-lbl">已获证</div></div>
            </div>

            {/* 认定流程 + 表 */}
            <div className="rd-card">
              <div className="rd-card-head">
                <div className="rd-card-head-title"><NodeIndexOutlined /> 认定流程</div>
                <span className="rd-hint">条件审核 → 理论/实操考核 → 花名册 → 人社提交 → 证书发放</span>
              </div>
              <div className="rd-flow-steps">
                {FLOW_LABELS.map((label, i) => {
                  const Icon = FLOW_ICONS[i];
                  let cls = 'rd-flow-step';
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
              <div className="rd-toolbar">
                <Button type="primary" icon={<CheckCircleOutlined />} onClick={handleCheckCondition}>批量条件审核</Button>
                <Button icon={<EditOutlined />} onClick={handleBatchExam}>批量录入考核成绩</Button>
                <Button icon={<FileExcelOutlined />} onClick={handleGenRoster}>生成获证花名册</Button>
                <Button icon={<CloudUploadOutlined />} onClick={handleSubmitRsb}>提交人社系统</Button>
                <Button style={{ background: '#10B981', color: '#fff', borderColor: '#10B981' }} icon={<TrophyOutlined />} onClick={handleIssueCert}>证书发放登记</Button>
              </div>

              <div className="rd-filter-bar">
                <div className="rd-form-group" style={{ minWidth: 160, margin: 0 }}>
                  <label>班级</label>
                  <Select placeholder="全部" value={filterClass || undefined} onChange={(v) => setFilterClass(v || '')} allowClear style={{ width: '100%' }}
                    options={classNames.map((n) => ({ value: n, label: n }))} />
                </div>
                <div className="rd-form-group" style={{ minWidth: 160, margin: 0 }}>
                  <label>考核结果</label>
                  <Select placeholder="全部" value={filterResult || undefined} onChange={(v) => setFilterResult(v || '')} allowClear style={{ width: '100%' }}
                    options={['待考', '通过', '未通过'].map((v) => ({ value: v, label: v }))} />
                </div>
              </div>

              <Table dataSource={filteredList} columns={columns} rowKey="studentId" pagination={false}
                locale={{ emptyText: '暂无学员，请先分班' }} />
            </div>

            {/* 底部操作 */}
            <div className="rd-footer-actions">
              <Button icon={<SafetyCertificateOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-training-implementation'); }}>返回培训</Button>
              <Button onClick={() => navigateTo('zhixun-training-process')}>保存并返回流程</Button>
              <Button type="primary" style={{ background: '#10B981', borderColor: '#10B981' }} icon={<ArrowLeftOutlined style={{ transform: 'rotate(180deg)' }} />}
                onClick={() => {
                  if (!currentProject) { message.warning('请先选择项目'); return; }
                  message.success(`项目「${currentProject.name}」认定信息已保存，进入申报环节`);
                  setCurrentProjectId(currentProject.id);
                  navigateTo('zhixun-declaration-service');
                }}>
                完成认定准备，进入申报服务
              </Button>
            </div>

            {/* 考核成绩弹窗 */}
            <Modal open={examModalOpen} onCancel={() => setExamModalOpen(false)} title="录入考核成绩"
              footer={
                <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                  <Button onClick={() => setExamModalOpen(false)}>取消</Button>
                  <Button type="primary" onClick={saveExamModal}>保存</Button>
                </div>
              }>
              <p style={{ marginBottom: 16, fontWeight: 600 }}>
                {candidates.find((x) => x.studentId === editingId)?.name} · {candidates.find((x) => x.studentId === editingId)?.className}
              </p>
              <div className="rd-form-grid">
                <div className="rd-form-group">
                  <label>理论成绩</label>
                  <Input type="number" min={0} max={100} placeholder="0-100" value={theoryScore} onChange={(e) => setTheoryScore(e.target.value)} />
                </div>
                <div className="rd-form-group">
                  <label>实操成绩</label>
                  <Input type="number" min={0} max={100} placeholder="0-100" value={practicalScore} onChange={(e) => setPracticalScore(e.target.value)} />
                </div>
                <div className="rd-form-group" style={{ gridColumn: '1/-1' }}>
                  <label>认定条件符合</label>
                  <Select value={conditionVal || undefined} onChange={(v) => setConditionVal(v || '')} style={{ width: '100%' }}
                    options={[{ value: 'true', label: '符合' }, { value: 'false', label: '不符合' }, { value: '', label: '待审核' }]} />
                </div>
              </div>
            </Modal>
          </>
        )}
      </main>
    </div>
  );
}
