import { useState, useMemo, useCallback } from 'react';
import { Typography, Input, Select, Button, Table, Modal, message } from 'antd';
import {
  ArrowLeftOutlined,
  UserSwitchOutlined,
  InfoCircleOutlined,
  FileExcelOutlined,
  EyeOutlined,
  EditOutlined,
  AuditOutlined,
  SendOutlined,
  CloudUploadOutlined,
  CloseOutlined,
  DeleteOutlined,
  ThunderboltOutlined,
  CheckOutlined,
  ArrowRightOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import './TalentIdentificationPage.css';

// ============================================================
// 类型
// ============================================================
interface Person {
  id: number;
  name: string;
  code: string;
  dept: string;
  occ: string;
  level: string;
  status: string;
}

interface Company {
  id: string;
  name: string;
  unidentified: number;
  pending: number;
  confirmed: number;
  status: string;
  persons?: Person[];
}

// ============================================================
// 模拟数据
// ============================================================
const DEFAULT_COMPANIES: Company[] = [
  { id: 'c1', name: '南南铝业股份有限公司', unidentified: 286, pending: 42, confirmed: 897, status: '已审核' },
  { id: 'c2', name: '华锡集团', unidentified: 156, pending: 28, confirmed: 521, status: '已确认' },
  { id: 'c3', name: '金海不锈钢', unidentified: 98, pending: 15, confirmed: 368, status: '未审核' },
  { id: 'c4', name: '柳州钢铁有限公司', unidentified: 45, pending: 12, confirmed: 210, status: '已确认' },
  { id: 'c5', name: '南宁供电局', unidentified: 32, pending: 8, confirmed: 156, status: '已审核' },
];

const DEFAULT_TASKS = [
  { id: 1, name: '2026年度安全培训计划编制', enterprise: '广西建工集团', planCount: 120 },
  { id: 2, name: '电工高级班认定材料整理', enterprise: '南宁供电局', planCount: 42 },
  { id: 3, name: 'Q2培训项目回款跟进', enterprise: '柳州钢铁有限公司', planCount: 36 },
  { id: 4, name: '焊工中级班排课协调', enterprise: '桂林旅游股份公司', planCount: 28 },
  { id: 5, name: '培训档案年度归档', enterprise: '梧州港务集团', planCount: 15 },
];

const NAMES = ['张伟', '李娜', '王强', '赵丽', '刘洋', '陈静', '杨帆', '周敏'];
const DEPTS = ['生产一部', '生产二部', '质检部', '设备科'];
const OCCS = ['电工', '焊工', '起重工', '安全员'];
const LEVELS = ['初级', '中级', '高级', '技师'];

function genPersons(company: Company): Person[] {
  if (company.persons && company.persons.length) return company.persons;
  const n = Math.min(company.unidentified + company.pending + company.confirmed, 12) || 8;
  const persons: Person[] = [];
  for (let i = 0; i < n; i++) {
    let st = '已确认';
    if (i < company.unidentified) st = '未识别';
    else if (i < company.unidentified + company.pending) st = '待确认';
    persons.push({
      id: i + 1,
      name: NAMES[i % NAMES.length] + (i > 7 ? i : ''),
      code: 'GH' + String(1000 + i),
      dept: DEPTS[i % DEPTS.length],
      occ: OCCS[i % OCCS.length],
      level: LEVELS[i % LEVELS.length],
      status: st,
    });
  }
  company.persons = persons;
  return persons;
}

function statusClass(st: string) {
  if (st === '已审核') return 'st-audited';
  if (st === '已确认') return 'st-confirmed';
  return 'st-pending';
}

// ============================================================
// 主页面
// ============================================================
export function TalentIdentificationPage() {
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const currentProjectId = useNavStore((s) => s.currentProjectId);
  const setCurrentProjectId = useNavStore((s) => s.setCurrentProjectId);

  const [companies, setCompanies] = useState<Company[]>(() => {
    // 从 URL 参数 / navStore 匹配项目
    const list = [...DEFAULT_COMPANIES];
    if (currentProjectId) {
      const task = DEFAULT_TASKS.find((t) => t.id === currentProjectId);
      if (task && !list.some((c) => c.name === task.enterprise)) {
        list.unshift({
          id: 't' + currentProjectId,
          name: task.enterprise || task.name,
          unidentified: Math.floor((task.planCount || 30) * 0.3),
          pending: Math.floor((task.planCount || 30) * 0.15),
          confirmed: Math.floor((task.planCount || 30) * 0.5),
          status: '未审核',
        });
      }
    }
    return list;
  });

  const [filterStatus, setFilterStatus] = useState('');
  const [searchKeyword, setSearchKeyword] = useState(() => {
    if (currentProjectId) {
      const task = DEFAULT_TASKS.find((t) => t.id === currentProjectId);
      return task ? (task.enterprise || task.name) : '';
    }
    return '';
  });
  const [currentUnitId, setCurrentUnitId] = useState<string | null>(null);
  const [personFilter, setPersonFilter] = useState('all');
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importUnit, setImportUnit] = useState('');
  const [importFileName, setImportFileName] = useState('');

  const currentCompany = currentUnitId ? companies.find((c) => c.id === currentUnitId) : null;
  const projectTask = currentProjectId ? DEFAULT_TASKS.find((t) => t.id === currentProjectId) : null;

  // 统计
  const stats = useMemo(() => ({
    count: companies.length,
    unidentified: companies.reduce((s, c) => s + c.unidentified, 0),
    pending: companies.reduce((s, c) => s + c.pending, 0),
    confirmed: companies.reduce((s, c) => s + c.confirmed, 0),
  }), [companies]);

  // 筛选单位
  const filteredCompanies = useMemo(() =>
    companies.filter((c) => {
      if (filterStatus && c.status !== filterStatus) return false;
      if (searchKeyword && !c.name.toLowerCase().includes(searchKeyword.toLowerCase())) return false;
      return true;
    }), [companies, filterStatus, searchKeyword]);

  // 人员列表
  const persons = useMemo(() => {
    if (!currentCompany) return [];
    const p = genPersons(currentCompany);
    if (personFilter === 'all') return p;
    if (personFilter === 'unidentified') return p.filter((x) => x.status === '未识别');
    if (personFilter === 'pending') return p.filter((x) => x.status === '待确认');
    return p.filter((x) => x.status === '已确认');
  }, [currentCompany, personFilter]);

  const updateCompany = useCallback((id: string, updater: (c: Company) => void) => {
    setCompanies((prev) => prev.map((c) => {
      if (c.id !== id) return c;
      const clone = { ...c, persons: c.persons ? [...c.persons] : undefined };
      updater(clone);
      return clone;
    }));
  }, []);

  // 操作
  const handleIdentify = (personId: number) => {
    if (!currentUnitId) return;
    updateCompany(currentUnitId, (c) => {
      const p = c.persons?.find((x) => x.id === personId);
      if (p && p.status === '未识别') {
        p.status = '待确认';
        c.unidentified = Math.max(0, c.unidentified - 1);
        c.pending++;
      }
    });
  };

  const handleConfirmPerson = (personId: number) => {
    if (!currentUnitId) return;
    updateCompany(currentUnitId, (c) => {
      const p = c.persons?.find((x) => x.id === personId);
      if (p && p.status === '待确认') {
        p.status = '已确认';
        c.pending = Math.max(0, c.pending - 1);
        c.confirmed++;
      }
    });
  };

  const handleBatchIdentify = () => {
    if (!currentUnitId) return;
    updateCompany(currentUnitId, (c) => {
      let n = 0;
      c.persons?.forEach((p) => { if (p.status === '未识别') { p.status = '待确认'; n++; } });
      c.unidentified = Math.max(0, c.unidentified - n);
      c.pending += n;
    });
    message.success('已批量识别，进入待确认');
  };

  const handleAudit = (id: string) => {
    updateCompany(id, (c) => { c.status = '已审核'; });
    message.success('已审核通过');
  };

  const handleSendConfirm = () => {
    if (!currentUnitId) return;
    updateCompany(currentUnitId, (c) => { c.status = '已确认'; });
    message.success(`「${currentCompany?.name}」识别结果已送企业确认`);
  };

  const handleAuditDetail = () => {
    if (!currentUnitId) return;
    updateCompany(currentUnitId, (c) => { c.status = '已审核'; });
    message.success(`「${currentCompany?.name}」审核通过`);
  };

  const handleImportConfirm = () => {
    if (!importUnit) { message.warning('请选择单位'); return; }
    if (!importFileName) { message.warning('请上传 Excel 文件'); return; }
    updateCompany(importUnit, (c) => {
      const add = Math.floor(Math.random() * 30) + 20;
      c.unidentified += add;
      c.persons = undefined; // 重置人员缓存
    });
    message.success(`已导入 ${Math.floor(Math.random() * 30) + 20} 条人员数据（演示）`);
    setImportModalOpen(false);
    setImportFileName('');
    setImportUnit('');
  };

  const handleNextStep = () => {
    if (companies.some((c) => c.unidentified > 0)) {
      message.warning('仍有未识别人员');
    }
    navigateTo('zhixun-task-archives'); // 暂跳任务档案，后续替换为初步方案
  };

  // ---- 单位列表列 ----
  const companyColumns: ColumnsType<Company> = [
    { title: '单位名称', dataIndex: 'name', render: (v) => <strong>{v}</strong> },
    { title: '未识别人员', dataIndex: 'unidentified', render: (v) => <span style={{ color: '#EF4444', fontWeight: 600 }}>{v}</span> },
    { title: '待确认数量', dataIndex: 'pending', render: (v) => <span style={{ color: '#F59E0B', fontWeight: 600 }}>{v}</span> },
    { title: '已确认数量', dataIndex: 'confirmed', render: (v) => <span style={{ color: '#10B981', fontWeight: 600 }}>{v}</span> },
    {
      title: '状态', dataIndex: 'status',
      render: (v) => <span className={`status-badge ${statusClass(v)}`}>{v}</span>,
    },
    {
      title: '操作', key: 'actions',
      render: (_, c) => (
        <div className="ti-actions">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setCurrentUnitId(c.id)}>查看详情</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => message.info(`编辑单位「${c.name}」信息（演示）`)} style={{ color: '#F59E0B' }}>编辑</Button>
          {c.status === '未审核' && (
            <Button type="link" size="small" icon={<AuditOutlined />} onClick={() => handleAudit(c.id)}>审核</Button>
          )}
          <Button type="link" size="small" icon={<SendOutlined />} onClick={() => message.info(`已向「${c.name}」发送企业确认通知（演示）`)}>送企业确认</Button>
        </div>
      ),
    },
  ];

  // ---- 人员列表列 ----
  const personColumns: ColumnsType<Person> = [
    { title: '姓名', dataIndex: 'name', render: (v) => <strong>{v}</strong> },
    { title: '工号', dataIndex: 'code' },
    { title: '部门', dataIndex: 'dept' },
    { title: '工种', dataIndex: 'occ' },
    { title: '技能等级', dataIndex: 'level' },
    {
      title: '识别状态', dataIndex: 'status',
      render: (v) => {
        const cls = v === '已确认' ? 'st-audited' : v === '待确认' ? 'st-confirmed' : 'st-pending';
        return <span className={`status-badge ${cls}`}>{v}</span>;
      },
    },
    {
      title: '操作', key: 'actions',
      render: (_, p) => (
        <>
          {p.status === '未识别' && <Button type="link" size="small" onClick={() => handleIdentify(p.id)}>识别</Button>}
          {p.status === '待确认' && <Button type="link" size="small" onClick={() => handleConfirmPerson(p.id)}>确认</Button>}
        </>
      ),
    },
  ];

  return (
    <div className="talent-id-page">
      {/* 顶部栏 */}
      <header className="ti-topbar">
        <div className="ti-topbar-left">
          <a className="ti-back-link" onClick={() => navigateTo('zhixun-training-process')}>
            <ArrowLeftOutlined /> 返回培训流程
          </a>
          <div>
            <h1 className="ti-title">人才识别</h1>
            <span className="ti-sub">
              {projectTask ? `${projectTask.enterprise || projectTask.name} · 企业人才数据识别与确认 · 步骤 1 / 9` : '企业人才数据识别与确认 · 步骤 1 / 9'}
            </span>
          </div>
        </div>
        <span className="ti-process-badge"><UserSwitchOutlined /> 人才识别 → 初步方案 → …</span>
      </header>

      <main className="ti-page">
        {/* 列表面板 */}
        {!currentUnitId && (
          <>
            <div className="ti-flow-hint">
              <InfoCircleOutlined style={{ color: '#2A6DFF' }} />
              {' '}业务流程：<strong>Excel导入人员数据 → 系统技能识别 → 培训部审核 → 企业确认 → 进入初步方案</strong>
            </div>

            {/* 统计卡片 */}
            <div className="ti-stat-row">
              <div className="ti-stat-card identify">
                <div className="ti-stat-val">{stats.count}</div>
                <div className="ti-stat-lbl">参与识别单位</div>
              </div>
              <div className="ti-stat-card danger">
                <div className="ti-stat-val red">{stats.unidentified}</div>
                <div className="ti-stat-lbl">未识别人员</div>
              </div>
              <div className="ti-stat-card warn">
                <div className="ti-stat-val yellow">{stats.pending}</div>
                <div className="ti-stat-lbl">待确认数量</div>
              </div>
              <div className="ti-stat-card success">
                <div className="ti-stat-val green">{stats.confirmed}</div>
                <div className="ti-stat-lbl">已确认数量</div>
              </div>
            </div>

            {/* 单位列表 */}
            <div className="ti-card">
              <div className="ti-toolbar">
                <div className="ti-toolbar-left">
                  <div className="ti-form-inline">
                    <label>状态</label>
                    <Select
                      placeholder="全部状态"
                      value={filterStatus || undefined}
                      onChange={(v) => setFilterStatus(v || '')}
                      allowClear
                      style={{ width: 130 }}
                      options={['未审核', '已确认', '已审核'].map((v) => ({ value: v, label: v }))}
                    />
                  </div>
                  <Input
                    placeholder="搜索单位名称…"
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                    allowClear
                    style={{ width: 220 }}
                  />
                </div>
                <Button className="btn-identify" icon={<FileExcelOutlined />} onClick={() => setImportModalOpen(true)}>Excel 导入</Button>
              </div>

              <Table
                dataSource={filteredCompanies}
                columns={companyColumns}
                rowKey="id"
                pagination={false}
                locale={{ emptyText: '暂无单位数据，请先 Excel 导入或从任务档案创建项目' }}
              />

              <div className="ti-footer-actions">
                <Button icon={<UnorderedListOutlined />} onClick={() => navigateTo('zhixun-task-archives')}>任务档案</Button>
                <Button type="primary" icon={<ArrowRightOutlined />} onClick={handleNextStep}>完成识别，进入初步方案</Button>
              </div>
            </div>
          </>
        )}

        {/* 详情面板 */}
        {currentUnitId && currentCompany && (
          <div className="ti-card">
            <div className="ti-detail-header">
              <h2>{currentCompany.name} · 人员识别明细</h2>
              <Button size="small" icon={<ArrowLeftOutlined />} onClick={() => setCurrentUnitId(null)}>返回单位列表</Button>
            </div>

            <div className="ti-filter-tabs">
              {[
                { key: 'all', label: '全部' },
                { key: 'unidentified', label: '未识别' },
                { key: 'pending', label: '待确认' },
                { key: 'confirmed', label: '已确认' },
              ].map((tab) => (
                <button
                  key={tab.key}
                  className={`ti-filter-tab${personFilter === tab.key ? ' active' : ''}`}
                  onClick={() => setPersonFilter(tab.key)}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <Table
              dataSource={persons}
              columns={personColumns}
              rowKey="id"
              pagination={false}
              locale={{ emptyText: '暂无人员' }}
            />

            <div className="ti-footer-actions">
              <Button className="btn-identify" size="small" icon={<ThunderboltOutlined />} onClick={handleBatchIdentify}>批量智能识别</Button>
              <Button size="small" icon={<SendOutlined />} onClick={handleSendConfirm}>送企业确认</Button>
              <Button type="primary" size="small" icon={<CheckOutlined />} onClick={handleAuditDetail}>审核通过</Button>
            </div>
          </div>
        )}
      </main>

      {/* Excel 导入弹窗 */}
      <Modal
        open={importModalOpen}
        onCancel={() => { setImportModalOpen(false); setImportFileName(''); setImportUnit(''); }}
        title={<><FileExcelOutlined style={{ color: '#F18F01' }} /> Excel 导入</>}
        footer={
          <>
            <Button onClick={() => { setImportModalOpen(false); setImportFileName(''); setImportUnit(''); }}>取消</Button>
            <Button className="btn-identify" onClick={handleImportConfirm}>确认导入</Button>
          </>
        }
      >
        <div className="ti-form-group">
          <label>选择单位 / 企业</label>
          <Select
            placeholder="请选择单位"
            value={importUnit || undefined}
            onChange={(v) => setImportUnit(v || '')}
            style={{ width: '100%' }}
            options={companies.map((c) => ({ value: c.id, label: c.name }))}
          />
        </div>
        <div className="ti-form-group">
          <label>上传 Excel 文件</label>
          <div
            className="ti-upload-zone"
            onClick={() => {
              const input = document.createElement('input');
              input.type = 'file';
              input.accept = '.xlsx,.xls';
              input.onchange = (e) => {
                const f = (e.target as HTMLInputElement).files?.[0];
                if (f) setImportFileName(f.name);
              };
              input.click();
            }}
          >
            <CloudUploadOutlined style={{ fontSize: 36, color: '#F18F01' }} />
            <p>点击上传 Excel 文件</p>
            <p style={{ fontSize: 12, color: '#64748B', marginTop: 6 }}>支持 .xlsx / .xls 格式</p>
          </div>
          {importFileName && (
            <div className="ti-file-preview show">
              <span>{importFileName}</span>
              <Button type="link" danger size="small" icon={<CloseOutlined />} onClick={() => setImportFileName('')} />
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}
