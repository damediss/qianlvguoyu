import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useState, useMemo } from 'react';
import { Table, Button, Input, InputNumber, Modal, Form, Select, Tag, Space, message } from 'antd';
import { ArrowLeftOutlined, PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined, InboxOutlined, ToolOutlined, BookOutlined, BankOutlined, ShopOutlined, EnterOutlined, AreaChartOutlined, } from '@ant-design/icons';
import { useNavStore } from '../../stores/navStore';
import ReactECharts from 'echarts-for-react';
import * as echarts from 'echarts';
import './ArchivePages.css';
const INIT_DATA = {
    equipment: [
        { id: 'EQ01', name: '电工实训台', model: 'DG-200A', qty: 24, rate: 96, nextMaintain: '2026-06-15', status: '正常' },
        { id: 'EQ02', name: '焊机', model: 'NBC-350', qty: 18, rate: 88, nextMaintain: '2026-05-20', status: '正常' },
        { id: 'EQ03', name: '起重机模拟器', model: 'QZ-SIM3', qty: 2, rate: 72, nextMaintain: '2026-04-10', status: '维修' },
        { id: 'EQ04', name: 'PLC实验装置', model: 'PLC-Lab', qty: 12, rate: 100, nextMaintain: '2026-08-01', status: '正常' },
        { id: 'EQ05', name: '安全防护套装', model: 'SAFE-KIT', qty: 50, rate: 0, nextMaintain: '—', status: '报废' },
    ],
    materials: [
        { id: 'M001', name: '焊条', spec: 'J422 φ3.2', stock: 120, safety: 100, unit: 'kg', lastIn: '2026-05-01' },
        { id: 'M002', name: '电工教材', spec: '2025版高级', stock: 45, safety: 50, unit: '册', lastIn: '2026-04-18' },
        { id: 'M003', name: '安全帽', spec: 'ABS标准', stock: 28, safety: 30, unit: '顶', lastIn: '2026-03-22' },
        { id: 'M004', name: '实训手套', spec: '防割五级', stock: 200, safety: 80, unit: '双', lastIn: '2026-05-10' },
        { id: 'M005', name: '绝缘胶带', spec: 'PVC 20m', stock: 15, safety: 40, unit: '卷', lastIn: '2026-02-15' },
    ],
    rules: [
        { id: 'R01', name: '教学方法SOP', version: 'V2.1', effective: '2025-01-01', file: '教学方法SOP.pdf', summary: '规定理实一体授课流程：课前准备→理论讲授→示范演示→学员实操→点评总结。' },
        { id: 'R02', name: '考核评分规则', version: 'V3.0', effective: '2025-06-01', file: '考核评分规则.pdf', summary: '理论30%、实操50%、安全规范20%。' },
        { id: 'R03', name: '安全操作规范', version: 'V1.8', effective: '2024-12-01', file: '安全操作规范.pdf', summary: '进入实训区须穿戴劳保；设备使用前点检。' },
        { id: 'R04', name: '学员考勤管理办法', version: 'V1.2', effective: '2025-03-15', file: '考勤办法.pdf', summary: '缺课超20%不得参加结业考核。' },
    ],
    environment: {
        bases: [
            { id: 'B1', name: '总部培训中心', address: '南宁市青秀区实训路88号', area: 3200, capacity: 200, utilization: 76 },
            { id: 'B2', name: '华东实训基地', address: '柳州市鱼峰区技能大道12号', area: 4800, capacity: 280, utilization: 68 },
            { id: 'B3', name: '桂林教学点', address: '桂林市七星区教育园5号', area: 1200, capacity: 80, utilization: 45 },
        ],
        rooms: [
            { id: 'RM01', name: '电工实训室A', base: '总部培训中心', status: '可用' },
            { id: 'RM02', name: '焊工实训室B', base: '总部培训中心', status: '占用' },
            { id: 'RM03', name: '理论教室301', base: '总部培训中心', status: '可用' },
            { id: 'RM04', name: '起重模拟厅', base: '华东实训基地', status: '维修' },
            { id: 'RM05', name: '多媒体教室201', base: '桂林教学点', status: '可用' },
        ],
    },
};
const PAGE_META = {
    equipment: { title: '教学设备', sub: '设备台账、完好率与保养计划', tag: '项目档案 · 机', icon: _jsx(ToolOutlined, {}) },
    materials: { title: '物料台账', sub: '库存管理、安全库存与出入库', tag: '项目档案 · 料', icon: _jsx(InboxOutlined, {}) },
    rules: { title: '方法规则', sub: 'SOP、考核规范与安全制度文档', tag: '项目档案 · 法', icon: _jsx(BookOutlined, {}) },
    environment: { title: '环境配置', sub: '实训基地、场地与环境满意度', tag: '项目档案 · 环', icon: _jsx(BankOutlined, {}) },
};
const STATUS_TAG = {
    '正常': 'success', '维修': 'warning', '报废': 'error', '可用': 'success', '占用': 'warning',
};
export function createArchiveTablePage(module) {
    return function ArchiveTablePageInner() {
        const navigateTo = useNavStore((s) => s.setSelectedKey);
        const [data, setData] = useState(() => {
            if (module === 'environment')
                return { ...INIT_DATA.environment };
            return [...INIT_DATA[module]];
        });
        const [search, setSearch] = useState('');
        const [modalOpen, setModalOpen] = useState(false);
        const [modalMode, setModalMode] = useState('add');
        const [editingId, setEditingId] = useState(null);
        const [form] = Form.useForm();
        const [rulePreview, setRulePreview] = useState('点击规则名称查看摘要。');
        const [envEntity, setEnvEntity] = useState('bases');
        const meta = PAGE_META[module];
        /* ---- search ---- */
        const filtered = useMemo(() => {
            if (module === 'environment') {
                const q = search.toLowerCase();
                return {
                    bases: data.bases.filter((b) => ['name', 'address'].some(k => String(b[k]).toLowerCase().includes(q))),
                    rooms: data.rooms.filter((r) => ['name', 'base'].some(k => String(r[k]).toLowerCase().includes(q))),
                };
            }
            if (!search)
                return data;
            const q = search.toLowerCase();
            const keys = module === 'equipment' ? ['name', 'model', 'id'] : module === 'materials' ? ['name', 'id', 'spec'] : ['name'];
            return data.filter((row) => keys.some((k) => String(row[k] ?? '').toLowerCase().includes(q)));
        }, [data, search]);
        const getList = () => (module === 'environment' ? filtered : filtered);
        /* ---- modal ---- */
        const openModal = (mode, id, entity) => {
            setModalMode(mode);
            setEditingId(id || null);
            if (module === 'environment' && entity)
                setEnvEntity(entity);
            if (mode === 'edit' && id) {
                const list = module === 'environment' ? [...data.bases, ...data.rooms] : data;
                const record = list.find((x) => x.id === id);
                if (record)
                    form.setFieldsValue(record);
            }
            else {
                form.resetFields();
                if (module === 'equipment')
                    form.setFieldsValue({ id: 'EQ' + Date.now(), qty: 1, rate: 100, status: '正常' });
                else if (module === 'materials')
                    form.setFieldsValue({ id: 'M' + Date.now(), stock: 0, safety: 0 });
                else if (module === 'rules')
                    form.setFieldsValue({ id: 'R' + Date.now(), version: 'V1.0' });
                else if (module === 'environment' && entity === 'bases')
                    form.setFieldsValue({ id: 'B' + Date.now(), name: '', address: '', area: 0, capacity: 0, utilization: 0 });
                else if (module === 'environment' && entity === 'rooms')
                    form.setFieldsValue({ id: 'RM' + Date.now(), name: '', base: '总部培训中心', status: '可用' });
            }
            setModalOpen(true);
        };
        const handleSave = () => {
            form.validateFields().then((vals) => {
                if (module === 'environment') {
                    if (modalMode === 'add') {
                        setData((prev) => ({ ...prev, [envEntity]: [...prev[envEntity], vals] }));
                    }
                    else {
                        setData((prev) => ({
                            ...prev,
                            [envEntity]: prev[envEntity].map((x) => x.id === editingId ? { ...x, ...vals } : x),
                        }));
                    }
                }
                else {
                    if (modalMode === 'add')
                        setData((prev) => [...prev, vals]);
                    else
                        setData((prev) => prev.map((x) => x.id === editingId ? { ...x, ...vals } : x));
                }
                setModalOpen(false);
                message.success(modalMode === 'add' ? '新增成功' : '保存成功');
            });
        };
        const handleDelete = (id, entity) => {
            if (module === 'environment' && entity) {
                setData((prev) => ({ ...prev, [entity]: prev[entity].filter((x) => x.id !== id) }));
            }
            else if (module !== 'environment') {
                setData((prev) => prev.filter((x) => x.id !== id));
            }
            message.success('已删除');
        };
        /* ---- columns ---- */
        const equipmentColumns = [
            { title: '设备ID', dataIndex: 'id' }, { title: '设备名称', dataIndex: 'name' },
            { title: '型号', dataIndex: 'model' }, { title: '数量', dataIndex: 'qty' },
            { title: '完好率', dataIndex: 'rate', render: (v) => `${v}%` },
            { title: '下次保养', dataIndex: 'nextMaintain' },
            { title: '状态', dataIndex: 'status', render: (v) => _jsx(Tag, { color: STATUS_TAG[v] || 'default', children: v }) },
            { title: '操作', width: 120, render: (_, r) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openModal('edit', r.id) }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r.id) })] })) },
        ];
        const materialColumns = [
            { title: '物料编码', dataIndex: 'id' }, { title: '名称', dataIndex: 'name' }, { title: '规格', dataIndex: 'spec' },
            { title: '库存', dataIndex: 'stock', render: (v, r) => _jsx("strong", { style: { color: v < r.safety ? '#EF4444' : undefined }, children: v }) },
            { title: '安全库存', dataIndex: 'safety' }, { title: '单位', dataIndex: 'unit' }, { title: '上次入库', dataIndex: 'lastIn' },
            { title: '操作', width: 200, render: (_, r) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", onClick: () => {
                                setData((prev) => prev.map(m => m.id === r.id ? { ...m, stock: Math.max(0, m.stock + 10), lastIn: new Date().toISOString().slice(0, 10) } : m));
                            }, children: "+\u5165\u5E93" }), _jsx(Button, { size: "small", onClick: () => {
                                setData((prev) => prev.map(m => m.id === r.id ? { ...m, stock: Math.max(0, m.stock - 10) } : m));
                            }, children: "-\u51FA\u5E93" }), _jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openModal('edit', r.id) }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r.id) })] })) },
        ];
        const ruleColumns = [
            { title: '规则名称', dataIndex: 'name', render: (v, r) => _jsx("a", { className: "pa-link", onClick: () => setRulePreview(r.summary), children: v }) },
            { title: '版本', dataIndex: 'version' }, { title: '生效日期', dataIndex: 'effective' },
            { title: '附件', dataIndex: 'file', render: (v) => _jsx("a", { className: "pa-link", children: v }) },
            { title: '操作', width: 120, render: (_, r) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openModal('edit', r.id) }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r.id) })] })) },
        ];
        const baseColumns = [
            { title: '基地', dataIndex: 'name' }, { title: '地址', dataIndex: 'address' },
            { title: '面积(㎡)', dataIndex: 'area' }, { title: '容量', dataIndex: 'capacity' },
            { title: '利用率%', dataIndex: 'utilization', render: (v, r) => (_jsx(InputNumber, { min: 0, max: 100, value: v, size: "small", style: { width: 72 }, onChange: (val) => {
                        setData((prev) => ({ ...prev, bases: prev.bases.map((b) => b.id === r.id ? { ...b, utilization: val ?? 0 } : b) }));
                    } })) },
            { title: '操作', width: 120, render: (_, r) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openModal('edit', r.id, 'bases') }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r.id, 'bases') })] })) },
        ];
        const roomColumns = [
            { title: '场地', dataIndex: 'name' }, { title: '基地', dataIndex: 'base' },
            { title: '状态', dataIndex: 'status', render: (v) => _jsx(Tag, { color: STATUS_TAG[v] || 'default', children: v }) },
            { title: '操作', width: 120, render: (_, r) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openModal('edit', r.id, 'rooms') }), _jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => handleDelete(r.id, 'rooms') })] })) },
        ];
        return (_jsxs("div", { className: "pa-page", children: [_jsxs("header", { className: "pa-topbar", children: [_jsxs("div", { className: "pa-topbar-left", children: [_jsxs("a", { className: "pa-back-link", onClick: () => navigateTo('zhixun-project-archives'), children: [_jsx(ArrowLeftOutlined, {}), " \u8FD4\u56DE\u9879\u76EE\u6863\u6848"] }), _jsxs("div", { children: [_jsx("h1", { children: meta.title }), _jsx("span", { className: "pa-sub", children: meta.sub })] })] }), _jsxs("span", { className: "pa-topbar-tag", children: [meta.icon, " ", meta.tag] })] }), _jsx("main", { className: "pa-main", children: module === 'environment' ? (_jsxs(_Fragment, { children: [_jsxs("div", { className: "pa-grid-2", children: [_jsxs("div", { className: "pa-card", children: [_jsxs("div", { className: "pa-card-head", children: [_jsxs("div", { className: "pa-title-wrap", children: [_jsx(ShopOutlined, {}), " \u5B9E\u8BAD\u57FA\u5730"] }), _jsx(Button, { type: "primary", size: "small", icon: _jsx(PlusOutlined, {}), onClick: () => openModal('add', undefined, 'bases'), children: "\u65B0\u589E" })] }), _jsx("div", { className: "pa-table-wrap", children: _jsx(Table, { columns: baseColumns, dataSource: data.bases, rowKey: "id", size: "small", pagination: false }) })] }), _jsxs("div", { className: "pa-card", children: [_jsxs("div", { className: "pa-card-head", children: [_jsxs("div", { className: "pa-title-wrap", children: [_jsx(EnterOutlined, {}), " \u6559\u5BA4/\u573A\u5730"] }), _jsx(Button, { type: "primary", size: "small", icon: _jsx(PlusOutlined, {}), onClick: () => openModal('add', undefined, 'rooms'), children: "\u65B0\u589E" })] }), _jsx("div", { className: "pa-table-wrap", children: _jsx(Table, { columns: roomColumns, dataSource: data.rooms, rowKey: "id", size: "small", pagination: false }) })] })] }), _jsxs("div", { className: "pa-card", children: [_jsx("div", { className: "pa-card-head", children: _jsxs("div", { className: "pa-title-wrap", children: [_jsx(AreaChartOutlined, {}), " \u73AF\u5883\u6EE1\u610F\u5EA6"] }) }), _jsx(ReactECharts, { echarts: echarts, style: { height: 300 }, option: {
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
                                        } })] })] })) : (_jsxs("div", { className: "pa-card", children: [_jsx("div", { className: "pa-card-head", children: _jsxs("div", { className: "pa-title-wrap", children: [meta.icon, " ", meta.title] }) }), _jsxs("div", { className: "pa-toolbar", children: [_jsx(Input, { prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u2026", value: search, onChange: e => setSearch(e.target.value), className: "pa-search-input", allowClear: true }), _jsx("div", { className: "pa-toolbar-actions", children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => openModal('add'), children: "\u65B0\u589E" }) })] }), _jsxs("div", { className: "pa-table-wrap", children: [module === 'equipment' && _jsx(Table, { columns: equipmentColumns, dataSource: getList(), rowKey: "id", size: "small", scroll: { x: 900 }, pagination: { defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] } }), module === 'materials' && _jsx(Table, { columns: materialColumns, dataSource: getList(), rowKey: "id", size: "small", scroll: { x: 1000 }, pagination: { defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }, rowClassName: (r) => r.stock < r.safety ? 'row-warn' : '' }), module === 'rules' && (_jsxs(_Fragment, { children: [_jsx(Table, { columns: ruleColumns, dataSource: getList(), rowKey: "id", size: "small", pagination: { defaultPageSize: 10, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] } }), _jsxs("div", { className: "pa-rule-preview", children: [_jsx("h4", { children: "\u89C4\u5219\u6458\u8981\u9884\u89C8" }), _jsx("p", { children: rulePreview })] })] }))] })] })) }), _jsx(Modal, { title: `${modalMode === 'add' ? '新增' : '编辑'}${module === 'environment' ? (envEntity === 'bases' ? '基地' : '场地') : ' — ' + meta.title}`, open: modalOpen, onOk: handleSave, onCancel: () => setModalOpen(false), destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", children: [module === 'equipment' && (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "id", label: "\u8BBE\u5907ID", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "name", label: "\u8BBE\u5907\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "model", label: "\u578B\u53F7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "qty", label: "\u6570\u91CF", children: _jsx(Input, { type: "number" }) }), _jsx(Form.Item, { name: "rate", label: "\u5B8C\u597D\u7387%", children: _jsx(Input, { type: "number" }) }), _jsx(Form.Item, { name: "nextMaintain", label: "\u4E0B\u6B21\u4FDD\u517B\u65E5\u671F", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: ['正常', '维修', '报废'].map(v => ({ label: v, value: v })) }) })] })), module === 'materials' && (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "id", label: "\u7269\u6599\u7F16\u7801", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "name", label: "\u7269\u6599\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "spec", label: "\u89C4\u683C", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "stock", label: "\u5E93\u5B58\u91CF", children: _jsx(Input, { type: "number" }) }), _jsx(Form.Item, { name: "safety", label: "\u5B89\u5168\u5E93\u5B58", children: _jsx(Input, { type: "number" }) }), _jsx(Form.Item, { name: "unit", label: "\u5355\u4F4D", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "lastIn", label: "\u4E0A\u6B21\u5165\u5E93\u65E5\u671F", children: _jsx(Input, {}) })] })), module === 'rules' && (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "id", label: "\u89C4\u5219ID", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "name", label: "\u89C4\u5219\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "version", label: "\u7248\u672C", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "effective", label: "\u751F\u6548\u65E5\u671F", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "file", label: "\u9644\u4EF6\u6587\u4EF6\u540D", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "summary", label: "\u6458\u8981", children: _jsx(Input.TextArea, { rows: 3 }) })] })), module === 'environment' && envEntity === 'bases' && (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "id", label: "ID", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "name", label: "\u57FA\u5730\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "address", label: "\u5730\u5740", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "area", label: "\u9762\u79EF(\u33A1)", children: _jsx(Input, { type: "number" }) }), _jsx(Form.Item, { name: "capacity", label: "\u5BB9\u91CF", children: _jsx(Input, { type: "number" }) }), _jsx(Form.Item, { name: "utilization", label: "\u5229\u7528\u7387%", children: _jsx(Input, { type: "number" }) })] })), module === 'environment' && envEntity === 'rooms' && (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "id", label: "ID", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "name", label: "\u573A\u5730\u540D\u79F0", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "base", label: "\u57FA\u5730", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", children: _jsx(Select, { options: ['可用', '占用', '维修'].map(v => ({ label: v, value: v })) }) })] }))] }) })] }));
    };
}
