import { AndroidSignatureAdapter, hasAndroidSignatureBridge } from './AndroidSignatureAdapter';
import type { SignatureAdapter } from './SignatureAdapter';

const androidSignatureAdapter = new AndroidSignatureAdapter();

export { hasAndroidSignatureBridge };

export function getSignatureAdapter(): SignatureAdapter {
  return androidSignatureAdapter;
}
