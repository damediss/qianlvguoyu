import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { Alert, DatePicker, Divider, Empty, Form, Input, Spin, Typography } from 'antd';
import dayjs from 'dayjs';
import { isBusinessStageNode, } from '../../../types/competitionStagePlanning';
import { StageContentPanel } from './StageContentPanel';
import { StagePromotionRulePanel } from './StagePromotionRulePanel';
import './stagePlanning.css';
export function isDateOccupiedByOtherStage(current, currentNodeId, nodes) {
    if (!current.isValid())
        return false;
    const currentDate = current.startOf('day');
    return nodes.some((candidate) => {
        if (candidate.id === currentNodeId || !isBusinessStageNode(candidate))
            return false;
        const dates = [candidate.data.startAt, candidate.data.endAt]
            .filter((value) => Boolean(value))
            .map((value) => dayjs(value));
        if (dates.length === 0 || dates.some((value) => !value.isValid()))
            return false;
        const start = dates.reduce((earliest, value) => value.isBefore(earliest) ? value : earliest);
        const end = dates.reduce((latest, value) => value.isAfter(latest) ? value : latest);
        return !currentDate.isBefore(start.startOf('day')) && !currentDate.isAfter(end.endOf('day'));
    });
}
function validDateTime(value) {
    if (!value)
        return undefined;
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed : undefined;
}
function laterDateTime(first, second) {
    if (!first)
        return second;
    if (!second)
        return first;
    return first.isAfter(second) ? first : second;
}
function earlierDateTime(first, second) {
    if (!first)
        return second;
    if (!second)
        return first;
    return first.isBefore(second) ? first : second;
}
function disabledTimeForRange(current, lowerBound, upperBound) {
    const lowerOnCurrentDay = lowerBound?.isSame(current, 'day') ? lowerBound : undefined;
    const upperOnCurrentDay = upperBound?.isSame(current, 'day') ? upperBound : undefined;
    const hours = Array.from({ length: 24 }, (_, hour) => hour);
    const minutes = Array.from({ length: 60 }, (_, minute) => minute);
    const seconds = Array.from({ length: 60 }, (_, second) => second);
    return {
        disabledHours: () => hours.filter((hour) => ((lowerOnCurrentDay && hour < lowerOnCurrentDay.hour())
            || (upperOnCurrentDay && hour > upperOnCurrentDay.hour()))),
        disabledMinutes: (selectedHour) => minutes.filter((minute) => ((lowerOnCurrentDay
            && selectedHour === lowerOnCurrentDay.hour()
            && minute < lowerOnCurrentDay.minute())
            || (upperOnCurrentDay
                && selectedHour === upperOnCurrentDay.hour()
                && minute > upperOnCurrentDay.minute()))),
        disabledSeconds: (selectedHour, selectedMinute) => seconds.filter((second) => ((lowerOnCurrentDay
            && selectedHour === lowerOnCurrentDay.hour()
            && selectedMinute === lowerOnCurrentDay.minute()
            && second < lowerOnCurrentDay.second())
            || (upperOnCurrentDay
                && selectedHour === upperOnCurrentDay.hour()
                && selectedMinute === upperOnCurrentDay.minute()
                && second > upperOnCurrentDay.second()))),
    };
}
export function StageInspector({ node, edge, nodes, eventStartAt, eventEndAt, stagePlanConfirmed = false, editable, contentVisible = true, contentEditable = editable, contentLoading = false, contents = [], promotionVisible = false, promotionEditable = editable, eventWorkTypeId, promotionConfigs = [], promotionLoading = false, onPromotionPercentageChange, onNodeChange, onEdgeChange, onContentChange, }) {
    if (node && isBusinessStageNode(node)) {
        const startValue = validDateTime(node.data.startAt);
        const endValue = validDateTime(node.data.endAt);
        const eventStartValue = validDateTime(eventStartAt);
        const eventEndValue = validDateTime(eventEndAt);
        const rangeValue = startValue || endValue ? [startValue || null, endValue || null] : null;
        const contentValues = node.data.contents ?? contents;
        const stageId = node.data.stageId ?? node.id;
        const stageContentEditable = contentEditable
            && (node.data.status === 'draft' || node.data.status === 'configuring');
        const isDateDisabled = (current, from) => {
            if (isDateOccupiedByOtherStage(current, node.id, nodes))
                return true;
            if (eventStartValue && current.isBefore(eventStartValue, 'day'))
                return true;
            if (eventEndValue && current.isAfter(eventEndValue, 'day'))
                return true;
            if (from && current.isBefore(from, 'day'))
                return true;
            if (!from && endValue?.isValid() && current.isAfter(endValue, 'day'))
                return true;
            return false;
        };
        return (_jsxs("aside", { className: "stage-planning-inspector", "data-testid": "stage-inspector", children: [_jsx(Typography.Title, { level: 4, className: "stage-planning-inspector__stage-title", children: node.data.name }), _jsx(Form, { layout: "vertical", children: _jsx(Form.Item, { label: "\u65F6\u95F4\u8303\u56F4", children: _jsx(DatePicker.RangePicker, { showTime: true, value: rangeValue, disabled: !editable, placeholder: ['开始时间', '结束时间'], format: "YYYY-MM-DD HH:mm:ss", disabledDate: (current, info) => isDateDisabled(current, info.from), disabledTime: (current, range, info) => {
                                const lowerBound = laterDateTime(eventStartValue, range === 'end' ? (info.from || startValue) : undefined);
                                const upperBound = earlierDateTime(eventEndValue, range === 'start' ? endValue : undefined);
                                return disabledTimeForRange(current, lowerBound, upperBound);
                            }, style: { width: '100%' }, onChange: (values) => onNodeChange(node.id, {
                                startAt: values?.[0]?.format('YYYY-MM-DDTHH:mm:ss') || null,
                                endAt: values?.[1]?.format('YYYY-MM-DDTHH:mm:ss') || null,
                            }), "data-testid": "stage-inspector-time-range" }) }) }), promotionVisible && (_jsx(StagePromotionRulePanel, { stage: { id: stageId, name: node.data.name, stageType: node.data.stageType }, config: promotionConfigs.find((item) => item.stageType === node.data.stageType), editable: promotionEditable, workTypeSelected: Boolean(eventWorkTypeId), loading: promotionLoading, onPercentageChange: (percentage) => onPromotionPercentageChange?.(node.data.stageType, stageId, node.data.name, percentage) })), contentVisible && (_jsxs(_Fragment, { children: [_jsx(Divider, { className: "stage-planning-inspector__divider" }), contentLoading && _jsx(Spin, { size: "small", className: "stage-planning-inspector__content-loading" }), _jsx(StageContentPanel, { stage: { id: stageId, name: node.data.name }, contents: contentValues, contentEditable: stageContentEditable, contentSaving: false, compact: true, showActions: false, onChange: (nextContents) => onContentChange?.(node.id, nextContents) }), node.data.contents !== undefined && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5185\u5BB9\u4E0E\u6743\u91CD\u5DF2\u52A0\u5165\u5F53\u524D\u8D5B\u6BB5\u7F16\u6392\u8349\u7A3F", description: "\u70B9\u51FB\u9875\u9762\u9876\u90E8\u201C\u4FDD\u5B58\u8D5B\u6BB5\u7F16\u6392\u201D\u540E\u7EDF\u4E00\u751F\u6548\u3002" }))] })), _jsx(Form, { layout: "vertical", children: _jsx(Form.Item, { label: "\u8D5B\u6BB5\u8BF4\u660E", children: _jsx(Input.TextArea, { value: node.data.description || '', disabled: !editable, rows: 3, maxLength: 2000, showCount: true, onChange: (event) => onNodeChange(node.id, { description: event.target.value }), "data-testid": "stage-inspector-description" }) }) })] }));
    }
    if (edge?.data?.business) {
        return (_jsxs("aside", { className: "stage-planning-inspector", "data-testid": "stage-inspector", children: [_jsx(Typography.Title, { level: 5, children: "\u6D41\u8F6C\u8BF4\u660E" }), _jsx(Typography.Paragraph, { type: "secondary", children: "\u8BE5\u8BF4\u660E\u53EA\u4FDD\u5B58\u5728\u5F53\u524D\u6D4F\u89C8\u5668\uFF0C\u4E0D\u4F1A\u8DE8\u6D4F\u89C8\u5668\u6216\u8BBE\u5907\u5171\u4EAB\u3002" }), _jsx(Input.TextArea, { rows: 6, maxLength: 500, showCount: true, value: edge.data.note || '', disabled: !editable, onChange: (event) => onEdgeChange(edge.id, { note: event.target.value }), "data-testid": "stage-edge-note-editor" })] }));
    }
    return (_jsx("aside", { className: "stage-planning-inspector stage-planning-inspector--empty", "data-testid": "stage-inspector-empty", children: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u9009\u62E9\u8282\u70B9\u6216\u8FDE\u7EBF\u67E5\u770B\u5C5E\u6027" }) }));
}
