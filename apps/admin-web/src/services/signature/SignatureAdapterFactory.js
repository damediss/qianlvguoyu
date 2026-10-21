import { AndroidSignatureAdapter, hasAndroidSignatureBridge } from './AndroidSignatureAdapter';
const androidSignatureAdapter = new AndroidSignatureAdapter();
export { hasAndroidSignatureBridge };
export function getSignatureAdapter() {
    return androidSignatureAdapter;
}
