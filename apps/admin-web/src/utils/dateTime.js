function pad2(value) {
    return String(value).padStart(2, '0');
}
function formatDate(date) {
    return [
        date.getFullYear(),
        pad2(date.getMonth() + 1),
        pad2(date.getDate()),
    ].join('-') + ' ' + [
        pad2(date.getHours()),
        pad2(date.getMinutes()),
        pad2(date.getSeconds()),
    ].join(':');
}
export function formatBackendDateTime(value = new Date()) {
    if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value)) {
        return value;
    }
    const date = value instanceof Date ? value : new Date(value);
    if (!Number.isNaN(date.getTime())) {
        return formatDate(date);
    }
    return formatDate(new Date());
}
