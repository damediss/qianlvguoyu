package com.example.webviewshelldemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public final class QrCodeScanHelper {
    private static final String TAG = "QrCodeScanHelper";
    static final int CAMERA_PERMISSION_REQUEST_CODE = 4101;

    public interface ResultCallback {
        void onComplete(QrCodeScanPayload payload);
    }

    private final Activity activity;
    private boolean scanInProgress;
    private String pendingRequestId;
    private ResultCallback pendingCallback;

    public QrCodeScanHelper(Activity activity) {
        this.activity = activity;
    }

    public void startScan(String requestId, ResultCallback callback) {
        if (scanInProgress) {
            Log.w(TAG, "scanQRCode ignored: scan already in progress, requestId=" + requestId);
            callback.onComplete(QrCodeScanPayload.inProgress(requestId));
            return;
        }

        pendingRequestId = requestId;
        pendingCallback = callback;
        scanInProgress = true;
        Log.i(TAG, "scanQRCode requested, requestId=" + requestId);

        if (!hasCameraPermission()) {
            Log.i(TAG, "requesting CAMERA permission, requestId=" + requestId);
            activity.requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
            return;
        }

        launchScanner();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != CAMERA_PERMISSION_REQUEST_CODE) {
            return;
        }

        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, "CAMERA permission result granted=" + granted + ", requestId=" + pendingRequestId);

        if (!granted) {
            complete(QrCodeScanPayload.permissionDenied(pendingRequestId));
            return;
        }

        launchScanner();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null) {
            return;
        }

        if (!scanInProgress) {
            Log.w(TAG, "ignored scanner activity result because no scan is in progress");
            return;
        }

        if (result.getContents() == null) {
            Log.i(TAG, "scan cancelled, requestId=" + pendingRequestId);
            complete(QrCodeScanPayload.cancelled(pendingRequestId));
            return;
        }

        String rawText = result.getContents().trim();
        if (rawText.isEmpty()) {
            Log.w(TAG, "scan result empty, requestId=" + pendingRequestId);
            complete(QrCodeScanPayload.failed(pendingRequestId, "扫码结果为空"));
            return;
        }

        Log.i(TAG, "scan success, requestId=" + pendingRequestId + ", rawTextLength=" + rawText.length());
        complete(QrCodeScanPayload.success(
                pendingRequestId,
                rawText,
                QrCodeContentParser.parse(rawText)
        ));
    }

    private void launchScanner() {
        Log.i(TAG, "opening scanner activity, requestId=" + pendingRequestId);
        try {
            IntentIntegrator integrator = new IntentIntegrator(activity);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("将考生二维码放入框内");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.setBarcodeImageEnabled(false);
            integrator.setCameraId(0);
            integrator.initiateScan();
        } catch (Exception ex) {
            Log.e(TAG, "failed to open scanner activity, requestId=" + pendingRequestId, ex);
            complete(QrCodeScanPayload.failed(pendingRequestId, "扫码页面打开失败"));
        }
    }

    private boolean hasCameraPermission() {
        return activity.checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void complete(QrCodeScanPayload payload) {
        ResultCallback callback = pendingCallback;
        pendingCallback = null;
        pendingRequestId = null;
        scanInProgress = false;
        if (callback != null) {
            callback.onComplete(payload);
        }
    }
}
