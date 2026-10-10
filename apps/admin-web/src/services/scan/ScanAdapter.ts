import type { ParsedScanResult } from './parseScanQRCodeResult';

export type ScanAdapterType = 'MANUAL' | 'MOCK' | 'ANDROID_BRIDGE';

export type ScanResultCode = 'SUCCESS' | 'CANCELLED' | 'FAILED' | 'PERMISSION_DENIED';

export interface ScanResult {
  success: boolean;
  code?: ScanResultCode;
  token?: string;
  rawText?: string;
  parsed?: ParsedScanResult;
  message?: string;
}

export interface ScanAdapter {
  startScan(): Promise<ScanResult>;
}
