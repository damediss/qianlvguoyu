const MOCK_DEVICE_SN = 'MOCK-FP-DEVICE-001';
const MOCK_DEVICE_MODEL = 'MockFingerprintDevice';
function requestId() {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `mock-fp-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
function delay(ms) {
    return new Promise((resolve) => {
        window.setTimeout(resolve, ms);
    });
}
export class MockFingerprintAdapter {
    async checkDevice() {
        await delay(120);
        return {
            connected: true,
            deviceSn: MOCK_DEVICE_SN,
            deviceModel: MOCK_DEVICE_MODEL,
            status: 'CONNECTED',
            message: 'Mock fingerprint device connected',
        };
    }
    async startCapture(candidate) {
        await delay(500);
        const failed = Math.random() < 0.08;
        return {
            success: !failed,
            code: failed ? 'MOCK_CAPTURE_FAILED' : 'SUCCESS',
            message: failed ? `Mock fingerprint capture failed for ${candidate.name}` : 'Mock capture success',
            deviceSn: MOCK_DEVICE_SN,
            deviceModel: MOCK_DEVICE_MODEL,
            adapterType: 'MOCK',
            qualityScore: failed ? undefined : Math.round(78 + Math.random() * 20),
            collectedAt: new Date().toISOString(),
            requestId: requestId(),
        };
    }
    async cancelCapture() {
        await delay(80);
    }
}
