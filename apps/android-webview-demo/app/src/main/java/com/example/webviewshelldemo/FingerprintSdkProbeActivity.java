package com.example.webviewshelldemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.UUID;

public class FingerprintSdkProbeActivity extends Activity {
    private FingerprintSdkAdapter fingerprintSdkAdapter;

    private TextView connectionValue;
    private TextView vidPidValue;
    private TextView serialNumberValue;
    private TextView sdkDeviceTypeValue;
    private TextView captureStatusValue;
    private TextView sdkErrorCodeValue;
    private TextView errorMessageValue;
    private TextView startTimeValue;
    private TextView endTimeValue;
    private TextView durationValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("FAP10 Fingerprint SDK Probe");

        fingerprintSdkAdapter = new FingerprintSdkAdapter(this);
        fingerprintSdkAdapter.setStatusListener(new FingerprintSdkAdapter.StatusListener() {
            @Override
            public void onStatus(FingerprintCapturePayload payload) {
                applyResult(payload);
            }
        });

        setContentView(buildContentView());
        fingerprintSdkAdapter.start();
        applyResult(fingerprintSdkAdapter.checkDevice());
    }

    private View buildContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("FAP10 Fingerprint SDK Probe");
        title.setTextSize(22);
        title.setGravity(Gravity.START);
        title.setPadding(0, 0, 0, dp(6));
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Development page for SDK status checks only. It does not display, save, upload, or return sensitive fingerprint payloads.");
        subtitle.setTextSize(14);
        subtitle.setPadding(0, 0, 0, dp(14));
        root.addView(subtitle, matchWrap());

        LinearLayout buttonGroup = new LinearLayout(this);
        buttonGroup.setOrientation(LinearLayout.VERTICAL);
        buttonGroup.setPadding(0, 0, 0, dp(16));
        root.addView(buttonGroup, matchWrap());

        addButton(buttonGroup, "Detect device", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyResult(fingerprintSdkAdapter.checkDevice());
            }
        });
        addButton(buttonGroup, "Request USB permission", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fingerprintSdkAdapter.requestUsbPermission(new FingerprintSdkAdapter.ResultCallback() {
                    @Override
                    public void onComplete(FingerprintCapturePayload payload) {
                        applyResult(payload);
                    }
                });
            }
        });
        addButton(buttonGroup, "Open device", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyResult(fingerprintSdkAdapter.openDevice());
            }
        });
        addButton(buttonGroup, "Capture once", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fingerprintSdkAdapter.collectOnce(UUID.randomUUID().toString(), new FingerprintSdkAdapter.ResultCallback() {
                    @Override
                    public void onComplete(FingerprintCapturePayload payload) {
                        applyResult(payload);
                    }
                });
            }
        });
        addButton(buttonGroup, "Close device", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyResult(fingerprintSdkAdapter.closeDevice());
            }
        });

        connectionValue = addValueRow(root, "Connection", "Not checked");
        vidPidValue = addValueRow(root, "VID/PID", "-");
        serialNumberValue = addValueRow(root, "Device SN", "-");
        sdkDeviceTypeValue = addValueRow(root, "SDK device type", "-");
        captureStatusValue = addValueRow(root, "Collect status", "Not started");
        sdkErrorCodeValue = addValueRow(root, "Error code", "-");
        errorMessageValue = addValueRow(root, "Error message", "-");
        startTimeValue = addValueRow(root, "Started at", "-");
        endTimeValue = addValueRow(root, "Finished at", "-");
        durationValue = addValueRow(root, "Duration", "-");

        return scrollView;
    }

    private void addButton(LinearLayout parent, String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(16);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        params.setMargins(0, 0, 0, dp(8));
        parent.addView(button, params);
    }

    private TextView addValueRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(13);
        labelView.setGravity(Gravity.START);
        row.addView(labelView, matchWrap());

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(17);
        valueView.setTextIsSelectable(true);
        row.addView(valueView, matchWrap());

        parent.addView(row, matchWrap());
        return valueView;
    }

    private void applyResult(FingerprintCapturePayload payload) {
        if (payload == null || connectionValue == null) {
            return;
        }
        connectionValue.setText(payload.success ? "OK" : "Not ready");
        vidPidValue.setText(formatVidPid(payload));
        serialNumberValue.setText(display(payload.deviceSn));
        sdkDeviceTypeValue.setText(display(payload.sdkDeviceType));
        captureStatusValue.setText(display(payload.collectStatus));
        sdkErrorCodeValue.setText(display(payload.errorCode));
        errorMessageValue.setText(display(payload.errorMessage));
        startTimeValue.setText(display(payload.startedAt));
        endTimeValue.setText(display(payload.finishedAt));
        durationValue.setText(payload.durationMs == null ? "-" : payload.durationMs + " ms");
    }

    private String formatVidPid(FingerprintCapturePayload payload) {
        if (payload.deviceVendorId == null || payload.deviceProductId == null) {
            return "-";
        }
        return payload.deviceVendorId + " / " + payload.deviceProductId;
    }

    private String display(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (fingerprintSdkAdapter != null) {
            fingerprintSdkAdapter.release();
            fingerprintSdkAdapter = null;
        }
        super.onDestroy();
    }
}
