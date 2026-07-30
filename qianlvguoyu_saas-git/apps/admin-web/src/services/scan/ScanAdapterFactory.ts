import type { ScanAdapter, ScanAdapterType } from './ScanAdapter';
import { AndroidScanAdapter, hasAndroidScanBridge } from './AndroidScanAdapter';
import { ManualScanAdapter } from './ManualScanAdapter';
import { MockScanAdapter } from './MockScanAdapter';

const manualAdapter = new ManualScanAdapter();
const mockAdapter = new MockScanAdapter();
const androidAdapter = new AndroidScanAdapter();

function configuredMode(): ScanAdapterType {
  const mode = (import.meta.env.VITE_SCAN_MODE || 'MANUAL').toUpperCase();
  if (mode === 'MANUAL' || mode === 'MOCK' || mode === 'ANDROID_BRIDGE') {
    return mode;
  }
  return 'MANUAL';
}

export function createScanAdapter(): ScanAdapter {
  if (hasAndroidScanBridge()) {
    return androidAdapter;
  }

  const mode = configuredMode();
  if (mode === 'MOCK') {
    return mockAdapter;
  }
  if (mode === 'ANDROID_BRIDGE') {
    console.warn('Android bridge scan adapter is unavailable. Falling back to MANUAL.');
  }
  return manualAdapter;
}

export function getScanAdapter(): ScanAdapter {
  return createScanAdapter();
}

export { hasAndroidScanBridge };
