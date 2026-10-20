import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Form, Input, InputNumber, Radio, Row, Select, Space, Switch, Tag, Typography, message, } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { getCompetitionPracticalExecutionConfig, listCompetitionPracticalScoringSchemes, saveCompetitionPracticalExecutionConfig, validateCompetitionPracticalExecutionConfig, } from '../../api/competitionPracticalExecutionConfig';
const defaults = (eventWorkTypeId, levelId) => ({
    eventWorkTypeId,
    eventWorkTypeLevelId: levelId ?? '',
    examName: '实操考试',
    durationMinutes: 90,
    startMode: 'UNIFIED_START',
    allowEarlySubmit: false,
    timeScoringEnabled: false,
    overtimeRuleType: 'NONE',
    earlyFinishRuleType: 'NONE',
    overtimeAction: 'ALLOW_CONTINUE',
});
function fromConfig(config) {
    return {
        eventWorkTypeId: config.eventWorkTypeId,
        eventWorkTypeLevelId: config.eventWorkTypeLevelId,
        scoringSchemeId: config.scoringSchemeId,
        examName: config.examName,
        durationMinutes: config.durationSeconds / 60,
        startMode: config.startMode,
        allowEarlySubmit: config.allowEarlySubmit,
        timeScoringEnabled: config.timeScoringEnabled,
        overtimeRuleType: config.overtimeRuleType,
        overtimeStepMinutes: config.overtimeStepSeconds == null ? undefined : config.overtimeStepSeconds / 60,
        overtimeScorePerStep: config.overtimeScorePerStep ?? undefined,
        overtimeFixedDeduction: config.overtimeFixedDeduction ?? undefined,
        overtimeMaxDeduction: config.overtimeMaxDeduction ?? undefined,
        earlyFinishRuleType: config.earlyFinishRuleType,
        earlyFinishStepMinutes: config.earlyFinishStepSeconds == null ? undefined : config.earlyFinishStepSeconds / 60,
        earlyFinishScorePerStep: config.earlyFinishScorePerStep ?? undefined,
        earlyFinishFixedBonus: config.earlyFinishFixedBonus ?? undefined,
        earlyFinishMaxBonus: config.earlyFinishMaxBonus ?? undefined,
        overtimeAction: config.overtimeAction,
        maxDurationMinutes: config.maxDurationSeconds == null ? undefined : config.maxDurationSeconds / 60,
        finalScoreUpperLimit: config.finalScoreUpperLimit,
    };
}
export function buildPracticalRulePreview(values, scheme) {
    const duration = values.durationMinutes ?? 0;
    const lines = [`本场实操${values.examName ? `“${values.examName}”` : ''}标准考试时间为${duration}分钟。`];
    if (!values.timeScoringEnabled) {
        lines.push('实际用时只记录，不参与最终成绩计算。');
    }
    else {
        if (values.overtimeRuleType === 'STEP_DEDUCTION') {
            lines.push(`超过标准时间后，每超${values.overtimeStepMinutes ?? 0}分钟扣${values.overtimeScorePerStep ?? 0}分，最大扣${values.overtimeMaxDeduction ?? 0}分；不足一步按整步向上取整。`);
        }
        else if (values.overtimeRuleType === 'FIXED_DEDUCTION') {
            lines.push(`超过标准时间后固定扣${values.overtimeFixedDeduction ?? 0}分。`);
        }
        else {
            lines.push('不进行超时扣分。');
        }
        if (values.earlyFinishRuleType === 'STEP_BONUS') {
            lines.push(`每提前${values.earlyFinishStepMinutes ?? 0}分钟完成加${values.earlyFinishScorePerStep ?? 0}分，最大奖励${values.earlyFinishMaxBonus ?? 0}分；只按完整步长计算。`);
        }
        else if (values.earlyFinishRuleType === 'FIXED_BONUS') {
            lines.push(`提前完成固定奖励${values.earlyFinishFixedBonus ?? 0}分。`);
        }
        else {
            lines.push('不进行提前完成奖励。');
        }
    }
    if (values.overtimeAction === 'AUTO_FINISH') {
        lines.push(`达到${values.maxDurationMinutes ?? 0}分钟后由服务端自动结束考试。`);
    }
    else {
        lines.push('超过标准时间后允许继续考试。');
    }
    const upper = values.finalScoreUpperLimit ?? scheme?.totalScore ?? 0;
    lines.push(`最终成绩范围为0至${upper}分；服务端以开始、结束时间和暂停时长计算。`);
    return lines;
}
function toRequest(values) {
    const seconds = (minutes) => minutes == null ? null : Math.round(minutes * 60);
    return {
        eventWorkTypeId: values.eventWorkTypeId,
        eventWorkTypeLevelId: values.eventWorkTypeLevelId,
        scoringSchemeId: values.scoringSchemeId,
        examName: values.examName.trim(),
        durationSeconds: seconds(values.durationMinutes),
        startMode: values.startMode,
        allowEarlySubmit: values.allowEarlySubmit,
        timeScoringEnabled: values.timeScoringEnabled,
        overtimeRuleType: values.overtimeRuleType,
        overtimeStepSeconds: seconds(values.overtimeStepMinutes),
        overtimeScorePerStep: values.overtimeScorePerStep ?? null,
        overtimeFixedDeduction: values.overtimeFixedDeduction ?? null,
        overtimeMaxDeduction: values.overtimeMaxDeduction ?? null,
        earlyFinishRuleType: values.earlyFinishRuleType,
        earlyFinishStepSeconds: seconds(values.earlyFinishStepMinutes),
        earlyFinishScorePerStep: values.earlyFinishScorePerStep ?? null,
        earlyFinishFixedBonus: values.earlyFinishFixedBonus ?? null,
        earlyFinishMaxBonus: values.earlyFinishMaxBonus ?? null,
        overtimeAction: values.overtimeAction,
        maxDurationSeconds: seconds(values.maxDurationMinutes),
        finalScoreUpperLimit: values.finalScoreUpperLimit,
    };
}
function scoreInput(min = 0) {
    return _jsx(InputNumber, { min: min, precision: 2, style: { width: 140 } });
}
function MinutesInput({ min = 1, ...inputProps }) {
    return (_jsxs(Space.Compact, { style: { width: 180 }, children: [_jsx(InputNumber, { ...inputProps, min: min, precision: 0, style: { ...inputProps.style, width: 'calc(100% - 60px)' } }), _jsx(Input, { value: "\u5206\u949F", readOnly: true, tabIndex: -1, style: { width: 60, textAlign: 'center' } })] }));
}
function minutesInput(min = 1) {
    return _jsx(MinutesInput, { min: min });
}
function errorMessage(error) {
    return error instanceof Error ? error.message : '操作失败，请刷新后重试';
}
export function CompetitionPracticalExecutionConfigPanel({ eventId, stageId, stageName, eventWorkTypeId, eventWorkTypeLevelId, editable, }) {
    const [form] = Form.useForm();
    const queryClient = useQueryClient();
    const [previewValues, setPreviewValues] = useState(defaults(eventWorkTypeId, eventWorkTypeLevelId));
    const configQuery = useQuery({
        queryKey: ['competition-practical-execution-config', stageId],
        queryFn: () => getCompetitionPracticalExecutionConfig(stageId),
        enabled: Boolean(stageId && eventWorkTypeLevelId),
    });
    const schemesQuery = useQuery({
        queryKey: ['competition-practical-scoring-schemes', eventId, eventWorkTypeId, eventWorkTypeLevelId],
        queryFn: () => listCompetitionPracticalScoringSchemes({
            eventId, eventWorkTypeId, eventWorkTypeLevelId: eventWorkTypeLevelId,
        }),
        enabled: Boolean(eventId && eventWorkTypeId && eventWorkTypeLevelId),
    });
    const schemes = schemesQuery.data ?? [];
    const currentScheme = useMemo(() => schemes.find((item) => String(item.id) === String(previewValues.scoringSchemeId)), [previewValues.scoringSchemeId, schemes]);
    const configuredSchemeUnavailable = Boolean(configQuery.data?.scoringSchemeId
        && !schemes.some(item => String(item.id) === String(configQuery.data?.scoringSchemeId)));
    const schemeOptions = schemes.map(scheme => ({
        value: scheme.id,
        label: `${scheme.title} ${scheme.paperCode}卷 v${scheme.versionNo} · ${scheme.totalScore}分 · 来源赛事${scheme.sourceEventId}`,
        disabled: false,
    }));
    if (configuredSchemeUnavailable && configQuery.data?.scoringScheme) {
        const legacyScheme = configQuery.data.scoringScheme;
        schemeOptions.push({
            value: legacyScheme.id,
            label: `${legacyScheme.title} ${legacyScheme.paperCode}卷 v${legacyScheme.versionNo}（旧配置，需改选）`,
            disabled: true,
        });
    }
    useEffect(() => {
        if (configQuery.isLoading)
            return;
        const initial = configQuery.data ? fromConfig(configQuery.data) : defaults(eventWorkTypeId, eventWorkTypeLevelId);
        form.setFieldsValue(initial);
        setPreviewValues(initial);
    }, [configQuery.data, configQuery.isLoading, eventWorkTypeId, eventWorkTypeLevelId, form]);
    const saveMutation = useMutation({
        mutationFn: (values) => saveCompetitionPracticalExecutionConfig(stageId, toRequest(values)),
        onSuccess: async (config) => {
            message.success('赛段实操运行规则已保存');
            queryClient.setQueryData(['competition-practical-execution-config', stageId], config);
            await queryClient.invalidateQueries({ queryKey: ['competition-practical-execution-config', stageId] });
        },
        onError: (error) => message.error(errorMessage(error)),
    });
    const validateMutation = useMutation({
        mutationFn: (request) => validateCompetitionPracticalExecutionConfig(stageId, request),
        onSuccess: (result) => {
            if (result.valid)
                message.success('配置校验通过');
            else
                message.warning(result.message || result.errors.join('；'));
        },
        onError: (error) => message.error(errorMessage(error)),
    });
    const timeScoringEnabled = Form.useWatch('timeScoringEnabled', form) ?? false;
    const overtimeRuleType = Form.useWatch('overtimeRuleType', form) ?? 'NONE';
    const earlyFinishRuleType = Form.useWatch('earlyFinishRuleType', form) ?? 'NONE';
    const overtimeAction = Form.useWatch('overtimeAction', form) ?? 'ALLOW_CONTINUE';
    const onValuesChange = (changed, values) => {
        if (Object.prototype.hasOwnProperty.call(changed, 'scoringSchemeId')) {
            const scheme = schemes.find((item) => String(item.id) === String(values.scoringSchemeId));
            if (scheme) {
                form.setFieldValue('finalScoreUpperLimit', scheme.totalScore);
                values.finalScoreUpperLimit = scheme.totalScore;
            }
        }
        setPreviewValues({ ...values });
    };
    const onValidate = async () => {
        try {
            const values = await form.validateFields();
            validateMutation.mutate(toRequest(values));
        }
        catch (error) {
            if (error instanceof Error)
                message.error(error.message);
        }
    };
    const refresh = async () => {
        const [configResult] = await Promise.all([configQuery.refetch(), schemesQuery.refetch()]);
        if (configResult.isError)
            return;
        const initial = configResult.data
            ? fromConfig(configResult.data)
            : defaults(eventWorkTypeId, eventWorkTypeLevelId);
        form.resetFields();
        form.setFieldsValue(initial);
        setPreviewValues(initial);
    };
    if (configQuery.isError || schemesQuery.isError) {
        return _jsx(Alert, { type: "error", showIcon: true, message: "\u5B9E\u64CD\u8FD0\u884C\u89C4\u5219\u52A0\u8F7D\u5931\u8D25", description: errorMessage(configQuery.error ?? schemesQuery.error), action: _jsx(Button, { onClick: () => void refresh(), children: "\u91CD\u65B0\u52A0\u8F7D" }) });
    }
    return (_jsxs("div", { "data-testid": "competition-practical-execution-config-panel", children: [configuredSchemeUnavailable && _jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u5F53\u524D\u8D5B\u6BB5\u7ED1\u5B9A\u7684\u662F\u65E7\u5B9E\u64CD\u7EC4\u5377\u5FEB\u7167", description: "\u65E7\u6267\u884C\u5FEB\u7167\u7EE7\u7EED\u6709\u6548\uFF1B\u5982\u9700\u4FEE\u6539\u672C\u8D5B\u6BB5\u8FD0\u884C\u89C4\u5219\uFF0C\u8BF7\u5148\u5728\u5B9E\u64CD\u8BC4\u5206\u65B9\u6848\u4E2D\u53D1\u5E03\u65B0\u65B9\u6848\uFF0C\u518D\u5728\u6B64\u91CD\u65B0\u7ED1\u5B9A\u3002" }), _jsxs(Space, { style: { marginBottom: 12 }, wrap: true, children: [_jsxs(Tag, { color: "orange", children: ["\u8D5B\u6BB5\uFF1A", stageName || String(stageId)] }), _jsx(Tag, { children: "\u5355\u4F4D\uFF1A\u5206\u949F\uFF08\u670D\u52A1\u7AEF\u4FDD\u5B58\u4E3A\u79D2\uFF09" }), _jsx(Button, { "data-testid": "runtime-config-refresh", icon: _jsx(ReloadOutlined, {}), onClick: () => void refresh(), children: "\u5237\u65B0" })] }), _jsxs(Form, { form: form, layout: "vertical", disabled: !editable || configQuery.isFetching || schemesQuery.isFetching, initialValues: defaults(eventWorkTypeId, eventWorkTypeLevelId), onValuesChange: onValuesChange, onFinish: (values) => saveMutation.mutate(values), children: [_jsxs(Row, { gutter: [16, 16], children: [_jsx(Col, { xs: 24, xl: 12, children: _jsxs(Card, { title: "\u5B9E\u64CD\u57FA\u7840\u914D\u7F6E", size: "small", className: "competition-filter-card", children: [_jsx(Form.Item, { name: "examName", label: "\u5B9E\u64CD\u8003\u8BD5\u540D\u79F0", rules: [{ required: true, whitespace: true, message: '请输入实操考试名称' }], children: _jsx(Input, { maxLength: 128 }) }), _jsx(Form.Item, { name: "scoringSchemeId", label: "\u7ED1\u5B9A\u5B9E\u64CD\u8BC4\u5206\u65B9\u6848", rules: [{ required: true, message: '请选择已发布的实操评分方案' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", loading: schemesQuery.isLoading, placeholder: "\u9009\u62E9\u5DF2\u53D1\u5E03\u7684\u5B9E\u64CD\u8BC4\u5206\u65B9\u6848", options: schemeOptions }) }), _jsxs(Form.Item, { label: "\u8BC4\u5206\u65B9\u6848\u6807\u51C6\u603B\u5206", children: [_jsxs(Typography.Text, { strong: true, "data-testid": "practical-standard-total-score", children: [currentScheme?.totalScore ?? configQuery.data?.standardTotalScore ?? '选择评分方案后自动读取', " \u5206"] }), _jsx(Typography.Text, { type: "secondary", style: { marginLeft: 8 }, children: "\u7531\u5DF2\u53D1\u5E03\u8BC4\u5206\u65B9\u6848\u786E\u5B9A" })] }), _jsxs(Row, { gutter: 12, children: [_jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "durationMinutes", label: "\u6807\u51C6\u8003\u6838\u65F6\u957F\uFF08\u5206\u949F\uFF09", rules: [{ required: true, message: '请输入标准考核时长' }], children: minutesInput() }) }), _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "startMode", label: "\u5F00\u59CB\u65B9\u5F0F", rules: [{ required: true }], children: _jsx(Select, { options: [
                                                                { value: 'UNIFIED_START', label: '统一开始' },
                                                                { value: 'MANUAL_START', label: '单独开始 / 裁判开始' },
                                                            ] }) }) })] }), _jsx(Form.Item, { name: "allowEarlySubmit", label: "\u5141\u8BB8\u63D0\u524D\u4EA4\u5377", valuePropName: "checked", children: _jsx(Switch, { checkedChildren: "\u5141\u8BB8", unCheckedChildren: "\u4E0D\u5141\u8BB8" }) })] }) }), _jsx(Col, { xs: 24, xl: 12, children: _jsxs(Card, { title: "\u65F6\u95F4\u4E0E\u6210\u7EE9\u89C4\u5219", size: "small", className: "competition-filter-card", children: [_jsx(Form.Item, { name: "timeScoringEnabled", label: "\u65F6\u95F4\u53C2\u4E0E\u6210\u7EE9", valuePropName: "checked", children: _jsx(Switch, { checkedChildren: "\u53C2\u4E0E", unCheckedChildren: "\u53EA\u8BB0\u5F55" }) }), !timeScoringEnabled && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5B9E\u9645\u7528\u65F6\u53EA\u8BB0\u5F55\uFF0C\u4E0D\u589E\u52A0\u6216\u6263\u51CF\u6210\u7EE9\u3002" })), timeScoringEnabled && (_jsxs(_Fragment, { children: [_jsx(Typography.Text, { strong: true, children: "\u8D85\u65F6\u6263\u5206" }), _jsxs(Row, { gutter: 12, style: { marginTop: 8 }, children: [_jsx(Col, { span: 24, children: _jsx(Form.Item, { name: "overtimeRuleType", label: "\u8D85\u65F6\u89C4\u5219", children: _jsx(Select, { options: [
                                                                        { value: 'NONE', label: '不扣分' },
                                                                        { value: 'STEP_DEDUCTION', label: '按时间步长扣分' },
                                                                        { value: 'FIXED_DEDUCTION', label: '超时固定扣分' },
                                                                    ] }) }) }), overtimeRuleType === 'STEP_DEDUCTION' && _jsxs(_Fragment, { children: [_jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "overtimeStepMinutes", label: "\u8D85\u65F6\u6B65\u957F", rules: [{ required: true, type: 'number', min: 1 }], children: minutesInput() }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "overtimeScorePerStep", label: "\u6BCF\u6B65\u6263\u5206", rules: [{ required: true, type: 'number', min: 0 }], children: scoreInput() }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "overtimeMaxDeduction", label: "\u6700\u5927\u6263\u5206", rules: [{ required: true, type: 'number', min: 0 }], children: scoreInput() }) })] }), overtimeRuleType === 'FIXED_DEDUCTION' && _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "overtimeFixedDeduction", label: "\u8D85\u65F6\u56FA\u5B9A\u6263\u5206", rules: [{ required: true, type: 'number', min: 0 }], children: scoreInput() }) })] }), _jsx(Typography.Text, { strong: true, children: "\u63D0\u524D\u5B8C\u6210\u5956\u52B1" }), _jsxs(Row, { gutter: 12, style: { marginTop: 8 }, children: [_jsx(Col, { span: 24, children: _jsx(Form.Item, { name: "earlyFinishRuleType", label: "\u5956\u52B1\u89C4\u5219", children: _jsx(Select, { options: [
                                                                        { value: 'NONE', label: '不奖励' },
                                                                        { value: 'STEP_BONUS', label: '按完整步长奖励' },
                                                                        { value: 'FIXED_BONUS', label: '提前完成固定奖励' },
                                                                    ] }) }) }), earlyFinishRuleType === 'STEP_BONUS' && _jsxs(_Fragment, { children: [_jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "earlyFinishStepMinutes", label: "\u63D0\u524D\u6B65\u957F", rules: [{ required: true, type: 'number', min: 1 }], children: minutesInput() }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "earlyFinishScorePerStep", label: "\u6BCF\u6B65\u52A0\u5206", rules: [{ required: true, type: 'number', min: 0 }], children: scoreInput() }) }), _jsx(Col, { span: 8, children: _jsx(Form.Item, { name: "earlyFinishMaxBonus", label: "\u6700\u5927\u5956\u52B1", rules: [{ required: true, type: 'number', min: 0 }], children: scoreInput() }) })] }), earlyFinishRuleType === 'FIXED_BONUS' && _jsx(Col, { span: 12, children: _jsx(Form.Item, { name: "earlyFinishFixedBonus", label: "\u63D0\u524D\u5B8C\u6210\u56FA\u5B9A\u5956\u52B1", rules: [{ required: true, type: 'number', min: 0 }], children: scoreInput() }) })] })] })), _jsx(Form.Item, { name: "finalScoreUpperLimit", label: "\u6700\u7EC8\u6210\u7EE9\u4E0A\u9650", rules: [{ required: true, type: 'number', min: 0.01 }], children: scoreInput(0.01) })] }) }), _jsx(Col, { span: 24, children: _jsxs(Card, { title: "\u8D85\u65F6\u5904\u7406", size: "small", className: "competition-filter-card", children: [_jsx(Form.Item, { name: "overtimeAction", children: _jsx(Radio.Group, { options: [
                                                    { value: 'ALLOW_CONTINUE', label: '超时后允许继续考试' },
                                                    { value: 'AUTO_FINISH', label: '达到最大允许时间后自动结束' },
                                                ] }) }), overtimeAction === 'AUTO_FINISH' && (_jsx(Form.Item, { name: "maxDurationMinutes", label: "\u6700\u5927\u5141\u8BB8\u4F5C\u7B54\u65F6\u95F4\uFF08\u5206\u949F\uFF09", rules: [{ required: true, type: 'number', min: 1 }], children: minutesInput() })), _jsx(Alert, { type: "warning", showIcon: true, message: "\u6807\u51C6\u8003\u6838\u65F6\u957F\u7528\u4E8E\u8BA1\u7B97\u65F6\u95F4\u6210\u7EE9\uFF1B\u6700\u5927\u5141\u8BB8\u65F6\u95F4\u53EA\u51B3\u5B9A\u8003\u8BD5\u505C\u6B62\u65F6\u70B9\u3002" })] }) }), _jsx(Col, { span: 24, children: _jsxs(Card, { title: "\u5F53\u524D\u89C4\u5219\u9884\u89C8", size: "small", className: "competition-filter-card", children: [_jsx(Space, { direction: "vertical", size: 4, "data-testid": "practical-rule-preview", children: buildPracticalRulePreview(previewValues, currentScheme).map((line) => (_jsx(Typography.Text, { children: line }, line))) }), _jsxs(Typography.Paragraph, { type: "secondary", style: { marginTop: 12, marginBottom: 0 }, children: [_jsx(SafetyCertificateOutlined, {}), " \u8BA1\u5206\u4F7F\u7528\u670D\u52A1\u7AEF\u8BB0\u5F55\u7684\u5F00\u59CB\u65F6\u95F4\u3001\u7ED3\u675F\u65F6\u95F4\u548C\u6682\u505C\u65F6\u957F\uFF1BPad \u53EA\u663E\u793A\u5012\u8BA1\u65F6\u3002"] })] }) })] }), !eventWorkTypeLevelId && _jsx(Alert, { style: { marginTop: 16 }, type: "warning", showIcon: true, message: "\u5F53\u524D\u5DE5\u79CD\u7B49\u7EA7\u5C1A\u672A\u89E3\u6790\uFF0C\u6682\u65F6\u4E0D\u80FD\u4FDD\u5B58\u8FD0\u884C\u89C4\u5219\u3002" }), _jsxs(Space, { style: { marginTop: 16 }, children: [_jsx(Button, { type: "primary", htmlType: "submit", loading: saveMutation.isPending, disabled: !editable || !eventWorkTypeLevelId || configQuery.isLoading || schemesQuery.isLoading, "data-testid": "practical-runtime-save", children: "\u4FDD\u5B58\u8FD0\u884C\u89C4\u5219" }), _jsx(Button, { onClick: () => void onValidate(), loading: validateMutation.isPending, disabled: !editable || !eventWorkTypeLevelId, "data-testid": "practical-runtime-validate", children: "\u6821\u9A8C\u914D\u7F6E" })] })] })] }));
}
