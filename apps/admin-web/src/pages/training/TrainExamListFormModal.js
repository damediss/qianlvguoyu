import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { DatePicker, Form, Input, InputNumber, Modal, Select, Space, message, } from 'antd';
import dayjs from 'dayjs';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createExam, updateExam, getExam, pageInfoLists, importExamPeopleFromTrain, } from '../../api/training';
import { pageCourses as pageExamCourses } from '../../api/exam';
import { listOrgs } from '../../api/system';
export function TrainExamListFormModal({ open, editingId, onClose }) {
    const queryClient = useQueryClient();
    const [form] = Form.useForm();
    const isEdit = editingId != null;
    // 培训下拉
    const { data: trainList } = useQuery({
        queryKey: ['train-exam-form-train-list', open],
        queryFn: () => pageInfoLists({ page: 1, pageSize: 500 }),
        enabled: open,
    });
    // 企业下拉
    const { data: orgList } = useQuery({
        queryKey: ['train-exam-form-org-list', open],
        queryFn: () => listOrgs(),
        enabled: open,
    });
    // 课程下拉
    const { data: courseList } = useQuery({
        queryKey: ['train-exam-form-course-list', open],
        queryFn: () => pageExamCourses({ page: 1, pageSize: 500 }),
        enabled: open,
    });
    // 编辑时回填
    const { data: detail } = useQuery({
        queryKey: ['train-exam-detail', editingId],
        queryFn: () => getExam(editingId),
        enabled: open && isEdit,
    });
    useEffect(() => {
        if (open && isEdit && detail) {
            form.setFieldsValue({
                title: detail.title,
                level: detail.level,
                type: detail.type,
                source: detail.source,
                trainId: detail.trainId,
                courseIdList: detail.courseIdList,
                examDate: detail.examDate ? dayjs(detail.examDate) : undefined,
                examTime: detail.examTime,
                sortNumber: detail.sortNumber,
                invigilateUsername: detail.invigilateUsername,
                evaluateUsername: detail.evaluateUsername,
                watcherUsername: detail.watcherUsername,
                staffUsername: detail.staffUsername,
                propositionUsername: detail.propositionUsername,
                markingUsername: detail.markingUsername,
                orgId: detail.orgId,
                orgName: detail.orgName,
            });
        }
        else if (open && !isEdit) {
            form.resetFields();
            form.setFieldsValue({ type: 0, source: 0, sortNumber: 1 });
        }
    }, [open, isEdit, detail, form]);
    const saveMu = useMutation({
        mutationFn: async (values) => {
            const payload = {
                ...values,
                examDate: values.examDate
                    ? (typeof values.examDate === 'string' ? values.examDate : values.examDate.format('YYYY-MM-DD'))
                    : undefined,
            };
            if (isEdit) {
                return updateExam(editingId, payload);
            }
            return createExam(payload);
        },
        onSuccess: async (created, values) => {
            if (!isEdit && values.trainId && created?.id) {
                // 选了培训 → 创建后自动导入考生
                try {
                    const r = await importExamPeopleFromTrain(created.id, values.trainId);
                    message.success(`已导入 ${r.count} 名考生`);
                }
                catch {
                    message.warning('考生自动导入失败，可稍后在「考生」按钮中重试');
                }
            }
            else {
                message.success(isEdit ? '修改成功' : '新增成功');
            }
            queryClient.invalidateQueries({ queryKey: ['train-exam-list'] });
            onClose();
        },
        onError: () => message.error(isEdit ? '修改失败' : '新增失败'),
    });
    return (_jsx(Modal, { title: isEdit ? '编辑考试计划' : '新增考试计划', open: open, width: 720, destroyOnHidden: true, onCancel: onClose, onOk: () => form.submit(), confirmLoading: saveMu.isPending, children: _jsxs(Form, { form: form, layout: "vertical", onFinish: v => saveMu.mutate(v), initialValues: { type: 0, source: 0, sortNumber: 1 }, children: [_jsx(Form.Item, { name: "title", label: "\u6807\u9898", rules: [{ required: true, message: '请输入标题' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u8003\u8BD5\u8BA1\u5212\u6807\u9898" }) }), _jsxs(Space.Compact, { block: true, children: [_jsx(Form.Item, { name: "level", label: "\u7B49\u7EA7", style: { width: '25%' }, children: _jsx(Input, { placeholder: "\u5982\uFF1A\u521D\u7EA7" }) }), _jsx(Form.Item, { name: "type", label: "\u7C7B\u578B", style: { width: '25%', marginLeft: 8 }, children: _jsx(Select, { options: [{ value: 0, label: '线上' }, { value: 1, label: '线下' }] }) }), _jsx(Form.Item, { name: "source", label: "\u6765\u6E90", style: { width: '25%', marginLeft: 8 }, children: _jsx(Select, { options: [{ value: 0, label: '内部' }, { value: 1, label: '外部' }, { value: 3, label: '赛前复习' }] }) }), _jsx(Form.Item, { name: "sortNumber", label: "\u6392\u5E8F\u53F7", style: { width: '20%', marginLeft: 8 }, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) })] }), _jsx(Form.Item, { name: "orgId", label: "\u7ED1\u5B9A\u4F01\u4E1A", rules: [{ required: true, message: '请选择企业' }], children: _jsx(Select, { allowClear: true, showSearch: true, placeholder: "\u8BF7\u9009\u62E9\u4F01\u4E1A", optionFilterProp: "label", options: (orgList ?? []).map(o => ({ value: o.id, label: o.name })), onChange: (value) => {
                            const org = orgList?.find(o => o.id === value);
                            form.setFieldsValue({ orgName: org?.name });
                        } }) }), _jsx(Form.Item, { name: "orgName", hidden: true, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "trainId", label: "\u5173\u8054\u57F9\u8BAD", children: _jsx(Select, { allowClear: true, showSearch: true, placeholder: "\u9009\u62E9\u57F9\u8BAD\uFF08\u7528\u4E8E\u81EA\u52A8\u5BFC\u5165\u8003\u751F\uFF09", optionFilterProp: "label", options: (trainList?.list ?? []).map(t => ({ value: t.id, label: t.title })) }) }), _jsx(Form.Item, { name: "courseIdList", label: "\u5173\u8054\u8BFE\u7A0B/\u79D1\u76EE\uFF08\u591A\u9009\uFF09", children: _jsx(Select, { mode: "multiple", allowClear: true, placeholder: "\u9009\u62E9\u8BFE\u7A0B\uFF0C\u5C06\u81EA\u52A8\u56DE\u586B\u5DE5\u79CD/\u7B49\u7EA7", optionFilterProp: "label", options: (courseList?.items ?? []).map(c => ({ value: c.id, label: c.title })) }) }), _jsxs(Space.Compact, { block: true, children: [_jsx(Form.Item, { name: "examDate", label: "\u8003\u8BD5\u65E5\u671F", style: { width: '50%' }, children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "examTime", label: "\u8003\u8BD5\u65F6\u957F(\u5206\u949F)", style: { width: '50%', marginLeft: 8 }, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) })] }), _jsxs("details", { children: [_jsx("summary", { style: { cursor: 'pointer', color: '#888' }, children: "\u8003\u52A1\u4EBA\u5458\uFF08\u9009\u586B\uFF09" }), _jsxs(Space.Compact, { block: true, style: { marginTop: 8 }, children: [_jsx(Form.Item, { name: "invigilateUsername", label: "\u76D1\u8003\u4EBA", style: { width: '50%' }, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "evaluateUsername", label: "\u8003\u8BC4\u4EBA", style: { width: '50%', marginLeft: 8 }, children: _jsx(Input, {}) })] }), _jsxs(Space.Compact, { block: true, children: [_jsx(Form.Item, { name: "watcherUsername", label: "\u7763\u5BFC\u5458", style: { width: '50%' }, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "staffUsername", label: "\u8003\u52A1\u4EBA\u5458", style: { width: '50%', marginLeft: 8 }, children: _jsx(Input, {}) })] }), _jsxs(Space.Compact, { block: true, children: [_jsx(Form.Item, { name: "propositionUsername", label: "\u547D\u9898\u4EBA", style: { width: '50%' }, children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "markingUsername", label: "\u9605\u5377\u4EBA", style: { width: '50%', marginLeft: 8 }, children: _jsx(Input, {}) })] })] })] }) }));
}
