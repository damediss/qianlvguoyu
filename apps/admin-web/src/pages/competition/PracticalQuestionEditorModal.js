import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, EyeOutlined, PlusOutlined, } from '@ant-design/icons';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Checkbox, Col, Form, Input, InputNumber, Modal, Radio, Row, Select, Space, Steps, Tabs, Table, Typography, message, } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { createCompetitionQuestion, getCompetitionQuestion, updateCompetitionQuestion, } from '../../api/competitionQuestion';
import { RichTextEditor, RichTextPreview } from '../../components/RichTextEditor';
import './PracticalQuestionEditorModal.css';
const emptyElement = (elementOrder = 1) => ({
    key: `element-${Date.now()}-${Math.random()}`,
    elementOrder,
    elementHtml: '<p></p>',
    allocationEnabled: true,
    maxScore: undefined,
    standardHtml: '<p></p>',
    maxDeduction: undefined,
    deductionStep: undefined,
});
const emptyContent = (sortOrder = 1) => ({
    key: `content-${Date.now()}-${Math.random()}`,
    sortOrder,
    contentHtml: '<p></p>',
    elements: [emptyElement()],
});
function htmlText(value) {
    return (value || '').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
}
function nextKey(prefix, id) {
    return `${prefix}-${id || ''}-${Date.now()}-${Math.random()}`;
}
function fromDetail(question, detail) {
    const contents = detail?.assessmentContents || [];
    if (contents.length > 0) {
        return contents.map((content, contentIndex) => ({
            key: nextKey('content', content.id),
            sortOrder: content.sortOrder || contentIndex + 1,
            contentHtml: content.contentHtml || '<p></p>',
            elements: (content.elements || []).map((element, elementIndex) => ({
                key: nextKey('element', element.id),
                elementOrder: element.elementOrder || elementIndex + 1,
                elementHtml: element.elementHtml || '<p></p>',
                allocationEnabled: element.allocationEnabled !== false,
                maxScore: element.maxScore == null ? undefined : Number(element.maxScore),
                standardHtml: element.standardHtml || '<p></p>',
                maxDeduction: element.maxDeduction == null ? undefined : Number(element.maxDeduction),
                deductionStep: element.deductionStep == null ? undefined : Number(element.deductionStep),
            })),
        }));
    }
    const legacy = (question.scoreItems || []).map((item, index) => ({
        key: nextKey('element', item.id),
        elementOrder: index + 1,
        elementHtml: item.itemName ? `<p>${item.itemName}</p>` : '<p></p>',
        allocationEnabled: true,
        maxScore: item.maxScore == null ? undefined : Number(item.maxScore),
        standardHtml: item.criterion ? `<p>${item.criterion}</p>` : '<p></p>',
        maxDeduction: item.maxDeduction == null ? undefined : Number(item.maxDeduction),
        deductionStep: item.deductionStep == null ? undefined : Number(item.deductionStep),
    }));
    return [{
            ...emptyContent(),
            contentHtml: detail?.assessmentRequirementsHtml || question.operationContent || '<p>默认考核内容</p>',
            elements: legacy.length > 0 ? legacy : [emptyElement()],
        }];
}
function normalizeContents(contents) {
    return contents.map((content, contentIndex) => ({
        ...content,
        sortOrder: contentIndex + 1,
        elements: content.elements.map((element, elementIndex) => ({ ...element, elementOrder: elementIndex + 1 })),
    }));
}
function contentAllocatedTotal(content) {
    return content.elements.reduce((sum, element) => sum + (element.allocationEnabled ? Number(element.maxScore || 0) : 0), 0);
}
function buildPreviewRows(contents, values) {
    const rows = [];
    for (const content of contents) {
        const elements = content.elements.length > 0 ? content.elements : [emptyElement()];
        const contentTotal = elements.reduce((sum, element) => sum + (element.allocationEnabled ? Number(element.maxScore || 0) : 0), 0);
        elements.forEach((element, index) => rows.push({
            key: `${content.key}-${element.key}`,
            contentKey: content.key,
            contentHtml: content.contentHtml,
            contentTotal,
            contentRowSpan: elements.length,
            contentFirst: index === 0,
            elementHtml: element.elementHtml,
            maxScore: element.allocationEnabled ? element.maxScore : undefined,
            standardHtml: element.standardHtml,
            maxDeduction: element.allocationEnabled && values.scoringMethod === 'deductive' ? element.maxDeduction : undefined,
            deductionStep: element.allocationEnabled && values.scoringMethod === 'deductive' ? element.deductionStep : undefined,
            scoringMethod: values.scoringMethod,
            allocationEnabled: element.allocationEnabled,
        }));
    }
    return rows;
}
export function PracticalScoreTablePreview({ contents, values }) {
    const rows = useMemo(() => buildPreviewRows(contents, values), [contents, values]);
    const columns = [
        {
            title: '考核内容', dataIndex: 'contentHtml', width: 180,
            render: (value, row) => row.contentFirst ? { children: _jsx(RichTextPreview, { value: value }), props: { rowSpan: row.contentRowSpan } } : { children: null, props: { rowSpan: 0 } },
        },
        {
            title: '总分', dataIndex: 'contentTotal', width: 70, align: 'center',
            render: (value, row) => row.contentFirst ? { children: value.toFixed(2).replace(/\.00$/, ''), props: { rowSpan: row.contentRowSpan } } : { children: null, props: { rowSpan: 0 } },
        },
        { title: '评分要素', dataIndex: 'elementHtml', width: 190, render: (value) => _jsx(RichTextPreview, { value: value }) },
        { title: '配分', dataIndex: 'maxScore', width: 70, align: 'center', render: (value, row) => row.allocationEnabled ? value : null },
        { title: '评分标准', dataIndex: 'standardHtml', width: 360, render: (value) => _jsx(RichTextPreview, { value: value }) },
        { title: '最大扣分', dataIndex: 'maxDeduction', width: 90, align: 'center', render: (value, row) => row.scoringMethod === 'deductive' && row.allocationEnabled ? value : null },
        { title: '步长', dataIndex: 'deductionStep', width: 70, align: 'center', render: (value, row) => row.scoringMethod === 'deductive' && row.allocationEnabled ? value : null },
        { title: '评分方式', dataIndex: 'scoringMethod', width: 90, align: 'center', render: (value) => value === 'additive' ? '加分' : '扣分' },
    ];
    return _jsx(Table, { bordered: true, size: "small", pagination: false, rowKey: "key", dataSource: rows, columns: columns, scroll: { x: 1120 } });
}
export function PracticalQuestionEditorModal({ open, eventId, workTypeId, levelId, workTypes, levels, editing, onClose, onSaved, blueprintId, blueprintNodes, }) {
    const [form] = Form.useForm();
    const [activeTab, setActiveTab] = useState('requirements');
    const [step, setStep] = useState(0);
    const [contents, setContents] = useState([emptyContent()]);
    const [assessmentRequirementsHtml, setAssessmentRequirementsHtml] = useState('');
    const [examRoomPrepareHtml, setExamRoomPrepareHtml] = useState('');
    const [candidatePrepareHtml, setCandidatePrepareHtml] = useState('');
    const [negativeItemsHtml, setNegativeItemsHtml] = useState('');
    const [questionMaterialsHtml, setQuestionMaterialsHtml] = useState('');
    const [previewOpen, setPreviewOpen] = useState(false);
    const detailQuery = useQuery({
        queryKey: ['competition-practical-question-editor', eventId, editing?.id],
        queryFn: () => getCompetitionQuestion(eventId, editing.id),
        enabled: open && Boolean(editing?.id),
    });
    useEffect(() => {
        if (!open)
            return;
        setStep(0);
        setActiveTab('requirements');
        setPreviewOpen(false);
        if (!editing) {
            form.resetFields();
            form.setFieldsValue({
                eventWorkTypeId: String(workTypeId), eventWorkTypeLevelId: String(levelId),
                durationMinutes: 60, scoreDifference: 10, totalScore: 100,
                scoringMethod: 'deductive', status: 'enabled', stem: '',
                blueprintId, blueprintNodeId: undefined,
            });
            setContents([emptyContent()]);
            setAssessmentRequirementsHtml('');
            setExamRoomPrepareHtml('');
            setCandidatePrepareHtml('');
            setNegativeItemsHtml('');
            setQuestionMaterialsHtml('');
        }
    }, [editing, form, levelId, open, workTypeId]);
    useEffect(() => {
        const question = detailQuery.data;
        if (!open || !editing || !question)
            return;
        const detail = question.practicalDetail;
        form.setFieldsValue({
            eventWorkTypeId: question.eventWorkTypeId,
            eventWorkTypeLevelId: question.eventWorkTypeLevelId,
            stem: question.stem,
            durationMinutes: detail?.durationMinutes || 60,
            scoreDifference: detail?.scoreDifference == null ? 10 : Number(detail.scoreDifference),
            totalScore: detail?.totalScore == null ? 100 : Number(detail.totalScore),
            scoringMethod: detail?.scoringMethod === 'additive' ? 'additive' : 'deductive',
            status: question.status,
            blueprintNodeId: question.blueprintNodeId || undefined,
        });
        setContents(fromDetail(question, detail));
        setAssessmentRequirementsHtml(detail?.assessmentRequirementsHtml || question.operationContent || '');
        setExamRoomPrepareHtml(detail?.examRoomPrepareHtml || question.equipmentPrepare || '');
        setCandidatePrepareHtml(detail?.candidatePrepareHtml || question.studentPrepare || '');
        setNegativeItemsHtml(detail?.negativeItemsHtml || '');
        setQuestionMaterialsHtml(detail?.questionMaterialsHtml || '');
    }, [detailQuery.data, editing, form, open]);
    const saveMutation = useMutation({
        mutationFn: async (values) => {
            const normalizedContents = normalizeContents(contents);
            const allocatedTotal = normalizedContents.reduce((sum, content) => sum + content.elements.reduce((inner, element) => inner + (element.allocationEnabled ? Number(element.maxScore || 0) : 0), 0), 0);
            if (normalizedContents.some((content) => !htmlText(content.contentHtml)))
                throw new Error('请完善所有考核内容');
            if (normalizedContents.some((content) => content.elements.length === 0 || content.elements.some((element) => !htmlText(element.elementHtml) || !htmlText(element.standardHtml)))) {
                throw new Error('请完善所有评分要素和评分标准');
            }
            if (Number(values.durationMinutes) <= 0 || !Number.isInteger(Number(values.durationMinutes))) {
                throw new Error('考试时间必须为正整数分钟');
            }
            if (Number(values.totalScore) <= 0 || Number(values.totalScore) > 100) {
                throw new Error('试题总分必须大于0且不超过100分');
            }
            if (Number(values.scoreDifference) < 0 || Number(values.scoreDifference) > Number(values.totalScore)) {
                throw new Error('评分差值必须在0到试题总分之间');
            }
            if (Math.abs(allocatedTotal - Number(values.totalScore)) > 1e-8)
                throw new Error(`配分合计 ${allocatedTotal} 分，必须等于试题总分 ${values.totalScore} 分`);
            if (normalizedContents.some((content) => content.elements.some((element) => element.allocationEnabled && (element.maxScore == null || element.maxScore <= 0 || element.maxScore > 100)))) {
                throw new Error('最大配分必须大于0且不超过100分');
            }
            if (values.scoringMethod === 'deductive' && normalizedContents.some((content) => content.elements.some((element) => element.allocationEnabled && (element.maxDeduction == null || element.deductionStep == null || element.maxDeduction < 0 || element.maxDeduction > Number(element.maxScore) || element.deductionStep <= 0 || Math.abs((Number(element.maxDeduction) / Number(element.deductionStep)) - Math.round(Number(element.maxDeduction) / Number(element.deductionStep))) > 1e-8)))) {
                throw new Error('扣分制的最大扣分和步长设置无效');
            }
            if (values.scoringMethod === 'additive' && normalizedContents.some((content) => content.elements.some((element) => element.maxDeduction != null || element.deductionStep != null))) {
                throw new Error('加分制不需要填写最大扣分和步长');
            }
            const practicalDetail = {
                durationMinutes: Number(values.durationMinutes),
                scoreDifference: Number(values.scoreDifference),
                totalScore: Number(values.totalScore),
                scoringMethod: values.scoringMethod,
                assessmentRequirementsHtml,
                examRoomPrepareHtml,
                candidatePrepareHtml,
                negativeItemsHtml,
                questionMaterialsHtml,
                assessmentContents: normalizedContents.map(({ key: _key, ...content }) => ({
                    ...content,
                    elements: content.elements.map(({ key: _elementKey, ...element }) => element),
                })),
            };
            const body = {
                eventWorkTypeId: values.eventWorkTypeId,
                eventWorkTypeLevelId: values.eventWorkTypeLevelId,
                contentType: 'practical', questionType: 'practical', stem: values.stem,
                status: values.status, practicalDetail,
            };
            // Keep legacy/unbound questions editable.  Binding is sent for a new
            // question, or only when an editor explicitly picks a different node;
            // otherwise an archived historical binding is left untouched.
            if (!editing) {
                body.blueprintId = blueprintId;
                body.blueprintNodeId = values.blueprintNodeId;
            }
            else if (values.blueprintNodeId && values.blueprintNodeId !== editing.blueprintNodeId) {
                body.blueprintId = blueprintId;
                body.blueprintNodeId = values.blueprintNodeId;
            }
            return editing ? updateCompetitionQuestion(eventId, editing.id, body) : createCompetitionQuestion(eventId, body);
        },
        onSuccess: () => { message.success(editing ? '竞赛实操试题已更新' : '竞赛实操试题已创建'); onSaved(); },
        onError: (error) => message.error(error.message || '保存失败'),
    });
    const values = Form.useWatch([], form);
    const currentValues = values || {
        eventWorkTypeId: String(workTypeId), eventWorkTypeLevelId: String(levelId), stem: '',
        durationMinutes: 60, scoreDifference: 10, totalScore: 100, scoringMethod: 'deductive', status: 'enabled',
    };
    const updateContent = (key, updater) => {
        setContents((current) => normalizeContents(current.map((content) => content.key === key ? updater(content) : content)));
    };
    const updateElement = (contentKey, elementKey, updater) => {
        updateContent(contentKey, (content) => ({ ...content, elements: content.elements.map((element) => element.key === elementKey ? updater(element) : element) }));
    };
    const moveContent = (index, direction) => {
        setContents((current) => {
            const next = [...current];
            const target = index + direction;
            if (target < 0 || target >= next.length)
                return current;
            [next[index], next[target]] = [next[target], next[index]];
            return normalizeContents(next);
        });
    };
    const moveElement = (contentKey, index, direction) => {
        updateContent(contentKey, (content) => {
            const elements = [...content.elements];
            const target = index + direction;
            if (target < 0 || target >= elements.length)
                return content;
            [elements[index], elements[target]] = [elements[target], elements[index]];
            return { ...content, elements };
        });
    };
    const renderContentsStep = () => (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u7B2C\u4E00\u6B65\uFF1A\u589E\u52A0\u8003\u6838\u5185\u5BB9\u3002\u8003\u6838\u5185\u5BB9\u662F\u8BC4\u5206\u8868\u4E2D\u5DE6\u4FA7\u5408\u5E76\u5355\u5143\u683C\u7684\u5206\u7EC4\u6807\u9898\u3002" }), contents.map((content, index) => (_jsx(Card, { size: "small", title: _jsxs(Space, { children: ["\u8003\u6838\u5185\u5BB9 ", index + 1, _jsxs(Typography.Text, { type: "secondary", children: ["\u603B\u5206\uFF1A", contentAllocatedTotal(content).toFixed(2).replace(/\.00$/, ''), " \u5206"] })] }), extra: (_jsxs(Space, { size: 0, children: [_jsx(Button, { type: "text", size: "small", icon: _jsx(ArrowUpOutlined, {}), disabled: index === 0, onClick: () => moveContent(index, -1) }), _jsx(Button, { type: "text", size: "small", icon: _jsx(ArrowDownOutlined, {}), disabled: index === contents.length - 1, onClick: () => moveContent(index, 1) }), _jsx(Button, { type: "text", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), disabled: contents.length <= 1, onClick: () => setContents((current) => normalizeContents(current.filter((item) => item.key !== content.key))) })] })), children: _jsx(RichTextEditor, { minHeight: 120, value: content.contentHtml, onChange: (value) => updateContent(content.key, (item) => ({ ...item, contentHtml: value })), placeholder: "\u8BF7\u8F93\u5165\u8003\u6838\u5185\u5BB9" }) }, content.key))), _jsx(Button, { type: "dashed", block: true, icon: _jsx(PlusOutlined, {}), onClick: () => setContents((current) => [...current, emptyContent(current.length + 1)]), children: "\u589E\u52A0\u8003\u6838\u5185\u5BB9" })] }));
    const renderElementsStep = () => (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u7B2C\u4E8C\u6B65\uFF1A\u5F55\u5165\u8BC4\u5206\u8981\u7D20\u3002\u672A\u914D\u5206\u8981\u7D20\u4FDD\u7559\u5728\u9884\u89C8\u8868\u4E2D\uFF0C\u4F46\u4E0D\u8BA1\u5165\u8BD5\u9898\u603B\u5206\u3002" }), contents.map((content, contentIndex) => (_jsx(Card, { size: "small", title: _jsxs(Space, { children: ["\u8003\u6838\u5185\u5BB9 ", contentIndex + 1, _jsxs(Typography.Text, { type: "secondary", children: ["\u603B\u5206\uFF1A", contentAllocatedTotal(content).toFixed(2).replace(/\.00$/, ''), " \u5206"] }), _jsx(RichTextPreview, { value: content.contentHtml, className: "practical-question-editor__inline-preview" })] }), children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [content.elements.map((element, index) => (_jsxs(Card, { type: "inner", size: "small", title: `评分要素 ${index + 1}`, extra: (_jsxs(Space, { size: 0, children: [_jsx(Button, { type: "text", size: "small", icon: _jsx(ArrowUpOutlined, {}), disabled: index === 0, onClick: () => moveElement(content.key, index, -1) }), _jsx(Button, { type: "text", size: "small", icon: _jsx(ArrowDownOutlined, {}), disabled: index === content.elements.length - 1, onClick: () => moveElement(content.key, index, 1) }), _jsx(Button, { type: "text", danger: true, size: "small", icon: _jsx(DeleteOutlined, {}), disabled: content.elements.length <= 1, onClick: () => updateContent(content.key, (item) => ({ ...item, elements: item.elements.filter((candidate) => candidate.key !== element.key) })) })] })), children: [_jsx(RichTextEditor, { minHeight: 100, value: element.elementHtml, onChange: (value) => updateElement(content.key, element.key, (item) => ({ ...item, elementHtml: value })), placeholder: "\u8BF7\u8F93\u5165\u8BC4\u5206\u8981\u7D20" }), _jsxs(Row, { gutter: 16, align: "middle", style: { marginTop: 12 }, children: [_jsx(Col, { flex: "none", children: _jsx(Checkbox, { checked: element.allocationEnabled, onChange: (event) => updateElement(content.key, element.key, (item) => ({ ...item, allocationEnabled: event.target.checked, maxScore: event.target.checked ? item.maxScore : undefined, maxDeduction: event.target.checked ? item.maxDeduction : undefined, deductionStep: event.target.checked ? item.deductionStep : undefined })), children: "\u662F\u5426\u914D\u5206" }) }), element.allocationEnabled && _jsx(Col, { flex: "220px", children: _jsxs(Space, { children: ["\u6700\u5927\u914D\u5206", _jsx(InputNumber, { min: 0.01, max: 100, precision: 2, value: element.maxScore, onChange: (value) => updateElement(content.key, element.key, (item) => ({ ...item, maxScore: value == null ? undefined : Number(value) })) }), "\u5206"] }) })] })] }, element.key))), _jsx(Button, { type: "dashed", icon: _jsx(PlusOutlined, {}), onClick: () => updateContent(content.key, (item) => ({ ...item, elements: [...item.elements, emptyElement(item.elements.length + 1)] })), children: "\u589E\u52A0\u8BC4\u5206\u8981\u7D20" })] }) }, content.key)))] }));
    const renderStandardsStep = () => (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 12, children: [_jsx(Alert, { type: "info", showIcon: true, message: currentValues.scoringMethod === 'deductive' ? '第三步：录入评分标准、最大扣分和步长。最大扣分必须不超过该要素配分，且能被步长整除。' : '第三步：录入评分标准。当前为加分制，不需要填写最大扣分和步长。' }), contents.map((content, contentIndex) => (_jsx(Card, { size: "small", title: `考核内容 ${contentIndex + 1}`, children: content.elements.map((element, index) => (_jsxs(Card, { type: "inner", size: "small", title: `评分要素 ${index + 1}`, style: { marginBottom: 10 }, children: [_jsx(RichTextEditor, { minHeight: 110, value: element.standardHtml, onChange: (value) => updateElement(content.key, element.key, (item) => ({ ...item, standardHtml: value })), placeholder: "\u8BF7\u8F93\u5165\u8BC4\u5206\u6807\u51C6" }), element.allocationEnabled && currentValues.scoringMethod === 'deductive' && (_jsxs(Row, { gutter: 16, style: { marginTop: 12 }, children: [_jsx(Col, { children: _jsxs(Space, { children: ["\u6700\u5927\u6263\u5206", _jsx(InputNumber, { min: 0, max: element.maxScore || 100, precision: 2, value: element.maxDeduction, onChange: (value) => updateElement(content.key, element.key, (item) => ({ ...item, maxDeduction: value == null ? undefined : Number(value) })) }), "\u5206"] }) }), _jsx(Col, { children: _jsxs(Space, { children: ["\u6B65\u957F", _jsx(InputNumber, { min: 0.01, max: element.maxDeduction || element.maxScore || 100, precision: 2, value: element.deductionStep, onChange: (value) => updateElement(content.key, element.key, (item) => ({ ...item, deductionStep: value == null ? undefined : Number(value) })) }), "\u5206"] }) })] }))] }, element.key))) }, content.key)))] }));
    const tabItems = [
        { key: 'requirements', label: '考核要求', children: _jsx(RichTextEditor, { minHeight: 240, value: assessmentRequirementsHtml, onChange: setAssessmentRequirementsHtml, placeholder: "\u8BF7\u8F93\u5165\u8003\u6838\u8981\u6C42" }) },
        { key: 'room', label: '考场准备', children: _jsx(RichTextEditor, { minHeight: 240, value: examRoomPrepareHtml, onChange: setExamRoomPrepareHtml, placeholder: "\u8BF7\u8F93\u5165\u8003\u573A\u51C6\u5907" }) },
        { key: 'candidate', label: '考生准备', children: _jsx(RichTextEditor, { minHeight: 240, value: candidatePrepareHtml, onChange: setCandidatePrepareHtml, placeholder: "\u8BF7\u8F93\u5165\u8003\u751F\u51C6\u5907" }) },
        { key: 'scoring', label: '评分标准表', children: _jsx("div", { className: "practical-question-editor__scoring-tab", children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: 14, children: [_jsxs(Space, { wrap: true, children: [_jsx(Steps, { current: step, size: "small", items: [{ title: '考核内容' }, { title: '评分要素' }, { title: '评分标准' }] }), _jsx(Button, { icon: _jsx(EyeOutlined, {}), onClick: () => setPreviewOpen(true), children: "\u9884\u89C8\u8BC4\u5206\u8868" })] }), step === 0 ? renderContentsStep() : step === 1 ? renderElementsStep() : renderStandardsStep(), _jsxs("div", { className: "practical-question-editor__step-actions", children: [_jsx(Button, { disabled: step === 0, onClick: () => setStep((current) => current - 1), children: "\u4E0A\u4E00\u6B65" }), _jsx(Button, { type: "primary", disabled: step === 2, onClick: () => setStep((current) => current + 1), children: "\u4E0B\u4E00\u6B65" })] })] }) }) },
        { key: 'negative', label: '否定项', children: _jsx(RichTextEditor, { minHeight: 240, value: negativeItemsHtml, onChange: setNegativeItemsHtml, placeholder: "\u8BF7\u8F93\u5165\u5426\u5B9A\u9879" }) },
        { key: 'materials', label: '试题材料', children: _jsx(RichTextEditor, { minHeight: 240, value: questionMaterialsHtml, onChange: setQuestionMaterialsHtml, placeholder: "\u8BF7\u8F93\u5165\u8BD5\u9898\u6750\u6599" }) },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(Modal, { title: editing ? '编辑实操试题' : '新增实操试题', open: open, onCancel: onClose, onOk: () => form.submit(), confirmLoading: saveMutation.isPending, destroyOnClose: true, width: "min(1280px, 96vw)", styles: { body: { maxHeight: 'calc(100vh - 180px)', overflowY: 'auto' } }, children: detailQuery.isLoading && editing ? _jsx(Typography.Text, { children: "\u6B63\u5728\u52A0\u8F7D\u8BD5\u9898\u8BE6\u60C5\u2026" }) : (_jsxs(Form, { form: form, layout: "vertical", onFinish: (formValues) => saveMutation.mutate(formValues), children: [_jsxs(Row, { gutter: 16, children: [_jsx(Col, { xs: 24, lg: 8, children: _jsx(Form.Item, { name: "eventWorkTypeId", label: "\u5DE5\u79CD", rules: [{ required: true }], children: _jsx(Select, { disabled: true, options: workTypes.map((item) => ({ value: String(item.id), label: item.displayName || item.workTypeName || String(item.workTypeId) })) }) }) }), _jsx(Col, { xs: 24, lg: 6, children: _jsx(Form.Item, { name: "eventWorkTypeLevelId", label: "\u7B49\u7EA7", rules: [{ required: true }], children: _jsx(Select, { disabled: true, options: levels.map((item) => ({ value: String(item.id), label: item.levelName })) }) }) }), _jsx(Col, { xs: 24, lg: 10, children: _jsx(Form.Item, { name: "stem", label: "\u8BD5\u9898\u540D\u79F0", rules: [{ required: true, whitespace: true, message: '请输入试题名称' }], children: _jsx(Input, { maxLength: 255, placeholder: "\u8BF7\u8F93\u5165\u8BD5\u9898\u540D\u79F0" }) }) }), blueprintId && _jsx(Col, { xs: 24, lg: 10, children: _jsx(Form.Item, { name: "blueprintNodeId", label: "\u8003\u8BC4\u70B9", rules: [{ required: !editing, message: '请选择末级考评点' }], children: _jsx(Select, { allowClear: true, showSearch: true, optionFilterProp: "label", options: blueprintNodes, placeholder: editing ? '如需绑定可选择末级考评点' : '选择细目表末级考评点' }) }) })] }), _jsxs(Row, { gutter: 16, children: [_jsx(Col, { xs: 24, sm: 12, lg: 6, children: _jsx(Form.Item, { name: "durationMinutes", label: "\u8003\u8BD5\u65F6\u95F4\uFF08\u5206\u949F\uFF09", rules: [{ required: true, type: 'number', min: 1, message: '请输入正整数分钟' }, { validator: (_rule, value) => Number.isInteger(Number(value)) && Number(value) > 0 ? Promise.resolve() : Promise.reject(new Error('请输入正整数分钟')) }], children: _jsx(InputNumber, { precision: 0, min: 1, style: { width: '100%' } }) }) }), _jsx(Col, { xs: 24, sm: 12, lg: 6, children: _jsx(Form.Item, { name: "scoreDifference", label: "\u8BC4\u5206\u5DEE\u503C", dependencies: ['totalScore'], rules: [{ required: true, type: 'number', min: 0, message: '评分差值不能小于0' }, { validator: (_rule, value) => { const total = Number(form.getFieldValue('totalScore')); return Number(value) >= 0 && Number(value) <= total ? Promise.resolve() : Promise.reject(new Error('评分差值不能超过试题总分')); } }], children: _jsx(InputNumber, { min: 0, precision: 2, style: { width: '100%' } }) }) }), _jsx(Col, { xs: 24, sm: 12, lg: 6, children: _jsx(Form.Item, { name: "totalScore", label: "\u8BD5\u9898\u603B\u5206", rules: [{ required: true, type: 'number', min: 0.01, max: 100, message: '总分必须在0到100之间' }], children: _jsx(InputNumber, { min: 0.01, max: 100, precision: 2, style: { width: '100%' } }) }) }), _jsx(Col, { xs: 24, sm: 12, lg: 6, children: _jsx(Form.Item, { name: "scoringMethod", label: "\u8BC4\u5206\u65B9\u5F0F", rules: [{ required: true }], children: _jsx(Radio.Group, { options: [{ label: '加分制', value: 'additive' }, { label: '扣分制', value: 'deductive' }], onChange: (event) => { if (event.target.value === 'additive')
                                                setContents((current) => current.map((content) => ({ ...content, elements: content.elements.map((element) => ({ ...element, maxDeduction: undefined, deductionStep: undefined })) }))); } }) }) })] }), _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: tabItems }), _jsx(Form.Item, { name: "status", label: "\u72B6\u6001", hidden: true, children: _jsx(Select, { options: [{ value: 'enabled', label: '启用' }, { value: 'disabled', label: '停用' }] }) })] })) }), _jsxs(Modal, { title: "\u67E5\u770B\u8BC4\u5206\u8868", open: previewOpen, onCancel: () => setPreviewOpen(false), footer: null, width: "min(1500px, 98vw)", styles: { body: { maxHeight: 'calc(100vh - 150px)', overflowY: 'auto' } }, children: [_jsx(Typography.Title, { level: 5, children: currentValues.stem || '实操试题评分表' }), _jsx(PracticalScoreTablePreview, { contents: contents, values: currentValues })] })] }));
}
