import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Alert, Card, InputNumber, Typography } from 'antd';
export function StagePromotionRulePanel({ stage, config, editable, workTypeSelected, loading = false, onPercentageChange, }) {
    if (!workTypeSelected) {
        return (_jsx(Card, { size: "small", title: "\u664B\u7EA7\u89C4\u5219", "data-testid": "stage-promotion-rule-panel", children: _jsx(Alert, { type: "info", showIcon: true, message: "\u8BF7\u5148\u5728\u9875\u9762\u4E0A\u65B9\u9009\u62E9\u8D5B\u4E8B\u5DE5\u79CD" }) }));
    }
    if (stage.stageType === 'final') {
        return (_jsx(Card, { size: "small", title: "\u664B\u7EA7\u89C4\u5219", "data-testid": "stage-promotion-rule-panel", children: _jsx(Alert, { type: "info", showIcon: true, message: "\u51B3\u8D5B\u4E0D\u914D\u7F6E\u664B\u7EA7\u767E\u5206\u6BD4", description: "\u51B3\u8D5B\u7ED3\u7B97\u540E\uFF0C\u7531\u8D5B\u4E8B\u7BA1\u7406\u5458\u786E\u8BA4\u552F\u4E00\u7B2C1\u30012\u30013\u540D\uFF1B\u6709\u6548\u9009\u624B\u4E0D\u8DB33\u4EBA\u65F6\u9700\u586B\u5199\u7279\u6B8A\u8BF4\u660E\u3002" }) }));
    }
    const existingPercentage = config?.promotionRuleType === 'PERCENTAGE'
        ? config.promotionPercentage ?? undefined
        : undefined;
    const isLegacyRule = config?.promotionRuleType && config.promotionRuleType !== 'PERCENTAGE';
    return (_jsxs(Card, { size: "small", title: "\u664B\u7EA7\u89C4\u5219", loading: loading, "data-testid": "stage-promotion-rule-panel", children: [_jsxs(Typography.Text, { type: "secondary", children: [stage.name, "\u6309\u767E\u5206\u6BD4\u664B\u7EA7"] }), _jsx(InputNumber, { "aria-label": `${stage.name}晋级百分比`, min: 1, max: 100, precision: 0, addonAfter: "%", value: existingPercentage, disabled: !editable || loading, onChange: (value) => onPercentageChange?.(value ?? null), style: { width: '100%', marginTop: 8 } }), _jsx(Typography.Paragraph, { type: "secondary", style: { margin: '8px 0 0' }, children: "\u9884\u8BA1\u664B\u7EA7\u4EBA\u6570\u5C06\u5728\u53C2\u8003\u540D\u5355\u751F\u6210\u65F6\uFF0C\u6309\u6709\u6548\u9009\u624B\u57FA\u6570\u5411\u4E0A\u53D6\u6574\uFF1B\u786E\u8BA4\u540E\u9501\u5B9A\u57FA\u6570\u548C\u76EE\u6807\u4EBA\u6570\u3002" }), isLegacyRule && (_jsx(Alert, { type: "warning", showIcon: true, message: "\u5F53\u524D\u4E3A\u5386\u53F2\u56FA\u5B9A\u4EBA\u6570\u89C4\u5219", description: "\u586B\u5199\u767E\u5206\u6BD4\u5E76\u4FDD\u5B58\u8349\u7A3F\u540E\uFF0C\u5C06\u4EE5\u65B0\u7684\u767E\u5206\u6BD4\u89C4\u5219\u4FDD\u5B58\uFF1B\u5386\u53F2\u5DF2\u5F00\u59CB\u6216\u5DF2\u6709\u6210\u7EE9\u7684\u8D5B\u4E8B\u4E0D\u4F1A\u81EA\u52A8\u5207\u6362\u3002" }))] }));
}
