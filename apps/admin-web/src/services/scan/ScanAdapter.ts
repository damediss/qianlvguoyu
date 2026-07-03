export type ScanAdapterType = 'MANUAL' | 'MOCK' | 'ANDROID_BRIDGE';

export interface ScanResult {
  success: boolean;
  token?: string;
  rawText?: string;
  message?: string;
}

export interface ScanAdapter {
  startScan(): Promise<ScanResult>;
}
