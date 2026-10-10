import { useState } from 'react';
import { Typography, Button } from 'antd';
import {
  ArrowRightOutlined,
  UserSwitchOutlined,
  FileTextOutlined,
  FileProtectOutlined,
  AuditOutlined,
  ToolOutlined,
  SafetyCertificateOutlined,
  FileAddOutlined,
  TrophyOutlined,
  DollarOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './TrainingProcessPage.css';

// ============================================================
// 常量
// ============================================================
const STEP_NAMES = ['人才识别', '初步方案', '最终方案', '开班申报', '培训实施', '认定准备', '申报服务', '培训结业', '项目回款'];

const PROCESS_PAGES: Record<string, { route: string; icon: React.ReactNode; title: string; entry: string }> = {
  '人才识别': { route: 'zhixun-talent-identification', icon: <UserSwitchOutlined />, title: '人才识别', entry: '导入企业人才数据、岗位需求分析与参训人员识别确认。' },
  '初步方案': { route: 'zhixun-preliminary-plan', icon: <FileTextOutlined />, title: '初步方案', entry: '编制培训初步方案：工种等级、人数规模、周期与资源测算。' },
  '最终方案': { route: 'zhixun-final-plan', icon: <FileProtectOutlined />, title: '最终方案', entry: '完善并审批培训最终方案，确定课表结构与实施计划。' },
  '开班申报': { route: 'zhixun-class-declaration', icon: <AuditOutlined />, title: '开班申报', entry: '办理开班申请、承诺书、人社材料提交与开班仪式。' },
  '培训实施': { route: 'zhixun-training-implementation', icon: <ToolOutlined />, title: '培训实施', entry: '课次考勤、课堂留痕、跟班日志与培训过程异常处理。' },
  '认定准备': { route: 'zhixun-certification', icon: <SafetyCertificateOutlined />, title: '认定准备', entry: '认定条件审核、考核安排、获证花名册与人社材料准备。' },
  '申报服务': { route: 'zhixun-declaration-service', icon: <FileAddOutlined />, title: '申报服务', entry: '补贴材料包生成、人社申报提交与拨款进度跟踪。' },
  '培训结业': { route: 'zhixun-graduation', icon: <TrophyOutlined />, title: '培训结业', entry: '结课统计、结业证发放与学员档案归档。' },
  '项目回款': { route: 'zhixun-refund', icon: <DollarOutlined />, title: '项目回款', entry: '回款计划、收款登记、项目结算与审批结案。' },
};

const PROCESS_DATA: Record<string, { title: string; desc: string; stats: { val: string; lbl: string }[] }> = {
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

  return (
    <div className="tp-page">
      <header className="tp-header">
        <h2>培训流程</h2>
        <p>人才识别 → 初步方案 → 最终方案 → 开班申报 → 培训实施 → 认定准备 → 申报服务 → 培训结业 → 项目回款</p>
      </header>

      {/* 步骤导航 */}
      <nav className="tp-steps-nav">
        {STEP_NAMES.map((step, i) => (
          <div
            key={step}
            className={`tp-step-item${step === currentStep ? ' active' : ''}`}
            onClick={() => setCurrentStep(step)}
          >
            <span className="tp-step-num">步骤 {i + 1}</span>
            {step}
          </div>
        ))}
      </nav>

      {/* 步骤内容 */}
      <div className="tp-card">
        {/* 进入按钮卡片 */}
        <a className="tp-entry-card" onClick={() => navigateTo(page.route)}>
          <div className="tp-entry-text">
            <h3>{page.icon} {page.title}</h3>
            <p>{page.entry}</p>
          </div>
          <span className="tp-entry-btn"><ArrowRightOutlined /> 进入{page.title}</span>
        </a>

        <p className="tp-desc">{data.desc}</p>

        {/* 迷你统计 */}
        <div className="tp-stat-mini-grid">
          {data.stats.map((s, i) => (
            <div key={i} className="tp-stat-mini">
              <div className="tp-stat-val">{s.val}</div>
              <div className="tp-stat-lbl">{s.lbl}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
