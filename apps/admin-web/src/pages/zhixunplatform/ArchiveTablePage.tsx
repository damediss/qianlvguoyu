import { useState, useMemo } from 'react';
import { Table, Button, Input, InputNumber, Modal, Form, Select, Tag, Space, message } from 'antd';
import {
  ArrowLeftOutlined,
  PlusOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  InboxOutlined,
  ToolOutlined,
  BookOutlined,
  BankOutlined,
  ShopOutlined,
  EnterOutlined,
  AreaChartOutlined,
} from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import type { ColumnsType } from 'antd/es/table';
import ReactECharts from 'echarts-for-react';
import * as echarts from 'echarts';
import './ArchivePages.css';

/* ============================================================
   模拟数据（对照 archive-data.js）
   ============================================================ */

interface Equipment {
  id: string; name: string; model: string; qty: number; rate: number; nextMaintain: string; status: string;
}
interface Material {
  id: string; name: string; spec: string; stock: number; safety: number; unit: string; lastIn: string;
}
interface Rule {
  id: string; name: string; version: string; effective: string; file: string; summary: string;
}
interface Base {
  id: string; name: string; address: string; area: number; capacity: number; utilization: number;
}
interface Room {
  id: string; name: string; base: string; status: string;
}

const INIT_DATA = {
  equipment: [
    { id: 'EQ01', name: '电工实训台', model: 'DG-200A', qty: 24, rate: 96, nextMaintain: '2026-06-15', status: '正常' },
    { id: 'EQ02', name: '焊机', model: 'NBC-350', qty: 18, rate: 88, nextMaintain: '2026-05-20', status: '正常' },
    { id: 'EQ03', name: '起重机模拟器', model: 'QZ-SIM3', qty: 2, rate: 72, nextMaintain: '2026-04-10', status: '维修' },
    { id: 'EQ04', name: 'PLC实验装置', model: 'PLC-Lab', qty: 12, rate: 100, nextMaintain: '2026-08-01', status: '正常' },
    { id: 'EQ05', name: '安全防护套装', model: 'SAFE-KIT', qty: 50, rate: 0, nextMaintain: '—', status: '报废' },
  ] as Equipment[],
  materials: [
    { id: 'M001', name: '焊条', spec: 'J422 φ3.2', stock: 120, safety: 100, unit: 'kg', lastIn: '2026-05-01' },
    { id: 'M002', name: '电工教材', spec: '2025版高级', stock: 45, safety: 50, unit: '册', lastIn: '2026-04-18' },
    { id: 'M003', name: '安全帽', spec: 'ABS标准', stock: 28, safety: 30, unit: '顶', lastIn: '2026-03-22' },
    { id: 'M004', name: '实训手套', spec: '防割五级', stock: 200, safety: 80, unit: '双', lastIn: '2026-05-10' },
    { id: 'M005', name: '绝缘胶带', spec: 'PVC 20m', stock: 15, safety: 40, unit: '卷', lastIn: '2026-02-15' },
  ] as Material[],
  rules: [
    { id: 'R01', name: '教学方法SOP', version: 'V2.1', effective: '2025-01-01', file: '教学方法SOP.pdf', summary: '规定理实一体授课流程：课前准备→理论讲授→示范演示→学员实操→点评总结。' },
    { id: 'R02', name: '考核评分规则', version: 'V3.0', effective: '2025-06-01', file: '考核评分规则.pdf', summary: '理论30%、实操50%、安全规范20%。' },
    { id: 'R03', name: '安全操作规范', version: 'V1.8', effective: '2024-12-01', file: '安全操作规范.pdf', summary: '进入实训区须穿戴劳保；设备使用前点检。' },
    { id: 'R04', name: '学员考勤管理办法', version: 'V1.2', effective: '2025-03-15', file: '考勤办法.pdf', summary: '缺课超20%不得参加结业考核。' },
  ] as Rule[],
  environment: {
    bases: [
      { id: 'B1', name: '总部培训中心', address: '南宁市青秀区实训路88号', area: 3200, capacity: 200, utilization: 76 },
      { id: 'B2', name: '华东实训基地', address: '柳州市鱼峰区技能大道12号', area: 4800, capacity: 280, utilization: 68 },
      { id: 'B3', name: '桂林教学点', address: '桂林市七星区教育园5号', area: 1200, capacity: 80, utilization: 45 },
    ] as Base[],
    rooms: [
      { id: 'RM01', name: '电工实训室A', base: '总部培训中心', status: '可用' },
      { id: 'RM02', name: '焊工实训室B', base: '总部培训中心', status: '占用' },
      { id: 'RM03', name: '理论教室301', base: '总部培训中心', status: '可用' },
      { id: 'RM04', name: '起重模拟厅', base: '华东实训基地', status: '维修' },
      { id: 'RM05', name: '多媒体教室201', base: '桂林教学点', status: '可用' },
    ] as Room[],
  },
};

const PAGE_META: Record<string, { title: string; sub: string; tag: string; icon: React.ReactNode }> = {
  equipment: { title: '教学设备', sub: '设备台账、完好率与保养计划', tag: '项目档案 · 机', icon: <ToolOutlined /> },
  materials: { title: '物料台账', sub: '库存管理、安全库存与出入库', tag: '项目档案 · 料', icon: <InboxOutlined /> },
  rules: { title: '方法规则', sub: 'SOP、考核规范与安全制度文档', tag: '项目档案 · 法', icon: <BookOutlined /> },
  environment: { title: '环境配置', sub: '实训基地、场地与环境满意度', tag: '项目档案 · 环', icon: <BankOutlined /> },
};

const STATUS_TAG: Record<string, string> = {
  '正常': 'success', '维修': 'warning', '报废': 'error', '可用': 'success', '占用': 'warning',
};

type ModuleKey = 'equipment' | 'materials' | 'rules' | 'environment';

interface Props { module: ModuleKey }

export function createArchiveTablePage(module: ModuleKey) {
  return function ArchiveTablePageInner() {
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const [data, setData] = useState<any>(() => {
      if (module === 'environment') return { ...INIT_DATA.environment };
      return [...INIT_DATA[module as 'equipment' | 'materials' | 'rules']];
    });
    const [search, setSearch] = useState('');
    const [modalOpen, setModalOpen] = useState(false);
    const [modalMode, setModalMode] = useState<'add' | 'edit'>('add');
    const [editingId, setEditingId] = useState<string | null>(null);
    const [form] = Form.useForm();
    const [rulePreview, setRulePreview] = useState('点击规则名称查看摘要。');
    const [envEntity, setEnvEntity] = useState<'bases' | 'rooms'>('bases');

    const meta = PAGE_META[module];

    /* ---- search ---- */
    const filtered = useMemo(() => {
      if (module === 'environment') {
        const q = search.toLowerCase();
        return {
          bases: data.bases.filter((b: Base) => ['name', 'address'].some(k => String((b as any)[k]).toLowerCase().includes(q))),
          rooms: data.rooms.filter((r: Room) => ['name', 'base'].some(k => String((r as any)[k]).toLowerCase().includes(q))),
        };
      }
      if (!search) return data;
      const q = search.toLowerCase();
      const keys = module === 'equipment' ? ['name', 'model', 'id'] : module === 'materials' ? ['name', 'id', 'spec'] : ['name'];
      return data.filter((row: any) => keys.some((k: string) => String(row[k] ?? '').toLowerCase().includes(q)));
    }, [data, search]);

    const getList = (): any[] => (module === 'environment' ? filtered : filtered);

    /* ---- modal ---- */
    const openModal = (mode: 'add' | 'edit', id?: string, entity?: string) => {
      setModalMode(mode); setEditingId(id || null);
      if (module === 'environment' && entity) setEnvEntity(entity as 'bases' | 'rooms');
      if (mode === 'edit' && id) {
        const list = module === 'environment' ? [...data.bases, ...data.rooms] : data;
        const record = list.find((x: any) => x.id === id);
        if (record) form.setFieldsValue(record);
      } else {
        form.resetFields();
        if (module === 'equipment') form.setFieldsValue({ id: 'EQ' + Date.now(), qty: 1, rate: 100, status: '正常' });
        else if (module === 'materials') form.setFieldsValue({ id: 'M' + Date.now(), stock: 0, safety: 0 });
        else if (module === 'rules') form.setFieldsValue({ id: 'R' + Date.now(), version: 'V1.0' });
        else if (module === 'environment' && entity === 'bases') form.setFieldsValue({ id: 'B' + Date.now(), name: '', address: '', area: 0, capacity: 0, utilization: 0 });
        else if (module === 'environment' && entity === 'rooms') form.setFieldsValue({ id: 'RM' + Date.now(), name: '', base: '总部培训中心', status: '可用' });
      }
      setModalOpen(true);
    };

    const handleSave = () => {
      form.validateFields().then((vals: any) => {
        if (module === 'environment') {
          if (modalMode === 'add') {
            setData((prev: any) => ({ ...prev, [envEntity]: [...prev[envEntity], vals] }));
          } else {
            setData((prev: any) => ({
              ...prev,
              [envEntity]: prev[envEntity].map((x: any) => x.id === editingId ? { ...x, ...vals } : x),
            }));
          }
        } else {
          if (modalMode === 'add') setData((prev: any[]) => [...prev, vals]);
          else setData((prev: any[]) => prev.map((x: any) => x.id === editingId ? { ...x, ...vals } : x));
        }
        setModalOpen(false);
        message.success(modalMode === 'add' ? '新增成功' : '保存成功');
      });
    };

    const handleDelete = (id: string, entity?: string) => {
      if (module === 'environment' && entity) {
        setData((prev: any) => ({ ...prev, [entity]: prev[entity].filter((x: any) => x.id !== id) }));
      } else if (module !== 'environment') {
        setData((prev: any[]) => prev.filter((x: any) => x.id !== id));
      }
      message.success('已删除');
    };

    /* ---- columns ---- */
    const equipmentColumns: ColumnsType<Equipment> = [
      { title: '设备ID', dataIndex: 'id' }, { title: '设备名称', dataIndex: 'name' },
      { title: '型号', dataIndex: 'model' }, { title: '数量', dataIndex: 'qty' },
      { title: '完好率', dataIndex: 'rate', render: (v) => `${v}%` },
      { title: '下次保养', dataIndex: 'nextMaintain' },
      { title: '状态', dataIndex: 'status', render: (v) => <Tag color={STATUS_TAG[v] || 'default'}>{v}</Tag> },
      { title: '操作', width: 120, render: (_, r) => (
        <Space size={4}><Button size="small" icon={<EditOutlined />} onClick={() => openModal('edit', r.id)} /><Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id)} /></Space>
      )},
    ];

    const materialColumns: ColumnsType<Material> = [
      { title: '物料编码', dataIndex: 'id' }, { title: '名称', dataIndex: 'name' }, { title: '规格', dataIndex: 'spec' },
      { title: '库存', dataIndex: 'stock', render: (v, r) => <strong style={{ color: v < r.safety ? '#EF4444' : undefined }}>{v}</strong> },
      { title: '安全库存', dataIndex: 'safety' }, { title: '单位', dataIndex: 'unit' }, { title: '上次入库', dataIndex: 'lastIn' },
      { title: '操作', width: 200, render: (_, r) => (
        <Space size={4}>
          <Button size="small" onClick={() => {
            setData((prev: Material[]) => prev.map(m => m.id === r.id ? { ...m, stock: Math.max(0, m.stock + 10), lastIn: new Date().toISOString().slice(0, 10) } : m));
          }}>+入库</Button>
          <Button size="small" onClick={() => {
            setData((prev: Material[]) => prev.map(m => m.id === r.id ? { ...m, stock: Math.max(0, m.stock - 10) } : m));
          }}>-出库</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openModal('edit', r.id)} />
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id)} />
        </Space>
      )},
    ];

    const ruleColumns: ColumnsType<Rule> = [
      { title: '规则名称', dataIndex: 'name', render: (v, r) => <a className="pa-link" onClick={() => setRulePreview(r.summary)}>{v}</a> },
      { title: '版本', dataIndex: 'version' }, { title: '生效日期', dataIndex: 'effective' },
      { title: '附件', dataIndex: 'file', render: (v) => <a className="pa-link">{v}</a> },
      { title: '操作', width: 120, render: (_, r) => (
        <Space size={4}><Button size="small" icon={<EditOutlined />} onClick={() => openModal('edit', r.id)} /><Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id)} /></Space>
      )},
    ];

    const baseColumns: ColumnsType<Base> = [
      { title: '基地', dataIndex: 'name' }, { title: '地址', dataIndex: 'address' },
      { title: '面积(㎡)', dataIndex: 'area' }, { title: '容量', dataIndex: 'capacity' },
      { title: '利用率%', dataIndex: 'utilization', render: (v, r) => (
        <InputNumber min={0} max={100} value={v} size="small" style={{ width: 72 }}
          onChange={(val) => {
            setData((prev: any) => ({ ...prev, bases: prev.bases.map((b: Base) => b.id === r.id ? { ...b, utilization: val ?? 0 } : b) }));
          }}
        />
      )},
      { title: '操作', width: 120, render: (_, r) => (
        <Space size={4}><Button size="small" icon={<EditOutlined />} onClick={() => openModal('edit', r.id, 'bases')} /><Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id, 'bases')} /></Space>
      )},
    ];

    const roomColumns: ColumnsType<Room> = [
      { title: '场地', dataIndex: 'name' }, { title: '基地', dataIndex: 'base' },
      { title: '状态', dataIndex: 'status', render: (v) => <Tag color={STATUS_TAG[v] || 'default'}>{v}</Tag> },
      { title: '操作', width: 120, render: (_, r) => (
        <Space size={4}><Button size="small" icon={<EditOutlined />} onClick={() => openModal('edit', r.id, 'rooms')} /><Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id, 'rooms')} /></Space>
      )},
    ];

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
          {module === 'environment' ? (
            <>
              <div className="pa-grid-2">
                <div className="pa-card">
                  <div className="pa-card-head">
                    <div className="pa-title-wrap"><ShopOutlined /> 实训基地</div>
                    <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => openModal('add', undefined, 'bases')}>新增</Button>
                  </div>
                  <div className="pa-table-wrap">
                    <Table columns={baseColumns} dataSource={data.bases} rowKey="id" size="small" pagination={false} />
                  </div>
                </div>
                <div className="pa-card">
                  <div className="pa-card-head">
                    <div className="pa-title-wrap"><EnterOutlined /> 教室/场地</div>
                    <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => openModal('add', undefined, 'rooms')}>新增</Button>
                  </div>
                  <div className="pa-table-wrap">
                    <Table columns={roomColumns} dataSource={data.rooms} rowKey="id" size="small" pagination={false} />
                  </div>
                </div>
              </div>
              <div className="pa-card">
                <div className="pa-card-head">
                  <div className="pa-title-wrap"><AreaChartOutlined /> 环境满意度</div>
                </div>
                <ReactECharts
                  echarts={echarts}
                  style={{ height: 300 }}
                  option={{
                    tooltip: {},
                    radar: {
                      indicator: [
                        { name: '卫生', max: 100 }, { name: '设备', max: 100 }, { name: '网络', max: 100 },
                        { name: '安全', max: 100 }, { name: '舒适度', max: 100 },
                      ],
                    },
                    series: [{
                      type: 'radar',
                      data: [
                        { value: [88, 82, 75, 92, 85], name: '总部培训中心' },
                        { value: [80, 78, 70, 90, 80], name: '华东实训基地' },
                      ],
                    }],
                  }}
                />
              </div>
            </>
          ) : (
            <div className="pa-card">
              <div className="pa-card-head">
                <div className="pa-title-wrap">{meta.icon} {meta.title}</div>
              </div>
              <div className="pa-toolbar">
                <Input prefix={<SearchOutlined />} placeholder="搜索…" value={search} onChange={e => setSearch(e.target.value)} className="pa-search-input" allowClear />
                <div className="pa-toolbar-actions">
                  <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal('add')}>新增</Button>
                </div>
              </div>
              <div className="pa-table-wrap">
                {module === 'equipment' && <Table columns={equipmentColumns} dataSource={getList()} rowKey="id" size="small" scroll={{ x: 900 }} pagination={{ defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }} />}
                {module === 'materials' && <Table columns={materialColumns} dataSource={getList()} rowKey="id" size="small" scroll={{ x: 1000 }} pagination={{ defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }} rowClassName={(r: Material) => r.stock < r.safety ? 'row-warn' : ''} />}
                {module === 'rules' && (
                  <>
                    <Table columns={ruleColumns} dataSource={getList()} rowKey="id" size="small" pagination={{ defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }} />
                    <div className="pa-rule-preview"><h4>规则摘要预览</h4><p>{rulePreview}</p></div>
                  </>
                )}
              </div>
            </div>
          )}
        </main>

        <Modal title={`${modalMode === 'add' ? '新增' : '编辑'}${module === 'environment' ? (envEntity === 'bases' ? '基地' : '场地') : ' — ' + meta.title}`} open={modalOpen} onOk={handleSave} onCancel={() => setModalOpen(false)} destroyOnHidden>
          <Form form={form} layout="vertical">
            {module === 'equipment' && (
              <>
                <Form.Item name="id" label="设备ID"><Input /></Form.Item>
                <Form.Item name="name" label="设备名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="model" label="型号"><Input /></Form.Item>
                <Form.Item name="qty" label="数量"><Input type="number" /></Form.Item>
                <Form.Item name="rate" label="完好率%"><Input type="number" /></Form.Item>
                <Form.Item name="nextMaintain" label="下次保养日期"><Input /></Form.Item>
                <Form.Item name="status" label="状态"><Select options={['正常', '维修', '报废'].map(v => ({ label: v, value: v }))} /></Form.Item>
              </>
            )}
            {module === 'materials' && (
              <>
                <Form.Item name="id" label="物料编码"><Input /></Form.Item>
                <Form.Item name="name" label="物料名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="spec" label="规格"><Input /></Form.Item>
                <Form.Item name="stock" label="库存量"><Input type="number" /></Form.Item>
                <Form.Item name="safety" label="安全库存"><Input type="number" /></Form.Item>
                <Form.Item name="unit" label="单位"><Input /></Form.Item>
                <Form.Item name="lastIn" label="上次入库日期"><Input /></Form.Item>
              </>
            )}
            {module === 'rules' && (
              <>
                <Form.Item name="id" label="规则ID"><Input /></Form.Item>
                <Form.Item name="name" label="规则名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="version" label="版本"><Input /></Form.Item>
                <Form.Item name="effective" label="生效日期"><Input /></Form.Item>
                <Form.Item name="file" label="附件文件名"><Input /></Form.Item>
                <Form.Item name="summary" label="摘要"><Input.TextArea rows={3} /></Form.Item>
              </>
            )}
            {module === 'environment' && envEntity === 'bases' && (
              <>
                <Form.Item name="id" label="ID"><Input /></Form.Item>
                <Form.Item name="name" label="基地名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="address" label="地址"><Input /></Form.Item>
                <Form.Item name="area" label="面积(㎡)"><Input type="number" /></Form.Item>
                <Form.Item name="capacity" label="容量"><Input type="number" /></Form.Item>
                <Form.Item name="utilization" label="利用率%"><Input type="number" /></Form.Item>
              </>
            )}
            {module === 'environment' && envEntity === 'rooms' && (
              <>
                <Form.Item name="id" label="ID"><Input /></Form.Item>
                <Form.Item name="name" label="场地名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="base" label="基地"><Input /></Form.Item>
                <Form.Item name="status" label="状态"><Select options={['可用', '占用', '维修'].map(v => ({ label: v, value: v }))} /></Form.Item>
              </>
            )}
          </Form>
        </Modal>
      </div>
    );
  };
}
