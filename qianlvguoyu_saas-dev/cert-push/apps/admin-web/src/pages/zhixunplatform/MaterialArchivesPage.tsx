import { useState, useMemo, useCallback } from 'react';
import { Typography, Select, Input, Button, Table, Tabs, message, Progress } from 'antd';
import {
  ArrowLeftOutlined,
  DatabaseOutlined,
  FilterOutlined,
  UnorderedListOutlined,
  SyncOutlined,
  CloudUploadOutlined,
  AuditOutlined,
  FileZipOutlined,
  TrophyOutlined,
  FilePdfOutlined,
  FileExcelOutlined,
  FileWordOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
  DownloadOutlined,
  CheckOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './MaterialArchivesPage.css';

// ============================================================
// 类型
// ============================================================
interface DocItem {
  id: string;
  name: string;
  category: string;
  stage: string;
  version: string;
  status: string;
  date: string;
  type: string;
  projectId: number;
  enterprise?: string;
  projectName?: string;
}

interface ProjectMeta {
  projectId: number;
  documents: DocItem[];
  reviewDone: boolean;
  archived: boolean;
  approvalStatus: string;
  approvalStep: number;
  physicalLocation: string;
  archiveDate: string;
}

// ============================================================
// 常量
// ============================================================
const DEFAULT_TASKS = [
  { id: 1, name: '2026年度安全培训计划编制', owner: '张明', deadline: '2026-06-15', priority: '高', enterprise: '广西建工集团', occupation: '安全员', skillLevel: '高级', region: '南宁', projectType: '安全培训', status: '进行中', planCount: 120, startDate: '2026-03-01', endDate: '2026-06-30' },
  { id: 2, name: '电工高级班认定材料整理', owner: '李芳', deadline: '2026-05-25', priority: '高', enterprise: '南宁供电局', occupation: '电工', skillLevel: '高级', region: '南宁', projectType: '资质认证', status: '进行中', planCount: 42 },
  { id: 3, name: 'Q2培训项目回款跟进', owner: '王强', deadline: '2026-05-30', priority: '中', enterprise: '柳州钢铁有限公司', occupation: '焊工', skillLevel: '中级', region: '柳州', projectType: '新员工培训', status: '草稿', planCount: 36 },
  { id: 4, name: '焊工中级班排课协调', owner: '赵丽', deadline: '2026-05-22', priority: '中', enterprise: '桂林旅游股份公司', occupation: '焊工', skillLevel: '中级', region: '桂林', projectType: '资质认证', status: '待审批', planCount: 28 },
  { id: 5, name: '培训档案年度归档', owner: '陈伟', deadline: '2026-06-30', priority: '低', enterprise: '梧州港务集团', occupation: '起重工', skillLevel: '初级', region: '梧州', projectType: '管理研修', status: '草稿', planCount: 15 },
];

const CATEGORIES = ['协议合同', '培训课件', '过程留痕', '认定证书', '补贴申报', '结业归档', '结算收尾'];
const STAGES = ['立项', '人才识别', '初步方案', '最终方案', '开班申报', '培训实施', '认定准备', '申报服务', '培训结业', '项目回款'];
const APPROVAL_NODES = ['培训部归档', '总经理审批', '资料中心入库'];

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

// ============================================================
// 同步流程资料
// ============================================================
function syncDocsFromFlow(task: (typeof DEFAULT_TASKS)[number]): DocItem[] {
  const pid = String(task.id);
  const docs: DocItem[] = [];

  const add = (name: string, category: string, stage: string, version: string, status: string, type: string) => {
    docs.push({ id: pid + '-' + stage + '-' + docs.length, name, category, stage, version: version || 'v1', status, date: todayStr(), type, projectId: task.id, enterprise: task.enterprise, projectName: task.name });
  };

  add(task.name + '-培训服务合同.pdf', '协议合同', '立项', 'v1', '已归档', 'pdf');
  add(task.name + '-项目立项表.xlsx', '协议合同', '立项', 'v1', '待归档', 'xls');
  add('学员名册与分班表.xlsx', '过程留痕', '分班', 'v1', '已生成', 'xls');
  add('班级编制说明.docx', '过程留痕', '分班', 'v1', '待归档', 'doc');
  add('课程排课表.xlsx', '过程留痕', '排课', 'v1', '已生成', 'xls');
  add('开班仪式材料包.zip', '过程留痕', '开班', 'v1', '已生成', 'pdf');
  add('考勤汇总表.xlsx', '过程留痕', '培训', 'v1', '已生成', 'xls');
  add('课堂留痕证据目录.pdf', '过程留痕', '培训', 'v1', '待归档', 'pdf');
  add('技能认定获证花名册.xlsx', '认定证书', '认定', 'v1', '已生成', 'xls');
  add('补贴申报材料包.pdf', '补贴申报', '申报', 'v1', '待归档', 'pdf');
  add('拨款进度跟踪表.xlsx', '补贴申报', '申报', 'v1', '待归档', 'xls');
  add('结业证明与档案目录.pdf', '结业归档', '结业', 'v1', '待归档', 'cert');
  add('项目结算单.pdf', '结算收尾', '回款', 'v1', '待归档', 'pdf');

  return docs;
}

// ============================================================
// 文件图标
// ============================================================
function FileTypeIcon({ type }: { type: string }) {
  const iconMap: Record<string, { icon: React.ReactNode; cls: string }> = {
    ppt: { icon: <FilePdfOutlined />, cls: 'file-ppt' },
    pdf: { icon: <FilePdfOutlined />, cls: 'file-pdf' },
    doc: { icon: <FileWordOutlined />, cls: 'file-doc' },
    cert: { icon: <SafetyCertificateOutlined />, cls: 'file-cert' },
    xls: { icon: <FileExcelOutlined />, cls: 'file-xls' },
  };
  const { icon, cls } = iconMap[type] || iconMap.pdf;
  return <span className={`file-icon ${cls}`}>{icon}</span>;
}

// ============================================================
// 主页面
// ============================================================
export function MaterialArchivesPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);
  const [projectMetas, setProjectMetas] = useState<Record<string, ProjectMeta>>({});
  const [selectedProjectId, setSelectedProjectId] = useState<string>(() => String(currentProjectId || ''));
  const [filterCategory, setFilterCategory] = useState('');
  const [filterStage, setFilterStage] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [activeTab, setActiveTab] = useState('list');

  const getProjectMeta = useCallback(
    (projectId: number): ProjectMeta => {
      const key = String(projectId);
      if (!projectMetas[key]) {
        const task = DEFAULT_TASKS.find((t) => t.id === projectId);
        const docs = task ? syncDocsFromFlow(task) : [];
        const meta: ProjectMeta = {
          projectId,
          documents: docs,
          reviewDone: false,
          archived: false,
          approvalStatus: '草稿',
          approvalStep: 0,
          physicalLocation: '',
          archiveDate: '',
        };
        setProjectMetas((prev) => ({ ...prev, [key]: meta }));
        return meta;
      }
      return projectMetas[key];
    },
    [projectMetas],
  );

  const updateProjectMeta = useCallback((projectId: number, updater: (prev: ProjectMeta) => ProjectMeta) => {
    setProjectMetas((prev) => {
      const key = String(projectId);
      const current = prev[key] || getProjectMeta(projectId);
      return { ...prev, [key]: updater(current) };
    });
  }, [getProjectMeta]);

  // 全部文档
  const allDocs = useMemo(() => {
    const docs: DocItem[] = [];
    DEFAULT_TASKS.forEach((t) => {
      const pid = String(t.id);
      let meta = projectMetas[pid];
      if (!meta) {
        meta = { projectId: t.id, documents: syncDocsFromFlow(t), reviewDone: false, archived: false, approvalStatus: '草稿', approvalStep: 0, physicalLocation: '', archiveDate: '' };
      }
      if (!meta.documents.length) {
        meta.documents = syncDocsFromFlow(t);
      }
      meta.documents.forEach((d) => {
        d.projectId = t.id;
        d.enterprise = t.enterprise;
        d.projectName = t.name;
        docs.push(d);
      });
    });
    return docs;
  }, [projectMetas]);

  const filteredDocs = useMemo(() => {
    return allDocs.filter((d) => {
      if (selectedProjectId && String(d.projectId) !== selectedProjectId) return false;
      if (filterCategory && d.category !== filterCategory) return false;
      if (filterStage && d.stage !== filterStage) return false;
      if (searchKeyword) {
        const hay = [d.name, d.enterprise || '', d.projectName || ''].join(' ').toLowerCase();
        if (!hay.includes(searchKeyword.toLowerCase())) return false;
      }
      return true;
    });
  }, [allDocs, selectedProjectId, filterCategory, filterStage, searchKeyword]);

  // 统计
  const stats = useMemo(() => {
    const archivedProjects = DEFAULT_TASKS.filter((t) => {
      const m = projectMetas[String(t.id)];
      return m?.archived || m?.approvalStatus === '已入库';
    }).length;
    const pending = allDocs.filter((d) => d.status !== '已归档').length;
    return { projects: DEFAULT_TASKS.length, docs: allDocs.length, pending, archived: archivedProjects };
  }, [allDocs, projectMetas]);

  // 选中项目
  const selectedProject = selectedProjectId ? DEFAULT_TASKS.find((t) => String(t.id) === selectedProjectId) : null;
  const selectedMeta = selectedProjectId ? projectMetas[selectedProjectId] : null;

  const handleSyncDocs = () => {
    const targets = selectedProjectId ? [+selectedProjectId] : DEFAULT_TASKS.map((t) => t.id);
    targets.forEach((id) => {
      const task = DEFAULT_TASKS.find((t) => t.id === id);
      if (task) {
        updateProjectMeta(id, (prev) => ({ ...prev, documents: syncDocsFromFlow(task) }));
      }
    });
    message.success('已从培训流程各环节同步资料清单');
  };

  const handleReview = () => {
    if (!selectedProjectId) { message.warning('请先选择项目'); return; }
    updateProjectMeta(+selectedProjectId, (prev) => ({ ...prev, reviewDone: true }));
    message.success('归档审查通过，资料完整度符合要求');
  };

  const handlePackArchive = () => {
    if (!selectedProjectId) { message.warning('请先选择项目'); return; }
    updateProjectMeta(+selectedProjectId, (prev) => {
      if (!prev.reviewDone) { message.warning('请先完成归档审查'); return prev; }
      return { ...prev, documents: prev.documents.map((d) => ({ ...d, status: '已归档' })), archiveDate: todayStr() };
    });
    message.success('项目全生命周期资料已打包电子归档');
  };

  const handleSubmitArchive = () => {
    if (!selectedProjectId) { message.warning('请先选择项目'); return; }
    updateProjectMeta(+selectedProjectId, (prev) => {
      if (prev.approvalStatus === '草稿') {
        if (!prev.archiveDate) { message.warning('请先电子归档打包'); return prev; }
        return { ...prev, approvalStatus: '审批中', approvalStep: 1 };
      } else if (prev.approvalStatus === '审批中') {
        const newStep = prev.approvalStep + 1;
        if (newStep >= APPROVAL_NODES.length) return { ...prev, approvalStatus: '已入库', archived: true, approvalStep: newStep };
        return { ...prev, approvalStep: newStep };
      }
      return prev;
    });
    message.success('归档审批进度已更新');
  };

  const handleMarkArchived = (docId: string) => {
    if (!selectedProjectId) return;
    updateProjectMeta(+selectedProjectId, (prev) => ({
      ...prev,
      documents: prev.documents.map((d) => (d.id === docId ? { ...d, status: '已归档' } : d)),
    }));
  };

  // ---- 项目列表列 ----
  const projectColumns: ColumnsType<any> = [
    {
      title: '企业 · 项目',
      key: 'project',
      render: (_, t) => (
        <div>
          <strong>{t.enterprise || '—'}</strong>
          <br />
          <small>{t.name}</small>
        </div>
      ),
    },
    {
      title: '档案完整度',
      key: 'completeness',
      render: (_, t) => {
        const docs = allDocs.filter((d) => d.projectId === t.id);
        const total = docs.length || 1;
        const done = docs.filter((d) => d.status === '已归档').length;
        const pct = Math.round((done / total) * 100);
        return (
          <div>
            <div style={{ fontSize: 13 }}>{pct}%</div>
            <Progress percent={pct} showInfo={false} size="small" />
          </div>
        );
      },
    },
    {
      title: '条目数',
      key: 'count',
      render: (_, t) => allDocs.filter((d) => d.projectId === t.id).length,
    },
    {
      title: '归档状态',
      key: 'archiveStatus',
      render: (_, t) => {
        const m = projectMetas[String(t.id)];
        const st = m?.archived ? '已入库' : m?.approvalStatus === '审批中' ? '审批中' : '待归档';
        const cls = m?.archived ? 'st-archived' : m?.approvalStatus === '审批中' ? 'st-pending' : 'st-draft';
        return <span className={`status-badge ${cls}`}>{st}</span>;
      },
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, t) => (
        <Button
          size="small"
          onClick={() => {
            setSelectedProjectId(String(t.id));
            setActiveTab('business');
          }}
        >
          查看档案
        </Button>
      ),
    },
  ];

  // ---- 业务档案列 ----
  const docColumns: ColumnsType<DocItem> = [
    {
      title: '资料名称',
      key: 'name',
      render: (_, d) => (
        <div>
          <FileTypeIcon type={d.type} />
          <strong>{d.name}</strong>
          {d.projectName && <br />}
          {d.projectName && <small style={{ color: '#64748B' }}>{d.enterprise} · {d.projectName}</small>}
        </div>
      ),
    },
    { title: '分类', dataIndex: 'category' },
    { title: '阶段', dataIndex: 'stage' },
    { title: '版本', dataIndex: 'version' },
    {
      title: '归档状态',
      dataIndex: 'status',
      render: (v) => {
        const cls = v === '已归档' ? 'st-done' : v === '已生成' ? 'st-pending' : 'st-draft';
        return <span className={`status-badge ${cls}`}>{v}</span>;
      },
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, d) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <Button size="small" onClick={() => message.info('下载演示：' + d.name)}>下载</Button>
          <Button size="small" onClick={() => handleMarkArchived(d.id)}>标记归档</Button>
        </div>
      ),
    },
  ];

  // ---- 数据台账 ----
  const ledgerItems = useMemo(() => {
    const projects = selectedProjectId ? DEFAULT_TASKS.filter((t) => String(t.id) === selectedProjectId) : DEFAULT_TASKS;
    const items: { label: string; val: string | number }[] = [];
    projects.forEach((t) => {
      items.push(
        { label: t.name + ' · 班级数', val: 0 },
        { label: '参训学员', val: '0 人' },
        { label: '培训课次', val: '0 次' },
        { label: '认定通过', val: '0 人' },
        { label: '补贴申报', val: '—' },
        { label: '结业归档', val: '未完成' },
        { label: '回款状态', val: '—' },
      );
    });
    return items;
  }, [selectedProjectId]);

  // ---- 归档审批 ----
  const approvalTrack = useMemo(() => {
    if (!selectedProjectId || !selectedMeta) return null;
    const step = selectedMeta.approvalStep || 0;
    return APPROVAL_NODES.map((name, i) => {
      let cls = 'approval-node';
      if (selectedMeta.archived || selectedMeta.approvalStatus === '已入库') cls += ' done';
      else if (i < step) cls += ' done';
      else if (i === step && selectedMeta.approvalStatus === '审批中') cls += ' active';
      return <div key={name} className={cls}>{name}</div>;
    });
  }, [selectedProjectId, selectedMeta]);

  return (
    <div className="material-archives-page">
      {/* 顶部栏 */}
      <header className="ma-topbar">
        <div className="ma-topbar-left">
          <a className="ma-back-link" onClick={() => navigateTo('zhixun-project-archives')}>
            <ArrowLeftOutlined /> 返回项目档案
          </a>
          <div>
            <h1 className="ma-title">资料档案</h1>
            <span className="ma-sub">
              {selectedProject ? `${selectedProject.enterprise} · ${selectedProject.name} · 资料档案` : '多项目资料归档 · 项目-班级-阶段-版本'}
            </span>
          </div>
        </div>
        <span className="ma-badge"><DatabaseOutlined /> 列表 · 业务档案 · 数据台账</span>
      </header>

      <main className="ma-page">
        {/* 检索卡片 */}
        <div className="ma-card">
          <div className="ma-card-head">
            <div className="ma-card-head-title"><FilterOutlined /> 检索与项目</div>
            <Button size="small" icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
          </div>
          <div className="ma-filter-bar">
            <div className="ma-filter-item">
              <label>企业培训项目</label>
              <Select
                placeholder="全部项目…"
                value={selectedProjectId || undefined}
                onChange={(v) => { setSelectedProjectId(v || ''); setCurrentProjectId(v ? +v : null); }}
                allowClear
                style={{ width: '100%' }}
                options={DEFAULT_TASKS.map((t) => ({ value: String(t.id), label: `${t.enterprise || '—'} · ${t.name}` }))}
              />
            </div>
            <div className="ma-filter-item">
              <label>资料分类</label>
              <Select
                placeholder="全部分类"
                value={filterCategory || undefined}
                onChange={(v) => setFilterCategory(v || '')}
                allowClear
                style={{ width: '100%' }}
                options={CATEGORIES.map((v) => ({ value: v, label: v }))}
              />
            </div>
            <div className="ma-filter-item">
              <label>流程阶段</label>
              <Select
                placeholder="全部阶段"
                value={filterStage || undefined}
                onChange={(v) => setFilterStage(v || '')}
                allowClear
                style={{ width: '100%' }}
                options={STAGES.map((v) => ({ value: v, label: v }))}
              />
            </div>
            <div className="ma-filter-item">
              <label>关键词</label>
              <Input
                placeholder="文件名、企业名…"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                allowClear
              />
            </div>
          </div>
          {selectedProject && (
            <div className="ma-project-banner">
              <div className="ma-banner-item"><label>企业</label><strong>{selectedProject.enterprise || '—'}</strong></div>
              <div className="ma-banner-item"><label>项目</label><strong>{selectedProject.name || '—'}</strong></div>
              <div className="ma-banner-item"><label>档案条目</label><strong>{allDocs.filter((d) => String(d.projectId) === selectedProjectId).length} 份</strong></div>
              <div className="ma-banner-item"><label>归档状态</label><strong>{selectedMeta?.approvalStatus || '草稿'}</strong></div>
            </div>
          )}
        </div>

        {/* 统计卡片 */}
        <div className="ma-stat-row">
          <div className="ma-stat-card">
            <div className="ma-stat-val">{stats.projects}</div>
            <div className="ma-stat-lbl">在管项目</div>
          </div>
          <div className="ma-stat-card">
            <div className="ma-stat-val success">{stats.docs}</div>
            <div className="ma-stat-lbl">档案条目</div>
          </div>
          <div className="ma-stat-card">
            <div className="ma-stat-val warn">{stats.pending}</div>
            <div className="ma-stat-lbl">待归档</div>
          </div>
          <div className="ma-stat-card">
            <div className="ma-stat-val">{stats.archived}</div>
            <div className="ma-stat-lbl">已归档项目</div>
          </div>
        </div>

        {/* 主标签页 */}
        <div className="ma-card">
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={[
              {
                key: 'list',
                label: '项目列表',
                children: (
                  <Table
                    dataSource={DEFAULT_TASKS}
                    columns={projectColumns}
                    rowKey="id"
                    pagination={false}
                    locale={{ emptyText: '暂无培训项目，请先在任务档案创建' }}
                  />
                ),
              },
              {
                key: 'business',
                label: '业务档案',
                children: (
                  <>
                    <div className="ma-toolbar">
                      <Button type="primary" size="small" icon={<SyncOutlined />} onClick={handleSyncDocs}>同步流程资料</Button>
                      <Button size="small" icon={<CloudUploadOutlined />} onClick={() => message.info('上传资料功能演示')}>上传资料</Button>
                      <Button size="small" icon={<AuditOutlined />} onClick={handleReview}>归档审查</Button>
                    </div>
                    <Table
                      dataSource={filteredDocs}
                      columns={docColumns}
                      rowKey="id"
                      pagination={false}
                      locale={{ emptyText: '暂无资料，请选择项目或点击「同步流程资料」' }}
                    />
                  </>
                ),
              },
              {
                key: 'ledger',
                label: '数据台账',
                children: (
                  <>
                    <p style={{ fontSize: 13, color: '#64748B', marginBottom: 16 }}>汇总各培训流程环节产生的结构化数据（人才识别、方案、开班申报、培训实施、认定准备、申报服务、培训结业、项目回款）。</p>
                    {ledgerItems.length ? (
                      <div className="ma-ledger-grid">
                        {ledgerItems.map((it, i) => (
                          <div key={i} className="ma-ledger-item">
                            <label>{it.label}</label>
                            <strong>{it.val}</strong>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p style={{ color: '#64748B' }}>请选择项目查看数据台账</p>
                    )}
                  </>
                ),
              },
              {
                key: 'archive',
                label: '归档审批',
                children: (
                  <>
                    <p style={{ fontSize: 13, color: '#64748B', marginBottom: 16 }}>按 MD 流程：培训部资料归档 → 总经理归档审批 → 电子归档至资料中心。</p>
                    <div className="ma-toolbar">
                      <Button type="primary" size="small" icon={<FileZipOutlined />} onClick={handlePackArchive}>电子归档打包</Button>
                      <Button size="small" className="btn-success" icon={<TrophyOutlined />} onClick={handleSubmitArchive}>提交归档审批</Button>
                    </div>
                    <div className="ma-form-group" style={{ maxWidth: 400, marginBottom: 16 }}>
                      <label>纸质档案存放位置（可选）</label>
                      <Input
                        placeholder="如：档案室 A-12 柜"
                        value={selectedMeta?.physicalLocation || ''}
                        onChange={(e) => {
                          if (!selectedProjectId) return;
                          updateProjectMeta(+selectedProjectId, (prev) => ({ ...prev, physicalLocation: e.target.value }));
                        }}
                      />
                    </div>
                    <div className="ma-approval-track">
                      {approvalTrack || <p style={{ color: '#64748B', fontSize: 14 }}>请先选择具体项目以办理归档审批</p>}
                    </div>
                    {selectedMeta?.archiveDate && (
                      <div className="ma-archive-summary">
                        <strong>电子归档包</strong> 已于 {selectedMeta.archiveDate} 打包入库。存放位置：{selectedMeta.physicalLocation || '未登记'}
                      </div>
                    )}
                  </>
                ),
              },
            ]}
          />
        </div>
      </main>
    </div>
  );
}
