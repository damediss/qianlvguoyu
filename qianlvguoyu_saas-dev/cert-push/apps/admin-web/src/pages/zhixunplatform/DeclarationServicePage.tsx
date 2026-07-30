import { useState, useMemo, useCallback, useEffect } from 'react';
import { Typography, Select, Input, Button, Table, InputNumber, message } from 'antd';
import {
  ArrowLeftOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  FileAddOutlined,
  FileZipOutlined,
  CheckCircleOutlined,
  CloudUploadOutlined,
  DollarOutlined,
  SafetyCertificateOutlined,
  NodeIndexOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './DeclarationServicePage.css';

// ============================================================
// 类型
// ============================================================
interface MaterialItem {
  key: string;
  name: string;
  stage: string;
  status: string;
  submitDate: string;
  ready: boolean;
}

interface ShenbaoBundle {
  projectId: number | null;
  policyTemplate: string;
  subsidyAmount: number;
  materials: MaterialItem[];
  submitStatus: string;
  submitDate: string;
  refundStatus: string;
  rejectReason: string;
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

const FLOW_LABELS = ['政策模板', '材料包', '人社提交', '拨款跟踪'];
const FLOW_ICONS = [FileTextOutlined, FolderOpenOutlined, CloudUploadOutlined, DollarOutlined];
const POLICY_OPTIONS = ['企业职工岗位技能培训补贴', '职业技能提升行动补贴', '鉴定考核补贴'];

const DEFAULT_MATERIALS: MaterialItem[] = [
  { key: 'plan', name: '培训计划备案表', stage: '项目立项', status: '草稿', submitDate: '', ready: false },
  { key: 'roster', name: '学员名册及考勤', stage: '培训执行', status: '草稿', submitDate: '', ready: false },
  { key: 'cert', name: '获证花名册', stage: '认定', status: '草稿', submitDate: '', ready: false },
  { key: 'subsidy', name: '补贴申报材料包', stage: '补贴申报', status: '草稿', submitDate: '', ready: false },
  { key: 'contract', name: '培训协议及承诺书', stage: '协议签订', status: '草稿', submitDate: '', ready: false },
  { key: 'evidence', name: '过程留痕证据链目录', stage: '培训执行', status: '草稿', submitDate: '', ready: false },
  { key: 'graduate', name: '结业证明申请', stage: '结业', status: '草稿', submitDate: '', ready: false },
];

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

function statusClass(st: string) {
  if (st === '已提交' || st === '已通过') return 'st-pass';
  if (st === '审核中' || st === '已齐') return 'st-submit';
  if (st === '待补充' || st === '已退回') return 'st-reject';
  if (st === '草稿') return 'st-draft';
  return 'st-ready';
}

// ============================================================
// Mock 数据
// ============================================================
function getMockCertifiedCount(projectId: number): number {
  return 8;
}

function initBundle(projectId: number): ShenbaoBundle {
  const certCount = getMockCertifiedCount(projectId);
  const materials = DEFAULT_MATERIALS.map((m) => ({ ...m }));
  if (certCount > 0) {
    const cert = materials.find((m) => m.key === 'cert');
    if (cert) { cert.ready = true; cert.status = '已齐'; }
  }
  return {
    projectId,
    policyTemplate: POLICY_OPTIONS[0],
    subsidyAmount: (certCount || 40) * 1200,
    materials,
    submitStatus: '草稿',
    submitDate: '',
    refundStatus: '未拨款',
    rejectReason: '',
  };
}

function flowStep(bundle: ShenbaoBundle) {
  if (bundle.submitStatus === '已通过' || bundle.refundStatus === '已拨款') return 4;
  if (bundle.submitStatus === '审核中' || bundle.submitStatus === '已提交') return 3;
  const ready = bundle.materials.filter((m) => m.ready).length;
  if (ready === bundle.materials.length) return 2;
  if (bundle.policyTemplate) return 1;
  return 0;
}

// ============================================================
// 主页面
// ============================================================
export function DeclarationServicePage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
  const [bundle, setBundle] = useState<ShenbaoBundle | null>(null);

  const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
  const certCount = selectedProjectId ? getMockCertifiedCount(+selectedProjectId) : 0;

  useEffect(() => {
    if (selectedProjectId) {
      setBundle(initBundle(+selectedProjectId));
    } else {
      setBundle(null);
    }
  }, [selectedProjectId]);

  const handleProjectChange = (v: string) => {
    setSelectedProjectId(v);
    setCurrentProjectId(v ? +v : null);
  };

  const updateBundle = useCallback((updater: (b: ShenbaoBundle) => void) => {
    setBundle((prev) => {
      if (!prev) return prev;
      const next = { ...prev, materials: prev.materials.map((m) => ({ ...m })) };
      updater(next);
      return next;
    });
  }, []);

  const stats = useMemo(() => {
    if (!bundle) return { ready: 0, total: 0, submitStatus: '草稿', certCount: 0, refund: '—' };
    const ready = bundle.materials.filter((m) => m.ready).length;
    return { ready, total: bundle.materials.length, submitStatus: bundle.submitStatus, certCount, refund: bundle.refundStatus };
  }, [bundle, certCount]);

  const step = bundle ? flowStep(bundle) : 0;

  const handleGenPackage = () => {
    updateBundle((b) => {
      b.materials.forEach((m) => { m.ready = true; m.status = m.status === '草稿' ? '已齐' : m.status; });
    });
    message.success('已根据政策模板生成本项目申报材料包');
  };

  const handleVerify = () => {
    if (!bundle) return;
    const notReady = bundle.materials.filter((m) => !m.ready);
    if (notReady.length) {
      message.warning(`尚有 ${notReady.length} 项材料未齐备：${notReady.map((m) => m.name).join('、')}`);
      return;
    }
    message.success('补贴申报材料核验清单已全部通过');
  };

  const handleSubmitRsb = () => {
    if (!bundle) return;
    const notReady = bundle.materials.filter((m) => !m.ready);
    if (notReady.length) { message.warning('请先齐备全部材料后再提交'); return; }
    const today = todayStr();
    updateBundle((b) => {
      b.submitStatus = '审核中';
      b.submitDate = today;
      b.materials.forEach((m) => { if (m.ready) { m.status = '已提交'; m.submitDate = today; } });
    });
    message.success('培训及补贴材料已提交人社系统，进入审核中');
  };

  const handleTrackRefund = () => {
    if (!bundle) return;
    if (bundle.submitStatus !== '审核中' && bundle.submitStatus !== '已通过') {
      message.warning('请先完成人社材料提交');
      return;
    }
    if (bundle.submitStatus === '审核中') {
      updateBundle((b) => { b.submitStatus = '已通过'; b.refundStatus = '拨款中'; });
    } else if (bundle.refundStatus === '拨款中') {
      updateBundle((b) => { b.refundStatus = '已拨款'; });
      message.success('拨款已到账');
    } else {
      message.info('当前拨款状态：' + bundle.refundStatus);
    }
  };

  const handleToggleReady = (key: string) => {
    updateBundle((b) => {
      const m = b.materials.find((x) => x.key === key);
      if (m) { m.ready = !m.ready; m.status = m.ready ? '已齐' : '草稿'; }
    });
  };

  const materialColumns: ColumnsType<MaterialItem> = [
    { title: '材料名称', dataIndex: 'name', render: (v) => <strong>{v}</strong> },
    { title: '所属环节', dataIndex: 'stage' },
    { title: '状态', dataIndex: 'status', render: (v) => <span className={`status-badge ${statusClass(v)}`}>{v}</span> },
    { title: '提交日期', dataIndex: 'submitDate', render: (v) => v || '—' },
    {
      title: '操作', key: 'actions',
      render: (_, r) => (
        <Button size="small" onClick={() => handleToggleReady(r.key)}>
          {r.ready ? '标记草稿' : '标记已齐'}
        </Button>
      ),
    },
  ];

  return (
    <div className="sb-page">
      <header className="sb-topbar">
        <div className="sb-topbar-left">
          <a className="sb-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="sb-title">申报服务</h1>
            <span className="sb-sub">{currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 7 / 9` : '补贴申报服务 · 步骤 7 / 9'}</span>
          </div>
        </div>
        <span className="sb-process-badge"><FileAddOutlined /> … → 申报服务 → 培训结业 → …</span>
      </header>

      <main className="sb-page-body">
        {/* 项目选择 */}
        <div className="sb-card">
          <div className="sb-card-head">
            <div className="sb-card-head-title"><FolderOpenOutlined /> 培训项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="sb-form-group">
            <label>选择企业培训项目</label>
            <Select placeholder="请选择项目…" value={selectedProjectId || undefined} onChange={handleProjectChange} allowClear style={{ width: '100%' }}
              options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))} />
          </div>
          {currentProject && bundle && (
            <div className="sb-project-banner">
              <div className="sb-banner-item"><label>企业</label><strong>{currentProject.enterprise || '—'}</strong></div>
              <div className="sb-banner-item"><label>培训项目</label><strong>{currentProject.name || '—'}</strong></div>
              <div className="sb-banner-item"><label>政策模板</label><strong>{bundle.policyTemplate || '—'}</strong></div>
              <div className="sb-banner-item"><label>获证人数</label><strong>{certCount} 人</strong></div>
              <div className="sb-banner-item"><label>申报状态</label><strong>{bundle.submitStatus}</strong></div>
            </div>
          )}
        </div>

        {!currentProject ? (
          <div className="sb-empty">
            <InfoCircleOutlined style={{ fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' }} />
            <p>请先选择<strong>企业培训项目</strong>，再准备并提交培训/补贴申报材料。</p>
            <p style={{ marginTop: 8, fontSize: 13 }}>建议先完成 认定 获证花名册；也可从任务档案进入。</p>
          </div>
        ) : bundle && (
          <>
            {/* 统计卡片 */}
            <div className="sb-stat-row">
              <div className="sb-stat-card"><div className="sb-stat-val">{stats.ready}/{stats.total}</div><div className="sb-stat-lbl">材料齐备</div></div>
              <div className="sb-stat-card"><div className={`sb-stat-val${stats.submitStatus === '草稿' ? ' warn' : ' success'}`}>{stats.submitStatus}</div><div className="sb-stat-lbl">申报状态</div></div>
              <div className="sb-stat-card"><div className="sb-stat-val success">{stats.certCount}</div><div className="sb-stat-lbl">获证人数</div></div>
              <div className="sb-stat-card"><div className="sb-stat-val">{stats.refund}</div><div className="sb-stat-lbl">拨款状态</div></div>
            </div>

            {/* 申报流程 */}
            <div className="sb-card">
              <div className="sb-card-head">
                <div className="sb-card-head-title"><NodeIndexOutlined /> 申报流程</div>
                <span className="sb-hint">政策模板 → 生成材料包 → 提交人社 → 拨款跟踪</span>
              </div>
              <div className="sb-flow-steps">
                {FLOW_LABELS.map((label, i) => {
                  const Icon = FLOW_ICONS[i];
                  let cls = 'sb-flow-step';
                  if (i < step) cls += ' done';
                  else if (i === step) cls += ' active';
                  return (
                    <div key={i} className={cls}>
                      <Icon style={{ display: 'block', fontSize: 18, marginBottom: 6 }} />
                      {label}
                    </div>
                  );
                })}
              </div>
              <div className="sb-form-grid">
                <div className="sb-form-group">
                  <label>补贴政策模板</label>
                  <Select value={bundle.policyTemplate} style={{ width: '100%' }}
                    onChange={(v) => updateBundle((b) => { b.policyTemplate = v; })}
                    options={POLICY_OPTIONS.map((p) => ({ value: p, label: p }))} />
                </div>
                <div className="sb-form-group">
                  <label>预计补贴金额（元）</label>
                  <InputNumber min={0} placeholder="根据政策测算" value={bundle.subsidyAmount} style={{ width: '100%' }}
                    onChange={(v) => updateBundle((b) => { b.subsidyAmount = v || 0; })} />
                </div>
              </div>
              <div className="sb-toolbar">
                <Button type="primary" icon={<FileZipOutlined />} onClick={handleGenPackage}>生成申报材料包</Button>
                <Button icon={<CheckCircleOutlined />} onClick={handleVerify}>材料核验清单</Button>
                <Button icon={<CloudUploadOutlined />} onClick={handleSubmitRsb}>提交人社系统</Button>
                <Button style={{ background: '#10B981', color: '#fff', borderColor: '#10B981' }} icon={<DollarOutlined />} onClick={handleTrackRefund}>拨款进度登记</Button>
              </div>
              {bundle.submitStatus === '已退回' && bundle.rejectReason && (
                <div className="sb-reject-box">
                  <ExclamationCircleOutlined /> <strong>退回原因：</strong>{bundle.rejectReason}
                </div>
              )}
              <Table dataSource={bundle.materials} columns={materialColumns} rowKey="key" pagination={false}
                locale={{ emptyText: '暂无材料' }} />
            </div>

            {/* 底部操作 */}
            <div className="sb-footer-actions">
              <Button icon={<SafetyCertificateOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-certification'); }}>返回认定</Button>
              <Button onClick={() => navigateTo('zhixun-training-process')}>保存并返回流程</Button>
              <Button type="primary" style={{ background: '#10B981', borderColor: '#10B981' }} icon={<ArrowLeftOutlined style={{ transform: 'rotate(180deg)' }} />}
                onClick={() => {
                  if (!currentProject) { message.warning('请先选择项目'); return; }
                  setCurrentProjectId(+selectedProjectId);
                  navigateTo('zhixun-graduation');
                }}>
                完成申报服务，进入培训结业
              </Button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
