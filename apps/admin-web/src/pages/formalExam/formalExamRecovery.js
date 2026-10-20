const handlers = new Set();
let attached = false;
function isTestRuntime() {
    try {
        return import.meta.env?.MODE === 'test';
    }
    catch {
        return false;
    }
}
function dispatch(reason) {
    for (const handler of handlers) {
        handler(reason);
    }
}
const onFocus = () => dispatch('focus');
const onVisible = () => {
    if (typeof document !== 'undefined' && document.visibilityState === 'visible') {
        dispatch('visible');
    }
};
const onOnline = () => dispatch('online');
const onPageShow = () => dispatch('pageshow');
function attach() {
    if (attached || typeof window === 'undefined')
        return;
    window.addEventListener('focus', onFocus);
    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('online', onOnline);
    window.addEventListener('pageshow', onPageShow);
    attached = true;
}
function detach() {
    if (!attached || typeof window === 'undefined')
        return;
    window.removeEventListener('focus', onFocus);
    document.removeEventListener('visibilitychange', onVisible);
    window.removeEventListener('online', onOnline);
    window.removeEventListener('pageshow', onPageShow);
    attached = false;
}
/**
 * Single foreground/network recovery coordinator for Pad H5.
 * Multiple screens can subscribe; window listeners are attached once.
 */
export function subscribeFormalExamRecovery(handler) {
    if (typeof window === 'undefined') {
        return () => undefined;
    }
    handlers.add(handler);
    attach();
    return () => {
        handlers.delete(handler);
        if (handlers.size === 0) {
            detach();
        }
    };
}
export function formalExamRecoveryJitterMs(maxMs = 5000) {
    if (isTestRuntime())
        return 0;
    return Math.floor(Math.random() * Math.max(0, maxMs));
}
