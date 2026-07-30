import { useState, useMemo } from 'react';
import { Typography, Table, Button, Input, Select, Modal, Form, Tag, Space, Tabs, message, Progress } from 'antd';
import {
  ArrowLeftOutlined,
  UserOutlined,
  PlusOutlined,
  ImportOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  TeamOutlined,
  IdcardOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './ArchivePages.css';

/* ============================================================
   模拟数据（对照 archive-data.js）
   ============================================================ */

const PROJECTS = ['电工认证项目', '焊工晋升项目', '起重工安全培训', '安全员岗前培训'];

interface Teacher {
  id: string; name: string; gender: string; role: string; title: string; teachLevel: string;
  skills: string; project: string; certName: string; certNo: string; certExpiry: string;
  agreement: string; nda: string; rating: number; phone: string; status: string; hours: number; settleStatus: string;
}

interface Student {
  id: string; name: string; gender: string; idCard: string; enterprise: string; trade: string;
  level: string; project: string; className: string; enrollStatus: string; confirmStatus: string;
  auditStatus: string; materialStatus: string; attendance: number; homework: number; mockScore: number;
  warnLevel: string; progress: number; cert: string;
}

const MOCK_TEACHERS: Teacher[] = [
  { id: 'T001', name: '张建国', gender: '男', role: '主讲', title: '高级', teachLevel: '技师', skills: '电工', project: '电工认证项目', certName: '电工技师职业资格证', certNo: 'GX202401001', certExpiry: '2027-08-15', agreement: '已签署', nda: '已签署', rating: 4.8, phone: '138****1001', status: '在职', hours: 120, settleStatus: '已结算' },
  { id: 'T002', name: '李秀英', gender: '女', role: '主讲', title: '专家', teachLevel: '高级技师', skills: '焊工', project: '焊工晋升项目', certName: '焊工高级技师证', certNo: 'GX202308882', certExpiry: '2026-06-20', agreement: '已签署', nda: '已签署', rating: 4.9, phone: '139****2002', status: '在职', hours: 96, settleStatus: '待结算' },
  { id: 'T003', name: '王德明', gender: '男', role: '助教', title: '中级', teachLevel: '高级工', skills: '起重工', project: '起重工安全培训', certName: '起重机械作业证', certNo: 'GX202205501', certExpiry: '2026-04-01', agreement: '未签署', nda: '已签署', rating: 4.2, phone: '137****3003', status: '兼职', hours: 48, settleStatus: '未结算' },
  { id: 'T004', name: '陈丽华', gender: '女', role: '班主任', title: '高级', teachLevel: '—', skills: '教务管理', project: '电工认证项目', certName: '人力资源管理师', certNo: 'GX202011203', certExpiry: '2028-01-10', agreement: '已签署', nda: '已签署', rating: 4.7, phone: '136****4004', status: '在职', hours: 0, settleStatus: '—' },
  { id: 'T005', name: '刘志强', gender: '男', role: '主讲', title: '初级', teachLevel: '中级工', skills: '安全员', project: '安全员岗前培训', certName: '安全员培训师资证', certNo: 'GX202506001', certExpiry: '2029-03-01', agreement: '已签署', nda: '已签署', rating: 4.0, phone: '135****5005', status: '在职', hours: 64, settleStatus: '已结算' },
  { id: 'T006', name: '赵敏', gender: '女', role: '主讲', title: '中级', teachLevel: '技师', skills: '电工', project: '电工认证项目', certName: '电工技师职业资格证', certNo: 'GX201907766', certExpiry: '2026-05-28', agreement: '审批中', nda: '未签署', rating: 4.5, phone: '134****6006', status: '兼职', hours: 32, settleStatus: '未结算' },
  { id: 'T007', name: '黄伟', gender: '男', role: '跟班', title: '中级', teachLevel: '高级工', skills: '焊工', project: '焊工晋升项目', certName: '焊工高级工证', certNo: 'GX202103445', certExpiry: '2027-11-30', agreement: '已签署', nda: '已签署', rating: 4.3, phone: '133****7007', status: '在职', hours: 80, settleStatus: '待结算' },
];

const MOCK_STUDENTS: Student[] = [
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
function badgeTag(text: string, type?: string) {
  const colorMap: Record<string, string> = {
    '已签署': 'success', '已确认': 'success', '已通过': 'success', '已齐全': 'success', '已结算': 'success',
    '在职': 'success', '在培': 'processing', '待确认': 'warning', '待审核': 'warning', '补正中': 'warning',
    '审批中': 'warning', '未签署': 'warning', '未结算': 'warning', '未提交': 'error', '缺勤预警': 'error',
    '综合预警': 'error', '作业预警': 'error', '兼职': 'default',
  };
  const color = colorMap[text] || type || 'default';
  return <Tag color={color}>{text || '—'}</Tag>;
}

function daysUntil(d: string) {
  if (!d || d === '—') return 999;
  const x = new Date(d);
  return isNaN(x.getTime()) ? 999 : Math.ceil((x.getTime() - Date.now()) / 86400000);
}

function isCertWarn(t: Teacher) { const d = daysUntil(t.certExpiry); return d >= 0 && d <= 90; }
function isAgreePending(t: Teacher) { return t.status === '兼职' && (t.agreement !== '已签署' || t.nda !== '已签署'); }
function isStudentWarn(s: Student) { return !!s.warnLevel || s.attendance < 80 || s.homework < 70 || s.mockScore < 60; }

/* ============================================================
   PersonnelArchivePage
   ============================================================ */
export function PersonnelArchivePage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const [teachers, setTeachers] = useState<Teacher[]>(MOCK_TEACHERS);
  const [students, setStudents] = useState<Student[]>(MOCK_STUDENTS);
  const [activeTab, setActiveTab] = useState<'teachers' | 'students'>('teachers');
  const [searchT, setSearchT] = useState('');
  const [searchS, setSearchS] = useState('');
  const [teacherFilter, setTeacherFilter] = useState('all');
  const [studentFilter, setStudentFilter] = useState('all');
  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'add' | 'edit'>('add');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form] = Form.useForm();

  const filterTeachers = useMemo(() => {
    let list = teachers;
    if (searchT) { const q = searchT.toLowerCase(); list = list.filter(t => [t.id, t.name, t.skills, t.project, t.certName].some(k => String(k).toLowerCase().includes(q))); }
    if (teacherFilter === 'certWarn') list = list.filter(isCertWarn);
    if (teacherFilter === 'agreement') list = list.filter(t => t.status === '兼职' && (t.agreement !== '已签署' || t.nda !== '已签署'));
    if (teacherFilter === 'settle') list = list.filter(t => t.settleStatus === '待结算' || t.settleStatus === '未结算');
    return list;
  }, [teachers, searchT, teacherFilter]);

  const filterStudents = useMemo(() => {
    let list = students;
    if (searchS) { const q = searchS.toLowerCase(); list = list.filter(s => [s.id, s.name, s.enterprise, s.trade, s.className].some(k => String(k).toLowerCase().includes(q))); }
    if (studentFilter === 'confirm') list = list.filter(s => s.confirmStatus === '待确认');
    if (studentFilter === 'audit') list = list.filter(s => s.auditStatus === '待审核');
    if (studentFilter === 'material') list = list.filter(s => ['未提交', '补正中', '审核中'].includes(s.materialStatus));
    if (studentFilter === 'warn') list = list.filter(s => !!s.warnLevel || s.attendance < 80 || s.homework < 70 || s.mockScore < 60);
    return list;
  }, [students, searchS, studentFilter]);

  const kpi = useMemo(() => ({
    certWarn: teachers.filter(isCertWarn).length,
    pendingConfirm: students.filter(s => s.confirmStatus === '待确认').length,
    pendingAudit: students.filter(s => s.auditStatus === '待审核').length,
    warnStudents: students.filter(s => !!s.warnLevel || s.attendance < 80 || s.homework < 70 || s.mockScore < 60).length,
  }), [teachers, students]);

  const openModal = (mode: 'add' | 'edit', id?: string) => {
    setModalMode(mode);
    setEditingId(id || null);
    if (mode === 'edit' && id) {
      const entity = activeTab === 'teachers' ? teachers : students;
      const record = entity.find(x => x.id === id);
      if (record) form.setFieldsValue(record);
    } else {
      form.resetFields();
      if (activeTab === 'teachers') form.setFieldsValue({ id: 'T' + Date.now(), gender: '男', role: '主讲', title: '中级', status: '兼职', agreement: '未签署', nda: '未签署', settleStatus: '未结算' });
      else form.setFieldsValue({ id: 'S' + Date.now(), confirmStatus: '待确认', auditStatus: '待审核', materialStatus: '未提交', progress: 0, cert: '未开始', enrollStatus: '待入班' });
    }
    setModalOpen(true);
  };

  const handleSave = () => {
    form.validateFields().then((vals) => {
      if (modalMode === 'add') {
        if (activeTab === 'teachers') setTeachers(prev => [...prev, vals as Teacher]);
        else setStudents(prev => [...prev, vals as Student]);
      } else {
        if (activeTab === 'teachers') setTeachers(prev => prev.map(t => t.id === editingId ? { ...t, ...vals } : t));
        else setStudents(prev => prev.map(s => s.id === editingId ? { ...s, ...vals } : s));
      }
      setModalOpen(false);
      message.success(modalMode === 'add' ? '新增成功' : '保存成功');
    });
  };

  const handleDelete = (id: string) => {
    if (activeTab === 'teachers') setTeachers(prev => prev.filter(t => t.id !== id));
    else setStudents(prev => prev.filter(s => s.id !== id));
    message.success('已删除');
  };

  const teacherColumns: ColumnsType<Teacher> = [
    { title: '编号', dataIndex: 'id', width: 80 },
    { title: '姓名', dataIndex: 'name', render: (v, r) => <><strong>{v}</strong><br /><span style={{ fontSize: 11, color: '#64748B' }}>{r.phone}</span></> },
    { title: '角色', dataIndex: 'role', render: (v) => badgeTag(v) },
    { title: '职称/等级', render: (_, r) => <>{r.title}<br /><span style={{ fontSize: 11, color: '#64748B' }}>{r.teachLevel}</span></> },
    { title: '工种', dataIndex: 'skills' },
    { title: '项目', dataIndex: 'project' },
    { title: '资质', dataIndex: 'certName' },
    { title: '到期', dataIndex: 'certExpiry', render: (v, r) => {
      const d = daysUntil(v);
      if (d < 0) return <Tag color="error">已过期</Tag>;
      if (d <= 90) return <Tag color="warning">{v}（{d}天）</Tag>;
      return v || '—';
    }},
    { title: '协议', dataIndex: 'agreement', render: (v) => badgeTag(v) },
    { title: '承诺', dataIndex: 'nda', render: (v) => badgeTag(v) },
    { title: '课酬', dataIndex: 'settleStatus', render: (v) => badgeTag(v) },
    { title: '评价', dataIndex: 'rating', render: (v) => v ? `${v} ★` : '—' },
    { title: '状态', dataIndex: 'status', render: (v) => badgeTag(v) },
    { title: '操作', width: 120, render: (_, r) => (
      <Space size={4}>
        <Button size="small" icon={<EditOutlined />} onClick={() => openModal('edit', r.id)} />
        <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id)} />
      </Space>
    )},
  ];

  const studentColumns: ColumnsType<Student> = [
    { title: '编号', dataIndex: 'id', width: 80 },
    { title: '姓名', dataIndex: 'name', render: (v, r) => <><strong>{v}</strong><br /><span style={{ fontSize: 11, color: '#64748B' }}>{r.idCard}</span></> },
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
    { title: '预警', dataIndex: 'warnLevel', render: (v) => v ? <Tag color="error">{v}</Tag> : '—' },
    { title: '进度', dataIndex: 'progress', render: (v) => <Progress percent={v} size="small" /> },
    { title: '证书', dataIndex: 'cert', render: (v) => badgeTag(v) },
    { title: '操作', width: 120, render: (_, r) => (
      <Space size={4}>
        <Button size="small" icon={<EditOutlined />} onClick={() => openModal('edit', r.id)} />
        <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id)} />
      </Space>
    )},
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

  return (
    <div className="pa-page">
      {/* topbar */}
      <header className="pa-topbar">
        <div className="pa-topbar-left">
          <a className="pa-back-link" onClick={() => navigateTo('zhixun-project-archives')}>
            <ArrowLeftOutlined /> 返回项目档案
          </a>
          <div>
            <h1>人员档案</h1>
            <span className="pa-sub">5M·人：师资与教务 + 参训学员</span>
          </div>
        </div>
        <span className="pa-topbar-tag"><TeamOutlined /> 项目档案</span>
      </header>

      <main className="pa-main">
        {/* KPI */}
        <div className="pa-stat-row">
          <div className="pa-stat-card"><div className="pa-stat-val">{teachers.length}</div><div className="pa-stat-lbl">在册讲师</div></div>
          <div className={`pa-stat-card${kpi.certWarn ? ' warn' : ''}`}><div className="pa-stat-val">{kpi.certWarn}</div><div className="pa-stat-lbl">资质90天内到期</div></div>
          <div className="pa-stat-card"><div className="pa-stat-val">{students.length}</div><div className="pa-stat-lbl">参训学员</div></div>
          <div className={`pa-stat-card${kpi.pendingConfirm ? ' warn' : ''}`}><div className="pa-stat-val">{kpi.pendingConfirm}</div><div className="pa-stat-lbl">待企业确认</div></div>
          <div className={`pa-stat-card${kpi.pendingAudit ? ' warn' : ''}`}><div className="pa-stat-val">{kpi.pendingAudit}</div><div className="pa-stat-lbl">待资格审核</div></div>
          <div className={`pa-stat-card${kpi.warnStudents ? ' danger' : ''}`}><div className="pa-stat-val">{kpi.warnStudents}</div><div className="pa-stat-lbl">过程预警学员</div></div>
        </div>

        <div className="pa-hint-card">
          <strong>人机料法环 · 人</strong>：讲师资质/协议/课酬；学员名单确认、资格审核、材料审核与过程预警。
        </div>

        {/* Tabs */}
        <div className="pa-tab-bar">
          <button className={`pa-tab-btn${activeTab === 'teachers' ? ' active' : ''}`} onClick={() => setActiveTab('teachers')}>
            <IdcardOutlined /> 师资与教务
          </button>
          <button className={`pa-tab-btn${activeTab === 'students' ? ' active' : ''}`} onClick={() => setActiveTab('students')}>
            <UserOutlined /> 参训学员
          </button>
        </div>

        {/* Card */}
        <div className="pa-card">
          <div className="pa-card-head">
            <div className="pa-title-wrap">
              {activeTab === 'teachers' ? <IdcardOutlined /> : <UserOutlined />}
              {activeTab === 'teachers' ? '讲师档案与资质' : '培训人员台账'}
            </div>
          </div>

          {/* Filter chips */}
          <div className="pa-filter-chips">
          {chips.map(c => (
            <button
              key={c.key}
              className={`pa-filter-chip${(activeTab === 'teachers' ? teacherFilter : studentFilter) === c.key ? ' active' : ''}`}
              onClick={() => activeTab === 'teachers' ? setTeacherFilter(c.key) : setStudentFilter(c.key)}
            >
              {c.label}<span className="pa-chip-count">{c.count}</span>
            </button>
          ))}
        </div>

        {/* Toolbar */}
        <div className="pa-toolbar">
          <Input
            prefix={<SearchOutlined />}
            placeholder={activeTab === 'teachers' ? '搜索姓名、工种、项目…' : '搜索姓名、企业、班级…'}
            value={activeTab === 'teachers' ? searchT : searchS}
            onChange={e => activeTab === 'teachers' ? setSearchT(e.target.value) : setSearchS(e.target.value)}
            className="pa-search-input"
            allowClear
          />
          <div className="pa-toolbar-actions">
            <Button icon={<ImportOutlined />}>{activeTab === 'teachers' ? '导入讲师池' : '导入名单'}</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal('add')}>
              {activeTab === 'teachers' ? '新增讲师' : '新增学员'}
            </Button>
          </div>
        </div>

        {/* Table */}
        <div className="pa-table-wrap">
          <Table
            columns={(activeTab === 'teachers' ? teacherColumns : studentColumns) as ColumnsType<Teacher & Student>}
            dataSource={(activeTab === 'teachers' ? filterTeachers : filterStudents) as (Teacher & Student)[]}
            rowKey="id"
            size="small"
            scroll={{ x: activeTab === 'teachers' ? 1400 : 1700 }}
            pagination={{ pageSize: 10, showSizeChanger: true, showTotal: t => `共 ${t} 条` }}
            rowClassName={activeTab === 'teachers'
              ? ((r: Teacher & Student) => (isCertWarn(r as unknown as Teacher) || isAgreePending(r as unknown as Teacher)) ? 'row-warn' : '')
              : ((r: Teacher & Student) => isStudentWarn(r as unknown as Student) ? 'row-warn' : '')
            }
          />
        </div>
        </div>{/* .pa-card */}
      </main>

      {/* Modal */}
      <Modal
        title={`${modalMode === 'add' ? '新增' : '编辑'}${activeTab === 'teachers' ? '讲师' : '学员'}`}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        width={640}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          {activeTab === 'teachers' ? (
            <>
              <Form.Item name="id" label="编号"><Input /></Form.Item>
              <Form.Item name="name" label="姓名" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="gender" label="性别"><Select options={[{ label: '男', value: '男' }, { label: '女', value: '女' }]} /></Form.Item>
              <Form.Item name="role" label="角色"><Select options={['主讲', '助教', '班主任', '跟班'].map(v => ({ label: v, value: v }))} /></Form.Item>
              <Form.Item name="title" label="职称"><Select options={['初级', '中级', '高级', '专家'].map(v => ({ label: v, value: v }))} /></Form.Item>
              <Form.Item name="teachLevel" label="授课等级"><Input /></Form.Item>
              <Form.Item name="skills" label="工种"><Input /></Form.Item>
              <Form.Item name="project" label="项目"><Select options={PROJECTS.map(v => ({ label: v, value: v }))} /></Form.Item>
              <Form.Item name="certName" label="资质名称"><Input /></Form.Item>
              <Form.Item name="certExpiry" label="到期日"><Input /></Form.Item>
              <Form.Item name="agreement" label="协议"><Select options={['已签署', '审批中', '未签署'].map(v => ({ label: v, value: v }))} /></Form.Item>
              <Form.Item name="nda" label="保密承诺"><Select options={['已签署', '未签署'].map(v => ({ label: v, value: v }))} /></Form.Item>
              <Form.Item name="phone" label="电话"><Input /></Form.Item>
              <Form.Item name="status" label="状态"><Select options={['在职', '兼职'].map(v => ({ label: v, value: v }))} /></Form.Item>
            </>
          ) : (
            <>
              <Form.Item name="id" label="编号"><Input /></Form.Item>
              <Form.Item name="name" label="姓名" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="enterprise" label="企业"><Input /></Form.Item>
              <Form.Item name="trade" label="工种"><Input /></Form.Item>
              <Form.Item name="className" label="班级"><Input /></Form.Item>
              <Form.Item name="confirmStatus" label="企业确认"><Select options={['待确认', '已确认'].map(v => ({ label: v, value: v }))} /></Form.Item>
              <Form.Item name="auditStatus" label="资格审核"><Select options={['待审核', '已通过', '未通过'].map(v => ({ label: v, value: v }))} /></Form.Item>
              <Form.Item name="materialStatus" label="材料"><Select options={['未提交', '审核中', '补正中', '已齐全'].map(v => ({ label: v, value: v }))} /></Form.Item>
              <Form.Item name="progress" label="进度%"><Input type="number" /></Form.Item>
              <Form.Item name="cert" label="证书"><Select options={['未开始', '培训中', '已结业', '已获证'].map(v => ({ label: v, value: v }))} /></Form.Item>
            </>
          )}
        </Form>
      </Modal>
    </div>
  );
}
