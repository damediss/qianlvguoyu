package com.example.webviewshelldemo;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SignatureCaptureDialog extends Dialog {
    public interface ResultCallback {
        void onComplete(SignatureCapturePayload payload);
    }

    private final Activity activity;
    private final NativeFileUploadService.UploadRequest uploadRequest;
    private final NativeFileUploadService uploadService = new NativeFileUploadService();
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private final String requestId;
    private final String taskId;
    private final String candidateId;
    private final String signerType;
    private final String signerName;
    private final ResultCallback callback;

    private SignatureView signatureView;
    private TextView statusView;
    private ProgressBar progressBar;
    private Button clearButton;
    private Button cancelButton;
    private Button confirmButton;
    private boolean completed;
    private boolean uploading;

    public SignatureCaptureDialog(
            Activity activity,
            NativeFileUploadService.UploadRequest uploadRequest,
            String requestId,
            String taskId,
            String candidateId,
            String signerType,
            String signerName,
            ResultCallback callback
    ) {
        super(activity);
        this.activity = activity;
        this.uploadRequest = uploadRequest;
        this.requestId = requestId;
        this.taskId = taskId;
        this.candidateId = candidateId;
        this.signerType = NativeFileUploadService.trim(signerType) == null ? "CANDIDATE" : signerType.trim();
        this.signerName = NativeFileUploadService.trim(signerName) == null ? "考生" : signerName.trim();
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
        setUploading(false, "请考生签名");
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView title = new TextView(activity);
        title.setText("电子签名");
        title.setTextSize(22);
        root.addView(title, matchWrap());

        statusView = new TextView(activity);
        statusView.setTextSize(15);
        statusView.setPadding(0, dp(8), 0, dp(12));
        root.addView(statusView, matchWrap());

        signatureView = new SignatureView(activity);
        LinearLayout.LayoutParams signatureParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(280)
        );
        root.addView(signatureView, signatureParams);

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
        clearButton = addActionButton(actions, "清空");
        confirmButton = addActionButton(actions, "确认");

        cancelButton.setOnClickListener(view -> cancelFlow());
        clearButton.setOnClickListener(view -> {
            if (!uploading) {
                signatureView.clearSignature();
                setUploading(false, "请考生签名");
            }
        });
        confirmButton.setOnClickListener(view -> confirmSignature());
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

    private void confirmSignature() {
        if (completed || uploading) {
            return;
        }
        if (!signatureView.hasSignature()) {
            setUploading(false, "请完成签名后再确认");
            return;
        }
        final Bitmap bitmap;
        try {
            bitmap = signatureView.renderToBitmap();
        } catch (RuntimeException ex) {
            setUploading(false, "签名图片生成失败，请重试");
            return;
        }
        setUploading(true, "保存中...");
        uploadExecutor.execute(() -> {
            try {
                byte[] pngBytes = bitmapToPng(bitmap);
                NativeFileUploadService.UploadResult upload = uploadService.uploadPng(
                        pngBytes,
                        "signature-" + safeNamePart(taskId) + "-" + safeNamePart(candidateId)
                                + "-" + System.currentTimeMillis() + ".png",
                        uploadRequest
                );
                JSONObject deviceInfo = buildDeviceInfo();
                activity.runOnUiThread(() -> applyUploadSuccess(upload, deviceInfo));
            } catch (NativeFileUploadService.UploadException ex) {
                activity.runOnUiThread(() ->
                        setUploading(false, safeMessage(ex.getMessage(), "签名图片上传失败")));
            } catch (RuntimeException ex) {
                activity.runOnUiThread(() -> setUploading(false, "签名图片生成失败，请重试"));
            } finally {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        });
    }

    private void applyUploadSuccess(NativeFileUploadService.UploadResult upload, JSONObject deviceInfo) {
        if (completed) {
            return;
        }
        completed = true;
        callback.onComplete(SignatureCapturePayload.success(
                requestId,
                upload,
                signerType,
                signerName,
                deviceInfo
        ));
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

    private JSONObject buildDeviceInfo() {
        JSONObject json = new JSONObject();
        try {
            json.put("platform", "android");
            json.put("manufacturer", Build.MANUFACTURER);
            json.put("model", Build.MODEL);
            json.put("sdkInt", Build.VERSION.SDK_INT);
        } catch (JSONException ignored) {
            // Optional metadata only.
        }
        return json;
    }

    private void cancelFlow() {
        if (completed) {
            return;
        }
        completed = true;
        callback.onComplete(SignatureCapturePayload.failure(
                requestId,
                "SIGNATURE_CANCELLED",
                "用户取消签名"
        ));
        dismiss();
    }

    private void setUploading(boolean value, String message) {
        uploading = value;
        statusView.setText(message);
        progressBar.setVisibility(value ? View.VISIBLE : View.GONE);
        clearButton.setEnabled(!value);
        cancelButton.setEnabled(!value);
        confirmButton.setEnabled(!value);
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
