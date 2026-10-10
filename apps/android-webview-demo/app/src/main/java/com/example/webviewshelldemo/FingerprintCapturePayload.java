package com.example.webviewshelldemo;

import org.json.JSONException;
import org.json.JSONObject;

public final class FingerprintCapturePayload {
    String requestId;
    String type = "fingerprint";
    boolean success;
    String collectStatus;
    String deviceVendorId;
    String deviceProductId;
    String deviceSn;
    String deviceModel;
    String sdkDeviceType;
    String startedAt;
    String finishedAt;
    Long durationMs;
    Double qualityScore;
    String fingerprintImageFileId;
    String fingerprintImageHash;
    String fingerprintImageFormat;
    String fingerprintImageCapturedAt;
    String errorCode;
    String errorMessage;

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("requestId", valueOrNull(requestId));
            json.put("type", type);
            json.put("success", success);
            json.put("collectStatus", valueOrNull(collectStatus));
            json.put("deviceVendorId", valueOrNull(deviceVendorId));
            json.put("deviceProductId", valueOrNull(deviceProductId));
            json.put("deviceSn", valueOrNull(deviceSn));
            json.put("deviceModel", valueOrNull(deviceModel));
            json.put("sdkDeviceType", valueOrNull(sdkDeviceType));
            json.put("startedAt", valueOrNull(startedAt));
            json.put("finishedAt", valueOrNull(finishedAt));
            json.put("durationMs", durationMs == null ? JSONObject.NULL : durationMs);
            json.put("qualityScore", qualityScore == null ? JSONObject.NULL : qualityScore);
            json.put("fingerprintImageFileId", valueOrNull(fingerprintImageFileId));
            json.put("fingerprintImageHash", valueOrNull(fingerprintImageHash));
            json.put("fingerprintImageFormat", valueOrNull(fingerprintImageFormat));
            json.put("fingerprintImageCapturedAt", valueOrNull(fingerprintImageCapturedAt));
            json.put("errorCode", valueOrNull(errorCode));
            json.put("errorMessage", valueOrNull(errorMessage));
        } catch (JSONException ignored) {
            // All values are primitive metadata and should not fail JSON serialization.
        }
        return json;
    }

    private Object valueOrNull(String value) {
        return value == null || value.trim().isEmpty() ? JSONObject.NULL : value;
    }
}
