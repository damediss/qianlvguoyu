import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DeleteOutlined, EyeOutlined, PlusOutlined, SaveOutlined, StopOutlined } from '@ant-design/icons';
import { Button, DatePicker, Input, InputNumber, Modal, Select, Space, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { certificationTypeOptions, createEmptyExamInfoItem, createEmptyScheduleItem, defaultScheduleTemplate, formatPreparationDays, levelOptions, scheduleStatusOptions, updateExamItem, validateSchedulePlan, } from './constants';
const containerStyle = {
    maxWidth: 1200,
    margin: '0 auto',
    borderRadius: 12,
    overflow: 'hidden',
    background: '#fff',
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    border: '1px solid #eef2f7',
};
const headerStyle = {
    padding: 32,
    color: '#fff',
    background: 'linear-gradient(135deg, #fa8c16 0%, #d46b08 100%)',
};
const toolbarStyle = {
    padding: '20px 32px',
    background: '#f8fafc',
    borderBottom: '1px solid #e5e7eb',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    flexWrap: 'wrap',
};
const bodyStyle = {
    padding: 32,
};
const gridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
    gap: 16,
};
const sectionTitleStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 20,
    paddingBottom: 12,
    borderBottom: '2px solid #e5e7eb',
};
const sectionNumberStyle = {
    width: 32,
    height: 32,
    borderRadius: 999,
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#fff',
    background: '#1890ff',
    fontWeight: 700,
};
const timelineItemStyle = {
    display: 'flex',
    gap: 16,
    alignItems: 'flex-start',
    padding: 20,
    background: '#f8fafc',
    borderRadius: 10,
    border: '1px solid #e5e7eb',
    marginBottom: 16,
};
const examRowStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, minmax(0, 1fr)) 40px',
    gap: 16,
    alignItems: 'end',
    padding: 20,
    background: '#f8fafc',
    borderRadius: 10,
    border: '1px solid #e5e7eb',
    marginBottom: 16,
};
function requiredLabel(text) {
    return (_jsxs("span", { children: [text, _jsx("span", { style: { color: '#ff4d4f', marginLeft: 4 }, children: "*" })] }));
}
function updateItem(items, index, patch) {
    return items.map((row, i) => (i === index ? { ...row, ...patch } : row));
}
function toDateValue(value) {
    return value ? dayjs(value) : null;
}
export function ScheduleEditor({ task, taskFields, examInfos, items, members, saving, onTaskFieldsChange, onExamInfosChange, onItemsChange, onSave, onPreview, onCancel, }) {
    const memberOptions = members
        .filter((member) => member.id)
        .map((member) => ({ label: member.memberName, value: member.id }));
    const patchTask = (patch) => onTaskFieldsChange({ ...taskFields, ...patch });
    const handleSave = () => {
        const error = validateSchedulePlan(taskFields, examInfos, items);
        if (error) {
            message.warning(error);
            return;
        }
        onSave();
    };
    const handleDeleteSchedule = (index) => {
        Modal.confirm({
            title: '确定要删除该安排吗？',
            okText: '删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => onItemsChange(items.filter((_, i) => i !== index)),
        });
    };
    const handleDeleteExam = (index) => {
        Modal.confirm({
            title: '确定要删除该考试信息吗？',
            okText: '删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: () => onExamInfosChange(examInfos.filter((_, i) => i !== index)),
        });
    };
    return (_jsxs("div", { style: containerStyle, children: [_jsxs("div", { style: headerStyle, children: [_jsx(Typography.Title, { level: 2, style: { margin: 0, color: '#fff' }, children: "\u7F16\u8F91\u8FDB\u5EA6\u8BA1\u5212" }), _jsx(Typography.Paragraph, { style: { margin: '8px 0 0', color: 'rgba(255,255,255,0.9)' }, children: "\u8BF7\u6309\u300C\u57FA\u672C\u4FE1\u606F \u2192 \u8003\u8BD5\u4FE1\u606F \u2192 \u51C6\u5907\u671F\u5173\u952E\u5B89\u6392\u300D\u586B\u5199\u8FDB\u5EA6\u8BA1\u5212" })] }), _jsxs("div", { style: toolbarStyle, children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { type: "primary", icon: _jsx(SaveOutlined, {}), loading: saving, onClick: handleSave, children: "\u4FDD\u5B58\u4FEE\u6539" }), _jsx(Button, { icon: _jsx(EyeOutlined, {}), onClick: onPreview, children: "\u9884\u89C8" }), _jsx(Button, { icon: _jsx(StopOutlined, {}), onClick: () => {
                                    Modal.confirm({
                                        title: '确定要取消编辑吗？',
                                        content: '未保存的修改将丢失。',
                                        okText: '确定取消',
                                        cancelText: '继续编辑',
                                        onOk: onCancel,
                                    });
                                }, children: "\u53D6\u6D88" })] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u51C6\u5907\u65F6\u95F4\uFF1A", formatPreparationDays(taskFields.examDate, taskFields.planStartDate)] })] }), _jsxs("div", { style: bodyStyle, children: [_jsxs("div", { style: { marginBottom: 32 }, children: [_jsxs("div", { style: sectionTitleStyle, children: [_jsx("span", { style: sectionNumberStyle, children: "1" }), _jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: "\u57FA\u672C\u4FE1\u606F" })] }), _jsxs("div", { style: gridStyle, children: [_jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('考试日期') }), _jsx(DatePicker, { style: { width: '100%' }, value: toDateValue(taskFields.examDate), onChange: (value) => patchTask({ examDate: value ? value.format('YYYY-MM-DD') : undefined }) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('启动日期') }), _jsx(DatePicker, { style: { width: '100%' }, value: toDateValue(taskFields.planStartDate), onChange: (value) => patchTask({ planStartDate: value ? value.format('YYYY-MM-DD') : undefined }) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('准备时间') }), _jsx(Input, { readOnly: true, value: formatPreparationDays(taskFields.examDate, taskFields.planStartDate) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('认定状态') }), _jsx(Select, { style: { width: '100%' }, value: taskFields.status, options: scheduleStatusOptions, onChange: (value) => patchTask({ status: value }) })] }), _jsxs("label", { style: { gridColumn: 'span 2' }, children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('对外联系人') }), _jsx(Select, { style: { width: '100%' }, value: taskFields.externalContactMemberId, options: memberOptions, placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u6210\u5458", onChange: (value) => patchTask({ externalContactMemberId: value }) })] }), _jsxs("label", { style: { gridColumn: 'span 2' }, children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('认定负责人') }), _jsx(Select, { style: { width: '100%' }, value: taskFields.certificationOwnerMemberId, options: memberOptions, placeholder: "\u8BF7\u9009\u62E9\u9879\u76EE\u6210\u5458", onChange: (value) => patchTask({ certificationOwnerMemberId: value }) })] })] })] }), _jsxs("div", { style: { marginBottom: 32 }, children: [_jsxs("div", { style: sectionTitleStyle, children: [_jsx("span", { style: sectionNumberStyle, children: "2" }), _jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: "\u8003\u8BD5\u4FE1\u606F" }), _jsx(Button, { size: "small", icon: _jsx(PlusOutlined, {}), style: { marginLeft: 'auto' }, onClick: () => onExamInfosChange([...examInfos, createEmptyExamInfoItem(task)]), children: "\u65B0\u589E\u8003\u8BD5\u4FE1\u606F" })] }), examInfos.length === 0 ? (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u8003\u8BD5\u4FE1\u606F\uFF0C\u8BF7\u70B9\u51FB\u300C\u65B0\u589E\u8003\u8BD5\u4FE1\u606F\u300D\u6DFB\u52A0" })) : (examInfos.map((row, index) => (_jsxs("div", { style: examRowStyle, children: [_jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('认定类型') }), _jsx(Select, { style: { width: '100%' }, value: row.certificationType, options: certificationTypeOptions, onChange: (value) => onExamInfosChange(updateExamItem(examInfos, index, { certificationType: value })) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('工种') }), _jsx(Input, { value: row.workTypeName, placeholder: "\u8BF7\u8F93\u5165\u5DE5\u79CD", onChange: (e) => onExamInfosChange(updateExamItem(examInfos, index, { workTypeName: e.target.value })) })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('等级') }), _jsx(Select, { style: { width: '100%' }, value: row.levelCode, options: levelOptions, placeholder: "\u8BF7\u9009\u62E9\u7B49\u7EA7", onChange: (value, option) => {
                                                    const label = Array.isArray(option) ? undefined : option?.label;
                                                    onExamInfosChange(updateExamItem(examInfos, index, {
                                                        levelCode: value,
                                                        levelName: typeof label === 'string' ? label : row.levelName,
                                                    }));
                                                } })] }), _jsxs("label", { children: [_jsx("div", { style: { marginBottom: 6 }, children: requiredLabel('认定人数') }), _jsx(InputNumber, { min: 0, style: { width: '100%' }, value: row.plannedPeopleCount, onChange: (value) => onExamInfosChange(updateExamItem(examInfos, index, { plannedPeopleCount: value ?? undefined })) })] }), _jsx(Button, { danger: true, type: "text", icon: _jsx(DeleteOutlined, {}), disabled: examInfos.length <= 1, onClick: () => handleDeleteExam(index) })] }, row.id || `exam-${index}`))))] }), _jsxs("div", { children: [_jsxs("div", { style: sectionTitleStyle, children: [_jsx("span", { style: sectionNumberStyle, children: "3" }), _jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: "\u51C6\u5907\u671F\u5173\u952E\u5B89\u6392" }), _jsxs(Space, { style: { marginLeft: 'auto' }, children: [items.length === 0 ? (_jsx(Button, { size: "small", onClick: () => onItemsChange(defaultScheduleTemplate()), children: "\u8F7D\u5165\u9ED8\u8BA4\u6A21\u677F" })) : null, _jsx(Button, { size: "small", icon: _jsx(PlusOutlined, {}), onClick: () => onItemsChange([...items, createEmptyScheduleItem(items.length)]), children: "\u65B0\u589E\u5B89\u6392" })] })] }), items.map((item, index) => (_jsxs("div", { style: timelineItemStyle, children: [_jsx(Input, { style: { width: 120 }, type: "number", min: 1, value: item.dayNo, placeholder: "\u5929\u6570", onChange: (e) => onItemsChange(updateItem(items, index, { dayNo: Number(e.target.value) || undefined })) }), _jsx(Input, { style: { flex: 1 }, value: item.itemName, placeholder: "\u8BF7\u8F93\u5165\u5B89\u6392\u5185\u5BB9", onChange: (e) => onItemsChange(updateItem(items, index, { itemName: e.target.value })) }), _jsx(Button, { danger: true, type: "text", icon: _jsx(DeleteOutlined, {}), onClick: () => handleDeleteSchedule(index) })] }, item.id || `schedule-edit-${index}`)))] })] })] }));
}
