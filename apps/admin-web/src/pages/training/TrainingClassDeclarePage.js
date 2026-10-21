import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 开班申报 — 对齐 yun_shang classApplication.vue
 * 班级列表 + 材料模板管理 + 班级材料挂载 + 生成材料包
 */
import { useMemo, useState } from 'react';
import { Alert, AutoComplete, Button, Card, Empty, Form, Input, Modal, Pagination, Select, Space, Table, Tag, Typography, Upload, message, } from 'antd';
import { InboxOutlined, PlusOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { uploadFile } from '../../api/knowledge';
import { FilePreviewModal } from '../../components/FilePreviewModal';
import { fileNameFromUrl, downloadUrlFile } from '../../api/files';
import './TrainingClassDeclarePage.css';
import { TrainingProcessSteps } from './TrainingProcessSteps';
const { Dragger } = Upload;
const FILE_TEMP_TITLES = ['开班申请表', '导入表', '开班花名册', '课时导入表'];
/** 班级工种等级：5=五级 … 1=一级（与开班准备一致） */
const CLASS_LEVEL_LABELS = {
    5: '五级/初级工',
    4: '四级/中级工',
    3: '三级/高级工',
    2: '二级/技师',
    1: '一级/高级技师',
};
function formatClassLevel(level) {
    if (level == null || Number.isNaN(Number(level)))
        return '—';
    const n = Number(level);
    return CLASS_LEVEL_LABELS[n] ?? `${n}级`;
}
function materialCoverage(rows) {
    const titles = new Set(rows.map((r) => r.title).filter(Boolean));
    const missing = FILE_TEMP_TITLES.filter((t) => !titles.has(t));
    return { titles, missing, ready: missing.length === 0 };
}
function errMsg(e, fallback) {
    return e instanceof Error && e.message ? e.message : fallback;
}
function uploadAcceptByTitle(title) {
    if (title === '开班申请表')
        return '.docx';
    if (title === '导入表' || title === '开班花名册' || title === '课时导入表')
        return '.xlsx,.xls';
    return '.docx,.xlsx,.xls';
}
function isExcelExt(ext) {
    return ext === 'xlsx' || ext === 'xls';
}
function fileTypeHint(title) {
    if (title === '开班申请表') {
        return '上传已含 ${pxdw}、${company}、${worktype}、${level}、${nums}、${pxtime}、${teacher1} 等占位符的 Word 即可；生成材料时自动填入班级信息。「设置变量后的Word」可选，用于覆盖原件。';
    }
    if (title === '导入表' || title === '开班花名册' || title === '课时导入表') {
        return `上传 Excel 模板（.xlsx/.xls），须为人社部门提供的「${title}」标准表头`;
    }
    return '请先选择文件名称，再上传对应格式的模板文件';
}
const GUANGXI_CITIES = [
    '南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市',
    '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市',
];
function parseFileTempIds(raw) {
    if (raw == null || raw === '')
        return [];
    if (Array.isArray(raw))
        return raw.map((v) => Number(v)).filter((n) => !Number.isNaN(n));
    if (typeof raw === 'string') {
        try {
            const parsed = JSON.parse(raw);
            if (Array.isArray(parsed))
                return parsed.map((v) => Number(v)).filter((n) => !Number.isNaN(n));
        }
        catch {
            return raw.replace(/[\[\]]/g, '').split(',').map((s) => Number(s.trim())).filter((n) => !Number.isNaN(n));
        }
    }
    return [];
}
function fileUrl(path) {
    if (!path)
        return '';
    if (path.startsWith('http') || path.startsWith('/api/'))
        return path;
    return path.startsWith('/') ? path : `/${path}`;
}
export function TrainingClassDeclarePage() {
    const [companyIdFilter, setCompanyIdFilter] = useState();
    const [workTypeIdFilter, setWorkTypeIdFilter] = useState();
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize] = useState(20);
    const [generating, setGenerating] = useState(false);
    const [fileTempOpen, setFileTempOpen] = useState(false);
    const [fileTempImport, setFileTempImport] = useState(false);
    const [fileTempRows, setFileTempRows] = useState([]);
    const [selectedTempKeys, setSelectedTempKeys] = useState([]);
    const [fileInfoOpen, setFileInfoOpen] = useState(false);
    const [uploadOpen, setUploadOpen] = useState(false);
    const [uploadField, setUploadField] = useState('pathSource');
    const [uploadAccept, setUploadAccept] = useState('.docx,.xlsx');
    const [formFileTemp] = Form.useForm();
    const [classMaterialOpen, setClassMaterialOpen] = useState(false);
    const [classId, setClassId] = useState(0);
    const [classTitle, setClassTitle] = useState('');
    const [materialRows, setMaterialRows] = useState([]);
    const attachedTempIds = useMemo(() => new Set(materialRows.map((r) => r.id)), [materialRows]);
    const coverage = useMemo(() => materialCoverage(materialRows), [materialRows]);
    const [previewTarget, setPreviewTarget] = useState(null);
    const { data: publicData } = useQuery({
        queryKey: ['demand-public-data', 'class-declare'],
        queryFn: () => trainApi.getDemandPublicData(['company', 'worktype']),
        staleTime: 5 * 60 * 1000,
    });
    const { data: classData, isLoading, refetch } = useQuery({
        queryKey: ['info-classes-declare', currentPage, pageSize, companyIdFilter, workTypeIdFilter],
        queryFn: () => trainApi.pageInfoClassList({
            page: currentPage,
            pageSize,
            companyId: companyIdFilter,
            workTypeId: workTypeIdFilter,
        }),
    });
    const workTypeOptions = useMemo(() => {
        const groups = publicData?.worktype_list ?? [];
        return groups.flatMap((g) => (g.options ?? []).map((o) => ({
            value: o.id,
            label: o.title,
        })));
    }, [publicData]);
    const loadFileTemps = async (ids = [], forMaterial = false) => {
        const res = await trainApi.listFileTemps({ type: 1, ids: ids.length ? ids : undefined });
        const list = res?.list ?? [];
        if (forMaterial) {
            setMaterialRows(ids.length ? list : []);
        }
        else {
            setFileTempRows(list);
            setFileTempOpen(true);
        }
    };
    const handleReset = () => {
        setCompanyIdFilter(undefined);
        setWorkTypeIdFilter(undefined);
        setCurrentPage(1);
        refetch();
    };
    const openTemplateManage = () => {
        setFileTempImport(false);
        setSelectedTempKeys([]);
        loadFileTemps([]);
    };
    const resetFileTempForm = () => {
        formFileTemp.resetFields();
        formFileTemp.setFieldsValue({ type: 1, id: undefined, city: '', title: '', pathSource: '', pathUsed: '' });
    };
    const handleFileAdd = () => {
        resetFileTempForm();
        setFileInfoOpen(true);
    };
    const handleFileEdit = (row) => {
        formFileTemp.setFieldsValue({
            id: row.id,
            type: 1,
            city: row.city,
            title: row.title,
            pathSource: row.path_source,
            pathUsed: row.path_used,
            ext: row.ext,
        });
        setFileInfoOpen(true);
    };
    const handleFileDelete = (row) => {
        Modal.confirm({
            title: '友情提示',
            content: '确定要删除该文件吗？',
            onOk: async () => {
                const res = await trainApi.batchDeleteFileTemps([row.id]);
                if (res?.count) {
                    message.success('删除成功');
                    loadFileTemps([]);
                }
            },
        });
    };
    const fileUploadSubmit = async () => {
        const v = await formFileTemp.validateFields();
        if (!v.title) {
            message.error('请填写文件名称');
            return;
        }
        if (!v.pathSource) {
            message.error('请上传文件');
            return;
        }
        if (v.title === '开班申请表' && v.ext !== 'docx') {
            message.error('开班申请表须上传 Word 文档（.docx）');
            return;
        }
        if (v.title !== '开班申请表' && !isExcelExt(v.ext)) {
            message.error(`${v.title}须上传 Excel 文件（.xlsx/.xls）`);
            return;
        }
        const res = await trainApi.saveFileTemp({
            id: v.id,
            type: 1,
            city: v.city,
            title: v.title,
            pathSource: v.pathSource,
            pathUsed: v.pathUsed,
            ext: v.ext,
        });
        if (res?.info) {
            message.success('成功');
            setFileInfoOpen(false);
            resetFileTempForm();
            loadFileTemps([]);
        }
    };
    const openUpload = (field, ext) => {
        setUploadField(field);
        setUploadAccept(ext);
        setUploadOpen(true);
    };
    const uploadProps = {
        multiple: false,
        maxCount: 1,
        showUploadList: false,
        accept: uploadAccept,
        className: 'upload-demos',
        customRequest: async ({ file, onSuccess, onError }) => {
            try {
                const result = await uploadFile(file, 'public');
                const url = `/api/files/${result.id}/download`;
                formFileTemp.setFieldValue(uploadField, url);
                if (uploadField === 'pathSource') {
                    const name = file.name;
                    const ext = name.includes('.') ? name.split('.').pop()?.toLowerCase() : '';
                    formFileTemp.setFieldValue('ext', ext);
                }
                message.success('上传成功');
                setUploadOpen(false);
                onSuccess?.(result);
            }
            catch (e) {
                message.error('上传失败');
                onError?.(e);
            }
        },
    };
    const handleFileImport = async () => {
        if (!selectedTempKeys.length) {
            message.error('请选择要导入的模板');
            return;
        }
        const ids = selectedTempKeys.map((k) => Number(k));
        try {
            const res = await trainApi.attachFileTemps(classId, ids);
            if (res?.count != null || res?.ids) {
                message.success('导入成功（同名模板已自动替换）');
                setFileTempOpen(false);
                setFileTempRows([]);
                const newIds = res.ids ?? res.fileTemps ?? ids;
                await loadFileTemps(newIds, true);
                refetch();
            }
        }
        catch (e) {
            message.error(errMsg(e, '导入失败'));
        }
    };
    const createFile = async (row) => {
        setGenerating(true);
        try {
            const res = await trainApi.generateMaterials(Number(row.id));
            if (res?.downUrl) {
                await downloadUrlFile(res.downUrl, `${row.title || '班级'}_申报材料.zip`);
                message.success('材料已生成');
            }
            else {
                message.warning('未返回下载地址');
            }
        }
        catch (e) {
            message.error(errMsg(e, '生成失败'));
        }
        finally {
            setGenerating(false);
        }
    };
    const manageFile = (row) => {
        setClassId(Number(row.id));
        setClassTitle(row.title || '');
        const ids = parseFileTempIds(row.file_temps);
        loadFileTemps(ids, true);
        setClassMaterialOpen(true);
    };
    const handleMaterialDelete = (row) => {
        Modal.confirm({
            title: '友情提示',
            content: `确定从本班移除「${row.title}」吗？模板本身不会删除。`,
            onOk: async () => {
                try {
                    const res = await trainApi.detachFileTemp(classId, row.id);
                    if (res?.fileTemps) {
                        message.success('已移除');
                        await loadFileTemps(res.fileTemps, true);
                        refetch();
                    }
                }
                catch (e) {
                    message.error(errMsg(e, '移除失败'));
                }
            },
        });
    };
    const handleMaterialAdd = async () => {
        setFileTempImport(true);
        setSelectedTempKeys([]);
        await loadFileTemps([]);
    };
    const onPreviewFile = (row, field = 'urlSource') => {
        const url = field === 'urlUsed'
            ? (row.urlUsed || row.path_used || row.urlSource || row.path_source)
            : (row.urlSource || row.path_source);
        if (!url) {
            message.warning('暂无可预览文件');
            return;
        }
        const resolved = fileUrl(url);
        setPreviewTarget({
            fileUrl: resolved,
            fileName: fileNameFromUrl(resolved) || row.title || 'preview',
            fileType: row.ext,
        });
    };
    const classColumns = [
        { title: '序号', width: 60, align: 'center', render: (_v, _r, i) => (currentPage - 1) * pageSize + i + 1 },
        { title: '班级名称', dataIndex: 'title', align: 'center' },
        { title: '公司', dataIndex: 'company_name', align: 'center' },
        { title: '工种', dataIndex: 'work_type_title', align: 'center' },
        {
            title: '等级',
            dataIndex: 'work_type_level',
            width: 120,
            align: 'center',
            render: (v) => formatClassLevel(v),
        },
        { title: '人数', dataIndex: 'nums', width: 80, align: 'center' },
        { title: '地点', dataIndex: 'position', align: 'center' },
        { title: '课时', dataIndex: 'course_num', align: 'center' },
        {
            title: '操作',
            width: 220,
            align: 'center',
            render: (_, row) => {
                const attached = parseFileTempIds(row.file_temps).length;
                return (_jsxs(Space, { children: [_jsx(Button, { type: "link", className: "link-action", loading: generating, onClick: () => createFile(row), children: "\u751F\u6210\u6750\u6599" }), _jsxs(Button, { type: "link", className: "link-action", onClick: () => manageFile(row), children: ["\u6750\u6599\u7BA1\u7406", attached > 0 ? `(${attached})` : ''] })] }));
            },
        },
    ];
    const tempColumns = [
        { title: 'ID', dataIndex: 'id', width: 60 },
        { title: '城市', dataIndex: 'city', width: 100 },
        { title: '文件名称', dataIndex: 'title' },
        { title: '文件类型', dataIndex: 'ext', width: 100 },
        {
            title: '原文件',
            width: 160,
            render: (_, row) => (_jsx(Button, { type: "link", onClick: () => onPreviewFile(row, 'urlSource'), children: "\u9884\u89C8" })),
        },
        {
            title: '使用文件',
            width: 160,
            render: (_, row) => row.ext === 'docx' ? (_jsxs(Space, { children: [_jsx("a", { href: fileUrl(row.urlSource), target: "_blank", rel: "noreferrer", className: "mar-r10", children: "\u4E0B\u8F7D" }), row.path_used ? (_jsx(Button, { type: "link", onClick: () => onPreviewFile(row, 'urlUsed'), children: "\u9884\u89C8" })) : null] })) : null,
        },
        ...(!fileTempImport
            ? [{
                    title: '操作',
                    width: 150,
                    align: 'center',
                    render: (_, row) => (_jsxs(Space, { children: [_jsx(Button, { type: "link", onClick: () => handleFileEdit(row), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", onClick: () => handleFileDelete(row), children: "\u5220\u9664" })] })),
                }]
            : []),
    ];
    const materialColumns = [
        { title: '城市', dataIndex: 'city', width: 100, render: (v) => v || '-' },
        { title: '文件名称', dataIndex: 'title' },
        {
            title: '文件类型',
            dataIndex: 'ext',
            width: 100,
            render: (ext) => (ext ? _jsx(Tag, { children: ext }) : '-'),
        },
        {
            title: '文件',
            width: 160,
            render: (_, row) => {
                const canPreviewUsed = Boolean(row.path_used || row.urlUsed);
                const canPreviewSource = Boolean(row.path_source || row.urlSource);
                if (row.ext === 'docx') {
                    if (canPreviewUsed) {
                        return _jsx(Button, { type: "link", onClick: () => onPreviewFile(row, 'urlUsed'), children: "\u9884\u89C8" });
                    }
                    if (canPreviewSource) {
                        return _jsx(Button, { type: "link", onClick: () => onPreviewFile(row, 'urlSource'), children: "\u9884\u89C8" });
                    }
                    return _jsx(Typography.Text, { type: "secondary", children: "\u672A\u4E0A\u4F20" });
                }
                if (isExcelExt(row.ext) && canPreviewSource) {
                    return _jsx(Button, { type: "link", onClick: () => onPreviewFile(row, 'urlSource'), children: "\u9884\u89C8" });
                }
                return _jsx(Typography.Text, { type: "secondary", children: "-" });
            },
        },
        {
            title: '操作',
            width: 100,
            align: 'center',
            render: (_, row) => (_jsx(Button, { type: "link", danger: true, onClick: () => handleMaterialDelete(row), children: "\u79FB\u9664" })),
        },
    ];
    const importTempColumns = [
        ...tempColumns.filter((c) => c.title !== '操作'),
        {
            title: '状态',
            width: 100,
            align: 'center',
            render: (_, row) => attachedTempIds.has(row.id) ? _jsx(Tag, { color: "blue", children: "\u5DF2\u6302\u8F7D" }) : _jsx(Tag, { children: "\u53EF\u9009" }),
        },
    ];
    return (_jsxs("div", { className: "training-class-declare-page training-page training-table-container", children: [_jsxs("section", { className: "page-heading", children: [_jsx(Typography.Title, { level: 4, children: "\u5F00\u73ED\u7533\u62A5" }), _jsx(Typography.Text, { type: "secondary", children: "\u7BA1\u7406\u5F00\u73ED\u7533\u62A5\u6750\u6599\uFF0C\u914D\u7F6E\u6A21\u677F\u5E76\u751F\u6210\u7533\u62A5\u6587\u4EF6" })] }), _jsx(TrainingProcessSteps, { current: 4 }), _jsx(Card, { size: "small", style: { marginBottom: 16 }, children: _jsx("div", { className: "filter-bar", children: _jsxs(Space, { wrap: true, children: [_jsx(Select, { placeholder: "\u4F01\u4E1A\u7B5B\u9009", allowClear: true, showSearch: true, style: { width: 200 }, value: companyIdFilter, onChange: (v) => { setCompanyIdFilter(v); setCurrentPage(1); }, options: publicData?.company_list?.map((c) => ({
                                    value: Number(c.id),
                                    label: c.title,
                                })), filterOption: (input, opt) => (opt?.label ?? '').includes(input) }), _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u5DE5\u79CD", allowClear: true, showSearch: true, style: { width: 200 }, value: workTypeIdFilter, onChange: (v) => { setWorkTypeIdFilter(v); setCurrentPage(1); }, options: workTypeOptions, filterOption: (input, opt) => (opt?.label ?? '').includes(input) }), _jsx(Button, { type: "primary", onClick: handleReset, children: "\u91CD\u7F6E" }), _jsx(Button, { type: "primary", onClick: openTemplateManage, children: "\u6750\u6599\u6A21\u677F\u7BA1\u7406" })] }) }) }), _jsx("div", { className: "table-wrap", children: _jsx(Table, { rowKey: "id", bordered: true, loading: isLoading, columns: classColumns, dataSource: classData?.list ?? [], pagination: false, scroll: { y: 600 } }) }), _jsx("div", { className: "pagination-bar", children: _jsx(Pagination, { current: currentPage, pageSize: pageSize, total: classData?.total ?? 0, showSizeChanger: false, showTotal: (t) => `共 ${t} 条`, onChange: (p) => setCurrentPage(p) }) }), _jsxs(Modal, { title: "\u6750\u6599\u6A21\u677F\u5217\u8868", open: fileTempOpen, width: "50%", onCancel: () => { setFileTempRows([]); setFileTempOpen(false); setFileTempImport(false); }, footer: null, destroyOnHidden: true, children: [_jsx(Alert, { type: "info", showIcon: true, className: "mar-b10", message: "Word \u4E0E Excel \u9700\u5206\u522B\u65B0\u5EFA\u6A21\u677F", description: "\u5F00\u73ED\u7533\u8BF7\u8868\uFF1A\u4E0A\u4F20\u4E00\u4EFD\u5DF2\u63D2\u5165 ${pxdw}\u3001${company}\u3001${worktype}\u3001${level}\u3001${nums}\u3001${pxtime}\u3001${teacher1} \u7B49\u53D8\u91CF\u7684 Word \u5373\u53EF\uFF1B\u751F\u6210\u6750\u6599\u65F6\u81EA\u52A8\u5199\u5165\u73ED\u7EA7\u4FE1\u606F\u3002\u4E5F\u53EF\u53E6\u4F20\u300C\u8BBE\u7F6E\u53D8\u91CF\u540E\u7684Word\u300D\u8986\u76D6\u3002Excel \u6A21\u677F\uFF08\u5BFC\u5165\u8868/\u82B1\u540D\u518C/\u8BFE\u65F6\u8868\uFF09\u987B\u5355\u72EC\u65B0\u5EFA\u3002" }), fileTempImport ? (_jsx(Button, { className: "mar-b10", type: "primary", onClick: handleFileImport, children: "\u786E\u8BA4\u5BFC\u5165" })) : (_jsx(Button, { className: "mar-b10", type: "primary", onClick: handleFileAdd, children: "\u65B0\u589E" })), _jsx(Table, { rowKey: "id", bordered: true, size: "small", columns: fileTempImport ? importTempColumns : tempColumns, dataSource: fileTempRows, scroll: { y: 300 }, pagination: false, locale: { emptyText: _jsx(Empty, { description: fileTempImport ? '暂无可用模板，请先在材料模板管理中新增' : '暂无模板' }) }, rowSelection: fileTempImport ? {
                            selectedRowKeys: selectedTempKeys,
                            onChange: (keys) => setSelectedTempKeys(keys),
                            getCheckboxProps: (row) => ({
                                disabled: attachedTempIds.has(row.id),
                            }),
                        } : undefined })] }), _jsx(Modal, { title: "\u6587\u4EF6\u6A21\u677F\u4FE1\u606F", open: fileInfoOpen, width: "50%", onCancel: () => { setFileInfoOpen(false); resetFileTempForm(); }, onOk: fileUploadSubmit, okText: "\u786E\u5B9A", cancelText: "\u53D6\u6D88", destroyOnHidden: true, children: _jsxs(Form, { form: formFileTemp, labelCol: { span: 8 }, wrapperCol: { span: 16 }, children: [_jsx(Form.Item, { name: "id", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "type", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "ext", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { noStyle: true, shouldUpdate: (p, c) => p.title !== c.title, children: () => (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: fileTypeHint(formFileTemp.getFieldValue('title')) })) }), _jsx(Form.Item, { name: "title", label: "\u6587\u4EF6\u540D\u79F0", rules: [{ required: true }], children: _jsx(Select, { placeholder: "\u8BF7\u9009\u62E9\u540D\u79F0", options: FILE_TEMP_TITLES.map((t) => ({ value: t, label: t })), onChange: () => {
                                    formFileTemp.setFieldsValue({ pathSource: '', pathUsed: '', ext: '' });
                                } }) }), _jsx(Form.Item, { name: "pathSource", label: "\u6587\u4EF6", rules: [{ required: true }], children: _jsx(Input, { readOnly: true, addonAfter: _jsx(Button, { type: "link", onClick: () => {
                                        const title = formFileTemp.getFieldValue('title');
                                        if (!title) {
                                            message.error('请先选择文件名称');
                                            return;
                                        }
                                        openUpload('pathSource', uploadAcceptByTitle(title));
                                    }, children: "\u4E0A\u4F20" }) }) }), _jsx(Form.Item, { noStyle: true, shouldUpdate: (p, c) => p.title !== c.title, children: () => formFileTemp.getFieldValue('title') === '开班申请表' ? (_jsx(Form.Item, { name: "city", label: "\u5730\u5E02", rules: [{ required: true, message: '请选择或输入地市' }], children: _jsx(AutoComplete, { allowClear: true, placeholder: "\u8BF7\u9009\u62E9\u6216\u8F93\u5165\u5730\u5E02", options: GUANGXI_CITIES.map((c) => ({ value: c })), filterOption: (input, option) => String(option?.value ?? '')
                                        .toLowerCase()
                                        .includes(input.trim().toLowerCase()) }) })) : null }), _jsx(Form.Item, { noStyle: true, shouldUpdate: (p, c) => p.title !== c.title || p.ext !== c.ext, children: () => {
                                const title = formFileTemp.getFieldValue('title');
                                const fileExt = formFileTemp.getFieldValue('ext');
                                if (title !== '开班申请表' || fileExt !== 'docx')
                                    return null;
                                return (_jsx(Form.Item, { name: "pathUsed", label: "\u8BBE\u7F6E\u53D8\u91CF\u540E\u7684Word", extra: "\u53EF\u9009\u3002\u9ED8\u8BA4\u4F7F\u7528\u4E0A\u65B9\u300C\u6587\u4EF6\u300D\uFF1B\u82E5\u9700\u5355\u72EC\u4E00\u4EFD\u542B\u5360\u4F4D\u7B26\u7684 Word\uFF0C\u53EF\u5728\u6B64\u4E0A\u4F20\u8986\u76D6", children: _jsx(Input, { readOnly: true, addonAfter: _jsx(Button, { type: "link", onClick: () => openUpload('pathUsed', '.docx'), children: "\u4E0A\u4F20" }) }) }));
                            } })] }) }), _jsx(Modal, { title: "\u4E0A\u4F20\u6587\u4EF6", open: uploadOpen, width: 500, onCancel: () => setUploadOpen(false), footer: null, destroyOnHidden: true, children: _jsxs(Dragger, { ...uploadProps, children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(InboxOutlined, {}) }), _jsxs("p", { className: "ant-upload-text", children: ["\u5C06\u6587\u4EF6\u62D6\u5230\u6B64\u5904\uFF0C\u6216", _jsx("em", { children: "\u70B9\u51FB\u4E0A\u4F20" })] }), _jsx("p", { style: { color: '#ccc', marginTop: 10 }, children: "\u63D0\u793A:\u4E0A\u4F20\u8FC7\u7A0B\u4E2D,\u8BF7\u4E0D\u8981\u505A\u4EFB\u4F55\u64CD\u4F5C" }), _jsxs("p", { className: "ant-upload-hint", children: ["\u53EA\u80FD\u4E0A\u4F20", uploadAccept, "\u6587\u4EF6"] })] }) }), _jsx(FilePreviewModal, { open: previewTarget != null, target: previewTarget, onClose: () => setPreviewTarget(null) }), _jsxs(Modal, { title: classTitle ? `班级材料管理 — ${classTitle}` : '班级材料管理', open: classMaterialOpen, width: "60%", onCancel: () => {
                    setClassMaterialOpen(false);
                    setFileTempImport(false);
                    setMaterialRows([]);
                    setClassTitle('');
                }, footer: null, destroyOnHidden: true, children: [_jsx(Alert, { className: "mar-b10", showIcon: true, type: coverage.ready ? 'success' : 'warning', message: coverage.ready ? '四类模板已齐全，可生成申报材料包' : '模板尚未齐全', description: coverage.ready
                            ? '包含：开班申请表、导入表、开班花名册、课时导入表'
                            : `缺少：${coverage.missing.join('、')}。请点击「新增」从模板库挂载。` }), _jsxs(Space, { className: "mar-b10", wrap: true, children: [_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: handleMaterialAdd, children: "\u65B0\u589E" }), _jsx(Button, { type: "primary", loading: generating, disabled: !materialRows.length, onClick: () => createFile({ id: classId, title: classTitle }), children: "\u751F\u6210\u6750\u6599" })] }), _jsx(Table, { rowKey: "id", bordered: true, size: "small", columns: materialColumns, dataSource: materialRows, scroll: { y: 300 }, pagination: false, locale: {
                            emptyText: (_jsx(Empty, { description: "\u5C1A\u672A\u6302\u8F7D\u6A21\u677F", image: Empty.PRESENTED_IMAGE_SIMPLE, children: _jsx(Button, { type: "primary", onClick: handleMaterialAdd, children: "\u4ECE\u6A21\u677F\u5E93\u5BFC\u5165" }) })),
                        } })] })] }));
}
