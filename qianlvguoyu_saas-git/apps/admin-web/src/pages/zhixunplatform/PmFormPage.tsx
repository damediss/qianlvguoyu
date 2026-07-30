import { useState, useMemo, Fragment } from 'react';
import { Button, Input, message } from 'antd';
import {
  ArrowLeftOutlined, PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  AimOutlined, BarChartOutlined, UserSwitchOutlined, FileTextOutlined, FlagOutlined,
  WarningOutlined, BugOutlined, RetweetOutlined, MessageOutlined, BulbOutlined,
  ApartmentOutlined, SendOutlined, ProfileOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './ArchivePages.css';

/* ============================================================
   基础字段
   ============================================================ */
const PM_BASE_FIELDS: { key: string; label: string; type?: string }[] = [
  { key: 'projectName', label: '项目名称' },
  { key: 'projectNo', label: '项目编号' },
  { key: 'preparedBy', label: '制作人' },
  { key: 'reviewedBy', label: '审核人' },
  { key: 'projectManager', label: '项目经理' },
  { key: 'prepareDate', label: '制作日期', type: 'date' },
];

function uid(prefix: string) {
  return prefix + '-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
}

function emptyDoc(type: string): any {
  const d: any = { id: uid(type) };
  PM_BASE_FIELDS.forEach(f => { d[f.key] = ''; });
  if (type === 'meeting') {
    Object.assign(d, { meetingName: '', convener: '', meetingDate: '', startTime: '', location: '', duration: '', recorder: '', reviewer: '' });
  }
  const subKeys = ['stakeholders', 'teamMembers', 'risks', 'history', 'plans', 'taskStatus', 'wbsRows', 'tasks', 'rows'];
  subKeys.forEach(k => { d[k] = []; });
  return d;
}

/* ============================================================
   子表列定义（对照 archive-pm-forms.js SUB_COLS / GENERIC_TABLE_COLS）
   ============================================================ */
const SUB_COLS: Record<string, { key: string; label: string }[]> = {
  teamMembers: [
    { key: 'name', label: '成员姓名' }, { key: 'role', label: '项目角色' }, { key: 'dept', label: '所在部门' },
    { key: 'duty', label: '职责' }, { key: 'dateRange', label: '项目起止日期' }, { key: 'workload', label: '投入频度及工作量' },
    { key: 'phone', label: '联系电话' }, { key: 'supervisor', label: '主管经理' },
  ],
  stakeholders: [
    { key: 'name', label: '姓名' }, { key: 'category', label: '类别' }, { key: 'dept', label: '部门' }, { key: 'position', label: '职务' },
  ],
  risks: [
    { key: 'seq', label: '序号' }, { key: 'desc', label: '风险描述' }, { key: 'probability', label: '发生概率' },
    { key: 'impact', label: '影响程度' }, { key: 'level', label: '风险等级' }, { key: 'responsePlan', label: '风险响应计划' },
    { key: 'owner', label: '责任人' }, { key: 'status', label: '开放/关闭' },
  ],
  history: [
    { key: 'seq', label: '序号' }, { key: 'changeTime', label: '变更时间' }, { key: 'tasks', label: '涉及项目任务' },
    { key: 'points', label: '变更要点' }, { key: 'reason', label: '变更理由' }, { key: 'applicant', label: '申请人' }, { key: 'approver', label: '审批人' },
  ],
  plans: [
    { key: 'stakeholder', label: '利益干系人' }, { key: 'info', label: '所需信息' }, { key: 'frequency', label: '频率' },
    { key: 'method', label: '方法' }, { key: 'owner', label: '责任人' },
  ],
  taskStatus: [
    { key: 'keyTask', label: '关键任务' }, { key: 'indicator', label: '状态指标' }, { key: 'desc', label: '状态描述' },
  ],
  wbsRows: [
    { key: 'code', label: '分解代码' }, { key: 'taskName', label: '任务名称' }, { key: 'activity', label: '包含活动' },
    { key: 'hours', label: '工时估算' }, { key: 'hr', label: '人力资源' }, { key: 'otherRes', label: '其他资源' },
    { key: 'cost', label: '费用估算' }, { key: 'duration', label: '工期' },
  ],
  tasks: [
    { key: 'phase', label: '任务/阶段' }, { key: 'code', label: '编号' }, { key: 'owner', label: '责任人' }, { key: 'milestone', label: '关键里程碑' },
  ],
};

const GENERIC_TABLE_COLS: Record<string, { key: string; label: string }[]> = {
  strategic: [
    { key: 'goal', label: '战略目标' }, { key: 'kpi', label: '关键指标' }, { key: 'owner', label: '责任人' }, { key: 'deadline', label: '完成时限' },
  ],
  orgPerf: [
    { key: 'dept', label: '部门' }, { key: 'target', label: '目标' }, { key: 'actual', label: '实际' }, { key: 'gap', label: '差距说明' },
  ],
  pbc: [
    { key: 'item', label: '承诺事项' }, { key: 'weight', label: '权重' }, { key: 'status', label: '状态' },
  ],
  issue: [
    { key: 'title', label: '问题描述' }, { key: 'severity', label: '严重程度' }, { key: 'owner', label: '责任人' },
    { key: 'status', label: '状态' }, { key: 'due', label: '计划关闭日' },
  ],
};

/* ============================================================
   页面元数据
   ============================================================ */
const PAGE_META: Record<string, { title: string; sub: string; tag: string; icon: React.ReactNode }> = {
  strategic: { title: '战略解码表', sub: '填写：战略目标、关键行动、衡量指标、责任部门、完成时限与进度（组织级OKR分解）', tag: '项目管控', icon: <AimOutlined /> },
  orgPerf: { title: '组织绩效表', sub: '填写：考核部门、指标、目标值、实际值、得分（部门年度/季度绩效）', tag: '项目管控', icon: <BarChartOutlined /> },
  pbc: { title: '个人绩效承诺（PBC）', sub: '填写：员工、岗位、个人目标、KR、权重、自评与上级评分', tag: '项目管控', icon: <UserSwitchOutlined /> },
  charter: { title: '项目立项 Charter', sub: '一、基本情况；二、项目描述；三、里程碑计划；四、评价标准；五、假定与约束；六、利益干系人；项目组成员', tag: '项目管控', icon: <FileTextOutlined /> },
  milestone: { title: '里程碑 / 项目进度表', sub: '项目基本情况 + 任务阶段、责任人、关键里程碑（对齐进度表模板）', tag: '项目管控', icon: <FlagOutlined /> },
  risk: { title: '风险管理表', sub: '风险概率准则 + 风险清单（描述、概率、影响、等级、响应计划、责任人、开放/关闭）', tag: '项目管控', icon: <WarningOutlined /> },
  issue: { title: '问题管理表', sub: '项目基本情况 + 问题登记清单', tag: '项目管控', icon: <BugOutlined /> },
  change: { title: '变更管理表', sub: '历史变更记录、变更描述、影响分析、审批结果（变更申请模板）', tag: '项目管控', icon: <RetweetOutlined /> },
  meeting: { title: '会议纪要 / 行动项', sub: '会议基本信息、目标、参会人、材料、发言记录、决议、发放范围', tag: '项目管控', icon: <MessageOutlined /> },
  knowledge: { title: '知识收割表', sub: '项目基本情况 + 经验发现 + 改进建议', tag: '项目管控', icon: <BulbOutlined /> },
  wbs: { title: '工作分解结构 WBS', sub: '分解代码、任务名称、活动、工时/人力/费用/工期', tag: '项目管控', icon: <ApartmentOutlined /> },
  communication: { title: '项目沟通计划', sub: '利益干系人、所需信息、频率、方法、责任人', tag: '项目管控', icon: <SendOutlined /> },
  status: { title: '项目状态报告', sub: '任务状态、本周期活动、下周期计划、财务、遗留问题、本期求助', tag: '项目管控', icon: <ProfileOutlined /> },
};

const HINTS: Record<string, string> = {
  charter: '按立项 Charter 模板：基本情况、项目描述、里程碑、评价标准、假定约束、利益干系人与项目组成员。',
  milestone: '按项目进度表：任务阶段、责任人、关键里程碑（周次甘特可在实施阶段扩展）。',
  risk: '含风险概率准则及风险清单（描述、概率、影响、等级、响应计划、责任人、开放/关闭）。',
  change: '含历史变更记录、变更描述、影响分析与审批结果。',
  meeting: '按会议纪要模板：基本信息、目标、参会人、材料、发言记录、决议与发放范围。',
  issue: '问题登记清单，可关联项目基本情况。',
  knowledge: '项目结束后的经验发现与改进建议。',
  wbs: '工作分解：分解代码、任务、活动、工时、人力、其他资源、费用、工期。',
  communication: '沟通计划：利益干系人、所需信息、频率、方法、责任人。',
  status: '状态报告：任务状态表、本周期活动、下周期计划、财务、遗留问题、本期求助。',
};

/* ============================================================
   初始数据
   ============================================================ */
const INIT_DOCS: Record<string, any[]> = {
  charter: [{
    id: 'chr-001', projectName: '电工技能等级认证培训项目', projectNo: 'PRJ-2026-001', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '王强', prepareDate: '2026-05-15',
    background: '为满足企业电工岗位持证上岗要求，解决一线员工技能等级偏低、实操考核通过率不足的问题，特立项开展电工技能等级认证培训。',
    goals: '质量目标：实操考核通过率≥85%；进度目标：2026年6月底前完成全部学员认证；成本目标：人均培训成本控制在预算内；交付物：培训方案、实操记录、考核成绩单、发证台账。',
    milestonePlan: 'M1（5.20）完成开班与摸底测评；M2（6.10）完成理论+实操集训；M3（6.25）组织等级认定考试；M4（6.30）归档发证与项目总结。',
    evaluationCriteria: '学员全部完成规定学时且考核合格；教学设备点检合格；资料档案完整可追溯；项目总结经审核人签字确认后验收。',
    assumptions: '假定实训场地按期开放、师资档期无冲突；约束：须符合安全生产规范，高温季节调整户外实操时段。',
    stakeholders: [
      { name: '李华', category: '高层管理', dept: '培训部', position: '部长' },
      { name: '赵敏', category: '客户代表', dept: '生产一部', position: 'HR对接人' },
      { name: '周工', category: '供应商', dept: '鉴定中心', position: '考务联络人' },
    ],
    teamMembers: [
      { name: '王强', role: '项目经理', dept: '培训部', duty: '统筹计划、资源协调、验收', dateRange: '2026-05-01 ~ 2026-06-30', workload: '全程 80%', phone: '138****1001', supervisor: '李华' },
      { name: '陈老师', role: '主讲讲师', dept: '培训中心', duty: '理论授课、实操指导', dateRange: '2026-05-15 ~ 2026-06-20', workload: '每周 3 天', phone: '139****2002', supervisor: '王强' },
    ],
  }],
  milestone: [{
    id: 'ms-001', projectName: '焊工晋升集训项目', projectNo: 'PRJ-2026-002', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '刘洋', prepareDate: '2026-05-15',
    scheduleNote: '按周跟踪关键任务，责任人落实到人。',
    tasks: [
      { phase: '开始', code: '', owner: '', milestone: '项目启动' },
      { phase: '开工会', code: '', owner: '刘洋', milestone: '' },
      { phase: '阶段I 摸底测评', code: '111', owner: '教务组', milestone: '' },
      { phase: '阶段II 理论集训', code: '211', owner: '陈老师', milestone: '完成80学时' },
      { phase: '阶段III 实操考核', code: '311', owner: '考评组', milestone: '晋升认定' },
    ],
  }],
  risk: [{
    id: 'risk-001', projectName: '起重机械操作工复训项目', projectNo: 'PRJ-2026-003', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '赵磊', prepareDate: '2026-05-15',
    risks: [
      { seq: 1, desc: '实训设备故障导致实操课延期', probability: '30%', impact: '高', level: '中风险', responsePlan: '提前一周完成设备点检，准备备用工位与机动讲师', owner: '设备管理员', status: '开放' },
      { seq: 2, desc: '学员出勤率不足影响考试批次', probability: '20%', impact: '中', level: '低风险', responsePlan: '与用人部门确认脱产安排，缺勤补训', owner: '班主任', status: '开放' },
    ],
  }],
  change: [{
    id: 'chg-001', projectName: '安全员继续教育培训', projectNo: 'PRJ-2026-004', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '孙婷', prepareDate: '2026-05-15',
    history: [{ seq: 1, changeTime: '2026-04-10', tasks: '开班时间', points: '推迟 3 天', reason: '鉴定中心考位调整', applicant: '孙婷', approver: '李华' }],
    requestDesc: '建议将第三期实操考核由 6/18 调整至 6/22，以便与鉴定中心统一排考。',
    impactBaseline: ['进度计划', '资源计划'],
    needAnalysis: '是', costImpact: '无额外费用，场地预约顺延', scheduleImpact: '整体进度延后 4 天，仍在合同期内',
    resourceImpact: '讲师档期需微调 1 次', changeLevel: '低', ifNotChangeImpact: '可能导致学员与考位冲突，影响发证时效',
    applicantSign: '孙婷', applyDate: '2026-05-12', approvalOpinion: '同意调整，请更新里程碑并通知学员',
    approverSign: '李华', approvalDate: '2026-05-13',
  }],
  meeting: [{
    id: 'mtg-001', meetingName: '电工认证项目周例会', convener: '王强', meetingDate: '2026-05-16', startTime: '14:00',
    location: '培训部会议室 A', duration: '1.5 小时', recorder: '小张', reviewer: '李华',
    projectName: '电工技能等级认证培训项目', projectNo: 'PRJ-2026-001',
    objectives: '通报本周教学进度、设备点检结果及下周考试安排',
    participants: '王强（PM）、陈老师（讲师）、设备管理员、教务 2 人',
    materials: '进度周报、设备点检表、学员出勤表',
    speechRecord: '设备组反馈 2 号实训台接触器需更换；讲师提出周末加练需求；PM 要求周五前完成补课名单确认。',
    resolution: '1）周五前完成设备维修；2）周六安排加练 4 小时；3）缺勤学员名单报生产部备案',
    reportTo: '培训部', mainTo: '项目组全员', copyTo: '生产一部 HR',
  }],
  status: [{
    id: 'st-001', projectName: '电工技能等级认证培训项目', projectNo: 'PRJ-2026-001', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '王强', prepareDate: '2026-05-15',
    currentStatus: '按计划进行', aheadFlag: '', reportPeriod: '2026-05-09 ~ 2026-05-16',
    taskStatus: [
      { keyTask: '理论模块授课', indicator: '绿', desc: '已完成 32/40 学时' },
      { keyTask: '实操模块', indicator: '黄', desc: '设备维修中，预计周五恢复' },
    ],
    periodActivities: '完成安全培训、摸底测评；提交设备点检报告；确认考试批次 48 人。',
    nextPlan: '完成剩余理论课；组织模拟考；启动发证资料收集。',
    finance: '已用预算 62%，无超支风险。',
    legacyIssues: '上期 3 名缺勤学员已安排补课。',
    issuesHelp: '需协调鉴定中心考位确认函。',
  }],
  communication: [{
    id: 'com-001', projectName: '焊工晋升集训项目', projectNo: 'PRJ-2026-002', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '刘洋', prepareDate: '2026-05-15',
    plans: [
      { stakeholder: '生产一部主管', info: '学员出勤与考核结果', frequency: '每周', method: '邮件+周报', owner: '刘洋' },
      { stakeholder: '学员', info: '课表、考场安排', frequency: '按需', method: '企业微信', owner: '班主任' },
    ],
  }],
  wbs: [{
    id: 'wbs-001', projectName: 'T 客户考察公司接待项目（示例）', projectNo: 'PRJ-DEMO-001', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '王强', prepareDate: '2026-05-15',
    wbsRows: [
      { code: '1', taskName: '邀请客户', activity: '提交邀请函给客户', hours: '4', hr: '市场部 2 人', otherRes: '—', cost: '500', duration: '3 天' },
      { code: '1.1', taskName: '', activity: '安排行程', hours: '8', hr: '行政', otherRes: '—', cost: '1200', duration: '5 天' },
      { code: '2', taskName: '落实资源', activity: '安排高层接待与座谈人员', hours: '16', hr: '多部门', otherRes: '会议室', cost: '3000', duration: '7 天' },
    ],
  }],
  strategic: [{
    id: 'stg-001', projectName: '2026 培训部战略解码', projectNo: 'STR-2026', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '李华', prepareDate: '2026-05-15',
    rows: [{ goal: '持证率提升', kpi: '关键岗位持证率≥95%', owner: '培训部', deadline: '2026-12' }],
  }],
  orgPerf: [{
    id: 'org-001', projectName: '培训部 Q2 组织绩效', projectNo: 'ORG-2026-Q2', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '李华', prepareDate: '2026-05-15',
    rows: [{ dept: '教务组', target: '开班准时率 100%', actual: '98%', gap: '2 个项目延期' }],
  }],
  pbc: [{
    id: 'pbc-001', projectName: '个人绩效承诺 — 王强', projectNo: 'PBC-2026-001', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '王强', prepareDate: '2026-05-15',
    rows: [{ item: '完成 3 个认证项目交付', weight: '40%', status: '进行中' }],
  }],
  issue: [{
    id: 'iss-001', projectName: '电工技能等级认证培训项目', projectNo: 'PRJ-2026-001', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '王强', prepareDate: '2026-05-15',
    rows: [{ title: '2 号实训台故障', severity: '中', owner: '设备组', status: '处理中', due: '2026-05-17' }],
  }],
  knowledge: [{
    id: 'kn-001', projectName: '电工认证项目知识收割', projectNo: 'KM-2026-001', preparedBy: '培训部-张明',
    reviewedBy: '部门负责人-李华', projectManager: '王强', prepareDate: '2026-05-15',
    findings: '摸底测评题库可复用；实操评分表需增加安全项检查点。',
    suggestions: '沉淀为标准作业指导书附件，纳入方法规则库。',
  }],
};

/* ============================================================
   辅助组件
   ============================================================ */

/** 基础信息网格 */
function BaseInfoGrid({ doc, onChange, readonly }: { doc: any; onChange?: (k: string, v: string) => void; readonly?: boolean }) {
  const rows: { key: string; label: string; type?: string }[][] = [];
  for (let i = 0; i < PM_BASE_FIELDS.length; i += 2) {
    rows.push([PM_BASE_FIELDS[i], PM_BASE_FIELDS[i + 1]]);
  }
  return (
    <div className="pa-pm-sheet-wrap">
      <div className="pa-pm-section-head">一、项目基本情况</div>
      <table className="pa-pm-sheet"><tbody>
        {rows.map((pair, ri) => (
          <tr key={ri}>
            {pair.map(f => f ? (
              <Fragment key={f.key}>
                <th className="pa-pm-label">{f.label}</th>
                <td>
                  {readonly
                    ? <span>{doc[f.key] || '—'}</span>
                    : <Input value={doc[f.key] || ''} onChange={e => onChange?.(f.key, e.target.value)} bordered={false} style={{ padding: 0 }} />}
                </td>
              </Fragment>
            ) : null)}
          </tr>
        ))}
      </tbody></table>
    </div>
  );
}

/** 文本区段 */
function TextSection({ title, hint, value, onChange, readonly, rows }: {
  title: string; hint?: string; value: string; onChange?: (v: string) => void; readonly?: boolean; rows?: number;
}) {
  return (
    <div className="pa-pm-sheet-wrap">
      <div className="pa-pm-section-head">{title}</div>
      {hint && <div className="pa-pm-hint">{hint}</div>}
      <table className="pa-pm-sheet"><tbody><tr><td>
        {readonly
          ? <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontFamily: 'inherit', fontSize: 13 }}>{value || '—'}</pre>
          : <Input.TextArea value={value || ''} onChange={e => onChange?.(e.target.value)} rows={rows || 4} bordered={false} style={{ padding: 0, resize: 'vertical' }} />}
      </td></tr></tbody></table>
    </div>
  );
}

/** 子表（动态行） */
function SubTable({ title, cols, rows, onChange, readonly }: {
  title: string; cols: { key: string; label: string }[]; rows: any[]; onChange?: (rows: any[]) => void; readonly?: boolean;
}) {
  const addRow = () => {
    const newRow: any = {};
    cols.forEach(c => { newRow[c.key] = ''; });
    onChange?.([...rows, newRow]);
  };
  const delRow = (idx: number) => {
    onChange?.(rows.filter((_, i) => i !== idx));
  };
  const updateCell = (idx: number, key: string, val: string) => {
    const next = rows.map((r, i) => i === idx ? { ...r, [key]: val } : r);
    onChange?.(next);
  };

  return (
    <div className="pa-pm-sheet-wrap">
      <div className="pa-pm-section-head">{title}</div>
      {!readonly && (
        <div className="pa-pm-sub-toolbar">
          <Button size="small" onClick={addRow}>+ 添加行</Button>
        </div>
      )}
      <table className="pa-pm-sheet">
        <thead>
          <tr>
            {cols.map(c => <th key={c.key} style={{ background: '#e8f0fe', fontWeight: 600, border: '1px solid #94a3b8', padding: '8px 10px', fontSize: 13 }}>{c.label}</th>)}
            {!readonly && <th style={{ width: 72, background: '#e8f0fe', border: '1px solid #94a3b8' }}>操作</th>}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && readonly ? (
            <tr><td colSpan={cols.length} style={{ textAlign: 'center', color: '#999', padding: 16 }}>暂无数据</td></tr>
          ) : (
            rows.map((row, idx) => (
              <tr key={idx}>
                {cols.map(c => (
                  <td key={c.key} style={{ border: '1px solid #94a3b8', padding: '8px 10px' }}>
                    {readonly
                      ? <span>{row[c.key] || '—'}</span>
                      : <Input value={row[c.key] || ''} onChange={e => updateCell(idx, c.key, e.target.value)} bordered={false} style={{ padding: 0 }} />}
                  </td>
                ))}
                {!readonly && (
                  <td style={{ border: '1px solid #94a3b8', padding: '8px 10px' }}>
                    <Button size="small" danger onClick={() => delRow(idx)}>删除</Button>
                  </td>
                )}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

/** 会议基础信息（独立于 BaseInfoGrid） */
function MeetingBasicInfo({ doc, onChange, readonly }: { doc: any; onChange?: (k: string, v: string) => void; readonly?: boolean }) {
  const pairs = [
    ['meetingName', '会议名称', 'convener', '召集人'],
    ['meetingDate', '会议日期', 'startTime', '开始时间'],
    ['location', '会议地点', 'duration', '持续时间'],
    ['recorder', '记录人', 'reviewer', '审核人'],
  ];
  const InputCell = ({ k }: { k: string }) => readonly
    ? <span>{doc[k] || '—'}</span>
    : <Input value={doc[k] || ''} onChange={e => onChange?.(k, e.target.value)} bordered={false} style={{ padding: 0 }} />;
  return (
    <div className="pa-pm-sheet-wrap">
      <div className="pa-pm-section-head">会议纪要</div>
      <table className="pa-pm-sheet"><tbody>
        {pairs.map(([k1, l1, k2, l2]) => (
          <tr key={k1}>
            <th className="pa-pm-label">{l1}</th><td><InputCell k={k1} /></td>
            <th className="pa-pm-label">{l2}</th><td><InputCell k={k2} /></td>
          </tr>
        ))}
        <tr>
          <th className="pa-pm-label">项目名称</th><td><InputCell k="projectName" /></td>
          <th className="pa-pm-label">项目编号</th><td><InputCell k="projectNo" /></td>
        </tr>
      </tbody></table>
    </div>
  );
}

/** 多选组（变更管理 — 受影响基准计划） */
function CheckGroup({ options, selected, onChange, readonly }: {
  options: string[]; selected: string[]; onChange?: (v: string[]) => void; readonly?: boolean;
}) {
  const toggle = (v: string) => {
    if (!onChange) return;
    if (selected.includes(v)) onChange(selected.filter(x => x !== v));
    else onChange([...selected, v]);
  };
  return (
    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
      {options.map(o => (
        <label key={o} style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 13, cursor: readonly ? 'default' : 'pointer' }}>
          <input type="checkbox" checked={selected.includes(o)} disabled={readonly} onChange={() => toggle(o)} /> {o}
        </label>
      ))}
    </div>
  );
}

/* ============================================================
   表单渲染（类型特定）
   ============================================================ */
function renderFormSections(type: string, doc: any, onChange: (k: string, v: any) => void, readonly: boolean) {
  switch (type) {
    case 'charter':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <TextSection title="二、项目描述 — 项目背景与目的" hint="所有项目都源于一定的业务问题，本节应简要描述这些问题。" value={doc.background} onChange={v => onChange('background', v)} readonly={readonly} />
        <TextSection title="项目目标" hint="包括质量目标、进度目标、成本目标及交付产品特性等说明。" value={doc.goals} onChange={v => onChange('goals', v)} readonly={readonly} />
        <TextSection title="三、项目里程碑计划" hint="包括里程碑时间点及成果。" value={doc.milestonePlan} onChange={v => onChange('milestonePlan', v)} readonly={readonly} />
        <TextSection title="四、评价标准" hint="说明何种情况下项目成果将被接受。" value={doc.evaluationCriteria} onChange={v => onChange('evaluationCriteria', v)} readonly={readonly} />
        <TextSection title="五、项目假定与约束条件" hint="说明项目主要假定及限制条件。" value={doc.assumptions} onChange={v => onChange('assumptions', v)} readonly={readonly} />
        <SubTable title="六、项目主要利益干系人" cols={SUB_COLS.stakeholders} rows={doc.stakeholders || []} onChange={v => onChange('stakeholders', v)} readonly={readonly} />
        <SubTable title="项目组成员" cols={SUB_COLS.teamMembers} rows={doc.teamMembers || []} onChange={v => onChange('teamMembers', v)} readonly={readonly} />
      </>;

    case 'wbs':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <div className="pa-pm-hint" style={{ border: '1px solid #94a3b8', marginBottom: 0 }}>（R-负责；As-辅助；I-通知；Ap-审批）</div>
        <SubTable title="二、工作分解结构" cols={SUB_COLS.wbsRows} rows={doc.wbsRows || []} onChange={v => onChange('wbsRows', v)} readonly={readonly} />
      </>;

    case 'milestone':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <TextSection title="二、项目进度说明" hint="按周跟踪任务，可在下方维护任务清单。" value={doc.scheduleNote} onChange={v => onChange('scheduleNote', v)} readonly={readonly} rows={2} />
        <SubTable title="项目进度表（任务 / 责任人 / 里程碑）" cols={SUB_COLS.tasks} rows={doc.tasks || []} onChange={v => onChange('tasks', v)} readonly={readonly} />
      </>;

    case 'risk':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <div className="pa-pm-sheet-wrap">
          <div className="pa-pm-section-head">二、项目风险管理</div>
          <div className="pa-pm-criteria"><strong>风险发生概率判断准则：</strong><br />高风险：发生概率 &gt;60%；中风险：30%–60%；低风险：&lt;30%。</div>
        </div>
        <SubTable title="风险清单" cols={SUB_COLS.risks} rows={doc.risks || []} onChange={v => onChange('risks', v)} readonly={readonly} />
      </>;

    case 'communication':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <SubTable title="二、项目沟通计划" cols={SUB_COLS.plans} rows={doc.plans || []} onChange={v => onChange('plans', v)} readonly={readonly} />
      </>;

    case 'change':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <SubTable title="二、历史变更记录" cols={SUB_COLS.history} rows={doc.history || []} onChange={v => onChange('history', v)} readonly={readonly} />
        <TextSection title="三、请求变更信息" hint="建议的变更描述及参考资料" value={doc.requestDesc} onChange={v => onChange('requestDesc', v)} readonly={readonly} rows={5} />
        <div className="pa-pm-sheet-wrap">
          <div className="pa-pm-section-head">四、影响分析</div>
          <table className="pa-pm-sheet"><tbody>
            <tr>
              <th className="pa-pm-label">受影响基准计划</th>
              <td colSpan={3}>
                <CheckGroup options={['进度计划', '费用计划', '资源计划']} selected={doc.impactBaseline || []} onChange={v => onChange('impactBaseline', v)} readonly={readonly} />
              </td>
            </tr>
            <tr>
              <th className="pa-pm-label">需成本/进度影响分析</th>
              <td>
                {readonly ? <span>{doc.needAnalysis || '—'}</span> :
                  <select value={doc.needAnalysis || '是'} onChange={e => onChange('needAnalysis', e.target.value)} style={{ width: '100%', border: '1px solid #E2E8F0', borderRadius: 6, padding: '8px 10px', fontSize: 13 }}>
                    <option>是</option><option>否</option>
                  </select>}
              </td>
              <th className="pa-pm-label">变更程度</th>
              <td>
                {readonly ? <span>{doc.changeLevel || '—'}</span> :
                  <select value={doc.changeLevel || '低'} onChange={e => onChange('changeLevel', e.target.value)} style={{ width: '100%', border: '1px solid #E2E8F0', borderRadius: 6, padding: '8px 10px', fontSize: 13 }}>
                    <option>高</option><option>中</option><option>低</option>
                  </select>}
              </td>
            </tr>
            {[
              ['costImpact', '对成本的影响'],
              ['scheduleImpact', '对进度的影响'],
              ['resourceImpact', '对资源的影响'],
              ['ifNotChangeImpact', '若不进行变更为何影响'],
            ].map(([k, label]) => (
              <tr key={k}>
                <th className="pa-pm-label">{label}</th>
                <td colSpan={3}>
                  {readonly ? <span>{doc[k] || '—'}</span> : <Input value={doc[k] || ''} onChange={e => onChange(k, e.target.value)} bordered={false} style={{ padding: 0 }} />}
                </td>
              </tr>
            ))}
            <tr>
              <th className="pa-pm-label">申请人签字</th><td>{readonly ? <span>{doc.applicantSign || '—'}</span> : <Input value={doc.applicantSign || ''} onChange={e => onChange('applicantSign', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td>
              <th className="pa-pm-label">申请日期</th><td>{readonly ? <span>{doc.applyDate || '—'}</span> : <Input value={doc.applyDate || ''} onChange={e => onChange('applyDate', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td>
            </tr>
          </tbody></table>
        </div>
        <div className="pa-pm-sheet-wrap">
          <div className="pa-pm-section-head">五、审批结果</div>
          <table className="pa-pm-sheet"><tbody>
            <tr><th className="pa-pm-label">审批意见</th><td colSpan={3}>
              {readonly ? <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontFamily: 'inherit' }}>{doc.approvalOpinion || '—'}</pre> : <Input.TextArea value={doc.approvalOpinion || ''} onChange={e => onChange('approvalOpinion', e.target.value)} rows={3} bordered={false} style={{ padding: 0, resize: 'vertical' }} />}
            </td></tr>
            <tr>
              <th className="pa-pm-label">审批人签字</th><td>{readonly ? <span>{doc.approverSign || '—'}</span> : <Input value={doc.approverSign || ''} onChange={e => onChange('approverSign', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td>
              <th className="pa-pm-label">日期</th><td>{readonly ? <span>{doc.approvalDate || '—'}</span> : <Input value={doc.approvalDate || ''} onChange={e => onChange('approvalDate', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td>
            </tr>
          </tbody></table>
        </div>
      </>;

    case 'meeting':
      return <>
        <MeetingBasicInfo doc={doc} onChange={onChange} readonly={readonly} />
        <TextSection title="会议目标" hint="简要说明会议目标及期望结果" value={doc.objectives} onChange={v => onChange('objectives', v)} readonly={readonly} rows={2} />
        <TextSection title="参加人员" hint="列出参会人员及项目中角色" value={doc.participants} onChange={v => onChange('participants', v)} readonly={readonly} rows={3} />
        <TextSection title="发放材料" hint="列出会议讨论的项目资料" value={doc.materials} onChange={v => onChange('materials', v)} readonly={readonly} rows={2} />
        <TextSection title="发言记录" hint="记录发言人观点、意见和建议" value={doc.speechRecord} onChange={v => onChange('speechRecord', v)} readonly={readonly} rows={4} />
        <TextSection title="会议决议" hint="说明会议结论" value={doc.resolution} onChange={v => onChange('resolution', v)} readonly={readonly} rows={2} />
        <div className="pa-pm-sheet-wrap">
          <div className="pa-pm-section-head">会议纪要发放范围</div>
          <table className="pa-pm-sheet"><tbody>
            <tr>
              <th className="pa-pm-label">报送</th><td>{readonly ? <span>{doc.reportTo || '—'}</span> : <Input value={doc.reportTo || ''} onChange={e => onChange('reportTo', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td>
              <th className="pa-pm-label">主送</th><td>{readonly ? <span>{doc.mainTo || '—'}</span> : <Input value={doc.mainTo || ''} onChange={e => onChange('mainTo', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td>
            </tr>
            <tr><th className="pa-pm-label">抄送</th><td colSpan={3}>{readonly ? <span>{doc.copyTo || '—'}</span> : <Input value={doc.copyTo || ''} onChange={e => onChange('copyTo', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td></tr>
          </tbody></table>
        </div>
      </>;

    case 'status':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <div className="pa-pm-sheet-wrap">
          <table className="pa-pm-sheet"><tbody>
            <tr>
              <th className="pa-pm-label">当前项目状况</th><td>{readonly ? <span>{doc.currentStatus || '—'}</span> : <Input value={doc.currentStatus || ''} onChange={e => onChange('currentStatus', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td>
              <th className="pa-pm-label">汇报周期</th><td>{readonly ? <span>{doc.reportPeriod || '—'}</span> : <Input value={doc.reportPeriod || ''} onChange={e => onChange('reportPeriod', e.target.value)} bordered={false} style={{ padding: 0 }} />}</td>
            </tr>
          </tbody></table>
        </div>
        <SubTable title="二、当前任务状态" cols={SUB_COLS.taskStatus} rows={doc.taskStatus || []} onChange={v => onChange('taskStatus', v)} readonly={readonly} />
        <TextSection title="三、本周期主要活动" hint="对本周期主要交付物进行总结" value={doc.periodActivities} onChange={v => onChange('periodActivities', v)} readonly={readonly} />
        <TextSection title="四、下一周期活动计划" hint="需与项目计划及 WBS 对应" value={doc.nextPlan} onChange={v => onChange('nextPlan', v)} readonly={readonly} />
        <TextSection title="五、财务状况" value={doc.finance} onChange={v => onChange('finance', v)} readonly={readonly} rows={3} />
        <TextSection title="六、上期遗留问题处理" value={doc.legacyIssues} onChange={v => onChange('legacyIssues', v)} readonly={readonly} rows={3} />
        <TextSection title="七、本期问题与求助" value={doc.issuesHelp} onChange={v => onChange('issuesHelp', v)} readonly={readonly} rows={3} />
      </>;

    case 'strategic':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <SubTable title="二、战略解码" cols={GENERIC_TABLE_COLS.strategic} rows={doc.rows || []} onChange={v => onChange('rows', v)} readonly={readonly} />
      </>;

    case 'orgPerf':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <SubTable title="二、组织绩效" cols={GENERIC_TABLE_COLS.orgPerf} rows={doc.rows || []} onChange={v => onChange('rows', v)} readonly={readonly} />
      </>;

    case 'pbc':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <SubTable title="二、个人绩效承诺" cols={GENERIC_TABLE_COLS.pbc} rows={doc.rows || []} onChange={v => onChange('rows', v)} readonly={readonly} />
      </>;

    case 'issue':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <SubTable title="二、问题清单" cols={GENERIC_TABLE_COLS.issue} rows={doc.rows || []} onChange={v => onChange('rows', v)} readonly={readonly} />
      </>;

    case 'knowledge':
      return <>
        <BaseInfoGrid doc={doc} onChange={onChange} readonly={readonly} />
        <TextSection title="二、经验与发现" value={doc.findings} onChange={v => onChange('findings', v)} readonly={readonly} />
        <TextSection title="三、改进建议" value={doc.suggestions} onChange={v => onChange('suggestions', v)} readonly={readonly} />
      </>;

    default:
      return <p>未配置表单模板</p>;
  }
}

/* ============================================================
   Factory
   ============================================================ */
export type PmModule = 'strategic' | 'orgPerf' | 'pbc' | 'charter' | 'milestone' | 'risk' | 'issue' | 'change' | 'meeting' | 'knowledge' | 'wbs' | 'communication' | 'status';

export function createPmFormPage(module: PmModule) {
  return function PmFormPageInner() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const meta = PAGE_META[module];

    const [docs, setDocs] = useState<any[]>(() => {
      const init = INIT_DOCS[module];
      return init ? [...init] : [];
    });
    const [search, setSearch] = useState('');
    const [mode, setMode] = useState<'list' | 'form'>('list');
    const [editingDoc, setEditingDoc] = useState<any>(null);
    const [formReadonly, setFormReadonly] = useState(false);

    /* ---- 列表标题 ---- */
    function listTitle(doc: any) {
      if (module === 'meeting') return doc.meetingName || '未命名会议';
      return doc.projectName || doc.projectNo || '未命名文档';
    }
    function listSubtitle(doc: any) {
      if (module === 'meeting') return (doc.meetingDate || '') + ' · ' + (doc.projectName || '');
      return (doc.projectNo || '') + ' · 项目经理：' + (doc.projectManager || '—') + ' · ' + (doc.prepareDate || '');
    }

    const filtered = useMemo(() => {
      if (!search) return docs;
      const q = search.toLowerCase();
      return docs.filter(d => {
        const t = (listTitle(d) + listSubtitle(d)).toLowerCase();
        return t.includes(q);
      });
    }, [docs, search]);

    /* ---- 表单操作 ---- */
    const openForm = (id: string | null, readonly: boolean) => {
      setFormReadonly(readonly);
      if (id) {
        const doc = docs.find(d => d.id === id);
        setEditingDoc(doc ? { ...doc } : emptyDoc(module));
      } else {
        setEditingDoc(emptyDoc(module));
      }
      setMode('form');
    };

    const backToList = () => {
      setMode('list');
      setEditingDoc(null);
    };

    const saveForm = () => {
      if (!editingDoc) return;
      const idx = docs.findIndex(d => d.id === editingDoc.id);
      let next: any[];
      if (idx >= 0) {
        next = docs.map((d, i) => i === idx ? editingDoc : d);
      } else {
        next = [...docs, editingDoc];
      }
      setDocs(next);
      message.success('已保存');
      backToList();
    };

    const deleteDoc = (id: string) => {
      setDocs(prev => prev.filter(d => d.id !== id));
      message.success('已删除');
    };

    const updateField = (k: string, v: any) => {
      setEditingDoc((prev: any) => ({ ...prev, [k]: v }));
    };

    /* ---- 提示文案 ---- */
    const hint = HINTS[module];

    return (
      <div className="pa-page">
        <header className="pa-topbar">
          <div className="pa-topbar-left">
            <a className="pa-back-link" onClick={() => navigateTo('zhixun-project-archives')}>
              <ArrowLeftOutlined /> 返回项目档案
            </a>
            <div>
              <h1>{meta.title}</h1>
              <span className="pa-sub">{meta.sub}</span>
            </div>
          </div>
          <span className="pa-topbar-tag">{meta.icon} {meta.tag}</span>
        </header>

        <main className="pa-main">
          <div className="pa-hint-card">
            <strong>分节表单台账</strong>：字段结构对齐您提供的 Excel 管理表模板。支持列表 <strong>查</strong>、表单 <strong>增/改</strong>、记录 <strong>删</strong>；子表支持动态添加行。
            {hint && <><br />{hint}</>}
          </div>

          {mode === 'list' ? (
            <div className="pa-card">
              <div className="pa-toolbar">
                <Button type="primary" icon={<PlusOutlined />} onClick={() => openForm(null, false)}>新建</Button>
                <Input
                  prefix={<SearchOutlined />}
                  placeholder="搜索项目名称、编号…"
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  className="pa-search-input"
                  style={{ maxWidth: 280 }}
                  allowClear
                />
              </div>
              <div className="pa-list-panel">
                {!filtered.length ? (
                  <p style={{ textAlign: 'center', color: '#64748B', padding: 32, fontSize: 14 }}>暂无记录，点击「新建」填写表单。</p>
                ) : (
                  filtered.map(doc => (
                    <div key={doc.id} className="pa-record-card">
                      <div>
                        <h4>{listTitle(doc)}</h4>
                        <p>{listSubtitle(doc)}</p>
                      </div>
                      <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
                        <Button size="small" icon={<EyeOutlined />} onClick={() => openForm(doc.id, true)}>查看</Button>
                        <Button size="small" icon={<EditOutlined />} onClick={() => openForm(doc.id, false)}>编辑</Button>
                        <Button size="small" danger icon={<DeleteOutlined />} onClick={() => deleteDoc(doc.id)}>删除</Button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          ) : (
            <div className="pa-card">
              <div className="pa-form-actions-bar">
                <Button onClick={backToList}>← 返回列表</Button>
                {!formReadonly && <Button type="primary" onClick={saveForm}>保存</Button>}
              </div>
              {editingDoc && renderFormSections(module, editingDoc, updateField, formReadonly)}
            </div>
          )}
        </main>
      </div>
    );
  };
}
