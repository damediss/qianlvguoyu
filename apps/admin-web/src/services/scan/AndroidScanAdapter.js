import { parseScanQRCodeResult } from './parseScanQRCodeResult';
const pendingRequests = new Map();
let callbacksInstalled = false;
let previousSuccessCallback;
let previousErrorCallback;
let scanning = false;
function nativeWindow() {
    if (typeof window === 'undefined') {
        return undefined;
    }
    return window;
}
function createRequestId() {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `android-scan-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
function normalizeString(value) {
    if (value === null || value === undefined) {
        return undefined;
    }
    const text = String(value).trim();
    return text ? text : undefined;
}
function parseNativePayload(payloadJson) {
    if (typeof payloadJson === 'string') {
        return JSON.parse(payloadJson);
    }
    return payloadJson;
}
function callPrevious(callback, payloadJson) {
    try {
        callback?.(payloadJson);
    }
    catch {
        // Native callback fan-out must not break scan request resolution.
    }
}
function handleNativeCallback(payloadJson) {
    let payload;
    try {
        payload = parseNativePayload(payloadJson);
    }
    catch {
        rejectFirstPending('原生扫码回调不是合法 JSON');
        return;
    }
    if (payload.type !== 'scan') {
        return;
    }
    const requestId = normalizeString(payload.requestId);
    const pending = requestId ? pendingRequests.get(requestId) : undefined;
    if (!pending || !requestId) {
        rejectFirstPending('原生扫码回调 requestId 不匹配');
        return;
    }
    window.clearTimeout(pending.timeoutId);
    pendingRequests.delete(requestId);
    pending.resolve(payload);
}
function rejectFirstPending(message) {
    const first = pendingRequests.entries().next();
    if (first.done) {
        return;
    }
    const [requestId, pending] = first.value;
    window.clearTimeout(pending.timeoutId);
    pendingRequests.delete(requestId);
    pending.reject(new Error(message));
}
function installNativeCallbacks() {
    const currentWindow = nativeWindow();
    if (callbacksInstalled || !currentWindow) {
        return;
    }
    previousSuccessCallback = currentWindow.__onNativeCapabilitySuccess;
    previousErrorCallback = currentWindow.__onNativeCapabilityError;
    currentWindow.__onNativeCapabilitySuccess = (payloadJson) => {
        callPrevious(previousSuccessCallback, payloadJson);
        handleNativeCallback(payloadJson);
    };
    currentWindow.__onNativeCapabilityError = (payloadJson) => {
        callPrevious(previousErrorCallback, payloadJson);
        handleNativeCallback(payloadJson);
    };
    callbacksInstalled = true;
}
function bridge() {
    return nativeWindow()?.AndroidBridge;
}
function invokeNativeScan(timeoutMs) {
    installNativeCallbacks();
    const nativeBridge = bridge();
    const nativeMethod = nativeBridge?.scanQRCode;
    if (typeof nativeMethod !== 'function') {
        return Promise.reject(new Error('当前环境未提供 Android 扫码桥接能力'));
    }
    const requestId = createRequestId();
    const payloadJson = JSON.stringify({ requestId, type: 'scan' });
    return new Promise((resolve, reject) => {
        const timeoutId = window.setTimeout(() => {
            pendingRequests.delete(requestId);
            reject(new Error('等待 Android 扫码回调超时，请重试'));
        }, timeoutMs);
        pendingRequests.set(requestId, { resolve, reject, timeoutId });
        try {
            if (import.meta.env.DEV) {
                console.info('[AndroidScanAdapter] invoke scanQRCode', { requestId });
            }
            nativeMethod.call(nativeBridge, requestId, payloadJson);
        }
        catch (error) {
            window.clearTimeout(timeoutId);
            pendingRequests.delete(requestId);
            reject(error instanceof Error ? error : new Error('调用 Android 扫码桥接失败'));
        }
    });
}
function toScanResult(payload) {
    const rawText = normalizeString(payload.rawText) || '';
    const parsed = parseScanQRCodeResult(rawText);
    const code = normalizeString(payload.code) || normalizeString(payload.errorCode);
    const success = payload.success === true;
    return {
        success,
        code: code || (success ? 'SUCCESS' : 'FAILED'),
        token: parsed.token,
        rawText,
        parsed,
        message: normalizeString(payload.message) || normalizeString(payload.errorMessage),
    };
}
export function hasAndroidScanBridge() {
    return typeof bridge()?.scanQRCode === 'function';
}
export class AndroidScanAdapter {
    async startScan() {
        if (!hasAndroidScanBridge()) {
            return {
                success: false,
                code: 'FAILED',
                message: '当前环境未提供 Android 扫码桥接能力',
            };
        }
        if (scanning) {
            return {
                success: false,
                code: 'FAILED',
                message: '已有扫码任务进行中，请稍候',
            };
        }
        scanning = true;
        try {
            const payload = await invokeNativeScan(120000);
            const result = toScanResult(payload);
            if (import.meta.env.DEV) {
                console.info('[AndroidScanAdapter] scan result', result);
            }
            return result;
        }
        finally {
            scanning = false;
        }
    }
}
