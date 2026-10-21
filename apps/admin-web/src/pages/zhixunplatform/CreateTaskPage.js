import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useCallback, useEffect } from 'react';
import { Form, Input, Select, DatePicker, InputNumber, Button, Card, Checkbox, Table, Progress, message } from 'antd';
import { ArrowLeftOutlined, InfoCircleOutlined, BankOutlined, CheckCircleOutlined, TeamOutlined, UserOutlined, HomeOutlined, BookOutlined, DollarOutlined, CalendarOutlined, NodeIndexOutlined, AuditOutlined, EnterOutlined, SaveOutlined, CheckOutlined, FileAddOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './CreateTaskPage.css';
// ============================================================
// 步骤3 配置项定义
// ============================================================
const CONFIG_GROUPS = [
    {
        title: '人员安排',
        icon: _jsx(TeamOutlined, {}),
        items: [
            { key: 'humanTeacher', title: '师资与教务', desc: '主讲、班主任、跟班与授课资质' },
            { key: 'humanStudent', title: '参训学员', desc: '学员名单、企业确认与资格审核' },
        ],
    },
    {
        title: '资源与场地',
        icon: _jsx(HomeOutlined, {}),
        items: [
            { key: 'machine', title: '机构与场地', desc: '合作机构、讲师资源、场地设备与占用' },
        ],
    },
    {
        title: '课程与经费',
        icon: _jsx(BookOutlined, {}),
        items: [
            { key: 'materialCourse', title: '课程与课件', desc: '课纲、课件课包及版本审批' },
            { key: 'materialCost', title: '预算与费用', desc: '预算科目、结算分成与回款' },
        ],
    },
    {
        title: '计划与制度',
        icon: _jsx(CalendarOutlined, {}),
        items: [
            { key: 'lawSchedule', title: '分班排课', desc: '分班、排课、课表与冲突检测' },
            { key: 'lawMode', title: '培养方案', desc: '培养路径与规划方案版本' },
            { key: 'lawRule', title: '业务规则', desc: '分班、考勤等业务策略' },
        ],
    },
    {
        title: '开班准备',
        icon: _jsx(EnterOutlined, {}),
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
    return (_jsxs("div", { className: "config-form-grid", children: [_jsx(Form.Item, { label: "\u4E3B\u8BB2\u8BB2\u5E08", name: ['config', 'humanTeacher', 'mainTeacher'], children: _jsx(Input, { placeholder: "\u59D3\u540D" }) }), _jsx(Form.Item, { label: "\u52A9\u6559", name: ['config', 'humanTeacher', 'assistant'], children: _jsx(Input, { placeholder: "\u53EF\u9009" }) }), _jsx(Form.Item, { label: "\u73ED\u4E3B\u4EFB", name: ['config', 'humanTeacher', 'headTeacher'], children: _jsx(Input, { placeholder: "\u59D3\u540D" }) }), _jsx(Form.Item, { label: "\u8DDF\u73ED\u6559\u52A1", name: ['config', 'humanTeacher', 'followStaff'], children: _jsx(Input, { placeholder: "\u59D3\u540D" }) }), _jsx(Form.Item, { label: "\u8D44\u8D28\u72B6\u6001", name: ['config', 'humanTeacher', 'qualification'], initialValue: "\u6709\u6548", children: _jsx(Select, { options: ['有效', '即将到期', '待补充'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u6D3E\u5E08\u9501\u5B9A", name: ['config', 'humanTeacher', 'dispatchLock'], initialValue: "\u662F", children: _jsx(Select, { options: ['是', '否'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u5907\u6CE8", name: ['config', 'humanTeacher', 'remark'], className: "full", children: _jsx(Input.TextArea, { placeholder: "\u8D44\u8D28\u8BF4\u660E\u3001\u627F\u8BFA\u4E66\u7F16\u53F7\u7B49", rows: 3 }) })] }));
}
function ConfigPanelHumanStudent() {
    return (_jsxs(_Fragment, { children: [_jsxs("div", { className: "config-form-grid", children: [_jsx(Form.Item, { label: "\u5E94\u5230\u4EBA\u6570", name: ['config', 'humanStudent', 'expectedCount'], initialValue: 40, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u5DF2\u786E\u8BA4\u4EBA\u6570", name: ['config', 'humanStudent', 'confirmedCount'], initialValue: 38, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u4F01\u4E1A\u786E\u8BA4\u72B6\u6001", name: ['config', 'humanStudent', 'corpConfirm'], initialValue: "\u5DF2\u786E\u8BA4", children: _jsx(Select, { options: ['已确认', '部分确认', '待确认'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u8D44\u683C\u5BA1\u6838", name: ['config', 'humanStudent', 'qualReview'], initialValue: "\u901A\u8FC7", children: _jsx(Select, { options: ['通过', '待补充', '审核中'].map((v) => ({ value: v, label: v })) }) })] }), _jsx("div", { style: { fontSize: 13, fontWeight: 500, marginTop: 16 }, children: "\u5B66\u5458\u540D\u518C" }), _jsx(Table, { dataSource: MOCK_STUDENTS.map((s, i) => ({ ...s, key: i })), pagination: false, size: "small", bordered: true, style: { marginTop: 8 }, columns: [
                    { title: '姓名', dataIndex: 'name' },
                    { title: '身份证', dataIndex: 'idCard' },
                    { title: '工种', dataIndex: 'occupation' },
                    { title: '状态', dataIndex: 'status' },
                ] }), _jsx(Button, { icon: _jsx(FileAddOutlined, {}), style: { marginTop: 12 }, children: "\u5BFC\u5165\u540D\u5355" })] }));
}
function ConfigPanelMachine() {
    return (_jsxs("div", { className: "config-form-grid", children: [_jsx(Form.Item, { label: "\u5408\u4F5C\u57F9\u8BAD\u673A\u6784", name: ['config', 'machine', 'orgName'], children: _jsx(Input, { placeholder: "\u673A\u6784\u5168\u79F0" }) }), _jsx(Form.Item, { label: "\u57F9\u8BAD\u573A\u5730", name: ['config', 'machine', 'venue'], children: _jsx(Input, { placeholder: "\u5B9E\u8BAD\u57FA\u5730 / \u6559\u5BA4" }) }), _jsx(Form.Item, { label: "\u573A\u5730\u5BB9\u91CF", name: ['config', 'machine', 'capacity'], children: _jsx(InputNumber, { min: 0, placeholder: "\u4EBA", style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u8BBE\u5907\u72B6\u6001", name: ['config', 'machine', 'equipment'], initialValue: "\u9F50\u5168", children: _jsx(Select, { options: ['齐全', '部分到位', '待落实'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u5360\u7528\u65F6\u6BB5", name: ['config', 'machine', 'occupyTime'], children: _jsx(Input, { placeholder: "\u5982 5\u670812\u65E5-6\u670830\u65E5" }) }), _jsx(Form.Item, { label: "\u51B2\u7A81\u68C0\u6D4B", name: ['config', 'machine', 'conflict'], initialValue: "\u65E0\u51B2\u7A81", children: _jsx(Select, { options: ['无冲突', '有冲突待协调'].map((v) => ({ value: v, label: v })) }) })] }));
}
function ConfigPanelMaterialCourse() {
    return (_jsxs("div", { className: "config-form-grid", children: [_jsx(Form.Item, { label: "\u8BFE\u7A0B\u540D\u79F0", name: ['config', 'materialCourse', 'courseName'], children: _jsx(Input, { placeholder: "\u5982\uFF1A\u7535\u5DE5\u5B89\u5168\u6280\u672F" }) }), _jsx(Form.Item, { label: "\u8BFE\u7EB2\u7248\u672C", name: ['config', 'materialCourse', 'syllabusVer'], initialValue: "v2026.1", children: _jsx(Input, {}) }), _jsx(Form.Item, { label: "\u8BFE\u4EF6\u8BFE\u5305", name: ['config', 'materialCourse', 'courseware'], children: _jsx(Input, { placeholder: "\u8BFE\u4EF6\u5305\u540D\u79F0" }) }), _jsx(Form.Item, { label: "\u5BA1\u6279\u72B6\u6001", name: ['config', 'materialCourse', 'approveStatus'], initialValue: "\u5DF2\u5BA1\u6279", children: _jsx(Select, { options: ['已审批', '审批中', '草稿'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u4EA4\u4ED8\u8BF4\u660E", name: ['config', 'materialCourse', 'deliveryNote'], className: "full", children: _jsx(Input.TextArea, { placeholder: "\u7406\u8BBA/\u5B9E\u64CD\u8BFE\u65F6\u5206\u914D\u7B49", rows: 3 }) })] }));
}
function ConfigPanelMaterialCost() {
    return (_jsxs(_Fragment, { children: [_jsxs("div", { className: "config-form-grid", children: [_jsx(Form.Item, { label: "\u9884\u7B97\u603B\u989D\uFF08\u5143\uFF09", name: ['config', 'materialCost', 'budgetTotal'], initialValue: 280000, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u5DF2\u62E8\u4ED8\uFF08\u5143\uFF09", name: ['config', 'materialCost', 'paidAmount'], initialValue: 120000, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u4F01\u4E1A\u5206\u6210\u6BD4\u4F8B", name: ['config', 'materialCost', 'corpShare'], initialValue: "30%", children: _jsx(Input, {}) }), _jsx(Form.Item, { label: "\u673A\u6784\u5206\u6210\u6BD4\u4F8B", name: ['config', 'materialCost', 'orgShare'], initialValue: "70%", children: _jsx(Input, {}) })] }), _jsx(Table, { dataSource: MOCK_BUDGET_ITEMS.map((r, i) => ({ ...r, key: i })), pagination: false, size: "small", bordered: true, style: { marginTop: 16 }, columns: [
                    { title: '科目', dataIndex: 'item' },
                    { title: '金额（元）', dataIndex: 'amount' },
                    { title: '说明', dataIndex: 'note' },
                ] })] }));
}
function ConfigPanelLawSchedule() {
    return (_jsxs(_Fragment, { children: [_jsxs("div", { className: "config-form-grid", children: [_jsx(Form.Item, { label: "\u73ED\u7EA7\u540D\u79F0", name: ['config', 'lawSchedule', 'className'], children: _jsx(Input, { placeholder: "\u5982\uFF1A\u7535\u5DE5\u9AD8\u7EA7\u73ED\u7B2C3\u671F" }) }), _jsx(Form.Item, { label: "\u8BA1\u5212\u5F00\u73ED", name: ['config', 'lawSchedule', 'classStart'], children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u5468\u8BFE\u65F6", name: ['config', 'lawSchedule', 'weeklyHours'], initialValue: 24, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u6392\u8BFE\u5B8C\u6210\u7387", name: ['config', 'lawSchedule', 'scheduleRate'], initialValue: "96%", children: _jsx(Input, {}) })] }), _jsx(Table, { dataSource: MOCK_SCHEDULE.map((r, i) => ({ ...r, key: i })), pagination: false, size: "small", bordered: true, style: { marginTop: 16 }, columns: [
                    { title: '课程', dataIndex: 'course' },
                    { title: '讲师', dataIndex: 'teacher' },
                    { title: '时间', dataIndex: 'time' },
                ] })] }));
}
function ConfigPanelLawMode() {
    return (_jsxs("div", { className: "config-form-grid", children: [_jsx(Form.Item, { label: "\u57F9\u517B\u6A21\u5F0F", name: ['config', 'lawMode', 'trainMode'], initialValue: "\u96C6\u4E2D\u57F9\u8BAD", children: _jsx(Select, { options: ['集中培训', '工学结合', '线上+线下'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u65B9\u6848\u7248\u672C", name: ['config', 'lawMode', 'planVersion'], initialValue: "2026-Q2-V1", children: _jsx(Input, {}) }), _jsx(Form.Item, { label: "\u57F9\u517B\u8DEF\u5F84\u8BF4\u660E", name: ['config', 'lawMode', 'pathDesc'], className: "full", children: _jsx(Input.TextArea, { placeholder: "\u9636\u6BB5\u76EE\u6807\u3001\u8003\u6838\u8282\u70B9\u2026", rows: 3 }) })] }));
}
function ConfigPanelLawRule() {
    return (_jsxs(_Fragment, { children: [_jsxs("ul", { className: "check-list", children: [_jsx("li", { children: _jsx(Form.Item, { name: ['config', 'lawRule', 'ruleAttendance'], valuePropName: "checked", initialValue: true, noStyle: true, children: _jsx(Checkbox, { children: "\u542F\u7528\u8003\u52E4\u6253\u5361\u89C4\u5219\uFF08\u6BCF\u65E52\u6B21\uFF09" }) }) }), _jsx("li", { children: _jsx(Form.Item, { name: ['config', 'lawRule', 'ruleLate'], valuePropName: "checked", initialValue: true, noStyle: true, children: _jsx(Checkbox, { children: "\u8FDF\u5230\u8D85\u8FC73\u6B21\u89E6\u53D1\u9884\u8B66" }) }) }), _jsx("li", { children: _jsx(Form.Item, { name: ['config', 'lawRule', 'ruleClass'], valuePropName: "checked", initialValue: true, noStyle: true, children: _jsx(Checkbox, { children: "\u6309\u5DE5\u79CD\u81EA\u52A8\u5206\u73ED" }) }) }), _jsx("li", { children: _jsx(Form.Item, { name: ['config', 'lawRule', 'ruleExam'], valuePropName: "checked", noStyle: true, children: _jsx(Checkbox, { children: "\u7ED3\u4E1A\u524D\u5F3A\u5236\u6A21\u62DF\u8003\u8BD5" }) }) })] }), _jsx(Form.Item, { label: "\u81EA\u5B9A\u4E49\u89C4\u5219\u8BF4\u660E", name: ['config', 'lawRule', 'ruleCustom'], style: { marginTop: 16 }, children: _jsx(Input.TextArea, { placeholder: "\u5176\u4ED6\u4E1A\u52A1\u7EA6\u675F\u2026", rows: 3 }) })] }));
}
function ConfigPanelEnvironment() {
    return (_jsxs(_Fragment, { children: [_jsxs("div", { className: "config-form-grid", children: [_jsx(Form.Item, { label: "\u8BA1\u5212\u5F00\u73ED\u65E5\u671F", name: ['config', 'environment', 'openDate'], children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u5F00\u73ED\u5730\u70B9", name: ['config', 'environment', 'openPlace'], children: _jsx(Input, { placeholder: "\u8BE6\u7EC6\u5730\u5740" }) }), _jsx(Form.Item, { label: "\u573A\u5730\u843D\u5B9E", name: ['config', 'environment', 'venueReady'], initialValue: "\u5DF2\u843D\u5B9E", children: _jsx(Select, { options: ['已落实', '待确认'].map((v) => ({ value: v, label: v })) }) })] }), _jsx("div", { style: { fontSize: 13, fontWeight: 500, margin: '16px 0 10px' }, children: "\u4EBA\u793E\u6750\u6599\u6E05\u5355" }), _jsxs("ul", { className: "check-list", children: [_jsx("li", { children: _jsx(Form.Item, { name: ['config', 'environment', 'docPlan'], valuePropName: "checked", initialValue: true, noStyle: true, children: _jsx(Checkbox, { children: "\u57F9\u8BAD\u8BA1\u5212\u5907\u6848" }) }) }), _jsx("li", { children: _jsx(Form.Item, { name: ['config', 'environment', 'docRoster'], valuePropName: "checked", initialValue: true, noStyle: true, children: _jsx(Checkbox, { children: "\u5B66\u5458\u540D\u518C" }) }) }), _jsx("li", { children: _jsx(Form.Item, { name: ['config', 'environment', 'docVenue'], valuePropName: "checked", noStyle: true, children: _jsx(Checkbox, { children: "\u573A\u5730\u8BC1\u660E" }) }) }), _jsx("li", { children: _jsx(Form.Item, { name: ['config', 'environment', 'docContract'], valuePropName: "checked", noStyle: true, children: _jsx(Checkbox, { children: "\u57F9\u8BAD\u5408\u540C" }) }) })] })] }));
}
const PANEL_MAP = {
    humanTeacher: { icon: _jsx(UserOutlined, {}), title: '师资与教务', desc: '指派主讲、助教、班主任与跟班，绑定授课资质与承诺书', component: _jsx(ConfigPanelHumanTeacher, {}) },
    humanStudent: { icon: _jsx(TeamOutlined, {}), title: '参训学员', desc: '学员名单导入、企业确认与资格审核', component: _jsx(ConfigPanelHumanStudent, {}) },
    machine: { icon: _jsx(HomeOutlined, {}), title: '机构与场地', desc: '合作机构、讲师资源池、场地设备与占用冲突检测', component: _jsx(ConfigPanelMachine, {}) },
    materialCourse: { icon: _jsx(BookOutlined, {}), title: '课程与课件', desc: '课纲、课件课包版本与审批状态', component: _jsx(ConfigPanelMaterialCourse, {}) },
    materialCost: { icon: _jsx(DollarOutlined, {}), title: '预算与费用', desc: '预算科目、结算分成与回款跟踪', component: _jsx(ConfigPanelMaterialCost, {}) },
    lawSchedule: { icon: _jsx(CalendarOutlined, {}), title: '分班排课', desc: '分班、排课、课表发布与冲突检测', component: _jsx(ConfigPanelLawSchedule, {}) },
    lawMode: { icon: _jsx(NodeIndexOutlined, {}), title: '培养方案', desc: '培养路径与规划方案版本绑定', component: _jsx(ConfigPanelLawMode, {}) },
    lawRule: { icon: _jsx(AuditOutlined, {}), title: '业务规则', desc: '分班、考勤等业务策略配置', component: _jsx(ConfigPanelLawRule, {}) },
    environment: { icon: _jsx(EnterOutlined, {}), title: '开班与合规', desc: '开班条件、场地落实与人社材料准备', component: _jsx(ConfigPanelEnvironment, {}) },
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
    const [configuredKeys, setConfiguredKeys] = useState(new Set());
    const configDoneCount = configuredKeys.size;
    const configTotal = ALL_CONFIG_ITEMS.length;
    const configPct = Math.round((configDoneCount / configTotal) * 100);
    const markConfigured = useCallback((key) => {
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
    const goStep = useCallback((n) => {
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
    }, [currentStep, form]);
    const handleSubmit = () => {
        form.validateFields().then((values) => {
            console.log('提交数据:', values);
            message.success('任务已创建，可在任务档案中查看');
        }).catch(() => message.warning('请完善必填项'));
    };
    const panel = PANEL_MAP[activeConfigTab];
    return (_jsxs("div", { className: "create-task-page", children: [_jsxs("header", { className: "ct-topbar", children: [_jsxs("div", { className: "ct-topbar-left", children: [_jsxs("a", { className: "ct-back-link", onClick: handleBack, children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u4EFB\u52A1\u7BA1\u7406"] }), _jsxs("div", { children: [_jsx("h1", { className: "ct-title", children: "\u521B\u5EFA\u57F9\u8BAD\u4EFB\u52A1" }), _jsx("span", { className: "ct-sub", children: "\u57FA\u672C\u4FE1\u606F \u2192 \u9879\u76EE\u5173\u8054 \u2192 \u4EFB\u52A1\u914D\u7F6E" })] })] }), _jsx("span", { className: "ct-task-code", children: "\u65B0\u5EFA\u4EFB\u52A1" })] }), _jsxs("main", { className: `ct-page${currentStep === 2 ? ' ct-page-wide' : ''}`, children: [_jsx("nav", { className: "ct-steps-bar", children: ['基本信息', '项目关联', '任务配置', '确认提交'].map((label, i) => (_jsxs("button", { className: `ct-step-pill${currentStep === i ? ' active' : ''}${i < currentStep ? ' done' : ''}`, onClick: () => goStep(i), children: [_jsxs("span", { className: "num", children: ["\u6B65\u9AA4 ", i + 1] }), label] }, i))) }), _jsxs(Form, { form: form, layout: "vertical", onFinish: handleSubmit, children: [currentStep === 0 && (_jsx("section", { className: "ct-section", children: _jsx(Card, { title: _jsxs(_Fragment, { children: [_jsx(InfoCircleOutlined, { style: { color: '#2A6DFF' } }), " \u4EFB\u52A1\u57FA\u672C\u4FE1\u606F"] }), className: "ct-card", children: _jsxs("div", { className: "ct-form-grid", children: [_jsx(Form.Item, { label: "\u4EFB\u52A1\u540D\u79F0", name: "taskName", rules: [{ required: true, message: '请填写' }], className: "full", children: _jsx(Input, { placeholder: "\u5982\uFF1A2026\u5E74\u5357\u5B81\u7535\u5DE5\u9AD8\u7EA7\u73ED\u57F9\u8BAD\u4EFB\u52A1" }) }), _jsx(Form.Item, { label: "\u8D1F\u8D23\u4EBA", name: "taskOwner", rules: [{ required: true, message: '请填写' }], children: _jsx(Input, { placeholder: "\u9879\u76EE\u7ECF\u7406/\u57F9\u8BAD\u4E13\u5458" }) }), _jsx(Form.Item, { label: "\u622A\u6B62\u65E5\u671F", name: "taskDeadline", rules: [{ required: true, message: '请选择' }], children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u4F18\u5148\u7EA7", name: "taskPriority", initialValue: "\u4E2D", children: _jsx(Select, { options: ['高', '中', '低'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u4EFB\u52A1\u72B6\u6001", name: "taskStatus", initialValue: "\u8349\u7A3F", children: _jsx(Select, { options: ['草稿', '进行中', '待审批'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u4EFB\u52A1\u8BF4\u660E", name: "taskDesc", className: "full", children: _jsx(Input.TextArea, { placeholder: "\u57F9\u8BAD\u76EE\u6807\u3001\u4EA4\u4ED8\u8981\u6C42\u3001\u5907\u6CE8\u2026", rows: 4 }) })] }) }) })), currentStep === 1 && (_jsx("section", { className: "ct-section", children: _jsx(Card, { title: _jsxs(_Fragment, { children: [_jsx(BankOutlined, { style: { color: '#2A6DFF' } }), " \u9879\u76EE\u5173\u8054\u4FE1\u606F"] }), extra: _jsx("span", { style: { fontSize: 12, color: '#64748b' }, children: "\u5BF9\u5E94\u7ACB\u9879\u8868 / \u9879\u76EE\u521B\u5EFA\u5B57\u6BB5" }), className: "ct-card", children: _jsxs("div", { className: "ct-form-grid", children: [_jsx(Form.Item, { label: "\u59D4\u6258\u4F01\u4E1A", name: "enterprise", rules: [{ required: true, message: '请填写' }], children: _jsx(Input, { placeholder: "\u4F01\u4E1A\u5168\u79F0" }) }), _jsx(Form.Item, { label: "\u6240\u5C5E\u533A\u57DF\uFF08\u5E7F\u897F\uFF09", name: "region", initialValue: "\u5357\u5B81", children: _jsx(Select, { options: ['南宁', '桂林', '柳州', '玉林', '梧州', '其他'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u57F9\u8BAD\u5DE5\u79CD", name: "occupation", rules: [{ required: true, message: '请选择' }], children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9", options: ['电工', '焊工', '起重工', '架子工', '安全员'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u6280\u80FD\u7B49\u7EA7", name: "skillLevel", initialValue: "\u4E2D\u7EA7", children: _jsx(Select, { options: ['初级', '中级', '高级', '技师'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u9879\u76EE\u7C7B\u578B", name: "projectType", initialValue: "\u8D44\u8D28\u8BA4\u8BC1", children: _jsx(Select, { options: ['新员工培训', '晋升培训', '资质认证', '安全培训', '管理研修'].map((v) => ({ value: v, label: v })) }) }), _jsx(Form.Item, { label: "\u8BA1\u5212\u4EBA\u6570", name: "planCount", initialValue: 40, children: _jsx(InputNumber, { min: 1, placeholder: "\u5982\uFF1A42", style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u8BA1\u5212\u5F00\u59CB", name: "startDate", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u8BA1\u5212\u7ED3\u675F", name: "endDate", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u57F9\u8BAD\u9700\u6C42\u6458\u8981", name: "trainingNeed", className: "full", children: _jsx(Input.TextArea, { placeholder: "\u4F01\u4E1A\u9700\u6C42\u3001\u8865\u8D34\u7533\u62A5\u610F\u5411\u3001\u7279\u6B8A\u8981\u6C42\u2026", rows: 4 }) })] }) }) })), currentStep === 2 && (_jsxs("section", { className: "ct-section", children: [_jsxs("div", { className: "ct-config-header", children: [_jsxs("div", { children: [_jsx("h2", { style: { fontSize: 18, fontWeight: 700, marginBottom: 4 }, children: "\u4EFB\u52A1\u914D\u7F6E" }), _jsx("p", { style: { fontSize: 13, color: '#64748b', margin: 0 }, children: "\u9010\u9879\u5B8C\u6210\u4EBA\u5458\u3001\u8D44\u6E90\u3001\u8BFE\u7A0B\u3001\u8BA1\u5212\u4E0E\u5F00\u73ED\u51C6\u5907\u914D\u7F6E" })] }), _jsxs("div", { className: "ct-config-progress", children: [_jsxs("div", { className: "ct-config-progress-label", children: [_jsx("span", { children: "\u914D\u7F6E\u5B8C\u6210\u5EA6" }), _jsxs("span", { children: [configDoneCount, " / ", configTotal] })] }), _jsx(Progress, { percent: configPct, showInfo: false, size: "small", strokeColor: { from: '#2A6DFF', to: '#5B8FFF' }, trailColor: "#e2e8f0" })] })] }), _jsxs("div", { className: "ct-config-layout", children: [_jsx("nav", { className: "ct-config-sidebar", children: CONFIG_GROUPS.map((group) => (_jsxs("div", { children: [_jsx("div", { className: "ct-config-nav-group", children: group.title }), group.items.map((item) => (_jsxs("button", { type: "button", className: `ct-config-nav-item${activeConfigTab === item.key ? ' active' : ''}${configuredKeys.has(item.key) ? ' done' : ''}`, onClick: () => setActiveConfigTab(item.key), children: [_jsx("span", { className: "nav-dot" }), item.title] }, item.key)))] }, group.title))) }), _jsx("div", { className: "ct-config-main", children: _jsxs("div", { className: "ct-config-panel active", children: [_jsxs("div", { className: "ct-config-panel-head", children: [_jsxs("h3", { children: [panel.icon, " ", panel.title] }), _jsx("p", { children: panel.desc })] }), panel.component, _jsxs("div", { className: "ct-panel-actions", children: [_jsx("span", { className: "hint", children: "\u586B\u5199\u540E\u70B9\u51FB\u4FDD\u5B58\uFF0C\u53EF\u5207\u6362\u5176\u4ED6\u914D\u7F6E\u9879" }), _jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), onClick: () => markConfigured(activeConfigTab), children: "\u4FDD\u5B58\u672C\u8282" })] })] }, activeConfigTab) })] })] })), currentStep === 3 && (_jsx("section", { className: "ct-section", children: _jsx(Card, { title: _jsxs(_Fragment, { children: [_jsx(CheckCircleOutlined, { style: { color: '#2A6DFF' } }), " \u786E\u8BA4\u5E76\u63D0\u4EA4"] }), className: "ct-card", children: _jsxs("div", { style: { fontSize: 14, lineHeight: 1.8, color: '#64748b' }, children: [_jsxs("p", { children: [_jsx("strong", { children: "\u4EFB\u52A1\u540D\u79F0\uFF1A" }), form.getFieldValue('taskName') || '—'] }), _jsxs("p", { children: [_jsx("strong", { children: "\u8D1F\u8D23\u4EBA\uFF1A" }), form.getFieldValue('taskOwner') || '—', " \u00B7 ", _jsx("strong", { children: "\u622A\u6B62\uFF1A" }), form.getFieldValue('taskDeadline')?.format?.('YYYY-MM-DD') || '—'] }), _jsxs("p", { children: [_jsx("strong", { children: "\u4F01\u4E1A / \u5DE5\u79CD\uFF1A" }), form.getFieldValue('enterprise') || '—', " \u00B7 ", form.getFieldValue('occupation') || '—', " \u00B7 ", form.getFieldValue('skillLevel') || '—'] }), _jsxs("p", { children: [_jsx("strong", { children: "\u533A\u57DF\uFF1A" }), form.getFieldValue('region') || '—', " \u00B7 ", _jsx("strong", { children: "\u7C7B\u578B\uFF1A" }), form.getFieldValue('projectType') || '—'] }), _jsxs("p", { children: [_jsx("strong", { children: "\u914D\u7F6E\u5B8C\u6210\u5EA6\uFF1A" }), configDoneCount, " / ", configTotal, " \u9879\uFF08\u53EF\u9009\uFF0C\u63D0\u4EA4\u540E\u53EF\u7EE7\u7EED\u8865\u5145\uFF09"] })] }) }) })), _jsxs("div", { style: { display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 8 }, children: [currentStep > 0 && (_jsx(Button, { onClick: () => goStep(currentStep - 1), children: "\u4E0A\u4E00\u6B65" })), currentStep < 3 && (_jsx(Button, { onClick: () => goStep(currentStep + 1), children: "\u4E0B\u4E00\u6B65" })), currentStep === 3 && (_jsx(Button, { type: "primary", icon: _jsx(CheckOutlined, {}), htmlType: "submit", children: "\u63D0\u4EA4\u4EFB\u52A1" }))] })] })] })] }));
}
