import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 工作台发起区：工种 → 等级 → 细目表批次 三级联动 + 配额 + 提交生成。
 * 批次候选走服务端按 workTypeId+level 过滤（不再全量拉取，细目表超过 200 条也不缺候选）。
 */
import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, InputNumber, message, Select, Space } from 'antd';
import { listBlueprints, listTheoryModelConfigs, submitGenBatch } from '../theoryGenApi';
import { LEVEL_NAME, QUOTA_DEFAULTS, bpOptionLabel } from './constants';
const LEVEL_OPTIONS = [5, 4, 3, 2, 1].map((v) => ({ value: v, label: `${v}级/${LEVEL_NAME[v]}` }));
const GenStartBar = ({ workTypeOptions, preset, onSubmitted }) => {
    const qc = useQueryClient();
    const [workTypeId, setWorkTypeId] = useState();
    const [level, setLevel] = useState(5);
    const [blueprintId, setBlueprintId] = useState();
    const [modelConfigId, setModelConfigId] = useState();
    const [quotaSingle, setQuotaSingle] = useState(null);
    const [quotaJudge, setQuotaJudge] = useState(null);
    const [quotaMulti, setQuotaMulti] = useState(null);
    const [quotaSub5, setQuotaSub5] = useState(null);
    const [quotaSub10, setQuotaSub10] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const bpQuery = useQuery({
        queryKey: ['theorygen-blueprints', 'pick', workTypeId, level],
        queryFn: () => listBlueprints({ workTypeId, level, page: 1, pageSize: 50 }),
        enabled: !!workTypeId && !!level,
    });
    const mcQuery = useQuery({
        queryKey: ['theorygen-model-configs'],
        queryFn: listTheoryModelConfigs,
    });
    // 模型默认选中 theory-gen-glm（未配置时取第一个启用项）
    React.useEffect(() => {
        if (!modelConfigId && mcQuery.data?.length) {
            const preferred = mcQuery.data.find((c) => c.modelCode === 'theory-gen-glm');
            setModelConfigId((preferred ?? mcQuery.data[0]).id);
        }
    }, [mcQuery.data, modelConfigId]);
    // 页签间跳转带入批次：定位到该批次的工种/等级并选中
    React.useEffect(() => {
        if (!preset) {
            return;
        }
        setWorkTypeId(preset.workTypeId);
        setLevel(preset.level);
        setBlueprintId(preset.id);
    }, [preset]);
    const selectedBp = bpQuery.data?.items.find((b) => b.id === blueprintId);
    const multiDisabled = selectedBp ? selectedBp.level === 5 || selectedBp.level === 4 : false;
    // 主观题配额仅二级/一级（指引：二级/一级客观+主观结合，主观分值占比 40%~60%）
    const subjectiveEnabled = selectedBp ? selectedBp.level <= 2 : false;
    // 选中批次变化 → 按等级填入推荐题量
    React.useEffect(() => {
        const d = selectedBp ? QUOTA_DEFAULTS[selectedBp.level] : undefined;
        setQuotaSingle(d?.single ?? null);
        setQuotaJudge(d?.judge ?? null);
        setQuotaMulti(d?.multi ?? null);
        setQuotaSub5(d?.sub5 ?? null);
        setQuotaSub10(d?.sub10 ?? null);
    }, [selectedBp]);
    const submit = async () => {
        if (!blueprintId || !modelConfigId) {
            message.warning('请选择细目表批次与模型');
            return;
        }
        const quota = {};
        if (quotaSingle != null)
            quota.single = quotaSingle;
        if (quotaJudge != null)
            quota.judge = quotaJudge;
        if (quotaMulti != null)
            quota.multi = quotaMulti;
        if (subjectiveEnabled) {
            if (quotaSub5 != null)
                quota.sub5 = quotaSub5;
            if (quotaSub10 != null)
                quota.sub10 = quotaSub10;
        }
        setSubmitting(true);
        try {
            const b = await submitGenBatch(blueprintId, modelConfigId, Object.keys(quota).length ? quota : undefined);
            message.success('批次已提交');
            onSubmitted(b.id);
            qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
            qc.invalidateQueries({ queryKey: ['theorygen-blueprints'] });
        }
        catch {
            // 错误提示由拦截器统一处理
        }
        finally {
            setSubmitting(false);
        }
    };
    return (_jsxs("div", { className: "gen-start-bar", children: [_jsxs(Space, { style: { marginBottom: 8 }, wrap: true, children: [_jsx(Select, { style: { width: 220 }, placeholder: "1. \u9009\u62E9\u5DE5\u79CD\uFF08\u53EF\u641C\u7D22\uFF09", showSearch: true, optionFilterProp: "label", options: workTypeOptions, value: workTypeId, onChange: (v) => { setWorkTypeId(v); setBlueprintId(undefined); } }), _jsx(Select, { style: { width: 160 }, placeholder: "2. \u9009\u62E9\u7B49\u7EA7", options: LEVEL_OPTIONS, value: level, onChange: (v) => { setLevel(v); setBlueprintId(undefined); } }), _jsx(Select, { style: { width: 360 }, placeholder: "3. \u9009\u62E9\u7EC6\u76EE\u8868\u6279\u6B21", showSearch: true, optionFilterProp: "label", loading: bpQuery.isFetching, disabled: !workTypeId, value: blueprintId, onChange: (v) => setBlueprintId(v), options: (bpQuery.data?.items ?? []).map((b) => ({
                            value: b.id, label: bpOptionLabel(b)
                        })) }), _jsx(Select, { style: { width: 300 }, placeholder: "\u9009\u62E9\u6A21\u578B\u914D\u7F6E", value: modelConfigId, onChange: (v) => setModelConfigId(v), loading: mcQuery.isLoading, options: (mcQuery.data ?? []).map((c) => ({
                            value: c.id,
                            label: `${c.modelName}（${c.modelId}${c.thinking === 'enabled' ? ' · 思考开' : ''}）`,
                        })) })] }), _jsxs(Space, { style: { marginBottom: 8 }, wrap: true, children: [_jsx(InputNumber, { min: 0, addonBefore: "\u5355\u9009", addonAfter: "\u9053/\u70B9", style: { width: 170 }, value: quotaSingle ?? undefined, onChange: (v) => setQuotaSingle(v) }), _jsx(InputNumber, { min: 0, addonBefore: "\u5224\u65AD", addonAfter: "\u9053/\u70B9", style: { width: 170 }, value: quotaJudge ?? undefined, onChange: (v) => setQuotaJudge(v) }), _jsx(InputNumber, { min: 0, addonBefore: "\u591A\u9009", addonAfter: "\u9053/\u70B9", style: { width: 170 }, disabled: multiDisabled, value: quotaMulti ?? undefined, onChange: (v) => setQuotaMulti(v) }), _jsx(InputNumber, { min: 1, addonBefore: "\u4E3B\u89C25\u5206\u5236", addonAfter: "\u70B9/\u9053", style: { width: 200 }, disabled: !subjectiveEnabled, value: quotaSub5 ?? undefined, onChange: (v) => setQuotaSub5(v) }), _jsx(InputNumber, { min: 1, addonBefore: "\u4E3B\u89C210\u5206\u5236", addonAfter: "\u70B9/\u9053", style: { width: 200 }, disabled: !subjectiveEnabled, value: quotaSub10 ?? undefined, onChange: (v) => setQuotaSub10(v) }), _jsx(Button, { type: "primary", loading: submitting, onClick: submit, children: "\u63D0\u4EA4\u751F\u6210" })] }), _jsx("div", { style: { marginBottom: 8, color: '#888', fontSize: 12 }, children: "\u300C\u9053/\u70B9\u300D\u662F\u6BCF\u4E2A\u8003\u8BC4\u70B9\u8981\u751F\u6210\u7684\u9898\u76EE\u6570\u91CF\uFF0C\u5DF2\u6309\u901A\u7528\u89C4\u8303\u81EA\u52A8\u586B\u5165\u63A8\u8350\u503C\uFF1A\u4E94/\u56DB\u7EA7 \u5355\u90094+\u5224\u65AD2\uFF1B \u4E09~\u4E00\u7EA7 \u5355\u90093+\u591A\u90092+\u5224\u65AD2\u3002\u53EF\u8C03\u5927\uFF0C\u4E0D\u5EFA\u8BAE\u8C03\u4F4E\u3002\u300C\u4E3B\u89C2\u9898 \u70B9/\u9053\u300D\u4EC5\u4E8C\u7EA7\u3001\u4E00\u7EA7\u9700\u8981\uFF1A \u4E3B\u89C2\u9898\u6309\"\u6BCF N \u4E2A\u8003\u8BC4\u70B9 1 \u9053\"\u8F6E\u8F6C\u751F\u6210\uFF0C\u9ED8\u8BA4\u6BCF 3 \u70B9 1 \u9053 5 \u5206\u5236\u9898 + \u6BCF 3 \u70B9 1 \u9053 10 \u5206\u5236\u9898 \uFF08120 \u70B9\u65F6\u5404 40 \u9053\uFF0C\u5168\u5E93\u4E0D\u5C11\u4E8E 920 \u9898\uFF09\u3002\u95F4\u9694\u53EF\u8C03\u5C0F\uFF08\u9898\u66F4\u591A\uFF09\uFF0C\u8C03\u5927\u4F1A\u4F4E\u4E8E\u9898\u91CF\u4E0B\u9650\u800C\u88AB\u62D2\u7EDD\u3002" })] }));
};
export default GenStartBar;
