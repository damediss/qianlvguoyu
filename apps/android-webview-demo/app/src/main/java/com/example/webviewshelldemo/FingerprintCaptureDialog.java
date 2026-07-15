package com.example.webviewshelldemo;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FingerprintCaptureDialog extends Dialog {
    public interface ResultCallback {
        void onComplete(FingerprintCapturePayload payload);
    }

    private enum UiState {
        IDLE,
        CAPTURING,
        PREVIEW,
        UPLOADING,
        ERROR
    }

    private final Activity activity;
    private final FingerprintSdkAdapter fingerprintSdkAdapter;
    private final NativeFileUploadService.UploadRequest uploadRequest;
    private final NativeFileUploadService uploadService = new NativeFileUploadService();
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private final String requestId;
    private final String taskId;
    private final String candidateId;
    private final ResultCallback callback;

    private TextView statusView;
    private ImageView previewImage;
    private ProgressBar progressBar;
    private Button startButton;
    private Button saveButton;
    private Button recaptureButton;
    private Button cancelButton;

    private Bitmap capturedBitmap;
    private FingerprintCapturePayload capturedPayload;
    private UiState state = UiState.IDLE;
    private boolean completed;

    public FingerprintCaptureDialog(
            Activity activity,
            FingerprintSdkAdapter fingerprintSdkAdapter,
            NativeFileUploadService.UploadRequest uploadRequest,
            String requestId,
            String taskId,
            String candidateId,
            ResultCallback callback
    ) {
        super(activity);
        this.activity = activity;
        this.fingerprintSdkAdapter = fingerprintSdkAdapter;
        this.uploadRequest = uploadRequest;
        this.requestId = requestId;
        this.taskId = taskId;
        this.candidateId = candidateId;
        this.callback = callback;
        setCanceledOnTouchOutside(false);
        setOnCancelListener(dialog -> cancelFlow());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(buildContentView());
        Window dialogWindow = getWindow();
        if (dialogWindow != null) {
            dialogWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        startCapture();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView title = new TextView(activity);
        title.setText("指纹采集");
        title.setTextSize(22);
        title.setGravity(Gravity.START);
        root.addView(title, matchWrap());

        statusView = new TextView(activity);
        statusView.setTextSize(15);
        statusView.setPadding(0, dp(8), 0, dp(12));
        root.addView(statusView, matchWrap());

        previewImage = new ImageView(activity);
        previewImage.setBackgroundColor(0xfff8fafc);
        previewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(280)
        );
        root.addView(previewImage, imageParams);

        progressBar = new ProgressBar(activity);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.setMargins(0, dp(14), 0, dp(4));
        root.addView(progressBar, progressParams);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, dp(14), 0, 0);
        root.addView(actions, matchWrap());

        cancelButton = addActionButton(actions, "取消");
        recaptureButton = addActionButton(actions, "重新采集");
        startButton = addActionButton(actions, "开始采集");
        saveButton = addActionButton(actions, "确认保存");

        cancelButton.setOnClickListener(view -> cancelFlow());
        recaptureButton.setOnClickListener(view -> {
            clearCapturedBitmap();
            startCapture();
        });
        startButton.setOnClickListener(view -> startCapture());
        saveButton.setOnClickListener(view -> confirmUpload());
        return root;
    }

    private Button addActionButton(LinearLayout parent, String text) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(46)
        );
        params.setMargins(dp(8), 0, 0, 0);
        parent.addView(button, params);
        return button;
    }

    private void startCapture() {
        if (completed || state == UiState.CAPTURING || state == UiState.UPLOADING) {
            return;
        }
        clearCapturedBitmap();
        setState(UiState.CAPTURING, "采集中，请保持手指按压");
        fingerprintSdkAdapter.collectImageOnce(requestId, result ->
                activity.runOnUiThread(() -> handleCaptureResult(result)));
    }

    private void handleCaptureResult(FingerprintImageCaptureResult result) {
        if (completed) {
            if (result != null && result.bitmap != null && !result.bitmap.isRecycled()) {
                result.bitmap.recycle();
            }
            return;
        }
        FingerprintCapturePayload payload = result == null ? null : result.payload;
        if (payload == null) {
            setState(UiState.ERROR, "指纹采集失败，请重新采集");
            return;
        }
        if (!payload.success) {
            setState(UiState.ERROR, safeMessage(payload.errorMessage, "指纹采集失败，请重新采集"));
            return;
        }
        if (result.bitmap == null || result.bitmap.isRecycled()) {
            setState(UiState.ERROR, "指纹图片读取失败，请重新采集");
            return;
        }
        capturedPayload = payload;
        capturedBitmap = result.bitmap;
        previewImage.setImageBitmap(capturedBitmap);
        setState(UiState.PREVIEW, "请确认指纹图片后保存");
    }

    private void confirmUpload() {
        if (completed || state == UiState.UPLOADING) {
            return;
        }
        if (capturedBitmap == null || capturedBitmap.isRecycled() || capturedPayload == null) {
            setState(UiState.ERROR, "暂无可保存的指纹图片，请重新采集");
            return;
        }
        setState(UiState.UPLOADING, "保存中...");
        uploadExecutor.execute(() -> {
            try {
                byte[] pngBytes = bitmapToPng(capturedBitmap);
                NativeFileUploadService.UploadResult upload = uploadService.uploadPng(
                        pngBytes,
                        "fingerprint-" + safeNamePart(taskId) + "-" + safeNamePart(candidateId)
                                + "-" + System.currentTimeMillis() + ".png",
                        uploadRequest
                );
                activity.runOnUiThread(() -> applyUploadSuccess(upload));
            } catch (NativeFileUploadService.UploadException ex) {
                activity.runOnUiThread(() ->
                        setState(UiState.ERROR, safeMessage(ex.getMessage(), "指纹图片上传失败")));
            } catch (RuntimeException ex) {
                activity.runOnUiThread(() ->
                        setState(UiState.ERROR, "指纹图片生成失败，请重新采集"));
            }
        });
    }

    private void applyUploadSuccess(NativeFileUploadService.UploadResult upload) {
        if (completed || capturedPayload == null) {
            return;
        }
        capturedPayload.fingerprintImageFileId = upload.fileId;
        capturedPayload.fingerprintImageHash = upload.sha256Hex;
        capturedPayload.fingerprintImageFormat = upload.format;
        capturedPayload.fingerprintImageCapturedAt = capturedPayload.finishedAt == null
                ? upload.uploadedAt
                : capturedPayload.finishedAt;
        completed = true;
        FingerprintCapturePayload result = capturedPayload;
        clearCapturedBitmap();
        callback.onComplete(result);
        dismiss();
    }

    private byte[] bitmapToPng(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (!ok || output.size() == 0) {
            throw new IllegalStateException("PNG compression failed");
        }
        return output.toByteArray();
    }

    private void cancelFlow() {
        if (completed) {
            return;
        }
        completed = true;
        if (state == UiState.CAPTURING) {
            fingerprintSdkAdapter.cancelActiveCapture("用户取消指纹采集");
        }
        clearCapturedBitmap();
        callback.onComplete(cancelPayload());
        dismiss();
    }

    private FingerprintCapturePayload cancelPayload() {
        FingerprintCapturePayload payload = new FingerprintCapturePayload();
        payload.requestId = requestId;
        payload.success = false;
        payload.collectStatus = FingerprintSdkAdapter.STATUS_CAPTURE_CANCELLED;
        payload.errorCode = FingerprintSdkAdapter.STATUS_CAPTURE_CANCELLED;
        payload.errorMessage = "用户取消指纹采集";
        payload.finishedAt = NativeFileUploadService.formatNow();
        return payload;
    }

    private void setState(UiState nextState, String message) {
        state = nextState;
        statusView.setText(message);
        progressBar.setVisibility(nextState == UiState.CAPTURING || nextState == UiState.UPLOADING
                ? View.VISIBLE
                : View.GONE);
        startButton.setVisibility(nextState == UiState.IDLE || (nextState == UiState.ERROR && capturedBitmap == null)
                ? View.VISIBLE
                : View.GONE);
        saveButton.setVisibility(nextState == UiState.PREVIEW || (nextState == UiState.ERROR && capturedBitmap != null)
                ? View.VISIBLE
                : View.GONE);
        recaptureButton.setVisibility(nextState == UiState.PREVIEW || nextState == UiState.ERROR
                ? View.VISIBLE
                : View.GONE);
        cancelButton.setEnabled(nextState != UiState.UPLOADING);
        startButton.setEnabled(nextState != UiState.CAPTURING && nextState != UiState.UPLOADING);
        saveButton.setEnabled(nextState != UiState.UPLOADING);
        recaptureButton.setEnabled(nextState != UiState.UPLOADING);
        saveButton.setText(nextState == UiState.ERROR && capturedBitmap != null ? "重试保存" : "确认保存");
        if (nextState == UiState.IDLE) {
            previewImage.setImageDrawable(null);
        }
    }

    private void clearCapturedBitmap() {
        previewImage.setImageDrawable(null);
        if (capturedBitmap != null && !capturedBitmap.isRecycled()) {
            capturedBitmap.recycle();
        }
        capturedBitmap = null;
        capturedPayload = null;
    }

    private String safeMessage(String value, String fallback) {
        String text = NativeFileUploadService.trim(value);
        return text == null ? fallback : text;
    }

    private String safeNamePart(String value) {
        String text = NativeFileUploadService.trim(value);
        return text == null ? "unknown" : text.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    @Override
    public void dismiss() {
        uploadExecutor.shutdownNow();
        super.dismiss();
    }
}
