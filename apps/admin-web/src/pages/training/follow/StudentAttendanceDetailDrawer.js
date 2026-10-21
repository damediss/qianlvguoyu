import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo } from 'react';
import { Alert, Col, Drawer, Empty, Progress, Row, Space, Statistic, Table, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
const ATTENDANCE_STATUS = {
    0: { text: '缺勤', color: 'error' },
    1: { text: '出勤', color: 'success' },
    2: { text: '迟到', color: 'warning' },
    3: { text: '早退', color: 'orange' },
};
function formatClassHours(value) {
    if (value == null || value === '')
        return '—';
    const n = typeof value === 'number' ? value : Number(value);
    if (!Number.isFinite(n))
        return '—';
    return n.toFixed(1);
}
function toNumId(id) {
    const n = typeof id === 'number' ? id : Number(id);
    return Number.isFinite(n) ? n : null;
}
export function StudentAttendanceDetailDrawer({ open, classId, student, hoursRow, onClose }) {
    const userId = student ? toNumId(student.id) : null;
    const { data, isLoading, isError } = useQuery({
        queryKey: ['follow-student-attendance-detail', classId, userId],
        queryFn: () => trainApi.pageAttendances({
            page: 1,
            pageSize: 500,
            classId: classId,
            userId: userId,
        }),
        enabled: open && classId != null && classId > 0 && userId != null,
        retry: false,
    });
    const rows = useMemo(() => {
        const list = [...(data?.items ?? [])];
        list.sort((a, b) => String(b.attendanceDate ?? '').localeCompare(String(a.attendanceDate ?? '')));
        return list;
    }, [data?.items]);
    const summary = useMemo(() => {
        let present = 0;
        let absent = 0;
        let late = 0;
        let early = 0;
        for (const row of rows) {
            const s = row.status;
            if (s === 1)
                present += 1;
            else if (s === 0)
                absent += 1;
            else if (s === 2)
                late += 1;
            else if (s === 3)
                early += 1;
        }
        const recorded = rows.length;
        const effectivePresent = present + late + early;
        const rate = recorded > 0 ? Math.round((effectivePresent / recorded) * 1000) / 10 : 0;
        return { present, absent, late, early, recorded, effectivePresent, rate };
    }, [rows]);
    const columns = [
        {
            title: '日期',
            dataIndex: 'attendanceDate',
            width: 120,
            render: (v) => v || '—',
        },
        {
            title: '课次',
            dataIndex: 'scheduleId',
            width: 100,
            render: (v) => (v != null ? `课次#${v}` : '按日'),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (status) => {
                if (status == null)
                    return _jsx(Tag, { children: "\u672A\u77E5" });
                const meta = ATTENDANCE_STATUS[status] ?? { text: String(status), color: 'default' };
                return _jsx(Tag, { color: meta.color, children: meta.text });
            },
        },
        {
            title: '签到时间',
            dataIndex: 'checkInTime',
            width: 110,
            render: (v) => v || '—',
        },
        {
            title: '签退时间',
            dataIndex: 'checkOutTime',
            width: 110,
            render: (v) => v || '—',
        },
        {
            title: '备注',
            dataIndex: 'remark',
            ellipsis: true,
            render: (v) => v || '—',
        },
    ];
    return (_jsx(Drawer, { title: student ? `${student.realname} · 考勤明细` : '考勤明细', width: 800, open: open, onClose: onClose, destroyOnClose: true, children: student && (_jsxs(Space, { direction: "vertical", size: "middle", style: { width: '100%' }, children: [_jsx(Typography.Text, { type: "secondary", children: [student.phone, student.id_card].filter(Boolean).join(' · ') || '暂无联系方式' }), isError && (_jsx(Alert, { type: "warning", showIcon: true, message: "\u8003\u52E4\u660E\u7EC6\u52A0\u8F7D\u5931\u8D25", description: "\u8BF7\u786E\u8BA4\u8D26\u53F7\u5177\u5907 train:attendance:view \u6743\u9650\u3002" })), _jsxs(Row, { gutter: [12, 12], children: [_jsx(Col, { span: 6, children: _jsx(Statistic, { title: "\u767B\u8BB0\u6761\u6570", value: summary.recorded }) }), _jsx(Col, { span: 6, children: _jsx(Statistic, { title: "\u6709\u6548\u51FA\u52E4", value: summary.effectivePresent, suffix: `/ ${summary.recorded || 0}` }) }), _jsx(Col, { span: 6, children: _jsx(Statistic, { title: "\u5DF2\u4E0A\u8BFE\u65F6", value: formatClassHours(hoursRow?.attendedHours), suffix: hoursRow?.attendedHours != null ? '课时' : undefined }) }), _jsx(Col, { span: 6, children: _jsx(Statistic, { title: "\u8BA1\u5212\u5B66\u65F6", value: formatClassHours(hoursRow?.plannedHours), suffix: hoursRow?.plannedHours != null ? '课时' : undefined }) })] }), hoursRow && hoursRow.completionRate != null ? (_jsxs("div", { children: [_jsxs(Typography.Text, { type: "secondary", children: ["\u8BFE\u65F6\u5B8C\u6210\u7387 ", Number(hoursRow.completionRate).toFixed(1), "%", hoursRow.hoursStatus === 'unrecorded' ? '（无考勤记录，按未旷课计）' : ''] }), _jsx(Progress, { percent: Number(hoursRow.completionRate), size: "small", showInfo: false, style: { marginTop: 4 } })] })) : null, _jsx(Row, { gutter: [12, 12], children: _jsxs(Col, { span: 8, children: [_jsx(Statistic, { title: "\u51FA\u52E4\u7387", value: summary.rate, suffix: "%" }), _jsx(Progress, { percent: summary.rate, size: "small", showInfo: false, style: { marginTop: 4 } })] }) }), _jsxs(Space, { wrap: true, children: [_jsxs(Tag, { color: "success", children: ["\u51FA\u52E4 ", summary.present] }), _jsxs(Tag, { color: "warning", children: ["\u8FDF\u5230 ", summary.late] }), _jsxs(Tag, { color: "orange", children: ["\u65E9\u9000 ", summary.early] }), _jsxs(Tag, { color: "error", children: ["\u7F3A\u52E4 ", summary.absent] })] }), _jsx(Table, { rowKey: (r) => String(r.id), size: "small", loading: isLoading, columns: columns, dataSource: rows, pagination: { pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 条` }, locale: { emptyText: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u5386\u53F2\u8003\u52E4\u8BB0\u5F55" }) }, scroll: { y: 420 } })] })) }));
}
