import { useState, useMemo, useCallback, useEffect } from 'react';
import { Typography, Select, InputNumber, Input, Button, Table, Tabs, message } from 'antd';
import {
  ArrowLeftOutlined,
  DollarOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  NodeIndexOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  AuditOutlined,
  TrophyOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
  CalendarOutlined,
  PayCircleOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './RefundPage.css';

// ============================================================
// 类型
// ============================================================
interface PaymentPlan {
  phase: string;
  dueDate: string;
  amount: number;
  status: string;
}

interface Payment {
  id: number;
  date: string;
  amount: number;
  phase: string;
  method: string;
  remark: string;
}

interface Settlement {
  generated: boolean;
  orgAmount: number;
  enterpriseAmount: number;
  subsidyAmount: number;
  approvalStatus: string;
  approvalStep: number;
}

interface HuikuanData {
  projectId: number | null;
  contractAmount: number;
  planGenerated: boolean;
  paymentPlan: PaymentPlan[];
  payments: Payment[];
  orgSharePct: number;
  settlement: Settlement;
  projectSummary: string;
  projectClosed: boolean;
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

const FLOW_LABELS = ['回款计划', '收款登记', '项目结算', '审批结案'];
const APPROVAL_NODES = ['培训部提交', '财务复核', '总经理审批', '企业确认'];

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

function formatWan(yuan: number) {
  return (yuan / 10000).toFixed(2);
}

function agingLabel(dueDate: string) {
  const due = new Date(dueDate);
  const today = new Date(todayStr());
  const days = Math.floor((today.getTime() - due.getTime()) / 86400000);
  if (days < 0) return `剩余 ${-days} 天`;
  if (days === 0) return '今日到期';
  if (days <= 30) return `${days} 天`;
  if (days <= 90) return '31-90天';
  return '90天以上';
}

function planStatusClass(st: string) {
  if (st === '已回款') return 'st-done';
  if (st === '逾期') return 'st-overdue';
  if (st === '部分回款') return 'st-partial';
  return 'st-pending';
}

// ============================================================
// Mock 数据
// ============================================================
function getMockSubsidy(projectId: number) {
  return { refundStatus: '拨款中', subsidyAmount: 48000 };
}

function defaultContractAmount(planCount: number) {
  return (planCount || 40) * 45000;
}

function initData(projectId: number, planCount: number): HuikuanData {
  const contract = defaultContractAmount(planCount);
  const shenbao = getMockSubsidy(projectId);
  return {
    projectId,
    contractAmount: contract,
    planGenerated: false,
    paymentPlan: [],
    payments: [],
    orgSharePct: 70,
    settlement: {
      generated: false,
      orgAmount: 0,
      enterpriseAmount: 0,
      subsidyAmount: shenbao.subsidyAmount || 0,
      approvalStatus: '草稿',
      approvalStep: 0,
    },
    projectSummary: '',
    projectClosed: false,
  };
}

// ============================================================
// 主页面
// ============================================================
export function RefundPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [selectedProjectId, setSelectedProjectId] = useState(() => String(currentProjectId || ''));
  const [data, setData] = useState<HuikuanData | null>(null);
  const [activeTab, setActiveTab] = useState('plan');

  const currentProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;

  useEffect(() => {
    if (selectedProjectId) {
      setData(initData(+selectedProjectId, currentProject?.planCount || 40));
    } else {
      setData(null);
    }
  }, [selectedProjectId]);

  const handleProjectChange = (v: string) => {
    setSelectedProjectId(v);
    setCurrentProjectId(v ? +v : null);
    setActiveTab('plan');
  };

  const shenbao = useMemo(() => getMockSubsidy(selectedProjectId ? +selectedProjectId : 0), [selectedProjectId]);

  const totalReceived = useCallback(() => {
    if (!data) return 0;
    return data.payments.reduce((s, p) => s + (p.amount || 0), 0);
  }, [data]);

  const syncPlanStatus = useCallback((d: HuikuanData) => {
    if (!d.paymentPlan.length) return;
    const received = d.payments.reduce((s, p) => s + (p.amount || 0), 0);
    let allocated = 0;
    d.paymentPlan.forEach((plan) => {
      const planAmt = plan.amount || 0;
      if (allocated + planAmt <= received) {
        plan.status = '已回款';
        allocated += planAmt;
      } else if (allocated < received) {
        plan.status = '部分回款';
        allocated = received;
      } else {
        const due = new Date(plan.dueDate);
        const today = new Date(todayStr());
        plan.status = due < today ? '逾期' : '待回款';
      }
    });
  }, []);

  const updateData = useCallback((updater: (d: HuikuanData) => void) => {
    setData((prev) => {
      if (!prev) return prev;
      const next = { ...prev, paymentPlan: prev.paymentPlan.map((p) => ({ ...p })), payments: prev.payments.map((p) => ({ ...p })), settlement: { ...prev.settlement } };
      updater(next);
      syncPlanStatus(next);
      return next;
    });
  }, [syncPlanStatus]);

  const stats = useMemo(() => {
    if (!data) return { contract: 0, received: 0, pending: 0, rate: 0 };
    const received = totalReceived();
    const contract = data.contractAmount || 0;
    const pending = Math.max(0, contract - received);
    const rate = contract ? Math.round((received / contract) * 100) : 0;
    return { contract, received, pending, rate };
  }, [data, totalReceived]);

  const flowStep = useMemo(() => {
    if (!data) return 0;
    if (data.projectClosed || data.settlement?.approvalStatus === '已结案') return 4;
    if (data.settlement?.approvalStatus === '审批中' || data.settlement?.approvalStatus === '已通过') return 3;
    if (data.settlement?.generated) return 2;
    if (data.payments?.length || data.planGenerated) return 1;
    return 0;
  }, [data]);

  const overduePlans = useMemo(() => (data?.paymentPlan || []).filter((p) => p.status === '逾期'), [data]);

  const handleGenPlan = () => {
    if (!data) return;
    const contract = data.contractAmount;
    if (!contract) { message.warning('请填写培训合同金额'); return; }
    const p1 = Math.round(contract * 0.4);
    const p2 = Math.round(contract * 0.4);
    const p3 = contract - p1 - p2;
    const base = new Date();
    const addDays = (d: number) => {
      const x = new Date(base);
      x.setDate(x.getDate() + d);
      return x.toISOString().slice(0, 10);
    };
    updateData((d) => {
      d.contractAmount = contract;
      d.paymentPlan = [
        { phase: '首期（40%）', dueDate: addDays(0), amount: p1, status: '待回款' },
        { phase: '中期（40%）', dueDate: addDays(60), amount: p2, status: '待回款' },
        { phase: '尾款（20%）', dueDate: addDays(120), amount: p3, status: '待回款' },
      ];
      d.planGenerated = true;
    });
    message.success('已生成三期回款计划');
  };

  const handleRegisterPay = () => {
    if (!data || !data.planGenerated) { message.warning('请先生成回款计划'); return; }
    const pending = data.paymentPlan.find((p) => p.status !== '已回款');
    const phase = pending?.phase || '补充收款';
    const amount = pending?.amount || 0;
    updateData((d) => {
      d.payments.push({
        id: Date.now(),
        date: todayStr(),
        amount,
        phase,
        method: '银行转账',
        remark: '企业付款',
      });
    });
    message.success(`已登记收款 ${amount.toLocaleString()} 元`);
  };

  const handleGenSettlement = () => {
    if (!data) return;
    const contract = data.contractAmount;
    if (!contract) { message.warning('请填写合同金额'); return; }
    const received = data.payments.reduce((s, p) => s + (p.amount || 0), 0);
    const subsidy = shenbao?.subsidyAmount || 0;
    const totalSettle = received + (shenbao?.refundStatus === '已拨款' ? subsidy : 0);
    const orgPct = data.orgSharePct;
    updateData((d) => {
      d.orgSharePct = orgPct;
      d.settlement = {
        generated: true,
        orgAmount: Math.round(totalSettle * orgPct / 100),
        enterpriseAmount: Math.round(totalSettle * (100 - orgPct) / 100),
        subsidyAmount: subsidy,
        approvalStatus: '草稿',
        approvalStep: 0,
      };
    });
    message.success('项目结算单已生成');
  };

  const handleSubmitApproval = () => {
    if (!data || !data.settlement?.generated) { message.warning('请先生成结算单'); return; }
    updateData((d) => {
      const s = d.settlement;
      if (s.approvalStatus === '草稿') {
        s.approvalStatus = '审批中';
        s.approvalStep = 1;
        message.success('结算单已提交，进入财务复核');
      } else if (s.approvalStatus === '审批中') {
        if (s.approvalStep < APPROVAL_NODES.length) {
          s.approvalStep++;
          if (s.approvalStep >= APPROVAL_NODES.length) {
            s.approvalStatus = '已通过';
            message.success('结算审批已全部通过');
          } else {
            message.success(`当前节点：${APPROVAL_NODES[s.approvalStep]}`);
          }
        }
      } else if (s.approvalStatus === '已通过') {
        s.approvalStatus = '已结案';
        d.projectClosed = true;
        message.success('项目已结案，全流程完成');
      }
    });
  };

  const planColumns: ColumnsType<PaymentPlan> = [
    { title: '期次', dataIndex: 'phase', render: (v) => <strong>{v}</strong> },
    { title: '计划回款日', dataIndex: 'dueDate' },
    { title: '计划金额（元）', dataIndex: 'amount', render: (v) => (v || 0).toLocaleString() },
    { title: '账龄', dataIndex: 'dueDate', render: (v) => agingLabel(v) },
    { title: '状态', dataIndex: 'status', render: (v) => <span className={`status-badge ${planStatusClass(v)}`}>{v}</span> },
  ];

  const paymentColumns: ColumnsType<Payment> = [
    { title: '收款日期', dataIndex: 'date' },
    { title: '金额（元）', dataIndex: 'amount', render: (v) => (v || 0).toLocaleString() },
    { title: '对应期次', dataIndex: 'phase' },
    { title: '方式', dataIndex: 'method' },
    { title: '备注', dataIndex: 'remark', render: (v) => v || '—' },
  ];

  const tabItems = [
    {
      key: 'plan', label: '回款计划',
      children: (
        <Table dataSource={data?.paymentPlan || []} columns={planColumns} rowKey="phase" pagination={false}
          locale={{ emptyText: '请点击「生成回款计划」' }} />
      ),
    },
    {
      key: 'payment', label: '收款明细',
      children: (
        <Table dataSource={data?.payments || []} columns={paymentColumns} rowKey="id" pagination={false}
          locale={{ emptyText: '暂无收款记录' }} />
      ),
    },
    {
      key: 'settlement', label: '项目结算',
      children: (
        <div>
          {!data?.settlement?.generated ? (
            <div className="hk-settlement-empty">请先生成项目结算单</div>
          ) : (
            <div>
              <div className="hk-settlement-box">
                <div className="hk-settlement-row"><span>培训合同总额</span><span>{data.contractAmount.toLocaleString()} 元</span></div>
                <div className="hk-settlement-row"><span>累计已回款</span><span>{totalReceived().toLocaleString()} 元</span></div>
                <div className="hk-settlement-row"><span>补贴到账（预计）</span><span>{(data.settlement.subsidyAmount || 0).toLocaleString()} 元</span></div>
                <div className="hk-settlement-row"><span>机构分成（{data.orgSharePct}%）</span><span>{data.settlement.orgAmount.toLocaleString()} 元</span></div>
                <div className="hk-settlement-row"><span>企业结算份额</span><span>{data.settlement.enterpriseAmount.toLocaleString()} 元</span></div>
                <div className="hk-settlement-row hk-settlement-total"><span>结算单状态</span><span>{data.settlement.approvalStatus}</span></div>
              </div>
              <div className="hk-approval-track">
                {APPROVAL_NODES.map((name, i) => {
                  const s = data.settlement;
                  let cls = 'hk-approval-node';
                  if (i < s.approvalStep) cls += ' done';
                  else if (i === s.approvalStep && s.approvalStatus === '审批中') cls += ' active';
                  else if (s.approvalStatus === '已结案' || s.approvalStatus === '已通过') cls += ' done';
                  return <div key={i} className={cls}>{name}</div>;
                })}
              </div>
              <div className="hk-form-group" style={{ marginTop: 20 }}>
                <label>项目总结摘要</label>
                <Input.TextArea rows={3} placeholder="填写项目结算说明、经验沉淀要点（可选）"
                  value={data.projectSummary}
                  onChange={(e) => updateData((d) => { d.projectSummary = e.target.value; })} />
              </div>
            </div>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="hk-page">
      <header className="hk-topbar">
        <div className="hk-topbar-left">
          <a className="hk-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="hk-title">项目回款</h1>
            <span className="hk-sub">{currentProject ? `${currentProject.enterprise} · ${currentProject.name} · 步骤 9 / 9` : '合同回款与项目结算 · 步骤 9 / 9'}</span>
          </div>
        </div>
        <span className="hk-process-badge"><DollarOutlined /> … → 结业 → 回款 ✓</span>
      </header>

      <main className="hk-page-body">
        {/* 项目选择 */}
        <div className="hk-card">
          <div className="hk-card-head">
            <div className="hk-card-head-title"><FolderOpenOutlined /> 培训项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="hk-form-group">
            <label>选择企业培训项目</label>
            <Select placeholder="请选择项目…" value={selectedProjectId || undefined} onChange={handleProjectChange} allowClear style={{ width: '100%' }}
              options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))} />
          </div>
          {currentProject && data && (
            <div className="hk-project-banner">
              <div className="hk-banner-item"><label>企业</label><strong>{currentProject.enterprise || '—'}</strong></div>
              <div className="hk-banner-item"><label>培训项目</label><strong>{currentProject.name || '—'}</strong></div>
              <div className="hk-banner-item"><label>合同金额</label><strong>{formatWan(data.contractAmount)} 万</strong></div>
              <div className="hk-banner-item"><label>已回款</label><strong>{formatWan(totalReceived())} 万</strong></div>
              <div className="hk-banner-item"><label>结算状态</label><strong>{data.settlement?.approvalStatus || '草稿'}</strong></div>
            </div>
          )}
        </div>

        {!currentProject ? (
          <div className="hk-empty">
            <InfoCircleOutlined style={{ fontSize: 40, color: '#E2E8F0', marginBottom: 16, display: 'block' }} />
            <p>请先选择<strong>企业培训项目</strong>，再管理回款计划、收款登记与项目结算。</p>
            <p style={{ marginTop: 8, fontSize: 13 }}>建议先完成 结业 与 申报；也可从任务档案进入。</p>
          </div>
        ) : data && (
          <>
            {overduePlans.length > 0 && (
              <div className="hk-alert warn">
                <ExclamationCircleOutlined />
                <span>有 {overduePlans.length} 期回款计划已逾期，请跟进企业付款或更新收款登记。</span>
              </div>
            )}

            {/* 统计卡片 */}
            <div className="hk-stat-row">
              <div className="hk-stat-card"><div className="hk-stat-val">{formatWan(stats.contract)}</div><div className="hk-stat-lbl">合同金额（万）</div></div>
              <div className="hk-stat-card"><div className="hk-stat-val success">{formatWan(stats.received)}</div><div className="hk-stat-lbl">已回款（万）</div></div>
              <div className="hk-stat-card"><div className="hk-stat-val warn">{formatWan(stats.pending)}</div><div className="hk-stat-lbl">待回款（万）</div></div>
              <div className="hk-stat-card"><div className={`hk-stat-val${stats.rate >= 100 ? ' success' : stats.rate < 50 ? ' warn' : ''}`}>{stats.rate}%</div><div className="hk-stat-lbl">回款率</div></div>
            </div>

            {/* 回款与结算流程 */}
            <div className="hk-card">
              <div className="hk-card-head">
                <div className="hk-card-head-title"><NodeIndexOutlined /> 回款与结算流程</div>
                <span className="hk-hint">回款计划 → 收款登记 → 项目结算 → 审批结案</span>
              </div>
              <div className="hk-flow-steps">
                {FLOW_LABELS.map((label, i) => {
                  const flowIcons = [CalendarOutlined, PayCircleOutlined, FileTextOutlined, AuditOutlined];
                  const Icon = flowIcons[i];
                  let cls = 'hk-flow-step';
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
              <div className="hk-form-grid">
                <div className="hk-form-group">
                  <label>培训合同金额（元）</label>
                  <InputNumber min={0} placeholder="企业培训服务合同总额" value={data.contractAmount} style={{ width: '100%' }}
                    onChange={(v) => updateData((d) => { d.contractAmount = v || 0; })} />
                </div>
                <div className="hk-form-group">
                  <label>补贴拨款状态（来自申报）</label>
                  <Input value={shenbao?.refundStatus || '未申报'} readOnly />
                </div>
                <div className="hk-form-group">
                  <label>预计补贴金额（元）</label>
                  <InputNumber value={shenbao?.subsidyAmount || data.settlement?.subsidyAmount || 0} readOnly style={{ width: '100%' }} />
                </div>
                <div className="hk-form-group">
                  <label>机构分成比例（%）</label>
                  <InputNumber min={0} max={100} value={data.orgSharePct} style={{ width: '100%' }}
                    onChange={(v) => updateData((d) => { d.orgSharePct = v || 70; })} />
                </div>
              </div>
              <div className="hk-toolbar">
                <Button type="primary" icon={<CalendarOutlined />} onClick={handleGenPlan}>生成回款计划</Button>
                <Button icon={<PayCircleOutlined />} onClick={handleRegisterPay}>登记收款</Button>
                <Button icon={<FileTextOutlined />} onClick={handleGenSettlement}>生成结算单</Button>
                <Button style={{ background: '#10B981', color: '#fff', borderColor: '#10B981' }} icon={<AuditOutlined />} onClick={handleSubmitApproval}>提交结算审批</Button>
              </div>

              <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
            </div>

            {/* 底部操作 */}
            <div className="hk-footer-actions">
              <Button icon={<TrophyOutlined />} onClick={() => { setCurrentProjectId(selectedProjectId ? +selectedProjectId : null); navigateTo('zhixun-graduation'); }}>返回结业</Button>
              <Button onClick={() => navigateTo('zhixun-training-process')}>保存并返回流程</Button>
              <Button type="primary" style={{ background: '#10B981', borderColor: '#10B981' }} icon={<CheckCircleOutlined />}
                onClick={() => {
                  if (!currentProject) { message.warning('请先选择项目'); return; }
                  navigateTo('zhixun-training-process');
                }}>
                完成全流程
              </Button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
