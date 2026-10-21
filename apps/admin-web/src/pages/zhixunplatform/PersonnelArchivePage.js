import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { useState, useMemo } from 'react';
import { Table, Button, Input, Select, Modal, Form, Tag, Space, message, Progress } from 'antd';
import { ArrowLeftOutlined, UserOutlined, PlusOutlined, ImportOutlined, SearchOutlined, EditOutlined, DeleteOutlined, TeamOutlined, IdcardOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import './ArchivePages.css';
/* ============================================================
   模拟数据（对照 archive-data.js）
   ============================================================ */
const PROJECTS = ['电工认证项目', '焊工晋升项目', '起重工安全培训', '安全员岗前培训'];
const MOCK_TEACHERS = [
    { id: 'T001', name: '张建国', gender: '男', role: '主讲', title: '高级', teachLevel: '技师', skills: '电工', project: '电工认证项目', certName: '电工技师职业资格证', certNo: 'GX202401001', certExpiry: '2027-08-15', agreement: '已签署', nda: '已签署', rating: 4.8, phone: '138****1001', status: '在职', hours: 120, settleStatus: '已结算' },
    { id: 'T002', name: '李秀英', gender: '女', role: '主讲', title: '专家', teachLevel: '高级技师', skills: '焊工', project: '焊工晋升项目', certName: '焊工高级技师证', certNo: 'GX202308882', certExpiry: '2026-06-20', agreement: '已签署', nda: '已签署', rating: 4.9, phone: '139****2002', status: '在职', hours: 96, settleStatus: '待结算' },
    { id: 'T003', name: '王德明', gender: '男', role: '助教', title: '中级', teachLevel: '高级工', skills: '起重工', project: '起重工安全培训', certName: '起重机械作业证', certNo: 'GX202205501', certExpiry: '2026-04-01', agreement: '未签署', nda: '已签署', rating: 4.2, phone: '137****3003', status: '兼职', hours: 48, settleStatus: '未结算' },
    { id: 'T004', name: '陈丽华', gender: '女', role: '班主任', title: '高级', teachLevel: '—', skills: '教务管理', project: '电工认证项目', certName: '人力资源管理师', certNo: 'GX202011203', certExpiry: '2028-01-10', agreement: '已签署', nda: '已签署', rating: 4.7, phone: '136****4004', status: '在职', hours: 0, settleStatus: '—' },
    { id: 'T005', name: '刘志强', gender: '男', role: '主讲', title: '初级', teachLevel: '中级工', skills: '安全员', project: '安全员岗前培训', certName: '安全员培训师资证', certNo: 'GX202506001', certExpiry: '2029-03-01', agreement: '已签署', nda: '已签署', rating: 4.0, phone: '135****5005', status: '在职', hours: 64, settleStatus: '已结算' },
    { id: 'T006', name: '赵敏', gender: '女', role: '主讲', title: '中级', teachLevel: '技师', skills: '电工', project: '电工认证项目', certName: '电工技师职业资格证', certNo: 'GX201907766', certExpiry: '2026-05-28', agreement: '审批中', nda: '未签署', rating: 4.5, phone: '134****6006', status: '兼职', hours: 32, settleStatus: '未结算' },
    { id: 'T007', name: '黄伟', gender: '男', role: '跟班', title: '中级', teachLevel: '高级工', skills: '焊工', project: '焊工晋升项目', certName: '焊工高级工证', certNo: 'GX202103445', certExpiry: '2027-11-30', agreement: '已签署', nda: '已签署', rating: 4.3, phone: '133****7007', status: '在职', hours: 80, settleStatus: '待结算' },
];
const MOCK_STUDENTS = [
    { id: 'S1001', name: '陈伟', gender: '男', idCard: '4501**********1234', enterprise: '广西建工集团', trade: '电工', level: '高级工', project: '电工认证项目', className: '电工高级班（第1期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 94, homework: 88, mockScore: 76, warnLevel: '', progress: 78, cert: '培训中' },
    { id: 'S1002', name: '李娜', gender: '女', idCard: '4501**********5678', enterprise: '广西建工集团', trade: '电工', level: '高级工', project: '电工认证项目', className: '电工高级班（第1期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 96, homework: 92, mockScore: 82, warnLevel: '', progress: 85, cert: '培训中' },
    { id: 'S1003', name: '周强', gender: '男', idCard: '4502**********9012', enterprise: '柳州钢铁公司', trade: '焊工', level: '中级工', project: '焊工晋升项目', className: '焊工中级班（第2期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '补正中', attendance: 72, homework: 65, mockScore: 58, warnLevel: '缺勤预警', progress: 62, cert: '培训中' },
    { id: 'S1004', name: '吴芳', gender: '女', idCard: '4503**********3456', enterprise: '桂林旅游股份公司', trade: '焊工', level: '中级工', project: '焊工晋升项目', className: '焊工中级班（第2期）', enrollStatus: '已结业', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 98, homework: 95, mockScore: 88, warnLevel: '', progress: 100, cert: '已结业' },
    { id: 'S1005', name: '郑磊', gender: '男', idCard: '4501**********7890', enterprise: '南宁轨道交通', trade: '起重工', level: '高级工', project: '起重工安全培训', className: '起重工安全班', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 85, homework: 70, mockScore: 62, warnLevel: '', progress: 45, cert: '培训中' },
    { id: 'S1006', name: '孙静', gender: '女', idCard: '4509**********2345', enterprise: '玉林能源公司', trade: '电工', level: '高级工', project: '电工认证项目', className: '电工高级班（第1期）', enrollStatus: '已获证', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 100, homework: 98, mockScore: 91, warnLevel: '', progress: 100, cert: '已获证' },
    { id: 'S1007', name: '马超', gender: '男', idCard: '4502**********6789', enterprise: '柳州钢铁公司', trade: '安全员', level: '岗前', project: '安全员岗前培训', className: '安全员岗前班', enrollStatus: '在培', confirmStatus: '待确认', auditStatus: '待审核', materialStatus: '未提交', attendance: 0, homework: 0, mockScore: 0, warnLevel: '', progress: 0, cert: '未开始' },
    { id: 'S1008', name: '黄丽', gender: '女', idCard: '4501**********1122', enterprise: '广西建工集团', trade: '电工', level: '高级工', project: '电工认证项目', className: '待分班', enrollStatus: '待入班', confirmStatus: '待确认', auditStatus: '待审核', materialStatus: '未提交', attendance: 0, homework: 0, mockScore: 0, warnLevel: '', progress: 0, cert: '未开始' },
    { id: 'S1009', name: '林峰', gender: '男', idCard: '4503**********3344', enterprise: '桂林旅游股份公司', trade: '焊工', level: '中级工', project: '焊工晋升项目', className: '焊工中级班（第2期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '审核中', attendance: 68, homework: 55, mockScore: 52, warnLevel: '综合预警', progress: 56, cert: '培训中' },
    { id: 'S1010', name: '何娟', gender: '女', idCard: '4501**********5566', enterprise: '梧州港务集团', trade: '电工', level: '中级工', project: '电工认证项目', className: '电工高级班（第1期）', enrollStatus: '在培', confirmStatus: '已确认', auditStatus: '已通过', materialStatus: '已齐全', attendance: 78, homework: 72, mockScore: 60, warnLevel: '作业预警', progress: 70, cert: '培训中' },
];
/* ============================================================
   工具函数
   ============================================================ */
function badgeTag(text, type) {
    const colorMap = {
        '已签署': 'success', '已确认': 'success', '已通过': 'success', '已齐全': 'success', '已结算': 'success',
        '在职': 'success', '在培': 'processing', '待确认': 'warning', '待审核': 'warning', '补正中': 'warning',
        '审批中': 'warning', '未签署': 'warning', '未结算': 'warning', '未提交': 'error', '缺勤预警': 'error',
        '综合预警': 'error', '作业预警': 'error', '兼职': 'default',
    };
    const color = colorMap[text] || type || 'default';
    return _jsx(Tag, { color: color, children: text || '—' });
}
function daysUntil(d) {
    if (!d || d === '—')
        return 999;
    const x = new Date(d);
    return isNaN(x.getTime()) ? 999 : Math.ceil((x.getTime() - Date.now()) / 86400000);
}
function isCertWarn(t) { const d = daysUntil(t.certExpiry); return d >= 0 && d <= 90; }
function isAgreePending(t) { return t.status === '兼职' && (t.agreement !== '已签署' || t.nda !== '已签署'); }
function isStudentWarn(s) { return !!s.warnLevel || s.attendance < 80 || s.homework < 70 || s.mockScore < 60; }
/* ============================================================
   PersonnelArchivePage
   ============================================================ */
export function PersonnelArchivePage() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const [teachers, setTeachers] = useState(MOCK_TEACHERS);
    const [students, setStudents] = useState(MOCK_STUDENTS);
    const [activeTab, setActiveTab] = useState('teachers');
    const [searchT, setSearchT] = useState('');
    const [searchS, setSearchS] = useState('');
    const [teacherFilter, setTeacherFilter] = useState('all');
    const [studentFilter, setStudentFilter] = useState('all');
    const [modalOpen, setModalOpen] = useState(false);
    const [modalMode, setModalMode] = useState('add');
    const [editingId, setEditingId] = useState(null);
    const [form] = Form.useForm();
    const filterTeachers = useMemo(() => {
        let list = teachers;
        if (searchT) {
            const q = searchT.toLowerCase();
            list = list.filter(t => [t.id, t.name, t.skills, t.project, t.certName].some(k => String(k).toLowerCase().includes(q)));
        }
        if (teacherFilter === 'certWarn')
            list = list.filter(isCertWarn);
        if (teacherFilter === 'agreement')
            list = list.filter(t => t.status === '兼职' && (t.agreement !== '已签署' || t.nda !== '已签署'));
        if (teacherFilter === 'settle')
            list = list.filter(t => t.settleStatus === '待结算' || t.settleStatus === '未结算');
        return list;
    }, [teachers, searchT, teacherFilter]);
    const filterStudents = useMemo(() => {
        let list = students;
        if (searchS) {
            const q = searchS.toLowerCase();
            list = list.filter(s => [s.id, s.name, s.enterprise, s.trade, s.className].some(k => String(k).toLowerCase().includes(q)));
        }
        if (studentFilter === 'confirm')
            list = list.filter(s => s.confirmStatus === '待确认');
        if (studentFilter === 'audit')
            list = list.filter(s => s.auditStatus === '待审核');
        if (studentFilter === 'material')
            list = list.filter(s => ['未提交', '补正中', '审核中'].includes(s.materialStatus));
        if (studentFilter === 'warn')
            list = list.filter(s => !!s.warnLevel || s.attendance < 80 || s.homework < 70 || s.mockScore < 60);
        return list;
    }, [students, searchS, studentFilter]);
    const kpi = useMemo(() => ({
        certWarn: teachers.filter(isCertWarn).length,
        pendingConfirm: students.filter(s => s.confirmStatus === '待确认').length,
        pendingAudit: students.filter(s => s.auditStatus === '待审核').length,
        warnStudents: students.filter(s => !!s.warnLevel || s.attendance < 80 || s.homework < 70 || s.mockScore < 60).length,
    }), [teachers, students]);
    const openModal = (mode, id) => {
        setModalMode(mode);
        setEditingId(id || null);
        if (mode === 'edit' && id) {
            const entity = activeTab === 'teachers' ? teachers : students;
            const record = entity.find(x => x.id === id);
            if (record)
                form.setFieldsValue(record);
        }
        else {
            form.resetFields();
            if (activeTab === 'teachers')
                form.setFieldsValue({ id: 'T' + Date.now(), gender: '男', role: '主讲', title: '中级', status: '兼职', agreement: '未签署', nda: '未签署', settleStatus: '未结算' });
            else
                form.setFieldsValue({ id: 'S' + Date.now(), confirmStatus: '待确认', auditStatus: '待审核', materialStatus: '未提交', progress: 0, cert: '未开始', enrollStatus: '待入班' });
        }
        setModalOpen(true);
    };
    const handleSave = () => {
        form.validateFields().then((vals) => {
            if (modalMode === 'add') {
                if (activeTab === 'teachers')
                    setTeachers(prev => [...prev, vals]);
                else
                    setStudents(prev => [...prev, vals]);
            }
            else {
                if (activeTab === 'teachers')
                    setTeachers(prev => prev.map(t => t.id === editingId ? { ...t, ...vals } : t));
                else
                    setStudents(prev => prev.map(s => s.id === editingId ? { ...s, ...vals } : s));
            }
            setModalOpen(false);
            message.success(modalMode === 'add' ? '新增成功' : '保存成功');
        });
    };
    const handleDelete = (id) => {
        if (activeTab === 'teachers')
            setTeachers(prev => prev.filter(t => t.id !== id));
        else
            setStudents(prev => prev.filter(s => s.id !== id));
        message.success('已删除');
    };
    const teacherColumns = [
        { title: '编号', dataIndex: 'id', width: 80 },
        { title: '姓名', dataIndex: 'name', render: (v, r) => _jsxs(_Fragment, { children: [_jsx("strong", { children: v }), _jsx("br", {}), _jsx("span", { style: { fontSize: 11, color: '#64748B' }, children: r.phone })] }) },
        { title: '角色', dataIndex: 'role', render: (v) => badgeTag(v) },
        { title: '职称/等级', render: (_, r) => _jsxs(_Fragment, { children: [r.title, _jsx("br", {}), _jsx("span", { style: { fontSize: 11, color: '#64748B' }, children: r.teachLevel })] }) },
        { title: '工种', dataIndex: 'skills' },
        { title: '项目', dataIndex: 'project' },
        { title: '资质', dataIndex: 'certName' },
        { title: '到期', dataIndex: 'certExpiry', render: (v, r) => {
                const d = daysUntil(v);
                if (d < 0)
                    return _jsx(Tag, { color: "error", children: "\u5DF2\u8FC7\u671F" });
                if (d <= 90)
                    return _jsxs(Tag, { color: "warning", children: [v, "\uFF08", d, "\u5929\uFF09"] });
                return v || '—';
            } },
        { title: '协议', dataIndex: 'agreement', render: (v) => badgeTag(v) },
        { title: '承诺', dataIndex: 'nda', render: (v) => badgeTag(v) },
        { title: '课酬', dataIndex: 'settleStatus', render: (v) => badgeTag(v) },
        { title: '评价', dataIndex: 'rating', render: (v) => v ? `${v} ★` : '—' },
        { title: '状态', dataIndex: 'status', render: (v) => badgeTag(v) },
        { title: '操作', width: 120, render: (_, r) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openModal('edit', r.id) }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r.id) })] })) },
    ];
    const studentColumns = [
        { title: '编号', dataIndex: 'id', width: 80 },
        { title: '姓名', dataIndex: 'name', render: (v, r) => _jsxs(_Fragment, { children: [_jsx("strong", { children: v }), _jsx("br", {}), _jsx("span", { style: { fontSize: 11, color: '#64748B' }, children: r.idCard })] }) },
        { title: '企业', dataIndex: 'enterprise' },
        { title: '工种', dataIndex: 'trade' },
        { title: '项目', dataIndex: 'project' },
        { title: '班级', dataIndex: 'className' },
        { title: '参训', dataIndex: 'enrollStatus', render: (v) => badgeTag(v) },
        { title: '确认', dataIndex: 'confirmStatus', render: (v) => badgeTag(v) },
        { title: '审核', dataIndex: 'auditStatus', render: (v) => badgeTag(v) },
        { title: '材料', dataIndex: 'materialStatus', render: (v) => badgeTag(v) },
        { title: '出勤', dataIndex: 'attendance', render: (v) => `${v}%` },
        { title: '作业', dataIndex: 'homework', render: (v) => `${v}%` },
        { title: '模拟', dataIndex: 'mockScore', render: (v) => v || '—' },
        { title: '预警', dataIndex: 'warnLevel', render: (v) => v ? _jsx(Tag, { color: "error", children: v }) : '—' },
        { title: '进度', dataIndex: 'progress', render: (v) => _jsx(Progress, { percent: v, size: "small" }) },
        { title: '证书', dataIndex: 'cert', render: (v) => badgeTag(v) },
        { title: '操作', width: 120, render: (_, r) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openModal('edit', r.id) }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r.id) })] })) },
    ];
    const chips = activeTab === 'teachers' ? [
        { key: 'all', label: '全部', count: teachers.length },
        { key: 'certWarn', label: '资质即将到期', count: kpi.certWarn },
        { key: 'agreement', label: '协议待签署', count: teachers.filter(t => t.status === '兼职' && (t.agreement !== '已签署' || t.nda !== '已签署')).length },
        { key: 'settle', label: '课酬待结算', count: teachers.filter(t => t.settleStatus === '待结算' || t.settleStatus === '未结算').length },
    ] : [
        { key: 'all', label: '全部', count: students.length },
        { key: 'confirm', label: '待企业确认', count: kpi.pendingConfirm },
        { key: 'audit', label: '待资格审核', count: kpi.pendingAudit },
        { key: 'material', label: '材料待办', count: students.filter(s => ['未提交', '补正中', '审核中'].includes(s.materialStatus)).length },
        { key: 'warn', label: '过程预警', count: kpi.warnStudents },
    ];
    return (_jsxs("div", { className: "pa-page", children: [_jsxs("header", { className: "pa-topbar", children: [_jsxs("div", { className: "pa-topbar-left", children: [_jsxs("a", { className: "pa-back-link", onClick: () => navigateTo('zhixun-project-archives'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u9879\u76EE\u6863\u6848"] }), _jsxs("div", { children: [_jsx("h1", { children: "\u4EBA\u5458\u6863\u6848" }), _jsx("span", { className: "pa-sub", children: "5M\u00B7\u4EBA\uFF1A\u5E08\u8D44\u4E0E\u6559\u52A1 + \u53C2\u8BAD\u5B66\u5458" })] })] }), _jsxs("span", { className: "pa-topbar-tag", children: [_jsx(TeamOutlined, {}), " \u9879\u76EE\u6863\u6848"] })] }), _jsxs("main", { className: "pa-main", children: [_jsxs("div", { className: "pa-stat-row", children: [_jsxs("div", { className: "pa-stat-card", children: [_jsx("div", { className: "pa-stat-val", children: teachers.length }), _jsx("div", { className: "pa-stat-lbl", children: "\u5728\u518C\u8BB2\u5E08" })] }), _jsxs("div", { className: `pa-stat-card${kpi.certWarn ? ' warn' : ''}`, children: [_jsx("div", { className: "pa-stat-val", children: kpi.certWarn }), _jsx("div", { className: "pa-stat-lbl", children: "\u8D44\u8D2890\u5929\u5185\u5230\u671F" })] }), _jsxs("div", { className: "pa-stat-card", children: [_jsx("div", { className: "pa-stat-val", children: students.length }), _jsx("div", { className: "pa-stat-lbl", children: "\u53C2\u8BAD\u5B66\u5458" })] }), _jsxs("div", { className: `pa-stat-card${kpi.pendingConfirm ? ' warn' : ''}`, children: [_jsx("div", { className: "pa-stat-val", children: kpi.pendingConfirm }), _jsx("div", { className: "pa-stat-lbl", children: "\u5F85\u4F01\u4E1A\u786E\u8BA4" })] }), _jsxs("div", { className: `pa-stat-card${kpi.pendingAudit ? ' warn' : ''}`, children: [_jsx("div", { className: "pa-stat-val", children: kpi.pendingAudit }), _jsx("div", { className: "pa-stat-lbl", children: "\u5F85\u8D44\u683C\u5BA1\u6838" })] }), _jsxs("div", { className: `pa-stat-card${kpi.warnStudents ? ' danger' : ''}`, children: [_jsx("div", { className: "pa-stat-val", children: kpi.warnStudents }), _jsx("div", { className: "pa-stat-lbl", children: "\u8FC7\u7A0B\u9884\u8B66\u5B66\u5458" })] })] }), _jsxs("div", { className: "pa-hint-card", children: [_jsx("strong", { children: "\u4EBA\u673A\u6599\u6CD5\u73AF \u00B7 \u4EBA" }), "\uFF1A\u8BB2\u5E08\u8D44\u8D28/\u534F\u8BAE/\u8BFE\u916C\uFF1B\u5B66\u5458\u540D\u5355\u786E\u8BA4\u3001\u8D44\u683C\u5BA1\u6838\u3001\u6750\u6599\u5BA1\u6838\u4E0E\u8FC7\u7A0B\u9884\u8B66\u3002"] }), _jsxs("div", { className: "pa-tab-bar", children: [_jsxs("button", { className: `pa-tab-btn${activeTab === 'teachers' ? ' active' : ''}`, onClick: () => setActiveTab('teachers'), children: [_jsx(IdcardOutlined, {}), " \u5E08\u8D44\u4E0E\u6559\u52A1"] }), _jsxs("button", { className: `pa-tab-btn${activeTab === 'students' ? ' active' : ''}`, onClick: () => setActiveTab('students'), children: [_jsx(UserOutlined, {}), " \u53C2\u8BAD\u5B66\u5458"] })] }), _jsxs("div", { className: "pa-card", children: [_jsx("div", { className: "pa-card-head", children: _jsxs("div", { className: "pa-title-wrap", children: [activeTab === 'teachers' ? _jsx(IdcardOutlined, {}) : _jsx(UserOutlined, {}), activeTab === 'teachers' ? '讲师档案与资质' : '培训人员台账'] }) }), _jsx("div", { className: "pa-filter-chips", children: chips.map(c => (_jsxs("button", { className: `pa-filter-chip${(activeTab === 'teachers' ? teacherFilter : studentFilter) === c.key ? ' active' : ''}`, onClick: () => activeTab === 'teachers' ? setTeacherFilter(c.key) : setStudentFilter(c.key), children: [c.label, _jsx("span", { className: "pa-chip-count", children: c.count })] }, c.key))) }), _jsxs("div", { className: "pa-toolbar", children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: activeTab === 'teachers' ? '搜索姓名、工种、项目…' : '搜索姓名、企业、班级…', value: activeTab === 'teachers' ? searchT : searchS, onChange: e => activeTab === 'teachers' ? setSearchT(e.target.value) : setSearchS(e.target.value), className: "pa-search-input", allowClear: true }), _jsxs("div", { className: "pa-toolbar-actions", children: [_jsx(Button, { icon: _jsx(ImportOutlined, {}), children: activeTab === 'teachers' ? '导入讲师池' : '导入名单' }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => openModal('add'), children: activeTab === 'teachers' ? '新增讲师' : '新增学员' })] })] }), _jsx("div", { className: "pa-table-wrap", children: _jsx(Table, { columns: (activeTab === 'teachers' ? teacherColumns : studentColumns), dataSource: (activeTab === 'teachers' ? filterTeachers : filterStudents), rowKey: "id", size: "small", scroll: { x: activeTab === 'teachers' ? 1400 : 1700 }, pagination: { defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showTotal: t => `共 ${t} 条` }, rowClassName: activeTab === 'teachers'
                                        ? ((r) => (isCertWarn(r) || isAgreePending(r)) ? 'row-warn' : '')
                                        : ((r) => isStudentWarn(r) ? 'row-warn' : '') }) })] })] }), _jsx(Modal, { title: `${modalMode === 'add' ? '新增' : '编辑'}${activeTab === 'teachers' ? '讲师' : '学员'}`, open: modalOpen, onOk: handleSave, onCancel: () => setModalOpen(false), width: 640, destroyOnHidden: true, children: _jsx(Form, { form: form, layout: "vertical", children: activeTab === 'teachers' ? (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "id", label: "\u7F16\u53F7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "name", label: "\u59D3\u540D", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "gender", label: "\u6027\u522B", children: _jsx(Select, { options: [{ label: '男', value: '男' }, { label: '女', value: '女' }] }) }), _jsx(Form.Item, { name: "role", label: "\u89D2\u8272", children: _jsx(Select, { options: ['主讲', '助教', '班主任', '跟班'].map(v => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "title", label: "\u804C\u79F0", children: _jsx(Select, { options: ['初级', '中级', '高级', '专家'].map(v => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "teachLevel", label: "\u6388\u8BFE\u7B49\u7EA7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "skills", label: "\u5DE5\u79CD", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "project", label: "\u9879\u76EE", children: _jsx(Select, { options: PROJECTS.map(v => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "certName", label: "\u8D44\u8D28\u540D\u79F0", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "certExpiry", label: "\u5230\u671F\u65E5", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "agreement", label: "\u534F\u8BAE", children: _jsx(Select, { options: ['已签署', '审批中', '未签署'].map(v => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "nda", label: "\u4FDD\u5BC6\u627F\u8BFA", children: _jsx(Select, { options: ['已签署', '未签署'].map(v => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "phone", label: "\u7535\u8BDD", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: ['在职', '兼职'].map(v => ({ label: v, value: v })) }) })] })) : (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "id", label: "\u7F16\u53F7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "name", label: "\u59D3\u540D", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "enterprise", label: "\u4F01\u4E1A", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "trade", label: "\u5DE5\u79CD", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "className", label: "\u73ED\u7EA7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "confirmStatus", label: "\u4F01\u4E1A\u786E\u8BA4", children: _jsx(Select, { options: ['待确认', '已确认'].map(v => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "auditStatus", label: "\u8D44\u683C\u5BA1\u6838", children: _jsx(Select, { options: ['待审核', '已通过', '未通过'].map(v => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "materialStatus", label: "\u6750\u6599", children: _jsx(Select, { options: ['未提交', '审核中', '补正中', '已齐全'].map(v => ({ label: v, value: v })) }) }), _jsx(Form.Item, { name: "progress", label: "\u8FDB\u5EA6%", children: _jsx(Input, { type: "number" }) }), _jsx(Form.Item, { name: "cert", label: "\u8BC1\u4E66", children: _jsx(Select, { options: ['未开始', '培训中', '已结业', '已获证'].map(v => ({ label: v, value: v })) }) })] })) }) })] }));
}
