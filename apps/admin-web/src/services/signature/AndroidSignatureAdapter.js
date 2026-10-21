import { buildNativeFileUploadPayload } from '../nativeUploadContext';
const pendingRequests = new Map();
let callbacksInstalled = false;
let previousSuccessCallback;
let previousErrorCallback;
function normalizeString(value) {
    if (value === null || value === undefined) {
        return undefined;
    }
    const text = String(value).trim();
    return text ? text : undefined;
}
export class NativeSignatureError extends Error {
    code;
    constructor(message, code) {
        super(message);
        this.name = 'NativeSignatureError';
        this.code = normalizeString(code);
    }
}
export function isSignatureCancelled(error) {
    return error instanceof NativeSignatureError && error.code === 'SIGNATURE_CANCELLED';
}
function createRequestId() {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `android-sign-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
function parseNativePayload(payloadJson) {
    if (typeof payloadJson === 'string') {
        return JSON.parse(payloadJson);
    }
    return payloadJson;
}
function nativeErrorMessage(payload) {
    return normalizeString(payload.errorMessage)
        || normalizeString(payload.errorCode)
        || '原生签名失败，请重试';
}
function nativeError(payload) {
    return new NativeSignatureError(nativeErrorMessage(payload), payload.errorCode);
}
function rejectFirstPendingSignature(message) {
    const first = pendingRequests.entries().next();
    if (first.done) {
        return;
    }
    const [requestId, pending] = first.value;
    window.clearTimeout(pending.timeoutId);
    pendingRequests.delete(requestId);
    pending.reject(new Error(message));
}
function handleNativeCallback(payloadJson, fromErrorCallback) {
    let payload;
    try {
        payload = parseNativePayload(payloadJson);
    }
    catch {
        rejectFirstPendingSignature('原生签名回调不是合法 JSON');
        return;
    }
    if (payload.type !== 'signature') {
        return;
    }
    const requestId = normalizeString(payload.requestId);
    const pending = requestId ? pendingRequests.get(requestId) : undefined;
    if (!pending || !requestId) {
        rejectFirstPendingSignature('原生签名回调 requestId 不匹配');
        return;
    }
    window.clearTimeout(pending.timeoutId);
    pendingRequests.delete(requestId);
    if (fromErrorCallback || payload.success === false) {
        pending.reject(nativeError(payload));
        return;
    }
    pending.resolve(payload);
}
function callPrevious(callback, payloadJson) {
    try {
        callback?.(payloadJson);
    }
    catch {
        // Native callback fan-out must not break signature request resolution.
    }
}
function installNativeCallbacks() {
    if (callbacksInstalled || typeof window === 'undefined') {
        return;
    }
    const nativeWindow = window;
    previousSuccessCallback = nativeWindow.__onNativeCapabilitySuccess;
    previousErrorCallback = nativeWindow.__onNativeCapabilityError;
    nativeWindow.__onNativeCapabilitySuccess = (payloadJson) => {
        callPrevious(previousSuccessCallback, payloadJson);
        handleNativeCallback(payloadJson, false);
    };
    nativeWindow.__onNativeCapabilityError = (payloadJson) => {
        callPrevious(previousErrorCallback, payloadJson);
        handleNativeCallback(payloadJson, true);
    };
    callbacksInstalled = true;
}
function bridge() {
    if (typeof window === 'undefined') {
        return undefined;
    }
    return window.AndroidBridge;
}
export function hasAndroidSignatureBridge() {
    return typeof bridge()?.openSignaturePad === 'function';
}
function invokeNativeSignature(payload, timeoutMs) {
    installNativeCallbacks();
    const nativeBridge = bridge();
    const nativeMethod = nativeBridge?.openSignaturePad;
    if (typeof nativeMethod !== 'function') {
        return Promise.reject(new Error('当前环境未提供 Android 原生签名能力'));
    }
    const requestId = normalizeString(payload.requestId) || createRequestId();
    const payloadJson = JSON.stringify({ ...payload, requestId, type: 'signature' });
    return new Promise((resolve, reject) => {
        const timeoutId = window.setTimeout(() => {
            pendingRequests.delete(requestId);
            reject(new Error('等待 Android 签名回调超时，请确认是否已完成签名'));
        }, timeoutMs);
        pendingRequests.set(requestId, { resolve, reject, timeoutId });
        try {
            nativeMethod.call(nativeBridge, requestId, payloadJson);
        }
        catch (error) {
            window.clearTimeout(timeoutId);
            pendingRequests.delete(requestId);
            reject(error instanceof Error ? error : new Error('调用 Android 原生签名失败'));
        }
    });
}
function toSignatureResult(payload) {
    const signatureFileId = payload.signatureFileId;
    const signatureHash = normalizeString(payload.signatureHash);
    const signedAt = normalizeString(payload.signedAt) || new Date().toISOString();
    if (!signatureFileId || !signatureHash) {
        throw new Error('Android 签名回调缺少文件 ID 或 hash');
    }
    return {
        requestId: normalizeString(payload.requestId) || createRequestId(),
        success: true,
        signatureFileId,
        signatureHash,
        signatureFormat: normalizeString(payload.signatureFormat),
        signedAt,
        signerType: (normalizeString(payload.signerType) || 'CANDIDATE'),
        signerName: normalizeString(payload.signerName) || '',
        deviceInfo: payload.deviceInfo || undefined,
    };
}
export class AndroidSignatureAdapter {
    async open(candidate) {
        return openNativeSignaturePad({
            taskId: String(candidate.taskId),
            candidateId: String(candidate.candidateId),
            sessionId: candidate.sessionId ? String(candidate.sessionId) : undefined,
            signerType: 'CANDIDATE',
            signerName: candidate.name,
        });
    }
}
export async function openNativeSignaturePad(options) {
    const adapterGuard = androidSigningGuard;
    if (adapterGuard.signing) {
        throw new Error('已有签名任务进行中，请等待原生回调完成');
    }
    adapterGuard.signing = true;
    try {
        const payload = await invokeNativeSignature({
            taskId: options.taskId,
            candidateId: options.candidateId,
            sessionId: options.sessionId,
            signerType: options.signerType || 'EVALUATOR',
            signerName: options.signerName,
            ...buildNativeFileUploadPayload(),
        }, 300000);
        return toSignatureResult(payload);
    }
    finally {
        adapterGuard.signing = false;
    }
}
const androidSigningGuard = { signing: false };
