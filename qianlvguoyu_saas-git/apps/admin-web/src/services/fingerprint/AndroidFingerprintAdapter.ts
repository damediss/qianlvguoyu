import type { CandidateMatchVO } from '../../types/certificationPracticalCollection';
import { buildNativeFileUploadPayload } from '../nativeUploadContext';
import type {
  FingerprintAdapter,
  FingerprintCaptureResult,
  FingerprintDeviceStatus,
} from './FingerprintAdapter';

interface NativeFingerprintPayload {
  requestId?: string;
  type?: string;
  success?: boolean;
  collectStatus?: string;
  deviceVendorId?: string | number | null;
  deviceProductId?: string | number | null;
  deviceSn?: string | null;
  deviceModel?: string | null;
  sdkDeviceType?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  durationMs?: number | null;
  qualityScore?: number | null;
  fingerprintImageFileId?: string | number | null;
  fingerprintImageHash?: string | null;
  fingerprintImageFormat?: string | null;
  fingerprintImageCapturedAt?: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
}

interface AndroidBridgeApi {
  collectFingerprint?: (requestId: string, payloadJson: string) => void;
  checkFingerprintDevice?: (requestId: string, payloadJson: string) => void;
  openSignaturePad?: (requestId: string, payloadJson: string) => void;
}

interface PendingNativeRequest {
  resolve: (payload: NativeFingerprintPayload) => void;
  reject: (error: Error) => void;
  timeoutId: number;
}

declare global {
  interface Window {
    AndroidBridge?: AndroidBridgeApi;
    __onNativeCapabilitySuccess?: (payloadJson: string | NativeFingerprintPayload) => void;
    __onNativeCapabilityError?: (payloadJson: string | NativeFingerprintPayload) => void;
  }
}

const pendingRequests = new Map<string, PendingNativeRequest>();
let callbacksInstalled = false;
let previousSuccessCallback: Window['__onNativeCapabilitySuccess'];
let previousErrorCallback: Window['__onNativeCapabilityError'];

export class NativeFingerprintError extends Error {
  readonly code?: string;

  constructor(message: string, code?: string | null) {
    super(message);
    this.name = 'NativeFingerprintError';
    this.code = normalizeString(code);
  }
}

export function isFingerprintCaptureCancelled(error: unknown): boolean {
  return error instanceof NativeFingerprintError
    && error.code === 'FINGERPRINT_CAPTURE_CANCELLED';
}

function createRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `android-fp-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function parseNativePayload(payloadJson: string | NativeFingerprintPayload): NativeFingerprintPayload {
  if (typeof payloadJson === 'string') {
    return JSON.parse(payloadJson) as NativeFingerprintPayload;
  }
  return payloadJson;
}

function normalizeString(value: unknown): string | undefined {
  if (value === null || value === undefined) {
    return undefined;
  }
  const text = String(value).trim();
  return text ? text : undefined;
}

function nativeErrorMessage(payload: NativeFingerprintPayload): string {
  return normalizeString(payload.errorMessage)
    || normalizeString(payload.errorCode)
    || normalizeString(payload.collectStatus)
    || '原生指纹采集失败，请检查 USB 指纹模块后重试';
}

function nativeError(payload: NativeFingerprintPayload): NativeFingerprintError {
  return new NativeFingerprintError(nativeErrorMessage(payload), payload.errorCode);
}

function rejectFirstPendingFingerprint(message: string): void {
  const first = pendingRequests.entries().next();
  if (first.done) {
    return;
  }
  const [requestId, pending] = first.value;
  window.clearTimeout(pending.timeoutId);
  pendingRequests.delete(requestId);
  pending.reject(new Error(message));
}

function handleNativeCallback(payloadJson: string | NativeFingerprintPayload, fromErrorCallback: boolean): void {
  let payload: NativeFingerprintPayload;
  try {
    payload = parseNativePayload(payloadJson);
  } catch {
    rejectFirstPendingFingerprint('原生指纹回调不是合法 JSON');
    return;
  }

  if (payload.type !== 'fingerprint') {
    return;
  }

  const requestId = normalizeString(payload.requestId);
  const pending = requestId ? pendingRequests.get(requestId) : undefined;
  if (!pending || !requestId) {
    rejectFirstPendingFingerprint('原生指纹回调 requestId 不匹配');
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

function callPrevious(
  callback: Window['__onNativeCapabilitySuccess'] | Window['__onNativeCapabilityError'],
  payloadJson: string | NativeFingerprintPayload,
): void {
  try {
    callback?.(payloadJson);
  } catch {
    // Native callback fan-out must not break fingerprint request resolution.
  }
}

function installNativeCallbacks(): void {
  if (callbacksInstalled || typeof window === 'undefined') {
    return;
  }
  previousSuccessCallback = window.__onNativeCapabilitySuccess;
  previousErrorCallback = window.__onNativeCapabilityError;
  window.__onNativeCapabilitySuccess = (payloadJson) => {
    callPrevious(previousSuccessCallback, payloadJson);
    handleNativeCallback(payloadJson, false);
  };
  window.__onNativeCapabilityError = (payloadJson) => {
    callPrevious(previousErrorCallback, payloadJson);
    handleNativeCallback(payloadJson, true);
  };
  callbacksInstalled = true;
}

function bridge(): AndroidBridgeApi | undefined {
  if (typeof window === 'undefined') {
    return undefined;
  }
  return window.AndroidBridge;
}

function invokeNativeFingerprint(
  methodName: keyof AndroidBridgeApi,
  payload: Record<string, unknown>,
  timeoutMs: number,
): Promise<NativeFingerprintPayload> {
  installNativeCallbacks();
  const nativeBridge = bridge();
  const nativeMethod = nativeBridge?.[methodName];
  if (typeof nativeMethod !== 'function') {
    return Promise.reject(new Error('当前环境未提供 Android 指纹桥接能力'));
  }

  const requestId = normalizeString(payload.requestId) || createRequestId();
  const payloadJson = JSON.stringify({ ...payload, requestId, type: 'fingerprint' });
  return new Promise((resolve, reject) => {
    const timeoutId = window.setTimeout(() => {
      pendingRequests.delete(requestId);
      reject(new Error('等待 Android 指纹回调超时，请确认设备连接和 USB 授权状态'));
    }, timeoutMs);
    pendingRequests.set(requestId, { resolve, reject, timeoutId });
    try {
      nativeMethod.call(nativeBridge, requestId, payloadJson);
    } catch (error) {
      window.clearTimeout(timeoutId);
      pendingRequests.delete(requestId);
      reject(error instanceof Error ? error : new Error('调用 Android 指纹桥接失败'));
    }
  });
}

function toCaptureResult(payload: NativeFingerprintPayload): FingerprintCaptureResult {
  const requestId = normalizeString(payload.requestId) || createRequestId();
  const success = payload.success === true;
  const finishedAt = normalizeString(payload.finishedAt) || new Date().toISOString();
  return {
    success,
    code: normalizeString(payload.errorCode) || (success ? 'SUCCESS' : normalizeString(payload.collectStatus) || 'FAILED'),
    message: success ? 'Android fingerprint capture success' : nativeErrorMessage(payload),
    deviceSn: normalizeString(payload.deviceSn),
    deviceModel: normalizeString(payload.deviceModel),
    deviceVendorId: normalizeString(payload.deviceVendorId),
    deviceProductId: normalizeString(payload.deviceProductId),
    sdkDeviceType: normalizeString(payload.sdkDeviceType),
    adapterType: 'ANDROID_BRIDGE',
    collectStatus: normalizeString(payload.collectStatus),
    qualityScore: typeof payload.qualityScore === 'number' ? payload.qualityScore : null,
    startedAt: normalizeString(payload.startedAt),
    finishedAt,
    durationMs: typeof payload.durationMs === 'number' ? payload.durationMs : null,
    collectedAt: finishedAt,
    fingerprintImageFileId: payload.fingerprintImageFileId ?? undefined,
    fingerprintImageHash: normalizeString(payload.fingerprintImageHash),
    fingerprintImageFormat: normalizeString(payload.fingerprintImageFormat),
    fingerprintImageCapturedAt: normalizeString(payload.fingerprintImageCapturedAt),
    requestId,
  };
}

export function hasAndroidFingerprintBridge(): boolean {
  return typeof bridge()?.collectFingerprint === 'function';
}

export class AndroidFingerprintAdapter implements FingerprintAdapter {
  private capturing = false;

  async checkDevice(): Promise<FingerprintDeviceStatus> {
    if (!hasAndroidFingerprintBridge()) {
      return {
        connected: false,
        status: 'DISCONNECTED',
        message: '当前环境未检测到 AndroidBridge',
      };
    }

    if (typeof bridge()?.checkFingerprintDevice !== 'function') {
      return {
        connected: true,
        status: 'CONNECTED',
        message: 'AndroidBridge 已就绪，采集时将检测 USB 指纹设备',
      };
    }

    try {
      const payload = await invokeNativeFingerprint('checkFingerprintDevice', {}, 10000);
      return {
        connected: payload.success === true,
        deviceSn: normalizeString(payload.deviceSn),
        deviceModel: normalizeString(payload.deviceModel),
        status: payload.success === true ? 'CONNECTED' : 'DISCONNECTED',
        message: payload.success === true ? 'Android 指纹设备已就绪' : nativeErrorMessage(payload),
      };
    } catch (error) {
      return {
        connected: false,
        status: 'ERROR',
        message: error instanceof Error ? error.message : 'Android 指纹设备检测失败',
      };
    }
  }

  async startCapture(candidate: CandidateMatchVO): Promise<FingerprintCaptureResult> {
    if (this.capturing) {
      throw new Error('已有指纹采集任务进行中，请等待原生回调完成');
    }

    this.capturing = true;
    try {
      const payload = await invokeNativeFingerprint('collectFingerprint', {
        taskId: String(candidate.taskId),
        candidateId: String(candidate.candidateId),
        candidateName: candidate.name,
        ...buildNativeFileUploadPayload(),
      }, 300000);
      return toCaptureResult(payload);
    } finally {
      this.capturing = false;
    }
  }

  async cancelCapture(): Promise<void> {
    return Promise.resolve();
  }
}
