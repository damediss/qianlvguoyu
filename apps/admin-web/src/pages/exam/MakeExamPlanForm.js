import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, InputNumber, Select, Switch, Typography, } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { listKnowledgeRates } from '../../api/exam';
import { DIFFICULTY_MAP } from '../../types/exam';
const { Title } = Typography;
export const MAKE_EXAM_PLAN_QUESTION_TYPES = [
    { label: '单选题', value: 'single' },
    { label: '多选题', value: 'multi' },
    { label: '判断题', value: 'judge' },
    { label: '填空题', value: 'fill' },
    { label: '简答题', value: 'essay' },
    { label: '论述题', value: 'explain' },
    { label: '计算题', value: 'cal' },
    { label: '简答题(答)', value: 'answer' },
    { label: '分析题', value: 'analysis' },
    { label: '简单填空', value: 'simple_fill' },
];
/** 难度下拉选项：只显示中文档位，值域 0~4（与 DIFFICULTY_MAP 一致，后端按数值区间抽题） */
export const MAKE_EXAM_PLAN_DIFFICULTY_OPTIONS = [0, 1, 2, 3, 4]
    .map((value) => ({ label: DIFFICULTY_MAP[value], value }));
/** 将表单值转为与题库「添加组卷」一致的创建请求体 */
export function buildMakeExamPlanCreatePayload(values) {
    const isAll = values.isAll;
    return {
        title: String(values.title ?? ''),
        courseId: Number(values.courseId),
        examType: Number(values.examType),
        rateId: values.rateId || undefined,
        type: values.type,
        time: values.time,
        isComputer: values.isComputer ? 1 : 0,
        isPrint: values.isPrint ? 1 : 0,
        openRate: values.openRate,
        totalPoint: values.totalPoint,
        passPoint: values.passPoint,
        isAll,
        comments: values.comments || undefined,
        detailList: isAll === 1 ? undefined : values.detailList,
    };
}
/**
 * 题库「组卷计划」添加/编辑表单（与 MakeExamPlanPage 弹窗字段一致）。
 * 题库页与跟班「添加组卷」共用，禁止另写第二套字段。
 */
export function MakeExamPlanForm({ form, courses, examTypeLocked, coursePlaceholder, children, }) {
    const watchedCourseId = Form.useWatch('courseId', form);
    const rateQuery = useQuery({
        queryKey: ['knowledge-rates-list', watchedCourseId],
        queryFn: () => listKnowledgeRates(watchedCourseId),
        enabled: watchedCourseId != null && watchedCourseId > 0,
    });
    const rates = (rateQuery.data || []);
    return (_jsxs(Form, { form: form, layout: "horizontal", labelCol: { flex: '80px' }, wrapperCol: { flex: 1 }, labelAlign: "right", children: [_jsx(Form.Item, { name: "title", label: "\u540D\u79F0", rules: [{ required: true, message: '请输入名称' }], children: _jsx(Input, {}) }), _jsxs("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }, children: [_jsx(Form.Item, { name: "courseId", label: "\u79D1\u76EE", rules: [{ required: true, message: '请选择科目' }], children: _jsx(Select, { placeholder: coursePlaceholder || '选择科目', options: courses.map((c) => ({ label: c.title, value: c.id })), showSearch: true, optionFilterProp: "label" }) }), _jsx(Form.Item, { name: "examType", label: "\u8003\u8BD5\u7C7B\u578B", rules: [{ required: true }], children: _jsx(Select, { disabled: examTypeLocked != null, options: [
                                { label: '理论', value: 0 },
                                { label: '实操', value: 1 },
                            ] }) }), _jsx(Form.Item, { name: "time", label: "\u65F6\u957F(\u5206\u949F)", children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "rateId", label: "\u77E5\u8BC6\u6BD4\u91CD", children: _jsx(Select, { allowClear: true, placeholder: "\u9009\u62E9\u6BD4\u91CD(\u53EF\u9009)", options: rates.map((r) => ({ label: r.title, value: r.id })) }) }), _jsx(Form.Item, { name: "totalPoint", label: "\u603B\u5206", children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "passPoint", label: "\u53CA\u683C\u5206", children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: "isComputer", label: "\u673A\u8003", valuePropName: "checked", children: _jsx(Switch, {}) }), _jsx(Form.Item, { name: "isPrint", label: "\u6253\u5370", valuePropName: "checked", children: _jsx(Switch, {}) }), _jsx(Form.Item, { name: "isAll", label: "\u5168\u9009\u6A21\u5F0F", children: _jsx(Select, { options: [
                                { label: '全选(自动统计)', value: 1 },
                                { label: '自定义', value: 0 },
                            ] }) })] }), _jsx(Form.Item, { name: "comments", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2 }) }), _jsx(Form.Item, { noStyle: true, shouldUpdate: (prev, cur) => prev.isAll !== cur.isAll, children: ({ getFieldValue }) => getFieldValue('isAll') !== 1 ? (_jsxs(_Fragment, { children: [_jsx(Title, { level: 5, children: "\u9898\u578B\u914D\u7F6E" }), _jsx(Form.List, { name: "detailList", children: (fields, { add, remove }) => (_jsxs(_Fragment, { children: [fields.map((field) => (_jsx(Card, { size: "small", style: { marginBottom: 8 }, extra: _jsx(MinusCircleOutlined, { onClick: () => remove(field.name), style: { color: '#ff4d4f' } }), children: _jsxs("div", { style: {
                                                display: 'grid',
                                                gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr 1fr 1fr',
                                                gap: 12,
                                                alignItems: 'end',
                                            }, children: [_jsx(Form.Item, { name: [field.name, 'type'], label: "\u9898\u578B", rules: [{ required: true }], style: { marginBottom: 0 }, children: _jsx(Select, { options: MAKE_EXAM_PLAN_QUESTION_TYPES }) }), _jsx(Form.Item, { name: [field.name, 'num'], label: "\u6570\u91CF", rules: [{ required: true }], style: { marginBottom: 0 }, children: _jsx(InputNumber, { min: 1, style: { width: '100%' } }) }), _jsx(Form.Item, { name: [field.name, 'point'], label: "\u6BCF\u9898\u5206\u503C", style: { marginBottom: 0 }, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: [field.name, 'difficultyStart'], label: "\u96BE\u5EA6\u8D77", style: { marginBottom: 0 }, children: _jsx(Select, { options: MAKE_EXAM_PLAN_DIFFICULTY_OPTIONS, placeholder: "\u4E0D\u9650", allowClear: true }) }), _jsx(Form.Item, { name: [field.name, 'difficultyEnd'], label: "\u96BE\u5EA6\u6B62", style: { marginBottom: 0 }, children: _jsx(Select, { options: MAKE_EXAM_PLAN_DIFFICULTY_OPTIONS, placeholder: "\u4E0D\u9650", allowClear: true }) }), _jsx(Form.Item, { name: [field.name, 'startPoint'], label: "\u5206\u503C\u8D77", style: { marginBottom: 0 }, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { name: [field.name, 'endPoint'], label: "\u5206\u503C\u6B62", style: { marginBottom: 0 }, children: _jsx(InputNumber, { min: 0, style: { width: '100%' } }) })] }) }, field.key))), _jsx(Button, { type: "dashed", onClick: () => add(), block: true, icon: _jsx(PlusOutlined, {}), children: "\u6DFB\u52A0\u9898\u578B" })] })) })] })) : null }), children] }));
}
