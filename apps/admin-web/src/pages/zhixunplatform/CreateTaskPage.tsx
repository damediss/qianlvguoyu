import { useState, useCallback, useEffect } from 'react';
import { Typography, Form, Input, Select, DatePicker, InputNumber, Steps, Button, Card, Checkbox, Table, Progress, message } from 'antd';
import {
  ArrowLeftOutlined,
  InfoCircleOutlined,
  BankOutlined,
  CheckCircleOutlined,
  TeamOutlined,
  UserOutlined,
  HomeOutlined,
  BookOutlined,
  DollarOutlined,
  CalendarOutlined,
  NodeIndexOutlined,
  AuditOutlined,
  EnterOutlined,
  SaveOutlined,
  CheckOutlined,
  FileAddOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './CreateTaskPage.css';

// ============================================================
// 步骤3 配置项定义
// ============================================================
const CONFIG_GROUPS = [
  {
    title: '人员安排',
    icon: <TeamOutlined />,
    items: [
      { key: 'humanTeacher', title: '师资与教务', desc: '主讲、班主任、跟班与授课资质' },
      { key: 'humanStudent', title: '参训学员', desc: '学员名单、企业确认与资格审核' },
    ],
  },
  {
    title: '资源与场地',
    icon: <HomeOutlined />,
    items: [
      { key: 'machine', title: '机构与场地', desc: '合作机构、讲师资源、场地设备与占用' },
    ],
  },
  {
    title: '课程与经费',
    icon: <BookOutlined />,
    items: [
      { key: 'materialCourse', title: '课程与课件', desc: '课纲、课件课包及版本审批' },
      { key: 'materialCost', title: '预算与费用', desc: '预算科目、结算分成与回款' },
    ],
  },
  {
    title: '计划与制度',
    icon: <CalendarOutlined />,
    items: [
      { key: 'lawSchedule', title: '分班排课', desc: '分班、排课、课表与冲突检测' },
      { key: 'lawMode', title: '培养方案', desc: '培养路径与规划方案版本' },
      { key: 'lawRule', title: '业务规则', desc: '分班、考勤等业务策略' },
    ],
  },
  {
    title: '开班准备',
    icon: <EnterOutlined />,
    items: [
      { key: 'environment', title: '开班与合规', desc: '开班条件、场地落实与人社材料' },
    ],
  },
];

const ALL_CONFIG_ITEMS = CONFIG_GROUPS.flatMap((g) => g.items);

// ============================================================
// 模拟学员名册
// ============================================================
const MOCK_STUDENTS = [
  { name: '张三', idCard: '4501**********1234', occupation: '电工', status: '已确认' },
  { name: '李四', idCard: '4501**********5678', occupation: '电工', status: '已确认' },
  { name: '王五', idCard: '4501**********9012', occupation: '电工', status: '待补充材料' },
];

// ============================================================
// 预算表格
// ============================================================
const MOCK_BUDGET_ITEMS = [
  { item: '讲师课酬', amount: '120,000', note: '主讲+实操' },
  { item: '场地设备', amount: '45,000', note: '实训基地' },
  { item: '管理与杂费', amount: '35,000', note: '—' },
];

// ============================================================
// 排课表格
// ============================================================
const MOCK_SCHEDULE = [
  { course: '安全规范', teacher: '陈教授', time: '周一 09:00-11:30' },
  { course: '实操训练', teacher: '刘师傅', time: '周三 14:00-17:00' },
];

// ============================================================
// 步骤3 各面板
// ============================================================
function ConfigPanelHumanTeacher() {
  return (
    <div className="config-form-grid">
      <Form.Item label="主讲讲师" name={['config', 'humanTeacher', 'mainTeacher']}><Input placeholder="姓名" /></Form.Item>
      <Form.Item label="助教" name={['config', 'humanTeacher', 'assistant']}><Input placeholder="可选" /></Form.Item>
      <Form.Item label="班主任" name={['config', 'humanTeacher', 'headTeacher']}><Input placeholder="姓名" /></Form.Item>
      <Form.Item label="跟班教务" name={['config', 'humanTeacher', 'followStaff']}><Input placeholder="姓名" /></Form.Item>
      <Form.Item label="资质状态" name={['config', 'humanTeacher', 'qualification']} initialValue="有效">
        <Select options={['有效', '即将到期', '待补充'].map((v) => ({ value: v, label: v }))} />
      </Form.Item>
      <Form.Item label="派师锁定" name={['config', 'humanTeacher', 'dispatchLock']} initialValue="是">
        <Select options={['是', '否'].map((v) => ({ value: v, label: v }))} />
      </Form.Item>
      <Form.Item label="备注" name={['config', 'humanTeacher', 'remark']} className="full">
        <Input.TextArea placeholder="资质说明、承诺书编号等" rows={3} />
      </Form.Item>
    </div>
  );
}

function ConfigPanelHumanStudent() {
  return (
    <>
      <div className="config-form-grid">
        <Form.Item label="应到人数" name={['config', 'humanStudent', 'expectedCount']} initialValue={40}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="已确认人数" name={['config', 'humanStudent', 'confirmedCount']} initialValue={38}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="企业确认状态" name={['config', 'humanStudent', 'corpConfirm']} initialValue="已确认">
          <Select options={['已确认', '部分确认', '待确认'].map((v) => ({ value: v, label: v }))} />
        </Form.Item>
        <Form.Item label="资格审核" name={['config', 'humanStudent', 'qualReview']} initialValue="通过">
          <Select options={['通过', '待补充', '审核中'].map((v) => ({ value: v, label: v }))} />
        </Form.Item>
      </div>
      <div style={{ fontSize: 13, fontWeight: 500, marginTop: 16 }}>学员名册</div>
      <Table dataSource={MOCK_STUDENTS.map((s, i) => ({ ...s, key: i }))} pagination={false} size="small" bordered style={{ marginTop: 8 }}
        columns={[
          { title: '姓名', dataIndex: 'name' },
          { title: '身份证', dataIndex: 'idCard' },
          { title: '工种', dataIndex: 'occupation' },
          { title: '状态', dataIndex: 'status' },
        ]}
      />
      <Button icon={<FileAddOutlined />} style={{ marginTop: 12 }}>导入名单</Button>
    </>
  );
}

function ConfigPanelMachine() {
  return (
    <div className="config-form-grid">
      <Form.Item label="合作培训机构" name={['config', 'machine', 'orgName']}><Input placeholder="机构全称" /></Form.Item>
      <Form.Item label="培训场地" name={['config', 'machine', 'venue']}><Input placeholder="实训基地 / 教室" /></Form.Item>
      <Form.Item label="场地容量" name={['config', 'machine', 'capacity']}><InputNumber min={0} placeholder="人" style={{ width: '100%' }} /></Form.Item>
      <Form.Item label="设备状态" name={['config', 'machine', 'equipment']} initialValue="齐全">
        <Select options={['齐全', '部分到位', '待落实'].map((v) => ({ value: v, label: v }))} />
      </Form.Item>
      <Form.Item label="占用时段" name={['config', 'machine', 'occupyTime']}><Input placeholder="如 5月12日-6月30日" /></Form.Item>
      <Form.Item label="冲突检测" name={['config', 'machine', 'conflict']} initialValue="无冲突">
        <Select options={['无冲突', '有冲突待协调'].map((v) => ({ value: v, label: v }))} />
      </Form.Item>
    </div>
  );
}

function ConfigPanelMaterialCourse() {
  return (
    <div className="config-form-grid">
      <Form.Item label="课程名称" name={['config', 'materialCourse', 'courseName']}><Input placeholder="如：电工安全技术" /></Form.Item>
      <Form.Item label="课纲版本" name={['config', 'materialCourse', 'syllabusVer']} initialValue="v2026.1"><Input /></Form.Item>
      <Form.Item label="课件课包" name={['config', 'materialCourse', 'courseware']}><Input placeholder="课件包名称" /></Form.Item>
      <Form.Item label="审批状态" name={['config', 'materialCourse', 'approveStatus']} initialValue="已审批">
        <Select options={['已审批', '审批中', '草稿'].map((v) => ({ value: v, label: v }))} />
      </Form.Item>
      <Form.Item label="交付说明" name={['config', 'materialCourse', 'deliveryNote']} className="full">
        <Input.TextArea placeholder="理论/实操课时分配等" rows={3} />
      </Form.Item>
    </div>
  );
}

function ConfigPanelMaterialCost() {
  return (
    <>
      <div className="config-form-grid">
        <Form.Item label="预算总额（元）" name={['config', 'materialCost', 'budgetTotal']} initialValue={280000}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="已拨付（元）" name={['config', 'materialCost', 'paidAmount']} initialValue={120000}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="企业分成比例" name={['config', 'materialCost', 'corpShare']} initialValue="30%"><Input /></Form.Item>
        <Form.Item label="机构分成比例" name={['config', 'materialCost', 'orgShare']} initialValue="70%"><Input /></Form.Item>
      </div>
      <Table dataSource={MOCK_BUDGET_ITEMS.map((r, i) => ({ ...r, key: i }))} pagination={false} size="small" bordered style={{ marginTop: 16 }}
        columns={[
          { title: '科目', dataIndex: 'item' },
          { title: '金额（元）', dataIndex: 'amount' },
          { title: '说明', dataIndex: 'note' },
        ]}
      />
    </>
  );
}

function ConfigPanelLawSchedule() {
  return (
    <>
      <div className="config-form-grid">
        <Form.Item label="班级名称" name={['config', 'lawSchedule', 'className']}><Input placeholder="如：电工高级班第3期" /></Form.Item>
        <Form.Item label="计划开班" name={['config', 'lawSchedule', 'classStart']}><DatePicker style={{ width: '100%' }} /></Form.Item>
        <Form.Item label="周课时" name={['config', 'lawSchedule', 'weeklyHours']} initialValue={24}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="排课完成率" name={['config', 'lawSchedule', 'scheduleRate']} initialValue="96%"><Input /></Form.Item>
      </div>
      <Table dataSource={MOCK_SCHEDULE.map((r, i) => ({ ...r, key: i }))} pagination={false} size="small" bordered style={{ marginTop: 16 }}
        columns={[
          { title: '课程', dataIndex: 'course' },
          { title: '讲师', dataIndex: 'teacher' },
          { title: '时间', dataIndex: 'time' },
        ]}
      />
    </>
  );
}

function ConfigPanelLawMode() {
  return (
    <div className="config-form-grid">
      <Form.Item label="培养模式" name={['config', 'lawMode', 'trainMode']} initialValue="集中培训">
        <Select options={['集中培训', '工学结合', '线上+线下'].map((v) => ({ value: v, label: v }))} />
      </Form.Item>
      <Form.Item label="方案版本" name={['config', 'lawMode', 'planVersion']} initialValue="2026-Q2-V1"><Input /></Form.Item>
      <Form.Item label="培养路径说明" name={['config', 'lawMode', 'pathDesc']} className="full">
        <Input.TextArea placeholder="阶段目标、考核节点…" rows={3} />
      </Form.Item>
    </div>
  );
}

function ConfigPanelLawRule() {
  return (
    <>
      <ul className="check-list">
        <li><Form.Item name={['config', 'lawRule', 'ruleAttendance']} valuePropName="checked" initialValue={true} noStyle><Checkbox>启用考勤打卡规则（每日2次）</Checkbox></Form.Item></li>
        <li><Form.Item name={['config', 'lawRule', 'ruleLate']} valuePropName="checked" initialValue={true} noStyle><Checkbox>迟到超过3次触发预警</Checkbox></Form.Item></li>
        <li><Form.Item name={['config', 'lawRule', 'ruleClass']} valuePropName="checked" initialValue={true} noStyle><Checkbox>按工种自动分班</Checkbox></Form.Item></li>
        <li><Form.Item name={['config', 'lawRule', 'ruleExam']} valuePropName="checked" noStyle><Checkbox>结业前强制模拟考试</Checkbox></Form.Item></li>
      </ul>
      <Form.Item label="自定义规则说明" name={['config', 'lawRule', 'ruleCustom']} style={{ marginTop: 16 }}>
        <Input.TextArea placeholder="其他业务约束…" rows={3} />
      </Form.Item>
    </>
  );
}

function ConfigPanelEnvironment() {
  return (
    <>
      <div className="config-form-grid">
        <Form.Item label="计划开班日期" name={['config', 'environment', 'openDate']}><DatePicker style={{ width: '100%' }} /></Form.Item>
        <Form.Item label="开班地点" name={['config', 'environment', 'openPlace']}><Input placeholder="详细地址" /></Form.Item>
        <Form.Item label="场地落实" name={['config', 'environment', 'venueReady']} initialValue="已落实">
          <Select options={['已落实', '待确认'].map((v) => ({ value: v, label: v }))} />
        </Form.Item>
      </div>
      <div style={{ fontSize: 13, fontWeight: 500, margin: '16px 0 10px' }}>人社材料清单</div>
      <ul className="check-list">
        <li><Form.Item name={['config', 'environment', 'docPlan']} valuePropName="checked" initialValue={true} noStyle><Checkbox>培训计划备案</Checkbox></Form.Item></li>
        <li><Form.Item name={['config', 'environment', 'docRoster']} valuePropName="checked" initialValue={true} noStyle><Checkbox>学员名册</Checkbox></Form.Item></li>
        <li><Form.Item name={['config', 'environment', 'docVenue']} valuePropName="checked" noStyle><Checkbox>场地证明</Checkbox></Form.Item></li>
        <li><Form.Item name={['config', 'environment', 'docContract']} valuePropName="checked" noStyle><Checkbox>培训合同</Checkbox></Form.Item></li>
      </ul>
    </>
  );
}

const PANEL_MAP: Record<string, { icon: React.ReactNode; title: string; desc: string; component: React.ReactNode }> = {
  humanTeacher: { icon: <UserOutlined />, title: '师资与教务', desc: '指派主讲、助教、班主任与跟班，绑定授课资质与承诺书', component: <ConfigPanelHumanTeacher /> },
  humanStudent: { icon: <TeamOutlined />, title: '参训学员', desc: '学员名单导入、企业确认与资格审核', component: <ConfigPanelHumanStudent /> },
  machine: { icon: <HomeOutlined />, title: '机构与场地', desc: '合作机构、讲师资源池、场地设备与占用冲突检测', component: <ConfigPanelMachine /> },
  materialCourse: { icon: <BookOutlined />, title: '课程与课件', desc: '课纲、课件课包版本与审批状态', component: <ConfigPanelMaterialCourse /> },
  materialCost: { icon: <DollarOutlined />, title: '预算与费用', desc: '预算科目、结算分成与回款跟踪', component: <ConfigPanelMaterialCost /> },
  lawSchedule: { icon: <CalendarOutlined />, title: '分班排课', desc: '分班、排课、课表发布与冲突检测', component: <ConfigPanelLawSchedule /> },
  lawMode: { icon: <NodeIndexOutlined />, title: '培养方案', desc: '培养路径与规划方案版本绑定', component: <ConfigPanelLawMode /> },
  lawRule: { icon: <AuditOutlined />, title: '业务规则', desc: '分班、考勤等业务策略配置', component: <ConfigPanelLawRule /> },
  environment: { icon: <EnterOutlined />, title: '开班与合规', desc: '开班条件、场地落实与人社材料准备', component: <ConfigPanelEnvironment /> },
};

// ============================================================
// 主页面
// ============================================================
// 任务数据（与 TaskArchivesPage 共享的模拟数据）
const DEFAULT_TASKS = [
  { id: 1, name: '2026年度安全培训计划编制', owner: '张明', deadline: '2026-06-15', priority: '高', enterprise: '广西建工集团', occupation: '安全员', skillLevel: '高级', region: '南宁', projectType: '安全培训', status: '进行中', planCount: 120, startDate: '2026-03-01', endDate: '2026-06-30', description: '' },
  { id: 2, name: '电工高级班认定材料整理', owner: '李芳', deadline: '2026-05-25', priority: '高', enterprise: '南宁供电局', occupation: '电工', skillLevel: '高级', region: '南宁', projectType: '资质认证', status: '进行中', planCount: 42 },
  { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', deadline: '2026-05-30', priority: '中', enterprise: '柳州钢铁有限公司', occupation: '焊工', skillLevel: '中级', region: '柳州', projectType: '新员工培训', status: '草稿', planCount: 36 },
  { id: 4, name: '焊工中级班排课协调', owner: '赵丽', deadline: '2026-05-22', priority: '中', enterprise: '桂林旅游股份公司', occupation: '焊工', skillLevel: '中级', region: '桂林', projectType: '资质认证', status: '待审批', planCount: 28 },
  { id: 5, name: '培训档案年度归档', owner: '陈伟', deadline: '2026-06-30', priority: '低', enterprise: '梧州港务集团', occupation: '起重工', skillLevel: '初级', region: '梧州', projectType: '管理研修', status: '草稿', planCount: 15 },
];

export function CreateTaskPage() {
  const [form] = Form.useForm();
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const editingTaskId = useNavStore((s) => s.editingTaskId);
  const setEditingTaskId = useNavStore((s) => s.setEditingTaskId);
  const [currentStep, setCurrentStep] = useState(0);
  const [activeConfigTab, setActiveConfigTab] = useState('humanTeacher');
  const [configuredKeys, setConfiguredKeys] = useState<Set<string>>(new Set());

  const configDoneCount = configuredKeys.size;
  const configTotal = ALL_CONFIG_ITEMS.length;
  const configPct = Math.round((configDoneCount / configTotal) * 100);

  const markConfigured = useCallback((key: string) => {
    setConfiguredKeys((prev) => new Set(prev).add(key));
  }, []);

  // 编辑模式：回填表单
  useEffect(() => {
    if (editingTaskId) {
      const task = DEFAULT_TASKS.find((t) => t.id === editingTaskId);
      if (task) {
        form.setFieldsValue({
          taskName: task.name,
          taskOwner: task.owner,
          taskDeadline: undefined, // DatePicker needs dayjs object
          taskPriority: task.priority,
          taskStatus: task.status,
          taskDesc: task.description,
          enterprise: task.enterprise,
          region: task.region,
          occupation: task.occupation,
          skillLevel: task.skillLevel,
          projectType: task.projectType,
          planCount: task.planCount,
          startDate: undefined,
          endDate: undefined,
          trainingNeed: '',
        });
      }
    }
  }, [editingTaskId, form]);

  // 返回时清除编辑 ID
  const handleBack = useCallback(() => {
    setEditingTaskId(null);
    navigateTo('zhixun-task-management');
  }, [navigateTo, setEditingTaskId]);

  const goStep = useCallback(
    (n: number) => {
      if (n === 1 && currentStep === 0) {
        form.validateFields(['taskName', 'taskOwner', 'taskDeadline']).catch(() => {
          message.warning('请填写任务名称、负责人和截止日期');
          return;
        });
      }
      if (n === 2 && currentStep === 1) {
        form.validateFields(['enterprise']).catch(() => {
          message.warning('请填写委托企业');
          return;
        });
      }
      setCurrentStep(n);
    },
    [currentStep, form],
  );

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      console.log('提交数据:', values);
      message.success('任务已创建，可在任务档案中查看');
    }).catch(() => message.warning('请完善必填项'));
  };

  const panel = PANEL_MAP[activeConfigTab];

  return (
    <div className="create-task-page">
      {/* 顶部栏 */}
      <header className="ct-topbar">
        <div className="ct-topbar-left">
          <a className="ct-back-link" onClick={handleBack}>
            <ArrowLeftOutlined /> 返回任务管理
          </a>
          <div>
            <h1 className="ct-title">创建培训任务</h1>
            <span className="ct-sub">基本信息 → 项目关联 → 任务配置</span>
          </div>
        </div>
        <span className="ct-task-code">新建任务</span>
      </header>

      {/* 主内容 */}
      <main className={`ct-page${currentStep === 2 ? ' ct-page-wide' : ''}`}>
        {/* 步骤条 */}
        <nav className="ct-steps-bar">
          {['基本信息', '项目关联', '任务配置', '确认提交'].map((label, i) => (
            <button
              key={i}
              className={`ct-step-pill${currentStep === i ? ' active' : ''}${i < currentStep ? ' done' : ''}`}
              onClick={() => goStep(i)}
            >
              <span className="num">步骤 {i + 1}</span>
              {label}
            </button>
          ))}
        </nav>

        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          {/* 步骤1：基本信息 */}
          {currentStep === 0 && (
            <section className="ct-section">
              <Card title={<><InfoCircleOutlined style={{ color: '#2A6DFF' }} /> 任务基本信息</>} className="ct-card">
                <div className="ct-form-grid">
                  <Form.Item label="任务名称" name="taskName" rules={[{ required: true, message: '请填写' }]} className="full">
                    <Input placeholder="如：2026年南宁电工高级班培训任务" />
                  </Form.Item>
                  <Form.Item label="负责人" name="taskOwner" rules={[{ required: true, message: '请填写' }]}>
                    <Input placeholder="项目经理/培训专员" />
                  </Form.Item>
                  <Form.Item label="截止日期" name="taskDeadline" rules={[{ required: true, message: '请选择' }]}>
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item label="优先级" name="taskPriority" initialValue="中">
                    <Select options={['高', '中', '低'].map((v) => ({ value: v, label: v }))} />
                  </Form.Item>
                  <Form.Item label="任务状态" name="taskStatus" initialValue="草稿">
                    <Select options={['草稿', '进行中', '待审批'].map((v) => ({ value: v, label: v }))} />
                  </Form.Item>
                  <Form.Item label="任务说明" name="taskDesc" className="full">
                    <Input.TextArea placeholder="培训目标、交付要求、备注…" rows={4} />
                  </Form.Item>
                </div>
              </Card>
            </section>
          )}

          {/* 步骤2：项目关联 */}
          {currentStep === 1 && (
            <section className="ct-section">
              <Card title={<><BankOutlined style={{ color: '#2A6DFF' }} /> 项目关联信息</>} extra={<span style={{ fontSize: 12, color: '#64748b' }}>对应立项表 / 项目创建字段</span>} className="ct-card">
                <div className="ct-form-grid">
                  <Form.Item label="委托企业" name="enterprise" rules={[{ required: true, message: '请填写' }]}>
                    <Input placeholder="企业全称" />
                  </Form.Item>
                  <Form.Item label="所属区域（广西）" name="region" initialValue="南宁">
                    <Select options={['南宁', '桂林', '柳州', '玉林', '梧州', '其他'].map((v) => ({ value: v, label: v }))} />
                  </Form.Item>
                  <Form.Item label="培训工种" name="occupation" rules={[{ required: true, message: '请选择' }]}>
                    <Select placeholder="请选择" options={['电工', '焊工', '起重工', '架子工', '安全员'].map((v) => ({ value: v, label: v }))} />
                  </Form.Item>
                  <Form.Item label="技能等级" name="skillLevel" initialValue="中级">
                    <Select options={['初级', '中级', '高级', '技师'].map((v) => ({ value: v, label: v }))} />
                  </Form.Item>
                  <Form.Item label="项目类型" name="projectType" initialValue="资质认证">
                    <Select options={['新员工培训', '晋升培训', '资质认证', '安全培训', '管理研修'].map((v) => ({ value: v, label: v }))} />
                  </Form.Item>
                  <Form.Item label="计划人数" name="planCount" initialValue={40}>
                    <InputNumber min={1} placeholder="如：42" style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item label="计划开始" name="startDate"><DatePicker style={{ width: '100%' }} /></Form.Item>
                  <Form.Item label="计划结束" name="endDate"><DatePicker style={{ width: '100%' }} /></Form.Item>
                  <Form.Item label="培训需求摘要" name="trainingNeed" className="full">
                    <Input.TextArea placeholder="企业需求、补贴申报意向、特殊要求…" rows={4} />
                  </Form.Item>
                </div>
              </Card>
            </section>
          )}

          {/* 步骤3：任务配置 */}
          {currentStep === 2 && (
            <section className="ct-section">
              <div className="ct-config-header">
                <div>
                  <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 4 }}>任务配置</h2>
                  <p style={{ fontSize: 13, color: '#64748b', margin: 0 }}>逐项完成人员、资源、课程、计划与开班准备配置</p>
                </div>
                <div className="ct-config-progress">
                  <div className="ct-config-progress-label">
                    <span>配置完成度</span>
                    <span>{configDoneCount} / {configTotal}</span>
                  </div>
                  <Progress percent={configPct} showInfo={false} size="small" strokeColor={{ from: '#2A6DFF', to: '#5B8FFF' }} trailColor="#e2e8f0" />
                </div>
              </div>

              <div className="ct-config-layout">
                {/* 左侧导航 */}
                <nav className="ct-config-sidebar">
                  {CONFIG_GROUPS.map((group) => (
                    <div key={group.title}>
                      <div className="ct-config-nav-group">{group.title}</div>
                      {group.items.map((item) => (
                        <button
                          key={item.key}
                          type="button"
                          className={`ct-config-nav-item${activeConfigTab === item.key ? ' active' : ''}${configuredKeys.has(item.key) ? ' done' : ''}`}
                          onClick={() => setActiveConfigTab(item.key)}
                        >
                          <span className="nav-dot" />
                          {item.title}
                        </button>
                      ))}
                    </div>
                  ))}
                </nav>

                {/* 右侧面板 */}
                <div className="ct-config-main">
                  <div className="ct-config-panel active" key={activeConfigTab}>
                    <div className="ct-config-panel-head">
                      <h3>{panel.icon} {panel.title}</h3>
                      <p>{panel.desc}</p>
                    </div>
                    {panel.component}
                    <div className="ct-panel-actions">
                      <span className="hint">填写后点击保存，可切换其他配置项</span>
                      <Button type="primary" icon={<SaveOutlined />} onClick={() => markConfigured(activeConfigTab)}>
                        保存本节
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          )}

          {/* 步骤4：确认提交 */}
          {currentStep === 3 && (
            <section className="ct-section">
              <Card title={<><CheckCircleOutlined style={{ color: '#2A6DFF' }} /> 确认并提交</>} className="ct-card">
                <div style={{ fontSize: 14, lineHeight: 1.8, color: '#64748b' }}>
                  <p><strong>任务名称：</strong>{form.getFieldValue('taskName') || '—'}</p>
                  <p><strong>负责人：</strong>{form.getFieldValue('taskOwner') || '—'} · <strong>截止：</strong>{form.getFieldValue('taskDeadline')?.format?.('YYYY-MM-DD') || '—'}</p>
                  <p><strong>企业 / 工种：</strong>{form.getFieldValue('enterprise') || '—'} · {form.getFieldValue('occupation') || '—'} · {form.getFieldValue('skillLevel') || '—'}</p>
                  <p><strong>区域：</strong>{form.getFieldValue('region') || '—'} · <strong>类型：</strong>{form.getFieldValue('projectType') || '—'}</p>
                  <p><strong>配置完成度：</strong>{configDoneCount} / {configTotal} 项（可选，提交后可继续补充）</p>
                </div>
              </Card>
            </section>
          )}

          {/* 底部按钮 */}
          <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 8 }}>
            {currentStep > 0 && (
              <Button onClick={() => goStep(currentStep - 1)}>上一步</Button>
            )}
            {currentStep < 3 && (
              <Button onClick={() => goStep(currentStep + 1)}>下一步</Button>
            )}
            {currentStep === 3 && (
              <Button type="primary" icon={<CheckOutlined />} htmlType="submit">
                提交任务
              </Button>
            )}
          </div>
        </Form>
      </main>
    </div>
  );
}
