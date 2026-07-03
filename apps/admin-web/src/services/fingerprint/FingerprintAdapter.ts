import type { CandidateMatchVO } from '../../types/certificationPracticalCollection';

export type FingerprintAdapterType = 'MOCK' | 'WEB' | 'ANDROID_BRIDGE';

export interface FingerprintDeviceStatus {
  connected: boolean;
  deviceSn?: string;
  deviceModel?: string;
  status: 'UNKNOWN' | 'CONNECTED' | 'DISCONNECTED' | 'ERROR';
  message?: string;
}

export interface FingerprintCaptureResult {
  success: boolean;
  code: string;
  message: string;
  deviceSn?: string;
  deviceModel?: string;
  deviceVendorId?: string;
  deviceProductId?: string;
  sdkDeviceType?: string;
  adapterType: FingerprintAdapterType;
  collectStatus?: string;
  qualityScore?: number | null;
  startedAt?: string;
  finishedAt?: string;
  durationMs?: number | null;
  collectedAt: string;
  fingerprintImageFileId?: string | number;
  fingerprintImageHash?: string;
  fingerprintImageFormat?: string;
  fingerprintImageCapturedAt?: string;
  requestId: string;
}

export interface FingerprintAdapter {
  checkDevice(): Promise<FingerprintDeviceStatus>;
  startCapture(candidate: CandidateMatchVO): Promise<FingerprintCaptureResult>;
  cancelCapture(): Promise<void>;
}
