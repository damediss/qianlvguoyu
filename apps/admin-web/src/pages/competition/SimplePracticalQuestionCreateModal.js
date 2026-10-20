import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useMutation } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Form, Input, InputNumber, Modal, Radio, Row, Space, Typography, message, } from 'antd';
import { useEffect } from 'react';
import { createCompetitionSimplePracticalQuestion } from '../../api/competitionQuestion';
function escapeHtml(value) {
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/\r?\n/g, '<br/>');
}
function toParagraph(value) {
    return `<p>${escapeHtml(value.trim())}</p>`;
}
function formatScore(value) {
    return Number(value.toFixed(2)).toString();
}
export function SimplePracticalQuestionCreateModal({ open, eventId, workTypeId, levelId, onClose, onSaved }) {
    const [form] = Form.useForm();
    const scoringMethod = Form.useWatch('scoringMethod', form) || 'deductive';
    const items = Form.useWatch('items', form) ?? [];
    const totalScore = items.reduce((total, item) => total + Number(item?.score || 0), 0);
    useEffect(() => {
        if (open) {
            form.setFieldsValue({
                projectName: '',
                scoringMethod: 'deductive',
                items: [{ assessmentContent: '', itemName: '', standard: '', stepText: '', score: undefined }],
            });
        }
    }, [form, open]);
    const saveMutation = useMutation({
        mutationFn: async (values) => {
            const projectName = values.projectName.trim();
            const method = values.scoringMethod;
            const validRows = values.items.map((row) => ({
                assessmentContent: row.assessmentContent?.trim() || '',
                itemName: row.itemName?.trim() || '',
                standard: row.standard?.trim() || '',
                stepText: row.stepText?.trim() || '',
                score: Number(row.score),
            }));
            if (!projectName)
                throw new Error('请输入考核项目');
            if (validRows.length === 0)
                throw new Error('请至少添加一个评分项');
            if (validRows.some((row) => !row.assessmentContent || !row.itemName || !row.standard || !Number.isFinite(row.score) || row.score <= 0)) {
                throw new Error('请填写每行的考核内容、评分项、评分标准和正数分值');
            }
            if (method === 'deductive' && validRows.some((row) => {
                const step = Number(row.stepText);
                return !row.stepText || !Number.isFinite(step) || step <= 0
                    || Math.abs((row.score / step) - Math.round(row.score / step)) > 1e-8;
            })) {
                throw new Error('扣分制的步骤必须填写正数扣分步长，且能整除对应评分项分值');
            }
            const itemTotal = validRows.reduce((total, row) => total + row.score, 0);
            if (itemTotal <= 0 || itemTotal > 100)
                throw new Error('项目总分必须大于0且不超过100分');
            const contents = new Map();
            validRows.forEach((row) => {
                const current = contents.get(row.assessmentContent) ?? [];
                current.push(row);
                contents.set(row.assessmentContent, current);
            });
            const body = {
                eventWorkTypeId: workTypeId,
                eventWorkTypeLevelId: levelId,
                contentType: 'practical',
                questionType: 'practical',
                stem: projectName,
                status: 'enabled',
                practicalDetail: {
                    durationMinutes: 60,
                    scoreDifference: Math.min(itemTotal, 10),
                    totalScore: itemTotal,
                    scoringMethod: method,
                    assessmentContents: Array.from(contents.entries()).map(([content, rows], contentIndex) => ({
                        sortOrder: contentIndex + 1,
                        contentHtml: toParagraph(content),
                        elements: rows.map((row, rowIndex) => ({
                            elementOrder: rowIndex + 1,
                            elementHtml: toParagraph(row.stepText),
                            itemName: row.itemName,
                            allocationEnabled: true,
                            maxScore: row.score,
                            standardHtml: toParagraph(row.standard),
                            maxDeduction: method === 'deductive' ? row.score : undefined,
                            deductionStep: method === 'deductive' ? Number(row.stepText) : undefined,
                        })),
                    })),
                },
            };
            return createCompetitionSimplePracticalQuestion(eventId, body);
        },
        onSuccess: () => {
            message.success('实操评分项已新增');
            form.resetFields();
            onSaved();
        },
        onError: (error) => message.error(error.message || '新增失败'),
    });
    const close = () => {
        form.resetFields();
        onClose();
    };
    return (_jsx(Modal, { title: "\u65B0\u589E\u5B9E\u64CD\u8BC4\u5206\u9879", open: open, onCancel: close, onOk: () => form.submit(), confirmLoading: saveMutation.isPending, destroyOnClose: true, width: "min(1280px, 96vw)", styles: { body: { maxHeight: 'calc(100vh - 180px)', overflowY: 'auto' } }, children: _jsxs(Form, { form: form, layout: "vertical", onFinish: (values) => saveMutation.mutate(values), children: [_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u4E00\u6B21\u65B0\u589E\u4E00\u4E2A\u8003\u6838\u9879\u76EE\uFF0C\u53EF\u6DFB\u52A0\u591A\u884C\u8BC4\u5206\u9879\uFF1B\u76F8\u540C\u8003\u6838\u5185\u5BB9\u4F1A\u81EA\u52A8\u5408\u5E76\u3002" }), _jsxs(Row, { gutter: 16, children: [_jsx(Col, { xs: 24, md: 12, children: _jsx(Form.Item, { name: "projectName", label: "\u8003\u6838\u9879\u76EE", rules: [{ required: true, whitespace: true, message: '请输入考核项目' }], children: _jsx(Input, { maxLength: 255, placeholder: "\u8BF7\u8F93\u5165\u8003\u6838\u9879\u76EE\u540D\u79F0" }) }) }), _jsx(Col, { xs: 24, md: 12, children: _jsx(Form.Item, { name: "scoringMethod", label: "\u8BA1\u5206\u65B9\u5F0F", rules: [{ required: true, message: '请选择计分方式' }], children: _jsx(Radio.Group, { options: [{ label: '加分制', value: 'additive' }, { label: '扣分制', value: 'deductive' }] }) }) })] }), _jsx(Typography.Text, { strong: true, children: "\u8BC4\u5206\u9879\u660E\u7EC6" }), _jsx(Form.List, { name: "items", children: (fields, { add, remove }) => (_jsxs(Space, { direction: "vertical", size: 8, style: { display: 'flex', marginTop: 8 }, children: [_jsxs(Row, { gutter: 8, style: { paddingInline: 8 }, children: [_jsx(Col, { span: 5, children: _jsx(Typography.Text, { type: "secondary", children: "\u8003\u6838\u5185\u5BB9" }) }), _jsx(Col, { span: 4, children: _jsx(Typography.Text, { type: "secondary", children: "\u8BC4\u5206\u9879" }) }), _jsx(Col, { span: 5, children: _jsx(Typography.Text, { type: "secondary", children: "\u8BC4\u5206\u6807\u51C6" }) }), _jsx(Col, { span: 4, children: _jsx(Typography.Text, { type: "secondary", children: "\u6B65\u9AA4" }) }), _jsx(Col, { span: 3, children: _jsx(Typography.Text, { type: "secondary", children: "\u5206\u503C" }) }), _jsx(Col, { span: 3, children: _jsx(Typography.Text, { type: "secondary", children: "\u64CD\u4F5C" }) })] }), fields.map((field, index) => (_jsxs(Card, { size: "small", styles: { body: { padding: 8 } }, children: [_jsxs(Row, { gutter: 8, align: "top", children: [_jsx(Col, { xs: 24, md: 5, children: _jsx(Form.Item, { name: [field.name, 'assessmentContent'], rules: [{ required: true, whitespace: true, message: '请填写考核内容' }], style: { marginBottom: 0 }, children: _jsx(Input.TextArea, { autoSize: { minRows: 1 }, placeholder: "\u8003\u6838\u5185\u5BB9" }) }) }), _jsx(Col, { xs: 24, md: 4, children: _jsx(Form.Item, { name: [field.name, 'itemName'], rules: [{ required: true, whitespace: true, message: '请填写评分项' }], style: { marginBottom: 0 }, children: _jsx(Input.TextArea, { autoSize: { minRows: 1 }, maxLength: 255, placeholder: "\u8BC4\u5206\u9879" }) }) }), _jsx(Col, { xs: 24, md: 5, children: _jsx(Form.Item, { name: [field.name, 'standard'], rules: [{ required: true, whitespace: true, message: '请填写评分标准' }], style: { marginBottom: 0 }, children: _jsx(Input.TextArea, { autoSize: { minRows: 1 }, placeholder: "\u8BC4\u5206\u6807\u51C6" }) }) }), _jsx(Col, { xs: 24, md: 4, children: _jsx(Form.Item, { name: [field.name, 'stepText'], rules: scoringMethod === 'deductive' ? [{ required: true, whitespace: true, message: '请填写扣分步长' }] : [], style: { marginBottom: 0 }, children: _jsx(Input, { placeholder: scoringMethod === 'deductive' ? '正数扣分步长' : '步骤说明（选填）' }) }) }), _jsx(Col, { xs: 24, md: 3, children: _jsx(Form.Item, { name: [field.name, 'score'], rules: [{ required: true, type: 'number', min: 0.01, message: '请输入正数分值' }], style: { marginBottom: 0 }, children: _jsx(InputNumber, { min: 0.01, max: 100, precision: 2, style: { width: '100%' }, placeholder: "\u5206\u503C" }) }) }), _jsx(Col, { xs: 24, md: 3, children: _jsx(Button, { danger: true, type: "text", icon: _jsx(DeleteOutlined, {}), disabled: fields.length <= 1, onClick: () => remove(field.name), children: "\u5220\u9664" }) })] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u7B2C ", index + 1, " \u9879"] })] }, field.key))), _jsx(Button, { type: "dashed", block: true, icon: _jsx(PlusOutlined, {}), onClick: () => add({ assessmentContent: '', itemName: '', standard: '', stepText: '', score: undefined }), children: "\u6DFB\u52A0\u8BC4\u5206\u9879" }), _jsx("div", { style: { textAlign: 'right' }, children: _jsxs(Typography.Text, { strong: true, children: ["\u9879\u76EE\u603B\u5206\uFF1A", formatScore(totalScore), " \u5206"] }) })] })) })] }) }));
}
