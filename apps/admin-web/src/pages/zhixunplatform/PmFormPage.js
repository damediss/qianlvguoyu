import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo, Fragment } from 'react';
import { Button, Input, message } from 'antd';
import { ArrowLeftOutlined, PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined, EyeOutlined, AimOutlined, BarChartOutlined, UserSwitchOutlined, FileTextOutlined, FlagOutlined, WarningOutlined, BugOutlined, RetweetOutlined, MessageOutlined, BulbOutlined, ApartmentOutlined, SendOutlined, ProfileOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './ArchivePages.css';
/* ============================================================
   基础字段
   ============================================================ */
const PM_BASE_FIELDS = [
    { key: 'projectName', label: '项目名称' },
    { key: 'projectNo', label: '项目编号' },
    { key: 'preparedBy', label: '制作人' },
    { key: 'reviewedBy', label: '审核人' },
    { key: 'projectManager', label: '项目经理' },
    { key: 'prepareDate', label: '制作日期', type: 'date' },
];
function uid(prefix) {
    return prefix + '-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
}
function emptyDoc(type) {
    const d = { id: uid(type) };
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
const SUB_COLS = {
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
const GENERIC_TABLE_COLS = {
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
const PAGE_META = {
    strategic: { title: '战略解码表', sub: '填写：战略目标、关键行动、衡量指标、责任部门、完成时限与进度（组织级OKR分解）', tag: '项目管控', icon: _jsx(AimOutlined, {}) },
    orgPerf: { title: '组织绩效表', sub: '填写：考核部门、指标、目标值、实际值、得分（部门年度/季度绩效）', tag: '项目管控', icon: _jsx(BarChartOutlined, {}) },
    pbc: { title: '个人绩效承诺（PBC）', sub: '填写：员工、岗位、个人目标、KR、权重、自评与上级评分', tag: '项目管控', icon: _jsx(UserSwitchOutlined, {}) },
    charter: { title: '项目立项 Charter', sub: '一、基本情况；二、项目描述；三、里程碑计划；四、评价标准；五、假定与约束；六、利益干系人；项目组成员', tag: '项目管控', icon: _jsx(FileTextOutlined, {}) },
    milestone: { title: '里程碑 / 项目进度表', sub: '项目基本情况 + 任务阶段、责任人、关键里程碑（对齐进度表模板）', tag: '项目管控', icon: _jsx(FlagOutlined, {}) },
    risk: { title: '风险管理表', sub: '风险概率准则 + 风险清单（描述、概率、影响、等级、响应计划、责任人、开放/关闭）', tag: '项目管控', icon: _jsx(WarningOutlined, {}) },
    issue: { title: '问题管理表', sub: '项目基本情况 + 问题登记清单', tag: '项目管控', icon: _jsx(BugOutlined, {}) },
    change: { title: '变更管理表', sub: '历史变更记录、变更描述、影响分析、审批结果（变更申请模板）', tag: '项目管控', icon: _jsx(RetweetOutlined, {}) },
    meeting: { title: '会议纪要 / 行动项', sub: '会议基本信息、目标、参会人、材料、发言记录、决议、发放范围', tag: '项目管控', icon: _jsx(MessageOutlined, {}) },
    knowledge: { title: '知识收割表', sub: '项目基本情况 + 经验发现 + 改进建议', tag: '项目管控', icon: _jsx(BulbOutlined, {}) },
    wbs: { title: '工作分解结构 WBS', sub: '分解代码、任务名称、活动、工时/人力/费用/工期', tag: '项目管控', icon: _jsx(ApartmentOutlined, {}) },
    communication: { title: '项目沟通计划', sub: '利益干系人、所需信息、频率、方法、责任人', tag: '项目管控', icon: _jsx(SendOutlined, {}) },
    status: { title: '项目状态报告', sub: '任务状态、本周期活动、下周期计划、财务、遗留问题、本期求助', tag: '项目管控', icon: _jsx(ProfileOutlined, {}) },
};
const HINTS = {
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
const INIT_DOCS = {
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
function BaseInfoGrid({ doc, onChange, readonly }) {
    const rows = [];
    for (let i = 0; i < PM_BASE_FIELDS.length; i += 2) {
        rows.push([PM_BASE_FIELDS[i], PM_BASE_FIELDS[i + 1]]);
    }
    return (_jsxs("div", { className: "pa-pm-sheet-wrap", children: [_jsx("div", { className: "pa-pm-section-head", children: "\u4E00\u3001\u9879\u76EE\u57FA\u672C\u60C5\u51B5" }), _jsx("table", { className: "pa-pm-sheet", children: _jsx("tbody", { children: rows.map((pair, ri) => (_jsx("tr", { children: pair.map(f => f ? (_jsxs(Fragment, { children: [_jsx("th", { className: "pa-pm-label", children: f.label }), _jsx("td", { children: readonly
                                        ? _jsx("span", { children: doc[f.key] || '—' })
                                        : _jsx(Input, { value: doc[f.key] || '', onChange: e => onChange?.(f.key, e.target.value), bordered: false, style: { padding: 0 } }) })] }, f.key)) : null) }, ri))) }) })] }));
}
/** 文本区段 */
function TextSection({ title, hint, value, onChange, readonly, rows }) {
    return (_jsxs("div", { className: "pa-pm-sheet-wrap", children: [_jsx("div", { className: "pa-pm-section-head", children: title }), hint && _jsx("div", { className: "pa-pm-hint", children: hint }), _jsx("table", { className: "pa-pm-sheet", children: _jsx("tbody", { children: _jsx("tr", { children: _jsx("td", { children: readonly
                                ? _jsx("pre", { style: { margin: 0, whiteSpace: 'pre-wrap', fontFamily: 'inherit', fontSize: 13 }, children: value || '—' })
                                : _jsx(Input.TextArea, { value: value || '', onChange: e => onChange?.(e.target.value), rows: rows || 4, bordered: false, style: { padding: 0, resize: 'vertical' } }) }) }) }) })] }));
}
/** 子表（动态行） */
function SubTable({ title, cols, rows, onChange, readonly }) {
    const addRow = () => {
        const newRow = {};
        cols.forEach(c => { newRow[c.key] = ''; });
        onChange?.([...rows, newRow]);
    };
    const delRow = (idx) => {
        onChange?.(rows.filter((_, i) => i !== idx));
    };
    const updateCell = (idx, key, val) => {
        const next = rows.map((r, i) => i === idx ? { ...r, [key]: val } : r);
        onChange?.(next);
    };
    return (_jsxs("div", { className: "pa-pm-sheet-wrap", children: [_jsx("div", { className: "pa-pm-section-head", children: title }), !readonly && (_jsx("div", { className: "pa-pm-sub-toolbar", children: _jsx(Button, { size: "small", onClick: addRow, children: "+ \u6DFB\u52A0\u884C" }) })), _jsxs("table", { className: "pa-pm-sheet", children: [_jsx("thead", { children: _jsxs("tr", { children: [cols.map(c => _jsx("th", { style: { background: '#e8f0fe', fontWeight: 600, border: '1px solid #94a3b8', padding: '8px 10px', fontSize: 13 }, children: c.label }, c.key)), !readonly && _jsx("th", { style: { width: 72, background: '#e8f0fe', border: '1px solid #94a3b8' }, children: "\u64CD\u4F5C" })] }) }), _jsx("tbody", { children: rows.length === 0 && readonly ? (_jsx("tr", { children: _jsx("td", { colSpan: cols.length, style: { textAlign: 'center', color: '#999', padding: 16 }, children: "\u6682\u65E0\u6570\u636E" }) })) : (rows.map((row, idx) => (_jsxs("tr", { children: [cols.map(c => (_jsx("td", { style: { border: '1px solid #94a3b8', padding: '8px 10px' }, children: readonly
                                        ? _jsx("span", { children: row[c.key] || '—' })
                                        : _jsx(Input, { value: row[c.key] || '', onChange: e => updateCell(idx, c.key, e.target.value), bordered: false, style: { padding: 0 } }) }, c.key))), !readonly && (_jsx("td", { style: { border: '1px solid #94a3b8', padding: '8px 10px' }, children: _jsx(Button, { size: "small", danger: true, onClick: () => delRow(idx), children: "\u5220\u9664" }) }))] }, idx)))) })] })] }));
}
/** 会议基础信息（独立于 BaseInfoGrid） */
function MeetingBasicInfo({ doc, onChange, readonly }) {
    const pairs = [
        ['meetingName', '会议名称', 'convener', '召集人'],
        ['meetingDate', '会议日期', 'startTime', '开始时间'],
        ['location', '会议地点', 'duration', '持续时间'],
        ['recorder', '记录人', 'reviewer', '审核人'],
    ];
    const InputCell = ({ k }) => readonly
        ? _jsx("span", { children: doc[k] || '—' })
        : _jsx(Input, { value: doc[k] || '', onChange: e => onChange?.(k, e.target.value), bordered: false, style: { padding: 0 } });
    return (_jsxs("div", { className: "pa-pm-sheet-wrap", children: [_jsx("div", { className: "pa-pm-section-head", children: "\u4F1A\u8BAE\u7EAA\u8981" }), _jsx("table", { className: "pa-pm-sheet", children: _jsxs("tbody", { children: [pairs.map(([k1, l1, k2, l2]) => (_jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: l1 }), _jsx("td", { children: _jsx(InputCell, { k: k1 }) }), _jsx("th", { className: "pa-pm-label", children: l2 }), _jsx("td", { children: _jsx(InputCell, { k: k2 }) })] }, k1))), _jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u9879\u76EE\u540D\u79F0" }), _jsx("td", { children: _jsx(InputCell, { k: "projectName" }) }), _jsx("th", { className: "pa-pm-label", children: "\u9879\u76EE\u7F16\u53F7" }), _jsx("td", { children: _jsx(InputCell, { k: "projectNo" }) })] })] }) })] }));
}
/** 多选组（变更管理 — 受影响基准计划） */
function CheckGroup({ options, selected, onChange, readonly }) {
    const toggle = (v) => {
        if (!onChange)
            return;
        if (selected.includes(v))
            onChange(selected.filter(x => x !== v));
        else
            onChange([...selected, v]);
    };
    return (_jsx("div", { style: { display: 'flex', gap: 16, flexWrap: 'wrap' }, children: options.map(o => (_jsxs("label", { style: { display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 13, cursor: readonly ? 'default' : 'pointer' }, children: [_jsx("input", { type: "checkbox", checked: selected.includes(o), disabled: readonly, onChange: () => toggle(o) }), " ", o] }, o))) }));
}
/* ============================================================
   表单渲染（类型特定）
   ============================================================ */
function renderFormSections(type, doc, onChange, readonly) {
    switch (type) {
        case 'charter':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(TextSection, { title: "\u4E8C\u3001\u9879\u76EE\u63CF\u8FF0 \u2014 \u9879\u76EE\u80CC\u666F\u4E0E\u76EE\u7684", hint: "\u6240\u6709\u9879\u76EE\u90FD\u6E90\u4E8E\u4E00\u5B9A\u7684\u4E1A\u52A1\u95EE\u9898\uFF0C\u672C\u8282\u5E94\u7B80\u8981\u63CF\u8FF0\u8FD9\u4E9B\u95EE\u9898\u3002", value: doc.background, onChange: v => onChange('background', v), readonly: readonly }), _jsx(TextSection, { title: "\u9879\u76EE\u76EE\u6807", hint: "\u5305\u62EC\u8D28\u91CF\u76EE\u6807\u3001\u8FDB\u5EA6\u76EE\u6807\u3001\u6210\u672C\u76EE\u6807\u53CA\u4EA4\u4ED8\u4EA7\u54C1\u7279\u6027\u7B49\u8BF4\u660E\u3002", value: doc.goals, onChange: v => onChange('goals', v), readonly: readonly }), _jsx(TextSection, { title: "\u4E09\u3001\u9879\u76EE\u91CC\u7A0B\u7891\u8BA1\u5212", hint: "\u5305\u62EC\u91CC\u7A0B\u7891\u65F6\u95F4\u70B9\u53CA\u6210\u679C\u3002", value: doc.milestonePlan, onChange: v => onChange('milestonePlan', v), readonly: readonly }), _jsx(TextSection, { title: "\u56DB\u3001\u8BC4\u4EF7\u6807\u51C6", hint: "\u8BF4\u660E\u4F55\u79CD\u60C5\u51B5\u4E0B\u9879\u76EE\u6210\u679C\u5C06\u88AB\u63A5\u53D7\u3002", value: doc.evaluationCriteria, onChange: v => onChange('evaluationCriteria', v), readonly: readonly }), _jsx(TextSection, { title: "\u4E94\u3001\u9879\u76EE\u5047\u5B9A\u4E0E\u7EA6\u675F\u6761\u4EF6", hint: "\u8BF4\u660E\u9879\u76EE\u4E3B\u8981\u5047\u5B9A\u53CA\u9650\u5236\u6761\u4EF6\u3002", value: doc.assumptions, onChange: v => onChange('assumptions', v), readonly: readonly }), _jsx(SubTable, { title: "\u516D\u3001\u9879\u76EE\u4E3B\u8981\u5229\u76CA\u5E72\u7CFB\u4EBA", cols: SUB_COLS.stakeholders, rows: doc.stakeholders || [], onChange: v => onChange('stakeholders', v), readonly: readonly }), _jsx(SubTable, { title: "\u9879\u76EE\u7EC4\u6210\u5458", cols: SUB_COLS.teamMembers, rows: doc.teamMembers || [], onChange: v => onChange('teamMembers', v), readonly: readonly })] });
        case 'wbs':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx("div", { className: "pa-pm-hint", style: { border: '1px solid #94a3b8', marginBottom: 0 }, children: "\uFF08R-\u8D1F\u8D23\uFF1BAs-\u8F85\u52A9\uFF1BI-\u901A\u77E5\uFF1BAp-\u5BA1\u6279\uFF09" }), _jsx(SubTable, { title: "\u4E8C\u3001\u5DE5\u4F5C\u5206\u89E3\u7ED3\u6784", cols: SUB_COLS.wbsRows, rows: doc.wbsRows || [], onChange: v => onChange('wbsRows', v), readonly: readonly })] });
        case 'milestone':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(TextSection, { title: "\u4E8C\u3001\u9879\u76EE\u8FDB\u5EA6\u8BF4\u660E", hint: "\u6309\u5468\u8DDF\u8E2A\u4EFB\u52A1\uFF0C\u53EF\u5728\u4E0B\u65B9\u7EF4\u62A4\u4EFB\u52A1\u6E05\u5355\u3002", value: doc.scheduleNote, onChange: v => onChange('scheduleNote', v), readonly: readonly, rows: 2 }), _jsx(SubTable, { title: "\u9879\u76EE\u8FDB\u5EA6\u8868\uFF08\u4EFB\u52A1 / \u8D23\u4EFB\u4EBA / \u91CC\u7A0B\u7891\uFF09", cols: SUB_COLS.tasks, rows: doc.tasks || [], onChange: v => onChange('tasks', v), readonly: readonly })] });
        case 'risk':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsxs("div", { className: "pa-pm-sheet-wrap", children: [_jsx("div", { className: "pa-pm-section-head", children: "\u4E8C\u3001\u9879\u76EE\u98CE\u9669\u7BA1\u7406" }), _jsxs("div", { className: "pa-pm-criteria", children: [_jsx("strong", { children: "\u98CE\u9669\u53D1\u751F\u6982\u7387\u5224\u65AD\u51C6\u5219\uFF1A" }), _jsx("br", {}), "\u9AD8\u98CE\u9669\uFF1A\u53D1\u751F\u6982\u7387 >60%\uFF1B\u4E2D\u98CE\u9669\uFF1A30%\u201360%\uFF1B\u4F4E\u98CE\u9669\uFF1A<30%\u3002"] })] }), _jsx(SubTable, { title: "\u98CE\u9669\u6E05\u5355", cols: SUB_COLS.risks, rows: doc.risks || [], onChange: v => onChange('risks', v), readonly: readonly })] });
        case 'communication':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(SubTable, { title: "\u4E8C\u3001\u9879\u76EE\u6C9F\u901A\u8BA1\u5212", cols: SUB_COLS.plans, rows: doc.plans || [], onChange: v => onChange('plans', v), readonly: readonly })] });
        case 'change':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(SubTable, { title: "\u4E8C\u3001\u5386\u53F2\u53D8\u66F4\u8BB0\u5F55", cols: SUB_COLS.history, rows: doc.history || [], onChange: v => onChange('history', v), readonly: readonly }), _jsx(TextSection, { title: "\u4E09\u3001\u8BF7\u6C42\u53D8\u66F4\u4FE1\u606F", hint: "\u5EFA\u8BAE\u7684\u53D8\u66F4\u63CF\u8FF0\u53CA\u53C2\u8003\u8D44\u6599", value: doc.requestDesc, onChange: v => onChange('requestDesc', v), readonly: readonly, rows: 5 }), _jsxs("div", { className: "pa-pm-sheet-wrap", children: [_jsx("div", { className: "pa-pm-section-head", children: "\u56DB\u3001\u5F71\u54CD\u5206\u6790" }), _jsx("table", { className: "pa-pm-sheet", children: _jsxs("tbody", { children: [_jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u53D7\u5F71\u54CD\u57FA\u51C6\u8BA1\u5212" }), _jsx("td", { colSpan: 3, children: _jsx(CheckGroup, { options: ['进度计划', '费用计划', '资源计划'], selected: doc.impactBaseline || [], onChange: v => onChange('impactBaseline', v), readonly: readonly }) })] }), _jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u9700\u6210\u672C/\u8FDB\u5EA6\u5F71\u54CD\u5206\u6790" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.needAnalysis || '—' }) :
                                                        _jsxs("select", { value: doc.needAnalysis || '是', onChange: e => onChange('needAnalysis', e.target.value), style: { width: '100%', border: '1px solid #E2E8F0', borderRadius: 6, padding: '8px 10px', fontSize: 13 }, children: [_jsx("option", { children: "\u662F" }), _jsx("option", { children: "\u5426" })] }) }), _jsx("th", { className: "pa-pm-label", children: "\u53D8\u66F4\u7A0B\u5EA6" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.changeLevel || '—' }) :
                                                        _jsxs("select", { value: doc.changeLevel || '低', onChange: e => onChange('changeLevel', e.target.value), style: { width: '100%', border: '1px solid #E2E8F0', borderRadius: 6, padding: '8px 10px', fontSize: 13 }, children: [_jsx("option", { children: "\u9AD8" }), _jsx("option", { children: "\u4E2D" }), _jsx("option", { children: "\u4F4E" })] }) })] }), [
                                            ['costImpact', '对成本的影响'],
                                            ['scheduleImpact', '对进度的影响'],
                                            ['resourceImpact', '对资源的影响'],
                                            ['ifNotChangeImpact', '若不进行变更为何影响'],
                                        ].map(([k, label]) => (_jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: label }), _jsx("td", { colSpan: 3, children: readonly ? _jsx("span", { children: doc[k] || '—' }) : _jsx(Input, { value: doc[k] || '', onChange: e => onChange(k, e.target.value), bordered: false, style: { padding: 0 } }) })] }, k))), _jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u7533\u8BF7\u4EBA\u7B7E\u5B57" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.applicantSign || '—' }) : _jsx(Input, { value: doc.applicantSign || '', onChange: e => onChange('applicantSign', e.target.value), bordered: false, style: { padding: 0 } }) }), _jsx("th", { className: "pa-pm-label", children: "\u7533\u8BF7\u65E5\u671F" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.applyDate || '—' }) : _jsx(Input, { value: doc.applyDate || '', onChange: e => onChange('applyDate', e.target.value), bordered: false, style: { padding: 0 } }) })] })] }) })] }), _jsxs("div", { className: "pa-pm-sheet-wrap", children: [_jsx("div", { className: "pa-pm-section-head", children: "\u4E94\u3001\u5BA1\u6279\u7ED3\u679C" }), _jsx("table", { className: "pa-pm-sheet", children: _jsxs("tbody", { children: [_jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u5BA1\u6279\u610F\u89C1" }), _jsx("td", { colSpan: 3, children: readonly ? _jsx("pre", { style: { margin: 0, whiteSpace: 'pre-wrap', fontFamily: 'inherit' }, children: doc.approvalOpinion || '—' }) : _jsx(Input.TextArea, { value: doc.approvalOpinion || '', onChange: e => onChange('approvalOpinion', e.target.value), rows: 3, bordered: false, style: { padding: 0, resize: 'vertical' } }) })] }), _jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u5BA1\u6279\u4EBA\u7B7E\u5B57" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.approverSign || '—' }) : _jsx(Input, { value: doc.approverSign || '', onChange: e => onChange('approverSign', e.target.value), bordered: false, style: { padding: 0 } }) }), _jsx("th", { className: "pa-pm-label", children: "\u65E5\u671F" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.approvalDate || '—' }) : _jsx(Input, { value: doc.approvalDate || '', onChange: e => onChange('approvalDate', e.target.value), bordered: false, style: { padding: 0 } }) })] })] }) })] })] });
        case 'meeting':
            return _jsxs(_Fragment, { children: [_jsx(MeetingBasicInfo, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(TextSection, { title: "\u4F1A\u8BAE\u76EE\u6807", hint: "\u7B80\u8981\u8BF4\u660E\u4F1A\u8BAE\u76EE\u6807\u53CA\u671F\u671B\u7ED3\u679C", value: doc.objectives, onChange: v => onChange('objectives', v), readonly: readonly, rows: 2 }), _jsx(TextSection, { title: "\u53C2\u52A0\u4EBA\u5458", hint: "\u5217\u51FA\u53C2\u4F1A\u4EBA\u5458\u53CA\u9879\u76EE\u4E2D\u89D2\u8272", value: doc.participants, onChange: v => onChange('participants', v), readonly: readonly, rows: 3 }), _jsx(TextSection, { title: "\u53D1\u653E\u6750\u6599", hint: "\u5217\u51FA\u4F1A\u8BAE\u8BA8\u8BBA\u7684\u9879\u76EE\u8D44\u6599", value: doc.materials, onChange: v => onChange('materials', v), readonly: readonly, rows: 2 }), _jsx(TextSection, { title: "\u53D1\u8A00\u8BB0\u5F55", hint: "\u8BB0\u5F55\u53D1\u8A00\u4EBA\u89C2\u70B9\u3001\u610F\u89C1\u548C\u5EFA\u8BAE", value: doc.speechRecord, onChange: v => onChange('speechRecord', v), readonly: readonly, rows: 4 }), _jsx(TextSection, { title: "\u4F1A\u8BAE\u51B3\u8BAE", hint: "\u8BF4\u660E\u4F1A\u8BAE\u7ED3\u8BBA", value: doc.resolution, onChange: v => onChange('resolution', v), readonly: readonly, rows: 2 }), _jsxs("div", { className: "pa-pm-sheet-wrap", children: [_jsx("div", { className: "pa-pm-section-head", children: "\u4F1A\u8BAE\u7EAA\u8981\u53D1\u653E\u8303\u56F4" }), _jsx("table", { className: "pa-pm-sheet", children: _jsxs("tbody", { children: [_jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u62A5\u9001" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.reportTo || '—' }) : _jsx(Input, { value: doc.reportTo || '', onChange: e => onChange('reportTo', e.target.value), bordered: false, style: { padding: 0 } }) }), _jsx("th", { className: "pa-pm-label", children: "\u4E3B\u9001" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.mainTo || '—' }) : _jsx(Input, { value: doc.mainTo || '', onChange: e => onChange('mainTo', e.target.value), bordered: false, style: { padding: 0 } }) })] }), _jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u6284\u9001" }), _jsx("td", { colSpan: 3, children: readonly ? _jsx("span", { children: doc.copyTo || '—' }) : _jsx(Input, { value: doc.copyTo || '', onChange: e => onChange('copyTo', e.target.value), bordered: false, style: { padding: 0 } }) })] })] }) })] })] });
        case 'status':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx("div", { className: "pa-pm-sheet-wrap", children: _jsx("table", { className: "pa-pm-sheet", children: _jsx("tbody", { children: _jsxs("tr", { children: [_jsx("th", { className: "pa-pm-label", children: "\u5F53\u524D\u9879\u76EE\u72B6\u51B5" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.currentStatus || '—' }) : _jsx(Input, { value: doc.currentStatus || '', onChange: e => onChange('currentStatus', e.target.value), bordered: false, style: { padding: 0 } }) }), _jsx("th", { className: "pa-pm-label", children: "\u6C47\u62A5\u5468\u671F" }), _jsx("td", { children: readonly ? _jsx("span", { children: doc.reportPeriod || '—' }) : _jsx(Input, { value: doc.reportPeriod || '', onChange: e => onChange('reportPeriod', e.target.value), bordered: false, style: { padding: 0 } }) })] }) }) }) }), _jsx(SubTable, { title: "\u4E8C\u3001\u5F53\u524D\u4EFB\u52A1\u72B6\u6001", cols: SUB_COLS.taskStatus, rows: doc.taskStatus || [], onChange: v => onChange('taskStatus', v), readonly: readonly }), _jsx(TextSection, { title: "\u4E09\u3001\u672C\u5468\u671F\u4E3B\u8981\u6D3B\u52A8", hint: "\u5BF9\u672C\u5468\u671F\u4E3B\u8981\u4EA4\u4ED8\u7269\u8FDB\u884C\u603B\u7ED3", value: doc.periodActivities, onChange: v => onChange('periodActivities', v), readonly: readonly }), _jsx(TextSection, { title: "\u56DB\u3001\u4E0B\u4E00\u5468\u671F\u6D3B\u52A8\u8BA1\u5212", hint: "\u9700\u4E0E\u9879\u76EE\u8BA1\u5212\u53CA WBS \u5BF9\u5E94", value: doc.nextPlan, onChange: v => onChange('nextPlan', v), readonly: readonly }), _jsx(TextSection, { title: "\u4E94\u3001\u8D22\u52A1\u72B6\u51B5", value: doc.finance, onChange: v => onChange('finance', v), readonly: readonly, rows: 3 }), _jsx(TextSection, { title: "\u516D\u3001\u4E0A\u671F\u9057\u7559\u95EE\u9898\u5904\u7406", value: doc.legacyIssues, onChange: v => onChange('legacyIssues', v), readonly: readonly, rows: 3 }), _jsx(TextSection, { title: "\u4E03\u3001\u672C\u671F\u95EE\u9898\u4E0E\u6C42\u52A9", value: doc.issuesHelp, onChange: v => onChange('issuesHelp', v), readonly: readonly, rows: 3 })] });
        case 'strategic':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(SubTable, { title: "\u4E8C\u3001\u6218\u7565\u89E3\u7801", cols: GENERIC_TABLE_COLS.strategic, rows: doc.rows || [], onChange: v => onChange('rows', v), readonly: readonly })] });
        case 'orgPerf':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(SubTable, { title: "\u4E8C\u3001\u7EC4\u7EC7\u7EE9\u6548", cols: GENERIC_TABLE_COLS.orgPerf, rows: doc.rows || [], onChange: v => onChange('rows', v), readonly: readonly })] });
        case 'pbc':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(SubTable, { title: "\u4E8C\u3001\u4E2A\u4EBA\u7EE9\u6548\u627F\u8BFA", cols: GENERIC_TABLE_COLS.pbc, rows: doc.rows || [], onChange: v => onChange('rows', v), readonly: readonly })] });
        case 'issue':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(SubTable, { title: "\u4E8C\u3001\u95EE\u9898\u6E05\u5355", cols: GENERIC_TABLE_COLS.issue, rows: doc.rows || [], onChange: v => onChange('rows', v), readonly: readonly })] });
        case 'knowledge':
            return _jsxs(_Fragment, { children: [_jsx(BaseInfoGrid, { doc: doc, onChange: onChange, readonly: readonly }), _jsx(TextSection, { title: "\u4E8C\u3001\u7ECF\u9A8C\u4E0E\u53D1\u73B0", value: doc.findings, onChange: v => onChange('findings', v), readonly: readonly }), _jsx(TextSection, { title: "\u4E09\u3001\u6539\u8FDB\u5EFA\u8BAE", value: doc.suggestions, onChange: v => onChange('suggestions', v), readonly: readonly })] });
        default:
            return _jsx("p", { children: "\u672A\u914D\u7F6E\u8868\u5355\u6A21\u677F" });
    }
}
export function createPmFormPage(module) {
    return function PmFormPageInner() {
        const navigateTo = useNavStore((s) => s.setSelectedKey);
        const meta = PAGE_META[module];
        const [docs, setDocs] = useState(() => {
            const init = INIT_DOCS[module];
            return init ? [...init] : [];
        });
        const [search, setSearch] = useState('');
        const [mode, setMode] = useState('list');
        const [editingDoc, setEditingDoc] = useState(null);
        const [formReadonly, setFormReadonly] = useState(false);
        /* ---- 列表标题 ---- */
        function listTitle(doc) {
            if (module === 'meeting')
                return doc.meetingName || '未命名会议';
            return doc.projectName || doc.projectNo || '未命名文档';
        }
        function listSubtitle(doc) {
            if (module === 'meeting')
                return (doc.meetingDate || '') + ' · ' + (doc.projectName || '');
            return (doc.projectNo || '') + ' · 项目经理：' + (doc.projectManager || '—') + ' · ' + (doc.prepareDate || '');
        }
        const filtered = useMemo(() => {
            if (!search)
                return docs;
            const q = search.toLowerCase();
            return docs.filter(d => {
                const t = (listTitle(d) + listSubtitle(d)).toLowerCase();
                return t.includes(q);
            });
        }, [docs, search]);
        /* ---- 表单操作 ---- */
        const openForm = (id, readonly) => {
            setFormReadonly(readonly);
            if (id) {
                const doc = docs.find(d => d.id === id);
                setEditingDoc(doc ? { ...doc } : emptyDoc(module));
            }
            else {
                setEditingDoc(emptyDoc(module));
            }
            setMode('form');
        };
        const backToList = () => {
            setMode('list');
            setEditingDoc(null);
        };
        const saveForm = () => {
            if (!editingDoc)
                return;
            const idx = docs.findIndex(d => d.id === editingDoc.id);
            let next;
            if (idx >= 0) {
                next = docs.map((d, i) => i === idx ? editingDoc : d);
            }
            else {
                next = [...docs, editingDoc];
            }
            setDocs(next);
            message.success('已保存');
            backToList();
        };
        const deleteDoc = (id) => {
            setDocs(prev => prev.filter(d => d.id !== id));
            message.success('已删除');
        };
        const updateField = (k, v) => {
            setEditingDoc((prev) => ({ ...prev, [k]: v }));
        };
        /* ---- 提示文案 ---- */
        const hint = HINTS[module];
        return (_jsxs("div", { className: "pa-page", children: [_jsxs("header", { className: "pa-topbar", children: [_jsxs("div", { className: "pa-topbar-left", children: [_jsxs("a", { className: "pa-back-link", onClick: () => navigateTo('zhixun-project-archives'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u9879\u76EE\u6863\u6848"] }), _jsxs("div", { children: [_jsx("h1", { children: meta.title }), _jsx("span", { className: "pa-sub", children: meta.sub })] })] }), _jsxs("span", { className: "pa-topbar-tag", children: [meta.icon, " ", meta.tag] })] }), _jsxs("main", { className: "pa-main", children: [_jsxs("div", { className: "pa-hint-card", children: [_jsx("strong", { children: "\u5206\u8282\u8868\u5355\u53F0\u8D26" }), "\uFF1A\u5B57\u6BB5\u7ED3\u6784\u5BF9\u9F50\u60A8\u63D0\u4F9B\u7684 Excel \u7BA1\u7406\u8868\u6A21\u677F\u3002\u652F\u6301\u5217\u8868 ", _jsx("strong", { children: "\u67E5" }), "\u3001\u8868\u5355 ", _jsx("strong", { children: "\u589E/\u6539" }), "\u3001\u8BB0\u5F55 ", _jsx("strong", { children: "\u5220" }), "\uFF1B\u5B50\u8868\u652F\u6301\u52A8\u6001\u6DFB\u52A0\u884C\u3002", hint && _jsxs(_Fragment, { children: [_jsx("br", {}), hint] })] }), mode === 'list' ? (_jsxs("div", { className: "pa-card", children: [_jsxs("div", { className: "pa-toolbar", children: [_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => openForm(null, false), children: "\u65B0\u5EFA" }), _jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u9879\u76EE\u540D\u79F0\u3001\u7F16\u53F7\u2026", value: search, onChange: e => setSearch(e.target.value), className: "pa-search-input", style: { maxWidth: 280 }, allowClear: true })] }), _jsx("div", { className: "pa-list-panel", children: !filtered.length ? (_jsx("p", { style: { textAlign: 'center', color: '#64748B', padding: 32, fontSize: 14 }, children: "\u6682\u65E0\u8BB0\u5F55\uFF0C\u70B9\u51FB\u300C\u65B0\u5EFA\u300D\u586B\u5199\u8868\u5355\u3002" })) : (filtered.map(doc => (_jsxs("div", { className: "pa-record-card", children: [_jsxs("div", { children: [_jsx("h4", { children: listTitle(doc) }), _jsx("p", { children: listSubtitle(doc) })] }), _jsxs("div", { style: { display: 'flex', gap: 8, flexShrink: 0 }, children: [_jsx(Button, { size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => openForm(doc.id, true), children: "\u67E5\u770B" }), _jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openForm(doc.id, false), children: "\u7F16\u8F91" }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => deleteDoc(doc.id), children: "\u5220\u9664" })] })] }, doc.id)))) })] })) : (_jsxs("div", { className: "pa-card", children: [_jsxs("div", { className: "pa-form-actions-bar", children: [_jsx(Button, { onClick: backToList, children: "\u2190 \u8FD4\u56DE\u5217\u8868" }), !formReadonly && _jsx(Button, { type: "primary", onClick: saveForm, children: "\u4FDD\u5B58" })] }), editingDoc && renderFormSections(module, editingDoc, updateField, formReadonly)] }))] })] }));
    };
}
