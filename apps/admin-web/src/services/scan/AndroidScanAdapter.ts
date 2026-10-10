import type { ScanAdapter, ScanResult } from './ScanAdapter';
import { parseScanQRCodeResult } from './parseScanQRCodeResult';

interface NativeScanPayload {
  requestId?: string;
  type?: string;
  success?: boolean;
  code?: string;
  rawText?: string;
  parsed?: Record<string, unknown>;
  message?: string;
  errorCode?: string | null;
  errorMessage?: string | null;
}

interface AndroidScanBridgeApi {
  scanQRCode?: (requestId: string, payloadJson: string) => void;
}

interface PendingNativeRequest {
  resolve: (payload: NativeScanPayload) => void;
  reject: (error: Error) => void;
  timeoutId: number;
}

type NativeCallbackPayload = unknown;
type NativeCallback = (payloadJson: NativeCallbackPayload) => void;
type NativeWindow = Window & {
  AndroidBridge?: AndroidScanBridgeApi;
  __onNativeCapabilitySuccess?: NativeCallback;
  __onNativeCapabilityError?: NativeCallback;
};

const pendingRequests = new Map<string, PendingNativeRequest>();
let callbacksInstalled = false;
let previousSuccessCallback: NativeCallback | undefined;
let previousErrorCallback: NativeCallback | undefined;
let scanning = false;

function nativeWindow(): NativeWindow | undefined {
  if (typeof window === 'undefined') {
    return undefined;
  }
  return window as NativeWindow;
}

function createRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `android-scan-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function normalizeString(value: unknown): string | undefined {
  if (value === null || value === undefined) {
    return undefined;
  }
  const text = String(value).trim();
  return text ? text : undefined;
}

function parseNativePayload(payloadJson: NativeCallbackPayload): NativeScanPayload {
  if (typeof payloadJson === 'string') {
    return JSON.parse(payloadJson) as NativeScanPayload;
  }
  return payloadJson as NativeScanPayload;
}

function callPrevious(callback: NativeCallback | undefined, payloadJson: NativeCallbackPayload): void {
  try {
    callback?.(payloadJson);
  } catch {
    // Native callback fan-out must not break scan request resolution.
  }
}

function handleNativeCallback(payloadJson: NativeCallbackPayload): void {
  let payload: NativeScanPayload;
  try {
    payload = parseNativePayload(payloadJson);
  } catch {
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

function rejectFirstPending(message: string): void {
  const first = pendingRequests.entries().next();
  if (first.done) {
    return;
  }
  const [requestId, pending] = first.value;
  window.clearTimeout(pending.timeoutId);
  pendingRequests.delete(requestId);
  pending.reject(new Error(message));
}

function installNativeCallbacks(): void {
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

function bridge(): AndroidScanBridgeApi | undefined {
  return nativeWindow()?.AndroidBridge;
}

function invokeNativeScan(timeoutMs: number): Promise<NativeScanPayload> {
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
    } catch (error) {
      window.clearTimeout(timeoutId);
      pendingRequests.delete(requestId);
      reject(error instanceof Error ? error : new Error('调用 Android 扫码桥接失败'));
    }
  });
}

function toScanResult(payload: NativeScanPayload): ScanResult {
  const rawText = normalizeString(payload.rawText) || '';
  const parsed = parseScanQRCodeResult(rawText);
  const code = normalizeString(payload.code) || normalizeString(payload.errorCode);
  const success = payload.success === true;
  return {
    success,
    code: (code as ScanResult['code']) || (success ? 'SUCCESS' : 'FAILED'),
    token: parsed.token,
    rawText,
    parsed,
    message: normalizeString(payload.message) || normalizeString(payload.errorMessage),
  };
}

export function hasAndroidScanBridge(): boolean {
  return typeof bridge()?.scanQRCode === 'function';
}

export class AndroidScanAdapter implements ScanAdapter {
  async startScan(): Promise<ScanResult> {
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
    } finally {
      scanning = false;
    }
  }
}
