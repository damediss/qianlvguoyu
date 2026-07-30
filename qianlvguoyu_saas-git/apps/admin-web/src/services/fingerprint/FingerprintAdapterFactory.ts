import type { FingerprintAdapter, FingerprintAdapterType } from './FingerprintAdapter';
import { AndroidFingerprintAdapter, hasAndroidFingerprintBridge } from './AndroidFingerprintAdapter';
import { MockFingerprintAdapter } from './MockFingerprintAdapter';

const androidAdapter = new AndroidFingerprintAdapter();
const mockAdapter = new MockFingerprintAdapter();

function configuredMode(): FingerprintAdapterType {
  const mode = (import.meta.env.VITE_FINGERPRINT_MODE || 'MOCK').toUpperCase();
  if (mode === 'WEB' || mode === 'ANDROID_BRIDGE' || mode === 'MOCK') {
    return mode;
  }
  return 'MOCK';
}

export function createFingerprintAdapter(): FingerprintAdapter {
  if (hasAndroidFingerprintBridge()) {
    return androidAdapter;
  }

  const mode = configuredMode();
  if (mode !== 'MOCK') {
    // WEB remains a reserved integration point. Browser and test environments fall back to mock collection.
    console.warn(`Fingerprint mode ${mode} is not implemented yet. Falling back to MOCK.`);
  }
  return mockAdapter;
}

export function getFingerprintAdapter(): FingerprintAdapter {
  return createFingerprintAdapter();
}
