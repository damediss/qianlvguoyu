export function parseServerDateTime(value) {
    if (!value)
        return null;
    const normalized = value.includes('T') ? value : value.replace(' ', 'T');
    const ms = Date.parse(normalized);
    return Number.isNaN(ms) ? null : ms;
}
export function formatRemaining(ms) {
    const total = Math.max(0, Math.floor(ms / 1000));
    const hours = String(Math.floor(total / 3600)).padStart(2, '0');
    const minutes = String(Math.floor((total % 3600) / 60)).padStart(2, '0');
    const seconds = String(total % 60).padStart(2, '0');
    return `${hours}:${minutes}:${seconds}`;
}
export function remainingMs(serverTime, deadlineTime, clientNow = Date.now()) {
    const serverMs = parseServerDateTime(serverTime);
    const deadlineMs = parseServerDateTime(deadlineTime);
    if (deadlineMs == null)
        return 0;
    const offset = serverMs == null ? 0 : serverMs - clientNow;
    return Math.max(0, deadlineMs - (clientNow + offset));
}
export function formatSubmitClock(value) {
    if (!value)
        return '-';
    const match = String(value).match(/(\d{2}):(\d{2}):(\d{2})/);
    if (match)
        return `${match[1]}:${match[2]}:${match[3]}`;
    const ms = parseServerDateTime(value);
    if (ms == null)
        return String(value);
    const date = new Date(ms);
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${hours}:${minutes}:${seconds}`;
}
