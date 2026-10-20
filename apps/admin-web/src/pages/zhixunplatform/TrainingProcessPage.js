import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
import { ArrowRightOutlined, UserSwitchOutlined, FileTextOutlined, FileProtectOutlined, AuditOutlined, ToolOutlined, SafetyCertificateOutlined, FileAddOutlined, TrophyOutlined, DollarOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TrainingProcessPage.css';
// ============================================================
// 常量
// ============================================================
const STEP_NAMES = ['人才识别', '初步方案', '最终方案', '开班申报', '培训实施', '认定准备', '申报服务', '培训结业', '项目回款'];
const PROCESS_PAGES = {
    '人才识别': { route: 'zhixun-talent-identification', icon: _jsx(UserSwitchOutlined, {}), title: '人才识别', entry: '导入企业人才数据、岗位需求分析与参训人员识别确认。' },
    '初步方案': { route: 'zhixun-preliminary-plan', icon: _jsx(FileTextOutlined, {}), title: '初步方案', entry: '编制培训初步方案：工种等级、人数规模、周期与资源测算。' },
    '最终方案': { route: 'zhixun-final-plan', icon: _jsx(FileProtectOutlined, {}), title: '最终方案', entry: '完善并审批培训最终方案，确定课表结构与实施计划。' },
    '开班申报': { route: 'zhixun-class-declaration', icon: _jsx(AuditOutlined, {}), title: '开班申报', entry: '办理开班申请、承诺书、人社材料提交与开班仪式。' },
    '培训实施': { route: 'zhixun-training-implementation', icon: _jsx(ToolOutlined, {}), title: '培训实施', entry: '课次考勤、课堂留痕、跟班日志与培训过程异常处理。' },
    '认定准备': { route: 'zhixun-certification', icon: _jsx(SafetyCertificateOutlined, {}), title: '认定准备', entry: '认定条件审核、考核安排、获证花名册与人社材料准备。' },
    '申报服务': { route: 'zhixun-declaration-service', icon: _jsx(FileAddOutlined, {}), title: '申报服务', entry: '补贴材料包生成、人社申报提交与拨款进度跟踪。' },
    '培训结业': { route: 'zhixun-graduation', icon: _jsx(TrophyOutlined, {}), title: '培训结业', entry: '结课统计、结业证发放与学员档案归档。' },
    '项目回款': { route: 'zhixun-refund', icon: _jsx(DollarOutlined, {}), title: '项目回款', entry: '回款计划、收款登记、项目结算与审批结案。' },
};
const PROCESS_DATA = {
    '人才识别': { title: '人才识别', desc: '基于企业人才数据与岗位需求，识别参训对象并建立培训人员台账。', stats: [{ val: '486', lbl: '已识别人数' }, { val: '12', lbl: '在办项目' }, { val: '96%', lbl: '数据完整率' }] },
    '初步方案': { title: '初步方案', desc: '编制培训初步方案：培训目标、工种等级、周期安排与资源需求测算。', stats: [{ val: '18', lbl: '方案草稿' }, { val: '12', lbl: '待审批' }, { val: '6', lbl: '已通过' }] },
    '最终方案': { title: '最终方案', desc: '确定培训最终方案与课表结构，完成多方确认后进入开班申报。', stats: [{ val: '15', lbl: '已定稿' }, { val: '156', lbl: '计划课时' }, { val: '100%', lbl: '方案确认率' }] },
    '开班申报': { title: '开班申报', desc: '按最终方案办理开班申请、承诺书、人社材料提交与开班仪式。', stats: [{ val: '3', lbl: '本周开班' }, { val: '128', lbl: '新入学员' }, { val: '5', lbl: '培训场地' }] },
    '培训实施': { title: '培训实施', desc: '按项目跟踪在培班级：课次考勤、过程留痕、跟班日志与异常处理。', stats: [{ val: '92.4%', lbl: '平均签到率' }, { val: '78%', lbl: '课程进度' }, { val: '4.6', lbl: '满意度' }] },
    '认定准备': { title: '认定准备', desc: '认定条件审核、理论/实操考核安排、获证花名册与人社材料准备。', stats: [{ val: '342', lbl: '待认定' }, { val: '87.6%', lbl: '通过率' }, { val: '156', lbl: '本月已认定' }] },
    '申报服务': { title: '申报服务', desc: '按政策模板生成补贴材料包、提交人社并跟踪拨款进度。', stats: [{ val: '12', lbl: '待申报' }, { val: '8', lbl: '审核中' }, { val: '45', lbl: '已通过' }] },
    '培训结业': { title: '培训结业', desc: '结课统计、材料包整理、结业证发放与学员档案归档。', stats: [{ val: '1,248', lbl: '本年结业' }, { val: '1,186', lbl: '已发证' }, { val: '62', lbl: '待发证' }] },
    '项目回款': { title: '项目回款', desc: '回款计划与账龄、收款登记、项目结算分成及审批结案。', stats: [{ val: '2,860万', lbl: '累计回款' }, { val: '428万', lbl: '待回款' }, { val: '87%', lbl: '回款率' }] },
};
// ============================================================
// 主页面
// ============================================================
export function TrainingProcessPage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const [currentStep, setCurrentStep] = useState('人才识别');
    const data = PROCESS_DATA[currentStep];
    const page = PROCESS_PAGES[currentStep];
    return (_jsxs("div", { className: "tp-page", children: [_jsxs("header", { className: "tp-header", children: [_jsx("h2", { children: "\u57F9\u8BAD\u6D41\u7A0B" }), _jsx("p", { children: "\u4EBA\u624D\u8BC6\u522B \u2192 \u521D\u6B65\u65B9\u6848 \u2192 \u6700\u7EC8\u65B9\u6848 \u2192 \u5F00\u73ED\u7533\u62A5 \u2192 \u57F9\u8BAD\u5B9E\u65BD \u2192 \u8BA4\u5B9A\u51C6\u5907 \u2192 \u7533\u62A5\u670D\u52A1 \u2192 \u57F9\u8BAD\u7ED3\u4E1A \u2192 \u9879\u76EE\u56DE\u6B3E" })] }), _jsx("nav", { className: "tp-steps-nav", children: STEP_NAMES.map((step, i) => (_jsxs("div", { className: `tp-step-item${step === currentStep ? ' active' : ''}`, onClick: () => setCurrentStep(step), children: [_jsxs("span", { className: "tp-step-num", children: ["\u6B65\u9AA4 ", i + 1] }), step] }, step))) }), _jsxs("div", { className: "tp-card", children: [_jsxs("a", { className: "tp-entry-card", onClick: () => navigateTo(page.route), children: [_jsxs("div", { className: "tp-entry-text", children: [_jsxs("h3", { children: [page.icon, " ", page.title] }), _jsx("p", { children: page.entry })] }), _jsxs("span", { className: "tp-entry-btn", children: [_jsx(ArrowRightOutlined, {}), " \u8FDB\u5165", page.title] })] }), _jsx("p", { className: "tp-desc", children: data.desc }), _jsx("div", { className: "tp-stat-mini-grid", children: data.stats.map((s, i) => (_jsxs("div", { className: "tp-stat-mini", children: [_jsx("div", { className: "tp-stat-val", children: s.val }), _jsx("div", { className: "tp-stat-lbl", children: s.lbl })] }, i))) })] })] }));
}
