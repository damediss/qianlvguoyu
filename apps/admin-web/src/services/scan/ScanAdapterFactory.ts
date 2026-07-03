import type { ScanAdapter, ScanAdapterType } from './ScanAdapter';
import { ManualScanAdapter } from './ManualScanAdapter';
import { MockScanAdapter } from './MockScanAdapter';

const manualAdapter = new ManualScanAdapter();
const mockAdapter = new MockScanAdapter();

function configuredMode(): ScanAdapterType {
  const mode = (import.meta.env.VITE_SCAN_MODE || 'MANUAL').toUpperCase();
  if (mode === 'MANUAL' || mode === 'MOCK' || mode === 'ANDROID_BRIDGE') {
    return mode;
  }
  return 'MANUAL';
}

export function createScanAdapter(): ScanAdapter {
  const mode = configuredMode();
  if (mode === 'MOCK') {
    return mockAdapter;
  }
  if (mode === 'ANDROID_BRIDGE') {
    // Reserved for the Android shell. P0 keeps scan access behind the adapter contract.
    console.warn('Android bridge scan adapter is not implemented yet. Falling back to MANUAL.');
  }
  return manualAdapter;
}

export function getScanAdapter(): ScanAdapter {
  return createScanAdapter();
}
