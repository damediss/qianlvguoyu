export function isFormalExamTerminalStatus(status) {
    return status === 'submitted'
        || status === 'auto_submitted'
        || status === 'forced_submitted';
}
export function isFormalExamClosedStatus(status) {
    return isFormalExamTerminalStatus(status) || status === 'absent';
}
export function shouldStopFormalExamPolling(context) {
    if (!context)
        return false;
    const sessionStatus = context.examSession?.status;
    if (sessionStatus === 'finished' || sessionStatus === 'cancelled')
        return true;
    return isFormalExamClosedStatus(context.candidateExam?.status);
}
function isTestRuntime() {
    try {
        return import.meta.env?.MODE === 'test';
    }
    catch {
        return false;
    }
}
export function formalExamPollIntervalMs(realtimeConnected = false, options) {
    const testRuntime = options?.testRuntime ?? isTestRuntime();
    const random = options?.random ?? Math.random;
    if (testRuntime)
        return 4000;
    // Keep HTTP fallback ~4s even when WS reports connected. A silent socket
    // previously delayed 开考/清退 until the Pad was restarted.
    void realtimeConnected;
    return 4000 + Math.floor(random() * 1500);
}
export function formalExamInitialDelayMs() {
    if (isTestRuntime())
        return 0;
    return Math.floor(Math.random() * 4000);
}
