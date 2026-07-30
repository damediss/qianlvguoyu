import type { ScanAdapter, ScanResult } from './ScanAdapter';

const DEFAULT_MOCK_TOKEN = 'mock-collection-token';

export class MockScanAdapter implements ScanAdapter {
  async startScan(): Promise<ScanResult> {
    const token = (import.meta.env.VITE_MOCK_SCAN_TOKEN || DEFAULT_MOCK_TOKEN).trim();
    return {
      success: true,
      token,
      rawText: token,
      message: 'Mock scan success',
    };
  }
}
