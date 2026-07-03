import type { ScanAdapter, ScanResult } from './ScanAdapter';

export class ManualScanAdapter implements ScanAdapter {
  async startScan(): Promise<ScanResult> {
    if (typeof window === 'undefined' || typeof window.prompt !== 'function') {
      return {
        success: false,
        message: 'Manual token input is required on the page.',
      };
    }
    const rawText = window.prompt('请输入或粘贴采集二维码 token');
    const token = rawText?.trim();
    if (!token) {
      return {
        success: false,
        rawText: rawText ?? undefined,
        message: 'No token entered.',
      };
    }
    return {
      success: true,
      token,
      rawText: token,
    };
  }
}
