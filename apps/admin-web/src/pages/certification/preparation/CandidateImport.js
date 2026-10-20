import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, DownloadOutlined, EditOutlined, FileProtectOutlined, PlusOutlined, ReloadOutlined, UploadOutlined, } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, DatePicker, Form, Input, Modal, Popconfirm, Segmented, Select, Space, Table, Typography, Upload, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useMemo, useState } from 'react';
import { pagePeople } from '../../../api/training';
import { getCertificationTask } from '../../../api/certification';
import { createPreparationCandidate, deletePreparationCandidate, getPreparationCandidateApplicationSource, importPreparationCandidates, importPreparationCandidatesExcel, downloadPreparationCandidateExcelTemplate, generatePreparationImportBatchMaterials, listAllPreparationCandidates, pageCandidateStaging, pagePreparationCandidates, pagePreparationImportErrors, promoteCandidateStaging, updatePreparationCandidate, } from '../../../api/certificationPreparation';
import { PermissionGate } from '../../../directives/permission';
import { CandidateDocumentDrawer } from './CandidateDocumentDrawer';
import { candidateDocumentRatioTag, dash, dateTimeText, levelOptions, statusTag } from './shared';
const certificationLevelToTrainingLevel = {
    junior: 0,
    intermediate: 1,
    senior: 2,
    technician: 3,
    senior_technician: 4,
};
export function CandidateImport({ taskId, onChanged }) {
    const queryClient = useQueryClient();
    const [query, setQuery] = useState({ page: 1, pageSize: 10 });
    const [importChannel, setImportChannel] = useState('other');
    const [selectedScopeId, setSelectedScopeId] = useState();
    const [candidateModalOpen, setCandidateModalOpen] = useState(false);
    const [editingCandidate, setEditingCandidate] = useState(null);
    const [documentCandidate, setDocumentCandidate] = useState(null);
    const [lastImportBatch, setLastImportBatch] = useState(null);
    const [candidateForm] = Form.useForm();
    const [importFileList, setImportFileList] = useState([]);
    const [importRawFile, setImportRawFile] = useState(null);
    const [trainingQuery, setTrainingQuery] = useState({ page: 1, pageSize: 10, type: 'student' });
    const [selectedTrainingPeople, setSelectedTrainingPeople] = useState(new Map());
    const [stagingQuery, setStagingQuery] = useState({ page: 1, pageSize: 10 });
    const [selectedStagingRows, setSelectedStagingRows] = useState(new Map());
    const candidatesQuery = useQuery({
        queryKey: ['certification-preparation-candidates', taskId, query],
        queryFn: () => pagePreparationCandidates(taskId, query),
        enabled: Boolean(taskId),
    });
    const taskQuery = useQuery({
        queryKey: ['certification-task-import-scopes', taskId],
        queryFn: () => getCertificationTask(taskId),
        enabled: Boolean(taskId),
    });
    const scopeOptions = (taskQuery.data?.scopes || []).map((scope) => ({
        value: scope.id,
        label: `${scope.workTypeName || '未设置工种'}｜${scope.levelName || '未设置等级'}`,
    }));
    const selectedScope = (taskQuery.data?.scopes || []).find((scope) => scope.id === selectedScopeId);
    const selectedTrainingLevel = selectedScope?.levelCode
        ? certificationLevelToTrainingLevel[selectedScope.levelCode]
        : undefined;
    const stagingPeopleQuery = useQuery({
        queryKey: ['certification-preparation-candidate-staging', taskId, stagingQuery],
        queryFn: () => pageCandidateStaging(taskId, stagingQuery),
        enabled: Boolean(taskId) && importChannel === 'staging',
    });
    const trainingPeopleQuery = useQuery({
        queryKey: ['training-people-import', trainingQuery, selectedScopeId],
        queryFn: () => pagePeople({
            ...trainingQuery,
            applyWorkTypeName: selectedScope.workTypeName,
            applyLevel: selectedTrainingLevel,
        }),
        enabled: Boolean(taskId)
            && importChannel === 'training'
            && Boolean(selectedScope?.workTypeName)
            && selectedTrainingLevel !== undefined,
    });
    const importedCandidatesQuery = useQuery({
        queryKey: ['certification-preparation-candidates-imported', taskId],
        queryFn: () => listAllPreparationCandidates(taskId, { status: 'active' }),
        enabled: Boolean(taskId) && importChannel === 'training',
    });
    const importedTrainingKeys = useMemo(() => {
        const keys = new Set();
        (importedCandidatesQuery.data?.items || []).forEach((candidate) => {
            if (candidate.idCardNo) {
                keys.add(`id:${candidate.idCardNo}`);
            }
            if (candidate.phone) {
                keys.add(`phone:${candidate.phone}`);
            }
        });
        return keys;
    }, [importedCandidatesQuery.data?.items]);
    const trainingPeople = (trainingPeopleQuery.data?.items || []).filter((person) => {
        if (person.idCard && importedTrainingKeys.has(`id:${person.idCard}`)) {
            return false;
        }
        if (person.phone && importedTrainingKeys.has(`phone:${person.phone}`)) {
            return false;
        }
        return true;
    });
    const errorQuery = useQuery({
        queryKey: ['certification-preparation-import-errors', taskId, lastImportBatch?.id],
        queryFn: () => pagePreparationImportErrors(taskId, lastImportBatch.id, { page: 1, pageSize: 50 }),
        enabled: Boolean(taskId && lastImportBatch?.id && (lastImportBatch.failedCount || 0) > 0),
    });
    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates-imported', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidate-staging', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-candidates-select', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-documents', taskId] });
        queryClient.invalidateQueries({ queryKey: ['certification-preparation-overview', taskId] });
        onChanged?.();
    };
    const createMutation = useMutation({
        mutationFn: (payload) => createPreparationCandidate(taskId, payload),
        onSuccess: () => {
            message.success('学员已新增');
            setCandidateModalOpen(false);
            candidateForm.resetFields();
            refresh();
        },
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, payload }) => updatePreparationCandidate(taskId, id, payload),
        onSuccess: () => {
            message.success('学员已更新');
            setCandidateModalOpen(false);
            setEditingCandidate(null);
            candidateForm.resetFields();
            refresh();
        },
    });
    const deleteMutation = useMutation({
        mutationFn: (candidateId) => deletePreparationCandidate(taskId, candidateId),
        onSuccess: () => {
            message.success('学员已删除');
            refresh();
        },
    });
    const promoteMutation = useMutation({
        mutationFn: (stagingIds) => promoteCandidateStaging(taskId, { stagingIds }),
        onSuccess: (result) => {
            message.success(`导入完成：成功 ${result.successCount}，失败 ${result.failedCount}`);
            setSelectedStagingRows(new Map());
            refresh();
        },
        onError: (error) => {
            message.error(error.message || '导入失败');
        },
    });
    const importMutation = useMutation({
        mutationFn: (payload) => {
            if (!selectedScopeId) {
                throw new Error('请选择认定班级（工种和等级）');
            }
            if (!payload.trainingPeopleIds?.length) {
                throw new Error('请勾选要导入的培训学员');
            }
            return importPreparationCandidates(taskId, {
                sourceType: payload.sourceType,
                trainingPeopleIds: payload.trainingPeopleIds,
                taskScopeId: selectedScopeId,
            });
        },
        onSuccess: (batch) => {
            message.success(`导入完成：成功 ${batch.successCount || 0}，失败 ${batch.failedCount || 0}`);
            setLastImportBatch(batch);
            setImportFileList([]);
            setSelectedTrainingPeople(new Map());
            refresh();
        },
        onError: (error) => {
            message.error(error.message || '导入失败');
        },
    });
    const excelImportMutation = useMutation({
        mutationFn: (file) => {
            const scopeId = selectedScopeId || scopeOptions[0]?.value;
            if (!scopeId)
                throw new Error('当前任务尚未配置认定班级');
            return importPreparationCandidatesExcel(taskId, scopeId, file);
        },
        onSuccess: (batch) => {
            message.success(`XLSX 导入完成：成功 ${batch.successCount || 0}，失败 ${batch.failedCount || 0}`);
            setLastImportBatch(batch);
            setImportFileList([]);
            setImportRawFile(null);
            refresh();
        },
        onError: (error) => message.error(error.message || 'XLSX 导入失败'),
    });
    const generateBatchMaterialsMutation = useMutation({
        mutationFn: (batchId) => generatePreparationImportBatchMaterials(taskId, batchId),
        onSuccess: (batch) => {
            message.success(`申报材料已生成：${batch.successCount || 0} 名考生`);
            queryClient.invalidateQueries({ queryKey: ['certification-preparation-materials', taskId] });
        },
        onError: (error) => message.error(error.message || '申报材料生成失败，请先补齐并审核必传资料'),
    });
    const stagingPeopleColumns = [
        { title: '姓名', dataIndex: 'candidateName', width: 100, render: dash },
        { title: '性别', dataIndex: 'gender', width: 72, render: dash },
        { title: '身份证号', dataIndex: 'idCardNo', width: 170, ellipsis: true, render: dash },
        { title: '手机号', dataIndex: 'phone', width: 130, render: dash },
        { title: '报考等级', dataIndex: 'applyLevelName', width: 130, render: dash },
        { title: '工作单位', dataIndex: 'companyName', minWidth: 180, ellipsis: true, render: dash },
    ];
    const trainingPeopleColumns = [
        { title: '姓名', dataIndex: 'realname', width: 100, render: dash },
        { title: '手机号', dataIndex: 'phone', width: 130, render: dash },
        { title: '身份证', dataIndex: 'idCard', width: 170, ellipsis: true, render: dash },
        { title: '培训ID', dataIndex: 'trainId', width: 90, render: dash },
        { title: '班级ID', dataIndex: 'trainClassId', width: 90, render: dash },
    ];
    function handleStagingSelectionChange(_keys, rows) {
        setSelectedStagingRows((prev) => {
            const next = new Map(prev);
            const currentPageIds = (stagingPeopleQuery.data?.items || []).map((row) => row.id);
            currentPageIds.forEach((id) => next.delete(id));
            rows.forEach((row) => next.set(row.id, row));
            return next;
        });
    }
    function submitStagingPromote() {
        const stagingIds = Array.from(selectedStagingRows.keys());
        if (stagingIds.length === 0) {
            message.warning('请勾选要导入的测试学员');
            return;
        }
        promoteMutation.mutate(stagingIds);
    }
    function handleTrainingSelectionChange(_keys, rows) {
        setSelectedTrainingPeople((prev) => {
            const next = new Map(prev);
            const currentPageIds = trainingPeople.map((person) => person.id);
            currentPageIds.forEach((id) => next.delete(id));
            rows.forEach((row) => next.set(row.id, row));
            return next;
        });
    }
    function submitTrainingImport() {
        const selectedPeople = Array.from(selectedTrainingPeople.values());
        if (selectedPeople.some((person) => !person.realname || !person.idCard)) {
            message.warning('所选培训学员缺少姓名或身份证号，无法导入');
            return;
        }
        importMutation.mutate({
            sourceType: 'training',
            trainingPeopleIds: selectedPeople.map((person) => person.id),
        });
    }
    function submitOtherImport() {
        const file = importRawFile;
        if (!file) {
            message.warning('请先选择要导入的 XLSX 文件');
            return;
        }
        excelImportMutation.mutate(file);
    }
    async function downloadExcelTemplate() {
        const response = await downloadPreparationCandidateExcelTemplate();
        const url = URL.createObjectURL(response.data);
        const link = document.createElement('a');
        link.href = url;
        link.download = '考生信息批量导入模板.xlsx';
        link.click();
        URL.revokeObjectURL(url);
    }
    const columns = useMemo(() => [
        { title: '姓名', dataIndex: 'candidateName', width: 110, fixed: 'left' },
        { title: '准考证号', dataIndex: 'admissionTicketNo', width: 130, render: dash },
        { title: '身份证号', dataIndex: 'maskedIdCardNo', width: 180, render: (value, record) => value || record.idCardNo || '-' },
        { title: '手机号', dataIndex: 'maskedPhone', width: 140, render: (value, record) => value || record.phone || '-' },
        { title: '等级', dataIndex: 'levelName', width: 100, render: dash },
        { title: '资料进度', width: 110, align: 'center', render: (_, record) => candidateDocumentRatioTag(record) },
        { title: '资料齐全', dataIndex: 'documentComplete', width: 90, render: (value) => (value ? '是' : '否') },
        { title: '状态', dataIndex: 'status', width: 100, render: statusTag },
        { title: '创建时间', dataIndex: 'createdAt', width: 170, render: dateTimeText },
        {
            title: '操作',
            width: 120,
            align: 'center',
            fixed: 'right',
            render: (_, record) => (_jsxs(Space, { size: 2, children: [_jsx(Button, { type: "link", size: "small", title: "\u4E0A\u4F20\u8D44\u6599", "aria-label": "\u4E0A\u4F20\u8D44\u6599", icon: _jsx(FileProtectOutlined, {}), onClick: () => setDocumentCandidate(record), style: { paddingInline: 5 } }), _jsx(PermissionGate, { code: "certification.preparation.candidate.edit", children: _jsx(Button, { type: "link", size: "small", title: "\u7F16\u8F91", "aria-label": "\u7F16\u8F91", icon: _jsx(EditOutlined, {}), onClick: () => openEdit(record), style: { paddingInline: 5 } }) }), _jsx(PermissionGate, { code: "certification.preparation.candidate.delete", children: _jsx(Popconfirm, { title: "\u786E\u8BA4\u5220\u9664\u8BE5\u5B66\u5458\uFF1F", okText: "\u5220\u9664", cancelText: "\u53D6\u6D88", onConfirm: () => deleteMutation.mutate(record.id), children: _jsx(Button, { type: "link", size: "small", danger: true, title: "\u5220\u9664", "aria-label": "\u5220\u9664", icon: _jsx(DeleteOutlined, {}), style: { paddingInline: 5 } }) }) })] })),
        },
    ], [deleteMutation]);
    const errorColumns = [
        { title: '行号', dataIndex: 'rowNo', width: 80 },
        { title: '姓名', dataIndex: 'candidateName', width: 110, render: dash },
        { title: '身份证号', dataIndex: 'idCardNo', width: 180, render: dash },
        { title: '手机号', dataIndex: 'phone', width: 140, render: dash },
        { title: '失败原因', dataIndex: 'errorMessage', render: dash },
    ];
    function openCreate() {
        setEditingCandidate(null);
        candidateForm.resetFields();
        candidateForm.setFieldsValue({ importSource: 'other' });
        setCandidateModalOpen(true);
    }
    async function openEdit(record) {
        setEditingCandidate(record);
        candidateForm.resetFields();
        candidateForm.setFieldsValue({
            candidateName: record.candidateName,
            admissionTicketNo: record.admissionTicketNo,
            idCardNo: record.idCardNo || record.maskedIdCardNo || '',
            phone: record.phone || record.maskedPhone,
            levelCode: record.levelCode,
            levelName: record.levelName,
            importSource: record.importSource || 'other',
            orgName: record.orgName,
            taskScopeId: record.taskScopeId,
            remark: undefined,
        });
        setCandidateModalOpen(true);
        try {
            const source = await getPreparationCandidateApplicationSource(taskId, record.id);
            if (!source) {
                return;
            }
            candidateForm.setFieldsValue({
                gender: source.gender,
                education: source.education,
                graduateDate: source.graduateDate ? dayjs(source.graduateDate) : undefined,
                companyAddress: source.companyAddress,
                zipCode: source.zipCode,
                workStartDate: source.workStartDate ? dayjs(source.workStartDate) : undefined,
                originalWorkTypeName: source.originalWorkTypeName,
                originalLevelName: source.originalLevelName,
                originalCertNo: source.originalCertNo,
                applyWorkYears: source.applyWorkYears,
                companyCreditCode: source.companyCreditCode,
                workTypeCode: source.workTypeCode,
                workDirection: source.workDirection,
                workResume: source.workResume,
                trainingSummary: source.trainingSummary,
                homeAddress: source.homeAddress,
                school: source.school,
                major: source.major,
                nation: source.nation,
                orgName: source.companyName || record.orgName,
                remark: source.remark,
            });
        }
        catch {
            // Editing still works with candidate base fields when source snapshot is absent.
        }
    }
    function toDateString(value) {
        if (!value) {
            return undefined;
        }
        if (dayjs.isDayjs(value)) {
            return value.format('YYYY-MM-DD');
        }
        if (typeof value === 'string' && value.trim()) {
            return value.trim();
        }
        return undefined;
    }
    function submitCandidate() {
        candidateForm.validateFields().then((values) => {
            const payload = {
                ...values,
                importSource: values.importSource || 'other',
                graduateDate: toDateString(values.graduateDate),
                workStartDate: toDateString(values.workStartDate),
            };
            if (editingCandidate) {
                updateMutation.mutate({ id: editingCandidate.id, payload });
                return;
            }
            createMutation.mutate(payload);
        });
    }
    if (!taskId) {
        return (_jsx(ProCard, { children: _jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u9009\u62E9\u8BA4\u5B9A\u4EFB\u52A1\u540E\u7EF4\u62A4\u5B66\u5458\u3002" }) }));
    }
    return (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 16, children: [_jsxs(ProCard, { children: [_jsx(Segmented, { value: importChannel, onChange: (value) => {
                            setImportChannel(value);
                            setImportFileList([]);
                            setImportRawFile(null);
                            setSelectedTrainingPeople(new Map());
                            setSelectedStagingRows(new Map());
                        }, options: [
                            { label: '认定途径', value: 'other' },
                            { label: '培训途径', value: 'training' },
                        ], style: { marginBottom: 16 } }), importChannel === 'staging' ? (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u4ECE\u6D4B\u8BD5\u5B66\u5458\u6C60\u52FE\u9009\u540E\u5BFC\u5165\u5F53\u524D\u8BA4\u5B9A\u4EFB\u52A1\uFF1B\u5BFC\u5165\u540E\u4EC5\u9700\u4E0A\u4F20\u8BC1\u4EF6\u7167\uFF0C\u7CFB\u7EDF\u53EF\u81EA\u52A8\u751F\u6210\u62A5\u540D\u4FE1\u606F\u8868\u3002" }), _jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u59D3\u540D\u3001\u8EAB\u4EFD\u8BC1\u3001\u624B\u673A\u53F7", onSearch: (keyword) => setStagingQuery((prev) => ({ ...prev, keyword, page: 1 })) }), _jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: ["\u5DF2\u9009 ", selectedStagingRows.size, " \u4EBA\uFF0C\u52FE\u9009\u540E\u70B9\u51FB\u300C\u786E\u8BA4\u5BFC\u5165\u300D\u3002"] }), _jsx(Table, { rowKey: "id", size: "small", columns: stagingPeopleColumns, dataSource: stagingPeopleQuery.data?.items || [], loading: stagingPeopleQuery.isFetching, scroll: { y: 240 }, rowSelection: {
                                    selectedRowKeys: Array.from(selectedStagingRows.keys()),
                                    onChange: handleStagingSelectionChange,
                                    preserveSelectedRowKeys: true,
                                }, pagination: {
                                    current: stagingQuery.page,
                                    pageSize: stagingQuery.pageSize,
                                    total: stagingPeopleQuery.data?.total || 0,
                                    showSizeChanger: true,
                                    onChange: (page, pageSize) => setStagingQuery((prev) => ({ ...prev, page, pageSize })),
                                } }), _jsx(Space, { wrap: true, children: _jsx(PermissionGate, { code: "certification.preparation.candidate.import", children: _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), loading: promoteMutation.isPending, onClick: submitStagingPromote, children: "\u786E\u8BA4\u5BFC\u5165\u6D4B\u8BD5\u5B66\u5458" }) }) })] })) : importChannel === 'training' ? (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(Select, { allowClear: true, placeholder: "\u9009\u62E9\u8BA4\u5B9A\u73ED\u7EA7\uFF08\u5DE5\u79CD\uFF5C\u7B49\u7EA7\uFF09", value: selectedScopeId, onChange: (value) => {
                                    setSelectedScopeId(value);
                                    setSelectedTrainingPeople(new Map());
                                    setTrainingQuery((prev) => ({ ...prev, page: 1 }));
                                }, options: scopeOptions, loading: taskQuery.isFetching, style: { minWidth: 280 } }), _jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u59D3\u540D\u3001\u624B\u673A\u53F7", onSearch: (keyword) => setTrainingQuery((prev) => ({ ...prev, keyword, page: 1 })) }), _jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: ["\u5DF2\u9009 ", selectedTrainingPeople.size, " \u4EBA\uFF0C\u52FE\u9009\u540E\u70B9\u51FB\u300C\u786E\u8BA4\u5BFC\u5165\u300D\u3002"] }), _jsx(Table, { rowKey: "id", size: "small", columns: trainingPeopleColumns, dataSource: trainingPeople, loading: trainingPeopleQuery.isFetching, scroll: { y: 240 }, rowSelection: {
                                    selectedRowKeys: Array.from(selectedTrainingPeople.keys()),
                                    onChange: handleTrainingSelectionChange,
                                    preserveSelectedRowKeys: true,
                                }, pagination: {
                                    current: trainingQuery.page,
                                    pageSize: trainingQuery.pageSize,
                                    total: trainingPeopleQuery.data?.total || 0,
                                    showSizeChanger: true,
                                    onChange: (page, pageSize) => setTrainingQuery((prev) => ({ ...prev, page, pageSize })),
                                } }), _jsx(Space, { wrap: true, children: _jsx(PermissionGate, { code: "certification.preparation.candidate.import", children: _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), loading: importMutation.isPending, onClick: submitTrainingImport, children: "\u786E\u8BA4\u5BFC\u5165\u57F9\u8BAD\u5B66\u5458" }) }) })] })) : (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "Excel \u6279\u91CF\u5BFC\u5165\u7684\u6BCF\u540D\u8003\u751F\u90FD\u5FC5\u987B\u586B\u5199\u51C6\u8003\u8BC1\u53F7\uFF1B\u672A\u586B\u5199\u7684\u884C\u4F1A\u6807\u8BB0\u4E3A\u5BFC\u5165\u5931\u8D25\u3002" }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), onClick: downloadExcelTemplate, children: "\u4E0B\u8F7D\u6A21\u677F" }), _jsx(Upload, { accept: ".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", maxCount: 1, fileList: importFileList, beforeUpload: (file) => {
                                            setImportFileList([file]);
                                            setImportRawFile(file);
                                            return false;
                                        }, onRemove: () => {
                                            setImportFileList([]);
                                            setImportRawFile(null);
                                        }, children: _jsx(Button, { size: "small", icon: _jsx(UploadOutlined, {}), children: "\u9009\u62E9\u6587\u4EF6" }) }), _jsx(PermissionGate, { code: "certification.preparation.candidate.import", children: _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), loading: excelImportMutation.isPending, onClick: submitOtherImport, children: "\u786E\u8BA4\u5BFC\u5165\u5176\u4ED6\u5B66\u5458" }) }), _jsx(PermissionGate, { code: "certification.preparation.candidate.edit", children: _jsx(Button, { icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: "\u65B0\u589E\u5B66\u5458" }) })] })] }))] }), _jsxs(ProCard, { title: "\u5B66\u5458\u5217\u8868", extra: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refresh, children: "\u5237\u65B0" }), children: [_jsxs(Form, { layout: "inline", style: { marginBottom: 16 }, onFinish: (values) => setQuery({ ...query, ...values, page: 1 }), children: [_jsx(Form.Item, { name: "keyword", children: _jsx(Input.Search, { allowClear: true, placeholder: "\u59D3\u540D\u3001\u8EAB\u4EFD\u8BC1\u3001\u624B\u673A\u53F7", style: { width: 260 } }) }), _jsx(Form.Item, { name: "taskScopeId", children: _jsx(Select, { allowClear: true, placeholder: "\u9009\u62E9\u8BA4\u5B9A\u73ED\u7EA7\uFF08\u5DE5\u79CD\uFF5C\u7EA7\u522B\uFF09", style: { width: 280 }, options: scopeOptions }) }), _jsx(Form.Item, { name: "levelCode", children: _jsx(Select, { allowClear: true, placeholder: "\u7B49\u7EA7", style: { width: 140 }, options: levelOptions }) }), _jsx(Button, { htmlType: "submit", children: "\u67E5\u8BE2" })] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: candidatesQuery.data?.items || [], loading: candidatesQuery.isFetching, scroll: { x: 1420 }, pagination: {
                            current: query.page,
                            pageSize: query.pageSize,
                            total: candidatesQuery.data?.total || 0,
                            showSizeChanger: true,
                            onChange: (page, pageSize) => setQuery({ ...query, page, pageSize }),
                        } })] }), lastImportBatch && (_jsxs(ProCard, { title: `最近导入批次：${lastImportBatch.batchNo}`, collapsible: true, defaultCollapsed: (lastImportBatch.failedCount || 0) === 0, children: [_jsxs(Space, { style: { marginBottom: 12 }, children: [_jsxs(Typography.Text, { children: ["\u603B\u6570 ", lastImportBatch.totalCount || 0] }), _jsxs(Typography.Text, { type: "success", children: ["\u6210\u529F ", lastImportBatch.successCount || 0] }), _jsxs(Typography.Text, { type: "danger", children: ["\u5931\u8D25 ", lastImportBatch.failedCount || 0] }), statusTag(lastImportBatch.status), lastImportBatch.sourceType === 'other_xlsx' && (lastImportBatch.successCount || 0) > 0 ? (_jsx(PermissionGate, { code: "certification.preparation.material.generate", children: _jsx(Button, { type: "primary", size: "small", loading: generateBatchMaterialsMutation.isPending, onClick: () => generateBatchMaterialsMutation.mutate(lastImportBatch.id), children: "\u751F\u6210\u672C\u6279\u7533\u62A5\u6750\u6599" }) })) : null] }), _jsx(Table, { rowKey: "id", size: "small", columns: errorColumns, dataSource: errorQuery.data?.items || [], pagination: false })] })), _jsx(CandidateDocumentDrawer, { taskId: taskId, candidate: documentCandidate, open: Boolean(documentCandidate), onClose: () => setDocumentCandidate(null), onChanged: onChanged }), _jsx(Modal, { title: editingCandidate ? '编辑学员' : '新增学员（其他学员）', open: candidateModalOpen, onOk: submitCandidate, onCancel: () => setCandidateModalOpen(false), confirmLoading: createMutation.isPending || updateMutation.isPending, width: 960, styles: { body: { maxHeight: '70vh', overflowY: 'auto' } }, children: _jsxs(Form, { form: candidateForm, layout: "vertical", children: [_jsx(Typography.Text, { type: "secondary", children: "\u4EE5\u4E0B\u5B57\u6BB5\u4F1A\u5199\u5165\u804C\u4E1A\u6280\u80FD\u7B49\u7EA7\u8BA4\u5B9A\u7533\u8BF7\u8868\uFF1B\u672A\u586B\u5199\u7684\u9879\u751F\u6210\u7533\u8BF7\u8868\u65F6\u4FDD\u6301\u7A7A\u767D\u3002" }), _jsxs("div", { style: { display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12, marginTop: 12 }, children: [_jsx(Form.Item, { name: "candidateName", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "admissionTicketNo", label: "\u51C6\u8003\u8BC1\u53F7", rules: [{ required: true, whitespace: true, message: '请输入准考证号' }], children: _jsx(Input, { maxLength: 64, placeholder: "\u8BF7\u8F93\u5165\u51C6\u8003\u8BC1\u53F7" }) }), _jsx(Form.Item, { name: "idCardNo", label: "\u8EAB\u4EFD\u8BC1\u53F7", rules: [{ required: true, pattern: /^\d{17}[0-9Xx]$/, message: '请输入18位身份证号' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "gender", label: "\u6027\u522B", children: _jsx(Select, { allowClear: true, options: [
                                            { value: '男', label: '男' },
                                            { value: '女', label: '女' },
                                        ], placeholder: "\u53EF\u7559\u7A7A\uFF0C\u9ED8\u8BA4\u6309\u8EAB\u4EFD\u8BC1\u8BC6\u522B" }) }), _jsx(Form.Item, { name: "phone", label: "\u624B\u673A\u53F7", rules: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "nation", label: "\u6C11\u65CF", children: _jsx(Input, { placeholder: "\u5982\uFF1A\u6C49\u65CF" }) }), _jsx(Form.Item, { name: "education", label: "\u6587\u5316\u7A0B\u5EA6", children: _jsx(Input, { placeholder: "\u5982\uFF1A\u521D\u4E2D/\u9AD8\u4E2D/\u5927\u4E13/\u672C\u79D1" }) }), _jsx(Form.Item, { name: "graduateDate", label: "\u6BD5\u4E1A\u65F6\u95F4", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "school", label: "\u6BD5\u4E1A\u5B66\u6821", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "major", label: "\u6240\u5B66\u4E13\u4E1A", children: _jsx(Input, { placeholder: "\u521D\u4E2D\u3001\u9AD8\u4E2D\u5B66\u5386\u53EF\u586B\u65E0" }) }), _jsx(Form.Item, { name: "homeAddress", label: "\u8EAB\u4EFD\u8BC1\u5730\u5740", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "taskScopeId", label: "\u8BA4\u5B9A\u73ED\u7EA7\uFF08\u5DE5\u79CD/\u7B49\u7EA7\uFF09", children: _jsx(Select, { allowClear: true, options: scopeOptions, placeholder: "\u7528\u4E8E\u586B\u5165\u7533\u62A5/\u8BA4\u5B9A\u804C\u4E1A\u4E0E\u7B49\u7EA7" }) }), _jsx(Form.Item, { name: "levelCode", label: "\u7B49\u7EA7", children: _jsx(Select, { allowClear: true, options: levelOptions, onChange: (_, option) => {
                                            const item = Array.isArray(option) ? option[0] : option;
                                            candidateForm.setFieldValue('levelName', item?.label);
                                        } }) }), _jsx(Form.Item, { name: "levelName", label: "\u7B49\u7EA7\u540D\u79F0", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "orgName", label: "\u5DE5\u4F5C\u5355\u4F4D", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "companyAddress", label: "\u5355\u4F4D\u5730\u5740", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "zipCode", label: "\u90AE\u7F16", children: _jsx(Input, { maxLength: 12 }) }), _jsx(Form.Item, { name: "companyCreditCode", label: "\u7EDF\u4E00\u793E\u4F1A\u4FE1\u7528\u4EE3\u7801", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "workStartDate", label: "\u53C2\u52A0\u5DE5\u4F5C\u65F6\u95F4", children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "applyWorkYears", label: "\u7533\u62A5\u5DE5\u79CD\u5DE5\u9F84", children: _jsx(Input, { placeholder: "\u5982\uFF1A5\u5E74" }) }), _jsx(Form.Item, { name: "workTypeCode", label: "\u804C\u4E1A\u4EE3\u7801", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "workDirection", label: "\u5DE5\u79CD\u65B9\u5411", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "originalWorkTypeName", label: "\u539F\u804C\u4E1A\uFF08\u5DE5\u79CD\uFF09", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "originalLevelName", label: "\u539F\u6280\u672F\u7B49\u7EA7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "originalCertNo", label: "\u539F\u8BC1\u4E66\u7F16\u53F7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "importSource", label: "\u5BFC\u5165\u9014\u5F84", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "workResume", label: "\u4E2A\u4EBA\u5DE5\u4F5C\u7B80\u5386", style: { gridColumn: '1 / -1' }, children: _jsx(Input.TextArea, { rows: 3, placeholder: "\u6709\u5219\u5982\u5B9E\u586B\u5199\uFF1B\u7A7A\u5219\u7533\u8BF7\u8868\u5BF9\u5E94\u680F\u7559\u7A7A" }) }), _jsx(Form.Item, { name: "trainingSummary", label: "\u53C2\u52A0\u57F9\u8BAD\u60C5\u51B5", style: { gridColumn: '1 / -1' }, children: _jsx(Input.TextArea, { rows: 3, placeholder: "\u65F6\u95F4\u3001\u671F\u9650\u3001\u5185\u5BB9\u7B49\uFF1B\u7A7A\u5219\u7533\u8BF7\u8868\u5BF9\u5E94\u680F\u7559\u7A7A" }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", style: { gridColumn: '1 / -1' }, children: _jsx(Input, {}) })] })] }) })] }));
}
