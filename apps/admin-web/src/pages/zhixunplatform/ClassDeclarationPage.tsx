import { useState, useMemo, useCallback, useEffect } from 'react';
import { Typography, Select, Input, Button, Table, Modal, message } from 'antd';
import {
  ArrowLeftOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  NodeIndexOutlined,
  FileZipOutlined,
  CloudUploadOutlined,
  BellOutlined,
  EditOutlined,
  CalendarOutlined,
  ArrowRightOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  CloseOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './ClassDeclarationPage.css';

// ============================================================
// 类型
// ============================================================
interface ClassRecord {
  classId: number;
  className: string;
  studentCount: number;
  openDate: string;
  venue: string;
  ceremonyTime: string;
  headTeacher: string;
  status: string;
  materials: Record<string, boolean>;
  commitmentSigned: boolean;
  ceremonyDone: boolean;
  remark: string;
  rsbSubmittedAt: string | null;
  enterpriseNotified: boolean;
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

const VENUES = ['总部培训中心', '华东实训基地A区', '华南技能中心', '柳州培训基地', '桂林教学点'];
const MATERIAL_KEYS = [
  { key: 'application', label: '开班申请表' },
  { key: 'roster', label: '开班花名册' },
  { key: 'commitment', label: '开班承诺书' },
  { key: 'schedule', label: '已发布课表' },
  { key: 'venue', label: '场地确认单' },
];
const FLOW_LABELS = ['开班申请', '承诺书签署', '人社提交', '审批通过', '正式开班'];
const FLOW_ICONS = [FileZipOutlined, EditOutlined, CloudUploadOutlined, CheckCircleOutlined, ArrowRightOutlined];

function statusClass(status: string) {
  const map: Record<string, string> = {
    '待申请': 'st-draft',
    '材料准备中': 'st-prep',
    '已提交人社': 'st-submit',
    '审批中': 'st-review',
    '已批准': 'st-approved',
    '正式开班': 'st-open',
  };
  return map[status] || 'st-draft';
}

function flowIndexForStatus(status: string) {
  const order = ['待申请', '材料准备中', '已提交人社', '审批中', '已批准', '正式开班'];
  const i = order.indexOf(status);
  if (i <= 0) return 0;
  if (i === 1) return 1;
  if (i === 2) return 2;
  if (i === 3) return 3;
  if (i === 4) return 4;
  return 5;
}

// ============================================================
// Mock 班级数据生成
// ============================================================
function buildMockRecords(projectId: number): ClassRecord[] {
  const task = DEFAULT_TASKS.find((t) => t.id === projectId);
  if (!task) return [];
  return [
    {
      classId: 1, className: `${task.occupation || '电工'}高级工1班`, studentCount: 30,
      openDate: '2026-07-01', venue: VENUES[0], ceremonyTime: '09:00',
      headTeacher: task.owner, status: '材料准备中',
      materials: { application: true, roster: true, commitment: false, schedule: true, venue: false },
      commitmentSigned: false, ceremonyDone: false, remark: '', rsbSubmittedAt: null, enterpriseNotified: false,
    },
    {
      classId: 2, className: `${task.occupation || '电工'}中级工1班`, studentCount: 25,
      openDate: '2026-07-15', venue: VENUES[1], ceremonyTime: '10:00',
      headTeacher: '李老师', status: '待申请',
      materials: { application: false, roster: false, commitment: false, schedule: false, venue: false },
      commitmentSigned: false, ceremonyDone: false, remark: '', rsbSubmittedAt: null, enterpriseNotified: false,
    },
  ];
}

function materialsProgress(m: Record<string, boolean>) {
  const total = MATERIAL_KEYS.length;
  const done = MATERIAL_KEYS.filter((k) => m[k.key]).length;
  return `${done}/${total}`;
}

// ============================================================
// 主页面
// ============================================================
export function ClassDeclarationPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
  const [records, setRecords] = useState<ClassRecord[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  // modal form state
  const [mOpenDate, setMOpenDate] = useState('');
  const [mCeremonyTime, setMCeremonyTime] = useState('09:00');
  const [mVenue, setMVenue] = useState(VENUES[0]);
  const [mHeadTeacher, setMHeadTeacher] = useState('');
  const [mStatus, setMStatus] = useState('待申请');
  const [mCommitment, setMCommitment] = useState(false);
  const [mCeremony, setMCeremony] = useState(false);
  const [mRemark, setMRemark] = useState('');
  const [mMaterials, setMMaterials] = useState<Record<string, boolean>>({});

  const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;

  useEffect(() => {
    if (selectedProjectId) {
      setRecords(buildMockRecords(+selectedProjectId));
    } else {
      setRecords([]);
    }
  }, [selectedProjectId]);

  const handleProjectChange = (v: string) => {
    setSelectedProjectId(v);
    setCurrentProjectId(v ? +v : null);
  };

  const stats = useMemo(() => {
    const total = records.length;
    const pending = records.filter((r) => r.status === '审批中' || r.status === '已提交人社').length;
    const opened = records.filter((r) => r.status === '正式开班').length;
    const ceremony = records.filter((r) => r.ceremonyDone).length;
    return { total, pending, opened, ceremony };
  }, [records]);

  const flowAvg = useMemo(() => {
    if (!records.length) return 0;
    return Math.round(records.reduce((s, r) => s + flowIndexForStatus(r.status), 0) / records.length);
  }, [records]);

  const openModal = useCallback((classId: number) => {
    const r = records.find((x) => x.classId === classId);
    if (!r) return;
    setEditingId(classId);
    setMOpenDate(r.openDate || '');
    setMCeremonyTime(r.ceremonyTime || '09:00');
    setMVenue(r.venue || VENUES[0]);
    setMHeadTeacher(r.headTeacher || '');
    setMStatus(r.status);
    setMCommitment(!!r.commitmentSigned);
    setMCeremony(!!r.ceremonyDone);
    setMRemark(r.remark || '');
    setMMaterials({ ...r.materials });
    setModalOpen(true);
  }, [records]);

  const saveModal = useCallback(() => {
    if (editingId == null) return;
    setRecords((prev) =>
      prev.map((r) =>
        r.classId === editingId
          ? {
              ...r,
              openDate: mOpenDate,
              ceremonyTime: mCeremonyTime,
              venue: mVenue,
              headTeacher: mHeadTeacher,
              status: mStatus,
              materials: { ...mMaterials },
              commitmentSigned: mCommitment,
              ceremonyDone: mCeremony,
              remark: mRemark,
            }
          : r,
      ),
    );
    setModalOpen(false);
    message.success('班级开班信息已保存');
  }, [editingId, mOpenDate, mCeremonyTime, mVenue, mHeadTeacher, mStatus, mMaterials, mCommitment, mCeremony, mRemark]);

  const allMaterialsReady = useCallback((r: ClassRecord) => {
    return MATERIAL_KEYS.every((k) => r.materials[k.key]);
  }, []);

  const handleGenMaterials = () => {
    if (!records.length) { message.warning('暂无班级'); return; }
    setRecords((prev) =>
      prev.map((r) => {
        const m = { ...r.materials };
        MATERIAL_KEYS.forEach((k) => { m[k.key] = true; });
        return { ...r, materials: m, status: r.status === '待申请' ? '材料准备中' : r.status };
      }),
    );
    message.success('已为本项目全部班级勾选开班材料清单');
  };

  const submitToRsb = (targets: ClassRecord[], single: boolean) => {
    const ready = targets.filter((r) => allMaterialsReady(r));
    const notReady = targets.filter((r) => !allMaterialsReady(r));
    if (!ready.length) {
      message.warning('请先完善开班材料清单（申请表、花名册、承诺书、课表、场地确认单）');
      return;
    }
    setRecords((prev) =>
      prev.map((r) => {
        if (!ready.find((x) => x.classId === r.classId)) return r;
        return { ...r, status: '已提交人社', rsbSubmittedAt: new Date().toISOString().slice(0, 10) };
      }),
    );
    message.success(single ? `班级「${ready[0].className}」开班材料已提交人社系统` : `已向人社系统提交 ${ready.length} 个班级的开班材料`);
  };

  const handleNotifyEnterprise = () => {
    if (!currentProject) return;
    setRecords((prev) => prev.map((r) => ({ ...r, enterpriseNotified: true })));
    message.success(`已向企业「${currentProject.enterprise}」发送开班时间与场地确认通知`);
  };

  const columns: ColumnsType<ClassRecord> = [
    { title: '班级', dataIndex: 'className', render: (v) => <strong>{v}</strong> },
    { title: '学员数', dataIndex: 'studentCount', render: (v) => `${v} 人` },
    { title: '开班日期', dataIndex: 'openDate', render: (v) => v || '—' },
    { title: '场地', dataIndex: 'venue', render: (v) => v || '—' },
    { title: '仪式时间', dataIndex: 'ceremonyTime', render: (v) => v || '—' },
    { title: '材料', key: 'materials', render: (_, r) => materialsProgress(r.materials || {}) },
    { title: '状态', dataIndex: 'status', render: (v) => <span className={`status-badge ${statusClass(v)}`}>{v}</span> },
    {
      title: '操作', key: 'actions',
      render: (_, r) => (
        <div className="action-btns" onClick={(e) => e.stopPropagation()}>
          <button className="action-btn" title="编辑" onClick={() => openModal(r.classId)}><EditOutlined /></button>
          <button className="action-btn" title="提交人社" onClick={() => submitToRsb([r], true)}><CloudUploadOutlined /></button>
        </div>
      ),
    },
  ];

  return (
    <div className="kb-page">
      <header className="kb-topbar">
        <div className="kb-topbar-left">
          <a className="kb-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="kb-title">开班申报</h1>
            <span className="kb-sub">{currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 4 / 9` : '按项目办理开班申报 · 步骤 4 / 9'}</span>
          </div>
        </div>
        <span className="kb-process-badge"><CheckCircleOutlined /> … → 开班申报 → 培训实施 → …</span>
      </header>

      <main className="kb-page-body">
        {/* 项目选择 */}
        <div className="kb-card">
          <div className="kb-card-head">
            <div className="kb-card-head-title"><FolderOpenOutlined /> 培训项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="kb-form-group">
            <label>选择企业培训项目（开班针对该项目已排课班级）</label>
            <Select placeholder="请选择项目…" value={selectedProjectId || undefined} onChange={handleProjectChange} allowClear style={{ width: '100%' }}
              options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))} />
          </div>
          {currentProject && (
            <div className="kb-project-banner">
              <div className="kb-banner-item"><label>企业</label><strong>{currentProject.enterprise || '—'}</strong></div>
              <div className="kb-banner-item"><label>培训项目</label><strong>{currentProject.name || '—'}</strong></div>
              <div className="kb-banner-item"><label>工种</label><strong>{currentProject.occupation || '—'}</strong></div>
              <div className="kb-banner-item"><label>班级数</label><strong>{records.length} 个</strong></div>
              <div className="kb-banner-item"><label>负责人</label><strong>{currentProject.owner || '—'}</strong></div>
            </div>
          )}
        </div>

        {!currentProject ? (
          <div className="kb-empty">
            <InfoCircleOutlined style={{ fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' }} />
            <p>请先选择<strong>企业培训项目</strong>，再办理各班级开班申请与仪式。</p>
            <p style={{ marginTop: 8, fontSize: 13 }}>需先完成 人才识别、最终方案；也可从任务档案进入。</p>
          </div>
        ) : records.length === 0 ? (
          <div className="kb-alert warn">
            <ExclamationCircleOutlined />
            <div>本项目尚无班级，请先 人才识别 并完成 最终方案。</div>
          </div>
        ) : (
          <>
            {/* 统计卡片 */}
            <div className="kb-stat-row">
              <div className="kb-stat-card"><div className="kb-stat-val">{stats.total}</div><div className="kb-stat-lbl">待办班级</div></div>
              <div className="kb-stat-card"><div className="kb-stat-val warn">{stats.pending}</div><div className="kb-stat-lbl">审批中</div></div>
              <div className="kb-stat-card"><div className="kb-stat-val success">{stats.opened}</div><div className="kb-stat-lbl">已正式开班</div></div>
              <div className="kb-stat-card"><div className="kb-stat-val">{stats.ceremony}</div><div className="kb-stat-lbl">仪式已完成</div></div>
            </div>

            {/* 开班流程 */}
            <div className="kb-card">
              <div className="kb-card-head">
                <div className="kb-card-head-title"><NodeIndexOutlined /> 开班流程</div>
                <span className="kb-hint">开班申请 → 承诺书 → 人社提交 → 审批通过 → 正式开班</span>
              </div>
              <div className="kb-flow-steps">
                {FLOW_LABELS.map((label, i) => {
                  const Icon = FLOW_ICONS[i];
                  let cls = 'kb-flow-step';
                  if (i < flowAvg) cls += ' done';
                  else if (i === flowAvg) cls += ' active';
                  return (
                    <div key={i} className={cls}>
                      <Icon style={{ display: 'block', fontSize: 18, marginBottom: 6 }} />
                      {label}
                    </div>
                  );
                })}
              </div>
              <div className="kb-toolbar">
                <Button type="primary" icon={<FileZipOutlined />} onClick={handleGenMaterials}>批量生成开班材料包</Button>
                <Button icon={<CloudUploadOutlined />} onClick={() => submitToRsb(records, false)}>提交人社系统</Button>
                <Button icon={<BellOutlined />} onClick={handleNotifyEnterprise}>通知企业确认</Button>
              </div>
            </div>

            {/* 班级开班台账 */}
            <div className="kb-card">
              <div className="kb-card-head">
                <div className="kb-card-head-title"><CalendarOutlined /> 班级开班台账</div>
              </div>
              <Table dataSource={records} columns={columns} rowKey="classId" pagination={false}
                locale={{ emptyText: '暂无班级，请先分班排课' }} />
            </div>

            {/* 底部操作 */}
            <div className="kb-footer-actions">
              <Button icon={<CalendarOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-final-plan'); }}>返回最终方案</Button>
              <Button onClick={() => navigateTo('zhixun-training-process')}>保存并返回流程</Button>
              <Button type="primary" icon={<ArrowRightOutlined />} style={{ background: '#10B981', borderColor: '#10B981' }}
                onClick={() => {
                  if (!currentProject) { message.warning('请先选择项目'); return; }
                  message.success(`项目「${currentProject.name}」开班信息已保存，进入培训环节`);
                  setCurrentProjectId(currentProject.id);
                  navigateTo('zhixun-training-implementation');
                }}>
                完成开班申报，进入培训实施
              </Button>
            </div>

            {/* 编辑弹窗 */}
            <Modal
              open={modalOpen}
              onCancel={() => setModalOpen(false)}
              title="班级开班信息"
              footer={
                <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                  <Button onClick={() => setModalOpen(false)}>取消</Button>
                  <Button type="primary" onClick={saveModal}>保存</Button>
                </div>
              }
              width={560}
            >
              <div className="kb-modal-form">
                <div className="kb-form-group-full">
                  <label>班级名称</label>
                  <Input disabled value={records.find((r) => r.classId === editingId)?.className || ''} />
                </div>
                <div className="kb-form-grid">
                  <div className="kb-form-group">
                    <label>计划开班日期</label>
                    <Input type="date" value={mOpenDate} onChange={(e) => setMOpenDate(e.target.value)} />
                  </div>
                  <div className="kb-form-group">
                    <label>开班仪式时间</label>
                    <Input type="time" value={mCeremonyTime} onChange={(e) => setMCeremonyTime(e.target.value)} />
                  </div>
                  <div className="kb-form-group">
                    <label>培训场地</label>
                    <Select value={mVenue} onChange={setMVenue} style={{ width: '100%' }}
                      options={VENUES.map((v) => ({ value: v, label: v }))} />
                  </div>
                  <div className="kb-form-group">
                    <label>班主任</label>
                    <Input placeholder="姓名" value={mHeadTeacher} onChange={(e) => setMHeadTeacher(e.target.value)} />
                  </div>
                  <div className="kb-form-group">
                    <label>开班状态</label>
                    <Select value={mStatus} onChange={setMStatus} style={{ width: '100%' }}
                      options={['待申请', '材料准备中', '已提交人社', '审批中', '已批准', '正式开班'].map((s) => ({ value: s, label: s }))} />
                  </div>
                </div>
                <div className="kb-form-group-full" style={{ marginTop: 16 }}>
                  <label>开班材料清单</label>
                  <div className="kb-material-grid">
                    {MATERIAL_KEYS.map((mk) => (
                      <label key={mk.key} className={`kb-material-item${mMaterials[mk.key] ? ' done' : ''}`}>
                        <input type="checkbox" checked={mMaterials[mk.key] || false}
                          onChange={(e) => setMMaterials((prev) => ({ ...prev, [mk.key]: e.target.checked }))} />
                        {mk.label}
                      </label>
                    ))}
                  </div>
                </div>
                <div className="kb-check-row">
                  <input type="checkbox" checked={mCommitment} onChange={(e) => setMCommitment(e.target.checked)} />
                  <label>企业/机构已签署开班承诺书</label>
                </div>
                <div className="kb-check-row">
                  <input type="checkbox" checked={mCeremony} onChange={(e) => setMCeremony(e.target.checked)} />
                  <label>开班仪式已完成（5–10 分钟）</label>
                </div>
                <div className="kb-form-group-full" style={{ marginTop: 16 }}>
                  <label>备注</label>
                  <Input.TextArea placeholder="场地确认、企业参与情况等" value={mRemark} onChange={(e) => setMRemark(e.target.value)} rows={3} />
                </div>
              </div>
            </Modal>
          </>
        )}
      </main>
    </div>
  );
}
