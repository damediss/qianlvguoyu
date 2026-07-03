package com.example.webviewshelldemo;

import android.app.Activity;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

public final class AndroidBridge {
    private final Activity activity;
    private final WebView webView;
    private final FingerprintSdkAdapter fingerprintSdkAdapter;
    private boolean fingerprintDialogOpen;
    private boolean signatureDialogOpen;

    public AndroidBridge(
            Activity activity,
            WebView webView,
            FingerprintSdkAdapter fingerprintSdkAdapter
    ) {
        this.activity = activity;
        this.webView = webView;
        this.fingerprintSdkAdapter = fingerprintSdkAdapter;
    }

    @JavascriptInterface
    public void collectFingerprint(String requestId, String payloadJson) {
        String resolvedRequestId = resolveRequestId(requestId, payloadJson);
        JSONObject payload = parsePayload(payloadJson);
        if (payload == null) {
            callback("window.__onNativeCapabilityError", fingerprintFailure(
                    resolvedRequestId,
                    FingerprintSdkAdapter.STATUS_SDK_EXCEPTION,
                    "AndroidBridge.collectFingerprint payloadJson 非法"
            ));
            return;
        }
        NativeFileUploadService.UploadRequest uploadRequest;
        try {
            uploadRequest = NativeFileUploadService.UploadRequest.fromPayload(payload);
        } catch (NativeFileUploadService.UploadException ex) {
            callback("window.__onNativeCapabilityError", fingerprintFailure(
                    resolvedRequestId,
                    "FINGERPRINT_IMAGE_UPLOAD_FAILED",
                    ex.getMessage()
            ));
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (fingerprintDialogOpen) {
                    callback("window.__onNativeCapabilityError", fingerprintFailure(
                            resolvedRequestId,
                            FingerprintSdkAdapter.STATUS_CAPTURE_IN_PROGRESS,
                            "已有指纹采集流程进行中"
                    ));
                    return;
                }
                fingerprintDialogOpen = true;
                FingerprintCaptureDialog dialog = new FingerprintCaptureDialog(
                        activity,
                        fingerprintSdkAdapter,
                        uploadRequest,
                        resolvedRequestId,
                        payload.optString("taskId", ""),
                        payload.optString("candidateId", ""),
                        new FingerprintCaptureDialog.ResultCallback() {
                            @Override
                            public void onComplete(FingerprintCapturePayload result) {
                                callback(result.success
                                        ? "window.__onNativeCapabilitySuccess"
                                        : "window.__onNativeCapabilityError", result);
                            }
                        }
                );
                dialog.setOnDismissListener(dialogInterface -> fingerprintDialogOpen = false);
                dialog.show();
            }
        });
    }

    @JavascriptInterface
    public void openSignaturePad(String requestId, String payloadJson) {
        String resolvedRequestId = resolveRequestId(requestId, payloadJson);
        JSONObject payload = parsePayload(payloadJson);
        if (payload == null) {
            callback("window.__onNativeCapabilityError", SignatureCapturePayload.failure(
                    resolvedRequestId,
                    "SIGNATURE_UNKNOWN_ERROR",
                    "AndroidBridge.openSignaturePad payloadJson 非法"
            ));
            return;
        }
        NativeFileUploadService.UploadRequest uploadRequest;
        try {
            uploadRequest = NativeFileUploadService.UploadRequest.fromPayload(payload);
        } catch (NativeFileUploadService.UploadException ex) {
            callback("window.__onNativeCapabilityError", SignatureCapturePayload.failure(
                    resolvedRequestId,
                    "SIGNATURE_UPLOAD_FAILED",
                    ex.getMessage()
            ));
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (signatureDialogOpen) {
                    callback("window.__onNativeCapabilityError", SignatureCapturePayload.failure(
                            resolvedRequestId,
                            "SIGNATURE_UNKNOWN_ERROR",
                            "已有签名流程进行中"
                    ));
                    return;
                }
                signatureDialogOpen = true;
                SignatureCaptureDialog dialog = new SignatureCaptureDialog(
                        activity,
                        uploadRequest,
                        resolvedRequestId,
                        payload.optString("taskId", ""),
                        payload.optString("candidateId", ""),
                        payload.optString("signerType", "CANDIDATE"),
                        payload.optString("signerName", ""),
                        new SignatureCaptureDialog.ResultCallback() {
                            @Override
                            public void onComplete(SignatureCapturePayload result) {
                                callback(result.success
                                        ? "window.__onNativeCapabilitySuccess"
                                        : "window.__onNativeCapabilityError", result);
                            }
                        }
                );
                dialog.setOnDismissListener(dialogInterface -> signatureDialogOpen = false);
                dialog.show();
            }
        });
    }

    @JavascriptInterface
    public void checkFingerprintDevice(String requestId, String payloadJson) {
        FingerprintCapturePayload payload = fingerprintSdkAdapter.checkDevice();
        String resolvedRequestId = resolveRequestId(requestId, payloadJson);
        payload.requestId = resolvedRequestId;
        if (FingerprintSdkAdapter.STATUS_USB_PERMISSION_REQUIRED.equals(payload.collectStatus)) {
            fingerprintSdkAdapter.requestUsbPermission(new FingerprintSdkAdapter.ResultCallback() {
                @Override
                public void onComplete(FingerprintCapturePayload permissionPayload) {
                    permissionPayload.requestId = resolvedRequestId;
                    callback(permissionPayload.success
                            ? "window.__onNativeCapabilitySuccess"
                            : "window.__onNativeCapabilityError", permissionPayload);
                }
            });
            return;
        }
        callback(payload.success
                ? "window.__onNativeCapabilitySuccess"
                : "window.__onNativeCapabilityError", payload);
    }

    @JavascriptInterface
    public void getDeviceInfo(String requestId, String payloadJson) {
        String resolvedRequestId = resolveRequestId(requestId, payloadJson);
        JSONObject payload = new JSONObject();
        try {
            payload.put("requestId", resolvedRequestId);
            payload.put("type", "deviceInfo");
            payload.put("success", true);
            payload.put("platform", "android");
            payload.put("manufacturer", Build.MANUFACTURER);
            payload.put("model", Build.MODEL);
            payload.put("sdkInt", Build.VERSION.SDK_INT);
        } catch (JSONException ignored) {
            // Primitive metadata only.
        }
        callback("window.__onNativeCapabilitySuccess", payload);
    }

    private String resolveRequestId(String requestId, String payloadJson) {
        if (requestId != null && !requestId.trim().isEmpty()) {
            return requestId.trim();
        }
        if (payloadJson != null && !payloadJson.trim().isEmpty()) {
            try {
                JSONObject json = new JSONObject(payloadJson);
                String payloadRequestId = json.optString("requestId", "");
                if (!payloadRequestId.trim().isEmpty()) {
                    return payloadRequestId.trim();
                }
            } catch (JSONException ignored) {
                // Invalid H5 payloads are handled by generating a native request id.
            }
        }
        return "android-fp-" + System.currentTimeMillis();
    }

    private void callback(String functionName, FingerprintCapturePayload payload) {
        callback(functionName, payload.toJson());
    }

    private void callback(String functionName, SignatureCapturePayload payload) {
        callback(functionName, payload.toJson());
    }

    private void callback(String functionName, JSONObject payloadJson) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String quotedPayload = JSONObject.quote(payloadJson.toString());
                String script = "if (" + functionName + ") { " + functionName + "(" + quotedPayload + "); }";
                webView.evaluateJavascript(script, null);
            }
        });
    }

    private JSONObject parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(payloadJson);
        } catch (JSONException ex) {
            return null;
        }
    }

    private FingerprintCapturePayload fingerprintFailure(String requestId, String errorCode, String errorMessage) {
        FingerprintCapturePayload payload = new FingerprintCapturePayload();
        payload.requestId = requestId;
        payload.success = false;
        payload.collectStatus = errorCode;
        payload.errorCode = errorCode;
        payload.errorMessage = errorMessage;
        payload.finishedAt = NativeFileUploadService.formatNow();
        return payload;
    }
}
