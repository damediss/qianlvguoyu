package com.example.webviewshelldemo;

import org.json.JSONException;
import org.json.JSONObject;

public final class QrCodeScanPayload {
    String requestId;
    String type = "scan";
    boolean success;
    String code;
    String rawText;
    JSONObject parsed;
    String message;
    String errorCode;
    String errorMessage;

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("requestId", valueOrNull(requestId));
            json.put("type", type);
            json.put("success", success);
            json.put("code", valueOrNull(code));
            json.put("rawText", valueOrNull(rawText));
            json.put("parsed", parsed == null ? new JSONObject() : parsed);
            json.put("message", valueOrNull(message));
            json.put("errorCode", valueOrNull(errorCode));
            json.put("errorMessage", valueOrNull(errorMessage));
        } catch (JSONException ignored) {
            // Primitive metadata only.
        }
        return json;
    }

    static QrCodeScanPayload success(String requestId, String rawText, JSONObject parsed) {
        QrCodeScanPayload payload = new QrCodeScanPayload();
        payload.requestId = requestId;
        payload.success = true;
        payload.code = "SUCCESS";
        payload.rawText = rawText == null ? "" : rawText;
        payload.parsed = parsed == null ? new JSONObject() : parsed;
        payload.message = "扫码成功";
        return payload;
    }

    static QrCodeScanPayload cancelled(String requestId) {
        QrCodeScanPayload payload = new QrCodeScanPayload();
        payload.requestId = requestId;
        payload.success = false;
        payload.code = "CANCELLED";
        payload.rawText = "";
        payload.parsed = new JSONObject();
        payload.message = "用户取消扫码";
        payload.errorCode = "CANCELLED";
        payload.errorMessage = "用户取消扫码";
        return payload;
    }

    static QrCodeScanPayload failed(String requestId, String message) {
        QrCodeScanPayload payload = new QrCodeScanPayload();
        payload.requestId = requestId;
        payload.success = false;
        payload.code = "FAILED";
        payload.rawText = "";
        payload.parsed = new JSONObject();
        payload.message = message == null || message.trim().isEmpty() ? "扫码失败" : message.trim();
        payload.errorCode = "FAILED";
        payload.errorMessage = payload.message;
        return payload;
    }

    static QrCodeScanPayload permissionDenied(String requestId) {
        QrCodeScanPayload payload = new QrCodeScanPayload();
        payload.requestId = requestId;
        payload.success = false;
        payload.code = "PERMISSION_DENIED";
        payload.rawText = "";
        payload.parsed = new JSONObject();
        payload.message = "未授予摄像头权限";
        payload.errorCode = "PERMISSION_DENIED";
        payload.errorMessage = "未授予摄像头权限";
        return payload;
    }

    static QrCodeScanPayload inProgress(String requestId) {
        QrCodeScanPayload payload = failed(requestId, "已有扫码流程进行中");
        payload.code = "SCAN_IN_PROGRESS";
        payload.errorCode = "SCAN_IN_PROGRESS";
        return payload;
    }

    private Object valueOrNull(String value) {
        return value == null || value.trim().isEmpty() ? JSONObject.NULL : value;
    }
}
