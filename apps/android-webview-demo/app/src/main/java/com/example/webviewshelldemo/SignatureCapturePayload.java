package com.example.webviewshelldemo;

import org.json.JSONException;
import org.json.JSONObject;

public final class SignatureCapturePayload {
    String requestId;
    String type = "signature";
    boolean success;
    String signatureFileId;
    String signatureHash;
    String signatureFormat;
    String signedAt;
    String signerType;
    String signerName;
    JSONObject deviceInfo;
    String errorCode;
    String errorMessage;

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("requestId", valueOrNull(requestId));
            json.put("type", type);
            json.put("success", success);
            json.put("signatureFileId", valueOrNull(signatureFileId));
            json.put("signatureHash", valueOrNull(signatureHash));
            json.put("signatureFormat", valueOrNull(signatureFormat));
            json.put("signedAt", valueOrNull(signedAt));
            json.put("signerType", valueOrNull(signerType));
            json.put("signerName", valueOrNull(signerName));
            json.put("deviceInfo", deviceInfo == null ? JSONObject.NULL : deviceInfo);
            json.put("errorCode", valueOrNull(errorCode));
            json.put("errorMessage", valueOrNull(errorMessage));
        } catch (JSONException ignored) {
            // All values are primitive metadata and should not fail JSON serialization.
        }
        return json;
    }

    static SignatureCapturePayload success(
            String requestId,
            NativeFileUploadService.UploadResult upload,
            String signerType,
            String signerName,
            JSONObject deviceInfo
    ) {
        SignatureCapturePayload payload = new SignatureCapturePayload();
        payload.requestId = requestId;
        payload.success = true;
        payload.signatureFileId = upload.fileId;
        payload.signatureHash = upload.sha256Hex;
        payload.signatureFormat = upload.format;
        payload.signedAt = upload.uploadedAt;
        payload.signerType = signerType;
        payload.signerName = signerName;
        payload.deviceInfo = deviceInfo;
        return payload;
    }

    static SignatureCapturePayload failure(String requestId, String errorCode, String errorMessage) {
        SignatureCapturePayload payload = new SignatureCapturePayload();
        payload.requestId = requestId;
        payload.success = false;
        payload.errorCode = errorCode;
        payload.errorMessage = errorMessage;
        return payload;
    }

    private Object valueOrNull(String value) {
        return value == null || value.trim().isEmpty() ? JSONObject.NULL : value;
    }
}
